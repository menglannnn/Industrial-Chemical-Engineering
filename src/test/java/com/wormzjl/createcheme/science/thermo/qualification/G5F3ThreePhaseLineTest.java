package com.wormzjl.createcheme.science.thermo.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * G5 family 3 (P5, batch 2026-09-24-coolprop-low-temperature): the CO2 + CH4 solid-liquid-vapour line. At each measured
 * temperature the pilot's three-phase pressure (where the TP answer of a 95 % CO2 feed changes from the crystal beside a
 * vapour to the crystal beside a liquid, by bisection on the engine's own answers) against Davis, Rodewald and Kurata 1962
 * Table 1 (31 points, 97.5 to 211.7 K, 0.028 to 4.87 MPa; accurate to 2 psi) and Souza et al. 2020 (7 points, 204 to
 * 216 K, 0.96 to 4.86 MPa, U95 70 kPa); the vapour and liquid compositions on the line against Davis Tables 3 and 4
 * (8 and 11 points; Kurata-Im 1971 Table VI interpolates them, report only in G5 family 5).
 *
 * <p>Scored in pressure (relative) and, where the model's line is not flat (|d ln P/dT| above 0.01 1/K: not at its
 * pressure maximum near 205 K), as the equivalent temperature {@code -ln(P_model/P)/(d ln P/dT)} against the 2 K target;
 * compositions in {@code ln x}. Measured 2026-09-25 (9019 J/mol): Davis MAD 1.64 % (max 5.61 % at 97.5 K, where the
 * line is methane's vapour pressure), 0.32 K equivalent (max 0.85 K); Souza 2.50 % (max 4.60 %), 0.44 K (max 2.03 K next to
 * the maximum); Davis y MAD 21 % (max 105 % at its lowest, 0.12 %), x 11.9 % (max 19.9 %). Bounds with a margin.</p>
 */
class G5F3ThreePhaseLineTest {
    static final double TARGET = 2.0;
    static final double DAVIS_PRESSURE_MAD = 2.0, DAVIS_PRESSURE_MAX = 6.5;
    static final double SOUZA_PRESSURE_MAD = 3.0, SOUZA_PRESSURE_MAX = 5.5;
    static final double EQUIVALENT_MAX = 2.5;
    static final double Y_MAD = 25, Y_MAX = 115, X_MAD = 14, X_MAX = 23;

    record Line(List<Double> pressure, List<Double> equivalent) {}

    static Line score(String name, double[][] line) {
        var ws = G5Support.SERVICE.newWorkspace();
        List<Double> pressure = new ArrayList<>(), equivalent = new ArrayList<>();
        System.out.println("dataset | T K | P data MPa | P model MPa | dP/P % | d ln P/dT 1/K | T-equivalent K | x_CO2 | y_CO2");
        for (double[] point : line) {
            double[] three = G5Support.threePhase(G5Support.SERVICE, point[0], 1, ws);
            assertNotNull(three, name + " at " + point[0] + " K: no three-phase point");
            double[] next = G5Support.threePhase(G5Support.SERVICE, point[0] + 0.05, 1, ws);
            assertNotNull(next);
            double slope = (Math.log(next[0]) - Math.log(three[0])) / 0.05;
            double d = 100 * (three[0] / point[1] - 1);
            double te = Math.abs(slope) > 0.01 ? -Math.log(three[0] / point[1]) / slope : Double.NaN;
            pressure.add(d);
            if (Double.isFinite(te)) equivalent.add(te);
            System.out.printf(Locale.ROOT, "%s | %.2f | %.4f | %.4f | %+.2f | %.4f | %+.3f | %.5f | %.6f%n", name, point[0], point[1] / 1e6, three[0] / 1e6, d, slope,
                    te, three[1], three[2]);
        }
        return new Line(pressure, equivalent);
    }

    static double mad(List<Double> v) { return v.stream().mapToDouble(Math::abs).average().orElse(Double.NaN); }
    static double max(List<Double> v) { return v.stream().mapToDouble(Math::abs).max().orElse(Double.NaN); }

    @Test
    void theThreePhaseLineAgainstDavisAndSouza() {
        var davis = score("Davis 1962", G5Support.davisLine());
        var souza = score("Souza 2020", G5Support.souzaLine());
        assertEquals(31, davis.pressure().size());
        assertEquals(7, souza.pressure().size());
        System.out.printf(Locale.ROOT, "G5F3 Davis: |dP/P| MAD %.2f %%, max %.2f %%; T-equivalent MAD %.3f K (%d points), max %.3f K (target %.1f K)%n",
                mad(davis.pressure()), max(davis.pressure()), mad(davis.equivalent()), davis.equivalent().size(), max(davis.equivalent()), TARGET);
        System.out.printf(Locale.ROOT, "G5F3 Souza: |dP/P| MAD %.2f %%, max %.2f %%; T-equivalent MAD %.3f K (%d points), max %.3f K (target %.1f K)%n",
                mad(souza.pressure()), max(souza.pressure()), mad(souza.equivalent()), souza.equivalent().size(), max(souza.equivalent()), TARGET);
        assertTrue(mad(davis.pressure()) <= DAVIS_PRESSURE_MAD && max(davis.pressure()) <= DAVIS_PRESSURE_MAX, "Davis pressure");
        assertTrue(mad(souza.pressure()) <= SOUZA_PRESSURE_MAD && max(souza.pressure()) <= SOUZA_PRESSURE_MAX, "Souza pressure");
        assertTrue(mad(davis.equivalent()) <= TARGET && mad(souza.equivalent()) <= TARGET, "the 2 K target in the equivalent temperature");
        assertTrue(max(davis.equivalent()) <= EQUIVALENT_MAX && max(souza.equivalent()) <= EQUIVALENT_MAX, "equivalent temperature per point");
    }

    @Test
    void theVapourAndLiquidCompositionsOnTheLineAgainstDavis() {
        var ws = G5Support.SERVICE.newWorkspace();
        for (boolean vapour : new boolean[] {true, false}) {
            List<Double> dev = new ArrayList<>();
            for (double[] r : G5Support.davisCompositions(vapour)) {
                double[] three = G5Support.threePhase(G5Support.SERVICE, r[0], 1, ws);
                assertNotNull(three);
                double model = vapour ? three[2] : three[1];
                double d = 100 * Math.log(model / r[1]);
                dev.add(d);
                System.out.printf(Locale.ROOT, "Davis %s | %.2f K | data %.5f | model %.5f | %+.1f %% in ln%n", vapour ? "Table 3 y" : "Table 4 x", r[0], r[1], model, d);
            }
            System.out.printf(Locale.ROOT, "G5F3 Davis %s on the line: %d points, |ln ratio| MAD %.1f %%, max %.1f %%%n", vapour ? "vapour" : "liquid", dev.size(),
                    mad(dev), max(dev));
            assertEquals(vapour ? 8 : 11, dev.size());
            if (vapour) assertTrue(mad(dev) <= Y_MAD && max(dev) <= Y_MAX, "vapour compositions");
            else assertTrue(mad(dev) <= X_MAD && max(dev) <= X_MAX, "liquid compositions");
        }
    }
}
