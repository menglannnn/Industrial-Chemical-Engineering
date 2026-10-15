package com.wormzjl.createcheme.science.column.v3;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Nitrogen as a column feed component: the {@code tia_juana_nitrogen} preset is the Tia Juana column on the fluid network's
 * package ({@code tjl20_methane_nitrogen}). Nitrogen (Tc 126 K) cannot condense at the 59 C condenser, so all of it must
 * leave with the overhead vapor. The bundled model's axis has no nitrogen, so the default route solves it classically.
 */
class V3NitrogenColumnTest {
    @Test void aTwoPercentNitrogenFeedSolvesAndLeavesOverhead() {
        var catalog = com.wormzjl.createcheme.science.material.MaterialRuntime.current();
        var preset = catalog.presets().column("tia_juana_nitrogen").input(catalog);
        assertEquals("createcheme:tjl20_methane_nitrogen", preset.packageId());
        int nitrogen = preset.componentBasis().componentIds().indexOf("Nitrogen");
        assertTrue(nitrogen >= 0, "the nitrogen package's axis carries Nitrogen");
        double[] feed = preset.feedComponentMolarFlowsMolPerSecond().clone();
        double total = Arrays.stream(feed).sum();
        for (int i = 0; i < feed.length; i++) feed[i] *= 0.98;
        feed[nitrogen] = 0.02 * total;
        var input = new V3ColumnInput(preset.schemaVersion(), preset.packageId(), preset.assayId(), preset.componentBasis(), feed,
                preset.feedTemperatureKelvin(), preset.stageCount(), preset.feedStageNumber(), preset.topPressurePascal(),
                preset.stagePressureDropPascal(), preset.specifications(), preset.sideDraws(), preset.steamFeeds(), preset.pumparounds(),
                preset.columnDiameterMetres());
        assertTrue(V3NeuralRegistry.bundled().predict(input, V3SolveControl.UNBOUNDED).isEmpty());
        long start = System.nanoTime();
        var outcome = V3ColumnCalculator.calculate(input, () -> {
            if (System.nanoTime() - start > 120_000_000_000L) throw new java.util.concurrent.CancellationException("nitrogen deadline");
        }, 0, 0, V3TestInputs.LEARNED, V3NeuralRegistry.bundled(), V3ColumnCalculator.DEFAULT_LIQUID_SUPPLY_SCREEN_RATIO);
        System.out.println("Nitrogen column elapsed ms=" + (System.nanoTime() - start) / 1e6);
        var success = assertInstanceOf(V3ColumnOutcome.Success.class, outcome, outcome::toString);
        assertTrue(success.result().convergenceEvidence().satisfiesGates());
        // Conserved over the products; dissolved in the liquid distillate at about 1e-3 mole fraction per bar of its
        // partial pressure (Henry's law), so about 1 % of it leaves in the liquid products and the rest overhead.
        assertEquals(feed[nitrogen], nitrogenIn(success, s -> true), feed[nitrogen] * 1e-7, "nitrogen is conserved");
        double overhead = nitrogenIn(success, s -> s.streamId().equals("overhead_vapor"));
        assertTrue(overhead > 0.95 * feed[nitrogen], "nitrogen leaves with the overhead vapor: " + overhead + " of " + feed[nitrogen]);
    }
    private static double nitrogenIn(V3ColumnOutcome.Success success, java.util.function.Predicate<V3ColumnStreamProperties> streams) {
        return success.result().streams().stream().filter(streams)
                .mapToDouble(s -> s.molarFlowMolPerSecond() * s.moleFractions().stream().filter(f -> f.componentId().equals("Nitrogen"))
                        .mapToDouble(V3ColumnStreamProperties.ComponentFraction::moleFraction).sum()).sum();
    }
}
