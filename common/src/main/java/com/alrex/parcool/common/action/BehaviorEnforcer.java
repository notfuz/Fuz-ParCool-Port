package com.alrex.parcool.common.action;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

public class BehaviorEnforcer {
    /**
     * Re-entrancy guard for the movement enforcer, set while a move is being applied on the enforcer's
     * behalf.
     *
     * <p>It lives here, in a plain mod class, rather than as a {@code @Unique} field on the mixin that
     * needs it: Mixin does not carry a {@code @Unique static} field over from a mixin class into the
     * target, so the flag was silently never read or written and the guard did nothing.
     *
     * <p>A {@link ThreadLocal} rather than a plain boolean: the enforcers are per-player and the flag
     * must not leak between the render thread and the server thread.
     */
    private static final ThreadLocal<Boolean> APPLYING_ENFORCED_MOVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** @return whether a move is currently being applied on an enforcer's behalf. */
    public static boolean isApplyingEnforcedMove() {
        return APPLYING_ENFORCED_MOVE.get();
    }

    /** Opens or closes the re-entrancy window opened by {@link #isApplyingEnforcedMove()}. */
    public static void setApplyingEnforcedMove(boolean applying) {
        APPLYING_ENFORCED_MOVE.set(applying);
    }

    /**
     * A jump-suppressing marker, plus how long it has been alive.
     *
     * <p>Upstream drops a marker as soon as its owner stops doing, but only when somebody asks
     * ({@link #cancelJump()}), and the owner is a method reference into the action. If the action ever
     * gets stuck reporting {@code isDoing() == true} the jump gate stays shut for good, which reads as
     * "every parkour feature is dead". The age gives the watchdog a way to cut that knot.
     */
    public static class RegisteredMarker {
        final Marker marker;
        int age = 0;

        RegisteredMarker(Marker marker) {
            this.marker = marker;
        }
    }

    /**
     * Hard cap for a jump-suppressing marker, in ticks (10 s).
     *
     * <p>Only the jump map is watched, because only its owners are provably short-lived: Dodge
     * ({@code MAX_TICK}), Roll ({@code getRollMaxTick()}), Slide (sliding-continuable tick) and Tap
     * ({@code 8}). The other maps belong to actions that are meant to last as long as the player
     * wants - ClingToCliff, HangDown and HideInBlock all have an indefinite {@code canContinue} - so
     * they are left to remove themselves.
     */
    public static final int STALE_JUMP_MARKER_TICK = 200;

    public static class ID implements Comparable<ID> {
        private static int idValue = 0;

        private static ID newID() {
            return new ID(idValue++);
        }

        private final int value;

        private ID(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(ID o) {
            return Integer.compare(this.value, o.value);
        }
    }

    public static ID newID() {
        return ID.newID();
    }

    public interface Marker {
        boolean remain();
    }

    public static class Enforcer<T> {
        final Marker marker;
        final Supplier<T> behaviorSupplier;

        Enforcer(Marker marker, Supplier<T> supplier) {
            this.marker = marker;
            this.behaviorSupplier = supplier;
        }

        boolean remain() {
            return marker.remain();
        }

        T getBehavior() {
            return behaviorSupplier.get();
        }
    }

    private final TreeMap<ID, RegisteredMarker> jumpCancelMarks = new TreeMap<>();
    private final TreeMap<ID, Marker> descendFromEdgeCancelMarks = new TreeMap<>();
    private final ConcurrentSkipListMap<ID, Marker> sneakCancelMarks = new ConcurrentSkipListMap<>();
    private final TreeMap<ID, Marker> sprintCancelMarks = new TreeMap<>();
    private final TreeMap<ID, Marker> fallFlyingCancelMarks = new TreeMap<>();
    private final TreeMap<ID, Marker> showNameCancelMarks = new TreeMap<>();
    @Nullable
    private Enforcer<Vec3> movementEnforcer = null;

    /**
     * Whether ParCool itself is the reason {@link net.minecraft.world.entity.Entity#noPhysics} is set
     * on this player.
     *
     * <p>The flag on {@code Entity} is shared with vanilla, and vanilla owns it: {@code Player#tick}
     * runs {@code noPhysics = isSpectator()} every tick, and that assignment is the entire mechanism
     * behind spectator noclip. A cleanup that clears {@code noPhysics} whenever nothing is being
     * enforced therefore wipes out vanilla's flag and breaks flying through blocks in spectator mode,
     * on every version, and Ctrl+P does not help because no action has to be running for it to fire.
     *
     * <p>So ParCool records that it was the one who raised the flag, and only ever gives it back when
     * the flag is its own. Verified against the mojmap jar:
     *
     * <pre>
     * javap -c net.minecraft.world.entity.player.Player     # inside tick()
     *        2: invokevirtual isSpectator:()Z
     *        5: putfield      noPhysics:Z
     * </pre>
     *
     * @see com.alrex.parcool.mixin.common.EntityMixin
     */
    private boolean noPhysicsRaisedByParCool = false;
    @Nullable
    private Enforcer<Vec3> positionEnforcer = null;

    public void addMarkerCancellingJump(ID id, Marker marker) {
        jumpCancelMarks.put(id, new RegisteredMarker(marker));
    }

    public void addMarkerCancellingSneak(ID id, Marker marker) {
        sneakCancelMarks.put(id, marker);
    }

    public void addMarkerCancellingDescendFromEdge(ID id, Marker marker) {
        descendFromEdgeCancelMarks.put(id, marker);
    }

    public void addMarkerCancellingSprint(ID id, Marker marker) {
        sprintCancelMarks.put(id, marker);
    }

    public void addMarkerCancellingFallFlying(ID id, Marker marker) {
        fallFlyingCancelMarks.put(id, marker);
    }

    public void addMarkerCancellingShowName(ID id, Marker marker) {
        showNameCancelMarks.put(id, marker);
    }

    public void setMarkerEnforceMovePoint(Marker marker, Supplier<Vec3> movementSupplier) {
        movementEnforcer = new Enforcer<>(marker, movementSupplier);
    }

    public void setMarkerEnforcePosition(Marker marker, Supplier<Vec3> movementSupplier) {
        positionEnforcer = new Enforcer<>(marker, movementSupplier);
    }

    public boolean cancelJump() {
        jumpCancelMarks.values().removeIf(it -> !it.marker.remain());
        return !jumpCancelMarks.isEmpty();
    }

    /**
     * Ages the jump-suppressing markers and releases any that outlived their owner.
     *
     * <p>Called once a second for the local player. Returns the number of markers that had to be cut,
     * which is the signal that an action reported {@code isDoing()} long past the point where its own
     * {@code canContinue} should have ended it - the "all parkour is dead until /kill" freeze.
     */
    public int expireStaleJumpMarkers() {
        int expired = 0;
        var iterator = jumpCancelMarks.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            RegisteredMarker registered = entry.getValue();
            if (!registered.marker.remain()) {
                iterator.remove();
                continue;
            }
            if (++registered.age > STALE_JUMP_MARKER_TICK) {
                iterator.remove();
                expired++;
            }
        }
        return expired;
    }

    public boolean cancelSneak() {
        sneakCancelMarks.values().removeIf(it -> !it.remain());
        return !sneakCancelMarks.isEmpty();
    }

    public boolean cancelDescendFromEdge() {
        descendFromEdgeCancelMarks.values().removeIf(it -> !it.remain());
        return !descendFromEdgeCancelMarks.isEmpty();
    }

    public boolean cancelSprint() {
        sprintCancelMarks.values().removeIf(it -> !it.remain());
        return !sprintCancelMarks.isEmpty();
    }

    public boolean cancelFallFlying() {
        fallFlyingCancelMarks.values().removeIf(it -> !it.remain());
        return !fallFlyingCancelMarks.isEmpty();
    }

    public boolean cancelShowingName() {
        showNameCancelMarks.values().removeIf(it -> !it.remain());
        return !showNameCancelMarks.isEmpty();
    }

    @Nullable
    public Vec3 getEnforcedMovePoint() {
        if (movementEnforcer != null && remainsOrRelease(movementEnforcer)) {
            return movementEnforcer.getBehavior();
        }
        movementEnforcer = null;
        return null;
    }

    @Nullable
    public boolean isNoPhysicsRaisedByParCool() {
        return this.noPhysicsRaisedByParCool;
    }

    public void setNoPhysicsRaisedByParCool(boolean raised) {
        this.noPhysicsRaisedByParCool = raised;
    }

    public Vec3 getEnforcedPosition() {
        if (positionEnforcer != null && remainsOrRelease(positionEnforcer)) {
            return positionEnforcer.getBehavior();
        }
        positionEnforcer = null;
        return null;
    }

    /**
     * A stuck movement/position enforcer pins the player in place, so a throwing owner is treated as
     * "no longer valid" rather than being allowed to escape into the movement code every tick.
     */
    private boolean remainsOrRelease(Enforcer<?> enforcer) {
        try {
            return enforcer.remain();
        } catch (RuntimeException e) {
            com.alrex.parcool.ParCool.LOGGER.error("[parcool] behaviour enforcer threw, releasing it", e);
            return false;
        }
    }
}
