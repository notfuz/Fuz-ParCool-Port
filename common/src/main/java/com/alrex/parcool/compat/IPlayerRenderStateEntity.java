package com.alrex.parcool.compat;

import net.minecraft.client.player.AbstractClientPlayer;

/**
 * The duck interface {@code mixin.client.PlayerRenderStateEntityMixin} adds to
 * {@link net.minecraft.client.renderer.entity.state.PlayerRenderState}.
 *
 * <p>1.21.4 entity renderers no longer see the entity while rendering: {@code setupRotations} and
 * {@code PlayerModel#setupAnim} only get a render state, and a state holds no entity reference.
 * ParCool's animators are keyed on the player, so the player has to travel with the state - the mixin
 * writes it in while the render state is extracted and the two mixins read it back through this
 * interface.
 *
 * <p>It lives in {@code compat} and not in the mixin package because mixin refuses to let a class in
 * its own package be referenced from ordinary code
 * ({@code IllegalClassLoadError: ... is in a defined mixin package}).
 */
public interface IPlayerRenderStateEntity {

    AbstractClientPlayer parcool$getPlayer();

    void parcool$setPlayer(AbstractClientPlayer player);
}
