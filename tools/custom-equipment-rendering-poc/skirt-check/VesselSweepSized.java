import com.wormzjl.createcheme.science.equipment.*;
import com.wormzjl.createcheme.science.equipment.Mesh.Polygon;
import java.util.*;

/** Sight lines from outside a whole vessel: the first polygon met must face the viewer (cells walked in order). */
public class VesselSweepSized {
    record P(double[] pos, double[] n, Mesh.Material material) {}
    static double OUTSIDE;

    public static void main(String[] args) {
        var shapes = new LinkedHashMap<String, EquipmentShape>();
        shapes.put("drum 3x6 skirt", EquipmentShape.drum(3, 6, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 3));
        shapes.put("drum 2.5x4 saddles", EquipmentShape.drum(2.5, 4, EquipmentShape.Orientation.HORIZONTAL, EquipmentShape.Heading.EAST, 1));
        shapes.put("drum 6x10 skirt", EquipmentShape.drum(6, 10, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.EAST, 3));
        shapes.put("drum 2x4 skirt", EquipmentShape.drum(2, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2));
        shapes.put("drum .5x2 skirt", EquipmentShape.drum(.5, 2, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2));
        shapes.put("drum 3x4 skirt1", EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 1));
        shapes.put("drum 1x2 skirt", EquipmentShape.drum(1, 2, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.SOUTH, 2));
        shapes.put("column 3m 20 trays", EquipmentShape.column(3, ColumnTrays.simple(20, 10), EquipmentShape.Heading.NORTH, 3));
        shapes.put("column 6m 40 trays", EquipmentShape.column(6, ColumnTrays.simple(40, 20), EquipmentShape.Heading.WEST, 4));
        shapes.put("drum 3x4 hillside", EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2, java.util.List.of(new EquipmentShape.Connection(3, 4, 0), new EquipmentShape.Connection(1, 4, 0), new EquipmentShape.Connection(2, 2, 1), new EquipmentShape.Connection(0, 4, -1))));
        shapes.put("drum 4x12 lying 5 nozzles", EquipmentShape.drum(4, 12, EquipmentShape.Orientation.HORIZONTAL, EquipmentShape.Heading.EAST, 2, java.util.List.of(new EquipmentShape.Connection(1, 4, 0), new EquipmentShape.Connection(0, 6, 1), new EquipmentShape.Connection(2, 6, -1), new EquipmentShape.Connection(3, 6, 0), new EquipmentShape.Connection(1, 9, 1))));
        int rays = Integer.parseInt(args.length > 0 ? args[0] : "3000");
        OUTSIDE = Double.parseDouble(args.length > 1 ? args[1] : "1");
        for (var entry : shapes.entrySet()) {
            var shape = entry.getValue();
            var layout = ShapeRasterizer.layout(shape);
            long codes = 0;
            for (int k = 0; k < shape.nozzles().size(); k++) codes = ShapeRasterizer.withNozzle(codes, k, OUTSIDE);
            var cells = new HashMap<Long, List<P>>();
            int[] lo = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, hi = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            int count = 0;
            for (var c : layout.shell()) {
                var list = new ArrayList<P>();
                for (Polygon p : ShapeRasterizer.surface(ShapeRasterizer.key(shape, c.x(), c.y(), c.z(), codes))) {
                    var pos = p.positions().clone();
                    for (int i = 0; i < p.size(); i++) { pos[3 * i] += c.x(); pos[3 * i + 1] += c.y(); pos[3 * i + 2] += c.z(); }
                    list.add(new P(pos, newell(pos), p.material()));
                    count++;
                }
                cells.put(pack(c.x(), c.y(), c.z()), list);
                int[] v = {c.x(), c.y(), c.z()};
                for (int a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], v[a]); hi[a] = Math.max(hi[a], v[a] + 1); }
            }
            var random = new Random(5);
            var seen = new TreeMap<String, Integer>();
            int holes = 0, hits = 0;
            for (int ray = 0; ray < rays; ray++) {
                double[] t0 = new double[3], d = new double[3];
                for (int a = 0; a < 3; a++) t0[a] = lo[a] + (hi[a] - lo[a]) * random.nextDouble();
                double l = 0;
                for (int a = 0; a < 3; a++) { d[a] = random.nextGaussian(); l += d[a] * d[a]; }
                l = Math.sqrt(l);
                for (int a = 0; a < 3; a++) d[a] /= l;
                double far = 0; for (int a = 0; a < 3; a++) far += (hi[a] - lo[a]) * (hi[a] - lo[a]);
                far = Math.sqrt(far) + 2;
                double[] o = {t0[0] - far * d[0], t0[1] - far * d[1], t0[2] - far * d[2]};
                // Walk cells along the line (Amanatides-Woo) from the origin.
                int[] cell = {(int) Math.floor(o[0]), (int) Math.floor(o[1]), (int) Math.floor(o[2])};
                int[] step = new int[3]; double[] tMax = new double[3], tDelta = new double[3];
                for (int a = 0; a < 3; a++) {
                    step[a] = d[a] > 0 ? 1 : -1;
                    tDelta[a] = Math.abs(1 / d[a]);
                    double next = d[a] > 0 ? cell[a] + 1 - o[a] : o[a] - cell[a];
                    tMax[a] = next * tDelta[a];
                }
                double tEnd = 2 * far, tCell = 0;
                P found = null; boolean back = false;
                while (tCell < tEnd) {
                    var list = cells.get(pack(cell[0], cell[1], cell[2]));
                    int a = tMax[0] < tMax[1] ? (tMax[0] < tMax[2] ? 0 : 2) : (tMax[1] < tMax[2] ? 1 : 2);
                    double tExit = tMax[a];
                    if (list != null) {
                        double front = 1e18, rear = 1e18; P rp = null;
                        for (var p : list) {
                            double t = hit(p, o, d);
                            if (Double.isNaN(t) || t < tCell - 1e-9 || t > tExit + 1e-9) continue;
                            if (p.n[0] * d[0] + p.n[1] * d[1] + p.n[2] * d[2] < 0) front = Math.min(front, t);
                            else if (t < rear) { rear = t; rp = p; }
                        }
                        if (front < 1e18 || rear < 1e18) {
                            hits++;
                            if (rear < front - 1e-7) { back = true; found = rp; }
                            break;
                        }
                    }
                    cell[a] += step[a]; tCell = tExit; tMax[a] += tDelta[a];
                }
                if (back) {
                    holes++;
                    double[] c = new double[3]; int m = found.pos.length / 3;
                    for (int i = 0; i < m; i++) for (int a = 0; a < 3; a++) c[a] += found.pos[3 * i + a] / m;
                    double nl = Math.sqrt(found.n[0] * found.n[0] + found.n[1] * found.n[1] + found.n[2] * found.n[2]);
                    seen.merge(String.format(Locale.ROOT, "%s n=(%.1f,%.1f,%.1f) at (%.1f,%.1f,%.1f)", found.material, found.n[0] / nl, found.n[1] / nl, found.n[2] / nl, c[0], c[1], c[2]), 1, Integer::sum);
                }
            }
            System.out.println(entry.getKey() + ": " + count + " polygons, " + hits + " sight lines met it, " + holes + " met a back face first");
            seen.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(8).forEach(e -> System.out.println("   " + e.getValue() + " " + e.getKey()));
        }
    }
    static long pack(int x, int y, int z) { return ((long) (x + 4096) << 42) | ((long) (y + 4096) << 21) | (z + 4096); }
    static double hit(P poly, double[] o, double[] d) {
        double[] p = poly.pos, n = poly.n;
        double along = n[0] * d[0] + n[1] * d[1] + n[2] * d[2];
        if (Math.abs(along) < 1e-12) return Double.NaN;
        double t = (n[0] * (p[0] - o[0]) + n[1] * (p[1] - o[1]) + n[2] * (p[2] - o[2])) / along;
        double[] q = {o[0] + t * d[0], o[1] + t * d[1], o[2] + t * d[2]};
        int m = p.length / 3;
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            double[] e = {p[3 * j] - p[3 * i], p[3 * j + 1] - p[3 * i + 1], p[3 * j + 2] - p[3 * i + 2]};
            double[] w = {q[0] - p[3 * i], q[1] - p[3 * i + 1], q[2] - p[3 * i + 2]};
            double[] c = {e[1] * w[2] - e[2] * w[1], e[2] * w[0] - e[0] * w[2], e[0] * w[1] - e[1] * w[0]};
            if (c[0] * n[0] + c[1] * n[1] + c[2] * n[2] < -1e-12) return Double.NaN;
        }
        return t;
    }
    static double[] newell(double[] p) {
        double[] n = new double[3]; int m = p.length / 3;
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            n[0] += (p[3 * i + 1] - p[3 * j + 1]) * (p[3 * i + 2] + p[3 * j + 2]);
            n[1] += (p[3 * i + 2] - p[3 * j + 2]) * (p[3 * i] + p[3 * j]);
            n[2] += (p[3 * i] - p[3 * j]) * (p[3 * i + 1] + p[3 * j + 1]);
        }
        return n;
    }
}
