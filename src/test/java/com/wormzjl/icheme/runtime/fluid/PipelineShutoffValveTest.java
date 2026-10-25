package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.fluid.support.FluidTestSupport;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fresh current-format worlds; real registry/coordinator edits, no Minecraft client or compatibility fixtures. */
class PipelineShutoffValveTest {
    private final com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics model=FluidTestSupport.networkModel();
    private static final String DIM="minecraft:overworld";
    private static final Runnable NOOP=()->{};

    private EventPathWorld line(Direction axis) {
        var records=new LinkedHashMap<Long,WorldTopologyLedger.Registration>();var stock=new HashMap<Long,PassiveNetwork.Reservoir>();
        for(int i=0;i<5;i++) {
            long id=i+1;var kind=i==0||i==4?Kind.RESERVOIR:Kind.PIPE;
            var p=new Position(DIM,axis.x*i,64+axis.y*i,axis.z*i);
            var d=new Device(id,p,kind,axis,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
            var spec=FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG);records.put(id,new WorldTopologyLedger.Registration(d,spec,0));
            if(kind==Kind.RESERVOIR)stock.put(id,new PassiveNetwork.Reservoir(id,p.y(),model.initialNitrogenCharge(spec.volume(),298.15,150000+10000*i,NOOP)));
        }
        var compiled=PhysicalFluidTopology.compile(records.values().stream().map(WorldTopologyLedger.Registration::device).toList(),stock);
        assertEquals(1,compiled.islands().size());
        var graph=compiled.islands().getFirst().graph();
        var island=new IslandCoordinator.Snapshot(10,0,graph,new IslandClock.Snapshot(0,0,100,100),Optional.empty(),"READY");
        var empty=WorldTopologyLedger.Snapshot.empty(EventPathWorld.CATALOG);
        var total=WorldTopologyLedger.MaterialTotal.empty(model.components().size()).plus(stock.values(),model.components(),model.molecularWeights());
        var saved=new WorldTopologyLedger.Snapshot(0,11,records,List.of(),total,empty.destroyed(),empty.basis());
        return new EventPathWorld(0,saved,List.of(island));
    }
    private static Map<Long,PassiveNetwork.Inventory> stock(EventPathWorld world) {
        var result=new TreeMap<Long,PassiveNetwork.Inventory>();
        for(var island:world.coordinator.snapshots())for(var node:island.graph().reservoirs())if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR)result.put(node.id(),node.inventory());
        return result;
    }
    private static Device valve(EventPathWorld world){return world.registry.registrations().get(3L).device();}

    @Test void everyAxisCutsBothDirectionsAndReopensWithoutChangingOwnedContents() {
        for(var axis:List.of(Direction.UP,Direction.SOUTH,Direction.EAST)) {
            var world=line(axis);var before=stock(world);var original=valve(world);
            assertFalse(world.registry.installPipeCutoff(3,axis==Direction.UP?Direction.EAST:Direction.UP),"physical neighbours must agree with the installed axis");
            assertTrue(world.registry.installPipeCutoff(3,axis));
            assertFalse(world.registry.installPipeCutoff(3,axis),"duplicate installation queues nothing");
            world.registry.applyPending();
            assertEquals(world.registry.owner(1),world.registry.owner(5));
            assertEquals(2,valve(world).contacts().size());
            for(var d:Direction.values())assertEquals(d==axis||d==opposite(axis),valve(world).mechanicallyConnects(d));
            world.registry.togglePipeCutoff(3);
            assertEquals(Cutoff.OPEN,world.ledger.active().get(3L).device().cutoff(),"interaction never applies the topology early");
            world.registry.applyPending();
            assertNotEquals(world.registry.owner(1),world.registry.owner(5),"both charged tanks advance as independent islands");
            assertTrue(valve(world).contacts().isEmpty(),"closed in both directions");
            assertEquals(before,stock(world));
            world.registry.togglePipeCutoff(3);world.registry.applyPending();
            assertEquals(world.registry.owner(1),world.registry.owner(5));
            assertEquals(before,stock(world));assertEquals(original.id(),valve(world).id());assertEquals(original.geometry(),valve(world).geometry());
            assertTrue(world.commands.isEmpty(),"topology edits do not force a solve");
        }
    }

    @Test void rapidTogglesAndDiameterEditsSurviveAFullCheckpointWithAPendingToggle() {
        var world=line(Direction.EAST);world.registry.installPipeCutoff(3,Direction.EAST);world.registry.applyPending();
        var before=stock(world);world.registry.togglePipeCutoff(3);
        var record=world.registry.registrations().get(3L);var g=record.device().geometry();
        var resized=record.device().withGeometry(new PipeResistance.Geometry(g.length(),.25,g.roughness(),g.minorLoss()));
        world.registry.submit(List.of(new WorldTopologyLedger.Edit(3,new WorldTopologyLedger.Registration(resized,record.spec(),record.revision()+1))),world.ledger.nextIdentity(),null);
        world.registry.togglePipeCutoff(3);world.registry.togglePipeCutoff(3);
        assertEquals(Cutoff.CLOSED,valve(world).cutoff());assertEquals(.25,valve(world).geometry().diameter());
        var snapshots=world.coordinator.snapshots();
        var checkpoint=new FluidCheckpointCodec.Checkpoint(snapshots.stream().map(s->new FluidCheckpointCodec.IslandEntry(DIM,"icheme:tjl20_methane_nitrogen",s)).toList());
        var store=FluidCheckpointStore.memory();store.commit(store.prepare(checkpoint,Optional.of(world.ledger.snapshot()),world.ledger.onlineTick(),key->model));
        var image=store.image();var loaded=FluidCheckpointStore.memory(image).load(image.core(),key->model);
        var saved=loaded.world().orElseThrow();assertEquals(world.ledger.onlineTick(),saved.onlineTick());
        assertArrayEquals(FluidCheckpointCodec.encodeTopology(world.ledger.snapshot()),FluidCheckpointCodec.encodeTopology(saved),"material, active state and pending edits round-trip byte-for-byte");
        var restored=loaded.checkpoint().islands().stream().map(FluidCheckpointCodec.IslandEntry::snapshot).toList();
        assertEquals(snapshots.stream().map(IslandCoordinator.Snapshot::clock).toList(),restored.stream().map(IslandCoordinator.Snapshot::clock).toList());
        var reload=new EventPathWorld(0,saved,restored);
        assertEquals(Cutoff.OPEN,reload.ledger.active().get(3L).device().cutoff());assertEquals(Cutoff.CLOSED,valve(reload).cutoff());
        reload.registry.applyPending();assertEquals(Cutoff.CLOSED,reload.ledger.active().get(3L).device().cutoff());
        assertNotEquals(reload.registry.owner(1),reload.registry.owner(5));assertEquals(before,stock(reload));
        assertEquals(.25,valve(reload).geometry().diameter());assertTrue(reload.commands.isEmpty());
    }

    @Test void aClosedValveDoesNotRemoveARealBypass() {
        var world=line(Direction.EAST);world.registry.installPipeCutoff(3,Direction.EAST);world.registry.togglePipeCutoff(3);
        long next=world.ledger.nextIdentity();var edits=new ArrayList<WorldTopologyLedger.Edit>();
        for(int x=1;x<=3;x++) {
            var d=new Device(next,new Position(DIM,x,64,1),Kind.PIPE,Direction.EAST,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
            edits.add(new WorldTopologyLedger.Edit(next,new WorldTopologyLedger.Registration(d,FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG),0)));next++;
        }
        world.registry.submit(edits,next,null);world.registry.applyPending();
        assertEquals(Cutoff.CLOSED,valve(world).cutoff());assertEquals(world.registry.owner(1),world.registry.owner(5));
        assertFalse(valve(world).mechanicallyConnects(Direction.SOUTH));
    }

    @Test void malformedCodesAndCutoffsOnOtherDeviceKindsAreRejectedByTheReader() {
        var device=new Device(1,new Position(DIM,0,64,0),Kind.PIPE,Direction.EAST,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
        var open=image(device.withCutoff(Direction.EAST,Cutoff.OPEN));var closed=image(device.withCutoff(Direction.EAST,Cutoff.CLOSED));
        int code=difference(open,closed);var bad=open.clone();bad[code]=(byte)255;
        assertTrue(assertThrows(IllegalArgumentException.class,()->FluidCheckpointCodec.decodeTopology(bad,0,key->model)).getMessage().contains("Unknown pipe cutoff"));
        var other=new Device(1,device.position(),Kind.VOID,device.facing(),device.geometry(),device.control());
        var pipe=image(device);var sink=image(other);int kind=difference(pipe,sink);var illegalKind=open.clone();illegalKind[kind]=sink[kind];
        assertTrue(assertThrows(IllegalArgumentException.class,()->FluidCheckpointCodec.decodeTopology(illegalKind,0,key->model)).getMessage().contains("Only a pipe"));
        assertThrows(IllegalArgumentException.class,()->other.withCutoff(Direction.EAST,Cutoff.OPEN));
    }
    @Test void wrenchRemovalRestoresOrdinaryPipeAndPreservesPendingDiameterAndContents(){
        for(var axis:List.of(Direction.EAST,Direction.UP,Direction.SOUTH))for(int type=0;type<4;type++){
            var world=line(axis);var before=stock(world);
            if(type==0){world.registry.installPipeCutoff(3,axis);world.registry.togglePipeCutoff(3);}else if(type==1)world.registry.installPipeValve(3,axis,Cutoff.CHECK);else if(type==2)world.registry.installPipePressureValve(3,axis);else world.registry.installPipeFilter(3,axis);
            world.registry.applyPending();var record=world.registry.registrations().get(3L);var g=record.device().geometry();
            world.registry.submit(List.of(new WorldTopologyLedger.Edit(3,new WorldTopologyLedger.Registration(record.device().withGeometry(new PipeResistance.Geometry(g.length(),.25,g.roughness(),g.minorLoss())),record.spec(),record.revision()+1))),world.ledger.nextIdentity(),null);
            assertTrue(world.registry.removePipeAccessory(3));int pending=world.ledger.snapshot().events().size();assertTrue(world.registry.removePipeAccessory(3));assertEquals(pending,world.ledger.snapshot().events().size());
            world.registry.applyPending();assertEquals(3,valve(world).id());assertEquals(.25,valve(world).geometry().diameter());assertEquals(Cutoff.NONE,valve(world).cutoff());assertInstanceOf(FlowControl.Passive.class,valve(world).control());assertEquals(world.registry.owner(1),world.registry.owner(5));assertEquals(before,stock(world));assertTrue(world.commands.isEmpty());
        }
    }
    private byte[] image(Device device) {
        var empty=WorldTopologyLedger.Snapshot.empty(EventPathWorld.CATALOG);
        var saved=new WorldTopologyLedger.Snapshot(0,2,Map.of(device.id(),new WorldTopologyLedger.Registration(device,FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG),0)),List.of(),empty.constructed(),empty.destroyed(),empty.basis());
        return FluidCheckpointCodec.encodeTopology(saved);
    }
    private static int difference(byte[] a,byte[] b) {
        assertEquals(a.length,b.length);int changed=-1;
        for(int i=0;i<a.length;i++)if(a[i]!=b[i]){assertEquals(-1,changed,"only the requested discriminator differs");changed=i;}
        assertTrue(changed>=0);return changed;
    }
}
