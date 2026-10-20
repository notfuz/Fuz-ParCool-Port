package com.alrex.parcool.common.potion;

import com.alrex.parcool.ParCool;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredRegister;

public class Potions {
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ParCool.MOD_ID, Registries.POTION);
    public static final RegistrySupplier<Potion> POOR_ENERGY_DRINK =
			POTIONS.register(
					"poor_energy_drink",
					() -> new Potion(
                            "poor_energy_drink",
                            new MobEffectInstance(com.alrex.parcool.api.Effects.INEXHAUSTIBLE, 2400/*2 min*/),
							new MobEffectInstance(MobEffects.HUNGER, 100),
							new MobEffectInstance(MobEffects.POISON, 100)
					)
			);
    public static final RegistrySupplier<Potion> ENERGY_DRINK =
			POTIONS.register(
					"energy_drink",
					() -> new Potion(
                            "energy_drink",
                            new MobEffectInstance(com.alrex.parcool.api.Effects.INEXHAUSTIBLE, 9600/*8 min*/)
					)
			);

	public static void registerAll() {
        POTIONS.register();
    }
}
