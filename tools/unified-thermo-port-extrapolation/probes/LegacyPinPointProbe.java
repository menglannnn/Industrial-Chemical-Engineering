package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import com.wormzjl.createcheme.science.thermo.phase.PhaseDerivatives;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * Probe (range-extrapolation batch): LegacyNetworkPathPinTest's digest point by point, one line per probe stage, so the
 * outputs of two trees diff to the points whose numbers or refusals moved. Prints "PIN t p composition root stage: N
 * hash" or "... R refusal".
 */
class LegacyPinPointProbe {
    @Test
    void points() throws Exception {
        var catalog = MaterialCatalog.bundled();
        var model = FluidTestSupport.model(catalog, LegacyNetworkPathPinTest.NETWORK);
        var hydrocarbon = model.hydrocarbon;
        var eos = hydrocarbon.translated();
        var evaluator = LegacyNetworkPathPinTest.evaluator(catalog, model);
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
        System.out.println("PIN reference " + model.pumpReferenceDensity());
        double[] temperatures = {77.0, 110.0, 150.0, 293.15, 350.0, 450.0, 700.0};
        double[] pressures = {1.0e5, 5.0e5, 2.0e6};
        double[][] compositions = {pure, lean, mixture};
        String[] names = {"pure", "lean", "mixture"};
        for (double t : temperatures) {
            for (double p : pressures) {
                for (int c = 0; c < 3; c++) {
                    double[] x = compositions[c];
                    for (PengRobinsonKernel.Root root : PengRobinsonKernel.Root.values()) {
                        String key = "PIN " + t + " " + p + " " + names[c] + " " + root + " ";
                        try {
                            var phase = hydrocarbon.phase(t, p, x, root);
                            var s = new Sink();
                            s.number(phase.molarVolume()).number(phase.molarEnthalpy()).number(phase.molarInternalEnergy())
                                    .number(phase.volumePressureDerivative()).numbers(phase.logFugacityView());
                            System.out.println(key + "phase: N " + s.hex() + " v=" + phase.molarVolume() + " h=" + phase.molarEnthalpy());
                        } catch (RuntimeException refused) {
                            System.out.println(key + "phase: R " + refused);
                        }
                        try {
                            var v = eos.evaluateValues(t, p, x, root, eos.prepare(t));
                            var s = new Sink();
                            s.number(v.molarVolume()).number(v.molarEnthalpy()).number(v.molarInternalEnergy())
                                    .number(v.heatCapacity()).number(v.idealGasHeatCapacity())
                                    .numbers(v.logFugacityCoefficientsView());
                            System.out.println(key + "eos: N " + s.hex() + " h=" + v.molarEnthalpy() + " cp=" + v.heatCapacity());
                        } catch (RuntimeException refused) {
                            System.out.println(key + "eos: R " + refused);
                        }
                        try {
                            evaluator.derivatives(t, p, x, root, workspace, derivatives);
                            var state = derivatives.state();
                            var s = new Sink();
                            s.number(state.molarVolume()).number(state.idealGasEnthalpy()).number(state.residualEnthalpy())
                                    .number(state.idealGasEntropy()).number(state.residualEntropy())
                                    .number(state.molarGibbsEnergy()).numbers(state.logFugacityCoefficientsView())
                                    .number(derivatives.enthalpyTemperatureDerivative());
                            System.out.println(key + "engine: N " + s.hex() + " hig=" + state.idealGasEnthalpy());
                        } catch (RuntimeException refused) {
                            System.out.println(key + "engine: R " + refused);
                        }
                    }
                    String key = "PIN " + t + " " + p + " " + names[c] + " - ";
                    try {
                        double[] overall = new double[n + 1];
                        System.arraycopy(x, 0, overall, 0, n);
                        overall[n] = 0.1;
                        var state = model.flashTP(t, p, overall, () -> { });
                        var s = new Sink();
                        s.number(state.volume()).number(state.enthalpy()).number(state.internalEnergy())
                                .numbers(state.liquidView()).numbers(state.vaporView()).number(state.waterLiquid())
                                .number(state.waterVapor()).number(state.waterVolume()).number(model.velocityLimit(state));
                        System.out.println(key + "flash: N " + s.hex() + " h=" + state.enthalpy() + " wl=" + state.waterLiquid());
                    } catch (RuntimeException refused) {
                        System.out.println(key + "flash: R " + refused);
                    }
                }
            }
        }
    }

    private static final class Sink {
        private final MessageDigest digest;
        private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

        Sink() throws Exception { digest = MessageDigest.getInstance("SHA-256"); }

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

        String hex() { return HexFormat.of().formatHex(digest.digest()).substring(0, 12); }
    }
}
