package com.alrex.parcool.mixin.client;

import com.alrex.parcool.compat.IPlayerRenderStateEntity;

import com.alrex.parcool.client.animation.PlayerModelRotator;
import com.alrex.parcool.common.data.client.Animation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs ParCool's per-frame action rotation ({@code Animation#rotatePre} / {@code rotatePost}) around
 * the player's own body rotation.
 *
 * <h2>What 1.21.4 changed</h2>
 * {@code PlayerRenderer} still exists but no longer receives the entity - it receives a
 * {@link PlayerRenderState}. Its rotation hook lost the {@code partialTick} argument and now takes
 * {@code (state, poseStack, bodyRot, scale)}, where {@code bodyRot} is what 1.21.1 passed as
 * {@code yBodyRot}.
 *
 * <p>ParCool's animators need the {@link AbstractClientPlayer} itself and a render state carries no
 * entity, so {@link PlayerRenderStateExtractorMixin} stashes the player on the state as it is
 * extracted. The {@code instanceof} guard keeps the scope identical to 1.21.1's
 * {@code PlayerRenderer}: other avatar-like client entities are not animated by ParCool.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Unique
    private PlayerModelRotator parCool$rotator = null;

    // Explicit descriptor: PlayerRenderer declares setupRotations(PlayerRenderState, ...) and also
    // inherits the LivingEntityRenderState one, and a name-only target lets mixin pick the wrong one -
    // which silently drops the PoseStack rotation the animators do.
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("RETURN"))
    protected void onSetupRotationsTail(PlayerRenderState state, PoseStack poseStack, float bodyRot, float scale,
                                        CallbackInfo ci) {
        if (!(((IPlayerRenderStateEntity) state).parcool$getPlayer() instanceof AbstractClientPlayer player)) return;
        Animation animation = Animation.get(player);
        if (animation == null) {
            return;
        }
        if (parCool$rotator != null) {
            animation.rotatePost(player, parCool$rotator);
            parCool$rotator = null;
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("HEAD"), cancellable = true)
    protected void onSetupRotationsHead(PlayerRenderState state, PoseStack poseStack, float bodyRot, float scale,
                                        CallbackInfo ci) {
        if (!(((IPlayerRenderStateEntity) state).parcool$getPlayer() instanceof AbstractClientPlayer player)) return;
        Animation animation = Animation.get(player);
        if (animation == null) {
            return;
        }
        // 1.21.1 received the partial tick as an argument of setupRotations; the render state does not
        // carry it, and Minecraft's own delta tracker holds the very value that was passed in.
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        parCool$rotator = new PlayerModelRotator(poseStack, player, partialTick, bodyRot);
        if (animation.rotatePre(player, parCool$rotator)) {
            parCool$rotator = null;
            ci.cancel();
        }
    }
}
