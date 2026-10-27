package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

/** S3 probe: CrystalFlushBudgetTest's row E (a vessel freezing CO2-I beside liquid methane, the solid event path), six
 * 5 s intervals, every committed double printed as raw bits, so two trees compare bit for bit. */
class S3RowEProbe {
    private final FluidThermodynamics model = FluidTestSupport.model(MaterialCatalog.bundled(), "createcheme:pilot_cryogenic");
    private FluidThermodynamics.State metre(double t, double p, double[] feed) {
        var probe = model.flashTP(t, p, feed, () -> {});
        double[] amounts = feed.clone();
        for (int i = 0; i < amounts.length; i++) amounts[i] /= probe.volume();
        return model.flashTP(t, p, amounts, () -> {});
    }
    private static String bits(double v){return Long.toHexString(Double.doubleToRawLongBits(v==0?0:v));}
    @Test void rowE() {
        var pipe = new PipeResistance.Geometry(1.0, 0.05, 4.5e-5, 0.0);
        var nitrogen = metre(298.15, 101325, new double[] {1, 0, 0, 0, 0});
        var graph = new PassiveNetwork(List.of(
                new PassiveNetwork.Reservoir(63, 0, metre(115, 2e5, new double[] {0, 1, 0, 0, 0}), PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(65, 0, metre(250, 2.5e5, new double[] {0, 0, 0, 1, 0}), PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(71, 0, nitrogen),
                new PassiveNetwork.Reservoir(75, 0, nitrogen, PassiveNetwork.NodeKind.VOID)),
                List.of(new PassiveNetwork.Pipe(1, 0, 2, pipe), new PassiveNetwork.Pipe(2, 1, 2, pipe), new PassiveNetwork.Pipe(3, 2, 3, pipe)));
        var solver = new PassiveIntervalSolver(model);
        double step = 0.05;
        for (int k = 0; k < 6; k++) {
            var result = solver.solve(graph, 5, PassiveIntervalSolver.Settings.defaults(), () -> {}, step);
            step = solver.nextStepEstimate(); graph = result.graph();
            var line = new StringBuilder("S3_ROWE interval " + (k + 1) + " accepted=" + result.acceptedSubsteps() + " rejected=" + result.rejectedSubsteps() + " step=" + bits(step));
            for (var node : graph.reservoirs()) {
                var s = node.state();
                line.append(" | ").append(node.id());
                for (double v : new double[] {s.temperature(), s.pressure(), s.mass(), s.volume(), s.liquidVolume(), s.waterVolume(), s.vaporVolume(), node.inventory().internalEnergy(), node.inventory().solids().massKg()}) line.append(' ').append(bits(v));
                for (double n : node.inventory().moles()) line.append(' ').append(bits(n));
            }
            for (double f : result.averageMassFlows()) line.append(" f=").append(bits(f));
            for (var b : result.boundaries()) { line.append(" b=").append(b.nodeId()).append(':').append(bits(b.totalEnergyJoule())); for (double n : b.moles()) line.append(',').append(bits(n)); }
            System.out.println(line);
        }
    }
}
