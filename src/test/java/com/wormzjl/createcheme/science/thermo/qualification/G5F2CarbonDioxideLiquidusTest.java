package com.wormzjl.createcheme.science.thermo.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * G5 family 2 (P5, batch 2026-09-24-coolprop-low-temperature): the CO2 liquidus in liquid methane, the freezing
 * temperature of a liquid at its printed composition and pressure ({@code freezingTemperature}: the liquid as one phase,
 * the standard solid-liquid solubility temperature) against Shen et al. 2012 (NIST TRC ThermoML: CH4 + CO2, 112 to 170 K,
 * and with CH4 + N2 and CH4 + C2H6 solvents of 90 to 98 % CH4; the survey's open liquidus) and Davis, Rodewald and Kurata
 * 1962 Table 4 (the crystal points of 11 liquids, 0.16 to 20.5 % CO2, at the locus pressure of their temperature, their
 * Table 1).
 *
 * <p>P0 target: 2 K (the survey's correction of the P0 row: a dataset MAD, D17's rule). Measured 2026-09-25 on the pilot
 * family (9019 J/mol): Shen 2.039 K (bias -2.04 K: the liquidus lies about 2 K cold, the solubility about 25 % high,
 * every group), Davis 1.902 K; the P0 target is met by Davis and missed by 0.04 K by Shen. The bounds are the measured
 * values with a margin (the declared-error grade of this family); with 8875 J/mol Shen scores 2.594 K and Davis 2.148 K
 * (the stage document's fusion-enthalpy comparison).</p>
 */
class G5F2CarbonDioxideLiquidusTest {
    static final double TARGET = 2.0;
    /** Per dataset at what is measured with a margin: Shen 2.039 K, Davis 1.902 K. */
    static final double SHEN_MAD = 2.2;
    static final double DAVIS_MAD = 2.1;
    /** Per point: measured 3.667 K (Shen, the solvent with 5 % N2). */
    static final double POINT_MAX = 4.0;

    @Test
    void liquidusOfCarbonDioxideInLiquidMethaneAndItsSolvents() {
        List<G5Support.Point> points = new ArrayList<>(G5Support.shen());
        points.addAll(G5Support.davisCrystalPoints());
        assertEquals(63 + 11, points.size(), "Shen's 9 + 27 + 27 and Davis's 11");
        var scores = G5Support.score(G5Support.SERVICE, points, true);
        var datasets = G5Support.summary("G5F2 liquidus", scores, TARGET);
        var all = new G5Support.Score();
        datasets.values().forEach(all::add);
        System.out.printf(Locale.ROOT, "G5F2 pooled: %d points, MAD %.3f K, bias %+.3f K, max %.3f K (P0 target %.1f K: %s)%n", all.deviations.size(), all.mad(),
                all.bias(), all.max(), TARGET, all.mad() <= TARGET ? "met" : "missed");
        for (var e : datasets.entrySet()) {
            System.out.printf(Locale.ROOT, "  %s: MAD %.3f K against the P0 target %.1f K: %s%n", e.getKey(), e.getValue().mad(), TARGET,
                    e.getValue().mad() <= TARGET ? "met" : "missed");
        }
        assertTrue(all.holds.isEmpty() && all.extrapolated.isEmpty() && all.failures.isEmpty(), all.holds + " " + all.extrapolated + " " + all.failures);
        assertTrue(datasets.get("Shen 2012").mad() <= SHEN_MAD, "Shen MAD " + datasets.get("Shen 2012").mad());
        assertTrue(datasets.get("Davis 1962").mad() <= DAVIS_MAD, "Davis MAD " + datasets.get("Davis 1962").mad());
        assertTrue(all.max() <= POINT_MAX, "max " + all.max());
    }
}
