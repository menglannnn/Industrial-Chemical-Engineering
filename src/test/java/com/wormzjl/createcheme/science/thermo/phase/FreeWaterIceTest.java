package com.wormzjl.createcheme.science.thermo.phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult.Classification;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult.Status;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * WP4 of batch 2026-09-24-coolprop-low-temperature, gates (a) and (b) (RANGE_EXTRAPOLATION_PLAN.md sections 1 and 6):
 * free water below its melting line is ice Ih, decided by water's own rule.
 *
 * <p>Physics. Water freezes below {@code T_m = 273.16 K - 7.42e-8 K/Pa (P - 611.657 Pa)} (Clapeyron with the ice
 * record's volume and fusion enthalpy): 273.1526 K at 1 bar, 272.418 K at 10 MPa. Over ice the steam keeps IAPWS
 * R14-08's sublimation pressure: 259.87 Pa at 263.15 K (a supercooled liquid would keep 287 Pa) and 5.6e-12 Pa at
 * 111.6 K. Nitrogen at 1 bar holding 1170 Pa of water (50 % humidity at 20 °C) cooled to -10 °C keeps 260 Pa: 0.91 % of
 * its moles, 78 % of its water, leave as ice. Water in LNG at 111.6 K is all ice. Across the freeze a PH or UV
 * specification lands on the melting line with the ice fraction its balance gives.</p>
 */
class FreeWaterIceTest {
    private static final String ICE = "createcheme:crystal_ice_ih";

    private static FluidTpEquilibrium service() {
        return PhaseTestSupport.networkService(PhaseTestSupport.networkFreeWater(FluidTestSupport.networkModel()));
    }

    private static PhaseAmounts icePhase(EquilibriumResult result) {
        PhaseAmounts ice = null;
        for (PhaseAmounts phase : result.phases()) {
            if (phase.kind() == PhaseKind.SOLID) {
                assertEquals(ICE, phase.crystal(), "the solid is the free water's ice");
                ice = phase;
            }
        }
        return ice;
    }

    /** Gate (a): humid nitrogen at 1 bar cooled to -10 °C keeps the ice's sublimation pressure; the rest of its water is ice. */
    @Test
    void humidNitrogenBelowItsFrostPointKeepsTheSublimationPressure() {
        var service = service();
        var contract = service.contract();
        int water = contract.waterIndex();
        double p = 1.0e5;
        double t = 263.15;
        double[] feed = new double[contract.components().size()];
        feed[water] = 1170.0 / p;
        feed[contract.index("Nitrogen")] = 1.0 - feed[water];
        var result = service.tp(EquilibriumRequest.tp(t, p, contract.components(), feed, PhaseCompetition.FLUID_ONLY));
        assertEquals(Status.CONVERGED, result.status(), result.toString());
        assertEquals(Classification.FREE_WATER, result.classification(), result.toString());
        var split = result.freeWater();
        assertEquals(0.0, split.liquidWater(), "below the melting line the condensed water is ice");
        var ice = icePhase(result);
        assertNotNull(ice, result.toString());
        assertEquals(split.ice(), ice.amount(water), 0.0);
        // R14-08 at 263.15 K: 259.874 Pa, scaled by 1 + 7.3e-5 to meet the liquid on the melting line at 1 bar.
        assertEquals(259.89, split.waterPartialPressure(), 0.02, "the steam over ice");
        assertTrue(split.waterPartialPressure() < 0.95 * service.freeWater().saturationPressure(t), "below the supercooled liquid's 287 Pa");
        double share = split.ice() / (feed[water] + feed[contract.index("Nitrogen")]);
        assertEquals(0.00912, share, 0.00005, "0.91 % of the moles leave as ice");
        assertEquals(0.780, split.ice() / feed[water], 0.003, "78 % of the water");
        assertTrue(result.conservationDefect(feed) < 1e-13);
        System.out.printf(Locale.ROOT, "WP4 gate (a) humid N2 at 263.15 K, 1 bar: p_w %.3f Pa (supercooled liquid %.3f Pa), ice %.5f of the moles, %.4f of the water%n",
                split.waterPartialPressure(), service.freeWater().saturationPressure(t), share, split.ice() / feed[water]);
    }

    /** Gate (a): water in LNG (methane with 5 % nitrogen) at 111.6 K and 1 atm is all ice; its steam is at 5.6e-12 Pa. */
    @Test
    void waterInLngIsAllIce() {
        var service = service();
        var contract = service.contract();
        int water = contract.waterIndex();
        double p = 101325.0;
        double t = 111.6;
        double[] feed = new double[contract.components().size()];
        feed[contract.index("Methane")] = 0.95;
        feed[contract.index("Nitrogen")] = 0.05;
        feed[water] = 0.1;
        var result = service.tp(EquilibriumRequest.tp(t, p, contract.components(), feed, PhaseCompetition.FLUID_ONLY));
        assertEquals(Status.CONVERGED, result.status(), result.toString());
        assertEquals(Classification.FREE_WATER, result.classification(), result.toString());
        var split = result.freeWater();
        assertEquals(0.0, split.liquidWater());
        assertNotNull(icePhase(result), result.toString());
        assertTrue(split.ice() > feed[water] * (1.0 - 1.0e-12), "all of the water is ice: " + split.ice());
        var ice = service.freeWater().ice();
        assertEquals(5.63e-12, ice.fugacity(t, p), 0.02e-12, "R14-08's sublimation pressure at 111.6 K");
        // The ice's enthalpy (R10-06 through the record): water from 20 °C to ice at 111.6 K gives up 1.5 + 6.0 + 4.4 kJ/mol.
        double[] liquid = new double[2];
        service.freeWater().liquid(293.15, p, liquid);
        double given = liquid[1] - ice.properties(t, p).enthalpy();
        assertEquals(11.94e3, given, 0.1e3, "liquid at 20 °C to ice at 111.6 K");
        System.out.printf(Locale.ROOT, "WP4 gate (a) water in LNG at 111.6 K: ice %.12f of the water, f_ice %.4e Pa, %s; water from 20 °C to ice gives up %.1f J/mol%n",
                split.ice() / feed[water], ice.fugacity(t, p), result.classification(), given);
    }

    /**
     * Gate (b): pure water across its freeze, PH at 1 bar and 10 MPa and UV at 1 bar, with 40 % of it ice by the balance:
     * the answer lies on the melting line (273.1526 K and 272.418 K) with that ice fraction.
     */
    @Test
    void phAndUvAcrossTheFreezeLandOnTheMeltingLine() {
        var service = service();
        var contract = service.contract();
        var workspace = service.newWorkspace();
        List<String> species = contract.components();
        int water = contract.waterIndex();
        var model = service.freeWater();
        var ice = model.ice();
        double[] pure = new double[species.size()];
        pure[water] = 1.0;
        double beta = 0.4;
        assertEquals(273.1526, ice.meltingTemperature(1.0e5), 2e-4, "Clapeyron at 1 bar");
        assertEquals(272.418, ice.meltingTemperature(1.0e7), 2e-3, "Clapeyron at 10 MPa");
        for (double p : new double[] {1.0e5, 1.0e7}) {
            double melting = ice.meltingTemperature(p);
            double[] liquid = new double[2];
            model.liquid(melting, p, liquid);
            var solid = ice.properties(melting, p);
            double h = (1.0 - beta) * liquid[1] + beta * solid.enthalpy();
            var ph = service.ph(EquilibriumRequest.ph(p, h, species, pure, PhaseCompetition.FLUID_ONLY), workspace);
            assertFreezing(ph, melting, p, beta, pure, water, "PH at " + p + " Pa");
            assertEquals(h, ph.enthalpy(), 1e-9 * Math.abs(h));
            if (p == 1.0e5) {
                double v = (1.0 - beta) * liquid[0] + beta * solid.volume();
                var uv = service.uv(EquilibriumRequest.uv(h - p * v, v, species, pure, PhaseCompetition.FLUID_ONLY), workspace);
                assertFreezing(uv, melting, p, beta, pure, water, "UV at " + p + " Pa");
                System.out.printf(Locale.ROOT, "WP4 gate (b) UV at 1 bar: %.6f K, %.3f Pa, ice %.8f (%d TP calls)%n",
                        uv.phases().get(0).temperature(), uv.phases().get(0).pressure(), uv.freeWater().ice(), uv.diagnostics().outerIterations());
            }
            System.out.printf(Locale.ROOT, "WP4 gate (b) PH at %.0f Pa: %.6f K (T_m %.6f K), ice %.8f (%d TP calls)%n", p,
                    ph.phases().get(0).temperature(), melting, ph.freeWater().ice(), ph.diagnostics().outerIterations());
        }
    }

    /**
     * Gate (b): vacuum freezing. Liquid water at 25 °C let down to 100 Pa, below its triple point's 611.657 Pa, at its
     * enthalpy: it boils until it is cold, then freezes, and stands on the sublimation line where the ice's vapour pressure
     * is 100 Pa (252.9 K); the balance gives the steam {@code (h - h_ice) / (h_steam - h_ice)}, about 17 %, the rest ice.
     */
    @Test
    void liquidWaterLetDownBelowItsTriplePointFreezes() {
        var service = service();
        var contract = service.contract();
        List<String> species = contract.components();
        int water = contract.waterIndex();
        var model = service.freeWater();
        var ice = model.ice();
        double[] pure = new double[species.size()];
        pure[water] = 1.0;
        double p = 100.0;
        double[] liquid = new double[2];
        model.liquid(298.15, 101325.0, liquid);
        var ph = service.ph(EquilibriumRequest.ph(p, liquid[1], species, pure, PhaseCompetition.FLUID_ONLY), service.newWorkspace());
        assertEquals(Status.CONVERGED, ph.status(), ph.toString());
        assertEquals(Classification.FREE_WATER, ph.classification(), ph.toString());
        double t = ph.phases().get(0).temperature();
        assertEquals(p, ice.fugacity(t, p), 1e-6 * p, "on the sublimation line");
        assertEquals(252.9, t, 0.1, "where the ice's vapour pressure is 100 Pa");
        var split = ph.freeWater();
        assertEquals(0.0, split.liquidWater(), "no liquid is left below the triple point");
        double steam = (liquid[1] - ice.properties(t, p).enthalpy()) / (model.vapor().enthalpy(t) - ice.properties(t, p).enthalpy());
        assertEquals(steam, split.waterVapor(), 1e-6, "the steam from the balance");
        assertEquals(1.0 - steam, split.ice(), 1e-6, "the rest is ice");
        assertEquals(liquid[1], ph.enthalpy(), 1e-9 * Math.abs(liquid[1]));
        assertTrue(ph.conservationDefect(pure) < 1e-13);
        System.out.printf(Locale.ROOT, "WP4 gate (b) liquid water at 25 °C let down to 100 Pa: %.4f K, steam %.5f, ice %.5f (%d TP calls)%n",
                t, split.waterVapor(), split.ice(), ph.diagnostics().outerIterations());
    }

    private static void assertFreezing(EquilibriumResult result, double melting, double p, double beta, double[] overall, int water, String at) {
        assertEquals(Status.CONVERGED, result.status(), at + ": " + result);
        assertEquals(Classification.FREE_WATER, result.classification(), at);
        assertTrue(result.diagnostics().coexistenceFromBalance(), at);
        assertEquals(melting, result.phases().get(0).temperature(), 1e-6, at + " on the melting line");
        assertEquals(p, result.phases().get(0).pressure(), 1e-3 * p, at);
        assertEquals(beta * overall[water], result.freeWater().ice(), 1e-6, at + " ice fraction from the balance");
        assertEquals((1.0 - beta) * overall[water], result.freeWater().liquidWater(), 1e-6, at);
        assertNotNull(icePhase(result), at);
        assertTrue(result.conservationDefect(overall) < 1e-13, at);
    }
}
