package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D59 (5), the void driven the forbidden way (A342), t28: an end that may not fill a connection has no
 * column of its own. A void only receives, so its air never stands in the line to it; the line stands in what the other
 * end sends. Before, a water junction standing below the ambient just above a void was given the void's air column, on
 * which its drive points out of the void: the line started on its cap with the void supplying air, the pass converged
 * there and the pass loop closed the line for the solve, on every step; the water above the outlet never fell into it.
 */
class VoidColumnTest {
    /**
     * World defaults 244 as placed (28 blocks, placements from tick 149 to 2052; tools/phase-ports-probes/hold-classes logs
     * cc25). A 1 atm water generator at y = 2 feeds a pipe run at y = 2 whose second pipe stands directly above a void at
     * y = 1; the run turns down a column to a lower void, which draws the run to about 93 to 98 kPa. The water in the run
     * still falls 1 m into the upper void: its column drives about 1.9 to 6.7 kPa down, where the void's air column read 3 kPa
     * up. With the rule the upper void receives water (8.4 kg/s at the end); without it its line was closed at every step and
     * the upper void received nothing (traced in logs cc20 and cc21).
     */
    @Test void waterAboveAnOutletFallsIntoIt() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
        w.runTo(149);w.place(-1,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(170);w.place(-1,0,-1,Kind.PIPE,NORTH,passive,null,.05);w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(205);w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(215);long upper=w.place(2,1,-4,Kind.VOID,NORTH,.05);
        w.runTo(341);w.place(0,0,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(537);w.place(3,2,-4,Kind.GENERATOR,NORTH,passive,water,.05);w.place(0,0,-3,Kind.VOID,NORTH,.05);
        w.runTo(584);w.place(-3,0,-5,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(616);w.place(-2,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(623);w.place(1,1,-4,Kind.PIPE,NORTH,passive,null,.05);w.place(-4,0,-5,Kind.COMPRESSOR,EAST,new FlowControl.Compressor(.05,3,1),null,.05);
        w.place(0,4,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(659);w.place(-3,0,-2,Kind.VALVE,WEST,new FlowControl.PressureValve(200000),null,.05);
        w.runTo(745);w.place(0,1,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(749);w.place(0,5,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1002);w.place(-2,0,-5,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1023);w.place(0,2,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1271);w.place(-3,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1323);w.place(-2,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1454);w.place(0,3,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1724);w.place(-1,0,-3,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1740);w.place(2,2,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(1998);w.place(0,0,-5,Kind.VALVE,NORTH,new FlowControl.PressureValve(200000),null,.05);
        w.runTo(2007);w.place(-1,0,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(2020);w.place(0,0,0,Kind.VALVE,SOUTH,new FlowControl.PressureValve(200000),null,.05);
        w.runTo(2021);w.place(1,2,-4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(2052);w.place(-1,0,-5,Kind.VOID,NORTH,.05);
        w.runTo(2052+20*100);w.assertKeptUp("world defaults 244");
        // A void in several islands (owner decision D17) receives from each of them.
        double received=0;
        for(var s:w.coordinator.snapshots()) {
            var graph=s.graph();int node=-1;for(int i=0;i<graph.reservoirs().size();i++)if(graph.reservoirs().get(i).id()==upper)node=i;
            if(node<0||s.lastResult().isEmpty())continue;var q=s.lastResult().orElseThrow().averageMassFlows();
            for(int e=0;e<graph.pipes().size();e++){var pipe=graph.pipes().get(e);if(pipe.second()==node)received+=q[e];if(pipe.first()==node)received-=q[e];}
            System.out.println("VOID_COLUMN island "+s.id()+" status="+s.status()+" flows="+Arrays.toString(q));
        }
        System.out.println("VOID_COLUMN upperVoidReceives="+received);
        assertTrue(received>.1,"the upper void receives the water that falls into it: "+received+" kg/s");
    }
}
