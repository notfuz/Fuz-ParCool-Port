package com.alrex.parcool.common.handlers;

import com.alrex.parcool.common.action.impl.HideInBlock;
import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerVisibilityHandler {
    /**
     * Was {@code LivingEvent.LivingVisibilityEvent} (which multiplied the visibility score by
     * 0.1). NeoForge's variant had no Architectury counterpart, so the hiding player is now hidden
     * outright from the viewer: {@code mixin.common.EntityVisibilityMixin} injects into
     * {@code Entity#canBeSeenByOthers} and returns {@code false} while this returns true.
     */
    public static boolean isHiddenFrom(Entity viewer, Player target) {
        Parkourability parkourability = Parkourability.get(target);
        if (parkourability == null) return false;
        return parkourability.get(HideInBlock.class).isDoing();
    }
}
