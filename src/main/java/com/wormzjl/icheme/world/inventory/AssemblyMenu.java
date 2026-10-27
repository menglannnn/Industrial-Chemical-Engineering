package com.wormzjl.icheme.world.inventory;

import com.wormzjl.icheme.network.AssemblyNetwork;
import com.wormzjl.icheme.registry.ModMenus;
import com.wormzjl.icheme.science.assembly.AssemblyReport;
import com.wormzjl.icheme.science.assembly.AssemblyView;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A column's GUI (batch 2026-10-26-multi-machine-framework, plan sections 8 and 10): the column at {@code origin}, found
 * through its cell {@code cell}, opened at {@code anchor} (the column, or a member that its report holds, which can stand
 * far from it), with the member {@code selected} ({@link AssemblyReport#NONE} for none). On the server the menu is a
 * session of the engine's presentation: opening it replays the stored view, closing it ends the session. On the client it
 * holds the last view delivered.
 */
public final class AssemblyMenu extends AbstractContainerMenu {
    /** How far from where it was opened the player may stand, blocks, beyond the structure's own size. */
    public static final double REACH = 8;

    private final BlockPos origin, cell, anchor;
    private final double reach;
    private final long selected;
    private final @Nullable ServerPlayer serverPlayer;
    private @Nullable AssemblyView view;

    public AssemblyMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readDouble(), buffer.readLong());
    }

    public AssemblyMenu(int id, Inventory inventory, BlockPos origin, BlockPos cell, BlockPos anchor, double reach, long selected) {
        super(ModMenus.ASSEMBLY.get(), id);
        this.origin = origin.immutable();
        this.cell = cell.immutable();
        this.anchor = anchor.immutable();
        this.reach = reach;
        this.selected = selected;
        serverPlayer = inventory.player instanceof ServerPlayer p ? p : null;
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(origin);
        buffer.writeBlockPos(cell);
        buffer.writeBlockPos(anchor);
        buffer.writeDouble(reach);
        buffer.writeLong(selected);
    }

    public BlockPos origin() {
        return origin;
    }

    public BlockPos cell() {
        return cell;
    }

    public long selected() {
        return selected;
    }

    /** The server player of this session, or null on the client. */
    public @Nullable ServerPlayer serverPlayer() {
        return serverPlayer;
    }

    /** Client: the last view delivered, or null before the first. */
    public @Nullable AssemblyView view() {
        return view;
    }

    /** Client: a view delivered for this menu. */
    public void accept(AssemblyView view) {
        this.view = view;
    }

    @Override
    public boolean stillValid(Player player) {
        var level = player.level();
        if (!level.hasChunkAt(cell) || player.distanceToSqr(anchor.getCenter()) > reach * reach) return false;
        var column = EquipmentStructure.find(level, cell);
        return column != null && column.origin().equals(origin);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Closing ends the session. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (serverPlayer != null) AssemblyNetwork.closed(this);
    }
}
