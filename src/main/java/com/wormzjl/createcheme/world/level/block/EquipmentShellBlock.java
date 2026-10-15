package com.wormzjl.createcheme.world.level.block;

import com.mojang.serialization.MapCodec;
import com.wormzjl.createcheme.client.EquipmentClient;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentShellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A cell crossed by the surface of a parametric vessel. Its model is generated from its block entity's cell key and
 * baked into the chunk mesh; its collision shape is the vessel's solid inside the cell. It is transparent to light,
 * because a curved quad inside the cell is lit by the cell's own light value; the interior cells cast the shadow.
 */
public final class EquipmentShellBlock extends BaseEntityBlock {
    public static final MapCodec<EquipmentShellBlock> CODEC = simpleCodec(EquipmentShellBlock::new);

    public EquipmentShellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<EquipmentShellBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new EquipmentShellBlockEntity(position, state);
    }

    /** Unbound cells (before their data arrives) are full blocks, so nothing falls through while a vessel loads. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return level.getBlockEntity(position) instanceof EquipmentShellBlockEntity shell && shell.key() != null
                ? EquipmentStructure.collision(shell.key()) : Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos position) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos position) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos position) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos position) {
        return 1;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Using any cell opens the vessel's screen (its controller's datasheet), built on the client from synced cells. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position, Player player, BlockHitResult hit) {
        if (level.isClientSide()) EquipmentClient.openVessel(position);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(position) instanceof EquipmentShellBlockEntity shell && shell.shape() != null)
            EquipmentStructure.dismantle(level, new EquipmentStructure.Vessel(shell.origin(), shell.shape()));
        super.onRemove(state, level, position, replacement, moved);
    }
}
