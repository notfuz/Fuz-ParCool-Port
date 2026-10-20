package com.alrex.parcool.mixin.common;

import com.alrex.parcool.common.data.ParCoolData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Persists the persistent {@link com.alrex.parcool.common.data.DataKey}s into the player save file.
 * <p>
 * This is the loader-agnostic stand-in for NeoForge's
 * {@code AttachmentType#serialize(Codec)}, which stored attachment data in the entity's own NBT.
 * Only keys registered as persistent are touched, so action/render state stays transient exactly as
 * before.
 */
@Mixin(Player.class)
public abstract class ParCoolDataPersistenceMixin {

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void parcool$saveData(CompoundTag tag, CallbackInfo ci) {
        ParCoolData.saveToTag((Player) (Object) this, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void parcool$loadData(CompoundTag tag, CallbackInfo ci) {
        ParCoolData.loadFromTag((Player) (Object) this, tag);
    }
}
