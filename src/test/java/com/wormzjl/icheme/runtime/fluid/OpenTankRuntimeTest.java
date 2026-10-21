package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.ConservativeTransport.BoundaryTransfer.Kind;
import com.wormzjl.icheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.icheme.science.fluid.solver.PhaseLayout;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/**
 * An open (floating-roof) tank in the runtime (batch 2026-10-21-floating-roof-tanks, WP2): registered open with its two
 * bottom LIQUID nozzles and its OVERFLOW nozzle at the fill height, fed water carrying a little nitrogen, its overflow
 * draining to a void. It fills, overflows and vents at the ambient pressure, certifies STEADY with its vent and overflow
 * as rates (the certificate does not read them as one-time flushes), names its vent on the status line and in its view,
 * and its registration and island round-trip the open flag, the OVERFLOW port and the boundary kinds. Non-volatile only
 * (owner decision of 2026-10-21).
 */
class OpenTankRuntimeTest {
    private static PhysicalFluidTopology.Position at(int x,int y,int z){return new PhysicalFluidTopology.Position(EventPathWorld.DIM,x,y,z);}
    /** A 3-wide tank in a box from (0,0,0): node (1,0,1), bottom nozzles (0,0,0) and (2,0,0) north, overflow (1,2,0) north. */
    private static final List<PhysicalFluidTopology.Port> PORTS=List.of(
            new PhysicalFluidTopology.Port(at(0,0,0),NORTH,PassiveNetwork.PhasePort.LIQUID),
            new PhysicalFluidTopology.Port(at(2,0,0),NORTH,PassiveNetwork.PhasePort.LIQUID),
            new PhysicalFluidTopology.Port(at(1,2,0),NORTH,PassiveNetwork.PhasePort.OVERFLOW));
    /** Water carrying 0.1 % (mole) nitrogen at 298.15 K and 1.5 bar: a generator's spec. */
    private static FluidDeviceSpec wetFeed() {
        var axis=FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG).composition();double[] n=new double[axis.length];
        int nitrogen=0;for(int c=0;c<axis.length;c++)if(axis[c]==1)nitrogen=c;
        n[n.length-1]=.999;n[nitrogen]=.001;
        return new FluidDeviceSpec(1,298.15,150000,n);
    }

    @Test void aFedOpenTankOverflowsVentsAndCertifiesSteady() {
        var w=new EventPathWorld();w.runTo(100);
        long tank=w.placeVessel(at(1,0,1),PORTS,2,2,true);
        w.place(0,0,-1,com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PIPE,NORTH,.025);
        w.place(0,0,-2,com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.GENERATOR,NORTH,wetFeed(),.025);
        w.place(1,2,-1,com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PIPE,NORTH,.05);
        w.place(1,2,-2,com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.VOID,NORTH,.05);
        w.runTo(30000);w.assertKeptUp("a fed open tank");
        long island=w.registry.owner(tank);var snapshot=w.coordinator.observe(island);
        var node=snapshot.graph().reservoirs().stream().filter(n->n.id()==tank).findFirst().orElseThrow();
        assertTrue(node.inventory().open(),"the island keeps the open flag");
        assertEquals(PhaseLayout.AMBIENT_PRESSURE,node.state().pressure(),1e-6*PhaseLayout.AMBIENT_PRESSURE,"the surface stands at the ambient pressure");
        double held=node.state().liquidVolume()+node.state().waterVolume();
        assertTrue(held>=2-1e-6&&held<2.5,"the tank stands at its overflow: "+held+" m3 of 2; "+snapshot.status());
        assertTrue(snapshot.status().startsWith("STEADY"),"a steadily venting, overflowing tank certifies: "+snapshot.status());
        assertTrue(snapshot.status().contains("venting"),"the status line names the vent: "+snapshot.status());
        var last=snapshot.lastResult().orElseThrow();
        assertTrue(last.boundaries().stream().anyMatch(b->b.kind()==Kind.VENT&&b.nodeId()==tank),"the last interval vents at the tank");
        var view=TankOutlets.tank(w.model,node,last);
        assertTrue(view.open()&&view.ventKgPerSecond()>0&&view.spillKgPerSecond()==0,"the tank view: "+view);
        // The island's unit round-trips the open inventory and the vent's kind.
        var image=FluidCheckpointCodec.encode(new FluidCheckpointCodec.Checkpoint(List.of(new FluidCheckpointCodec.IslandEntry(EventPathWorld.DIM,"icheme:tjl20_methane_nitrogen",snapshot))),key->w.model);
        var after=FluidCheckpointCodec.decode(image,key->w.model).islands().getFirst().snapshot();
        assertEquals(snapshot.graph().reservoirs().stream().map(PassiveNetwork.Reservoir::inventory).toList(),after.graph().reservoirs().stream().map(PassiveNetwork.Reservoir::inventory).toList(),"inventories with their open flag");
        assertEquals(snapshot.graph().pipes(),after.graph().pipes(),"the OVERFLOW port");
        assertEquals(last.boundaries().stream().map(b->b.kind()).toList(),after.lastResult().orElseThrow().boundaries().stream().map(b->b.kind()).toList(),"the boundary kinds");
        System.out.println("OPEN_TANK_RUNTIME held="+held+" status="+snapshot.status()+" vent="+view.ventKgPerSecond());
    }

    @Test void anOpenTanksRegistrationRoundTripsItsFlagAndOverflowPort() {
        var empty=WorldTopologyLedger.Snapshot.empty(EventPathWorld.CATALOG);
        var device=new PhysicalFluidTopology.Device(1,at(1,0,1),com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.RESERVOIR,NORTH,
                new com.wormzjl.icheme.science.fluid.network.PipeResistance.Geometry(1,.05,.000045,0),new com.wormzjl.icheme.science.fluid.network.FlowControl.Passive(),PORTS);
        var spec=new FluidDeviceSpec(28_000,298.15,101325,EventPathWorld.nitrogen().composition(),SlurryFeed.NONE,23,true);
        var world=new WorldTopologyLedger.Snapshot(5,2,Map.of(1L,new WorldTopologyLedger.Registration(device,spec,0)),List.of(),empty.constructed(),empty.destroyed(),empty.basis(),Map.of());
        var bytes=FluidCheckpointCodec.encodeTopology(world);var decoded=FluidCheckpointCodec.decodeTopology(bytes,5,key->{throw new AssertionError("no recovery, no model");});
        assertEquals(world.active(),decoded.active(),"the open flag, the volume above 10,000 m3 and the OVERFLOW port come back");
        assertTrue(decoded.active().get(1L).spec().open());
        assertArrayEquals(bytes,FluidCheckpointCodec.encodeTopology(decoded));
        var reservoir=spec.initialize(device,w().model,()->{});
        assertTrue(reservoir.inventory().open()&&reservoir.empty(),"an open tank is placed empty and open");
    }
    private static EventPathWorld w(){return new EventPathWorld();}
}
