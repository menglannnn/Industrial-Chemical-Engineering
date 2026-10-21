package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;

import java.util.ArrayList;

import static com.wormzjl.icheme.science.equipment.DelayedCokerPlan.*;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/**
 * A delayed coker's concrete table, drums, bottom and top works, lines and nozzles ({@link DelayedCokerPlan};
 * {@code NEW_UNITS_SURVEY_FCC_COKER.md} section 3).
 *
 * <p>The table (the kit's {@link ProcessUnit#concreteTable}): a 1.5 m slab on square columns of 0.15 D (1.2 m at least)
 * standing on pads, at the structure's corners and between the drums, front and back, a square opening round each drum's
 * cone; its top's handrail is the structure's ({@link DelayedCokerStructure#tableRail}). Each drum: its skirt from the
 * table's top to just over the cone (the skirt is welded to the lower shell and rests on the table [US8221591]), with an
 * access opening; the aluminium jacket over 100 mm of insulation up to the top tangent and over the 2:1 head; the 30° cone
 * inside the skirt down to the 2 m bottom flange. Under the flange the bottom spool with its side-entry feed stub, the
 * slide-gate unheading valve (a flat body about 2.7 to 3.5 flange diameters long with its bonnet and two hydraulic
 * cylinders [Coking 101]) and the telescoping chute (three sections of 2.2, 2 and 1.8 m and a cone to the 1 m toe; only
 * drum 0's toe is the unit's coke outlet [Q5], the others end open over the pad). On top: the top spool to the slide-type
 * top unheading device on the cutting deck [Coking 101], the overhead line (0.06 D, 0.3 m at least [Ellis &amp; Paul:
 * 0.3 to 0.6 m]) rising off the head's shoulder to a tee into the vapour header in front and, through its valve, the
 * blowdown header behind, and the relief valve on the other shoulder into the relief header over the blowdown header.
 * Under the slab each pair's switch valve sends the feed through transfer lines to its drums' feed stubs; the feed
 * header, the quench water header and the steam line join the switch valves. Lines are the kit's
 * ({@link ProcessUnit#pipe}: long-radius mitred elbows).
 *
 * <p>Nozzles (survey's table; each role once; each at its line's end, {@link ProcessUnit#end}, its flange face on the
 * next block face): FEED 2- (0.03 D, 0.15 m at least) and WATER 2+ (0.03 D) under the slab's front and back, STEAM 0-
 * (0.02 D, 0.1 m at least) at its -x end; VAPOUR_OUTLET 0- (0.08 D, 1 m at most) at the vapour
 * header's end, QUENCH 1+ (0.06 m) on its top near that end, BLOWDOWN 0+ (0.05 D) at the blowdown header's end, RELIEF
 * 2+ (0.04 D) off the relief header's -x end to the back; CUTTING_WATER 0+ (0.15 m) at the foot of the riser outside the
 * +x end; SOLIDS_OUTLET 1- (1 m) at drum 0's chute toe.
 */
final class DelayedCokerDrums {
    /** How far up the cone is drawn over its flange, m: as far as is seen through the table's opening (its radius there about 2.4 m). */
    static final double CONE_SHOWN = 2.4;

    private DelayedCokerDrums() {
    }

    static void draw(ProcessUnit u, DelayedCokerPlan p) {
        table(u, p);
        for (int i = 0; i < p.drums; i++) {
            drum(u, p, i);
            bottom(u, p, i);
            top(u, p, i);
        }
        underSlab(u, p);
        overheads(u, p);
    }

    // ---- The table ------------------------------------------------------------------------------------------------

    /**
     * The concrete table ({@link ProcessUnit#concreteTable}): its columns on their pads at the structure's corners and
     * between the drums, front and back, flush with the slab's faces; the slab cut round a square opening under each drum.
     */
    static void table(ProcessUnit u, DelayedCokerPlan p) {
        double c = p.concrete, h = TABLE_HOLE;
        var posts = new ArrayList<double[]>();
        for (double x : p.lines) {
            double xc = Math.max(p.x0, Math.min(x - c / 2, p.x1 - c)) + c / 2;
            posts.add(new double[]{xc, p.z0 + c / 2});
            posts.add(new double[]{xc, p.zb - c / 2});
        }
        var holes = new ArrayList<double[]>();
        for (double x : p.xd) holes.add(new double[]{x - h, p.zc - h, x + h, p.zc + h});
        u.concreteTable(p.x0, p.x1, p.z0, p.zb, p.yTable, SLAB, holes, c, posts);
    }

    // ---- A drum ---------------------------------------------------------------------------------------------------

    /**
     * Drum {@code i}: bottom flange, cone, skirt with its access opening, jacket and jacketed head; it collides and encloses
     * as drawn. The cone is drawn up to {@link #CONE_SHOWN} over the flange, as much as is seen through the table's opening;
     * above it the skirt hides it, and a ring across the skirt closes the drum's inside there.
     */
    static void drum(ProcessUnit u, DelayedCokerPlan p, int i) {
        double x = p.xd[i], z = p.zc, s = p.support, shown = Math.min(p.cone, CONE_SHOWN), yc = s + shown, rc = BOTTOM + (p.r - BOTTOM) * shown / p.cone;
        int n = p.sides;
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        // The bottom flange: its underside meets the spool's flange; its top shows round the cone.
        int nb = ProcessUnit.sides(2 * BOTTOM + .3);
        u.mesh.prism(up, BOTTOM + .15, s, s + FLANGE, nb, Material.FLANGE);
        u.mesh.annulus(up, BOTTOM, BOTTOM + .15, s + FLANGE, nb, 1, Material.FLANGE);
        u.mesh.frustum(up, BOTTOM, rc, s + FLANGE, yc, n, Material.SHELL);
        if (shown < p.cone) u.mesh.annulus(up, rc, p.r, yc, n, -1, Material.SHELL);
        u.prism(up, p.r, p.yTable, p.ySkirt, n, Material.SUPPORT);
        u.mesh.annulus(up, p.r, p.jacket, p.ySkirt, n, -1, Material.CLADDING);
        u.prism(up, p.jacket, p.ySkirt, p.yTangent, n, Material.CLADDING);
        u.mesh.head(up, p.jacket, p.yTangent, p.jacket / 2, n, Material.CLADDING);
        // The skirt's access opening at the front, where it is tall enough: a corner of the skirt's polygon faces -z, its faces
        // recede 0.35 tan 7.5° = 0.046 m at the opening's sides, so the opening stands from 0.02 m in front of the corner to
        // just in front of the faces there.
        if (p.ySkirt - p.yTable > 1.6)
            u.mesh.block(new double[]{x, p.yTable + .85, z - p.r + .01}, new double[]{.35, 0, 0}, new double[]{0, .6, 0}, new double[]{0, 0, .03},
                    Material.OPENING);
        double[] axis = {x, 0, z};
        u.solids.add(ProcessUnit.Solid.cone(axis, 1, s, yc, BOTTOM + .15, rc, 0));
        u.solids.add(ProcessUnit.Solid.rod(axis, 1, p.yTable, p.yTangent, p.jacket, p.jacket / 2));
        u.volumes.add(ProcessUnit.Solid.cone(axis, 1, s + .2, yc, .94 * (BOTTOM + .1), .94 * rc, 0));
        u.volumes.add(ProcessUnit.Solid.rod(axis, 1, yc, p.yTangent, .94 * p.r, .9 * p.jacket / 2));
        u.round(x, z, p.ySkirt, p.yTangent, p.jacket, 1);
        u.round(x, z, p.yTable, p.ySkirt, p.r, 1);
    }

    /**
     * Drum {@code i}'s bottom works under the table: the bottom spool (its feed stub towards its pair's switch valve), the
     * unheading valve (body, bonnet, end plate, two hydraulic cylinders) and the telescoping chute.
     */
    static void bottom(ProcessUnit u, DelayedCokerPlan p, int i) {
        double x = p.xd[i], z = p.zc, s = p.support, fl = BOTTOM + .15;
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        int nf = ProcessUnit.sides(2 * fl);
        // The spool, flanged at both ends, its feed stub at the feed row.
        u.mesh.prism(up, fl, s - FLANGE, s, nf, Material.FLANGE);
        u.mesh.annulus(up, BOTTOM - .05, fl, s - FLANGE, nf, -1, Material.FLANGE);
        u.mesh.prism(up, BOTTOM - .05, s - SPOOL + FLANGE, s - FLANGE, nf, Material.MACHINE);
        u.mesh.prism(up, fl, s - SPOOL, s - SPOOL + FLANGE, nf, Material.FLANGE);
        u.mesh.annulus(up, BOTTOM - .05, fl, s - SPOOL + FLANGE, nf, 1, Material.FLANGE);
        int toward = p.xp[i / 2] > x ? 1 : -1;
        var stub = Frame.of(new double[]{0, p.yFeed, z}, 0, toward);
        double sx = toward * x;
        u.mesh.prism(stub, p.feedOd / 2 + .03, sx + BOTTOM - .1, sx + 1.25, ProcessUnit.sides(p.feedOd), Material.MACHINE);
        u.mesh.cylinder(stub, p.feedOd / 2 + .1, sx + 1.25, sx + 1.3, ProcessUnit.sides(p.feedOd + .2), Material.FLANGE);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{x, 0, z}, 1, s - SPOOL, s, fl, 0));
        u.volumes.add(ProcessUnit.Solid.rod(new double[]{x, 0, z}, 1, s - SPOOL + .15, s - .15, BOTTOM - .15, 0));
        // The slide-gate valve: its body round the bore, the bonnet the gate retracts into, the end plate, the cylinders.
        double v1 = s - SPOOL, v0 = v1 - VALVE, length = Math.min(3.5 * 2 * BOTTOM, p.zb - p.zc - .4 + 1.3), back = z - 1.3 + length;
        double plate = back - .9;
        u.box(new double[]{x - 1.3, v0, z - 1.3}, new double[]{x + 1.3, v1, z + 1.3}, Material.MACHINE, true);
        u.box(new double[]{x - 1.1, v0 + .1, z + 1.3}, new double[]{x + 1.1, v1 - .1, plate}, ALL_FACES & ~16, Material.MACHINE, true);
        u.box(new double[]{x - 1.2, v0 + .05, plate}, new double[]{x + 1.2, v1 - .05, plate + .1}, Material.MACHINE, true);
        for (int side = -1; side <= 1; side += 2) {
            var cylinder = Frame.of(new double[]{x + side * .55, (v0 + v1) / 2, 0}, 2, 1);
            u.mesh.cylinder(cylinder, .16, plate + .1, back, 12, Material.ACTUATOR);
            u.mesh.cylinder(cylinder, .2, back - .08, back, 12, Material.ACTUATOR);
        }
        // The telescoping chute: three sections, each 0.1 m narrower, a 30° cone to the 1 m toe.
        double top = v0, toe = 1.6, coneTop = toe + .7, sectionLength = (top - .1 - coneTop) / 3;
        u.mesh.prism(up, 1.2, top - .1, top, ProcessUnit.sides(2.4), Material.FLANGE);
        u.mesh.annulus(up, 1.1, 1.2, top - .1, ProcessUnit.sides(2.4), -1, Material.FLANGE);
        double[] radii = {1.1, 1, .9};
        double upper = top - .1;
        for (int k = 0; k < 3; k++) {
            double lower = k == 2 ? coneTop : upper - sectionLength;
            u.prism(up, radii[k], lower, upper, ProcessUnit.sides(2 * radii[k]), Material.MACHINE);
            if (k < 2) {
                // The step to the next section with the collar round its foot.
                u.mesh.annulus(up, radii[k + 1], radii[k] + .05, lower, ProcessUnit.sides(2 * radii[k]), -1, Material.MACHINE);
                u.mesh.prism(up, radii[k] + .05, lower, lower + .12, ProcessUnit.sides(2 * radii[k]), Material.MACHINE);
            }
            upper = lower;
        }
        u.mesh.frustum(up, .9, .5, coneTop, toe, ProcessUnit.sides(1.8), Material.MACHINE);
        if (i > 0) {
            u.mesh.ring(up, .45, .6, toe - .08, toe, 16, 1, Material.FLANGE);
            u.mesh.annulus(up, 0, .5, toe + .02, 16, -1, Material.OPENING);
        }
        u.solids.add(ProcessUnit.Solid.rod(new double[]{x, 0, z}, 1, toe, top, 1.2, 0));
        u.volumes.add(ProcessUnit.Solid.rod(new double[]{x, 0, z}, 1, coneTop, top - .15, .82, 0));
        u.round(x, z, coneTop, top, 1.1, 1);
    }

    /**
     * Drum {@code i}'s top works: the insulated top spool and its flange, the top unheading device in the cutting deck's
     * opening (a flat box with two cylinders on it and the stem's entry); the overhead line off the head's -x shoulder
     * with its flange, rising to a tee: the vapour branch to the front header, the blowdown branch with its valve to the
     * back one; the relief valve on the +x shoulder, its outlet into the relief header.
     */
    static void top(ProcessUnit u, DelayedCokerPlan p, int i) {
        double x = p.xd[i], z = p.zc, deck = p.cutLevel, apex = p.yTangent + p.jacket / 2;
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        double rs = p.topFlange / 2 - .1;
        u.prism(up, rs, apex - .1, deck - .25, ProcessUnit.sides(2 * rs), Material.CLADDING);
        u.mesh.cylinder(up, p.topFlange / 2, deck - .25, deck - .1, ProcessUnit.sides(p.topFlange), Material.FLANGE);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{x, 0, z}, 1, apex - .1, deck - .1, p.topFlange / 2, 0));
        double dl = Math.min(p.base / 2 - .15, 1.3 * p.topFlange), dw = .55 * p.topFlange;
        u.box(new double[]{x - dl, deck - .1, z - dw}, new double[]{x + dl, deck + .5, z + dw}, Material.MACHINE, true);
        for (int side = -1; side <= 1; side += 2)
            u.mesh.cylinder(Frame.of(new double[]{0, deck + .59, z + side * .5 * dw}, 0, 1), .09, x - .75 * dl, x + .75 * dl, 12, Material.ACTUATOR);
        u.mesh.annulus(up, 0, .13, deck + .505, 12, 1, Material.OPENING);
        // The overhead line, its flange over the nozzle, the tee into both headers; the blowdown branch's valve.
        double xo = x - p.rho, shoulder = headAt(p, p.rho);
        u.pipe(p.overheadOd / 2, Material.CLADDING, true, new double[]{xo, shoulder - .1, z}, new double[]{xo, p.yVapour, z}, new double[]{xo, p.yVapour, p.zVapour});
        u.mesh.cylinder(Frame.of(new double[]{xo, 0, z}, 1, 1), p.overheadOd / 2 + .08, shoulder + .3, shoulder + .4, ProcessUnit.sides(p.overheadOd + .16), Material.FLANGE);
        u.pipe(p.blowdownOd / 2, Material.PIPE, true, new double[]{xo, p.yVapour, z}, new double[]{xo, p.yVapour, p.zBlow});
        double zv = z + .55 * (p.zBlow - z), body = .8 * p.blowdownOd / 2 + .12;
        u.box(new double[]{xo - body, p.yVapour - body, zv - .18}, new double[]{xo + body, p.yVapour + body, zv + .18}, Material.MACHINE, true);
        u.mesh.cylinder(Frame.of(new double[]{xo, 0, zv}, 1, 1), .14, p.yVapour + body, p.yVapour + body + .35, 12, Material.ACTUATOR);
        // The relief valve: inlet neck, body, bonnet; its outlet to the relief header.
        double xr = x + p.rho, inlet = Math.max(.08, .3 * p.reliefOd);
        u.mesh.prism(Frame.of(new double[]{xr, 0, z}, 1, 1), inlet, shoulder - .1, p.yRelief - .22, 12, Material.PIPE);
        u.box(new double[]{xr - .22, p.yRelief - .22, z - .22}, new double[]{xr + .22, p.yRelief + .22, z + .22}, Material.ACTUATOR, true);
        u.mesh.cylinder(Frame.of(new double[]{xr, 0, z}, 1, 1), .1, p.yRelief + .22, p.yRelief + .62, 12, Material.ACTUATOR);
        u.pipe(p.reliefOd / 2, Material.PIPE, true, new double[]{xr, p.yRelief, z + .2}, new double[]{xr, p.yRelief, p.zBlow});
    }

    /** The jacket's head's height at {@code rho} from a drum's axis. */
    static double headAt(DelayedCokerPlan p, double rho) {
        double t = rho / p.jacket;
        return p.yTangent + p.jacket / 2 * Math.sqrt(Math.max(0, 1 - t * t));
    }

    // ---- Lines ----------------------------------------------------------------------------------------------------

    /**
     * Under the slab: each pair's switch valve (plug valve body on its operator) with its transfer lines to its drums'
     * feed stubs; the feed header in front of the spools and the FEED line to the front, the quench water header behind
     * them and the WATER line to the back, the steam line from the -x end joining each switch valve's feed branch from below.
     */
    static void underSlab(ProcessUnit u, DelayedCokerPlan p) {
        double s = p.support, yf = p.yFeed, zc = p.zc;
        for (int k = 0; k < p.pairs; k++) {
            double x = p.xp[k];
            u.box(new double[]{x - .45, s - 2.15, zc - .5}, new double[]{x + .45, s - 1.05, zc + .5}, Material.MACHINE, true);
            u.box(new double[]{x - .3, s - 2.75, zc - .3}, new double[]{x + .3, s - 2.15, zc + .3}, Material.ACTUATOR, true);
            for (int side = -1; side <= 1; side += 2) {
                double xd = p.xd[2 * k + (side < 0 ? 0 : 1)];
                u.pipe(p.feedOd / 2, Material.PIPE, true, new double[]{x + side * .45, yf, zc}, new double[]{xd - side * 1.3, yf, zc});
            }
            u.pipe(p.feedOd / 2, Material.PIPE, true, new double[]{x, yf, zc - .5}, new double[]{x, yf, p.zFeed});
            u.pipe(p.waterOd / 2, Material.PIPE, true, new double[]{x, yf, zc + .5}, new double[]{x, yf, p.zWater});
            u.pipe(p.steamOd / 2, Material.PIPE, true, new double[]{x, p.ySteam, p.zSteam}, new double[]{x, p.ySteam, zc - 1}, new double[]{x, yf, zc - 1});
        }
        double xf = feedX(p), lo = Math.min(p.xp[0], xf), hi = Math.max(p.xp[p.pairs - 1], xf);
        if (hi - lo > .05) {
            u.pipe(p.feedOd / 2, Material.PIPE, true, new double[]{lo, yf, p.zFeed}, new double[]{hi, yf, p.zFeed});
            cap(u, new double[]{lo, yf, p.zFeed}, 0, -1, p.feedOd);
            cap(u, new double[]{hi, yf, p.zFeed}, 0, 1, p.feedOd);
        }
        u.pipe(p.feedOd / 2, Material.PIPE, true, new double[]{xf, yf, p.zFeed}, new double[]{xf, yf, p.z0 + .2});
        double xw = waterX(p);
        u.pipe(p.waterOd / 2, Material.PIPE, true, new double[]{p.xp[p.pairs - 1], yf, p.zWater}, new double[]{xw, yf, p.zWater}, new double[]{xw, yf, p.zb - .2});
        cap(u, new double[]{p.xp[p.pairs - 1], yf, p.zWater}, 0, 1, p.waterOd);
        u.pipe(p.steamOd / 2, Material.PIPE, true, new double[]{p.x0 + .2, p.ySteam, p.zSteam}, new double[]{p.xp[p.pairs - 1], p.ySteam, p.zSteam});
        cap(u, new double[]{p.xp[p.pairs - 1], p.ySteam, p.zSteam}, 0, 1, p.steamOd);
        u.nozzles.add(ProcessUnit.end(Role.FEED, new double[]{xf, yf, p.z0 + .2}, 2, -1, p.feedOd, p.feedOd));
        u.nozzles.add(ProcessUnit.end(Role.WATER, new double[]{xw, yf, p.zb - .2}, 2, 1, p.waterOd, PipeSizes.od(p.waterOd, .1, p.waterOd)));
        u.nozzles.add(ProcessUnit.end(Role.STEAM, new double[]{p.x0 + .2, p.ySteam, p.zSteam}, 0, -1, p.steamOd, PipeSizes.od(p.steamOd, .1, p.steamOd)));
        u.nozzles.add(new Nozzle(Role.SOLIDS_OUTLET, new double[]{p.xd[0], 1.6, zc}, 1, -1, .6, SOLIDS_OD, SOLIDS_OD, 0, .03));
    }

    /** The FEED line's x: the cell centre just past the front table column at the unit's middle. */
    static double feedX(DelayedCokerPlan p) {
        return Math.floor((p.xd[0] + p.xd[p.drums - 1]) / 2 + p.concrete / 2 + .3) + .5;
    }

    /** The WATER line's x: two metres short of drum 0's axis, clear of its valve and of the stair tower behind. */
    static double waterX(DelayedCokerPlan p) {
        return p.xd[0] - 2;
    }

    /**
     * Over the heads: the vapour header in front from the VAPOUR_OUTLET at the -x end (QUENCH on its top there) to the last
     * drum's tee; the blowdown header behind to the BLOWDOWN at the +x end; the relief header over it, turning at the -x
     * end to the RELIEF at the back; the cutting water line a metre in front of the axes from drum 0's standpipe to the +x
     * end and down the riser outside it to the CUTTING_WATER at its foot.
     */
    static void overheads(ProcessUnit u, DelayedCokerPlan p) {
        double yv = p.yVapour, yr = p.yRelief, last = p.xd[p.drums - 1];
        double xa = p.x0 + .3, xq = p.x0 + 1.5, xRel = p.x0 + 1.5;
        u.pipe(p.vapourOd / 2, Material.CLADDING, true, new double[]{xa, yv, p.zVapour}, new double[]{last - p.rho + .1, yv, p.zVapour});
        cap(u, new double[]{last - p.rho + .1, yv, p.zVapour}, 0, 1, p.vapourOd);
        double b0 = p.xd[0] - p.rho - .1;
        u.pipe(p.blowdownOd / 2, Material.PIPE, true, new double[]{b0, yv, p.zBlow}, new double[]{p.x1 - .2, yv, p.zBlow});
        cap(u, new double[]{b0, yv, p.zBlow}, 0, -1, p.blowdownOd);
        double r1 = last + p.rho + .1;
        u.pipe(p.reliefOd / 2, Material.PIPE, true, new double[]{r1, yr, p.zBlow}, new double[]{xRel, yr, p.zBlow}, new double[]{xRel, yr, p.zb - .2});
        cap(u, new double[]{r1, yr, p.zBlow}, 0, 1, p.reliefOd);
        double xr = p.x1 + .5;
        u.pipe(CUT_OD / 2, Material.PIPE, true, new double[]{p.xd[0], yr, p.zCut}, new double[]{xr, yr, p.zCut}, new double[]{xr, 1.5, p.zCut}, new double[]{xr + .25, 1.5, p.zCut});
        cap(u, new double[]{p.xd[0], yr, p.zCut}, 0, -1, CUT_OD);
        u.nozzles.add(ProcessUnit.end(Role.VAPOUR_OUTLET, new double[]{xa, yv, p.zVapour}, 0, -1, p.vapourOd, PipeSizes.od(p.vapourOd, .1, p.vapourOd)));
        u.nozzles.add(ProcessUnit.end(Role.QUENCH, new double[]{xq, yv + p.vapourOd / 2 - .03, p.zVapour}, 1, 1, QUENCH_OD, QUENCH_OD));
        u.nozzles.add(ProcessUnit.end(Role.BLOWDOWN, new double[]{p.x1 - .2, yv, p.zBlow}, 0, 1, p.blowdownOd, PipeSizes.od(p.blowdownOd, .1, p.blowdownOd)));
        u.nozzles.add(ProcessUnit.end(Role.RELIEF, new double[]{xRel, yr, p.zb - .2}, 2, 1, p.reliefOd, PipeSizes.od(p.reliefOd, .1, p.reliefOd)));
        u.nozzles.add(ProcessUnit.end(Role.CUTTING_WATER, new double[]{xr + .25, 1.5, p.zCut}, 0, 1, CUT_OD, CUT_OD));
    }

    /** A blind end of a line of {@code od} at {@code at}, facing {@code sign} along frame axis {@code axis}. */
    static void cap(ProcessUnit u, double[] at, int axis, int sign, double od) {
        u.mesh.annulus(Frame.of(at, axis, sign), 0, od / 2 + .002, .002, PipeSizes.sides(od), 1, Material.PIPE);
    }
}
