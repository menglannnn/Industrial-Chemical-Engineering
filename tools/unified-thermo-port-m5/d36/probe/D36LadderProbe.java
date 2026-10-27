package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** D36 scratch: HoldClassWorldsTest's TurbulentWalk world (random 498's dead-end ladder), its flows and pressures every
 * 1000 ticks (D36LADDER) and in full at the end (D36LADDER_END). -Dd36.end=<tick> sets the end (default 12100). */
class D36LadderProbe {
    @Test void ladder() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(0,3,-3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NITROGEN,272.72073259613035,480371.272983223),.01);
        w.place(0,3,-2,Kind.PIPE,NORTH,.01);
        w.place(0,1,0,Kind.PIPE,NORTH,.01);
        w.place(0,2,-1,Kind.PIPE,NORTH,.02);
        w.place(0,0,-1,Kind.PIPE,NORTH,.02);
        w.place(0,1,-1,Kind.PIPE,NORTH,.02);
        w.place(0,3,-1,Kind.PIPE,NORTH,.01);
        w.place(0,0,0,Kind.PIPE,NORTH,.05);
        w.place(0,2,0,Kind.PIPE,NORTH,.01);
        int end=Integer.getInteger("d36.end",100+120*100);
        for(int tick=200;tick<=end;tick+=tick<1100?100:1000) {
            w.runTo(tick);
            for(var s:w.coordinator.snapshots()) {
                var nodes=s.graph().reservoirs();double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY,max=0;
                for(var n:nodes){lo=Math.min(lo,n.state().pressure());hi=Math.max(hi,n.state().pressure());}
                var flows=s.lastResult().map(r->r.averageMassFlows()).orElse(new double[0]);for(double q:flows)max=Math.max(max,Math.abs(q));
                System.out.println("D36LADDER tick="+tick+" island="+s.id()+" status="+s.status()+" committed="+s.clock().committedTick()+" maxFlow="+max+" P=["+lo+","+hi+"] spread="+(hi-lo));
            }
        }
        for(var s:w.coordinator.snapshots()) {
            var g=s.graph();
            for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);System.out.println("D36LADDER_END node "+i+" id="+n.id()+" kind="+n.kind()+" z="+n.elevation()+" P="+n.state().pressure()+" T="+n.state().temperature());}
            var flows=s.lastResult().map(r->r.averageMassFlows()).orElse(new double[0]);
            for(int i=0;i<g.pipes().size();i++){var p=g.pipes().get(i);System.out.println("D36LADDER_END pipe "+i+" "+p.first()+"-"+p.second()+" dz="+(g.reservoirs().get(p.second()).elevation()-g.reservoirs().get(p.first()).elevation())+" flow="+(i<flows.length?flows[i]:Double.NaN));}
        }
        w.assertKeptUp("ladder probe");
    }
}
