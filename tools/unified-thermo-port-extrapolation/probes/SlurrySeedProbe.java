package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** Probe (range-extrapolation batch): SlurryTankPumpTest's island before the mint, its supplier seeds (round 0) and the
 * fixed point's start, node by node, to see where the suction junction's start comes from. */
class SlurrySeedProbe {
    @Test
    void seeds() throws Exception {
        var test = new SlurryTankPumpTest();
        var pilot = test.pilot;
        var devices = List.of(SlurryTankPumpTest.device(SlurryTankPumpTest.VENT, 0, 3, 0, Kind.VOID), SlurryTankPumpTest.device(2, 0, 2, 0, Kind.PIPE),
                SlurryTankPumpTest.device(SlurryTankPumpTest.TANK, 0, 1, 0, Kind.RESERVOIR), SlurryTankPumpTest.device(4, 0, 0, 0, Kind.PIPE),
                SlurryTankPumpTest.device(5, 0, 0, 1, Kind.PIPE), SlurryTankPumpTest.device(6, 0, 0, 2, Kind.PIPE),
                SlurryTankPumpTest.device(SlurryTankPumpTest.PUMP, 0, 0, 3, Kind.PUMP, SOUTH, new FlowControl.Pump(.005, 500000, 1)),
                SlurryTankPumpTest.device(9, 0, 0, 4, Kind.PIPE), SlurryTankPumpTest.device(10, 0, 0, 5, Kind.VOID));
        double[] air = new double[pilot.componentCount()]; air[0] = 1;
        var boundaries = new LinkedHashMap<Long, PassiveNetwork.Reservoir>();
        boundaries.put(SlurryTankPumpTest.TANK, test.slurryTank());
        for (var d : devices) if (d.kind() == Kind.VOID) boundaries.put(d.id(), new FluidDeviceSpec(1, 298.15, 101325, air).initialize(d, pilot, () -> {}));
        var compiled = PhysicalFluidTopology.compile(devices, boundaries, Map.of(), NetworkReduction.ambient(pilot));
        var graph = compiled.islands().stream().filter(i -> i.physicalIds().size() == devices.size()).findFirst().orElseThrow().graph();
        print("compiled", graph.reservoirs().stream().map(PassiveNetwork.Reservoir::state).toList(), graph);
        for (var p : graph.pipes()) System.out.println("PROBE pipe " + p.first() + "->" + p.second() + " " + p.control().getClass().getSimpleName() + " ports " + p.firstPort() + "/" + p.secondPort());
        var solver = new PassiveStepSolver(pilot);
        Method supplier = PassiveStepSolver.class.getDeclaredMethod("supplierSeeds", PassiveNetwork.class, Runnable.class);
        supplier.setAccessible(true);
        @SuppressWarnings("unchecked") var round0 = (List<FluidThermodynamics.State>) supplier.invoke(solver, graph, (Runnable) () -> {});
        print("round0", round0, graph);
        Method fixed = PassiveStepSolver.class.getDeclaredMethod("fixedPointStarts", PassiveNetwork.class);
        fixed.setAccessible(true);
        @SuppressWarnings("unchecked") var starts = (List<FluidThermodynamics.State>) fixed.invoke(new PassiveStepSolver(pilot), graph);
        print("fixed", starts, graph);
    }

    static void print(String label, List<FluidThermodynamics.State> states, PassiveNetwork graph) {
        for (int i = 0; i < states.size(); i++) {
            var s = states.get(i); var node = graph.reservoirs().get(i);
            if (s == null) { System.out.println("PROBE " + label + " node " + i + " id " + node.id() + " " + node.kind() + ": null"); continue; }
            double[] liquid = s.liquidView(), vapor = s.vaporView();
            System.out.printf(Locale.ROOT, "PROBE %s node %d id %d %s z %.1f: %.3f K %.1f Pa rho %.1f liquidV %.3e vapourV %.3e  liq %s vap %s solids %.3f kg%n",
                    label, i, node.id(), node.kind(), node.elevation(), s.temperature(), s.pressure(), s.mass() / s.volume(), s.liquidVolume(), s.vaporVolume(),
                    Arrays.toString(liquid), Arrays.toString(vapor), s.solidMoments().mass());
        }
    }
}
