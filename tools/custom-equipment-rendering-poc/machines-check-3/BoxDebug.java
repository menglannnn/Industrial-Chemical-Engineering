package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import java.util.Arrays;

/** A unit's frame box and its nozzles. Args: kind size length support. */
public final class BoxDebug {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Heading.NORTH, Integer.parseInt(args[3]));
        var unit = ProcessUnit.of(shape);
        System.out.println("box " + Arrays.toString(unit.box()) + " size " + shape.sizeX() + "x" + shape.sizeY() + "x" + shape.sizeZ());
        for (var n : unit.nozzles()) System.out.println(n.role() + " base " + Arrays.toString(n.base()) + " axis " + n.axis() + n.sign() + " length " + n.length());
    }
}
