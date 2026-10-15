package com.wormzjl.createcheme.science.column.v3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wormzjl.createcheme.science.column.v3.thermo.V3FlashResult;
import com.wormzjl.createcheme.science.column.v3.thermo.V3FugacityResult;
import com.wormzjl.createcheme.science.column.v3.thermo.V3ThermoModel;
import com.wormzjl.createcheme.science.column.v3.thermo.V3ThermoWorkspace;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root;
import java.util.List;
import org.junit.jupiter.api.Test;

class V3DryMeshCoordinateMapTest {
    @Test
    void ledgerOrderedLogCoordinatesRoundTripAllActiveFlowsAndTemperaturesWithoutAFloor() {
        V3ColumnProblem problem = problem();
        V3DryMeshCoordinateMap map = new V3DryMeshCoordinateMap(problem);
        V3DryMeshState state = state(problem.topology());

        double[] coordinates = map.encode(state);
        V3DryMeshState decoded = map.decode(coordinates);

        assertEquals(29, map.coordinateCount());
        for (int node = 0; node < state.nodeCount(); node++) {
            for (int component = 0; component < state.componentCount(); component++) {
                assertEquals(state.liquidFlow(node, component), decoded.liquidFlow(node, component), 1.0e-12);
                assertEquals(state.vaporFlow(node, component), decoded.vaporFlow(node, component), 1.0e-12);
            }
            assertEquals(state.temperatureKelvin(node), decoded.temperatureKelvin(node), 1.0e-12);
        }
    }

    @Test
    void coordinateMapRejectsZeroActiveFlowRatherThanAddingABalanceFloor() {
        V3ColumnProblem problem = problem();
        double[][] liquid = liquidFlows();
        liquid[0][0] = 0.0;
        V3DryMeshState zeroActiveFlow = new V3DryMeshState(problem.topology(), 2, liquid, vaporFlows(), temperatures());

        assertThrows(IllegalArgumentException.class, () -> new V3DryMeshCoordinateMap(problem).encode(zeroActiveFlow));
    }

    @Test
    void wholeSystemFiniteDifferenceJacobianIsFiniteAndMatchesAnAnalyticMaterialDerivative() {
        V3ColumnProblem problem = problem();
        SmoothManufacturedThermo thermo = new SmoothManufacturedThermo();
        V3MeshResidualEvaluator evaluator = new V3MeshResidualEvaluator(problem, thermo, 0.0);
        V3DryMeshCoordinateMap map = new V3DryMeshCoordinateMap(problem);

        V3FiniteDifferenceJacobian.Jacobian jacobian = V3JacobianOracle.evaluate(
                evaluator, map, state(problem.topology()), thermo::newWorkspace);

        assertEquals(29, jacobian.equations().size());
        assertEquals(29, jacobian.unknowns().size());
        for (double[] row : jacobian.values()) {
            assertEquals(29, row.length);
            for (double value : row) assertTrue(Double.isFinite(value));
        }
        int condenserMaterial = jacobian.equations().indexOf(new V3DegreeOfFreedomLedger.EquationId(
                V3DegreeOfFreedomLedger.EquationFamily.COMPONENT_MATERIAL_BALANCE, 0, 0));
        int trayOneVapor = jacobian.unknowns().indexOf(new V3DegreeOfFreedomLedger.UnknownId(
                V3DegreeOfFreedomLedger.UnknownFamily.VAPOR_COMPONENT_FLOW, 1, 0));
        // The condenser balance of component a is 18 in, 8 out as vapor and 10 out as liquid. Its scale is the
        // largest of those terms, so the derivative with respect to that term's own log flow is exactly one:
        // every material row now carries a unit entry, which is what keeps the equilibrated LU well posed.
        assertEquals(1.0, jacobian.values()[condenserMaterial][trayOneVapor], 1.0e-6);
    }

    private static V3ColumnProblem problem() {
        V3ColumnInput input = V3TestInputs.dry(V3ColumnInput.SCHEMA_VERSION, "test:manufactured", "test:binary",
                new V3ComponentBasis(List.of("component-a", "component-b")), new double[] {30.0, 60.0}, 400.0,
                4, 2, 250_000.0, 750.0, List.of(
                        new V3ColumnSpecification.CondenserOutletTemperature(400.0),
                        new V3ColumnSpecification.OrganicRefluxRatio(1.0),
                        new V3ColumnSpecification.ReboilerDuty(0.0)));
        return V3ColumnProblemResolver.resolve(input, V3CondenserPhaseBranch.TWO_PHASE);
    }

    private static V3DryMeshState state(V3ColumnTopology topology) {
        return new V3DryMeshState(topology, 2, liquidFlows(), vaporFlows(), temperatures());
    }

    private static double[][] liquidFlows() {
        return new double[][] {
                {10.0, 10.0}, {5.0, 5.0}, {35.0, 65.0}, {35.0, 65.0}, {35.0, 65.0}, {17.0, 53.0}
        };
    }

    private static double[][] vaporFlows() {
        return new double[][] {
                {8.0, 2.0}, {18.0, 12.0}, {18.0, 12.0}, {18.0, 12.0}, {18.0, 12.0}, {18.0, 12.0}
        };
    }

    private static double[] temperatures() {
        return new double[] {400.0, 410.0, 420.0, 430.0, 440.0, 450.0};
    }

    private static final class SmoothManufacturedThermo implements V3ThermoModel {
        private static final double[][] K = {
                {1.6, 0.4}, {1.2, 0.8}, {12.0 / 7.0, 8.0 / 13.0},
                {12.0 / 7.0, 8.0 / 13.0}, {12.0 / 7.0, 8.0 / 13.0}, {42.0 / 17.0, 28.0 / 53.0}
        };
        private final V3ComponentBasis basis = new V3ComponentBasis(List.of("component-a", "component-b"));

        @Override public V3ComponentBasis componentBasis() { return basis; }
        @Override public V3ThermoWorkspace newWorkspace() { return new V3ThermoWorkspace(2); }

        @Override
        public V3FugacityResult fugacity(
                double temperatureKelvin, double pressurePascal, double[] composition, Root phase,
                V3ThermoWorkspace workspace) {
            int node = (int) Math.round((temperatureKelvin - 400.0) / 10.0);
            if (node < 0 || node >= K.length) throw new IllegalArgumentException("Manufactured VLE temperature is outside its grid");
            double[] logPhi = new double[2];
            if (phase == Root.LIQUID) {
                double referenceTemperature = 400.0 + 10.0 * node;
                for (int component = 0; component < 2; component++) {
                    logPhi[component] = Math.log(K[node][component])
                            + (component + 1) * 1.0e-3 * (temperatureKelvin - referenceTemperature);
                }
            }
            return new V3FugacityResult(phase, logPhi, 1.0, 0.0, 1, 0.0);
        }

        @Override
        public double molarEnthalpy(
                double temperatureKelvin, double pressurePascal, double[] composition, Root phase,
                V3ThermoWorkspace workspace) {
            return 0.0;
        }

        @Override
        public V3FlashResult flashTP(
                double temperatureKelvin, double pressurePascal, double[] overallComposition, V3ThermoWorkspace workspace) {
            throw new UnsupportedOperationException("The smooth manufactured model does not implement a flash");
        }
    }

    // Merged from V3DryMeshStateOwnershipTest.
    @Test
    void copiedAndAdoptedStatesPreserveBitsAndIndependentPerturbations() {
        var topology = V3ColumnTopology.twoPhase(2, 1);
        double[][] liquid = flows();
        double[][] vapor = flows();
        double[] temperatures = {300, 310, 320, 330};
        double[] water = {-0.0, 1, 2, 0};
        var copied = new V3DryMeshState(topology, 2, liquid, vapor, temperatures, water);
        liquid[1][0] = 999;
        vapor[2][1] = 999;
        temperatures[1] = 999;
        water[1] = 999;
        var adopted = V3DryMeshState.fromOwnedArrays(topology, 2, flows(), flows(),
                new double[] {300, 310, 320, 330}, new double[] {-0.0, 1, 2, 0});
        var moved = adopted.withLiquidFlow(1, 0, 8).withVaporFlow(2, 1, 9)
                .withTemperatureKelvin(1, 350).withFreeWaterFlow(2, 3);
        for (int node = 0; node < 4; node++) {
            assertEquals(copied.temperatureKelvin(node), adopted.temperatureKelvin(node));
            assertEquals(Double.doubleToRawLongBits(copied.freeWaterFlow(node)),
                    Double.doubleToRawLongBits(adopted.freeWaterFlow(node)));
            for (int component = 0; component < 2; component++) {
                assertEquals(copied.liquidFlow(node, component), adopted.liquidFlow(node, component));
                assertEquals(copied.vaporFlow(node, component), adopted.vaporFlow(node, component));
            }
        }
        assertEquals(8, moved.liquidFlow(1, 0));
        assertEquals(9, moved.vaporFlow(2, 1));
        assertEquals(350, moved.temperatureKelvin(1));
        assertEquals(3, moved.freeWaterFlow(2));
    }

    @Test
    void adoptionRetainsEveryShapeFiniteDomainAndPhasePlacementCheck() {
        for (boolean adopt : new boolean[] {false, true}) {
            for (int fault = 0; fault < 10; fault++) {
                var topology = fault == 8 ? V3ColumnTopology.liquidOnly(2, 1)
                        : fault == 9 ? V3ColumnTopology.vaporOnly(2, 1) : V3ColumnTopology.twoPhase(2, 1);
                double[][] liquid = flows();
                double[][] vapor = flows();
                double[] temperatures = {300, 310, 320, 330};
                double[] water = {0, 1, 2, 0};
                switch (fault) {
                    case 0 -> liquid[1] = null;
                    case 1 -> vapor[1] = new double[1];
                    case 2 -> liquid[1][0] = Double.NaN;
                    case 3 -> vapor[1][0] = -1;
                    case 4 -> temperatures[2] = Double.POSITIVE_INFINITY;
                    case 5 -> temperatures[2] = 0;
                    case 6 -> water[2] = -1;
                    case 7 -> water[0] = 1;
                    default -> { } // The terminal phase is absent but its input flow is nonzero.
                }
                assertThrows(IllegalArgumentException.class, () -> {
                    if (adopt) V3DryMeshState.fromOwnedArrays(topology, 2, liquid, vapor, temperatures, water);
                    else new V3DryMeshState(topology, 2, liquid, vapor, temperatures, water);
                });
            }
        }
    }

    private static double[][] flows() {
        return new double[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}};
    }
}
