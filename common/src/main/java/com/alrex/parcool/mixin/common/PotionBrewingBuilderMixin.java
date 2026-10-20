package com.alrex.parcool.mixin.common;

import com.alrex.parcool.common.potion.ParCoolBrewingRecipe;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces {@code RegisterBrewingRecipesEvent}.
 * <p>
 * On NeoForge 21.4 brewing is data driven and the event does not exist on Fabric at all. Vanilla
 * 1.21.1 builds its recipe table in {@code PotionBrewing.Builder#build()}, which is called once
 * during {@code BrewingStandBlockEntity}'s static initialisation — a vanilla hook that behaves
 * identically on both loaders.
 */
@Mixin(PotionBrewing.Builder.class)
public abstract class PotionBrewingBuilderMixin {

    @Inject(method = "build", at = @At("TAIL"))
    private void parcool$addMixes(CallbackInfoReturnable<PotionBrewing> cir) {
        ParCoolBrewingRecipe.addMixes((PotionBrewing.Builder) (Object) this);
    }
}
