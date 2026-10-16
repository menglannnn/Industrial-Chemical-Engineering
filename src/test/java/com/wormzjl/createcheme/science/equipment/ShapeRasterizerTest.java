package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Clash;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Connection;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.math.DeterministicMath;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wormzjl.createcheme.science.equipment.EquipmentShape.drum;
import static org.junit.jupiter.api.Assertions.*;

class ShapeRasterizerTest {
    /**
     * Bare drums (small, odd, half-metre and largest), drums on skirts, lying drums on saddles facing each way, and a
     * column with its default units and one with every unit, two pumparounds and the most skirt a kettle needs. (0.5, 5)
     * lying puts a saddle face exactly on a cell plane; (10, 60) is the report's largest vessel.
     */
    private static final List<EquipmentShape> SHAPES = List.of(new EquipmentShape(.5, 1), new EquipmentShape(2, 3),
            new EquipmentShape(2.5, 2), new EquipmentShape(3, 9), new EquipmentShape(4, 5), new EquipmentShape(10, 60),
            drum(2, 3, Orientation.VERTICAL, Heading.NORTH, 2), drum(6, 10, Orientation.VERTICAL, Heading.EAST, 3),
            drum(2.5, 2, Orientation.HORIZONTAL, Heading.WEST, 1), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2),
            drum(.5, 5, Orientation.HORIZONTAL, Heading.SOUTH, 1), drum(4, 12, Orientation.HORIZONTAL, Heading.EAST, 1),
            EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 2),
            EquipmentShape.column(1.5, new ColumnTrays(25, 13, List.of(8), List.of(26), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(15, 13))),
                    new ColumnOptions(ColumnOptions.Condenser.AIR, true, ColumnOptions.Reboiler.KETTLE), Heading.EAST,
                    EquipmentShape.minimumSkirt(1.5, new ColumnOptions(ColumnOptions.Condenser.AIR, true, ColumnOptions.Reboiler.KETTLE))));

    @Test
    void rejectsShapesOffTheGrid() {
        for (double[] bad : new double[][]{{.25, 3}, {2.25, 3}, {10.5, 3}, {Double.NaN, 3}, {2, 0}, {2, 1.5}, {2, 65}, {2, Double.POSITIVE_INFINITY}})
            assertThrows(IllegalArgumentException.class, () -> new EquipmentShape(bad[0], bad[1]), bad[0] + " x " + bad[1]);
        // A skirt shorter than the bottom head, saddles on the ground, supports above 16 m.
        for (var bad : List.of(new Object[]{6., Orientation.VERTICAL, 1}, new Object[]{2., Orientation.VERTICAL, -1}, new Object[]{2., Orientation.VERTICAL, 17},
                new Object[]{2., Orientation.HORIZONTAL, 0}, new Object[]{2., Orientation.HORIZONTAL, 17}))
            assertThrows(IllegalArgumentException.class, () -> drum((double) bad[0], 3, (Orientation) bad[1], Heading.NORTH, (int) bad[2]), List.of(bad).toString());
        // Columns reach 15 m, drums 10 m; a column stands on a skirt and takes its length from its trays.
        assertDoesNotThrow(() -> EquipmentShape.column(12, ColumnTrays.simple(20, 10), Heading.NORTH, 3));
        assertThrows(IllegalArgumentException.class, () -> drum(12, 5, Orientation.VERTICAL, Heading.NORTH, 3));
        assertThrows(IllegalArgumentException.class, () -> EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.NORTH, 0));
        assertThrows(IllegalArgumentException.class, () -> new EquipmentShape(EquipmentShape.Kind.COLUMN, 3, 4, Orientation.VERTICAL, Heading.NORTH, 2, ColumnTrays.simple(12, 6), ColumnOptions.DEFAULT, List.of()));
        assertEquals(2, EquipmentShape.minimumSkirt(6));
        assertEquals(3, EquipmentShape.defaultSupport(6, Orientation.VERTICAL));
        assertEquals(4, EquipmentShape.defaultSupport(10, Orientation.VERTICAL));
        assertEquals(1, EquipmentShape.defaultSupport(10, Orientation.HORIZONTAL));
    }

    @Test
    void surfacePiecesAddUpToTheFacetedArea() {
        for (var shape : SHAPES) {
            double area = 0;
            for (var cell : ShapeRasterizer.layout(shape).shell())
                for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) area += area(polygon);
            // Every facet is drawn once, whole: clipping to cells and splitting at texture tiles neither lose nor double area.
            double facets = ShapeRasterizer.facetArea(shape);
            assertEquals(facets, area, 1e-9 * facets, shape.toString());
            if (!shape.skirt() && !shape.saddles())
                assertEquals(vesselArea(shape), facets - ShapeRasterizer.nozzleArea(shape), 1e-9 * facets, shape.toString());
        }
    }

    @Test
    void partsCarryTheirMaterials() {
        // Skirt row 0 (base ring, chairs, floor plate, the bottom outlet leaving through the access opening, which a drum
        // leaves open), a skirt row showing the bottom head inside it, a shell row, the top head with its nozzle.
        var standing = drum(6, 10, Orientation.VERTICAL, Heading.NORTH, 3);
        assertEquals(EnumSet.of(Material.SUPPORT, Material.PIPE, Material.FLANGE), materials(standing, 0, 1));
        assertEquals(EnumSet.of(Material.SUPPORT, Material.HEAD), materials(standing, 2, 3));
        assertEquals(Set.of(Material.SHELL), materials(standing, 5, 6));
        assertTrue(materials(standing, standing.sizeY() - 1, standing.sizeY()).containsAll(Set.of(Material.HEAD, Material.PIPE, Material.FLANGE)));
        // A lying drum's lowest row holds only its saddles.
        assertEquals(Set.of(Material.SUPPORT), materials(drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2), 0, 1));
    }

    @Test
    void everyPieceStaysInsideItsCellAndFacesOutwards() {
        for (var shape : SHAPES)
            for (var cell : ShapeRasterizer.layout(shape).shell()) {
                var polygons = ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()));
                // A cell only a nozzle's largest flange reaches is empty while the nozzle is drawn at its default size; a
                // drum's fluid node inside its skirt is empty too.
                assertTrue(!polygons.isEmpty() || reachedByNozzle(shape, cell) || cell.equals(ShapeRasterizer.fluidCell(shape)), shape + " " + cell);
                for (var polygon : polygons) {
                    assertTrue(polygon.size() >= 3);
                    for (double v : polygon.positions()) assertTrue(v >= 0 && v <= 1, shape + " " + cell + " position " + v);
                    for (double v : polygon.uvs()) assertTrue(v >= 0 && v <= 1, shape + " " + cell + " uv " + v);
                    double[] face = newell(polygon), smooth = new double[3];
                    for (int i = 0; i < polygon.size(); i++) {
                        double nx = polygon.normals()[3 * i], ny = polygon.normals()[3 * i + 1], nz = polygon.normals()[3 * i + 2];
                        assertEquals(1, nx * nx + ny * ny + nz * nz, 1e-9);
                        smooth[0] += nx;
                        smooth[1] += ny;
                        smooth[2] += nz;
                    }
                    assertTrue(face[0] * smooth[0] + face[1] * smooth[1] + face[2] * smooth[2] > 0, shape + " " + cell + " winding " + polygon.material());
                }
            }
    }

    @Test
    void bodyKeyGeometryRepeatsAlongTheAxis() {
        for (var shape : SHAPES) {
            int bodies = 0;
            for (var cell : ShapeRasterizer.layout(shape).shell()) {
                var key = ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z());
                if (axial(key) > ShapeRasterizer.CellKey.BODY) continue;
                bodies++;
                var shared = ShapeRasterizer.surface(key);
                var actual = ShapeRasterizer.surface(shape, cell.x(), cell.y(), cell.z(), 0);
                assertEquals(actual.size(), shared.size(), shape + " " + cell);
                // Clipping may start a polygon at another vertex on another row: equal up to a cyclic shift.
                for (int i = 0; i < actual.size(); i++)
                    assertTrue(sameUpToRotation(actual.get(i), shared.get(i)), shape + " " + cell + " polygon " + i);
            }
            if (shape.tangentLength() >= 9) assertTrue(bodies > 0, shape + " has no body rows");
        }
    }

    @Test
    void interiorIsSealedByTheShell() {
        for (var shape : SHAPES) {
            var layout = ShapeRasterizer.layout(shape);
            var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());
            occupied.addAll(layout.interior());
            for (var cell : layout.interior())
                for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}})
                    assertTrue(occupied.contains(new ShapeRasterizer.Cell(cell.x() + d[0], cell.y() + d[1], cell.z() + d[2])),
                            shape + " interior " + cell + " touches the outside");
        }
    }

    @Test
    void solidMatchesTheFacetedVolume() {
        int s = ShapeRasterizer.SOLID_RESOLUTION;
        for (var shape : SHAPES) {
            var layout = ShapeRasterizer.layout(shape);
            // A column's auxiliaries fill their units' insides too: only the vessel's interior counts here.
            double volume = layout.interior().stream().filter(c -> ShapeRasterizer.insideVessel(shape, c.x() + .5, c.y() + .5, c.z() + .5)).count();
            for (var cell : layout.shell()) {
                int parts = 0, sampled = 0;
                for (var box : ShapeRasterizer.solid(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) {
                    assertTrue(box.x0() < box.x1() && box.x1() <= s && box.y0() < box.y1() && box.y1() <= s && box.z0() < box.z1() && box.z1() <= s);
                    parts += (box.x1() - box.x0()) * (box.y1() - box.y0()) * (box.z1() - box.z0());
                }
                for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) for (int c = 0; c < s; c++)
                    if (ShapeRasterizer.inside(shape, cell.x() + (a + .5) / s, cell.y() + (b + .5) / s, cell.z() + (c + .5) / s)) sampled++;
                assertEquals(sampled, parts, shape + " " + cell + ": boxes overlap or miss samples");
                // The vessel's own solid: a column's platforms, ladders and manways are checked by ColumnStructureTest.
                int vessel = 0;
                for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) for (int c = 0; c < s; c++)
                    if (ShapeRasterizer.insideVessel(shape, cell.x() + (a + .5) / s, cell.y() + (b + .5) / s, cell.z() + (c + .5) / s)) vessel++;
                volume += vessel / (double) (s * s * s);
            }
            // A misclassified sample lies within half a sample diagonal of the surface.
            double exact = facetedVolume(shape) + nozzleVolume(shape), band = ShapeRasterizer.facetArea(shape) * Math.sqrt(3) / (2 * s);
            assertEquals(exact, volume, band, shape.toString());
            // Lattice alignment alone moves a 2 m cross-section by 2 % (16 facets centred on a cell midline sampled +2.1 %).
            if (shape.diameter() >= 2.5) assertEquals(exact, volume, .02 * exact, shape.toString());
        }
    }

    /** A drum's fluid node carries its identity (a block entity), so it is a shell cell even inside a skirt, where nothing is drawn. */
    @Test
    void aDrumsFluidNodeIsAShellCell() {
        for (var shape : SHAPES)
            if (!shape.column()) assertTrue(ShapeRasterizer.layout(shape).shell().contains(ShapeRasterizer.fluidCell(shape)), shape.toString());
    }

    @Test
    void controllerIsTheLowestShellCellThenWestThenNorth() {
        for (var shape : SHAPES) {
            var layout = ShapeRasterizer.layout(shape);
            var controller = layout.controller();
            assertTrue(layout.shell().contains(controller), shape.toString());
            for (var cell : layout.shell()) {
                assertTrue(cell.y() >= controller.y(), shape + " " + cell);
                if (cell.y() == controller.y()) assertTrue(cell.x() > controller.x() || cell.x() == controller.x() && cell.z() >= controller.z(), shape + " " + cell);
            }
        }
    }

    @Test
    void vesselStandsOnTheGroundAndFillsItsBox() {
        for (var shape : SHAPES) {
            // An odd width puts the axis, and every nozzle's centre line, through cell centres.
            assertEquals(1, shape.width() % 2, shape.toString());
            int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, max = {-1, -1, -1};
            for (var cell : ShapeRasterizer.layout(shape).shell()) {
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

    @Test
    void turningTheFrontTurnsTheVessel() {
        // Quarter turns are exact: every heading gives the same cells, sizes and area, with x and z swapped.
        for (var orientation : Orientation.values()) {
            double[] first = null;
            for (var heading : Heading.values()) {
                var shape = drum(2.5, 5, orientation, heading, orientation.horizontal() ? 1 : 2);
                var layout = ShapeRasterizer.layout(shape);
                boolean alongX = heading.dx != 0;
                double[] seen = {layout.shell().size(), layout.interior().size(), shape.sizeY(),
                        alongX ? shape.sizeX() : shape.sizeZ(), alongX ? shape.sizeZ() : shape.sizeX(), ShapeRasterizer.facetArea(shape)};
                if (first == null) first = seen;
                else assertArrayEquals(first, seen, 1e-9, orientation + " " + heading);
            }
        }
    }

    @Test
    void nozzlesEndOnBlockFacesWithRoomForAPipe() {
        for (var shape : SHAPES) {
            var layout = ShapeRasterizer.layout(shape);
            var occupied = new HashSet<ShapeRasterizer.Cell>(layout.shell());
            occupied.addAll(layout.interior());
            var transform = shape.transform();
            for (var port : ShapeRasterizer.ports(shape)) {
                var nozzle = shape.nozzles().get(port.nozzle());
                double[] face = transform.point(nozzle.face()), out = transform.direction(nozzle.direction());
                int[] cell = {port.cell().x(), port.cell().y(), port.cell().z()}, step = {port.dx(), port.dy(), port.dz()};
                for (int a = 0; a < 3; a++) {
                    assertEquals(out[a], step[a], 0, shape + " " + nozzle.role());
                    // On the plane between the port cell and its neighbour, centred on that cell face.
                    double expected = step[a] == 0 ? cell[a] + .5 : cell[a] + (step[a] > 0 ? 1 : 0);
                    assertEquals(expected, face[a], 1e-9, shape + " " + nozzle.role() + " axis " + a);
                }
                assertTrue(occupied.contains(port.cell()), shape + " " + nozzle.role());
                var front = new ShapeRasterizer.Cell(cell[0] + step[0], cell[1] + step[1], cell[2] + step[2]);
                assertFalse(occupied.contains(front), shape + " " + nozzle.role() + ": no room for a pipe at " + front);
                // The flange stands clear of the vessel: just behind the flange, off the neck, is outside.
                double reach = PipeSizes.flange(nozzle.maxOd()) / 2 + .02, behind = PipeSizes.flangeThickness(nozzle.maxOd()) + .02;
                var beside = nozzle.face().clone();
                beside[nozzle.axis()] -= nozzle.sign() * behind;
                beside[(nozzle.axis() + 1) % 3] += reach;
                var p = transform.point(beside);
                assertFalse(ShapeRasterizer.inside(shape, p[0], p[1], p[2]), shape + " " + nozzle.role() + " flange against the vessel");
            }
            assertEquals(shape.column() ? 0 : shape.nozzles().size(), ShapeRasterizer.ports(shape).size(), shape.toString());
        }
    }

    @Test
    void drumNozzlesArePortsByPosition() {
        // Owner decision 2026-10-16: the top nozzle is a vapour port, the lowest a liquid port, the rest bulk ports.
        for (var heading : Heading.values()) {
            var standing = drum(3, 4, Orientation.VERTICAL, heading, 2);
            var lying = drum(3, 6, Orientation.HORIZONTAL, heading, 1);
            for (var shape : List.of(standing, lying)) {
                var ports = ShapeRasterizer.ports(shape);
                assertEquals(Set.of(EquipmentShape.PortClass.values()), Set.copyOf(ports.stream().map(ShapeRasterizer.Port::port).toList()), shape.toString());
                int lowest = Integer.MAX_VALUE, liquid = 0;
                for (var port : ports) {
                    int[] d = {port.dx(), port.dy(), port.dz()};
                    switch (port.port()) {
                        case VAPOUR -> assertArrayEquals(new int[]{0, 1, 0}, d, shape.toString());
                        // A standing drum's bottom outlet leaves its skirt through the access opening on the front (owner
                        // report 2026-10-16); a lying drum's leaves its front head.
                        case LIQUID -> {
                            assertArrayEquals(new int[]{heading.dx, 0, heading.dz}, d, shape.toString());
                            liquid = port.cell().y();
                        }
                        case BULK -> assertArrayEquals(shape.saddles() ? new int[]{0, 1, 0}
                                : new int[]{heading.counterClockwise().dx, 0, heading.counterClockwise().dz}, d, shape.toString());
                    }
                    lowest = Math.min(lowest, port.cell().y());
                }
                assertEquals(lowest, liquid, shape.toString());
            }
        }
    }

    @Test
    void nozzleDrawsAtTheConnectedPipeSize() {
        var standing = drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2);
        int k = 0;
        while (standing.nozzles().get(k).role() != EquipmentShape.Role.LIQUID_OUTLET) k++;
        var nozzle = standing.nozzles().get(k);
        long small = ShapeRasterizer.withNozzle(0, k, .1), medium = ShapeRasterizer.withNozzle(0, k, .3), large = ShapeRasterizer.withNozzle(0, k, 2);
        assertEquals(0, ShapeRasterizer.withNozzle(small, k, 0));
        double[] area = new double[3];
        int[] solid = new int[3];
        long[] codes = {small, medium, large};
        for (var cell : ShapeRasterizer.nozzleCells(standing, k))
            for (int i = 0; i < 3; i++) {
                var key = ShapeRasterizer.key(standing, cell.x(), cell.y(), cell.z(), codes[i]);
                assertEquals(codes[i], key.nozzles(), cell.toString());
                for (var polygon : ShapeRasterizer.surface(key)) area[i] += area(polygon);
                for (var box : ShapeRasterizer.solid(key)) solid[i] += (box.x1() - box.x0()) * (box.y1() - box.y0()) * (box.z1() - box.z0());
            }
        // Bigger pipes draw bigger nozzles: the neck up to its largest size, the flange (and beyond the neck, a reducer) at the pipe's.
        assertTrue(area[0] < area[1] && area[1] < area[2], List.of(area[0], area[1], area[2]).toString());
        assertTrue(solid[0] < solid[1] && solid[1] < solid[2]);
        assertTrue(nozzle.maxOd() < 2);
        // A cell no nozzle reaches drops the codes, so body rows still share one key.
        var top = new ShapeRasterizer.Cell(standing.width() / 2, standing.sizeY() - 1, standing.width() / 2);
        assertFalse(ShapeRasterizer.nozzleCells(standing, k).contains(top));
        assertEquals(ShapeRasterizer.key(standing, top.x(), top.y(), top.z()), ShapeRasterizer.key(standing, top.x(), top.y(), top.z(), large));
    }

    @Test
    void aPipeWiderThanTheNeckMeetsAFlangeOfItsOwnSize() {
        // Owner decision 2026-10-16: resizing starts where the port meets the vessel, never at the flange. A 1 m line on the
        // bottom outlet, whose line fits under the head and through its port at 0.69 m at most: the flange is the line's,
        // and the neck keeps its own size until it has left the skirt, then widens in a reducer.
        var standing = drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2);
        int k = 0;
        while (standing.nozzles().get(k).role() != EquipmentShape.Role.LIQUID_OUTLET) k++;
        var nozzle = standing.nozzles().get(k);
        assertTrue(nozzle.maxOd() < .7, nozzle.toString());
        long line = ShapeRasterizer.withNozzle(0, k, 1);
        double flange = 0, neck = 0, reducer = 0;
        for (var cell : ShapeRasterizer.nozzleCells(standing, k))
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(standing, cell.x(), cell.y(), cell.z(), line)))
                for (int i = 0; i < polygon.size(); i++) {
                    var pos = polygon.positions();
                    var f = standing.transform().local(new double[]{cell.x() + pos[3 * i], cell.y() + pos[3 * i + 1], cell.z() + pos[3 * i + 2]});
                    var base = nozzle.base();
                    double s = (f[nozzle.axis()] - base[nozzle.axis()]) * nozzle.sign(), rho = 0;
                    for (int a = 0; a < 3; a++) if (a != nozzle.axis()) rho += (f[a] - base[a]) * (f[a] - base[a]);
                    rho = Math.sqrt(rho);
                    if (polygon.material() == Material.FLANGE) flange = Math.max(flange, rho);
                    else if (polygon.material() == Material.PIPE && s >= 0)
                        if (s <= nozzle.exit() + 1e-9) neck = Math.max(neck, rho);
                        else reducer = Math.max(reducer, rho);
                }
        assertEquals(PipeSizes.flange(1) / 2, flange, 1e-6);
        assertEquals(nozzle.maxOd() / 2, neck, 1e-6);
        assertEquals(PipeSizes.hubOutside(1) / 2, reducer, 1e-6);
        // The neck has left the skirt where the reducer starts.
        assertTrue(nozzle.base()[2] - nozzle.exit() < standing.axis() - standing.radius() - EquipmentShape.SKIRT_WALL, nozzle.toString());
    }

    @Test
    void skirtShowsItsInsideThroughTheOpeningAndSlots() {
        // Owner reports 2026-10-16: the bottom outlet leaves through its skirt port on the front, centred on the outlet
        // line, the port 5 cm wider than the line drawn, and slots on the left, back and right show the skirt's inside.
        // Sight lines (frame points) between the default line (r 0.15 m) and its port (r 0.2 m), and through each slot's
        // middle, cross nothing; one between the slots meets both faces of the plate.
        double between = Math.toRadians(302.5);
        for (var heading : Heading.values()) {
            var standing = drum(3, 4, Orientation.VERTICAL, heading, 2);
            double a = standing.axis(), out = standing.radius() + .05, slot = 1.1;
            assertEquals(EquipmentShape.SKIRT_OUTLET, standing.openingHeight(), 0);
            assertTrue(standing.openingRadius() > .25, heading.toString());
            assertEquals(0, crossings(standing, new double[]{a + .175, .5, a}, new double[]{a + .175, .5, a - out}), heading + " port");
            for (double[] d : new double[][]{{1, 0}, {0, 1}, {-1, 0}})
                assertEquals(0, crossings(standing, new double[]{a + d[0], slot, a + d[1]}, new double[]{a + out * d[0], slot, a + out * d[1]}),
                        heading + " slot " + d[0] + "," + d[1]);
            double c = Math.cos(between), s = Math.sin(between);
            assertEquals(2, crossings(standing, new double[]{a + c, slot, a + s}, new double[]{a + out * c, slot, a + out * s}), heading + " plate");
            // The outlet line inside the skirt is drawn: its cells are shell cells.
            int k = 0;
            while (standing.nozzles().get(k).role() != EquipmentShape.Role.LIQUID_OUTLET) k++;
            assertTrue(ShapeRasterizer.layout(standing).shell().containsAll(ShapeRasterizer.nozzleCells(standing, k)), heading.toString());
            // A column's skirt keeps its opening's dark back, on its front under the head.
            var column = EquipmentShape.column(3, ColumnTrays.simple(12, 6), heading, 2);
            var axis = column.transform().point(new double[]{column.axis(), 0, column.axis()});
            var centres = openingCentres(column);
            assertFalse(centres.isEmpty(), heading.toString());
            for (var centre : centres) {
                assertTrue((centre[0] - axis[0]) * heading.dx + (centre[2] - axis[2]) * heading.dz > 1, heading + " opening off the front");
                assertTrue(centre[1] > .4 && centre[1] < 1.4, heading + " opening at " + centre[1]);
            }
        }
        // A port only where a bottom outlet line leaves a skirt: none on a bare or lying drum, nor on a skirt too low for the
        // line (D3 on 1 m: its pole at 0.25 m); a small drum's is only its line's (D 0.5: a 0.25 m line plus clearance).
        assertEquals(0, new EquipmentShape(3, 4).openingRadius());
        assertEquals(0, drum(3, 4, Orientation.HORIZONTAL, Heading.NORTH, 2).openingRadius());
        assertEquals(0, drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 1).openingRadius());
        assertEquals(.25 / 2 + EquipmentShape.PORT_CLEARANCE, drum(.5, 4, Orientation.VERTICAL, Heading.NORTH, 1).openingRadius(), 1e-12);
    }

    @Test
    void sideConnectionsAreValidatedAndClash() {
        // A new drum's side connection is its former inlet: 0.4 D below the top tangent line, facing frame -x.
        assertEquals(List.of(new Connection(3, 4, 0)), drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2).connections());
        var placed = List.of(new Connection(3, 4, 0), new Connection(1, 4, 0), new Connection(2, 2, 1));
        assertEquals(placed, Connection.decode(Connection.encode(placed)));
        assertThrows(IllegalArgumentException.class, () -> Connection.decode(new int[]{0, 3}));
        assertThrows(IllegalArgumentException.class, () -> Connection.decode(new int[3 * (EquipmentShape.MAX_CONNECTIONS + 1)]));
        assertThrows(IllegalArgumentException.class, () -> new Connection(EquipmentShape.CONNECTION_SIDES, 3, 0));
        // D 3 m on a 2 m skirt, T/T 4 m: centre lines 0.1 m inside the tangent lines at 2 and 6 m, offsets within 0.85 R.
        assertArrayEquals(new int[]{2, 5}, EquipmentShape.stations(3, 4, Orientation.VERTICAL, 2));
        assertEquals(1, EquipmentShape.maximumOffset(3));
        for (var bad : List.of(new Connection(0, 1, 0), new Connection(0, 6, 0), new Connection(0, 3, 2)))
            assertThrows(IllegalArgumentException.class, () -> drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2, List.of(bad)), bad.toString());
        assertThrows(IllegalArgumentException.class, () -> drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2,
                java.util.Collections.nCopies(EquipmentShape.MAX_CONNECTIONS + 1, new Connection(0, 3, 0))));
        var trays = ColumnTrays.simple(12, 6);
        assertThrows(IllegalArgumentException.class, () -> new EquipmentShape(EquipmentShape.Kind.COLUMN, 3, trays.tangentLength(3), Orientation.VERTICAL,
                Heading.NORTH, 2, trays, ColumnOptions.DEFAULT, List.of(new Connection(0, 3, 0))));
        // Right, left and a hillside nozzle at the back: clear of each other and of the outlets, the hillside one off centre.
        var drum = drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2, placed);
        assertEquals(List.of(), drum.clashes());
        var nozzles = drum.nozzles();
        assertEquals(5, nozzles.size());
        assertEquals(5, ShapeRasterizer.ports(drum).size());
        var hillside = nozzles.get(4);
        assertEquals(EquipmentShape.Role.SIDE, hillside.role());
        assertEquals(drum.axis() + 1, hillside.base()[0], 0);
        assertEquals(2.5, hillside.base()[1], 0);
        // Two on one place; one beside the bare drum's liquid outlet; one on a saddle; one facing a ground too near.
        assertEquals(List.of(new Clash(1, 0)), drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2, List.of(new Connection(1, 3, 0), new Connection(1, 3, 0))).clashes());
        assertEquals(List.of(new Clash(0, Clash.LIQUID)), drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 0, List.of(new Connection(0, 1, 0))).clashes());
        assertTrue(drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2, List.of(new Connection(3, 2, 0))).clashes().contains(new Clash(0, Clash.SADDLE)));
        assertEquals(List.of(new Clash(0, Clash.GROUND)), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 1, List.of(new Connection(3, 5, 0))).clashes());
        assertEquals(List.of(), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2, List.of(new Connection(3, 5, 0))).clashes());
    }

    @Test
    void outlineFollowsTheViewer() {
        // Owner report 2026-10-16: a fixed meridian down the drum's front cut it in half. The silhouette's two meridians
        // stand where the sight lines touch the shell, within a facet of φ ± acos(R / d), and turn with the viewer.
        var standing = drum(3, 4, Orientation.VERTICAL, Heading.EAST, 2);
        double a = standing.axis(), facet = 360.0 / standing.facets();
        for (double[] eye : new double[][]{{0, -20}, {20, 0}, {-14, 14}}) {
            double d = Math.hypot(eye[0], eye[1]), towards = Math.toDegrees(Math.atan2(eye[1], eye[0])), spread = Math.toDegrees(Math.acos(standing.outerRadius() / d));
            var p = standing.transform().point(new double[]{a + eye[0], 3, a + eye[1]});
            var meridians = new java.util.ArrayList<Double>();
            for (var s : ShapeRasterizer.outline(standing, p[0], p[1], p[2]))
                if (Math.abs(s[4] - s[1]) >= standing.tangentLength()) {
                    var f = standing.transform().local(new double[]{s[0], s[1], s[2]});
                    meridians.add(Math.toDegrees(Math.atan2(f[2] - a, f[0] - a)));
                }
            assertEquals(2, meridians.size(), List.of(eye[0], eye[1]).toString());
            for (double side : new double[]{-1, 1}) {
                double expected = towards + side * spread;
                assertTrue(meridians.stream().anyMatch(m -> Math.abs(Math.IEEEremainder(m - expected, 360)) <= facet / 2 + 1e-9), meridians + " for " + expected);
            }
        }
    }

    @Test
    void supportsCarryTheVessel() {
        // A skirt continues the shell to the ground on a base ring 1/8 m wider; a bare vessel touches it only at its lower apex.
        var skirted = drum(6, 10, Orientation.VERTICAL, Heading.NORTH, 3);
        double a = skirted.axis();
        assertEquals(7, skirted.width());
        assertEquals(3 + 10 + 1.5, skirted.topElevation(), 0);
        assertEquals(12, skirted.anchorChairs());
        // A drum's skirt is drawn hollow but stays solid to collision, so nothing spawns or is placed inside it.
        assertTrue(in(skirted, a - 2.8, .5, a));
        assertFalse(in(skirted, a - 3.05, .5, a));
        assertTrue(in(skirted, a + 3.1, .03, a));
        assertFalse(in(skirted, a + 3.2, .03, a));
        var bare = new EquipmentShape(6, 10);
        assertFalse(in(bare, bare.axis() + 2.8, .5, bare.axis()));
        // Saddles a fifth of T/T in from each tangent line, under the axis; nothing under the vessel between them.
        for (var lying : List.of(drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2), drum(4, 12, Orientation.HORIZONTAL, Heading.EAST, 1))) {
            double bottom = lying.bottomTangent(), length = lying.tangentLength(), under = -lying.support() / 2.0;
            assertEquals(lying.support() + lying.width() / 2.0 + lying.radius(), lying.topElevation(), 0);
            for (double along : new double[]{bottom + .2 * length, bottom + .8 * length, bottom + .5 * length}) {
                boolean saddle = along != bottom + .5 * length;
                assertEquals(saddle, in(lying, under, along, lying.axis()), lying + " at " + along);
            }
        }
    }

    @Test
    void datasheetVolumeAndAreaBoundTheFacetedVessel() {
        // D 2 m, T/T 3 m: π·1·3 + π·8/12 = 11π/3 m³; 6π + 8 × 1.08398 m².
        var drum = new EquipmentShape(2, 3);
        assertEquals(11 * Math.PI / 3, drum.volume(), 1e-12);
        assertEquals(1.08398, EquipmentShape.HEAD_AREA_FACTOR, 1e-5);
        assertEquals(6 * Math.PI + 8 * EquipmentShape.HEAD_AREA_FACTOR, drum.surfaceArea(), 1e-12);
        assertEquals(4, drum.overallLength(), 0);
        assertEquals(4, drum.topElevation(), 0);
        // The liquid head stands over the apex-to-apex height, or a lying drum's diameter.
        assertEquals(4, drum.levelHeight(), 0);
        assertEquals(2, drum(2, 3, Orientation.HORIZONTAL, Heading.NORTH, 1).levelHeight(), 0);
        // The faceted vessel is inscribed in the smooth one: smaller, and closer for finer facets. Supports are not vessel.
        for (var shape : SHAPES) {
            double volume = vesselVolume(shape), area = vesselArea(shape);
            assertTrue(volume < shape.volume() && volume > .88 * shape.volume(), shape + " volume " + volume + " of " + shape.volume());
            assertTrue(area < shape.surfaceArea() && area > .9 * shape.surfaceArea(), shape + " area " + area + " of " + shape.surfaceArea());
        }
    }

    /** A frame point inside the solid. */
    private static boolean in(EquipmentShape shape, double x, double y, double z) {
        var p = shape.transform().point(new double[]{x, y, z});
        return ShapeRasterizer.inside(shape, p[0], p[1], p[2]);
    }

    private static int axial(ShapeRasterizer.CellKey key) {
        var along = key.shape().transform().direction(new double[]{0, 1, 0});
        return along[0] != 0 ? key.x() : along[1] != 0 ? key.y() : key.z();
    }

    private static boolean reachedByNozzle(EquipmentShape shape, ShapeRasterizer.Cell cell) {
        for (int k = 0; k < shape.nozzles().size(); k++) if (ShapeRasterizer.nozzleCells(shape, k).contains(cell)) return true;
        return false;
    }

    /** Surface pieces the segment between frame points {@code from} and {@code to} crosses. */
    private static int crossings(EquipmentShape shape, double[] from, double[] to) {
        double[] p = shape.transform().point(from), q = shape.transform().point(to), d = {q[0] - p[0], q[1] - p[1], q[2] - p[2]};
        int count = 0;
        for (var cell : ShapeRasterizer.layout(shape).shell())
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) {
                // Newell's normal, the plane through the first vertex, and the crossing point in cell coordinates.
                double[] pos = polygon.positions(), n = new double[3];
                int size = polygon.size();
                for (int i = 0; i < size; i++) {
                    int j = (i + 1) % size;
                    n[0] += (pos[3 * i + 1] - pos[3 * j + 1]) * (pos[3 * i + 2] + pos[3 * j + 2]);
                    n[1] += (pos[3 * i + 2] - pos[3 * j + 2]) * (pos[3 * i] + pos[3 * j]);
                    n[2] += (pos[3 * i] - pos[3 * j]) * (pos[3 * i + 1] + pos[3 * j + 1]);
                }
                double[] o = {p[0] - cell.x(), p[1] - cell.y(), p[2] - cell.z()};
                double denominator = n[0] * d[0] + n[1] * d[1] + n[2] * d[2];
                if (Math.abs(denominator) < 1e-12) continue;
                double t = (n[0] * (pos[0] - o[0]) + n[1] * (pos[1] - o[1]) + n[2] * (pos[2] - o[2])) / denominator;
                if (t < 0 || t > 1) continue;
                double[] x = {o[0] + t * d[0], o[1] + t * d[1], o[2] + t * d[2]};
                // Even-odd test in the plane of the normal's two smaller components.
                int drop = Math.abs(n[0]) >= Math.abs(n[1]) && Math.abs(n[0]) >= Math.abs(n[2]) ? 0 : Math.abs(n[1]) >= Math.abs(n[2]) ? 1 : 2, u = (drop + 1) % 3, v = (drop + 2) % 3;
                boolean in = false;
                for (int i = 0, j = size - 1; i < size; j = i++) {
                    double ui = pos[3 * i + u], vi = pos[3 * i + v], uj = pos[3 * j + u], vj = pos[3 * j + v];
                    if ((vi > x[v]) != (vj > x[v]) && x[u] < (uj - ui) * (x[v] - vi) / (vj - vi) + ui) in = !in;
                }
                if (in) count++;
            }
        return count;
    }

    /** Box positions of the access opening's dark back, one per polygon piece. */
    private static List<double[]> openingCentres(EquipmentShape shape) {
        var out = new java.util.ArrayList<double[]>();
        for (var cell : ShapeRasterizer.layout(shape).shell())
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z())))
                if (polygon.material() == Material.OPENING) {
                    var c = new double[]{cell.x(), cell.y(), cell.z()};
                    for (int i = 0; i < polygon.size(); i++) for (int a = 0; a < 3; a++) c[a] += polygon.positions()[3 * i + a] / polygon.size();
                    out.add(c);
                }
        return out;
    }

    /** Area of the faceted pressure vessel: the N-gon prism over T/T and two banded heads. */
    private static double vesselArea(EquipmentShape shape) {
        return shellArea(shape, shape.tangentLength()) + 2 * headArea(shape);
    }

    private static double vesselVolume(EquipmentShape shape) {
        return prismVolume(shape, shape.tangentLength()) + 2 * headVolume(shape);
    }

    /**
     * Volume of the faceted solid without nozzles: a skirt's prism to the ground, its base ring (an annular N-gon prism
     * 1/8 m wider) and chairs; or the vessel and two saddles, each a top plate around the contact sector, a web and two
     * ribs under the top plate's outer edge, and a base plate. The skirt's access opening is drawn but not cut from it.
     */
    private static double facetedVolume(EquipmentShape shape) {
        int n = shape.facets();
        double sin = DeterministicMath.sin(2 * Math.PI / n), cos = DeterministicMath.cos(Math.PI / n), r = shape.radius();
        if (shape.skirt()) {
            double ring = r + EquipmentShape.BASE_RING / cos;
            // A column's shell and top head are clad: the skirt stands at the shell radius, the cladding over it.
            return prismVolume(shape, shape.support(), r) + prismVolume(shape, shape.tangentLength(), shape.outerRadius())
                    + headVolume(shape, rings(shape, shape.outerRadius(), shape.outerHeadDepth()))
                    + EquipmentShape.PLATE * n / 2.0 * sin * (ring * ring - r * r)
                    + shape.anchorChairs() * EquipmentShape.BASE_RING * EquipmentShape.CHAIR * EquipmentShape.CHAIR;
        }
        if (!shape.saddles()) return vesselVolume(shape);
        int m = (int) Math.round(n * EquipmentShape.SADDLE_CONTACT_DEGREES / 720);
        double outer = r + EquipmentShape.PLATE / cos, lateral = outer * DeterministicMath.sin(2 * Math.PI * m / n), web = lateral - EquipmentShape.SADDLE_RIB;
        double width = EquipmentShape.SADDLE_WIDTH;
        double saddle = width * m * sin * (outer * outer - r * r)
                + EquipmentShape.SADDLE_WEB * underChain(shape, -web, web) + 2 * width * underChain(shape, web, lateral)
                + width * 2 * lateral * EquipmentShape.PLATE;
        return vesselVolume(shape) + 2 * saddle;
    }

    /**
     * The unconnected nozzles outside the vessel: each neck from the vessel's surface on its centre line to the back of
     * its flange, and the flange. The surface across the neck differs from that on its centre line by a sliver.
     */
    private static double nozzleVolume(EquipmentShape shape) {
        double total = 0, a = shape.axis();
        for (var nozzle : shape.nozzles()) {
            double od = nozzle.defaultOd(), t = PipeSizes.flangeThickness(od), flange = PipeSizes.flange(od) / 2;
            var base = nozzle.base();
            double surface;
            if (nozzle.axis() != 1) surface = a + nozzle.sign() * shape.outerRadius(); // a shell (cladding) vertex ridge
            else {
                double off = headOffset(shape, Math.hypot(base[0] - a, base[2] - a)); // the head, on a vertex meridian
                surface = nozzle.sign() > 0 ? shape.topTangent() + off : shape.bottomTangent() - off;
            }
            double depth = (surface - base[nozzle.axis()]) * nozzle.sign();
            total += Math.PI * od * od / 4 * (nozzle.length() - t - depth) + Math.PI * flange * flange * t;
        }
        return total;
    }

    /** Distance from the tangent line at which the faceted head's circumradius falls to {@code e}. */
    private static double headOffset(EquipmentShape shape, double e) {
        var rings = rings(shape, shape.outerRadius(), shape.outerHeadDepth());
        for (int k = 0; k + 1 < rings.length; k++)
            if (e >= rings[k + 1][1]) return rings[k][0] + (rings[k + 1][0] - rings[k][0]) * (rings[k][1] - e) / (rings[k][1] - rings[k + 1][1]);
        return shape.headDepth();
    }

    /** Area between a saddle's deck and its top plate's outer edge, over lateral offsets [low, high] from the axis. */
    private static double underChain(EquipmentShape shape, double low, double high) {
        int n = shape.facets(), m = (int) Math.round(n * EquipmentShape.SADDLE_CONTACT_DEGREES / 720);
        double outer = shape.radius() + EquipmentShape.PLATE / DeterministicMath.cos(Math.PI / n), area = 0;
        for (int j = n / 2 - m; j < n / 2 + m; j++) {
            double z0 = outer * DeterministicMath.sin(2 * Math.PI * j / n), z1 = outer * DeterministicMath.sin(2 * Math.PI * (j + 1) / n);
            double h0 = shape.support() + shape.width() / 2.0 + outer * DeterministicMath.cos(2 * Math.PI * j / n) - EquipmentShape.PLATE;
            double h1 = shape.support() + shape.width() / 2.0 + outer * DeterministicMath.cos(2 * Math.PI * (j + 1) / n) - EquipmentShape.PLATE;
            // The edge runs from z0 down to z1 < z0; take its part within [low, high].
            double a = Math.max(z1, low), b = Math.min(z0, high);
            if (b <= a) continue;
            double ha = h1 + (h0 - h1) * (a - z1) / (z0 - z1), hb = h1 + (h0 - h1) * (b - z1) / (z0 - z1);
            area += (b - a) * (ha + hb) / 2;
        }
        return area;
    }

    private static Set<Material> materials(EquipmentShape shape, int low, int high) {
        var found = EnumSet.noneOf(Material.class);
        for (var cell : ShapeRasterizer.layout(shape).shell())
            if (cell.y() >= low && cell.y() < high)
                for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) found.add(polygon.material());
        return found;
    }

    private static double shellArea(EquipmentShape shape, double length) {
        int n = shape.facets();
        return n * 2 * shape.radius() * DeterministicMath.sin(Math.PI / n) * length;
    }

    private static double prismVolume(EquipmentShape shape, double length) {
        return prismVolume(shape, length, shape.radius());
    }

    private static double prismVolume(EquipmentShape shape, double length, double r) {
        int n = shape.facets();
        return n / 2.0 * DeterministicMath.sin(2 * Math.PI / n) * r * r * length;
    }

    private static double headArea(EquipmentShape shape) {
        int n = shape.facets();
        double sin = DeterministicMath.sin(Math.PI / n), cos = DeterministicMath.cos(Math.PI / n), area = 0;
        double[][] rings = rings(shape);
        for (int k = 0; k + 1 < rings.length; k++) {
            double dh = rings[k + 1][0] - rings[k][0], dr = (rings[k][1] - rings[k + 1][1]) * cos;
            area += n * (rings[k][1] + rings[k + 1][1]) * sin * Math.sqrt(dh * dh + dr * dr);
        }
        return area;
    }

    private static double headVolume(EquipmentShape shape) {
        return headVolume(shape, rings(shape));
    }

    private static double headVolume(EquipmentShape shape, double[][] rings) {
        int n = shape.facets();
        double polygon = n / 2.0 * DeterministicMath.sin(2 * Math.PI / n), volume = 0; // area of the N-gon of unit circumradius
        for (int k = 0; k + 1 < rings.length; k++) {
            double a = polygon * rings[k][1] * rings[k][1], b = polygon * rings[k + 1][1] * rings[k + 1][1];
            volume += (rings[k + 1][0] - rings[k][0]) / 3 * (a + b + Math.sqrt(a * b));
        }
        return volume;
    }

    /** The steel head's rings. */
    private static double[][] rings(EquipmentShape shape) {
        return rings(shape, shape.radius(), shape.headDepth());
    }

    /** Rings of a head of radius {@code r} and depth {@code depth}: a column's clad head has the outer ones. */
    private static double[][] rings(EquipmentShape shape, double r, double depth) {
        int bands = shape.bands();
        var rings = new double[bands + 1][];
        for (int k = 0; k <= bands; k++) {
            double phi = Math.PI / 2 * k / bands;
            rings[k] = new double[]{depth * DeterministicMath.sin(phi), k == bands ? 0 : r * DeterministicMath.cos(phi)};
        }
        return rings;
    }

    private static boolean sameUpToRotation(Mesh.Polygon a, Mesh.Polygon b) {
        int m = a.size();
        if (m != b.size() || a.material() != b.material()) return false;
        rotation:
        for (int r = 0; r < m; r++) {
            for (int i = 0; i < m; i++) {
                int j = (i + r) % m;
                for (int c = 0; c < 3; c++)
                    if (Math.abs(a.positions()[3 * i + c] - b.positions()[3 * j + c]) > 1e-12
                            || Math.abs(a.normals()[3 * i + c] - b.normals()[3 * j + c]) > 1e-12) continue rotation;
                for (int c = 0; c < 2; c++)
                    if (Math.abs(a.uvs()[2 * i + c] - b.uvs()[2 * j + c]) > 1e-12) continue rotation;
            }
            return true;
        }
        return false;
    }

    private static double area(Mesh.Polygon polygon) {
        var n = newell(polygon);
        return .5 * Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
    }

    private static double[] newell(Mesh.Polygon polygon) {
        double[] p = polygon.positions(), n = new double[3];
        int m = polygon.size();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            n[0] += (p[3 * i + 1] - p[3 * j + 1]) * (p[3 * i + 2] + p[3 * j + 2]);
            n[1] += (p[3 * i + 2] - p[3 * j + 2]) * (p[3 * i] + p[3 * j]);
            n[2] += (p[3 * i] - p[3 * j]) * (p[3 * i + 1] + p[3 * j + 1]);
        }
        return n;
    }
}
