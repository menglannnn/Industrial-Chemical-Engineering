package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * Reduces a fuzz seed's placements to a smallest list that still holds with the same class (the class of the seed's first
 * hold, as ../hold-fuzz/classify.sh names it). Greedy: repeatedly drops the first block whose removal keeps the class
 * (candidates tried in parallel), then tries merging placement ticks into the previous one. Prints the result as a
 * placement list. -Dminimize.seed=30 -Dminimize.defaults=true (holdFuzz.defaults) -Dminimize.class=E5 (optional).
 */
class HoldMinimizerTest {
    static final String MATCH=System.getProperty("minimize.match","(?s).*");
    static String cls(String s) {
        if(s.startsWith("HELD (thermo domain)"))return "C";
        if(s.startsWith("HELD: wall deadline")||s.startsWith("HELD: soft budget"))return "F";
        if(s.matches("(?s).*Backward-Euler state change #=\\d{3,}.*"))return "A";
        if(s.contains("Matrix values must be finite"))return "B";
        s=s.replaceAll("[;,] reasons=.*","");
        if(s.contains("No finite Jacobian"))return "E10";
        if(s.contains("Singular matrix: empty or zero row"))return "G";
        if(s.startsWith("HELD: Singular Newton Jacobian"))return "D";
        if(s.startsWith("HELD: Newton iteration limit")||s.startsWith("HELD: Newton line search stalled")||s.startsWith("HELD: Phase/device active-set cycle"))return "E1";
        if(s.startsWith("HELD: Substep refinement exhausted: Singular Newton Jacobian"))return "E2";
        if(s.contains("partial pressure below the numerical floor")||s.contains("Water-vapor approximation outside"))return "E3";
        if(s.contains("equation gate"))return "E4";
        if(s.startsWith("HELD: Substep refinement exhausted: Newton line search stalled"))return "E5";
        if(s.startsWith("HELD: Substep refinement exhausted: Newton iteration limit"))return "E6";
        if(s.contains("active-set cycle"))return "E7";
        if(s.contains("Interval substep limit"))return "E8";
        return "E9";
    }
    record Outcome(String cls,String status,long tick) {}
    static Outcome run(List<HoldFuzzTest.Placement> placements,int slices) {
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
        try {
            for(var p:placements){w.runTo(p.tick());try{w.place(p.block());}catch(IllegalArgumentException refused){}}
            long end=placements.getLast().tick()+100L*slices;
            while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
        }catch(Throwable t){return new Outcome("X",String.valueOf(t),w.ledger.onlineTick());}
        if(w.held.isEmpty())return new Outcome("clean","",w.ledger.onlineTick());
        var h=w.held.getFirst();return new Outcome(cls(h.status()),h.status(),h.tick());
    }
    static String render(List<HoldFuzzTest.Placement> list) {
        var b=new StringBuilder();for(var p:list)b.append(p.tick()).append(':').append(p.block().label()).append(' ');return b.toString();
    }
    @Test void minimize() throws Exception {
        long seed=Long.getLong("minimize.seed",30);int slices=Integer.getInteger("minimize.slices",40);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var list=new ArrayList<>(HoldFuzzTest.placements(r,blocks));var start=List.copyOf(list);
        var first=run(list,slices);String target=System.getProperty("minimize.class",first.cls());
        System.out.println("MINIMIZE seed="+seed+" start blocks="+list.size()+" class="+first.cls()+" target="+target+" status="+first.status());
        if(!first.cls().equals(target))return;
        var pool=Executors.newFixedThreadPool(Integer.getInteger("minimize.threads",8));
        boolean progress=true;
        while(progress) {
            progress=false;var futures=new ArrayList<Future<Outcome>>();
            for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->run(candidate,slices)));}
            for(int i=0;i<futures.size();i++) {
                if(futures.get(i)==null)continue;var o=futures.get(i).get();
                if(o.cls().equals(target)&&o.status().matches(MATCH)){list.remove(i);progress=true;System.out.println("MINIMIZE seed="+seed+" drop -> "+list.size()+" "+o.status());for(var f:futures)if(f!=null)f.cancel(true);break;}
            }
        }
        // Tick compaction: every placement at the tick of the previous one, then the first one earlier.
        for(int i=1;i<list.size();i++) {
            var candidate=new ArrayList<>(list);candidate.set(i,new HoldFuzzTest.Placement(list.get(i-1).tick(),list.get(i).block()));
            for(int j=i+1;j<candidate.size();j++)candidate.set(j,new HoldFuzzTest.Placement(candidate.get(j).tick()-(list.get(i).tick()-list.get(i-1).tick()),candidate.get(j).block()));
            {var o=run(candidate,slices);if(o.cls().equals(target)&&o.status().matches(MATCH))list=candidate;}
        }
        pool.shutdown();
        var last=run(list,slices);
        var original=start;var keep=new StringBuilder();
        for(var p:list)for(int k=0;k<original.size();k++)if(original.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
        System.out.println("MINIMIZE seed="+seed+" KEEP "+keep);
        System.out.println("MINIMIZE seed="+seed+" RESULT blocks="+list.size()+" class="+last.cls()+" tick="+last.tick()+" status="+last.status()+"\nMINIMIZE seed="+seed+" LIST "+render(list));
    }
    /** t26: prints a minimized list (-Dreplay.seed, -Dreplay.keep) as EventPathWorld placement code with every digit. */
    @Test void code() {
        String keep=System.getProperty("replay.keep");if(keep==null)return;long seed=Long.getLong("replay.seed");
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);long at=-1;
        for(var item:keep.split(",")){var parts=item.split("@");long tick=Long.parseLong(parts[1]);var b=original.get(Integer.parseInt(parts[0])).block();
            if(tick!=at){System.out.println("CODE w.runTo("+tick+");");at=tick;}
            var s=b.spec();String spec=s==null?"null":"new FluidDeviceSpec("+s.volume()+","+s.temperature()+","+s.pressure()+",new double[]"+Arrays.toString(s.composition()).replace('[','{').replace(']','}')+")";
            System.out.println("CODE w.place("+b.x()+","+b.y()+","+b.z()+",Kind."+b.kind()+","+b.facing()+","+b.control()+","+spec+","+b.bore()+"); // "+b.label());}
    }
    /** Replays a minimized list (-Dreplay.seed, -Dreplay.keep="i@tick,..." from a KEEP line) and prints each held island. */
    @Test void replay() {
        String keep=System.getProperty("replay.keep");if(keep==null)return;long seed=Long.getLong("replay.seed");
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);var list=new ArrayList<HoldFuzzTest.Placement>();
        for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        System.out.println("REPLAY seed="+seed+" "+render(list));
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
        for(var p:list){w.runTo(p.tick());w.place(p.block());}
        long end=list.getLast().tick()+100L*Integer.getInteger("minimize.slices",40);int reported=0;
        while(w.ledger.onlineTick()<end&&w.held.size()<Integer.getInteger("replay.holds",1))w.tick();
        for(var h:w.held)System.out.println("REPLAY_HELD "+h+"\n"+w.describe(h.island()));
        for(var s:w.coordinator.observe())System.out.println("REPLAY_END "+w.describe(s.id()));
    }
}
