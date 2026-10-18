package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.ProcessUnit.Solid;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * Parts shared by the two hydroprocessing reactors ({@link TrickleBedReactorDesign}, {@link EbullatedBedReactorDesign};
 * NEW_UNITS_SURVEY_HYDROPROCESSING.md, 2026-10-18): a heavy-wall shell clad over its insulation with a hemispherical top
 * head, the forged top nozzle with its Class 2500 ring-joint flange pair and elbow, the square top deck on its girders,
 * the platforms and caged ladders round the shell, a pipe davit, a fireproofed skirt on its octagonal pedestal, flashing
 * bands and small fittings. Both stand on the vertical axis through (a, a) of the unit's frame; an angle runs from +x
 * towards +z (the back is π/2, the front -π/2).
 */
final class TrickleBedReactorParts {
    private TrickleBedReactorParts() {
    }

    static final double[] UP = {0, 1, 0}, DOWN = {0, -1, 0};
    /** The pedestal's height and its reach beyond the skirt; the skirt's fireproofing; flashing bands' section, m. */
    static final double PEDESTAL = .3, PEDESTAL_REACH = .25, FIREPROOFING = .05, BAND = 1 / 16.0;
    /** The square top deck's girders: depth and width, m. */
    static final double GIRDER_WIDTH = .15;
    /**
     * The square top deck's width beyond the cladding, m: room in front of the caged ladder coming up through it for a player
     * (0.86 m) and the handrail's band (survey: about 1 m beyond the OD).
     */
    static final double TOP_DECK = 1.2;
    /** Sides of the square top deck: where the caged ladder comes up through it. */
    static final int LEFT = 0, BACK = 1, RIGHT = 2;

    /** The horizontal unit vector at angle {@code t}. */
    static double[] radial(double t) {
        return new double[]{DeterministicMath.cos(t), 0, DeterministicMath.sin(t)};
    }

    /** The horizontal unit vector a quarter turn on from angle {@code t} (towards increasing angle). */
    static double[] tangent(double t) {
        return new double[]{-DeterministicMath.sin(t), 0, DeterministicMath.cos(t)};
    }

    /** The point {@code r} out from the axis through (a, a) at angle {@code t}, height {@code y}. */
    static double[] at(double a, double r, double t, double y) {
        return new double[]{a + r * DeterministicMath.cos(t), y, a + r * DeterministicMath.sin(t)};
    }

    /** The box round a straight line of radius {@code r} from {@code p} to {@code q}, a solid. */
    static Solid tube(double[] p, double[] q, double r) {
        return Solid.box(new double[]{Math.min(p[0], q[0]) - r, Math.min(p[1], q[1]) - r, Math.min(p[2], q[2]) - r},
                new double[]{Math.max(p[0], q[0]) + r, Math.max(p[1], q[1]) + r, Math.max(p[2], q[2]) + r});
    }

    /** The smaller angle between two directions, radians. */
    static double apart(double a, double b) {
        return ProcessUnit.apart(a, b);
    }

    // ---- Shell ------------------------------------------------------------------------------------------------------

    /**
     * A heavy-wall shell's cladding on the axis through (a, a): radius {@code wall} from the bottom tangent {@code yB} to
     * the top tangent {@code yT} and over the hemispherical top head, and the cladding's end under it down to the skirt's
     * radius {@code skirt}. It collides as drawn and encloses its inside from {@code yB} up.
     */
    static void cladShell(ProcessUnit u, double a, double yB, double yT, double wall, double skirt, int n) {
        var up = Frame.of(new double[]{a, 0, a}, 1, 1);
        u.mesh.prism(up, wall, yB, yT, n, Material.CLADDING);
        u.mesh.head(up, wall, yT, wall, n, Material.CLADDING);
        u.mesh.annulus(up, skirt, wall, yB, n, -1, Material.CLADDING);
        u.solids.add(Solid.rod(new double[]{a, 0, a}, 1, yB, yT, wall, wall));
        u.volumes.add(Solid.rod(new double[]{a, 0, a}, 1, yB, yT, .94 * wall, .94 * wall));
        u.round(a, a, yB, yT, wall, 1);
    }

    /**
     * The inside of a hemispherical bottom head of outer radius {@code r} under the tangent {@code yB}, enclosed: stacked
     * frustums inscribed at 0.94 of it (a solid's head only bulges up), within 0.2 m of the drawn head everywhere.
     */
    static void bottomHeadVolume(ProcessUnit u, double a, double yB, double r) {
        int bands = 6;
        double inner = .94 * r;
        for (int k = 0; k < bands; k++) {
            double p0 = Math.PI / 2 * k / bands, p1 = Math.PI / 2 * (k + 1) / bands;
            double y0 = yB - inner * DeterministicMath.cos(p0), y1 = yB - inner * DeterministicMath.cos(p1);
            u.volumes.add(Solid.cone(new double[]{a, 0, a}, 1, y0, y1, inner * DeterministicMath.sin(p0), inner * DeterministicMath.sin(p1), 0));
        }
    }

    /**
     * A hemispherical bottom head of outer radius {@code r} under the tangent {@code yB} on the axis through (a, a), seen
     * inside an open skirt: drawn in {@code material}, colliding as stacked frustums (a solid's head only bulges up) and
     * enclosing its inside ({@link #bottomHeadVolume}).
     */
    static void bottomHead(ProcessUnit u, double a, double yB, double r, int n, Material material) {
        u.mesh.head(Frame.of(new double[]{a, 0, a}, 1, -1), r, -yB, r, n, material);
        int bands = 6;
        for (int k = 0; k < bands; k++) {
            double p0 = Math.PI / 2 * k / bands, p1 = Math.PI / 2 * (k + 1) / bands;
            double y0 = yB - r * DeterministicMath.cos(p0), y1 = yB - r * DeterministicMath.cos(p1);
            u.solids.add(Solid.cone(new double[]{a, 0, a}, 1, y0, y1, r * DeterministicMath.sin(p0), r * DeterministicMath.sin(p1), 0));
        }
        bottomHeadVolume(u, a, yB, r);
    }

    /** The height of a hemispherical head's outside of radius {@code r} from the tangent at {@code rho} off its axis (0 beyond it). */
    static double rise(double r, double rho) {
        return rho >= r ? 0 : Math.sqrt(r * r - rho * rho);
    }

    /**
     * Flashing bands on the cladding (radius {@code wall}, {@code n} sides) at {@code heights}, a band {@link #BAND} square
     * stopping where a fitting passes the cladding: {@code fittings} hold {angle, half angle, low, high} each.
     */
    static void bands(ProcessUnit u, double a, double wall, int n, List<Double> heights, List<double[]> fittings) {
        double r = wall + BAND;
        for (double y : heights)
            for (int i = 0; i < n; i++) {
                double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n, mid = (a0 + a1) / 2;
                boolean crossed = false;
                for (var f : fittings) crossed |= y + BAND > f[2] && y < f[3] && apart(mid, f[0]) < f[1] + Math.PI / n;
                if (crossed) continue;
                u.mesh.flat(List.of(at(a, r, a0, y), at(a, r, a1, y), at(a, r, a1, y + BAND), at(a, r, a0, y + BAND)), radial(mid), Material.CLADDING);
                u.mesh.flat(List.of(at(a, wall, a0, y + BAND), at(a, wall, a1, y + BAND), at(a, r, a1, y + BAND), at(a, r, a0, y + BAND)), UP, Material.CLADDING);
                u.mesh.flat(List.of(at(a, wall, a0, y), at(a, wall, a1, y), at(a, r, a1, y), at(a, r, a0, y)), DOWN, Material.CLADDING);
            }
    }

    // ---- Top nozzle -------------------------------------------------------------------------------------------------

    /**
     * A heavy-wall reactor's top nozzle on the axis through (x, z), for a line {@code p} across (survey: B16.5 Class 2500
     * proportions): a forged neck 1.3 p across from under the cladding's apex {@code apex}; the ring-joint flange pair,
     * each flange 2.15 p across and 0.45 p thick, the ring showing between them; 12 to 24 studs of 0.16 p, their nuts and
     * ends above and below (the signature: kept at every size); a hub down to the line; a spool and a long-radius elbow
     * (1.5 p) turning to the front (frame -z). The run's centre line lies on the lowest cell centre at least {@code over}
     * high. Returns {the run's height, the elbow's end along z}.
     */
    static double[] topNozzle(ProcessUnit u, double x, double z, double apex, double p, double over) {
        double rn = .65 * p, rf = 1.075 * p, tf = Math.max(.04, .45 * p), gap = .01 + .01 * p, hub = .5 * p, bend = 1.5 * p;
        double y0 = apex + .25 + .3 * p, y1 = y0 + tf + gap, y2 = y1 + tf + hub;
        double run = Math.ceil(Math.max(y2 + .05 + bend, over) - .5) + .5, e0 = run - bend;
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        int nf = ProcessUnit.sides(2 * rf), np = ProcessUnit.sides(p);
        u.mesh.prism(up, rn, apex - .15, y0, nf, Material.SHELL);
        u.mesh.cylinder(up, rf, y0, y0 + tf, nf, Material.FLANGE);
        u.mesh.prism(up, .85 * rf, y0 + tf, y1, nf, Material.BOLT);
        u.mesh.cylinder(up, rf, y1, y1 + tf, nf, Material.FLANGE);
        studs(u, x, z, (rn + rf) / 2, y0, y1 + tf, p);
        u.mesh.frustum(up, rn, p / 2, y1 + tf, y2, np, Material.PIPE);
        if (e0 > y2 + .01) u.mesh.prism(up, p / 2, y2, e0, np, Material.PIPE);
        u.mesh.torus(new Frame(new double[]{x, e0, z - bend}, new double[]{-1, 0, 0}, new double[]{0, 0, 1}), bend, p / 2, 0, Math.PI / 2, 6, np, Material.PIPE);
        u.solids.add(Solid.rod(new double[]{x, 0, z}, 1, apex - .1, y1 + tf, rf, 0));
        u.solids.add(Solid.rod(new double[]{x, 0, z}, 1, y1 + tf, e0, p / 2, 0));
        u.solids.add(Solid.box(new double[]{x - p / 2, e0, z - bend}, new double[]{x + p / 2, run + p / 2, z + p / 2}));
        return new double[]{run, z - bend};
    }

    /**
     * A heavy flange pair's studs round the vertical axis through (x, z) on the bolt circle {@code circle}: nuts under the
     * lower flange's face {@code low} and over the upper's {@code high}, with the studs' ends past them; 12 to 24 of
     * them by the line {@code p} (Class 2500).
     */
    static void studs(ProcessUnit u, double x, double z, double circle, double low, double high, double p) {
        studs(u, x, z, circle, low, high, p < .25 ? 12 : p < .45 ? 16 : p < .65 ? 20 : 24, Math.max(.02, .16 * p));
    }

    /** {@code count} studs {@code d} across on a heavy flange pair's bolt circle, as {@link #studs(ProcessUnit, double, double, double, double, double, double)}. */
    static void studs(ProcessUnit u, double x, double z, double circle, double low, double high, int count, double d) {
        double nut = Math.max(Mesh.MIN_NUT, 1.6 * d) / Math.sqrt(3);
        for (int k = 0; k < count; k++) {
            double t = 2 * Math.PI * (k + .5) / count;
            for (int side = -1; side <= 1; side += 2) {
                var c = at(0, circle, t, side < 0 ? low : high);
                c[0] += x;
                c[2] += z;
                var f = new Frame(c, side < 0 ? DOWN : UP, new double[]{0, 0, 1});
                u.mesh.cylinder(f, nut, 0, d, 6, Material.BOLT);
                if (d >= .04) u.mesh.cylinder(f, d / 2, d, 1.5 * d, 6, Material.BOLT);
            }
        }
    }

    /**
     * A heavy flange pair across a straight line at {@code station} along frame {@code f} (its axis along the line):
     * two flanges 1.9 times the line {@code od} across, each 0.3 od thick with a 0.01 m joint, and the nuts of 8 to 16
     * studs on their outer faces.
     */
    static void heavyJoint(ProcessUnit u, Frame f, double od, double station) {
        double rf = .95 * od, tf = Math.max(.03, .3 * od), gap = .01;
        int n = ProcessUnit.sides(2 * rf), count = od < .3 ? 8 : od < .6 ? 12 : 16;
        u.mesh.cylinder(f, rf, station - gap / 2 - tf, station - gap / 2, n, Material.FLANGE);
        u.mesh.cylinder(f, rf, station + gap / 2, station + gap / 2 + tf, n, Material.FLANGE);
        double d = Math.max(.02, .12 * od), nut = Math.max(Mesh.MIN_NUT, 1.6 * d) / Math.sqrt(3), circle = (od / 2 + .05 + rf) / 2;
        if (rf - od / 2 - .05 < 1.6 * d) return;
        var axis = f.axis();
        for (int k = 0; k < count; k++) {
            double t = 2 * Math.PI * (k + .5) / count;
            for (int side = -1; side <= 1; side += 2) {
                var c = f.at(t, circle, station + side * (gap / 2 + tf));
                u.mesh.cylinder(new Frame(c, side < 0 ? neg(axis) : axis, f.ref()), nut, 0, d, 6, Material.BOLT);
            }
        }
    }

    /**
     * A 90° elbow of bend radius {@code bend} and radius {@code r} from {@code start}, heading {@code d0} there and
     * {@code d1} at its end (unit vectors at right angles); returns its end.
     */
    static double[] elbow(ProcessUnit u, double[] start, double[] d0, double[] d1, double bend, double r, int n, Material material) {
        double[] centre = plus(start, scale(d1, bend));
        u.mesh.torus(new Frame(centre, cross(d0, d1), neg(d1)), bend, r, 0, Math.PI / 2, 6, n, material);
        return plus(centre, scale(d0, bend));
    }

    // ---- Access -----------------------------------------------------------------------------------------------------

    /**
     * The caged ladders round a clad body (axis through (a, a), cladding radius {@code wall}) from the ground to each of
     * the platforms at {@code levels} (whole metres, rising) and on to the top deck at {@code top}, and those platforms:
     * a full ring where {@code rings} says so (open where its ladder comes up), else a part ring from where its ladder
     * comes up round to the next one, closed beyond it. The ladder onto the top deck stands at angle {@code up}, the
     * others alternate between {@code other} and {@code up} going down, so each starts on the deck its predecessor reached,
     * a quarter or half turn on (the column's pattern, {@link ColumnStructure#plan}). Decks are
     * {@link ColumnStructure#MIN_DECK} wide: a player stands on one in front of the ladder that starts there.
     */
    static void access(ProcessUnit u, double a, double wall, List<Integer> levels, List<Boolean> rings, int top, double up, double other) {
        access(u, a, wall, levels, rings, top, up, other, Double.NaN);
    }

    /**
     * {@link #access(ProcessUnit, double, double, List, List, int, double, double)} with each part ring passing the angle
     * {@code via} on its way from one ladder to the next (half a turn apart: either way round); NaN, the shorter way.
     */
    static void access(ProcessUnit u, double a, double wall, List<Integer> levels, List<Boolean> rings, int top, double up, double other, double via) {
        double width = ColumnStructure.MIN_DECK, edge = wall + width, rung = wall + Access.STANDOFF;
        double across = Access.CAGE_HALF + .2, out = wall + Access.STANDOFF - .05, clear = DeterministicMath.acos(out / Math.sqrt(out * out + across * across));
        int m = levels.size();
        for (int j = 0; j <= m; j++) {
            double t = (m - j) % 2 == 0 ? up : other;
            double base = j == 0 ? 0 : levels.get(j - 1) + Access.DECK, landing = (j == m ? top : levels.get(j)) + Access.DECK + Access.LANDING;
            u.ladder(a + rung * DeterministicMath.cos(t), a + rung * DeterministicMath.sin(t), radial(t), Access.STANDOFF + .12, base, landing, false);
        }
        for (int j = 0; j < m; j++) {
            double in = (m - j) % 2 == 0 ? up : other, next = towards(in, (m - j - 1) % 2 == 0 ? up : other, via);
            int level = levels.get(j);
            if (rings.get(j)) u.platform(new Access.Arc(a, a, wall, in + clear, in + 2 * Math.PI - clear, edge, Access.Arc.step(edge)), level, width, false, false);
            else if (in > next) u.platform(new Access.Arc(a, a, wall, next - clear, in - clear, edge, Access.Arc.step(edge)), level, width, true, false);
            else u.platform(new Access.Arc(a, a, wall, in + clear, next + clear, edge, Access.Arc.step(edge)), level, width, false, true);
        }
    }

    /** {@code next} moved by whole turns so that going from {@code in} to it, at most half a turn, passes {@code via} (NaN: unmoved). */
    private static double towards(double in, double next, double via) {
        if (Double.isNaN(via)) return next;
        for (int k = -1; k <= 1; k++) {
            double to = next + 2 * Math.PI * k, lo = Math.min(in, to), hi = Math.max(in, to);
            if (hi - lo > Math.PI + 1e-9) continue;
            for (int w = -2; w <= 2; w++) {
                double v = via + 2 * Math.PI * w;
                if (v > lo && v < hi) return to;
            }
        }
        return next;
    }

    /**
     * Platform levels for climbs from the ground through the {@code wanted} levels (rising) to the top deck at {@code top}:
     * the wanted ones (full rings), and rest platforms (part rings) splitting any climb longer than
     * {@link ColumnStructure#MAX_CLIMB} evenly (old OSHA 1910.27). Fills {@code levels} and {@code rings}.
     */
    static void levels(List<Integer> wanted, int top, List<Integer> levels, List<Boolean> rings) {
        var all = new ArrayList<>(wanted);
        all.add(top);
        int previous = 0;
        for (int i = 0; i < all.size(); i++) {
            int level = all.get(i), gap = level - previous, parts = (gap + ColumnStructure.MAX_CLIMB - 1) / ColumnStructure.MAX_CLIMB;
            for (int j = 1; j < parts; j++) {
                levels.add(previous + (int) Math.round(gap * j / (double) parts));
                rings.add(false);
            }
            if (i + 1 < all.size()) {
                levels.add(level);
                rings.add(true);
            }
            previous = level;
        }
    }

    /**
     * A square top deck round a clad body (axis through (a, a), cladding radius {@code wall}) at {@code level}: grating
     * {@link #TOP_DECK} wide beyond the cladding's tangent square, its corners between the square and the body filled;
     * four girders {@code girder} deep under the square, bearing on the shell at its sides; a handrail round its outer
     * edge; a hole in the run on {@code side} ({@link #LEFT} +x, {@link #BACK}, {@link #RIGHT} -x) where the caged
     * ladder comes up through it.
     */
    static void squareDeck(ProcessUnit u, double a, double wall, int level, int side, double girder) {
        double w = TOP_DECK, h = wall, outer = wall + w, g = Access.CAGE_HALF + .2, gap = Access.GAP, y0 = level, y1 = level + Access.DECK;
        var front = new Access.Line(new double[]{a - outer, 0, a - h}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, 2 * outer);
        var back = new Access.Line(new double[]{a + outer, 0, a + h}, new double[]{-1, 0, 0}, new double[]{0, 0, 1}, 2 * outer);
        var left = new Access.Line(new double[]{a + h, 0, a - h - gap}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, 2 * (h + gap));
        var right = new Access.Line(new double[]{a - h, 0, a + h + gap}, new double[]{0, 0, -1}, new double[]{-1, 0, 0}, 2 * (h + gap));
        deck(u, front, level, w);
        for (var run : new Access.Line[]{back, left, right}) {
            boolean holed = run == back ? side == BACK : run == left ? side == LEFT : side == RIGHT;
            double s = run == back ? outer : h + gap, length = run.length();
            if (!holed) deck(u, run, level, w);
            else {
                deck(u, Access.part(run, 0, s - g), level, w);
                deck(u, Access.part(run, s + g, length), level, w);
            }
        }
        // The handrail round the outer edge, each side the whole square's length.
        u.railing(front, level, w);
        u.railing(back, level, w);
        u.railing(new Access.Line(new double[]{a + h, 0, a - outer}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, 2 * outer), level, w);
        u.railing(new Access.Line(new double[]{a - h, 0, a + outer}, new double[]{0, 0, -1}, new double[]{-1, 0, 0}, 2 * outer), level, w);
        // The girders under the deck's inner edge, the one where the ladder comes up open round its rails.
        double in = h + gap, gw = GIRDER_WIDTH, lo = y0 - girder, keep = Access.LADDER_HALF + .12;
        girder(u, new double[]{a - outer, lo, a - in - gw}, new double[]{a + outer, y0, a - in}, Double.NaN, 0);
        girder(u, new double[]{a - outer, lo, a + in}, new double[]{a + outer, y0, a + in + gw}, side == BACK ? a : Double.NaN, keep);
        girder(u, new double[]{a + in, lo, a - in}, new double[]{a + in + gw, y0, a + in}, side == LEFT ? a : Double.NaN, keep);
        girder(u, new double[]{a - in - gw, lo, a - in}, new double[]{a - in, y0, a + in}, side == RIGHT ? a : Double.NaN, keep);
        // The corners between the tangent square and the body: grating fans, colliding as the square's corner boxes.
        int steps = 6;
        for (int q = 0; q < 4; q++) {
            double t0 = -Math.PI / 2 + q * Math.PI / 2, sx = q == 0 || q == 1 ? 1 : -1, sz = q == 1 || q == 2 ? 1 : -1;
            double[] corner = {a + sx * in, 0, a + sz * in};
            for (int k = 0; k < steps; k++) {
                double ta = t0 + Math.PI / 2 * k / steps, tb = t0 + Math.PI / 2 * (k + 1) / steps;
                for (double y : new double[]{y0, y1}) {
                    double[] c = {corner[0], y, corner[2]};
                    u.mesh.flat(List.of(c, at(a, in, ta, y), at(a, in, tb, y)), y == y1 ? UP : DOWN, Material.GRATING);
                }
            }
            double c = .7071 * in;
            u.solids.add(ProcessUnit.span(new double[]{a + sx * c, y0, a + sz * c}, new double[]{a + sx * in, y1, a + sz * in}));
        }
    }

    /**
     * A girder from {@code lo} to {@code hi}, colliding; where {@code at} is a number, open across ±{@code keep} round it
     * along its length (a ladder's rails pass it).
     */
    private static void girder(ProcessUnit u, double[] lo, double[] hi, double at, double keep) {
        int along = hi[0] - lo[0] > hi[2] - lo[2] ? 0 : 2;
        if (Double.isNaN(at)) {
            u.box(lo, hi, Material.STEELWORK, true);
            return;
        }
        double[] a1 = hi.clone(), b0 = lo.clone();
        a1[along] = at - keep;
        b0[along] = at + keep;
        u.box(lo, a1, Material.STEELWORK, true);
        u.box(b0, hi, Material.STEELWORK, true);
    }

    /** A deck without its own handrail (the square deck's rail runs round its whole edge): grating, solid, outline. */
    static void deck(ProcessUnit u, Access.Run run, int level, double width) {
        Access.deck(u.mesh, run, level, width);
        u.decks.add(new ProcessUnit.Deck(run, level, width, false, false));
        double top = level + Access.DECK, length = run.length();
        u.solids.add(ProcessUnit.span(run.at(0, Access.GAP, level), run.at(length, width, top)));
        int count = Math.max(2, (int) Math.ceil(length / .5));
        for (int j = 0; j < count; j++) u.segment(run.at(length * j / count, width, top), run.at(length * (j + 1) / count, width, top));
    }

    /**
     * A pipe davit on the top deck at angle {@code t}, {@code r} from the axis through (a, a), from the deck {@code base}
     * to {@code top}: the mast on its base plate, the jib in over the top nozzle and 0.2 m past the axis with its knee
     * brace, the cable and the hook block (the column's davit, {@code ColumnStructure}).
     */
    static void davit(ProcessUnit u, double a, double t, double r, double base, double top) {
        double[] dir = radial(t), side = tangent(t), foot = at(a, r, t, base);
        double reach = -.2;
        var f = new Frame(foot, UP, dir);
        u.mesh.prism(f, .1, 0, top - base, 8, Material.HANDRAIL);
        u.mesh.annulus(f, 0, .1, top - base, 8, 1, Material.HANDRAIL);
        u.mesh.block(at(a, r, t, base + .02), scale(dir, .18), new double[]{0, .02, 0}, scale(side, .18), Material.STEELWORK);
        u.mesh.bar(at(a, r + .1, t, top - .1), at(a, reach, t, top - .1), .14, UP, Material.HANDRAIL);
        u.mesh.bar(at(a, r, t, top - 1.1), at(a, r - .6 * (r - reach), t, top - .17), .07, side, Material.HANDRAIL);
        u.mesh.bar(at(a, reach + .05, t, top - .17), at(a, reach + .05, t, top - 1), .025, dir, Material.STEELWORK);
        u.mesh.block(at(a, reach + .05, t, top - 1.07), scale(dir, .07), new double[]{0, .07, 0}, scale(side, .07), Material.STEELWORK);
        u.solids.add(Solid.rod(foot, 1, base, top, .1, 0));
    }

    // ---- Supports ---------------------------------------------------------------------------------------------------

    /**
     * An octagonal concrete pedestal (pile cap) of apothem {@code apothem} round the axis through (a, a), {@code h} high,
     * its faces square to the frame; its top reaches every cell under it.
     */
    static void pedestal(ProcessUnit u, double a, double apothem, double h) {
        double rc = apothem / DeterministicMath.cos(Math.PI / 8);
        var top = new ArrayList<double[]>();
        for (int k = 0; k < 8; k++) {
            double t0 = -Math.PI / 8 + k * Math.PI / 4, t1 = t0 + Math.PI / 4;
            u.mesh.flat(List.of(at(a, rc, t0, 0), at(a, rc, t1, 0), at(a, rc, t1, h), at(a, rc, t0, h)), radial(t0 + Math.PI / 8), Material.CONCRETE);
            top.add(at(a, rc, t0, h));
        }
        u.mesh.flat(top, UP, Material.CONCRETE);
        u.solids.add(Solid.rod(new double[]{a, 0, a}, 1, 0, h, rc, 0));
    }

    /** The circumradius of a {@link #pedestal} of apothem {@code apothem}. */
    static double pedestalCorner(double apothem) {
        return apothem / DeterministicMath.cos(Math.PI / 8);
    }

    /**
     * A skirt's base ring on the pedestal and its anchor chairs (one per 1.5 m of circumference or so, in fours; the
     * drum's count, {@link EquipmentShape#anchorChairs}), the skirt's outer radius {@code r}, its foot at {@code y}; none in
     * the skirt's {@code openings} ({centre angle, half width (m)}).
     */
    static void baseRing(ProcessUnit u, double a, double r, double y, double diameter, int n, double[]... openings) {
        var up = Frame.of(new double[]{a, 0, a}, 1, 1);
        u.mesh.ring(up, r, r + .1, y, y + .06, n, 2, Material.SUPPORT);
        int chairs = 4 * (int) Math.max(1, Math.round(Math.PI * diameter / 6));
        for (int k = 0; k < chairs; k++) {
            double t = 2 * Math.PI * (k + .5) / chairs;
            boolean open = false;
            for (var o : openings) open |= apart(t, o[0]) < DeterministicMath.asin(Math.min(1, o[1] / r)) + .12 / r;
            if (open) continue;
            u.mesh.block(at(a, r + .1, t, y + .21), scale(radial(t), .1), new double[]{0, .15, 0}, scale(tangent(t), .12), Material.SUPPORT);
            u.mesh.cylinder(Frame.of(at(a, r + .12, t, 0), 1, 1), .03, y + .36, y + .42, 6, Material.BOLT);
        }
    }

    /**
     * A skirt of outer radius {@code r} on the axis through (a, a) from {@code y0} to {@code y1}, {@code n} sides, fireproofed
     * (concrete faced); its plate {@code thick} through. Open from its foot up to {@code top} over the angles of each of
     * {@code openings} ({centre angle, half width (m)}), where its inside face, the openings' jambs and lintels are drawn;
     * closed (no openings), its inside is never seen and is not drawn. It collides as drawn.
     */
    static void skirt(ProcessUnit u, double a, double r, double thick, double y0, double y1, double top, int n, double[]... openings) {
        double[] axis = {a, 0, a};
        var outside = new Radial(axis, UP);
        var inside = new Inward(axis, UP);
        double ri = r - thick;
        boolean open = openings.length > 0;
        var cut = new boolean[n];
        for (int i = 0; i < n; i++) {
            double mid = 2 * Math.PI * (i + .5) / n;
            for (var o : openings) cut[i] |= apart(mid, o[0]) < DeterministicMath.asin(Math.min(1, o[1] / r));
        }
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n, lo = cut[i] ? top : y0;
            u.mesh.add(List.of(at(a, r, a0, lo), at(a, r, a1, lo), at(a, r, a1, y1), at(a, r, a0, y1)), new double[]{a, (lo + y1) / 2, a}, outside, Material.CONCRETE, null, null);
            if (open)
                u.mesh.add(List.of(at(a, ri, a0, lo), at(a, ri, a1, lo), at(a, ri, a1, y1), at(a, ri, a0, y1)), at(a, 2 * r, (a0 + a1) / 2, (lo + y1) / 2), inside,
                        Material.CONCRETE, null, null);
            if (cut[i]) u.mesh.flat(List.of(at(a, ri, a0, top), at(a, ri, a1, top), at(a, r, a1, top), at(a, r, a0, top)), DOWN, Material.CONCRETE);
            int prev = (i + n - 1) % n;
            if (cut[i] != cut[prev]) {
                // A jamb at this facet's edge, facing into the opening.
                double[] facing = cut[i] ? tangent(a0) : neg(tangent(a0));
                u.mesh.flat(List.of(at(a, ri, a0, y0), at(a, r, a0, y0), at(a, r, a0, top), at(a, ri, a0, top)), facing, Material.CONCRETE);
            }
        }
        if (!open) {
            u.solids.add(Solid.rod(axis, 1, y0, y1, r, 0));
            return;
        }
        u.solids.add(Solid.ring(a, a, top, y1, ri, r, 0, 0));
        // The walls between the openings, each from one opening's edge round to the next's.
        for (int i = 0; i < n; i++) {
            if (cut[i] || !cut[(i + n - 1) % n]) continue;
            int j = i;
            while (!cut[(j + 1) % n]) j++;
            u.solids.add(Solid.ring(a, a, y0, top, ri, r, 2 * Math.PI * i / n, 2 * Math.PI * (j + 1) / n));
        }
    }

    // ---- Fittings ---------------------------------------------------------------------------------------------------

    /**
     * A control valve on a vertical line of radius {@code r} at (x, y, z): a cast body between two flanges, its yoke out
     * along {@code out} to the diaphragm actuator (the quench valve stations, survey).
     */
    static void valve(ProcessUnit u, double x, double y, double z, double r, double[] out) {
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        double body = 1.6 * r + .03, fr = Math.max(body + .02, PipeSizes.flange(2 * r) / 2), cap = .1 + r;
        u.mesh.cylinder(up, body, y - body, y + body, 12, Material.MACHINE);
        u.mesh.cylinder(up, fr, y - body - .04, y - body, 12, Material.FLANGE);
        u.mesh.cylinder(up, fr, y + body, y + body + .04, 12, Material.FLANGE);
        double[] c0 = {x + out[0] * body, y, z + out[2] * body}, c1 = {x + out[0] * (body + .25), y, z + out[2] * (body + .25)};
        u.mesh.bar(c0, c1, .04, UP, Material.ACTUATOR);
        var f = new Frame(c1, out, UP);
        u.mesh.cylinder(f, cap, 0, .1, 12, Material.ACTUATOR);
        u.mesh.cylinder(f, .04, .1, .16, 8, Material.ACTUATOR);
        u.solids.add(ProcessUnit.span(new double[]{x - body, y - body, z - body}, new double[]{x + body, y + body, z + body}));
        double[] far = {c1[0] + out[0] * .16, y, c1[2] + out[2] * .16};
        u.solids.add(ProcessUnit.span(new double[]{Math.min(c0[0], far[0]) - cap * Math.abs(out[2]), y - cap, Math.min(c0[2], far[2]) - cap * Math.abs(out[0])},
                new double[]{Math.max(c0[0], far[0]) + cap * Math.abs(out[2]), y + cap, Math.max(c0[2], far[2]) + cap * Math.abs(out[0])}));
    }

    /** A thermowell on the cladding (radius {@code wall}) at angle {@code t} and height {@code y}: a short stub and its junction head. */
    static void thermowell(ProcessUnit u, double a, double wall, double t, double y) {
        double[] dir = radial(t);
        u.mesh.cylinder(new Frame(at(a, wall - .02, t, y), dir, UP), .03, 0, .14, 8, Material.FLANGE);
        u.mesh.block(at(a, wall + .19, t, y), scale(dir, .07), new double[]{0, .08, 0}, scale(tangent(t), .07), Material.MOTOR);
        u.solids.add(ProcessUnit.span(at(a, wall, t, y - .08), at(a, wall + .26, t, y + .08)));
    }

    /**
     * A blind-flanged nozzle {@code od} across leaving a skirt (radius {@code r}) at angle {@code t} and height {@code y},
     * 45° down and out through a sleeve (a catalyst dump nozzle, survey), {@code length} beyond the skirt.
     */
    static void dumpNozzle(ProcessUnit u, double a, double r, double t, double y, double od, double length) {
        double c = Math.sqrt(.5);
        double[] dir = {c * DeterministicMath.cos(t), -c, c * DeterministicMath.sin(t)}, start = at(a, r - .05, t, y + .05);
        var f = new Frame(start, dir, tangent(t));
        double fr = PipeSizes.flange(od) / 2;
        u.mesh.prism(f, od / 2, 0, length, ProcessUnit.sides(od), Material.PIPE);
        u.mesh.cylinder(f, od / 2 + .04, .02, .1, 12, Material.SUPPORT);
        u.mesh.cylinder(f, fr, length, length + .04, 12, Material.FLANGE);
        u.mesh.cylinder(f, fr, length + .04, length + .1, 12, Material.FLANGE);
        var end = f.along(length + .05);
        u.solids.add(ProcessUnit.span(new double[]{end[0] - fr, end[1] - fr, end[2] - fr}, new double[]{end[0] + fr, end[1] + fr, end[2] + fr}));
    }
}
