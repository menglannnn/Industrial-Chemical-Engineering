package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The layered rack cell (batch 2026-10-26-pipe-laying-system, RACK_LAYERS_PLAN option A, support P2 with the lowered
 * support): every pair of lanes that fit one cell together, at every option, arm pair and test size, in every shape,
 * keeps 1/64 m between their surfaces; every lane stays in its block and clear of the steel; lanes of racks side by
 * side meet where their face points coincide, whatever their facings, and keep their order round a corner.
 */
class PipeRackGeometryTest {
    /** Test sizes as outside codes (1/64 m): 2", the largest of each width, and both sides of the lowered support. */
    private static final int[] CODES = {4, 11, 23, 35, 36, 37, 47};
    private static final int NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

    @Test
    void aLineTakesThePositionsItsSizeNeedsAndRestsOnTheBeam() {
        assertEquals(1, RackGeometry.width(.168), "6 inch");
        assertEquals(2, RackGeometry.width(.356), "14 inch");
        assertEquals(3, RackGeometry.width(.508), "20 inch");
        assertEquals(4, RackGeometry.width(.711), "28 inch");
        assertTrue(RackGeometry.width(PipeSizes.MAX_OUTSIDE) > RackGeometry.MAX_WIDTH, "32 inch is not laid on racks (RD10)");
        for (int width = 1; width <= RackGeometry.MAX_WIDTH; width++) assertEquals(width, RackGeometry.width(RackGeometry.largest(width)));
        assertEquals(14, RackGeometry.height(.0625), "2 inch: 14/32");
        assertEquals(16, RackGeometry.height(.168), "6 inch: 16/32");
        assertEquals(19, RackGeometry.height(.356), "14 inch: 19/32");
        assertEquals(22, RackGeometry.height(.508), "20 inch: 22/32");
        for (int code = 1; code <= 47; code++) {
            double od = PipeSizes.outsideOf(code), bottom = RackGeometry.height(od) / 32.0 - od / 2;
            if (RackGeometry.height(od) == 16 && code > 36) {
                // Too large to rest on the beam within the block: at mid-height on a lowered beam, as support P1.
                assertTrue(RackGeometry.top(od) <= 1, code + " fits its block");
                assertTrue(RackGeometry.beam(List.of(lane(RackGeometry.option(0, RackGeometry.TIER), 4, 1 << NORTH | 1 << SOUTH, code))) * 2 / 64.0 <= bottom + 1e-9, code + " on its beam");
            } else {
                assertTrue(bottom >= 13 / 32.0 - 1e-9 && bottom < 14 / 32.0, code + " rests on the beam: bottom " + bottom);
                assertTrue(RackGeometry.top(od) + RackGeometry.CLEARANCE <= 1 + 1e-9, code + " clears the block's top");
            }
        }
    }

    @Test
    void crossingLevelsClearTheLinesTheyPass() {
        assertTrue(RackGeometry.clears(.273, .168), "a 6 inch crossing passes over a 10 inch line");
        assertFalse(RackGeometry.clears(.3238, .168), "but not over a 12 inch one");
        for (int code = 1; code <= 36; code++) assertTrue(RackGeometry.clearsUnder(PipeSizes.outsideOf(code), RackGeometry.largest(1)), code + " rests over the lower level");
        assertFalse(RackGeometry.clearsUnder(PipeSizes.outsideOf(47), .0625), "a line on a lowered beam closes the lower level");
        var rack = new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT);
        var wide = List.of(lane(RackGeometry.option(0, RackGeometry.TIER), 4, 1 << NORTH | 1 << SOUTH, 47));
        assertFalse(RackGeometry.open(rack, wide, RackGeometry.crossing(RackGeometry.ABOVE), 1, .0625));
        assertFalse(RackGeometry.open(rack, wide, RackGeometry.crossing(RackGeometry.BELOW), 1, .0625));
        var small = List.of(lane(RackGeometry.option(1, RackGeometry.TIER), 1, 1 << NORTH | 1 << SOUTH, 11));
        assertTrue(RackGeometry.open(rack, small, RackGeometry.crossing(RackGeometry.ABOVE), 1, .168));
        assertTrue(RackGeometry.open(rack, small, RackGeometry.crossing(RackGeometry.BELOW), 1, .168));
    }

    @Test
    void aFlatExitClosesThePositionsBetweenItAndItsSide() {
        var rack = new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT);
        // Facing north, the right is east: a lane at position 1 entering from the back (south) leaving flat to the east.
        var flat = lane(RackGeometry.option(1, RackGeometry.TIER), 1, 1 << SOUTH | 1 << EAST, 11);
        assertEquals(0b1110, RackGeometry.taken(rack, List.of(flat)));
        assertTrue(RackGeometry.fits(rack, List.of(flat), lane(RackGeometry.option(0, RackGeometry.TIER), 1, 1 << NORTH | 1 << SOUTH, 11)));
        assertFalse(RackGeometry.fits(rack, List.of(flat), lane(RackGeometry.option(3, RackGeometry.TIER), 1, 1 << NORTH | 1 << SOUTH, 11)));
        assertFalse(RackGeometry.fits(rack, List.of(lane(RackGeometry.option(3, RackGeometry.TIER), 1, 1 << NORTH | 1 << SOUTH, 11)), flat), "nor passes over one");
        assertTrue(RackGeometry.fits(rack, List.of(lane(RackGeometry.option(3, RackGeometry.TIER), 1, 1 << NORTH | 1 << SOUTH, 11)),
                lane(RackGeometry.option(1, RackGeometry.ABOVE), 1, 1 << SOUTH | 1 << EAST, 11)), "over it instead");
        assertFalse(RackGeometry.fits(rack, List.of(), lane(RackGeometry.option(0, RackGeometry.TIER), 4, 1 << SOUTH | 1 << EAST, 47)), "a line on a lowered beam cannot leave flat over the stringer");
    }

    @Test
    void everyPairOfLanesInACellKeepsItsClearance() {
        double worst = Double.MAX_VALUE;
        for (int corner = RackGeometry.STRAIGHT; corner <= RackGeometry.FRONT_RIGHT; corner++) {
            var rack = new RackGeometry.Rack(NORTH, corner);
            var lanes = lanes(rack);
            var shapes = new ArrayList<List<double[][]>>();
            for (var lane : lanes) shapes.add(segments(rack, lane));
            for (int i = 0; i < lanes.size(); i++)
                for (int j = i + 1; j < lanes.size(); j++) {
                    var a = lanes.get(i);
                    var b = lanes.get(j);
                    if (!RackGeometry.fits(rack, List.of(a), b)) continue;
                    double gap = distance(shapes.get(i), shapes.get(j)) - a.diameter() / 2 - b.diameter() / 2;
                    worst = Math.min(worst, gap);
                    // Each chord spans 1/32 of a quarter turn: it lies at most 1 mm inside the bend it stands for.
                    assertTrue(gap >= RackGeometry.CLEARANCE - 1e-3, "corner " + corner + ": " + a + " and " + b + " gap " + gap);
                }
        }
        assertEquals(RackGeometry.CLEARANCE, worst, 1e-3, "the narrowest gap is the clearance the positions are sized for");
    }

    @Test
    void everyLaneStaysInItsBlockAndClearOfTheSteel() {
        for (int facing : new int[]{NORTH, SOUTH, WEST, EAST})
            for (int corner = RackGeometry.STRAIGHT; corner <= RackGeometry.FRONT_RIGHT; corner++) {
                var geometry = new RackGeometry.Rack(facing, corner);
                for (var lane : lanes(geometry)) {
                    var rack = new RackGeometry.Rack(facing, corner, 3, true, 0, RackGeometry.beam(List.of(lane)));
                    double r = lane.diameter() / 2;
                    // A resting line touches its beam; a chord lies at most 1 mm inside the bend it stands for.
                    for (var segment : segments(rack, lane))
                        for (var box : RackGeometry.members(rack))
                            assertTrue(boxDistance(segment, box) >= r - 1e-3, "facing " + facing + " corner " + corner + ": " + lane + " runs into the steel " + java.util.Arrays.toString(box));
                    for (var polygon : RackGeometry.lane(rack, lane)) {
                        var p = polygon.positions();
                        for (int v = 0; v < p.length; v++) assertTrue(p[v] >= -1e-9 && p[v] <= 1 + 1e-9, lane + " leaves its block");
                    }
                }
            }
    }

    @Test
    void lanesMeetWhereTheirPointsCoincideWhateverTheFacing() {
        // Two racks in line north-south, facing the same way and opposite ways: each tier lane meets exactly one lane of
        // the next rack, the same positions seen from the world.
        for (int other : new int[]{NORTH, SOUTH}) {
            var near = new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT);
            var far = new RackGeometry.Rack(other, RackGeometry.STRAIGHT);
            for (int code : CODES) {
                int width = RackGeometry.width(PipeSizes.outsideOf(code));
                for (int p = 0; p + width <= RackGeometry.POSITIONS; p++) {
                    int point = RackGeometry.point(near, RackGeometry.option(p, RackGeometry.TIER), width, PipeSizes.outsideOf(code), NORTH);
                    int meets = 0, at = -1;
                    for (int q = 0; q + width <= RackGeometry.POSITIONS; q++)
                        if (RackGeometry.point(far, RackGeometry.option(q, RackGeometry.TIER), width, PipeSizes.outsideOf(code), SOUTH) == point) {
                            meets++;
                            at = q;
                        }
                    assertEquals(1, meets, "position " + p + " of " + width);
                    assertEquals(other == NORTH ? p : RackGeometry.POSITIONS - width - p, at, "the numbering is a label only");
                }
            }
        }
    }

    @Test
    void cornerLanesKeepTheirOrderAndMeetTheNextRun() {
        // A run facing north turning east: the corner faces north and turns from its back to its right, into a run facing east.
        var corner = new RackGeometry.Rack(NORTH, RackGeometry.BACK_RIGHT);
        var next = new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT);
        double od = PipeSizes.outsideOf(11);
        for (int p = 0; p < RackGeometry.POSITIONS; p++) {
            int option = RackGeometry.option(p, RackGeometry.TIER);
            assertTrue(RackGeometry.valid(corner, option, 1, 1 << SOUTH | 1 << EAST));
            assertFalse(RackGeometry.valid(corner, option, 1, 1 << SOUTH | 1 << NORTH), "a corner carries corner lanes only");
            int out = RackGeometry.point(corner, option, 1, od, EAST);
            assertEquals(out, RackGeometry.point(next, option, 1, od, WEST), "position " + p + " stays position " + p + " round the corner");
        }
        assertFalse(RackGeometry.valid(corner, RackGeometry.crossing(RackGeometry.ABOVE), 1, 1 << WEST | 1 << EAST), "no crossing in a corner");
        var members = RackGeometry.members(new RackGeometry.Rack(NORTH, RackGeometry.BACK_RIGHT, 0, false, 0, RackGeometry.BEAM_TOP));
        for (var box : members) assertFalse(box[0] >= 28 / 32.0 && box[2] >= 28 / 32.0 && box[1] == 0, "no post at the inner (south-east) corner");
        assertEquals(5, members.size(), "three posts and two beams");
    }

    @Test
    void anOpenLaneEndIsCappedAndAJoinedOneIsNot() {
        var rack = new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT);
        int arms = 1 << NORTH | 1 << SOUTH;
        var open = RackGeometry.lane(rack, new RackGeometry.Lane(RackGeometry.option(1, RackGeometry.TIER), 1, arms, 0, PipeSizes.code(.1)));
        var joined = RackGeometry.lane(rack, new RackGeometry.Lane(RackGeometry.option(1, RackGeometry.TIER), 1, arms, arms, PipeSizes.code(.1)));
        assertTrue(open.size() > joined.size(), "the open lane has two caps");
        var exit = new RackGeometry.Lane(RackGeometry.option(1, RackGeometry.ABOVE), 1, 1 << SOUTH | 1 << EAST, 0, PipeSizes.code(.1));
        assertEquals(3, RackGeometry.boxes(rack, exit).size(), "a lane over the tier: along, up and across");
    }

    @Test
    void theBentsStandWhereTheFrameSays() {
        var bare = RackGeometry.members(new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT, 0, false, 0, RackGeometry.BEAM_TOP));
        assertEquals(2, bare.size(), "between bents: the stringers alone");
        var bent = RackGeometry.members(new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT, 1, false, 0, RackGeometry.BEAM_TOP));
        assertEquals(5, bent.size(), "a bent: two posts and a beam");
        for (var box : bent) assertTrue(box[4] <= 13 / 32.0 + 1e-9, "nothing stands over the tier");
        var stacked = RackGeometry.members(new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT, 1, true, 0, RackGeometry.BEAM_TOP));
        assertEquals(2, stacked.stream().filter(box -> box[4] == 1).count(), "under a rack the posts run on up");
        var joined = RackGeometry.members(new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT, 1, false, 3, RackGeometry.BEAM_TOP));
        assertEquals(1, joined.size(), "joined on both sides: the beam alone, running through");
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------

    private static RackGeometry.Lane lane(int option, int width, int arms, int code) {
        return new RackGeometry.Lane(option, width, arms, 0, code);
    }

    /** Every lane the cell takes alone, at every option and arm pair, for each test size. */
    private static List<RackGeometry.Lane> lanes(RackGeometry.Rack rack) {
        var out = new ArrayList<RackGeometry.Lane>();
        for (int code : CODES) {
            int width = RackGeometry.width(PipeSizes.outsideOf(code));
            for (int option = 0; option < RackGeometry.OPTIONS; option++) {
                int w = RackGeometry.tier(option) ? width : 1;
                if (!RackGeometry.tier(option) && width != 1) continue;
                for (int a = 0; a < 6; a++)
                    for (int b = a + 1; b < 6; b++) {
                        int arms = 1 << a | 1 << b;
                        if (!RackGeometry.valid(rack, option, w, arms)) continue;
                        var lane = new RackGeometry.Lane(option, w, arms, 0, code);
                        if (RackGeometry.fits(rack, List.of(), lane)) out.add(lane);
                    }
            }
        }
        return out;
    }

    /** A lane's centre line as segments: its straights, and its bends in chords of 1/32 of a quarter turn each. */
    private static List<double[][]> segments(RackGeometry.Rack rack, RackGeometry.Lane lane) {
        var path = RackGeometry.path(rack, lane);
        var out = new ArrayList<double[][]>();
        double od = lane.diameter();
        if (path.size() == 2) {
            out.add(new double[][]{path.get(0), path.get(1)});
            return out;
        }
        if (RackGeometry.tier(lane.option()) && rack.cornered()) {
            double[] da = LineGeometry.step(RackGeometry.first(rack, lane)), db = LineGeometry.step(RackGeometry.second(rack, lane)), corner = path.get(1);
            double bend = Math.abs(Mesh.dot(Mesh.sub(path.get(0), corner), da));
            var origin = Mesh.plus(corner, Mesh.scale(Mesh.plus(da, db), bend));
            arc(out, origin, Mesh.sub(path.get(0), origin), Mesh.sub(path.get(2), origin), Math.PI / 2);
            return out;
        }
        var end = new LineGeometry.End(od, false, 0, 0, true);
        var fitted = LineGeometry.fitted(path, end, end, od, RackGeometry.BEND);
        var points = fitted.points();
        int legs = fitted.lengths().length;
        double bend = fitted.bend() < 1.05 * od / 2 ? 0 : fitted.bend();
        double[] from = points.getFirst();
        for (int k = 0; k < legs; k++) {
            var dir = fitted.dirs()[k];
            var to = k == legs - 1 ? points.getLast() : Mesh.sub(points.get(k + 1), Mesh.scale(dir, bend * fitted.tans()[k + 1]));
            out.add(new double[][]{from, to});
            if (k == legs - 1) break;
            var next = fitted.dirs()[k + 1];
            if (bend == 0) {
                from = to;
                continue;
            }
            var inward = Mesh.unit(Mesh.sub(next, Mesh.scale(dir, Mesh.dot(next, dir))));
            var centre = Mesh.plus(to, Mesh.scale(inward, bend));
            var c = Mesh.cross(dir, next);
            double turn = Math.atan2(Math.sqrt(Mesh.dot(c, c)), Mesh.dot(dir, next));
            arc(out, centre, Mesh.sub(to, centre), Mesh.scale(dir, bend), turn);
            from = Mesh.plus(points.get(k + 1), Mesh.scale(next, bend * fitted.tans()[k + 1]));
        }
        return out;
    }

    /** Chords of the arc about {@code centre} from {@code u} (a radius vector) towards {@code v} (square to it, of the same length), through {@code turn}. */
    private static void arc(List<double[][]> out, double[] centre, double[] u, double[] v, double turn) {
        int chords = Math.max(4, (int) Math.ceil(turn / (Math.PI / 64)));
        double[] last = Mesh.plus(centre, u);
        for (int k = 1; k <= chords; k++) {
            double t = turn * k / chords;
            var p = Mesh.plus(centre, Mesh.plus(Mesh.scale(u, Math.cos(t)), Mesh.scale(v, Math.sin(t))));
            out.add(new double[][]{last, p});
            last = p;
        }
    }

    private static double distance(List<double[][]> a, List<double[][]> b) {
        double best = Double.MAX_VALUE;
        for (var s : a) for (var t : b) best = Math.min(best, segmentDistance(s[0], s[1], t[0], t[1]));
        return best;
    }

    /** The distance between segments pq and rs (closest points, clamped). */
    static double segmentDistance(double[] p, double[] q, double[] r, double[] s) {
        double[] d1 = Mesh.sub(q, p), d2 = Mesh.sub(s, r), w = Mesh.sub(p, r);
        double a = Mesh.dot(d1, d1), e = Mesh.dot(d2, d2), f = Mesh.dot(d2, w);
        double sc, tc;
        if (a < 1e-15 && e < 1e-15) return Math.sqrt(Mesh.dot(w, w));
        if (a < 1e-15) {
            sc = 0;
            tc = clamp(f / e);
        } else {
            double c = Mesh.dot(d1, w);
            if (e < 1e-15) {
                tc = 0;
                sc = clamp(-c / a);
            } else {
                double b = Mesh.dot(d1, d2), denom = a * e - b * b;
                sc = denom > 1e-15 ? clamp((b * f - c * e) / denom) : 0;
                tc = (b * sc + f) / e;
                if (tc < 0) {
                    tc = 0;
                    sc = clamp(-c / a);
                } else if (tc > 1) {
                    tc = 1;
                    sc = clamp((b - c) / a);
                }
            }
        }
        var between = Mesh.sub(Mesh.plus(p, Mesh.scale(d1, sc)), Mesh.plus(r, Mesh.scale(d2, tc)));
        return Math.sqrt(Mesh.dot(between, between));
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    /** A segment's least distance to a box, sampled finely (the boxes are thin, the segments short). */
    private static double boxDistance(double[][] segment, double[] box) {
        double best = Double.MAX_VALUE;
        for (int k = 0; k <= 256; k++) {
            var p = Mesh.plus(segment[0], Mesh.scale(Mesh.sub(segment[1], segment[0]), k / 256.0));
            double dx = Math.max(0, Math.max(box[0] - p[0], p[0] - box[3])), dy = Math.max(0, Math.max(box[1] - p[1], p[1] - box[4])), dz = Math.max(0, Math.max(box[2] - p[2], p[2] - box[5]));
            best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return best;
    }
}
