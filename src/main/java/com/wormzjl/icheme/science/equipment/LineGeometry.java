package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.icheme.science.equipment.Mesh.*;

/**
 * The drawn shape of one fluid device block, inside its cell (batch 2026-10-15-custom-equipment-rendering, owner request
 * 2026-10-16: pipes drawn at their diameter up to 1 m, accessories with connections sized by the connected pipe).
 *
 * <p>A pipe runs from its block's centre to each connected face at its own outside diameter. Two arms in line make a
 * straight run, two at right angles a long-radius elbow (bend radius 1.5 OD, shortened to leave room for an end
 * fitting), more a tee or cross along a straight run, or a ball core where no two arms are in line; one arm, or none,
 * runs straight through. Where the next pipe is smaller, a concentric reducer in the larger pipe's block narrows to it
 * at the face. Flanges stand only where a line meets equipment or a vessel nozzle, as a pair in the middle of every
 * sixth straight block (spool ends; photo survey section 3), and as a blind flange where a line ends unconnected
 * (owner request 2026-10-16); pipes are welded elsewhere.
 *
 * <p>Pipeline devices draw their bodies from photographs (survey section 4): a globe control valve with its diaphragm
 * actuator above a horizontal line or beside a vertical one; a vertical in-line pump with its motor up on a pedestal; a
 * compressor barrel with its motor on a skid; a Y-strainer with its screen leg 45° down and downstream; a small
 * vertical tank on legs; and the feed and drain boxes of the source and sink. Each draws a flanged nozzle at the size of
 * the line met at each connected face; an in-line device draws its two line nozzles even unconnected.
 */
public final class LineGeometry {
    /** Faces in Minecraft's {@code Direction} order (down, up, north, south, west, east), and their unit steps. */
    public static final int[][] STEPS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    /** A line nozzle with no pipe met and no size of its own: 0.1 m, a 3" line. */
    public static final double NOZZLE = .1;
    private static final double[] CENTRE = {.5, .5, .5};
    private static final int UP = 1, NORTH = 2, SOUTH = 3;

    /**
     * What a device block draws. {@code facing} and the six faces follow {@link #STEPS}; {@code connections} has a bit per
     * connected face; {@code outside} is the device's own outside diameter as a {@link PipeSizes#code}; {@code faces}
     * holds, seven bits per face, the outside diameter of the line met there (0: none); {@code flanged} has a bit per face
     * whose joint is flanged; {@code spool} marks a straight pipe at a spool end.
     */
    public record Key(Kind kind, int facing, int connections, int outside, long faces, int flanged, boolean spool) {
        public boolean connected(int face) {
            return (connections >> face & 1) != 0;
        }

        public int face(int face) {
            return (int) (faces >>> (7 * face) & 127);
        }

        public boolean flanged(int face) {
            return (flanged >> face & 1) != 0;
        }

        /** An item's look: a pipe 0.25 m across lying north-south, a device on a 0.15 m line. */
        public static Key item(Kind kind) {
            if (kind == Kind.PIPE) {
                int code = PipeSizes.code(.25);
                return new Key(kind, NORTH, 1 << NORTH | 1 << SOUTH, code, (long) code << 7 * NORTH | (long) code << 7 * SOUTH, 0, false);
            }
            return new Key(kind, NORTH, 0, PipeSizes.code(.15), 0, 0, false);
        }
    }

    private LineGeometry() {
    }

    /** The block's polygons, cell-local. */
    public static List<Polygon> polygons(Key key) {
        return mesh(key).cell(0, 0, 0);
    }

    static Mesh mesh(Key key) {
        var m = new Mesh();
        switch (key.kind()) {
            case PIPE -> pipe(m, key);
            case VALVE -> valve(m, key);
            case PUMP -> pump(m, key);
            case COMPRESSOR -> compressor(m, key);
            case FILTER -> strainer(m, key);
            case RESERVOIR -> tank(m, key);
            case GENERATOR -> box(m, key, Material.FEED);
            case VOID -> box(m, key, Material.DRAIN);
        }
        return m;
    }

    // ---- Pipes -----------------------------------------------------------------------------------------------------

    /** How a line ends at a face: the outside it narrows to, whether it ends in a flange, and the length of both fittings. */
    record End(double outside, boolean flange, double reducer, double tail) {
        static End flanged(double outside) {
            return new End(outside, true, 0, PipeSizes.flangeThickness(outside) + PipeSizes.hub(outside));
        }
    }

    /**
     * A pipe of outside {@code od} at face {@code i}: it narrows to a smaller line met there, and is flanged where the joint
     * is; where it meets nothing it ends in a blind flange of its own size (owner request 2026-10-16).
     */
    static End end(Key key, int i, double od) {
        if (!key.connected(i)) return End.flanged(od);
        int code = key.face(i);
        double met = code == 0 ? od : Math.min(od, PipeSizes.outsideOf(code));
        double reducer = met < od - EPS ? reducerLength(od) : 0;
        double tail = reducer + (key.flanged(i) ? PipeSizes.flangeThickness(met) + PipeSizes.hub(met) : 0);
        return new End(met, key.flanged(i), reducer, tail);
    }

    /** A concentric reducer's length: ASME B16.9 gives 0.6 to 1.3 times the large end; 0.8 OD, a texel to 0.3 m. */
    static double reducerLength(double large) {
        return Math.max(1 / 16.0, Math.min(.3, .8 * large));
    }

    private static void pipe(Mesh m, Key key) {
        double od = PipeSizes.outsideOf(key.outside());
        int sides = PipeSizes.sides(od);
        var arms = new ArrayList<Integer>();
        for (int i = 0; i < 6; i++) if (key.connected(i)) arms.add(i);
        if (arms.size() <= 1) {
            // Unconnected, a length along its facing; a dead end, straight on from its connection: blind flanges where
            // the line ends.
            run(m, key, arms.isEmpty() ? key.facing() & ~1 : arms.getFirst(), od, sides, false);
            return;
        }
        if (arms.size() == 2) {
            int a = arms.get(0), b = arms.get(1);
            if ((a ^ 1) == b) run(m, key, a, od, sides, key.spool());
            else elbow(m, key, a, b, od, sides);
            return;
        }
        // Tee or cross: a straight run through the block (the facing's axis first), branches welded into it.
        int through = -1;
        for (int i : new int[]{key.facing() & ~1, 4, 2, 0})
            if (through < 0 && key.connected(i) && key.connected(i ^ 1)) through = i;
        if (through < 0) ball(m, od, sides);
        else run(m, key, through, od, sides, false);
        for (int i : arms)
            if (through < 0 || (i & ~1) != through) arm(m, i, 0, od, end(key, i, od), ref(i), sides, true);
    }

    /**
     * The ball a dead end, a corner or a core ends its arms in: around the pipe's facets, but inside the block, so a line
     * of one block across keeps its ball whole; the arms ending in it close their start (see {@link #arm}).
     */
    private static void ball(Mesh m, double od, int sides) {
        m.sphere(CENTRE, Math.min(.5, od / 2 / DeterministicMath.cos(Math.PI / sides)), sides, Material.PIPE);
    }

    /** A straight length through faces {@code i} and its opposite, with a flange pair in the middle at a spool end. */
    private static void run(Mesh m, Key key, int i, double od, int sides, boolean spool) {
        double from = 0;
        if (spool) {
            double t = PipeSizes.flangeThickness(od), hub = PipeSizes.hub(od);
            var f = frame(i, ref(i));
            double flange = PipeSizes.flange(od) / 2, hubR = PipeSizes.hubOutside(od) / 2;
            m.prism(f, flange, -t, t, sides, Material.FLANGE);
            for (int side = -1; side <= 1; side += 2) {
                m.annulus(f, hubR, flange, side * t, sides, side, Material.FLANGE);
                m.bolts(f, od, hubR, flange, side * t, side, Material.BOLT);
                m.frustum(f, hubR, od / 2, side * t, side * (t + hub), sides, Material.PIPE);
            }
            from = t + hub;
        }
        arm(m, i, from, od, end(key, i, od), ref(i), sides, false);
        arm(m, i ^ 1, from, od, end(key, i ^ 1, od), ref(i ^ 1), sides, false);
    }

    /**
     * The ends of an elbow from face {@code a} to face {@code b} and its bend radius: 1.5 OD, less where end fittings take
     * room (reducers shorten to a texel first); 0 where not even a bend of about one radius fits.
     */
    static double bend(End ea, End eb, double od) {
        double bend = Math.min(1.5 * od, Math.min(.5 - ea.tail(), .5 - eb.tail()));
        return bend < 1.05 * od / 2 ? 0 : bend;
    }

    /** {@code end} with its reducer shortened (to a texel at least) so its fittings take at most {@code room}. */
    static End fit(End end, double room) {
        if (end.tail() <= room || end.reducer() == 0) return end;
        double reducer = Math.max(1 / 16.0, end.reducer() - (end.tail() - room));
        return new End(end.outside(), end.flange(), reducer, end.tail() - end.reducer() + reducer);
    }

    /** A long-radius elbow from face {@code a} to face {@code b}, or a ball corner where the fittings leave no room for it. */
    private static void elbow(Mesh m, Key key, int a, int b, double od, int sides) {
        double r = od / 2, room = .5 - 1.05 * r;
        End ea = fit(end(key, a, od), room), eb = fit(end(key, b, od), room);
        double bend = bend(ea, eb, od);
        if (bend == 0) {
            ball(m, od, sides);
            arm(m, a, 0, od, ea, ref(a), sides, true);
            arm(m, b, 0, od, eb, ref(b), sides, true);
            return;
        }
        double[] da = step(a), db = step(b);
        var origin = new double[]{.5 + bend * (da[0] + db[0]), .5 + bend * (da[1] + db[1]), .5 + bend * (da[2] + db[2])};
        // Angle 0 at the tangent point on arm a (towards -b), sweeping a quarter turn to the tangent point on arm b.
        var f = new Frame(origin, cross(db, da), neg(db));
        int segments = Math.max(3, (int) Math.ceil(Math.PI / 2 * bend / .08));
        m.torus(f, bend, r, 0, Math.PI / 2, segments, sides, Material.PIPE);
        // Each arm's prism starts its facets where the torus's cross-section does, so the rims meet vertex to vertex.
        arm(m, a, bend, od, ea, neg(db), sides, false);
        arm(m, b, bend, od, eb, neg(da), sides, false);
    }

    /**
     * A line from {@code from} (metres from the block's centre along face {@code i}) to the face, at outside {@code od},
     * ending as {@code end} says: a reducer, then a square-shouldered flange, or a square cut where nothing connects. A
     * {@code closed} arm also closes its start: it starts inside a ball or a device body whose facets need not cover its
     * rim, and as a closed solid it shows no opening wherever it pokes out. An arm starting within its flange is the
     * flange alone, closed by the flange's own back.
     */
    static void arm(Mesh m, int i, double from, double od, End end, double[] ref, int sides, boolean closed) {
        var f = frame(i, ref);
        double r = od / 2, z = Math.max(from, .5 - end.tail());
        double body = end.flange() ? .5 - PipeSizes.flangeThickness(end.outside()) : .5;
        if (closed && from < body - EPS) m.annulus(f, 0, r, from, sides, -1, Material.PIPE);
        if (z > from + EPS) m.prism(f, r, from, z, sides, Material.PIPE);
        if (end.reducer() > 0) {
            double z1 = Math.max(z, .5 - end.tail() + end.reducer());
            if (z1 > z + EPS) m.frustum(f, r, end.outside() / 2, z, z1, sides, Material.PIPE);
            z = z1;
        }
        if (end.flange()) flange(m, f, end.outside(), z, sides);
    }

    /**
     * A square-shouldered flange whose face lies on the block face: its straight neck from {@code z}, the back of the flange, its rim and its
     * face, a blind flange where nothing meets it. Where a flange of one size meets it, the two faces lie in one plane
     * and look opposite ways, so only one is ever towards the viewer, and it lies inside the other flange. Without a hub
     * (the line starts within the flange) its back is a whole disc.
     */
    private static void flange(Mesh m, Frame f, double od, double z, int sides) {
        double t = PipeSizes.flangeThickness(od), flange = PipeSizes.flange(od) / 2, hubR = PipeSizes.hubOutside(od) / 2;
        boolean hub = .5 - t > z + EPS;
        if (hub) m.frustum(f, od / 2, hubR, z, .5 - t, sides, Material.PIPE);
        m.annulus(f, hub ? hubR : 0, flange, .5 - t, sides, -1, Material.FLANGE);
        m.bolts(f, od, hub ? hubR : od / 2, flange, .5 - t, -1, Material.BOLT);
        m.prism(f, flange, .5 - t, .5, sides, Material.FLANGE);
        m.annulus(f, 0, flange, .5, sides, 1, Material.FLANGE);
    }

    // ---- Pipeline devices ------------------------------------------------------------------------------------------

    /** The size of the line through an in-line device: the larger pipe met on its axis, else its own size (at least a 3" stub). */
    private static double line(Key key) {
        int a = key.facing() & ~1;
        double od = Math.max(key.face(a), key.face(a ^ 1)) > 0 ? PipeSizes.outsideOf(Math.max(key.face(a), key.face(a ^ 1)))
                : Math.max(NOZZLE, PipeSizes.outsideOf(key.outside()));
        return od;
    }

    /**
     * An in-line device's two nozzles, flanged at the face, at the size of the line met at each (or the device's line
     * size); each closes its start inside the body.
     */
    private static void lineNozzles(Mesh m, Key key, double from, double od) {
        int a = key.facing() & ~1;
        for (int i : new int[]{a, a ^ 1}) {
            double size = key.face(i) > 0 ? PipeSizes.outsideOf(key.face(i)) : od;
            arm(m, i, from, size, End.flanged(size), ref(i), PipeSizes.sides(size), true);
        }
    }

    /** A boundary device's nozzles: one per connected face with a line met, from inside the body at {@code from[face]}. */
    private static void faceNozzles(Mesh m, Key key, double[] from) {
        for (int i = 0; i < 6; i++) {
            if (!key.connected(i) || key.face(i) == 0) continue;
            double size = PipeSizes.outsideOf(key.face(i));
            arm(m, i, from[i], size, End.flanged(size), ref(i), PipeSizes.sides(size), true);
        }
    }

    /**
     * A globe control valve: a round body, flanged to the line; a bonnet, a yoke with the stem and a diaphragm casing
     * along the actuator's direction (up over a horizontal line, horizontal beside a vertical one).
     */
    private static void valve(Mesh m, Key key) {
        double od = line(key), body = Math.min(.42, .6 * PipeSizes.flange(od));
        int sides = PipeSizes.sides(2 * body);
        m.sphere(CENTRE, body, Math.max(12, sides), Material.MACHINE);
        lineNozzles(m, key, .7 * body, od);
        int vertical = key.facing() >> 1 == 0 ? 1 : 0;
        int up = vertical == 1 ? (key.facing() == UP ? NORTH : SOUTH) : UP;
        var f = frame(up, ref(up));
        double bonnet = body + .06, yoke = Math.max(.04, Math.min(.2, .3 * body + .06)), casing = Math.min(.7, .3 + .5 * od);
        // The casing stands 0.3 of its diameter plus its 0.02 m rim above the yoke; all of it stays in the block.
        double room = .49 - bonnet - .02;
        if (yoke + .3 * casing > room) {
            double k = Math.max(0, room) / (yoke + .3 * casing);
            yoke *= k;
            casing *= k;
        }
        m.frustum(f, .45 * body, .3 * body, .8 * body, bonnet, sides, Material.MACHINE);
        m.annulus(f, 0, .3 * body, bonnet, sides, 1, Material.MACHINE);
        if (casing < .08) return;
        // Yoke: two legs from the bonnet to the casing, the stem between them.
        double leg = Math.min(.025, .1 * casing), spread = .3 * body;
        var across = side(f);
        for (int s = -1; s <= 1; s += 2) {
            var a = f.at(0, 0, bonnet);
            var b = f.at(0, 0, bonnet + yoke);
            for (int c = 0; c < 3; c++) {
                a[c] += s * spread * across[c];
                b[c] += s * spread * across[c];
            }
            m.bar(a, b, leg, f.ref(), Material.STEELWORK);
        }
        m.bar(f.along(bonnet), f.along(bonnet + yoke), .4 * leg, f.ref(), Material.STEELWORK);
        // Diaphragm casing: two shallow cones about a rim, the lower one bolted to the yoke.
        double rc = casing / 2, z0 = bonnet + yoke, zr = z0 + .12 * casing, z1 = zr + .02, top = z1 + .18 * casing;
        int n = Math.max(12, PipeSizes.sides(casing));
        m.frustum(f, .3 * rc, rc, z0, zr, n, Material.ACTUATOR);
        m.annulus(f, 0, .3 * rc, z0, n, -1, Material.ACTUATOR);
        m.prism(f, rc + .01, zr, z1, n, Material.ACTUATOR);
        m.annulus(f, rc, rc + .01, zr, n, -1, Material.ACTUATOR);
        m.annulus(f, rc, rc + .01, z1, n, 1, Material.ACTUATOR);
        m.frustum(f, rc, .45 * rc, z1, top, n, Material.ACTUATOR);
        m.annulus(f, 0, .45 * rc, top, n, 1, Material.ACTUATOR);
    }

    /**
     * A vertical in-line pump (API 610 OH3): a round casing on the line, flanged suction and discharge, a bearing bracket
     * and the motor standing on it, the whole on a pedestal to the floor.
     */
    private static void pump(Mesh m, Key key) {
        double od = line(key), rc = Math.max(.12, Math.min(.4, .5 * PipeSizes.flange(od) + .06));
        int n = Math.max(12, PipeSizes.sides(2 * rc));
        var up = Frame.of(new double[]{.5, 0, .5}, 1, 1);
        double low = .5 - .55 * rc, high = .5 + .55 * rc, bracket = Math.max(.04, Math.min(.1, .3 * rc)), motor = high + bracket;
        m.cylinder(up, rc, low, high, n, Material.MACHINE);
        lineNozzles(m, key, .8 * rc, od);
        m.frustum(up, .55 * rc, .4 * rc, high, motor, n, Material.MACHINE);
        double rm = Math.max(.1, Math.min(.36, .75 * rc + .04)), cowl = Math.min(.06, .3 * (.98 - motor));
        m.cylinder(up, rm, motor, .98 - cowl, n, Material.MOTOR);
        m.frustum(up, .9 * rm, .6 * rm, .98 - cowl, .98, n, Material.MOTOR);
        m.annulus(up, 0, .6 * rm, .98, n, 1, Material.MOTOR);
        // Pedestal and its foot plate.
        m.prism(up, .45 * rc, .04, low, n, Material.STEELWORK);
        double foot = .6 * rc;
        m.box(new double[]{.5 - foot, 0, .5 - foot}, new double[]{.5 + foot, .04, .5 + foot}, ALL_FACES, Material.STEELWORK);
    }

    /** A compressor on its skid: a barrel casing on the line, its motor lying above it, pedestals under the barrel. */
    private static void compressor(Mesh m, Key key) {
        double od = line(key), rb = Math.max(.14, Math.min(.3, .5 * PipeSizes.flange(od) + .05)), half = .32;
        int a = key.facing() & ~1, n = Math.max(12, PipeSizes.sides(2 * rb));
        var f = frame(a, ref(a));
        m.box(new double[]{.04, 0, .04}, new double[]{.96, .06, .96}, ALL_FACES, Material.STEELWORK);
        m.cylinder(f, rb, -half, half, n, Material.MACHINE);
        lineNozzles(m, key, half, od);
        double[] along = step(a);
        double width = .06;
        for (double s : new double[]{-.2, .2}) {
            double cx = .5 + s * along[0], cz = .5 + s * along[2];
            double wx = along[0] != 0 ? width : .6 * rb, wz = along[2] != 0 ? width : .6 * rb;
            m.box(new double[]{cx - wx, .06, cz - wz}, new double[]{cx + wx, .5 - .7 * rb, cz + wz}, ALL_FACES & ~12, Material.STEELWORK);
        }
        double rm = Math.min(.16, (.98 - .5 - rb - .02) / 2);
        if (rm < .05) return;
        var motor = new Frame(new double[]{.5, .5 + rb + .02 + rm, .5}, along, new double[]{0, 1, 0});
        m.cylinder(motor, rm, -.24, .24, Math.max(12, PipeSizes.sides(2 * rm)), Material.MOTOR);
    }

    /** A Y-strainer: a body on the line, its screen leg 45° down and downstream (the facing), closed by a blind flange. */
    private static void strainer(Mesh m, Key key) {
        double od = line(key), rb = Math.min(.3, .5 * PipeSizes.flange(od)), half = Math.max(.1, Math.min(.35, .8 * PipeSizes.flange(od)));
        int a = key.facing() & ~1, n = Math.max(8, PipeSizes.sides(2 * rb));
        var f = frame(a, ref(a));
        m.prism(f, rb, -half, half, n, Material.MACHINE);
        m.annulus(f, 0, rb, -half, n, -1, Material.MACHINE);
        m.annulus(f, 0, rb, half, n, 1, Material.MACHINE);
        lineNozzles(m, key, half, od);
        double[] downstream = step(key.facing());
        var axis = unit(downstream[0], -1, downstream[2]);
        var lateral = cross(downstream, new double[]{0, 1, 0});
        var leg = new Frame(CENTRE, axis, unit(lateral));
        double rl = .8 * rb, cover = Math.min(.48, PipeSizes.flange(2 * rl) / 2), length = Math.min(1.6 * rb + .08, (.47 - cover) * Math.sqrt(2));
        m.prism(leg, rl, 0, length - .03, n, Material.MACHINE);
        m.annulus(leg, rl, cover, length - .03, n, -1, Material.FLANGE);
        m.prism(leg, cover, length - .03, length, n, Material.FLANGE);
        m.annulus(leg, 0, cover, length, n, 1, Material.FLANGE);
        m.bolts(leg, 2 * rl, rl, cover, length, 1, Material.BOLT);
        m.cylinder(leg, Math.min(.03, .3 * rl), length, length + .03, 8, Material.FLANGE);
    }

    /** A small vertical tank on four legs, with dished heads, and a flanged nozzle to each line met. */
    private static void tank(Mesh m, Key key) {
        double radius = .38, bottom = .22, top = .78, depth = .12;
        int n = 24;
        var up = Frame.of(new double[]{.5, 0, .5}, 1, 1);
        m.prism(up, radius, bottom, top, n, Material.SHELL);
        // Dished heads as three bands of a 2:1 ellipse squashed to the head depth.
        double[][] head = {{1, 0}, {.85, .55}, {.5, .87}, {0, 1}};
        for (int k = 0; k + 1 < head.length; k++)
            for (int s = -1; s <= 1; s += 2) {
                double base = s > 0 ? top : bottom;
                m.frustum(up, radius * head[k][0], radius * head[k + 1][0], base + s * depth * head[k][1], base + s * depth * head[k + 1][1], n, Material.HEAD);
            }
        for (int corner = 0; corner < 4; corner++) {
            double angle = Math.PI / 4 + corner * Math.PI / 2, x = .5 + .3 * DeterministicMath.cos(angle), z = .5 + .3 * DeterministicMath.sin(angle);
            m.box(new double[]{x - .03, 0, z - .03}, new double[]{x + .03, bottom + .04, z + .03}, ALL_FACES & ~8, Material.STEELWORK);
        }
        double side = radius * DeterministicMath.cos(Math.PI / n) - .02, end = top - .5 + depth - .02;
        faceNozzles(m, key, new double[]{end, end, side, side, side, side});
    }

    /** The source or sink: a painted box on the floor with a flanged nozzle to each line met. */
    private static void box(Mesh m, Key key, Material material) {
        m.box(new double[]{.18, 0, .18}, new double[]{.82, .7, .82}, ALL_FACES, material);
        m.box(new double[]{.15, .7, .15}, new double[]{.85, .76, .85}, ALL_FACES, Material.STEELWORK);
        faceNozzles(m, key, new double[]{.5, .25, .3, .3, .3, .3});
    }

    // ---- Frames ----------------------------------------------------------------------------------------------------

    static double[] step(int face) {
        var s = STEPS[face];
        return new double[]{s[0], s[1], s[2]};
    }

    /** The default facet reference for an arm along face {@code i}: the next box axis, as vessel nozzles use it. */
    static double[] ref(int face) {
        var r = new double[3];
        r[(axis(face) + 1) % 3] = 1;
        return r;
    }

    static int axis(int face) {
        return face < 2 ? 1 : face < 4 ? 2 : 0;
    }

    /** The frame of an arm from the block centre towards face {@code i}, facets starting at {@code ref}. */
    static Frame frame(int face, double[] ref) {
        return new Frame(CENTRE, step(face), ref);
    }

    private static double[] side(Frame f) {
        return cross(f.axis(), f.ref());
    }
}
