package com.alrex.parcool.client.hud;

import com.alrex.parcool.client.hud.impl.StaminaHUDController;
import com.alrex.parcool.common.data.ParCoolDataKeys;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

public class HUDManager {
    private static HUDManager instance = null;

    private final StaminaHUDController staminaHUD = new StaminaHUDController();

    public static HUDManager getInstance() {
        if (instance == null) instance = new HUDManager();
        return instance;
    }

    /**
     * Was {@code RegisterGuiLayersEvent#registerAbove(minecraft:food_level, ...)}.
     * <p>
     * Architectury exposes the HUD as {@link ClientGuiEvent.RENDER_HUD} rather than as a registered
     * layer, so the layer is driven manually here. Consequence: the stamina bar now draws <i>after</i>
     * every vanilla layer instead of directly above the hunger bar. {@code StaminaHUDController}
     * already positions itself from the screen size and the vanilla right-column height, so the
     * drawing is unchanged; only the z-order of overlapping elements differs.
     */
    public void renderHud(GuiGraphics graphics, DeltaTracker partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (ParCoolDataKeys.getParkourability(player) == null) return;
        this.staminaHUD.render(graphics, partialTick);
    }

    public void onTick() {
        this.staminaHUD.onTick();
    }
}
