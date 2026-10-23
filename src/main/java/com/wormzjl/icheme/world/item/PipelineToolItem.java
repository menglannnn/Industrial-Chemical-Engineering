package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.registry.ModDataComponents;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.fluid.network.PipeResistance;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.inventory.PipelineToolMenu;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Item interactions read static registrations and queue edits; they never inspect or publish process state. */
public final class PipelineToolItem extends Item {
    public PipelineToolItem(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.PIPELINE_TOOL_SETTINGS.get(), PipelineToolSettings.DEFAULT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        return new InteractionResultHolder<>(configure(player, hand), stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return configure(player, context.getHand());
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof FluidDeviceBlock block)
                || block.kind() != TopologyCompiler.Kind.PIPE) return InteractionResult.PASS;
        var settings = stack.get(ModDataComponents.PIPELINE_TOOL_SETTINGS.get());
        if (settings == null || !player.mayBuild() || player.isSpectator() || !level.mayInteract(player, pos))
            return InteractionResult.FAIL;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FluidDeviceBlockEntity entity) {
            FluidWorldAuthority.find(serverPlayer.server).ifPresent(world -> {
                var position = new PhysicalFluidTopology.Position(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
                world.at(position).filter(record -> record.device().id() == entity.fluidIdentity()
                        && record.device().kind() == TopologyCompiler.Kind.PIPE).ifPresent(record -> {
                    var device = record.device();
                    var geometry = device.geometry();
                    if (geometry.diameter() == settings.diameter()) return;
                    var resized = new PipeResistance.Geometry(geometry.length(), settings.diameter(), geometry.roughness(), geometry.minorLoss());
                    world.edit(device.id(), record.revision(), new PhysicalFluidTopology.Device(device.id(), device.position(),
                            device.kind(), device.facing(), resized, device.control(), device.ports()), record.spec());
                });
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static InteractionResult configure(Player player, InteractionHand hand) {
        if (player.isSpectator() || player.getItemInHand(hand).get(ModDataComponents.PIPELINE_TOOL_SETTINGS.get()) == null)
            return InteractionResult.FAIL;
        if (player instanceof ServerPlayer serverPlayer) PipelineToolMenu.open(serverPlayer, hand);
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var settings = stack.get(ModDataComponents.PIPELINE_TOOL_SETTINGS.get());
        if (settings != null) tooltip.add(Component.translatable("item.icheme.pipeline_tool.target", Double.toString(settings.diameter()))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.icheme.pipeline_tool.configure").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.icheme.pipeline_tool.apply").withStyle(ChatFormatting.GRAY));
    }
}
