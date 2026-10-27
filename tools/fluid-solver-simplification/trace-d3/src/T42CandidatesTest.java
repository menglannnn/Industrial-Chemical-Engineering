package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t42b (lane exports only): M7CandidatesTest (tools/unified-thermo-port-m5/trace/src) with a total-work class, for a seed whose
 * form-D3c behaviour is a cost blow-up rather than a hold. A world's class is its first hold's class (as M5MinimizerTest.cls
 * names it); otherwise "W" when the whole run makes at least {@code -Dt42.total} cancellation checks (M5TraceWorld's
 * deterministic work count; 0: never), otherwise "clean". Same switches as M7CandidatesTest ({@code -Dm7.seed},
 * {@code -Dm7.lists}, {@code -Dm7.base} with {@code -Dm7.drops=true}, {@code -Dm7.slices}, {@code -Dminimize.threads}).
 * One line per world: {@code M7CAND seed= keep= class= tick= total= status=}.
 */
class T42CandidatesTest {
    static final long TOTAL=Long.getLong("t42.total",0);
    record Outcome(String cls,String status,long tick,long total) {}
    static Outcome run(List<HoldFuzzTest.Placement> placements,int slices){return M5TraceWorld.within(()->runInContext(placements,slices));}
    static Outcome runInContext(List<HoldFuzzTest.Placement> placements,int slices) {
        var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),false);long[] total={0};
        w.after=s->total[0]+=s.checkpoints();
        try {
            w.placeAll(placements);
            long end=placements.getLast().tick()+100L*slices;
            while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
        }catch(Throwable t){return new Outcome("X",String.valueOf(t),w.ledger.onlineTick(),total[0]);}
        if(!w.held.isEmpty()){var h=w.held.getFirst();return new Outcome(M5MinimizerTest.cls(h.status()),h.status(),h.tick(),total[0]);}
        return new Outcome(TOTAL>0&&total[0]>=TOTAL?"W":"clean","total="+total[0],w.ledger.onlineTick(),total[0]);
    }
    @Test void candidates() throws Exception {
        long seed=Long.getLong("m7.seed",30);int slices=Integer.getInteger("m7.slices",40);
        var worlds=new ArrayList<String>();
        if(Boolean.getBoolean("m7.drops")) {
            var base=M7CandidatesTest.items(seed,System.getProperty("m7.base",""));worlds.add(M7CandidatesTest.keepOf(base));
            for(int i=0;i<base.size();i++){var c=new ArrayList<>(base);c.remove(i);if(!c.isEmpty())worlds.add(M7CandidatesTest.keepOf(c));}
        } else for(var keep:System.getProperty("m7.lists","").split("\\|"))worlds.add(M7CandidatesTest.keepOf(M7CandidatesTest.items(seed,keep.trim())));
        var pool=Executors.newFixedThreadPool(Math.min(4,Integer.getInteger("minimize.threads",4)));var futures=new ArrayList<Future<Outcome>>();
        for(var keep:worlds)futures.add(pool.submit(()->run(M5SolveLogTest.list(seed,keep),slices)));
        for(int i=0;i<worlds.size();i++){var o=futures.get(i).get();var status=o.status().length()>160?o.status().substring(0,160):o.status();
            System.out.println("M7CAND seed="+seed+" keep="+worlds.get(i)+" class="+o.cls()+" tick="+o.tick()+" total="+o.total()+" status="+status);}
        pool.shutdown();
    }
}
