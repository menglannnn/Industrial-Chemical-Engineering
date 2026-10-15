package com.wormzjl.createcheme.science.column.v3;

import static org.junit.jupiter.api.Assertions.*;

import com.wormzjl.createcheme.world.level.block.entity.ColumnCalculatorV3BlockEntity;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The decoder's below-floor prune. A kept component whose decoded flow lands below its support floor is
 * pruned to exactly zero; every assertion here is an exact comparison.
 */
class V3DecoderPresenceFloorTest {
    private static final int STAGES = 17, NODE = 5, COMPONENT = 3, LIQUID_COMPOSITION = 3;
    private static final double PRESENCE_THRESHOLD = .02;

    @Test void defaultDecodeStillPrunesAKeptComponentThatFallsBelowItsFloor() {
        var input = input(); double[][] raw = belowFloor(input, 8);
        assertEquals(0, decode(input, raw, V3FactorizedNeuralFeatures.DecodeOptions.NONE).liquid()[NODE][COMPONENT]);
        // The shipped zero-phase floor only seeds empty phases; a below-floor trace of a populated phase is pruned.
        assertEquals(0, decode(input, raw, V3FactorizedNeuralFeatures.DecodeOptions.zeroPhaseFloor(10))
                .liquid()[NODE][COMPONENT]);
    }

    @Test void malformedDecodeOptionsFailClosed() {
        var input = input(); double[][] raw = belowFloor(input, 8);
        assertThrows(IllegalArgumentException.class, () -> V3FactorizedNeuralFeatures.decode(
                input, "test", V3CondenserPhaseBranch.TWO_PHASE, raw, PRESENCE_THRESHOLD, null));
    }

    private static V3NeuralSeed decode(V3ColumnInput input, double[][] raw, V3FactorizedNeuralFeatures.DecodeOptions options) {
        return V3FactorizedNeuralFeatures.decode(input, "test", V3CondenserPhaseBranch.TWO_PHASE, raw, PRESENCE_THRESHOLD, options);
    }

    /** One liquid trace of {@link #NODE} driven far under its floor, with a chosen presence logit. */
    private static double[][] belowFloor(V3ColumnInput input, double presenceLogit) {
        double[][] raw = V3FactorizedNeuralFeatures.targets(teacher(input));
        int c = input.componentBasis().componentCount();
        raw[NODE][LIQUID_COMPOSITION + COMPONENT] = -100;
        raw[NODE][5 + 2*c + COMPONENT] = presenceLogit;
        return raw;
    }

    private static V3ColumnInput input() {
        var base = ColumnCalculatorV3BlockEntity.methaneCduInput();
        double[] feed = base.feedComponentMolarFlowsMolPerSecond(); feed[0] = 0;
        return new V3ColumnInput(base.schemaVersion(), base.packageId(), base.assayId(), base.componentBasis(), feed,
                base.feedTemperatureKelvin(), STAGES, Math.max(1, STAGES/2), 200_000, 100,
                List.of(new V3ColumnSpecification.CondenserOutletTemperature(313.15),
                        new V3ColumnSpecification.OrganicRefluxRatio(2), new V3ColumnSpecification.ReboilerDuty(1e6)));
    }

    private static V3NeuralSeed teacher(V3ColumnInput input) {
        int nodes = input.stageCount()+2; double[] feed = input.feedComponentMolarFlowsMolPerSecond();
        double[][] l = new double[nodes][feed.length], v = new double[nodes][feed.length];
        double[] t = new double[nodes]; Arrays.fill(t, 400); t[0] = 313.15;
        for (int n = 0; n < nodes; n++) for (int c = 0; c < feed.length; c++) { l[n][c] = 2*feed[c]; v[n][c] = .5*feed[c]; }
        return new V3NeuralSeed(input, "test-property", V3CondenserPhaseBranch.TWO_PHASE, l, v, t, new double[nodes], new boolean[nodes]);
    }
}
