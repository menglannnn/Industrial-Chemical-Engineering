package com.wormzjl.icheme.science.equipment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Scratch probe (batch 2026-10-26-multi-machine-framework, final lane): over NewSweep's 315 F3 designs, every column neck
 * that is not clear at a pipe's largest (RoomSweep's rule), counted by diameter, role and what its first blocked ring
 * beyond the surface allowance meets: the vessel, the access structure (decks, rails, ladders, davit), a manway, another
 * nozzle, or "never leaves" (the ring never clears the shell within half a pipe's largest).
 */
public final class NeckWhy {
    public static void main(String[] args) throws Exception {
        Method geometry = ShapeRasterizer.class.getDeclaredMethod("geometry", EquipmentShape.class);
        geometry.setAccessible(true);
        var counts = new TreeMap<String, Integer>();
        for (double d : new double[]{.5, 1, 1.5, 2, 3, 4, 6, 8, 15})
            for (int n : new int[]{1, 3, 5, 10, 15, 20, 30, 40, 64})
                for (String config : new String[]{"kettle", "kettle+steam+draw+PA", "steam+draw", "full"}) {
                    ColumnTrays t;
                    try {
                        t = NewSweep.trays(n, config);
                    } catch (IllegalArgumentException invalid) {
                        continue;
                    }
                    var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d));
                    if (ColumnDesign.misfit(s).isPresent()) continue;
                    Object g = geometry.invoke(null, s);
                    for (int k = 0; k < s.nozzles().size(); k++) {
                        var role = s.nozzles().get(k).role();
                        if (d == .5 && role != EquipmentShape.Role.VAPOUR_OUTLET && role != EquipmentShape.Role.BOTTOMS || d == 3. && role == EquipmentShape.Role.VAPOUR_OUTLET) continue;
                        if (ProcessUnitTest.NozzleRoom.clear(s, k, PipeSizes.MAX_OUTSIDE)) continue;
                        counts.merge(String.format("d %4.1f %-16s %s", d, role, why(g, s, k)), 1, Integer::sum);
                    }
                }
        int total = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.println(String.format("%4d  %s", e.getValue(), e.getKey()));
            total += e.getValue();
        }
        System.out.println("TOTAL " + total);
    }

    /** What the first ring that NozzleRoom.clear finds blocked meets. */
    static String why(Object g, EquipmentShape s, int k) throws Exception {
        var nozzle = s.nozzles().get(k);
        double od = PipeSizes.MAX_OUTSIDE, to = nozzle.length() - PipeSizes.flangeThickness(od) - PipeSizes.hub(od) - .01, radius = od / 2 + .01;
        double near = Math.max(0, leaves(s, nozzle, nozzle.defaultOd() / 2 + .01, 0, to));
        double out = leaves(s, nozzle, radius, near, Math.min(to, near + PipeSizes.MAX_OUTSIDE / 2)), from = out + ProcessUnitTest.NozzleRoom.SURFACE;
        if (out < 0) return "never leaves " + parts(g, s, nozzle, k, radius, near + PipeSizes.MAX_OUTSIDE / 2);
        int rings = Math.max(1, (int) Math.ceil((to - from) / .05));
        for (int i = 0; i <= rings && to > from; i++) {
            double at = from + (to - from) * i / rings;
            var p = parts(g, s, nozzle, k, radius, at);
            if (!p.isEmpty()) return p.toString();
        }
        return "?";
    }

    private static double leaves(EquipmentShape s, EquipmentShape.Nozzle nozzle, double radius, double from, double to) {
        for (double x = from; x <= to + 1e-9; x += .02) if (ring(s, nozzle, radius, x)) return x;
        return -1;
    }

    private static boolean ring(EquipmentShape s, EquipmentShape.Nozzle nozzle, double radius, double at) {
        var transform = s.transform();
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        for (int j = 0; j < 24; j++) {
            double angle = 2 * Math.PI * j / 24;
            var p = nozzle.base().clone();
            p[axis] += nozzle.sign() * at;
            p[u] += radius * Math.cos(angle);
            p[v] += radius * Math.sin(angle);
            var box = transform.point(p);
            if (ShapeRasterizer.inside(s, box[0], box[1], box[2])) return false;
        }
        return true;
    }

    private static TreeSet<String> parts(Object g, EquipmentShape s, EquipmentShape.Nozzle nozzle, int k, double radius, double at) throws Exception {
        Method inVessel = method(g, "inVessel", double[].class), inNozzle = method(g, "inNozzle", double[].class, int.class, int.class);
        Object column = field(g, "column"), manway = field(g, "manway");
        Method columnInside = column == null ? null : method(column, "inside", double[].class);
        Method manwayInside = manway == null ? null : method(manway, "inside", double[].class);
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        var out = new TreeSet<String>();
        for (int j = 0; j < 24; j++) {
            double angle = 2 * Math.PI * j / 24;
            var p = nozzle.base().clone();
            p[axis] += nozzle.sign() * at;
            p[u] += radius * Math.cos(angle);
            p[v] += radius * Math.sin(angle);
            if ((boolean) inVessel.invoke(g, (Object) p)) out.add("vessel");
            if (columnInside != null && (boolean) columnInside.invoke(column, (Object) p)) out.add("access");
            if (manwayInside != null && (boolean) manwayInside.invoke(manway, (Object) p)) out.add("manway");
            for (int n = 0; n < s.nozzles().size(); n++) if (n != k && (boolean) inNozzle.invoke(g, p, n, 0)) out.add("nozzle " + s.nozzles().get(n).role());
        }
        return out;
    }

    private static Method method(Object o, String name, Class<?>... types) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
            try {
                var m = c.getDeclaredMethod(name, types);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
        throw new NoSuchMethodException(o.getClass() + "." + name);
    }

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
