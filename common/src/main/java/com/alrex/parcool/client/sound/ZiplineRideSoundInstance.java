package com.alrex.parcool.client.sound;

import com.alrex.parcool.ParCool;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The rope-sliding loop for {@code RideZipline}.
 *
 * <p>Upstream 3.4.3.3 registers only {@code zipline.set} / {@code zipline.remove} and never plays
 * anything while riding, so the ride is completely silent. The clip is a purpose-built seamless
 * 2.09 s loop ({@code assets/parcool/sounds/zipline_slide.ogg}), so this instance is a
 * {@code looping} sound that follows the player and scales its volume and pitch with the current
 * speed instead of being re-triggered every few ticks.
 *
 * <p>Kept in its own client-only class: {@code RideZipline} itself is loaded on a dedicated server.
 */
public class ZiplineRideSoundInstance extends AbstractTickableSoundInstance {

    /** Below this the ride is not really moving and the loop fades out to the idle floor. */
    private static final double MIN_AUDIBLE_SPEED = 0.02;
    /**
     * Never fully silent. The instance is handed to the sound engine once, at the moment the ride
     * starts, and the first speed sample there is usually still ~0 (the player has just been hooked);
     * a volume of 0 at that point is what made the loop inaudible for the first seconds.
     */
    private static final float MIN_VOLUME = 0.15f;
    /** Speed at which the loop reaches its full volume. */
    private static final double FULL_SPEED = 0.55;
    private static final double MIN_PITCH = 0.85;
    private static final double MAX_PITCH = 1.35;
    private static final float BASE_VOLUME = 0.85f;

    private final Player player;
    private double speed;

    public ZiplineRideSoundInstance(SoundEvent sound, Player player, double speed) {
        super(sound, SoundSource.PLAYERS, RandomSource.create(ParCool.MOD_ID.hashCode()));
        this.player = player;
        this.looping = true;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        setSpeed(speed);
    }

    /** Called every client tick from {@code RideZipline#onWorkingTickInLocalClient}. */
    public void setSpeed(double speed) {
        // The rope is ridden in whichever direction it was hooked, so the scalar speed can be
        // negative; loudness and pitch follow its magnitude, not its sign, or the loop would be
        // muted exactly when the ride is fastest.
        this.speed = Math.abs(speed);
        this.volume = BASE_VOLUME * volumeFactor();
        this.pitch = (float) (MIN_PITCH + (MAX_PITCH - MIN_PITCH)
                * Math.min(1.0, this.speed / FULL_SPEED));
    }

    private float volumeFactor() {
        if (speed <= MIN_AUDIBLE_SPEED) return MIN_VOLUME;
        return MIN_VOLUME + (float) ((1.0f - MIN_VOLUME) * Math.min(1.0, speed / FULL_SPEED));
    }

    @Override
    public void tick() {
        if (player.isRemoved() || !player.isAlive() || player.level() != null && !player.level().isClientSide) {
            stopPlayback();
            return;
        }
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    /** Public alias for the protected {@link AbstractTickableSoundInstance#stop()}. */
    public void stopPlayback() {
        stop();
    }
}
