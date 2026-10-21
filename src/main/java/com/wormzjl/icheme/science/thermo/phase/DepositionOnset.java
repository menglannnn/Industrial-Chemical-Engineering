package com.wormzjl.icheme.science.thermo.phase;

import java.util.Objects;

/**
 * The onset of a crystal in a fluid of fixed composition (P4 stage 2a): the frost point
 * ({@link FluidTpEquilibrium#depositionTemperature}, the temperature at a pressure) or the frost pressure
 * ({@link FluidTpEquilibrium#depositionPressure}, the pressure at a temperature) where the crystal's chemical potential
 * equals its species' in the fluid answer of the whole feed: {@code mu_i(T, P, z) = mu_s(T, P)}, fugacities on the
 * package's fluid family, never partial pressures.
 *
 * <p>{@link #approximation()} is the partial-pressure approximation of the same onset, for diagnostics only: the
 * temperature where the family's pure sublimation pressure equals {@code y_i P}, or the pressure
 * {@code p_sub,pure(T) / y_i}; {@code NaN} where the pure species has no sublimation point there (its partial pressure
 * above the triple-point pressure). It is never the answer.</p>
 *
 * <p>Since P5 the same search answers the onset whatever the fluid answer is there ({@link #fluid()}): a vapour (a
 * frost point), a liquid or a liquid-like one-root fluid (a liquidus point, {@link FluidTpEquilibrium#freezingTemperature}),
 * or a vapour-liquid split (a three-phase point).</p>
 *
 * <p>Status rules as {@link EquilibriumResult}'s: {@code CONVERGED} carries the onset state and the fluid answer at it;
 * {@code UNSUPPORTED} a typed reason (the contract's refusals, or {@link EquilibriumResult#CRYSTAL_PHASES} when the fluid
 * at the onset is two liquids); {@code NOT_CONVERGED} the search's failure (an onset beyond the search window: below
 * the numerical box's floor, above the crystal competition's ceiling, or past the box's pressure). A converged onset
 * outside a data range of the models it used is graded {@link EquilibriumResult.CoverageGrade#EXTRAPOLATED}.</p>
 *
 * @param crystal the crystal whose onset this is ({@code null} when the request was refused before one was chosen)
 * @param temperature K: the onset's for {@code depositionTemperature}, the given one for {@code depositionPressure}
 *        ({@code NaN} without an answer)
 * @param pressure Pa: the given one for {@code depositionTemperature}, the onset's for {@code depositionPressure}
 * @param approximation the partial-pressure approximation of the answer's variable (K or Pa), or {@code NaN}
 * @param residual {@code |mu_i - mu_s| / (R T)} at the returned onset, {@code NaN} without one
 * @param fluidEquilibria fluid TP equilibria the search evaluated
 * @param fluid the fluid answer of the whole feed at the onset, {@code null} without one
 */
public record DepositionOnset(EquilibriumResult.Status status, EquilibriumResult.UnsupportedReason reason, String detail,
                              String crystal, double temperature, double pressure,
                              double approximation, double residual, int fluidEquilibria, EquilibriumResult fluid,
                              EquilibriumResult.Coverage coverage) {
    public DepositionOnset {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(coverage, "coverage");
        if ((status == EquilibriumResult.Status.UNSUPPORTED) != (reason != null)) {
            throw new IllegalArgumentException("Exactly an unsupported onset has a reason");
        }
        boolean converged = status == EquilibriumResult.Status.CONVERGED;
        if (converged != (fluid != null) || converged && (!Double.isFinite(temperature) || !Double.isFinite(pressure)
                || crystal == null || !coverage.grade().answer())) {
            throw new IllegalArgumentException("A converged onset has a state, a crystal, its fluid answer and an answer grade");
        }
    }

    public boolean converged() { return status == EquilibriumResult.Status.CONVERGED; }

    /** A refusal or failure: no onset. */
    static DepositionOnset without(EquilibriumResult.Status status, EquilibriumResult.UnsupportedReason reason, String detail,
                                   String crystal, double temperature, double pressure,
                                   double approximation, int fluidEquilibria) {
        return new DepositionOnset(status, reason, detail, crystal, temperature, pressure, approximation, Double.NaN,
                fluidEquilibria, null, new EquilibriumResult.Coverage(EquilibriumResult.CoverageGrade.UNAVAILABLE, detail));
    }
}
