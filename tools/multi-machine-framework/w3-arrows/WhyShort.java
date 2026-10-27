package com.wormzjl.icheme.science.equipment;

import java.util.*;

/** W3 scratch: what stops the clear run on the shortest necks (vessel or nozzle versus a column's access structure). */
public final class WhyShort {
    public static void main(String[] args) {
        for (var shape : EquipmentLayoutGoldenTest.shapes()) {
            if (shape.kind() != EquipmentShape.Kind.COLUMN) continue;
            var nozzles = shape.nozzles();
            for (int k = 0; k < nozzles.size(); k++) {
                var n = nozzles.get(k);
                if (PortMarkings.flow(shape.kind(), n.role()) == PortMarkings.Flow.NONE) continue;
                double od = n.defaultOd(), end = n.length() - PipeSizes.flangeThickness(od) - .055, r = od / 2 + .01;
                var frame = Mesh.Frame.of(n.base(), n.axis(), n.sign());
                var b = new StringBuilder(String.format(Locale.ROOT, "d=%.2f %-14s base=%s axis=%d sign=%d len=%.3f:", shape.diameter(), n.role(), Arrays.toString(n.base()), n.axis(), n.sign(), n.length()));
                for (double z = end; z >= 0; z -= .005) {
                    var p = frame.along(z);
                    var q = p.clone(); q[(n.axis() + 1) % 3] += r;
                    var w = shape.transform().point(q);
                    if (ShapeRasterizer.inside(shape, w[0], w[1], w[2])) {
                        b.append(String.format(Locale.ROOT, " stop z=%.3f vessel/nozzle=%s", z, ShapeRasterizer.insideVessel(shape, w[0], w[1], w[2])));
                        break;
                    }
                }
                System.out.println(b);
            }
        }
    }
}
