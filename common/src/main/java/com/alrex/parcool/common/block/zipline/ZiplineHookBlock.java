package com.alrex.parcool.common.block.zipline;

import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.common.block.TileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class ZiplineHookBlock extends DirectionalBlock implements EntityBlock {

    public ZiplineHookBlock(Properties p_i48440_1_) {
        // Was `getPistonPushReaction(BlockState) -> DESTROY`, a NeoForge-only override. Vanilla
        // reads the push reaction from the block properties, so it is declared here instead.
        super(p_i48440_1_.pushReaction(PushReaction.DESTROY));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    public Vec3 getActualZiplinePoint(BlockPos pos, BlockState state) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        return this.defaultBlockState().setValue(FACING, direction);
    }

    /**
     * Drops the rope items a broken hook was holding.
     *
     * <p>{@code BlockBehaviour#onRemove} is still overridden here on purpose: 1.21.4 keeps the hook
     * (vanilla only removed this method in 1.21.9, which is why the 1.21.11 port has no override at
     * all). Without it a hook that is broken while a rope is attached would delete the rope silently,
     * because the block entity - and with it the connection list - is destroyed together with the
     * block and nothing would ever hand the items back.
     */
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos,
                         @Nonnull BlockState oldState, boolean movedByPiston) {
        if (!world.isClientSide()) {
            var tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof ZiplineHookTileEntity ziplineHookTileEntity) {
                List<ItemStack> itemStacks = ziplineHookTileEntity.removeAllConnection();
                itemStacks.forEach(it -> Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), it));
            }
        }
        super.onRemove(state, world, pos, oldState, movedByPiston);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader levelAccessor, ScheduledTickAccess scheduledTicks,
                                  BlockPos pos, Direction direction, BlockPos pos1, BlockState state1, RandomSource random) {
        Direction facing = state.getValue(FACING);
        return direction == facing.getOpposite() && !canSurvive(state, levelAccessor, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, levelAccessor, scheduledTicks, pos, direction, pos1, state1, random);
    }


    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportingBlock = pos.relative(facing.getOpposite());
        return canSupportCenter(world, supportingBlock, facing);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
        if (stack.getItem() instanceof ShearsItem) {
            var tileEntity = player.level().getBlockEntity(pos);
            if (tileEntity instanceof ZiplineHookTileEntity ziplineHookTileEntity) {
                if (ziplineHookTileEntity.getConnectionPoints().isEmpty())
                    return InteractionResult.PASS;

                List<ItemStack> itemStacks = ziplineHookTileEntity.removeAllConnection();
                if (!itemStacks.isEmpty()) {
                    player.playSound(SoundEvents.ZIPLINE_REMOVE.get(), 1, 1);
                }
                if (player.level().isClientSide()) {
                    return InteractionResult.SUCCESS;
                } else {
                    itemStacks.forEach((it) -> Containers.dropItemStack(player.level(), pos.getX(), pos.getY(), pos.getZ(), it));
                    if (!itemStacks.isEmpty()) {
                        if (stack.isDamageableItem()) {
                            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                        }
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos blockPos, @Nonnull BlockState blockState) {
        return new ZiplineHookTileEntity(TileEntities.ZIPLINE_HOOK.get(), blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return type == TileEntities.ZIPLINE_HOOK.get() ? ZiplineHookTileEntity::tick : null;
    }
}
