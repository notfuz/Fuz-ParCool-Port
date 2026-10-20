package com.alrex.parcool.common.handlers;

import com.alrex.parcool.common.action.impl.ChargeJump;
import com.alrex.parcool.common.action.impl.Dive;
import com.alrex.parcool.common.action.impl.Flipping;
import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.world.entity.player.Player;

public class PlayerJumpHandler {
	/**
	 * Was {@code LivingEvent.LivingJumpEvent}. Dispatched from
	 * {@code mixin.common.PlayerMixin#jumpFromGround} <b>TAIL</b>, i.e. after
	 * {@code super.jumpFromGround()} has set the launch velocity. That is deliberately not the point
	 * NeoForge fires the event from (HEAD of {@code LivingEntity#jumpFromGround}): NeoForge 21.1 makes
	 * the jump absolute ({@code setDeltaMovement(x, getJumpPower(), z)}) where vanilla adds to the
	 * current motion, so anything added before it is thrown away on NeoForge. See the mixin for the
	 * measurement that pinned this down.
	 */
	public static void onJump(Player player) {
		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) return;
		parkourability.getAdditionalProperties().onJump();
		if (!player.isLocalPlayer()) return;
		parkourability.get(Dive.class).onJump(player, parkourability);
		parkourability.get(Flipping.class).onJump(player, parkourability);
		parkourability.get(ChargeJump.class).onJump(player, parkourability);
	}
}
