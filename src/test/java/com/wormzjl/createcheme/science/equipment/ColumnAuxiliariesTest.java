package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.ColumnOptions.Condenser;
import com.wormzjl.createcheme.science.equipment.ColumnOptions.Reboiler;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A column's auxiliaries (batch 2026-10-15-custom-equipment-rendering): options, the skirt a kettle needs, the box, placement and clashes. */
class ColumnAuxiliariesTest {
    private static final ColumnTrays SIMPLE = ColumnTrays.simple(20, 10);
    /** Four pumparounds, two side draws: the most lanes the structure carries in the clash sweep. */
    private static final ColumnTrays FOUR = new ColumnTrays(40, 20, List.of(10, 25), List.of(41), List.of(new ColumnTrays.Pumparound(5, 3),
            new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(22, 20), new ColumnTrays.Pumparound(33, 31)));
    private static final ColumnOptions ALL = new ColumnOptions(Condenser.AIR, true, Reboiler.KETTLE);

    @Test
    void optionsRoundTrip() {
        var seen = new java.util.HashSet<Integer>();
        for (var condenser : Condenser.values())
            for (boolean ejector : new boolean[]{false, true})
                for (var reboiler : Reboiler.values()) {
                    var options = new ColumnOptions(condenser, ejector, reboiler);
                    assertEquals(options, ColumnOptions.decode(options.encode()));
                    assertTrue(seen.add(options.encode()), options.toString());
                }
        assertThrows(IllegalArgumentException.class, () -> ColumnOptions.decode(-1));
        assertThrows(IllegalArgumentException.class, () -> ColumnOptions.decode(8));
    }

    @Test
    void aKettleRaisesTheLeastSkirt() {
        var kettle = new ColumnOptions(Condenser.WATER, false, Reboiler.KETTLE);
        assertEquals(EquipmentShape.minimumSkirt(6), EquipmentShape.minimumSkirt(6, ColumnOptions.DEFAULT));
        assertTrue(EquipmentShape.minimumSkirt(6, kettle) > EquipmentShape.minimumSkirt(6));
        for (double d : new double[]{.5, 3, 6, 15}) {
            int least = EquipmentShape.minimumSkirt(d, kettle);
            assertDoesNotThrow(() -> EquipmentShape.column(d, SIMPLE, kettle, Heading.NORTH, least));
            if (least > EquipmentShape.minimumSkirt(d))
                assertThrows(IllegalArgumentException.class, () -> EquipmentShape.column(d, SIMPLE, kettle, Heading.NORTH, least - 1), "D " + d);
        }
    }

    @Test
    void theColumnStaysOnTheConstructorsLine() {
        // Facing north, east, south and west: the column's axis cell lies on the line through the constructor; the units stand aside.
        for (int[] step : new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            var front = VesselPlacement.Form.COLUMN.front(step[0], step[1]);
            var shape = EquipmentShape.column(3, FOUR, ALL, front, EquipmentShape.minimumSkirt(3, ALL));
            int[] origin = VesselPlacement.DEFAULT.origin(0, 0, 0, step[0], step[1], shape.sizeX(), shape.sizeZ(), shape.columnBox());
            double[] axis = shape.transform().point(new double[]{shape.axis(), 0, shape.axis()});
            int x = origin[0] + (int) Math.floor(axis[0]), z = origin[2] + (int) Math.floor(axis[2]);
            assertEquals(0, step[0] == 0 ? x : z, "facing " + step[0] + ", " + step[1]);
            assertTrue(shape.sizeX() * shape.sizeZ() > (shape.columnBox()[2] - shape.columnBox()[0]) * (shape.columnBox()[3] - shape.columnBox()[1]));
        }
    }

    /** Sizes, trays and every set of options: no line runs through another line, a unit, the column's structure or its shell; the box holds all. */
    @Test
    void linesClearEverythingTheyPass() {
        for (var trays : List.of(SIMPLE, FOUR))
            for (var condenser : Condenser.values())
                for (boolean ejector : new boolean[]{false, true})
                    for (var reboiler : Reboiler.values())
                        for (double d : new double[]{.5, 3, 10}) {
                            var options = new ColumnOptions(condenser, ejector, reboiler);
                            int skirt = Math.max(EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL), EquipmentShape.minimumSkirt(d, options));
                            var shape = EquipmentShape.column(d, trays, options, Heading.NORTH, skirt);
                            assertEquals(List.of(), clashes(shape), shape.toString());
                            double[] bounds = ColumnAuxiliaries.of(shape).bounds(), box = shape.frameBox();
                            for (int k = 0; k < 3; k++) {
                                assertTrue(bounds[k] >= box[k] - 1e-9 && bounds[k + 3] <= box[k + 3] + 1e-9, shape + " axis " + k);
                            }
                        }
    }

    /** Every floor leaves a walkway round its units: a player (0.6 m wide, 1.8 m tall) fits all along the walkway's middle. */
    @Test
    void floorsLeaveAWalkwayRoundTheUnits() {
        for (var trays : List.of(SIMPLE, FOUR))
            for (var condenser : Condenser.values())
                for (boolean ejector : new boolean[]{false, true})
                    for (var reboiler : Reboiler.values())
                        for (double d : new double[]{.5, 3, 10}) {
                            var options = new ColumnOptions(condenser, ejector, reboiler);
                            int skirt = Math.max(EquipmentShape.defaultSupport(d, EquipmentShape.Orientation.VERTICAL), EquipmentShape.minimumSkirt(d, options));
                            var shape = EquipmentShape.column(d, trays, options, Heading.NORTH, skirt);
                            var aux = ColumnAuxiliaries.of(shape);
                            assertEquals(condenser == Condenser.WATER ? 2 : 1, aux.floors().size(), shape.toString());
                            for (var f : aux.floors()) {
                                double inset = ColumnStructure.RAIL_BAND + ColumnAuxiliaries.WALKWAY / 2, deck = f[4] + ColumnAuxiliaries.DECK;
                                double x0 = f[0] + inset, z0 = f[1] + inset, x1 = f[2] - inset, z1 = f[3] - inset;
                                double[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
                                for (int k = 0; k < 4; k++) {
                                    double[] p = corners[k], q = corners[(k + 1) % 4];
                                    int steps = (int) Math.ceil(Math.max(Math.abs(q[0] - p[0]), Math.abs(q[1] - p[1])) / .25);
                                    for (int i = 0; i <= steps; i++) {
                                        double x = p[0] + (q[0] - p[0]) * i / steps, z = p[1] + (q[1] - p[1]) * i / steps;
                                        String blocked = blocked(aux, x, deck, z);
                                        assertEquals(null, blocked, shape + " floor at " + f[4]);
                                    }
                                }
                            }
                        }
    }

    /** Where a player standing on a deck at (x, deck, z) meets a part, or null. */
    private static String blocked(ColumnAuxiliaries aux, double x, double deck, double z) {
        for (double dy : new double[]{.05, .5, 1, 1.5, 1.79})
            for (double dx : new double[]{-.29, 0, .29})
                for (double dz : new double[]{-.29, 0, .29})
                    if (aux.inside(new double[]{x + dx, deck + dy, z + dz})) return format(new double[]{x + dx, deck + dy, z + dz});
        return null;
    }

    /**
     * Where a line's runs meet another line, or its centre or skin (four points round it) lies in a unit, the column's
     * drawn structure or its shell: sampled every 0.1 m, away from each line's ends, where it leaves its nozzles.
     */
    private static List<String> clashes(EquipmentShape shape) {
        var aux = ColumnAuxiliaries.of(shape);
        var column = new ColumnStructure(shape);
        var transform = shape.transform();
        var runs = aux.runs();
        Map<Integer, List<double[]>> ends = new HashMap<>();
        for (var r : runs) {
            var list = ends.computeIfAbsent((int) r[7], k -> new ArrayList<>());
            if (list.isEmpty()) list.add(new double[]{r[0], r[1], r[2]});
            if (list.size() > 1) list.remove(1);
            list.add(new double[]{r[3], r[4], r[5]});
        }
        var found = new ArrayList<String>();
        var seen = new java.util.HashSet<String>();
        for (var r : runs) {
            int line = (int) r[7];
            double radius = r[6], length = distance(new double[]{r[0], r[1], r[2]}, new double[]{r[3], r[4], r[5]});
            int steps = (int) Math.ceil(length / .1), axis = Math.abs(r[3] - r[0]) > 1e-9 ? 0 : Math.abs(r[4] - r[1]) > 1e-9 ? 1 : 2;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double[] p = {r[0] + (r[3] - r[0]) * t, r[1] + (r[4] - r[1]) * t, r[2] + (r[5] - r[2]) * t};
                if (near(p, ends.get(line), 2 * radius + .4)) continue;
                for (var o : runs) {
                    int other = (int) o[7];
                    if (other == line || near(p, ends.get(other), 2 * o[6] + 2 * radius + .4)) continue;
                    if (segment(p, o) - radius - o[6] < .02 && seen.add(line + "/" + other)) found.add("line " + line + " meets line " + other + " at " + format(p));
                }
                for (int k = 0; k < 5; k++) {
                    var q = p.clone();
                    if (k > 0) q[(axis + 1 + (k - 1) / 2) % 3] += (k % 2 == 0 ? -1 : 1) * radius * .9;
                    var b = transform.point(q);
                    String what = aux.insidePart(q) ? "a unit" : column.insideDrawn(q) ? "the column's structure"
                            : ShapeRasterizer.insideVessel(shape, b[0], b[1], b[2]) ? "the shell" : null;
                    if (what != null && seen.add(line + " " + what)) found.add("line " + line + " in " + what + " at " + format(q));
                }
            }
        }
        return found;
    }

    private static boolean near(double[] p, List<double[]> points, double reach) {
        for (var e : points) if (distance(p, e) < reach) return true;
        return false;
    }

    private static double segment(double[] p, double[] s) {
        double[] ab = {s[3] - s[0], s[4] - s[1], s[5] - s[2]};
        double length2 = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = length2 == 0 ? 0 : Math.max(0, Math.min(1, ((p[0] - s[0]) * ab[0] + (p[1] - s[1]) * ab[1] + (p[2] - s[2]) * ab[2]) / length2));
        return distance(p, new double[]{s[0] + ab[0] * t, s[1] + ab[1] * t, s[2] + ab[2] * t});
    }

    private static double distance(double[] p, double[] q) {
        return Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2]));
    }

    private static String format(double[] p) {
        return String.format(java.util.Locale.ROOT, "(%.2f, %.2f, %.2f)", p[0], p[1], p[2]);
    }
}
