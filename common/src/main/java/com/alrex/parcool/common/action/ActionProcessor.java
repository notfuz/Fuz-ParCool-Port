package com.alrex.parcool.common.action;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.client.action.ClientActionProcessor;
import com.alrex.parcool.common.data.ParCoolDataKeys;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.network.payload.ActionStatePayload;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.BufferUtil;
import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.api.event.ParCoolEventBus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;


import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionProcessor {
	private final ByteBuffer bufferOfPostState = ByteBuffer.allocate(BufferUtil.SYNC_BUFFER_SIZE);
	private final ByteBuffer bufferOfPreState = ByteBuffer.allocate(BufferUtil.SYNC_BUFFER_SIZE);
	private final ByteBuffer bufferOfStarting = ByteBuffer.allocate(BufferUtil.SYNC_BUFFER_SIZE);

	/** How often the local player's watchdog runs, in ticks. */
	private static final int WATCHDOG_INTERVAL = 20;
	/** How often a client with running actions proves it is alive, in ticks. */
	private static final int KEEP_ALIVE_INTERVAL = 20;
	/**
	 * How long the server lets an action run without any client state update before force-stopping it,
	 * in ticks. Comfortably longer than the keep-alive interval so a laggy client is never rescued.
	 */
	private static final int SERVER_ACTION_TIMEOUT_TICK = 100;
	/** Ticks without a server limitation snapshot before the watchdog re-asks for one, in ticks. */
	private static final int LIMITATION_MISSING_WARN_TICK = 200;
	/**
	 * Action classes already reported, so a persistent failure is logged once and not every tick.
	 * Keyed per player: upstream shared one set across the whole world, so the first player to trip an
	 * action muted the report for every other player that hit the same one.
	 */
	private final Map<UUID, HashSet<Class<?>>> reportedFailures = new ConcurrentHashMap<>();

	private HashSet<Class<?>> reportedFailuresOf(Player player) {
		return reportedFailures.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
	}


	public void onTick(Player player) {
		Parkourability parkourability = Parkourability.get(player);

		boolean inClient = player.level().isClientSide();
		boolean inServer = !inClient;

		onTick$doPreprocess();
		if (inClient) {
			ClientActionProcessor.preprocess(player, parkourability);
		} else {
			onTick$doPreprocessInServer();
		}

		List<Action> actions = parkourability.getList();
		boolean needSync = player.isLocalPlayer();

		if (needSync) {
			onTick$checkLimitationSynchronization(player, parkourability);
		}

		parkourability.getAdditionalProperties().onTick(player, parkourability);
		LinkedList<ActionStatePayload.Entry> syncStates = new LinkedList<>();
		for (Action action : actions) {
			ParCoolEventBus.post(new ParCoolActionEvent.Tick.Pre(player, action));
			try {
				processAction(player, parkourability, syncStates, inClient, action);
			} catch (RuntimeException e) {
				// One broken action used to abort the whole loop, so every action after it missed both
				// its canContinue check and its state sync. A stuck action then keeps its behaviour
				// enforcer marker alive forever and the player loses jump / sprint / movement - i.e.
				// "every parkour feature is dead until /kill". Keep the tick alive instead.
				reportActionFailure(player, action, e);
				try {
					if (action.isDoing()) {
						action.finish(player);
						syncStates.addLast(new ActionStatePayload.Entry(
								action.getClass(), ActionStatePayload.Entry.Type.Finish, new byte[0]));
					}
				} catch (RuntimeException finishError) {
					reportActionFailure(player, action, finishError);
				}
			}
			ParCoolEventBus.post(new ParCoolActionEvent.Tick.Post(player, action));
		}
		if (needSync && (!syncStates.isEmpty() || sendsKeepAlive(player, parkourability))) {
			onTick$sendSynchronizationPacket(player, syncStates);
		}

		if (needSync) {
			onTick$watchdog(player, parkourability);
		} else if (inServer) {
			onTick$serverWatchdog(player, parkourability);
		}

		if (inClient) ClientActionProcessor.postProcess(player, parkourability);
	}

	private void reportActionFailure(Player player, Action action, RuntimeException e) {
		if (reportedFailuresOf(player).add(action.getClass())) {
			ParCool.LOGGER.error("[parcool] action {} threw for {}; it is force-stopped and skipped "
					+ "from now on. Further failures of this action are not logged again.", action.getClass().getSimpleName(), player.getName().getString(), e);
		} else {
			ParCool.LOGGER.debug("[parcool] action {} threw again for {}", action.getClass().getSimpleName(), player.getName().getString(), e);
		}
	}

	/**
	 * Whether this tick should send an empty action-state packet purely to prove the client is alive.
	 *
	 * <p>The packet is only sent when something changed, so a long-running action (HideInBlock, a wall
	 * run) produces silence while it is working. The server watchdog cannot tell that apart from a
	 * client that crashed or was modified to never send the matching Finish, so a client with running
	 * actions emits one empty packet per {@link #KEEP_ALIVE_INTERVAL}.
	 */
	private boolean sendsKeepAlive(Player player, Parkourability parkourability) {
		return parkourability.hasAnyDoingAction() && player.tickCount % KEEP_ALIVE_INTERVAL == 0;
	}

	/**
	 * Server-side rescue for actions the client started and never finished.
	 *
	 * <p>Upstream is client-authoritative: the server never calls canStart/canContinue, so a client that
	 * stops sending is the only signal the server gets. Without this, a stuck action keeps its
	 * behaviour-enforcer markers alive (so the player loses jump/sprint), and HideInBlock keeps
	 * {@code noPhysics} on - the player is frozen where the client last put them. Forcing finish() puts
	 * both back.
	 */
	private void onTick$serverWatchdog(Player player, Parkourability parkourability) {
		if (player.tickCount % WATCHDOG_INTERVAL != 0) return;
		if (!parkourability.hasAnyDoingAction()) return;
		int last = parkourability.getLastActionStateTick();
		// last == -1 means the client never sent anything; the timeout is measured from the join tick in
		// that case, so a client that never syncs at all is still rescued.
		int stale = player.tickCount - Math.max(last, 0);
		if (stale < SERVER_ACTION_TIMEOUT_TICK) return;

		parkourability.setLastActionStateTick(player.tickCount);
		int finished = 0;
		for (Action action : parkourability.getList()) {
			if (!action.isDoing()) continue;
			try {
				action.finish(player);
				finished++;
			} catch (RuntimeException e) {
				reportActionFailure(player, action, e);
			}
		}
		// finish() is what normally releases the markers; a client that never sent Finish may also have
		// dropped them mid-action, so the stale-marker sweep runs unconditionally here.
		parkourability.getBehaviorEnforcer().expireStaleJumpMarkers();
		ParCool.LOGGER.warn("[parcool] {} action(s) of {} ran for {}s without a single state update; "
						+ "force-stopped on the server. The client stopped reporting its actions.",
				finished, player.getName().getString(), stale / 20);
	}

	/**
	 * Self-heal for the two states that make every action refuse to start and that nothing else
	 * recovers from: a jump-suppressing behaviour marker whose owner is stuck, and a limitation
	 * snapshot that never arrived (or arrived and was never applied), which leaves
	 * {@code ServerLimitation#UNSYNCED_INSTANCE} in place - and that instance denies every action.
	 */
	private void onTick$watchdog(Player player, Parkourability parkourability) {
		if (player.tickCount % WATCHDOG_INTERVAL != 0) return;

		int expired = parkourability.getBehaviorEnforcer().expireStaleJumpMarkers();
		if (expired > 0) {
			ParCool.LOGGER.warn("[parcool] released {} jump-suppressing marker(s) that outlived their action; "
					+ "an action was stuck reporting isWorking. This is the 'parkour is dead' freeze.", expired);
		}

		if (parkourability.limitationIsNotSynced()) {
			int missing = parkourability.addLimitationMissingTick(WATCHDOG_INTERVAL);
			if (missing >= LIMITATION_MISSING_WARN_TICK) {
				parkourability.setLimitationMissingTick(0);
				ParCool.LOGGER.warn("[parcool] the server limitation snapshot has been missing for {}s, so no action "
								+ "can start. Asking the server again (ParCoolIsActive={}).",
						LIMITATION_MISSING_WARN_TICK / 20,
						parkourability.getClientInfo().get(ParCoolConfig.Client.Booleans.ParCoolIsActive));
				parkourability.trySyncLimitation(player);
			}
		} else if (parkourability.getLimitationMissingTick() != 0) {
			parkourability.setLimitationMissingTick(0);
			ParCool.LOGGER.info("[parcool] server limitation snapshot is in sync again");
		}
	}

	private void onTick$doPreprocess() {
	}

	private void onTick$doPreprocessInServer() {

	}

	private void onTick$checkLimitationSynchronization(Player player, Parkourability parkourability) {
		if (!parkourability.limitationIsNotSynced()) {
			// Upstream never reset the counter, so the five attempts were spent once and one dropped
			// packet disabled every action until the player respawned. A snapshot that arrives proves
			// the connection is healthy, so the budget starts over.
			parkourability.resetSynchronizeTrialCount();
			return;
		}
		if (player.tickCount <= 127 || player.tickCount % 256 != 0) return;
		{
			int trialCount = parkourability.getSynchronizeTrialCount();
			if (trialCount < 5) {
				parkourability.trySyncLimitation(player);
				if (ParCoolConfig.Client.Booleans.ShowAutoResynchronizationNotification.get()) {
					player.displayClientMessage(Component.translatable("parcool.message.error.limitation.not_synced"), false);
				}
				ParCool.LOGGER.warn( "Detected ParCool Limitation is not synced. Sending synchronization request...");
			} else if (trialCount >= Parkourability.MAX_SYNCHRONIZE_TRIAL_COUNT) {
				// The counter saturates at MAX_SYNCHRONIZE_TRIAL_COUNT, so the one-shot failure message
				// below is posted once and the retry loop after it is the only thing that keeps running.
				parkourability.resetSynchronizeTrialCount();
				player.displayClientMessage(Component.translatable("parcool.message.error.limitation.fail_sync").withStyle(ChatFormatting.DARK_RED), false);
				ParCool.LOGGER.error( "Failed to synchronize ParCool Limitation. There may be problems about server connection. Please report to the developer after checking connection");
			} else {
				// Upstream stopped here for good. Keep asking; onTick$watchdog asks again every 10 s.
				ParCool.LOGGER.debug("ParCool Limitation is still not synced after {} attempt(s), retrying", trialCount);
				parkourability.trySyncLimitation(player);
			}
		}
	}

	private void onTick$sendSynchronizationPacket(Player player, List<ActionStatePayload.Entry> syncStates) {
		NetworkRegistries.sendToServer(ActionStatePayload.CODEC, new ActionStatePayload(player.getUUID(), syncStates));

	}

	private void processAction(Player player, Parkourability parkourability, LinkedList<ActionStatePayload.Entry> syncStates, boolean inClientSide, Action action) {
		boolean needSync = player.isLocalPlayer();

		if (needSync) {
			saveSynchronizationState(action, bufferOfPreState);
		}
		action.tick();

		action.onTick(player, parkourability);
		if (inClientSide) {
			action.onClientTick(player, parkourability);
		} else {
			action.onServerTick(player, parkourability);
		}

		if (needSync) {
			checkAndChangeActionState(player, parkourability, action, syncStates);
		}

		if (action.isDoing()) {
			action.onWorkingTick(player, parkourability);
			if (inClientSide) {
				action.onWorkingTickInClient(player, parkourability);
				if (needSync) {
					action.onWorkingTickInLocalClient(player, parkourability);
					if (action.getStaminaConsumeTiming() == StaminaConsumeTiming.OnWorking) {
						ClientActionProcessor.consumeStamina(player, parkourability.getActionInfo().getStaminaConsumptionOf(action.getClass()));
					}
				} else {
					action.onWorkingTickInOtherClient(player, parkourability);
				}
			} else {
				action.onWorkingTickInServer(player, parkourability);
			}
		}

		if (needSync) {
			saveSynchronizationState(action, bufferOfPostState);

			if (!BufferUtil.haveSameContents(bufferOfPreState, bufferOfPostState)) {
				bufferOfPostState.rewind();
				var data = new byte[bufferOfPostState.remaining()];
				bufferOfPostState.get(data);
				syncStates.addLast(new ActionStatePayload.Entry(
						action.getClass(),
						ActionStatePayload.Entry.Type.Normal,
						data
				));
				bufferOfPreState.clear();
				bufferOfPostState.clear();
			}
		}
	}

	private void checkAndChangeActionState(Player player, Parkourability parkourability, Action action, LinkedList<ActionStatePayload.Entry> syncStates) {
		// Player#isLocalPlayer() lives on the server-safe Player type; the pattern match on
		// LocalPlayer would force the client class to load on a dedicated server.
		if (!player.isLocalPlayer()) return;
		if (action.isDoing()) {
			boolean canContinue = parkourability.getActionInfo().can(action.getClass())
					&& !ParCoolDataKeys.getStamina(player).isExhausted()
					&& !ParCoolEventBus.post(new ParCoolActionEvent.TryToContinueEvent(player, action)).isCanceled()
					&& !ParCoolEventBus.post(new ParCoolActionEvent.TryToContinue(player, action)).isCanceled()
					&& action.canContinue(player, parkourability);
			if (!canContinue) {
				ParCoolEventBus.post(new ParCoolActionEvent.Finish.Pre(player, action));
				action.finish(player);
				ParCoolEventBus.post(new ParCoolActionEvent.StopEvent(player, action));
				ParCoolEventBus.post(new ParCoolActionEvent.Finish.Post(player, action));
				syncStates.addLast(new ActionStatePayload.Entry(action.getClass(), ActionStatePayload.Entry.Type.Finish, new byte[0]));
			}
		} else {
			bufferOfStarting.clear();
			boolean start = !player.isSpectator()
					&& !ParCoolDataKeys.getStamina(player).isExhausted()
					&& parkourability.getActionInfo().can(action.getClass())
					&& !ParCoolEventBus.post(new ParCoolActionEvent.TryToStartEvent(player, action)).isCanceled()
					&& !ParCoolEventBus.post(new ParCoolActionEvent.TryToStart(player, action)).isCanceled()
					&& action.canStart(player, parkourability, bufferOfStarting);
			bufferOfStarting.flip();
			if (start) {
				ParCoolEventBus.post(new ParCoolActionEvent.Start.Pre(player, action));
				action.start(player, parkourability, bufferOfStarting);
				ParCoolEventBus.post(new ParCoolActionEvent.StartEvent(player, action));
				ParCoolEventBus.post(new ParCoolActionEvent.Start.Post(player, action));
				if (action.getStaminaConsumeTiming() == StaminaConsumeTiming.OnStart) {
					ClientActionProcessor.consumeStamina(player, parkourability.getActionInfo().getStaminaConsumptionOf(action.getClass()));
				}
				var data = new byte[bufferOfStarting.remaining()];
				bufferOfStarting.get(data);
				syncStates.addLast(new ActionStatePayload.Entry(
						action.getClass(),
						ActionStatePayload.Entry.Type.Start,
						data
				));
			}
		}
	}

	private void saveSynchronizationState(Action action, ByteBuffer buffer) {
		buffer.clear();
		action.saveSynchronizedState(buffer);
		buffer.flip();
	}

	// ====

}
