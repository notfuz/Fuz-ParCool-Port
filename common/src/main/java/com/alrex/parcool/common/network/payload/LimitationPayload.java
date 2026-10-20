package com.alrex.parcool.common.network.payload;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.data.client.LocalStamina;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.info.ServerLimitation;
import io.netty.buffer.ByteBuf;
import dev.architectury.networking.NetworkManager;

import javax.annotation.Nonnull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

public record LimitationPayload(ServerLimitation limitation) implements CustomPacketPayload {
    public static final Type<LimitationPayload> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "payload.limitation"));
    public static final StreamCodec<ByteBuf, LimitationPayload> CODEC = StreamCodec.composite(
            ServerLimitation.STREAM_CODEC,
            LimitationPayload::limitation,
            LimitationPayload::new
    );

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(LimitationPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            var player = context.getPlayer();
            Parkourability parkourability = Parkourability.get(player);
            parkourability.getActionInfo().setServerLimitation(payload.limitation());
            if (player instanceof LocalPlayer localPlayer) {
                parkourability.getActionInfo().updateStaminaType(LocalStamina.get(localPlayer), localPlayer);
            }
        });
    }

    public static void handleServer(LimitationPayload payload, NetworkManager.PacketContext context) {
        throw new UnsupportedOperationException("This should have been designed not to be called");
    }
}
