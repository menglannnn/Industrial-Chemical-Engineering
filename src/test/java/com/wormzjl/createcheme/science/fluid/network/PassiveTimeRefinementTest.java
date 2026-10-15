package com.wormzjl.createcheme.science.fluid.network;

import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;

class PassiveTimeRefinementTest {
    @Test void adaptiveGasBlowdownMatchesIndependentlyRefinedFixedSteps() throws Exception {
        var model=com.wormzjl.createcheme.fluid.support.FluidTestSupport.model(MaterialCatalog.bundled(),"createcheme:tjl20_methane",1e-9);
        var reservoirs=new ArrayList<PassiveNetwork.Reservoir>();
        for(double p:new double[]{200000,101325}) {
            double[] n=new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.CRUDE+1];n[0]=1;var unit=model.flashTP(350,p,n,()->{});n[0]/=unit.volume();
            reservoirs.add(new PassiveNetwork.Reservoir(reservoirs.size(),0,model.flashTP(350,p,n,()->{})));
        }
        var graph=new PassiveNetwork(reservoirs,List.of(new PassiveNetwork.Pipe(1,0,1,new PipeResistance.Geometry(10,.05,.000045,0))));
        var adaptive=new PassiveIntervalSolver(model).solve(graph,1,PassiveIntervalSolver.Settings.defaults(),()->{});
        var reference=graph;var step=new PassiveStepSolver(model);double integrated=0;
        for(int i=0;i<400;i++){var result=step.solve(reference,.0025,()->{});reference=PassiveIntervalSolver.replace(reference,result);integrated+=result.massFlows()[0]*.0025;}
        double maxP=0,maxT=0;
        for(int i=0;i<2;i++) {
            var a=adaptive.graph().reservoirs().get(i).state();var b=reference.reservoirs().get(i).state();
            maxP=Math.max(maxP,Math.abs(a.pressure()/b.pressure()-1));maxT=Math.max(maxT,Math.abs(a.temperature()-b.temperature()));
        }
        double flowError=Math.abs(adaptive.averageMassFlows()[0]/integrated-1);
        // Backward Euler (WP1 of the mixed-gas junction batch): first order in time. Measured 0.00475 / 0.496 K / 0.0141
        // against 400 fixed 2.5 ms steps; the TR-BDF2 bounds were 0.005 / 0.5 K / 0.005.
        assertTrue(maxP<=.0075,"pressure "+maxP);assertTrue(maxT<=.75,"temperature "+maxT);assertTrue(flowError<=.02,"integrated mass "+flowError);
    }
}
