package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/** FCC proportions probe (2026-10-18): per size, the fitting ranges, the default form's elevations, box and quads. */
public class FccProbe {
    public static void main(String[] a) {
        var design = (FccConverterDesign) UnitDesign.of(Kind.FCC_CONVERTER);
        System.out.println("D minL defL minS defS fitPairs | regenTop reactorTop stripperBottom reactorH/OD | box quads");
        int totalPairs = 0;
        for (double d = 4; d <= 17.001; d += .5) {
            int minL = design.minLength(d), defL = design.defaultLength(d), minS = design.minSupport(d, defL), defS = ProcessUnit.defaultSupport(Kind.FCC_CONVERTER, d, defL);
            int pairs = 0;
            for (int l = FccConverterLayout.MIN_RISER; l <= FccConverterLayout.MAX_RISER; l++)
                for (int s = FccConverterLayout.MIN_BOTTOM; s <= FccConverterLayout.MAX_BOTTOM; s++)
                    if (new FccConverterLayout(d, l, s).fits) pairs++;
            totalPairs += pairs;
            var lay = new FccConverterLayout(d, defL, defS);
            String box = "-", quads = "-";
            if (a.length > 0 && (Math.abs(d - 4) < 1e-9 || Math.abs(d - 10.5) < 1e-9 || Math.abs(d - 17) < 1e-9 || a[0].equals("all"))) {
                var shape = EquipmentShape.unit(Kind.FCC_CONVERTER, d, defL, Heading.NORTH, defS);
                box = shape.sizeX() + "x" + shape.sizeY() + "x" + shape.sizeZ();
                long q = 0;
                for (var cell : ShapeRasterizer.layout(shape).shell()) q += ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z())).size();
                quads = Long.toString(q);
            }
            System.out.println(String.format(Locale.ROOT, "%.1f %d %d %d %d %d | %.1f %.1f %.1f %.2f | %s %s", d, minL, defL, minS, defS, pairs,
                    lay.support + lay.straight + lay.head, lay.reactorTop + lay.rr / 2, lay.stripperBottom, lay.reactorShell / (2 * lay.rr), box, quads));
        }
        System.out.println("fitting (length, support) pairs over all sizes: " + totalPairs);
    }
}
