package com.alrex.parcool.common.action.impl;

import com.alrex.parcool.client.RenderBehaviorEnforcer;
import com.alrex.parcool.client.animation.impl.HideInBlockAnimator;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.BehaviorEnforcer;
import com.alrex.parcool.common.action.StaminaConsumeTiming;
import com.alrex.parcool.common.data.client.Animation;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.BufferUtil;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class HideInBlock extends Action {
    private static final BehaviorEnforcer.ID ID_SHOW_NAME = BehaviorEnforcer.newID();
    private static final BehaviorEnforcer.ID ID_SNEAK = BehaviorEnforcer.newID();
    @Nullable
    Vec3 hidingPoint = null;
    @Nullable
    Tuple<BlockPos, BlockPos> hidingArea = null;
    @Nullable
    Vec3 enterPoint = null;
    @Nullable
    Vec3 lookDirection = null;
    boolean hidingBlockChanged = false;
    boolean keyPressed;
    boolean startedFromDiving;
    /**
     * Set when the server refused the start packet (see {@link #adoptServerSideState}). While it is set
     * the action must not touch the player's position, because the claimed hiding point was discarded.
     */
    private boolean startRejected = false;

    @Nullable
    public Vec3 getLookDirection() {
        return lookDirection;
    }

    public boolean isStandbyInAir(Parkourability parkourability) {
        if (!keyPressed) return false;
        Dive dive = parkourability.get(Dive.class);
        return (dive.isDoing() || dive.getNotDoingTick() < 2)
                && (parkourability.getAdditionalProperties().getLandingTick() <= 1);
    }

    @Override
    public boolean canStart(Player player, Parkourability parkourability, ByteBuffer startInfo) {
        if (player.isSprinting()
                || player.noPhysics
                || !player.onGround()
                || player.isInWater()
                || player.isPassenger()
                || player.isVisuallySwimming()
                || getNotDoingTick() < 6
                || parkourability.get(Crawl.class).isDoing()
        ) {
            return false;
        }

        BlockPos hideBaseBlockPos = null;
        boolean startFromDiving = false;
        if (isStandbyInAir(parkourability)) {
            hideBaseBlockPos = player.blockPosition().below();
            startFromDiving = true;
        } else if (KeyBindings.isDown(KeyBindings.getKeyHideInBlock()) && (!ParCoolConfig.Client.Booleans.HideInBlockSneakNeeded.get() || player.getPose() == Pose.CROUCHING)) {
            HitResult result = Minecraft.getInstance().hitResult;
            if (result instanceof BlockHitResult && parkourability.isDoingNothing()) {
                hideBaseBlockPos = ((BlockHitResult) result).getBlockPos();
            }
        }
        if (!startFromDiving && player.hurtTime > 0) {
            return false;
        }

        if (hideBaseBlockPos != null) {
            Tuple<BlockPos, BlockPos> hideArea = WorldUtil.getHideAbleSpace(player, hideBaseBlockPos);
            if (hideArea == null) return false;
            Vec3 hidePoint = new Vec3(
                    0.5 + (hideArea.getA().getX() + hideArea.getB().getX()) / 2.,
                    Math.min(hideArea.getA().getY(), hideArea.getB().getY()),
                    0.5 + (hideArea.getA().getZ() + hideArea.getB().getZ()) / 2.
            );
            if (!player.position().closerThan(hidePoint, 1.8)) return false;
            {
                int minX = Math.min(hideArea.getA().getX(), hideArea.getB().getX());
                int maxX = Math.max(hideArea.getA().getX(), hideArea.getB().getX());
                int minY = Math.min(hideArea.getA().getY(), hideArea.getB().getY());
                int maxY = Math.max(hideArea.getA().getY(), hideArea.getB().getY());
                int minZ = Math.min(hideArea.getA().getZ(), hideArea.getB().getZ());
                int maxZ = Math.max(hideArea.getA().getZ(), hideArea.getB().getZ());
                hideArea = new Tuple<>(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            }
            Vec3 direction;
            boolean stand = player.getBbHeight() < (hideArea.getB().getY() - hideArea.getA().getY() + 1);
            if (stand && startFromDiving) return false;
            if (stand) {
                Vec3 lookAngle = player.getLookAngle();
                direction = Math.abs(lookAngle.x()) > Math.abs(lookAngle.z()) ?
                        new Vec3(lookAngle.x() > 0 ? 1 : -1, 0, 0) :
                        new Vec3(0, 0, lookAngle.z() > 0 ? 1 : -1);
            } else {
                boolean zLonger = Math.abs(hideArea.getA().getZ() - hideArea.getB().getZ()) > Math.abs(hideArea.getA().getX() - hideArea.getB().getX());
                direction = zLonger ?
                        new Vec3(0, 0, player.getLookAngle().z() > 0 ? 1 : -1) :
                        new Vec3(player.getLookAngle().x() > 0 ? 1 : -1, 0, 0);
            }
            BufferUtil.wrap(startInfo)
                    .putBoolean(stand)
                    .putBoolean(startFromDiving)
                    .putBlockPos(hideArea.getA())
                    .putBlockPos(hideArea.getB())
                    .putVec3(hidePoint)
                    .putVec3(player.position())
                    .putVec3(direction);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinue(Player player, Parkourability parkourability) {
        if (startRejected) {
            startRejected = false;
            return false;
        }
        if (hidingBlockChanged) {
            return hidingBlockChanged = false;
        }
        return (player.hurtTime <= 0 || (startedFromDiving && getDoingTick() < 10))
                && player.getPose() == Pose.STANDING
                && (getDoingTick() < 6 || KeyBindings.isDown(KeyBindings.getKeyHideInBlock()) || KeyBindings.isDown(KeyBindings.getKeySneak()));
    }

    @Override
    public void onStart(Player player, Parkourability parkourability, ByteBuffer startData) {
        boolean _stand = BufferUtil.getBoolean(startData);
        startedFromDiving = BufferUtil.getBoolean(startData);
        var claimedArea = new Tuple<>(BufferUtil.getBlockPos(startData), BufferUtil.getBlockPos(startData));
        var claimedPoint = BufferUtil.getVec3(startData);
        enterPoint = BufferUtil.getVec3(startData);
        lookDirection = BufferUtil.getVec3(startData);
        // The local client computed these itself in canStart and may use them as they are; every other
        // side re-derives them, see adoptServerSideState.
        if (player.isLocalPlayer()) {
            hidingArea = claimedArea;
            hidingPoint = claimedPoint;
        } else if (!adoptServerSideState(player, claimedArea)) {
            return;
        }
        if (startedFromDiving) {
            parkourability.getBehaviorEnforcer().setMarkerEnforcePosition(
                    this::isDoing,
                    () -> hidingPoint
            );
        } else {
            parkourability.getBehaviorEnforcer().setMarkerEnforcePosition(
                    this::isDoing,
                    () -> {
                        if (getDoingTick() == 0)
                            return hidingPoint.subtract(enterPoint).scale(0.75).add(enterPoint);
                        return hidingPoint;
                    }
            );
        }
        parkourability.getBehaviorEnforcer().addMarkerCancellingSneak(ID_SNEAK, this::isDoing);
        player.setPose(Pose.STANDING);
        player.noPhysics = true;
        // Claim the flag: EntityMixin's cleanup only gives back a noPhysics that ParCool raised, so a
        // spectator's own noPhysics (Player#tick assigns it from isSpectator()) is never cleared.
        parkourability.getBehaviorEnforcer().setNoPhysicsRaisedByParCool(true);
        player.playSound(player.level()
                        .getBlockState(
                                new BlockPos(
                                        Mth.floor(hidingPoint.x()),
                                        Mth.floor(hidingPoint.y() + 0.2),
                                        Mth.floor(hidingPoint.z())
                                )
                        )
                        .getSoundType().getBreakSound(),
                1, 1
        );
    }

    @Override
    public void onStartInLocalClient(Player player, Parkourability parkourability, ByteBuffer startData) {
        boolean stand = BufferUtil.getBoolean(startData);
        RenderBehaviorEnforcer.serMarkerEnforceCameraType(this::isDoing, () -> CameraType.THIRD_PERSON_BACK);
        parkourability.getBehaviorEnforcer().addMarkerCancellingShowName(ID_SHOW_NAME, this::isDoing);
        spawnOnHideParticles(player);
        Animation animation = Animation.get(player);
        animation.setAnimator(HideInBlockAnimator.class, stand, startedFromDiving);
    }

    @Override
    public void onStartInOtherClient(Player player, Parkourability parkourability, ByteBuffer startData) {
        boolean stand = BufferUtil.getBoolean(startData);
        parkourability.getBehaviorEnforcer().addMarkerCancellingShowName(ID_SHOW_NAME, this::isDoing);
        spawnOnHideParticles(player);
        Animation animation = Animation.get(player);
        animation.setAnimator(HideInBlockAnimator.class, stand, startedFromDiving);
    }


    /**
     * Re-derives the hiding space from the world instead of trusting the start packet.
     *
     * <p>The mod is client-authoritative by design, but {@link #onWorkingTickInServer} moves the player
     * with a coordinate that arrived over the wire, so a modified client could otherwise teleport
     * anywhere - and, by simply never sending Finish, keep a player with {@code noPhysics} pinned
     * there. The packet therefore only carries the <i>intent</i> ("hide at the block I am looking at",
     * which is the area's lower corner); everything the server acts on is recomputed here from the
     * level: the space has to be tagged {@code parcool:hide_able}, the block above it has to be air, the
     * player has to actually fit into it, and the resulting point has to be next to the player.
     *
     * @return {@code false} when the claim does not hold; the caller then drops the action and
     * {@link #canContinue} ends it on the next tick without the player ever having been moved.
     */
    private boolean adoptServerSideState(Player player, Tuple<BlockPos, BlockPos> claimedArea) {
        // getHideAbleSpace answers exactly "is this block hide_able, is the block above air, does the
        // player fit, and is there a second hide_able neighbour it may lie in" - i.e. it re-validates
        // every property the client claimed, against the server's own world.
        var actual = WorldUtil.getHideAbleSpace(player, claimedArea.getA());
        if (actual == null) {
            startRejected = true;
            return false;
        }
        int minX = Math.min(actual.getA().getX(), actual.getB().getX());
        int maxX = Math.max(actual.getA().getX(), actual.getB().getX());
        int minY = Math.min(actual.getA().getY(), actual.getB().getY());
        int maxY = Math.max(actual.getA().getY(), actual.getB().getY());
        int minZ = Math.min(actual.getA().getZ(), actual.getB().getZ());
        int maxZ = Math.max(actual.getA().getZ(), actual.getB().getZ());
        hidingArea = new Tuple<>(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        var point = new Vec3(
                0.5 + (minX + maxX) / 2.,
                minY,
                0.5 + (minZ + maxZ) / 2.
        );
        // Same reach limit canStart applies on the client, so a claim for a far away hideable block is
        // refused as well.
        if (!player.position().closerThan(point, 1.8)) {
            startRejected = true;
            return false;
        }
        hidingPoint = point;
        return true;
    }

    @Override
    public void onWorkingTickInServer(Player player, Parkourability parkourability) {
        if (startRejected || hidingPoint == null) return;
        player.setPos(hidingPoint.x(), hidingPoint.y(), hidingPoint.z());
    }

    @Override
    public void onWorkingTick(Player player, Parkourability parkourability) {
        player.setDeltaMovement(Vec3.ZERO);
        player.noPhysics = true;
        parkourability.getBehaviorEnforcer().setNoPhysicsRaisedByParCool(true);
        player.setSprinting(false);
        player.setPose(Pose.STANDING);
    }

    @Override
    public void onStopInLocalClient(Player player) {
        final Vec3 hidePos = hidingPoint;
        final Vec3 entPos = enterPoint;
        if (hidePos == null || entPos == null) return;
        Parkourability parkourability = Parkourability.get(player);
        parkourability.getBehaviorEnforcer().setMarkerEnforcePosition(
                () -> this.getNotDoingTick() <= 1,
                () -> {
                    if (getNotDoingTick() == 0)
                        return entPos.subtract(hidePos).scale(0.65).add(hidePos);
                    return entPos;
                }
        );
        spawnOnHideParticles(player);
        player.playSound(player.level()
                        .getBlockState(
                                new BlockPos(
                                        Mth.floor(hidingPoint.x()),
                                        Mth.floor(hidingPoint.y() + 0.2),
                                        Mth.floor(hidingPoint.z())
                                )
                        )
                        .getSoundType().getBreakSound(),
                1, 1
        );
    }

    @Override
    public void onStopInOtherClient(Player player) {
        spawnOnHideParticles(player);
        // A Finish broadcast for a player this client never saw start - it entered the render distance
        // while already hidden - reaches here with nothing known, so the un-hide sound has no block to
        // be read from. onStop clears hidingPoint right after this, which is what used to NPE.
        if (hidingPoint == null) return;
        player.playSound(player.level()
                        .getBlockState(
                                new BlockPos(
                                        Mth.floor(hidingPoint.x()),
                                        Mth.floor(hidingPoint.y() + 0.2),
                                        Mth.floor(hidingPoint.z())
                                )
                        )
                        .getSoundType().getBreakSound(),
                1, 1
        );
    }

    @Override
    public void onStop(Player player) {
        hidingPoint = null;
        enterPoint = null;
        hidingArea = null;
        lookDirection = null;
        player.noPhysics = false;
        // onStop only gets the player, so the enforcer is looked up rather than passed in.
        Parkourability parcool$parkourability = Parkourability.get(player);
        if (parcool$parkourability != null) {
            parcool$parkourability.getBehaviorEnforcer().setNoPhysicsRaisedByParCool(false);
        }
    }

    private void spawnOnHideParticles(Player player) {
        if (hidingArea == null) return;
        Level world = player.level();
        int minX = hidingArea.getA().getX();
        int minY = hidingArea.getA().getY();
        int minZ = hidingArea.getA().getZ();
        int maxX = hidingArea.getB().getX();
        int maxY = hidingArea.getB().getY();
        int maxZ = hidingArea.getB().getZ();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.isLoaded(pos)) break;
                    Minecraft.getInstance().particleEngine.destroy(pos, world.getBlockState(pos));
                }
            }
        }
    }

    @Override
    public void onClientTick(Player player, Parkourability parkourability) {
        keyPressed = KeyBindings.isDown(KeyBindings.getKeyHideInBlock());
    }

    @Override
    public void saveSynchronizedState(ByteBuffer buffer) {
        BufferUtil.wrap(buffer).putBoolean(keyPressed);
    }

    @Override
    public void restoreSynchronizedState(ByteBuffer buffer) {
        keyPressed = BufferUtil.getBoolean(buffer);
    }

    private boolean isHidingBlock(BlockPos pos) {
        if (hidingArea == null) {
            return false;
        }
        BlockPos posA = hidingArea.getA(), posB = hidingArea.getB();
        return (posA.getX() <= pos.getX() && pos.getX() <= posB.getX()
                && posA.getY() <= pos.getY() && pos.getY() <= posB.getY()
                && posA.getZ() <= pos.getZ() && pos.getZ() <= posB.getZ()
        );
    }

    public void notifyBlockChanged(BlockPos pos) {
        if (isHidingBlock(pos)) {
            hidingBlockChanged = true;
        }
    }

    @Override
    public StaminaConsumeTiming getStaminaConsumeTiming() {
        return StaminaConsumeTiming.None;
    }

    @Nullable
    public Tuple<BlockPos, BlockPos> getHidingArea() {
        return hidingArea;
    }
}
