package com.alrex.parcool.api.event;

/**
 * Loader-agnostic replacement for NeoForge's {@code net.neoforged.bus.api.ICancellableEvent}.
 * <p>
 * Only the ParCool subclasses that were cancellable upstream implement it, so the
 * "cancellable / not cancellable" distinction of the public API is preserved exactly.
 */
public interface CancellableEvent {

    boolean isCanceled();

    void setCanceled(boolean canceled);
}
