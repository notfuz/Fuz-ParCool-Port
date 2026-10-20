package com.alrex.parcool.common.network.payload;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.network.ServerPayloadGuard;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.Actions;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.network.ActionSynchronizationBroadcaster;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import com.alrex.parcool.api.event.ParCoolEventBus;
import dev.architectury.networking.NetworkManager;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ActionStatePayload(UUID playerID, List<Entry> states) implements CustomPacketPayload {
    public static final Type<ActionStatePayload> TYPE
            = new Type<>(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "payload.action_state"));
    public static final StreamCodec<ByteBuf, ActionStatePayload> CODEC = StreamCodec.of(
            ActionStatePayload::encode,
            ActionStatePayload::decode
    );

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, ActionStatePayload payload) {
        buf.writeLong(payload.playerID().getMostSignificantBits());
        buf.writeLong(payload.playerID().getLeastSignificantBits());
        buf.writeInt(payload.states().size());
        for (var s : payload.states()) {
            s.encode(buf);
        }
    }

    /**
     * Upper bound on the number of action-state entries in one packet. The list is rebroadcast to every
     * client, so an unbounded count read off the wire is a memory amplifier; no honest client ever
     * sends more than one entry per running action.
     */
    public static final int MAX_ENTRIES = 32;

    private static ActionStatePayload decode(ByteBuf buf) {
        var id = new UUID(buf.readLong(), buf.readLong());
        int size = buf.readInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException("Illegal action state count: " + size + " (max " + MAX_ENTRIES + ")");
        }
        ArrayList<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.addLast(Entry.decode(buf));
        }
        return new ActionStatePayload(id, entries);
    }

    public void processPlayer(Player player) {
        Parkourability parkourability = Parkourability.get(player);

        for (var state : this.states()) {
            Action action = parkourability.get(state.action());
            switch (state.type()) {
                case Start:
                    var buf = state.getDataAsBuffer();
                    ParCoolEventBus.post(new ParCoolActionEvent.Start.Pre(player, action));
                    action.start(player, parkourability, buf);
                    ParCoolEventBus.post(new ParCoolActionEvent.StartEvent(player, action));
                    ParCoolEventBus.post(new ParCoolActionEvent.Start.Post(player, action));
                    break;
                case Finish:
                    ParCoolEventBus.post(new ParCoolActionEvent.Finish.Pre(player, action));
                    action.finish(player);
                    ParCoolEventBus.post(new ParCoolActionEvent.StopEvent(player, action));
                    ParCoolEventBus.post(new ParCoolActionEvent.Finish.Post(player, action));
                    break;
                case Normal:
                    action.restoreSynchronizedState(state.getDataAsBuffer());
                    break;
            }
        }
    }

    public static void handleClient(ActionStatePayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player;
            Level world = context.getPlayer().level();
            player = world.getPlayerByUUID(payload.playerID());
            if (player == null || player.isLocalPlayer()) return;

            payload.processPlayer(player);
        });
    }

    public static void handleServer(ActionStatePayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            // Upstream accepted the payload's playerID verbatim and rebroadcast it to every client,
            // so a modified client could publish action state for any other player. Only the
            // sender's own id is accepted; the identity is rewritten so the broadcast cannot lie.
            if (!ServerPayloadGuard.isSelf(player, payload.playerID())) {
                ParCool.LOGGER.debug("Rejected action state for foreign player {}", payload.playerID());
                return;
            }
            // Stamped before processing, so a packet that fails validation still proves the client is
            // alive; the watchdog is about liveness, not about payload contents.
            Parkourability.get(player).setLastActionStateTick(player.tickCount);
            // processPlayer hands the entry buffers to the actions, which read them without bounds
            // checks of their own; a truncated or over-long Start payload therefore has to be a
            // rejected packet, not a RuntimeException thrown out of a netty handler.
            try {
                payload.processPlayer(player);
            } catch (DecoderException | IndexOutOfBoundsException | NegativeArraySizeException |
                     ArithmeticException e) {
                ParCool.LOGGER.warn("Rejected malformed action state from {}", player.getName().getString(), e);
                return;
            }
            ActionSynchronizationBroadcaster.add(payload);
        });
    }

    public record Entry(Class<? extends Action> action, Type type, byte[] data) {
        public enum Type {
            Normal, Start, Finish;
        }

        private void encode(ByteBuf buf) {
            buf
                    .writeShort(Actions.getIndexOf(action))
                    .writeByte(type().ordinal())
                    .writeInt(data().length)
                    .writeBytes(data);
        }

        public ByteBuffer getDataAsBuffer() {
            return ByteBuffer.wrap(data);
        }

        /**
         * Upper bound for the per-action state blob. The biggest honest payload today is HideInBlock's
         * 98-byte start buffer, so this leaves ample head-room while keeping a crafted length from
         * allocating a huge array - or a negative one, which would be a NegativeArraySizeException.
         */
        public static final int MAX_DATA_LENGTH = 4096;

        private static Entry decode(ByteBuf buf) {
            int index = buf.readShort();
            var action = Actions.getByIndex(index);
            if (action == null) {
                // readShort is signed and unbounded, so the index has to be resolved defensively.
                throw new DecoderException("Unknown action index " + index);
            }
            int typeOrdinal = buf.readUnsignedByte();
            Type[] types = Type.values();
            if (typeOrdinal >= types.length) {
                throw new DecoderException("Unknown action state type " + typeOrdinal);
            }
            Type type = types[typeOrdinal];
            int remaining = buf.readInt();
            if (remaining < 0 || remaining > MAX_DATA_LENGTH) {
                throw new DecoderException("Illegal action state length: " + remaining
                        + " (max " + MAX_DATA_LENGTH + ")");
            }
            if (buf.readableBytes() < remaining) {
                throw new DecoderException("Truncated action state: " + remaining
                        + " bytes announced, " + buf.readableBytes() + " available");
            }
            var data = new byte[remaining];
            buf.readBytes(data);
            return new Entry(action, type, data);
        }
    }
}
