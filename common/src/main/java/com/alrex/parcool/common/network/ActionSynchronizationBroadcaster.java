package com.alrex.parcool.common.network;

import com.alrex.parcool.common.network.payload.ActionStateBroadcastPayload;
import com.alrex.parcool.common.network.payload.ActionStatePayload;


import java.util.ArrayList;
import java.util.List;

public class ActionSynchronizationBroadcaster {
    private static List<ActionStatePayload> pendingPayloads = new ArrayList<>();

    /// This have to be called from same thread as game ticking in logical server
    public static void add(ActionStatePayload payload) {
        pendingPayloads.addLast(payload);
    }

    private static void send() {
        if (pendingPayloads.isEmpty()) return;
        NetworkRegistries.sendToAllPlayers(ActionStateBroadcastPayload.CODEC, new ActionStateBroadcastPayload(pendingPayloads));
        pendingPayloads = new ArrayList<>();
    }

    public static void onTick() {
        send();
    }
}
