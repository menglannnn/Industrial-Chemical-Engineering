package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * M7 trace (tools/unified-thermo-port-m5/trace, never tracked): the class of each candidate world, for a minimization that
 * must hold at one commit and run clean at another (scripts/m7-pair-minimize.sh alternates the two commits). A world is a
 * KEEP list of the seed's placements ("i@tick,...", as M5MinimizerTest prints it; empty: the whole seed).
 * {@code -Dm7.seed}; {@code -Dm7.lists="keep|keep|..."} (each list's class); {@code -Dm7.base=keep} with
 * {@code -Dm7.drops=true} (the base and every list that drops one of its placements); {@code -Dm7.slices} (40, as the
 * minimizer); {@code -Dminimize.threads} (at most 4). One line per world: {@code M7CAND seed= keep= class= tick= status=}.
 */
class M7CandidatesTest {
    static String keepOf(List<String> items){return String.join(",",items);}
    static List<String> items(long seed,String keep) {
        if(keep!=null&&!keep.isBlank())return List.of(keep.split(","));
        var r=new Random(seed);var original=HoldFuzzTest.placements(r,HoldFuzzTest.world(r));var list=new ArrayList<String>();
        for(int k=0;k<original.size();k++)list.add(k+"@"+original.get(k).tick());
        return list;
    }
    @Test void candidates() throws Exception {
        long seed=Long.getLong("m7.seed",30);int slices=Integer.getInteger("m7.slices",40);
        var worlds=new ArrayList<String>();
        if(Boolean.getBoolean("m7.drops")) {
            var base=items(seed,System.getProperty("m7.base",""));worlds.add(keepOf(base));
            for(int i=0;i<base.size();i++){var c=new ArrayList<>(base);c.remove(i);if(!c.isEmpty())worlds.add(keepOf(c));}
        } else for(var keep:System.getProperty("m7.lists","").split("\\|"))worlds.add(keepOf(items(seed,keep.trim())));
        var pool=Executors.newFixedThreadPool(Math.min(4,Integer.getInteger("minimize.threads",4)));var futures=new ArrayList<Future<M5MinimizerTest.Outcome>>();
        for(var keep:worlds)futures.add(pool.submit(()->M5MinimizerTest.run(M5SolveLogTest.list(seed,keep),slices)));
        for(int i=0;i<worlds.size();i++){var o=futures.get(i).get();var status=o.status().length()>160?o.status().substring(0,160):o.status();
            System.out.println("M7CAND seed="+seed+" keep="+worlds.get(i)+" class="+o.cls()+" tick="+o.tick()+" status="+status);}
        pool.shutdown();
    }
}
