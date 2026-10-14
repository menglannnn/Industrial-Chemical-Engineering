package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * The step starts from the committed flows (phase-ports batch, PHASE_PORTS_REVIEW.md "The step starts from the committed
 * flows; forbidden directions closed on first solves (2026-10-13)", card E6-S): flows are continuous in time, so an island
 * job's first step starts from the flows its committed interval ended with (PassiveIntervalSolver.Result.endpointFlows),
 * as every later step starts from the step before, and the start estimate states its column as the flow row does.
 */
class StepStartFlowsTest {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);

    /**
     * Card E6-S's world (world-default fuzz seed 786, minimized to 8 blocks keeping the group, tools/phase-ports-probes/
     * hold-classes logs y18 and sf09). A 1 atm water generator feeds a junction beside it that drains 1 m down into a void;
     * both rise 1 m to a junction above, which the last pipe joins to a nitrogen tank. The tank vents through the lower
     * junction into the void and stands near 90.9 kPa, so the generator lifts water up its riser (10.4 kPa of drive against
     * 9.8 kPa of column) and it falls back down the other: about 4.8 kg/s circulating, 15.9 kg/s into the void, the tank
     * taking a trickle. With the committed flows carried into every job the world runs 120 slices. With the old job start
     * (the static per-connection estimate, its own column rule) it held at tick 3210 on "Newton iteration limit at
     * residual 0.0789" at every step size: every job restarted the circulation from the estimate.
     */
    @Test void aCirculationBetweenTwoRisersContinuesFromItsCommittedFlows() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(158);w.place(0,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(473);w.place(-3,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(504);w.place(-1,-1,2,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.runTo(553);w.place(0,-1,2,Kind.PIPE,NORTH,.05);
        w.runTo(625);w.place(-1,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(643);w.place(0,-2,2,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.runTo(694);w.place(-3,0,1,Kind.RESERVOIR,NORTH,passive,NITROGEN,.05);
        w.runTo(1283);w.place(-2,0,2,Kind.PIPE,NORTH,.05);
        w.runTo(1283+120*100);w.assertKeptUp("a circulation between two risers");
    }
}
