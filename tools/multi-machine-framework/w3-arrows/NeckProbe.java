package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.*;

/**
 * W3 scratch probe: for every shape of the layout table and every nozzle with a flow sign, the neck an arrow band can sit
 * on. Per side (the four arrow sides of MoverPortMarkings.mark), the clear run along the neck that ends where the mover's
 * arrow ends (clear of the flange): points just outside the neck's facets that lie in no solid of the shape.
 * Run: java -cp tmp/w3/classes;build/classes/java/main;build/classes/java/test com.wormzjl.icheme.science.equipment.NeckProbe
 */
public final class NeckProbe {
    public static void main(String[] args) {
        var rows = new ArrayList<String>();
        var worst = new TreeMap<Double, String>();
        int marked = 0, short28 = 0, short15 = 0, short10 = 0;
        var seen = new HashSet<String>();
        for (var shape : EquipmentLayoutGoldenTest.shapes()) {
            var nozzles = shape.nozzles();
            for (int k = 0; k < nozzles.size(); k++) {
                var nozzle = nozzles.get(k);
                var flow = PortMarkings.flow(shape.kind(), nozzle.role());
                if (flow == PortMarkings.Flow.NONE) continue;
                marked++;
                double od = nozzle.defaultOd();
                double end = nozzle.length() - PipeSizes.flangeThickness(od) - .055;
                int sides = PipeSizes.sides(od);
                double r = od / 2 + .01; // outside the circle the solid test uses
                var frame = Mesh.Frame.of(nozzle.base(), nozzle.axis(), nozzle.sign());
                var tangent = frame.side();
                double[] clear = new double[4];
                for (int side = 0; side < 4; side++) {
                    double angle = 2 * Math.PI * (side * sides / 4 + .5) / sides;
                    double c = DeterministicMath.cos(angle), s = DeterministicMath.sin(angle);
                    var normal = new double[3];
                    for (int i = 0; i < 3; i++) normal[i] = c * frame.ref()[i] + s * tangent[i];
                    double run = 0;
                    for (double z = end; z >= 0; z -= .005) {
                        var p = frame.along(z);
                        for (int i = 0; i < 3; i++) p[i] += r * normal[i];
                        var b = shape.transform().point(p);
                        if (ShapeRasterizer.inside(shape, b[0], b[1], b[2])) break;
                        run = end - z;
                    }
                    clear[side] = run;
                }
                double lo = Arrays.stream(clear).min().orElse(0), hi = Arrays.stream(clear).max().orElse(0);
                double mover = Math.min(.65, Math.min(end - .02, Math.max(.28, 1.6 * od)));
                String size = String.format(Locale.ROOT, "%.2f", shape.diameter());
                rows.add(String.format(Locale.ROOT, "%-22s d=%-5s %-20s %-3s len=%.3f od=%.3f end=%.3f clear[min %.3f max %.3f] moverArrow=%.3f",
                        shape.kind(), size, nozzle.role(), flow == PortMarkings.Flow.IN ? "in" : "out", nozzle.length(), od, end, lo, hi, mover));
                if (lo < .28) short28++;
                if (lo < .15) short15++;
                if (lo < .10) short10++;
                String key = shape.kind() + " " + nozzle.role();
                if (seen.add(key + size)) worst.put(lo + 1e-9 * worst.size(), key + " d=" + size + " od=" + String.format(Locale.ROOT, "%.3f", od));
            }
        }
        rows.forEach(System.out::println);
        System.out.println();
        System.out.printf(Locale.ROOT, "marked nozzles %d; clear < 0.28 m: %d, < 0.15 m: %d, < 0.10 m: %d%n", marked, short28, short15, short10);
        System.out.println("shortest 25:");
        worst.entrySet().stream().limit(25).forEach(e -> System.out.printf(Locale.ROOT, "  %.3f  %s%n", e.getKey(), e.getValue()));
    }
}
