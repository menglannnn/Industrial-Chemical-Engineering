package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.transport.MixtureViscosity;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t16 probe (tools only): replays fuzz seeds as {@link HoldFuzzTest} does and, at every hold, prints the held island's
 * gauge numbers on its committed state: the largest allowed static drive (Pa) of a passive connection, the largest
 * average flow of its last interval against the flow 0.1 Pa drives through that connection, the loops among its
 * non-fixed nodes, its identical generators; and the same numbers for the island's last four committed intervals.
 * {@code -Dt16.seeds=1,2 -Dt16.threads=8 -Dt16.out=<file>}, {@code holdFuzz.defaults} for the distribution.
 */
class T16TieProbeTest {
    record Sample(long tick,double drive,double flowRatio,double flow,String status) {
        @Override public String toString(){return String.format(Locale.ROOT,"(%d drive=%.3e q=%.3e q/qres=%.3e %s)",tick,drive,flow,flowRatio,status.length()>24?status.substring(0,24):status);}
    }
    /** {largest allowed passive drive, its pipe index, largest |q|/q_res, largest |q|} of a snapshot. */
    static double[] gauge(IslandCoordinator.Snapshot s,com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics model,MixtureViscosity.Workspace scratch) {
        var g=s.graph();double drive=Double.NEGATIVE_INFINITY;int at=-1;
        try {
            var d=T16Gauge.drives(g,model);
            for(int i=0;i<d.length;i++){if(!(g.pipes().get(i).control() instanceof FlowControl.Passive))continue;double m=Math.max(d[i][0],d[i][1]);if(m>drive){drive=m;at=i;}}
        }catch(RuntimeException e){drive=Double.NaN;}
        double ratio=0,flow=0;
        if(s.lastResult().isPresent()&&s.lastResult().get().averageMassFlows().length==g.pipes().size()) {
            var q=s.lastResult().get().averageMassFlows();
            for(int i=0;i<q.length;i++) {
                flow=Math.max(flow,Math.abs(q[i]));
                try{var p=g.pipes().get(i);double r=Math.min(T16Gauge.resolutionFlow(g,model,i,p.first(),scratch),T16Gauge.resolutionFlow(g,model,i,p.second(),scratch));ratio=Math.max(ratio,Math.abs(q[i])/r);}
                catch(RuntimeException e){ratio=Double.NaN;}
            }
        }
        return new double[]{drive,at,ratio,flow};
    }
    static String probe(long seed,boolean defaults) {
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var placements=HoldFuzzTest.placements(r,blocks);
        var out=new StringBuilder();var scratch=new MixtureViscosity.Workspace();
        var history=new HashMap<Long,ArrayDeque<Sample>>();var lastTick=new HashMap<Long,Long>();
        try {
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
            for(var p:placements){w.runTo(p.tick());try{w.place(p.block());}catch(IllegalArgumentException ignored){}}
            long end=placements.getLast().tick()+100L*Integer.getInteger("holdFuzz.slices",120);int reported=0;
            while(w.ledger.onlineTick()<end) {
                w.tick();
                for(var s:w.coordinator.observe()) {
                    long c=s.clock().committedTick();if(Objects.equals(lastTick.get(s.id()),c))continue;lastTick.put(s.id(),c);
                    var x=gauge(s,w.model,scratch);var h=history.computeIfAbsent(s.id(),k->new ArrayDeque<>());
                    h.addLast(new Sample(c,x[0],x[2],x[3],s.status()));if(h.size()>4)h.removeFirst();
                }
                while(w.held.size()>reported) {
                    var held=w.held.get(reported++);var s=w.coordinator.observe(held.island());var g=s.graph();var x=gauge(s,w.model,scratch);
                    int gens=0,voids=0,tanks=0,junctions=0,devices=0;
                    for(var n:g.reservoirs())switch(n.kind()){case GENERATOR->gens++;case VOID->voids++;case RESERVOIR->tanks++;case JUNCTION->junctions++;default->{}}
                    for(var p:g.pipes())if(!(p.control() instanceof FlowControl.Passive))devices++;
                    out.append(String.format(Locale.ROOT,"T16_HOLD %s seed=%d tick=%d island=%d class=%s gens=%d voids=%d tanks=%d junctions=%d pipes=%d devices=%d loops=%d identical=%s maxDrive=%.4e at=%d maxFlow=%.4e flowRatio=%.4e status=%s%n",
                            defaults?"defaults":"random",seed,held.tick(),held.island(),HoldFuzzTest.classify(held.status()),gens,voids,tanks,junctions,g.pipes().size(),devices,
                            T16Gauge.loops(g),T16Gauge.identicalGenerators(g),x[0],(int)x[1],x[3],x[2],held.status().length()>110?held.status().substring(0,110):held.status()));
                    out.append("  traj ").append(history.getOrDefault(held.island(),new ArrayDeque<>())).append('\n');
                    if(Boolean.getBoolean("t16.describe"))out.append(w.describe(held.island()));
                }
            }
        }catch(Throwable t){out.append("T16_EXCEPTION seed=").append(seed).append(' ').append(t).append('\n');}
        return out.toString();
    }
    @Test void ties() throws Exception {
        var seeds=HoldFuzzTest.seeds(System.getProperty("t16.seeds","47"));int threads=Integer.getInteger("t16.threads",1);
        boolean defaults=Boolean.getBoolean("holdFuzz.defaults");
        var pool=Executors.newFixedThreadPool(threads);var futures=new ArrayList<Future<String>>();
        for(long seed:seeds)futures.add(pool.submit(()->probe(seed,defaults)));
        var all=new StringBuilder();for(var f:futures)all.append(f.get());pool.shutdown();
        System.out.println(all);
        var out=System.getProperty("t16.out");if(out!=null){var path=Path.of(out);Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,all.toString(),StandardCharsets.UTF_8);}
    }
}
