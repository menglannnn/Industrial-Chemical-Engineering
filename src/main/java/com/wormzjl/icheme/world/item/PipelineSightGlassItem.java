package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.science.equipment.SightGlassGeometry;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Installs a static visual accessory without replacing a pipe or editing its fluid registration. */
public final class PipelineSightGlassItem extends Item {
    public PipelineSightGlassItem(Properties properties){super(properties.stacksTo(64));}

    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof FluidDeviceBlock block)||block.kind()!=TopologyCompiler.Kind.PIPE)return InteractionResult.PASS;
        if(player==null||!player.mayBuild()||player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        if(!FluidDeviceBlock.supportsSightGlass(state)) {
            if(!level.isClientSide())player.displayClientMessage(Component.translatable("item.icheme.pipeline_sight_glass.straight_only"),true);
            return InteractionResult.FAIL;
        }
        if(FluidDeviceBlock.hasSightGlass(state))return InteractionResult.sidedSuccess(level.isClientSide());
        if(!level.isClientSide()) {
            if(!(level.getBlockEntity(pos) instanceof FluidDeviceBlockEntity entity))return InteractionResult.FAIL;
            var eye=player.getEyePosition().subtract(pos.getCenter());
            var window=SightGlassGeometry.towards(FluidDeviceBlock.connectionMask(state),state.getValue(FluidDeviceBlock.FACING).ordinal(),eye.x,eye.y,eye.z);
            var installed=state.setValue(FluidDeviceBlock.SIGHT_GLASS,true).setValue(FluidDeviceBlock.SIGHT_GLASS_AXIS,Direction.Axis.valueOf(window.name()));
            if(!level.setBlock(pos,installed,Block.UPDATE_ALL))return InteractionResult.FAIL;
            entity.refreshGeometry(entity.bore(),true);
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("item.icheme.pipeline_sight_glass.install").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.icheme.pipeline_sight_glass.straight_only").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.icheme.pipeline_sight_glass.recover").withStyle(ChatFormatting.GRAY));
    }
}
