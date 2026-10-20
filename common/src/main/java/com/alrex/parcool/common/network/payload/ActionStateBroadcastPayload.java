package com.alrex.parcool.common.network.payload;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.network.ListStreamCodec;
import io.netty.buffer.ByteBuf;
import dev.architectury.networking.NetworkManager;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

public record ActionStateBroadcastPayload(List<ActionStatePayload> payloads) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionStateBroadcastPayload> TYPE
            = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "payload.action_state.broadcast"));

    public static final StreamCodec<ByteBuf, ActionStateBroadcastPayload> CODEC = StreamCodec.composite(
            new ListStreamCodec<>(ActionStatePayload.CODEC),
            ActionStateBroadcastPayload::payloads,
            ActionStateBroadcastPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ActionStateBroadcastPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player;
            Level world = context.getPlayer().level();
            for (var statePayload : payload.payloads()) {
                player = world.getPlayerByUUID(statePayload.playerID());
                // `continue`, not `return`: upstream aborted the whole batch as soon as one UUID was
                // unresolvable (entity still loading) or referred to the local player, which silently
                // dropped every other player's action state in the same packet.
                if (player == null || player.isLocalPlayer()) continue;

                statePayload.processPlayer(player);
            }
        });
    }
}
