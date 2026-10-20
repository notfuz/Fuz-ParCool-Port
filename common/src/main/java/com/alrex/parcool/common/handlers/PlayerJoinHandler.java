package com.alrex.parcool.common.handlers;

import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.info.ClientSetting;
import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.common.network.payload.ClientInformationPayload;
import net.minecraft.client.player.LocalPlayer;

public class PlayerJoinHandler {
    /**
     * Was {@code EntityJoinLevelEvent} on the client. The handler only ever acted on the local
     * player, which is exactly what {@code ClientPlayerEvent.CLIENT_PLAYER_JOIN} reports.
     */
    public static void onClientPlayerJoin(LocalPlayer player) {
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) return;
        parkourability.getActionInfo().setClientSetting(ClientSetting.readFromLocalConfig());
        NetworkRegistries.sendToServer(ClientInformationPayload.CODEC, new ClientInformationPayload(player.getUUID(), true, parkourability.getClientInfo()));
    }
}
