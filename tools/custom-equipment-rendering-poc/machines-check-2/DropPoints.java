package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/**
 * World points for the in-game climbing check: for a unit placed by {@code /createcheme equipment unit X Y Z kind size
 * length support north}, each ladder's climb-zone centre (0.36 m in front of the rungs) near its top, and the yaw that
 * faces the rungs. Args: kind size length support X Y Z.
 */
public final class DropPoints {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Heading.NORTH, Integer.parseInt(args[3]));
        int x = Integer.parseInt(args[4]), y = Integer.parseInt(args[5]), z = Integer.parseInt(args[6]);
        int ox = x - shape.sizeX() / 2, oz = z - shape.sizeZ() / 2;
        int k = 0;
        for (var l : ProcessUnit.of(shape).ladders()) {
            double out = Access.RUNG_FACE + .3;
            var b = shape.transform().point(new double[]{l.x() + out * l.outX(), l.rungTop() - .4, l.z() + out * l.outZ()});
            var dir = shape.transform().point(new double[]{l.x() - l.outX(), 0, l.z() - l.outZ()});
            var at = shape.transform().point(new double[]{l.x(), 0, l.z()});
            double yaw = Math.toDegrees(Math.atan2(-(dir[0] - at[0]), dir[2] - at[2]));
            System.out.printf("ladder %d foot %.2f landing %.2f: tp @s %.3f %.2f %.3f %.0f 0%n", k++, l.base(), l.rungTop(), ox + b[0], y + b[1], oz + b[2], yaw);
        }
    }
}
