package com.wormzjl.icheme.science.equipment;

/**
 * A nozzle's neck as the world sees it (owner decision 2026-10-21: a 1 m pipe has a 1 m root, other components give way):
 * whether a neck of a given outside diameter, from where it has left the vessel to its flange's hub, stands clear of the
 * rest of the solid (the vessel, its supports, the unit's parts, the other nozzles).
 */
final class NozzleRoom {
    /** How far beyond where it leaves the vessel a neck may still meet the vessel's own rings (girth flanges, pads, a channel's cover), m. */
    static final double SURFACE = .1;
    /** The step along the neck at which {@link #leaves} looks for where a ring has left the solid, m. */
    private static final double STEP = .02;

    private NozzleRoom() {
    }

    /**
     * Whether nozzle {@code k}'s neck at outside diameter {@code od} stands clear of the rest of the shape's solid, beyond
     * {@link #SURFACE} from where it has left the vessel. Where it leaves: the first point along the neck where a ring of
     * its size is wholly out of the solid, looked for from where a ring just round its unconnected neck has left (its own
     * vessel's surface there) up to half a pipe's largest beyond, the most a convex body's curve holds a neck's edge
     * behind its centre line, or up to its flange's hub; a neck that has not left the solid by then meets it.
     */
    static boolean clear(EquipmentShape shape, int k, double od) {
        var nozzle = shape.nozzles().get(k);
        double to = nozzle.length() - PipeSizes.flangeThickness(od) - PipeSizes.hub(od) - .01, radius = od / 2 + .01;
        double near = Math.max(0, leaves(shape, nozzle, nozzle.defaultOd() / 2 + .01, 0, to));
        double out = leaves(shape, nozzle, radius, near, Math.min(to, near + PipeSizes.MAX_OUTSIDE / 2)), from = out + SURFACE;
        if (out < 0) return false;
        int rings = Math.max(1, (int) Math.ceil((to - from) / .05));
        for (int i = 0; i <= rings && to > from; i++)
            if (!ringClear(shape, nozzle, radius, from + (to - from) * i / rings)) return false;
        return true;
    }

    /** The first distance from {@code from} to {@code to} along the neck, in {@link #STEP}s, where a ring of {@code radius} is wholly out of the solid; -1 where there is none. */
    private static double leaves(EquipmentShape shape, EquipmentShape.Nozzle nozzle, double radius, double from, double to) {
        for (double s = from; s <= to + 1e-9; s += STEP) if (ringClear(shape, nozzle, radius, s)) return s;
        return -1;
    }

    /** Whether the ring of {@code radius} round the nozzle's axis, {@code s} from its base, lies wholly out of the shape's solid. */
    private static boolean ringClear(EquipmentShape shape, EquipmentShape.Nozzle nozzle, double radius, double s) {
        var transform = shape.transform();
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3, around = 24;
        for (int j = 0; j < around; j++) {
            double angle = 2 * Math.PI * j / around;
            var p = nozzle.base().clone();
            p[axis] += nozzle.sign() * s;
            p[u] += radius * Math.cos(angle);
            p[v] += radius * Math.sin(angle);
            var box = transform.point(p);
            if (ShapeRasterizer.inside(shape, box[0], box[1], box[2])) return false;
        }
        return true;
    }

    /**
     * The largest neck, to 0.05 m and at most a pipe's largest, that stands clear, holding the nozzle's base; searched from
     * just over its unconnected size (whose own neck the solid holds), -1 where even that is not clear.
     */
    static double room(EquipmentShape shape, int k) {
        double best = -1;
        for (double od = Math.ceil((shape.nozzles().get(k).defaultOd() + .02) / .05) * .05; od <= PipeSizes.MAX_OUTSIDE + 1e-9; od += .05) {
            if (!clear(shape, k, od)) break;
            best = od;
        }
        return best;
    }
}
