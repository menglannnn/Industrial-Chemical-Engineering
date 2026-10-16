package com.wormzjl.createcheme.science.equipment;

/**
 * Drawn proportions of a line and its flanges from its bore, shared by pipes, vessel nozzles and pipeline devices
 * (photo survey 2026-10-16, section 0: ASME B36.10 pipe and B16.5 / B16.47 Class 150 flanges). Sizes are capped at one
 * block (owner request 2026-10-16: pipes capped at 1 m for now).
 */
public final class PipeSizes {
    /** The smallest drawn outside diameter: one texel. */
    public static final double MIN_OUTSIDE = 1 / 16.0;
    /** The largest drawn outside diameter: one block. */
    public static final double MAX_OUTSIDE = 1;

    private PipeSizes() {
    }

    /** Outside diameter of a pipe of bore {@code bore}: the wall adds about a tenth (2" 52.5/60.3 mm, 12" 303/324 mm). */
    public static double outside(double bore) {
        return Math.max(MIN_OUTSIDE, Math.min(MAX_OUTSIDE, 1.1 * bore));
    }

    /**
     * Flange outside diameter: about twice the pipe at 4", 1.5 times at 12", 1.3 times from 24" (1.25 OD + 85 mm fits the
     * table within a few percent), at most one block.
     */
    public static double flange(double outside) {
        return Math.min(1, 1.25 * outside + .085);
    }

    /** The largest pipe whose flange is at most {@code flange} across (the inverse of {@link #flange}). */
    public static double largestUnder(double flange) {
        return (flange - .085) / 1.25;
    }

    /** One flange of a pair: a pair is one texel thick up to 24", two above. */
    public static double flangeThickness(double outside) {
        return outside > .6 ? 1 / 16.0 : 1 / 32.0;
    }

    /** Length of a weld-neck flange's hub taper. */
    public static double hub(double outside) {
        return Math.min(.15, .04 + .25 * outside);
    }

    /** Outside diameter of the hub where it meets the flange. */
    public static double hubOutside(double outside) {
        return Math.min(flange(outside), 1.15 * outside);
    }

    /**
     * Class 150 bolting by the pipe's outside diameter, inches: {largest outside, bolts, bolt diameter} (photo survey
     * EQUIPMENT_DETAILS_PHOTO_SURVEY.md, section C: ASME B16.5 to 24", B16.47 series A to 48").
     */
    private static final double[][] BOLTING = {
            {1.9, 4, .5}, {3.5, 4, .625}, {4.5, 8, .625}, {8.625, 8, .75}, {12.75, 12, .875}, {14, 12, 1}, {16, 16, 1}, {18, 16, 1.125},
            {20, 20, 1.125}, {24, 20, 1.25}, {26, 24, 1.25}, {30, 28, 1.25}, {32, 28, 1.5}, {36, 32, 1.5}, {42, 36, 1.5}, {48, 44, 1.5}};
    private static final double INCH = .0254;

    /** Bolts in a flange of a line of outside diameter {@code outside}, m; beyond 48" as many as keep 48"'s pitch, in fours. */
    public static int bolts(double outside) {
        double inches = outside / INCH;
        for (var row : BOLTING) if (inches <= row[0] * 1.02) return (int) row[1];
        return 4 * (int) Math.ceil(44 * inches / 48 / 4);
    }

    /** Diameter of those bolts, m. */
    public static double boltDiameter(double outside) {
        double inches = outside / INCH;
        for (var row : BOLTING) if (inches <= row[0] * 1.02) return row[2] * INCH;
        return 1.5 * INCH;
    }

    /** Facets around a pipe of this outside diameter: at most about 1/3 m wide, at least eight. */
    public static int sides(double outside) {
        return Math.max(8, 4 * (int) Math.ceil(.75 * Math.PI * outside));
    }

    /** A drawn outside diameter in 1/64 m steps (1 to 64), as cell keys carry it. */
    public static int code(double outside) {
        return (int) Math.max(1, Math.min(64, Math.round(outside * 64)));
    }

    public static double outsideOf(int code) {
        return code / 64.0;
    }
}
