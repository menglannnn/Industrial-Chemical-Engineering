package com.wormzjl.icheme.science.equipment;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * W3 scratch renderer (batch 2026-10-26-multi-machine-framework), adapted from the main checkout's
 * tools/mover-equipment-arrows/MoverPortRender.java: the shape's real shell cell polygons and nozzle arrows, the mod's
 * sprites, the game's directional shade, culled back faces, a depth buffer. A pipe block leaves each flow-marked port.
 *
 * <p>Args: {@code out.png before|after spec...}, a spec {@code KIND:index} into EquipmentLayoutGoldenTest.shapes() of that
 * kind (drums: 0-2 standing, 3-5 lying). "before" draws the old MoverPortMarkings (compiled from HEAD into
 * tmp/w3/before-classes). One sheet: per spec an overview and a close-up of each flow-marked nozzle.
 */
public class ArrowRender {
    private static final String ASSETS = "src/main/resources/assets/icheme/";
    private static final int TILE = 300;

    private record Triangle(double[][] p, double[][] uv, double[] normal, Mesh.Material material) {
    }

    private record Sprite(int width, int height, int[] argb) {
        int sample(double u, double v) {
            int x = Math.floorMod((int) Math.floor(u * width), width), y = Math.floorMod((int) Math.floor(v * height), height);
            return argb[y * width + x];
        }
    }

    private record Tile(BufferedImage image, String caption) {
    }

    public static void main(String[] args) throws Exception {
        String out = args[0];
        boolean before = args[1].equals("before");
        Method old = before ? Class.forName("com.wormzjl.icheme.science.equipment.MoverPortMarkings").getMethod("surface", ShapeRasterizer.CellKey.class) : null;
        var sprites = sprites();
        var all = EquipmentLayoutGoldenTest.shapes();
        var rows = new ArrayList<List<Tile>>();
        for (int a = 2; a < args.length; a++) {
            var spec = args[a].split(":");
            var kind = EquipmentShape.Kind.valueOf(spec[0]);
            var shape = all.stream().filter(s -> s.kind() == kind).skip(Integer.parseInt(spec[1])).findFirst().orElseThrow();
            var triangles = new ArrayList<Triangle>();
            double[] diameters = new double[shape.nozzles().size()];
            for (int i = 0; i < diameters.length; i++) {
                var n = shape.nozzles().get(i);
                if (PortMarkings.flow(kind, n.role()) != PortMarkings.Flow.NONE) diameters[i] = PipeSizes.outsideOf(PipeSizes.code(n.defaultOd()));
            }
            int marks = 0;
            for (var cell : ShapeRasterizer.layout(shape).shell()) {
                var key = ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z(), ShapeRasterizer.cellCodes(shape, cell, diameters));
                for (var polygon : ShapeRasterizer.surface(key)) add(triangles, polygon, cell.x(), cell.y(), cell.z());
                @SuppressWarnings("unchecked")
                var marked = before ? (List<Mesh.Polygon>) old.invoke(null, key) : PortMarkings.surface(key);
                marks += marked.size();
                for (var polygon : marked) add(triangles, polygon, cell.x(), cell.y(), cell.z());
            }
            for (var port : ShapeRasterizer.ports(shape)) {
                if (diameters[port.nozzle()] == 0) continue;
                int face = 0;
                for (int i = 0; i < 6; i++)
                    if (LineGeometry.STEPS[i][0] == -port.dx() && LineGeometry.STEPS[i][1] == -port.dy() && LineGeometry.STEPS[i][2] == -port.dz()) face = i;
                int code = PipeSizes.code(diameters[port.nozzle()]), mask = (1 << face) | (1 << (face ^ 1));
                long sizes = ((long) code << (7 * face)) | ((long) code << (7 * (face ^ 1)));
                var key = new LineGeometry.Key(com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PIPE, face, mask, code, sizes, 1 << face, false);
                int x = port.cell().x() + port.dx(), y = port.cell().y() + port.dy(), z = port.cell().z() + port.dz();
                for (var polygon : LineGeometry.polygons(key)) add(triangles, polygon, x, y, z);
            }
            String title = String.format(Locale.ROOT, "%s %.2f m %s", kind, shape.diameter(), shape.orientation());
            System.out.println(title + ": " + marks + " marking polygons");
            var row = new ArrayList<Tile>();
            row.add(new Tile(render(triangles, shape, unit(new double[]{-1, -.75, 1.15}), sprites, null, 0), title));
            for (int k = 0; k < shape.nozzles().size(); k++) {
                var n = shape.nozzles().get(k);
                var flow = PortMarkings.flow(kind, n.role());
                if (flow == PortMarkings.Flow.NONE) continue;
                var band = new Mesh();
                band.transform(shape.transform());
                PortMarkings.band(band, shape, k, 0, flow);
                double od = n.defaultOd();
                double[] focus;
                if (band.facets().isEmpty()) {
                    double[] face = n.face(), dir = n.direction();
                    focus = new double[3];
                    for (int i = 0; i < 3; i++) focus[i] = face[i] - .15 * dir[i];
                    focus = shape.transform().point(focus);
                } else {
                    double[] lo = {1e9, 1e9, 1e9}, hi = {-1e9, -1e9, -1e9};
                    for (var f : band.facets()) for (var p : f.positions()) for (int i = 0; i < 3; i++) { lo[i] = Math.min(lo[i], p[i]); hi[i] = Math.max(hi[i], p[i]); }
                    focus = new double[]{(lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2};
                }
                var axis = shape.transform().direction(n.direction());
                double[] depth = view(shape, focus, axis);
                row.add(new Tile(render(triangles, shape, depth, sprites, focus, Math.max(.4, .9 * od)),
                        n.role().name().toLowerCase(Locale.ROOT) + (flow == PortMarkings.Flow.IN ? " (in)" : " (out)") + (band.facets().isEmpty() ? " no band" : "")));
            }
            rows.add(row);
        }
        int columns = rows.stream().mapToInt(List::size).max().orElse(1), caption = 22;
        int width = Math.min(columns, 6) * TILE, height = 0;
        for (var row : rows) height += (int) Math.ceil(row.size() / 6.0) * (TILE + caption);
        var sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = sheet.createGraphics();
        g.setColor(new Color(0xEEF1F4));
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        int y = 0;
        for (var row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0 && i % 6 == 0) y += TILE + caption;
                int x = (i % 6) * TILE;
                var tile = row.get(i);
                double s = Math.min((double) (TILE - 6) / tile.image.getWidth(), (double) (TILE - 6) / tile.image.getHeight());
                int w = (int) (tile.image.getWidth() * s), h = (int) (tile.image.getHeight() * s);
                g.drawImage(tile.image, x + (TILE - w) / 2, y + caption + (TILE - h) / 2, w, h, null);
                g.setColor(Color.DARK_GRAY);
                g.drawString(tile.caption, x + 6, y + 16);
            }
            y += TILE + caption;
        }
        g.dispose();
        new File(out).getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(sheet, "png", new File(out));
        System.out.println("wrote " + out);
    }

    /** A view direction onto a nozzle's band from its side, slightly from outward, from a side no solid blocks. */
    private static double[] view(EquipmentShape shape, double[] focus, double[] axis) {
        var candidates = new ArrayList<double[]>();
        double[][] around = Math.abs(axis[1]) > .5 ? new double[][]{{0, 0, -1}, {-1, 0, 0}, {1, 0, 0}, {0, 0, 1}}
                : new double[][]{cross(axis, new double[]{0, 1, 0}), cross(new double[]{0, 1, 0}, axis), {0, 1, 0}, {0, -1, 0}};
        for (var side : around) {
            // The camera sits at side + .35 axis + a little height; the view runs back to the focus.
            double[] eye = unit(new double[]{side[0] + .35 * axis[0], side[1] + .35 * axis[1] + .25, side[2] + .35 * axis[2]});
            candidates.add(eye);
        }
        for (var eye : candidates) {
            boolean clear = true;
            for (double t = .3; t < 4 && clear; t += .02)
                if (ShapeRasterizer.inside(shape, focus[0] + t * eye[0], focus[1] + t * eye[1], focus[2] + t * eye[2])) clear = false;
            if (clear) return new double[]{-eye[0], -eye[1], -eye[2]};
        }
        var eye = candidates.get(0);
        return new double[]{-eye[0], -eye[1], -eye[2]};
    }

    /** The sprites by material, from the shell model's texture names. */
    private static Map<Mesh.Material, Sprite> sprites() throws Exception {
        String json = Files.readString(Path.of(ASSETS + "models/block/equipment_shell.json"));
        var names = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"icheme:block/(\\w+)\"").matcher(json);
        var files = new HashMap<String, String>();
        while (names.find()) files.put(names.group(1), names.group(2));
        var out = new EnumMap<Mesh.Material, Sprite>(Mesh.Material.class);
        for (var material : Mesh.Material.values()) {
            String file = files.get(material.texture());
            if (file == null) continue;
            var image = ImageIO.read(new File(ASSETS + "textures/block/" + file + ".png"));
            int w = image.getWidth(), h = image.getHeight() > w && image.getHeight() % w == 0 ? w : image.getHeight();
            int[] argb = new int[w * h];
            image.getRGB(0, 0, w, h, argb, 0, w);
            out.put(material, new Sprite(w, h, argb));
        }
        return out;
    }

    private static void add(List<Triangle> out, Mesh.Polygon polygon, int x, int y, int z) {
        double[] p = polygon.positions(), uv = polygon.uvs();
        int n = polygon.size();
        for (int i = 1; i + 1 < n; i++) {
            int[] c = {0, i, i + 1};
            double[][] pts = new double[3][], uvs = new double[3][];
            for (int k = 0; k < 3; k++) {
                pts[k] = new double[]{p[3 * c[k]] + x, p[3 * c[k] + 1] + y, p[3 * c[k] + 2] + z};
                uvs[k] = new double[]{uv[2 * c[k]], uv[2 * c[k] + 1]};
            }
            var normal = cross(sub(pts[1], pts[0]), sub(pts[2], pts[0]));
            double l = Math.sqrt(dot(normal, normal));
            if (l < 1e-12) continue;
            out.add(new Triangle(pts, uvs, new double[]{normal[0] / l, normal[1] / l, normal[2] / l}, polygon.material()));
        }
    }

    private static double shade(double[] n) {
        return Math.min(n[0] * n[0] * .6 + n[1] * n[1] * ((3 + n[1]) / 4) + n[2] * n[2] * .8, 1);
    }

    private static BufferedImage render(List<Triangle> triangles, EquipmentShape shape, double[] depth, Map<Mesh.Material, Sprite> sprites, double[] focus, double focusRadius) {
        double[] right = unit(Math.abs(depth[1]) > .99 ? new double[]{1, 0, 0} : cross(depth, new double[]{0, 1, 0}));
        double[] up = Math.abs(depth[1]) > .99 ? new double[]{0, 0, -1} : cross(right, depth);
        int gx0 = -2, gz0 = -2, gx1 = shape.sizeX() + 2, gz1 = shape.sizeZ() + 2;
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        var corners = new ArrayList<double[]>();
        for (var t : triangles) corners.addAll(Arrays.asList(t.p));
        for (int x : new int[]{gx0, gx1}) for (int z : new int[]{gz0, gz1}) corners.add(new double[]{x, 0, z});
        for (var v : corners) {
            minX = Math.min(minX, dot(v, right));
            maxX = Math.max(maxX, dot(v, right));
            minY = Math.min(minY, dot(v, up));
            maxY = Math.max(maxY, dot(v, up));
        }
        if (focus != null) {
            minX = dot(focus, right) - focusRadius;
            maxX = dot(focus, right) + focusRadius;
            minY = dot(focus, up) - focusRadius;
            maxY = dot(focus, up) + focusRadius;
        }
        double scale = focus == null ? Math.max(10, Math.min(100, 900 / Math.max(maxX - minX, maxY - minY))) : 600 / (2 * focusRadius);
        int w = (int) Math.ceil((maxX - minX) * scale) + 20, h = (int) Math.ceil((maxY - minY) * scale) + 20;
        var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var zbuf = new double[w * h];
        Arrays.fill(zbuf, Double.MAX_VALUE);
        for (int i = 0; i < w * h; i++) image.setRGB(i % w, i / w, 0x9CC3F5);
        double fx = minX, fy = minY;
        Rasterizer raster = (a, b, c, colour) -> {
            double[][] s = new double[3][];
            int k = 0;
            for (var v : new double[][]{a, b, c}) s[k++] = new double[]{(dot(v, right) - fx) * scale + 10, h - ((dot(v, up) - fy) * scale + 10), dot(v, depth)};
            int x0 = (int) Math.max(0, Math.floor(Math.min(s[0][0], Math.min(s[1][0], s[2][0])))), x1 = (int) Math.min(w - 1, Math.ceil(Math.max(s[0][0], Math.max(s[1][0], s[2][0]))));
            int y0 = (int) Math.max(0, Math.floor(Math.min(s[0][1], Math.min(s[1][1], s[2][1])))), y1 = (int) Math.min(h - 1, Math.ceil(Math.max(s[0][1], Math.max(s[1][1], s[2][1]))));
            double area = edge(s[0], s[1], s[2][0], s[2][1]);
            if (Math.abs(area) < 1e-12) return;
            for (int y = y0; y <= y1; y++)
                for (int x = x0; x <= x1; x++) {
                    double px = x + .5, py = y + .5;
                    double w0 = edge(s[1], s[2], px, py) / area, w1 = edge(s[2], s[0], px, py) / area, w2 = edge(s[0], s[1], px, py) / area;
                    if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                    double z = w0 * s[0][2] + w1 * s[1][2] + w2 * s[2][2];
                    if (z >= zbuf[y * w + x]) continue;
                    int rgb = colour.at(w0, w1, w2);
                    if (rgb < 0) continue;
                    zbuf[y * w + x] = z;
                    image.setRGB(x, y, rgb);
                }
        };
        if (focus == null)
            for (int x = gx0; x < gx1; x++)
                for (int z = gz0; z < gz1; z++) {
                    double[] a = {x, -.001, z}, b = {x + 1, -.001, z}, c = {x + 1, -.001, z + 1}, d = {x, -.001, z + 1};
                    raster.fill(a, b, c, (w0, w1, w2) -> 0x6A9A3C);
                    raster.fill(a, c, d, (w0, w1, w2) -> 0x6A9A3C);
                }
        for (var t : triangles) {
            if (dot(t.normal, depth) >= 0) continue;
            var sprite = sprites.get(t.material);
            double light = shade(t.normal);
            int channel = t.material.channel();
            int tint = channel < 0 ? 0xFFFFFF : Paint.DEFAULT.colour(channel);
            double tr = Math.min(255, (tint >> 16 & 255) * 255.0 / Paint.SPRITE_VALUE) / 255, tg = Math.min(255, (tint >> 8 & 255) * 255.0 / Paint.SPRITE_VALUE) / 255,
                    tb = Math.min(255, (tint & 255) * 255.0 / Paint.SPRITE_VALUE) / 255;
            if (channel < 0) tr = tg = tb = 1;
            double ftr = tr, ftg = tg, ftb = tb;
            raster.fill(t.p[0], t.p[1], t.p[2], (w0, w1, w2) -> {
                int texel = 0xFFFF00FF;
                if (sprite != null) {
                    double u = w0 * t.uv[0][0] + w1 * t.uv[1][0] + w2 * t.uv[2][0], v = w0 * t.uv[0][1] + w1 * t.uv[1][1] + w2 * t.uv[2][1];
                    texel = sprite.sample(u, v);
                    if ((texel >>> 24) < 128) return -1;
                }
                int r = (int) Math.min(255, (texel >> 16 & 255) * ftr * light), g = (int) Math.min(255, (texel >> 8 & 255) * ftg * light),
                        b = (int) Math.min(255, (texel & 255) * ftb * light);
                return r << 16 | g << 8 | b;
            });
        }
        return image;
    }

    private interface Colour {
        int at(double w0, double w1, double w2);
    }

    private interface Rasterizer {
        void fill(double[] a, double[] b, double[] c, Colour colour);
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
