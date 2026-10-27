package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import java.lang.management.ManagementFactory;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * M5 trace (tools/unified-thermo-port-m5/trace, never tracked): replays one seed, or a minimized list of it, on one thread and
 * prints one SOLVE line per island solve: its interval, cancellation checks (the deterministic work count of
 * {@link M5TraceWorld}), wall and thread-CPU milliseconds, the solver counters it moved, accepted and rejected substeps, its
 * rejection reasons and its detail; then the islands at the end and the ten costliest solves.
 * {@code -Dreplay.seed}, {@code -Dreplay.keep} ("i@tick,..." from a minimizer KEEP line; default the whole seed),
 * {@code -Dreplay.slices} (120), {@code -Dreplay.min} (print solves with at least this many checks; 0 prints all),
 * {@code -Dreplay.island} (also print this island's network before each of its solves).
 */
class M5SolveLogTest {
    static final List<String> NAMES=List.of("activeSetPasses","newtonSolves","newtonIterations","newtonBacktracks","jacobianBuilds","residualEvaluations",
            "stepAttempts","stepAttemptsAccepted","reconstructCalls","stateCalls","flashCalls","phaseSeeds","phaseRefusals","traceFolds","traceFoldPasses","equationGateRejections");
    static List<HoldFuzzTest.Placement> list(long seed,String keep) {
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);
        if(keep==null||keep.isBlank())return original;
        var list=new ArrayList<HoldFuzzTest.Placement>();
        for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        return list;
    }
    @Test void replay() {
        // -Dreplay.budgets="wall:soft,..." (ns): one replay per pair, the coordinator budgets set before each.
        for(var pair:System.getProperty("replay.budgets","2000000000:1500000000").split(",")){var b=pair.split(":");System.setProperty("holdFuzz.wallNanos",b[0]);System.setProperty("holdFuzz.softNanos",b[1]);System.out.println("M5LOG budgets wall="+b[0]+" soft="+b[1]);M5TraceWorld.within(this::replayOnce);}
    }
    void replayOnce() {
        long seed=Long.getLong("replay.seed",47);String keep=System.getProperty("replay.keep");int slices=Integer.getInteger("replay.slices",120);
        long min=Long.getLong("replay.min",0);long island=Long.getLong("replay.island",-1);
        var list=list(seed,keep);var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),island>0);
        System.out.println("M5LOG seed="+seed+" keep="+keep+" layout "+M5MinimizerTest.render(list));
        SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;var bean=ManagementFactory.getThreadMXBean();
        long[] cpu={0};Map<String,Long>[] before=new Map[1];
        w.before=(a,c)->{cpu[0]=bean.getCurrentThreadCpuTime();before[0]=sample();
            if(a.islandId()==island)System.out.println("M5NET island="+island+" start="+a.slice().startTick()+" end="+a.slice().endTick()+"\n"+M5TraceWorld.describe(c.snapshot(),w.model));};
        w.after=s->{
            long cpuMs=(bean.getCurrentThreadCpuTime()-cpu[0])/1_000_000;var after=sample();var delta=new StringBuilder();
            for(var n:NAMES)delta.append(' ').append(n).append('=').append(after.get(n)-before[0].get(n));
            if(s.checkpoints()>=min||s.island()==island)System.out.println("SOLVE island="+s.island()+" ["+s.start()+","+s.end()+"] checks="+s.checkpoints()+" ms="+s.nanos()/1_000_000+" cpuMs="+cpuMs
                    +" acc="+s.accepted()+" rej="+s.rejected()+" reasons="+s.reasons()+delta+" detail="+s.detail());
        };
        var refused=w.placeAll(list);for(var r:refused)System.out.println("M5LOG REFUSED "+r);
        long end=list.getLast().tick()+100L*slices;
        while(w.ledger.onlineTick()<end)w.tick();
        SolverDiagnostics.ENABLED=false;
        for(var h:w.held)System.out.println("M5LOG HELD "+h);
        for(var s:w.coordinator.observe())System.out.println("M5LOG END island="+s.id()+" status="+s.status());
        var top=new ArrayList<>(w.solves);top.sort(Comparator.comparingLong(M5TraceWorld.Solve::checkpoints).reversed());
        long total=0,totalNanos=0;for(var s:w.solves){total+=s.checkpoints();totalNanos+=s.nanos();}
        System.out.println("M5LOG solves="+w.solves.size()+" checks="+total+" ms="+totalNanos/1_000_000);
        for(var s:top.subList(0,Math.min(10,top.size())))System.out.println("M5TOP island="+s.island()+" ["+s.start()+","+s.end()+"] checks="+s.checkpoints()+" ms="+s.nanos()/1_000_000+" acc="+s.accepted()+" rej="+s.rejected()+" detail="+s.detail());
    }
    /** -Dreplay.variants="label/keep/retemp|...": one summary per variant of -Dreplay.seed at the default budgets; keep a KEEP list
     * (empty: the whole seed), retemp "index:K" (empty: none) restates one placement's generator temperature. */
    @Test void variants() {
        String spec=System.getProperty("replay.variants");if(spec==null)return;long seed=Long.getLong("replay.seed",47);int slices=Integer.getInteger("replay.slices",120);
        for(var v:spec.split("\\|"))M5TraceWorld.within(()->variant(v,seed,slices));
    }
    void variant(String v,long seed,int slices) {
        {
            var parts=(v+"/ / ").split("/");String label=parts[0],keep=parts[1].isBlank()?null:parts[1].trim(),retemp=parts[2].trim();
            var list=new ArrayList<>(list(seed,keep));
            if(!retemp.isEmpty()){var r=retemp.split(":");var original=HoldFuzzTest.placements(new Random(seed),HoldFuzzTest.world(new Random(seed)));
                var block=original.get(Integer.parseInt(r[0])).block();var s0=block.spec();
                var moved=new HoldFuzzTest.Block(block.x(),block.y(),block.z(),block.kind(),block.facing(),block.bore(),block.control(),
                        new FluidDeviceSpec(s0.volume(),Double.parseDouble(r[1]),s0.pressure(),s0.composition()),block.fluid());
                for(int i=0;i<list.size();i++)if(list.get(i).block().equals(block))list.set(i,new HoldFuzzTest.Placement(list.get(i).tick(),moved));}
            var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),false);var refused=w.placeAll(list);
            long end=list.getLast().tick()+100L*slices;while(w.ledger.onlineTick()<end)w.tick();
            long total=0,nanos=0;for(var s:w.solves){total+=s.checkpoints();nanos+=s.nanos();}
            var top=new ArrayList<>(w.solves);top.sort(Comparator.comparingLong(M5TraceWorld.Solve::checkpoints).reversed());
            var b=new StringBuilder("M5VARIANT "+label+" blocks="+list.size()+" refused="+refused+" solves="+w.solves.size()+" checks="+total+" ms="+nanos/1_000_000+" holds="+w.held.size());
            if(!w.held.isEmpty())b.append(" first=").append(w.held.getFirst());
            for(var s:w.coordinator.observe())b.append("\n  end island=").append(s.id()).append(' ').append(s.status());
            for(var s:top.subList(0,Math.min(3,top.size())))b.append("\n  top island=").append(s.island()).append(" [").append(s.start()).append(',').append(s.end()).append("] checks=").append(s.checkpoints()).append(" ms=").append(s.nanos()/1_000_000).append(" acc=").append(s.accepted()).append(" rej=").append(s.rejected()).append(" ").append(s.detail().length()>120?s.detail().substring(0,120):s.detail());
            System.out.println(b);
        }
    }
    static Map<String,Long> sample(){var s=SolverDiagnostics.sample();var m=new HashMap<String,Long>();for(var n:NAMES)m.put(n,s.value(n));return m;}
}
