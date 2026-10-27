package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import org.junit.jupiter.api.Test;

/** Probe: pure liquid water's UV round trip at 320 K, 1 MPa of FluidTpEquilibriumPhUvTest (range-extrapolation batch). */
class PureWaterUvProbe {
    @Test
    void trace() {
        var fluid = FluidTestSupport.networkModel();
        var service = PhaseTestSupport.networkService(PhaseTestSupport.networkFreeWater(fluid));
        var contract = service.contract();
        var ws = service.newWorkspace();
        double[] pure = new double[contract.components().size()];
        pure[contract.waterIndex()] = 1.5;
        for (double[] s : new double[][] {{320, 1e6}, {300, 1e6}, {350, 2e6}}) {
            var tp = service.tp(EquilibriumRequest.tp(s[0], s[1], contract.components(), pure, PhaseCompetition.FLUID_ONLY), ws);
            var uv = service.uv(EquilibriumRequest.uv(tp.internalEnergy(), tp.volume(), contract.components(), pure, PhaseCompetition.FLUID_ONLY), ws);
            var w = fluid.waterLiquid(s[0], s[1]);
            System.out.printf(java.util.Locale.ROOT, "PROBE %.0f K %.1f MPa: v %.17g h %.17g | UV dT/T %.3e dP/P %.3e outer %d%n", s[0], s[1] / 1e6,
                    w.molarVolume(), w.molarEnthalpy(), uv.phases().get(0).temperature() / s[0] - 1, uv.phases().get(0).pressure() / s[1] - 1,
                    uv.diagnostics().outerIterations());
        }
    }
}
