package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;

/**
 * The phase identification parameter of Venkatarathnam and Oellrich (2011, Fluid Phase Equilibria 301, 225-233) on
 * the untranslated PR78 cubic: a liquid-like or vapour-like label for one fluid state that needs no saturation
 * property and no mixture critical point.
 *
 * <p>{@code PIP = v [ (d2P/dT dv) / (dP/dT)_v - (d2P/dv2) / (dP/dv)_T ]}, which is
 * {@code d ln[(dP/dT)_v / -(dP/dv)_T] / d ln v} at constant temperature. The ideal gas has {@code PIP = 1} exactly;
 * a dense liquid tends to {@code v/(v - b) > 1}; a dilute real gas is {@code 1 + (B - T dB/dT)/v} with the second
 * virial coefficient {@code B = b - a/(R T)} of the cubic, below one while {@code B < T dB/dT}. So {@code PIP > 1} is
 * liquid-like and {@code PIP < 1} vapour-like (gas-like), as the paper states; the P3 plan's parenthesis
 * "(> 1 vapour-like)" has the sign reversed and is not followed.</p>
 *
 * <p><b>The label rule (P3 WP7c): liquid-like when {@code PIP > 1} and {@code PIP > Z}</b>, i.e. {@code PIP > max(1, Z)}
 * with {@code Z = P v/(R T)} of the same untranslated root; vapour-like otherwise. The bare {@code PIP > 1} misreads a
 * hot gas: {@code B - T dB/dT = b - 2 a/(R T) + (da/dT)/R} turns positive at high temperature, and a dilute gas then
 * has a parameter just above one (nitrogen above about 620 K on the network and pilot kernels, 1.000079 at 700 K and
 * 0.1 MPa; methane on the pilot kernel above about 1000 K, carbon dioxide above about 1150 K). Such a gas has a positive
 * {@code B} (it is above its Boyle temperature), so {@code Z = 1 + B/v} is above one as well, and above its
 * parameter: to first order in {@code 1/v}, {@code PIP - Z = -T (dB/dT)/v < 0}, because the cubic's {@code B} rises
 * with temperature ({@code a/T} falls) wherever the alpha function decreases, {@code sqrt(T/Tc) < 1 + 1/m}; for the
 * bundled components that holds to 1388 K (nitrogen, the lowest), above every fluid domain's 1200 K. A liquid-like state
 * has its parameter well above one and above {@code Z}: every liquid and dense supercritical state with {@code Z < 1}
 * (nitrogen at 130 K and 6 MPa: {@code Z} 0.39, {@code PIP} 4.97), where the rule is {@code PIP > 1 and Z < 1}, and a
 * compressed liquid whose {@code Z} has reached one (the heaviest crude cut {@code crude_pc12} at 293.15 K and its
 * 2 MPa maximum: {@code Z} 1.15, {@code PIP} about 92), which a bare {@code Z < 1} guard would call vapour. Measured
 * over the one-root states with {@code PIP > 1} and {@code Z >= 1} of every bundled component and several mixtures
 * inside their fluid domains: those less dense than the cubic's critical volume ({@code v > 3.95 b}) all have
 * {@code PIP < Z} (vapour-like; the difference shrinks towards zero from below with the density, as the first-order
 * term says), the denser ones {@code PIP - Z > 91} (liquid-like). The three-root case does not use this rule: there
 * the stability root decides.</p>
 *
 * <p>For the PR78 form {@code P = R T/(v - b) - a/D}, {@code D = v^2 + 2 b v - b^2}, the four derivatives are the ones
 * {@code TranslatedPengRobinson.fill} forms for its volumetric block:</p>
 * <pre>
 *   dP/dv    = -R T/(v - b)^2 + 2 a (v + b)/D^2
 *   dP/dT    =  R/(v - b) - (da/dT)/D
 *   d2P/dv2  =  2 R T/(v - b)^3 + 2 a/D^2 - 8 a (v + b)^2/D^3
 *   d2P/dTdv = -R/(v - b)^2 + 2 (da/dT)(v + b)/D^2
 * </pre>
 * <p>so the parameter needs the mixture's {@code a}, {@code b}, {@code da/dT} and the root's volume only: no second
 * temperature derivative of {@code a} and no composition derivative. It is evaluated on the untranslated volume
 * {@code v = Z R T/P}, the cubic on which roots are selected and equilibria computed (a constant translation shifts
 * the volume axis and so changes the {@code v} prefactor; the label belongs to the root, not to the density
 * correction). It is undefined where {@code (dP/dT)_v = 0} (never for a PR root with {@code v > b} and
 * {@code da/dT < 0}) and at a mechanically unstable root ({@code dP/dv >= 0}, refused by the evaluators anyway);
 * at a pure-component critical point {@code dP/dv} and {@code d2P/dv2} vanish together and the ratio has a finite
 * limit, but a state exactly there is not separated from its neighbours by this parameter, which is why the critical
 * band ({@link FluidTpEquilibrium}) grades such states research-only whatever their label.</p>
 */
public final class PhaseIdentification {
    private static final double R = PengRobinsonKernel.GAS_CONSTANT;

    private PhaseIdentification() {}

    /**
     * The parameter at molar volume {@code v} of the untranslated cubic with mixture parameters {@code a}, {@code b} and
     * {@code da/dT}; {@code NaN} where it is undefined.
     */
    public static double parameter(double temperatureKelvin, double volume, double a, double b, double dadt) {
        double free = volume - b;
        double denominator = volume * volume + 2.0 * b * volume - b * b;
        double dpdv = -R * temperatureKelvin / (free * free) + 2.0 * a * (volume + b) / (denominator * denominator);
        double dpdt = R / free - dadt / denominator;
        double dpdvv = 2.0 * R * temperatureKelvin / (free * free * free) + 2.0 * a / (denominator * denominator)
                - 8.0 * a * (volume + b) * (volume + b) / (denominator * denominator * denominator);
        double dpdtv = -R / (free * free) + 2.0 * dadt * (volume + b) / (denominator * denominator);
        if (!(dpdv < 0.0) || dpdt == 0.0) return Double.NaN;
        return volume * (dpdtv / dpdt - dpdvv / dpdv);
    }

    /** The parameter of a kernel evaluation at its temperature and pressure (its root's untranslated volume). */
    public static double parameter(PengRobinsonKernel.Evaluation evaluation, double temperatureKelvin, double pressurePascal) {
        double volume = evaluation.compressibility() * R * temperatureKelvin / pressurePascal;
        return parameter(temperatureKelvin, volume, evaluation.aMix(), evaluation.bMix(), evaluation.daMixDt());
    }

    /**
     * The parameter's own side of one: {@code PIP > 1}. Not a label by itself (a hot dilute gas has a parameter just
     * above one): label a root with {@link #liquidLike(double, double)}. An undefined parameter is not above one.
     */
    public static boolean liquidLike(double parameter) { return parameter > 1.0; }

    /** The parameter's own side of one: {@code PIP <= 1}; see {@link #liquidLike(double)}. Undefined is neither side. */
    public static boolean vapourLike(double parameter) { return parameter <= 1.0; }

    /**
     * The label of one root (class comment, P3 WP7c): liquid-like when its parameter exceeds both one and its
     * compressibility factor {@code Z = P v/(R T)} on the untranslated cubic. An undefined parameter is not liquid-like.
     */
    public static boolean liquidLike(double parameter, double compressibility) {
        return parameter > 1.0 && parameter > compressibility;
    }

    /**
     * Vapour-like: a defined parameter at most {@code max(1, Z)} (the ideal gas, exactly 1, is vapour; a hot gas whose
     * parameter is just above one lies below its {@code Z}). An undefined parameter is not vapour-like.
     */
    public static boolean vapourLike(double parameter, double compressibility) {
        return parameter <= 1.0 || parameter <= compressibility;
    }

    /** {@link #liquidLike(double, double)} of a kernel evaluation's root at its temperature and pressure. */
    public static boolean liquidLike(PengRobinsonKernel.Evaluation evaluation, double temperatureKelvin, double pressurePascal) {
        return liquidLike(parameter(evaluation, temperatureKelvin, pressurePascal), evaluation.compressibility());
    }

    /** {@link #vapourLike(double, double)} of a kernel evaluation's root at its temperature and pressure. */
    public static boolean vapourLike(PengRobinsonKernel.Evaluation evaluation, double temperatureKelvin, double pressurePascal) {
        return vapourLike(parameter(evaluation, temperatureKelvin, pressurePascal), evaluation.compressibility());
    }

    /**
     * The untranslated PR78 cubic's critical molar volume in units of its co-volume, {@code v_c / b = Z_c / Omega_b =
     * 3.9513730355914}: the kernel's own vapour-branch constant on {@code v/b} (the retired network root heuristic
     * {@code a/(R T b) < 5.8773599486044 || v/b > 3.9513730355914}, and the P1 stability probe's single-root branch
     * test). A root less dense than this is on the gas side of the cubic's critical volume.
     */
    public static final double VAPOUR_BRANCH_VOLUME_RATIO = 3.9513730355914;

    /**
     * Whether a root is denser than the cubic's critical volume: {@code v < }{@link #VAPOUR_BRANCH_VOLUME_RATIO}
     * {@code b} with the untranslated {@code v = Z R T / P} and the mixture co-volume {@code b} of the evaluation.
     */
    public static boolean denserThanCriticalVolume(PengRobinsonKernel.Evaluation evaluation, double temperatureKelvin, double pressurePascal) {
        double volume = evaluation.compressibility() * R * temperatureKelvin / pressurePascal;
        return volume < VAPOUR_BRANCH_VOLUME_RATIO * evaluation.bMix();
    }

    /**
     * The test each product of a liquid-liquid split must pass (P3 WP11): liquid-like by the label rule
     * ({@link #liquidLike(PengRobinsonKernel.Evaluation, double, double)}, {@code PIP > max(1, Z)}) <b>and</b> denser
     * than the cubic's critical volume ({@link #denserThanCriticalVolume}). The parameter alone calls the lighter phase
     * of some high-pressure splits liquid-like by a small margin while it is a dense gas less dense than the critical
     * volume (N2/C2H6 at 195 K and 10 MPa: {@code PIP} 1.048, {@code Z} 0.694, {@code v/b} 4.38; CH4/CO2 at 245 K and
     * 8 MPa: {@code PIP} 1.203, {@code v/b} 3.96); with the volume guard such a split is vapour-liquid again, while the
     * liquid-liquid splits of the type III and CO2-in-methane regions (lighter liquid {@code v/b} 1.09 to 1.1) keep
     * their label.
     */
    public static boolean denseLiquidLike(PengRobinsonKernel.Evaluation evaluation, double temperatureKelvin, double pressurePascal) {
        return liquidLike(evaluation, temperatureKelvin, pressurePascal)
                && denserThanCriticalVolume(evaluation, temperatureKelvin, pressurePascal);
    }
}
