package com.alrex.parcool.client.action;

import com.alrex.parcool.client.stamina.StaminaOps;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.data.ParCoolDataKeys;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.data.client.Animation;
import com.alrex.parcool.common.data.client.LocalStamina;
import com.alrex.parcool.common.event.CompatEvents;
import com.alrex.parcool.config.ParCoolConfig;
import java.util.List;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

/**
 * The client-only half of {@link com.alrex.parcool.common.action.ActionProcessor}.
 *
 * <h2>Why it is a separate class</h2>
 * {@code ActionProcessor} is instantiated on a dedicated server (it is the single
 * {@code TickEvent.PLAYER_POST} listener) and NeoForge's {@code RuntimeDistCleaner} aborts the server
 * as soon as a class loaded there calls a member on a {@code @OnlyIn(Dist.CLIENT)} receiver. Every
 * call that needs {@code Minecraft}, {@code LocalPlayer} or the animation/stamina caches therefore
 * lives here, and {@code ActionProcessor} only calls into it with server-safe parameter types.
 */
public final class ClientActionProcessor {

    /** Cooldown between two stamina reports to the server, in ticks. */
    private static int staminaSyncCoolTimeTick = 5;

    private ClientActionProcessor() {
    }

    public static void preprocess(Player player, Parkourability parkourability) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;
        Animation animation = Animation.get(clientPlayer);
        if (animation == null) return;
        animation.tick(clientPlayer, parkourability);
    }

    public static void postProcess(Player player, Parkourability parkourability) {
        if (!player.isLocalPlayer()) return;
        LocalPlayer localPlayer = (LocalPlayer) player;
        if (!parkourability.limitationIsNotSynced()) {
            LocalStamina.get(localPlayer).onTick(localPlayer);
            if (--staminaSyncCoolTimeTick <= 0) {
                StaminaOps.sync(localPlayer, ParCoolDataKeys.getStamina(localPlayer));
                staminaSyncCoolTimeTick = 5;
            }
        }

        boolean penalise = ParCoolConfig.Client.Booleans.EnableStaminaExhaustionPenalty.get()
                && LocalStamina.get(localPlayer).imposeExhaustionPenalty(localPlayer);
        if (penalise) {
            player.setSprinting(false);
            AttributeInstance attribute = localPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
            if (!attribute.hasModifier(STAMINA_DEPLETED_SLOWNESS_MODIFIER_ID)) {
                attribute.addTransientModifier(STAMINA_DEPLETED_SLOWNESS_MODIFIER);
            }
        } else {
            AttributeInstance attribute = localPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
            attribute.removeModifier(STAMINA_DEPLETED_SLOWNESS_MODIFIER_ID);
        }
    }

    public static void consumeStamina(Player player, int value) {
        if (!player.isLocalPlayer()) return;
        LocalPlayer localPlayer = (LocalPlayer) player;
        LocalStamina.get(localPlayer).consume(localPlayer, value);
    }

    /**
     * Advances the animators of every visible player except the local one.
     *
     * <p>Registered on {@code ClientTickEvent.CLIENT_PRE}, i.e. exactly once per game tick - the same
     * rate at which {@link #preprocess} advances the local player's. It must stay a tick hook: an
     * earlier revision did this from {@link #onRenderFrame}, which runs once per rendered frame, and
     * that advanced the local player's animators a second time at the frame rate, so with a 200 fps
     * display the entire mod ran roughly ten times too fast.
     *
     * <p>Remote-only, because the local player already gets its tick from {@code preprocess}.
     */
    public static void tickRemoteAnimations(Minecraft client) {
        Player localPlayer = client.player;
        if (localPlayer == null || !(localPlayer.level() instanceof ClientLevel level)) return;
        for (Player player : level.players()) {
            if (player.isLocalPlayer() || !(player instanceof AbstractClientPlayer avatarPlayer)) continue;
            Parkourability parkourability = Parkourability.get(player);
            if (parkourability == null) continue;
            Animation animation = Animation.get(avatarPlayer);
            if (animation == null) continue;
            animation.tick(avatarPlayer, parkourability);
        }
    }

    public static void onRenderFrame(CompatEvents.RenderFrameEvent.Pre event) {
        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) return;
        for (Player player : clientPlayer.level().players()) {
            Parkourability parkourability = Parkourability.get(player);
            // continue, not return: one player without ParCool data must not stop the render update of
            // every other player in the world.
            if (parkourability == null) continue;
            List<Action> actions = parkourability.getList();
            for (Action action : actions) {
                action.onRenderTick(event, player, parkourability);
            }
            Animation animation = Animation.get(player);
            if (animation == null) continue;
            // Animation#tick is deliberately NOT called here. This hook fires once per rendered frame
            // (GameRenderer#renderLevel), not once per tick, so advancing the counters here ran every
            // animator at the frame rate. Their tick lives in #tickRemoteAnimations (client tick) and
            // in #preprocess (local player) instead.
            animation.onRenderTick(event, player, parkourability);
        }
    }

    public static void onViewRender(CompatEvents.ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) return;
        Animation animation = Animation.get(player);
        if (animation == null) return;
        animation.cameraSetup(event, player, parkourability);
    }

    private static final ResourceLocation STAMINA_DEPLETED_SLOWNESS_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(com.alrex.parcool.ParCool.MOD_ID, "exhausted.speed");

    private static final AttributeModifier STAMINA_DEPLETED_SLOWNESS_MODIFIER = new AttributeModifier(
            STAMINA_DEPLETED_SLOWNESS_MODIFIER_ID,
            -0.05,
            AttributeModifier.Operation.ADD_VALUE
    );
}
