package com.alrex.parcool.api;

import com.alrex.parcool.ParCool;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.DeferredRegister;

public class SoundEvents {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ParCool.MOD_ID, Registries.SOUND_EVENT);
    private static final SoundEvent VAULT_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.vault"));
    private static final SoundEvent VERTICAL_WALL_RUN_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.v_wall_run"));
    private static final SoundEvent HORIZONTAL_WALL_RUN_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.h_wall_run"));
    private static final SoundEvent BREAKFALL_JUST_TIME_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.breakfall.just"));
    private static final SoundEvent ROLL_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.breakfall.roll"));
    private static final SoundEvent SAFETY_TAP_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.breakfall.tap"));
    private static final SoundEvent CATLEAP_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.catleap"));
    private static final SoundEvent CHARGE_JUMP_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.charge_jump"));
    private static final SoundEvent WALL_JUMP_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.wall_jump"));
    private static final SoundEvent CLING_TO_CLIFF_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.cling_to_cliff.grab"));
    private static final SoundEvent CLING_TO_CLIFF_JUMP_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.cling_to_cliff.jump"));
    private static final SoundEvent HANG_DOWN_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.hang_down.grab"));
    private static final SoundEvent HANG_DOWN_JUMP_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.hang_down.jump"));
    private static final SoundEvent SLIDE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.slide"));
    private static final SoundEvent DODGE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "action.dodge"));
    private static final SoundEvent PARCOOL_ENABLE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "parcool.enable"));
    private static final SoundEvent PARCOOL_DISABLE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "parcool.disable"));
    private static final SoundEvent ZIPLINE_SET_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "zipline.set"));
    private static final SoundEvent ZIPLINE_REMOVE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "zipline.remove"));
    private static final SoundEvent ZIPLINE_RIDE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "zipline.ride"));

    public static final RegistrySupplier<SoundEvent> VAULT = SOUNDS.register("action.vault", () -> VAULT_SOUND);
    public static final RegistrySupplier<SoundEvent> VERTICAL_WALL_RUN = SOUNDS.register("action.v_wall_run", () -> VERTICAL_WALL_RUN_SOUND);
    public static final RegistrySupplier<SoundEvent> HORIZONTAL_WALL_RUN = SOUNDS.register("action.h_wall_run", () -> HORIZONTAL_WALL_RUN_SOUND);
    public static final RegistrySupplier<SoundEvent> BREAKFALL_JUST_TIME = SOUNDS.register("action.breakfall.just", () -> BREAKFALL_JUST_TIME_SOUND);
    public static final RegistrySupplier<SoundEvent> ROLL = SOUNDS.register("action.breakfall.roll", () -> ROLL_SOUND);
    public static final RegistrySupplier<SoundEvent> SAFETY_TAP = SOUNDS.register("action.breakfall.tap", () -> SAFETY_TAP_SOUND);
    public static final RegistrySupplier<SoundEvent> CATLEAP = SOUNDS.register("action.catleap", () -> CATLEAP_SOUND);
    public static final RegistrySupplier<SoundEvent> CHARGE_JUMP = SOUNDS.register("action.charge_jump", () -> CHARGE_JUMP_SOUND);
    public static final RegistrySupplier<SoundEvent> WALL_JUMP = SOUNDS.register("action.wall_jump", () -> WALL_JUMP_SOUND);
    public static final RegistrySupplier<SoundEvent> CLING_TO_CLIFF = SOUNDS.register("action.cling_to_cliff.grab", () -> CLING_TO_CLIFF_SOUND);
    public static final RegistrySupplier<SoundEvent> CLING_TO_CLIFF_JUMP = SOUNDS.register("action.cling_to_cliff.jump", () -> CLING_TO_CLIFF_JUMP_SOUND);
    public static final RegistrySupplier<SoundEvent> HANG_DOWN = SOUNDS.register("action.hang_down.grab", () -> HANG_DOWN_SOUND);
    public static final RegistrySupplier<SoundEvent> HANG_DOWN_JUMP = SOUNDS.register("action.hang_down.jump", () -> HANG_DOWN_JUMP_SOUND);
    public static final RegistrySupplier<SoundEvent> SLIDE = SOUNDS.register("action.slide", () -> SLIDE_SOUND);
    public static final RegistrySupplier<SoundEvent> DODGE = SOUNDS.register("action.dodge", () -> DODGE_SOUND);
    public static final RegistrySupplier<SoundEvent> PARCOOL_ENABLE = SOUNDS.register("parcool.enable", () -> PARCOOL_ENABLE_SOUND);
    public static final RegistrySupplier<SoundEvent> PARCOOL_DISABLE = SOUNDS.register("parcool.disable", () -> PARCOOL_DISABLE_SOUND);
    public static final RegistrySupplier<SoundEvent> ZIPLINE_SET = SOUNDS.register("zipline.set", () -> ZIPLINE_SET_SOUND);
    public static final RegistrySupplier<SoundEvent> ZIPLINE_REMOVE = SOUNDS.register("zipline.remove", () -> ZIPLINE_REMOVE_SOUND);
    /** Looped while the player is actually moving along a rope; see client.sound.ZiplineRideSound. */
    public static final RegistrySupplier<SoundEvent> ZIPLINE_RIDE = SOUNDS.register("zipline.ride", () -> ZIPLINE_RIDE_SOUND);

    public static void registerAll() {
        SOUNDS.register();
    }
}
