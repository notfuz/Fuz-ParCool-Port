package com.alrex.parcool.mixin.client;

import com.alrex.parcool.common.handlers.InputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses the vanilla "use item / place block" and "attack" actions while a ParCool action that
 * upstream vetoed them is running — {@code HideInBlock}, and the {@code ClingToCliff} /
 * {@code RideZipline} / {@code WallSlide} wall grips, whose vanilla binding is the right mouse
 * button.
 *
 * <h2>Why the guard lives here and not on the key mappings</h2>
 * An earlier version suppressed the interaction by draining the click counter and forcing the
 * ParCool {@code KeyMapping} objects down. That was wrong twice over: Minecraft 1.21.1 keeps only one
 * mapping per physical key in {@code KeyMapping.MAP}, so forcing our own mappings down also took the
 * vanilla {@code keyUse} / {@code keyAttack} state with them (they are the same objects that shared
 * map points at), which is exactly what stopped right-click from placing blocks. Intercepting the two
 * action entry points instead leaves every key mapping untouched and only suppresses the two
 * interactions ParCool actually wants to veto.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void parcool$suppressUseItem(CallbackInfo ci) {
        if (InputHandler.shouldSuppressUse()) ci.cancel();
    }

    /** {@code startAttack} returns a boolean, so a cancellable injection needs the returnable callback. */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void parcool$suppressAttack(CallbackInfoReturnable<Boolean> cir) {
        if (InputHandler.shouldSuppressAttack()) cir.setReturnValue(false);
    }
}
