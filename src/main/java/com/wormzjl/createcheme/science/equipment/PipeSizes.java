package com.wormzjl.createcheme.science.equipment;

/**
 * Drawn proportions of a line and its flanges from its bore, shared by pipes, vessel nozzles and pipeline devices
 * (photo survey 2026-10-16, section 0: ASME B36.10 pipe and B16.5 / B16.47 Class 150 flanges). Sizes are capped at one
 * block (owner request 2026-10-16: pipes capped at 1 m for now).
 */
public final class PipeSizes {
    /** The smallest drawn outside diameter: one texel. */
    public static final double MIN_OUTSIDE = 1 / 16.0;

    private PipeSizes() {
    }

    /** Outside diameter of a pipe of bore {@code bore}: the wall adds about a tenth (2" 52.5/60.3 mm, 12" 303/324 mm). */
    public static double outside(double bore) {
        return Math.max(MIN_OUTSIDE, Math.min(1, 1.1 * bore));
    }

    /**
     * Flange outside diameter: about twice the pipe at 4", 1.5 times at 12", 1.3 times from 24" (1.25 OD + 85 mm fits the
     * table within a few percent), at most one block.
     */
    public static double flange(double outside) {
        return Math.min(1, 1.25 * outside + .085);
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
