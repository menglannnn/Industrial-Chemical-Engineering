package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Probe (not a gate): variants of the GUI-fixes run-1 rig, printing every slice. */
class FedTankOpenOutletsProbeTest {
    static final String DIMENSION="minecraft:overworld";
    static final int Y=-57;
    static PipeResistance.Geometry pipe(double d){return new PipeResistance.Geometry(1,d,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);}
    final FluidThermodynamics model=FluidTestSupport.networkModel();

    static PhysicalFluidTopology.Device device(long id,int x,int y,int z,Kind kind,PipeResistance.Geometry g) {
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position(DIMENSION,x,y,z),kind,PhysicalFluidTopology.Direction.NORTH,g,new FlowControl.Passive());
    }
    static double[] water(){double[] n=new double[MaterialTestBasis.NETWORK+1];n[n.length-1]=1;return n;}
    static double[] nitrogen(){double[] n=new double[MaterialTestBasis.NETWORK+1];n[MaterialTestBasis.NITROGEN]=1;return n;}
    static int node(PassiveNetwork graph,long id){for(int i=0;i<graph.reservoirs().size();i++)if(graph.reservoirs().get(i).id()==id)return i;return -1;}

    static final long TANK=1;
    long traceFrom=-1;
    /** In-game placement order and online ticks (client log run 1, 20 TPS from the world load). Each entry: tick, devices. */
    List<Object[]> placements(double outlet,int generatorTick,boolean generatorFirst) {
        var g=pipe(.05);var o=pipe(outlet);
        var list=new ArrayList<Object[]>();
        list.add(new Object[]{664,List.of(device(TANK,0,Y,0,Kind.RESERVOIR,g))});
        if(generatorFirst){list.add(new Object[]{680,List.of(device(2,-1,Y,0,Kind.PIPE,g))});list.add(new Object[]{690,List.of(device(11,-2,Y,0,Kind.GENERATOR,g))});}
        else list.add(new Object[]{698,List.of(device(2,-1,Y,0,Kind.PIPE,g))});
        list.add(new Object[]{732,List.of(device(3,1,Y,0,Kind.PIPE,o))});
        list.add(new Object[]{767,List.of(device(4,0,Y,-1,Kind.PIPE,o))});
        list.add(new Object[]{800,List.of(device(5,0,Y+1,0,Kind.PIPE,o))});
        list.add(new Object[]{834,List.of(device(6,0,Y-1,0,Kind.PIPE,o))});
        list.add(new Object[]{868,List.of(device(7,2,Y,0,Kind.VOID,g))});
        list.add(new Object[]{903,List.of(device(8,0,Y,-2,Kind.VOID,g))});
        list.add(new Object[]{936,List.of(device(9,0,Y+2,0,Kind.VOID,g))});
        list.add(new Object[]{970,List.of(device(10,0,Y-2,0,Kind.VOID,g))});
        if(!generatorFirst)list.add(new Object[]{generatorTick,List.of(device(11,-2,Y,0,Kind.GENERATOR,g))});
        return list;
    }
    /** Every block of {@link #placements} in one topology event at tick 0. */
    List<Object[]> atOnce(double outlet) {
        var all=new ArrayList<PhysicalFluidTopology.Device>();
        for(var p:placements(outlet,1008,false))for(var d:(List<?>)p[1])all.add((PhysicalFluidTopology.Device)d);
        var list=new ArrayList<Object[]>();list.add(new Object[]{0,all});return list;
    }
    FluidDeviceSpec spec(Kind kind,double generatorPressure) {
        return switch(kind){case GENERATOR->new FluidDeviceSpec(1,298.15,generatorPressure,water());default->new FluidDeviceSpec(1,298.15,101325,nitrogen());};
    }
    PassiveNetwork compile(List<PhysicalFluidTopology.Device> devices,PassiveNetwork.Reservoir tank,double generatorPressure) {
        var boundaries=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        for(var d:devices)if(d.kind()==Kind.RESERVOIR||d.kind()==Kind.VOID||d.kind()==Kind.GENERATOR)
            boundaries.put(d.id(),d.id()==TANK&&tank!=null?tank:spec(d.kind(),generatorPressure).initialize(d,model,()->{}));
        var compiled=PhysicalFluidTopology.compile(devices,boundaries,Map.of(),model.initialNitrogenCharge(1,298.15,101325,()->{}));
        return compiled.islands().stream().filter(i->i.physicalIds().contains(TANK)).findFirst().orElseThrow().graph();
    }

    /**
     * Runs the rig as the world does: each placement is a topology event at its tick, which starts the island's slice ladder
     * at one tick (IslandCoordinator.topology; every accepted full interval doubles it back to the 100-tick cadence), slices
     * end at the next event's tick (its fence), and a hold halves the slice on a fresh solver. Returns the hold count.
     */
    int run(String label,List<Object[]> placed,double generatorPressure,int slicesAfterGenerator) {
        var devices=new ArrayList<PhysicalFluidTopology.Device>();int next=0;
        long tick=(Integer)placed.get(0)[0];
        PassiveNetwork graph=null;PassiveNetwork.Reservoir tank=null;RetainedSolver retained=null;PassiveIntervalSolver.Result last=null;
        int span=1,holds=0,slices=0,maxAttempts=0;
        while(true) {
            if(next<placed.size()&&(Integer)placed.get(next)[0]==tick) {
                for(var d:(List<?>)placed.get(next)[1])devices.add((PhysicalFluidTopology.Device)d);next++;
                graph=compile(devices,tank,generatorPressure);retained=new RetainedSolver();last=null;span=1;
                System.out.println("F7_TOPOLOGY "+label+" tick="+tick+" devices="+devices.size()+" nodes="+graph.reservoirs().size()+" pipes="+graph.pipes().size());
            }
            long fence=next<placed.size()?(Integer)placed.get(next)[0]:Long.MAX_VALUE;
            long end=Math.min(tick+Math.min(100,span),fence);int ticks=(int)(end-tick);
            retained.committed(last);
            int t=node(graph,TANK);
            try {
                PassiveIntervalSolver.F7_TRACE=traceFrom>=0&&tick>=traceFrom&&tick<traceFrom+200;
                if(PassiveIntervalSolver.F7_TRACE)System.out.println("F7_TRACED_SLICE "+label+" start="+tick+" ticks="+ticks);
                var result=retained.solve(model,graph,ticks/20.0,PassiveIntervalSolver.Settings.defaults(),()->{});
                graph=result.graph();last=result;tank=graph.reservoirs().get(t);tick=end;
                span=span>=50?Integer.MAX_VALUE:span*2;
                if(next>=placed.size())maxAttempts=Math.max(maxAttempts,result.acceptedSubsteps()+result.rejectedSubsteps());
                var s=tank.state();
                System.out.println("F7_SLICE "+label+" end="+end+" ticks="+ticks+" P="+s.pressure()+" mass="+s.mass()+" condensed="+model.liquidMass(s)+" vapourShare="+s.vaporVolume()/s.volume()
                        +" flows="+Arrays.toString(result.averageMassFlows())+" modes="+result.endpointModes()+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps()+" reasons="+result.rejectionReasons());
            } catch(RuntimeException held) {
                holds++;retained=new RetainedSolver();
                System.out.println("F7_HELD "+label+" start="+tick+" ticks="+ticks+" "+held.getMessage());
                if(ticks>1)span=Math.max(1,ticks/2);
            }
            PassiveIntervalSolver.F7_TRACE=false;
            if(next>=placed.size())slices++;
            if(next>=placed.size()&&slices>=slicesAfterGenerator)break;
        }
        System.out.println("F7_END "+label+" holds="+holds+" tick="+tick+" maxAttempts="+maxAttempts+" tankMass="+tank.state().mass()+" condensed="+model.liquidMass(tank.state())+" P="+tank.state().pressure());
        return holds;
    }

    @Test void traceInGame(){traceFrom=1007;run("trace",placements(.05,1007,false),101325,9);}
    @Test void variants() {
        var summary=new ArrayList<String>();
        for(int offset:new int[]{0,-3,-2,-1,1,2,3,5,10,20})summary.add("staged gen@"+(1008+offset)+" holds="+run("staged"+offset,placements(.05,1008+offset,false),101325,140));
        summary.add("generator first holds="+run("genfirst",placements(.05,1008,true),101325,140));
        summary.add("at once holds="+run("once",atOnce(.05),101325,140));
        summary.add("outlets 0.01 holds="+run("out01",placements(.01,1008,false),101325,140));
        summary.add("generator 130 kPa holds="+run("gen130",placements(.05,1008,false),130000,140));
        summary.forEach(l->System.out.println("F7_SUMMARY "+l));
    }
}
