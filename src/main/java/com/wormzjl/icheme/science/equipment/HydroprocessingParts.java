package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.Mesh.Frame;
import com.wormzjl.icheme.science.equipment.Mesh.Material;
import com.wormzjl.icheme.science.equipment.ProcessUnit.Ring;
import com.wormzjl.icheme.science.equipment.ProcessUnit.Solid;

import java.util.List;

import static com.wormzjl.icheme.science.equipment.Access.around;
import static com.wormzjl.icheme.science.equipment.ProcessUnit.outward;
import static com.wormzjl.icheme.science.equipment.ProcessUnit.tangential;

/**
 * Parts the two hydroprocessing reactors share, the trickle-bed and the ebullated-bed reactor (NEW_UNITS_PLAN.md;
 * {@code NEW_UNITS_SURVEY_HYDROPROCESSING.md} sections 1 and 2): the clad shell on its skirt,
 * the platforms moved off the bottom tangent, the square top deck the caged ladder comes up through on its girders, the
 * forged top nozzle with its Class 2500 flange pair, the lifting trunnions and the cladding's flashing bands. Valves,
 * thermowells, skirts and studs are the kit's ({@link ProcessUnit#valve}, {@link ProcessUnit#thermowell},
 * {@link ProcessUnit#skirt}, {@link ProcessUnit#studs}).
 */
final class HydroprocessingParts {
    /** Flashing bands' section, m. */
    static final double BAND = 1 / 16.0;
    /**
     * The square top deck's width beyond the cladding, m: room in front of the caged ladder coming up through it for a player
     * (0.86 m) and the handrail's band (survey: about 1 m beyond the OD); its girders' width.
     */
    static final double TOP_DECK = 1.2, GIRDER_WIDTH = .15;
    /** The bottom ports' row: the cell centre 1.5 m over grade, clear of the base ring (survey: 0.8 to 1.2 m, 1.5 m). */
    static final double PORT_ROW = 1.5;
    private static final double[] UP = {0, 1, 0};

    private HydroprocessingParts() {
    }

    /** The insulation under the aluminium cladding for an inside diameter {@code d}, m (survey). */
    static double insulation(double d) {
        return .1 + .02 * d;
    }

    /** The top deck's girders' depth for an inside diameter {@code d}, m. */
    static double girder(double d) {
        return .3 + .04 * d;
    }

    /**
     * The clad shell (radius {@code wall} about the vertical axis through (a, a), {@code n} sides) from the bottom tangent
     * {@code yB} to the top {@code yT} and over its clad top head, the cladding's end over the skirt of radius
     * {@code skirt}, the vessel's solid and volume.
     */
    static void cladShell(ProcessUnit u, double a, double wall, double skirt, double yB, double yT, int n) {
        double[] axis = {a, 0, a};
        var up = Frame.of(axis, 1, 1);
        u.mesh.prism(up, wall, yB, yT, n, Material.CLADDING);
        u.mesh.head(up, wall, yT, wall, n, Material.CLADDING);
        u.mesh.annulus(up, skirt, wall, yB, n, -1, Material.CLADDING);
        u.solids.add(Solid.rod(axis, 1, yB, yT, wall, wall));
        u.volumes.add(Solid.rod(axis, 1, yB, yT, .94 * wall, .94 * wall));
        u.round(a, a, yB, yT, wall, 1);
    }

    /**
     * Moves a platform level off the bottom tangent {@code tangent} (where its brackets would hang off the skirt's top):
     * a metre lower onto the skirt, or a metre higher where the climb over it would then be too long; {@code level} is
     * the top deck's.
     */
    static void offTangent(List<Integer> levels, int tangent, int level) {
        for (int j = 0; j < levels.size(); j++) {
            if (levels.get(j) != tangent) continue;
            int below = j == 0 ? 0 : levels.get(j - 1), above = j + 1 < levels.size() ? levels.get(j + 1) : level;
            if (above - (tangent - 1) <= Access.MAX_CLIMB && tangent - 1 > below) levels.set(j, tangent - 1);
            else if (tangent + 1 - below <= Access.MAX_CLIMB && tangent + 1 < above) levels.set(j, tangent + 1);
        }
    }

    /** How many of {@code levels} (rising) lie under the bottom tangent {@code yB}: their decks are laid round the skirt. */
    static int underTangent(List<Integer> levels, double yB) {
        int low = 0;
        while (low < levels.size() && levels.get(low) < yB) low++;
        return low;
    }

    /**
     * The platforms and ladders round the reactor (axis through (a, a)): the {@code low} rings under the bottom tangent laid
     * round the skirt (radius {@code skirt}), as much wider as the cladding (radius {@code wall}) stands proud of it, their
     * outer edge in line with the rest, so their brackets bear on it, the ladders up to them standing off the skirt; the
     * rest round the cladding ({@link ProcessUnit#ringAccess}); the caged ladder at angle {@code t} from the highest ring's
     * deck (the ground without one) up through the {@link #topDeck} at {@code level}, its girders {@code girder} deep.
     */
    static void access(ProcessUnit u, double a, double wall, double skirt, List<Integer> levels, int low, List<Ring> rings, double t, int level, double girder) {
        int m = levels.size();
        u.ringAccess(a, a, skirt, Access.MIN_DECK + wall - skirt, 0, rings.subList(0, low));
        u.ringAccess(a, a, wall, Access.MIN_DECK, low == 0 ? 0 : levels.get(low - 1) + Access.DECK, rings.subList(low, m));
        u.verticalLadder(a, a, wall, t, m == 0 ? 0 : levels.getLast() + Access.DECK, level + Access.DECK + Access.LANDING, false);
        topDeck(u, a, wall, level, t, girder);
    }

    /**
     * The square top deck round the cladding (radius {@code wall}, axis through (a, a)) at {@code level}: grating
     * {@link #TOP_DECK} wide beyond the cladding's tangent square, its corners round the body filled, the caged ladder at
     * angle {@code t} (0, π/2 or π) coming up through a hole in the band on its side, the handrail running on over the
     * hole's outer side (the kit's {@link ProcessUnit#squareDeck}). Four girders {@code girder} deep under the square's
     * inner edge bear on the shell at its sides, the one on the ladder's side open round its rails.
     */
    static void topDeck(ProcessUnit u, double a, double wall, int level, double t, double girder) {
        double in = wall + Access.GAP, outer = wall + TOP_DECK, hole = Access.CAGE_HALF + .2, keep = Access.LADDER_HALF + .12, lo = level - girder, gw = GIRDER_WIDTH;
        double[] out = outward(t);
        int sx = (int) Math.round(out[0]), sz = (int) Math.round(out[2]);
        u.squareDeck(a - outer, a + outer, a - outer, a + outer, level, sz != 0 ? 0 : 2, a, in, new double[]{a}, in, 24, Access.BRACKETS | Access.RAILED, null,
                new double[]{sx + sz, a - hole, a + hole});
        // The girders {x0, z0, x1, z1}: front and back the square's whole length, left and right between them.
        double[][] girders = {{a - outer, a - in - gw, a + outer, a - in}, {a - outer, a + in, a + outer, a + in + gw}, {a + in, a - in, a + in + gw, a + in},
                {a - in - gw, a - in, a - in, a + in}};
        int side = sz > 0 ? 1 : sx > 0 ? 2 : 3;
        for (int k = 0; k < 4; k++) {
            double[] p0 = {girders[k][0], lo, girders[k][1]}, p1 = {girders[k][2], level, girders[k][3]};
            if (k != side) {
                u.beam(p0, p1);
                continue;
            }
            int run = k < 2 ? 0 : 2;
            double[] a1 = p1.clone(), b0 = p0.clone();
            a1[run] = a - keep;
            b0[run] = a + keep;
            u.beam(p0, a1);
            u.beam(b0, p1);
        }
    }

    /**
     * A heavy-wall reactor's top nozzle on the axis through (x, z), for a line {@code p} across (survey: B16.5 Class 2500
     * proportions): a forged neck 1.3 p across from under the cladding's apex {@code apex}; the ring-joint flange pair,
     * each flange 2.15 p across and 0.45 p thick, the ring showing between them; 12 to 24 studs of 0.16 p, their nuts and
     * ends above and below ({@link ProcessUnit#studs}: the signature, kept at every size); a hub down to the line; a spool
     * and a long-radius elbow (1.5 p) turning to the front (frame -z). The run's centre line lies on the lowest cell centre
     * where a line at a pipe's largest passes over the rails of the top deck walked at {@code deck} (owner decision
     * 2026-10-21: a 1 m pipe has a 1 m root, other components give way). Returns {the run's height, the elbow's end along z}.
     */
    static double[] topNozzle(ProcessUnit u, double x, double z, double apex, double p, double deck) {
        double rn = .65 * p, rf = 1.075 * p, tf = Math.max(.04, .45 * p), gap = .01 + .01 * p, hub = .5 * p, bend = 1.5 * p;
        double y0 = apex + .25 + .3 * p, y1 = y0 + tf + gap, y2 = y1 + tf + hub, over = deck + Access.FENCE + .05 + PipeSizes.MAX_OUTSIDE / 2;
        double run = Math.ceil(Math.max(y2 + .05 + bend, over) - .5) + .5, e0 = run - bend;
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        int nf = ProcessUnit.sides(2 * rf), np = ProcessUnit.sides(p);
        u.mesh.prism(up, rn, apex - .15, y0, nf, Material.SHELL);
        u.mesh.cylinder(up, rf, y0, y0 + tf, nf, Material.FLANGE);
        u.mesh.prism(up, .85 * rf, y0 + tf, y1, nf, Material.BOLT);
        u.mesh.cylinder(up, rf, y1, y1 + tf, nf, Material.FLANGE);
        u.studs(x, z, (rn + rf) / 2, y0, y1 + tf, p);
        u.mesh.frustum(up, rn, p / 2, y1 + tf, y2, np, Material.PIPE);
        if (e0 > y2 + .01) u.mesh.prism(up, p / 2, y2, e0, np, Material.PIPE);
        u.elbow(new double[]{x, e0, z}, UP, new double[]{0, 0, -1}, bend, p / 2, np, 6, Material.PIPE);
        u.solids.add(Solid.rod(new double[]{x, 0, z}, 1, apex - .1, y1 + tf, rf, 0));
        u.solids.add(Solid.rod(new double[]{x, 0, z}, 1, y1 + tf, e0, p / 2, 0));
        u.solids.add(Solid.box(new double[]{x - p / 2, e0, z - bend}, new double[]{x + p / 2, run + p / 2, z + p / 2}));
        return new double[]{run, z - bend};
    }

    /**
     * Two lifting trunnions (0.15 D across, 0.12 D proud, D the inside diameter {@code d}) on the cladding (radius
     * {@code wall}, axis through (a, a)) at the {@code angles} under the top deck's girders ({@code girder} deep under
     * {@code level}); each height band they take is added to {@code fittings}.
     */
    static void trunnions(ProcessUnit u, double a, double wall, double d, int level, double girder, List<double[]> fittings, double... angles) {
        double trunnion = Math.max(.06, .075 * d), proud = Math.max(.15, .12 * d), yTr = level - girder - trunnion - .15, rt = trunnion + .04;
        for (double t : angles) {
            var f = new Frame(around(a, a, wall - .05, t, yTr), outward(t), UP);
            u.mesh.prism(f, trunnion, 0, proud + .05, 12, Material.SHELL);
            u.mesh.cylinder(f, rt, proud + .05, proud + .1, 12, Material.SHELL);
            u.solids.add(Solid.ring(a, a, yTr - rt, yTr + rt, wall, wall + proud + .1, t - rt / wall, t + rt / wall));
            fittings.add(new double[]{yTr - trunnion - .1, yTr + trunnion + .1});
        }
    }

    /**
     * Flashing bands every 3.5 m on the cladding (frame {@code up}, radius {@code wall}, {@code n} sides) from 1.5 m over
     * the bottom tangent {@code yB} to half a metre under the top {@code yT}, off the top deck's girders, the platforms'
     * brackets and the {@code fittings}' height bands.
     */
    static void bands(ProcessUnit u, Frame up, double wall, int n, double yB, double yT, int level, double girder, List<Integer> levels, List<double[]> fittings) {
        for (double y = yB + 1.5; y < yT - .5; y += 3.5) {
            boolean clear = y < level - girder - .1 && clear(y, levels, .75, .1);
            for (var f : fittings) clear &= y + BAND < f[0] || y > f[1];
            if (clear) u.mesh.hoop(up, wall, wall + BAND, y, y + BAND, n, Material.CLADDING);
        }
    }

    /** Whether height {@code y} keeps {@code below} under and {@code above} over every platform deck at {@code levels}. */
    static boolean clear(double y, List<Integer> levels, double below, double above) {
        for (int lv : levels) if (y > lv - below && y < lv + Access.DECK + above) return false;
        return true;
    }
}
