package com.alrex.parcool;

import net.fabricmc.api.ClientModInitializer;

/** Fabric {@code client} entrypoint. */
public class ParCoolFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ParCool.initClient();
    }
}
