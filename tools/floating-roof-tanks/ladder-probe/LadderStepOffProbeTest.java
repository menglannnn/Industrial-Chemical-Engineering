package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;
import org.junit.jupiter.api.Test;

import java.util.*;

/** Probe: a simulated player climbs each ladder and tries to leave it at its landing (vanilla 1.21 movement, 1/8 voxels). */
class TmpLadderStepOffProbeTest {
    static final double W = .3, H = 1.8, STEP = .6;

    static final class Sim {
        final EquipmentShape shape;
        final Set<ShapeRasterizer.Cell> shell, interior;
        final Map<Long, Boolean> cache = new HashMap<>();
        double[] p = new double[3], v = new double[3];
        boolean onGround, hColl, vColl;
        String blocker = "", head = "";
        String[] axisBlocker = new String[3];
        boolean trace;

        Sim(EquipmentShape shape) {
            this.shape = shape;
            var layout = ShapeRasterizer.layout(shape);
            shell = new HashSet<>(layout.shell());
            interior = new HashSet<>(layout.interior());
        }

        Sim copy() {
            var s = new Sim(shape, shell, interior, cache);
            s.p = p.clone();
            s.v = v.clone();
            s.onGround = onGround;
            return s;
        }

        private Sim(EquipmentShape shape, Set<ShapeRasterizer.Cell> shell, Set<ShapeRasterizer.Cell> interior, Map<Long, Boolean> cache) {
            this.shape = shape;
            this.shell = shell;
            this.interior = interior;
        }

        boolean voxel(int i, int j, int k) {
            long key = (((long) i & 0x1FFFFF) << 42) | (((long) j & 0x1FFFFF) << 21) | ((long) k & 0x1FFFFF);
            var hit = cache.get(key);
            if (hit != null) return hit;
            var cell = new ShapeRasterizer.Cell(Math.floorDiv(i, 8), Math.floorDiv(j, 8), Math.floorDiv(k, 8));
            boolean solid = j < 0 || interior.contains(cell) || shell.contains(cell) && ShapeRasterizer.inside(shape, (i + .5) / 8, (j + .5) / 8, (k + .5) / 8);
            cache.put(key, solid);
            return solid;
        }

        boolean ladder(double[] q) {
            var cell = new ShapeRasterizer.Cell((int) Math.floor(q[0]), (int) Math.floor(q[1]), (int) Math.floor(q[2]));
            return shell.contains(cell) && ShapeRasterizer.climbable(shape, q[0], q[1], q[2]);
        }

        double[] box(double[] q) {
            return new double[]{q[0] - W, q[1], q[2] - W, q[0] + W, q[1] + H, q[2] + W};
        }

        /** How far the box moves along {@code axis} of {@code d}, clipped by solid voxels. */
        double clip(double[] b, int axis, double d) {
            if (d == 0) return 0;
            int u = (axis + 1) % 3, w = (axis + 2) % 3;
            int u0 = (int) Math.floor(b[u] * 8 + 1e-7), u1 = (int) Math.ceil(b[u + 3] * 8 - 1e-7) - 1;
            int w0 = (int) Math.floor(b[w] * 8 + 1e-7), w1 = (int) Math.ceil(b[w + 3] * 8 - 1e-7) - 1;
            int[] idx = new int[3];
            if (d > 0) {
                double edge = b[axis + 3];
                for (int a = (int) Math.ceil(edge * 8 - 1e-6); a / 8.0 < edge + d; a++)
                    for (int uu = u0; uu <= u1; uu++)
                        for (int ww = w0; ww <= w1; ww++) {
                            idx[axis] = a;
                            idx[u] = uu;
                            idx[w] = ww;
                            if (voxel(idx[0], idx[1], idx[2])) {
                                blocker = "voxel " + (idx[0] + .5) / 8 + "," + (idx[1] + .5) / 8 + "," + (idx[2] + .5) / 8;
                                return Math.max(0, Math.min(d, a / 8.0 - edge));
                            }
                        }
            } else {
                double edge = b[axis];
                for (int a = (int) Math.floor(edge * 8 + 1e-6) - 1; (a + 1) / 8.0 > edge + d; a--)
                    for (int uu = u0; uu <= u1; uu++)
                        for (int ww = w0; ww <= w1; ww++) {
                            idx[axis] = a;
                            idx[u] = uu;
                            idx[w] = ww;
                            if (voxel(idx[0], idx[1], idx[2])) {
                                blocker = "voxel " + (idx[0] + .5) / 8 + "," + (idx[1] + .5) / 8 + "," + (idx[2] + .5) / 8;
                                return Math.min(0, Math.max(d, (a + 1) / 8.0 - edge));
                            }
                        }
            }
            return d;
        }

        double[] collide(double[] b0, double[] d) {
            var b = b0.clone();
            double[] out = new double[3];
            int[] order = Math.abs(d[0]) < Math.abs(d[2]) ? new int[]{1, 2, 0} : new int[]{1, 0, 2};
            for (int axis : order) {
                blocker = "";
                double m = clip(b, axis, d[axis]);
                axisBlocker[axis] = Math.abs(m - d[axis]) > 1e-9 ? blocker : "";
                out[axis] = m;
                b[axis] += m;
                b[axis + 3] += m;
            }
            return out;
        }

        void move(double[] d) {
            var b = box(p);
            var r = collide(b, d);
            boolean hx = Math.abs(r[0] - d[0]) > 1e-9, hz = Math.abs(r[2] - d[2]) > 1e-9, vy = Math.abs(r[1] - d[1]) > 1e-9;
            if ((onGround || vy && d[1] < 0) && (hx || hz)) {
                var b1 = b.clone();
                if (vy) {
                    b1[1] += r[1];
                    b1[4] += r[1];
                }
                double base = b1[1];
                double best = r[0] * r[0] + r[2] * r[2];
                for (int k = (int) Math.ceil(base * 8 + 1e-6); k / 8.0 <= base + STEP + 1e-9; k++) {
                    double h = k / 8.0 - base;
                    var s = collide(b1, new double[]{d[0], h, d[2]});
                    if (s[0] * s[0] + s[2] * s[2] > best + 1e-12) {
                        r = new double[]{s[0], s[1] + (b1[1] - b[1]), s[2]};
                        hx = Math.abs(r[0] - d[0]) > 1e-9;
                        hz = Math.abs(r[2] - d[2]) > 1e-9;
                        vy = false;
                        break;
                    }
                }
            }
            for (int a = 0; a < 3; a++) p[a] += r[a];
            hColl = hx || hz;
            vColl = vy;
            onGround = vy && d[1] < 0;
            if (hx) v[0] = 0;
            if (hz) v[2] = 0;
            if (vy) v[1] = 0;
            if (vy && d[1] > 0) head = axisBlocker[1] + " at feet " + String.format("%.2f", p[1]);
            if (trace) System.out.printf("PROBE    p (%.3f, %.3f, %.3f) d (%.3f, %.3f, %.3f) r (%.3f, %.3f, %.3f) h %s v %s ground %s ladder %s | x %s | y %s | z %s%n", p[0], p[1], p[2], d[0], d[1], d[2], r[0], r[1], r[2], hColl, vColl, onGround, ladder(p), axisBlocker[0], axisBlocker[1], axisBlocker[2]);
        }

        /** One tick of a player's travel with horizontal wish {@code wish} (world, length at most 1), holding jump or not. */
        void tick(double[] wish, boolean jump) {
            if (jump && onGround && !ladder(p)) v[1] = .42;
            double speed = onGround ? .1 : .02;
            v[0] += wish[0] * .98 * speed;
            v[2] += wish[2] * .98 * speed;
            if (ladder(p)) {
                v[0] = Math.max(-.15, Math.min(.15, v[0]));
                v[2] = Math.max(-.15, Math.min(.15, v[2]));
                v[1] = Math.max(v[1], -.15);
            }
            move(v.clone());
            if ((hColl || jump) && ladder(p)) v[1] = .2;
            v[1] = (v[1] - .08) * .98;
            double f = onGround ? .6 * .91 : .91;
            v[0] *= f;
            v[2] *= f;
        }
    }

    static double[] dir(EquipmentShape shape, double[] frameAt, double[] frameDir) {
        var t = shape.transform();
        var a = t.point(frameAt);
        var b = t.point(new double[]{frameAt[0] + frameDir[0], frameAt[1] + frameDir[1], frameAt[2] + frameDir[2]});
        return new double[]{b[0] - a[0], b[1] - a[1], b[2] - a[2]};
    }

    /** Null when the player gets off at the landing, else why not. */
    static String tryLadder(EquipmentShape shape, Access.Climb l) {
        var sim = new Sim(shape);
        double[] frameFoot = {l.x(), l.base(), l.z()};
        double[] out = dir(shape, frameFoot, new double[]{l.outX(), 0, l.outZ()}), across = {-out[2], 0, out[0]}, in = {-out[0], 0, -out[2]};
        var start = start(shape, l);
        sim.p = start;
        for (int i = 0; i < 10; i++) sim.tick(new double[3], false);
        double settled = sim.p[1], foot = shape.transform().point(frameFoot)[1];
        if (settled < foot - .2) return String.format("no footing: start %.2f fell to %.2f (start %s)", foot, settled, fmt(start));
        double landing = shape.transform().point(new double[]{l.x(), l.top() - Access.LANDING, l.z()})[1];
        var center = shape.transform().point(new double[]{l.x(), 0, l.z()});
        double top = 0;
        for (int i = 0; i < 600; i++) {
            sim.tick(steer(sim, in, center, across, 1), false);
            top = Math.max(top, sim.p[1]);
            double ahead0 = (sim.p[0] - center[0]) * out[0] + (sim.p[2] - center[2]) * out[2];
            if (sim.onGround && !sim.ladder(sim.p) && ahead0 < -.05 && sim.p[1] > landing - .07 && sim.p[1] < landing + .6) return null;
            if (sim.p[1] > landing + 1.2) break;
        }
        if (top < landing - .01) return String.format("climb stops at %.2f, landing %.2f (head %s; ladder %s; off %.2f)", top, landing, sim.head, sim.ladder(sim.p), offAcross(sim, center, across));
        boolean walkThrough = l.rungTop() < l.top() - 1e-9;
        String best = "";
        double bestFeet = -1e9;
        for (int mode = 0; mode < 7; mode++)
            for (boolean jump : new boolean[]{false, true}) {
                if (mode == 0 && !walkThrough) continue;
                var s = sim.copy();
                int side = mode % 2 == 1 ? 1 : -1;
                double k = mode <= 2 ? 0 : mode <= 4 ? .5 : -.5;
                double off = 0, ahead = 0;
                for (int i = 0; i < 80; i++) {
                    double[] wish = mode == 0 ? steer(s, in, center, across, 1) : unit(new double[]{side * across[0] + k * in[0], 0, side * across[2] + k * in[2]});
                    s.tick(wish, jump && i < 40);
                    off = Math.abs(offAcross(s, center, across));
                    ahead = (s.p[0] - center[0]) * out[0] + (s.p[2] - center[2]) * out[2];
                    boolean offLadder = !s.ladder(s.p) && (off > Access.Climb.HALF || ahead < -.05);
                    if (s.onGround && offLadder && s.p[1] > landing - .07 && s.p[1] < landing + .6) return null;
                }
                if (s.p[1] > bestFeet) {
                    bestFeet = s.p[1];
                    best = String.format("mode %d jump %s: feet %.2f (landing %.2f) off %.2f ahead %.2f ground %s ladder %s last %s", mode, jump, s.p[1], landing, off, ahead, s.onGround,
                            s.ladder(s.p), s.blocker);
                }
            }
        String last = best;
        return (walkThrough ? "walk-through " : "side-step ") + last;
    }

    /** Where a player stands to climb: clear of the rung plane by the box's reach along {@code out} and a voxel, on top of the voxel the base is in. */
    static double[] start(EquipmentShape shape, Access.Climb l) {
        double reach = Access.RUNG_FACE + W * (Math.abs(l.outX()) + Math.abs(l.outZ())) + .15;
        var q = shape.transform().point(new double[]{l.x() + reach * l.outX(), l.base(), l.z() + reach * l.outZ()});
        q[1] = Math.ceil(q[1] * 8 - 1e-6) / 8 + .01;
        return q;
    }

    static double offAcross(Sim s, double[] center, double[] across) {
        return (s.p[0] - center[0]) * across[0] + (s.p[2] - center[2]) * across[2];
    }

    /** {@code wish} steered back to the ladder's middle line, at most unit length. */
    static double[] steer(Sim s, double[] wish, double[] center, double[] across, double gain) {
        double c = Math.max(-1, Math.min(1, -4 * gain * offAcross(s, center, across)));
        return unit(new double[]{wish[0] + c * across[0], 0, wish[2] + c * across[2]});
    }

    static double[] unit(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[2] * a[2]);
        return l <= 1 ? a : new double[]{a[0] / l, 0, a[2] / l};
    }

    static String fmt(double[] a) {
        return String.format("(%.2f,%.2f)", a[0], a[2]);
    }

    static void probe(List<EquipmentShape> shapes) {
        int ladders = 0, stuck = 0;
        var kinds = new TreeMap<String, int[]>();
        for (var shape : shapes) {
            for (var l : ShapeRasterizer.climbs(shape)) {
                ladders++;
                String why = PlayerWalk.leave(shape, l);
                var k = kinds.computeIfAbsent(shape.kind().toString(), x -> new int[2]);
                k[0]++;
                if (why != null) {
                    stuck++;
                    k[1]++;
                    System.out.printf("PROBE STUCK %s %.2fx%.0f s%d ladder base %.2f top %.2f rungTop %.2f: %s%n", shape.kind(), shape.diameter(), shape.tangentLength(), shape.support(), l.base(),
                            l.top(), l.rungTop(), why);
                }
            }
        }
        System.out.println("PROBE ladders " + ladders + " stuck " + stuck + " by kind " + kinds.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()[1] + "/" + e.getValue()[0]).toList());
    }

    @Test
    void trace() {
        for (var shape : List.of(EquipmentShape.unit(Kind.VERTICAL_HEATER, 5.5, 9, Heading.NORTH, 3), EquipmentShape.unit(Kind.TRICKLE_BED_REACTOR, 1, 1, Heading.NORTH, 3))) {
            var l = ProcessUnit.of(shape).ladders().getFirst();
            System.out.println("PROBE TRACE " + shape.kind() + " " + l);
            traceClimb(shape, l, 90);
        }
    }

    static void traceClimb(EquipmentShape shape, Access.Climb l, int ticks) {
        var sim = new Sim(shape);
        double[] frameFoot = {l.x(), l.base(), l.z()};
        double[] out = dir(shape, frameFoot, new double[]{l.outX(), 0, l.outZ()}), across = {-out[2], 0, out[0]}, in = {-out[0], 0, -out[2]};
        sim.p = start(shape, l);
        var center = shape.transform().point(new double[]{l.x(), 0, l.z()});
        System.out.println("PROBE   out " + fmt(out) + " start " + java.util.Arrays.toString(sim.p) + " center " + java.util.Arrays.toString(center));
        sim.trace = true;
        for (int i = 0; i < ticks; i++) sim.tick(i < 10 ? new double[3] : steer(sim, in, center, across, 1), false);
    }

    @Test
    void traceWalk() {
        for (var shape : List.of(EquipmentShape.unit(Kind.ADSORBER, 1.6, 1, Heading.NORTH, 2), EquipmentShape.unit(Kind.STIRRED_REACTOR, 3.6, 3, Heading.NORTH, 1),
                EquipmentShape.unit(Kind.CYCLONE, 1.2, 1, Heading.NORTH, 4))) {
            var l = ProcessUnit.of(shape).ladders().getFirst();
            var sim = new Sim(shape);
            double[] frameFoot = {l.x(), l.base(), l.z()};
            double[] out = dir(shape, frameFoot, new double[]{l.outX(), 0, l.outZ()}), across = {-out[2], 0, out[0]}, in = {-out[0], 0, -out[2]};
            sim.p = shape.transform().point(new double[]{l.x() + (Access.RUNG_FACE + .45) * l.outX(), l.base() + .01, l.z() + (Access.RUNG_FACE + .45) * l.outZ()});
            var center = shape.transform().point(new double[]{l.x(), 0, l.z()});
            double landing = shape.transform().point(new double[]{l.x(), l.top() - Access.LANDING, l.z()})[1];
            for (int i = 0; i < 10; i++) sim.tick(new double[3], false);
            System.out.printf("PROBE %s landing %.2f%n", shape.kind(), landing);
            for (int i = 0; i < 160; i++) {
                sim.tick(steer(sim, in, center, across, 1), false);
                double ahead = (sim.p[0] - center[0]) * out[0] + (sim.p[2] - center[2]) * out[2];
                if (sim.p[1] > landing - .5) System.out.printf("PROBE   t%d feet %.3f ahead %.2f off %.2f ground %s ladder %s h %s | x %s | y %s | z %s%n", i, sim.p[1], ahead, offAcross(sim, center, across), sim.onGround,
                        sim.ladder(sim.p), sim.hColl, sim.axisBlocker[0], sim.axisBlocker[1], sim.axisBlocker[2]);
            }
        }
    }

    @Test
    void whatIsAt() {
        var h = EquipmentShape.storage(Kind.TANK, 20, 12, Heading.NORTH, 0);
        var u = ProcessUnit.of(h);
        var roof = ShapeRasterizer.roof(h);
        System.out.println("PROBE pivot " + java.util.Arrays.toString(roof.pivot()) + " along " + java.util.Arrays.toString(roof.along()) + " axis " + roof.x() + "," + roof.z());
        for (double[] q : new double[][]{{4.5625, 11.0625, 12.4375}, {3.9375, 10.9375, 12.4375}}) {
            System.out.println("PROBE point " + java.util.Arrays.toString(q) + " inside " + ShapeRasterizer.inside(h, q[0], q[1], q[2]) + " vessel " + ShapeRasterizer.insideVessel(h, q[0], q[1], q[2]) + " unit " + u.inside(h.transform().local(q)));
            for (var sd : u.solids) if (sd.contains(h.transform().local(q))) System.out.println("PROBE   solid " + java.util.Arrays.toString(sd.min()) + " .. " + java.util.Arrays.toString(sd.max()) + " axis " + sd.axis() + " r " + sd.radius() + " inner " + sd.inner());
        }
    }

    static void traceExit(EquipmentShape h, double base) {
        var ls = ShapeRasterizer.climbs(h);
        var l = ls.stream().filter(c -> Math.abs(c.base() - base) < .01).findFirst().orElseThrow();
        System.out.println("PROBE EXIT " + h.kind() + " " + h.diameter() + " " + l);
        var sim = new Sim(h);
        double[] frameFoot = {l.x(), l.base(), l.z()};
        double[] out = dir(h, frameFoot, new double[]{l.outX(), 0, l.outZ()}), across = {-out[2], 0, out[0]}, in = {-out[0], 0, -out[2]};
        sim.p = start(h, l);
        var center = h.transform().point(new double[]{l.x(), 0, l.z()});
        double landing = h.transform().point(new double[]{l.x(), l.top() - Access.LANDING, l.z()})[1];
        for (int i = 0; i < 10; i++) sim.tick(new double[3], false);
        int i = 0;
        for (; i < 600 && sim.p[1] < landing + .3; i++) {
            sim.tick(steer(sim, in, center, across, 1), false);
            if (i < 12) System.out.printf("PROBE   t%d feet %.3f ahead %.2f off %.2f ladder %s h %s | x %s | z %s%n", i, sim.p[1], (sim.p[0] - center[0]) * out[0] + (sim.p[2] - center[2]) * out[2], offAcross(sim, center, across), sim.ladder(sim.p), sim.hColl, sim.axisBlocker[0], sim.axisBlocker[2]);
        }
        System.out.printf("PROBE   climbed %d ticks feet %.3f landing %.3f ahead %.2f off %.2f%n", i, sim.p[1], landing, (sim.p[0] - center[0]) * out[0] + (sim.p[2] - center[2]) * out[2], offAcross(sim, center, across));
        for (int mode = 1; mode < 7; mode++) {
            var s = sim.copy();
            int side = mode % 2 == 1 ? 1 : -1;
            double k = mode <= 2 ? 0 : mode <= 4 ? .5 : -.5;
            var sb = new StringBuilder();
            for (int t = 0; t < 40; t++) {
                s.tick(unit(new double[]{side * across[0] + k * in[0], 0, side * across[2] + k * in[2]}), false);
                if (t % 4 == 3) sb.append(String.format(" [%.2f a%.2f o%.2f %s%s]", s.p[1], (s.p[0] - center[0]) * out[0] + (s.p[2] - center[2]) * out[2], offAcross(s, center, across), s.onGround ? "G" : "", s.hColl ? "H" + s.axisBlocker[0] + s.axisBlocker[2] : ""));
            }
            System.out.println("PROBE   mode " + mode + sb);
        }
    }

    static void at(EquipmentShape shape, double[] f, String what) {
        var u = ProcessUnit.of(shape);
        System.out.println("PROBE " + what + " frame " + java.util.Arrays.toString(f));
        for (var sd : u.solids) if (sd.contains(f)) System.out.println("PROBE   solid " + java.util.Arrays.toString(sd.min()) + " .. " + java.util.Arrays.toString(sd.max()) + " axis " + sd.axis() + " r " + sd.radius() + " inner " + sd.inner());
    }

    @Test
    void traceFcc() {
        var shape = EquipmentShape.unit(Kind.FCC_CONVERTER, 6, 26, Heading.NORTH, 8);
        var l = ProcessUnit.of(shape).ladders().getFirst();
        var sim = new Sim(shape);
        double[] frameFoot = {l.x(), l.base(), l.z()};
        double[] out = dir(shape, frameFoot, new double[]{l.outX(), 0, l.outZ()}), across = {-out[2], 0, out[0]}, in = {-out[0], 0, -out[2]};
        sim.p = shape.transform().point(new double[]{l.x() + (Access.RUNG_FACE + .45) * l.outX(), l.base() + .01, l.z() + (Access.RUNG_FACE + .45) * l.outZ()});
        var center = shape.transform().point(new double[]{l.x(), 0, l.z()});
        double landing = shape.transform().point(new double[]{l.x(), l.top() - Access.LANDING, l.z()})[1];
        for (int i = 0; i < 10; i++) sim.tick(new double[3], false);
        int i = 0;
        for (; i < 600 && sim.p[1] < landing + 1.2; i++) sim.tick(steer(sim, in, center, across, 1), false);
        System.out.printf("PROBE climbed %d ticks to %.3f, landing %.2f, off %.2f ladder %s%n", i, sim.p[1], landing, offAcross(sim, center, across), sim.ladder(sim.p));
        sim.trace = true;
        for (int k = 0; k < 30; k++) sim.tick(new double[]{-across[0], 0, -across[2]}, false);
    }

    @Test
    void decksNear() {
        dump(EquipmentShape.unit(Kind.FCC_CONVERTER, 6, 26, Heading.NORTH, 8), 0);
        dump(EquipmentShape.unit(Kind.FLUIDIZED_BED_GASIFIER, 2.25, 1, Heading.NORTH, 7), -1);
        dump(EquipmentShape.unit(Kind.FLUIDIZED_BED_GASIFIER, 1.5, 1, Heading.NORTH, 6), -1);
    }

    /** Each ladder (or ladder k) of the shape in the frame, and the decks within 3 m of its landing level, as along/across spans from the rung plane. */
    static void dump(EquipmentShape shape, int only) {
        var unit = ProcessUnit.of(shape);
        var ls = unit.ladders();
        for (int i = 0; i < ls.size(); i++) {
            if (only >= 0 && i != only) continue;
            var l = ls.get(i);
            double landing = l.top() - Access.LANDING;
            System.out.printf("PROBE %s %.2fx%.0f ladder %d at (%.2f, %.2f) out (%.2f, %.2f) base %.2f landing %.2f rungTop %.2f%n", shape.kind(), shape.diameter(), shape.tangentLength(), i, l.x(), l.z(), l.outX(),
                    l.outZ(), l.base(), landing, l.rungTop());
            for (var d : unit.decks()) {
                double y = d.level() + Access.DECK;
                if (Math.abs(y - landing) > .01 && Math.abs(y - l.base()) > .01) continue;
                double alo = 1e9, ahi = -1e9, clo = 1e9, chi = -1e9;
                int n = 16;
                for (int j = 0; j <= n; j++)
                    for (double off : new double[]{Access.GAP, d.width()}) {
                        var q = d.run().at(d.run().length() * j / n, off, y);
                        double al = (q[0] - l.x()) * l.outX() + (q[2] - l.z()) * l.outZ(), ac = -(q[0] - l.x()) * l.outZ() + (q[2] - l.z()) * l.outX();
                        alo = Math.min(alo, al); ahi = Math.max(ahi, al); clo = Math.min(clo, ac); chi = Math.max(chi, ac);
                    }
                if (alo > 3 || ahi < -3 || clo > 3 || chi < -3) continue;
                System.out.printf("PROBE    deck at %.2f (%s) along %.2f..%.2f across %.2f..%.2f closed %s/%s%n", y, Math.abs(y - landing) < .01 ? "landing" : "base", alo, ahi, clo, chi, d.closedStart(), d.closedEnd());
            }
        }
    }

    @Test
    void heaters() {
        probe(List.of(EquipmentShape.unit(Kind.VERTICAL_HEATER, 2.5, 4, Heading.NORTH, 3), EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 9, Heading.EAST, 3),
                EquipmentShape.unit(Kind.VERTICAL_HEATER, 12, 20, Heading.SOUTH, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 4, 6, Heading.NORTH, 3),
                EquipmentShape.unit(Kind.CABIN_HEATER, 6, 12, Heading.WEST, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 12, 40, Heading.NORTH, 4)));
    }

    @Test
    void gasifiers() {
        probe(ProcessUnitTest.everySize().stream().filter(x -> x.kind() == Kind.FLUIDIZED_BED_GASIFIER || x.kind() == Kind.CYCLONE).toList());
    }

    /** Solids met walking each deck's middle line at knee, waist and head height. */
    @Test
    void deckLanes() {
        int decks = 0, blocked = 0;
        for (var shape : ProcessUnitTest.everySize()) {
            if (shape.kind() != Kind.FLUIDIZED_BED_GASIFIER) continue;
            var u = ProcessUnit.of(shape);
            for (var d : u.decks()) {
                decks++;
                double off = (Access.GAP + d.width() - Access.RAIL_BAND) / 2, y = d.level() + Access.DECK;
                var hits = new ArrayList<String>();
                for (double t = .5; t < d.run().length() - .5; t += .1)
                  for (double o = Access.GAP + .1; o < d.width() - Access.RAIL_BAND - .04; o += .15)
                    for (double h : new double[]{.3, 1, 1.6}) {
                        var q = d.run().at(t, o, y + h);
                        if (u.inside(q) && !u.climbable(q)) hits.add(String.format("s%.1f h%.1f (%.2f,%.2f,%.2f)", t, h, q[0], q[1], q[2]));
                    }
                if (!hits.isEmpty()) {
                    blocked++;
                    System.out.printf("PROBE LANE %.2fx%.0f s%d deck %.2f len %.2f w %.2f: %d hits %s%n", shape.diameter(), shape.tangentLength(), shape.support(), d.level(), d.run().length(), d.width(), hits.size(), hits.subList(0, Math.min(3, hits.size())));
                }
            }
        }
        System.out.println("PROBE LANES decks " + decks + " blocked " + blocked);
    }

    @Test
    void others() {
        var shapes = new ArrayList<EquipmentShape>();
        for (var sh : ShapeRasterizerTest.shapes()) if (!sh.kind().designed()) shapes.add(sh);
        shapes.add(EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 4));
        shapes.add(EquipmentShape.column(6, new ColumnTrays(40, 20, List.of(10, 25), List.of(41), List.of(new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(30, 28))), Heading.NORTH, 5));
        shapes.add(EquipmentShape.column(1, ColumnTrays.simple(20, 10), Heading.EAST, 3));
        shapes.add(EquipmentShape.column(15, ColumnTrays.simple(30, 15), Heading.WEST, 16));
        for (int d : new int[]{4, 5, 8, 12, 20, 30, 40}) for (int sh : new int[]{4, 8, 12, 24}) shapes.add(EquipmentShape.storage(Kind.TANK, d, sh, Heading.NORTH, 0));
        for (int d : new int[]{4, 9, 14, 24}) for (int sp : new int[]{2, 5, 8}) shapes.add(EquipmentShape.storage(Kind.SPHERE, d, 1, Heading.NORTH, sp));
        probe(shapes);
    }

    @Test
    void everyUnit() {
        probe(ProcessUnitTest.everySize());
    }
}
