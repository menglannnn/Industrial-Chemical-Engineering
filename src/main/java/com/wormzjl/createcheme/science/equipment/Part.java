package com.wormzjl.createcheme.science.equipment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A low-poly part modelled in Blender (owner decision 2026-10-16: "Blender parts, scripted"; the script and its checks are
 * in {@code tools/custom-equipment-rendering-poc/ac-blender/}), read from the mod's OBJ file
 * {@code assets/createcheme/parts/<name>.obj}: triangles only, a texture coordinate and a normal at every corner, closed
 * and wound outward. Metres, +y up. Each triangle becomes a facet of its own ({@link Mesh#triangle}), so parts are cut
 * into cells like the procedural ones.
 */
final class Part {
    private static final Map<String, Part> READ = new ConcurrentHashMap<>();
    /** Per triangle and corner: position, texture coordinate (v down, as a sprite's) and normal. */
    private final double[][][] positions, uvs, normals;

    private Part(double[][][] positions, double[][][] uvs, double[][][] normals) {
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
    }

    static Part of(String name) {
        return READ.computeIfAbsent(name, Part::read);
    }

    int triangles() {
        return positions.length;
    }

    /**
     * Adds the part to {@code mesh} in material {@code material}: scaled by {@code across} in x and z and by {@code up}
     * in y, turned {@code turns} quarter turns counter-clockwise seen from above, its origin at {@code at}.
     */
    void place(Mesh mesh, double[] at, double across, double up, int turns, Mesh.Material material) {
        double[] scale = {across, up, across};
        for (int t = 0; t < positions.length; t++) {
            double[][] p = new double[3][], n = new double[3][];
            for (int c = 0; c < 3; c++) {
                var q = turn(new double[]{positions[t][c][0] * scale[0], positions[t][c][1] * scale[1], positions[t][c][2] * scale[2]}, turns);
                p[c] = new double[]{at[0] + q[0], at[1] + q[1], at[2] + q[2]};
                // A normal scales by the inverse of the part's scale.
                n[c] = turn(new double[]{normals[t][c][0] / scale[0], normals[t][c][1] / scale[1], normals[t][c][2] / scale[2]}, turns);
            }
            mesh.triangle(p, uvs[t], n, material);
        }
    }

    /** {@code p} turned {@code turns} quarter turns counter-clockwise about +y seen from above, as {@link Mesh.Transform#yaw}. */
    private static double[] turn(double[] p, int turns) {
        double x = p[0], z = p[2];
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> new double[]{z, p[1], -x};
            case 2 -> new double[]{-x, p[1], -z};
            case 3 -> new double[]{-z, p[1], x};
            default -> p;
        };
    }

    /** @throws IllegalStateException for a missing or malformed part file */
    private static Part read(String name) {
        var path = "/assets/createcheme/parts/" + name + ".obj";
        var stream = Part.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing part " + path);
        List<double[]> v = new ArrayList<>(), vt = new ArrayList<>(), vn = new ArrayList<>();
        List<double[][]> p = new ArrayList<>(), t = new ArrayList<>(), n = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                var w = line.trim().split("\\s+");
                switch (w[0]) {
                    case "v" -> v.add(new double[]{Double.parseDouble(w[1]), Double.parseDouble(w[2]), Double.parseDouble(w[3])});
                    // OBJ puts v = 0 at the bottom of the image, a sprite at its top.
                    case "vt" -> vt.add(new double[]{Double.parseDouble(w[1]), 1 - Double.parseDouble(w[2])});
                    case "vn" -> vn.add(new double[]{Double.parseDouble(w[1]), Double.parseDouble(w[2]), Double.parseDouble(w[3])});
                    case "f" -> {
                        if (w.length != 4) throw new IllegalStateException("Part " + name + " has a face that is not a triangle");
                        double[][] corners = new double[3][], uv = new double[3][], normal = new double[3][];
                        for (int c = 0; c < 3; c++) {
                            var k = w[c + 1].split("/");
                            if (k.length != 3 || k[1].isEmpty()) throw new IllegalStateException("Part " + name + " lacks a texture coordinate or a normal");
                            corners[c] = v.get(Integer.parseInt(k[0]) - 1);
                            uv[c] = vt.get(Integer.parseInt(k[1]) - 1);
                            normal[c] = vn.get(Integer.parseInt(k[2]) - 1);
                        }
                        p.add(corners);
                        t.add(uv);
                        n.add(normal);
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
        if (p.isEmpty()) throw new IllegalStateException("Part " + name + " has no triangles");
        return new Part(p.toArray(double[][][]::new), t.toArray(double[][][]::new), n.toArray(double[][][]::new));
    }
}
