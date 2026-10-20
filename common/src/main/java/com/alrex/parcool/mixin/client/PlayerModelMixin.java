package com.alrex.parcool.mixin.client;

import com.alrex.parcool.client.animation.PlayerModelTransformer;
import com.alrex.parcool.compat.IPlayerRenderStateEntity;
import com.alrex.parcool.common.data.client.Animation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs ParCool's model animation ({@code Animation#animatePre} / {@code animatePost}) around
 * {@code PlayerModel#setupAnim}.
 *
 * <h2>What 1.21.4 changed</h2>
 * {@code PlayerModel} lives in {@code net.minecraft.client.model} (it moved to
 * {@code net.minecraft.client.model.player} only in 1.21.9) and is a plain
 * {@code HumanoidModel<PlayerRenderState>}: the setup hook takes a single {@link PlayerRenderState}
 * instead of {@code (entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)}. The five
 * floats are still there on the state under the names {@code walkAnimationPos},
 * {@code walkAnimationSpeed}, {@code ageInTicks}, {@code yRot} and {@code xRot}, and they carry the
 * same values, so {@link PlayerModelTransformer} is fed straight from it. {@code attackTime} and the
 * two arm poses live on {@code HumanoidRenderState} / {@code ArmedEntityRenderState} instead of on the
 * model.
 *
 * <p>The state holds no entity, so the player comes from
 * {@link PlayerRenderStateEntityMixin}. {@code ear} is not a model part in 1.21.4 any more, so the
 * 1.21.1 shadow of that field - which was never read - is gone.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Shadow
    @Final
    private boolean slim;

    @Unique
    private PlayerModelTransformer parCool$transformer = null;

    // Explicit descriptor: 1.21.4 gives PlayerModel three setupAnim overloads
    // (PlayerRenderState + the two bridges), and a name-only target lets mixin pick the wrong one.
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    protected void onSetupAnimHead(PlayerRenderState state, CallbackInfo info) {
        if (!(((IPlayerRenderStateEntity) state).parcool$getPlayer() instanceof AbstractClientPlayer player)) return;
        PlayerModel model = (PlayerModel) (Object) this;

        parCool$transformer = new PlayerModelTransformer(
                player,
                model,
                slim,
                state.ageInTicks,
                state.walkAnimationPos,
                state.walkAnimationSpeed,
                state.yRot,
                state.xRot,
                state.attackTime,
                state.leftArmPose,
                state.rightArmPose
        );
        parCool$transformer.reset();

        Animation animation = Animation.get(player);
        if (animation == null) return;

        boolean shouldCancel = animation.animatePre(player, parCool$transformer);
        parCool$transformer.copyFromBodyToWear();
        if (shouldCancel) {
            parCool$transformer = null;
            info.cancel();
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V",
            at = @At("TAIL"))
    protected void onSetupAnimTail(PlayerRenderState state, CallbackInfo info) {
        if (!(((IPlayerRenderStateEntity) state).parcool$getPlayer() instanceof AbstractClientPlayer player)) return;

        Animation animation = Animation.get(player);
        if (animation == null) {
            parCool$transformer = null;
            return;
        }

        if (parCool$transformer != null) {
            animation.animatePost(player, parCool$transformer);
            parCool$transformer.copyFromBodyToWear();
            parCool$transformer = null;
        }
    }
}
