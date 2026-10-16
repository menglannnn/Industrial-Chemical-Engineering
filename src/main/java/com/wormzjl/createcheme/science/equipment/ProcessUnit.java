package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * A standalone process unit (owner request and decisions 2026-10-16, batch 2026-10-15-custom-equipment-rendering,
 * {@code STANDALONE_UNITS_PLAN.md}): a shell-and-tube heat exchanger, a kettle reboiler, an air cooler bay or a steam
 * ejector, drawn only. The player places it with the constructor and runs the pipes; its nozzles take the pipe's size.
 *
 * <p>Sizes, from the physics of the plan. An exchanger (TEMA AES) of shell D and tubes L holds about 1,260 D² tubes of
 * 3/4" on a 1" pitch, about 75 D² L m² of surface; its tubes are at least 3 D long. A kettle's bundle of D_b lies in a
 * shell of 1.6 D_b with vapour space over the pool. An air cooler is a bank of single-pass bays in parallel and in series
 * ({@link Bank}), each 1.5 to 3 m wide and 2 to 6 m long where its one to three fans cover at least 40 % of its bundle
 * face (API 661; owner requests 2026-10-16: smaller bays, banks), its fans, rings and motors Blender parts ({@link Part}).
 * An ejector's proportions follow its suction s (8.3 s long).
 *
 * <p>Built in the unit's frame, which stands as a vertical vessel's ({@link EquipmentShape}): y up from the ground, the
 * front (frame -z) towards the constructor, a lying unit's axis along z with its channel (the air cooler's inlet header)
 * at the front. Every nozzle's centre line runs through cell centres and its flange face lies on a block face; the block
 * beyond each face is left free for a pipe. {@code diameter}, {@code tangentLength} and {@code support} of the shape are
 * the unit's own sizes: an exchanger's shell, tube length and the height of its shell's bottom; a kettle's bundle, tube
 * length and the height of its kettle's bottom; an air cooler's bay width, bay length and the height of its bundle; an
 * ejector's suction, 1 (unused) and the height of its discharge flange.
 */
public final class ProcessUnit {
    private static final int CACHE = 64;
    private static final Map<EquipmentShape, ProcessUnit> BUILT = new ConcurrentHashMap<>();

    /**
     * Steps of an exchanger's shell and a kettle's bundle, and of an ejector's suction, m; the largest suction of an
     * ejector on a single post (owner request 2026-10-16: "allow smaller ejectors").
     */
    public static final double SHELL_STEP = .25, SUCTION_STEP = .05, SMALL_EJECTOR = .2;
    /**
     * An air cooler bay's lengths, m (header to header, whole metres; owner request 2026-10-16: "allow smaller ejectors and
     * ACs"); its width runs from 1.5 to 3 m in 0.5 m steps. A bay is built only where its fans cover {@link #FAN_COVERAGE}
     * of its bundle face (API 661), which a bay under 1.5 m wide never does.
     */
    public static final int MIN_BAY_LENGTH = 2, MAX_BAY_LENGTH = 6;
    public static final double FAN_COVERAGE = .4;
    /**
     * Air cooler parts, m: the largest header depth along the tubes, a header's height, the bundle's depth, and the
     * deepest a fan ring and guard hang under the plenum (the Blender ring is 0.35 of its fan deep).
     */
    static final double HEADER = .6, HEADER_HEIGHT = .6, BUNDLE = .5, RING_DEPTH = .9;

    /**
     * An air cooler bank (owner request 2026-10-16: "AC should be able to be built in series & parallel", decided: one bank
     * unit with its manifolds and crossovers): {@code parallel} trains of bays side by side across the frame's x, each
     * {@code series} bays back to back along z. Single-pass bays: the process enters a train at its first bay's front
     * header and crosses from each back header to the next front header; the bank has one inlet and one outlet.
     */
    public record Bank(int parallel, int series) {
        public static final Bank ONE = new Bank(1, 1);
        public static final int MAX_PARALLEL = 6, MAX_SERIES = 4;

        public Bank {
            if (parallel < 1 || parallel > MAX_PARALLEL || series < 1 || series > MAX_SERIES)
                throw new IllegalArgumentException("A bank is 1 to " + MAX_PARALLEL + " bays in parallel by 1 to " + MAX_SERIES + " in series, not " + parallel + " × " + series);
        }

        public int bays() {
            return parallel * series;
        }

        public int[] encode() {
            return new int[]{parallel, series};
        }

        /** @throws IllegalArgumentException unless {@code values} holds the bays in parallel and in series, in range */
        public static Bank decode(int[] values) {
            if (values.length != 2) throw new IllegalArgumentException("A bank holds its bays in parallel and in series");
            return new Bank(values[0], values[1]);
        }
    }

    /** A bay's fans: how many, along its tubes, and their diameter, m. */
    public record Fans(int count, double diameter) {
    }
    /** Steelwork, m: a post's section, a beam's depth and flange. */
    static final double POST = .2, BEAM = .2, FLANGE_BEAM = .15;

    // ---- Sizes ------------------------------------------------------------------------------------------------------

    public static double minDiameter(Kind kind) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> .5;
            case AIR_COOLER -> 1.5;
            case EJECTOR -> SUCTION_STEP;
            default -> throw notUnit(kind);
        };
    }

    public static double maxDiameter(Kind kind) {
        return switch (kind) {
            case EXCHANGER -> 2.5;
            case KETTLE -> 2;
            case AIR_COOLER -> 3;
            case EJECTOR -> 1;
            default -> throw notUnit(kind);
        };
    }

    public static double diameterStep(Kind kind) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> SHELL_STEP;
            case AIR_COOLER -> .5;
            case EJECTOR -> SUCTION_STEP;
            default -> throw notUnit(kind);
        };
    }

    /**
     * The shortest tubes, whole metres: three bundle diameters; an air cooler bay of width {@code diameter}, the shortest
     * whose fans cover {@link #FAN_COVERAGE} of its bundle face; an ejector's 1, unused.
     */
    public static int minLength(Kind kind, double diameter) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> Math.max(2, (int) Math.ceil(3 * diameter - 1e-9));
            case AIR_COOLER -> {
                int length = MIN_BAY_LENGTH;
                while (length < MAX_BAY_LENGTH && coverage(diameter, length) < FAN_COVERAGE) length++;
                yield length;
            }
            case EJECTOR -> 1;
            default -> throw notUnit(kind);
        };
    }

    public static int maxLength(Kind kind) {
        return switch (kind) {
            case EXCHANGER -> 12;
            case KETTLE -> 10;
            case AIR_COOLER -> MAX_BAY_LENGTH;
            case EJECTOR -> 1;
            default -> throw notUnit(kind);
        };
    }

    /**
     * The lowest support, m: a lying unit's bottom 2 m up leaves a block under its bottom nozzles for their pipes; an air
     * cooler's bundle 5 m up leaves 2.5 m under its fan motors; an ejector's discharge 1 m up.
     */
    public static int minSupport(Kind kind) {
        return switch (kind) {
            case EXCHANGER, KETTLE -> 2;
            case AIR_COOLER -> 5;
            case EJECTOR -> 1;
            default -> throw notUnit(kind);
        };
    }

    public static int defaultSupport(Kind kind) {
        return kind == Kind.AIR_COOLER ? 6 : 2;
    }

    /** An ejector stands; the others lie. */
    public static Orientation orientation(Kind kind) {
        return kind == Kind.EJECTOR ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }

    /** A diameter on its kind's grid, cleared of rounding (0.30000000000000004 becomes 0.3). */
    public static double snap(Kind kind, double diameter) {
        double step = diameterStep(kind), min = minDiameter(kind);
        double k = Math.max(0, Math.min(Math.round((maxDiameter(kind) - min) / step), Math.round((diameter - min) / step)));
        return Math.round((min + k * step) * 1000) / 1000.0;
    }

    static void validate(Kind kind, double diameter, double length, Orientation orientation, int support, Bank bank) {
        String name = kind.name().toLowerCase(Locale.ROOT);
        if (kind != Kind.AIR_COOLER && !bank.equals(Bank.ONE)) throw new IllegalArgumentException("Only air coolers are built in banks");
        if (!(diameter >= minDiameter(kind) - 1e-9 && diameter <= maxDiameter(kind) + 1e-9) || Math.abs(snap(kind, diameter) - diameter) > 1e-9)
            throw new IllegalArgumentException("A " + name + "'s size must be " + minDiameter(kind) + " to " + maxDiameter(kind) + " m in " + diameterStep(kind)
                    + " m steps, not " + diameter);
        if (length != Math.rint(length) || length < minLength(kind, diameter) || length > maxLength(kind))
            throw new IllegalArgumentException("A " + name + "'s length must be " + minLength(kind, diameter) + " to " + maxLength(kind) + " whole metres, not " + length);
        if (orientation != orientation(kind)) throw new IllegalArgumentException("A " + name + " " + (orientation(kind).horizontal() ? "lies" : "stands"));
        if (support < minSupport(kind) || support > EquipmentShape.MAX_SUPPORT)
            throw new IllegalArgumentException("A " + name + "'s support must be " + minSupport(kind) + " to " + EquipmentShape.MAX_SUPPORT + " m, not " + support);
        if (kind == Kind.AIR_COOLER && coverage(diameter, length) < FAN_COVERAGE)
            throw new IllegalArgumentException("An air cooler bay of " + diameter + " × " + length + " m has no room for fans covering " + FAN_COVERAGE + " of its bundle");
    }

    private static IllegalArgumentException notUnit(Kind kind) {
        return new IllegalArgumentException(kind + " is not a unit");
    }

    /** Tube surface of an exchanger or a kettle's bundle of shell {@code shell} and tubes {@code tubes}, m². */
    public static double tubeSurface(double shell, double tubes) {
        return 75 * shell * shell * tubes;
    }

    /** A kettle's shell over its bundle: 1.6 times, to 0.25 m. */
    public static double kettleShell(double bundle) {
        return Math.round(1.6 * bundle * 4) / 4.0;
    }

    /** An air cooler bay's header depth along its tubes, m: 0.15 of its length, at most {@link #HEADER}. */
    static double header(double length) {
        return Math.min(HEADER, .15 * length);
    }

    /** An air cooler bay's bundle face between its headers (centred 0.5 m in from each end), m². */
    public static double bundleFace(double width, double length) {
        return (width - .22) * (length - 1 - header(length));
    }

    /**
     * An air cooler bay's fans: one to three along its tubes, as many as cover its bundle face best, each as large as fits
     * its share of the plenum's floor (0.42 m narrower than the bay, 0.1 m shorter than the bundle) less 0.1 m.
     */
    public static Fans fans(double width, double length) {
        Fans best = null;
        double covered = -1;
        for (int k = 1; k <= 3; k++) {
            double d = Math.min(width - .42, (length - 1.1 - header(length)) / k) - .1, c = d > 0 ? k * Math.PI * d * d / 4 : 0;
            if (c > covered + 1e-9) {
                covered = c;
                best = new Fans(k, d);
            }
        }
        return best;
    }

    /** Every air cooler bay a player may build, {width, length}, the narrowest and shortest first. */
    public static List<double[]> bays() {
        var out = new ArrayList<double[]>();
        for (double w = minDiameter(Kind.AIR_COOLER); w <= maxDiameter(Kind.AIR_COOLER) + 1e-9; w += diameterStep(Kind.AIR_COOLER))
            for (int l = minLength(Kind.AIR_COOLER, w); l <= MAX_BAY_LENGTH; l++) if (coverage(w, l) >= FAN_COVERAGE) out.add(new double[]{snap(Kind.AIR_COOLER, w), l});
        return out;
    }

    /** The share of an air cooler bay's bundle face its fans cover (API 661 asks 0.4 at least). */
    public static double coverage(double width, double length) {
        var fans = fans(width, length);
        return fans.count() * Math.PI * fans.diameter() * fans.diameter() / 4 / bundleFace(width, length);
    }

    /** An ejector's length from its steam chest to its diffuser's exit, m: 8.3 suction diameters. */
    public static double ejectorLength(double suction) {
        return 8.3 * suction;
    }

    static ProcessUnit of(EquipmentShape shape) {
        var built = BUILT.get(shape);
        if (built != null) return built;
        if (BUILT.size() >= CACHE) BUILT.clear();
        return BUILT.computeIfAbsent(shape, s -> new ProcessUnit(s, false));
    }

    // ---- Collision parts --------------------------------------------------------------------------------------------

    /**
     * A solid part in the frame: an axis-aligned box ({@code radius} 0), or a cylinder along frame axis {@code axis}
     * between {@code lo} and {@code hi} on it, its radius going from {@code radius} at {@code lo} to {@code radiusHi} at
     * {@code hi} round the line through {@code centre} (a cone where they differ), with a 2:1 head of depth {@code headHi}
     * beyond its high end (0: flat).
     */
    private record Solid(double[] min, double[] max, int axis, double[] centre, double radius, double radiusHi, double lo, double hi, double headHi) {
        static Solid box(double[] min, double[] max) {
            return new Solid(min, max, -1, null, 0, 0, 0, 0, 0);
        }

        static Solid rod(double[] centre, int axis, double lo, double hi, double radius, double headHi) {
            return cone(centre, axis, lo, hi, radius, radius, headHi);
        }

        static Solid cone(double[] centre, int axis, double lo, double hi, double radius, double radiusHi, double headHi) {
            double[] min = centre.clone(), max = centre.clone();
            double widest = Math.max(radius, radiusHi);
            for (int a = 0; a < 3; a++) {
                min[a] -= widest;
                max[a] += widest;
            }
            min[axis] = lo;
            max[axis] = hi + headHi;
            return new Solid(min, max, axis, centre, radius, radiusHi, lo, hi, headHi);
        }

        boolean contains(double[] p) {
            for (int a = 0; a < 3; a++) if (p[a] < min[a] || p[a] > max[a]) return false;
            if (axis < 0) return true;
            double rho2 = 0;
            for (int a = 0; a < 3; a++) if (a != axis) rho2 += (p[a] - centre[a]) * (p[a] - centre[a]);
            double s = p[axis];
            if (s <= hi) {
                double r = hi > lo ? radius + (radiusHi - radius) * (s - lo) / (hi - lo) : radius;
                return rho2 <= r * r;
            }
            double out = (s - hi) / headHi;
            return headHi > 0 && out * out + rho2 / (radiusHi * radiusHi) <= 1;
        }
    }

    // ---- The unit ---------------------------------------------------------------------------------------------------

    private final EquipmentShape shape;
    /** The mesh being drawn: the cached unit drops it once built; {@link #mesh()} draws the facets again. */
    private Mesh mesh = new Mesh();
    private final List<Solid> solids = new ArrayList<>(), volumes = new ArrayList<>();
    private final Map<Long, List<Solid>> solidCells = new HashMap<>(), volumeCells = new HashMap<>();
    private final List<Nozzle> nozzles = new ArrayList<>();
    /** Fixed outline edges, {x0, y0, z0, x1, y1, z1}; round bodies outlined against the eye, {x, y, z on the axis, axis, radius, lo, hi}. */
    private final List<double[]> edges = new ArrayList<>(), rounds = new ArrayList<>();
    private final double[] box;

    private ProcessUnit(EquipmentShape s, boolean drawing) {
        shape = s;
        switch (s.kind()) {
            case EXCHANGER -> exchanger();
            case KETTLE -> kettle();
            case AIR_COOLER -> airCooler();
            case EJECTOR -> ejector();
            default -> throw notUnit(s.kind());
        }
        box = box(mesh.bounds());
        if (drawing) return;
        mesh = null;
        index(solids, solidCells);
        index(volumes, volumeCells);
    }

    /** The parts' facets in the frame, drawn afresh: the layout is deterministic. */
    Mesh mesh() {
        return new ProcessUnit(shape, true).mesh;
    }

    List<Nozzle> nozzles() {
        return List.copyOf(nozzles);
    }

    /** The box in the frame, {x0, y0, z0, x1, y1, z1}: the parts and every nozzle's flange face, in whole cells. */
    double[] box() {
        return box.clone();
    }

    /** Length along the frame's z (a lying unit's axis), m. */
    double length() {
        return box[5] - box[2];
    }

    /** Height of the unit's top (its parts, not its nozzles) above the ground, m. */
    double height() {
        return top;
    }

    private double top;

    private double[] box(double[] bounds) {
        double[] out = {Math.floor(bounds[0] + 1e-9), 0, Math.floor(bounds[2] + 1e-9), Math.ceil(bounds[3] - 1e-9), Math.ceil(bounds[4] - 1e-9), Math.ceil(bounds[5] - 1e-9)};
        top = bounds[4];
        for (var n : nozzles) {
            double face = n.face()[n.axis()];
            if (n.sign() > 0) out[n.axis() + 3] = Math.max(out[n.axis() + 3], face);
            else out[n.axis()] = Math.min(out[n.axis()], face);
        }
        return out;
    }

    /** Whether a frame point lies in a part a player collides with. */
    boolean inside(double[] p) {
        return within(solidCells, p);
    }

    /** Whether a frame point lies inside a closed body (a shell, a plenum): no cell there is open. */
    boolean filled(double[] p) {
        return within(volumeCells, p);
    }

    /** Outline edges in the frame: the round bodies' rims and the air cooler's bundle. */
    List<double[]> edges() {
        return edges;
    }

    /** The round bodies' outlines seen from frame point {@code eye}: the two lines along each body where it turns away. */
    void silhouette(double[] eye, List<double[]> out) {
        for (var r : rounds) {
            int ax = (int) r[3], u = (ax + 1) % 3, v = (ax + 2) % 3;
            double radius = r[4], du = eye[u] - r[u], dv = eye[v] - r[v], d = Math.sqrt(du * du + dv * dv);
            if (d <= radius) continue;
            double towards = DeterministicMath.atan2(dv, du), spread = DeterministicMath.acos(radius / d);
            for (double angle : new double[]{towards - spread, towards + spread}) {
                double[] p = {r[0], r[1], r[2]}, q = {r[0], r[1], r[2]};
                p[u] += radius * DeterministicMath.cos(angle);
                p[v] += radius * DeterministicMath.sin(angle);
                q[u] = p[u];
                q[v] = p[v];
                p[ax] = r[5];
                q[ax] = r[6];
                out.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
            }
        }
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

    // ---- Shell-and-tube exchanger -----------------------------------------------------------------------------------

    /**
     * A TEMA AES exchanger lying along z on two saddles, its bottom {@code support} up: from the front, the channel's
     * cover, the channel (tube side in at its bottom, out at its top), the tubesheet's flange, the shell (shell side in
     * on top near the back, out at the bottom near the front), the floating head's flange and its cover.
     */
    private void exchanger() {
        double d = shape.diameter(), r = d / 2, y = shape.support() + r, a = centre(1.1 * r);
        int n = sides(d);
        double channel = .5 * d + .2, zn = Math.ceil(.08 + channel / 2 - .5) + .5;
        double c0 = zn - channel / 2 - .08, ch1 = zn + channel / 2, s0 = ch1 + .1, s1 = s0 + .95 * shape.tangentLength(), end = s1 + .1;
        var f = new Frame(new double[]{a, y, 0}, new double[]{0, 0, 1}, new double[]{0, 1, 0});
        mesh.ring(f, 0, 1.1 * r, c0, c0 + .08, n, 3, Material.FLANGE);
        mesh.prism(f, r, c0 + .08, ch1, n, Material.SHELL);
        mesh.ring(f, r, 1.1 * r, ch1, s0, n, 3, Material.FLANGE);
        mesh.prism(f, r, s0, s1, n, Material.SHELL);
        mesh.ring(f, r, 1.08 * r, s1, end, n, 3, Material.FLANGE);
        head(f, r, end, d / 4, n, Material.HEAD);
        solids.add(Solid.rod(new double[]{a, y, 0}, 2, c0, end, 1.05 * r, d / 4));
        volumes.add(Solid.rod(new double[]{a, y, 0}, 2, c0 + .1, end, .94 * r, .9 * d / 4));
        round(a, y, c0, end, r);

        double tube = Math.min(1, .4 * d), shellOd = Math.min(1, d / 2), flange = PipeSizes.flange(shellOd);
        nozzles.add(radial(Role.TUBE_INLET, a, y, r, n, zn, -1, tube, ColumnStructure.size(.25 * d, tube)));
        nozzles.add(radial(Role.TUBE_OUTLET, a, y, r, n, zn, 1, tube, ColumnStructure.size(.25 * d, tube)));
        double zIn = Math.floor(s1 - flange / 2 - .1 - .5) + .5, zOut = Math.ceil(s0 + flange / 2 + .1 - .5) + .5;
        nozzles.add(radial(Role.SHELL_INLET, a, y, r, n, zIn, 1, shellOd, ColumnStructure.size(.3 * d, shellOd)));
        nozzles.add(radial(Role.SHELL_OUTLET, a, y, r, n, zOut, -1, shellOd, ColumnStructure.size(.3 * d, shellOd)));
        // The front saddle stands beyond the outlet's pipe block, the back one a fifth of the shell in from its end.
        double w = saddleWidth(r), length = s1 - s0;
        saddle(Math.max(s0 + .2 * length, zOut + .55 + w / 2), a, y, r, w);
        saddle(s1 - .2 * length, a, y, r, w);
    }

    // ---- Kettle reboiler --------------------------------------------------------------------------------------------

    /**
     * A kettle reboiler (TEMA AKT) lying along z, its kettle's bottom {@code support} up: the channel at the front (steam
     * in at its top, condensate out at its bottom), a cone up to the kettle's shell (1.6 times the bundle), the shell
     * (liquid fed low at its front, vapour out on top) and its back head, where the product leaves low behind the weir.
     */
    private void kettle() {
        double db = shape.diameter(), rb = db / 2, dk = kettleShell(db), rk = dk / 2, y = shape.support() + rk, a = centre(rk);
        int n = sides(dk), nb = sides(db);
        double channel = .5 * db + .2, zn = Math.ceil(.08 + channel / 2 - .5) + .5;
        double c0 = zn - channel / 2 - .08, ch1 = zn + channel / 2, cone = Math.max(.3, .6 * (dk - db)), s0 = ch1 + .1 + cone, s1 = s0 + .9 * shape.tangentLength() + .3;
        var f = new Frame(new double[]{a, y, 0}, new double[]{0, 0, 1}, new double[]{0, 1, 0});
        mesh.ring(f, 0, 1.1 * rb, c0, c0 + .08, nb, 3, Material.FLANGE);
        mesh.prism(f, rb, c0 + .08, ch1, nb, Material.SHELL);
        mesh.ring(f, rb, 1.12 * rb, ch1, ch1 + .1, nb, 3, Material.FLANGE);
        mesh.frustum(f, 1.05 * rb, rk, ch1 + .1, s0, n, Material.SHELL);
        mesh.prism(f, rk, s0, s1, n, Material.SHELL);
        head(f, rk, s1, dk / 4, n, Material.HEAD);
        // Channel, cone and kettle collide as drawn and enclose their insides.
        var axis = new double[]{a, y, 0};
        solids.add(Solid.rod(axis, 2, c0, ch1 + .1, 1.12 * rb, 0));
        solids.add(Solid.cone(axis, 2, ch1 + .1, s0, 1.05 * rb, rk, 0));
        solids.add(Solid.rod(axis, 2, s0, s1, rk, dk / 4));
        volumes.add(Solid.rod(axis, 2, c0 + .1, ch1 + .1, .94 * rb, 0));
        volumes.add(Solid.cone(axis, 2, ch1 + .1, s0 + .02, .94 * 1.05 * rb, .94 * rk, 0));
        volumes.add(Solid.rod(axis, 2, s0 + .02, s1, .94 * rk, .9 * dk / 4));
        round(a, y, c0, ch1 + .1, rb);
        round(a, y, s0, s1, rk);

        double tube = Math.min(1, .4 * db), liquid = Math.min(1, db / 2), vapour = Math.min(1, dk / 2), flange = PipeSizes.flange(liquid);
        nozzles.add(radial(Role.TUBE_INLET, a, y, rb, nb, zn, 1, tube, ColumnStructure.size(.25 * db, tube)));
        nozzles.add(radial(Role.TUBE_OUTLET, a, y, rb, nb, zn, -1, tube, ColumnStructure.size(.15 * db, tube)));
        double zFeed = Math.ceil(s0 + flange / 2 + .1 - .5) + .5;
        nozzles.add(radial(Role.FEED, a, y, rk, n, zFeed, -1, liquid, ColumnStructure.size(.15 * db, liquid)));
        nozzles.add(radial(Role.VAPOUR_OUTLET, a, y, rk, n, Math.floor((s0 + s1) / 2) + .5, 1, vapour, ColumnStructure.size(.3 * dk, vapour)));
        // The product: low on the back head, at the lowest cell centre 0.3 m over the shell's bottom.
        double yP = Math.ceil(shape.support() + .3 - .5) + .5, e = y - yP, depth = dk / 4;
        double od = Math.min(liquid, 2 * (.85 * rk - Math.abs(e)));
        double edge = (Math.abs(e) + od / 2) / rk, under = depth * Math.sqrt(Math.max(0, 1 - edge * edge)), start = s1 + under - .03;
        double nearest = Math.max(0, Math.abs(e) - od / 2) / rk, bulge = depth * Math.sqrt(Math.max(0, 1 - nearest * nearest));
        nozzles.add(new Nozzle(Role.LIQUID_OUTLET, new double[]{a, yP, start}, 2, 1, Math.ceil(s1 + depth + EquipmentShape.STAND_OUT) - start, od,
                ColumnStructure.size(.15 * db, od), 0, bulge - under + .03));
        double w = saddleWidth(rk), length = s1 - s0;
        saddle(Math.max(s0 + .2 * length, zFeed + .55 + w / 2), a, y, rk, w);
        saddle(s1 - .2 * length, a, y, rk, w);
    }

    // ---- Air cooler -------------------------------------------------------------------------------------------------

    /**
     * An air cooler bank of forced-draft single-pass bays ({@link Bank}), their tubes along z. Per bay: the inlet header at
     * its front and the outlet header at its back (each centred half a metre in from its end), the finned bundle between
     * them {@code support} up, a plenum under it tapering to its fans' floor, each fan (a Blender rotor in its ring and
     * guard) with its motor under it on a beam, and legs under the headers' ends (one shared where trains meet). Bays in
     * series join back header to front header by crossover spools. Trains in parallel take the process from an inlet
     * manifold over their first front headers, the bank's inlet on its top, and give it to an outlet manifold behind their
     * last back headers, the outlet under it where the condensate drains; a single train's inlet is on its first header and
     * its outlet under a stub behind its last.
     */
    private void airCooler() {
        var bank = shape.bank();
        int trains = bank.parallel(), rows = bank.series();
        double w = shape.diameter(), length = shape.tangentLength(), hb = shape.support(), h = header(length);
        double low = hb - .05, high = low + HEADER_HEIGHT, a = centre(trains * w / 2), x0 = a - trains * w / 2, x1 = x0 + trains * w, end = rows * length;
        // The fans' parts: the ring and guard hang at most RING_DEPTH m (scaled to 0.35 of it), the rotor turns half way down
        // the ring, the motor hangs under the guard, its shaft up to the rotor's hub; a small fan's motor is scaled down.
        var fans = fans(w, length);
        double d = fans.diameter(), up = Math.min(d, RING_DEPTH), bottom = hb - (.2 + .4 * d), rotor = bottom - .15 * up;
        double motor = Math.max(.45, Math.min(1, d / 1.9)), motorTop = bottom - .35 * up - .03, motorBottom = motorTop - .58 * motor;
        // A bay's nozzles and crossovers by its width; a manifold carries the trains' flow at the same speed.
        double stub = .1 + .04 * w, manifold = Math.min(HEADER_HEIGHT - .1, stub * Math.sqrt(trains));
        var fanZ = new ArrayList<Double>();
        for (int k = 0; k < rows; k++) {
            double front = k * length + .5, back = (k + 1) * length - .5, front1 = front + h / 2, back0 = back - h / 2;
            for (int i = 0; i < trains; i++) {
                double bx0 = x0 + i * w, bx1 = bx0 + w, mid = bx0 + w / 2, ix0 = bx0 + .11, ix1 = bx1 - .11;
                for (double zh : new double[]{front, back})
                    box(new double[]{bx0 + .05, low, zh - h / 2}, new double[]{bx1 - .05, high, zh + h / 2}, Material.AC_HEADER, true);
                box(new double[]{ix0, hb, front1}, new double[]{ix1, hb + BUNDLE, back0}, Material.AC_FINS, true);
                for (double x : new double[]{bx0 + .05, ix1})
                    box(new double[]{x, hb - .02, front1}, new double[]{x + .06, hb + BUNDLE + .02, back0}, Material.STEELWORK, false);
                plenum(ix0, ix1, front1, back0, hb, bottom);
                double share = (back0 - front1 - .1) / fans.count();
                for (int f = 0; f < fans.count(); f++) {
                    double zc = front1 + .05 + (f + .5) * share;
                    if (i == 0 && k == 0) fanZ.add(zc);
                    Part.of("ac_fan_ring").place(mesh, new double[]{mid, bottom, zc}, d, up, 0, Material.AC_RING);
                    Part.of("ac_fan").place(mesh, new double[]{mid, rotor, zc}, d, d, 0, Material.AC_FAN);
                    // Terminal boxes face out of the bank.
                    Part.of("ac_motor").place(mesh, new double[]{mid, motorTop, zc}, motor, motor, 2 * i < trains - 1 ? 2 : 0, Material.AC_MOTOR);
                    mesh.cylinder(Frame.of(new double[]{mid, 0, zc}, 1, 1), .025 * motor, motorTop, rotor - .06 * d, 8, Material.FLANGE);
                    solids.add(Solid.rod(new double[]{mid, 0, zc}, 1, bottom - .35 * up, bottom, .535 * d, 0));
                    solids.add(Solid.rod(new double[]{mid, 0, zc}, 1, motorBottom, motorTop, .2 * motor, 0));
                }
                if (k + 1 < rows) spool(mid, (low + high) / 2, back + h / 2, back + 1 - h / 2, stub);
            }
        }
        // Legs under the headers' ends; beams under the headers across the bank and along each leg line; at the motors'
        // level beams along the leg lines and across the bank under each row of motors.
        var legX = new ArrayList<Double>();
        legX.add(x0 + .15);
        for (int i = 1; i < trains; i++) legX.add(x0 + i * w);
        legX.add(x1 - .15);
        var legZ = new ArrayList<Double>();
        for (int k = 0; k < rows; k++) {
            legZ.add(k * length + .5);
            legZ.add((k + 1) * length - .5);
        }
        for (double x : legX)
            for (double z : legZ) box(new double[]{x - POST / 2, 0, z - POST / 2}, new double[]{x + POST / 2, low, z + POST / 2}, Material.STEELWORK, true);
        for (double z : legZ) box(new double[]{x0 + .05, low - BEAM, z - FLANGE_BEAM / 2}, new double[]{x1 - .05, low, z + FLANGE_BEAM / 2}, Material.STEELWORK, true);
        for (double x : legX)
            for (double y : new double[]{low, motorBottom})
                box(new double[]{x - FLANGE_BEAM / 2, y - BEAM, legZ.getFirst()}, new double[]{x + FLANGE_BEAM / 2, y, legZ.getLast()}, Material.STEELWORK, true);
        for (int k = 0; k < rows; k++)
            for (double zc : fanZ)
                box(new double[]{x0 + .15, motorBottom - BEAM, k * length + zc - FLANGE_BEAM / 2}, new double[]{x1 - .15, motorBottom, k * length + zc + FLANGE_BEAM / 2},
                        Material.STEELWORK, true);
        // The process in at the front, on top; out at the back, underneath.
        double zIn = .5, zOut = end + .5;
        if (trains == 1) nozzles.add(new Nozzle(Role.PROCESS_INLET, new double[]{a, high - .03, zIn}, 1, 1, Math.ceil(high + EquipmentShape.STAND_OUT) - high + .03,
                stub, stub, 0, .03));
        else {
            double ym = high + .1 + manifold / 2;
            manifold(x0 + w / 2, x1 - w / 2, ym, zIn, manifold);
            for (int i = 0; i < trains; i++) {
                var f = Frame.of(new double[]{x0 + (i + .5) * w, 0, zIn}, 1, 1);
                mesh.prism(f, stub / 2, high, ym, sides(stub), Material.PIPE);
                mesh.ring(f, stub / 2, PipeSizes.flange(stub) / 2, high, high + .03, sides(stub), 3, Material.FLANGE);
            }
            nozzles.add(new Nozzle(Role.PROCESS_INLET, new double[]{a, ym + manifold / 2 - .03, zIn}, 1, 1,
                    Math.ceil(ym + manifold / 2 + EquipmentShape.STAND_OUT) - ym - manifold / 2 + .03, manifold, manifold, 0, .03));
        }
        double out = trains == 1 ? stub : manifold, yo = low + .05 + stub / 2;
        for (int i = 0; i < trains; i++) {
            double mid = x0 + (i + .5) * w, z0 = end - .5 + h / 2;
            var f = Frame.of(new double[]{mid, yo, 0}, 2, 1);
            mesh.prism(f, stub / 2, z0, trains == 1 ? zOut + stub / 2 : zOut, sides(stub), Material.PIPE);
            mesh.ring(f, stub / 2, PipeSizes.flange(stub) / 2, z0, z0 + .03, sides(stub), 3, Material.FLANGE);
            if (trains == 1) mesh.annulus(f, 0, stub / 2, zOut + stub / 2, sides(stub), 1, Material.PIPE);
            solids.add(Solid.rod(new double[]{mid, yo, 0}, 2, z0, zOut, stub / 2, 0));
        }
        if (trains > 1) manifold(x0 + w / 2, x1 - w / 2, yo, zOut, manifold);
        double outBase = yo - out / 2 + .03;
        nozzles.add(new Nozzle(Role.PROCESS_OUTLET, new double[]{a, outBase, zOut}, 1, -1, outBase - Math.floor(yo - out / 2 - EquipmentShape.STAND_OUT), out, out, 0, .03));
        var outline = new double[][]{{x0 + .05, high, .5 - h / 2}, {x1 - .05, high, .5 - h / 2}, {x1 - .05, high, end - .5 + h / 2}, {x0 + .05, high, end - .5 + h / 2}};
        for (int k = 0; k < 4; k++) segment(outline[k], outline[(k + 1) % 4]);
    }

    /** A plenum from a bundle's underside (x0 to x1, z0 to z1 at {@code top}) tapering to its fans' floor at {@code bottom}. */
    private void plenum(double x0, double x1, double z0, double z1, double top, double bottom) {
        double inset = .1, gap = .05;
        double[][] upper = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        double[][] lower = {{x0 + inset, z0 + gap}, {x1 - inset, z0 + gap}, {x1 - inset, z1 - gap}, {x0 + inset, z1 - gap}};
        var centre = new double[]{(x0 + x1) / 2, (top + bottom) / 2, (z0 + z1) / 2};
        for (int c = 0; c < 4; c++) {
            var p = upper[c];
            var q = upper[(c + 1) % 4];
            var r = lower[(c + 1) % 4];
            var s = lower[c];
            mesh.add(List.of(new double[]{p[0], top, p[1]}, new double[]{q[0], top, q[1]}, new double[]{r[0], bottom, r[1]}, new double[]{s[0], bottom, s[1]}), centre, FLAT,
                    Material.AC_PLENUM, null, null);
        }
        var floor = new ArrayList<double[]>();
        for (int c = 3; c >= 0; c--) floor.add(new double[]{lower[c][0], bottom, lower[c][1]});
        mesh.flat(floor, new double[]{0, -1, 0}, Material.AC_PLENUM);
        // Collides as its section at mid-height; encloses its inside.
        solids.add(Solid.box(new double[]{x0 + inset / 2, bottom, z0 + gap / 2}, new double[]{x1 - inset / 2, top, z1 - gap / 2}));
        volumes.add(Solid.box(new double[]{x0 + inset + .02, bottom + .02, z0 + gap + .02}, new double[]{x1 - inset - .02, top - .02, z1 - gap - .02}));
    }

    /** A crossover spool along z at (x, y) from a back header's face {@code z0} to the next front header's {@code z1}, flanged at both. */
    private void spool(double x, double y, double z0, double z1, double od) {
        var f = Frame.of(new double[]{x, y, 0}, 2, 1);
        int n = sides(od);
        double r = PipeSizes.flange(od) / 2;
        mesh.prism(f, od / 2, z0, z1, n, Material.PIPE);
        mesh.ring(f, od / 2, r, z0, z0 + .03, n, 3, Material.FLANGE);
        mesh.ring(f, od / 2, r, z1 - .03, z1, n, 3, Material.FLANGE);
        solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0, z1, od / 2, 0));
    }

    /** A manifold along x from {@code from} to {@code to} (capped half its size beyond each) at height {@code y}, station {@code z}. */
    private void manifold(double from, double to, double y, double z, double od) {
        var f = Frame.of(new double[]{0, y, z}, 0, 1);
        mesh.cylinder(f, od / 2, from - od / 2, to + od / 2, sides(od), Material.PIPE);
        solids.add(Solid.rod(new double[]{0, y, z}, 0, from - od / 2, to + od / 2, od / 2, 0));
    }

    // ---- Steam ejector ----------------------------------------------------------------------------------------------

    /**
     * A steam ejector standing on its own frame, sized by its suction s: from the top, the steam chest (motive steam in on
     * top), the suction chamber (suction from the front), the inlet cone, the throat and the diffuser down to its discharge
     * flange, {@code support} up. Four posts round it from the ground to over the chest, tied by square frames every
     * 2.5 m, open at the front in the suction's row; a small one ({@link #SMALL_EJECTOR}), which a plant hangs on its
     * piping, on one post in the cell behind it. Lengths in suction diameters: chest 0.8, chamber 1.5 (1.2 wide), inlet
     * 2.2, throat 0.8 (0.35 wide), diffuser 3.0 (to 0.75 wide).
     */
    private void ejector() {
        // Posts at ±e round the axis, their base plates outside the pipe blocks of the suction and the discharge (half a
        // block each side of the axis), and clear of the chamber; a small ejector's axis on its cell's centre.
        double s = shape.diameter(), e = Math.max(.5 + POST, .6 * s + .3), h = shape.support();
        boolean small = s <= SMALL_EJECTOR + 1e-9;
        double a = small ? .5 : centre(e + POST);
        // The suction's centre on a cell centre: the diffuser's exit at least 0.2 m over the discharge flange.
        int row = (int) Math.ceil(h + .2 + 6.75 * s - .5);
        double exit = row + .5 - 6.75 * s, chamber = exit + 6 * s, chest = exit + 7.5 * s, top = exit + 8.3 * s;
        var f = Frame.of(new double[]{a, exit, a}, 1, 1);
        int n = sides(1.2 * s);
        double rc = .6 * s, throat = .175 * s, outlet = .375 * s;
        mesh.ring(f, 0, outlet + .05 + .1 * s, -.03, 0, n, 1, Material.FLANGE);
        mesh.frustum(f, outlet, throat, 0, 3 * s, n, Material.MACHINE);
        mesh.prism(f, throat, 3 * s, 3.8 * s, n, Material.MACHINE);
        mesh.frustum(f, throat, rc, 3.8 * s, 6 * s, n, Material.MACHINE);
        mesh.prism(f, rc, 6 * s, 7.5 * s, n, Material.MACHINE);
        mesh.annulus(f, .225 * s, rc, 7.5 * s, n, 1, Material.MACHINE);
        mesh.ring(f, .225 * s, .3 * s, 7.5 * s, 7.58 * s, n, 3, Material.FLANGE);
        mesh.cylinder(f, .225 * s, 7.5 * s, 8.3 * s, n, Material.MACHINE);
        solids.add(Solid.rod(new double[]{a, 0, a}, 1, exit, chest, rc, 0));
        solids.add(Solid.rod(new double[]{a, 0, a}, 1, chest, top, .225 * s, 0));
        round(a, a, chamber, chest, rc, 1);
        nozzles.add(radialSide(Role.SUCTION, a, row + .5, a, rc, n, s, s));
        double motive = Math.max(PipeSizes.MIN_OUTSIDE, .45 * s);
        nozzles.add(new Nozzle(Role.MOTIVE, new double[]{a, top - .03, a}, 1, 1, Math.ceil(top + EquipmentShape.STAND_OUT) - top + .03, motive,
                ColumnStructure.size(.3 * s, motive), 0, .03));
        double discharge = Math.max(PipeSizes.MIN_OUTSIDE, 2 * outlet);
        nozzles.add(new Nozzle(Role.DISCHARGE, new double[]{a, exit + .03, a}, 1, -1, exit + .03 - h, discharge, discharge, 0, .03));
        double postTop = top + .3, bar = .14, clamp = exit + 3.4 * s;
        if (small) {
            // One post in the cell behind (the suction's pipe leaves in front, the discharge's below, the motive's above),
            // braced to the throat and the chamber.
            double z = a + 1;
            box(new double[]{a - POST / 2, 0, z - POST / 2}, new double[]{a + POST / 2, postTop, z + POST / 2}, Material.STEELWORK, true);
            box(new double[]{a - POST, 0, z - POST}, new double[]{a + POST, .04, z + POST}, Material.SUPPORT, false);
            mesh.bar(new double[]{a, clamp, z}, new double[]{a, clamp, a + throat}, bar * .6, new double[]{0, 1, 0}, Material.STEELWORK);
            mesh.bar(new double[]{a, (chamber + chest) / 2, z}, new double[]{a, (chamber + chest) / 2, a + rc}, bar * .6, new double[]{0, 1, 0}, Material.STEELWORK);
            return;
        }
        // The frame: posts beside the suction's pipe block, square frames open at the front in its row, a clamp at the
        // throat.
        double[][] corners = {{a - e, a - e}, {a + e, a - e}, {a + e, a + e}, {a - e, a + e}};
        for (var c : corners) {
            box(new double[]{c[0] - POST / 2, 0, c[1] - POST / 2}, new double[]{c[0] + POST / 2, postTop, c[1] + POST / 2}, Material.STEELWORK, true);
            box(new double[]{c[0] - POST, 0, c[1] - POST}, new double[]{c[0] + POST, .04, c[1] + POST}, Material.SUPPORT, false);
        }
        var levels = new ArrayList<Double>();
        for (double y = 2.5; y < postTop - .3; y += 2.5) levels.add(y);
        levels.add(clamp);
        levels.add(postTop - .1);
        for (double y : levels) {
            // In the suction's row its pipe leaves between the front posts: the front bar and brackets give way there.
            boolean suction = y + bar / 2 > row - .05 && y - bar / 2 < row + 1.05;
            for (int k = suction ? 1 : 0; k < 4; k++) {
                var p = corners[k];
                var q = corners[(k + 1) % 4];
                mesh.bar(new double[]{p[0], y, p[1]}, new double[]{q[0], y, q[1]}, bar * .8, new double[]{0, 1, 0}, Material.STEELWORK);
            }
            // At the clamp, brackets from the corners to the throat.
            if (y == clamp)
                for (var c : corners)
                    if (!suction || c[1] > a)
                        mesh.bar(new double[]{c[0], y, c[1]}, new double[]{a + (c[0] - a) * throat / e, y, a + (c[1] - a) * throat / e}, bar * .6,
                                new double[]{0, 1, 0}, Material.STEELWORK);
        }
    }

    // ---- Parts ------------------------------------------------------------------------------------------------------

    /** The middle of a box whose width is the least odd whole number of cells holding {@code half} each side: the centre line runs through cell centres. */
    private static double centre(double half) {
        int width = (int) Math.ceil(2 * half - 1e-9);
        if (width % 2 == 0) width++;
        return width / 2.0;
    }

    /**
     * A nozzle on top ({@code sign} +1) or under (-1) a body lying along z (its axis at (x, y), radius {@code r} to its
     * {@code n} vertices) at station {@code z}: its neck starts under the surface at its largest size's edge, its flange
     * face on the next block face {@link EquipmentShape#STAND_OUT} beyond.
     */
    private static Nozzle radial(Role role, double x, double y, double r, int n, double z, int sign, double maxOd, double defaultOd) {
        double apothem = r * DeterministicMath.cos(Math.PI / n), start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double surface = y + sign * r, plane = sign > 0 ? Math.ceil(surface + EquipmentShape.STAND_OUT) : Math.floor(surface - EquipmentShape.STAND_OUT);
        return new Nozzle(role, new double[]{x, y + sign * start, z}, 1, sign, Math.abs(plane - (y + sign * start)), maxOd, defaultOd, 0, r - start);
    }

    /** A nozzle on the front (frame -z) of a standing body (its axis at (x, z), radius {@code r}) at height {@code y}. */
    private static Nozzle radialSide(Role role, double x, double y, double z, double r, int n, double maxOd, double defaultOd) {
        double apothem = r * DeterministicMath.cos(Math.PI / n), start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double plane = Math.floor(z - r - EquipmentShape.STAND_OUT);
        return new Nozzle(role, new double[]{x, y, z - start}, 2, -1, z - start - plane, maxOd, defaultOd, 0, r - start);
    }

    private static double saddleWidth(double r) {
        return Math.max(.15, Math.min(.3, .12 * r + .1));
    }

    /** A saddle at station {@code z} under a body lying along z at (x, y) of radius {@code r}: a plate from the ground and its base plate. */
    private void saddle(double z, double x, double y, double r, double w) {
        double across = .8 * r;
        box(new double[]{x - across, 0, z - w / 2}, new double[]{x + across, y - .6 * r, z + w / 2}, Material.SUPPORT, true);
        box(new double[]{x - across - .05, 0, z - w / 2 - .05}, new double[]{x + across + .05, .04, z + w / 2 + .05}, Material.SUPPORT, false);
    }

    /**
     * A 2:1 head from its tangent line at frame station {@code at} to its apex {@code depth} beyond, along the frame's
     * axis, drawn as bands of the ellipsoid.
     */
    private void head(Frame f, double r, double at, double depth, int n, Material material) {
        int bands = Math.max(3, n / 4);
        var base = f.along(at);
        var spheroid = new Spheroid(base, f.axis(), r, depth);
        for (int k = 0; k < bands; k++) {
            double p0 = Math.PI / 2 * k / bands, p1 = Math.PI / 2 * (k + 1) / bands;
            double r0 = r * DeterministicMath.cos(p0), r1 = k + 1 == bands ? 0 : r * DeterministicMath.cos(p1);
            double z0 = at + depth * DeterministicMath.sin(p0), z1 = at + depth * DeterministicMath.sin(p1);
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

    /** A round body lying along z at (x, y) from {@code lo} to {@code hi}: its rims are fixed edges, its sides outlined against the eye. */
    private void round(double x, double y, double lo, double hi, double r) {
        round(x, y, lo, hi, r, 2);
    }

    /** A round body along frame axis {@code axis} through the point (x, y, z) less that axis, from {@code lo} to {@code hi}. */
    private void round(double x, double y, double lo, double hi, double r, int axis) {
        double[] centre = axis == 2 ? new double[]{x, y, 0} : new double[]{x, 0, y};
        rounds.add(new double[]{centre[0], centre[1], centre[2], axis, r, lo, hi});
        int u = (axis + 1) % 3, v = (axis + 2) % 3, steps = 24;
        for (double at : new double[]{lo, hi})
            for (int k = 0; k < steps; k++) {
                double a0 = 2 * Math.PI * k / steps, a1 = 2 * Math.PI * (k + 1) / steps;
                double[] p = centre.clone(), q = centre.clone();
                p[axis] = at;
                q[axis] = at;
                p[u] += r * DeterministicMath.cos(a0);
                p[v] += r * DeterministicMath.sin(a0);
                q[u] += r * DeterministicMath.cos(a1);
                q[v] += r * DeterministicMath.sin(a1);
                segment(p, q);
            }
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
}
