package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Hold F8, "a world reload cured it": the in-game line placed at its ticks (InlineFilterLineHoldTest), the world run to the
 * online tick the second client run loaded at (24168), saved and loaded through the checkpoint codec (island units and the
 * topology unit), registered as FluidWorldAuthority's constructor registers them, and run two minutes more. Prints the
 * island, its status, the held lines and the queued events before and after the load.
 */
class F8ReloadProbeTest {
    @Test void aReloadOfTheHeldWorld() {
        var outer=new InlineFilterLineHoldTest();
        var w=outer.new World();outer.run(w,InlineFilterLineHoldTest.LINE.length,24168-11436);
        for(var s:w.coordinator.observe())System.out.println("F8_BEFORE_RELOAD "+w.describe(s.id())+" retryAt="+s.clock().retryAtTick());
        System.out.println("F8_BEFORE_RELOAD online="+w.ledger.onlineTick()+" pending events="+w.ledger.events().size()+" holds="+w.held.size()+" last="+(w.held.isEmpty()?"-":w.held.getLast()));
        var checkpoint=new FluidCheckpointCodec.Checkpoint(w.coordinator.snapshots().stream().map(s->new FluidCheckpointCodec.IslandEntry(InlineFilterLineHoldTest.DIM,"createcheme:tjl20_methane_nitrogen",1e-9,s)).toList(),new BufferedTransfers.Snapshot(0,Map.of(),Map.of()));
        var loaded=FluidCheckpointCodec.decode(FluidCheckpointCodec.encode(checkpoint,w.ledger.onlineTick(),key->outer.model),key->outer.model);
        var topology=FluidCheckpointCodec.decodeTopology(FluidCheckpointCodec.encodeTopology(w.ledger.snapshot()),w.ledger.onlineTick());
        var l=outer.new World(topology,loaded.islands().stream().map(FluidCheckpointCodec.IslandEntry::snapshot).toList());
        for(var s:l.coordinator.observe())System.out.println("F8_AFTER_LOAD "+l.describe(s.id())+" retryAt="+s.clock().retryAtTick());
        // The second client run lasted 154 s (21:23:23 to 21:25:57): 3080 ticks.
        l.runTo(l.ledger.onlineTick()+3080);
        for(var s:l.coordinator.observe())System.out.println("F8_AFTER_RELOAD "+l.describe(s.id()));
        for(var h:l.held)System.out.println("F8_RELOAD_HELD "+h);
        System.out.println("F8_AFTER_RELOAD online="+l.ledger.onlineTick()+" pending events="+l.ledger.events().size()+" holds="+l.held.size());
        l.runTo(l.ledger.onlineTick()+8000);
        for(var h:l.held)System.out.println("F8_RELOAD_HELD_LATER "+h);
        System.out.println("F8_AFTER_RELOAD_LATER online="+l.ledger.onlineTick()+" pending events="+l.ledger.events().size()+" holds="+l.held.size());
    }
}
