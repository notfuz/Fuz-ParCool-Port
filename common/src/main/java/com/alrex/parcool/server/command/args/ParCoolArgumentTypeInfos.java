package com.alrex.parcool.server.command.args;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;

import java.util.List;

/**
 * The six command argument types ParCool adds to {@code net.minecraft.commands.synchronization.
 * ArgumentTypeInfos}.
 *
 * <p>Upstream this was a {@code DeferredRegister} on {@code Registries.COMMAND_ARGUMENT_TYPE} whose
 * suppliers called NeoForge's public {@code ArgumentTypeInfos.registerByClass}. Vanilla 1.21.1 has
 * no such method: the table is filled by the private static
 * {@code ArgumentTypeInfos#register(Registry, String, Class, ArgumentTypeInfo)}, which is only
 * called from {@code ArgumentTypeInfos#bootstrap}. Architectury has no command-argument registry
 * either, so the registrations are performed by
 * {@code mixin.common.ArgumentTypeInfosMixin} from that exact bootstrap point, which behaves the
 * same on Fabric and NeoForge.
 */
public class ParCoolArgumentTypeInfos {

    public record Entry(
            String id,
            Class<? extends ArgumentType<?>> argumentType,
            ArgumentTypeInfo<?, ?> info
    ) {
    }

    /** Receives one registration; the mixin supplies the call into the private vanilla method. */
    @FunctionalInterface
    public interface Registrar {
        void register(String id, Class<? extends ArgumentType<?>> argumentType, ArgumentTypeInfo<?, ?> info);
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("action", ActionArgumentType.class, SingletonArgumentInfo.contextFree(ActionArgumentType::action)),
            new Entry("limitation_bool", LimitationItemArgumentType.Booleans.class, SingletonArgumentInfo.contextFree(LimitationItemArgumentType::booleans)),
            new Entry("limitation_int", LimitationItemArgumentType.Integers.class, SingletonArgumentInfo.contextFree(LimitationItemArgumentType::integers)),
            new Entry("limitation_reals", LimitationItemArgumentType.Doubles.class, SingletonArgumentInfo.contextFree(LimitationItemArgumentType::doubles)),
            new Entry("limitation_id", LimitationIDArgumentType.class, SingletonArgumentInfo.contextFree(LimitationIDArgumentType::new)),
            new Entry("stamina_type", StaminaTypeArgumentType.class, SingletonArgumentInfo.contextFree(StaminaTypeArgumentType::new))
    );

    private ParCoolArgumentTypeInfos() {
    }

    @SuppressWarnings("rawtypes")
    public static void registerAll(Registrar registrar) {
        for (Entry entry : ENTRIES) {
            registrar.register(entry.id(), entry.argumentType(), entry.info());
        }
    }
}
