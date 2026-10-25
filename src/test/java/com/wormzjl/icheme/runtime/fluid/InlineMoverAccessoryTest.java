package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.equipment.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.*;
import static org.junit.jupiter.api.Assertions.*;

/** Current-format horizontal pipe fittings compile to the existing mover model. */
class InlineMoverAccessoryTest {
    private static final String DIM="minecraft:overworld";
    private static final Runnable NOOP=()->{};
    private final com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics model=com.wormzjl.icheme.fluid.support.FluidTestSupport.networkModel();

    @Test void eachHorizontalHeadingUsesTheNativeActuatorWithoutChangingHostOrOwnedContents(){
        for(var axis:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST))for(var kind:List.of(Kind.PUMP,Kind.COMPRESSOR)){
            var world=line(axis);var before=stock(world);var old=world.registry.registrations().get(3L);
            assertFalse(world.registry.installPipeMover(3,Direction.UP,kind));assertFalse(world.registry.installPipeMover(3,axis.x==0?Direction.EAST:Direction.NORTH,kind));
            assertTrue(world.registry.installPipeMover(3,axis,kind));assertFalse(world.registry.installPipeMover(3,axis,kind));
            assertFalse(world.registry.installPipeFilter(3,axis));assertFalse(world.registry.installPipePressureValve(3,axis));assertFalse(world.registry.installPipeCutoff(3,axis));
            var installed=world.registry.registrations().get(3L);var d=installed.device();
            assertEquals(Kind.PIPE,d.kind());assertEquals(kind,d.hydraulicKind());assertEquals(PhysicalFluidTopology.defaultMover(kind),d.control());assertEquals(old.spec(),installed.spec());assertEquals(old.device().geometry(),d.geometry());assertEquals(old.device().id(),d.id());
            world.registry.applyPending();assertEquals(before,stock(world));
            var boundaries=new HashMap<Long,PassiveNetwork.Reservoir>();for(var n:graph(world).reservoirs())if(n.id()>0&&n.kind()==PassiveNetwork.NodeKind.RESERVOIR)boundaries.put(n.id(),n);
            var nativeDevices=world.ledger.active().values().stream().map(r->{var x=r.device();return x.id()==3?new Device(x.id(),x.position(),kind,x.facing(),x.geometry(),x.control()):x;}).toList();
            var nativeGraph=PhysicalFluidTopology.compile(nativeDevices,boundaries).islands().getFirst().graph();
            assertEquals(nativeGraph.pipes(),graph(world).pipes());assertEquals(1,graph(world).pipes().stream().filter(p->p.control() instanceof FlowControl.Mover).count());
            assertEquals(PipePresentation.actuatorMappings(new Device(d.id(),d.position(),kind,d.facing(),d.geometry(),d.control()),graph(world)),PipePresentation.actuatorMappings(d,graph(world)));assertTrue(world.commands.isEmpty());
        }
    }
    @Test void pendingControlResizeRemovalAndReinstallationComposeAndRoundTrip(){
        for(var kind:List.of(Kind.PUMP,Kind.COMPRESSOR)){
            var world=line(Direction.EAST);var before=stock(world);world.registry.installPipeMover(3,Direction.EAST,kind);var r=world.registry.registrations().get(3L);var d=r.device();
            FlowControl control=kind==Kind.PUMP?new FlowControl.Pump(.02,600000):new FlowControl.Compressor(.08,2);
            var edited=new Device(d.id(),d.position(),d.kind(),d.facing(),new PipeResistance.Geometry(1,.25,.000045,0),control,d.ports(),true,d.cutoff(),false);
            world.registry.submit(List.of(new WorldTopologyLedger.Edit(3,new WorldTopologyLedger.Registration(edited,r.spec(),r.revision()+1))),world.ledger.nextIdentity(),null);
            var image=FluidCheckpointCodec.encodeTopology(world.ledger.snapshot());var loaded=FluidCheckpointCodec.decodeTopology(image,0,k->model);assertArrayEquals(image,FluidCheckpointCodec.encodeTopology(loaded));
            var restored=new EventPathWorld(0,loaded,world.coordinator.snapshots());restored.registry.applyPending();assertEquals(edited,restored.ledger.active().get(3L).device());
            var active=FluidCheckpointCodec.encodeTopology(restored.ledger.snapshot());assertArrayEquals(active,FluidCheckpointCodec.encodeTopology(FluidCheckpointCodec.decodeTopology(active,0,k->model)));
            assertTrue(restored.registry.removePipeAccessory(3));int events=restored.ledger.snapshot().events().size();assertTrue(restored.registry.removePipeAccessory(3));assertEquals(events,restored.ledger.snapshot().events().size());
            assertTrue(restored.registry.installPipeFilter(3,Direction.EAST));assertTrue(restored.registry.removePipeAccessory(3));restored.registry.applyPending();var ordinary=restored.registry.registrations().get(3L).device();assertEquals(3,ordinary.id());assertEquals(.25,ordinary.geometry().diameter());assertInstanceOf(FlowControl.Passive.class,ordinary.control());assertEquals(before,stock(restored));assertTrue(restored.commands.isEmpty());
        }
    }
    @Test void verticalHostsAndConflictingOrDisabledMoverControlsAreRejected(){
        var world=line(Direction.UP);for(var kind:List.of(Kind.PUMP,Kind.COMPRESSOR)){
            assertFalse(world.registry.installPipeMover(3,Direction.UP,kind));assertFalse(world.registry.installPipeMover(3,Direction.EAST,kind));
            var d=world.registry.registrations().get(3L).device();assertThrows(IllegalArgumentException.class,()->new Device(d.id(),d.position(),Kind.PIPE,Direction.UP,d.geometry(),PhysicalFluidTopology.defaultMover(kind)));
            assertThrows(IllegalArgumentException.class,()->new Device(d.id(),d.position(),Kind.PIPE,Direction.EAST,d.geometry(),PhysicalFluidTopology.defaultMover(kind),List.of(),true,Cutoff.OPEN));
            assertThrows(IllegalArgumentException.class,()->new Device(d.id(),d.position(),Kind.PIPE,Direction.EAST,d.geometry(),PhysicalFluidTopology.defaultMover(kind),List.of(),true,Cutoff.NONE,true));
            FlowControl disabled=kind==Kind.PUMP?new FlowControl.Pump(.01,500000,false):new FlowControl.Compressor(.05,3,false);assertThrows(IllegalArgumentException.class,()->new Device(d.id(),d.position(),Kind.PIPE,Direction.EAST,d.geometry(),disabled));
        }
    }
    @Test void eligibilityAndMeshesCoverBothAxesHeadingsAndDiameterExtremes(){
        for(int face=0;face<6;face++)for(int mask=0;mask<64;mask++){
            boolean expected=face>=2&&SightGlassGeometry.supports(mask)&&(mask==0||(mask&3)==0);
            // A connected run determines its axis, independently of the old isolated pipe facing.
            if(mask!=0)expected=SightGlassGeometry.supports(mask)&&(mask&3)==0;
            assertEquals(expected,InlineMoverGeometry.supports(mask,face));
        }
        for(int face:new int[]{2,3,4,5})for(double bore:new double[]{.001,.05,1})for(boolean compressor:List.of(false,true)){
            var key=new LineGeometry.Key(Kind.PIPE,face,3<<(face&~1),PipeSizes.code(PipeSizes.outside(bore)),0,0,false);var polygons=InlineMoverGeometry.polygons(key,compressor,false);
            assertFalse(polygons.isEmpty());double top=0;for(var p:polygons)for(int i=0;i<p.positions().length;i++){assertTrue(Double.isFinite(p.positions()[i]));if(i%3==1)top=Math.max(top,p.positions()[i]);}assertTrue(top>1,"upright motor can exceed one block");assertTrue(polygons.stream().anyMatch(p->p.material()==Mesh.Material.GUARD),"body-owned flow arrow");
            for(var p:polygons){var v=p.positions();var normals=p.normals();for(int i=1;i<p.size()-1;i++){
                double ax=v[3*i]-v[0],ay=v[3*i+1]-v[1],az=v[3*i+2]-v[2],bx=v[3*i+3]-v[0],by=v[3*i+4]-v[1],bz=v[3*i+5]-v[2];
                assertTrue((ay*bz-az*by)*normals[0]+(az*bx-ax*bz)*normals[1]+(ax*by-ay*bx)*normals[2]>=-1e-9,"winding follows the native outward shading normal");
            }}
        }
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
