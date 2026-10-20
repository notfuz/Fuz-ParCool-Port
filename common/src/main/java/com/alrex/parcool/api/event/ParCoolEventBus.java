package com.alrex.parcool.api.event;

import com.alrex.parcool.api.client.gui.ParCoolHUDEvent;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.api.unstable.animation.ParCoolAnimationInfoEvent;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

/**
 * The mod's own event API, ported off {@code NeoForge.EVENT_BUS}.
 * <p>
 * The three event families ParCool exposes (action lifecycle, stamina HUD, per-frame animation
 * option) are dispatched here. Each family is backed by an Architectury {@link Event}, so
 * {@code register}/{@code unregister}/{@code clearListeners} behave as on any other Architectury
 * event, while the call sites keep the upstream {@code post(event) -> event} shape that lets them
 * inspect {@link CancellableEvent#isCanceled()} inline.
 */
public final class ParCoolEventBus {

    @FunctionalInterface
    public interface ActionListener {
        void onParCoolActionEvent(ParCoolActionEvent event);
    }

    @FunctionalInterface
    public interface HudListener {
        void onParCoolHudEvent(ParCoolHUDEvent event);
    }

    @FunctionalInterface
    public interface AnimationInfoListener {
        void onParCoolAnimationInfoEvent(ParCoolAnimationInfoEvent event);
    }

    private static final Event<ActionListener> ACTION_EVENT = EventFactory.of(listeners -> event -> {
        for (ActionListener listener : listeners) {
            listener.onParCoolActionEvent(event);
        }
    });

    private static final Event<HudListener> HUD_EVENT = EventFactory.of(listeners -> event -> {
        for (HudListener listener : listeners) {
            listener.onParCoolHudEvent(event);
        }
    });

    private static final Event<AnimationInfoListener> ANIMATION_INFO_EVENT = EventFactory.of(listeners -> event -> {
        for (AnimationInfoListener listener : listeners) {
            listener.onParCoolAnimationInfoEvent(event);
        }
    });

    private ParCoolEventBus() {
    }

    // ------------------------------------------------------------------
    // posting
    // ------------------------------------------------------------------

    public static ParCoolActionEvent post(ParCoolActionEvent event) {
        ACTION_EVENT.invoker().onParCoolActionEvent(event);
        return event;
    }

    public static ParCoolHUDEvent post(ParCoolHUDEvent event) {
        HUD_EVENT.invoker().onParCoolHudEvent(event);
        return event;
    }

    public static ParCoolAnimationInfoEvent post(ParCoolAnimationInfoEvent event) {
        ANIMATION_INFO_EVENT.invoker().onParCoolAnimationInfoEvent(event);
        return event;
    }

    // ------------------------------------------------------------------
    // subscribing
    // ------------------------------------------------------------------

    public static Event<ActionListener> actionEvents() {
        return ACTION_EVENT;
    }

    public static Event<HudListener> hudEvents() {
        return HUD_EVENT;
    }

    public static Event<AnimationInfoListener> animationInfoEvents() {
        return ANIMATION_INFO_EVENT;
    }
}
