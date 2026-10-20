package com.alrex.parcool.extern.epicfight;

import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.common.network.payload.StaminaProcessOnServerPayload;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import com.alrex.parcool.common.stamina.StaminaType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;


import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.common.network.ServerPayloadGuard;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import javax.annotation.Nullable;

public class EpicFightStaminaHandler implements IParCoolStaminaHandler {
    private float consumed = 0;

    private ReadonlyStamina readCurrentStamina(Player player, @Nullable ReadonlyStamina current) {
        var patch = EpicFightManager.getInstance().getPlayerPatch(player);
        if (patch == null) {
            if (current == null) return ReadonlyStamina.createDefault();
            else return current;
        }

        return new ReadonlyStamina(
                !patch.hasStamina(1),
                (int) patch.getStamina(),
                (int) patch.getMaxStamina()
        );
    }

    @Override
    public ReadonlyStamina initializeStamina(LocalPlayer player, ReadonlyStamina current) {
        return readCurrentStamina(player, current);
    }

    @Override
    public ReadonlyStamina consume(LocalPlayer player, ReadonlyStamina current, int value) {
        consumed += value / 60f;
        return current;
    }

    @Override
    public ReadonlyStamina recover(LocalPlayer player, ReadonlyStamina current, int value) {
        consumed -= value / 60f;
        return current;
    }

    @Override
    public ReadonlyStamina onTick(LocalPlayer player, ReadonlyStamina current) {
        if (consumed != 0) {
            // Clamped to the same bound the server applies, in the same units, so the value the client
            // reports is the value the server records. Before this the accumulator could exceed
            // MAX_STAMINA_PROCESS_VALUE and be silently truncated server side, and a negative value was
            // clamped to zero, so the two sides drifted apart on stamina without any visible error.
            int value = ServerPayloadGuard.clampStaminaProcessValue((int) (consumed * 2048));
            consumed = 0;
            NetworkRegistries.sendToServer(StaminaProcessOnServerPayload.CODEC, new StaminaProcessOnServerPayload(StaminaType.EPIC_FIGHT, value));
        }
        return readCurrentStamina(player, current);
    }

    @Override
    public boolean shouldImposeExhaustionPenalty(LocalPlayer player, ReadonlyStamina current) {
        return false;
    }

    @Override
    public void processOnServer(Player player, int value) {
        float consumedValue = value / 2048f;
        PlayerPatch<?> patch = EpicFightManager.getInstance().getPlayerPatch(player);
        if (patch == null) return;
        patch.resetActionTick();
        patch.setStamina(patch.getStamina() - consumedValue);
    }

    @Override
    public boolean isExternalStamina() {
        return true;
    }
}
