package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/** Most datasheet lines per unit kind over its sizes and second sizes (kind, design lines, length, top, support, cells, controller, nozzles). */
public class LineCount {
    public static void main(String[] a) {
        for (var kind : Kind.values()) {
            if (!kind.unit()) continue;
            var d = UnitDesign.of(kind);
            int most = 0; String at = "";
            for (double s = d.minSize(); s <= d.maxSize() + 1e-9; s += d.sizeStep())
                for (int l = d.minLength(s); l <= d.maxLength(s); l += d.lengthStep()) {
                    EquipmentShape shape;
                    try { shape = EquipmentShape.unit(kind, s, l, Heading.NORTH, d.defaultSupport(s, l)); } catch (IllegalArgumentException e) { continue; }
                    int n = 1 + d.datasheet(shape).size() + 3 + 2 + shape.nozzles().size();
                    if (n > most) { most = n; at = UnitDesign.size(s) + " x " + l; }
                }
            System.out.println(kind + " " + most + " at " + at);
        }
    }
}
