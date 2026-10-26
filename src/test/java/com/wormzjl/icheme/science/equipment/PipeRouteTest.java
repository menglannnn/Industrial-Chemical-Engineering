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

/** The laying tool's route search and the server's route check (batch 2026-10-26-pipe-laying-system, plan 4.3 and 4.4). */
class PipeRouteTest {
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final Cell O = new Cell(0, 0, 0);

    private static PipeRoute.Space without(Set<Cell> blocked) {
        return PipeRoute.Space.of(cell -> blocked.contains(cell) ? Room.BLOCKED : Room.FREE, List.of());
    }

    private static PipeRoute.Space with(Map<Cell, Room> rooms) {
        return PipeRoute.Space.of(cell -> rooms.getOrDefault(cell, Room.FREE), rooms.keySet());
    }

    private static PipeRoute.Result search(End start, int heading, End end, PipeRoute.Space space) {
        return PipeRoute.search(start, heading, end, 1, space, PipeRoute.Limits.DEFAULT);
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
        assertEquals(plain(6), route.slots());
        assertEquals(0, PipeRoute.elbows(route.cells(), new End(O, -1), end));
        var joints = PipeRoute.laidJoints(route.cells(), new End(O, -1), end);
        assertEquals(1 << EAST, joints[0], "an open start: one joint, drawn to a blind flange");
        assertEquals(1 << WEST | 1 << EAST, joints[3]);
        assertEquals(1 << WEST, joints[5]);
        assertArrayEquals(new int[]{EAST, EAST, EAST, EAST, EAST, EAST}, PipeRoute.facings(route.cells(), EAST));
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
        assertNull(PipeRoute.check(route.cells(), route.slots(), new End(O, -1), end, 1, 128));
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
        assertEquals(Refusal.BLOCKED, PipeRoute.search(new End(O, -1), EAST, new End(target, -1), 1, without(walls), new PipeRoute.Limits(128, 32768, 2)).refusal());
        assertEquals(Refusal.TOO_LONG, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(10, 0, 0), -1), 1, free, new PipeRoute.Limits(5, 32768, 8)).refusal());
        assertEquals(Refusal.TOO_LONG, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(10, 0, 0), -1), 1, without(Set.of(new Cell(5, 0, 0))), new PipeRoute.Limits(128, 3, 8)).refusal());
        assertThrows(IllegalArgumentException.class, () -> search(new End(O, WEST), NORTH, new End(target, -1), free), "a jointed start is entered from its joint");
    }

    @Test
    void theServerRejectsATamperedRoute() {
        var start = new End(O, -1);
        var end = new End(new Cell(3, 0, 0), -1);
        assertNull(PipeRoute.check(line(4), plain(4), start, end, 1, 128));
        assertEquals(Refusal.TOO_LONG, PipeRoute.check(line(4), plain(4), start, end, 1, 3));
        assertEquals(Refusal.TOO_LONG, PipeRoute.check(List.of(), List.of(), start, end, 1, 128));
        assertEquals(Refusal.BROKEN, PipeRoute.check(List.of(O, new Cell(2, 0, 0), new Cell(3, 0, 0)), plain(3), start, end, 1, 128), "a gap");
        assertEquals(Refusal.BROKEN, PipeRoute.check(List.of(O, new Cell(1, 0, 0), O, new Cell(1, 0, 0), new Cell(2, 0, 0), new Cell(3, 0, 0)), plain(6), start, end, 1, 128), "a cell twice");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), plain(4), start, new End(new Cell(4, 0, 0), -1), 1, 128), "not ending at the end");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), plain(3), start, end, 1, 128), "a slot missing");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), List.of(-1, 1, 2, -1), start, end, 1, 128), "a lane jumping slots");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), List.of(-1, 4, 4, -1), start, end, 2, 128), "a range past the fifth slot");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), plain(4), start, new End(new Cell(3, 0, 0), -1, 2), 1, 128), "not the slot the end keeps");
        assertEquals(Refusal.JOINTS, PipeRoute.check(line(4), plain(4), new End(O, EAST), end, 1, 128), "the start joint on the face the line leaves by");
        assertEquals(Refusal.JOINTS, PipeRoute.check(line(4), plain(4), start, new End(new Cell(3, 0, 0), WEST), 1, 128));
    }

    /** A rack along x from x 1 to {@code length}, one block south of the straight line, with these slots used. */
    private static Map<Cell, Room> rack(int length, int used, int faces) {
        var rooms = new HashMap<Cell, Room>();
        for (int x = 1; x <= length; x++) rooms.put(new Cell(x, 0, 1), Room.rack(used, faces, 0));
        return rooms;
    }

    @Test
    void aLongRouteFollowsARackAndKeepsItsSlot() {
        assertArrayEquals(new int[]{1, 1, 2, 2, 0, 0}, new int[]{PipeRoute.axis(DOWN), PipeRoute.axis(UP), PipeRoute.axis(NORTH), PipeRoute.axis(SOUTH), PipeRoute.axis(WEST), PipeRoute.axis(EAST)},
                "a rack's axis counts x, y, z as Direction.Axis does");
        var end = new End(new Cell(22, 0, 0), -1);
        var route = search(new End(O, -1), EAST, end, with(rack(21, 0, 0)));
        assertTrue(route.found());
        var lanes = new ArrayList<Integer>();
        for (int i = 0; i < route.cells().size(); i++) if (route.slots().get(i) >= 0) lanes.add(route.slots().get(i));
        assertTrue(lanes.size() >= 15, "the route runs along the rack: " + route.cells());
        assertTrue(lanes.stream().allMatch(slot -> slot == 0), "one slot along the run, the first free one");
        for (int i = 0; i < route.cells().size(); i++)
            assertEquals(route.cells().get(i).z() == 1 && route.cells().get(i).x() >= 1 && route.cells().get(i).x() <= 21, route.slots().get(i) >= 0, "a lane in every rack cell, none elsewhere");
        assertNull(PipeRoute.check(route.cells(), route.slots(), new End(O, -1), end, 1, 128));
        var wide = PipeRoute.search(new End(O, -1), EAST, end, 2, with(rack(21, 1, 0)), PipeRoute.Limits.DEFAULT);
        assertEquals(3, wide.slots().stream().filter(slot -> slot >= 0).findFirst().orElseThrow(), "a wide line takes the free outer range, slots 3 and 4");
    }

    @Test
    void aFullRackIsPassedByAndALaneMayNotShareAFaceWithAnother() {
        var end = new End(new Cell(22, 0, 0), -1);
        var full = search(new End(O, -1), EAST, end, with(rack(21, 31, 0)));
        assertTrue(full.found() && full.slots().stream().allMatch(slot -> slot < 0), "every slot used: the line runs beside the rack");
        // Every rack cell already has a lane jointed north, towards the straight line's cells: the line still follows the
        // rack, but enters and leaves it only through other faces (here its ends).
        var pointed = search(new End(O, -1), EAST, end, with(rack(21, 1, 1 << NORTH)));
        assertTrue(pointed.slots().stream().filter(slot -> slot >= 0).count() >= 15, "the rack is still followed: " + pointed.cells());
        for (int i = 1; i < pointed.cells().size(); i++) {
            boolean in = pointed.slots().get(i) >= 0, before = pointed.slots().get(i - 1) >= 0;
            if (in == before) continue;
            var rackCell = in ? pointed.cells().get(i) : pointed.cells().get(i - 1);
            var plainCell = in ? pointed.cells().get(i - 1) : pointed.cells().get(i);
            assertNotEquals(rackCell.step(NORTH), plainCell, "entered or left through a face another lane already uses");
        }
    }

    @Test
    void anExtendedLaneKeepsItsSlotAndAnEmptyRackTakesAnyFree() {
        var rooms = rack(10, 1 << 1, 0);
        // Start inside the rack on slot 3 (an extension of a lane there), end at the rack's far cell on any free slot.
        var route = search(new End(new Cell(1, 0, 1), -1, 3), EAST, new End(new Cell(10, 0, 1), -1), with(rooms));
        assertEquals(10, route.cells().size());
        assertTrue(route.slots().stream().allMatch(slot -> slot == 3));
        assertEquals(Refusal.START_OCCUPIED, search(new End(new Cell(1, 0, 1), -1, 1), EAST, new End(new Cell(10, 0, 1), -1), with(rooms)).refusal(), "slot 1 is used");
    }

    @Test
    void anOpenRouteCostsTheSearchAboutOneNodePerBlock() {
        // Far from racks the estimate is the full distance, so the search runs straight to the goal and turns twice.
        var end = new End(new Cell(40, 10, 40), -1);
        var route = PipeRoute.search(new End(O, -1), EAST, end, 1, without(Set.of()), new PipeRoute.Limits(128, 4 * 91, 8));
        assertTrue(route.found(), "a 91-block route within four expansions per block: " + route.refusal());
        assertEquals(91, route.cells().size());
        assertEquals(2, PipeRoute.elbows(route.cells(), new End(O, -1), end));
        // Beside a rack run the estimate falls to what the rack allows. Every free slot is an equal choice there, so the
        // search walks each one along the run: about one expansion per block and free slot.
        var beside = PipeRoute.search(new End(O, -1), EAST, new End(new Cell(22, 0, 0), -1), 1, with(rack(21, 0, 0)), new PipeRoute.Limits(128, 6 * 23, 8));
        assertTrue(beside.found() && beside.slots().stream().anyMatch(slot -> slot >= 0), "the rack route within six expansions per block: " + beside.refusal());
    }

    @Test
    void theServerChecksEachCellAgainstTheWorldAsTheSearchDoes() {
        var end = new End(new Cell(22, 0, 0), -1);
        var rooms = rack(21, 1 << 1, 0);
        var route = search(new End(O, -1), EAST, end, with(rooms));
        assertEquals(-1, PipeRoute.blocked(route.cells(), route.slots(), 1, with(rooms)), "a searched route passes");
        int first = route.slots().indexOf(0);
        var taken = new ArrayList<>(route.slots());
        taken.set(first, 1);
        assertEquals(first, PipeRoute.blocked(route.cells(), taken, 1, with(rooms)), "a lane on a used slot");
        var plain = new ArrayList<>(route.slots());
        plain.set(first, -1);
        assertEquals(first, PipeRoute.blocked(route.cells(), plain, 1, with(rooms)), "a plain pipe in a rack cell");
        assertEquals(0, PipeRoute.blocked(line(3), plain(3), 1, without(Set.of(O))), "a blocked cell");
        assertEquals(1, PipeRoute.blocked(line(3), List.of(-1, 0, -1), 1, without(Set.of())), "a lane outside a rack");
        // A rack at x 1 whose lanes already have a joint west, towards x 0: the line may not pass between them there.
        var pointed = Map.of(new Cell(1, 0, 0), Room.rack(1 << 4, 1 << WEST, 0));
        assertEquals(1, PipeRoute.blocked(line(3), List.of(-1, 0, -1), 1, with(pointed)));
        assertEquals(-1, PipeRoute.blocked(line(3), List.of(-1, 0, -1), 1, with(Map.of(new Cell(1, 0, 0), Room.rack(1 << 4, 1 << NORTH, 0)))));
    }
}
