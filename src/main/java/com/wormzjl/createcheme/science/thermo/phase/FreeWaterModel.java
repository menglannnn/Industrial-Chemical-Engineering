package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.science.thermo.IdealGasFunction;

/**
 * The water of {@link WaterParticipation#SEPARATE_FREE_WATER}: what {@link FluidTpEquilibrium} needs to apply the fluid
 * network's free-water rule (plan P3 section 6.2; the rule as encoded is described on {@link FluidTpEquilibrium}).
 *
 * <p>The network's own implementation is {@code FluidThermodynamics} ({@code saturationPressure},
 * {@code waterLiquid}, {@code vaporWaterEnthalpy}, {@code waterVaporPressureLimit}); an adapter over those methods is
 * the network-integration step's (WP7). Enthalpies must be on the datum of the evaluator's ideal-gas functions, so a
 * result's aggregate enthalpy is one sum.</p>
 */
public interface FreeWaterModel {
    /** Revision of the water model; part of the contract's thermodynamic identity (its water model revision). */
    String revision();

    /**
     * Saturation pressure of pure water, Pa; {@code +infinity} at or above the critical temperature (no liquid). Below
     * the triple point the supercooled liquid's (the model continues past its data, owner rule of 2026-10-16).
     */
    double saturationPressure(double temperature);

    /**
     * Largest partial pressure of ideal steam the approximation is qualified for at this temperature, Pa: a data range,
     * past which a converged answer is graded extrapolated.
     */
    double vaporPartialPressureLimit(double temperature);

    /**
     * Liquid water at (T, P): {@code out[0]} the molar volume (m3/mol), {@code out[1]} the molar enthalpy (J/mol, the
     * ideal-gas functions' datum). Allocates nothing.
     */
    void liquid(double temperature, double pressure, double[] out);

    /** Ideal steam: heat capacity, enthalpy and entropy of water vapour as an ideal gas. */
    IdealGasFunction vapor();
}
