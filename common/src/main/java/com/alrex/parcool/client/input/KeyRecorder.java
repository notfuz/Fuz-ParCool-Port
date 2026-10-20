package com.alrex.parcool.client.input;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.KeyMapping;

public class KeyRecorder {
	public static final KeyState keyForward = new KeyState();
	public static final KeyState keyBack = new KeyState();
	public static final KeyState keyRight = new KeyState();
	public static final KeyState keyLeft = new KeyState();
	public static final KeyState keySneak = new KeyState();
	public static final KeyState keyJumpState = new KeyState();
	public static final KeyState keySprintState = new KeyState();
	public static final KeyState keyCrawlState = new KeyState();
	public static final KeyState keyOpenSettingsState = new KeyState();
	public static final KeyState keyFastRunning = new KeyState();
	public static final KeyState keyDodge = new KeyState();
    public static final KeyState keyRideZipline = new KeyState();
	public static final KeyState keyBreakfall = new KeyState();
	public static final KeyState keyWallJump = new KeyState();
	public static final KeyState keyQuickTurn = new KeyState();
	public static final KeyState keyFlipping = new KeyState();
	public static final KeyState keyGrabWall = new KeyState();
	public static Vec3 lastDirection = null;

	/**
	 * Was {@code MovementInputUpdateEvent}, which has no Architectury counterpart. Dispatched from
	 * {@code mixin.client.KeyboardInputMixin} on {@code KeyboardInput#tick} RETURN — the same point
	 * NeoForge fires the event from — so the sampled key states see the same values on both loaders.
	 */
	public static void onClientTick() {
		// Fabric runs the client entrypoint before Minecraft builds its Options, so vanilla's own
		// bindings are created *after* ours and re-claim the shared keys. Repair once the world is up.
		KeyBindings.restoreVanillaBindings();
        record(KeyBindings.isKeyForwardDown(), keyForward);
        record(KeyBindings.isKeyBackDown(), keyBack);
        record(KeyBindings.isKeyRightDown(), keyRight);
        record(KeyBindings.isKeyLeftDown(), keyLeft);
		record(KeyBindings.getKeySneak(), keySneak);
        record(KeyBindings.isKeyJumpDown(), keyJumpState);
		record(KeyBindings.getKeySprint(), keySprintState);
		record(KeyBindings.getKeyCrawl(), keyCrawlState);
		record(KeyBindings.isKeyOpenSettingsDown(), keyOpenSettingsState);
		record(KeyBindings.getKeyFastRunning(), keyFastRunning);
		record(KeyBindings.getKeyDodge(), keyDodge);
        record(KeyBindings.getKeyRideZipline(), keyRideZipline);
		record(KeyBindings.getKeyBreakfall(), keyBreakfall);
		record(KeyBindings.isWallJumpDown(), keyWallJump);
		record(KeyBindings.getKeyQuickTurn(), keyQuickTurn);
		record(KeyBindings.getKeyFlipping(), keyFlipping);
		record(KeyBindings.getKeyGrabWall(), keyGrabWall);
		recordMovingVector(KeyBindings.isAnyMovingKeyDown());
	}

	@Nullable
	public static Vec3 getLastMoveVector() {
		return lastDirection;
	}

    private static void record(Boolean isDown, KeyState state) {
        // isDown == null means the helper could not answer (no local player yet, or input null around a
        // respawn). Treated as "not pressed" - unboxing threw a NullPointerException out of the key
        // recorder, i.e. out of KeyboardInput#tick.
        boolean down = isDown != null && isDown;
        state.pressed = (down && state.tickKeyDown == 0);
        state.released = (!down && state.tickNotKeyDown == 0);
        state.doubleTapped = (down && 0 < state.tickNotKeyDown && state.tickNotKeyDown <= 2);
        if (state.pressed && state.tickNotKeyDown > 0) {
            state.previousTickNotKeyDown = state.tickNotKeyDown;
        }
        if (down) {
			state.tickKeyDown++;
			state.tickNotKeyDown = 0;
		} else {
			state.tickKeyDown = 0;
			state.tickNotKeyDown++;
		}
	}

    private static void record(KeyMapping keyBinding, KeyState state) {
        record(KeyBindings.isDown(keyBinding), state);
    }

	private static void recordMovingVector(boolean isMoving) {
		if (isMoving) lastDirection = KeyBindings.getCurrentMoveVector();
	}

	public static class KeyState {
		private boolean pressed = false;
		private boolean released = false;
		private boolean doubleTapped = false;
		private int tickKeyDown = 0;
		private int tickNotKeyDown = 0;
        private int previousTickNotKeyDown = Integer.MAX_VALUE;

		public boolean isPressed() {
			return pressed;
		}

		public boolean isReleased() {
			return released;
		}

		public boolean isDoubleTapped() {
			return doubleTapped;
		}

		public int getTickKeyDown() {
			return tickKeyDown;
		}

		public int getTickNotKeyDown() {
			return tickNotKeyDown;
		}

        public int getPreviousTickNotKeyDown() {
            return previousTickNotKeyDown;
        }
	}
}
