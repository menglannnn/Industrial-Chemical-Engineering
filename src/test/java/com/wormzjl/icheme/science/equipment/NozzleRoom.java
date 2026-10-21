package com.wormzjl.icheme.science.equipment;

/**
 * A nozzle's neck as the world sees it (owner request 2026-10-21: a nozzle's root follows the connected pipe's size, up
 * to the room the vessel has there): whether a neck of a given outside diameter, from where it has left the vessel to its
 * flange's hub, stands clear of the rest of the solid (the vessel, its supports, the unit's parts, the other nozzles).
 */
final class NozzleRoom {
    /** How far beyond where it leaves the vessel a neck may still meet the vessel's own rings (girth flanges, pads, a channel's cover), m. */
    static final double SURFACE = .1;

    private NozzleRoom() {
    }

    /** Whether nozzle {@code k}'s neck at outside diameter {@code od} stands clear of the rest of the shape's solid. */
    static boolean clear(EquipmentShape shape, int k, double od) {
        var nozzle = shape.nozzles().get(k);
        var transform = shape.transform();
        int axis = nozzle.axis(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        // Up to the hub of the flange at this size; the nozzle's own unconnected neck, hub and flange (no larger) lie inside.
        double from = nozzle.exit() + SURFACE, to = nozzle.length() - PipeSizes.flangeThickness(od) - PipeSizes.hub(od) - .01, radius = od / 2 + .01;
        int rings = Math.max(1, (int) Math.ceil((to - from) / .05)), around = 24;
        for (int i = 0; i <= rings && to > from; i++) {
            double s = from + (to - from) * i / rings;
            for (int j = 0; j < around; j++) {
                double angle = 2 * Math.PI * j / around;
                var p = nozzle.base().clone();
                p[axis] += nozzle.sign() * s;
                p[u] += radius * Math.cos(angle);
                p[v] += radius * Math.sin(angle);
                var box = transform.point(p);
                if (ShapeRasterizer.inside(shape, box[0], box[1], box[2])) return false;
            }
        }
        return true;
    }

    /**
     * The largest neck, to 0.05 m and at most a pipe's largest, that stands clear, holding the nozzle's base and exit;
     * searched from just over its unconnected size (whose own neck the solid holds), -1 where even that is not clear.
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
