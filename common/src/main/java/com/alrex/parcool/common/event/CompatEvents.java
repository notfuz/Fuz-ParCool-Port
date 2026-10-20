package com.alrex.parcool.common.event;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The handful of NeoForge events that have no Architectury counterpart.
 * <p>
 * Architectury covers ticks, player/entity lifecycle, commands, HUD, key bindings, item colours and
 * entity renderers, but it deliberately does not mirror the NeoForge client render pipeline hooks or
 * the input pipeline hooks. Rather than leak a platform abstraction into every action, those events
 * are modelled as small loader-agnostic value objects and dispatched from ordinary vanilla mixins,
 * which behave identically on Fabric and NeoForge.
 */
public final class CompatEvents {

    private CompatEvents() {
    }

    /**
     * Stand-in for {@code net.neoforged.neoforge.client.event.RenderFrameEvent}.
     * Dispatched from {@code mixin.client.GameRendererTickMixin} on {@code Minecraft#render} HEAD.
     * Only {@link #getPartialTick()} is read by ParCool.
     */
    public static class RenderFrameEvent {

        private final DeltaTracker partialTick;

        public RenderFrameEvent(DeltaTracker partialTick) {
            this.partialTick = partialTick;
        }

        public DeltaTracker getPartialTick() {
            return this.partialTick;
        }

        /** Fired once per frame before the world is rendered. */
        public static class Pre extends RenderFrameEvent {
            public Pre(DeltaTracker partialTick) {
                super(partialTick);
            }
        }
    }

    /**
     * Stand-in for {@code net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles}.
     * Dispatched from {@code mixin.client.CameraAnglesMixin} on {@code Camera#setup} TAIL; the
     * mutated pitch/roll are read back out of the event by the mixin.
     */
    public static class ViewportEvent {

        public static class ComputeCameraAngles {

            private final Camera camera;
            private final float partialTick;

            private float yaw;
            private float pitch;
            private float roll;

            public ComputeCameraAngles(Camera camera, float partialTick,
                                       float yaw, float pitch, float roll) {
                this.camera = camera;
                this.partialTick = partialTick;
                this.yaw = yaw;
                this.pitch = pitch;
                this.roll = roll;
            }

            public Camera getCamera() {
                return this.camera;
            }

            /** The interpolated frame time, i.e. NeoForge's {@code ComputeCameraAngles#getPartialTick()}. */
            public float getPartialTick() {
                return this.partialTick;
            }

            public float getYaw() {
                return this.yaw;
            }

            public void setYaw(float yaw) {
                this.yaw = yaw;
            }

            public float getPitch() {
                return this.pitch;
            }

            public void setPitch(float pitch) {
                this.pitch = pitch;
            }

            public float getRoll() {
                return this.roll;
            }

            public void setRoll(float roll) {
                this.roll = roll;
            }
        }
    }

    /**
     * Stand-in for {@code net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered}.
     * Dispatched from {@code mixin.client.MinecraftInputMixin} for every ParCool-relevant key
     * interaction; the mixin reads {@link #isCanceled()} / {@link #shouldSwingHand()} back out to
     * decide whether vanilla should still run the interaction.
     */
    public static class InputEvent {

        public static class InteractionKeyMappingTriggered {

            private final boolean useItem;
            private final KeyMapping keyMapping;

            private boolean canceled = false;
            private boolean swingHand = true;

            public InteractionKeyMappingTriggered(boolean useItem, KeyMapping keyMapping) {
                this.useItem = useItem;
                this.keyMapping = keyMapping;
            }

            public boolean isUseItem() {
                return this.useItem;
            }

            public boolean isAttack() {
                return !this.useItem;
            }

            public KeyMapping getKeyMapping() {
                return this.keyMapping;
            }

            public boolean isCanceled() {
                return this.canceled;
            }

            public void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }

            public boolean shouldSwingHand() {
                return this.swingHand;
            }

            public void setSwingHand(boolean swingHand) {
                this.swingHand = swingHand;
            }
        }
    }

    /**
     * Stand-in for {@code net.neoforged.neoforge.event.entity.living.LivingFallEvent}.
     * Dispatched from {@code mixin.common.LivingEntityFallMixin} on
     * {@code LivingEntity#causeFallDamage} HEAD. The mixin both honours {@link #isCanceled()} and
     * applies {@link #getDamageMultiplier()} to the amount handed to {@code hurt()}.
     */
    public static class LivingFallEvent {

        private final LivingEntity entity;
        private final float distance;
        private final DamageSource source;

        private float damageMultiplier = 1.0F;
        private boolean canceled = false;

        public LivingFallEvent(LivingEntity entity, float distance, DamageSource source) {
            this.entity = entity;
            this.distance = distance;
            this.source = source;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public Player getPlayer() {
            return this.entity instanceof Player player ? player : null;
        }

        public float getDistance() {
            return this.distance;
        }

        public DamageSource getSource() {
            return this.source;
        }

        public boolean isCanceled() {
            return this.canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public float getDamageMultiplier() {
            return this.damageMultiplier;
        }

        public void setDamageMultiplier(float damageMultiplier) {
            this.damageMultiplier = damageMultiplier;
        }
    }

    /**
     * Stand-in for {@code net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent}.
     * Architectury does expose {@code EntityEvent.LIVING_HURT}, but that one reports the amount
     * after vanilla mitigation; ParCool only needs a "cancel this incoming hit" veto, which is what
     * this event models. Returning {@code true} from a listener cancels the damage.
     */
    public static class LivingIncomingDamageEvent {

        public interface Listener {
            boolean onIncomingDamage(LivingEntity entity, DamageSource source);
        }
    }
}
