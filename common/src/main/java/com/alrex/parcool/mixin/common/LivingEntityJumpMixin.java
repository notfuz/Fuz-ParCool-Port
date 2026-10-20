package com.alrex.parcool.mixin.common;

import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.handlers.PlayerJumpHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ParCool's two jump hooks. 1.21.1 had {@code Player#jumpFromGround} overriding the inherited
 * method, so the mixin could target {@code Player} and the cast was safe; 1.21.4 moved the method
 * up to {@code LivingEntity} and dropped the override, so the hooks have to live here. Living
 * entities that are not players return immediately, which is what the cast used to guarantee.
 *
 * <h2>Why the second hook has to run at TAIL</h2>
 * It was {@code LivingEvent.LivingJumpEvent}, which NeoForge fires from
 * {@code LivingEntity#jumpFromGround} HEAD. It has to run at <b>TAIL</b> instead, because
 * NeoForge 21.1 made the vanilla jump absolute while vanilla made it relative:
 *
 * <pre>
 * vanilla 1.21.1:  setDeltaMovement(getDeltaMovement().add(0, 0.42, 0))
 * NeoForge 21.1:   setDeltaMovement(vec3.x, getJumpPower(), vec3.z)
 * </pre>
 *
 * <p>So an impulse added before {@code super.jumpFromGround()} is silently overwritten on NeoForge
 * and only survives on Fabric. Measured on NeoForge: the charge jump set
 * {@code dy -0.0784 -> 0.0816} and the first airborne tick still reported the untouched vanilla
 * {@code dy 0.3332} - i.e. the {@code +0.16 * power} boost was discarded and the jump was the
 * normal one block. TAIL also gives {@code Dive#onJump} the real launch velocity instead of the
 * resting {@code -0.0784}.
 *
 * <p>Verified against 1.21.4's vanilla: the jump is still the last thing inside the method that
 * touches the vertical velocity, so the boost {@code ChargeJump} applies from
 * {@code onStartInLocalClient} (after {@code LivingEntity#travel}) still has to be followed by this
 * TAIL hook rather than being applied before the jump.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    public void onJumpFromGround(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) return;
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) return;
        if (parkourability.getBehaviorEnforcer().cancelJump()) {
            ci.cancel();
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    public void onJumpFromGroundTail(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) return;
        PlayerJumpHandler.onJump(player);
    }
}
