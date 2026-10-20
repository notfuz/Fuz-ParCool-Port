package com.alrex.parcool.common.handlers;

import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.info.ClientSetting;
import com.alrex.parcool.common.network.payload.ClientInformationPayload;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.common.network.NetworkRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;


public class EnableOrDisableParCoolHandler {

    /**
     * Rising-edge latch for the enable binding.
     * <p>
     * Upstream used {@code KeyMapping#consumeClick()}, which is edge based because the NeoForge
     * {@code KeyModifier} path already fed the click counter. The portable modifier poll in
     * {@link KeyBindings#isKeyEnableParCoolDown()} is level based, so the edge is recovered here.
     */
    private static boolean enableKeyWasDown = false;

    public static void onTick() {
        boolean isDown = KeyBindings.isKeyEnableParCoolDown();
        boolean justPressed = isDown && !enableKeyWasDown;
        enableKeyWasDown = isDown;
        if (!justPressed) return;

        {
            boolean currentStatus = !ParCoolConfig.Client.Booleans.ParCoolIsActive.get();
            ParCoolConfig.Client.Booleans.ParCoolIsActive.set(currentStatus);
            // Upstream only called set() and never save(), so the toggle was lost on restart even
            // though the settings screens do persist the same key.
            ParCoolConfig.Client.Booleans.ParCoolIsActive.getInternalInstance().save();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            Parkourability parkourability = Parkourability.get(player);
            if (parkourability == null) return;
            parkourability.getActionInfo().setClientSetting(ClientSetting.readFromLocalConfig());
            NetworkRegistries.sendToServer(ClientInformationPayload.CODEC, new ClientInformationPayload(player.getUUID(), false, parkourability.getClientInfo()));
            player.displayClientMessage(Component.translatable(currentStatus ? "parcool.message.enabled" : "parcool.message.disabled"), true);
            if (currentStatus) {
                player.playSound(SoundEvents.PARCOOL_ENABLE.get(), 1.0f, 1.0f);
            } else {
                player.playSound(SoundEvents.PARCOOL_DISABLE.get(), 1.0f, 1.0f);
            }
        }
    }
}
