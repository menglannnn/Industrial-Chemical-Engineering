package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.icheme.science.equipment.Mesh.Frame;
import com.wormzjl.icheme.science.equipment.Mesh.Material;
import com.wormzjl.icheme.science.equipment.ProcessUnit.Solid;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.icheme.science.equipment.Mesh.ALL_FACES;

/**
 * A horizontally split multistage centrifugal compressor train (API 617; owner request 2026-10-18, {@code NEW_UNITS_PLAN.md};
 * {@code NEW_UNITS_SURVEY_ROTATING.md} section 1), sized by its impeller diameter D, 0.3 to 1.5 m, and its impellers, 2
 * to 9 (beyond nine a second casing). Its casing [survey; Elliott, MAN and Siemens outline drawings]: 1.9 D across, 1.2 D
 * plus 0.35 D an impeller long (the inlet plenum and the discharge volute 0.6 D each, banded a little proud), split at the
 * shaft by the heavy joint flange (0.12 D proud, 0.15 D thick, studs about 0.25 m apart: the signature) and standing on
 * feet at that joint; bearing housings 0.7 D across and 0.5 D long at both ends, the thrust bearing's cover in front; the
 * suction and discharge nozzles (0.9 D and 0.45 D, at most 1 m) cast pointing down through the table. Behind it the
 * coupling, a parallel-shaft step-up gear (1.2 by 2 by 2 Hm, its low-speed shaft 0.6 Hm aside) and the induction
 * motor (4.5 Hm long and 2 Hm across with its top-hat cooler), Hm = 0.5 m (P / 1 MW)^¼ from the shaft power P (Elliott
 * 32M4, D 0.65 m with four impellers: 33,000 m³/h, 2.5 MW, 7,176 rpm, scaled with D² and the impellers). The train stands
 * on a concrete table-top ({@link ProcessUnit#concreteTable}) whose 1 m deck lies {@code support} up (4 m and more, so
 * the suction line's elbow and strainer fit under it), on columns on pads at its edges; grating walkways with rails beside
 * it at its level, the right-hand one reached by the stair tower beside it ({@link ProcessUnit#stairTower}, its top
 * landing at the table's front); the API 614 lube-oil console (reservoir, twin pumps on plain motors and filters, twin
 * coolers) at grade on its left with its overhead rundown tank on an X-braced stand on the table beside the motor; a
 * seal-gas panel by the casing.
 * The motor's cooling fan and the coupling's shaft collar turn. Nozzles: suction down through the table; discharge down
 * through it and turned under it out to the right, one block clear of the suction's pipe; cooling water in at the front
 * and out at the back of the console's lower cooler.
 */
final class CentrifugalCompressorDesign extends UnitDesign {
    /** The casing's diameter, its inlet plenum, an impeller's stage and the discharge volute, impeller diameters. */
    static final double CASING = 1.9, PLENUM = .6, STAGE = .35, VOLUTE = .6;
    /** The anchor machine (Elliott 32M4): its impeller diameter, m, its impellers, its flow, m³/h, and its shaft power, MW. */
    static final double ANCHOR = .65, ANCHOR_FLOW = 33000, ANCHOR_POWER = 2.5;
    static final int ANCHOR_STAGES = 4;
    /** The impellers' tip speed, m/s (32M4: 0.65 m at 7,176 rpm). */
    static final double TIP_SPEED = 245;
    /** The suction and discharge nozzles over the impeller diameter (survey: 0.9 and 0.45). */
    static final double SUCTION = .9, DISCHARGE = .45;
    /** The table-top's slab and the walkways' width (the right-hand one wider to the stair tower), m. */
    static final double SLAB = 1, WALK = 1.1;
    /** The lowest table deck, m (the survey's envelopes: 3.5 m at the smallest), and the highest (two stair tower flights of at most 3.6 m). */
    static final int MIN_TABLE = 4, MAX_TABLE = 7;

    @Override
    public double minSize() {
        return .3;
    }

    @Override
    public double maxSize() {
        return 1.5;
    }

    @Override
    public double sizeStep() {
        return .1;
    }

    @Override
    public int minLength(double size) {
        return 2;
    }

    @Override
    public int maxLength(double size) {
        return 9;
    }

    /** The lowest table: 4 m, more for a suction line whose elbow and strainer (1.5 and 2 of its diameters) hang under the slab. */
    @Override
    public int minSupport(double size, int length) {
        return Math.max(MIN_TABLE, (int) Math.ceil(2.5 + 3.5 * suction(size) - 1e-9));
    }

    @Override
    public int maxSupport() {
        return MAX_TABLE;
    }

    @Override
    public Orientation orientation() {
        return Orientation.HORIZONTAL;
    }

    @Override
    public boolean moves() {
        return true;
    }

    @Override
    public String sizeRow() {
        return "centrifugal_compressor.impeller";
    }

    @Override
    public String lengthRow() {
        return "centrifugal_compressor.impellers";
    }

    @Override
    public boolean lengthInMetres() {
        return false;
    }

    @Override
    public String supportRow() {
        return "centrifugal_compressor.table";
    }

    @Override
    public Line held(EquipmentShape shape) {
        double d = shape.diameter();
        return new Line("centrifugal_compressor.flow", whole(flow(d)), number(power(d, (int) shape.tangentLength())), whole(speed(d)));
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        double d = shape.diameter();
        int stages = (int) shape.tangentLength();
        return List.of(new Line("centrifugal_compressor", stages, size(d), size(CASING * d)),
                new Line("centrifugal_compressor.duty", whole(flow(d)), number(power(d, stages)), whole(speed(d))));
    }

    /** The inlet flow, m³/h: the anchor's, scaled with the impeller's area. */
    static double flow(double d) {
        return ANCHOR_FLOW * (d / ANCHOR) * (d / ANCHOR);
    }

    /** The shaft power, MW: the anchor's, scaled with the flow and the impellers (the head per impeller fixed by the tip speed). */
    static double power(double d, int stages) {
        return ANCHOR_POWER * (d / ANCHOR) * (d / ANCHOR) * stages / ANCHOR_STAGES;
    }

    /** The shaft speed, rpm, at {@link #TIP_SPEED}. */
    static double speed(double d) {
        return 60 * TIP_SPEED / (Math.PI * d);
    }

    /** The suction nozzle, m. */
    static double suction(double d) {
        return Math.min(PipeSizes.MAX_OUTSIDE, SUCTION * d);
    }

    /** {@code z} moved at least {@code gap} off {@code avoid}, staying within lo..hi, towards the side it lies on where that fits. */
    private static double clear(double z, double avoid, double gap, double lo, double hi) {
        if (Math.abs(z - avoid) >= gap) return z;
        boolean down = z <= avoid ? avoid - gap >= lo : avoid + gap > hi;
        return down ? Math.max(lo, avoid - gap) : Math.min(hi, avoid + gap);
    }

    /** Draws the train where it lies, its shaft and suction on cell centres: the box starts at the parts' and nozzle faces' least whole metre. */
    @Override
    void build(ProcessUnit unit) {
        draw(unit, .5, .5);
    }

    /** The train with its shaft over x = ax and its suction nozzle's centre at z = zs, both cell centres: see the class. */
    private static void draw(ProcessUnit u, double ax, double zs) {
        double d = u.shape.diameter(), k = (d - .3) / 1.2;
        int stages = (int) u.shape.tangentLength(), h = u.shape.support();
        double hm = .5 * Math.sqrt(Math.sqrt(power(d, stages)));
        // The shaft: 1 D over the soleplates, or high enough for the motor's frame on its base.
        double ys = h + .05 + Math.max(d, hm + .25);
        double r = CASING * d / 2, rb = 1.04 * r, xj = rb + .12 * d, length = (PLENUM + STAGE * stages + VOLUTE) * d;
        double zc0 = zs - PLENUM * d / 2, zc1 = zc0 + length, zp = zc0 + PLENUM * d, zw = zc1 - VOLUTE * d, zv = zc1 - VOLUTE * d / 2;
        var along = Frame.of(new double[]{ax, ys, 0}, 2, 1);
        var backwards = Frame.of(new double[]{ax, ys, 0}, 2, -1);
        int n = ProcessUnit.sides(CASING * d);
        // The casing: the banded plenum and volute either side of the barrel, its end walls; the joint flange along both
        // sides at the shaft, its nuts on top, its split line across the end walls.
        u.mesh.prism(along, rb, zc0, zp, n, Material.MACHINE);
        u.mesh.prism(along, r, zp, zw, n, Material.MACHINE);
        u.mesh.prism(along, rb, zw, zc1, n, Material.MACHINE);
        u.mesh.annulus(along, r, rb, zp, n, 1, Material.MACHINE);
        u.mesh.annulus(along, r, rb, zw, n, -1, Material.MACHINE);
        double housing = .35 * d;
        u.mesh.annulus(along, housing, rb, zc0, n, -1, Material.MACHINE);
        u.mesh.annulus(along, housing, rb, zc1, n, 1, Material.MACHINE);
        u.solids.add(Solid.rod(new double[]{ax, ys, 0}, 2, zc0, zc1, rb, 0));
        u.volumes.add(Solid.rod(new double[]{ax, ys, 0}, 2, zc0 + .05, zc1 - .05, r - .05, 0));
        u.round(ax, ys, zc0, zc1, rb);
        double half = .075 * d;
        for (int side = -1; side <= 1; side += 2) {
            double inner = ax + side * .97 * r, outer = ax + side * xj;
            u.mesh.box(new double[]{Math.min(inner, outer), ys - half, zc0 - .02}, new double[]{Math.max(inner, outer), ys + half, zc1 + .02}, ALL_FACES & ~(side > 0 ? 1 : 2),
                    Material.MACHINE);
            u.solids.add(ProcessUnit.span(new double[]{inner, ys - half, zc0 - .02}, new double[]{outer, ys + half, zc1 + .02}));
            double end0 = ax + side * (housing + .01), end1 = ax + side * rb;
            u.mesh.box(new double[]{Math.min(end0, end1), ys - .6 * half, zc0 - .03}, new double[]{Math.max(end0, end1), ys + .6 * half, zc0}, ALL_FACES & ~32, Material.MACHINE);
            u.mesh.box(new double[]{Math.min(end0, end1), ys - .6 * half, zc1}, new double[]{Math.max(end0, end1), ys + .6 * half, zc1 + .03}, ALL_FACES & ~16, Material.MACHINE);
            // The joint's nuts about 0.25 m apart on top of the flange.
            int nuts = Math.max(4, (int) Math.round((length - .1) / .25) + 1);
            double nut = .03 + .03 * d, xn = ax + side * (rb + .06 * d);
            for (int j = 0; j < nuts; j++) {
                double z = zc0 + .05 + (length - .1) * j / (nuts - 1);
                var f = Frame.of(new double[]{xn, 0, z}, 1, 1);
                u.mesh.prism(f, nut, ys + half, ys + half + .8 * nut, 6, Material.BOLT);
                u.mesh.annulus(f, 0, nut, ys + half + .8 * nut, 6, 1, Material.BOLT);
            }
            // The feet at the joint over the plenum and the volute, on pedestals and soleplates.
            for (double zf : new double[]{zc0 + .15 * d, zc1 - .15 * d}) {
                double f0 = ax + side * (rb - .05 * d), f1 = ax + side * (xj + .1 * d), p0 = ax + side * (xj - .05 * d), foot = ys - half - .12 * d;
                u.box(new double[]{Math.min(f0, f1), foot, zf - .1 * d}, new double[]{Math.max(f0, f1), ys - half, zf + .1 * d}, Material.SUPPORT, true);
                u.box(new double[]{Math.min(p0, f1), h + .05, zf - .1 * d}, new double[]{Math.max(p0, f1), foot, zf + .1 * d}, Material.SUPPORT, true);
                u.box(new double[]{Math.min(p0, f1) - .05, h, zf - .15 * d - .03}, new double[]{Math.max(p0, f1) + .05, h + .05, zf + .15 * d + .03}, Material.STEELWORK, false);
            }
        }
        // Lifting lugs on top of the barrel.
        for (double zl : new double[]{zp + .1 * d, zw - .1 * d - .2 * d})
            u.box(new double[]{ax - .02 - .01 * d, ys + .95 * r, zl}, new double[]{ax + .02 + .01 * d, ys + r + .12 * d + .04, zl + .2 * d}, Material.MACHINE, false);
        // The bearing housings, split at the shaft; the thrust bearing's cover in front.
        double zb0 = zc0 - .5 * d, zb1 = zc1 + .5 * d, cover = zb0 - .12 * d;
        for (double[] span : new double[][]{{zb0, zc0}, {zc1, zb1}}) {
            u.mesh.prism(along, housing, span[0], span[1], ProcessUnit.sides(2 * housing), Material.MACHINE);
            u.box(new double[]{ax - housing - .04 * d, ys - .04 * d, span[0]}, new double[]{ax + housing + .04 * d, ys + .04 * d, span[1]}, Material.MACHINE, false);
            u.solids.add(Solid.rod(new double[]{ax, ys, 0}, 2, span[0], span[1], housing, 0));
        }
        u.mesh.cylinder(along, .25 * d, cover, zb0, ProcessUnit.sides(.5 * d), Material.MACHINE);
        u.mesh.annulus(backwards, .25 * d, housing, -zb0, ProcessUnit.sides(2 * housing), 1, Material.MACHINE);
        u.mesh.annulus(along, .02 + .04 * d, housing, zb1, ProcessUnit.sides(2 * housing), 1, Material.MACHINE);
        u.solids.add(Solid.rod(new double[]{ax, ys, 0}, 2, cover, zb0, .25 * d, 0));
        // The nozzles' castings down to their flanges over the table; the discharge's runs on into the slab.
        double odS = suction(d), odD = DISCHARGE * d, top = ys - .6 * r;
        for (int which = 0; which < 2; which++) {
            double od = which == 0 ? odS : odD, z = which == 0 ? zs : zv;
            var down = Frame.of(new double[]{ax, 0, z}, 1, 1);
            int nn = ProcessUnit.sides(od);
            u.mesh.prism(down, od / 2 + .03, h + .13, top, nn, Material.MACHINE);
            u.mesh.ring(down, od / 2, PipeSizes.flange(od) / 2, h + .1, h + .13, nn, 3, Material.FLANGE);
            u.solids.add(Solid.rod(new double[]{ax, 0, z}, 1, h + .1, top, PipeSizes.flange(od) / 2, 0));
            if (which == 1) u.mesh.prism(down, od / 2, h, h + .1, nn, Material.PIPE);
        }
        u.nozzles.add(new Nozzle(Role.SUCTION, new double[]{ax, h + .1, zs}, 1, -1, h + .1 - (h - SLAB), odS, odS, 0, .03));
        // Balance line and bearing drains from 0.6 m.
        if (d >= .6 - 1e-9) {
            double xb = ax - (xj + .1 * d + .1), yb = ys - .5 * r;
            u.line(.03 + .03 * d, Material.PIPE, new double[]{ax - .8 * r, yb, zw + .2 * d}, new double[]{xb, yb, zw + .2 * d}, new double[]{xb, yb, zp - .2 * d},
                    new double[]{ax - .8 * r, yb, zp - .2 * d});
            for (double zd : new double[]{(zb0 + zc0) / 2, (zc1 + zb1) / 2})
                u.line(.02 + .01 * d, Material.PIPE, new double[]{ax + .2 * d, ys - .25 * d, zd}, new double[]{ax + .2 * d, h, zd});
        }

        // The coupling: the exposed shaft and its collar turning, then the guard to the gear.
        double rs = .025 + .04 * d, zg0 = zb1 + .1 + .15 * d, zg1 = zg0 + Math.max(.3, .75 * d);
        u.shaft(along, rs, new double[][]{{zb1 - .01, zg0 + .01}}, (zb1 + zg0) / 2);
        double guard = .12 + .22 * d;
        u.mesh.cylinder(along, guard, zg0, zg1, ProcessUnit.sides(2 * guard), Material.GUARD);
        u.solids.add(Solid.rod(new double[]{ax, ys, 0}, 2, zg0, zg1, guard, 0));
        // The step-up gear: its lower casing on a soleplate, the split flange, the upper cover over the bull gear, bearing
        // caps on both ends (the pinion's in line with the compressor, the bull gear's with the motor), breather and cover.
        double xm = ax - .6 * hm, xg = ax - .3 * hm, zG0 = zg1 + .08, zG1 = zG0 + 1.2 * hm;
        u.box(new double[]{xg - hm, h + .05, zG0}, new double[]{xg + hm, ys - .04, zG1}, Material.MACHINE, true);
        u.box(new double[]{xg - hm - .08, h, zG0 - .08}, new double[]{xg + hm + .08, h + .05, zG1 + .08}, Material.STEELWORK, false);
        u.box(new double[]{xg - hm - .05, ys - .04, zG0 - .03}, new double[]{xg + hm + .05, ys + .04, zG1 + .03}, Material.MACHINE, true);
        var outline = List.of(new double[]{xg - hm, ys + .04}, new double[]{xg + hm, ys + .04}, new double[]{xg + hm, ys + .5 * hm}, new double[]{xg + .3 * hm, ys + hm},
                new double[]{xg - .7 * hm, ys + hm}, new double[]{xg - hm, ys + .7 * hm});
        u.extrude(outline, zG0 + .02, zG1 - .02, Material.MACHINE);
        u.solids.add(Solid.box(new double[]{xg - hm, ys, zG0}, new double[]{xg + hm, ys + hm, zG1}));
        u.volumes.add(Solid.box(new double[]{xg - hm + .05, h + .1, zG0 + .05}, new double[]{xg + hm - .05, ys + .5 * hm, zG1 - .05}));
        for (double[] cap : new double[][]{{ax, .18}, {xm, .25}}) {
            var axis = Frame.of(new double[]{cap[0], ys, 0}, 2, 1);
            double rc = cap[1] * hm + .02;
            u.mesh.cylinder(axis, rc, zG0 - .08, zG0, 12, Material.MACHINE);
            u.mesh.cylinder(axis, rc, zG1, zG1 + .08, 12, Material.MACHINE);
        }
        u.mesh.cylinder(Frame.of(new double[]{xg - .2 * hm, 0, zG0 + .3 * hm}, 1, 1), .04 + .02 * k, ys + hm, ys + hm + .12, 8, Material.MACHINE);
        u.box(new double[]{xg - .55 * hm, ys + hm, zG0 + .55 * hm}, new double[]{xg + .1 * hm, ys + hm + .03, zG1 - .2 * hm}, Material.MACHINE, false);
        // The motor's coupling guard and the motor (its fan turns).
        double zm0 = zG1 + .08 + Math.max(.3, .7 * hm), guard2 = .1 + .25 * hm, lm = 4.5 * hm;
        var motorAxis = Frame.of(new double[]{xm, ys, 0}, 2, 1);
        u.mesh.cylinder(motorAxis, guard2, zG1 + .08, zm0 - .02, ProcessUnit.sides(2 * guard2), Material.GUARD);
        u.solids.add(Solid.rod(new double[]{xm, ys, 0}, 2, zG1 + .08, zm0 - .02, guard2, 0));
        u.largeMotor(xm, ys, zm0, 2 * hm, lm, h, false);

        // The table-top's reach, and the half width q of its opening round the suction.
        double terminal = Math.min(1.2, .3 + .4 * hm), pd = .4 + .2 * k, pl = .6 + .4 * k, ph = 1.4 + .6 * k;
        double tod = .6 + .5 * k, tl = 1.5 + 1.5 * k, sw = tod / 2 + .05, xt = ax + .6 * hm + .4 * terminal + .25 + sw;
        double left = Math.max(xj + .2 * d, 1.7 * hm) + .6, right = Math.max(Math.max(xj + .5 + pd + .4, xt - ax + sw + .4), .7 * hm + .5);
        double x0 = ax - left, x1 = ax + right, zt0 = cover - .8, zt1 = zm0 + lm + .7, y0 = h - SLAB;
        double q = Math.max(odS / 2 + .08, PipeSizes.flange(odS) / 2 + .03);
        // The discharge leaves the slab's underside (a dog-leg hidden in it keeps it clear of the suction's pipe on a short
        // casing), drops to an elbow and runs out to the right, jogged onto a cell centre, to its flange.
        double rd = odD / 2, bend = Math.max(.15, odD), xv = zv - zs >= .55 + rd ? ax : ax - .55 - rd;
        double yd = Math.floor(y0 - .1 - bend - .5) + .5, zd = Math.floor(zv) + .5, jog = Math.abs(zd - zv);
        int nd = ProcessUnit.sides(odD);
        u.mesh.prism(Frame.of(new double[]{xv, 0, zv}, 1, 1), rd, yd + bend, y0, nd, Material.PIPE);
        u.mesh.torus(new Frame(new double[]{xv - bend, yd + bend, zv}, new double[]{0, 0, 1}, new double[]{1, 0, 0}), bend, rd, -Math.PI / 2, 0, 6, nd, Material.PIPE);
        double xr = xv - bend - .1, faceD = Math.min(Math.floor(xr - jog - 1.1 * rd - .03 - EquipmentShape.STAND_OUT), ax - 1.5), xn = faceD + .03 + EquipmentShape.STAND_OUT;
        u.line(rd, Material.PIPE, new double[]{xv - bend, yd, zv}, new double[]{xr, yd, zv}, new double[]{xr - jog, yd, zd}, new double[]{xn + .01, yd, zd});
        u.solids.add(Solid.rod(new double[]{xv, 0, zv}, 1, yd + bend, y0, rd, 0));
        u.solids.add(ProcessUnit.span(new double[]{xn, yd - rd, Math.min(zv, zd) - rd}, new double[]{xv + rd, yd + bend, Math.max(zv, zd) + rd}));
        u.post(xr, zv, 0, yd - rd, .12, false);
        u.nozzles.add(new Nozzle(Role.DISCHARGE, new double[]{xn, yd, zd}, 0, -1, xn - faceD, odD, odD, 0, .03));
        // The table-top on columns at both edges at most 5 m apart, the right-hand ones clear of the discharge's pipe.
        double c = .6 + .4 * k;
        int count = Math.max(2, (int) Math.ceil((zt1 - zt0 - c) / 5 - 1e-9) + 1);
        var posts = new ArrayList<double[]>();
        for (int side = -1; side <= 1; side += 2)
            for (int j = 0; j < count; j++) {
                double z = zt0 + c / 2 + (zt1 - zt0 - c) * j / (count - 1);
                if (side < 0) z = clear(z, zd, c / 2 + .6, zt0 + c / 2, zt1 - c / 2);
                posts.add(new double[]{(side < 0 ? x0 : x1) - side * c / 2, z});
            }
        u.concreteTable(x0, x1, zt0, zt1, h, SLAB, List.of(new double[]{ax - q, zs - q, ax + q, zs + q}), c, posts);
        // The stair tower beside the right-hand walkway, its lanes on cell centres (that walkway as wide again as reaches
        // them), its near end at the table's front end, or behind the discharge's pipe and its pipe block where they come
        // out under the tower, rising to the back; its top landing joins the walkway. The walkways beside the table at its
        // level, rails along its front and back.
        double deck = h - Access.DECK, edge = Math.floor(x0 - WALK - .95) + .95;
        double front = faceD - 1 < edge ? Math.max(zt0, Math.max(zd + .5, Math.max(zv, zd) + rd) + .05) : zt0;
        var plan = ProcessUnit.towerPlan(deck);
        var tower = u.stairTower(plan, edge - ProcessUnit.TOWER_FLIGHT / 2, ProcessUnit.TowerPlan.foot(front, 1, plan.from()), 2, 1, -1, true);
        var walk = new Access.Line(new double[]{x0, 0, zt0}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, zt1 - zt0);
        u.platform(walk, deck, x0 - edge, true, true, walk.gap(tower.openings().getFirst()));
        u.platform(new Access.Line(new double[]{x1, 0, zt0}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, zt1 - zt0), deck, WALK, true, true);
        u.railing(new Access.Line(new double[]{x0, 0, zt0 + 1}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, x1 - x0), deck, 1);
        u.railing(new Access.Line(new double[]{x0, 0, zt1 - 1}, new double[]{1, 0, 0}, new double[]{0, 0, 1}, x1 - x0), deck, 1);

        // The seal-gas panel by the casing's left; the rundown tank on its braced stand beside the motor, its line down to
        // the drive-end bearing.
        double sx = ax + xj + .5, sz = zc0 + .1 * length;
        u.box(new double[]{sx, h, sz}, new double[]{sx + pd, h + ph, sz + pl}, Material.MACHINE, true);
        u.box(new double[]{sx - .01, h + .45 * ph, sz + .1}, new double[]{sx, h + .9 * ph, sz + pl - .1}, Material.OPENING, false);
        double yt = ys + 4 + .5 * k, lo = zm0 + (lm - tl) / 2, hi = lo + tl, legTop = yt - .35 * tod, leg = .1 + .04 * k;
        u.bottle(xt, yt, lo, hi, tod, ProcessUnit.sides(tod));
        for (double lz : new double[]{lo + .15, hi - .15}) {
            for (int side = -1; side <= 1; side += 2) u.post(xt + side * sw, lz, h, legTop, leg, false);
            u.box(new double[]{xt - sw - leg / 2, legTop - .12, lz - leg / 2}, new double[]{xt + sw + leg / 2, legTop, lz + leg / 2}, Material.STEELWORK, true);
        }
        for (int side = -1; side <= 1; side += 2)
            u.brace(new double[]{xt + side * sw, lo + .15}, new double[]{xt + side * sw, hi - .15}, h + .2, legTop - .12, .06, ProcessUnit.BRACE_TIER);
        double ro = .025 + .02 * k, zr = (zc1 + zb1) / 2;
        u.line(ro, Material.PIPE, new double[]{xt, yt - .4 * tod, (lo + hi) / 2}, new double[]{xt, h + .25, (lo + hi) / 2}, new double[]{xt, h + .25, zr},
                new double[]{ax + housing + .1, h + .25, zr}, new double[]{ax + housing + .1, ys - .2 * d, zr}, new double[]{ax + .6 * housing, ys - .2 * d, zr});

        // The lube-oil console at grade on the left: its skid, the reservoir with its two pumps and filters on top, and
        // the twin coolers stacked on its outer side, the changeover's water jumpers between them (cooling water in at
        // the lower one's front head, out at its back head; the skid stops short of both so their pipes find room).
        double odC = .25 + .25 * k, rC = odC / 2, wc = 1.2 + 1.8 * k, lc = 2.4 + 1.8 * k;
        double xcc = Math.ceil(x1 + .3 + wc - .15 - rC - .5 - 1e-9) + .5, xk1 = xcc + rC + .15, xk0 = xk1 - wc;
        double zl1 = zt1 - .7 - odC / 4, zl0 = zl1 - lc, zk0 = zl0 - odC / 4 - .1, zk1 = zl1 + odC / 4 + .1;
        u.box(new double[]{xk0, 0, zk0}, new double[]{xk1, .15, zk1}, Material.STEELWORK, true);
        double hr = .9 + 1.1 * k, xr1 = xk0 + .5 * wc;
        u.box(new double[]{xk0 + .08, .15, zk0 + .15}, new double[]{xr1, .15 + hr, zk1 - .15}, Material.MACHINE, true);
        double rp = .1 + .06 * k, tp = .15 + hr;
        for (double pz : new double[]{zk1 - .3 - 1.2 * rp, zk1 - .4 - 3.6 * rp}) {
            var up = Frame.of(new double[]{(xk0 + xr1) / 2, 0, pz}, 1, 1);
            u.mesh.cylinder(up, 1.2 * rp, tp, tp + .2, 12, Material.MACHINE);
            u.solids.add(Solid.rod(new double[]{(xk0 + xr1) / 2, 0, pz}, 1, tp, tp + .2, 1.2 * rp, 0));
            u.motor(up, rp, tp + .2, tp + .55 + .25 * k, 0, tp, ProcessUnit.MotorDetail.PLAIN);
        }
        for (double fz : new double[]{zk0 + .3 + rp, zk0 + .4 + 3 * rp})
            u.mesh.cylinder(Frame.of(new double[]{(xk0 + xr1) / 2, 0, fz}, 1, 1), .8 * rp, tp, tp + .5 + .3 * k, 12, Material.MACHINE);
        int nc = ProcessUnit.sides(odC);
        double yc0 = .5, yc1 = .5 + odC + .2;
        for (double yc : new double[]{yc0, yc1}) u.bottle(xcc, yc, zl0, zl1, odC, nc);
        for (double sz2 : new double[]{zl0 + .2, zl1 - .2}) {
            u.box(new double[]{xcc - .7 * rC, .15, sz2 - .05}, new double[]{xcc + .7 * rC, yc0 - .7 * rC, sz2 + .05}, Material.STEELWORK, false);
            for (int side = -1; side <= 1; side += 2)
                u.box(new double[]{xcc + side * (rC + .04) - .03, .15, sz2 - .03}, new double[]{xcc + side * (rC + .04) + .03, yc1, sz2 + .03}, Material.STEELWORK, false);
            u.box(new double[]{xcc - rC - .07, yc1 - .7 * rC - .05, sz2 - .03}, new double[]{xcc + rC + .07, yc1 - .7 * rC, sz2 + .03}, Material.STEELWORK, false);
        }
        double odW = .06 + .08 * k, rw = .4 * odW;
        for (double zj : new double[]{zl0 - .12 * odC, zl1 + .12 * odC})
            u.line(rw, Material.PIPE, new double[]{xcc, yc0 + .8 * rC, zj}, new double[]{xcc, yc1 - .8 * rC, zj});
        double tip0 = zl0 - odC / 4, face0 = Math.floor(tip0 - EquipmentShape.STAND_OUT), tip1 = zl1 + odC / 4, face1 = Math.ceil(tip1 + EquipmentShape.STAND_OUT);
        u.nozzles.add(new Nozzle(Role.COOLANT_INLET, new double[]{xcc, yc0, zl0 - .02}, 2, -1, zl0 - .02 - face0, .15, odW, 0, zl0 - .02 - tip0));
        u.nozzles.add(new Nozzle(Role.COOLANT_OUTLET, new double[]{xcc, yc0, zl1 + .02}, 2, 1, face1 - zl1 - .02, .15, odW, 0, tip1 - zl1 - .02));
        // The oil supply and return lines rise into the slab's left edge between its last two columns.
        double zcol = zt0 + c / 2 + (zt1 - zt0 - c) * (count - 1.5) / (count - 1), xo = xk0 + .3;
        for (int j = 0; j < 2; j++) {
            double z = Math.max(zk0 + .3, Math.min(zk1 - .3, zcol + (j == 0 ? -.2 : .2))), rl = j == 0 ? .03 + .03 * k : .04 + .04 * k;
            u.line(rl, Material.PIPE, new double[]{xo, .15 + hr, z}, new double[]{xo, y0 - .3, z}, new double[]{x1 - .5, y0 - .3, z}, new double[]{x1 - .5, y0, z});
        }
    }
}
