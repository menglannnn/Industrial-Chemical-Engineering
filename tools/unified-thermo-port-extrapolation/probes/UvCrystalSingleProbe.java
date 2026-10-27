package com.wormzjl.createcheme.science.thermo.phase;

import org.junit.jupiter.api.Test;

/** Probe: one VAPOR_SOLID UV round trip (y 0.1, 150 K, 1 MPa) of GasSolidEquilibriumTest(c), traced with -Duvtrace. */
class UvCrystalSingleProbe {
    @Test
    void trace() {
        var service = GasSolidEquilibriumTest.SERVICE;
        var ws = service.newWorkspace();
        double[] z = GasSolidEquilibriumTest.inNitrogen(0.1);
        var r = GasSolidEquilibriumTest.tp(150, 1.0e6, z, ws);
        System.out.println("PROBE tp " + r.classification() + " u " + r.internalEnergy() + " v " + r.volume());
        var uv = service.uv(EquilibriumRequest.uv(r.internalEnergy(), r.volume(), GasSolidEquilibriumTest.BASIS, z,
                GasSolidEquilibriumTest.CRYSTALS), ws);
        System.out.println("PROBE uv " + uv.status() + " " + uv.classification() + " outer " + uv.diagnostics().outerIterations() + " " + uv.detail());
    }
}
