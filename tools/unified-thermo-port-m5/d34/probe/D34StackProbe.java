package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;

/** D34 scratch: PhysicalFluidTopologyTest's stacked tanks, per slice. */
class D34StackProbe {
    final FluidThermodynamics model=FluidTestSupport.networkModel();
    PhysicalFluidTopology.Device at(long id,int x,int y,int z,Kind kind){return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position("minecraft:overworld",x,y,z),kind,PhysicalFluidTopology.Direction.NORTH,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());}
    @Test void stack() {
        double t=298.15,pressure=101325;double[] mw=model.molecularWeights();double[] n=new double[mw.length];int water=model.components().indexOf("Water"),nitrogen=model.components().indexOf("Nitrogen");
        n[water]=.3*997/mw[water];n[nitrogen]=pressure*(1-.3)/(FluidThermodynamics.R*t);
        var unit=model.flashTP(t,pressure,n,()->{});for(int c=0;c<n.length;c++)n[c]/=unit.volume();
        var start=new PassiveNetwork.Reservoir(1,2,model.flashTP(t,pressure,n,()->{}));
        var devices=List.of(at(1,0,2,0,Kind.RESERVOIR),at(2,0,1,0,Kind.PIPE),at(3,0,0,0,Kind.RESERVOIR));
        var compiled=PhysicalFluidTopology.compile(devices,Map.of(1L,start,3L,new PassiveNetwork.Reservoir(3,0,model.initialNitrogenCharge(1,298.15,101325,()->{}),PassiveNetwork.NodeKind.RESERVOIR)));
        var graph=compiled.islands().getFirst().graph();int upper=-1,lower=-1;
        for(int i=0;i<graph.reservoirs().size();i++){if(graph.reservoirs().get(i).id()==1)upper=i;if(graph.reservoirs().get(i).id()==3)lower=i;}
        int edge=-1;for(int i=0;i<graph.pipes().size();i++){var p=graph.pipes().get(i);if(p.first()==upper&&p.second()==lower||p.first()==lower&&p.second()==upper)edge=i;}
        System.out.println("D34STACK line volume="+(Math.PI*.05*.05/4)+" ports="+graph.pipes().get(edge).firstPort()+"/"+graph.pipes().get(edge).secondPort()+" dz="+(graph.reservoirs().get(lower).elevation()-graph.reservoirs().get(upper).elevation()));
        var solver=new PassiveIntervalSolver(model);
        for(int slice=0;slice<40;slice++) {
            var result=solver.solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{});graph=result.graph();
            var a=graph.reservoirs().get(upper).state();var b=graph.reservoirs().get(lower).state();
            System.out.println("D34STACK slice="+slice+" flow="+result.averageMassFlows()[edge]+" Pu="+a.pressure()+" Tu="+a.temperature()+" Lu="+a.waterVolume()+" Pl="+b.pressure()+" Tl="+b.temperature()+" Ll="+b.waterVolume()
                    +" acc="+result.acceptedSubsteps()+" rej="+result.rejectedSubsteps()+" rest="+NetworkReduction.atRest(graph,model));
        }
    }
}
