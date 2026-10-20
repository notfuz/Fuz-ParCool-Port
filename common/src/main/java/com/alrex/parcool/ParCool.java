package com.alrex.parcool;

import com.alrex.parcool.api.Attributes;
import com.alrex.parcool.api.Effects;
import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.client.hud.HUDManager;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.renderer.Renderers;
import com.alrex.parcool.common.block.Blocks;
import com.alrex.parcool.common.block.TileEntities;
import com.alrex.parcool.common.entity.EntityTypes;
import com.alrex.parcool.common.event.ParCoolEvents;
import com.alrex.parcool.common.item.CreativeTabs;
import com.alrex.parcool.common.item.DataComponents;
import com.alrex.parcool.client.registry.ItemColors;
import com.alrex.parcool.common.item.Items;
import com.alrex.parcool.common.item.recipe.Recipes;
import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.common.potion.Potions;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.extern.AdditionalMods;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loader-agnostic mod entry point.
 *
 * <p>Upstream this class carried the NeoForge {@code @Mod} annotation and drove everything from the
 * FML mod/game buses. Here it only performs the shared registration; the two loader modules
 * ({@code fabric}, {@code neoforge}) call {@link #init()} / {@link #initClient()} from their own
 * entry points.
 */
public class ParCool {

    public static final String MOD_ID = "parcool";

    public static final Logger LOGGER = LogManager.getLogger();

    private static volatile boolean initialized;
    private static volatile boolean clientInitialized;

    private ParCool() {
    }

    /**
     * Runs on both sides. Mirrors the registration order of the original {@code @Mod} constructor so
     * that registry dependencies (attributes before entities, blocks before items, ...) still
     * resolve.
     */
    public static void init() {
        // Idempotent: NeoForge constructs every @Mod class of the mod id, and a duplicated
        // registration is a hard failure (e.g. "Duplicate DefaultAttributes entry:
        // entity.minecraft.player" from EntityAttributeRegistry).
        if (initialized) {
            LOGGER.debug("ParCool.init() already ran, skipping");
            return;
        }
        initialized = true;

        Effects.registerAll();
        Potions.registerAll();
        // NOTE: com.alrex.parcool.api.Attributes is deliberately *not* touched here. NeoForge freezes the
        // built-in registries before the mod constructors run, so the two entries are registered by
        // :neoforge's NeoForgeAttributes (a NeoForge DeferredRegister) and only *resolved* by Attributes
        // - and a class whose static initialiser resolves a Holder cannot be loaded this early on
        // NeoForge. On Fabric the loader's own entry point still calls Attributes.registerAll() inside the
        // writable window; see ParCoolFabric.
        SoundEvents.registerAll();
        Blocks.registerAll();
        Items.registerAll();
        CreativeTabs.registerAll();
        Recipes.registerAll();
        EntityTypes.registerAll();
        TileEntities.registerAll();
        DataComponents.registerAll();

        NetworkRegistries.registerPayloads();

        ParCoolConfig.load();
        ParCoolEvents.register();

        // Was FMLLoadCompleteEvent -> AdditionalMods.init()
        AdditionalMods.init();
        if (Platform.getEnvironment() == Env.SERVER) {
            AdditionalMods.initInDedicatedServer();
        }
    }

    /** Client-only half. */
    public static void initClient() {
        if (clientInitialized) {
            LOGGER.debug("ParCool.initClient() already ran, skipping");
            return;
        }
        clientInitialized = true;

        KeyBindings.register();
        Renderers.register();
        ItemColors.register();
        HUDManager.getInstance();
        AdditionalModsEventConsumerRegistrar.register();
        // Upstream never reached this: ParagliderManager#initInClient is the only path to
        // EventConsumerForParaglider#register, so the hook that cancels ParCool animations while the
        // player is gliding was never subscribed on any loader.
        AdditionalMods.initInClient();
        ParCoolEvents.registerClient();
    }

    /** Tiny indirection so {@link #initClient()} does not need to import the client-only consumer. */
    private static final class AdditionalModsEventConsumerRegistrar {
        static void register() {
            com.alrex.parcool.extern.AdditionalModsEventConsumer.Client.register();
        }
    }
}
