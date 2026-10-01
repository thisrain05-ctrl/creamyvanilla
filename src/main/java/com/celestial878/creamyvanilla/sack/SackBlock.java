package com.celestial878.creamyvanilla.sack;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A falling, waterloggable container block. Behaves like the sack from Supplementaries:
 * it drops when unsupported (hurting entities harder the fuller it is), keeps its contents
 * while falling, and angers piglins when opened or broken.
 */
public class SackBlock extends ColoredFallingBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final VoxelShape SHAPE_CLOSED = Shapes.or(
            Block.box(2, 0, 2, 14, 12, 14),
            Block.box(6, 12, 6, 10, 13, 10),
            Block.box(5, 13, 5, 11, 16, 11));
    public static final VoxelShape SHAPE_OPEN = Shapes.or(
            Block.box(2, 0, 2, 14, 12, 14),
            Block.box(6, 12, 6, 10, 13, 10),
            Block.box(3, 13, 3, 13, 14, 13));

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final ColorRGBA DUST_COLOR = new ColorRGBA(0xba8f6a);
    public static final MapCodec<SackBlock> CODEC = simpleCodec(properties -> new SackBlock(properties));

    public SackBlock(Properties properties) {
        super(DUST_COLOR, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(WATERLOGGED, false));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MapCodec<ColoredFallingBlock> codec() {
        return (MapCodec) CODEC;
    }

    public static boolean canFall(BlockPos pos, LevelAccessor level) {
        return FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN, WATERLOGGED);
    }

    // ---- falling ----

    // Schedule the fall check when a sack is placed, but not on mere state changes such as OPEN toggling.
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (state.getBlock() != oldState.getBlock()) {
            level.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SackBlockEntity sack) {
            sack.recheckOpen();
            if (canFall(pos, level)) {
                // Has to be saved before the block is removed by FallingBlockEntity.fall.
                CompoundTag data = sack.saveWithoutMetadata(level.registryAccess());
                float power = this.getAnalogOutputSignal(state, level, pos) / 15f;
                FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state.setValue(OPEN, false));
                entity.blockData = data;
                entity.setHurtsEntities(1 + power * 5, 40);
            }
        }
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock) {
        super.onLand(level, pos, state, replaceableState, fallingBlock);
        if (!fallingBlock.isSilent()) {
            level.playSound(null, pos, state.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        // in case it landed on something that then disappears
        level.scheduleTick(pos, this, this.getDelayAfterPlace());
    }

    // If the falling sack can't land (e.g. the spot got blocked) vanilla only drops an empty sack item,
    // so hand out what it was carrying.
    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (level.isClientSide || fallingBlock.blockData == null) return;
        NonNullList<ItemStack> items = NonNullList.withSize(SackBlockEntity.MAX_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(fallingBlock.blockData, items, level.registryAccess());
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, fallingBlock.getX(), fallingBlock.getY(),
                        fallingBlock.getZ(), stack);
            }
        }
    }

    // ---- waterlogging ----

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, water);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    // ---- block entity & interaction ----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SackBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (player.isSpectator()) {
            return InteractionResult.CONSUME;
        } else if (level.getBlockEntity(pos) instanceof SackBlockEntity sack && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(sack, buf -> buf.writeInt(sack.getContainerSize()));
            PiglinAi.angerNearbyPiglins(player, true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // Creative players don't get loot table drops, so drop the sack with its contents by hand.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof SackBlockEntity sack) {
            if (!level.isClientSide && player.isCreative() && !sack.isEmpty()) {
                ItemStack stack = new ItemStack(this);
                stack.applyComponents(sack.collectComponents());
                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            } else {
                sack.unpackLootTable(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? SHAPE_OPEN : SHAPE_CLOSED;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        }
        return 0;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MenuProvider provider ? provider : null;
    }
}
