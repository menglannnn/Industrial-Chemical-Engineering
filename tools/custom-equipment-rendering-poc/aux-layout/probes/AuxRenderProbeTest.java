package com.wormzjl.createcheme.science.equipment;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Temporary probe (not a gate; detached before the batch closes): renders a vessel's cell polygons, as the chunk renderer
 * receives them, to PNG views with a software z-buffer, so the auxiliaries' layout can be checked without the game.
 */
class AuxRenderProbeTest {
    private static final Map<Mesh.Material, int[]> COLOURS = Map.ofEntries(Map.entry(Mesh.Material.SHELL, new int[]{200, 198, 190}),
            Map.entry(Mesh.Material.HEAD, new int[]{190, 188, 180}), Map.entry(Mesh.Material.SUPPORT, new int[]{120, 120, 115}),
            Map.entry(Mesh.Material.PIPE, new int[]{80, 120, 190}), Map.entry(Mesh.Material.FLANGE, new int[]{60, 90, 150}),
            Map.entry(Mesh.Material.OPENING, new int[]{20, 20, 20}), Map.entry(Mesh.Material.CLADDING, new int[]{225, 225, 228}),
            Map.entry(Mesh.Material.GRATING, new int[]{150, 140, 90}), Map.entry(Mesh.Material.HANDRAIL, new int[]{230, 190, 40}),
            Map.entry(Mesh.Material.STEELWORK, new int[]{110, 130, 110}), Map.entry(Mesh.Material.MACHINE, new int[]{70, 140, 90}),
            Map.entry(Mesh.Material.MOTOR, new int[]{60, 70, 160}), Map.entry(Mesh.Material.ACTUATOR, new int[]{200, 90, 50}),
            Map.entry(Mesh.Material.FEED, new int[]{200, 60, 60}), Map.entry(Mesh.Material.DRAIN, new int[]{60, 60, 200}));

    @Test
    void render() throws Exception {
        var crude = new ColumnTrays(25, 13, List.of(8), List.of(26), List.of(new ColumnTrays.Pumparound(6, 4), new ColumnTrays.Pumparound(15, 13)));
        var cases = List.of(
                new Object[]{"d3-water", EquipmentShape.column(3, crude, ColumnOptions.DEFAULT, EquipmentShape.Heading.NORTH, 2)},
                new Object[]{"d3-air-ejector-kettle", EquipmentShape.column(3, crude, new ColumnOptions(ColumnOptions.Condenser.AIR, true, ColumnOptions.Reboiler.KETTLE),
                        EquipmentShape.Heading.NORTH, 2)},
                new Object[]{"d1-water-ejector", EquipmentShape.column(1, crude, new ColumnOptions(ColumnOptions.Condenser.WATER, true, ColumnOptions.Reboiler.NONE),
                        EquipmentShape.Heading.NORTH, 2)});
        new File("build/tmp/aux-probe").mkdirs();
        for (var c : cases) {
            var shape = (EquipmentShape) c[1];
            var triangles = triangles(shape);
            // Front: the constructor's side (box -z) looking +z; plan from above; the right side (box -x) looking +x; an isometric view.
            view(triangles, new double[]{1, 0, 0}, new double[]{0, 1, 0}, new double[]{0, 0, 1}, 24, "build/tmp/aux-probe/" + c[0] + "-front.png");
            view(triangles, new double[]{1, 0, 0}, new double[]{0, 0, -1}, new double[]{0, -1, 0}, 24, "build/tmp/aux-probe/" + c[0] + "-plan.png");
            double s = Math.sqrt(.5);
            var right = new double[]{s, 0, -s};
            var depth = new double[]{.5, -Math.sqrt(.5), .5};
            var up = cross(depth, right);
            view(triangles, right, up, depth, 20, "build/tmp/aux-probe/" + c[0] + "-iso.png");
            var right2 = new double[]{s, 0, s};
            var depth2 = new double[]{-.5, -Math.sqrt(.5), .5};
            view(triangles, right2, cross(depth2, right2), depth2, 20, "build/tmp/aux-probe/" + c[0] + "-iso2.png");
        }
    }

    private record Triangle(double[] a, double[] b, double[] c, Mesh.Material material) {
    }

    private static List<Triangle> triangles(EquipmentShape shape) {
        var out = new ArrayList<Triangle>();
        for (var cell : ShapeRasterizer.layout(shape).shell())
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z()))) {
                var p = polygon.positions();
                int n = polygon.size();
                double[] first = {p[0] + cell.x(), p[1] + cell.y(), p[2] + cell.z()};
                for (int i = 1; i + 1 < n; i++)
                    out.add(new Triangle(first, new double[]{p[3 * i] + cell.x(), p[3 * i + 1] + cell.y(), p[3 * i + 2] + cell.z()},
                            new double[]{p[3 * i + 3] + cell.x(), p[3 * i + 4] + cell.y(), p[3 * i + 5] + cell.z()}, polygon.material()));
            }
        return out;
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
