package com.wormzjl.icheme.science.thermo.qualification;

import static com.wormzjl.icheme.science.thermo.qualification.G3Support.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.icheme.science.thermo.TangentPlaneStability;
import com.wormzjl.icheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.icheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.icheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.icheme.science.thermo.phase.PhaseKind;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * G3 fixture family F6 (plan {@code P3_PILOT_ENGINE_PLAN.md} section 8.2; P0 rows N16 and C2; D5): compact probes of the
 * current crude cuts on the bundled network package {@code icheme:tjl20_methane_nitrogen} (Tia Juana light assay),
 * <b>probes only, no qualification claim</b>; the values are recorded for the G3 report and the later process families
 * (VDU batch WP0, P7).
 *
 * <ul>
 *   <li>VDU: PR78 pure-cut saturation temperatures at 1, 5 and 13 kPa next to the D5 declared bias against
 *   Maxwell-Bonnell (P0 section 6.1, recorded, not recomputed), the normal boiling point reproduced; the heavy cut's
 *   K-values of a vacuum flash at Case A (655.15 K, 13.1 kPa) and at 673 K, 4 kPa through the network's flash.</li>
 *   <li>CDU: pure-cut saturation temperatures at 250 kPa (with the recorded PR - MB there) and the whole assay at the
 *   literature preset's 638.15 K and 250 kPa.</li>
 *   <li>Coking 739-783 K at 0.20-0.52 MPa: the assay through the network's flash; the engine's feed stability verdict
 *   agrees with the phase count.</li>
 *   <li>Hydrotreating 589-713 K at 6.1 and 9.1 MPa: carried by the network past the cuts' 2 MPa data on their
 *   continuations (a notice since the owner rule of 2026-10-16, a typed refusal before); the same states on a research contract of the engine (open domain): phase count,
 *   labels, verdict (H2 absent until P7).</li>
 *   <li>Cut and light-end liquid densities at 300 K from 0.1 to 10 MPa: the root is liquid-like at every pressure
 *   (the NIST 300 K isotherms of {@code LiquidCompressionQualificationTest} end at 1.1 MPa; no reference above).</li>
 * </ul>
 */
class G3F6CurrentCutProbeTest {
    static final String[] CUTS = {"crude_pc01", "crude_pc02", "crude_pc03", "crude_pc04", "crude_pc05", "crude_pc06", "crude_pc07",
            "crude_pc08", "crude_pc09", "crude_pc10", "crude_pc11", "crude_pc12"};
    /** P0 section 6.1 / {@code research/.../vdu-k-policy/psat-mb-check-output.txt}: PR - MB at 1, 5, 13 and 250 kPa, K (D5, recorded). */
    static final double[][] PR_MINUS_MB = {
            {-1.6, -0.8, -0.3, -0.06}, {-2.5, -1.5, -0.9, 0.47}, {-3.1, -2.1, -1.5, 1.04}, {-4.0, -2.9, -2.2, 1.71}, {-5.4, -4.1, -3.1, 2.57},
            {-6.9, -5.4, -4.2, 3.47}, {-8.4, -6.7, -5.3, 4.38}, {-10.1, -8.2, -6.5, 5.44}, {-11.9, -9.9, -7.9, 6.63}, {-12.8, -10.9, -8.9, 7.55},
            {-12.6, -11.1, -9.2, 7.94}, {-12.9, -11.7, -9.8, 8.56}};

    static double[] assay(FluidThermodynamics model) {
        var pkg = model.hydrocarbon.propertyPackage();
        var assay = pkg.assays().get("icheme:tia_juana_light_methane");
        double[] z = new double[model.componentCount()];
        for (int c = 0; c < assay.components().size(); c++) z[model.hydrocarbon.components().indexOf(assay.components().get(c))] = assay.amounts().get(c);
        return z;
    }

    /** The model's pure-component saturation temperature at {@code p}: a secant on ln Psat against 1/T from the NBP. */
    static double saturationTemperature(FluidThermodynamics model, String component, double p) {
        var property = model.hydrocarbon.propertyPackage().properties().get(model.hydrocarbon.components().indexOf(component));
        double tc = property.pr().criticalTemperature(), pc = property.pr().criticalPressure(), omega = property.pr().acentricFactor();
        double x0 = 1 / property.normalBoilingPoint(), x1 = x0 * (p < 101325 ? 1.02 : 0.98);
        double f0 = Double.NaN, f1;
        for (int k = 0; k < 60; k++) {
            double t = 1 / x1;
            double wilson = pc * Math.exp(5.373 * (1 + omega) * (1 - tc / t));
            double psat = G3Support.saturationPressure(model, component, t, wilson);
            f1 = Math.log(psat / p);
            if (Math.abs(f1) < 1e-11) return t;
            if (!Double.isFinite(f1)) return Double.NaN;
            if (k == 0) {
                double t0 = 1 / x0;
                f0 = Math.log(G3Support.saturationPressure(model, component, t0, pc * Math.exp(5.373 * (1 + omega) * (1 - tc / t0))) / p);
            }
            double next = x1 - f1 * (x1 - x0) / (f1 - f0);
            next = Math.max(1 / (0.999 * tc), Math.min(1 / (0.3 * property.normalBoilingPoint()), next));
            x0 = x1;
            f0 = f1;
            x1 = next;
        }
        return Double.NaN;
    }

    @Test
    void vacuumAndAtmosphericCutSaturation() {
        var model = network();
        var table = new Table("G3 F6 Tsat", "Cut", "NBP K (record)", "PR Tsat(101.325 kPa) - NBP K", "PR Tsat 1 / 5 / 13 kPa K",
                "D5 PR - MB 1 / 5 / 13 kPa K (recorded)", "Implied MB Tsat 13 kPa K", "PR Tsat 250 kPa K", "PR - MB 250 kPa K (recorded)");
        for (int c = 0; c < CUTS.length; c++) {
            var property = model.hydrocarbon.propertyPackage().properties().get(model.hydrocarbon.components().indexOf(CUTS[c]));
            double nbp = property.normalBoilingPoint();
            double t101 = saturationTemperature(model, CUTS[c], 101325), t1 = saturationTemperature(model, CUTS[c], 1e3),
                    t5 = saturationTemperature(model, CUTS[c], 5e3), t13 = saturationTemperature(model, CUTS[c], 13e3),
                    t250 = saturationTemperature(model, CUTS[c], 250e3);
            table.row(CUTS[c], f("%.1f", nbp), f("%+.3f", t101 - nbp), f("%.1f / %.1f / %.1f", t1, t5, t13),
                    f("%+.1f / %+.1f / %+.1f", PR_MINUS_MB[c][0], PR_MINUS_MB[c][1], PR_MINUS_MB[c][2]), f("%.1f", t13 - PR_MINUS_MB[c][2]),
                    f("%.1f", t250), f("%+.2f", PR_MINUS_MB[c][3]));
            // The P0 harness's PR78 passes through the record's NBP within 0.01 K; the network's cubic is the same PR78.
            assertEquals(nbp, t101, 0.05, CUTS[c] + " NBP");
            assertTrue(t1 < t5 && t5 < t13 && t13 < t101 && t101 < t250, CUTS[c] + " monotone");
        }
        table.print("PR78 pure-cut saturation temperatures on the network package (probe; the D5 vacuum bias declared, P0 section 6.1)");
    }

    @Test
    void processFlashesThroughTheNetwork() {
        var model = network();
        double[] z = assay(model);
        int heavyStart = model.hydrocarbon.components().indexOf("crude_pc06");
        double[] residue = new double[z.length];
        for (int i = heavyStart; i < heavyStart + 7; i++) residue[i] = z[i];
        var engine = researchEngine(networkEvaluator(), "tjl20-network-probe");
        var table = new Table("G3 F6 flash", "Probe", "Feed", "T K", "P kPa", "Vapour fraction (network flash)", "Engine feed verdict",
                "Engine phases", "Verdict = phase count", "K-values PC06..PC12 (y/x)");
        record Probe(String name, boolean residueFeed, double t, double p) {}
        var probes = List.of(new Probe("VDU Case A", true, 655.15, 13.1e3), new Probe("VDU deep cut", true, 673, 4e3),
                new Probe("CDU flash zone (literature preset feed)", false, 638.15, 250e3),
                new Probe("Coking drum feed", false, 739, 200e3), new Probe("Coking drum", false, 761, 340e3),
                new Probe("Coking heater outlet", false, 783, 520e3), new Probe("Coking heater outlet", false, 755, 520e3),
                new Probe("Coking drum", false, 783, 200e3));
        var ws = engine.newWorkspace();
        for (var probe : probes) {
            double[] feed = probe.residueFeed() ? residue : z;
            var state = model.flashTP(probe.t(), probe.p(), feed, () -> { });
            double vapour = 0, total = 0;
            for (int i = 0; i < state.vaporView().length; i++) {
                vapour += state.vaporView()[i];
                total += state.vaporView()[i] + state.liquidView()[i];
            }
            var result = engine.tp(EquilibriumRequest.tp(probe.t(), probe.p(), engine.contract().components(),
                    java.util.Arrays.copyOf(feed, engine.contract().components().size()), PhaseCompetition.FLUID_ONLY), ws);
            assertTrue(result.converged(), probe.name() + " " + result.detail());
            var verdict = result.diagnostics().feedVerdict();
            boolean consistent = verdict == TangentPlaneStability.Verdict.UNSTABLE ? result.phases().size() == 2 : result.phases().size() == 1;
            assertTrue(consistent, probe.name() + ": verdict " + verdict + ", " + result.phases().size() + " phases");
            boolean networkTwoPhase = vapour > 0 && vapour < total;
            assertEquals(result.phases().size() == 2, networkTwoPhase, probe.name() + ": network phase count = engine phase count");
            var k = new StringBuilder();
            if (networkTwoPhase) {
                double liquidTotal = total - vapour;
                for (int i = heavyStart; i < heavyStart + 7; i++) {
                    double y = state.vaporView()[i] / vapour, x = state.liquidView()[i] / liquidTotal;
                    k.append(i == heavyStart ? "" : " ").append(f("%.3g", y / x));
                }
            }
            table.row(probe.name(), probe.residueFeed() ? "PC06-PC12 of the assay" : "whole assay", number(probe.t()), f("%.1f", probe.p() / 1e3),
                    f("%.4f", vapour / total), verdict, result.phases().size(), consistent ? "yes" : "NO", networkTwoPhase ? k : "-");
        }
        table.print("process flashes of the Tia Juana light assay on the network (probe; VDU K-values carry the D5 bias: PR/MB vapour"
                + " pressure 1.13-1.24 at 13 kPa for PC06-PC12, i.e. K 13-24 % high)");
    }

    @Test
    void hydrotreatingPressureIsCarriedByTheNetworkAndProbedOnAResearchContract() {
        var model = network();
        double[] z = assay(model);
        // Past the crude records' 2 MPa data the network carries the state on their continuations (owner rule of
        // 2026-10-16): no refusal, a notice.
        var carried = assertDoesNotThrow(() -> model.flashTP(623, 9.1e6, z, () -> { }));
        assertTrue(Double.isFinite(carried.enthalpy()) && carried.volume() > 0);
        System.out.println("G3 F6 hydrotreating on the network: carried, vapour " + f("%.4f", java.util.Arrays.stream(carried.vaporView()).sum() / java.util.Arrays.stream(z).sum()));
        var engine = researchEngine(networkEvaluator(), "tjl20-hydrotreating-probe");
        var ws = engine.newWorkspace();
        var table = new Table("G3 F6 hydrotreating", "T K", "P MPa", "Status", "Classification", "Phases (labels)", "Vapour fraction",
                "Feed verdict", "Grade");
        for (double p : new double[] {6.1e6, 9.1e6}) {
            for (double t : new double[] {589, 623, 663, 713}) {
                var result = engine.tp(EquilibriumRequest.tp(t, p, engine.contract().components(), java.util.Arrays.copyOf(z, engine.contract().components().size()),
                        PhaseCompetition.FLUID_ONLY), ws);
                assertTrue(result.converged(), t + " K " + p + " Pa: " + result.detail());
                var labels = new StringBuilder();
                double vapour = 0;
                for (var phase : result.phases()) {
                    labels.append(labels.isEmpty() ? "" : ", ").append(phase.kind());
                    if (phase.kind() == PhaseKind.VAPOR) vapour += phase.total();
                }
                var verdict = result.diagnostics().feedVerdict();
                assertTrue(verdict == TangentPlaneStability.Verdict.UNSTABLE ? result.phases().size() == 2 : result.phases().size() == 1);
                assertEquals(EquilibriumResult.CoverageGrade.RESEARCH_ONLY, result.coverage().grade());
                table.row(number(t), f("%.1f", p / 1e6), result.status(), result.classification(), result.phases().size() + " (" + labels + ")",
                        f("%.4f", vapour / result.totalAmount()), verdict, result.coverage().grade());
            }
        }
        table.print("the assay at hydrotreater temperatures and pressures on an open research contract (probe, no H2; the network refuses)");
    }

    @Test
    void cutLiquidDensitiesAtThreeHundredKelvinToTenMegapascals() {
        var model = network();
        var table = new Table("G3 F6 liquid", "Component", "rho 0.1 MPa kg/m3", "2 MPa", "5 MPa", "10 MPa", "rho(10)/rho(0.1) - 1 %", "Roots liquid-like");
        for (String component : new String[] {"N-butane", "N-pentane", "crude_pc01", "crude_pc03", "crude_pc06", "crude_pc09", "crude_pc12"}) {
            double[] x = pure(model, component);
            double mw = model.hydrocarbon.molecularWeight(model.hydrocarbon.components().indexOf(component));
            double[] rho = new double[4];
            double[] pressures = {1e5, 2e6, 5e6, 1e7};
            boolean liquidLike = true;
            for (int k = 0; k < 4; k++) {
                var e = evaluate(model, 300, pressures[k], x, Root.LIQUID);
                rho[k] = mw / e.molarVolume();
                liquidLike &= e.roots() == 3 || e.pip() > 1;
            }
            table.row(component, f("%.1f", rho[0]), f("%.1f", rho[1]), f("%.1f", rho[2]), f("%.1f", rho[3]), f("%.2f", 100 * (rho[3] / rho[0] - 1)),
                    liquidLike ? "yes" : "NO");
            assertTrue(liquidLike, component + " liquid root");
            assertTrue(rho[3] > rho[2] && rho[2] > rho[1] && rho[1] > rho[0], component + " compresses");
        }
        table.print("cut and light-end liquids at 300 K on the network's direct liquid path (probe; above 2 MPa these records are not qualified)");
    }
}
