package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.registry.ModBlockEntities;
import com.wormzjl.icheme.runtime.fluid.FluidView;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.world.inventory.FluidDeviceMenu;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.science.equipment.ProcessUnit;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One shell cell of a parametric vessel: the vessel's shape, this cell's coordinates inside it, the drawn sizes of the
 * nozzles that reach it, and the vessel's {@link Paint}. Never ticks. The client model turns the cell's
 * {@link ShapeRasterizer.CellKey} into the quads of this cell and tints them with the paint, and the block turns the key
 * into its collision shape, so the cell needs nothing from any other cell or chunk. The nozzle sizes follow the pipes
 * connected to them; the server sets them ({@code EquipmentStructure}).
 *
 * <p>A drum is a reservoir of the fluid network: its node cell ({@link ShapeRasterizer#fluidCell}) carries the drum's
 * fluid identity and is its presentation binding, as a device block entity is for a one-block tank. Nothing of the
 * drum's contents is kept here.
 */
public final class EquipmentShellBlockEntity extends BlockEntity implements FluidView.Receiver {
    /** Model data read by the shell model while the chunk is meshed. */
    public static final ModelProperty<ShapeRasterizer.CellKey> CELL = new ModelProperty<>();
    /** A saved cell or box beyond this many cells along any side is corrupt (the largest unit is under 100). */
    private static final int MAX_CELL = 512;
    private @Nullable EquipmentShape shape;
    private int cellX, cellY, cellZ;
    private long nozzles;
    /** The vessel's paint, the same on every cell; the client tints the cell's painted parts with it. */
    private Paint paint = Paint.DEFAULT;
    private @Nullable ShapeRasterizer.CellKey key;
    /** The drum's fluid identity, on its node cell only (0 elsewhere and while unregistered). */
    private long identity;
    /**
     * The layout this cell was placed by: its {@link ShapeRasterizer#stamp} and its box (0 for a cell saved before the
     * stamp existed). Another stamp on load sends the vessel to {@link EquipmentStructure#refreshLater}.
     */
    private long stamp;
    private int[] box = new int[3];
    private @Nullable FluidView lastView;
    /**
     * Client: the cells holding a moving part's pivot, whose parts the client moves (owner requests 2026-10-16 and
     * 2026-10-18: air cooler fans, pump shafts and motor fans, compressor piston rods). Render thread only; weak, so an
     * unloaded level frees them.
     */
    private static final Set<EquipmentShellBlockEntity> CLIENT_MOVING = Collections.newSetFromMap(new WeakHashMap<>());
    /** Client: the moving parts whose pivots this cell holds, by index ({@link ShapeRasterizer#moving}). */
    private int[] moving = new int[0];

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

    /** Client: the cells holding a moving part's pivot in the client's level. */
    public static List<EquipmentShellBlockEntity> clientMovingCells() {
        return List.copyOf(CLIENT_MOVING);
    }

    public int[] moving() {
        return moving;
    }

    /** Client: lists the moving parts whose pivots lie in this cell ({@link ShapeRasterizer#moving}) for the client that moves them. */
    private void trackMoving() {
        moving = new int[0];
        CLIENT_MOVING.remove(this);
        if (shape == null || level == null || !level.isClientSide || !shape.unit() || !ProcessUnit.moves(shape.kind())) return;
        var parts = ShapeRasterizer.moving(shape);
        var here = new ArrayList<Integer>();
        for (int k = 0; k < parts.size(); k++) {
            var p = parts.get(k).motion().pivot();
            if (Math.floor(p[0]) == cellX && Math.floor(p[1]) == cellY && Math.floor(p[2]) == cellZ) here.add(k);
        }
        if (here.isEmpty()) return;
        moving = here.stream().mapToInt(Integer::intValue).toArray();
        CLIENT_MOVING.add(this);
    }

    /** Server: a node cell binds the drum the ledger holds here, as a device block entity binds its registration; the client lists its moving parts. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) trackMoving();
        if (!(level instanceof ServerLevel serverLevel) || shape == null) return;
        if (stale()) EquipmentStructure.refreshLater(serverLevel, origin(), shape, box, worldPosition);
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

    /** Unloaded or removed: publications stop marking the drum; the client stops moving its parts. */
    private void released() {
        CLIENT_MOVING.remove(this);
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
                return Component.translatable("gui.icheme.vessel.title.drum");
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

    public void bind(EquipmentShape shape, ShapeRasterizer.Cell cell, Paint paint) {
        assign(shape, cell, 0);
        this.paint = paint;
        stamp = ShapeRasterizer.stamp(shape);
        box = new int[]{shape.sizeX(), shape.sizeY(), shape.sizeZ()};
        setChanged();
    }

    /** A cell inside the vessel's box takes its key; one a smaller layout left outside draws nothing until its vessel is refreshed. */
    private void assign(EquipmentShape shape, ShapeRasterizer.Cell cell, long nozzles) {
        this.shape = shape;
        cellX = cell.x();
        cellY = cell.y();
        cellZ = cell.z();
        this.nozzles = nozzles;
        boolean inside = cellX < shape.sizeX() && cellY < shape.sizeY() && cellZ < shape.sizeZ();
        key = inside ? ShapeRasterizer.key(shape, cellX, cellY, cellZ, nozzles) : null;
    }

    /** Whether this cell was placed by another layout than the one the code draws for its shape (server, after load). */
    public boolean stale() {
        return shape != null && (key == null || stamp != ShapeRasterizer.stamp(shape));
    }

    /** The box of the layout this cell was placed by (zeros when it was saved before the stamp existed). */
    public int[] placedBox() {
        return box.clone();
    }

    /** Server: a refreshed drum's node moved elsewhere; this cell no longer stands for its fluid identity. */
    public void releaseIdentity() {
        if (identity == 0) return;
        released();
        identity = 0;
        setChanged();
    }

    public @Nullable EquipmentShape shape() {
        return shape;
    }

    public Paint paint() {
        return paint;
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
        paint = Paint.DEFAULT;
        stamp = 0;
        box = new int[3];
        identity = tag.getLong("FluidIdentity");
        lastView = null;
        if (!tag.contains("Diameter")) return;
        try {
            var read = readShape(tag);
            // A cell outside the box of the current layout stays bound until its vessel is refreshed (it was placed by
            // a larger one); the box it was placed by, when saved, bounds it.
            int[] cell = tag.getIntArray("Cell"), placed = tag.contains("Box") ? tag.getIntArray("Box") : new int[3];
            if (cell.length != 3 || cell[0] < 0 || cell[1] < 0 || cell[2] < 0 || cell[0] >= MAX_CELL || cell[1] >= MAX_CELL || cell[2] >= MAX_CELL)
                throw new IllegalArgumentException("Cell outside any vessel");
            if (placed.length != 3 || placed[0] < 0 || placed[1] < 0 || placed[2] < 0 || placed[0] > MAX_CELL || placed[1] > MAX_CELL || placed[2] > MAX_CELL)
                throw new IllegalArgumentException("Invalid placed box");
            var painted = Paint.decode(tag.getIntArray("Paint"));
            assign(read, new ShapeRasterizer.Cell(cell[0], cell[1], cell[2]), tag.getLong("Nozzles"));
            paint = painted;
            stamp = tag.getLong("Layout");
            box = placed;
        } catch (IllegalArgumentException invalid) {
            IChemE.LOGGER.warn("Equipment shell at {} has invalid data and stays unbound: {}", worldPosition, invalid.getMessage());
        }
    }

    private void write(CompoundTag tag) {
        if (shape == null) return;
        writeShape(tag, shape);
        tag.putIntArray("Cell", new int[]{cellX, cellY, cellZ});
        tag.putLong("Nozzles", nozzles);
        tag.putIntArray("Paint", paint.encode());
        tag.putLong("Layout", stamp);
        tag.putIntArray("Box", box);
    }

    /**
     * A shape's NBT: kind, diameter, tangent length, orientation, front, support, a column's trays, a drum's side
     * connections and whether it has a boot, an air cooler's bank.
     */
    public static void writeShape(CompoundTag tag, EquipmentShape shape) {
        tag.putString("Kind", shape.kind().name().toLowerCase(Locale.ROOT));
        tag.putDouble("Diameter", shape.diameter());
        tag.putDouble("TangentLength", shape.tangentLength());
        tag.putString("Orientation", shape.orientation().name().toLowerCase(Locale.ROOT));
        tag.putString("Front", shape.front().name().toLowerCase(Locale.ROOT));
        tag.putInt("Support", shape.support());
        if (shape.trays() != null) tag.putIntArray("Trays", shape.trays().encode());
        else if (shape.drum()) {
            tag.putIntArray("Connections", EquipmentShape.Connection.encode(shape.connections()));
            tag.putBoolean("Boot", shape.boot());
        }
        if (shape.kind() == EquipmentShape.Kind.AIR_COOLER) tag.putIntArray("Bank", shape.bank().encode());
    }

    /** @throws IllegalArgumentException for a missing or invalid field */
    public static EquipmentShape readShape(CompoundTag tag) {
        var kind = EquipmentShape.Kind.valueOf(tag.getString("Kind").toUpperCase(Locale.ROOT));
        var front = EquipmentShape.Heading.valueOf(tag.getString("Front").toUpperCase(Locale.ROOT));
        if (kind == EquipmentShape.Kind.COLUMN)
            return EquipmentShape.column(tag.getDouble("Diameter"), ColumnTrays.decode(tag.getIntArray("Trays")), front, tag.getInt("Support"));
        if (kind.designed()) return EquipmentShape.designed(kind, tag.getDouble("Diameter"), tag.getDouble("TangentLength"), front, tag.getInt("Support"),
                    kind == EquipmentShape.Kind.AIR_COOLER ? ProcessUnit.Bank.decode(tag.getIntArray("Bank")) : ProcessUnit.Bank.ONE);
        if (!tag.contains("Connections")) throw new IllegalArgumentException("A drum lists its side connections");
        return EquipmentShape.drum(tag.getDouble("Diameter"), tag.getDouble("TangentLength"),
                EquipmentShape.Orientation.valueOf(tag.getString("Orientation").toUpperCase(Locale.ROOT)), front, tag.getInt("Support"),
                EquipmentShape.Connection.decode(tag.getIntArray("Connections")), tag.getBoolean("Boot"));
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
        trackMoving();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
    }

    @Override
    public ModelData getModelData() {
        return key == null ? ModelData.EMPTY : ModelData.of(CELL, key);
    }
}
