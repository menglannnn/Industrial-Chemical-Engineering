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

    /** Solid-model family id, part of the thermodynamic identity of a package that admits the crystal. */
    String family();

    /** The crystal id, e.g. {@code createcheme:crystal_carbon_dioxide_i}. */
    String crystal();

    /** The species the crystal is made of, a component id of the shared basis. */
    String component();

    /**
     * Fills {@code out} (one component) with the crystal at {@code (temperature, pressure)}; its Gibbs energy at the
     * anchor's state equals the anchor's chemical potential.
     */
    PhaseState evaluate(double temperature, double pressure, TriplePointAnchor anchor, PhaseState out);

    /** {@code g_s(T, P)}, J/mol. */
    default double chemicalPotential(double temperature, double pressure, TriplePointAnchor anchor) {
        return evaluate(temperature, pressure, anchor, new PhaseState(1)).molarGibbsEnergy();
    }

    /**
     * The derivative blocks {@link #derivatives} provides (P4 stage 2a); empty for a model without derivatives. A pure
     * crystal has a fixed composition, so it never provides
     * {@link PhaseEvaluator.Capability#FUGACITY_COMPOSITION_DERIVATIVES}.
     */
    default Set<PhaseEvaluator.Capability> capabilities() { return Set.of(); }

    /**
     * Fills {@code out} (one component) with the crystal at {@code (temperature, pressure)} and every derivative block of
     * {@link #capabilities()}: {@code d ln phi/dT} and {@code d ln phi/dP} against the species' ideal gas
     * ({@code d mu/dT = -s}, {@code d mu/dP = v}), {@code dv/dT = alpha v}, {@code dv/dP = -kappa v}, {@code c_p} and
     * {@code dh/dP = v (1 - alpha T)}. A model without derivatives throws {@link UnsupportedOperationException}.
     */
    default PhaseDerivatives derivatives(double temperature, double pressure, TriplePointAnchor anchor, PhaseDerivatives out) {
        throw new UnsupportedOperationException(family() + " provides no derivatives");
    }
}
