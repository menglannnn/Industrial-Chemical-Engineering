package com.wormzjl.icheme.science.equipment;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

// DEBUG-NOZZLE-ROOM (temporary)
class TmpDiagTest {
    static String probe(EquipmentShape shape, EquipmentShape.Nozzle nozzle, double radius, double s) {
        var transform = shape.transform();
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        for (int j = 0; j < 24; j++) {
            double angle = 2 * Math.PI * j / 24;
            var p = nozzle.base().clone();
            p[axis] += nozzle.sign() * s;
            p[u] += radius * Math.cos(angle);
            p[v] += radius * Math.sin(angle);
            var box = transform.point(p);
            if (ShapeRasterizer.inside(shape, box[0], box[1], box[2])) return ShapeRasterizer.what(shape, box[0], box[1], box[2]) + " @" + (j * 15) + "deg";
        }
        return null;
    }

    static double pen(EquipmentShape shape, EquipmentShape.Nozzle nozzle, double radius, double s) {
        for (double r = radius; r > .02; r -= .01) if (probe(shape, nozzle, r, s) == null) return radius - r;
        return radius;
    }

    @Test
    void diagnose() throws Exception {
        var shapes = new ArrayList<>(ProcessUnitTest.everySize());
        shapes.addAll(ShapeRasterizerTest.shapes());
        var out = new StringBuilder();
        double od = PipeSizes.MAX_OUTSIDE, radius = od / 2 + .01;
        for (var shape : shapes)
            for (int k = 0; k < shape.nozzles().size(); k++) {
                if (NozzleRoom.clear(shape, k, od)) continue;
                var n = shape.nozzles().get(k);
                double to = n.length() - PipeSizes.flangeThickness(od) - PipeSizes.hub(od) - .01;
                double near = -1;
                for (double s = 0; s <= to + 1e-9; s += .02) if (probe(shape, n, n.defaultOd() / 2 + .01, s) == null) { near = s; break; }
                near = Math.max(0, near);
                double outS = -1;
                for (double s = near; s <= Math.min(to, near + .5) + 1e-9; s += .02) if (probe(shape, n, radius, s) == null) { outS = s; break; }
                String head = shape.kind() + " " + n.role() + " | d " + shape.diameter() + " L " + shape.tangentLength() + " sup " + shape.support() + " len " + String.format("%.2f", n.length()) + " def " + String.format("%.2f", n.defaultOd());
                double start = (outS < 0 ? near : outS) + NozzleRoom.SURFACE, neck = 0, hub = 0, end = n.length() - PipeSizes.flangeThickness(od);
                String neckPart = "", hubPart = "";
                for (double s = start; s <= end + 1e-9; s += .02) {
                    String p = probe(shape, n, radius, s);
                    if (p == null) continue;
                    double e = pen(shape, n, radius, s);
                    if (s <= to) { if (e > neck) { neck = e; neckPart = p.replaceAll(" @.*", "") + " @" + String.format("%.2f", s); } }
                    else if (e > hub) { hub = e; hubPart = p.replaceAll(" @.*", "") + " @" + String.format("%.2f", s); }
                }
                double last = -1; String lastPart = "";
                for (double s = near; s <= end + 1e-9; s += .02) { String p = probe(shape, n, radius, s); if (p != null) { last = s; lastPart = p.replaceAll(" @.*", ""); } }
                out.append(head).append(String.format(" | last-to %.2f last-near %.2f ", last - to, last - near)).append(lastPart).append(String.format(" | near %.2f out %.2f to %.2f", near, outS, to)).append(String.format(" | neck %.2f ", neck)).append(neckPart).append(String.format(" | hub %.2f ", hub)).append(hubPart).append(System.lineSeparator());
            }
        java.nio.file.Files.writeString(java.nio.file.Path.of("C:/Users/wormz/AppData/Local/Temp/nzl/diag.txt"), out);
    }
}
