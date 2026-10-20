package com.alrex.parcool.mixin.client;


import com.alrex.parcool.common.action.BehaviorEnforcer;
import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Deliberately does not extend AbstractClientPlayer: a mixin class that extends the target's own
// supertype forces mixin to validate the inherited constructor at load time, and none of the two handlers
// below needs it. The cast at the use site is explicit instead.
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
    public void onIsShiftKeyDown(CallbackInfoReturnable<Boolean> cir) {
        Parkourability parkourability = Parkourability.get((Player) (Object) this);

        if (parkourability == null) return;
        if (parkourability.getBehaviorEnforcer().cancelSneak()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Guards the re-entrant {@code Entity#move} call below.
     *
     * <p>{@code LocalPlayer} does override {@code move}, and the {@code ((Entity) player).move(…)} call
     * that applies the enforced point is a virtual dispatch, so it lands right back in this injection
     * at {@code move}'s HEAD. The enforcer keeps answering "yes" for as long as it lives, so without a
     * guard the two are mutual recursion and the client dies with a {@code StackOverflowError} the
     * first time an action enforces a movement point:
     *
     * <pre>
     * at ...LocalPlayerMixin.onMove      (this handler)
     * at ...LocalPlayer.move
     * at ...LocalPlayerMixin.onMove      (this handler)
     * at ...LocalPlayer.move
     * ...
     * </pre>
     *
     * <p>The flag lives in {@link BehaviorEnforcer}, not here. An earlier revision declared it as a
     * {@code @Unique private static} field on this mixin, which does not work: Mixin does not carry
     * such a field into the target, and the exported mixed class contained no reference to it at all,
     * so the guard was compiled away in effect and the recursion came straight back. The guard only
     * silences this handler, so the nested pass still reaches {@code EntityMixin}, which is the
     * position enforcer (HideInBlock) and has to keep working there.
     */
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void onMove(MoverType type, Vec3 pos, CallbackInfo ci) {
        if (BehaviorEnforcer.isApplyingEnforcedMove()) return;
        var player = (LocalPlayer) (Object) this;
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) return;
        if (type != MoverType.SELF) return;
        var enforcedMovePos = parkourability.getBehaviorEnforcer().getEnforcedMovePoint();
        if (enforcedMovePos != null) {
            ci.cancel();
            var dMove = enforcedMovePos.subtract(player.position());
            player.setDeltaMovement(dMove);
            // Entity#move, reached through the target: super.move(…) is only spellable from a mixin
            // class that extends the target's own supertype. This is the call that re-enters the
            // injection, hence the guard.
            BehaviorEnforcer.setApplyingEnforcedMove(true);
            try {
                ((Entity) player).move(type, dMove);
            } finally {
                BehaviorEnforcer.setApplyingEnforcedMove(false);
            }
        }
    }

}
