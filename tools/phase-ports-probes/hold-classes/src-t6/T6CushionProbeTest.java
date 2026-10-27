package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.InventoryEquilibrium;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * G2 follow-up probe (2026-10-12). Replays fuzz seeds one after another (-Pt6seed=400 or 220,431; the distribution from
 * -PholdFuzzDefaults), or a minimized list of one seed (-Pt6keep=i@tick,... from a MINIMIZE KEEP line), tick by tick on the
 * fuzz's world, and prints one T6_TICK line per tick that ran a solve: its wall time, the Newton solves and iterations (the
 * vessel closures' Newtons included), the step attempts (accepted / all), the equation gate's refusals and polishes
 * (accepted / all), and the vessel closures (calls, those that needed a Newton, their iterations; -1 without the t6
 * instrumentation patch). Each seed stops at its first hold unless -Pt6all=true; -Pt6until=<tick> stops at that tick.
 * A T6_SEED line sums each seed. The budget is the fuzz's unless -PholdFuzz.wallNanos / -PholdFuzz.softNanos are given.
 */
class T6CushionProbeTest {
    @Test void replay() {
        String seedText=System.getProperty("t6seed");if(seedText==null)return;
        for(long seed:HoldFuzzTest.seeds(seedText))replay(seed);
    }
    static void replay(long seed) {
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);var list=new ArrayList<HoldFuzzTest.Placement>();
        String keep=System.getProperty("t6keep");
        if(keep==null)list.addAll(original);
        else for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        boolean all=Boolean.getBoolean("t6all");long until=Long.getLong("t6until",Long.MAX_VALUE);
        System.out.println("T6 seed="+seed+" defaults="+HoldFuzzTest.DEFAULTS+" "+HoldMinimizerTest.render(list));
        for(var p:list){var b=p.block();System.out.println("T6_BLOCK "+p.tick()+" "+b.label()+" kind="+b.kind()+" bore="+b.bore()+" control="+b.control()
                +(b.spec()!=null?" T="+b.spec().temperature()+" P="+b.spec().pressure()+" fluid="+b.fluid():""));}
        SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
        long end=list.getLast().tick()+100L*Integer.getInteger("holdFuzz.slices",120);int next=0,reported=0;
        long[] first=counters(),last=first;double maxMs=0;long maxTick=0,slices=0;double totalMs=0;
        while(w.ledger.onlineTick()<Math.min(end,until)) {
            while(next<list.size()&&list.get(next).tick()<=w.ledger.onlineTick()){try{w.place(list.get(next).block());}catch(IllegalArgumentException refused){System.out.println("T6_REFUSED "+refused.getMessage());}next++;}
            long started=System.nanoTime();w.tick();long nanos=System.nanoTime()-started;
            long[] now=counters();
            if(now[0]!=last[0]||now[6]!=last[6]) {
                var d=new long[now.length];for(int i=0;i<d.length;i++)d[i]=now[i]-last[i];
                System.out.printf(Locale.ROOT,"T6_TICK seed=%d tick=%d ms=%.1f newtonSolves=%d newtonIterations=%d attempts=%d/%d gateRefusals=%d polishes=%d/%d closureCalls=%d closureSolves=%d closureIterations=%d%n",
                        seed,w.ledger.onlineTick(),nanos/1e6,d[0],d[1],d[3],d[2],d[4],d[5],d[6],d[7],d[8],d[9]);
                slices++;totalMs+=nanos/1e6;if(nanos/1e6>maxMs){maxMs=nanos/1e6;maxTick=w.ledger.onlineTick();}
            }
            last=now;
            if(w.held.size()>reported){for(var h:w.held.subList(reported,w.held.size()))System.out.println("T6_HELD seed="+seed+" "+h+"\n"+w.describe(h.island()));reported=w.held.size();if(!all)break;}
        }
        var d=new long[last.length];for(int i=0;i<d.length;i++)d[i]=last[i]-first[i];
        System.out.printf(Locale.ROOT,"T6_SEED seed=%d end=%d holds=%d first=%s solveTicks=%d totalMs=%.0f maxMs=%.0f at %d newtonSolves=%d newtonIterations=%d attempts=%d/%d gateRefusals=%d closureCalls=%d closureSolves=%d closureIterations=%d%n",
                seed,w.ledger.onlineTick(),w.held.size(),w.held.isEmpty()?"-":HoldMinimizerTest.cls(w.held.getFirst().status()),slices,totalMs,maxMs,maxTick,d[0],d[1],d[3],d[2],d[4],d[7],d[8],d[9]);
        SolverDiagnostics.ENABLED=false;
    }
    static long[] counters() {
        return new long[]{SolverDiagnostics.newtonSolves.sum(),SolverDiagnostics.newtonIterations.sum(),SolverDiagnostics.stepAttempts.sum(),SolverDiagnostics.stepAttemptsAccepted.sum(),
                SolverDiagnostics.equationGateRejections.sum(),SolverDiagnostics.equationGatePolishesAccepted.sum(),SolverDiagnostics.equationGatePolishes.sum(),
                instr("T6_CALLS"),instr("T6_SOLVES"),instr("T6_ITERATIONS")};
    }
    /** A closure counter of the t6 instrumentation patch, or -1 without it. */
    static long instr(String name) {
        try{return ((java.util.concurrent.atomic.AtomicLong)InventoryEquilibrium.class.getField(name).get(null)).get();}
        catch(ReflectiveOperationException absent){return -1;}
    }
}
