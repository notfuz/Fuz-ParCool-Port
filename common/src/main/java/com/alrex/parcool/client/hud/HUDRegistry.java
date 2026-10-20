package com.alrex.parcool.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Thin adapter between the Architectury HUD event and {@link HUDManager}.
 * <p>
 * Upstream this class was an {@code @SubscribeEvent} listener for
 * {@code RegisterGuiLayersEvent}; the layering itself now happens in {@code ParCoolEvents}.
 */
public class HUDRegistry {
    public static void renderHud(GuiGraphics graphics, DeltaTracker partialTick) {
        HUDManager.getInstance().renderHud(graphics, partialTick);
    }
}
