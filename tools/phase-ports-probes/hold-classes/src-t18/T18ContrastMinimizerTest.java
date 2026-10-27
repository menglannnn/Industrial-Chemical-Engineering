package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.NetworkReduction;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t18 (2026-10-13, card E9 (a)): reduces a fuzz seed's placements to a smallest list in which two modes of
 * src/scratch-t18-switch.patch (NetworkReduction.SCRATCH_T18, a thread-local list of flags; the mint runs on the thread
 * that ticks the world) differ in outcome: -Dt18bad=<mode> holds in the target class and -Dt18good=<mode> runs clean
 * (defaults: bad=on, good=nocolumn). Copy of T16ContrastMinimizerTest. -Dminimize.seed -Dminimize.slices (120)
 * -Dminimize.class (default: the holding mode's first class) -Dminimize.threads (8); holdFuzz.defaults for the distribution.
 */
class T18ContrastMinimizerTest {
    static final String BAD=System.getProperty("t18bad","on"),GOOD=System.getProperty("t18good","nocolumn");
    record Two(HoldMinimizerTest.Outcome bad,HoldMinimizerTest.Outcome good) {
        boolean contrast(String target){return bad.cls().equals(target)&&good.cls().equals("clean");}
        @Override public String toString(){return BAD+"="+bad.cls()+"@"+bad.tick()+" "+GOOD+"="+good.cls()+"@"+good.tick()+" | "+bad.status();}
    }
    static HoldMinimizerTest.Outcome under(String mode,List<HoldFuzzTest.Placement> list,int slices) {
        NetworkReduction.SCRATCH_T18.set(mode);
        try{return HoldMinimizerTest.run(list,slices);}finally{NetworkReduction.SCRATCH_T18.remove();}
    }
    static Two two(List<HoldFuzzTest.Placement> list,int slices) {
        var bad=under(BAD,list,slices);if(bad.cls().equals("clean"))return new Two(bad,bad);
        return new Two(bad,under(GOOD,list,slices));
    }
    @Test void contrast() throws Exception {
        long seed=Long.getLong("minimize.seed",842);int slices=Integer.getInteger("minimize.slices",120);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var list=new ArrayList<>(HoldFuzzTest.placements(r,blocks));var start=List.copyOf(list);
        var first=two(list,slices);String target=System.getProperty("minimize.class",first.bad().cls());
        System.out.println("CONTRAST seed="+seed+" bad="+BAD+" good="+GOOD+" start blocks="+list.size()+" "+first);
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
