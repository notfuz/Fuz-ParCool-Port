package com.alrex.parcool.platform;

import com.alrex.parcool.ParCool;

import java.util.ServiceLoader;

/**
 * Static holder for the loader specific {@link ParCoolPlatform} implementation.
 *
 * <p>Architectury's {@code @ExpectPlatform} annotation processor would generate this lookup too, but
 * a plain {@link ServiceLoader} keeps the build free of an extra annotation processor and makes the
 * binding visible in the IDE.
 */
public final class PlatformServices {

    private static volatile ParCoolPlatform instance;

    private PlatformServices() {
    }

    public static ParCoolPlatform get() {
        ParCoolPlatform local = instance;
        if (local == null) {
            synchronized (PlatformServices.class) {
                local = instance;
                if (local == null) {
                    local = load();
                    instance = local;
                }
            }
        }
        return local;
    }

    private static ParCoolPlatform load() {
        for (ParCoolPlatform candidate : ServiceLoader.load(ParCoolPlatform.class)) {
            ParCool.LOGGER.debug("Using ParCool platform implementation: {}", candidate.getClass().getName());
            return candidate;
        }
        throw new IllegalStateException(
                "No ParCoolPlatform implementation found. Is the " + ParCool.MOD_ID
                        + " loader module present on the classpath?");
    }
}
