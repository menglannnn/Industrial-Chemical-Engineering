package com.wormzjl.icheme.science.thermo.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * G5 family 4 (P5, batch 2026-09-24-coolprop-low-temperature): the ternaries with N2 inside the pilot's domain. Each point
 * at its printed composition and pressure: a liquid by {@code freezingTemperature}, a vapour by {@code frostTemperature}
 * (the gas as one phase: the standard solid-vapour temperature at a measured vapour composition, which a three-phase
 * point's vapour is). Campestrini et al. 2022 (HAL manuscript: 4 MPa, 0, 5 and 10 % N2 in the solvent: SLE, SVE and the
 * SLVE liquids and vapours of Table 12, U(T) 0.004 K) and Riva and Stringari 2018 (Table 4, series 1 and 2 with N2; series
 * 3 and 4 carry O2, absent from the pilot, and are skipped).
 *
 * <p>P0 target 2 K (dataset MAD). Measured 2026-09-25 (9019 J/mol): Campestrini 0.560 K (max 1.765 K), met; Riva and
 * Stringari 2.642 K (liquids 3.69 K, max 6.73 K; vapours 1.59 K), missed: their series 2 liquids (about 9 % N2) at 124.5 and
 * 132.1 K are 6.7 K warmer than this family's liquidus, whose solubility there agrees with Shen 2012 and Gao 2012 at the
 * same solvent (Riva's 532 and 1185 ppm against Shen's 909 and about 1700 ppm): a dataset the others contradict, bounded
 * at what it measures (D17's rule for an inconsistent set).</p>
 */
class G5F4TernarySolidLiquidVapourTest {
    static final double TARGET = 2.0;
    static final double CAMPESTRINI_MAD = 0.8, CAMPESTRINI_MAX = 2.2;
    static final double RIVA_MAD = 3.0, RIVA_MAX = 7.5;

    @Test
    void campestriniAndRivaStringariWithNitrogen() {
        var campestrini = G5Support.campestrini();
        var riva = G5Support.rivaStringari();
        assertEquals(67, campestrini.size());
        assertEquals(12, riva.size());
        var points = new java.util.ArrayList<>(campestrini);
        points.addAll(riva);
        var datasets = G5Support.summary("G5F4 ternaries", G5Support.score(G5Support.SERVICE, points, true), TARGET);
        for (var e : datasets.entrySet()) {
            var s = e.getValue();
            System.out.printf(Locale.ROOT, "  %s: MAD %.3f K against the P0 target %.1f K: %s%n", e.getKey(), s.mad(), TARGET, s.mad() <= TARGET ? "met" : "missed");
            assertTrue(s.holds.isEmpty() && s.extrapolated.isEmpty() && s.failures.isEmpty(), s.holds + " " + s.extrapolated + " " + s.failures);
        }
        var c = datasets.get("Campestrini 2022");
        var r = datasets.get("Riva-Stringari 2018");
        assertTrue(c.mad() <= CAMPESTRINI_MAD && c.max() <= CAMPESTRINI_MAX, "Campestrini " + c.mad() + " / " + c.max());
        assertTrue(r.mad() <= RIVA_MAD && r.max() <= RIVA_MAX, "Riva-Stringari " + r.mad() + " / " + r.max());
    }
}
