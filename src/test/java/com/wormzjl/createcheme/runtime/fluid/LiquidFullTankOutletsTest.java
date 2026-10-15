package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.fluid.support.FluidTestSupport.nitrogen;
import static com.wormzjl.createcheme.fluid.support.FluidTestSupport.water;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Defect F2 of the phase-ports in-game check (2026-09-27): scenario 1's tank, fed by a 130 kPa water generator on its west
 * face with 0.01 m pipes open on its top, east and bottom faces to 101325 Pa voids, held its island once it ran
 * liquid-full ("Substep refinement exhausted: Newton iteration limit at residual 1.147e-9", 996 kg, the top outlet
 * overflowing water). Built from blocks as in game (tools/phase-ports-probes/in-game/NOTES.md, scenario 1) and run as the
 * world runs an island: consecutive 5 s jobs on one {@link RetainedSolver}. The tank must fill, run liquid-full and settle
 * to a steady overflow without a hold.
 */
class LiquidFullTankOutletsTest {
    static final String DIMENSION="minecraft:overworld";
    static final int Y=-57;
    static final PipeResistance.Geometry BLOCK=new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    static final PipeResistance.Geometry OUTLET=new PipeResistance.Geometry(1,.01,PipeResistance.DEFAULT_ROUGHNESS_METRES,0);
    final FluidThermodynamics model=FluidTestSupport.networkModel();

    static PhysicalFluidTopology.Device device(long id,int x,int y,Kind kind,PipeResistance.Geometry geometry) {
        return new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position(DIMENSION,x,y,0),kind,PhysicalFluidTopology.Direction.NORTH,geometry,new FlowControl.Passive());
    }
    static int node(PassiveNetwork graph,long id){for(int i=0;i<graph.reservoirs().size();i++)if(graph.reservoirs().get(i).id()==id)return i;throw new AssertionError(id);}

    @Test void aTankFedLiquidFullWithItsOutletsOpenSettlesToASteadyOverflow() {
        long tankId=3;
        var devices=List.of(device(1,-2,Y,Kind.GENERATOR,BLOCK),device(2,-1,Y,Kind.PIPE,BLOCK),device(tankId,0,Y,Kind.RESERVOIR,BLOCK),
                device(4,0,Y+1,Kind.PIPE,OUTLET),device(5,0,Y+2,Kind.VOID,BLOCK),
                device(6,1,Y,Kind.PIPE,OUTLET),device(7,2,Y,Kind.VOID,BLOCK),
                device(8,0,Y-1,Kind.PIPE,OUTLET),device(9,0,Y-2,Kind.VOID,BLOCK));
        var specs=new HashMap<Long,FluidDeviceSpec>();
        specs.put(1L,new FluidDeviceSpec(1,298.15,130000,water()));specs.put(tankId,new FluidDeviceSpec(1,298.15,101325,nitrogen()));
        for(long v:new long[]{5,7,9})specs.put(v,new FluidDeviceSpec(1,298.15,101325,nitrogen()));
        var boundaries=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        for(var d:devices)if(specs.containsKey(d.id()))boundaries.put(d.id(),FluidTestSupport.chargedBoundary(specs.get(d.id()),d,model));
        var compiled=PhysicalFluidTopology.compile(devices,boundaries,Map.of(),model.initialNitrogenCharge(1,298.15,101325,()->{}));
        var connected=compiled.islands().stream().filter(i->i.physicalIds().size()==devices.size()).toList();
        assertEquals(1,connected.size(),"one connected island: "+compiled.diagnostics());
        var graph=connected.getFirst().graph();
        int tank=node(graph,tankId);
        var retained=new RetainedSolver();PassiveIntervalSolver.Result last=null;int fullAt=-1,slices=0;double previousMass=Double.NaN;int steady=0;
        for(int slice=0;slice<150;slice++) {
            retained.committed(last);PassiveIntervalSolver.Result result;
            try{result=retained.solve(model,graph,5,PassiveIntervalSolver.Settings.defaults(),()->{});}
            catch(RuntimeException held) {
                var t=graph.reservoirs().get(tank).state();
                fail("HELD at slice "+slice+" (t="+5*slice+" s), tank P="+t.pressure()+" mass="+t.mass()+" vapour share="+t.vaporVolume()/t.volume()+": "+held.getMessage());
                return;
            }
            graph=result.graph();last=result;
            var t=graph.reservoirs().get(tank).state();double share=t.vaporVolume()/t.volume();
            System.out.println("F2_SLICE slice="+slice+" tankP="+t.pressure()+" mass="+t.mass()+" vapourShare="+share+" flows="+Arrays.toString(result.averageMassFlows())
                    +" modes="+result.endpointModes()+" accepted="+result.acceptedSubsteps()+" rejected="+result.rejectedSubsteps()+" reasons="+result.rejectionReasons());
            if(share<1e-6&&fullAt<0)fullAt=slice;
            steady=fullAt>=0&&Math.abs(t.mass()-previousMass)<1e-3?steady+1:0;previousMass=t.mass();slices=slice+1;
            // Five minutes of overflow past the slice the tank ran full; in game the island held 14 slices after it. The
            // nitrogen left in the full tank does not dissolve in free water: it stays a vapour phase that the top port
            // halves at every step (decision D11: its capacity is an equal share of the tank's two phase-port ends), about
            // 0.9 decades per slice, so this run carries it from 1e-6 to about 1e-60 of the tank, through the 1e-31 at which
            // it used to hold (tools/phase-ports-probes/holds/). What happens near the bottom of the double range, about
            // 1e-300, is the review's open item, not this test.
            if(fullAt>=0&&slice>=fullAt+60)break;
        }
        System.out.println("F2_END slices="+slices+" fullAt="+fullAt+" steady="+steady);
        assertTrue(fullAt>=0,"the tank runs liquid-full");
        assertEquals(fullAt+61,slices,"sixty slices of overflow after the tank ran full");
        assertTrue(steady>=10,"a steady overflow: ten slices without a change of the tank's mass");
        // At rest the three outlets carry the feed and the top outlet overflows water.
        var outlets=TankOutlets.of(model,graph,tank,last,compiled.tankFaces().get(tankId));
        var top=outlets.stream().filter(o->o.port()==PassiveNetwork.PhasePort.VAPOR).findFirst().orElseThrow();
        assertTrue(top.massFlow()>0,"the top outlet overflows");
        assertEquals(1,top.drawn(),"the top outlet draws water (overflow)");
        double in=0,out=0;
        for(var o:outlets){if(o.massFlow()>0)out+=o.massFlow();else in-=o.massFlow();}
        assertEquals(in,out,1e-3*in,"feed = overflow at rest");
    }
}
