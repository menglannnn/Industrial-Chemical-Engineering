package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class AccessoryWrenchItem extends Item {
    public AccessoryWrenchItem(Properties properties){super(properties.stacksTo(1));}
    private InteractionResult interact(Player player,InteractionHand hand){if(!player.isShiftKeyDown())return InteractionResult.PASS;if(player instanceof ServerPlayer server)FluidWorldAuthority.find(server.server).ifPresent(w->w.instruments().request(server,hand,true));return InteractionResult.sidedSuccess(player.level().isClientSide());}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return context.getPlayer()==null?InteractionResult.PASS:interact(context.getPlayer(),context.getHand());}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return new InteractionResultHolder<>(interact(player,hand),player.getItemInHand(hand));}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.icheme.accessory_wrench"));}
}
