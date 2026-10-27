package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * M5 trace (tools/unified-thermo-port-m5/trace, never tracked): the contrast on main's code (ffeafe8a, main's harness
 * tools/phase-ports-probes/hold-fuzz/src read in place, attached by ../m5-main-contrast.init.gradle). Replays a minimized list
 * of a seed on main's HoldFuzzTest.World and prints, every time an island commits, each vessel's temperature, pressure,
 * liquid volume and phase bits (M5MAIN lines, as M5HIST on the port), then the holds and the islands at the end.
 * {@code -Dreplay.seed}, {@code -Dreplay.keep}, {@code -Dreplay.slices} (40).
 */
class M5MainContrastTest {
    @Test void contrast() {
        long seed=Long.getLong("replay.seed",322);String keep=System.getProperty("replay.keep");int slices=Integer.getInteger("replay.slices",40);
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);var list=new ArrayList<HoldFuzzTest.Placement>();
        if(keep==null)list.addAll(original);
        else for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        // -Dreplay.retemp="index:K": one placement's generator restated at another temperature (as M5SolveLogTest.variants).
        String retemp=System.getProperty("replay.retemp");
        if(retemp!=null){var rt=retemp.split(":");var block=original.get(Integer.parseInt(rt[0])).block();var s0=block.spec();
            var moved=new HoldFuzzTest.Block(block.x(),block.y(),block.z(),block.kind(),block.facing(),block.bore(),block.control(),new FluidDeviceSpec(s0.volume(),Double.parseDouble(rt[1]),s0.pressure(),s0.composition()),block.fluid());
            for(int i=0;i<list.size();i++)if(list.get(i).block().equals(block))list.set(i,new HoldFuzzTest.Placement(list.get(i).tick(),moved));}
        boolean quiet=Boolean.getBoolean("replay.quiet");long started=System.nanoTime();
        com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics.reset();com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics.ENABLED=true;
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());var committed=new HashMap<Long,Long>();
        for(var p:list){w.runTo(p.tick());try{w.place(p.block());}catch(IllegalArgumentException refused){System.out.println("M5MAIN REFUSED "+p.block().label()+": "+refused.getMessage());}}
        long end=list.getLast().tick()+100L*slices;
        while(w.ledger.onlineTick()<end) {
            w.tick();
            for(var s:w.coordinator.observe()) {
                long c=s.clock().committedTick();if(Objects.equals(committed.put(s.id(),c),c))continue;
                var b=new StringBuilder("M5MAIN island="+s.id()+" committed="+c+" status="+(s.status().length()>80?s.status().substring(0,80):s.status()));var g=s.graph();
                for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);if(n.kind()!=PassiveNetwork.NodeKind.RESERVOIR)continue;var st=n.state();
                    b.append(String.format(Locale.ROOT," n%d:T=%.2f,P=%.0f,Vliq=%.3e,phases=%d",i,st.temperature(),st.pressure(),st.liquidVolume(),((st.liquidVolume()>0?1:0)|(st.vaporVolume()>0?2:0)|(st.waterLiquid()>0?4:0)|(st.waterVapor()>0?8:0))));}
                if(!quiet)System.out.println(b);
            }
        }
        var sample=com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics.sample();var counters=new StringBuilder();
        for(var name:List.of("newtonSolves","newtonIterations","jacobianBuilds","stepAttempts","stepAttemptsAccepted","activeSetPasses"))counters.append(' ').append(name).append('=').append(sample.value(name));
        System.out.println("M5MAIN SUMMARY seed="+seed+" retemp="+retemp+" ms="+(System.nanoTime()-started)/1_000_000+" holds="+w.held.size()+counters);
        for(var h:w.held)System.out.println("M5MAIN HELD "+h);
        for(var s:w.coordinator.observe())System.out.println("M5MAIN END "+w.describe(s.id()));
    }
}
