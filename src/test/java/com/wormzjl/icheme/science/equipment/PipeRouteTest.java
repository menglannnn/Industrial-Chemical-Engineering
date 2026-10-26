package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.PipeRoute.Cell;
import com.wormzjl.icheme.science.equipment.PipeRoute.End;
import com.wormzjl.icheme.science.equipment.PipeRoute.Refusal;
import com.wormzjl.icheme.science.equipment.PipeRoute.Room;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The laying tool's route search and the server's route check (batch 2026-10-26-pipe-laying-system, plan 4.3 and 4.4, RACK_LAYERS_PLAN R3). */
class PipeRouteTest {
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final Cell O = new Cell(0, 0, 0);
    /** A 2" line, one rack position wide; a 6" line; a 14" line, two positions. */
    private static final double SMALL = .0625, SIX = .171875, WIDE = .359375;

    private static PipeRoute.Space without(Set<Cell> blocked) {
        return PipeRoute.Space.of(cell -> blocked.contains(cell) ? Room.BLOCKED : Room.FREE, List.of());
    }

    private static PipeRoute.Space with(Map<Cell, Room> rooms) {
        return PipeRoute.Space.of(cell -> rooms.getOrDefault(cell, Room.FREE), rooms.keySet());
    }

    private static PipeRoute.Result search(End start, int heading, End end, PipeRoute.Space space) {
        return PipeRoute.search(start, heading, end, SMALL, space, PipeRoute.Limits.DEFAULT);
    }

    private static List<Cell> line(int length) {
        var cells = new ArrayList<Cell>();
        for (int x = 0; x < length; x++) cells.add(new Cell(x, 0, 0));
        return cells;
    }

    private static List<Integer> plain(int length) {
        return Collections.nCopies(length, -1);
    }

    @Test
    void aClearRunIsStraightAndItsCellsJoinEachOther() {
        var end = new End(new Cell(5, 0, 0), -1);
        var route = search(new End(O, -1), EAST, end, without(Set.of()));
        assertTrue(route.found());
        assertEquals(line(6), route.cells());
        assertEquals(plain(6), route.options());
        assertEquals(0, PipeRoute.elbows(route.cells(), new End(O, -1), end));
        var joints = PipeRoute.laidJoints(route.cells(), new End(O, -1), end);
        assertEquals(1 << EAST, joints[0], "an open start: one joint, drawn to a blind flange");
        assertEquals(1 << WEST | 1 << EAST, joints[3]);
        assertEquals(1 << WEST, joints[5]);
        assertArrayEquals(new int[]{EAST, EAST, EAST, EAST, EAST, EAST}, PipeRoute.facings(route.cells(), EAST));
        assertArrayEquals(new int[]{1 << WEST | 1 << EAST, 1 << WEST | 1 << EAST}, java.util.Arrays.copyOf(PipeRoute.arms(route.cells(), new End(O, -1), EAST, end), 2),
                "an open start runs on from the face behind its heading");
    }

    @Test
    void theClickedFaceSteersTheFirstLegAndTiesAreDeterministic() {
        var end = new End(new Cell(3, 0, 3), -1);
        var route = search(new End(O, -1), EAST, end, without(Set.of()));
        assertEquals(new Cell(3, 0, 0), route.cells().get(3), "east first, along the clicked face, then one elbow south");
        assertEquals(1, PipeRoute.elbows(route.cells(), new End(O, -1), end));
        assertEquals(route, search(new End(O, -1), EAST, end, without(Set.of())));
        var south = search(new End(O, -1), SOUTH, end, without(Set.of()));
        assertEquals(new Cell(0, 0, 3), south.cells().get(3));
    }

    @Test
    void aRouteGoesRoundAnObstacleAndStaysLevelWhenItCan() {
        var end = new End(new Cell(6, 0, 0), -1);
        var route = search(new End(O, -1), EAST, end, without(Set.of(new Cell(3, 0, 0))));
        assertTrue(route.found());
        assertEquals(9, route.cells().size(), "two cells longer than straight");
        assertFalse(route.cells().contains(new Cell(3, 0, 0)));
        assertTrue(route.cells().stream().allMatch(c -> c.y() == 0), "a vertical step costs more than a horizontal one");
        assertNull(PipeRoute.check(route.cells(), route.options(), new End(O, -1), end, 128));
    }

    @Test
    void anEndJointedToADeviceArrivesThroughAnotherFace() {
        // The end cell meets a tank to its north: the line ends in an elbow into it, or runs straight into it.
        var start = new End(O, WEST);
        var end = new End(new Cell(4, 0, 0), NORTH);
        var route = search(start, EAST, end, without(Set.of()));
        assertEquals(line(5), route.cells());
        var joints = PipeRoute.laidJoints(route.cells(), start, end);
        assertEquals(1 << WEST | 1 << EAST, joints[0], "the start cell joins the device behind it");
        assertEquals(1 << WEST | 1 << NORTH, joints[4]);
        assertEquals(1, PipeRoute.elbows(route.cells(), start, end));
        var straight = new End(new Cell(4, 0, 0), EAST);
        assertEquals(0, PipeRoute.elbows(search(start, EAST, straight, without(Set.of())).cells(), start, straight));
    }

    @Test
    void aOneCellLineJoinsBothEnds() {
        var start = new End(O, WEST);
        var route = search(start, EAST, new End(O, UP), without(Set.of()));
        assertEquals(List.of(O), route.cells());
        assertArrayEquals(new int[]{1 << WEST | 1 << UP}, PipeRoute.laidJoints(route.cells(), start, new End(O, UP)));
        assertEquals(Refusal.SAME_FACE, search(start, EAST, new End(O, WEST), without(Set.of())).refusal());
    }

    @Test
    void refusalsNameTheirReason() {
        var free = without(Set.of());
        assertEquals(Refusal.START_OCCUPIED, search(new End(O, -1), EAST, new End(new Cell(4, 0, 0), -1), without(Set.of(O))).refusal());
        assertEquals(Refusal.END_OCCUPIED, search(new End(O, -1), EAST, new End(new Cell(4, 0, 0), -1), without(Set.of(new Cell(4, 0, 0)))).refusal());
        var target = new Cell(4, 0, 0);
        var walls = new java.util.HashSet<Cell>();
        for (int face = 0; face < 6; face++) walls.add(target.step(face));
        // A small box: the search tries every cell and heading in it before it calls the end unreachable.
        assertEquals(Refusal.BLOCKED, PipeRoute.search(new End(O, -1), EAST, new End(target, -1), SMALL, without(walls), new PipeRoute.Limits(128, 32768, 2)).refusal());
        assertEquals(Refusal.TOO_LONG, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(10, 0, 0), -1), SMALL, free, new PipeRoute.Limits(5, 32768, 8)).refusal());
        assertEquals(Refusal.TOO_LONG, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(10, 0, 0), -1), SMALL, without(Set.of(new Cell(5, 0, 0))), new PipeRoute.Limits(128, 3, 8)).refusal());
        assertThrows(IllegalArgumentException.class, () -> search(new End(O, WEST), NORTH, new End(target, -1), free), "a jointed start is entered from its joint");
    }

    @Test
    void theServerRejectsATamperedRoute() {
        var start = new End(O, -1);
        var end = new End(new Cell(3, 0, 0), -1);
        assertNull(PipeRoute.check(line(4), plain(4), start, end, 128));
        assertEquals(Refusal.TOO_LONG, PipeRoute.check(line(4), plain(4), start, end, 3));
        assertEquals(Refusal.TOO_LONG, PipeRoute.check(List.of(), List.of(), start, end, 128));
        assertEquals(Refusal.BROKEN, PipeRoute.check(List.of(O, new Cell(2, 0, 0), new Cell(3, 0, 0)), plain(3), start, end, 128), "a gap");
        assertEquals(Refusal.BROKEN, PipeRoute.check(List.of(O, new Cell(1, 0, 0), O, new Cell(1, 0, 0), new Cell(2, 0, 0), new Cell(3, 0, 0)), plain(6), start, end, 128), "a cell twice");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), plain(4), start, new End(new Cell(4, 0, 0), -1), 128), "not ending at the end");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), plain(3), start, end, 128), "an option missing");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), List.of(-1, RackGeometry.OPTIONS, 2, -1), start, end, 128), "no such option");
        assertEquals(Refusal.JOINTS, PipeRoute.check(line(4), plain(4), new End(O, EAST), end, 128), "the start joint on the face the line leaves by");
        assertEquals(Refusal.JOINTS, PipeRoute.check(line(4), plain(4), start, new End(new Cell(3, 0, 0), WEST), 128));
    }

    // ---- racks ---------------------------------------------------------------------------------------------------------

    private static Room rack(int facing, int corner, RackGeometry.Lane... lanes) {
        int faces = 0;
        for (var lane : lanes) faces |= lane.joined();
        return Room.rack(new RackGeometry.Rack(facing, corner), List.of(lanes), faces);
    }

    /** A lane on {@code position}, {@code width} wide, of outside {@code outside}, with these arms, all of them joints. */
    private static RackGeometry.Lane lane(int position, int width, double outside, int arms) {
        return new RackGeometry.Lane(RackGeometry.option(position, RackGeometry.TIER), width, arms, arms, PipeSizes.code(outside));
    }

    /**
     * Racks facing east along x from {@code from} to {@code to} at z {@code z}, with these lanes in every cell, open at the
     * run's two outer ends: a lane jointed there would have a pipe in the cell beyond.
     */
    private static Map<Cell, Room> row(int from, int to, int z, RackGeometry.Lane... lanes) {
        var rooms = new HashMap<Cell, Room>();
        for (int x = from; x <= to; x++) {
            int outer = (x == from ? 1 << WEST : 0) | (x == to ? 1 << EAST : 0);
            var cell = new RackGeometry.Lane[lanes.length];
            for (int i = 0; i < lanes.length; i++) cell[i] = new RackGeometry.Lane(lanes[i].option(), lanes[i].width(), lanes[i].arms(), lanes[i].joined() & ~outer, lanes[i].outside());
            rooms.put(new Cell(x, 0, z), rack(EAST, RackGeometry.STRAIGHT, cell));
        }
        return rooms;
    }

    /** A rack run from x 1 to {@code length}, one block south of the straight line. */
    private static Map<Cell, Room> run(int length, RackGeometry.Lane... lanes) {
        return row(1, length, 1, lanes);
    }

    private static List<Integer> lanes(PipeRoute.Result route) {
        return route.options().stream().filter(option -> option >= 0).toList();
    }

    @Test
    void aLongRouteFollowsARackOnTheSideItLeavesTowards() {
        var end = new End(new Cell(22, 0, 0), -1);
        var route = search(new End(O, -1), EAST, end, with(run(21)));
        assertTrue(route.found());
        assertTrue(lanes(route).size() >= 15, "the route runs along the rack: " + route.cells());
        for (int i = 0; i < route.cells().size(); i++)
            assertEquals(route.cells().get(i).z() == 1 && route.cells().get(i).x() >= 1 && route.cells().get(i).x() <= 21, route.options().get(i) >= 0, "a lane in every rack cell, none elsewhere");
        // The line leaves to the north, the rack's left as it faces east: position 0, flat.
        assertTrue(lanes(route).stream().allMatch(option -> option == RackGeometry.option(0, RackGeometry.TIER)), "position 0 along the run: " + route.options());
        assertNull(PipeRoute.check(route.cells(), route.options(), new End(O, -1), end, 128));
        assertEquals(-1, PipeRoute.blocked(route.cells(), route.options(), new End(O, -1), EAST, end, SMALL, with(run(21))));
        var wide = PipeRoute.search(new End(O, -1), EAST, end, WIDE, with(run(21, lane(0, 1, SMALL, 1 << WEST | 1 << EAST))), PipeRoute.Limits.DEFAULT);
        assertEquals(RackGeometry.option(1, RackGeometry.TIER), lanes(wide).getFirst(), "a wide line beside the lane on position 0: positions 1 and 2");
    }

    @Test
    void aLineOverTheMaximumWidthIsNotLaidOnRacks() {
        var end = new End(new Cell(22, 0, 0), -1);
        var big = PipeRoute.search(new End(O, -1), EAST, end, PipeSizes.MAX_OUTSIDE, with(run(21)), PipeRoute.Limits.DEFAULT);
        assertTrue(big.found() && lanes(big).isEmpty(), "a 32 inch line runs beside the rack");
        var lowered = PipeRoute.search(new End(O, -1), EAST, end, .7, with(run(21)), PipeRoute.Limits.DEFAULT);
        assertTrue(lanes(lowered).size() >= 15 && lanes(lowered).stream().allMatch(option -> option == 0), "a 28 inch line fills the tier on its lowered beam");
    }

    @Test
    void aFullRackIsPassedByAndALaneMayNotShareAFaceWithAnother() {
        var end = new End(new Cell(22, 0, 0), -1);
        int along = 1 << WEST | 1 << EAST;
        var full = search(new End(O, -1), EAST, end, with(run(21, lane(0, 1, SIX, along), lane(1, 1, SIX, along), lane(2, 1, SIX, along), lane(3, 1, SIX, along))));
        assertTrue(full.found() && lanes(full).isEmpty(), "every position taken: the line runs beside the rack");
        // Every rack cell but the run's ends has a lane on position 0 jointed north, towards the straight line's cells: the
        // line still follows the rack on the other positions, but enters and leaves it only through other faces.
        var rooms = run(21);
        rooms.putAll(row(2, 20, 1, lane(0, 1, SIX, 1 << WEST | 1 << NORTH)));
        var pointed = search(new End(O, -1), EAST, end, with(rooms));
        assertTrue(lanes(pointed).size() >= 15, "the rack is still followed: " + pointed.cells());
        for (int i = 1; i < pointed.cells().size(); i++) {
            boolean in = pointed.options().get(i) >= 0, before = pointed.options().get(i - 1) >= 0;
            if (in == before) continue;
            var rackCell = in ? pointed.cells().get(i) : pointed.cells().get(i - 1);
            var plainCell = in ? pointed.cells().get(i - 1) : pointed.cells().get(i);
            assertNotEquals(rackCell.step(NORTH), plainCell, "entered or left through a face another lane already uses");
        }
    }

    @Test
    void racksInLineJoinWhateverTheirFacingAndTheLineKeepsItsPlace() {
        // Facings alternate east and west along x: a lane's option flips with the facing, its place in the world stays.
        var rooms = new HashMap<Cell, Room>();
        for (int x = 1; x <= 10; x++) rooms.put(new Cell(x, 0, 0), rack(x % 2 == 0 ? WEST : EAST, RackGeometry.STRAIGHT));
        var end = new End(new Cell(11, 0, 0), -1);
        var route = search(new End(O, -1), EAST, end, with(rooms));
        var options = route.options().subList(1, 11);
        assertTrue(options.stream().allMatch(option -> option >= 0), "the whole run: " + route.options());
        for (int i = 1; i < options.size(); i++) assertEquals(RackGeometry.POSITIONS - 1 - RackGeometry.position(options.get(i - 1)), RackGeometry.position(options.get(i)));
        assertEquals(-1, PipeRoute.blocked(route.cells(), route.options(), new End(O, -1), EAST, end, SMALL, with(rooms)));
        var shifted = new ArrayList<>(route.options());
        shifted.set(3, RackGeometry.option((RackGeometry.position(shifted.get(3)) + 1) % 4, RackGeometry.TIER));
        assertTrue(PipeRoute.blocked(route.cells(), shifted, new End(O, -1), EAST, end, SMALL, with(rooms)) >= 0, "a lane that does not meet the next at the shared face");
    }

    @Test
    void aCornerTurnsTheRunAndKeepsTheOrder() {
        // A run facing north from z 5 to z 1, its end block at z 0 a corner to the right (east), into a run facing east.
        var rooms = new HashMap<Cell, Room>();
        for (int z = 1; z <= 5; z++) rooms.put(new Cell(0, 0, z), rack(NORTH, RackGeometry.STRAIGHT));
        rooms.put(O, rack(NORTH, RackGeometry.BACK_RIGHT));
        for (int x = 1; x <= 5; x++) rooms.put(new Cell(x, 0, 0), rack(EAST, RackGeometry.STRAIGHT));
        var start = new End(new Cell(0, 0, 6), -1);
        var end = new End(new Cell(6, 0, 0), -1);
        var route = search(start, NORTH, end, with(rooms));
        assertTrue(route.found());
        int at = route.cells().indexOf(O);
        assertTrue(at > 0 && route.options().get(at) >= 0, "through the corner: " + route.cells());
        int p = RackGeometry.position(route.options().get(at));
        assertEquals(p, RackGeometry.position(route.options().get(at - 1)));
        assertEquals(p, RackGeometry.position(route.options().get(at + 1)), "the same position on both runs");
        assertEquals(-1, PipeRoute.blocked(route.cells(), route.options(), start, NORTH, end, SMALL, with(rooms)));
    }

    @Test
    void aLineLeavesOverTheTierWhereALaneLiesBetweenItAndItsSide() {
        // A rack facing east along x at z 0; a straight lane on position 0, the north (left) edge. A line from the west end
        // to a point north of the rack takes position 1 and, with position 0 between it and the north, leaves over the tier.
        var rooms = row(0, 5, 0, lane(0, 1, SIX, 1 << WEST | 1 << EAST));
        var start = new End(new Cell(-1, 0, 0), -1);
        var end = new End(new Cell(3, 0, -3), -1);
        var route = search(start, EAST, end, with(rooms));
        var options = lanes(route);
        assertFalse(options.isEmpty(), "the line uses the rack: " + route.cells());
        assertEquals(RackGeometry.option(1, RackGeometry.ABOVE), options.getLast(), "position 1, out over the tier: " + route.options());
        assertEquals(RackGeometry.option(0, RackGeometry.TIER), lanes(search(start, EAST, end, with(row(0, 5, 0)))).getLast(), "with nothing between, position 0 leaves flat");
    }

    @Test
    void aLineCrossesARackBesideOnItsCrossingLevel() {
        // Two racks facing east side by side, z 0 (north) and z 1 (south), x 0 to 5; the north rack's tier is full. A line
        // from the south rack's west end to a point north of both leaves the south rack over its tier and crosses the
        // north rack at the same level.
        int along = 1 << WEST | 1 << EAST;
        var rooms = row(0, 5, 0, lane(0, 1, SIX, along), lane(1, 1, SIX, along), lane(2, 1, SIX, along), lane(3, 1, SIX, along));
        rooms.putAll(row(0, 5, 1));
        var start = new End(new Cell(-1, 0, 1), -1);
        var end = new End(new Cell(3, 0, -2), -1);
        var route = search(start, EAST, end, with(rooms));
        assertTrue(route.found());
        int crossing = -1;
        for (int i = 0; i < route.cells().size(); i++) if (route.cells().get(i).z() == 0 && route.options().get(i) >= 0) crossing = i;
        assertTrue(crossing > 0, "the line crosses the north rack: " + route.cells() + " " + route.options());
        int option = route.options().get(crossing);
        assertFalse(RackGeometry.tier(option), "as a crossing lane");
        assertEquals(RackGeometry.level(option), RackGeometry.level(route.options().get(crossing - 1)), "at the level it left the south rack at");
        assertEquals(-1, PipeRoute.blocked(route.cells(), route.options(), start, EAST, end, SMALL, with(rooms)));
    }

    @Test
    void anExtendedLaneKeepsItsPointAndAnEmptyRackTakesAnyFree() {
        var rooms = run(10, lane(1, 1, SMALL, 1 << WEST | 1 << EAST));
        var rackOf = new RackGeometry.Rack(EAST, RackGeometry.STRAIGHT);
        // Start in the rack's first cell, continuing a lane on position 3 through the west face; end at the far cell.
        int point = RackGeometry.point(rackOf, RackGeometry.option(3, RackGeometry.TIER), 1, SMALL, WEST);
        var route = search(new End(new Cell(1, 0, 1), WEST, point), EAST, new End(new Cell(10, 0, 1), -1), with(rooms));
        assertEquals(10, route.cells().size());
        assertTrue(route.options().stream().allMatch(option -> option == RackGeometry.option(3, RackGeometry.TIER)), route.options().toString());
        int taken = RackGeometry.point(rackOf, RackGeometry.option(1, RackGeometry.TIER), 1, SMALL, WEST);
        assertEquals(Refusal.START_OCCUPIED, search(new End(new Cell(1, 0, 1), WEST, taken), EAST, new End(new Cell(10, 0, 1), -1), with(rooms)).refusal(), "position 1 is taken");
    }

    @Test
    void anOpenRouteCostsTheSearchAboutOneNodePerBlock() {
        // Far from racks the estimate is the full distance, so the search runs straight to the goal and turns twice.
        var end = new End(new Cell(40, 10, 40), -1);
        var route = PipeRoute.search(new End(O, -1), EAST, end, SMALL, without(Set.of()), new PipeRoute.Limits(128, 4 * 91, 8));
        assertTrue(route.found(), "a 91-block route within four expansions per block: " + route.refusal());
        assertEquals(91, route.cells().size());
        assertEquals(2, PipeRoute.elbows(route.cells(), new End(O, -1), end));
        // Beside a rack run the estimate falls to what the rack allows. The open options are one state, not one each, so
        // the search walks the run once.
        var beside = PipeRoute.search(new End(O, -1), EAST, new End(new Cell(22, 0, 0), -1), SMALL, with(run(21)), new PipeRoute.Limits(128, 2 * 23, 8));
        assertTrue(beside.found() && !lanes(beside).isEmpty(), "the rack route within two expansions per block: " + beside.refusal());
    }

    @Test
    void theServerChecksEachCellAgainstTheWorldAsTheSearchDoes() {
        var end = new End(new Cell(22, 0, 0), -1);
        var rooms = run(21, lane(1, 1, SMALL, 1 << WEST | 1 << EAST));
        var start = new End(O, -1);
        var route = search(start, EAST, end, with(rooms));
        assertEquals(-1, PipeRoute.blocked(route.cells(), route.options(), start, EAST, end, SMALL, with(rooms)), "a searched route passes");
        int first = route.options().indexOf(route.options().stream().filter(o -> o >= 0).findFirst().orElseThrow());
        var taken = new ArrayList<>(route.options());
        for (int i = 0; i < taken.size(); i++) if (taken.get(i) >= 0) taken.set(i, RackGeometry.option(1, RackGeometry.TIER));
        assertEquals(first, PipeRoute.blocked(route.cells(), taken, start, EAST, end, SMALL, with(rooms)), "a lane on a taken position");
        var plain = new ArrayList<>(route.options());
        plain.set(first, -1);
        assertEquals(first, PipeRoute.blocked(route.cells(), plain, start, EAST, end, SMALL, with(rooms)), "a plain pipe in a rack cell");
        assertEquals(0, PipeRoute.blocked(line(3), plain(3), start, EAST, new End(new Cell(2, 0, 0), -1), SMALL, without(Set.of(O))), "a blocked cell");
        assertEquals(1, PipeRoute.blocked(line(3), List.of(-1, 0, -1), start, EAST, new End(new Cell(2, 0, 0), -1), SMALL, without(Set.of())), "a lane outside a rack");
        // A rack at x 1 facing east whose lane already has a joint west, towards x 0: the line may not pass in beside it.
        var pointed = Map.of(new Cell(1, 0, 0), rack(EAST, RackGeometry.STRAIGHT, lane(3, 1, SMALL, 1 << WEST | 1 << EAST)));
        var through = List.of(-1, RackGeometry.option(0, RackGeometry.TIER), -1);
        assertEquals(1, PipeRoute.blocked(line(3), through, start, EAST, new End(new Cell(2, 0, 0), -1), SMALL, with(pointed)));
        // A lane on position 3 out to the right (south), open at its east end: it has no joint on the faces the line passes.
        var beside = Map.of(new Cell(1, 0, 0), rack(EAST, RackGeometry.STRAIGHT,
                new RackGeometry.Lane(RackGeometry.option(3, RackGeometry.TIER), 1, 1 << SOUTH | 1 << EAST, 1 << SOUTH, PipeSizes.code(SMALL))));
        assertEquals(-1, PipeRoute.blocked(line(3), through, start, EAST, new End(new Cell(2, 0, 0), -1), SMALL, with(beside)));
    }
}
