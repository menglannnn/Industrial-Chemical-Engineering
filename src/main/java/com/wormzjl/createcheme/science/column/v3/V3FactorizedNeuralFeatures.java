package com.wormzjl.createcheme.science.column.v3;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import java.util.Arrays;

/** Gen3 targets separate phase traffic from normalized compositions and optional learned seed support. */
public final class V3FactorizedNeuralFeatures {
    public static final double TRACE_FLOOR_FRACTION = 1e-10;
    private static final double MAXIMUM_LOG_TOTAL = DeterministicMath.log1p(1000);

    private V3FactorizedNeuralFeatures() {}

    public static int outputWidth(int components) { return 4 * components + 5; }

    /** T, log1p(L/F), log1p(V/F), centered log(x/zFeed), centered log(y/zFeed), water, wet, and signed support labels. */
    public static double[][] targets(V3NeuralSeed seed) {
        double[] feed = seed.input().feedComponentMolarFlowsMolPerSecond();
        double total = Arrays.stream(feed).sum(); int c = feed.length;
        double[][] liquid = seed.liquid(), vapor = seed.vapor();
        double[] temperatures = seed.temperatures(), water = seed.freeWater(); boolean[] wet = seed.wetTrays();
        double[][] result = new double[temperatures.length][outputWidth(c)];
        for (int n = 0; n < result.length; n++) {
            result[n][0] = temperatures[n];
            encodePhase(result[n], 1, 3, 5 + 2*c, liquid[n], feed, total);
            encodePhase(result[n], 2, 3 + c, 5 + 3*c, vapor[n], feed, total);
            result[n][3 + 2*c] = DeterministicMath.log1p(water[n] / (total * 1e-8));
            result[n][4 + 2*c] = wet[n] ? 1 : 0;
        }
        return result;
    }

    private static void encodePhase(double[] target, int totalIndex, int compositionOffset, int presenceOffset,
            double[] flows, double[] feed, double totalFeed) {
        double phaseTotal = Arrays.stream(flows).sum();
        target[totalIndex] = DeterministicMath.log1p(phaseTotal / totalFeed);
        double center = 0; int active = 0;
        for (int c = 0; c < feed.length; c++) {
            double floor = Math.max(feed[c], totalFeed * 1e-12) * TRACE_FLOOR_FRACTION;
            target[presenceOffset + c] = feed[c] > 0 && flows[c] >= floor ? 8 : -8;
            if (feed[c] == 0 || phaseTotal == 0) continue;
            double logRatio = DeterministicMath.log(Math.max(flows[c], floor) / phaseTotal / (feed[c] / totalFeed));
            target[compositionOffset + c] = logRatio; center += logRatio; active++;
        }
        if (active > 0) for (int c = 0; c < feed.length; c++)
            if (feed[c] > 0) target[compositionOffset + c] -= center / active;
    }

    /**
     * The decoder's zero-phase floor.
     *
     * <p>The decode prunes a kept component whose decoded flow lands below its own support floor to exactly
     * zero, and the native support refresh then has to reinsert the point. Of F0's 819 above-floor
     * omissions across the 168 certified reference profiles, 765 are whole phases whose <b>phase-total</b>
     * head decoded to zero and 54 are components the presence head itself dropped. A phase total of zero
     * empties the phase whatever the composition head says, so {@code zeroPhaseFloorFactor} seeds such a
     * phase at that many support floors summed over the components the presence head did keep, and then
     * decodes it by the unchanged rule. A phase the condenser branch forbids is never lifted, and neither is
     * one in which the presence head kept nothing: the fallback component that a positive phase total
     * rescues when every support score is uncertain is not a kept component.</p>
     *
     * @param zeroPhaseFloorFactor support floors to seed a zero phase total at, or zero to leave it empty
     */
    record DecodeOptions(double zeroPhaseFloorFactor) {
        /** No floor: a zero phase total decodes to an empty phase. */
        static final DecodeOptions NONE = new DecodeOptions(0);

        DecodeOptions {
            if (!Double.isFinite(zeroPhaseFloorFactor) || zeroPhaseFloorFactor < 0 || zeroPhaseFloorFactor > 1e6
                    || (zeroPhaseFloorFactor > 0 && zeroPhaseFloorFactor < 1))
                throw new IllegalArgumentException("Invalid decoder zero-phase floor factor");
        }

        /** Seeds a zero phase total at {@code factor} support floors over the kept components. */
        static DecodeOptions zeroPhaseFloor(double factor) { return new DecodeOptions(factor); }

        boolean liftsZeroPhases() { return zeroPhaseFloorFactor > 0; }
    }

    static V3NeuralSeed decode(V3ColumnInput input, String revision, V3CondenserPhaseBranch branch,
            double[][] outputs, double presenceThreshold, DecodeOptions options) {
        if (options == null) throw new IllegalArgumentException("Missing learned seed decode options");
        if (!Double.isFinite(presenceThreshold) || presenceThreshold < 0 || presenceThreshold > .5)
            throw new IllegalArgumentException("Invalid learned seed presence threshold");
        double[] feed = input.feedComponentMolarFlowsMolPerSecond(); double total = Arrays.stream(feed).sum();
        int count = input.stageCount()+2, c = feed.length;
        if (outputs.length != count) throw new IllegalArgumentException("Factorized neural output node mismatch");
        double[][] liquid = new double[count][c], vapor = new double[count][c];
        double[] temperatures = new double[count], water = new double[count]; boolean[] wet = new boolean[count];
        for (int n = 0; n < count; n++) {
            double[] row = outputs[n];
            if (row.length != outputWidth(c)) throw new IllegalArgumentException("Factorized neural output width mismatch");
            for (double value : row) if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite factorized prediction");
            temperatures[n] = row[0];
            if (row[0] < 100 || row[0] > 1500) throw new IllegalArgumentException("Unbounded factorized temperature");
            double liquidTotal = phaseTotal(row[1], total), vaporTotal = phaseTotal(row[2], total);
            if (n == 0 && branch == V3CondenserPhaseBranch.LIQUID_ONLY) vaporTotal = 0;
            if (n == 0 && branch == V3CondenserPhaseBranch.VAPOR_ONLY) liquidTotal = 0;
            // A phase the condenser branch forbids is empty by topology, not by prediction, and is never lifted.
            boolean liquidAllowed = !(n == 0 && branch == V3CondenserPhaseBranch.VAPOR_ONLY);
            boolean vaporAllowed = !(n == 0 && branch == V3CondenserPhaseBranch.LIQUID_ONLY);
            liquid[n] = decodePhase(row, 3, 5 + 2*c, liquidTotal, feed, total, presenceThreshold, options, liquidAllowed);
            vapor[n] = decodePhase(row, 3 + c, 5 + 3*c, vaporTotal, feed, total, presenceThreshold, options, vaporAllowed);
            double waterLog = row[3 + 2*c];
            if (waterLog > 30) throw new IllegalArgumentException("Unbounded factorized water flow");
            wet[n] = row[4 + 2*c] >= .5 && n > 0 && n < count-1 && !input.steamFeeds().isEmpty();
            water[n] = wet[n] ? DeterministicMath.expm1(Math.max(0, waterLog)) * total * 1e-8 : 0;
        }
        for (var spec : input.specifications()) if (spec instanceof V3ColumnSpecification.CondenserOutletTemperature t) temperatures[0] = t.kelvin();
        return new V3NeuralSeed(input, revision, branch, liquid, vapor, temperatures, water, wet);
    }

    private static double phaseTotal(double value, double feed) {
        if (value > MAXIMUM_LOG_TOTAL) throw new IllegalArgumentException("Unbounded factorized phase total");
        return DeterministicMath.expm1(Math.max(0, value)) * feed;
    }

    private static double[] decodePhase(double[] row, int compositionOffset, int presenceOffset, double total,
            double[] feed, double totalFeed, double presenceThreshold, DecodeOptions options, boolean branchAllows) {
        double[] flows = new double[feed.length], logits = new double[feed.length];
        boolean[] present = new boolean[feed.length]; int fallback = -1;
        double maximum = -Double.MAX_VALUE, fallbackMaximum = -Double.MAX_VALUE;
        double thresholdLogit = presenceThreshold == 0 ? -Double.MAX_VALUE : DeterministicMath.log(presenceThreshold / (1-presenceThreshold));
        for (int c = 0; c < feed.length; c++) {
            if (Math.abs(row[compositionOffset+c]) > 120 || Math.abs(row[presenceOffset+c]) > 120)
                throw new IllegalArgumentException("Unbounded factorized composition or presence logit");
            if (feed[c] == 0) continue;
            logits[c] = DeterministicMath.log(feed[c] / totalFeed) + row[compositionOffset+c];
            if (fallback < 0 || logits[c] > fallbackMaximum) { fallback = c; fallbackMaximum = logits[c]; }
            present[c] = row[presenceOffset+c] >= thresholdLogit;
            if (present[c]) maximum = Math.max(maximum, logits[c]);
        }
        if (total == 0) {
            if (!options.liftsZeroPhases() || !branchAllows || maximum == -Double.MAX_VALUE) return flows;
            double floors = 0;
            for (int c = 0; c < feed.length; c++)
                if (present[c]) floors += Math.max(feed[c], totalFeed * 1e-12) * TRACE_FLOOR_FRACTION;
            if (!(floors > 0)) return flows;
            total = floors * options.zeroPhaseFloorFactor();
        }
        // A positive phase-total head retains at least one component if all support scores are uncertain.
        if (maximum == -Double.MAX_VALUE) { present[fallback] = true; maximum = fallbackMaximum; }
        double sum = 0;
        for (int c = 0; c < feed.length; c++) if (present[c]) { flows[c] = DeterministicMath.exp(logits[c]-maximum); sum += flows[c]; }
        double retained = 0;
        for (int c = 0; c < feed.length; c++) {
            flows[c] *= total / sum;
            double floor = Math.max(feed[c], totalFeed * 1e-12) * TRACE_FLOOR_FRACTION;
            if (flows[c] < floor) flows[c] = 0;
            retained += flows[c];
        }
        // This only forms a seed. The unchanged native support refresh and final physical audits decide acceptance.
        if (retained > 0) for (int c = 0; c < flows.length; c++) flows[c] *= total / retained;
        return flows;
    }
}
