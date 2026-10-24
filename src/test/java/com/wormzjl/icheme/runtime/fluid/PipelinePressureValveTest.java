package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.*;
import static org.junit.jupiter.api.Assertions.*;

/** Accessory adapter coverage; the pressure-valve constitutive model is unchanged. */
class PipelinePressureValveTest {
    private static final String DIM="minecraft:overworld";
    private static final Runnable NOOP=()->{};
    private final com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics model=com.wormzjl.icheme.fluid.support.FluidTestSupport.networkModel();

    @Test void allSixDirectionsUseExistingPressureControlAndPreservePipeAndReservoirs() {
        for(var axis:Direction.values()) {
            var world=line(axis);var before=stock(world);var original=world.registry.registrations().get(3L);
            assertFalse(world.registry.installPipePressureValve(3,axis.y!=0?Direction.EAST:Direction.UP));
            assertTrue(world.registry.installPipePressureValve(3,axis));assertFalse(world.registry.installPipePressureValve(3,axis));
            var installed=world.registry.registrations().get(3L);var d=installed.device();
            assertEquals(original.device().id(),d.id());assertEquals(original.device().position(),d.position());
            assertEquals(original.device().geometry(),d.geometry());assertEquals(original.spec(),installed.spec());
            assertEquals(Kind.PIPE,d.kind());assertEquals(Kind.VALVE,d.hydraulicKind());
            assertFalse(d.mechanicallyConnects(axis.y!=0?Direction.EAST:Direction.UP));
            world.registry.applyPending();assertEquals(before,stock(world));
            assertTrue(graph(world).pipes().stream().anyMatch(p->p.control().equals(new FlowControl.PressureValve(200000))));
            assertFalse(world.registry.installPipeCutoff(3,axis));assertTrue(world.commands.isEmpty());
        }
    }

    @Test void directBoundaryConnectionsAreOrientedAndPressureSetpointClosesFlow() {
        for(var axis:List.of(Direction.UP,Direction.SOUTH,Direction.WEST))for(double upstream:new double[]{150000,350000}) {
            var devices=new ArrayList<Device>();var stock=new HashMap<Long,PassiveNetwork.Reservoir>();
            for(int i=0;i<3;i++) {
                long id=i+1;var position=new Position(DIM,i*axis.x,64+i*axis.y,i*axis.z);
                devices.add(new Device(id,position,i==1?Kind.PIPE:Kind.RESERVOIR,axis,new PipeResistance.Geometry(1,.05,.000045,0),i==1?new FlowControl.PressureValve(200000):new FlowControl.Passive()));
                if(i!=1)stock.put(id,new PassiveNetwork.Reservoir(id,position.y(),model.initialNitrogenCharge(1,298.15,i==0?upstream:100000,NOOP)));
            }
            var compiled=PhysicalFluidTopology.compile(devices,stock);assertEquals(1,compiled.islands().size());
            var graph=compiled.islands().getFirst().graph();assertTrue(graph.pipes().stream().anyMatch(p->p.control() instanceof FlowControl.PressureValve));
            var result=new PassiveStepSolver(model).solve(graph,.01,NOOP);
            for(double q:result.massFlows())if(upstream<200000)assertEquals(0,q,1e-8);else assertTrue(q>0,"forward flow follows the installed arrow");
        }
    }

    @Test void pendingInstallResizeAndPressureEditsRoundTripWithoutAnotherFormatField() {
        var world=line(Direction.EAST);var before=stock(world);assertTrue(world.registry.installPipePressureValve(3,Direction.EAST));
        var record=world.registry.registrations().get(3L);var d=record.device();
        var edited=new Device(d.id(),d.position(),d.kind(),d.facing(),new PipeResistance.Geometry(1,.25,d.geometry().roughness(),0),new FlowControl.PressureValve(275000));
        world.registry.submit(List.of(new WorldTopologyLedger.Edit(3,new WorldTopologyLedger.Registration(edited,record.spec(),record.revision()+1))),world.ledger.nextIdentity(),null);
        var snapshots=world.coordinator.snapshots();var checkpoint=new FluidCheckpointCodec.Checkpoint(snapshots.stream().map(s->new FluidCheckpointCodec.IslandEntry(DIM,"icheme:tjl20_methane_nitrogen",s)).toList());
        var store=FluidCheckpointStore.memory();store.commit(store.prepare(checkpoint,Optional.of(world.ledger.snapshot()),world.ledger.onlineTick(),key->model));
        var image=store.image();var loaded=FluidCheckpointStore.memory(image).load(image.core(),key->model);
        assertArrayEquals(FluidCheckpointCodec.encodeTopology(world.ledger.snapshot()),FluidCheckpointCodec.encodeTopology(loaded.world().orElseThrow()));
        var restored=new EventPathWorld(0,loaded.world().orElseThrow(),loaded.checkpoint().islands().stream().map(FluidCheckpointCodec.IslandEntry::snapshot).toList());
        restored.registry.applyPending();assertEquals(before,stock(restored));assertEquals(edited,restored.ledger.active().get(3L).device());
        var active=FluidCheckpointCodec.decodeTopology(FluidCheckpointCodec.encodeTopology(restored.ledger.snapshot()),restored.ledger.onlineTick(),key->model);
        assertEquals(edited,active.active().get(3L).device());
        assertThrows(IllegalArgumentException.class,()->edited.withCutoff(Direction.EAST,Cutoff.OPEN));
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
