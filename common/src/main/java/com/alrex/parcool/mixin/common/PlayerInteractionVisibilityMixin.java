package com.alrex.parcool.mixin.common;

import com.alrex.parcool.common.handlers.PlayerVisibilityHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces {@code LivingEvent.LivingVisibilityEvent}.
 * <p>
 * NeoForge posts that event from {@code CommonHooks#getEntityVisibilityMultiplier}, whose only
 * vanilla caller is {@code Player#canInteractWithEntity(Entity, double)} — the predicate the entity
 * tracker uses to decide whether a viewer can see a tracked entity at a given range. The upstream
 * handler multiplied the visibility modifier by 0.1, which always lands below the interaction range
 * threshold, so returning {@code false} outright is behaviourally equivalent and is what this
 * injection does.
 */
@Mixin(Player.class)
public abstract class PlayerInteractionVisibilityMixin {

    @Inject(method = "canInteractWithEntity(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
    private void parcool$hideFromViewer(Entity target, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Player other && PlayerVisibilityHandler.isHiddenFrom((Player) (Object) this, other)) {
            cir.setReturnValue(false);
        }
    }
}
