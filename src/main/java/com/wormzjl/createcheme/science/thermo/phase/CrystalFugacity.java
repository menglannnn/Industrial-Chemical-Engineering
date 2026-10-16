package com.wormzjl.createcheme.science.thermo.phase;

/**
 * A crystal's fugacity, as the model that decides the crystal states it: a competing crystal's anchored record
 * ({@link CrystalPhaseEvaluator.Anchored}, its chemical potential against the species' ideal gas) or the free water's ice
 * ({@link FreeWaterIce}, the steam's partial pressure over it). The fluid network's crystal refresh reads a crystal's
 * drive and its equilibrium row through it.
 */
public interface CrystalFugacity {
    /** {@code ln(f_s/P)} at {@code (temperature, pressure)}: the crystal's chemical potential against the ideal gas of its species at P, over R T. */
    double logFugacityCoefficient(double temperature, double pressure);
}
