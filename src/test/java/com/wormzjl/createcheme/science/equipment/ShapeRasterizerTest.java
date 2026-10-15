package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;
import com.wormzjl.createcheme.science.math.DeterministicMath;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShapeRasterizerTest {
    /**
     * Bare vessels (small, odd, half-metre and largest; (10, 60) is the report's largest column), skirted columns, and
     * vessels lying along x and z on saddles. (0.5, 5) puts a saddle face exactly on a cell plane.
     */
    private static final List<EquipmentShape> SHAPES = List.of(new EquipmentShape(.5, 1), new EquipmentShape(2, 3),
            new EquipmentShape(2.5, 2), new EquipmentShape(3, 9), new EquipmentShape(4, 5), new EquipmentShape(10, 60),
            new EquipmentShape(2, 3, Orientation.VERTICAL, 2), new EquipmentShape(6, 10, Orientation.VERTICAL, 3),
            new EquipmentShape(2.5, 2, Orientation.ALONG_X, 1), new EquipmentShape(3, 9, Orientation.ALONG_Z, 2),
            new EquipmentShape(.5, 5, Orientation.ALONG_X, 1), new EquipmentShape(4, 12, Orientation.ALONG_Z, 1));

    @Test
    void rejectsShapesOffTheGrid() {
        for (double[] bad : new double[][]{{.25, 3}, {2.25, 3}, {10.5, 3}, {Double.NaN, 3}, {2, 0}, {2, 1.5}, {2, 65}, {2, Double.POSITIVE_INFINITY}})
            assertThrows(IllegalArgumentException.class, () -> new EquipmentShape(bad[0], bad[1]), bad[0] + " x " + bad[1]);
        // A skirt shorter than the bottom head, saddles on the ground, supports above 16 m.
        for (var bad : List.of(new Object[]{6., Orientation.VERTICAL, 1}, new Object[]{2., Orientation.VERTICAL, -1}, new Object[]{2., Orientation.VERTICAL, 17},
                new Object[]{2., Orientation.ALONG_X, 0}, new Object[]{2., Orientation.ALONG_Z, 17}))
            assertThrows(IllegalArgumentException.class, () -> new EquipmentShape((double) bad[0], 3, (Orientation) bad[1], (int) bad[2]), List.of(bad).toString());
        assertEquals(2, EquipmentShape.minimumSkirt(6));
        assertEquals(3, EquipmentShape.defaultSupport(6, Orientation.VERTICAL));
        assertEquals(4, EquipmentShape.defaultSupport(10, Orientation.VERTICAL));
        assertEquals(1, EquipmentShape.defaultSupport(10, Orientation.ALONG_Z));
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
            if (!shape.skirt() && !shape.saddles()) assertEquals(vesselArea(shape), facets, 1e-9 * facets, shape.toString());
        }
    }

    @Test
    void partsCarryTheirMaterials() {
        // Skirt row 0 (base ring, chairs) and a skirt row are support steel, a shell row is plate, the top row is a pressed head.
        var column = new EquipmentShape(6, 10, Orientation.VERTICAL, 3);
        assertEquals(Set.of(ShapeRasterizer.Material.SUPPORT), materials(column, 0, 1));
        assertEquals(Set.of(ShapeRasterizer.Material.SUPPORT), materials(column, 1, 2));
        assertEquals(Set.of(ShapeRasterizer.Material.SHELL), materials(column, 5, 6));
        assertEquals(Set.of(ShapeRasterizer.Material.HEAD), materials(column, column.sizeY() - 1, column.sizeY()));
        // A lying drum's lowest row holds only its saddles.
        assertEquals(Set.of(ShapeRasterizer.Material.SUPPORT), materials(new EquipmentShape(3, 9, Orientation.ALONG_Z, 2), 0, 1));
    }

    @Test
    void everyPieceStaysInsideItsCellAndFacesOutwards() {
        for (var shape : SHAPES)
            for (var cell : ShapeRasterizer.layout(shape).shell()) {
                var polygons = ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()));
                assertFalse(polygons.isEmpty(), shape + " " + cell);
                for (var polygon : polygons) {
                    assertTrue(polygon.size() >= 3);
                    for (double v : polygon.positions()) assertTrue(v >= 0 && v <= 1, shape + " " + cell + " position " + v);
                    for (double v : polygon.uvs()) assertTrue(v >= 0 && v <= 1, shape + " " + cell + " uv " + v);
                    double[] face = newell(polygon), smooth = new double[3];
                    for (int i = 0; i < polygon.size(); i++) {
                        double nx = polygon.normals()[3 * i], ny = polygon.normals()[3 * i + 1], nz = polygon.normals()[3 * i + 2];
                        assertEquals(1, nx * nx + ny * ny + nz * nz, 1e-12);
                        smooth[0] += nx;
                        smooth[1] += ny;
                        smooth[2] += nz;
                    }
                    assertTrue(face[0] * smooth[0] + face[1] * smooth[1] + face[2] * smooth[2] > 0, shape + " " + cell + " winding");
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
                var actual = ShapeRasterizer.surface(shape, cell.x(), cell.y(), cell.z());
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
            double volume = layout.interior().size();
            for (var cell : layout.shell()) {
                int parts = 0, sampled = 0;
                for (var box : ShapeRasterizer.solid(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) {
                    assertTrue(box.x0() < box.x1() && box.x1() <= s && box.y0() < box.y1() && box.y1() <= s && box.z0() < box.z1() && box.z1() <= s);
                    parts += (box.x1() - box.x0()) * (box.y1() - box.y0()) * (box.z1() - box.z0());
                }
                for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) for (int c = 0; c < s; c++)
                    if (ShapeRasterizer.inside(shape, cell.x() + (a + .5) / s, cell.y() + (b + .5) / s, cell.z() + (c + .5) / s)) sampled++;
                assertEquals(sampled, parts, shape + " " + cell + ": boxes overlap or miss samples");
                volume += parts / (double) (s * s * s);
            }
            // A misclassified sample lies within half a sample diagonal of the surface.
            double exact = facetedVolume(shape), band = ShapeRasterizer.facetArea(shape) * Math.sqrt(3) / (2 * s);
            assertEquals(exact, volume, band, shape.toString());
            // Lattice alignment alone moves a 2 m cross-section by 2 % (16 facets centred on a cell midline sampled +2.1 %).
            if (shape.diameter() >= 2.5) assertEquals(exact, volume, .02 * exact, shape.toString());
        }
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
    void supportsCarryTheVessel() {
        // A skirt continues the shell to the ground on a base ring 1/8 m wider; a bare vessel touches it only at its lower apex.
        var skirted = new EquipmentShape(6, 10, Orientation.VERTICAL, 3);
        double middle = skirted.width() / 2.0;
        assertEquals(7, skirted.width());
        assertEquals(3 + 10 + 1.5, skirted.topElevation(), 0);
        assertEquals(12, skirted.anchorChairs());
        assertTrue(ShapeRasterizer.inside(skirted, middle + 2.8, .5, middle));
        assertFalse(ShapeRasterizer.inside(skirted, middle + 3.05, .5, middle));
        assertTrue(ShapeRasterizer.inside(skirted, middle + 3.1, .03, middle));
        assertFalse(ShapeRasterizer.inside(skirted, middle + 3.2, .03, middle));
        assertFalse(ShapeRasterizer.inside(new EquipmentShape(6, 10), 3 + 2.8, .5, 3));
        // Saddles a fifth of T/T in from each tangent line, under the axis; nothing under the vessel between them.
        for (var lying : List.of(new EquipmentShape(3, 9, Orientation.ALONG_Z, 2), new EquipmentShape(4, 12, Orientation.ALONG_X, 1))) {
            double tangent = lying.diameter() / 4, length = lying.tangentLength(), centre = lying.width() / 2.0, under = lying.support() / 2.0;
            assertEquals(lying.support() + lying.width() / 2.0 + lying.radius(), lying.topElevation(), 0);
            for (double along : new double[]{tangent + .2 * length, tangent + .8 * length, tangent + .5 * length}) {
                boolean saddle = along != tangent + .5 * length;
                double x = lying.orientation() == Orientation.ALONG_X ? along : centre, z = lying.orientation() == Orientation.ALONG_Z ? along : centre;
                assertEquals(saddle, ShapeRasterizer.inside(lying, x, under, z), lying + " at " + along);
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
        // The faceted vessel is inscribed in the smooth one: smaller, and closer for finer facets. Supports are not vessel.
        for (var shape : SHAPES) {
            double volume = vesselVolume(shape), area = vesselArea(shape);
            assertTrue(volume < shape.volume() && volume > .88 * shape.volume(), shape + " volume " + volume + " of " + shape.volume());
            assertTrue(area < shape.surfaceArea() && area > .9 * shape.surfaceArea(), shape + " area " + area + " of " + shape.surfaceArea());
        }
    }

    private static int axial(ShapeRasterizer.CellKey key) {
        return switch (key.shape().orientation()) {
            case VERTICAL -> key.y();
            case ALONG_X -> key.x();
            case ALONG_Z -> key.z();
        };
    }

    /** Area of the faceted pressure vessel: the N-gon prism over T/T and two banded heads. */
    private static double vesselArea(EquipmentShape shape) {
        return shellArea(shape, shape.tangentLength()) + 2 * headArea(shape);
    }

    private static double vesselVolume(EquipmentShape shape) {
        return prismVolume(shape, shape.tangentLength()) + 2 * headVolume(shape);
    }

    /**
     * Volume of the faceted solid: a skirt's prism to the ground, its base ring (an annular N-gon prism 1/8 m wider) and
     * chairs; or the vessel and two saddles, each a top plate around the contact sector, a web and two ribs under the top
     * plate's outer edge, and a base plate.
     */
    private static double facetedVolume(EquipmentShape shape) {
        int n = shape.facets();
        double sin = DeterministicMath.sin(2 * Math.PI / n), cos = DeterministicMath.cos(Math.PI / n), r = shape.radius();
        if (shape.skirt()) {
            double ring = r + EquipmentShape.BASE_RING / cos;
            return prismVolume(shape, shape.support() + shape.tangentLength()) + headVolume(shape)
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

    private static Set<ShapeRasterizer.Material> materials(EquipmentShape shape, int low, int high) {
        var found = EnumSet.noneOf(ShapeRasterizer.Material.class);
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
        int n = shape.facets();
        return n / 2.0 * DeterministicMath.sin(2 * Math.PI / n) * shape.radius() * shape.radius() * length;
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
        int n = shape.facets();
        double polygon = n / 2.0 * DeterministicMath.sin(2 * Math.PI / n), volume = 0; // area of the N-gon of unit circumradius
        double[][] rings = rings(shape);
        for (int k = 0; k + 1 < rings.length; k++) {
            double a = polygon * rings[k][1] * rings[k][1], b = polygon * rings[k + 1][1] * rings[k + 1][1];
            volume += (rings[k + 1][0] - rings[k][0]) / 3 * (a + b + Math.sqrt(a * b));
        }
        return volume;
    }

    private static double[][] rings(EquipmentShape shape) {
        int bands = shape.bands();
        var rings = new double[bands + 1][];
        for (int k = 0; k <= bands; k++) {
            double phi = Math.PI / 2 * k / bands;
            rings[k] = new double[]{shape.headDepth() * DeterministicMath.sin(phi), k == bands ? 0 : shape.radius() * DeterministicMath.cos(phi)};
        }
        return rings;
    }

    private static boolean sameUpToRotation(ShapeRasterizer.Polygon a, ShapeRasterizer.Polygon b) {
        int m = a.size();
        if (m != b.size()) return false;
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

    private static double area(ShapeRasterizer.Polygon polygon) {
        var n = newell(polygon);
        return .5 * Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
    }

    private static double[] newell(ShapeRasterizer.Polygon polygon) {
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
