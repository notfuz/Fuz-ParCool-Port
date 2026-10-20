package com.alrex.parcool.mixin.common;

import com.alrex.parcool.compat.IPlayerForcedPose;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * A vanilla stand-in for NeoForge's forced player pose.
 *
 * <p>NeoForge adds {@code @Nullable Pose forcedPose} to {@code Player} plus a guard at the top of
 * {@code Player#updatePlayerPose}:
 *
 * <pre>{@code
 * protected void updatePlayerPose() {
 *     if (forcedPose != null) { this.setPose(forcedPose); return; }
 *     ... vanilla pose determination ...
 * }
 * }</pre>
 *
 * <p>Without it, Crawl and Slide cannot hold {@link Pose#SWIMMING}: vanilla recomputes the pose every
 * tick from crouch / swim / fall-fly state and overwrites it, the player keeps a standing hitbox, and
 * {@code Entity#isVisuallyCrawling()} stays false - which in turn lets {@code FastRun} and
 * {@code ChargeJump} keep running while the player is supposed to be crawling.
 *
 * <p>On NeoForge the platform still uses the loader's own {@code setForcedPose}, so this field simply
 * stays null and the injection is a no-op there.
 */
@Mixin(Player.class)
public abstract class PlayerForcedPoseMixin implements IPlayerForcedPose {

    @Unique
    @Nullable
    private Pose parCool$forcedPose = null;

    @Override
    @Nullable
    public Pose parcool$getForcedPose() {
        return parCool$forcedPose;
    }

    @Override
    public void parcool$setForcedPose(@Nullable Pose pose) {
        this.parCool$forcedPose = pose;
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void parCool$applyForcedPose(CallbackInfo ci) {
        if (parCool$forcedPose == null) return;
        Player self = (Player) (Object) this;
        self.setPose(parCool$forcedPose);
        ci.cancel();
    }
}
