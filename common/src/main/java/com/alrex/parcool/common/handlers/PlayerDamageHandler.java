package com.alrex.parcool.common.handlers;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.api.event.ParCoolEventBus;
import com.alrex.parcool.common.event.CompatEvents;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.common.network.payload.StartBreakfallEventPayload;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.WorldUtil;
import com.alrex.parcool.common.network.NetworkRegistries;
import com.alrex.parcool.api.event.ParCoolEventBus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;


public class PlayerDamageHandler {
    /**
     * Was {@code LivingIncomingDamageEvent}. Returns {@code true} to cancel the incoming hit.
     * Called from {@code ParCoolEvents} (Architectury's {@code EntityEvent.LIVING_HURT}).
     */
    public static boolean onAttack(LivingEntity entity, DamageSource source) {
        if (entity instanceof Player player) {
            Parkourability parkourability = Parkourability.get(player);
            if (parkourability == null) return false;
            Dodge dodge = parkourability.get(Dodge.class);
            if (dodge.isDoing()) {
                if (!parkourability.getServerLimitation().get(ParCoolConfig.Server.Booleans.DodgeProvideInvulnerableFrame))
                    return false;
                if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return false;
                return dodge.getDoingTick() <= 10;
            }
        }
        return false;
    }

    /**
     * Was {@code LivingFallEvent}. Dispatched from {@code mixin.common.LivingEntityFallMixin}, which
     * honours {@link CompatEvents.LivingFallEvent#isCanceled()} and applies the damage multiplier.
     */
    public static void onFall(CompatEvents.LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            Parkourability parkourability = Parkourability.get(player);

			if (parkourability.get(BreakfallReady.class).isDoing()
					&& (parkourability.getActionInfo().can(Tap.class)
					|| parkourability.getActionInfo().can(Roll.class))
			) {
				boolean justTime = parkourability.get(BreakfallReady.class).getDoingTick() < parkourability.getLimitedValue(
						ParCoolConfig.Client.Integers.JustTimeBreakfallTick,
						ParCoolConfig.Server.Integers.MaxJustTimeBreakfallTick
				);
				float distance = event.getDistance();
				if (distance > parkourability.getLimitedValue(
						ParCoolConfig.Client.Doubles.LowestFallDistanceForBreakfall,
						ParCoolConfig.Server.Doubles.MinLowestFallDistanceForBreakfall
				)) {
                    NetworkRegistries.sendToPlayer(player, StartBreakfallEventPayload.CODEC, new StartBreakfallEventPayload(justTime));
				} else {
					return;
				}
				double damageRemoveHeight = parkourability.getLimitedValue(
						ParCoolConfig.Client.Doubles.DamageCompleteRemovableHeightBreakfall,
						ParCoolConfig.Server.Doubles.MaxDamageCompleteRemovableHeightBreakfall
				);
				if (distance < damageRemoveHeight || (justTime && distance < damageRemoveHeight * 1.34)) {
					event.setCanceled(true);
				} else {
					float damageReductionRate = (float) parkourability.getLimitedValue(
							ParCoolConfig.Client.Doubles.DamageReductionRateBreakfall,
							ParCoolConfig.Server.Doubles.MaxDamageReductionRateBreakfall
					);
					event.setDamageMultiplier(event.getDamageMultiplier() * (justTime ? 0.66f * damageReductionRate : damageReductionRate));
				}
			} else {
				HideInBlock hideInBlock = parkourability.get(HideInBlock.class);
				if (hideInBlock.isStandbyInAir(parkourability)
						&& parkourability.getActionInfo().can(HideInBlock.class)
						&& !ParCoolEventBus.post(new ParCoolActionEvent.TryToStartEvent(player, hideInBlock)).isCanceled()
						&& !ParCoolEventBus.post(new ParCoolActionEvent.TryToStart(player, hideInBlock)).isCanceled()
				) {
					Tuple<BlockPos, BlockPos> area = WorldUtil.getHideAbleSpace(player, new BlockPos(player.blockPosition().below()));
					if (area != null) {
						boolean stand = player.getBbHeight() < (Math.abs(area.getB().getY() - area.getA().getY()) + 1);
						if (!stand) {
							if (event.getDistance() < 10) {
								event.setCanceled(true);
							} else {
								event.setDamageMultiplier(event.getDamageMultiplier() * 0.4f);
							}
						}
					}
				}
			}
		} else if (event.getEntity() instanceof Player player) {
			if (!player.isLocalPlayer()) {
				return;
			}
			Parkourability parkourability = Parkourability.get(player);
			if (parkourability.getAdditionalProperties().getNotLandingTick() > 5 && event.getDistance() < 0.4f) {
				parkourability.get(ChargeJump.class).onLand(player, parkourability);
			}
		}
	}
}
