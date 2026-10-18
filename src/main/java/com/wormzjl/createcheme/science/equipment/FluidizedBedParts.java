package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.Mesh.Frame;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Motion;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Solid;
import com.wormzjl.createcheme.science.math.DeterministicMath;

/**
 * Parts the fluidized-bed boiler and gasifier share (NEW_UNITS_PLAN.md, owner decisions D1, D3, D4 of 2026-10-18;
 * {@code NEW_UNITS_SURVEY_SOLID_FUEL.md} section 1): a refractory-lined reverse-flow cyclone and its standpipe, a
 * screw or drag-chain feeder's drive, a centrifugal fan on its plinth, a stack, a turning rotary drum, and enclosed
 * boxes. Proportions are the survey's (Basu as quoted there) unless a method says otherwise.
 */
final class FluidizedBedParts {
    /** A feeder's motor radius, m, and its {@link #drive}'s length from the casing's end to the motor's fan cowl. */
    static final double FEEDER_MOTOR = .22, DRIVE_LENGTH = .6 + 4.5 * FEEDER_MOTOR;

    private FluidizedBedParts() {
    }

    /** A box that collides and encloses its inside (a duct, a casing, a bin): every face drawn. */
    static void closedBox(ProcessUnit u, double[] lo, double[] hi, Material material) {
        u.box(lo, hi, material, true);
        if (hi[0] - lo[0] > .3 && hi[1] - lo[1] > .3 && hi[2] - lo[2] > .3)
            u.volumes.add(Solid.box(new double[]{lo[0] + .1, lo[1] + .1, lo[2] + .1}, new double[]{hi[0] - .1, hi[1] - .1, hi[2] - .1}));
    }

    /** A box seen only from outside (a beam, a hood): every face drawn, collides. */
    static void box(ProcessUnit u, double x0, double y0, double z0, double x1, double y1, double z1, Material material) {
        u.box(new double[]{Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1)}, new double[]{Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1)}, material, true);
    }

    /** A box drawn only (trim, a buckstay's flange, a door): no collision. */
    static void trim(ProcessUnit u, double x0, double y0, double z0, double x1, double y1, double z1, Material material) {
        u.box(new double[]{Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1)}, new double[]{Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1)}, material, false);
    }

    /**
     * A band round a box (a girth stiffener, an expansion joint's frame, a bin's rim): its four sides only, no collision;
     * its top and bottom would each cost a quad per cell of the box's plan.
     */
    static void band(ProcessUnit u, double x0, double y0, double z0, double x1, double y1, double z1, Material material) {
        u.mesh.box(new double[]{x0, y0, z0}, new double[]{x1, y1, z1}, 1 | 2 | 16 | 32, material);
    }

    /**
     * A hoop round a round body of radius {@code rIn} (a stiffening band, a riding ring, a flange) on frame {@code f}
     * from {@code z0} to {@code z1}: its rim and both faces; its bore lies on the body and is not drawn.
     */
    static void hoop(ProcessUnit u, Frame f, double rIn, double rOut, double z0, double z1, int n, Material material) {
        u.mesh.prism(f, rOut, z0, z1, n, material);
        u.mesh.annulus(f, rIn, rOut, Math.min(z0, z1), n, -1, material);
        u.mesh.annulus(f, rIn, rOut, Math.max(z0, z1), n, 1, material);
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
        hoop(u, up, r, r + .06, barrel, barrel + .12, n, Material.CLADDING);
        int ns = ProcessUnit.sides(2 * ro);
        u.mesh.prism(up, ro, roof, roof + stub, ns, Material.CLADDING);
        u.mesh.prism(Frame.of(axis, 1, 1), rs, standpipeBottom, foot + .02, ProcessUnit.sides(2 * rs), Material.CLADDING);
        hoop(u, up, rs, rs + .08, foot - .3, foot - .18, ProcessUnit.sides(2 * rs), Material.FLANGE);
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
     * A feeder's or a screw's drive at {@code end} of its casing, on its axis at (x, y), the drive standing towards
     * {@code -sign} along z from it: a helical gearbox, the coupling shaft turning in the gap (its collar's set screws
     * show it turning: {@link ProcessUnit#shaft}), the TEFC motor with its fan cowl and feet on a bracket down to
     * {@code floor}. {@code r} is the motor's radius (0.15 to 0.4 m). Returns the drive's far end along z.
     */
    static double drive(ProcessUnit u, double x, double y, double end, int sign, double r, double floor) {
        double gear = .9 * r + .15, lg = 1.6 * r + .2, shaft = .03 + .08 * r, gap = .25 + .3 * r, lm = 2.6 * r + .15;
        double g0 = end - sign * lg, s1 = g0 - sign * gap, m1 = s1 - sign * lm;
        trim(u, x - gear, y - gear, end, x + gear, y + .8 * gear, g0, Material.MACHINE);
        u.solids.add(Solid.box(new double[]{x - gear, y - gear, Math.min(end, g0)}, new double[]{x + gear, y + .8 * gear, Math.max(end, g0)}));
        var f = Frame.of(new double[]{x, y, 0}, 2, 1);
        u.shaft(f, shaft, new double[][]{{Math.min(g0, s1), Math.max(g0, s1)}}, (g0 + s1) / 2);
        // The motor: body, end shield, fan cowl with its grille; feet and a bracket to the floor.
        double cowl = s1 - sign * .75 * lm;
        u.mesh.cylinder(f, r, Math.min(s1, cowl), Math.max(s1, cowl), 12, Material.MOTOR);
        u.mesh.frustum(f, 1.08 * r, .95 * r, cowl, m1, 12, Material.MOTOR);
        u.mesh.annulus(f, 0, .95 * r, m1, 12, -sign, Material.OPENING);
        u.mesh.annulus(f, .95 * r, 1.08 * r, m1, 12, -sign, Material.MOTOR);
        trim(u, x - .3 * r, y + r - .02, s1 - sign * .2 * lm, x + .3 * r, y + 1.4 * r, s1 - sign * .45 * lm, Material.MOTOR);
        double low = Math.min(y - gear, y - r);
        if (low - floor > .05) trim(u, x - .9 * r, floor, Math.min(end, m1), x + .9 * r, low, Math.max(end, m1), Material.STEELWORK);
        u.solids.add(Solid.rod(new double[]{x, y, 0}, 2, Math.min(s1, m1), Math.max(s1, m1), 1.08 * r, 0));
        return m1;
    }

    /**
     * A centrifugal fan at grade, its shaft along z at (x, y) (owner D4: the PA, SA and ID fans the boiler stands with;
     * survey: scrolls 3 to 6 m high): the volute ({@link ProcessUnit#scroll}) from {@code z0}, {@code 0.6 r1} wide,
     * growing from {@code 0.62 r1} to {@code r1} round a hub of {@code 0.5 r1} whose eye faces the front (-z), its throat
     * turning up; an inlet bell and, when {@code silencer}, an inlet silencer in front of it (the air's way in, survey;
     * without one the caller closes the bell's mouth with an inlet box);
     * behind it the bearing pedestal, the coupling shaft turning and the motor ({@link ProcessUnit#motor}) of radius
     * {@code rm}; a concrete plinth under them. Returns {front of the inlet, the casing's back, the motor's end}.
     */
    static double[] fan(ProcessUnit u, double x, double y, double z0, double r1, double rm, boolean silencer) {
        double r0 = .62 * r1, rh = .5 * r1, w = .6 * r1, z1 = z0 + w, zh = z0 + .25 * w, bell = .3 + .2 * r1;
        u.scroll(x, y, z0, z1, zh, rh, r0, r1);
        var f = Frame.of(new double[]{x, y, 0}, 2, 1);
        int n = ProcessUnit.sides(2 * r1);
        u.mesh.annulus(f, 0, rh, zh, n, -1, Material.OPENING);
        u.mesh.annulus(f, 0, rh, z1, n, 1, Material.MACHINE);
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
            trim(u, x + side * .35 * r1 - .15, plinth, z0 + .05, x + side * .35 * r1 + .15, bottom + .1, z1 - .05, Material.MACHINE);
        // The bearing pedestal, the coupling shaft and the motor.
        double shaft = .04 + .03 * r1, zb = z1 + .3 + .15 * r1, lm = 3.2 * rm + .1, zm0 = zb + .35 + .2 * r1, zm1 = zm0 + lm;
        trim(u, x - .25 - .1 * r1, plinth, z1 + .05, x + .25 + .1 * r1, y - .12, zb, Material.MACHINE);
        u.mesh.cylinder(f, .18 + .06 * r1, z1 + .05, zb, 12, Material.MACHINE);
        u.shaft(f, shaft, new double[][]{{zb, zm0 - .1}}, (zb + zm0) / 2);
        motor(u, f, x, y, rm, zm0, zm1, shaft, plinth);
        // The plinth.
        double half = Math.max(.6 * r1, rm + .3);
        trim(u, x - half, 0, z0 - bell - .1, x + half, plinth, zm1 + .2, Material.CONCRETE);
        u.solids.add(Solid.box(new double[]{x - half, 0, z0 - bell - .1}, new double[]{x + half, plinth, zm1 + .2}));
        return new double[]{front, z1, zm1};
    }

    /**
     * A fan's TEFC motor on frame {@code f} (its shaft at (x, y), along z) of radius {@code r} from {@code z0} to
     * {@code z1}: the body with twelve axial fins round all but its underside, the fan cowl (the last 18 %) with its
     * grille, the shaft's end, the terminal box on top, a pedestal down to {@code floor}. The kit's
     * {@link ProcessUnit#motor} draws 16 to 32 fins and a turning fan; at a boiler's three fans that is 3 000 quads.
     */
    static void motor(ProcessUnit u, Frame f, double x, double y, double r, double z0, double z1, double shaft, double floor) {
        double cowl = z1 - .18 * (z1 - z0), fin = .025 + .06 * r, rc = r + fin;
        int n = ProcessUnit.sides(2 * r);
        u.mesh.prism(f, r, z0, cowl, n, Material.MOTOR);
        u.mesh.annulus(f, 0, r, z0, n, -1, Material.MOTOR);
        u.mesh.prism(f, shaft, z0 - .1, z0, 8, Material.PIPE);
        for (int k = 0; k < 12; k++) {
            double angle = Math.PI / 2 + Math.PI / 6 * k;
            if (ProcessUnit.apart(angle, -Math.PI / 2) < .9) continue;
            double[] radial = {DeterministicMath.cos(angle), DeterministicMath.sin(angle), 0};
            u.mesh.block(f.at(angle, r + fin / 2 - .01, (z0 + cowl) / 2), new double[]{radial[0] * fin / 2, radial[1] * fin / 2, 0},
                    new double[]{0, 0, (cowl - z0) / 2 - .03}, new double[]{-radial[1] * .015, radial[0] * .015, 0}, Material.MOTOR);
        }
        u.mesh.annulus(f, r, rc, cowl, n, -1, Material.MOTOR);
        u.mesh.prism(f, rc, cowl, z1, n, Material.MOTOR);
        u.mesh.annulus(f, 0, rc, z1, n, 1, Material.GUARD);
        trim(u, x - .35 * r, y + r - .02, z0 + .25 * (z1 - z0), x + .35 * r, y + 1.4 * r, z0 + .25 * (z1 - z0) + .5 * r, Material.MOTOR);
        double feet = y - .6 * r;
        if (feet > floor + .01) box(u, x - .85 * r, floor, z0 + .05, x + .85 * r, feet, cowl - .05, Material.STEELWORK);
        u.solids.add(Solid.rod(new double[]{x, y, 0}, 2, z0, z1, rc, 0));
        u.round(x, y, z0, z1, r);
    }

    /** The throat's sides across x of a {@link #fan} at x of size r1: from just past the top on the left to the cutwater. */
    static double[] throat(double x, double r1) {
        return new double[]{x - .12 * r1, x + .4 * r1};
    }

    /**
     * A steel stack of {@code od} round (x, z) from its concrete foundation to {@code top}: a flanged joint every 12 m, a
     * band under its top, open at the top, a lightning rod on its rim. The flue gas stays in the unit (survey: a 30 MWth
     * boiler's 1.2 m duct does not fit a 1 m pipe), as on the fired heaters.
     */
    static void stack(ProcessUnit u, double x, double z, double od, double top) {
        double r = od / 2, base = .6;
        int n = Math.max(16, Math.min(24, 4 * (int) Math.ceil(.6 * Math.PI * od)));
        closedBox(u, new double[]{x - r - .6, 0, z - r - .6}, new double[]{x + r + .6, base, z + r + .6}, Material.CONCRETE);
        var up = Frame.of(new double[]{x, 0, z}, 1, 1);
        u.mesh.prism(up, r, base, top, n, Material.SHELL);
        // The joints' flanges as plain bands (their ledges cost a quad per cell they cross: 3 000 on a 6 m stack).
        for (double y = base + 12; y < top - 3; y += 12) u.mesh.prism(up, r + .05, y, y + .15, n, Material.HEATER);
        u.band(up, r, top - .6, n);
        u.mesh.annulus(up, .92 * r, r, top, n, 1, Material.SHELL);
        u.mesh.bore(up, .92 * r, top - .3, top, n, Material.SHELL);
        u.mesh.annulus(up, 0, .92 * r, top - .3, n, 1, Material.OPENING);
        u.mesh.bar(new double[]{x + .96 * r, top, z}, new double[]{x + .96 * r, top + 1.2, z}, .03, new double[]{1, 0, 0}, Material.PIPE);
        u.solids.add(Solid.rod(new double[]{x, 0, z}, 1, base, top, r, 0));
        u.round(x, z, base, top, r, 1);
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
        for (double t : new double[]{.18, .82}) hoop(u, f, r, r + .07, z0 + t * length - .12, z0 + t * length + .12, n, Material.FLANGE);
        hoop(u, f, r, r + .1, z0 + .5 * length - .08, z0 + .5 * length + .08, n, Material.BOLT);
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
