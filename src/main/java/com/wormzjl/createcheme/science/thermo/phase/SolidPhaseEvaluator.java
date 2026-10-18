package com.wormzjl.createcheme.science.thermo.phase;

import java.util.Objects;
import java.util.Set;

/**
 * A pure crystal of one species: the P2 solid-evaluator contract, implemented for crystal records by
 * {@link CrystalPhaseEvaluator} (P4; the solid CO2 model of Jaeger and Span 2012).
 *
 * <p>A pure solid is anchored at the triple point to the fluid model (plan section 4): the caller evaluates the fluid
 * family's pure liquid at the species' triple point and passes its chemical potential and enthalpy as a
 * {@link TriplePointAnchor}; the solid model contributes everything else from its own record (its enthalpy of fusion
 * at the anchor, heat capacity, volume, expansion and compressibility), so that g_s = mu_L and h_s = h_L - dH_fus
 * there. Melting, sublimation and the triple point then agree with the fluid family by construction, and a change of
 * the fluid family moves the solid with it: the anchor is part of the result's thermodynamic identity through the
 * fluid family.</p>
 *
 * <p>The state is filled as {@link PhaseKind#SOLID} with {@link #crystal()}, over the one-component basis of
 * {@link #component()}, with {@code ln phi = (g_s - g_ig(T, P))/(R T)} against the species' ideal-gas function, so its
 * {@link PhaseState#chemicalPotential(int)} compares directly with a fluid phase's.</p>
 */
public interface SolidPhaseEvaluator {
    /**
     * The fluid family's pure liquid at the species' triple point: chemical potential and molar enthalpy, J/mol on the
     * spine's reference. P4 added the enthalpy: the solid's entropy at the anchor is the liquid's minus
     * {@code dH_fus / T_t}, and the liquid's entropy is {@code (h_L - mu_L) / T_t}.
     *
     * @param component the species id; must equal the evaluator's {@link #component()}
     */
    record TriplePointAnchor(String component, double temperature, double pressure, double chemicalPotential,
                             double liquidEnthalpy) {
        public TriplePointAnchor {
            Objects.requireNonNull(component, "component");
            if (!(temperature > 0.0) || !(pressure > 0.0) || !Double.isFinite(temperature) || !Double.isFinite(pressure)
                    || !Double.isFinite(chemicalPotential) || !Double.isFinite(liquidEnthalpy)) {
                throw new IllegalArgumentException("A triple-point anchor needs a finite state, chemical potential and enthalpy");
            }
        }
    }

    /**
     * Fills {@code out} (one component) with the crystal at {@code (temperature, pressure)}; its Gibbs energy at the
     * anchor's state equals the anchor's chemical potential.
     */
    PhaseState evaluate(double temperature, double pressure, TriplePointAnchor anchor, PhaseState out);

    /** {@code g_s(T, P)}, J/mol. */
    default double chemicalPotential(double temperature, double pressure, TriplePointAnchor anchor) {
        return evaluate(temperature, pressure, anchor, new PhaseState(1)).molarGibbsEnergy();
    }

}
