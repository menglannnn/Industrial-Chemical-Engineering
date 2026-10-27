package com.wormzjl.createcheme.science.equipment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Temporary probe: where a column's auxiliary lines meet another line, a part or the column, across sizes and options. */
class AuxClashProbeTest {
    @Test
    void probe() throws Exception {
        try {
            run();
        } catch (Throwable t) {
            var w = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(w));
            Files.createDirectories(Path.of("build/tmp/aux-probe"));
            Files.writeString(Path.of("build/tmp/aux-probe/clash-error.txt"), w.toString());
        }
    }

    private void run() throws Exception {
        var out = new StringBuilder();
        var crude = new ColumnTrays(25, 13, List.of(8), List.of(26), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(15, 13)));
        var four = new ColumnTrays(40, 20, List.of(10, 25), List.of(41), List.of(new ColumnTrays.Pumparound(5, 3), new ColumnTrays.Pumparound(12, 10),
                new ColumnTrays.Pumparound(22, 20), new ColumnTrays.Pumparound(33, 31)));
        var simple = ColumnTrays.simple(20, 10);
        for (var trays : List.of(simple, crude, four))
            for (var condenser : ColumnOptions.Condenser.values())
                for (boolean ejector : new boolean[]{false, true})
                    for (var reboiler : ColumnOptions.Reboiler.values())
                        for (double d : new double[]{.5, 1, 2, 3, 4.5, 6, 10, 15}) {
                            var options = new ColumnOptions(condenser, ejector, reboiler);
                            int skirt = Math.max(EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL), EquipmentShape.minimumSkirt(d, options));
                            var shape = EquipmentShape.column(d, trays, options, EquipmentShape.Heading.NORTH, skirt);
                            var clashes = clashes(shape);
                            if (!clashes.isEmpty())
                                out.append(String.format("D %.1f trays %d PAs %d %s/%s/%s: %d clashes, e.g. %s%n", d, trays.trays(), trays.pumparounds().size(), condenser,
                                        ejector ? "ejector" : "-", reboiler, clashes.size(), clashes.subList(0, Math.min(4, clashes.size()))));
                        }
        Files.createDirectories(Path.of("build/tmp/aux-probe"));
        Files.writeString(Path.of("build/tmp/aux-probe/clashes.txt"), out.isEmpty() ? "none\n" : out);
    }

    static List<String> clashes(EquipmentShape shape) {
        var aux = ColumnAuxiliaries.of(shape);
        var column = new ColumnStructure(shape);
        var transform = shape.transform();
        var runs = aux.runs();
        var ends = new HashMap<Integer, List<double[]>>();
        for (var r : runs) {
            int line = (int) r[7];
            var list = ends.computeIfAbsent(line, k -> new ArrayList<>());
            if (list.isEmpty()) list.add(new double[]{r[0], r[1], r[2]});
            if (list.size() > 1) list.remove(1);
            list.add(new double[]{r[3], r[4], r[5]});
        }
        var found = new ArrayList<String>();
        var seen = new java.util.HashSet<String>();
        for (var r : runs) {
            int line = (int) r[7];
            double radius = r[6], length = Math.hypot(Math.hypot(r[3] - r[0], r[4] - r[1]), r[5] - r[2]);
            int steps = (int) Math.ceil(length / .1);
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double[] p = {r[0] + (r[3] - r[0]) * t, r[1] + (r[4] - r[1]) * t, r[2] + (r[5] - r[2]) * t};
                boolean nearEnd = false;
                for (var e : ends.get(line)) if (distance(p, e) < 2 * radius + .4) nearEnd = true;
                if (nearEnd) continue;
                for (var o : runs) {
                    if ((int) o[7] == line) continue;
                    double gap = segment(p, o) - radius - o[6];
                    boolean otherEnd = false;
                    for (var e : ends.get((int) o[7])) if (distance(p, e) < 2 * o[6] + 2 * radius + .4) otherEnd = true;
                    if (gap < .02 && !otherEnd && seen.add("line " + line + "/" + (int) o[7])) found.add("line " + line + " meets line " + (int) o[7] + " at " + fmt(p));
                }
                int axis = Math.abs(r[3] - r[0]) > 1e-9 ? 0 : Math.abs(r[4] - r[1]) > 1e-9 ? 1 : 2;
                for (int k = 0; k < 5; k++) {
                    var q = p.clone();
                    if (k > 0) {
                        int across = (axis + 1 + (k - 1) / 2) % 3;
                        q[across] += (k % 2 == 0 ? -1 : 1) * radius * .9;
                    }
                    var box = transform.point(q);
                    String what = aux.insidePart(q) ? "part" : column.insideDrawn(q) ? "column structure" : ShapeRasterizer.insideVessel(shape, box[0], box[1], box[2]) ? "vessel" : null;
                    if (what != null && seen.add("line " + line + " " + what)) found.add("line " + line + " in " + what + " at " + fmt(q));
                }
            }
        }
        return found;
    }

    private static double segment(double[] p, double[] s) {
        double[] a = {s[0], s[1], s[2]}, b = {s[3], s[4], s[5]};
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double len2 = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((p[0] - a[0]) * ab[0] + (p[1] - a[1]) * ab[1] + (p[2] - a[2]) * ab[2]) / len2));
        return distance(p, new double[]{a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t});
    }

    private static double distance(double[] p, double[] q) {
        return Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2]));
    }

    private static String fmt(double[] p) {
        return String.format("(%.2f, %.2f, %.2f)", p[0], p[1], p[2]);
    }
}
