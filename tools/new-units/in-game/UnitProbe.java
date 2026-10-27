package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/** Box size and moving parts (pivot, axis, speed, crank, reach, polygons) of the scene units. Box coordinates, heading north. */
public final class UnitProbe {
    public static void main(String[] args) {
        String[][] units = {{"STIRRED_REACTOR", "2.6", "3", "1"}, {"ADSORBER", "2.8", "2", "2"}, {"TRICKLE_BED_REACTOR", "3", "2", "4"},
                {"EBULLATED_BED_REACTOR", "4.5", "23", "9"}, {"CENTRIFUGAL_COMPRESSOR", "0.9", "4", "6"}, {"STEAM_TURBINE", "1.8", "6", "0"},
                {"CONDENSING_TURBINE", "1.8", "6", "6"}, {"CYCLONE", "1.1", "2", "4"}, {"FCC_CONVERTER", "10.5", "34", "9"}, {"DELAYED_COKER", "7", "2", "12"},
                {"FLUIDIZED_BED_BOILER", "15", "2", "6"}, {"FLUIDIZED_BED_GASIFIER", "3.75", "1", "9"}, {"GRATE_BOILER", "7", "8", "3"}, {"FIXED_BED_GASIFIER", "3.5", "1", "3"}};
        for (var a : units) {
            var shape = EquipmentShape.unit(Kind.valueOf(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Heading.NORTH, Integer.parseInt(a[3]));
            var moving = ShapeRasterizer.moving(shape);
            System.out.printf("%s box %d x %d x %d moving %d%n", a[0], shape.sizeX(), shape.sizeY(), shape.sizeZ(), moving.size());
            int k = 0;
            for (var part : moving) {
                var m = part.motion();
                System.out.printf("  %d pivot (%.2f, %.2f, %.2f) axis (%.0f, %.0f, %.0f) speed %.3f crank %.3f rod %.3f reach %.2f polygons %d%n", k++, m.pivot()[0], m.pivot()[1], m.pivot()[2],
                        m.axis()[0], m.axis()[1], m.axis()[2], m.speed(), m.crank(), m.rod(), part.reach(), part.polygons().size());
                if (k >= 8) { System.out.println("  ..."); break; }
            }
        }
    }
}
