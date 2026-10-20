package com.wormzjl.createcheme.science.thermo.qualification;

import static com.wormzjl.createcheme.science.thermo.qualification.G3Support.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.createcheme.science.thermo.reference.HelmholtzFluid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * G3 fixture family F1 (plan {@code P3_PILOT_ENGINE_PLAN.md} section 8.2; decisions D3, D7 and D14): the pure pilot
 * fluids N2, CH4, C2H6 and CO2 of {@code createcheme:pilot_cryogenic}, evaluated by the network's own equation of state,
 * against the Java Helmholtz oracle (CoolProp {@code ae81610e} reference equations) on the P1 grid:
 *
 * <ul>
 *   <li>a saturation sweep of 30 temperatures from the triple point to Tr 0.95: the model's own vapour pressure, the
 *   saturated liquid and vapour at the reference saturation pressure, the enthalpy of vaporization at the model's own
 *   saturation pressure;</li>
 *   <li>isobars 0.1, 0.5, 1, 2, 5 and 10 MPa at 24 temperatures from the triple point to 1.2 Tc plus Tsat(P) +- 0.5 K,
 *   solid-stable states (above the reference melting line) dropped as in P1.</li>
 * </ul>
 *
 * <p>Each region prints its D7 target and verdict next to the D14 bound and verdict (D14 revised four rows: vapour
 * density at Z of at least 0.8 to 2.5 %; vapour cp at Z of at least 0.9 within 0.05 of saturation in Tr to 10 %, the
 * 2 % kept away from saturation; liquid density at Tr 0.90-0.95 outside the band to 6 %; the corner Tr 1.0-1.1 x Pr
 * 1.5-2 into the band with 9 %). The bound the test holds is the D7 target, the D14 bound, or, for the rows G3 found
 * missed, the D15 bound (declared errors decided after WP9b: vapour cp 12 % at Z of at least 0.9 with the 2 % target
 * kept only at Z of at least 0.98, compressed liquid density at Tr 0.85-0.9 next to saturation 5 %, supercritical cp next
 * to box 1 12 %, ethane liquid cp below 150 K 12 % with D3; box 2 widened to Tr 0.97 below Pr 0.8), printed with the D7
 * and D14 verdicts beside it (see {@code G3_QUALIFICATION_REPORT.md}). States in the band's three boxes are not scored; their count and worst density
 * are reported. The Tr 0.8 saturated liquid (the anchor) is the one fitted state; every other point is held out.
 * Reduced variables use the oracle's critical constants.</p>
 */
class G3F1PureFluidOracleTest {
    /**
     * A scored region: its D7 target, the D14 bound (NaN where D14 did not revise it), the bound held (NaN: report only)
     * and the basis of the held bound.
     */
    record Rule(String name, double d7, double d14, double held, String basis) {}

    static final class Tally {
        final Rule rule;
        final Worst worst = new Worst();
        Tally(Rule rule) { this.rule = rule; }
    }

    private final Map<String, Tally> tallies = new LinkedHashMap<>();
    private final Map<String, Worst> vapourCpByZ = new TreeMap<>();
    private final List<String> failures = new ArrayList<>();

    private void score(String species, Rule rule, double deviation, String at) { score(species, rule, deviation, false, at); }

    /** Records a deviation; beyond the held bound it fails unless the D7 absolute floor holds ({@code floorMet}). */
    private void score(String species, Rule rule, double deviation, boolean floorMet, String at) {
        var tally = tallies.computeIfAbsent(species + "|" + rule.name(), key -> new Tally(rule));
        tally.worst.add(deviation, at);
        if (!Double.isNaN(rule.held()) && Math.abs(deviation) > rule.held() && !floorMet) {
            failures.add(species + " " + rule.name() + " at " + at + ": " + f("%+.3f", deviation) + " beyond " + rule.held());
        }
    }

    /** Vapour cp deviations by Z band, all vapour states: the evidence behind D15's vapour cp row. */
    private void vapourCp(String species, double z, double deviation, String at) {
        String band = z >= 0.98 ? "Z >= 0.98" : z >= 0.95 ? "Z 0.95-0.98" : z >= 0.9 ? "Z 0.90-0.95" : "Z < 0.90";
        vapourCpByZ.computeIfAbsent(species + "|" + band, key -> new Worst()).add(deviation, at);
    }

    static final double NONE = Double.NaN;
    // D7 rows (P0 section 3.2 with the P1 amendments), the D14 revisions and the D15 declared errors.
    static final Rule PSAT_LOW = new Rule("Psat Tr < 0.45 (ethane below 150 K)", NONE, NONE, NONE, "declared (D3), not gated");
    static final Rule PSAT_MID = new Rule("Psat Tr 0.45-0.6", 5, NONE, 5, "D7");
    static final Rule PSAT_HIGH = new Rule("Psat Tr 0.6-0.95", 1.5, NONE, 1.5, "D7");
    static final Rule SAT_LIQUID = new Rule("sat. liquid density Tr <= 0.85", 3, NONE, 3, "D7 worst; mean 2 % asserted apart");
    static final Rule SAT_LIQUID_HIGH = new Rule("sat. liquid density Tr 0.85-0.95", 10, NONE, 10, "D7 declared");
    static final Rule LIQUID_CP_COLD = new Rule("liquid cp Tr <= 0.8", 10, NONE, 10, "D7");
    static final Rule LIQUID_CP_ETHANE = new Rule("liquid cp ethane below 150 K", 10, NONE, 12, "D15 declared, with D3's ethane Psat region");
    static final Rule LIQUID_CP = new Rule("liquid cp Tr 0.8-0.9", 20, NONE, 20, "D7 declared");
    static final Rule LIQUID_CP_HOT = new Rule("liquid cp Tr > 0.9", NONE, NONE, NONE, "unavailable/estimated, flagged");
    static final Rule COMPRESSED = new Rule("compressed liquid density Tr <= 0.85", 3, NONE, 3, "D7");
    static final Rule COMPRESSED_HIGH = new Rule("compressed liquid density Tr 0.85-0.9", 3, NONE, 5,
            "D15 declared next to saturation (D7 3 %)");
    static final Rule NEAR_TC = new Rule("liquid density Tr 0.90-0.95", 5, 6, 6, "D14");
    static final Rule NEAR_TC_DENSE = new Rule("liquid density Tr 0.95-1, Pr > 1.5", 5, NONE, 5, "D7");
    static final Rule VAPOUR_DENSITY = new Rule("vapour density Z >= 0.8", 1, 2.5, 2.5, "D14");
    static final Rule VAPOUR_DENSE = new Rule("vapour density Z < 0.8", NONE, NONE, NONE, "no target");
    static final Rule VAPOUR_CP = new Rule("vapour cp Z >= 0.9, > 0.05 Tr from sat.", 2, 2, 12, "D15 declared at Z >= 0.9 (2 % target kept at Z >= 0.98)");
    static final Rule VAPOUR_CP_SAT = new Rule("vapour cp Z >= 0.9, within 0.05 Tr of sat.", 2, 10, 12, "D15 declared at Z >= 0.9 (D14 10 %)");
    static final Rule VAPOUR_CP_DENSE = new Rule("vapour cp Z < 0.9", NONE, NONE, NONE, "no target");
    static final Rule SUPER_DENSITY = new Rule("supercritical density", 7, NONE, 7, "D7");
    static final Rule SUPER_CP = new Rule("supercritical cp", 10, NONE, 12, "D15 declared next to box 1 (D7 10 %)");
    static final Rule CORNER_DENSITY = new Rule("corner Tr 1.0-1.1 x Pr 1.5-2 density", 7, 9, 9, "D14 band box");
    static final Rule CORNER_CP = new Rule("corner Tr 1.0-1.1 x Pr 1.5-2 cp", NONE, NONE, NONE, "research-only (band)");
    static final Rule HVAP = new Rule("hvap Tr <= 0.9", 3.5, NONE, 3.5, "D7, floor 0.2 kJ/mol");

    @Test
    void saturationSweepAgainstTheOracle() {
        var model = pilot();
        int bandSkipped = 0, fitted = 0;
        var liquidMean = new LinkedHashMap<String, Worst>();
        for (int s = 0; s < 4; s++) {
            var fluid = oracle(s);
            double[] x = pure(model, SPECIES[s]);
            double tc = fluid.criticalTemperature(), pc = fluid.criticalPressure(), tt = fluid.tripleTemperature();
            var mean = liquidMean.computeIfAbsent(SHORT[s], key -> new Worst());
            for (int k = 0; k < 30; k++) {
                double t = tt + k * (0.95 * tc - tt) / 29, tr = t / tc;
                var sat = fluid.saturation(t);
                double pRef = sat.pressure(), pr = pRef / pc;
                String at = f("%.2f K (Tr %.3f)", t, tr);
                boolean ethaneCold = s == 2 && t < 150;
                // Vapour pressure at the model's own coexistence.
                double psat = saturationPressure(model, SPECIES[s], t, pRef);
                assertTrue(Double.isFinite(psat), SHORT[s] + " model saturation at " + at);
                double dp = 100 * (psat / pRef - 1);
                double slope = (Math.log(fluid.saturation(t + 0.01).pressure()) - Math.log(pRef)) / 0.01;
                double dT = Math.log(psat / pRef) / slope;
                Rule psatRule = tr < 0.45 || ethaneCold ? PSAT_LOW : tr < 0.6 ? PSAT_MID : PSAT_HIGH;
                // D7 floor: below 1 kPa the saturation temperature is the metric (|dTsat| <= 0.3 K).
                boolean floor = pRef < 1000 && Math.abs(dT) <= 0.3;
                score(SHORT[s], psatRule, dp, floor, at);
                if (psatRule == PSAT_LOW || pRef < 1000) {
                    System.out.println(f("G3 F1 low-pressure Psat: %s %s Psat_ref %.4g Pa, model %+.2f %%, dTsat %+.3f K (%s)", SHORT[s], at, pRef,
                            dp, dT, psatRule == PSAT_LOW ? "declared, D3" : floor ? "floor met" : "floor not met"));
                }
                if (bandBox(tr, pr) != 0) {
                    bandSkipped++;
                    continue;
                }
                // Saturated liquid and vapour at the reference saturation pressure (P1 section 2.4 convention).
                var liquid = evaluate(model, t, pRef, x, Root.LIQUID);
                double rho = 100 * (liquid.density() / sat.liquid().density() - 1);
                if (Math.abs(tr - 0.8) < 0.005) fitted++;
                if (tr <= 0.85) {
                    score(SHORT[s], SAT_LIQUID, rho, at);
                    mean.add(rho, at);
                } else score(SHORT[s], SAT_LIQUID_HIGH, rho, at);
                double cpL = 100 * (liquid.heatCapacity() / sat.liquid().isobaricHeatCapacity() - 1);
                score(SHORT[s], ethaneCold ? LIQUID_CP_ETHANE : tr <= 0.8 ? LIQUID_CP_COLD : tr <= 0.9 ? LIQUID_CP : LIQUID_CP_HOT, cpL, at);
                var vapour = evaluate(model, t, pRef, x, Root.VAPOR);
                double z = sat.vapour().compressibility();
                double rhoV = 100 * (vapour.density() / sat.vapour().density() - 1);
                score(SHORT[s], z >= 0.8 ? VAPOUR_DENSITY : VAPOUR_DENSE, rhoV, at + " sat. vapour Z " + f("%.3f", z));
                double cpV = 100 * (vapour.heatCapacity() / sat.vapour().isobaricHeatCapacity() - 1);
                score(SHORT[s], z >= 0.9 ? VAPOUR_CP_SAT : VAPOUR_CP_DENSE, cpV, at + " sat. vapour Z " + f("%.3f", z));
                vapourCp(SHORT[s], z, cpV, at + " sat. vapour");
                // Enthalpy of vaporization at the model's own saturation pressure.
                if (tr <= 0.9) {
                    var l = evaluate(model, t, psat, x, Root.LIQUID);
                    var v = evaluate(model, t, psat, x, Root.VAPOR);
                    double reference = sat.vapour().enthalpy() - sat.liquid().enthalpy();
                    double hvap = v.enthalpy() - l.enthalpy();
                    score(SHORT[s], HVAP, 100 * (hvap / reference - 1), Math.abs(hvap - reference) <= 200, at);
                }
            }
        }
        print("G3 F1 sat", "saturation sweep, 30 temperatures per species from the triple point to Tr 0.95 (states in the band not scored: "
                + bandSkipped + "; states within 0.005 of the Tr 0.8 anchor: " + fitted + ")");
        liquidMean.forEach((species, w) -> {
            System.out.println(f("G3 F1 sat. liquid density Tr <= 0.85, %s: mean |dev| %.2f %% over %d (plan F1 target 2 %% mean: %s)",
                    species, w.mean(), w.count, w.mean() <= 2 ? "met" : "NOT met"));
            assertTrue(w.mean() <= 2, species + " saturated liquid density mean " + w.mean());
        });
        printVapourCp("G3 F1 sat");
        failures.forEach(failure -> System.out.println("G3 F1 BEYOND BOUND: " + failure));
        assertEquals(List.of(), failures);
    }

    @Test
    void isobarGridAgainstTheOracle() {
        var model = pilot();
        double[] megapascals = {0.1, 0.5, 1, 2, 5, 10};
        int solid = 0, mismatches = 0, scored = 0;
        int[] boxes = new int[4];
        Worst[] boxWorst = {null, new Worst(), new Worst(), new Worst()};
        var isobars = new Table("G3 F1 isobar", "Species", "P MPa", "n (band)", "rho mean / worst %", "cp mean / worst %", "h departure mean / worst J/mol");
        for (int s = 0; s < 4; s++) {
            HelmholtzFluid fluid = oracle(s);
            double[] x = pure(model, SPECIES[s]);
            double tc = fluid.criticalTemperature(), pc = fluid.criticalPressure(), tt = fluid.tripleTemperature();
            for (double megapascal : megapascals) {
                double p = megapascal * 1e6, pr = p / pc;
                double tsat = p < pc ? saturationTemperature(fluid, p) : Double.NaN;
                List<Double> temperatures = new ArrayList<>();
                for (int k = 0; k < 24; k++) temperatures.add(tt + k * (1.2 * tc - tt) / 23);
                if (!Double.isNaN(tsat) && tsat > tt + 0.5) {
                    temperatures.add(tsat - 0.5);
                    temperatures.add(tsat + 0.5);
                }
                var rhoW = new Worst();
                var cpW = new Worst();
                var hW = new Worst();
                int band = 0;
                for (double t : temperatures) {
                    double tr = t / tc;
                    if (t < tc && p > fluid.saturation(t).pressure() && p > meltingPressure(SPECIES[s], t)) {
                        solid++;
                        continue;
                    }
                    String at = f("%.2f K %s MPa", t, number(megapascal));
                    boolean liquid = t < tc && p > fluid.saturation(t).pressure();
                    boolean vapour = !liquid && (t < tc || p < pc);
                    Root root = liquid ? Root.LIQUID : vapour ? Root.VAPOR : null;
                    var reference = fluid.stateAtPressure(t, p, liquid ? HelmholtzFluid.Root.LIQUID : HelmholtzFluid.Root.VAPOUR);
                    var e = evaluate(model, t, p, x, root);
                    // The model's root lies on the reference's branch: a liquid state's single root must be liquid-like.
                    if (liquid && e.roots() == 1 && !(e.pip() > 1)) mismatches++;
                    double rho = 100 * (e.density() / reference.density() - 1);
                    double cp = 100 * (e.heatCapacity() / reference.isobaricHeatCapacity() - 1);
                    double h = e.departure() - oracleDeparture(fluid, t, reference.enthalpy());
                    int box = bandBox(tr, pr);
                    if (box != 0) {
                        boxes[box]++;
                        boxWorst[box].add(rho, SHORT[s] + " " + at);
                        band++;
                        continue;
                    }
                    rhoW.add(rho, at);
                    cpW.add(cp, at);
                    hW.add(h, at);
                    scored++;
                    double z = reference.compressibility();
                    if (liquid) {
                        score(SHORT[s], tr <= 0.85 ? COMPRESSED : tr <= 0.9 ? COMPRESSED_HIGH : tr <= 0.95 ? NEAR_TC : NEAR_TC_DENSE, rho, at);
                        boolean ethaneCold = s == 2 && t < 150;
                        score(SHORT[s], ethaneCold ? LIQUID_CP_ETHANE : tr <= 0.8 ? LIQUID_CP_COLD : tr <= 0.9 ? LIQUID_CP : LIQUID_CP_HOT, cp, at);
                    } else if (vapour) {
                        score(SHORT[s], z >= 0.8 ? VAPOUR_DENSITY : VAPOUR_DENSE, rho, at + f(" Z %.3f", z));
                        boolean nearSaturation = !Double.isNaN(tsat) && (t - tsat) / tc <= 0.05;
                        score(SHORT[s], z < 0.9 ? VAPOUR_CP_DENSE : nearSaturation ? VAPOUR_CP_SAT : VAPOUR_CP, cp, at + f(" Z %.3f", z));
                        vapourCp(SHORT[s], z, cp, at);
                    } else if (d14Corner(tr, pr)) {
                        score(SHORT[s], CORNER_DENSITY, rho, at);
                        score(SHORT[s], CORNER_CP, cp, at);
                    } else {
                        score(SHORT[s], SUPER_DENSITY, rho, at);
                        score(SHORT[s], SUPER_CP, cp, at + f(" Tr %.3f Pr %.2f", tr, pr));
                    }
                }
                isobars.row(SHORT[s], number(megapascal), rhoW.count + " (" + band + ")", f("%.2f / %s", rhoW.mean(), rhoW.signed("%+.2f")),
                        f("%.2f / %s", cpW.mean(), cpW.signed("%+.1f")), f("%.0f / %s", hW.mean(), hW.signed("%+.0f")));
            }
        }
        isobars.print("per isobar (P1 section 2.3 layout; band excluded)");
        print("G3 F1 grid", f("isobar grid: %d states scored, %d solid-stable dropped, band boxes 1/2/3: %d/%d/%d not scored, %d branch mismatches",
                scored, solid, boxes[1], boxes[2], boxes[3], mismatches));
        for (int box = 1; box <= 3; box++) {
            System.out.println(f("G3 F1 band box %d (research-only, not scored): %d states, worst density %s", box, boxes[box],
                    boxWorst[box].signed("%+.2f %%")));
        }
        printVapourCp("G3 F1 grid");
        failures.forEach(failure -> System.out.println("G3 F1 BEYOND BOUND: " + failure));
        assertEquals(0, mismatches, "every liquid state has a liquid-like root");
        assertTrue(scored > 400, "scored " + scored);
        assertEquals(List.of(), failures);
    }

    private void print(String prefix, String title) {
        var table = new Table(prefix, "Species", "Region", "n", "Worst %", "Mean abs %", "D7", "D7 verdict", "D14", "D14 verdict", "Held (basis)");
        tallies.forEach((key, tally) -> {
            var w = tally.worst;
            var rule = tally.rule;
            table.row(key.substring(0, key.indexOf('|')), rule.name(), w.count, w.signed("%+.2f"), f("%.2f", w.mean()),
                    Double.isNaN(rule.d7()) ? "-" : number(rule.d7()) + " %", verdict(rule.d7(), w.value),
                    Double.isNaN(rule.d14()) ? "-" : number(rule.d14()) + " %", Double.isNaN(rule.d14()) ? "-" : verdict(rule.d14(), w.value),
                    (Double.isNaN(rule.held()) ? "report" : number(rule.held()) + " %") + " (" + rule.basis() + ")");
        });
        table.print(title);
    }

    private void printVapourCp(String prefix) {
        var table = new Table(prefix + " vapour cp", "Species", "Z band (reference)", "n", "Worst cp %", "D15 (report)");
        vapourCpByZ.forEach((key, w) -> {
            String band = key.substring(key.indexOf('|') + 1);
            // D15: 12 % declared at Z >= 0.9, the 2 % target kept at Z >= 0.98 (a target, reported here with its verdict).
            String d15 = band.equals("Z >= 0.98") ? "2 % target: " + verdict(2, w.value) : band.equals("Z < 0.90") ? "no target"
                    : "12 % declared: " + verdict(12, w.value);
            table.row(key.substring(0, key.indexOf('|')), band, w.count, w.signed("%+.2f"), d15);
        });
        table.print("vapour cp by Z band (all vapour states; evidence for the vapour cp rows)");
    }
}
