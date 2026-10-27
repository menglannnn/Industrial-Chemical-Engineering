package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/** Each nozzle's connection cell and face for one unit, with the unit's box. Args: kind size length. */
public class PortCells {
    public static void main(String[] args) {
        var kind = Kind.valueOf(args[0]);
        double size = Double.parseDouble(args[1]);
        int length = Integer.parseInt(args[2]);
        var shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, ProcessUnit.defaultSupport(kind, size, length));
        var ports = ShapeRasterizer.ports(shape);
        var nozzles = shape.nozzles();
        var box = shape.frameBox();
        System.out.printf("%s %.2f x %d box x %.1f..%.1f y %.1f..%.1f z %.1f..%.1f%n", kind, size, length, box[0], box[3], box[1], box[4], box[2], box[5]);
        for (int i = 0; i < ports.size(); i++) {
            var p = ports.get(i);
            var n = nozzles.get(i);
            var f = n.face();
            System.out.printf("  %-18s cell [%d,%d,%d] dir (%d,%d,%d) face frame (%.2f, %.2f, %.2f) maxOd %.2f%n", n.role(), p.cell().x() + p.dx(), p.cell().y() + p.dy(), p.cell().z() + p.dz(),
                    p.dx(), p.dy(), p.dz(), f[0], f[1], f[2], n.maxOd());
        }
    }
}
