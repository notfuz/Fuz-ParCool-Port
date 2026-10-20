package com.alrex.parcool.common.handlers;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.HideInBlock;
import com.alrex.parcool.common.action.impl.RideZipline;
import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Decides whether a vanilla interaction must be suppressed while a ParCool action is running.
 *
 * <h2>What upstream did</h2>
 * NeoForge fired {@code InputEvent.InteractionKeyMappingTriggered} from inside the key click handling,
 * carrying both the key mapping that was triggered and whether the interaction was a use or an
 * attack. The handler cancelled use/swing while {@code HideInBlock} was running (unconditionally) and
 * cancelled use for the three wall-grip actions when the triggering key was the action's own key —
 * all three of which are bound to the right mouse button, the same key as vanilla's use/attack.
 *
 * <h2>How it works now</h2>
 * Architectury has no equivalent event and vanilla has no hook inside the key click path, so the
 * decision is applied at the two action entry points instead
 * ({@code mixin.client.MinecraftInputMixin} intercepts {@code startUseItem} and {@code startAttack}).
 * Because ParCool reads its bindings by polling the physical key state, "is the action's own key
 * held" is answered directly, so the check is unconditional for an action that is running — which is
 * the behaviour upstream produced, without touching any key mapping.
 */
public class InputHandler {

    public static boolean shouldSuppressUse() {
        Parkourability parkourability = parkourability();
        if (parkourability == null) return false;

        // HideInBlock vetoed unconditionally upstream.
        if (parkourability.get(HideInBlock.class).isDoing()) return true;

        // The three wall grips only veto the interaction that came from their own key, which is the
        // right mouse button — the same key vanilla uses for use/place. Requiring the key to actually
        // be held reproduces that exactly: if an action ever reports itself as running while its key
        // is up, placement keeps working instead of being blocked forever.
        return isGripping(parkourability, ClingToCliff.class, KeyBindings.getKeyGrabWall())
                || isGripping(parkourability, RideZipline.class, KeyBindings.getKeyRideZipline())
                || isGripping(parkourability, WallSlide.class, KeyBindings.getKeyWallSlide());
    }

    private static boolean isGripping(Parkourability parkourability,
                                      Class<? extends com.alrex.parcool.common.action.Action> type,
                                      net.minecraft.client.KeyMapping key) {
        if (key == null) return false;
        return parkourability.get(type).isDoing() && KeyBindings.isDown(key);
    }

    /** Upstream only suppressed the swing for {@code HideInBlock}. */
    public static boolean shouldSuppressAttack() {
        Parkourability parkourability = parkourability();
        return parkourability != null && parkourability.get(HideInBlock.class).isDoing();
    }

    /**
     * Kept for the public API shape: dispatches a veto for one synthetic interaction. Used by tests
     * and by anything that wants the old "cancel and do not swing" semantics for a given key.
     */
    public static void onInput(com.alrex.parcool.common.event.CompatEvents.InputEvent
                                      .InteractionKeyMappingTriggered event) {
        if (event.isCanceled() || !event.shouldSwingHand()) return;
        if (shouldSuppressUse()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        } else if (!event.isUseItem() && shouldSuppressAttack()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    /** Diagnostic helper: whether the named action currently reports itself as running. */
    public static String state(Class<? extends com.alrex.parcool.common.action.Action> type) {
        Parkourability p = parkourability();
        if (p == null) return "no-parkourability";
        return String.valueOf(p.get(type).isDoing());
    }

    /** @return whether any ParCool binding is physically held right now. */
    public static boolean anyParCoolKeyDown() {
        for (var mapping : new net.minecraft.client.KeyMapping[] {
                KeyBindings.getKeyGrabWall(),
                KeyBindings.getKeyRideZipline(),
                KeyBindings.getKeyWallSlide(),
                KeyBindings.getKeyHideInBlock(),
                KeyBindings.getKeyBreakfall(),
                KeyBindings.getKeyCrawl(),
                KeyBindings.getKeyHangDown(),
        }) {
            if (mapping != null && KeyBindings.isDown(mapping)) return true;
        }
        return false;
    }

    private static Parkourability parkourability() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        return Parkourability.get(player);
    }
}
