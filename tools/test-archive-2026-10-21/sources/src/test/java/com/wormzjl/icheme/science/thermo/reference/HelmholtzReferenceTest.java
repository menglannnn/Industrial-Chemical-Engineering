package com.wormzjl.icheme.science.thermo.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Internal consistency of the Helmholtz oracle: analytic alpha derivatives against central differences. */
class HelmholtzReferenceTest {
    private static final double TOLERANCE = 1.0e-7;
    private static final double STEP = 1.0e-4;
    /** (tau, delta) pairs: dilute gas, near-critical (inside the non-analytic terms' reach), dense liquid. */
    private static final double[][] STATES = {
            {0.6, 0.05}, {0.9, 0.4}, {1.02, 1.05}, {0.98, 0.93}, {1.3, 2.2}, {1.8, 2.6}};

    @ParameterizedTest
    @ValueSource(strings = {"Nitrogen", "CarbonDioxide", "Methane", "Ethane", "Hydrogen", "Water"})
    void analyticDerivativesMatchCentralDifferences(String fluidName) {
        HelmholtzFluid fluid = HelmholtzFluid.load(fluidName);
        List<String> failures = new ArrayList<>();
        for (double[] state : STATES) {
            check(fluidName + " alpha0", state[0], state[1], fluid::idealDerivatives, failures);
            check(fluidName + " alphar", state[0], state[1], fluid::residualDerivatives, failures);
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nitrogen", "CarbonDioxide", "Methane", "Ethane", "Hydrogen", "Water"})
    void saturationHasEqualPressureAndFugacity(String fluidName) {
        HelmholtzFluid fluid = HelmholtzFluid.load(fluidName);
        double tt = fluid.tripleTemperature();
        double tc = fluid.criticalTemperature();
        for (double fraction : new double[] {0.0, 0.2, 0.5, 0.8, 0.97}) {
            double t = tt + fraction * (tc - tt);
            SaturationState sat = fluid.saturation(t);
            // The liquid-side pressure is a small difference of large terms: its relative rounding error is about
            // 1e-15 / Z_L, so equality of pressure and of ln phi is checked to 1e-12 / Z_L.
            double tolerance = 1.0e-12 / sat.liquid().compressibility();
            assertEquals(0.0, sat.liquid().logFugacityCoefficient() - sat.vapour().logFugacityCoefficient(),
                    tolerance, fluidName + " ln phi at " + t + " K");
            assertEquals(1.0, sat.liquid().pressure() / sat.vapour().pressure(), tolerance,
                    fluidName + " pressure at " + t + " K");
            // At the coexistence pressure p(rho) = Psat has three roots; each root choice must find its own phase.
            assertEquals(1.0, fluid.density(t, sat.pressure(), HelmholtzFluid.Root.LIQUID) / sat.liquid().density(),
                    1.0e-12, fluidName + " liquid root at " + t + " K");
            assertEquals(1.0, fluid.density(t, sat.pressure(), HelmholtzFluid.Root.VAPOUR) / sat.vapour().density(),
                    1.0e-12, fluidName + " vapour root at " + t + " K");
        }
    }

    @Test
    void unknownTermFamilyIsRefused() {
        JsonObject root = JsonFluidFiles.resource("Nitrogen.json");
        JsonArray alphar = root.getAsJsonArray("EOS").get(0).getAsJsonObject().getAsJsonArray("alphar");
        alphar.add(JsonParser.parseString("{\"type\": \"ResidualHelmholtzGaoB\", \"n\": [1.0]}"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> HelmholtzFluid.parse(root));
        assertTrue(error.getMessage().contains("ResidualHelmholtzGaoB"), error.getMessage());
    }

    @FunctionalInterface
    private interface Evaluator {
        void evaluate(double tau, double delta, AlphaDerivatives out);
    }

    /**
     * Fourth-order central differences, f'(x) = [f(x - 2h) - 8 f(x - h) + 8 f(x + h) - f(x + 2h)] / (12 h), with
     * h = 1e-4 x: the Gaussian and non-analytic terms vary on a scale of 1/sqrt(2 beta), 0.03 in tau for water's
     * beta = 700 to 800, which a second-order stencil at an affordable step cannot resolve to 1e-7.
     */
    private static void check(String label, double tau, double delta, Evaluator evaluator, List<String> failures) {
        AlphaDerivatives centre = new AlphaDerivatives();
        evaluator.evaluate(tau, delta, centre);
        double hd = STEP * delta;
        double ht = STEP * tau;
        AlphaDerivatives[] alongDelta = new AlphaDerivatives[4];
        AlphaDerivatives[] alongTau = new AlphaDerivatives[4];
        double[] offsets = {-2.0, -1.0, 1.0, 2.0};
        for (int i = 0; i < 4; i++) {
            alongDelta[i] = new AlphaDerivatives();
            alongTau[i] = new AlphaDerivatives();
            evaluator.evaluate(tau, delta + offsets[i] * hd, alongDelta[i]);
            evaluator.evaluate(tau + offsets[i] * ht, delta, alongTau[i]);
        }
        String where = String.format(Locale.ROOT, "%s at tau=%.3f delta=%.3f", label, tau, delta);
        compare(where + " dDelta", centre.dDelta, stencil(alongDelta, hd, d -> d.alpha), centre.alpha, failures);
        compare(where + " dTau", centre.dTau, stencil(alongTau, ht, d -> d.alpha), centre.alpha, failures);
        compare(where + " dDelta2", centre.dDelta2, stencil(alongDelta, hd, d -> d.dDelta), centre.dDelta, failures);
        compare(where + " dTau2", centre.dTau2, stencil(alongTau, ht, d -> d.dTau), centre.dTau, failures);
        compare(where + " dDeltaDTau (from dDelta)", centre.dDeltaDTau, stencil(alongTau, ht, d -> d.dDelta),
                centre.dDelta, failures);
        compare(where + " dDeltaDTau (from dTau)", centre.dDeltaDTau, stencil(alongDelta, hd, d -> d.dTau),
                centre.dTau, failures);
    }

    private static double stencil(AlphaDerivatives[] points, double h,
                                  ToDoubleFunction<AlphaDerivatives> value) {
        return (value.applyAsDouble(points[0]) - 8.0 * value.applyAsDouble(points[1])
                + 8.0 * value.applyAsDouble(points[2]) - value.applyAsDouble(points[3])) / (12.0 * h);
    }

    /**
     * Relative comparison; a derivative that is zero or nearly so (the ideal part's mixed derivative, a residual
     * derivative crossing zero) is measured against the magnitude of the function it was differentiated from.
     */
    private static void compare(String where, double analytic, double numeric, double parent, List<String> failures) {
        double scale = Math.max(Math.abs(analytic), 1.0e-3 * Math.max(Math.abs(parent), 1.0));
        double deviation = Math.abs(analytic - numeric) / scale;
        if (!(deviation <= TOLERANCE)) {
            failures.add(String.format(Locale.ROOT, "%s: analytic %.12g numeric %.12g (rel %.2e)", where, analytic,
                    numeric, deviation));
        }
    }
}
