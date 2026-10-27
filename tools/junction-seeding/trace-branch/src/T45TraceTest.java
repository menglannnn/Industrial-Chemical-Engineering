package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.T45Probe;
import org.junit.jupiter.api.Test;

/**
 * t45 (tools/junction-seeding/trace-branch, never tracked): replays one KEEP list of a fuzz seed and prints, per island solve,
 * its detail, the end flows of its last accepted step and its junctions at the end (T45Probe, export only), then the islands'
 * end statuses. {@code -Dreplay.seed}, {@code -Dreplay.keep}, {@code -Dreplay.slices} (40), {@code -Dt45.trace=true} for the
 * probes inside the solver (balance rounds, rate root), {@code -Dt45.every} (print every n-th solve after the tenth; 1).
 */
class T45TraceTest {
    @Test void trace() {
        M5TraceWorld.within(()->{
            long seed=Long.getLong("replay.seed",4);String keep=System.getProperty("replay.keep");int slices=Integer.getInteger("replay.slices",40);
            int every=Integer.getInteger("t45.every",1);
            var list=M5SolveLogTest.list(seed,keep);var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),true);
            System.out.println("T45 seed="+seed+" keep="+keep+" layout "+M5MinimizerTest.render(list));
            int[] count={0};
            w.before=(a,c)->System.out.println("T45BEGIN island="+a.islandId()+" ["+a.slice().startTick()+","+a.slice().endTick()+"]");
            w.after=s->{
                count[0]++;
                System.out.println("T45SOLVE island="+s.island()+" ["+s.start()+","+s.end()+"] checks="+s.checkpoints()+" acc="+s.accepted()+" rej="+s.rejected()+" reasons="+s.reasons()+" detail="+s.detail());
                if(count[0]>10&&count[0]%every!=0)return;
                s.result().candidate().ifPresent(r->{T45Probe.flows("T45END-q",r.graph(),r.endpointFlows());
                    System.out.println("T45END-modes "+r.endpointModes());T45Probe.nodes("T45END",r.graph(),w.model);});
            };
            var refused=w.placeAll(list);for(var r:refused)System.out.println("T45 REFUSED "+r);
            long end=list.getLast().tick()+100L*slices;while(w.ledger.onlineTick()<end)w.tick();
            for(var h:w.held)System.out.println("T45 HELD "+h);
            for(var st:w.coordinator.observe())System.out.println("T45 END island="+st.id()+" status="+st.status());
        });
    }
}
