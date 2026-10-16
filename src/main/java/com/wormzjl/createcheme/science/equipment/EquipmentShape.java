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
 * 2026-10-15) and is a fluid reservoir whose nozzles are its ports. Two are fixed: the vapour outlet (a standing drum's at
 * the top head's centre, a lying drum's on top near its far end) and the liquid outlet (a skirted drum's bottom outlet,
 * whose line drops from the bottom head inside the skirt and leaves through the access opening towards the front, owner
 * report 2026-10-16; without room under the head a nozzle just above the bottom tangent line facing the front; a lying
 * drum's low on its front head). The rest are the player's side {@link Connection}s, up to {@link #MAX_CONNECTIONS}
 * (owner request 2026-10-16), a new drum's being its former standard inlet. A <b>column</b> is a tall vertical vessel
 * laid out by its trays ({@link ColumnTrays}), insulated and clad, with its platforms, ladders, manways and process
 * nozzles at fixed sides ({@link ColumnStructure}); the player places its condenser, drum, reboiler and pumps and runs
 * the pipes (owner decisions 2026-10-16). A <b>unit</b> (heat exchanger, kettle reboiler, air cooler, steam ejector;
 * owner request 2026-10-16) is built from its parts, not as a vessel of revolution ({@link ProcessUnit}): {@code diameter},
 * {@code tangentLength} and {@code support} are its own sizes there. Columns and units are drawn only: their nozzles take
 * the connected pipe's size, but they are not fluid devices yet.
 *
 * <p>Supports. A vertical vessel stands on a skirt: a cylinder of the shell diameter from the ground to the bottom tangent
 * line, {@code support} metres high (0 = none: the vessel rests on its lower head, the debug command's bare form). The
 * skirt is a {@link #SKIRT_WALL} plate showing its inside (the bottom head, a column's bare under its cladding, the outlet
 * line, a floor plate) through its outlet's port and three slots. The skirt stands on a base ring projecting
 * {@link #BASE_RING} beyond it, with {@link #anchorChairs()} anchor-bolt chairs. A horizontal vessel lies on two saddles centred {@link #SADDLE_POSITION} ×
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
                             @Nullable ColumnTrays trays, List<Connection> connections, ProcessUnit.Bank bank) {
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
    /**
     * The lowest row a skirted vessel's bottom outlet line leaves the skirt on, through the centre of its skirt port: the
     * lowest cell centre. A taller skirt lifts the line until it carries its full size ({@link #openingHeight}).
     */
    public static final double SKIRT_OUTLET = .5;
    /**
     * A drum's skirt port, m: its bore leaves this much round the line at the line's drawn size, and a collar this wide
     * rings the hole cut for the largest line (owner request 2026-10-16: the port scales with the line).
     */
    public static final double PORT_CLEARANCE = .05, PORT_COLLAR = 1 / 32.0;
    /** A drum's skirt plate, m (a texel), drawn on both faces: its inside shows through the opening and the slots. */
    public static final double SKIRT_WALL = 1 / 16.0;
    /** The most side connections a drum takes: with its two fixed outlets, every nozzle's size fits in a cell key. */
    public static final int MAX_CONNECTIONS = 6;
    /**
     * The frame directions a side connection faces, as {axis, sign}: -z, +x, +z, -x. A standing drum's front (towards
     * the constructor), left, back and right; a lying drum's sides and its top (+x) and bottom (-x).
     */
    static final int[][] SIDES = {{2, -1}, {0, 1}, {2, 1}, {0, -1}};
    /** The number of sides a connection may face. */
    public static final int CONNECTION_SIDES = SIDES.length;
    /** A column's insulation under its aluminium cladding, m (photo survey: 50-150 mm); a drum is bare. */
    public static final double INSULATION = .1;
    /**
     * Outside area of one 2:1 head over D²: half an oblate spheroid of semi-axes D/2 and D/4 (eccentricity √3/2),
     * π (1/4 + ln(7 + 4√3) / (16√3)) = 1.0840, the datasheet value.
     */
    public static final double HEAD_AREA_FACTOR = Math.PI * (.25 + DeterministicMath.log(7 + 4 * Math.sqrt(3)) / (16 * Math.sqrt(3)));

    public enum Kind {
        DRUM, COLUMN, EXCHANGER, KETTLE, AIR_COOLER, EJECTOR;

        /** A standalone unit, built from its parts ({@link ProcessUnit}); a drum and a column are vessels of revolution. */
        public boolean unit() {
            return ordinal() >= EXCHANGER.ordinal();
        }
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

    /**
     * What a nozzle is for, and the fluid port its position makes it (owner decision 2026-10-16: fixed class by position):
     * a drum's, a column's, then a unit's (tube side and shell side of an exchanger or a kettle's channel, an air cooler's
     * process line, an ejector's suction, motive steam and discharge).
     */
    public enum Role {
        VAPOUR_OUTLET(PortClass.VAPOUR), SIDE(PortClass.BULK), LIQUID_OUTLET(PortClass.LIQUID), FEED(PortClass.BULK), REFLUX(PortClass.BULK),
        DRAW(PortClass.BULK), PUMPAROUND_DRAW(PortClass.BULK), RETURN(PortClass.BULK), STEAM(PortClass.BULK), BOTTOMS(PortClass.LIQUID),
        VAPOUR_RETURN(PortClass.BULK), TUBE_INLET(PortClass.BULK), TUBE_OUTLET(PortClass.BULK), SHELL_INLET(PortClass.BULK), SHELL_OUTLET(PortClass.BULK),
        PROCESS_INLET(PortClass.BULK), PROCESS_OUTLET(PortClass.BULK), SUCTION(PortClass.BULK), MOTIVE(PortClass.BULK), DISCHARGE(PortClass.BULK);

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
     * flange and outer end take the connected pipe's size, whatever it is, so a pipe never steps at the flange (owner
     * decision 2026-10-16: resizing starts at the port-to-vessel connection); unconnected it shows {@code defaultOd} under
     * a blind flange. The neck takes the pipe's size up to {@code maxOd}, the largest the vessel carries there; a larger
     * pipe's neck widens in a concentric reducer from {@code exit}, the distance from {@code base} where the neck at
     * {@code maxOd} has left the vessel (a skirted bottom outlet: its skirt), to the flange. A line coming down from the
     * vessel ({@code riser} > 0, a skirted drum's bottom outlet) rises to frame height {@code riser} at {@code maxOd} behind
     * {@code base}, turning into the neck through an elbow of bend radius 1 D of the line drawn and a straight run.
     */
    public record Nozzle(Role role, double[] base, int axis, int sign, double length, double maxOd, double defaultOd, double riser, double exit) {
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

    /**
     * A side connection the player places on a drum's shell (owner request 2026-10-16): a radial nozzle facing frame
     * direction {@link #SIDES}[{@code side}], its centre line at frame height {@code station} + 0.5 along the vessel's
     * axis and {@code offset} whole cells across it, along the frame axis that is neither the vessel's nor its own (a
     * hillside nozzle when not 0). Its ranges follow the drum ({@link #stations}, {@link #maximumOffset}).
     */
    public record Connection(int side, int station, int offset) {
        public Connection {
            if (side < 0 || side >= SIDES.length) throw new IllegalArgumentException("A connection faces side 0 to 3, not " + side);
        }

        public int axis() {
            return SIDES[side][0];
        }

        public int sign() {
            return SIDES[side][1];
        }

        /** The frame axis the offset runs along. */
        public int across() {
            return 2 - axis();
        }

        /** Encoded for NBT and the network: side, station and offset per connection. */
        public static int[] encode(List<Connection> connections) {
            var out = new int[3 * connections.size()];
            for (int i = 0; i < connections.size(); i++) {
                var c = connections.get(i);
                out[3 * i] = c.side;
                out[3 * i + 1] = c.station;
                out[3 * i + 2] = c.offset;
            }
            return out;
        }

        /** @throws IllegalArgumentException for a malformed array or side; the ranges are checked with the shape */
        public static List<Connection> decode(int[] encoded) {
            if (encoded.length % 3 != 0 || encoded.length > 3 * MAX_CONNECTIONS)
                throw new IllegalArgumentException("Connections need three numbers each, at most " + MAX_CONNECTIONS);
            var out = new ArrayList<Connection>(encoded.length / 3);
            for (int i = 0; i < encoded.length; i += 3) out.add(new Connection(encoded[i], encoded[i + 1], encoded[i + 2]));
            return List.copyOf(out);
        }
    }

    /**
     * A side connection that cannot be built as placed: connection {@code connection} meets {@code other}, another
     * connection's index or one of {@link #VAPOUR}, {@link #LIQUID}, {@link #SADDLE}, {@link #GROUND}.
     */
    public record Clash(int connection, int other) {
        public static final int VAPOUR = -1, LIQUID = -2, SADDLE = -3, GROUND = -4;
    }

    /** A bare vertical drum resting on its lower head, front to the north: the debug command's form. */
    public EquipmentShape(double diameter, double tangentLength) {
        this(Kind.DRUM, diameter, tangentLength, Orientation.VERTICAL, Heading.NORTH, 0, null, defaultConnections(diameter, tangentLength, Orientation.VERTICAL, 0), ProcessUnit.Bank.ONE);
    }

    /** A drum with a new drum's side connection ({@link #defaultConnections}). */
    public static EquipmentShape drum(double diameter, double tangentLength, Orientation orientation, Heading front, int support) {
        return drum(diameter, tangentLength, orientation, front, support, defaultConnections(diameter, tangentLength, orientation, support));
    }

    public static EquipmentShape drum(double diameter, double tangentLength, Orientation orientation, Heading front, int support, List<Connection> connections) {
        return new EquipmentShape(Kind.DRUM, diameter, tangentLength, orientation, front, support, null, connections, ProcessUnit.Bank.ONE);
    }

    public static EquipmentShape column(double diameter, ColumnTrays trays, Heading front, int skirt) {
        return new EquipmentShape(Kind.COLUMN, diameter, trays.tangentLength(diameter), Orientation.VERTICAL, front, skirt, trays, List.of(), ProcessUnit.Bank.ONE);
    }

    /** A standalone unit of kind {@code kind}, its sizes as {@link ProcessUnit} reads them; it lies or stands as its kind does. */
    public static EquipmentShape unit(Kind kind, double diameter, double tangentLength, Heading front, int support) {
        return unit(kind, diameter, tangentLength, front, support, ProcessUnit.Bank.ONE);
    }

    /** A unit as {@link #unit(Kind, double, double, Heading, int)}; an air cooler built as a bank of bays. */
    public static EquipmentShape unit(Kind kind, double diameter, double tangentLength, Heading front, int support, ProcessUnit.Bank bank) {
        if (!kind.unit()) throw new IllegalArgumentException(kind + " is not a unit");
        return new EquipmentShape(kind, diameter, tangentLength, ProcessUnit.orientation(kind), front, support, null, List.of(), bank);
    }

    /**
     * A new drum's side connections: its former standard inlet, a standing drum's in the upper shell (0.4 D below the
     * top tangent line) facing the right, a lying drum's on top near its front end when the drum is long enough for it
     * beside the vapour outlet.
     */
    public static List<Connection> defaultConnections(double diameter, double tangentLength, Orientation orientation, int support) {
        double bT = orientation.horizontal() || support == 0 ? diameter / 4 : support, tT = bT + tangentLength;
        if (orientation.horizontal()) {
            double near = Math.ceil(bT) + .5, far = Math.floor(tT - 1) + .5;
            return far - near >= 1.5 ? List.of(new Connection(1, (int) Math.floor(near), 0)) : List.of();
        }
        double y = Math.floor(Math.max(bT + .25, tT - .4 * diameter)) + .5;
        if (y > tT - .1) y -= 1;
        return y > bT + .1 ? List.of(new Connection(3, (int) Math.floor(y), 0)) : List.of();
    }

    public EquipmentShape {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(front, "front");
        Objects.requireNonNull(bank, "bank");
        connections = List.copyOf(connections);
        if (kind.unit()) {
            if (trays != null || !connections.isEmpty()) throw new IllegalArgumentException("A unit has no trays and no side connections");
            ProcessUnit.validate(kind, diameter, tangentLength, orientation, support, bank);
        } else {
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
            if (!bank.equals(ProcessUnit.Bank.ONE)) throw new IllegalArgumentException("Only air coolers are built in banks");
            if (kind == Kind.COLUMN && !connections.isEmpty()) throw new IllegalArgumentException("A column's nozzles follow its trays");
            if (connections.size() > MAX_CONNECTIONS) throw new IllegalArgumentException("A drum takes at most " + MAX_CONNECTIONS + " side connections");
            int[] range = stations(diameter, tangentLength, orientation, support);
            int across = maximumOffset(diameter);
            for (var c : connections) {
                if (c.station() < range[0] || c.station() > range[1])
                    throw new IllegalArgumentException("A side connection stands at row " + range[0] + " to " + range[1] + ", not " + c.station());
                if (Math.abs(c.offset()) > across) throw new IllegalArgumentException("A side connection's offset is at most " + across + ", not " + c.offset());
            }
        }
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

    public boolean drum() {
        return kind == Kind.DRUM;
    }

    public boolean unit() {
        return kind.unit();
    }

    /** A vessel's skirt; a unit's supports are its own parts. */
    public boolean skirt() {
        return !unit() && orientation == Orientation.VERTICAL && support > 0;
    }

    /** A lying drum's two saddles; a unit's supports are its own parts. */
    public boolean saddles() {
        return drum() && orientation.horizontal();
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

    /** Length of the vessel from apex to apex, m: tangent length plus two head depths; a unit's along its frame z. */
    public double overallLength() {
        return unit() ? ProcessUnit.of(this).length() : tangentLength + diameter / 2;
    }

    /** Height of the vessel's top above the ground, m. */
    public double topElevation() {
        return unit() ? ProcessUnit.of(this).height() : orientation.horizontal() ? support + axis() + radius() : top();
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
     * The frame-to-box transform: a quarter-turn rotation (a lying drum: frame x up, frame y along world -x before the
     * turn, so the front head faces west; a unit's frame stands, as a vertical vessel's, its front frame -z) turning the
     * front to {@link #front()}, then a shift so the box starts at 0.
     */
    public Mesh.Transform transform() {
        Mesh.Transform base = saddles() ? new Mesh.Transform(new int[]{0, 1, 0, 1, 0, 0, 0, 0, -1}, new double[3]) : Mesh.Transform.IDENTITY;
        var turned = Mesh.Transform.yaw(front.turnsFrom(saddles() ? Heading.WEST : Heading.NORTH), 0, 0, 0).after(base);
        var f = frameBox();
        double[] a = turned.point(new double[]{f[0], f[1], f[2]}), b = turned.point(new double[]{f[3], f[4], f[5]});
        var shift = new Mesh.Transform(Mesh.Transform.IDENTITY.matrix(), new double[]{-Math.min(a[0], b[0]), -Math.min(a[1], b[1]), -Math.min(a[2], b[2])});
        return shift.after(turned);
    }

    /**
     * The box in the vessel frame, {x0, y0, z0, x1, y1, z1}: the vessel and its supports (a column's up to its lifting
     * davit), widened to every nozzle's flange face, in whole cells; a unit's from its parts ({@link ProcessUnit#box()}).
     */
    double[] frameBox() {
        if (unit()) return ProcessUnit.of(this).box();
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

    /** The nozzles in the frame: a drum's standard set, a column's from its trays, a unit's own. */
    public List<Nozzle> nozzles() {
        if (unit()) return ProcessUnit.of(this).nozzles();
        var out = new ArrayList<Nozzle>();
        double a = axis(), r = radius(), bT = bottomTangent(), tT = topTangent(), apothem = r * DeterministicMath.cos(Math.PI / facets());
        if (column()) {
            ColumnStructure.nozzles(this, out);
            return out;
        }
        if (!orientation.horizontal()) {
            // Top head centre: the vapour outlet.
            double topOd = Math.min(1, diameter / 2);
            double topBase = headSurface(topOd / 2) - .03;
            out.add(new Nozzle(Role.VAPOUR_OUTLET, new double[]{a, topBase, a}, 1, 1, Math.ceil(top() + STAND_OUT) - topBase, topOd, clampOd(.2 * diameter, topOd), 0,
                    headSurface(0) - topBase));
            // Liquid outlet. On a skirt it is the bottom outlet (owner report 2026-10-16), through the skirt's port.
            var bottom = skirtOutlet(Role.LIQUID_OUTLET, .1 * diameter);
            if (bottom != null) out.add(bottom);
            else {
                // Without room under the head: the lowest cell centre at least 0.25 m above the bottom tangent, facing the front (frame -z).
                double yL = Math.ceil(bT + .25 - .5) + .5;
                double liquidOd = Math.min(Math.min(1, diameter / 2), 2 * (Math.min(yL - bT, tT - yL) - .05));
                if (yL < tT && liquidOd > .05)
                    out.add(side(Role.LIQUID_OUTLET, a, yL, 2, -1, apothem, liquidOd, clampOd(.1 * diameter, liquidOd)));
            }
            for (var c : connections) out.add(connection(c, a, bT, tT, apothem));
            return out;
        }
        // Horizontal: the vapour outlet on top (frame +x) near the far end, a liquid outlet low on the front head (frame -y).
        double topOd = Math.min(1, diameter / 2);
        // A drum too short for a connection beside it: the outlet at the cell centre nearest its middle.
        double far = Math.floor(tT - 1) + .5;
        if (far < Math.ceil(bT) + .5) far = Math.floor((bT + tT) / 2) + .5;
        double rise = Math.ceil(a + r + STAND_OUT) - (Math.sqrt(apothem * apothem - topOd * topOd / 4) + a - .03);
        double topStart = Math.sqrt(apothem * apothem - topOd * topOd / 4) - .03;
        out.add(new Nozzle(Role.VAPOUR_OUTLET, new double[]{topStart + a, far, a}, 0, 1, rise, topOd, clampOd(.2 * diameter, topOd), 0, r - topStart));
        // Low on the front head: the lowest cell centre at least 0.3 m above the shell's bottom.
        double xH = Math.ceil(a - r + .3 - .5) + .5, e = a - xH;
        double headOd = Math.min(Math.min(1, diameter / 2), 2 * (.85 * r - Math.abs(e)));
        if (headOd > .05) {
            double edge = (Math.abs(e) + headOd / 2) / r, depth = headDepth() * Math.sqrt(Math.max(0, 1 - edge * edge));
            double start = bT - depth + .03;
            double nearest = Math.max(0, Math.abs(e) - headOd / 2) / r, bulge = headDepth() * Math.sqrt(Math.max(0, 1 - nearest * nearest));
            out.add(new Nozzle(Role.LIQUID_OUTLET, new double[]{xH, start, a}, 1, -1, start - Math.floor(-STAND_OUT), headOd, clampOd(.1 * diameter, headOd), 0,
                    bulge - depth + .03));
        }
        for (var c : connections) out.add(connection(c, a, bT, tT, apothem));
        return out;
    }

    /**
     * Side connection {@code c} as a nozzle. Its largest size keeps its edge 0.05 m inside the tangent lines and within
     * 0.85 of the radius across; its neck starts under the shell at its edge, its flange face on the next block face.
     */
    private Nozzle connection(Connection c, double a, double bT, double tT, double apothem) {
        double y = c.station() + .5, off = Math.abs(c.offset());
        double od = Math.min(Math.min(1, diameter / 2), 2 * Math.min(Math.min(y - bT, tT - y) - .05, .85 * radius() - off));
        double edge = off + od / 2, start = Math.sqrt(Math.max(0, apothem * apothem - edge * edge)) - .03;
        double surface = a + c.sign() * outerRadius(), plane = c.sign() > 0 ? Math.ceil(surface + STAND_OUT) : Math.floor(surface - STAND_OUT);
        var base = new double[]{a, y, a};
        base[c.axis()] = a + c.sign() * start;
        base[c.across()] = a + c.offset();
        double nearest = Math.max(0, off - od / 2), exit = Math.sqrt(outerRadius() * outerRadius() - nearest * nearest) - start;
        return new Nozzle(Role.SIDE, base, c.axis(), c.sign(), Math.abs(plane - base[c.axis()]), od, clampOd(.15 * diameter, od), 0, exit);
    }

    /**
     * The frame stations (cell rows along the axis) a drum's side connection may take: its centre line at least 0.1 m
     * inside both tangent lines, {lowest, highest}.
     */
    public static int[] stations(double diameter, double tangentLength, Orientation orientation, int support) {
        double bT = orientation.horizontal() || support == 0 ? diameter / 4 : support, tT = bT + tangentLength;
        return new int[]{(int) Math.ceil(bT - .4 - 1e-9), (int) Math.floor(tT - .6 + 1e-9)};
    }

    /** The largest offset of a side connection across the drum, whole cells: a hillside nozzle's edge stays within 0.85 of the radius. */
    public static int maximumOffset(double diameter) {
        return Math.max(0, (int) Math.floor(.85 * diameter / 2 - .05 + 1e-9));
    }

    /**
     * Where the side connections cannot be built as placed: one overlapping another or a fixed outlet with the largest
     * pipe's flange (one block: nozzles on different block lines never meet), one on a lying drum's saddle, or one
     * facing down with no cell for a pipe above the ground. Building refuses a drum with any; the constructor's screen
     * shows the first.
     */
    public List<Clash> clashes() {
        if (column() || connections.isEmpty()) return List.of();
        var all = nozzles();
        int fixed = all.size() - connections.size();
        var out = new ArrayList<Clash>();
        double sr = radius() + PLATE, lateral = sr * DeterministicMath.sin(Math.toRadians(SADDLE_CONTACT_DEGREES / 2)),
                horn = axis() - sr * DeterministicMath.cos(Math.toRadians(SADDLE_CONTACT_DEGREES / 2));
        for (int i = fixed; i < all.size(); i++) {
            var box = reach(all.get(i));
            for (int j = 0; j < i; j++)
                if (overlap(box, reach(all.get(j))))
                    out.add(new Clash(i - fixed, j >= fixed ? j - fixed : all.get(j).role() == Role.VAPOUR_OUTLET ? Clash.VAPOUR : Clash.LIQUID));
            if (!saddles()) continue;
            for (double c : saddleCentres())
                if (overlap(box, new double[]{-support, c - SADDLE_WIDTH / 2, axis() - lateral, horn, c + SADDLE_WIDTH / 2, axis() + lateral}))
                    out.add(new Clash(i - fixed, Clash.SADDLE));
            var n = all.get(i);
            if (n.axis() == 0 && n.sign() < 0 && n.face()[0] - 1 < -support - 1e-9) out.add(new Clash(i - fixed, Clash.GROUND));
        }
        return out;
    }

    /** A nozzle's room with the largest pipe, {x0, y0, z0, x1, y1, z1} in the frame: its neck and flange, and a riser's line. */
    private static double[] reach(Nozzle n) {
        double half = PipeSizes.flange(PipeSizes.MAX_OUTSIDE) / 2;
        var box = new double[6];
        for (int a = 0; a < 3; a++) {
            box[a] = n.base()[a] - half;
            box[a + 3] = n.base()[a] + half;
        }
        double face = n.face()[n.axis()];
        box[n.axis()] = Math.min(n.base()[n.axis()], face);
        box[n.axis() + 3] = Math.max(n.base()[n.axis()], face);
        if (n.riser() > 0) box[4] = Math.max(box[4], n.riser());
        return box;
    }

    private static boolean overlap(double[] a, double[] b) {
        for (int i = 0; i < 3; i++) if (a[i] >= b[i + 3] - 1e-6 || b[i] >= a[i + 3] - 1e-6) return false;
        return true;
    }

    /**
     * A skirted vessel's bottom outlet (a drum's liquid outlet, owner report 2026-10-16; a column's bottoms, owner request
     * 2026-10-16: "output on the skirt like the drum"), or null when the skirt leaves it no room: its line drops from the
     * bottom head's lowest point inside the skirt, turns in an elbow of bend radius maxOd, and leaves through the skirt's
     * port towards the front (frame -z) on the row {@link #skirtOutletLine} picks, which also sizes all three.
     */
    @Nullable Nozzle skirtOutlet(Role role, double defaultOd) {
        var line = skirtOutletLine();
        if (line == null) return null;
        double od = line[1], a = axis(), pole = bottomTangent() - headDepth(), start = a - od, plane = Math.floor(a - outerRadius() - STAND_OUT);
        return new Nozzle(role, new double[]{a, line[0], start}, 2, -1, start - plane, od, clampOd(defaultOd, od), pole + .03, radius() + SKIRT_WALL + .02 - od);
    }

    /**
     * A skirted vessel's bottom outlet line, {run centre height, largest size}, or null when it has none (owner requests
     * 2026-10-16: the line follows the pipe; "1m pipe on the bottom of drum & column still looks weird", decided: the line
     * rises with the skirt). On a row of cell centres, its elbow (bend radius 1 D) stays under the bottom head, and its
     * skirt port, the line's clearance and collar round it, clears the base plate (so its flange clears grade) and the
     * head seam. The line takes the lowest row that carries its full size, half the diameter and at most one block; a
     * skirt too short for that leaves it on the row that carries the largest, and a larger pipe meets a reducer outside.
     */
    private double @Nullable [] skirtOutletLine() {
        if (!skirt()) return null;
        double ring = PORT_CLEARANCE + PORT_COLLAR, pole = bottomTangent() - headDepth(), full = Math.min(diameter / 2, PipeSizes.MAX_OUTSIDE);
        double[] best = null;
        for (double c = SKIRT_OUTLET; c < pole && (best == null || best[1] < full - 1e-9); c++) {
            double od = Math.min(Math.min(full, pole - c - .05), 2 * Math.min(c - PLATE - .01 - ring, bottomTangent() - .1 - c - ring));
            if (od > .05 && (best == null || od > best[1] + 1e-9)) best = new double[]{c, od};
        }
        return best;
    }

    /** Radius of a skirt's port, or 0: cut for its largest bottom outlet line (the port's bore follows the line drawn). */
    public double openingRadius() {
        var line = skirtOutletLine();
        return line != null ? line[1] / 2 + PORT_CLEARANCE : 0;
    }

    /** Centre height of a skirt's port, or 0: on its bottom outlet line. */
    public double openingHeight() {
        var line = skirtOutletLine();
        return line != null ? line[0] : 0;
    }

    /** A radial nozzle on the cylindrical shell (a column's cladding) at height {@code y}, along frame axis {@code axis} in direction {@code sign}. */
    Nozzle side(Role role, double a, double y, int axis, int sign, double apothem, double maxOd, double defaultOd) {
        double start = Math.sqrt(Math.max(0, apothem * apothem - maxOd * maxOd / 4)) - .03;
        double surface = a + sign * outerRadius(), plane = sign > 0 ? Math.ceil(surface + STAND_OUT) : Math.floor(surface - STAND_OUT);
        var base = new double[]{a, y, a};
        base[axis] = a + sign * start;
        return new Nozzle(role, base, axis, sign, Math.abs(plane - base[axis]), maxOd, defaultOd, 0, outerRadius() - start);
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
