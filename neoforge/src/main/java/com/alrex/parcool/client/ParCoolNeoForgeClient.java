package com.alrex.parcool.client;

import com.alrex.parcool.ParCool;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Client-only NeoForge entrypoint.
 * <p>
 * Architectury's {@code InitEvent.Client} is the loader-agnostic hook, but registering a second
 * {@code @Mod} class keeps the entry points symmetric with the Fabric module and avoids pulling the
 * architectury-neoforge client event bridge into {@code common}.
 */
@Mod(value = ParCool.MOD_ID, dist = Dist.CLIENT)
public class ParCoolNeoForgeClient {

    public ParCoolNeoForgeClient() {
        ParCool.initClient();
    }
}
