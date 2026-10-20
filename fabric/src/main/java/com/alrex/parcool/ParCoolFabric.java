package com.alrex.parcool;

import net.fabricmc.api.ModInitializer;

/** Fabric {@code main} entrypoint. */
public class ParCoolFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ParCool.init();
        // Fabric runs its entry points before Minecraft's Bootstrap#bootStrap, i.e. while
        // BuiltInRegistries is still writable, so the two player attributes can be registered directly
        // from the common class. On NeoForge the registries are already frozen here and
        // :neoforge's NeoForgeAttributes owns the registration instead - see ParCool#init.
        com.alrex.parcool.api.Attributes.registerAll();
    }
}
