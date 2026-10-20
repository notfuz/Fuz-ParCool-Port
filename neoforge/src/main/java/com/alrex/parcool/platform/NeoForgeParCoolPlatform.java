package com.alrex.parcool.platform;

import com.alrex.parcool.ParCool;
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
import net.neoforged.neoforge.common.NeoForgeMod;

import javax.annotation.Nullable;

/**
 * NeoForge implementation of {@link ParCoolPlatform}: the loader that still owns every API the
 * optional integrations need, so everything here can use the real NeoForge calls.
 */
public class NeoForgeParCoolPlatform implements ParCoolPlatform {

    @Override
    @Nullable
    public AttributeInstance getSwimSpeedAttribute(Player player) {
        return player.getAttribute(NeoForgeMod.SWIM_SPEED);
    }

    @Override
    @Nullable
    public DamageSource createDamageSource(Player player, ResourceKey<DamageType> type) {
        return player.level().damageSources().source(type);
    }

    @Override
    public float getFriction(BlockState state, Level level, BlockPos pos, @Nullable Entity entity) {
        return state.getFriction(level, pos, entity);
    }

    @Override
    @Nullable
    public Pose getForcedPose(Player player) {
        return player.getForcedPose();
    }

    @Override
    public void setForcedPose(Player player, @Nullable Pose pose) {
        player.setForcedPose(pose);
    }

    /** Single instance: the Fabric implementation de-duplicates registrations per payload class,
     *  which only works if the same object is returned for the whole mod lifetime. */
    private static final com.alrex.parcool.platform.ParCoolNetwork NETWORK =
            new com.alrex.parcool.platform.NeoForgeParCoolNetwork();

    @Override
    public com.alrex.parcool.platform.ParCoolNetwork getNetwork() {
        return NETWORK;
    }

    /**
     * NeoForge freezes the built-in registries before the mod constructors run, so the type has to be
     * created from a {@code RegisterEvent} - which is what NeoForge's {@code DeferredRegister} hooks.
     * 1.21.4 deleted {@code BlockEntityType.Builder} and left the type's own construction private,
     * so the value the deferred register stores is a plain {@code new BlockEntityType<>(...)}, which
     * works here because NeoForge re-opens that constructor. The Fabric side cannot use this path and
     * goes through {@code mixin.common.BlockEntityTypeInvoker} instead.
     */
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>>
            BLOCK_ENTITY_TYPES = net.neoforged.neoforge.registries.DeferredRegister.create(
                    net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, ParCool.MOD_ID);

    private static boolean blockEntityTypesRegistered;

    /**
     * {@code RegisterEvent} is an {@code IModBusEvent}, so the deferred register has to listen on the
     * mod's own bus. Called from the mod constructor, before any registration runs.
     */
    public void setModEventBus(net.neoforged.bus.api.IEventBus bus) {
        MOD_EVENT_BUS = bus;
    }

    private static net.neoforged.bus.api.IEventBus MOD_EVENT_BUS;

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> java.util.function.Supplier<
            net.minecraft.world.level.block.entity.BlockEntityType<T>> registerBlockEntityType(
            String name,
            net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier<T> factory,
            java.util.function.Supplier<net.minecraft.world.level.block.Block[]> blocks) {
        if (!blockEntityTypesRegistered) {
            blockEntityTypesRegistered = true;
            BLOCK_ENTITY_TYPES.register(MOD_EVENT_BUS);
        }
        return BLOCK_ENTITY_TYPES.register(name, () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(factory, blocks.get()));
    }

    @Override
    public boolean isBetterThirdPersonCameraDecoupled() {
        return com.alrex.parcool.extern.betterthirdperson.BetterThirdPersonManager.getInstance()
                .isCameraDecoupled();
    }

    @Override
    public IParCoolStaminaHandler newParagliderStaminaHandler(Player player) {
        return com.alrex.parcool.extern.paraglider.ParagliderManager.getInstance()
                .newParagliderStaminaHandlerFor(player);
    }

    @Override
    public boolean isUsingParagliderStamina(Parkourability parkourability) {
        return com.alrex.parcool.extern.paraglider.ParagliderManager.getInstance()
                .isUsingParagliderStamina(parkourability);
    }

    @Override
    public boolean isFallingWithParaglider(Player player) {
        return com.alrex.parcool.extern.paraglider.ParagliderManager.getInstance()
                .isFallingWithParaglider(player);
    }

    @Override
    public IParCoolStaminaHandler newEpicFightStaminaHandler(Player player) {
        return com.alrex.parcool.extern.epicfight.EpicFightManager.getInstance().newStaminaHandlerFor(player);
    }

    @Override
    public boolean isUsingEpicFightStamina(Parkourability parkourability) {
        return com.alrex.parcool.extern.epicfight.EpicFightManager.getInstance()
                .isUsingEpicFightStamina(parkourability);
    }

    @Override
    public Dodge.DodgeDirection handleDodgeCameraRotation(Dodge.DodgeDirection direction) {
        return com.alrex.parcool.extern.betterthirdperson.BetterThirdPersonManager.getInstance()
                .handleCustomCameraRotationForDodge(direction);
    }
}
