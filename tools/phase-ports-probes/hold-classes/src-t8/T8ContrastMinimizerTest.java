package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.NetworkReduction;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t8 (2026-10-12/13, junction start (m')): reduces a fuzz seed's placements to a smallest list in which the three junction
 * start rules differ in outcome: the committed rule (nearest) holds in the target class, form (c2) (strongest) holds, and
 * (m') runs clean for the slices. Needs src/scratch-t8-rule-switch.patch applied (NetworkReduction.SCRATCH_RULE, a
 * thread-local rule; the mint runs on the thread that ticks the world). Greedy block removal as HoldMinimizerTest, then
 * the same tick compaction. -Dminimize.seed -Dminimize.slices (120) -Dminimize.class (default: nearest's first class)
 * -Dminimize.threads (8); holdFuzz.defaults for the distribution. Also prints the three outcomes of the start list.
 */
class T8ContrastMinimizerTest {
    record Three(HoldMinimizerTest.Outcome nearest,HoldMinimizerTest.Outcome strongest,HoldMinimizerTest.Outcome mprime) {
        boolean contrast(String target){return nearest.cls().equals(target)&&!strongest.cls().equals("clean")&&mprime.cls().equals("clean");}
        @Override public String toString(){return "nearest="+nearest.cls()+"@"+nearest.tick()+" strongest="+strongest.cls()+"@"+strongest.tick()+" mprime="+mprime.cls()+"@"+mprime.tick()+" | "+nearest.status();}
    }
    static HoldMinimizerTest.Outcome under(String rule,List<HoldFuzzTest.Placement> list,int slices) {
        NetworkReduction.SCRATCH_RULE.set(rule);
        try{return HoldMinimizerTest.run(list,slices);}finally{NetworkReduction.SCRATCH_RULE.remove();}
    }
    static Three three(List<HoldFuzzTest.Placement> list,int slices) {
        var n=under("nearest",list,slices);if(n.cls().equals("clean"))return new Three(n,n,n);
        var m=under("mprime",list,slices);if(!m.cls().equals("clean"))return new Three(n,m,m);
        return new Three(n,under("strongest",list,slices),m);
    }
    @Test void contrast() throws Exception {
        long seed=Long.getLong("minimize.seed",30);int slices=Integer.getInteger("minimize.slices",120);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var list=new ArrayList<>(HoldFuzzTest.placements(r,blocks));var start=List.copyOf(list);
        var first=three(list,slices);String target=System.getProperty("minimize.class",first.nearest().cls());
        System.out.println("CONTRAST seed="+seed+" start blocks="+list.size()+" "+first);
        if(!first.contrast(target))return;
        var pool=Executors.newFixedThreadPool(Integer.getInteger("minimize.threads",8));
        boolean progress=true;
        while(progress) {
            progress=false;var futures=new ArrayList<Future<Three>>();
            for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->three(candidate,slices)));}
            for(int i=0;i<futures.size();i++) {
                if(futures.get(i)==null)continue;var o=futures.get(i).get();
                if(o.contrast(target)){list.remove(i);progress=true;System.out.println("CONTRAST seed="+seed+" drop -> "+list.size()+" "+o);for(var f:futures)if(f!=null)f.cancel(true);break;}
            }
        }
        for(int i=1;i<list.size();i++) {
            var candidate=new ArrayList<>(list);candidate.set(i,new HoldFuzzTest.Placement(list.get(i-1).tick(),list.get(i).block()));
            for(int j=i+1;j<candidate.size();j++)candidate.set(j,new HoldFuzzTest.Placement(candidate.get(j).tick()-(list.get(i).tick()-list.get(i-1).tick()),candidate.get(j).block()));
            if(three(candidate,slices).contrast(target))list=candidate;
        }
        pool.shutdown();
        var last=three(list,slices);var keep=new StringBuilder();
        for(var p:list)for(int k=0;k<start.size();k++)if(start.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
        System.out.println("CONTRAST seed="+seed+" KEEP "+keep);
        System.out.println("CONTRAST seed="+seed+" RESULT blocks="+list.size()+" "+last+"\nCONTRAST seed="+seed+" LIST "+HoldMinimizerTest.render(list));
    }
}
