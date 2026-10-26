package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Rack cells placed together (batch 2026-10-26-pipe-laying-system, RACK_JUNCTION_PLAN J1 and J2): the shape each takes
 * once, as it is placed, from the racks whose runs point into it and the player's look, one test per row of the J2
 * table; groups and turns built block by block; and the steel they draw.
 */
class RackGroupTest {
    private static final int NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final int[][] STEPS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    private record Cell(int x, int y, int z) {
        Cell step(int face) {
            return new Cell(x + STEPS[face][0], y + STEPS[face][1], z + STEPS[face][2]);
        }
    }

    private static final Cell O = new Cell(0, 0, 0);

    private static RackGeometry.Neighbours around(Map<Cell, RackGeometry.Rack> world, Cell cell) {
        return face -> world.get(cell.step(face));
    }

    /** The world after placing a rack at {@code cell} looking {@code look}: its shape decided from what stands there, nothing reshaped (J1). */
    private static RackGeometry.Rack place(Map<Cell, RackGeometry.Rack> world, Cell cell, int look, boolean sneaking) {
        var rack = RackGeometry.placed(around(world, cell), look, sneaking);
        world.put(cell, rack);
        return rack;
    }

    private static RackGeometry.Rack place(Map<Cell, RackGeometry.Rack> world, Cell cell, int look) {
        return place(world, cell, look, false);
    }

    /** A straight run of racks facing {@code facing} from {@code from}, {@code length} blocks along it. */
    private static void run(Map<Cell, RackGeometry.Rack> world, Cell from, int facing, int length) {
        var at = from;
        for (int i = 0; i < length; i++, at = at.step(facing)) world.put(at, new RackGeometry.Rack(facing, RackGeometry.STRAIGHT));
    }

    private static int faces(int... faces) {
        int out = 0;
        for (int face : faces) out |= 1 << face;
        return out;
    }

    private static int steel(Map<Cell, RackGeometry.Rack> world, Cell cell) {
        var rack = world.get(cell);
        return RackGeometry.steel(around(world, cell), rack, LineGeometry.axis(rack.facing()) == 2 ? cell.z() : cell.x(), 0);
    }

    // ---- the J2 table ----------------------------------------------------------------------------------------------

    @Test
    void noFeedAndNoRackBesideGoesAlongTheLook() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), place(world, O, EAST));
        // A rack under it is no feed: a run across it stands on it (crossing at two heights, X1).
        assertEquals(new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT), place(world, new Cell(0, 1, 0), NORTH));
    }

    @Test
    void noFeedBesideARunFacesTheLook() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(1, 0, 0), NORTH, 1);
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), place(world, O, EAST), "the look, whatever stands beside");
    }

    @Test
    void oneFeedInLineWithTheLookRunsOn() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(0, 0, 1), SOUTH, 1);
        assertEquals(new RackGeometry.Rack(NORTH, RackGeometry.STRAIGHT), place(world, O, NORTH), "on along the feed's run, facing the look");
    }

    @Test
    void oneFeedAcrossTheLookTurnsTowardsTheLook() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(0, 0, 1), NORTH, 1);
        var corner = place(world, O, EAST);
        assertTrue(corner.cornered(), "a corner");
        assertEquals(EAST, corner.facing(), "facing the look");
        assertEquals(faces(SOUTH, EAST), RackGeometry.runs(corner), "from the run (south) towards the look (east)");
        // Out of a corner's side the next block runs on away from it.
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), place(world, new Cell(1, 0, 0), EAST));
    }

    @Test
    void twoFeedsInLineWithTheLookAlongThemFillTheGap() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(-1, 0, 0), WEST, 1);
        run(world, new Cell(1, 0, 0), WEST, 1);
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), place(world, O, EAST), "facing the look");
    }

    @Test
    void twoFeedsInLineWithTheLookAcrossMakeATeeTowardsTheLook() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(0, 0, -1), NORTH, 1);
        run(world, new Cell(0, 0, 1), NORTH, 1);
        var tee = place(world, O, EAST);
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.TEE), tee, "the branch opens towards the look");
        assertEquals(faces(NORTH, SOUTH, EAST), RackGeometry.runs(tee));
    }

    @Test
    void twoFeedsAtACornerMakeTheCornerJoiningThem() {
        for (int look : new int[]{NORTH, SOUTH, WEST, EAST}) {
            var world = new HashMap<Cell, RackGeometry.Rack>();
            run(world, new Cell(0, 0, 1), NORTH, 1);
            run(world, new Cell(1, 0, 0), EAST, 1);
            var corner = place(world, O, look);
            assertTrue(corner.cornered() && corner.facing() == look, "look " + look + ": " + corner);
            assertEquals(faces(SOUTH, EAST), RackGeometry.runs(corner), "look " + look);
        }
    }

    @Test
    void threeFeedsMakeTheTeeJoiningThem() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(0, 0, -1), NORTH, 1);
        run(world, new Cell(0, 0, 1), SOUTH, 1);
        run(world, new Cell(-1, 0, 0), WEST, 1);
        assertEquals(new RackGeometry.Rack(WEST, RackGeometry.TEE), place(world, O, NORTH), "the branch is the feed with none across from it; the tee faces it");
    }

    @Test
    void fourFeedsPlaceAStraightRackAlongTheLook() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        for (int face : new int[]{NORTH, SOUTH}) run(world, O.step(face), NORTH, 1);
        for (int face : new int[]{WEST, EAST}) run(world, O.step(face), EAST, 1);
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), place(world, O, EAST), "no cross block (X1)");
    }

    @Test
    void sneakingPlacesAPlainStraightRackAlongTheLook() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(0, 0, 1), NORTH, 1);
        assertEquals(new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT), place(world, O, EAST, true), "no corner while sneaking");
    }

    // ---- groups ----------------------------------------------------------------------------------------------------

    @Test
    void aGroupPlacedLookingAlongItIsOneStraightRack() {
        // Two racks wide and three long, every block placed looking north, row by row or column by column.
        for (boolean rows : new boolean[]{true, false}) {
            var world = new HashMap<Cell, RackGeometry.Rack>();
            for (int i = 0; i < 6; i++) place(world, rows ? new Cell(i % 2, 0, i / 2) : new Cell(i / 3, 0, i % 3), NORTH);
            for (var cell : world.keySet()) {
                assertEquals(RackGeometry.STRAIGHT, world.get(cell).corner(), cell + " straight");
                int steel = steel(world, cell);
                assertEquals(cell.x() == 0 ? RackGeometry.RIGHT_JOINED : RackGeometry.LEFT_JOINED, steel & (RackGeometry.LEFT_JOINED | RackGeometry.RIGHT_JOINED), cell + " joins the rack beside it");
                assertEquals(cell.z() != 1, (steel & 3) != 0, cell + " has a bent at the run's ends only");
            }
        }
    }

    @Test
    void aTwoWideTurnBuiltRunByRunNestsItsCornersOnTheDiagonal() {
        // Two runs side by side going north, x 0 and 1, turning east into two runs at z 0 and 1: each turns at its own
        // corner, (0, 0) for the outer and (1, 1) for the inner (rack-7). The runs are laid first, each looking along
        // itself; each corner then looking towards the turn; then the turned runs looking east.
        var north = List.of(new Cell(0, 0, 3), new Cell(1, 0, 3), new Cell(0, 0, 2), new Cell(1, 0, 2), new Cell(0, 0, 1));
        var east = List.of(new Cell(1, 0, 0), new Cell(2, 0, 0), new Cell(3, 0, 0), new Cell(2, 0, 1), new Cell(3, 0, 1));
        var corners = List.of(O, new Cell(1, 0, 1));
        for (boolean cornersFirst : new boolean[]{true, false}) {
            var world = new HashMap<Cell, RackGeometry.Rack>();
            for (var cell : north) place(world, cell, NORTH);
            var order = new ArrayList<Cell>();
            if (cornersFirst) order.addAll(corners);
            order.addAll(east);
            if (!cornersFirst) order.addAll(corners);
            for (var cell : order) place(world, cell, EAST);
            for (var cell : world.keySet()) {
                boolean corner = corners.contains(cell);
                assertEquals(corner, world.get(cell).cornered(), cell + (cornersFirst ? ", corners first" : ", corners last"));
                if (corner) assertEquals(faces(SOUTH, EAST), RackGeometry.runs(world.get(cell)), cell + " turns from the south to the east");
            }
        }
    }

    @Test
    void aJunctionsNeighboursStandAWholeBentAgainstIt() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        run(world, new Cell(0, 0, 4), NORTH, 4);
        place(world, O, EAST);
        // The run's last block before the corner: along z 1, a bent only by its neighbours.
        int steel = steel(world, new Cell(0, 0, 1));
        assertEquals(RackGeometry.BENT_FRONT | RackGeometry.END_FRONT, steel & (RackGeometry.BENT_FRONT | RackGeometry.END_FRONT), "a whole bent towards the corner");
        assertEquals(0, steel & RackGeometry.END_BACK, "a shared bent, or none, along the run");
    }

    @Test
    void aStackedRackCarriesTheColumnsUpToTheRackOnIt() {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        place(world, O, EAST);
        place(world, new Cell(0, 1, 0), EAST);
        int lower = steel(world, O), upper = steel(world, new Cell(0, 1, 0));
        assertEquals(RackGeometry.STACKED | RackGeometry.GROUNDED, lower & (RackGeometry.STACKED | RackGeometry.GROUNDED), "stacked, on the ground");
        assertEquals(0, upper & (RackGeometry.STACKED | RackGeometry.GROUNDED), "nothing on the upper rack, and it stands on the lower");
        double top = RackGeometry.members(RackGeometry.framed(world.get(O), lower, RackGeometry.BEAM_TOP)).stream().mapToDouble(box -> box[4]).max().orElseThrow();
        assertEquals(1, top, 1e-9, "the lower rack's columns reach the upper rack");
    }
}
