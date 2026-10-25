package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.SightGlassGeometry;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Installs an axis-locked valve without replacing the host pipe or its identity. */
public abstract class PipelineValveItem extends Item {
    protected enum Type {SHUTOFF,CHECK,PRESSURE}
    private final Type type;
    private final PhysicalFluidTopology.Cutoff installed;
    private final String translation;
    protected PipelineValveItem(Properties properties,Type type){
        super(properties.stacksTo(64));
        this.type=java.util.Objects.requireNonNull(type);
        installed=switch(type){case SHUTOFF->PhysicalFluidTopology.Cutoff.OPEN;case CHECK->PhysicalFluidTopology.Cutoff.CHECK;case PRESSURE->PhysicalFluidTopology.Cutoff.NONE;};
        translation=type==Type.PRESSURE?"item.icheme.pressure_control_valve.":"item.icheme.pipeline_"+(type==Type.CHECK?"check":"shutoff")+"_valve.";
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof FluidDeviceBlock block)||block.kind()!=TopologyCompiler.Kind.PIPE)return InteractionResult.PASS;
        if(player==null||!player.mayBuild()||player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        boolean duplicate=switch(type){case PRESSURE->FluidDeviceBlock.hasPressureValve(state);case SHUTOFF->FluidDeviceBlock.hasShutoff(state);case CHECK->FluidDeviceBlock.cutoff(state)==installed;};
        if(duplicate)return InteractionResult.sidedSuccess(level.isClientSide());
        if(state.getValue(FluidDeviceBlock.SIGHT_GLASS)||FluidDeviceBlock.hasPipeValve(state)) {
            if(!level.isClientSide())player.displayClientMessage(Component.translatable(translation+"one_accessory"),true);
            return InteractionResult.FAIL;
        }
        int mask=FluidDeviceBlock.connectionMask(state);
        if(!SightGlassGeometry.supports(mask)) {
            if(!level.isClientSide())player.displayClientMessage(Component.translatable(translation+"straight_only"),true);
            return InteractionResult.FAIL;
        }
        if(level instanceof ServerLevel server) {
            if(!(level.getBlockEntity(pos) instanceof FluidDeviceBlockEntity entity))return InteractionResult.FAIL;
            var world=FluidWorldAuthority.find(server.getServer()).orElse(null);
            if(world==null||entity.fluidIdentity()==0)return InteractionResult.FAIL;
            if(world.instruments().occupied(entity.fluidIdentity()))return InteractionResult.FAIL;
            var record=world.registrations().get(entity.fluidIdentity());
            var at=new PhysicalFluidTopology.Position(level.dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ());
            if(record==null||record.device().kind()!=TopologyCompiler.Kind.PIPE||!record.device().position().equals(at))return InteractionResult.FAIL;
            if(record.device().cutoff()!=PhysicalFluidTopology.Cutoff.NONE||!(record.device().control() instanceof com.wormzjl.icheme.science.fluid.network.FlowControl.Passive))return InteractionResult.SUCCESS;
            var facing=mask==0?state.getValue(FluidDeviceBlock.FACING):Direction.values()[Integer.numberOfTrailingZeros(mask)];
            if(type!=Type.SHUTOFF) {
                var look=player.getLookAngle();double along=look.x*facing.getStepX()+look.y*facing.getStepY()+look.z*facing.getStepZ();
                if(along<0)facing=facing.getOpposite();
            }
            var changed=state.setValue(FluidDeviceBlock.FACING,facing);
            changed=type==Type.PRESSURE?changed.setValue(FluidDeviceBlock.PRESSURE_CONTROL,true):changed.setValue(FluidDeviceBlock.SHUTOFF,FluidDeviceBlock.Shutoff.of(installed));
            changed=FluidDeviceBlock.connectedState(changed,level,pos);
            if(!level.setBlock(pos,changed,Block.UPDATE_ALL))return InteractionResult.FAIL;
            var axis=PhysicalFluidTopology.Direction.valueOf(facing.name());
            if(!(type==Type.PRESSURE?world.installPipePressureValve(entity.fluidIdentity(),axis):world.installPipeValve(entity.fluidIdentity(),axis,installed))) {
                level.setBlock(pos,state,Block.UPDATE_ALL);return InteractionResult.FAIL;
            }
            world.instruments().legacyInstalled(entity.fluidIdentity(),switch(type){case SHUTOFF->com.wormzjl.icheme.runtime.instrument.AccessoryStore.Type.SHUTOFF;case CHECK->com.wormzjl.icheme.runtime.instrument.AccessoryStore.Type.CHECK;case PRESSURE->com.wormzjl.icheme.runtime.instrument.AccessoryStore.Type.PRESSURE_VALVE;});
            entity.refreshGeometry(entity.bore(),true);
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        for(String key:List.of("install","toggle","straight_only","one_accessory","recover"))
            tooltip.add(Component.translatable(translation+key).withStyle(ChatFormatting.GRAY));
    }
}
