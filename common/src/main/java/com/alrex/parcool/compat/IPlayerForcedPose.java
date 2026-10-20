package com.alrex.parcool.compat;

import net.minecraft.world.entity.Pose;

import javax.annotation.Nullable;

/**
 * NeoForge's forced-pose extension of {@code Player}, reimplemented for loaders that do not patch it.
 * <p>
 * {@code Player#getForcedPose} / {@code Player#setForcedPose} exist only because NeoForge patches
 * {@code Player} with a {@code forcedPose} field and short-circuits {@code Player#updatePlayerPose}
 * when it is set. ParCool's Crawl and Slide need it to hold {@link Pose#SWIMMING} so the player
 * actually lies down (and so {@code Entity#isVisuallyCrawling()} reports the truth, which several
 * other actions gate on). Vanilla has no such concept, so {@code FabricParCoolPlatform} used to
 * no-op: crawling on Fabric left the hitbox and the pose standing.
 *
 * @see com.alrex.parcool.mixin.common.PlayerForcedPoseMixin
 */
public interface IPlayerForcedPose {
    @Nullable
    Pose parcool$getForcedPose();

    void parcool$setForcedPose(@Nullable Pose pose);
}
