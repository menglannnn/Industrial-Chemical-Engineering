package com.wormzjl.createcheme.science.thermo.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * G5 family 5 (P5, batch 2026-09-24-coolprop-low-temperature), report only, no bound on the deviations: Gao et al. 2012
 * (CO2 in liquid CH4, Table 2, whose values are Shen 2012's binary at nearly the same temperatures, no pressure printed:
 * the liquid at Davis's locus pressure; and Table 4 with 10 to 70 % N2 in the solvent, 83 to 123 K, partly below the
 * pilot's 90 K CO2 floor), Sampson et al. 2023 (52 to 500 ppm CO2 in compressed liquid CH4 at 7 to 10 MPa, temperature
 * uncertainty 1.7 to 2.4 K, k = 1; Maltby et al. 2025 report every model at 3 % of T or worse there) and Kurata and Im 1971
 * Table VI (the three-phase x and y of Davis 1962, interpolated). Printed; the only checks are the counts and that every
 * point is answered (above the 10 MPa envelope or below CO2's 90 K spine a point is extrapolated, counted apart: owner
 * rule of 2026-10-16, out of domain before).
 *
 * <p>Measured 2026-09-25 (9019 J/mol): Gao binary 2.58 K MAD (bias -2.58 K, as Shen), 10 % N2 0.77 K, 30 % 2.80 K,
 * 50 % 4.13 K (one point below 90 K), 70 % 6.13 K (bias +6.1 K: the N2-rich liquids freeze warm on this family);
 * Sampson's three points inside 10 MPa -4.4 to -6.4 K (three above the ceiling).</p>
 */
class G5F5ReportOnlyLiquidusTest {
    @Test
    void gaoSampsonAndKurataImAreReported() {
        var gao = G5Support.gao();
        var sampson = G5Support.sampson();
        assertEquals(9 + 32, gao.size());
        assertEquals(6, sampson.size());
        var points = new java.util.ArrayList<>(gao);
        points.addAll(sampson);
        var datasets = G5Support.summary("G5F5 report only", G5Support.score(G5Support.SERVICE, points, true), Double.NaN);
        for (var s : datasets.values()) assertTrue(s.holds.isEmpty() && s.failures.isEmpty(), s.holds + " " + s.failures);
        assertEquals(3, datasets.get("Sampson 2023").extrapolated.size(), "three of Sampson's points lie above 10 MPa");
        // Kurata-Im Table VI: the three-phase x and y at their temperature.
        var ws = G5Support.SERVICE.newWorkspace();
        System.out.println("Kurata-Im Table VI | T K | P data MPa | P model MPa | x data | x model | y data | y model");
        for (String[] c : G4F1CarbonDioxideFrostPointHoldoutTest.rows("kurata-im-1971-jced-16-295-table6.tsv")) {
            double t = G5Support.number(c[1]);
            double[] three = G5Support.threePhase(G5Support.SERVICE, t, 1, ws);
            assertNotNull(three, t + " K");
            System.out.printf(Locale.ROOT, "Kurata-Im | %.2f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f%n", t, G5Support.number(c[3]), three[0] / 1e6,
                    G5Support.number(c[4]), three[1], G5Support.number(c[6]), three[2]);
        }
    }
}
