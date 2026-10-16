package com.wormzjl.createcheme.world.level.block.entity;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.registry.ModBlockEntities;
import com.wormzjl.createcheme.runtime.fluid.FluidView;
import com.wormzjl.createcheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.createcheme.world.inventory.FluidDeviceMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.wormzjl.createcheme.science.equipment.ColumnOptions;
import com.wormzjl.createcheme.science.equipment.ColumnTrays;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One shell cell of a parametric vessel: the vessel's shape, this cell's coordinates inside it, and the drawn sizes of
 * the nozzles that reach it. Never ticks. The client model turns the cell's {@link ShapeRasterizer.CellKey} into the
 * quads of this cell, and the block turns it into its collision shape, so the cell needs nothing from any other cell or
 * chunk. The nozzle sizes follow the pipes connected to them; the server sets them ({@code EquipmentStructure}).
 *
 * <p>A drum is a reservoir of the fluid network: its node cell ({@link ShapeRasterizer#fluidCell}) carries the drum's
 * fluid identity and is its presentation binding, as a device block entity is for a one-block tank. Nothing of the
 * drum's contents is kept here.
 */
public final class EquipmentShellBlockEntity extends BlockEntity implements FluidView.Receiver {
    /** Model data read by the shell model while the chunk is meshed. */
    public static final ModelProperty<ShapeRasterizer.CellKey> CELL = new ModelProperty<>();
    private @Nullable EquipmentShape shape;
    private int cellX, cellY, cellZ;
    private long nozzles;
    private @Nullable ShapeRasterizer.CellKey key;
    /** The drum's fluid identity, on its node cell only (0 elsewhere and while unregistered). */
    private long identity;
    private @Nullable FluidView lastView;

    public EquipmentShellBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.EQUIPMENT_SHELL.get(), position, state);
    }

    @Override
    public long fluidIdentity() {
        return identity;
    }

    /** The engine's bucket hands over the drum's view (the authority checked the identity). */
    @Override
    public void acceptFluidView(FluidView view) {
        lastView = view;
    }

    public @Nullable FluidView lastView() {
        return lastView;
    }

    /** Server: the drum registered with this cell as its node takes its identity; its presentation follows the engine's buckets. */
    public void bindIdentity(long id) {
        if (id <= 0) throw new IllegalStateException("Invalid fluid identity");
        identity = id;
        setChanged();
        if (level instanceof ServerLevel serverLevel)
            FluidWorldAuthority.find(serverLevel.getServer()).ifPresent(world -> world.deviceLoaded(id));
    }

    /** This cell's position in the fluid topology. */
    public PhysicalFluidTopology.Position fluidPosition() {
        return new PhysicalFluidTopology.Position(level.dimension().location().toString(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    /** Server: a node cell binds the drum the ledger holds here, as a device block entity binds its registration. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (!(level instanceof ServerLevel serverLevel) || shape == null) return;
        var here = fluidPosition();
        FluidWorldAuthority.find(serverLevel.getServer()).ifPresent(world -> world.at(here)
                .filter(record -> record.device().position().equals(here) && !record.device().ports().isEmpty())
                .ifPresent(record -> {
                    if (identity != record.device().id()) bindIdentity(record.device().id());
                    else world.deviceLoaded(identity);
                }));
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        released();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        released();
    }

    /** Unloaded or removed: publications stop marking the drum. */
    private void released() {
        if (identity != 0 && level instanceof ServerLevel serverLevel && serverLevel.getServer().isSameThread())
            FluidWorldAuthority.find(serverLevel.getServer()).ifPresent(world -> world.deviceUnloaded(identity));
    }

    /** Server: opens the drum's tank screen; the player may stand anywhere within reach of the drum, not only of this cell. */
    public void open(ServerPlayer player, boolean debug) {
        if (identity == 0 || shape == null) {
            player.displayClientMessage(Component.literal("Drum is waiting for its world identity."), true);
            return;
        }
        double reach = FluidDeviceMenu.REACH + Math.max(shape.sizeX(), Math.max(shape.sizeY(), shape.sizeZ()));
        player.openMenu(new MenuProvider() {
            public Component getDisplayName() {
                return Component.translatable("gui.createcheme.vessel.title.drum");
            }

            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new FluidDeviceMenu(containerId, inventory, worldPosition, identity, debug, reach);
            }
        }, buffer -> {
            buffer.writeBlockPos(worldPosition);
            buffer.writeLong(identity);
            buffer.writeBoolean(debug);
            buffer.writeDouble(reach);
        });
        // Replay only after the opening packet and server menu exist, preserving packet order.
        if (player.containerMenu instanceof FluidDeviceMenu menu && menu.identity() == identity)
            FluidWorldAuthority.find(player.server).ifPresent(world -> world.replayOnOpen(menu));
    }

    public void bind(EquipmentShape shape, ShapeRasterizer.Cell cell) {
        assign(shape, cell, 0);
        setChanged();
    }

    private void assign(EquipmentShape shape, ShapeRasterizer.Cell cell, long nozzles) {
        this.shape = shape;
        cellX = cell.x();
        cellY = cell.y();
        cellZ = cell.z();
        this.nozzles = nozzles;
        key = ShapeRasterizer.key(shape, cellX, cellY, cellZ, nozzles);
    }

    public @Nullable EquipmentShape shape() {
        return shape;
    }

    public @Nullable ShapeRasterizer.CellKey key() {
        return key;
    }

    public ShapeRasterizer.Cell cell() {
        return new ShapeRasterizer.Cell(cellX, cellY, cellZ);
    }

    public long nozzles() {
        return nozzles;
    }

    /** Server: draw this cell's nozzles at new sizes; syncs when its key changes. */
    public void nozzles(long nozzles) {
        if (shape == null || nozzles == this.nozzles) return;
        var before = key;
        assign(shape, cell(), nozzles);
        setChanged();
        if (!key.equals(before) && level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    /** The vessel's origin cell (its lower north-west corner), or null while unbound. */
    public @Nullable BlockPos origin() {
        return shape == null ? null : worldPosition.offset(-cellX, -cellY, -cellZ);
    }

    /** The nozzle whose flange face this cell holds, facing {@code outward}, or null. */
    public EquipmentShape.@Nullable Nozzle nozzleFacing(Direction outward) {
        var port = port(outward);
        return port == null ? null : shape.nozzles().get(port.nozzle());
    }

    /** The port this cell offers towards {@code outward}, or null. */
    public ShapeRasterizer.@Nullable Port port(Direction outward) {
        if (shape == null) return null;
        for (var port : ShapeRasterizer.ports(shape))
            if (port.cell().x() == cellX && port.cell().y() == cellY && port.cell().z() == cellZ
                    && port.dx() == outward.getStepX() && port.dy() == outward.getStepY() && port.dz() == outward.getStepZ()) return port;
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
        if (identity != 0) tag.putLong("FluidIdentity", identity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shape = null;
        key = null;
        identity = tag.getLong("FluidIdentity");
        lastView = null;
        if (!tag.contains("Diameter")) return;
        try {
            var read = readShape(tag);
            int[] cell = tag.getIntArray("Cell");
            if (cell.length != 3 || cell[0] < 0 || cell[0] >= read.sizeX() || cell[1] < 0 || cell[1] >= read.sizeY() || cell[2] < 0 || cell[2] >= read.sizeZ())
                throw new IllegalArgumentException("Cell outside the vessel");
            assign(read, new ShapeRasterizer.Cell(cell[0], cell[1], cell[2]), tag.getLong("Nozzles"));
        } catch (IllegalArgumentException invalid) {
            CreateChemE.LOGGER.warn("Equipment shell at {} has invalid data and stays unbound: {}", worldPosition, invalid.getMessage());
        }
    }

    private void write(CompoundTag tag) {
        if (shape == null) return;
        writeShape(tag, shape);
        tag.putIntArray("Cell", new int[]{cellX, cellY, cellZ});
        tag.putLong("Nozzles", nozzles);
    }

    /** A shape's NBT: kind, diameter, tangent length, orientation, front, support, a column's trays and options, a drum's side connections. */
    public static void writeShape(CompoundTag tag, EquipmentShape shape) {
        tag.putString("Kind", shape.kind().name().toLowerCase(Locale.ROOT));
        tag.putDouble("Diameter", shape.diameter());
        tag.putDouble("TangentLength", shape.tangentLength());
        tag.putString("Orientation", shape.orientation().name().toLowerCase(Locale.ROOT));
        tag.putString("Front", shape.front().name().toLowerCase(Locale.ROOT));
        tag.putInt("Support", shape.support());
        if (shape.trays() != null) {
            tag.putIntArray("Trays", shape.trays().encode());
            tag.putInt("Options", shape.options().encode());
        } else tag.putIntArray("Connections", EquipmentShape.Connection.encode(shape.connections()));
    }

    /** @throws IllegalArgumentException for a missing or invalid field */
    public static EquipmentShape readShape(CompoundTag tag) {
        var kind = EquipmentShape.Kind.valueOf(tag.getString("Kind").toUpperCase(Locale.ROOT));
        var front = EquipmentShape.Heading.valueOf(tag.getString("Front").toUpperCase(Locale.ROOT));
        if (kind == EquipmentShape.Kind.COLUMN)
            return EquipmentShape.column(tag.getDouble("Diameter"), ColumnTrays.decode(tag.getIntArray("Trays")), ColumnOptions.decode(tag.getInt("Options")), front,
                    tag.getInt("Support"));
        if (!tag.contains("Connections")) throw new IllegalArgumentException("A drum lists its side connections");
        return EquipmentShape.drum(tag.getDouble("Diameter"), tag.getDouble("TangentLength"),
                EquipmentShape.Orientation.valueOf(tag.getString("Orientation").toUpperCase(Locale.ROOT)), front, tag.getInt("Support"),
                EquipmentShape.Connection.decode(tag.getIntArray("Connections")));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        write(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Data that arrives after the block was meshed: refresh the model data and rebuild the section. */
    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        requestModelDataUpdate();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
    }

    @Override
    public ModelData getModelData() {
        return key == null ? ModelData.EMPTY : ModelData.of(CELL, key);
    }
}
