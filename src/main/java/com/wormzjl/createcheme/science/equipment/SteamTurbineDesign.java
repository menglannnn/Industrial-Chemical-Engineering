package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.equipment.Mesh.Frame;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.List;

/**
 * A back-pressure steam turbine-generator set (owner decision D2 2026-10-18, {@code NEW_UNITS_PLAN.md};
 * {@code NEW_UNITS_SURVEY_ROTATING.md} section 2): the refinery's let-down turbine, exhausting at 1.5 to 25 bar into a
 * header. Its main size is the exhaust-end casing's outside diameter Dc, 0.6 to 3 m in 0.1 m steps (real 0.5 m for a
 * single-stage turbine to about 4 m; about 5 Dc² MW); its second size its stages, 1 to 12 (1 is the API 611
 * single-stage look with hand valves). The train ({@link SteamTurbineTrain}) stands at grade on a steel base frame
 * holding its oil (the SST-100 keeps its oil system in its base frame), 0.25 to 0.45 m deep, on a chamfered concrete
 * plinth of 0.3 to 0.55 m (survey: 0.3 to 1 m), its shaft 0.6 Dc over the frame; the auxiliary oil pump's motor stands on
 * the frame beside the front standard. The exhaust rises from the exhaust barrel's top through a flanged expansion
 * joint of 0.15 Dc (0.15 m at least) to its flange, 0.4 Dc across (less on a casing too short for it), a concentric
 * reducer taking a larger one to the 1 m nozzle; a single-stage casing carries a sentinel valve. Steam enters the
 * trip-and-throttle valve from the side (+x), on the cell row nearest the steam chest's axis that its stand reaches.
 * Nozzles: STEAM into the trip-and-throttle valve (0.15 Dc, 0.05 m at least) and EXHAUST up (0.4 Dc, at most 1 m).
 */
final class SteamTurbineDesign extends UnitDesign {
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

    @Override
    public int minSupport(double size, int length) {
        return 0;
    }

    /** The plinth stands at grade. */
    @Override
    public int maxSupport() {
        return 0;
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
        return null;
    }

    @Override
    public Line held(EquipmentShape shape) {
        double d = shape.diameter();
        return new Line("steam_turbine.power", number(SteamTurbineTrain.power(d)), whole(SteamTurbineTrain.speed(d)));
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        double d = shape.diameter();
        return List.of(new Line("steam_turbine.casing", (int) shape.tangentLength(), size(d)),
                new Line("steam_turbine.power", number(SteamTurbineTrain.power(d)), whole(SteamTurbineTrain.speed(d))));
    }

    @Override
    void build(ProcessUnit u) {
        double d = u.shape.diameter();
        var t = new SteamTurbineTrain(d, (int) u.shape.tangentLength(), false);
        var m = u.mesh;
        double plinth = .25 + .1 * d, frame = Math.min(.45, .15 + .1 * d), base = plinth + frame, shaft = base + t.rise, rb = t.valveBody(), od = t.steamLine();
        // The base frame as wide as the generator and its cooling water lines, the plinth 0.12 m wider.
        double wb = Math.max(.5 * d + .15, 1.25 * t.hg + .25), wp = wb + .12;
        // Across x: the shaft on a cell centre, the plinth's and the train's left side in the first cell; the trip-and-throttle
        // valve's handwheel out of the exhaust's column of cells (a small set's exhaust rises past it).
        double a = Math.ceil(Math.max(wp + .02, t.leftReach()) - .5) + .5, xv = a + Math.max(t.valveOffset(), .52 + t.handwheel() / 2);
        // Along z: the exhaust's centre line on a cell centre, the plinth's front in the first cell.
        double lead = Math.max(.3, t.frontReach());
        double zE = Math.ceil(lead + t.exhaust - .5 - 1e-9) + .5, z0 = zE - t.exhaust;
        // The trip-and-throttle valve's inlet on the cell row nearest the chest's axis that its stand reaches; its stand
        // on the base frame, the plinth or the ground.
        double stand = xv < a + wb ? base : xv < a + wp ? plinth : 0, yc = base + t.chestAxis();
        double yv = Math.max(Math.floor(yc) + .5, Math.ceil(stand + rb + .12 - .5) + .5), zv = Math.floor(z0 + t.chest) + .5;
        t.draw(u, a, base, z0, xv, yv, zv);
        u.box(new double[]{xv - .05, stand, zv - .05}, new double[]{xv + .05, yv - rb, zv + .05}, Material.STEELWORK, true);
        u.box(new double[]{xv - .1, stand, zv - .1}, new double[]{xv + .1, stand + .02, zv + .1}, Material.STEELWORK, false);

        // The base frame with its drip rim, on the chamfered plinth.
        double zf0 = z0 - .15, zf1 = z0 + t.length() + .1, chamfer = .04;
        u.box(new double[]{a - wb, plinth, zf0}, new double[]{a + wb, base, zf1}, Material.STEELWORK, true);
        for (int side = -1; side <= 1; side += 2) {
            double x0 = side < 0 ? a - wb : a + wb - .03, ze = side < 0 ? zf0 : zf1 - .03;
            u.box(new double[]{x0, base, zf0}, new double[]{x0 + .03, base + .05, zf1}, Material.STEELWORK, false);
            u.box(new double[]{a - wb + .03, base, ze}, new double[]{a + wb - .03, base + .05, ze + .03}, Material.STEELWORK, false);
        }
        u.extrude(List.of(new double[]{a - wp, 0}, new double[]{a + wp, 0}, new double[]{a + wp, plinth - chamfer}, new double[]{a + wp - chamfer, plinth},
                new double[]{a - wp + chamfer, plinth}, new double[]{a - wp, plinth - chamfer}), zf0 - .12, zf1 + .12, Material.CONCRETE);
        u.solids.add(ProcessUnit.Solid.box(new double[]{a - wp, 0, zf0 - .12}, new double[]{a + wp, plinth, zf1 + .12}));
        // The auxiliary oil pump on the frame beside the front standard: its casing, its motor and fan cowl.
        double sx = Math.max(.2, .32 * d), xo = a - (sx + wb) / 2, ro = Math.min(.1 + .03 * d, (wb - sx) / 2 - .04), zo = z0 + .5 * t.standard;
        var oil = Frame.of(new double[]{xo, 0, zo}, 1, 1);
        m.cylinder(oil, .8 * ro, base, base + .6 * ro, 12, Material.MACHINE);
        m.cylinder(oil, ro, base + .6 * ro, base + 2.6 * ro, 12, Material.MOTOR);
        m.cylinder(oil, .9 * ro, base + 2.6 * ro, base + 3 * ro, 12, Material.MOTOR);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{xo, 0, zo}, 1, base, base + 3 * ro, ro, 0));

        // The exhaust: a branch up from the barrel's top, its flange pair, the expansion joint's convolutions, a reducer
        // to a nozzle of 1 m where the branch is larger, the exhaust flange and its nuts.
        double len = t.exhaustEnd - t.coneEnd, rbr = Math.min(.2 * d, .5 * len - .02 * d), oe = Math.min(1, 2 * rbr);
        var up = Frame.of(new double[]{a, 0, zE}, 1, 1);
        int ne = ProcessUnit.sides(2 * rbr);
        double y1 = shaft + d / 2 + .06 * d + .04, joint = Math.max(.15, .15 * d), y2 = y1 + joint, rr = rbr + .03 + .02 * d, tf = .03 + .01 * d;
        m.prism(up, rbr, shaft + .3 * d, y1 - tf, ne, Material.MACHINE);
        m.ring(up, rbr, rr, y1 - tf, y1, ne, 3, Material.FLANGE);
        m.prism(up, rbr + .01, y1, y2, ne, Material.PIPE);
        int folds = Math.max(3, (int) Math.round(joint / .07));
        for (int k = 0; k < folds; k++) {
            double y = y1 + joint * (k + .5) / folds, w = .015 + .005 * d;
            m.ring(up, rbr + .01, rbr + .03 + .015 * d, y - w, y + w, ne, 3, Material.PIPE);
        }
        m.ring(up, rbr, rr, y2, y2 + tf, ne, 3, Material.FLANGE);
        double y3 = y2 + tf, step = 2 * rbr > oe + 1e-9 ? .2 * d : .05 + .03 * d;
        if (2 * rbr > oe + 1e-9) m.frustum(up, rbr, oe / 2, y3, y3 + step, ne, Material.MACHINE);
        else m.prism(up, rbr, y3, y3 + step, ne, Material.MACHINE);
        y3 += step;
        double rf = PipeSizes.flange(oe) / 2, face = y3 + tf;
        m.cylinder(up, rf, y3, face, ProcessUnit.sides(2 * rf), Material.FLANGE);
        m.bolts(up, oe, oe / 2 + .03, rf, face, 1, Material.BOLT);
        u.solids.add(ProcessUnit.Solid.rod(new double[]{a, 0, zE}, 1, shaft + .3 * d, face, rr, 0));
        u.round(a, zE, y1, y2, rbr + .01, 1);
        // A single-stage casing's sentinel valve on the exhaust barrel's upper -x side.
        if (t.stages == 1) {
            double[] dir = {DeterministicMath.cos(.75 * Math.PI), DeterministicMath.sin(.75 * Math.PI), 0};
            var fs = new Frame(new double[]{a + .48 * d * dir[0], shaft + .48 * d * dir[1], zE}, dir, new double[]{0, 0, 1});
            double rs = .03 + .015 * d;
            m.cylinder(fs, rs, 0, .05 + .02 * d, 10, Material.FLANGE);
            m.cylinder(fs, .7 * rs, .05 + .02 * d, .15 + .06 * d, 10, Material.MACHINE);
            m.cylinder(fs, .9 * rs, .15 + .06 * d, .18 + .07 * d, 10, Material.MACHINE);
        }

        // Steam into the valve from the side, its pipe block two columns at least from the exhaust's; the exhaust up.
        double inlet = xv + rb + .08, steamFace = Math.max(Math.ceil(inlet + EquipmentShape.STAND_OUT), a + 1.5);
        u.nozzles.add(new Nozzle(Role.STEAM, new double[]{inlet - .01, yv, zv}, 0, 1, steamFace - inlet + .01, od, od, 0, .03));
        u.nozzles.add(new Nozzle(Role.EXHAUST, new double[]{a, face - .01, zE}, 1, 1, Math.ceil(face + EquipmentShape.STAND_OUT) - face + .01, oe, oe, 0, .03));
    }
}
