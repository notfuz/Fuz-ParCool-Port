package com.alrex.parcool.common.network.payload;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.action.impl.BreakfallReady;
import com.alrex.parcool.common.data.Parkourability;
import io.netty.buffer.ByteBuf;
import dev.architectury.networking.NetworkManager;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StartBreakfallEventPayload(boolean justTimed) implements CustomPacketPayload {
    public static final Type<StartBreakfallEventPayload> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "payload.start_breakfall_event"));
    public static final StreamCodec<ByteBuf, StartBreakfallEventPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            StartBreakfallEventPayload::justTimed,
            StartBreakfallEventPayload::new
    );

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(StartBreakfallEventPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            Parkourability parkourability = Parkourability.get(player);
            parkourability.get(BreakfallReady.class).startBreakfall(player, parkourability, payload.justTimed());
        });
    }

    public static void handleServer(StartBreakfallEventPayload payload, NetworkManager.PacketContext context) {
        throw new UnsupportedOperationException("This should have been designed not to be called");
    }
}
