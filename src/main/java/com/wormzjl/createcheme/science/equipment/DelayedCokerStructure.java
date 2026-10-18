package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.DelayedCokerPlan.*;
import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * A delayed coker's steel frame on the table, its decks and its stair tower ({@link DelayedCokerPlan};
 * {@code NEW_UNITS_SURVEY_FCC_COKER.md} section 3; owner decision D4 2026-10-18).
 *
 * <p>The frame: H-section columns (0.4 + 0.02 D) at the table's corners and between the drums, front and back, from the
 * table's top to the cutting deck; 0.45 m beams along both faces and across at every column line under each deck; one
 * brace a story in both end frames; lateral guides from the faces' beams to a band on each drum under the intermediate
 * deck [US8221591]. The decks lie on the beams (grating, toe boards and handrails, no brackets), their outer edges 0.05 m
 * in from the structure's faces: the switch deck under the table [Coking 101], full but for an opening round each chute;
 * the intermediate deck, full but for a square round each jacket whose corners are filled in to the jacket; the cutting
 * deck on top, full, the derricks standing on it. The stair tower stands behind the middle of the row: two lanes of 1 m
 * flights side by side (the one next to the structure rising +x, the other -x), at most 3.6 m a flight, with a landing at
 * each end piece by piece (each starts where its lane's flight ends, so every flight's length fits), joining the switch
 * deck, the table's top, the intermediate deck and the cutting deck through openings in their back handrails; four posts
 * at its corners.
 */
final class DelayedCokerStructure {
    private static final double[] UP = {0, 1, 0}, DOWN = {0, -1, 0};
    /** A beam's depth under a deck, a beam's flange width, a brace's section, m. */
    static final double BEAM = .45, FLANGE_WIDTH = .3, BRACE = .15;

    private DelayedCokerStructure() {
    }

    static void draw(ProcessUnit u, DelayedCokerPlan p) {
        frame(u, p);
        switchDeck(u, p);
        for (int k = 0; k < p.middle.length; k++) middleDeck(u, p, k);
        cuttingDeck(u, p);
        tower(u, p);
    }

    /** The steel frame: columns, beams under every deck, end bracing, lateral guides at the intermediate deck. */
    static void frame(ProcessUnit u, DelayedCokerPlan p) {
        double sc = p.steel;
        for (double x : p.lines)
            for (double za : new double[]{p.z0, p.zb - sc}) {
                double xa = Math.max(p.x0, Math.min(x - sc / 2, p.x1 - sc));
                DelayedCokerDrums.box(u, new double[]{xa, p.yTable, za}, new double[]{xa + sc, p.cutLevel, za + sc}, ALL_FACES & ~(4 | 8), Material.STEELWORK, true);
            }
        var levels = new double[p.middle.length + 1];
        for (int k = 0; k < p.middle.length; k++) levels[k] = p.middle[k];
        levels[p.middle.length] = p.cutLevel;
        for (double level : levels) {
            double y0 = level - BEAM;
            for (double za : new double[]{p.z0, p.zb - FLANGE_WIDTH})
                DelayedCokerDrums.box(u, new double[]{p.x0, y0, za}, new double[]{p.x1, level, za + FLANGE_WIDTH}, ALL_FACES & ~8, Material.STEELWORK, true);
            for (double x : p.lines) {
                double xa = Math.max(p.x0, Math.min(x - FLANGE_WIDTH / 2, p.x1 - FLANGE_WIDTH));
                DelayedCokerDrums.box(u, new double[]{xa, y0, p.z0 + FLANGE_WIDTH}, new double[]{xa + FLANGE_WIDTH, level, p.zb - FLANGE_WIDTH}, ALL_FACES & ~(8 | 16 | 32),
                        Material.STEELWORK, true);
            }
        }
        // One brace a story in both end frames, alternating, between the columns' inner faces.
        double from = p.yTable;
        for (int k = 0; k < levels.length; k++) {
            double level = levels[k], za = k % 2 == 0 ? p.z0 + sc : p.zb - sc, zz = k % 2 == 0 ? p.zb - sc : p.z0 + sc;
            for (double xe : new double[]{p.x0 + sc / 2, p.x1 - sc / 2})
                u.mesh.bar(new double[]{xe, from + .2, za}, new double[]{xe, level - BEAM, zz}, BRACE, new double[]{1, 0, 0}, NO_ENDS, Material.STEELWORK);
            from = level;
        }
        // Lateral guides: a band round each drum under the intermediate deck, struts to the faces' beams.
        double top = p.middle[p.middle.length - 1], y0 = top - BEAM, y1 = top - .15;
        for (double x : p.xd) {
            var up = Frame.of(new double[]{x, 0, p.zc}, 1, 1);
            double band = p.jacket + .06;
            u.mesh.prism(up, band, y0 - .05, y1, p.sides, Material.STEELWORK);
            u.mesh.annulus(up, p.jacket, band, y0 - .05, p.sides, -1, Material.STEELWORK);
            u.mesh.annulus(up, p.jacket, band, y1, p.sides, 1, Material.STEELWORK);
            for (int side = -1; side <= 1; side += 2) {
                double za = side < 0 ? p.z0 + FLANGE_WIDTH : p.zc + band, zz = side < 0 ? p.zc - band : p.zb - FLANGE_WIDTH;
                DelayedCokerDrums.box(u, new double[]{x - .12, y0, za}, new double[]{x + .12, y1, zz}, ALL_FACES & ~(16 | 32), Material.STEELWORK, true);
            }
        }
    }

    /** The switch deck under the table: full but for an opening round each chute. */
    static void switchDeck(ProcessUnit u, DelayedCokerPlan p) {
        holed(u, p, p.switchLevel, CHUTE_HOLE, p.gaps(p.stops[0]));
    }

    /**
     * Intermediate deck {@code k}: the whole floor of the structure round the drums, its square opening round each jacket
     * filled in at the corners, so the grating meets the jacket all round.
     */
    static void middleDeck(ProcessUnit u, DelayedCokerPlan p, int k) {
        double level = p.middle[k], j = p.jacket + .05, ra = p.jacket + .02, y1 = level + Access.DECK;
        holed(u, p, level, j, p.gaps(p.stops[2 + k]));
        int m = 2;
        for (double x : p.xd) {
            for (int q = 0; q < 4; q++) {
                // Corner q (+x+z, -x+z, -x-z, +x-z): two triangles from the square's corner to the jacket at 0, 45 and 90°
                // (their far edges pass a little inside the jacket, out of sight); a ring collides from the jacket out.
                double sx = q == 0 || q == 3 ? 1 : -1, sz = q < 2 ? 1 : -1;
                double[] c = {x + sx * j, 0, p.zc + sz * j};
                var arc = new double[m + 1][];
                for (int a = 0; a <= m; a++) {
                    double t = Math.PI / 2 * a / m, ca = DeterministicMath.cos(t), sa = DeterministicMath.sin(t);
                    arc[a] = new double[]{x + sx * (a == 0 ? j : ra * ca), 0, p.zc + sz * (a == m ? j : ra * sa)};
                }
                for (int a = 0; a < m; a++)
                    for (double y : new double[]{level, y1}) {
                        var tri = List.of(at(c, y), at(arc[a], y), at(arc[a + 1], y));
                        u.mesh.flat(tri, y == y1 ? UP : DOWN, Material.GRATING);
                    }
            }
            u.solids.add(ProcessUnit.Solid.ring(x, p.zc, level, y1, p.jacket, j * Math.sqrt(2) + .01, 0, 0));
        }
    }

    private static double[] at(double[] p, double y) {
        return new double[]{p[0], y, p[2]};
    }

    /** The cutting deck: the whole floor of the structure; the derricks stand on it, the top unheading devices in it. */
    static void cuttingDeck(ProcessUnit u, DelayedCokerPlan p) {
        holed(u, p, p.cutLevel, 0, p.gaps(p.stops[p.stops.length - 1]));
    }

    /**
     * A full deck at {@code level} over the structure but for a square opening of half side {@code hole} round each drum's
     * axis (none: 0): a strip in front and one behind (its handrail open at {@code gaps}), pieces between the openings and
     * at the ends.
     */
    static void holed(ProcessUnit u, DelayedCokerPlan p, double level, double hole, double[] gaps) {
        // Each piece's wall line lies Access.GAP back from where its grating starts, so the pieces meet without slits.
        double g = Access.GAP, xa = p.x0 + EDGE, xb = p.x1 - EDGE, length = xb - xa, zf = p.zc - hole, zr = p.zc + hole;
        strip(u, new Access.Line(new double[]{xa, 0, zf + g}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, length), level, zf + g - p.z0 - EDGE, true, true, true);
        strip(u, new Access.Line(new double[]{xa, 0, zr - g}, new double[]{1, 0, 0}, new double[]{0, 0, 1}, length), level, p.zb - EDGE - zr + g, true, true, true, gaps);
        if (hole <= 0) return;
        double first = p.xd[0] - hole + g, last = p.xd[p.drums - 1] + hole - g;
        floor(u, new Access.Line(new double[]{first, 0, zf}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, 2 * hole), level, first - xa, false, false, true);
        for (int i = 0; i + 1 < p.drums; i++)
            floor(u, new Access.Line(new double[]{p.xd[i] + hole - g, 0, zf}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, 2 * hole), level, p.pitch - 2 * hole + g, false, false,
                    false);
        floor(u, new Access.Line(new double[]{last, 0, zf}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, 2 * hole), level, xb - last, false, false, true);
    }

    /**
     * A grating deck lying on the frame's beams ({@link Access}'s sizes, no brackets): its deck {@code width} out from
     * {@code run}'s wall line, its walking surface {@link Access#DECK} over {@code level}; when {@code railed} a toe board and
     * a handrail ({@link ProcessUnit#rails}) along its outer edge but over the {@code gaps}, and across its closed ends. It
     * is registered with the unit's decks for the access gates and collides as drawn.
     */
    static void floor(ProcessUnit u, Access.Line run, double level, double width, boolean closedStart, boolean closedEnd, boolean railed, double... gaps) {
        deck(u, run, level, width, true, closedStart, closedEnd, railed, gaps);
    }

    /** A {@link #floor} whose edge along its wall line is not drawn: another piece abuts it there, or a drum stands in front of it. */
    static void strip(ProcessUnit u, Access.Line run, double level, double width, boolean closedStart, boolean closedEnd, boolean railed, double... gaps) {
        deck(u, run, level, width, false, closedStart, closedEnd, railed, gaps);
    }

    private static void deck(ProcessUnit u, Access.Line run, double level, double width, boolean wall, boolean closedStart, boolean closedEnd, boolean railed, double... gaps) {
        double y0 = level, y1 = level + Access.DECK, t = y1 + Access.TOE, length = run.length(), g = Access.GAP, toe = width - 1 / 32.0;
        double[] out = run.out(0), along = run.along(0);
        u.mesh.flat(List.of(run.at(0, g, y1), run.at(length, g, y1), run.at(length, width, y1), run.at(0, width, y1)), UP, Material.GRATING);
        u.mesh.flat(List.of(run.at(0, g, y0), run.at(length, g, y0), run.at(length, width, y0), run.at(0, width, y0)), DOWN, Material.GRATING);
        if (wall) u.mesh.flat(List.of(run.at(0, g, y0), run.at(length, g, y0), run.at(length, g, y1), run.at(0, g, y1)), neg(out), Material.GRATING);
        for (double s : new double[]{0, length})
            u.mesh.flat(List.of(run.at(s, g, y0), run.at(s, width, y0), run.at(s, width, y1), run.at(s, g, y1)), s == 0 ? neg(along) : along, Material.GRATING);
        // The toe board along each piece of the handrail, its outer face one with the deck's edge (as Access.deck draws it);
        // the bare edge elsewhere.
        var pieces = new ArrayList<double[]>();
        if (railed) {
            double start = 0;
            for (int k = 0; k + 1 < gaps.length; k += 2) {
                if (gaps[k] > start + .05) pieces.add(new double[]{start, gaps[k]});
                start = Math.max(start, gaps[k + 1]);
            }
            if (length > start + .05) pieces.add(new double[]{start, length});
        }
        double from = 0;
        for (var q : pieces) {
            if (q[0] > from + 1e-6) u.mesh.flat(List.of(run.at(from, width, y0), run.at(q[0], width, y0), run.at(q[0], width, y1), run.at(from, width, y1)), out, Material.GRATING);
            u.mesh.flat(List.of(run.at(q[0], width, y0), run.at(q[1], width, y0), run.at(q[1], width, t), run.at(q[0], width, t)), out, Material.HANDRAIL);
            u.mesh.flat(List.of(run.at(q[0], toe, y1), run.at(q[1], toe, y1), run.at(q[1], toe, t), run.at(q[0], toe, t)), neg(out), Material.HANDRAIL);
            from = q[1];
        }
        if (length > from + 1e-6) u.mesh.flat(List.of(run.at(from, width, y0), run.at(length, width, y0), run.at(length, width, y1), run.at(from, width, y1)), out, Material.GRATING);
        if (railed) u.rails(run, level, width, closedStart, closedEnd, gaps);
        else u.rails(run, level, width, closedStart, closedEnd, 0, length);
        u.decks.add(new ProcessUnit.Deck(run, level, width, closedStart, closedEnd));
        u.solids.add(ProcessUnit.span(run.at(0, g, level), run.at(length, width, y1)));
        u.segment(run.at(0, width, y1), run.at(length, width, y1));
    }

    /**
     * The stair tower ({@link DelayedCokerPlan#levels}): flights alternating between lane A (next to the
     * structure, rising +x from its west landing line) and lane B (rising -x from its east one); after each flight a landing
     * piece in each lane at that end, the arriving lane's from where its flight ends, the other's from where the next
     * flight starts, out to the tower's end; the lane A piece open towards the structure where a deck joins it; four posts.
     */
    static void tower(ProcessUnit u, DelayedCokerPlan p) {
        double t0 = p.towerX0, a0 = t0 + LAND, b1 = a0 + p.lenMax, t1 = b1 + LAND, zb = p.zb, za = zb + EDGE, lane = 1 - 2 * EDGE, g = Access.GAP;
        int flights = p.levels.length;
        for (int f = 1; f <= flights; f++) {
            boolean laneA = f % 2 == 1, joins = false, last = f == flights;
            for (int stop : p.stops) joins |= stop == f;
            double level = p.levels[f - 1], base = p.walk(f - 1), top = p.walk(f);
            if (laneA) {
                u.stair(a0, zb + .5, 0, 1, FLIGHT, base, top);
                double ia = a0 + p.lens[f - 1];
                floor(u, new Access.Line(new double[]{ia - g, 0, za}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, lane), level, t1 - EDGE - ia + g, !joins, last, true);
                if (!last) floor(u, new Access.Line(new double[]{b1 - g, 0, za + 1}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, lane), level, t1 - EDGE - b1 + g, false, true, true);
            } else {
                u.stair(b1, zb + 1.5, 0, -1, FLIGHT, base, top);
                double ib = b1 - p.lens[f - 1];
                floor(u, new Access.Line(new double[]{ib + g, 0, za + 1}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, lane), level, ib + g - t0 - EDGE, false, true, true);
                floor(u, new Access.Line(new double[]{a0 + g, 0, za}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, lane), level, a0 + g - t0 - EDGE, !joins, false, true);
                // At the top, the lane A piece's flight-side edge has no flight up from it: a handrail closes it.
                if (last) u.rails(new Access.Line(new double[]{a0 + 1, 0, za}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, lane), level, 1, false, false);
            }
        }
        double height = p.walk(flights) + Access.RAIL + .1;
        for (double x : new double[]{t0 - .1, t1 + .1})
            for (double z : new double[]{zb + .1, zb + 1.9})
                DelayedCokerDrums.box(u, new double[]{x - .075, 0, z - .075}, new double[]{x + .075, height, z + .075}, ALL_FACES & ~4, Material.STEELWORK, true);
    }
}
