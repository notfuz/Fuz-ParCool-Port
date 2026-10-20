package com.alrex.parcool.compat;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Exposes {@link net.minecraft.client.KeyMapping}'s private {@code key} field.
 * <p>
 * NeoForge patches {@code KeyMapping} with a public {@code getKey()}; vanilla keeps the bound key in
 * a private field, so ParCool's key-conflict comparisons (does the ParCool key share a binding with
 * sneak / zipline / wall-slide?) had to be redirected here. See {@code mixin.client.KeyMappingMixin}.
 */
public interface IKeyMappingDuck {
    InputConstants.Key parcool$getKey();
}
