package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import org.junit.jupiter.api.Test;

/** Probe: the wet crude UV round trips at 460 K, 1.4 MPa of FluidTpEquilibriumPhUvTest (range-extrapolation batch). */
class WetUvRoundTripProbe {
    @Test
    void trace() {
        var service = PhaseTestSupport.networkService(PhaseTestSupport.networkFreeWater(FluidTestSupport.networkModel()));
        var contract = service.contract();
        var ws = service.newWorkspace();
        int water = contract.waterIndex();
        for (double ratio : new double[] {0.01, 1.0}) {
            for (double t : new double[] {340.0, 460.0, 580.0}) {
                for (double p : new double[] {0.5e6, 1.4e6}) {
                    double[] amounts = PhaseTestSupport.crudeWithNitrogen(contract);
                    amounts[water] = ratio * PhaseTestSupport.sum(amounts);
                    var tp = service.tp(EquilibriumRequest.tp(t, p, contract.components(), amounts, PhaseCompetition.FLUID_ONLY), ws);
                    var uv = service.uv(EquilibriumRequest.uv(tp.internalEnergy(), tp.volume(), contract.components(), amounts, PhaseCompetition.FLUID_ONLY), ws);
                    var fw = tp.freeWater();
                    System.out.printf(java.util.Locale.ROOT, "PROBE ratio %.2f %.0f K %.1f MPa: %s %s liquid water %.4g steam %.4g p_w %.4g psat %.4g | UV %s dT/T %.2e dP/P %.2e outer %d%n",
                            ratio, t, p / 1e6, tp.classification(), tp.coverage().grade(), fw == null ? 0 : fw.liquidWater(), fw == null ? 0 : fw.waterVapor(),
                            fw == null ? 0 : fw.waterPartialPressure(), fw == null ? 0 : fw.saturationPressure(), uv.status(),
                            uv.converged() ? uv.phases().get(0).temperature() / t - 1 : Double.NaN, uv.converged() ? uv.phases().get(0).pressure() / p - 1 : Double.NaN,
                            uv.diagnostics().outerIterations());
                }
            }
        }
    }
}
