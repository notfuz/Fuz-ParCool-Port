package com.alrex.parcool.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;


public record ReadonlyStamina(boolean isExhausted, int value, int max) {
    public static ReadonlyStamina createDefault() {
        return new ReadonlyStamina(false, 0, 2000);
    }

    public ReadonlyStamina consumed(int value) {
        if (isExhausted()) return this;
        int newValue = this.value() - value;
        boolean exhausted = false;
        if (newValue < 0) {
            newValue = 0;
            exhausted = true;
        }
        return new ReadonlyStamina(exhausted, newValue, max());
    }

    public ReadonlyStamina recovered(int value) {
        int newValue = this.value() + value;
        boolean exhausted = isExhausted();
        // >=, not >: recovering up to exactly max() left the exhausted flag set, so a player on a full
        // bar was still treated as exhausted and could not start anything until the bar was pushed over
        // the limit by a single point.
        if (newValue >= max) {
            newValue = max;
            exhausted = false;
        }
        return new ReadonlyStamina(exhausted, newValue, max());
    }

    public static final Codec<ReadonlyStamina> CODEC = RecordCodecBuilder.create(staminaInstance ->
            staminaInstance.group(
                    Codec.BOOL.fieldOf("exhausted").forGetter(ReadonlyStamina::isExhausted),
                    Codec.INT.fieldOf("value").forGetter(ReadonlyStamina::value),
                    Codec.INT.fieldOf("max").forGetter(ReadonlyStamina::max)
            ).apply(staminaInstance, ReadonlyStamina::new)
    );
    public static final StreamCodec<ByteBuf, ReadonlyStamina> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ReadonlyStamina::isExhausted,
            ByteBufCodecs.VAR_INT,
            ReadonlyStamina::value,
            ByteBufCodecs.INT,
            ReadonlyStamina::max,
            ReadonlyStamina::new
    );
}
