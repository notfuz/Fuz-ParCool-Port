package com.alrex.parcool.extern.epicfight;

import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import com.alrex.parcool.common.stamina.StaminaType;
import com.alrex.parcool.common.stamina.handlers.ParCoolStaminaHandler;
import com.alrex.parcool.extern.ModManager;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import javax.annotation.Nullable;

public class EpicFightManager extends ModManager {
    public EpicFightManager() {
        super("epicfight");
    }

    private static EpicFightManager instance;

    public static EpicFightManager getInstance() {
        if (instance == null) instance = new EpicFightManager();
        return instance;
    }

    public IParCoolStaminaHandler newStaminaHandlerFor(Player player) {
        // EpicFight has no 1.21.4 build, so a client that asks for EPIC_FIGHT stamina reaches this
        // without EpicFight on the classpath. Handing out the handler would then NoClassDefFoundError as
        // soon as its getPlayerPatch() signature is resolved.
        if (isEpicFightUsable() && isUsingEpicFightStamina(Parkourability.get(player))) {
            return new EpicFightStaminaHandler();
        }
        return new ParCoolStaminaHandler();
    }

    /** True only when EpicFight's own classes are actually loadable, not merely advertised. */
    public static boolean isEpicFightUsable() {
        try {
            Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities", false,
                    EpicFightManager.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Nullable
    PlayerPatch<?> getPlayerPatch(Player player) {
        if (!isInstalled() || !isEpicFightUsable()) {
            return null;
        }
        return EpicFightCapabilities.getPlayerPatch(player);
    }

    public boolean isUsingEpicFightStamina(Parkourability parkourability) {
        if (!isInstalled() || !isEpicFightUsable()) return false;
        var forcedStamina = parkourability.getServerLimitation().getForcedStamina();
        if (forcedStamina == StaminaType.EPIC_FIGHT) return true;
        return forcedStamina == StaminaType.NONE && parkourability.getClientInfo().getRequestedStamina() == StaminaType.EPIC_FIGHT;
    }
}
