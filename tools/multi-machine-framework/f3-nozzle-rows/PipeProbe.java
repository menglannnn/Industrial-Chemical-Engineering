package com.wormzjl.icheme.science.equipment;

import java.util.List;

/** What stands in the 0.5 m test column's reflux pipe cell: the nozzle, the plan, the cells around it. */
public final class PipeProbe {
    public static void main(String[] args) {
        var t = new ColumnTrays(30, 15, List.of(8, 20), List.of(31), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(12, 10),
                new ColumnTrays.Pumparound(18, 16), new ColumnTrays.Pumparound(26, 24)));
        var s = EquipmentShape.column(.5, t, EquipmentShape.Heading.NORTH, 2);
        System.out.println("misfit " + ColumnDesign.misfit(s).orElse("-") + " axis " + s.axis() + " outer " + s.outerRadius());
        for (var n : ColumnDesign.sides(s)) System.out.println("  " + n.role() + " " + n.index() + " side " + n.side() + (n.placed() ? " y " + n.nozzle().base()[1] + " face " + java.util.Arrays.toString(n.nozzle().face()) : " UNPLACED"));
        var plan = ColumnStructure.plan(s);
        for (var p : plan.platforms()) System.out.printf("  platform %d %.0f-%.0f ring %s manway %s closed %s %s%n", p.level(), Math.toDegrees(p.from()), Math.toDegrees(p.to()), p.ring(), p.manway(), p.closedFrom(), p.closedTo());
        for (var l : plan.ladders()) System.out.printf("  ladder %.0f %.2f-%.2f%n", Math.toDegrees(l.angle()), l.base(), l.top());
        System.out.printf("  davit %.0f top %.2f deck radius %.2f%n", Math.toDegrees(plan.davitAngle()), plan.davitTop(), plan.deckRadius());
        var layout = ShapeRasterizer.layout(s);
        var shell = new java.util.HashSet<>(layout.shell());
        for (var port : ShapeRasterizer.ports(s)) {
            var nozzle = s.nozzles().get(port.nozzle());
            var front = new ShapeRasterizer.Cell(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
            if (shell.contains(front)) {
                System.out.println("  BLOCKED " + nozzle.role() + " port " + port.cell() + " pipe " + front + " key solids " + ShapeRasterizer.solid(ShapeRasterizer.key(s, front.x(), front.y(), front.z())).size());
                for (var b : ShapeRasterizer.solid(ShapeRasterizer.key(s, front.x(), front.y(), front.z()))) System.out.println("     box " + b);
                int hits = 0;
                for (int i = 0; i < 20 && hits < 12; i++) for (int j = 0; j < 20 && hits < 12; j++) for (int k = 0; k < 20 && hits < 12; k++) {
                    double x = front.x() + (i + .5) / 20, y = front.y() + (j + .5) / 20, z = front.z() + (k + .5) / 20;
                    if (ShapeRasterizer.inside(s, x, y, z)) { hits++; var q = new double[]{x, y, z}; double dx = q[0] - s.axis(), dz = q[2] - s.axis(); System.out.printf("     inside world %.2f %.2f %.2f frame r %.2f deg %.1f y %.2f%n", x, y, z, Math.hypot(dx, dz), Math.toDegrees(Math.atan2(dz, dx)), q[1]); }
                }
            }
        }
    }
}
