package com.alrex.parcool.mixin.common;

import com.alrex.parcool.common.block.zipline.ZiplineHookTileEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the chunk-unload notification that {@code BlockEntity#onChunkUnloaded()} gave ParCool on
 * NeoForge. Vanilla 1.21.1 has no such callback, so it is injected just before a chunk drops its
 * block entities; the zipline hook tile entity uses it to unlink itself from its partner hooks.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void parcool$notifyBlockEntitiesUnloaded(CallbackInfo ci) {
        LevelChunk self = (LevelChunk) (Object) this;
        for (var entry : self.getBlockEntities().entrySet()) {
            if (entry.getValue() instanceof ZiplineHookTileEntity hook) {
                hook.parcool$onChunkUnloaded();
            }
        }
    }
}
