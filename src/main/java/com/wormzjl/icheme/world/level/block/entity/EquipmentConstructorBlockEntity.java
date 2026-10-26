package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.registry.ModBlockEntities;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.science.equipment.ProcessUnit;
import com.wormzjl.icheme.science.equipment.ShapeRepair;
import com.wormzjl.icheme.science.equipment.VesselPlacement;
import com.wormzjl.icheme.world.level.block.EquipmentConstructorBlock;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The design an equipment constructor builds: the vessel's shape and where it stands relative to the constructor.
 * Never ticks. The design is synced to clients so that looking at the constructor shows the vessel's ghost.
 */
public final class EquipmentConstructorBlockEntity extends BlockEntity {
    /**
     * A vessel relative to its constructor: diameter, tangent length, which way it lies, its support height and where it
     * stands, and a drum's side connections; a column's height follows its trays instead of the tangent length; a unit
     * reads diameter, length and support as its own sizes ({@link ProcessUnit}). Like the placement, the form only becomes
     * a world direction with the constructor's facing. The drum's length and connections, the column's trays and the air
     * cooler's bank are all kept, so changing the form loses none of them; so is the boot, which only a lying drum carries.
     * The paint colours whatever is built. {@code outlet}: the quarter turns from the front a bottom outlet leaving through
     * a skirt takes (owner request 2026-10-25), valid for the form ({@link EquipmentShape#outlets}): a standing drum's or
     * a trickle-bed reactor's; 0 for every other form.
     */
    public record Design(double diameter, int tangentLength, VesselPlacement.Form form, int support, VesselPlacement placement, ColumnTrays trays,
                         List<EquipmentShape.Connection> connections, Paint paint, ProcessUnit.Bank bank, boolean boot, int outlet) {
        /** A new column's trays: twenty, fed in the middle. */
        public static final ColumnTrays TRAYS = ColumnTrays.simple(20, 10);
        /**
         * A 2 m drum, 4 m between tangent lines, on a 2 m skirt, right in front of its constructor, with a new drum's side
         * connection, its bottom outlet to the front, in the built-in paint; a new constructor proposes it in the configured
         * paint ({@link #fresh}).
         */
        public static final Design DEFAULT = new Design(2, 4, VesselPlacement.Form.VERTICAL, EquipmentShape.defaultSupport(2, Orientation.VERTICAL),
                VesselPlacement.DEFAULT, TRAYS, EquipmentShape.defaultConnections(2, 4, Orientation.VERTICAL, EquipmentShape.defaultSupport(2, Orientation.VERTICAL), false),
                Paint.DEFAULT, ProcessUnit.Bank.ONE, false, 0);

        /** What a new constructor proposes: {@link #DEFAULT} in the config's primary and secondary colours. */
        public static Design fresh() {
            var d = DEFAULT;
            return new Design(d.diameter, d.tangentLength, d.form, d.support, d.placement, d.trays, d.connections, IChemE.defaultPaint(), d.bank, d.boot, d.outlet);
        }

        public Design {
            Objects.requireNonNull(form, "form");
            Objects.requireNonNull(placement, "placement");
            Objects.requireNonNull(trays, "trays");
            Objects.requireNonNull(paint, "paint");
            Objects.requireNonNull(bank, "bank");
            connections = List.copyOf(connections);
            // Validates the shape from the parameters (a compact constructor assigns the fields after its body); the
            // facing does not change what is valid.
            shape(diameter, tangentLength, form, support, trays, connections, bank, boot, outlet, EquipmentShape.Heading.NORTH);
        }

        public EquipmentShape shape(Direction facing) {
            return shape(diameter, tangentLength, form, support, trays, connections, bank, boot, outlet, form.front(facing.getStepX(), facing.getStepZ()));
        }

        /** The shape; an outlet the form does not turn to is refused ({@link EquipmentShape#outlets}). */
        private static EquipmentShape shape(double diameter, int tangentLength, VesselPlacement.Form form, int support, ColumnTrays trays,
                                            List<EquipmentShape.Connection> connections, ProcessUnit.Bank bank, boolean boot, int outlet, EquipmentShape.Heading front) {
            if (form.designed())
                return EquipmentShape.designed(form.kind(), diameter, tangentLength, front, support, form.kind() == EquipmentShape.Kind.AIR_COOLER ? bank : ProcessUnit.Bank.ONE, outlet);
            if (form.column()) {
                if (outlet != 0) throw new IllegalArgumentException("A column's bottoms leave to the front");
                return EquipmentShape.column(diameter, trays, front, support);
            }
            return EquipmentShape.drum(diameter, tangentLength, form.orientation(), front, support, connections, boot && form.orientation().horizontal(), outlet);
        }
    }

    /**
     * Constructors loaded on the client, for their ghosts (owner request 2026-10-16: shown without aiming at the
     * constructor). Render thread only; weak, so an unloaded level frees them even without {@link #setRemoved}.
     */
    private static final Set<EquipmentConstructorBlockEntity> CLIENT_LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private Design design = Design.fresh();

    public EquipmentConstructorBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.EQUIPMENT_CONSTRUCTOR.get(), position, state);
    }

    /** Client: the constructors loaded in the client's level. */
    public static List<EquipmentConstructorBlockEntity> clientLoaded() {
        return List.copyOf(CLIENT_LOADED);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) CLIENT_LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_LOADED.remove(this);
    }

    public Design design() {
        return design;
    }

    public Direction facing() {
        return getBlockState().getValue(EquipmentConstructorBlock.FACING);
    }

    /** Server: store a design the player chose and show it to every client that sees the constructor. */
    public void apply(Design design) {
        if (design.equals(this.design)) return;
        this.design = design;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** The vessel this constructor would build for a design: its orientation and origin follow the constructor's facing. */
    public EquipmentStructure.Vessel vessel(Design design) {
        var facing = facing();
        var shape = design.shape(facing);
        int[] origin = design.placement().origin(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                facing.getStepX(), facing.getStepZ(), shape.sizeX(), shape.sizeZ());
        return new EquipmentStructure.Vessel(new BlockPos(origin[0], origin[1], origin[2]), shape);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = Design.fresh();
        if (!tag.contains("Diameter")) return;
        try {
            design = readDesign(tag, design, repair -> IChemE.LOGGER.warn("equipment_compat constructor at={} repair={}", worldPosition.toShortString(), repair));
        } catch (IllegalArgumentException invalid) {
            IChemE.LOGGER.error("Equipment constructor at {} has a design its repair cannot read and proposes the default: {}", worldPosition, invalid.getMessage());
        }
    }

    /**
     * A saved design read onto the current rules (batch 2026-10-26-backward-compat): its shape through {@link ShapeRepair},
     * its placement and paint kept where valid; a form this build no longer has proposes the default shape with the
     * saved placement and paint, as {@code fresh} (what a new constructor proposes) has them otherwise. Each change goes to
     * {@code repairs}.
     */
    public static Design readDesign(CompoundTag tag, Design fresh, java.util.function.Consumer<String> repairs) {
        var placement = fresh.placement();
        int[] offsets = tag.getIntArray("Offsets");
        try {
            if (offsets.length != 3) throw new IllegalArgumentException("Offsets must hold forward, right and up");
            placement = new VesselPlacement(offsets[0], offsets[1], offsets[2]);
        } catch (IllegalArgumentException invalid) {
            repairs.accept("placement (" + invalid.getMessage() + ") -> default");
        }
        var paint = fresh.paint();
        try {
            paint = Paint.decode(tag.getIntArray("Paint"));
        } catch (IllegalArgumentException invalid) {
            repairs.accept("paint (" + invalid.getMessage() + ") -> default");
        }
        var form = ShapeRepair.form(tag.getString("Form")).orElse(null);
        if (form == null) {
            repairs.accept("form " + tag.getString("Form") + " -> " + fresh.form().name().toLowerCase(Locale.ROOT) + " (this build no longer has it)");
            return new Design(fresh.diameter(), fresh.tangentLength(), fresh.form(), fresh.support(), placement, fresh.trays(), fresh.connections(), paint, fresh.bank(), fresh.boot(), fresh.outlet());
        }
        var trays = Design.TRAYS;
        if (tag.contains("Trays"))
            try {
                trays = ColumnTrays.decode(tag.getIntArray("Trays"));
            } catch (IllegalArgumentException invalid) {
                repairs.accept("trays (" + invalid.getMessage() + ") -> " + Design.TRAYS.trays() + " trays");
            }
        var read = ShapeRepair.read(new ShapeRepair.Saved(form.kind().name(), "north", form.orientation().name(), tag.getDouble("Diameter"), tag.getInt("TangentLength"),
                tag.getInt("Support"), trays.encode(), tag.contains("Connections") ? tag.getIntArray("Connections") : null, tag.getBoolean("Boot"), tag.getInt("Outlet"),
                tag.contains("Bank") ? tag.getIntArray("Bank") : null));
        read.repairs().forEach(repairs);
        var shape = read.shape();
        // A column's own length follows its trays; the design keeps the drum length it was saved with for another form.
        int length = form.column() ? (int) Math.max(1, Math.min(EquipmentShape.MAX_TANGENT_LENGTH, tag.getInt("TangentLength"))) : (int) shape.tangentLength();
        return new Design(shape.diameter(), length, form, shape.support(), placement, shape.trays() != null ? shape.trays() : trays, shape.connections(), paint,
                shape.bank(), shape.boot(), shape.outlet());
    }

    private void write(CompoundTag tag) {
        tag.putDouble("Diameter", design.diameter());
        tag.putInt("TangentLength", design.tangentLength());
        tag.putString("Form", design.form().name().toLowerCase(Locale.ROOT));
        tag.putInt("Support", design.support());
        var p = design.placement();
        tag.putIntArray("Offsets", new int[]{p.forward(), p.right(), p.up()});
        tag.putIntArray("Trays", design.trays().encode());
        tag.putIntArray("Connections", EquipmentShape.Connection.encode(design.connections()));
        tag.putIntArray("Paint", design.paint().encode());
        tag.putIntArray("Bank", design.bank().encode());
        tag.putBoolean("Boot", design.boot());
        tag.putInt("Outlet", design.outlet());
    }

    /**
     * Server: the design of the column calculator nearest this constructor within {@code reach} blocks, as a column with
     * this design's support (at least the column's least skirt) and placement; its diameter where the calculator sizes
     * its trays (otherwise this design's). A read of the calculator's stored input: nothing is solved.
     */
    public java.util.Optional<Design> importColumn(int reach) {
        if (level == null) return java.util.Optional.empty();
        ColumnCalculatorV3BlockEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (var at : BlockPos.betweenClosed(worldPosition.offset(-reach, -reach, -reach), worldPosition.offset(reach, reach, reach))) {
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof ColumnCalculatorV3BlockEntity calculator)) continue;
            double distance = at.distSqr(worldPosition);
            if (distance < best) {
                best = distance;
                nearest = calculator;
            }
        }
        if (nearest == null) return java.util.Optional.empty();
        var input = nearest.currentInput();
        double sized = input.columnDiameterMetres();
        double diameter = sized > 0 ? Math.max(EquipmentShape.MIN_DIAMETER, Math.min(EquipmentShape.MAX_COLUMN_DIAMETER, Math.round(sized * 2) / 2.0))
                : Math.min(EquipmentShape.MAX_COLUMN_DIAMETER, design.diameter());
        return java.util.Optional.of(new Design(diameter, design.tangentLength(), VesselPlacement.Form.COLUMN, Math.max(design.support(), EquipmentShape.minimumSkirt(diameter)),
                design.placement(), ColumnTrays.of(input), design.connections(), design.paint(), design.bank(), design.boot(), 0));
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
}
