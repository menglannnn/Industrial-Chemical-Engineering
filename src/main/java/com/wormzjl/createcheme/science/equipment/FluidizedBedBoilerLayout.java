package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Nozzle;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.createcheme.science.equipment.Mesh.Frame;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.equipment.ProcessUnit.Solid;

import java.util.ArrayList;
import java.util.List;

import static com.wormzjl.createcheme.science.equipment.FluidizedBedParts.box;
import static com.wormzjl.createcheme.science.equipment.FluidizedBedParts.closedBox;
import static com.wormzjl.createcheme.science.equipment.FluidizedBedParts.trim;

/**
 * A circulating fluidized-bed boiler's parts ({@link FluidizedBedBoilerDesign}; {@code NEW_UNITS_SURVEY_SOLID_FUEL.md}
 * section 1), in the unit's frame: the front (-z) towards the constructor, +x its left.
 *
 * <p>From the front: the fuel day bins (one per 5 m of furnace width) on their legs, a drag-chain feeder under each
 * driven at its front end, the chutes down into the front wall 2.5 m over the grid. The furnace: membrane water walls
 * behind profiled cladding, W wide and 0.65 W deep, its lower 15 % tapered to a grid 0.6 deep (refractory-lined), the
 * windbox under the grid, buckstays round it at every landing of the stair tower (3.6 m); two start-up burners on its
 * left wall, the secondary-air ring 1 to 1.7 m over the grid. Behind it the hot cyclones in a row on the rear wall
 * (Basu: barrel 1.25 dc, 2.75 dc overall, the inlet duct 0.9 dc high and 0.3 dc wide from the upper rear wall), each
 * standpipe (0.2 dc) down to its loop seal and the return leg into the rear wall 3 m over the grid; a crossover duct from
 * each cyclone's roof outlet into the back pass's top. The back pass (W wide, 0.45 D deep, as high as the furnace), the
 * tubular air heater a quarter of the furnace's height under it on legs, its fly-ash hoppers; the hot primary air into
 * the windbox and the secondary air into the ring under the cyclones' loop. Behind the air heater the PA, ID and SA fans
 * at grade, the flue gas to a drawn stack (its top 10 m over the roof, 60 m at most). Under the grid the bed-ash drains to
 * rotary drum coolers turning slowly, their ash on a drag conveyor rising to its head beside the furnace.
 *
 * <p>The steel: columns on concrete pedestals at the frame's corners (the front line, behind the furnace and behind the
 * cyclones) and between them across the front, side girders and cross girders over the roof (the boiler hangs from them;
 * survey: 2 to 4 m deep), X-bracing in the side bays between the decks; on top the steam drum (W + 2 m long) on its
 * beams, riser bundles from the roof header, the downcomer down the left rear corner, safety valves with tall vent
 * silencers. Decks ring the furnace's front and sides at the feeder floor, every fourth landing above it and the highest,
 * reached by a stair tower on its right (flights of 3.6 m above the feeder floor; OSHA: 3.7); where a deck is over the
 * air heater its right side runs on past the cyclones to a deck along the back pass (no caged ladders, survey).
 */
final class FluidizedBedBoilerLayout {
    /** The water wall and its cladding, m; a deck's width; a stair flight's width and a landing's depth. */
    static final double WALL = .25, DECK = 1.2, STAIR = 1.1, LANDING = 1.2;

    final ProcessUnit u;
    final int n, chutes, top;
    final double w, d, h, g, yt, hx, s, ht, hw, dc, tc, rc, wi, pitch, hc, colOff, feeder, hf, e;
    final double binWidth, binAt, a, zF, zR, zc, zb0, zb1, ab, hxb, r1, r1id, px, yah0, yah1, zdr, dg, yd, rd;
    final double[] cx;
    /** The bed-ash conveyor's line along z, set when the coolers are drawn. */
    double ashZ;

    FluidizedBedBoilerLayout(ProcessUnit unit) {
        u = unit;
        w = u.shape.diameter();
        n = (int) u.shape.tangentLength();
        g = u.shape.support();
        d = FluidizedBedBoilerDesign.depth(w);
        h = FluidizedBedBoilerDesign.height(w);
        yt = g + h;
        hx = w / 2 + WALL;
        s = .2 * d;
        ht = .15 * h;
        hw = Math.max(2, Math.min(3, g - 3));
        dc = FluidizedBedBoilerDesign.cyclone(w, n);
        tc = .3 + .015 * dc;
        rc = dc / 2 + tc;
        wi = .3 * dc + 2 * tc;
        pitch = 2 * rc + .6;
        hc = Math.max(.45, Math.min(.8, .4 + .015 * w));
        colOff = hx + DECK + hc / 2;
        // The stair tower: four equal flights to the feeder floor 4.5 m over the grid, flights of HIGH above it; the
        // highest deck 1.2 m under the roof at least.
        feeder = g + 4.5;
        hf = feeder / 4;
        int k = 4;
        while (landing(k + 2) <= yt - 1.2) k += 2;
        top = k;
        e = hx + DECK + hc + .1;
        // The day bins: one per 5 m of width (survey: a chute per 3 to 5 m; the upper end keeps the quads), 1.2 m apart at least.
        chutes = Math.max(1, Math.min(6, (int) Math.round(w / 5)));
        double pb = w / chutes;
        binWidth = Math.min(pb - 1.2, Math.max(1.6, Math.min(3.2, .12 * w + 1.4)));
        binAt = Math.ceil(DECK + hc + .2 + binWidth / 2 - .5) + .5;
        double front = Math.max(binAt + .45 + FluidizedBedParts.DRIVE_LENGTH, binAt + binWidth / 2 + .15) + .05;
        zF = Math.ceil(front - 1e-9);
        zR = zF + d + 2 * WALL;
        zc = zR + .6 + .1 * dc + rc;
        zb0 = zc + rc + 1 + hc;
        zb1 = zb0 + Math.max(1.5, .45 * d) + 2 * WALL;
        // Fans: PA and SA scrolls 3 to 6 m high (survey), the ID fan larger; the three side by side behind the air heater.
        r1 = Math.max(1.2, Math.min(2.8, .9 + .08 * w));
        r1id = Math.max(1.3, Math.min(3.2, 1 + .09 * w));
        px = r1id + r1 + 1.6;
        // The cyclones' row: their inlet ducts (on each barrel's right, tangential) centred on the furnace.
        double span = (n - 1) * pitch + wi, first = -span / 2 + rc, last = first + (n - 1) * pitch;
        double shift = Math.rint((first + last) / 2), wx = .5 * dc + .3;
        double half = hx;
        for (int i = 0; i < n; i++) half = Math.max(half, Math.abs(first + i * pitch - shift) + wx / 2 + .25);
        hxb = half;
        double left = Math.max(e + 2 * STAIR + .3, px + r1 + .8 - shift);
        a = Math.ceil(left - .5) + .5;
        ab = a + shift;
        cx = new double[n];
        for (int i = 0; i < n; i++) cx[i] = a + first + i * pitch;
        yah0 = g - hw;
        yah1 = yah0 + .25 * h;
        // The drum over the roof's rear on a cell centre (its blowdown's line), above the roof steel.
        dg = Math.max(2, Math.min(3.5, 1.5 + .06 * w));
        rd = Math.max(.6, Math.min(.9, .6 + .012 * w));
        zdr = Math.floor(zR - .5) + .5;
        yd = Math.ceil(yt + .5 + dg + .7 + rd - .5) + .5;
    }

    void draw() {
        furnace();
        burners();
        cyclones();
        backPass();
        fans();
        bins();
        coolers();
        steel();
        roof();
        decks();
        tower();
        nozzles();
    }

    // ---- Furnace ------------------------------------------------------------------------------------------------------

    /** The front wall's outer face at height y: inset by the lower furnace's taper under {@code g + ht}. */
    double front(double y) {
        return zF + s * Math.max(0, 1 - (y - g) / ht);
    }

    double rear(double y) {
        return zR - s * Math.max(0, 1 - (y - g) / ht);
    }

    /**
     * The furnace: the clad box over the taper, the taper's sloping front and rear walls and its side walls, the roof;
     * the windbox under the grid; the lower headers along the side walls' foot; corner trims; buckstays.
     */
    void furnace() {
        double x0 = a - hx, x1 = a + hx, y1 = g + ht, f0 = zF + s, r0 = zR - s;
        var m = u.mesh;
        m.box(new double[]{x0, y1, zF}, new double[]{x1, yt, zR}, Mesh.ALL_FACES & ~4, Material.CLADDING);
        m.flat(List.of(new double[]{x0, g, f0}, new double[]{x1, g, f0}, new double[]{x1, y1, zF}, new double[]{x0, y1, zF}), new double[]{0, -s, -ht}, Material.CLADDING);
        m.flat(List.of(new double[]{x0, g, r0}, new double[]{x1, g, r0}, new double[]{x1, y1, zR}, new double[]{x0, y1, zR}), new double[]{0, -s, ht}, Material.CLADDING);
        for (double x : new double[]{x0, x1})
            m.flat(List.of(new double[]{x, g, f0}, new double[]{x, g, r0}, new double[]{x, y1, zR}, new double[]{x, y1, zF}), new double[]{x - a, 0, 0}, Material.CLADDING);
        u.solids.add(Solid.box(new double[]{x0, y1, zF}, new double[]{x1, yt, zR}));
        u.volumes.add(Solid.box(new double[]{x0 + .1, y1 - .2, zF + .1}, new double[]{x1 - .1, yt - .1, zR - .1}));
        // The taper in steps of half a metre or less, each to its walls at its top: a sliver of a step outside a sloping
        // wall is under 0.4 m thick, so the cells it reaches hold the wall.
        int bands = (int) Math.ceil(2 * ht);
        for (int k = 0; k < bands; k++) {
            double b0 = g + k * ht / bands, b1 = b0 + ht / bands;
            u.solids.add(Solid.box(new double[]{x0, b0, front(b1)}, new double[]{x1, b1, rear(b1)}));
            u.volumes.add(Solid.box(new double[]{x0 + .1, b0, front(b1) + .1}, new double[]{x1 - .1, b1, rear(b1) - .1}));
        }
        // Corner trims up the box.
        for (double x : new double[]{x0 - .05, x1 - .05})
            for (double z : new double[]{zF - .05, zR - .05})
                u.mesh.box(new double[]{x, y1, z}, new double[]{x + .1, yt, z + .1}, (x < a ? 1 : 2) | (z < zF ? 16 : 32) | 8, Material.CLADDING);
        for (double[] p : new double[][]{{x0, zF, x1, zF}, {x1, zF, x1, zR}, {x1, zR, x0, zR}, {x0, zR, x0, zF}})
            u.segment(new double[]{p[0], yt, p[1]}, new double[]{p[2], yt, p[3]});
        // The windbox under the grid, the primary air coming in at its back.
        // Its top is the grid, inside the furnace: not drawn.
        u.mesh.box(new double[]{x0, g - hw, f0}, new double[]{x1, g, r0}, Mesh.ALL_FACES & ~8, Material.CLADDING);
        u.solids.add(Solid.box(new double[]{x0, g - hw, f0}, new double[]{x1, g, r0}));
        u.volumes.add(Solid.box(new double[]{x0 + .1, g - hw + .1, f0 + .1}, new double[]{x1 - .1, g - .1, r0 - .1}));
        FluidizedBedParts.band(u, x0 - .08, g - .25, f0 - .08, x1 + .08, g, r0 + .08, Material.STEELWORK);
        // The lower headers along the side walls' foot.
        double rh = .15 + .004 * w;
        for (double x : new double[]{x0 - rh, x1 + rh}) {
            u.mesh.cylinder(Frame.of(new double[]{x, g + rh, 0}, 2, 1), rh, f0 + .1, r0 - .1, 12, Material.PIPE);
            u.solids.add(Solid.rod(new double[]{x, g + rh, 0}, 2, f0 + .1, r0 - .1, rh, 0));
        }
        buckstays();
        secondaryAir();
    }

    /** A flight's rise above the feeder floor, m: 18 rises of 0.2 m (OSHA 1910.25: at most 3.7 m between landings). */
    static final double HIGH = 3.6;

    /** The landings' heights, deck tops over grade: {@code j hf} up to the feeder floor, then every {@link #HIGH}. */
    double landing(int j) {
        return j <= 4 ? j * hf : feeder + (j - 4) * HIGH;
    }

    /** The length of the flight from landing {@code j}: its treads' goings. */
    double flight(int j) {
        return Math.ceil((landing(j + 1) - landing(j)) / ProcessUnit.STAIR_RISE - 1e-9) * ProcessUnit.STAIR_GOING;
    }

    /** Whether the furnace has decks at landing {@code j}: the feeder floor, every fourth landing above it, and the highest. */
    boolean decked(int j) {
        return j >= 4 && j % 2 == 0 && ((j - 4) % 4 == 0 && j <= top - 4 || j == top);
    }

    /**
     * Buckstays round the furnace under every landing's height above the taper (survey: every 3 to 4 m, a signature): an
     * I-beam 0.45 m deep standing 0.3 m off the wall, the deck at that level resting on it; round the rear wall's inlet
     * ducts. Their undersides and the faces against the wall or inside a neighbour are not drawn (2 500 quads at W 26).
     */
    void buckstays() {
        double x0 = a - hx, x1 = a + hx, out = .3, depth = .45;
        int ends = 8, along = ends | 16 | 32;
        for (int j = 1; landing(j) - Access.DECK < yt - .4; j++) {
            double top = landing(j) - Access.DECK, low = top - depth;
            if (low < g + ht + .2) continue;
            // Faces against the wall or inside a neighbour are not drawn.
            faced(new double[]{x0 - out, low, zF - out}, new double[]{x0, top, zR + out}, along | 1);
            faced(new double[]{x1, low, zF - out}, new double[]{x1 + out, top, zR + out}, along | 2);
            faced(new double[]{x0, low, zF - out}, new double[]{x1, top, zF}, ends | 16);
            // The rear wall's between the cyclones' inlet ducts where they are at that height.
            double from = x0;
            for (double c : cx) {
                double d0 = c - rc - .1, d1 = c - rc + wi + .1;
                if (top > inletLow() - .05 && low < yt - .25) {
                    if (d0 > from + .1) faced(new double[]{from, low, zR}, new double[]{d0, top, zR + out}, ends | 32 | 1 | 2);
                    from = Math.max(from, d1);
                }
            }
            if (x1 > from + .1) faced(new double[]{from, low, zR}, new double[]{x1, top, zR + out}, ends | 32 | 1 | 2);
        }
    }

    /** A steel box drawn with only the faces in {@code mask} (the others against a wall or inside a neighbour); it collides. */
    void faced(double[] lo, double[] hi, int mask) {
        u.mesh.box(lo, hi, mask, Material.STEELWORK);
        u.solids.add(Solid.box(lo, hi));
    }

    /** The cyclones' inlet ducts' underside. */
    double inletLow() {
        return yt - .25 - .9 * dc;
    }

    /** The secondary-air ring 1 to 1.7 m over the grid round all four walls (survey: SA manifolds 1.5 to 5 m over the grid). */
    void secondaryAir() {
        double y0 = g + .9, y1 = g + 1.7, f = front(y1), r = rear(y1), o = .7, x0 = a - hx, x1 = a + hx;
        box(u, x0 - o, y0, f - o, x1 + o, y1, f, Material.CLADDING);
        box(u, x0 - o, y0, r, x1 + o, y1, r + o, Material.CLADDING);
        box(u, x0 - o, y0, f, x0, y1, r, Material.CLADDING);
        box(u, x1, y0, f, x1 + o, y1, r, Material.CLADDING);
    }

    /**
     * Two start-up burners on the furnace's left wall (+x) 3 m over the grid (survey: two to four at the windbox and on
     * the side walls): an air register drum, its oil or gas line from a header along the wall at 2.5 m that runs to the
     * front, where FUEL comes in.
     */
    void burners() {
        double x = a + hx, yb = g + 3, rb = .3 + .008 * w, header = x + .9, yh = g + 2.5, pipe = .05;
        for (double t : new double[]{.3, .7}) {
            double z = zF + WALL + s + t * (d - 2 * s);
            var f = Frame.of(new double[]{0, yb, z}, 0, 1);
            u.mesh.cylinder(f, rb, x, x + .55, 12, Material.MACHINE);
            FluidizedBedParts.hoop(u, f, rb, rb + .04, x + .2, x + .3, 12, Material.FLANGE);
            u.mesh.cylinder(f, .08, x + .55, x + .75, 8, Material.PIPE);
            u.solids.add(Solid.rod(new double[]{0, yb, z}, 0, x, x + .75, rb + .04, 0));
            u.line(pipe, Material.PIPE, new double[]{x + .75, yb, z}, new double[]{header, yb, z}, new double[]{header, yh, z});
        }
        double z1 = zF + WALL + s + .7 * (d - 2 * s);
        u.line(pipe, Material.PIPE, new double[]{header, yh, z1}, new double[]{header, yh, zF - .2}, new double[]{fuelX(), yh, zF - .2});
    }

    /** The fuel nozzle's x in front of the furnace: the cell centre a quarter of a metre either side of the left wall's plane, clear of the corner column. */
    double fuelX() {
        return Math.floor(a + hx + .2) + .5;
    }

    // ---- The solids loop ----------------------------------------------------------------------------------------------

    /**
     * Each hot cyclone ({@link FluidizedBedParts#cyclone}, its roof level with the furnace's) with its tangential inlet
     * duct from the upper rear wall, its standpipe down to the loop seal (a box 2.5 standpipes long, 1.25 wide and 2
     * high, Basu) and the return leg sloping down at 45° into the rear wall 3 m over the grid (survey: 2 to 4 m); the
     * crossover duct from its roof outlet into the back pass's top.
     */
    void cyclones() {
        double iTop = yt - .25, iLow = inletLow(), stub = .8, wx = .5 * dc + .3, hxd = .45 * dc + .3, dsp = .2 * dc, rl = .5 * dsp + .12, entry = g + 3;
        double legStart = zc - 2 * dsp, run = legStart - rear(entry), yTop = entry + run + rl + .15, yLow = yTop - 2 * dsp - .3, zx1 = (zb0 + zb1) / 2 + wx / 2;
        for (double x : cx) {
            FluidizedBedParts.cyclone(u, x, zc, yt, dc, tc, stub, yTop - .05, 24);
            u.mesh.box(new double[]{x - rc, iLow, zR}, new double[]{x - rc + wi, iTop, zc}, Mesh.ALL_FACES & ~(16 | 32), Material.CLADDING);
            u.solids.add(Solid.box(new double[]{x - rc, iLow, zR}, new double[]{x - rc + wi, iTop, zc - .4 * rc}));
            u.mesh.box(new double[]{x - rc - .06, iLow - .06, zR + .3}, new double[]{x - rc + wi + .06, iTop + .06, zR + .5}, 1 | 2 | 4 | 8, Material.CLADDING);
            // The loop seal and its return leg.
            closedBox(u, new double[]{x - .625 * dsp - .15, yLow, legStart - .15}, new double[]{x + .625 * dsp + .15, yTop, zc + .5 * dsp + .15}, Material.CLADDING);
            u.line(rl, Material.CLADDING, new double[]{x, entry + run, legStart}, new double[]{x, entry, rear(entry) + .05});
            FluidizedBedParts.hoop(u, Frame.of(new double[]{x, 0, zc}, 1, 1), .1 * dc + .15, .1 * dc + .25, yTop + .3, yTop + .5, 12, Material.FLANGE);
            // The crossover over the back pass and its drop into its roof.
            closedBox(u, new double[]{x - wx / 2, yt + stub, zc - wx / 2}, new double[]{x + wx / 2, yt + stub + hxd, zx1}, Material.CLADDING);
            box(u, x - wx / 2, yt, zx1 - wx, x + wx / 2, yt + stub, zx1, Material.CLADDING);
        }
    }

    // ---- Back pass, air heater, fans, stack -----------------------------------------------------------------------------

    /**
     * The back pass (a clad box as high as the furnace), the tubular air heater under it a quarter of the furnace's height
     * on legs, 2 to 4 pyramidal fly-ash hoppers with their rotary valves; the hot primary air duct into the windbox's
     * back and the secondary air duct into the ring's back, under the loop seals.
     */
    void backPass() {
        double x0 = ab - hxb, x1 = ab + hxb, o = .15;
        // The back pass stands on the air heater: the faces between them are not drawn, the insides join.
        u.mesh.box(new double[]{x0, yah1, zb0}, new double[]{x1, yt, zb1}, Mesh.ALL_FACES & ~4, Material.CLADDING);
        u.solids.add(Solid.box(new double[]{x0, yah1, zb0}, new double[]{x1, yt, zb1}));
        u.volumes.add(Solid.box(new double[]{x0 + .1, yah1 - .3, zb0 + .1}, new double[]{x1 - .1, yt - .1, zb1 - .1}));
        for (double x : new double[]{x0 - .05, x1 - .05})
            for (double z : new double[]{zb0 - .05, zb1 - .05})
                u.mesh.box(new double[]{x, yah1, z}, new double[]{x + .1, yt, z + .1}, (x < ab ? 1 : 2) | (z < zb0 ? 16 : 32) | 8, Material.CLADDING);
        for (double[] p : new double[][]{{x0, zb0, x1, zb0}, {x1, zb0, x1, zb1}, {x1, zb1, x0, zb1}, {x0, zb1, x0, zb0}})
            u.segment(new double[]{p[0], yt, p[1]}, new double[]{p[2], yt, p[3]});
        // An access door and soot-blower heads on its right (-x) wall at each deck (owner D3: detail before size): the
        // lances' wall boxes, their carriages and motors 1.2 m over the deck.
        for (int j = 4; j <= top; j += 2) {
            double level = landing(j) - Access.DECK, y = level + Access.DECK + 1.2;
            if (!decked(j) || !backPassDeck(level)) continue;
            trim(u, x0 - .08, level + Access.DECK + .1, zb0 + .35, x0, level + Access.DECK + 1.6, zb0 + .95, Material.MACHINE);
            for (double z = zb0 + 1.4; z < zb1 - .3; z += 1.6) {
                trim(u, x0 - .12, y - .2, z - .2, x0, y + .2, z + .2, Material.MACHINE);
                trim(u, x0 - .3, y - .12, z - .12, x0 - .12, y + .12, z + .12, Material.ACTUATOR);
            }
        }
        // The air heater with an expansion joint under the back pass.
        double[] lo = {x0 - o, yah0, zb0 - o}, hi = {x1 + o, yah1, zb1 + o};
        u.mesh.box(lo, hi, Mesh.ALL_FACES & ~8, Material.MACHINE);
        u.mesh.flat(List.of(new double[]{x0 - o, yah1, zb0 - o}, new double[]{x1 + o, yah1, zb0 - o}, new double[]{x1 + o, yah1, zb0}, new double[]{x0 - o, yah1, zb0}), new double[]{0, 1, 0}, Material.MACHINE);
        u.mesh.flat(List.of(new double[]{x0 - o, yah1, zb1}, new double[]{x1 + o, yah1, zb1}, new double[]{x1 + o, yah1, zb1 + o}, new double[]{x0 - o, yah1, zb1 + o}), new double[]{0, 1, 0}, Material.MACHINE);
        u.solids.add(Solid.box(lo, hi));
        u.volumes.add(Solid.box(new double[]{x0 - o + .1, yah0 + .1, zb0 - o + .1}, new double[]{x1 + o - .1, yah1 + .3, zb1 + o - .1}));
        FluidizedBedParts.band(u, x0 - .05, yah1, zb0 - .05, x1 + .05, yah1 + .25, zb1 + .05, Material.STEELWORK);
        for (double y = yah0 + 2; y < yah1 - .5; y += 2) FluidizedBedParts.band(u, x0 - o - .06, y, zb0 - o - .06, x1 + o + .06, y + .12, zb1 + o + .06, Material.MACHINE);
        // The hoppers.
        int hoppers = w < 8 ? 2 : w < 16 ? 3 : 4;
        double depth = zb1 - zb0 + 2 * o, hh = Math.min(.6 * depth, yah0 - 1.3), cz = (zb0 + zb1) / 2, out = .2, each = (x1 - x0 + 2 * o) / hoppers;
        for (int k = 0; k < hoppers; k++) {
            double h0 = x0 - o + k * each, h1 = h0 + each, hcx = (h0 + h1) / 2, yb = yah0 - hh, za = zb0 - o, zz = zb1 + o;
            double[][] top = {{h0, yah0, za}, {h1, yah0, za}, {h1, yah0, zz}, {h0, yah0, zz}};
            double[][] bottom = {{hcx - out, yb, cz - out}, {hcx + out, yb, cz - out}, {hcx + out, yb, cz + out}, {hcx - out, yb, cz + out}};
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                double[] outward = {(top[i][0] + top[j][0]) / 2 - hcx, -.3, (top[i][2] + top[j][2]) / 2 - cz};
                u.mesh.flat(List.of(top[i], top[j], bottom[j], bottom[i]), outward, Material.MACHINE);
            }
            u.solids.add(Solid.box(new double[]{hcx - .3 * each, yb, cz - .3 * depth}, new double[]{hcx + .3 * each, yah0, cz + .3 * depth}));
            box(u, hcx - .22, yb - .45, cz - .22, hcx + .22, yb, cz + .22, Material.MACHINE);
            trim(u, hcx + .22, yb - .35, cz - .1, hcx + .45, yb - .1, cz + .1, Material.MOTOR);
            u.mesh.cylinder(Frame.of(new double[]{hcx, 0, cz}, 1, 1), .12, Math.max(.3, yb - 1.2), yb - .45, 8, Material.PIPE);
        }
        // Legs under the air heater's corners and between its hoppers, braced along the sides.
        var legs = new ArrayList<Double>();
        for (int k = 0; k <= hoppers; k++) legs.add(x0 - o + k * each);
        double leg = .35;
        for (double x : legs)
            for (double z : new double[]{zb0 - o, zb1 + o}) {
                double lx = Math.max(x0 - o, Math.min(x1 + o - leg, x - leg / 2)), lz = z < cz ? z : z - leg;
                box(u, lx, 0, lz, lx + leg, yah0, lz + leg, Material.STEELWORK);
            }
        for (double z : new double[]{zb0 - o + leg / 2, zb1 + o - leg / 2})
            for (int k = 0; k < hoppers; k += 2) {
                double p = legs.get(k) + leg / 2, q = legs.get(k + 1) - leg / 2;
                u.brace(new double[]{p, z}, new double[]{q, z}, .4, yah0 - .3);
            }
        // The hot primary air into the windbox's back, the secondary air into the ring's back.
        double wpa = Math.max(1, Math.min(4.5, .25 * w));
        closedBox(u, new double[]{a - wpa / 2, g - hw + .2, rear(g) - .05}, new double[]{a + wpa / 2, g - .15, zb0 - o + .05}, Material.CLADDING);
        closedBox(u, new double[]{a - wpa / 2, g + .9, rear(g + 1.7) + .65}, new double[]{a + wpa / 2, g + 1.7, zb0 - o + .05}, Material.CLADDING);
    }

    /**
     * The fans at grade behind the air heater ({@link FluidizedBedParts#fan}), their inlets towards it: the ID fan in the
     * middle drawing the flue gas from the air heater's back through its inlet box and blowing it up and back into the
     * stack's foot; the PA fan on its right and the SA fan on its left, each with an inlet silencer (the air's way in),
     * blowing up and forward into the air heater's back through a cold-air duct. The stack behind the ID fan's motor.
     */
    void fans() {
        double bell = .3 + .2 * r1, ls = .6 + .5 * r1, z0 = zb1 + .55 + Math.max(bell + ls, 1.2);
        double rm = Math.max(.35, Math.min(.8, .3 + .02 * w)), rmId = Math.max(.4, Math.min(.9, .35 + .025 * w));
        double yId = .81 * r1id + .35, yFan = .81 * r1 + .35;
        var id = FluidizedBedParts.fan(u, ab, yId, z0, r1id, rmId, false);
        // The inlet box from the air heater's back to the ID fan's bell.
        double idTop = Math.max(yId + .7 * r1id, yah0 + 1);
        closedBox(u, new double[]{ab - .7 * r1id, yId - .7 * r1id, zb1 + .15}, new double[]{ab + .7 * r1id, idTop, z0 - .3 - .2 * r1id}, Material.MACHINE);
        // Up from the throat and back into the stack.
        double ds = Math.max(1.5, Math.min(6, .25 * w)), zst = id[2] + .8 + ds / 2, hsd = .6 * r1id, ys1 = yId + r1id + .4 + hsd;
        double[] t = FluidizedBedParts.throat(ab, r1id);
        closedBox(u, new double[]{t[0], yId + .5 * r1id, z0 + .05}, new double[]{t[1], ys1, id[1] - .05}, Material.MACHINE);
        closedBox(u, new double[]{t[0], ys1 - hsd, id[1] - .05}, new double[]{t[1], ys1, zst - ds / 2 + .3}, Material.MACHINE);
        FluidizedBedParts.stack(u, ab, zst, ds, Math.min(60, yt + 10));
        // The PA (right) and SA (left) fans and their cold-air ducts.
        double hcd = .5 * r1, wcd = .5 * r1, yc1 = Math.max(yFan + r1 + .4, idTop + .3) + hcd;
        for (int side = -1; side <= 1; side += 2) {
            double x = ab + side * px;
            var fan = FluidizedBedParts.fan(u, x, yFan, z0, r1, rm, true);
            double[] th = FluidizedBedParts.throat(x, r1);
            closedBox(u, new double[]{th[0], yFan + .5 * r1, z0 + .05}, new double[]{th[1], yc1, fan[1] - .05}, Material.MACHINE);
            double xt = (th[0] + th[1]) / 2, xe = Math.max(ab - hxb + .3 + wcd / 2, Math.min(ab + hxb - .3 - wcd / 2, xt)), zt = (z0 + fan[1]) / 2;
            double[] p = {xt, yc1 - hcd / 2, zt}, q = {xe, yc1 - hcd / 2, zb1 + .1};
            double[] along = {(q[0] - p[0]) / 2, 0, (q[2] - p[2]) / 2};
            double l = Math.sqrt(along[0] * along[0] + along[2] * along[2]);
            u.mesh.block(new double[]{(p[0] + q[0]) / 2, p[1], (p[2] + q[2]) / 2}, along, new double[]{0, hcd / 2, 0}, new double[]{-along[2] / l * wcd / 2, 0, along[0] / l * wcd / 2},
                    Material.MACHINE);
        }
    }

    // ---- Fuel feed and bed ash ----------------------------------------------------------------------------------------

    /** The bins' tops, m over grade. */
    double binTop() {
        return feeder - .85 + .7 * binWidth + 1 + .6 * binWidth;
    }

    /**
     * One day bin per chute on four legs in front of the front deck, its pyramid outlet over a drag-chain feeder that runs
     * under the feeder floor's deck to the chute, driven at its front end ({@link FluidizedBedParts#drive}, the coupling
     * turning); the chute down into the front wall 2.5 m over the grid (survey: 2 to 3 m). Two bins and more are filled by
     * a screw conveyor over their roofs, the fuel coming in on its middle.
     */
    void bins() {
        double pb = w / chutes, bw = binWidth, zb = zF - binAt, yo = feeder - .85, cone = .7 * bw, top = binTop(), f0 = feeder - 1.5, f1 = yo;
        for (int i = 0; i < chutes; i++) {
            double x = a - w / 2 + (i + .5) * pb;
            closedBox(u, new double[]{x - bw / 2, yo + cone, zb - bw / 2}, new double[]{x + bw / 2, top, zb + bw / 2}, Material.MACHINE);
            double[][] rim = {{x - bw / 2, yo + cone, zb - bw / 2}, {x + bw / 2, yo + cone, zb - bw / 2}, {x + bw / 2, yo + cone, zb + bw / 2}, {x - bw / 2, yo + cone, zb + bw / 2}};
            double[][] neck = {{x - .25, yo, zb - .25}, {x + .25, yo, zb - .25}, {x + .25, yo, zb + .25}, {x - .25, yo, zb + .25}};
            for (int k = 0; k < 4; k++) {
                int j = (k + 1) % 4;
                u.mesh.flat(List.of(rim[k], rim[j], neck[j], neck[k]), new double[]{(rim[k][0] + rim[j][0]) / 2 - x, -.3, (rim[k][2] + rim[j][2]) / 2 - zb}, Material.MACHINE);
            }
            u.solids.add(Solid.box(new double[]{x - .3 * bw, yo, zb - .3 * bw}, new double[]{x + .3 * bw, yo + cone, zb + .3 * bw}));
            FluidizedBedParts.band(u, x - bw / 2 - .06, yo + cone - .2, zb - bw / 2 - .06, x + bw / 2 + .06, yo + cone, zb + bw / 2 + .06, Material.STEELWORK);
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz = -1; sz <= 1; sz += 2) {
                    double lx = x + sx * (bw / 2 - .1), lz = zb + sz * (bw / 2 - .1);
                    box(u, lx - .1, 0, lz - .1, lx + .1, yo + cone - .2, lz + .1, Material.STEELWORK);
                }
            // The feeder, its drive and its chute.
            box(u, x - .3, f0, zb - .45, x + .3, f1, zF - .15, Material.MACHINE);
            FluidizedBedParts.drive(u, x, (f0 + f1) / 2, zb - .45, 1, FluidizedBedParts.FEEDER_MOTOR, f0);
            u.mesh.bar(new double[]{x, f0 + .2, zF - .4}, new double[]{x, g + 2.5, front(g + 2.5) + .1}, .45, new double[]{1, 0, 0}, Material.MACHINE);
            u.mesh.ring(Frame.of(new double[]{x, g + 2.5, front(g + 2.5)}, 2, -1), 0, .35, .1, .25, 12, 3, Material.FLANGE);
        }
        if (chutes > 1) {
            double c0 = a - w / 2 + .5 * pb - .4, c1 = a + w / 2 - .5 * pb + .4;
            closedBox(u, new double[]{c0, top + .4, zb - .3}, new double[]{c1, top + 1, zb + .3}, Material.MACHINE);
            for (int i = 0; i < chutes; i++) {
                double x = a - w / 2 + (i + .5) * pb;
                box(u, x - .15, top, zb - .15, x + .15, top + .4, zb + .15, Material.MACHINE);
            }
            trim(u, c1, top + .35, zb - .35, c1 + .5, top + 1.05, zb + .35, Material.MOTOR);
        }
    }

    /**
     * Under the grid: a bed-ash drain per cooler from the windbox's floor into the inlet hood of a rotary drum cooler
     * (survey: 2 to 4 drains; drums 1 to 1.5 m by 4 to 6 m, water-cooled, turning at 1 to 6 rpm: here 3 rpm,
     * {@link #DRUM_SPEED}), the drum on trunnion rollers under its riding rings, its pinion and motor
     * under its girth gear; its outlet hood drops the ash into a drag-chain conveyor across the furnace's back that rises
     * at 45° to its head beside the furnace's left, where the bed ash leaves.
     */
    void coolers() {
        int count = w < 10 ? 2 : w < 18 ? 3 : 4;
        double dcl = Math.max(1, Math.min(1.5, .9 + .025 * w)), lc = Math.max(4, Math.min(6, 3.5 + .1 * w)), ycl = .75 + dcl / 2;
        double zd0 = front(g) + .55, zd1 = zd0 + lc, zcv = conveyorZ(zd1), pitchC = w / count;
        ashZ = zcv;
        for (int k = 0; k < count; k++) {
            double x = a - w / 2 + (k + .5) * pitchC, half = dcl / 2 + .15;
            FluidizedBedParts.drum(u, x, ycl, zd0, zd1, dcl, DRUM_SPEED, .618 * k);
            // The inlet hood and the drain into it; the outlet hood down into the conveyor.
            box(u, x - half, ycl - .3 * dcl, zd0 - .55, x + half, ycl + dcl / 2 + .35, zd0 + .2, Material.MACHINE);
            u.mesh.cylinder(Frame.of(new double[]{x, 0, zd0 - .2}, 1, 1), .18, ycl + dcl / 2 + .35, g - hw + .02, 10, Material.PIPE);
            u.mesh.cylinder(Frame.of(new double[]{x, 0, zd0 - .2}, 1, 1), .26, ycl + dcl / 2 + .9, ycl + dcl / 2 + 1.1, 10, Material.ACTUATOR);
            u.solids.add(Solid.rod(new double[]{x, 0, zd0 - .2}, 1, ycl + dcl / 2 + .35, g - hw, .18, 0));
            box(u, x - half, .75, zd1 - .2, x + half, ycl + dcl / 2 + .3, zcv + .35, Material.MACHINE);
            // Rollers under the riding rings, the drive under the girth gear.
            for (double t : new double[]{.18, .82}) {
                double z = zd0 + t * lc;
                trim(u, x - .45 * dcl, 0, z - .3, x + .45 * dcl, .2, z + .3, Material.CONCRETE);
                for (int sx = -1; sx <= 1; sx += 2)
                    u.mesh.cylinder(Frame.of(new double[]{x + sx * .3 * dcl, .2 + .12, 0}, 2, 1), .12, z - .2, z + .2, 10, Material.FLANGE);
                u.solids.add(Solid.box(new double[]{x - .45 * dcl, 0, z - .3}, new double[]{x + .45 * dcl, ycl - dcl / 2, z + .3}));
            }
            double zm = zd0 + .5 * lc;
            trim(u, x - .3, 0, zm - .25, x + .3, ycl - dcl / 2 - .12, zm + .25, Material.MACHINE);
            u.mesh.cylinder(Frame.of(new double[]{x, .32, 0}, 2, 1), .2, zm + .3, zm + 1.1, 12, Material.MOTOR);
            u.solids.add(Solid.box(new double[]{x - .3, 0, zm - .25}, new double[]{x + .3, ycl - dcl / 2 - .12, zm + 1.1}));
        }
        // The conveyor: along the back under the coolers' outlets, up at 45° beside the furnace to its head and spout.
        double xe = a + hx + .4, xh = xe + 2.2, xn = outletX();
        box(u, a - w / 2 - .2, .15, zcv - .35, xe, .75, zcv + .35, Material.MACHINE);
        u.mesh.block(new double[]{(xe + xh) / 2, (.45 + 2.65) / 2, zcv}, new double[]{(xh - xe) / 2 + .2, (2.65 - .45) / 2 + .2, 0}, new double[]{-.21, .21, 0}, new double[]{0, 0, .35},
                Material.MACHINE);
        box(u, xh - .3, 2.25, zcv - .4, xh + .75, 3.1, zcv + .4, Material.MACHINE);
        trim(u, xh + .75, 2.5, zcv - .2, xh + 1.05, 2.9, zcv + .2, Material.MOTOR);
        box(u, xn - .3, 1.6, zcv - .3, xn + .3, 2.25, zcv + .3, Material.MACHINE);
        // A post under the incline; the head's post stands beside it, clear of the cell under the spout (the outlet's pipe).
        box(u, xe + .52, 0, zcv - .3, xe + .68, 1, zcv - .14, Material.STEELWORK);
        box(u, xh - .18, 0, zcv + .52, xh - .02, 2.25, zcv + .68, Material.STEELWORK);
        trim(u, xh - .18, 2.1, zcv + .35, xh - .02, 2.25, zcv + .52, Material.STEELWORK);
    }

    /** A bed-ash cooler drum's turning, rev/s: 3 rpm (real 1 to 6), whole turns in {@link ProcessUnit#MOTION_PERIOD} ticks. */
    static final double DRUM_SPEED = .05;

    /** The ash conveyor's line along z: a cell centre past the coolers' ends and clear of the side columns behind the furnace. */
    double conveyorZ(double zd1) {
        double z = Math.floor(zd1 + .55) + .5, zm = midColumn();
        while (Math.abs(z - zm) < hc / 2 + .9) z += 1;
        return z;
    }

    /** The bed-ash outlet's x: the cell centre under the conveyor head's spout. */
    double outletX() {
        return Math.floor(a + hx + .4 + 2.2 + .2) + .5;
    }

    /** The middle columns' line along z: just behind the furnace. */
    double midColumn() {
        return zR + .3 + hc / 2;
    }

    /** The rear columns' line along z: just behind the cyclones. */
    double rearColumn() {
        return zc + rc + .15 + hc / 2;
    }

    // ---- Steel and the roof -------------------------------------------------------------------------------------------

    /** The front columns' line along z: outside the front deck. */
    double frontColumn() {
        return zF - DECK - hc / 2;
    }

    /** The cross girder's line over the roof, a third back. */
    double crossGirder() {
        return zF + .3 * (d + .5);
    }

    /** The columns' and girders' top, m over grade. */
    double steelTop() {
        return yt + .5 + dg;
    }

    /**
     * The open frame the boiler hangs in (survey: 4 to 8 perimeter columns, roof girders 2 to 4 m deep, X-bracing in the
     * end bays only): columns on concrete pedestals at both sides of the front, middle and rear lines and across the front
     * at the bins' gaps (every 10 m at most); girders along the front, middle and sides and a cross girder over the roof
     * with hanger rods down to it; X-bracing in the side bays between the decks above the feeder floor (on the right only
     * behind the stair tower, whose landings cross its front bay).
     */
    void steel() {
        double zf = frontColumn(), zm = midColumn(), zr = rearColumn(), top = steelTop(), y0 = top - dg, gw = .225;
        var fronts = new ArrayList<Double>(List.of(a - colOff, a + colOff));
        int between = (int) Math.ceil(2 * colOff / 10 - 1e-9) - 1;
        double pb = w / chutes;
        for (int k = 1; k <= between; k++) {
            double want = a - colOff + 2 * colOff * k / (between + 1), best = want, gap = Double.MAX_VALUE;
            for (int i = 1; i < chutes; i++) {
                double at = a - w / 2 + i * pb;
                if (Math.abs(at - want) < gap) {
                    gap = Math.abs(at - want);
                    best = at;
                }
            }
            fronts.add(best);
        }
        for (double x : fronts) column(x, zf, top);
        for (double z : new double[]{zm, zr})
            for (int side = -1; side <= 1; side += 2) column(a + side * colOff, z, top);
        // Girders.
        box(u, a - colOff, y0, zf - gw, a + colOff, top, zf + gw, Material.STEELWORK);
        box(u, a - colOff, y0, zm - gw, a + colOff, top, zm + gw, Material.STEELWORK);
        for (int side = -1; side <= 1; side += 2) box(u, a + side * colOff - gw, y0, zf, a + side * colOff + gw, top, zr, Material.STEELWORK);
        double z1 = crossGirder();
        box(u, a - colOff + gw, y0, z1 - gw, a + colOff - gw, top, z1 + gw, Material.STEELWORK);
        for (double x = a - w / 2 + 1; x < a + w / 2 - .5; x += 2.5) u.mesh.bar(new double[]{x, yt, z1}, new double[]{x, y0, z1}, .06, new double[]{1, 0, 0}, Material.STEELWORK);
        // X-bracing in the side bays between the decked levels (the decks rest on the buckstays).
        int from = 4;
        for (int j = 6; j <= top(); j += 2) {
            if (!decked(j)) continue;
            double y0b = landing(from) - Access.DECK, y1b = landing(j) - Access.DECK - .35;
            for (int side = -1; side <= 1; side += 2) {
                double x = a + side * colOff;
                if (side > 0) u.brace(new double[]{x, zf + hc / 2}, new double[]{x, zm - hc / 2}, y0b, y1b);
                u.brace(new double[]{x, zm + hc / 2}, new double[]{x, zr - hc / 2}, y0b, y1b);
            }
            from = j;
        }
    }

    /** The highest deck's landing. */
    int top() {
        return top;
    }

    /** A column at (x, z) from its concrete pedestal to {@code top}: an H-section drawn as its box. */
    void column(double x, double z, double top) {
        double p = hc / 2 + .2;
        double[] lo = {x - p, 0, z - p}, hi = {x + p, .6, z + p}, clo = {x - hc / 2, .6, z - hc / 2}, chi = {x + hc / 2, top, z + hc / 2};
        u.mesh.box(lo, hi, Mesh.ALL_FACES & ~4, Material.CONCRETE);
        u.mesh.box(clo, chi, Mesh.ALL_FACES & ~4, Material.STEELWORK);
        u.solids.add(Solid.box(lo, hi));
        u.solids.add(Solid.box(clo, chi));
    }

    /**
     * Over the roof: the steam drum (W + 2 m long, 1.2 to 1.8 m across, survey) on two beams resting on the cross and
     * middle girders, the riser bundles from the roof header under it, the downcomer down the left wall's rear corner to
     * the lower furnace (one, of two downcomers' bore: the right side's walkway runs back to the back pass), two safety
     * valves on top with their tall vent silencers (a signature skyline). Survey: drum, downcomers and riser bundles as
     * single bars.
     */
    void roof() {
        double top = steelTop(), z1 = crossGirder(), zm = midColumn(), x0 = a - w / 2 - 1, x1 = a + w / 2 + 1;
        for (int side = -1; side <= 1; side += 2) {
            double x = a + side * .3 * w;
            box(u, x - .2, top, z1 - .225, x + .2, top + .45, zm + .225, Material.STEELWORK);
            box(u, x - .45 * rd, top + .45, zdr - .3, x + .45 * rd, yd - .6 * rd, zdr + .3, Material.STEELWORK);
        }
        var f = Frame.of(new double[]{0, yd, zdr}, 0, 1);
        int n = ProcessUnit.sides(2 * rd);
        u.mesh.prism(f, rd, x0, x1, n, Material.SHELL);
        u.mesh.head(f, rd, x1, rd / 2, n, Material.HEAD);
        u.mesh.head(Frame.of(new double[]{0, yd, zdr}, 0, -1), rd, -x0, rd / 2, n, Material.HEAD);
        FluidizedBedParts.hoop(u, f, rd, rd + .05, (x0 + x1) / 2 - .4, (x0 + x1) / 2 - .3, n, Material.SHELL);
        u.solids.add(Solid.rod(new double[]{0, yd, zdr}, 0, x0 - rd / 2, x1 + rd / 2, rd, 0));
        u.volumes.add(Solid.rod(new double[]{0, yd, zdr}, 0, x0, x1, .9 * rd, 0));
        u.round(new double[]{0, yd, zdr}, 0, x0, x1, rd);
        // The roof header and the riser bundles, one per 4 m of width between the drum's beams.
        double rh = .15 + .004 * w, bundle = .35 + .01 * w;
        box(u, a - w / 2 + .3, yt, zdr - rh, a + w / 2 - .3, yt + 2 * rh, zdr + rh, Material.PIPE);
        int bundles = Math.max(2, (int) Math.round(w / 4));
        for (int k = 0; k < bundles; k++) {
            double x = a - w / 2 + (k + .5) * w / bundles;
            double beam = a + Math.signum(x - a) * .3 * w, clear = .45 * rd + bundle / 2 + .1;
            if (Math.abs(x - beam) < clear) x = beam + (x >= beam ? clear : -clear);
            trim(u, x - bundle / 2, yt + 2 * rh, zdr - .15, x + bundle / 2, yd - .8 * rd, zdr + .15, Material.PIPE);
        }
        // The downcomer: a square bar down the left wall's rear corner, into the wall over the lower headers.
        double half = downcomer(), xdc = a + hx + .65, ye = Math.max(g + 2.2, g + ht * Math.max(0, 1 - (zR - zdr - .3) / s));
        box(u, xdc - half, ye - half, zdr - half, xdc + half, yd - .5 * rd, zdr + half, Material.PIPE);
        box(u, a + hx, ye - half, zdr - half, xdc - half, ye + half, zdr + half, Material.PIPE);
        // The safety valves and their silencers.
        for (int side = -1; side <= 1; side += 2) {
            double x = a + side * w / 4, y0 = yd + rd - .05, vent = y0 + 3.2, rs = .25 + .006 * w;
            trim(u, x - .18, y0, zdr - .18, x + .18, y0 + .45, zdr + .18, Material.MACHINE);
            u.mesh.prism(Frame.of(new double[]{x, 0, zdr}, 1, 1), .06 + .002 * w, y0 + .45, vent - 1.3, 8, Material.PIPE);
            var up = Frame.of(new double[]{x, 0, zdr}, 1, 1);
            u.mesh.cylinder(up, rs, vent - 1.3, vent, 12, Material.MACHINE);
            u.mesh.prism(up, .06 + .002 * w, vent, vent + .3, 8, Material.PIPE);
        }
    }

    /** The downcomer's half side, m: two downcomers' bore of 0.2 + 0.005 W radius each. */
    double downcomer() {
        return Math.sqrt(2 * Math.PI) / 2 * (.2 + .005 * w);
    }

    // ---- Access -------------------------------------------------------------------------------------------------------

    /** The stair tower's front, just behind the furnace's front plane. */
    double towerFront() {
        return zF + .1;
    }

    /**
     * Decks round the furnace's front and sides at the feeder floor, every fourth landing above it and the highest
     * (survey: at the feeder floor, the SA levels, the cyclone inlets and the roof), each on its buckstay; the front one
     * reaches back into the lower furnace's taper; the left one stops short of the downcomer. The right one opens onto the
     * stair tower's landings and, where it is over the air heater, runs on past the cyclones (a rail on its inner side) to
     * a deck along the back pass's right wall (survey: back-pass platforms at the soot blowers; a stair tower, not caged
     * ladders), a rail across that deck's open start.
     */
    void decks() {
        double zt = towerFront(), end = zdr - downcomer() - .2;
        for (int j = 4; j <= top; j += 2) {
            if (!decked(j)) continue;
            double level = landing(j) - Access.DECK, wall = front(level);
            boolean back = backPassDeck(level);
            u.platform(new Access.Line(new double[]{a - hx - DECK, 0, wall}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, 2 * hx + 2 * DECK), level, DECK + wall - zF, true, true);
            u.platform(new Access.Line(new double[]{a - hx, 0, wall}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, (back ? zb0 : zR) - wall), level, DECK, false, !back, zt - wall,
                    zt + LANDING - wall);
            u.platform(new Access.Line(new double[]{a + hx, 0, wall}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, end - wall), level, DECK, false, true);
            if (!back) continue;
            double face = ab - hxb;
            u.railing(new Access.Line(new double[]{a - hx - DECK, 0, zR}, new double[]{0, 0, 1}, new double[]{1, 0, 0}, zb0 - zR), level, DECK - Access.GAP);
            u.platform(new Access.Line(new double[]{face, 0, zb0}, new double[]{0, 0, 1}, new double[]{-1, 0, 0}, zb1 - zb0), level, face - (a - hx - DECK), false, true);
            u.railing(new Access.Line(new double[]{a - hx, 0, zb0 + Access.RAIL_BAND}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, face - (a - hx)), level, Access.RAIL_BAND);
        }
    }

    /** Whether a furnace deck at {@code level} also serves the back pass: it is over the air heater and its expansion joint. */
    boolean backPassDeck(double level) {
        return level >= yah1 + .3;
    }

    /**
     * The stair tower on the furnace's right (survey: boiler structures use a stair tower with a landing every 3 to 4 m,
     * not caged ladders): two lanes of straight flights ({@link #hf} high to the feeder floor, {@link #HIGH} above it), the
     * outer climbing back from the front landings,
     * the inner climbing forward from the back landings; the front landings reach across the column line onto the
     * furnace's decks; posts at its outer corners.
     */
    void tower() {
        double z0 = towerFront(), xo = a - e - 2 * STAIR, xi = a - hx - DECK, z1 = z0 + 2 * LANDING + flight(4);
        for (int j = 0; j < top; j += 2) {
            // A pair of flights: out along the outer lane to the back landing, back along the inner lane; both the same
            // rise, so the back landing sits a flight behind the front one.
            double back = z0 + LANDING + flight(j);
            u.stair(a - e - 1.5 * STAIR, z0 + LANDING, 2, 1, STAIR, landing(j), landing(j + 1));
            u.stair(a - e - .5 * STAIR, back, 2, -1, STAIR, landing(j + 1), landing(j + 2));
            u.platform(new Access.Line(new double[]{xo, 0, back}, new double[]{1, 0, 0}, new double[]{0, 0, 1}, 2 * STAIR + .1), landing(j + 1) - Access.DECK, LANDING, true, true);
            double level = landing(j + 2) - Access.DECK;
            u.platform(new Access.Line(new double[]{xo, 0, z0 + LANDING}, new double[]{1, 0, 0}, new double[]{0, 0, -1}, xi - xo), level, LANDING, true, !decked(j + 2));
        }
        for (double z : new double[]{z0 - .15, z1 + .15}) {
            double[] lo = {xo - .25, 0, z - .1}, hi = {xo - .05, landing(top) + 1.2, z + .1};
            u.mesh.box(lo, hi, Mesh.ALL_FACES & ~4, Material.STEELWORK);
            u.solids.add(Solid.box(lo, hi));
        }
    }

    // ---- Nozzles ------------------------------------------------------------------------------------------------------

    /** The economiser inlet's and the superheater outlet's rows on the back pass's left wall. */
    double feedwaterY() {
        return Math.floor(g + .35 * h) + .5;
    }

    double steamY() {
        return Math.floor(yt - 1.5) + .5;
    }

    /** A nozzle out of a wall facing +x at {@code x}, its flange face on the next block face 0.15 m beyond. */
    static Nozzle left(Role role, double x, double y, double z, double maxOd, double defaultOd) {
        double base = x - .03;
        return new Nozzle(role, new double[]{base, y, z}, 0, 1, Math.ceil(x + EquipmentShape.STAND_OUT) - base, maxOd, defaultOd, 0, .03);
    }

    /**
     * Seven nozzles (survey; owner decision: flue gas inside): feedwater into the economiser on the back pass's left wall
     * at 0.35 H and steam out of the final superheater on it under the roof; blowdown from the drum's left end; the
     * lower headers' drain out to the right at grade; start-up fuel to the burners' line at the front; the fuel into the
     * bins' conveyor (or the one bin) on top; the bed ash out of the conveyor head's spout, down.
     */
    void nozzles() {
        double zm = Math.floor((zb0 + zb1) / 2) + .5;
        double fw = Math.max(.1, Math.min(.45, .015 * w + .08)), st = Math.max(.15, Math.min(.6, .025 * w + .1));
        u.nozzles.add(left(Role.FEEDWATER, ab + hxb, feedwaterY(), zm, fw, fw));
        u.nozzles.add(left(Role.STEAM, ab + hxb, steamY(), zm, st, st));
        u.nozzles.add(left(Role.BLOWDOWN, a + w / 2 + 1 + rd / 2 - .02, yd, zdr, .08, .06));
        // The drain from the right lower header down to grade.
        double rh = .15 + .004 * w, xd = a - hx - rh, zd = zF + Math.ceil(s - .4) + .5;
        u.line(.04, Material.PIPE, new double[]{xd, g + rh, zd}, new double[]{xd, .5, zd});
        double base = xd + .03;
        u.nozzles.add(new Nozzle(Role.DRAIN, new double[]{base, .5, zd}, 0, -1, base - Math.floor(xd - .03 - EquipmentShape.STAND_OUT), .08, .05, 0, .03));
        u.nozzles.add(ProcessUnit.front(Role.FUEL, fuelX(), g + 2.5, zF - .2, .1, .08));
        double zb = zF - binAt, roof = chutes > 1 ? binTop() + 1 : binTop(), in = roof - .03;
        u.nozzles.add(new Nozzle(Role.SOLIDS_INLET, new double[]{a, in, zb}, 1, 1, Math.ceil(roof + EquipmentShape.STAND_OUT) - in, chutes > 1 ? .5 : Math.min(1, binWidth - .4), .3, 0, .03));
        u.nozzles.add(new Nozzle(Role.SOLIDS_OUTLET, new double[]{outletX(), 1.63, ashZ}, 1, -1, .63, .5, .3, 0, .03));
    }
}
