package com.wormzjl.createcheme.science.equipment;

import java.util.List;

import static com.wormzjl.createcheme.science.equipment.DelayedCokerPlan.*;
import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * A delayed coker's derricks and decoking gear ({@link DelayedCokerPlan}; {@code NEW_UNITS_SURVEY_FCC_COKER.md}
 * section 3: a lattice derrick per drum on the cutting deck, crown block, crosshead on guide rails, drill stem,
 * high-pressure hose loop, standpipe; derrick patent: about 40 m).
 *
 * <p>Each derrick: four straight legs at the corners of its 3.5 m square ({@link DelayedCokerPlan#DERRICK}), struts at
 * stories three sides high (four stories at least) and one diagonal a face a story, alternating (Warren bracing; the
 * front face's lowest story left open as the way in), struts and diagonals flat bars; the crown frame and sheave housing
 * on top, the hoist's drum and its motor (the kit's {@link ProcessUnit#motor}, plain) beside the sheaves; two guide
 * rails 0.6 m either side of the axis, tied to the struts. The cutting water line's standpipe rises 0.9 m beside the axis
 * to a gooseneck, the hose (a kit {@link ProcessUnit#pipe} that does not collide) hangs from it in a loop to the swivel
 * on the crosshead; under the swivel the travelling block and its ropes up to the crown, under the crosshead the drill
 * stem.
 *
 * <p>The second drum of each pair is being cut: its crosshead, swivel, block, stem, the hose's loop and the ropes' ends
 * ride a slider-crank up and down a 4 m band ({@link DelayedCokerPlan#CRANK}, about two minutes a cycle; the pairs half
 * a cycle apart). The parts it leaves behind hide its ends over the whole stroke: the stem's foot stays in the top
 * unheading device and the drum, the hose's and ropes' moving pieces are thinner than the still ones they slide into. The
 * first drum's crosshead is parked at the top, the stem withdrawn with its cutting tool clear of the deck.
 */
final class DelayedCokerDerrick {
    /** Leg, strut and brace sections; the guide rails' offset from the axis and section, m. */
    static final double LEG = .3, STRUT = .15, BRACE = .12, GUIDE = .6, RAIL = .15;
    /** A story's height in derrick sides (4 stories at least). */
    static final double STORY = 3;
    /** The crown frame's and the sheave housing's heights, m. */
    static final double CROWN_FRAME = .35, SHEAVES = .75;
    /** The crosshead: half length along the rails' line, half depth, half height; the swivel's radius and top; the block's top over the crosshead, m. */
    static final double HEAD = .5, HEAD_DEPTH = .3, HEAD_HALF = .25, SWIVEL = .18, SWIVEL_TOP = .75, BLOCK_TOP = 1.45;
    /** The drill stem's and the cutting tool's diameters, the hose's and wire ropes' (the moving pieces a little thinner), m. */
    static final double STEM = .2, TOOL = .4, HOSE = .18, HOSE_IN = .15, ROPE = .07, ROPE_IN = .055;
    /** The drill stem's sides: a thin round part, hexagonal. */
    static final int STEM_SIDES = 6;
    /** The standpipe's offset +x from the axis; the hose's swivel leg and loop: its leg's offset -z, the loop's depth under the crosshead, m. */
    static final double STANDPIPE = .9, SWIVEL_LEG = .42, LOOP = 3.5;
    /** The highest and lowest offsets of the slider-crank from where its part is drawn, m (offset(0), -offset(0.5)). */
    static final double UP_STROKE = 2.15, DOWN_STROKE = 1.95;

    private DelayedCokerDerrick() {
    }

    static void draw(ProcessUnit u, DelayedCokerPlan p) {
        for (int i = 0; i < p.drums; i++) {
            lattice(u, p, i);
            if (i % 2 == 1) cutting(u, p, i);
            else parked(u, p, i);
        }
    }

    /**
     * A diagonal brace or a strut from {@code a} to {@code b} in a face of the lattice facing {@code out}: a flat bar
     * {@code width} wide, drawn on both sides (a box bar's edges, seen across, cost as much again where a long bar crosses cells).
     */
    static void plate(ProcessUnit u, double[] a, double[] b, double[] out, double width) {
        var along = unit(sub(b, a));
        var across = scale(unit(cross(out, along)), width / 2);
        var face = List.of(plus(a, neg(across)), plus(b, neg(across)), plus(b, across), plus(a, across));
        u.mesh.flat(face, out, Material.STEELWORK);
        u.mesh.flat(face, neg(out), Material.STEELWORK);
    }

    /** The crown frame's underside: the top of the lattice. */
    static double crownFoot(DelayedCokerPlan p) {
        return p.yTop - CROWN_FRAME - SHEAVES;
    }

    /** The crosshead's centre on the drum being cut, as drawn (mid band): the stem well into the drum, the hose loop clear of the deck. */
    static double cutHead(DelayedCokerPlan p) {
        return p.yCut + Math.max(8, .35 * p.derrick);
    }

    /** The goosenecks' height: over the cut drum's crosshead by more than the loop's moving piece reaches. */
    static double gooseneck(DelayedCokerPlan p) {
        return cutHead(p) + 4.5;
    }

    /**
     * Derrick {@code i}'s lattice: legs, struts, Warren diagonals, the crown frame and sheave housing, the guide rails and
     * their ties; each leg collides as drawn.
     */
    static void lattice(ProcessUnit u, DelayedCokerPlan p, int i) {
        double x = p.xd[i], z = p.zc, y0 = p.yCut, top = crownFoot(p), h = p.base / 2;
        int stories = Math.max(4, (int) Math.round((top - y0) / (STORY * p.base)));
        double story = (top - y0) / stories;
        double[] up = {0, 1, 0}, xAxis = {1, 0, 0};
        int[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (var c : corners) {
            double cx = x + c[0] * h, cz = z + c[1] * h;
            u.box(new double[]{cx - LEG / 2, y0, cz - LEG / 2}, new double[]{cx + LEG / 2, top, cz + LEG / 2}, ALL_FACES & ~(4 | 8), Material.STEELWORK, true);
        }
        // Struts at each story, one diagonal a face a story, alternating; the front face's lowest story open.
        for (int k = 0; k < stories; k++) {
            double ya = y0 + k * story, yb = ya + story;
            for (int f = 0; f < 4; f++) {
                var a = corners[f];
                var b = corners[(f + 1) % 4];
                if (k > 0 || f != 0) {
                    boolean rising = (k + f) % 2 == 0;
                    int[] lo = rising ? a : b, hi = rising ? b : a;
                    plate(u, new double[]{x + lo[0] * h, ya, z + lo[1] * h}, new double[]{x + hi[0] * h, yb, z + hi[1] * h}, new double[]{(a[0] + b[0]) / 2, 0, (a[1] + b[1]) / 2}, BRACE);
                }
                if (k + 1 < stories)
                    plate(u, new double[]{x + a[0] * h, yb, z + a[1] * h}, new double[]{x + b[0] * h, yb, z + b[1] * h}, new double[]{(a[0] + b[0]) / 2, 0, (a[1] + b[1]) / 2}, STRUT);
            }
            // The guide rails' ties to the side faces' struts.
            if (k + 1 < stories)
                for (int side = -1; side <= 1; side += 2)
                    u.mesh.bar(new double[]{x + side * (GUIDE + RAIL / 2), yb, z}, new double[]{x + side * h, yb, z}, RAIL * .8, up, NO_ENDS, Material.STEELWORK);
        }
        // The guide rails from over the top unheading device to the crown frame.
        for (int side = -1; side <= 1; side += 2)
            u.mesh.bar(new double[]{x + side * GUIDE, p.yCut + .45, z}, new double[]{x + side * GUIDE, top, z}, RAIL, xAxis, START, Material.STEELWORK);
        // The crown frame and the sheave housing on it.
        u.box(new double[]{x - h - .15, top, z - h - .15}, new double[]{x + h + .15, top + CROWN_FRAME, z + h + .15}, Material.STEELWORK, true);
        u.box(new double[]{x - .55, top + CROWN_FRAME, z - .45}, new double[]{x + .55, p.yTop, z + .45}, ALL_FACES & ~4, Material.MACHINE, true);
        for (int side = -1; side <= 1; side += 2)
            u.mesh.cylinder(Frame.of(new double[]{x, top + CROWN_FRAME + SHEAVES / 2, 0}, 2, 1), .33, z + side * .45, z + side * .5, 16, Material.MACHINE);
        // The hoist on the crown frame beside the sheaves: its drum and motor.
        var hoist = Frame.of(new double[]{x + h - .2, top + CROWN_FRAME + .3, 0}, 2, 1);
        u.mesh.cylinder(hoist, .3, z - .5, z + .3, 12, Material.MACHINE);
        u.motor(hoist, .25, z + .3, z + .9, 0, top + CROWN_FRAME, ProcessUnit.MotorDetail.PLAIN);
        // The standpipe from the cutting water line beside the axis up to the gooseneck, which turns down into the hose; tied to the front face.
        double xs = x + STANDPIPE, zs = p.zCut, yg = gooseneck(p), rg = STANDPIPE / 2;
        u.pipe(CUT_OD / 2, Material.PIPE, true, new double[]{xs, p.yRelief, zs}, new double[]{xs, yg, zs});
        u.mesh.torus(new Frame(new double[]{x + rg, yg, zs}, new double[]{0, 0, 1}, new double[]{1, 0, 0}), rg, CUT_OD / 2, 0, Math.PI, 4, PipeSizes.sides(CUT_OD), Material.PIPE);
        u.mesh.cylinder(Frame.of(new double[]{x, yg, zs}, 1, -1), HOSE / 2 + .04, 0, .12, 12, Material.FLANGE);
        for (int k = 1; k < stories; k++) {
            double y = y0 + k * story;
            if (y < yg - .3) u.mesh.bar(new double[]{xs, y, zs - CUT_OD / 2}, new double[]{xs, y, z - h}, .08, up, NO_ENDS, Material.STEELWORK);
        }
    }

    /**
     * The crosshead and what it carries, centred at {@code y} on drum {@code i}'s axis: the crosshead between its guide
     * shoes, the swivel and the travelling block on it, drawn into the current part.
     */
    static void crosshead(ProcessUnit u, DelayedCokerPlan p, int i, double y) {
        double x = p.xd[i], z = p.zc;
        u.box(new double[]{x - HEAD, y - HEAD_HALF, z - HEAD_DEPTH}, new double[]{x + HEAD, y + HEAD_HALF, z + HEAD_DEPTH}, Material.FLANGE, false);
        for (int side = -1; side <= 1; side += 2)
            u.box(new double[]{x + side * GUIDE - .1, y - .45, z - .16}, new double[]{x + side * GUIDE + .1, y + .45, z + .16}, Material.FLANGE, false);
        var axis = Frame.of(new double[]{x, 0, z}, 1, 1);
        u.mesh.prism(axis, SWIVEL, y + HEAD_HALF, y + SWIVEL_TOP, 12, Material.FLANGE);
        u.box(new double[]{x - .3, y + SWIVEL_TOP, z - .22}, new double[]{x + .3, y + BLOCK_TOP, z + .22}, Material.GUARD, false);
    }

    /**
     * The drum being cut: crosshead, swivel, block, stem, the hose's loop with its piece sliding into the still leg, and the
     * ropes' lower pieces sliding into the still ropes, one moving part on its slider-crank; the still hose leg and ropes.
     */
    static void cutting(ProcessUnit u, DelayedCokerPlan p, int i) {
        double x = p.xd[i], z = p.zc, y = cutHead(p), zs = p.zCut, yg = gooseneck(p), top = crownFoot(p);
        double loop = y - LOOP, ys = y + .55, slide = loop + .15 + UP_STROKE, reach = slide + DOWN_STROKE + .25;
        u.mesh.part(u.motions.size());
        crosshead(u, p, i, y);
        u.prism(Frame.of(new double[]{x, 0, z}, 1, 1), STEM / 2, p.yCut - 2.5, y - HEAD_HALF, STEM_SIDES, Material.PIPE);
        u.pipe(HOSE_IN / 2, Material.OPENING, false, new double[]{x, ys, z - .15}, new double[]{x, ys, z - SWIVEL_LEG}, new double[]{x, loop, z - SWIVEL_LEG},
                new double[]{x, loop, zs}, new double[]{x, reach, zs});
        for (int side = -1; side <= 1; side += 2)
            u.mesh.bar(new double[]{x + side * .15, y + BLOCK_TOP, z}, new double[]{x + side * .15, y + BLOCK_TOP + UP_STROKE + DOWN_STROKE + .3, z}, ROPE_IN, new double[]{1, 0, 0},
                    NO_ENDS, Material.BOLT);
        u.mesh.part(-1);
        u.motions.add(new ProcessUnit.Motion(new double[]{x, y, z}, new double[]{0, 1, 0}, 1 / PERIOD, .5 * (i / 2), CRANK, ROD));
        // The still hose leg from the gooseneck down to where the loop's piece slides into it, and the ropes from the crown.
        var leg = Frame.of(new double[]{x, 0, zs}, 1, 1);
        int n = PipeSizes.sides(HOSE);
        u.prism(leg, HOSE / 2, slide, yg, n, Material.OPENING);
        u.mesh.annulus(leg, HOSE_IN / 2, HOSE / 2, slide, n, -1, Material.OPENING);
        for (int side = -1; side <= 1; side += 2)
            u.mesh.bar(new double[]{x + side * .15, y + BLOCK_TOP + UP_STROKE, z}, new double[]{x + side * .15, top, z}, ROPE, new double[]{1, 0, 0}, START, Material.BOLT);
    }

    /** The drum not being cut: its crosshead parked under the crown, the stem withdrawn with its cutting tool, the hose hanging in its loop. */
    static void parked(ProcessUnit u, DelayedCokerPlan p, int i) {
        double x = p.xd[i], z = p.zc, top = crownFoot(p), y = top - BLOCK_TOP - .4, zs = p.zCut, yg = gooseneck(p), ys = y + .55;
        crosshead(u, p, i, y);
        double foot = Math.max(p.yCut + 1.5, y - HEAD_HALF - p.drumHeight());
        var axis = Frame.of(new double[]{x, 0, z}, 1, 1);
        u.prism(axis, STEM / 2, foot + 1, y - HEAD_HALF, STEM_SIDES, Material.PIPE);
        u.mesh.cylinder(axis, TOOL / 2, foot, foot + 1, 12, Material.FLANGE);
        double loop = yg - 4;
        u.pipe(HOSE / 2, Material.OPENING, false, new double[]{x, yg, zs}, new double[]{x, loop, zs}, new double[]{x, loop, z - SWIVEL_LEG},
                new double[]{x, ys, z - SWIVEL_LEG}, new double[]{x, ys, z - .15});
        for (int side = -1; side <= 1; side += 2)
            u.mesh.bar(new double[]{x + side * .15, y + BLOCK_TOP, z}, new double[]{x + side * .15, top, z}, ROPE, new double[]{1, 0, 0}, NO_ENDS, Material.BOLT);
    }
}
