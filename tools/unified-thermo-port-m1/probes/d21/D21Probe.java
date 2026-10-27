import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.createcheme.science.thermo.phase.FluidTpEquilibrium;
import com.wormzjl.createcheme.science.thermo.phase.PhaseAmounts;
import com.wormzjl.createcheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.createcheme.science.thermo.phase.PhaseKind;
import java.util.List;
import java.util.Locale;

/** M1 probe (D21): the row A vessel's UV, a PH across the sublimation line, and a PH into deep deposition, at trace fractions f. */
public final class D21Probe {
    public static void main(String[] args) {
        var service = FluidTpEquilibrium.forPackage(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
        List<String> basis = service.contract().components();
        int n2 = basis.indexOf("Nitrogen"), co2 = basis.indexOf("CarbonDioxide");
        var ws = service.newWorkspace();
        String mode = args.length > 0 ? args[0] : "all";
        if (mode.equals("all") || mode.equals("uv")) {
            double u = -9.972099514183599e7, v = 1.0, co2Amount = 249.1435;
            for (String trace : new String[] {"Nitrogen", "Methane"}) {
                int k = basis.indexOf(trace);
                for (double f : new double[] {0, 1e-12, 1e-9, 1e-8, 3e-8, 1e-7, 3e-7, 6e-7, 1e-6, 2e-6, 1e-5}) {
                    double[] z = new double[basis.size()];
                    z[co2] = co2Amount;
                    z[k] = f / (1 - f) * co2Amount;
                    var r = service.uv(EquilibriumRequest.uv(u, v, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "UV %s f=%.0e: ", trace, f) + line(r, co2));
                }
            }
        }
        double p = 101325.0;
        if (mode.equals("all") || mode.equals("ph")) {
            for (double f : new double[] {0, 1e-12, 1e-9, 1e-7, 6e-7, 1e-6, 2e-6}) {
                double[] z = new double[basis.size()];
                z[co2] = 1.0;
                z[n2] = f / (1 - f);
                double h0 = service.tp(EquilibriumRequest.tp(200.0, p, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws).enthalpy();
                for (double dh : new double[] {-1000, -5000, -12000, -20000, -24000, -25000}) {
                    var r = service.ph(EquilibriumRequest.ph(p, h0 + dh, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "PH N2 f=%.0e dh=%.0f: ", f, dh) + line(r, co2));
                }
            }
        }
        if (mode.equals("triple")) {
            double[] pure = new double[basis.size()];
            pure[co2] = 1.0;
            var vap = service.tp(EquilibriumRequest.tp(216.6, 400000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var liq = service.tp(EquilibriumRequest.tp(216.6, 700000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var sol = service.tp(EquilibriumRequest.tp(216.0, 700000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            System.out.println("vap " + line(vap, co2) + "\nliq " + line(liq, co2) + "\nsol " + line(sol, co2));
            double u0 = 0.3 * vap.internalEnergy() + 0.3 * liq.internalEnergy() + 0.4 * sol.internalEnergy();
            double v0 = 0.3 * vap.volume() + 0.3 * liq.volume() + 0.4 * sol.volume();
            var guess = service.uv(EquilibriumRequest.uv(u0, v0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            System.out.println("guess " + line(guess, co2));
            double uv = 0, ul = 0, us = 0, vv = 0, vl = 0, vs = 0;
            for (PhaseAmounts phase : guess.phases()) {
                double mu = phase.state().molarInternalEnergy(), mv = phase.state().molarVolume();
                if (phase.kind() == PhaseKind.SOLID) { us = mu; vs = mv; }
                else if (phase.kind() == PhaseKind.LIQUID) { ul = mu; vl = mv; }
                else { uv = mu; vv = mv; }
            }
            double[][] sets = System.getProperty("m1.one") != null ? new double[][] {{1e-3, 1e-3}} : new double[][] {{0.3, 0.3}, {1e-3, 0.5}, {1e-4, 0.5}, {1e-5, 0.5}, {1e-7, 0.5}, {0.3, 1e-5}, {1e-5, 1e-5}, {1e-3, 1e-3}};
            for (double[] set : sets) {
                double nv = set[0], nl = set[1], ns = 1.0 - nv - nl;
                double u = nv * uv + nl * ul + ns * us, v = nv * vv + nl * vl + ns * vs;
                for (double f : System.getProperty("m1.one") != null ? new double[] {1e-6} : new double[] {0, 1e-9, 1e-7, 1e-6}) {
                    double[] z = new double[basis.size()];
                    z[co2] = 1.0;
                    z[n2] = f / (1 - f);
                    var r = service.uv(EquilibriumRequest.uv(u, v, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "TRIPLE nv=%.0e nl=%.0e f=%.0e: ", nv, nl, f) + line(r, co2));
                }
            }
        }
        if (mode.equals("triple3")) {
            // The triple point's own states from a pure VLS answer, then targets with chosen amounts.
            double[] pure = new double[basis.size()];
            pure[co2] = 1.0;
            var sol = service.tp(EquilibriumRequest.tp(216.0, 700000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var vap = service.tp(EquilibriumRequest.tp(216.6, 400000.0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            double u0 = 0.05 * vap.internalEnergy() + 0.93 * sol.internalEnergy();
            double v0 = 0.05 * vap.volume() + 0.93 * sol.volume();
            var vls = service.uv(EquilibriumRequest.uv(u0, v0, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            System.out.println("vls " + line(vls, co2));
            double uv = 0, ul = 0, us = 0, vv = 0, vl = 0, vs = 0;
            for (PhaseAmounts phase : vls.phases()) {
                double mu = phase.state().molarInternalEnergy(), mv = phase.state().molarVolume();
                if (phase.kind() == PhaseKind.SOLID) { us = mu; vs = mv; }
                else if (phase.kind() == PhaseKind.LIQUID) { ul = mu; vl = mv; }
                else { uv = mu; vv = mv; }
                System.out.println("  " + phase.kind() + " u " + mu + " v " + mv);
            }
            double[][] sets = {{0.3, 0.3}, {0.05, 0.02}, {2e-3, 3e-3}, {3e-3, 2e-3}, {1e-4, 0.3}, {0.3, 1e-4}, {1e-3, 1e-3}, {1e-5, 1e-2}};
            for (double[] set : sets) {
                double nv = set[0], nl = set[1], ns = 1.0 - nv - nl;
                double u = nv * uv + nl * ul + ns * us, v = nv * vv + nl * vl + ns * vs;
                for (double f : new double[] {0, 1e-9, 1e-7, 1e-6}) {
                    double[] z = new double[basis.size()];
                    z[co2] = 1.0;
                    z[n2] = f / (1 - f);
                    var r = service.uv(EquilibriumRequest.uv(u, v, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "TRIPLE3 nv=%.0e nl=%.0e f=%.0e: ", nv, nl, f) + line(r, co2));
                }
            }
        }
        if (mode.equals("above")) {
            // PH at 101.325 kPa above theta: small crystals (the general path's resolution).
            for (double f : new double[] {2e-6, 5e-6, 1e-5, 3e-5, 1e-4, 1e-3}) {
                double[] z = new double[basis.size()];
                z[co2] = 1.0;
                z[n2] = f / (1 - f);
                double h0 = service.tp(EquilibriumRequest.tp(200.0, p, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws).enthalpy();
                for (double dh : new double[] {-150, -200, -300, -500, -1000, -2000, -3000, -5000, -12000}) {
                    var r = service.ph(EquilibriumRequest.ph(p, h0 + dh, basis, z, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "ABOVE PH N2 f=%.0e dh=%.0f: ", f, dh) + line(r, co2));
                }
            }
        }
        if (mode.equals("all") || mode.equals("deep")) {
            // Deep deposition at 101.325 kPa: the vapour share beta from 1e-2 to below f, from the pure step's two ends.
            double[] pure = new double[basis.size()];
            pure[co2] = 1.0;
            var solidEnd = service.tp(EquilibriumRequest.tp(190.0, p, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var vapourEnd = service.tp(EquilibriumRequest.tp(200.0, p, basis, pure, PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            var mid = service.ph(EquilibriumRequest.ph(p, 0.5 * (solidEnd.enthalpy() + vapourEnd.enthalpy()), basis, pure,
                    PhaseCompetition.FLUID_AND_CRYSTALS), ws);
            double hv = 0, hs = 0;
            for (PhaseAmounts phase : mid.phases()) {
                if (phase.kind() == PhaseKind.SOLID) hs = phase.state().molarEnthalpy(); else hv = phase.state().molarEnthalpy();
            }
            for (double f : new double[] {1e-9, 1e-7, 1e-6}) {
                double[] z = new double[basis.size()];
                z[co2] = 1.0;
                z[n2] = f / (1 - f);
                for (double beta : new double[] {1e-2, 1e-3, 3e-4, 1.2e-4, 1e-4, 8e-5, 3e-5, 1e-5, 3e-6, 1e-6, 3e-7, 1e-7, 1e-8}) {
                    var r = service.ph(EquilibriumRequest.ph(p, (1 + f / (1 - f)) * (hs + beta * (hv - hs)), basis, z,
                            PhaseCompetition.FLUID_AND_CRYSTALS), ws);
                    System.out.println(String.format(Locale.ROOT, "DEEP N2 f=%.0e beta=%.1e: ", f, beta) + line(r, co2));
                }
            }
        }
    }

    static String line(EquilibriumResult r, int co2) {
        StringBuilder s = new StringBuilder(r.status() + " " + r.classification());
        if (r.converged() && !r.phases().isEmpty()) {
            double crystal = 0;
            for (PhaseAmounts phase : r.phases()) if (phase.kind() == PhaseKind.SOLID) crystal += phase.amount(co2);
            s.append(String.format(Locale.ROOT, " T %.9f K P %.4f Pa crystal %.9f mol", r.phases().get(0).temperature(),
                    r.phases().get(0).pressure(), crystal));
        }
        s.append(" outer ").append(r.diagnostics().outerIterations()).append(" kernel ").append(r.diagnostics().kernelEvaluations());
        String d = r.detail();
        int at = d.indexOf("near-pure");
        if (!r.converged()) s.append(" | ").append(d, 0, Math.min(d.length(), 420));
        else if (at >= 0) s.append(" | rule");
        return s.toString();
    }
}
