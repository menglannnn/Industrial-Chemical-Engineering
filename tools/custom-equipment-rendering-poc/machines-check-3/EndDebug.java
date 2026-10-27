package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/** The gate's closed-end region of every deck of one unit, with the filled voxel count and what lies around it. Args: kind size length support heading. */
public final class EndDebug {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Heading.valueOf(args[4]), Integer.parseInt(args[3]));
        int n = 0;
        for (var d : ProcessUnit.of(shape).decks()) {
            for (int e = 0; e < 2; e++) {
                double len = d.run().length(), from = e == 0 ? 0 : len - Access.RAIL_BAND, off = (Access.GAP + d.width() - Access.RAIL_BAND) / 2, y = d.level() + Access.DECK;
                var a = d.run().at(from, off - .3, y + .3);
                var b = d.run().at(from + Access.RAIL_BAND, off + .3, y + 1.2);
                var p = shape.transform().point(a);
                var q = shape.transform().point(b);
                int met = 0, total = 0;
                for (int i = (int) Math.ceil(Math.min(p[0], q[0]) * 8 - .5); (i + .5) / 8 <= Math.max(p[0], q[0]); i++)
                    for (int j = (int) Math.ceil(Math.min(p[1], q[1]) * 8 - .5); (j + .5) / 8 <= Math.max(p[1], q[1]); j++)
                        for (int k = (int) Math.ceil(Math.min(p[2], q[2]) * 8 - .5); (k + .5) / 8 <= Math.max(p[2], q[2]); k++) {
                            total++;
                            if (ShapeRasterizer.inside(shape, (i + .5) / 8, (j + .5) / 8, (k + .5) / 8)) met++;
                        }
                var mid = d.run().at(from + Access.RAIL_BAND / 2, off, y + .7);
                var unit = ProcessUnit.of(shape);
                System.out.printf("deck %d %s level %.2f len %.2f width %.2f closed %b/%b end %d: %d/%d; frame a (%.2f %.2f %.2f) b (%.2f %.2f %.2f); mid inside %b%n", n,
                        d.run().getClass().getSimpleName(), d.level(), len, d.width(), d.closedStart(), d.closedEnd(), e, met, total, a[0], a[1], a[2], b[0], b[1], b[2], unit.inside(mid));
            }
            n++;
        }
    }
}
