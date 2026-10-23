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

/** Adds an initially open, axis-locked cutoff without replacing the pipe or its identity. */
public final class PipelineShutoffValveItem extends Item {
    public PipelineShutoffValveItem(Properties properties){super(properties.stacksTo(64));}

    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof FluidDeviceBlock block)||block.kind()!=TopologyCompiler.Kind.PIPE)return InteractionResult.PASS;
        if(player==null||!player.mayBuild()||player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        if(FluidDeviceBlock.hasShutoff(state))return InteractionResult.sidedSuccess(level.isClientSide());
        if(state.getValue(FluidDeviceBlock.SIGHT_GLASS)) {
            if(!level.isClientSide())player.displayClientMessage(Component.translatable("item.icheme.pipeline_shutoff_valve.one_accessory"),true);
            return InteractionResult.FAIL;
        }
        int mask=FluidDeviceBlock.connectionMask(state);
        if(!SightGlassGeometry.supports(mask)) {
            if(!level.isClientSide())player.displayClientMessage(Component.translatable("item.icheme.pipeline_shutoff_valve.straight_only"),true);
            return InteractionResult.FAIL;
        }
        if(level instanceof ServerLevel server) {
            if(!(level.getBlockEntity(pos) instanceof FluidDeviceBlockEntity entity))return InteractionResult.FAIL;
            var world=FluidWorldAuthority.find(server.getServer()).orElse(null);
            if(world==null||entity.fluidIdentity()==0)return InteractionResult.FAIL;
            var record=world.registrations().get(entity.fluidIdentity());
            var at=new PhysicalFluidTopology.Position(level.dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ());
            if(record==null||record.device().kind()!=TopologyCompiler.Kind.PIPE||!record.device().position().equals(at))return InteractionResult.FAIL;
            if(record.device().cutoff()!=PhysicalFluidTopology.Cutoff.NONE)return InteractionResult.SUCCESS;
            var facing=mask==0?state.getValue(FluidDeviceBlock.FACING):Direction.values()[Integer.numberOfTrailingZeros(mask)];
            var installed=FluidDeviceBlock.connectedState(state.setValue(FluidDeviceBlock.SHUTOFF,FluidDeviceBlock.Shutoff.OPEN).setValue(FluidDeviceBlock.FACING,facing),level,pos);
            if(!level.setBlock(pos,installed,Block.UPDATE_ALL))return InteractionResult.FAIL;
            if(!world.installPipeCutoff(entity.fluidIdentity(),PhysicalFluidTopology.Direction.valueOf(facing.name()))) {
                level.setBlock(pos,state,Block.UPDATE_ALL);return InteractionResult.FAIL;
            }
            entity.refreshGeometry(entity.bore(),true);
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        for(String key:List.of("install","toggle","straight_only","one_accessory","recover"))
            tooltip.add(Component.translatable("item.icheme.pipeline_shutoff_valve."+key).withStyle(ChatFormatting.GRAY));
    }
}
