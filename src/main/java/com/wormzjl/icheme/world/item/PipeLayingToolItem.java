package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The pipe laying tool (batch 2026-10-26-pipe-laying-system): lays a line of pipe of its set bore between two anchors,
 * along a route the client searches, and dismantles one pipe at a time. Usable in survival, where each laid block takes
 * a pipe item (owner decision D1). The client turns each use into one explicit command ({@code PipeLayingClient});
 * the item's own use does nothing, so a Shift + use that cancels a line never also opens the settings.
 */
public final class PipeLayingToolItem extends Item {
    public PipeLayingToolItem(Properties properties) {
        super(properties.stacksTo(1).component(ModItems.PIPE_LAYING_SETTINGS.get(), PipelineToolItem.Settings.DEFAULT));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var settings = stack.get(ModItems.PIPE_LAYING_SETTINGS.get());
        if (settings != null) tooltip.add(Component.translatable("item.icheme.pipe_laying_tool.size", Double.toString(settings.diameter())).withStyle(ChatFormatting.AQUA));
        for (var line : List.of("lay", "cancel", "dismantle", "configure", "apart"))
            tooltip.add(Component.translatable("item.icheme.pipe_laying_tool." + line).withStyle(ChatFormatting.GRAY));
    }
}
