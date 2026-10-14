package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Owner decision D59 (2), t28: a residue a junction's buffer leaves in an emptied tank is nothing (the bag's resolution,
 * 1e-12 of its capacity), so a tank holding it gives nothing.
 */
class ResidueTankTest {
    /**
     * The smallest world of world-defaults fuzz seed 322 (13 blocks, all placed at tick 498; minimized in
     * tools/phase-ports-probes/hold-classes logs cc40): two 1 atm water generators, two voids, pipes, and two placed tanks,
     * one 2 m above the other. The upper tank is left holding about 1e-71 kg of water by the junction buffers, so it was no
     * dry end: the line from it to the empty tank below stated a 2 m water column of that residue (19.5 kPa) with nothing to
     * move, and the step stalled at 4.2106 at every step size (held at tick 501). With the residue read as nothing the line
     * is a dry end's, closed for the solve, and the island keeps up for 120 slices.
     */
    @Test void aTankHoldingAResidueGivesNothing() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
        w.runTo(498);
        w.place(1,0,-1,Kind.RESERVOIR,NORTH,.05);
        w.place(0,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,2,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(2,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,0,0,Kind.VOID,NORTH,.05);
        w.place(0,0,-2,Kind.GENERATOR,NORTH,passive,water,.05);
        w.place(2,0,-2,Kind.GENERATOR,NORTH,passive,water,.05);
        w.place(1,3,-2,Kind.VOID,NORTH,.05);
        w.place(1,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,-2,-1,Kind.RESERVOIR,NORTH,.05);
        w.place(2,0,0,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(498+120*100);w.assertKeptUp("a tank holding a residue");
    }
}
