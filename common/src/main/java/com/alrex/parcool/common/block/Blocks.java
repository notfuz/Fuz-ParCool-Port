package com.alrex.parcool.common.block;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.block.zipline.IronZiplineHookBlock;
import com.alrex.parcool.common.block.zipline.WoodenZiplineHookBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredRegister;

public class Blocks {
    private static final DeferredRegister<Block> REGISTER = DeferredRegister.create(ParCool.MOD_ID, Registries.BLOCK);

    /**
     * {@code setId(...)} is mandatory from 1.21.2 on. {@code BlockBehaviour}'s constructor resolves
     * the loot table and the description id out of the properties, and both go through
     * {@code Objects.requireNonNull(this.id, "Block id not set")} - so a block built without the key
     * throws a {@link NullPointerException} the moment the game asks the hook for its description,
     * its model or its drops. Vanilla sets the key in {@code Blocks.register}, NeoForge's
     * {@code DeferredRegister.Blocks} sets it too, Architectury's {@link DeferredRegister} does not,
     * so each block's own registry key is supplied here.
     *
     * <p>Do not "clean this up" as redundant: it looks like the register call already knows the name,
     * and it does not pass it down to the properties. Removing it turns a green build into a crash
     * during registry fill.
     */
    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, name));
    }

    public static final RegistrySupplier<Block> WOODEN_ZIPLINE_HOOK = REGISTER.register(
            "wooden_zipline_hook",
            () -> new WoodenZiplineHookBlock(BlockBehaviour.Properties
                    .of()
                    .setId(key("wooden_zipline_hook"))
                    .mapColor(MapColor.WOOD)
                    .strength(1.0f, 3.0f)
                    .sound(SoundType.WOOD)
            )
    );
    public static final RegistrySupplier<Block> IRON_ZIPLINE_HOOK = REGISTER.register(
            "iron_zipline_hook",
            () -> new IronZiplineHookBlock(BlockBehaviour.Properties
                    .of()
                    .setId(key("iron_zipline_hook"))
                    .mapColor(MapColor.METAL)
                    .strength(1.0f, 3.0f)
                    .noCollission()
                    .sound(SoundType.CHAIN)
            )
    );

    public static void registerAll() {
        REGISTER.register();
    }

}
