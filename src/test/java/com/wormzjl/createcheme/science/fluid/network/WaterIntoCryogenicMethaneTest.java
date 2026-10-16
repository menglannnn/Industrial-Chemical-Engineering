package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The pilot world's rows B and D (the branch's P6b regression, re-derived for main's vessel wall in milestone M3 of the
 * unified thermo port): a 1 m3 vessel of nitrogen at 298.15 K and 101.325 kPa between a liquid methane generator, a water
 * generator and a void at 101.325 kPa, on the pilot network package.
 *
 * <p>On the branch the cold methane drew the rigid vessel below the water generator's pressure, water entered, and the
 * mixture fell below 273.16 K, where the water model has no state (D12); its water arrived only in the second TR-BDF2
 * stage, whose companion was a negative amount (fixed there by clipping it). Main has neither: backward Euler has one
 * implicit stage, and a vessel's wall goes slack at the ambient instead of below it (D54), so a vessel the methane cools
 * stands at or above 101.325 kPa and a water generator at the ambient has no drive into it: the rows commit and no water
 * enters. Wherever water is driven into a vessel that falls below 273.16 K (the water generator at 1 MPa) the liquid water
 * would freeze: until ice is modelled (WP4 of the range-extrapolation plan) that is the interim ice hold, a phase hold
 * (leaving the water model's data is none since the owner rule of 2026-10-16). That hold names the vessel: it refuses
 * the line search of the vessel's closure, and the reconstruction that closes the vessel names the failed closure as it names a hold (the design
 * review's section 5; before it the hold named no node, an open item of milestone M3).</p>
 */
class WaterIntoCryogenicMethaneTest {
    private static final String PILOT = "createcheme:pilot_cryogenic";
    private static final long VESSEL = 11;
    private static final int WATER = 4;
    private final FluidThermodynamics model = FluidTestSupport.model(MaterialCatalog.bundled(), PILOT);

    /** One cubic metre of {@code feed} (basis N2, CH4, C2H6, CO2, water) at (T, P), the engine's TP answer. */
    private FluidThermodynamics.State metre(double t, double p, double[] feed) {
        var probe = model.flashTP(t, p, feed, () -> {});
        double scale = 1.0 / probe.volume();
        double[] amounts = feed.clone();
        for (int i = 0; i < amounts.length; i++) amounts[i] *= scale;
        return model.flashTP(t, p, amounts, () -> {});
    }

    private PassiveNetwork island(double methaneTemperature, double methanePressure, double waterPressure) {
        var pipe = new PipeResistance.Geometry(1.0, 0.05, 4.5e-5, 0.0);
        var nitrogen = metre(298.15, PhaseLayout.AMBIENT_PRESSURE, new double[] {1, 0, 0, 0, 0});
        return new PassiveNetwork(List.of(
                new PassiveNetwork.Reservoir(7, 0, metre(methaneTemperature, methanePressure, new double[] {0, 1, 0, 0, 0}), PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(VESSEL, 0, nitrogen),
                new PassiveNetwork.Reservoir(15, 0, metre(298.15, waterPressure, new double[] {0, 0, 0, 0, 1}), PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(34, 0, nitrogen, PassiveNetwork.NodeKind.VOID)),
                List.of(new PassiveNetwork.Pipe(1, 0, 1, pipe), new PassiveNetwork.Pipe(2, 1, 3, pipe), new PassiveNetwork.Pipe(3, 1, 2, pipe)));
    }

    /** Four 5 s intervals commit; the vessel stands at or above the ambient and holds no water. */
    private void commitsWithNoWaterEntering(double methaneTemperature, double methanePressure) {
        var solver = new PassiveIntervalSolver(model);
        var graph = island(methaneTemperature, methanePressure, PhaseLayout.AMBIENT_PRESSURE);
        double step = 0.05;
        for (int k = 0; k < 4; k++) {
            var result = solver.solve(graph, 5, PassiveIntervalSolver.Settings.defaults(), () -> {}, step);
            step = solver.nextStepEstimate();
            graph = result.graph();
            var vessel = graph.reservoirs().get(1);
            System.out.printf(Locale.ROOT, "methane at %.0f K, %.0f Pa, interval %d: vessel %.3f K %.1f Pa, liquid %.4e m3, water %.3e mol (%d substeps)%n", methaneTemperature,
                    methanePressure, k + 1, vessel.state().temperature(), vessel.state().pressure(), vessel.state().liquidVolume(), vessel.inventory().moles()[WATER],
                    result.acceptedSubsteps());
            assertTrue(vessel.state().pressure() >= PhaseLayout.AMBIENT_PRESSURE, "the wall goes slack at the ambient, never below it");
            assertEquals(0, vessel.inventory().moles()[WATER], "no water enters");
        }
    }

    @Test
    void waterBesideLiquidMethaneAt120KDoesNotEnterTheVessel() {
        commitsWithNoWaterEntering(120, 1.2e6);
    }

    @Test
    void waterBesideLiquidMethaneAt115KDoesNotEnterTheVessel() {
        commitsWithNoWaterEntering(115, 2.0e5);
    }

    /** Water driven in at 1 MPa while the methane cools the vessel below 273.16 K: the interim ice hold, naming the vessel. */
    @Test
    void waterDrivenIntoTheCooledVesselHoldsOnTheInterimIceHold() {
        var solver = new PassiveIntervalSolver(model);
        RuntimeException refused = assertThrows(RuntimeException.class, () -> {
            var graph = island(120, 1.2e6, 1e6);
            for (int k = 0; k < 4; k++) graph = solver.solve(graph, 5, PassiveIntervalSolver.Settings.defaults(), () -> {}, 0.05).graph();
        });
        System.out.println("water at 1 MPa: " + refused.getMessage());
        assertFalse(refused.getMessage().contains("Negative/nonfinite inventory"), "no untyped inventory refusal: " + refused.getMessage());
        var hold = assertInstanceOf(FluidThermodynamics.UnsupportedPhases.class, SparseNewton.domainViolation(refused), "a typed phase hold: " + refused.getMessage());
        assertEquals(FluidThermodynamics.UnsupportedPhases.Kind.ICE, hold.kind(), hold.getMessage());
        assertTrue(hold.temperature() < 273.16, hold.getMessage());
        assertEquals(VESSEL, hold.node(), "the hold names the vessel: " + hold.getMessage());
    }
}
