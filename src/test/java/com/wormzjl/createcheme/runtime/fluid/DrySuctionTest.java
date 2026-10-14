package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.network.InletPhase;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.PhasePort;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D58, case A1 of the phase-ports batch (REMAINING_HOLDS.md, "Remaining issues sorted for the owner"): a pump
 * on a dry suction. In real life a pump with nothing at its suction runs dry: no flow, no head. The suction is refused like
 * a wrong phase (decision D6), with its own reason, decided per slice; a refused pump is closed for the solve.
 */
class DrySuctionTest {
    /**
     * The smallest world of world-defaults fuzz seed 922 (6 blocks, all placed at tick 100; minimized in
     * tools/phase-ports-probes/hold-classes logs rc02). A 1 atm water generator, two pipes, a pump facing the generator, a
     * pipe and a placed tank. The pump's suction is the empty tank and its discharge the generator, which only supplies:
     * nothing can move. The first slice refused the pump on the empty tank's ambient (read as gas), the junction's buffer
     * left 2.2e-17 kg of water in the tank, and from the next slice the pump read water and stood on its head limit against
     * a dry end: the step stalled at 500000 / 101325 = 4.934616 at every step size and the island held at tick 103. With
     * the dry rule the tank (below 0.5 % of its capacity) leaves the pump refused, and the island keeps up for 120 slices.
     */
    @Test void aPumpOnAnEmptyTankIsRefusedAsDry() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(0,0,0,Kind.PUMP,NORTH,new FlowControl.Pump(.01,500000,1),null,.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325),.05);
        long tank=w.place(0,0,2,Kind.RESERVOIR,NORTH,.05);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(100+120*100);w.assertKeptUp("a pump on an empty tank");
        var island=w.coordinator.snapshots().stream().filter(s->s.graph().reservoirs().stream().anyMatch(n->n.id()==tank)).findFirst().orElseThrow();
        var graph=island.graph();var result=island.lastResult().orElseThrow();
        int pump=-1;for(int e=0;e<graph.pipes().size();e++)if(graph.pipes().get(e).control() instanceof FlowControl.Pump)pump=e;
        String reason=InletPhase.reason(w.model,graph,pump,5);
        System.out.println("DRY_SUCTION modes="+result.endpointModes()+" flows="+Arrays.toString(result.averageMassFlows())+" reason="+reason);
        assertEquals(FlowControl.Mode.INLET_WRONG_PHASE,result.endpointModes().get(pump),"the pump is refused");
        // With the pump refused nothing can move, so the island rests (A3): not even a roundoff flow from the junction's buffer.
        for(double q:result.averageMassFlows())assertEquals(0,q,"nothing moves");
        assertTrue(reason.startsWith("ERROR: pump inlet dry (from node "),"its reason is the dry suction: "+reason);
    }

    /** The dry decision on the measure alone: refused while what feeds the suction holds below 0.5 % of its capacity; once
     * refused, until it holds 1 %; a supply whose feed is not measured is never dry. */
    @Test void theDryThresholdsAndReason() {
        var pump=new FlowControl.Pump(.001,500000,1);var compressor=new FlowControl.Compressor(.05,3,1);
        for(double[] c:new double[][]{{.0049,0,1},{.0051,0,0},{.0051,1,1},{.0099,1,1},{.01,1,0},{0,0,1},{Double.POSITIVE_INFINITY,1,0}}) {
            var supply=new InletPhase.Supply(0,7,PhasePort.BULK,0,0,0,c[0]);
            assertEquals(c[2]==1,pump.refuses(supply,c[1]==1),Arrays.toString(c));
            assertEquals(c[2]==1,new InletPhase.Supply(0,7,PhasePort.BULK,1,0,0,c[0]).dry(c[1]==1),Arrays.toString(c));
        }
        assertFalse(new InletPhase.Supply(0,7,PhasePort.BULK,0,0,0).dry(true),"no measured feed: never dry");
        assertEquals("ERROR: pump inlet dry (from node 7)",InletPhase.reason(pump,new InletPhase.Supply(0,7,PhasePort.BULK,1,0,0,0)));
        assertEquals("ERROR: compressor inlet dry (from node 7)",InletPhase.reason(compressor,new InletPhase.Supply(0,7,PhasePort.BULK,0,1,0,.002)));
    }
}
