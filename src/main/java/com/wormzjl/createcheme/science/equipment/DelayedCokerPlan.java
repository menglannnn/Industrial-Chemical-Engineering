package com.wormzjl.createcheme.science.equipment;

/**
 * A delayed coker's sizes and layout in the unit's frame ({@link DelayedCokerDesign}; {@code NEW_UNITS_SURVEY_FCC_COKER.md}
 * section 3, whose numbers are the definition; owner decisions D3 and D4 2026-10-18: real sizes, its own structure).
 *
 * <p>The drums, D = {@code d} outside the shell, stand in one row along x at a pitch of D + 4 m, both pairs' drums in the
 * same row; the structure is about 2N (D + 4) + 2 m long and D + 5 m wide (rounded so that drum 0's axis lies on a cell
 * centre, its chute's toe being the coke outlet). A drum: the straight shell min(29.3, 2.5 D + 8) m [Ellis &amp; Paul;
 * Bechtel's 29.26 m limit], a 2:1 top head of D/4, a 30° cone of 0.87 (D - 2) m down to the 2 m bottom flange at
 * {@code support} (8 to 16 m) [Ellis &amp; Paul; unheading patents], 100 mm of insulation under an aluminium jacket. The
 * massive concrete table (1.5 m slab, columns of 0.15 D and 1.2 m at least) carries the drums' skirts on its top, 0.5 m
 * over the bottom flanges; the switch deck lies 3.9 m under its top [Coking 101; US8221591]; the steel frame on it
 * carries an intermediate deck half way up and the cutting deck 1.2 to 2.2 m over the relief and cutting water lines,
 * which run with the overhead headers over the top heads' shoulders. Each derrick stands on the cutting deck over its
 * drum: a straight square lattice of 3.5 m (room for the hose loop inside [survey: 3 m and more]), as tall as the drum
 * and 5 m more so the withdrawn drill stem clears it [survey; derrick patent: about 40 m]. One stair tower (the kit's
 * {@link ProcessUnit#stairTower}) stands behind the middle of the row (one for both pairs: a second would cost a sixth of
 * the quad budget), its flights of at most 3.6 m rise (OSHA 3.7 m) switching back between landings at its two ends and
 * stopping at every deck ({@link #stops}). Where the quad
 * budget binds (owner D3: detail goes before size), detail that grows with the unit is drawn coarser: 24 drum sides
 * (facets of 1.35 m at most), one intermediate deck, derrick stories of three sides.
 */
final class DelayedCokerPlan {
    /** The jacket's insulation, m (100 mm under aluminium [Ellis &amp; Paul]). */
    static final double INSULATION = .1;
    /** The bottom flange's radius (2 m across) and the cone's small end, m. */
    static final double BOTTOM = 1;
    /** The bottom spool from the drum's flange down to the unheading valve, the valve's body height, a flange's thickness, m. */
    static final double SPOOL = 2, VALVE = .8, FLANGE = .12;
    /** The table slab's thickness and the half side of its square opening round each cone, m. */
    static final double SLAB = 1.5, TABLE_HOLE = 1.5;
    /** The half side of the switch deck's opening round each chute, m (the chute's upper section is 2.2 m across). */
    static final double CHUTE_HOLE = 1.45;
    /** How far a deck's outer edge stays in from the structure's faces, m: its handrail then stands inside one row of cells. */
    static final double EDGE = .05;
    /** The cut drum's slider-crank (survey: a 4 m band worked up and down, about 2 min a cycle): crank, rod, m; period, s. */
    static final double CRANK = 2, ROD = 20, PERIOD = 120;
    /**
     * A derrick's side, m: room for the hose loop inside [survey: 3 m and more]; straight, its legs 1.75 m off the axis, so
     * each stands inside one column of cells from the deck to the crown (a tapered leg crosses a column's edge for half its height).
     */
    static final double DERRICK = 3.5;
    /**
     * Coke per drum (calc): the bed's density in the drum, t/m³ (sponge coke 0.8 to 0.95 [Ellis &amp; Paul]), the filled
     * share of the straight shell (the rest is the foam outage), the coke yield of the feed by weight (vacuum residue, 25 to
     * 35 %), a drum's fill time, h (12 to 24 h; 16 h typical [WP-DC]).
     */
    static final double BED = .85, FILLED = .75, YIELD = .3, FILL_HOURS = 16;
    /** Fixed line sizes, m: quench oil into the overhead header, the cutting water riser [survey: 0.15 m], the coke outlet [Q5]. */
    static final double QUENCH_OD = .06, CUT_OD = .15, SOLIDS_OD = 1;

    final double d, r, jacket, straight, cone, head, topFlange;
    final int pairs, drums, support, sides;
    /** Plan: the drums' pitch, the structure's ends x0, x1 and its front and back z0, zb, the drums' row zc, their axes. */
    final double pitch, x0 = 1, z0 = 1, x1, zb, zc, concrete, steel;
    final double[] xd, xp, lines;
    /** Elevations: the slab's underside and top, the skirt's top (the jacket's foot), the top tangent and apex. */
    final double yUnder, yTable, ySkirt, yTangent, yApex, yCut;
    final int switchLevel, cutLevel;
    final int[] middle;
    /** The derricks: their side (square, straight), height over the cutting deck, top. */
    final double base, derrick, yTop;
    /** Line rows: feed, transfer, water; steam; the overhead headers; relief and cutting water; their z. */
    final double yFeed, ySteam, yVapour, yRelief, zVapour, zBlow, zFeed, zWater, zSteam, zCut, rho;
    final double feedOd, steamOd, waterOd, vapourOd, overheadOd, blowdownOd, reliefOd;
    /** The stair tower's plan ({@link ProcessUnit#towerPlan}: to every stop). */
    final ProcessUnit.TowerPlan tower;
    /** Its first foot (lane A, next to the structure, rising +x), on a whole metre: the tower centred behind the row. */
    final double towerX;

    DelayedCokerPlan(EquipmentShape shape) {
        d = shape.diameter();
        pairs = (int) shape.tangentLength();
        drums = 2 * pairs;
        support = shape.support();
        r = d / 2;
        jacket = r + INSULATION;
        straight = Math.min(29.3, 2.5 * d + 8);
        cone = (r - BOTTOM) * Math.sqrt(3);
        head = d / 4;
        topFlange = 1.2 + .05 * (d - 4);
        // A drum's sides: 24, a facet of 0.55 to 1.35 m round the jacket (the drums cost a third of the quad budget).
        sides = 24;
        pitch = d + 4;
        concrete = Math.max(1.2, .15 * d);
        steel = .4 + .02 * d;
        // Drum 0's axis on a cell centre about 1 m + half a pitch in from the -x end; the +x end the same, on a whole metre.
        double end = Math.floor(1 + pitch / 2) + .5;
        xd = new double[drums];
        for (int i = 0; i < drums; i++) xd[i] = x0 + end + i * pitch;
        x1 = Math.ceil(xd[drums - 1] + end - 1e-9);
        zc = z0 + Math.round((d + 5) / 2 - .5) + .5;
        zb = 2 * zc - z0;
        xp = new double[pairs];
        for (int k = 0; k < pairs; k++) xp[k] = (xd[2 * k] + xd[2 * k + 1]) / 2;
        lines = new double[drums + 1];
        lines[0] = x0;
        lines[drums] = x1;
        for (int i = 1; i < drums; i++) lines[i] = xd[i - 1] + pitch / 2;

        int s = support;
        yUnder = s - 1;
        yTable = yUnder + SLAB;
        ySkirt = s + cone + .3;
        yTangent = s + cone + straight;
        yApex = yTangent + head;
        switchLevel = s - 4;
        feedOd = Math.max(.15, .03 * d);
        steamOd = Math.max(.1, .02 * d);
        waterOd = .03 * d;
        vapourOd = Math.min(1, .08 * d);
        overheadOd = Math.max(.3, .06 * d);
        blowdownOd = .05 * d;
        reliefOd = .04 * d;
        // The overhead headers' row over the jacket's apex; relief and cutting water a row up; the cutting deck 1.5 m over that.
        double apex = yTangent + jacket / 2;
        yVapour = Math.ceil(apex + .3 + Math.max(vapourOd, overheadOd) / 2 - .5 - 1e-9) + .5;
        yRelief = yVapour + 1;
        cutLevel = (int) Math.ceil(yRelief + 1.2 - 1e-9);
        yCut = cutLevel + Access.DECK;
        // One intermediate deck half way from the table's top to the cutting deck (a second costs 6 % of the quad budget).
        int m = 1;
        middle = new int[m];
        for (int k = 1; k <= m; k++) middle[k - 1] = (int) Math.round(yTable + k * (cutLevel - yTable) / (m + 1));
        base = DERRICK;
        derrick = cone + straight + head + 5;
        yTop = yCut + derrick;
        // Lines under the slab: feed and water headers either side of the spools, steam in front of the valve bodies, over
        // the head of a player on the switch deck.
        yFeed = s - 1.5;
        ySteam = s - 1.85;
        zFeed = zc - 1.8;
        zWater = zc + 1.8;
        zSteam = zc - 3;
        // Over the heads: the vapour header over the front shoulders, blowdown and relief over the back ones, clear of the
        // top spool; the cutting water line a metre in front of the axes (its riser's foot on a cell centre).
        zVapour = zc - Math.max(Math.ceil(.85 + vapourOd / 2 + .15 - 1e-9), Math.round(.7 * r));
        zBlow = zc + Math.max(Math.ceil(.85 + Math.max(blowdownOd, reliefOd) / 2 + .15 - 1e-9), Math.round(.7 * r));
        zCut = zc - 1;
        rho = Math.max(Math.round(.55 * r), .95 + overheadOd / 2 + .1);
        tower = ProcessUnit.towerPlan(stops());
        towerX = Math.round((xd[0] + xd[drums - 1]) / 2 - (tower.from() + tower.to()) / 2);
    }

    /**
     * The deck levels the stair tower stops at, rising (its openings come back in this order): the switch deck, the
     * table's top (its walking surface {@link Access#DECK} over this), the intermediate decks, the cutting deck.
     */
    double[] stops() {
        var out = new double[3 + middle.length];
        out[0] = switchLevel;
        out[1] = yTable - Access.DECK;
        for (int k = 0; k < middle.length; k++) out[2 + k] = middle[k];
        out[2 + middle.length] = cutLevel;
        return out;
    }

    /** The overall drum height, from the bottom flange to the apex, m. */
    double drumHeight() {
        return cone + straight + head;
    }

    /** Coke per drum, t (calc): the bed's density times the cone and the filled share of the straight shell, inside the shell. */
    static double coke(double d) {
        double ri = d / 2 - .05, cone = (d / 2 - BOTTOM) * Math.sqrt(3), straight = Math.min(29.3, 2.5 * d + 8);
        return BED * (Math.PI * ri * ri * FILLED * straight + Math.PI / 3 * cone * (ri * ri + ri * BOTTOM + BOTTOM * BOTTOM));
    }

    /** The unit's feed, t/h (calc): each pair fills one drum every {@link #FILL_HOURS} at {@link #YIELD} coke. */
    static double feed(double d, int pairs) {
        return pairs * coke(d) / YIELD / FILL_HOURS;
    }
}
