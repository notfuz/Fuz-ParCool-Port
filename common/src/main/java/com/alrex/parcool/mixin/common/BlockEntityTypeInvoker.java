package com.alrex.parcool.mixin.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@code BlockEntityType}'s private factory.
 *
 * <p>1.21.4 removed {@code BlockEntityType.Builder} and left {@code register(String, factory, blocks)}
 * private, so a mod has no public way to build a type - while {@code LevelChunk#setBlockState} still
 * creates block entities through {@code EntityBlock#newBlockEntity}, i.e. through the block, and
 * {@code BlockEntity} still carries its {@code BlockEntityType} for the network and validity checks.
 *
 * <p>This is a plain static invoker, so it behaves identically on Fabric and NeoForge and needs no
 * platform seam. It is only ever called from {@code TileEntities}' supplier, i.e. once per type, at
 * the same point in the lifecycle the removed builder was used.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeInvoker {

    @Invoker("register")
    static <T extends BlockEntity> BlockEntityType<T> parcool$register(
            String name, BlockEntityType.BlockEntitySupplier<? extends T> factory, Block... blocks) {
        throw new AssertionError();
    }
}
