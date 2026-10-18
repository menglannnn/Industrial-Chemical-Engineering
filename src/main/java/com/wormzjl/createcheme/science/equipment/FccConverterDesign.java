package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Orientation;

import java.util.List;

/**
 * A side-by-side FCC converter with a single-stage regenerator (UOP and Shell look; {@code NEW_UNITS_SURVEY_FCC_COKER.md}
 * section 1, owner decisions D3 and D4 of 2026-10-18; drawn by {@link FccConverterBuilder}, its numbers in
 * {@link FccConverterLayout}): the regenerator's shell 4 to 17 m across in 0.5 m steps, the riser up to 44 whole metres
 * from its wye to its end inside the reactor (the contact time of 2 to 4 s fixes it, whatever the regenerator), the
 * regenerator's bottom tangent 6 to 14 m up. A short riser or a high regenerator is offered only where both standpipes
 * keep within 45° of vertical and their slide valves find their floors: a larger regenerator's longer stripper needs a
 * longer riser (from 25 m at 4 m to 44 m at 17 m), and its lowest bottom rises with the standpipe's run and the air
 * line's elbow under it (from 8 m at 4 m to 11 m at 17 m).
 *
 * <p>The feed it takes follows the regenerator's dense bed: air at 0.6 to 1 m/s burns the coke the feed lays down, so the
 * bed's area, D², follows the feed rate; about 50,000 barrels a day at 9.6 m (survey).
 */
final class FccConverterDesign extends UnitDesign {
    /** Fresh feed at a 9.6 m regenerator, m³/h: 50,000 barrels a day of 0.159 m³ (survey). */
    static final double FEED_AT_9_6 = 50_000 * .159 / 24;

    @Override
    public double minSize() {
        return 4;
    }

    @Override
    public double maxSize() {
        return 17;
    }

    @Override
    public double sizeStep() {
        return .5;
    }

    @Override
    public int minLength(double size) {
        return FccConverterLayout.minLength(size);
    }

    @Override
    public int maxLength(double size) {
        return FccConverterLayout.MAX_RISER;
    }

    /** A new converter's riser: 32 m (survey default; modern risers 30 to 40 m), or the least that fits a larger regenerator. */
    @Override
    public int defaultLength(double size) {
        return Math.max(minLength(size), Math.min(maxLength(size), 32));
    }

    @Override
    public int minSupport(double size, int length) {
        return Math.min(FccConverterLayout.MAX_BOTTOM, FccConverterLayout.minSupport(size, Math.max(length, minLength(size))));
    }

    @Override
    public int maxSupport() {
        return FccConverterLayout.MAX_BOTTOM;
    }

    @Override
    public Orientation orientation() {
        return Orientation.VERTICAL;
    }

    @Override
    public String sizeRow() {
        return "fcc_converter.regenerator";
    }

    @Override
    public String lengthRow() {
        return "fcc_converter.riser";
    }

    @Override
    public String supportRow() {
        return "fcc_converter.regenerator_bottom";
    }

    /** The fresh feed, m³/h, for a regenerator of {@code d}. */
    static double feed(double d) {
        return FEED_AT_9_6 * d * d / (9.6 * 9.6);
    }

    @Override
    public Line held(EquipmentShape shape) {
        double feed = feed(shape.diameter());
        return new Line("fcc_converter.feed", whole(feed), whole(feed * 24 / .159 / 1000));
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        double d = shape.diameter(), feed = feed(d);
        return List.of(new Line("fcc_converter.regenerator", size(d), number(14 + 1.3 * d)),
                new Line("fcc_converter.reactor", number(.6 * d), number(.4 * d)),
                new Line("fcc_converter.riser", size(PipeSizes.od(.15 * d + .1, .1, 3)), (int) shape.tangentLength()),
                new Line("fcc_converter.feed", whole(feed), whole(feed * 24 / .159 / 1000)));
    }

    @Override
    void build(ProcessUnit unit) {
        new FccConverterBuilder(unit, new FccConverterLayout(unit.shape.diameter(), (int) unit.shape.tangentLength(), unit.shape.support())).build();
    }
}
