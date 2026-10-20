package com.alrex.parcool.common.data;

import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.Actions;
import com.alrex.parcool.common.action.AdditionalProperties;
import com.alrex.parcool.common.action.BehaviorEnforcer;
import com.alrex.parcool.common.data.ParCoolDataKeys;
import com.alrex.parcool.common.info.ActionInfo;
import com.alrex.parcool.common.info.ClientSetting;
import com.alrex.parcool.common.info.ServerLimitation;
import com.alrex.parcool.common.network.payload.ClientInformationPayload;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.common.network.NetworkRegistries;
import net.minecraft.world.entity.player.Player;


import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;

public class Parkourability {
	public static Parkourability get(Player player) {
		return ParCoolDataKeys.getParkourability(player);
	}

    private final ActionInfo info;
	private final AdditionalProperties properties = new AdditionalProperties();
	private final BehaviorEnforcer enforcer = new BehaviorEnforcer();
	private final List<Action> actions = Actions.constructActionsList();
	private final HashMap<Class<? extends Action>, Action> actionsMap;
	private int synchronizeTrialCount = 0;
	/** Ticks the server limitation snapshot has been missing, for the ActionProcessor watchdog. */
	private int limitationMissingTick = 0;
	/**
	 * Tick of the last C2S {@code ActionStatePayload} this player sent, used by the server watchdog to
	 * tell a hung action from a running one. -1 until the first packet arrives.
	 */
	private int lastActionStateTick = -1;

	public Parkourability() {
		actionsMap = new HashMap<>((int) (actions.size() * 1.5));
        for (Action action : actions) {
			actionsMap.put(action.getClass(), action);
		}
        info = new ActionInfo();
	}

	public <T extends Action> T get(Class<T> action) {
		T value = (T) actionsMap.getOrDefault(action, null);
		if (value == null) {
			throw new IllegalArgumentException("The Action instance is not registered:" + action.getSimpleName());
		}
		return value;
	}

	public short getActionID(Action instance) {
		return Actions.getIndexOf(instance.getClass());
	}

	@Nullable
	public Action getActionFromID(short id) {
		if (0 <= id && id < actions.size()) {
			return actions.get(id);
		}
		return null;
	}

	public AdditionalProperties getAdditionalProperties() {
		return properties;
	}

	public BehaviorEnforcer getBehaviorEnforcer() {
		return enforcer;
    }

	public ActionInfo getActionInfo() {
		return info;
	}

    public ClientSetting getClientInfo() {
        return info.getClientSetting();
	}

    public ServerLimitation getServerLimitation() {
        return info.getServerLimitation();
    }

	public List<Action> getList() {
		return actions;
	}

	public void CopyFrom(Parkourability original) {
        getActionInfo().setClientSetting(original.getActionInfo().getClientSetting());
        getActionInfo().setServerLimitation(original.getActionInfo().getServerLimitation());
	}

	public boolean isDoingNothing() {
		return actions.stream().noneMatch(Action::isDoing);
	}

	public boolean getLimitedValue(ParCoolConfig.Client.Booleans client, ParCoolConfig.Server.Booleans server) {
		if (server.AdvantageousValue) {
			return (getClientInfo().get(client) && getServerLimitation().get(server));
		} else {
			return !(getClientInfo().get(client) || getServerLimitation().get(server));
		}
	}

	public int getLimitedValue(ParCoolConfig.Client.Integers client, ParCoolConfig.Server.Integers server) {
		if (server.Advantageous == ParCoolConfig.AdvantageousDirection.Higher) {
			return Math.min(getClientInfo().get(client), getServerLimitation().get(server));
		} else {
			return Math.max(getClientInfo().get(client), getServerLimitation().get(server));
		}
	}

	public double getLimitedValue(ParCoolConfig.Client.Doubles client, ParCoolConfig.Server.Doubles server) {
		if (server.Advantageous == ParCoolConfig.AdvantageousDirection.Higher) {
			return Math.min(getClientInfo().get(client), getServerLimitation().get(server));
		} else {
			return Math.max(getClientInfo().get(client), getServerLimitation().get(server));
		}
	}

	/**
	 * Asks the server to (re)send the limitation set. Only ever called for the local player, but the
	 * parameter is typed {@link Player} so that {@code Parkourability}, which a dedicated server does
	 * load, never references a client-only class.
	 */
	public void trySyncLimitation(Player player) {
		synchronizeTrialCount++;
		NetworkRegistries.sendToServer(ClientInformationPayload.CODEC, new ClientInformationPayload(player.getUUID(), true, getClientInfo()));
	}

	public int getSynchronizeTrialCount() {
		return synchronizeTrialCount;
	}

	public void incrementSynchronizeTrialCount() {
		synchronizeTrialCount = Math.min(synchronizeTrialCount + 1, MAX_SYNCHRONIZE_TRIAL_COUNT);
	}

	/**
	 * Upper bound on the retry counter. Upstream had none: the counter was only ever compared against
	 * 5 and kept being incremented, so it grew without limit for as long as a player stayed on a server
	 * that never answers - an int that eventually wraps back to "5" and re-arms the failure branch.
	 */
	public static final int MAX_SYNCHRONIZE_TRIAL_COUNT = 5;

	public void resetSynchronizeTrialCount() {
		if (synchronizeTrialCount == 0) return;
		synchronizeTrialCount = 0;
	}

	public int getLastActionStateTick() {
		return lastActionStateTick;
	}

	public void setLastActionStateTick(int tick) {
		this.lastActionStateTick = tick;
	}

	/** True when at least one action believes it is running. */
	public boolean hasAnyDoingAction() {
		return actions.stream().anyMatch(Action::isDoing);
	}

	public int getLimitationMissingTick() {
		return limitationMissingTick;
	}

	public void setLimitationMissingTick(int tick) {
		limitationMissingTick = tick;
	}

	/** Returns the new value, so the caller can compare against its threshold in one statement. */
	public int addLimitationMissingTick(int tick) {
		limitationMissingTick += tick;
		return limitationMissingTick;
	}

	public boolean limitationIsNotSynced() {
		return !getServerLimitation().isSynced();
	}

	@SafeVarargs
	public final Boolean isDoingAny(Class<? extends Action>... actions) {
		for (Class<? extends Action> action : actions) {
			if (get(action).isDoing()) {
				return true;
			}
		}

		return false;
	}
}
