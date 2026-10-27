package com.wormzjl.icheme.science.equipment;

import java.lang.reflect.*;
import java.util.*;

/** W3 scratch: the cyclone feed at 0.59 m, which vertex is hidden and by what. */
public final class CycloneProbe {
    public static void main(String[] args) throws Exception {
        var shape = EquipmentLayoutGoldenTest.shapes().stream().filter(s -> s.kind() == EquipmentShape.Kind.CYCLONE && s.diameter() == 1.1).findFirst().orElseThrow();
        Method geo = ShapeRasterizer.class.getDeclaredMethod("geometry", EquipmentShape.class); geo.setAccessible(true);
        Object g = geo.invoke(null, shape);
        Method solid = g.getClass().getDeclaredMethod("solid", double[].class); solid.setAccessible(true);
        Method inNozzle = g.getClass().getDeclaredMethod("inNozzle", double[].class, int.class, int.class); inNozzle.setAccessible(true);
        var nozzles = shape.nozzles();
        int code = 38;
        for (int k = 0; k < nozzles.size(); k++) {
            var n = nozzles.get(k);
            if (n.role() != EquipmentShape.Role.FEED) continue;
            System.out.println("feed base " + Arrays.toString(n.base()) + " axis " + n.axis() + " sign " + n.sign() + " len " + n.length() + " defaultOd " + n.defaultOd());
            var mesh = new Mesh();
            PortMarkings.band(mesh, shape, k, code, PortMarkings.Flow.IN);
            double round = PipeSizes.outsideOf(code) / 2 + .01;
            for (var f : mesh.facets()) for (var p : f.positions()) {
                double off = 0; int a0 = n.axis();
                for (int a = 0; a < 3; a++) if (a != a0) off += (p[a] - n.base()[a]) * (p[a] - n.base()[a]);
                var q = p.clone();
                for (int a = 0; a < 3; a++) if (a != a0) q[a] = n.base()[a] + (p[a] - n.base()[a]) * round / Math.sqrt(off);
                if (!ShapeRasterizer.hides(shape, k, code, q)) continue;
                var hits = new ArrayList<String>();
                if ((boolean) solid.invoke(g, (Object) q)) hits.add("solid");
                for (int j = 0; j < nozzles.size(); j++) if ((boolean) inNozzle.invoke(g, q, j, j == k ? code : 0)) hits.add(nozzles.get(j).role() + "#" + j);
                double s = (p[a0] - n.base()[a0]) * n.sign();
                System.out.printf(Locale.ROOT, "hidden vertex s=%.4f q=%s by %s%n", s, Arrays.toString(q), hits);
            }
        }
    }
}
