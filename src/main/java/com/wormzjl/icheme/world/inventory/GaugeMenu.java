package com.wormzjl.icheme.world.inventory;

import com.wormzjl.icheme.registry.*;
import com.wormzjl.icheme.world.item.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** The exact hand, slot, stack object and original component are bound for this editing session. */
public final class GaugeMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final int slot;
    private final ItemStack stack;
    private final GaugeSettings initial;
    public GaugeMenu(int id,Inventory inventory,RegistryFriendlyByteBuf b){this(id,inventory,b.readEnum(InteractionHand.class),b.readVarInt(),GaugeSettings.STREAM_CODEC.decode(b));}
    private GaugeMenu(int id,Inventory inventory,InteractionHand hand,int slot,GaugeSettings initial){super(ModMenus.GAUGE.get(),id);this.hand=hand;this.slot=slot;this.initial=initial;stack=inventory.player.getItemInHand(hand);}
    public static void open(ServerPlayer player,InteractionHand hand){var stack=player.getItemInHand(hand);var settings=stack.get(ModItems.GAUGE_SETTINGS.get());if(!(stack.getItem() instanceof GaugeItem item)||settings==null||settings.kind()!=item.kind())return;int slot=player.getInventory().selected;player.openMenu(new SimpleMenuProvider((id,inv,p)->new GaugeMenu(id,inv,hand,slot,settings),stack.getHoverName()),b->{b.writeEnum(hand);b.writeVarInt(slot);GaugeSettings.STREAM_CODEC.encode(b,settings);});}
    public GaugeSettings initial(){return initial;}
    public void save(GaugeSettings value){if(value.kind()!=initial.kind())throw new IllegalArgumentException("Gauge kind changed");stack.set(ModItems.GAUGE_SETTINGS.get(),value);}
    @Override public boolean stillValid(Player player){return !player.isSpectator()&&stack.getItem() instanceof GaugeItem item&&item.kind()==initial.kind()&&player.getItemInHand(hand)==stack&&initial.equals(stack.get(ModItems.GAUGE_SETTINGS.get()))&&(hand==InteractionHand.OFF_HAND||player.getInventory().selected==slot);}
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
}
