package com.wormzjl.createcheme.science.thermo.reference;

/**
 * A pure-fluid saturation state at temperature T: equal pressure and equal Gibbs energy of the coexisting liquid and
 * vapour. {@code pressure} is the vapour-side pressure p(T, rho_V), the well-conditioned one: near the triple point
 * the liquid-side pressure is a small difference of large terms and carries a relative rounding error of order
 * 1e-15 / Z_L (about 1e-8 for ethane at 90.4 K).
 */
public record SaturationState(double temperature, double pressure, HelmholtzState liquid, HelmholtzState vapour) {
}
