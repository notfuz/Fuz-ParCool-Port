package com.alrex.parcool.mixin.client;

import com.alrex.parcool.compat.IPlayerRenderStateEntity;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stashes the player on the render state, see {@link PlayerRenderStateEntityMixin}.
 *
 * <p>{@code PlayerRenderer#extractRenderState(AbstractClientPlayer, PlayerRenderState, float)} is the
 * only place where the renderer still holds the entity, and it runs once per frame before the state is
 * rendered, so it is the exact equivalent of the entity 1.21.1's {@code setupRotations} could read.
 *
 * <p>Explicit descriptor: {@code PlayerRenderer} inherits three {@code extractRenderState} overloads
 * (the {@code AbstractClientPlayer} one plus the {@code LivingEntity} / {@code Entity} bridges) and a
 * name-only target lets mixin pick the wrong one.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRenderStateExtractorMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"))
    private void parcool$keepPlayer(AbstractClientPlayer entity, PlayerRenderState state, float partialTick,
                                    CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            ((IPlayerRenderStateEntity) state).parcool$setPlayer(player);
        }
    }
}
