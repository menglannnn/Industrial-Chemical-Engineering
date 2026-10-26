package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.wormzjl.icheme.science.equipment.Access.*;
import static com.wormzjl.icheme.science.equipment.ColumnStructure.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A column's access structure and nozzles (batch 2026-10-15-custom-equipment-rendering, photo survey section 2): where
 * the platforms, ladders, manways and nozzles stand, and what a player walks on and climbs.
 */
class ColumnStructureTest {
    /** A small column, a crude-like one with draws, pumparounds and steam, a slim one, and the widest on its tallest skirt. */
    private static final List<EquipmentShape> COLUMNS = List.of(
            EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 4),
            EquipmentShape.column(6, new ColumnTrays(40, 20, List.of(10, 25), List.of(41),
                    List.of(new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(30, 28))), Heading.NORTH, 5),
            EquipmentShape.column(1, ColumnTrays.simple(20, 10), Heading.EAST, 3),
            EquipmentShape.column(15, ColumnTrays.simple(30, 15), Heading.WEST, 16));

    @Test
    void platformsStandUnderTheManwaysWithinAClimbOfEachOther() {
        for (var shape : COLUMNS) {
            var plan = ColumnStructure.plan(shape);
            var platforms = plan.platforms();
            int previous = 0;
            for (var p : platforms) {
                assertTrue(p.level() >= 1 && p.level() - previous <= Access.MAX_CLIMB, shape + " climb to " + p.level());
                if (previous > 0) assertTrue(p.level() - previous >= MIN_PITCH, shape + " platforms at " + previous + " and " + p.level());
                previous = p.level();
                if (p.manway()) {
                    assertTrue(p.manwayHeight() - COVER / 2 > shape.bottomTangent() && p.manwayHeight() + COVER / 2 < shape.topTangent(), shape + " manway on the shell");
                    assertEquals(MANWAY_HEIGHT, p.manwayHeight() - p.level() - DECK, 1e-12);
                }
                if (p == platforms.getLast()) continue;
                // A part ring over the back quarter, between the two ladder positions.
                assertFalse(p.ring());
                assertEquals(Math.PI / 2, p.to() - p.from(), 1e-12, shape.toString());
                assertTrue(p.from() >= LADDER_B - ColumnStructure.clearance(shape) - 1e-12 && p.to() <= LADDER_A + ColumnStructure.clearance(shape) + 1e-12, shape + " " + platforms);
            }
            assertTrue(platforms.getLast().ring() && platforms.getLast().manway(), shape + ": a full ring with the top manway");
            assertTrue(platforms.stream().filter(Platform::manway).count() >= 2, shape + ": top and bottom manways");
            // The lifting davit stands on the top ring and rises over the top head.
            assertTrue(plan.davitTop() > shape.top() + 1 && plan.davitTop() <= shape.sizeY());
        }
        // The tall skirt: a rest platform under the bottom manway, which carries none.
        var tall = ColumnStructure.plan(COLUMNS.get(3));
        assertFalse(tall.platforms().getFirst().manway(), "a rest platform on a 16 m skirt");
    }

    @Test
    void decksAreWalkableAndTheirEdgesFenced() {
        int s = ShapeRasterizer.SOLID_RESOLUTION;
        for (var shape : COLUMNS) {
            var plan = ColumnStructure.plan(shape);
            double inner = shape.outerRadius() + GAP, edge = plan.deckRadius();
            for (var p : plan.platforms()) {
                double a = p.ring() ? p.from() + Math.PI : (p.from() + p.to()) / 2, middle = (inner + edge) / 2;
                assertTrue(in(shape, middle, a, p.level() + DECK / 2), shape + " deck at " + p.level());
                assertFalse(in(shape, middle, a, p.level() + DECK + .3), shape + " room over the deck at " + p.level());
                assertTrue(in(shape, edge - .1, a, p.level() + 1), shape + " handrail at " + p.level());
                // The deck's collision is one 1/8 m layer: a player stands on it 1/8 m over the block boundary.
                var point = box(shape, middle, a, p.level() + DECK / 2);
                int x = (int) Math.floor(point[0]), y = (int) Math.floor(point[1]), z = (int) Math.floor(point[2]);
                int px = (int) ((point[0] - x) * s), pz = (int) ((point[2] - z) * s);
                assertTrue(ShapeRasterizer.solid(ShapeRasterizer.key(shape, x, y, z)).stream()
                        .anyMatch(b -> b.y0() == 0 && b.y1() == 1 && b.x0() <= px && px < b.x1() && b.z0() <= pz && pz < b.z1()), shape + " walkable deck at " + p.level());
            }
        }
    }

    @Test
    void laddersRiseFromTheGroundToEachDeckAndAlternate() {
        for (var shape : COLUMNS) {
            ProcessUnitTest.climbedFromShellCells(shape, new ColumnStructure(shape).climbs());
            var plan = ColumnStructure.plan(shape);
            var ladders = plan.ladders();
            var platforms = plan.platforms();
            assertEquals(platforms.size(), ladders.size());
            assertEquals(0, ladders.getFirst().base());
            double rung = shape.outerRadius() + STANDOFF;
            for (int k = 0; k < ladders.size(); k++) {
                var l = ladders.get(k);
                assertEquals(platforms.get(k).level() + DECK + LANDING, l.top(), 1e-12, "rails stand 1.07 m over the landing");
                if (k > 0) {
                    assertEquals(platforms.get(k - 1).level() + DECK, l.base(), 1e-12, "a climb starts on the deck below");
                    assertNotEquals(ladders.get(k - 1).angle(), l.angle(), "ladders alternate");
                }
                // The climb ends beside its landing deck, not through it.
                var landing = platforms.get(k);
                double along = landing.ring() ? l.angle() + 2 * Math.PI : l.angle();
                assertTrue(along < landing.from() || along > landing.to(), shape + " ladder " + k + " lands beside the deck");
                double y = (l.base() + l.top()) / 2;
                var front = box(shape, rung + .3, l.angle(), y);
                assertTrue(ShapeRasterizer.climbable(shape, front[0], front[1], front[2]), shape + " climbable in front of the rungs");
                var away = box(shape, rung + 1.5, l.angle(), y);
                assertFalse(ShapeRasterizer.climbable(shape, away[0], away[1], away[2]), shape + " not climbable outside the cage");
                assertTrue(in(shape, rung - .04, l.angle(), y), shape + " the rung plane is pressed against to climb");
                assertFalse(in(shape, rung + .3, l.angle(), y), shape + " room to climb in front of the rungs");
            }
        }
    }

    /**
     * A player (0.6 m square, 1.8 m tall, aligned with the world, not the deck) walks each deck from the landing beside the
     * arriving ladder to the next ladder, or round the top ring to its manway, and stands on the deck in front of the next
     * ladder; tested against the cells' collision boxes.
     */
    @Test
    void aPlayerWalksTheDecksAndStandsInFrontOfTheNextLadder() {
        for (var shape : COLUMNS) {
            var plan = ColumnStructure.plan(shape);
            var platforms = plan.platforms();
            var layout = ShapeRasterizer.layout(shape);
            var shell = new java.util.HashSet<>(layout.shell());
            var interior = new java.util.HashSet<>(layout.interior());
            double rung = shape.outerRadius() + STANDOFF, edge = plan.deckRadius(), walk = (rung + RUNG_FACE + edge - RAIL_BAND) / 2;
            double clear = ColumnStructure.clearance(shape);
            for (int k = 0; k < platforms.size(); k++) {
                double y = platforms.get(k).level() + DECK, arrive = plan.ladders().get(k).angle();
                double end = k + 1 < platforms.size() ? plan.ladders().get(k + 1).angle() : BACK, way = Math.signum(end - arrive);
                for (double a = arrive + way * (clear + .05); way * (end - a) >= 0; a += way * .05)
                    assertTrue(fits(shape, shell, interior, walk, a, y), shape + " walks at " + a + " on the deck at " + platforms.get(k).level());
                if (k + 1 == platforms.size()) continue;
                assertTrue(fits(shape, shell, interior, walk, end, y), shape + " stands in front of the ladder on the deck at " + platforms.get(k).level());
                assertFalse(fits(shape, shell, interior, walk, end, y - .05), shape + " is held by the deck at " + platforms.get(k).level());
            }
        }
    }

    @Test
    void nozzlesFaceThePipingSideAndEndInsideTheHandrail() {
        var crude = COLUMNS.get(1);
        var roles = crude.nozzles().stream().map(EquipmentShape.Nozzle::role).toList();
        assertEquals(List.of(1L, 1L, 1L, 1L, 2L, 2L, 2L, 1L, 1L), List.of(Role.VAPOUR_OUTLET, Role.REFLUX, Role.FEED, Role.BOTTOMS, Role.DRAW, Role.PUMPAROUND_DRAW, Role.RETURN, Role.STEAM,
                        Role.VAPOUR_RETURN).stream()
                .map(r -> roles.stream().filter(r::equals).count()).toList(), "every process connection has its nozzle: " + roles);
        // Owner decision 2026-10-16 (fixed sides): feed and bottoms face the front, reflux and pumparounds the right, draws,
        // steam and the reboiler's vapour return the left (frame +x); the overhead vapour leaves upwards.
        var sides = java.util.Map.of(Role.FEED, "2-", Role.BOTTOMS, "2-", Role.REFLUX, "0-", Role.PUMPAROUND_DRAW, "0-", Role.RETURN, "0-",
                Role.DRAW, "0+", Role.STEAM, "0+", Role.VAPOUR_RETURN, "0+", Role.VAPOUR_OUTLET, "1+");
        for (var shape : COLUMNS) {
            double edge = ColumnStructure.plan(shape).deckRadius();
            for (var nozzle : shape.nozzles()) {
                assertEquals(sides.get(nozzle.role()), nozzle.axis() + (nozzle.sign() > 0 ? "+" : "-"), shape + " " + nozzle.role());
                // The bottoms leave at grade through the skirt's port, under every platform.
                if (nozzle.axis() == 1 || nozzle.role() == Role.BOTTOMS) continue;
                double reach = Math.abs(nozzle.face()[nozzle.axis()] - shape.axis());
                assertTrue(reach < edge - .1, shape + " " + nozzle.role() + " flange at " + reach + " past the handrail at " + edge);
            }
        }
    }

    /**
     * Owner request 2026-10-16 ("let users arrange them"): every nozzle leaves a free block beyond its flange for the
     * player's pipe, clear of platforms, cages and the davit, whatever the diameter.
     */
    @Test
    void everyNozzleLeavesAFreeBlockForItsPipe() {
        // A crowded design (draws and pumparounds on neighbouring trays) keeps its pipes apart too.
        var crowded = EquipmentShape.column(4, new ColumnTrays(30, 15, List.of(8, 9, 20, 21), List.of(31),
                List.of(new ColumnTrays.Pumparound(11, 10), new ColumnTrays.Pumparound(23, 22))), Heading.NORTH, 4);
        assertEquals(14, crowded.nozzles().size());
        for (var shape : java.util.stream.Stream.concat(COLUMNS.stream(), java.util.stream.Stream.of(crowded)).toList()) {
            ProcessUnitTest.pipesApart(shape);
            var layout = ShapeRasterizer.layout(shape);
            var occupied = new java.util.HashSet<ShapeRasterizer.Cell>(layout.shell());
            occupied.addAll(layout.interior());
            var ports = ShapeRasterizer.ports(shape);
            assertEquals(shape.nozzles().size(), ports.size(), shape.toString());
            for (var port : ports) {
                var pipe = new ShapeRasterizer.Cell(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
                assertFalse(occupied.contains(pipe), shape + " " + shape.nozzles().get(port.nozzle()).role() + ": no room for a pipe at " + pipe);
            }
        }
    }

    /**
     * Finding F1 (batch 2026-10-26-multi-machine-framework): a short column with four draws and two steam feeds has no row
     * for every nozzle on its left side (the reboiler's vapour return, the draws, the steam); the ones left out are listed,
     * not dropped in silence. The other designs place all.
     */
    @Test
    void aNozzleWithoutAFreeRowIsListedAsUnplaced() {
        var crowded = EquipmentShape.column(2, new ColumnTrays(4, 2, List.of(1, 2, 3, 4), List.of(4, 5), List.of()), Heading.NORTH, 3);
        var unplaced = ColumnStructure.unplaced(crowded);
        assertFalse(unplaced.isEmpty(), "seven nozzles on the left of a 5 m shell");
        for (var u : unplaced) {
            assertTrue(u.role() == Role.VAPOUR_RETURN || u.role() == Role.DRAW || u.role() == Role.STEAM, u.toString());
            assertEquals(0, u.axis());
            assertEquals(1, u.sign(), "the left side");
        }
        // Vapour outlet, reflux, feed, vapour return, four draws, two steam and the bottoms through the skirt, less those left out.
        assertEquals(11 - unplaced.size(), crowded.nozzles().size());
        for (var shape : COLUMNS) assertEquals(List.of(), ColumnStructure.unplaced(shape), shape.toString());
    }

    /** A design imported from a column calculator keeps its numbers; cutting trays drops what no longer fits; the layout round-trips. */
    @Test
    void traysComeFromACalculatorAndRefitWhenCut() {
        var input = com.wormzjl.icheme.science.column.v3.V3HollandExample32.input();
        var imported = ColumnTrays.of(input);
        assertEquals(input.stageCount(), imported.trays());
        assertEquals(input.feedStageNumber(), imported.feed());
        assertEquals(List.of(input.sideDraws().getFirst().trayNumber()), imported.draws());
        var crude = COLUMNS.get(1).trays();
        assertEquals(crude, ColumnTrays.decode(crude.encode()));
        assertEquals(new ColumnTrays(20, 20, List.of(10), List.of(), List.of(new ColumnTrays.Pumparound(12, 10))), crude.withTrays(20));
        assertThrows(IllegalArgumentException.class, () -> ColumnTrays.decode(new int[]{12, 6, 1}));
    }

    /** Whether the frame point {@code r} out from the axis at angle {@code a} and height {@code y} lies in the solid. */
    private static boolean in(EquipmentShape shape, double r, double a, double y) {
        var p = box(shape, r, a, y);
        return ShapeRasterizer.inside(shape, p[0], p[1], p[2]);
    }

    /** Whether a player's box with its feet at frame point (r, a, y) meets no collision box of the column's cells. */
    private static boolean fits(EquipmentShape shape, java.util.Set<ShapeRasterizer.Cell> shell, java.util.Set<ShapeRasterizer.Cell> interior, double r, double a, double y) {
        var feet = box(shape, r, a, y);
        double[] lo = {feet[0] - .3, feet[1], feet[2] - .3}, hi = {feet[0] + .3, feet[1] + 1.8, feet[2] + .3};
        double s = ShapeRasterizer.SOLID_RESOLUTION, eps = 1e-7;
        for (int x = (int) Math.floor(lo[0]); x < hi[0]; x++)
            for (int cy = (int) Math.floor(lo[1]); cy < hi[1]; cy++)
                for (int z = (int) Math.floor(lo[2]); z < hi[2]; z++) {
                    var cell = new ShapeRasterizer.Cell(x, cy, z);
                    List<ShapeRasterizer.Box> boxes = shell.contains(cell) ? ShapeRasterizer.solid(ShapeRasterizer.key(shape, x, cy, z))
                            : interior.contains(cell) ? List.of(new ShapeRasterizer.Box(0, 0, 0, (int) s, (int) s, (int) s)) : List.of();
                    for (var b : boxes)
                        if (lo[0] < x + b.x1() / s - eps && hi[0] > x + b.x0() / s + eps && lo[1] < cy + b.y1() / s - eps && hi[1] > cy + b.y0() / s + eps
                                && lo[2] < z + b.z1() / s - eps && hi[2] > z + b.z0() / s + eps) return false;
                }
        return true;
    }

    private static double[] box(EquipmentShape shape, double r, double a, double y) {
        double axis = shape.axis();
        return shape.transform().point(new double[]{axis + r * Math.cos(a), y, axis + r * Math.sin(a)});
    }
}
