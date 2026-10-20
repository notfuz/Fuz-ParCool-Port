package com.alrex.parcool.mixin.client;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.compat.IKeyMappingDuck;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Implements {@link IKeyMappingDuck} by shadowing {@code KeyMapping#key}. */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements IKeyMappingDuck {

    @Shadow
    private InputConstants.Key key;

    @Override
    public InputConstants.Key parcool$getKey() {
        return this.key;
    }

    /**
     * Hands every shared key back to vanilla right after {@code KeyMapping.MAP} is rebuilt.
     *
     * <h2>Why the repair cannot wait for a tick</h2>
     * {@code resetMapping()} refills {@code MAP} from {@code ALL}, a {@code HashMap}, in its iteration
     * order - so which mapping wins a shared key is decided by the hash of its name, not by who owns
     * the action. The ParCool names that default to the right mouse button ({@code key.parcool.Vault}
     * and friends) can therefore end up owning right-click; vanilla's {@code keyUse} then receives
     * neither the click nor the press events at all, and eating and block placing simply stop working.
     *
     * <p>That rebuild happens in exactly two places: {@code Options#load()} (every start) and
     * {@code KeyBindsList#resetMappingAndUpdateButtons()} - the latter is what runs when a player
     * changes <em>any</em> key in the Controls screen, which is precisely the reported case of binding
     * an action to the right mouse button and losing right-click for the whole session (the screen only
     * stops asking for the repair once a world has been loaded, because the old repair path ran from
     * the client tick).
     *
     * <p>Repairing here covers both call sites, leaves the vanilla code untouched, and is a no-op on
     * NeoForge, where {@code MAP} is a {@code KeyMappingLookup} that stores every mapping of a key.
     */
    @Inject(method = "resetMapping", at = @At("TAIL"))
    private static void parcool$restoreSharedVanillaKeys(CallbackInfo ci) {
        KeyBindings.restoreVanillaBindings();
    }
}
