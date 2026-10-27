package com.wormzjl.icheme.science.equipment;

import java.util.*;

/** W3 scratch: the arrows at other drawn sizes (1/16 to 0.8 m): every vertex on neck no solid hides; how many sides drop out. */
public final class SizeProbe {
    public static void main(String[] args) {
        int checked = 0, hidden = 0, empty = 0;
        var notes = new TreeSet<String>();
        for (var shape : EquipmentLayoutGoldenTest.shapes()) {
            var nozzles = shape.nozzles();
            for (int k = 0; k < nozzles.size(); k++) {
                var n = nozzles.get(k);
                var flow = PortMarkings.flow(shape.kind(), n.role());
                if (flow == PortMarkings.Flow.NONE) continue;
                for (int code : new int[]{4, 13, 26, 38, 51}) {
                    checked++;
                    var mesh = new Mesh();
                    PortMarkings.band(mesh, shape, k, code, flow);
                    if (mesh.facets().isEmpty()) { empty++; notes.add(shape.kind() + " " + n.role() + " code " + code); continue; }
                    double round = PipeSizes.outsideOf(code) / 2 + .01;
                    boolean bad = false;
                    for (var f : mesh.facets()) for (var p : f.positions()) {
                        double off = 0; int a0 = n.axis();
                        for (int a = 0; a < 3; a++) if (a != a0) off += (p[a] - n.base()[a]) * (p[a] - n.base()[a]);
                        var q = p.clone();
                        for (int a = 0; a < 3; a++) if (a != a0) q[a] = n.base()[a] + (p[a] - n.base()[a]) * round / Math.sqrt(off);
                        if (ShapeRasterizer.hides(shape, k, code, q)) bad = true;
                    }
                    if (bad) { hidden++; notes.add("HIDDEN " + shape.kind() + " d=" + shape.diameter() + " " + n.role() + " code " + code); }
                }
            }
        }
        System.out.printf(Locale.ROOT, "checked %d, hidden vertices %d, no band %d%n", checked, hidden, empty);
        notes.stream().filter(s -> s.startsWith("HIDDEN")).limit(20).forEach(System.out::println);
    }
}
