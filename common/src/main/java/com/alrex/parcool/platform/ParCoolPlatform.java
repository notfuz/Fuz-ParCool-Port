package com.alrex.parcool.platform;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Everything ParCool needs from a specific loader.
 *
 * <h2>Why this exists</h2>
 * <ul>
 *     <li>{@code net.neoforged.neoforge.common.NeoForgeMod#SWIM_SPEED} is a NeoForge-added entity
 *     attribute ({@code neoforgeswim_speed}); vanilla Fabric has no equivalent, so FastSwim's
 *     server-side speed bonus has nothing to modify there.</li>
 *     <li>{@code Level#damageSources()} is a NeoForge-added accessor, so resolving a
 *     {@link DamageSource} from a {@link DamageType} key needs a loader specific call.</li>
 *     <li><b>Paraglider</b>, <b>EpicFight</b> and <b>BetterThirdPerson</b> ship NeoForge-only jars
 *     for 1.21.1, so their code lives in the {@code neoforge} module and is reached through this
 *     interface. Fabric ships no-op implementations below. Fabric ships no-op implementations.</li>
 * </ul>
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader}, so {@code common} stays
 * free of {@code net.fabricmc.*} and {@code net.neoforged.*} imports. See
 * {@code fabric/.../platform/FabricParCoolPlatform} and
 * {@code neoforge/.../platform/NeoForgeParCoolPlatform}.
 */
public interface ParCoolPlatform {

    /**
     * @return the player's {@code neoforgeswim_speed} attribute, or {@code null} on loaders that
     * do not have it.
     */
    @Nullable
    AttributeInstance getSwimSpeedAttribute(Player player);

    /**
     * @return a damage source for the given damage type key, or {@code null} if it cannot be built.
     */
    @Nullable
    DamageSource createDamageSource(Player player, ResourceKey<DamageType> type);

    /**
     * Entity-aware block friction. NeoForge adds {@code BlockState#getFriction(Level, BlockPos, Entity)}
     * so that, for example, a sneaking player slides less on ice; vanilla only offers the
     * state-level {@code Block#getFriction()}.
     */
    float getFriction(BlockState state, Level level, BlockPos pos, @Nullable Entity entity);

    // ------------------------------------------------------------------
    // forced pose (NeoForge-only Player#getForcedPose / #setForcedPose)
    // ------------------------------------------------------------------

    @Nullable
    Pose getForcedPose(Player player);

    void setForcedPose(Player player, @Nullable Pose pose);

    // ------------------------------------------------------------------
    // BetterThirdPerson (NeoForge only)
    // ------------------------------------------------------------------

    boolean isBetterThirdPersonCameraDecoupled();

    // ------------------------------------------------------------------
    // Paraglider (NeoForge only)
    // ------------------------------------------------------------------

    IParCoolStaminaHandler newParagliderStaminaHandler(Player player);

    boolean isUsingParagliderStamina(Parkourability parkourability);

    boolean isFallingWithParaglider(Player player);

    // ------------------------------------------------------------------
    // EpicFight (NeoForge only)
    // ------------------------------------------------------------------

    IParCoolStaminaHandler newEpicFightStaminaHandler(Player player);

    boolean isUsingEpicFightStamina(Parkourability parkourability);

    /** The loader's network plumbing; see {@link ParCoolNetwork} for why this is a seam. */
    ParCoolNetwork getNetwork();

    // ------------------------------------------------------------------
    // block entity type (1.21.4 removed BlockEntityType.Builder)
    // ------------------------------------------------------------------

    /**
     * Registers one {@link net.minecraft.world.level.block.entity.BlockEntityType} and returns a
     * supplier for it.
     *
     * <p>This is a seam because 1.21.4 deleted {@code BlockEntityType.Builder} and left only
     * {@code BlockEntityType}'s own construction private, while the registry entry still has to be
     * created inside the writable window - which is the mod constructor on Fabric and the registry
     * event on NeoForge.
     *
     * <p>{@code blocks} is a supplier because the block registry entries do not exist yet when the
     * caller hands them over on NeoForge.
     */
    <T extends net.minecraft.world.level.block.entity.BlockEntity> java.util.function.Supplier<
            net.minecraft.world.level.block.entity.BlockEntityType<T>> registerBlockEntityType(
            String name,
            net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier<T> factory,
            java.util.function.Supplier<net.minecraft.world.level.block.Block[]> blocks);

    // ------------------------------------------------------------------
    // shared
    // ------------------------------------------------------------------

    /**
     * Lets a third-party shoulder-camera mod redirect the Dodge camera animation.
     *
     * @return {@code Front} when the camera is decoupled, otherwise {@code direction} unchanged.
     */
    default Dodge.DodgeDirection handleDodgeCameraRotation(Dodge.DodgeDirection direction) {
        return direction;
    }
}
