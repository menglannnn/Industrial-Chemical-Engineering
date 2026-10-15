package com.wormzjl.createcheme.world.level.block.entity;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.registry.ModBlockEntities;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.VesselPlacement;
import com.wormzjl.createcheme.world.level.block.EquipmentConstructorBlock;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;
import java.util.Objects;

/**
 * The design an equipment constructor builds: the vessel's shape and where it stands relative to the constructor.
 * Never ticks. The design is synced to clients so that looking at the constructor shows the vessel's ghost.
 */
public final class EquipmentConstructorBlockEntity extends BlockEntity {
    /**
     * A vessel relative to its constructor: diameter, tangent length, which way it lies, its support height and where it
     * stands. Like the placement, the form only becomes a world direction with the constructor's facing.
     */
    public record Design(double diameter, int tangentLength, VesselPlacement.Form form, int support, VesselPlacement placement) {
        /** What a new constructor proposes: a 2 m drum, 4 m between tangent lines, on a 2 m skirt, right in front of it. */
        public static final Design DEFAULT = new Design(2, 4, VesselPlacement.Form.VERTICAL, EquipmentShape.defaultSupport(2, Orientation.VERTICAL),
                VesselPlacement.DEFAULT);

        public Design {
            Objects.requireNonNull(form, "form");
            Objects.requireNonNull(placement, "placement");
            // Validates the shape from the parameters (a compact constructor assigns the fields after its body); the
            // facing does not change what is valid.
            new EquipmentShape(diameter, tangentLength, form.orientation(0, -1), support);
        }

        public EquipmentShape shape(Direction facing) {
            return new EquipmentShape(diameter, tangentLength, form.orientation(facing.getStepX(), facing.getStepZ()), support);
        }
    }

    private Design design = Design.DEFAULT;

    public EquipmentConstructorBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.EQUIPMENT_CONSTRUCTOR.get(), position, state);
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
        design = Design.DEFAULT;
        if (!tag.contains("Diameter")) return;
        try {
            int[] offsets = tag.getIntArray("Offsets");
            if (offsets.length != 3) throw new IllegalArgumentException("Offsets must hold forward, right and up");
            design = new Design(tag.getDouble("Diameter"), tag.getInt("TangentLength"), VesselPlacement.Form.valueOf(tag.getString("Form").toUpperCase(Locale.ROOT)),
                    tag.getInt("Support"), new VesselPlacement(offsets[0], offsets[1], offsets[2]));
        } catch (IllegalArgumentException invalid) {
            CreateChemE.LOGGER.warn("Equipment constructor at {} has an invalid design and proposes the default: {}", worldPosition, invalid.getMessage());
        }
    }

    private void write(CompoundTag tag) {
        tag.putDouble("Diameter", design.diameter());
        tag.putInt("TangentLength", design.tangentLength());
        tag.putString("Form", design.form().name().toLowerCase(Locale.ROOT));
        tag.putInt("Support", design.support());
        var p = design.placement();
        tag.putIntArray("Offsets", new int[]{p.forward(), p.right(), p.up()});
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
