package com.alrex.parcool.mixin.common;

import com.alrex.parcool.common.event.CompatEvents;
import com.alrex.parcool.common.handlers.PlayerDamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces {@code LivingEvent.LivingFallEvent}, which has no Architectury counterpart.
 *
 * <p>{@code LivingEntity#causeFallDamage(float, float, DamageSource)} (1.21.4 still passes the fall
 * distance as a {@code float}; it widens to {@code double} only in 1.21.5) delegates to
 * {@code super.causeFallDamage}, then computes the amount with {@code calculateFallDamage} and
 * applies it with {@code hurt(source, amount)}. Two hooks reproduce the NeoForge event
 * exactly:
 * <ul>
 *     <li>HEAD + {@code cancellable} for {@link CompatEvents.LivingFallEvent#isCanceled()};</li>
 *     <li>a redirect of the single {@code hurt} call (in 1.21.4 it is already
 *     {@code final void hurt(DamageSource, float)}, exactly as in 1.21.7+) for
 *     {@link CompatEvents.LivingFallEvent#getDamageMultiplier()}. Redirecting the damage call rather
 *     than {@code calculateFallDamage} keeps the fall sound and the method's return value intact and
 *     avoids needing access to the protected damage formula.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFallMixin {

    /** Damage multiplier requested by the last dispatched event; reset on every entry. */
    @Unique
    private float parcool$fallDamageMultiplier = 1.0F;

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void parcool$onFall(float fallDistance, float damageMultiplier, DamageSource source,
                                CallbackInfoReturnable<Boolean> cir) {
        this.parcool$fallDamageMultiplier = 1.0F;
        CompatEvents.LivingFallEvent event =
                new CompatEvents.LivingFallEvent((LivingEntity) (Object) this, fallDistance, source);
        PlayerDamageHandler.onFall(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            return;
        }
        this.parcool$fallDamageMultiplier = event.getDamageMultiplier();
    }

    // The damage call LivingEntity#causeFallDamage makes in 1.21.4 is this.hurt(source, amount)V -
    // NOT hurtOrSimulate(...). javap -c on the 1.21.4 dev jar (minecraft-merged-...-1.21.4-loom.*.jar)
    // shows causeFallDamage ending in
    //     39: aload_0
    //     40: aload_3
    //     41: iload 5
    //     42: i2f
    //     43: invokevirtual #944  // Method hurt:(Lnet/minecraft/world/damagesource/DamageSource;F)V
    //     46: iconst_1
    //     47: ireturn
    // and javap -p on Entity confirms `public final void hurt(DamageSource, float)` next to
    // `public final boolean hurtOrSimulate(DamageSource, float)`. hurtOrSimulate exists in 1.21.4 but
    // causeFallDamage never calls it, so redirecting it made Mixin scan 0 targets and abort the boot:
    //   InvalidInjectionException: Injection validation failed: Redirector
    //   parcool$applyDamageMultiplier(...)Z ... expected 1 invocation(s) but 0 succeeded. Scanned 0
    //   target(s). No refMap loaded. [INJECT_APPLY ... -> PostInject -> @Redirect::parcool$applyDamageMultiplier]
    //   Mixin apply for mod parcool failed parcool-common.mixins.json:common.LivingEntityFallMixin
    // This is the same target the 1.21.7 and 1.21.9 ports use; the redirect follows the callee, and
    // the callee returns void, so the handler does too.
    @Redirect(
            method = "causeFallDamage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V")
    )
    private void parcool$applyDamageMultiplier(LivingEntity self, DamageSource source, float amount) {
        float multiplier = this.parcool$fallDamageMultiplier;
        self.hurt(source, multiplier == 1.0F ? amount : amount * multiplier);
    }
}
