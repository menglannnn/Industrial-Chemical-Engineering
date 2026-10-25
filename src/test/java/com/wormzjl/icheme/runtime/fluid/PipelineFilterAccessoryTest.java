package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.*;
import static org.junit.jupiter.api.Assertions.*;

class PipelineFilterAccessoryTest {
    private static final String DIM="minecraft:overworld";
    private static final Runnable NOOP=()->{};
    private final com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics model=com.wormzjl.icheme.fluid.support.FluidTestSupport.networkModel();

    @Test void everyAxisUsesNativeFilterAndRemovalComposesWithPendingDiameterEdits(){
        for(var axis:Direction.values()){
            var world=line(axis);var before=stock(world);var original=world.registry.registrations().get(3L);
            assertFalse(world.registry.installPipeFilter(3,axis.y==0?Direction.UP:Direction.EAST));
            assertTrue(world.registry.installPipeFilter(3,axis));assertFalse(world.registry.installPipeFilter(3,axis));
            assertFalse(world.registry.installPipeCutoff(3,axis));assertFalse(world.registry.installPipePressureValve(3,axis));
            var installed=world.registry.registrations().get(3L);assertEquals(Kind.PIPE,installed.device().kind());assertEquals(Kind.FILTER,installed.device().hydraulicKind());
            assertEquals(original.device().id(),installed.device().id());assertEquals(original.device().geometry(),installed.device().geometry());assertEquals(original.spec(),installed.spec());
            world.registry.applyPending();assertEquals(before,stock(world));assertEquals(1,graph(world).pipes().stream().filter(p->p.filter()!=null).count());
            assertNotNull(world.registry.cake(3));var d=installed.device();var g=d.geometry();
            world.registry.submit(List.of(new WorldTopologyLedger.Edit(3,new WorldTopologyLedger.Registration(d.withGeometry(new PipeResistance.Geometry(g.length(),.25,g.roughness(),g.minorLoss())),installed.spec(),installed.revision()+1))),world.ledger.nextIdentity(),null);
            assertTrue(world.registry.removePipeAccessory(3));int events=world.ledger.snapshot().events().size();assertTrue(world.registry.removePipeAccessory(3));assertEquals(events,world.ledger.snapshot().events().size());
            world.registry.applyPending();assertEquals(before,stock(world));assertEquals(.25,world.registry.registrations().get(3L).device().geometry().diameter());assertFalse(world.registry.registrations().get(3L).device().inlineFilter());assertTrue(graph(world).pipes().stream().noneMatch(p->p.filter()!=null));assertTrue(world.commands.isEmpty());
        }
    }
    @Test void activeAndPendingFilterFlagsRoundTripAndMalformedFlagsAreRejected(){
        var world=line(Direction.EAST);world.registry.installPipeFilter(3,Direction.EAST);
        var pending=FluidCheckpointCodec.decodeTopology(FluidCheckpointCodec.encodeTopology(world.ledger.snapshot()),0,k->model);assertArrayEquals(FluidCheckpointCodec.encodeTopology(world.ledger.snapshot()),FluidCheckpointCodec.encodeTopology(pending));
        world.registry.applyPending();var active=world.ledger.snapshot();assertArrayEquals(FluidCheckpointCodec.encodeTopology(active),FluidCheckpointCodec.encodeTopology(FluidCheckpointCodec.decodeTopology(FluidCheckpointCodec.encodeTopology(active),0,k->model)));
        var r=active.active().get(3L);var plain=new LinkedHashMap<>(active.active());plain.put(3L,new WorldTopologyLedger.Registration(r.device().withInlineFilter(Direction.EAST,false),r.spec(),r.revision()));
        var clean=new WorldTopologyLedger.Snapshot(active.onlineTick(),active.nextIdentity(),plain,active.events(),active.constructed(),active.destroyed(),active.basis(),active.recoveries());
        var encoded=FluidCheckpointCodec.encodeTopology(active);var other=FluidCheckpointCodec.encodeTopology(clean);int changed=-1;for(int i=0;i<encoded.length;i++)if(encoded[i]!=other[i]){assertEquals(-1,changed);changed=i;}assertTrue(changed>=0);encoded[changed]=2;assertThrows(IllegalArgumentException.class,()->FluidCheckpointCodec.decodeTopology(encoded,0,k->model));
        var d=r.device();assertThrows(IllegalArgumentException.class,()->new Device(d.id(),d.position(),Kind.VOID,d.facing(),d.geometry(),d.control(),List.of(),true,Cutoff.NONE,true));assertThrows(IllegalArgumentException.class,()->d.withCutoff(Direction.EAST,Cutoff.OPEN));
    }
    @Test void removingLoadedCakeQueuesOneConservedWorldRecovery(){
        var world=line(Direction.EAST);world.registry.installPipeFilter(3,Direction.EAST);world.registry.applyPending();
        var solids=new com.wormzjl.icheme.science.fluid.state.SolidInventory(List.of(new com.wormzjl.icheme.science.fluid.state.SolidInventory.Population(EventPathWorld.CATALOG.solids().require("icheme:demo_particle"),com.wormzjl.icheme.science.fluid.state.ParticleSize.micrometres("100"),.25)));
        var cake=InlineFilter.empty().add(solids,125);var snapshot=world.coordinator.snapshots().getFirst();
        var pipes=snapshot.graph().pipes().stream().map(p->p.filter()==null?p:p.withFilter(cake)).toList();
        var loaded=new EventPathWorld(0,world.ledger.snapshot(),List.of(new IslandCoordinator.Snapshot(snapshot.id(),snapshot.revision(),new PassiveNetwork(snapshot.graph().reservoirs(),pipes),snapshot.clock(),Optional.empty(),snapshot.status())));
        var before=stock(loaded);assertTrue(loaded.registry.removePipeAccessory(3));assertTrue(loaded.registry.removePipeAccessory(3));loaded.registry.applyPending();
        assertEquals(before,stock(loaded));var recoveries=loaded.ledger.snapshot().recoveries();assertEquals(1,recoveries.size());var recovered=recoveries.values().iterator().next();assertEquals(solids,recovered.solids());assertEquals(125,recovered.energyJoule());assertNull(recovered.player());
        assertTrue(loaded.registry.removePipeAccessory(3));loaded.registry.applyPending();assertEquals(recoveries,loaded.ledger.snapshot().recoveries());
    }
    private EventPathWorld line(Direction axis) {
        var records=new LinkedHashMap<Long,WorldTopologyLedger.Registration>();var stock=new HashMap<Long,PassiveNetwork.Reservoir>();
        for(int i=0;i<5;i++) {
            long id=i+1;var kind=i==0||i==4?Kind.RESERVOIR:Kind.PIPE;var p=new Position(DIM,axis.x*i,64+axis.y*i,axis.z*i);
            var d=new Device(id,p,kind,axis,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
            var spec=FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG);records.put(id,new WorldTopologyLedger.Registration(d,spec,0));
            if(kind==Kind.RESERVOIR)stock.put(id,new PassiveNetwork.Reservoir(id,p.y(),model.initialNitrogenCharge(1,298.15,i==0?350000:100000,NOOP)));
        }
        var compiled=PhysicalFluidTopology.compile(records.values().stream().map(WorldTopologyLedger.Registration::device).toList(),stock);
        var snapshot=new IslandCoordinator.Snapshot(10,0,compiled.islands().getFirst().graph(),new IslandClock.Snapshot(0,0,100,100),Optional.empty(),"READY");
        var empty=WorldTopologyLedger.Snapshot.empty(EventPathWorld.CATALOG);
        var total=WorldTopologyLedger.MaterialTotal.empty(model.components().size()).plus(stock.values(),model.components(),model.molecularWeights());
        return new EventPathWorld(0,new WorldTopologyLedger.Snapshot(0,11,records,List.of(),total,empty.destroyed(),empty.basis()),List.of(snapshot));
    }
    private static PassiveNetwork graph(EventPathWorld world){return world.coordinator.snapshots().getFirst().graph();}
    private static Map<Long,PassiveNetwork.Inventory> stock(EventPathWorld world) {
        var out=new TreeMap<Long,PassiveNetwork.Inventory>();
        for(var island:world.coordinator.snapshots())for(var n:island.graph().reservoirs())if(n.kind()==PassiveNetwork.NodeKind.RESERVOIR)out.put(n.id(),n.inventory());
        return out;
    }
}
