import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.createcheme.science.thermo.phase.FluidTpEquilibrium;
import com.wormzjl.createcheme.science.thermo.phase.PhaseAmounts;
import com.wormzjl.createcheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.createcheme.science.thermo.phase.PhaseKind;
import java.util.List;
import java.util.Locale;

/**
 * M3a probe (D21 follow-ups, batch 2026-09-24-coolprop-low-temperature): the PH point at f = 2e-6 that failed under
 * theta = 1e-6, row A's vessel around the new theta, and UV targets whose nested search crosses a vapour-solid to
 * liquid-solid step at one temperature (the stored inner secant, M1 proposal 4). Prints status, classification, T, P,
 * crystal, TP equilibria and kernel calls per case. No timing.
 */
public final class M3aProbe {
    static FluidTpEquilibrium service;
    static List<String> basis;
    static int n2, ch4, co2;

    public static void main(String[] args) {
        service = FluidTpEquilibrium.forPackage(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
        basis = service.contract().components();
        n2 = basis.indexOf("Nitrogen");
        ch4 = basis.indexOf("Methane");
        co2 = basis.indexOf("CarbonDioxide");
        String mode = args.length > 0 ? args[0] : "all";
        if (mode.equals("all") || mode.equals("ph")) ph();
        if (mode.equals("all") || mode.equals("uvrow")) uvRow();
        if (mode.equals("all") || mode.equals("triple")) triple();
        if (mode.equals("all") || mode.equals("grid")) grid();
        if (mode.equals("deep")) deep();
    }

    /**
     * Deep deposition at 101.325 kPa between the old and the new theta: PH targets at a vapour share beta of the pure
     * step's two ends. The rule answers down to beta = f / 1e-4 (STEP_FLUID_TRACES), the general solve below it.
     */
    static void deep() {
        var ws = service.newWorkspace();
        double p = 101325.0;
        double[] pure = feed(1.0, n2, 0.0);
        var solidEnd = service.tp(EquilibriumRequest.tp(190.0, p, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        var vapourEnd = service.tp(EquilibriumRequest.tp(200.0, p, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        var mid = service.ph(EquilibriumRequest.ph(p, 0.5 * (solidEnd.enthalpy() + vapourEnd.enthalpy()), basis, pure,
                PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        double hv = 0, hs = 0;
        for (PhaseAmounts phase : mid.phases()) {
            if (phase.kind() == PhaseKind.SOLID) hs = phase.state().molarEnthalpy(); else hv = phase.state().molarEnthalpy();
        }
        for (double f : new double[] {2e-6, 5e-6, 1e-5}) {
            double[] z = feed(1.0, n2, f);
            for (double beta : new double[] {0.5, 0.2, 0.12, 0.1, 0.08, 0.05, 0.02, 1e-2, 1e-3, 1e-4}) {
                var r = service.ph(EquilibriumRequest.ph(p, (1 + f / (1 - f)) * (hs + beta * (hv - hs)), basis, z,
                        PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                System.out.println(String.format(Locale.ROOT, "DEEP N2 f=%.0e beta=%.2g: ", f, beta) + line(r));
            }
        }
    }

    static double[] feed(double co2Amount, int trace, double f) {
        double[] z = new double[basis.size()];
        z[co2] = co2Amount;
        z[trace] = f / (1 - f) * co2Amount;
        return z;
    }

    /** PH at 101.325 kPa just above the old theta (f = 2e-6, dh = -1000 J failed at 8a06b22f) and around the new one. */
    static void ph() {
        var ws = service.newWorkspace();
        double p = 101325.0;
        for (double f : new double[] {1e-6, 2e-6, 5e-6, 1e-5, 2e-5}) {
            double[] z = feed(1.0, n2, f);
            double h0 = service.tp(EquilibriumRequest.tp(200.0, p, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws).enthalpy();
            for (double dh : new double[] {-1000, -5000, -24000}) {
                var r = service.ph(EquilibriumRequest.ph(p, h0 + dh, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                System.out.println(String.format(Locale.ROOT, "PH N2 f=%.0e dh=%.0f: ", f, dh) + line(r));
            }
        }
    }

    /** Row A's vessel (1 m3, 249.1435 mol CO2, 212.22 K) with N2 traces around the new theta. */
    static void uvRow() {
        var ws = service.newWorkspace();
        double u = -9.972099514183599e7, v = 1.0;
        for (double f : new double[] {0, 1e-6, 2e-6, 1e-5, 2e-5}) {
            var r = service.uv(EquilibriumRequest.uv(u, v, basis, feed(249.1435, n2, f), PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            System.out.println(String.format(Locale.ROOT, "UV rowA N2 f=%.0e: ", f) + line(r));
        }
    }

    /**
     * UV targets near CO2's triple point for traces above theta (the general solve, the binary's three-phase handling):
     * built from the pure triple point's phase states with chosen amounts, as M1's triple3 probe. The nested search's
     * inner pressure searches cross the binary's vapour-solid to liquid-solid step at one temperature.
     */
    static void triple() {
        var ws = service.newWorkspace();
        double[] pure = feed(1.0, n2, 0.0);
        var sol = service.tp(EquilibriumRequest.tp(216.0, 700000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        var vap = service.tp(EquilibriumRequest.tp(216.6, 400000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        double u0 = 0.05 * vap.internalEnergy() + 0.93 * sol.internalEnergy();
        double v0 = 0.05 * vap.volume() + 0.93 * sol.volume();
        var vls = service.uv(EquilibriumRequest.uv(u0, v0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
        System.out.println("vls " + line(vls));
        double uv = 0, ul = 0, us = 0, vv = 0, vl = 0, vs = 0;
        for (PhaseAmounts phase : vls.phases()) {
            double mu = phase.state().molarInternalEnergy(), mv = phase.state().molarVolume();
            if (phase.kind() == PhaseKind.SOLID) { us = mu; vs = mv; }
            else if (phase.kind() == PhaseKind.LIQUID) { ul = mu; vl = mv; }
            else { uv = mu; vv = mv; }
        }
        double[][] sets = {{0.3, 0.3}, {0.05, 0.02}, {2e-3, 3e-3}, {1e-4, 0.3}, {0.3, 1e-4}, {1e-3, 1e-3}, {1e-5, 1e-2}};
        for (int trace : new int[] {n2, ch4}) {
            for (double[] set : sets) {
                double nv = set[0], nl = set[1], ns = 1.0 - nv - nl;
                double u = nv * uv + nl * ul + ns * us, v = nv * vv + nl * vl + ns * vs;
                for (double f : new double[] {2e-5, 1e-4, 1e-3}) {
                    var r = service.uv(EquilibriumRequest.uv(u, v, basis, feed(1.0, trace, f), PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "TRIPLE %s nv=%.0e nl=%.0e f=%.0e: ", basis.get(trace), nv, nl, f) + line(r));
                }
            }
        }
    }

    /** G5F6's UV round trips: 20 liquids 3 K below their freezing points, and the grid's four vapour-liquid-crystal states. */
    static void grid() {
        var ws = service.newWorkspace();
        double[][] states = {
                {0, .998, 0, .002, 1e6}, {0, .998, 0, .002, 5e6}, {0, .99, 0, .01, 3e6}, {0, .99, 0, .01, 8e6}, {0, .97, 0, .03, 4e6},
                {0, .97, 0, .03, 8e6}, {0, .95, 0, .05, 6e6}, {0, .9, 0, .1, 9e6}, {.05, .945, 0, .005, 3e6}, {.05, .93, 0, .02, 5e6},
                {.1, .88, 0, .02, 7e6}, {0, .891, .099, .01, 2e6}, {0, .855, .095, .05, 4e6}, {0, .5, .45, .05, 2e6}, {0, .3, .6, .1, 2e6},
                {0, 0, .9, .1, 1e6}, {0, 0, .7, .3, 3e6}, {.99, 0, 0, .01, 7e6}, {.95, 0, 0, .05, 9e6}, {0, .1, .5, .4, 5e6}};
        for (double[] s : states) {
            double[] z = {s[0], s[1], s[2], s[3], 0.0};
            var onset = service.freezingTemperature(null, s[4], basis, z, ws);
            var r = service.tp(EquilibriumRequest.tp(onset.temperature() - 3, s[4], basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var uv = service.uv(EquilibriumRequest.uv(r.internalEnergy(), r.volume(), basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            System.out.println(String.format(Locale.ROOT, "G5F6 liquid %s %.0f MPa: ", java.util.Arrays.toString(java.util.Arrays.copyOf(s, 4)), s[4] / 1e6) + line(uv));
        }
        double[][] grid = {{100, 1.0e5}, {120, 1.0e5}, {120, 3.0e5}, {150, 3.0e5}};
        for (double[] g : grid) {
            double[] z = {0.05, 0.8, 0.1, 0.05, 0.0};
            var r = service.tp(EquilibriumRequest.tp(g[0], g[1], basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var uv = service.uv(EquilibriumRequest.uv(r.internalEnergy(), r.volume(), basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            System.out.println(String.format(Locale.ROOT, "G5F6 grid %.0f K %.1f MPa (TP %s): ", g[0], g[1] / 1e6, r.classification()) + line(uv));
        }
    }

    static String line(EquilibriumResult r) {
        StringBuilder s = new StringBuilder(r.status() + " " + r.classification());
        if (r.converged() && !r.phases().isEmpty()) {
            double crystal = 0;
            for (PhaseAmounts phase : r.phases()) if (phase.kind() == PhaseKind.SOLID) crystal += phase.amount(co2);
            s.append(String.format(Locale.ROOT, " T %.9f K P %.4f Pa crystal %.9f mol", r.phases().get(0).temperature(),
                    r.phases().get(0).pressure(), crystal));
        }
        s.append(" TPeq ").append(r.diagnostics().outerIterations()).append(" kernel ").append(r.diagnostics().kernelEvaluations());
        String d = r.detail();
        if (!r.converged()) s.append(" | ").append(d, 0, Math.min(d.length(), 300));
        else if (d.contains("near-pure")) s.append(" | rule");
        return s.toString();
    }
}
