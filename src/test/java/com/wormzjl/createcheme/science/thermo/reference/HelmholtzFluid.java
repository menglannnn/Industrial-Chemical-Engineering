package com.wormzjl.createcheme.science.thermo.reference;

import com.google.gson.JsonObject;

/**
 * Test oracle: a pure fluid's Helmholtz reference equation of state ported from CoolProp's {@code dev/fluids/*.json}
 * (decision D4 of batch 2026-09-24-coolprop-low-temperature). The reduced Helmholtz energy is
 * a(T, rho) / (R T) = alpha0(tau, delta) + alphar(tau, delta) with tau = T_r / T and delta = rho / rho_r at the EOS
 * block's reducing state; the ideal part carries the file's reference-state offset, so enthalpy and entropy match
 * CoolProp's Hmolar and Smolar directly. The gas constant is the EOS block's own, as CoolProp uses for pure fluids.
 *
 * <p>Test code only: this class is not part of the mod and is not a runtime property backend (decision D1).
 */
public final class HelmholtzFluid {
    /** Which root of p(T, rho) = P a density solve returns below the critical temperature. */
    public enum Root { LIQUID, VAPOUR }

    private static final int MAX_DENSITY_ITERATIONS = 200;
    private static final int MAX_SATURATION_ITERATIONS = 100;

    private final String name;
    private final double gasConstant;
    private final double molarMass;
    private final double reducingTemperature;
    private final double reducingDensity;
    private final double criticalTemperature;
    private final double criticalPressure;
    private final double criticalDensity;
    private final double tripleTemperature;
    private final double maximumTemperature;
    private final double maximumPressure;
    private final IdealHelmholtz ideal;
    private final ResidualHelmholtz residual;
    private final SaturationAncillary saturationPressureAncillary;
    private final SaturationAncillary liquidDensityAncillary;
    private final SaturationAncillary vapourDensityAncillary;

    private HelmholtzFluid(JsonObject root) {
        JsonObject eos = root.getAsJsonArray("EOS").get(0).getAsJsonObject();
        JsonObject reducing = eos.getAsJsonObject("STATES").getAsJsonObject("reducing");
        JsonObject critical = root.getAsJsonObject("STATES").getAsJsonObject("critical");
        JsonObject ancillaries = root.getAsJsonObject("ANCILLARIES");
        requireUnits(eos, "gas_constant_units", "J/mol/K");
        requireUnits(eos, "molar_mass_units", "kg/mol");
        requireUnits(reducing, "rhomolar_units", "mol/m^3");
        this.name = root.getAsJsonObject("INFO").get("NAME").getAsString();
        this.gasConstant = JsonFluidFiles.number(eos, "gas_constant");
        this.molarMass = JsonFluidFiles.number(eos, "molar_mass");
        this.reducingTemperature = JsonFluidFiles.number(reducing, "T");
        this.reducingDensity = JsonFluidFiles.number(reducing, "rhomolar");
        this.criticalTemperature = JsonFluidFiles.number(critical, "T");
        this.criticalPressure = JsonFluidFiles.number(critical, "p");
        this.criticalDensity = JsonFluidFiles.number(critical, "rhomolar");
        this.tripleTemperature = JsonFluidFiles.number(eos, "Ttriple");
        this.maximumTemperature = JsonFluidFiles.number(eos, "T_max");
        this.maximumPressure = JsonFluidFiles.number(eos, "p_max");
        this.ideal = IdealHelmholtz.parse(eos.getAsJsonArray("alpha0"));
        this.residual = ResidualHelmholtz.parse(eos.getAsJsonArray("alphar"));
        this.saturationPressureAncillary = SaturationAncillary.parse(ancillaries.getAsJsonObject("pS"));
        this.liquidDensityAncillary = SaturationAncillary.parse(ancillaries.getAsJsonObject("rhoL"));
        this.vapourDensityAncillary = SaturationAncillary.parse(ancillaries.getAsJsonObject("rhoV"));
        if (!(gasConstant > 0.0 && reducingTemperature > 0.0 && reducingDensity > 0.0 && criticalDensity > 0.0)) {
            throw new IllegalArgumentException(name + ": non-positive gas constant or reducing/critical state");
        }
    }

    /** Loads {@code <fluidFile>.json} (for example {@code "Nitrogen"}) from the bundled CoolProp fluid files. */
    public static HelmholtzFluid load(String fluidFile) {
        return parse(JsonFluidFiles.resource(fluidFile + ".json"));
    }

    /** Parses a CoolProp fluid file; only the first EOS block is read. Unknown term or ancillary types are refused. */
    public static HelmholtzFluid parse(JsonObject root) {
        return new HelmholtzFluid(root);
    }

    public double gasConstant() {
        return gasConstant;
    }

    public double reducingTemperature() {
        return reducingTemperature;
    }

    public double reducingDensity() {
        return reducingDensity;
    }

    public double criticalTemperature() {
        return criticalTemperature;
    }

    public double criticalPressure() {
        return criticalPressure;
    }

    public double tripleTemperature() {
        return tripleTemperature;
    }

    public double maximumTemperature() {
        return maximumTemperature;
    }

    /** alpha0 and its derivatives at (tau, delta), written into {@code out}. */
    public void idealDerivatives(double tau, double delta, AlphaDerivatives out) {
        ideal.evaluate(tau, delta, out);
    }

    /** alphar and its derivatives at (tau, delta), written into {@code out}. */
    public void residualDerivatives(double tau, double delta, AlphaDerivatives out) {
        residual.evaluate(tau, delta, out);
    }

    /** All properties at temperature {@code temperature} (K) and molar density {@code density} (mol/m3). */
    public HelmholtzState state(double temperature, double density) {
        double tau = reducingTemperature / temperature;
        double delta = density / reducingDensity;
        AlphaDerivatives a0 = new AlphaDerivatives();
        AlphaDerivatives ar = new AlphaDerivatives();
        ideal.evaluate(tau, delta, a0);
        residual.evaluate(tau, delta, ar);
        double r = gasConstant;
        double deltaArD = delta * ar.dDelta;
        double deltaSqArDD = delta * delta * ar.dDelta2;
        double tauA0T = tau * a0.dTau;
        double tauArT = tau * ar.dTau;
        double tauSqATT = tau * tau * (a0.dTau2 + ar.dTau2);
        double deltaTauArDT = delta * tau * ar.dDeltaDTau;
        double z = 1.0 + deltaArD;
        double pressure = density * r * temperature * z;
        double enthalpy = r * temperature * (1.0 + tauA0T + tauArT + deltaArD);
        double entropy = r * (tauA0T + tauArT - a0.alpha - ar.alpha);
        double internalEnergy = r * temperature * (tauA0T + tauArT);
        double gibbs = r * temperature * (1.0 + a0.alpha + ar.alpha + deltaArD);
        double cv = -r * tauSqATT;
        double stiffness = 1.0 + 2.0 * deltaArD + deltaSqArDD;
        double numerator = 1.0 + deltaArD - deltaTauArDT;
        double cp = cv + r * numerator * numerator / stiffness;
        double logPhi = ar.alpha + deltaArD - Math.log(z);
        return new HelmholtzState(temperature, density, pressure, z, enthalpy, entropy, internalEnergy, gibbs, cp, cv,
                logPhi, r * temperature * stiffness);
    }

    /**
     * Molar density (mol/m3) at temperature {@code temperature} (K) and pressure {@code pressure} (Pa). Below the
     * critical temperature {@code root} selects the vapour root (the smallest, on the branch rising from zero
     * density, searched below the critical density) or the liquid root (the largest, on the branch rising to
     * infinite density, searched above the critical density); a metastable root is returned when one exists. At or
     * above the critical temperature p(rho) is monotonic and {@code root} is ignored. Newton's method on p(rho),
     * safeguarded by a bracket that bisection falls back on; the start is the ideal-gas density for a vapour and the
     * ancillary saturated-liquid density for a liquid.
     */
    public double density(double temperature, double pressure, Root root) {
        if (!(temperature > 0.0 && pressure > 0.0)) {
            throw new IllegalArgumentException(name + ": temperature and pressure must be positive");
        }
        boolean subcritical = temperature < criticalTemperature;
        boolean vapour = subcritical && root == Root.VAPOUR;
        boolean liquid = subcritical && root == Root.LIQUID;
        double tau = reducingTemperature / temperature;
        double rt = gasConstant * temperature;
        double lo = liquid ? criticalDensity : 0.0;
        double hi = vapour ? criticalDensity : Double.POSITIVE_INFINITY;
        double rho;
        if (liquid) {
            double ancillary = liquidDensityAncillary.evaluate(temperature);
            rho = ancillary > criticalDensity ? ancillary : 1.5 * criticalDensity;
        } else {
            rho = Math.min(pressure / rt, vapour ? 0.5 * criticalDensity : Double.POSITIVE_INFINITY);
        }
        AlphaDerivatives ar = new AlphaDerivatives();
        for (int iteration = 0; iteration < MAX_DENSITY_ITERATIONS; iteration++) {
            double delta = rho / reducingDensity;
            residual.evaluate(tau, delta, ar);
            double deltaArD = delta * ar.dDelta;
            double p = rho * rt * (1.0 + deltaArD);
            double dpdrho = rt * (1.0 + 2.0 * deltaArD + delta * delta * ar.dDelta2);
            double f = p - pressure;
            if (f == 0.0) {
                return rho;
            }
            boolean rising = dpdrho > 0.0;
            if (vapour) {
                // Past the vapour spinodal (p falling) the vapour root lies below, whatever the sign of f.
                if (!rising || f > 0.0) {
                    hi = rho;
                } else {
                    lo = rho;
                }
            } else if (liquid) {
                // Left of the liquid spinodal (p falling) the liquid root lies above, whatever the sign of f.
                if (!rising || f < 0.0) {
                    lo = rho;
                } else {
                    hi = rho;
                }
            } else if (f > 0.0) {
                hi = rho;
            } else {
                lo = rho;
            }
            double next = rising ? rho - f / dpdrho : Double.NaN;
            if (!(next > lo && next < hi)) {
                next = Double.isInfinite(hi) ? 2.0 * Math.max(rho, lo) : 0.5 * (lo + hi);
            }
            double step = next - rho;
            rho = next;
            if (Math.abs(step) <= 1.0e-13 * rho) {
                return rho;
            }
        }
        throw new IllegalStateException(name + ": no " + (subcritical ? root : "supercritical")
                + " density at T = " + temperature + " K, p = " + pressure + " Pa");
    }

    /** Density at (T, P) followed by all properties there. */
    public HelmholtzState stateAtPressure(double temperature, double pressure, Root root) {
        return state(temperature, density(temperature, pressure, root));
    }

    /**
     * Saturation at temperature {@code temperature} (K), between the triple point and the critical temperature: the
     * liquid and vapour densities with equal pressure and equal Gibbs energy. Newton's method on Akasaka's (2008)
     * form J(delta_L) = J(delta_V), K(delta_L) = K(delta_V) with J = delta (1 + delta alphar_delta) and
     * K = delta alphar_delta + alphar + ln delta, in the unknowns (delta_L, ln delta_V), started from the ancillaries.
     */
    public SaturationState saturation(double temperature) {
        if (!(temperature >= tripleTemperature && temperature < criticalTemperature)) {
            throw new IllegalArgumentException(name + ": saturation needs " + tripleTemperature + " K <= T < "
                    + criticalTemperature + " K, got " + temperature);
        }
        double tau = reducingTemperature / temperature;
        double deltaL = liquidDensityAncillary.evaluate(temperature) / reducingDensity;
        double logDeltaV = Math.log(vapourDensityAncillary.evaluate(temperature) / reducingDensity);
        if (!(deltaL > 0.0 && Double.isFinite(logDeltaV))) {
            throw new IllegalStateException(name + ": ancillaries give no start at " + temperature + " K");
        }
        AlphaDerivatives liquid = new AlphaDerivatives();
        AlphaDerivatives vapour = new AlphaDerivatives();
        boolean converged = false;
        for (int iteration = 0; iteration < MAX_SATURATION_ITERATIONS && !converged; iteration++) {
            double deltaV = Math.exp(logDeltaV);
            residual.evaluate(tau, deltaL, liquid);
            residual.evaluate(tau, deltaV, vapour);
            double jL = deltaL * (1.0 + deltaL * liquid.dDelta);
            double jV = deltaV * (1.0 + deltaV * vapour.dDelta);
            double kL = deltaL * liquid.dDelta + liquid.alpha + Math.log(deltaL);
            double kV = deltaV * vapour.dDelta + vapour.alpha + logDeltaV;
            double f1 = jV - jL;
            double f2 = kV - kL;
            double jLd = 1.0 + 2.0 * deltaL * liquid.dDelta + deltaL * deltaL * liquid.dDelta2;
            double kLd = 2.0 * liquid.dDelta + deltaL * liquid.dDelta2 + 1.0 / deltaL;
            double kVx = 1.0 + 2.0 * deltaV * vapour.dDelta + deltaV * deltaV * vapour.dDelta2; // dK_V/d(ln delta_V)
            double jVx = deltaV * kVx;                                                         // dJ_V/d(ln delta_V)
            double det = -jLd * kVx + jVx * kLd;
            if (!(Math.abs(det) > 0.0) || !Double.isFinite(det)) {
                throw new IllegalStateException(name + ": singular saturation Jacobian at " + temperature + " K");
            }
            double stepL = (-f1 * kVx + jVx * f2) / det;
            double stepX = (jLd * f2 - kLd * f1) / det;
            double scale = Math.min(1.0, Math.min(0.2 * deltaL / Math.abs(stepL), 0.5 / Math.abs(stepX)));
            deltaL += scale * stepL;
            logDeltaV += scale * stepX;
            converged = scale == 1.0 && Math.abs(stepL) <= 1.0e-13 * deltaL && Math.abs(stepX) <= 1.0e-13;
        }
        double rhoL = deltaL * reducingDensity;
        double rhoV = Math.exp(logDeltaV) * reducingDensity;
        if (!converged || !(rhoL > rhoV * (1.0 + 1.0e-6))) {
            throw new IllegalStateException(name + ": saturation did not converge at " + temperature + " K");
        }
        HelmholtzState liquidState = state(temperature, rhoL);
        HelmholtzState vapourState = state(temperature, rhoV);
        return new SaturationState(temperature, vapourState.pressure(), liquidState, vapourState);
    }

    private static void requireUnits(JsonObject object, String key, String expected) {
        if (object.has(key) && !expected.equals(object.get(key).getAsString())) {
            throw new IllegalArgumentException("expected " + key + " = " + expected + ", got " + object.get(key));
        }
    }
}
