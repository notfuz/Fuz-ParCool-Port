package com.alrex.parcool.common.data;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Loader-agnostic replacement for NeoForge's {@code IAttachmentHolder}.
 * <p>
 * Architectury has no attachment API, so ParCool stores its per-entity state in a plain map injected
 * into {@link Entity} by {@code mixin.common.ParCoolDataHolderMixin}. This keeps the storage, the
 * default-value semantics and the client/server split identical on Fabric and NeoForge, and it gives
 * us explicit control over persistence and clone-on-death (which NeoForge's
 * {@code AttachmentType#copyOnDeath} used to gate).
 *
 * @see ParCoolData
 * @see DataKey
 */
public interface IParCoolDataHolder {

    /**
     * @return the backing store, created on first access. Never {@code null}, possibly empty.
     */
    Map<DataKey<?>, Object> parcool$getDataMap();

    /**
     * Convenience for holders that are not {@link Entity}s; returns {@code null} for foreign objects
     * so that call sites can stay null-tolerant the way they were with {@code getData(...)}.
     */
    @Nullable
    static Map<DataKey<?>, Object> of(@Nullable Object holder) {
        return holder instanceof IParCoolDataHolder h ? h.parcool$getDataMap() : null;
    }
}
