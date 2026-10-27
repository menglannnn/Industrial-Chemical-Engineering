import java.util.*;

/**
 * Offline estimate for batch 2026-10-15-custom-equipment-rendering. No Minecraft classes.
 *
 * A vertical vessel = regular N-gon prism (the faceted cylinder) + two 2:1 ellipsoidal heads (depth D/4),
 * placed on the block grid (1 block = 1 m). For each case it counts what each rendering route would carry:
 * blocks to place, shell cells (cells crossed by the surface), unique per-cell geometry keys (body cells
 * repeat along the axis), chunk-mesh quads after clipping every facet to its block cell (route A), the uncut
 * mesh a single renderer would draw every frame (route B), and per-cell collision boxes after a greedy merge
 * of the solid at 1/16 and 1/8 block resolution.
 *
 * Run: java EquipmentGeometryEstimate.java
 */
public final class EquipmentGeometryEstimate {
    static final double EPS = 1e-9;

    record Case(String name, double diameter, double tangentLength) {}

    public static void main(String[] args) {
        var cases = List.of(
                new Case("small drum", 2, 3),
                new Case("flash drum", 3, 9),
                new Case("reactor", 4, 5),
                new Case("column", 2, 30),
                new Case("large column", 6, 40),
                new Case("max column", 10, 60));
        System.out.println("| case | D m | T/T m | facets | blocks | shell cells | unique keys | chunk quads (cell UV) | chunk quads (arc UV) | uncut quads (BER) | boxes/cell 1/16 mean, max | boxes/cell 1/8 mean, max |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|");
        for (var c : cases) run(c);
    }

    static void run(Case c) {
        var v = new Vessel(c.diameter(), c.tangentLength());
        Set<Long> shell = new HashSet<>();
        long quadsCell = 0, quadsArc = 0;

        // Body facets: vertical rectangles, split where the chord crosses a cell boundary (x or z integer),
        // optionally where the arc-length texture coordinate crosses an integer, and at every integer y.
        double chord = 2 * v.r * Math.sin(Math.PI / v.n);
        var layers = breaks(v.yBottom, v.yTop);
        for (int i = 0; i < v.n; i++) {
            double ax = v.cx + v.r * Math.cos(v.theta(i)), az = v.cz + v.r * Math.sin(v.theta(i));
            double bx = v.cx + v.r * Math.cos(v.theta(i + 1)), bz = v.cz + v.r * Math.sin(v.theta(i + 1));
            var cellT = new TreeSet<Double>();
            addCrossings(cellT, ax, bx);
            addCrossings(cellT, az, bz);
            var arcT = new TreeSet<>(cellT);
            double u0 = i * chord, u1 = (i + 1) * chord;
            for (double k = Math.floor(u0) + 1; k < u1 - EPS; k++) arcT.add((k - u0) / chord);
            quadsCell += (long) (cellT.size() + 1) * (layers.size() - 1);
            quadsArc += (long) (arcT.size() + 1) * (layers.size() - 1);
            var ts = new ArrayList<Double>();
            ts.add(0.0);
            ts.addAll(cellT);
            ts.add(1.0);
            for (int s = 0; s + 1 < ts.size(); s++) {
                double tm = (ts.get(s) + ts.get(s + 1)) / 2;
                int x = (int) Math.floor(ax + tm * (bx - ax)), z = (int) Math.floor(az + tm * (bz - az));
                for (int l = 0; l + 1 < layers.size(); l++)
                    shell.add(key(x, (int) Math.floor((layers.get(l) + layers.get(l + 1)) / 2), z));
            }
        }

        // Heads: planar band facets (quads, triangles at the apex) clipped to every cell they cross.
        long headQuads = 0;
        for (var poly : v.headPolygons()) {
            double[] lo = {1e9, 1e9, 1e9}, hi = {-1e9, -1e9, -1e9};
            for (var p : poly) for (int a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], p[a]); hi[a] = Math.max(hi[a], p[a]); }
            for (int x = (int) Math.floor(lo[0]); x < Math.ceil(hi[0]); x++)
                for (int y = (int) Math.floor(lo[1]); y < Math.ceil(hi[1]); y++)
                    for (int z = (int) Math.floor(lo[2]); z < Math.ceil(hi[2]); z++) {
                        var clipped = clipToCell(poly, x, y, z);
                        if (clipped.size() < 3 || area(clipped) < 1e-7) continue;
                        headQuads += Math.max(1, (clipped.size() - 1) / 2); // convex m-gon -> ceil((m-2)/2) quads
                        shell.add(key(x, y, z));
                    }
        }
        quadsCell += headQuads;
        quadsArc += headQuads;

        // Interior cells: inside the solid and not crossed by the surface.
        int interior = 0;
        int width = (int) Math.ceil(c.diameter()), height = (int) Math.ceil(v.height);
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < width; z++)
            if (!shell.contains(key(x, y, z)) && v.inside(x + .5, y + .5, z + .5)) interior++;

        // Unique geometry keys: a cell wholly inside the body repeats at every height, so its key drops y.
        Map<String, int[]> unique = new HashMap<>(); // key -> {x, y, z, occurrences}
        for (long k : shell) {
            int x = (int) (k >> 40), y = (int) ((k >> 20) & 0xFFFFF), z = (int) (k & 0xFFFFF);
            boolean body = y >= v.yBottom - EPS && y + 1 <= v.yTop + EPS;
            String id = body ? "b" + x + "," + z : "h" + x + "," + y + "," + z;
            unique.computeIfAbsent(id, s -> new int[]{x, y, z, 0})[3]++;
        }

        double[] fine = boxStats(v, unique.values(), 16), coarse = boxStats(v, unique.values(), 8);
        long uncut = (long) v.n * (long) Math.ceil(c.tangentLength()) + 2L * v.n * v.bands;
        System.out.printf(Locale.ROOT, "| %s | %.0f | %.0f | %d | %d | %d | %d | %d | %d | %d | %.1f, %.0f | %.1f, %.0f |%n",
                c.name(), c.diameter(), c.tangentLength(), v.n, interior + shell.size(), shell.size(), unique.size(),
                quadsCell, quadsArc, uncut, fine[0], fine[1], coarse[0], coarse[1]);
    }

    /** Mean (weighted by occurrences) and max greedy-merged box count of the solid inside each unique cell. */
    static double[] boxStats(Vessel v, Collection<int[]> cells, int res) {
        double sum = 0, max = 0, count = 0;
        for (var cell : cells) {
            var grid = new boolean[res][res][res];
            for (int a = 0; a < res; a++) for (int b = 0; b < res; b++) for (int d = 0; d < res; d++)
                grid[a][b][d] = v.inside(cell[0] + (a + .5) / res, cell[1] + (b + .5) / res, cell[2] + (d + .5) / res);
            int boxes = greedyBoxes(grid, res);
            sum += (double) boxes * cell[3];
            count += cell[3];
            max = Math.max(max, boxes);
        }
        return new double[]{sum / count, max};
    }

    static int greedyBoxes(boolean[][][] g, int s) {
        var used = new boolean[s][s][s];
        int count = 0;
        for (int x = 0; x < s; x++) for (int y = 0; y < s; y++) for (int z = 0; z < s; z++) {
            if (!g[x][y][z] || used[x][y][z]) continue;
            int z1 = z;
            while (z1 + 1 < s && g[x][y][z1 + 1] && !used[x][y][z1 + 1]) z1++;
            int x1 = x;
            growX:
            while (x1 + 1 < s) {
                for (int zz = z; zz <= z1; zz++) if (!g[x1 + 1][y][zz] || used[x1 + 1][y][zz]) break growX;
                x1++;
            }
            int y1 = y;
            growY:
            while (y1 + 1 < s) {
                for (int xx = x; xx <= x1; xx++) for (int zz = z; zz <= z1; zz++)
                    if (!g[xx][y1 + 1][zz] || used[xx][y1 + 1][zz]) break growY;
                y1++;
            }
            for (int xx = x; xx <= x1; xx++) for (int yy = y; yy <= y1; yy++) for (int zz = z; zz <= z1; zz++) used[xx][yy][zz] = true;
            count++;
        }
        return count;
    }

    static List<Double> breaks(double from, double to) {
        var out = new ArrayList<Double>();
        out.add(from);
        for (double k = Math.floor(from) + 1; k < to - EPS; k++) out.add(k);
        out.add(to);
        return out;
    }

    static void addCrossings(TreeSet<Double> ts, double a, double b) {
        double lo = Math.min(a, b), hi = Math.max(a, b);
        for (double k = Math.floor(lo) + 1; k < hi - EPS; k++) {
            double t = (k - a) / (b - a);
            if (t > EPS && t < 1 - EPS) ts.add(t);
        }
    }

    static long key(int x, int y, int z) { return ((long) x << 40) | ((long) y << 20) | z; }

    static List<double[]> clipToCell(List<double[]> poly, int x, int y, int z) {
        var out = poly;
        int[] cell = {x, y, z};
        for (int axis = 0; axis < 3; axis++) {
            out = clip(out, axis, cell[axis], true);
            out = clip(out, axis, cell[axis] + 1, false);
        }
        return out;
    }

    /** Sutherland-Hodgman against one axis plane; keep the side >= bound (lower) or <= bound (upper). */
    static List<double[]> clip(List<double[]> poly, int axis, double bound, boolean lower) {
        var out = new ArrayList<double[]>();
        for (int i = 0; i < poly.size(); i++) {
            double[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
            boolean pin = lower ? p[axis] >= bound - EPS : p[axis] <= bound + EPS;
            boolean qin = lower ? q[axis] >= bound - EPS : q[axis] <= bound + EPS;
            if (pin) out.add(p);
            if (pin != qin) {
                double t = (bound - p[axis]) / (q[axis] - p[axis]);
                out.add(new double[]{p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1]), p[2] + t * (q[2] - p[2])});
            }
        }
        return out;
    }

    static double area(List<double[]> poly) {
        double sx = 0, sy = 0, sz = 0;
        for (int i = 0; i < poly.size(); i++) {
            double[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
            sx += p[1] * q[2] - p[2] * q[1];
            sy += p[2] * q[0] - p[0] * q[2];
            sz += p[0] * q[1] - p[1] * q[0];
        }
        return .5 * Math.sqrt(sx * sx + sy * sy + sz * sz);
    }

    static final class Vessel {
        final double r, hd, cx, cz, yBottom, yTop, height;
        final int n, bands;
        final double[] cos, sin;

        Vessel(double diameter, double tangentLength) {
            r = diameter / 2;
            hd = diameter / 4;                                            // 2:1 ellipsoidal head depth
            n = Math.max(8, 4 * (int) Math.ceil(Math.PI * diameter / 2)); // facet chord <= about 0.5 m
            bands = Math.max(2, n / 8);
            cx = cz = Math.ceil(diameter) / 2;
            yBottom = hd;
            yTop = hd + tangentLength;
            height = yTop + hd;
            cos = new double[n];
            sin = new double[n];
            for (int i = 0; i < n; i++) {
                cos[i] = Math.cos((i + .5) * 2 * Math.PI / n);
                sin[i] = Math.sin((i + .5) * 2 * Math.PI / n);
            }
        }

        double theta(int i) { return i * 2 * Math.PI / n; }

        /** Ring k of a head (k = 0 at the tangent line, k = bands at the apex): {height offset, radius}. */
        double[] ring(int k) {
            double phi = Math.PI / 2 * k / bands;
            return new double[]{hd * Math.sin(phi), r * Math.cos(phi)};
        }

        double circumradiusAt(double y) {
            if (y < 0 || y > height) return 0;
            if (y >= yBottom && y <= yTop) return r;
            double off = y < yBottom ? yBottom - y : y - yTop;
            for (int k = 0; k < bands; k++) {
                double[] a = ring(k), b = ring(k + 1);
                if (off <= b[0] + EPS) return a[1] + (b[1] - a[1]) * (off - a[0]) / (b[0] - a[0]);
            }
            return 0;
        }

        boolean inside(double x, double y, double z) {
            double rc = circumradiusAt(y);
            if (rc <= 0) return false;
            double apothem = rc * Math.cos(Math.PI / n), px = x - cx, pz = z - cz;
            for (int i = 0; i < n; i++) if (px * cos[i] + pz * sin[i] > apothem) return false;
            return true;
        }

        List<List<double[]>> headPolygons() {
            var out = new ArrayList<List<double[]>>();
            for (int side = 0; side < 2; side++) {
                double base = side == 0 ? yBottom : yTop, sign = side == 0 ? -1 : 1;
                for (int k = 0; k < bands; k++) {
                    double[] a = ring(k), b = ring(k + 1);
                    for (int i = 0; i < n; i++) {
                        var poly = new ArrayList<double[]>();
                        poly.add(point(a[1], i, base + sign * a[0]));
                        poly.add(point(a[1], i + 1, base + sign * a[0]));
                        poly.add(point(b[1], i + 1, base + sign * b[0]));
                        if (b[1] > EPS) poly.add(point(b[1], i, base + sign * b[0]));
                        out.add(poly);
                    }
                }
            }
            return out;
        }

        double[] point(double radius, int i, double y) {
            return new double[]{cx + radius * Math.cos(theta(i)), y, cz + radius * Math.sin(theta(i))};
        }
    }
}
