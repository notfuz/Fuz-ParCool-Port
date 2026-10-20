package com.alrex.parcool.client.sound;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.config.ParCoolConfig;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;

/**
 * Owns the single {@link ZiplineRideSoundInstance} that plays while the local player rides a rope.
 *
 * <p>{@code RideZipline} lives in {@code common} and is loaded on a dedicated server, so the client
 * sound engine is only ever touched from here.
 *
 * <h2>Why every step is guarded</h2>
 * A throw anywhere in here used to propagate out of {@code Action#start}, which the
 * {@code ActionProcessor} try/catch turns into a force-stopped action - so a broken sound silently
 * killed the whole zipline ride, animation included. Sound setup must therefore never be able to
 * break the action that owns it.
 */
public final class ZiplineRideSound {

    @Nullable
    private static ZiplineRideSoundInstance current;

    private ZiplineRideSound() {
    }

    public static void start(Player player, double speed) {
        stop();
        try {
            if (!ParCoolConfig.Client.Booleans.EnableActionSounds.get()) return;
            if (!player.level().isClientSide) return;
            ZiplineRideSoundInstance instance =
                    new ZiplineRideSoundInstance(SoundEvents.ZIPLINE_RIDE.get(), player, speed);
            current = instance;
            Minecraft.getInstance().getSoundManager().play(instance);
            // Read only after play(): AbstractSoundInstance#getVolume needs the resolved Sound, which
            // exists from here on and not before.
            ParCool.LOGGER.debug("[parcool] zipline ride sound started {} volume={} pitch={}",
                    instance.getLocation(), instance.getVolume(), instance.getPitch());
        } catch (RuntimeException | LinkageError e) {
            current = null;
            ParCool.LOGGER.error("[parcool] zipline ride sound could not be started", e);
        }
    }

    public static void update(double speed) {
        ZiplineRideSoundInstance instance = current;
        if (instance == null) return;
        try {
            instance.setSpeed(speed);
        } catch (RuntimeException e) {
            stop();
        }
    }

    public static void stop() {
        ZiplineRideSoundInstance instance = current;
        if (instance == null) return;
        // Never leave a stale instance behind: a throwing stop() used to wedge `current`, so every
        // later start() unwound into stop() and no sound was ever played.
        current = null;
        try {
            instance.stopPlayback();
        } catch (RuntimeException | LinkageError e) {
            ParCool.LOGGER.debug("[parcool] stopping the zipline sound failed", e);
        }
    }
}
