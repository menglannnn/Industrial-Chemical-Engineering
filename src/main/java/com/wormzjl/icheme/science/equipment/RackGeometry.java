package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.icheme.science.equipment.Mesh.*;

/**
 * A layered pipe rack cell (batch 2026-10-26-pipe-laying-system, RACK_LAYERS_PLAN option A with support P2): one tier of
 * four positions across the run, the lanes laid on it, a crossing level above it and one below, and the steel that
 * carries them.
 *
 * <p>Canonical cell coordinates: u across the run from the left side (as seen along the facing), y up, w along the run
 * from the back face; a {@link Rack} maps them to block coordinates. Every tier line rests on the beam, whose top lies at
 * {@link #BEAM_TOP}: a line's centre stands its radius above it, rounded up to the 1/32 m grid ({@link #height}); a line
 * too large to rest there within the block (a four-position line) sits at mid-height on a beam lowered beneath it, as
 * support P1 has it (owner, 2026-10-26). So every
 * point where a lane meets a face lies on the transition pipes' grid ({@link LineGeometry#OFFSET}). Lines turn and leave
 * at mid-block, between the bents, which stand in the block's face planes.</p>
 *
 * <p>A lane's option says where it lies: a tier lane by its first position p and the level l its side arm leaves at
 * (option p + 4 l), a crossing lane by its level (option 12 above the tier, 13 below). Lanes meet across a face where
 * their points on it coincide, so the numbering is only a label.</p>
 */
public final class RackGeometry {
    public static final int POSITIONS = 4, MAX_WIDTH = 4;
    /** The pitch of the positions, the first position's centre, and the clearance kept between lines. */
    public static final double PITCH = 3 / 16.0, FIRST = 7 / 32.0, CLEARANCE = 1 / 64.0;
    /** Heights in 1/32 m steps: the beam's top and bottom, the stringers' bottom, the crossing levels above and below. */
    public static final int BEAM_TOP = 13, BEAM_BOTTOM = 11, STRINGER_BOTTOM = 12, UPPER = 26, LOWER = 6;
    /** How a side arm leaves: at its tier (flat), over the tier at the upper level, or under it at the lower. */
    public static final int TIER = 0, ABOVE = 1, BELOW = 2;
    public static final int OPTIONS = 14;
    /** Corners: none, or the end face (back, front) and the side (left, right) a corner's lanes turn between. */
    public static final int STRAIGHT = 0, BACK_LEFT = 1, BACK_RIGHT = 2, FRONT_LEFT = 3, FRONT_RIGHT = 4;
    /** The canonical faces a block face is to a rack cell. */
    static final int FRONT = 0, BACK = 1, LEFT = 2, RIGHT = 3, TOP = 4, BOTTOM = 5;
    private static final double S = LineGeometry.OFFSET, EPS = 1e-9;
    /** A lane's bend radius at most: its bends start clear of the bents' planes, 2/32 m deep at each run face, so a riser never sweeps a beam. */
    static final double BEND = .5 - 2 * S;

    /**
     * A rack cell's frame: its facing (a horizontal face in {@link LineGeometry#STEPS} order), its corner, and its drawn
     * steel: a bit per bent (1 in the back face's plane, 2 in the front's), whether a rack stands on it (its posts run on
     * up), a bit per side a rack beside it joins (1 left, 2 right: no posts or stringer there, the beams run through), and
     * its beams' top in 1/32 m steps: {@link #BEAM_TOP}, or lower beneath a line too large to rest there ({@link #beam}).
     */
    public record Rack(int facing, int corner, int bents, boolean stacked, int sides, int beam) {
        public Rack {
            if (facing < 2 || facing > 5 || corner < STRAIGHT || corner > FRONT_RIGHT || (bents & ~3) != 0 || (sides & ~3) != 0 || beam < 2 || beam > BEAM_TOP)
                throw new IllegalArgumentException("Invalid rack frame");
        }

        /** A cell's lane geometry alone: its facing and corner. */
        public Rack(int facing, int corner) {
            this(facing, corner, 0, false, 0, BEAM_TOP);
        }

        public int front() {
            return facing;
        }

        public int back() {
            return facing ^ 1;
        }

        public int left() {
            return ccw(facing);
        }

        public int right() {
            return cw(facing);
        }

        public boolean cornered() {
            return corner != STRAIGHT;
        }

        /** A corner's end face, where its lanes run in from the run. */
        public int end() {
            return corner <= BACK_RIGHT ? back() : front();
        }

        /** A corner's side face, where its lanes turn out to the next run. */
        public int side() {
            return corner == BACK_LEFT || corner == FRONT_LEFT ? left() : right();
        }

        /** Canonical face {@link #FRONT} to {@link #BOTTOM} of block face {@code face}. */
        int canonical(int face) {
            if (face == 1) return TOP;
            if (face == 0) return BOTTOM;
            return face == front() ? FRONT : face == back() ? BACK : face == left() ? LEFT : RIGHT;
        }

        /** The block coordinates of canonical point (u, y, w). */
        public double[] world(double u, double y, double w) {
            return switch (facing) {
                case 2 -> new double[]{u, y, 1 - w};
                case 3 -> new double[]{1 - u, y, w};
                case 4 -> new double[]{1 - w, y, 1 - u};
                default -> new double[]{w, y, u};
            };
        }
    }

    /**
     * A lane as drawn: its option, width in positions (1 for a crossing), its arms (the two faces its pipe runs to, a bit
     * per face), those a line next door meets (welded there, capped elsewhere), and its outside size code.
     */
    public record Lane(int option, int width, int arms, int joined, int outside) {
        public Lane {
            if (!takes(option, width) || Integer.bitCount(arms) != 2 || (arms & ~63) != 0 || (joined & ~arms) != 0)
                throw new IllegalArgumentException("Invalid rack lane " + option + "+" + width);
        }

        public double diameter() {
            return PipeSizes.outsideOf(outside);
        }
    }

    private RackGeometry() {
    }

    // ---- options and sizes -----------------------------------------------------------------------------------------

    public static int option(int position, int level) {
        return position + 4 * level;
    }

    public static int crossing(int level) {
        return 11 + level;
    }

    public static boolean tier(int option) {
        return option < 12;
    }

    /** A tier option's first position. */
    public static int position(int option) {
        return option % 4;
    }

    /** An option's level: a tier lane's side arm's, or a crossing's. */
    public static int level(int option) {
        return option < 12 ? option / 4 : option - 11;
    }

    /** The positions a line of outside diameter {@code outside} takes: (OD + 1/64) / (3/16), rounded up; over {@link #MAX_WIDTH} it is not laid on racks. */
    public static int width(double outside) {
        return (int) Math.ceil((outside + CLEARANCE) / PITCH - EPS);
    }

    /** The largest outside diameter a lane of {@code width} positions carries. */
    public static double largest(int width) {
        return width * PITCH - CLEARANCE;
    }

    /** A lane's centre across the run: the mean of its positions' centres. */
    public static double centre(int position, int width) {
        return FIRST + PITCH * (position + (width - 1) / 2.0);
    }

    /**
     * A tier line's centre height in 1/32 m steps: resting on the beam, its radius over the beam's top rounded up to the
     * grid (support P2); a line whose top would then come closer than the clearance to the block's top sits at mid-height
     * instead, on a beam lowered just beneath it (as support P1; owner, 2026-10-26).
     */
    public static int height(double outside) {
        int resting = (int) Math.ceil(BEAM_TOP + outside / 2 / S - EPS);
        return resting * S + outside / 2 + CLEARANCE <= 1 + EPS ? resting : 16;
    }

    /** The beams' top in a cell with {@code lanes}: {@link #BEAM_TOP}, or just beneath the lowest tier line where one sits lower. */
    public static int beam(List<Lane> lanes) {
        int top = BEAM_TOP;
        for (var lane : lanes) if (tier(lane.option)) top = Math.min(top, (int) Math.floor(bottom(lane.diameter()) / S + EPS));
        return Math.max(2, top);
    }

    /** The top of a tier line. */
    static double top(double outside) {
        return height(outside) * S + outside / 2;
    }

    /** The bottom of a tier line. */
    static double bottom(double outside) {
        return height(outside) * S - outside / 2;
    }

    /** Whether a tier line of outside {@code below} passes under a line of outside {@code above} at the upper level, with the clearance. */
    public static boolean clears(double below, double above) {
        return top(below) + CLEARANCE <= UPPER * S - above / 2 + EPS;
    }

    /** Whether a line of outside {@code below} at the lower level passes under a tier line of outside {@code above}, with the clearance. */
    public static boolean clearsUnder(double above, double below) {
        return bottom(above) + EPS >= LOWER * S + below / 2 + CLEARANCE;
    }

    static int cw(int face) {
        return switch (face) {
            case 2 -> 5;
            case 5 -> 3;
            case 3 -> 4;
            case 4 -> 2;
            default -> throw new IllegalArgumentException("Not a horizontal face " + face);
        };
    }

    static int ccw(int face) {
        return cw(cw(cw(face)));
    }

    // ---- groups ----------------------------------------------------------------------------------------------------

    /** A rack cell's neighbours, as its shape and its drawn steel see them. */
    public interface Neighbours {
        /** The rack beyond face {@code face} (its facing and corner), or null where none stands. */
        Rack rack(int face);

        /** Whether what stands beyond {@code face} is known: false in an unloaded chunk, where the steel keeps what it drew. */
        default boolean known(int face) {
            return true;
        }
    }

    /**
     * The corner a rack facing {@code facing} takes from its neighbours: towards the first side (right, then left) where
     * a rack runs across, from the end where its run continues (the back where neither or only the back does); straight
     * where no rack runs across beside it, or where its run continues at both ends. A corner whose turn comes in through
     * an end continues the run there, so a turn several racks wide nests: each run turns at its own corner, on the
     * diagonal between the two runs, and the outer runs pass the inner corners straight.
     */
    public static int corner(Neighbours around, int facing) {
        boolean front = inLine(around.rack(facing), facing, facing), back = inLine(around.rack(facing ^ 1), facing, facing ^ 1);
        if (front && back) return STRAIGHT;
        for (int side : new int[]{cw(facing), ccw(facing)}) {
            var neighbour = around.rack(side);
            if (neighbour == null || LineGeometry.axis(neighbour.facing) != LineGeometry.axis(side)) continue;
            boolean right = side == cw(facing);
            return front && !back ? right ? FRONT_RIGHT : FRONT_LEFT : right ? BACK_RIGHT : BACK_LEFT;
        }
        return STRAIGHT;
    }

    /** Whether the rack beyond {@code face} continues the run of a rack facing {@code facing}: it runs the same way, or it turns into this one. */
    private static boolean inLine(Rack neighbour, int facing, int face) {
        return neighbour != null && (LineGeometry.axis(neighbour.facing) == LineGeometry.axis(facing) || neighbour.cornered() && neighbour.side() == (face ^ 1));
    }

    /**
     * The drawn steel of {@code rack} at {@code along} (its coordinate along its run), as {@link Rack}'s bents, stacked
     * and sides packed (bents bits 0 and 1, stacked bit 2, sides bits 3 and 4); a bit whose neighbour is not known keeps
     * its value in {@code previous}. A straight cell stands a bent in its low face's plane every third block along its
     * run and in the plane of each end without a rack beyond; its posts run on up under a rack; a side with a straight
     * rack of its run's axis beside it joins it.
     */
    public static int steel(Neighbours around, Rack rack, int along, int previous) {
        int out = bit(around, 1, 4, previous, r -> r != null);
        if (rack.cornered()) return out;
        boolean up = rack.facing == 3 || rack.facing == 5;
        if (Math.floorMod(along, 3) == 0) out |= up ? 1 : 2;
        out |= bit(around, rack.back(), 1, previous, r -> r == null);
        out |= bit(around, rack.front(), 2, previous, r -> r == null);
        java.util.function.Predicate<Rack> beside = r -> r != null && !r.cornered() && LineGeometry.axis(r.facing) == LineGeometry.axis(rack.facing);
        out |= bit(around, rack.left(), 8, previous, beside);
        out |= bit(around, rack.right(), 16, previous, beside);
        return out;
    }

    private static int bit(Neighbours around, int face, int mask, int previous, java.util.function.Predicate<Rack> test) {
        if (!around.known(face)) return previous & mask;
        return test.test(around.rack(face)) ? mask : 0;
    }

    /** A frame from its lane geometry and the packed steel of {@link #steel}, its beams at {@code beam}. */
    public static Rack framed(Rack rack, int steel, int beam) {
        return new Rack(rack.facing, rack.corner, steel & 3, (steel >> 2 & 1) != 0, steel >> 3 & 3, beam);
    }

    // ---- lanes -----------------------------------------------------------------------------------------------------

    /**
     * Whether a lane of {@code option}, {@code width} and {@code arms} suits a cell: a crossing runs side to side across a
     * straight cell; a corner's lane runs from its end face to its side; any other tier lane has an end arm and runs on
     * to the other end, up, down, or out of a side, where only a one-position line leaves above or below the tier.
     */
    public static boolean valid(Rack rack, int option, int width, int arms) {
        if (!takes(option, width) || Integer.bitCount(arms) != 2 || (arms & ~63) != 0) return false;
        int a = rack.canonical(Integer.numberOfTrailingZeros(arms)), b = rack.canonical(31 - Integer.numberOfLeadingZeros(arms));
        int level = level(option);
        if (!tier(option)) return !rack.cornered() && side(a) && side(b);
        if (rack.cornered()) return level == TIER && arms == (1 << rack.end() | 1 << rack.side());
        boolean endA = a == FRONT || a == BACK, endB = b == FRONT || b == BACK;
        if (!endA && !endB) return false;
        int other = endA ? b : a;
        return side(other) || level == TIER;
    }

    /**
     * Whether {@code option} takes a line {@code width} positions wide, whatever the cell: a tier lane within the tier,
     * leaving above or below it only one position wide; a crossing one position wide.
     */
    public static boolean takes(int option, int width) {
        if (option < 0 || option >= OPTIONS || width < 1 || width > MAX_WIDTH) return false;
        if (!tier(option)) return width == 1;
        return position(option) + width <= POSITIONS && (level(option) == TIER || width == 1);
    }

    private static boolean side(int canonical) {
        return canonical == LEFT || canonical == RIGHT;
    }

    /** A lane's first arm: its end arm for a tier lane (the back of a straight one), the left one for a crossing. */
    static int first(Rack rack, Lane lane) {
        int a = Integer.numberOfTrailingZeros(lane.arms), b = 31 - Integer.numberOfLeadingZeros(lane.arms);
        int ca = rack.canonical(a), cb = rack.canonical(b);
        if (!tier(lane.option)) return ca == LEFT ? a : b;
        if (ca == BACK || cb == BACK) return ca == BACK ? a : b;
        return ca == FRONT ? a : b;
    }

    static int second(Rack rack, Lane lane) {
        return Integer.numberOfTrailingZeros(lane.arms & ~(1 << first(rack, lane)));
    }

    /** The tier positions a lane takes, a bit each: its own, none for a crossing. */
    static int own(Lane lane) {
        return tier(lane.option) ? ((1 << lane.width) - 1) << position(lane.option) : 0;
    }

    /** Whether a lane leaves through a side face at its tier's height. */
    private static boolean flat(Rack rack, Lane lane) {
        if (!tier(lane.option) || rack.cornered() || level(lane.option) != TIER) return false;
        for (int face = 2; face < 6; face++) if ((lane.arms >> face & 1) != 0 && side(rack.canonical(face))) return true;
        return false;
    }

    /** The tier positions a lane's flat side arm passes over: those between the lane and that side. */
    static int shadow(Rack rack, Lane lane) {
        if (!tier(lane.option) || rack.cornered() || level(lane.option) != TIER) return 0;
        int out = 0, p = position(lane.option);
        for (int face = 0; face < 6; face++) {
            if ((lane.arms >> face & 1) == 0) continue;
            int c = rack.canonical(face);
            if (c == LEFT) out |= (1 << p) - 1;
            else if (c == RIGHT) out |= ((1 << POSITIONS) - 1) & ~((1 << (p + lane.width)) - 1);
        }
        return out;
    }

    /** The tier positions a cell's lanes take or pass over flat, a bit each: no new lane may take them. */
    public static int taken(Rack rack, List<Lane> lanes) {
        int out = 0;
        for (var lane : lanes) out |= own(lane) | shadow(rack, lane);
        return out;
    }

    /** The levels a lane uses, a bit each (1 upper, 2 lower): a riser through the top or bottom face, a side arm over or under the tier, a crossing. */
    static int levels(Rack rack, Lane lane) {
        int level = level(lane.option);
        if (!tier(lane.option) || level != TIER) return level == ABOVE ? 1 : 2;
        int out = 0;
        if ((lane.arms >> 1 & 1) != 0) out |= 1;
        if ((lane.arms & 1) != 0) out |= 2;
        return out;
    }

    /** Whether a lane takes the upper level over the tier, where every tier line of its cell must pass under it. */
    private static boolean over(Lane lane) {
        return level(lane.option) == ABOVE;
    }

    /** Whether a lane takes the lower level under the tier, where every tier line of its cell must pass over it. */
    private static boolean under(Lane lane) {
        return level(lane.option) == BELOW;
    }

    /** Whether tier lane {@code tier} clears a lane at a crossing level beside it in one cell (true for any other pair). */
    private static boolean apart(Lane tier, Lane crossing) {
        if (!tier(tier.option) || tier == crossing) return true;
        return (!over(crossing) || clears(tier.diameter(), crossing.diameter())) && (!under(crossing) || clearsUnder(tier.diameter(), crossing.diameter()));
    }

    /**
     * Whether {@code lane} fits a cell beside {@code lanes}: it suits the cell; it takes no position another lane takes or
     * passes over flat, nor passes over one flat; it shares no level; and every tier line of the cell clears any line at
     * a crossing level by the clearance: at the upper level lines up to about 10" pass under, at the lower every line
     * resting on the beam passes over (a line on a lowered beam closes it).
     */
    public static boolean fits(Rack rack, List<Lane> lanes, Lane lane) {
        if (!valid(rack, lane.option, lane.width, lane.arms)) return false;
        int own = own(lane), shadow = shadow(rack, lane), levels = levels(rack, lane);
        // A flat side arm passes over the stringer at the side: only a line resting on the beam clears it.
        if (flat(rack, lane) && bottom(lane.diameter()) < BEAM_TOP * S - EPS) return false;
        for (var other : lanes) {
            if ((own & (own(other) | shadow(rack, other))) != 0 || (shadow & own(other)) != 0 || (levels & levels(rack, other)) != 0) return false;
            if (!apart(other, lane) || !apart(lane, other)) return false;
        }
        return true;
    }

    /**
     * Whether a line of {@code outside} may take {@code option} beside {@code lanes} before its arms are known: its
     * positions free (a tier lane), or its level free and clear over the tier (a crossing).
     */
    public static boolean open(Rack rack, List<Lane> lanes, int option, int width, double outside) {
        if (!takes(option, width)) return false;
        if (tier(option)) {
            int own = ((1 << width) - 1) << position(option);
            for (var other : lanes) {
                if ((own & (own(other) | shadow(rack, other))) != 0) return false;
                if (over(other) && !clears(outside, other.diameter()) || under(other) && !clearsUnder(outside, other.diameter())) return false;
            }
            return true;
        }
        if (rack.cornered()) return false;
        int level = level(option) == ABOVE ? 1 : 2;
        for (var other : lanes) {
            if ((levels(rack, other) & level) != 0) return false;
            if (tier(other.option) && (level == 1 ? !clears(other.diameter(), outside) : !clearsUnder(other.diameter(), outside))) return false;
        }
        return true;
    }

    /** Whether a line in this cell may step through {@code face} along the run: an end face, or a corner's end or side. */
    public static boolean along(Rack rack, int face) {
        if (rack.cornered()) return face == rack.end() || face == rack.side();
        return face == rack.front() || face == rack.back();
    }

    // ---- points ----------------------------------------------------------------------------------------------------

    /**
     * The canonical point (u, y, w) where a lane of {@code option} meets face {@code face}, or null where the option has
     * no arm there or does not take the width ({@link #takes}). {@code outside} sets a tier line's height.
     */
    static double[] canonicalPoint(Rack rack, int option, int width, double outside, int face) {
        if (!takes(option, width)) return null;
        int c = rack.canonical(face);
        double mid = .5;
        if (!tier(option)) return rack.cornered() || !side(c) ? null : new double[]{c == LEFT ? 0 : 1, (option == crossing(ABOVE) ? UPPER : LOWER) * S, mid};
        int level = level(option);
        double u = centre(position(option), width), h = height(outside) * S;
        if (rack.cornered()) {
            if (face == rack.end()) return new double[]{u, h, c == BACK ? 0 : 1};
            if (face != rack.side()) return null;
            double d = c == RIGHT ? 1 - u : u;
            return new double[]{c == RIGHT ? 1 : 0, h, rack.canonical(rack.end()) == BACK ? d : 1 - d};
        }
        return switch (c) {
            case FRONT -> new double[]{u, h, 1};
            case BACK -> new double[]{u, h, 0};
            case TOP -> level == TIER ? new double[]{u, 1, mid} : null;
            case BOTTOM -> level == TIER ? new double[]{u, 0, mid} : null;
            default -> new double[]{c == LEFT ? 0 : 1, (level == TIER ? h : level == ABOVE ? UPPER * S : LOWER * S), mid};
        };
    }

    /**
     * Where a lane of {@code option} meets face {@code face}: its offsets from the face's centre in 1/32 m steps along
     * the face's in-plane axes, packed as ten bits as {@link LineGeometry.Key#offsets} packs them; -1 where it has no arm
     * there. Two lanes in cells side by side meet where their points on the shared face are equal.
     */
    public static int point(Rack rack, int option, int width, double outside, int face) {
        var p = canonicalPoint(rack, option, width, outside, face);
        if (p == null) return -1;
        var world = rack.world(p[0], p[1], p[2]);
        int axis = LineGeometry.axis(face);
        int u = (int) Math.round((world[(axis + 1) % 3] - .5) / S), v = (int) Math.round((world[(axis + 2) % 3] - .5) / S);
        return (int) (LineGeometry.Key.offsets(0, u, v));
    }

    /** A point's two offsets, unpacked: along the face's first and second in-plane axis. */
    public static int[] steps(int point) {
        int u = point & 31, v = point >> 5 & 31;
        return new int[]{u >= 16 ? u - 32 : u, v >= 16 ? v - 32 : v};
    }

    /** A lane's points on its arm faces, ten bits per face (as {@link LineGeometry.Key#offsets}): what its registration carries. */
    public static long points(Rack rack, Lane lane) {
        long out = 0;
        for (int face = 0; face < 6; face++)
            if ((lane.arms >> face & 1) != 0) out |= (long) point(rack, lane.option, lane.width, lane.diameter(), face) << 10 * face;
        return out;
    }

    /** A lane's point on face {@code face} in block coordinates. */
    public static double[] facePoint(Rack rack, Lane lane, int face) {
        var p = canonicalPoint(rack, lane.option, lane.width, lane.diameter(), face);
        return p == null ? null : rack.world(p[0], p[1], p[2]);
    }

    // ---- centre lines ----------------------------------------------------------------------------------------------

    /**
     * A lane's centre line in block coordinates, from its first arm's face point to its second's through its corners: a
     * straight line, a corner's two legs meeting at its corner point (it is drawn as a bend about the inner corner), a
     * turn at mid-block up, down or out flat, or a rise or drop to a crossing level at mid-block and out sideways.
     */
    static List<double[]> path(Rack rack, Lane lane) {
        int a = first(rack, lane), b = second(rack, lane);
        var pa = canonicalPoint(rack, lane.option, lane.width, lane.diameter(), a);
        var pb = canonicalPoint(rack, lane.option, lane.width, lane.diameter(), b);
        var points = new ArrayList<double[]>();
        points.add(pa);
        int cb = rack.canonical(b);
        if (tier(lane.option) && rack.cornered()) points.add(new double[]{pa[0], pa[1], pb[2]});
        else if (tier(lane.option) && cb != FRONT && cb != BACK) {
            points.add(new double[]{pa[0], pa[1], .5});
            // A side arm over or under the tier rises or drops at mid-block first.
            if (side(cb) && level(lane.option) != TIER) points.add(new double[]{pa[0], pb[1], .5});
        }
        points.add(pb);
        var out = new ArrayList<double[]>(points.size());
        for (var p : points) out.add(rack.world(p[0], p[1], p[2]));
        return out;
    }

    /** One lane's polygons, cell-local: the rack draws each lane apart for its paint, and the laying tool's ghost draws a lane it would lay. */
    public static List<Polygon> lane(Rack rack, Lane lane) {
        var m = new Mesh();
        lane(m, rack, lane);
        return m.cell(0, 0, 0);
    }

    /**
     * One lane: a straight pipe; a corner's bend about the inner corner, so the bends of a corner nest and keep their
     * order; or the bends of a turn at mid-block, as a transition pipe draws them. A lane is welded where a line next door
     * continues it and capped where nothing does: a flange would be wider than its position.
     */
    static void lane(Mesh m, Rack rack, Lane lane) {
        double od = lane.diameter(), r = od / 2;
        int sides = PipeSizes.sides(od);
        int a = first(rack, lane), b = second(rack, lane);
        var path = path(rack, lane);
        LineGeometry.End ea = bare(lane, a, od), eb = bare(lane, b, od);
        if (path.size() == 2) {
            var middle = lerp(path.get(0), path.get(1), .5);
            LineGeometry.arm(m, middle, a, 0, od, ea, LineGeometry.ref(a), sides, false);
            LineGeometry.arm(m, middle, b, 0, od, eb, LineGeometry.ref(b), sides, false);
            return;
        }
        if (tier(lane.option) && rack.cornered()) {
            // Radius: the lane's distance from the inner corner, the same on both legs.
            var corner = path.get(1);
            double[] da = LineGeometry.step(a), db = LineGeometry.step(b);
            double bend = Math.abs(dot(sub(path.get(0), corner), da));
            var origin = new double[]{corner[0] + bend * (da[0] + db[0]), corner[1] + bend * (da[1] + db[1]), corner[2] + bend * (da[2] + db[2])};
            var f = new Frame(origin, cross(db, da), neg(db));
            m.torus(f, bend, r, 0, Math.PI / 2, Math.max(3, (int) Math.ceil(Math.PI / 2 * bend / .08)), sides, Material.PIPE);
            if (!ea.joined()) m.annulus(new Frame(corner, da, neg(db)), 0, r, bend, sides, 1, Material.PIPE);
            if (!eb.joined()) m.annulus(new Frame(corner, db, neg(da)), 0, r, bend, sides, 1, Material.PIPE);
            return;
        }
        LineGeometry.line(m, LineGeometry.fitted(path, ea, eb, od, BEND), a, b, od, sides);
    }

    /** A lane's plain end at face {@code face}: no fitting, welded where joined, else capped. */
    private static LineGeometry.End bare(Lane lane, int face, double od) {
        return new LineGeometry.End(od, false, 0, 0, (lane.joined >> face & 1) != 0);
    }

    /**
     * The boxes a lane fills, cell-local {lo x, lo y, lo z, hi x, hi y, hi z}, one per leg of its centre line (a corner's
     * legs meet at its corner point): its shape for collision and for picking the lane a player aims at.
     */
    public static List<double[]> boxes(Rack rack, Lane lane) {
        double r = lane.diameter() / 2;
        var path = path(rack, lane);
        var out = new ArrayList<double[]>();
        for (int k = 0; k + 1 < path.size(); k++) {
            double[] p = path.get(k), q = path.get(k + 1), box = new double[6];
            for (int i = 0; i < 3; i++) {
                box[i] = Math.max(0, Math.min(p[i], q[i]) - r);
                box[i + 3] = Math.min(1, Math.max(p[i], q[i]) + r);
            }
            out.add(box);
        }
        return out;
    }

    /** The lane a click hits and the arm the leg it hits leads to, by index in a rack's lane list and face; {@link #NONE} off every lane. */
    public record Pick(int lane, int face) {
    }

    public static final Pick NONE = new Pick(-1, -1);
    /** How far outside a leg box a hit point may lie and still pick it: a hit lands on the box's surface, give or take rounding. */
    public static final double PICK_TOLERANCE = 1e-3;

    /**
     * The lane a click at cell-local point {@code (x, y, z)} hits: the one with a leg box nearest the point, within
     * {@link #PICK_TOLERANCE}. Its first leg leads to its first arm, every later leg to its second.
     */
    public static Pick pick(Rack rack, List<Lane> lanes, double x, double y, double z) {
        var best = NONE;
        double nearest = PICK_TOLERANCE;
        for (int i = 0; i < lanes.size(); i++) {
            var boxes = boxes(rack, lanes.get(i));
            for (int k = 0; k < boxes.size(); k++) {
                var b = boxes.get(k);
                double outside = Math.max(Math.max(Math.max(b[0] - x, x - b[3]), Math.max(b[1] - y, y - b[4])), Math.max(b[2] - z, z - b[5]));
                if (outside <= nearest) {
                    nearest = outside;
                    best = new Pick(i, k == 0 ? first(rack, lanes.get(i)) : second(rack, lanes.get(i)));
                }
            }
        }
        return best;
    }

    // ---- steel -----------------------------------------------------------------------------------------------------

    /** The frame's polygons, cell-local. */
    public static List<Polygon> polygons(Rack rack) {
        var m = new Mesh();
        for (var box : members(rack)) m.box(new double[]{box[0], box[1], box[2]}, new double[]{box[3], box[4], box[5]}, ALL_FACES, Material.STEELWORK);
        return m.cell(0, 0, 0);
    }

    /**
     * The steel of a cell, block-local boxes {lo x, lo y, lo z, hi x, hi y, hi z}. A straight cell: a bent in each plane
     * it has one in, two posts 1/16 m square inset 1/32 m from the sides and a beam 1/16 m deep whose top carries the
     * lines; and a stringer 1/32 m deep along each post line at the beam's top. A corner: posts at its three outer
     * corners, beams along its two outer sides. A straight cell's beams stand at its {@link Rack#beam} top. Posts stop at
     * the stringers' top, or run to the top under a stacked rack. A side
     * joined by a rack beside it has no post or stringer, its beams running through to the next.
     */
    public static List<double[]> members(Rack rack) {
        var out = new ArrayList<double[]>();
        double post = (rack.stacked ? 32 : BEAM_TOP) * S;
        if (rack.cornered()) {
            boolean right = rack.canonical(rack.side()) == RIGHT, back = rack.canonical(rack.end()) == BACK;
            // The inner corner at (right ? 1 : 0, back ? 0 : 1): posts at the other three.
            for (int cu = 0; cu < 2; cu++)
                for (int cw = 0; cw < 2; cw++) {
                    if (cu == (right ? 1 : 0) && cw == (back ? 0 : 1)) continue;
                    out.add(box(rack, cu == 0 ? 1 : 29, cu == 0 ? 3 : 31, 0, post / S, cw == 0 ? 1 : 29, cw == 0 ? 3 : 31));
                }
            double uLow = right ? 1 : 29, wLow = back ? 29 : 1;
            out.add(box(rack, uLow, uLow + 2, BEAM_BOTTOM, BEAM_TOP, 1, 31));
            out.add(box(rack, 1, 31, BEAM_BOTTOM, BEAM_TOP, wLow, wLow + 2));
            return out;
        }
        boolean leftJoined = (rack.sides & 1) != 0, rightJoined = (rack.sides & 2) != 0;
        for (int plane = 0; plane < 2; plane++) {
            if ((rack.bents >> plane & 1) == 0) continue;
            double w0 = plane == 0 ? 0 : 30, w1 = w0 + 2;
            if (!leftJoined) out.add(box(rack, 1, 3, 0, post / S, w0, w1));
            if (!rightJoined) out.add(box(rack, 29, 31, 0, post / S, w0, w1));
            out.add(box(rack, leftJoined ? 0 : 1, rightJoined ? 32 : 31, rack.beam - (BEAM_TOP - BEAM_BOTTOM), rack.beam, w0, w1));
        }
        if (!leftJoined) out.add(box(rack, 1, 3, STRINGER_BOTTOM, BEAM_TOP, 0, 32));
        if (!rightJoined) out.add(box(rack, 29, 31, STRINGER_BOTTOM, BEAM_TOP, 0, 32));
        return out;
    }

    /** A canonical box, in 1/32 m steps, as block coordinates. */
    private static double[] box(Rack rack, double u0, double u1, double y0, double y1, double w0, double w1) {
        var a = rack.world(u0 * S, y0 * S, w0 * S);
        var b = rack.world(u1 * S, y1 * S, w1 * S);
        return new double[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])};
    }
}
