package com.wormzjl.icheme.science.equipment;

import java.lang.reflect.*;
import java.util.*;

/** W3 scratch: on the 0.5 m column, which solid (vessel or which nozzle) the reflux neck's clear run meets. */
public final class WhyShort2 {
    public static void main(String[] args) throws Exception {
        var shape = EquipmentLayoutGoldenTest.shapes().stream().filter(s -> s.kind() == EquipmentShape.Kind.COLUMN).findFirst().orElseThrow();
        Method geo = ShapeRasterizer.class.getDeclaredMethod("geometry", EquipmentShape.class); geo.setAccessible(true);
        Object g = geo.invoke(null, shape);
        Method inVessel = g.getClass().getDeclaredMethod("inVessel", double[].class); inVessel.setAccessible(true);
        Method inNozzle = g.getClass().getDeclaredMethod("inNozzle", double[].class, int.class, int.class); inNozzle.setAccessible(true);
        var nozzles = shape.nozzles();
        for (int k = 0; k < nozzles.size(); k++) {
            var n = nozzles.get(k);
            if (n.role() != EquipmentShape.Role.REFLUX && n.role() != EquipmentShape.Role.VAPOUR_OUTLET) continue;
            double od = n.defaultOd(), r = od / 2 + .01;
            var frame = Mesh.Frame.of(n.base(), n.axis(), n.sign());
            for (double z = .45; z >= .2; z -= .01) {
                var p = frame.along(z); p[(n.axis() + 1) % 3] += r;
                var hits = new ArrayList<String>();
                if ((boolean) inVessel.invoke(g, (Object) p)) hits.add("vessel");
                for (int j = 0; j < nozzles.size(); j++) if ((boolean) inNozzle.invoke(g, p, j, 0)) hits.add(nozzles.get(j).role() + "#" + j + "(od " + nozzles.get(j).defaultOd() + ")");
                System.out.printf(Locale.ROOT, "%s z=%.2f %s%n", n.role(), z, hits);
            }
        }
    }
}
