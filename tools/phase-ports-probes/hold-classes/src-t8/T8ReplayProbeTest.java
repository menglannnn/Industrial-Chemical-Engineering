package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * t8 (2026-10-13): replays a minimized fuzz list (-Dt8seed, -Dt8keep="i@tick,..." from a KEEP line, holdFuzz.defaults for
 * the distribution) and prints every block with its exact spec (to write a gate test from), every island's minted junction
 * states before its first job, the junctions after the island's first committed interval, and every node at the end
 * (-Dt8slices, default 120) or at the first hold. Runs whatever start rule the working tree holds (with
 * src/scratch-t8-rule-switch.patch: -Dt8rule=nearest|strongest|mprime). Not part of any gate suite.
 */
class T8ReplayProbeTest {
    @Test void replay() {
        String keep=System.getProperty("t8keep");if(keep==null)return;long seed=Long.getLong("t8seed");int slices=Integer.getInteger("t8slices",120);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);var list=new ArrayList<HoldFuzzTest.Placement>();
        for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        System.out.println("T8_WORLD seed="+seed+" defaults="+HoldFuzzTest.DEFAULTS+" rule="+System.getProperty("t8rule","(tree)")+" slices="+slices);
        for(var p:list) {
            var b=p.block();var s=b.spec();
            System.out.println("T8_BLOCK tick="+p.tick()+" "+b.label()+" kind="+b.kind()+" at="+b.x()+","+b.y()+","+b.z()+" facing="+b.facing()+" bore="+b.bore()+" control="+b.control()
                    +(s==null?" spec=null":" spec volume="+s.volume()+" T="+s.temperature()+" P="+s.pressure()+" composition="+Arrays.toString(s.composition())+" solids="+s.solids()));
        }
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());var seen=new HashSet<Long>();var firsts=new HashSet<Long>();var firstAt=new HashMap<Long,Long>();
        long end=list.getLast().tick()+100L*slices;int next=0;
        while(w.ledger.onlineTick()<end) {
            while(next<list.size()&&list.get(next).tick()<=w.ledger.onlineTick()){try{w.place(list.get(next).block());}catch(IllegalArgumentException refused){System.out.println("T8_REFUSED "+refused.getMessage());}next++;}
            w.ledger.tick();w.coordinator.tick();
            if(w.ledger.hasPendingEvents()){w.registry.applyPending();w.coordinator.pumpIfUseful();}
            for(var s:w.coordinator.observe())if(seen.add(s.id())){T7StartProbeTest.print("START",w,s,false);firstAt.put(s.id(),s.clock().committedTick());}
            for(long request:List.copyOf(w.attempts.keySet())) {
                var attempt=w.attempts.remove(request);var command=w.commands.remove(request);
                w.coordinator.completed(attempt,Optional.of((com.wormzjl.createcheme.runtime.ProcessSolveServices.FluidIslandSolveResult)command.solve(HoldFuzzTest.NEVER)));
            }
            w.coordinator.pump();
            for(var s:w.coordinator.observe())if(s.clock().committedTick()>firstAt.computeIfAbsent(s.id(),k->s.clock().committedTick())&&firsts.add(s.id()))T7StartProbeTest.print("FIRST",w,s,false);
            if(!w.held.isEmpty()){System.out.println("T8_HELD "+w.held.getFirst());break;}
        }
        for(var s:w.coordinator.observe())T7StartProbeTest.print("END",w,s,true);
        System.out.println("T8_DONE tick="+w.ledger.onlineTick()+" holds="+w.held.size());
    }
}
