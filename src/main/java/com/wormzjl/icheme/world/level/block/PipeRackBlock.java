package com.wormzjl.icheme.world.level.block;

import com.mojang.serialization.MapCodec;
import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.world.level.block.entity.PipeRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A pipe rack cell (batch 2026-10-26-pipe-laying-system, plan 5.4): a thin frame along its axis that carries up to five
 * laid lanes ({@link PipeRackBlockEntity}). The axis orients the frame and makes steps along it cheap for the route
 * search; the lanes do not depend on it. A rack that carries lanes is not broken (owner decision D9): its pipes are
 * dismantled first, with the laying tool.
 */
public final class PipeRackBlock extends BaseEntityBlock {
    public static final MapCodec<PipeRackBlock> CODEC = simpleCodec(PipeRackBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public PipeRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<PipeRackBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /**
     * In line with a rack next door that runs through this cell; otherwise along the player's horizontal facing, or along
     * their look when they sneak (a vertical rack).
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var at = context.getClickedPos();
        for (var direction : Direction.values()) {
            var neighbour = level.getBlockState(at.relative(direction));
            if (neighbour.getBlock() instanceof PipeRackBlock && neighbour.getValue(AXIS) == direction.getAxis()) return defaultBlockState().setValue(AXIS, direction.getAxis());
        }
        var axis = context.isSecondaryUseActive() ? context.getNearestLookingDirection().getAxis() : context.getHorizontalDirection().getAxis();
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new PipeRackBlockEntity(position, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return level.getBlockEntity(position) instanceof PipeRackBlockEntity rack ? rack.shape() : PipeRackBlockEntity.frame(state.getValue(AXIS).ordinal());
    }

    /** A loaded line next door changed: the lanes redraw which faces it meets. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos position, BlockPos neighbourPosition) {
        if (level instanceof ServerLevel server) server.getServer().execute(() -> {
            if (server.getBlockEntity(position) instanceof PipeRackBlockEntity rack && rack.refreshJoined()) server.sendBlockUpdated(position, rack.getBlockState(), rack.getBlockState(), 2);
        });
        return super.updateShape(state, direction, neighbour, level, position, neighbourPosition);
    }

    private static boolean carries(BlockGetter level, BlockPos position) {
        return level.getBlockEntity(position) instanceof PipeRackBlockEntity rack && !rack.lanes().isEmpty();
    }

    /** A rack with lanes cannot be mined (D9): its pipes come out first. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos position) {
        return carries(level, position) ? 0 : super.getDestroyProgress(state, player, level, position);
    }

    /** A path that removes the block for a player without the break event below keeps it too. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos position, Player player, boolean willHarvest, FluidState fluid) {
        return !carries(level, position) && super.onDestroyedByPlayer(state, level, position, player, willHarvest, fluid);
    }

    /**
     * A player's break of a rack with lanes is cancelled (a creative player's instant break too). A cancelled break sends
     * the player the block and its lanes again, which the client had already removed.
     */
    @EventBusSubscriber(modid = IChemE.MOD_ID)
    public static final class Breaking {
        private Breaking() {
        }

        @SubscribeEvent
        public static void refuse(BlockEvent.BreakEvent event) {
            if (!(event.getState().getBlock() instanceof PipeRackBlock) || !carries(event.getLevel(), event.getPos())) return;
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(PipeLaying.message("rack_carries"), true);
        }
    }

    /** An explosion leaves a rack with lanes standing, as it cannot be mined. */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos position, Explosion explosion) {
        if (!carries(level, position)) super.onBlockExploded(state, level, position, explosion);
    }

    /** Removed any other way (a command), its lanes leave the ledger with it. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moved) {
        if (state.getBlock() != replacement.getBlock() && level instanceof ServerLevel server && level.getBlockEntity(position) instanceof PipeRackBlockEntity rack)
            FluidWorldAuthority.find(server.getServer()).ifPresent(world -> {
                for (var lane : rack.lanes()) world.remove(lane.identity());
            });
        super.onRemove(state, level, position, replacement, moved);
    }
}
