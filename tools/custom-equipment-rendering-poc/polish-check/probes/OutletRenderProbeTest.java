package com.wormzjl.createcheme.science.equipment;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Temporary probe (detached before the batch closes): renders a vessel's cell polygons with a pipe on its bottom outlet, as the chunk renderer
 * receives them, to PNG views with a software z-buffer, so the auxiliaries' layout can be checked without the game.
 */
class OutletRenderProbeTest {
    private static final Map<Mesh.Material, int[]> COLOURS = Map.ofEntries(Map.entry(Mesh.Material.SHELL, new int[]{200, 198, 190}),
            Map.entry(Mesh.Material.HEAD, new int[]{190, 188, 180}), Map.entry(Mesh.Material.SUPPORT, new int[]{120, 120, 115}),
            Map.entry(Mesh.Material.PIPE, new int[]{80, 120, 190}), Map.entry(Mesh.Material.FLANGE, new int[]{60, 90, 150}),
            Map.entry(Mesh.Material.OPENING, new int[]{20, 20, 20}), Map.entry(Mesh.Material.CLADDING, new int[]{225, 225, 228}),
            Map.entry(Mesh.Material.GRATING, new int[]{150, 140, 90}), Map.entry(Mesh.Material.HANDRAIL, new int[]{230, 190, 40}),
            Map.entry(Mesh.Material.STEELWORK, new int[]{110, 130, 110}), Map.entry(Mesh.Material.MACHINE, new int[]{70, 140, 90}),
            Map.entry(Mesh.Material.MOTOR, new int[]{60, 70, 160}), Map.entry(Mesh.Material.ACTUATOR, new int[]{200, 90, 50}),
            Map.entry(Mesh.Material.FEED, new int[]{110, 160, 80}), Map.entry(Mesh.Material.DRAIN, new int[]{60, 60, 200}));

    @Test
    void render() throws Exception {
        var cases = List.of(
                new Object[]{"drum-d3", EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2, List.of())},
                new Object[]{"drum-d6", EquipmentShape.drum(6, 8, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 3, List.of())},
                new Object[]{"drum-d3-s4", EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 4, List.of())},
                new Object[]{"column-d3", EquipmentShape.column(3, ColumnTrays.simple(12, 6), EquipmentShape.Heading.NORTH, 2)},
                new Object[]{"ac-3x6", EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 3, 6, EquipmentShape.Heading.NORTH, 5)},
                new Object[]{"ac-2x4-bank3x2", EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 2, 4, EquipmentShape.Heading.NORTH, 5, new ProcessUnit.Bank(3, 2))},
                new Object[]{"ac-1.5x4", EquipmentShape.unit(EquipmentShape.Kind.AIR_COOLER, 1.5, 4, EquipmentShape.Heading.NORTH, 5)},
                new Object[]{"ejector-0.1", EquipmentShape.unit(EquipmentShape.Kind.EJECTOR, .1, 1, EquipmentShape.Heading.NORTH, 1)});
        for (double pipe : new double[]{1})
            for (var c : cases) {
                var shape = (EquipmentShape) c[1];
                var triangles = triangles(shape, pipe);
                String name = "build/tmp/outlet-probe/" + c[0] + "-" + pipe;
                double s = Math.sqrt(.5);
                var right = new double[]{s, 0, s};
                var depth = new double[]{-.5, -.35, .79};
                view(triangles, right, unit(cross(depth, right)), unit(depth), 48, name + "-iso.png");
                view(triangles, new double[]{0, 0, -1}, new double[]{0, 1, 0}, new double[]{1, 0, 0}, 48, name + "-side.png");
            }
    }

    private record Triangle(double[] a, double[] b, double[] c, Mesh.Material material) {
    }

    /** The vessel's cells with a pipe of {@code pipe} m on its bottom outlet, and two pipe blocks beyond the port (the first flanged to it). */
    private static List<Triangle> triangles(EquipmentShape shape, double pipe) {
        var out = new ArrayList<Triangle>();
        var nozzles = shape.nozzles();
        double[] outside = new double[nozzles.size()];
        ShapeRasterizer.Port bottom = null;
        for (var port : ShapeRasterizer.ports(shape)) {
            var role = nozzles.get(port.nozzle()).role();
            if (role == EquipmentShape.Role.LIQUID_OUTLET || role == EquipmentShape.Role.BOTTOMS) {
                outside[port.nozzle()] = pipe;
                bottom = port;
            }
        }
        for (var cell : ShapeRasterizer.layout(shape).shell())
            add(out, ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z(), ShapeRasterizer.cellCodes(shape, cell, outside))), cell.x(), cell.y(), cell.z());
        // The ground (drawn as FEED, green), round the port.
        int gx = bottom == null ? 0 : bottom.cell().x(), gz = bottom == null ? 0 : bottom.cell().z();
        double[] g0 = {gx - 4, 0, gz - 4}, g1 = {gx + 5, 0, gz - 4}, g2 = {gx + 5, 0, gz + 5}, g3 = {gx - 4, 0, gz + 5};
        out.add(new Triangle(g0, g2, g1, Mesh.Material.FEED));
        out.add(new Triangle(g0, g3, g2, Mesh.Material.FEED));
        if (bottom != null) {
            int code = PipeSizes.code(pipe);
            int toward = face(-bottom.dx(), -bottom.dy(), -bottom.dz()), away = toward ^ 1;
            for (int k = 1; k <= 2; k++) {
                long faces = (long) code << 7 * toward | (long) code << 7 * away;
                var key = new LineGeometry.Key(com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind.PIPE, away, 1 << toward | 1 << away, code, faces,
                        k == 1 ? 1 << toward : 0, false);
                add(out, LineGeometry.polygons(key), bottom.cell().x() + k * bottom.dx(), bottom.cell().y() + k * bottom.dy(), bottom.cell().z() + k * bottom.dz());
            }
        }
        return out;
    }

    private static int face(int dx, int dy, int dz) {
        for (int i = 0; i < 6; i++) {
            var s = LineGeometry.STEPS[i];
            if (s[0] == dx && s[1] == dy && s[2] == dz) return i;
        }
        throw new IllegalArgumentException();
    }

    private static void add(List<Triangle> out, List<Mesh.Polygon> polygons, int x, int y, int z) {
        for (var polygon : polygons) {
            var p = polygon.positions();
            int n = polygon.size();
            double[] first = {p[0] + x, p[1] + y, p[2] + z};
            for (int i = 1; i + 1 < n; i++)
                out.add(new Triangle(first, new double[]{p[3 * i] + x, p[3 * i + 1] + y, p[3 * i + 2] + z}, new double[]{p[3 * i + 3] + x, p[3 * i + 4] + y, p[3 * i + 5] + z},
                        polygon.material()));
        }
    }

    /** Orthographic view: screen x along {@code right}, screen y along {@code up}, looking along {@code depth}. */
    private static void view(List<Triangle> triangles, double[] right, double[] up, double[] depth, double scale, String file) throws Exception {
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (var t : triangles)
            for (var v : new double[][]{t.a, t.b, t.c}) {
                minX = Math.min(minX, dot(v, right));
                maxX = Math.max(maxX, dot(v, right));
                minY = Math.min(minY, dot(v, up));
                maxY = Math.max(maxY, dot(v, up));
            }
        int w = (int) Math.ceil((maxX - minX) * scale) + 20, h = (int) Math.ceil((maxY - minY) * scale) + 20;
        var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var zbuf = new double[w * h];
        java.util.Arrays.fill(zbuf, Double.MAX_VALUE);
        for (int i = 0; i < w * h; i++) image.setRGB(i % w, i / w, 0xffffff);
        double[] light = unit(new double[]{-.4, .8, -.45});
        for (var t : triangles) {
            var normal = unit(cross(sub(t.b, t.a), sub(t.c, t.a)));
            if (dot(normal, depth) > 0) continue; // back face
            double shade = .45 + .55 * Math.max(0, dot(normal, light));
            int[] base = COLOURS.getOrDefault(t.material, new int[]{255, 0, 255});
            int rgb = (int) (base[0] * shade) << 16 | (int) (base[1] * shade) << 8 | (int) (base[2] * shade);
            double[][] s = new double[3][];
            int k = 0;
            for (var v : new double[][]{t.a, t.b, t.c})
                s[k++] = new double[]{(dot(v, right) - minX) * scale + 10, h - ((dot(v, up) - minY) * scale + 10), dot(v, depth)};
            int x0 = (int) Math.max(0, Math.floor(Math.min(s[0][0], Math.min(s[1][0], s[2][0])))), x1 = (int) Math.min(w - 1, Math.ceil(Math.max(s[0][0], Math.max(s[1][0], s[2][0]))));
            int y0 = (int) Math.max(0, Math.floor(Math.min(s[0][1], Math.min(s[1][1], s[2][1])))), y1 = (int) Math.min(h - 1, Math.ceil(Math.max(s[0][1], Math.max(s[1][1], s[2][1]))));
            double area = edge(s[0], s[1], s[2][0], s[2][1]);
            if (Math.abs(area) < 1e-12) continue;
            for (int y = y0; y <= y1; y++)
                for (int x = x0; x <= x1; x++) {
                    double px = x + .5, py = y + .5;
                    double w0 = edge(s[1], s[2], px, py) / area, w1 = edge(s[2], s[0], px, py) / area, w2 = edge(s[0], s[1], px, py) / area;
                    if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                    double z = w0 * s[0][2] + w1 * s[1][2] + w2 * s[2][2];
                    if (z < zbuf[y * w + x]) {
                        zbuf[y * w + x] = z;
                        image.setRGB(x, y, rgb);
                    }
                }
        }
        ImageIO.write(image, "png", new File(file));
    }

    private static double edge(double[] a, double[] b, double x, double y) {
        return (b[0] - a[0]) * (y - a[1]) - (b[1] - a[1]) * (x - a[0]);
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unit(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return new double[]{a[0] / l, a[1] / l, a[2] / l};
    }
}
