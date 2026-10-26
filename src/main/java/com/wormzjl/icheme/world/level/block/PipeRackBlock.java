package com.wormzjl.icheme.world.level.block;

import com.mojang.serialization.MapCodec;
import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.science.equipment.RackGeometry;
import com.wormzjl.icheme.world.level.block.entity.PipeRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A layered pipe rack cell (batch 2026-10-26-pipe-laying-system, RACK_LAYERS_PLAN option A): one tier of four positions
 * across its run, carrying up to four laid lanes ({@link PipeRackBlockEntity}). Its facing, taken from the player's
 * look as for stairs, orders the positions and sets the run; racks in line join into one run whatever their facings. A
 * rack becomes a corner where a rack with a perpendicular facing stands at the side of an end of its run, its lanes
 * turning between that end and that side; a rack with racks in line at both ends stays straight (no tee, RD8). A rack
 * that carries lanes keeps its shape, as its lanes were laid in it. A rack that carries lanes is not broken (owner
 * decision D9): its pipes are dismantled first, with the laying tool.
 */
public final class PipeRackBlock extends BaseEntityBlock {
    public static final MapCodec<PipeRackBlock> CODEC = simpleCodec(PipeRackBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    /** A rack cell's shape, in {@link RackGeometry}'s corner order: straight, or a corner from an end to a side. */
    public enum Shape implements StringRepresentable {
        STRAIGHT, BACK_LEFT, BACK_RIGHT, FRONT_LEFT, FRONT_RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public PipeRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, Shape.STRAIGHT));
    }

    @Override
    protected MapCodec<PipeRackBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    /** The lane geometry of a rack cell in this state: its facing and corner. */
    public static RackGeometry.Rack rack(BlockState state) {
        return new RackGeometry.Rack(state.getValue(FACING).ordinal(), state.getValue(SHAPE).ordinal());
    }

    /** Facing the player's horizontal look, shaped by the racks next door. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing).setValue(SHAPE, shape(context.getLevel(), context.getClickedPos(), facing));
    }

    /**
     * The shape a rack facing {@code facing} at {@code position} takes from its neighbours: a corner towards the first
     * side (right, then left) where a rack runs across, from the end where its run continues (the back where neither or
     * one only at the back does); straight where no rack runs across beside it, or where its run continues at both ends.
     */
    static Shape shape(BlockGetter level, BlockPos position, Direction facing) {
        boolean front = inLine(level, position, facing, facing), back = inLine(level, position, facing.getOpposite(), facing);
        if (front && back) return Shape.STRAIGHT;
        for (var side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            var neighbour = level.getBlockState(position.relative(side));
            if (!(neighbour.getBlock() instanceof PipeRackBlock) || neighbour.getValue(FACING).getAxis() != side.getAxis()) continue;
            boolean right = side == facing.getClockWise();
            return front && !back ? right ? Shape.FRONT_RIGHT : Shape.FRONT_LEFT : right ? Shape.BACK_RIGHT : Shape.BACK_LEFT;
        }
        return Shape.STRAIGHT;
    }

    /** Whether the rack at {@code position} facing {@code facing} has a rack in line beyond its face {@code towards}. */
    private static boolean inLine(BlockGetter level, BlockPos position, Direction towards, Direction facing) {
        var neighbour = level.getBlockState(position.relative(towards));
        return neighbour.getBlock() instanceof PipeRackBlock && neighbour.getValue(FACING).getAxis() == facing.getAxis();
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
        return level.getBlockEntity(position) instanceof PipeRackBlockEntity rack ? rack.shape() : PipeRackBlockEntity.frame(new RackGeometry.Rack(state.getValue(FACING).ordinal(), state.getValue(SHAPE).ordinal(), 0, false, 0, RackGeometry.BEAM_TOP));
    }

    /**
     * A neighbour changed: an empty rack takes the shape its neighbours give it, and the drawn steel and the faces its
     * lanes meet redraw once the change is in the world.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos position, BlockPos neighbourPosition) {
        var shaped = carries(level, position) ? state : state.setValue(SHAPE, shape(level, position, state.getValue(FACING)));
        if (level instanceof ServerLevel server) server.getServer().execute(() -> {
            if (server.getBlockEntity(position) instanceof PipeRackBlockEntity rack && rack.refreshDrawn()) server.sendBlockUpdated(position, rack.getBlockState(), rack.getBlockState(), 2);
        });
        return super.updateShape(shaped, direction, neighbour, level, position, neighbourPosition);
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
