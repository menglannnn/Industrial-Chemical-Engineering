package com.wormzjl.icheme.science.thermo.reference;

/**
 * A reduced Helmholtz energy (ideal part alpha0 or residual part alphar) and its partial derivatives to second
 * order in the reduced density delta = rho / rho_r and the inverse reduced temperature tau = T_r / T.
 * The fields are plain partial derivatives, named like CoolProp's {@code dalphar_dDelta}, {@code d2alphar_dDelta2},
 * {@code d2alphar_dDelta_dTau}; a caller reuses one instance across evaluations.
 */
public final class AlphaDerivatives {
    public double alpha;
    public double dDelta;
    public double dTau;
    public double dDelta2;
    public double dTau2;
    public double dDeltaDTau;

    void clear() {
        alpha = 0.0;
        dDelta = 0.0;
        dTau = 0.0;
        dDelta2 = 0.0;
        dTau2 = 0.0;
        dDeltaDTau = 0.0;
    }
}
