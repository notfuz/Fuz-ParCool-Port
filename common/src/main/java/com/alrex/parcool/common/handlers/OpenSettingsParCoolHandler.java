package com.alrex.parcool.common.handlers;

import com.alrex.parcool.client.gui.SettingActionLimitationScreen;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class OpenSettingsParCoolHandler {
    public static void onTick() {

		if (KeyRecorder.keyOpenSettingsState.isPressed()) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) return;
			Parkourability parkourability = Parkourability.get(player);
			if (parkourability == null) return;
			Minecraft.getInstance().setScreen(new SettingActionLimitationScreen(Component.literal("ParCool Setting"), parkourability.getActionInfo(), ParCoolConfig.Client.getInstance().GUIColorTheme.get()));
		}
	}
}
