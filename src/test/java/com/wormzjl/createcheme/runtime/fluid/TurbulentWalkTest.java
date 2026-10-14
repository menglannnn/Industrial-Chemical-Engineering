package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * The turbulent walk on gas lines (B2 of the phase-ports batch, decision A371; PHASE_PORTS_REVIEW.md "The turbulent walk
 * on gas lines (2026-10-14)"): a chord step reuses a Jacobian only while every connection's loss slope stays within a
 * factor two of the slope that Jacobian states.
 */
class TurbulentWalkTest {
    /**
     * Random fuzz seed 498 minimized to 9 blocks (tools/phase-ports-probes/hold-classes logs tw08), all placed at tick 100.
     * A 480 kPa nitrogen generator feeds, through two 1 cm pipes, a dead-end ladder of 1, 2 and 5 cm pipes: two loops of
     * junctions below it and nothing else. Physics: the ladder fills to the generator's pressure plus its gas column and
     * then nothing moves. The first solve starts its flows from the static estimate on the junctions' old pressures (about
     * 125 kPa against 480 kPa), so its lines start turbulent (0.01 to 0.03 kg/s, Re up to 2e5) and the root is near rest
     * (1e-4 kg/s, laminar). Newton halves a turbulent flow per fresh step; the chord steps between them kept the resistance
     * of flows up to twice as large and closed only a part of each line's gap (residual contractions 0.75 to 0.8, which the
     * solver read as progress), and the first step ran out of its 20 iterations at every step size: held at tick 101 on
     * "Newton iteration limit at residual 1.5497028030015203E-5" (logs tw09). With the Jacobian refreshed once a line's
     * slope has moved by more than a factor two it runs 120 slices.
     */
    @Test void aGeneratorFillingADeadEndLadderOfGasLinesSettlesWithinTheNewton() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(0,3,-3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,272.72073259613035,480371.272983223),.01);
        w.place(0,3,-2,Kind.PIPE,NORTH,.01);
        w.place(0,1,0,Kind.PIPE,NORTH,.01);
        w.place(0,2,-1,Kind.PIPE,NORTH,.02);
        w.place(0,0,-1,Kind.PIPE,NORTH,.02);
        w.place(0,1,-1,Kind.PIPE,NORTH,.02);
        w.place(0,3,-1,Kind.PIPE,NORTH,.01);
        w.place(0,0,0,Kind.PIPE,NORTH,.05);
        w.place(0,2,0,Kind.PIPE,NORTH,.01);
        w.runTo(100+120*100);w.assertKeptUp("a generator filling a dead-end ladder of gas lines");
    }
}
