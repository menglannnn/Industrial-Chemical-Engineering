package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * WP4 probe (tools/unified-thermo-port-ice/probes, not a tracked test): the network ice gate's island (WaterIntoCryogenicMethaneTest)
 * stepped in short intervals, printing the vessel after each, to see where a step stalls. Run with EXTRA_TESTS.
 */
class IceGateProbe {
    private static final String PILOT = "createcheme:pilot_cryogenic";
    private final FluidThermodynamics model = FluidTestSupport.model(MaterialCatalog.bundled(), PILOT);

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
                new PassiveNetwork.Reservoir(11, 0, nitrogen),
                new PassiveNetwork.Reservoir(15, 0, metre(298.15, waterPressure, new double[] {0, 0, 0, 0, 1}), PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(34, 0, nitrogen, PassiveNetwork.NodeKind.VOID)),
                List.of(new PassiveNetwork.Pipe(1, 0, 1, pipe), new PassiveNetwork.Pipe(2, 1, 3, pipe), new PassiveNetwork.Pipe(3, 1, 2, pipe)));
    }

    static String describe(FluidThermodynamics.State s) {
        return String.format(Locale.ROOT, "%.4f K %.1f Pa L[%s] V[%s] W %.4e S %.4e pc %.2f pw %.4e Vl %.3e Vw %.3e Vg %.3e solids %s",
                s.temperature(), s.pressure(), arr(s.liquidView()), arr(s.vaporView()), s.waterLiquid(), s.waterVapor(), s.hydrocarbonPartialPressure(),
                s.waterPartialPressure(), s.liquidVolume(), s.waterVolume(), s.vaporVolume(), s.solids().populations());
    }

    static String arr(double[] a) {
        var b = new StringBuilder();
        for (double x : a) b.append(String.format(Locale.ROOT, "%.3e ", x));
        return b.toString().trim();
    }

    @Test
    void stepThroughTheGate() {
        var solver = new PassiveIntervalSolver(model);
        var graph = island(120, 1.2e6, 1e6);
        double duration = Double.parseDouble(System.getProperty("probe.interval", "0.05"));
        int count = Integer.getInteger("probe.count", 200);
        double step = Math.min(0.05, duration);
        double time = 0;
        for (int k = 0; k < count; k++) {
            try {
                var result = solver.solve(graph, duration, PassiveIntervalSolver.Settings.defaults(), () -> {}, step);
                step = solver.nextStepEstimate();
                graph = result.graph();
                time += duration;
                var vessel = graph.reservoirs().get(1);
                System.out.printf(Locale.ROOT, "t=%.3f vessel %s | inv water %.4e | %d acc %d rej %s%n", time, describe(vessel.state()),
                        vessel.inventory().moles()[4], result.acceptedSubsteps(), result.rejectedSubsteps(), result.rejectionReasons());
            } catch (RuntimeException failed) {
                System.out.println("FAILED at t=" + time + ": " + failed);
                var vessel = graph.reservoirs().get(1);
                System.out.println("last vessel " + describe(vessel.state()));
                for (var node : graph.reservoirs()) System.out.println("  node " + node.id() + " " + describe(node.state()));
                throw failed;
            }
        }
    }
}
