package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.FlowControl;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A gas bubble in a liquid-full junction is pushed through (owner decision D41, phase-ports batch, PHASE_PORTS_REVIEW.md "A
 * gas bubble in a liquid-full junction is pushed through (2026-10-13)"): the liquid carries it to the next gas space, and a
 * junction states no equilibrium of its own for it. Before, a junction full of water that a nitrogen trace reached kept the
 * trace as a gas phase with a water-vapour saturation row; the row read the ratio of two amounts far below what the
 * junction's rows resolve, and the step Newton never converged (hold class E6, group T).
 *
 * <p>The card world of group E6-T: world-default fuzz seed 844, minimized keeping the group (tools/phase-ports-probes/
 * hold-classes logs y16), world-default blocks placed through the event path at the fuzz's ticks.
 */
class CarriedGasTest {
    private static final FluidDeviceSpec WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);

    /**
     * A 1 atm water generator drains 3 m down through a junction into a 1 atm nitrogen void; a 1 m3 nitrogen tank at 1 atm
     * stands beside the junction. Water falls through the junction at about 17 kg/s (the 3 m column less the pipe losses),
     * the junction near 103.8 kPa and the tank beside it at the same pressure, at rest. Its connection exchanges nanograms
     * per second with the junction at rest, so a nitrogen trace reaches the water: in real life a bubble, carried down to
     * the void with the water. Before the rule the island held at tick 2067 on the junction's saturation row, "Newton
     * iteration limit at residual 8.76e-7" at every step size.
     */
    @Test void aWaterGeneratorDrainingPastANitrogenTankIntoAVoidKeepsUp() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(477);w.place(0,-2,0,Kind.PIPE,NORTH,.05);
        w.runTo(514);long tank=w.place(0,-1,-1,Kind.RESERVOIR,NORTH,.05);
        w.runTo(672);w.place(0,-3,0,Kind.VOID,NORTH,.05);
        w.runTo(694);w.place(0,-1,0,Kind.PIPE,NORTH,.05);
        w.runTo(1040);w.place(0,0,0,Kind.GENERATOR,WEST,passive,WATER,.05);
        w.runTo(1040+120*100);w.assertKeptUp("a water generator draining past a nitrogen tank into a void");
        var graph=w.coordinator.snapshots().stream().filter(s->s.graph().reservoirs().stream().anyMatch(n->n.id()==tank)).findFirst().orElseThrow().graph();
        double pressure=graph.reservoirs().stream().filter(n->n.id()==tank).findFirst().orElseThrow().state().pressure();
        System.out.println("CARRIED_GAS_CARD tankP="+pressure);
        assertTrue(pressure>101325&&pressure<101325+3*9.81*1000,"the tank stands at the junction's pressure, between the void's and the generator's head: "+pressure);
    }
}
