package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * A distillation column's auxiliaries beside it, drawn only (owner request and decisions 2026-10-16, batch
 * 2026-10-15-custom-equipment-rendering, {@code COLUMN_AUXILIARIES_PLAN.md}): the overhead condenser with its reflux drum,
 * a steam ejector before it, a pumparound cooler per pumparound, a kettle reboiler, their pumps and the lines between
 * them and the column. Sizes scale with the column ("for now"); the units lie on their sides.
 *
 * <p>Physics behind the layout (D 3 m reference): the overhead vapour, about 24 kg/s and 7 MW, falls through the
 * condenser into the reflux drum below it, so the condenser stands over the drum; the reflux pump at grade lifts the
 * reflux to tray 1 and each pumparound pump lifts its cooled liquid back above its draw. A water condenser needs about
 * 490 m², a 1.25 m shell of 6 m tubes; its shell grows as D^(2/3). An air cooler at a 35 K air rise and 3 m/s takes
 * about 120 kW per m² of face, so its face is 8.5 times the column's cross-section, in bays of up to 5 × 12 m. A kettle
 * is fed by gravity from the sump and returns its vapour under the bottom tray, so the column's skirt must lift the sump
 * over it ({@link #minimumSkirt}).
 *
 * <p>Layout, in the column's frame ({@link EquipmentShape}: axis at (a, y, a), front -z, the right side -x):
 * <ul>
 *   <li>a steel structure on the right, the side of the draws: units lying along x centred on the column's z, their
 *   channels at the outer end. The reflux drum on the first floor, a water condenser on the second over it; the
 *   pumparound coolers at grade in lanes beside the column's line, each with its pump nearer the column;</li>
 *   <li>a pipe bay between the column and the structure: every line leaves the column's right side at z = a, steps
 *   across to its lane just outside the column's square and drops in its lane; the reflux rises in the middle lane, the
 *   overhead's down-leg (with the ejector, upright in it) beyond;</li>
 *   <li>the overhead line rises from the top head, crosses to the down-leg and runs over the structure's top beams to
 *   the condenser's inlet, or on to an air cooler on its own stand 1 m beyond the structure (owner decision 1);</li>
 *   <li>a kettle reboiler at grade on the left, along z, fed from the bottoms nozzle at the front and returning its
 *   vapour to a nozzle on the left.</li>
 * </ul>
 * Utility and product ends (cooling water, steam, distillate, bottoms, a drum's water boot) end in blind flanges.
 *
 * <p>Built once per shape in the column's frame ({@link #of}); {@link ShapeRasterizer} turns its facets into the box.
 * Collision follows the parts a player meets: floors with fenced edges, posts and beams, the units, pumps, lines, the
 * air cooler and its legs, and the structure's ladder, climbed like the column's.
 */
final class ColumnAuxiliaries {
    private static final int CACHE = 32;
    private static final Map<EquipmentShape, ColumnAuxiliaries> BUILT = new ConcurrentHashMap<>();

    /** Floors and beams: grating thickness, beam depth and width, post section, handrail and toe board as on the column's decks. */
    static final double DECK = ColumnStructure.DECK, BEAM = .2, FLANGE_BEAM = .15, POST = .25;
    /** Saddle height of a lying unit over its floor or the ground, and a nozzle's neck over its shell, m. */
    static final double SADDLE = .35, NECK = .25;
    /** The air cooler's distance from the structure, m (owner decision 1: its own stand). */
    static final double AC_GAP = 1;
    /**
     * Clear walkway round every unit on a floor, inside the fence's band, and headroom over a floor's deck (EN ISO 14122-2:
     * walkways 0.6 m wide at least, 0.8 m preferred, 2.1 m of headroom), m.
     */
    static final double WALKWAY = .8, HEADROOM = 2.1;
    /** Air cooler face per m² of column cross-section, largest bay face (m²), most bays (owner decision 5). */
    static final double AC_FACE = 8.5, AC_BAY_FACE = 60;
    static final int AC_BAYS = 4;

    // ---- Sizes (static: the box and the skirt's lower bound need them without a build) ----------------------------

    /** Shell diameter of the condenser (and a kettle's bundle): 0.65 D^(2/3), to 0.25 m. */
    static double condenserShell(double d) {
        return Math.max(.5, Math.round(.65 * DeterministicMath.cbrt(d * d) * 4) / 4.0);
    }

    /** A pumparound cooler's shell: 0.8 of the condenser's, to 0.25 m. */
    static double coolerShell(double d) {
        return Math.max(.5, Math.round(.8 * condenserShell(d) * 4) / 4.0);
    }

    /** Tube length of an exchanger of shell {@code shell}: 4.5 shell diameters, whole metres, at least 2. */
    static double tubes(double shell) {
        return Math.max(2, Math.floor(4.5 * shell + .5));
    }

    /** The reflux drum: 0.6 D to 0.5 m, 1 to 4 m. */
    static double drumDiameter(double d) {
        return Math.max(1, Math.min(4, Math.round(.6 * d * 2) / 2.0));
    }

    /** A kettle's shell: 1.6 times its bundle (the condenser's shell), to 0.25 m. */
    static double kettleShell(double d) {
        return Math.round(1.6 * condenserShell(d) * 4) / 4.0;
    }

    /** Centre height of a kettle reboiler's vapour return on the column, wanted between the sump level and the bottom tray. */
    static double vapourReturnHeight(double d, double yB) {
        return yB + .75 * ColumnTrays.bottomSpace(d);
    }

    /**
     * The lowest skirt for these options: with a kettle, the bottoms nozzle (0.3 m over the bottom tangent line) must stand
     * an elbow over the kettle's inlet, and the vapour return's row an elbow over the kettle's vapour outlet.
     */
    static int minimumSkirt(double d, ColumnOptions options) {
        if (options.reboiler() != ColumnOptions.Reboiler.KETTLE) return 0;
        double dk = kettleShell(d), vr = ColumnStructure.size(.27 * d, 1), bt = ColumnStructure.size(d / 6, .6);
        double row = Math.floor(.75 * ColumnTrays.bottomSpace(d)) + .5;
        double vapour = SADDLE + dk + NECK + vr + .05 - row, liquid = SADDLE + dk / 2 + bt + .05 - .3;
        return (int) Math.ceil(Math.max(vapour, liquid) - 1e-9);
    }

    /**
     * Whether a column nozzle carries a line to the auxiliaries: those on the structure side (reflux, pumparound draws
     * and returns), the vapour outlet, and with a kettle the bottoms and the vapour return.
     */
    static boolean lined(EquipmentShape s, EquipmentShape.Nozzle n) {
        return switch (n.role()) {
            case REFLUX, PUMPAROUND_DRAW, RETURN, VAPOUR_OUTLET, VAPOUR_RETURN -> true;
            case BOTTOMS -> s.options().reboiler() == ColumnOptions.Reboiler.KETTLE;
            default -> false;
        };
    }

    static ColumnAuxiliaries of(EquipmentShape shape) {
        var built = BUILT.get(shape);
        if (built != null) return built;
        if (BUILT.size() >= CACHE) BUILT.clear();
        return BUILT.computeIfAbsent(shape, s -> new ColumnAuxiliaries(s, false));
    }

    // ---- Collision parts ------------------------------------------------------------------------------------------

    /**
     * A solid part in the frame: an axis-aligned box ({@code radius} 0), or a cylinder along frame axis {@code axis}
     * between {@code lo} and {@code hi} on it, of radius {@code radius} round the line through {@code centre}, with 2:1
     * heads of depth {@code headLo} and {@code headHi} beyond its ends (0: flat).
     */
    private record Solid(double[] min, double[] max, int axis, double[] centre, double radius, double lo, double hi, double headLo, double headHi) {
        static Solid box(double[] min, double[] max) {
            return new Solid(min, max, -1, null, 0, 0, 0, 0, 0);
        }

        static Solid rod(double[] centre, int axis, double lo, double hi, double radius, double headLo, double headHi) {
            double[] min = centre.clone(), max = centre.clone();
            for (int a = 0; a < 3; a++) {
                min[a] -= radius;
                max[a] += radius;
            }
            min[axis] = lo - headLo;
            max[axis] = hi + headHi;
            return new Solid(min, max, axis, centre, radius, lo, hi, headLo, headHi);
        }

        boolean contains(double[] p) {
            for (int a = 0; a < 3; a++) if (p[a] < min[a] || p[a] > max[a]) return false;
            if (axis < 0) return true;
            double rho2 = 0;
            for (int a = 0; a < 3; a++) if (a != axis) rho2 += (p[a] - centre[a]) * (p[a] - centre[a]);
            double r2 = radius * radius, s = p[axis];
            if (s >= lo && s <= hi) return rho2 <= r2;
            double head = s < lo ? headLo : headHi, out = s < lo ? lo - s : s - hi;
            return head > 0 && (out / head) * (out / head) + rho2 / r2 <= 1;
        }
    }

    // ---- The layout -----------------------------------------------------------------------------------------------

    private final EquipmentShape shape;
    /**
     * The mesh being drawn. The cached layout drops it once built and keeps its collision and outline only; {@link #mesh()}
     * draws the facets again in a layout of its own, so the geometry that takes them in holds the only copy.
     */
    private Mesh mesh = new Mesh();
    private final List<Solid> solids = new ArrayList<>(), volumes = new ArrayList<>();
    /** Each line's straight runs, {x0, y0, z0, x1, y1, z1, radius, line}, for the clash check of the tests. */
    private final List<double[]> runs = new ArrayList<>();
    private int lines;
    /**
     * Solids the part check of the tests passes over: the lines' straight runs (checked line against line) and the bands
     * only a player collides with (fences above their top rail), which no mesh draws.
     */
    private final java.util.Set<Solid> notParts = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final Map<Long, List<Solid>> solidCells = new HashMap<>(), volumeCells = new HashMap<>();
    /** Ladder climbing boxes, {x0, y0, z0, x1, y1, z1}. */
    private final List<double[]> climbs = new ArrayList<>();
    private final List<double[]> edges = new ArrayList<>();
    /** The structure's floors, {x0, z0, x1, z1, level}: the deck's top is {@link #DECK} over its level. */
    private final List<double[]> floors = new ArrayList<>();
    private final double[] bounds;

    private final double d, a;
    private final boolean water, ejector, kettle;

    /** A layout of {@code s}'s units; {@code drawing} keeps its mesh and leaves its collision unindexed. */
    private ColumnAuxiliaries(EquipmentShape s, boolean drawing) {
        shape = s;
        d = s.diameter();
        a = s.axis();
        var o = s.options();
        water = o.condenser() == ColumnOptions.Condenser.WATER;
        ejector = o.ejector();
        kettle = o.reboiler() == ColumnOptions.Reboiler.KETTLE;
        build();
        bounds = mesh.bounds();
        if (drawing) return;
        mesh = null;
        index(solids, solidCells);
        index(volumes, volumeCells);
    }

    /** The parts' facets in the frame, drawn afresh: the layout is deterministic. */
    Mesh mesh() {
        return new ColumnAuxiliaries(shape, true).mesh;
    }

    /** The structure's floors, {x0, z0, x1, z1, level} (tests). */
    List<double[]> floors() {
        return floors;
    }

    /** The parts' extent in the frame, {x0, y0, z0, x1, y1, z1}. */
    double[] bounds() {
        return bounds.clone();
    }

    /** The lines' straight runs, {x0, y0, z0, x1, y1, z1, radius, line} (tests). */
    List<double[]> runs() {
        return runs;
    }

    /** Whether a frame point lies in a part other than a line (tests: a line through another part). */
    boolean insidePart(double[] p) {
        for (var s : solids) if (!notParts.contains(s) && s.contains(p)) return true;
        return false;
    }

    /** Whether a frame point lies in a part a player collides with. */
    boolean inside(double[] p) {
        return within(solidCells, p);
    }

    /** Whether a frame point lies inside a closed unit (a drum, an exchanger's shell, an air cooler's plenum): no cell there is open. */
    boolean filled(double[] p) {
        return within(volumeCells, p);
    }

    /** Whether a frame point lies where the structure's ladder is climbed. */
    boolean climbable(double[] p) {
        for (var c : climbs) if (p[0] >= c[0] && p[0] <= c[3] && p[1] >= c[1] && p[1] <= c[4] && p[2] >= c[2] && p[2] <= c[5]) return true;
        return false;
    }

    /** Outline edges in the frame: the floors' and the air cooler's outer edges, {x0, y0, z0, x1, y1, z1} each. */
    List<double[]> edges() {
        return edges;
    }

    private static boolean within(Map<Long, List<Solid>> cells, double[] p) {
        var list = cells.get(key(Mesh.floor(p[0]), Mesh.floor(p[1]), Mesh.floor(p[2])));
        if (list == null) return false;
        for (var s : list) if (s.contains(p)) return true;
        return false;
    }

    private static void index(List<Solid> parts, Map<Long, List<Solid>> cells) {
        for (var s : parts)
            for (int x = Mesh.floor(s.min()[0]); x <= Mesh.floor(s.max()[0]); x++)
                for (int y = Mesh.floor(s.min()[1]); y <= Mesh.floor(s.max()[1]); y++)
                    for (int z = Mesh.floor(s.min()[2]); z <= Mesh.floor(s.max()[2]); z++) cells.computeIfAbsent(key(x, y, z), k -> new ArrayList<>()).add(s);
    }

    private static long key(int x, int y, int z) {
        return (long) (x + 1024) << 42 | (long) (y + 1024) << 21 | (z + 1024);
    }

    // ---- Building ---------------------------------------------------------------------------------------------------

    /** A pumparound's two column nozzles. */
    private record Pumparound(EquipmentShape.Nozzle draw, EquipmentShape.Nozzle ret) {
    }

    private void build() {
        var s = shape;
        var nozzles = s.nozzles();
        EquipmentShape.Nozzle top = null, reflux = null, bottoms = null, vapourReturn = null;
        var draws = new ArrayList<EquipmentShape.Nozzle>();
        var returns = new ArrayList<EquipmentShape.Nozzle>();
        for (var n : nozzles)
            switch (n.role()) {
                case VAPOUR_OUTLET -> top = n;
                case REFLUX -> reflux = n;
                case BOTTOMS -> bottoms = n;
                case VAPOUR_RETURN -> vapourReturn = n;
                case PUMPAROUND_DRAW -> draws.add(n);
                case RETURN -> returns.add(n);
                default -> {
                }
            }
        // A pumparound whose draw stands under its return (the usual case) gets its lines; the rare pair the nozzle rows
        // turned over keeps its blind flanges.
        var pumparounds = new ArrayList<Pumparound>();
        for (int k = 0; k < Math.min(draws.size(), returns.size()); k++)
            if (returns.get(k).base()[1] > draws.get(k).base()[1]) pumparounds.add(new Pumparound(draws.get(k), returns.get(k)));
        pumparounds.sort((p, q) -> Double.compare(p.draw().base()[1], q.draw().base()[1]));

        double topOd = top.maxOd(), lineB = ejector ? ColumnStructure.size(.75 * topOd, 1) : topOd;
        double paOd = ColumnStructure.size(d / 6, .6), rfOd = reflux != null ? reflux.maxOd() : ColumnStructure.size(d / 8, .5);
        double cdOd = ColumnStructure.size(d / 6, .6);
        double dc = condenserShell(d), lc = tubes(dc), dr = drumDiameter(d), tr = Math.max(2, Math.floor(3 * dr + .5)), dp = coolerShell(d), lp = tubes(dp);
        double faceRight = Math.floor(a - s.outerRadius() - EquipmentShape.STAND_OUT);

        // Pumps: casing radius and shaft height from the line.
        double rcPa = pumpRadius(paOd), rcRf = pumpRadius(rfOd);
        double ysPa = shaft(rcPa), ysRf = shaft(rcRf);
        // The ejector: suction ds = the overhead; its frame stands e from its axis.
        double ds = topOd, e = .6 * ds + .3;

        // ---- Pipe bay: x positions, from the column outwards (negative) ----
        double maxR = Math.max(pumparounds.isEmpty() ? 0 : paOd, rfOd) / 2;
        double xJog = -.3 - maxR;
        double xV1 = xJog - 2 * Math.max(paOd, rfOd) - .1;
        double xSuction = xV1 - paOd - .15, xCasing = xSuction - NECK * .6 - pumpCasing(rcPa) / 2;
        double motorEnd = xCasing - pumpLength(rcPa) + pumpCasing(rcPa) / 2;
        double xV2 = xCasing - paOd - .2;
        double xDown = ejector ? xV1 - rfOd / 2 - .25 - e - POST / 2 : xV1 - (rfOd + topOd) / 2 - .4;
        double xFace = pumparounds.isEmpty() ? xV1 - rfOd / 2 - .5 : xV2 - paOd / 2 - .35;
        if (ejector) xFace = Math.min(xFace, xDown - e - POST / 2 - .3);
        // The structure starts at its inner post row, a little past the pipe bay.
        double xF = Math.floor((xFace) * 4) / 4;
        double xUnits = xF - ColumnStructure.RAIL_BAND - WALKWAY;
        double xCoolers = pumparounds.isEmpty() ? xUnits : Math.min(xUnits, motorEnd - .4);

        // ---- Lanes: pumparound k (lowest draw first) nearest the middle, front and back alternately ----
        double lane = Math.max(dp + .5, 2 * rcPa + .6);
        if (ejector) lane = Math.max(lane, e + POST / 2 + rcPa + .3);
        var lanes = new double[pumparounds.size()];
        double extent = 0;
        for (int k = 0; k < lanes.length; k++) {
            lanes[k] = a + (k % 2 == 0 ? -1 : 1) * (k / 2 + 1) * lane;
            extent = Math.max(extent, Math.abs(lanes[k] - a));
        }

        // ---- Heights ----
        double coolerTop = SADDLE + dp, coolerFace = coolerTop + NECK;
        double yDischarge = coolerFace + paOd + .05, yReturnRun = yDischarge + paOd + .3;
        double yRefluxRun = shaft(rcRf) + rcRf + .45 + rfOd;
        double underFloor = pumparounds.isEmpty() ? yRefluxRun + rfOd / 2 : Math.max(yRefluxRun + rfOd / 2, yReturnRun + paOd / 2);
        int f1 = (int) Math.max(3, Math.ceil(underFloor + .15 + BEAM));
        double drumCentre = f1 + DECK + SADDLE + dr / 2, drumTop = drumCentre + dr / 2;
        int f2 = (int) Math.ceil(Math.max(drumTop + 1, f1 + DECK + HEADROOM + BEAM) - 1e-9);
        double condenserCentre = f2 + DECK + SADDLE + dc / 2;

        // ---- Units along x, inner ends at xUnits ----
        double drumLength = tr + dr / 2, condenserLength = exchangerLength(dc, lc), coolerLength = exchangerLength(dp, lp);
        double drumInner = xUnits - dr / 4, drumOuter = drumInner - tr;
        double longest = Math.max(drumLength, water ? condenserLength : 0);
        double xs0 = Math.min(xUnits - longest - ColumnStructure.RAIL_BAND - WALKWAY, xCoolers - (pumparounds.isEmpty() ? 0 : coolerLength) - .5);
        xs0 = Math.floor(xs0 * 4) / 4;
        double half = Math.max(Math.max(dr, water ? dc : 0) / 2 + ColumnStructure.RAIL_BAND + WALKWAY, pumparounds.isEmpty() ? 0 : extent + dp / 2 + .45);
        half = Math.max(half, rcRf + .8);
        double zs0 = a - half, zs1 = a + half;

        // ---- Overhead: the inlet it ends at ----
        double xInlet, inletFace;
        double hb = 0, acLength = 0, bayWidth = 0, fan = 0, header = 0, bundle = 0, plenum = 0, xA1 = 0, xA0 = 0;
        int bays = 0;
        if (water) {
            xInlet = xUnits - dc / 4 - .1 - .95 * lc / 2;
            inletFace = condenserCentre + dc / 2 + NECK;
        } else {
            double face = AC_FACE * Math.PI * d * d / 4;
            bays = (int) Math.min(AC_BAYS, Math.ceil(face / AC_BAY_FACE - 1e-9));
            acLength = Math.max(3, Math.min(12, Math.ceil(Math.sqrt(2.4 * face / bays) - 1e-9)));
            bayWidth = Math.max(2, Math.min(5, Math.ceil(face / (bays * acLength) * 2 - 1e-9) / 2));
            fan = Math.min(.8 * bayWidth, Math.min(.45 * acLength, 4.2)) / 2;
            header = Math.max(.35, Math.min(.8, .5 * lineB + .15));
            bundle = Math.max(.3, Math.min(.6, .1 * bayWidth + .15));
            plenum = .4 * bayWidth;
            hb = Math.max(8, Math.ceil(drumTop + 2));
            xA1 = xs0 - AC_GAP;
            xA0 = xA1 - acLength;
            xInlet = xA1 - header / 2;
            // The overhead tees into a manifold over the inner header, which feeds a drop to each bay.
            inletFace = hb + bundle + .05 + .3 + Math.min(lineB, header - .1) + lineB / 2;
        }
        // The overhead run and the beams under it leave headroom over the top floor.
        double yLow = Math.max(inletFace + lineB + (water ? .1 : .2), (water ? f2 : f1) + DECK + HEADROOM + BEAM + .02 + lineB / 2);
        double postTop = yLow - lineB / 2 - .02;
        double faceTop = top.face()[1];
        double yHigh = Math.max(faceTop + 1.2 * topOd, ejector ? yLow + lineB + .3 + 6.75 * ds : yLow + 2 * topOd + .3);

        // ---- Draw ----
        overhead(top, topOd, lineB, xDown, yHigh, yLow, xInlet, inletFace, ds);
        structure(xs0, xF, zs0, zs1, f1, water ? f2 : -1, postTop, ejector ? xDown : Double.NaN, e, new double[]{yLow, lineB / 2, yRefluxRun, rfOd / 2});
        drum(xUnits, drumCentre, dr, tr, f1);
        if (water) condenser(xUnits, condenserCentre, dc, lc, f2, inletFace, lineB);
        else airCooler(xA0, xA1, bays, bayWidth, acLength, fan, header, bundle, plenum, hb, xInlet, inletFace, lineB);

        // Condensate into the drum's top, near its inner end (water) or its outer end (air).
        if (water) {
            double x = Math.min(xUnits - dc / 4, drumInner) - .1 - .6;
            double from = condenserCentre - dc / 2 - NECK, to = drumTop + NECK;
            nozzle(new double[]{x, condenserCentre - dc / 2 + .05, a}, 1, -1, from, cdOd, false);
            nozzle(new double[]{x, drumTop - .05, a}, 1, 1, to, cdOd, false);
            pipe(cdOd, true, true, new double[]{x, from, a}, new double[]{x, to, a});
        } else {
            double od = Math.min(cdOd, bundle + .05), yOut = hb - .05 + (bundle + .1) / 2, x = drumOuter + .4 + od / 2;
            double yRun = Math.min(hb - .6, drumTop + NECK + 2 * od + .3), xGap = xA1 + AC_GAP / 2;
            nozzle(new double[]{xA1 - .05, yOut, a}, 0, 1, xA1 + NECK, od, false);
            nozzle(new double[]{x, drumTop - .05, a}, 1, 1, drumTop + NECK, od, false);
            pipe(od, true, true, new double[]{xA1 + NECK, yOut, a}, new double[]{xGap, yOut, a}, new double[]{xGap, yRun, a}, new double[]{x, yRun, a},
                    new double[]{x, drumTop + NECK, a});
        }
        if (ejector) {
            // The water boot under the drum's outer end: the ejector's motive steam condenses with the overhead.
            double boot = Math.max(.25, .35 * dr), x = drumOuter + .3 + boot / 2, bottom = drumCentre - .5 * dr - .5 * dr;
            var f = Frame.of(new double[]{x, 0, a}, 1, 1);
            mesh.prism(f, boot / 2, bottom, drumCentre - dr / 4, 16, Material.SHELL);
            mesh.annulus(f, 0, boot / 2, bottom, 16, -1, Material.SHELL);
            solids.add(Solid.rod(new double[]{x, 0, a}, 1, bottom, drumCentre, boot / 2, 0, 0));
            stub(new double[]{x, bottom + .02, a}, 1, -1, bottom - NECK * .8, Math.max(.1, ColumnStructure.size(boot / 3, .3)));
        }

        // Reflux: the drum's outlet down to the reflux pump in the middle lane, up the pipe bay to tray 1.
        if (reflux != null) {
            double xRc = xV1 - 2 * rfOd - .3 - pumpCasing(rcRf) / 2;
            double xDo = xRc - pumpCasing(rcRf) / 2 - NECK * .6 - rfOd - .15;
            xDo = Math.min(xDo, drumInner - .3 - rfOd / 2);
            xDo = Math.max(xDo, drumOuter + .3 + rfOd / 2);
            xRc = xDo + rfOd + .15 + NECK * .6 + pumpCasing(rcRf) / 2;
            double outlet = drumCentre - dr / 2 - NECK;
            nozzle(new double[]{xDo, drumCentre - dr / 2 + .05, a}, 1, -1, outlet, rfOd, false);
            double suction = pump(xRc, ysRf, a, 1, rfOd, rcRf);
            pipe(rfOd, true, true, new double[]{xDo, outlet, a}, new double[]{xDo, ysRf, a}, new double[]{suction, ysRf, a});
            double h = reflux.base()[1];
            pipe(rfOd, true, true, new double[]{xRc, ysRf + rcRf + NECK, a}, new double[]{xRc, yRefluxRun, a}, new double[]{xV1, yRefluxRun, a},
                    new double[]{xV1, h, a}, new double[]{faceRight, h, a});
        }

        // Pumparounds: draw down its lane to its pump, through its cooler, back up its lane over its draw.
        for (int k = 0; k < pumparounds.size(); k++) {
            var p = pumparounds.get(k);
            double z = lanes[k], hd = p.draw().base()[1], hr = p.ret().base()[1], od = p.draw().maxOd();
            double suction = pump(xCasing, ysPa, z, -1, od, rcPa);
            pipe(od, true, true, new double[]{faceRight, hd, a}, new double[]{xJog, hd, a}, new double[]{xJog, hd, z}, new double[]{xV1, hd, z},
                    new double[]{xV1, ysPa, z}, new double[]{suction, ysPa, z});
            double[] ports = exchanger(xCoolers, SADDLE + dp / 2, z, dp, lp, 0, od, true);
            pipe(od, true, true, new double[]{xCasing, ysPa + rcPa + NECK, z}, new double[]{xCasing, yDischarge, z}, new double[]{ports[0], yDischarge, z},
                    new double[]{ports[0], coolerFace, z});
            pipe(od, true, true, new double[]{ports[1], coolerFace, z}, new double[]{ports[1], yReturnRun, z}, new double[]{xV2, yReturnRun, z},
                    new double[]{xV2, hr, z}, new double[]{xJog, hr, z}, new double[]{xJog, hr, a}, new double[]{faceRight, hr, a});
        }

        if (kettle && bottoms != null && vapourReturn != null) kettle(bottoms, vapourReturn);
    }

    // ---- Units ----------------------------------------------------------------------------------------------------

    /** Overall length of a shell-and-tube exchanger along its axis: floating-head cover, shell, channel and its cover. */
    private static double exchangerLength(double shell, double tubes) {
        return shell / 4 + .1 + .95 * tubes + .1 + .5 * shell + .2 + .08;
    }

    /**
     * A shell-and-tube exchanger (TEMA AES) lying along x from its floating-head cover's apex at {@code inner} outwards
     * (-x) to its channel cover, centred at height {@code y} and z {@code z}, on two saddles over the level {@code base};
     * its shell-side nozzles on top (sized {@code processOd}, 0 for none) near both ends, cooling water on the channel. Returns the
     * x of its {inlet, outlet} nozzles, whose faces stand {@link #NECK} over the shell.
     */
    private double[] exchanger(double inner, double y, double z, double shell, double tubes, double base, double processOd, boolean pa) {
        double r = shell / 2, cover = shell / 4, t0 = inner - cover - .1, t1 = t0 - .95 * tubes, ch0 = t1 - .1, ch1 = ch0 - .5 * shell - .2;
        var f = new Frame(new double[]{inner, y, z}, new double[]{-1, 0, 0}, new double[]{0, 1, 0});
        int n = sides(shell);
        head(f, r, cover, cover, -1, n, Material.SHELL);
        mesh.ring(f, r, r * 1.08, cover, cover + .1, n, 3, Material.FLANGE);
        mesh.prism(f, r, cover + .1, inner - t1, n, Material.SHELL);
        mesh.ring(f, r, r * 1.1, inner - t1, inner - ch0, n, 3, Material.FLANGE);
        mesh.prism(f, r, inner - ch0, inner - ch1, n, Material.SHELL);
        mesh.annulus(f, 0, r * 1.1, inner - ch1 + .08, n, 1, Material.FLANGE);
        mesh.ring(f, r, r * 1.1, inner - ch1, inner - ch1 + .08, n, 1, Material.FLANGE);
        solids.add(Solid.rod(new double[]{0, y, z}, 0, ch1 - .08, t0 + .1, r * 1.05, 0, cover));
        volumes.add(Solid.rod(new double[]{0, y, z}, 0, ch1 + .02, t0 + .08, r * .94, 0, cover * .9));
        saddles(t0 - .2 * (t0 - t1), t1 + .2 * (t0 - t1), y, z, r, base);
        // Cooling water on the channel: out on top, in below (on a pumparound cooler both on top, the lanes being close).
        double water = Math.max(.1, ColumnStructure.size(.3 * shell, .5)), xc = (ch0 + ch1) / 2;
        double offset = pa ? Math.min(.25 * (ch0 - ch1), water * .7) : 0;
        stub(new double[]{xc - offset, y + r - .05, z}, 1, 1, y + r + NECK, water);
        if (pa) stub(new double[]{xc + offset, y + r - .05, z}, 1, 1, y + r + NECK, water);
        else stub(new double[]{xc, y - r + .05, z}, 1, -1, y - r - Math.min(NECK, Math.max(.12, y - r - base - DECK - .05)), water);
        if (processOd <= 0) return new double[0];
        double xIn = t0 - processOd / 2 - .25, xOut = t1 + processOd / 2 + .25;
        nozzle(new double[]{xIn, y + r - .05, z}, 1, 1, y + r + NECK, processOd, false);
        nozzle(new double[]{xOut, y + r - .05, z}, 1, 1, y + r + NECK, processOd, false);
        return new double[]{xIn, xOut};
    }

    /** The water condenser on the second floor: its vapour inlet on top in the middle of the shell. */
    private void condenser(double inner, double y, double shell, double tubes, int floor, double inletFace, double inletOd) {
        exchanger(inner, y, a, shell, tubes, floor + DECK, 0, false);
        double x = inner - shell / 4 - .1 - .95 * tubes / 2;
        nozzle(new double[]{x, y + shell / 2 - .05, a}, 1, 1, inletFace, inletOd, false);
    }

    /** The reflux drum on the first floor: a lying drum with 2:1 heads, its inner head's apex at {@code inner}. */
    private void drum(double inner, double y, double dr, double tt, int floor) {
        double r = dr / 2, depth = dr / 4;
        var f = new Frame(new double[]{inner - depth, y, a}, new double[]{-1, 0, 0}, new double[]{0, 1, 0});
        int n = sides(dr);
        head(f, r, 0, depth, -1, n, Material.HEAD);
        mesh.prism(f, r, 0, tt, n, Material.SHELL);
        head(f, r, tt, depth, 1, n, Material.HEAD);
        double t0 = inner - depth, t1 = t0 - tt;
        solids.add(Solid.rod(new double[]{0, y, a}, 0, t1, t0, r, depth, depth));
        volumes.add(Solid.rod(new double[]{0, y, a}, 0, t1, t0, r * .94, depth * .9, depth * .9));
        saddles(t0 - .2 * tt, t1 + .2 * tt, y, a, r, floor + DECK);
    }

    /**
     * A head of a lying unit, drawn as bands of a 2:1 ellipsoid: from its tangent line at frame station {@code at} to its
     * apex {@code depth} beyond, along the frame axis ({@code sign} +1) or against it (-1).
     */
    private void head(Frame f, double r, double at, double depth, double sign, int n, Material material) {
        int bands = Math.max(3, n / 4);
        var base = f.along(at);
        var spheroid = new Spheroid(base, f.axis(), r, depth);
        for (int k = 0; k < bands; k++) {
            double p0 = Math.PI / 2 * k / bands, p1 = Math.PI / 2 * (k + 1) / bands;
            double r0 = r * DeterministicMath.cos(p0), r1 = k + 1 == bands ? 0 : r * DeterministicMath.cos(p1);
            double z0 = at + sign * depth * DeterministicMath.sin(p0), z1 = at + sign * depth * DeterministicMath.sin(p1);
            for (int i = 0; i < n; i++) {
                double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
                var polygon = new ArrayList<double[]>(4);
                polygon.add(f.at(a0, r0, z0));
                polygon.add(f.at(a1, r0, z0));
                if (r1 > 0) {
                    polygon.add(f.at(a1, r1, z1));
                    polygon.add(f.at(a0, r1, z1));
                } else polygon.add(f.along(z1));
                mesh.add(polygon, base, spheroid, material, null, null);
            }
        }
    }

    /** Two saddles under a unit lying along x, at x {@code s0} and {@code s1}, from {@code base} up to its shell. */
    private void saddles(double s0, double s1, double y, double z, double r, double base) {
        double width = Math.max(.15, Math.min(.3, .12 * r + .1)), across = .8 * r, top = y - .6 * r;
        for (double x : new double[]{s0, s1}) {
            box(new double[]{x - width / 2, base, z - across}, new double[]{x + width / 2, top, z + across}, Material.SUPPORT, true);
            box(new double[]{x - width / 2 - .05, base, z - across - .05}, new double[]{x + width / 2 + .05, base + .04, z + across + .05}, Material.SUPPORT, false);
        }
    }

    // ---- Pumps ----------------------------------------------------------------------------------------------------

    private static double pumpRadius(double od) {
        return Math.max(.18, .8 * od + .05);
    }

    private static double pumpCasing(double rc) {
        return Math.max(.2, .7 * rc);
    }

    /** From the suction flange's face to the motor's end. */
    private static double pumpLength(double rc) {
        return NECK * .6 + pumpCasing(rc) + .8 * rc + .5 * rc + 2.2 * rc;
    }

    private static double shaft(double rc) {
        return .12 + .08 + rc;
    }

    /**
     * An end-suction pump on its baseplate at grade, its shaft along x at height {@code y} in lane {@code z}: the volute
     * centred at {@code casing}, its suction axial on the side away from the motor, which lies towards {@code motor}
     * (+1 or -1), its discharge on top. Returns the x of the suction flange's face.
     */
    private double pump(double casing, double y, double z, int motor, double od, double rc) {
        double lc = pumpCasing(rc), m = motor;
        var axis = new double[]{m, 0, 0};
        var f = new Frame(new double[]{casing, y, z}, axis, new double[]{0, 1, 0});
        int n = Math.max(12, sides(2 * rc));
        mesh.cylinder(f, rc, -lc / 2, lc / 2, n, Material.MACHINE);
        // Suction neck and flange against the motor's direction; discharge neck up from the volute's top.
        double suction = casing - m * (lc / 2 + NECK * .6);
        nozzle(new double[]{casing - m * (lc / 2 - .02), y, z}, 0, -motor, suction, od, false);
        nozzle(new double[]{casing, y + rc - .05, z}, 1, 1, y + rc + NECK, od, false);
        double bearing = lc / 2 + .8 * rc, guard = bearing + .5 * rc, end = guard + 2.2 * rc;
        mesh.frustum(f, .55 * rc, .4 * rc, lc / 2, bearing, n, Material.MACHINE);
        box(new double[]{Math.min(casing + m * bearing, casing + m * guard), y - .3 * rc, z - .3 * rc},
                new double[]{Math.max(casing + m * bearing, casing + m * guard), y + .3 * rc, z + .3 * rc}, Material.ACTUATOR, false);
        double rm = .75 * rc;
        mesh.cylinder(f, rm, guard, end - .1 * rc, n, Material.MOTOR);
        mesh.frustum(f, .9 * rm, .6 * rm, end - .1 * rc, end, n, Material.MOTOR);
        mesh.annulus(f, 0, .6 * rm, end, n, 1, Material.MOTOR);
        double x0 = Math.min(casing - m * lc / 2, casing + m * end), x1 = Math.max(casing - m * lc / 2, casing + m * end);
        box(new double[]{x0, 0, z - .9 * rc}, new double[]{x1, .12, z + .9 * rc}, Material.STEELWORK, true);
        box(new double[]{casing - .3 * rc, .12, z - .5 * rc}, new double[]{casing + .3 * rc, y - .8 * rc, z + .5 * rc}, Material.STEELWORK, false);
        solids.add(Solid.rod(new double[]{0, y, z}, 0, x0, x1, rc, 0, 0));
        return suction;
    }

    // ---- Kettle ---------------------------------------------------------------------------------------------------

    /**
     * The kettle reboiler at grade on the left, lying along z: channel at the front, a cone up to the kettle shell, a 2:1
     * head at the back. The sump's liquid comes from the bottoms nozzle round the front into its side; its vapour rises
     * from its top to the column's vapour return. Steam in and condensate out on the channel; the overflow (bottoms) low on
     * the back head.
     */
    private void kettle(EquipmentShape.Nozzle bottoms, EquipmentShape.Nozzle vapour) {
        double db = condenserShell(d), dk = kettleShell(d), lb = tubes(db), rk = dk / 2, rb = db / 2;
        double btOd = bottoms.maxOd(), vrOd = vapour.maxOd();
        double xk = shape.width() + .35 + rk, y = SADDLE + rk;
        double frontFace = bottoms.face()[2], hb = bottoms.base()[1], zl = frontFace - btOd;
        double s0 = zl - btOd / 2 - .3, cone = Math.max(.3, .6 * (dk - db)), ch0 = s0 - cone - .1, ch1 = ch0 - .5 * db - .2;
        double s1 = Math.max(s0 + .9 * lb + .3, a + vrOd / 2 + .6);
        var f = new Frame(new double[]{xk, y, 0}, new double[]{0, 0, 1}, new double[]{0, 1, 0});
        int n = sides(dk), nb = sides(db);
        mesh.ring(f, 0, rb * 1.1, ch1, ch1 + .08, nb, 3, Material.FLANGE);
        mesh.prism(f, rb, ch1 + .08, ch0, nb, Material.SHELL);
        mesh.ring(f, 0, rb * 1.12, ch0, ch0 + .1, nb, 3, Material.FLANGE);
        mesh.frustum(f, rb * 1.05, rk, ch0 + .1, s0, n, Material.SHELL);
        mesh.prism(f, rk, s0, s1, n, Material.SHELL);
        head(f, rk, s1, dk / 4, 1, n, Material.HEAD);
        solids.add(Solid.rod(new double[]{xk, y, 0}, 2, s0, s1, rk, 0, dk / 4));
        solids.add(Solid.rod(new double[]{xk, y, 0}, 2, ch1, s0, rb * 1.1, 0, 0));
        volumes.add(Solid.rod(new double[]{xk, y, 0}, 2, s0 + .02, s1, rk * .94, 0, dk / 4 * .9));
        double w = Math.max(.15, Math.min(.3, .12 * rk + .1));
        for (double z : new double[]{s0 + .2 * (s1 - s0), s1 - .2 * (s1 - s0)}) {
            box(new double[]{xk - .8 * rk, 0, z - w / 2}, new double[]{xk + .8 * rk, y - .6 * rk, z + w / 2}, Material.SUPPORT, true);
            box(new double[]{xk - .8 * rk - .05, 0, z - w / 2 - .05}, new double[]{xk + .8 * rk + .05, .04, z + w / 2 + .05}, Material.SUPPORT, false);
        }
        double steam = Math.max(.1, ColumnStructure.size(.3 * db, .5)), zc = (ch0 + ch1) / 2;
        stub(new double[]{xk, y + rb - .05, zc}, 1, 1, y + rb + NECK, steam);
        stub(new double[]{xk, y - rb + .05, zc}, 1, -1, Math.max(.12, y - rb - NECK), steam);
        stub(new double[]{xk, y - rk / 2, s1 + .02}, 2, 1, s1 + Math.sqrt(.75) * dk / 4 + NECK, Math.max(.1, ColumnStructure.size(d / 8, .5)));
        // Sump liquid: out of the bottoms nozzle towards the front, down, and across into the kettle's side.
        double inlet = xk - rk - NECK;
        nozzle(new double[]{xk - rk + .05, y, zl}, 0, -1, inlet, btOd, false);
        pipe(btOd, true, true, new double[]{a, hb, frontFace}, new double[]{a, hb, zl}, new double[]{a, y, zl}, new double[]{inlet, y, zl});
        // Vapour: up from the kettle's top at the column's line, across into the vapour return.
        double top = y + rk + NECK, h = vapour.base()[1], faceLeft = vapour.face()[0];
        nozzle(new double[]{xk, y + rk - .05, a}, 1, 1, top, vrOd, false);
        pipe(vrOd, true, true, new double[]{xk, top, a}, new double[]{xk, h, a}, new double[]{faceLeft, h, a});
    }

    // ---- The structure --------------------------------------------------------------------------------------------

    /**
     * The steel structure from x {@code x0} to {@code x1} and z {@code z0} to {@code z1}: posts at its corners and every
     * 6 m or less along it up to {@code top}, beams round each floor and across the top under the overhead line, grating
     * floors with fenced edges, and a caged ladder on the back face at the outer end. With an ejector, its frame in the
     * pipe bay at {@code ejectorX}.
     */
    private void structure(double x0, double x1, double z0, double z1, int f1, int f2, double top, double ejectorX, double e, double[] clear) {
        int rows = Math.max(1, (int) Math.ceil((x1 - x0) / 6 - 1e-9));
        var xs = new double[rows + 1];
        for (int i = 0; i <= rows; i++) xs[i] = x1 - POST / 2 - (x1 - x0 - POST) * i / rows;
        double[] zs = {z0 + POST / 2, z1 - POST / 2};
        for (double x : xs)
            for (double z : zs) box(new double[]{x - POST / 2, 0, z - POST / 2}, new double[]{x + POST / 2, top, z + POST / 2}, Material.STEELWORK, true);
        var levels = new ArrayList<Double>();
        levels.add((double) f1);
        if (f2 > 0) levels.add((double) f2);
        levels.add(top);
        for (double level : levels) {
            for (double z : zs)
                box(new double[]{x0, level - BEAM, z - FLANGE_BEAM / 2}, new double[]{x1, level, z + FLANGE_BEAM / 2}, Material.STEELWORK, true);
            for (double x : xs)
                box(new double[]{x - FLANGE_BEAM / 2, level - BEAM, z0}, new double[]{x + FLANGE_BEAM / 2, level, z1}, Material.STEELWORK, true);
        }
        int highest = f2 > 0 ? f2 : f1;
        double ladderX = x0 + .7, rung = z1 + ColumnStructure.STANDOFF;
        for (double level : levels) {
            if (level == top) continue;
            floor(x0, x1, z0, z1, level, ladderX);
        }
        ladder(ladderX, rung, highest + DECK + ColumnStructure.LANDING);
        if (!Double.isNaN(ejectorX)) {
            // Four bars round the ejector from the ground over its steam chest, with square frames below its suction.
            double bar = .14;
            double[][] corners = {{ejectorX - e, a - e}, {ejectorX + e, a - e}, {ejectorX + e, a + e}, {ejectorX - e, a + e}};
            for (var c : corners) box(new double[]{c[0] - bar / 2, 0, c[1] - bar / 2}, new double[]{c[0] + bar / 2, ejectorTop, c[1] + bar / 2}, Material.STEELWORK, true);
            for (double y = 2.5; y < ejectorBraces; y += 2.5) {
                // Not where a line passes through the frame: {height, half its size} pairs.
                boolean free = true;
                for (int c = 0; c + 1 < clear.length; c += 2) if (Math.abs(y - clear[c]) < clear[c + 1] + .15) free = false;
                if (!free) continue;
                for (int k = 0; k < 4; k++) {
                    var p = corners[k];
                    var q = corners[(k + 1) % 4];
                    mesh.bar(new double[]{p[0], y, p[1]}, new double[]{q[0], y, q[1]}, bar * .8, new double[]{0, 1, 0}, Material.STEELWORK);
                }
            }
        }
    }

    /** Height of the ejector frame's bars and the highest its braces stand, set by {@link #overhead}. */
    private double ejectorTop, ejectorBraces;

    /** A grating floor at {@code level} over the structure, fenced on every edge but where the ladder arrives. */
    private void floor(double x0, double x1, double z0, double z1, double level, double ladderX) {
        floors.add(new double[]{x0, z0, x1, z1, level});
        double y1 = level + DECK;
        box(new double[]{x0, level, z0}, new double[]{x1, y1, z1}, Material.GRATING, true);
        double in = .05, rail = ColumnStructure.RAIL, bar = ColumnStructure.BAR, toe = ColumnStructure.TOE;
        double[][] corners = {{x1 - in, z0 + in}, {x0 + in, z0 + in}, {x0 + in, z1 - in}, {x1 - in, z1 - in}};
        double gap = ColumnStructure.CAGE_HALF + .1;
        for (int k = 0; k < 4; k++) {
            var p = corners[k];
            var q = corners[(k + 1) % 4];
            var spans = new ArrayList<double[][]>();
            // The back edge (z1, from x0 to x1) opens where the ladder arrives.
            if (k == 2) {
                spans.add(new double[][]{p, {ladderX - gap, p[1]}});
                spans.add(new double[][]{{ladderX + gap, p[1]}, q});
            } else spans.add(new double[][]{p, q});
            for (var span : spans) {
                double[] s0 = span[0], s1 = span[1];
                double length = Math.sqrt((s1[0] - s0[0]) * (s1[0] - s0[0]) + (s1[1] - s0[1]) * (s1[1] - s0[1]));
                int posts = Math.max(1, (int) Math.ceil(length / ColumnStructure.POSTS));
                for (int j = 0; j <= posts; j++) {
                    double t = (double) j / posts;
                    double x = s0[0] + (s1[0] - s0[0]) * t, z = s0[1] + (s1[1] - s0[1]) * t;
                    mesh.bar(new double[]{x, y1, z}, new double[]{x, y1 + rail + bar / 2, z}, bar, new double[]{1, 0, 0}, Material.HANDRAIL);
                }
                for (double h : new double[]{rail, ColumnStructure.MID_RAIL})
                    mesh.bar(new double[]{s0[0], y1 + h, s0[1]}, new double[]{s1[0], y1 + h, s1[1]}, bar, new double[]{0, 1, 0}, Material.HANDRAIL);
                double[] lo = {Math.min(s0[0], s1[0]) - .015, y1, Math.min(s0[1], s1[1]) - .015}, hi = {Math.max(s0[0], s1[0]) + .015, y1 + toe, Math.max(s0[1], s1[1]) + .015};
                box(lo, hi, Material.HANDRAIL, false);
            }
            // The fence's collision: a band inside the edge, as high as the column's, open at the ladder.
            for (var span : spans) {
                double[] s0 = span[0], s1 = span[1];
                double band = ColumnStructure.RAIL_BAND;
                double[] lo = {Math.min(s0[0], s1[0]), y1, Math.min(s0[1], s1[1])}, hi = {Math.max(s0[0], s1[0]), y1 + ColumnStructure.FENCE, Math.max(s0[1], s1[1])};
                if (k % 2 == 0) {
                    // Along x: the band lies inside, towards the floor's middle in z.
                    double inward = k == 0 ? 1 : -1;
                    lo[2] = Math.min(s0[1], s0[1] + inward * band);
                    hi[2] = Math.max(s0[1], s0[1] + inward * band);
                } else {
                    double inward = k == 1 ? 1 : -1;
                    lo[0] = Math.min(s0[0], s0[0] + inward * band);
                    hi[0] = Math.max(s0[0], s0[0] + inward * band);
                }
                // To the top rail it stands for the rails drawn; above, only the player meets it.
                double top = y1 + rail + bar;
                solids.add(Solid.box(lo, new double[]{hi[0], top, hi[2]}));
                solids.add(Solid.box(new double[]{lo[0], top, lo[2]}, hi));
                notParts.add(solids.getLast());
            }
        }
        segment(new double[]{x0, y1, z0}, new double[]{x1, y1, z0});
        segment(new double[]{x1, y1, z0}, new double[]{x1, y1, z1});
        segment(new double[]{x1, y1, z1}, new double[]{x0, y1, z1});
        segment(new double[]{x0, y1, z1}, new double[]{x0, y1, z0});
    }

    /**
     * A caged ladder on the structure's back face at x {@code x}, its rungs in the plane z = {@code rung}, from the ground
     * to {@code top}: as the column's, the player presses against the rung plane to climb.
     */
    private void ladder(double x, double rung, double top) {
        double half = ColumnStructure.LADDER_HALF, cage = ColumnStructure.CAGE_HALF, bar = ColumnStructure.BAR;
        var up = new double[]{0, 1, 0};
        for (double side : new double[]{-half, half}) {
            mesh.bar(new double[]{x + side, 0, rung}, new double[]{x + side, top, rung}, bar, new double[]{0, 0, 1}, Material.HANDRAIL);
            for (double y = 1; y < top - .3; y += 2)
                mesh.bar(new double[]{x + side, y, rung - ColumnStructure.STANDOFF + .02}, new double[]{x + side, y, rung - bar / 2}, bar, up, Material.STEELWORK);
        }
        for (double y = ColumnStructure.RUNG; y <= top - ColumnStructure.LANDING + 1e-9; y += ColumnStructure.RUNG)
            mesh.bar(new double[]{x - half, y, rung}, new double[]{x + half, y, rung}, bar * .8, up, Material.HANDRAIL);
        double first = ColumnStructure.CAGE_START;
        var hoop = new ArrayList<double[]>();
        hoop.add(new double[]{-half, 0});
        hoop.add(new double[]{-cage, 0});
        for (int k = 0; k <= 4; k++) {
            double phi = Math.PI * k / 4;
            hoop.add(new double[]{-cage * DeterministicMath.cos(phi), cage + cage * DeterministicMath.sin(phi)});
        }
        hoop.add(new double[]{cage, 0});
        hoop.add(new double[]{half, 0});
        for (double y = first; y <= top + 1e-9; y = y + ColumnStructure.HOOP > top && y < top - .2 ? top : y + ColumnStructure.HOOP)
            for (int k = 0; k + 1 < hoop.size(); k++)
                mesh.bar(new double[]{x + hoop.get(k)[0], y, rung + hoop.get(k)[1]}, new double[]{x + hoop.get(k + 1)[0], y, rung + hoop.get(k + 1)[1]}, bar, up, Material.HANDRAIL);
        for (int k : new int[]{2, 3, 4, 5, 6})
            mesh.bar(new double[]{x + hoop.get(k)[0], first, rung + hoop.get(k)[1]}, new double[]{x + hoop.get(k)[0], top, rung + hoop.get(k)[1]}, bar,
                    new double[]{0, 0, 1}, Material.HANDRAIL);
        solids.add(Solid.box(new double[]{x - half, 0, rung - .14}, new double[]{x + half, top, rung + ColumnStructure.RUNG_FACE}));
        climbs.add(new double[]{x - cage - .05, -.1, rung - .05, x + cage + .05, top, rung + 2 * cage + .1});
    }

    // ---- The overhead -----------------------------------------------------------------------------------------------

    /**
     * The overhead line: up from the top head to {@code high}, across to the down-leg at {@code xDown}, down to
     * {@code low} and over the structure to the inlet at {@code xInlet}. With the ejector, the line ends at its suction,
     * the ejector stands upright in the down-leg (chamber at {@code high}, diffuser below) and a line of the diffuser's
     * size continues down; its motive steam enters the steam chest on top.
     */
    private void overhead(EquipmentShape.Nozzle top, double od, double lineB, double xDown, double high, double low, double xInlet, double inletFace, double ds) {
        double[] face = top.face();
        // A water condenser's inlet is a nozzle; an air cooler's manifold takes the line as a tee.
        if (!ejector) {
            pipe(od, true, water, face, new double[]{a, high, a}, new double[]{xDown, high, a}, new double[]{xDown, low, a}, new double[]{xInlet, low, a},
                    new double[]{xInlet, inletFace, a});
            return;
        }
        // Lengths in suction diameters (figure 05): steam chest 0.8, chamber 1.5 (1.2 wide), inlet 2.2, throat 0.8
        // (0.35 wide), diffuser 3.0 (to 0.75 wide).
        double chamber = .6 * ds, chestTop = high + (.75 + .8) * ds;
        var f = Frame.of(new double[]{xDown, high + .75 * ds, a}, 1, -1);
        int n = sides(1.2 * ds);
        double z = 0;
        mesh.cylinder(f, .225 * ds, -.8 * ds, 0, n, Material.MACHINE);
        mesh.ring(f, .225 * ds, .3 * ds, -.08 * ds, 0, n, 3, Material.FLANGE);
        mesh.prism(f, chamber, z, z + 1.5 * ds, n, Material.MACHINE);
        mesh.annulus(f, 0, chamber, z, n, -1, Material.MACHINE);
        z += 1.5 * ds;
        mesh.frustum(f, chamber, .175 * ds, z, z + 2.2 * ds, n, Material.MACHINE);
        z += 2.2 * ds;
        mesh.prism(f, .175 * ds, z, z + .8 * ds, n, Material.MACHINE);
        z += .8 * ds;
        mesh.frustum(f, .175 * ds, lineB / 2, z, z + 3 * ds, n, Material.MACHINE);
        z += 3 * ds;
        double exit = high + .75 * ds - z;
        mesh.ring(f, 0, PipeSizes.flange(lineB) / 2, z - PipeSizes.flangeThickness(lineB), z, n, 1, Material.FLANGE);
        solids.add(Solid.rod(new double[]{xDown, 0, a}, 1, exit, chestTop, chamber, 0, 0));
        // Motive steam: a stub up from the steam chest.
        stub(new double[]{xDown, chestTop - .02, a}, 1, 1, chestTop + NECK, Math.max(.1, ColumnStructure.size(.3 * ds, .5)));
        ejectorTop = chestTop + .3;
        ejectorBraces = high - chamber - .4 - od / 2;
        // Suction: on the chamber's side towards the column.
        double suction = xDown + chamber + NECK;
        nozzle(new double[]{xDown + chamber - .05, high, a}, 0, 1, suction, od, false);
        pipe(od, true, true, face, new double[]{a, high, a}, new double[]{suction, high, a});
        pipe(lineB, false, water, new double[]{xDown, exit, a}, new double[]{xDown, low, a}, new double[]{xInlet, low, a}, new double[]{xInlet, inletFace, a});
    }

    // ---- The air cooler ---------------------------------------------------------------------------------------------

    /**
     * The forced-draft air cooler on its own stand from x {@code x0} to {@code x1}: {@code bays} bays side by side along
     * z, each a finned-tube bundle at {@code hb} between a split inlet-outlet header (inner) and a return header (outer),
     * a plenum under it and two fans under that, their motors below; legs under the headers. The overhead tees into a
     * manifold over the inner header, which drops into each bay.
     */
    private void airCooler(double x0, double x1, int bays, double bay, double length, double fan, double header, double bundle, double plenum, double hb,
                           double xInlet, double inletFace, double lineB) {
        double z0 = a - bays * bay / 2, z1 = a + bays * bay / 2, low = hb - .05, high = hb + bundle + .05;
        // Headers and bundles.
        box(new double[]{x1 - header, low, z0}, new double[]{x1, high, z1}, Material.MACHINE, true);
        box(new double[]{x0, low, z0}, new double[]{x0 + header, high, z1}, Material.MACHINE, true);
        for (int k = 0; k < bays; k++) {
            double b0 = z0 + k * bay, b1 = b0 + bay;
            box(new double[]{x0 + header, hb, b0 + .06}, new double[]{x1 - header, hb + bundle, b1 - .06}, Material.GRATING, true);
            for (double z : new double[]{b0 + .03, b1 - .03})
                box(new double[]{x0 + header, hb - .02, z - .03}, new double[]{x1 - header, hb + bundle + .02, z + .03}, Material.STEELWORK, false);
            // Plenum: tapering under the bundle to its floor at the fans.
            double inset = .15 * plenum, bottom = hb - plenum;
            double[][] topRect = {{x0 + header, b0 + .06}, {x1 - header, b0 + .06}, {x1 - header, b1 - .06}, {x0 + header, b1 - .06}};
            double[][] lowRect = {{x0 + header + inset, b0 + .06 + inset}, {x1 - header - inset, b0 + .06 + inset}, {x1 - header - inset, b1 - .06 - inset},
                    {x0 + header + inset, b1 - .06 - inset}};
            var centre = new double[]{(x0 + x1) / 2, hb - plenum / 2, (b0 + b1) / 2};
            for (int c = 0; c < 4; c++) {
                var p = topRect[c];
                var q = topRect[(c + 1) % 4];
                var r = lowRect[(c + 1) % 4];
                var s = lowRect[c];
                mesh.add(List.of(new double[]{p[0], hb, p[1]}, new double[]{q[0], hb, q[1]}, new double[]{r[0], bottom, r[1]}, new double[]{s[0], bottom, s[1]}), centre,
                        FLAT, Material.CLADDING, null, null);
            }
            mesh.flat(List.of(new double[]{lowRect[0][0], bottom, lowRect[0][1]}, new double[]{lowRect[1][0], bottom, lowRect[1][1]},
                    new double[]{lowRect[2][0], bottom, lowRect[2][1]}, new double[]{lowRect[3][0], bottom, lowRect[3][1]}), new double[]{0, -1, 0}, Material.CLADDING);
            solids.add(Solid.box(new double[]{x0 + header, bottom, b0}, new double[]{x1 - header, hb + bundle, b1}));
            volumes.add(Solid.box(new double[]{x0 + header + inset + .02, bottom + .02, b0 + .06 + inset + .02},
                    new double[]{x1 - header - inset - .02, hb - .02, b1 - .06 - inset - .02}));
            // Fans, each in a ring under the plenum floor, its motor hanging below on a beam.
            double zc = (b0 + b1) / 2, ring = .3, motorR = Math.max(.15, Math.min(.35, .12 * fan)), motorL = .5 + .1 * fan;
            double inner = x0 + header, span = length - 2 * header;
            double motorBottom = bottom - ring - .05 - motorL;
            for (double t : new double[]{.25, .75}) {
                double xc = inner + span * t;
                var f = Frame.of(new double[]{xc, bottom, zc}, 1, 1);
                int n = Math.max(16, sides(2 * fan));
                mesh.ring(f, fan, fan + .05, -ring, 0, n, 1, Material.MACHINE);
                mesh.cylinder(f, .12 * fan, -ring + .05, -.05, 12, Material.MACHINE);
                for (int blade = 0; blade < 4; blade++) {
                    double angle = Math.PI / 2 * blade + Math.PI / 8;
                    var tip = f.at(angle, .93 * fan, -ring / 2);
                    mesh.bar(f.at(angle, .12 * fan, -ring / 2), tip, .06, f.at(angle + Math.PI / 2, 1, 0), Material.MACHINE);
                }
                mesh.cylinder(f, motorR, -ring - .05 - motorL, -ring - .05, 12, Material.MOTOR);
                mesh.bar(f.along(-ring - .05), f.along(-ring + .05), .06, new double[]{1, 0, 0}, Material.STEELWORK);
                solids.add(Solid.rod(new double[]{xc, 0, zc}, 1, bottom - ring, bottom, fan + .05, 0, 0));
                solids.add(Solid.rod(new double[]{xc, 0, zc}, 1, motorBottom, bottom - ring, motorR, 0, 0));
            }
            box(new double[]{x0 + .05, motorBottom - BEAM, zc - FLANGE_BEAM / 2}, new double[]{x1 - .05, motorBottom, zc + FLANGE_BEAM / 2}, Material.STEELWORK, true);
        }
        // Legs under the headers at each bay's edge, beams across under the headers and under the motors.
        double motorLevel = hb - plenum - .3 - .05 - (.5 + .1 * fan);
        for (double x : new double[]{x0 + POST / 2, x1 - POST / 2}) {
            for (int k = 0; k <= bays; k++) {
                double z = Math.max(z0 + POST / 2, Math.min(z1 - POST / 2, z0 + k * bay));
                box(new double[]{x - POST / 2, 0, z - POST / 2}, new double[]{x + POST / 2, low, z + POST / 2}, Material.STEELWORK, true);
            }
            box(new double[]{x - FLANGE_BEAM / 2, low - BEAM, z0}, new double[]{x + FLANGE_BEAM / 2, low, z1}, Material.STEELWORK, true);
            box(new double[]{x - FLANGE_BEAM / 2, motorLevel - 2 * BEAM, z0}, new double[]{x + FLANGE_BEAM / 2, motorLevel - BEAM, z1}, Material.STEELWORK, true);
        }
        // The inlet manifold over the inner header and its drops into each bay.
        double drop = Math.min(lineB, header - .1), manifold = hb + bundle + .05 + .3 + drop + lineB / 2;
        double first = bays == 1 ? a - lineB : z0 + bay / 2, last = bays == 1 ? a + lineB : z1 - bay / 2;
        var mf = Frame.of(new double[]{xInlet, manifold, first}, 2, 1);
        int nm = sides(lineB);
        mesh.prism(mf, lineB / 2, 0, last - first, nm, Material.PIPE);
        mesh.annulus(mf, 0, lineB / 2, 0, nm, -1, Material.PIPE);
        mesh.annulus(mf, 0, lineB / 2, last - first, nm, 1, Material.PIPE);
        solids.add(Solid.rod(new double[]{xInlet, manifold, 0}, 2, first, last, lineB / 2, 0, 0));
        for (int k = 0; k < bays; k++) {
            double zc = bays == 1 ? a : z0 + bay * (k + .5);
            pipe(drop, false, false, new double[]{xInlet, manifold, zc}, new double[]{xInlet, high, zc});
        }
        var outline = new double[][]{{x0, high, z0}, {x1, high, z0}, {x1, high, z1}, {x0, high, z1}};
        for (int k = 0; k < 4; k++) segment(outline[k], outline[(k + 1) % 4]);
    }

    // ---- Lines and nozzles ------------------------------------------------------------------------------------------

    /**
     * A line through {@code points} (each step along one frame axis) of outside diameter {@code od}: straight runs and an
     * elbow at every turn, of bend radius 1 D where the runs leave room and less where they do not (a ball joint where
     * not even half a diameter fits). A flange pairs it with a nozzle at an end marked {@code flangeStart} or
     * {@code flangeEnd}.
     */
    private void pipe(double od, boolean flangeStart, boolean flangeEnd, double[]... points) {
        var p = new ArrayList<double[]>();
        for (var q : points) if (p.isEmpty() || distance(p.getLast(), q) > 1e-6) p.add(q);
        if (p.size() < 2) return;
        lines++;
        double r = od / 2;
        int n = sides(od), count = p.size();
        var bends = new double[count];
        for (int i = 1; i + 1 < count; i++) {
            var din = unit(sub(p.get(i), p.get(i - 1)));
            var dout = unit(sub(p.get(i + 1), p.get(i)));
            if (dot(din, dout) > .999) continue;
            double before = distance(p.get(i - 1), p.get(i)) / (i - 1 == 0 ? 1 : 2), after = distance(p.get(i), p.get(i + 1)) / (i + 1 == count - 1 ? 1 : 2);
            bends[i] = Math.min(od, Math.min(before, after) - .01);
        }
        for (int i = 0; i + 1 < count; i++) {
            var from = p.get(i);
            var to = p.get(i + 1);
            var dir = unit(sub(to, from));
            double length = distance(from, to);
            double start = bends[i] >= r ? bends[i] : 0, end = length - (bends[i + 1] >= r ? bends[i + 1] : 0);
            var ref = Math.abs(dir[1]) > .9 ? new double[]{1, 0, 0} : new double[]{0, 1, 0};
            var f = new Frame(from, dir, ref);
            if (end > start + 1e-6) mesh.prism(f, r, start, end, n, Material.PIPE);
            int axis = Math.abs(dir[0]) > .9 ? 0 : Math.abs(dir[1]) > .9 ? 1 : 2;
            solids.add(Solid.rod(from.clone(), axis, Math.min(from[axis], to[axis]), Math.max(from[axis], to[axis]), r, 0, 0));
            runs.add(new double[]{from[0], from[1], from[2], to[0], to[1], to[2], r, lines});
            notParts.add(solids.getLast());
            if (i == 0 && flangeStart) mesh.ring(f, r, PipeSizes.flange(od) / 2, 0, PipeSizes.flangeThickness(od), n, 2, Material.FLANGE);
            if (i + 2 == count && flangeEnd) mesh.ring(f, r, PipeSizes.flange(od) / 2, length - PipeSizes.flangeThickness(od), length, n, 1, Material.FLANGE);
        }
        for (int i = 1; i + 1 < count; i++) {
            var din = unit(sub(p.get(i), p.get(i - 1)));
            var dout = unit(sub(p.get(i + 1), p.get(i)));
            if (dot(din, dout) > .999) continue;
            double b = bends[i];
            if (b >= r) {
                var centre = add(p.get(i), scale(din, -b), scale(dout, b));
                var f = new Frame(centre, cross(din, dout), neg(dout));
                mesh.torus(f, b, r, 0, Math.PI / 2, Math.max(3, Math.min(8, (int) Math.ceil(b * 6))), n, Material.PIPE);
            } else mesh.sphere(p.get(i), r, n, Material.PIPE);
        }
    }

    /**
     * A unit's nozzle connected to a line: a neck from {@code base} (under the unit's surface) along frame axis
     * {@code axis} in direction {@code sign} to its flange face at coordinate {@code face} on that axis, the line's
     * flange meeting it there.
     */
    private void nozzle(double[] base, int axis, int sign, double face, double od, boolean blind) {
        var dir = new double[3];
        dir[axis] = sign;
        var ref = new double[3];
        ref[(axis + 1) % 3] = 1;
        var f = new Frame(base, dir, ref);
        double length = Math.abs(face - base[axis]), t = PipeSizes.flangeThickness(od), flange = PipeSizes.flange(od) / 2;
        int n = sides(od);
        mesh.prism(f, od / 2, 0, length - t, n, Material.PIPE);
        mesh.annulus(f, od / 2, flange, length - t, n, -1, Material.FLANGE);
        mesh.prism(f, flange, length - t, length, n, Material.FLANGE);
        if (blind) mesh.annulus(f, 0, flange, length, n, 1, Material.FLANGE);
        double[] centre = base.clone();
        solids.add(Solid.rod(centre, axis, Math.min(base[axis], face), Math.max(base[axis], face), od / 2, 0, 0));
    }

    /** An unconnected nozzle under a blind flange: a utility or product end. */
    private void stub(double[] base, int axis, int sign, double face, double od) {
        nozzle(base, axis, sign, face, od, true);
    }

    /** An axis-aligned box, every face drawn; {@code solid} for one a player collides with. */
    private void box(double[] lo, double[] hi, Material material, boolean solid) {
        mesh.box(lo, hi, ALL_FACES, material);
        if (solid) solids.add(Solid.box(lo.clone(), hi.clone()));
    }

    private void segment(double[] p, double[] q) {
        edges.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
    }

    private static int sides(double od) {
        return Math.max(12, PipeSizes.sides(od));
    }

    private static double distance(double[] p, double[] q) {
        double dx = p[0] - q[0], dy = p[1] - q[1], dz = p[2] - q[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] add(double[] p, double[] u, double[] v) {
        return new double[]{p[0] + u[0] + v[0], p[1] + u[1] + v[1], p[2] + u[2] + v[2]};
    }
}
