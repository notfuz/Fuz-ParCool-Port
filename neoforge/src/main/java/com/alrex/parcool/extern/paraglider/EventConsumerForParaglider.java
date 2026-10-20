package com.alrex.parcool.extern.paraglider;

import com.alrex.parcool.api.event.ParCoolEventBus;
import com.alrex.parcool.api.unstable.animation.ParCoolAnimationInfoEvent;

/**
 * Was an {@code @SubscribeEvent} listener on NeoForge's bus. It now registers itself on
 * {@link ParCoolEventBus}, the loader-agnostic replacement for ParCool's own event families.
 */
public class EventConsumerForParaglider {

    private EventConsumerForParaglider() {
    }

    public static void register() {
        ParCoolEventBus.animationInfoEvents().register(EventConsumerForParaglider::onUpdateAnimateInfo);
    }

    public static void onUpdateAnimateInfo(ParCoolAnimationInfoEvent event) {
        if (ParagliderManager.getInstance().isFallingWithParaglider(event.getPlayer())) {
            event.getOption().cancelAnimation();
        }
    }
}
