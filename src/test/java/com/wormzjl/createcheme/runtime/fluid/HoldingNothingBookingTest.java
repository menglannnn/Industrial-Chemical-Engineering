package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Thread t32, owner decision D59 (2): a vessel that holds nothing at the bag's resolution (1e-12 of its capacity) after a
 * step's booking keeps its residue in its inventory and is stated at its flash; no closure is solved for it.
 */
class HoldingNothingBookingTest {
    /**
     * The smallest world of random fuzz seed 707 on the close's code (12 blocks; minimized in
     * tools/phase-ports-probes/hold-classes logs cw06): a 327 K crude generator at 371 kPa and a 311 K water generator at
     * 363 kPa filling three empty tanks, the water line joined at tick 165. The booking reached a tank holding 1e-42 mol of
     * crude and 1e-84 mol of water vapour and closed its state by a Newton whose water column found no finite difference
     * trial at any step size (held at tick 167). With the residue stated at its flash the island keeps up for 120 slices.
     */
    @Test void aResidueIsBookedWithoutAClosure() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[20]=1;
        double[] air=new double[21];air[19]=1;
        double[] crude={0.005000000000000001,0.0011908995509034748,0.010591918105298616,0.006510980717557732,0.0222592523684086,0.01663176428669243,
                0.024555314957131154,0.16629362769970432,0.12314847499525976,0.1177490756868832,0.10469103210175804,0.08701659237073239,0.07336455325619681,
                0.06154184102834706,0.06057002423058549,0.06607465545777229,0.02967631275527244,0.015001342402485261,0.008132338029011052,0.0,0.0};
        w.runTo(142);
        w.place(0,-3,0,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,326.6101199519573,371434.1948684311,crude),.02);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,-2,-2,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,311.4239803319713,362954.9489303839,water),.05);
        w.place(0,-2,0,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,-2,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-2,0,2,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,air),.01);
        w.place(0,1,1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,air),.05);
        w.place(-1,0,2,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,0,Kind.RESERVOIR,WEST,passive,new FluidDeviceSpec(1,298.15,101325,air),.05);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.02);
        w.runTo(165);
        w.place(0,-2,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(165+120*100);w.assertKeptUp("a residue booked into an empty tank");
    }
}
