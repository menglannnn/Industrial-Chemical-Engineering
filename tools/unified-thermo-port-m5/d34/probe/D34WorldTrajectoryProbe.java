package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** D34 scratch: the seed 322 world's drop line and tanks per slice. */
class D34WorldTrajectoryProbe {
    private static final FluidDeviceSpec NITROGEN=EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,101325);
    private static final int METHANE=EventPathWorld.CATALOG.requirePackage("createcheme:tjl20_methane_nitrogen").components().indexOf("Methane");
    private static FluidDeviceSpec crude(double temperature,double pressure) {
        double[] n=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),MaterialTestBasis.NETWORK+1);
        return new FluidDeviceSpec(1,temperature,pressure,n);
    }
    @Test void trajectory() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(498);
        long upper=w.place(1,0,-1,Kind.RESERVOIR,NORTH,passive,NITROGEN,.1);
        w.place(0,1,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,2,-2,Kind.PIPE,NORTH,passive,null,.1);
        w.place(2,0,-1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,0,0,Kind.VOID,NORTH,passive,NITROGEN,.05);
        w.place(0,0,-2,Kind.GENERATOR,NORTH,passive,crude(362.6583839483169,276473.9715734725),.02);
        w.place(2,0,-2,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(METHANE,357.0575469354789,431096.16156985326),.1);
        w.place(1,3,-2,Kind.VOID,NORTH,passive,NITROGEN,.1);
        w.place(1,1,-2,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(680);w.place(1,0,-2,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(729);long lower=w.place(1,-2,-1,Kind.RESERVOIR,NORTH,passive,NITROGEN,.05);
        w.runTo(770);w.place(2,0,0,Kind.PIPE,NORTH,passive,null,.1);
        long last=-1;
        for(long tick=800;tick<=Long.getLong("d34.end",4770L);tick+=10) {
            w.runTo(tick);
            for(var s:w.coordinator.snapshots()) {
                var nodes=s.graph().reservoirs();int a=-1,b=-1,line=-1;
                for(int i=0;i<nodes.size();i++){if(nodes.get(i).id()==upper)a=i;if(nodes.get(i).id()==lower)b=i;}
                if(a<0||b<0||s.clock().committedTick()==last)continue;last=s.clock().committedTick();
                for(int i=0;i<s.graph().pipes().size();i++){var p=s.graph().pipes().get(i);if(p.first()==a&&p.second()==b||p.first()==b&&p.second()==a)line=i;}
                var top=nodes.get(a).state();var bottom=nodes.get(b).state();
                String flow=s.lastResult().map(r->String.valueOf(r.averageMassFlows()[0>1?0:0])).orElse("-");
                final int l=line;String drop=s.lastResult().map(r->l>=0?String.valueOf(r.averageMassFlows()[l]):"-").orElse("-");
                double rl=top.liquidVolume()>0?w.model.phaseMass(top,1)/com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.phaseVolume(top,1):Double.NaN,rg=w.model.vaporDensity(top),vl=Math.PI*.05*.05/4,vt=top.liquidVolume()>0?com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.phaseVolume(top,1):0;
                double col=(Double.isNaN(rl)?0:rl*vt)+rg*(vl-vt);col/=vl;
                System.out.println("D34COL committed="+s.clock().committedTick()+" rho_l="+rl+" rho_g="+rg+" Vtrace="+vt+" column="+col+" oldDrive="+(top.pressure()-bottom.pressure()+(Double.isNaN(rl)?rg:rl)*9.80665*2)+" newDrive="+(top.pressure()-bottom.pressure()+col*9.80665*2));
                System.out.println("D34TRAJ committed="+s.clock().committedTick()+" status="+(s.status().length()>40?s.status().substring(0,40):s.status())+" Pu="+top.pressure()+" Tu="+top.temperature()+" Lu="+top.liquidVolume()
                        +" Pl="+bottom.pressure()+" Tl="+bottom.temperature()+" Ll="+bottom.liquidVolume()+" dP="+(bottom.pressure()-top.pressure())+" drop="+drop+" ports="+(line>=0?s.graph().pipes().get(line).firstPort()+"/"+s.graph().pipes().get(line).secondPort():"-"));
            }
        }
        for(var h:w.held)System.out.println("D34TRAJ HELD "+h);
    }
}
