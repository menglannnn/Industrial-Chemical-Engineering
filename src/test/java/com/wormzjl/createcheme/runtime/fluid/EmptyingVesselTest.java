package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * A vessel that runs dry within a step (decision D54, A313) is solved as a junction holding its stock, from the step's
 * own start: the same carried flows as every other pass of the step (PHASE_PORTS_REVIEW.md "Active-set cycles and the
 * flush family (2026-10-14)", B1).
 */
class EmptyingVesselTest {
    /**
     * Random fuzz seed 257 minimized to 7 blocks (tools/phase-ports-probes/hold-classes logs cf10). A 384 kPa, 350 K water
     * generator pushes through a 1 cm pipe into a junction that feeds an empty 1 m3 tank's top and, round a loop, a
     * junction at the tank's side, which drains 1 m down into a 1 atm void. Physics: the 1 cm pipe passes about 1.4 kg/s;
     * the tank's side outlet, on a 1 m drain to the ambient, can carry ten times what reaches the tank, so the tank holds
     * nothing: water passes through it into the void. Each time it has drained its last grams it runs dry within the step.
     * The emptying pass stands it as a junction; its connections' ports turn to the bulk, which changed their identity, so
     * the pass started from the static estimate instead of the step's carried flows, read the void's air as the drain's
     * column, converged with the tank above the ambient and released it; the vessel form has no root, and the pass sequence
     * cycled at every step size: with that start it held at tick 154 on "Phase/device active-set cycle" (logs cf10). From
     * the carried flows it runs 120 slices.
     */
    @Test void aTankThatWaterPassesThroughRunsDryAtEachStep() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(151);
        w.place(0,-1,-1,Kind.PIPE,NORTH,.01);
        w.place(0,0,-1,Kind.PIPE,NORTH,.05);
        w.place(0,0,0,Kind.GENERATOR,SOUTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,350.1538205969658,384088.6831723288),.02);
        w.place(0,-3,-2,Kind.VOID,NORTH,.05);
        w.place(0,-2,-1,Kind.RESERVOIR,NORTH,.01);
        w.place(0,-2,-2,Kind.PIPE,NORTH,.05);
        w.place(0,-1,-2,Kind.PIPE,NORTH,.05);
        w.runTo(151+120*100);w.assertKeptUp("a tank that water passes through");
    }
}
