package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.registry.ModDataComponents;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.world.inventory.GaugeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class GaugeItem extends Item {
    private final GaugeSettings.Kind kind;
    public GaugeItem(Properties properties,GaugeSettings settings){super(properties.stacksTo(64).component(ModDataComponents.GAUGE_SETTINGS.get(),settings));kind=settings.kind();}
    public GaugeSettings.Kind kind(){return kind;}
    private InteractionResult interact(Player player,InteractionHand hand){
        if(player instanceof ServerPlayer server){if(player.isShiftKeyDown())GaugeMenu.open(server,hand);else FluidWorldAuthority.find(server.server).ifPresent(w->w.instruments().request(server,hand,false));}
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return context.getPlayer()==null?InteractionResult.PASS:interact(context.getPlayer(),context.getHand());}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return new InteractionResultHolder<>(interact(player,hand),player.getItemInHand(hand));}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){var s=stack.get(ModDataComponents.GAUGE_SETTINGS.get());if(s!=null)lines.add(Component.literal(s.number(s.minimum())+" – "+s.number(s.maximum())+" "+s.label()));lines.add(Component.translatable("tooltip.icheme.gauge.configure"));lines.add(Component.translatable("tooltip.icheme.gauge.install"));}
}
