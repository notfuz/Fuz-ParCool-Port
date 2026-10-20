package com.alrex.parcool.extern.paraglider;

import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;


import tictim.paraglider.api.stamina.Stamina;

public class ParagliderStaminaHandler implements IParCoolStaminaHandler {
    private Stamina getInternalInstance(Player player) {
        return Stamina.get(player);
    }

    @Override
    public ReadonlyStamina initializeStamina(LocalPlayer player, ReadonlyStamina current) {
        var stamina = getInternalInstance(player);
        return new ReadonlyStamina(false, (int) stamina.stamina(), (int) Math.ceil(stamina.maxStamina()));
    }

    @Override
    public ReadonlyStamina consume(LocalPlayer player, ReadonlyStamina current, int value) {
        return current;
    }

    @Override
    public ReadonlyStamina recover(LocalPlayer player, ReadonlyStamina current, int value) {
        return current;
    }

    @Override
    public ReadonlyStamina onTick(LocalPlayer player, ReadonlyStamina current) {
        var stamina = getInternalInstance(player);
        return new ReadonlyStamina(
                stamina.isDepleted(),
                (int) stamina.stamina(),
                (int) Math.ceil(stamina.maxStamina())
        );
    }

    @Override
    public boolean isExternalStamina() {
        return true;
    }
}
