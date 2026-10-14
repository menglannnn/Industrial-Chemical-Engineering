package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The latent event-path case of hold class X (PHASE_PORTS_REVIEW.md "Hold class X", owner decision D27: fix it as a plain
 * defect with a gate test): a queued removal hides its device from a later event's walk ({@code PhysicalRegistry.submit}
 * walks the registry with the queued events applied, where the removed block is gone). The later event must still be fenced
 * on every island the batch changes. Built on a line whose island is mid-slice, so the removal waits for its fence: a
 * generator, five pipes and a void; the middle pipe is broken, and one tick later a new pipe is placed in the gap it left and
 * another beside the line's far half. Every event must apply without an exception and the islands must keep up.
 */
class QueuedRemovalWalkTest {
    private static void remove(EventPathWorld w,long id) {
        w.registry.submit(List.of(new WorldTopologyLedger.Edit(id,null)),w.ledger.nextIdentity(),null);
    }
    @Test void anEventBesideAQueuedRemovalIsFencedOnTheIslandsItChanges() {
        var w=new EventPathWorld();w.runTo(100);var passive=new FlowControl.Passive();
        w.place(0,0,0,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,150000),.05);
        var pipes=new ArrayList<Long>();for(int x=1;x<=5;x++)pipes.add(w.place(x,0,0,Kind.PIPE,NORTH,.05));
        w.place(6,0,0,Kind.VOID,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),.05);
        w.runTo(450);
        // Mid-slice: the removal is queued behind the island's fence.
        remove(w,pipes.get(2));assertEquals(1,w.ledger.events().size(),"the removal waits for the island's slice");
        w.runTo(451);w.place(3,0,0,Kind.PIPE,NORTH,.02);w.place(4,1,0,Kind.PIPE,NORTH,.05);
        w.runTo(2000);
        assertTrue(w.ledger.events().isEmpty(),"every event applied");
        assertNotNull(w.registry.owner(pipes.get(0)),"the line is whole again through the new pipe");
        assertEquals(w.registry.owner(pipes.get(0)),w.registry.owner(pipes.get(4)),"one island from the generator to the void");
        w.assertKeptUp("a queued removal and an event in its gap");
    }
    @Test void anEventBeyondAQueuedRemovalOnTheFarSideIsFencedToo() {
        var w=new EventPathWorld();w.runTo(100);var passive=new FlowControl.Passive();
        long tank=w.place(0,0,0,Kind.RESERVOIR,NORTH,.05);
        var pipes=new ArrayList<Long>();for(int x=1;x<=4;x++)pipes.add(w.place(x,0,0,Kind.PIPE,NORTH,.05));
        w.place(5,0,0,Kind.VOID,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325),.05);
        w.runTo(450);remove(w,pipes.get(1));
        // The later event is placed on the far side, next to the pipe the removal isolates from the tank.
        w.runTo(452);w.place(3,1,0,Kind.PIPE,NORTH,.05);w.place(3,2,0,Kind.RESERVOIR,NORTH,.05);
        w.runTo(2000);
        assertTrue(w.ledger.events().isEmpty(),"every event applied");
        assertNotEquals(w.registry.owner(tank),w.registry.owner(pipes.get(3)),"the removal split the line");
        w.assertKeptUp("an event beyond a queued removal");
    }
}
