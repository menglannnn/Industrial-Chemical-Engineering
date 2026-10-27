package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/** Kit in-game check: args are groups "x z kind size length support" (a scene line); prints each unit's box in world blocks. */
public class KBoxProbe {
    public static void main(String[] a) {
        for (int i = 0; i + 5 < a.length; i += 6) {
            int x = Integer.parseInt(a[i]), z = Integer.parseInt(a[i + 1]);
            var kind = Kind.valueOf(a[i + 2].toUpperCase(Locale.ROOT));
            var shape = EquipmentShape.unit(kind, Double.parseDouble(a[i + 3]), Integer.parseInt(a[i + 4]), Heading.NORTH, Integer.parseInt(a[i + 5]));
            int ox = x - shape.sizeX() / 2, oz = z - shape.sizeZ() / 2;
            System.out.printf(Locale.ROOT, "%-24s %5s x %3s s%-2s box %2d x %2d x %2d  x %d..%d  y -60..%d  z %d..%d%n", a[i + 2], a[i + 3], a[i + 4], a[i + 5],
                    shape.sizeX(), shape.sizeY(), shape.sizeZ(), ox, ox + shape.sizeX() - 1, -60 + shape.sizeY() - 1, oz, oz + shape.sizeZ() - 1);
        }
    }
}
