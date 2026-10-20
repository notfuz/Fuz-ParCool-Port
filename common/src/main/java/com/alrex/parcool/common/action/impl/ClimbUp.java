package com.alrex.parcool.common.action.impl;

import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.client.animation.impl.ClimbUpAnimator;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.StaminaConsumeTiming;
import com.alrex.parcool.common.data.client.Animation;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;

public class ClimbUp extends Action {
	@Override
    public boolean canStart(Player player, Parkourability parkourability, ByteBuffer startInfo) {
		ClingToCliff cling = parkourability.get(ClingToCliff.class);
		return cling.isDoing()
				&& cling.getDoingTick() > 2
				&& cling.getFacingDirection() == ClingToCliff.FacingDirection.ToWall
				&& KeyRecorder.keyJumpState.isPressed();
	}

	@Override
    public boolean canContinue(Player player, Parkourability parkourability) {
		return getDoingTick() < 2;
	}

	@Override
    public void onStartInLocalClient(Player player, Parkourability parkourability, ByteBuffer startData) {
        Vec3 speed = player.getDeltaMovement();
        player.setDeltaMovement(speed.x(), 0.6, speed.z());
		if (ParCoolConfig.Client.Booleans.EnableActionSounds.get())
            player.playSound(SoundEvents.CLING_TO_CLIFF_JUMP.get(), 1f, 1f);
		Animation animation = Animation.get(player);
		if (animation != null) animation.setAnimator(ClimbUpAnimator.class);
	}

	@Override
	public void onStartInOtherClient(Player player, Parkourability parkourability, ByteBuffer startData) {
        if (ParCoolConfig.Client.Booleans.EnableActionSounds.get())
            player.playSound(SoundEvents.CLING_TO_CLIFF_JUMP.get(), 1f, 1f);
		Animation animation = Animation.get(player);
		if (animation != null) animation.setAnimator(ClimbUpAnimator.class);
	}

	@Override
	public StaminaConsumeTiming getStaminaConsumeTiming() {
		return StaminaConsumeTiming.OnStart;
	}
}
