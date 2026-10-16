package com.wormzjl.createcheme.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A cell wholly inside a parametric vessel: an invisible, opaque full cube with no data of its own. It keeps mobs,
 * fluids and placed blocks out of the vessel, casts its shadow and gives Distant Horizons a solid voxel.
 */
public final class EquipmentInteriorBlock extends Block {
    public static final MapCodec<EquipmentInteriorBlock> CODEC = simpleCodec(EquipmentInteriorBlock::new);

    public EquipmentInteriorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<EquipmentInteriorBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
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
