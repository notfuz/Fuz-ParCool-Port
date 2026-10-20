package com.alrex.parcool.common.action;

import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import com.alrex.parcool.common.event.CompatEvents;

import java.nio.ByteBuffer;

public abstract class Action {
	private boolean doing = false;
	private int doingTick = 0;
	private int notDoingTick = 0;
	private int tickFromStarted = -1;

	public boolean isJustStarted() {
		return isDoing() && getDoingTick() == 0;
	}

	public int getDoingTick() {
		return doingTick;
	}

	public int getNotDoingTick() {
		return notDoingTick;
	}

	public int getTickFromLastStarted() {
		return tickFromStarted;
	}

	public boolean isDoing() {
		return doing;
	}

	public void tick() {
		if (doing) {
			doingTick++;
			notDoingTick = 0;
		} else {
			notDoingTick++;
			doingTick = 0;
		}
		if (tickFromStarted >= 0) {
			tickFromStarted++;
		}
	}

	public void start(Player player, Parkourability parkourability, ByteBuffer startInfo) {
		doing = true;
		tickFromStarted = 0;
		onStart(player, parkourability, startInfo);
		startInfo.rewind();
		if (player.isLocalPlayer()) {
			onStartInLocalClient(player, parkourability, startInfo);
		} else {
			if (player.level().isClientSide()) {
				onStartInOtherClient(player, parkourability, startInfo);
			} else {
				onStartInServer(player, parkourability, startInfo);
			}
		}
		startInfo.rewind();
	}

	public void finish(Player player) {
		doing = false;
		if (player.isLocalPlayer()) {
			onStopInLocalClient(player);
		} else {
			if (player.level().isClientSide()) {
				onStopInOtherClient(player);
			} else {
				onStopInServer(player);
			}
		}
		onStop(player);
	}

	public abstract boolean canStart(Player player, Parkourability parkourability, ByteBuffer startInfo);

	public abstract boolean canContinue(Player player, Parkourability parkourability);

	public void onStart(Player player, Parkourability parkourability, ByteBuffer startInfo) {
	}

	public void onStartInServer(Player player, Parkourability parkourability, ByteBuffer startInfo) {
	}

	public void onStartInOtherClient(Player player, Parkourability parkourability, ByteBuffer startInfo) {
	}

	public void onStartInLocalClient(Player player, Parkourability parkourability, ByteBuffer startInfo) {
	}

	public void onStop(Player player) {
	}

	public void onStopInServer(Player player) {
	}

	public void onStopInOtherClient(Player player) {
	}

	public void onStopInLocalClient(Player player) {
	}

	public void onWorkingTick(Player player, Parkourability parkourability) {
	}

	public void onWorkingTickInServer(Player player, Parkourability parkourability) {
	}

	public void onWorkingTickInClient(Player player, Parkourability parkourability) {
	}

	public void onWorkingTickInOtherClient(Player player, Parkourability parkourability) {
	}

	public void onWorkingTickInLocalClient(Player player, Parkourability parkourability) {
	}

	public void onTick(Player player, Parkourability parkourability) {
	}

	public void onServerTick(Player player, Parkourability parkourability) {
	}

	public void onClientTick(Player player, Parkourability parkourability) {
	}

	public void onRenderTick(CompatEvents.RenderFrameEvent event, Player player, Parkourability parkourability) {
	}

	public void restoreSynchronizedState(ByteBuffer buffer) {
	}

	public void saveSynchronizedState(ByteBuffer buffer) {
	}

    /**
     * The status-bar hooks take a plain {@link Player}: the JVM verifier resolves a parameter type when
     * it checks that a subclass really overrides this method, and {@code Actions}' static initialiser
     * is reached on a dedicated server - so a {@code LocalPlayer} parameter makes the server die with
     * "Attempted to load class net.minecraft.client.player.LocalPlayer which is not present on the
     * dedicated server". No implementation uses anything client-only, and the only callers
     * ({@code StaminaHUD} / {@code LightStaminaHUD}) already hold the local player.
     */
    public boolean wantsToShowStatusBar(Player player, Parkourability parkourability) {
        return false;
    }

    /** See {@link #wantsToShowStatusBar} for why this is a {@link Player} and not a {@code LocalPlayer}. */
    public float getStatusValue(Player player, Parkourability parkourability) {
        return 0;
    }

	public abstract StaminaConsumeTiming getStaminaConsumeTiming();
}
