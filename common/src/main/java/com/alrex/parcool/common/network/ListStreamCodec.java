package com.alrex.parcool.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ListStreamCodec<B extends ByteBuf, R> implements StreamCodec<B, List<R>> {
    private final StreamCodec<B, R> CODEC;

    public ListStreamCodec(StreamCodec<B, R> codec) {
        CODEC = codec;
    }

    /**
     * Upper bound on the element count read off the wire. The list is replicated to every client, so a
     * single crafted packet with a huge count turns into a multi-gigabyte allocation on every server
     * that receives it.
     */
    public static final int MAX_ENTRIES = 512;

    @Nonnull
    @Override
    public List<R> decode(B b) {
        int count = b.readInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new DecoderException("Illegal list length: " + count + " (max " + MAX_ENTRIES + ")");
        }
        var list = new ArrayList<R>(count);
        for (int i = 0; i < count; i++) {
            list.addLast(CODEC.decode(b));
        }
        return list;
    }

    @Override
    public void encode(B b, List<R> rs) {
        b.writeInt(rs.size());
        for (R elem : rs) {
            CODEC.encode(b, elem);
        }
    }
}
