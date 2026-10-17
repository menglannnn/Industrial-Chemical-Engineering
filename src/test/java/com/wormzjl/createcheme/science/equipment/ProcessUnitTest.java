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
 * {@code STANDALONE_UNITS_PLAN.md}; machines and heaters 2026-10-17, {@code MACHINES_HEATERS_PLAN.md}): their sizes and
 * ranges, where each nozzle faces, and that at every size each nozzle leaves a free block for its pipe, clear of the
 * saddles, frames and other nozzles.
 */
class ProcessUnitTest {
    /**
     * Every size a player can choose, at the shortest and longest tubes and the lowest support (a compressor's at 2 throws,
     * a heater's at its shortest radiant section, the largest also at the longest); odd and turned ones.
     */
    private static List<EquipmentShape> everySize() {
        var shapes = new ArrayList<EquipmentShape>();
        for (var kind : List.of(Kind.EXCHANGER, Kind.KETTLE, Kind.AIR_COOLER, Kind.EJECTOR, Kind.PUMP, Kind.COMPRESSOR, Kind.VERTICAL_HEATER, Kind.CABIN_HEATER)) {
            double step = ProcessUnit.diameterStep(kind);
            boolean large = kind == Kind.COMPRESSOR || kind == Kind.VERTICAL_HEATER || kind == Kind.CABIN_HEATER;
            for (double d = ProcessUnit.minDiameter(kind); d <= ProcessUnit.maxDiameter(kind) + 1e-9; d += step) {
                double size = ProcessUnit.snap(kind, d);
                boolean last = size == ProcessUnit.maxDiameter(kind);
                for (int length : large && !last ? new int[]{ProcessUnit.minLength(kind, size)} : new int[]{ProcessUnit.minLength(kind, size), ProcessUnit.maxLength(kind, size)})
                    shapes.add(EquipmentShape.unit(kind, size, length, Heading.NORTH, ProcessUnit.minSupport(kind, size, length)));
            }
        }
        shapes.add(EquipmentShape.unit(Kind.PUMP, .25, 1, Heading.EAST, 0));
        shapes.add(EquipmentShape.unit(Kind.PUMP, .45, 1, Heading.SOUTH, 0));
        shapes.add(EquipmentShape.unit(Kind.COMPRESSOR, .15, 6, Heading.SOUTH, 3));
        shapes.add(EquipmentShape.unit(Kind.COMPRESSOR, .45, 4, Heading.WEST, ProcessUnit.minSupport(Kind.COMPRESSOR, .45, 4)));
        shapes.add(EquipmentShape.unit(Kind.VERTICAL_HEATER, 4.5, 9, Heading.EAST, 5));
        shapes.add(EquipmentShape.unit(Kind.CABIN_HEATER, 7, 20, Heading.WEST, 4));
        shapes.add(EquipmentShape.unit(Kind.CABIN_HEATER, 4, 25, Heading.SOUTH, 3));
        shapes.add(EquipmentShape.unit(Kind.EXCHANGER, 1.25, 7, Heading.EAST, 5));
        shapes.add(EquipmentShape.unit(Kind.EJECTOR, .7, 1, Heading.SOUTH, 9));
        // Air cooler banks (owner request 2026-10-16): every count in parallel and in series of the smallest bay, the
        // largest bank of the largest bay, and an odd one turned.
        for (int p = 1; p <= ProcessUnit.Bank.MAX_PARALLEL; p++)
            for (int s = 1; s <= ProcessUnit.Bank.MAX_SERIES; s++)
                shapes.add(EquipmentShape.unit(Kind.AIR_COOLER, 1.5, 4, Heading.NORTH, 5, new ProcessUnit.Bank(p, s)));
        shapes.add(EquipmentShape.unit(Kind.AIR_COOLER, 3, 6, Heading.NORTH, 5, new ProcessUnit.Bank(ProcessUnit.Bank.MAX_PARALLEL, ProcessUnit.Bank.MAX_SERIES)));
        shapes.add(EquipmentShape.unit(Kind.AIR_COOLER, 2.5, 4, Heading.WEST, 7, new ProcessUnit.Bank(3, 2)));
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
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 3, 6, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 1, 4, Heading.NORTH, 5),
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 3, 3, Heading.NORTH, 5),
                () -> EquipmentShape.unit(Kind.AIR_COOLER, 2.5, 3, Heading.NORTH, 5),
                () -> EquipmentShape.unit(Kind.EXCHANGER, 1, 4, Heading.NORTH, 2, new ProcessUnit.Bank(2, 1)),
                () -> new ProcessUnit.Bank(ProcessUnit.Bank.MAX_PARALLEL + 1, 1),
                () -> new ProcessUnit.Bank(1, 0),
                () -> EquipmentShape.unit(Kind.EJECTOR, 1.1, 1, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, .33, 1, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, 0, 1, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, .3, 2, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.EJECTOR, .3, 1, Heading.NORTH, 17),
                () -> EquipmentShape.unit(Kind.DRUM, 2, 4, Heading.NORTH, 2),
                // Machines and heaters (2026-10-17): a pump off its grid, too large or raised; odd throws, a bore too large, a
                // foundation too low for the discharge bottles; a vertical heater too slender or its floor too low; a cabin off
                // its 1 m grid or too long.
                () -> EquipmentShape.unit(Kind.PUMP, .12, 1, Heading.NORTH, 0),
                () -> EquipmentShape.unit(Kind.PUMP, .8, 1, Heading.NORTH, 0),
                () -> EquipmentShape.unit(Kind.PUMP, .1, 1, Heading.NORTH, 1),
                () -> EquipmentShape.unit(Kind.COMPRESSOR, .3, 3, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.COMPRESSOR, 1.25, 2, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.COMPRESSOR, .45, 12, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.VERTICAL_HEATER, 10, 21, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.COMPRESSOR, .8, 6, Heading.NORTH, ProcessUnit.minSupport(Kind.COMPRESSOR, .8, 6) - 1),
                () -> EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 12, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 5, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 8, Heading.NORTH, 2),
                () -> EquipmentShape.unit(Kind.CABIN_HEATER, 4.5, 8, Heading.NORTH, 3),
                () -> EquipmentShape.unit(Kind.CABIN_HEATER, 6, 49, Heading.NORTH, 3)))
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
        // Owner request 2026-10-16 ("allow smaller ACs"): every bay from 1.5 m wide, each with fans over 40 % of its bundle
        // face (API 661); 1.5 and 2 m wide from 3 m long, 2.5 and 3 m wide from 4 m (a 3 m bay of 3 m has 28 %). The 3 × 6 m bay keeps two
        // fans of 1.9 m, their rings 2.13 m across (owner report 2026-10-16: the rings of 2.05 m fans overlapped).
        var bays = ProcessUnit.bays();
        assertEquals(14, bays.size());
        for (var bay : bays) {
            assertTrue(ProcessUnit.coverage(bay[0], bay[1]) >= ProcessUnit.FAN_COVERAGE, bay[0] + " x " + bay[1]);
            EquipmentShape.unit(Kind.AIR_COOLER, bay[0], bay[1], Heading.NORTH, 5);
        }
        assertEquals(3, ProcessUnit.minLength(Kind.AIR_COOLER, 1.5));
        assertEquals(4, ProcessUnit.minLength(Kind.AIR_COOLER, 2.5));
        assertEquals(3, ProcessUnit.minLength(Kind.AIR_COOLER, 2));
        assertEquals(4, ProcessUnit.minLength(Kind.AIR_COOLER, 3));
        assertEquals(new ProcessUnit.Fans(2, 1.9), new ProcessUnit.Fans(ProcessUnit.fans(3, 6).count(), Math.round(ProcessUnit.fans(3, 6).diameter() * 100) / 100.0));
        assertEquals(2.49, ProcessUnit.ejectorLength(.3), 1e-9);
        // Owner request 2026-10-16 ("allow smaller ejectors"): suction from 0.05 m; one of 0.2 m or less stands on a single
        // post in the cell behind it, so it takes one cell by two; a larger one keeps its four-post frame.
        assertEquals(.05, ProcessUnit.minDiameter(Kind.EJECTOR), 0);
        for (double s : new double[]{.05, .1, .2}) {
            var small = EquipmentShape.unit(Kind.EJECTOR, s, 1, Heading.NORTH, 1);
            assertEquals(1, small.sizeX(), small.toString());
            assertEquals(2, small.sizeZ(), small.toString());
        }
        assertEquals(3, EquipmentShape.unit(Kind.EJECTOR, .25, 1, Heading.NORTH, 1).sizeX());
        // Machines and heaters (MACHINES_HEATERS_PLAN.md): a pump's suction one size up; liquid at 3 m/s in its discharge
        // (85 m³/h at 0.1 m), 33 kW at a 100 m head and 70 %, a TEFC motor of about 0.46 m; four 0.4 m cylinders at a 4 m/s
        // mean piston speed (600 rpm on a 0.2 m stroke) sweep about 7,200 m³/h, a 1.2 m bore runs about 300 rpm
        // (MACHINES_SCALE_RESEARCH.md); a vertical heater of D 4 × 8 m about 6.3 MW, a cabin of 6 × 12 m about 13.5 MW, of
        // 12 × 24 m 56.4 MW and a twin cell twice that at 48 m (12 × 40 m: two cells of 20 m, 93.1 MW); radiant heights from 1.5 to 2.75 diameters and 20 m at most; a heater's floor 3 m up; a pump at
        // grade.
        assertEquals(.15, ProcessUnit.pumpSuction(.1), 0);
        assertEquals(.55, ProcessUnit.pumpSuction(.4), 0);
        assertEquals(.95, ProcessUnit.pumpSuction(.75), 0);
        assertEquals(4771, ProcessUnit.pumpFlow(.75), 1);
        assertEquals(84.8, ProcessUnit.pumpFlow(.1), .1);
        assertEquals(33.0, ProcessUnit.pumpPower(.1), .1);
        assertEquals(.46, ProcessUnit.motorDiameter(ProcessUnit.pumpPower(.1)), .01);
        assertEquals(7238, ProcessUnit.displacement(.4, 4), 1);
        assertEquals(300, ProcessUnit.compressorSpeed(1.2), 1e-9);
        assertEquals(6.29, ProcessUnit.heaterDuty(Kind.VERTICAL_HEATER, 4, 8), .01);
        assertEquals(13.5, ProcessUnit.heaterDuty(Kind.CABIN_HEATER, 6, 12), .05);
        assertEquals(93.1, ProcessUnit.heaterDuty(Kind.CABIN_HEATER, 12, 40), .05);
        assertEquals(2 * ProcessUnit.heaterDuty(Kind.CABIN_HEATER, 12, 24), ProcessUnit.heaterDuty(Kind.CABIN_HEATER, 12, 48), 1e-9);
        assertEquals(List.of(1, 2), List.of(ProcessUnit.cabinCells(24), ProcessUnit.cabinCells(25)));
        assertEquals(48, ProcessUnit.maxLength(Kind.CABIN_HEATER, 12));
        assertEquals(6, ProcessUnit.minLength(Kind.VERTICAL_HEATER, 4));
        assertEquals(11, ProcessUnit.maxLength(Kind.VERTICAL_HEATER, 4));
        assertEquals(20, ProcessUnit.maxLength(Kind.VERTICAL_HEATER, 12));
        assertEquals(3, ProcessUnit.minSupport(Kind.VERTICAL_HEATER, 4, 8));
        assertEquals(0, ProcessUnit.maxSupport(Kind.PUMP));
        // A compressor's lowest foundation keeps its discharge bottles 0.3 m and its motor 0.1 m over grade, and is the
        // lowest that does.
        for (double b = .15; b <= ProcessUnit.maxDiameter(Kind.COMPRESSOR) + 1e-9; b += .05)
            for (int throwsCount = 2; throwsCount <= ProcessUnit.MAX_THROWS; throwsCount += 2) {
                double bore = ProcessUnit.snap(Kind.COMPRESSOR, b);
                var c = ProcessUnit.Compressor.of(bore, throwsCount);
                int h = ProcessUnit.minSupport(Kind.COMPRESSOR, bore, throwsCount);
                assertTrue(c.clears(h) && (h == 1 || !c.clears(h - 1)), bore + " x " + throwsCount);
                assertTrue(c.dischargeAxis(h) - c.discharge() / 2 >= .3 && h + c.crank() - c.motor() / 2 >= .1, bore + " x " + throwsCount);
            }
    }

    @Test
    void theLowestBundleLeavesFreeAirUnderTheFans() {
        // A forced-draft fan takes its air from under its ring; half a fan diameter of free air there is a usual least.
        // Every bay on its lowest bundle (3 m, owner request 2026-10-16) keeps its rings' inlets that high (the 3 x 5 m
        // bay's 2.48 m fan least, at 0.6 D), and nothing of the unit lies under them between its motor and the ring's rim
        // but the beams carrying the motors.
        for (var bay : ProcessUnit.bays()) {
            int lowest = ProcessUnit.minSupport(Kind.AIR_COOLER, bay[0], bay[1]);
            var fans = ProcessUnit.fans(bay[0], bay[1]);
            var stack = ProcessUnit.FanStack.of(lowest, fans.diameter());
            assertTrue(stack.inlet() >= .5 * fans.diameter(), bay[0] + " x " + bay[1] + ": inlet " + stack.inlet());
            var unit = ProcessUnit.of(EquipmentShape.unit(Kind.AIR_COOLER, bay[0], bay[1], Heading.NORTH, lowest));
            double[] box = unit.box();
            // Across the bay at the ring's mid-radius, beside the motor and its beam, under the ring.
            double x = (box[0] + box[3]) / 2 + .35 * fans.diameter();
            for (double z = .75; z < bay[1] - .7; z += .1)
                for (double y = .05; y < stack.inlet() - .02; y += .1)
                    if (y < stack.motorBottom() - ProcessUnit.BEAM - .01 || y > stack.motorBottom() + .01)
                        assertFalse(unit.inside(new double[]{x, y, z}), bay[0] + " x " + bay[1] + " at " + y + ", " + z);
        }
    }

    @Test
    void nozzlesFaceWhereTheirFluidGoes() {
        // Tube side in at the channel's bottom and out at its top; a condensing shell side in on top and out underneath;
        // a kettle fed underneath, its vapour up, its product over the weir at the back; an air cooler's process in on top
        // of its inlet header and out underneath; an ejector's suction at the front, motive steam on top, discharge down; a
        // pump's suction at the front along its shaft and discharge up, an axially split one's suction to your right (-x)
        // and discharge to your left (owner decision 2026-10-17); a compressor's gas in on top of its suction header and
        // out at the front of its discharge header (one each, owner request 2026-10-17); a heater's convection and radiant
        // coils' inlets and outlets and its fuel gas towards the front.
        var expected = Map.of(
                Kind.EXCHANGER, Map.of(Role.TUBE_INLET, "1-", Role.TUBE_OUTLET, "1+", Role.SHELL_INLET, "1+", Role.SHELL_OUTLET, "1-"),
                Kind.KETTLE, Map.of(Role.TUBE_INLET, "1+", Role.TUBE_OUTLET, "1-", Role.FEED, "1-", Role.VAPOUR_OUTLET, "1+", Role.LIQUID_OUTLET, "2+"),
                Kind.AIR_COOLER, Map.of(Role.PROCESS_INLET, "1+", Role.PROCESS_OUTLET, "1-"),
                Kind.EJECTOR, Map.of(Role.SUCTION, "2-", Role.MOTIVE, "1+", Role.DISCHARGE, "1-"),
                Kind.PUMP, Map.of(Role.SUCTION, "2-", Role.DISCHARGE, "1+"),
                Kind.COMPRESSOR, Map.of(Role.SUCTION, "1+", Role.DISCHARGE, "2-"),
                Kind.VERTICAL_HEATER, Map.of(Role.CONVECTION_INLET, "2-", Role.CONVECTION_OUTLET, "2-", Role.RADIANT_INLET, "2-", Role.RADIANT_OUTLET, "2-", Role.FUEL, "2-"),
                Kind.CABIN_HEATER, Map.of(Role.CONVECTION_INLET, "2-", Role.CONVECTION_OUTLET, "2-", Role.RADIANT_INLET, "2-", Role.RADIANT_OUTLET, "2-", Role.FUEL, "2-"));
        for (var shape : everySize()) {
            var sides = shape.kind() == Kind.PUMP && shape.diameter() > ProcessUnit.OH2_MAX ? Map.of(Role.SUCTION, "0-", Role.DISCHARGE, "0+") : expected.get(shape.kind());
            var nozzles = shape.nozzles();
            assertEquals(sides.keySet(), new HashSet<>(nozzles.stream().map(EquipmentShape.Nozzle::role).toList()), shape.toString());
            assertEquals(sides.size(), nozzles.size(), shape.toString());
            for (var nozzle : nozzles) {
                assertEquals(sides.get(nozzle.role()), nozzle.axis() + (nozzle.sign() > 0 ? "+" : "-"), shape + " " + nozzle.role());
                assertTrue(nozzle.defaultOd() > 0 && nozzle.defaultOd() <= nozzle.maxOd(), shape + " " + nozzle.role());
            }
        }
    }

    /**
     * Owner rule 2026-10-17 ("piping connection should have 1 block spacing between them to prevent misconnection"):
     * pipes in blocks that share a face join, so no two of a shape's pipe blocks touch, by a face, an edge or a corner.
     */
    static void pipesApart(EquipmentShape shape) {
        var ports = ShapeRasterizer.ports(shape);
        for (int i = 0; i < ports.size(); i++)
            for (int j = i + 1; j < ports.size(); j++) {
                var p = ports.get(i);
                var q = ports.get(j);
                int dx = Math.abs(p.cell().x() + p.dx() - q.cell().x() - q.dx()), dy = Math.abs(p.cell().y() + p.dy() - q.cell().y() - q.dy()),
                        dz = Math.abs(p.cell().z() + p.dz() - q.cell().z() - q.dz());
                assertTrue(Math.max(dx, Math.max(dy, dz)) >= 2, shape + ": the pipes of " + shape.nozzles().get(p.nozzle()).role() + " and "
                        + shape.nozzles().get(q.nozzle()).role() + " touch");
            }
    }

    @Test
    void everyNozzleLeavesAFreeBlockForItsPipe() {
        for (var shape : everySize()) {
            var layout = ShapeRasterizer.layout(shape);
            var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());
            occupied.addAll(layout.interior());
            var ports = ShapeRasterizer.ports(shape);
            assertEquals(shape.nozzles().size(), ports.size(), shape.toString());
            for (var port : ports) {
                var role = shape.nozzles().get(port.nozzle()).role();
                var pipe = new ShapeRasterizer.Cell(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
                assertTrue(occupied.contains(port.cell()), shape + " " + role);
                assertFalse(occupied.contains(pipe), shape + " " + role + ": no room for a pipe at " + pipe);
                assertTrue(pipe.y() >= 0, shape + " " + role + " pipe under the ground");
            }
            pipesApart(shape);
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

    @Test
    void fanRingsKeepApartInsideThePlenum() {
        // Owner report 2026-10-16: two fans' rings overlapped. In every bay of a bank, rings side by side along the tubes
        // keep 0.02 m apart at their widest (flange and bellmouth, RING_OD rotor diameters), and each ring stays 0.05 m
        // inside the plenum's frame across the bay.
        for (var bay : ProcessUnit.bays()) {
            var shape = EquipmentShape.unit(Kind.AIR_COOLER, bay[0], bay[1], Heading.NORTH, 4, new ProcessUnit.Bank(2, 2));
            var rotors = ProcessUnit.rotors(shape);
            double ring = ProcessUnit.RING_OD * rotors.getFirst()[3];
            assertTrue(ring <= bay[0] - .32 + 1e-9, bay[0] + " x " + bay[1]);
            for (var a : rotors)
                for (var b : rotors)
                    if (a != b && Math.abs(a[0] - b[0]) < 1e-9)
                        assertTrue(Math.abs(a[2] - b[2]) >= ring + .02 - 1e-9, bay[0] + " x " + bay[1] + ": " + Math.abs(a[2] - b[2]) + " < " + ring);
        }
    }

    @Test
    void fanRotorsTurnAboutTheirHubs() {
        // Owner request 2026-10-16: the client turns each fan's rotor about its hub. A bank has a rotor per fan per bay,
        // each hub inside the bank's box; a rotor alone lies within its radius of the hub, and all of them together are
        // the area of the still rotors the chunk mesh would draw.
        var bank = new ProcessUnit.Bank(2, 2);
        var shape = EquipmentShape.unit(Kind.AIR_COOLER, 3, 6, Heading.EAST, 4, bank);
        var fans = ProcessUnit.fans(3, 6);
        var rotors = ProcessUnit.rotors(shape);
        assertEquals(bank.bays() * fans.count(), rotors.size());
        for (var r : rotors) {
            assertEquals(fans.diameter(), r[3], 1e-12);
            assertTrue(r[0] > 0 && r[0] < shape.sizeX() && r[1] > 0 && r[1] < shape.sizeY() && r[2] > 0 && r[2] < shape.sizeZ());
        }
        double radius = fans.diameter() / 2, one = 0;
        for (var polygon : ProcessUnit.rotor(fans.diameter())) {
            assertEquals(Mesh.Material.AC_FAN, polygon.material());
            double[] p = polygon.positions();
            for (int i = 0; i < p.length; i += 3) {
                assertTrue(p[i] * p[i] + p[i + 2] * p[i + 2] <= radius * radius + 1e-9);
                assertTrue(Math.abs(p[i + 1]) <= radius);
            }
            one += area(polygon);
        }
        double still = 0;
        for (var cell : ShapeRasterizer.layout(shape).shell())
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z())))
                if (polygon.material() == Mesh.Material.AC_FAN) still += area(polygon);
        assertTrue(one > 0);
        assertEquals(rotors.size() * one, still, 1e-6 * still);
        assertEquals(List.of(), ProcessUnit.rotors(EquipmentShape.unit(Kind.EXCHANGER, 1, 4, Heading.NORTH, 2)));
    }

    @Test
    void heatersCarryWalkableDecksAndClimbableLadders() {
        // Owner feedback 2026-10-17 (MACHINES_HEATERS_PLAN.md, API 560 12.4.1): every deck is walked on with head room over
        // it; every ladder starts on a deck or the ground, is climbed in front of its rungs and pressed against at them; one
        // walked through is passed between its rails over its landing. Owner report 2026-10-17 ("ladder on both furnace are
        // not functional"): a player pressing the rungs stands in a shell cell all the way up, so Minecraft asks the shell.
        var shapes = List.of(EquipmentShape.unit(Kind.VERTICAL_HEATER, 2.5, 4, Heading.NORTH, 3), EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 9, Heading.EAST, 3),
                EquipmentShape.unit(Kind.VERTICAL_HEATER, 12, 20, Heading.SOUTH, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 4, 6, Heading.NORTH, 3),
                EquipmentShape.unit(Kind.CABIN_HEATER, 6, 12, Heading.WEST, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 12, 40, Heading.NORTH, 4),
                EquipmentShape.unit(Kind.CABIN_HEATER, 4, 25, Heading.EAST, 3));
        for (var shape : shapes) {
            var unit = ProcessUnit.of(shape);
            var starts = new HashSet<Double>(List.of(0.0));
            assertFalse(unit.decks().isEmpty(), shape.toString());
            for (var deck : unit.decks()) {
                double mid = deck.run().length() / 2, off = (Access.GAP + deck.width() - Access.RAIL_BAND) / 2;
                assertTrue(in(shape, deck.run().at(mid, off, deck.level() + Access.DECK / 2)), shape + " deck at " + deck.level());
                assertFalse(in(shape, deck.run().at(mid, off, deck.level() + Access.DECK + .3)), shape + " room over the deck at " + deck.level());
                starts.add(deck.level() + Access.DECK);
            }
            for (var l : unit.ladders()) {
                assertTrue(starts.contains(l.base()), shape + " a climb from " + l.base() + " starts on a deck or the ground");
                double y = (l.base() + l.rungTop()) / 2;
                assertTrue(climbable(shape, new double[]{l.x() + .3 * l.outX(), y, l.z() + .3 * l.outZ()}), shape + " climbable in front of the rungs at " + y);
                assertFalse(climbable(shape, new double[]{l.x() + 1.5 * l.outX(), y, l.z() + 1.5 * l.outZ()}), shape + " not climbable outside the cage");
                assertTrue(in(shape, new double[]{l.x() - .04 * l.outX(), y, l.z() - .04 * l.outZ()}), shape + " the rung plane is pressed against to climb");
                if (l.rungTop() < l.top()) assertFalse(in(shape, new double[]{l.x(), l.rungTop() + .6, l.z()}), shape + " walked through over its landing");
            }
            climbedFromShellCells(shape, unit.ladders());
            stoppedAtClosedEnds(shape, unit.decks());
        }
        assertTrue(ProcessUnit.of(EquipmentShape.unit(Kind.CABIN_HEATER, 6, 12, Heading.WEST, 3)).decks().size() >= 6);
    }

    /**
     * A player walking off a deck's closed end meets the game's collision there (owner report 2026-10-17, "box furnace
     * walkway end hitbox is not working"): some 1/8-block voxel, solid at its centre, lies in the end's rail band across the
     * player's width from knee to chest height.
     */
    static void stoppedAtClosedEnds(EquipmentShape shape, List<ProcessUnit.Deck> decks) {
        for (var d : decks)
            for (int e = 0; e < 2; e++) {
                if (!(e == 0 ? d.closedStart() : d.closedEnd())) continue;
                double len = d.run().length(), from = e == 0 ? 0 : len - Access.RAIL_BAND, off = (Access.GAP + d.width() - Access.RAIL_BAND) / 2, y = d.level() + Access.DECK;
                // The box round the band's eight corners (a round deck's band is curved), in the shape's box coordinates.
                double[] p = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, q = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
                for (double s : new double[]{from, from + Access.RAIL_BAND})
                    for (double o : new double[]{off - .3, off + .3})
                        for (double h : new double[]{y + .3, y + 1.2}) {
                            var c = shape.transform().point(d.run().at(s, o, h));
                            for (int i = 0; i < 3; i++) {
                                p[i] = Math.min(p[i], c[i]);
                                q[i] = Math.max(q[i], c[i]);
                            }
                        }
                int met = 0;
                for (int i = (int) Math.ceil(Math.min(p[0], q[0]) * 8 - .5); (i + .5) / 8 <= Math.max(p[0], q[0]); i++)
                    for (int j = (int) Math.ceil(Math.min(p[1], q[1]) * 8 - .5); (j + .5) / 8 <= Math.max(p[1], q[1]); j++)
                        for (int k = (int) Math.ceil(Math.min(p[2], q[2]) * 8 - .5); (k + .5) / 8 <= Math.max(p[2], q[2]); k++)
                            if (ShapeRasterizer.inside(shape, (i + .5) / 8, (j + .5) / 8, (k + .5) / 8)) met++;
                assertTrue(met > 0, shape + " the deck at " + d.level() + " stops a player at its closed end " + e);
            }
    }

    /** A player pressing a ladder's rungs (centre {@link Access#RUNG_FACE} + 0.3 m out) stands in a shell cell from its foot to its landing. */
    static void climbedFromShellCells(EquipmentShape shape, List<Access.Climb> climbs) {
        var shell = new HashSet<>(ShapeRasterizer.layout(shape).shell());
        for (var l : climbs)
            for (double y = l.base() + .05; y < l.rungTop(); y += .25) {
                double out = Access.RUNG_FACE + .3;
                var b = shape.transform().point(new double[]{l.x() + out * l.outX(), y, l.z() + out * l.outZ()});
                var cell = new ShapeRasterizer.Cell((int) Math.floor(b[0]), (int) Math.floor(b[1]), (int) Math.floor(b[2]));
                assertTrue(shell.contains(cell), shape + " a climber at " + y + " over the foot " + l.base() + " stands in shell cell " + cell);
                assertTrue(ShapeRasterizer.climbable(shape, b[0], b[1], b[2]), shape + " climbs at " + y);
            }
    }

    private static boolean in(EquipmentShape shape, double[] p) {
        var b = shape.transform().point(p);
        return ShapeRasterizer.inside(shape, b[0], b[1], b[2]);
    }

    private static boolean climbable(EquipmentShape shape, double[] p) {
        var b = shape.transform().point(p);
        return ShapeRasterizer.climbable(shape, b[0], b[1], b[2]);
    }

    private static double area(Mesh.Polygon polygon) {
        double[] p = polygon.positions(), n = new double[3];
        int m = polygon.size();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            n[0] += (p[3 * i + 1] - p[3 * j + 1]) * (p[3 * i + 2] + p[3 * j + 2]);
            n[1] += (p[3 * i + 2] - p[3 * j + 2]) * (p[3 * i] + p[3 * j]);
            n[2] += (p[3 * i] - p[3 * j]) * (p[3 * i + 1] + p[3 * j + 1]);
        }
        return .5 * Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
    }

    private static boolean overlap(double[][] p, double[][] q) {
        for (int a = 0; a < 3; a++) if (p[1][a] <= q[0][a] || q[1][a] <= p[0][a]) return false;
        return true;
    }
}
