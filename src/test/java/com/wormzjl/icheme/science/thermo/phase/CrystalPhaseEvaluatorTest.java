package com.wormzjl.icheme.science.thermo.phase;

import static com.wormzjl.icheme.science.thermo.phase.SolidCarbonDioxideReferences.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.icheme.science.material.CrystalReference;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog;
import com.wormzjl.icheme.science.thermo.PengRobinsonKernel;
import com.wormzjl.icheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.icheme.science.thermo.reference.HelmholtzFluid;
import org.junit.jupiter.api.Test;

/**
 * The crystal evaluator (P4 stage 1, batch 2026-09-24-coolprop-low-temperature): the CO2-I crystal anchored to the pilot
 * family's liquid (the P2 contract: g_s = mu_L and h_s = h_L - dH_fus at the triple point, ln phi against the spine),
 * and the port and the anchoring validated independently of any reference-state convention: anchored to the Span-Wagner
 * oracle's liquid with Jaeger and Span's 8875 J/mol, the crystal recovers the paper's own g0 and g1 and reproduces its
 * Figures 9 (sublimation) and 11 (melting) against the Span-Wagner correlations.
 */
class CrystalPhaseEvaluatorTest {
    static final String PILOT = "icheme:pilot_cryogenic";
    static final String CRYSTAL = "icheme:crystal_carbon_dioxide_i";
    static final String CRYSTAL_PATH = "data/icheme/materials/crystals/carbon_dioxide_i.json";

    /** The CO2-I record with Jaeger and Span's own fusion enthalpy, the value they fitted with the Span-Wagner fluid. */
    static CrystalReference jaegerSpanRecord() {
        return PilotCryogenicTestCatalog.parseWith(CRYSTAL_PATH, o -> o.getAsJsonObject("anchor").addProperty("fusion_enthalpy_j_per_mol", 8875))
                .crystals().get(CRYSTAL);
    }

    @Test
    void anchorsToThePilotLiquidAndFillsAConsistentSolidState() {
        MaterialCatalog catalog = MaterialCatalog.bundled();
        var fluid = CubicPhaseEvaluator.forPackage(catalog, PILOT);
        var solid = CrystalPhaseEvaluator.forPackage(catalog, PILOT, CRYSTAL);
        assertEquals("crystal_jaeger_span_2012_triple_point_anchored_v1", solid.family());
        assertEquals(CRYSTAL, solid.crystal());
        assertEquals("CarbonDioxide", solid.component());
        var anchor = solid.anchor(fluid);
        int co2 = fluid.components().indexOf("CarbonDioxide");
        double[] pure = new double[fluid.componentCount()];
        pure[co2] = 1;
        PhaseState liquid = fluid.evaluate(TRIPLE_TEMPERATURE, TRIPLE_PRESSURE, pure, Root.LIQUID, fluid.newWorkspace(), fluid.newState());
        assertEquals(PhaseKind.LIQUID, liquid.kind());
        assertEquals(liquid.chemicalPotential(co2), anchor.chemicalPotential(), 0.0);
        assertEquals(liquid.molarEnthalpy(), anchor.liquidEnthalpy(), 0.0);
        double fusion = solid.record().anchor().fusionEnthalpy();

        // At the anchor: g_s = mu_L, h_s = h_L - dH_fus, and the state's chemical potential is g_s.
        PhaseState at = solid.evaluate(TRIPLE_TEMPERATURE, TRIPLE_PRESSURE, anchor, new PhaseState(1));
        assertEquals(PhaseKind.SOLID, at.kind());
        assertEquals(CRYSTAL, at.crystal());
        assertNull(at.root());
        assertEquals(anchor.chemicalPotential(), at.molarGibbsEnergy(), 1e-9);
        assertEquals(anchor.chemicalPotential(), at.chemicalPotential(0), 1e-9);
        assertEquals(anchor.liquidEnthalpy() - fusion, at.molarEnthalpy(), 1e-9);
        assertEquals((anchor.liquidEnthalpy() - anchor.chemicalPotential() - fusion) / TRIPLE_TEMPERATURE, at.molarEntropy(), 1e-12);
        System.out.printf("CO2-I on %s: mu_L %.6f J/mol, h_L %.6f J/mol, dH_fus %.0f J/mol, g0 %.9f, g1 %.9f%n", PILOT, anchor.chemicalPotential(),
                anchor.liquidEnthalpy(), fusion, solid.record().integrationConstants(anchor.chemicalPotential(), anchor.liquidEnthalpy())[0],
                solid.record().integrationConstants(anchor.chemicalPotential(), anchor.liquidEnthalpy())[1]);

        // Away from the anchor: dg/dT = -s, dg/dP = v, g = h - T s, u = h - P v, ln phi against the spine's ideal gas.
        var spine = catalog.requireSpine(PILOT, "CarbonDioxide");
        for (double[] state : new double[][] {{95, 1e3}, {180, 1e5}, {216.592, 5e6}, {250, 1e8}, {299, 4.9e8}}) {
            double t = state[0], p = state[1];
            PhaseState s = solid.evaluate(t, p, anchor, new PhaseState(1)).copy();
            double dt = 1e-3, dp = Math.max(1e-4 * p, 10);
            double dgdt = (solid.chemicalPotential(t + dt, p, anchor) - solid.chemicalPotential(t - dt, p, anchor)) / (2 * dt);
            double dgdp = (solid.chemicalPotential(t, p + dp, anchor) - solid.chemicalPotential(t, p - dp, anchor)) / (2 * dp);
            assertEquals(-s.molarEntropy(), dgdt, 1e-6 * Math.abs(s.molarEntropy()), "dg/dT at " + t + " K");
            assertEquals(s.molarVolume(), dgdp, 1e-6 * s.molarVolume(), "dg/dP at " + t + " K, " + p + " Pa");
            assertEquals(s.molarEnthalpy() - t * s.molarEntropy(), s.molarGibbsEnergy(), 1e-9 * Math.abs(s.molarGibbsEnergy()));
            assertEquals(s.molarEnthalpy() - p * s.molarVolume(), s.molarInternalEnergy(), 1e-12 * Math.abs(s.molarEnthalpy()));
            double gIdeal = spine.enthalpy(t) - t * spine.entropy(t, p);
            assertEquals((s.molarGibbsEnergy() - gIdeal) / (PengRobinsonKernel.GAS_CONSTANT * t), s.logFugacityCoefficient(0), 1e-12);
            assertEquals(spine.enthalpy(t), s.idealGasEnthalpy(), 0.0);
            assertEquals(s.molarGibbsEnergy(), s.chemicalPotential(0), 1e-9 * Math.abs(s.molarGibbsEnergy()));
            var properties = solid.properties(t, p, anchor);
            assertEquals(solid.record().heatCapacity(t, p), properties.heatCapacity(), 1e-12 * properties.heatCapacity());
        }
    }

    @Test
    void refusesForeignAnchorsAndContinuesPastItsRange() {
        MaterialCatalog catalog = MaterialCatalog.bundled();
        var fluid = CubicPhaseEvaluator.forPackage(catalog, PILOT);
        var solid = CrystalPhaseEvaluator.forPackage(catalog, PILOT, CRYSTAL);
        var anchor = solid.anchor(fluid);
        var other = new SolidPhaseEvaluator.TriplePointAnchor("Methane", TRIPLE_TEMPERATURE, TRIPLE_PRESSURE, anchor.chemicalPotential(), anchor.liquidEnthalpy());
        assertThrows(IllegalArgumentException.class, () -> solid.evaluate(180, 1e5, other, new PhaseState(1)));
        var elsewhere = new SolidPhaseEvaluator.TriplePointAnchor("CarbonDioxide", 216.0, TRIPLE_PRESSURE, anchor.chemicalPotential(), anchor.liquidEnthalpy());
        var moved = assertThrows(IllegalArgumentException.class, () -> solid.evaluate(180, 1e5, elsewhere, new PhaseState(1)));
        assertTrue(moved.getMessage().contains("not at the record's triple point"), moved.getMessage());
        // Past the crystal's range (80 to 300 K, to 500 MPa) and the spine's (CO2 from 90 K) both continue (owner rule of
        // 2026-10-16); only a state that is not positive is refused.
        for (double[] state : new double[][] {{79.9, 1e5}, {301, 1e5}, {200, 5.1e8}, {85, 1e-3}}) {
            var continued = solid.evaluate(state[0], state[1], anchor, new PhaseState(1));
            assertTrue(Double.isFinite(continued.chemicalPotential(0)), "continued at " + state[0] + " K, " + state[1] + " Pa");
        }
        assertThrows(IllegalArgumentException.class, () -> solid.evaluate(200, 0, anchor, new PhaseState(1)));
        assertThrows(IllegalArgumentException.class, () -> solid.evaluate(0, 1e5, anchor, new PhaseState(1)));
        assertThrows(IllegalArgumentException.class, () -> solid.evaluate(180, 1e5, anchor, new PhaseState(2)));
        assertThrows(IllegalArgumentException.class, () -> CrystalPhaseEvaluator.forPackage(catalog, "icheme:tjl20_methane_nitrogen", CRYSTAL));
        // An unusable record is refused at construction.
        var incomplete = PilotCryogenicTestCatalog.parseWith(CRYSTAL_PATH, o -> {
            o.getAsJsonObject("anchor").addProperty("temperature_kelvin", 310);
            o.getAsJsonObject("coverage").addProperty("grade", "research_only");
        });
        assertThrows(IllegalStateException.class, () -> CrystalPhaseEvaluator.forPackage(incomplete, PILOT, CRYSTAL));
    }

    /**
     * Anchored to the oracle's Span-Wagner liquid at (216.592 K, 517 950 Pa) with 8875 J/mol, the solved constants are
     * the paper's to 3.6e-5 (g0) and 9.7e-7 (g1): the g0 difference, 0.045 J/mol, is the oracle's own vapour-liquid
     * chemical-potential mismatch at that pressure (the Span-Wagner triple pressure is 517 964 Pa on the equation).
     */
    @Test
    void oracleAnchoringRecoversThePublishedIntegrationConstants() {
        var record = jaegerSpanRecord();
        var co2 = HelmholtzFluid.load("CarbonDioxide");
        double[] constants = oracleConstants(record, co2);
        var js = record.gibbsFunction();
        var liquid = co2.stateAtPressure(TRIPLE_TEMPERATURE, TRIPLE_PRESSURE, HelmholtzFluid.Root.LIQUID);
        var vapour = co2.stateAtPressure(TRIPLE_TEMPERATURE, TRIPLE_PRESSURE, HelmholtzFluid.Root.VAPOUR);
        System.out.printf("Oracle anchoring (8875 J/mol): g0 %.9f vs %.7f (%.2e), g1 %.9f vs %.7f (%.2e); oracle mu_V - mu_L at (T_t, p_t) %.4f J/mol%n",
                constants[0], js.publishedG0(), constants[0] - js.publishedG0(), constants[1], js.publishedG1(), constants[1] - js.publishedG1(),
                vapour.gibbsEnergy() - liquid.gibbsEnergy());
        assertEquals(js.publishedG0(), constants[0], 5e-5);
        assertEquals(js.publishedG1(), constants[1], 2e-6);
        assertEquals(Math.abs(vapour.gibbsEnergy() - liquid.gibbsEnergy()), Math.abs(constants[0] - js.publishedG0()) * js.gasConstant() * js.referenceTemperature(), 2e-3);
        // The anchored solid's triple-point entropy is the paper's Table 5 value (Span-Wagner reference state).
        assertEquals(-1.803247012e1, js.evaluate(TRIPLE_TEMPERATURE, TRIPLE_PRESSURE, constants[0], constants[1]).entropy(), 2e-5);
    }

    /**
     * Figure 9 of the paper: dp = p_SW(3.12) - p_calc. The calculated line stays within the Span-Wagner uncertainty
     * (50 Pa below 170 K, 250 Pa from 185 K to T_t) except from 170 to 185 K, where it reaches 1.5 times the 100 Pa band,
     * as the paper's own line does; its extremes, about -227 Pa near 194 K and +237 Pa near 212 K, are the figure's.
     * Trusler's equation 48 (his solid with the same fluid) agrees within 0.85 % from 150 K to T_t.
     */
    @Test
    void oracleSublimationFollowsTheSpanWagnerCorrelationAsInFigureNine() {
        var record = jaegerSpanRecord();
        var co2 = HelmholtzFluid.load("CarbonDioxide");
        double[] constants = oracleConstants(record, co2);
        var js = record.gibbsFunction();
        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY, tMinimum = 0, tMaximum = 0, worstBand = 0, worstTrusler = 0;
        double[] temperatures = new double[70];
        for (int k = 0; k < 69; k++) temperatures[k] = 80 + 2 * k;
        temperatures[69] = TRIPLE_TEMPERATURE;
        for (double t : temperatures) {
            double p = oracleCoexistence(js, constants, co2, t, HelmholtzFluid.Root.VAPOUR, spanWagnerSublimationPressure(t));
            double dp = spanWagnerSublimationPressure(t) - p;
            double band = spanWagnerSublimationUncertainty(t);
            double allowed = t >= 170 && t < 185 ? 1.55 * band : band;
            assertTrue(Math.abs(dp) <= allowed, "dp " + dp + " Pa at " + t + " K beyond " + allowed + " Pa");
            worstBand = Math.max(worstBand, Math.abs(dp) / band);
            if (dp < minimum) { minimum = dp; tMinimum = t; }
            if (dp > maximum) { maximum = dp; tMaximum = t; }
            if (t >= 150) worstTrusler = Math.max(worstTrusler, Math.abs(p / truslerSublimationPressure(t) - 1));
        }
        System.out.printf("Oracle sublimation 80..216.592 K: dp min %.1f Pa at %.0f K, max %.1f Pa at %.0f K, worst |dp|/uncertainty %.3f, worst vs Trusler eq 48 %.3f %%%n",
                minimum, tMinimum, maximum, tMaximum, worstBand, 100 * worstTrusler);
        assertTrue(minimum > -240 && minimum < -215 && tMinimum >= 192 && tMinimum <= 196, "Figure 9 minimum");
        assertTrue(maximum > 225 && maximum < 250 && tMaximum >= 210 && tMaximum <= 214, "Figure 9 maximum");
        assertTrue(worstTrusler < 0.0085);
        // Sublimation enthalpy at the normal sublimation point on the reference equations: within 0.1 % of 25.2 kJ/mol.
        double t = 194.67;
        double p = oracleCoexistence(js, constants, co2, t, HelmholtzFluid.Root.VAPOUR, spanWagnerSublimationPressure(t));
        double dh = co2.stateAtPressure(t, p, HelmholtzFluid.Root.VAPOUR).enthalpy() - js.evaluate(t, p, constants[0], constants[1]).enthalpy();
        System.out.printf("Oracle sublimation enthalpy at %.2f K: %.1f J/mol (%.3f %% from %.0f)%n", t, dh, 100 * (dh / GIAUQUE_EGAN_SUBLIMATION_ENTHALPY - 1),
                GIAUQUE_EGAN_SUBLIMATION_ENTHALPY);
        assertEquals(GIAUQUE_EGAN_SUBLIMATION_ENTHALPY, dh, 0.001 * GIAUQUE_EGAN_SUBLIMATION_ENTHALPY);
    }

    /**
     * Figure 11 of the paper: (p_SW(3.10) - p_calc)/p_SW within the Span-Wagner uncertainty (1.5 % to 225 K, 0.5 % from
     * 225 to 270 K), measured worst 0.93 of the band: +0.85 % near 217.5 K, -0.47 % near 245 K, +0.10 % at 270 K.
     */
    @Test
    void oracleMeltingFollowsTheSpanWagnerCorrelationAsInFigureEleven() {
        var record = jaegerSpanRecord();
        var co2 = HelmholtzFluid.load("CarbonDioxide");
        double[] constants = oracleConstants(record, co2);
        var js = record.gibbsFunction();
        double worst = 0, minimum = 0, maximum = 0;
        for (double t : new double[] {216.6, 216.7, 217, 217.5, 218, 219, 220, 222, 225, 230, 235, 240, 245, 250, 255, 260, 265, 270}) {
            double p = oracleCoexistence(js, constants, co2, t, HelmholtzFluid.Root.LIQUID, spanWagnerMeltingPressure(t));
            double relative = (spanWagnerMeltingPressure(t) - p) / spanWagnerMeltingPressure(t);
            assertTrue(Math.abs(relative) <= spanWagnerMeltingUncertainty(t), "melting at " + t + " K: " + relative);
            worst = Math.max(worst, Math.abs(relative) / spanWagnerMeltingUncertainty(t));
            minimum = Math.min(minimum, relative);
            maximum = Math.max(maximum, relative);
        }
        System.out.printf("Oracle melting 216.6..270 K: dp/p from %.3f %% to %.3f %%, worst |dp/p|/uncertainty %.3f%n", 100 * minimum, 100 * maximum, worst);
        assertTrue(maximum > 0.0080 && maximum < 0.0090 && minimum < -0.0045 && minimum > -0.0050, "Figure 11 extremes");
    }
}
