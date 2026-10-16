package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standalone process units (owner request and decisions 2026-10-16, batch 2026-10-15-custom-equipment-rendering,
 * {@code STANDALONE_UNITS_PLAN.md}): their sizes and ranges, where each nozzle faces, and that at every size each
 * nozzle leaves a free block for its pipe, clear of the saddles, frames and other nozzles.
 */
class ProcessUnitTest {
    /** Every size a player can choose, at the shortest and longest tubes and the lowest support; an odd and a turned one. */
    private static List<EquipmentShape> everySize() {
        var shapes = new ArrayList<EquipmentShape>();
        for (var kind : List.of(Kind.EXCHANGER, Kind.KETTLE, Kind.AIR_COOLER, Kind.EJECTOR)) {
            double step = ProcessUnit.diameterStep(kind);
            for (double d = ProcessUnit.minDiameter(kind); d <= ProcessUnit.maxDiameter(kind) + 1e-9; d += step) {
                double size = ProcessUnit.snap(kind, d);
                for (int length : new int[]{ProcessUnit.minLength(kind, size), ProcessUnit.maxLength(kind)})
                    shapes.add(EquipmentShape.unit(kind, size, length, Heading.NORTH, ProcessUnit.minSupport(kind)));
            }
        }
        shapes.add(EquipmentShape.unit(Kind.EXCHANGER, 1.25, 7, Heading.EAST, 5));
        shapes.add(EquipmentShape.unit(Kind.EJECTOR, .7, 1, Heading.SOUTH, 9));
        return shapes;
    }

    @Test
    void sizesFollowTheirRangesAndGrid() {
        // Off the grid, outside the range, tubes under three bundle diameters, a second air cooler size, supports too low.
        for (Runnable bad : List.<Runnable>of(
                () -> EquipmentShape.unit(Kind.EXCHANGER, .4, 2, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EXCHANGER, 2.75, 12, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EXCHANGER, .6, 2, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EXCHANGER, 2, 5, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EXCHANGER, 1, 13, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EXCHANGER, 1, 4, Heading.NORTH, 1),
                () -> EquipmentShape.unit(Kind.KETTLE, 2.25, 10, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.KETTLE, 1, 11, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 4, 6, Heading.NORTH, 6),
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 3, 7, Heading.NORTH, 6),
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 3, 6, Heading.NORTH, 4),
                () -> EquipmentShape.unit(Kind.EJECTOR, 1.1, 1, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, .35, 1, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, .3, 2, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, .3, 1, Heading.NORTH, 17),
                () -> EquipmentShape.unit(Kind.DRUM, 2, 4, Heading.NORTH, 2)))
            assertThrows(IllegalArgumentException.class, bad::run);
        assertEquals(.3, ProcessUnit.snap(Kind.EJECTOR, .1 + .1 + .1), 0);
        assertEquals(2.5, ProcessUnit.snap(Kind.EXCHANGER, 3), 0);
        assertEquals(.75, ProcessUnit.snap(Kind.KETTLE, .8), 0);
        assertEquals(3, ProcessUnit.minLength(Kind.EXCHANGER, .75));
        assertEquals(2, ProcessUnit.minLength(Kind.EXCHANGER, .5));
        // Physics of the plan: 1 m shell, 6 m tubes ≈ 450 m²; a kettle's shell 1.6 bundles; fans cover at least 40 % of the
        // bundle face (API 661); an ejector of 0.3 m suction is 2.5 m long.
        assertEquals(450, ProcessUnit.tubeSurface(1, 6), 1e-9);
        assertEquals(1.5, ProcessUnit.kettleShell(1), 0);
        assertEquals(3.25, ProcessUnit.kettleShell(2), 0);
        double fans = 2 * Math.PI * ProcessUnit.FAN * ProcessUnit.FAN / 4;
        assertTrue(fans >= .4 * ProcessUnit.bundleFace(), fans + " of " + ProcessUnit.bundleFace());
        assertEquals(2.49, ProcessUnit.ejectorLength(.3), 1e-9);
    }

    @Test
    void nozzlesFaceWhereTheirFluidGoes() {
        // Tube side in at the channel's bottom and out at its top; a condensing shell side in on top and out underneath;
        // a kettle fed underneath, its vapour up, its product over the weir at the back; an air cooler's process in on top
        // of its inlet header and out underneath; an ejector's suction at the front, motive steam on top, discharge down.
        var expected = Map.of(
                Kind.EXCHANGER, Map.of(Role.TUBE_INLET, "1-", Role.TUBE_OUTLET, "1+", Role.SHELL_INLET, "1+", Role.SHELL_OUTLET, "1-"),
                Kind.KETTLE, Map.of(Role.TUBE_INLET, "1+", Role.TUBE_OUTLET, "1-", Role.FEED, "1-", Role.VAPOUR_OUTLET, "1+", Role.LIQUID_OUTLET, "2+"),
                Kind.AIR_COOLER, Map.of(Role.PROCESS_INLET, "1+", Role.PROCESS_OUTLET, "1-"),
                Kind.EJECTOR, Map.of(Role.SUCTION, "2-", Role.MOTIVE, "1+", Role.DISCHARGE, "1-"));
        for (var shape : everySize()) {
            var sides = expected.get(shape.kind());
            var nozzles = shape.nozzles();
            assertEquals(sides.keySet(), new HashSet<>(nozzles.stream().map(EquipmentShape.Nozzle::role).toList()), shape.toString());
            assertEquals(sides.size(), nozzles.size(), shape.toString());
            for (var nozzle : nozzles) {
                assertEquals(sides.get(nozzle.role()), nozzle.axis() + (nozzle.sign() > 0 ? "+" : "-"), shape + " " + nozzle.role());
                assertTrue(nozzle.defaultOd() > 0 && nozzle.defaultOd() <= nozzle.maxOd(), shape + " " + nozzle.role());
            }
        }
    }

    @Test
    void everyNozzleLeavesAFreeBlockForItsPipe() {
        for (var shape : everySize()) {
            var layout = ShapeRasterizer.layout(shape);
            var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());
            occupied.addAll(layout.interior());
            var pipes = new HashSet<ShapeRasterizer.Cell>();
            var ports = ShapeRasterizer.ports(shape);
            assertEquals(shape.nozzles().size(), ports.size(), shape.toString());
            for (var port : ports) {
                var role = shape.nozzles().get(port.nozzle()).role();
                var pipe = new ShapeRasterizer.Cell(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
                assertTrue(occupied.contains(port.cell()), shape + " " + role);
                assertFalse(occupied.contains(pipe), shape + " " + role + ": no room for a pipe at " + pipe);
                assertTrue(pipe.y() >= 0, shape + " " + role + " pipe under the ground");
                assertTrue(pipes.add(pipe), shape + " " + role + " shares its pipe's block");
            }
            // Flanges at their largest stand clear of each other.
            var nozzles = shape.nozzles();
            for (int i = 0; i < nozzles.size(); i++)
                for (int j = i + 1; j < nozzles.size(); j++)
                    assertFalse(overlap(flange(nozzles.get(i)), flange(nozzles.get(j))), shape + " " + nozzles.get(i).role() + " meets " + nozzles.get(j).role());
            // The enclosed insides (shells, channels, the kettle's cone, the plenum) are sealed by drawn cells.
            for (var cell : layout.interior())
                for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}})
                    assertTrue(occupied.contains(new ShapeRasterizer.Cell(cell.x() + d[0], cell.y() + d[1], cell.z() + d[2])), shape + " interior " + cell + " touches the outside");
            // On the ground and filling its box.
            int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, max = {-1, -1, -1};
            for (var cell : layout.shell()) {
                int[] c = {cell.x(), cell.y(), cell.z()};
                for (int a = 0; a < 3; a++) {
                    min[a] = Math.min(min[a], c[a]);
                    max[a] = Math.max(max[a], c[a]);
                }
            }
            assertArrayEquals(new int[]{0, 0, 0}, min, shape.toString());
            assertArrayEquals(new int[]{shape.sizeX() - 1, shape.sizeY() - 1, shape.sizeZ() - 1}, max, shape.toString());
        }
    }

    /** The box of a nozzle's largest flange in the frame: its thickness behind the face, its radius across. */
    private static double[][] flange(EquipmentShape.Nozzle nozzle) {
        double r = PipeSizes.flange(nozzle.maxOd()) / 2, t = PipeSizes.flangeThickness(nozzle.maxOd());
        double[] face = nozzle.face(), lo = new double[3], hi = new double[3];
        for (int a = 0; a < 3; a++) {
            lo[a] = face[a] - r;
            hi[a] = face[a] + r;
        }
        int x = nozzle.axis();
        lo[x] = Math.min(face[x], face[x] - nozzle.sign() * t);
        hi[x] = Math.max(face[x], face[x] - nozzle.sign() * t);
        return new double[][]{lo, hi};
    }

    private static boolean overlap(double[][] p, double[][] q) {
        for (int a = 0; a < 3; a++) if (p[1][a] <= q[0][a] || q[1][a] <= p[0][a]) return false;
        return true;
    }
}
