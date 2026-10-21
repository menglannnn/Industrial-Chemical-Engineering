package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.icheme.science.equipment.Mesh.Frame;
import com.wormzjl.icheme.science.equipment.Mesh.Material;
import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.List;

/**
 * A storage sphere (owner request 2026-10-21, batch 2026-10-21-floating-roof-tanks, {@code FLOATING_ROOF_TANKS_PLAN.md}
 * section 3.5; concept sheet and photo survey approved 2026-10-21): a drum's physics in a sphere of diameter D, 4 to 24 m
 * in whole metres, its bottom {@code support} 2 to 8 m over grade. {@code volume() = π D³/6}, {@code levelHeight() = D}.
 *
 * <p>The sphere stands on column legs tangent to it at the equator, an even count of about one per 4.5 m of equator and
 * at least four, 0.05 D wide, their tops 1 m over the equator, with crossed tie rods in each bay below the equator (thin:
 * no collision). One stair wound round it (owner approval 2026-10-21) climbs from grade beside the front-right leg,
 * outside the legs below the equator and over the upper half, to a landing beside the top platform: a ring round the top
 * nozzle about 0.36 D across with its handrail, two relief valves with 2 m vertical stacks and a manway. Its ports are
 * fixed: N1 VAPOUR at the top pole, its flange facing up over the platform, and N2 LIQUID from the bottom pole, its line
 * dropping, turning to the front and ending at the front edge on the lowest row between the two front legs, as a skirted
 * drum's bottom outlet.
 */
final class SphereDesign extends UnitDesign {
    /** The legs: their width over D (the photo survey's 0.05 D), the least, and their tops over the equator, m. */
    static final double LEG = .05, LEAST_LEG = .2, LEG_TOP = 1;
    /** Equator per leg, m (the concept sheet: about one leg per 4.5 m). */
    static final double LEG_SPACING = 4.5;
    /** The top platform's diameter over D, its least radius, its deck over the top pole, m. */
    static final double PLATFORM = .36, LEAST_PLATFORM = 1.25, CROWN = .1;
    /** The stair's height over the upper half's surface at its inner edge, and where it leaves the legs over the equator, m. */
    static final double OVER_SURFACE = .45, LEAVES_LEGS = 1.5;
    /**
     * Where the stair leaves the legs, it comes in towards the axis {@code RAMP} m for each metre it climbs, to the upper
     * half's surface; it runs at the platform's radius from {@code LEVEL_RUN} m under the platform's walking level, so its
     * last treads meet the landing square (walk test 2026-10-21: still coming in, they ran into the landing's rail).
     */
    static final double RAMP = 2, LEVEL_RUN = .5;
    /** The landing at the stair's top, along the stair, m. */
    static final double LANDING = 1.4;
    /** The relief valves' stacks over the platform's walking surface, m (photo survey: 2 m vertical stacks). */
    static final double STACK = 2;

    @Override
    public double minSize() {
        return 4;
    }

    @Override
    public double maxSize() {
        return 24;
    }

    @Override
    public double sizeStep() {
        return 1;
    }

    /** The clearance under the sphere: 2 to 8 m. */
    @Override
    public int minSupport(double size, int length) {
        return 2;
    }

    @Override
    public int maxSupport() {
        return 8;
    }

    @Override
    public Orientation orientation() {
        return Orientation.VERTICAL;
    }

    @Override
    public String sizeRow() {
        return "sphere.diameter";
    }

    @Override
    public String supportRow() {
        return "sphere.support";
    }

    @Override
    public Line held(EquipmentShape shape) {
        return new Line("sphere.held", whole(shape.volume()));
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        var p = Plan.of(shape.diameter(), shape.support());
        return List.of(new Line("sphere", size(p.d), p.n, size(p.wl)), new Line("sphere.access", number(p.level + Access.DECK)));
    }

    /**
     * A sphere's sizes, m, in its frame (y up from grade, front -z): its diameter, radius, clearance under it and centre
     * height, the legs' count, width and radius, the axis (x and z), the platform's radius and deck level, the stair's
     * outer radius, and the nozzles' largest and default sizes.
     */
    record Plan(double d, double r, int s, double yc, int n, double wl, double legs, double a, double rp, double level, double outer, double topOd, double topDefault,
                double bottomOd, double bottomDefault) {
        static Plan of(double d, int s) {
            double r = d / 2, wl = Math.max(LEAST_LEG, LEG * d), outer = r + wl + Access.GAP + Access.WOUND_WIDTH;
            int n = Math.max(4, 2 * (int) Math.round(Math.PI * d / (2 * LEG_SPACING)));
            double topOd = PipeSizes.od(.1 + .02 * d, .2, .6), bottomOd = PipeSizes.od(.1 + .015 * d, .2, .5);
            // The platform reaches out to where the stair stands half a metre under its walking level.
            double rp = Math.max(Math.max(PLATFORM * d / 2, LEAST_PLATFORM), around(s + d + CROWN + Access.DECK - LEVEL_RUN, r, wl, s + r));
            return new Plan(d, r, s, s + r, n, wl, r + wl / 2, ProcessUnit.centre(outer + .05), rp, s + d + CROWN, outer,
                    topOd, PipeSizes.od(.06 + .012 * d, .15, topOd), bottomOd, PipeSizes.od(.06 + .01 * d, .15, bottomOd));
        }

        double[] at(double rho, double t, double y) {
            return Access.around(a, a, rho, t, y);
        }

        /** Leg {@code i}'s angle: the front's middle (-π/2) between legs 0 and n - 1. */
        double leg(int i) {
            return -Math.PI / 2 + (i + .5) * 2 * Math.PI / n;
        }

        /** The sphere's radius at height y, 0 above or under it. */
        double radiusAt(double y) {
            double h = y - yc;
            return Math.sqrt(Math.max(0, r * r - h * h));
        }

        /** The sphere's height over its centre at radius {@code rho} from the axis (its upper surface). */
        double heightAt(double rho) {
            return Math.sqrt(Math.max(0, r * r - rho * rho));
        }
    }

    private static final double[] UP = {0, 1, 0};

    @Override
    void build(ProcessUnit u) {
        var p = Plan.of(u.shape.diameter(), u.shape.support());
        sphere(u, p);
        legs(u, p);
        var stair = u.woundStair(stair(p));
        double end = stair.angle(stair.steps()), span = LANDING / (p.rp + Access.GAP + Access.WOUND_WIDTH / 2);
        crown(u, p, end, span);
        u.nozzles.add(top(p));
        u.nozzles.add(bottom(p));
        u.fluidPoint = new double[]{p.a, p.s + .01, p.a};
    }

    /** The sphere, its solid and its inside (a drum's), outlined round its equator. */
    private static void sphere(ProcessUnit u, Plan p) {
        var centre = new double[]{p.a, p.yc, p.a};
        u.mesh.sphere(centre, p.r, 4 * (int) Math.ceil(Math.PI * p.d / 2.4), Material.HEAD);
        u.solids.add(ProcessUnit.Solid.rod(centre, 1, p.yc, p.yc, p.r, p.r, p.r));
        u.volumes.add(ProcessUnit.Solid.rod(centre, 1, p.yc, p.yc, p.r, p.r, p.r));
        u.round(p.a, p.a, p.yc, p.yc, p.r, 1);
    }

    /**
     * The legs, tangent to the sphere at the equator, on base plates, capped 1 m over the equator, and in each bay below
     * the equator a pair of crossed tie rods, their upper ends low enough that the rod between two legs stays clear of
     * the sphere.
     */
    private static void legs(ProcessUnit u, Plan p) {
        double top = p.yc + LEG_TOP, half = p.wl / 2;
        double chord = p.legs * DeterministicMath.cos(Math.PI / p.n) - Access.GAP, high = Math.min(p.yc - .5, p.yc - p.heightAt(chord)), low = .6;
        for (int i = 0; i < p.n; i++) {
            double t = p.leg(i);
            var foot = p.at(p.legs, t, 0);
            var up = Frame.of(foot, 1, 1);
            u.mesh.cylinder(up, half, 0, top, 12, Material.SUPPORT);
            u.mesh.box(new double[]{foot[0] - half - .04, 0, foot[2] - half - .04}, new double[]{foot[0] + half + .04, .04, foot[2] + half + .04}, Mesh.ALL_FACES, Material.SUPPORT);
            u.solids.add(ProcessUnit.Solid.rod(foot, 1, 0, top, half, 0));
            u.segment(new double[]{foot[0], 0, foot[2]}, new double[]{foot[0], top, foot[2]});
            if (high < low + 1) continue;
            var next = p.at(p.legs, p.leg(i + 1), 0);
            u.mesh.bar(new double[]{foot[0], low, foot[2]}, new double[]{next[0], high, next[2]}, .04, UP, Material.STEELWORK);
            u.mesh.bar(new double[]{foot[0], high, foot[2]}, new double[]{next[0], low, next[2]}, .04, UP, Material.STEELWORK);
        }
    }

    /**
     * The stair from grade beside the front-right leg (seen from the front), wound clockwise seen from above: outside the
     * legs up to 1.5 m over the equator, then in towards the axis and over the upper half at
     * {@link #OVER_SURFACE} over its surface, round the platform's edge at the last, to the platform's walking level.
     */
    /**
     * What the stair winds round at height y, but the platform: the legs up to {@link #LEAVES_LEGS} over the equator,
     * then in at {@link #RAMP} to the upper half {@link #OVER_SURFACE} under it (a sphere of radius r, centre yc).
     */
    static double around(double y, double r, double wl, double yc) {
        double h = y - OVER_SURFACE - yc;
        return Math.max(r + wl - RAMP * Math.max(0, y - yc - LEAVES_LEGS), Math.sqrt(Math.max(0, r * r - h * h)));
    }

    static Access.WoundStair stair(Plan p) {
        return Access.WoundStair.wind(p.a, p.a, y -> Math.max(around(y, p.r, p.wl, p.yc), p.rp), Access.GAP,
                -Math.PI / 2 - Math.PI / p.n, -1, 0, p.level + Access.DECK, Access.WOUND_WIDTH, Access.WOUND_SLOPE, List.of(), false);
    }

    /**
     * The top platform: a ring deck round the top nozzle from its flange out to {@link Plan#rp}, railed round but where the
     * landing joins it; the landing over the stair's band beyond the stair's top ({@code end} to {@code end - span}), railed
     * round but towards the stair and the ring; braces under both to the sphere; the two relief valves and their stacks,
     * and the manway, on the ring away from the landing.
     */
    private static void crown(ProcessUnit u, Plan p, double end, double span) {
        double inner = PipeSizes.flange(p.topOd) / 2 + .02, wall = inner - Access.GAP, width = p.rp - wall, deck = p.level + Access.DECK, mid = end - span / 2;
        var ring = new Access.Arc(p.a, p.a, wall, mid - Math.PI, mid + Math.PI, p.rp, Access.Arc.step(p.rp));
        double half = span / 2 * p.rp + .05, centre = Math.PI * p.rp;
        u.platform(ring, p.level, width, Access.RAILED, false, false, centre - half, centre + half);
        double edge = p.rp + 2 * Access.GAP + Access.WOUND_WIDTH;
        var landing = new Access.Arc(p.a, p.a, p.rp - Access.GAP, end - span, end, edge, Access.Arc.step(edge));
        u.platform(landing, p.level, edge - p.rp + Access.GAP, Access.RAILED, true, false);
        // The ring's and the landing's braces to the sphere.
        int count = Math.max(3, (int) Math.round(2 * Math.PI * p.rp / 2));
        for (int i = 0; i < count; i++) {
            double t = mid + Math.PI * (2 * i + 1) / count, rho = p.rp - .3;
            u.mesh.bar(p.at(rho, t, p.yc + p.heightAt(rho) - .05), p.at(rho, t, p.level), .08, new double[]{1, 0, 0}, Material.STEELWORK);
        }
        for (double t : new double[]{end - span + .1 / edge, end - .1 / edge}) {
            double rho = Math.min(p.rp + .2, .98 * p.r);
            u.mesh.bar(p.at(rho, t, p.yc + p.heightAt(rho) - .05), p.at(edge - .1, t, p.level - .02), .08, UP, Material.STEELWORK);
        }
        // The relief valves on the ring, their stacks up; the manway's cover, a step on the deck.
        double rv = Math.max(inner + .3, p.rp - .45);
        for (double t : new double[]{mid + 2 * Math.PI / 3, mid - 2 * Math.PI / 3}) {
            var foot = p.at(rv, t, deck);
            u.mesh.cylinder(Frame.of(foot, 1, 1), .12, 0, .55, 12, Material.MACHINE);
            u.mesh.cylinder(Frame.of(foot, 1, 1), .07, .55, STACK, 8, Material.PIPE);
            u.solids.add(ProcessUnit.Solid.rod(foot, 1, deck, deck + .55, .12, 0));
        }
        double rm = Math.max(inner + .4, (inner + p.rp) / 2);
        var manway = p.at(rm, mid + Math.PI, deck);
        var cover = Frame.of(manway, 1, 1);
        u.mesh.cylinder(cover, .36, -.4, .1, 16, Material.FLANGE);
        u.mesh.bolts(cover, .6, .3, .36, .1, 1, Material.BOLT);
        u.solids.add(ProcessUnit.Solid.rod(manway, 1, deck - .4, deck + .1, .36, 0));
    }

    /**
     * N1, VAPOUR: at the top pole, facing up, its neck from under the shell at its largest size's edge, its flange face on
     * the block face 0.4 m or more over the platform's walking surface.
     */
    private static Nozzle top(Plan p) {
        double surface = p.yc + p.r, start = p.yc + Math.sqrt(p.r * p.r - p.topOd * p.topOd / 4) * apothem(p) - .03;
        double plane = Math.ceil(p.level + Access.DECK + .4 + .03);
        return new Nozzle(Role.VAPOUR_OUTLET, new double[]{p.a, start, p.a}, 1, 1, plane - start, p.topOd, p.topDefault, 0, surface - start);
    }

    /**
     * N2, LIQUID: from the bottom pole, as a skirted drum's bottom outlet: the riser drops from the pole, the elbow turns it
     * to the front, the line runs on the lowest row between the two front legs to its flange face on the box's front edge.
     */
    private static Nozzle bottom(Plan p) {
        double y = .5, base = p.a - p.bottomOd, plane = Math.floor(p.a - p.outer - .03 + 1e-9);
        double pole = p.s + p.r * (1 - apothem(p)) + .05;
        return new Nozzle(Role.LIQUID_OUTLET, new double[]{p.a, y, base}, 2, -1, base - plane, p.bottomOd, p.bottomDefault, pole, p.r + p.wl + Access.GAP - p.bottomOd);
    }

    /** How far the drawn sphere's facets stand inside its radius at a pole, as a share of it. */
    private static double apothem(Plan p) {
        int n = 4 * (int) Math.ceil(Math.PI * p.d / 2.4);
        return DeterministicMath.cos(Math.PI / n) * DeterministicMath.cos(2 * Math.PI / n);
    }
}
