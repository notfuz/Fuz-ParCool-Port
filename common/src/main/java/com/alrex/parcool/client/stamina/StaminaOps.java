package com.alrex.parcool.client.stamina;

import com.alrex.parcool.api.Attributes;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.common.network.payload.StaminaPayload;
import net.minecraft.client.player.LocalPlayer;

/**
 * The two {@link LocalPlayer}-bound operations of {@link ReadonlyStamina}.
 *
 * <h2>Why they are not methods on the record</h2>
 * {@code ReadonlyStamina} is loaded on a dedicated server (it is the payload codec and the value
 * stored in the player NBT), so every method of that class is verified there. A method that calls a
 * member on a {@code @OnlyIn(Dist.CLIENT)} type forces NeoForge's {@code RuntimeDistCleaner} to load
 * {@code LocalPlayer} and abort the server with "Attempted to load class
 * net.minecraft.client.player.LocalPlayer for invalid dist DEDICATED_SERVER". Keeping the two
 * client-only operations in a class only the client loads removes that edge.
 */
public final class StaminaOps {

    private StaminaOps() {
    }

    /** Re-clamps the stamina maximum against the player's attribute and the server limitation. */
    public static ReadonlyStamina updateMax(LocalPlayer player, ReadonlyStamina current) {
        var attr = player.getAttribute(Attributes.MAX_STAMINA);
        if (attr == null) return current;
        var parkourability = Parkourability.get(player);
        if (parkourability == null) return current;
        int newMax = (int) Math.min(Math.floor(attr.getValue()),
                parkourability.getActionInfo().getMaxStaminaLimit());
        if (current.max() == newMax) return current;
        return new ReadonlyStamina(current.isExhausted(), current.value(), newMax);
    }

    /**
     * The recovery step of {@code ParCoolStaminaHandler}. It lives here for the same reason
     * {@link #updateMax} does: the handler is instantiated on a dedicated server (it is one of the
     * {@code StaminaType} factories) and the {@code player.getAttribute(...)} call would force the
     * verifier to load {@code LocalPlayer}.
     */
    public static ReadonlyStamina recoverTick(LocalPlayer player, ReadonlyStamina current,
                                               int recoveryCoolDown) {
        // Upstream: `if (recoveryCoolDown <= 0) { ...recover... }` — the cooldown *blocks* recovery
        // while it counts down, so recovery runs only once it has expired. An earlier refactor of this
        // method inverted the condition, which meant stamina was only regenerated during the 30-tick
        // cooldown right after spending it, and then stopped for good.
        if (recoveryCoolDown > 0) {
            return current;
        }
        var parkourability = Parkourability.get(player);
        if (parkourability == null) return current;
        var attr = player.getAttribute(Attributes.STAMINA_RECOVERY);
        if (attr == null) return current;
        int recoverValue = (int) Math.min(parkourability.getActionInfo().getStaminaRecoveryLimit(), attr.getValue());
        return player.onGround()
                ? current.recovered(recoverValue)
                : current.recovered(recoverValue / 5);
    }



    /** Reports the local stamina value to the server. */
    public static void sync(LocalPlayer player, ReadonlyStamina stamina) {
        NetworkRegistries.sendToServer(StaminaPayload.CODEC, new StaminaPayload(player.getUUID(), stamina));
    }
}
