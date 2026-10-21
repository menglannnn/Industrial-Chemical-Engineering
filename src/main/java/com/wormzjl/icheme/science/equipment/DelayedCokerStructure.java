package com.wormzjl.icheme.science.equipment;

import java.util.List;

import static com.wormzjl.icheme.science.equipment.DelayedCokerPlan.*;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/**
 * A delayed coker's steel frame on the table, its decks and its stair tower ({@link DelayedCokerPlan};
 * {@code NEW_UNITS_SURVEY_FCC_COKER.md} section 3; owner decision D4 2026-10-18).
 *
 * <p>The frame: H-section columns (0.4 + 0.02 D) at the table's corners and between the drums, front and back, from the
 * table's top to the cutting deck; 0.45 m beams along both faces and across at every column line under each deck; X
 * braces in both end frames ({@link ProcessUnit#brace}), one a storey; lateral guides from the faces' beams to a band on
 * each drum under the intermediate deck [US8221591]. The decks lie on the beams (the kit's grating decks without
 * brackets), their outer edges 0.05 m in from the structure's faces: the switch deck under the table [Coking 101], full
 * but for an opening round each chute; the intermediate deck, full but for a square round each jacket filled in to the
 * jacket ({@link ProcessUnit#squareDeck}); the cutting deck on top, full, the derricks standing on it; a handrail round
 * the table's top. The stair tower ({@link ProcessUnit#stairTower}) stands behind the middle of the row: 0.9 m flights on
 * two lanes 1 m apart (the one next to the structure rising +x), at most 3.6 m a flight, switching back between landings
 * at its two ends on beams and corner posts, and joining the switch deck, the table's top, the intermediate deck and the
 * cutting deck through openings in their back handrails.
 */
final class DelayedCokerStructure {
    private static final double[] X = {1, 0, 0}, Z = {0, 0, 1};
    /** A beam's depth under a deck, a beam's flange width, a brace's section, m. */
    static final double BEAM = .45, FLANGE_WIDTH = .3, BRACE = .15;

    private DelayedCokerStructure() {
    }

    static void draw(ProcessUnit u, DelayedCokerPlan p) {
        List<double[]> openings = u.stairTower(p.tower, p.towerX, p.zb + .5, 0, 1, 1, true).openings();
        frame(u, p);
        u.squareDeck(p.x0 + EDGE, p.x1 - EDGE, p.z0 + EDGE, p.zb - EDGE, p.switchLevel, 0, p.zc, CHUTE_HOLE, p.xd, 0, 0, Access.RAILED | Access.WALL_EDGE,
                gaps(openings.get(0)), null);
        tableRail(u, p, openings.get(1));
        // The intermediate deck's fill round each jacket: an octagon on the jacket's corners, its chords out of sight inside
        // the jacket (a fill of the jacket's 24 sides costs 190 quads more at the largest size, which binds: owner D3).
        for (int k = 0; k < p.middle.length; k++)
            u.squareDeck(p.x0 + EDGE, p.x1 - EDGE, p.z0 + EDGE, p.zb - EDGE, p.middle[k], 0, p.zc, p.jacket + .05, p.xd, p.jacket, 8, Access.RAILED,
                    gaps(openings.get(2 + k)), null);
        cuttingDeck(u, p, openings.getLast());
    }

    /** A square deck's rail gaps ({@link ProcessUnit#squareDeck}): the stair tower's opening on its back (+z) edge. */
    private static double[][] gaps(double[] opening) {
        return new double[][]{null, null, opening, null};
    }

    /** The steel frame: columns, beams under every deck, end bracing, lateral guides at the intermediate deck. */
    static void frame(ProcessUnit u, DelayedCokerPlan p) {
        double sc = p.steel;
        for (double x : p.lines)
            for (double za : new double[]{p.z0, p.zb - sc}) {
                double xa = Math.max(p.x0, Math.min(x - sc / 2, p.x1 - sc));
                u.box(new double[]{xa, p.yTable, za}, new double[]{xa + sc, p.cutLevel, za + sc}, ALL_FACES & ~(4 | 8), Material.STEELWORK, true);
            }
        var levels = new double[p.middle.length + 1];
        for (int k = 0; k < p.middle.length; k++) levels[k] = p.middle[k];
        levels[p.middle.length] = p.cutLevel;
        for (double level : levels) {
            double y0 = level - BEAM;
            for (double za : new double[]{p.z0, p.zb - FLANGE_WIDTH}) u.beam(new double[]{p.x0, y0, za}, new double[]{p.x1, level, za + FLANGE_WIDTH});
            for (double x : p.lines) {
                double xa = Math.max(p.x0, Math.min(x - FLANGE_WIDTH / 2, p.x1 - FLANGE_WIDTH));
                u.box(new double[]{xa, y0, p.z0 + FLANGE_WIDTH}, new double[]{xa + FLANGE_WIDTH, level, p.zb - FLANGE_WIDTH}, ALL_FACES & ~(8 | 16 | 32), Material.STEELWORK,
                        true);
            }
        }
        // An X brace a storey in both end frames, between the columns' inner faces: its diagonals at about 55°, nearer the
        // 45° of practice than two tiers a storey would be (about 35°), and the cheapest X (the largest size binds: owner D3).
        double from = p.yTable;
        for (double level : levels) {
            for (double xe : new double[]{p.x0 + sc / 2, p.x1 - sc / 2})
                u.brace(new double[]{xe, p.z0 + sc}, new double[]{xe, p.zb - sc}, from + .2, level - BEAM, BRACE, Double.POSITIVE_INFINITY);
            from = level;
        }
        // Lateral guides: a band round each drum under the intermediate deck, struts to the faces' beams.
        double top = p.middle[p.middle.length - 1], y0 = top - BEAM, y1 = top - .15;
        for (double x : p.xd) {
            double band = p.jacket + .06;
            u.mesh.hoop(Frame.of(new double[]{x, 0, p.zc}, 1, 1), p.jacket, band, y0 - .05, y1, p.sides, Material.STEELWORK);
            for (int side = -1; side <= 1; side += 2) {
                double za = side < 0 ? p.z0 + FLANGE_WIDTH : p.zc + band, zz = side < 0 ? p.zc - band : p.zb - FLANGE_WIDTH;
                u.box(new double[]{x - .12, y0, za}, new double[]{x + .12, y1, zz}, ALL_FACES & ~(16 | 32), Material.STEELWORK, true);
            }
        }
    }

    /** The handrail round the table's top, its walking surface, open behind where the stair tower lands ({@code opening}, x from, x to). */
    static void tableRail(ProcessUnit u, DelayedCokerPlan p, double[] opening) {
        double level = p.yTable - Access.DECK, xa = p.x0 + EDGE, length = p.x1 - p.x0 - 2 * EDGE, depth = p.zb - p.z0 - 2 * EDGE, w = 1 - EDGE;
        u.rails(new Access.Line(new double[]{xa, 0, p.z0 + 1}, X, neg(Z), length), level, w, false, false);
        var back = new Access.Line(new double[]{xa, 0, p.zb - 1}, X, Z, length);
        u.rails(back, level, w, false, false, back.gap(opening));
        u.rails(new Access.Line(new double[]{p.x0 + 1, 0, p.z0 + EDGE}, Z, neg(X), depth), level, w, false, false);
        u.rails(new Access.Line(new double[]{p.x1 - 1, 0, p.z0 + EDGE}, Z, X, depth), level, w, false, false);
    }

    /**
     * The cutting deck: the whole floor of the structure in two strips meeting over the drums' axes, railed round, its
     * back rail open at the stair tower's {@code opening}; the derricks stand on it, the top unheading devices in it.
     */
    static void cuttingDeck(ProcessUnit u, DelayedCokerPlan p, double[] opening) {
        double g = Access.GAP, xa = p.x0 + EDGE, length = p.x1 - EDGE - xa;
        u.platform(new Access.Line(new double[]{xa, 0, p.zc + g}, X, neg(Z), length), p.cutLevel, p.zc + g - p.z0 - EDGE, Access.RAILED, true, true);
        var back = new Access.Line(new double[]{xa, 0, p.zc - g}, X, Z, length);
        u.platform(back, p.cutLevel, p.zb - EDGE - p.zc + g, Access.RAILED, true, true, back.gap(opening));
    }
}
