package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Probe (not tracked; tools/unified-thermo-port-quick-fixes/probe): UV targets of pure water across its freeze. (a) a
 * rigid vessel twice the liquid's volume (liquid and its steam, no gas) with energy removed: the triple point, ice, liquid
 * and steam at 273.16 K and 611.657 Pa; (c, d) a liquid-full rigid vessel (the liquid's volume at 273.16 K and 1 bar)
 * with 2000 and 5500 J/mol removed: freezing pressurises it along the melting line (ice 9 % larger, liquid
 * compressibility 5e-10/Pa: about 2.3e8 Pa per unit ice fraction).
 */
class FreezeUvProbe {
    @Test void probe() {
        var service = PhaseTestSupport.networkService(PhaseTestSupport.networkFreeWater(FluidTestSupport.networkModel()));
        var contract = service.contract();
        var workspace = service.newWorkspace();
        List<String> species = contract.components();
        int water = contract.waterIndex();
        var model = service.freeWater();
        double[] pure = new double[species.size()];
        pure[water] = 1.0;
        double[] liquid = new double[2];
        model.liquid(273.16, 1.0e5, liquid);
        double ul = liquid[1] - 1.0e5 * liquid[0];
        double[][] cases = {{2.0 * liquid[0], ul - 2000.0}, {liquid[0], ul - 2000.0}, {liquid[0], ul - 5500.0}, {liquid[0], ul - 6500.0}};
        String[] labels = {"a twice the liquid's volume, -2000 J", "c liquid-full, -2000 J", "d liquid-full, -5500 J", "e liquid-full, -6500 J"};
        for (int i = 0; i < cases.length; i++) {
            var uv = service.uv(EquilibriumRequest.uv(cases[i][1], cases[i][0], species, pure, PhaseCompetition.FLUID_ONLY), workspace);
            String phases = uv.converged() ? uv.phases().get(0).temperature() + " K, " + uv.phases().get(0).pressure() + " Pa, "
                    + (uv.freeWater() == null ? "" : "ice " + uv.freeWater().ice() + " liquid " + uv.freeWater().liquidWater()
                    + " steam " + uv.freeWater().waterVapor()) : "";
            System.out.printf(Locale.ROOT, "FREEZEUV %s: %s %s %s (%d TP calls) %s%n", labels[i], uv.status(), uv.classification(),
                    phases, uv.diagnostics().outerIterations(), uv.converged() ? "" : uv.detail());
        }
    }
}
