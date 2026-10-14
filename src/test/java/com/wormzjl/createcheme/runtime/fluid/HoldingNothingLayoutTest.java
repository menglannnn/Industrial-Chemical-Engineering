package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Thread t29, owner decision D59 (2): a vessel that holds nothing at the bag's resolution (1e-12 of its capacity) is laid
 * out dry, and a roundoff that reaches it is not an arrival: it stays booked in the inventory and the vessel stays dry.
 */
class HoldingNothingLayoutTest {
    /**
     * The smallest world of random fuzz seed 708 after its first hold was cured (17 blocks, all placed at tick 332;
     * minimized in tools/phase-ports-probes/hold-classes logs fx30): a 366 K crude generator at 172 kPa, a 200 kPa relief
     * valve, two placed tanks and two voids. A pass's flows reached an empty tank with 2e-29 kg of crude; the pass loop laid
     * the tank out from it, as two phases, and its fugacity rows, stated for that roundoff, stalled the line search at every
     * step size (held at tick 333). With the roundoff read as nothing the tank stays dry and the island keeps up for 120
     * slices.
     */
    @Test void aRoundoffArrivalLeavesAnEmptyTankDry() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[19]=1;
        double[] crude={0.005000000000000001,0.0011908995509034748,0.010591918105298616,0.006510980717557732,0.0222592523684086,0.01663176428669243,
                0.024555314957131154,0.16629362769970432,0.12314847499525976,0.1177490756868832,0.10469103210175804,0.08701659237073239,0.07336455325619681,
                0.06154184102834706,0.06057002423058549,0.06607465545777229,0.02967631275527244,0.015001342402485261,0.008132338029011052,0.0,0.0};
        w.runTo(332);
        w.place(0,0,0,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,4,Kind.PIPE,NORTH,passive,null,.1);
        w.place(0,0,6,Kind.VALVE,NORTH,new FlowControl.PressureValve(200000),null,.01);
        w.place(0,-1,1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(0,-1,0,Kind.PIPE,NORTH,passive,null,.01);
        w.place(0,0,2,Kind.PIPE,NORTH,passive,null,.1);
        w.place(-1,0,5,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(2,0,2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(3,0,2,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.02);
        w.place(-1,0,7,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.1);
        w.place(1,0,2,Kind.PIPE,NORTH,passive,null,.02);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(0,0,5,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,0,7,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,365.5491586603084,172266.18504004818,crude),.05);
        w.place(0,0,7,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,4,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,3,Kind.PIPE,NORTH,passive,null,.1);
        w.runTo(332+120*100);w.assertKeptUp("a roundoff arrival in an empty tank");
    }
}
