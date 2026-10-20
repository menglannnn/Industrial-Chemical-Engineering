package com.wormzjl.createcheme.science.thermo.reference;

/**
 * Properties of a pure fluid at (T, rho) from its Helmholtz equation of state, in SI molar units and in the fluid
 * file's reference state (so enthalpy and entropy compare directly with CoolProp's Hmolar and Smolar).
 *
 * @param temperature K
 * @param density mol/m3
 * @param pressure Pa
 * @param compressibility Z = p / (rho R T) = 1 + delta dalphar/ddelta
 * @param enthalpy J/mol
 * @param entropy J/(mol K)
 * @param internalEnergy J/mol
 * @param gibbsEnergy J/mol
 * @param isobaricHeatCapacity cp, J/(mol K)
 * @param isochoricHeatCapacity cv, J/(mol K)
 * @param logFugacityCoefficient ln phi = alphar + delta dalphar/ddelta - ln Z
 * @param pressureDensityDerivative (dp/drho) at constant T, Pa m3/mol
 */
public record HelmholtzState(double temperature, double density, double pressure, double compressibility,
                             double enthalpy, double entropy, double internalEnergy, double gibbsEnergy,
                             double isobaricHeatCapacity, double isochoricHeatCapacity,
                             double logFugacityCoefficient, double pressureDensityDerivative) {

    public double fugacityCoefficient() {
        return Math.exp(logFugacityCoefficient);
    }
}
