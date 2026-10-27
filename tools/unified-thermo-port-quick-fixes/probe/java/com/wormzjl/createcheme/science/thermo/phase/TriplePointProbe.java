package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Probe (not tracked; tools/unified-thermo-port-quick-fixes/probe): UV targets of 1 mol of pure water around its triple
 * point, from the corners of the triangle (steam, liquid, ice at the ice record's anchor): inside at ice fractions 0.1,
 * 0.5, 0.9 with a little steam, and just outside each edge (no ice: liquid and steam above T_t; no liquid: ice and steam
 * below; no steam: ice and liquid compressed on the melting line), and the gate's vessel (FreeWaterIceTest: twice the liquid's volume at ice fractions 0.1, 0.5, 0.9).
 */
class TriplePointProbe {
    // The gate's vessel: 1 mol in twice its liquid's volume, ice fraction beta, the steam the volume leaves.
    private static double[] gate(double beta, double vl, double vs, double vv) {
        double steam = (2.0 * vl - vl - beta * (vs - vl)) / (vv - vl);
        return new double[] {steam, 1.0 - steam - beta, beta};
    }

    @Test void probe() {
        var service = PhaseTestSupport.networkService(PhaseTestSupport.networkFreeWater(FluidTestSupport.networkModel()));
        var contract = service.contract();
        var workspace = service.newWorkspace();
        List<String> species = contract.components();
        int water = contract.waterIndex();
        var model = service.freeWater();
        var ice = model.ice();
        double t = ice.tripleTemperature();
        double p = ice.anchored().record().anchor().pressure();
        double[] liquid = new double[2];
        model.liquid(t, p, liquid);
        var solid = ice.properties(t, p);
        double r = PhaseTestSupport.R;
        double uv = model.vapor().enthalpy(t) - r * t, vv = r * t / p;
        double ul = liquid[1] - p * liquid[0], vl = liquid[0];
        double us = solid.enthalpy() - p * solid.volume(), vs = solid.volume();
        System.out.printf(Locale.ROOT, "TRIPLE corners at %.4f K, %.4f Pa (p_sat %.6f Pa): steam u %.4f v %.6e, liquid u %.4f v %.6e, ice u %.4f v %.6e%n",
                t, p, model.saturationPressure(t), uv, vv, ul, vl, us, vs);
        double[] pure = new double[species.size()];
        pure[water] = 1.0;
        // (steam, liquid, ice) amounts of 1 mol; negative ones put the target outside the triangle.
        double[][] cases = {{1e-5, 0.9 - 1e-5, 0.1}, {1e-5, 0.5 - 1e-5, 0.5}, {1e-5, 0.1 - 1e-5, 0.9}, {0.2, 0.4, 0.4},
                {1e-5, 1.0 + 1e-3 - 1e-5, -1e-3}, {1e-5, -1e-3, 1.0 + 1e-3 - 1e-5}, {-1e-7, 0.5 + 1e-7, 0.5},
                {1e-5, 1.0 + 1e-5 - 1e-5, -1e-5}, {1e-5, -1e-5, 1.0 + 1e-5 - 1e-5}, {-1e-10, 0.5 + 1e-10, 0.5}, {-1e-12, 0.5 + 1e-12, 0.5},
                gate(0.1, vl, vs, vv), gate(0.5, vl, vs, vv), gate(0.9, vl, vs, vv)};
        String[] labels = {"inside ice 0.1", "inside ice 0.5", "inside ice 0.9", "inside steam 0.2",
                "outside: no ice (liquid 1.001)", "outside: no liquid (ice 1.001)", "outside: no steam (-1e-7)", "outside: no ice (-1e-5)", "outside: no liquid (-1e-5)", "outside: no steam (-1e-10)", "within the specification: steam -1e-12",
                "gate vessel (twice the liquid volume) ice 0.1", "gate vessel ice 0.5", "gate vessel ice 0.9"};
        for (int i = 0; i < cases.length; i++) {
            double[] a = cases[i];
            double u = a[0] * uv + a[1] * ul + a[2] * us;
            double v = a[0] * vv + a[1] * vl + a[2] * vs;
            var result = service.uv(EquilibriumRequest.uv(u, v, species, pure, PhaseCompetition.FLUID_ONLY), workspace);
            String what = result.converged() ? String.format(Locale.ROOT, "%.6f K, %.4f Pa, steam %.6e liquid %.8f ice %.8f, residual %.2e",
                    result.phases().get(0).temperature(), result.phases().get(0).pressure(), result.freeWater().waterVapor(),
                    result.freeWater().liquidWater(), result.freeWater().ice(), result.diagnostics().specificationResidual())
                    : result.detail();
            System.out.printf(Locale.ROOT, "TRIPLE %s: %s %s %s (%d TP calls)%n", labels[i], result.status(), result.classification(), what,
                    result.diagnostics().outerIterations());
        }
    }
}
