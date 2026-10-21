package com.wormzjl.icheme.science.material;

import com.wormzjl.icheme.science.math.DeterministicMath;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * The Gibbs energy equation of solid carbon dioxide (phase I, dry ice) of Jäger and Span 2012 (J. Chem. Eng. Data 57,
 * 590, equation 8 with equations 9, 10, 21 and 22 and the constants of Table 1), the {@code gibbs_function} model kind
 * of a {@code crystals} record (batch 2026-09-24-coolprop-low-temperature, P4):
 *
 * <pre>
 * g / (R T0) = g0 + g1 dT + g2 dT^2
 *            + g3 { ln[(t^2 + g4^2)/(1 + g4^2)] - (2 t/g4) [atan(t/g4) - atan(1/g4)] }
 *            + g5 { ln[(t^2 + g6^2)/(1 + g6^2)] - (2 t/g6) [atan(t/g6) - atan(1/g6)] }
 *            + g7 dP [exp(f_a(t)) + K(t) g8] + g9 K(t) [(p + g10)^((n-1)/n) - (1 + g10)^((n-1)/n)]
 * </pre>
 *
 * with {@code t = T/T0}, {@code dT = t - 1}, {@code p = P/P0}, {@code dP = p - 1}, the integral of the expansion
 * coefficient {@code f_a(t)} of equation 21 and {@code K(t) = gk0 t^2 + gk1 t + gk2} of equation 22. The derivatives
 * are the analytic ones of Tables 3 and 4 and the properties those of Table 2.
 *
 * <p>{@code g0} and {@code g1} are integration constants (the paper's equations 19 and 20 fix them at the triple point
 * against the fluid equation). They are not part of this object: every evaluation takes them as arguments, and
 * {@link #integrationConstants} solves them from a Gibbs energy and an entropy at one state, which is how a crystal is
 * anchored to whichever fluid family evaluates its triple point. The record's published values are kept only for the
 * check against the paper's Table 5.</p>
 *
 * <p>Validity (paper, "Range of Validity"): 80 to 300 K and 0 to 500 MPa, as declared by the record: the data range a
 * notice reports. Past it {@link #evaluate} continues with the end's heat capacity, {@code dv/dT} and {@code dv/dP}.</p>
 */
public final class JaegerSpanGibbsFunction {
    public static final String TYPE = "jaeger_span_2012";

    /**
     * g and its first and second derivatives at one state, SI molar units (J/mol, K, Pa, m3/mol), with the properties
     * of the paper's Table 2.
     */
    public record State(double temperature, double pressure, double gibbs, double dT, double dP, double dTT,
                        double dTP, double dPP) {
        /** v = dg/dP, m3/mol (equation 11). */
        public double volume() { return dP; }
        /** s = -dg/dT, J/(mol K) (equation 12). */
        public double entropy() { return -dT; }
        /** h = g - T dg/dT, J/mol (equation 13). */
        public double enthalpy() { return gibbs - temperature * dT; }
        /** u = g - T dg/dT - P dg/dP, J/mol (equation 14). */
        public double internalEnergy() { return gibbs - temperature * dT - pressure * dP; }
        /** f = g - P dg/dP, J/mol (equation 15). */
        public double helmholtzEnergy() { return gibbs - pressure * dP; }
        /** c_p = -T d2g/dT2, J/(mol K) (equation 16). */
        public double heatCapacity() { return -temperature * dTT; }
        /** alpha = (d2g/dT dP) / (dg/dP), 1/K (equation 17). */
        public double expansion() { return dTP / dP; }
        /** kappa = -(d2g/dP2) / (dg/dP), 1/Pa (equation 18). */
        public double compressibility() { return -dPP / dP; }
    }

    private final double referenceTemperature;
    private final double referencePressure;
    private final double referenceVolume;
    private final double gasConstant;
    private final double n;
    /** g2..g10 at indexes 2..10 (0 and 1 unused: they are the integration constants). */
    private final double[] g;
    private final double[] alpha;
    private final double[] kappa;
    private final double publishedG0;
    private final double publishedG1;
    private final double minimumTemperature, maximumTemperature, minimumPressure, maximumPressure;
    // Constants of the expressions, fixed by the coefficients.
    private final double exponent;
    private final double offsetPower;
    private final double arctan4, arctan6, log4, log6;
    private final double alphaLog1, alphaLog4, alphaArctan5, alphaArctan8;
    private final Object science;

    private JaegerSpanGibbsFunction(JsonObject o) {
        String type = JsonFields.text(o, "type");
        if (!type.equals(TYPE)) throw new IllegalArgumentException("gibbs_function.type: expected " + TYPE + ", got " + type);
        JsonFields.text(o, "source");
        referenceTemperature = JsonFields.positive(o, "reference_temperature_kelvin");
        referencePressure = JsonFields.positive(o, "reference_pressure_pascal");
        referenceVolume = JsonFields.positive(o, "reference_molar_volume_m3_per_mol");
        gasConstant = JsonFields.positive(o, "gas_constant_j_per_mol_kelvin");
        n = JsonFields.number(o, "n");
        if (!(n > 1) || !Double.isFinite(n)) throw new IllegalArgumentException("gibbs_function.n: expected a finite value above 1");
        double[] tail = ReferenceSpine.doubles(o, "g2_to_g10", 9, 9);
        g = new double[11];
        System.arraycopy(tail, 0, g, 2, 9);
        alpha = ReferenceSpine.doubles(o, "g_alpha_0_to_8", 9, 9);
        kappa = ReferenceSpine.doubles(o, "g_kappa_0_to_2", 3, 3);
        publishedG0 = JsonFields.number(o, "published_g0");
        publishedG1 = JsonFields.number(o, "published_g1");
        minimumTemperature = JsonFields.positive(o, "temperature_min_kelvin");
        maximumTemperature = JsonFields.positive(o, "temperature_max_kelvin");
        minimumPressure = JsonFields.number(o, "pressure_min_pascal");
        maximumPressure = JsonFields.positive(o, "pressure_max_pascal");
        if (!(maximumTemperature > minimumTemperature) || !(minimumPressure >= 0) || !(maximumPressure > minimumPressure)
                || !Double.isFinite(maximumTemperature) || !Double.isFinite(maximumPressure))
            throw new IllegalArgumentException("gibbs_function: invalid temperature or pressure range");
        for (double value : g) if (!Double.isFinite(value)) throw new IllegalArgumentException("gibbs_function.g2_to_g10: not finite");
        if (g[4] == 0 || g[6] == 0 || alpha[7] == 0) throw new IllegalArgumentException("gibbs_function: g4, g6 and g_alpha_7 must be nonzero");
        if (!(g[10] > 0)) throw new IllegalArgumentException("gibbs_function: g10 must be positive (the pressure term needs p + g10 > 0)");

        exponent = (n - 1) / n;
        offsetPower = DeterministicMath.pow(1 + g[10], exponent);
        arctan4 = DeterministicMath.atan(1 / g[4]);
        arctan6 = DeterministicMath.atan(1 / g[6]);
        log4 = 1 + g[4] * g[4];
        log6 = 1 + g[6] * g[6];
        alphaLog1 = 1 - alpha[2] + alpha[3];
        alphaLog4 = 1 + alpha[2] + alpha[3];
        alphaArctan5 = DeterministicMath.atan((1 - alpha[6]) / alpha[7]);
        alphaArctan8 = DeterministicMath.atan((1 + alpha[6]) / alpha[7]);
        if (!(alphaLog1 > 0) || !(alphaLog4 > 0)) throw new IllegalArgumentException("gibbs_function: f_alpha logarithms undefined at T0");

        // The function must give a positive volume and heat capacity over its declared range.
        double pLow = Math.max(minimumPressure, Math.min(referencePressure, maximumPressure));
        for (double t : new double[] {minimumTemperature, 0.5 * (minimumTemperature + maximumTemperature), maximumTemperature}) {
            for (double p : new double[] {pLow, maximumPressure}) {
                State s = evaluate(t, p, 0, 0);
                if (!(s.volume() > 0) || !(s.heatCapacity() > 0) || !(s.compressibility() > 0))
                    throw new IllegalArgumentException("gibbs_function: volume, heat capacity or compressibility not positive at "
                            + t + " K, " + p + " Pa");
            }
        }

        var numbers = new ArrayList<Object>();
        numbers.add(TYPE);
        for (double v : new double[] {referenceTemperature, referencePressure, referenceVolume, gasConstant, n}) numbers.add(v);
        numbers.add(list(tail));
        numbers.add(list(alpha));
        numbers.add(list(kappa));
        numbers.add(List.of(publishedG0, publishedG1));
        numbers.add(List.of(minimumTemperature, maximumTemperature, minimumPressure, maximumPressure));
        science = List.copyOf(numbers);
    }

    static JaegerSpanGibbsFunction read(JsonObject o) { return new JaegerSpanGibbsFunction(o); }

    private static List<Double> list(double[] values) {
        var list = new ArrayList<Double>();
        for (double v : values) list.add(v);
        return list;
    }

    public double minimumTemperature() { return minimumTemperature; }
    public double maximumTemperature() { return maximumTemperature; }
    public double minimumPressure() { return minimumPressure; }
    public double maximumPressure() { return maximumPressure; }
    /** The paper's reference state T0, K. */
    public double referenceTemperature() { return referenceTemperature; }
    /** The paper's v0 (equation 10), m3/mol; not an input of equation 8, carried for the check v(T0, p0) = v0. */
    public double referenceVolume() { return referenceVolume; }
    /** The model's own gas constant, J/(mol K) (8.314472 in the paper); part of the fitted function. */
    public double gasConstant() { return gasConstant; }
    /** The paper's g0 (Table 1), dimensionless, on the reference state of Span and Wagner 1996; for the check only. */
    public double publishedG0() { return publishedG0; }
    /** The paper's g1 (Table 1), dimensionless; for the check only. */
    public double publishedG1() { return publishedG1; }
    /** Numeric content for the package spine fingerprint. */
    Object science() { return science; }

    /** Whether (T, P) lies in the declared range (pressure strictly positive). */
    public boolean covers(double temperatureKelvin, double pressurePascal) {
        return temperatureKelvin >= minimumTemperature && temperatureKelvin <= maximumTemperature
                && pressurePascal >= minimumPressure && pressurePascal > 0 && pressurePascal <= maximumPressure;
    }

    /**
     * g and its derivatives at (T, P) with the integration constants {@code g0} and {@code g1}: the function inside its
     * declared range, continued past it (owner rule of 2026-10-16) with the heat capacity, {@code dv/dT} and
     * {@code dv/dP} held at the nearer end. Above the pressure range {@code g} is quadratic in P with the end's
     * volume and {@code dv/dP}; past a temperature end it continues with the end's {@code c_p}
     * ({@code s = s_e + c_p ln(T/T_e)}) and the volume linear in T with the end's {@code dv/dT}. Value and first
     * derivatives are continuous at the ends; the pressure derivative of the held heat capacity is dropped, a
     * second-order term in the distance from the range (a solid's {@code c_p} barely depends on P).
     *
     * @throws IllegalArgumentException for a temperature or pressure that is not positive and finite, or a non-finite
     *         integration constant
     */
    public State evaluate(double temperatureKelvin, double pressurePascal, double g0, double g1) {
        if (!(temperatureKelvin > 0) || !Double.isFinite(temperatureKelvin) || !(pressurePascal > 0) || !Double.isFinite(pressurePascal)
                || !Double.isFinite(g0) || !Double.isFinite(g1))
            throw new IllegalArgumentException("Jaeger-Span solid: (" + temperatureKelvin + " K, " + pressurePascal + " Pa) is not a positive finite state");
        double te = Math.min(Math.max(temperatureKelvin, minimumTemperature), maximumTemperature);
        double pe = Math.min(Math.max(pressurePascal, minimumPressure), maximumPressure);
        State end = inside(te, pe, g0, g1);
        if (te == temperatureKelvin && pe == pressurePascal) return end;
        double gibbs = end.gibbs(), dT = end.dT(), dP = end.dP(), dTT = end.dTT(), dTP = end.dTP(), dPP = end.dPP();
        if (pe != pressurePascal) {
            double step = pressurePascal - pe;
            gibbs += dP * step + 0.5 * dPP * step * step;
            dT += dTP * step;
            dP += dPP * step;
        }
        if (te != temperatureKelvin) {
            double step = temperatureKelvin - te, heatCapacity = -te * dTT, log = DeterministicMath.log(temperatureKelvin / te);
            gibbs += dT * step - heatCapacity * (temperatureKelvin * log - step);
            dT -= heatCapacity * log;
            dP += dTP * step;
            dTT = -heatCapacity / temperatureKelvin;
        }
        return new State(temperatureKelvin, pressurePascal, gibbs, dT, dP, dTT, dTP, dPP);
    }

    /** The function itself at (T, P), which lie in its declared range. */
    private State inside(double temperatureKelvin, double pressurePascal, double g0, double g1) {
        double t = temperatureKelvin / referenceTemperature;
        double pi = pressurePascal / referencePressure;
        double dt = t - 1;
        double dp = pi - 1;
        double t2 = t * t;

        // The two heat-capacity terms: A(t) and its derivatives.
        double a4 = DeterministicMath.atan(t / g[4]) - arctan4;
        double a6 = DeterministicMath.atan(t / g[6]) - arctan6;
        double sq4 = t2 + g[4] * g[4];
        double sq6 = t2 + g[6] * g[6];
        double heat = g[3] * (DeterministicMath.log(sq4 / log4) - 2 * t / g[4] * a4) + g[5] * (DeterministicMath.log(sq6 / log6) - 2 * t / g[6] * a6);
        double heatT = -2 * g[3] / g[4] * a4 - 2 * g[5] / g[6] * a6;
        double heatTT = -2 * g[3] / sq4 - 2 * g[5] / sq6;

        // f_alpha (equation 21) and its derivatives (Table 4).
        double q1 = t2 - alpha[2] * t + alpha[3];
        double q4 = t2 + alpha[2] * t + alpha[3];
        double r5 = (t - alpha[6]) / alpha[7];
        double r8 = (t + alpha[6]) / alpha[7];
        double f = alpha[0] * (t2 - 1) + alpha[1] * DeterministicMath.log(q1 / alphaLog1) + alpha[4] * DeterministicMath.log(q4 / alphaLog4)
                + alpha[5] * (DeterministicMath.atan(r5) - alphaArctan5) + alpha[8] * (DeterministicMath.atan(r8) - alphaArctan8);
        double d1 = 2 * t - alpha[2];
        double d4 = 2 * t + alpha[2];
        double w5 = 1 + r5 * r5;
        double w8 = 1 + r8 * r8;
        double fT = 2 * alpha[0] * t + alpha[1] * d1 / q1 + alpha[4] * d4 / q4 + alpha[5] / alpha[7] / w5 + alpha[8] / alpha[7] / w8;
        double fTT = 2 * alpha[0] + alpha[1] * (2 * q1 - d1 * d1) / (q1 * q1) + alpha[4] * (2 * q4 - d4 * d4) / (q4 * q4)
                - alpha[5] / (alpha[7] * alpha[7]) * 2 * r5 / (w5 * w5) - alpha[8] / (alpha[7] * alpha[7]) * 2 * r8 / (w8 * w8);
        double e = DeterministicMath.exp(f);

        // K(t) (equation 22) and the pressure term.
        double k = kappa[0] * t2 + kappa[1] * t + kappa[2];
        double kT = 2 * kappa[0] * t + kappa[1];
        double kTT = 2 * kappa[0];
        double base = pi + g[10];
        double power = DeterministicMath.pow(base, exponent);
        double q = power - offsetPower;
        double qP = exponent * power / base;
        double qPP = exponent * (exponent - 1) * power / (base * base);

        double gibbs = g0 + g1 * dt + g[2] * dt * dt + heat + g[7] * dp * (e + k * g[8]) + g[9] * k * q;
        double gT = g1 + 2 * g[2] * dt + heatT + g[7] * dp * (e * fT + g[8] * kT) + g[9] * kT * q;
        double gP = g[7] * (e + g[8] * k) + g[9] * k * qP;
        double gTT = 2 * g[2] + heatTT + g[7] * dp * (e * (fTT + fT * fT) + g[8] * kTT) + g[9] * kTT * q;
        double gTP = g[7] * (e * fT + g[8] * kT) + g[9] * kT * qP;
        double gPP = g[9] * k * qPP;

        double rt0 = gasConstant * referenceTemperature;
        return new State(temperatureKelvin, pressurePascal, rt0 * gibbs, gasConstant * gT, rt0 / referencePressure * gP,
                gasConstant / referenceTemperature * gTT, gasConstant / referencePressure * gTP,
                rt0 / (referencePressure * referencePressure) * gPP);
    }

    /**
     * The integration constants {g0, g1} for which the function has Gibbs energy {@code gibbs} (J/mol) and entropy
     * {@code entropy} (J/(mol K)) at (T, P): the paper's equations 19 and 20 with the fluid's values. Both conditions
     * are linear in g0 and g1: s = -R (g1 + r_t) fixes g1, then g = R T0 (g0 + g1 (t - 1) + r) fixes g0, where r and r_t
     * are the rest of equation 8 and its t-derivative at the state.
     */
    public double[] integrationConstants(double temperatureKelvin, double pressurePascal, double gibbs, double entropy) {
        if (!Double.isFinite(gibbs) || !Double.isFinite(entropy)) throw new IllegalArgumentException("Anchor values must be finite");
        State rest = evaluate(temperatureKelvin, pressurePascal, 0, 0);
        double g1 = -entropy / gasConstant - rest.dT() / gasConstant;
        double dt = temperatureKelvin / referenceTemperature - 1;
        double g0 = gibbs / (gasConstant * referenceTemperature) - g1 * dt - rest.gibbs() / (gasConstant * referenceTemperature);
        return new double[] {g0, g1};
    }
}
