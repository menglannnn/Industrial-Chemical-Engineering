package com.wormzjl.icheme.world.inventory;

import com.wormzjl.icheme.registry.ModDataComponents;
import com.wormzjl.icheme.registry.ModItems;
import com.wormzjl.icheme.registry.ModMenus;
import com.wormzjl.icheme.world.item.PipelineToolSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** A slot-free settings session bound to the exact held stack; it requests no fluid presentation. */
public final class PipelineToolMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final int selectedSlot;
    private final ItemStack tool;
    private final PipelineToolSettings initialSettings;

    public PipelineToolMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class), buffer.readVarInt(), PipelineToolSettings.STREAM_CODEC.decode(buffer));
    }

    private PipelineToolMenu(int id, Inventory inventory, InteractionHand hand, int selectedSlot, PipelineToolSettings settings) {
        super(ModMenus.PIPELINE_TOOL.get(), id);
        this.hand = hand;
        this.selectedSlot = selectedSlot;
        tool = inventory.player.getItemInHand(hand);
        initialSettings = Objects.requireNonNull(settings);
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        int slot = player.getInventory().selected;
        var settings = Objects.requireNonNull(player.getItemInHand(hand).get(ModDataComponents.PIPELINE_TOOL_SETTINGS.get()));
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new PipelineToolMenu(id, inventory, hand, slot, settings),
                Component.translatable("item.icheme.pipeline_tool")), buffer -> {
            buffer.writeEnum(hand);
            buffer.writeVarInt(slot);
            PipelineToolSettings.STREAM_CODEC.encode(buffer, settings);
        });
    }

    public PipelineToolSettings initialSettings() {
        return initialSettings;
    }

    /** Server: the packet handler has checked this session's id, lifetime and diameter. */
    public void save(PipelineToolSettings settings) {
        tool.set(ModDataComponents.PIPELINE_TOOL_SETTINGS.get(), settings);
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isSpectator() && tool.is(ModItems.PIPELINE_TOOL.get()) && player.getItemInHand(hand) == tool
                && (hand == InteractionHand.OFF_HAND || player.getInventory().selected == selectedSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
