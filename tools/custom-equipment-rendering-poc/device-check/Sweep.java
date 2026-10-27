import com.wormzjl.createcheme.science.equipment.LineGeometry;
import com.wormzjl.createcheme.science.equipment.Mesh;
import com.wormzjl.createcheme.science.equipment.PipeSizes;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;

public class Sweep {
    public static void main(String[] args) {
        var keys = new ArrayList<LineGeometry.Key>();
        for (var kind : Kind.values())
            for (int facing : new int[]{1, 2, 5})
                for (int code = 2; code <= 64; code += 3) {
                    boolean inline = kind == Kind.VALVE || kind == Kind.PUMP || kind == Kind.COMPRESSOR || kind == Kind.FILTER;
                    int a = facing & ~1;
                    int[][] patterns = kind == Kind.PIPE ? new int[][]{{}, {5}, {4, 5}, {2, 5}, {1, 3}, {4, 5, 1}, {1, 3, 5}, {0, 1, 2, 3, 4, 5}}
                            : inline ? new int[][]{{}, {a}, {a, a + 1}} : new int[][]{{}, {1}, {1, a, a + 1}, {0, 2, 3, 4, 5}};
                    for (int[] faces : patterns)
                        for (int met : new int[]{0, 6, 40, 64}) {
                            int connections = 0; long codes = 0; int flanged = 0;
                            for (int f : faces) { connections |= 1 << f; codes |= (long) (met == 0 ? code : met) << 7 * f; if (kind != Kind.PIPE || f == 5) flanged |= 1 << f; }
                            keys.add(new LineGeometry.Key(kind, facing, connections, code, codes, flanged, false));
                        }
                }
        var random = new Random(3);
        int bad = 0;
        for (var key : keys) {
            var polygons = LineGeometry.polygons(key);
            int holes = 0;
            for (int ray = 0; ray < 4000; ray++) {
                double[] t0 = {.01 + .98 * random.nextDouble(), .01 + .98 * random.nextDouble(), .01 + .98 * random.nextDouble()};
                double[] d = {random.nextGaussian(), random.nextGaussian(), random.nextGaussian()};
                double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                for (int c = 0; c < 3; c++) d[c] /= l;
                double[] o = {t0[0] - 3 * d[0], t0[1] - 3 * d[1], t0[2] - 3 * d[2]};
                if (key.connected(entry(o, d))) continue;
                double front = 1e9, back = 1e9;
                for (var p : polygons) {
                    double t = BackFaces.hit(p, o, d);
                    if (Double.isNaN(t)) continue;
                    var n = BackFaces.newell(p);
                    if (n[0] * d[0] + n[1] * d[1] + n[2] * d[2] < 0) front = Math.min(front, t); else back = Math.min(back, t);
                }
                if (back < front - 1e-7) holes++;
            }
            if (holes > 0) { bad++; if (bad <= 25) System.out.println(holes + " " + key); }
        }
        System.out.println(bad + " of " + keys.size() + " keys with holes");
    }
    static int entry(double[] o, double[] d) {
        int face = -1; double enter = -1e18; int[][] faces = {{4, 5}, {0, 1}, {2, 3}};
        for (int a = 0; a < 3; a++) {
            if (Math.abs(d[a]) < 1e-12) continue;
            double t = ((d[a] > 0 ? 0 : 1) - o[a]) / d[a];
            if (t > enter) { enter = t; face = faces[a][d[a] > 0 ? 0 : 1]; }
        }
        return face;
    }
}
