package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/** Each moving part of a unit: pivot and axis in box coordinates, motion, reach, polygon count. Args: kind size length support. */
public class MovingParts {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Integer.parseInt(args[2]), Heading.NORTH, Integer.parseInt(args[3]));
        System.out.printf("box %d x %d x %d%n", shape.sizeX(), shape.sizeY(), shape.sizeZ());
        int k = 0;
        for (var part : ShapeRasterizer.moving(shape)) {
            var m = part.motion();
            System.out.printf("%d pivot (%.2f, %.2f, %.2f) axis (%.0f, %.0f, %.0f) speed %.1f phase %.3f crank %.3f reach %.2f polygons %d%n", k++, m.pivot()[0], m.pivot()[1], m.pivot()[2],
                    m.axis()[0], m.axis()[1], m.axis()[2], m.speed(), m.phase(), m.crank(), part.reach(), part.polygons().size());
        }
    }
}
