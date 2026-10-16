package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Owner decision D59 (4) of the phase-ports batch (A335): a pump cannot pull its suction below the vapour pressure of the
 * liquid it draws. There it cavitates and delivers only what its suction line gives with the suction standing at
 * {@code P_vapour(T)}: the row {@code P_suction = P_vapour(T_suction)} replaces the target-flow row while the target would
 * pull the suction below it, as the head limit replaces it while the target would need more head than the pump has.
 */
class PumpCavitationTest {
    /**
     * The smallest world of world-defaults fuzz seed 696 (11 blocks, all placed at tick 143; minimized in
     * tools/phase-ports-probes/hold-classes logs cc04). A 1 atm water generator 2 m above the pump's suction feeds it through
     * a vertical line; the pump discharges towards a second 1 atm generator and a placed tank 3 m below. The head-limit trial
     * pulls the suction junction to the data's 100 Pa floor, where the water flashes to its triple point: without the suction
     * limit the island held at tick 1570 (water at 273.1599 K at the suction junction, the ice hold of the time; since WP4 of
     * the range-extrapolation plan such a flash freezes, 83 % ice and 17 % steam at 252.8 K from 25 °C at 100 Pa,
     * FreeWaterIceTest, which a step carries as the stand-in, supercooled liquid, until a vessel's refresh freezes it). With
     * it the failed trial puts the pump
     * on its suction limit, that pass converges with the suction line giving more than the target (22.5 kg/s), the pump
     * returns to its target and then its head limit as the full tank's pressure rises, and stands at its shutoff once the
     * tank is pressed to it; the island keeps up for 120 slices.
     */
    @Test void aHeadLimitTrialBelowTheVapourPressureIsSolvedOnTheSuctionLimit() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        var water=EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325);
        w.runTo(143);
        w.place(0,0,-3,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-3,1,Kind.RESERVOIR,NORTH,.05);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,2,-3,Kind.GENERATOR,NORTH,passive,water,.05);
        w.place(0,1,-3,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-1,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,-2,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,0,Kind.PUMP,SOUTH,new FlowControl.Pump(.01,500000,1),null,.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,2,Kind.GENERATOR,NORTH,passive,water,.05);
        w.runTo(143+120*100);w.assertKeptUp("a head-limit trial below the vapour pressure");
        for(var s:w.coordinator.snapshots()) {
            var result=s.lastResult().orElseThrow();
            System.out.println("PUMP_CAVITATION_696 island "+s.id()+" modes="+result.endpointModes()+" flows="+Arrays.toString(result.averageMassFlows()));
            for(int e=0;e<s.graph().pipes().size();e++)if(s.graph().pipes().get(e).control() instanceof FlowControl.Pump)
                assertEquals(FlowControl.Mode.CLOSED,result.endpointModes().get(e),"the pump stands at its shutoff against the full tank");
        }
    }

    /**
     * Cavitation as the steady state: a 1 atm water generator feeds a pump through two 1 m pipes of 30 mm bore, and the pump
     * discharges into a 1 atm void. The target, 0.01 m3/s (10 kg/s), would need about 134 kPa of suction-line loss, more than
     * the 98 kPa between the generator and the vapour pressure of water at 298 K (3.17 kPa): the pump stands on its suction
     * limit, its suction junction at the vapour pressure, and delivers what the line gives there, below its target.
     */
    @Test void aPumpWhoseSuctionLineCannotFeedItsTargetStandsOnItsSuctionLimit() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        w.runTo(100);
        w.place(0,0,-3,Kind.GENERATOR,NORTH,passive,EventPathWorld.pure(MaterialTestBasis.NETWORK,298.15,101325),.05);
        w.place(0,0,-2,Kind.PIPE,NORTH,passive,null,.03);
        w.place(0,0,-1,Kind.PIPE,NORTH,passive,null,.03);
        w.place(0,0,0,Kind.PUMP,SOUTH,new FlowControl.Pump(.01,500000,1),null,.05);
        w.place(0,0,1,Kind.PIPE,NORTH,passive,null,.05);
        w.place(0,0,2,Kind.VOID,NORTH,passive,null,.05);
        w.runTo(100+20*100);w.assertKeptUp("a pump on a starved suction");
        var s=w.coordinator.snapshots().getFirst();var result=s.lastResult().orElseThrow();var graph=s.graph();
        int pump=-1;for(int e=0;e<graph.pipes().size();e++)if(graph.pipes().get(e).control() instanceof FlowControl.Pump)pump=e;
        var suction=graph.reservoirs().get(graph.pipes().get(pump).first()).state();
        double vapour=w.model.saturationPressure(suction.temperature()),q=result.averageMassFlows()[pump];
        System.out.println("PUMP_CAVITATION_STEADY modes="+result.endpointModes()+" flows="+Arrays.toString(result.averageMassFlows())
                +" suctionP="+suction.pressure()+" vapourP="+vapour);
        assertEquals(FlowControl.Mode.PUMP_SUCTION_LIMIT,result.endpointModes().get(pump),"the pump cavitates");
        assertEquals(vapour,suction.pressure(),1e-3*vapour,"its suction stands at the vapour pressure");
        assertTrue(q>0&&q<.01*suction.mass()/suction.volume(),"it delivers what the line gives, below its target: "+q);
    }
}
