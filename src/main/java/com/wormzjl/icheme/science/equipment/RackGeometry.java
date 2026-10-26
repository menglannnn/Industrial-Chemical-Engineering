package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.icheme.science.equipment.Mesh.*;

/**
 * A pipe rack cell (batch 2026-10-26-pipe-laying-system, plan 5): five slots on the block's main diagonal, the lanes laid
 * in them, and the thin frame around them.
 *
 * <p>Slot k's centre is (c_k, c_k, c_k), c_k = 1/8 + 3k/16, in block coordinates, the same in every rack whatever its
 * axis. Two slots differ by at least one pitch (3/16 m) in every coordinate, so arms of lanes in disjoint slot ranges stay
 * apart whatever faces they use, every lane shows from every side, and a lane keeps its slot from rack to rack. A lane of
 * m slots carries a line up to m pitches less 1/64 m across; it turns with a short elbow, of radius at most (m + 1)/2
 * pitches, because a long-radius elbow would cut into the next slot's lane.</p>
 */
public final class RackGeometry {
    public static final int SLOTS = 5;
    /** The slot pitch, the first slot's centre, the clearance kept between lanes, and the frame members' width. */
    public static final double PITCH = 3 / 16.0, FIRST = 1 / 8.0, CLEARANCE = 1 / 64.0, MEMBER = 1 / 32.0;

    /** A lane as drawn: its first slot and width, its joints, the joint faces met by a line next door, and its outside size code. */
    public record Lane(int slot, int width, int joints, int joined, int outside) {
        public Lane {
            if (slot < 0 || width < 1 || slot + width > SLOTS || (joints & ~63) != 0 || (joined & ~joints) != 0)
                throw new IllegalArgumentException("Invalid rack lane " + slot + "+" + width);
        }

        public double centre() {
            return RackGeometry.centre(slot, width);
        }

        public int mask() {
            return ((1 << width) - 1) << slot;
        }
    }

    private RackGeometry() {
    }

    /** Slot {@code slot}'s centre coordinate, the same on all three axes. */
    public static double centre(int slot) {
        return FIRST + PITCH * slot;
    }

    /** A lane's centre coordinate: the mean of its slots' centres. */
    public static double centre(int slot, int width) {
        return FIRST + PITCH * (slot + (width - 1) / 2.0);
    }

    /** The slots a line of outside diameter {@code outside} takes: (OD + 1/64) / (3/16), rounded up; five for the largest line. */
    public static int width(double outside) {
        return Math.min(SLOTS, (int) Math.ceil((outside + CLEARANCE) / PITCH - 1e-9));
    }

    /** The largest outside diameter a lane of {@code width} slots carries. */
    public static double largest(int width) {
        return width >= SLOTS ? PipeSizes.MAX_OUTSIDE : width * PITCH - CLEARANCE;
    }

    /**
     * The bend radius of a lane's elbow from face {@code a} to face {@code b}: (m + 1)/2 pitches, and small enough that the
     * bend's centre stays a member and a clearance inside the block. The inside of a bend lies between its centre and the
     * lane, so the bend then clears the frame members along the block's edges; a bend centred on an edge would cut into
     * the member there. Too small for an elbow, the lane turns in a ball ({@link #lane}).
     */
    public static double elbow(Lane lane, int a, int b) {
        return Math.min((lane.width + 1) / 2.0 * PITCH, Math.min(arm(lane, a), arm(lane, b)) - MEMBER - CLEARANCE);
    }

    /** The length of a lane's arm from its centre to face {@code face} (faces in {@link LineGeometry#STEPS} order). */
    public static double arm(Lane lane, int face) {
        return (face & 1) == 1 ? 1 - lane.centre() : lane.centre();
    }

    /**
     * The first slots where a lane of {@code width} fits beside the slots {@code used} (a bit per slot), outer slots
     * first for a wide line, then the lower index (plan 5.3).
     */
    public static List<Integer> free(int used, int width) {
        var out = new ArrayList<Integer>();
        for (int slot = 0; slot + width <= SLOTS; slot++) if ((used & ((1 << width) - 1) << slot) == 0) out.add(slot);
        // A wide line nearer an edge of the diagonal leaves the middle free; equally near, the lower index.
        if (width > 1) out.sort(java.util.Comparator.comparingInt((Integer slot) -> Math.min(slot, SLOTS - width - slot)).thenComparingInt(slot -> slot));
        return out;
    }

    /** The faces a lane runs along: its joints, or straight through along {@code facing} where it has fewer than two. */
    public static int arms(Lane lane, int facing) {
        int joints = lane.joints;
        if (Integer.bitCount(joints) >= 2) return joints;
        int axis = joints == 0 ? facing & ~1 : Integer.numberOfTrailingZeros(joints) & ~1;
        return 1 << axis | 1 << (axis + 1);
    }

    /**
     * Every polygon of a rack cell, cell-local: the frame along the rack's {@code axis} (0 x, 1 y, 2 z) and each lane. A
     * lane is a plain pipe from its centre to its joint faces, welded where the next lane or pipe continues it and capped
     * where nothing does: a flange would be wider than its slot.
     */
    public static List<Polygon> polygons(int axis, List<Lane> lanes, List<Integer> facings) {
        var m = new Mesh();
        frame(m, axis);
        for (int i = 0; i < lanes.size(); i++) lane(m, lanes.get(i), facings.get(i));
        return m.cell(0, 0, 0);
    }

    static void frame(Mesh m, int axis) {
        for (var box : members(axis)) m.box(new double[]{box[0], box[1], box[2]}, new double[]{box[3], box[4], box[5]}, ALL_FACES, Material.STEELWORK);
    }

    /**
     * The thin frame's members, cell-local as {lo x, lo y, lo z, hi x, hi y, hi z}: four along the axis (0 x, 1 y, 2 z) at
     * the block's edges, and a portal of four in the low face across it. They stand along the block's edges only, where
     * no lane runs (plan 5.4).
     */
    public static List<double[]> members(int axis) {
        double w = MEMBER;
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        var out = new ArrayList<double[]>();
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++) out.add(box(axis, 0, 1, a, i == 0 ? 0 : 1 - w, i == 0 ? w : 1, b, j == 0 ? 0 : 1 - w, j == 0 ? w : 1));
        for (int i = 0; i < 2; i++) {
            out.add(box(axis, 0, w, a, i == 0 ? 0 : 1 - w, i == 0 ? w : 1, b, w, 1 - w));
            out.add(box(axis, 0, w, a, w, 1 - w, b, i == 0 ? 0 : 1 - w, i == 0 ? w : 1));
        }
        return out;
    }

    private static double[] box(int axis, double lo, double hi, int a, double loA, double hiA, int b, double loB, double hiB) {
        var box = new double[6];
        box[axis] = lo;
        box[axis + 3] = hi;
        box[a] = loA;
        box[a + 3] = hiA;
        box[b] = loB;
        box[b + 3] = hiB;
        return box;
    }

    /** One lane's polygons, cell-local: the laying tool's ghost of a lane it would lay. */
    public static List<Polygon> lane(Lane lane, int facing) {
        var m = new Mesh();
        lane(m, lane, facing);
        return m.cell(0, 0, 0);
    }

    /** One lane: a straight run, a short elbow, or arms into a ball where an elbow has no room. */
    static void lane(Mesh m, Lane lane, int facing) {
        double od = PipeSizes.outsideOf(lane.outside), r = od / 2, c = lane.centre();
        int sides = PipeSizes.sides(od);
        var centre = new double[]{c, c, c};
        int arms = arms(lane, facing);
        int a = Integer.numberOfTrailingZeros(arms), b = 31 - Integer.numberOfLeadingZeros(arms);
        if ((a ^ 1) == b) {
            arm(m, lane, centre, a, 0, r, sides, LineGeometry.ref(a));
            arm(m, lane, centre, b, 0, r, sides, LineGeometry.ref(b));
            return;
        }
        double bend = elbow(lane, a, b);
        if (bend < 1.05 * r) {
            m.sphere(centre, Math.min(c, r / DeterministicMath.cos(Math.PI / sides)), sides, Material.PIPE);
            arm(m, lane, centre, a, 0, r, sides, LineGeometry.ref(a));
            arm(m, lane, centre, b, 0, r, sides, LineGeometry.ref(b));
            return;
        }
        double[] da = LineGeometry.step(a), db = LineGeometry.step(b);
        var origin = new double[]{c + bend * (da[0] + db[0]), c + bend * (da[1] + db[1]), c + bend * (da[2] + db[2])};
        var f = new Frame(origin, cross(db, da), neg(db));
        int segments = Math.max(3, (int) Math.ceil(Math.PI / 2 * bend / .08));
        m.torus(f, bend, r, 0, Math.PI / 2, segments, sides, Material.PIPE);
        arm(m, lane, centre, a, bend, r, sides, neg(db));
        arm(m, lane, centre, b, bend, r, sides, neg(da));
    }

    /** An arm from {@code from} along face {@code face} to the face, capped there unless the next block continues the line. */
    private static void arm(Mesh m, Lane lane, double[] centre, int face, double from, double r, int sides, double[] ref) {
        var f = new Frame(centre, LineGeometry.step(face), ref);
        double length = arm(lane, face);
        if (length > from + 1e-9) m.prism(f, r, from, length, sides, Material.PIPE);
        if ((lane.joined >> face & 1) == 0) m.annulus(f, 0, r, length, sides, 1, Material.PIPE);
    }

    /**
     * The boxes a lane's arms fill, cell-local, as {lo x, lo y, lo z, hi x, hi y, hi z}: its shape for collision and for
     * picking which lane the player aims at.
     */
    public static List<double[]> boxes(Lane lane, int facing) {
        double r = PipeSizes.outsideOf(lane.outside) / 2, c = lane.centre();
        var out = new ArrayList<double[]>();
        int arms = arms(lane, facing);
        for (int face = 0; face < 6; face++) {
            if ((arms >> face & 1) == 0) continue;
            var step = LineGeometry.STEPS[face];
            var box = new double[]{c - r, c - r, c - r, c + r, c + r, c + r};
            for (int k = 0; k < 3; k++)
                if (step[k] < 0) box[k] = 0;
                else if (step[k] > 0) box[k + 3] = 1;
            out.add(box);
        }
        return out;
    }

    /** The lane a click hits and the arm it hits, by index in a rack's lane list and face; {@link #NONE} when it hits the frame or nothing. */
    public record Pick(int lane, int face) {
    }

    public static final Pick NONE = new Pick(-1, -1);
    /** How far outside an arm box a hit point may lie and still pick it: a hit lands on the box's surface, give or take rounding. */
    public static final double PICK_TOLERANCE = 1e-3;

    /**
     * The lane a click at cell-local point {@code (x, y, z)} hits: the one whose arm box lies nearest the point, within
     * {@link #PICK_TOLERANCE}. A hit lies on the surface of the box it hit, and no lane's box overlaps another's
     * (disjoint slots are a pitch apart on every axis), so the nearest box is the one hit.
     */
    public static Pick pick(List<Lane> lanes, List<Integer> facings, double x, double y, double z) {
        var best = NONE;
        double nearest = PICK_TOLERANCE;
        for (int i = 0; i < lanes.size(); i++) {
            int arms = arms(lanes.get(i), facings.get(i));
            var boxes = boxes(lanes.get(i), facings.get(i));
            int k = 0;
            for (int face = 0; face < 6; face++) {
                if ((arms >> face & 1) == 0) continue;
                var b = boxes.get(k++);
                double outside = Math.max(Math.max(Math.max(b[0] - x, x - b[3]), Math.max(b[1] - y, y - b[4])), Math.max(b[2] - z, z - b[5]));
                if (outside <= nearest) {
                    nearest = outside;
                    best = new Pick(i, face);
                }
            }
        }
        return best;
    }
}
