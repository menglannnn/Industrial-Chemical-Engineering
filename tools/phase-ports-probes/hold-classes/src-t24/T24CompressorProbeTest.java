package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** t24 probe (never commit): the GameTest compressor world, two placed (empty) tanks with a compressor between; and a
 * nitrogen generator compressed into an empty tank. Prints every node each second. */
class T24CompressorProbeTest {
    void dump(EventPathWorld w,String label) {
        for(var s:w.coordinator.snapshots()) {
            System.out.println("T24 "+label+" tick="+w.ledger.onlineTick()+" island="+s.id()+" committed="+s.clock().committedTick()+" "+s.status()
                    +" flows="+s.lastResult().map(r->java.util.Arrays.toString(r.averageMassFlows())).orElse("-")+" modes="+s.lastResult().map(r->r.endpointModes().toString()).orElse("-"));
            for(var node:s.graph().reservoirs())System.out.println("T24   node "+node.id()+" "+node.kind()+" empty="+node.empty()+" P="+node.state().pressure()
                    +" T="+node.state().temperature()+" m="+node.state().mass()+" inv="+java.util.Arrays.stream(node.inventory().moles()).sum()+" V="+node.state().volume());
        }
    }
    @Test void twoEmptyTanks() {
        var w=new EventPathWorld();w.runTo(100);
        w.place(0,0,0,Kind.RESERVOIR,NORTH,.05);w.place(4,0,0,Kind.RESERVOIR,NORTH,.05);w.place(2,0,0,Kind.COMPRESSOR,EAST,.05);
        w.place(1,0,0,Kind.PIPE,NORTH,.05);w.place(3,0,0,Kind.PIPE,NORTH,.05);
        for(int t=120;t<=100+20*60;t+=20){w.runTo(t);if(t<=400||t%200==0)dump(w,"empty");}
        for(var h:w.held)System.out.println("T24 HELD "+h);
    }
    @Test void nitrogenIntoAnEmptyTank() {
        var w=new EventPathWorld();w.runTo(100);
        w.place(0,0,0,Kind.GENERATOR,NORTH,new FlowControl.Passive(),EventPathWorld.pure(MaterialTestBasis.NITROGEN,298.15,150000),.05);
        w.place(4,0,0,Kind.RESERVOIR,NORTH,.05);w.place(2,0,0,Kind.COMPRESSOR,EAST,.05);
        w.place(1,0,0,Kind.PIPE,NORTH,.05);w.place(3,0,0,Kind.PIPE,NORTH,.05);
        for(int t=120;t<=100+20*120;t+=20){w.runTo(t);if(t<=400||t%400==0)dump(w,"gen");}
        for(var h:w.held)System.out.println("T24 HELD "+h);
    }
}
