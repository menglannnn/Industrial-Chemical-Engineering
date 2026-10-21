package com.wormzjl.icheme.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cell wholly inside a parametric vessel: an invisible full cube with no data of its own. It keeps mobs, fluids and
 * placed blocks out of the vessel. A closed vessel's cell is opaque: it casts its shadow and gives Distant Horizons a
 * solid voxel. An {@link #OPEN} cell (a floating-roof tank's, owner request 2026-10-21: its roof shows from above, plan
 * section 3.3) collides alike but passes light and hides no face beside it.
 */
public final class EquipmentInteriorBlock extends Block {
    public static final MapCodec<EquipmentInteriorBlock> CODEC = simpleCodec(EquipmentInteriorBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public EquipmentInteriorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<EquipmentInteriorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos position) {
        return state.getValue(OPEN) ? Shapes.empty() : super.getOcclusionShape(state, level, position);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos position) {
        return state.getValue(OPEN) ? 0 : super.getLightBlock(state, level, position);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos position) {
        return state.getValue(OPEN) || super.propagatesSkylightDown(state, level, position);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos position) {
        return state.getValue(OPEN) ? 1 : super.getShadeBrightness(state, level, position);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Using any cell opens the vessel's screen ({@link EquipmentShellBlock#use}); an interior cell finds its vessel through the shell. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position, Player player, BlockHitResult hit) {
        return EquipmentShellBlock.use(level, position, player, false);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moved) {
        if (!level.isClientSide() && !state.is(replacement.getBlock())) {
            var vessel = EquipmentStructure.find(level, position);
            if (vessel != null) EquipmentStructure.dismantle(level, vessel);
        }
        super.onRemove(state, level, position, replacement, moved);
    }
}
