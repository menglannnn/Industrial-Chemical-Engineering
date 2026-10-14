package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The pump of defect F1 on a tank that a generator keeps fed, run long enough for the pump junction's
 * nitrogen trace to wash out: 400 slices of 5 s without a hold. Before the trace flush (owner decision 2026-09-27) the
 * island held at slice 173 when the junction was seeded from the tank. */
class TraceFlushFedPumpTest {
    @Test void aPumpedWaterLineRunsThroughTheWholeLifetimeOfItsJunctionsGasTrace() {
        var base=new PumpBottomPortDrainTest();var model=base.model;long tankId=3;var tank=base.filledTank(tankId);
        int y=PumpBottomPortDrainTest.Y;
        var devices=List.of(PumpBottomPortDrainTest.device(7,-2,y,Kind.GENERATOR),PumpBottomPortDrainTest.device(8,-1,y,Kind.PIPE),
                PumpBottomPortDrainTest.device(tankId,0,y,Kind.RESERVOIR),
                PumpBottomPortDrainTest.device(9,0,y-1,Kind.PIPE),PumpBottomPortDrainTest.device(4,0,y-2,Kind.PUMP,PhysicalFluidTopology.Direction.DOWN,new FlowControl.Pump(.001,500000,1)),
                PumpBottomPortDrainTest.device(5,0,y-3,Kind.PIPE),PumpBottomPortDrainTest.device(6,0,y-4,Kind.VOID));
        var graph=base.island(devices,Map.of(7L,new com.wormzjl.createcheme.runtime.fluid.FluidDeviceSpec(1,298.15,130000,PumpBottomPortDrainTest.water()),
                6L,new com.wormzjl.createcheme.runtime.fluid.FluidDeviceSpec(1,298.15,101325,PumpBottomPortDrainTest.nitrogen())),Map.of(tankId,tank));
        var retained=new RetainedSolver();PassiveIntervalSolver.Result last=null;
        for(int slice=0;slice<400;slice++) {
            retained.committed(last);PassiveIntervalSolver.Result result;
            try{result=retained.solve(model,graph,5,PassiveIntervalSolver.Settings.defaults(),()->{});}
            catch(RuntimeException held){System.out.println("FED_PUMP HELD at slice "+slice+": "+held.getMessage());fail(held.getMessage());return;}
            graph=result.graph();last=result;
            var line=new StringBuilder("FED_PUMP slice="+slice+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps()+" modes="+result.endpointModes());
            for(var node:graph.reservoirs())if(node.junction())line.append(" junction ").append(node.id()).append(" vapour=").append(Arrays.stream(node.state().vaporView()).sum()).append(" wV=").append(node.state().waterVapor());
            var t=graph.reservoirs().get(PumpBottomPortDrainTest.node(graph,tankId)).state();line.append(" tank P=").append(t.pressure()).append(" water=").append(model.liquidMass(t));
            System.out.println(line);
        }
    }
}
