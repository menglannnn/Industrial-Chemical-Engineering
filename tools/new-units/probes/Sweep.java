package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

import java.util.HashSet;

/**
 * Integration probe: every size, every length and every support of the named kinds through the walk and climb gate, the
 * nozzle spacing gate and the free pipe block check, with each shape's chunk quads (a polygon of n corners bakes
 * (n - 1) / 2); prints failures, each kind's shape count and its largest quad count with the shape. Args: kind names.
 */
public class Sweep {
    public static void main(String[] args) {
        int bad = 0, total = 0;
        for (String name : args) {
            var kind = Kind.valueOf(name);
            int n = 0, most = -1;
            String worst = "";
            double step = ProcessUnit.diameterStep(kind);
            for (double d = ProcessUnit.minDiameter(kind); d <= ProcessUnit.maxDiameter(kind) + 1e-9; d += step) {
                double size = ProcessUnit.snap(kind, d);
                for (int length = ProcessUnit.minLength(kind, size); length <= ProcessUnit.maxLength(kind, size); length++)
                    for (int h = ProcessUnit.minSupport(kind, size, length); h <= ProcessUnit.maxSupport(kind); h++) {
                        EquipmentShape shape;
                        try {
                            shape = EquipmentShape.unit(kind, size, length, Heading.NORTH, h);
                        } catch (IllegalArgumentException e) {
                            continue; // not a shape the kind offers (a balanced-opposed compressor's odd throw count)
                        }
                        n++;
                        try {
                            ProcessUnitTest.walkedAndClimbed(shape);
                            ProcessUnitTest.pipesApart(shape);
                            var layout = ShapeRasterizer.layout(shape);
                            var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());
                            occupied.addAll(layout.interior());
                            for (var port : ShapeRasterizer.ports(shape)) {
                                var pipe = new ShapeRasterizer.Cell(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
                                if (occupied.contains(pipe)) throw new AssertionError(shape.nozzles().get(port.nozzle()).role() + " pipe block taken at " + pipe);
                            }
                            int quads = 0;
                            for (var cell : layout.shell())
                                for (var p : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) quads += (p.size() - 1) / 2;
                            if (quads > most) {
                                most = quads;
                                worst = String.format("%.2f x %d s%d %dx%dx%d", size, length, h, shape.sizeX(), shape.sizeY(), shape.sizeZ());
                            }
                            if (quads >= 80_000) throw new AssertionError(quads + " quads");
                        } catch (Throwable t) {
                            bad++;
                            System.out.println("FAIL " + kind + " " + size + " x " + length + " s" + h + ": " + t.getMessage());
                        }
                    }
            }
            total += n;
            System.out.println("KIND " + kind + ": " + n + " shapes, most quads " + most + " at " + worst);
        }
        System.out.println(total + " shapes, " + bad + " failed");
    }
}
