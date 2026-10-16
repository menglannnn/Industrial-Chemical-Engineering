package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.LineGeometry.Key;
import com.wormzjl.createcheme.science.equipment.Mesh.Material;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineGeometryTest {
    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    private static final double[] SIZES = {1 / 16.0, .15, .4, 1};

    /** A key whose connected faces meet lines of {@code met} outside (0: own size), flanged where {@code flanged} says. */
    private static Key key(Kind kind, int facing, double own, int[] faces, double met, int flanged, boolean spool) {
        int connections = 0;
        long codes = 0;
        for (int face : faces) {
            connections |= 1 << face;
            codes |= (long) PipeSizes.code(met > 0 ? met : own) << 7 * face;
        }
        return new Key(kind, facing, connections, PipeSizes.code(own), codes, flanged, spool);
    }

    private static List<Key> keys() {
        var keys = new ArrayList<Key>();
        int[][] arms = {{}, {EAST}, {WEST, EAST}, {DOWN, UP}, {NORTH, EAST}, {UP, SOUTH}, {WEST, EAST, UP}, {WEST, EAST, NORTH, SOUTH}, {UP, EAST, SOUTH}};
        for (double od : SIZES)
            for (int[] faces : arms)
                for (int flanged : new int[]{0, 1 << EAST | 1 << UP}) {
                    keys.add(key(Kind.PIPE, NORTH, od, faces, 0, flanged, false));
                    keys.add(key(Kind.PIPE, NORTH, od, faces, od / 2, flanged, false));
                }
        for (double od : SIZES) keys.add(key(Kind.PIPE, EAST, od, new int[]{WEST, EAST}, 0, 0, true));
        for (var kind : Kind.values()) {
            if (kind == Kind.PIPE) continue;
            for (int facing : new int[]{NORTH, EAST, UP})
                for (double od : SIZES) {
                    int a = facing & ~1;
                    boolean inline = kind == Kind.VALVE || kind == Kind.PUMP || kind == Kind.COMPRESSOR || kind == Kind.FILTER;
                    int[] faces = inline ? new int[]{a, a + 1} : new int[]{UP, a, a + 1};
                    int flanged = 0;
                    for (int face : faces) flanged |= 1 << face;
                    keys.add(key(kind, facing, od, faces, 0, flanged, false));
                    keys.add(key(kind, facing, od, new int[0], 0, 0, false));
                }
            keys.add(Key.item(kind));
        }
        keys.add(Key.item(Kind.PIPE));
        return keys;
    }

    @Test
    void everyPieceStaysInItsCellAndFacesOutwards() {
        for (var key : keys()) {
            var polygons = LineGeometry.polygons(key);
            assertFalse(polygons.isEmpty(), key.toString());
            for (var polygon : polygons) {
                assertTrue(polygon.size() >= 3);
                for (double v : polygon.positions()) assertTrue(v >= 0 && v <= 1, key + " position " + v);
                for (double v : polygon.uvs()) assertTrue(v >= 0 && v <= 1, key + " uv " + v);
                double[] face = newell(polygon), smooth = new double[3];
                for (int i = 0; i < polygon.size(); i++)
                    for (int c = 0; c < 3; c++) smooth[c] += polygon.normals()[3 * i + c];
                assertTrue(face[0] * smooth[0] + face[1] * smooth[1] + face[2] * smooth[2] > 0, key + " winding " + polygon.material());
            }
        }
    }

    @Test
    void runsElbowsReducersAndFlangesAreWatertightBetweenTheirFaces() {
        // Every edge is shared by two facets, except the rims left open on a face plane where the next block continues.
        for (double od : SIZES)
            for (int[] faces : new int[][]{{WEST, EAST}, {DOWN, UP}, {NORTH, EAST}, {UP, SOUTH}, {DOWN, WEST}})
                for (double met : new double[]{0, od / 2})
                    for (int flanged : new int[]{0, 1 << faces[0]})
                        for (boolean spool : new boolean[]{false, true}) {
                            if (spool && (faces[0] ^ 1) != faces[1]) continue;
                            var key = key(Kind.PIPE, NORTH, od, faces, met, flanged, spool);
                            double drawn = PipeSizes.outsideOf(key.outside()), room = .5 - 1.05 * drawn / 2;
                            // A ball corner (no room for a bend) is open inside by design: its arms end in the ball.
                            if ((faces[0] ^ 1) != faces[1] && LineGeometry.bend(LineGeometry.fit(LineGeometry.end(key, faces[0], drawn), room),
                                    LineGeometry.fit(LineGeometry.end(key, faces[1], drawn), room), drawn) == 0) continue;
                            var edges = new HashMap<String, Integer>();
                            var ends = new HashMap<String, double[][]>();
                            for (var facet : LineGeometry.mesh(key).facets()) {
                                var p = facet.positions();
                                for (int i = 0; i < p.size(); i++) {
                                    double[] a = p.get(i), b = p.get((i + 1) % p.size());
                                    String ka = round(a), kb = round(b), edge = ka.compareTo(kb) < 0 ? ka + "|" + kb : kb + "|" + ka;
                                    edges.merge(edge, 1, Integer::sum);
                                    ends.put(edge, new double[][]{a, b});
                                }
                            }
                            for (var entry : edges.entrySet()) {
                                if (entry.getValue() == 2) continue;
                                assertEquals(1, entry.getValue(), key + " edge used " + entry.getValue() + " times");
                                double[][] e = ends.get(entry.getKey());
                                assertTrue(onFacePlane(e[0]) && onFacePlane(e[1]), key + " open edge off the face planes at " + round(e[0]));
                            }
                        }
    }

    @Test
    void aReducerNarrowsTheLargerPipeToTheSmallerAtTheFace() {
        // A 0.4 m pipe meeting a 0.15 m one: the large block ends at 0.15 m on the shared face, the small one is straight.
        var large = key(Kind.PIPE, EAST, .4, new int[]{WEST, EAST}, .15, 0, false);
        var small = key(Kind.PIPE, EAST, .15, new int[]{WEST, EAST}, .4, 0, false);
        double narrow = PipeSizes.outsideOf(PipeSizes.code(.15)) / 2, wide = PipeSizes.outsideOf(PipeSizes.code(.4)) / 2;
        assertEquals(narrow, rimRadius(large, 0, 1, Material.PIPE), 1e-9);
        assertEquals(narrow, rimRadius(small, 0, 0, Material.PIPE), 1e-9);
        // The large pipe meets the 0.15 m line at both ends here (both faces carry it), so it narrows at both.
        assertEquals(narrow, rimRadius(large, 0, 0, Material.PIPE), 1e-9);
        var meetsLarge = key(Kind.PIPE, EAST, .4, new int[]{WEST, EAST}, 0, 0, false);
        assertEquals(wide, rimRadius(meetsLarge, 0, 0, Material.PIPE), 1e-9);
    }

    @Test
    void flangePairsMatchAcrossTheJoint() {
        // A pipe flanged to a pump on its east face and the pump's west nozzle draw flanges of one size on the shared plane.
        for (double od : new double[]{1 / 16.0, .15, .4}) {
            var pipe = key(Kind.PIPE, EAST, od, new int[]{WEST, EAST}, 0, 1 << EAST, false);
            var pump = key(Kind.PUMP, EAST, .05, new int[]{WEST, EAST}, od, 1 << WEST | 1 << EAST, false);
            double expected = PipeSizes.flange(PipeSizes.outsideOf(PipeSizes.code(od))) / 2;
            assertEquals(expected, rimRadius(pipe, 0, 1, Material.FLANGE), 1e-9, "pipe " + od);
            assertEquals(expected, rimRadius(pump, 0, 0, Material.FLANGE), 1e-9, "pump " + od);
        }
    }

    @Test
    void devicesStandAsPhotographed() {
        // A control valve's actuator above a horizontal line and beside a vertical one; a pump's motor on top; a
        // strainer's leg below the line and downstream.
        assertTrue(centroid(LineGeometry.polygons(key(Kind.VALVE, EAST, .1, new int[0], 0, 0, false)), Material.ACTUATOR)[1] > .7);
        var vertical = centroid(LineGeometry.polygons(key(Kind.VALVE, UP, .1, new int[0], 0, 0, false)), Material.ACTUATOR);
        assertTrue(vertical[2] < .3 && Math.abs(vertical[1] - .5) < .1, List.of(vertical[0], vertical[1], vertical[2]).toString());
        assertTrue(centroid(LineGeometry.polygons(key(Kind.PUMP, NORTH, .1, new int[0], 0, 0, false)), Material.MOTOR)[1] > .7);
        var leg = new double[3];
        int count = 0;
        for (var polygon : LineGeometry.polygons(key(Kind.FILTER, EAST, .15, new int[0], 0, 0, false)))
            for (int i = 0; i < polygon.size(); i++)
                if (polygon.positions()[3 * i + 1] < .3) {
                    for (int c = 0; c < 3; c++) leg[c] += polygon.positions()[3 * i + c];
                    count++;
                }
        assertTrue(count > 0 && leg[0] / count > .6, "leg points downstream");
    }

    /** Largest distance from the line's centre of {@code material} vertices on the plane {@code axis} = {@code plane}. */
    private static double rimRadius(Key key, int axis, double plane, Material material) {
        double best = 0;
        for (var polygon : LineGeometry.polygons(key)) {
            if (polygon.material() != material) continue;
            for (int i = 0; i < polygon.size(); i++) {
                double[] p = {polygon.positions()[3 * i], polygon.positions()[3 * i + 1], polygon.positions()[3 * i + 2]};
                if (Math.abs(p[axis] - plane) > 1e-9) continue;
                double d = 0;
                for (int c = 0; c < 3; c++) if (c != axis) d += (p[c] - .5) * (p[c] - .5);
                best = Math.max(best, Math.sqrt(d));
            }
        }
        return best;
    }

    private static double[] centroid(List<Mesh.Polygon> polygons, Material material) {
        var c = new double[3];
        int n = 0;
        for (var polygon : polygons) {
            if (polygon.material() != material) continue;
            for (int i = 0; i < polygon.size(); i++, n++)
                for (int a = 0; a < 3; a++) c[a] += polygon.positions()[3 * i + a];
        }
        assertTrue(n > 0, material + " drawn");
        for (int a = 0; a < 3; a++) c[a] /= n;
        return c;
    }

    private static boolean onFacePlane(double[] p) {
        for (double v : p) if (Math.abs(v) < 1e-9 || Math.abs(v - 1) < 1e-9) return true;
        return false;
    }

    private static String round(double[] p) {
        return Math.round(p[0] * 1e7) + "," + Math.round(p[1] * 1e7) + "," + Math.round(p[2] * 1e7);
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
