package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.science.material.CrystalReference;
import com.wormzjl.createcheme.science.math.DeterministicMath;
import com.wormzjl.createcheme.science.thermo.IdealGasFunction;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * The free water's ice, ice Ih (WP4 of batch 2026-09-24-coolprop-low-temperature, {@code RANGE_EXTRAPOLATION_PLAN.md}
 * section 6, decisions 3 to 6 A): what the free-water rule of {@link FluidTpEquilibrium} needs below the melting line. Ice
 * is decided by water's own rule, never by a crystal competition: free water is pure water beside the hydrocarbons, and
 * its condensed phase is liquid above the melting line {@code T_m(P)} and ice below it.
 *
 * <p>The model. The water record's ice record ({@code crystals/ice_ih.json}, a segments {@link CrystalReference} fitted
 * to IAPWS R10-06) anchored to the water model's liquid at the triple point: the ice's enthalpy is the liquid's there
 * less the fusion enthalpy, and its heat capacity and volume are the record's. The melting line is Clapeyron's from the
 * record, {@code dT/dP = T_t (v_L - v_s) / h_fus} with {@code v_L} the water model's liquid at the triple point
 * (decision 5 A): -7.42e-8 K/Pa, 272.42 K at 10 MPa.</p>
 *
 * <p>The steam over ice. The rule's liquid water has the fugacity {@code p_sat(T)} at every pressure. The ice's is
 * {@code f_s(T, P) = p_sub(T) p_sat(T_m(P)) / p_sub(T_m(P))}: IAPWS R14-08's sublimation pressure (equation 6, from the
 * water record), scaled at each pressure so that it meets the liquid's exactly on the melting line. Ice is then the
 * condensed phase of the lower fugacity on each side of the line ({@code p_sub/p_sat} grows with T below the triple
 * point), the steam's partial pressure is continuous across it, and a freeze at fixed pressure changes the condensed
 * water alone. The scale is {@code 1 + 7.3e-5} at 1 bar and {@code 1 + 7.2e-3} at 10 MPa, of the order of the
 * pressure's true enhancement of the vapour over ice (Poynting, 9e-4 at 1 bar); at the triple-point pressure it is the
 * water model's own {@code p_sat(T_t)/p_sub(T_t)}, 1 within 3e-6. Both functions continue past their data (owner rule of
 * 2026-10-16).</p>
 *
 * <p>Immutable and safe to share between threads; it allocates nothing but the property records of
 * {@link #properties}.</p>
 */
public final class FreeWaterIce implements CrystalFugacity {
    private final CrystalPhaseEvaluator.Anchored anchored;
    private final DoubleUnaryOperator saturation;
    private final double tripleTemperature, triplePressure, slope;
    private final double[] coefficients, exponents;

    private FreeWaterIce(CrystalPhaseEvaluator.Anchored anchored, DoubleUnaryOperator saturation, double liquidVolume,
                         List<Double> coefficients, List<Double> exponents) {
        this.anchored = anchored;
        this.saturation = saturation;
        CrystalReference record = anchored.record();
        tripleTemperature = record.anchor().temperature();
        triplePressure = record.anchor().pressure();
        double solidVolume = anchored.properties(tripleTemperature, triplePressure).volume();
        slope = tripleTemperature * (liquidVolume - solidVolume) / record.anchor().fusionEnthalpy();
        if (!(slope < 0.0) || !Double.isFinite(slope)) {
            throw new IllegalArgumentException("Ice " + record.id() + ": the liquid at the triple point (" + liquidVolume
                    + " m3/mol) must be denser than the ice (" + solidVolume + " m3/mol)");
        }
        if (coefficients.size() != exponents.size() || coefficients.isEmpty()) {
            throw new IllegalArgumentException("Ice " + record.id() + ": the sublimation pressure needs one exponent per coefficient");
        }
        this.coefficients = coefficients.stream().mapToDouble(Double::doubleValue).toArray();
        this.exponents = exponents.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * The ice of a free-water model: {@code record}, a usable crystal record of water, anchored at its triple point to the
     * model's liquid there ({@code liquidVolume} and {@code liquidEnthalpy}, m3/mol and J/mol on the datum of
     * {@code steam}), with the liquid's chemical potential {@code g_ig(T_t, p_sat(T_t))} of the rule; the sublimation
     * pressure's R14-08 coefficients and exponents; the liquid's saturation pressure {@code saturation} (Pa of K).
     */
    public static FreeWaterIce of(CrystalReference record, List<Double> coefficients, List<Double> exponents,
                                  DoubleUnaryOperator saturation, double liquidVolume, double liquidEnthalpy, IdealGasFunction steam) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(saturation, "saturation");
        if (!record.component().equals("Water")) throw new IllegalArgumentException("Ice " + record.id() + " is made of " + record.component());
        double t = record.anchor().temperature();
        double p = record.anchor().pressure();
        double chemicalPotential = steam.enthalpy(t) - t * steam.entropy(t, saturation.applyAsDouble(t));
        var anchor = new SolidPhaseEvaluator.TriplePointAnchor(record.component(), t, p, chemicalPotential, liquidEnthalpy);
        return new FreeWaterIce(CrystalPhaseEvaluator.of(record, steam).anchored(anchor), saturation, liquidVolume,
                coefficients, exponents);
    }

    /** The ice record's id, the crystal an ice phase names. */
    public String crystal() { return anchored.crystal(); }
    /** The record anchored to the water model's liquid: the ice's enthalpy, volume and heat capacity. */
    public CrystalPhaseEvaluator.Anchored anchored() { return anchored; }
    /** The record's triple point, K. */
    public double tripleTemperature() { return tripleTemperature; }
    /** {@code dT_m/dP}, K/Pa: Clapeyron's slope of the melting line from the record. */
    public double meltingSlope() { return slope; }

    /** The melting temperature at {@code pressure}: {@code T_t + (dT/dP) (P - p_t)}, K. */
    public double meltingTemperature(double pressure) {
        return tripleTemperature + slope * (pressure - triplePressure);
    }

    /** The pressure at which {@code temperature} is the melting temperature (the line inverted), Pa; not positive above it. */
    public double meltingPressure(double temperature) {
        return triplePressure + (temperature - tripleTemperature) / slope;
    }

    /**
     * IAPWS R14-08 equation 6: {@code ln(p_sub/p_t) = (T_t/T) sum a_i theta^b_i}, {@code theta = T/T_t}, Pa (5.6e-12 Pa at
     * 111.6 K, 259.87 Pa at 263.15 K); continued past its data, smooth to the numerical box.
     */
    public double sublimationPressure(double temperature) {
        return triplePressure * DeterministicMath.exp(logSublimationRatio(temperature));
    }

    private double logSublimationRatio(double temperature) {
        double theta = temperature / tripleTemperature;
        double sum = 0.0;
        for (int i = 0; i < coefficients.length; i++) sum += coefficients[i] * DeterministicMath.pow(theta, exponents[i]);
        return sum / theta;
    }

    /**
     * The ice's fugacity at {@code (temperature, pressure)}, Pa: {@code p_sub(T) p_sat(T_m(P)) / p_sub(T_m(P))}, the steam's
     * partial pressure over ice (see the class comment).
     */
    public double fugacity(double temperature, double pressure) {
        double melting = meltingTemperature(pressure);
        double scale = DeterministicMath.log(saturation.applyAsDouble(melting) / triplePressure) - logSublimationRatio(melting);
        return triplePressure * DeterministicMath.exp(logSublimationRatio(temperature) + scale);
    }

    /** {@code ln(f_s/P)}: the ice's chemical potential against ideal steam at the state pressure. */
    @Override
    public double logFugacityCoefficient(double temperature, double pressure) {
        return DeterministicMath.log(fugacity(temperature, pressure) / pressure);
    }

    /** The ice's g, h, s, v, c_p, expansion and compressibility at (T, P) on the water model's datum (the record's, continued). */
    public CrystalReference.State properties(double temperature, double pressure) {
        return anchored.properties(temperature, pressure);
    }
}
