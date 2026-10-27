package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;

/** D36 scratch: RestResolutionTest's two D36 gate graphs without assertions: drives, the gauge, and three 5 s steps. */
class D36GateProbe {
    final FluidThermodynamics model=FluidTestSupport.networkModel();
    final PipeResistance.Geometry run=new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0),riser=new PipeResistance.Geometry(2,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    FluidThermodynamics.State nitrogen(double t){return model.initialNitrogenCharge(1,t,480371.272983223,()->{});}
    void report(String label,PassiveNetwork graph) {
        System.out.println(label+" drives="+Arrays.deepToString(NetworkReduction.drives(graph,model))+" atRest="+NetworkReduction.atRest(graph,model)+" loopDriven="+NetworkReduction.loopDriven(graph,NetworkReduction.drives(graph,model)));
        var solver=new PassiveStepSolver(model);
        for(int step=0;step<3;step++) {
            var result=solver.solve(graph,5,()->{});
            var nodes=new ArrayList<PassiveNetwork.Reservoir>();var p=new StringBuilder();
            for(int i=0;i<graph.reservoirs().size();i++){var node=graph.reservoirs().get(i);nodes.add(new PassiveNetwork.Reservoir(node.id(),node.elevation(),result.states().get(i),node.kind(),result.inventories().get(i)));p.append(' ').append(result.states().get(i).pressure());}
            graph=new PassiveNetwork(nodes,graph.pipes(),graph.scheduledTransfers());
            System.out.println(label+" step="+step+" flows="+Arrays.toString(result.massFlows())+" P="+p);
        }
    }
    @Test void gates() {
        double cold=272.72073259613035,hot=cold+100;
        report("D36PROBE_RISER",new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,nitrogen(cold)),new PassiveNetwork.Reservoir(2,2,nitrogen(cold))),List.of(new PassiveNetwork.Pipe(10,0,1,riser))));
        report("D36PROBE_LOOP",new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,nitrogen(cold)),new PassiveNetwork.Reservoir(2,0,nitrogen(hot)),
                new PassiveNetwork.Reservoir(3,2,nitrogen(hot)),new PassiveNetwork.Reservoir(4,2,nitrogen(cold))),
                List.of(new PassiveNetwork.Pipe(10,0,1,run),new PassiveNetwork.Pipe(11,1,2,riser),new PassiveNetwork.Pipe(12,2,3,run),new PassiveNetwork.Pipe(13,3,0,riser))));
    }
}
