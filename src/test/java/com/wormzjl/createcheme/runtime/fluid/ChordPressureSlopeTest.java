package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Thread t29: a chord step reuses the Jacobian only while it still closes at least half of every row's gap, the rows'
 * slopes in their ends' pressures included (a row's slope in {@code ln P} is the pressure itself).
 */
class ChordPressureSlopeTest {
    /**
     * The smallest world of random fuzz seed 886 (18 blocks, all placed at tick 239; minimized in
     * tools/phase-ports-probes/hold-classes logs fx16): a 490 kPa methane generator feeding two placed tanks and, through a
     * filter, a void, every run 10 mm at its narrowest. The step's active set is the real one (the line after the filter
     * choked, the filter on its law at 4.7e-4 below its own cap, 105 Pa across it), but the filter's two junctions started
     * 373 kPa apart and walked from about 143 kPa, where the last Jacobian was built, to 222 kPa: the chord steps stated the
     * filter row's pressure columns at 1/1.55 of their own, overshot by 0.55 per step, and the Newton ran out of iterations
     * at 1.9e-3 at every step size (held at tick 240). With the pressure slopes in the chord test the island keeps up for
     * 120 slices.
     */
    @Test void aFilterBetweenTwoWanderingJunctionsConverges() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[19]=1;double[] methane=new double[21];methane[0]=1;
        w.runTo(239);
        w.place(0,0,0,Kind.FILTER,SOUTH,passive,null,.01);
        w.place(0,0,4,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,1,4,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,-3,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.01);
        w.place(0,-3,-2,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.02);
        w.place(0,1,5,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(-3,0,3,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,372.0615276045455,490111.61641710874,methane),.01);
        w.place(-2,0,3,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,-2,-2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,1,6,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,3,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,1,7,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(0,-1,-2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,0,3,Kind.PIPE,NORTH,passive,null,.01);
        w.runTo(239+120*100);w.assertKeptUp("a filter between two wandering junctions");
    }
}
