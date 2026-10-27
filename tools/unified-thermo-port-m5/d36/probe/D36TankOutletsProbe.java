package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;

/** D36 scratch: TankOutletsTest's tank (0.5 m3 of water under 120 kPa nitrogen, outlets up, east and down to 1 atm voids),
 * one 5 s interval and then 0.25 s intervals over the same 5 s: flows, the tank's state and the substeps. */
class D36TankOutletsProbe {
    final FluidThermodynamics model=FluidTestSupport.networkModel();
    PhysicalFluidTopology.Device at(long id,int x,int y,int z,Kind kind){return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,y,z),kind,PhysicalFluidTopology.Direction.NORTH,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());}
    FluidThermodynamics.State waterUnderNitrogen(double waterVolume,double pressure) {
        double t=298.15;double[] mw=model.molecularWeights();double[] n=new double[mw.length];int water=model.components().indexOf("Water");
        n[water]=waterVolume*997/mw[water];n[model.components().indexOf("Nitrogen")]=pressure*(1-waterVolume)/(FluidThermodynamics.R*t);
        var unit=model.flashTP(t,pressure,n,()->{});for(int c=0;c<n.length;c++)n[c]/=unit.volume();
        return model.flashTP(t,pressure,n,()->{});
    }
    PassiveNetwork graph() {
        var devices=List.of(at(1,0,0,0,Kind.RESERVOIR),at(2,0,1,0,Kind.PIPE),at(3,0,2,0,Kind.VOID),at(4,1,0,0,Kind.PIPE),at(5,2,0,0,Kind.VOID),at(6,0,-1,0,Kind.PIPE),at(7,0,-2,0,Kind.VOID));
        var boundaries=new HashMap<Long,PassiveNetwork.Reservoir>();boundaries.put(1L,new PassiveNetwork.Reservoir(1,0,waterUnderNitrogen(.5,120000)));
        for(long v:new long[]{3,5,7})boundaries.put(v,new PassiveNetwork.Reservoir(v,devices.get((int)v-1).position().y(),model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.VOID));
        return PhysicalFluidTopology.compile(devices,boundaries).islands().getFirst().graph();
    }
    void run(String label,double interval,int count) {
        var graph=graph();var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;
        double[] mass=new double[graph.pipes().size()];
        for(int i=0;i<count;i++) {
            solver.replayStart(graph,committed);
            var result=solver.solve(graph,interval,PassiveIntervalSolver.Settings.defaults(),()->{});committed=result;graph=result.graph();
            for(int e=0;e<mass.length;e++)mass[e]+=result.averageMassFlows()[e]*interval;
            var tank=graph.reservoirs().stream().filter(n->n.id()==1).findFirst().orElseThrow().state();
            System.out.println(label+" i="+i+" flows="+Arrays.toString(result.averageMassFlows())+" P="+tank.pressure()+" water="+tank.waterVolume()+" acc="+result.acceptedSubsteps()+" rej="+result.rejectedSubsteps()+" "+result.rejectionReasons());
        }
        for(int e=0;e<mass.length;e++){var p=graph.pipes().get(e);System.out.println(label+" pipe "+e+" ports="+p.firstPort()+"/"+p.secondPort()+" mean="+mass[e]/(interval*count));}
    }
    @Test void outlets(){run("D36TANK 5s",5,1);run("D36TANK 0.25s",.25,20);}
}
