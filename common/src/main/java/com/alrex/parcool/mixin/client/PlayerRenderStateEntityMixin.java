package com.alrex.parcool.mixin.client;

import com.alrex.parcool.compat.IPlayerRenderStateEntity;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Carries the rendered player on {@link PlayerRenderState}.
 *
 * <p>1.21.4 entity renderers no longer see the entity while rendering: {@code setupRotations} and
 * {@code PlayerModel#setupAnim} only get a render state, and a state holds no entity reference.
 * ParCool's animators are keyed on the player (its {@code Animation}, its {@code Parkourability}, its
 * pose and head yaw), so the player has to travel with the state.
 *
 * <p>Written in {@link PlayerRenderStateExtractorMixin} while the render state is extracted, i.e. at
 * the same point in the frame where 1.21.1's {@code setupRotations} could still read the entity. The
 * field is created lazily - mixin field initialisers run in the mixin's own constructor and are
 * unreliable for a state object the renderer pool reuses.
 */
@Mixin(PlayerRenderState.class)
public abstract class PlayerRenderStateEntityMixin implements IPlayerRenderStateEntity {

    @Unique
    private AbstractClientPlayer parcool$player;

    @Override
    public AbstractClientPlayer parcool$getPlayer() {
        return this.parcool$player;
    }

    @Override
    public void parcool$setPlayer(AbstractClientPlayer player) {
        this.parcool$player = player;
    }
}
