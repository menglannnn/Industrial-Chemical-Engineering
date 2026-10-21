package com.wormzjl.icheme.science.equipment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A low-poly part modelled in Blender (owner decision 2026-10-16: "Blender parts, scripted"; the script and its checks are
 * in {@code tools/custom-equipment-rendering-poc/ac-blender/}), read from the mod's OBJ file
 * {@code assets/icheme/parts/<name>.obj}: triangles only, a texture coordinate and a normal at every corner, closed
 * and wound outward. Metres, +y up. Faces are cut into cells like the procedural ones.
 *
 * <p>The OBJ is triangulated, and a chunk draws a triangle as a whole quad: two triangles of one flat quad would cost two
 * chunk quads and two facets. On reading, a triangle is merged with a neighbour across a shared edge when the four
 * corners make a flat convex quad over which the texture coordinates and the normals are one affine field (each within
 * {@link #EXACT} of the neighbour's own corner): the quad then draws exactly as its two triangles did, at any scale and
 * turn, for half the quads (owner question 2026-10-16, measured in {@code QuadShares}).
 */
final class Part {
    private static final Map<String, Part> READ = new ConcurrentHashMap<>();
    /** How far, in metres of the part file, texture fraction or normal, a merged corner may stray from its own value. */
    static final double EXACT = 1e-4;
    /** Per face: its corners (3 or 4, counter-clockwise from outside), and its reference triangle's corners, texture coordinates (v down) and normals. */
    private final List<Face> faces;
    private final int triangles;

    private record Face(double[][] corners, double[][] positions, double[][] uvs, double[][] normals) {
    }

    private Part(List<Face> faces, int triangles) {
        this.faces = faces;
        this.triangles = triangles;
    }

    static Part of(String name) {
        return READ.computeIfAbsent(name, Part::read);
    }

    /** Triangles in the part's file. */
    int triangles() {
        return triangles;
    }

    /** Faces drawn: triangles and merged quads. */
    int faces() {
        return faces.size();
    }

    /**
     * Adds the part to {@code mesh} in material {@code material}: scaled by {@code across} in x and z and by {@code up}
     * in y, turned {@code turns} quarter turns counter-clockwise seen from above, its origin at {@code at}.
     */
    void place(Mesh mesh, double[] at, double across, double up, int turns, Mesh.Material material) {
        double[] scale = {across, up, across};
        for (var face : faces) {
            var corners = new double[face.corners.length][];
            for (int c = 0; c < corners.length; c++) corners[c] = point(face.corners[c], scale, turns, at);
            double[][] p = new double[3][], n = new double[3][];
            for (int c = 0; c < 3; c++) {
                p[c] = point(face.positions[c], scale, turns, at);
                // A normal scales by the inverse of the part's scale.
                n[c] = turn(new double[]{face.normals[c][0] / scale[0], face.normals[c][1] / scale[1], face.normals[c][2] / scale[2]}, turns);
            }
            mesh.imported(corners, p, face.uvs, n, material);
        }
    }

    private static double[] point(double[] p, double[] scale, int turns, double[] at) {
        var q = turn(new double[]{p[0] * scale[0], p[1] * scale[1], p[2] * scale[2]}, turns);
        return new double[]{at[0] + q[0], at[1] + q[1], at[2] + q[2]};
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
        var path = "/assets/icheme/parts/" + name + ".obj";
        var stream = Part.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing part " + path);
        List<double[]> v = new ArrayList<>(), vt = new ArrayList<>(), vn = new ArrayList<>();
        // Per triangle: vertex indices (for shared edges), positions, texture coordinates and normals of its corners.
        List<int[]> index = new ArrayList<>();
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
                        var ids = new int[3];
                        for (int c = 0; c < 3; c++) {
                            var k = w[c + 1].split("/");
                            if (k.length != 3 || k[1].isEmpty()) throw new IllegalStateException("Part " + name + " lacks a texture coordinate or a normal");
                            ids[c] = Integer.parseInt(k[0]) - 1;
                            corners[c] = v.get(ids[c]);
                            uv[c] = vt.get(Integer.parseInt(k[1]) - 1);
                            normal[c] = vn.get(Integer.parseInt(k[2]) - 1);
                        }
                        index.add(ids);
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
        return new Part(merge(index, p, t, n), p.size());
    }

    /**
     * Pairs triangles into quads where {@link #quad} allows, greedily in file order (each triangle's edges in turn, the
     * first neighbour that fits), so a part always reads the same.
     */
    private static List<Face> merge(List<int[]> index, List<double[][]> p, List<double[][]> t, List<double[][]> n) {
        // Each directed edge (from, to) of a triangle, by its vertices; a neighbour has it the other way round.
        var edges = new HashMap<Long, Integer>();
        for (int i = 0; i < index.size(); i++)
            for (int e = 0; e < 3; e++) edges.putIfAbsent(edge(index.get(i)[e], index.get(i)[(e + 1) % 3]), i);
        var used = new boolean[index.size()];
        var faces = new ArrayList<Face>();
        for (int i = 0; i < index.size(); i++) {
            if (used[i]) continue;
            used[i] = true;
            double[][] corners = null;
            for (int e = 0; e < 3 && corners == null; e++) {
                int[] a = index.get(i);
                Integer j = edges.get(edge(a[(e + 1) % 3], a[e]));
                if (j == null || used[j]) continue;
                corners = quad(e, p.get(i), t.get(i), n.get(i), index.get(j), a, p.get(j), t.get(j), n.get(j));
                if (corners != null) used[j] = true;
            }
            faces.add(new Face(corners != null ? corners : p.get(i), p.get(i), t.get(i), n.get(i)));
        }
        return List.copyOf(faces);
    }

    private static long edge(int from, int to) {
        return (long) from << 32 | to & 0xFFFFFFFFL;
    }

    /**
     * The quad of triangle {@code p} and its neighbour {@code q} across {@code p}'s edge {@code e} (corner e to e + 1), or
     * null unless the neighbour's third corner lies in {@code p}'s plane, the quad is convex, and {@code p}'s texture
     * coordinates and normals, extended affinely, meet the neighbour's at that corner.
     */
    private static double[][] quad(int e, double[][] p, double[][] uv, double[][] n, int[] qIndex, int[] pIndex, double[][] q, double[][] quv, double[][] qn) {
        int far = -1;
        for (int c = 0; c < 3; c++) if (qIndex[c] != pIndex[e] && qIndex[c] != pIndex[(e + 1) % 3]) far = c;
        if (far < 0) return null;
        double[] w = q[far], e1 = Mesh.sub(p[1], p[0]), e2 = Mesh.sub(p[2], p[0]), normal = Mesh.cross(e1, e2);
        double area = Math.sqrt(Mesh.dot(normal, normal));
        if (area < EXACT * EXACT) return null;
        // In p's plane: barycentric (r, s, t) of w from the plane's own coordinates.
        var d = Mesh.sub(w, p[0]);
        if (Math.abs(Mesh.dot(d, normal)) / area > EXACT) return null;
        double a = Mesh.dot(e1, e1), b = Mesh.dot(e1, e2), c = Mesh.dot(e2, e2), det = a * c - b * b;
        double s = (c * Mesh.dot(d, e1) - b * Mesh.dot(d, e2)) / det, tt = (a * Mesh.dot(d, e2) - b * Mesh.dot(d, e1)) / det, r = 1 - s - tt;
        for (int k = 0; k < 2; k++) if (Math.abs(r * uv[0][k] + s * uv[1][k] + tt * uv[2][k] - quv[far][k]) > EXACT) return null;
        for (int k = 0; k < 3; k++) if (Math.abs(r * n[0][k] + s * n[1][k] + tt * n[2][k] - qn[far][k]) > EXACT) return null;
        // The quad's corners in p's winding, w between the shared edge's ends (p[e], w, p[e + 1], p[e + 2] in turn), and
        // convex: no turn against p's.
        double[][] corners = {p[e], w, p[(e + 1) % 3], p[(e + 2) % 3]};
        for (int k = 0; k < 4; k++) {
            var turn = Mesh.cross(Mesh.sub(corners[(k + 1) % 4], corners[k]), Mesh.sub(corners[(k + 2) % 4], corners[(k + 1) % 4]));
            if (Mesh.dot(turn, normal) < 0) return null;
        }
        return corners;
    }
}
