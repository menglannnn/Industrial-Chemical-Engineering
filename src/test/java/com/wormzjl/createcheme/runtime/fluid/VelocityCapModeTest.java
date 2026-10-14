package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * The velocity cap is an active-set mode of the connection (phase-ports batch, PHASE_PORTS_REVIEW.md "The velocity cap as
 * an active-set mode (2026-10-13)", card E6-C): a line at its cap states the cap flow of the end it leaves, as a pump at its
 * target states its flow, and the pass loop decides when a line enters and leaves the mode.
 */
class VelocityCapModeTest {
    /**
     * Card E6-C's world (random fuzz seed 529, minimized to 7 blocks keeping the group, tools/phase-ports-probes/hold-classes
     * logs y19 and y25). A 496 kPa methane generator fills tank B (nitrogen, 441 kPa by tick 1157) through a junction and
     * two pipes; the last block joins tank B to tank A (nitrogen, 1 atm) through a 2 cm pipe. The generator drives 55 kPa
     * into tank B: the inflow runs at its cap (about 0.6 kg/s, a cap drop of about 3 kPa), the junction sits just above
     * tank B and its two outlets below their caps; tank B drives 340 kPa into tank A through the 2 cm pipe, capped at about
     * 0.09 kg/s. With the cap as a mode the world runs 120 slices. With the cap as a branch of the flow row it held at tick
     * 1159 on "Newton iteration limit at residual 0.65" at every step size: the junction's pressure jumped about 2e5 Pa
     * per iteration between its capped inflow and its outlets, which went on and off their caps.
     */
    @Test void aJunctionBetweenALineAtItsCapAndItsOutletsFindsItsPressure() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);w.place(1,-1,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(0,311.67397547863067,495884.91340665857),.1);
        w.runTo(113);w.place(0,-1,-2,Kind.PIPE,NORTH,.05);
        w.runTo(458);w.place(-2,0,-2,Kind.RESERVOIR,NORTH,.02);
        w.runTo(869);w.place(0,-1,-1,Kind.PIPE,NORTH,.05);w.place(0,0,-1,Kind.PIPE,NORTH,.05);
        w.runTo(1104);w.place(0,0,-2,Kind.RESERVOIR,NORTH,.05);
        w.runTo(1157);w.place(-1,0,-2,Kind.PIPE,NORTH,.02);
        w.runTo(1157+120*100);w.assertKeptUp("a junction between a capped inflow and its outlets");
    }
}
