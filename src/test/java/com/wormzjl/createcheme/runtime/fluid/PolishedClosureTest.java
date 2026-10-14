package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * Thread t32: a point the D13 polish re-solves from the reconstruction may show a drive the converged point did not, a
 * one-way end that would receive; the pass loop closes that boundary and solves the pass again, as it does for any
 * converged point, instead of refusing the step.
 */
class PolishedClosureTest {
    /**
     * The smallest world of random fuzz seed 71 on the close's code (9 blocks, all placed at tick 207; minimized in
     * tools/phase-ports-probes/hold-classes logs cw05): a 317 K water generator at 334 kPa fills an empty 1 m3 tank 3 m
     * below it, beside a 146 kPa generator its lines may not feed. Full, the tank stands at 363.4 kPa (the generator and
     * the column) and nothing flows. The step's Newton accepted its start, the tank 1e-11 of its volume over its capacity
     * (within the small headspace's tolerance); the booking stated the tank at its contents, 0.01 Pa above what the
     * generator holds (water's stiffness times 1e-11), and the line into it read that as 2.8e-8 against the 1e-8 equation
     * gate. The polish converged (1.2e-14) with that 0.01 Pa pushing water back into the generator, a forbidden direction,
     * and the step was refused at every step size (held at tick 734). With the generator's line closed the island keeps up
     * for 120 slices.
     */
    @Test void aFullTankAboveItsGeneratorClosesTheGeneratorsLine() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] water=new double[21];water[20]=1;
        double[] air=new double[21];air[19]=1;
        w.runTo(207);
        w.place(1,-2,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(1,1,-1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(1,-1,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.01);
        w.place(2,1,-1,Kind.PIPE,NORTH,passive,null,.1);
        w.place(2,0,-1,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,297.6508444710189,146277.4155068182,water),.1);
        w.place(0,1,-1,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1,317.097467877553,334274.03661019984,water),.02);
        w.place(0,-2,-1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1,298.15,101325,air),.01);
        w.runTo(207+120*100);w.assertKeptUp("a tank filled full of water by a generator");
    }
}
