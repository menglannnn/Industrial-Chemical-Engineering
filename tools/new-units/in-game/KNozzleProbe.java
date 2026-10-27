package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.Locale;

/** Kit in-game check: args "x z kind size length support"; prints each nozzle's base in world coordinates (heading north, y 0 = -60). */
public class KNozzleProbe {
    public static void main(String[] a) {
        int x = Integer.parseInt(a[0]), z = Integer.parseInt(a[1]);
        var shape = EquipmentShape.unit(Kind.valueOf(a[2].toUpperCase(Locale.ROOT)), Double.parseDouble(a[3]), Integer.parseInt(a[4]), Heading.NORTH, Integer.parseInt(a[5]));
        var f = shape.frameBox();
        double ox = x - shape.sizeX() / 2 - f[0], oz = z - shape.sizeZ() / 2 - f[2];
        for (var n : shape.nozzles())
            System.out.printf(Locale.ROOT, "%-16s base %.2f %.2f %.2f axis %d sign %d od %.2f%n", n.role(), ox + n.base()[0], -60 + n.base()[1], oz + n.base()[2], n.axis(), n.sign(), n.defaultOd());
    }
}
