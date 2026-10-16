package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.wormzjl.createcheme.science.equipment.Mesh.*;

/**
 * Turns an {@link EquipmentShape} into what the world needs, one block cell at a time: which cells hold the shell and
 * which the interior ({@link #layout}), and for a shell cell its surface polygons ({@link #surface}) and its collision
 * solid ({@link #solid}). Every polygon is clipped to its own cell, so a chunk renderer can bake each cell like any
 * block and a vessel needs no lookup across cells or chunks (batch 2026-10-15-custom-equipment-rendering, route A1).
 *
 * <p>The vessel is built in its own frame ({@link EquipmentShape}), axis along +y: a profile of revolution (skirt or
 * bottom head, cylinder, top head), a skirt's base ring, anchor chairs and access opening (a drum's skirt hollow, with
 * slots, its inside drawn), and, lying down, two saddles below it. {@link EquipmentShape#transform()} then turns it into
 * the box; a rotation keeps every polygon counter-clockwise seen from outside.
 *
 * <p>Nozzles are built apart: their drawn size follows the pipe connected to them (owner decision 2026-10-16), which the
 * cell's {@link CellKey#nozzles()} carries as 1/64 m codes, seven bits per nozzle (0: unconnected, drawn at its default
 * size under a blind flange). The cells a nozzle may touch are those of its largest size, so the layout never depends
 * on what is connected.
 *
 * <p>Texture follows a fabricated vessel. The shell is rolled plate in {@link #COURSE} courses from the bottom tangent
 * line, each course closed by plates about {@link #PLATE_LENGTH} long fitted to the circumference; one shell sprite
 * holds two courses with staggered seams. Heads are pressed in one piece: their sprite runs once from the tangent line
 * weld to the apex. Supports carry a plain painted sprite repeating every {@link #SUPPORT_TILE}.
 *
 * <p>A cell wholly inside the cylindrical shell or skirt, clear of the skirt base, the opening, the slots, a hollow
 * skirt's bottom head, the saddles and every nozzle, has the same geometry at every position along the axis and a texture that repeats every two courses; its
 * {@link CellKey} carries {@link CellKey#BODY} minus its texture class instead of that coordinate, so callers that cache
 * by key build it once per class (at most eight).
 */
public final class ShapeRasterizer {
    /** Collision solid resolution, in parts of a block: 1/8 gives 4 to 11 boxes per shell cell on average (report section 4). */
    public static final int SOLID_RESOLUTION = 8;
    /** Texture layout, m: a shell course, the nominal shell plate length, a support sprite's repeat. */
    public static final double COURSE = 2, PLATE_LENGTH = 4, SUPPORT_TILE = 2;
    /** Bits per nozzle in a cell key, and the most nozzles a key carries. */
    private static final int CODE_BITS = 7, KEYED_NOZZLES = 9;
    private static final int GEOMETRY_CACHE = 64;
    private static final Map<EquipmentShape, Geometry> GEOMETRIES = new ConcurrentHashMap<>();

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
     * {@code nozzles} holds the drawn size codes of the nozzles that reach this cell (zero for the others).
     */
    public record CellKey(EquipmentShape shape, int x, int y, int z, long nozzles) {
        public static final int BODY = -1;
    }

    /** Solid box in units of 1/{@link #SOLID_RESOLUTION} block, lower corner inclusive, upper exclusive. */
    public record Box(int x0, int y0, int z0, int x1, int y1, int z1) {
    }

    /**
     * A nozzle as a fluid port: the cell holding its flange face, the box direction a pipe meets it from (the neighbour
     * at {@code cell + (dx, dy, dz)}), its port class and its index among the shape's nozzles.
     */
    public record Port(Cell cell, int dx, int dy, int dz, EquipmentShape.PortClass port, int nozzle) {
    }

    private ShapeRasterizer() {
    }

    public static Layout layout(EquipmentShape shape) {
        return geometry(shape).layout;
    }

    /** The key of a cell with every nozzle unconnected. */
    public static CellKey key(EquipmentShape shape, int x, int y, int z) {
        return key(shape, x, y, z, 0);
    }

    /** The key of a cell whose nozzles are drawn at {@code nozzles} (seven bits each; see {@link #withNozzle}). */
    public static CellKey key(EquipmentShape shape, int x, int y, int z, long nozzles) {
        var g = geometry(shape);
        int[] cell = {x, y, z};
        long reaching = 0;
        for (int k = 0; k < g.nozzleCells.size() && k < KEYED_NOZZLES; k++)
            if (g.nozzleCells.get(k).contains(new Cell(x, y, z))) reaching |= nozzles & (127L << (CODE_BITS * k));
        int bodyClass = g.bodyClass(cell[g.axial]);
        if (bodyClass >= 0 && reaching == 0 && !g.ladderColumns.contains(column(x, z))) cell[g.axial] = CellKey.BODY - bodyClass;
        return new CellKey(shape, cell[0], cell[1], cell[2], reaching);
    }

    /** {@code nozzles} with nozzle {@code k} drawn at {@code outside} metres (0: unconnected). */
    public static long withNozzle(long nozzles, int k, double outside) {
        if (k >= KEYED_NOZZLES) return nozzles;
        long code = outside <= 0 ? 0 : PipeSizes.code(outside);
        return nozzles & ~(127L << (CODE_BITS * k)) | code << (CODE_BITS * k);
    }

    public static List<Polygon> surface(CellKey key) {
        int[] c = cell(key);
        return surface(key.shape(), c[0], c[1], c[2], key.nozzles());
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
                    filled[a][b][d] = g.inside(c[0] + (a + .5) / s, c[1] + (b + .5) / s, c[2] + (d + .5) / s, key.nozzles());
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

    /** Whether a box-local point lies inside the faceted solid (vessel, supports, unconnected nozzles, a column's decks and rails). */
    public static boolean inside(EquipmentShape shape, double x, double y, double z) {
        return geometry(shape).inside(x, y, z, 0);
    }

    /** Whether a box-local point lies in the vessel, its supports or an unconnected nozzle: the solid without a column's access structure. */
    static boolean insideVessel(EquipmentShape shape, double x, double y, double z) {
        var g = geometry(shape);
        var p = g.transform.local(new double[]{x, y, z});
        if (g.inVessel(p)) return true;
        for (int k = 0; k < g.nozzles.size(); k++) if (g.inNozzle(p, k, 0)) return true;
        return false;
    }

    /** Whether a box-local point lies where one of a column's ladders is climbed. */
    public static boolean climbable(EquipmentShape shape, double x, double y, double z) {
        var g = geometry(shape);
        return g.column != null && g.column.climbable(g.transform.local(new double[]{x, y, z}));
    }

    /** A box column (x, z) of a standing vessel as one number. */
    private static long column(int x, int z) {
        return (long) x << 32 | z & 0xffffffffL;
    }

    /**
     * The selection outline seen from the box-local point {@code (x, y, z)}, as box-local segments {x0, y0, z0, x1, y1,
     * z1}. The vessel's silhouette against that eye: the two meridians, end to end, where the shell turns away from it
     * (seen along the axis, the shell's rim instead). Fixed meridians drew a line down the middle of a drum (owner report
     * 2026-10-16). Then the fixed edges: a skirt's base ring, a column's decks, each saddle's main edges and each nozzle's
     * flange face.
     */
    public static List<double[]> outline(EquipmentShape shape, double x, double y, double z) {
        var g = geometry(shape);
        var out = new ArrayList<>(g.edges());
        g.silhouette(new double[]{x, y, z}, out);
        return out;
    }

    /** The nozzles as fluid ports, in the order of {@link EquipmentShape#nozzles()}. */
    public static List<Port> ports(EquipmentShape shape) {
        return geometry(shape).ports;
    }

    /** The cells nozzle {@code k} may reach at its largest size: those whose keys carry its size. */
    public static Set<Cell> nozzleCells(EquipmentShape shape, int k) {
        return geometry(shape).nozzleCells.get(k);
    }

    /** The shell cell at the vessel's lowest inside point, where its fluid node stands: the bottom apex, or a lying vessel's bottom line. */
    public static Cell fluidCell(EquipmentShape shape) {
        return geometry(shape).fluidCell;
    }

    /** Surface polygons of the cell at its actual position, without key normalization. */
    static List<Polygon> surface(EquipmentShape shape, int x, int y, int z, long nozzles) {
        var g = geometry(shape);
        var out = new ArrayList<Polygon>();
        for (var facet : g.mesh.facets()) g.mesh.cell(facet, x, y, z, out);
        var cell = new Cell(x, y, z);
        for (int k = 0; k < g.nozzleCells.size(); k++)
            if (g.nozzleCells.get(k).contains(cell)) {
                var mesh = g.nozzle(k, code(nozzles, k));
                for (var facet : mesh.facets()) mesh.cell(facet, x, y, z, out);
            }
        return List.copyOf(out);
    }

    /** Total area of the vessel's facets with every nozzle unconnected, m²: what the cells' surface pieces must add up to. */
    static double facetArea(EquipmentShape shape) {
        return geometry(shape).mesh.area() + nozzleArea(shape);
    }

    /** Area of the nozzles drawn unconnected, m². */
    static double nozzleArea(EquipmentShape shape) {
        var g = geometry(shape);
        double total = 0;
        for (int k = 0; k < g.nozzleCells.size(); k++) total += g.nozzle(k, 0).area();
        return total;
    }

    private static int code(long nozzles, int k) {
        return k < KEYED_NOZZLES ? (int) (nozzles >>> (CODE_BITS * k) & 127) : 0;
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

    /** Per-shape tables (facet directions, head rings, plate and saddle layout), the static mesh, nozzles and layout. */
    private static final class Geometry {
        private final EquipmentShape shape;
        private final Transform transform;
        private final int n, contact;
        /** Index (0 x, 1 y, 2 z) of the box coordinate that runs along the vessel's axis. */
        private final int axial;
        /** Body texture class of each row of cells along the axis (-1: not a body row), and the first row of each class. */
        private int[] bodyClasses, representatives;
        /** The shell's radius, and its outside's (a column's cladding; the shell's for a drum). */
        private final double axis, radius, outer, apothemFactor, ground, plateLength;
        /** A column's access structure, or null. */
        private final ColumnStructure column;
        /** Box columns (x, z) a column's ladders pass through: their cells differ row by row, so they never share a body key. */
        private final Set<Long> ladderColumns = new HashSet<>();
        /** Outer radii (vertex) of a saddle's top plate and a skirt's base ring; a saddle's half width across the axis. */
        private final double saddleRadius, ringRadius, saddleLateral;
        /** A skirt's access opening: centre height and radius (0: none). */
        private final double openingHeight, openingRadius;
        /**
         * A drum's skirt is hollow: drawn on both faces, with its bottom head, its outlet line and a floor plate inside
         * (owner report 2026-10-16). Its slots, from {@code slotLow} to {@code slotHigh} (0: none), are one facet wide and
         * centred on the shell vertices at frame +x, +z and -x: the left, the back and the right.
         */
        private final boolean hollow;
        private final double slotLow, slotHigh, innerRadius;
        private final double[] normalCos, normalSin, vertexCos, vertexSin, saddles, meridian;
        private final double[][] rings;
        private final Mesh mesh = new Mesh();
        private final List<EquipmentShape.Nozzle> nozzles;
        private final List<Set<Cell>> nozzleCells = new ArrayList<>();
        private final Map<Integer, Mesh> nozzleMeshes = new ConcurrentHashMap<>();
        private final List<Port> ports = new ArrayList<>();
        private final Layout layout;
        private final Cell fluidCell;

        Geometry(EquipmentShape shape) {
            this.shape = shape;
            transform = shape.transform();
            mesh.transform(transform);
            n = shape.facets();
            axis = shape.axis();
            radius = shape.radius();
            outer = shape.outerRadius();
            column = shape.column() ? new ColumnStructure(shape) : null;
            var along = transform.direction(new double[]{0, 1, 0});
            axial = along[0] != 0 ? 0 : along[1] != 0 ? 1 : 2;
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
                rings[k] = new double[]{shape.outerHeadDepth() * DeterministicMath.sin(phi), k == bands ? 0 : outer * DeterministicMath.cos(phi)};
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
            openingHeight = shape.openingHeight();
            openingRadius = shape.openingRadius();
            hollow = shape.skirt() && !shape.column();
            innerRadius = radius - EquipmentShape.SKIRT_WALL / apothemFactor;
            double slotTop = Math.min(shape.bottomTangent() - .2, .4 + 1.6);
            boolean slots = hollow && shape.diameter() >= 1 && slotTop - .4 >= .4;
            slotLow = slots ? .4 : 0;
            slotHigh = slots ? slotTop : 0;
            nozzles = shape.nozzles();

            build();
            if (shape.skirt()) skirtBase();
            if (openingRadius > 0) openingSleeve();
            for (double c : saddles) saddle(c);
            if (column != null) {
                var ladders = new Mesh();
                ladders.transform(transform);
                column.build(mesh, ladders, nozzles);
                var cells = new HashSet<Cell>();
                for (var facet : ladders.facets()) cells(facet, cells);
                for (var cell : cells) ladderColumns.add(column(cell.x(), cell.z()));
                mesh.facets().addAll(ladders.facets());
            }
            for (int k = 0; k < nozzles.size(); k++) {
                var nozzle = nozzles.get(k);
                var cells = new HashSet<Cell>();
                // Every cell the largest pipe's nozzle reaches: a nozzle takes any pipe.
                var largest = nozzle(k, PipeSizes.code(PipeSizes.MAX_OUTSIDE));
                for (var facet : largest.facets()) cells(facet, cells);
                nozzleCells.add(Set.copyOf(cells));
                var face = nozzle.face();
                var inner = transform.point(new double[]{face[0] - nozzle.direction()[0] * .01, face[1] - nozzle.direction()[1] * .01, face[2] - nozzle.direction()[2] * .01});
                var out = transform.direction(nozzle.direction());
                // A column's nozzles are drawn only: it is not a fluid device (yet).
                if (!shape.column())
                    ports.add(new Port(new Cell(floor(inner[0]), floor(inner[1]), floor(inner[2])), (int) out[0], (int) out[1], (int) out[2], nozzle.role().port, k));
            }
            classify();
            var low = shape.orientation().horizontal()
                    ? transform.point(new double[]{axis - radius + .01, (shape.bottomTangent() + shape.topTangent()) / 2, axis})
                    : transform.point(new double[]{axis, shape.bottomTangent() - shape.headDepth() + .01, axis});
            fluidCell = new Cell(floor(low[0]), floor(low[1]), floor(low[2]));
            layout = buildLayout();
        }

        /**
         * The shell, the skirt (with its opening cut out) and the heads. A column's shell and top head are clad: the
         * cladding stands {@link EquipmentShape#INSULATION} out from the shell, in aluminium panels, its lower edge closed
         * over the skirt at the bottom tangent line.
         */
        private void build() {
            double yB = shape.bottomTangent(), yT = shape.topTangent(), plate = EquipmentShape.PLATE;
            boolean clad = column != null;
            var up = new double[]{0, 1, 0};
            var courses = new Uv(new double[]{axis, yB, axis}, up, 0, clad ? Material.CLADDING.period() : 2 * COURSE);
            var skirtTiles = new Uv(new double[]{axis, yB, axis}, up, 0, SUPPORT_TILE);
            var centre = new double[]{axis, (yB + yT) / 2, axis};
            var round = new Radial(new double[]{axis, 0, axis}, up);
            double chord = 2 * outer * DeterministicMath.sin(Math.PI / n);
            for (int i = 0; i < n; i++) {
                double[] a = shell(i, 0), b = shell(i + 1, 0);
                double[] tangent = unit(b[0] - a[0], 0, b[2] - a[2]);
                // Arc length runs counter-clockwise seen from the top head, leftwards seen from outside: u runs against it.
                var plates = new Uv(a, neg(tangent), -i * chord, clad ? Material.CLADDING.period() : plateLength);
                mesh.add(List.of(shell(i, yB), shell(i + 1, yB), shell(i + 1, yT), shell(i, yT)), centre, round, clad ? Material.CLADDING : Material.SHELL, plates, courses);
                if (clad) mesh.flat(List.of(ring(i, yB), ring(i + 1, yB), shell(i + 1, yB), shell(i, yB)), new double[]{0, -1, 0}, Material.CLADDING);
                if (shape.skirt()) {
                    var skirtU = new Uv(a, neg(tangent), -i * chord, plateLength / 2);
                    double apothem = radius * apothemFactor;
                    for (var piece : skirtFacet(i, radius, apothem)) mesh.add(piece, centre, round, Material.SUPPORT, skirtU, skirtTiles);
                    if (hollow) {
                        // The inside face, seen through the opening and the slots, and the slots' cut edges.
                        var beyond = new double[]{axis + 2 * radius * normalCos[i], yB / 2, axis + 2 * radius * normalSin[i]};
                        var inward = new Inward(new double[]{axis, 0, axis}, up);
                        for (var piece : skirtFacet(i, innerRadius, apothem - EquipmentShape.SKIRT_WALL))
                            mesh.add(piece, beyond, inward, Material.SUPPORT, skirtU, skirtTiles);
                        slotEdges(i);
                    }
                }
                for (int side = 0; side < 2; side++) {
                    if (side == 0 && shape.skirt() && !hollow) continue; // a column's skirt hides its bottom head
                    double base = side == 0 ? yB : yT, sign = side == 0 ? -1 : 1;
                    var apex = new double[]{axis, base, axis};
                    var spheroid = new Spheroid(apex, up, outer, shape.outerHeadDepth());
                    for (int k = 0; k < rings.length - 1; k++) {
                        var polygon = new ArrayList<double[]>(4);
                        polygon.add(head(i, k, base, sign));
                        polygon.add(head(i + 1, k, base, sign));
                        polygon.add(head(i + 1, k + 1, base, sign));
                        if (rings[k + 1][1] > 0) polygon.add(head(i, k + 1, base, sign));
                        // v runs from the tangent line weld (sprite top) to the apex (bottom), once over the head.
                        double[] outer = lerp(head(i, k, base, sign), head(i + 1, k, base, sign), .5);
                        double[] inner = lerp(head(i, k + 1, base, sign), head(i + 1, k + 1, base, sign), .5);
                        var towardsTangent = unit(outer[0] - inner[0], outer[1] - inner[1], outer[2] - inner[2]);
                        mesh.add(polygon, apex, spheroid, Material.HEAD, plates, new Uv(outer, towardsTangent, -meridian[k], meridian[rings.length - 1]));
                    }
                }
            }
        }

        /**
         * Skirt facet {@code i} from the base ring to the bottom tangent line, its vertices at radius {@code r} (its plane
         * at {@code apothem} from the axis), less the access opening and a slot where they reach it.
         */
        private List<List<double[]>> skirtFacet(int i, double r, double apothem) {
            double plate = EquipmentShape.PLATE, yB = shape.bottomTangent();
            var normal = new double[]{normalCos[i], 0, normalSin[i]};
            List<List<double[]>> pieces = List.of(List.of(vertex(i, r, plate), vertex(i + 1, r, plate), vertex(i + 1, r, yB), vertex(i, r, yB)));
            var hole = openingRadius > 0 ? hole(i, apothem) : null;
            if (hole != null) pieces = cut(pieces, hole, normal);
            var slot = slot(i, r);
            if (slot != null) pieces = cut(pieces, slot, normal);
            return pieces;
        }

        private static List<List<double[]>> cut(List<List<double[]>> pieces, List<double[]> hole, double[] normal) {
            var out = new ArrayList<List<double[]>>();
            for (var piece : pieces) out.addAll(subtract(piece, hole, normal));
            return out;
        }

        /**
         * The access opening on the plane of skirt facet {@code i} at {@code apothem} from the axis, or null when it does
         * not reach that facet: the opening's circle around its horizontal axis towards the front (frame -z), projected
         * along that axis onto the facet.
         */
        private List<double[]> hole(int i, double apothem) {
            if (normalSin[i] > -.3) return null;
            double low = Math.min(vertexCos[i], vertexCos[i + 1]) * radius, high = Math.max(vertexCos[i], vertexCos[i + 1]) * radius;
            if (high < -openingRadius || low > openingRadius) return null;
            var hole = new ArrayList<double[]>(16);
            for (int j = 0; j < 16; j++) {
                double angle = 2 * Math.PI * j / 16, x = openingRadius * DeterministicMath.cos(angle), y = openingHeight + openingRadius * DeterministicMath.sin(angle);
                hole.add(new double[]{axis + x, y, axis + (apothem - x * normalCos[i]) / normalSin[i]});
            }
            return hole;
        }

        /** The part {s0, s1} of skirt facet {@code i} (0 at vertex i, 1 at vertex i + 1) a slot takes, or null. */
        private double[] slotSpan(int i) {
            if (slotHigh <= 0) return null;
            if (slotVertex(i)) return new double[]{0, .5};
            if (slotVertex(i + 1)) return new double[]{.5, 1};
            return null;
        }

        private boolean slotVertex(int v) {
            v = Math.floorMod(v, n);
            return v == 0 || v == n / 4 || v == n / 2;
        }

        /** The slot's rectangle on skirt facet {@code i} at vertex radius {@code r}, or null. */
        private List<double[]> slot(int i, double r) {
            var span = slotSpan(i);
            if (span == null) return null;
            double[] a0 = vertex(i, r, slotLow), b0 = vertex(i + 1, r, slotLow), a1 = vertex(i, r, slotHigh), b1 = vertex(i + 1, r, slotHigh);
            return List.of(lerp(a0, b0, span[0]), lerp(a0, b0, span[1]), lerp(a1, b1, span[1]), lerp(a1, b1, span[0]));
        }

        /** A slot's cut edges through the skirt plate on facet {@code i}: its sill, its head, and its jamb where the slot ends on this facet. */
        private void slotEdges(int i) {
            var span = slotSpan(i);
            if (span == null) return;
            for (double y : new double[]{slotLow, slotHigh}) {
                var edge = List.of(lerp(vertex(i, radius, y), vertex(i + 1, radius, y), span[0]), lerp(vertex(i, radius, y), vertex(i + 1, radius, y), span[1]),
                        lerp(vertex(i, innerRadius, y), vertex(i + 1, innerRadius, y), span[1]), lerp(vertex(i, innerRadius, y), vertex(i + 1, innerRadius, y), span[0]));
                mesh.flat(edge, new double[]{0, y == slotLow ? 1 : -1, 0}, Material.SUPPORT);
            }
            // The jamb stands where the slot ends inside this facet (s = 0.5); the facet's other end is the slot's middle.
            double s = span[0] == 0 ? span[1] : span[0];
            var jamb = List.of(lerp(vertex(i, radius, slotLow), vertex(i + 1, radius, slotLow), s), lerp(vertex(i, radius, slotHigh), vertex(i + 1, radius, slotHigh), s),
                    lerp(vertex(i, innerRadius, slotHigh), vertex(i + 1, innerRadius, slotHigh), s), lerp(vertex(i, innerRadius, slotLow), vertex(i + 1, innerRadius, slotLow), s));
            var along = sub(vertex(i + 1, radius, 0), vertex(i, radius, 0));
            // It faces into the slot: towards vertex i when the slot holds vertex i, the plate lying towards vertex i + 1.
            mesh.flat(jamb, span[0] == 0 ? neg(along) : along, Material.SUPPORT);
        }

        /** Vertex {@code i} of a skirt face at radius {@code r} and axial position {@code y}, in the vessel frame. */
        private double[] vertex(int i, double r, double y) {
            return new double[]{axis + r * vertexCos[i], y, axis + r * vertexSin[i]};
        }

        /**
         * The opening's sleeve: a collar round the hole, standing proud of the skirt. A column's carries its bore and a dark
         * back; a drum's (a hollow skirt's) is its skirt port, whose bore and faces its bottom outlet draws at the line's
         * size ({@link #skirtPort}).
         */
        private void openingSleeve() {
            var f = openingFrame();
            double apothem = radius * apothemFactor, rim = openingRadius + EquipmentShape.PORT_COLLAR, outer = radius + 1 / 16.0, back = apothem - .12;
            // A hollow skirt's sleeve is one tube through the plate (inside it the tube is hidden), closed at its inner end.
            mesh.prism(f, rim, hollow ? back : Math.sqrt(apothem * apothem - rim * rim) - .02, outer, 16, Material.SUPPORT);
            if (hollow) return;
            mesh.bore(f, openingRadius, back, outer, 16, Material.SUPPORT);
            mesh.annulus(f, openingRadius, rim, outer, 16, 1, Material.SUPPORT);
            mesh.annulus(f, 0, openingRadius, back, 16, 1, Material.OPENING);
        }

        /**
         * A drum's skirt port round its outlet line of radius {@code line} (owner request 2026-10-16: the port follows the
         * line): its bore, the line's clearance wider, and the collar's outer and inner faces out to the hole cut for the
         * largest line.
         */
        private void skirtPort(Mesh m, double line) {
            var f = openingFrame();
            double apothem = radius * apothemFactor, rim = openingRadius + EquipmentShape.PORT_COLLAR, outer = radius + 1 / 16.0, back = apothem - .12;
            double port = Math.min(openingRadius, line + EquipmentShape.PORT_CLEARANCE);
            m.bore(f, port, back, outer, 16, Material.SUPPORT);
            m.annulus(f, port, rim, outer, 16, 1, Material.SUPPORT);
            m.annulus(f, port, rim, back, 16, -1, Material.SUPPORT);
        }

        private Frame openingFrame() {
            return new Frame(new double[]{axis, openingHeight, axis}, new double[]{0, 0, -1}, new double[]{1, 0, 0});
        }

        /** A skirt's foot: the plate under it and its base ring, the ring's top and rim, and the anchor chairs. */
        private void skirtBase() {
            double plate = EquipmentShape.PLATE;
            var centre = new double[]{axis, plate / 2, axis};
            var round = new Radial(new double[]{axis, 0, axis}, new double[]{0, 1, 0});
            var bottom = new ArrayList<double[]>(n);
            for (int i = 0; i < n; i++) bottom.add(rim(i, 0));
            mesh.add(bottom, centre, FLAT, Material.SUPPORT, null, null);
            if (hollow) {
                // A hollow skirt's floor: the base plate's top inside the skirt plate.
                var floor = new ArrayList<double[]>(n);
                for (int i = 0; i < n; i++) floor.add(vertex(i, innerRadius, plate));
                mesh.add(floor, new double[]{axis, 0, axis}, FLAT, Material.SUPPORT, null, null);
            }
            for (int i = 0; i < n; i++) {
                mesh.add(List.of(rim(i, plate), rim(i + 1, plate), ring(i + 1, plate), ring(i, plate)), centre, FLAT, Material.SUPPORT, null, null);
                mesh.add(List.of(rim(i, 0), rim(i + 1, 0), rim(i + 1, plate), rim(i, plate)), centre, round, Material.SUPPORT, null, null);
            }
            int chairs = shape.anchorChairs();
            double apothem = radius * apothemFactor, depth = EquipmentShape.BASE_RING, half = EquipmentShape.CHAIR / 2;
            double low = plate, high = plate + EquipmentShape.CHAIR;
            for (int k = 0; k < chairs; k++) {
                int i = chairFacet(k, chairs);
                // A box on the ring against shell facet i: corners at radial r and tangential t from the axis.
                double[] in0 = chair(i, apothem, -half), in1 = chair(i, apothem, half);
                double[] out0 = chair(i, apothem + depth, -half), out1 = chair(i, apothem + depth, half);
                var middle = chair(i, apothem + depth / 2, 0);
                middle[1] = (low + high) / 2;
                mesh.add(List.of(at(out0, low), at(out1, low), at(out1, high), at(out0, high)), middle, FLAT, Material.SUPPORT, null, null);
                mesh.add(List.of(at(in0, high), at(out0, high), at(out1, high), at(in1, high)), middle, FLAT, Material.SUPPORT, null, null);
                mesh.add(List.of(at(in0, low), at(out0, low), at(out0, high), at(in0, high)), middle, FLAT, Material.SUPPORT, null, null);
                mesh.add(List.of(at(in1, low), at(out1, low), at(out1, high), at(in1, high)), middle, FLAT, Material.SUPPORT, null, null);
            }
        }

        /**
         * The shell facet anchor chair {@code k} of {@code chairs} stands against. A drum's skirt is pierced on its axes (the
         * access opening at the front, slots at the left, back and right), so its chairs stand between them; a column's
         * ladders come down on the diagonals, so its chairs keep to the axes.
         */
        private int chairFacet(int k, int chairs) {
            return shape.column() ? k * n / chairs : (2 * k + 1) * n / (2 * chairs);
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
            var round = new Radial(new double[]{axis, 0, axis}, new double[]{0, 1, 0});
            // Top plate: outside faces (beside the web), axial ends and horn ends.
            for (int j = first; j < last; j++) {
                for (double[] span : new double[][]{{y0, w0}, {w1, y1}})
                    mesh.add(List.of(outer(j, span[0]), outer(j + 1, span[0]), outer(j + 1, span[1]), outer(j, span[1])), axisPoint, round, Material.SUPPORT, null, null);
                for (double y : new double[]{y0, y1})
                    mesh.add(List.of(ring(j, y), ring(j + 1, y), outer(j + 1, y), outer(j, y)), axisPoint, FLAT, Material.SUPPORT, null, null);
            }
            for (int j : new int[]{first, last})
                mesh.add(List.of(ring(j, y0), ring(j, y1), outer(j, y1), outer(j, y0)), lowest, FLAT, Material.SUPPORT, null, null);
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
                mesh.add(List.of(new double[]{deck, y0, horn[2]}, new double[]{deck, y1, horn[2]}, at(horn, y1), at(horn, y0)), ribCentre, FLAT, Material.SUPPORT, null, null);
                for (double y : new double[]{y0, y1}) strips(edge, y, deck, ribCentre);
                for (double[] span : new double[][]{{y0, w0}, {w1, y1}})
                    mesh.add(List.of(new double[]{deck, span[0], inner[2]}, new double[]{deck, span[1], inner[2]}, at(inner, span[1]), at(inner, span[0])),
                            ribCentre, FLAT, Material.SUPPORT, null, null);
            }
            // Base plate: underside, four sides, and its top beside the web (the web and ribs cover the rest).
            double lateral = saddleLateral;
            var baseCentre = new double[]{ground + plate / 2, c, axis};
            double[][] corners = {{-lateral, y0}, {lateral, y0}, {lateral, y1}, {-lateral, y1}};
            var under = new ArrayList<double[]>(4);
            for (var corner : corners) under.add(new double[]{ground, corner[1], axis + corner[0]});
            mesh.add(under, baseCentre, FLAT, Material.SUPPORT, null, null);
            for (int e = 0; e < 4; e++) {
                double[] a = corners[e], b = corners[(e + 1) % 4];
                mesh.add(List.of(new double[]{ground, a[1], axis + a[0]}, new double[]{ground, b[1], axis + b[0]}, new double[]{deck, b[1], axis + b[0]},
                        new double[]{deck, a[1], axis + a[0]}), baseCentre, FLAT, Material.SUPPORT, null, null);
            }
            for (double[] span : new double[][]{{y0, w0}, {w1, y1}})
                mesh.add(List.of(new double[]{deck, span[0], axis - webHalf}, new double[]{deck, span[0], axis + webHalf}, new double[]{deck, span[1], axis + webHalf},
                        new double[]{deck, span[1], axis - webHalf}), baseCentre, FLAT, Material.SUPPORT, null, null);
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
                mesh.add(List.of(new double[]{deck, y, a[2]}, new double[]{deck, y, b[2]}, b, a), centre, FLAT, Material.SUPPORT, null, null);
            }
        }

        /**
         * Nozzle {@code k} drawn at size {@code code} (0: unconnected, at its default size): a neck from under the vessel's
         * surface and a flange at the pipe's size whose face lies on the block face. Up to the nozzle's largest size the
         * neck is the pipe's and meets the flange in a weld-neck hub; a larger pipe's neck keeps that size until it has
         * left the vessel and widens from there to the flange in a concentric reducer, so the size changes where the port
         * meets the vessel and never at the flange (owner decision 2026-10-16). A bottom outlet's line also has its riser
         * from the head and its elbow into the neck, and its skirt port round it, all at the neck's size (owner request
         * 2026-10-16). Built once per size.
         */
        Mesh nozzle(int k, int code) {
            return nozzleMeshes.computeIfAbsent(k * 128 + code, key -> {
                var nozzle = nozzles.get(k);
                double od = code == 0 ? nozzle.defaultOd() : PipeSizes.outsideOf(code), neck = Math.min(od, nozzle.maxOd());
                var m = new Mesh();
                m.transform(transform);
                var dir = nozzle.direction();
                var ref = new double[3];
                ref[(nozzle.axis() + 1) % 3] = 1;
                var f = new Frame(nozzle.base(), dir, ref);
                double r = neck / 2, flange = PipeSizes.flange(od) / 2, t = PipeSizes.flangeThickness(od), hub = PipeSizes.hub(od), length = nozzle.length();
                double hubR = PipeSizes.hubOutside(od) / 2;
                int sides = PipeSizes.sides(od);
                // The neck's taper into the flange: a weld-neck hub, or the reducer from where the neck has left the vessel.
                double taper = neck < od ? Math.max(0, Math.min(nozzle.exit(), length - t - .02)) : length - t - hub;
                m.prism(f, r, 0, taper, sides, Material.PIPE);
                m.frustum(f, r, hubR, taper, length - t, sides, Material.PIPE);
                m.annulus(f, hubR, flange, length - t, sides, -1, Material.FLANGE);
                m.prism(f, flange, length - t, length, sides, Material.FLANGE);
                m.annulus(f, 0, flange, length, sides, 1, Material.FLANGE);
                if (nozzle.riser() > 0) {
                    // Back from the neck's start: a straight run, the elbow (bend radius 1 D of the line drawn) and the riser
                    // up into the head at its pole, maxOd behind the neck's start (owner request 2026-10-16: the line from
                    // the drum's bottom to its port follows the pipe).
                    double bend = neck, run = nozzle.maxOd() - bend, lift = nozzle.base()[1] + bend;
                    var down = new double[]{0, -1, 0};
                    m.prism(f, r, -run, 0, sides, Material.PIPE);
                    var corner = new double[]{nozzle.base()[0] - dir[0] * run, lift, nozzle.base()[2] - dir[2] * run};
                    m.torus(new Frame(corner, cross(down, neg(dir)), down), bend, r, 0, Math.PI / 2, 6, sides, Material.PIPE);
                    var foot = new double[]{corner[0] - dir[0] * bend, lift, corner[2] - dir[2] * bend};
                    m.prism(new Frame(foot, new double[]{0, 1, 0}, dir), r, 0, nozzle.riser() - lift, sides, Material.PIPE);
                }
                if (nozzle.riser() > 0 && hollow && openingRadius > 0) skirtPort(m, r);
                return m;
            });
        }

        /** Whether a frame point lies in nozzle {@code k} at size {@code code}: its neck (a larger pipe's reducer too), or its flange. */
        private boolean inNozzle(double[] p, int k, int code) {
            var nozzle = nozzles.get(k);
            double od = code == 0 ? nozzle.defaultOd() : PipeSizes.outsideOf(code), neck = Math.min(od, nozzle.maxOd());
            var base = nozzle.base();
            double s = (p[nozzle.axis()] - base[nozzle.axis()]) * nozzle.sign(), rho2 = 0;
            for (int a = 0; a < 3; a++) if (a != nozzle.axis()) rho2 += (p[a] - base[a]) * (p[a] - base[a]);
            double t = PipeSizes.flangeThickness(od), flange = PipeSizes.flange(od) / 2;
            double length = nozzle.length(), radius = neck / 2;
            if (neck < od) {
                double taper = Math.max(0, Math.min(nozzle.exit(), length - t - .02));
                if (s > taper) radius += (PipeSizes.hubOutside(od) / 2 - radius) * Math.min(1, (s - taper) / (length - t - taper));
            }
            return s >= 0 && s <= length - t && rho2 <= radius * radius || s > length - t && s <= length && rho2 <= flange * flange;
        }

        private void cells(Facet facet, Set<Cell> out) {
            for (int x = floor(facet.min()[0] - EPS); x < ceil(facet.max()[0] + EPS); x++)
                for (int y = floor(facet.min()[1] - EPS); y < ceil(facet.max()[1] + EPS); y++)
                    for (int z = floor(facet.min()[2] - EPS); z < ceil(facet.max()[2] + EPS); z++)
                        if (piece(facet, x, y, z) != null) out.add(new Cell(x, y, z));
        }

        /**
         * Shell cells: every cell a facet or a nozzle reaches, and a drum's fluid node, which carries its identity even where
         * nothing is drawn. The other cells inside the vessel are interior, a hollow skirt's too: its collision stays a
         * full cylinder, so nothing spawns or is placed inside it, and the cells showing its inside are shell cells.
         */
        private Layout buildLayout() {
            int sx = shape.sizeX(), sy = shape.sizeY(), sz = shape.sizeZ();
            var cells = new HashSet<Cell>();
            for (var facet : mesh.facets()) cells(facet, cells);
            for (var nozzle : nozzleCells) cells.addAll(nozzle);
            if (!shape.column()) cells.add(fluidCell);
            var shellCells = new ArrayList<Cell>();
            var interiorCells = new ArrayList<Cell>();
            for (int x = 0; x < sx; x++)
                for (int y = 0; y < sy; y++)
                    for (int z = 0; z < sz; z++) {
                        var cell = new Cell(x, y, z);
                        if (cells.contains(cell)) shellCells.add(cell);
                        else if (inVessel(transform.local(new double[]{x + .5, y + .5, z + .5}))) interiorCells.add(cell);
                    }
            return new Layout(shape, shellCells, interiorCells);
        }

        /** Body rows along the axis and their texture classes: rows wholly in the cylinder, clear of supports, opening and nozzles. */
        private void classify() {
            int cells = axial == 1 ? shape.sizeY() : axial == 0 ? shape.sizeX() : shape.sizeZ();
            double yB = shape.bottomTangent();
            bodyClasses = new int[cells];
            var classes = new HashMap<Integer, Integer>();
            var firstRows = new ArrayList<Integer>();
            for (int a = 0; a < cells; a++) {
                bodyClasses[a] = -1;
                var probe = new double[3];
                probe[axial] = a + .5;
                double low = transform.local(probe)[1] - .5;
                if (!body(low)) continue;
                // Rows of one class share geometry and the texture phase: region (skirt or shell) and course pair.
                int signature = (low < yB ? 0 : 4) + Math.floorMod((int) Math.round(low) - floor(yB), 4);
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

        /**
         * Whether the frame rows [low, low + 1) lie wholly in the cylindrical shell or skirt, clear of every special part
         * (a column's: the cladding's lower edge, its flashing rings, and its platforms with their manways).
         */
        private boolean body(double low) {
            double high = low + 1, start = shape.skirt() ? 1 : shape.bottomTangent(), half = EquipmentShape.SADDLE_WIDTH / 2;
            if (low < start - EPS || high > shape.topTangent() + EPS) return false;
            if (column != null) {
                double yB = shape.bottomTangent();
                if (low < yB + 1 - EPS && high > yB - 1 + EPS) return false;
                for (double y : column.flashingHeights()) if (low < y + ColumnStructure.FLASHING + EPS && high > y - EPS) return false;
                for (var span : column.platformSpans()) if (low < span[1] + EPS && high > span[0] - EPS) return false;
            }
            for (double centre : saddles) if (low < centre + half + EPS && high > centre - half - EPS) return false;
            if (openingRadius > 0 && low < openingHeight + openingRadius + .1 && high > openingHeight - openingRadius - .1) return false;
            if (slotHigh > 0 && low < slotHigh + EPS && high > slotLow - EPS) return false;
            // A hollow skirt shows its bottom head in the rows it reaches.
            if (hollow && high > shape.bottomTangent() - shape.headDepth() - EPS && low < shape.bottomTangent() + EPS) return false;
            for (var nozzle : nozzles) {
                double reach = PipeSizes.flange(nozzle.maxOd()) / 2, y0, y1;
                if (nozzle.axis() == 1) {
                    y0 = Math.min(nozzle.base()[1], nozzle.face()[1]);
                    y1 = Math.max(nozzle.base()[1], nozzle.face()[1]);
                } else {
                    y0 = nozzle.base()[1] - reach;
                    y1 = Math.max(nozzle.base()[1] + reach, nozzle.riser());
                }
                if (low < y1 + EPS && high > y0 - EPS) return false;
            }
            return true;
        }

        int bodyClass(int a) {
            return a >= 0 && a < bodyClasses.length ? bodyClasses[a] : -1;
        }

        boolean inside(double x, double y, double z, long codes) {
            var p = transform.local(new double[]{x, y, z});
            if (inVessel(p) || column != null && column.inside(p)) return true;
            for (int k = 0; k < nozzles.size(); k++) if (inNozzle(p, k, code(codes, k))) return true;
            return false;
        }

        /** Inside the vessel and its supports, at a frame point: what the interior cells fill. */
        private boolean inVessel(double[] p) {
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
                int i = chairFacet(k, chairs);
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
            if (y >= (shape.skirt() ? 0 : shape.bottomTangent()) && y <= shape.topTangent()) return y < shape.bottomTangent() ? radius : outer;
            double off = y < shape.bottomTangent() ? shape.bottomTangent() - y : y - shape.topTangent();
            for (int k = 0; k + 1 < rings.length; k++) {
                double[] a = rings[k], b = rings[k + 1];
                if (off <= b[0]) return a[1] + (b[1] - a[1]) * (off - a[0]) / (b[0] - a[0]);
            }
            return 0;
        }

        /** The outline's fixed edges, built once; the silhouette's meridians follow the eye ({@link #silhouette}). */
        private volatile List<double[]> edges;

        List<double[]> edges() {
            var built = edges;
            if (built == null) edges = built = buildEdges();
            return built;
        }

        /**
         * The meridians on the silhouette seen from a box point: where the line from the eye touches the shell's
         * cross-section, at the shell vertices nearest the two tangent points; from inside that cross-section (looking
         * along the axis) the shell's rim at the tangent line nearer the eye.
         */
        void silhouette(double[] eye, List<double[]> out) {
            var e = transform.local(eye);
            double dx = e[0] - axis, dz = e[2] - axis, d = Math.sqrt(dx * dx + dz * dz);
            if (d <= outer) {
                double y = e[1] >= (shape.bottomTangent() + shape.topTangent()) / 2 ? shape.topTangent() : shape.bottomTangent();
                for (int i = 0; i < n; i++) segment(out, shell(i, y), shell(i + 1, y));
                return;
            }
            double towards = DeterministicMath.atan2(dz, dx), spread = DeterministicMath.acos(outer / d);
            for (double angle : new double[]{towards - spread, towards + spread})
                meridian(Math.floorMod((int) Math.round(angle * n / (2 * Math.PI)), n), out);
        }

        /** Meridian {@code i} from the vessel's foot (a skirt's base, or the bottom apex) over the top head to its apex. */
        private void meridian(int i, List<double[]> out) {
            double yB = shape.bottomTangent(), yT = shape.topTangent();
            var points = new ArrayList<double[]>();
            if (shape.skirt()) points.add(ring(i, EquipmentShape.PLATE));
            else for (int k = rings.length - 1; k >= 0; k--) points.add(head(i, k, yB, -1));
            for (int k = 0; k < rings.length; k++) points.add(head(i, k, yT, 1));
            for (int p = 0; p + 1 < points.size(); p++) segment(out, points.get(p), points.get(p + 1));
        }

        private List<double[]> buildEdges() {
            var segments = new ArrayList<double[]>();
            double plate = EquipmentShape.PLATE;
            if (column != null)
                for (var p : column.plan().platforms()) {
                    double y = p.level() + ColumnStructure.DECK, r = column.plan().deckRadius();
                    int count = Math.max(2, (int) Math.ceil((p.to() - p.from()) * r / .5));
                    for (int j = 0; j < count; j++) {
                        double a0 = p.from() + (p.to() - p.from()) * j / count, a1 = p.from() + (p.to() - p.from()) * (j + 1) / count;
                        segment(segments, new double[]{axis + r * DeterministicMath.cos(a0), y, axis + r * DeterministicMath.sin(a0)},
                                new double[]{axis + r * DeterministicMath.cos(a1), y, axis + r * DeterministicMath.sin(a1)});
                    }
                }
            if (shape.skirt())
                for (int i = 0; i < n; i++) segment(segments, rim(i, plate), rim(i + 1, plate));
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
            // Each nozzle's flange face as a square around its centre.
            for (var nozzle : nozzles) {
                var face = nozzle.face();
                double h = PipeSizes.flange(nozzle.defaultOd()) / 2;
                int a = (nozzle.axis() + 1) % 3, b = (nozzle.axis() + 2) % 3;
                double[][] square = new double[4][];
                for (int corner = 0; corner < 4; corner++) {
                    square[corner] = face.clone();
                    square[corner][a] += corner == 0 || corner == 3 ? -h : h;
                    square[corner][b] += corner < 2 ? -h : h;
                }
                for (int e = 0; e < 4; e++) segment(segments, square[e], square[(e + 1) % 4]);
            }
            return List.copyOf(segments);
        }

        private void segment(List<double[]> segments, double[] a, double[] b) {
            double[] p = transform.point(a), q = transform.point(b);
            segments.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
        }

        private static double[] at(double[] p, double y) {
            return new double[]{p[0], y, p[2]};
        }

        /** Shell vertex {@code i} at axial position {@code y}, in the vessel frame (a column's under its cladding: the skirt's). */
        private double[] ring(int i, double y) {
            return new double[]{axis + radius * vertexCos[i], y, axis + radius * vertexSin[i]};
        }

        /** Vertex {@code i} of the outside surface (a column's cladding) at axial position {@code y}. */
        private double[] shell(int i, double y) {
            return new double[]{axis + outer * vertexCos[i], y, axis + outer * vertexSin[i]};
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
    }
}
