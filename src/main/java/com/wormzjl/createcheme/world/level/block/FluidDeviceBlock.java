package com.wormzjl.createcheme.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wormzjl.createcheme.runtime.fluid.*;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.createcheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Gameplay adapter only: authoritative inventory is never stored in this block or its block entity. */
public final class FluidDeviceBlock extends BaseEntityBlock {
    public static final MapCodec<FluidDeviceBlock> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Codec.STRING.xmap(TopologyCompiler.Kind::valueOf,TopologyCompiler.Kind::name).fieldOf("device_kind").forGetter(block->block.kind),propertiesCodec()).apply(instance,FluidDeviceBlock::new));
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    private static final java.util.Map<Direction,BooleanProperty> CONNECTIONS=java.util.Map.of(
            Direction.DOWN,BlockStateProperties.DOWN,Direction.UP,BlockStateProperties.UP,Direction.NORTH,BlockStateProperties.NORTH,
            Direction.SOUTH,BlockStateProperties.SOUTH,Direction.WEST,BlockStateProperties.WEST,Direction.EAST,BlockStateProperties.EAST);
    private final TopologyCompiler.Kind kind;
    public FluidDeviceBlock(TopologyCompiler.Kind kind,BlockBehaviour.Properties properties) {
        super(properties);this.kind=kind;var state=stateDefinition.any().setValue(FACING,Direction.NORTH);for(var property:CONNECTIONS.values())state=state.setValue(property,false);registerDefaultState(state);
    }
    public TopologyCompiler.Kind kind(){return kind;}
    @Override public MapCodec<FluidDeviceBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);for(var property:CONNECTIONS.values())builder.add(property);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    /** Pipe shapes by drawn outside code and connected faces. */
    private static final java.util.Map<Integer,net.minecraft.world.phys.shapes.VoxelShape> PIPE_SHAPES=new java.util.concurrent.ConcurrentHashMap<>();
    /** A pipe's shape follows its drawn outside diameter (at least 3/16 block across, so a thin line can be aimed at); devices fill their block. */
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos position,net.minecraft.world.phys.shapes.CollisionContext context) {
        if(kind!=TopologyCompiler.Kind.PIPE)return super.getShape(state,level,position,context);
        double bore=level.getBlockEntity(position) instanceof FluidDeviceBlockEntity entity?entity.bore():.05;
        int code=com.wormzjl.createcheme.science.equipment.PipeSizes.code(com.wormzjl.createcheme.science.equipment.PipeSizes.outside(bore)),mask=0;
        for(var direction:Direction.values())if(state.getValue(CONNECTIONS.get(direction)))mask|=1<<direction.ordinal();
        // Unconnected, it lies along its facing as drawn.
        if(mask==0)mask=3<<(state.getValue(FACING).ordinal()&~1);
        return PIPE_SHAPES.computeIfAbsent(code<<6|mask,FluidDeviceBlock::pipeShape);
    }
    private static net.minecraft.world.phys.shapes.VoxelShape pipeShape(int key) {
        double h=Math.max(com.wormzjl.createcheme.science.equipment.PipeSizes.outsideOf(key>>6)/2,3/32.0)*16,lo=8-h,hi=8+h;
        var shape=Block.box(lo,lo,lo,hi,hi,hi);
        for(var direction:Direction.values())if((key>>direction.ordinal()&1)!=0) {
            var arm=switch(direction){case DOWN->Block.box(lo,0,lo,hi,8,hi);case UP->Block.box(lo,8,lo,hi,16,hi);case NORTH->Block.box(lo,lo,0,hi,hi,8);
                case SOUTH->Block.box(lo,lo,8,hi,hi,16);case WEST->Block.box(0,lo,lo,8,hi,hi);case EAST->Block.box(8,lo,lo,16,hi,hi);};
            shape=net.minecraft.world.phys.shapes.Shapes.or(shape,arm);
        }
        return shape.optimize();
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos position,BlockState state){return new FluidDeviceBlockEntity(position,state);}
    /** Only pipes and valves may stand in a vertical line (owner decision 2026-10-16); every other device faces a horizontal direction. */
    public static boolean vertical(TopologyCompiler.Kind kind){return kind==TopologyCompiler.Kind.PIPE||kind==TopologyCompiler.Kind.VALVE;}
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing=vertical(kind)?context.getNearestLookingDirection().getOpposite():context.getHorizontalDirection().getOpposite();
        var state=defaultBlockState().setValue(FACING,facing);
        for(var direction:Direction.values()){var at=context.getClickedPos().relative(direction);state=state.setValue(CONNECTIONS.get(direction),connects(state,context.getLevel(),at,context.getLevel().getBlockState(at),direction));}
        return state;
    }
    public static BooleanProperty connection(Direction direction){return CONNECTIONS.get(direction);}
    private static boolean boundary(TopologyCompiler.Kind kind){return kind==TopologyCompiler.Kind.RESERVOIR||kind==TopologyCompiler.Kind.GENERATOR||kind==TopologyCompiler.Kind.VOID;}
    private static boolean face(BlockState state,Direction direction) {
        var block=(FluidDeviceBlock)state.getBlock();return block.kind!=TopologyCompiler.Kind.PUMP&&block.kind!=TopologyCompiler.Kind.COMPRESSOR&&block.kind!=TopologyCompiler.Kind.VALVE&&block.kind!=TopologyCompiler.Kind.FILTER||state.getValue(FACING).getAxis()==direction.getAxis();
    }
    /** Whether this block meets its neighbour towards {@code direction}: another device by the rules above, or, for a pipe, a vessel nozzle facing it. */
    private static boolean connects(BlockState state,BlockGetter level,BlockPos neighborPosition,BlockState neighbor,Direction direction) {
        if(neighbor.getBlock() instanceof FluidDeviceBlock other)return !(boundary(((FluidDeviceBlock)state.getBlock()).kind)&&boundary(other.kind))&&face(state,direction)&&face(neighbor,direction.getOpposite());
        return ((FluidDeviceBlock)state.getBlock()).kind==TopologyCompiler.Kind.PIPE&&neighbor.getBlock() instanceof EquipmentShellBlock
                &&level.getBlockEntity(neighborPosition) instanceof com.wormzjl.createcheme.world.level.block.entity.EquipmentShellBlockEntity shell&&shell.nozzleFacing(direction.getOpposite())!=null;
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos position,BlockPos neighborPosition) {
        // The drawn faces follow the new connections once the state is in the world.
        if(level instanceof ServerLevel server)server.getServer().execute(()->{if(server.getBlockEntity(position) instanceof FluidDeviceBlockEntity entity)entity.refreshFacesAndSync();});
        return super.updateShape(state.setValue(CONNECTIONS.get(direction),connects(state,level,neighborPosition,neighbor,direction)),direction,neighbor,level,position,neighborPosition);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos position,BlockState previous,boolean moved) {
        super.onPlace(state,level,position,previous,moved);if(!(level instanceof ServerLevel serverLevel))return;
        FluidWorldAuthority.find(serverLevel.getServer()).ifPresent(world->{
            var p=new PhysicalFluidTopology.Position(level.dimension().location().toString(),position.getX(),position.getY(),position.getZ());
            if(previous.getBlock()!=this) {
                var record=world.place(p,kind,PhysicalFluidTopology.Direction.valueOf(state.getValue(FACING).name()));
                if(level.getBlockEntity(position) instanceof FluidDeviceBlockEntity entity)entity.bindIdentity(record.device().id());
            } else if(previous.getValue(FACING)!=state.getValue(FACING)) {
                world.at(p).ifPresent(old->{var d=old.device();world.edit(d.id(),old.revision(),new PhysicalFluidTopology.Device(d.id(),d.position(),d.kind(),PhysicalFluidTopology.Direction.valueOf(state.getValue(FACING).name()),d.geometry(),d.control()),old.spec());});
            }
        });
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos position,BlockState replacement,boolean moved) {
        if(state.getBlock()!=replacement.getBlock()&&level instanceof ServerLevel serverLevel)FluidWorldAuthority.find(serverLevel.getServer()).ifPresent(world->{
            var p=new PhysicalFluidTopology.Position(level.dimension().location().toString(),position.getX(),position.getY(),position.getZ());world.at(p).ifPresent(r->world.remove(r.device().id()));
        });
        super.onRemove(state,level,position,replacement,moved);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos position,Player player,BlockHitResult hit) {
        if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(position) instanceof FluidDeviceBlockEntity entity)entity.open(serverPlayer,false);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos position,Player player,InteractionHand hand,BlockHitResult hit) {
        if(stack.is(com.wormzjl.createcheme.registry.ModItems.FLUID_DEBUGGER.get())) {
            if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(position) instanceof FluidDeviceBlockEntity entity)entity.open(serverPlayer,true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
