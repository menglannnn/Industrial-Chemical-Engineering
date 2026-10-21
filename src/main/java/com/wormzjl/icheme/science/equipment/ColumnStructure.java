package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static com.wormzjl.icheme.science.equipment.Access.*;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/**
 * What a distillation column carries besides its shell (owner request 2026-10-16, batch
 * 2026-10-15-custom-equipment-rendering; photo survey section 2): the insulation's aluminium cladding with its rain
 * flashing rings, part-ring platforms at the manways (grating decks, handrails with mid-rails and toe boards, knee
 * braces), caged ladders between them, the manways with their hinge davits, a full ring at the top with the lifting davit,
 * and the process nozzles. Decks, handrails and ladders are drawn by {@link Access}, shared with the fired heaters.
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
    /** Least deck width, m: the kit's ({@link Access#MIN_DECK}), which the designs reach through the column's name too. */
    static final double MIN_DECK = Access.MIN_DECK;
    /** Manway: neck and cover diameters (24" class 150), cover face beyond the cladding, centre over the deck, m. */
    static final double MANWAY = Manway.LARGE, COVER = Manway.COVER_RATIO * MANWAY, MANWAY_REACH = Manway.REACH, MANWAY_HEIGHT = .9;
    /** Rain flashing rings: pitch, band height and projection, m. */
    static final double FLASHING_PITCH = 3, FLASHING = 1 / 16.0;
    /** Platforms: at least this far apart, and no climb longer than {@link #MAX_CLIMB} (the kit's {@link Access#MAX_CLIMB}, old OSHA 1910.27), m. */
    static final int MIN_PITCH = 3, MAX_CLIMB = Access.MAX_CLIMB;
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
            return Access.caged(base, top);
        }
    }

    /** The layout: platforms bottom to top (the last is the top ring), ladders (the first from the ground), the lifting davit. */
    record Plan(List<Platform> platforms, List<Ladder> ladders, double deckRadius, double davitAngle, double davitTop) {
        Platform top() {
            return platforms.getLast();
        }
    }

    private static final int CACHE = 64;
    /** The nozzles of each shape: every box and transform reads them, and placing them tests the structure row by row. */
    private static final Map<EquipmentShape, List<EquipmentShape.Nozzle>> NOZZLES = new ConcurrentHashMap<>();

    private final EquipmentShape shape;
    private final Plan plan;
    private final double axis, outer, apothem, rung;
    private final int n;
    /** The plan's ladders as a player meets them, the first from the ground. */
    private final List<Access.Climb> climbs = new ArrayList<>();

    ColumnStructure(EquipmentShape shape) {
        this.shape = shape;
        axis = shape.axis();
        outer = shape.outerRadius();
        n = shape.facets();
        apothem = outer * DeterministicMath.cos(Math.PI / n);
        rung = outer + STANDOFF;
        plan = plan(shape);
        for (var l : plan.ladders()) {
            var foot = at(rung, l.angle(), 0);
            var out = radial(l.angle());
            climbs.add(new Access.Climb(foot[0], foot[2], out[0], out[2], l.base(), l.top(), l.top()));
        }
    }

    List<Access.Climb> climbs() {
        return climbs;
    }

    /** A side nozzle's heading in the frame, from +x towards +z. */
    private static double heading(int axis, int sign) {
        return axis == 0 ? (sign > 0 ? 0 : Math.PI) : (sign > 0 ? Math.PI / 2 : 3 * Math.PI / 2);
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

    /** Half the angle a ladder and its cage take at the deck's inner edge, with room for a rail: where a deck ends beside a ladder ({@link Access#clearance}). */
    static double clearance(EquipmentShape shape) {
        return Access.clearance(shape.outerRadius());
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
     * The process nozzles at fixed sides (owner decision 2026-10-16: the player places the units and runs the pipes), each
     * centred on a cell row so its flange face lies on a block face: the vapour outlet on the top head; on the front,
     * towards the constructor and the pipe rack, the feed over its tray and the bottoms through the skirt's port (on the
     * shell just over the bottom tangent line where the skirt leaves it no room); on the right the reflux over tray 1 and
     * each pumparound's draw under its draw tray and return over its return tray; on the left the reboiler's vapour return
     * between the sump's level and the bottom tray, the side draws under their trays and the stripping steam under its
     * stage. Sizes follow the column diameter; each takes any pipe. A nozzle moves to the nearest row where it meets no
     * other nozzle on its side and leaves its neck and the block beyond its flange clear of the access structure
     * ({@link Clearance}), so a pipe can always meet it. Cached per shape.
     */
    static void nozzles(EquipmentShape s, List<EquipmentShape.Nozzle> out) {
        var cached = NOZZLES.get(s);
        if (cached == null) {
            if (NOZZLES.size() >= CACHE) NOZZLES.clear();
            cached = NOZZLES.computeIfAbsent(s, ColumnStructure::place);
        }
        out.addAll(cached);
    }

    private static List<EquipmentShape.Nozzle> place(EquipmentShape s) {
        var t = s.trays();
        double d = s.diameter(), a = s.axis(), yB = s.bottomTangent();
        // The top neck rooted under the head where a pipe's largest neck's edge meets it (owner decision 2026-10-21: a 1 m
        // pipe has a 1 m root).
        double topOd = size(.4 * d, 1), start = s.headSurface(PipeSizes.MAX_OUTSIDE / 2) - .03;
        var out = new ArrayList<EquipmentShape.Nozzle>();
        out.add(new EquipmentShape.Nozzle(EquipmentShape.Role.VAPOUR_OUTLET, new double[]{a, start, a}, 1, 1, Math.ceil(s.top() + EquipmentShape.STAND_OUT) - start, topOd, 0));
        double sump = ColumnTrays.bottomSpace(d);
        var clearance = new Clearance(s);
        var placed = new ArrayList<EquipmentShape.Nozzle>();
        side(s, clearance, placed, EquipmentShape.Role.REFLUX, yB + t.trayHeight(d, 1) + .45, 0, -1, size(d / 8, .5));
        side(s, clearance, placed, EquipmentShape.Role.FEED, yB + (t.feed() > t.trays() ? sump - .3 : t.trayHeight(d, t.feed()) + .3), 2, -1, size(d / 5, .8));
        for (var p : t.pumparounds()) {
            side(s, clearance, placed, EquipmentShape.Role.PUMPAROUND_DRAW, yB + t.trayHeight(d, p.draw()) - .2, 0, -1, size(d / 6, .6));
            side(s, clearance, placed, EquipmentShape.Role.RETURN, yB + t.trayHeight(d, p.ret()) + .3, 0, -1, size(d / 6, .6));
        }
        side(s, clearance, placed, EquipmentShape.Role.VAPOUR_RETURN, yB + .75 * sump, 0, 1, size(.27 * d, 1));
        for (int draw : t.draws()) side(s, clearance, placed, EquipmentShape.Role.DRAW, yB + t.trayHeight(d, draw) - .2, 0, 1, size(d / 8, .5));
        for (int stage : t.steam())
            side(s, clearance, placed, EquipmentShape.Role.STEAM, yB + (stage > t.trays() ? sump : t.trayHeight(d, stage)) - .3, 0, 1, size(d / 8, .5));
        var bottoms = s.skirtOutlet(EquipmentShape.Role.BOTTOMS, size(d / 6, .6));
        if (bottoms == null) side(s, clearance, placed, EquipmentShape.Role.BOTTOMS, yB + .3, 2, -1, size(d / 6, .6));
        out.addAll(placed);
        if (bottoms != null) out.add(bottoms);
        return List.copyOf(out);
    }

    /**
     * A nozzle or line size from a fraction of the diameter: to the next 5 cm, at least 0.1 m, at most {@code max} (which
     * wins: a neck narrower than 0.1 m, a small ejector's, defaults to its largest size).
     */
    static double size(double od, double max) {
        return Math.min(max, Math.max(.1, Math.round(od * 20) / 20.0));
    }

    /**
     * Adds a radial nozzle at the cell row nearest {@code y} on its side where it meets no other nozzle there, its pipe
     * block keeps one free block from every other ({@link EquipmentShape.Nozzle#crowds}), and the structure leaves its
     * neck and pipe block clear; nothing when the shell has no such row.
     */
    private static void side(EquipmentShape s, Clearance clearance, List<EquipmentShape.Nozzle> placed, EquipmentShape.Role role, double y, int axis, int sign,
                             double od) {
        double half = PipeSizes.flange(od) / 2, yB = s.bottomTangent(), yT = s.topTangent(), heading = heading(axis, sign);
        double wanted = Math.floor(y) + .5;
        int rows = (int) Math.ceil(yT - yB) + 2;
        for (int step = 0; step <= 2 * rows; step++) {
            double at = wanted + (step % 2 == 0 ? step / 2 : -(step + 1) / 2);
            if (at - half < yB + .05 || at + half > yT - .05) continue;
            var nozzle = s.side(role, s.axis(), at, axis, sign, s.outerRadius() * DeterministicMath.cos(Math.PI / s.facets()), od);
            boolean free = true;
            // Flanges on one side never overlap, and no two pipes stand in touching blocks (one free block between).
            for (var other : placed)
                if (other.axis() == axis && other.sign() == sign && Math.abs(other.base()[1] - at) < half + PipeSizes.flange(other.defaultOd()) / 2 + .05 || nozzle.crowds(other)) free = false;
            if (!free || clearance.blocked(Math.floor(at), heading)) continue;
            placed.add(nozzle);
            return;
        }
    }

    /**
     * What a side nozzle must keep clear of the access structure: from the cladding out to the far side of the block
     * beyond its flange face (where its pipe stands), {@link #REACH} to each side of its centre line, over its cell row.
     * The structure's parts there, seen from above: each platform's deck, rail and braces over its rows (from its knee
     * braces up to its top rail), each ladder with its cage between its foot and top, and the davit's mast. Each is a
     * convex outline (a deck in arcs of at most {@link #ARC}), tested against the clear box by separating axes.
     */
    private static final class Clearance {
        /** Half the width kept clear beside a nozzle's centre line: half a block and a margin. */
        static final double REACH = .55, ARC = Math.PI / 12;
        private final Plan plan;
        private final double outer, face;

        Clearance(EquipmentShape s) {
            plan = plan(s);
            outer = s.outerRadius();
            // The flange face's plane, from the axis (the axis runs through a cell centre).
            face = Math.ceil(s.axis() + outer + EquipmentShape.STAND_OUT) - s.axis();
        }

        /** Whether a nozzle in cell row [row, row + 1) heading {@code heading} meets a part. */
        boolean blocked(double row, double heading) {
            double[][] clear = {{outer, -REACH}, {face + 1, -REACH}, {face + 1, REACH}, {outer, REACH}};
            double edge = plan.deckRadius();
            for (var p : plan.platforms()) {
                if (row >= p.level() + DECK + RAIL + BAR || row + 1 <= p.level() - .65) continue;
                int pieces = Math.max(1, (int) Math.ceil((p.to() - p.from()) / ARC));
                for (int k = 0; k < pieces; k++) {
                    double a0 = p.from() + (p.to() - p.from()) * k / pieces, a1 = p.from() + (p.to() - p.from()) * (k + 1) / pieces;
                    var piece = new double[][]{local(outer, a0, 0, heading), local(edge, a0, 0, heading), local(edge, (a0 + a1) / 2, 0, heading),
                            local(edge, a1, 0, heading), local(outer, a1, 0, heading)};
                    if (overlap(clear, piece)) return true;
                }
            }
            for (var l : plan.ladders()) {
                if (row >= l.top() || row + 1 <= l.base()) continue;
                double r = outer + STANDOFF;
                var cage = new double[][]{local(outer, l.angle(), -CAGE_HALF, heading), local(r + 2 * CAGE_HALF, l.angle(), -CAGE_HALF, heading),
                        local(r + 2 * CAGE_HALF, l.angle(), CAGE_HALF, heading), local(outer, l.angle(), CAGE_HALF, heading)};
                if (overlap(clear, cage)) return true;
            }
            if (row + 1 > plan.top().level() && row < plan.davitTop()) {
                var mast = local(outer + .35, plan.davitAngle(), 0, heading);
                var square = new double[][]{{mast[0] - .1, mast[1] - .1}, {mast[0] + .1, mast[1] - .1}, {mast[0] + .1, mast[1] + .1}, {mast[0] - .1, mast[1] + .1}};
                return overlap(clear, square);
            }
            return false;
        }

        /** The point {@code r} out at angle {@code a}, {@code t} across, as (along, across) the heading {@code heading}. */
        private static double[] local(double r, double a, double t, double heading) {
            double d = a - heading, c = DeterministicMath.cos(d), s = DeterministicMath.sin(d);
            return new double[]{r * c - t * s, r * s + t * c};
        }

        /** Whether two convex outlines overlap: no edge normal of either separates them. */
        private static boolean overlap(double[][] a, double[][] b) {
            return !separates(a, b) && !separates(b, a);
        }

        private static boolean separates(double[][] edges, double[][] other) {
            for (int i = 0; i < edges.length; i++) {
                double[] p = edges[i], q = edges[(i + 1) % edges.length];
                double nx = q[1] - p[1], nz = p[0] - q[0];
                double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE, minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
                for (var v : edges) {
                    double d = v[0] * nx + v[1] * nz;
                    minA = Math.min(minA, d);
                    maxA = Math.max(maxA, d);
                }
                for (var v : other) {
                    double d = v[0] * nx + v[1] * nz;
                    minB = Math.min(minB, d);
                    maxB = Math.max(maxB, d);
                }
                if (maxA <= minB + 1e-9 || maxB <= minA + 1e-9) return true;
            }
            return false;
        }
    }

    // ---- Mesh ---------------------------------------------------------------------------------------------------

    /** Draws the cladding's flashing rings, the platforms, the manways and the lifting davit into {@code mesh}, the ladders into {@code ladders}. */
    void build(Mesh mesh, Mesh ladders, List<EquipmentShape.Nozzle> nozzles) {
        flashing(mesh, nozzles);
        for (var p : plan.platforms()) {
            var run = new Access.Arc(axis, axis, outer, p.from(), p.to(), plan.deckRadius(), Access.Arc.STEP);
            Access.deck(mesh, run, p.level(), plan.deckRadius() - outer, Access.ALL_PARTS);
            Access.handrail(mesh, run, p.level(), plan.deckRadius() - outer, p.closedFrom(), p.closedTo());
            if (p.manway()) manway(p.manwayHeight()).draw(mesh);
        }
        for (var climb : climbs) Access.ladder(ladders, climb, STANDOFF);
        davit(mesh);
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
            double reach = PipeSizes.flange(PipeSizes.MAX_OUTSIDE) / 2 + .05;
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

    /** The manway over a platform, at height {@code y}, facing the back ({@link Manway}): its 24" neck through the cladding. */
    private Manway manway(double y) {
        double r = MANWAY / 2;
        return new Manway(new double[]{axis, y, axis}, new double[]{0, 0, 1}, UP, MANWAY, Math.sqrt(apothem * apothem - r * r) - .03, outer, outer + MANWAY_REACH);
    }

    /** The lifting davit on the top ring ({@link Access#davit}): a mast on a base plate, a jib out past the deck with a knee brace, a cable and a hook block. */
    private void davit(Mesh m) {
        double a = plan.davitAngle(), r = outer + .35, base = plan.top().level() + DECK, top = plan.davitTop();
        double reach = Math.min(plan.deckRadius() + .5, (axis - .25) / Math.max(Math.abs(DeterministicMath.cos(a)), Math.abs(DeterministicMath.sin(a))));
        Access.davit(m, axis, axis, a, r, base, top, reach);
    }

    // ---- Solids -------------------------------------------------------------------------------------------------

    /**
     * Whether a frame point lies in a part a player collides with: a deck (walkable at {@link #DECK}), a handrail or a
     * closed end as a fence {@link #FENCE} high, a ladder's rung plane (a player climbs by pressing against it), a manway,
     * or the davit's mast. Walls are 0.2 m thick so the collision sampling cannot miss them; cages and braces do not collide.
     */
    boolean inside(double[] p) {
        double dx = p[0] - axis, dz = p[2] - axis, y = p[1], rho = Math.sqrt(dx * dx + dz * dz), edge = plan.deckRadius();
        for (var platform : plan.platforms()) {
            double level = platform.level();
            if (y < level || y > level + DECK + FENCE) continue;
            boolean within = within(dx, dz, platform.from(), platform.to());
            if (within && rho >= outer + GAP && rho <= edge && (y <= level + DECK || rho >= edge - RAIL_BAND)) return true;
            if (y > level + DECK)
                for (int e = 0; e < 2; e++) {
                    if (!(e == 0 ? platform.closedFrom() : platform.closedTo())) continue;
                    double a = e == 0 ? platform.from() : platform.to(), along = along(dx, dz, a), across = across(dx, dz, a);
                    if (along >= outer + GAP && along <= edge && Math.abs(across) <= RAIL_BAND / 2) return true;
                }
        }
        for (var climb : climbs) if (climb.rungs(p)) return true;
        for (var platform : plan.platforms()) {
            if (!platform.manway()) continue;
            if (manway(platform.manwayHeight()).inside(p)) return true;
        }
        var mast = at(outer + .35, plan.davitAngle(), 0);
        double mx = p[0] - mast[0], mz = p[2] - mast[2];
        return y >= plan.top().level() && y <= plan.davitTop() && mx * mx + mz * mz <= .01;
    }

    /** Whether a frame point lies where a ladder is climbed: in front of its rungs, inside its cage, from its foot to its top. */
    boolean climbable(double[] p) {
        for (var climb : climbs) if (climb.climbable(p)) return true;
        return false;
    }

    /** Whether the direction (dx, dz) from the axis lies in the arc [from, to] (counter-clockwise from +x towards +z). */
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
}
