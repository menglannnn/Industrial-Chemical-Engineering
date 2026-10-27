package com.wormzjl.icheme.runtime.fluid;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * M5 trace (tools/unified-thermo-port-m5/trace, never tracked): main's HoldMinimizerTest (phase-ports batch,
 * tools/phase-ports-probes/hold-classes/src) on the port's bundled harness. Reduces a fuzz seed's placements to a smallest list
 * that still holds in the same class (the class of the seed's first hold, as hold-fuzz/classify.sh names it): repeatedly drops
 * the first block whose removal keeps the class (candidates in parallel), then merges placement ticks.
 *
 * <p>The work criterion for a budget hold (class F): with {@code -Dminimize.work=K} a list is "W" when one island solve makes
 * at least K cancellation checks ({@link M5TraceWorld}), a deterministic count; run it with the budgets out of the way
 * ({@code -DholdFuzz.wallNanos}, {@code -DholdFuzz.softNanos}) so the slow interval completes instead of being cut.
 * {@code -Dminimize.seed}, {@code -Dminimize.class} (default: the first outcome's), {@code -Dminimize.slices} (40),
 * {@code -Dminimize.threads} (4), {@code -Dminimize.match} (a regex the status must match).
 */
class M5MinimizerTest {
    static final String MATCH=System.getProperty("minimize.match","(?s).*");
    static final long WORK=Long.getLong("minimize.work",0);
    /** The class of a hold status, as the main checkout's tools/phase-ports-probes/hold-fuzz/classify.sh (2026-10-17) names it. */
    static String cls(String s) {
        if(s.startsWith("HELD (phases)")||s.startsWith("HELD (thermo domain)"))return "C";
        if(s.startsWith("HELD: wall deadline")||s.startsWith("HELD: soft budget"))return "F";
        if(s.matches("(?s).*Backward-Euler state change #=\\d{3,}.*"))return "A";
        if(s.contains("Matrix values must be finite"))return "B";
        s=s.replaceAll("[;,] reasons=.*","");
        if(s.contains("No finite Jacobian"))return "E10";
        if(s.contains("Singular matrix: empty or zero row"))return "G";
        if(s.startsWith("HELD: Singular Newton Jacobian"))return "D";
        if(s.startsWith("HELD: Newton iteration limit")||s.startsWith("HELD: Newton line search stalled")||s.startsWith("HELD: Phase/device active-set cycle"))return "E1";
        if(s.startsWith("HELD: Substep refinement exhausted: Singular Newton Jacobian"))return "E2";
        if(s.contains("partial pressure below the numerical floor")||s.matches("(?s).*Partial pressure .* outside the numerical box.*")||s.contains("Water-vapor approximation outside"))return "E3";
        if(s.contains("equation gate"))return "E4";
        if(s.startsWith("HELD: Substep refinement exhausted: Newton line search stalled"))return "E5";
        if(s.startsWith("HELD: Substep refinement exhausted: Newton iteration limit"))return "E6";
        if(s.contains("active-set cycle"))return "E7";
        if(s.contains("Interval substep limit"))return "E8";
        return "E9";
    }
    record Outcome(String cls,String status,long tick,long maxCheckpoints,long island,long solveStart) {}
    static Outcome run(List<HoldFuzzTest.Placement> placements,int slices){return M5TraceWorld.within(()->runInContext(placements,slices));}
    static Outcome runInContext(List<HoldFuzzTest.Placement> placements,int slices) {
        var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),false);long[] max={0,0,0};
        w.after=s->{if(s.checkpoints()>max[0]){max[0]=s.checkpoints();max[1]=s.island();max[2]=s.start();}};
        try {
            w.placeAll(placements);
            long end=placements.getLast().tick()+100L*slices;
            while(w.ledger.onlineTick()<end&&(WORK>0?max[0]<WORK:w.held.isEmpty()))w.tick();
        }catch(Throwable t){return new Outcome("X",String.valueOf(t),w.ledger.onlineTick(),max[0],max[1],max[2]);}
        if(WORK>0&&max[0]>=WORK)return new Outcome("W","checkpoints="+max[0]+" island="+max[1]+" start="+max[2],w.ledger.onlineTick(),max[0],max[1],max[2]);
        if(w.held.isEmpty())return new Outcome("clean","maxCheckpoints="+max[0]+" island="+max[1]+" start="+max[2],w.ledger.onlineTick(),max[0],max[1],max[2]);
        var h=w.held.getFirst();return new Outcome(cls(h.status()),h.status(),h.tick(),max[0],max[1],max[2]);
    }
    static String render(List<HoldFuzzTest.Placement> list) {
        var b=new StringBuilder();for(var p:list)b.append(p.tick()).append(':').append(p.block().label()).append(' ');return b.toString();
    }
    static String keep(List<HoldFuzzTest.Placement> list,List<HoldFuzzTest.Placement> original) {
        var keep=new StringBuilder();
        for(var p:list)for(int k=0;k<original.size();k++)if(original.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
        return keep.toString();
    }
    @Test void minimize() throws Exception {
        long seed=Long.getLong("minimize.seed",322);int slices=Integer.getInteger("minimize.slices",40);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var list=new ArrayList<>(HoldFuzzTest.openTanks(seed,HoldFuzzTest.placements(r,blocks)));var start=List.copyOf(list);
        long t0=System.nanoTime();
        var first=run(list,slices);String target=System.getProperty("minimize.class",first.cls());
        System.out.println("MINIMIZE seed="+seed+" start blocks="+list.size()+" class="+first.cls()+" target="+target+" work="+WORK+" maxCheckpoints="+first.maxCheckpoints()+" island="+first.island()+" tick="+first.tick()+" status="+first.status());
        if(!first.cls().equals(target))return;
        var pool=Executors.newFixedThreadPool(Math.min(4,Integer.getInteger("minimize.threads",4)));
        boolean progress=true;
        while(progress) {
            progress=false;var futures=new ArrayList<Future<Outcome>>();
            for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->run(candidate,slices)));}
            for(int i=0;i<futures.size();i++) {
                if(futures.get(i)==null)continue;var o=futures.get(i).get();
                if(o.cls().equals(target)&&o.status().matches(MATCH)){var dropped=list.remove(i);progress=true;
                    System.out.println("MINIMIZE seed="+seed+" drop "+dropped.block().label()+" -> "+list.size()+" ["+(System.nanoTime()-t0)/1_000_000_000+" s] "+o.status());
                    for(var f:futures)if(f!=null)f.cancel(true);break;}
            }
        }
        // Tick compaction: every placement at the tick of the previous one, then the first one earlier.
        for(int i=1;i<list.size();i++) {
            var candidate=new ArrayList<>(list);candidate.set(i,new HoldFuzzTest.Placement(list.get(i-1).tick(),list.get(i).block()));
            for(int j=i+1;j<candidate.size();j++)candidate.set(j,new HoldFuzzTest.Placement(candidate.get(j).tick()-(list.get(i).tick()-list.get(i-1).tick()),candidate.get(j).block()));
            var o=run(candidate,slices);if(o.cls().equals(target)&&o.status().matches(MATCH))list=candidate;
        }
        pool.shutdown();
        var last=run(list,slices);
        System.out.println("MINIMIZE seed="+seed+" KEEP "+keep(list,start));
        System.out.println("MINIMIZE seed="+seed+" RESULT blocks="+list.size()+" class="+last.cls()+" tick="+last.tick()+" maxCheckpoints="+last.maxCheckpoints()+" island="+last.island()+" status="+last.status()
                +"\nMINIMIZE seed="+seed+" LIST "+render(list)+"\nMINIMIZE seed="+seed+" seconds="+(System.nanoTime()-t0)/1_000_000_000);
    }
}
