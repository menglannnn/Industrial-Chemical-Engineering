import com.wormzjl.createcheme.science.fluid.thermo.SpinePackageData;
import com.wormzjl.createcheme.science.fluid.thermo.TranslatedPengRobinson;
import com.wormzjl.createcheme.science.fluid.thermo.WaterRegion1;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.material.ViscosityCorrelation;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * Range-extrapolation probe of batch 2026-09-24-coolprop-low-temperature (RANGE_EXTRAPOLATION_PLAN.md): what the
 * property models give outside their declared ranges, with no code change. Read-only; prints one block per model.
 */
public class ExtrapolationProbe {
    static final double M_WATER = 18.015268e-3;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        if97();
        pengRobinson();
        viscosityTables();
        steamAndSaturation();
    }

    /** IF97 Region 1 Gibbs sums below the triple point (WaterRegion1.fill, no range check), against a hold at 273.16 K. */
    static void if97() throws Exception {
        Method fill = WaterRegion1.class.getDeclaredMethod("fill", WaterRegion1.Workspace.class, double.class, double.class);
        fill.setAccessible(true);
        double[] temperatures = {273.16, 265, 260, 250, 240, 230, 220, 210, 200, 190, 180, 170, 160, 150, 140, 130, 120, 110, 100};
        for (double p : new double[]{1e5, 1e6, 1e7}) {
            var ref = new WaterRegion1.Workspace();
            fill.invoke(null, ref, 273.16, p);
            var r = ref.state();
            for (double t : temperatures) {
                var w = new WaterRegion1.Workspace();
                try {
                    fill.invoke(null, w, t, p);
                    var s = w.state();
                    double hold = r.specificEnthalpy() + r.specificHeatCapacity() * (t - 273.16);
                    System.out.printf("IF97 T=%.2f P=%.0e rho=%.2f kg/m3 cp=%.2f J/molK h-h(Tt)=%.0f J/mol held=%.0f J/mol dvdP=%.3e alpha=%.3e /K%n",
                            t, p, 1 / s.specificVolume(), s.specificHeatCapacity() * M_WATER,
                            (s.specificEnthalpy() - r.specificEnthalpy()) * M_WATER, (hold - r.specificEnthalpy()) * M_WATER,
                            s.volumePressureDerivative(), s.volumeTemperatureDerivative() / s.specificVolume());
                } catch (InvocationTargetException e) {
                    System.out.printf("IF97 T=%.2f P=%.0e REFUSED %s%n", t, p, e.getCause().getMessage());
                }
            }
        }
    }

    /** The pilot package PR78 (residual part, translated) for each pure component and an LNG below triple points, and methane past the alpha zero. */
    static void pengRobinson() {
        var data = SpinePackageData.of(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
        var components = data.components();
        var pr = TranslatedPengRobinson.residualOnly(components, data.interactions(), data.translations());
        int n = components.size();
        for (int i = 0; i < n; i++) {
            var c = components.get(i);
            for (double t : new double[]{120, 100, 90.69, 80, 70, 60, 50, 40, 30}) {
                double[] x = new double[n];
                x[i] = 1;
                liquid(pr, c.id(), t, 1e5, x, c.molarMassKilogramPerMol());
            }
        }
        double[] lng = new double[n];
        double mass = 0;
        for (int i = 0; i < n; i++) {
            String id = components.get(i).id().toLowerCase(Locale.ROOT);
            lng[i] = id.contains("methane") ? 0.90 : id.contains("ethane") ? 0.05 : id.contains("nitrogen") ? 0.0495 : 0.0005;
            mass += lng[i] * components.get(i).molarMassKilogramPerMol();
        }
        for (double t : new double[]{111.6, 90, 80, 70, 60}) liquid(pr, "LNG(0.90 CH4,0.05 C2H6,0.0495 N2,0.0005 CO2)", t, 1e5, lng, mass);
        for (int i = 0; i < n; i++) {
            if (!components.get(i).id().toLowerCase(Locale.ROOT).contains("methane")) continue;
            double[] x = new double[n];
            x[i] = 1;
            for (double t : new double[]{1200, 2000, 2401, 2500, 3000, 5000}) {
                try {
                    var ph = pr.evaluate(t, 1e6, x, PengRobinsonKernel.Root.VAPOR);
                    System.out.printf("PR methane vapour T=%.0f P=1e6 Z=%.6f hR+Pc=%.2f J/mol cpR=%.4f J/molK%n",
                            t, 1e6 * ph.molarVolume() / (8.314462618 * t), ph.molarEnthalpy(), ph.heatCapacity());
                } catch (RuntimeException e) {
                    System.out.printf("PR methane vapour T=%.0f REFUSED %s%n", t, e.getMessage());
                }
            }
        }
    }

    static void liquid(TranslatedPengRobinson pr, String name, double t, double p, double[] x, double molarMass) {
        try {
            var ph = pr.evaluate(t, p, x, PengRobinsonKernel.Root.LIQUID);
            System.out.printf("PR %s T=%.2f P=%.0e liquid rho=%.1f kg/m3 v=%.3f cm3/mol hR+Pc=%.0f J/mol cpR=%.2f J/molK kappaT=%.3e /Pa%n",
                    name, t, p, molarMass / ph.molarVolume(), ph.molarVolume() * 1e6, ph.molarEnthalpy(), ph.heatCapacity(),
                    ph.isothermalCompressibility());
        } catch (RuntimeException e) {
            System.out.printf("PR %s T=%.2f REFUSED %s%n", name, t, e.getMessage());
        }
    }

    /** Each log_table continued past both ends: liquid ln(mu) linear in 1/T, vapour ln(mu) linear in ln T, matched to the end segment slope. */
    static void viscosityTables() {
        var catalog = MaterialCatalog.bundled();
        for (String pkg : List.of("createcheme:pilot_cryogenic", "createcheme:tjl20_methane_nitrogen")) {
            for (String component : catalog.requirePackage(pkg).components()) {
                for (var phase : ViscosityCorrelation.Phase.values()) {
                    var record = catalog.viscosity(pkg, component, phase);
                    if (record.isEmpty() || record.get().model() != ViscosityCorrelation.Model.LOG_TABLE) continue;
                    var v = record.get();
                    var nodes = v.temperaturesKelvin();
                    var mu = v.coefficients();
                    int last = nodes.size() - 1;
                    double tLow = nodes.get(0), tHigh = nodes.get(last);
                    double slopeLow = (Math.log(mu.get(1)) - Math.log(mu.get(0))) / (nodes.get(1) - tLow);
                    double slopeHigh = (Math.log(mu.get(last)) - Math.log(mu.get(last - 1))) / (tHigh - nodes.get(last - 1));
                    var line = new StringBuilder(String.format("VISC %s %s %s table %.3f..%.3f K mu(min)=%.4e mu(max)=%.4e",
                            pkg.substring(12), component.replace("createcheme:", ""), phase, tLow, tHigh, mu.get(0), mu.get(last)));
                    for (double t : phase == ViscosityCorrelation.Phase.LIQUID ? new double[]{tLow - 30, tLow - 10, tHigh + 20}
                            : new double[]{tLow * 0.4, tLow * 0.7, tHigh * 1.5, tHigh * 2.5}) {
                        boolean below = t < tLow;
                        double tEnd = below ? tLow : tHigh, s = below ? slopeLow : slopeHigh, muEnd = below ? mu.get(0) : mu.get(last);
                        double value = phase == ViscosityCorrelation.Phase.LIQUID
                                ? muEnd * Math.exp(-s * tEnd * tEnd * (1 / t - 1 / tEnd))
                                : muEnd * Math.exp(s * tEnd * Math.log(t / tEnd));
                        line.append(String.format(" | %.1fK %.4e", t, value));
                    }
                    line.append(String.format(" | endExp low=%.3f high=%.3f", phase == ViscosityCorrelation.Phase.LIQUID ? -slopeLow * tLow * tLow : slopeLow * tLow,
                            phase == ViscosityCorrelation.Phase.LIQUID ? -slopeHigh * tHigh * tHigh : slopeHigh * tHigh));
                    System.out.println(line);
                }
            }
        }
        var water = catalog.viscosity("createcheme:tjl20_methane_nitrogen", "Water", ViscosityCorrelation.Phase.LIQUID);
        water.ifPresent(v -> {
            var line = new StringBuilder("VISC water liquid " + v.model() + " " + v.minimumTemperatureKelvin() + ".." + v.maximumTemperatureKelvin());
            for (double t : new double[]{273.16, 253.15, 240, 230, 220, 200, 150}) {
                double total = 0;
                for (int i = 0; i < v.coefficients().size(); i++)
                    total += v.coefficients().get(i) * Math.pow(t / v.referenceTemperatureKelvin(), v.exponents().get(i));
                line.append(String.format(" | %.2fK %.4e", t, total));
            }
            System.out.println(line);
        });
    }

    /** The dilute-steam viscosity formula, Wagner-Pruss continued below 273.16 K, ice Ih sublimation and melting (IAPWS R14-08). */
    static void steamAndSaturation() {
        for (double t : new double[]{273.16, 250, 200, 160, 140, 135, 134, 120}) {
            double r = t / 647.096;
            double mu = 1e-4 * Math.sqrt(r) / (1.67752 + 2.20462 / r + .6366564 / (r * r) - .241605 / (r * r * r));
            System.out.printf("STEAM mu T=%.2f %.4e Pa s%n", t, mu);
        }
        double tc = 647.096, pc = 22.064e6, tt = 273.16, pt = 611.657;
        double[] a = {-7.85951783, 1.84408259, -11.7866497, 22.6807411, -15.9618719, 1.80122502};
        double[] sa = {-0.212144006e2, 0.273203819e2, -0.610598130e1}, sb = {0.333333333e-2, 0.120666667e1, 0.170333333e1};
        for (double t : new double[]{273.16, 263.15, 253.15, 233.15, 200, 150, 111.6, 100}) {
            double tau = 1 - t / tc;
            double wp = pc * Math.exp(tc / t * (a[0] * tau + a[1] * Math.pow(tau, 1.5) + a[2] * Math.pow(tau, 3) + a[3] * Math.pow(tau, 3.5)
                    + a[4] * Math.pow(tau, 4) + a[5] * Math.pow(tau, 7.5)));
            double theta = t / tt, sum = 0;
            for (int i = 0; i < 3; i++) sum += sa[i] * Math.pow(theta, sb[i]);
            double sub = pt * Math.exp(sum / theta);
            System.out.printf("SAT T=%.2f wagnerPruss=%.4e Pa iceSublimation=%.4e Pa ratio=%.4f%n", t, wp, sub, wp / sub);
        }
        double[] ma = {0.119539337e7, 0.808183159e5, 0.333826860e4}, mb = {0.300000e1, 0.257500e2, 0.103750e3};
        for (double t : new double[]{273.16, 273.0, 272.5, 271.0, 268.0, 263.0, 251.165}) {
            double theta = t / tt, sum = 1;
            for (int i = 0; i < 3; i++) sum += ma[i] * (1 - Math.pow(theta, mb[i]));
            double pm = pt * sum;
            System.out.printf("MELT T=%.3f p_melt=%.4e Pa clapeyronLinear=%.4e Pa%n", t, pm, pt + (t - tt) / -7.43e-8);
        }
    }
}
