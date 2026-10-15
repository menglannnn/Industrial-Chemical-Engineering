package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns an {@link EquipmentShape} into what the world needs, one block cell at a time: which cells hold the shell and
 * which the interior ({@link #layout}), and for a shell cell its surface polygons ({@link #surface}) and its collision
 * solid ({@link #solid}). Every polygon is clipped to its own cell, so a chunk renderer can bake each cell like any
 * block and a vessel needs no lookup across cells or chunks (batch 2026-10-15-custom-equipment-rendering, route A1).
 *
 * <p>The vessel is built in its own frame, axis along +y: a profile of revolution (skirt or bottom head, cylinder, top
 * head), a skirt's base ring and anchor chairs, and, lying down, two saddles below it. A quarter turn then lays a
 * horizontal vessel's axis along world x or z; it is a rotation, so every polygon stays counter-clockwise seen from
 * outside.
 *
 * <p>Texture follows a fabricated vessel. The shell is rolled plate in {@link #COURSE} courses from the bottom tangent
 * line, each course closed by plates about {@link #PLATE_LENGTH} long fitted to the circumference; one shell sprite
 * holds two courses with staggered seams. Heads are pressed in one piece: their sprite runs once from the tangent line
 * weld to the apex. Supports carry a plain painted sprite repeating every {@link #SUPPORT_TILE}.
 *
 * <p>A cell wholly inside the cylindrical shell or skirt, clear of the skirt base and the saddles, has the same geometry
 * at every position along the axis and a texture that repeats every two courses; its {@link CellKey} carries
 * {@link CellKey#BODY} minus its texture class instead of that coordinate, so callers that cache by key build it once
 * per class (at most eight).
 */
public final class ShapeRasterizer {
    /** Collision solid resolution, in parts of a block: 1/8 gives 4 to 11 boxes per shell cell on average (report section 4). */
    public static final int SOLID_RESOLUTION = 8;
    /** Texture layout, m: a shell course, the nominal shell plate length, a support sprite's repeat. */
    public static final double COURSE = 2, PLATE_LENGTH = 4, SUPPORT_TILE = 2;
    private static final double EPS = 1e-9, MIN_AREA = 1e-7, OWNER_NUDGE = 1e-6;
    private static final int GEOMETRY_CACHE = 64;
    private static final Map<EquipmentShape, Geometry> GEOMETRIES = new ConcurrentHashMap<>();

    /** Which sprite a polygon samples: shell plate, pressed head, painted support steel. */
    public enum Material {
        SHELL, HEAD, SUPPORT
    }

    public record Cell(int x, int y, int z) {
    }

    /** Shell and interior cells of a vessel in x-major, then y, then z order; every other cell of its box is outside. */
    public record Layout(EquipmentShape shape, List<Cell> shell, List<Cell> interior) {
        public Layout {
            shell = List.copyOf(shell);
            interior = List.copyOf(interior);
        }

        /**
         * The vessel's controller: its shell cell in the lowest row, then westmost, then northmost (owner decision
         * 2026-10-15: the controller is a role of one shell cell, not a block of its own).
         */
        public Cell controller() {
            Cell best = shell.getFirst();
            for (var cell : shell)
                if (cell.y() < best.y() || cell.y() == best.y() && (cell.x() < best.x() || cell.x() == best.x() && cell.z() < best.z())) best = cell;
            return best;
        }
    }

    /**
     * Geometry key of one shell cell. Its coordinate along the vessel's axis is {@link #BODY} minus a texture class (0 to
     * 7) where the cell lies wholly in the cylindrical shell or skirt; all such cells of one class are identical.
     */
    public record CellKey(EquipmentShape shape, int x, int y, int z) {
        public static final int BODY = -1;
    }

    /**
     * Convex planar polygon in cell-local coordinates [0, 1], counter-clockwise seen from outside the vessel; texture
     * coordinates in [0, 1] of its material's sprite; one unit normal per vertex from the smooth vessel surface, for
     * lighting.
     */
    public record Polygon(double[] positions, double[] uvs, double[] normals, Material material) {
        public int size() {
            return positions.length / 3;
        }
    }

    /** Solid box in units of 1/{@link #SOLID_RESOLUTION} block, lower corner inclusive, upper exclusive. */
    public record Box(int x0, int y0, int z0, int x1, int y1, int z1) {
    }

    private ShapeRasterizer() {
    }

    public static Layout layout(EquipmentShape shape) {
        var g = geometry(shape);
        int sx = shape.sizeX(), sy = shape.sizeY(), sz = shape.sizeZ();
        var shell = new boolean[sx][sy][sz];
        // Bounds widened by EPS: a facet lying in a cell plane is offered to the cells on both sides of it.
        for (var facet : g.facets)
            for (int x = Math.max(0, floor(facet.min[0] - EPS)); x < Math.min(sx, ceil(facet.max[0] + EPS)); x++)
                for (int y = Math.max(0, floor(facet.min[1] - EPS)); y < Math.min(sy, ceil(facet.max[1] + EPS)); y++)
                    for (int z = Math.max(0, floor(facet.min[2] - EPS)); z < Math.min(sz, ceil(facet.max[2] + EPS)); z++)
                        if (!shell[x][y][z] && piece(facet, x, y, z) != null) shell[x][y][z] = true;
        var shellCells = new ArrayList<Cell>();
        var interiorCells = new ArrayList<Cell>();
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++)
                    if (shell[x][y][z]) shellCells.add(new Cell(x, y, z));
                    else if (g.inside(x + .5, y + .5, z + .5)) interiorCells.add(new Cell(x, y, z));
        return new Layout(shape, shellCells, interiorCells);
    }

    public static CellKey key(EquipmentShape shape, int x, int y, int z) {
        var g = geometry(shape);
        int[] cell = {x, y, z};
        int bodyClass = g.bodyClass(cell[g.axial]);
        if (bodyClass >= 0) cell[g.axial] = CellKey.BODY - bodyClass;
        return new CellKey(shape, cell[0], cell[1], cell[2]);
    }

    public static List<Polygon> surface(CellKey key) {
        int[] c = cell(key);
        return surface(key.shape(), c[0], c[1], c[2]);
    }

    /** Greedy merge of the cell's solid sampled at {@link #SOLID_RESOLUTION}: along z, then x, then y. */
    public static List<Box> solid(CellKey key) {
        var g = geometry(key.shape());
        int s = SOLID_RESOLUTION;
        int[] c = cell(key);
        var filled = new boolean[s][s][s];
        for (int a = 0; a < s; a++)
            for (int b = 0; b < s; b++)
                for (int d = 0; d < s; d++)
                    filled[a][b][d] = g.inside(c[0] + (a + .5) / s, c[1] + (b + .5) / s, c[2] + (d + .5) / s);
        var used = new boolean[s][s][s];
        var boxes = new ArrayList<Box>();
        for (int x = 0; x < s; x++)
            for (int y = 0; y < s; y++)
                for (int z = 0; z < s; z++) {
                    if (!filled[x][y][z] || used[x][y][z]) continue;
                    int z1 = z + 1;
                    while (z1 < s && filled[x][y][z1] && !used[x][y][z1]) z1++;
                    int x1 = x + 1;
                    while (x1 < s && run(filled, used, x1, x1 + 1, y, y + 1, z, z1)) x1++;
                    int y1 = y + 1;
                    while (y1 < s && run(filled, used, x, x1, y1, y1 + 1, z, z1)) y1++;
                    for (int a = x; a < x1; a++) for (int b = y; b < y1; b++) for (int d = z; d < z1; d++) used[a][b][d] = true;
                    boxes.add(new Box(x, y, z, x1, y1, z1));
                }
        return List.copyOf(boxes);
    }

    /** Whether a box-local point lies inside the faceted solid (vessel and supports). */
    public static boolean inside(EquipmentShape shape, double x, double y, double z) {
        return geometry(shape).inside(x, y, z);
    }

    /**
     * Silhouette for the selection outline, as box-local segments {x0, y0, z0, x1, y1, z1}: the shell polygon at the
     * tangent lines (and the skirt's base ring), four meridians from end to end, and the main edges of each saddle.
     */
    public static List<double[]> outline(EquipmentShape shape) {
        return geometry(shape).outline();
    }

    /** Surface polygons of the cell at its actual position, without key normalization. */
    static List<Polygon> surface(EquipmentShape shape, int x, int y, int z) {
        var g = geometry(shape);
        var out = new ArrayList<Polygon>();
        for (var facet : g.facets) {
            if (facet.max[0] < x - EPS || facet.min[0] > x + 1 + EPS || facet.max[1] < y - EPS || facet.min[1] > y + 1 + EPS
                    || facet.max[2] < z - EPS || facet.min[2] > z + 1 + EPS) continue;
            var clipped = piece(facet, x, y, z);
            if (clipped == null) continue;
            // Split where the texture wraps, so each piece maps into one sprite.
            for (var across : tiles(clipped, facet.u))
                for (var along : tiles(across.polygon, facet.v))
                    out.add(g.polygon(along.polygon, facet, x, y, z, across.index, along.index));
        }
        return List.copyOf(out);
    }

    /** Total area of the vessel's facets, m²: what the cells' surface pieces must add up to. */
    static double facetArea(EquipmentShape shape) {
        double total = 0;
        for (var facet : geometry(shape).facets) total += area(facet.positions);
        return total;
    }

    /** The cell a key stands for: a body key stands for the first body cell of its class along the axis. */
    private static int[] cell(CellKey key) {
        var g = geometry(key.shape());
        int[] c = {key.x(), key.y(), key.z()};
        if (c[g.axial] <= CellKey.BODY) c[g.axial] = g.representatives[CellKey.BODY - c[g.axial]];
        return c;
    }

    private static boolean run(boolean[][][] filled, boolean[][][] used, int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int a = x0; a < x1; a++)
            for (int b = y0; b < y1; b++)
                for (int c = z0; c < z1; c++)
                    if (!filled[a][b][c] || used[a][b][c]) return false;
        return true;
    }

    private static Geometry geometry(EquipmentShape shape) {
        var g = GEOMETRIES.get(shape);
        if (g != null) return g;
        if (GEOMETRIES.size() >= GEOMETRY_CACHE) GEOMETRIES.clear();
        return GEOMETRIES.computeIfAbsent(shape, Geometry::new);
    }

    /**
     * The facet clipped to the cell, or null when it covers no area there. A facet lying in a cell face belongs to the
     * cell on its inner side, so each piece of surface is drawn by exactly one cell.
     */
    private static List<double[]> piece(Facet facet, int x, int y, int z) {
        var p = facet.positions;
        p = clip(p, new double[]{1, 0, 0}, -x);
        p = clip(p, new double[]{-1, 0, 0}, x + 1);
        p = clip(p, new double[]{0, 1, 0}, -y);
        p = clip(p, new double[]{0, -1, 0}, y + 1);
        p = clip(p, new double[]{0, 0, 1}, -z);
        p = clip(p, new double[]{0, 0, -1}, z + 1);
        if (p.size() < 3 || area(p) < MIN_AREA) return null;
        double cx = 0, cy = 0, cz = 0;
        for (var v : p) {
            cx += v[0];
            cy += v[1];
            cz += v[2];
        }
        cx = cx / p.size() - facet.normal[0] * OWNER_NUDGE;
        cy = cy / p.size() - facet.normal[1] * OWNER_NUDGE;
        cz = cz / p.size() - facet.normal[2] * OWNER_NUDGE;
        return cx >= x && cx < x + 1 && cy >= y && cy < y + 1 && cz >= z && cz < z + 1 ? p : null;
    }

    private record Tile(List<double[]> polygon, int index) {
    }

    /** The polygon cut where a texture coordinate crosses whole tiles. */
    private static List<Tile> tiles(List<double[]> polygon, Uv uv) {
        double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
        for (var p : polygon) {
            double t = uv.at(p);
            low = Math.min(low, t);
            high = Math.max(high, t);
        }
        int first = floor(low + EPS), last = ceil(high - EPS);
        if (last <= first + 1) return List.of(new Tile(polygon, first));
        var out = new ArrayList<Tile>(last - first);
        double[] c = {uv.axis[0] / uv.period, uv.axis[1] / uv.period, uv.axis[2] / uv.period};
        double base = (uv.offset - dot(uv.origin, uv.axis)) / uv.period; // uv.at(p) = dot(c, p) + base
        for (int k = first; k < last; k++) {
            var tile = clip(clip(polygon, c, base - k), neg(c), k + 1 - base);
            if (tile.size() >= 3 && area(tile) > 1e-12) out.add(new Tile(tile, k));
        }
        return out;
    }

    /** Sutherland-Hodgman: keeps the part of a convex polygon where dot(c, p) + d >= 0. */
    private static List<double[]> clip(List<double[]> polygon, double[] c, double d) {
        var out = new ArrayList<double[]>(polygon.size() + 2);
        for (int i = 0; i < polygon.size(); i++) {
            double[] p = polygon.get(i), q = polygon.get((i + 1) % polygon.size());
            double fp = dot(c, p) + d, fq = dot(c, q) + d;
            boolean pIn = fp >= -EPS, qIn = fq >= -EPS;
            if (pIn) out.add(p);
            if (pIn != qIn) {
                double t = fp / (fp - fq);
                out.add(new double[]{p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1]), p[2] + t * (q[2] - p[2])});
            }
        }
        return out;
    }

    private static double[] newell(List<double[]> polygon) {
        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < polygon.size(); i++) {
            double[] p = polygon.get(i), q = polygon.get((i + 1) % polygon.size());
            nx += (p[1] - q[1]) * (p[2] + q[2]);
            ny += (p[2] - q[2]) * (p[0] + q[0]);
            nz += (p[0] - q[0]) * (p[1] + q[1]);
        }
        return new double[]{nx, ny, nz};
    }

    private static double area(List<double[]> polygon) {
        var n = newell(polygon);
        return .5 * Math.sqrt(dot(n, n));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] neg(double[] a) {
        return new double[]{-a[0], -a[1], -a[2]};
    }

    private static double[] unit(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        return new double[]{x / length, y / length, z / length};
    }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1]), a[2] + t * (b[2] - a[2])};
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }

    private static int ceil(double v) {
        return (int) Math.ceil(v);
    }

    /** What lights a facet: the radial normal of a surface of revolution, a head's ellipsoid normal, or its own plane. */
    private enum Kind {
        ROUND, HEAD, FLAT
    }

    /** A texture coordinate, in sprite tiles: (offset + dot(p - origin, axis)) / period, with p and origin in the box. */
    private record Uv(double[] origin, double[] axis, double offset, double period) {
        double at(double[] p) {
            return (offset + (p[0] - origin[0]) * axis[0] + (p[1] - origin[1]) * axis[1] + (p[2] - origin[2]) * axis[2]) / period;
        }
    }

    /** A planar convex facet in box-local coordinates, counter-clockwise seen from outside, with its texture frame. */
    private record Facet(List<double[]> positions, double[] normal, double[] min, double[] max, Kind kind, Material material, Uv u, Uv v) {
    }

    /** Per-shape tables (facet directions, head rings, plate and saddle layout), the frame turn and the facets themselves. */
    private static final class Geometry {
        private final EquipmentShape shape;
        private final int n, contact;
        /** Index (0 x, 1 y, 2 z) of the box coordinate that runs along the vessel's axis. */
        private final int axial;
        /** Body texture class of each row of cells along the axis (-1: not a body row), and the first row of each class. */
        private final int[] bodyClasses, representatives;
        private final double axis, radius, apothemFactor, ground, plateLength;
        /** Outer radii (vertex) of a saddle's top plate and a skirt's base ring; a saddle's half width across the axis. */
        private final double saddleRadius, ringRadius, saddleLateral;
        private final double[] normalCos, normalSin, vertexCos, vertexSin, saddles, meridian;
        private final double[][] rings;
        private final List<Facet> facets = new ArrayList<>();

        Geometry(EquipmentShape shape) {
            this.shape = shape;
            n = shape.facets();
            axis = shape.axis();
            radius = shape.radius();
            axial = switch (shape.orientation()) {
                case VERTICAL -> 1;
                case ALONG_X -> 0;
                case ALONG_Z -> 2;
            };
            // A lying vessel's frame has the ground below it at x = -support (world y = 0).
            ground = -shape.support();
            apothemFactor = DeterministicMath.cos(Math.PI / n);
            normalCos = new double[n];
            normalSin = new double[n];
            vertexCos = new double[n + 1];
            vertexSin = new double[n + 1];
            for (int i = 0; i < n; i++) {
                double mid = (i + .5) * 2 * Math.PI / n;
                normalCos[i] = DeterministicMath.cos(mid);
                normalSin[i] = DeterministicMath.sin(mid);
            }
            for (int i = 0; i <= n; i++) {
                double angle = 2 * Math.PI * (i % n) / n;
                vertexCos[i] = DeterministicMath.cos(angle);
                vertexSin[i] = DeterministicMath.sin(angle);
            }
            int bands = shape.bands();
            rings = new double[bands + 1][];
            for (int k = 0; k <= bands; k++) {
                double phi = Math.PI / 2 * k / bands;
                rings[k] = new double[]{shape.headDepth() * DeterministicMath.sin(phi), k == bands ? 0 : radius * DeterministicMath.cos(phi)};
            }
            // Meridian distance from the tangent line to each head ring, measured between facet centre lines.
            meridian = new double[bands + 1];
            for (int k = 0; k < bands; k++) {
                double dh = rings[k + 1][0] - rings[k][0], dr = (rings[k][1] - rings[k + 1][1]) * apothemFactor;
                meridian[k + 1] = meridian[k] + Math.sqrt(dh * dh + dr * dr);
            }
            double chord = 2 * radius * DeterministicMath.sin(Math.PI / n), circumference = n * chord;
            plateLength = circumference / Math.max(1, Math.round(circumference / PLATE_LENGTH));
            // Saddles cradle the shell vertices within half the contact angle of the bottom vertex (n/2, angle π, towards the ground).
            contact = (int) Math.round(n * EquipmentShape.SADDLE_CONTACT_DEGREES / 720);
            saddleRadius = radius + EquipmentShape.PLATE / apothemFactor;
            saddleLateral = saddleRadius * vertexSin[contact];
            ringRadius = radius + EquipmentShape.BASE_RING / apothemFactor;
            saddles = shape.saddleCentres();

            double yB = shape.bottomTangent(), yT = shape.topTangent(), plate = EquipmentShape.PLATE;
            var up = new double[]{0, 1, 0};
            var courses = new Uv(new double[]{axis, yB, axis}, up, 0, 2 * COURSE);
            var skirtTiles = new Uv(new double[]{axis, yB, axis}, up, 0, SUPPORT_TILE);
            var centre = new double[]{axis, (yB + yT) / 2, axis};
            for (int i = 0; i < n; i++) {
                double[] a = ring(i, 0), b = ring(i + 1, 0);
                double[] tangent = unit(b[0] - a[0], 0, b[2] - a[2]);
                // Arc length runs counter-clockwise seen from the top head, leftwards seen from outside: u runs against it.
                var plates = new Uv(a, neg(tangent), -i * chord, plateLength);
                add(List.of(ring(i, yB), ring(i + 1, yB), ring(i + 1, yT), ring(i, yT)), centre, Kind.ROUND, Material.SHELL, plates, courses);
                if (shape.skirt())
                    add(List.of(ring(i, plate), ring(i + 1, plate), ring(i + 1, yB), ring(i, yB)), centre, Kind.ROUND, Material.SUPPORT,
                            new Uv(a, neg(tangent), -i * chord, plateLength / 2), skirtTiles);
                for (int side = 0; side < 2; side++) {
                    if (side == 0 && shape.skirt()) continue; // the skirt hides the bottom head
                    double base = side == 0 ? yB : yT, sign = side == 0 ? -1 : 1;
                    var apex = new double[]{axis, base, axis};
                    for (int k = 0; k < bands; k++) {
                        var polygon = new ArrayList<double[]>(4);
                        polygon.add(head(i, k, base, sign));
                        polygon.add(head(i + 1, k, base, sign));
                        polygon.add(head(i + 1, k + 1, base, sign));
                        if (rings[k + 1][1] > 0) polygon.add(head(i, k + 1, base, sign));
                        // v runs from the tangent line weld (sprite top) to the apex (bottom), once over the head.
                        double[] outer = lerp(head(i, k, base, sign), head(i + 1, k, base, sign), .5);
                        double[] inner = lerp(head(i, k + 1, base, sign), head(i + 1, k + 1, base, sign), .5);
                        var towardsTangent = unit(outer[0] - inner[0], outer[1] - inner[1], outer[2] - inner[2]);
                        add(polygon, apex, Kind.HEAD, Material.HEAD, plates, new Uv(outer, towardsTangent, -meridian[k], meridian[bands]));
                    }
                }
            }
            if (shape.skirt()) skirtBase();
            for (double c : saddles) saddle(c);

            int cells = shape.axialCells();
            bodyClasses = new int[cells];
            var classes = new HashMap<Integer, Integer>();
            var firstRows = new ArrayList<Integer>();
            for (int a = 0; a < cells; a++) {
                bodyClasses[a] = -1;
                if (!body(a)) continue;
                // Rows of one class share geometry and the texture phase: region (skirt or shell) and course pair.
                int signature = (a < yB ? 0 : 4) + Math.floorMod(a - floor(yB), 4);
                Integer known = classes.get(signature);
                if (known == null) {
                    known = firstRows.size();
                    classes.put(signature, known);
                    firstRows.add(a);
                }
                bodyClasses[a] = known;
            }
            representatives = firstRows.stream().mapToInt(Integer::intValue).toArray();
        }

        /** A skirt's foot: the plate under it and its base ring, the ring's top and rim, and the anchor chairs. */
        private void skirtBase() {
            double plate = EquipmentShape.PLATE;
            var centre = new double[]{axis, plate / 2, axis};
            var bottom = new ArrayList<double[]>(n);
            for (int i = 0; i < n; i++) bottom.add(rim(i, 0));
            add(bottom, centre, Kind.FLAT, Material.SUPPORT, null, null);
            for (int i = 0; i < n; i++) {
                add(List.of(rim(i, plate), rim(i + 1, plate), ring(i + 1, plate), ring(i, plate)), centre, Kind.FLAT, Material.SUPPORT, null, null);
                add(List.of(rim(i, 0), rim(i + 1, 0), rim(i + 1, plate), rim(i, plate)), centre, Kind.ROUND, Material.SUPPORT, null, null);
            }
            int chairs = shape.anchorChairs();
            double apothem = radius * apothemFactor, depth = EquipmentShape.BASE_RING, half = EquipmentShape.CHAIR / 2;
            double low = plate, high = plate + EquipmentShape.CHAIR;
            for (int k = 0; k < chairs; k++) {
                int i = k * n / chairs;
                // A box on the ring against shell facet i: corners at radial r and tangential t from the axis.
                double[] in0 = chair(i, apothem, -half), in1 = chair(i, apothem, half);
                double[] out0 = chair(i, apothem + depth, -half), out1 = chair(i, apothem + depth, half);
                var middle = chair(i, apothem + depth / 2, 0);
                middle[1] = (low + high) / 2;
                add(List.of(at(out0, low), at(out1, low), at(out1, high), at(out0, high)), middle, Kind.FLAT, Material.SUPPORT, null, null);
                add(List.of(at(in0, high), at(out0, high), at(out1, high), at(in1, high)), middle, Kind.FLAT, Material.SUPPORT, null, null);
                add(List.of(at(in0, low), at(out0, low), at(out0, high), at(in0, high)), middle, Kind.FLAT, Material.SUPPORT, null, null);
                add(List.of(at(in1, low), at(out1, low), at(out1, high), at(in1, high)), middle, Kind.FLAT, Material.SUPPORT, null, null);
            }
        }

        /** Cross-section point at radial distance {@code r} along facet {@code i}'s normal and {@code t} along the facet. */
        private double[] chair(int i, double r, double t) {
            return new double[]{axis + r * normalCos[i] - t * normalSin[i], 0, axis + r * normalSin[i] + t * normalCos[i]};
        }

        /**
         * One saddle at an axial centre, in the vessel frame. A top plate wraps the shell over the contact arc; a web plate
         * under it and a rib at each horn stand on a base plate. Faces that touch another part are not drawn.
         */
        private void saddle(double c) {
            double plate = EquipmentShape.PLATE, rib = EquipmentShape.SADDLE_RIB, deck = ground + plate;
            double y0 = c - EquipmentShape.SADDLE_WIDTH / 2, y1 = c + EquipmentShape.SADDLE_WIDTH / 2;
            double w0 = c - EquipmentShape.SADDLE_WEB / 2, w1 = c + EquipmentShape.SADDLE_WEB / 2;
            int first = n / 2 - contact, last = n / 2 + contact;
            var axisPoint = new double[]{axis, c, axis};
            var lowest = new double[]{axis - radius, c, axis};
            // Top plate: outside faces (beside the web), axial ends and horn ends.
            for (int j = first; j < last; j++) {
                for (double[] span : new double[][]{{y0, w0}, {w1, y1}})
                    add(List.of(outer(j, span[0]), outer(j + 1, span[0]), outer(j + 1, span[1]), outer(j, span[1])), axisPoint, Kind.ROUND, Material.SUPPORT, null, null);
                for (double y : new double[]{y0, y1})
                    add(List.of(ring(j, y), ring(j + 1, y), outer(j + 1, y), outer(j, y)), axisPoint, Kind.FLAT, Material.SUPPORT, null, null);
            }
            for (int j : new int[]{first, last})
                add(List.of(ring(j, y0), ring(j, y1), outer(j, y1), outer(j, y0)), lowest, Kind.FLAT, Material.SUPPORT, null, null);
            // Web: strips under the top plate between the ribs, on both faces.
            double webHalf = saddleLateral - rib;
            var webCentre = new double[]{deck, c, axis};
            for (double y : new double[]{w0, w1}) strips(chain(-webHalf, webHalf), y, deck, webCentre);
            // Ribs: from the deck up to the top plate, outside the web.
            for (int side = 0; side < 2; side++) {
                var edge = side == 0 ? chain(webHalf, saddleLateral) : chain(-saddleLateral, -webHalf);
                double[] horn = side == 0 ? edge.getFirst() : edge.getLast(), inner = side == 0 ? edge.getLast() : edge.getFirst();
                double sign = side == 0 ? 1 : -1;
                var ribCentre = new double[]{(deck + horn[0]) / 2, c, axis + sign * (saddleLateral - rib / 2)};
                add(List.of(new double[]{deck, y0, horn[2]}, new double[]{deck, y1, horn[2]}, at(horn, y1), at(horn, y0)), ribCentre, Kind.FLAT, Material.SUPPORT, null, null);
                for (double y : new double[]{y0, y1}) strips(edge, y, deck, ribCentre);
                for (double[] span : new double[][]{{y0, w0}, {w1, y1}})
                    add(List.of(new double[]{deck, span[0], inner[2]}, new double[]{deck, span[1], inner[2]}, at(inner, span[1]), at(inner, span[0])),
                            ribCentre, Kind.FLAT, Material.SUPPORT, null, null);
            }
            // Base plate: underside, four sides, and its top beside the web (the web and ribs cover the rest).
            double lateral = saddleLateral;
            var baseCentre = new double[]{ground + plate / 2, c, axis};
            double[][] corners = {{-lateral, y0}, {lateral, y0}, {lateral, y1}, {-lateral, y1}};
            var under = new ArrayList<double[]>(4);
            for (var corner : corners) under.add(new double[]{ground, corner[1], axis + corner[0]});
            add(under, baseCentre, Kind.FLAT, Material.SUPPORT, null, null);
            for (int e = 0; e < 4; e++) {
                double[] a = corners[e], b = corners[(e + 1) % 4];
                add(List.of(new double[]{ground, a[1], axis + a[0]}, new double[]{ground, b[1], axis + b[0]}, new double[]{deck, b[1], axis + b[0]},
                        new double[]{deck, a[1], axis + a[0]}), baseCentre, Kind.FLAT, Material.SUPPORT, null, null);
            }
            for (double[] span : new double[][]{{y0, w0}, {w1, y1}})
                add(List.of(new double[]{deck, span[0], axis - webHalf}, new double[]{deck, span[0], axis + webHalf}, new double[]{deck, span[1], axis + webHalf},
                        new double[]{deck, span[1], axis - webHalf}), baseCentre, Kind.FLAT, Material.SUPPORT, null, null);
        }

        /**
         * The top plate's outer edge over lateral offsets [low, high] from the axis, as cross-section points (x, 0, z) in
         * order of decreasing z: its vertices within the range and the points where the range cuts it.
         */
        private List<double[]> chain(double low, double high) {
            var points = new ArrayList<double[]>();
            for (int j = n / 2 - contact; j < n / 2 + contact; j++) {
                double[] a = outer(j, 0), b = outer(j + 1, 0);
                double za = a[2] - axis, zb = b[2] - axis, top = Math.min(za, high), bottom = Math.max(zb, low);
                if (top <= bottom + EPS) continue;
                if (points.isEmpty()) points.add(lerp(a, b, (za - top) / (za - zb)));
                points.add(lerp(a, b, (za - bottom) / (za - zb)));
            }
            return points;
        }

        /** The faces at axial position {@code y} of the part between the deck and an edge: one strip under each edge segment. */
        private void strips(List<double[]> edge, double y, double deck, double[] centre) {
            for (int p = 0; p + 1 < edge.size(); p++) {
                double[] a = at(edge.get(p), y), b = at(edge.get(p + 1), y);
                add(List.of(new double[]{deck, y, a[2]}, new double[]{deck, y, b[2]}, b, a), centre, Kind.FLAT, Material.SUPPORT, null, null);
            }
        }

        private static double[] at(double[] p, double y) {
            return new double[]{p[0], y, p[2]};
        }

        /** Shell vertex {@code i} at axial position {@code y}, in the vessel frame. */
        private double[] ring(int i, double y) {
            return new double[]{axis + radius * vertexCos[i], y, axis + radius * vertexSin[i]};
        }

        /** Vertex {@code i} of a saddle's top plate, outside the shell. */
        private double[] outer(int i, double y) {
            return new double[]{axis + saddleRadius * vertexCos[i], y, axis + saddleRadius * vertexSin[i]};
        }

        /** Vertex {@code i} of a skirt's base ring rim. */
        private double[] rim(int i, double y) {
            return new double[]{axis + ringRadius * vertexCos[i], y, axis + ringRadius * vertexSin[i]};
        }

        /** Point of head ring {@code k} at shell vertex {@code i}, on the head that leaves its tangent line {@code base} in direction {@code sign}. */
        private double[] head(int i, int k, double base, double sign) {
            return new double[]{axis + rings[k][1] * vertexCos[i], base + sign * rings[k][0], axis + rings[k][1] * vertexSin[i]};
        }

        /** Vessel frame to the box: the identity, or a quarter turn laying the axis along x or z (a rotation: windings keep their sense). */
        private double[] world(double[] p) {
            return switch (shape.orientation()) {
                case VERTICAL -> p.clone();
                case ALONG_X -> new double[]{p[1], p[0] + shape.support(), shape.width() - p[2]};
                case ALONG_Z -> new double[]{p[2], p[0] + shape.support(), p[1]};
            };
        }

        private double[] worldDirection(double[] d) {
            return switch (shape.orientation()) {
                case VERTICAL -> d.clone();
                case ALONG_X -> new double[]{d[1], d[0], -d[2]};
                case ALONG_Z -> new double[]{d[2], d[0], d[1]};
            };
        }

        private double[] vessel(double x, double y, double z) {
            return switch (shape.orientation()) {
                case VERTICAL -> new double[]{x, y, z};
                case ALONG_X -> new double[]{y - shape.support(), x, shape.width() - z};
                case ALONG_Z -> new double[]{y - shape.support(), z, x};
            };
        }

        /**
         * Orients a vessel-frame polygon counter-clockwise seen from outside (away from {@code centre}), turns it and its
         * texture frame into the box and records its bounds. Without a frame the texture is projected along the facet's
         * main box axis.
         */
        private void add(List<double[]> polygon, double[] centre, Kind kind, Material material, Uv u, Uv v) {
            var normal = newell(polygon);
            double[] centroid = new double[3];
            for (var p : polygon)
                for (int a = 0; a < 3; a++) centroid[a] += p[a] / polygon.size();
            var oriented = new ArrayList<>(polygon);
            if (dot(normal, new double[]{centroid[0] - centre[0], centroid[1] - centre[1], centroid[2] - centre[2]}) < 0) {
                Collections.reverse(oriented);
                normal = neg(normal);
            }
            var positions = new ArrayList<double[]>(oriented.size());
            double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            for (var p : oriented) {
                var w = world(p);
                positions.add(w);
                for (int a = 0; a < 3; a++) {
                    min[a] = Math.min(min[a], w[a]);
                    max[a] = Math.max(max[a], w[a]);
                }
            }
            var facing = worldDirection(unit(normal[0], normal[1], normal[2]));
            Uv worldU, worldV;
            if (u != null) {
                worldU = new Uv(world(u.origin), worldDirection(u.axis), u.offset, u.period);
                worldV = new Uv(world(v.origin), worldDirection(v.axis), v.offset, v.period);
            } else {
                // Box-axis projection, the sprite upright on side faces and unmirrored seen from outside.
                double ax = Math.abs(facing[0]), ay = Math.abs(facing[1]), az = Math.abs(facing[2]);
                double[] uAxis, vAxis;
                if (ay >= ax && ay >= az) {
                    uAxis = new double[]{1, 0, 0};
                    vAxis = new double[]{0, 0, facing[1] > 0 ? -1 : 1};
                } else if (ax >= az) {
                    uAxis = new double[]{0, 0, facing[0] > 0 ? -1 : 1};
                    vAxis = new double[]{0, 1, 0};
                } else {
                    uAxis = new double[]{facing[2] > 0 ? 1 : -1, 0, 0};
                    vAxis = new double[]{0, 1, 0};
                }
                var origin = new double[3];
                worldU = new Uv(origin, uAxis, 0, SUPPORT_TILE);
                worldV = new Uv(origin, vAxis, 0, SUPPORT_TILE);
            }
            facets.add(new Facet(List.copyOf(positions), facing, min, max, kind, material, worldU, worldV));
        }

        /** Whether the cells [a, a + 1) along the axis lie wholly in the cylindrical shell or skirt, clear of the skirt base and the saddles. */
        boolean body(int a) {
            double low = shape.skirt() ? 1 : shape.bottomTangent(), half = EquipmentShape.SADDLE_WIDTH / 2;
            if (a < low - EPS || a + 1 > shape.topTangent() + EPS) return false;
            for (double centre : saddles) if (a < centre + half + EPS && a + 1 > centre - half - EPS) return false;
            return true;
        }

        int bodyClass(int a) {
            return a >= 0 && a < bodyClasses.length ? bodyClasses[a] : -1;
        }

        boolean inside(double x, double y, double z) {
            var p = vessel(x, y, z);
            return inBody(p) || shape.skirt() && inSkirtBase(p) || saddles.length > 0 && inSaddle(p);
        }

        private boolean inBody(double[] p) {
            double rc = circumradius(p[1]);
            return rc > 0 && inPolygon(p, rc);
        }

        /** Inside the regular N-gon of circumradius {@code r} around the axis, in cross-section. */
        private boolean inPolygon(double[] p, double r) {
            double apothem = r * apothemFactor, px = p[0] - axis, pz = p[2] - axis;
            for (int i = 0; i < n; i++) if (px * normalCos[i] + pz * normalSin[i] > apothem) return false;
            return true;
        }

        private boolean inSkirtBase(double[] p) {
            double plate = EquipmentShape.PLATE;
            if (p[1] < 0 || p[1] > plate + EquipmentShape.CHAIR) return false;
            if (p[1] <= plate) return inPolygon(p, ringRadius);
            int chairs = shape.anchorChairs();
            double apothem = radius * apothemFactor, dx = p[0] - axis, dz = p[2] - axis;
            for (int k = 0; k < chairs; k++) {
                int i = k * n / chairs;
                double r = dx * normalCos[i] + dz * normalSin[i], t = -dx * normalSin[i] + dz * normalCos[i];
                if (r >= apothem && r <= apothem + EquipmentShape.BASE_RING && Math.abs(t) <= EquipmentShape.CHAIR / 2) return true;
            }
            return false;
        }

        /**
         * Inside a saddle: the base plate; the top plate (within the outer N-gon and the contact sector); below the top
         * plate's outer edge, the web and the ribs.
         */
        private boolean inSaddle(double[] p) {
            double dx = p[0] - axis, dz = Math.abs(p[2] - axis), deck = ground + EquipmentShape.PLATE;
            for (double c : saddles) {
                double along = Math.abs(p[1] - c);
                if (along > EquipmentShape.SADDLE_WIDTH / 2 || p[0] < ground || dx > 0 || dz > saddleLateral) continue;
                if (p[0] <= deck) return true;
                if (inPolygon(p, saddleRadius)) {
                    // Within the contact sector: the angle from straight down is at most half the contact angle.
                    if (dz * vertexCos[contact] <= -dx * vertexSin[contact]) return true;
                    continue;
                }
                if (dz >= saddleLateral - EquipmentShape.SADDLE_RIB || along <= EquipmentShape.SADDLE_WEB / 2) return true;
            }
            return false;
        }

        private double circumradius(double y) {
            if (y < 0 || y > shape.top()) return 0;
            if (y >= (shape.skirt() ? 0 : shape.bottomTangent()) && y <= shape.topTangent()) return radius;
            double off = y < shape.bottomTangent() ? shape.bottomTangent() - y : y - shape.topTangent();
            for (int k = 0; k + 1 < rings.length; k++) {
                double[] a = rings[k], b = rings[k + 1];
                if (off <= b[0]) return a[1] + (b[1] - a[1]) * (off - a[0]) / (b[0] - a[0]);
            }
            return 0;
        }

        List<double[]> outline() {
            var segments = new ArrayList<double[]>();
            double yB = shape.bottomTangent(), yT = shape.topTangent(), plate = EquipmentShape.PLATE;
            for (double y : new double[]{yB, yT})
                for (int i = 0; i < n; i++) segment(segments, ring(i, y), ring(i + 1, y));
            if (shape.skirt())
                for (int i = 0; i < n; i++) segment(segments, rim(i, plate), rim(i + 1, plate));
            for (int quarter = 0; quarter < 4; quarter++) {
                int i = quarter * n / 4;
                var points = new ArrayList<double[]>();
                if (shape.skirt()) points.add(ring(i, plate));
                else for (int k = rings.length - 1; k >= 0; k--) points.add(head(i, k, yB, -1));
                for (int k = 0; k < rings.length; k++) points.add(head(i, k, yT, 1));
                for (int p = 0; p + 1 < points.size(); p++) segment(segments, points.get(p), points.get(p + 1));
            }
            double deck = ground + plate;
            for (double c : saddles) {
                double y0 = c - EquipmentShape.SADDLE_WIDTH / 2, y1 = c + EquipmentShape.SADDLE_WIDTH / 2;
                double[][] corners = {{-saddleLateral, y0}, {saddleLateral, y0}, {saddleLateral, y1}, {-saddleLateral, y1}};
                for (int e = 0; e < 4; e++)
                    segment(segments, new double[]{deck, corners[e][1], axis + corners[e][0]}, new double[]{deck, corners[(e + 1) % 4][1], axis + corners[(e + 1) % 4][0]});
                for (int j : new int[]{n / 2 - contact, n / 2 + contact}) {
                    segment(segments, outer(j, y0), outer(j, y1));
                    for (double y : new double[]{y0, y1}) segment(segments, new double[]{deck, y, outer(j, 0)[2]}, outer(j, y));
                }
            }
            return List.copyOf(segments);
        }

        private void segment(List<double[]> segments, double[] a, double[] b) {
            double[] p = world(a), q = world(b);
            segments.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
        }

        /** Cell-local polygon with texture coordinates in tile ({@code tileU}, {@code tileV}) of its sprite, and smooth normals. */
        Polygon polygon(List<double[]> clipped, Facet facet, int x, int y, int z, int tileU, int tileV) {
            int m = clipped.size();
            var positions = new double[m * 3];
            var uvs = new double[m * 2];
            var normals = new double[m * 3];
            int[] cell = {x, y, z};
            for (int i = 0; i < m; i++) {
                var p = clipped.get(i);
                for (int a = 0; a < 3; a++) positions[3 * i + a] = clamp(p[a] - cell[a]);
                // The sprite's top row is the far end of the tile along v.
                uvs[2 * i] = clamp(facet.u.at(p) - tileU);
                uvs[2 * i + 1] = 1 - clamp(facet.v.at(p) - tileV);
                System.arraycopy(smoothNormal(p, facet), 0, normals, 3 * i, 3);
            }
            return new Polygon(positions, uvs, normals, facet.material);
        }

        /** Normal of the smooth vessel at a box point of a facet: radial on a surface of revolution, the 2:1 ellipsoid's in a head, the plane's elsewhere. */
        private double[] smoothNormal(double[] p, Facet facet) {
            if (facet.kind == Kind.FLAT) return facet.normal.clone();
            var v = vessel(p[0], p[1], p[2]);
            double dx = v[0] - axis, dz = v[2] - axis, r2 = radius * radius, hd = shape.headDepth();
            double h = facet.kind == Kind.ROUND ? 0
                    : v[1] < shape.bottomTangent() ? v[1] - shape.bottomTangent() : v[1] > shape.topTangent() ? v[1] - shape.topTangent() : 0;
            double nx = dx / r2, ny = h / (hd * hd), nz = dz / r2;
            if (nx * nx + ny * ny + nz * nz < EPS * EPS) return facet.normal.clone();
            return worldDirection(unit(nx, ny, nz));
        }

        private static double clamp(double v) {
            return Math.max(0, Math.min(1, v));
        }
    }
}
