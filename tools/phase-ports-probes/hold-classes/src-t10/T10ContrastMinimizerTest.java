package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.NetworkReduction;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t10 (2026-10-13, the drain-side gas column): reduces a fuzz seed's placements to a smallest list in which the start
 * with and without the drain-side gas column differ in outcome. -Dt10want=worse (default): the start with the column holds
 * in the target class and the start without it (t9's `5999da45`) runs clean. -Dt10want=better: the reverse (for a gate
 * world). Needs src/scratch-t10-column-switch.patch applied (NetworkReduction.SCRATCH_COLUMN, a thread-local on|off; the
 * mint runs on the thread that ticks the world). Greedy block removal and tick compaction as HoldMinimizerTest.
 * -Dminimize.seed -Dminimize.slices (120) -Dminimize.class (default: the holding start's first class) -Dminimize.threads
 * (8); holdFuzz.defaults for the distribution.
 */
class T10ContrastMinimizerTest {
    static final boolean WORSE=!"better".equals(System.getProperty("t10want","worse"));
    record Two(HoldMinimizerTest.Outcome on,HoldMinimizerTest.Outcome off) {
        HoldMinimizerTest.Outcome holding(){return WORSE?on:off;}
        HoldMinimizerTest.Outcome running(){return WORSE?off:on;}
        boolean contrast(String target){return holding().cls().equals(target)&&running().cls().equals("clean");}
        @Override public String toString(){return "column="+on.cls()+"@"+on.tick()+" none="+off.cls()+"@"+off.tick()+" | "+holding().status();}
    }
    static HoldMinimizerTest.Outcome under(String start,List<HoldFuzzTest.Placement> list,int slices) {
        NetworkReduction.SCRATCH_COLUMN.set(start);
        try{return HoldMinimizerTest.run(list,slices);}finally{NetworkReduction.SCRATCH_COLUMN.remove();}
    }
    static Two two(List<HoldFuzzTest.Placement> list,int slices) {
        var hold=under(WORSE?"on":"off",list,slices);if(hold.cls().equals("clean"))return WORSE?new Two(hold,hold):new Two(hold,hold);
        var run=under(WORSE?"off":"on",list,slices);
        return WORSE?new Two(hold,run):new Two(run,hold);
    }
    @Test void contrast() throws Exception {
        long seed=Long.getLong("minimize.seed",364);int slices=Integer.getInteger("minimize.slices",120);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var list=new ArrayList<>(HoldFuzzTest.placements(r,blocks));var start=List.copyOf(list);
        var first=two(list,slices);String target=System.getProperty("minimize.class",first.holding().cls());
        System.out.println("CONTRAST seed="+seed+" want="+(WORSE?"worse":"better")+" start blocks="+list.size()+" "+first);
        if(!first.contrast(target))return;
        var pool=Executors.newFixedThreadPool(Integer.getInteger("minimize.threads",8));
        boolean progress=true;
        while(progress) {
            progress=false;var futures=new ArrayList<Future<Two>>();
            for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->two(candidate,slices)));}
            for(int i=0;i<futures.size();i++) {
                if(futures.get(i)==null)continue;var o=futures.get(i).get();
                if(o.contrast(target)){list.remove(i);progress=true;System.out.println("CONTRAST seed="+seed+" drop -> "+list.size()+" "+o);for(var f:futures)if(f!=null)f.cancel(true);break;}
            }
        }
        for(int i=1;i<list.size();i++) {
            var candidate=new ArrayList<>(list);candidate.set(i,new HoldFuzzTest.Placement(list.get(i-1).tick(),list.get(i).block()));
            for(int j=i+1;j<candidate.size();j++)candidate.set(j,new HoldFuzzTest.Placement(candidate.get(j).tick()-(list.get(i).tick()-list.get(i-1).tick()),candidate.get(j).block()));
            if(two(candidate,slices).contrast(target))list=candidate;
        }
        pool.shutdown();
        var last=two(list,slices);var keep=new StringBuilder();
        for(var p:list)for(int k=0;k<start.size();k++)if(start.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
        System.out.println("CONTRAST seed="+seed+" KEEP "+keep);
        System.out.println("CONTRAST seed="+seed+" RESULT blocks="+list.size()+" "+last+"\nCONTRAST seed="+seed+" LIST "+HoldMinimizerTest.render(list));
    }
}
