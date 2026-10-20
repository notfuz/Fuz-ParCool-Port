package com.alrex.parcool.common.data;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Supplier;

/**
 * A typed slot in an {@link IParCoolDataHolder}'s map.
 * <p>
 * Mirrors the parts of NeoForge's {@code AttachmentType} that ParCool actually used: a default value
 * factory and an optional NBT (de)serializer. NeoForge's {@code sync(...)} flag is deliberately
 * absent -- ParCool never used attachment sync, all replication goes through the explicit payloads
 * in {@code com.alrex.parcool.common.network.payload}.
 *
 * @param <T> held value type
 */
public final class DataKey<T> {

    private final String id;

    private final Supplier<T> defaultValue;

    private final NbtSerializer<T> serializer;

    private DataKey(String id, Supplier<T> defaultValue, NbtSerializer<T> serializer) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.serializer = serializer;
    }

    /**
     * Slot whose value survives logout/login via the player NBT.
     */
    public static <T> DataKey<T> persistent(String id, Supplier<T> defaultValue, NbtSerializer<T> serializer) {
        return new DataKey<>(id, defaultValue, serializer);
    }

    /**
     * Slot that is rebuilt from scratch whenever the entity object is created (client-only
     * render state, per-tick action state, ...).
     */
    public static <T> DataKey<T> transientKey(String id, Supplier<T> defaultValue) {
        return new DataKey<>(id, defaultValue, null);
    }

    public String id() {
        return this.id;
    }

    public T createDefault() {
        return this.defaultValue.get();
    }

    public boolean isPersistent() {
        return this.serializer != null;
    }

    public NbtSerializer<T> serializer() {
        return this.serializer;
    }

    @Override
    public String toString() {
        return "DataKey[" + this.id + "]";
    }

    /** Reads/writes a single slot's payload into the holder's own NBT compound. */
    public interface NbtSerializer<T> {
        T read(CompoundTag tag);

        void write(T value, CompoundTag tag);
    }
}
