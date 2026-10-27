package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** D36 scratch: HoldClassWorldsTest's drain-side gas column world (seed 992), its nodes and flows at the end. */
class D36DrainSideProbe {
    @Test void drainSide() {
        var w=new EventPathWorld();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
        w.runTo(132);
        w.place(-3,-1,0,Kind.PIPE,NORTH,.05);w.place(-3,0,0,Kind.PIPE,NORTH,.05);w.place(-2,-1,-1,Kind.PIPE,NORTH,.05);
        w.place(-1,0,0,Kind.PIPE,NORTH,.05);w.place(0,0,0,Kind.RESERVOIR,NORTH,.05);w.place(-1,0,-2,Kind.RESERVOIR,NORTH,.05);
        w.place(-2,0,0,Kind.PIPE,NORTH,.05);w.place(-2,-1,0,Kind.PIPE,NORTH,.05);w.place(-2,-1,-2,Kind.GENERATOR,NORTH,new FlowControl.Passive(),water,.05);
        w.place(-1,0,-1,Kind.PIPE,NORTH,.05);
        w.runTo(165);
        w.place(-1,0,-3,Kind.PIPE,NORTH,.05);
        w.runTo(165+120*100);w.assertKeptUp("drain-side probe");
        for(var s:w.coordinator.snapshots()) {
            var g=s.graph();System.out.println("D36DRAIN island="+s.id()+" status="+s.status());
            for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);System.out.println("D36DRAIN node "+i+" id="+n.id()+" kind="+n.kind()+" y="+n.elevation()+" P="+n.state().pressure()+" liquid="+(n.state().liquidVolume()+n.state().waterVolume())+" vapour="+n.state().vaporVolume());}
            var flows=s.lastResult().map(r->r.averageMassFlows()).orElse(new double[0]);
            for(int i=0;i<g.pipes().size();i++){var p=g.pipes().get(i);System.out.println("D36DRAIN pipe "+i+" "+p.first()+"-"+p.second()+" flow="+(i<flows.length?flows[i]:Double.NaN));}
        }
    }
}
