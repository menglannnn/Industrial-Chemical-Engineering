package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A process vessel on the block grid (1 block = 1 m): a cylindrical shell of the given diameter and tangent-to-tangent
 * length between two 2:1 semi-ellipsoidal heads (depth D/4 each), standing or lying on its supports, with its nozzles.
 * The shell is drawn as a regular N-gon prism whose facets are at most about 1/3 m wide (a column's at most 1/4 m), and
 * each head as N-gon bands at the same angular step (owner decisions D3 and D5 of batch
 * 2026-10-15-custom-equipment-rendering: diameters in 0.5 m steps, lengths in whole metres; refined 2026-10-16).
 *
 * <p>Two kinds (owner request 2026-10-16). A <b>drum</b> stands on a skirt or lies on two saddles (owner decisions
 * 2026-10-15) and is a fluid reservoir whose standard nozzles are its ports: vertical — a vapour outlet at the top head's
 * centre, a liquid outlet just above the bottom tangent line facing the front and an inlet in the upper shell facing the
 * right; horizontal — two vapour nozzles on the top near the ends and a liquid outlet low on the front head. A
 * <b>column</b> is a tall vertical vessel laid out by its trays ({@link ColumnTrays}), insulated and clad, with its
 * platforms, ladders, manways and process nozzles ({@link ColumnStructure}).
 *
 * <p>Supports. A vertical vessel stands on a skirt: a cylinder of the shell diameter from the ground to the bottom tangent
 * line, {@code support} metres high, which hides the bottom head (0 = none: the vessel rests on its lower head, the debug
 * command's bare form). The skirt stands on a base ring projecting {@link #BASE_RING} beyond it, with
 * {@link #anchorChairs()} anchor-bolt chairs. A horizontal vessel lies on two saddles centred {@link #SADDLE_POSITION} ×
 * T/T from each tangent line (Zick), cradling the bottom {@link #SADDLE_CONTACT_DEGREES}° of the shell; {@code support}
 * is the height of the shell's box above the ground.
 *
 * <p>Frames. The vessel is built in its own frame, axis along +y, in cells of a box whose width is odd so the axis runs
 * through cell centres: then every nozzle's centre line does too and its flange face lies on a block face, where a pipe
 * meets it. Vertical: frame x and z are box-aligned before the turn, and the front (where the liquid outlet and a skirt's
 * access opening face) is frame -z. Horizontal: frame x is up, frame y the axis, frame z across; the front head is the
 * frame's y = 0 end. {@link #transform()} turns the frame by quarter turns so the front faces {@link #front()} and shifts
 * the box to start at the origin.
 */
public record EquipmentShape(Kind kind, double diameter, double tangentLength, Orientation orientation, Heading front, int support,
                             @Nullable ColumnTrays trays) {
    public static final double MIN_DIAMETER = .5, MAX_DIAMETER = 10, MAX_COLUMN_DIAMETER = 15, MAX_TANGENT_LENGTH = 64;
    public static final int MAX_SUPPORT = 16;
    /** Saddles: centred this fraction of T/T from each tangent line, this wide along the axis (m), over this arc of the shell. */
    public static final double SADDLE_POSITION = .2, SADDLE_WIDTH = .375, SADDLE_CONTACT_DEGREES = 120;
    /**
     * Support parts, m, at one to two texels (1/16 m) so they read at block scale: thin plates (saddle top and base plates,
     * skirt base ring), the saddle's web and rib thickness, the base ring's projection beyond the skirt, and the width and
     * height of an anchor chair.
     */
    public static final double PLATE = 1 / 16.0, SADDLE_WEB = .125, SADDLE_RIB = .125, BASE_RING = .125, CHAIR = .25;
    /** A nozzle's flange face stands at least this far beyond the vessel surface (photo survey: 150-300 mm), on the next block face. */
    public static final double STAND_OUT = .15;
    /** A column's insulation under its aluminium cladding, m (photo survey: 50-150 mm); a drum is bare. */
    public static final double INSULATION = .1;
    /**
     * Outside area of one 2:1 head over D²: half an oblate spheroid of semi-axes D/2 and D/4 (eccentricity √3/2),
     * π (1/4 + ln(7 + 4√3) / (16√3)) = 1.0840, the datasheet value.
     */
    public static final double HEAD_AREA_FACTOR = Math.PI * (.25 + DeterministicMath.log(7 + 4 * Math.sqrt(3)) / (16 * Math.sqrt(3)));

    public enum Kind {
        DRUM, COLUMN
    }

    public enum Orientation {
        VERTICAL, HORIZONTAL;

        public boolean horizontal() {
            return this == HORIZONTAL;
        }
    }

    /** A horizontal world direction: north is -z, east +x. */
    public enum Heading {
        NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

        public final int dx, dz;

        Heading(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public static Heading of(int dx, int dz) {
            for (var h : values()) if (h.dx == dx && h.dz == dz) return h;
            throw new IllegalArgumentException("Not a horizontal direction: " + dx + ", " + dz);
        }

        /** Quarter turns counter-clockwise seen from above from {@code base} to this heading. */
        int turnsFrom(Heading base) {
            // Counter-clockwise from above: north, west, south, east.
            int[] order = {0, 3, 2, 1};
            return Math.floorMod(order[ordinal()] - order[base.ordinal()], 4);
        }

        public Heading counterClockwise() {
            return switch (this) {
                case NORTH -> WEST;
                case WEST -> SOUTH;
                case SOUTH -> EAST;
                case EAST -> NORTH;
            };
        }
    }

    /** What a nozzle is for, and the fluid port its position makes it (owner decision 2026-10-16: fixed class by position). */
    public enum Role {
        VAPOUR_OUTLET(PortClass.VAPOUR), INLET(PortClass.BULK), LIQUID_OUTLET(PortClass.LIQUID), FEED(PortClass.BULK), REFLUX(PortClass.BULK),
        DRAW(PortClass.BULK), RETURN(PortClass.BULK), STEAM(PortClass.BULK), BOTTOMS(PortClass.LIQUID);

        public final PortClass port;

        Role(PortClass port) {
            this.port = port;
        }
    }

    /** The phase a nozzle draws first: the top outlet's vapour, a bottom outlet's liquid, or the bulk. */
    public enum PortClass {
        VAPOUR, BULK, LIQUID
    }

    /**
     * A nozzle in the vessel frame: its neck runs from {@code base} (inside the vessel, under its surface) along
     * {@code direction} (a frame axis) for {@code length} metres to its flange face, which lies on a block face. Its
     * outside diameter follows the connected pipe up to {@code maxOd}; unconnected it shows {@code defaultOd} under a blind
     * flange.
     */
    public record Nozzle(Role role, double[] base, int axis, int sign, double length, double maxOd, double defaultOd) {
        public double[] direction() {
            var d = new double[3];
            d[axis] = sign;
            return d;
        }

        /** The centre of the flange face, in the vessel frame. */
        public double[] face() {
            var f = base.clone();
            f[axis] += sign * length;
            return f;
        }
    }

    /** A bare vertical drum resting on its lower head, front to the north: the debug command's form. */
    public EquipmentShape(double diameter, double tangentLength) {
        this(Kind.DRUM, diameter, tangentLength, Orientation.VERTICAL, Heading.NORTH, 0, null);
    }

    public static EquipmentShape drum(double diameter, double tangentLength, Orientation orientation, Heading front, int support) {
        return new EquipmentShape(Kind.DRUM, diameter, tangentLength, orientation, front, support, null);
    }

    public static EquipmentShape column(double diameter, ColumnTrays trays, Heading front, int skirt) {
        return new EquipmentShape(Kind.COLUMN, diameter, trays.tangentLength(diameter), Orientation.VERTICAL, front, skirt, trays);
    }

    public EquipmentShape {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(front, "front");
        double maximum = kind == Kind.COLUMN ? MAX_COLUMN_DIAMETER : MAX_DIAMETER;
        if (!(diameter >= MIN_DIAMETER && diameter <= maximum) || diameter * 2 != Math.rint(diameter * 2))
            throw new IllegalArgumentException("Diameter must be 0.5 to " + (int) maximum + " m in 0.5 m steps, not " + diameter);
        if (!(tangentLength >= 1 && tangentLength <= MAX_TANGENT_LENGTH) || tangentLength != Math.rint(tangentLength))
            throw new IllegalArgumentException("Tangent length must be 1 to 64 whole metres, not " + tangentLength);
        if (kind == Kind.COLUMN) {
            if (trays == null || orientation.horizontal()) throw new IllegalArgumentException("A column stands, laid out by its trays");
            if (tangentLength != trays.tangentLength(diameter)) throw new IllegalArgumentException("A column's length follows its trays");
            if (support < minimumSkirt(diameter) || support > MAX_SUPPORT)
                throw new IllegalArgumentException("Skirt height must be " + minimumSkirt(diameter) + " to " + MAX_SUPPORT + " m, not " + support);
        } else if (trays != null) throw new IllegalArgumentException("A drum has no trays");
        if (orientation.horizontal() && (support < 1 || support > MAX_SUPPORT))
            throw new IllegalArgumentException("Saddle height must be 1 to " + MAX_SUPPORT + " m, not " + support);
        if (!orientation.horizontal() && support != 0 && (support < minimumSkirt(diameter) || support > MAX_SUPPORT))
            throw new IllegalArgumentException("Skirt height must be 0 (none) or " + minimumSkirt(diameter) + " to " + MAX_SUPPORT + " m, not " + support);
    }

    /** The lowest skirt that still encloses the bottom head, m. */
    public static int minimumSkirt(double diameter) {
        return (int) Math.ceil(diameter / 4);
    }

    /**
     * The support a new design proposes: a skirt leaving at least 1 m under the bottom head for the outlet piping and the
     * bottoms pump's suction head (2 m for D 2, 3 m for D 6, 4 m for D 10); saddles 1 m high.
     */
    public static int defaultSupport(double diameter, Orientation orientation) {
        return orientation.horizontal() ? 1 : (int) Math.ceil(diameter / 4 + 1);
    }

    public boolean column() {
        return kind == Kind.COLUMN;
    }

    public boolean skirt() {
        return orientation == Orientation.VERTICAL && support > 0;
    }

    public boolean saddles() {
        return orientation.horizontal();
    }

    public double radius() {
        return diameter / 2;
    }

    public double headDepth() {
        return diameter / 4;
    }

    /** Radius of the outside surface: a column's cladding over its insulation, a drum's shell. */
    public double outerRadius() {
        return radius() + (column() ? INSULATION : 0);
    }

    /** Depth of the outside of the top head over the top tangent line. */
    public double outerHeadDepth() {
        return headDepth() + (column() ? INSULATION : 0);
    }

    /** Shell facets: a multiple of four (the vessel is symmetric about both axes across it), each at most about 1/3 m (a column's 1/4 m). */
    public int facets() {
        return Math.max(12, 4 * (int) Math.ceil((column() ? 1 : .75) * Math.PI * diameter));
    }

    /** Latitude bands of each head. */
    public int bands() {
        return facets() / 4;
    }

    /** Length of the vessel from apex to apex, m: tangent length plus two head depths. */
    public double overallLength() {
        return tangentLength + diameter / 2;
    }

    /** Height of the vessel's top above the ground, m. */
    public double topElevation() {
        return orientation.horizontal() ? support + axis() + radius() : top();
    }

    /** Design volume of the smooth vessel, m³: the cylinder plus two 2:1 heads of π D³/24 each. Supports and nozzles hold no volume. */
    public double volume() {
        return Math.PI * diameter * diameter / 4 * tangentLength + Math.PI * diameter * diameter * diameter / 12;
    }

    /** Outside area of the smooth vessel, m²: the cylinder plus two 2:1 heads (supports and nozzles excluded). */
    public double surfaceArea() {
        return Math.PI * diameter * tangentLength + 2 * HEAD_AREA_FACTOR * diameter * diameter;
    }

    /**
     * The height over which the vessel's liquid level stands, m, for its liquid head {@code g m_c H / V}: a vertical
     * vessel's apex-to-apex length, a horizontal one's diameter.
     */
    public double levelHeight() {
        return orientation.horizontal() ? diameter : overallLength();
    }

    /**
     * Cells across the vessel: the shell, a skirt's base ring, or a column's platforms and their handrails, rounded up to an
     * odd count so the axis runs through cell centres.
     */
    public int width() {
        int cells = column() ? (int) Math.ceil(2 * (ColumnStructure.minimumDeckRadius(this) + .1)) : (int) Math.ceil(diameter + (skirt() ? 2 * BASE_RING : 0));
        return cells % 2 == 0 ? cells + 1 : cells;
    }

    /**
     * Anchor-bolt chairs around a skirt's base ring, about one per 1.5 m of circumference in fours; none under 1.5 m,
     * where a chair would be wider than a shell facet.
     */
    public int anchorChairs() {
        return skirt() && diameter >= 1.5 ? 4 * (int) Math.max(1, Math.round(Math.PI * diameter / 6)) : 0;
    }

    /** Size of the vessel's box in cells along world x. */
    public int sizeX() {
        return box()[0];
    }

    /** Size of the vessel's box in cells along world y. */
    public int sizeY() {
        return box()[1];
    }

    /** Size of the vessel's box in cells along world z. */
    public int sizeZ() {
        return box()[2];
    }

    private int[] box() {
        var t = transform();
        var f = frameBox();
        double[] a = t.point(new double[]{f[0], f[1], f[2]}), b = t.point(new double[]{f[3], f[4], f[5]});
        return new int[]{(int) Math.round(Math.abs(b[0] - a[0])), (int) Math.round(Math.abs(b[1] - a[1])), (int) Math.round(Math.abs(b[2] - a[2]))};
    }

    /**
     * The frame-to-box transform: a quarter-turn rotation (horizontal vessels: frame x up, frame y along world -x before
     * the turn, so the front head faces west) turning the front to {@link #front()}, then a shift so the box starts at 0.
     */
    public Mesh.Transform transform() {
        Mesh.Transform base = orientation.horizontal() ? new Mesh.Transform(new int[]{0, 1, 0, 1, 0, 0, 0, 0, -1}, new double[3]) : Mesh.Transform.IDENTITY;
        var turned = Mesh.Transform.yaw(front.turnsFrom(orientation.horizontal() ? Heading.WEST : Heading.NORTH), 0, 0, 0).after(base);
        var f = frameBox();
        double[] a = turned.point(new double[]{f[0], f[1], f[2]}), b = turned.point(new double[]{f[3], f[4], f[5]});
        var shift = new Mesh.Transform(Mesh.Transform.IDENTITY.matrix(), new double[]{-Math.min(a[0], b[0]), -Math.min(a[1], b[1]), -Math.min(a[2], b[2])});
        return shift.after(turned);
    }

    /**
     * The box in the vessel frame, {x0, y0, z0, x1, y1, z1}: the vessel and its supports (a column's up to its lifting
     * davit), widened to every nozzle's flange face.
     */
    double[] frameBox() {
        double x0 = orientation.horizontal() ? -support : 0, x1 = width(), y0 = 0, z0 = 0, z1 = width();
        double y1 = column() ? Math.max(axialCells(), Math.ceil(ColumnStructure.plan(this).davitTop())) : axialCells();
        for (var nozzle : nozzles()) {
            var face = nozzle.face();
            switch (nozzle.axis * 2 + (nozzle.sign > 0 ? 1 : 0)) {
                case 0 -> x0 = Math.min(x0, face[0]);
                case 1 -> x1 = Math.max(x1, face[0]);
                case 2 -> y0 = Math.min(y0, face[1]);
                case 3 -> y1 = Math.max(y1, face[1]);
                case 4 -> z0 = Math.min(z0, face[2]);
                default -> z1 = Math.max(z1, face[2]);
            }
        }
        return new double[]{x0, y0, z0, x1, y1, z1};
    }

    /** The vessel's nozzles in its frame: a drum's standard set, or a column's from its trays. */
    public List<Nozzle> nozzles() {
        var out = new ArrayList<Nozzle>();
        double a = axis(), r = radius(), bT = bottomTangent(), tT = topTangent(), apothem = r * DeterministicMath.cos(Math.PI / facets());
        if (column()) {
            ColumnStructure.nozzles(this, out);
            return out;
        }
        if (!orientation.horizontal()) {
            // Top head centre: the vapour outlet.
            double topOd = Math.min(1, diameter / 2);
            out.add(new Nozzle(Role.VAPOUR_OUTLET, new double[]{a, headSurface(topOd / 2) - .03, a}, 1, 1,
                    Math.ceil(top() + STAND_OUT) - (headSurface(topOd / 2) - .03), topOd, clampOd(.2 * diameter, topOd)));
            // Liquid outlet: the lowest cell centre at least 0.25 m above the bottom tangent, facing the front (frame -z).
            double yL = Math.ceil(bT + .25 - .5) + .5;
            double liquidOd = Math.min(Math.min(1, diameter / 2), 2 * (Math.min(yL - bT, tT - yL) - .05));
            if (yL < tT && liquidOd > .05)
                out.add(side(Role.LIQUID_OUTLET, a, yL, 2, -1, apothem, liquidOd, clampOd(.1 * diameter, liquidOd)));
            // Inlet: in the upper shell, 0.4 D below the top tangent, facing the right (frame -x).
            double yI = Math.floor(Math.max(bT + .25, tT - .4 * diameter)) + .5;
            if (yI > tT - .1) yI -= 1;
            double inletOd = Math.min(Math.min(1, diameter / 2), 2 * (Math.min(tT - yI, yI - bT) - .05));
            if (yI > bT && inletOd > .05) out.add(side(Role.INLET, a, yI, 0, -1, apothem, inletOd, clampOd(.15 * diameter, inletOd)));
            return out;
        }
        // Horizontal: two vapour nozzles on top (frame +x) near the ends, a liquid outlet low on the front head (frame -y).
        double topOd = Math.min(1, diameter / 2);
        double near = Math.ceil(bT) + .5, far = Math.floor(tT - 1) + .5;
        // A drum too short for two: one outlet at the cell centre nearest its middle.
        if (far < near) far = near = Math.floor((bT + tT) / 2) + .5;
        double rise = Math.ceil(a + r + STAND_OUT) - (Math.sqrt(apothem * apothem - topOd * topOd / 4) + a - .03);
        out.add(new Nozzle(Role.VAPOUR_OUTLET, new double[]{Math.sqrt(apothem * apothem - topOd * topOd / 4) + a - .03, far, a}, 0, 1, rise, topOd, clampOd(.2 * diameter, topOd)));
        if (far - near >= 1.5)
            out.add(new Nozzle(Role.INLET, new double[]{Math.sqrt(apothem * apothem - topOd * topOd / 4) + a - .03, near, a}, 0, 1, rise, topOd, clampOd(.15 * diameter, topOd)));
        // Low on the front head: the lowest cell centre at least 0.3 m above the shell's bottom.
        double xH = Math.ceil(a - r + .3 - .5) + .5, e = a - xH;
        double headOd = Math.min(Math.min(1, diameter / 2), 2 * (.85 * r - Math.abs(e)));
        if (headOd > .05) {
            double edge = (Math.abs(e) + headOd / 2) / r, depth = headDepth() * Math.sqrt(Math.max(0, 1 - edge * edge));
            double start = bT - depth + .03;
            out.add(new Nozzle(Role.LIQUID_OUTLET, new double[]{xH, start, a}, 1, -1, start - Math.floor(-STAND_OUT), headOd, clampOd(.1 * diameter, headOd)));
        }
        return out;
    }

    /** A radial nozzle on the cylindrical shell (a column's cladding) at height {@code y}, along frame axis {@code axis} in direction {@code sign}. */
    Nozzle side(Role role, double a, double y, int axis, int sign, double apothem, double maxOd, double defaultOd) {
        double start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double surface = a + sign * outerRadius(), plane = sign > 0 ? Math.ceil(surface + STAND_OUT) : Math.floor(surface - STAND_OUT);
        var base = new double[]{a, y, a};
        base[axis] = a + sign * start;
        return new Nozzle(role, base, axis, sign, Math.abs(plane - base[axis]), maxOd, defaultOd);
    }

    private static double clampOd(double od, double max) {
        return Math.max(Math.min(.1, max), Math.min(od, max));
    }

    /** The outside of the top head at radial distance {@code r} from the axis (the smooth 2:1 ellipsoid, a column's clad one). */
    double headSurface(double r) {
        double outer = outerRadius();
        return topTangent() + outerHeadDepth() * Math.sqrt(Math.max(0, 1 - (r / outer) * (r / outer)));
    }

    // The vessel's own frame: axis along +y at x = z = axis(), from y = 0 (bottom apex or skirt base) to y = top().

    int axialCells() {
        return (int) Math.ceil(top());
    }

    double axis() {
        return width() / 2.0;
    }

    double bottomTangent() {
        return skirt() ? support : headDepth();
    }

    double topTangent() {
        return bottomTangent() + tangentLength;
    }

    /** The top of the vessel's outside: the apex of its top head (a column's clad head). */
    double top() {
        return topTangent() + outerHeadDepth();
    }

    /** Axial centres of the saddles, or none. */
    double[] saddleCentres() {
        return saddles() ? new double[]{bottomTangent() + SADDLE_POSITION * tangentLength, topTangent() - SADDLE_POSITION * tangentLength} : new double[0];
    }
}
