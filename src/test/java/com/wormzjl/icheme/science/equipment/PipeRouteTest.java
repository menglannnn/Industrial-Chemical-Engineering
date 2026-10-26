package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.PipeRoute.Cell;
import com.wormzjl.icheme.science.equipment.PipeRoute.End;
import com.wormzjl.icheme.science.equipment.PipeRoute.Refusal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The laying tool's route search and the server's route check (batch 2026-10-26-pipe-laying-system, plan 4.3 and 4.4). */
class PipeRouteTest {
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final Cell O = new Cell(0, 0, 0);

    private static PipeRoute.Space without(Set<Cell> blocked) {
        return cell -> blocked.contains(cell) ? PipeRoute.Admit.BLOCKED : PipeRoute.Admit.FREE;
    }

    private static List<Cell> line(int length) {
        var cells = new ArrayList<Cell>();
        for (int x = 0; x < length; x++) cells.add(new Cell(x, 0, 0));
        return cells;
    }

    @Test
    void aClearRunIsStraightAndItsCellsJoinEachOther() {
        var end = new End(new Cell(5, 0, 0), -1);
        var route = PipeRoute.search(new End(O, -1), EAST, end, without(Set.of()), PipeRoute.Limits.DEFAULT);
        assertTrue(route.found());
        assertEquals(line(6), route.cells());
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
        var route = PipeRoute.search(new End(O, -1), EAST, end, without(Set.of()), PipeRoute.Limits.DEFAULT);
        assertEquals(new Cell(3, 0, 0), route.cells().get(3), "east first, along the clicked face, then one elbow south");
        assertEquals(1, PipeRoute.elbows(route.cells(), new End(O, -1), end));
        assertEquals(route, PipeRoute.search(new End(O, -1), EAST, end, without(Set.of()), PipeRoute.Limits.DEFAULT));
        var south = PipeRoute.search(new End(O, -1), SOUTH, end, without(Set.of()), PipeRoute.Limits.DEFAULT);
        assertEquals(new Cell(0, 0, 3), south.cells().get(3));
    }

    @Test
    void aRouteGoesRoundAnObstacleAndStaysLevelWhenItCan() {
        var end = new End(new Cell(6, 0, 0), -1);
        var route = PipeRoute.search(new End(O, -1), EAST, end, without(Set.of(new Cell(3, 0, 0))), PipeRoute.Limits.DEFAULT);
        assertTrue(route.found());
        assertEquals(9, route.cells().size(), "two cells longer than straight");
        assertFalse(route.cells().contains(new Cell(3, 0, 0)));
        assertTrue(route.cells().stream().allMatch(c -> c.y() == 0), "a vertical step costs more than a horizontal one");
        assertNull(PipeRoute.check(route.cells(), new End(O, -1), end, 128));
    }

    @Test
    void anEndJointedToADeviceArrivesThroughAnotherFace() {
        // The end cell meets a tank to its north: the line ends in an elbow into it, or runs straight into it.
        var start = new End(O, WEST);
        var end = new End(new Cell(4, 0, 0), NORTH);
        var route = PipeRoute.search(start, EAST, end, without(Set.of()), PipeRoute.Limits.DEFAULT);
        assertEquals(line(5), route.cells());
        var joints = PipeRoute.laidJoints(route.cells(), start, end);
        assertEquals(1 << WEST | 1 << EAST, joints[0], "the start cell joins the device behind it");
        assertEquals(1 << WEST | 1 << NORTH, joints[4]);
        assertEquals(1, PipeRoute.elbows(route.cells(), start, end));
        var straight = new End(new Cell(4, 0, 0), EAST);
        assertEquals(0, PipeRoute.elbows(PipeRoute.search(start, EAST, straight, without(Set.of()), PipeRoute.Limits.DEFAULT).cells(), start, straight));
    }

    @Test
    void aOneCellLineJoinsBothEnds() {
        var start = new End(O, WEST);
        var route = PipeRoute.search(start, EAST, new End(O, UP), without(Set.of()), PipeRoute.Limits.DEFAULT);
        assertEquals(List.of(O), route.cells());
        assertArrayEquals(new int[]{1 << WEST | 1 << UP}, PipeRoute.laidJoints(route.cells(), start, new End(O, UP)));
        assertEquals(Refusal.SAME_FACE, PipeRoute.search(start, EAST, new End(O, WEST), without(Set.of()), PipeRoute.Limits.DEFAULT).refusal());
    }

    @Test
    void refusalsNameTheirReason() {
        var free = without(Set.of());
        assertEquals(Refusal.START_OCCUPIED, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(4, 0, 0), -1), without(Set.of(O)), PipeRoute.Limits.DEFAULT).refusal());
        assertEquals(Refusal.END_OCCUPIED, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(4, 0, 0), -1), without(Set.of(new Cell(4, 0, 0))), PipeRoute.Limits.DEFAULT).refusal());
        var target = new Cell(4, 0, 0);
        var walls = new java.util.HashSet<Cell>();
        for (int face = 0; face < 6; face++) walls.add(target.step(face));
        // A small box: the search tries every cell and heading in it before it calls the end unreachable.
        assertEquals(Refusal.BLOCKED, PipeRoute.search(new End(O, -1), EAST, new End(target, -1), without(walls), new PipeRoute.Limits(128, 32768, 2)).refusal());
        assertEquals(Refusal.TOO_LONG, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(10, 0, 0), -1), free, new PipeRoute.Limits(5, 32768, 8)).refusal());
        assertEquals(Refusal.TOO_LONG, PipeRoute.search(new End(O, -1), EAST, new End(new Cell(10, 0, 0), -1), without(Set.of(new Cell(5, 0, 0))), new PipeRoute.Limits(128, 3, 8)).refusal());
        assertThrows(IllegalArgumentException.class, () -> PipeRoute.search(new End(O, WEST), NORTH, new End(target, -1), free, PipeRoute.Limits.DEFAULT),
                "a jointed start is entered from its joint");
    }

    @Test
    void theServerRejectsATamperedRoute() {
        var start = new End(O, -1);
        var end = new End(new Cell(3, 0, 0), -1);
        assertNull(PipeRoute.check(line(4), start, end, 128));
        assertEquals(Refusal.TOO_LONG, PipeRoute.check(line(4), start, end, 3));
        assertEquals(Refusal.TOO_LONG, PipeRoute.check(List.of(), start, end, 128));
        assertEquals(Refusal.BROKEN, PipeRoute.check(List.of(O, new Cell(2, 0, 0), new Cell(3, 0, 0)), start, end, 128), "a gap");
        assertEquals(Refusal.BROKEN, PipeRoute.check(List.of(O, new Cell(1, 0, 0), O, new Cell(1, 0, 0), new Cell(2, 0, 0), new Cell(3, 0, 0)), start, end, 128), "a cell twice");
        assertEquals(Refusal.BROKEN, PipeRoute.check(line(4), start, new End(new Cell(4, 0, 0), -1), 128), "not ending at the end");
        assertEquals(Refusal.JOINTS, PipeRoute.check(line(4), new End(O, EAST), end, 128), "the start joint on the face the line leaves by");
        assertEquals(Refusal.JOINTS, PipeRoute.check(line(4), start, new End(new Cell(3, 0, 0), WEST), 128));
        assertNull(PipeRoute.joints(line(4), new End(O, EAST), end));
    }
}
