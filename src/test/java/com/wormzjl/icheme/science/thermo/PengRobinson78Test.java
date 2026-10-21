package com.wormzjl.icheme.science.thermo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PengRobinson78Test {
    private static final ThermoComponent METHANE = new ThermoComponent(
            "methane", 190.564, 4_599_200.0, 0.01142, 0.016043);
    private static final ThermoComponent PROPANE = new ThermoComponent(
            "propane", 369.83, 4_248_000.0, 0.1523, 0.044097);

    @Test
    void methaneReferencePointMatchesPengRobinsonCalculation() {
        PengRobinson78 model = PengRobinson78.withoutBinaryInteractions(List.of(METHANE));

        PhaseProperties properties = model.evaluate(
                300.0, 5_000_000.0, new double[] {1.0}, PengRobinsonKernel.Root.VAPOR);

        assertEquals(0.9018307109528809, properties.compressibilityFactor(), 1.0e-12);
        assertEquals(-0.10383585031450415, properties.logFugacityCoefficient(0), 1.0e-12);
        assertEquals(-902.3534536795481, properties.residualEnthalpyJoulesPerMol(), 1.0e-9);
    }

    @Test
    void liquidAndVaporRootsAreSelectedAtAThreeRootState() {
        PengRobinson78 model = PengRobinson78.withoutBinaryInteractions(List.of(PROPANE));

        PhaseProperties liquid = model.evaluate(
                300.0, 1_000_000.0, new double[] {1.0}, PengRobinsonKernel.Root.LIQUID);
        PhaseProperties vapor = model.evaluate(
                300.0, 1_000_000.0, new double[] {1.0}, PengRobinsonKernel.Root.VAPOR);

        assertEquals(0.03478320039102689, liquid.compressibilityFactor(), 1.0e-12);
        assertEquals(0.8146036935018395, vapor.compressibilityFactor(), 1.0e-12);
        assertTrue(liquid.compressibilityFactor() < vapor.compressibilityFactor());
    }
}
