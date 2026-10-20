package com.alrex.parcool.common.action.impl;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.client.animation.impl.FastSwimAnimator;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.StaminaConsumeTiming;
import com.alrex.parcool.common.data.client.Animation;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.info.ActionInfo;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.platform.PlatformServices;

import java.nio.ByteBuffer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

public class FastSwim extends Action {
    private static final ResourceLocation FAST_SWIM_MODIFIER = ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "modifier.speed.fastswim");
    private double speedModifier = 0;
    private boolean toggleStatus;

    public double getSpeedModifier(ActionInfo info) {
        return Math.min(
                info.getClientSetting().get(ParCoolConfig.Client.Doubles.FastSwimSpeedModifier),
                info.getServerLimitation().get(ParCoolConfig.Server.Doubles.MaxFastSwimSpeedModifier)
        );
    }

    @Override
    public boolean canStart(Player player, Parkourability parkourability, ByteBuffer startInfo) {
        return canContinue(player, parkourability);
    }

    @Override
    public boolean canContinue(Player player, Parkourability parkourability) {
        return (player.isInWaterOrBubble()
                && player.getVehicle() == null
                && !player.isFallFlying()
                && player.isSprinting()
                && player.isSwimming()
                && !parkourability.get(FastRun.class).isDoing()
                && ((ParCoolConfig.Client.getInstance().FastRunControl.get() == FastRun.ControlType.PressKey && KeyBindings.isDown(KeyBindings.getKeyFastRunning()))
                || (ParCoolConfig.Client.getInstance().FastRunControl.get() == FastRun.ControlType.Toggle && toggleStatus)
                || ParCoolConfig.Client.getInstance().FastRunControl.get() == FastRun.ControlType.Auto)
        );
    }

    @Override
    public void onClientTick(Player player, Parkourability parkourability) {
        if (player.isLocalPlayer()) {
            if (ParCoolConfig.Client.getInstance().FastRunControl.get() == FastRun.ControlType.Toggle
                    && parkourability.getAdditionalProperties().getSprintingTick() > 3
                    && player.isInWaterOrBubble()
                    && player.isSwimming()
            ) {
                if (KeyRecorder.keyFastRunning.isPressed())
                    toggleStatus = !toggleStatus;
            } else {
                toggleStatus = false;
            }
        }
    }

    @Override
    public void onWorkingTickInClient(Player player, Parkourability parkourability) {
        Animation animation = Animation.get(player);
        if (animation != null && !animation.hasAnimator()) {
            animation.setAnimator(FastSwimAnimator.class);
        }
    }

    @Override
    public void onStartInServer(Player player, Parkourability parkourability, ByteBuffer startData) {
        speedModifier = parkourability.get(FastSwim.class).getSpeedModifier(parkourability.getActionInfo());
    }

    @Override
    public void onServerTick(Player player, Parkourability parkourability) {
        // null on loaders without the NeoForge swim-speed attribute -> FastSwim speed bonus is a no-op there
        AttributeInstance attr = PlatformServices.get().getSwimSpeedAttribute(player);
        if (attr == null) return;
        if (attr.getModifier(FAST_SWIM_MODIFIER) != null) attr.removeModifier(FAST_SWIM_MODIFIER);
        if (isDoing()) {
            player.setSprinting(true);
            attr.addTransientModifier(new AttributeModifier(
                    FAST_SWIM_MODIFIER,
                    speedModifier / 8d,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @Override
    public StaminaConsumeTiming getStaminaConsumeTiming() {
        return StaminaConsumeTiming.OnWorking;
    }
}
