package com.wormzjl.icheme.science.equipment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A player moving as vanilla 1.21 moves one (LivingEntity.travel and Entity.move): a 0.6 by 1.8 m box aligned with the
 * world; input 0.98 of 0.1 m/tick² on the ground, 0.02 in the air; on a ladder the horizontal speed held to 0.15 m/tick
 * on each axis and the fall to 0.15, a horizontal collision or a jump there lifting it 0.2; gravity 0.08 and drag 0.98;
 * friction 0.546 on the ground, 0.91 in the air; collision along y first, then the longer horizontal move; a 0.6 m
 * step-up on the ground or landing. It collides with solid 1/8-block voxels, as the game collides with equipment cells
 * ({@link ShapeRasterizer#solid}), and climbs where {@link Ladders} says (the block at its feet).
 */
public final class PlayerWalk {
    /** The box's half width and height, m, and the step-up, m. */
    public static final double HALF = .3, HEIGHT = 1.8, STEP_UP = .6;
    private static final int RES = ShapeRasterizer.SOLID_RESOLUTION;

    /** Whether the 1/8-block voxel (i, j, k) is solid. */
    public interface Voxels {
        boolean solid(int i, int j, int k);
    }

    /** Whether a body whose feet stand at the point is on a ladder. */
    public interface Ladders {
        boolean on(double[] feet);
    }

    private final Voxels voxels;
    private final Ladders ladders;
    private final Map<Long, Boolean> cache;
    /** The feet's middle and the motion, m and m/tick. */
    public double[] p = new double[3], v = new double[3];
    public boolean onGround, hColl;
    /** The last voxel that stopped the box, for a report. */
    public String blocker = "";

    public PlayerWalk(Voxels voxels, Ladders ladders) {
        this(voxels, ladders, new HashMap<>());
    }

    private PlayerWalk(Voxels voxels, Ladders ladders, Map<Long, Boolean> cache) {
        this.voxels = voxels;
        this.ladders = ladders;
        this.cache = cache;
    }

    /** The player in an equipment shape's box: its shell cells collide where the shape is solid, its interior cells whole, the ground under 0. */
    static PlayerWalk in(EquipmentShape shape) {
        var layout = ShapeRasterizer.layout(shape);
        Set<ShapeRasterizer.Cell> shell = new HashSet<>(layout.shell()), interior = new HashSet<>(layout.interior());
        return new PlayerWalk((i, j, k) -> {
            var cell = new ShapeRasterizer.Cell(Math.floorDiv(i, RES), Math.floorDiv(j, RES), Math.floorDiv(k, RES));
            return j < 0 || interior.contains(cell) || shell.contains(cell) && ShapeRasterizer.inside(shape, (i + .5) / RES, (j + .5) / RES, (k + .5) / RES);
        }, q -> shell.contains(new ShapeRasterizer.Cell((int) Math.floor(q[0]), (int) Math.floor(q[1]), (int) Math.floor(q[2])))
                && ShapeRasterizer.climbable(shape, q[0], q[1], q[2]));
    }

    /** A player in the same state, sharing the voxel cache. */
    public PlayerWalk copy() {
        var s = new PlayerWalk(voxels, ladders, cache);
        s.p = p.clone();
        s.v = v.clone();
        s.onGround = onGround;
        return s;
    }

    public boolean onLadder() {
        return ladders.on(p);
    }

    private boolean solid(int i, int j, int k) {
        long key = ((long) i & 0x1FFFFF) << 42 | ((long) j & 0x1FFFFF) << 21 | (long) k & 0x1FFFFF;
        return cache.computeIfAbsent(key, x -> voxels.solid(i, j, k));
    }

    /** How far a box {x0, y0, z0, x1, y1, z1} moves by {@code d} along {@code axis} before a solid voxel stops it. */
    private double clip(double[] b, int axis, double d) {
        if (d == 0) return 0;
        int u = (axis + 1) % 3, w = (axis + 2) % 3;
        int u0 = (int) Math.floor(b[u] * RES + 1e-7), u1 = (int) Math.ceil(b[u + 3] * RES - 1e-7) - 1;
        int w0 = (int) Math.floor(b[w] * RES + 1e-7), w1 = (int) Math.ceil(b[w + 3] * RES - 1e-7) - 1;
        int[] idx = new int[3];
        int a = d > 0 ? (int) Math.ceil(b[axis + 3] * RES - 1e-6) : (int) Math.floor(b[axis] * RES + 1e-6) - 1, sign = d > 0 ? 1 : -1;
        for (; d > 0 ? a / (double) RES < b[axis + 3] + d : (a + 1) / (double) RES > b[axis] + d; a += sign)
            for (int uu = u0; uu <= u1; uu++)
                for (int ww = w0; ww <= w1; ww++) {
                    idx[axis] = a;
                    idx[u] = uu;
                    idx[w] = ww;
                    if (solid(idx[0], idx[1], idx[2])) {
                        blocker = (idx[0] + .5) / RES + ", " + (idx[1] + .5) / RES + ", " + (idx[2] + .5) / RES;
                        return d > 0 ? Math.max(0, Math.min(d, a / (double) RES - b[axis + 3])) : Math.min(0, Math.max(d, (a + 1) / (double) RES - b[axis]));
                    }
                }
        return d;
    }

    private double[] collide(double[] b0, double[] d) {
        var b = b0.clone();
        double[] out = new double[3];
        for (int axis : Math.abs(d[0]) < Math.abs(d[2]) ? new int[]{1, 2, 0} : new int[]{1, 0, 2}) {
            out[axis] = clip(b, axis, d[axis]);
            b[axis] += out[axis];
            b[axis + 3] += out[axis];
        }
        return out;
    }

    private void move(double[] d) {
        double[] b = {p[0] - HALF, p[1], p[2] - HALF, p[0] + HALF, p[1] + HEIGHT, p[2] + HALF};
        var r = collide(b, d);
        boolean hx = Math.abs(r[0] - d[0]) > 1e-9, hz = Math.abs(r[2] - d[2]) > 1e-9, vy = Math.abs(r[1] - d[1]) > 1e-9;
        if ((onGround || vy && d[1] < 0) && (hx || hz)) {
            // The step-up: the highest the box climbs on whole voxels to STEP_UP that moves it further.
            var b1 = b.clone();
            if (vy) {
                b1[1] += r[1];
                b1[4] += r[1];
            }
            double best = r[0] * r[0] + r[2] * r[2];
            for (int k = (int) Math.ceil(b1[1] * RES + 1e-6); k / (double) RES <= b1[1] + STEP_UP + 1e-9; k++) {
                var s = collide(b1, new double[]{d[0], k / (double) RES - b1[1], d[2]});
                if (s[0] * s[0] + s[2] * s[2] > best + 1e-12) {
                    r = new double[]{s[0], s[1] + b1[1] - b[1], s[2]};
                    hx = Math.abs(r[0] - d[0]) > 1e-9;
                    hz = Math.abs(r[2] - d[2]) > 1e-9;
                    vy = false;
                    break;
                }
            }
        }
        for (int a = 0; a < 3; a++) p[a] += r[a];
        hColl = hx || hz;
        onGround = vy && d[1] < 0;
        if (hx) v[0] = 0;
        if (hz) v[2] = 0;
        if (vy) v[1] = 0;
    }

    /** One tick with horizontal input {@code wish} (world, at most unit length), jump held or not. */
    public void tick(double[] wish, boolean jump) {
        boolean ladder = onLadder();
        if (jump && onGround && !ladder) v[1] = .42;
        double speed = onGround ? .1 : .02;
        v[0] += wish[0] * .98 * speed;
        v[2] += wish[2] * .98 * speed;
        if (ladder) {
            v[0] = Math.max(-.15, Math.min(.15, v[0]));
            v[2] = Math.max(-.15, Math.min(.15, v[2]));
            v[1] = Math.max(v[1], -.15);
        }
        move(v.clone());
        if ((hColl || jump) && onLadder()) v[1] = .2;
        v[1] = (v[1] - .08) * .98;
        double f = onGround ? .6 * .91 : .91;
        v[0] *= f;
        v[2] *= f;
    }

    /** {@code wish} turned back towards the line through {@code centre} along the normal to {@code across}, at most unit length. */
    static double[] steer(PlayerWalk s, double[] wish, double[] centre, double[] across) {
        double c = Math.max(-1, Math.min(1, -4 * off(s, centre, across)));
        return unit(new double[]{wish[0] + c * across[0], 0, wish[2] + c * across[2]});
    }

    static double off(PlayerWalk s, double[] centre, double[] across) {
        return (s.p[0] - centre[0]) * across[0] + (s.p[2] - centre[2]) * across[2];
    }

    static double[] unit(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[2] * a[2]);
        return l <= 1 ? a : new double[]{a[0] / l, 0, a[2] / l};
    }

    /**
     * Null when a player climbs the ladder from its foot and steps off at its landing, else why not. The player starts on
     * the foot's deck clear of the rung plane, presses the rungs (steered to the ladder's middle) and climbs; a ladder
     * walked through is left by walking on between its rails; a side-step one by stepping off sideways, straight or
     * turned half towards or away from the rungs, jumping or not, from 0.15, 0.4, 0.8 and 1.2 m over the landing. Off
     * means on the ground at the landing's height, out of the ladder's climb zone.
     */
    static String leave(EquipmentShape shape, Access.Climb l) {
        var sim = in(shape);
        var t = shape.transform();
        double[] foot = {l.x(), l.base(), l.z()}, a = t.point(foot), b = t.point(new double[]{l.x() + l.outX(), l.base(), l.z() + l.outZ()});
        double[] out = {b[0] - a[0], 0, b[2] - a[2]}, across = {-out[2], 0, out[0]}, in = {-out[0], 0, -out[2]};
        // On the deck clear of the rung plane by the box's reach along out and a voxel, on top of the voxel the base lies in.
        double reach = Access.RUNG_FACE + HALF * (Math.abs(l.outX()) + Math.abs(l.outZ())) + .15;
        sim.p = t.point(new double[]{l.x() + reach * l.outX(), l.base(), l.z() + reach * l.outZ()});
        sim.p[1] = Math.ceil(sim.p[1] * RES - 1e-6) / RES + .01;
        for (int i = 0; i < 10; i++) sim.tick(new double[3], false);
        if (sim.p[1] < a[1] - .2) return String.format("no footing at the foot: fell to %.2f", sim.p[1]);
        double landing = t.point(new double[]{l.x(), l.top() - Access.LANDING, l.z()})[1];
        boolean walkThrough = l.rungTop() < l.top() - 1e-9;
        String why = "";
        int ticks = 0;
        for (double over : new double[]{.15, .4, .8, 1.2}) {
            double top = sim.p[1];
            for (; ticks < 600 && sim.p[1] < landing + over; ticks++) {
                sim.tick(steer(sim, in, a, across), false);
                top = Math.max(top, sim.p[1]);
                if (off(sim, a, landing, out, across, walkThrough)) return null;
            }
            if (top < landing - .01 && ticks >= 600) return String.format("the climb stops at %.2f under the landing at %.2f (stopped at %s)", top, landing, sim.blocker);
            if (walkThrough) continue;
            for (int mode = 0; mode < 6; mode++)
                for (boolean jump : new boolean[]{false, true}) {
                    var s = sim.copy();
                    int side = mode % 2 == 0 ? 1 : -1;
                    double k = mode < 2 ? 0 : mode < 4 ? .5 : -.5;
                    for (int i = 0; i < 80; i++) {
                        s.tick(unit(new double[]{side * across[0] + k * in[0], 0, side * across[2] + k * in[2]}), jump && i < 40);
                        if (off(s, a, landing, out, across, false)) return null;
                    }
                    why = String.format("from %.2f over the landing at %.2f: feet at %.2f, %.2f across, ground %s, ladder %s, stopped at %s", over, landing, s.p[1],
                            off(s, a, across), s.onGround, s.onLadder(), s.blocker);
                }
        }
        return (walkThrough ? "walked through: feet at " + String.format("%.2f under the landing at %.2f", sim.p[1], landing) : "stepped off sideways " + why);
    }

    /** Whether the player stands on the ground at the landing's height off the ladder whose foot is at {@code foot}. */
    private static boolean off(PlayerWalk s, double[] foot, double landing, double[] out, double[] across, boolean walkThrough) {
        double ahead = (s.p[0] - foot[0]) * out[0] + (s.p[2] - foot[2]) * out[2];
        boolean clear = !s.onLadder() && (Math.abs(off(s, foot, across)) > Access.Climb.HALF || ahead < -.05);
        return s.onGround && clear && s.p[1] > landing - .07 && s.p[1] < landing + .6;
    }
}
