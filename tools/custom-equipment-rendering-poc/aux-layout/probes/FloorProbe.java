package com.wormzjl.createcheme.science.equipment;

/** Temporary probe: column A's floors and walkway middles in world coordinates for a column placed at (330, -60, 0). */
public class FloorProbe {
    public static void main(String[] args) {
        var shape = EquipmentShape.column(3, ColumnTrays.simple(20, 10), ColumnOptions.DEFAULT, EquipmentShape.Heading.NORTH, 2);
        var aux = ColumnAuxiliaries.of(shape);
        var t = shape.transform();
        int ox = 330 - shape.sizeX() / 2, oy = -60, oz = -shape.sizeZ() / 2;
        System.out.println("origin " + ox + " " + oy + " " + oz + ", box " + shape.sizeX() + " x " + shape.sizeY() + " x " + shape.sizeZ());
        double in = ColumnStructure.RAIL_BAND + ColumnAuxiliaries.WALKWAY / 2;
        for (var f : aux.floors()) {
            var a = t.point(new double[]{f[0] + in, f[4] + ColumnAuxiliaries.DECK, f[1] + in});
            var b = t.point(new double[]{f[2] - in, f[4] + ColumnAuxiliaries.DECK, f[3] - in});
            System.out.printf("floor %.0f: deck top %.3f, walkway middles x %.2f..%.2f, z %.2f..%.2f%n", f[4], oy + a[1], ox + Math.min(a[0], b[0]), ox + Math.max(a[0], b[0]),
                    oz + Math.min(a[2], b[2]), oz + Math.max(a[2], b[2]));
        }
    }
}
