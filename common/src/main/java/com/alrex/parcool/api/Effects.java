package com.alrex.parcool.api;


import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.potion.effects.InexhaustibleEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredRegister;

public class Effects {
	private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ParCool.MOD_ID, Registries.MOB_EFFECT);
	public static final RegistrySupplier<MobEffect> INEXHAUSTIBLE = EFFECTS.register(
			"inexhaustible", InexhaustibleEffect::new
	);

	public static void registerAll() {
        EFFECTS.register();
    }
}
