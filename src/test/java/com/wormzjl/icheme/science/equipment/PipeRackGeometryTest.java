package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The rack's diagonal slots (batch 2026-10-26-pipe-laying-system, plan 5.1): every pair of lanes in disjoint slot ranges,
 * at the largest size their widths carry, turned through every pair of faces, keeps 1/64 m between their surfaces; every
 * lane shows from every side; and lanes stay inside the block and clear of the frame.
 */
class PipeRackGeometryTest {

    @Test
    void aLineTakesTheSlotsItsSizeNeeds() {
        assertEquals(1, RackGeometry.width(.168), "6 inch");
        assertEquals(2, RackGeometry.width(.356), "14 inch");
        assertEquals(3, RackGeometry.width(.508), "20 inch");
        assertEquals(4, RackGeometry.width(.711), "28 inch");
        assertEquals(5, RackGeometry.width(PipeSizes.MAX_OUTSIDE));
        for (int width = 1; width < RackGeometry.SLOTS; width++) assertEquals(width, RackGeometry.width(RackGeometry.largest(width)));
        assertEquals(.172, RackGeometry.largest(1), .001);
        assertEquals(List.of(0, 1, 3, 4), RackGeometry.free(1 << 2, 1));
        assertEquals(List.of(0, 3), RackGeometry.free(1 << 2, 2), "a wide line takes an outer range first");
        assertEquals(List.of(), RackGeometry.free(1 << 2, 3));
        assertEquals(List.of(0, 2, 1), RackGeometry.free(0, 3), "the middle range last");
    }

    @Test
    void everyPairOfLanesKeepsItsClearanceWhateverFacesTheyUse() {
        var lanes = lanes();
        double worst = Double.MAX_VALUE;
        for (var a : lanes)
            for (var b : lanes) {
                if ((a.mask() & b.mask()) != 0 || a.slot() >= b.slot()) continue;
                double ra = PipeSizes.outsideOf(a.outside()) / 2, rb = PipeSizes.outsideOf(b.outside()) / 2;
                for (int ja : jointPairs())
                    for (int jb : jointPairs()) {
                        var pa = centreline(new RackGeometry.Lane(a.slot(), a.width(), ja, ja, a.outside()));
                        var pb = centreline(new RackGeometry.Lane(b.slot(), b.width(), jb, jb, b.outside()));
                        double gap = Math.min(distance(pa, pb) - ra - rb, Math.min(ball(a, ja, pb, rb), ball(b, jb, pa, ra)));
                        worst = Math.min(worst, gap);
                        // Each chord spans 1/64 of a quarter turn: it lies at most 0.05 mm inside the bend it stands for.
                        assertTrue(gap >= RackGeometry.CLEARANCE - 2e-4, "lanes " + a + " " + b + " joints " + ja + "/" + jb + " gap " + gap);
                    }
            }
        assertEquals(RackGeometry.CLEARANCE, worst, 2e-4, "the narrowest gap is the clearance the widths are sized for");
    }

    @Test
    void everyLaneShowsFromEverySide() {
        // Seen along any axis, a lane's arms lie at its own centre coordinate; disjoint lanes' coordinates differ by more
        // than their radii, so neither covers the other except where two arms cross.
        for (var a : lanes())
            for (var b : lanes()) {
                if ((a.mask() & b.mask()) != 0 || a.slot() >= b.slot()) continue;
                double apart = Math.abs(a.centre() - b.centre()) - PipeSizes.outsideOf(a.outside()) / 2 - PipeSizes.outsideOf(b.outside()) / 2;
                assertTrue(apart >= RackGeometry.CLEARANCE - 1e-9, a + " hides " + b);
            }
    }

    @Test
    void lanesStayInTheirBlockAndClearOfTheFrame() {
        for (int axis = 0; axis < 3; axis++) {
            var members = RackGeometry.members(axis);
            for (var lane : lanes())
                for (int joints : jointPairs()) {
                    var drawn = new RackGeometry.Lane(lane.slot(), lane.width(), joints, 0, lane.outside());
                    var mesh = new Mesh();
                    RackGeometry.lane(mesh, drawn, 5);
                    for (var polygon : mesh.cell(0, 0, 0)) {
                        var p = polygon.positions();
                        for (int v = 0; v < p.length; v += 3) {
                            for (int k = 0; k < 3; k++) assertTrue(p[v + k] >= -1e-9 && p[v + k] <= 1 + 1e-9, drawn + " leaves its block");
                            for (var box : members)
                                assertFalse(p[v] > box[0] + 1e-6 && p[v] < box[3] - 1e-6 && p[v + 1] > box[1] + 1e-6 && p[v + 1] < box[4] - 1e-6 && p[v + 2] > box[2] + 1e-6 && p[v + 2] < box[5] - 1e-6,
                                        drawn + " runs into the frame along axis " + axis);
                        }
                    }
                }
        }
    }

    @Test
    void anOpenLaneEndIsCappedAndAJoinedOneIsNot() {
        var open = new Mesh();
        RackGeometry.lane(open, new RackGeometry.Lane(1, 1, 1 << 4 | 1 << 5, 0, PipeSizes.code(.1)), 5);
        var joined = new Mesh();
        RackGeometry.lane(joined, new RackGeometry.Lane(1, 1, 1 << 4 | 1 << 5, 1 << 4 | 1 << 5, PipeSizes.code(.1)), 5);
        assertTrue(open.cell(0, 0, 0).size() > joined.cell(0, 0, 0).size(), "the open lane has two caps");
        for (var box : RackGeometry.boxes(new RackGeometry.Lane(1, 1, 1 << 4 | 1 << 5, 0, PipeSizes.code(.1)), 5))
            assertTrue(box[0] == 0 || box[3] == 1, "an arm box reaches its face");
    }

    /** Each width at the largest size it carries, in every slot it fits. */
    private static List<RackGeometry.Lane> lanes() {
        var out = new ArrayList<RackGeometry.Lane>();
        for (int width = 1; width <= RackGeometry.SLOTS; width++) {
            int code = PipeSizes.code(RackGeometry.largest(width));
            while (PipeSizes.outsideOf(code) > RackGeometry.largest(width) + 1e-12) code--;
            for (int slot = 0; slot + width <= RackGeometry.SLOTS; slot++) out.add(new RackGeometry.Lane(slot, width, 0, 0, code));
        }
        return out;
    }

    /** Every pair of distinct faces: straight runs and elbows. */
    private static List<Integer> jointPairs() {
        var out = new ArrayList<Integer>();
        for (int a = 0; a < 6; a++) for (int b = a + 1; b < 6; b++) out.add(1 << a | 1 << b);
        return out;
    }

    /** A lane's centreline as segments: its arms from the elbow's tangent points to the faces, and the elbow's arc in {@link #CHORDS} chords. */
    private static List<double[][]> centreline(RackGeometry.Lane lane) {
        var out = new ArrayList<double[][]>();
        double c = lane.centre();
        int arms = RackGeometry.arms(lane, 5), a = Integer.numberOfTrailingZeros(arms), b = 31 - Integer.numberOfLeadingZeros(arms);
        double bend = (a ^ 1) == b ? 0 : RackGeometry.elbow(lane, a, b);
        if (bend < 1.05 * PipeSizes.outsideOf(lane.outside()) / 2) bend = 0;
        for (int face : new int[]{a, b}) {
            var step = LineGeometry.STEPS[face];
            double length = RackGeometry.arm(lane, face);
            out.add(new double[][]{{c + bend * step[0], c + bend * step[1], c + bend * step[2]}, {c + length * step[0], c + length * step[1], c + length * step[2]}});
        }
        if (bend > 0) {
            int[] da = LineGeometry.STEPS[a], db = LineGeometry.STEPS[b];
            double[] o = {c + bend * (da[0] + db[0]), c + bend * (da[1] + db[1]), c + bend * (da[2] + db[2])};
            double[] last = null;
            for (int k = 0; k <= CHORDS; k++) {
                double t = Math.PI / 2 * k / CHORDS;
                double[] p = {o[0] - bend * (db[0] * Math.cos(t) + da[0] * Math.sin(t)), o[1] - bend * (db[1] * Math.cos(t) + da[1] * Math.sin(t)),
                        o[2] - bend * (db[2] * Math.cos(t) + da[2] * Math.sin(t))};
                if (last != null) out.add(new double[][]{last, p});
                last = p;
            }
        }
        return out;
    }

    private static final int CHORDS = 64;

    /**
     * The gap from the ball a lane turns in where its elbow has no room (its centre, at the ball's drawn radius, a little
     * over the pipe's) to another lane's centreline {@code other} of radius {@code r}; unbounded when it has an elbow.
     */
    private static double ball(RackGeometry.Lane lane, int joints, List<double[][]> other, double r) {
        int a = Integer.numberOfTrailingZeros(joints), b = 31 - Integer.numberOfLeadingZeros(joints);
        double od = PipeSizes.outsideOf(lane.outside());
        var turned = new RackGeometry.Lane(lane.slot(), lane.width(), joints, joints, lane.outside());
        if ((a ^ 1) == b || RackGeometry.elbow(turned, a, b) >= 1.05 * od / 2) return Double.MAX_VALUE;
        double c = lane.centre(), drawn = Math.min(c, od / 2 / Math.cos(Math.PI / PipeSizes.sides(od)));
        double[] centre = {c, c, c};
        double best = Double.MAX_VALUE;
        for (var s : other) best = Math.min(best, segments(centre, centre, s[0], s[1]));
        return best - drawn - r;
    }

    private static double distance(List<double[][]> a, List<double[][]> b) {
        double best = Double.MAX_VALUE;
        for (var s : a) for (var t : b) best = Math.min(best, segments(s[0], s[1], t[0], t[1]));
        return best;
    }

    /** The distance between segments p0-p1 and q0-q1 (Ericson, Real-Time Collision Detection, 5.1.9). */
    private static double segments(double[] p0, double[] p1, double[] q0, double[] q1) {
        double[] d1 = sub(p1, p0), d2 = sub(q1, q0), r = sub(p0, q0);
        double a = dot(d1, d1), e = dot(d2, d2), f = dot(d2, r), s, t;
        if (a < 1e-15 && e < 1e-15) return Math.sqrt(dot(r, r));
        if (a < 1e-15) {
            s = 0;
            t = clamp(f / e);
        } else {
            double c = dot(d1, r);
            if (e < 1e-15) {
                t = 0;
                s = clamp(-c / a);
            } else {
                double b = dot(d1, d2), denominator = a * e - b * b;
                s = denominator > 1e-15 ? clamp((b * f - c * e) / denominator) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = clamp(-c / a);
                } else if (t > 1) {
                    t = 1;
                    s = clamp((b - c) / a);
                }
            }
        }
        double[] closest = {p0[0] + d1[0] * s - q0[0] - d2[0] * t, p0[1] + d1[1] * s - q0[1] - d2[1] * t, p0[2] + d1[2] * s - q0[2] - d2[2] * t};
        return Math.sqrt(dot(closest, closest));
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
