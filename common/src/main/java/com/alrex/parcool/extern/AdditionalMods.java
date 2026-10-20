package com.alrex.parcool.extern;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.data.client.LocalStamina;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import com.alrex.parcool.extern.shouldersurfing.ShoulderSurfingManager;
import com.alrex.parcool.platform.PlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point for the optional third-party integrations.
 *
 * <p>Upstream this was a four-valued enum of {@link ModManager}s. Paraglider, EpicFight and
 * BetterThirdPerson only ship NeoForge jars for 1.21.1, so their managers moved to the
 * {@code neoforge} module and are reached through {@link com.alrex.parcool.platform.ParCoolPlatform};
 * on Fabric the platform implementation reports "not installed" and ParCool degrades exactly the way
 * it does when the mod is simply absent. ShoulderSurfing is genuinely multiloader and stays here.
 */
public enum AdditionalMods {
    BETTER_THIRD_PERSON("betterthirdperson"),
    SHOULDER_SURFING("shouldersurfing"),
    PARAGLIDER("paraglider"),
    EPIC_FIGHT("epicfight");

    /** Only ShoulderSurfing is multiloader, so it is the only constant that owns a real manager. */
    private static final ShoulderSurfingManager SHOULDER_SURFING_MANAGER = new ShoulderSurfingManager();

    private final String modId;

    AdditionalMods(String modId) {
        this.modId = modId;
    }

    public void initMod() {
        if (this == SHOULDER_SURFING) {
            SHOULDER_SURFING_MANAGER.init();
        }
    }

    public void initModInClient() {
        if (this == SHOULDER_SURFING) {
            SHOULDER_SURFING_MANAGER.initInClient();
        }
    }

    public void initModInDedicatedServer() {
        if (this == SHOULDER_SURFING) {
            SHOULDER_SURFING_MANAGER.initInDedicatedServer();
        }
    }

    public boolean isModInstalled() {
        return this == SHOULDER_SURFING && SHOULDER_SURFING_MANAGER.isInstalled();
    }

    public String getModId() {
        return this.modId;
    }

    public static ShoulderSurfingManager shoulderSurfing() {
        return SHOULDER_SURFING_MANAGER;
    }

    // ------------------------------------------------------------------
    // cross-loader lookups
    // ------------------------------------------------------------------

    public static IParCoolStaminaHandler newParagliderStaminaHandler(Player player) {
        return PlatformServices.get().newParagliderStaminaHandler(player);
    }

    public static boolean isUsingParagliderStamina(Parkourability parkourability) {
        return PlatformServices.get().isUsingParagliderStamina(parkourability);
    }

    public static boolean isFallingWithParaglider(Player player) {
        return PlatformServices.get().isFallingWithParaglider(player);
    }

    public static IParCoolStaminaHandler newEpicFightStaminaHandler(Player player) {
        return PlatformServices.get().newEpicFightStaminaHandler(player);
    }

    public static boolean isUsingEpicFightStamina(Parkourability parkourability) {
        return PlatformServices.get().isUsingEpicFightStamina(parkourability);
    }

    /**
     * Was {@code betterThirdPerson().handleCustomCameraRotationForDodge(direction)} followed by
     * {@code shoulderSurfing().handleCustomCameraRotationForDodge(direction)}. BetterThirdPerson is
     * NeoForge only and now goes through the platform seam; ShoulderSurfing stays local.
     */
    public static Dodge.DodgeDirection handleDodgeCameraRotation(Dodge.DodgeDirection direction) {
        Dodge.DodgeDirection result = PlatformServices.get().handleDodgeCameraRotation(direction);
        return SHOULDER_SURFING_MANAGER.handleCustomCameraRotationForDodge(result);
    }

    public static boolean isCameraDecoupled() {
        return SHOULDER_SURFING_MANAGER.isCameraDecoupled()
                || PlatformServices.get().isBetterThirdPersonCameraDecoupled();
    }

    public static boolean isUsingExternalStamina() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? false : LocalStamina.get(player).isUsingExternalStamina();
    }

    public static void init() {
        for (AdditionalMods mod : values()) {
            mod.initMod();
        }
    }

    public static void initInClient() {
        for (AdditionalMods mod : values()) {
            mod.initModInClient();
        }
    }

    public static void initInDedicatedServer() {
        for (AdditionalMods mod : values()) {
            mod.initModInDedicatedServer();
        }
    }
}
