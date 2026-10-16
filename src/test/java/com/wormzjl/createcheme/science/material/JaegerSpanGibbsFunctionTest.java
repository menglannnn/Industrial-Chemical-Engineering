package com.wormzjl.createcheme.science.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The Jaeger and Span 2012 solid CO2 Gibbs function (P4 stage 1, batch 2026-09-24-coolprop-low-temperature) as carried
 * by the bundled CO2-I crystal record: the paper's Table 5 check values with its published g0 and g1, the analytic
 * derivatives of Tables 3 and 4 against central differences, the Table 2 relations, the integration constants and the
 * refusal outside 80 to 300 K and 0 to 500 MPa.
 */
class JaegerSpanGibbsFunctionTest {
    private static final String CRYSTAL = "createcheme:crystal_carbon_dioxide_i";

    static JaegerSpanGibbsFunction function() {
        return MaterialCatalog.bundled().crystals().get(CRYSTAL).gibbsFunction();
    }

    /**
     * Table 5 (page 593): g, v, s, c_p, alpha, kappa at (T_tr, p_tr) and (100 K, 100 MPa), printed to ten significant
     * digits; each computed value must round to the printed one (within half a unit of its last digit).
     */
    @Test
    void reproducesTableFiveCheckValues() {
        var js = function();
        double[][] rows = {
            {216.592, 517950, -1.447007522e3, 2.848595255e-5, -1.803247012e1, 5.913420271e1, 8.127788321e-4, 2.813585169e-10},
            {100, 1e8, -2.961795962e3, 2.614596591e-5, -5.623154438e1, 3.911045710e1, 3.843376525e-4, 1.149061787e-10},
        };
        String[] names = {"g", "v", "s", "cp", "alpha", "kappa"};
        for (double[] row : rows) {
            var s = js.evaluate(row[0], row[1], js.publishedG0(), js.publishedG1());
            double[] computed = {s.gibbs(), s.volume(), s.entropy(), s.heatCapacity(), s.expansion(), s.compressibility()};
            for (int i = 0; i < names.length; i++) {
                double printed = row[2 + i];
                double halfUnit = 0.5 * Math.pow(10, Math.floor(Math.log10(Math.abs(printed))) - 9);
                System.out.printf("Table 5 %7.3f K %9.0f Pa %-5s printed %.9e computed %.12e (%.2e of the last digit)%n", row[0], row[1], names[i],
                        printed, computed[i], Math.abs(computed[i] - printed) / (2 * halfUnit));
                assertEquals(printed, computed[i], halfUnit, names[i] + " at " + row[0] + " K");
            }
        }
        // v0 of equation 10 is the volume at (T0, p0); the paper prints it to eight digits and it agrees to 2.1e-8.
        var reference = js.evaluate(150, 101325, js.publishedG0(), js.publishedG1());
        assertEquals(js.referenceVolume(), reference.volume(), 5e-8 * js.referenceVolume());
    }

    /** Tables 3 and 4 against central differences of g and its first derivatives, 1e-6 relative, over the range. */
    @Test
    void analyticDerivativesMatchCentralDifferences() {
        var js = function();
        double g0 = js.publishedG0(), g1 = js.publishedG1();
        double worst = 0;
        for (double t : new double[] {80.01, 90, 120, 150, 180, 216.592, 250, 280, 299.99}) {
            for (double p : new double[] {1e3, 1e5, 517950, 1e7, 1e8, 3e8, 4.99e8}) {
                var s = js.evaluate(t, p, g0, g1);
                double ht = 1e-3, hp = Math.max(1e-4 * p, 10);
                var tPlus = js.evaluate(t + ht, p, g0, g1);
                var tMinus = js.evaluate(t - ht, p, g0, g1);
                var pPlus = js.evaluate(t, p + hp, g0, g1);
                var pMinus = js.evaluate(t, p - hp, g0, g1);
                double[][] pairs = {
                    {s.dT(), (tPlus.gibbs() - tMinus.gibbs()) / (2 * ht)},
                    {s.dP(), (pPlus.gibbs() - pMinus.gibbs()) / (2 * hp)},
                    {s.dTT(), (tPlus.dT() - tMinus.dT()) / (2 * ht)},
                    {s.dTP(), (tPlus.dP() - tMinus.dP()) / (2 * ht)},
                    {s.dTP(), (pPlus.dT() - pMinus.dT()) / (2 * hp)},
                    {s.dPP(), (pPlus.dP() - pMinus.dP()) / (2 * hp)},
                };
                String[] names = {"dg/dT", "dg/dP", "d2g/dT2", "d2g/dTdP", "d2g/dPdT", "d2g/dP2"};
                for (int i = 0; i < pairs.length; i++) {
                    double relative = Math.abs(pairs[i][0] - pairs[i][1]) / Math.abs(pairs[i][0]);
                    worst = Math.max(worst, relative);
                    assertEquals(pairs[i][0], pairs[i][1], 1e-6 * Math.abs(pairs[i][0]), names[i] + " at " + t + " K, " + p + " Pa");
                }
                // Table 2: the properties are the stated combinations of g and its derivatives.
                assertEquals(s.gibbs() - t * s.dT(), s.enthalpy(), 1e-12 * Math.abs(s.enthalpy()) + 1e-9);
                assertEquals(s.enthalpy() - p * s.volume(), s.internalEnergy(), 1e-12 * Math.abs(s.enthalpy()) + 1e-9);
                assertEquals(s.gibbs() - p * s.volume(), s.helmholtzEnergy(), 1e-12 * Math.abs(s.gibbs()) + 1e-9);
                assertTrue(s.volume() > 0 && s.heatCapacity() > 0 && s.compressibility() > 0 && s.expansion() > 0, "physical at " + t + " K, " + p + " Pa");
            }
        }
        System.out.printf("Jaeger-Span analytic derivatives: worst relative difference to central differences %.2e%n", worst);
    }

    /** g0 and g1 from a Gibbs energy and an entropy at one state reproduce both, and the published pair round-trips. */
    @Test
    void integrationConstantsReproduceTheirConditions() {
        var js = function();
        var published = js.evaluate(216.592, 517950, js.publishedG0(), js.publishedG1());
        double[] constants = js.integrationConstants(216.592, 517950, published.gibbs(), published.entropy());
        assertEquals(js.publishedG0(), constants[0], 1e-12);
        assertEquals(js.publishedG1(), constants[1], 1e-12);
        // An arbitrary anchor on the spine's scale: the solved function takes the anchor's g and s exactly.
        double g = -437_397.0, s = 60.25;
        double[] anchored = js.integrationConstants(216.592, 517950, g, s);
        var state = js.evaluate(216.592, 517950, anchored[0], anchored[1]);
        assertEquals(g, state.gibbs(), 1e-9 * Math.abs(g));
        assertEquals(s, state.entropy(), 1e-11 * s);
        // g0 and g1 do not change the volume, heat capacity, expansion or compressibility.
        var other = js.evaluate(180, 2e7, anchored[0], anchored[1]);
        var base = js.evaluate(180, 2e7, 0, 0);
        assertEquals(base.volume(), other.volume(), 0.0);
        assertEquals(base.heatCapacity(), other.heatCapacity(), 0.0);
        assertEquals(base.expansion(), other.expansion(), 0.0);
        assertEquals(base.compressibility(), other.compressibility(), 0.0);
    }

    @Test
    void continuesPastItsRangeAndRefusesOnlyAStateThatIsNotPositive() {
        var js = function();
        assertEquals(80, js.minimumTemperature(), 0.0);
        assertEquals(300, js.maximumTemperature(), 0.0);
        assertEquals(0, js.minimumPressure(), 0.0);
        assertEquals(5e8, js.maximumPressure(), 0.0);
        // Past the declared range (a data range since the owner rule of 2026-10-16) the function continues.
        for (double[] state : new double[][] {{79.999, 1e5}, {300.001, 1e5}, {200, 5.00001e8}, {20, 1e5}, {350, 6e8}}) {
            assertFalse(js.covers(state[0], state[1]));
            var s = js.evaluate(state[0], state[1], 0, 0);
            assertTrue(Double.isFinite(s.gibbs()) && s.volume() > 0 && s.heatCapacity() > 0, java.util.Arrays.toString(state));
        }
        for (double[] state : new double[][] {{200, 0}, {200, -1}, {0, 1e5}, {Double.NaN, 1e5}, {200, Double.NaN}}) {
            var error = assertThrows(IllegalArgumentException.class, () -> js.evaluate(state[0], state[1], 0, 0));
            assertTrue(error.getMessage().contains("not a positive finite state"), error.getMessage());
        }
        assertTrue(js.covers(80, 1e-3));
        assertTrue(js.covers(300, 5e8));
        assertThrows(IllegalArgumentException.class, () -> js.evaluate(200, 1e5, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> js.integrationConstants(200, 1e5, Double.POSITIVE_INFINITY, 0));
    }
}
