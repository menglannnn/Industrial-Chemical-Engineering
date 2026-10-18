package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

/**
 * The open steel of the solid-fuel units ({@code NEW_UNITS_SURVEY_SOLID_FUEL.md} section 2; owner decision D4 2026-10-18:
 * each unit carries the structure it stands in): a gasifier house's grating floors round the opening its vessel passes
 * through, and the stair tower beside a house or a boiler's furnace (the fixed-bed gasifier's and the grate boiler's).
 *
 * <p>A stair tower climbs from the ground to each of its levels in pairs of straight flights {@link #WIDTH} wide (OSHA
 * 1910.25: industrial stairs about 39°, at most {@link #FLIGHT} between landings): the first rises along +z in the outer
 * lane from the front landing to a landing {@link #DEPTH} deep behind it, the second back along -z in the inner lane to
 * the next front landing, so every front landing lies at the same place beside the wall, where the floors join it. A level
 * further up than two flights reach takes more pairs, their front landings railed on the wall side.
 */
final class FixedBedGasifierHouse {
    /** A flight's width between its stringers, a landing's depth along the flights, the inner lane's distance from the wall, m. */
    static final double WIDTH = 1.1, DEPTH = 1.2, INSIDE = .25;
    /** A landing's width out from the wall: both lanes and the 0.1 m between them. */
    static final double LANDING = INSIDE + 2 * WIDTH + .1;
    /** A flight's highest rise, m (OSHA 1910.25: 12 ft between landings). */
    static final double FLIGHT = 3.6;
    /** A floor beam's depth and flange, a column's section, m. */
    static final double BEAM = .3, FLANGE = .2, COLUMN = .35;

    private FixedBedGasifierHouse() {
    }

    /**
     * A house floor over x0..x1 by z0..z1 at {@code level} round the opening for a part {@code part} in radius about the
     * vertical through (a, c): four grating decks round a square opening (the long ones along both sides, railed at the
     * house's edge and across their ends, the short ones front and back between them), grating filling the opening's
     * corners round the part, and beams under the edges and round the opening. The west side's rail is open over
     * {@code westGaps} (pairs of z), where the stair tower's landings join.
     */
    static void floor(ProcessUnit u, double x0, double x1, double z0, double z1, double a, double c, double part, double level, double... westGaps) {
        double rh = part + Access.GAP, h = rh + Access.GAP, top = level + Access.DECK;
        double[] along = {0, 0, 1}, across = {1, 0, 0};
        var gaps = new double[westGaps.length];
        for (int k = 0; k < gaps.length; k++) gaps[k] = westGaps[k] - z0;
        u.platform(new Access.Line(new double[]{a - rh, 0, z0}, along, new double[]{-1, 0, 0}, z1 - z0), level, a - rh - x0, true, true, gaps);
        u.platform(new Access.Line(new double[]{a + rh, 0, z0}, along, new double[]{1, 0, 0}, z1 - z0), level, x1 - a - rh, true, true);
        u.platform(new Access.Line(new double[]{a - h, 0, c - rh}, across, new double[]{0, 0, -1}, 2 * h), level, c - rh - z0, false, false);
        u.platform(new Access.Line(new double[]{a - h, 0, c + rh}, across, new double[]{0, 0, 1}, 2 * h), level, z1 - c - rh, false, false);
        // The opening's corners: grating from the square's corner to the part's round, a fan of triangles each.
        int k = rh < 1.5 ? 3 : 6;
        for (int qx = -1; qx <= 1; qx += 2)
            for (int qz = -1; qz <= 1; qz += 2) {
                var edge = new ArrayList<double[]>();
                edge.add(new double[]{a + qx * h, c});
                for (int i = 0; i <= k; i++) {
                    double phi = Math.PI / 2 * i / k;
                    edge.add(new double[]{a + qx * rh * DeterministicMath.cos(phi), c + qz * rh * DeterministicMath.sin(phi)});
                }
                edge.add(new double[]{a, c + qz * h});
                for (int i = 0; i + 1 < edge.size(); i++)
                    for (double y : new double[]{level, top}) {
                        var p = edge.get(i);
                        var q = edge.get(i + 1);
                        u.mesh.flat(List.of(new double[]{a + qx * h, y, c + qz * h}, new double[]{p[0], y, p[1]}, new double[]{q[0], y, q[1]}), new double[]{0, y == top ? 1 : -1, 0},
                                Material.GRATING);
                    }
            }
        u.solids.add(ProcessUnit.Solid.ring(a, c, level, top, rh, h * Math.sqrt(2), 0, 0));
        // Beams under the house's edges and round the opening.
        double y0 = level - BEAM;
        beam(u, new double[]{x0, y0, z0}, new double[]{x1, level, z0 + FLANGE});
        beam(u, new double[]{x0, y0, z1 - FLANGE}, new double[]{x1, level, z1});
        beam(u, new double[]{x0, y0, z0 + FLANGE}, new double[]{x0 + FLANGE, level, z1 - FLANGE});
        beam(u, new double[]{x1 - FLANGE, y0, z0 + FLANGE}, new double[]{x1, level, z1 - FLANGE});
        beam(u, new double[]{x0 + FLANGE, y0, c - h - FLANGE}, new double[]{x1 - FLANGE, level, c - h});
        beam(u, new double[]{x0 + FLANGE, y0, c + h}, new double[]{x1 - FLANGE, level, c + h + FLANGE});
        beam(u, new double[]{a - h - FLANGE, y0, c - h}, new double[]{a - h, level, c + h});
        beam(u, new double[]{a + h, y0, c - h}, new double[]{a + h + FLANGE, level, c + h});
    }

    /** A beam under a deck: its top hidden under the grating; a player bumps into it. */
    static void beam(ProcessUnit u, double[] lo, double[] hi) {
        u.mesh.box(lo, hi, Mesh.ALL_FACES & ~8, Material.STEELWORK);
        u.solids.add(ProcessUnit.Solid.box(lo, hi));
    }

    /** A steel column of {@link #COLUMN} section at (x, z) from {@code y0} to {@code y1}. */
    static void column(ProcessUnit u, double x, double z, double y0, double y1) {
        u.box(new double[]{x - COLUMN / 2, y0, z - COLUMN / 2}, new double[]{x + COLUMN / 2, y1, z + COLUMN / 2}, Material.STEELWORK, true);
    }

    /**
     * A stair tower beside the wall at x {@code wall}, its lanes towards {@code sx} (±1 along x), its front landings from
     * {@code zF} - {@link #DEPTH} to {@code zF}, climbing from the ground to each deck level of {@code levels} (ascending);
     * a front landing whose level is {@code joined} opens onto a floor at the wall, the others are railed there, as are
     * the back landings. Posts at its corners and beams under its landings. Returns the back of its farthest landing.
     */
    static double tower(ProcessUnit u, double wall, int sx, double zF, double[] levels, boolean[] joined) {
        double inner = wall + sx * (INSIDE + WIDTH / 2), outer = wall + sx * (INSIDE + 1.5 * WIDTH + .1), far = zF, from = -Access.DECK;
        var backs = new ArrayList<double[]>();
        for (int k = 0; k < levels.length; k++) {
            int pairs = (int) Math.ceil((levels[k] - from) / (2 * FLIGHT) - 1e-9);
            double start = from;
            for (int p = 1; p <= pairs; p++) {
                double to = p == pairs ? levels[k] : start + (levels[k] - start) * p / pairs, mid = (from + to) / 2;
                double walk = from < 0 ? 0 : from + Access.DECK;
                double len = u.stair(outer, zF, 2, 1, WIDTH, walk, mid + Access.DECK);
                landing(u, wall, sx, zF + len, zF + len + DEPTH, mid, false, true, false);
                backs.add(new double[]{zF + len + DEPTH, mid});
                u.stair(inner, zF + len, 2, -1, WIDTH, mid + Access.DECK, to + Access.DECK);
                landing(u, wall, sx, zF - DEPTH, zF, to, true, false, joined[k] && p == pairs);
                far = Math.max(far, zF + len + DEPTH);
                from = to;
            }
        }
        // Posts at the tower's outer corners and at its back next to the wall; a beam from each back landing to the back
        // posts, and across.
        double top = levels[levels.length - 1] + Access.DECK + Access.RAIL, xo = wall + sx * (LANDING + .1), xi = wall + sx * (INSIDE - .05);
        for (double z : new double[]{zF - DEPTH - .1, far + .1}) post(u, xo, z, top);
        post(u, xi, far + .1, top);
        for (var b : backs) {
            if (far - b[0] > .05) {
                beam(u, new double[]{Math.min(xo, wall + sx * (LANDING - FLANGE)), b[1] - BEAM, b[0]}, new double[]{Math.max(xo, wall + sx * (LANDING - FLANGE)), b[1], far});
                beam(u, new double[]{Math.min(xi, wall + sx * (INSIDE + FLANGE)) - .05, b[1] - BEAM, b[0]}, new double[]{Math.max(xi, wall + sx * (INSIDE + FLANGE)) + .05, b[1], far});
            }
            beam(u, new double[]{Math.min(xi, xo), b[1] - BEAM, far - FLANGE}, new double[]{Math.max(xi, xo), b[1], far});
        }
        return far;
    }

    /** A post of the stair tower at (x, z) from the ground to {@code top}. */
    private static void post(ProcessUnit u, double x, double z, double top) {
        u.box(new double[]{x - .1, 0, z - .1}, new double[]{x + .1, top, z + .1}, Material.STEELWORK, true);
    }

    /**
     * A landing beside the wall from z0 to z1 at {@code level}, both lanes wide, railed across a closed end and, unless it
     * is {@code open} onto a floor, along the wall; a beam under its outer edge and its ends.
     */
    private static void landing(ProcessUnit u, double wall, int sx, double z0, double z1, double level, boolean closedStart, boolean closedEnd, boolean open) {
        double[] along = {0, 0, 1};
        u.platform(new Access.Line(new double[]{wall, 0, z0}, along, new double[]{sx, 0, 0}, z1 - z0), level, LANDING, closedStart, closedEnd);
        if (!open) u.railing(new Access.Line(new double[]{wall + sx * .3, 0, z0}, along, new double[]{-sx, 0, 0}, z1 - z0), level, .3);
        double xo = wall + sx * LANDING, xb = xo - sx * FLANGE, y0 = level - BEAM;
        beam(u, new double[]{Math.min(xo, xb), y0, z0}, new double[]{Math.max(xo, xb), level, z1});
        for (double z : new double[]{z0, z1 - FLANGE})
            beam(u, new double[]{Math.min(wall, xb), y0, z}, new double[]{Math.max(wall, xb), level, z + FLANGE});
    }
}
