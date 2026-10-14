package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D17 (phase-ports batch, PHASE_PORTS_REVIEW.md "Boundary split, dead ends and rest (2026-09-28)"): a fixed
 * boundary (a generator or a void) has a state nothing in the network can change, so two parts of a network that touch only
 * through one are independent islands, solved, certified and held apart. The world is the smallest one of fuzz seed 33
 * (random distribution, hold class E6): a 262 kPa water generator two blocks below a void, piped straight into it, and from
 * the same void a run to a junction pipe that goes on to a second void and to a dead-end pump. As one island it held on the
 * step Newton's iteration limit at every step size (the junction between the two voids is a zero-flow root, and the
 * generator's flow shared its Newton); split, the generator's line and the voids' line keep up.
 */
class FixedBoundarySplitTest {
    private static final FlowControl PASSIVE=new FlowControl.Passive();

    private record Seed33(EventPathWorld world,long generator,long firstVoid,long secondVoid,long junction) {}
    /** The world of seed 33, every block placed at tick 150 in the fuzz's order. */
    private static Seed33 seed33() {
        var w=new EventPathWorld();w.runTo(150);var nitrogen=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
        long generator=w.place(3,-2,1,Kind.GENERATOR,NORTH,PASSIVE,EventPathWorld.pure(MaterialTestBasis.NETWORK,341.15146677619,262476.5208586075),.05);
        w.place(0,0,1,Kind.PIPE,NORTH,.02);long secondVoid=w.place(0,0,7,Kind.VOID,NORTH,PASSIVE,nitrogen,.01);w.place(2,0,1,Kind.PIPE,NORTH,.05);
        w.place(0,0,5,Kind.PIPE,NORTH,.01);w.place(0,0,6,Kind.PIPE,NORTH,.01);w.place(0,0,3,Kind.PIPE,NORTH,.02);w.place(0,0,2,Kind.PIPE,NORTH,.02);
        long firstVoid=w.place(3,0,1,Kind.VOID,NORTH,PASSIVE,nitrogen,.05);w.place(-1,0,4,Kind.PIPE,NORTH,.1);w.place(1,0,1,Kind.PIPE,NORTH,.05);
        long junction=w.place(0,0,4,Kind.PIPE,NORTH,.02);w.place(3,-1,1,Kind.PIPE,NORTH,.05);w.place(-2,0,4,Kind.PUMP,WEST,new FlowControl.Pump(.01,500000,1),null,.1);
        return new Seed33(w,generator,firstVoid,secondVoid,junction);
    }
    private static Set<Long> islandsHolding(EventPathWorld w,long device) {
        var ids=new TreeSet<Long>();for(var s:w.coordinator.observe())if(w.registry.members(s.id()).contains(device))ids.add(s.id());return ids;
    }

    @Test void aVoidBetweenAGeneratorsLineAndAVoidToVoidLineSeparatesThem() {
        var s=seed33();var w=s.world();w.runTo(152);
        long line=w.registry.owner(s.generator()),voids=w.registry.owner(s.junction());
        assertNotEquals(line,voids,"the generator's line and the voids' line are separate islands");
        assertEquals(Set.of(line,voids),islandsHolding(w,s.firstVoid()),"the void they meet at belongs to both");
        assertFalse(w.registry.members(voids).contains(s.generator()),"the voids' line does not hold the generator");
        assertEquals(3,w.registry.members(line).size(),"the generator's line is the generator, its pipe and the void");
        assertEquals(Math.min(line,voids),w.registry.owner(s.firstVoid()),"a shared void's owner is the lowest of its islands");
        w.runTo(4200);w.assertKeptUp("seed 33 split at its void");
    }

    @Test void anEditBesideASharedVoidLeavesTheOtherIslandAndAnEditOfTheVoidReplacesBoth() {
        var s=seed33();var w=s.world();w.runTo(600);
        long line=w.registry.owner(s.generator()),voids=w.registry.owner(s.junction());
        // A pipe placed on the second void's free face touches only that void: the generator's line is not fenced, not replaced.
        w.place(0,1,7,Kind.PIPE,NORTH,.05);w.runTo(602);
        assertEquals(line,w.registry.owner(s.generator()),"an edit beyond a shared void does not reach the generator's line");
        assertEquals(voids,w.registry.owner(s.junction()),"a dead-end pipe on a void changes no island");
        // A dead-end pipe on the shared void's free face: the walk stops at the void, and neither island is replaced.
        w.place(4,0,1,Kind.PIPE,NORTH,.05);w.runTo(604);
        assertEquals(line,w.registry.owner(s.generator()),"a pipe on the shared void's free face leaves the generator's line");
        assertEquals(voids,w.registry.owner(s.junction()),"and the voids' line");
        assertEquals(Set.of(line,voids),islandsHolding(w,s.firstVoid()));
        // An edit of the shared void itself changes every island that holds it.
        var record=w.ledger.active().get(s.firstVoid());
        w.registry.submit(List.of(new WorldTopologyLedger.Edit(s.firstVoid(),new WorldTopologyLedger.Registration(record.device(),EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101000),record.revision()+1))),w.ledger.nextIdentity(),null);
        w.runTo(700);
        var after=islandsHolding(w,s.firstVoid());assertEquals(2,after.size(),"the void still joins two islands");
        assertFalse(after.contains(line)||after.contains(voids),"both islands holding the edited void were replaced: "+after);
        for(long island:after)for(var node:w.coordinator.observe(island).graph().reservoirs())if(node.id()==s.firstVoid())assertEquals(101000,node.state().pressure(),1e-6,"each island holds the void's new state");
        w.runTo(4200);w.assertKeptUp("seed 33 after the edits");
    }
}
