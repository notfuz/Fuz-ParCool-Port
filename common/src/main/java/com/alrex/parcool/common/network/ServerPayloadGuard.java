package com.alrex.parcool.common.network;

import com.alrex.parcool.common.stamina.StaminaType;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side trust boundary for the client-authoritative payloads.
 *
 * <h2>Why this exists</h2>
 * Four of the eight payloads are sent client to server and were accepted verbatim upstream:
 * <ul>
 *     <li>{@code action_state} carried a {@code playerID} that the server rebroadcast to every client,
 *     so a modified client could publish action state for <i>any</i> player.</li>
 *     <li>{@code client_info} likewise broadcast an unvalidated {@code playerID} plus a
 *     {@code ClientSetting}, letting a client overwrite every other player's client settings on every
 *     client.</li>
 *     <li>{@code stamina} wrote the reported value into the server-side stamina slot of whoever the
 *     {@code playerID} named, not the sender.</li>
 *     <li>{@code custom_stamina} let the client pick the {@link StaminaType}, i.e. which
 *     {@code IParCoolStaminaHandler} the server instantiates, and the amount that handler then
 *     applies — for example an unbounded {@code causeFoodExhaustion} or an EpicFight stamina drain.</li>
 * </ul>
 *
 * <p>The guard keeps the wire format and the client-authoritative design intact (ParCool's
 * gameplay depends on the client reporting its own action/stamina state) while making the server
 * use the sender's own identity and the server-side configuration as the authority.
 */
public final class ServerPayloadGuard {

    /**
     * Upper bound for a single reported exhaustion request. Upstream applied
     * {@code value / 1000f} exhaustion units with no cap; {@code value} is a VAR_INT, so a modified
     * client could request an arbitrary amount.
     */
    public static final int MAX_STAMINA_PROCESS_VALUE = 10_000;

    /**
     * How many stamina-processing packets one player may send per second. The legitimate client sends
     * one per tick, so a budget of a few per tick leaves room for retransmits while making a packet
     * flood pointless; the per-packet cap alone does not bound the *rate*.
     */
    public static final int STAMINA_PROCESS_PER_SECOND = 40;

    /** Per-player packet budget, keyed by UUID so it survives the player object being replaced. */
    private static final Map<UUID, Token> STAMINA_PROCESS_BUDGET = new ConcurrentHashMap<>();

    private static final class Token {
        long windowStart = System.currentTimeMillis();
        int used;
    }

    private ServerPayloadGuard() {
    }

    /**
     * @return {@code false} when the sender is over its budget, in which case the packet is dropped.
     */
    public static boolean allowStaminaProcess(Player player) {
        Token token = STAMINA_PROCESS_BUDGET.computeIfAbsent(player.getUUID(), id -> new Token());
        long now = System.currentTimeMillis();
        synchronized (token) {
            if (now - token.windowStart >= 1000L) {
                token.windowStart = now;
                token.used = 0;
            }
            if (++token.used > STAMINA_PROCESS_PER_SECOND) return false;
        }
        if (STAMINA_PROCESS_BUDGET.size() > 1024) {
            // Keep the table from growing with every player who ever connected.
            STAMINA_PROCESS_BUDGET.keySet()
                    .removeIf(id -> now - STAMINA_PROCESS_BUDGET.get(id).windowStart > 60_000L);
        }
        return true;
    }

    /**
     * Clamps a client-reported stamina into what the server allows.
     *
     * <p>Both bounds are compared against a value that may be NaN, and every comparison with NaN is
     * false, so a raw NaN would pass an {@code if (v < min)} / {@code if (v > max)} pair unchanged;
     * the result would be stored and later poison the HUD's own divisions.
     */
    public static int clampReportedStamina(int value, int max, int min) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * @return {@code true} when the payload may be trusted, i.e. it describes the sending player.
     */
    public static boolean isSelf(Player sender, @Nullable UUID claimedPlayerId) {
        return claimedPlayerId != null && claimedPlayerId.equals(sender.getUUID());
    }

    /**
     * Resolves the target of a client-reported payload to the sender itself.
     *
     * @return the sender, or {@code null} when the payload claimed somebody else and was rejected.
     */
    @Nullable
    public static Player resolveSelf(Player sender, @Nullable UUID claimedPlayerId) {
        return isSelf(sender, claimedPlayerId) ? sender : null;
    }

    /**
     * A client may only ask the server to process a stamina type the server actually offers: when
     * the server forces one, that is the only accepted value; otherwise the client may pick, but
     * not {@code NONE} (which would be a request to do nothing with a payload).
     */
    public static boolean isStaminaTypeAllowed(StaminaType requested, StaminaType forcedByServer) {
        if (forcedByServer != StaminaType.NONE) {
            return requested == forcedByServer;
        }
        return requested != StaminaType.NONE;
    }

    /** Clamps a client-reported stamina-processing amount into a sane range. */
    public static int clampStaminaProcessValue(int value) {
        if (value < 0) return 0;
        return Math.min(value, MAX_STAMINA_PROCESS_VALUE);
    }

    /** The stamina type the server forces on every player, or {@link StaminaType#NONE} if it does not. */
    public static StaminaType forcedStaminaType() {
        return ParCoolConfig.Server.getInstance().StaminaType.get();
    }
}
