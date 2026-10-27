package com.wormzjl.icheme.runtime.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wormzjl.icheme.fluid.support.FluidTestSupport;
import com.wormzjl.icheme.runtime.ProcessSolveServices;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * F2 on main 0.8.0 (tools/unified-thermo-port-test-world, F1/F2 bisect 2026-10-16; not a gate): the test world rig's row
 * replay (TestWorldRigTest) reduced to main's API, on main's bundled package with inert solids (water with 10 % of 100 um
 * demo particles). The world is main's EventPathWorld (the real PhysicalRegistry and IslandCoordinator, one synchronous
 * worker, 20 ticks per online second); the driver's time per placed block, edit and read is the rig's.
 * System properties: testWorld.rows (the rows folder), testWorld.only (comma-separated row ids).
 */
class MainSlurryRigTest {
    static final String DIM="minecraft:overworld";
    static final MaterialCatalog CATALOG=MaterialCatalog.bundled();

    static final class World {
        final FluidThermodynamics model=FluidTestSupport.networkModel();
        final WorldTopologyLedger ledger=new WorldTopologyLedger(WorldTopologyLedger.Snapshot.empty(CATALOG));
        final Map<Long,IslandCoordinator.Attempt> attempts=new LinkedHashMap<>();
        final Map<Long,ProcessSolveServices.FluidIslandCommand> commands=new HashMap<>();
        final List<String> events=new ArrayList<>();
        final Map<Long,WorldTopologyLedger.Registration> records=new HashMap<>();
        long requests;
        final IslandCoordinator coordinator;
        final PhysicalRegistry registry;
        World() {
            coordinator=new IslandCoordinator(new IslandCoordinator.Dispatcher() {
                public int availableWorkers(){return 1-attempts.size();}
                public long nextRequestId(){return ++requests;}
                public boolean submit(IslandCoordinator.Attempt attempt,ProcessSolveServices.FluidIslandCommand command){attempts.put(attempt.slice().requestId(),attempt);commands.put(attempt.slice().requestId(),command);return true;}
                public void cancel(long request){}
            },changed->{for(var s:changed)if(s.status().startsWith("HELD"))events.add(String.format(Locale.ROOT,"t=%.1fs island %d HELD at committed tick %d: %s",ledger.onlineTick()/20.0,s.id(),s.clock().committedTick(),s.status()));},
                    ()->0L,new IslandCoordinator.Settings(2_000_000_000L,1_500_000_000L,64,false,100,CertificatePolicy.defaults()),ledger::onlineTick);
            registry=new PhysicalRegistry(ledger,model,InlineFilter.empty(),new PhysicalRegistry.Host() {
                public IslandCoordinator coordinator(){return coordinator;}
                public void topology(String dimension,Set<UUID> events,Set<Long> affected,List<IslandCoordinator.Replacement> replacements,long committed,long online,
                                     Map<Long,PassiveNetwork.Reservoir> additions,Set<Long> removals,Map<Long,InlineFilter> releasedFilters,Runnable commit) {
                    coordinator.topology(events,affected,replacements,model,committed,online,additions,removals,releasedFilters,commit);
                }
                public boolean acceptsRecovery(UUID player){return true;}
                public void refused(UUID event,String reason){World.this.events.add(String.format(Locale.ROOT,"t=%.1fs event refused: %s",ledger.onlineTick()/20.0,reason));}
                public void committed(Set<Long> removed,Set<Long> recompiled){}
            });
            registry.load();
        }
        long place(Kind kind,PhysicalFluidTopology.Position at,PhysicalFluidTopology.Direction facing) {
            long id=ledger.nextIdentity();
            FlowControl control=switch(kind){case PUMP->new FlowControl.Pump(.01,500000,1);case COMPRESSOR->new FlowControl.Compressor(.05,3.0,1);case VALVE->new FlowControl.PressureValve(200000);default->new FlowControl.Passive();};
            var device=new PhysicalFluidTopology.Device(id,at,kind,facing,new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0),control);
            var spec=kind==Kind.GENERATOR?FluidDeviceSpec.water(CATALOG):FluidDeviceSpec.nitrogen(CATALOG);
            var record=new WorldTopologyLedger.Registration(device,spec,0);records.put(id,record);
            registry.submit(List.of(new WorldTopologyLedger.Edit(id,record)),id+1,null);
            return id;
        }
        void edit(long id,PhysicalFluidTopology.Device device,FluidDeviceSpec spec) {
            var old=records.get(id);var record=new WorldTopologyLedger.Registration(device,spec,old.revision()+1);records.put(id,record);
            registry.submit(List.of(new WorldTopologyLedger.Edit(id,record)),ledger.nextIdentity(),null);
        }
        void remove(long id){records.remove(id);registry.submit(List.of(new WorldTopologyLedger.Edit(id,null)),ledger.nextIdentity(),null);}
        void tick() {
            ledger.tick();coordinator.tick();
            if(ledger.hasPendingEvents()){registry.applyPending();coordinator.pumpIfUseful();}
            for(long request:List.copyOf(attempts.keySet())) {
                var attempt=attempts.remove(request);var command=commands.remove(request);
                var result=(ProcessSolveServices.FluidIslandSolveResult)command.solve(FluidTestSupport.NEVER);
                coordinator.completed(attempt,Optional.of(result));
            }
            coordinator.pump();
        }
        void run(double seconds){long end=ledger.onlineTick()+Math.round(seconds*20);while(ledger.onlineTick()<end)tick();}
    }

    static Kind kind(String block){return switch(block){case "generator"->Kind.GENERATOR;case "reservoir"->Kind.RESERVOIR;case "pipe"->Kind.PIPE;case "void"->Kind.VOID;case "pump"->Kind.PUMP;case "compressor"->Kind.COMPRESSOR;case "valve"->Kind.VALVE;default->null;};}
    static PhysicalFluidTopology.Direction facing(JsonObject b){return PhysicalFluidTopology.Direction.valueOf(b.has("facing")?b.get("facing").getAsString().toUpperCase(Locale.ROOT):"NORTH");}
    static List<String> names(JsonArray a){var list=new ArrayList<String>();for(var e:a)list.add(e.getAsString());return list;}

    final Map<String,Long> ids=new HashMap<>();
    String row;World w;JsonObject blocks;

    void runRow(Path file) throws Exception {
        var spec=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        row=spec.get("id").getAsString();blocks=spec.getAsJsonObject("blocks");ids.clear();
        w=new World();
        out("ROW %s: %s",row,spec.get("title").getAsString());
        long started=System.nanoTime();
        for(var element:spec.getAsJsonArray("steps")) {
            var s=element.getAsJsonObject();String op=s.get("op").getAsString();
            switch(op) {
                case "place"->{for(var id:names(s.getAsJsonArray("ids"))){var b=blocks.getAsJsonObject(id);var k=kind(b.get("block").getAsString());
                    if(k!=null){var at=b.getAsJsonArray("at");ids.put(id,w.place(k,new PhysicalFluidTopology.Position(DIM,at.get(0).getAsInt(),at.get(1).getAsInt(),at.get(2).getAsInt()),facing(b)));}
                    w.run(1.7);}}
                case "set"->{w.run(6);set(s);w.run(s.has("waitMs")?s.get("waitMs").getAsDouble()/1000:9);}
                case "wait"->w.run(s.get("s").getAsDouble());
                case "read"->{var views=s.has("views")?s.getAsJsonArray("views").size():1;
                    for(var id:names(s.getAsJsonArray("ids"))){read(id,s.has("tag")?s.get("tag").getAsString():"");w.run(1.5+views);}}
                case "remove","break"->{for(var id:names(s.getAsJsonArray("ids"))){Long d=ids.remove(id);if(d!=null)w.remove(d);w.run(1.7);}}
                case "note"->out("note: %s",s.get("text").getAsString());
                case "trace"->trace(s);
                default->{}
            }
            for(var e:w.events)out("event %s",e);w.events.clear();
        }
        out("END %s at t=%.1fs, %.1f s wall",row,w.ledger.onlineTick()/20.0,(System.nanoTime()-started)/1e9);
    }

    void set(JsonObject s) {
        String id=s.get("id").getAsString();long d=ids.get(id);var old=w.records.get(d);var device=old.device();var spec=old.spec();
        var geometry=device.geometry();var control=device.control();
        switch(device.kind()) {
            case PIPE->geometry=new PipeResistance.Geometry(geometry.length(),s.get("diameter").getAsDouble(),PipeResistance.DEFAULT_ROUGHNESS_METRES,geometry.minorLoss());
            case PUMP->control=new FlowControl.Pump(s.get("flow").getAsDouble(),s.get("head").getAsDouble(),1);
            case VALVE->control=new FlowControl.PressureValve(s.get("pressure").getAsDouble());
            case GENERATOR->{String m=s.get("mixture").getAsString();var water=FluidDeviceSpec.water(CATALOG).composition();
                var ids=new ArrayList<>(CATALOG.requirePackage(com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog.networkPackage(CATALOG)).components());ids.add("Water");
                double[] methane=new double[ids.size()];if(ids.contains("Methane"))methane[ids.indexOf("Methane")]=1;
                double t=s.get("temperatureK").getAsDouble(),p=s.get("pressure").getAsDouble();
                spec=switch(m){case "Water"->new FluidDeviceSpec(spec.volume(),t,p,water,SlurryFeed.NONE);
                    case "Water slurry (10% solids, 100 µm)"->new FluidDeviceSpec(spec.volume(),t,p,water,SlurryFeed.demo());
                    case "Methane slurry (10% solids, 100 µm)"->new FluidDeviceSpec(spec.volume(),t,p,methane,SlurryFeed.demo());
                    default->throw new IllegalArgumentException("no mixture "+m);};}
            default->throw new IllegalArgumentException("no settings on "+device.kind());
        }
        try{w.edit(d,new PhysicalFluidTopology.Device(d,device.position(),device.kind(),device.facing(),geometry,control),spec);}
        catch(RuntimeException refused){w.records.put(d,old);out("set %s refused at submit: %s",id,refused);return;}
        out("set %s %s",id,s);
    }

    void read(String name,String tag) {
        Long id=ids.get(name);if(id==null){out("read %s: not placed",name);return;}
        var kind=w.records.get(id).device().kind();
        String head=String.format(Locale.ROOT,"t=%.1fs %s %s %s",w.ledger.onlineTick()/20.0,tag,name,kind);
        if(kind==Kind.PIPE){pipe(id,head);return;}
        for(long island:w.registry.islands(id)) {
            var snap=w.coordinator.observe(island);var g=snap.graph();int node=-1;
            for(int i=0;i<g.reservoirs().size();i++)if(g.reservoirs().get(i).id()==id)node=i;
            if(node<0)continue;
            var n=g.reservoirs().get(node);var st=n.state();var inv=n.inventory();
            out("%s island=%d committed=%.1fs status=%s",head,island,snap.clock().committedTick()/20.0,snap.status());
            if(kind==Kind.RESERVOIR) {
                out("    T=%.2f K P=%.1f Pa V=%.4f/%.4f m3 mass=%.3f kg liquid=%.4e water=%.4e vapour=%.4e solids=%.4e m3",
                        st.temperature(),st.pressure(),st.volume(),inv.volume(),st.mass(),st.liquidVolume(),st.waterVolume(),st.vaporVolume(),inv.solids().moments().volume());
                for(var p:inv.solids().populations())out("    solid %s %.1f um %.6f kg",p.material().id(),p.size().diameterMetres()*1e6,p.massKg());
            }
            var result=snap.lastResult().orElse(null);
            if(result!=null)for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);
                if(p.first()!=node&&p.second()!=node)continue;
                String mode=e<result.endpointModes().size()?String.valueOf(result.endpointModes().get(e)):"-";
                String reason="";
                if(p.control() instanceof FlowControl.Mover&&"INLET_WRONG_PHASE".equals(mode))try{reason=" / "+InletPhase.reason(w.model,g,e,result.advancedSeconds());}catch(RuntimeException x){reason=" / reason: "+x;}
                out("    edge %d pipe %d %d->%d ports %s/%s %s mode=%s q=%.5f kg/s blocked=%d%s%s",e,p.id(),p.first(),p.second(),p.firstPort(),p.secondPort(),p.control().getClass().getSimpleName(),
                        mode,result.averageMassFlows()[e],p.blockedDirections(),p.blockedDirections()!=0?" / "+FluidWorldAuthority.solidClosure(p.id(),result.rejectionReasons()):"",reason);}
            if(result!=null)out("    interval %.2f s, substeps %d/%d rejected, reasons %s",result.advancedSeconds(),result.acceptedSubsteps(),result.rejectedSubsteps(),result.rejectionReasons());
        }
    }
    void pipe(long id,String head) {
        for(var view:w.registry.pipeViews(id))for(var snap:w.coordinator.observe()) {
            var g=snap.graph();var result=snap.lastResult().orElse(null);
            for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);if(p.id()!=view.pipeId())continue;
                var first=g.reservoirs().get(p.first());var second=g.reservoirs().get(p.second());
                out("%s island=%d status=%s pipe %d %s(%d)->%s(%d) ports %s/%s blocked=%d q=%s%s",head,snap.id(),snap.status(),p.id(),first.kind(),first.id(),second.kind(),second.id(),p.firstPort(),p.secondPort(),
                        p.blockedDirections(),result==null?"-":String.format(Locale.ROOT,"%.5f kg/s",result.averageMassFlows()[e]),
                        result!=null&&p.blockedDirections()!=0?" / "+FluidWorldAuthority.solidClosure(p.id(),result.rejectionReasons()):"");
            }
        }
    }
    void trace(JsonObject s) {
        String name=s.get("id").getAsString();Long id=ids.get(name);
        if(id==null){out("trace %s: not placed",name);return;}
        for(long island:w.registry.islands(id)) {
            var snap=w.coordinator.observe(island);var g=snap.graph();int node=-1;
            for(int i=0;i<g.reservoirs().size();i++)if(g.reservoirs().get(i).id()==id)node=i;
            if(node<0)continue;
            out("trace f2 %s island=%d committed=%.1fs status=%s",name,island,snap.clock().committedTick()/20.0,snap.status());
            TestWorldTrace.f2(w.model,g,snap.lastResult().orElse(null),node,text->out("%s",text));
        }
    }
    void out(String format,Object...args){System.out.println("RIG "+row+" "+String.format(Locale.ROOT,format,args));System.out.flush();}

    @Test void rows() throws Exception {
        String only=System.getProperty("testWorld.only","");var wanted=only.isBlank()?null:Set.of(only.split(","));
        try(var files=Files.list(Path.of(System.getProperty("testWorld.rows")))) {
            for(var file:files.filter(f->f.toString().endsWith(".json")).sorted().toList()) {
                var id=JsonParser.parseString(Files.readString(file)).getAsJsonObject().get("id").getAsString();
                if(wanted!=null&&!wanted.contains(id))continue;
                try{runRow(file);}catch(Throwable t){out("FAILED %s",t);for(var e:t.getStackTrace())if(e.getClassName().startsWith("com.wormzjl"))System.out.println("RIG   at "+e);}
            }
        }
    }
}
