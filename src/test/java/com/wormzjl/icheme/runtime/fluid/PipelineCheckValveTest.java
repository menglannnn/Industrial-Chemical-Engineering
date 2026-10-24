package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.fluid.support.FluidTestSupport;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fresh fixtures; exercises the existing direction constraints and real queued topology path. */
class PipelineCheckValveTest {
    private final com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics model=FluidTestSupport.networkModel();
    private static final String DIM="minecraft:overworld";
    private static final Runnable NOOP=()->{};

    private EventPathWorld line(Direction axis,boolean reversePressure) {
        var records=new LinkedHashMap<Long,WorldTopologyLedger.Registration>();var stock=new HashMap<Long,PassiveNetwork.Reservoir>();
        for(int i=0;i<5;i++) {
            long id=i+1;var kind=i==0||i==4?Kind.RESERVOIR:Kind.PIPE;
            var p=new Position(DIM,axis.x*i,64+axis.y*i,axis.z*i);
            var d=new Device(id,p,kind,axis,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
            var spec=FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG);records.put(id,new WorldTopologyLedger.Registration(d,spec,0));
            if(kind==Kind.RESERVOIR)stock.put(id,new PassiveNetwork.Reservoir(id,p.y(),model.initialNitrogenCharge(1,298.15,(i==0)^reversePressure?200000:100000,NOOP)));
        }
        var graph=PhysicalFluidTopology.compile(records.values().stream().map(WorldTopologyLedger.Registration::device).toList(),stock).islands().getFirst().graph();
        var island=new IslandCoordinator.Snapshot(10,0,graph,new IslandClock.Snapshot(0,0,100,100),Optional.empty(),"READY");
        var empty=WorldTopologyLedger.Snapshot.empty(EventPathWorld.CATALOG);
        var total=WorldTopologyLedger.MaterialTotal.empty(model.components().size()).plus(stock.values(),model.components(),model.molecularWeights());
        return new EventPathWorld(0,new WorldTopologyLedger.Snapshot(0,11,records,List.of(),total,empty.destroyed(),empty.basis()),List.of(island));
    }
    private static Device check(EventPathWorld world){return world.registry.registrations().get(3L).device();}
    private static PassiveNetwork graph(EventPathWorld world){return world.coordinator.snapshots().stream().map(IslandCoordinator.Snapshot::graph).filter(g->g.reservoirs().stream().anyMatch(n->n.id()==1)).findFirst().orElseThrow();}
    private static Map<Long,PassiveNetwork.Inventory> stock(EventPathWorld world) {
        var stocks=new TreeMap<Long,PassiveNetwork.Inventory>();
        for(var s:world.coordinator.snapshots())for(var n:s.graph().reservoirs())if(n.kind()==PassiveNetwork.NodeKind.RESERVOIR)stocks.put(n.id(),n.inventory());
        return stocks;
    }

    @Test void allSixDirectionsPassForwardAndBlockReverseWithoutChangingPipeOrOwnedContents() {
        for(var axis:Direction.values())for(boolean reverse:List.of(false,true)) {
            var world=line(axis,reverse);var before=stock(world);var original=check(world);
            var unrestricted=new PassiveStepSolver(model).solve(graph(world),.1,NOOP);
            assertTrue(reverse?unrestricted.massFlows()[0]<0:unrestricted.massFlows()[0]>0,"baseline must actually permit both directions");
            assertTrue(world.registry.installPipeValve(3,axis,Cutoff.CHECK));
            assertFalse(world.registry.installPipeValve(3,axis,Cutoff.CHECK));
            world.registry.applyPending();var network=graph(world);
            assertEquals(before,stock(world));assertEquals(original.id(),check(world).id());assertEquals(original.geometry(),check(world).geometry());
            assertEquals(2,check(world).contacts().size());
            for(var d:Direction.values())assertEquals(d==axis||d==opposite(axis),check(world).mechanicallyConnects(d));
            assertTrue(network.pipes().stream().allMatch(p->p.forbiddenDirections()!=0));
            var result=new PassiveStepSolver(model).solve(network,.1,NOOP);
            for(double flow:result.massFlows())if(reverse)assertEquals(0,flow,1e-10);else assertTrue(flow>0);
            assertTrue(world.commands.isEmpty(),"install does not force a solve");
        }
    }

    @Test void reversingComposesWithPendingDiameterEditsAndRoundTripsTheCompleteCheckpoint() {
        var world=line(Direction.EAST,false);world.registry.installPipeValve(3,Direction.EAST,Cutoff.CHECK);world.registry.applyPending();
        var before=stock(world);world.registry.togglePipeCutoff(3);
        var record=world.registry.registrations().get(3L);var g=record.device().geometry();
        world.registry.submit(List.of(new WorldTopologyLedger.Edit(3,new WorldTopologyLedger.Registration(record.device().withGeometry(new PipeResistance.Geometry(g.length(),.25,g.roughness(),g.minorLoss())),record.spec(),record.revision()+1))),world.ledger.nextIdentity(),null);
        world.registry.togglePipeCutoff(3);world.registry.togglePipeCutoff(3);
        assertEquals(Direction.WEST,check(world).facing());assertEquals(.25,check(world).geometry().diameter());
        assertEquals(Direction.EAST,world.ledger.active().get(3L).device().facing(),"committed direction changes only on the schedule");
        var checkpoint=new FluidCheckpointCodec.Checkpoint(world.coordinator.snapshots().stream().map(s->new FluidCheckpointCodec.IslandEntry(DIM,"icheme:tjl20_methane_nitrogen",s)).toList());
        var store=FluidCheckpointStore.memory();store.commit(store.prepare(checkpoint,Optional.of(world.ledger.snapshot()),world.ledger.onlineTick(),key->model));
        var image=store.image();var loaded=FluidCheckpointStore.memory(image).load(image.core(),key->model);var saved=loaded.world().orElseThrow();
        assertArrayEquals(FluidCheckpointCodec.encodeTopology(world.ledger.snapshot()),FluidCheckpointCodec.encodeTopology(saved));
        assertEquals(graph(world).pipes(),loaded.checkpoint().islands().getFirst().snapshot().graph().pipes(),"permanent scientific restrictions persist too");
        var reload=new EventPathWorld(0,saved,loaded.checkpoint().islands().stream().map(FluidCheckpointCodec.IslandEntry::snapshot).toList());
        reload.registry.applyPending();assertEquals(Direction.WEST,check(reload).facing());assertEquals(before,stock(reload));
        for(double flow:new PassiveStepSolver(model).solve(graph(reload),.1,NOOP).massFlows())assertEquals(0,flow,1e-10);
        assertTrue(reload.commands.isEmpty());
    }

    @Test void reopeningSolidClosuresCannotOpenTheCheckValveAndInvalidCodesAreRejected() {
        var world=line(Direction.NORTH,false);world.registry.installPipeValve(3,Direction.NORTH,Cutoff.CHECK);world.registry.applyPending();var graph=graph(world);
        var closed=new PassiveNetwork(graph.reservoirs(),graph.pipes().stream().map(p->p.withBlockedDirections(3)).toList());
        var reopened=NetworkReduction.reopened(closed);
        for(int i=0;i<graph.pipes().size();i++) {
            var p=reopened.pipes().get(i);assertEquals(0,p.blockedDirections());assertEquals(graph.pipes().get(i).forbiddenDirections(),p.forbiddenDirections());
            assertTrue(p.blocked(-1));assertFalse(p.blocked(1));
        }
        assertThrows(IllegalArgumentException.class,()->graph.pipes().getFirst().withForbiddenDirections(4));
        assertThrows(IllegalArgumentException.class,()->Cutoff.fromCode(4));
        var d=new Device(20,new Position(DIM,10,64,0),Kind.VOID,Direction.EAST,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive());
        assertThrows(IllegalArgumentException.class,()->d.withCutoff(Direction.EAST,Cutoff.CHECK));
        var a=topology(check(world));var b=topology(check(world).withCutoff(Direction.NORTH,Cutoff.OPEN));
        int at=onlyDifference(a,b);var bad=a.clone();bad[at]=4;
        assertThrows(IllegalArgumentException.class,()->FluidCheckpointCodec.decodeTopology(bad,0,key->model));
    }

    @Test void checkNodesRetainPressureAndRestrictionsSurviveDrivenEndSwapping() {
        for(var pumpFacing:List.of(Direction.EAST,Direction.WEST)) {
            var records=new ArrayList<Device>();var stocks=new HashMap<Long,PassiveNetwork.Reservoir>();
            for(int x=0;x<7;x++) {
                long id=x+1;var kind=x==0||x==6?Kind.RESERVOIR:x==4?Kind.PUMP:Kind.PIPE;
                var device=new Device(id,new Position(DIM,x,64,0),kind,kind==Kind.PUMP?pumpFacing:Direction.EAST,new PipeResistance.Geometry(1,.05,.000045,0),kind==Kind.PUMP?new FlowControl.Pump(.01,500000):new FlowControl.Passive());
                if(x==2)device=device.withCutoff(Direction.EAST,Cutoff.CHECK);
                records.add(device);if(kind==Kind.RESERVOIR)stocks.put(id,new PassiveNetwork.Reservoir(id,64,model.initialNitrogenCharge(1,298.15,200000,NOOP)));
            }
            var network=PhysicalFluidTopology.compile(records,stocks).islands().getFirst().graph();
            int node=-1;for(int i=0;i<network.reservoirs().size();i++)if(network.reservoirs().get(i).id()==3)node=i;
            assertTrue(node>=0,"check valve keeps a pressure node between independently driven sections");
            if(pumpFacing==Direction.WEST) {
                var driven=network.pipes().stream().filter(p->p.control() instanceof FlowControl.Pump).findFirst().orElseThrow();
                assertEquals(1,driven.forbiddenDirections(),"a discharge facing back through the check is blocked, even after swapping the run");
            }
        }
        for(boolean reverse:List.of(false,true)) {
            var world=line(Direction.EAST,reverse);
            world.registry.installPipeValve(3,Direction.EAST,Cutoff.CHECK);
            world.registry.installPipeValve(4,Direction.WEST,Cutoff.CHECK);world.registry.applyPending();
            assertTrue(graph(world).pipes().stream().anyMatch(p->p.forbiddenDirections()==3),"opposed check valves block both directions in their connecting run");
            for(double flow:new PassiveStepSolver(model).solve(graph(world),.1,NOOP).massFlows())assertEquals(0,flow,1e-10);
        }
    }
    private byte[] topology(Device device) {
        var empty=WorldTopologyLedger.Snapshot.empty(EventPathWorld.CATALOG);
        return FluidCheckpointCodec.encodeTopology(new WorldTopologyLedger.Snapshot(0,4,Map.of(device.id(),new WorldTopologyLedger.Registration(device,FluidDeviceSpec.nitrogen(EventPathWorld.CATALOG),0)),List.of(),empty.constructed(),empty.destroyed(),empty.basis()));
    }
    private static int onlyDifference(byte[] a,byte[] b) {
        assertEquals(a.length,b.length);int at=-1;for(int i=0;i<a.length;i++)if(a[i]!=b[i]){assertEquals(-1,at);at=i;}assertTrue(at>=0);return at;
    }
}
