package com.alrex.parcool;

import com.alrex.parcool.registry.NeoForgeAttributes;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint. */
@Mod(ParCool.MOD_ID)
public class ParCoolNeoForge {

    public ParCoolNeoForge(ModContainer container) {
        // Must run before ParCool.init(): the built-in attribute registry is read by
        // common/api/Attributes, and the two entries are owned by NeoForge's DeferredRegister because
        // NeoForge freezes BuiltInRegistries before this constructor is reached.
        NeoForgeAttributes.register(container.getEventBus());
        // The block entity type registration is a RegisterEvent, i.e. a mod-bus event, and it needs
        // that bus before ParCool.init() reaches common/block/TileEntities.
        ((com.alrex.parcool.platform.NeoForgeParCoolPlatform) com.alrex.parcool.platform.PlatformServices.get())
                .setModEventBus(container.getEventBus());
        ParCool.init();
    }
}
