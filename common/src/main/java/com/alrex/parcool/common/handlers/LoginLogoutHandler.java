package com.alrex.parcool.common.handlers;

import com.alrex.parcool.server.limitation.Limitations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class LoginLogoutHandler {
    public static void onQuit(Player player) {
        if (player instanceof ServerPlayer) {
            Limitations.unload(player.getUUID());
        }
    }
}
