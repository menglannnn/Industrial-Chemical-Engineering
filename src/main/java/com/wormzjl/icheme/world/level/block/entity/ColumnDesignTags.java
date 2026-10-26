package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.science.equipment.ColumnComponents;
import com.wormzjl.icheme.science.equipment.ColumnDesign;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.ShapeRepair;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * The tags of a saved column design (batch 2026-10-26-multi-machine-framework): the trays, the diameter and the
 * components, whose kinds are saved by name. A column shell's and a constructor's components are equipment facts
 * (AGENTS.md): their fields are append-only and read through {@link ShapeRepair}.
 */
public final class ColumnDesignTags {
    private ColumnDesignTags() {
    }

    public static CompoundTag write(ColumnDesign design) {
        var tag = writeComponents(design.components());
        tag.putIntArray("Trays", design.trays().encode());
        tag.putDouble("Diameter", design.diameter());
        return tag;
    }

    /** @throws IllegalArgumentException for a missing, invalid or inconsistent field */
    public static ColumnDesign read(CompoundTag tag) {
        if (!tag.contains("Trays", Tag.TAG_INT_ARRAY) || !tag.contains("Diameter", Tag.TAG_DOUBLE)) throw new IllegalArgumentException("design trays or diameter missing");
        int[] trays = tag.getIntArray("Trays");
        if (trays.length > ColumnTrays.MAX_ENCODED) throw new IllegalArgumentException("design trays overlong");
        return new ColumnDesign(ColumnTrays.decode(trays).withComponents(saved(tag).resolve()), tag.getDouble("Diameter"));
    }

    /** The components' fields: the condenser, the reflux pump, the pumparound pumps by name, and the two flags. */
    public static CompoundTag writeComponents(ColumnComponents components) {
        var tag = new CompoundTag();
        tag.putString("Condenser", components.condenser().name());
        tag.putString("RefluxPump", components.refluxPump().name());
        var pumps = new ListTag();
        for (var pump : components.pumparoundPumps()) pumps.add(StringTag.valueOf(pump.name()));
        tag.put("PumparoundPumps", pumps);
        tag.putBoolean("Ejector", components.ejector());
        tag.putBoolean("Kettle", components.kettle());
        return tag;
    }

    /**
     * The component design saved under {@code key} in {@code parent}, as written: null when the key is absent (a column
     * without one); a field that is not a compound reads as one with no kinds, which the shape's reader names as repaired.
     */
    public static @Nullable ShapeRepair.SavedComponents saved(CompoundTag parent, String key) {
        if (!parent.contains(key)) return null;
        return saved(parent.contains(key, Tag.TAG_COMPOUND) ? parent.getCompound(key) : new CompoundTag());
    }

    /** The component fields of {@code tag} as written, before any rule reads them; a missing kind reads as an empty name. */
    private static ShapeRepair.SavedComponents saved(CompoundTag tag) {
        var pumps = new ArrayList<String>();
        if (tag.get("PumparoundPumps") instanceof ListTag list)
            for (int i = 0; i < Math.min(list.size(), ColumnTrays.MAX_PUMPAROUNDS + 1); i++)
                pumps.add(list.get(i) instanceof StringTag name ? name.getAsString() : String.valueOf(list.get(i)));
        return new ShapeRepair.SavedComponents(tag.getString("Condenser"), tag.getString("RefluxPump"), pumps, tag.getBoolean("Ejector"), tag.getBoolean("Kettle"));
    }
}
