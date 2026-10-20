package com.alrex.parcool.client.registry;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.item.zipline.ZiplineRopeItem;
import com.alrex.parcool.mixin.client.ItemTintSourcesAccessor;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Client-only item tint registration for the zipline rope.
 *
 * <h2>Why this is not a method on the item registry</h2>
 * {@code Items} is loaded on a dedicated server (it holds the item registry), so anything reachable
 * from its methods is verified there too. {@link ZiplineRopeItem} is such a class, and an
 * {@code ItemTintSource} handed to a client-only parameter would force the verifier to load
 * {@code net.minecraft.client.color.item.ItemTintSource} from a server-side frame - which is exactly
 * what NeoForge's {@code RuntimeDistCleaner} aborts on with
 * "Attempted to load class ... for invalid dist DEDICATED_SERVER". Keeping the tint source in a class
 * only the client loads removes that verification edge.
 *
 * <h2>How the tint is applied in 1.21.4</h2>
 * 1.21.1 had {@code ColorHandlerRegistry.registerItemColors(ItemColor, items)}, and Architectury 16
 * dropped that method entirely - item tints are now declared per model in
 * {@code assets/parcool/items/zipline_rope.json} and resolved through a codec that is registered in
 * {@code ItemTintSources}' private id mapper. That is why {@link ItemTintSourcesAccessor} exists.
 *
 * <p>A single tint entry reproduces 1.21.1's {@code i > 0 ? -1 : color} exactly: the renderer looks
 * the quad's tint index up in the per-layer tint array and falls back to "no tint" when the index is
 * out of range, so the base layer ({@code layer0}) is dyed and the overlay ({@code layer1}) is not.
 */
public final class ItemColors {

    /** The {@code type} an {@code assets/parcool/items/*.json} tint entry refers to. */
    public static final ResourceLocation ZIPLINE_ROPE_TINT =
            ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "zipline_rope");

    public static final MapCodec<ItemTintSource> ZIPLINE_ROPE_TINT_CODEC =
            MapCodec.unit((ItemTintSource) ZiplineRopeItemTintSource.INSTANCE);

    private ItemColors() {
    }

    public static void register() {
        ItemTintSourcesAccessor.parcool$idMapper().put(ZIPLINE_ROPE_TINT, ZIPLINE_ROPE_TINT_CODEC);
    }

    /** The rope's own colour, which used to be {@code ZiplineRopeItem.RopeColor}. */
    public record ZiplineRopeItemTintSource() implements ItemTintSource {

        static final ZiplineRopeItemTintSource INSTANCE = new ZiplineRopeItemTintSource();

        @Override
        public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
            return ZiplineRopeItem.getColor(itemStack);
        }

        @Override
        public MapCodec<? extends ItemTintSource> type() {
            return ZIPLINE_ROPE_TINT_CODEC;
        }
    }
}
