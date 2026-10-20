package com.alrex.parcool.mixin.client;

import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the name tag of players ParCool is currently hiding (HideInBlock, WallSlide, ...), which used
 * to be {@code LivingEvent.LivingVisibilityEvent}'s consumer.
 *
 * <p>1.21.4 gave {@code LivingEntityRenderer} a third type parameter and moved the hook to
 * {@code shouldShowName(T, double)}, so the mixin no longer has to reproduce the renderer's own
 * supertypes - the handler parameter is declared as the erasure {@code LivingEntity} that the target
 * resolves to.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    protected void onShouldShowName(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            Parkourability parkourability = Parkourability.get(player);
            if (parkourability == null) return;
            if (parkourability.getBehaviorEnforcer().cancelShowingName()) {
                cir.setReturnValue(false);
            }
        }
    }

}
