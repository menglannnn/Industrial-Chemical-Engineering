package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Hold class E2 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E2: a singular step
 * at every step size (2026-09-28)"): the cold-start closure of receiving generators (decision D12) left islands with an
 * actuator out, so a generator below its junctions' pressure stayed a two-way boundary in the cold rate seed, the steps
 * started from that seed's failure, and the island ended on a singular step at every step size (a two-phase crude
 * junction whose phase split the Newton could not determine). The smallest world of fuzz seed 4: a 472 kPa methane
 * generator and a 261 kPa two-phase crude generator feeding a junction network with two pressure control valves (120 and
 * 200 kPa) and a void, every block in one tick. It must keep up. Rebuilt for decisions D18-D20 (2026-09-28): a pipe block
 * between the crude generator and the 200 kPa valve (they touched), and a pipe and a void below the 120 kPa valve (it
 * faced a dead end), so both valves are on valid runs; without the closure in actuated islands (a3d3a789) it holds.
 */
class ActuatedIslandGeneratorClosureTest {
    @Test void aLowCrudeGeneratorInAValveIslandDoesNotHoldTheSteps() {
        var catalog=EventPathWorld.CATALOG;int components=FluidDeviceSpec.nitrogen(catalog).composition().length;
        double[] crude=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),components);
        int methane=catalog.requirePackage("createcheme:tjl20_methane_nitrogen").components().indexOf("Methane");
        var w=new EventPathWorld();w.runTo(105);var passive=new FlowControl.Passive();
        w.place(-1,-2,1,Kind.PIPE,NORTH,.05);w.place(2,0,0,Kind.PIPE,NORTH,.05);w.place(-1,-1,1,Kind.PIPE,NORTH,.02);w.place(-1,-2,3,Kind.PIPE,NORTH,.05);
        w.place(0,-2,3,Kind.PIPE,NORTH,.02);w.place(1,-2,3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(methane,352.1261664904419,472036.05064761586),.05);
        w.place(-1,-3,0,Kind.VALVE,DOWN,new FlowControl.PressureValve(120000),null,.02);w.place(-1,-2,0,Kind.PIPE,NORTH,.02);
        w.place(-1,-4,0,Kind.PIPE,NORTH,.02);w.place(-1,-5,0,Kind.VOID,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),.02);
        w.place(0,0,0,Kind.PIPE,NORTH,.05);w.place(1,0,0,Kind.VALVE,EAST,new FlowControl.PressureValve(200000),null,.05);
        w.place(-1,0,0,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,368.8747936542612,261354.27900741925,crude),.02);
        w.place(-1,-2,2,Kind.PIPE,NORTH,.05);w.place(3,0,0,Kind.PIPE,NORTH,.05);
        w.place(4,0,0,Kind.VOID,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),.05);
        w.place(-1,-1,0,Kind.PIPE,NORTH,.02);w.place(-1,0,1,Kind.PIPE,NORTH,.02);
        w.runTo(4200);w.assertKeptUp("a low crude generator in a valve island");
    }
}
