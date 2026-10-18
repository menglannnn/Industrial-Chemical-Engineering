package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.equipment.Mesh.Frame;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.Mesh.ALL_FACES;

/**
 * A condensing steam turbine-generator set (owner decisions D2 and D4 2026-10-18, {@code NEW_UNITS_PLAN.md};
 * {@code NEW_UNITS_SURVEY_ROTATING.md} section 2): its exhaust, 0.08 to 0.3 bar abs and 0.5 to 3.2 m across, is too large
 * for the 1 m pipe cap, so the set carries its surface condenser and gives cooling water, condensate and air extraction
 * instead. Sizes as the back-pressure set's ({@link SteamTurbineDesign}): Dc 0.6 to 3 m, 1 to 12 stages; the support is
 * the operating deck's height. Physics: the exhaust falls straight into the condenser, so the table's height is the
 * condenser's.
 *
 * <p>The train ({@link SteamTurbineTrain}) stands on a reinforced-concrete table-top, its shaft 0.45 Dc over the deck: a
 * slab 0.8 to 1.5 m thick (0.3 + 0.4 Dc) on columns 0.6 to 1.1 m square (0.35 + 0.25 Dc) in frames at its ends and at
 * most 6 m apart, outside the condenser, with openings for the exhaust neck and the steam riser. Grating walkways 1.2 m
 * wide run along both sides at deck level with their handrails; a stair tower of flights alternating direction (OSHA:
 * at most 3.7 m a flight, a 1.2 m landing between) climbs to the -x walkway. Under the deck the exhaust neck, 1.1 by
 * 0.8 Dc (shorter on a short casing), falls through its expansion joint into the surface condenser: a shell 1.6 Dc
 * across and 3.5 Dc long, its axis under the shaft, two-pass with its water boxes (as deep as the cooling-water nozzle
 * and 0.1 m, 0.35 Dc at least) and their bolted covers at both ends, its hotwell 0.3 Dc deep under its middle, on two
 * saddles; behind it two vertical can-type condensate pumps (their cans in the pit, their discharge heads and motors in
 * sight, sized for 9.5 Dc² kW) and their discharge header. The atmospheric relief diaphragm sits on the exhaust casing.
 * The deck stands at least 2.4 Dc + 0.5 m and 3.5 m high (survey), and so high that the cooling-water inlet under the
 * front water box keeps its flange and a free pipe block over grade ({@link #heights}; a metre more than the survey's
 * least at Dc 1.1 to 1.4, 1.6 to 1.8, 2.1 and 2.2, 2.6 and 2.7, and 3 m).
 *
 * <p>Nozzles: STEAM up through the deck into the trip-and-throttle valve, its line leaving under the +x walkway
 * (0.15 Dc); COOLANT_INLET down from the front water box's bottom and COOLANT_OUTLET out of its cover's upper half (0.4 Dc,
 * at most 1 m); CONDENSATE out of the pumps' header to the back (0.08 Dc); VENT, the air extraction, out of the shell's
 * upper -x side (0.06 Dc); 0.05 m at least each.
 */
final class CondensingTurbineDesign extends UnitDesign {
    /** Walkway and landing widths, a stair flight's width, a flight's highest rise (OSHA 1910.25), m. */
    static final double WALK = 1.2, LANDING = 1.2, STAIR = 1, FLIGHT = 3.7;

    /** The table's slab, deck and beams in one, m: 0.3 + 0.4 Dc, 0.8 to 1.5 m. */
    static double slab(double d) {
        return Math.min(1.5, Math.max(.8, .3 + .4 * d));
    }

    @Override
    public double minSize() {
        return .6;
    }

    @Override
    public double maxSize() {
        return 3;
    }

    @Override
    public double sizeStep() {
        return .1;
    }

    @Override
    public int minLength(double size) {
        return 1;
    }

    @Override
    public int maxLength(double size) {
        return 12;
    }

    /**
     * The least deck: 2.4 Dc + 0.5 m and 3.5 m at least (survey), and the slab, a 0.15 m expansion joint, the condenser
     * (1.6 Dc) and 1.15 m under it for the cooling-water inlet's flange and its pipe block.
     */
    @Override
    public int minSupport(double size, int length) {
        int h = (int) Math.ceil(Math.max(3.5, 2.4 * size + .5) - 1e-9);
        while (heights(size, h) == null) h++;
        return h;
    }

    /**
     * Where the condenser hangs under a deck {@code h} high: {j, rowOut, inletFace}, its neck's joint, the coolant outlet's
     * row and the coolant inlet's flange face; {@code null} where it does not fit. The joint is 0.15 + 0.15 Dc at least
     * and leaves 1.15 m under the shell for the inlet's flange and pipe block; the outlet leaves the front cover's upper
     * half (from 0.05 r under the axis to its flange 0.2 r inside the rim, under the slab) on a cell row, the highest such
     * row the joint reaches, nearest 0.35 r over the axis; where the two pipes' blocks share a column of cells along z
     * (a cover nearer its block's face than 0.35 m, Dc 1.1 m and less), the inlet's block lies two rows under the outlet's.
     */
    static double[] heights(double d, double h) {
        double slab = slab(d), r = .8 * d, cw = Math.min(1, .4 * d), box = Math.max(.35 * d, cw + .1), cover = .04 * d + .02, under = h - slab;
        double j0 = .15 + .15 * d, jMax = under - 2 * r - 1.15;
        for (double c = Math.floor(Math.min(under - .2 * r - cw / 2 - .02 - j0, under - .5) - .5) + .5; c > 0 && under - 1.05 * r - c <= jMax + 1e-9; c--) {
            double lo = Math.max(j0, under - 1.05 * r - c), hi = Math.min(jMax, under - .2 * r - cw / 2 - .02 - c);
            if (lo > hi + 1e-9) continue;
            double j = Math.max(lo, Math.min(hi, under - .65 * r - c)), face = Math.floor(under - j - 2 * r - EquipmentShape.STAND_OUT + 1e-9);
            if (box / 2 + cover <= .35) face = Math.min(face, c - 1.5);
            return face >= 1 ? new double[]{j, c, face} : null;
        }
        return null;
    }

    @Override
    public Orientation orientation() {
        return Orientation.HORIZONTAL;
    }

    /** The exciter's fan turns behind its grille. */
    @Override
    public boolean moves() {
        return true;
    }

    @Override
    public String sizeRow() {
        return "steam_turbine.casing";
    }

    @Override
    public String lengthRow() {
        return "steam_turbine.stages";
    }

    @Override
    public boolean lengthInMetres() {
        return false;
    }

    @Override
    public String supportRow() {
        return "condensing_turbine.table";
    }

    @Override
    public Line held(EquipmentShape shape) {
        double d = shape.diameter();
        return new Line("steam_turbine.power", number(SteamTurbineTrain.power(d)), whole(SteamTurbineTrain.speed(d)));
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        double d = shape.diameter();
        return List.of(new Line("steam_turbine.casing", (int) shape.tangentLength(), size(d)), new Line("condensing_turbine.condenser", size(1.6 * d), number(3.5 * d)),
                new Line("steam_turbine.power", number(SteamTurbineTrain.power(d)), whole(SteamTurbineTrain.speed(d))));
    }

    @Override
    void build(ProcessUnit u) {
        // Laid out once to find its reach, then again shifted by whole cells so its box starts at the frame's origin.
        var first = new Layout(u.shape, .5, 0);
        new Layout(u.shape, .5 - Math.floor(first.xMin), -Math.floor(first.zMin)).draw(u);
    }

    /** A condensing set's layout in the frame, its shaft at x {@code a}, its front standard's face at z {@code z0}. */
    private static final class Layout {
        final SteamTurbineTrain t;
        final double d, h, a, z0, slab, column, r, ring, tube, cw, box, cover, steam, vent, condensate;
        final double j, yt, yk, yb, rowOut, inletFace, xv, yv, zv, ys, half, zT0, zT1, zE, zwb, zs0, zs1, rear, motor, head, spread, zp, zVent, yVent;
        final double xo, fLo, fHi, rise, xMin, zMin;
        final int flights;
        final List<Double> frames = new ArrayList<>();

        Layout(EquipmentShape shape, double a, double z0) {
            d = shape.diameter();
            h = shape.support();
            this.a = a;
            this.z0 = z0;
            t = new SteamTurbineTrain(d, (int) shape.tangentLength(), true);
            slab = CondensingTurbineDesign.slab(d);
            column = Math.min(1.1, Math.max(.6, .25 * d + .35));
            r = .8 * d;
            ring = .04 * d + .01;
            tube = .05 * d + .02;
            cw = Math.min(1, .4 * d);
            box = Math.max(.35 * d, cw + .1);
            cover = .04 * d + .02;
            steam = t.steamLine();
            vent = Math.max(.05, .06 * d);
            condensate = Math.max(.05, .08 * d);
            // Heights ({@link #heights}): the condenser under the deck by its neck's joint.
            double[] at = heights(d, h);
            j = at[0];
            rowOut = at[1];
            inletFace = at[2];
            yt = h - slab - j;
            yk = yt - r;
            yb = yk - r;
            // Across x: the trip-and-throttle valve beside the HP end, its riser clear of the condenser's flanges; the table
            // as wide as its columns outside the condenser and the train with its valve.
            double rb = t.valveBody();
            xv = a + Math.max(t.valveOffset(), r + ring + steam / 2 + .08);
            half = Math.max(Math.max(r + ring + .3 + column, t.rightReach(xv - a) + .25), t.leftReach() + .25);
            yv = Math.max(h + .13 + rb, h + t.chestAxis() - .35 * d);
            ys = Math.floor(h - Math.max(slab + .1 + steam / 2, 1.23) - .5) + .5;
            // Along z: the table from just before the governor actuator; the condenser's shell under the exhaust neck, its
            // front water box as near the train's front as that allows, its middle on a cell centre (the inlet's line).
            zT0 = z0 - t.frontReach() - .15;
            zE = z0 + t.exhaust;
            double lo = zE + t.neck / 2 + .1 - 3.5 * d, hi = zE - t.neck / 2 - .1, want = Math.min(hi, Math.max(lo, z0 + .1));
            double wb = Math.floor(want - tube - box / 2 - .5) + .5;
            if (wb + box / 2 + tube < lo) wb += 1;
            zwb = wb;
            zs0 = zwb + box / 2 + tube;
            zs1 = zs0 + 3.5 * d;
            rear = zs1 + tube + box + cover;
            motor = ProcessUnit.motorDiameter(Math.max(2, 9.5 * d * d)) / 2;
            head = 1.1 * motor;
            spread = head + .6;
            zp = rear + .35 + head;
            zT1 = Math.max(z0 + t.length() + .15, rear + .1);
            // Column frames at the table's ends and at most 6 m apart; the steam line and the vent's pipe block between them.
            int bays = Math.max(1, (int) Math.ceil((zT1 - zT0 - column) / 6));
            for (int k = 0; k <= bays; k++) frames.add(zT0 + column / 2 + (zT1 - zT0 - column) * k / bays);
            zv = free(Math.floor(z0 + t.chest) + .5, steam / 2 + .08 + column / 2, zT0 + .4, zT1 - .4);
            zVent = free(Math.floor(zs0 + .7 * 3.5 * d) + .5, .55 + column / 2, zs0 + .55, zs1 - .55);
            yVent = row(yk + .55 * r, yk, Math.min(yk + r - vent / 2 - .04, h - slab - .5));
            // The stair tower outside the -x walkway: its flights alternating, its top landing at the table's back end
            // (an odd count) or front end (even).
            flights = Math.max(2, (int) Math.ceil(h / FLIGHT - 1e-9));
            rise = h / flights;
            double flight = Math.ceil(rise / ProcessUnit.STAIR_RISE - 1e-9) * ProcessUnit.STAIR_GOING;
            xo = a - half - WALK;
            if (flights % 2 == 1) {
                fHi = zT1 - .05 - LANDING;
                fLo = fHi - flight;
            } else {
                fLo = zT0 + .05 + LANDING;
                fHi = fLo + flight;
            }
            xMin = xo - 2 * STAIR - .05;
            zMin = Math.min(Math.min(zT0, fLo - LANDING), Math.floor(zwb - box / 2 - cover - EquipmentShape.STAND_OUT)) - .02;
        }

        /** The cell centre nearest {@code want} in [lo, hi]; with none there, the highest under {@code hi}. */
        private static double row(double want, double lo, double hi) {
            double best = Math.floor(want) + .5;
            for (double c : new double[]{best, best - 1, best + 1}) if (c >= lo && c <= hi && Math.abs(c - want) <= Math.abs(best - want)) return c;
            for (double c = best + 1; c >= best - 2; c--) if (c >= lo && c <= hi) return c;
            return Math.floor(hi - .5) + .5;
        }

        /** The cell centre nearest {@code want} in [lo, hi] at least {@code clear} from every column frame's middle; {@code want} where none is. */
        private double free(double want, double clear, double lo, double hi) {
            for (int k = 0; k <= 12; k++) {
                double c = want + (k % 2 == 0 ? k / 2 : -(k + 1) / 2);
                if (c < lo || c > hi) continue;
                boolean ok = true;
                for (double f : frames) if (Math.abs(c - f) < clear) ok = false;
                if (ok) return c;
            }
            return want;
        }

        void draw(ProcessUnit u) {
            var m = u.mesh;
            double rp = steam / 2, rb = t.valveBody(), under = h - slab, shaft = h + t.rise;
            t.draw(u, a, h, z0, xv, yv, zv);

            // The table: its slab with the openings for the exhaust neck and the steam riser; its columns on their pads.
            double nx = .55 * d, n0 = zE - t.neck / 2, n1 = zE + t.neck / 2;
            cast(u, a - half, zT0, a + half, zT1, under, h, new double[][]{{a - nx - .05, n0 - .05, a + nx + .05, n1 + .05}, {xv - rp - .05, zv - rp - .05, xv + rp + .05, zv + rp + .05}});
            for (double zf : frames)
                for (int side = -1; side <= 1; side += 2) {
                    double x0 = a + side * (half - column), x1 = a + side * half;
                    u.box(new double[]{Math.min(x0, x1), .15, zf - column / 2}, new double[]{Math.max(x0, x1), under, zf + column / 2}, Material.CONCRETE, true);
                    u.box(new double[]{Math.min(x0, x1) - .1, 0, zf - column / 2 - .1}, new double[]{Math.max(x0, x1) + .1, .15, zf + column / 2 + .1}, Material.CONCRETE, true);
                }

            // The walkways along both sides at deck level, the -x one open where the stair tower's top landing joins it.
            double level = h - Access.DECK, length = zT1 - zT0;
            boolean odd = flights % 2 == 1;
            double g0 = odd ? fHi + Access.GAP - zT0 : fLo - LANDING - zT0, g1 = odd ? fHi + LANDING - zT0 : fLo - Access.GAP - zT0;
            u.platform(new Access.Line(new double[]{a + half, 0, zT0}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, length), level, WALK, true, true);
            u.platform(new Access.Line(new double[]{a - half, 0, zT0}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, length), level, WALK, true, true, g0, g1);
            // The table's front and back edges between the walkways, a drop of the table's height: handrails, as on the
            // centrifugal compressor's table.
            u.railing(new Access.Line(new double[]{a - half, 0, zT0 + 1}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, 2 * half), level, 1);
            u.railing(new Access.Line(new double[]{a - half, 0, zT1 - 1}, new double[]{1, 0, 0}, new double[]{0, 0, 1}, 2 * half), level, 1);
            // The stair tower: odd flights rise towards +z in the outer lane, even ones back in the inner lane, a landing
            // across both lanes at each turn; the top landing opens onto the walkway, a handrail on its edge over the lane
            // the last flight does not use.
            double outer = xo - 1.5 * STAIR, inner = xo - .5 * STAIR;
            for (int k = 1; k <= flights; k++) {
                boolean up = k % 2 == 1, last = k == flights;
                u.stair(up ? outer : inner, up ? fLo : fHi, 2, up ? 1 : -1, STAIR, (k - 1) * rise, last ? h : k * rise);
                double end = up ? fHi : fLo, out = up ? 1 : -1, top = last ? h : k * rise;
                u.platform(new Access.Line(new double[]{xo - 2 * STAIR, 0, end}, new double[]{1, 0, 0}, new double[]{0, 0, out}, 2 * STAIR), top - Access.DECK, LANDING, true, !last);
                if (last) {
                    double lane = up ? xo - STAIR : xo - 2 * STAIR;
                    u.railing(new Access.Line(new double[]{lane, 0, end + out * LANDING}, new double[]{1, 0, 0}, new double[]{0, 0, -out}, STAIR), top - Access.DECK,
                            LANDING - Access.GAP);
                }
            }

            // The exhaust neck from the casing's split line down through the deck into the condenser's top, its expansion
            // joint's frames and convolutions under the deck; the relief diaphragm on the exhaust casing.
            double bottom = yk + .5 * d, jb = .04 + .01 * d;
            m.box(new double[]{a - nx, bottom, n0}, new double[]{a + nx, shaft, n1}, ALL_FACES & ~8, Material.MACHINE);
            u.solids.add(ProcessUnit.Solid.box(new double[]{a - nx, bottom, n0}, new double[]{a + nx, shaft, n1}));
            u.volumes.add(ProcessUnit.Solid.box(new double[]{a - nx + .05, bottom, n0 + .05}, new double[]{a + nx - .05, shaft - .05, n1 - .05}));
            for (double y : new double[]{under - .07, yt + .03})
                u.box(new double[]{a - nx - jb, y - .03, n0 - jb}, new double[]{a + nx + jb, y + .03, n1 + jb}, Material.FLANGE, false);
            int folds = Math.max(1, (int) Math.round((under - yt - .2) / .12));
            for (int k = 0; k < folds; k++) {
                double y = yt + .1 + (under - yt - .2) * (k + .5) / folds;
                u.box(new double[]{a - nx - .6 * jb, y - .02, n0 - .6 * jb}, new double[]{a + nx + .6 * jb, y + .02, n1 + .6 * jb}, Material.PIPE, false);
            }
            double rr = .15 * d, top = shaft + .5 * d + .06 * d;
            var relief = Frame.of(new double[]{a, 0, zE}, 1, 1);
            m.prism(relief, rr, shaft + .4 * d, top, ProcessUnit.sides(2 * rr), Material.MACHINE);
            m.cylinder(relief, rr + .03, top, top + .02 + .01 * d, ProcessUnit.sides(2 * rr), Material.FLANGE);

            condenser(u);
            pumps(u);

            // The steam riser from the valve's inlet down through the deck, its elbow under the deck towards +x.
            double yi = yv - rb - .08;
            SteamTurbineTrain.pipe(u, rp, new double[]{xv, yi, zv}, new double[]{xv, ys, zv});
            m.sphere(new double[]{xv, ys, zv}, 1.05 * rp, 12, Material.PIPE);

            // The nozzles.
            double steamFace = Math.max(Math.ceil(xv + rp + EquipmentShape.STAND_OUT), Math.ceil(a + half + .1));
            u.nozzles.add(new Nozzle(Role.STEAM, new double[]{xv, ys, zv}, 0, 1, steamFace - xv, steam, steam, 0, rp + .03));
            int nk = ProcessUnit.sides(2 * r);
            double apothem = r * DeterministicMath.cos(Math.PI / nk), in = Math.sqrt(Math.max(0, apothem * apothem - cw * cw / 4)) - .03;
            u.nozzles.add(new Nozzle(Role.COOLANT_INLET, new double[]{a, yk - in, zwb}, 1, -1, yk - in - inletFace, cw, cw, 0, r - in));
            u.nozzles.add(ProcessUnit.front(Role.COOLANT_OUTLET, a, rowOut, zwb - box / 2 - cover + .03, cw, cw));
            double rc = condensate / 2, cz = zp + .3 * rc;
            u.nozzles.add(new Nozzle(Role.CONDENSATE, new double[]{a, .5, cz}, 2, 1, Math.ceil(zp + rc + EquipmentShape.STAND_OUT) - cz, condensate, condensate, 0, .7 * rc + .03));
            double e = Math.abs(yVent - yk), low = Math.max(0, e - .5), xs = Math.sqrt(Math.max(0, r * r - low * low)), start = Math.sqrt(Math.max(0, apothem * apothem - (e + vent / 2) * (e + vent / 2))) - .03;
            double ventFace = Math.min(Math.floor(a - xs - EquipmentShape.STAND_OUT), a - 1.5);
            u.nozzles.add(new Nozzle(Role.VENT, new double[]{a - start, yVent, zVent}, 0, -1, a - start - ventFace, vent, vent, 0, r - start));
        }

        /**
         * The surface condenser: its shell under the shaft, the tubesheets' flanges, the water boxes and their bolted covers
         * at both ends (nuts every 0.35 m or so from 1 m), the hotwell under its middle with the condensate's line down to
         * the pumps' pit, two saddles clear of the cooling-water inlet's pipe block.
         */
        private void condenser(ProcessUnit u) {
            var m = u.mesh;
            var fk = Frame.of(new double[]{a, yk, 0}, 2, 1);
            int nk = ProcessUnit.sides(2 * r);
            double big = r + ring;
            m.prism(fk, r, zs0, zs1, nk, Material.SHELL);
            for (int end = 0; end < 2; end++) {
                double s = end == 0 ? -1 : 1, t0 = end == 0 ? zs0 : zs1, w0 = t0 + s * tube, w1 = w0 + s * box, c1 = w1 + s * cover;
                m.prism(fk, big, Math.min(t0, w0), Math.max(t0, w0), nk, Material.FLANGE);
                m.annulus(fk, r, big, Math.min(t0, w0), nk, -1, Material.FLANGE);
                m.annulus(fk, r, big, Math.max(t0, w0), nk, 1, Material.FLANGE);
                m.prism(fk, r, Math.min(w0, w1), Math.max(w0, w1), nk, Material.MACHINE);
                m.prism(fk, big, Math.min(w1, c1), Math.max(w1, c1), nk, Material.FLANGE);
                m.annulus(fk, r, big, w1, nk, -s, Material.FLANGE);
                m.annulus(fk, 0, big, c1, nk, s, Material.FLANGE);
                if (d >= 1) {
                    double circle = r + ring / 2;
                    int nuts = Math.max(8, 4 * (int) Math.round(2 * Math.PI * circle / .35 / 4));
                    for (int k = 0; k < nuts; k++) {
                        double angle = 2 * Math.PI * (k + .5) / nuts, x = a + circle * DeterministicMath.cos(angle), y = yk + circle * DeterministicMath.sin(angle);
                        double z0n = Math.min(c1, c1 + s * .04), z1n = Math.max(c1, c1 + s * .04);
                        m.box(new double[]{x - .025, y - .025, z0n}, new double[]{x + .025, y + .025, z1n}, ALL_FACES & ~(s < 0 ? 32 : 16), Material.BOLT);
                    }
                }
            }
            double front = zs0 - tube - box - cover;
            u.solids.add(ProcessUnit.Solid.rod(new double[]{a, yk, 0}, 2, front, zs0, big, 0));
            u.solids.add(ProcessUnit.Solid.rod(new double[]{a, yk, 0}, 2, zs0, zs1, r, 0));
            u.solids.add(ProcessUnit.Solid.rod(new double[]{a, yk, 0}, 2, zs1, rear, big, 0));
            u.volumes.add(ProcessUnit.Solid.rod(new double[]{a, yk, 0}, 2, front + cover + .03, rear - cover - .03, .92 * r, 0));
            u.round(a, yk, zs0, zs1, r);
            // The hotwell, the condensate's line into the pit with its sleeve at grade.
            double length = zs1 - zs0, h0 = zs0 + .38 * length, h1 = zs0 + .62 * length, hw = .35 * d, hb = yb - .3 * d, hm = (h0 + h1) / 2, rl = Math.max(.04, .05 * d);
            u.box(new double[]{a - hw, hb, h0}, new double[]{a + hw, yk - .5 * r, h1}, Material.MACHINE, true);
            SteamTurbineTrain.pipe(u, rl, new double[]{a, hb, hm}, new double[]{a, 0, hm});
            m.cylinder(Frame.of(new double[]{a, 0, hm}, 1, 1), rl + .06, 0, .05, 12, Material.CONCRETE);
            double w = ProcessUnit.saddleWidth(r);
            u.saddle(ProcessUnit.saddleAt(zs0 + .15 * length, w, zs0, zs1, zwb), a, yk, r, w);
            u.saddle(ProcessUnit.saddleAt(zs1 - .15 * length, w, zs0, zs1, zwb), a, yk, r, w);
        }

        /**
         * The two condensate pumps behind the rear water box at x a ± spread: each a sole plate at grade (its can in the
         * pit), a discharge head with its outlet 0.5 m up, the motor stand, the vertical motor and its fan cowl, a terminal
         * box outboard; their outlets joined by the header the condensate nozzle leaves.
         */
        private void pumps(ProcessUnit u) {
            var m = u.mesh;
            double crown = .95, base = crown + .5 * motor + .1, cap = base + 1.4 * motor + .15;
            int n = ProcessUnit.sides(2 * head);
            for (int side = -1; side <= 1; side += 2) {
                double x = a + side * spread;
                var up = Frame.of(new double[]{x, 0, zp}, 1, 1);
                u.box(new double[]{x - head - .08, 0, zp - head - .08}, new double[]{x + head + .08, .06, zp + head + .08}, Material.STEELWORK, false);
                m.cylinder(up, head, .06, crown, n, Material.MACHINE);
                m.cylinder(up, .7 * motor, crown, base, 12, Material.MACHINE);
                m.cylinder(up, motor, base, cap, ProcessUnit.sides(2 * motor), Material.MOTOR);
                m.cylinder(up, .9 * motor, cap, cap + .3 * motor + .03, ProcessUnit.sides(2 * motor), Material.MOTOR);
                double xt = x + side * motor;
                u.box(new double[]{Math.min(xt, xt + side * .4 * motor), base + .5 * motor, zp - .3 * motor}, new double[]{Math.max(xt, xt + side * .4 * motor), base + 1.1 * motor, zp + .3 * motor},
                        Material.MOTOR, false);
                u.solids.add(ProcessUnit.Solid.rod(new double[]{x, 0, zp}, 1, 0, cap + .3 * motor + .03, head, 0));
            }
            SteamTurbineTrain.pipe(u, condensate / 2, new double[]{a - spread + .9 * head, .5, zp}, new double[]{a + spread - .9 * head, .5, zp});
        }

        /**
         * The table's slab from (x0, z0) to (x1, z1), its underside at {@code y0} and its deck at {@code y1}, but over the
         * {@code holes} ({x0, z0, x1, z1} each): in bands along z between the holes' edges, each band's pieces between them.
         */
        private static void cast(ProcessUnit u, double x0, double z0, double x1, double z1, double y0, double y1, double[][] holes) {
            var cuts = new ArrayList<Double>(List.of(z0, z1));
            for (var hole : holes)
                for (double z : new double[]{hole[1], hole[3]}) if (z > z0 && z < z1) cuts.add(z);
            cuts.sort(Double::compare);
            for (int b = 0; b + 1 < cuts.size(); b++) {
                double za = cuts.get(b), zb = cuts.get(b + 1);
                if (zb - za < 1e-6) continue;
                var spans = new ArrayList<double[]>();
                spans.add(new double[]{x0, x1});
                for (var hole : holes) {
                    if (hole[1] >= zb - 1e-9 || hole[3] <= za + 1e-9) continue;
                    var cut = new ArrayList<double[]>();
                    for (var s : spans) {
                        if (hole[2] <= s[0] || hole[0] >= s[1]) cut.add(s);
                        else {
                            if (hole[0] > s[0]) cut.add(new double[]{s[0], hole[0]});
                            if (hole[2] < s[1]) cut.add(new double[]{hole[2], s[1]});
                        }
                    }
                    spans = cut;
                }
                for (var s : spans) {
                    u.box(new double[]{s[0], y0, za}, new double[]{s[1], y1, zb}, Material.CONCRETE, true);
                    if (s[1] - s[0] > .2 && zb - za > .2 && y1 - y0 > .2)
                        u.volumes.add(ProcessUnit.Solid.box(new double[]{s[0] + .05, y0 + .05, za + .05}, new double[]{s[1] - .05, y1 - .05, zb - .05}));
                }
            }
        }
    }
}
