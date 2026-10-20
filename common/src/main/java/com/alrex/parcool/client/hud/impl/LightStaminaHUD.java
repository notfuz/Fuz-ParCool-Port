package com.alrex.parcool.client.hud.impl;

import com.alrex.parcool.api.Effects;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.data.ParCoolDataKeys;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.data.ReadonlyStamina;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.MathUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

public class LightStaminaHUD {

	/** See the note on {@code baseY} in {@link #render}. */
	public static final int VANILLA_RIGHT_COLUMN_HEIGHT = 49;

	private long lastStaminaChangedTick = 0;
	//1-> recovering, -1->consuming, 0->no changing
	private int lastChangingSign = 0;
	private int changingSign = 0;
	private long changingTimeTick = 0;
	private int randomOffset = 0;
	private boolean justBecameMax = false;

    private float statusValue = 0f;
    private float oldStatusValue = 0f;
    private boolean showStatus = false;
	private int oldValue = 0;

	public void onTick(LocalPlayer player) {
        Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) return;
		var stamina = ParCoolDataKeys.getStamina(player);
		int newValue = stamina.value();
		changingSign = (int) Math.signum(newValue - oldValue);
		final long gameTime = player.getCommandSenderWorld().getGameTime();
		if (changingSign != lastChangingSign) {
			lastChangingSign = changingSign;
			changingTimeTick = 0;
		} else {
			changingTimeTick++;
		}
		if (player.getRandom().nextInt(5) == 0) {
			randomOffset += player.getRandom().nextBoolean() ? 1 : -1;
		} else {
			randomOffset = 0;
		}
		if (newValue != oldValue || stamina.isExhausted()) {
			lastStaminaChangedTick = gameTime;
		}
		justBecameMax = oldValue < newValue && newValue == stamina.max();

        oldStatusValue = statusValue;
        boolean oldShowStatus = showStatus;
        showStatus = false;
        if (ParCoolConfig.Client.Booleans.ShowActionStatusBar.get()) {
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
        }
        if (!oldShowStatus && showStatus) {
            oldStatusValue = statusValue;
        }
		oldValue = newValue;
	}

	public void render(GuiGraphics graphics, Parkourability parkourability, ReadonlyStamina stamina, float partialTick) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		final boolean inexhaustible = player.hasEffect(Effects.INEXHAUSTIBLE);
		final boolean exhausted = stamina.isExhausted();

		if (!showStatus) {
			long gameTime = player.level().getGameTime();
			if (gameTime - lastStaminaChangedTick > 40 && !ParCoolConfig.Client.Booleans.ShowLightStaminaHUDAlways.get())
				return;
		}
		float staminaScale = (float) stamina.value() / stamina.max();
		// A reported max of 0 (or a non-finite value) would divide to Infinity/NaN, and both bounds
		// below are false for NaN, so the guard has to be a range check on the source values.
		if (!Float.isFinite(staminaScale)) staminaScale = 0;
		if (staminaScale < 0) staminaScale = 0;
		if (staminaScale > 1) staminaScale = 1;

        staminaScale *= 10f;
		float statusScale = showStatus ? MathUtil.lerp(oldStatusValue, statusValue, partialTick) * 10f : 0f;

		final int width = graphics.guiWidth();
		final int height = graphics.guiHeight();
        int baseX = width / 2 + 91 + ParCoolConfig.Client.Integers.HorizontalOffsetOfLightStaminaHUD.get();
		// Was `height - Minecraft.getInstance().gui.rightHeight`: `Gui#rightHeight` is a NeoForge-only
		// field and the Architectury HUD event fires *after* the vanilla layers, so the value is not
		// observable any more. VANILLA_RIGHT_COLUMN_HEIGHT is the vanilla right-column height for a
		// player with the default HUD (air + armour + health + food rows) and has been checked in a
		// world on both loaders.
		int baseY = height - VANILLA_RIGHT_COLUMN_HEIGHT + ParCoolConfig.Client.Integers.VerticalOffsetOfLightStaminaHUD.get();
		for (int i = 0; i < 10; i++) {
			int x = baseX - i * 8 - 9;
			int offsetY = 0;
            int textureX;
            if (inexhaustible) {
                if (showStatus) {
                    if (statusScale > i + 0.9f) {
                        textureX = 90;
                    } else {
                        textureX = 0;
                    }
                } else {
                    textureX = 54;
                }
            } else {
                if (exhausted) {
                    textureX = 27;
                } else if (statusScale > i + 0.9f) {
                    textureX = 90;
                } else {
                    textureX = 0;
                }
            }
			if (justBecameMax) {
				textureX = 81;
            } else if (staminaScale < i) {//empty
				textureX += 18;
			} else if (staminaScale < i + 0.5f) {//not full
				textureX += 9;
			}
			if (justBecameMax) {
				offsetY = -1;
			} else if (changingSign == 1) {
				if ((changingTimeTick & 0b11111) == i) {
					offsetY = -1;
				}
			} else if (i + 1 > staminaScale && staminaScale > i && changingSign == -1) {
				offsetY = randomOffset;
			}

			graphics.blit(RenderType::guiTextured, StaminaHUD.STAMINA, x, baseY + offsetY, textureX, 119, 9, 9, 128, 128, -1);
		}
	}
}
