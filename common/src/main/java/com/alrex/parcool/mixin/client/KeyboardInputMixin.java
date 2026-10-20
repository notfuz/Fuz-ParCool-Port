package com.alrex.parcool.mixin.client;

import com.alrex.parcool.client.input.KeyRecorder;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces {@code MovementInputUpdateEvent}, which has no Architectury counterpart.
 * <p>
 * NeoForge fires the event at the end of {@code KeyboardInput#tick} (or its patch thereof), so
 * sampling the key states from the RETURN of the same method gives ParCool the exact same data on
 * both loaders — including the timing relative to {@code LocalPlayer#aiStep}.
 *
 * <p>The target is written as the explicit {@code tick()V} because {@code KeyboardInput#tick} has no
 * parameters in 1.21.4 - javap on the 1.21.4 dev jar gives {@code public void tick()} and nothing
 * else, so the handler must take the {@link CallbackInfo} and nothing more. Leaving the 1.21.1
 * shape ({@code (boolean slowDown, float movingSpeed, CallbackInfo)}) on this port aborted the boot
 * as soon as the main menu loaded the class:
 * <pre>
 * Mixin apply for mod parcool failed parcool-common.mixins.json:client.KeyboardInputMixin from mod
 *   parcool -&gt; net.minecraft.client.player.KeyboardInput: InvalidInjectionException: Invalid
 *   descriptor on ...-&gt;@Inject::parcool$recordKeys(ZFLorg/spongepowered/asm/mixin/injection/
 *   callback/CallbackInfo;)V! Expected (Lorg/spongepowered/asm/mixin/injection/callback/
 *   CallbackInfo;)V but found (ZFLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V
 * </pre>
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void parcool$recordKeys(CallbackInfo ci) {
        KeyRecorder.onClientTick();
    }
}
