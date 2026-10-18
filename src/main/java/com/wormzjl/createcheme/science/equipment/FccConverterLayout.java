package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An FCC converter's numbers ({@link FccConverterDesign}; {@code NEW_UNITS_SURVEY_FCC_COKER.md} section 1, owner decisions
 * D3 and D4 of 2026-10-18), in a frame whose origin is the regenerator's axis at grade: the riser and reactor stand
 * {@link #x} m along +x, the front is -z. Every size follows the regenerator's shell D (survey proportions):
 * <ul>
 * <li>regenerator: straight shell 14 + 1.3 D, 2:1 heads; reactor (disengager) 0.6 D across, its straight shell 1.3 of
 * its own diameter; stripper 0.4 D across and 0.9 D + 3 m long under a 30° cone; riser 0.15 D + 0.1 m; standpipes
 * 0.09 D + 0.15 m; riser axis 0.7 D + 4 m from the regenerator's (to a whole metre, so both axes run through cell
 * centres); air line 0.12 D through a start-up heater 2.5 of it across and 3.75 of it high; flue gas line 0.2 D; vapour
 * line 0.17 D;</li>
 * <li>the riser's length {@code length} runs from the wye (3 m up, higher for a riser whose bottom head would not
 * clear grade) to its end just inside the reactor's straight shell; the feed injectors stand 3 m over the wye, pointing
 * 30° up into the riser;</li>
 * <li>the regenerated-catalyst standpipe falls straight from the regenerator's lower shell to the wye, the spent-catalyst
 * standpipe from the stripper's bottom to the regenerator's bed, its last stretch a 45° lateral over the other's exit,
 * both within 45° of vertical so catalyst flows (survey). Their slide valves sit over the structure's floors: the
 * spent-catalyst one near its low end, the regenerated-catalyst one as low as the injectors let it (in the headers' gap
 * over the injector floor, or a floor higher).</li>
 * </ul>
 * {@link #fits} says whether the layout keeps both standpipes within 45°, its floors 2.3 m apart and its valves clear of
 * the injectors, the riser, the stripper and each other; the design offers only lengths and supports that fit.
 */
final class FccConverterLayout {
    static final double COS30 = Math.sqrt(3) / 2, TAN15 = 2 - Math.sqrt(3);
    /** Least height between two floors' walking surfaces, m: 2.1 m of headroom and the deck. */
    static final double FLOOR_GAP = 2.3;
    /** A slide valve's body over its floor: its underside clears the opening's handrail (1.07 m) by a little, m. */
    static final double VALVE_CLEAR = 1.25;
    /** A slide valve's actuator at most this high over its floor, m (a hand reaches 2.1 m, a gauge is read higher). */
    static final double VALVE_REACH = 2.8;
    static final int MIN_RISER = 24, MAX_RISER = 44, MIN_BOTTOM = 6, MAX_BOTTOM = 14;

    final double d;
    final int length, support;
    // Regenerator: radius, straight shell, head depth.
    final double r, straight, head;
    // Reactor (radius, straight shell), stripper (radius, length), the cone between them (height), riser radius.
    final double rr, reactorShell, rs, stripperShell, cone, rri;
    // Standpipes (diameter, radius), the riser's axis from the regenerator's (whole metres).
    final double dsp, asp;
    final int x;
    // Lines: air, its heater (diameter, straight height), flue gas, vapour.
    final double air, heater, heaterHeight, flue, vapour;
    // Elevations: wye, riser end, reactor bottom and top tangents, stripper top and bottom tangents.
    final double wye, end, reactorBottom, reactorTop, stripperTop, stripperBottom;
    // Feed injectors: inner ends' height, reach outside the riser, diameter; their outer ends' radius and height; the feed
    // and steam ring headers' radii, heights and diameters; the zone's outer radius; the half-angle of the headers' gap
    // facing the regenerated-catalyst standpipe; how many injectors.
    final double injectorAt, injectorReach, injectorOd, outer, outerY, feedRing, feedY, feedOd, steamRing, steamY, steamOd, zone, gap;
    final int injectors;
    // The steel frame round the riser and reactor: its half width across z, its -x and +x faces; a floor's opening round
    // the riser (a deck stops 0.15 m from it, or from the standpipe passing).
    final double half, xf, xe, riserHole;
    // The regenerated-catalyst valve floor's -x end: the frame's face, or, under the regenerator's bottom, out to its table.
    final double rcsvEnd;
    // Walking surfaces: the injector floor, the regenerated- and spent-catalyst slide valves' floors (the first may be
    // the injector floor), the stripper's steam ring floor, the reactor manway floor and the top floor; the valve floors'
    // openings (half sizes).
    final double injectorFloor, rcsvFloor, scsvFloor, stripperFloor, manwayFloor, topFloor, rcsvHole, scsvHole;
    // The regenerated-catalyst standpipe: its centre at the regenerator's shell (height), sin, cos and tan of its angle
    // from vertical, its valve's centre (x, y), the valve body's half extents up and in plan.
    final double rcspTop, sin1, cos1, tan1, rcsvX, rcsvY, up1, plan1;
    // The spent-catalyst standpipe: its centre at the shell (height), the lateral's run, its elbow (x, y), its start in
    // the stripper (x, at stripperBottom), its upper run's angle from vertical, its valve's centre and half extents.
    final double scspIn, lateral, p1x, p1y, s0x, sin2, cos2, tan2, scsvX, scsvY, up2, plan2;
    // The start-up air heater: its axis's z, its bottom and top tangents, the air line's in and out heights, the air
    // riser's x; the regenerator's table: its four columns' radius (on the diagonals) and size.
    final double heaterZ, heaterBottom, heaterTop, airIn, airOut, airRiserX, tableRadius, tableColumn;
    final boolean rcsvOnInjectorFloor, fits;

    FccConverterLayout(double d, int length, int support) {
        this.d = d;
        this.length = length;
        this.support = support;
        r = d / 2;
        straight = 14 + 1.3 * d;
        head = d / 4;
        rr = .3 * d;
        reactorShell = .78 * d;
        rs = .2 * d;
        stripperShell = .9 * d + 3;
        cone = .1 * d * Math.sqrt(3);
        rri = (.15 * d + .1) / 2;
        dsp = .09 * d + .15;
        asp = dsp / 2;
        x = (int) Math.round(.7 * d + 4);
        air = .12 * d;
        heater = 2.5 * air;
        heaterHeight = 3.75 * air;
        flue = .2 * d;
        vapour = .17 * d;
        wye = wye(d);
        end = wye + length;
        reactorBottom = end - .05 * reactorShell;
        reactorTop = reactorBottom + reactorShell;
        stripperTop = reactorBottom - cone;
        stripperBottom = stripperTop - stripperShell;
        // Feed injectors, their ring headers under their outer ends, the floor 1.6 m under the outer ends.
        injectorAt = wye + 3;
        injectorReach = 1 + .06 * d;
        injectorOd = .1 + .02 * d;
        outer = rri + injectorReach * COS30;
        outerY = injectorAt - injectorReach / 2;
        injectorFloor = outerY - 1.6;
        feedOd = PipeSizes.od(.03 * d, .1, .5);
        steamOd = PipeSizes.od(.02 * d, .1, .5);
        feedRing = outer + .05;
        feedY = injectorFloor + 1.15;
        steamRing = outer + .45;
        steamY = injectorFloor + .75;
        zone = steamRing + steamOd / 2 + .15;
        injectors = (int) Math.round(4 + 4 * (d - 4) / 13);
        // The frame: room for the reactor's floors and for a slide valve's actuator ({@link #valveReach}) 0.5 m inside its
        // rail; its -x face clear of the regenerator's ring platforms (1.45 m wide); its +x face at least as far out, where
        // the stair tower's side meets it: the tower's lanes on cell centres, half a block off this frame's axes
        // (ProcessUnit#stairTower; the side 0.45 m from lane A's centre).
        half = Math.max(3.5, Math.max(rr + 1.4, valveReach(dsp) + .5));
        xf = r + 1.6;
        double side = 1 - ProcessUnit.TOWER_FLIGHT / 2;
        xe = x + Math.ceil(half - side - 1e-9) + side;
        riserHole = Math.max(rri + .15, asp + .25);
        // The regenerated-catalyst standpipe from the regenerator's lower shell to the wye.
        rcspTop = support + .3 + asp;
        tan1 = (x - r) / (rcspTop - wye);
        sin1 = tan1 / Math.sqrt(1 + tan1 * tan1);
        cos1 = 1 / Math.sqrt(1 + tan1 * tan1);
        up1 = .8 * dsp * cos1 + .65 * dsp * sin1;
        plan1 = .8 * dsp * sin1 + .65 * dsp * cos1;
        // Its valve: over the injector floor, in the injector headers' gap and clear of the riser, where its actuator is
        // in reach; else over a floor 2.3 m higher, whose opening holds the injectors if it is among them.
        double rho = Math.max((injectorFloor + VALVE_CLEAR + up1 - wye) * tan1, rri + .3 + plan1);
        rcsvOnInjectorFloor = wye + rho / tan1 - injectorFloor <= VALVE_REACH;
        double rcsv = injectorFloor;
        if (!rcsvOnInjectorFloor) {
            rcsv = Math.max(injectorFloor + FLOOR_GAP, wye + (rri + .3 + plan1) / tan1 - VALVE_CLEAR - up1);
            rho = (rcsv + VALVE_CLEAR + up1 - wye) * tan1;
        }
        rcsvFloor = rcsv;
        rcsvY = wye + rho / tan1;
        rcsvX = x - rho;
        rcsvHole = rcsvOnInjectorFloor || rcsv >= injectorAt + injectorOd / 2 + .8 ? riserHole : zone + .1;
        double column = Math.sqrt(.5) * (r + .1 + .71 * (1 + .03 * d)) + (1 + .03 * d) / 2;
        rcsvEnd = !rcsvOnInjectorFloor && rcsv + 2.1 < support - .3 ? Math.max(r + .3, column + .2) : xf;
        boolean rcsvFits = tan1 <= 1 + 1e-9 && rcsvX - plan1 >= rcsvEnd + .2 && rho + plan1 + .3 <= x - r;
        // The headers' gap: round the standpipe, and round the valve's body where it stands among the injectors.
        double around = (asp + Math.max(feedOd, steamOd) / 2 + .3) / feedRing;
        if (rcsvOnInjectorFloor) around = Math.max(around, (1.1 * dsp + Math.max(injectorOd, feedOd) / 2 + .15) / (rho - plan1));
        gap = DeterministicMath.asin(Math.min(.95, around));
        // The spent-catalyst standpipe: a 45° lateral into the bed over the other standpipe's exit, its upper run from the
        // stripper's bottom, its start inside the stripper.
        scspIn = rcspTop + 1.2 * dsp + .4;
        lateral = 1 + .5 * dsp;
        p1x = r + lateral;
        p1y = scspIn + lateral;
        s0x = x - (rs - asp - .05);
        double drop = stripperBottom - p1y;
        tan2 = drop > 0 ? (s0x - p1x) / drop : 1e3;
        sin2 = tan2 / Math.sqrt(1 + tan2 * tan2);
        cos2 = 1 / Math.sqrt(1 + tan2 * tan2);
        up2 = .8 * dsp * cos2 + .65 * dsp * sin2;
        plan2 = .8 * dsp * sin2 + .65 * dsp * cos2;
        // Its valve near its low end: past the elbow, inside the frame, its floor 2.3 m over the other valve's; that
        // floor's opening the stripper's where the stripper's head is within headroom over it.
        double sx = Math.max(p1x + (.8 * dsp + .6) * sin2, xf + plan2 + .2);
        double floor = Math.max(p1y + (sx - p1x) / tan2 - VALVE_CLEAR - up2, rcsv + FLOOR_GAP);
        scsvFloor = floor;
        scsvY = floor + VALVE_CLEAR + up2;
        scsvX = p1x + (scsvY - p1y) * tan2;
        stripperFloor = stripperBottom + .3;
        scsvHole = floor + 2.1 > stripperBottom - .1 * d ? rs + .15 : riserHole;
        double near = Math.max(0, x - scsvX - plan2);
        boolean underHead = near < rs && scsvY + up2 + .2 > stripperBottom - .1 * d * Math.sqrt(Math.max(0, 1 - near * near / (rs * rs)));
        boolean scsvFits = tan2 <= 1 + 1e-9 && scsvX + plan2 <= x - rri - .3 && floor <= stripperFloor - FLOOR_GAP && !underHead;
        manwayFloor = reactorBottom + .5 * reactorShell - .9;
        topFloor = reactorTop + .25;
        // The air heater in front of the regenerator; its outlet low, so the air line under the regenerator turns up into
        // the bottom head.
        heaterZ = -Math.ceil(r + .6 + heater / 2 - 1e-9);
        heaterBottom = .6 + heater / 4;
        heaterTop = heaterBottom + heaterHeight;
        airIn = heaterTop - .8 * air;
        airOut = heaterBottom + .8 * air;
        airRiserX = -Math.ceil(heater / 2 + .3 + air / 2 - 1e-9);
        tableColumn = 1 + .03 * d;
        tableRadius = r + .1 + .71 * tableColumn;
        fits = rcsvFits && scsvFits && clear() && support >= leastSupport(d);
    }

    /**
     * A slide valve's reach from its standpipe's axis to its actuator's end, m, for a standpipe {@code dsp} across (survey:
     * the body 2.2 OD along it, a bonnet and stem tube 1.5-2 OD long, the electro-hydraulic cylinder 2 OD): half the body
     * 1.1 dsp, the bonnet 0.6 dsp, the stem's tube 1.4 dsp, the cylinder 2 dsp.
     */
    static double valveReach(double dsp) {
        return 5.1 * dsp;
    }

    /** The wye's height: 3 m, or higher where the riser's bottom head (0.85 of its diameter under the wye) would come within 1.5 m of grade. */
    static double wye(double d) {
        return Math.max(3, 1.5 + .85 * (.15 * d + .1));
    }

    /**
     * The standpipes keep apart outside the regenerator: the spent-catalyst line clears the other line (its segment from
     * the wye to the shell) and its valve's body, and its own valve's body clears the other line.
     */
    private boolean clear() {
        double span = Math.sqrt((x - r) * (x - r) + (rcspTop - wye) * (rcspTop - wye));
        for (double y = scspIn; y <= stripperBottom; y += .2) {
            double px = y <= p1y ? r + (y - scspIn) : p1x + (y - p1y) * tan2;
            if (px < r + .05) continue;
            double vx = px - x, vy = y - wye, along = Math.max(0, Math.min(span, -vx * sin1 + vy * cos1));
            double dx = vx + along * sin1, dy = vy - along * cos1;
            if (dx * dx + dy * dy < (dsp + .25) * (dsp + .25)) return false;
            if (Math.abs(px - rcsvX) <= plan1 + asp + .2 && Math.abs(y - rcsvY) <= up1 + asp + .2) return false;
        }
        for (double px = scsvX - plan2; px <= scsvX + plan2; px += .1)
            if (px >= r && scsvY - up2 - rcspAt(px) < asp + .3) return false;
        return true;
    }

    /** The regenerated-catalyst standpipe's axis height at x {@code px}, m. */
    double rcspAt(double px) {
        return wye + (x - px) / tan1;
    }

    /**
     * The lowest regenerator bottom the geometry allows: its regenerated-catalyst standpipe within 45° of vertical, and
     * room under its bottom head for the air line's elbow (bend radius one diameter) up from the heater's outlet.
     */
    static int leastSupport(double d) {
        double r = d / 2, asp = (.09 * d + .15) / 2, air = .12 * d;
        int x = (int) Math.round(.7 * d + 4);
        double standpipe = x - r + wye(d) - .3 - asp, airLine = d / 4 + .6 + 2.5 * air / 4 + .8 * air + air + .4;
        return Math.max(MIN_BOTTOM, (int) Math.ceil(Math.max(standpipe, airLine) - 1e-9));
    }

    private static final Map<Long, Integer> LOWEST = new ConcurrentHashMap<>();

    /**
     * The lowest regenerator bottom from which every one up to {@link #MAX_BOTTOM} fits a riser of {@code length}
     * (the valves find their floors, the standpipes keep apart); {@link #MAX_BOTTOM} + 1 where the highest does not fit.
     */
    static int minSupport(double d, int length) {
        return LOWEST.computeIfAbsent(Math.round(d * 100) * 100 + length, k -> {
            int lowest = MAX_BOTTOM + 1;
            for (int s = MAX_BOTTOM; s >= leastSupport(d) && new FccConverterLayout(d, length, s).fits; s--) lowest = s;
            return lowest;
        });
    }

    /**
     * The shortest riser from which every longer one up to {@link #MAX_RISER} fits some regenerator bottom: a larger
     * regenerator's longer stripper needs a longer riser for the spent catalyst to fall to the bed of a raised
     * regenerator.
     */
    static int minLength(double d) {
        int least = MAX_RISER;
        for (int l = MAX_RISER; l >= MIN_RISER && minSupport(d, l) <= MAX_BOTTOM; l--) least = l;
        return least;
    }
}
