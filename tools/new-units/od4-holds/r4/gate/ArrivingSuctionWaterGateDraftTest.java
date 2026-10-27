package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Draft gate (t285 item 2, not tracked): bundled fuzz seed 889 pair-minimized to 11 blocks (held E9 with the junction's
 * arriving-phase seed as the TP flash at the mass-weighted temperature, clean with the PH split of 8fd61a13). A 326.6 K,
 * 398 kPa water generator 3 m up feeds, through 20 and 10 mm lines, a junction beside a nitrogen tank and a 10 mm suction
 * line into a pump [0.001 m3/s, 1 MPa]; the pump discharges through 10 mm into a second nitrogen tank. Its target needs
 * more suction-line loss than the supply has above the water's vapour pressure (14.6 kPa at 326.6 K), so it stands on its
 * suction limit (D59 (4)). Water reaching the suction junction at its vapour pressure is liquid beside a trace of steam
 * (6.6e-6 of its mass, the latent heat the PH split keeps); the TP flash at the mixed temperature made all of it steam and
 * the suction limit's row had no vapour pressure to stand at.
 */
class ArrivingSuctionWaterGateDraftTest {
    @Test void waterArrivingAtACavitatingSuctionStaysLiquid() {
        var w=new EventPathWorld();var passive=new FlowControl.Passive();
        double[] n2=new double[21];n2[19]=1;double[] water=new double[21];water[20]=1;
        w.runTo(152);w.place(2,0,0,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1.0,298.15,101325.0,n2),0.01);
        w.runTo(194);w.place(-2,0,2,Kind.PIPE,NORTH,passive,null,0.01);
        w.runTo(255);w.place(-1,0,1,Kind.PIPE,NORTH,passive,null,0.02);
        w.runTo(312);w.place(-2,0,1,Kind.PIPE,NORTH,passive,null,0.01);
        w.runTo(504);w.place(-2,2,2,Kind.PIPE,NORTH,passive,null,0.02);
        w.runTo(1442);w.place(1,0,0,Kind.PIPE,NORTH,passive,null,0.01);
        w.runTo(1496);w.place(0,0,1,Kind.RESERVOIR,NORTH,passive,new FluidDeviceSpec(1.0,298.15,101325.0,n2),0.02);
        w.runTo(2341);w.place(-2,1,2,Kind.PIPE,NORTH,passive,null,0.02);
        w.runTo(2642);w.place(-2,3,2,Kind.GENERATOR,NORTH,passive,new FluidDeviceSpec(1.0,326.5875545469788,397553.9461096364,water),0.02);
        w.runTo(2873);w.place(-1,0,0,Kind.PIPE,NORTH,passive,null,0.01);
        w.place(0,0,0,Kind.PUMP,EAST,new FlowControl.Pump(0.001,1000000.0),null,0.05);
        long end=Long.getLong("gate.end",2873+120*100);
        w.runTo(end);w.assertKeptUp("water arriving at a cavitating pump suction");
        var s=w.coordinator.snapshots().stream().filter(sn->sn.graph().pipes().stream().anyMatch(p->p.control() instanceof FlowControl.Pump)).findFirst().orElseThrow();
        var result=s.lastResult().orElseThrow();var graph=s.graph();
        int pump=-1;for(int e=0;e<graph.pipes().size();e++)if(graph.pipes().get(e).control() instanceof FlowControl.Pump)pump=e;
        var suction=graph.reservoirs().get(graph.pipes().get(pump).first()).state();
        double vapour=w.model.saturationPressure(suction.temperature()),q=result.averageMassFlows()[pump];
        double steamShare=suction.waterVapor()/(suction.waterLiquid()+suction.waterVapor());
        System.out.println("GATE889 status="+s.status()+" modes="+result.endpointModes()+" flows="+Arrays.toString(result.averageMassFlows())+" suctionP="+suction.pressure()
                +" vapourP="+vapour+" suctionT="+suction.temperature()+" wl="+suction.waterLiquid()+" wv="+suction.waterVapor()+" steamMolShare="+steamShare);
        assertEquals(FlowControl.Mode.PUMP_SUCTION_LIMIT,result.endpointModes().get(pump),"the pump cavitates");
        assertEquals(vapour,suction.pressure(),1e-3*vapour,"its suction stands at the vapour pressure");
        assertTrue(suction.waterLiquid()>0&&steamShare<1e-3,"the suction junction holds liquid water beside a trace of steam: "+steamShare);
        assertTrue(q>0&&q<.001*suction.mass()/suction.volume(),"it delivers what the line gives, below its target: "+q);
    }
}
