package com.alrex.parcool.client.hud.impl;


import com.alrex.parcool.ParCool;
import com.alrex.parcool.client.hud.Position;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.MathUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Tuple;
import net.minecraft.client.player.LocalPlayer;

public class StaminaHUD {
	public static final ResourceLocation STAMINA = ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "textures/gui/stamina_bar.png");

	public StaminaHUD() {
	}

	private float shadowScale = 1f;
	//0,1,2
	private int renderGageType = 0;
	private int renderGageTick = 0;
    private float statusValue = 0f;
    private float oldStatusValue = 0f;
    private boolean showStatus = false;

	public void onTick(LocalPlayer player) {
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) return;
		if (++renderGageTick >= 5) {
			renderGageTick = 0;
			if (++renderGageType > 2) {
				renderGageType = 0;
			}
		}
        oldStatusValue = statusValue;
        boolean oldShowStatus = showStatus;
        showStatus = false;
        for (Action a : parkourability.getList()) {
            if (a.wantsToShowStatusBar(player, parkourability)) {
                showStatus = true;
                statusValue = a.getStatusValue(player, parkourability);
                if (statusValue > 1f) {
                    statusValue = 1f;
                } else if (statusValue < 0f) {
                    statusValue = 0f;
                }
                break;
            }
        }
        if (!oldShowStatus && showStatus) {
            oldStatusValue = statusValue;
        }
	}

	public void render(GuiGraphics graphics, Parkourability parkourability, ReadonlyStamina stamina, float partialTick) {
		Position position = new Position(
				ParCoolConfig.Client.getInstance().AlignHorizontalStaminaHUD.get(),
				ParCoolConfig.Client.getInstance().AlignVerticalStaminaHUD.get(),
                ParCoolConfig.Client.Integers.HorizontalOffsetOfStaminaHUD.get(),
                ParCoolConfig.Client.Integers.VerticalOffsetOfStaminaHUD.get()
		);
		final int boxWidth = 91;
		final int boxHeight = 17;
		final int width = graphics.guiWidth();
		final int height = graphics.guiHeight();
		final Tuple<Integer, Integer> pos = position.calculate(boxWidth, boxHeight, width, height);

		float staminaScale = (float) stamina.value() / stamina.max();
		float statusScale = showStatus ? MathUtil.lerp(oldStatusValue, statusValue, partialTick) : 0f;

		// A reported max of 0 (or a non-finite value) would divide to Infinity/NaN, and both bounds
		// below are false for NaN, so the guard has to be a range check on the source values.
		if (!Float.isFinite(staminaScale)) staminaScale = 0;
		if (staminaScale < 0) staminaScale = 0;
		if (staminaScale > 1) staminaScale = 1;


    // 1.21.4 removed the convenience overloads that took the texture alone, so every blit has to name
    // its render type: `RenderType::guiTextured` is the vanilla GUI pipeline and the `-1` colour
    // argument is what the old `RenderSystem.setShaderColor(1, 1, 1, 1)` + uncoloured blit produced.
				graphics.blit(RenderType::guiTextured, STAMINA, pos.getA(), pos.getB(), 0, 0, 93, 17, 128, 128, -1);
		if (!stamina.isExhausted()) {
            graphics.blit(RenderType::guiTextured, STAMINA, pos.getA(), pos.getB(), 0, 102, (int) Math.ceil(92 * statusScale), 17, 128, 128, -1);
			graphics.blit(RenderType::guiTextured, STAMINA, pos.getA(), pos.getB(), 0, 85, Math.round(16 + 69 * shadowScale) + 1, 12, 128, 128, -1);
			graphics.blit(RenderType::guiTextured, STAMINA, pos.getA(), pos.getB(), 0, 17 * (renderGageType + 1), Math.round(16 + 69 * staminaScale) + 1, 12, 128, 128, -1);
		} else {
			graphics.blit(RenderType::guiTextured, STAMINA, pos.getA(), pos.getB(), 0, 68, Math.round(16 + 69 * staminaScale) + 1, 17, 128, 128, -1);
		}
		shadowScale = staminaScale - (staminaScale - shadowScale) / 1.1f;
	}
}
