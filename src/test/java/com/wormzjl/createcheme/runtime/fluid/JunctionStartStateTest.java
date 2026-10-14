package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Hold class E4 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E4, the junction start
 * from reachability (2026-10-12)", owner decision D37): a junction of an island formed from placed blocks starts as the stream
 * of the nearest boundary that can drive fluid into it, or, when none can, as the fluid standing highest among the boundaries
 * in contact with it (the start chain, retired by owner decision D54: a minted junction now starts from the island's fixed
 * point, PassiveNetwork.mintJunctions; the world stays as a regression). It used to start as the island's first boundary by identity, which
 * could be a void's nitrogen at a junction only water ever reaches; the step's material bookkeeping then failed its equation
 * gate at every step size.
 */
class JunctionStartStateTest {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);

    /**
     * The smallest world of world-default fuzz seed 438 with the start rule reverted (6 blocks, all placed at tick 510;
     * minimized in tools/phase-ports-probes/hold-classes logs i17). A 1 atm water generator feeds, through two pipe blocks
     * level with it, a junction with one void a block below it and another beside it at its own height. Water falls the 1 m
     * into the lower void and the island settles at a steady spill of about 6.3 kg/s. Nothing is driven into the junction by
     * the static test at the start (the generator stands exactly at the voids' pressure at its height), so it holds the
     * fluid standing highest in contact with it: the generator's water and the side void's nitrogen stand equal there, and the
     * denser water settles in. Started as the island's first boundary, the lower void's nitrogen, the island held at tick 573
     * on "Conservative reconstruction fails equation gate: 1.16e-4" at every step size; with the rule it keeps up for 120
     * slices.
     */
    @Test void aJunctionOnlyWaterReachesStartsAsWater() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(510);
        w.place(0,1,0,Kind.PIPE,NORTH,.05);w.place(0,0,0,Kind.VOID,NORTH,passive,NITROGEN,.05);w.place(0,1,2,Kind.PIPE,NORTH,.05);
        w.place(0,1,3,Kind.GENERATOR,NORTH,passive,WATER,.05);w.place(0,1,1,Kind.PIPE,NORTH,.05);w.place(1,1,0,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.runTo(510+120*100);w.assertKeptUp("a junction only water reaches");
    }
}
