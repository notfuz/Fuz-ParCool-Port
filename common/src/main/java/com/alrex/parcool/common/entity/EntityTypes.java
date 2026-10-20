package com.alrex.parcool.common.entity;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.entity.zipline.ZiplineRopeEntity;
import com.alrex.parcool.common.zipline.Zipline;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredRegister;

public class EntityTypes {
    private static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(ParCool.MOD_ID, Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<ZiplineRopeEntity>> ZIPLINE_ROPE
            = REGISTER.register("zipline_rope", () -> EntityType.Builder
            .of((EntityType.EntityFactory<ZiplineRopeEntity>) ZiplineRopeEntity::new, MobCategory.MISC)
            .noSave()
            .clientTrackingRange((int) (Zipline.MAXIMUM_HORIZONTAL_DISTANCE / 1.9))
            .updateInterval(Integer.MAX_VALUE)
            .sized(0.1f, 0.1f)
            .noSummon()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "zipline_rope")))
    );

    public static void registerAll() {
        REGISTER.register();
    }
}
