package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t45 (tools/junction-seeding/trace-branch, never tracked): M7CandidatesTest with a branch class instead of a hold class.
 * A world's class at its fixed end tick (last placement + 100 * m7.slices): "H<cls>" when an island held, "U" when an island
 * is not certified STEADY (its last solve was a full solve), "S" when every island is STEADY. The status carries each island's
 * end status (certified flow or the full solve's notice), so the signature is readable. Same switches as M7CandidatesTest.
 */
class T45BranchTest {
    static final String BAR=java.util.regex.Pattern.quote("|");
    static String run(List<HoldFuzzTest.Placement> placements,int slices) {
        return M5TraceWorld.within(()->{
            var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),false);
            try {
                w.placeAll(placements);long end=placements.getLast().tick()+100L*slices;
                while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
            }catch(Throwable t){return "X|"+t;}
            if(!w.held.isEmpty())return "H"+M5MinimizerTest.cls(w.held.getFirst().status())+"|"+w.held.getFirst().status();
            boolean steady=true;var b=new StringBuilder();
            for(var s:w.coordinator.observe()){var st=s.status();if(!st.startsWith("STEADY"))steady=false;
                b.append(" i").append(s.id()).append('=').append(st.length()>70?st.substring(0,70):st).append(';');}
            return (steady?"S":"U")+"|"+b.toString().replace(' ','_');
        });
    }
    @Test void candidates() throws Exception {
        long seed=Long.getLong("m7.seed",4);int slices=Integer.getInteger("m7.slices",40);
        var worlds=new ArrayList<String>();
        if(Boolean.getBoolean("m7.drops")) {
            var base=M7CandidatesTest.items(seed,System.getProperty("m7.base",""));worlds.add(M7CandidatesTest.keepOf(base));
            for(int i=0;i<base.size();i++){var c=new ArrayList<>(base);c.remove(i);if(!c.isEmpty())worlds.add(M7CandidatesTest.keepOf(c));}
        } else for(var keep:System.getProperty("m7.lists","").split(BAR))worlds.add(M7CandidatesTest.keepOf(M7CandidatesTest.items(seed,keep.trim())));
        var pool=Executors.newFixedThreadPool(Math.min(4,Integer.getInteger("minimize.threads",4)));var futures=new ArrayList<Future<String>>();
        for(var keep:worlds)futures.add(pool.submit(()->run(M5SolveLogTest.list(seed,keep),slices)));
        for(int i=0;i<worlds.size();i++){var o=futures.get(i).get();int bar=o.indexOf('|');var cls=o.substring(0,bar);var status=o.substring(bar+1);
            System.out.println("M7CAND seed="+seed+" keep="+worlds.get(i)+" class="+cls+" status="+(status.length()>400?status.substring(0,400):status));}
        pool.shutdown();
    }
}
