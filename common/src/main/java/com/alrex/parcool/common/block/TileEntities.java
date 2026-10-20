package com.alrex.parcool.common.block;

import com.alrex.parcool.common.block.zipline.ZiplineHookTileEntity;
import com.alrex.parcool.platform.PlatformServices;

import java.util.function.Supplier;

public class TileEntities {

    /**
     * 1.21.4 dropped {@code BlockEntityType.Builder} and left the type's own construction private,
     * so this goes through the platform: Fabric can still write the built-in registry directly from
     * its mod initializer, NeoForge has to wait for its registry event, and NeoForge additionally
     * re-opened the constructor. See {@code ParCoolPlatform#registerBlockEntityType}.
     */
    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ZiplineHookTileEntity>> ZIPLINE_HOOK =
            PlatformServices.get().registerBlockEntityType(
                    "zipline_hook",
                    (pos, state) -> new ZiplineHookTileEntity(TileEntities.ZIPLINE_HOOK.get(), pos, state),
                    () -> new net.minecraft.world.level.block.Block[]{
                            Blocks.WOODEN_ZIPLINE_HOOK.get(),
                            Blocks.IRON_ZIPLINE_HOOK.get()
                    }
            );

    public static void registerAll() {
        // Reading the field is what runs this class's static initialiser, i.e. the registration.
        // It has to happen here and not later: on Fabric the built-in registries are still open here
        // and frozen by the time a block is first placed.
        java.util.Objects.requireNonNull(ZIPLINE_HOOK);
    }
}
