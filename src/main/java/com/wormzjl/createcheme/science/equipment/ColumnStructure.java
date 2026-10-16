package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * What a distillation column carries besides its shell (owner request 2026-10-16, batch
 * 2026-10-15-custom-equipment-rendering; photo survey section 2): the insulation's aluminium cladding with its rain
 * flashing rings, part-ring platforms at the manways (grating decks, handrails with mid-rails and toe boards, knee
 * braces), caged ladders between them, the manways with their hinge davits, a full ring at the top with the lifting davit,
 * and the process nozzles.
 *
 * <p>Plant layout keeps a column's piping on one side and its access on the other: the nozzles face the front (towards the
 * constructor and the pipe rack) and the two sides, the platforms, ladders and manways the back. Ladders stand at the two
 * back diagonals and alternate at each platform, so every climb ends beside the next deck and starts on the deck below.
 *
 * <p>Built in the vessel frame ({@link EquipmentShape}): axis along +y through (a, ·, a), front -z, back +z; an angle θ
 * runs from +x towards +z, so the back is θ = 90°. Heights are frame y, which is box y for a standing vessel; deck levels
 * are whole metres, so a deck's walking surface is {@link #DECK} above a block boundary.
 */
final class ColumnStructure {
    /** Gap between cladding and deck, deck thickness (walkable at the collision resolution), m. */
    static final double GAP = .05, DECK = .125;
    /** Handrail height over the deck, mid-rail height, toe board height, rail and bar section, largest post spacing, m. */
    static final double RAIL = 1.07, MID_RAIL = .53, TOE = .1, BAR = 1 / 16.0, POSTS = 1.8;
    /** Ladder: rung plane beyond the cladding, half width between the rails, rung pitch, cage half width, cage start above the foot, hoop pitch, rails above the landing. */
    static final double STANDOFF = .2, LADDER_HALF = .225, RUNG = .25, CAGE_HALF = .35, CAGE_START = 2.2, HOOP = 1, LANDING = 1.07;
    /** Collision walls: the handrail's band inside the deck edge, and the rung plane's face beyond the rung line, m. */
    static final double RAIL_BAND = .2, RUNG_FACE = .06;
    /** A fence's collision height over the deck, above the drawn top rail so a player cannot jump it, m. */
    static final double FENCE = 1.4;
    /**
     * Room a player takes across a deck, m: its collision box is 0.6 m square and stays aligned with the world, and walls
     * are sampled at cell-fraction centres, so a wall's voxels stand up to half a voxel proud of it; on the back diagonals,
     * where the ladders stand, the two together span (0.6 + 1/8)·√2 radially.
     */
    static final double PLAYER = (.6 + 1.0 / ShapeRasterizer.SOLID_RESOLUTION) * Math.sqrt(2);
    /**
     * Least deck width, m, to the next 5 cm: a player stands on the deck in front of the ladder that starts there, between
     * the rung plane and the handrail (1.45 m; plant platforms are 0.9 to 1.5 m wide, those carrying a caged ladder the widest).
     */
    static final double MIN_DECK = Math.ceil((STANDOFF + RUNG_FACE + PLAYER + RAIL_BAND - GAP) * 20) / 20;
    /** Manway: neck and cover diameters (24" class 150), cover face beyond the cladding, centre over the deck, m. */
    static final double MANWAY = .6, COVER = .8, MANWAY_REACH = .25, MANWAY_HEIGHT = .9;
    /** Rain flashing rings: pitch, band height and projection, m. */
    static final double FLASHING_PITCH = 3, FLASHING = 1 / 16.0;
    /** Platforms: at least this far apart, and no climb longer than {@link #MAX_CLIMB} (old OSHA 1910.27), m. */
    static final int MIN_PITCH = 3, MAX_CLIMB = 9;
    /** The two ladder positions (back-right and back-left diagonals) and the manways' (the back). */
    static final double LADDER_A = 3 * Math.PI / 4, LADDER_B = Math.PI / 4, BACK = Math.PI / 2;

    /** A platform: its deck from frame y {@code level} to {@code level + DECK}, over angles [from, to]; which ends carry a rail. */
    record Platform(int level, double from, double to, boolean ring, boolean closedFrom, boolean closedTo, boolean manway) {
        double manwayHeight() {
            return level + DECK + MANWAY_HEIGHT;
        }
    }

    /** A ladder at angle {@code angle}, from its foot at {@code base} to the top of its rails at {@code top}. */
    record Ladder(double angle, double base, double top) {
        boolean caged() {
            return top - base > CAGE_START + .5;
        }
    }

    /** The layout: platforms bottom to top (the last is the top ring), ladders (the first from the ground), the lifting davit. */
    record Plan(List<Platform> platforms, List<Ladder> ladders, double deckRadius, double davitAngle, double davitTop) {
        Platform top() {
            return platforms.getLast();
        }
    }

    private final EquipmentShape shape;
    private final Plan plan;
    private final double axis, outer, apothem, rung;
    private final int n;
    /** Each platform's arc in pieces (see {@link #pieces}), in the order of the plan's platforms. */
    private final List<List<double[]>> pieces = new ArrayList<>();

    ColumnStructure(EquipmentShape shape) {
        this.shape = shape;
        axis = shape.axis();
        outer = shape.outerRadius();
        n = shape.facets();
        apothem = outer * DeterministicMath.cos(Math.PI / n);
        rung = outer + STANDOFF;
        var nozzles = shape.nozzles();
        var planned = plan(shape);
        var platforms = new ArrayList<Platform>();
        for (var p : planned.platforms()) platforms.add(p.ring() ? p : carried(p, nozzles));
        plan = new Plan(List.copyOf(platforms), planned.ladders(), planned.deckRadius(), planned.davitAngle(), planned.davitTop());
        for (var p : plan.platforms()) pieces.add(pieces(p, nozzles));
    }

    /**
     * A part ring whose end lies beside a line to the auxiliaries crossing its rail: carried on past the line, so the line
     * passes through a gap in the rail as round the top ring, and the closed end, a radial fence, clears the line's flange
     * from the shell out. Lined nozzles leave on the sides and the front, away from the ladders over the back, so an end
     * carried on moves away from its ladder.
     */
    private Platform carried(Platform p, List<EquipmentShape.Nozzle> nozzles) {
        double from = p.from(), to = p.to(), span = p.to() - p.from();
        for (var nozzle : nozzles) {
            if (!crosses(p, nozzle)) continue;
            double s = Math.min(1, (PipeSizes.flange(nozzle.maxOd()) / 2 + .15) / (outer + GAP));
            double clear = DeterministicMath.atan2(s, Math.sqrt(1 - s * s));
            double u = heading(nozzle) - p.from();
            u -= 2 * Math.PI * Math.floor(u / (2 * Math.PI));
            if (Math.abs(u - span) <= clear) to = Math.max(to, p.from() + u + clear);
            else if (u <= clear) from = Math.min(from, p.from() + u - clear);
            else if (2 * Math.PI - u <= clear) from = Math.min(from, p.from() + u - 2 * Math.PI - clear);
        }
        return new Platform(p.level(), from, to, false, p.closedFrom(), p.closedTo(), p.manway());
    }

    /** Whether a lined side nozzle's line crosses a platform's edge within its drawn rail's height. */
    private boolean crosses(Platform p, EquipmentShape.Nozzle nozzle) {
        if (nozzle.axis() == 1 || !ColumnAuxiliaries.lined(shape, nozzle)) return false;
        double y = nozzle.base()[1], half = nozzle.maxOd() / 2 + .03;
        return y - half < p.level() + DECK + RAIL + BAR && y + half > p.level();
    }

    /** A side nozzle's heading in the frame, from +x towards +z. */
    private static double heading(EquipmentShape.Nozzle nozzle) {
        return nozzle.axis() == 0 ? (nozzle.sign() > 0 ? 0 : Math.PI) : (nozzle.sign() > 0 ? Math.PI / 2 : 3 * Math.PI / 2);
    }

    /**
     * The least outer edge of the decks, m from the axis: at least {@link #MIN_DECK} wide, and beyond the flange face of
     * every side nozzle (on the first block plane {@link EquipmentShape#STAND_OUT} past the cladding; the axis runs through
     * a cell centre), so a nozzle ends inside the handrail. The column's box is as wide as this edge and its rail.
     */
    static double minimumDeckRadius(EquipmentShape shape) {
        double outer = shape.outerRadius();
        return Math.max(outer + GAP + MIN_DECK, Math.ceil(outer + EquipmentShape.STAND_OUT + .5) - .5 + .15);
    }

    /** The decks' outer edge: the least one, widened (by 0.15 m at most) to reach into the box's outermost cells. */
    static double deckRadius(EquipmentShape shape) {
        return Math.max(minimumDeckRadius(shape), shape.width() / 2.0 - .95);
    }

    /** Half the angle a ladder and its cage take at the deck's inner edge, with room for a rail: where a deck ends beside a ladder. */
    static double clearance(EquipmentShape shape) {
        double across = CAGE_HALF + .2, out = shape.outerRadius() + STANDOFF - .05;
        return DeterministicMath.acos(out / Math.sqrt(out * out + across * across));
    }

    /**
     * Platforms at the manways (photo survey: at the top, the bottom, the feed and every 15 trays or fewer), each one deck
     * {@link #MANWAY_HEIGHT} under its manway, kept {@link #MIN_PITCH} apart from the top down; rest platforms split any
     * climb longer than {@link #MAX_CLIMB}. The top platform is a full ring; the others are part rings over the back.
     */
    static Plan plan(EquipmentShape s) {
        var t = s.trays();
        double d = s.diameter(), yB = s.bottomTangent(), yT = s.topTangent();
        double bottom = Math.max(yB + COVER / 2 + .1, yB + ColumnTrays.bottomSpace(d) - .7);
        double feed = yB + (t.feed() > t.trays() ? ColumnTrays.bottomSpace(d) - .7 : t.trayHeight(d, t.feed()) + .3);
        double top = Math.max(bottom, Math.min(yT - COVER / 2 - .05, yB + t.trayHeight(d, 1) + .6));
        var wanted = new TreeSet<Integer>();
        for (double y : new double[]{bottom, feed, top}) wanted.add(Math.max(1, (int) Math.round(y - DECK - MANWAY_HEIGHT)));
        var kept = new ArrayList<Integer>();
        for (int level : wanted.descendingSet()) if (kept.isEmpty() || kept.getLast() - level >= MIN_PITCH) kept.add(level);
        Collections.reverse(kept);
        var levels = new ArrayList<Integer>();
        int previous = 0;
        for (int level : kept) {
            int gap = level - previous, parts = (gap + MAX_CLIMB - 1) / MAX_CLIMB;
            for (int i = 1; i < parts; i++) levels.add(previous + (int) Math.round(gap * i / (double) parts));
            levels.add(level);
            previous = level;
        }
        double clear = clearance(s), lowestManway = kept.getFirst();
        var platforms = new ArrayList<Platform>();
        var ladders = new ArrayList<Ladder>();
        for (int k = 0; k < levels.size(); k++) {
            int level = levels.get(k);
            double in = k % 2 == 0 ? LADDER_A : LADDER_B, out = k % 2 == 0 ? LADDER_B : LADDER_A;
            double manwayY = level + DECK + MANWAY_HEIGHT;
            boolean manway = level >= lowestManway && manwayY >= yB + COVER / 2 + .05 && manwayY <= yT - COVER / 2 - .05;
            ladders.add(new Ladder(in, k == 0 ? 0 : levels.get(k - 1) + DECK, level + DECK + LANDING));
            if (k == levels.size() - 1) platforms.add(new Platform(level, in + clear, in + 2 * Math.PI - clear, true, false, false, manway));
            else if (in > out) platforms.add(new Platform(level, out - clear, in - clear, false, true, false, manway));
            else platforms.add(new Platform(level, in + clear, out + clear, false, false, true, manway));
        }
        double gapAt = levels.size() % 2 == 1 ? LADDER_A : LADDER_B;
        double davitAngle = gapAt == LADDER_A ? Math.PI / 6 : 5 * Math.PI / 6;
        // At least 4.5 m over the top deck and 1.2 m over the head, its top off a block boundary inside the box's top row.
        double davitTop = Math.ceil(Math.max(levels.getLast() + DECK + 3.9, s.top() + .6)) + .6;
        return new Plan(List.copyOf(platforms), List.copyOf(ladders), deckRadius(s), davitAngle, davitTop);
    }

    // ---- Nozzles ------------------------------------------------------------------------------------------------

    /**
     * The process nozzles (shape only: a column is not a fluid device yet), each centred on a cell row so its flange face
     * lies on a block face: the vapour outlet on the top head; on the front the feed over its tray and the bottoms just
     * over the bottom tangent line; on the right, the side of the auxiliaries' structure (owner decision 2026-10-16), the
     * reflux over tray 1 and each pumparound's draw under its draw tray and return over its return tray, whose lines run
     * to the structure; on the left the side draws under their trays, the stripping steam under its stage and a kettle
     * reboiler's vapour return between the sump and the bottom tray. Sizes follow the column diameter. A nozzle that would
     * cross another on its side or the top ring's deck moves up or down a row; the lined ones are placed first.
     */
    static void nozzles(EquipmentShape s, List<EquipmentShape.Nozzle> out) {
        var t = s.trays();
        double d = s.diameter(), a = s.axis(), yB = s.bottomTangent(), yT = s.topTangent();
        double apothem = s.outerRadius() * DeterministicMath.cos(Math.PI / s.facets());
        int topDeck = plan(s).top().level();
        double topOd = size(.4 * d, 1);
        double start = s.headSurface(topOd / 2) - .03;
        out.add(new EquipmentShape.Nozzle(EquipmentShape.Role.VAPOUR_OUTLET, new double[]{a, start, a}, 1, 1, Math.ceil(s.top() + EquipmentShape.STAND_OUT) - start, topOd, topOd));
        double sump = ColumnTrays.bottomSpace(d);
        var placed = new ArrayList<EquipmentShape.Nozzle>();
        side(s, placed, EquipmentShape.Role.REFLUX, yB + t.trayHeight(d, 1) + .45, 0, -1, size(d / 8, .5), apothem, topDeck);
        side(s, placed, EquipmentShape.Role.FEED, yB + (t.feed() > t.trays() ? sump - .3 : t.trayHeight(d, t.feed()) + .3), 2, -1, size(d / 5, .8), apothem, topDeck);
        for (var p : t.pumparounds()) {
            side(s, placed, EquipmentShape.Role.PUMPAROUND_DRAW, yB + t.trayHeight(d, p.draw()) - .2, 0, -1, size(d / 6, .6), apothem, topDeck);
            side(s, placed, EquipmentShape.Role.RETURN, yB + t.trayHeight(d, p.ret()) + .3, 0, -1, size(d / 6, .6), apothem, topDeck);
        }
        if (s.options().reboiler() == ColumnOptions.Reboiler.KETTLE)
            side(s, placed, EquipmentShape.Role.VAPOUR_RETURN, ColumnAuxiliaries.vapourReturnHeight(d, yB), 0, 1, size(.27 * d, 1), apothem, topDeck);
        for (int draw : t.draws()) side(s, placed, EquipmentShape.Role.DRAW, yB + t.trayHeight(d, draw) - .2, 0, 1, size(d / 8, .5), apothem, topDeck);
        for (int stage : t.steam())
            side(s, placed, EquipmentShape.Role.STEAM, yB + (stage > t.trays() ? sump : t.trayHeight(d, stage)) - .3, 0, 1, size(d / 8, .5), apothem, topDeck);
        side(s, placed, EquipmentShape.Role.BOTTOMS, yB + .3, 2, -1, size(d / 6, .6), apothem, topDeck);
        out.addAll(placed);
    }

    /** A nozzle or line size from a fraction of the diameter: to the next 5 cm, at least 0.1 m, at most {@code max}. */
    static double size(double od, double max) {
        return Math.max(.1, Math.min(max, Math.round(od * 20) / 20.0));
    }

    /** Adds a radial nozzle at the free cell row nearest {@code y} on its side, or nothing when none is free within 4 m. */
    private static void side(EquipmentShape s, List<EquipmentShape.Nozzle> placed, EquipmentShape.Role role, double y, int axis, int sign, double od,
                             double apothem, int topDeck) {
        double half = PipeSizes.flange(od) / 2, yB = s.bottomTangent(), yT = s.topTangent();
        double wanted = Math.floor(y) + .5;
        for (int step = 0; step <= 8; step++) {
            double at = wanted + (step % 2 == 0 ? step / 2 : -(step + 1) / 2);
            if (at - half < yB + .05 || at + half > yT - .05) continue;
            if (at - half < topDeck + DECK + .02 && at + half > topDeck - .02) continue;
            boolean free = true;
            for (var other : placed)
                if (other.axis() == axis && other.sign() == sign && Math.abs(other.base()[1] - at) < half + PipeSizes.flange(other.maxOd()) / 2 + .05) free = false;
            if (!free) continue;
            placed.add(s.side(role, s.axis(), at, axis, sign, apothem, od, od));
            return;
        }
    }

    // ---- Mesh ---------------------------------------------------------------------------------------------------

    /** Draws the cladding's flashing rings, the platforms, the manways and the lifting davit into {@code mesh}, the ladders into {@code ladders}. */
    void build(Mesh mesh, Mesh ladders, List<EquipmentShape.Nozzle> nozzles) {
        flashing(mesh, nozzles);
        for (int i = 0; i < plan.platforms().size(); i++) {
            var p = plan.platforms().get(i);
            deck(mesh, p, pieces.get(i));
            handrail(mesh, p, pieces.get(i));
            if (p.manway()) manway(mesh, p.manwayHeight());
        }
        for (var l : plan.ladders()) ladder(ladders, l);
        davit(mesh);
    }

    /**
     * A platform's arc in pieces {from, to, railed}: railed 1, or 0 where a line to the auxiliaries crosses the deck's edge
     * within the handrail's height. There the toe board, the rails and the fence's collision stop at a post on each side, as
     * round a pipe through a plant handrail: the gap left beside the line is too narrow for a player.
     */
    private List<double[]> pieces(Platform p, List<EquipmentShape.Nozzle> nozzles) {
        double edge = plan.deckRadius();
        var gaps = new ArrayList<double[]>();
        for (var nozzle : nozzles) {
            if (!crosses(p, nozzle)) continue;
            double heading = heading(nozzle);
            double at = p.from() + (heading - p.from()) - 2 * Math.PI * Math.floor((heading - p.from()) / (2 * Math.PI));
            double w = (nozzle.maxOd() / 2 + .12) / edge;
            if (at - w > p.from() && at + w < p.to()) gaps.add(new double[]{at - w, at + w});
        }
        gaps.sort((g, h) -> Double.compare(g[0], h[0]));
        var out = new ArrayList<double[]>();
        double from = p.from();
        for (var gap : gaps) {
            if (gap[0] <= from) {
                from = Math.max(from, gap[1]);
                continue;
            }
            out.add(new double[]{from, gap[0], 1});
            out.add(new double[]{gap[0], gap[1], 0});
            from = gap[1];
        }
        out.add(new double[]{from, p.to(), 1});
        return out;
    }

    /** Rows of the cladding crossed by a flashing ring: their lower heights. */
    List<Double> flashingHeights() {
        var out = new ArrayList<Double>();
        for (double y = shape.bottomTangent() + FLASHING_PITCH + .5; y + FLASHING < shape.topTangent() - .25; y += FLASHING_PITCH) out.add(y);
        return out;
    }

    /** Heights a platform's deck, rails, braces and manway span. */
    List<double[]> platformSpans() {
        var out = new ArrayList<double[]>();
        for (var p : plan.platforms()) out.add(new double[]{p.level() - .7, p.level() + DECK + 1.45});
        return out;
    }

    Plan plan() {
        return plan;
    }

    private void flashing(Mesh m, List<EquipmentShape.Nozzle> nozzles) {
        double r = outer + FLASHING;
        for (double y : flashingHeights())
            for (int i = 0; i < n; i++) {
                double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
                if (crossed((a0 + a1) / 2, y + FLASHING / 2, nozzles)) continue;
                var outward = radial((a0 + a1) / 2);
                m.flat(List.of(at(r, a0, y), at(r, a1, y), at(r, a1, y + FLASHING), at(r, a0, y + FLASHING)), outward, Material.CLADDING);
                m.flat(List.of(at(outer, a0, y + FLASHING), at(outer, a1, y + FLASHING), at(r, a1, y + FLASHING), at(r, a0, y + FLASHING)), UP, Material.CLADDING);
                m.flat(List.of(at(outer, a0, y), at(outer, a1, y), at(r, a1, y), at(r, a0, y)), DOWN, Material.CLADDING);
            }
    }

    /** Whether a side nozzle or a manway passes through the cladding at angle {@code angle} and height {@code y}: the ring stops there. */
    private boolean crossed(double angle, double y, List<EquipmentShape.Nozzle> nozzles) {
        double half = Math.PI / n;
        for (var nozzle : nozzles) {
            if (nozzle.axis() == 1) continue;
            double reach = PipeSizes.flange(nozzle.maxOd()) / 2 + .05;
            double heading = nozzle.axis() == 0 ? (nozzle.sign() > 0 ? 0 : Math.PI) : (nozzle.sign() > 0 ? Math.PI / 2 : 3 * Math.PI / 2);
            if (Math.abs(nozzle.base()[1] - y) < reach && apart(angle, heading) < reach / outer + half) return true;
        }
        for (var p : plan.platforms())
            if (p.manway() && Math.abs(p.manwayHeight() - y) < COVER / 2 + .05 && apart(angle, BACK) < (COVER / 2 + .05) / outer + half) return true;
        return false;
    }

    private static double apart(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return Math.min(d, 2 * Math.PI - d);
    }

    /**
     * A deck of grating with its toe board (stopping at a line's gap), inner rim and ends, and a knee-braced bracket at
     * every post.
     */
    private void deck(Mesh m, Platform p, List<double[]> pieces) {
        double y0 = p.level(), y1 = y0 + DECK, inner = outer + GAP, edge = plan.deckRadius(), toe = edge - 1 / 32.0;
        for (var piece : pieces) {
            boolean railed = piece[2] > 0;
            int segments = segments(piece[0], piece[1]);
            for (int j = 0; j < segments; j++) {
                double a0 = piece[0] + (piece[1] - piece[0]) * j / segments, a1 = piece[0] + (piece[1] - piece[0]) * (j + 1) / segments, mid = (a0 + a1) / 2;
                double top = railed ? y1 + TOE : y1;
                m.flat(List.of(at(inner, a0, y1), at(inner, a1, y1), at(edge, a1, y1), at(edge, a0, y1)), UP, Material.GRATING);
                m.flat(List.of(at(inner, a0, y0), at(inner, a1, y0), at(edge, a1, y0), at(edge, a0, y0)), DOWN, Material.GRATING);
                m.flat(List.of(at(inner, a0, y0), at(inner, a1, y0), at(inner, a1, y1), at(inner, a0, y1)), neg(radial(mid)), Material.GRATING);
                m.flat(List.of(at(edge, a0, y0), at(edge, a1, y0), at(edge, a1, top), at(edge, a0, top)), radial(mid), Material.HANDRAIL);
                if (!railed) continue;
                m.flat(List.of(at(toe, a0, y1), at(toe, a1, y1), at(toe, a1, y1 + TOE), at(toe, a0, y1 + TOE)), neg(radial(mid)), Material.HANDRAIL);
                m.flat(List.of(at(toe, a0, y1 + TOE), at(toe, a1, y1 + TOE), at(edge, a1, y1 + TOE), at(edge, a0, y1 + TOE)), UP, Material.HANDRAIL);
            }
            // The toe board's ends at a gap; the deck's own ends at the arc's.
            if (!railed) continue;
            for (double end : new double[]{piece[0], piece[1]}) {
                var along = end == piece[0] ? neg(tangent(end)) : tangent(end);
                m.flat(List.of(at(toe, end, y1), at(edge, end, y1), at(edge, end, y1 + TOE), at(toe, end, y1 + TOE)), along, Material.HANDRAIL);
            }
        }
        for (double end : new double[]{p.from(), p.to()}) {
            var along = end == p.from() ? neg(tangent(end)) : tangent(end);
            m.flat(List.of(at(inner, end, y0), at(edge, end, y0), at(edge, end, y1), at(inner, end, y1)), along, Material.GRATING);
        }
        for (double a : posts(p.from(), p.to())) {
            m.bar(at(outer + .02, a, y0 - .04), at(edge - .05, a, y0 - .04), BAR * 1.5, UP, Material.STEELWORK);
            m.bar(at(outer + .02, a, y0 - .6), at(outer + .02 + .6 * (edge - outer), a, y0 - .07), BAR, radial(a), Material.STEELWORK);
        }
    }

    /** Posts along the outer edge, at a gap's sides and at closed ends; top and mid rails along the edge and across a closed end. */
    private void handrail(Mesh m, Platform p, List<double[]> pieces) {
        double base = p.level() + DECK, r = plan.deckRadius() - .03, inner = outer + .12;
        for (var piece : pieces) {
            if (piece[2] <= 0) continue;
            for (double a : posts(piece[0], piece[1])) m.bar(at(r, a, base), at(r, a, base + RAIL + BAR / 2), BAR, radial(a), Material.HANDRAIL);
            int segments = segments(piece[0], piece[1]);
            for (int j = 0; j < segments; j++) {
                double a0 = piece[0] + (piece[1] - piece[0]) * j / segments, a1 = piece[0] + (piece[1] - piece[0]) * (j + 1) / segments;
                for (double h : new double[]{RAIL, MID_RAIL}) m.bar(at(r, a0, base + h), at(r, a1, base + h), BAR, UP, Material.HANDRAIL);
            }
        }
        for (int e = 0; e < 2; e++) {
            if (!(e == 0 ? p.closedFrom() : p.closedTo())) continue;
            double a = e == 0 ? p.from() : p.to();
            m.bar(at(inner, a, base), at(inner, a, base + RAIL + BAR / 2), BAR, radial(a), Material.HANDRAIL);
            for (double h : new double[]{RAIL, MID_RAIL}) m.bar(at(inner, a, base + h), at(r, a, base + h), BAR, UP, Material.HANDRAIL);
        }
    }

    /** Segments of an arc of the deck's edge, at most 0.25 m each. */
    private int segments(double from, double to) {
        return Math.max(2, (int) Math.ceil((to - from) * plan.deckRadius() / .25));
    }

    /** Post angles over an arc: both ends and every {@link #POSTS} or less between. */
    private List<Double> posts(double from, double to) {
        int count = Math.max(1, (int) Math.ceil((to - from) * plan.deckRadius() / POSTS));
        var out = new ArrayList<Double>();
        for (int j = 0; j <= count; j++) out.add(from + (to - from) * j / count);
        return out;
    }

    /**
     * A manway: a 24" neck through the cladding, its flange and a painted blind cover, and the hinge davit beside it (a
     * post on two lugs, an arm over the cover centre, a drop to the cover).
     */
    private void manway(Mesh m, double y) {
        var f = new Frame(new double[]{axis, y, axis}, new double[]{0, 0, 1}, new double[]{1, 0, 0});
        double r = MANWAY / 2, cover = COVER / 2, start = Math.sqrt(apothem * apothem - r * r) - .03, face = outer + MANWAY_REACH, t = .06;
        m.prism(f, r, start, face - t, 16, Material.PIPE);
        m.annulus(f, r, cover, face - t, 16, -1, Material.FLANGE);
        m.prism(f, cover, face - t, face, 16, Material.FLANGE);
        m.prism(f, cover, face, face + t, 16, Material.ACTUATOR);
        m.annulus(f, 0, cover, face + t, 16, 1, Material.ACTUATOR);
        double post = -(cover + .08), postR = outer + .15;
        m.bar(at(postR, BACK, post, y - .35), at(postR, BACK, post, y + .58), BAR * 1.5, radial(BACK), Material.STEELWORK);
        for (double h : new double[]{-.22, .22}) m.bar(at(outer + .02, BACK, -r - .02, y + h), at(postR, BACK, post, y + h), BAR, UP, Material.STEELWORK);
        m.bar(at(postR, BACK, post, y + .55), at(face + t / 2, BACK, 0, y + .55), BAR * 1.5, UP, Material.STEELWORK);
        m.bar(at(face + t / 2, BACK, 0, y + .55), at(face + t / 2, BACK, 0, y + cover - .02), BAR, radial(BACK), Material.STEELWORK);
    }

    /**
     * A caged ladder: two rails stood off the cladding on brackets, rungs every {@link #RUNG} up to the landing, and from
     * {@link #CAGE_START} over the foot a cage of hoops every {@link #HOOP} with five vertical bars, to the rails' top.
     */
    private void ladder(Mesh m, Ladder l) {
        double a = l.angle(), landing = l.top() - LANDING;
        for (double side : new double[]{-LADDER_HALF, LADDER_HALF}) {
            m.bar(at(rung, a, side, l.base()), at(rung, a, side, l.top()), BAR, radial(a), Material.HANDRAIL);
            for (double y = l.base() + 1; y < l.top() - .3; y += 2)
                m.bar(at(outer + .01, a, side, y), at(rung - BAR / 2, a, side, y), BAR, UP, Material.STEELWORK);
        }
        for (double y = l.base() + RUNG; y <= landing + 1e-9; y += RUNG)
            m.bar(at(rung, a, -LADDER_HALF, y), at(rung, a, LADDER_HALF, y), BAR * .8, UP, Material.HANDRAIL);
        if (!l.caged()) return;
        var hoop = hoop();
        double first = l.base() + CAGE_START;
        for (double y = first; y <= l.top() + 1e-9; y = y + HOOP > l.top() && y < l.top() - .2 ? l.top() : y + HOOP)
            for (int k = 0; k + 1 < hoop.size(); k++)
                m.bar(at(rung + hoop.get(k)[0], a, hoop.get(k)[1], y), at(rung + hoop.get(k + 1)[0], a, hoop.get(k + 1)[1], y), BAR, UP, Material.HANDRAIL);
        for (int k : new int[]{2, 3, 4, 5, 6})
            m.bar(at(rung + hoop.get(k)[0], a, hoop.get(k)[1], first), at(rung + hoop.get(k)[0], a, hoop.get(k)[1], l.top()), BAR, radial(a), Material.HANDRAIL);
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

    /** The lifting davit on the top ring: a mast on a base plate, a jib out past the deck with a knee brace, a cable and a hook block. */
    private void davit(Mesh m) {
        double a = plan.davitAngle(), r = outer + .35, base = plan.top().level() + DECK, top = plan.davitTop();
        double reach = Math.min(plan.deckRadius() + .5, (axis - .25) / Math.max(Math.abs(DeterministicMath.cos(a)), Math.abs(DeterministicMath.sin(a))));
        var f = new Frame(at(r, a, base), UP, radial(a));
        m.prism(f, .1, 0, top - base, 8, Material.HANDRAIL);
        m.annulus(f, 0, .1, top - base, 8, 1, Material.HANDRAIL);
        m.block(at(r, a, base + .02), scale(radial(a), .18), new double[]{0, .02, 0}, scale(tangent(a), .18), Material.STEELWORK);
        m.bar(at(r - .1, a, top - .1), at(reach, a, top - .1), .14, UP, Material.HANDRAIL);
        m.bar(at(r, a, top - 1.1), at(r + .6 * (reach - r), a, top - .17), .07, tangent(a), Material.HANDRAIL);
        m.bar(at(reach - .05, a, top - .17), at(reach - .05, a, top - 1), .025, radial(a), Material.STEELWORK);
        m.block(at(reach - .05, a, top - 1.07), scale(radial(a), .07), new double[]{0, .07, 0}, scale(tangent(a), .07), Material.STEELWORK);
    }

    // ---- Solids -------------------------------------------------------------------------------------------------

    /**
     * Whether a frame point lies in a part a player collides with: a deck (walkable at {@link #DECK}), a handrail or a
     * closed end as a fence {@link #FENCE} high, a ladder's rung plane (a player climbs by pressing against it), a manway,
     * or the davit's mast. Walls are 0.2 m thick so the collision sampling cannot miss them; cages and braces do not collide.
     */
    boolean inside(double[] p) {
        return inside(p, FENCE);
    }

    /** As {@link #inside}, the fences to their top rail only: the parts drawn (tests: a line through the structure). */
    boolean insideDrawn(double[] p) {
        return inside(p, RAIL + BAR);
    }

    private boolean inside(double[] p, double fence) {
        double dx = p[0] - axis, dz = p[2] - axis, y = p[1], rho = Math.sqrt(dx * dx + dz * dz), edge = plan.deckRadius();
        for (int i = 0; i < plan.platforms().size(); i++) {
            var platform = plan.platforms().get(i);
            double level = platform.level();
            if (y < level || y > level + DECK + fence) continue;
            boolean within = within(dx, dz, platform.from(), platform.to());
            if (within && rho >= outer + GAP && rho <= edge && (y <= level + DECK || rho >= edge - RAIL_BAND && railed(i, dx, dz))) return true;
            if (y > level + DECK)
                for (int e = 0; e < 2; e++) {
                    if (!(e == 0 ? platform.closedFrom() : platform.closedTo())) continue;
                    double a = e == 0 ? platform.from() : platform.to(), along = along(dx, dz, a), across = across(dx, dz, a);
                    if (along >= outer + GAP && along <= edge && Math.abs(across) <= .1) return true;
                }
        }
        for (var l : plan.ladders()) {
            double along = along(dx, dz, l.angle()), across = across(dx, dz, l.angle());
            if (y >= l.base() && y <= l.top() && along >= rung - .14 && along <= rung + RUNG_FACE && Math.abs(across) <= LADDER_HALF) return true;
        }
        for (var platform : plan.platforms()) {
            if (!platform.manway()) continue;
            double h = y - platform.manwayHeight(), face = outer + MANWAY_REACH;
            if (dz >= outer - .1 && dz <= face && dx * dx + h * h <= MANWAY * MANWAY / 4) return true;
            if (dz >= face - .06 && dz <= face + .06 && dx * dx + h * h <= COVER * COVER / 4) return true;
        }
        var mast = at(outer + .35, plan.davitAngle(), 0);
        double mx = p[0] - mast[0], mz = p[2] - mast[2];
        return y >= plan.top().level() && y <= plan.davitTop() && mx * mx + mz * mz <= .01;
    }

    /** Whether a frame point lies where a ladder is climbed: in front of its rungs, inside its cage, from its foot to its top. */
    boolean climbable(double[] p) {
        double dx = p[0] - axis, dz = p[2] - axis;
        for (var l : plan.ladders()) {
            double along = along(dx, dz, l.angle()), across = across(dx, dz, l.angle());
            if (p[1] >= l.base() - .1 && p[1] <= l.top() && along >= rung - .05 && along <= rung + 2 * CAGE_HALF + .1 && Math.abs(across) <= CAGE_HALF + .05) return true;
        }
        return false;
    }

    /** Whether the direction (dx, dz) from the axis lies in the arc [from, to] (counter-clockwise from +x towards +z). */
    /** Whether platform {@code i}'s rail stands at the heading of (dx, dz): outside the gaps left for lines. */
    private boolean railed(int i, double dx, double dz) {
        for (var piece : pieces.get(i))
            if (piece[2] == 0 && within(dx, dz, piece[0], piece[1])) return false;
        return true;
    }

    private static boolean within(double dx, double dz, double from, double to) {
        double c0 = DeterministicMath.cos(from), s0 = DeterministicMath.sin(from), c1 = DeterministicMath.cos(to), s1 = DeterministicMath.sin(to);
        boolean afterFrom = c0 * dz - s0 * dx >= 0, beforeTo = dx * s1 - dz * c1 >= 0;
        return to - from <= Math.PI ? afterFrom && beforeTo : afterFrom || beforeTo;
    }

    private static double along(double dx, double dz, double a) {
        return dx * DeterministicMath.cos(a) + dz * DeterministicMath.sin(a);
    }

    private static double across(double dx, double dz, double a) {
        return -dx * DeterministicMath.sin(a) + dz * DeterministicMath.cos(a);
    }

    // ---- Frame helpers ------------------------------------------------------------------------------------------

    private static final double[] UP = {0, 1, 0}, DOWN = {0, -1, 0};

    private double[] at(double r, double a, double y) {
        return at(r, a, 0, y);
    }

    /** The point {@code r} out from the axis towards angle {@code a}, {@code t} across (towards increasing angle), at height {@code y}. */
    private double[] at(double r, double a, double t, double y) {
        double c = DeterministicMath.cos(a), s = DeterministicMath.sin(a);
        return new double[]{axis + r * c - t * s, y, axis + r * s + t * c};
    }

    private static double[] radial(double a) {
        return new double[]{DeterministicMath.cos(a), 0, DeterministicMath.sin(a)};
    }

    private static double[] tangent(double a) {
        return new double[]{-DeterministicMath.sin(a), 0, DeterministicMath.cos(a)};
    }

    private static double[] scale(double[] v, double k) {
        return new double[]{v[0] * k, v[1] * k, v[2] * k};
    }
}
