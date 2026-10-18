package com.wormzjl.createcheme.science.thermo.phase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wormzjl.createcheme.science.material.CrystalReference;
import com.wormzjl.createcheme.science.material.JaegerSpanGibbsFunction;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.createcheme.science.thermo.reference.HelmholtzFluid;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.DoubleUnaryOperator;

/**
 * Independent references for the solid CO2 gates (P4 stage 1, batch 2026-09-24-coolprop-low-temperature): the pure
 * CO2 sublimation and melting correlations and the auxiliary solid equations the crystal is checked against, and the
 * pure-component coexistence solve. Holdouts, never model inputs.
 *
 * <ul>
 *   <li>Span and Wagner 1996 (J. Phys. Chem. Ref. Data 25, 1509) melting pressure, equation 3.10: read from the bundled
 *       CoolProp {@code CarbonDioxide.json} {@code ANCILLARIES.melting_line} (a1 1955.539, a2 2055.4593). Its stated
 *       uncertainty, as Jaeger and Span 2012 quote it: 1.5 % to 225 K, 0.5 % from 225 to 270 K.</li>
 *   <li>Span and Wagner 1996 sublimation pressure, equation 3.12: ln(p/p_t) = (T_t/T) [a1 (1 - T/T_t)
 *       + a2 (1 - T/T_t)^1.9 + a3 (1 - T/T_t)^2.9], a1 -14.740846, a2 2.4327015, a3 -5.3061778. Not in the CoolProp file;
 *       transcribed from the published equation (the paper is not held locally) and confirmed numerically: it gives
 *       101 325 Pa at Span and Wagner's normal sublimation temperature 194.6855 K within 3.1e-6 and agrees with
 *       Trusler's equation 48 (below) within 0.12 % from 185 K to T_t and 0.36 % from 150 K. Uncertainty as quoted by
 *       Jaeger and Span: 250 Pa from 185 K to T_t, 100 Pa from 170 to 185 K, 50 Pa below 170 K.</li>
 *   <li>Trusler 2011 (J. Phys. Chem. Ref. Data 40, 043105), section 9: sublimation pressure (48), melting pressure
 *       (47), molar volume on the melting (49) and sublimation (50) curves, fitted to his own solid equation with the
 *       Span-Wagner fluid (48 represents it within 0.04 % from 150 to 216 K, 50 within 0.02 %). A published erratum to
 *       Trusler 2011 may exist; it was not searched for and is unverified.</li>
 *   <li>Fray and Schmitt 2009 (Planet. Space Sci. 57, 2053) CO2 sublimation fits, ln(P/bar) = A0 + sum A_i/T^i, as
 *       reproduced (four significant digits) in Table 2 of the supplementary appendix of Lisse et al. 2020
 *       (arXiv:2009.02277): CO2-1 (14.76, -2571, -7.781e4, 4.325e6, -1.207e8, 1.350e9) and CO2-2 (18.61, -4154,
 *       1.041e5). The reproduction states neither units nor ranges; bar is confirmed by both fits giving 1.01 to
 *       1.02 bar at 194.7 K, and CO2-1 is used below 194.7 K, CO2-2 from 194.7 K to the triple point.</li>
 * </ul>
 */
public final class SolidCarbonDioxideReferences {
    public static final double TRIPLE_TEMPERATURE = 216.592;
    public static final double TRIPLE_PRESSURE = 517_950.0;
    /** Span and Wagner's normal sublimation temperature, K (the pilot property record carries it too). */
    static final double NORMAL_SUBLIMATION_TEMPERATURE = 194.6855;
    /** The P0 target of the sublimation enthalpy at 195 K (Giauque and Egan 1937), J/mol. */
    static final double GIAUQUE_EGAN_SUBLIMATION_ENTHALPY = 25_200.0;

    private static final double[] MELTING;

    static {
        JsonObject melting;
        try (var stream = SolidCarbonDioxideReferences.class.getResourceAsStream("/science/thermo/coolprop/CarbonDioxide.json")) {
            if (stream == null) throw new IllegalStateException("CarbonDioxide.json not on the test classpath");
            melting = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("ANCILLARIES").getAsJsonObject("melting_line");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!melting.get("type").getAsString().equals("polynomial_in_Theta")) throw new IllegalStateException("unexpected melting_line type");
        JsonObject part = melting.getAsJsonArray("parts").get(0).getAsJsonObject();
        var a = part.getAsJsonArray("a");
        var t = part.getAsJsonArray("t");
        if (a.size() != 2 || t.get(0).getAsDouble() != 1 || t.get(1).getAsDouble() != 2) throw new IllegalStateException("unexpected melting_line terms");
        MELTING = new double[] {part.get("T_0").getAsDouble(), part.get("p_0").getAsDouble(), a.get(0).getAsDouble(), a.get(1).getAsDouble(),
                part.get("T_max").getAsDouble()};
    }

    private SolidCarbonDioxideReferences() {}

    /** Span and Wagner 1996 equation 3.12, Pa. */
    public static double spanWagnerSublimationPressure(double t) {
        double theta = 1 - t / TRIPLE_TEMPERATURE;
        return TRIPLE_PRESSURE * Math.exp(TRIPLE_TEMPERATURE / t
                * (-14.740846 * theta + 2.4327015 * Math.pow(theta, 1.9) - 5.3061778 * Math.pow(theta, 2.9)));
    }

    /** The stated uncertainty of equation 3.12, Pa. */
    static double spanWagnerSublimationUncertainty(double t) {
        return t < 170 ? 50 : t < 185 ? 100 : 250;
    }

    /** Span and Wagner 1996 equation 3.10 from the CoolProp file, Pa. */
    public static double spanWagnerMeltingPressure(double t) {
        if (t < MELTING[0] || t > MELTING[4]) throw new IllegalArgumentException("melting line covers " + MELTING[0] + ".." + MELTING[4] + " K");
        double x = t / MELTING[0] - 1;
        return MELTING[1] * (1 + MELTING[2] * x + MELTING[3] * x * x);
    }

    /** The stated relative uncertainty of equation 3.10. */
    public static double spanWagnerMeltingUncertainty(double t) {
        return t <= 225 ? 0.015 : 0.005;
    }

    /** Trusler 2011 equation 48, Pa. */
    static double truslerSublimationPressure(double t) {
        double theta = 1 - t / TRIPLE_TEMPERATURE;
        return TRIPLE_PRESSURE * Math.exp(TRIPLE_TEMPERATURE / t
                * (-14.97366 * theta + 1.805904 * Math.pow(theta, 1.5) - 2.834695 * Math.pow(theta, 2.5)));
    }

    /** Trusler 2011 equation 50: molar volume on the sublimation curve, m3/mol (V00 from Table 6). */
    static double truslerSublimationVolume(double t) {
        double x = t / 68.347;
        double polynomial = 0.00306956 + 0.0243269 * x - 0.00901163 * x * x + 0.00157151 * x * x * x;
        return 25.800e-6 * (1 + polynomial / (Math.exp(68.347 / t) - 1));
    }

    /**
     * d ln V / dT of equation 50 along the sublimation curve, 1/K: alpha - kappa dp_sub/dT, where the kappa term is
     * 1.2 % of alpha at T_t, 0.3 % at 195 K and less below.
     */
    static double truslerSublimationExpansion(double t) {
        double x = t / 68.347;
        double polynomial = 0.00306956 + 0.0243269 * x - 0.00901163 * x * x + 0.00157151 * x * x * x;
        double polynomialT = (0.0243269 - 2 * 0.00901163 * x + 3 * 0.00157151 * x * x) / 68.347;
        double e = Math.exp(68.347 / t);
        double denominator = e - 1;
        double denominatorT = -e * 68.347 / (t * t);
        double ratioT = (polynomialT * denominator - polynomial * denominatorT) / (denominator * denominator);
        return 25.800e-6 * ratioT / truslerSublimationVolume(t);
    }

    /** Fray and Schmitt 2009 as reproduced by Lisse et al. 2020, Pa. */
    static double fraySchmittSublimationPressure(double t) {
        double lnBar;
        if (t < 194.7) {
            lnBar = 14.76 - 2571 / t - 7.781e4 / (t * t) + 4.325e6 / Math.pow(t, 3) - 1.207e8 / Math.pow(t, 4) + 1.350e9 / Math.pow(t, 5);
        } else {
            lnBar = 18.61 - 4154 / t + 1.041e5 / (t * t);
        }
        return 1e5 * Math.exp(lnBar);
    }

    /** The crystal record anchored to the Span-Wagner oracle's liquid at the record's triple point: {g0, g1}. */
    static double[] oracleConstants(CrystalReference crystal, HelmholtzFluid co2) {
        var liquid = co2.stateAtPressure(crystal.anchor().temperature(), crystal.anchor().pressure(), HelmholtzFluid.Root.LIQUID);
        return crystal.integrationConstants(liquid.gibbsEnergy(), liquid.enthalpy());
    }

    /** Solid-oracle coexistence pressure at T: sublimation on the vapour root, melting on the liquid root, Pa. */
    static double oracleCoexistence(JaegerSpanGibbsFunction js, double[] constants, HelmholtzFluid co2, double t,
                                    HelmholtzFluid.Root root, double start) {
        return coexistencePressure(start, p -> js.evaluate(t, p, constants[0], constants[1]).gibbs(),
                p -> js.evaluate(t, p, constants[0], constants[1]).volume(), p -> co2.stateAtPressure(t, p, root).gibbsEnergy(),
                p -> 1 / co2.density(t, p, root));
    }

    /** Crystal-fluid coexistence pressure of the pure species on a fluid family at T, Pa. */
    static double familyCoexistence(CubicPhaseEvaluator fluid, CrystalPhaseEvaluator solid, SolidPhaseEvaluator.TriplePointAnchor anchor,
                                    double t, Root root, double start) {
        int i = fluid.components().indexOf(solid.component());
        double[] pure = new double[fluid.componentCount()];
        pure[i] = 1;
        var workspace = fluid.newWorkspace();
        var state = fluid.newState();
        var crystal = new PhaseState(1);
        return coexistencePressure(start, p -> solid.evaluate(t, p, anchor, crystal).molarGibbsEnergy(),
                p -> solid.evaluate(t, p, anchor, crystal).molarVolume(),
                p -> fluid.evaluate(t, p, pure, root, workspace, state).chemicalPotential(i),
                p -> fluid.evaluate(t, p, pure, root, workspace, state).molarVolume());
    }

    /** Span and Wagner's melting temperature at pressure p (inverse of equation 3.10 by bisection), K. */
    static double spanWagnerMeltingTemperature(double p) {
        double lo = TRIPLE_TEMPERATURE, hi = MELTING[4];
        for (int i = 0; i < 200 && hi - lo > 1e-12; i++) {
            double mid = 0.5 * (lo + hi);
            if (spanWagnerMeltingPressure(mid) > p) hi = mid; else lo = mid;
        }
        return 0.5 * (lo + hi);
    }

    /**
     * d ln p / dT of equation 3.12, 1/K (to express a pressure error as a temperature error): a central difference,
     * backward within 1e-4 K of the triple point where the equation ends.
     */
    static double spanWagnerSublimationSlope(double t) {
        double h = 1e-4;
        if (t + h > TRIPLE_TEMPERATURE) return (Math.log(spanWagnerSublimationPressure(t)) - Math.log(spanWagnerSublimationPressure(t - h))) / h;
        return (Math.log(spanWagnerSublimationPressure(t + h)) - Math.log(spanWagnerSublimationPressure(t - h))) / (2 * h);
    }

    /**
     * The pressure where a pure solid and a pure fluid of the same species have equal chemical potential at fixed T:
     * Newton on ln p for {@code solid(p) - fluid(p) = 0} with the derivative {@code p (v_s - v_f)}. The callers pass
     * g(p) and v(p) of each phase; converged when the chemical-potential difference is below 1e-9 J/mol or the step
     * changes the pressure by less than 1e-10 of itself. The second stop is the one the high melting pressures reach:
     * at 270 K and 315 MPa the Span-Wagner liquid's Gibbs energy is resolved to about 1e-8 J/mol only, and the Newton
     * on it 2-cycles at +-7e-9 J/mol (ln p steps of 5e-12) instead of entering 1e-9 J/mol.
     */
    static double coexistencePressure(double start, DoubleUnaryOperator solidGibbs, DoubleUnaryOperator solidVolume,
                                      DoubleUnaryOperator fluidGibbs, DoubleUnaryOperator fluidVolume) {
        double lnP = Math.log(start);
        for (int iteration = 0; iteration < 100; iteration++) {
            double p = Math.exp(lnP);
            double f = solidGibbs.applyAsDouble(p) - fluidGibbs.applyAsDouble(p);
            if (Math.abs(f) < 1e-9) return p;
            double derivative = p * (solidVolume.applyAsDouble(p) - fluidVolume.applyAsDouble(p));
            double step = -f / derivative;
            step = Math.max(-1.0, Math.min(1.0, step));
            lnP += step;
            if (Math.abs(step) < 1e-10) return Math.exp(lnP);
        }
        throw new IllegalStateException("coexistence pressure did not converge from " + start + " Pa");
    }
}
