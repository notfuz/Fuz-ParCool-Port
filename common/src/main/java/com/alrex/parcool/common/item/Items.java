package com.alrex.parcool.common.item;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.block.Blocks;
import com.alrex.parcool.common.item.zipline.ZiplineRopeItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class Items {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ParCool.MOD_ID, Registries.ITEM);

    /**
     * {@code setId(...)} is mandatory from 1.21.4 on. {@code Item}'s constructor resolves its
     * description id through {@code Item.Properties#effectiveDescriptionId()}, which ends in
     * {@code Objects.requireNonNull(this.id, "Item id not set")} - building any item without the key
     * throws a {@link NullPointerException} while the registry is being filled. Vanilla sets it in
     * {@code Items.registerBlock} and in the bootstrap, NeoForge's {@code DeferredRegister.Items} sets
     * it too, Architectury's {@link DeferredRegister} does not, so the item's own registry key is
     * supplied here.
     */
    private static Item.Properties properties(String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, name)));
    }

    /**
     * The block-item half of the same 1.21.4 change. The description id moved from a virtual
     * {@code getDescriptionId()} override on {@link BlockItem} to a field resolved once at
     * construction, and the block-vs-item distinction moved into {@code Item.Properties}. Vanilla
     * reaches {@code block.<id>} through {@code Items.registerBlock}, which Architectury's register
     * does not call, so without the prefix the two hooks would be named from {@code item.parcool.*}
     * - keys no translation file in this mod defines, i.e. the English name would silently degrade to
     * the raw key. The 1.21.1 port got the same names from the {@link BlockItem} override that
     * 1.21.4 deleted, so this call is what preserves them.
     *
     * <p>The same commit is why {@code assets/parcool/items/*.json} exists at all: from 1.21.4 an
     * item's model is resolved from the item model definition set
     * ({@code assets/<namespace>/items/<path>.json}) rather than from
     * {@code models/item/<id>.json}, and a missing definition renders the purple/black missing model.
     */
    private static Item.Properties blockItemProperties(String name) {
        return properties(name).useBlockDescriptionPrefix();
    }

    public static final RegistrySupplier<Item> WOODEN_ZIPLINE_HOOK = ITEMS.register("wooden_zipline_hook", () -> new BlockItem(Blocks.WOODEN_ZIPLINE_HOOK.get(), blockItemProperties("wooden_zipline_hook")));
    public static final RegistrySupplier<Item> IRON_ZIPLINE_HOOK = ITEMS.register("iron_zipline_hook", () -> new BlockItem(Blocks.IRON_ZIPLINE_HOOK.get(), blockItemProperties("iron_zipline_hook")));
    public static final RegistrySupplier<Item> ZIPLINE_ROPE = ITEMS.register("zipline_rope", () -> new ZiplineRopeItem(properties("zipline_rope")));

	public static void registerAll() {
        ITEMS.register();
    }

}
