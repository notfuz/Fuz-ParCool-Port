package com.alrex.parcool.api;


import com.alrex.parcool.common.data.ParCoolDataKeys;
import com.alrex.parcool.common.data.client.LocalStamina;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public class Stamina {
    public static Stamina get(Player player) {
		return new Stamina(player);
	}

	private final Player player;

	private Stamina(Player player) {
		this.player = player;
	}

	public int getMaxValue() {
		return ParCoolDataKeys.getStamina(player).max();
	}

	public int getValue() {
		return ParCoolDataKeys.getStamina(player).value();
	}

	public boolean isExhausted() {
		return ParCoolDataKeys.getStamina(player).isExhausted();
	}

	public void consume(int value) {
		if (!(player instanceof LocalPlayer localPlayer)) return;
		var stamina = LocalStamina.get(localPlayer);
		stamina.consume(localPlayer, value);
	}

	public void recover(int value) {
		if (!(player instanceof LocalPlayer localPlayer)) return;
		var stamina = LocalStamina.get(localPlayer);
		stamina.recover(localPlayer, value);
	}
}
