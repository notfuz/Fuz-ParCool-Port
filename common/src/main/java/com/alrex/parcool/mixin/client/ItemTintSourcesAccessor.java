package com.alrex.parcool.mixin.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs.LateBoundIdMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ItemTintSources}' private id mapper.
 *
 * <p>1.21.4 resolves every item model tint through a codec keyed by an {@link ResourceLocation}, and the
 * mapper is a private static {@link LateBoundIdMapper} that only {@code ItemTintSources#bootstrap}
 * (vanilla's eight entries) writes to. There is no public registration API on either loader any more -
 * Architectury 16 removed {@code ColorHandlerRegistry.registerItemColors} - so ParCool's rope tint
 * has to be added here. The mapper is late-bound, so writing to it after {@code CODEC} was built is
 * fine, which is what lets the registration happen from the client entry point.
 */
@Mixin(ItemTintSources.class)
public interface ItemTintSourcesAccessor {

    @Accessor("ID_MAPPER")
    static LateBoundIdMapper<ResourceLocation, MapCodec<? extends ItemTintSource>> parcool$idMapper() {
        throw new AssertionError();
    }
}
