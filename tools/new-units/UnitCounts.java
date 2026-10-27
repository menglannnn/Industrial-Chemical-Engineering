package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/**
 * One kind's units at its smallest, middle and largest size (least and most second size, lowest support): box, shell
 * and interior cells, chunk quads (a polygon of n corners bakes (n - 1) / 2), moving parts, nozzles and layout time. Args: kind names.
 */
public class UnitCounts {
    public static void main(String[] args) {
        System.out.println("| unit | box | shell | interior | quads | moving | nozzles | layout ms |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        for (String name : args) {
            var kind = Kind.valueOf(name);
            double lo = ProcessUnit.minDiameter(kind), hi = ProcessUnit.maxDiameter(kind);
            for (double d : new double[]{lo, ProcessUnit.snap(kind, (lo + hi) / 2), hi}) {
                int least = ProcessUnit.minLength(kind, d), most = ProcessUnit.maxLength(kind, d);
                for (int length : most > least ? new int[]{least, most} : new int[]{least}) {
                    var shape = EquipmentShape.unit(kind, d, length, Heading.NORTH, ProcessUnit.minSupport(kind, d, length));
                    long start = System.nanoTime();
                    var layout = ShapeRasterizer.layout(shape);
                    long ms = (System.nanoTime() - start) / 1_000_000;
                    int quads = 0;
                    for (var cell : layout.shell())
                        for (var p : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) quads += (p.size() - 1) / 2;
                    System.out.printf("| %s %.2f x %d | %dx%dx%d | %d | %d | %d | %d | %d | %d |%n", kind, d, length, shape.sizeX(), shape.sizeY(), shape.sizeZ(),
                            layout.shell().size(), layout.interior().size(), quads, ShapeRasterizer.moving(shape).size(), shape.nozzles().size(), ms);
                }
            }
        }
    }
}
