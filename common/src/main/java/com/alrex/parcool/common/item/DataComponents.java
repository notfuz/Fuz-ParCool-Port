package com.alrex.parcool.common.item;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.item.component.ZiplineColorComponent;
import com.alrex.parcool.common.item.component.ZiplinePositionComponent;
import com.alrex.parcool.common.item.component.ZiplineTensionComponent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

/**
 * Ported from NeoForge's {@code DeferredRegister.DataComponents}. The upstream helper only wrapped
 * the vanilla {@link DataComponentType.Builder}, so the persistence/network-sync flags are spelled
 * out here with the same vanilla builder calls.
 */
public class DataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(ParCool.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<ZiplineColorComponent>> ZIPLINE_COLOR =
            COMPONENTS.register("zipline_color", () -> DataComponentType.<ZiplineColorComponent>builder()
                    .persistent(ZiplineColorComponent.CODEC)
                    .networkSynchronized(ZiplineColorComponent.STREAM_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<ZiplinePositionComponent>> ZIPLINE_POSITION =
            COMPONENTS.register("zipline_pos", () -> DataComponentType.<ZiplinePositionComponent>builder()
                    .persistent(ZiplinePositionComponent.CODEC)
                    .networkSynchronized(ZiplinePositionComponent.STREAM_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<ZiplineTensionComponent>> ZIPLINE_TENSION =
            COMPONENTS.register("zipline_tension", () -> DataComponentType.<ZiplineTensionComponent>builder()
                    .persistent(ZiplineTensionComponent.CODEC)
                    .networkSynchronized(ZiplineTensionComponent.STREAM_CODEC)
                    .build());

    public static void registerAll() {
        COMPONENTS.register();
    }
}
