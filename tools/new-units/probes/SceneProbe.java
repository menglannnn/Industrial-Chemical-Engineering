package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/** Prints, for each new unit kind at a middle size, its default second size and support and its box, for an in-game scene. */
public class SceneProbe {
    public static void main(String[] a) {
        Object[][] picks = {
                {Kind.STIRRED_REACTOR, 2.6}, {Kind.ADSORBER, 2.8}, {Kind.TRICKLE_BED_REACTOR, 3.0}, {Kind.EBULLATED_BED_REACTOR, 4.5},
                {Kind.CENTRIFUGAL_COMPRESSOR, 0.9}, {Kind.STEAM_TURBINE, 1.8}, {Kind.CONDENSING_TURBINE, 1.8}, {Kind.CYCLONE, 1.1},
                {Kind.FCC_CONVERTER, 10.5}, {Kind.DELAYED_COKER, 7.0}, {Kind.FLUIDIZED_BED_BOILER, 15.0},
                {Kind.FLUIDIZED_BED_GASIFIER, 3.75}, {Kind.GRATE_BOILER, 7.0}, {Kind.FIXED_BED_GASIFIER, 3.5}};
        for (var p : picks) {
            var kind = (Kind) p[0];
            double size = (Double) p[1];
            var d = UnitDesign.of(kind);
            int length = d.defaultLength(size), support = d.defaultSupport(size, length);
            var shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, support);
            System.out.printf(Locale.ROOT, "%s %s %d %d  box x%d y%d z%d  moves=%s lengthRange=%d..%d support=%d..%d%n",
                    kind.name().toLowerCase(Locale.ROOT), UnitDesign.size(size), length, support, shape.sizeX(), shape.sizeY(), shape.sizeZ(),
                    d.moves(), d.minLength(size), d.maxLength(size), d.minSupport(size, length), d.maxSupport());
        }
    }
}
