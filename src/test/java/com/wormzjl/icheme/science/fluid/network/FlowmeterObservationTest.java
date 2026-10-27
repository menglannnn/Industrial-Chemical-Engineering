package com.wormzjl.icheme.science.fluid.network;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FlowmeterObservationTest extends PortFixture {
    @Test void acceptedGasFlowIsMeasuredAtEachLocalPressureWithoutChangingTheSolve(){
        var nodes=List.of(new PassiveNetwork.Reservoir(1,0,nitrogenAt(500000),PassiveNetwork.NodeKind.GENERATOR),new PassiveNetwork.Reservoir(2,0,nitrogenAt(101325),PassiveNetwork.NodeKind.VOID));
        var pipe=new PassiveNetwork.Pipe(10,0,1,new PipeResistance.Geometry(20,.02,.000045,0));
        var plain=new PassiveStepSolver(model).solve(new PassiveNetwork(nodes,List.of(pipe)),.1,NOOP);
        var points=List.of(new PassiveNetwork.Pipe.ObservationPoint(20,.2,0,false),new PassiveNetwork.Pipe.ObservationPoint(21,.8,0,false));
        var measured=new PassiveStepSolver(model).solve(new PassiveNetwork(nodes,List.of(pipe.withObservationPoints(points))),.1,NOOP);
        assertArrayEquals(plain.massFlows(),measured.massFlows());
        var samples=measured.observations().getFirst().flowSamples();assertEquals(2,samples.size());
        assertTrue(samples.get(1).volumeFlow()>samples.get(0).volumeFlow(),"lower local pressure means larger actual volume flow");
        for(var sample:samples){
            double[] n=new double[mw.length];n[nitrogen]=1;var local=model.flashTP(298.15,sample.pressure(),n,NOOP);
            assertEquals(sample.massFlow()*local.volume()/local.mass(),sample.volumeFlow(),1e-10*sample.volumeFlow());
        }
        assertEquals(pipe.identity(),pipe.withObservationPoints(points).identity(),"observing does not change hydraulic equations");
        assertEquals(points,pipe.withObservationPoints(points).withBlockedDirections(0).observationPoints());
    }
}
