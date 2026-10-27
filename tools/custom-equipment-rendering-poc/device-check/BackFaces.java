import com.wormzjl.createcheme.science.equipment.LineGeometry;
import com.wormzjl.createcheme.science.equipment.Mesh;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;

public class BackFaces {
    public static void main(String[] args) {
        var key = new LineGeometry.Key(Kind.valueOf(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), 0, 0, false);
        var polygons = LineGeometry.polygons(key);
        var random = new Random(1);
        var seen = new TreeMap<String, Integer>();
        int bad = 0;
        for (int ray = 0; ray < 200000; ray++) {
            double[] t0 = {.02 + .96 * random.nextDouble(), .02 + .96 * random.nextDouble(), .02 + .96 * random.nextDouble()};
            double[] d = {random.nextGaussian(), random.nextGaussian(), random.nextGaussian()};
            double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            for (int c = 0; c < 3; c++) d[c] /= l;
            double[] o = {t0[0] - 3 * d[0], t0[1] - 3 * d[1], t0[2] - 3 * d[2]};
            double front = 1e9, back = 1e9; Mesh.Polygon bp = null;
            for (var p : polygons) {
                double t = hit(p, o, d);
                if (Double.isNaN(t)) continue;
                var n = newell(p);
                if (n[0] * d[0] + n[1] * d[1] + n[2] * d[2] < 0) front = Math.min(front, t);
                else if (t < back) { back = t; bp = p; }
            }
            if (back < front - 1e-7) {
                bad++;
                var n = newell(bp); double nl = Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
                double[] c = new double[3];
                for (int i = 0; i < bp.size(); i++) for (int a = 0; a < 3; a++) c[a] += bp.positions()[3 * i + a] / bp.size();
                String k = String.format(Locale.ROOT, "%s n=(%.2f,%.2f,%.2f) c=(%.3f,%.3f,%.3f) gap=%.2e", bp.material(), n[0]/nl, n[1]/nl, n[2]/nl, c[0], c[1], c[2], Math.min(front, 9) - back);
                seen.merge(k.substring(0, k.indexOf(" gap")), 1, Integer::sum);
            }
        }
        System.out.println(bad + " of 200000 rays");
        seen.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(12).forEach(e -> System.out.println(e.getValue() + " " + e.getKey()));
    }
    static double hit(Mesh.Polygon polygon, double[] o, double[] d) {
        double[] p = polygon.positions(), n = newell(polygon);
        double along = n[0] * d[0] + n[1] * d[1] + n[2] * d[2];
        if (Math.abs(along) < 1e-12) return Double.NaN;
        double t = (n[0] * (p[0] - o[0]) + n[1] * (p[1] - o[1]) + n[2] * (p[2] - o[2])) / along;
        double[] q = {o[0] + t * d[0], o[1] + t * d[1], o[2] + t * d[2]};
        int m = polygon.size();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            double[] e = {p[3 * j] - p[3 * i], p[3 * j + 1] - p[3 * i + 1], p[3 * j + 2] - p[3 * i + 2]};
            double[] w = {q[0] - p[3 * i], q[1] - p[3 * i + 1], q[2] - p[3 * i + 2]};
            double[] c = {e[1] * w[2] - e[2] * w[1], e[2] * w[0] - e[0] * w[2], e[0] * w[1] - e[1] * w[0]};
            if (c[0] * n[0] + c[1] * n[1] + c[2] * n[2] < -1e-12) return Double.NaN;
        }
        return t;
    }
    static double[] newell(Mesh.Polygon polygon) {
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
