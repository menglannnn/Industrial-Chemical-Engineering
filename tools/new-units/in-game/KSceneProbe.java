package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/** Kit in-game check, scene C: per pick (kind size length support, length/support -1 = default) the box and the ranges. */
public class KSceneProbe {
    public static void main(String[] a) {
        Object[][] picks = {
                {Kind.CYCLONE, 2.0, -1, 8}, {Kind.STIRRED_REACTOR, 4.0, 8, 3}, {Kind.ADSORBER, 4.6, 1, 3},
                {Kind.FLUIDIZED_BED_GASIFIER, 3.75, -1, 16}, {Kind.FCC_CONVERTER, 17.0, -1, -1}, {Kind.CABIN_HEATER, 12.0, 15, 3},
                {Kind.CABIN_HEATER, 12.0, 15, 12}, {Kind.FLUIDIZED_BED_BOILER, 26.0, 3, -1}, {Kind.FLUIDIZED_BED_BOILER, 26.0, 3, 8},
                {Kind.CYCLONE, 1.1, 2, 4}, {Kind.DELAYED_COKER, 4.0, -1, -1}};
        for (var p : picks) {
            var kind = (Kind) p[0];
            double size = (Double) p[1];
            var d = UnitDesign.of(kind);
            int length = (Integer) p[2] < 0 ? d.defaultLength(size) : (Integer) p[2];
            int support = (Integer) p[3] < 0 ? d.defaultSupport(size, length) : (Integer) p[3];
            var shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, support);
            System.out.printf(Locale.ROOT, "%s %s %d %d  box x%d y%d z%d  lengthRange=%d..%d support=%d..%d default length %d support %d%n",
                    kind.name().toLowerCase(Locale.ROOT), UnitDesign.size(size), length, support, shape.sizeX(), shape.sizeY(), shape.sizeZ(),
                    d.minLength(size), d.maxLength(size), d.minSupport(size, length), d.maxSupport(), d.defaultLength(size), d.defaultSupport(size, d.defaultLength(size)));
        }
    }
}
