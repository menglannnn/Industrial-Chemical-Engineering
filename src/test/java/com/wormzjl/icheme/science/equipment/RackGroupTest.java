package com.wormzjl.icheme.science.equipment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Rack cells placed together (batch 2026-10-26-pipe-laying-system, RACK_LAYERS_PLAN I3): the shapes a group settles in
 * as each placement updates its neighbours, as the world's shape updates do, whatever the order the cells went down in;
 * and the steel they draw.
 */
class RackGroupTest {
    private static final int NORTH = 2, EAST = 5;
    private static final int[][] STEPS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    private record Cell(int x, int y, int z) {
        Cell step(int face) {
            return new Cell(x + STEPS[face][0], y + STEPS[face][1], z + STEPS[face][2]);
        }
    }

    private static RackGeometry.Neighbours around(Map<Cell, RackGeometry.Rack> world, Cell cell) {
        return face -> world.get(cell.step(face));
    }

    /** Places each rack in turn, its shape from its neighbours, and re-shapes every rack whose neighbour changed until none does. */
    private static Map<Cell, RackGeometry.Rack> place(List<Cell> order, Map<Cell, Integer> facings) {
        var world = new HashMap<Cell, RackGeometry.Rack>();
        for (var cell : order) {
            int facing = facings.get(cell);
            world.put(cell, new RackGeometry.Rack(facing, RackGeometry.corner(around(world, cell), facing)));
            var changed = new ArrayDeque<Cell>(List.of(cell));
            for (int updates = 0; !changed.isEmpty(); updates++) {
                assertTrue(updates < 1000, "the shapes settle");
                var from = changed.poll();
                for (int face = 0; face < 6; face++) {
                    var next = from.step(face);
                    var rack = world.get(next);
                    if (rack == null) continue;
                    int corner = RackGeometry.corner(around(world, next), rack.facing());
                    if (corner == rack.corner()) continue;
                    world.put(next, new RackGeometry.Rack(rack.facing(), corner));
                    changed.add(next);
                }
            }
        }
        return world;
    }

    private static int steel(Map<Cell, RackGeometry.Rack> world, Cell cell) {
        var rack = world.get(cell);
        return RackGeometry.steel(around(world, cell), rack, LineGeometry.axis(rack.facing()) == 2 ? cell.z() : cell.x(), 0);
    }

    @Test
    void aGroupFacingOneWayIsOneStraightRack() {
        var facings = new HashMap<Cell, Integer>();
        for (int x = 0; x < 2; x++) for (int z = 0; z < 3; z++) facings.put(new Cell(x, 0, z), NORTH);
        var world = place(new ArrayList<>(facings.keySet()), facings);
        for (var cell : world.keySet()) {
            assertEquals(RackGeometry.STRAIGHT, world.get(cell).corner(), cell + " straight");
            int steel = steel(world, cell);
            assertEquals(cell.x() == 0 ? 16 : 8, steel & 24, cell + " joins the rack beside it");
            assertEquals(cell.z() != 1, (steel & 3) != 0, cell + " has a bent at the run's ends only");
        }
    }

    @Test
    void aWideTurnNestsItsCornersOnTheDiagonalInAnyOrder() {
        // Two racks wide: a run facing north at x 0 to 1 from z 3, turning east into a run at z 0 to 1 out to x 3. Each
        // run turns at its own corner, (0, 0) for the outer and (1, 1) for the inner; the outer passes the inner straight.
        var facings = new HashMap<Cell, Integer>();
        for (var c : List.of(new Cell(0, 0, 1), new Cell(0, 0, 2), new Cell(0, 0, 3), new Cell(1, 0, 2), new Cell(1, 0, 3))) facings.put(c, NORTH);
        for (int x = 0; x <= 3; x++) facings.put(new Cell(x, 0, 0), EAST);
        for (int x = 1; x <= 3; x++) facings.put(new Cell(x, 0, 1), EAST);
        var order = new ArrayList<>(facings.keySet());
        order.sort((a, b) -> a.z() != b.z() ? a.z() - b.z() : a.x() - b.x());
        var random = new Random(7);
        for (int trial = 0; trial < 12; trial++) {
            var world = place(order, facings);
            for (var cell : world.keySet()) {
                boolean corner = cell.equals(new Cell(0, 0, 0)) || cell.equals(new Cell(1, 0, 1));
                assertEquals(corner ? RackGeometry.FRONT_RIGHT : RackGeometry.STRAIGHT, world.get(cell).corner(), cell + " in order " + order);
            }
            Collections.shuffle(order, random);
        }
    }

    @Test
    void aStackedRackCarriesThePostsUpToTheRackOnIt() {
        var facings = Map.of(new Cell(0, 0, 0), EAST, new Cell(0, 1, 0), EAST);
        var world = place(List.of(new Cell(0, 0, 0), new Cell(0, 1, 0)), facings);
        int lower = steel(world, new Cell(0, 0, 0)), upper = steel(world, new Cell(0, 1, 0));
        assertEquals(4, lower & 4, "stacked");
        assertEquals(0, upper & 4, "nothing on the upper rack");
        double top = RackGeometry.members(RackGeometry.framed(world.get(new Cell(0, 0, 0)), lower, RackGeometry.BEAM_TOP)).stream().mapToDouble(box -> box[4]).max().orElseThrow();
        assertEquals(1, top, 1e-9, "the lower rack's posts reach the upper rack");
    }
}
