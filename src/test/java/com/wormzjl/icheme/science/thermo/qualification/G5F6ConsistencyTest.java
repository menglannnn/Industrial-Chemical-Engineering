package com.wormzjl.icheme.science.thermo.qualification;

import static com.wormzjl.icheme.science.thermo.qualification.G5Support.BASIS;
import static com.wormzjl.icheme.science.thermo.qualification.G5Support.CO2;
import static com.wormzjl.icheme.science.thermo.qualification.G5Support.R;
import static com.wormzjl.icheme.science.thermo.qualification.G5Support.SERVICE;
import static com.wormzjl.icheme.science.thermo.qualification.G5Support.z;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.thermo.phase.CubicPhaseEvaluator;
import com.wormzjl.icheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.icheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.icheme.science.thermo.phase.EquilibriumResult.Classification;
import com.wormzjl.icheme.science.thermo.phase.PhaseAmounts;
import com.wormzjl.icheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.icheme.science.thermo.phase.PhaseKind;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * G5 family 6 (P5, batch 2026-09-24-coolprop-low-temperature): consistency of the crystal beside a liquid, where no
 * dataset measures amounts or heats (the survey). At 20 liquid states 3 K below their freezing points: conservation to
 * 1e-13, every fluid phase at the crystal's chemical potential to 1e-9 RT, PH and UV round trips (temperature, pressure and
 * the crystal to 1e-9), reversibility across the freezing onset (PH above it and back reproduces the answer), and the crystal
 * vanishing continuously at the onset. The four states of the WP5 pilot flash grid whose fluid-only answer indicates a third
 * fluid phase (the 5 % CO2 feed in liquid methane with N2 and ethane at 100 to 150 K and 0.1 to 0.3 MPa; P4's
 * THREE_PHASES holds) are answered. The engine's own three-phase point of a binary (PH through its step, the Newton
 * on the three-phase conditions) agrees with the independent bisection of G5 family 3 on the TP answers.
 */
class G5F6ConsistencyTest {
    /** Liquid feeds (N2, CH4, C2H6, CO2) and pressures: binaries, N2 and C2H6 ternaries, dense N2. */
    static final double[][] STATES = {
            {0, .998, 0, .002, 1e6}, {0, .998, 0, .002, 5e6}, {0, .99, 0, .01, 3e6}, {0, .99, 0, .01, 8e6}, {0, .97, 0, .03, 4e6},
            {0, .97, 0, .03, 8e6}, {0, .95, 0, .05, 6e6}, {0, .9, 0, .1, 9e6}, {.05, .945, 0, .005, 3e6}, {.05, .93, 0, .02, 5e6},
            {.1, .88, 0, .02, 7e6}, {0, .891, .099, .01, 2e6}, {0, .855, .095, .05, 4e6}, {0, .5, .45, .05, 2e6}, {0, .3, .6, .1, 2e6},
            {0, 0, .9, .1, 1e6}, {0, 0, .7, .3, 3e6}, {.99, 0, 0, .01, 7e6}, {.95, 0, 0, .05, 9e6}, {0, .1, .5, .4, 5e6}};

    static double worstMu(EquilibriumResult r, double t) {
        double solid = Double.NaN, worst = 0;
        for (PhaseAmounts phase : r.phases()) if (phase.kind() == PhaseKind.SOLID) solid = phase.state().molarGibbsEnergy();
        for (PhaseAmounts phase : r.phases()) {
            if (phase.kind() != PhaseKind.SOLID) worst = Math.max(worst, Math.abs(phase.state().chemicalPotential(CO2) - solid) / (R * t));
        }
        return worst;
    }

    static double crystal(EquilibriumResult r) {
        double s = 0;
        for (PhaseAmounts phase : r.phases()) if (phase.kind() == PhaseKind.SOLID) s += phase.amount(CO2);
        return s;
    }

    @Test
    void conservationChemicalPotentialsRoundTripsAndReversibilityAtTwentyLiquids() {
        var ws = SERVICE.newWorkspace();
        double worstConservation = 0, worstMu = 0, worstRound = 0, worstReversal = 0;
        System.out.println("feed | P MPa | T_f K | fluid at T_f | TP at T_f - 3 K | crystal mol | |dmu|/RT | PH T err | UV T err | UV P err | crystal err");
        for (double[] s : STATES) {
            double[] feed = z(s[0], s[1], s[2], s[3]);
            double p = s[4];
            var onset = SERVICE.freezingTemperature(null, p, BASIS, feed, ws);
            assertTrue(onset.converged(), onset.detail());
            double tf = onset.temperature(), t = tf - 3;
            assertEquals(1, onset.fluid().phases().size(), "a liquid at its freezing point: " + Arrays.toString(s) + " " + onset.fluid());
            var r = SERVICE.tp(EquilibriumRequest.tp(t, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            assertTrue(r.converged() && r.classification().holdsCrystal(), Arrays.toString(s) + ": " + r);
            double conservation = r.conservationDefect(feed), mu = worstMu(r, t), solid = crystal(r);
            worstConservation = Math.max(worstConservation, conservation);
            worstMu = Math.max(worstMu, mu);
            // PH and UV round trips.
            var ph = SERVICE.ph(EquilibriumRequest.ph(p, r.enthalpy(), BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var uv = SERVICE.uv(EquilibriumRequest.uv(r.internalEnergy(), r.volume(), BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            assertTrue(ph.converged() && uv.converged(), ph + " / " + uv);
            double phT = Math.abs(ph.phases().get(0).temperature() / t - 1), uvT = Math.abs(uv.phases().get(0).temperature() / t - 1);
            double uvP = Math.abs(uv.phases().get(0).pressure() / p - 1);
            double crystalError = Math.max(Math.abs(crystal(ph) - solid), Math.abs(crystal(uv) - solid)) / s[3];
            worstRound = Math.max(worstRound, Math.max(Math.max(phT, uvT), Math.max(uvP, crystalError)));
            // Reversibility across the onset: above it no crystal, back below the same answer.
            double hAbove = SERVICE.tp(EquilibriumRequest.tp(tf + 3, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws).enthalpy();
            var above = SERVICE.ph(EquilibriumRequest.ph(p, hAbove, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            assertTrue(above.converged() && crystal(above) == 0.0, above.toString());
            var back = SERVICE.ph(EquilibriumRequest.ph(p, r.enthalpy(), BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            worstReversal = Math.max(worstReversal, Math.max(Math.abs(back.phases().get(0).temperature() / ph.phases().get(0).temperature() - 1),
                    Math.abs(crystal(back) - crystal(ph)) / s[3]));
            // The crystal vanishes continuously at the onset.
            var justBelow = SERVICE.tp(EquilibriumRequest.tp(tf - 1e-4, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            assertTrue(crystal(justBelow) <= 1e-3 * s[3], "the crystal just below the onset: " + justBelow);
            System.out.printf(Locale.ROOT, "%s | %.0f | %.4f | %s | %s | %.6g | %.1e | %.1e | %.1e | %.1e | %.1e%n", Arrays.toString(Arrays.copyOf(s, 4)), p / 1e6, tf,
                    onset.fluid().classification(), r.classification(), solid, mu, phT, uvT, uvP, crystalError);
        }
        System.out.printf(Locale.ROOT, "20 liquids: conservation %.1e (bound 1e-13), |mu - mu_s|/RT %.1e (1e-9), round trips %.1e (1e-9), reversal %.1e (1e-12)%n",
                worstConservation, worstMu, worstRound, worstReversal);
        assertTrue(worstConservation <= 1e-13 && worstMu <= 1e-9 && worstRound <= 1e-9 && worstReversal <= 1e-12,
                worstConservation + " / " + worstMu + " / " + worstRound + " / " + worstReversal);
    }

    /**
     * P4's four THREE_PHASES holds of the WP5 pilot flash grid (N2, CH4, C2H6, CO2 5/80/10/5 at 100 K 0.1 MPa, 120 K 0.1 and
     * 0.3 MPa, 150 K 0.3 MPa): answered on the crystal competition.
     */
    @Test
    void theGridsThreePhaseStatesAreAnswered() {
        // The grid of SpineNetworkPathTest (dry feeds carrying CO2): the fluid answer P4's network saw is the fluid answer with
        // the crystals held out; where it indicates a third fluid phase, P4 held the state typed (THREE_PHASES).
        var ws = SERVICE.newWorkspace();
        double[][] feeds = {{0, 0, 0, 1}, {0, 0.8, 0, 0.2}, {0.2, 0, 0, 0.8}, {0, 0, 0.6, 0.4}, {0.05, 0.8, 0.1, 0.05}};
        int threeFluid = 0;
        for (double t : new double[] {100, 120, 150, 180, 230, 260, 300}) {
            for (double p : new double[] {1.0e5, 3.0e5, 1.0e6, 2.0e6, 5.0e6, 1.0e7}) {
                for (double[] f : feeds) {
                    double[] feed = z(f[0], f[1], f[2], f[3]);
                    var fluidOnly = SERVICE.tp(EquilibriumRequest.tp(t, p, BASIS, feed, PhaseCompetition.FLUID_ONLY), ws);
                    if (fluidOnly.converged() || !fluidOnly.detail().contains(EquilibriumResult.UNSTABLE_PRODUCT)) continue;
                    threeFluid++;
                    var r = SERVICE.tp(EquilibriumRequest.tp(t, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.printf(Locale.ROOT, "%.0f K, %.1f MPa, %s: fluid answer %s; crystal competition %s%n", t, p / 1e6, Arrays.toString(f),
                            fluidOnly.status(), r);
                    assertTrue(r.converged() && r.classification().holdsCrystal(), r.toString());
                    assertTrue(r.conservationDefect(feed) <= 1e-13 && worstMu(r, t) <= 1e-9, r.toString());
                    // PH and UV round trips of the vapour-liquid-crystal answer, and the crystal gone above the whole feed's onset.
                    var ph = SERVICE.ph(EquilibriumRequest.ph(p, r.enthalpy(), BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    var uv = SERVICE.uv(EquilibriumRequest.uv(r.internalEnergy(), r.volume(), BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    assertTrue(ph.converged() && uv.converged(), ph + " / " + uv);
                    double round = Math.max(Math.abs(ph.phases().get(0).temperature() / t - 1), Math.abs(uv.phases().get(0).temperature() / t - 1));
                    round = Math.max(round, Math.abs(uv.phases().get(0).pressure() / p - 1));
                    round = Math.max(round, Math.max(Math.abs(crystal(ph) - crystal(r)), Math.abs(crystal(uv) - crystal(r))) / feed[CO2]);
                    var onset = SERVICE.depositionTemperature(null, p, BASIS, feed, ws);
                    assertTrue(onset.converged(), onset.detail());
                    var above = SERVICE.tp(EquilibriumRequest.tp(onset.temperature() + 0.01, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    var below = SERVICE.tp(EquilibriumRequest.tp(onset.temperature() - 1e-4, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.printf(Locale.ROOT, "  %s and %s: round trips %.1e; the whole feed's onset %.4f K (%s): above it %s, crystal just below %.3e mol%n",
                            ph.classification(), uv.classification(), round, onset.temperature(), onset.fluid().classification(), above.classification(), crystal(below));
                    assertTrue(round <= 1e-9, "round trips " + round);
                    assertEquals(0.0, crystal(above), "no crystal above the onset");
                    assertTrue(crystal(below) <= 1e-3 * feed[CO2], "the crystal vanishes continuously at the onset");
                }
            }
        }
        System.out.println("fluid-only answers with a third fluid phase on the grid: " + threeFluid);
        assertEquals(4, threeFluid, "the grid's four THREE_PHASES states of P4 stage 2b");
    }

    /** The engine's three-phase point of a binary (PH through its step) against the bisection on its TP answers (G5 family 3). */
    @Test
    void theEnginesThreePhasePointAgreesWithTheBisection() {
        var ws = SERVICE.newWorkspace();
        double[] feed = z(0, 0.97, 0, 0.03);
        for (double p : new double[] {1e6, 2e6, 3e6}) {
            // Enthalpies either side of the step at this pressure: the three-phase answer from the energy balance between them.
            double t3 = Double.NaN;
            double[] three = null;
            for (double t = 120; t < 200; t += 0.5) {
                var lo = SERVICE.tp(EquilibriumRequest.tp(t, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                var hi = SERVICE.tp(EquilibriumRequest.tp(t + 0.5, p, BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                if (G5Support.hasLiquid(lo) != G5Support.hasLiquid(hi) && (lo.classification().holdsCrystal() || hi.classification().holdsCrystal())
                        && (lo.classification() == Classification.VAPOR_SOLID || hi.classification() == Classification.VAPOR_SOLID)) {
                    var r = SERVICE.ph(EquilibriumRequest.ph(p, 0.5 * (lo.enthalpy() + hi.enthalpy()), BASIS, feed, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    assertEquals(Classification.VAPOR_LIQUID_SOLID, r.classification(), r.toString());
                    assertTrue(r.diagnostics().coexistenceFromBalance());
                    assertEquals(0.0, r.conservationDefect(feed), 1e-14);
                    t3 = r.phases().get(0).temperature();
                    three = G5Support.threePhase(SERVICE, t3, 1, ws);
                    assertNotNull(three);
                    double x = r.phases().get(1).state().moleFraction(CO2), y = r.phases().get(0).state().moleFraction(CO2);
                    System.out.printf(Locale.ROOT, "P %.1f MPa: engine T3 %.6f K (x %.6f, y %.6f); bisection at T3: P %.3f Pa (x %.6f, y %.6f)%n", p / 1e6, t3, x, y,
                            three[0], three[1], three[2]);
                    assertEquals(p, three[0], 1e-7 * p, "the three-phase pressure");
                    assertEquals(x, three[1], 1e-6 * x);
                    assertEquals(y, three[2], 1e-6 * y);
                    break;
                }
            }
            assertTrue(Double.isFinite(t3), "a three-phase step at " + p + " Pa");
        }
    }
}
