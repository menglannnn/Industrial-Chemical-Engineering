package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * Access steelwork drawn along a wall (shared by a column's structure, {@link ColumnStructure}, and the fired heaters of
 * {@link ProcessUnit}; owner request 2026-10-17, {@code MACHINES_HEATERS_PLAN.md}): grating decks with toe boards and
 * knee-braced brackets, handrails with mid rails and posts, and caged ladders. A deck follows a {@link Run}: an arc round a
 * round casing or a straight wall. Sizes are plant practice (OSHA 1910.23/.29, the photo survey of the column batch).
 */
final class Access {
    /** Gap between the wall and a deck, deck thickness (walkable at the collision resolution), m. */
    static final double GAP = .05, DECK = .125;
    /** Handrail height over the deck, mid-rail height, toe board height, rail and bar section, largest post spacing, m. */
    static final double RAIL = 1.07, MID_RAIL = .53, TOE = .1, BAR = 1 / 16.0, POSTS = 1.8;
    /** Ladder: rung plane beyond the wall, half width between the rails, rung pitch, cage half width, cage start above the foot, hoop pitch, rails above the landing. */
    static final double STANDOFF = .2, LADDER_HALF = .225, RUNG = .25, CAGE_HALF = .35, CAGE_START = 2.2, HOOP = 1, LANDING = 1.07;
    /**
     * Collision walls: the handrail's band inside the deck edge (and across a closed end), and the rung plane's face beyond
     * the rung line, m. The game collides with a shell's solid sampled at the centres of 1/8-block voxels
     * ({@link ShapeRasterizer#solid}), so a wall a player must meet is at least that thick; a 0.1 m end fence fell between the
     * centres and let a player walk off a heater's walkway (owner report 2026-10-17).
     */
    static final double RAIL_BAND = .2, RUNG_FACE = .06;
    /** A fence's collision height over the deck, above the drawn top rail so a player cannot jump it, m. */
    static final double FENCE = 1.4;
    /**
     * A fence's collision band where a player walks between two of them (a stair flight's sides, a stair tower's lane
     * landing between its closed ends), m: just over one 1/8-block voxel, the least band that always holds a voxel centre.
     * A 0.9 m lane keeps 0.75 m clear on cell centres and 0.625 m anywhere for a 0.6 m player; {@link #RAIL_BAND} either
     * side left 0.5 m, and a player stuck.
     */
    static final double NARROW_BAND = .13;
    /**
     * Room a player takes across a deck, m: its collision box is 0.6 m square and stays aligned with the world, and walls
     * are sampled at cell-fraction centres, so a wall's voxels stand up to half a voxel proud of it; on a round casing's
     * diagonals, where ladders stand, the two together span (0.6 + 1/8)·√2 radially.
     */
    static final double PLAYER = (.6 + 1.0 / ShapeRasterizer.SOLID_RESOLUTION) * Math.sqrt(2);
    /**
     * Least deck width, m, to the next 5 cm: a player stands on the deck in front of the ladder that starts there, between
     * the rung plane and the handrail (1.45 m; plant platforms are 0.9 to 1.5 m wide, those carrying a caged ladder the widest).
     */
    static final double MIN_DECK = Math.ceil((STANDOFF + RUNG_FACE + PLAYER + RAIL_BAND - GAP) * 20) / 20;
    /**
     * The longest climb of a caged ladder between rest platforms, m (the old OSHA 1910.27 30 ft rule, kept by plant
     * practice; owner decision C1 2026-10-18: columns and units alike).
     */
    static final int MAX_CLIMB = 9;
    /**
     * Parts of a deck ({@link #deck}): knee-braced brackets to its wall at every post, a railed outer edge (toe board and
     * handrail over the rail's pieces, a bare grating edge elsewhere), and a face along its wall (a deck round an opening,
     * whose wall edge is seen); a deck on beams or posts has no brackets.
     */
    static final int BRACKETS = 1, RAILED = 2, WALL_EDGE = 4, ALL_PARTS = BRACKETS | RAILED | WALL_EDGE;
    /**
     * A deck walked along between its closed ends ({@link ProcessUnit#platform}): their fences are {@link #NARROW_BAND}
     * deep (a stair tower's lane landing, 0.9 m across). A flag beside the parts; it draws nothing.
     */
    static final int NARROW = 8;

    private static final double[] UP = {0, 1, 0}, DOWN = {0, -1, 0};

    private Access() {
    }

    /**
     * Half the angle a ladder and its cage take round a round wall of radius {@code wall} (its rung plane
     * {@link #STANDOFF} out), seen from the axis at the deck's inner edge, with room for a rail: where a deck ends beside
     * a ladder (a column's, a heater's or a reactor's).
     */
    static double clearance(double wall) {
        double across = CAGE_HALF + .2, out = wall + STANDOFF - .05;
        return DeterministicMath.acos(out / Math.sqrt(out * out + across * across));
    }

    /**
     * Deck levels for caged ladders from {@code base} (a deck level, or 0: the ground) up to {@code top}: each of the
     * {@code wanted} levels over base and under top (a manway's, a row of peep doors'), then top, and rest platforms
     * splitting every climb longer than {@link #MAX_CLIMB} into equal ones to the nearest metre (owner decision C1
     * 2026-10-18; a column's {@link ColumnStructure#plan}). Every level after base, rising, top last.
     */
    static List<Integer> climbs(int base, List<Integer> wanted, int top) {
        var stops = new java.util.TreeSet<Integer>();
        for (int level : wanted) if (level > base && level < top) stops.add(level);
        stops.add(top);
        var out = new ArrayList<Integer>();
        int previous = base;
        for (int level : stops) {
            int gap = level - previous, parts = (gap + MAX_CLIMB - 1) / MAX_CLIMB;
            for (int i = 1; i < parts; i++) out.add(previous + (int) Math.round(gap * i / (double) parts));
            out.add(level);
            previous = level;
        }
        return out;
    }

    /**
     * The pieces of a run of {@code length} left between its {@code gaps} (pairs of stations, rising): where a deck's
     * handrail and toe board run, open where a ladder is walked through or a stair lands; gaps reaching past the run's
     * ends are cut at them. A piece under 0.05 m is dropped.
     */
    static List<double[]> pieces(double length, double... gaps) {
        var out = new ArrayList<double[]>();
        double from = 0;
        for (int g = 0; g + 1 < gaps.length; g += 2) {
            double to = Math.min(length, gaps[g]);
            if (to > from + .05) out.add(new double[]{from, to});
            from = Math.max(from, Math.min(length, gaps[g + 1]));
        }
        if (length > from + .05) out.add(new double[]{from, length});
        return out;
    }

    /**
     * A deck's run along its wall: the point {@code off} out from the wall and {@code s} along the run (0 to
     * {@link #length}, measured on the deck's outer edge), at height {@code y}; the outward and along directions there.
     */
    interface Run {
        double length();

        /** Pieces the run's deck and rails are drawn in: one on a straight run, short chords round an arc. */
        int segments();

        double[] at(double s, double off, double y);

        double[] out(double s);

        double[] along(double s);
    }

    /**
     * A run round a round wall of radius {@code wall} about the vertical line through (cx, cz), from angle {@code from} to
     * {@code to} (counter-clockwise from +x towards +z), its deck's outer edge at radius {@code edge}, drawn in chords of
     * at most {@code step} along that edge.
     */
    record Arc(double cx, double cz, double wall, double from, double to, double edge, double step) implements Run {
        /** A column's chord: 0.25 m. */
        static final double STEP = .25;

        /** The longest chord round an edge of radius {@code edge} that strays at most 5 mm from it, and 0.25 m at least. */
        static double step(double edge) {
            return Math.max(STEP, Math.sqrt(8 * edge * .005));
        }

        public double length() {
            return (to - from) * edge;
        }

        public int segments() {
            return Math.max(2, (int) Math.ceil(length() / step));
        }

        private double angle(double s) {
            return from + (to - from) * s / length();
        }

        public double[] at(double s, double off, double y) {
            double a = angle(s), r = wall + off;
            return new double[]{cx + r * DeterministicMath.cos(a), y, cz + r * DeterministicMath.sin(a)};
        }

        public double[] out(double s) {
            double a = angle(s);
            return new double[]{DeterministicMath.cos(a), 0, DeterministicMath.sin(a)};
        }

        public double[] along(double s) {
            double a = angle(s);
            return new double[]{-DeterministicMath.sin(a), 0, DeterministicMath.cos(a)};
        }
    }

    /** A run along a straight wall from {@code start} (on the wall, at any height), {@code length} along the horizontal {@code direction}, decked towards {@code out}. */
    record Line(double[] start, double[] direction, double[] out, double length) implements Run {
        public double[] at(double s, double off, double y) {
            return new double[]{start[0] + s * direction[0] + off * out[0], y, start[2] + s * direction[2] + off * out[2]};
        }

        public int segments() {
            return 1;
        }

        public double[] out(double s) {
            return out;
        }

        public double[] along(double s) {
            return direction;
        }

        /**
         * The stations along this run of {@code span} ({lo, hi} frame coordinates along its direction's axis: a stair
         * tower's opening onto it, {@link ProcessUnit.Tower}), rising: a gap for its deck's rail.
         */
        double[] gap(double[] span) {
            int a = Math.abs(direction[0]) > .5 ? 0 : 2;
            double s0 = (span[0] - start[a]) * direction[a], s1 = (span[1] - start[a]) * direction[a];
            return new double[]{Math.min(s0, s1), Math.max(s0, s1)};
        }
    }

    /** The stretch of {@code run} from station {@code s0} to {@code s1}, a run of its own. */
    static Run part(Run run, double s0, double s1) {
        if (run instanceof Arc arc) {
            double span = arc.to() - arc.from(), length = arc.length();
            return new Arc(arc.cx(), arc.cz(), arc.wall(), arc.from() + span * s0 / length, arc.from() + span * s1 / length, arc.edge(), arc.step());
        }
        var line = (Line) run;
        return new Line(line.at(s0, 0, line.start()[1]), line.direction(), line.out(), s1 - s0);
    }

    /**
     * A deck of grating {@code width} out from its wall along {@code run}, its top {@link #DECK} over {@code level}, with
     * its {@code parts} ({@link #BRACKETS}, {@link #RAILED}, {@link #WALL_EDGE}): its top, underside and ends always; the
     * face along its wall; along a railed outer edge a toe board ({@link #TOE} high, OSHA 1910.29(k)) over each piece of
     * the handrail between the {@code railGaps} (stations, as {@link #pieces}), its outer face one with the deck's edge and
     * its ends closed, and the bare grating edge across each gap (no toe board across a ladder's or a stair's opening,
     * where it would trip the person stepping off: owner decision 2026-10-18); and a knee-braced bracket at every post.
     * Round an arc a gap's ends cut the deck's chords along their straight edge, so the edge keeps its outline.
     */
    static void deck(Mesh m, Run run, double level, double width, int parts, double... railGaps) {
        double y0 = level, y1 = y0 + DECK, inner = GAP, toe = width - 1 / 32.0, length = run.length();
        int segments = run.segments();
        var rail = (parts & RAILED) != 0 ? pieces(length, railGaps) : List.<double[]>of();
        for (int j = 0; j < segments; j++) {
            double s0 = length * j / segments, s1 = length * (j + 1) / segments, mid = (s0 + s1) / 2;
            var out = run.out(mid);
            m.flat(List.of(run.at(s0, inner, y1), run.at(s1, inner, y1), run.at(s1, width, y1), run.at(s0, width, y1)), UP, Material.GRATING);
            m.flat(List.of(run.at(s0, inner, y0), run.at(s1, inner, y0), run.at(s1, width, y0), run.at(s0, width, y0)), DOWN, Material.GRATING);
            if ((parts & WALL_EDGE) != 0) m.flat(List.of(run.at(s0, inner, y0), run.at(s1, inner, y0), run.at(s1, inner, y1), run.at(s0, inner, y1)), neg(out), Material.GRATING);
            // The outer edge between the chord's ends and the rail pieces' ends inside it: railed or bare in turn.
            var cuts = new ArrayList<Double>(List.of(s0));
            for (var piece : rail)
                for (double s : piece) if (s > s0 + 1e-9 && s < s1 - 1e-9) cuts.add(s);
            cuts.add(s1);
            cuts.sort(Double::compare);
            for (int c = 0; c + 1 < cuts.size(); c++) {
                double a = cuts.get(c), b = cuts.get(c + 1), top = y1 + TOE;
                boolean railed = false;
                for (var piece : rail) railed |= (a + b) / 2 > piece[0] && (a + b) / 2 < piece[1];
                if (!railed) {
                    m.flat(List.of(chord(run, s0, s1, a, width, y0), chord(run, s0, s1, b, width, y0), chord(run, s0, s1, b, width, y1), chord(run, s0, s1, a, width, y1)), out,
                            Material.GRATING);
                    continue;
                }
                m.flat(List.of(chord(run, s0, s1, a, width, y0), chord(run, s0, s1, b, width, y0), chord(run, s0, s1, b, width, top), chord(run, s0, s1, a, width, top)), out,
                        Material.HANDRAIL);
                m.flat(List.of(chord(run, s0, s1, a, toe, y1), chord(run, s0, s1, b, toe, y1), chord(run, s0, s1, b, toe, top), chord(run, s0, s1, a, toe, top)), neg(out),
                        Material.HANDRAIL);
                m.flat(List.of(chord(run, s0, s1, a, toe, top), chord(run, s0, s1, b, toe, top), chord(run, s0, s1, b, width, top), chord(run, s0, s1, a, width, top)), UP,
                        Material.HANDRAIL);
            }
        }
        for (double end : new double[]{0, length}) {
            var along = end == 0 ? neg(run.along(end)) : run.along(end);
            m.flat(List.of(run.at(end, inner, y0), run.at(end, width, y0), run.at(end, width, y1), run.at(end, inner, y1)), along, Material.GRATING);
        }
        // The toe board's ends, at each rail piece's.
        for (var piece : rail)
            for (int e = 0; e < 2; e++) {
                double end = piece[e];
                int j = Math.min(segments - 1, (int) Math.floor(end / length * segments));
                double s0 = length * j / segments, s1 = length * (j + 1) / segments;
                var along = e == 0 ? neg(run.along(end)) : run.along(end);
                m.flat(List.of(chord(run, s0, s1, end, toe, y1), chord(run, s0, s1, end, width, y1), chord(run, s0, s1, end, width, y1 + TOE), chord(run, s0, s1, end, toe, y1 + TOE)),
                        along, Material.HANDRAIL);
            }
        if ((parts & BRACKETS) != 0)
            for (double s : posts(length)) {
                m.bar(run.at(s, .02, y0 - .04), run.at(s, width - .05, y0 - .04), BAR * 1.5, UP, Material.STEELWORK);
                m.bar(run.at(s, .02, y0 - .6), run.at(s, .02 + .6 * width, y0 - .07), BAR, run.out(s), Material.STEELWORK);
            }
    }

    /**
     * The point {@code off} out at station {@code s} on the straight chord of {@code run} from station {@code s0} to
     * {@code s1}: the run's own point at the chord's ends (and at a run's ends), else on the chord between them.
     */
    private static double[] chord(Run run, double s0, double s1, double s, double off, double y) {
        if (s <= s0 + 1e-9 || s >= s1 - 1e-9 || run.segments() == 1) return run.at(s, off, y);
        return lerp(run.at(s0, off, y), run.at(s1, off, y), (s - s0) / (s1 - s0));
    }

    /**
     * The handrail of a deck {@code width} out from its wall: posts along the outer edge and at closed ends; top and mid
     * rails along the edge (mitred where they bend) and across a closed end. Posts stand in the deck with their tops flush
     * in the top rail, and rails end inside the posts at a run's ends: those bar ends are never seen and not drawn.
     */
    static void handrail(Mesh m, Run run, double level, double width, boolean closedStart, boolean closedEnd) {
        double base = level + DECK, r = width - .03, inner = .12, length = run.length();
        for (double s : posts(length)) m.bar(run.at(s, r, base), run.at(s, r, base + RAIL + BAR / 2), BAR, run.out(s), NO_ENDS, Material.HANDRAIL);
        int segments = run.segments();
        for (double h : new double[]{RAIL, MID_RAIL}) {
            var points = new ArrayList<double[]>(segments + 1);
            for (int j = 0; j <= segments; j++) points.add(run.at(length * j / segments, r, base + h));
            m.rail(points, BAR, UP, NO_ENDS, Material.HANDRAIL);
        }
        if (closedStart) endRail(m, run, 0, level, width);
        if (closedEnd) endRail(m, run, length, level, width);
    }

    /**
     * The rail across a deck's closed end at station {@code s} of its run: a post by the wall and the handrail and mid
     * rail out to the side rail's line. Drawn with the side rail's piece that reaches the end, or alone where a landing's
     * opening in the side rail starts at the end (a stair landing beside a walkway's end).
     */
    static void endRail(Mesh m, Run run, double s, double level, double width) {
        double base = level + DECK, r = width - .03, inner = .12;
        m.bar(run.at(s, inner, base), run.at(s, inner, base + RAIL + BAR / 2), BAR, run.out(s), NO_ENDS, Material.HANDRAIL);
        for (double h : new double[]{RAIL, MID_RAIL}) m.bar(run.at(s, inner, base + h), run.at(s, r, base + h), BAR, UP, NO_ENDS, Material.HANDRAIL);
    }

    /** Post stations along a run: both ends and every {@link #POSTS} or less between. */
    static List<Double> posts(double length) {
        int count = Math.max(1, (int) Math.ceil(length / POSTS));
        var out = new ArrayList<Double>();
        for (int j = 0; j <= count; j++) out.add(length * j / count);
        return out;
    }

    /** Whether a ladder from {@code base} to {@code top} carries a cage: a climb of more than {@link #CAGE_START} and half a metre. */
    static boolean caged(double base, double top) {
        return top - base > CAGE_START + .5;
    }

    /**
     * A caged ladder as a player meets it: its rung plane's middle at (x, z), facing (outX, outZ) (horizontal, away from
     * its wall), from its foot {@code base} to its {@code top}; its rung plane stops at {@code rungTop}, the landing of a
     * ladder walked through (the player steps off between its rails), else its top. Shared by a column's ladders and a
     * heater's, so both climb and collide alike (owner report 2026-10-17: the heaters' ladders could not be climbed).
     */
    record Climb(double x, double z, double outX, double outZ, double base, double top, double rungTop) {
        /** The climb zone's reach out from the rung plane and half width across it, a little beyond the cage. */
        static final double REACH = 2 * CAGE_HALF + .1, HALF = CAGE_HALF + .05;

        /** The rung plane a player presses against to climb: from 0.14 m behind the rung line to {@link #RUNG_FACE} in front, between the rails. */
        boolean rungs(double[] p) {
            double along = along(p);
            return p[1] >= base && p[1] <= rungTop && along >= -.14 && along <= RUNG_FACE && Math.abs(across(p)) <= LADDER_HALF;
        }

        /** Whether a player standing at {@code p} (feet) climbs: in front of the rungs, inside the cage, from just under the foot to the top. */
        boolean climbable(double[] p) {
            double along = along(p);
            return p[1] >= base - .1 && p[1] <= top && along >= -.05 && along <= REACH && Math.abs(across(p)) <= HALF;
        }

        /** The climb zone's footprint, {out from the rung plane, across}, its four corners. */
        double[][] footprint() {
            double[][] corners = new double[4][];
            int k = 0;
            for (double o : new double[]{-.05, REACH})
                for (double t : new double[]{-HALF, HALF}) corners[k++] = new double[]{x + o * outX - t * outZ, z + o * outZ + t * outX};
            return corners;
        }

        private double along(double[] p) {
            return (p[0] - x) * outX + (p[2] - z) * outZ;
        }

        private double across(double[] p) {
            return -(p[0] - x) * outZ + (p[2] - z) * outX;
        }
    }

    /**
     * A ladder ({@link Climb}) standing {@code back} out from its wall: two rails on brackets to the wall every 2 m, rungs every
     * {@link #RUNG} up to the landing {@link #LANDING} under the rails' top, and when {@link #caged} a cage of hoops every
     * {@link #HOOP} from {@link #CAGE_START} over the foot, with five vertical bars, to the rails' top. Rungs and hoops
     * (mitred where they bend) end inside the rails, cage bars start inside the first hoop (the last may sit below the
     * rails' top), brackets against the rails and the wall, and the rails stand on a deck or the ground: only the rails'
     * tops and the hoops' bends show their ends.
     */
    static void ladder(Mesh m, Climb climb, double back) {
        double[] foot = {climb.x(), 0, climb.z()}, out = {climb.outX(), 0, climb.outZ()}, across = {-out[2], 0, out[0]};
        double base = climb.base(), top = climb.top();
        double landing = top - LANDING;
        for (double side : new double[]{-LADDER_HALF, LADDER_HALF}) {
            m.bar(at(foot, out, across, 0, side, base), at(foot, out, across, 0, side, top), BAR, out, END, Material.HANDRAIL);
            for (double y = base + 1; y < top - .3; y += 2)
                m.bar(at(foot, out, across, .01 - back, side, y), at(foot, out, across, -BAR / 2, side, y), BAR, UP, NO_ENDS, Material.STEELWORK);
        }
        for (double y = base + RUNG; y <= landing + 1e-9; y += RUNG)
            m.bar(at(foot, out, across, 0, -LADDER_HALF, y), at(foot, out, across, 0, LADDER_HALF, y), BAR * .8, UP, NO_ENDS, Material.HANDRAIL);
        if (!caged(base, top)) return;
        var hoop = hoop();
        double first = base + CAGE_START;
        for (double y = first; y <= top + 1e-9; y = y + HOOP > top && y < top - .2 ? top : y + HOOP) {
            var points = new ArrayList<double[]>(hoop.size());
            for (var point : hoop) points.add(at(foot, out, across, point[0], point[1], y));
            m.rail(points, BAR, UP, NO_ENDS, Material.HANDRAIL);
        }
        for (int k : new int[]{2, 3, 4, 5, 6})
            m.bar(at(foot, out, across, hoop.get(k)[0], hoop.get(k)[1], first), at(foot, out, across, hoop.get(k)[0], hoop.get(k)[1], top), BAR, out, END, Material.HANDRAIL);
    }

    /** A cage hoop as (out from the rung plane, across) points: from one rail out and round the back to the other. */
    private static List<double[]> hoop() {
        var out = new ArrayList<double[]>();
        out.add(new double[]{0, -LADDER_HALF});
        out.add(new double[]{0, -CAGE_HALF});
        for (int k = 0; k <= 4; k++) {
            double phi = -Math.PI / 2 + Math.PI * k / 4;
            out.add(new double[]{CAGE_HALF + CAGE_HALF * DeterministicMath.cos(phi), CAGE_HALF * DeterministicMath.sin(phi)});
        }
        out.add(new double[]{0, CAGE_HALF});
        out.add(new double[]{0, LADDER_HALF});
        return out;
    }

    /**
     * A pipe davit on a round top deck, its mast {@code r} out from the vertical axis through (cx, cz) at angle {@code t}
     * (from +x towards +z), from the deck's walking level {@code base} to {@code top}: the mast on its base plate, the jib
     * from behind the mast out (or in) to {@code reach} from the axis with its knee brace, the cable and the hook block
     * (davits for lifting a column's or a reactor's internals and relief valves; photo survey section 2).
     */
    static void davit(Mesh m, double cx, double cz, double t, double r, double base, double top, double reach) {
        double[] radial = {DeterministicMath.cos(t), 0, DeterministicMath.sin(t)}, tangent = {-radial[2], 0, radial[0]};
        double s = reach > r ? 1 : -1;
        var f = new Frame(around(cx, cz, r, t, base), UP, radial);
        m.prism(f, .1, 0, top - base, 8, Material.HANDRAIL);
        m.annulus(f, 0, .1, top - base, 8, 1, Material.HANDRAIL);
        m.block(around(cx, cz, r, t, base + .02), scale(radial, .18), new double[]{0, .02, 0}, scale(tangent, .18), Material.STEELWORK);
        m.bar(around(cx, cz, r - s * .1, t, top - .1), around(cx, cz, reach, t, top - .1), .14, UP, Material.HANDRAIL);
        m.bar(around(cx, cz, r, t, top - 1.1), around(cx, cz, r + .6 * (reach - r), t, top - .17), .07, tangent, Material.HANDRAIL);
        m.bar(around(cx, cz, reach - s * .05, t, top - .17), around(cx, cz, reach - s * .05, t, top - 1), .025, radial, Material.STEELWORK);
        m.block(around(cx, cz, reach - s * .05, t, top - 1.07), scale(radial, .07), new double[]{0, .07, 0}, scale(tangent, .07), Material.STEELWORK);
    }

    /** The point {@code r} out from the vertical axis through (cx, cz) at angle {@code t}, at height {@code y}. */
    static double[] around(double cx, double cz, double r, double t, double y) {
        return new double[]{cx + r * DeterministicMath.cos(t), y, cz + r * DeterministicMath.sin(t)};
    }

    /** The point {@code o} out from the rung plane and {@code t} across from the ladder's middle at {@code foot}, at height {@code y}. */
    private static double[] at(double[] foot, double[] out, double[] across, double o, double t, double y) {
        return new double[]{foot[0] + o * out[0] + t * across[0], y, foot[2] + o * out[2] + t * across[2]};
    }

}
