package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Solid;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;
import static com.wormzjl.createcheme.science.equipment.TrickleBedReactorParts.*;

/**
 * A refinery hot-wall trickle-bed reactor (hydrotreater, hydrocracker; owner request 2026-10-18, NEW_UNITS_PLAN.md, form
 * logged from {@code NEW_UNITS_SURVEY_HYDROPROCESSING.md} section 1): oil runs down over fixed catalyst beds with hydrogen,
 * cold hydrogen quenching the flow between beds. Sizes: inside diameter D 1 to 5 m in 0.5 m steps (forged reactors reach
 * 5.5 m, L&T; the GE Massa hydrocracker is 5.5 m by 42 m), 1 to 4 beds; the skirt from the pedestal to the bottom
 * tangent, {@link #minSupport} at least.
 *
 * <p>Proportions (survey): wall t = 0.05 D (0.03 m at least), insulation 0.1 + 0.02 D under aluminium cladding; bed
 * Hb = min(1.2 D + 1, 7), quench zone Zq = 1 + 0.1 D, top zone (inlet diffuser and distributor tray) Zt = 0.8 + 0.1 D,
 * T/T = Zt + N Hb + (N - 1) Zq; hemispherical heads at every size (at 50 to 200 bar t ≈ PR/(S - 0.6P) makes them the
 * norm). Drawn, outside in: the clad shell with flashing bands every 3.5 m and its clad top head; the forged top nozzle
 * with its Class 2500 ring-joint flange pair and studs (the signature, {@link TrickleBedReactorParts#topNozzle}), the
 * elbow to the front and the hydrogen tee; the square top deck at the top tangent (1.2 m beyond the cladding, the room a
 * caged ladder's exit needs; survey about 1 m beyond the OD) on girders 0.3 + 0.04 D deep, with its handrail and a pipe
 * davit reaching over the nozzle; a platform ring 1.2 m under each quench injection with the quench riser's control valve
 * 0.8 m over its deck, rest platforms splitting climbs over 9 m, caged ladders alternating between the back and the
 * right; the quench trunk up the left side, its header round the front left under the lowest platform and a riser per
 * quench zone to its nozzle; a thermowell at the top, middle and bottom of each bed; two lifting trunnions (0.15 D across,
 * 0.12 D proud) under the top deck's girders; the fireproofed skirt (the shell's OD) on its base ring, anchor chairs and
 * octagonal pedestal, with two access openings, a vent row, the effluent's sleeve and a 45° catalyst dump nozzle,
 * blind-flanged.
 *
 * <p>Nozzles: FEED (oil and hydrogen mixed, 0.15 D, 0.15 to 1 m) leaves the elbow's run to the front above the top deck's
 * handrail; HYDROGEN (0.1 D) leaves the tee on that run to the left; both faces stand at the top deck's edge. EFFLUENT
 * (0.15 D) from the bottom head's centre turns inside the skirt and leaves through it to the front 1.5 m over grade;
 * QUENCH (0.05 D, 0.1 m at least) leaves the foot of the quench trunk to the left at the same height (real plants mix the
 * hydrogen upstream; the tee gives it a port of its own, survey Q2). A single bed has no quench zone: its riser trims the
 * inlet in the top zone.
 */
final class TrickleBedReactorDesign extends UnitDesign {
    /** The bottom ports' row: the cell centre 1.5 m over grade, clear of the pedestal (survey: 0.8 to 1.2 m). */
    static final double PORT_ROW = 1.5;

    static double wall(double d) {
        return Math.max(.03, .05 * d);
    }

    static double insulation(double d) {
        return .1 + .02 * d;
    }

    static double bed(double d) {
        return Math.min(1.2 * d + 1, 7);
    }

    static double quenchZone(double d) {
        return 1 + .1 * d;
    }

    static double topZone(double d) {
        return .8 + .1 * d;
    }

    static double tangentLength(double d, int beds) {
        return topZone(d) + beds * bed(d) + (beds - 1) * quenchZone(d);
    }

    /** Catalyst in the beds, m³. */
    static double catalyst(double d, int beds) {
        return beds * bed(d) * Math.PI * d * d / 4;
    }

    @Override
    public double minSize() {
        return 1;
    }

    @Override
    public double maxSize() {
        return 5;
    }

    @Override
    public double sizeStep() {
        return .5;
    }

    @Override
    public int minLength(double size) {
        return 1;
    }

    @Override
    public int maxLength(double size) {
        return 4;
    }

    /**
     * The skirt: D/2 + t + 1.5 m at least (survey), and room under the bottom head for the effluent's elbow (bend 1 D of
     * the line, 0.15 D) over the port row 1.5 m up: 1.55 + 0.7 D, which decides from 2.5 m.
     */
    @Override
    public int minSupport(double size, int length) {
        return (int) Math.ceil(Math.max(size / 2 + wall(size) + 1.5, PORT_ROW + .05 + .15 * size + size / 2 + wall(size)) - 1e-9);
    }

    @Override
    public Orientation orientation() {
        return Orientation.VERTICAL;
    }

    @Override
    public String sizeRow() {
        return "trickle_bed_reactor.diameter";
    }

    @Override
    public String lengthRow() {
        return "trickle_bed_reactor.beds";
    }

    @Override
    public boolean lengthInMetres() {
        return false;
    }

    @Override
    public String supportRow() {
        return "trickle_bed_reactor.skirt";
    }

    @Override
    public Line held(EquipmentShape shape) {
        int beds = (int) shape.tangentLength();
        return new Line("trickle_bed_reactor.catalyst", number(catalyst(shape.diameter(), beds)), beds);
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        double d = shape.diameter();
        int beds = (int) shape.tangentLength();
        return List.of(new Line("trickle_bed_reactor.vessel", size(d), number(tangentLength(d, beds)), size(wall(d))),
                new Line("trickle_bed_reactor.beds", beds, number(bed(d)), number(catalyst(d, beds))));
    }

    @Override
    void build(ProcessUnit u) {
        double d = u.shape.diameter(), r = d / 2, shell = r + wall(d), wall = shell + insulation(d), skirt = shell + FIREPROOFING;
        int beds = (int) u.shape.tangentLength(), s = u.shape.support();
        double hb = bed(d), zq = quenchZone(d), zt = topZone(d), yB = s, yT = s + tangentLength(d, beds);
        int n = ProcessUnit.sides(2 * wall), level = (int) Math.floor(yT);
        double outer = wall + ColumnStructure.MIN_DECK, a = ProcessUnit.centre(outer + .1);
        var up = Frame.of(new double[]{a, 0, a}, 1, 1);

        // ---- The shell, the skirt and its pedestal.
        cladShell(u, a, yB, yT, wall, skirt, n);
        double apothem = skirt + PEDESTAL_REACH;
        pedestal(u, a, apothem, PEDESTAL);
        skirt(u, a, skirt, wall(d) + FIREPROOFING, PEDESTAL, yB, yB, n);
        u.volumes.add(Solid.rod(new double[]{a, 0, a}, 1, PEDESTAL + .05, yB, skirt - .1, 0));
        baseRing(u, a, skirt, PEDESTAL, d, n);
        // Two access openings (closed covers seen dark), a vent row under the head seam.
        double half = Math.min(.3, .35 * skirt);
        for (double t : new double[]{Math.PI / 4, 5 * Math.PI / 4}) {
            u.mesh.block(at(a, skirt + .02, t, PEDESTAL + .7), scale(radial(t), .02), new double[]{0, .5, 0}, scale(tangent(t), half + .06), Material.SUPPORT);
            u.mesh.block(at(a, skirt + .045, t, PEDESTAL + .7), scale(radial(t), .005), new double[]{0, .42, 0}, scale(tangent(t), half), Material.OPENING);
        }
        for (int k = 0; k < 8; k++) {
            double t = Math.PI / 8 + k * Math.PI / 4;
            u.mesh.block(at(a, skirt + .005, t, yB - .4), scale(radial(t), .005), new double[]{0, .05, 0}, scale(tangent(t), .05), Material.OPENING);
        }
        // A catalyst dump nozzle from the bottom head (0.6 of its radius out), 45° down through a sleeve in the skirt.
        double dump = Math.max(.06, .04 * d), rho = .6 * r, yDump = yB - Math.sqrt(shell * shell - rho * rho) - (skirt - rho);
        dumpNozzle(u, a, skirt, 3 * Math.PI / 4, yDump, dump, .3 + 2 * dump);

        // ---- The bottom ports: the effluent through its sleeve to the front, the quench out of its trunk to the left.
        double corner = pedestalCorner(skirt + PEDESTAL_REACH), zFace = Math.floor(a - corner - .1), eff = Math.min(1, .15 * d), zEff = a - skirt + .15;
        u.nozzles.add(new Nozzle(Role.EFFLUENT, new double[]{a, PORT_ROW, zEff}, 2, -1, zEff - zFace, eff, ColumnStructure.size(.15 * d, eff), 0, .17));
        u.mesh.ring(Frame.of(new double[]{a, PORT_ROW, 0}, 2, 1), eff / 2 + .01, eff / 2 + .07, a - skirt - .06, a - skirt + .04, ProcessUnit.sides(eff), 1, Material.SUPPORT);
        double q = Math.min(1, Math.max(.1, .05 * d)), rq = Math.max(.08, .7 * q), rr = wall + .12 + rq / 2, xFace = Math.ceil(a + Math.max(corner, rr + q / 2) + .15);
        u.nozzles.add(new Nozzle(Role.QUENCH, new double[]{a + rr, PORT_ROW, a}, 0, 1, xFace - a - rr, q, ColumnStructure.size(.05 * d, q), 0, q / 2 + .03));

        // ---- The quench zones and the platforms: a ring 1.2 m under each injection, rest platforms between.
        int risers = Math.max(1, beds - 1);
        var quench = new double[risers];
        var wanted = new ArrayList<Integer>();
        if (beds == 1) quench[0] = Math.min(yT - zt / 2, level - .8);
        for (int k = 1; k < beds; k++) quench[k - 1] = yT - zt - k * hb - (k - .5) * zq;
        for (int k = beds - 1; k >= 1; k--) wanted.add((int) Math.floor(quench[k - 1] - 1.2));
        var levels = new ArrayList<Integer>();
        var rings = new ArrayList<Boolean>();
        levels(wanted, level, levels, rings);
        access(u, a, wall, levels, rings, level, Math.PI / 2, Math.PI);
        double girder = .3 + .04 * d;
        squareDeck(u, a, wall, level, BACK, girder);

        // ---- The quench trunk up the left from its port to the top header, the header round the front left at the skirt's
        // top, a riser from it to each zone's nozzle with its valve 0.8 m over the zone's deck; the first riser is the trunk's.
        double step = (rq + .3) / rr, yh = Math.max(PORT_ROW + .9, Math.min(yB - .6, (levels.isEmpty() ? level : levels.getFirst()) - .9));
        double[] port = {a + rr, PORT_ROW, a}, branch = {a + rr, yh, a};
        u.line(q / 2, Material.PIPE, port, branch);
        u.mesh.sphere(port, .52 * q, ProcessUnit.sides(q), Material.PIPE);
        u.solids.add(Solid.rod(new double[]{a + rr, 0, a}, 1, PORT_ROW, yh, q / 2, 0));
        if (risers > 1) {
            var header = new ArrayList<double[]>();
            double last = -(risers - 1) * step;
            int pieces = (int) Math.ceil(-last / .35);
            for (int k = 0; k <= pieces; k++) header.add(at(a, rr, last * k / pieces, yh));
            u.line(q / 2, Material.PIPE, header.toArray(double[][]::new));
            u.mesh.sphere(header.getFirst(), .52 * q, ProcessUnit.sides(q), Material.PIPE);
            u.mesh.sphere(header.getLast(), .52 * q, ProcessUnit.sides(q), Material.PIPE);
            for (int k = 0; k + 1 < header.size(); k++) u.solids.add(tube(header.get(k), header.get(k + 1), q / 2));
        }
        var fittings = new ArrayList<double[]>();
        for (int k = 1; k <= risers; k++) {
            double t = -(k - 1) * step, y = quench[k - 1];
            double[] foot = at(a, rr, t, yh), bend = at(a, rr, t, y), into = at(a, wall - .05, t, y);
            u.line(rq / 2, Material.PIPE, foot, bend, into);
            if (k > 1) u.mesh.sphere(foot, .52 * q, ProcessUnit.sides(q), Material.PIPE);
            u.mesh.ring(new Frame(at(a, wall + .04, t, y), radial(t), UP), rq / 2, PipeSizes.flange(rq) / 2 + .02, 0, .08, 12, 3, Material.FLANGE);
            u.solids.add(Solid.rod(new double[]{foot[0], 0, foot[2]}, 1, yh, y, rq / 2, 0));
            u.solids.add(tube(into, bend, rq / 2));
            fittings.add(new double[]{t, (PipeSizes.flange(rq) / 2 + .1) / wall, y - .2, y + .2});
            if (beds > 1) valve(u, foot[0], wanted.get(beds - 1 - k) + Access.DECK + .8, foot[2], rq / 2, radial(t));
        }
        if (beds == 1 && yh - PORT_ROW >= 1.2) valve(u, a + rr, (PORT_ROW + yh) / 2, a, q / 2, new double[]{1, 0, 0});

        // ---- The top: the nozzle, its elbow's run to the front with the hydrogen tee, the davit.
        double p = Math.min(1, Math.max(.15, .15 * d)), h2 = Math.min(1, Math.max(.1, .1 * d)), edge = wall + TOP_DECK;
        var top = topNozzle(u, a, a, yT + wall, p, level + Access.DECK + Access.RAIL + .12 + p / 2);
        double run = top[0], zEnd = top[1], zTee = Math.floor(zEnd - h2 / 2 - .08 - .5) + .5;
        u.mesh.prism(Frame.of(new double[]{a, run, 0}, 2, 1), p / 2, zTee, zEnd, ProcessUnit.sides(p), Material.PIPE);
        u.solids.add(Solid.rod(new double[]{a, run, 0}, 2, zTee - p / 2, zEnd, p / 2, 0));
        double zFeed = Math.floor(a - edge - .05), xH2 = Math.ceil(a + edge + .05);
        u.nozzles.add(new Nozzle(Role.FEED, new double[]{a, run, zTee + .03}, 2, -1, zTee + .03 - zFeed, p, ColumnStructure.size(p, p), 0, h2 / 2 + .03));
        u.nozzles.add(new Nozzle(Role.HYDROGEN, new double[]{a, run, zTee}, 0, 1, xH2 - a, h2, ColumnStructure.size(h2, h2), 0, p / 2 + .03));
        double deck = level + Access.DECK;
        davit(u, a, 3 * Math.PI / 4, wall + .6, deck, Math.max(deck + 3, run + p / 2 + 1.4));

        // ---- Trunnions under the top deck's girders on the diagonal between the ladders and the risers.
        double trunnion = Math.max(.06, .075 * d), proud = Math.max(.15, .12 * d), yTr = level - girder - trunnion - .15;
        for (double t : new double[]{-Math.PI / 4, 3 * Math.PI / 4}) {
            var f = new Frame(at(a, wall - .05, t, yTr), radial(t), UP);
            u.mesh.prism(f, trunnion, 0, proud + .05, 12, Material.SHELL);
            u.mesh.cylinder(f, trunnion + .04, proud + .05, proud + .1, 12, Material.SHELL);
            u.solids.add(tube(at(a, wall, t, yTr), at(a, wall + proud + .1, t, yTr), trunnion + .04));
            fittings.add(new double[]{t, (trunnion + .1) / wall, yTr - trunnion - .1, yTr + trunnion + .1});
        }

        // ---- A thermowell at the top, middle and bottom of each bed, back left, off the platforms.
        var heights = new ArrayList<Double>();
        for (int b = 1; b <= beds; b++) {
            double bedTop = yT - zt - (b - 1) * (hb + zq);
            for (double y : new double[]{bedTop - .3, bedTop - hb / 2, bedTop - hb + .3}) {
                boolean clear = true;
                for (int lv : levels) clear &= y < lv - .75 || y > lv + Access.DECK + .15;
                if (!clear) continue;
                thermowell(u, a, wall, Math.PI / 4, y);
                fittings.add(new double[]{Math.PI / 4, .2 / wall, y - .1, y + .1});
            }
        }
        // ---- Flashing bands every 3.5 m, off the decks' brackets.
        for (double y = yB + 1.5; y < yT - .5; y += 3.5) {
            boolean clear = y < level - girder - .1;
            for (int lv : levels) clear &= y < lv - .75 || y > lv + Access.DECK + .1;
            if (clear) heights.add(y);
        }
        bands(u, a, wall, n, heights, fittings);
        u.round(a, a, PEDESTAL, yB, skirt, 1);
    }
}
