package com.wormzjl.icheme.science.equipment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Scratch probe (batch 2026-10-26-multi-machine-framework, owner hint 2026-10-27 "off-centre connection of ports"): for
 * every column side nozzle of NewSweep's buildable designs whose neck is not clear at a pipe's largest (the F3 layout as
 * built), the largest clear outside diameter (0.05 m steps) on its centre line, and the largest with the nozzle shifted
 * off-centre in its pipe cell, in-face steps of 0.05 m up to 0.25 m along each axis, under the off-centre limit (pipe-laying
 * plan section 6): the flange stays on its own block face, max(|du|, |dv|) + flange(OD) / 2 <= 0.5 m.
 * The solid a neck must clear is the shape's without that nozzle's own design neck (vessel, access, manway, other nozzles).
 */
public final class ShiftProbe {
    static Method geometry, solid, inNozzle;

    public static void main(String[] args) throws Exception {
        geometry = ShapeRasterizer.class.getDeclaredMethod("geometry", EquipmentShape.class);
        geometry.setAccessible(true);
        var hist = new TreeMap<String, Integer>();
        int n = 0;
        double sumCentre = 0, sumShift = 0;
        for (double d : new double[]{.5, 1, 1.5, 2, 3, 4, 6, 8, 15})
            for (int trays : new int[]{1, 3, 5, 10, 15, 20, 30, 40, 64})
                for (String config : new String[]{"kettle", "kettle+steam+draw+PA", "steam+draw", "full"}) {
                    ColumnTrays t;
                    try {
                        t = NewSweep.trays(trays, config);
                    } catch (IllegalArgumentException invalid) {
                        continue;
                    }
                    var s = EquipmentShape.column(d, t, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(d));
                    if (ColumnDesign.misfit(s).isPresent()) continue;
                    Object g = geometry.invoke(null, s);
                    if (solid == null) {
                        solid = method(g, "solid", double[].class);
                        inNozzle = method(g, "inNozzle", double[].class, int.class, int.class);
                    }
                    for (int k = 0; k < s.nozzles().size(); k++) {
                        var nozzle = s.nozzles().get(k);
                        if (nozzle.axis() == 1 || d == .5) continue;
                        if (ProcessUnitTest.NozzleRoom.clear(s, k, PipeSizes.MAX_OUTSIDE)) continue;
                        double centre = largest(g, s, k, 0, 0), best = centre, bu = 0, bv = 0;
                        for (double du = -.25; du <= .25 + 1e-9; du += .05)
                            for (double dv = -.25; dv <= .25 + 1e-9; dv += .05) {
                                double od = largest(g, s, k, du, dv);
                                if (od > best + 1e-9) {
                                    best = od;
                                    bu = du;
                                    bv = dv;
                                }
                            }
                        n++;
                        sumCentre += centre;
                        sumShift += best;
                        hist.merge(String.format(Locale.ROOT, "d %4.1f %-14s centre %.2f -> shifted %.2f (shift across %+.2f, up %+.2f)", d, nozzle.role(), centre, best,
                                nozzle.axis() == 0 ? bv : bu, nozzle.axis() == 0 ? bu : bv), 1, Integer::sum);
                    }
                }
        hist.forEach((k, v) -> System.out.println(String.format("%4d  %s", v, k)));
        System.out.println(String.format(Locale.ROOT, "TOTAL %d necks: mean largest clear %.3f m centred, %.3f m shifted", n, sumCentre / n, sumShift / n));
    }

    /** The largest outside diameter, 0.05 m steps from 0.1 m to a pipe's largest, whose neck clears with the nozzle shifted (du, dv) in its face; 0 for none. */
    static double largest(Object g, EquipmentShape s, int k, double du, double dv) throws Exception {
        double best = 0;
        for (double od = .1; od <= PipeSizes.MAX_OUTSIDE + 1e-9; od += .05) {
            if (Math.max(Math.abs(du), Math.abs(dv)) + PipeSizes.flange(od) / 2 > .5 + 1e-9) break;
            if (!clear(g, s, k, od, du, dv)) break;
            best = od;
        }
        return best;
    }

    /** NozzleRoom.clear for a shifted neck, against the solid without the nozzle's own design neck. */
    static boolean clear(Object g, EquipmentShape s, int k, double od, double du, double dv) throws Exception {
        var nozzle = s.nozzles().get(k);
        double to = nozzle.length() - PipeSizes.flangeThickness(od) - PipeSizes.hub(od) - .01, radius = od / 2 + .01;
        double near = Math.max(0, leaves(g, s, k, nozzle.defaultOd() / 2 + .01, 0, to, du, dv));
        double out = leaves(g, s, k, radius, near, Math.min(to, near + PipeSizes.MAX_OUTSIDE / 2), du, dv), from = out + ProcessUnitTest.NozzleRoom.SURFACE;
        if (out < 0) return false;
        int rings = Math.max(1, (int) Math.ceil((to - from) / .05));
        for (int i = 0; i <= rings && to > from; i++) if (!ring(g, s, k, radius, from + (to - from) * i / rings, du, dv)) return false;
        return true;
    }

    private static double leaves(Object g, EquipmentShape s, int k, double radius, double from, double to, double du, double dv) throws Exception {
        for (double x = from; x <= to + 1e-9; x += .02) if (ring(g, s, k, radius, x, du, dv)) return x;
        return -1;
    }

    private static boolean ring(Object g, EquipmentShape s, int k, double radius, double at, double du, double dv) throws Exception {
        var nozzle = s.nozzles().get(k);
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        for (int j = 0; j < 24; j++) {
            double angle = 2 * Math.PI * j / 24;
            var p = nozzle.base().clone();
            p[axis] += nozzle.sign() * at;
            p[u] += du + radius * Math.cos(angle);
            p[v] += dv + radius * Math.sin(angle);
            if ((boolean) solid.invoke(g, (Object) p)) return false;
            for (int m = 0; m < s.nozzles().size(); m++) if (m != k && (boolean) inNozzle.invoke(g, p, m, 0)) return false;
        }
        return true;
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
}
