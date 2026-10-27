package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.PhasePort;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;

class StaticColumnTraceProbe extends PortFixture {
    private final int methane=index("Methane"),heavy=index("crude_pc08");
    private static final double T=369,P=430000,DROP_HEIGHT=2,SLICE=5;
    private static final PipeResistance.Geometry DROP=new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    private FluidThermodynamics.State vessel(double t,double p,double[] n) {
        var unit=model.flashTP(t,p,n,NOOP);double[] m=n.clone();for(int c=0;c<m.length;c++)m[c]/=unit.volume();
        return model.flashTP(t,p,m,NOOP);
    }
    private PassiveNetwork island(double traceMoles,double lowerT,boolean warm) {
        double[] n=new double[mw.length];n[methane]=P/(FluidThermodynamics.R*T);n[heavy]=1e-2;
        n[heavy]=model.flashTP(T,P,n,NOOP).vapor()[heavy]+traceMoles;
        var upper=vessel(T,P,n);
        double[] g=new double[mw.length];double lower=P;FluidThermodynamics.State gas=null;
        for(int i=0;i<3;i++){g[methane]=lower/(FluidThermodynamics.R*lowerT);gas=vessel(lowerT,lower,g);lower=P+model.vaporDensity(gas)*PassiveStepSolver.GRAVITY*DROP_HEIGHT;}
        return new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,upper),new PassiveNetwork.Reservoir(2,-DROP_HEIGHT,gas)),
                List.of(new PassiveNetwork.Pipe(10,0,1,List.of(DROP),new FlowControl.Passive(),0,null,PhasePort.LIQUID,PhasePort.VAPOR)));
    }
    @Test void probe() {
        for(double lowerT:new double[]{369,430})for(boolean replay:new boolean[]{true,false}) {
            var graph=island(4.5e-5,lowerT,replay);var solver=new PassiveIntervalSolver(model);
            if(replay)solver.replayStart(graph,NetworkReduction.restInterval(graph,SLICE));
            System.out.println("D34P case lowerT="+lowerT+" replay="+replay+" drive="+Arrays.toString(NetworkReduction.drives(graph,model)[0]));
            for(int s=0;s<6&&!NetworkReduction.atRest(graph,model);s++) {
                try{var r=solver.solve(graph,SLICE,PassiveIntervalSolver.Settings.defaults(),NOOP);graph=r.graph();
                    System.out.println("D34P slice="+s+" acc="+r.acceptedSubsteps()+" rej="+r.rejectedSubsteps()+" reasons="+r.rejectionReasons()+" avg="+Arrays.toString(r.averageMassFlows())+" end="+Arrays.toString(r.endpointFlows())
                        +" upperLiq="+graph.reservoirs().get(0).state().liquidVolume()+" lowerLiq="+graph.reservoirs().get(1).state().liquidVolume()+" P="+graph.reservoirs().get(0).state().pressure()+"/"+graph.reservoirs().get(1).state().pressure());}
                catch(RuntimeException e){System.out.println("D34P slice="+s+" HELD "+e.getMessage());break;}
            }
        }
    }
}
