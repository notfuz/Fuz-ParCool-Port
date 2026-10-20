package com.alrex.parcool.common.data.client;

import com.alrex.parcool.api.Effects;
import com.alrex.parcool.common.data.ParCoolDataKeys;
import com.alrex.parcool.common.data.client.ClientDataKeys;
import com.alrex.parcool.client.stamina.StaminaOps;
import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.common.stamina.IParCoolStaminaHandler;
import com.alrex.parcool.common.stamina.StaminaType;
import com.alrex.parcool.common.stamina.handlers.InfiniteStaminaHandler;
import net.minecraft.client.player.LocalPlayer;

import javax.annotation.Nullable;

public class LocalStamina {
    @Nullable
    private StaminaType currentType = null;
    @Nullable
    private IParCoolStaminaHandler handler = null;

    public static LocalStamina get(LocalPlayer player) {
        return ClientDataKeys.getLocalStamina(player);
    }

    public boolean isAvailable() {
        return handler != null && currentType != null;
    }

    public boolean isInfinite(LocalPlayer player) {
        return player.isCreative() || player.isSpectator() || handler instanceof InfiniteStaminaHandler;
    }

    public void changeType(LocalPlayer player, StaminaType type) {
        currentType = type;
        handler = type.newHandler(player);
        ParCoolDataKeys.setStamina(player, handler.initializeStamina(player, ParCoolDataKeys.getStamina(player)));
    }

    @Nullable
    public IParCoolStaminaHandler getHandler() {
        return handler;
    }

    public boolean isExhausted(LocalPlayer player) {
        return ParCoolDataKeys.getStamina(player).isExhausted();
    }

    public int getValue(LocalPlayer player) {
        return ParCoolDataKeys.getStamina(player).value();
    }

    public int getMax(LocalPlayer player) {
        return ParCoolDataKeys.getStamina(player).max();
    }

    public void consume(LocalPlayer player, int value) {
        if (player.isCreative() || player.isSpectator()) return;
        if (handler == null) return;
        if (isInfinite(player)) return;
        if (player.hasEffect(Effects.INEXHAUSTIBLE)) return;
        ParCoolDataKeys.setStamina(player,
                handler.consume(player, ParCoolDataKeys.getStamina(player), value)
        );
    }

    public void recover(LocalPlayer player, int value) {
        if (player.isCreative() || player.isSpectator()) return;
        if (handler == null) return;
        ParCoolDataKeys.setStamina(player,
                handler.recover(player, ParCoolDataKeys.getStamina(player), value)
        );
    }

    public void onTick(LocalPlayer player) {
        if (handler == null) return;
        ParCoolDataKeys.setStamina(player,
                handler.onTick(player, ParCoolDataKeys.getStamina(player))
        );
    }

    public boolean shouldShowHUD(LocalPlayer player) {
        if (handler == null) return false;
        return handler.shouldShowHUD(player);
    }

    public boolean imposeExhaustionPenalty(LocalPlayer player) {
        if (handler == null) return false;
        var current = ParCoolDataKeys.getStamina(player);
        return current.isExhausted() && handler.shouldImposeExhaustionPenalty(player, current);
    }

    private ReadonlyStamina oldStamina = ReadonlyStamina.createDefault();
    public void sync(LocalPlayer player) {
        ReadonlyStamina stamina = ParCoolDataKeys.getStamina(player);
        if (!stamina.equals(oldStamina)) {
            StaminaOps.sync(player, stamina);
        }
        oldStamina = stamina;
    }

    public boolean isUsingExternalStamina() {
        return handler != null && handler.isExternalStamina();
    }
}
