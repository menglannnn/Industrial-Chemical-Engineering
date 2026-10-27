package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the owner's review scene (2026-10-18): every structure the constructor builds, in rows along x with signs in
 * front (north), boxes 8 blocks apart and rows 20 apart. Row 1 the vessels, row 2 the older units, row 3 the fourteen
 * new units at their middle size, row 4 the kit consolidation's rest and largest cases. Prints the bridge command file.
 */
public class ReviewScene {
    record Pick(String command, String[] sign, EquipmentShape shape) {}

    static final int Y = -60;

    public static void main(String[] a) {
        var rows = new ArrayList<List<Pick>>();
        rows.add(List.of(
                new Pick("place %s 3 6 vertical 3 north", new String[]{"Vertical drum", "3 m x 6 m", "skirt 3 m"},
                        EquipmentShape.drum(3, 6, Orientation.VERTICAL, Heading.NORTH, 3)),
                new Pick("place %s 4 12 horizontal 4 north boot", new String[]{"Horizontal drum", "4 m x 12 m, boot", "saddles 4 m"},
                        EquipmentShape.drum(4, 12, Orientation.HORIZONTAL, Heading.NORTH, 4, EquipmentShape.defaultConnections(4, 12, Orientation.HORIZONTAL, 4, true), true)),
                new Pick("column %s 2 20 3 north", new String[]{"Column", "2 m, 20 trays", "skirt 3 m"},
                        EquipmentShape.column(2, ColumnTrays.simple(20, 10), Heading.NORTH, 3)),
                new Pick("column %s 4 40 4 north", new String[]{"Column", "4 m, 40 trays", "skirt 4 m"},
                        EquipmentShape.column(4, ColumnTrays.simple(40, 20), Heading.NORTH, 4))));
        rows.add(middles(Kind.EXCHANGER, Kind.KETTLE, Kind.AIR_COOLER, Kind.EJECTOR, Kind.PUMP, Kind.COMPRESSOR, Kind.VERTICAL_HEATER,
                Kind.CABIN_HEATER));
        rows.add(middles(Kind.STIRRED_REACTOR, Kind.ADSORBER, Kind.TRICKLE_BED_REACTOR, Kind.EBULLATED_BED_REACTOR, Kind.CENTRIFUGAL_COMPRESSOR,
                Kind.STEAM_TURBINE, Kind.CONDENSING_TURBINE, Kind.CYCLONE));
        rows.add(middles(Kind.FCC_CONVERTER, Kind.DELAYED_COKER, Kind.FLUIDIZED_BED_BOILER, Kind.FLUIDIZED_BED_GASIFIER, Kind.GRATE_BOILER,
                Kind.FIXED_BED_GASIFIER));
        rows.add(List.of(unit(Kind.CYCLONE, 2.0, 1, 8, "rest"), unit(Kind.STIRRED_REACTOR, 4.0, 8, 3, "rest"), unit(Kind.ADSORBER, 4.6, 1, 3, "rest"),
                unit(Kind.CABIN_HEATER, 12, 15, 3, "walkways"), unit(Kind.CABIN_HEATER, 12, 15, 12, "grade rest"),
                unit(Kind.FLUIDIZED_BED_GASIFIER, 3.75, 1, 16, "rests"), unit(Kind.FCC_CONVERTER, 17, 44, 11, "two rests"),
                unit(Kind.FLUIDIZED_BED_BOILER, 26, 3, 8, "largest"), unit(Kind.DELAYED_COKER, 10, 2, 16, "largest")));
        var out = new StringBuilder();
        int z = 0;
        for (int r = 0; r < rows.size(); r++) {
            var row = rows.get(r);
            int depth = 0;
            for (var p : row) depth = Math.max(depth, p.shape.sizeZ());
            if (r > 0) z += depth / 2;
            int x = 0;
            for (int i = 0; i < row.size(); i++) {
                var p = row.get(i);
                if (i > 0) x += row.get(i - 1).shape.sizeX() - row.get(i - 1).shape.sizeX() / 2 + 8 + p.shape.sizeX() / 2;
                int front = z - p.shape.sizeZ() / 2 - 3;
                out.append(String.format(Locale.ROOT, "tp @s %d %d %d%n", x, Y + 25, front - 20));
                out.append("createcheme equipment ").append(String.format(Locale.ROOT, p.command, x + " " + Y + " " + z)).append('\n');
                var lines = new StringBuilder();
                for (int k = 0; k < 4; k++) lines.append(k > 0 ? "," : "").append("'\"").append(k < p.sign.length ? p.sign[k] : "").append("\"'");
                out.append(String.format(Locale.ROOT, "setblock %d %d %d oak_sign[rotation=8]{front_text:{messages:[%s]}}%n", x, Y, front, lines));
            }
            z += depth - depth / 2 + 20;
        }
        out.append(String.format(Locale.ROOT, "tp @s -20 %d -40 facing 60 %d 40%n", Y + 30, Y));
        System.out.print(out);
    }

    static List<Pick> middles(Kind... kinds) {
        var picks = new ArrayList<Pick>();
        for (var kind : kinds) {
            double lo = ProcessUnit.minDiameter(kind), hi = ProcessUnit.maxDiameter(kind), size = ProcessUnit.snap(kind, (lo + hi) / 2);
            int length = ProcessUnit.defaultLength(kind, size);
            picks.add(unit(kind, size, length, ProcessUnit.defaultSupport(kind, size, length), "middle size"));
        }
        return picks;
    }

    static Pick unit(Kind kind, double size, int length, int support, String note) {
        String name = kind.name().toLowerCase(Locale.ROOT);
        String title = Character.toUpperCase(name.charAt(0)) + name.substring(1).replace('_', ' ');
        return new Pick("unit %s " + name + " " + UnitDesign.size(size) + " " + length + " " + support + " north",
                new String[]{title, UnitDesign.size(size) + " x " + length + ", support " + support, note},
                EquipmentShape.unit(kind, size, length, Heading.NORTH, support));
    }
}
