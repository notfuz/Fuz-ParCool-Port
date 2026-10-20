package com.alrex.parcool.common.stamina;

import com.alrex.parcool.common.data.ReadonlyStamina;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public interface IParCoolStaminaHandler {
    public ReadonlyStamina initializeStamina(LocalPlayer player, ReadonlyStamina current);

    public ReadonlyStamina consume(LocalPlayer player, ReadonlyStamina current, int value);

    public ReadonlyStamina recover(LocalPlayer player, ReadonlyStamina current, int value);

    public default ReadonlyStamina onTick(LocalPlayer player, ReadonlyStamina current) {
        return current;
    }

    public default boolean shouldShowHUD(LocalPlayer player) {
        return false;
    }

    public default boolean shouldImposeExhaustionPenalty(LocalPlayer player, ReadonlyStamina current) {
        return true;
    }

    public default void processOnServer(Player player, int value) {
    }

    public default boolean isExternalStamina() {
        return false;
    }
}
