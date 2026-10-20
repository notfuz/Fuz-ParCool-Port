package com.alrex.parcool.common.event;

import com.alrex.parcool.client.hud.HUDManager;
import com.alrex.parcool.client.hud.HUDRegistry;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.client.action.ClientActionProcessor;
import com.alrex.parcool.common.action.ActionProcessor;
import com.alrex.parcool.common.handlers.EnableOrDisableParCoolHandler;
import com.alrex.parcool.common.handlers.LoginLogoutHandler;
import com.alrex.parcool.common.handlers.OpenSettingsParCoolHandler;
import com.alrex.parcool.common.handlers.PlayerCloneHandler;
import com.alrex.parcool.common.handlers.PlayerDamageHandler;
import com.alrex.parcool.common.handlers.PlayerJoinHandler;
import com.alrex.parcool.common.network.ActionSynchronizationBroadcaster;
import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.common.network.StaminaSynchronizationBroadcaster;
import com.alrex.parcool.server.command.CommandRegistry;
import com.alrex.parcool.server.limitation.Limitations;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;


/**
 * The single place where ParCool subscribes to game events.
 *
 * <p>Replaces the two FML buses that upstream wired up in
 * {@code common.registries.EventBusForgeRegistry} plus the ad-hoc {@code NeoForge.EVENT_BUS}
 * listeners in {@code ParCool}. Everything that Architectury exposes is subscribed here; the seven
 * events Architectury does not mirror are dispatched from vanilla mixins and reach their handlers
 * through {@link CompatEvents}.
 */
public final class ParCoolEvents {

    private static final ActionProcessor ACTION_PROCESSOR = new ActionProcessor();

    public static ActionProcessor actionProcessor() {
        return ACTION_PROCESSOR;
    }

    private ParCoolEvents() {
    }

    /** Both-sides subscriptions. */
    public static void register() {
        // Was: bus.addListener(ACTION_PROCESSOR::onTick) on PlayerTickEvent.Post
        TickEvent.PLAYER_POST.register(ACTION_PROCESSOR::onTick);

        // Was: ActionSynchronizationBroadcaster / StaminaSynchronizationBroadcaster on ServerTickEvent.Post
        TickEvent.SERVER_POST.register(server -> ActionSynchronizationBroadcaster.onTick());
        TickEvent.SERVER_POST.register(server -> StaminaSynchronizationBroadcaster.onTick());

        // Was: PlayerCloneHandler on PlayerEvent.Clone
        PlayerEvent.PLAYER_CLONE.register(PlayerCloneHandler::onClone);
        // Was: LoginLogoutHandler on PlayerEvent.PlayerLoggedOutEvent
        PlayerEvent.PLAYER_QUIT.register(LoginLogoutHandler::onQuit);

        // Was: PlayerDamageHandler#onAttack on LivingIncomingDamageEvent.
        // Architectury's LIVING_HURT fires after vanilla mitigation, so it is only used to veto the
        // hit; PlayerDamageHandler still runs from the vanilla damage pipeline via the same call.
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                if (PlayerDamageHandler.onAttack(player, source)) {
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });

        // Was: RegisterCommandsEvent
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, registryAccess, environment) -> CommandRegistry.register(dispatcher));

        // Was: ServerAboutToStartEvent / ServerStoppingEvent -> Limitations
        // (NetworkRegistries#setServer replaces PacketDistributor's implicit server handle)
        LifecycleEvent.SERVER_STARTING.register(server -> {
            NetworkRegistries.setServer(server);
            Limitations.init(server);
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            Limitations.save(server);
            NetworkRegistries.setServer(null);
        });
    }

    /** Client-only subscriptions. */
    public static void registerClient() {
        // Was: KeyRecorder on MovementInputUpdateEvent -> dispatched by mixin.client.KeyboardInputMixin
        // Was: OpenSettingsParCoolHandler on ClientTickEvent.Pre
        ClientTickEvent.CLIENT_PRE.register(client -> OpenSettingsParCoolHandler.onTick());
        // Was: EnableOrDisableParCoolHandler on ClientTickEvent.Post
        ClientTickEvent.CLIENT_POST.register(client -> EnableOrDisableParCoolHandler.onTick());
        // Was: HUDManager on ClientTickEvent.Post
        ClientTickEvent.CLIENT_POST.register(client -> HUDManager.getInstance().onTick());
        // Animators of *other* players. Their Animator#getTick() used to advance only from
        // TickEvent.PLAYER_POST, which on a client fires for the local player alone - so an action
        // started on a remote player (Dodge, Roll, ClimbUp, Tap, Vault, VerticalWallRun, JumpFromBar,
        // Dive) never left getTick() == 0, shouldRemoved() never fired, and the remote player was
        // stuck in the action's first frame until they repeated it.
        //
        // This has to be a *tick* hook, not the per-frame render hook: ClientActionProcessor
        // #onRenderFrame fires from GameRenderer#renderLevel, i.e. once per rendered frame. Advancing
        // the counters there made every animator run at the frame rate instead of the tick rate - at
        // 200 fps the local player's animators were advanced 200 times a second on top of the 20 real
        // ticks, so the whole mod played roughly 10x too fast.
        ClientTickEvent.CLIENT_PRE.register(ClientActionProcessor::tickRemoteAnimations);

        // Was: PlayerJoinHandler on EntityJoinLevelEvent (client)
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(PlayerJoinHandler::onClientPlayerJoin);
        // Respawn replaces the LocalPlayer object, and the player data (including the client setting
        // snapshot that gates every action) lives on the entity, so the new player starts from the
        // "unsynced" default: ParCool would appear switched off until the world is re-entered.
        // Re-sending the settings on respawn is what PlayerJoinHandler does for a fresh join.
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register(
                (oldPlayer, newPlayer) -> PlayerJoinHandler.onClientPlayerJoin(newPlayer));

        // Was: HUDRegistry on RegisterGuiLayersEvent -> HUDManager#registerAbove(food_level)
        ClientGuiEvent.RENDER_HUD.register(HUDRegistry::renderHud);

        // Was: InputHandler on InputEvent.InteractionKeyMappingTriggered
        //        -> dispatched by mixin.client.MinecraftInputMixin
        // Was: ACTION_PROCESSOR::onRenderTick on RenderFrameEvent.Pre
        //        -> dispatched by mixin.client.GameRendererTickMixin
        // Was: ACTION_PROCESSOR::onViewRender on ViewportEvent.ComputeCameraAngles
        //        -> dispatched by mixin.client.CameraAnglesMixin
        // Was: ActionStatePayload / PlayerDamageHandler / PlayerJumpHandler
        //        -> dispatched by their vanilla mixins
    }
}
