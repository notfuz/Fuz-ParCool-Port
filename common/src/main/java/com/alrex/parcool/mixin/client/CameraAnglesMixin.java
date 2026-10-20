package com.alrex.parcool.mixin.client;

import com.alrex.parcool.common.event.CompatEvents;
import com.alrex.parcool.client.action.ClientActionProcessor;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces {@code ViewportEvent.ComputeCameraAngles}, which has no Architectury counterpart.
 *
 * <h2>Vanilla has no camera roll</h2>
 * NeoForge patches {@link Camera} with a roll field that its {@code ComputeCameraAngles} event can
 * set. Vanilla 1.21.1 has no such field, so roll is applied here by re-rotating the camera basis
 * ({@code look} / {@code up} / {@code left} are handed out live by
 * {@link Camera#getLookVector()}, {@link Camera#getUpVector()} and {@link Camera#getLeftVector()}, so
 * they can be mutated in place) and the exported {@link Camera#rotation()} quaternion.
 *
 * <p>This is the one ported hook whose visual result has to be looked at in game rather than
 * reasoned about: the pitch/yaw path is a direct translation, the roll path is new code.
 */
@Mixin(Camera.class)
public abstract class CameraAnglesMixin {

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void parcool$computeCameraAngles(BlockGetter level, Entity entity, boolean detached,
                                             boolean mirrored, float partialTick, CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        CompatEvents.ViewportEvent.ComputeCameraAngles event =
                new CompatEvents.ViewportEvent.ComputeCameraAngles(
                        self, partialTick, self.getYRot(), self.getXRot(), 0.0F);

        ClientActionProcessor.onViewRender(event);

        float pitch = event.getPitch();
        float roll = event.getRoll();
        if (pitch == self.getXRot() && roll == 0.0F) return;

        if (pitch != self.getXRot()) {
            setRotation(event.getYaw(), pitch);
        }
        if (roll != 0.0F) {
            applyRoll(self, roll);
        }
    }

    private static void applyRoll(Camera camera, float rollDegrees) {
        float radians = (float) Math.toRadians(rollDegrees);
        Quaternionf rotation = new Quaternionf().rotateZ(radians);

        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        up.rotate(rotation);
        left.rotate(rotation);

        // The exported orientation quaternion has to agree with the rotated basis, otherwise
        // entities and particles placed relative to the camera keep the un-rolled orientation.
        Quaternionf exported = camera.rotation();
        exported.mul(rotation).normalize();
    }
}
