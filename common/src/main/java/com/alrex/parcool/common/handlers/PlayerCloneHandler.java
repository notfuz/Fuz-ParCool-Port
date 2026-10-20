package com.alrex.parcool.common.handlers;

import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

public class PlayerCloneHandler {
	public static void onClone(Player original, Player clone, boolean wasDeath) {
        if (wasDeath && clone instanceof ServerPlayer) {
            Parkourability pFrom = Parkourability.get(original);
            Parkourability pTo = Parkourability.get(clone);
            if (pFrom != null && pTo != null) {
                pTo.CopyFrom(pFrom);
            }
        }
    }
}
