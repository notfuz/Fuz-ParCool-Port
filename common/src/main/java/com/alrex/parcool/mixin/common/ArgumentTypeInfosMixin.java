package com.alrex.parcool.mixin.common;

import com.alrex.parcool.server.command.args.ParCoolArgumentTypeInfos;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registers ParCool's command argument types.
 *
 * <p>Vanilla 1.21.1 populates the {@code ArgumentTypeInfo} table in the private static
 * {@code ArgumentTypeInfos#bootstrap(Registry)} through the equally private
 * {@code ArgumentTypeInfos#register(Registry, String, Class, ArgumentTypeInfo)}. NeoForge exposes a
 * public {@code registerByClass} wrapper and a registry entry, and Fabric has neither, so the
 * registration is injected at the single point that is identical on both loaders.
 */
@Mixin(ArgumentTypeInfos.class)
public abstract class ArgumentTypeInfosMixin {

    @Shadow
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>>
    ArgumentTypeInfo<A, T> register(Registry<ArgumentTypeInfo<?, ?>> registry,
                                    String id,
                                    Class<? extends A> argumentType,
                                    ArgumentTypeInfo<A, T> info) {
        throw new AssertionError();
    }

    @Inject(method = "bootstrap", at = @At("TAIL"))
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void parcool$registerArgumentTypes(Registry<ArgumentTypeInfo<?, ?>> registry,
                                                      CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> cir) {
        ParCoolArgumentTypeInfos.registerAll((id, argumentType, info) ->
                ArgumentTypeInfosMixin.register(registry, id, (Class) argumentType, (ArgumentTypeInfo) info));
    }
}
