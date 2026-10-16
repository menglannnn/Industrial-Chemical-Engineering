package com.wormzjl.createcheme.world.level.block;

import com.mojang.serialization.MapCodec;
import com.wormzjl.createcheme.client.EquipmentClient;
import com.wormzjl.createcheme.registry.ModItems;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
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

    /** A column's ladder is climbed where the entity stands in front of its rungs, inside its cage (not anywhere in its cells). */
    @Override
    public boolean isLadder(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos position, net.minecraft.world.entity.LivingEntity entity) {
        if (!(level.getBlockEntity(position) instanceof EquipmentShellBlockEntity shell) || shell.shape() == null || !shell.shape().column()) return false;
        var origin = shell.origin();
        return ShapeRasterizer.climbable(shell.shape(), entity.getX() - origin.getX(), entity.getY() - origin.getY(), entity.getZ() - origin.getZ());
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position, Player player, BlockHitResult hit) {
        return use(level, position, player, false);
    }

    /** The fluid debugger opens the drum's tank screen with its diagnostics. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos position, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.FLUID_DEBUGGER.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var vessel = EquipmentStructure.find(level, position);
        if (vessel == null || !vessel.shape().drum()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        use(level, position, player, true);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Using any cell of a vessel, shell or interior: a drum opens its tank screen, as a one-block tank's does, with the
     * drum's datasheet on a tab (owner request 2026-10-16); a column or a unit opens its datasheet, built on the client from synced
     * cells. Both sides decide alike, since the cells carry the shape.
     */
    static InteractionResult use(Level level, BlockPos position, Player player, boolean debug) {
        var vessel = EquipmentStructure.find(level, position);
        if (vessel == null) return InteractionResult.PASS;
        if (!vessel.shape().drum()) {
            if (level.isClientSide()) EquipmentClient.openVessel(position);
        } else if (player instanceof ServerPlayer server) {
            var cell = ShapeRasterizer.fluidCell(vessel.shape());
            var node = vessel.origin().offset(cell.x(), cell.y(), cell.z());
            if (level.isLoaded(node) && level.getBlockEntity(node) instanceof EquipmentShellBlockEntity entity) entity.open(server, debug);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** A pipe placed, removed or changed in front of a nozzle: the vessel redraws its nozzles at the pipe's size. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos position, net.minecraft.world.level.block.Block neighbor, BlockPos neighborPosition, boolean moved) {
        super.neighborChanged(state, level, position, neighbor, neighborPosition, moved);
        if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel server)
            // After the pipe's own shape update has taken its connection.
            server.getServer().execute(() -> EquipmentStructure.refreshNozzlesAt(level, position));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(position) instanceof EquipmentShellBlockEntity shell && shell.shape() != null)
            EquipmentStructure.dismantle(level, new EquipmentStructure.Vessel(shell.origin(), shell.shape()));
        super.onRemove(state, level, position, replacement, moved);
    }
}
