package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Thread t29: a vessel that starts a step holding nothing gives nothing in that step, whether the pass laid it out dry or
 * laid it out from what reached it (decision D54's dry end, read by every booking of a converged or polished point).
 */
class DryEndOutflowTest {
    /**
     * The smallest world of random fuzz seed 708 (8 blocks, all placed at tick 360; minimized in
     * tools/phase-ports-probes/hold-classes logs fx03): a 366 K crude generator at 172 kPa feeding a 200 kPa relief valve,
     * an empty placed tank looped by two lines to one junction, and a void. The pass's flows reached the empty tank with
     * 1.6e-22 kg/s, so it was laid out from that; the next pass converged with the same 1.6e-22 kg/s in by one line and out
     * by the other, and the booking, which starts the tank from nothing, ended it at exactly zero: "Candidate overdraws a
     * reservoir" at every step size (held at tick 361). With an outflow from a dry end within the forbidden-direction
     * tolerance read as no flow, the tank keeps the roundoff it received and the island keeps up for 120 slices.
     */
    @Test void anEmptyTankLoopedToOneJunctionGivesNothing() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[19]=1;
        double[] crude={0.005000000000000001,0.0011908995509034748,0.010591918105298616,0.006510980717557732,0.0222592523684086,0.01663176428669243,
                0.024555314957131154,0.16629362769970432,0.12314847499525976,0.1177490756868832,0.10469103210175804,0.08701659237073239,0.07336455325619681,
                0.06154184102834706,0.06057002423058549,0.06607465545777229,0.02967631275527244,0.015001342402485261,0.008132338029011052,0.0,0.0};
        w.runTo(360);
        w.place(0,0,4,Kind.PIPE,NORTH,passive,null,.1);
        w.place(0,0,6,Kind.VALVE,NORTH,new FlowControl.PressureValve(200000),null,.01);
        w.place(-1,0,5,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.05);
        w.place(-1,0,7,Kind.VOID,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,water),.1);
        w.place(0,0,5,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,0,7,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,365.5491586603084,172266.18504004818,crude),.05);
        w.place(0,0,7,Kind.PIPE,NORTH,passive,null,.05);
        w.place(-1,0,4,Kind.PIPE,NORTH,passive,null,.05);
        w.runTo(360+120*100);w.assertKeptUp("an empty tank looped to one junction");
    }
}
