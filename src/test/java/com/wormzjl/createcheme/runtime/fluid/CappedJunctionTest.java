package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Owner decision D59 (5), the capped gas junctions (A276, A341), t28: a junction holds nothing, so its mass balance is what
 * places its pressure. Where every line of a junction (or of a group of junctions joined by lines on their law) is choked
 * and every cap enters it, nothing places that pressure; where every cap leaves it and nothing enters, its balance has no
 * root. In real life its pressure moves until the line whose drive exceeds its cap's pressure drop by the least leaves its
 * choke: that line is on its law for the pass.
 */
class CappedJunctionTest {
    /**
     * The smallest world of random fuzz seed 529 (7 blocks; minimized in tools/phase-ports-probes/hold-classes logs cc34): a
     * 496 kPa methane generator feeds pipes around a placed tank, and a pipe added at tick 162 closes a loop. The new
     * island's start puts the lines of a junction group on their caps with no way for the group to balance: without the rule
     * the first Jacobian is singular at every step size (held at tick 164); with it the island keeps up for 120 slices.
     */
    @Test void aChokedJunctionThatCannotBalanceKeepsOneLineOnItsLaw() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(1,-1,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(0,311.67397547863067,495884.91340665857),.1);
        w.place(0,-1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-2,Kind.RESERVOIR,NORTH,.05);
        w.place(0,0,0,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(162);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(162+120*100);w.assertKeptUp("a junction whose caps all enter it");
    }
}
