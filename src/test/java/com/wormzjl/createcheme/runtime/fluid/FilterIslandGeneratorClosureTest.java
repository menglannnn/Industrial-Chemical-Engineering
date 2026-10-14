package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Hold class E1 of the placement-sequence fuzz (phase-ports batch, PHASE_PORTS_REVIEW.md "Hold class E1: the
 * interval-start rate pass (2026-09-28)"): an island with an inline filter runs a rate solve of its port graph at every
 * interval's start, and its failure holds the island. The cold-start closure of receiving generators (decision D12) left
 * filter islands out, so a generator that can only receive stayed a two-way boundary in that rate solve. The smallest
 * world of fuzz seed 6 with the world's placement defaults: a 1 atm water generator three blocks below a junction pipe
 * that joins two nitrogen voids at 1 atm and an inline filter facing down with nothing past it, every block in one tick.
 * It held at its first slice on "Newton line search stalled"; it must keep up. Rebuilt for decisions D18-D20 (2026-09-28): the
 * filter led nowhere and is now given a pipe and a void past it, and a 130 kPa water generator feeds the junction (without
 * it the island is at rest and never solved, decision D19); without the filter in the cold closure (bbb79790) it holds on
 * the interval-start rate pass again.
 */
class FilterIslandGeneratorClosureTest {
    @Test void aWaterGeneratorBelowAFilterIslandsJunctionDoesNotHoldTheRatePass() {
        var w=new EventPathWorld();w.runTo(662);var passive=new FlowControl.Passive();
        var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
        w.place(0,0,-2,Kind.VOID,NORTH,passive,nitrogen,.05);w.place(0,0,-1,Kind.PIPE,NORTH,.05);w.place(0,1,1,Kind.PIPE,NORTH,.05);
        w.place(-1,-3,1,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325),.05);
        w.place(-1,0,1,Kind.PIPE,NORTH,.05);w.place(-1,-1,1,Kind.PIPE,NORTH,.05);w.place(0,2,1,Kind.FILTER,DOWN,.05);
        w.place(0,0,0,Kind.VOID,NORTH,passive,nitrogen,.05);w.place(-1,-2,1,Kind.PIPE,NORTH,.05);w.place(0,0,1,Kind.PIPE,NORTH,.05);
        w.place(-1,0,0,Kind.PIPE,NORTH,.05);
        // Rebuilt for decisions D18-D20: the filter leads on to a void (a pipe and a void above it), and a 130 kPa water
        // generator feeds the junction through one pipe block, so the island flows and is solved.
        w.place(0,3,1,Kind.PIPE,NORTH,.05);w.place(0,4,1,Kind.VOID,NORTH,passive,nitrogen,.05);
        w.place(1,0,1,Kind.PIPE,NORTH,.05);w.place(2,0,1,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,130000),.05);
        w.runTo(4700);w.assertKeptUp("a water generator below a filter island's junction");
        long filter=w.registry.at(new PhysicalFluidTopology.Position(EventPathWorld.DIM,0,2,1)).orElseThrow().device().id();
        org.junit.jupiter.api.Assertions.assertTrue(w.registry.members(w.registry.owner(filter)).size()>1,"the filter is on a solved run, not dormant");
    }
}
