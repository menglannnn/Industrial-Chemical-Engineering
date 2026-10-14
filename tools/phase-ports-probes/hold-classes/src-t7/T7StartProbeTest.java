package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * t7 (2026-10-12, junction start forms nearest / strongest / mixture): prints every junction's start state (the graph an
 * island is registered with, its minted junctions) and the state after 120 slices, with the composition of each.
 * -Pt7seed=6 (with -PholdFuzzDefaults=true for the defaults distribution) replays a fuzz seed; -Pt7world=tank|void builds
 * the two-generator world: a 200 kPa nitrogen generator three blocks east and a 150 kPa methane generator one block west
 * of a junction pipe, both at 298.15 K and level with it, the junction draining down into a 1 m3 nitrogen tank at 1 atm
 * (tank) or a 1 atm nitrogen void (void). -Pt7slices (default 120). Not part of any gate suite.
 */
class T7StartProbeTest {
    static String composition(FluidThermodynamics.State s) {
        var n=PhaseLayout.totalAmounts(s);double total=0;for(double v:n)total+=v;
        return String.format(Locale.ROOT,"N2=%.6f CH4=%.6f H2O=%.6f",n[HoldFuzzTest.NITROGEN]/total,n[HoldFuzzTest.METHANE]/total,n[HoldFuzzTest.WATER]/total);
    }
    static String state(FluidThermodynamics.State s) {
        return String.format(Locale.ROOT,"P=%.6f T=%.6f m=%.9g vap=%.6g liq=%.6g water=%.6g %s",s.pressure(),s.temperature(),s.mass(),s.vaporVolume(),s.liquidVolume(),s.waterVolume(),composition(s));
    }
    static void print(String tag,HoldFuzzTest.World w,IslandCoordinator.Snapshot s,boolean all) {
        var g=s.graph();
        for(int i=0;i<g.reservoirs().size();i++) {
            var n=g.reservoirs().get(i);if(!all&&!n.junction())continue;
            System.out.println("T7_"+tag+" tick="+w.ledger.onlineTick()+" island="+s.id()+" node="+i+" id="+n.id()+" "+n.kind()+" z="+n.elevation()+" "+state(n.state()));
        }
        if(all)s.lastResult().ifPresent(r->System.out.println("T7_"+tag+" island="+s.id()+" status="+s.status()+" modes="+r.endpointModes()+" flows="+Arrays.toString(r.averageMassFlows())));
    }
    static HoldFuzzTest.Block block(int x,int y,int z,Kind kind,FluidDeviceSpec spec,String fluid) {
        return new HoldFuzzTest.Block(x,y,z,kind,PhysicalFluidTopology.Direction.NORTH,.05,new FlowControl.Passive(),spec,fluid);
    }
    static FluidDeviceSpec pure(int component,double t,double p){double[] n=new double[HoldFuzzTest.COMPONENTS];n[component]=1;return new FluidDeviceSpec(1,t,p,n);}

    @Test void startAndSteady() {
        int slices=Integer.getInteger("t7slices",120);
        List<HoldFuzzTest.Placement> placements;
        String world=System.getProperty("t7world");
        if("widevoid".equals(world)) {
            // Supply through 0.01 m runs, a 0.1 m junction pipe straight over the void: the drain is far more conductive than
            // either supply, so the junction stays near the void's pressure and both generators keep feeding it.
            placements=List.of(
                    new HoldFuzzTest.Placement(100,new HoldFuzzTest.Block(0,0,0,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,.1,new FlowControl.Passive(),null,"")),
                    new HoldFuzzTest.Placement(100,new HoldFuzzTest.Block(1,0,0,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,.01,new FlowControl.Passive(),null,"")),
                    new HoldFuzzTest.Placement(100,new HoldFuzzTest.Block(2,0,0,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,.01,new FlowControl.Passive(),null,"")),
                    new HoldFuzzTest.Placement(100,block(3,0,0,Kind.GENERATOR,pure(HoldFuzzTest.NITROGEN,298.15,200000),"N2")),
                    new HoldFuzzTest.Placement(100,new HoldFuzzTest.Block(-1,0,0,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,.01,new FlowControl.Passive(),null,"")),
                    new HoldFuzzTest.Placement(100,block(-2,0,0,Kind.GENERATOR,pure(HoldFuzzTest.METHANE,298.15,150000),"CH4")),
                    new HoldFuzzTest.Placement(100,block(0,-1,0,Kind.VOID,pure(HoldFuzzTest.NITROGEN,298.15,101325),"N2")));
        } else if(world!=null) {
            var drain=world.equals("void")?Kind.VOID:Kind.RESERVOIR;
            placements=List.of(
                    new HoldFuzzTest.Placement(100,block(0,0,0,Kind.PIPE,null,"")),
                    new HoldFuzzTest.Placement(100,block(1,0,0,Kind.PIPE,null,"")),
                    new HoldFuzzTest.Placement(100,block(2,0,0,Kind.PIPE,null,"")),
                    new HoldFuzzTest.Placement(100,block(3,0,0,Kind.GENERATOR,pure(HoldFuzzTest.NITROGEN,298.15,200000),"N2")),
                    new HoldFuzzTest.Placement(100,block(-1,0,0,Kind.GENERATOR,pure(HoldFuzzTest.METHANE,298.15,150000),"CH4")),
                    new HoldFuzzTest.Placement(100,block(0,-1,0,drain,pure(HoldFuzzTest.NITROGEN,298.15,101325),"N2")));
        } else {
            long seed=Long.getLong("t7seed",6);var r=new Random(seed);placements=HoldFuzzTest.placements(r,HoldFuzzTest.world(r));
        }
        System.out.println("T7_WORLD "+(world!=null?world:"seed="+System.getProperty("t7seed","6")+" defaults="+HoldFuzzTest.DEFAULTS)+" slices="+slices);
        for(var p:placements)System.out.println("T7_PLACE "+p.tick()+" "+p.block().label());
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());var seen=new HashSet<Long>();var firsts=new HashSet<Long>();var firstAt=new HashMap<Long,Long>();
        long end=placements.getLast().tick()+100L*slices;int next=0;
        while(w.ledger.onlineTick()<end) {
            while(next<placements.size()&&placements.get(next).tick()<=w.ledger.onlineTick()){try{w.place(placements.get(next).block());}catch(IllegalArgumentException refused){System.out.println("T7_REFUSED "+refused.getMessage());}next++;}
            // World.tick, with the islands a topology event registers observed before their first job runs: the minted start.
            w.ledger.tick();w.coordinator.tick();
            if(w.ledger.hasPendingEvents()){w.registry.applyPending();w.coordinator.pumpIfUseful();}
            for(var s:w.coordinator.observe())if(seen.add(s.id())){print("START",w,s,false);firstAt.put(s.id(),s.clock().committedTick());}
            for(long request:List.copyOf(w.attempts.keySet())) {
                var attempt=w.attempts.remove(request);var command=w.commands.remove(request);
                w.coordinator.completed(attempt,Optional.of((com.wormzjl.createcheme.runtime.ProcessSolveServices.FluidIslandSolveResult)command.solve(HoldFuzzTest.NEVER)));
            }
            w.coordinator.pump();
            // The state after each island's first committed interval (5 s): what the start has to be close to.
            for(var s:w.coordinator.observe())if(s.clock().committedTick()>firstAt.computeIfAbsent(s.id(),k->s.clock().committedTick())&&firsts.add(s.id()))print("FIRST",w,s,false);
            if(!w.held.isEmpty()){System.out.println("T7_HELD "+w.held.getFirst());break;}
        }
        for(var s:w.coordinator.observe())print("END",w,s,true);
        System.out.println("T7_DONE tick="+w.ledger.onlineTick()+" holds="+w.held.size());
    }
}
