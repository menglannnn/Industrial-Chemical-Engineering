package com.wormzjl.createcheme.science.thermo.qualification;

import static com.wormzjl.createcheme.science.thermo.qualification.G3Support.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.createcheme.science.thermo.reference.HelmholtzFluid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Test;

/**
 * G3 fixture family F4 (plan {@code P3_PILOT_ENGINE_PLAN.md} sections 2.6 and 8.2; D7, D14, D15): pressures up to 10 MPa and
 * continuity through the retired 2 MPa ceiling, for the pilot species ({@code createcheme:pilot_cryogenic}) and for the
 * bundled network package's nitrogen and methane records ({@code createcheme:tjl20_methane_nitrogen}, 10 MPa since WP7).
 *
 * <ul>
 *   <li>The P1 dense and supercritical probe states (P1 study section 2.5, H2 omitted) scored at the D7 targets and the
 *   D14 and D15 bounds; band states reported (box 2 widened by D15 since P3 WP7d).</li>
 *   <li>Isotherms from 0.1 MPa (or just above saturation) to 10 MPa: liquid isotherms at Tr 0.6, 0.7, 0.8, 0.9 and
 *   supercritical ones at Tr 1.3 and 2 for the pilot species; liquid nitrogen at 77, 90, 100 and 110 K and methane gas
 *   at 300 and 400 K through the bundled network's own domain-checked {@code phase}. Every point against the oracle;
 *   second differences of v(P) and h(P) across 2 MPa without a jump (the check of {@code DirectLiquidContinuityTest},
 *   here on the pilot and on the widened bundled domain).</li>
 * </ul>
 *
 * The closed-vessel compression of liquid nitrogen through 2 MPa (plan section 2.6 item 3) is WP4's
 * {@code DirectLiquidContinuityTest.aClosedLiquidNitrogenVesselCompressedThroughTwoMegapascalsFollowsTheAdiabat}; it is
 * not repeated here.
 */
class G3F4HighPressureContinuityTest {
    /** P1 section 2.5 rows (anchor b columns): species index, T K, P MPa, printed density %, printed cp %. */
    static final double[][] P1_DENSE = {
            {0, 77.36, 5, 1.53, -6.07}, {0, 77.36, 10, 1.46, -6.79}, {0, 100, 10, 1.79, -1.53}, {0, 126.2, 3.4, -14.39, 89.2}, {0, 130, 4, -4.03, -27.6},
            {0, 130, 6, -5.60, 7.05}, {0, 130, 10, -1.55, 4.14}, {0, 150, 10, -4.05, -3.75}, {0, 300, 10, 0.24, 1.00},
            {1, 110, 10, 2.14, -1.39}, {1, 150, 10, 1.76, 2.10}, {1, 190.6, 4.6, -2.47, -26.7}, {1, 200, 6, -5.20, -22.9}, {1, 200, 10, -4.69, 4.95},
            {1, 250, 10, 0.11, -4.56}, {1, 300, 10, 0.95, -0.17},
            {2, 200, 10, 2.20, -3.63}, {2, 300, 10, -2.91, 6.25}, {2, 305.4, 4.9, -15.96, 10.8}, {2, 320, 6, -0.31, -13.9}, {2, 320, 10, -5.92, 4.59},
            {2, 400, 10, 0.01, -2.04},
            {3, 280, 8, -3.24, 9.22}, {3, 280, 10, -2.38, 6.80}, {3, 300, 8, -10.06, 22.6}, {3, 304.5, 7.4, -1.36, -19.9}, {3, 310, 8, 0.17, -12.8},
            {3, 310, 10, -10.86, 11.5}, {3, 320, 8, 1.99, -7.07}, {3, 320, 10, -6.60, -20.8}, {3, 350, 10, 0.87, -3.61}, {3, 400, 10, 0.58, 0.01}};

    @Test
    void p1DenseAndSupercriticalStatesAtTheTargets() {
        var table = new Table("G3 F4 P1", "Package", "Species", "T K", "P MPa", "Region", "rho %", "cp %", "P1 rho / cp %", "D7 rho", "Held rho");
        int scored = 0, band = 0;
        for (var model : List.of(pilot(), network())) {
            boolean isPilot = model == pilot();
            for (double[] row : P1_DENSE) {
                int s = (int) row[0];
                if (!isPilot && (s == 2 || s == 3 || s == 1 && row[1] < 293.15)) continue; // the bundled network: N2, and CH4 from 293.15 K
                var fluid = oracle(s);
                double t = row[1], p = row[2] * 1e6, tc = fluid.criticalTemperature(), pc = fluid.criticalPressure();
                boolean liquid = t < tc && p > fluid.saturation(Math.min(t, tc - 1e-6)).pressure();
                boolean vapour = !liquid && (t < tc || p < pc);
                var e = evaluate(model, t, p, pure(model, SPECIES[s]), liquid ? Root.LIQUID : vapour ? Root.VAPOR : null);
                var reference = fluid.stateAtPressure(t, p, liquid ? HelmholtzFluid.Root.LIQUID : HelmholtzFluid.Root.VAPOUR);
                double rho = 100 * (e.density() / reference.density() - 1), cp = 100 * (e.heatCapacity() / reference.isobaricHeatCapacity() - 1);
                var rule = densityRule(fluid, t, p, reference.compressibility());
                table.row(isPilot ? "pilot" : "net", SHORT[s], number(t), number(row[2]), rule.region(), f("%+.2f", rho), f("%+.2f", cp),
                        isPilot ? f("%+.2f / %+.2f", row[3], row[4]) : "(anchor a)", verdict(rule.d7(), rho),
                        rule.scored() ? number(rule.held()) + " %" : rule.region().startsWith("band") ? "research-only" : "report");
                if (!rule.scored()) {
                    band++;
                    continue;
                }
                scored++;
                assertTrue(Math.abs(rho) <= rule.held(), SHORT[s] + " " + t + " K " + row[2] + " MPa density " + rho);
                // Outside the band the D7 supercritical and near-Tc liquid cp targets are 10 % (liquid cp Tr > 0.9 is flagged).
                if (!liquid || t / tc <= 0.9) assertTrue(Math.abs(cp) <= 12, SHORT[s] + " cp " + cp);
                if (isPilot) assertEquals(row[3], rho, 0.02, "P1 reproduced"); // ethane's record constants differ by 1e-5 (WP5)
            }
        }
        table.print(f("P1 dense and supercritical probe states: %d scored, %d in the band or report-only", scored, band));
    }

    record Isotherm(FluidThermodynamics model, String packageLabel, int species, double temperature, boolean networkEntry) {}

    @Test
    void isothermsToTenMegapascalsAndNoJumpAtTwo() {
        List<Isotherm> isotherms = new ArrayList<>();
        for (int s = 0; s < 4; s++) {
            double tc = oracle(s).criticalTemperature();
            for (double tr : new double[] {0.6, 0.7, 0.8, 0.9, 1.3, 2.0}) {
                if (tr * tc < oracle(s).tripleTemperature()) continue; // CO2's triple point is at Tr 0.712
                isotherms.add(new Isotherm(pilot(), "pilot", s, tr * tc, false));
            }
        }
        for (double t : new double[] {77, 90, 100, 110}) isotherms.add(new Isotherm(network(), "net", 0, t, true));
        for (double t : new double[] {300, 400}) isotherms.add(new Isotherm(network(), "net", 1, t, true));
        var table = new Table("G3 F4 isotherm", "Package", "Species", "T K", "Tr", "From MPa", "Points", "Worst rho % (P MPa)", "D7 rho met", "Held",
                "Band or report-only points", "2 MPa second differences");
        int points = 0;
        for (var iso : isotherms) {
            var fluid = oracle(iso.species());
            double t = iso.temperature(), tc = fluid.criticalTemperature(), tr = t / tc;
            double[] x = pure(iso.model(), SPECIES[iso.species()]);
            double psat = t < tc ? fluid.saturation(t).pressure() : 0;
            boolean liquidIsotherm = t < tc;
            double start = liquidIsotherm ? Math.max(1e5, 1.02 * psat) : 1e5;
            Root root = liquidIsotherm ? Root.LIQUID : null;
            var worst = new Worst();
            double held = 0;
            int band = 0, n = 0, d7met = 0;
            for (int k = 0; k <= 40; k++) {
                double p = k == 40 ? 1e7 : start * Math.pow(1e7 / start, k / 40.0);
                if (liquidIsotherm && p > meltingPressure(SPECIES[iso.species()], t)) continue;
                if (iso.networkEntry()) {
                    // The bundled network's own entry: domain-checked (N2 and CH4 to 10 MPa since WP7), liquid at the state pressure.
                    var phase = iso.model().hydrocarbon.phase(t, p, x, liquidIsotherm ? Root.LIQUID : Root.VAPOR);
                    assertTrue(phase.molarVolume() > 0);
                }
                var e = evaluate(iso.model(), t, p, x, root);
                var reference = fluid.stateAtPressure(t, p, liquidIsotherm ? HelmholtzFluid.Root.LIQUID : HelmholtzFluid.Root.VAPOUR);
                double rho = 100 * (e.density() / reference.density() - 1);
                var rule = densityRule(fluid, t, p, reference.compressibility());
                if (!rule.scored()) {
                    band++;
                    continue;
                }
                if (!(Math.abs(rho) > rule.d7())) d7met++;
                held = Math.max(held, rule.held());
                worst.add(rho, f("%.2f", p / 1e6));
                assertTrue(Math.abs(rho) <= rule.held(), iso.packageLabel() + " " + SHORT[iso.species()] + " " + t + " K " + p + " Pa: " + rho);
                n++;
            }
            points += n;
            // Differences across 2 MPa on 41 points in one phase region outside the band: 2 MPa +- 1 MPa, narrowed for a
            // liquid whose saturation pressure lies above 1 MPa (the window starts at 1.05 Psat).
            String smooth;
            double step = liquidIsotherm ? Math.min(5e4, (2e6 - 1.05 * psat) / 20) : 5e4;
            boolean window = step >= 5e3 && bandBox(tr, (2e6 - 20 * step) / fluid.criticalPressure()) == 0
                    && bandBox(tr, (2e6 + 20 * step) / fluid.criticalPressure()) == 0 && (liquidIsotherm || t > tc * 1.2);
            if (window) {
                // Liquids: v(P); gases: P v(P), whose second difference is nearly constant (v itself curves as 1/P).
                DoubleUnaryOperator v = liquidIsotherm ? p -> evaluate(iso.model(), t, p, x, root).molarVolume()
                        : p -> p * evaluate(iso.model(), t, p, x, root).molarVolume();
                DoubleUnaryOperator h = p -> evaluate(iso.model(), t, p, x, root).enthalpy();
                double jv = jump(v, step), jh = jump(h, step);
                smooth = f("%s %.1e, h %.1e (%.2f-%.2f MPa)", liquidIsotherm ? "v" : "Pv", jv, jh, (2e6 - 20 * step) / 1e6, (2e6 + 20 * step) / 1e6);
                assertTrue(jv <= 0.02 && jh <= 0.02, iso.packageLabel() + " " + SHORT[iso.species()] + " " + t + " K: second differences jump");
            } else smooth = "2 MPa at or below 1.05 Psat: no liquid window";
            table.row(iso.packageLabel(), SHORT[iso.species()], f("%.2f", t), f("%.2f", tr), f("%.3f", start / 1e6), n, worst.signed("%+.2f"),
                    d7met + " of " + n, number(held) + " %", band, smooth);
        }
        table.print(f("isotherms to 10 MPa (%d points scored); differences on 41 points around 2 MPa: largest fourth difference"
                + " relative to the largest second difference (a jump at 2 MPa would be of order 1)", points));
    }

    /**
     * f on 41 points 2 MPa +- 20 h: the largest fourth difference relative to the largest second difference. A
     * smooth isotherm's second differences drift slowly (a gas's curvature grows with P, a liquid's near its spinodal), so
     * their own second differences stay small; a kink or jump at 2 MPa makes one of order one next to it.
     */
    static double jump(DoubleUnaryOperator f, double h) {
        int n = 20;
        double[] y = new double[2 * n + 1];
        for (int j = -n; j <= n; j++) y[j + n] = f.applyAsDouble(2e6 + j * h);
        double[] second = new double[y.length - 2];
        double largest = 0, fourth = 0;
        for (int j = 0; j < second.length; j++) {
            second[j] = y[j + 2] - 2 * y[j + 1] + y[j];
            largest = Math.max(largest, Math.abs(second[j]));
        }
        for (int j = 0; j + 2 < second.length; j++) fourth = Math.max(fourth, Math.abs(second[j + 2] - 2 * second[j + 1] + second[j]));
        return fourth / largest;
    }
}
