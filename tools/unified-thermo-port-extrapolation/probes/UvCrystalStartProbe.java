package com.wormzjl.createcheme.science.thermo.phase;

import org.junit.jupiter.api.Test;

/** Probe: the VAPOR_SOLID round trips of GasSolidEquilibriumTest(c), printing each UV answer (range-extrapolation batch). */
class UvCrystalStartProbe {
    @Test
    void trace() {
        var service = GasSolidEquilibriumTest.SERVICE;
        var ws = service.newWorkspace();
        for (double y : new double[] {0.001, 0.01, 0.1, 0.3, 0.5}) {
            for (double[] state : new double[][] {{130, 1.0e5}, {150, 1.0e6}, {170, 5.0e5}, {190, 5.0e5}, {170, 2.0e6}, {210, 5.0e6}}) {
                double[] z = GasSolidEquilibriumTest.inNitrogen(y);
                var r = GasSolidEquilibriumTest.tp(state[0], state[1], z, ws);
                if (r.classification() != EquilibriumResult.Classification.VAPOR_SOLID) continue;
                var uv = service.uv(EquilibriumRequest.uv(r.internalEnergy(), r.volume(), GasSolidEquilibriumTest.BASIS, z,
                        GasSolidEquilibriumTest.CRYSTALS), ws);
                System.out.println("PROBE y " + y + " " + state[0] + " K " + state[1] + " Pa: " + uv.status() + " "
                        + uv.classification() + " outer " + uv.diagnostics().outerIterations() + " " + (uv.converged() ? "" : uv.detail()));
            }
        }
    }
}
