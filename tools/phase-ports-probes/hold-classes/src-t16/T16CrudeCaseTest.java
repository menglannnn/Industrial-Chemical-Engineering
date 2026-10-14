package com.wormzjl.createcheme.runtime.fluid;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t16 (owner decision D44): the random crude two-phase holds saved for the thermodynamics work. Tools only.
 *
 * <p>{@code minimize}: -Dt16.min=521 [-Dt16.keep=i@tick,...] [-Dt16.slices=40]. Greedy block removal (as
 * HoldMinimizerTest) keeping a first hold of the seed's class (E2 or E5, HoldMinimizerTest.cls) whose held island holds a
 * two-phase hydrocarbon node: a tank or junction with both vapour and hydrocarbon liquid (the crude). Prints the KEEP list.
 *
 * <p>{@code replay}: -Dt16.case=521 -Dt16.keep=... [-Dt16.caseOut=dir]. Replays the list as the fuzz harness places it and
 * writes {@code case.txt}: every block with its exact spec and placement tick, the first hold (tick, committed tick,
 * status) and the held island's nodes and connections at the hold.
 */
class T16CrudeCaseTest {
    record Run(String cls,HoldFuzzTest.Held held,String island,boolean crude) {}
    static List<HoldFuzzTest.Placement> list(long seed,String keep) {
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);
        if(keep==null||keep.isEmpty())return original;
        var list=new ArrayList<HoldFuzzTest.Placement>();
        for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        return list;
    }
    static Run run(List<HoldFuzzTest.Placement> placements,int slices) {
        try {
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
            long end=placements.getLast().tick()+100L*slices;int next=0;
            while(w.ledger.onlineTick()<end) {
                while(next<placements.size()&&w.ledger.onlineTick()>=placements.get(next).tick()){try{w.place(placements.get(next).block());}catch(IllegalArgumentException refused){}next++;}
                w.tick();
                if(!w.held.isEmpty()) {
                    var h=w.held.getFirst();var g=w.coordinator.observe(h.island()).graph();boolean crude=false;
                    for(var n:g.reservoirs())if(!n.fixed()&&n.state().liquidVolume()>0&&n.state().vaporVolume()>0)crude=true;
                    return new Run(HoldMinimizerTest.cls(h.status()),h,w.describe(h.island()),crude);
                }
            }
            return new Run("clean",null,"",false);
        }catch(Throwable t){return new Run("X "+t,null,"",false);}
    }
    static String render(List<HoldFuzzTest.Placement> list){var b=new StringBuilder();for(var p:list)b.append(p.tick()).append(':').append(p.block().label()).append(' ');return b.toString();}
    static String keep(long seed,List<HoldFuzzTest.Placement> list) {
        var start=list(seed,null);var keep=new StringBuilder();
        for(var p:list)for(int k=0;k<start.size();k++)if(start.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
        return keep.toString();
    }
    @Test void minimize() throws Exception {
        Long seed=Long.getLong("t16.min");if(seed==null)return;int slices=Integer.getInteger("t16.slices",40);
        var list=new ArrayList<>(list(seed,System.getProperty("t16.keep")));
        var first=run(list,slices);String cls=first.cls();
        System.out.println("T16MIN seed="+seed+" start blocks="+list.size()+" class="+cls+" crude="+first.crude()+" held="+first.held());
        if(!first.crude()){System.out.println("T16MIN seed="+seed+" no two-phase crude node in the held island");return;}
        var pool=Executors.newFixedThreadPool(Integer.getInteger("t16.threads",4));
        boolean progress=true;
        while(progress) {
            progress=false;var futures=new ArrayList<Future<Run>>();
            for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->run(candidate,slices)));}
            for(int i=0;i<futures.size();i++) {
                if(futures.get(i)==null)continue;var o=futures.get(i).get();
                if(o.cls().equals(cls)&&o.crude()){list.remove(i);progress=true;System.out.println("T16MIN seed="+seed+" drop -> "+list.size()+" "+o.held());for(var f:futures)if(f!=null)f.cancel(true);break;}
            }
        }
        pool.shutdown();
        var last=run(list,slices);
        System.out.println("T16MIN seed="+seed+" KEEP "+keep(seed,list));
        System.out.println("T16MIN seed="+seed+" RESULT blocks="+list.size()+" class="+last.cls()+" held="+last.held()+"\nT16MIN seed="+seed+" LIST "+render(list));
    }
    @Test void replay() throws Exception {
        Long seed=Long.getLong("t16.case");if(seed==null)return;
        var list=list(seed,System.getProperty("t16.keep"));var o=run(list,Integer.getInteger("holdFuzz.slices",120));
        var b=new StringBuilder("seed "+seed+" distribution "+(HoldFuzzTest.DEFAULTS?"defaults":"random")+" keep "+System.getProperty("t16.keep","")+"\n");
        b.append("blocks (placement tick: block with its spec, as HoldFuzzTest places it)\n");
        for(var p:list){var k=p.block();b.append("  ").append(p.tick()).append(": ").append(k.label()).append(" bore=").append(k.bore()).append(" control=").append(k.control())
                .append(k.spec()==null?"":" spec=[T="+k.spec().temperature()+" K, P="+k.spec().pressure()+" Pa, volume="+k.spec().volume()+" m3, composition (network basis, mole fractions)="+Arrays.toString(k.spec().composition())+"]").append('\n');}
        b.append("first hold: ").append(o.held()).append("\nclass: ").append(o.cls()).append(" crude two-phase node in the held island: ").append(o.crude()).append('\n').append(o.island());
        System.out.println("T16CASE "+b);
        var dir=System.getProperty("t16.caseOut");if(dir!=null){var path=Path.of(dir);Files.createDirectories(path);Files.writeString(path.resolve("case.txt"),b.toString(),StandardCharsets.UTF_8);}
    }
}
