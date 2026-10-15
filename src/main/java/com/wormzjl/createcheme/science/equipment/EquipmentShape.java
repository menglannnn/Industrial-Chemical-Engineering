package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.Objects;

/**
 * A process vessel on the block grid (1 block = 1 m): a cylindrical shell of the given diameter and tangent-to-tangent
 * length between two 2:1 semi-ellipsoidal heads (depth D/4 each), standing or lying on its supports. The shell is drawn
 * as a regular N-gon prism whose facets are at most about 1/3 m wide, and each head as N-gon bands at the same angular
 * step (owner decisions D3 and D5 of batch 2026-10-15-custom-equipment-rendering: diameters in 0.5 m steps, lengths in
 * whole metres; D3's 0.5 m facets refined on owner request 2026-10-16).
 *
 * <p>Supports (owner decisions 2026-10-15). A vertical vessel stands on a skirt: a cylinder of the shell diameter from
 * the ground to the bottom tangent line, {@code support} metres high, which hides the bottom head (0 = none: the vessel
 * rests on its lower head, the debug command's bare form). The skirt stands on a base ring projecting
 * {@link #BASE_RING} beyond it, with {@link #anchorChairs()} anchor-bolt chairs. A horizontal vessel lies on two saddles
 * centred {@link #SADDLE_POSITION} × T/T from each tangent line (Zick), cradling the bottom
 * {@link #SADDLE_CONTACT_DEGREES}° of the shell; {@code support} is the height of the vessel's box above the ground. A
 * saddle is built like a real one: a curved top plate around the contact arc, a web plate under it, a rib at each horn
 * and a base plate, all of {@link #SADDLE_WIDTH} along the axis except the web.
 *
 * <p>World coordinates are box-local metres: the vessel and its supports occupy cells [0, sizeX) × [0, sizeY) × [0,
 * sizeZ) and rest on y = 0. {@link ShapeRasterizer} builds the vessel in its own frame, axis along +y, and turns it into
 * the box: cells, surface polygons and collision boxes.
 */
public record EquipmentShape(double diameter, double tangentLength, Orientation orientation, int support) {
    public static final double MIN_DIAMETER = .5, MAX_DIAMETER = 10, MAX_TANGENT_LENGTH = 64;
    public static final int MAX_SUPPORT = 16;
    /** Saddles: centred this fraction of T/T from each tangent line, this wide along the axis (m), over this arc of the shell. */
    public static final double SADDLE_POSITION = .2, SADDLE_WIDTH = .375, SADDLE_CONTACT_DEGREES = 120;
    /**
     * Support parts, m, at one to two texels (1/16 m) so they read at block scale: thin plates (saddle top and base plates,
     * skirt base ring), the saddle's web and rib thickness, the base ring's projection beyond the skirt, and the width and
     * height of an anchor chair.
     */
    public static final double PLATE = 1 / 16.0, SADDLE_WEB = .125, SADDLE_RIB = .125, BASE_RING = .125, CHAIR = .25;
    /**
     * Outside area of one 2:1 head over D²: half an oblate spheroid of semi-axes D/2 and D/4 (eccentricity √3/2),
     * π (1/4 + ln(7 + 4√3) / (16√3)) = 1.0840, the datasheet value.
     */
    public static final double HEAD_AREA_FACTOR = Math.PI * (.25 + DeterministicMath.log(7 + 4 * Math.sqrt(3)) / (16 * Math.sqrt(3)));

    /** The direction of the vessel's axis in the world. */
    public enum Orientation {
        VERTICAL, ALONG_X, ALONG_Z;

        public boolean horizontal() {
            return this != VERTICAL;
        }
    }

    /** A bare vertical vessel resting on its lower head: the debug command's form. */
    public EquipmentShape(double diameter, double tangentLength) {
        this(diameter, tangentLength, Orientation.VERTICAL, 0);
    }

    public EquipmentShape {
        if (!(diameter >= MIN_DIAMETER && diameter <= MAX_DIAMETER) || diameter * 2 != Math.rint(diameter * 2))
            throw new IllegalArgumentException("Diameter must be 0.5 to 10 m in 0.5 m steps, not " + diameter);
        if (!(tangentLength >= 1 && tangentLength <= MAX_TANGENT_LENGTH) || tangentLength != Math.rint(tangentLength))
            throw new IllegalArgumentException("Tangent length must be 1 to 64 whole metres, not " + tangentLength);
        Objects.requireNonNull(orientation, "orientation");
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

    /** Shell facets: a multiple of four (the vessel is symmetric about both axes across it), each at most about 1/3 m. */
    public int facets() {
        return Math.max(12, 4 * (int) Math.ceil(.75 * Math.PI * diameter));
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

    /** Design volume of the smooth vessel, m³: the cylinder plus two 2:1 heads of π D³/24 each. Supports hold no volume. */
    public double volume() {
        return Math.PI * diameter * diameter / 4 * tangentLength + Math.PI * diameter * diameter * diameter / 12;
    }

    /** Outside area of the smooth vessel, m²: the cylinder plus two 2:1 heads (supports excluded). */
    public double surfaceArea() {
        return Math.PI * diameter * tangentLength + 2 * HEAD_AREA_FACTOR * diameter * diameter;
    }

    /** Cells across the vessel: the shell, or a skirt's base ring. */
    public int width() {
        return (int) Math.ceil(diameter + (skirt() ? 2 * BASE_RING : 0));
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
        return orientation == Orientation.ALONG_X ? axialCells() : width();
    }

    /** Size of the vessel's box in cells along world y. */
    public int sizeY() {
        return orientation.horizontal() ? width() + support : axialCells();
    }

    /** Size of the vessel's box in cells along world z. */
    public int sizeZ() {
        return orientation == Orientation.ALONG_Z ? axialCells() : width();
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

    double top() {
        return topTangent() + headDepth();
    }

    /** Axial centres of the saddles, or none. */
    double[] saddleCentres() {
        return saddles() ? new double[]{bottomTangent() + SADDLE_POSITION * tangentLength, topTangent() - SADDLE_POSITION * tangentLength} : new double[0];
    }
}
