package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;

/** Cold seed 183's refused crystal answer, replayed as a fresh dry TP request on the pilot package (quick-check fixes). */
class Seed183CrystalProbe {
    @Test void replay() {
        var service = FluidTpEquilibrium.forPackage(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
        var basis = service.contract().components();
        int co2 = basis.indexOf("CarbonDioxide");
        double t = 140.90028704290555, p = 619923.0283660815;
        double[] z = {0.17685241566161913, 54.73778258807344, 2.531240191239479, 2.0505124775177914, 0.0};
        var r = service.tp(EquilibriumRequest.tp(t, p, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS));
        System.out.println("PROBE183 status=" + r.status() + " class=" + r.classification() + " detail=" + r.detail());
        if (r.deposition() != null) System.out.println("PROBE183 deposition residual=" + r.deposition().residual() + " solid=" + r.deposition().solidAmount() + " iterations=" + r.deposition().iterations());
        for (PhaseAmounts phase : r.phases()) System.out.println("PROBE183 phase " + phase.kind() + " total=" + phase.total() + " mu_co2/RT=" + phase.state().chemicalPotential(co2) / (PhaseTestSupport.R * t));
    }
}
