package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;
import java.util.*;

public class MoveDiff {
    static double area(Mesh.Polygon polygon) {
        double[] p = polygon.positions(), n = new double[3];
        int m = polygon.size();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            n[0] += (p[3 * i + 1] - p[3 * j + 1]) * (p[3 * i + 2] + p[3 * j + 2]);
            n[1] += (p[3 * i + 2] - p[3 * j + 2]) * (p[3 * i] + p[3 * j]);
            n[2] += (p[3 * i] - p[3 * j]) * (p[3 * i + 1] + p[3 * j + 1]);
        }
        return .5 * Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
    }
    static String c(double[] p, int m, double[] off) {
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < m; i++) { x += p[3 * i]; y += p[3 * i + 1]; z += p[3 * i + 2]; }
        return String.format("(%.3f %.3f %.3f)", x / m + off[0], y / m + off[1], z / m + off[2]);
    }
    public static void main(String[] a) {
        var shape = EquipmentShape.unit(Kind.COMPRESSOR, 1.1, 2, Heading.NORTH, 3);
        var still = new TreeMap<String, Double>();
        for (var cell : ShapeRasterizer.layout(shape).shell()) {
            var key = ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z());
            var fixed = new HashMap<String, Integer>();
            for (var p : ShapeRasterizer.fixedSurface(key)) fixed.merge(Arrays.toString(p.positions()), 1, Integer::sum);
            for (var p : ShapeRasterizer.surface(key)) {
                String k = Arrays.toString(p.positions());
                if (fixed.getOrDefault(k, 0) > 0) { fixed.merge(k, -1, Integer::sum); continue; }
                still.merge(c(p.positions(), p.size(), new double[]{cell.x(), cell.y(), cell.z()}) + " " + p.material(), area(p), Double::sum);
            }
        }
        var moving = new TreeMap<String, Double>();
        for (var part : ShapeRasterizer.moving(shape)) {
            double[] pv = part.motion().pivot();
            for (var p : part.polygons()) moving.merge(c(p.positions(), p.size(), pv) + " " + p.material(), area(p), Double::sum);
        }
        double s = 0, m = 0;
        for (var e : still.entrySet()) s += e.getValue();
        for (var e : moving.entrySet()) m += e.getValue();
        System.out.println("still " + s + " moving " + m + " pieces " + still.size() + " / " + moving.size());
        var bymat = new TreeMap<String, double[]>();
        for (var e : still.entrySet()) bymat.computeIfAbsent(e.getKey().replaceAll(".* ", ""), k -> new double[2])[0] += e.getValue();
        for (var e : moving.entrySet()) bymat.computeIfAbsent(e.getKey().replaceAll(".* ", ""), k -> new double[2])[1] += e.getValue();
        for (var e : bymat.entrySet()) System.out.printf("%s still %.4f moving %.4f%n", e.getKey(), e.getValue()[0], e.getValue()[1]);
        for (var e : still.entrySet()) if (!moving.containsKey(e.getKey()) || Math.abs(moving.get(e.getKey()) - e.getValue()) > 1e-6) System.out.println("still only " + e.getKey() + " " + e.getValue() + " moving " + moving.get(e.getKey()));
        for (var e : moving.entrySet()) if (!still.containsKey(e.getKey())) System.out.println("moving only " + e.getKey() + " " + e.getValue());
    }
}
