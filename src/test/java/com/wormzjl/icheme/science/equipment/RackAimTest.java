package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Aiming on a rack's tier (batch 2026-10-26-pipe-laying-system; owner 2026-10-26, "Aim on the tier"): the run face nearest
 * the aimed point is where the line passes through to its route, the free slot nearest the point is its lane, and a cell
 * with no free slot gives the nearest one, not fitting.
 */
class RackAimTest {
    private static final int NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final double SMALL = PipeSizes.outsideOf(PipeSizes.code(.0625)), SIX = PipeSizes.outsideOf(PipeSizes.code(.17));

    private static RackGeometry.Lane laid(RackGeometry.Rack rack, int option, double outside, int arms) {
        return new RackGeometry.Lane(option, RackGeometry.width(outside), arms, arms, PipeSizes.code(outside));
    }

    @Test
    void theFaceAndSlotNearestThePointAreAimed() {
        // Facing north, x is across from the left and the front (north) is at z 0.
        var rack = new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT);
        assertEquals(new RackGeometry.Aim(RackGeometry.option(3, RackGeometry.TIER), NORTH, SOUTH, true), RackGeometry.aim(rack, List.of(), SMALL, new double[]{.8, .4, .1}));
        assertEquals(new RackGeometry.Aim(RackGeometry.option(0, RackGeometry.TIER), SOUTH, NORTH, true), RackGeometry.aim(rack, List.of(), SMALL, new double[]{.2, .4, .9}),
                "near the back: the line passes through the back and the lane ends open at the front");
        // A 14" line, two positions wide: the pair whose centre is nearest.
        var wide = RackGeometry.aim(rack, List.of(), PipeSizes.outsideOf(PipeSizes.code(.356)), new double[]{.5, .4, .1});
        assertEquals(2, RackGeometry.width(PipeSizes.outsideOf(PipeSizes.code(.356))));
        assertEquals(RackGeometry.option(1, RackGeometry.TIER), wide.option(), "positions 1 and 2, centred");
    }

    @Test
    void aTakenSlotGivesTheNearestFreeOneAndAFullCellTheNearestRefused() {
        var rack = new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT);
        int along = 1 << NORTH | 1 << SOUTH;
        var lanes = new ArrayList<RackGeometry.Lane>(List.of(laid(rack, RackGeometry.option(3, RackGeometry.TIER), SIX, along)));
        assertEquals(new RackGeometry.Aim(RackGeometry.option(2, RackGeometry.TIER), NORTH, SOUTH, true), RackGeometry.aim(rack, lanes, SMALL, new double[]{.8, .4, .1}));
        for (int p = 0; p < 3; p++) lanes.add(laid(rack, RackGeometry.option(p, RackGeometry.TIER), SIX, along));
        assertEquals(new RackGeometry.Aim(RackGeometry.option(3, RackGeometry.TIER), NORTH, SOUTH, false), RackGeometry.aim(rack, lanes, SMALL, new double[]{.8, .4, .1}));
        assertNull(RackGeometry.aim(rack, List.of(), PipeSizes.MAX_OUTSIDE, new double[]{.5, .4, .1}), "a line too wide for racks");
    }

    @Test
    void aCornersArmFaceNearestThePointIsAimed() {
        // Facing east with its front-right corner: its lanes turn between the front (east) and the right (south).
        var corner = new RackGeometry.Rack(EAST, RackGeometry.FRONT_RIGHT);
        var nearSide = RackGeometry.aim(corner, List.of(), SMALL, new double[]{.4, .4, .95});
        assertTrue(nearSide.fits() && nearSide.through() == SOUTH && nearSide.open() == EAST && RackGeometry.level(nearSide.option()) == RackGeometry.TIER, "" + nearSide);
        var nearEnd = RackGeometry.aim(corner, List.of(), SMALL, new double[]{.95, .4, .4});
        assertTrue(nearEnd.fits() && nearEnd.through() == EAST && nearEnd.open() == SOUTH, "" + nearEnd);
    }

    @Test
    void aTeesBranchOrRunFaceIsAimedAndItsLaneSuitsTheLeg() {
        // Facing north: the run from the west to the east, the branch out of the north.
        var tee = new RackGeometry.Rack(NORTH, RackGeometry.TEE);
        var branch = RackGeometry.aim(tee, List.of(), SMALL, new double[]{.6, .4, .05});
        assertTrue(branch.fits() && branch.through() == NORTH && RackGeometry.level(branch.option()) != RackGeometry.RUN, "the branch: a turning lane " + branch);
        var run = RackGeometry.aim(tee, List.of(), SMALL, new double[]{.95, .4, .85});
        assertTrue(run.fits() && run.through() == EAST && run.open() == WEST && RackGeometry.level(run.option()) == RackGeometry.RUN, "the run, far from the branch: " + run);
        for (var aim : List.of(branch, run))
            assertTrue(RackGeometry.valid(tee, aim.option(), 1, 1 << aim.through() | 1 << aim.open()), "suits its leg: " + aim);
    }
}
