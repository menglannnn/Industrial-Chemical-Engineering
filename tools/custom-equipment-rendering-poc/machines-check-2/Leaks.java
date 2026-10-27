package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import java.util.HashSet;

/** Interior cells next to an outside cell, with the frame point of each, for one unit. Args: kind size length support. */
public final class Leaks {
    public static void main(String[] args) {
        var shape = EquipmentShape.unit(Kind.valueOf(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Heading.NORTH, Integer.parseInt(args[3]));
        var layout = ShapeRasterizer.layout(shape);
        var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());
        occupied.addAll(layout.interior());
        System.out.println("box " + shape.sizeX() + "x" + shape.sizeY() + "x" + shape.sizeZ());
        for (var cell : layout.interior())
            for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                var n = new ShapeRasterizer.Cell(cell.x() + d[0], cell.y() + d[1], cell.z() + d[2]);
                if (!occupied.contains(n)) {
                    var p = shape.transform().local(new double[]{cell.x() + .5, cell.y() + .5, cell.z() + .5});
                    var q = shape.transform().local(new double[]{n.x() + .5, n.y() + .5, n.z() + .5});
                    System.out.printf("%s -> %s frame (%.2f %.2f %.2f) -> (%.2f %.2f %.2f)%n", cell, n, p[0], p[1], p[2], q[0], q[1], q[2]);
                }
            }
    }
}
