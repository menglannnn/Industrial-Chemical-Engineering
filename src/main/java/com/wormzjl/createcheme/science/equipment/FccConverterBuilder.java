package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Solid;
import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * Draws an FCC converter ({@link FccConverterDesign}) from its {@link FccConverterLayout} ({@code NEW_UNITS_SURVEY_FCC_COKER.md}
 * section 1; owner decisions D3 and D4 of 2026-10-18): a side-by-side UOP/Shell converter with a single-stage regenerator.
 * <ul>
 * <li>The regenerator: a squat shell with 2:1 heads on a concrete table of four columns on its diagonals, steel lugs at
 * its bottom tangent, tie beams front and back; two ring platforms (the lower 2 m over the bottom tangent, the upper at
 * about two thirds of the shell) with a manway each, reached by caged ladders on the -x side; the air line rising out of
 * the start-up air heater in front and turning up into the bottom head, the flue gas line out of the crown.</li>
 * <li>The riser on its stand with bellows, the feed injectors pointing 30° up into it in a ring with their feed and
 * atomising steam ring headers, open towards the regenerated-catalyst standpipe; the stripper over it with its steam ring,
 * the 30° cone and the reactor (disengager) with its manway, the vapour line out of its crown.</li>
 * <li>Both standpipes with their slide valves (body, bonnet, stem and hydraulic actuator towards the front, where the
 * operator stands).</li>
 * <li>The steel frame round the riser and reactor: columns fireproofed to the first floor, a beam under every deck's edges,
 * struts at most 8 m apart between floors more than 9 m apart, X-bracing on the front and back faces over the spent-catalyst valve's floor,
 * the -x face open in the middle where the standpipes pass. Floors at the feed injectors, both slide valves, the stripper's
 * steam ring, the reactor's manway and its top, each a pinwheel of grating decks round the riser's or vessel's opening
 * (its corners filled in to the vessel), open to the -x face where a standpipe passes (railed along the opening).</li>
 * <li>The stair tower on the +x face: two lanes of 1 m flights, at most 3.6 m a flight, joined by landings at both ends,
 * opening onto every floor through its +x handrail.</li>
 * </ul>
 * Lines wider than 1 m (air, flue gas, vapour) are drawn at their size and end in a 15° concentric reducer to a 1 m
 * nozzle. The layout's x and z are shifted half a block ({@link #O}) so both axes run through cell centres.
 */
final class FccConverterBuilder {
    private static final double[] UP = {0, 1, 0}, DOWN = {0, -1, 0}, PLUS_X = {1, 0, 0}, MINUS_X = {-1, 0, 0}, PLUS_Z = {0, 0, 1}, MINUS_Z = {0, 0, -1};
    /** The layout's origin (the regenerator's axis) on a cell's centre. */
    private static final double O = .5;
    /** A floor beam's depth and width, a strut's depth, a column's section, its fireproofing's, m. */
    static final double BEAM = .4, BEAM_WIDTH = .3, STRUT = .3, COLUMN = .35, FIREPROOF = .5;
    /** The stair tower: a lane's width, a flight's width, a landing's depth beyond a flight's foot, a flight's largest rise, m. */
    static final double LANE = 1.1, FLIGHT = 1, LAND = 1.1, FLIGHT_RISE = 3.6;
    /** The feed injectors' slope, 30° up into the riser. */
    private static final double TAN30 = 1 / Math.sqrt(3), COS30 = Math.sqrt(3) / 2;
    /** A vessel's inside kept this far within its surface, m (less than half a block: see {@link #inside}). */
    private static final double INSIDE = .3;

    /** A floor of the frame: its walking surface, its opening's half size, its decks' -x end, whether it is open to the -x face, the radius its opening's corners are filled to (0: not). */
    private record Floor(double surface, double hole, double from, boolean open, double fill) {
        double level() {
            return surface - Access.DECK;
        }
    }

    /** A stair flight of the tower: from walking level {@code base} to {@code top}, its landing's deck {@code level}, its lane, the floor it reaches (-1: none), its length. */
    private record Flight(double base, double top, double level, boolean laneA, int floor, double length) {
    }

    private final ProcessUnit u;
    private final FccConverterLayout l;
    private final Mesh m;

    FccConverterBuilder(ProcessUnit u, FccConverterLayout l) {
        this.u = u;
        this.l = l;
        m = u.mesh;
    }

    void build() {
        regenerator();
        table();
        air();
        reactor();
        riser();
        injectors();
        standpipes();
        structure();
        lines();
    }

    // ---- Regenerator --------------------------------------------------------------------------------------------------

    /** The regenerator's shell and heads, its ring platforms, ladders and manways, its catalyst nozzles. */
    private void regenerator() {
        double r = l.r, y0 = l.support, y1 = y0 + l.straight;
        int n = vesselSides(l.d);
        var axis = p(0, 0, 0);
        var up = Frame.of(axis, 1, 1);
        m.prism(up, r, y0, y1, n, Material.SHELL);
        head(up, r, y1, l.head, n, Material.HEAD);
        head(Frame.of(axis, 1, -1), r, -y0, l.head, n, Material.HEAD);
        u.solids.add(Solid.rod(axis, 1, y0, y1, r, l.head));
        bottomHead(axis, r, y0, l.head);
        inside(axis, r, n, y0, y1, l.head, l.head);
        u.round(axis, 1, y0, y1, r);
        // Ring platforms round the half away from the frame (back, -x, front), split at their ladders: the lower one's from
        // the ground at -x, the upper one's standing on the lower. Their chords stray 5 cm from the circle.
        double wall = r + .1, width = ColumnStructure.MIN_DECK, edge = wall + width;
        double low = y0 + 2 - Access.DECK, high = Math.round(y0 + .45 * l.straight) - Access.DECK;
        double reach = wall + Access.STANDOFF - .05, cage = Access.CAGE_HALF + .2, clear = DeterministicMath.acos(reach / Math.sqrt(reach * reach + cage * cage));
        double ladderLow = Math.PI, ladderHigh = 5 * Math.PI / 6;
        double chord = Math.sqrt(8 * edge * .05);
        for (double[] ring : new double[][]{{low, ladderLow}, {high, ladderHigh}}) {
            double at = ring[1];
            u.platform(new Access.Arc(O, O, wall, Math.PI / 2 + .35, at - clear, edge, chord), ring[0], width, true, false);
            u.platform(new Access.Arc(O, O, wall, at + clear, 1.5 * Math.PI - .35, edge, chord), ring[0], width, false, true);
        }
        ladder(wall, ladderLow, 0, low + Access.DECK + Access.LANDING);
        ladder(wall, ladderHigh, low + Access.DECK, high + Access.DECK + Access.LANDING);
        manway(0, 0, r, 7 * Math.PI / 6 + .15, low + Access.DECK + .9, n);
        manway(0, 0, r, 13 * Math.PI / 12, high + Access.DECK + .9, n);
        // Fresh catalyst in at the front over the bed; equilibrium catalyst out at the back just over the bottom tangent.
        u.nozzles.add(ProcessUnit.radialSide(Role.CATALYST_INLET, O, y0 + 4.5, O, r, n, .15, .1));
        u.nozzles.add(radialBack(Role.CATALYST_OUTLET, O, y0 + .5, O, r, n, .2, .15));
    }

    /** A caged ladder on the regenerator at angle {@code t}, its rung plane {@link Access#STANDOFF} beyond the platforms' wall. */
    private void ladder(double wall, double t, double base, double top) {
        double rung = wall + Access.STANDOFF, c = DeterministicMath.cos(t), s = DeterministicMath.sin(t);
        u.ladder(O + rung * c, O + rung * s, new double[]{c, 0, s}, Access.STANDOFF + .12, base, top, false);
    }

    /** A manway on a round shell (axis at local (x, z), radius {@code r}) at angle {@code t}, centred at {@code y}: neck, cover and hinge. */
    private void manway(double x, double z, double r, double t, double y, int n) {
        double c = DeterministicMath.cos(t), s = DeterministicMath.sin(t);
        double[] out = {c, 0, s};
        var f = new Frame(p(x + (r - .1) * c, y, z + (r - .1) * s), out, UP);
        m.prism(f, .3, 0, .45, 12, Material.SHELL);
        m.cylinder(f, .4, .45, .53, 12, Material.FLANGE);
        m.bar(f.at(Math.PI / 2, .35, .5), f.at(Math.PI / 2, .35, .7), .05, UP, Material.STEELWORK);
    }

    /** The bottom head's collision: three discs under the tangent at {@code y0}. */
    private void bottomHead(double[] axis, double r, double y0, double depth) {
        for (int k = 0; k < 3; k++) {
            double mid = (k + .5) / 3;
            u.solids.add(Solid.rod(axis, 1, y0 - depth * (k + 1) / 3, y0 - depth * k / 3, r * Math.sqrt(1 - mid * mid), 0));
        }
    }

    /**
     * A standing vessel's inside (axis at frame point {@code axis}, radius {@code r} to its {@code n} vertices) from its
     * bottom tangent {@code y0} (a 2:1 head {@code low} deep under it, 0: none) to its top tangent {@code y1} (a head
     * {@code high} deep over it), {@link #INSIDE} within its surface: every cell whose centre the surface does not come
     * within half a block of is inside it, so the inside's cells are enclosed by the cells the surface passes through. The
     * bottom head's inside is a disc per cell row (the volume's rod has a head only at its top).
     */
    private void inside(double[] axis, double r, int n, double y0, double y1, double low, double high) {
        double ri = r * DeterministicMath.cos(Math.PI / n) - INSIDE, lo = low - INSIDE, hi = Math.max(0, high - INSIDE);
        if (ri < .3) return;
        u.volumes.add(Solid.rod(axis, 1, y0, y1, ri, hi));
        if (lo <= 0) return;
        for (int k = (int) Math.floor(y0 - lo); k < y0; k++) {
            double centre = k + .5, depth = (y0 - centre) / lo;
            if (centre >= y0 || depth >= 1) continue;
            u.volumes.add(Solid.rod(axis, 1, k, Math.min(k + 1, y0), ri * Math.sqrt(1 - depth * depth), 0));
        }
    }

    /**
     * A 2:1 head as {@link Mesh#head} draws it, in fewer bands (a sixth of its sides, four at least, against a quarter): a
     * large vessel's heads cost a third fewer quads and keep their outline.
     */
    private void head(Frame f, double r, double at, double depth, int n, Material material) {
        int bands = Math.max(4, n / 6);
        var base = f.along(at);
        var spheroid = new Spheroid(base, f.axis(), r, depth);
        for (int k = 0; k < bands; k++) {
            double p0 = Math.PI / 2 * k / bands, p1 = Math.PI / 2 * (k + 1) / bands;
            double r0 = r * DeterministicMath.cos(p0), r1 = k + 1 == bands ? 0 : r * DeterministicMath.cos(p1);
            double z0 = at + depth * DeterministicMath.sin(p0), z1 = at + depth * DeterministicMath.sin(p1);
            for (int i = 0; i < n; i++) {
                double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
                var polygon = new ArrayList<double[]>(4);
                polygon.add(f.at(a0, r0, z0));
                polygon.add(f.at(a1, r0, z0));
                if (r1 > 0) {
                    polygon.add(f.at(a1, r1, z1));
                    polygon.add(f.at(a0, r1, z1));
                } else polygon.add(f.along(z1));
                m.add(polygon, base, spheroid, material, null, null);
            }
        }
    }

    // ---- Concrete table -----------------------------------------------------------------------------------------------

    /**
     * The regenerator's table: four square concrete columns on its diagonals just outside the shell, a steel lug on each
     * under the bottom tangent, tie beams front and back (the air line passes under the front one, the standpipes over the
     * +x side, the ladder at -x).
     */
    private void table() {
        double c = l.tableColumn, k = Math.sqrt(.5) * l.tableRadius, top = l.support - .3, y1 = l.support + .5;
        for (int i = 0; i < 4; i++) {
            double sx = i == 0 || i == 3 ? 1 : -1, sz = i < 2 ? 1 : -1, cx = sx * k, cz = sz * k;
            u.box(p(cx - c / 2, 0, cz - c / 2), p(cx + c / 2, top, cz + c / 2), Material.CONCRETE, true);
            double[] radial = {sx * Math.sqrt(.5), 0, sz * Math.sqrt(.5)}, across = {-radial[2], 0, radial[0]};
            double inner = l.r - .05, outer = l.tableRadius + .3 * c;
            var mid = p((inner + outer) / 2 * radial[0], (top + y1) / 2, (inner + outer) / 2 * radial[2]);
            m.block(mid, scale(radial, (outer - inner) / 2), new double[]{0, (y1 - top) / 2, 0}, scale(across, .06), Material.STEELWORK);
            m.block(p(cx, top + .03, cz), scale(radial, c * .4), new double[]{0, .03, 0}, scale(across, c * .35), Material.STEELWORK);
            for (int side = -1; side <= 1; side += 2)
                m.block(plus(mid, scale(across, side * .25)), scale(radial, (outer - inner) / 2), new double[]{0, (y1 - top) / 2, 0}, scale(across, .03), Material.STEELWORK);
            u.solids.add(ProcessUnit.span(p(inner * radial[0] - .3 * across[0], top, inner * radial[2] - .3 * across[2]), p(outer * radial[0] + .3 * across[0], y1, outer * radial[2] + .3 * across[2])));
        }
        double depth = .5 + .02 * l.d, wide = .4 + .02 * l.d;
        double y = Math.max(.5 * l.support, l.airOut + l.air / 2 + .45 + depth / 2);
        for (int sz = -1; sz <= 1; sz += 2)
            u.box(p(-k + c / 2, y - depth / 2, sz * k - wide / 2), p(k - c / 2, y + depth / 2, sz * k + wide / 2), Material.CONCRETE, true);
    }

    // ---- Air -----------------------------------------------------------------------------------------------------------

    /**
     * The start-up air heater in front of the regenerator (a vertical refractory-lined shell on four legs, its burner on
     * the front near the top) and the air line: from the blower's nozzle at the front, up beside the heater, into its upper
     * side, out of its lower back, along +z under the regenerator and up into the bottom head through its bellows.
     */
    private void air() {
        double a = l.air, ra = a / 2, hz = l.heaterZ, hr = l.heater / 2, yb = l.heaterBottom, yt = l.heaterTop, ax = l.airRiserX;
        int nh = Math.max(16, 4 * (int) Math.ceil(.4 * l.heater + 2)), np = lineSides(a);
        var axis = p(0, 0, hz);
        var up = Frame.of(axis, 1, 1);
        m.prism(up, hr, yb, yt, nh, Material.SHELL);
        head(up, hr, yt, hr / 2, nh, Material.HEAD);
        head(Frame.of(axis, 1, -1), hr, -yb, hr / 2, nh, Material.HEAD);
        u.solids.add(Solid.rod(axis, 1, yb, yt, hr, hr / 2));
        bottomHead(axis, hr, yb, hr / 2);
        inside(axis, hr, nh, yb, yt, hr / 2, hr / 2);
        u.round(axis, 1, yb, yt, hr);
        for (int i = 0; i < 4; i++) {
            double sx = i == 0 || i == 3 ? 1 : -1, sz = i < 2 ? 1 : -1, lx = sx * .5 * hr, lz = hz + sz * .5 * hr;
            u.box(p(lx - .1, 0, lz - .1), p(lx + .1, yb, lz + .1), Material.STEELWORK, true);
        }
        // The burner box and the fuel gas nozzle under it on the front.
        double box = Math.min(.4, .25 * hr + .15), yFuel = Math.floor(yt - .6) + .5, zBox = hz - hr - .45;
        u.box(p(-box, yFuel - box, zBox), p(box, yFuel + box, hz - hr + .1), Material.MACHINE, true);
        u.nozzles.add(ProcessUnit.front(Role.FUEL, O, yFuel, zBox + O, .15, .1));
        // The air riser beside the heater: its nozzle low at the front, an elbow up, an elbow into the heater's side.
        double yLow = Math.ceil(ra - .35 - 1e-9) + .5, r1 = -ax - hr, r2 = a;
        var top = elbow(p(ax, l.airIn - r1, hz), UP, PLUS_X, r1, ra, np, Material.PIPE);
        tube(top, p(-hr + .2, l.airIn, hz), ra, np, Material.PIPE);
        tube(p(ax, yLow + r2, hz), p(ax, l.airIn - r1, hz), ra, np, Material.PIPE);
        var low = elbow(p(ax, yLow + r2, hz), DOWN, MINUS_Z, r2, ra, np, Material.PIPE);
        double zEnd = Math.min(hz - r2 - .5, hz - hr - .3);
        tube(low, p(ax, yLow, zEnd), ra, np, Material.PIPE);
        u.solids.add(Solid.rod(p(ax, 0, hz), 1, yLow + r2, l.airIn - r1, ra, 0));
        u.solids.add(Solid.rod(p(ax, yLow, 0), 2, zEnd + O, hz - r2 + O, ra, 0));
        var end = p(ax, yLow, zEnd);
        if (a > 1) end = reducer(end, MINUS_Z, ra, .5, np, Material.PIPE);
        double airOd = a > 1 ? 1 : ColumnStructure.size(a, 1);
        u.nozzles.add(ProcessUnit.front(Role.AIR, end[0], yLow, end[2], airOd, airOd));
        // Out of the heater's lower back, under the regenerator and up into its bottom head.
        double r3 = a, yo = l.airOut, apex = l.support - l.head;
        tube(p(0, yo, hz + hr - .2), p(0, yo, -r3), ra, np, Material.PIPE);
        var turn = elbow(p(0, yo, -r3), PLUS_Z, UP, r3, ra, np, Material.PIPE);
        tube(turn, p(0, apex + .3, 0), ra, np, Material.PIPE);
        bellows(Frame.of(p(0, 0, 0), 1, 1), ra, Math.max(yo + r3 + .2, apex - 1.2), np);
        u.solids.add(Solid.rod(p(0, yo, 0), 2, hz + hr + O, -r3 + O, ra, 0));
        u.solids.add(Solid.rod(p(0, 0, 0), 1, yo + r3, apex + .3, ra, 0));
        u.round(p(0, yo, 0), 2, hz + hr + O, -r3 + O, ra);
        u.round(p(0, 0, 0), 1, yo + r3, apex, ra);
        u.round(p(ax, 0, hz), 1, yLow + r2, l.airIn - r1, ra);
        u.round(p(ax, yLow, 0), 2, zEnd + O, hz - r2 + O, ra);
    }

    // ---- Reactor, stripper and riser ----------------------------------------------------------------------------------

    /** The reactor (disengager) with its top head and manway, the 30° cone, the stripper with its bottom head and steam ring. */
    private void reactor() {
        double x = l.x, rr = l.rr, rs = l.rs;
        int n = vesselSides(2 * rr);
        var axis = p(x, 0, 0);
        var up = Frame.of(axis, 1, 1);
        m.prism(up, rr, l.reactorBottom, l.reactorTop, n, Material.SHELL);
        head(up, rr, l.reactorTop, rr / 2, n, Material.HEAD);
        m.frustum(up, rs, rr, l.stripperTop, l.reactorBottom, n, Material.SHELL);
        m.prism(up, rs, l.stripperBottom, l.stripperTop, n, Material.SHELL);
        head(Frame.of(axis, 1, -1), rs, -l.stripperBottom, rs / 2, n, Material.HEAD);
        u.solids.add(Solid.rod(axis, 1, l.reactorBottom, l.reactorTop, rr, rr / 2));
        u.solids.add(Solid.cone(axis, 1, l.stripperTop, l.reactorBottom, rs, rr, 0));
        u.solids.add(Solid.rod(axis, 1, l.stripperBottom, l.stripperTop, rs, 0));
        bottomHead(axis, rs, l.stripperBottom, rs / 2);
        inside(axis, rr, n, l.reactorBottom, l.reactorTop, 0, rr / 2);
        u.volumes.add(Solid.cone(axis, 1, l.stripperTop, l.reactorBottom, rs - INSIDE / COS30, rr - INSIDE / COS30, 0));
        inside(axis, rs, n, l.stripperBottom, l.stripperTop, rs / 2, 0);
        u.round(axis, 1, l.reactorBottom, l.reactorTop, rr);
        u.round(axis, 1, l.stripperBottom, l.stripperTop, rs);
        manway(x, 0, rr, Math.PI / 4, l.manwayFloor + .9, n);
        // The stripping steam ring over its floor, four feeds into the shell.
        double ring = rs + .35, y = l.stripperBottom + 1.2, od = l.steamOd;
        int np = lineSides(od);
        arc(p(x, y, 0), PLUS_X, PLUS_Z, ring, 0, 2 * Math.PI, Math.max(12, (int) Math.ceil(2 * Math.PI * ring / 1.5)), od / 2, np, Material.PIPE);
        u.solids.add(Solid.ring(x + O, O, y - od / 2, y + od / 2, ring - od / 2, ring + od / 2, 0, 2 * Math.PI));
        for (int k = 0; k < 4; k++) {
            double t = Math.PI / 4 + k * Math.PI / 2, c = DeterministicMath.cos(t), s = DeterministicMath.sin(t);
            tube(p(x + ring * c, y, ring * s), p(x + (rs - .05) * c, y, (rs - .05) * s), od / 2, np, Material.PIPE);
        }
    }

    /** The riser: on its stand from its bottom head (lift steam) up into the stripper's bottom head, three bellows. */
    private void riser() {
        double x = l.x, r = l.rri, yB = l.wye - 1.2 * r - .3, ratio = r / l.rs;
        double top = l.stripperBottom - l.rs / 2 * Math.sqrt(1 - ratio * ratio) + .1;
        int n = Math.max(16, lineSides(2 * r));
        var axis = p(x, 0, 0);
        var up = Frame.of(axis, 1, 1);
        m.prism(up, r, yB, top, n, Material.SHELL);
        head(Frame.of(axis, 1, -1), r, -yB, r / 2, n, Material.HEAD);
        u.solids.add(Solid.rod(axis, 1, yB, top, r, 0));
        bottomHead(axis, r, yB, r / 2);
        inside(axis, r, n, yB, top - .3, r / 2, 0);
        u.round(axis, 1, yB, top, r);
        for (double y : new double[]{l.wye + 5.5, l.stripperBottom - l.rs / 2 - 2.5}) bellows(up, r, y, n);
        double leg = .2, at = Math.sqrt(.5) * Math.max(r, .3);
        for (int i = 0; i < 4; i++) {
            double sx = i == 0 || i == 3 ? 1 : -1, sz = i < 2 ? 1 : -1;
            u.box(p(x + sx * at - leg / 2, 0, sz * at - leg / 2), p(x + sx * at + leg / 2, yB, sz * at + leg / 2), Material.STEELWORK, true);
        }
        // The lift steam into the bottom head.
        tube(p(x, yB - r / 2 + .05, 0), p(x, .35, 0), .06, 8, Material.PIPE);
        tube(p(x, .35, 0), p(x + 1, .35, 0), .06, 8, Material.PIPE);
    }

    /** A riser's or a line's bellows at station {@code at} along frame {@code f}: two flanges and three convolutions. */
    private void bellows(Frame f, double r, double at, int n) {
        double ring = r + .05 + .03 * r, flange = r + .08 + .05 * r;
        for (int k = 0; k < 2; k++) {
            double z = at + .14 + k * .14;
            m.prism(f, ring, z, z + .07, n, Material.PIPE);
            m.annulus(f, r, ring, z, n, -1, Material.PIPE);
            m.annulus(f, r, ring, z + .07, n, 1, Material.PIPE);
        }
        for (double z : new double[]{at, at + .43}) {
            m.prism(f, flange, z, z + .06, n, Material.FLANGE);
            m.annulus(f, r, flange, z, n, -1, Material.FLANGE);
            m.annulus(f, r, flange, z + .06, n, 1, Material.FLANGE);
        }
    }

    /**
     * The feed injectors in a ring round the riser, 30° up into it, each fed from below by the feed ring header and its
     * atomising steam by the steam ring header under that; both headers open towards the regenerated-catalyst standpipe
     * (and the injectors clear of its slide valve's actuator where it stands among them). The feed and steam lines drop
     * from their headers through the floor and run under it to their nozzles beyond the front and back faces.
     */
    private void injectors() {
        double x = l.x, a0 = Math.PI + l.gap, a1 = 3 * Math.PI - l.gap, first = a0;
        if (l.rcsvOnInjectorFloor) {
            double rho = x - l.rcsvX, w = l.injectorOd / 2 + .15, inside = (rho - .5 * l.dsp - w) / l.outer;
            if (inside < 1) first = Math.max(first, Math.PI + DeterministicMath.acos(Math.max(-1, inside)) + w / l.outer);
        }
        int n = l.injectors;
        double reachIn = l.outer - (l.rri - .05), rise = reachIn * TAN30;
        for (int k = 0; k < n; k++) {
            double t = first + (a1 - first) * (k + .5) / n, c = DeterministicMath.cos(t), s = DeterministicMath.sin(t);
            double[] outer = p(x + l.outer * c, l.outerY, l.outer * s), back = p(x + (l.outer + .3) * c, l.outerY - .3 * TAN30, (l.outer + .3) * s);
            double[] inner = p(x + (l.rri - .05) * c, l.outerY + rise, (l.rri - .05) * s);
            tube(back, inner, l.injectorOd / 2, 8, Material.PIPE);
            var dir = unit(sub(inner, back));
            var f = new Frame(outer, dir, unit(cross(dir, UP)));
            m.cylinder(f, l.injectorOd / 2 + .06, -.04, .04, 8, Material.FLANGE);
            // Feed up from its header into the injector's back, atomising steam from its header round to the back's end.
            double fr = Math.max(.03, .3 * l.feedOd), sr = Math.max(.025, .3 * l.steamOd);
            tube(p(x + l.feedRing * c, l.feedY, l.feedRing * s), p(x + (l.outer + .15) * c, l.outerY - .1, (l.outer + .15) * s), fr, 8, Material.PIPE);
            double rs = l.steamRing;
            tube(p(x + rs * c, l.steamY, rs * s), p(x + rs * c, l.outerY - .2, rs * s), sr, 6, Material.PIPE);
            tube(p(x + rs * c, l.outerY - .2, rs * s), p(x + (l.outer + .3) * c, l.outerY - .2, (l.outer + .3) * s), sr, 6, Material.PIPE);
        }
        header(l.feedRing, l.feedY, l.feedOd, a0, a1);
        header(l.steamRing, l.steamY, l.steamOd, a0, a1);
        // The feed and steam lines: each leaves its header where it crosses x + 1, drops under the floor's beams and runs
        // out to its nozzle.
        double under = l.injectorFloor - Access.DECK - BEAM - .1;
        double yF = Math.floor(under - l.feedOd / 2 - .5) + .5, yS = Math.floor(under - l.steamOd / 2 - .5) + .5;
        double zF = -Math.sqrt(l.feedRing * l.feedRing - 1), zS = Math.sqrt(l.steamRing * l.steamRing - 1), zFront = -l.half - .3, zBack = l.half + .3;
        int nf = lineSides(l.feedOd), ns = lineSides(l.steamOd);
        pipe(l.feedOd / 2, nf, Material.PIPE, p(x + 1, l.feedY, zF), p(x + 1, yF, zF), p(x + 1, yF, zFront));
        pipe(l.steamOd / 2, ns, Material.PIPE, p(x + 1, l.steamY, zS), p(x + 1, yS, zS), p(x + 1, yS, zBack));
        u.solids.add(Solid.rod(p(x + 1, yF, 0), 2, zFront + O, zF + O, l.feedOd / 2, 0));
        u.solids.add(Solid.rod(p(x + 1, yS, 0), 2, zS + O, zBack + O, l.steamOd / 2, 0));
        u.solids.add(Solid.rod(p(x + 1, 0, zF), 1, yF, l.feedY, l.feedOd / 2, 0));
        u.solids.add(Solid.rod(p(x + 1, 0, zS), 1, yS, l.steamY, l.steamOd / 2, 0));
        u.nozzles.add(ProcessUnit.front(Role.FEED, x + 1 + O, yF, zFront + O, l.feedOd, l.feedOd));
        u.nozzles.add(along(Role.STEAM, p(x + 1, yS, zBack), 2, 1, l.steamOd, l.steamOd));
    }

    /** A ring header round the riser at {@code y}, radius {@code ring}, from angle a0 to a1 (from +x towards +z), its ends capped. */
    private void header(double ring, double y, double od, double a0, double a1) {
        int np = lineSides(od), segments = Math.max(8, (int) Math.ceil((a1 - a0) * ring / 1.5));
        arc(p(l.x, y, 0), PLUS_X, PLUS_Z, ring, a0, a1, segments, od / 2, np, Material.PIPE);
        for (int e = 0; e < 2; e++) {
            double t = e == 0 ? a0 : a1, c = DeterministicMath.cos(t), s = DeterministicMath.sin(t);
            var cap = new Frame(p(l.x + ring * c, y, ring * s), e == 0 ? new double[]{s, 0, -c} : new double[]{-s, 0, c}, UP);
            m.annulus(cap, 0, od / 2, 0, np, 1, Material.PIPE);
        }
        u.solids.add(Solid.ring(l.x + O, O, y - od / 2, y + od / 2, ring - od / 2, ring + od / 2, a0, a1));
    }

    // ---- Standpipes ---------------------------------------------------------------------------------------------------

    /**
     * The regenerated-catalyst standpipe straight from the regenerator's lower shell to the riser's wye, the spent-catalyst
     * standpipe from the stripper's bottom head down to its 45° lateral into the regenerator's bed; a slide valve on each.
     */
    private void standpipes() {
        double r = l.asp, d = l.dsp;
        int n = lineSides(d);
        // Regenerated catalyst: along (sin1, -cos1) from the regenerator's shell.
        double ux = l.sin1, uy = -l.cos1, inside = r + .15;
        double tValve = (l.rcsvX - l.r) / l.sin1, tEnd = (l.x - l.rri - l.r) / l.sin1 + .1;
        double[] start = {l.r - inside * ux, l.rcspTop - inside * uy}, valveTop = {l.r + (tValve - .8 * d) * ux, l.rcspTop + (tValve - .8 * d) * uy};
        double[] valveBottom = {l.r + (tValve + .8 * d) * ux, l.rcspTop + (tValve + .8 * d) * uy}, end = {l.r + tEnd * ux, l.rcspTop + tEnd * uy};
        tube(p(start[0], start[1], 0), p(valveTop[0], valveTop[1], 0), r, n, Material.PIPE);
        tube(p(valveBottom[0], valveBottom[1], 0), p(end[0], end[1], 0), r, n, Material.PIPE);
        slanted(start, valveTop, r);
        slanted(valveBottom, end, r);
        slideValve(l.rcsvX, l.rcsvY, ux, uy);
        // Spent catalyst: from inside the stripper down along -(sin2, cos2) to the elbow over the lateral.
        double vx = l.sin2, vy = l.cos2, tv = (l.scsvY - l.p1y) / l.cos2;
        double[] s0 = {l.s0x, l.stripperBottom + .2}, p1 = {l.p1x, l.p1y}, in = {l.r - .3 * Math.sqrt(.5), l.scspIn - .3 * Math.sqrt(.5)};
        double[] top = {l.p1x + (tv + .8 * d) * vx, l.p1y + (tv + .8 * d) * vy}, bottom = {l.p1x + (tv - .8 * d) * vx, l.p1y + (tv - .8 * d) * vy};
        tube(p(s0[0], s0[1], 0), p(top[0], top[1], 0), r, n, Material.PIPE);
        pipe(r, n, Material.PIPE, p(bottom[0], bottom[1], 0), p(p1[0], p1[1], 0), p(in[0], in[1], 0));
        slanted(s0, top, r);
        slanted(bottom, p1, r);
        slanted(p1, in, r);
        slideValve(l.scsvX, l.scsvY, vx, vy);
    }

    /**
     * A slide valve on a standpipe (local centre (x, y) on z = 0, the pipe along (ax, ay)): its body 1.6 standpipe
     * diameters along the pipe, 1.3 across and 2.2 along z with a flange at each end; the bonnet, the stem's tube and the
     * hydraulic actuator out towards the front ({@link FccConverterLayout#valveReach} from the pipe's axis), its
     * positioner box.
     */
    private void slideValve(double x, double y, double ax, double ay) {
        double d = l.dsp;
        double[] c = p(x, y, 0), along = {ax, ay, 0}, across = {-ay, ax, 0};
        m.block(c, scale(along, .8 * d), scale(across, .65 * d), new double[]{0, 0, 1.1 * d}, Material.MACHINE);
        int n = lineSides(d);
        for (int s = -1; s <= 1; s += 2) {
            var f = new Frame(plus(c, scale(along, s * .8 * d)), along, across);
            double flange = l.asp + .08 + .05 * d;
            m.prism(f, flange, -.05, .05, n, Material.FLANGE);
            m.annulus(f, l.asp, flange, s > 0 ? .05 : -.05, n, s, Material.FLANGE);
        }
        m.block(plus(c, new double[]{0, 0, -1.4 * d}), scale(along, .5 * d), scale(across, .42 * d), new double[]{0, 0, .3 * d}, Material.MACHINE);
        m.block(plus(c, new double[]{0, 0, -1.12 * d}), scale(along, .62 * d), scale(across, .52 * d), new double[]{0, 0, .03}, Material.FLANGE);
        var stem = new Frame(plus(c, new double[]{0, 0, -1.7 * d}), MINUS_Z, PLUS_X);
        m.prism(stem, .12 * d, 0, 1.4 * d, 8, Material.ACTUATOR);
        double reach = FccConverterLayout.valveReach(d), cylinder = reach - 3.1 * d, r = .2 * d;
        m.cylinder(new Frame(plus(c, new double[]{0, 0, -3.1 * d}), MINUS_Z, PLUS_X), r, 0, cylinder, 12, Material.ACTUATOR);
        for (double z : new double[]{0, cylinder - .05})
            m.cylinder(new Frame(plus(c, new double[]{0, 0, -3.1 * d - z}), MINUS_Z, PLUS_X), r + .04, 0, .05, 12, Material.ACTUATOR);
        m.block(plus(c, new double[]{0, r + .1, -3.1 * d - cylinder / 2}), new double[]{.12, 0, 0}, new double[]{0, .1, 0}, new double[]{0, 0, .15}, Material.ACTUATOR);
        double px = .8 * d * Math.abs(ax) + .65 * d * Math.abs(ay), py = .8 * d * Math.abs(ay) + .65 * d * Math.abs(ax);
        u.solids.add(Solid.box(p(x - px, y - py, -1.1 * d), p(x + px, y + py, 1.1 * d)));
        u.solids.add(Solid.box(p(x - .45 * d, y - .45 * d, -reach), p(x + .45 * d, y + .45 * d, -1.1 * d)));
    }

    /** A slanted pipe's collision in the xy plane at z = 0 (local {x, y} ends): boxes 0.6 m along it. */
    private void slanted(double[] a, double[] b, double r) {
        double dx = b[0] - a[0], dy = b[1] - a[1], length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-6) return;
        int pieces = Math.max(1, (int) Math.ceil(length / .6));
        double ex = r * Math.abs(dy) / length, ey = r * Math.abs(dx) / length;
        for (int k = 0; k < pieces; k++) {
            double x0 = a[0] + dx * k / pieces, y0 = a[1] + dy * k / pieces, x1 = a[0] + dx * (k + 1) / pieces, y1 = a[1] + dy * (k + 1) / pieces;
            u.solids.add(Solid.box(p(Math.min(x0, x1) - ex, Math.min(y0, y1) - ey, -r), p(Math.max(x0, x1) + ex, Math.max(y0, y1) + ey, r)));
        }
    }

    // ---- Structure and stair tower ------------------------------------------------------------------------------------

    /** The frame's floors, columns, beams, struts and bracing, and the stair tower that joins every floor. */
    private void structure() {
        double x = l.x, xf = l.xf, xe = l.xe, half = l.half, riser = l.rri + .05;
        var floors = new ArrayList<Floor>();
        floors.add(new Floor(l.injectorFloor, l.riserHole, xf, true, riser));
        if (!l.rcsvOnInjectorFloor) floors.add(new Floor(l.rcsvFloor, l.rcsvHole, l.rcsvEnd, true, l.rcsvHole == l.riserHole ? riser : 0));
        floors.add(new Floor(l.scsvFloor, l.scsvHole, xf, true, l.scsvHole == l.riserHole ? riser : 0));
        floors.add(new Floor(l.stripperFloor, l.rs + .15, xf, false, l.rs + .05));
        floors.add(new Floor(l.manwayFloor, l.rr + .15, xf, false, l.rr + .05));
        double shoulder = .25 / (l.rr / 2);
        floors.add(new Floor(l.topFloor, l.rr + .15, xf, false, l.rr * Math.sqrt(Math.max(0, 1 - shoulder * shoulder)) + .05));
        // The tower's flights: each gap between floors in equal flights of at most FLIGHT_RISE, lanes alternating. The tower
        // is as long as its longest flight and a landing at each end, from the frame's front corner.
        var flights = new ArrayList<Flight>();
        double walk = 0;
        for (int i = 0; i < floors.size(); i++) {
            var floor = floors.get(i);
            double gap = floor.surface() - walk;
            int count = (int) Math.ceil(gap / FLIGHT_RISE - 1e-9);
            for (int j = 1; j <= count; j++) {
                double level = j == count ? floor.level() : walk + gap * j / count - Access.DECK, base = flights.isEmpty() ? 0 : flights.getLast().level() + Access.DECK;
                double top = level + Access.DECK;
                int steps = (int) Math.ceil((top - base) / ProcessUnit.STAIR_RISE - 1e-9);
                flights.add(new Flight(base, top, level, flights.size() % 2 == 0, j == count ? i : -1, steps * ProcessUnit.STAIR_GOING));
            }
            walk = floor.surface();
        }
        double longest = 0;
        for (var f : flights) longest = Math.max(longest, f.length());
        double a0 = -half + LAND, b1 = a0 + longest, end = b1 + LAND;
        var gaps = new double[floors.size()][];
        // The floors, each open in its +x handrail where the tower's landing joins it.
        for (int i = 0; i < floors.size(); i++) {
            double g0 = 0, g1 = 0;
            for (var f : flights)
                if (f.floor() == i) {
                    g0 = f.laneA() ? a0 + f.length() + half : 0;
                    g1 = f.laneA() ? end + half : LAND;
                }
            gaps[i] = new double[]{g0 - half, g1 - half};
            floor(floors.get(i), g0, g1);
        }
        frame(floors, gaps[floors.size() - 1]);
        tower(flights, a0, b1, end);
    }

    /**
     * A floor: the +x deck the frame's full depth, the front and back decks from its -x end to the +x deck, the -x deck
     * between them where the floor is closed and that deck at least {@link ColumnStructure#MIN_DECK} wide (else a handrail
     * along both sides of the opening), the opening's corners filled in to its vessel; beams under the decks' edges.
     */
    private void floor(Floor f, double gap0, double gap1) {
        double x = l.x, h = f.hole(), half = l.half, xe = l.xe, from = f.from(), level = f.level();
        boolean closed = !f.open() && x - h - l.xf >= ColumnStructure.MIN_DECK;
        deck(new Access.Line(p(x + h, 0, -half), PLUS_Z, PLUS_X, 2 * half), level, xe - x - h, true, true, gap0, gap1);
        deck(new Access.Line(p(from, 0, -h), PLUS_X, MINUS_Z, x + h - from), level, half - h, true, false);
        deck(new Access.Line(p(from, 0, h), PLUS_X, PLUS_Z, x + h - from), level, half - h, true, false);
        if (closed) deck(new Access.Line(p(x - h, 0, -h), PLUS_Z, MINUS_X, 2 * h), level, x - h - l.xf, false, false);
        else for (int s = -1; s <= 1; s += 2) u.rails(new Access.Line(p(from, 0, s * (h + 1)), PLUS_X, new double[]{0, 0, -s}, x - from), level, 1, false, false);
        if (f.fill() > 0 && f.fill() < h)
            for (int q = 0; q < 4; q++) if (closed || q == 0 || q == 3) fill(level, h + Access.GAP, f.fill(), q);
        // Beams under the outer edges (on the -x face only under the front and back decks' ends: the standpipes pass its
        // middle) and under the opening's front and back edges.
        double y0 = level - BEAM, w = BEAM_WIDTH;
        beam(from, xe, y0, level, -half, -half + w);
        beam(from, xe, y0, level, half - w, half);
        beam(xe - w, xe, y0, level, -half + w, half - w);
        for (int s = -1; s <= 1; s += 2) beam(l.xf, l.xf + w, y0, level, s > 0 ? h + .05 : -half + w, s > 0 ? half - w : -h - .05);
        beam(from, xe - w, y0, level, -h - .05 - w, -h - .05);
        beam(from, xe - w, y0, level, h + .05, h + .05 + w);
        if (from < l.xf - 1e-9) {
            // Under the regenerator towards its table: a post at each outer corner, a beam across the decks' end.
            for (int s = -1; s <= 1; s += 2) {
                double z = s * (half - COLUMN / 2);
                u.box(p(from, 0, z - COLUMN / 2), p(from + COLUMN, y0, z + COLUMN / 2), Material.STEELWORK, true);
                beam(from, from + w, y0, level, s > 0 ? h + .05 : -half, s > 0 ? half : -h - .05);
            }
        }
    }

    /** A steel beam (local box), its top under a deck, collided with. */
    private void beam(double x0, double x1, double y0, double y1, double z0, double z1) {
        m.box(p(x0, y0, z0), p(x1, y1, z1), ALL_FACES & ~8, Material.STEELWORK);
        u.solids.add(Solid.box(p(x0, y0, z0), p(x1, y1, z1)));
    }

    /** The grating filling quadrant {@code q} (0: +x +z, then counter-clockwise) of a floor's opening (half size {@code h}) in to radius {@code r} round the riser's axis. */
    private void fill(double level, double h, double r, int q) {
        double t0 = q * Math.PI / 2, y1 = level + Access.DECK;
        double[] d0 = {DeterministicMath.cos(t0), DeterministicMath.sin(t0)}, d1 = {-d0[1], d0[0]};
        double cx = l.x;
        double[] corner = p(cx + h * (d0[0] + d1[0]), 0, h * (d0[1] + d1[1]));
        var triangles = new ArrayList<double[][]>();
        int steps = 4;
        double[][] arc = new double[steps + 1][];
        for (int k = 0; k <= steps; k++) {
            double t = t0 + Math.PI / 2 * k / steps;
            arc[k] = p(cx + r * DeterministicMath.cos(t), 0, r * DeterministicMath.sin(t));
        }
        triangles.add(new double[][]{arc[0], p(cx + h * d0[0], 0, h * d0[1]), corner});
        for (int k = 0; k < steps; k++) triangles.add(new double[][]{arc[k], corner, arc[k + 1]});
        triangles.add(new double[][]{arc[steps], corner, p(cx + h * d1[0], 0, h * d1[1])});
        for (var t : triangles) {
            for (double y : new double[]{level, y1}) {
                var polygon = new ArrayList<double[]>(3);
                for (var v : t) polygon.add(new double[]{v[0], y, v[2]});
                m.flat(polygon, y == level ? DOWN : UP, Material.GRATING);
            }
        }
        u.solids.add(Solid.ring(cx + O, O, level, y1, r, h * Math.sqrt(2), t0, t0 + Math.PI / 2));
    }

    /**
     * A grating deck lying on beams (no brackets): {@code width} out from {@code run}'s wall line, its walking surface
     * {@link Access#DECK} over {@code level}, a toe board and handrail along its outer edge but over the gaps, across its
     * closed ends; registered for the access gates, collided as drawn.
     */
    private void deck(Access.Line run, double level, double width, boolean closedStart, boolean closedEnd, double... gaps) {
        double y0 = level, y1 = level + Access.DECK, length = run.length(), g = Access.GAP, toe = width - 1 / 32.0;
        double[] out = run.out(0), along = run.along(0);
        m.flat(List.of(run.at(0, g, y1), run.at(length, g, y1), run.at(length, width, y1), run.at(0, width, y1)), UP, Material.GRATING);
        m.flat(List.of(run.at(0, g, y0), run.at(length, g, y0), run.at(length, width, y0), run.at(0, width, y0)), DOWN, Material.GRATING);
        m.flat(List.of(run.at(0, g, y0), run.at(length, g, y0), run.at(length, g, y1), run.at(0, g, y1)), neg(out), Material.GRATING);
        m.flat(List.of(run.at(0, width, y0), run.at(length, width, y0), run.at(length, width, y1), run.at(0, width, y1)), out, Material.GRATING);
        for (double s : new double[]{0, length})
            m.flat(List.of(run.at(s, g, y0), run.at(s, width, y0), run.at(s, width, y1), run.at(s, g, y1)), s == 0 ? neg(along) : along, Material.GRATING);
        double start = 0, t = y1 + Access.TOE;
        var pieces = new ArrayList<double[]>();
        for (int k = 0; k + 1 < gaps.length; k += 2) {
            if (gaps[k] > start + .05) pieces.add(new double[]{start, gaps[k]});
            start = Math.max(start, gaps[k + 1]);
        }
        if (length > start + .05) pieces.add(new double[]{start, length});
        for (var q : pieces) {
            m.flat(List.of(run.at(q[0], width, y0), run.at(q[1], width, y0), run.at(q[1], width, t), run.at(q[0], width, t)), out, Material.HANDRAIL);
            m.flat(List.of(run.at(q[0], toe, y1), run.at(q[1], toe, y1), run.at(q[1], toe, t), run.at(q[0], toe, t)), neg(out), Material.HANDRAIL);
        }
        u.rails(run, level, width, closedStart, closedEnd, gaps);
        u.decks.add(new ProcessUnit.Deck(run, level, width, closedStart, closedEnd));
        u.solids.add(ProcessUnit.span(run.at(0, g, level), run.at(length, width, y1)));
        u.segment(run.at(0, width, y1), run.at(length, width, y1));
    }

    /**
     * The frame: columns at its corners, at the riser's line along its front and back where it is longer than 9 m and at
     * the middle of its +x face where it is deeper than 14.4 m (the -x face stays open for the standpipes), fireproofed to the
     * first floor; struts on its front and back at most 8 m apart between floors more than 9 m apart; X-bracing in alternate
     * front and back bays over the spent-catalyst valve's floor; a post carrying the vapour line on the top floor.
     */
    private void frame(List<Floor> floors, double[] topGap) {
        double xf = l.xf, xe = l.xe, half = l.half, c = COLUMN / 2, zc = half - c;
        var xs = new ArrayList<Double>(List.of(xf + c));
        if (xe - xf > 9) xs.add((double) l.x);
        xs.add(xe - c);
        var columns = new ArrayList<double[]>();
        for (double x : xs) for (int s = -1; s <= 1; s += 2) columns.add(new double[]{x, s * zc});
        // (beyond the tower's far landing, which ends at most 6.7 m from the front)
        if (half > 7.2) columns.add(new double[]{xe - c, 0});
        double cased = floors.getFirst().level() - BEAM, top = floors.getLast().level();
        for (var col : columns) {
            u.leg(col[0] + O, col[1] + O, cased, FIREPROOF, PLUS_X);
            m.box(p(col[0] - c, cased, col[1] - c), p(col[0] + c, top, col[1] + c), ALL_FACES & ~4, Material.STEELWORK);
            u.solids.add(Solid.box(p(col[0] - c, cased, col[1] - c), p(col[0] + c, top, col[1] + c)));
        }
        // Struts, and the levels the bracing runs between (each with its beam's depth).
        var levels = new ArrayList<double[]>();
        for (int i = 0; i < floors.size(); i++) {
            double level = floors.get(i).level();
            if (floors.get(i).surface() >= l.scsvFloor - 1e-9) levels.add(new double[]{level, BEAM});
            if (i + 1 == floors.size()) break;
            double next = floors.get(i + 1).level(), gap = next - level;
            int struts = gap > 9 ? (int) Math.ceil(gap / 8 - 1e-9) - 1 : 0;
            for (int k = 1; k <= struts; k++) {
                double y = level + gap * k / (struts + 1), y0 = y - STRUT, w = .2;
                beam(xf, xe, y0, y, -half, -half + w);
                beam(xf, xe, y0, y, half - w, half);
                if (floors.get(i).surface() >= l.scsvFloor - 1e-9) levels.add(new double[]{y, STRUT});
            }
        }
        for (int k = 0; k + 1 < levels.size(); k++)
            for (int j = 0; j + 1 < xs.size(); j++)
                for (int s = -1; s <= 1; s += 2)
                    if ((j + k) % 2 == 0) u.brace(new double[]{xs.get(j) + O, s * zc + O}, new double[]{xs.get(j + 1) + O, s * zc + O}, levels.get(k)[0], levels.get(k + 1)[0] - levels.get(k + 1)[1]);
        // The vapour line's support post on the top floor's +x beam, unless the tower's landing joins the floor there.
        double yV = vapourAxis();
        if (topGap[0] > .3 || topGap[1] < -.3) u.box(p(xe - .3, top, -.15), p(xe, yV - l.vapour / 2, .15), Material.STEELWORK, true);
    }

    /**
     * The stair tower on the frame's +x face, from its front corner to {@code end}: lane A next to the frame rising +z from
     * {@code a0}, lane B beyond it rising -z from {@code b1}; after each flight a landing at that end, across both lanes where
     * the flight is the longest, else a piece in each lane (the arriving lane's from where its flight ends, the other's from
     * where the next flight starts), open to the frame where a floor joins; posts at its outer corners.
     */
    private void tower(List<Flight> flights, double a0, double b1, double end) {
        double xe = l.xe, half = l.half, g = Access.GAP;
        for (int f = 0; f < flights.size(); f++) {
            var fl = flights.get(f);
            boolean last = f + 1 == flights.size(), joins = fl.floor() >= 0;
            double level = fl.level();
            if (fl.laneA()) {
                double length = u.stair(xe + LANE / 2 + O, a0 + O, 2, 1, FLIGHT, fl.base(), fl.top()), ia = a0 + length;
                if (!last && ia > b1 - 1e-9) deck(new Access.Line(p(xe, 0, b1 - g), PLUS_X, PLUS_Z, 2 * LANE), level, end - b1 + g, !joins, true);
                else {
                    deck(new Access.Line(p(xe, 0, ia - g), PLUS_X, PLUS_Z, LANE), level, end - ia + g, !joins, last);
                    if (!last) deck(new Access.Line(p(xe + LANE, 0, b1 - g), PLUS_X, PLUS_Z, LANE), level, end - b1 + g, false, true);
                }
            } else {
                double length = u.stair(xe + 1.5 * LANE + O, b1 + O, 2, -1, FLIGHT, fl.base(), fl.top()), ib = b1 - length;
                if (ib < a0 + 1e-9) deck(new Access.Line(p(xe, 0, a0 + g), PLUS_X, MINUS_Z, 2 * LANE), level, a0 + g + half, !joins, true);
                else {
                    deck(new Access.Line(p(xe + LANE, 0, ib + g), PLUS_X, MINUS_Z, LANE), level, ib + g + half, false, true);
                    deck(new Access.Line(p(xe, 0, a0 + g), PLUS_X, MINUS_Z, LANE), level, a0 + g + half, !joins, false);
                }
                if (last) u.rails(new Access.Line(p(xe, 0, a0 + 1), PLUS_X, MINUS_Z, LANE), level, 1, false, false);
            }
        }
        // Each post up to its end's highest landing (lane A's flights land at the back end, lane B's at the front).
        double front = -1, back = -1, x = xe + 2 * LANE;
        for (var fl : flights)
            if (fl.laneA()) back = fl.level();
            else front = fl.level();
        double[][] posts = {{-half + .075, front}, {end - .075, back}};
        for (double[] post : posts)
            if (post[1] >= 0) u.box(p(x, 0, post[0] - .075), p(x + .15, post[1] + Access.DECK + Access.RAIL + .1, post[0] + .075), Material.STEELWORK, true);
    }

    // ---- Flue gas and vapour lines ------------------------------------------------------------------------------------

    /** The flue gas line out of the regenerator's crown and over to its nozzle beyond the shell at -x; the vapour line out of the reactor's crown over the stair tower to its nozzle at +x. */
    private void lines() {
        // Flue gas.
        double rf = l.flue / 2, bend = l.flue, crown = l.support + l.straight + l.head, y = axis(crown, bend);
        int nf = lineSides(l.flue);
        tube(p(0, crown - .3, 0), p(0, y - bend, 0), rf, nf, Material.PIPE);
        var turn = elbow(p(0, y - bend, 0), UP, MINUS_X, bend, rf, nf, Material.PIPE);
        double xEnd = -(l.r + .3);
        tube(turn, p(xEnd, y, 0), rf, nf, Material.PIPE);
        u.solids.add(Solid.rod(p(0, 0, 0), 1, crown, y - bend, rf, 0));
        u.solids.add(Solid.rod(p(0, y, 0), 0, xEnd + O, -bend + O, rf, 0));
        u.round(p(0, y, 0), 0, xEnd + O, -bend + O, rf);
        var end = p(xEnd, y, 0);
        if (l.flue > 1) end = reducer(end, MINUS_X, rf, .5, nf, Material.PIPE);
        double flueOd = l.flue > 1 ? 1 : ColumnStructure.size(l.flue, 1);
        u.nozzles.add(along(Role.FLUE_GAS, end, 0, -1, flueOd, flueOd));
        // Vapour.
        double rv = l.vapour / 2, bv = 1.5 * l.vapour, top = l.reactorTop + l.rr / 2, yv = vapourAxis(), x = l.x;
        int nv = lineSides(l.vapour);
        tube(p(x, top - .3, 0), p(x, yv - bv, 0), rv, nv, Material.CLADDING);
        var over = elbow(p(x, yv - bv, 0), UP, PLUS_X, bv, rv, nv, Material.CLADDING);
        double xv = Math.max(x + bv + .5, l.xe + 2 * LANE + .5);
        tube(over, p(xv, yv, 0), rv, nv, Material.CLADDING);
        u.solids.add(Solid.rod(p(x, 0, 0), 1, top, yv - bv, rv, 0));
        u.solids.add(Solid.rod(p(0, yv, 0), 0, x + bv + O, xv + O, rv, 0));
        u.round(p(0, yv, 0), 0, x + bv + O, xv + O, rv);
        var out = p(xv, yv, 0);
        if (l.vapour > 1) out = reducer(out, PLUS_X, rv, .5, nv, Material.CLADDING);
        double vapourOd = l.vapour > 1 ? 1 : ColumnStructure.size(l.vapour, 1);
        u.nozzles.add(along(Role.VAPOUR_OUTLET, out, 0, 1, vapourOd, vapourOd));
    }

    /** The vapour line's horizontal axis: a cell centre at least its elbow and 0.3 m over the reactor's crown. */
    private double vapourAxis() {
        return axis(l.reactorTop + l.rr / 2, 1.5 * l.vapour);
    }

    /** A horizontal line's axis over a crown at {@code crown} through an elbow of {@code bend}: the cell centre at least 0.3 m higher than the elbow needs. */
    private static double axis(double crown, double bend) {
        return Math.ceil(crown + .3 + bend - .5 - 1e-9) + .5;
    }

    // ---- Pipe parts ---------------------------------------------------------------------------------------------------

    /** A straight round tube from {@code a} to {@code b} (frame points). */
    private void tube(double[] a, double[] b, double r, int n, Material material) {
        var along = sub(b, a);
        double length = Math.sqrt(dot(along, along));
        if (length < 1e-6) return;
        var dir = scale(along, 1 / length);
        var ref = Math.abs(dir[1]) < .9 ? UP : PLUS_X;
        m.prism(new Frame(a, dir, unit(cross(dir, ref))), r, 0, length, n, material);
    }

    /** A pipe along {@code points} (frame points) with a ball at each bend. */
    private void pipe(double r, int n, Material material, double[]... points) {
        for (int k = 0; k + 1 < points.length; k++) {
            tube(points[k], points[k + 1], r, n, material);
            if (k > 0) m.sphere(points[k], r * 1.03, n, material);
        }
    }

    /** An elbow of bend radius {@code bend} from {@code a} (frame point) heading {@code d0}, turning to {@code d1} (unit, square to it): returns its end. */
    private double[] elbow(double[] a, double[] d0, double[] d1, double bend, double r, int n, Material material) {
        arc(plus(a, scale(d1, bend)), neg(d1), d0, bend, 0, Math.PI / 2, Math.max(3, (int) Math.ceil(bend * Math.PI / 2 / .9)), r, n, material);
        return plus(a, scale(plus(d0, d1), bend));
    }

    /**
     * A bent pipe of {@code r} along the arc of radius {@code bend} round {@code centre} from angle a0 to a1 (from {@code ref}
     * towards {@code side}), in {@code segments} straight mitred pieces: each runs on past its bend by r tan(half the turn), so
     * the outside of the bend closes and the inside laps into the next piece. A segmented elbow or ring header costs half a
     * torus's quads (its facets are quads, not triangles) and keeps its outline.
     */
    private void arc(double[] centre, double[] ref, double[] side, double bend, double a0, double a1, int segments, double r, int n, Material material) {
        double step = (a1 - a0) / segments, lap = r * DeterministicMath.sin(step / 2) / DeterministicMath.cos(step / 2);
        boolean closed = a1 - a0 > 2 * Math.PI - 1e-9;
        for (int k = 0; k < segments; k++) {
            double t0 = a0 + k * step, t1 = t0 + step;
            var p0 = plus(centre, scale(plus(scale(ref, DeterministicMath.cos(t0)), scale(side, DeterministicMath.sin(t0))), bend));
            var p1 = plus(centre, scale(plus(scale(ref, DeterministicMath.cos(t1)), scale(side, DeterministicMath.sin(t1))), bend));
            var dir = unit(sub(p1, p0));
            tube(k == 0 && !closed ? p0 : plus(p0, scale(dir, -lap)), k + 1 == segments && !closed ? p1 : plus(p1, scale(dir, lap)), r, n, material);
        }
    }

    /** A 15° concentric reducer from radius r0 at {@code a} (frame point) along {@code dir} down to r1: returns its small end. */
    private double[] reducer(double[] a, double[] dir, double r0, double r1, int n, Material material) {
        double length = (r0 - r1) / FccConverterLayout.TAN15;
        var ref = Math.abs(dir[1]) < .9 ? UP : PLUS_X;
        m.frustum(new Frame(a, dir, unit(cross(dir, ref))), r0, r1, 0, length, n, material);
        return plus(a, scale(dir, length));
    }

    // ---- Nozzles ------------------------------------------------------------------------------------------------------

    /** A nozzle at the end of a line ({@code base}, a frame point) along frame axis {@code axis} towards {@code sign}: its face on the next block face {@link EquipmentShape#STAND_OUT} beyond. */
    private static Nozzle along(Role role, double[] base, int axis, int sign, double maxOd, double defaultOd) {
        double at = base[axis], plane = sign > 0 ? Math.ceil(at + .03 + EquipmentShape.STAND_OUT) : Math.floor(at - .03 - EquipmentShape.STAND_OUT);
        return new Nozzle(role, base, axis, sign, Math.abs(plane - at), maxOd, defaultOd, 0, .03);
    }

    /** A nozzle on the back (frame +z) of a standing body (its axis at (x, z), radius {@code r}, {@code n} sides) at height {@code y}: {@link ProcessUnit#radialSide} turned round. */
    private static Nozzle radialBack(Role role, double x, double y, double z, double r, int n, double maxOd, double defaultOd) {
        double apothem = r * DeterministicMath.cos(Math.PI / n), start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double plane = Math.ceil(z + r + EquipmentShape.STAND_OUT);
        return new Nozzle(role, new double[]{x, y, z + start}, 2, 1, plane - z - start, maxOd, defaultOd, 0, r - start);
    }

    // ---- Frame helpers ------------------------------------------------------------------------------------------------

    /** A local point (the layout's frame) in the unit's frame: x and z shifted half a block. */
    private static double[] p(double x, double y, double z) {
        return new double[]{x + O, y, z + O};
    }

    /** A line's sides: 8 up to 0.6 m, then about one every 0.7 m round a large line (its outline at a block's scale). */
    static int lineSides(double od) {
        return od <= .35 ? 6 : od <= .6 ? 8 : Math.max(12, 4 * (int) Math.ceil(.5 * od + 1.5));
    }

    /** A vessel's sides: at least 24, about one every 0.8 m round a large one. */
    static int vesselSides(double od) {
        return Math.max(24, 4 * (int) Math.ceil(.45 * od + 2));
    }
}
