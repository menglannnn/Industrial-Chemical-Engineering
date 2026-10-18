package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.Mesh.Frame;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.MotorDetail;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Motion;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Solid;
import com.wormzjl.createcheme.science.math.DeterministicMath;

/**
 * Parts the fluidized-bed boiler and gasifier share (NEW_UNITS_PLAN.md, owner decisions D1, D3, D4 of 2026-10-18;
 * {@code NEW_UNITS_SURVEY_SOLID_FUEL.md} section 1): a refractory-lined reverse-flow cyclone and its standpipe, a
 * screw or drag-chain feeder's drive, a centrifugal fan on its plinth, a turning rotary drum, and enclosed boxes. Their
 * motors, hoops, boxes and the boiler's stack are the kit's ({@link ProcessUnit#motor}, {@link Mesh#hoop},
 * {@link ProcessUnit#box}, {@link ProcessUnit#stack}). Proportions are the survey's (Basu as quoted there) unless a method
 * says otherwise.
 */
final class FluidizedBedParts {
    /** A feeder's motor radius, m, and its {@link #drive}'s length from the casing's end to the motor's fan cowl. */
    static final double FEEDER_MOTOR = .22, DRIVE_LENGTH = .6 + 4.5 * FEEDER_MOTOR;
    /** A band's faces round a box (a girth stiffener, an expansion joint's frame): its four sides, no top or bottom. */
    static final int BAND = 1 | 2 | 16 | 32;

    private FluidizedBedParts() {
    }

    /** A box that collides and encloses its inside (a duct, a casing, a bin): every face drawn. */
    static void closedBox(ProcessUnit u, double[] lo, double[] hi, Material material) {
        closedBox(u, lo, hi, Mesh.ALL_FACES, material);
    }

    /** A {@link #closedBox} drawn with {@code faces} only (an end inside the part it joins is never seen). */
    static void closedBox(ProcessUnit u, double[] lo, double[] hi, int faces, Material material) {
        u.box(lo, hi, faces, material, true);
        if (hi[0] - lo[0] > .3 && hi[1] - lo[1] > .3 && hi[2] - lo[2] > .3)
            u.volumes.add(Solid.box(new double[]{lo[0] + .1, lo[1] + .1, lo[2] + .1}, new double[]{hi[0] - .1, hi[1] - .1, hi[2] - .1}));
    }

    /**
     * A refractory-lined reverse-flow cyclone standing at (x, z), its roof at {@code roof}: barrel {@code dc} inside and
     * {@code lining} thick (its casing {@code dc / 2 + lining} round), 1.25 dc tall; the cone down to the standpipe's
     * casing 2.75 dc under the roof (Basu: barrel 1.0 to 1.5 dc, total 2 to 3.5 dc); the roof with the gas outlet's
     * casing (0.5 dc inside) rising {@code stub} over it; the standpipe (0.2 dc inside) from the cone down to
     * {@code standpipeBottom}. Clad, collides and encloses its inside. Returns the cone's foot.
     */
    static double cyclone(ProcessUnit u, double x, double z, double roof, double dc, double lining, double stub, double standpipeBottom, int n) {
        double r = dc / 2 + lining, barrel = roof - 1.25 * dc, foot = roof - 2.75 * dc, rs = .1 * dc + .15, ro = .25 * dc + .15;
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        double[] axis = {x, 0, z};
        u.mesh.prism(up, r, barrel, roof, n, Material.CLADDING);
        u.mesh.annulus(up, ro, r, roof, n, 1, Material.CLADDING);
        u.mesh.frustum(up, rs + .05, r, foot, barrel, n, Material.CLADDING);
        // A stiffening band at the barrel's foot.
        u.mesh.hoop(up, r, r + .06, barrel, barrel + .12, n, Material.CLADDING);
        int ns = ProcessUnit.sides(2 * ro);
        u.mesh.prism(up, ro, roof, roof + stub, ns, Material.CLADDING);
        u.mesh.prism(Frame.of(axis, 1, 1), rs, standpipeBottom, foot + .02, ProcessUnit.sides(2 * rs), Material.CLADDING);
        u.mesh.hoop(up, rs, rs + .08, foot - .3, foot - .18, ProcessUnit.sides(2 * rs), Material.FLANGE);
        u.solids.add(Solid.rod(axis, 1, barrel, roof, r, 0));
        u.solids.add(Solid.cone(axis, 1, foot, barrel, rs + .05, r, 0));
        u.solids.add(Solid.rod(axis, 1, standpipeBottom, foot, rs, 0));
        u.solids.add(Solid.rod(axis, 1, roof, roof + stub, ro, 0));
        u.volumes.add(Solid.rod(axis, 1, barrel, roof - .1, .94 * r, 0));
        u.volumes.add(Solid.rod(axis, 1, roof - .3, roof + stub + .2, .9 * ro, 0));
        u.volumes.add(Solid.rod(axis, 1, standpipeBottom - .2, foot + .3, .85 * rs, 0));
        u.volumes.add(Solid.cone(axis, 1, foot + .2, barrel, .9 * rs, .94 * r, 0));
        u.round(x, z, barrel, roof, r, 1);
        u.round(x, z, standpipeBottom, foot, rs, 1);
        return foot;
    }

    /**
     * A feeder's or a screw's drive at {@code end} of its casing, on its axis at (x, y), standing towards -z from it: a
     * helical gearbox on a bracket down to {@code floor}, the coupling shaft turning in the gap (its collar's set screws
     * show it turning: {@link ProcessUnit#shaft}), the kit's plain TEFC motor ({@link MotorDetail#PLAIN}: a small drive
     * among large parts) on its feet. {@code r} is the motor's radius (0.15 to 0.4 m). Returns the drive's far end along z.
     */
    static double drive(ProcessUnit u, double x, double y, double end, double r, double floor) {
        double gear = .9 * r + .15, lg = 1.6 * r + .2, shaft = .03 + .08 * r, gap = .25 + .3 * r, lm = 2.6 * r + .15;
        double g0 = end - lg, s1 = g0 - gap, m1 = s1 - lm;
        u.box(new double[]{x - gear, y - gear, g0}, new double[]{x + gear, y + .8 * gear, end}, Material.MACHINE, true);
        if (y - gear - floor > .05) u.box(new double[]{x - .9 * r, floor, g0 + .05}, new double[]{x + .9 * r, y - gear, end - .05}, Material.STEELWORK, false);
        u.shaft(Frame.of(new double[]{x, y, 0}, 2, 1), shaft, new double[][]{{s1, g0}}, (g0 + s1) / 2);
        // The motor's frame runs from its drive end at s1 towards -z.
        u.motor(Frame.of(new double[]{x, y, 0}, 2, -1), r, -s1, -m1, 0, floor, MotorDetail.PLAIN);
        return m1;
    }

    /**
     * A centrifugal fan at grade, its shaft along z at (x, y) (owner D4: the PA, SA and ID fans the boiler stands with;
     * survey: scrolls 3 to 6 m high): the volute ({@link ProcessUnit#scroll}) from {@code z0}, {@code 0.6 r1} wide,
     * growing from {@code 0.62 r1} to {@code r1} round a hub of {@code 0.5 r1} whose eye faces the front (-z), its throat
     * turning up; an inlet bell and, when {@code silencer}, an inlet silencer in front of it (the air's way in, survey;
     * without one the caller closes the bell's mouth with an inlet box);
     * behind it the bearing pedestal, the coupling shaft turning and the kit's finned motor ({@link MotorDetail#FINNED}:
     * twelve fins and a plain cowl, 3 000 quads fewer than a full one on a boiler's three fans) of radius {@code rm}; a
     * concrete plinth under them. Returns {front of the inlet, the casing's back, the motor's end}.
     */
    static double[] fan(ProcessUnit u, double x, double y, double z0, double r1, double rm, boolean silencer) {
        double r0 = .62 * r1, rh = .5 * r1, w = .6 * r1, z1 = z0 + w, zh = z0 + .25 * w, bell = .3 + .2 * r1;
        u.scroll(x, y, z0, z1, zh, rh, r0, r1);
        var f = Frame.of(new double[]{x, y, 0}, 2, 1);
        // Each round part's sides from its own size (the scroll's would put 64 on the ID fan's hub).
        int nh = ProcessUnit.sides(2 * rh), n = ProcessUnit.sides(2.7 * rh);
        u.mesh.annulus(f, 0, rh, zh, nh, -1, Material.OPENING);
        u.mesh.annulus(f, 0, rh, z1, nh, 1, Material.MACHINE);
        // The inlet bell (its mouth against the silencer or the caller's inlet box, so its inside is not drawn), and the
        // silencer's drum in front of it with its dark face.
        u.mesh.frustum(f, 1.25 * rh, rh, z0 - bell, z0, n, Material.MACHINE);
        double front = z0 - bell;
        if (silencer) {
            double ls = .6 + .5 * r1;
            u.mesh.prism(f, 1.35 * rh, front - ls, front, n, Material.MACHINE);
            u.mesh.annulus(f, 1.25 * rh, 1.35 * rh, front, n, 1, Material.MACHINE);
            u.mesh.annulus(f, .2 * rh, 1.35 * rh, front - ls, n, -1, Material.OPENING);
            u.mesh.cylinder(f, .2 * rh, front - ls - .05, front - ls + .3, 12, Material.MACHINE);
            u.solids.add(Solid.rod(new double[]{x, y, 0}, 2, front - ls, front, 1.35 * rh, 0));
            front -= ls;
        }
        u.solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0 - bell, z1, r1, 0));
        u.round(x, y, z0, z1, r1);
        // The casing's feet down to the plinth.
        double bottom = y - (r0 + r1) / 2, plinth = .3;
        for (int side = -1; side <= 1; side += 2)
            u.box(new double[]{x + side * .35 * r1 - .15, plinth, z0 + .05}, new double[]{x + side * .35 * r1 + .15, bottom + .1, z1 - .05}, Material.MACHINE, false);
        // The bearing pedestal, the coupling shaft and the motor.
        double shaft = .04 + .03 * r1, zb = z1 + .3 + .15 * r1, lm = 3.2 * rm + .1, zm0 = zb + .35 + .2 * r1, zm1 = zm0 + lm;
        u.box(new double[]{x - .25 - .1 * r1, plinth, z1 + .05}, new double[]{x + .25 + .1 * r1, y - .12, zb}, Material.MACHINE, false);
        u.mesh.cylinder(f, .18 + .06 * r1, z1 + .05, zb, 12, Material.MACHINE);
        u.shaft(f, shaft, new double[][]{{zb, zm0 - .1}}, (zb + zm0) / 2);
        u.motor(f, rm, zm0, zm1, shaft, plinth, MotorDetail.FINNED);
        // The plinth.
        double half = Math.max(.6 * r1, rm + .3);
        u.box(new double[]{x - half, 0, z0 - bell - .1}, new double[]{x + half, plinth, zm1 + .2}, Mesh.ALL_FACES & ~4, Material.CONCRETE, true);
        return new double[]{front, z1, zm1};
    }

    /** The throat's sides across x of a {@link #fan} at x of size r1: from just past the top on the left to the cutwater. */
    static double[] throat(double x, double r1) {
        return new double[]{x - .12 * r1, x + .4 * r1};
    }

    /**
     * A rotary drum lying along z at (x, y) from {@code z0} to {@code z1}, {@code d} across, turning at {@code speed}
     * (a moving part, unpainted): its shell, two riding rings and a girth gear, and four bolted strakes along it so its
     * turning shows. The still parts (rollers, hoods, drive) are the caller's.
     */
    static void drum(ProcessUnit u, double x, double y, double z0, double z1, double d, double speed, double phase) {
        double r = d / 2, length = z1 - z0;
        var f = Frame.of(new double[]{x, y, 0}, 2, 1);
        int n = ProcessUnit.sides(d);
        u.mesh.part(u.motions.size());
        u.mesh.cylinder(f, r, z0, z1, n, Material.PIPE);
        for (double t : new double[]{.18, .82}) u.mesh.hoop(f, r, r + .07, z0 + t * length - .12, z0 + t * length + .12, n, Material.FLANGE);
        u.mesh.hoop(f, r, r + .1, z0 + .5 * length - .08, z0 + .5 * length + .08, n, Material.BOLT);
        for (int k = 0; k < 4; k++) {
            double t = Math.PI / 2 * k + Math.PI / 4;
            var p = f.at(t, r + .025, z0 + .3);
            var q = f.at(t, r + .025, z1 - .3);
            u.mesh.bar(p, q, .05, new double[]{DeterministicMath.cos(t), DeterministicMath.sin(t), 0}, Material.MOTOR);
        }
        u.mesh.part(-1);
        u.motions.add(Motion.turning(new double[]{x, y, (z0 + z1) / 2}, new double[]{0, 0, 1}, speed, phase));
        u.solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0, z1, r + .07, 0));
        u.round(x, y, z0, z1, r);
    }
}
