package com.wormzjl.icheme.science.fluid.thermo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog;
import com.wormzjl.icheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.icheme.science.thermo.phase.CubicPhaseEvaluator;
import com.wormzjl.icheme.science.thermo.phase.PhaseDerivatives;
import com.wormzjl.icheme.science.thermo.phase.PhaseState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * {@link TranslatedPengRobinson} on a {@code PairInteractions} model, as the phase engine reaches it: the evaluator of a
 * package with a reference spine ({@link CubicPhaseEvaluator#forPackage}) runs the residual-only model on the kernel's
 * {@code TEMPERATURE_DEPENDENT_PAIRS}. The pilot package with constant pairs instead of its E-PPR78 rule is, through
 * that path, the constant matrix's model ({@link CubicPhaseEvaluator#fromPackage}, {@code SPARSE_PAIRS}) bit for bit,
 * values and every derivative block; on the E-PPR78 pilot every derivative block the evaluator forms matches central
 * differences of its own values.
 */
class TranslatedPengRobinsonPairInteractionsTest {
    private static final String PILOT = PilotCryogenicTestCatalog.PACKAGE_ID;
    private static final String INTERACTIONS = "data/icheme/materials/interactions/pilot_cryogenic.json";
    private static final double TEMPERATURE_STEP = 1.0e-3;
    private static final double MOLE_STEP = 1.0e-6;
    private static final double PRESSURE_FRACTION = 1.0e-4;
    private static final double TOLERANCE = 1.0e-6;
    /** The pilot's E-PPR78 k_ij at 300 K (appendix A of P3_PILOT_ENGINE_PLAN.md), as six constant pairs. */
    private static final Object[][] CONSTANT_PAIRS = {
            {"Methane", "Ethane", 0.0057}, {"Methane", "CarbonDioxide", 0.1104}, {"Nitrogen", "Methane", 0.0256},
            {"Ethane", "CarbonDioxide", 0.1267}, {"Nitrogen", "Ethane", 0.0262}, {"CarbonDioxide", "Nitrogen", -0.0306}};

    @Test void constantPairModelIsTheMatrixModelBitForBit() {
        var catalog = PilotCryogenicTestCatalog.parseWith(INTERACTIONS, o -> {
            o.remove("rule");
            for (Object[] row : CONSTANT_PAIRS) {
                var pair = new JsonObject();
                pair.addProperty("first", (String) row[0]);
                pair.addProperty("second", (String) row[1]);
                pair.addProperty("kij", (Double) row[2]);
                o.getAsJsonArray("pairs").add(pair);
            }
        });
        var pkg = catalog.requirePackage(PILOT);
        assertNull(pkg.groupContributions(), "the edited package has no rule");
        var data = SpinePackageData.of(catalog, PILOT);
        var model = CubicPhaseEvaluator.forPackage(catalog, PILOT);
        var matrix = CubicPhaseEvaluator.fromPackage(pkg, pkg.components(), data.translations(), data.idealGas());
        int n = pkg.components().size();
        var random = new Random(20260924L);
        var states = new ArrayList<State>();
        for (double t : new double[] {95, 110, 130, 150, 190, 250, 300, 450, 900}) {
            for (double p : new double[] {1e5, 1e6, 5e6}) {
                double[] x = new double[n];
                for (int i = 0; i < n; i++) x[i] = Math.pow(10.0, -2.0 * random.nextDouble());
                states.add(new State(t, p, x));
            }
        }
        states.add(new State(120, 2e6, new double[] {0.2, 0.8, 0, 0}));
        states.add(new State(240, 5e6, new double[] {0, 0.6, 0, 0.4}));
        var modelWorkspace = model.newWorkspace();
        var matrixWorkspace = matrix.newWorkspace();
        var modelDerivatives = model.newDerivatives();
        var matrixDerivatives = matrix.newDerivatives();
        int compared = 0;
        for (State state : states) {
            for (Root root : Root.values()) {
                RuntimeException modelFailure = null;
                RuntimeException matrixFailure = null;
                try { model.derivatives(state.temperature, state.pressure, state.amounts, root, modelWorkspace, modelDerivatives); }
                catch (RuntimeException failure) { modelFailure = failure; }
                try { matrix.derivatives(state.temperature, state.pressure, state.amounts, root, matrixWorkspace, matrixDerivatives); }
                catch (RuntimeException failure) { matrixFailure = failure; }
                String at = state + " " + root;
                assertEquals(matrixFailure == null, modelFailure == null, "same refusal at " + at);
                if (matrixFailure != null) continue;
                compared += same(matrixDerivatives.state(), modelDerivatives.state(), at);
                compared += same(matrixDerivatives, modelDerivatives, n, at);
                // The value path on its own, as a flash calls it.
                compared += same(matrix.evaluate(state.temperature, state.pressure, state.amounts, root, matrixWorkspace, matrix.newState()),
                        model.evaluate(state.temperature, state.pressure, state.amounts, root, modelWorkspace, model.newState()), at + " evaluate");
            }
        }
        System.out.println("CubicPhaseEvaluator on constant pairs (TEMPERATURE_DEPENDENT_PAIRS) vs the matrix (SPARSE_PAIRS), pilot package: "
                + compared + " doubles compared, all bit-identical");
        assertTrue(compared > 2_000, "compared " + compared);
    }

    private static int same(PhaseState expected, PhaseState actual, String at) {
        assertEquals(expected.kind(), actual.kind(), at);
        assertEquals(expected.physicalRootCount(), actual.physicalRootCount(), at);
        int n = expected.componentCount();
        double[] e = {expected.molarVolume(), expected.molarEnthalpy(), expected.idealGasEnthalpy(), expected.residualEnthalpy(),
                expected.molarEntropy(), expected.residualEntropy(), expected.molarGibbsEnergy(), expected.compressibilityFactor()};
        double[] a = {actual.molarVolume(), actual.molarEnthalpy(), actual.idealGasEnthalpy(), actual.residualEnthalpy(),
                actual.molarEntropy(), actual.residualEntropy(), actual.molarGibbsEnergy(), actual.compressibilityFactor()};
        int compared = same(e, a, at + " scalars");
        for (int i = 0; i < n; i++) compared += same(expected.logFugacityCoefficient(i), actual.logFugacityCoefficient(i), at + " ln phi " + i);
        return compared;
    }

    private static int same(PhaseDerivatives expected, PhaseDerivatives actual, int n, String at) {
        int compared = same(new double[] {expected.volumeTemperatureDerivative(), expected.volumePressureDerivative(),
                        expected.enthalpyTemperatureDerivative(), expected.enthalpyPressureDerivative()},
                new double[] {actual.volumeTemperatureDerivative(), actual.volumePressureDerivative(),
                        actual.enthalpyTemperatureDerivative(), actual.enthalpyPressureDerivative()}, at + " volumetric and caloric");
        compared += same(expected.logFugacityTemperatureDerivativeView(), actual.logFugacityTemperatureDerivativeView(), at + " d ln phi/dT");
        compared += same(expected.logFugacityPressureDerivativeView(), actual.logFugacityPressureDerivativeView(), at + " d ln phi/dP");
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            compared += same(expected.logFugacityCompositionDerivative(i, j), actual.logFugacityCompositionDerivative(i, j), at + " dn " + i + "," + j);
        }
        return compared;
    }

    private static int same(double[] expected, double[] actual, String what) {
        for (int i = 0; i < expected.length; i++) same(expected[i], actual[i], what + "[" + i + "]");
        return expected.length;
    }

    private static int same(double expected, double actual, String what) {
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual), () -> what + ": " + expected + " vs " + actual);
        return 1;
    }

    // ---------------------------------------------------------------------------------------------------------------

    /** The pilot states of the kernel test (seed 20260925): twelve four-component states and eight binaries. */
    private static List<State> pilotStates() {
        var states = new ArrayList<State>();
        var random = new Random(20260925L);
        for (double[] point : new double[][] {{95, 1e5}, {110, 5e5}, {130, 2e6}, {150, 1e6}, {170, 4e6}, {190, 2e6},
                {220, 6e6}, {250, 1e6}, {300, 1e7}, {450, 2e6}, {900, 2e5}, {1150, 2e5}}) {
            double[] x = new double[4];
            for (int i = 0; i < 4; i++) x[i] = Math.pow(10.0, -2.0 * random.nextDouble());
            states.add(new State(point[0], point[1], x));
        }
        states.add(new State(100, 3e5, new double[] {0.3, 0.7, 0, 0}));
        states.add(new State(160, 3e6, new double[] {0.2, 0.8, 0, 0}));
        states.add(new State(200, 2e6, new double[] {0, 0.5, 0.5, 0}));
        states.add(new State(240, 5e6, new double[] {0, 0.6, 0, 0.4}));
        states.add(new State(260, 3e6, new double[] {0, 0, 0.4, 0.6}));
        states.add(new State(230, 8e6, new double[] {0.3, 0, 0, 0.7}));
        states.add(new State(180, 1e6, new double[] {0.2, 0, 0.8, 0}));
        states.add(new State(600, 5e6, new double[] {0, 0.5, 0.5, 0}));
        return List.copyOf(states);
    }

    /**
     * The E-PPR78 pilot through the engine's evaluator: {@code d ln phi_i/dT}, {@code c_p}, {@code dv/dT},
     * {@code d ln phi_i/dP}, {@code dv/dP}, {@code dh/dP} and {@code d ln phi_i/dn_j} against central differences of the
     * evaluator's values on the same root; 1e-6 of the largest magnitude each quantity reaches at the state.
     */
    @Test void groupModelDerivativesMatchCentralDifferences() {
        var evaluator = CubicPhaseEvaluator.forPackage(MaterialCatalog.bundled(), PILOT);
        assertTrue(MaterialCatalog.bundled().requirePackage(PILOT).groupContributions() != null, "the pilot pairs follow E-PPR78");
        var workspace = evaluator.newWorkspace();
        var output = evaluator.newDerivatives();
        var worst = new LinkedHashMap<String, Worst>();
        int compared = 0;
        for (State state : pilotStates()) for (Root root : Root.values()) {
            try {
                evaluator.derivatives(state.temperature, state.pressure, state.amounts, root, workspace, output);
            } catch (RuntimeException unavailable) { continue; }
            compared++;
            int count = state.amounts.length;
            String at = state + " " + root;
            PhaseState phase = output.state().copy();

            if (state.keepsRoot(evaluator, root, TEMPERATURE_STEP, 0, 0)) {
                var higher = state.at(evaluator, root, TEMPERATURE_STEP, 0, 0);
                var lower = state.at(evaluator, root, -TEMPERATURE_STEP, 0, 0);
                double scale = 0;
                for (int i = 0; i < count; i++) scale = Math.max(scale, Math.abs(output.logFugacityTemperatureDerivativeView()[i]));
                for (int i = 0; i < count; i++) {
                    if (state.fractions[i] == 0) continue;
                    record(worst, "d ln phi_i/dT", at, (higher.logFugacityCoefficient(i) - lower.logFugacityCoefficient(i))
                            / (2 * TEMPERATURE_STEP), output.logFugacityTemperatureDerivativeView()[i], scale);
                }
                record(worst, "dh/dT (cp)", at, (higher.molarEnthalpy() - lower.molarEnthalpy()) / (2 * TEMPERATURE_STEP),
                        output.enthalpyTemperatureDerivative(), Math.abs(output.enthalpyTemperatureDerivative()));
                record(worst, "dv/dT", at, (higher.molarVolume() - lower.molarVolume()) / (2 * TEMPERATURE_STEP),
                        output.volumeTemperatureDerivative(), Math.abs(output.volumeTemperatureDerivative()));
            }
            double pressureStep = state.pressure * PRESSURE_FRACTION;
            if (state.keepsRoot(evaluator, root, 0, pressureStep, 0)) {
                var higher = state.at(evaluator, root, 0, pressureStep, 0);
                var lower = state.at(evaluator, root, 0, -pressureStep, 0);
                double scale = 0;
                for (int i = 0; i < count; i++) scale = Math.max(scale, Math.abs(output.logFugacityPressureDerivativeView()[i]));
                for (int i = 0; i < count; i++) {
                    if (state.fractions[i] == 0) continue;
                    record(worst, "d ln phi_i/dP", at, (higher.logFugacityCoefficient(i) - lower.logFugacityCoefficient(i))
                            / (2 * pressureStep), output.logFugacityPressureDerivativeView()[i], scale);
                }
                record(worst, "dv/dP", at, (higher.molarVolume() - lower.molarVolume()) / (2 * pressureStep),
                        output.volumePressureDerivative(), Math.abs(output.volumePressureDerivative()));
                record(worst, "dh/dP", at, (higher.molarEnthalpy() - lower.molarEnthalpy()) / (2 * pressureStep),
                        output.enthalpyPressureDerivative(), Math.abs(output.enthalpyPressureDerivative()));
            }
            double logPhiScale = 0;
            for (int i = 0; i < count; i++) for (int j = 0; j < count; j++) {
                logPhiScale = Math.max(logPhiScale, Math.abs(output.logFugacityCompositionDerivative(i, j)));
            }
            for (int j = 0; j < count; j++) {
                if (!(state.fractions[j] > 10 * MOLE_STEP) || !state.keepsRoot(evaluator, root, 0, 0, j + 1)) continue;
                var higher = state.at(evaluator, root, 0, 0, j + 1);
                var lower = state.at(evaluator, root, 0, 0, -(j + 1));
                for (int i = 0; i < count; i++) {
                    if (state.fractions[i] == 0) continue;
                    record(worst, "d ln phi_i/dn_j", at, (higher.logFugacityCoefficient(i) - lower.logFugacityCoefficient(i))
                            / (2 * MOLE_STEP), output.logFugacityCompositionDerivative(i, j), logPhiScale);
                }
            }
            assertEquals(Double.doubleToLongBits(phase.molarVolume()), Double.doubleToLongBits(
                    evaluator.evaluate(state.temperature, state.pressure, state.amounts, root, workspace, evaluator.newState()).molarVolume()),
                    "the derivative block describes the evaluated phase at " + at);
        }
        System.out.println("CubicPhaseEvaluator (E-PPR78 pilot) derivatives vs central differences: " + compared + " phase evaluations");
        worst.forEach((quantity, value) -> System.out.println("    " + String.format(Locale.ROOT, "%-18s", quantity) + value));
        assertTrue(compared >= 30, "too few phase evaluations: " + compared);
        for (String quantity : List.of("dh/dT (cp)", "dv/dT", "d ln phi_i/dT", "d ln phi_i/dP", "d ln phi_i/dn_j")) {
            assertTrue(worst.containsKey(quantity) && worst.get(quantity).count >= 20, quantity + " barely compared");
        }
        worst.forEach((quantity, value) -> assertTrue(value.deviation <= TOLERANCE, quantity + ": " + value));
    }

    /** {@code amounts} sums to one mole, so a mole perturbation is also a fraction perturbation. */
    private record State(double temperature, double pressure, double[] amounts, double[] fractions) {
        State(double temperature, double pressure, double[] raw) {
            this(temperature, pressure, normalized(raw), normalized(raw));
        }
        private static double[] normalized(double[] raw) {
            double total = 0;
            for (double value : raw) total += value;
            double[] fractions = new double[raw.length];
            for (int i = 0; i < raw.length; i++) fractions[i] = raw[i] / total;
            return fractions;
        }
        /** {@code component} is a signed one-based index; zero means no composition perturbation. */
        PhaseState at(CubicPhaseEvaluator evaluator, Root root, double temperatureStep, double pressureStep, int component) {
            double[] moles = amounts;
            if (component != 0) {
                moles = amounts.clone();
                moles[Math.abs(component) - 1] += Math.signum(component) * MOLE_STEP;
            }
            return evaluator.evaluate(temperature + temperatureStep, pressure + pressureStep, moles, root,
                    evaluator.newWorkspace(), evaluator.newState());
        }
        /** Both probe endpoints stay on the base state's root: a derivative is a derivative of one branch. */
        boolean keepsRoot(CubicPhaseEvaluator evaluator, Root root, double temperatureStep, double pressureStep, int component) {
            try {
                var base = at(evaluator, root, 0, 0, 0);
                var higher = at(evaluator, root, temperatureStep, pressureStep, component);
                var lower = at(evaluator, root, -temperatureStep, -pressureStep, component == 0 ? 0 : -component);
                return sameBranch(base, higher) && sameBranch(base, lower);
            } catch (RuntimeException unavailable) { return false; }
        }
        private static boolean sameBranch(PhaseState base, PhaseState probe) {
            return Math.abs(probe.molarVolume() - base.molarVolume()) < 1e-3 * Math.abs(base.molarVolume());
        }
        @Override public String toString() { return "T=" + temperature + " P=" + pressure; }
    }

    private record Worst(double deviation, double expected, double actual, String at, int count) {
        @Override public String toString() {
            return String.format(Locale.ROOT, "%.2e over %d (expected %.6e, actual %.6e at %s)", deviation, count, expected, actual, at);
        }
    }

    private static void record(Map<String, Worst> worst, String quantity, String at, double expected, double actual, double scale) {
        assertTrue(Double.isFinite(expected) && Double.isFinite(actual), quantity + " must be finite at " + at);
        double denominator = Math.max(scale, Math.max(Math.abs(expected), Math.abs(actual)));
        double deviation = denominator == 0 ? 0 : Math.abs(expected - actual) / denominator;
        var previous = worst.get(quantity);
        if (previous == null) worst.put(quantity, new Worst(deviation, expected, actual, at, 1));
        else if (deviation > previous.deviation) worst.put(quantity, new Worst(deviation, expected, actual, at, previous.count + 1));
        else worst.put(quantity, new Worst(previous.deviation, previous.expected, previous.actual, previous.at, previous.count + 1));
    }
}
