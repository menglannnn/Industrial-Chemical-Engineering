package com.wormzjl.createcheme.science.equipment;

import java.util.List;

/** Temporary probe: structure floors (length, width, levels) and facet counts for the review's numbers. */
public class ExtentProbe {
    public static void main(String[] args) {
        var crude = new ColumnTrays(25, 13, List.of(8), List.of(26), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(15, 13)));
        for (double d : new double[]{1, 3, 6})
            for (var o : List.of(ColumnOptions.DEFAULT, new ColumnOptions(ColumnOptions.Condenser.AIR, true, ColumnOptions.Reboiler.KETTLE))) {
                int skirt = Math.max(EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL), EquipmentShape.minimumSkirt(d, o));
                var shape = EquipmentShape.column(d, crude, o, EquipmentShape.Heading.NORTH, skirt);
                var aux = ColumnAuxiliaries.of(shape);
                var sb = new StringBuilder(String.format("D %.0f %s/%s/%s box %d x %d x %d, aux facets %d, floors:", d, o.condenser(), o.ejector() ? "ejector" : "-", o.reboiler(),
                        shape.sizeX(), shape.sizeY(), shape.sizeZ(), aux.mesh().facets().size()));
                for (var f : aux.floors()) sb.append(String.format(" %.2f x %.2f at %.0f m;", f[2] - f[0], f[3] - f[1], f[4]));
                System.out.println(sb);
            }
    }
}
