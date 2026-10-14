package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Hold class E6 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E6, option (a)
 * implemented (2026-10-12)", owner decision D35): a plain passive connection's hydraulic row is divided by a scale taken from
 * the velocity-cap drops of its ends, and the scale must not depend on which end the fluid would come from, since at a
 * zero-flow root neither end does. Taken from the donor picked by the driving pressure's sign, the scale jumped by orders at
 * that root wherever the two ends held different fluids, every differenced Jacobian straddled the jump and the step Newton
 * stopped at its iteration limit at every step size.
 *
 * <p>Both worlds were rebuilt on the code with the junction start rule (decision D37; review "Integration of R1 and the
 * junction start rule (2026-10-12)"), which removed the nitrogen-against-water starts the first two worlds were built on:
 * world-default fuzz seeds that hold in E6 with the donor's scale restored and run clean with the scale from both ends,
 * minimized with the donor's scale (tools/phase-ports-probes/hold-classes logs k05). Both are world-default blocks (0.05 m
 * pipes, 1 atm water generators at 298.15 K, 1 atm nitrogen voids) placed through the event path as the fuzz placed them.
 */
class ConnectionRowScaleTest {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),WATER=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);

    /**
     * The smallest world of world-default fuzz seed 724 (4 blocks, all placed at tick 110): two 1 atm water generators level
     * with a pipe between them, and a 1 atm nitrogen void one block below that pipe, which becomes their junction. Water
     * falls through the junction into the void, driven by its 1 m column (9.8 kPa); the two generators feed it equally and
     * the island rests at 13.94 kg/s (6.97 kg/s each), the junction at 99.32 kPa. With the donor's scale the first interval
     * held at tick 113 on "Newton iteration limit at residual 1.13e-8" at every step size (the full seed held the same way).
     */
    @Test void twoLevelWaterGeneratorsDrainingIntoAVoidBelowKeepUp() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(110);
        w.place(1,0,0,Kind.PIPE,NORTH,.05);w.place(2,0,0,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(1,-1,0,Kind.VOID,NORTH,passive,NITROGEN,.05);w.place(0,0,0,Kind.GENERATOR,EAST,passive,WATER,.05);
        w.runTo(110+120*100);w.assertKeptUp("two level water generators draining into a void below");
    }

    /**
     * The smallest world of world-default fuzz seed 247 (5 blocks, all placed at tick 140): a 1 atm water generator two
     * pipe blocks from a junction that has a 1 atm nitrogen void one block above and one block below. Water drains into the
     * lower void (9.12 kg/s at rest, the junction at 94.0 kPa, pulled below the generator by the drain's column); the upper
     * void only receives (decision D21) and stands above the junction, so its connection closes. With the donor's scale the
     * island held at tick 203 on "Newton iteration limit at residual 0.357" at every step size (the full seed's class).
     */
    @Test void aWaterGeneratorDrainingBetweenAVoidAboveAndAVoidBelowKeepsUp() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(140);
        w.place(2,1,0,Kind.PIPE,NORTH,.05);w.place(2,1,-1,Kind.PIPE,NORTH,.05);w.place(2,1,-2,Kind.GENERATOR,NORTH,passive,WATER,.05);
        w.place(2,2,0,Kind.VOID,NORTH,passive,NITROGEN,.05);w.place(2,0,0,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.runTo(140+120*100);w.assertKeptUp("a water generator draining between a void above and a void below");
    }
}
