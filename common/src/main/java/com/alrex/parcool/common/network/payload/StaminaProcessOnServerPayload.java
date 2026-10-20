package com.alrex.parcool.common.network.payload;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.network.ServerPayloadGuard;
import com.alrex.parcool.common.stamina.StaminaType;
import io.netty.buffer.ByteBuf;
import dev.architectury.networking.NetworkManager;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StaminaProcessOnServerPayload(StaminaType stamina, int value) implements CustomPacketPayload {
    public static final Type<StaminaProcessOnServerPayload> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "payload.custom_stamina"));
    public static final StreamCodec<ByteBuf, StaminaProcessOnServerPayload> CODEC = StreamCodec.composite(
            StaminaType.STREAM_CODEC,
            StaminaProcessOnServerPayload::stamina,
            ByteBufCodecs.VAR_INT,
            StaminaProcessOnServerPayload::value,
            StaminaProcessOnServerPayload::new
    );

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(StaminaProcessOnServerPayload payload, NetworkManager.PacketContext context) {
        throw new UnsupportedOperationException("This should have been designed not to be called");
    }

    public static void handleServer(StaminaProcessOnServerPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            if (!ServerPayloadGuard.allowStaminaProcess(player)) {
                ParCool.LOGGER.debug("Dropped stamina process packet from {}: over the rate limit",
                        player.getName().getString());
                return;
            }
            // Upstream let the client choose the StaminaType, i.e. which server-side handler gets
            // instantiated and what it is asked to do, and sent an unbounded amount. The type now has
            // to match what the server offers and the amount is clamped.
            StaminaType forced = ServerPayloadGuard.forcedStaminaType();
            if (!ServerPayloadGuard.isStaminaTypeAllowed(payload.stamina(), forced)) {
                ParCool.LOGGER.debug("Rejected stamina process request for type {} (server offers {})",
                        payload.stamina(), forced);
                return;
            }
            int value = ServerPayloadGuard.clampStaminaProcessValue(payload.value());
            payload.stamina().newHandler(player).processOnServer(player, value);
        });
    }
}
