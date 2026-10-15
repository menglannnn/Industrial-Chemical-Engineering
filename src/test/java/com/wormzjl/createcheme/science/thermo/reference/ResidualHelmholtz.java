package com.wormzjl.createcheme.science.thermo.reference;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;

/**
 * Residual part alphar(tau, delta) of a CoolProp pure-fluid equation of state, read from the {@code alphar} array of
 * a {@code dev/fluids/*.json} EOS block. The term families and their CoolProp names:
 * <ul>
 *   <li>{@code ResidualHelmholtzPower}: n delta^d tau^t, times exp(-delta^l) where l &gt; 0;</li>
 *   <li>{@code ResidualHelmholtzGaussian}: n delta^d tau^t exp(-eta (delta - epsilon)^2 - beta (tau - gamma)^2);</li>
 *   <li>{@code ResidualHelmholtzNonAnalytic}: n Delta^b delta psi with Delta = theta^2 + B ((delta - 1)^2)^a,
 *       theta = (1 - tau) + A ((delta - 1)^2)^(1 / (2 beta)), psi = exp(-C (delta - 1)^2 - D (tau - 1)^2)
 *       (Wagner and Pruss 2002, Span and Wagner 1996).</li>
 * </ul>
 * Any other term type is refused at load.
 */
final class ResidualHelmholtz {
    private double[] powerN = new double[0];
    private double[] powerD = new double[0];
    private double[] powerT = new double[0];
    private double[] powerL = new double[0];

    private double[] gaussN = new double[0];
    private double[] gaussD = new double[0];
    private double[] gaussT = new double[0];
    private double[] gaussEta = new double[0];
    private double[] gaussEpsilon = new double[0];
    private double[] gaussBeta = new double[0];
    private double[] gaussGamma = new double[0];

    private double[] naN = new double[0];
    private double[] naSmallA = new double[0];
    private double[] naSmallB = new double[0];
    private double[] naBeta = new double[0];
    private double[] naA = new double[0];
    private double[] naB = new double[0];
    private double[] naC = new double[0];
    private double[] naD = new double[0];

    static ResidualHelmholtz parse(JsonArray alphar) {
        ResidualHelmholtz residual = new ResidualHelmholtz();
        for (JsonElement element : alphar) {
            JsonObject term = element.getAsJsonObject();
            String type = term.get("type").getAsString();
            switch (type) {
                case "ResidualHelmholtzPower" -> {
                    double[] n = JsonFluidFiles.doubles(term, "n");
                    residual.powerN = concat(residual.powerN, n);
                    residual.powerD = concat(residual.powerD, sized(JsonFluidFiles.doubles(term, "d"), n, "d"));
                    residual.powerT = concat(residual.powerT, sized(JsonFluidFiles.doubles(term, "t"), n, "t"));
                    residual.powerL = concat(residual.powerL, sized(JsonFluidFiles.doubles(term, "l"), n, "l"));
                }
                case "ResidualHelmholtzGaussian" -> {
                    double[] n = JsonFluidFiles.doubles(term, "n");
                    residual.gaussN = concat(residual.gaussN, n);
                    residual.gaussD = concat(residual.gaussD, sized(JsonFluidFiles.doubles(term, "d"), n, "d"));
                    residual.gaussT = concat(residual.gaussT, sized(JsonFluidFiles.doubles(term, "t"), n, "t"));
                    residual.gaussEta = concat(residual.gaussEta, sized(JsonFluidFiles.doubles(term, "eta"), n, "eta"));
                    residual.gaussEpsilon = concat(residual.gaussEpsilon,
                            sized(JsonFluidFiles.doubles(term, "epsilon"), n, "epsilon"));
                    residual.gaussBeta = concat(residual.gaussBeta, sized(JsonFluidFiles.doubles(term, "beta"), n, "beta"));
                    residual.gaussGamma = concat(residual.gaussGamma,
                            sized(JsonFluidFiles.doubles(term, "gamma"), n, "gamma"));
                }
                case "ResidualHelmholtzNonAnalytic" -> {
                    double[] n = JsonFluidFiles.doubles(term, "n");
                    residual.naN = concat(residual.naN, n);
                    residual.naSmallA = concat(residual.naSmallA, sized(JsonFluidFiles.doubles(term, "a"), n, "a"));
                    residual.naSmallB = concat(residual.naSmallB, sized(JsonFluidFiles.doubles(term, "b"), n, "b"));
                    residual.naBeta = concat(residual.naBeta, sized(JsonFluidFiles.doubles(term, "beta"), n, "beta"));
                    residual.naA = concat(residual.naA, sized(JsonFluidFiles.doubles(term, "A"), n, "A"));
                    residual.naB = concat(residual.naB, sized(JsonFluidFiles.doubles(term, "B"), n, "B"));
                    residual.naC = concat(residual.naC, sized(JsonFluidFiles.doubles(term, "C"), n, "C"));
                    residual.naD = concat(residual.naD, sized(JsonFluidFiles.doubles(term, "D"), n, "D"));
                }
                default -> throw new IllegalArgumentException("unsupported alphar term type " + type);
            }
        }
        return residual;
    }

    void evaluate(double tau, double delta, AlphaDerivatives out) {
        out.clear();
        addPowerAndGaussian(tau, delta, out);
        addNonAnalytic(tau, delta, out);
    }

    /**
     * Power and Gaussian terms, accumulated as the reduced derivatives delta^i tau^j d^(i+j) alpha, which are
     * products of the term value with low-order polynomials; converted to plain derivatives at the end.
     */
    private void addPowerAndGaussian(double tau, double delta, AlphaDerivatives out) {
        double logTau = Math.log(tau);
        double logDelta = Math.log(delta);
        double sum = 0.0;
        double sumD = 0.0;
        double sumT = 0.0;
        double sumDD = 0.0;
        double sumTT = 0.0;
        double sumDT = 0.0;
        for (int i = 0; i < powerN.length; i++) {
            double d = powerD[i];
            double t = powerT[i];
            double l = powerL[i];
            double deltaL = l > 0.0 ? Math.pow(delta, l) : 0.0;
            // u = -delta^l (only for l > 0); g = delta du/ddelta = -l delta^l; delta^2 d2u/ddelta2 = (l - 1) g.
            double g = -l * deltaL;
            double value = powerN[i] * Math.exp(t * logTau + d * logDelta - deltaL);
            double b = d + g;
            sum += value;
            sumD += value * b;
            sumDD += value * (b * (b - 1.0) + l * g);
            sumT += value * t;
            sumTT += value * t * (t - 1.0);
            sumDT += value * t * b;
        }
        for (int i = 0; i < gaussN.length; i++) {
            double d = gaussD[i];
            double t = gaussT[i];
            double eta = gaussEta[i];
            double beta = gaussBeta[i];
            double dd = delta - gaussEpsilon[i];
            double dt = tau - gaussGamma[i];
            double value = gaussN[i] * Math.exp(t * logTau + d * logDelta - eta * dd * dd - beta * dt * dt);
            double b = d - 2.0 * eta * delta * dd;
            double c = t - 2.0 * beta * tau * dt;
            sum += value;
            sumD += value * b;
            sumDD += value * (b * b - d - 2.0 * eta * delta * delta);
            sumT += value * c;
            sumTT += value * (c * c - t - 2.0 * beta * tau * tau);
            sumDT += value * b * c;
        }
        out.alpha += sum;
        out.dDelta += sumD / delta;
        out.dDelta2 += sumDD / (delta * delta);
        out.dTau += sumT / tau;
        out.dTau2 += sumTT / (tau * tau);
        out.dDeltaDTau += sumDT / (delta * tau);
    }

    /**
     * Non-analytic critical-region terms with the derivatives of IAPWS-95 (Wagner and Pruss 2002, table 5). The
     * factors (delta - 1)^2 raised to negative powers in d2Delta/ddelta2 are multiplied out so that every term is
     * finite at delta = 1 (the terms' exponents 1/(2 beta) - 1 and a - 1 are positive for water and CO2).
     */
    private void addNonAnalytic(double tau, double delta, AlphaDerivatives out) {
        double dm1 = delta - 1.0;
        double dm1sq = dm1 * dm1;
        double tm1 = tau - 1.0;
        for (int i = 0; i < naN.length; i++) {
            double n = naN[i];
            double a = naSmallA[i];
            double b = naSmallB[i];
            double beta = naBeta[i];
            double bigA = naA[i];
            double bigB = naB[i];
            double bigC = naC[i];
            double bigD = naD[i];

            double q = Math.pow(dm1sq, 0.5 / beta - 1.0);        // ((delta-1)^2)^(1/(2 beta) - 1)
            double r = Math.pow(dm1sq, a - 1.0);                 // ((delta-1)^2)^(a - 1)
            double theta = (1.0 - tau) + bigA * q * dm1sq;       // A ((delta-1)^2)^(1/(2 beta))
            double bigDelta = theta * theta + bigB * r * dm1sq;  // B ((delta-1)^2)^a

            double psi = Math.exp(-bigC * dm1sq - bigD * tm1 * tm1);
            double psiD = -2.0 * bigC * dm1 * psi;
            double psiDD = (2.0 * bigC * dm1sq - 1.0) * 2.0 * bigC * psi;
            double psiT = -2.0 * bigD * tm1 * psi;
            double psiTT = (2.0 * bigD * tm1 * tm1 - 1.0) * 2.0 * bigD * psi;
            double psiDT = 4.0 * bigC * bigD * dm1 * tm1 * psi;

            // dDelta/ddelta = (delta - 1) * gradientOverDm1
            double gradientOverDm1 = bigA * theta * (2.0 / beta) * q + 2.0 * bigB * a * r;
            double bigDeltaD = dm1 * gradientOverDm1;
            double bigDeltaDD = gradientOverDm1
                    + 4.0 * bigB * a * (a - 1.0) * r
                    + 2.0 * bigA * bigA / (beta * beta) * q * q * dm1sq
                    + bigA * theta * (4.0 / beta) * (0.5 / beta - 1.0) * q;

            double powB = Math.pow(bigDelta, b);
            double powB1 = Math.pow(bigDelta, b - 1.0);
            double powB2 = Math.pow(bigDelta, b - 2.0);
            double dbD = b * powB1 * bigDeltaD;
            double dbDD = b * (powB1 * bigDeltaDD + (b - 1.0) * powB2 * bigDeltaD * bigDeltaD);
            double dbT = -2.0 * theta * b * powB1;
            double dbTT = 2.0 * b * powB1 + 4.0 * theta * theta * b * (b - 1.0) * powB2;
            double dbDT = -bigA * b * (2.0 / beta) * powB1 * dm1 * q - 2.0 * theta * b * (b - 1.0) * powB2 * bigDeltaD;

            out.alpha += n * powB * delta * psi;
            out.dDelta += n * (powB * (psi + delta * psiD) + dbD * delta * psi);
            out.dDelta2 += n * (powB * (2.0 * psiD + delta * psiDD) + 2.0 * dbD * (psi + delta * psiD)
                    + dbDD * delta * psi);
            out.dTau += n * delta * (dbT * psi + powB * psiT);
            out.dTau2 += n * delta * (dbTT * psi + 2.0 * dbT * psiT + powB * psiTT);
            out.dDeltaDTau += n * (powB * (psiT + delta * psiDT) + delta * dbD * psiT + dbT * (psi + delta * psiD)
                    + dbDT * delta * psi);
        }
    }

    private static double[] sized(double[] values, double[] n, String key) {
        if (values.length != n.length) {
            throw new IllegalArgumentException("alphar key " + key + " has " + values.length + " entries, n has "
                    + n.length);
        }
        return values;
    }

    private static double[] concat(double[] a, double[] b) {
        double[] joined = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, joined, a.length, b.length);
        return joined;
    }
}
