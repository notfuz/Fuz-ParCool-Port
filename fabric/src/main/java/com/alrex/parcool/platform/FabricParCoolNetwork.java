package com.alrex.parcool.platform;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * Fabric network plumbing, built on the raw {@link NetworkManager}, exactly like the NeoForge side.
 *
 * <h2>Why there is no longer a Fabric special case</h2>
 * 1.21.1 had to use {@code NetworkChannel} here: Architectury 13's {@code architectury-fabric} did not
 * implement {@code NetworkAggregator.Adaptor#registerS2C}, so the id-based
 * {@code NetworkManager.registerReceiver} died with
 * {@code AbstractMethodError: NetworkManagerImpl$1 does not define … registerS2C}, and the
 * channel - which registers one id per payload class for both directions - was the only working path.
 * Architectury 19.0.1 removed {@code NetworkChannel} entirely and
 * {@code architectury-fabric 19.0.1}'s adaptor implements {@code registerS2C}, so the id-based API is
 * usable and both loaders now share one code path and one set of wire ids.
 *
 * <h2>Why the S2C side is registered differently on a dedicated server</h2>
 * The obvious implementation - {@code NetworkManager.registerReceiver(Side.S2C, id, receiver)} on both
 * sides - crashes a dedicated server at mod init, right out of {@code ParCool.init}:
 *
 * <pre>
 * Caused by: java.lang.AbstractMethodError: Receiver class
 *   dev.architectury.networking.fabric.NetworkManagerImpl$1 does not define or inherit an
 *   implementation of the resolved method 'abstract void registerS2C(
 *     net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type,
 *     net.minecraft.network.codec.StreamCodec,
 *     dev.architectury.networking.NetworkManager$NetworkReceiver)' of interface
 *     dev.architectury.impl.NetworkAggregator$Adaptor
 *   at dev.architectury.impl.NetworkAggregator.registerS2CReceiver(NetworkAggregator.java:119)
 *   at dev.architectury.impl.NetworkAggregator.registerReceiver(NetworkAggregator.java:76)
 *   at dev.architectury.networking.NetworkManager.registerReceiver(NetworkManager.java:93)
 *   at com.alrex.parcool.platform.FabricParCoolNetwork.register(FabricParCoolNetwork.java:45)
 *   at com.alrex.parcool.common.network.NetworkRegistries.registerS2C(NetworkRegistries.java:93)
 *   at com.alrex.parcool.ParCool.init(ParCool.java:78)
 * </pre>
 *
 * <p>The counters on that same run read {@code Registering S2C receiver: 0} and
 * {@code Registering C2S receiver: 1} before the throw, and the server never reaches
 * {@code Starting Minecraft server on}. Note the shape: <b>the very first S2C receiver kills it</b>,
 * so nothing after it ever registers.
 *
 * <p>{@code javap -v} on {@code NetworkManagerImpl$1} inside {@code architectury-fabric-15.0.3.jar}
 * shows that {@code registerS2C} <b>is</b> declared, with exactly the descriptor the interface declares
 * - so this is not a version skew and not a stale jar. The same {@code javap} shows why the method is
 * missing at run time: it is the only method of {@code NetworkAggregator.Adaptor} that carries
 *
 * <pre>
 * RuntimeInvisibleAnnotations:
 *   net.fabricmc.api.Environment(value = EnvType.CLIENT)
 * </pre>
 *
 * which is exactly what the Architectury source says it is - the body of {@code registerS2C} calls
 * {@code ClientPlayNetworking.registerGlobalReceiver}, a client-only API. Fabric Loader's
 * {@code EnvironmentStripper} deletes {@code @Environment(CLIENT)} members when the game runs on a
 * dedicated server, so {@code NetworkManagerImpl$1} arrives at the aggregator without
 * {@code registerS2C} and the first {@code registerS2CReceiver} call throws {@code AbstractMethodError}.
 * {@code registerC2S}, {@code registerS2CType} and both {@code toXxxPacket} methods carry no such
 * annotation and are present on both sides - which is why the C2S counter reaches 1 and S2C
 * registers 0 times.
 *
 * <p><b>That is also exactly why the defect stayed invisible for months.</b> {@code registerS2C} is
 * not missing from the jar, so the compiler is happy, {@code @Override} resolves, and the call type
 * checks. In singleplayer the integrated server lives inside the <i>client</i> JVM, where the
 * {@code @Environment(CLIENT)} members survive stripping and the same call succeeds; the same holds
 * for a dedicated client. Only a dedicated server JVM strips it. Nothing but a dedicated server run
 * can find this class of bug - a green client and a green singleplayer session prove nothing here.
 *
 * <p>It is not a workaround to register the S2C receiver lazily, or only once the first player joins:
 * a server never receives a server-to-client packet, so the S2C <i>receiver</i> is dead weight there
 * and its only required job is to make the payload type sendable. Architectury ships exactly that API,
 * and documents the split in its own javadoc on {@code NetworkManager#registerS2CPayloadType}:
 *
 * <blockquote>For S2C types, {@code registerReceiver} should be called on the client side, while
 * {@code registerS2CPayloadType} should be called on the server side.</blockquote>
 *
 * <p>{@code registerS2CPayloadType(id)} fills {@code NetworkAggregator.S2C_TYPE},
 * {@code S2C_CODECS} and {@code S2C_TRANSFORMERS} and reaches the loader through
 * {@code Adaptor.registerS2CType}, which on Fabric is
 * {@code PayloadTypeRegistry.playS2C().register(type, BufCustomPacketPayload.streamCodec(type))} - the
 * unstripped sibling of {@code registerS2C}, minus the client-only
 * {@code ClientPlayNetworking.registerGlobalReceiver} call.
 *
 * <h2>Why the wire format does not change</h2>
 * Both registration shapes hand the loader the same aggregator payload:
 * {@code registerReceiver} stores {@code S2C_TYPE[id] = new Type(id)} and registers
 * {@code BufCustomPacketPayload.streamCodec(type)}, and so does {@code registerS2CPayloadType}. The
 * real payload codec only ever runs locally - on send inside
 * {@code NetworkAggregator.collectPackets}, on receive inside the receiver lambda. A packet therefore
 * travels as {@code BufCustomPacketPayload(id, <the bytes ParCool encoded>)} either way, so a
 * server-to-client packet written by a dedicated Fabric server is byte-for-byte what the NeoForge side
 * writes and what the client already decodes. The client path is untouched by this change.
 *
 * <h2>Why not {@code NetworkChannel}
 * {@code NetworkChannel} is the usual Architectury answer, and it does dodge the crash, but it is
 * <b>wrong for a dedicated server</b>. Its {@code register} wraps the S2C half in
 * {@code if (Platform.getEnvironment() == Env.CLIENT)}, so on a server only the C2S half of the
 * aggregator is ever populated and {@code NetworkChannel#sendToPlayer} then calls
 * {@code NetworkManager.toPacket(s2c(), channelId, buf)}, whose
 * {@code NetworkAggregator.collectPackets(sink, S2C, id, buf)} does
 * {@code new BufCustomPacketPayload(S2C_TYPE.get(id), bytes)} with {@code S2C_TYPE.get(id) == null} -
 * a {@code NullPointerException} on every single server-to-client packet (limitation snapshot, stamina
 * broadcast, action state, breakfall event). A green {@code Done} would have hidden it. The id based
 * path with a side split keeps one set of wire ids on both loaders and keeps sending working.
 *
 * <p>This is also what keeps M4 away: registration and sending both address a message by its explicit
 * id, so {@code NetworkAggregator.C2S_TYPE}/{@code S2C_TYPE} - which the id-based send reads - are
 * always populated.
 */
public class FabricParCoolNetwork implements ParCoolNetwork {

    @Override
    public <T extends CustomPacketPayload> void register(
            Class<T> payloadClass,
            CustomPacketPayload.Type<T> type,
            ResourceLocation wireId,
            StreamCodec<?, T> codec,
            boolean clientbound,
            BiConsumer<T, NetworkManager.PacketContext> handler) {
        if (clientbound && Platform.getEnvironment() == Env.SERVER) {
            // On a dedicated server Fabric Loader strips Adaptor#registerS2C (the method carries
            // @Environment(EnvType.CLIENT)), so registerReceiver(S2C, ...) must not be called there -
            // it throws AbstractMethodError. A server does not need the receiver either: it only ever
            // sends. Registering the payload type is enough for NetworkManager.toPacket to know how to
            // encode it. Inside the client the method is not stripped, so that path is unchanged.
            // The full derivation is in the class javadoc.
            NetworkManager.registerS2CPayloadType(wireId);
            return;
        }
        // The id overload hands the raw buffer to the receiver and does the C2S_TYPE/S2C_TYPE
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
