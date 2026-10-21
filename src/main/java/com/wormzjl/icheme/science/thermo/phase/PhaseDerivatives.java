package com.wormzjl.icheme.science.thermo.phase;

import java.util.Objects;

/**
 * First derivatives of one phase at fixed composition, with the {@link PhaseState} they were taken at (the same root).
 *
 * <p>Caller-owned and refilled per evaluation, like {@link PhaseState}. The fluid family fills every block; a pure
 * crystal fills all but the composition block, which a one-component phase has no use for (owner decision O5: no
 * capability gating, a block that was not filled reads as left).</p>
 *
 * <p>Conventions: {@code d ln phi_i / d n_j} at the evaluated composition normalised to one mole (the kernel's), at
 * constant T and P; {@code d ln phi_i / dT} at constant P and composition, {@code d ln phi_i / dP} at constant T and
 * composition; {@code dv/dT}, {@code dv/dP}, {@code dh/dT} (the whole {@code c_p}, ideal-gas part included) and
 * {@code dh/dP} of the molar quantities at constant composition.</p>
 */
public final class PhaseDerivatives {
    private final PhaseState state;
    private final double[][] logFugacityComposition;
    private final double[] logFugacityTemperature;
    private final double[] logFugacityPressure;
    private String family = "";
    private double volumeTemperature;
    private double volumePressure;
    private double enthalpyTemperature;
    private double enthalpyPressure;

    public PhaseDerivatives(int componentCount) {
        state = new PhaseState(componentCount);
        logFugacityComposition = new double[componentCount][componentCount];
        logFugacityTemperature = new double[componentCount];
        logFugacityPressure = new double[componentCount];
    }

    /** The phase these derivatives describe, evaluated on the same root. */
    public PhaseState state() { return state; }
    public int componentCount() { return logFugacityTemperature.length; }
    public String family() { return family; }

    public double logFugacityCompositionDerivative(int component, int respectTo) {
        return logFugacityComposition[component][respectTo];
    }
    public double[] logFugacityTemperatureDerivativeView() { return logFugacityTemperature; }
    public double[] logFugacityPressureDerivativeView() { return logFugacityPressure; }
    /** m3/(mol K). */
    public double volumeTemperatureDerivative() { return volumeTemperature; }
    /** m3/(mol Pa). */
    public double volumePressureDerivative() { return volumePressure; }
    /** {@code c_p}, J/(mol K), ideal-gas part included. */
    public double enthalpyTemperatureDerivative() { return enthalpyTemperature; }
    /** m3/mol ({@code J/(mol Pa)}). */
    public double enthalpyPressureDerivative() { return enthalpyPressure; }

    // Filling, for an evaluator. Nothing here allocates.

    /** Names the family filling this. */
    public void begin(String family) { this.family = Objects.requireNonNull(family, "family"); }
    /** Writable row buffer for {@code d ln phi_i / d n_j}. */
    public double[] logFugacityCompositionRowBuffer(int component) { return logFugacityComposition[component]; }
    public double[] logFugacityTemperatureBuffer() { return logFugacityTemperature; }
    public double[] logFugacityPressureBuffer() { return logFugacityPressure; }
    public void setVolumetric(double volumeTemperatureDerivative, double volumePressureDerivative) {
        volumeTemperature = volumeTemperatureDerivative;
        volumePressure = volumePressureDerivative;
    }
    public void setCaloric(double enthalpyTemperatureDerivative, double enthalpyPressureDerivative) {
        enthalpyTemperature = enthalpyTemperatureDerivative;
        enthalpyPressure = enthalpyPressureDerivative;
    }
}
