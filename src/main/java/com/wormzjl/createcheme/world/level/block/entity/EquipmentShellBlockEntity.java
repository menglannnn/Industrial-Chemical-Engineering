package com.wormzjl.createcheme.world.level.block.entity;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.registry.ModBlockEntities;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * One shell cell of a parametric vessel: the vessel's shape and this cell's coordinates inside it. Never ticks. The
 * client model turns the cell's {@link ShapeRasterizer.CellKey} into the quads of this cell, and the block turns it into
 * its collision shape, so the cell needs nothing from any other cell or chunk.
 */
public final class EquipmentShellBlockEntity extends BlockEntity {
    /** Model data read by the shell model while the chunk is meshed. */
    public static final ModelProperty<ShapeRasterizer.CellKey> CELL = new ModelProperty<>();
    private @Nullable EquipmentShape shape;
    private int cellX, cellY, cellZ;
    private @Nullable ShapeRasterizer.CellKey key;

    public EquipmentShellBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.EQUIPMENT_SHELL.get(), position, state);
    }

    public void bind(EquipmentShape shape, ShapeRasterizer.Cell cell) {
        assign(shape, cell);
        setChanged();
    }

    private void assign(EquipmentShape shape, ShapeRasterizer.Cell cell) {
        this.shape = shape;
        cellX = cell.x();
        cellY = cell.y();
        cellZ = cell.z();
        key = ShapeRasterizer.key(shape, cellX, cellY, cellZ);
    }

    public @Nullable EquipmentShape shape() {
        return shape;
    }

    public @Nullable ShapeRasterizer.CellKey key() {
        return key;
    }

    /** The vessel's origin cell (its lower north-west corner), or null while unbound. */
    public @Nullable BlockPos origin() {
        return shape == null ? null : worldPosition.offset(-cellX, -cellY, -cellZ);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shape = null;
        key = null;
        if (!tag.contains("Diameter")) return;
        try {
            var read = new EquipmentShape(tag.getDouble("Diameter"), tag.getDouble("TangentLength"),
                    EquipmentShape.Orientation.valueOf(tag.getString("Orientation").toUpperCase(Locale.ROOT)), tag.getInt("Support"));
            int[] cell = tag.getIntArray("Cell");
            if (cell.length != 3 || cell[0] < 0 || cell[0] >= read.sizeX() || cell[1] < 0 || cell[1] >= read.sizeY() || cell[2] < 0 || cell[2] >= read.sizeZ())
                throw new IllegalArgumentException("Cell outside the vessel");
            assign(read, new ShapeRasterizer.Cell(cell[0], cell[1], cell[2]));
        } catch (IllegalArgumentException invalid) {
            CreateChemE.LOGGER.warn("Equipment shell at {} has invalid data and stays unbound: {}", worldPosition, invalid.getMessage());
        }
    }

    private void write(CompoundTag tag) {
        if (shape == null) return;
        tag.putDouble("Diameter", shape.diameter());
        tag.putDouble("TangentLength", shape.tangentLength());
        tag.putString("Orientation", shape.orientation().name().toLowerCase(Locale.ROOT));
        tag.putInt("Support", shape.support());
        tag.putIntArray("Cell", new int[]{cellX, cellY, cellZ});
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
