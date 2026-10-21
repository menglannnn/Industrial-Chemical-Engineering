package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Clash;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Connection;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.icheme.science.equipment.Mesh.Material;
import com.wormzjl.icheme.science.math.DeterministicMath;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wormzjl.icheme.science.equipment.EquipmentShape.drum;
import static org.junit.jupiter.api.Assertions.*;

class ShapeRasterizerTest {
    @Test
    void aDedicatedServerLaysOutKeysAndCollidesWithoutFacets() {
        // ShapeRasterizer.serverOnly (2026-10-16): a dedicated server drops a geometry's facets once it is laid out. Its
        // layout, cell keys, ports and solids must be the drawing side's, and it must refuse to draw a cell.
        var shapes = List.of(drum(3, 9, Orientation.VERTICAL, Heading.NORTH, 2), drum(2, 6, Orientation.HORIZONTAL, Heading.EAST, 1),
                EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.NORTH, 3), EquipmentShape.unit(Kind.AIR_COOLER, 3, 6, Heading.NORTH, 4));
        var drawn = new java.util.ArrayList<Object>();
        for (var shape : shapes) drawn.add(serverView(shape));
        try {
            ShapeRasterizer.drawing(false);
            for (int k = 0; k < shapes.size(); k++) assertEquals(drawn.get(k), serverView(shapes.get(k)), shapes.get(k).toString());
            var cell = ShapeRasterizer.layout(shapes.getFirst()).shell().getFirst();
            assertThrows(IllegalStateException.class, () -> ShapeRasterizer.surface(ShapeRasterizer.key(shapes.getFirst(), cell.x(), cell.y(), cell.z())));
        } finally {
            ShapeRasterizer.drawing(true);
        }
    }

    /** What a server reads of a vessel: its layout, each shell cell's key and solid with a 0.1 m pipe on every nozzle, and its ports. */
    private static List<Object> serverView(EquipmentShape shape) {
        var layout = ShapeRasterizer.layout(shape);
        var keys = new java.util.ArrayList<Object>();
        var piped = new double[shape.nozzles().size()];
        java.util.Arrays.fill(piped, .1);
        for (var cell : layout.shell()) {
            var key = ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z(), ShapeRasterizer.cellCodes(shape, cell, piped));
            keys.add(List.of(key, ShapeRasterizer.solid(key)));
        }
        return List.of(layout, keys, ShapeRasterizer.ports(shape));
    }

    /**
     * Bare drums (small, odd, half-metre and largest), drums on skirts, lying drums on saddles facing each way, columns
     * (a crude column with two pumparounds on its least skirt, and the smallest diameter with four pumparounds, two draws
     * and steam), and units at their smallest, odd and largest sizes. (0.5, 5) lying puts a saddle face exactly on a cell
     * plane; (10, 60) is the report's largest vessel.
     */
    private static final List<EquipmentShape> SHAPES = List.of(new EquipmentShape(.5, 1), new EquipmentShape(2, 3),
            new EquipmentShape(2.5, 2), new EquipmentShape(3, 9), new EquipmentShape(4, 5), new EquipmentShape(10, 60),
            drum(2, 3, Orientation.VERTICAL, Heading.NORTH, 2), drum(6, 10, Orientation.VERTICAL, Heading.EAST, 3),
            drum(2.5, 2, Orientation.HORIZONTAL, Heading.WEST, 1), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2),
            drum(.5, 5, Orientation.HORIZONTAL, Heading.SOUTH, 1), drum(4, 12, Orientation.HORIZONTAL, Heading.EAST, 1),
            drum(3, 9, Orientation.HORIZONTAL, Heading.EAST, EquipmentShape.minimumBootSupport(3, 9),
                    EquipmentShape.defaultConnections(3, 9, Orientation.HORIZONTAL, EquipmentShape.minimumBootSupport(3, 9), true), true),
            drum(1, 4, Orientation.HORIZONTAL, Heading.SOUTH, EquipmentShape.minimumBootSupport(1, 4), List.of(), true),
            EquipmentShape.column(3, ColumnTrays.simple(12, 6), Heading.SOUTH, 2),
            EquipmentShape.column(1.5, new ColumnTrays(25, 13, List.of(8), List.of(26), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(15, 13))),
                    Heading.EAST, EquipmentShape.minimumSkirt(1.5)),
            EquipmentShape.column(.5, new ColumnTrays(30, 15, List.of(8, 20), List.of(31), List.of(new ColumnTrays.Pumparound(6, 4),
                    new ColumnTrays.Pumparound(12, 10), new ColumnTrays.Pumparound(18, 16), new ColumnTrays.Pumparound(26, 24))), Heading.NORTH, EquipmentShape.minimumSkirt(.5)),
            EquipmentShape.unit(Kind.EXCHANGER, .5, 2, Heading.NORTH, 2), EquipmentShape.unit(Kind.EXCHANGER, .75, 3, Heading.EAST, 2),
            EquipmentShape.unit(Kind.EXCHANGER, 2.5, 12, Heading.WEST, 3), EquipmentShape.unit(Kind.KETTLE, .5, 2, Heading.SOUTH, 2),
            EquipmentShape.unit(Kind.KETTLE, 2, 10, Heading.EAST, 2), EquipmentShape.unit(Kind.AIR_COOLER, 3, 6, Heading.NORTH, 5),
            EquipmentShape.unit(Kind.EJECTOR, .1, 1, Heading.NORTH, 1), EquipmentShape.unit(Kind.EJECTOR, .3, 1, Heading.EAST, 5),
            EquipmentShape.unit(Kind.EJECTOR, 1, 1, Heading.WEST, 2), EquipmentShape.unit(Kind.PUMP, .05, 1, Heading.NORTH, 0),
            EquipmentShape.unit(Kind.PUMP, .4, 1, Heading.EAST, 0), EquipmentShape.unit(Kind.COMPRESSOR, .15, 2, Heading.SOUTH, ProcessUnit.minSupport(Kind.COMPRESSOR, .15, 2)),
            EquipmentShape.unit(Kind.COMPRESSOR, .8, 6, Heading.NORTH, ProcessUnit.minSupport(Kind.COMPRESSOR, .8, 6)),
            EquipmentShape.unit(Kind.VERTICAL_HEATER, 4, 8, Heading.NORTH, 3), EquipmentShape.unit(Kind.CABIN_HEATER, 6, 12, Heading.WEST, 3));

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
        assertThrows(IllegalArgumentException.class, () -> new EquipmentShape(EquipmentShape.Kind.COLUMN, 3, 4, Orientation.VERTICAL, Heading.NORTH, 2, ColumnTrays.simple(12, 6), List.of(),
                ProcessUnit.Bank.ONE, false));
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
            // Every facet is drawn once, whole: clipping to cells and splitting at texture tiles neither lose nor double area,
            // but for a few slivers under Mesh.MIN_AREA, which are not drawn.
            double facets = ShapeRasterizer.facetArea(shape);
            assertEquals(facets, area, 1e-9 * facets + 20 * Mesh.MIN_AREA, shape.toString());
            if (shape.drum() && !shape.skirt() && !shape.saddles())
                assertEquals(vesselArea(shape), facets - ShapeRasterizer.nozzleArea(shape) - manwayArea(shape), 1e-9 * facets, shape.toString());
        }
    }

    @Test
    void partsCarryTheirMaterials() {
        // Skirt row 0 (base ring, chairs, floor plate, the bottom outlet with its flange and bolts leaving through the access
        // opening, which a drum leaves open), the skirt row under the head seam (the head inside it, the band over the seam,
        // the vents' dark bores), a shell row, the top head with its nozzle.
        var standing = drum(6, 10, Orientation.VERTICAL, Heading.NORTH, 3);
        assertEquals(EnumSet.of(Material.SUPPORT, Material.PIPE, Material.FLANGE, Material.BOLT), materials(standing, 0, 1));
        assertEquals(EnumSet.of(Material.SUPPORT, Material.HEAD, Material.FLANGE, Material.OPENING), materials(standing, 2, 3));
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
                // drum's fluid node inside its skirt is empty too, and so is a cell only a ladder's climb zone reaches.
                assertTrue(!polygons.isEmpty() || reachedByNozzle(shape, cell) || cell.equals(ShapeRasterizer.fluidCell(shape)) || reachedByClimb(shape, cell), shape + " " + cell);
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
            // A long drum has plain rows; a column only where its platforms and nozzles leave some, a unit none.
            if (shape.drum() && shape.tangentLength() >= 9) assertTrue(bodies > 0, shape + " has no body rows");
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
            // A unit has no vessel formula: its boxes match its samples above, its parts are checked by ProcessUnitTest.
            if (shape.unit()) continue;
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
            if (shape.drum()) assertTrue(ShapeRasterizer.layout(shape).shell().contains(ShapeRasterizer.fluidCell(shape)), shape.toString());
            else assertNull(ShapeRasterizer.fluidCell(shape), shape.toString());
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
            ProcessUnitTest.pipesApart(shape);
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
            assertEquals(shape.nozzles().size(), ShapeRasterizer.ports(shape).size(), shape.toString());
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
        double[][] outside = new double[3][standing.nozzles().size()];
        outside[0][k] = .1;
        outside[1][k] = .3;
        outside[2][k] = 2;
        double[] area = new double[3];
        int[] solid = new int[3];
        long large = 0;
        for (var cell : ShapeRasterizer.nozzleCells(standing, k)) {
            // Unconnected nozzles carry no code.
            assertEquals(0, ShapeRasterizer.cellCodes(standing, cell, new double[standing.nozzles().size()]), cell.toString());
            for (int i = 0; i < 3; i++) {
                long codes = ShapeRasterizer.cellCodes(standing, cell, outside[i]);
                assertNotEquals(0, codes, cell.toString());
                if (i == 2) large = codes;
                var key = ShapeRasterizer.key(standing, cell.x(), cell.y(), cell.z(), codes);
                assertEquals(codes, key.nozzles(), cell.toString());
                for (var polygon : ShapeRasterizer.surface(key)) area[i] += area(polygon);
                for (var box : ShapeRasterizer.solid(key)) solid[i] += (box.x1() - box.x0()) * (box.y1() - box.y0()) * (box.z1() - box.z0());
            }
        }
        // Bigger pipes draw bigger nozzles: the neck up to its largest size, the flange (and beyond the neck, a reducer) at the pipe's.
        assertTrue(area[0] < area[1] && area[1] < area[2], List.of(area[0], area[1], area[2]).toString());
        assertTrue(solid[0] < solid[1] && solid[1] < solid[2]);
        assertTrue(nozzle.maxOd() < 2);
        // A cell no nozzle reaches drops any codes, so body rows still share one key.
        int unreached = 0;
        for (var cell : ShapeRasterizer.layout(standing).shell())
            if (!reachedByNozzle(standing, cell)) {
                unreached++;
                assertEquals(ShapeRasterizer.key(standing, cell.x(), cell.y(), cell.z()), ShapeRasterizer.key(standing, cell.x(), cell.y(), cell.z(), large), cell.toString());
            }
        assertTrue(unreached > 0);
    }

    /**
     * Owner request 2026-10-16: every nozzle takes the size of its pipe, also where several reach one cell. A cell's key
     * keeps one code per reaching nozzle in nozzle order, so two nozzles in one cell draw at their own pipes' sizes; a
     * column's nozzles scale too, though only a drum is a reservoir.
     */
    @Test
    void nozzlesSharingACellEachKeepTheirPipeSize() {
        for (var shape : SHAPES) {
            int n = shape.nozzles().size();
            var shared = new java.util.HashMap<ShapeRasterizer.Cell, List<Integer>>();
            for (int k = 0; k < n; k++)
                for (var cell : ShapeRasterizer.nozzleCells(shape, k)) shared.computeIfAbsent(cell, c -> new java.util.ArrayList<>()).add(k);
            for (var entry : shared.entrySet()) {
                var cell = entry.getKey();
                var reaching = entry.getValue();
                assertTrue(reaching.size() <= 9, shape + " " + cell + " reached by " + reaching.size());
                // Each nozzle alone at a pipe of 0.3 m sets its own seven bits only.
                for (int j = 0; j < reaching.size(); j++) {
                    double[] outside = new double[n];
                    outside[reaching.get(j)] = .3;
                    long codes = ShapeRasterizer.cellCodes(shape, cell, outside);
                    assertEquals(codes, codes & (0x7FL << (7 * j)), shape + " " + cell + " nozzle " + reaching.get(j));
                    assertNotEquals(0, codes);
                }
            }
            // A column's nozzles are ports that scale, but only a drum is a reservoir.
            if (shape.column()) assertTrue(n > 0 && ShapeRasterizer.ports(shape).size() == n, shape.toString());
        }
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
        double[] line = new double[standing.nozzles().size()];
        line[k] = 1;
        double flange = 0, neck = 0, reducer = 0;
        for (var cell : ShapeRasterizer.nozzleCells(standing, k))
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(standing, cell.x(), cell.y(), cell.z(), ShapeRasterizer.cellCodes(standing, cell, line))))
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
    void bottomLineRisesWithTheSkirt() {
        // Owner request 2026-10-16 ("1m pipe on the bottom of drum & column still looks weird"), decided: the line rises
        // with the skirt. D 3 on 2 m: the head's low point (1.25 m) keeps the line on the lowest row, 0.69 m, its port on
        // the base plate; on 4 m it carries a full 1 m line one row up; D 2 on 3 m (low point 2.5 m): one row up, 0.95 m,
        // as large as its elbow fits.
        assertBottomLine(drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2), .5, .6925);
        assertBottomLine(drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 4), 1.5, 1);
        assertBottomLine(drum(2, 4, Orientation.VERTICAL, Heading.NORTH, 3), 1.5, .95);
        // Every skirted drum and column: the line runs on a cell centre row, its flange clears grade, its port the base
        // plate and the head seam, its elbow fits under the head, and a taller skirt never carries a smaller line.
        for (boolean column : new boolean[]{false, true})
            for (double d = EquipmentShape.MIN_DIAMETER; d <= (column ? EquipmentShape.MAX_COLUMN_DIAMETER : EquipmentShape.MAX_DIAMETER); d += .5) {
                double last = 0;
                for (int s = EquipmentShape.minimumSkirt(d); s <= EquipmentShape.MAX_SUPPORT; s++) {
                    var shape = column ? EquipmentShape.column(d, ColumnTrays.simple(12, 6), Heading.NORTH, s) : drum(d, 4, Orientation.VERTICAL, Heading.NORTH, s);
                    var line = shape.nozzles().stream().filter(n -> n.riser() > 0).findFirst();
                    assertEquals(line.isPresent(), shape.openingRadius() > 0, shape.toString());
                    if (line.isEmpty()) continue;
                    double c = line.get().base()[1], od = line.get().maxOd(), pole = shape.bottomTangent() - shape.headDepth();
                    String at = shape + " line " + od + " at " + c;
                    assertEquals(shape.openingHeight(), c, 0, at);
                    assertEquals(.5, c - Math.floor(c), 0, at);
                    assertTrue(c - PipeSizes.flange(od) / 2 > 0, at);
                    assertTrue(c - od / 2 - EquipmentShape.PORT_CLEARANCE - EquipmentShape.PORT_COLLAR >= EquipmentShape.PLATE, at);
                    assertTrue(c + od / 2 + EquipmentShape.PORT_CLEARANCE + EquipmentShape.PORT_COLLAR <= shape.bottomTangent(), at);
                    assertTrue(c + od <= pole, at);
                    assertTrue(od >= last - 1e-12, at);
                    last = od;
                }
            }
    }

    private static void assertBottomLine(EquipmentShape shape, double height, double od) {
        var line = shape.nozzles().stream().filter(n -> n.role() == EquipmentShape.Role.LIQUID_OUTLET).findFirst().orElseThrow();
        assertEquals(height, line.base()[1], 0, shape.toString());
        assertEquals(od, line.maxOd(), 1e-12, shape.toString());
        assertEquals(od / 2 + EquipmentShape.PORT_CLEARANCE, shape.openingRadius(), 1e-12, shape.toString());
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
            // Owner request 2026-10-16: a column's bottoms leave through its skirt's port on the front, like the drum's
            // outlet, and a sight line between the default line and the port crosses nothing.
            var column = EquipmentShape.column(3, ColumnTrays.simple(12, 6), heading, 2);
            var bottoms = column.nozzles().stream().filter(n -> n.role() == EquipmentShape.Role.BOTTOMS).findFirst().orElseThrow();
            assertEquals(EquipmentShape.SKIRT_OUTLET, bottoms.base()[1], 0, heading.toString());
            assertEquals(2, bottoms.axis());
            assertEquals(-1, bottoms.sign());
            assertTrue(column.openingRadius() > bottoms.maxOd() / 2, heading.toString());
            double c0 = column.axis(), wall = column.radius() + .05, gap = (bottoms.defaultOd() / 2 + column.openingRadius()) / 2;
            assertEquals(0, crossings(column, new double[]{c0 + gap, .5, c0}, new double[]{c0 + gap, .5, c0 - wall}), heading + " column port");
            assertTrue(openingCentres(column).isEmpty(), heading + " the column's access opening gave way to its port");
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
        var placed = List.of(new Connection(3, 4, 0), new Connection(1, 4, 0), new Connection(2, 4, 1));
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
                Heading.NORTH, 2, trays, List.of(new Connection(0, 3, 0)), ProcessUnit.Bank.ONE, false));
        // Right, left and a hillside nozzle at the back over the manway: clear of each other, of the outlets and of the
        // manway, the hillside one off centre.
        var drum = drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2, placed);
        assertEquals(List.of(), drum.clashes());
        var nozzles = drum.nozzles();
        assertEquals(5, nozzles.size());
        assertEquals(5, ShapeRasterizer.ports(drum).size());
        var hillside = nozzles.get(4);
        assertEquals(EquipmentShape.Role.SIDE, hillside.role());
        assertEquals(drum.axis() + 1, hillside.base()[0], 0);
        assertEquals(4.5, hillside.base()[1], 0);
        // Two on one place; two on neighbouring rows, their pipes touching (owner rule 2026-10-17); one beside the bare
        // drum's liquid outlet; one on a saddle; one facing a ground too near.
        assertEquals(List.of(new Clash(1, 0)), drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2, List.of(new Connection(1, 3, 0), new Connection(1, 3, 0))).clashes());
        assertEquals(List.of(new Clash(1, 0)), drum(3, 6, Orientation.VERTICAL, Heading.NORTH, 2, List.of(new Connection(1, 3, 0), new Connection(1, 4, 0))).clashes());
        assertEquals(List.of(), drum(3, 6, Orientation.VERTICAL, Heading.NORTH, 2, List.of(new Connection(1, 3, 0), new Connection(1, 5, 0))).clashes());
        assertEquals(List.of(new Clash(0, Clash.LIQUID)), drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 0, List.of(new Connection(0, 1, 0))).clashes());
        assertTrue(drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2, List.of(new Connection(3, 2, 0))).clashes().contains(new Clash(0, Clash.SADDLE)));
        assertEquals(List.of(new Clash(0, Clash.GROUND)), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 1, List.of(new Connection(3, 5, 0))).clashes());
        assertEquals(List.of(), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2, List.of(new Connection(3, 5, 0))).clashes());
    }

    @Test
    void connectionsFitEveryDrumDesign() {
        // Owner report 2026-10-16: the default lying drum's connection met a saddle. Every new drum's connection meets
        // nothing, and a connection put where a saddle stands moves to the nearest free station on its own side.
        for (var orientation : List.of(Orientation.VERTICAL, Orientation.HORIZONTAL))
            for (double d = .5; d <= EquipmentShape.MAX_DIAMETER; d += .5)
                for (int length = 1; length <= 24; length += length < 8 ? 1 : 4)
                    for (int support : orientation.horizontal() ? new int[]{1, 2, 3} : new int[]{0, EquipmentShape.minimumSkirt(d), EquipmentShape.defaultSupport(d, orientation)}) {
                        var drum = drum(d, length, orientation, Heading.NORTH, support);
                        assertEquals(List.of(), drum.clashes(), drum.toString());
                    }
        var onSaddle = List.of(new Connection(3, 2, 0));
        assertTrue(drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2, onSaddle).clashes().contains(new Clash(0, Clash.SADDLE)));
        var fitted = EquipmentShape.fit(onSaddle, drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2));
        assertEquals(List.of(), drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, 2, fitted).clashes());
        assertEquals(3, fitted.getFirst().side());
        // A connection that meets nothing stays where it is; the standing drum's own default fits as it is.
        var clear = List.of(new Connection(3, 4, 0), new Connection(1, 4, 0), new Connection(2, 4, 1));
        assertEquals(clear, EquipmentShape.fit(clear, drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2)));
        // One put on the standing drum's manway moves up its own side, clear of the cover and the davit.
        assertEquals(List.of(new Connection(2, 4, 0)), EquipmentShape.fit(List.of(new Connection(2, 2, 1)), drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2)));
    }

    @Test
    void bootsAndManwaysStandClear() {
        // Owner request 2026-10-16 (photo survey A, D): a boot hangs inboard of the far saddle and 0.15 m clear of it,
        // its drain facing down with a block for its pipe above the ground; every drum from 0.9 m carries a manway; the
        // default connections meet neither.
        int boots = 0;
        for (var orientation : List.of(Orientation.VERTICAL, Orientation.HORIZONTAL))
            for (double d = .5; d <= EquipmentShape.MAX_DIAMETER; d += .5)
                for (int length = 1; length <= 24; length += length < 8 ? 1 : 4) {
                    var plain = drum(d, length, orientation, Heading.NORTH, EquipmentShape.defaultSupport(d, orientation));
                    assertEquals(d >= .9 && (orientation.horizontal() || length >= 2), plain.manway() != null, plain.toString());
                    var boot = EquipmentShape.bootFor(d, length);
                    if (!orientation.horizontal() || boot == null) continue;
                    boots++;
                    int support = EquipmentShape.minimumBootSupport(d, length);
                    var drum = drum(d, length, orientation, Heading.NORTH, support, EquipmentShape.defaultConnections(d, length, orientation, support, true), true);
                    assertEquals(List.of(), drum.clashes(), drum.toString());
                    for (double saddle : drum.saddleCentres())
                        assertTrue(Math.abs(boot.station() - saddle) >= EquipmentShape.SADDLE_WIDTH / 2 + .15 + boot.diameter() / 2 - 1e-9, drum.toString());
                    assertEquals(1, drum.nozzles().stream().filter(n -> n.role() == EquipmentShape.Role.BOOT_DRAIN).count(), drum.toString());
                    if (support > 1) assertThrows(IllegalArgumentException.class, () -> drum(drum.diameter(), drum.tangentLength(), orientation, Heading.NORTH, support - 1, List.of(), true));
                }
        assertTrue(boots > 50, "boots checked: " + boots);
        assertThrows(IllegalArgumentException.class, () -> drum(3, 9, Orientation.VERTICAL, Heading.NORTH, 2, List.of(), true));
        assertNull(EquipmentShape.bootFor(3, 1));
        // The boot, its drain's port facing down, and the manways' covers are solid.
        var lying = drum(3, 9, Orientation.HORIZONTAL, Heading.NORTH, EquipmentShape.minimumBootSupport(3, 9), List.of(), true);
        var boot = lying.bootPlace();
        assertTrue(in(lying, lying.axis() - 1.5 - boot.length() / 2, boot.station(), lying.axis()));
        int drain = 0;
        while (lying.nozzles().get(drain).role() != EquipmentShape.Role.BOOT_DRAIN) drain++;
        final int k = drain;
        assertTrue(ShapeRasterizer.ports(lying).stream().anyMatch(p -> p.nozzle() == k && p.dy() == -1 && p.port() == EquipmentShape.PortClass.LIQUID));
        for (var shape : List.of(lying, drum(3, 4, Orientation.VERTICAL, Heading.NORTH, 2), drum(1, 3, Orientation.VERTICAL, Heading.EAST, 2))) {
            var m = shape.manway();
            var p = m.at(m.face() + .03, 0, 0);
            assertTrue(in(shape, p[0], p[1], p[2]), "manway cover of " + shape);
        }
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
            if (shape.unit()) continue;
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

    /** Whether a point of the cell, on a 0.02 m grid, is where a ladder is climbed. */
    private static boolean reachedByClimb(EquipmentShape shape, ShapeRasterizer.Cell cell) {
        for (int i = 0; i <= 50; i++)
            for (int j = 0; j <= 50; j++)
                for (double y : new double[]{.02, .5, .98})
                    if (ShapeRasterizer.climbable(shape, cell.x() + i / 50.0, cell.y() + y, cell.z() + j / 50.0)) return true;
        return false;
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
    /** The area of a drum's manway, which stands on its shell or head. */
    private static double manwayArea(EquipmentShape shape) {
        var manway = shape.manway();
        if (manway == null) return 0;
        var mesh = new Mesh();
        manway.draw(mesh);
        return mesh.area();
    }

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
