package com.alrex.parcool.common.stamina.handlers;

import com.alrex.parcool.client.stamina.StaminaOps;
import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import net.minecraft.client.player.LocalPlayer;

public class ParCoolStaminaHandler implements IParCoolStaminaHandler {
    private int recoveryCoolDown = 0;

    @Override
    public ReadonlyStamina initializeStamina(LocalPlayer player, ReadonlyStamina current) {
        return current;
    }

    @Override
    public ReadonlyStamina consume(LocalPlayer player, ReadonlyStamina current, int value) {
        recoveryCoolDown = 30;
        return current.consumed(value);
    }

    @Override
    public ReadonlyStamina recover(LocalPlayer player, ReadonlyStamina current, int value) {
        return current.recovered(value);
    }

    @Override
    public ReadonlyStamina onTick(LocalPlayer player, ReadonlyStamina current) {
        if (recoveryCoolDown > 0) {
            recoveryCoolDown--;
        }
        // The two LocalPlayer member calls live in StaminaOps: this class is instantiated on a
        // dedicated server through the StaminaType factories, and an in-body call on a client-only
        // receiver would make NeoForge's RuntimeDistCleaner abort the server.
        current = StaminaOps.updateMax(player, current);
        return StaminaOps.recoverTick(player, current, recoveryCoolDown);
    }

    @Override
    public boolean shouldShowHUD(LocalPlayer player) {
        return true;
    }
}
