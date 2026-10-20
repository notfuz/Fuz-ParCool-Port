package com.alrex.parcool.extern;

import com.alrex.parcool.api.client.gui.ParCoolHUDEvent;
import com.alrex.parcool.api.event.ParCoolEventBus;
import net.minecraft.client.Minecraft;

public class AdditionalModsEventConsumer {
    public static class Client {
        public static void register() {
            ParCoolEventBus.hudEvents().register(AdditionalModsEventConsumer.Client::onHUDRender);
        }

        public static void onHUDRender(ParCoolHUDEvent event) {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if (AdditionalMods.isUsingExternalStamina()) {
                event.setCanceled(true);
            }
        }
    }
}
