package com.wormzjl.createcheme.science.fluid.thermo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.IdealGasFunction;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import com.wormzjl.createcheme.science.thermo.phase.CubicPhaseEvaluator;
import com.wormzjl.createcheme.science.thermo.phase.PhaseDerivatives;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * P3 WP5 gate, ported at M2 of the unified thermo port: a package without a reference spine keeps its coefficient path
 * bit for bit. The spine path (ideal gas from the spine records, E-PPR78 pairs, Tr = 0.8 anchors, the thermodynamic
 * identity as the revision) is taken only for a package that declares a {@code spine}; the bundled network package does
 * not.
 *
 * <p>The digest hashes the raw bits of every number the network reads from its hydrocarbon model on a fixed probe (the
 * {@link HydrocarbonModel#phase} record on both roots, the translated equation of state's values, the derivatives of the
 * engine's evaluator of the package, the free-water {@link FluidThermodynamics#flashTP} state with its acoustic velocity
 * bound, the pump reference density), and the text of every refusal the probe meets. The value is JDK-specific and is
 * pinned on the toolchain's JDK 21.0.11; a change of the legacy formulation on purpose re-pins it with its reason.</p>
 */
class LegacyNetworkPathPinTest {
    static final String NETWORK = "createcheme:tjl20_methane_nitrogen";
    /**
     * SHA-256 of {@link #digest}, pinned at M2 (2026-10-16) by this test on the toolchain's JDK 21.0.11 under Gradle. The
     * branch's last value ({@code 778b87e5ff0613589a0010d39890640bebdafad2cacc9c2799887eec582d9fc6}, P3 WP7c) does not
     * carry over, by design: main's {@code DeterministicMath} replaces {@code java.lang.Math} (last-bit differences), the
     * derivative part is the engine evaluator's {@link PhaseDerivatives} instead of the branch's translated derivative
     * object (OD-P2: not on main), main's phase record carries no liquid-root-absent flag, and the engine's flash solves
     * Rachford-Rice in the minor phase's fraction (D24).
     */
    static final String DIGEST = "dc9a89199a46e4f1662925d27e18ffd1bd037acb2757e455c92950bd17d99e02";
    /**
     * The revision string: the formulation tag {@code direct-liquid-v1:tp-engine-v1} (the phases decided by the
     * equilibrium engine) and the fluid thermodynamic fingerprint of the widened {@code fluid_domain} (nitrogen, methane
     * and the envelope to 10 MPa, P3 WP7 data) replace main 0.8.0's: a fresh world.
     */
    static final String REVISION = "95cd8e6d4b4d4af2dfb8e712e443eda42acb0c93b4218635e7ebf462e21829cd:direct-liquid-v1:tp-engine-v1"
            + ":fluid-domain-data-v1:cp-segments-v1:catalog-volume-reference-v1";

    @Test
    void theBundledNetworkPackageKeepsItsCoefficientPathBitForBit() {
        var catalog = MaterialCatalog.bundled();
        var model = FluidTestSupport.model(catalog, NETWORK);
        String revision = model.hydrocarbon.revision();
        System.out.println("legacy network revision: " + revision);
        String digest = digest(catalog, model);
        System.out.println("legacy network digest: " + digest);
        // The revision keeps WP4's layout: the package's fluid thermodynamic fingerprint and the formulation tags.
        assertEquals(catalog.fluidThermoFingerprint(NETWORK) + ":" + HydrocarbonModel.FORMULATION
                + ":fluid-domain-data-v1:cp-segments-v1:catalog-volume-reference-v1", revision);
        assertEquals(REVISION, revision);
        assertEquals(DIGEST, digest);
        assertTrue(catalog.requirePackage(NETWORK).spine().isEmpty(), "the bundled network package declares no spine");
    }

    /**
     * The engine's evaluator of a package without a spine, as {@link NetworkPhaseEngine} builds it: the package's PR78
     * constants and constant interactions, the network model's own translations, and the {@code shifted_polynomial_5}
     * fits on the network's datum over each component's fluid validity.
     */
    static CubicPhaseEvaluator evaluator(MaterialCatalog catalog, FluidThermodynamics model) {
        var propertyPackage = catalog.requirePackage(NETWORK);
        var translated = model.hydrocarbon.translated();
        int n = propertyPackage.components().size();
        double[] translations = new double[n];
        List<IdealGasFunction> gas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            translations[i] = translated.translation(i);
            var property = propertyPackage.properties().get(i);
            var range = catalog.fluidValidity(NETWORK, propertyPackage.components().get(i));
            gas.add(new NetworkPhaseEngine.CoefficientIdealGas(property,
                    range.map(MaterialCatalog.Validity::minimumTemperature).orElse(property.minimumTemperature()),
                    range.map(MaterialCatalog.Validity::maximumTemperature).orElse(property.maximumTemperature())));
        }
        return CubicPhaseEvaluator.fromPackage(propertyPackage, propertyPackage.components(), translations, gas);
    }

    /** Every number of the probe, in a fixed order, as SHA-256 hex. */
    static String digest(MaterialCatalog catalog, FluidThermodynamics model) {
        var sink = new Sink();
        var hydrocarbon = model.hydrocarbon;
        var eos = hydrocarbon.translated();
        var evaluator = evaluator(catalog, model);
        var workspace = evaluator.newWorkspace();
        int n = hydrocarbon.componentCount();
        var derivatives = new PhaseDerivatives(n);
        int nitrogen = hydrocarbon.components().indexOf("Nitrogen");
        double[] pure = new double[n];
        pure[nitrogen] = 1;
        double[] mixture = new double[n];
        for (int i = 0; i < n; i++) mixture[i] = i == nitrogen ? 0.1 : (i % 5 + 1) * 0.01;
        double[] lean = new double[n];
        lean[nitrogen] = 0.9;
        lean[(nitrogen + 1) % n] = 0.1;
        sink.number(model.pumpReferenceDensity());
        double[] temperatures = {77.0, 110.0, 150.0, 293.15, 350.0, 450.0, 700.0};
        double[] pressures = {1.0e5, 5.0e5, 2.0e6};
        double[][] compositions = {pure, lean, mixture};
        for (double t : temperatures) {
            for (double p : pressures) {
                for (double[] x : compositions) {
                    for (PengRobinsonKernel.Root root : PengRobinsonKernel.Root.values()) {
                        try {
                            var phase = hydrocarbon.phase(t, p, x, root);
                            sink.number(phase.molarVolume()).number(phase.molarEnthalpy()).number(phase.molarInternalEnergy())
                                    .number(phase.volumePressureDerivative()).numbers(phase.logFugacityView())
                                    .flag(phase.vaporLike());
                        } catch (RuntimeException refused) {
                            sink.refusal(refused);
                        }
                        try {
                            var v = eos.evaluateValues(t, p, x, root, eos.prepare(t));
                            sink.number(v.molarVolume()).number(v.molarEnthalpy()).number(v.molarInternalEnergy())
                                    .number(v.volumeTemperatureDerivative()).number(v.volumePressureDerivative())
                                    .number(v.heatCapacity()).number(v.enthalpyPressureDerivative())
                                    .number(v.isothermalCompressibility()).number(v.volumeSecondTemperatureDerivative())
                                    .number(v.compressibility()).number(v.idealGasHeatCapacity())
                                    .numbers(v.logFugacityCoefficientsView()).numbers(v.partialMolarVolumesView())
                                    .number(v.phaseIdentificationParameter()).number(v.physicalRootCount());
                        } catch (RuntimeException refused) {
                            sink.refusal(refused);
                        }
                        try {
                            evaluator.derivatives(t, p, x, root, workspace, derivatives);
                            var state = derivatives.state();
                            sink.number(state.molarVolume()).number(state.idealGasEnthalpy()).number(state.residualEnthalpy())
                                    .number(state.idealGasEntropy()).number(state.residualEntropy())
                                    .number(state.molarGibbsEnergy()).numbers(state.logFugacityCoefficientsView())
                                    .number(state.physicalRootCount()).number(state.root().ordinal());
                            for (int i = 0; i < n; i++) {
                                for (int j = 0; j < n; j++) sink.number(derivatives.logFugacityCompositionDerivative(i, j));
                            }
                            sink.numbers(derivatives.logFugacityTemperatureDerivativeView())
                                    .numbers(derivatives.logFugacityPressureDerivativeView())
                                    .number(derivatives.volumeTemperatureDerivative()).number(derivatives.volumePressureDerivative())
                                    .number(derivatives.enthalpyTemperatureDerivative())
                                    .number(derivatives.enthalpyPressureDerivative());
                        } catch (RuntimeException refused) {
                            sink.refusal(refused);
                        }
                    }
                    try {
                        double[] overall = new double[n + 1];
                        System.arraycopy(x, 0, overall, 0, n);
                        overall[n] = 0.1;
                        var state = model.flashTP(t, p, overall, () -> { });
                        sink.number(state.volume()).number(state.enthalpy()).number(state.internalEnergy()).number(state.mass())
                                .numbers(state.liquidView()).numbers(state.vaporView()).number(state.waterLiquid())
                                .number(state.waterVapor()).number(state.hydrocarbonPartialPressure())
                                .number(state.waterPartialPressure()).number(state.liquidVolume()).number(state.waterVolume())
                                .number(state.vaporVolume()).number(state.waterCompressibility()).number(model.velocityLimit(state));
                    } catch (RuntimeException refused) {
                        sink.refusal(refused);
                    }
                }
            }
        }
        return sink.hex();
    }

    private static final class Sink {
        private final MessageDigest digest;
        private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

        Sink() {
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
        }

        Sink number(double value) {
            buffer.clear();
            buffer.putLong(Double.doubleToRawLongBits(value));
            digest.update(buffer.array());
            return this;
        }

        Sink numbers(double[] values) {
            number(values.length);
            for (double value : values) number(value);
            return this;
        }

        Sink flag(boolean value) { return number(value ? 1 : 0); }

        void refusal(RuntimeException refused) {
            digest.update((refused.getClass().getName() + ": " + refused.getMessage()).getBytes(StandardCharsets.UTF_8));
        }

        String hex() { return HexFormat.of().formatHex(digest.digest()); }
    }
}
