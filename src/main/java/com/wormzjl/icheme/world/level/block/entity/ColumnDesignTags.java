package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.science.equipment.ColumnComponents;
import com.wormzjl.icheme.science.equipment.ColumnDesign;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * The tags of a saved column design (batch 2026-10-26-multi-machine-framework): the trays, the diameter and the
 * components, whose kinds are saved by name.
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
        return new ColumnDesign(ColumnTrays.decode(trays), tag.getDouble("Diameter"), readComponents(tag));
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

    /** @throws IllegalArgumentException for a missing field or an unknown kind */
    public static ColumnComponents readComponents(CompoundTag tag) {
        for (var key : new String[]{"Condenser", "RefluxPump"})
            if (!tag.contains(key, Tag.TAG_STRING)) throw new IllegalArgumentException("design " + key + " missing");
        if (!tag.contains("PumparoundPumps", Tag.TAG_LIST) || !tag.contains("Ejector", Tag.TAG_BYTE) || !tag.contains("Kettle", Tag.TAG_BYTE))
            throw new IllegalArgumentException("design pumps or flags missing");
        var list = (ListTag) tag.get("PumparoundPumps");
        if (!list.isEmpty() && list.getElementType() != Tag.TAG_STRING || list.size() > ColumnTrays.MAX_PUMPAROUNDS)
            throw new IllegalArgumentException("design pumparound pumps invalid");
        var pumps = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) pumps.add(list.getString(i));
        return ColumnComponents.named(tag.getString("Condenser"), tag.getString("RefluxPump"), pumps, tag.getBoolean("Ejector"), tag.getBoolean("Kettle"));
    }
}
