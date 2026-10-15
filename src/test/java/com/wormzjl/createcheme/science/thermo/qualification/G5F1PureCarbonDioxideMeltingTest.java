package com.wormzjl.createcheme.science.thermo.qualification;

import static com.wormzjl.createcheme.science.thermo.qualification.G5Support.BASIS;
import static com.wormzjl.createcheme.science.thermo.qualification.G5Support.SERVICE;
import static com.wormzjl.createcheme.science.thermo.qualification.G5Support.z;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.createcheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult.Classification;
import com.wormzjl.createcheme.science.thermo.phase.PhaseAmounts;
import com.wormzjl.createcheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.createcheme.science.thermo.phase.PhaseKind;
import com.wormzjl.createcheme.science.thermo.phase.SolidCarbonDioxideReferences;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * G5 family 1 (P5, batch 2026-09-24-coolprop-low-temperature): pure CO2 melting on the pilot package, the first P5 step of
 * the plan. The melting temperature ({@code freezingTemperature} of pure CO2) against Span and Wagner 1996 equation 3.10
 * (the CoolProp melting-line ancillary, {@link SolidCarbonDioxideReferences}) from the triple point to the 10 MPa ceiling:
 * D16 declared 0.13 K (P4 stage 1 measured 0.125 K; kept). The melting step in PH from the energy balance (the crystal
 * beside its liquid), the family's own triple point, and the pure triple point in UV from the mole, energy and volume
 * balances.
 */
class G5F1PureCarbonDioxideMeltingTest {
    /** D16's declared melting error (P4 stage 1: 0.125 K at 10 MPa). */
    static final double MELTING_BOUND = 0.13;

    static double spanWagnerMeltingTemperature(double p) {
        double lo = SolidCarbonDioxideReferences.TRIPLE_TEMPERATURE, hi = 230;
        for (int k = 0; k < 200; k++) {
            double mid = 0.5 * (lo + hi);
            if (SolidCarbonDioxideReferences.spanWagnerMeltingPressure(mid) < p) lo = mid; else hi = mid;
        }
        return 0.5 * (lo + hi);
    }

    @Test
    void meltingTemperatureAgainstSpanWagnerToTheCeiling() {
        var ws = SERVICE.newWorkspace();
        double[] pure = z(0, 0, 0, 1);
        double worst = 0;
        System.out.println("P MPa | T_m model K | T_m Span-Wagner 3.10 K | model - SW K | fluid at the onset");
        for (double p : new double[] {0.6e6, 1e6, 2e6, 3e6, 4e6, 5e6, 6e6, 7e6, 8e6, 9e6, 1e7}) {
            var onset = SERVICE.freezingTemperature(null, p, BASIS, pure, ws);
            assertTrue(onset.converged(), onset.detail());
            double sw = spanWagnerMeltingTemperature(p);
            double d = onset.temperature() - sw;
            worst = Math.max(worst, Math.abs(d));
            System.out.printf(Locale.ROOT, "%.1f | %.6f | %.6f | %+.4f | %s%n", p / 1e6, onset.temperature(), sw, d, onset.fluid().classification());
            // TP either side of the melting point: the crystal alone below, the liquid above.
            var solid = SERVICE.tp(EquilibriumRequest.tp(onset.temperature() - 0.05, p, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var liquid = SERVICE.tp(EquilibriumRequest.tp(onset.temperature() + 0.05, p, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            assertEquals(Classification.SOLID_PRESENT, solid.classification(), solid.toString());
            assertEquals(PhaseKind.LIQUID, liquid.phases().get(0).kind(), liquid.toString());
        }
        System.out.printf(Locale.ROOT, "pure CO2 melting to 10 MPa: worst |T_model - T_SW| %.4f K (declared %.2f K, D16)%n", worst, MELTING_BOUND);
        assertTrue(worst <= MELTING_BOUND, "melting " + worst);
    }

    /** The melting step in PH: the crystal beside its liquid from the energy balance at the melting temperature. */
    @Test
    void theMeltingStepInPhFromTheEnergyBalance() {
        var ws = SERVICE.newWorkspace();
        double[] pure = z(0, 0, 0, 2);
        System.out.println("P MPa | T_m K | fusion enthalpy h_L - h_s J/mol | PH liquid fraction checks");
        for (double p : new double[] {1e6, 5e6, 1e7}) {
            double tm = SERVICE.freezingTemperature(null, p, BASIS, pure, ws).temperature();
            var solid = SERVICE.tp(EquilibriumRequest.tp(tm - 0.5, p, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var liquid = SERVICE.tp(EquilibriumRequest.tp(tm + 0.5, p, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            double fusion = Double.NaN;
            for (double beta : new double[] {0.1, 0.5, 0.9}) {
                double h = solid.enthalpy() + beta * (liquid.enthalpy() - solid.enthalpy());
                var r = SERVICE.ph(EquilibriumRequest.ph(p, h, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                if (r.classification() == Classification.LIQUID_SOLID) {
                    assertTrue(r.diagnostics().coexistenceFromBalance(), r.toString());
                    assertEquals(tm, r.phases().get(0).temperature(), 1e-8, "the balance's temperature is the melting point");
                    assertEquals(h, r.enthalpy(), 1e-9 * Math.abs(h));
                    assertEquals(0.0, r.conservationDefect(pure), 1e-15);
                    PhaseAmounts l = r.phases().get(0), s = r.phases().get(1);
                    fusion = l.state().molarEnthalpy() - s.state().molarEnthalpy();
                } else {
                    // Outside the step's enthalpy range the answer is one phase beside it (the heat capacities of solid and liquid).
                    assertTrue(r.converged(), r.toString());
                }
            }
            System.out.printf(Locale.ROOT, "%.1f | %.6f | %.2f%n", p / 1e6, tm, fusion);
            assertTrue(fusion > 8000 && fusion < 10000, "fusion enthalpy " + fusion);
        }
    }

    /** The family's own triple point and the pure triple point in UV from the three balances. */
    @Test
    void theFamilysTriplePointAndThePureTriplePointInUv() {
        var ws = SERVICE.newWorkspace();
        double[] pure = z(0, 0, 0, 1);
        // A vessel of one mole inside the triple point's triangle: the UV answer is vapour, liquid and crystal at one state.
        double v = 3.0e-4;
        var vapourEnd = SERVICE.uv(EquilibriumRequest.uv(-4.10e5, v, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        var r = SERVICE.uv(EquilibriumRequest.uv(-4.1337e5, v, BASIS, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        System.out.println("pure CO2 UV in the triple triangle: " + r + " (" + r.diagnostics().outerIterations() + " TP equilibria)");
        System.out.println("pure CO2 UV above it: " + vapourEnd);
        assertEquals(Classification.VAPOR_LIQUID_SOLID, r.classification(), r.toString());
        assertEquals(3, r.phases().size());
        assertTrue(r.diagnostics().coexistenceFromBalance());
        assertEquals(-4.1337e5, r.internalEnergy(), 1e-9 * 4.1337e5);
        assertEquals(v, r.volume(), 1e-12 * v);
        assertEquals(0.0, r.conservationDefect(pure), 1e-14);
        double t = r.phases().get(0).temperature(), p = r.phases().get(0).pressure();
        // The family's own triple point (216.5916 K, 516.05 kPa: the cubic's saturation pressure there is 0.34 % below Span and
        // Wagner's 517.95 kPa, P4 stage 1) is where its melting and sublimation lines meet.
        var melting = SERVICE.freezingTemperature(null, p * (1 + 1e-6), BASIS, pure, ws);
        var sublimation = SERVICE.depositionTemperature(null, p * (1 - 1e-6), BASIS, pure, ws);
        System.out.printf(Locale.ROOT, "family triple point %.6f K, %.2f Pa; melting just above %.6f K, sublimation just below %.6f K%n", t, p,
                melting.temperature(), sublimation.temperature());
        assertEquals(t, melting.temperature(), 1e-3);
        assertEquals(t, sublimation.temperature(), 1e-3);
        assertEquals(SolidCarbonDioxideReferences.TRIPLE_TEMPERATURE, t, 0.01);
        assertEquals(SolidCarbonDioxideReferences.TRIPLE_PRESSURE, p, 0.005 * p);
    }
}
