package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Scratch probe (batch 2026-10-26-multi-machine-framework, final lane): which part the neck of a column nozzle that fails
 * ProcessUnitTest.everyNozzleNeckAtItsLargestStandsClear meets at a pipe's largest. Prints, for each failing column
 * nozzle, its base and the first blocked ring with what blocks it: the vessel, the column's access structure (decks,
 * rails, ladders, davit), a manway, or another nozzle (by role).
 */
public final class RefluxProbe {
    public static void main(String[] args) throws Exception {
        Method geometry = ShapeRasterizer.class.getDeclaredMethod("geometry", EquipmentShape.class);
        geometry.setAccessible(true);
        var shapes = new ArrayList<>(ProcessUnitTest.everySize());
        shapes.addAll(ShapeRasterizerTest.shapes());
        double od = PipeSizes.MAX_OUTSIDE;
        int failing = 0;
        for (var shape : shapes) {
            if (shape.kind() != EquipmentShape.Kind.COLUMN) continue;
            for (int k = 0; k < shape.nozzles().size(); k++) {
                if (ProcessUnitTest.NozzleRoom.clear(shape, k, od)) continue;
                var nozzle = shape.nozzles().get(k);
                if (shape.diameter() == .5 || nozzle.role() == Role.VAPOUR_OUTLET && shape.diameter() == 3.) continue;
                failing++;
                System.out.println("== " + shape.diameter() + " m, " + shape.trays().trays() + " trays, front " + shape.front() + ", support " + shape.support()
                        + ", trays " + shape.trays() + " :: " + nozzle.role() + " k=" + k);
                System.out.println("   base " + fmt(nozzle.base()) + " axis " + nozzle.axis() + " sign " + nozzle.sign() + " length " + f(nozzle.length())
                        + " defaultOd " + f(nozzle.defaultOd()) + " room " + f(ProcessUnitTest.NozzleRoom.room(shape, k)));
                Object g = geometry.invoke(null, shape);
                report(g, shape, nozzle, k, od);
            }
        }
        System.out.println("failing " + failing);
    }

    private static void report(Object g, EquipmentShape shape, EquipmentShape.Nozzle nozzle, int k, double od) throws Exception {
        Method inVessel = method(g, "inVessel", double[].class), inNozzle = method(g, "inNozzle", double[].class, int.class, int.class);
        Object column = field(g, "column"), manway = field(g, "manway");
        Method columnInside = column == null ? null : method(column, "inside", double[].class);
        Method manwayInside = manway == null ? null : method(manway, "inside", double[].class);
        double to = nozzle.length() - PipeSizes.flangeThickness(od) - PipeSizes.hub(od) - .01, radius = od / 2 + .01;
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        int printed = 0;
        for (double s = 0; s <= to + 1e-9 && printed < 6; s += .05) {
            var hits = new StringBuilder();
            for (int j = 0; j < 24; j++) {
                double angle = 2 * Math.PI * j / 24;
                var p = nozzle.base().clone();
                p[axis] += nozzle.sign() * s;
                p[u] += radius * Math.cos(angle);
                p[v] += radius * Math.sin(angle);
                var what = new ArrayList<String>();
                if ((boolean) inVessel.invoke(g, (Object) p)) what.add("vessel");
                if (columnInside != null && (boolean) columnInside.invoke(column, (Object) p)) what.add("access");
                if (manwayInside != null && (boolean) manwayInside.invoke(manway, (Object) p)) what.add("manway");
                for (int n = 0; n < shape.nozzles().size(); n++)
                    if (n != k && (boolean) inNozzle.invoke(g, p, n, 0)) what.add("nozzle " + shape.nozzles().get(n).role());
                if (!what.isEmpty()) hits.append(" [").append(j * 15).append("deg ").append(fmt(p)).append(' ').append(what).append(']');
            }
            if (hits.length() > 0) {
                System.out.println("   s=" + f(s) + hits);
                printed++;
            }
        }
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

    private static String f(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }

    private static String fmt(double[] p) {
        return "(" + f(p[0]) + ", " + f(p[1]) + ", " + f(p[2]) + ")";
    }
}
