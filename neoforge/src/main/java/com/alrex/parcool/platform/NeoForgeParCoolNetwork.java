package com.alrex.parcool.platform;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * NeoForge network plumbing, built on the raw {@code NetworkManager}.
 *
 * <p>{@code NetworkChannel} cannot be used here: it registers a C2S and an S2C receiver under one id,
 * and NeoForge 21.1 refuses the second registration. With the raw API a bidirectional message is
 * simply registered twice under two different ids, which {@code NetworkRegistries} does. Sending
 * therefore addresses the message by its explicit id rather than by channel lookup.
 *
 * <h2>Why registration goes through the id overload</h2>
 * Architectury has two {@code NetworkManager.registerReceiver} overloads, and only the id-based one is
 * usable together with the id-based {@code sendTo*(id, buf)} that this class uses:
 * <ul>
 *     <li>the <b>id-based</b> one fills {@code NetworkAggregator.C2S_TYPE} / {@code S2C_TYPE} with
 *     {@code id -> new Type<BufCustomPacketPayload>(id)}, and the id-based send reads the type from
 *     exactly that map;</li>
 *     <li>the <b>Type-based</b> one only fills {@code C2S_CODECS} / {@code S2C_RECEIVER} /
 *     {@code C2S_TRANSFORMERS} and leaves the {@code *_TYPE} maps empty.</li>
 * </ul>
 * Registering the Type-based way and sending by id therefore produced
 * {@code NullPointerException: Cannot invoke "CustomPacketPayload$Type.id()" because "type" is null}
 * inside {@code NetworkAggregator.collectPackets} on the very first client -&gt; server packet, which
 * ParCool sends from {@code PlayerJoinHandler} during login. The exception propagates out of
 * {@code ClientboundLoginPacket#handle}, and NeoForge answers by dropping the connection with
 * "Network Protocol Error" - so on NeoForge the whole C2S direction (stamina reports, action sync,
 * the client-settings packet) was dead. It stayed invisible because the dedicated-server test never
 * had a client connect, and an integrated server does not send its own login packet.
 */
public class NeoForgeParCoolNetwork implements ParCoolNetwork {

    @Override
    public <T extends CustomPacketPayload> void register(
            Class<T> payloadClass,
            CustomPacketPayload.Type<T> type,
            ResourceLocation wireId,
            StreamCodec<?, T> codec,
            boolean clientbound,
            BiConsumer<T, NetworkManager.PacketContext> handler) {
        // The id overload hands the raw buffer to the receiver and takes care of the C2S_TYPE/S2C_TYPE
        // bookkeeping that the id-based send below depends on. Architectury's own id overload erases
        // the payload type to BufCustomPacketPayload, so the message is decoded here instead - the
        // payload codecs declare ByteBuf as their supertype, so the encoded bytes are unchanged.
        @SuppressWarnings({"unchecked", "rawtypes"})
        StreamCodec<? super RegistryFriendlyByteBuf, T> erased = (StreamCodec) codec;
        NetworkManager.registerReceiver(
                clientbound ? NetworkManager.Side.S2C : NetworkManager.Side.C2S,
                wireId,
                (buf, context) -> {
                    // The decode has to happen here, on the network thread, while the buffer is still
                    // alive: registerReceiver hands the lambda a raw buffer and releases it as soon as
                    // the lambda returns. Inside context.queue(...) the buffer is already refCnt 0, and
                    // any VarLong.read() then kills the thread with IllegalReferenceCountException. Only
                    // the handler belongs in the queue, for thread safety, not the decoding.
                    T payload = erased.decode((RegistryFriendlyByteBuf) buf);
                    context.queue(() -> handler.accept(payload, context));
                }
        );
    }

    @Override
    public <T extends CustomPacketPayload> void sendToServer(
            T payload, ResourceLocation wireId, StreamCodec<?, T> codec) {
        NetworkManager.sendToServer(wireId, ParCoolNetwork.encode(codec, payload));
    }

    @Override
    public <T extends CustomPacketPayload> void sendToPlayer(
            ServerPlayer player, T payload, ResourceLocation wireId, StreamCodec<?, T> codec) {
        NetworkManager.sendToPlayer(player, wireId, ParCoolNetwork.encode(codec, payload));
    }

    @Override
    public <T extends CustomPacketPayload> void sendToPlayers(
            Iterable<ServerPlayer> players, T payload, ResourceLocation wireId, StreamCodec<?, T> codec) {
        NetworkManager.sendToPlayers(players, wireId, ParCoolNetwork.encode(codec, payload));
    }
}
