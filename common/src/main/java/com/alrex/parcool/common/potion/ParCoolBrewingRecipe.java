package com.alrex.parcool.common.potion;

import com.alrex.parcool.ParCool;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Was registered through {@code RegisterBrewingRecipesEvent}, which only exists on NeoForge 21.1
 * (where brewing is data driven) and had no Fabric counterpart at all — on Fabric, 1.21.1 brewing
 * mixes are plain {@link PotionBrewing.Builder} entries.
 * <p>
 * The mixes are now applied through {@link #addMixes(PotionBrewing.Builder)}, which is invoked from
 * {@code mixin.common.PotionBrewingBuilderMixin} on {@code PotionBrewing.Builder#build} TAIL. That
 * hook is vanilla and therefore behaves identically on both loaders. 1.21.4 calls the builder from
 * {@code MinecraftServer}'s constructor, so the registry entries are long since frozen and complete.
 */
public class ParCoolBrewingRecipe {

    /**
     * The mixes take {@code Holder<Potion>} in 1.21.4, and the holder ends up in a synced
     * {@code PotionContents} component, so it has to be the registry holder - a
     * {@code Holder.direct(...)} would have no key to serialize.
     */
    private static Holder<Potion> parcoolPotion(String id) {
        return BuiltInRegistries.POTION.get(ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, id))
                .orElseThrow(() -> new IllegalStateException("ParCool potion " + id + " is not registered"));
    }

    public static void addMixes(PotionBrewing.Builder builder) {
        Holder<Potion> poorEnergyDrink = parcoolPotion("poor_energy_drink");
        Holder<Potion> energyDrink = parcoolPotion("energy_drink");
        builder.addMix(Potions.AWKWARD, Items.POISONOUS_POTATO, poorEnergyDrink);
        builder.addMix(Potions.AWKWARD, Items.CHICKEN, poorEnergyDrink);
        builder.addMix(Potions.AWKWARD, Items.QUARTZ, energyDrink);
        builder.addMix(poorEnergyDrink, Items.QUARTZ, energyDrink);
    }
}
