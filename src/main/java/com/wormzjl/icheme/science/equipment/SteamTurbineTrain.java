package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.Mesh.Frame;
import com.wormzjl.icheme.science.equipment.Mesh.Material;
import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.icheme.science.equipment.Mesh.ALL_FACES;

/**
 * A steam turbine-generator set's train (owner decisions D2 and D4 2026-10-18, {@code NEW_UNITS_PLAN.md}; its proportions
 * from {@code NEW_UNITS_SURVEY_ROTATING.md} section 2), shared by the back-pressure set at grade
 * ({@link SteamTurbineDesign}) and the condensing set on its table ({@link CondensingTurbineDesign}). Its shaft runs along
 * frame z; from the front: the front standard over the front bearing (the governor actuator and the overspeed trip on
 * its face), the turbine casing, the rear bearing pedestal, the high-speed coupling under its guard, the parallel-shaft
 * gear (sets up to 50-65 MW are geared, Siemens SST-100 to 400), the low-speed coupling, the 4-pole generator under its
 * TEWAC cooler, the outboard bearing and the brushless exciter. Across the top of the casing's HP end lies the steam
 * chest with its governor valves; beside it stands the trip-and-throttle valve, which with the steam chest is the set's
 * signature from 10 to 30 m.
 *
 * <p>Proportions in Dc, the exhaust-end casing's outside diameter (survey, typ.): the HP end 0.8 Dc (back-pressure) or
 * 0.65 Dc (condensing), joined to the exhaust end by a cone; (0.5 + 0.15 N) Dc between the end walls for N stages, a
 * third of it the HP barrel under its aluminium cladding and half the bare, ribbed exhaust barrel; the horizontal split
 * flange 0.08 Dc proud with its studs from Dc 1 m (smaller ones are left out first); the shaft 0.6 Dc over the base frame
 * or 0.45 Dc over the table (the exhaust casing's lower half sits in the deck opening); the front standard 0.6 Dc long;
 * the gear 0.6 Dc long, 1.0 Dc wide and 0.9 Dc tall, its pinion on the turbine's shaft line; the generator's shaft
 * {@link #generatorShaft} high, its body 3.5 Hg long, 2.2 Hg wide and 2.0 Hg tall under a 0.6 Hg cooler, the exciter
 * 0.6 Hg long. The steam chest, 0.25 Dc across and 0.8 Dc long, carries one governor valve (N 3 at most) to five under a
 * bar-lift bridge 0.2 Dc high worked by a hydraulic servo 0.13 Dc across; a single-stage turbine (API 611) has one
 * governor valve on a lever and one to three hand valves with handwheels of 0.15 to 0.25 m. The trip-and-throttle valve's
 * body is 0.25 Dc, its handwheel 0.4 to 0.6 m, its hydraulic trip cylinder beside its yoke, its steam line 0.15 Dc.
 * Gland condenser, drains, lube console and instrument tubing are left out (survey: leave out first). The exciter's fan
 * turns behind its grille, the one part seen moving (survey: shafts are enclosed, the turning gear runs only at shutdown).
 */
final class SteamTurbineTrain {
    /** The generator's shaft height over its feet, m: half the exhaust casing, 0.4 to 1.4 m (ABB 6 to 85 MVA on frames 710 to 1400 mm). */
    static double generatorShaft(double d) {
        return Math.max(.4, Math.min(1.4, .5 * d));
    }

    /** A set's power, MW: about 5 Dc² (survey, within a factor of 2; 13 MW at about 1.6 m). */
    static double power(double d) {
        return 5 * d * d;
    }

    /**
     * The turbine's speed, rev/min, to the next 100: the last stage's tip (about 0.8 Dc across) at 300 m/s, 3,000 to
     * 13,500 rpm (API 612 sets; the 13 MW set turns 4,500 rpm), geared down to the generator.
     */
    static double speed(double d) {
        return Math.round(Math.max(3000, Math.min(13500, 7200 / d)) / 100) * 100.0;
    }

    final double d, hp, casing, rise, hg, flange;
    final int stages, valves;
    final boolean condensing;
    /** Stations along z from the front standard's front face: its back, the casing's HP end wall, the HP barrel's end, the cone's end, the exhaust end wall, the rear pedestal, the gear, the generator, the outboard bearing's end, the exciter. */
    final double standard, hpEnd, hpBarrel, coneEnd, exhaustEnd, pedestal0, pedestal1, gear0, gear1, generator0, generator1, bearing1, exciter0, exciter1;
    /** The exhaust barrel's middle, the condensing exhaust neck's length along z, the steam chest's axis. */
    final double exhaust, neck, chest;

    SteamTurbineTrain(double d, int stages, boolean condensing) {
        this.d = d;
        this.stages = stages;
        this.condensing = condensing;
        hp = (condensing ? .65 : .8) * d;
        casing = (.5 + .15 * stages) * d;
        rise = (condensing ? .45 : .6) * d;
        hg = generatorShaft(d);
        flange = .08 * d;
        valves = Math.max(1, Math.min(5, (stages - 1) / 2));
        standard = .6 * d;
        hpEnd = standard + .08 * d + .04;
        hpBarrel = hpEnd + .3 * casing;
        coneEnd = hpEnd + .5 * casing;
        exhaustEnd = hpEnd + casing;
        pedestal0 = exhaustEnd + .08 * d + .03;
        pedestal1 = pedestal0 + .25 * d + .05;
        gear0 = pedestal1 + .2 * d + .1;
        gear1 = gear0 + .6 * d;
        generator0 = gear1 + .25 * hg + .1;
        generator1 = generator0 + 3.5 * hg;
        bearing1 = generator1 + .25 * hg + .1;
        exciter0 = bearing1 + .02;
        exciter1 = exciter0 + .6 * hg;
        exhaust = (coneEnd + exhaustEnd) / 2;
        neck = Math.max(.16 * d, Math.min(.8 * d, exhaustEnd - coneEnd - .1 * d));
        chest = hpEnd + Math.max(.14 * d, .15 * casing);
    }

    double length() {
        return exciter1;
    }

    /** The steam chest's axis over the base. */
    double chestAxis() {
        return rise + hp / 2 + .08 * d;
    }

    double chestRadius() {
        return .125 * d;
    }

    /** The trip-and-throttle valve's body radius, its handwheel across, the steam line's outside diameter (0.15 Dc, 0.05 m at least). */
    double valveBody() {
        return Math.max(.06, .125 * d);
    }

    double handwheel() {
        return Math.max(.4, Math.min(.6, .15 * d + .3));
    }

    double steamLine() {
        return Math.max(.05, .15 * d);
    }

    /** The least offset of the trip-and-throttle valve from the shaft along +x: clear of the steam chest's end and the HP casing's split flange. */
    double valveOffset() {
        return Math.max(.4 * d + .2 + valveBody(), hp / 2 + flange + valveBody() + .08);
    }

    /** The governor actuator's reach in front of the front standard. */
    double frontReach() {
        return .8 * Math.max(.06, .1 * d) + .02;
    }

    /** How far the train reaches to -x from its shaft: the servo, the gear's feet, the generator's feet, the split flange. */
    double leftReach() {
        return Math.max(Math.max(.63 * d + .02, .56 * d + .05), Math.max(1.2 * hg + .02, .5 * d + flange + .02));
    }

    /** How far the train reaches to +x with its trip-and-throttle valve {@code offset} from the shaft: the valve, the generator's terminal box and cooler lines. */
    double rightReach(double offset) {
        return Math.max(Math.max(leftReach(), 1.24 * hg + .2), Math.max(offset + Math.max(valveBody() + .1, handwheel() / 2) + .02, 1.5 * hg + .02));
    }

    // ---- Drawing ----------------------------------------------------------------------------------------------------

    /**
     * Draws the train on {@code base} (the base frame's or the table's top), its shaft along z at x {@code a}, its front
     * standard's front face at {@code z0}; the trip-and-throttle valve's body centred at (xv, yv, zv), its inlet towards
     * +x (back-pressure, at grade) or from below (condensing, an angle body), its outlet to the steam chest's +x end.
     */
    void draw(ProcessUnit u, double a, double base, double z0, double xv, double yv, double zv) {
        var m = u.mesh;
        double shaft = base + rise, gShaft = base + hg, r = d / 2, e = flange;
        int n = ProcessUnit.sides(d);
        var f = Frame.of(new double[]{a, shaft, 0}, 2, 1);
        double zs = z0 + standard, zh = z0 + hpEnd, z1 = z0 + hpBarrel, z2 = z0 + coneEnd, ze = z0 + exhaustEnd;

        // The front standard over the front bearing and its cap; the governor actuator and the overspeed trip on its face.
        double sx = Math.max(.2, .32 * d), top = shaft + Math.max(.12, .2 * d), ga = Math.max(.06, .1 * d);
        u.box(new double[]{a - sx, base, z0}, new double[]{a + sx, top, zs}, Material.MACHINE, true);
        u.box(new double[]{a - .6 * sx, top, z0 + .25 * standard}, new double[]{a + .6 * sx, top + .02 + .04 * d, zs - .25 * standard}, Material.MACHINE, false);
        u.box(new double[]{a - ga, shaft + .02, z0 - .8 * ga}, new double[]{a + ga, shaft + .02 + 1.8 * ga, z0}, Material.ACTUATOR, true);
        m.cylinder(Frame.of(new double[]{a + .55 * sx, shaft - .05 * d - .05, 0}, 2, 1), .04 * d + .02, z0 - .05 * d - .03, z0, 10, Material.MACHINE);

        // The casing: the clad HP end wall, barrel and cone, the bare exhaust barrel and its end wall; the cladding's edge
        // over the bare casing, the exhaust barrel's girth ribs; the glands at both ends.
        m.annulus(f, 0, hp / 2, zh, n, -1, Material.CLADDING);
        m.prism(f, hp / 2, zh, z1, n, Material.CLADDING);
        if (hp < d) m.frustum(f, hp / 2, r, z1, z2, n, Material.CLADDING);
        m.prism(f, r, z2, ze, n, Material.MACHINE);
        m.annulus(f, 0, r, ze, n, 1, Material.MACHINE);
        m.hoop(f, r, r + .02 + .01 * d, z2, z2 + .04, n, Material.CLADDING);
        if (d >= 1) {
            int ribs = Math.max(1, (int) Math.round((ze - z2) / (.35 * d)));
            for (int k = 1; k <= ribs; k++) {
                double z = z2 + (ze - z2) * k / (ribs + 1), w = .02 + .005 * d;
                m.hoop(f, r, r + .025 * d, z - w, z + w, n, Material.MACHINE);
            }
        }
        m.prism(f, .1 * d + .02, zs, zh, n, Material.MACHINE);
        m.prism(f, .1 * d + .02, ze, z0 + pedestal0, n, Material.MACHINE);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{a, shaft, 0}, 2, zh, ze, r, 0));
        u.volumes.add(ProcessUnit.Solid.rod(new double[]{a, shaft, 0}, 2, zh + .05, ze - .05, .9 * hp / 2, 0));
        u.round(a, shaft, z2, ze, r);
        u.round(a, shaft, zh, z1, hp / 2);

        // The horizontal split flange along both sides, its studs on top from 1 m; the casing's feet under it at the cone's end.
        double th = Math.max(.05, .1 * d);
        for (int side = -1; side <= 1; side += 2) {
            ledge(m, a, side, zh, hp / 2, z1, hp / 2, shaft - th / 2, shaft + th / 2, e, true, false, Material.CLADDING);
            if (hp < d) ledge(m, a, side, z1, hp / 2, z2, r, shaft - th / 2, shaft + th / 2, e, false, false, Material.CLADDING);
            ledge(m, a, side, z2, r, ze, r, shaft - th / 2, shaft + th / 2, e, false, true, Material.MACHINE);
            if (d >= 1) {
                int studs = (int) Math.floor((ze - zh) / .3);
                for (int k = 0; k < studs; k++) {
                    double z = zh + (ze - zh) * (k + .5) / studs, x = a + side * (radius(z, zh, z1, z2) + .55 * e);
                    m.box(new double[]{x - .025, shaft + th / 2, z - .025}, new double[]{x + .025, shaft + th / 2 + .06, z + .025}, ALL_FACES & ~4, Material.BOLT);
                }
            }
            double zf1 = z2 - .06, zf0 = Math.max(zh + .02, zf1 - .2 * d), x = a + side * (radius((zf0 + zf1) / 2, zh, z1, z2) + .55 * e), w = .02 + .02 * d;
            u.box(new double[]{x - w, base, zf0}, new double[]{x + w, shaft - th / 2, zf1}, Material.MACHINE, true);
        }

        // The rear bearing pedestal and its cap.
        double px = Math.max(.15, .25 * d), p0 = z0 + pedestal0, p1 = z0 + pedestal1, pt = shaft + .15 * d;
        u.box(new double[]{a - px, base, p0}, new double[]{a + px, pt, p1}, Material.MACHINE, true);
        u.box(new double[]{a - .6 * px, pt, p0 + .2 * (p1 - p0)}, new double[]{a + .6 * px, pt + .02 + .03 * d, p1 - .2 * (p1 - p0)}, Material.MACHINE, false);

        // The high-speed coupling's guard; the gear.
        guard(u, a, base, shaft, .14 * d + .06, p1, z0 + gear0);
        gear(u, a, base, shaft, gShaft, z0 + gear0, z0 + gear1);
        guard(u, a, base, gShaft, .15 * hg + .07, z0 + gear1, z0 + generator0);

        generator(u, a, base, gShaft, z0 + generator0, z0 + generator1, z0 + bearing1, z0 + exciter0, z0 + exciter1);
        steamChest(u, a, base + chestAxis(), z0 + chest);
        tripThrottle(u, a, xv, yv, zv, base + chestAxis(), z0 + chest);
    }

    /** The casing's radius at station {@code z}: the HP barrel's, the cone's between, the exhaust barrel's. */
    private double radius(double z, double zh, double z1, double z2) {
        if (z <= z1) return hp / 2;
        if (z >= z2) return d / 2;
        return hp / 2 + (d - hp) / 2 * (z - z1) / (z2 - z1);
    }

    /**
     * A length of the split flange on the casing's {@code side} (±x of {@code a}) from station {@code za} (the casing's
     * radius there {@code ra}) to {@code zb} ({@code rb}), {@code out} proud of the casing between heights y0 and y1: its
     * top, underside and outer face, and the end faces asked for (a cone's flange is a slanted hexahedron).
     */
    private static void ledge(Mesh m, double a, int side, double za, double ra, double zb, double rb, double y0, double y1, double out, boolean startCap, boolean endCap,
                              Material material) {
        double[][] in = {{a + side * (ra - .03), za}, {a + side * (rb - .03), zb}}, ex = {{a + side * (ra + out), za}, {a + side * (rb + out), zb}};
        for (double y : new double[]{y0, y1})
            m.flat(List.of(new double[]{in[0][0], y, za}, new double[]{ex[0][0], y, za}, new double[]{ex[1][0], y, zb}, new double[]{in[1][0], y, zb}),
                    new double[]{0, y == y1 ? 1 : -1, 0}, material);
        m.flat(List.of(new double[]{ex[0][0], y0, za}, new double[]{ex[1][0], y0, zb}, new double[]{ex[1][0], y1, zb}, new double[]{ex[0][0], y1, za}), new double[]{side, 0, 0},
                material);
        if (startCap)
            m.flat(List.of(new double[]{in[0][0], y0, za}, new double[]{ex[0][0], y0, za}, new double[]{ex[0][0], y1, za}, new double[]{in[0][0], y1, za}), new double[]{0, 0, -1},
                    material);
        if (endCap)
            m.flat(List.of(new double[]{in[1][0], y0, zb}, new double[]{ex[1][0], y0, zb}, new double[]{ex[1][0], y1, zb}, new double[]{in[1][0], y1, zb}), new double[]{0, 0, 1},
                    material);
    }

    /** A coupling guard from {@code za} to {@code zb}: a half-round hood of radius {@code g} over the shaft at height {@code y}, standing on {@code base}. */
    static void guard(ProcessUnit u, double a, double base, double y, double g, double za, double zb) {
        var hood = new ArrayList<double[]>();
        hood.add(new double[]{a - g, base});
        hood.add(new double[]{a + g, base});
        for (int k = 0; k <= 6; k++) hood.add(new double[]{a + g * DeterministicMath.cos(Math.PI * k / 6), y + g * DeterministicMath.sin(Math.PI * k / 6)});
        u.extrude(hood, za + .02, zb - .02, Material.GUARD);
        u.solids.add(ProcessUnit.Solid.box(new double[]{a - g, base, za + .02}, new double[]{a + g, y + g, zb - .02}));
    }

    /**
     * The gear from {@code g0} to {@code g1}: a split housing 1.0 Dc wide with chamfered top edges, as tall as reaches 0.2 Dc
     * over both shafts and 0.9 Dc at least, its split flange on the pinion's line ({@code shaft}) with studs from 1 m, an
     * inspection cover and a breather on top, feet along both sides; the wheel's shaft leaves its back at {@code gShaft}.
     */
    private void gear(ProcessUnit u, double a, double base, double shaft, double gShaft, double g0, double g1) {
        double gw = .5 * d, gt = Math.max(base + .9 * d, Math.max(shaft, gShaft) + .2 * d), c = .15 * d, gm = (g0 + g1) / 2, sp = Math.max(.025, .03 * d);
        u.extrude(List.of(new double[]{a - gw, base}, new double[]{a + gw, base}, new double[]{a + gw, gt - c}, new double[]{a + gw - c, gt}, new double[]{a - gw + c, gt},
                new double[]{a - gw, gt - c}), g0, g1, Material.MACHINE);
        u.solids.add(ProcessUnit.Solid.box(new double[]{a - gw, base, g0}, new double[]{a + gw, gt, g1}));
        u.volumes.add(ProcessUnit.Solid.box(new double[]{a - gw + .05, base + .05, g0 + .05}, new double[]{a + gw - .05, gt - .05, g1 - .05}));
        u.box(new double[]{a - gw - .04, shaft - sp, g0 - .03}, new double[]{a + gw + .04, shaft + sp, g1 + .03}, Material.MACHINE, false);
        if (d >= 1) {
            int studs = Math.max(2, (int) Math.round((g1 - g0) / .25));
            for (int side = -1; side <= 1; side += 2)
                for (int k = 0; k < studs; k++) {
                    double x = a + side * (gw + .02), z = g0 + (g1 - g0) * (k + .5) / studs;
                    u.mesh.box(new double[]{x - .02, shaft + sp, z - .02}, new double[]{x + .02, shaft + sp + .05, z + .02}, ALL_FACES & ~4, Material.BOLT);
                }
        }
        u.box(new double[]{a - .3 * gw, gt, gm - .2 * (g1 - g0)}, new double[]{a + .3 * gw, gt + .02 + .01 * d, gm + .2 * (g1 - g0)}, Material.MACHINE, false);
        u.mesh.cylinder(Frame.of(new double[]{a + .55 * gw, 0, gm}, 1, 1), .025 + .01 * d, gt, gt + .06 + .03 * d, 8, Material.PIPE);
        for (int side = -1; side <= 1; side += 2) {
            double x0 = a + side * gw, x1 = a + side * (gw + .04 + .04 * d);
            u.box(new double[]{Math.min(x0, x1), base, g0 + .03}, new double[]{Math.max(x0, x1), base + .06 + .06 * d, g1 - .03}, Material.MACHINE, false);
        }
    }

    /**
     * The 4-pole generator from {@code m0} to {@code m1}, its shaft at {@code gShaft}: a fabricated frame 2.2 Hg wide and 2.0
     * Hg tall with chamfered edges and banded ends, round end shields, feet along both sides; the TEWAC cooler on top with
     * its tube plates and the cooling water's two lines down its +x side; the terminal box on its +x side by the drive end;
     * the outboard pedestal bearing (its sight glass) to {@code b1}; the brushless exciter from {@code x0} to {@code x1}
     * on its foot, its fan turning behind the grille at its end (a moving part at {@link ProcessUnit#CRANK_SPEED}).
     */
    private void generator(ProcessUnit u, double a, double base, double gShaft, double m0, double m1, double b1, double x0, double x1) {
        var m = u.mesh;
        double wg = 1.1 * hg, yb0 = base + .1 * hg, yt0 = base + 2 * hg, ct = .3 * hg, cb = .15 * hg, length = m1 - m0, mid = (yb0 + yt0) / 2;
        var body = List.of(new double[]{a - wg + cb, yb0}, new double[]{a + wg - cb, yb0}, new double[]{a + wg, yb0 + cb}, new double[]{a + wg, yt0 - ct},
                new double[]{a + wg - ct, yt0}, new double[]{a - wg + ct, yt0}, new double[]{a - wg, yt0 - ct}, new double[]{a - wg, yb0 + cb});
        double s0 = m0 + .06 * hg, s1 = m1 - .06 * hg, band = .12 * hg;
        u.extrude(body, s0, s1, Material.MOTOR);
        var wide = new ArrayList<double[]>();
        for (var p : body) wide.add(new double[]{a + (p[0] - a) * (wg + .03) / wg, mid + (p[1] - mid) * ((yt0 - yb0) / 2 + .03) / ((yt0 - yb0) / 2)});
        u.extrude(wide, s0, s0 + band, Material.MOTOR);
        u.extrude(wide, s1 - band, s1, Material.MOTOR);
        u.solids.add(ProcessUnit.Solid.box(new double[]{a - wg, yb0, s0}, new double[]{a + wg, yt0, s1}));
        u.volumes.add(ProcessUnit.Solid.box(new double[]{a - wg + ct, yb0 + .05, s0 + .05}, new double[]{a + wg - ct, yt0 - .05, s1 - .05}));
        var fg = Frame.of(new double[]{a, gShaft, 0}, 2, 1);
        int ng = ProcessUnit.sides(1.5 * hg);
        m.cylinder(fg, .75 * hg, m0, s0 + .01, ng, Material.MOTOR);
        m.cylinder(fg, .28 * hg, m0 - .05, m0, 12, Material.MOTOR);
        m.cylinder(fg, .75 * hg, s1 - .01, m1, ng, Material.MOTOR);
        for (int side = -1; side <= 1; side += 2) {
            double xi = a + side * (wg - .12 * hg), xo = a + side * (wg + .08 * hg);
            u.box(new double[]{Math.min(xi, xo), base, m0 + .1 * length}, new double[]{Math.max(xi, xo), base + .3 * hg, m1 - .1 * length}, Material.MOTOR, false);
        }
        // The cooler on top, its tube plates; the cooling water in and out down the +x side.
        double c0 = m0 + .15 * length, c1 = m1 - .1 * length, cw = .8 * hg, ch = .6 * hg;
        u.box(new double[]{a - cw, yt0 - .02, c0}, new double[]{a + cw, yt0 + ch, c1}, Material.MOTOR, true);
        u.volumes.add(ProcessUnit.Solid.box(new double[]{a - cw + .05, yt0, c0 + .05}, new double[]{a + cw - .05, yt0 + ch - .05, c1 - .05}));
        for (double zc : new double[]{c0 - .04, c1})
            u.box(new double[]{a - cw - .03, yt0 - .02, zc}, new double[]{a + cw + .03, yt0 + ch + .03, zc + .04}, Material.MOTOR, false);
        double rw = .03 + .02 * hg, xw = a + wg + .1 * hg + .12 + rw;
        for (int k = 0; k < 2; k++) {
            double zw = c1 - .15 - (.15 + .2 * hg) * k, yw = yt0 + .35 * ch;
            u.line(rw, Material.PIPE, new double[]{a + cw, yw, zw}, new double[]{xw, yw, zw}, new double[]{xw, base + .02, zw});
            u.solids.add(ProcessUnit.Solid.rod(new double[]{xw, 0, zw}, 1, base, yw, rw, 0));
        }
        u.box(new double[]{a + wg - .02, base + .4 * hg, m0 + .18 * length}, new double[]{a + wg + .4 * hg, base + 1.2 * hg, m0 + .42 * length}, Material.MOTOR, true);
        // The outboard pedestal bearing with its housing and sight glass.
        double b0 = m1 + .02, pw = .3 * hg + .05, bm = (b0 + b1) / 2;
        u.box(new double[]{a - pw, base, b0}, new double[]{a + pw, gShaft + .2 * hg, b1}, Material.MOTOR, true);
        m.cylinder(fg, .12 + .1 * hg, b0 - .01, b1 + .01, 12, Material.MOTOR);
        u.box(new double[]{a + pw, gShaft - .1 * hg - .04, bm - .05}, new double[]{a + pw + .015, gShaft - .1 * hg + .04, bm + .05}, Material.OPENING, false);
        // The exciter on its foot, its grilled fan.
        double re = .42 * hg, xl = x1 - x0;
        int ne = ProcessUnit.sides(2 * re);
        m.prism(fg, re, x0, x1, ne, Material.MOTOR);
        m.annulus(fg, 0, re, x0, ne, -1, Material.MOTOR);
        m.annulus(fg, .82 * re, re, x1, ne, 1, Material.MOTOR);
        u.grilledFan(fg, .82 * re, x1, Math.min(.15, .3 * xl), ne, ProcessUnit.CRANK_SPEED);
        u.box(new double[]{a - .5 * re, base, x0 + .1 * xl}, new double[]{a + .5 * re, gShaft - .9 * re, x1 - .1 * xl}, Material.MOTOR, false);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{a, gShaft, 0}, 2, x0, x1, re, 0));
        u.round(a, gShaft, x0, x1, re);
    }

    /**
     * The steam chest across the HP end's top at (yc, zc), its covers' flanges at both ends (steam in at +x). Its
     * governor valves' bonnets and spindles rise to a bar-lift bridge 0.2 Dc over it, which a hydraulic servo on a bracket
     * beyond its -x end lifts by a lever; a single-stage turbine's one governor valve hangs on the servo's lever, and one to
     * three hand valves (1, 2 from 1.2 m, 3 from 2.2 m) stand beside it with their handwheels.
     */
    private void steamChest(ProcessUnit u, double a, double yc, double zc) {
        var m = u.mesh;
        double rc = chestRadius(), xl = a - .4 * d, xr = a + .4 * d, cap = .03 + .01 * d, rf = rc + .02 + .01 * d;
        var fc = Frame.of(new double[]{0, yc, zc}, 0, 1);
        int nc = ProcessUnit.sides(2 * rc);
        m.prism(fc, rc, xl, xr, nc, Material.CLADDING);
        m.cylinder(fc, rf, xl - cap, xl, nc, Material.FLANGE);
        m.cylinder(fc, rf, xr, xr + cap, nc, Material.FLANGE);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{0, yc, zc}, 0, xl - cap, xr + cap, rf, 0));
        double ct = yc + rc, bar = ct + .2 * d, w = Math.max(.04, .05 * d), xs = xl - cap - .03 - .065 * d, rs = Math.max(.04, .065 * d), st = bar + .08 * d;
        // The servo on its bracket beyond the -x end, its piston rod.
        m.cylinder(Frame.of(new double[]{xs, 0, zc}, 1, 1), rs, yc - .06 * d, st, 12, Material.ACTUATOR);
        m.bar(new double[]{xl - cap, yc, zc}, new double[]{xs, yc, zc}, Math.max(.04, .04 * d), new double[]{0, 1, 0}, Material.MACHINE);
        m.cylinder(Frame.of(new double[]{xs, 0, zc}, 1, 1), .015 + .006 * d, st, st + .06 * d + .02, 6, Material.PIPE);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{xs, 0, zc}, 1, yc - .06 * d, st, rs, 0));
        double lever = st + .06 * d + .02;
        if (stages >= 2) {
            for (int k = 0; k < valves; k++) spindle(m, a - .28 * d + .56 * d * (k + .5) / valves, ct, bar, zc);
            for (int s = -1; s <= 1; s += 2)
                m.bar(new double[]{a + s * .34 * d, ct - .02, zc}, new double[]{a + s * .34 * d, bar + w / 2, zc}, w, new double[]{0, 0, 1}, Material.MACHINE);
            m.bar(new double[]{a - .37 * d, bar, zc}, new double[]{a + .37 * d, bar, zc}, 1.2 * w, new double[]{0, 1, 0}, Material.MACHINE);
            m.bar(new double[]{xs, lever, zc}, new double[]{a - .37 * d, bar + .6 * w, zc}, .8 * w, new double[]{0, 0, 1}, Material.MACHINE);
            u.solids.add(ProcessUnit.Solid.box(new double[]{a - .37 * d, ct, zc - .6 * w}, new double[]{a + .37 * d, bar + .6 * w, zc + .6 * w}));
            return;
        }
        double xg = a - .15 * d, gTop = ct + .12 * d;
        spindle(m, xg, ct, gTop, zc);
        m.bar(new double[]{xs, lever, zc}, new double[]{xg, gTop, zc}, .8 * w, new double[]{0, 0, 1}, Material.MACHINE);
        int hand = d < 1.2 ? 1 : d < 2.2 ? 2 : 3;
        double hw = Math.max(.15, Math.min(.25, .08 * d + .1)) / 2;
        for (int k = 0; k < hand; k++) {
            double x = a + .05 * d + k * (2 * hw + .04), yh = ct + .1 * d + .06;
            spindle(m, x, ct, yh, zc);
            var fw = Frame.of(new double[]{x, 0, zc}, 1, 1);
            m.ring(fw, hw - .025, hw, yh - .015, yh + .015, 12, 3, Material.ACTUATOR);
            m.bar(new double[]{x - hw + .02, yh, zc}, new double[]{x + hw - .02, yh, zc}, .02, new double[]{0, 1, 0}, Material.ACTUATOR);
            m.bar(new double[]{x, yh, zc - hw + .02}, new double[]{x, yh, zc + hw - .02}, .02, new double[]{0, 1, 0}, Material.ACTUATOR);
        }
    }

    /** A governor or hand valve on the chest's top {@code from} at (x, zc): its bonnet and its spindle up to {@code to}. */
    private void spindle(Mesh m, double x, double from, double to, double zc) {
        var up = Frame.of(new double[]{x, 0, zc}, 1, 1);
        double bonnet = from + .06 * d + .02;
        m.cylinder(up, .04 * d + .015, from - .02, bonnet, 10, Material.MACHINE);
        m.cylinder(up, Math.max(.012, .01 * d + .006), bonnet, to, 6, Material.PIPE);
    }

    /**
     * The trip-and-throttle valve, its body centred at (xv, yv, zv): a globe body, its outlet's neck and flange towards
     * the steam chest and its inlet's towards +x (back-pressure) or underneath (condensing, an angle body); the bonnet, the
     * yoke's two arms and head, the stem and the handwheel on top; the hydraulic trip cylinder on a bracket beside the
     * yoke. Its outlet line runs to the chest's +x cover at (yc, zc), jogging where the valve stands off the chest's row.
     */
    private void tripThrottle(ProcessUnit u, double a, double xv, double yv, double zv, double yc, double zc) {
        var m = u.mesh;
        double rb = valveBody(), od = steamLine(), rf = PipeSizes.flange(od) / 2 + .02, neck = .55 * rb, hw = handwheel();
        int nv = ProcessUnit.sides(2 * rb);
        m.sphere(new double[]{xv, yv, zv}, rb, nv, Material.MACHINE);
        var fx = Frame.of(new double[]{0, yv, zv}, 0, 1);
        m.prism(fx, neck, xv - rb - .05, xv - .5 * rb, 12, Material.MACHINE);
        m.cylinder(fx, rf, xv - rb - .08, xv - rb - .05, 12, Material.FLANGE);
        if (!condensing) {
            m.prism(fx, neck, xv + .5 * rb, xv + rb + .05, 12, Material.MACHINE);
            m.cylinder(fx, rf, xv + rb + .05, xv + rb + .08, 12, Material.FLANGE);
        } else {
            var fy = Frame.of(new double[]{xv, 0, zv}, 1, 1);
            m.prism(fy, neck, yv - rb - .05, yv - .5 * rb, 12, Material.MACHINE);
            m.cylinder(fy, rf, yv - rb - .08, yv - rb - .05, 12, Material.FLANGE);
        }
        double yB = yv + rb + .08 * d + .03, yY = yB + .3 * d + .1, arm = .03 * d + .02, head = .04 + .02 * d, yH = yY + head + .08;
        var up = Frame.of(new double[]{xv, 0, zv}, 1, 1);
        m.cylinder(up, .6 * rb, yv + .5 * rb, yB, nv, Material.MACHINE);
        for (int s = -1; s <= 1; s += 2)
            m.bar(new double[]{xv, yB - .01, zv + s * .5 * rb}, new double[]{xv, yY, zv + s * .5 * rb}, arm, new double[]{1, 0, 0}, Material.MACHINE);
        u.box(new double[]{xv - .25 * rb - .02, yY, zv - .5 * rb - arm}, new double[]{xv + .25 * rb + .02, yY + head, zv + .5 * rb + arm}, Material.MACHINE, false);
        m.cylinder(up, .015 + .006 * d, yB, yH + .05, 6, Material.PIPE);
        m.ring(up, hw / 2 - .03, hw / 2, yH - .02, yH + .02, 16, 3, Material.ACTUATOR);
        m.bar(new double[]{xv - hw / 2 + .02, yH, zv}, new double[]{xv + hw / 2 - .02, yH, zv}, .025, new double[]{0, 1, 0}, Material.ACTUATOR);
        m.bar(new double[]{xv, yH, zv - hw / 2 + .02}, new double[]{xv, yH, zv + hw / 2 - .02}, .025, new double[]{0, 1, 0}, Material.ACTUATOR);
        double rt = .045 * d + .025, zt = zv + .5 * rb + arm + rt + .03;
        m.cylinder(Frame.of(new double[]{xv, 0, zt}, 1, 1), rt, yB - .02, yB + .2 * d + .12, 12, Material.ACTUATOR);
        m.bar(new double[]{xv, yB + .1 * d + .05, zv + .5 * rb}, new double[]{xv, yB + .1 * d + .05, zt - rt}, Math.max(.04, .03 * d), new double[]{0, 1, 0}, Material.MACHINE);
        u.solids.add(ProcessUnit.Solid.box(new double[]{xv - rb, yv - rb, zv - rb}, new double[]{xv + rb, yY + head, zt + rt}));
        u.solids.add(ProcessUnit.Solid.box(new double[]{xv - hw / 2, yH - .02, zv - hw / 2}, new double[]{xv + hw / 2, yH + .02, zv + hw / 2}));
        // The outlet line to the chest's +x cover ({@link ProcessUnit#pipe}, which leaves out a jog of no length): its legs
        // across, along z, up or down and across again.
        double xo = xv - rb - .08, xc = a + .4 * d + .03 + .01 * d, xm = (xo + xc) / 2;
        u.pipe(od / 2, Material.PIPE, true, new double[]{xo, yv, zv}, new double[]{xm, yv, zv}, new double[]{xm, yv, zc}, new double[]{xm, yc, zc}, new double[]{xc, yc, zc});
    }
}
