package com.alrex.parcool.common.network;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.network.payload.ActionStateBroadcastPayload;
import com.alrex.parcool.common.network.payload.ActionStatePayload;
import com.alrex.parcool.common.network.payload.ClientInformationPayload;
import com.alrex.parcool.common.network.payload.LimitationPayload;
import com.alrex.parcool.common.network.payload.StaminaBroadcastPayload;
import com.alrex.parcool.common.network.payload.StaminaPayload;
import com.alrex.parcool.common.network.payload.StaminaProcessOnServerPayload;
import com.alrex.parcool.common.network.payload.StartBreakfallEventPayload;
import com.alrex.parcool.platform.PlatformServices;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Loader-agnostic replacement for the NeoForge {@code PayloadRegistrar} /
 * {@code PacketDistributor} pair, built on Architectury's {@link NetworkManager}.
 *
 * <h2>Why not {@code NetworkChannel}</h2>
 * {@code NetworkChannel#register} derives the wire id from a hash of the message <i>class</i> and then
 * registers a C2S <i>and</i> an S2C receiver for it. Architectury's NeoForge backend maps each of
 * those to a separate {@code PayloadRegistrar.playTo…} call, and NeoForge 21.1 refuses to register
 * the same payload id twice ("Cannot register payload parcool:main/… as it is already registered").
 * A message that travels in both directions therefore needs <b>one id per direction</b>, which is
 * what this class does.
 *
 * <h2>Compatibility</h2>
 * The payloads are plain 1.21.1 {@link CustomPacketPayload} records and their codecs are untouched,
 * so the encoded bytes are identical to upstream's. The ids keep the upstream
 * {@code parcool:payload.*} names for the server-&gt;client direction; the client-&gt;server direction
 * of the three bidirectional messages uses the same name with a {@code .c2s} suffix. NeoForge's
 * channel version negotiation ({@code "3.3.0.0"}) has no Architectury equivalent, so compatibility is
 * enforced by the ids: a mismatched jar is rejected with an unknown-payload disconnect.
 */
public final class NetworkRegistries {

    /**
     * Architectury's channel has no "every player" primitive, so the broadcasting entry point needs
     * the player list. NeoForge's {@code PacketDistributor.sendToAllPlayers} obtained it implicitly;
     * the server is captured from the Architectury lifecycle events instead.
     */
    @Nullable
    private static MinecraftServer server;

    private static boolean payloadsRegistered;

    private NetworkRegistries() {
    }

    /** Called from the Architectury server lifecycle events. */
    public static void setServer(@Nullable MinecraftServer server) {
        NetworkRegistries.server = server;
    }

    // ------------------------------------------------------------------
    // registration
    // ------------------------------------------------------------------

    private static <T extends CustomPacketPayload> ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, path);
    }

    private static ResourceLocation serverbound(String path) {
        return ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, path + ".c2s");
    }

    /** Client -&gt; server message. */
    private static <T extends CustomPacketPayload> void registerC2S(
            Class<T> payloadClass,
            StreamCodec<? super FriendlyByteBuf, T> codec,
            CustomPacketPayload.Type<T> type,
            ResourceLocation wireId,
            BiConsumer<T, NetworkManager.PacketContext> handler) {
        PlatformServices.get().getNetwork().register(payloadClass, type, wireId, codec, false, handler);
    }

    /** Server -&gt; client message. */
    private static <T extends CustomPacketPayload> void registerS2C(
            Class<T> payloadClass,
            StreamCodec<? super FriendlyByteBuf, T> codec,
            CustomPacketPayload.Type<T> type,
            ResourceLocation wireId,
            BiConsumer<T, NetworkManager.PacketContext> handler) {
        PlatformServices.get().getNetwork().register(payloadClass, type, wireId, codec, true, handler);
    }

    public static void registerPayloads() {
        if (payloadsRegistered) return;
        payloadsRegistered = true;

        // --- client -> server only ---
        registerC2S(StaminaProcessOnServerPayload.class, StaminaProcessOnServerPayload.CODEC, StaminaProcessOnServerPayload.TYPE,
                serverbound("payload.custom_stamina"), StaminaProcessOnServerPayload::handleServer);

        // --- server -> client only ---
        registerS2C(StartBreakfallEventPayload.class, StartBreakfallEventPayload.CODEC, StartBreakfallEventPayload.TYPE,
                id("payload.start_breakfall_event"), StartBreakfallEventPayload::handleClient);
        registerS2C(LimitationPayload.class, LimitationPayload.CODEC, LimitationPayload.TYPE,
                id("payload.limitation"), LimitationPayload::handleClient);
        registerS2C(StaminaBroadcastPayload.class, StaminaBroadcastPayload.CODEC, StaminaBroadcastPayload.TYPE,
                id("payload.stamina.broadcast"), StaminaBroadcastPayload::handleClient);
        registerS2C(ActionStateBroadcastPayload.class, ActionStateBroadcastPayload.CODEC, ActionStateBroadcastPayload.TYPE,
                id("payload.action_state.broadcast"), ActionStateBroadcastPayload::handleClient);

        // --- both directions; one wire id per direction ---
        registerC2S(ActionStatePayload.class, ActionStatePayload.CODEC, ActionStatePayload.TYPE,
                serverbound("payload.action_state"), ActionStatePayload::handleServer);
        registerS2C(ActionStatePayload.class, ActionStatePayload.CODEC, ActionStatePayload.TYPE,
                id("payload.action_state"), ActionStatePayload::handleClient);

        registerC2S(ClientInformationPayload.class, ClientInformationPayload.CODEC, ClientInformationPayload.TYPE,
                serverbound("payload.client_info"), ClientInformationPayload::handleServer);
        registerS2C(ClientInformationPayload.class, ClientInformationPayload.CODEC, ClientInformationPayload.TYPE,
                id("payload.client_info"), ClientInformationPayload::handleClient);

        registerC2S(StaminaPayload.class, StaminaPayload.CODEC, StaminaPayload.TYPE,
                serverbound("payload.stamina"), StaminaPayload::handleServer);
        registerS2C(StaminaPayload.class, StaminaPayload.CODEC, StaminaPayload.TYPE,
                id("payload.stamina"), StaminaPayload::handleClient);
    }

    // ------------------------------------------------------------------
    // sending
    // ------------------------------------------------------------------

    public static <T extends CustomPacketPayload> void sendToServer(
            StreamCodec<? super FriendlyByteBuf, T> codec, T payload) {
        PlatformServices.get().getNetwork()
                .sendToServer(payload, serverbound(wirePath(payload)), codec);
    }

    public static <T extends CustomPacketPayload> void sendToPlayer(
            ServerPlayer player, StreamCodec<? super FriendlyByteBuf, T> codec, T payload) {
        PlatformServices.get().getNetwork()
                .sendToPlayer(player, payload, id(wirePath(payload)), codec);
    }

    public static <T extends CustomPacketPayload> void sendToPlayers(
            Iterable<ServerPlayer> players, StreamCodec<? super FriendlyByteBuf, T> codec, T payload) {
        PlatformServices.get().getNetwork()
                .sendToPlayers(players, payload, id(wirePath(payload)), codec);
    }

    /** Broadcast, the replacement for {@code PacketDistributor.sendToAllPlayers}. */
    public static <T extends CustomPacketPayload> void sendToAllPlayers(
            StreamCodec<? super FriendlyByteBuf, T> codec, T payload) {
        MinecraftServer current = server;
        if (current == null) return;
        sendToPlayers(current.getPlayerList().getPlayers(), codec, payload);
    }

    /**
     * The path component of a payload's declared id.
     *
     * <p>{@code ResourceLocation#getPath()} never contains the namespace, so this must
     * not strip one. It previously did, which chopped the first seven characters off
     * every path: {@code payload.client_info} became {@code nt.client_info}, and the
     * client then sent the join packet to {@code parcool:nt.client_info.c2s} while
     * the receiver was registered at {@code parcool:payload.client_info.c2s}. The id
     * lookup in Architectury's {@code NetworkAggregator} returned {@code null} and the
     * first client-to-server send of a session - {@code ClientInformationPayload} on
     * player join - failed with a "Network Protocol Error" disconnect screen.
     *
     * <p>{@link #id} and {@link #serverbound} re-attach the namespace.
     */
    private static String wirePath(CustomPacketPayload payload) {
        return payload.type().id().getPath();
    }
}
