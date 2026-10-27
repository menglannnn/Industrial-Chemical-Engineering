package com.wormzjl.icheme.runtime.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wormzjl.icheme.runtime.BoundedCpuSolveService;
import com.wormzjl.icheme.runtime.ProcessSolveServices;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.state.ParticleSize;
import com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.material.MaterialRuntime;
import com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * The unified thermo port's test world on the engine path, without Minecraft (tools/unified-thermo-port-test-world,
 * not a gate). Each row file of the test world (rows/NN-*.json) is replayed on the real runtime the game runs (the
 * {@link PhysicalRegistry} and {@link IslandCoordinator} with one synchronous worker, ticked at 20 per online second,
 * as tools/unified-thermo-port-m3/hold-fuzz's World), on the pilot package {@code icheme:pilot_cryogenic} with the
 * game's defaults (1 m3 tanks, 2 s wall budget, rest certificates). A device is placed with the game's defaults and
 * configured with an edit event, as FluidWorldAuthority.place and FluidNetwork.edit do; the mixtures are the network's
 * fluid presets and the test world's custom preset files. The in-game driver's own time is mirrored: 1.7 s per placed
 * block, 6 s of typing before an edit and the driver's wait after it, 1.5 s plus 1 s per view for a read. Every read
 * prints what the device's screen shows (state, solids at one size per solid, outlets, closures, mover modes).
 *
 * <p>System properties: testWorld.rows (the rows folder), testWorld.presets (the mixture preset folder),
 * testWorld.only (comma-separated row ids; default all but R6-R12).
 */
class TestWorldRigTest {
    static final String DIM="minecraft:overworld";
    static final BoundedCpuSolveService.CancellationToken NEVER=new BoundedCpuSolveService.CancellationToken() {
        public long deadlineNanos(){return Long.MAX_VALUE;}public boolean isDeadlineExceeded(){return false;}
        public boolean isCancellationRequested(){return false;}public void throwIfCancellationRequested(){}
    };
    static final MaterialCatalog CATALOG=PilotCryogenicTestCatalog.catalog();
    static final String PACKAGE=FluidMaterialCatalog.networkPackage(CATALOG);
    static final List<String> BASIS=basis();
    static List<String> basis(){var ids=new ArrayList<>(CATALOG.requirePackage(PACKAGE).components());ids.add("Water");return List.copyOf(ids);}

    // ---------------------------------------------------------------- the world (hold-fuzz's World, with edits and removals)
    static final class World {
        final FluidThermodynamics model;
        final WorldTopologyLedger ledger=new WorldTopologyLedger(WorldTopologyLedger.Snapshot.empty(CATALOG));
        final Map<Long,IslandCoordinator.Attempt> attempts=new LinkedHashMap<>();
        final Map<Long,ProcessSolveServices.FluidIslandCommand> commands=new HashMap<>();
        final List<String> events=new ArrayList<>();
        final Map<Long,WorldTopologyLedger.Registration> records=new HashMap<>();
        long requests;
        final IslandCoordinator coordinator;
        final PhysicalRegistry registry;
        World(FluidThermodynamics model) {
            this.model=model;
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
            FlowControl control=switch(kind){case PUMP->mover(FlowControl.Pump.class,.01,500000);case COMPRESSOR->mover(FlowControl.Compressor.class,.05,3.0);case VALVE->new FlowControl.PressureValve(200000);default->new FlowControl.Passive();};
            var device=new PhysicalFluidTopology.Device(id,at,kind,facing,new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0),control);
            var spec=kind==Kind.GENERATOR?FluidDeviceSpec.water(CATALOG):FluidDeviceSpec.nitrogen(CATALOG);
            if(kind==Kind.RESERVOIR)spec=new FluidDeviceSpec(1,298.15,com.wormzjl.icheme.science.fluid.solver.PhaseLayout.AMBIENT_PRESSURE,spec.composition());
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
                long start=attempt.slice().startTick();boolean m5=start>=m5From&&start<=m5To&&m5Left>0;if(m5){m5Left--;System.out.println("RIG "+row+" M5 island="+attempt.islandId()+" slice="+start+"-"+attempt.slice().endTick());}
                if(m5)m5(true,model);
                long t0=System.nanoTime();ProcessSolveServices.FluidIslandSolveResult result;
                try{result=(ProcessSolveServices.FluidIslandSolveResult)command.solve(NEVER);}finally{if(m5)m5(false,model);}
                coordinator.completed(attempt,Optional.of(result));
                if(start>=histFrom&&start<=histTo)history(attempt,command,result,System.nanoTime()-t0);
            }
            coordinator.pump();
        }
        void run(double seconds){long end=ledger.onlineTick()+Math.round(seconds*20);while(ledger.onlineTick()<end)tick();}
        // ------------------------------------------------ 2026-10-18 hold traces (H1-H4): per-solve history, M5 windows
        String row="";long histFrom=Long.MAX_VALUE,histTo=-1,m5From=Long.MAX_VALUE,m5To=-1;int m5Left=Integer.getInteger("testWorld.m5Max",1);
        void history(IslandCoordinator.Attempt a,ProcessSolveServices.FluidIslandCommand c,ProcessSolveServices.FluidIslandSolveResult r,long nanos) {
            var snap=coordinator.observe(a.islandId());var cand=r.candidate().orElse(null);
            String detail=r.detail()==null?"":r.detail();if(detail.length()>600)detail=detail.substring(0,600)+"...";
            System.out.println(String.format(Locale.ROOT,"RIG %s HIST t=%.2fs island=%d slice=%d-%d (%.2f s) status=%s committed=%d adv=%s sub=%s %.0f ms detail=%s",row,ledger.onlineTick()/20.0,a.islandId(),
                    a.slice().startTick(),a.slice().endTick(),c.durationSeconds(),snap.status().length()>160?snap.status().substring(0,160)+"...":snap.status(),snap.clock().committedTick(),
                    cand==null?"-":String.format(Locale.ROOT,"%.4f",cand.advancedSeconds()),cand==null?"-":cand.acceptedSubsteps()+"/"+cand.rejectedSubsteps(),nanos/1e6,detail));
            if(cand!=null&&!cand.rejectionReasons().isEmpty())System.out.println("RIG "+row+"   reasons "+cand.rejectionReasons());
            var g=snap.graph();
            for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);if(n.fixed())continue;var st=n.state();var inv=n.inventory();
                double solids=0;if(inv!=null)for(var p:inv.solids().oneSizePerSolid().populations())solids+=p.massKg();
                System.out.println(String.format(Locale.ROOT,"RIG %s   n%d %s id=%d z=%.0f T=%.4f P=%.2f m=%.5f L=%.5e V=%.5e W=%.4e S=%.4f kg moles=%s liq=%s vap=%s",row,i,n.kind(),n.id(),n.elevation(),
                        st.temperature(),st.pressure(),st.mass(),st.liquidVolume(),st.vaporVolume(),st.waterVolume(),solids,inv==null?"-":fmt(inv.moles()),fmt(st.liquidView()),fmt(st.vaporView())));}
            if(cand!=null){var b=new StringBuilder("RIG "+row+"   flows");for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);b.append(String.format(Locale.ROOT," e%d(%d->%d %s %s)=%.5g",e,p.first(),p.second(),p.firstPort(),
                    e<cand.endpointModes().size()?cand.endpointModes().get(e):"-",cand.averageMassFlows()[e]));}System.out.println(b);}
        }
        static void m5(boolean on,FluidThermodynamics model) {
            try {
                var c=Class.forName("com.wormzjl.icheme.science.fluid.network.M5RowTrace");
                if(on){c.getField("MODEL").set(null,model);c.getMethod("install").invoke(null);c.getMethod("reset").invoke(null);
                    try{Class.forName("com.wormzjl.icheme.science.fluid.thermo.M9CheckTrace").getMethod("install").invoke(null);}catch(ClassNotFoundException absent){}}
                @SuppressWarnings("unchecked") var on0=(ThreadLocal<Boolean>)c.getField("ON").get(null);on0.set(on);
            }catch(ReflectiveOperationException e){throw new IllegalStateException("testWorld.m5 needs a PATCH export with M5RowTrace (EXTRA_PROBES): "+e,e);}
        }
    }
    /** A mover as the game builds it: S6 (9445106a) removed the efficiency; before it the game passed 1 (FluidNetwork.edit). */
    static FlowControl mover(Class<? extends FlowControl> type,double flow,double limit) {
        try {
            for(var c:type.getDeclaredConstructors()) {
                if(c.getParameterCount()==2)return (FlowControl)c.newInstance(flow,limit);
                if(c.getParameterCount()==3)return (FlowControl)c.newInstance(flow,limit,1.0);
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        throw new IllegalStateException("no mover constructor on "+type);
    }
    /** "ROW@from-to,ROW@from-to" (online seconds) for this row: {from,to} in ticks, or null. */
    static long[] window(String property,String row) {
        for(var part:System.getProperty(property,"").split(",")) {
            int at=part.indexOf(64);if(at<0||!part.substring(0,at).equals(row))continue;
            var range=part.substring(at+1).split("-");
            return new long[]{Math.round(Double.parseDouble(range[0])*20),Math.round(Double.parseDouble(range[1])*20)};
        }
        return null;
    }

    // ---------------------------------------------------------------- mixtures: the network's fluid presets and the custom files
    record Mixture(double[] composition,SlurryFeed solids) {}
    static Mixture mixture(String name) {
        double[] n=new double[BASIS.size()];
        switch(name) {
            case "Water"->n[BASIS.indexOf("Water")]=1;
            case "Nitrogen"->n[BASIS.indexOf("Nitrogen")]=1;
            case "Methane"->n[BASIS.indexOf("Methane")]=1;
            case "Ethane"->n[BASIS.indexOf("Ethane")]=1;
            case "Carbon dioxide"->n[BASIS.indexOf("CarbonDioxide")]=1;
            case "Water slurry (10% solids, 100 µm)"->{n[BASIS.indexOf("Water")]=1;return new Mixture(n,SlurryFeed.demo());}
            default->{
                try(var files=Files.list(Path.of(System.getProperty("testWorld.presets")))) {
                    for(var file:files.sorted().toList()) {
                        var o=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                        if(!o.get("name").getAsString().equals(name))continue;
                        double total=0;for(var c:o.getAsJsonArray("components")){var row=c.getAsJsonObject();double a=row.get("amount").getAsDouble();n[BASIS.indexOf(row.get("id").getAsString())]+=a;total+=a;}
                        for(int i=0;i<n.length;i++)n[i]/=total;
                        var solids=SlurryFeed.NONE;
                        if(o.has("solids")){var s=o.getAsJsonObject("solids");var grades=new ArrayList<SlurryFeed.Grade>();
                            for(var g:s.getAsJsonArray("grades")){var row=g.getAsJsonObject();grades.add(new SlurryFeed.Grade(row.get("material").getAsString(),ParticleSize.micrometres(row.get("diameter_um").getAsBigDecimal().toPlainString()),row.get("mass_share").getAsDouble()));}
                            solids=new SlurryFeed(s.get("volume_fraction_percent").getAsDouble()/100,grades);}
                        return new Mixture(n,solids);
                    }
                }catch(java.io.IOException e){throw new RuntimeException(e);}
                throw new IllegalArgumentException("no mixture named "+name);
            }
        }
        return new Mixture(n,SlurryFeed.NONE);
    }

    // ---------------------------------------------------------------- one row
    static Kind kind(String block){return switch(block){case "generator"->Kind.GENERATOR;case "reservoir"->Kind.RESERVOIR;case "pipe"->Kind.PIPE;case "void"->Kind.VOID;case "pump"->Kind.PUMP;case "compressor"->Kind.COMPRESSOR;case "valve"->Kind.VALVE;default->null;};}
    static PhysicalFluidTopology.Direction facing(JsonObject b){return PhysicalFluidTopology.Direction.valueOf(b.has("facing")?b.get("facing").getAsString().toUpperCase(Locale.ROOT):"NORTH");}

    final Map<String,Long> ids=new HashMap<>();
    String row;World w;JsonObject blocks;

    void runRow(Path file) throws Exception {
        var spec=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        row=spec.get("id").getAsString();blocks=spec.getAsJsonObject("blocks");ids.clear();
        w=new World(FluidThermodynamics.forNetwork(CATALOG,PACKAGE));w.row=row;
        var hw=window("testWorld.history",row);if(hw!=null){w.histFrom=hw[0];w.histTo=hw[1];}
        var mw=window("testWorld.m5",row);if(mw!=null){w.m5From=mw[0];w.m5To=mw[1];}
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
                case "reload"->out("reload: not replayed by the rig");
                default->{}
            }
            for(var e:w.events)out("event %s",e);w.events.clear();
        }
        out("END %s at t=%.1fs, %.1f s wall",row,w.ledger.onlineTick()/20.0,(System.nanoTime()-started)/1e9);
    }
    static List<String> names(JsonArray a){var list=new ArrayList<String>();for(var e:a)list.add(e.getAsString());return list;}

    /** An edit as FluidNetwork.edit builds it from the screen's controls. */
    void set(JsonObject s) {
        String id=s.get("id").getAsString();long d=ids.get(id);var old=w.records.get(d);var device=old.device();var spec=old.spec();
        var geometry=device.geometry();var control=device.control();
        switch(device.kind()) {
            case PIPE->geometry=new PipeResistance.Geometry(geometry.length(),s.get("diameter").getAsDouble(),PipeResistance.DEFAULT_ROUGHNESS_METRES,geometry.minorLoss());
            case PUMP->control=mover(FlowControl.Pump.class,s.get("flow").getAsDouble(),s.get("head").getAsDouble());
            case COMPRESSOR->control=mover(FlowControl.Compressor.class,s.get("flow").getAsDouble(),s.get("ratio").getAsDouble());
            case VALVE->control=new FlowControl.PressureValve(s.get("pressure").getAsDouble());
            case GENERATOR->{var m=mixture(s.get("mixture").getAsString());spec=new FluidDeviceSpec(spec.volume(),s.get("temperatureK").getAsDouble(),s.get("pressure").getAsDouble(),m.composition(),m.solids());}
            case VOID->spec=new FluidDeviceSpec(spec.volume(),spec.temperature(),s.get("pressure").getAsDouble(),spec.composition());
            default->throw new IllegalArgumentException("no settings on "+device.kind());
        }
        try{if(device.kind()==Kind.GENERATOR||device.kind()==Kind.VOID)spec.validate(w.model,device.kind());}
        catch(RuntimeException refused){out("set %s refused at the handler: %s",id,refused.getMessage());return;}
        try{w.edit(d,new PhysicalFluidTopology.Device(d,device.position(),device.kind(),device.facing(),geometry,control),spec);}
        catch(RuntimeException refused){var inner=(Throwable)refused;while(inner.getCause()!=null)inner=inner.getCause();
            w.records.put(d,old);out("set %s refused at submit (the reply reads \"Not applied: ...\"): %s",id,inner.getMessage());return;}
        out("set %s %s",id,s);
    }

    /** The F1/F2 bisect traces (TestWorldTrace) on the committed graph of the island holding the named device. */
    void trace(JsonObject s) {
        String kind=s.get("kind").getAsString(),name=s.get("id").getAsString();Long id=ids.get(name);
        if(id==null){out("trace %s %s: not placed",kind,name);return;}
        for(long island:w.registry.islands(id)) {
            var snap=w.coordinator.observe(island);var g=snap.graph();int node=-1;
            for(int i=0;i<g.reservoirs().size();i++)if(g.reservoirs().get(i).id()==id)node=i;
            if(node<0)continue;
            var result=snap.lastResult().orElse(null);
            out("trace %s %s island=%d committed=%.1fs status=%s",kind,name,island,snap.clock().committedTick()/20.0,snap.status());
            java.util.function.Consumer<String> line=text->out("%s",text);
            if(kind.equals("f1"))TestWorldTrace.f1(w.model,g,result,node,line);else if(kind.equals("h4"))TestWorldTrace.h4(w.model,g,node,line);else TestWorldTrace.f2(w.model,g,result,node,line);
        }
    }

    // ---------------------------------------------------------------- reads: what the device's screen shows
    void read(String name,String tag) {
        Long id=ids.get(name);if(id==null){out("read %s: not placed",name);return;}
        var record=w.records.get(id);var kind=record.device().kind();
        String head=String.format(Locale.ROOT,"t=%.1fs %s %s %s",w.ledger.onlineTick()/20.0,tag,name,kind);
        if(kind==Kind.PIPE){junction(id,head);pipe(name,id,head);return;}
        for(long island:w.registry.islands(id)) {
            var snap=w.coordinator.observe(island);var g=snap.graph();int node=-1;
            for(int i=0;i<g.reservoirs().size();i++)if(g.reservoirs().get(i).id()==id)node=i;
            if(node<0)continue;
            var n=g.reservoirs().get(node);var st=n.state();var inv=n.inventory();
            out("%s island=%d committed=%.1fs status=%s",head,island,snap.clock().committedTick()/20.0,snap.status());
            if(n.empty()){out("    EMPTY");continue;}
            boolean actuator=kind==Kind.PUMP||kind==Kind.COMPRESSOR||kind==Kind.VALVE;
            if(!actuator){
            double cap=kind==Kind.RESERVOIR?inv.volume():st.volume();
            out("    T=%.2f K (%.2f C) P=%.1f Pa V=%.4f/%.4f m3 (fill %.1f %%) mass=%.3f kg liquid=%.4e water=%.4e vapour=%.4e solids=%.4e m3",
                    st.temperature(),st.temperature()-273.15,st.pressure(),st.volume(),cap,100*st.volume()/cap,st.mass(),st.liquidVolume(),st.waterVolume(),st.vaporVolume(),inv.solids().moments().volume());
            out("    moles %s liquid %s vapour %s water %.4e/%.4e",fmt(inv.moles()),fmt(st.liquidView()),fmt(st.vaporView()),st.waterLiquid(),st.waterVapor());
            for(var p:inv.solids().oneSizePerSolid().populations())out("    solid %s %.1f um %.6f kg",p.material().id(),p.size().diameterMetres()*1e6,p.massKg());
            }
            var result=snap.lastResult().orElse(null);
            if(kind==Kind.RESERVOIR&&result!=null)for(var o:TankOutlets.of(w.model,g,node,result,w.registry.tankFaces(id)))
                out("    outlet %s %s mass %.4f kg/s (%.3f kmol/h) drawn=%d head=%.3f kPa",o.face(),o.port(),o.massFlow(),o.moleFlow()*3.6,o.drawn(),o.head()/1000);
            if(result!=null)for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);
                if(p.first()!=node&&p.second()!=node)continue;
                String mode=e<result.endpointModes().size()?String.valueOf(result.endpointModes().get(e)):"-";
                String reason="";
                if(p.control() instanceof FlowControl.Mover&&"INLET_WRONG_PHASE".equals(mode))try{reason=" / "+InletPhase.reason(w.model,g,e,result.advancedSeconds());}catch(RuntimeException x){reason=" / reason: "+x;}
                out("    edge %d pipe %d %d->%d ports %s/%s %s mode=%s q=%.5f kg/s head=%.1f Pa blocked=%d%s%s",e,p.id(),p.first(),p.second(),p.firstPort(),p.secondPort(),p.control().getClass().getSimpleName(),
                        mode,result.averageMassFlows()[e],e<result.endpointHeads().length?result.endpointHeads()[e]:Double.NaN,p.blockedDirections(),
                        p.blockedDirections()!=0?" / "+FluidWorldAuthority.solidClosure(p.id(),result.rejectionReasons()):"",reason);}
            if(result!=null)out("    interval %.2f s, substeps %d/%d rejected, reasons %s",result.advancedSeconds(),result.acceptedSubsteps(),result.rejectedSubsteps(),result.rejectionReasons());
        }
    }
    /** A pipe block with three or more connections is a junction node of its island (its holdup m_J, 0.6.0). */
    void junction(long id,String head) {
        for(var snap:w.coordinator.observe())for(var n:snap.graph().reservoirs())if(n.id()==id&&n.kind()==PassiveNetwork.NodeKind.JUNCTION) {
            var st=n.state();
            out("%s island=%d status=%s JUNCTION T=%.2f K P=%.1f Pa mass=%.5f kg liquid=%.3e vapour=%.3e solids=%.3e m3 moles %s",head,snap.id(),snap.status(),st.temperature(),st.pressure(),st.mass(),
                    st.liquidVolume(),st.vaporVolume(),n.inventory().solids().moments().volume(),fmt(n.inventory().moles()));
        }
    }
    void pipe(String name,long id,String head) {
        for(var view:w.registry.pipeViews(id))for(var snap:w.coordinator.observe()) {
            var g=snap.graph();var result=snap.lastResult().orElse(null);
            for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);if(p.id()!=view.pipeId())continue;
                var first=g.reservoirs().get(p.first());var second=g.reservoirs().get(p.second());
                out("%s island=%d status=%s pipe %d %s(%d)->%s(%d) ports %s/%s blocked=%d q=%s%s",head,snap.id(),snap.status(),p.id(),first.kind(),first.id(),second.kind(),second.id(),p.firstPort(),p.secondPort(),
                        p.blockedDirections(),result==null?"-":String.format(Locale.ROOT,"%.5f kg/s",result.averageMassFlows()[e]),
                        result!=null&&p.blockedDirections()!=0?" / "+FluidWorldAuthority.solidClosure(p.id(),result.rejectionReasons()):"");
                if(result!=null)for(var t:result.pipeTransfers())if(t.pipeId()==p.id())out("    carried forward %.4f kg (solids %.6f kg) reverse %.4f kg over %.2f s",t.forward().massKg(),t.forward().solids().massKg(),t.reverse().massKg(),result.advancedSeconds());
            }
        }
    }
    static String fmt(double[] v){var b=new StringBuilder("[");for(int i=0;i<v.length;i++){if(i>0)b.append(' ');b.append(String.format(Locale.ROOT,"%.4g",v[i]));}return b.append(']').toString();}
    void out(String format,Object...args){System.out.println("RIG "+row+" "+String.format(Locale.ROOT,format,args));System.out.flush();}

    @Test void rows() throws Exception {
        String only=System.getProperty("testWorld.only","");var wanted=only.isBlank()?null:Set.of(only.split(","));
        for(var folder:System.getProperty("testWorld.rows").split(";"))try(var files=Files.list(Path.of(folder))) {
            for(var file:files.filter(f->f.toString().endsWith(".json")).sorted().toList()) {
                var id=JsonParser.parseString(Files.readString(file)).getAsJsonObject().get("id").getAsString();
                if(wanted==null?id.equals("R6-R12"):!wanted.contains(id))continue;
                MaterialRuntime.with(CATALOG,PACKAGE,()->{try{runRow(file);}catch(Throwable t){out("FAILED %s",t);for(var e:t.getStackTrace())if(e.getClassName().startsWith("com.wormzjl"))System.out.println("RIG   at "+e);}return null;});
            }
        }
    }
}
