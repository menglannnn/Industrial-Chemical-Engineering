package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Hold class X of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class X: topology event
 * outside its fence (2026-09-27)"): {@code PhysicalRegistry.apply} threw "Topology event reached boundary/filter ... outside
 * the islands it fenced" on the server tick. Two ways an event's fence missed an island its component walk reaches, each
 * the smallest world of its fuzz seeds:
 * <ul>
 * <li>seed 28: a pipe placed beside a lone void while the void's island is mid-slice, the next pipe beside it a tick later.
 * The second event's walk stops at the first pipe (a queued event places it); when the first event applies, that pipe is a
 * dead end no island owns, so the second event was never fenced on the void's new island.</li>
 * <li>seed 234: two inline filters facing each other with no boundary (each in a dormant island of its own), then a tank
 * beside one of them. The tank's walk stopped at the first filter, whose island does not hold the second. Since only pipes
 * connect (decision D20) a pipe block stands between the filters and between the filter and the tank.</li>
 * </ul>
 */
class TopologyEventFenceTest {
    @Test void anEventBehindADeadEndPipeAQueuedEventPlacedIsFencedOnTheIslandBeyond() {
        var w=new EventPathWorld();
        // A tank a generator keeps filling, not the fuzz's lone void: since decision D17 a void stops an event's walk and is
        // fenced for no island, so it no longer holds the second event back; the flowing tank's island is mid-slice at 272.
        w.runTo(105);w.place(0,0,-3,Kind.GENERATOR,NORTH,new com.wormzjl.createcheme.science.fluid.network.FlowControl.Passive(),EventPathWorld.pure(com.wormzjl.createcheme.science.material.MaterialTestBasis.NETWORK,298.15,150000),.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,.05);w.place(0,0,-1,Kind.RESERVOIR,NORTH,.05);
        w.runTo(272);w.place(1,0,-1,Kind.PIPE,NORTH,.1);
        w.runTo(273);w.place(2,0,-1,Kind.PIPE,NORTH,.1);
        w.runTo(1000);w.assertKeptUp("pipes beside a tank a tick apart");
    }
    @Test void aWalkGoesOnThroughAFilterInADormantIslandOfItsOwn() {
        var w=new EventPathWorld();
        // A pipe and a void past the second filter (decisions D18-D20): the chain is a dead end, each filter dormant, until
        // the tank gives it its other end; the tank's event must reach the second filter through the first.
        w.runTo(517);w.place(4,0,0,Kind.VOID,NORTH,.05);w.place(3,0,0,Kind.PIPE,NORTH,.05);w.place(2,0,0,Kind.FILTER,WEST,.05);w.place(1,0,0,Kind.PIPE,NORTH,.05);
        w.runTo(718);w.place(0,0,0,Kind.FILTER,EAST,.05);w.place(-1,0,0,Kind.PIPE,NORTH,.05);
        w.runTo(776);w.place(-2,0,0,Kind.RESERVOIR,NORTH,.05);
        w.runTo(1200);w.assertKeptUp("a tank beside a chain of two dormant filters");
    }
}
