package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.createcheme.science.material.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import java.util.*;

/** Server-owned physical registry and hydraulic authority. Only presentation buckets inspect loaded block entities. */
public final class FluidWorldAuthority implements AutoCloseable {
    private static final Map<MinecraftServer,FluidWorldAuthority> SERVERS=new IdentityHashMap<>();
    private final MinecraftServer server;
    private final MaterialCatalog catalog;
    private final Map<FluidCheckpointCodec.PackageKey,FluidThermodynamics> models=new HashMap<>();
    private final FluidThermodynamics model;
    private final FluidPropertyReloadGuard propertyReload;
    private String propertyHold;
    private final CreateChemE.FluidOptions options;
    private final List<FluidPresetCatalog.Preset> presetCache;
    private final List<String> componentNames;
    private final FluidSavedData data;
    private final WorldTopologyLedger topology;
    private final MinecraftFluidRuntime runtime;
    /** Island ownership, compiler diagnostics and chunk membership of the registered devices, and the event path. */
    private final PhysicalRegistry registry;
    /**
     * Queued topology events apply as one batch at the next tick's hook, or earlier when something reads what they
     * change (a view, a capture, a filter recovery). A run of events inside one tick (a command placing thousands of
     * blocks) applies every this many, far inside the ledger's queue bound, so the queue never waits on a whole command.
     */
    static final int APPLY_BATCH=1024;
    /** How long an undeliverable recovery (offline player, full inventory, unloaded chunk) waits for its next attempt. */
    private static final int RECOVERY_RETRY_TICKS=20;
    private final Set<Long> unboundBindings=new HashSet<>();
    /** Engine-owned presentation: loaded devices and open menus, updated on their island's bucket only. */
    private final FluidPresentation<com.wormzjl.createcheme.world.inventory.FluidDeviceMenu> presentation;
    /** Complete immutable snapshots from the last bucket; bounded by loaded/open devices, never player replies. */
    private final Map<Long,com.wormzjl.createcheme.network.FluidNetwork.MenuData> publishedMenuData=new HashMap<>();
    private boolean closed;
    private long lastDebugChat=Long.MIN_VALUE;
    private MaterialCatalog observedSolidData;
    private String solidPropertyHold;
    private java.util.function.BiConsumer<List<IslandCoordinator.Snapshot>,Map<Long,IslandCoordinator.Metrics>> observer;

    private FluidWorldAuthority(MinecraftServer server) {
        this.server=server;owned();catalog=MaterialRuntime.active();
        options=CreateChemE.fluidOptions();data=FluidSavedData.open(server,this::model);
        model=model(new FluidCheckpointCodec.PackageKey(com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog.networkPackage(catalog)));
        propertyReload=new FluidPropertyReloadGuard(catalog,com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog.networkPackage(catalog),model);
        presetCache=FluidPresetCatalog.resolve(catalog);componentNames=model.components();
        var savedTopology=data.world().orElseGet(()->WorldTopologyLedger.Snapshot.empty(catalog));savedTopology.basis().requireCurrent(catalog);
        FluidPropertyReloadGuard.validateSolids(catalog.solids(),data.checkpoint(),savedTopology);observedSolidData=catalog;
        topology=new WorldTopologyLedger(savedTopology);
        presentation=new FluidPresentation<>(new PresentationHost(),topology.onlineTick());
        var settings=new IslandCoordinator.Settings(options.wallBudgetNanos(),options.wallBudgetNanos()*3/4,64,options.adaptiveCadence(),options.initialCadenceTicks(),options.certificates());
        // Island clocks read the world's online tick: one epoch increment per tick advances every island.
        runtime=new MinecraftFluidRuntime(server,this::published,settings,topology::onlineTick);
        runtime.coordinator().onReplayed(this::replayed);
        runtime.coordinator().onDomainHold(this::domainHeld);runtime.coordinator().nodeNames(this::deviceLabel);
        registry=new PhysicalRegistry(topology,model,new InlineFilter(options.solids().filterCapacity(),options.solids().filterResistance(),com.wormzjl.createcheme.science.fluid.state.SolidInventory.EMPTY,0),new RegistryHost());
        for(var entry:data.checkpoint().islands())runtime.register(dimension(entry.dimension()),entry.snapshot(),model(entry.key()));
        logLoad();
        registry.load();data.bindCapture(this::capture);
        // Block entities whose chunks loaded before this authority existed never reported themselves: every device
        // in an already loaded chunk is marked once, and its bucket binds and presents it. Nothing is pushed here.
        registry.forEachChunk((name,chunk,devices)->{var level=server.getLevel(dimension(name));var at=new net.minecraft.world.level.ChunkPos(chunk);
            if(level!=null&&level.hasChunk(at.x,at.z))for(long id:devices)presentation.markDirty(id);});
        // Saved recoveries are first attempted on the first tick, as before, then on their retry deadline.
        if(topology.hasRecoveries())scheduleRecoveryRetry(1);
    }
    /** What the load restored: the checkpoint format, islands, certificates saved and restored (a restored island is
     * materialised and holds only its horizon; it solves nothing at the load), and every discarded certificate. */
    private void logLoad() {
        var loaded=runtime.coordinator().observe();
        long saved=data.checkpoint().islands().stream().filter(e->e.snapshot().certificate().flatMap(IslandCoordinator.Certified::saved).isPresent()).count();
        long restored=loaded.stream().filter(s->s.certificate().isPresent()).count();
        CreateChemE.LOGGER.info("fluid_world status=LOADED format={} online_tick={} islands={} certificates_saved={} certificates_restored={} awake={}",
                FluidCheckpointCodec.VERSION,topology.onlineTick(),loaded.size(),saved,restored,loaded.size()-restored);
        for(var s:loaded)if(s.status().startsWith("WAITING: saved "))CreateChemE.LOGGER.warn("fluid_island={} status=CERTIFICATE_DISCARDED detail={}",s.id(),s.status());
    }
    public static void start(MinecraftServer server){if(SERVERS.containsKey(server))throw new IllegalStateException("Duplicate fluid world authority");SERVERS.put(server,new FluidWorldAuthority(server));}
    public static Optional<FluidWorldAuthority> find(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Fluid world lookup requires server thread");return Optional.ofNullable(SERVERS.get(server));}
    public static void tick(MinecraftServer server){find(server).ifPresent(FluidWorldAuthority::tick);}
    /** Also called before between-tick completion routing, so an old worker cannot win a reload race. */
    public static void refreshProperties(MinecraftServer server){find(server).ifPresent(FluidWorldAuthority::refreshProperties);}
    private void refreshProperties() {
        owned();if(closed)return;
        var current=MaterialRuntime.active();String next=propertyReload.inspect(current).orElse(null);
        if(current!=observedSolidData){observedSolidData=current;try{var saved=capture();FluidPropertyReloadGuard.validateSolids(current.solids(),saved.checkpoint(),saved.world());solidPropertyHold=null;}catch(IllegalArgumentException incompatible){solidPropertyHold="HELD: solid property data changed; restore the definitions this world was saved with or create a fresh world";}}
        if(next==null)next=solidPropertyHold;
        if(Objects.equals(next,propertyHold))return;
        propertyHold=next;
        if(next!=null)runtime.coordinator().suspendForPropertyChange(next);
        else runtime.coordinator().resumeQualifiedProperties();
        presentation.markAllLoaded();
        data.setDirty();
    }
    public static void stop(MinecraftServer server){find(server).ifPresent(FluidWorldAuthority::close);}
    public static void forget(MinecraftServer server){com.wormzjl.createcheme.network.ColumnV3Network.forgetPresentation(server);var world=SERVERS.remove(server);if(world!=null)world.close();}
    private void owned(){if(!server.isSameThread())throw new IllegalStateException("Fluid authority requires the logical server thread");}
    /** Every island of this world solves on a model built from the same captured options, so the
     * configured trace cutoff is one value for every worker and every retained solver: a retained
     * solver is replaced whenever its model identity changes, and the model is the cutoff's home. */
    private FluidThermodynamics model(FluidCheckpointCodec.PackageKey key){return models.computeIfAbsent(key,k->FluidThermodynamics.forNetwork(catalog,k.packageId(),options.maximumVelocity(),options.traceCutoffMoleFraction(),options.solids()));}
    private static ResourceKey<Level> dimension(String name){return ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(name));}
    public List<FluidPresetCatalog.Preset> presets(){owned();return presetCache;}
    public List<String> components(){owned();return componentNames;}
    /** The world's network property model; its {@code domain()} is where a state may be evaluated. */
    public FluidThermodynamics model(){owned();return model;}
    public Map<String,MaterialName> materialNames(){owned();var result=new LinkedHashMap<String,MaterialName>();for(String id:componentNames)result.put(id,catalog.name(id));return Map.copyOf(result);}
    public long onlineTick(){owned();return topology.onlineTick();}
    /** Optional read-only diagnostics; all values are immutable and callbacks run on the server thread. */
    public void observe(java.util.function.BiConsumer<List<IslandCoordinator.Snapshot>,Map<Long,IslandCoordinator.Metrics>> observer){owned();this.observer=observer;}
    /** Every island as stored, without materialising certified ones: a diagnostic read that advances no island. The
     * events queued since the last application are applied first, so the islands they create are among them. */
    public List<IslandCoordinator.Snapshot> diagnosticSnapshots(){owned();flush();return runtime.coordinator().observe();}
    /** Materialises every certified island to now, as a save does; the observer receives the replayed accounting. */
    public void materialiseAll(){owned();flush();runtime.coordinator().snapshots();}
    /** The rest and steady-flow certificate policy captured at server start. */
    public CertificatePolicy certificates(){owned();return options.certificates();}
    /** Why an island's last closed interval did not certify it, or null; diagnostics only. */
    public String certificationRefusal(long island){owned();return runtime.coordinator().certificationRefusal(island);}
    /** What the certificate evidence measured at an island's last usable solved interval, or null; diagnostics only. */
    public IslandCoordinator.Evidence certificationEvidence(long island){owned();return runtime.coordinator().certificationEvidence(island);}
    public Map<Long,WorldTopologyLedger.Registration> registrations(){owned();return registry.registrations();}
    /** The registration at a position (every block-entity load and binding asks). */
    public Optional<WorldTopologyLedger.Registration> at(PhysicalFluidTopology.Position position){owned();return registry.at(position);}
    /** Applied events that did nothing for the player who asked (a recovery that found no cake or a full inventory), by event, for their reply. Bounded. */
    private final LinkedHashMap<UUID,String> refusedEvents=new LinkedHashMap<>();
    private void refuseEvent(UUID event,String reason){refusedEvents.put(event,reason);if(refusedEvents.size()>WorldTopologyLedger.MAXIMUM_EVENTS)refusedEvents.remove(refusedEvents.keySet().iterator().next());}
    public WorldTopologyLedger.Registration place(PhysicalFluidTopology.Position position,TopologyCompiler.Kind kind,PhysicalFluidTopology.Direction facing) {
        owned();
        long id=topology.nextIdentity();var control=switch(kind){case PUMP->new FlowControl.Pump(.01,500000,1);case COMPRESSOR->new FlowControl.Compressor(.05,3.0,1);case VALVE->new FlowControl.PressureValve(200000);default->new FlowControl.Passive();};
        var device=new PhysicalFluidTopology.Device(id,position,kind,facing,new PipeResistance.Geometry(1,.05,PipeResistance.DEFAULT_ROUGHNESS_METRES,0),control);
        var spec=kind==TopologyCompiler.Kind.GENERATOR?FluidDeviceSpec.water(catalog):FluidDeviceSpec.nitrogen(catalog);
        // A reservoir is placed empty (decision D54): of its settings only the capacity means anything.
        if(kind==TopologyCompiler.Kind.RESERVOIR)spec=new FluidDeviceSpec(options.volume(),298.15,com.wormzjl.createcheme.science.fluid.solver.PhaseLayout.AMBIENT_PRESSURE,spec.composition());
        var record=new WorldTopologyLedger.Registration(device,spec,0);
        submit(List.of(new WorldTopologyLedger.Edit(id,record)),Math.addExact(id,1));return record;
    }
    /** Queues a configuration change as a ledger event and returns it, or null when nothing would change. */
    public WorldTopologyLedger.Event edit(long id,long expectedRevision,PhysicalFluidTopology.Device device,FluidDeviceSpec spec) {
        owned();var old=registrations().get(id);if(old==null||old.revision()!=expectedRevision||device.id()!=id)throw new IllegalStateException("Stale fluid controls");
        if(old.device().equals(device)&&old.spec().equals(spec))return null;
        return submit(List.of(new WorldTopologyLedger.Edit(id,new WorldTopologyLedger.Registration(device,spec,Math.addExact(expectedRevision,1)))),topology.nextIdentity());
    }
    public void remove(long id){owned();var old=registrations().get(id);if(old!=null)submit(List.of(new WorldTopologyLedger.Edit(id,null)),topology.nextIdentity(),old.device().kind()==TopologyCompiler.Kind.FILTER?new WorldTopologyLedger.Recovery(id,null):null);}
    /**
     * Queues a filter's solid recovery as a ledger event and returns it, as an edit is queued: nothing is applied or
     * delivered here. The engine applies it with the next batch of events, takes the cake the island holds then and
     * delivers the item; an empty filter or a full inventory at that point refuses it in the menu's next reply.
     */
    public WorldTopologyLedger.Event recoverFilter(long id,long expectedRevision,net.minecraft.server.level.ServerPlayer player){
        owned();var old=registrations().get(id);if(old==null||old.revision()!=expectedRevision||old.device().kind()!=TopologyCompiler.Kind.FILTER)throw new IllegalStateException("Stale filter controls");
        if(player.getInventory().getFreeSlot()<0)throw new IllegalStateException("Inventory full; filter unchanged");
        return submit(List.of(new WorldTopologyLedger.Edit(id,new WorldTopologyLedger.Registration(old.device(),old.spec(),Math.addExact(old.revision(),1)))),topology.nextIdentity(),new WorldTopologyLedger.Recovery(id,player.getUUID()));
    }
    private WorldTopologyLedger.Event submit(List<WorldTopologyLedger.Edit> edits,long nextId){return submit(edits,nextId,null);}
    private WorldTopologyLedger.Event submit(List<WorldTopologyLedger.Edit> edits,long nextId,WorldTopologyLedger.Recovery recovery) {
        if(closed)throw new IllegalStateException("Fluid world is closed");
        var event=registry.submit(edits,nextId,recovery);data.setDirty();
        if(registry.unapplied()>=APPLY_BATCH)applyPending();
        return event;
    }
    /** Applies the events queued since the last application before a read that must see them. */
    private void flush(){if(!closed&&registry.unapplied()>0)applyPending();}
    /**
     * The per-tick hook does only what is due. The epoch increment advances every island clock at once; the
     * coordinator's tick resets the dispatch budget and pops due deadlines (island slices and retries, round
     * timeouts, the allocator shrink, recovery retries) and pumps only if that would act;
     * queued topology events are tried while any exist - everything queued since the last tick applies here as
     * batches, each compiling only the components it touches - and an island an event created or released is
     * offered to the pump in the same tick. No island or registry is visited otherwise. Last, the
     * presentation bucket of this tick is flushed if one is due (an O(1) check otherwise), after the tick's
     * events, so its replies and views include them.
     */
    private void tick() {
        owned();if(closed)return;refreshProperties();topology.tick();
        runtime.tick();
        if(topology.hasPendingEvents()){applyPending();runtime.coordinator().pumpIfUseful();}
        presentation.tick(topology.onlineTick());
        com.wormzjl.createcheme.network.ColumnV3Network.presentationTick(server, topology.onlineTick());
        data.setDirty();
    }
    /** Attempted when a recovery is queued and on its retry deadline, never polled; delivery stays exactly-once. */
    private void deliverRecoveries(){
        if(!topology.hasRecoveries())return;
        int delivered=0;boolean capped=false;for(var entry:topology.recoveries().entrySet()){
            if(delivered++>=64){capped=true;break;}var pending=entry.getValue();var stack=com.wormzjl.createcheme.world.item.RecoveredSolidsItem.create(entry.getKey(),pending);boolean accepted=false;
            if(pending.player()!=null){var player=server.getPlayerList().getPlayer(pending.player());if(player!=null){for(int slot=0;slot<player.getInventory().getContainerSize();slot++)if(com.wormzjl.createcheme.world.item.RecoveredSolidsItem.carriesTransfer(player.getInventory().getItem(slot),entry.getKey())){accepted=true;break;}if(!accepted&&player.getInventory().getFreeSlot()>=0)accepted=player.getInventory().add(stack);}}
            else {var position=pending.position();var level=server.getLevel(dimension(position.dimension()));var pos=new net.minecraft.core.BlockPos(position.x(),position.y(),position.z());
                if(level!=null&&level.hasChunkAt(pos))accepted=level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack));}
            if(accepted){topology.deliveredRecovery(entry.getKey());data.setDirty();}
        }
        // An offline player, a full inventory or an unloaded chunk leaves the record queued for a bounded retry;
        // more than one batch continues on the next tick.
        if(topology.hasRecoveries())scheduleRecoveryRetry(capped?1:RECOVERY_RETRY_TICKS);
    }
    private void scheduleRecoveryRetry(int ticks) {
        var coordinator=runtime.coordinator();coordinator.schedule(IslandScheduler.Kind.RECOVERY_RETRY,Math.addExact(coordinator.now(),ticks),()->{if(!closed)deliverRecoveries();});
    }
    private void applyPending() {
        if(propertyHold!=null)return;
        // A newly queued recovery is attempted at once, as the old per-tick attempt after applying events did.
        if(registry.applyPending())deliverRecoveries();
        data.setDirty();
    }
    /** A chunk loaded: its registered devices are marked for their buckets, which bind and present them. Nothing is pushed. */
    public void loadedChunk(ResourceKey<Level> dimension,long chunk){owned();for(long id:registry.chunk(dimension.location().toString(),chunk))presentation.markDirty(id);}
    /** A device's block entity loaded or was bound: marked for its bucket, never presented at once. */
    public void deviceLoaded(long id){owned();if(!closed)presentation.deviceLoaded(id);}
    /** A device's block entity unloaded or was removed: publications stop marking it. */
    public void deviceUnloaded(long id){owned();presentation.deviceUnloaded(id);publishedMenuData.remove(id);}
    /** Marks a device for its next bucket (a test or tool asking for a refresh gets it on the engine's schedule). */
    public void markViewDirty(long id){owned();presentation.markDirty(id);}
    /** An open menu becomes a consumer of its device's bucket. The first delivery comes with that bucket. */
    public void subscribe(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu){owned();if(!closed)presentation.subscribe(menu,menu.identity());}
    /** Called after openMenu has installed the requested menu and queued its opening packet. */
    public boolean replayOnOpen(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu){
        owned();return !closed&&presentation.replayOnOpen(menu);
    }
    public void unsubscribe(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu){owned();presentation.unsubscribe(menu);}
    /**
     * A player input from a menu, already validated and queued as a ledger event (or refused): recorded for the
     * reply its menu receives with its next bucket. Returns nothing to the handler, which sends nothing.
     */
    public void input(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu,WorldTopologyLedger.Event event,String refusal) {
        owned();long now=topology.onlineTick();
        presentation.input(menu,menu.identity(),refusal!=null?FluidPresentation.Input.refused(now,refusal):event==null?new FluidPresentation.Input(now,null,null,-1):FluidPresentation.Input.queued(now,event.id(),event.tick()));
    }
    /** Diagnostics: what one menu has been delivered, bucket by bucket. */
    public List<FluidPresentation.Delivery> deliveries(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu){owned();return presentation.deliveries(menu);}
    public FluidPresentation.Stats presentationStats(){owned();return presentation.stats();}
    /** Diagnostics: a device's bucket key (its island, or itself without one); its buckets fall at ticks congruent to it modulo 100. */
    public long presentationKey(long device){owned();flush();return bucketKey(device);}
    /** The bucket key as the presentation reads it, with no event applied: marking a device never forces a batch. */
    private long bucketKey(long device){Long owner=registry.owner(device);return owner!=null?owner:device;}
    public boolean viewDirty(long device){owned();return presentation.dirty(device);}
    public FluidSavedData.Capture capture() {
        owned();flush();var islands=new ArrayList<FluidCheckpointCodec.IslandEntry>();var world=topology.snapshot();
        for(var snapshot:runtime.coordinator().snapshots()) {
            if(registry.members(snapshot.id()).isEmpty())throw new IllegalStateException("Fluid island "+snapshot.id()+" holds no device; refusing to save it");
            String dimension=world.active().get(registry.members(snapshot.id()).getFirst()).device().position().dimension();
            islands.add(new FluidCheckpointCodec.IslandEntry(dimension,com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog.networkPackage(catalog),snapshot));
        }
        return new FluidSavedData.Capture(new FluidCheckpointCodec.Checkpoint(islands),world);
    }
    /** A device's view, as a presentation read: queued events are not applied (they apply on the engine's tick), and a
     * certified island is materialised to now but never woken by it. */
    public FluidView view(long id){owned();return view(id,new HashMap<>());}
    /** {@code islands} caches island reads, so one bucket reads each island once however many of its devices it presents. */
    private FluidView view(long id,Map<Long,IslandCoordinator.Snapshot> islands) {
        FluidRuntimeDiagnostics.count(FluidRuntimeDiagnostics.viewBuilds);var registration=registrations().get(id);if(registration==null)return new FluidView(id,0,0,topology.onlineTick(),"REMOVED",null,0,List.of());
        Long owner=registry.owner(id);if(owner==null)return new FluidView(id,registration.revision(),0,topology.onlineTick(),!topology.active().containsKey(id)?"WAITING: topology event":Objects.requireNonNullElse(registry.diagnostic(id),"NO FLOW: no hydraulic boundary"),null,0,List.of(),null,0,List.of(),null);
        var snapshot=islands.computeIfAbsent(owner,runtime.coordinator()::presentation);FluidView.State state=null;int nodeIndex=-1;boolean empty=false;
        for(int i=0;i<snapshot.graph().reservoirs().size();i++)if(snapshot.graph().reservoirs().get(i).id()==id){nodeIndex=i;var node=snapshot.graph().reservoirs().get(i);empty=node.empty();if(!empty)state=FluidView.State.from(node.state(),node.kind()==PassiveNetwork.NodeKind.RESERVOIR?node.inventory().volume():node.state().volume());break;}
        InlineFilter filter=null;if(registration.device().kind()==TopologyCompiler.Kind.FILTER){state=null;for(var p:snapshot.graph().pipes())if(p.id()==PhysicalFluidTopology.filterIdentity(id))filter=p.filter();}
        var history=new ArrayList<PipeTransfer>();double flow=0;Double devicePressureChange=null;String status=snapshot.status();
        // Decision D54: a placed tank holds nothing until something fills it.
        if(empty)status="EMPTY: holds nothing / "+status;
        if(snapshot.lastResult().isPresent()) {
            var result=snapshot.lastResult().orElseThrow();var q=result.averageMassFlows();
            for(int i=0;i<snapshot.graph().pipes().size();i++){var pipe=snapshot.graph().pipes().get(i);if(pipe.first()==nodeIndex)flow+=q[i];if(pipe.second()==nodeIndex)flow-=q[i];}
            for(var mapping:registry.pipeViews(id))for(var value:result.pipeTransfers())if(value.pipeId()==mapping.pipeId()) {
                history.add(value);
                for(int edge=0;edge<snapshot.graph().pipes().size();edge++)if(snapshot.graph().pipes().get(edge).id()==value.pipeId()&&result.endpointModes().get(edge).name().contains("VELOCITY_LIMIT"))status+=" / "+result.endpointModes().get(edge);
            }
            if(!history.isEmpty())flow=(history.getFirst().forward().massKg()-history.getFirst().reverse().massKg())/result.advancedSeconds();
            if(registration.device().actuator())for(int i=0;i<snapshot.graph().pipes().size();i++){var pipe=snapshot.graph().pipes().get(i);if(pipe.first()==nodeIndex&&!(pipe.control() instanceof FlowControl.Passive)){flow=q[i];devicePressureChange=result.endpointHeads()[i];status+=" / "+result.endpointModes().get(i);
                if(pipe.control() instanceof FlowControl.Mover mover&&!snapshot.graph().reservoirs().get(pipe.first()).empty()) {
                    if(result.endpointModes().get(i)==FlowControl.Mode.INLET_WRONG_PHASE&&result.endpointModes().size()==snapshot.graph().pipes().size())status+=" / "+inletReason(snapshot.graph(),i,result.advancedSeconds());
                    // Owner decision D59 (4): a pump on its suction limit delivers what its suction line gives at the vapour
                    // pressure.
                    else if(result.endpointModes().get(i)==FlowControl.Mode.PUMP_SUCTION_LIMIT)status+=" / pump cavitating: suction at the vapour pressure";
                    // The limit on the suction stream the mover draws (its own junction, what arrives at its inlet; decision
                    // D4): a pump's setting is the rise for water and scales with the suction density, a compressor's is the
                    // ratio over the suction pressure.
                    else {var suction=snapshot.graph().reservoirs().get(pipe.first()).state();double limit=mover.riseLimit(suction.mass()/suction.volume(),suction.pressure(),model.pumpReferenceDensity());
                        status+=String.format(java.util.Locale.ROOT,mover instanceof FlowControl.Pump?" (limit %.0f Pa on this fluid)":" (limit %.0f Pa at this suction)",limit);}
                }}}
        }
        // A generator or a void in several islands (owner decision D17) reads the status of its owner (the lowest island) and
        // the net flow of all of them.
        if(registration.device().fixed())for(long other:registry.islands(id))if(other!=owner) {
            var shared=islands.computeIfAbsent(other,runtime.coordinator()::presentation);if(shared.lastResult().isEmpty())continue;var q=shared.lastResult().orElseThrow().averageMassFlows();
            int index=-1;for(int i=0;i<shared.graph().reservoirs().size();i++)if(shared.graph().reservoirs().get(i).id()==id)index=i;
            for(int i=0;i<shared.graph().pipes().size();i++){var pipe=shared.graph().pipes().get(i);if(pipe.first()==index)flow+=q[i];if(pipe.second()==index)flow-=q[i];}
        }
        if(filter!=null)for(var pipe:snapshot.graph().pipes())if(pipe.id()==PhysicalFluidTopology.filterIdentity(id))devicePressureChange=snapshot.graph().reservoirs().get(pipe.first()).state().pressure()-snapshot.graph().reservoirs().get(pipe.second()).state().pressure();
        if(filter!=null&&filter.clogged())status="filter clogged / "+status;
        var closures=snapshot.lastResult().map(PassiveIntervalSolver.Result::rejectionReasons).orElse(Map.of());
        for(var mapping:registry.pipeViews(id))for(var pipe:snapshot.graph().pipes())if(pipe.id()==mapping.pipeId()&&pipe.blockedDirections()!=0&&pipe.filter()==null)status=solidClosure(mapping.pipeId(),closures)+" / "+status;
        if(registry.diagnostic(id)!=null)status=registry.diagnostic(id)+" / "+status;
        var active=topology.active().get(id);if(active==null||active.revision()!=registration.revision())status="WAITING: configuration event / "+status;
        if(unboundBindings.contains(id))status="UNBOUND: saved inventory retained / "+status;
        var routes=new ArrayList<FluidView.PipeRoute>();
        for(var historyEntry:history)for(var pipe:snapshot.graph().pipes())if(pipe.id()==historyEntry.pipeId()) {
            routes.add(new FluidView.PipeRoute(pipe.id(),nodeLabel(snapshot.graph().reservoirs().get(pipe.first()).id()),nodeLabel(snapshot.graph().reservoirs().get(pipe.second()).id())));
        }
        double seconds=snapshot.lastResult().map(PassiveIntervalSolver.Result::advancedSeconds).orElse(0.0);
        var view=new FluidView(id,registration.revision(),snapshot.clock().committedTick(),topology.onlineTick(),status,state,flow,history,devicePressureChange,
                seconds,routes,filter);
        if(registration.device().kind()==TopologyCompiler.Kind.RESERVOIR&&nodeIndex>=0)view=view.withOutlets(TankOutlets.of(model,snapshot.graph(),nodeIndex,snapshot.lastResult().orElse(null),registry.tankFaces(id)));
        if(registration.device().kind()==TopologyCompiler.Kind.PIPE||registration.device().kind()==TopologyCompiler.Kind.FILTER)
            view=view.withPipeInfo(PipePresentation.withBulkSpeed(
                PipePresentation.inspect(registration.device(),snapshot.graph(),registry.pipeViews(id),history,seconds,componentNames.size()),
                runtime.coordinator().bulkVolumeRates(owner),registration.device().geometry().area()));
        return view;
    }
    /**
     * Why this connection carries nothing, in the words the solver used, for the device status a
     * player reads. The event integrator names the connection and the two velocities in the reason
     * it records against every closure, and a closure the interval started with is recorded again
     * each interval, so the account stays true for as long as the connection is shut. Nothing else
     * carries it: the blocked mask on the pipe says only that something closed it.
     *
     * <p>The status line is bounded and wraps to three lines on the screen, so this stays to one
     * short clause. A reason with no velocity of its own - a population limit, a stopped filter -
     * is named without one rather than with two zeroes, and an unrecognised or missing record
     * falls back to the bare text the mask alone justifies.
     */
    static String solidClosure(long pipeId,Map<String,Integer> reasons) {
        for(String key:reasons.keySet()) {
            if(!key.startsWith(SOLID_CLOSURE)||!key.contains("; pipe="+pipeId+";"))continue;
            int end=key.indexOf(';',SOLID_CLOSURE.length());if(end<0)break;
            double velocity=closureField(key,"; velocity="),minimum=closureField(key,"; deposition=");
            return SOLID_CLOSURE+key.substring(SOLID_CLOSURE.length(),end)
                    +(minimum>0?" ("+speed(velocity)+" m/s, needs "+speed(minimum)+" m/s)":"");
        }
        return "blocked with solid";
    }
    private static final String SOLID_CLOSURE="blocked with solid: ";
    private static double closureField(String key,String token) {
        int at=key.indexOf(token);if(at<0)return 0;
        int end=key.indexOf(';',at+token.length());
        try{return Double.parseDouble(key.substring(at+token.length(),end<0?key.length():end));}catch(RuntimeException unreadable){return 0;}
    }
    private static String speed(double value) {
        return String.format(java.util.Locale.ROOT,Math.abs(value)>=.01?"%.2f":"%.2e",value);
    }
    /** A node as a status line names it: a device by kind and position, anything else as the pipe junction it is. */
    private String deviceLabel(long id) {
        var registration=topology.active().get(id);if(registration==null)return "pipe junction "+id;
        var p=registration.device().position();return registration.device().kind().name().toLowerCase(java.util.Locale.ROOT)+" at "+p.x()+", "+p.y()+", "+p.z();
    }
    /**
     * One WARN per island per thermo-domain hold, a phase set the network cannot carry (D21 option C; the coordinator
     * rate-limits repeats of the same island and hold): where, which package, its reason key and state, and what to do
     * about it. Leaving a data range is no hold (owner rule of 2026-10-16).
     */
    private void domainHeld(long island,com.wormzjl.createcheme.science.fluid.thermo.ThermoHold hold,String where,boolean parked) {
        String dimension="unknown";
        var members=registry.members(island);if(!members.isEmpty()){var record=topology.active().get(members.getFirst());if(record!=null)dimension=record.device().position().dimension();}
        Object node=hold.node()==com.wormzjl.createcheme.science.fluid.thermo.ThermoHold.NO_NODE?"unknown":hold.node();String device=where.isEmpty()?"unknown":where;
        CreateChemE.LOGGER.warn("fluid_island={} dimension={} node={} device={} status=THERMO_DOMAIN code={} package={} temperature={} pressure={} waits_for_inputs={} detail={} action=Change what the island mixes or the conditions it holds: the network carries one hydrocarbon liquid, one vapour, water and the package's crystals",
                island,dimension,node,device,hold.reasonKey().substring(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold.REASON_PREFIX.length()),hold.packageId(),hold.temperature(),hold.pressure(),parked,hold.getMessage());
    }
    /**
     * Why a mover is refused (decision D6), as the solver's inlet check states it on the committed state the next slice
     * decides on ({@link InletPhase#reason}), with the node the supply walk ended at named as a device. Pure arithmetic on
     * the committed state's stored phase amounts: no solve and no flash. Its transport closures read as open, as that
     * slice reads them (owner decision D33, {@link NetworkReduction#reopened}).
     */
    private String inletReason(PassiveNetwork graph,int edge,double seconds) {
        var mover=(FlowControl.Mover)graph.pipes().get(edge).control();
        var supply=InletPhase.supply(model,NetworkReduction.reopened(graph),edge,mover.targetVolumeFlow(),seconds);
        return InletPhase.reason(mover,supply).replace("from node "+supply.nodeId()+")","from "+deviceLabel(supply.nodeId())+")");
    }
    private String nodeLabel(long id) {
        var registration=topology.active().get(id);if(registration==null)return "Junction "+id;
        var p=registration.device().position();return p.x()+", "+p.y()+", "+p.z();
    }
    /** The world side of {@link FluidPresentation}: ownership, views, loaded block entities and menu delivery. */
    private final class PresentationHost implements FluidPresentation.Host<com.wormzjl.createcheme.world.inventory.FluidDeviceMenu> {
        public long key(long device){return bucketKey(device);}
        public long revision(long device){var record=registrations().get(device);return record==null?-1:record.revision();}
        public boolean eventPending(UUID event){for(var queued:topology.events())if(queued.id().equals(event))return true;return false;}
        public String eventRefusal(UUID event){return refusedEvents.remove(event);}
        public boolean open(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu) {
            var player=menu.serverPlayer();return player!=null&&!player.hasDisconnected()&&player.containerMenu==menu&&menu.stillValid(player);
        }
        public FluidView view(long device,Map<Long,IslandCoordinator.Snapshot> islands){
            var view=FluidWorldAuthority.this.view(device,islands);
            var record=registrations().get(device);
            if(record!=null&&(record.device().kind()==TopologyCompiler.Kind.PIPE||record.device().kind()==TopologyCompiler.Kind.FILTER)&&view.pipeInfo()==null)
                view=view.withPipeInfo(FluidView.PipeInfo.empty(componentNames.size()));
            if(record!=null)publishedMenuData.put(device,com.wormzjl.createcheme.network.FluidNetwork.snapshot(FluidWorldAuthority.this,record,view));
            return view;
        }
        public long replay(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu,long device){
            var data=publishedMenuData.get(device);if(data==null)return -1;
            com.wormzjl.createcheme.network.FluidNetwork.deliver(menu,true,data,"");
            return data.view().inputRevision();
        }
        /** Binds a loaded block entity that does not yet carry its identity, then hands it the view. Never loads a chunk. */
        public boolean present(long id,java.util.function.Supplier<FluidView> view) {
            var record=registrations().get(id);if(record==null)return false;var p=record.device().position();var level=server.getLevel(dimension(p.dimension()));if(level==null)return false;
            var pos=new BlockPos(p.x(),p.y(),p.z());if(!level.hasChunkAt(pos))return false;
            if(!(level.getBlockState(pos).getBlock() instanceof com.wormzjl.createcheme.world.level.block.FluidDeviceBlock block)||block.kind()!=record.device().kind()) {
                if(unboundBindings.add(id))CreateChemE.LOGGER.warn("fluid_binding={} status=UNBOUND detail=Loaded block mismatch; saved inventory and topology retained",id);
                return false;
            }
            unboundBindings.remove(id);
            if(level.getBlockEntity(pos) instanceof com.wormzjl.createcheme.world.level.block.entity.FluidDeviceBlockEntity entity&&entity.fluidIdentity()!=id)entity.bindIdentity(id);
            if(!(level.getBlockEntity(pos) instanceof FluidView.Receiver receiver)||receiver.fluidIdentity()!=id)return false;
            receiver.acceptFluidView(view.get());return true;
        }
        public void failed(long device,RuntimeException failure){CreateChemE.LOGGER.error("fluid_presentation device={} status=FAILED detail=The view could not be presented at its bucket",device,failure);}
        public void deliver(com.wormzjl.createcheme.world.inventory.FluidDeviceMenu menu,long device,boolean withStatic,FluidView view,String reply) {
            var data=publishedMenuData.get(device);
            if(data!=null)com.wormzjl.createcheme.network.FluidNetwork.deliver(menu,withStatic,data,reply);
        }
    }
    /** The world side of {@link PhysicalRegistry}: the runtime's islands, players for recoveries, and what a commit changes here. */
    private final class RegistryHost implements PhysicalRegistry.Host {
        public IslandCoordinator coordinator(){return runtime.coordinator();}
        public void topology(String name,Set<UUID> events,Set<Long> affected,List<IslandCoordinator.Replacement> replacements,long committed,long online,
                             Map<Long,PassiveNetwork.Reservoir> additions,Set<Long> removals,Map<Long,InlineFilter> releasedFilters,Runnable commit) {
            runtime.topology(dimension(name),events,affected,replacements,model,committed,online,additions,removals,releasedFilters,commit);
        }
        public boolean acceptsRecovery(UUID player){var online=server.getPlayerList().getPlayer(player);return online!=null&&online.getInventory().getFreeSlot()>=0;}
        public void refused(UUID event,String reason){refuseEvent(event,reason);}
        public void committed(Set<Long> removed,Set<Long> recompiled) {
            unboundBindings.removeAll(removed);publishedMenuData.keySet().removeAll(removed);
            // Every recompiled device's cached snapshot predates the change, and one left in no island (a pipe whose only
            // tank was broken) gets no publication that would mark it: each is marked for its next bucket, which presents
            // its new view (its replacement island, or "no hydraulic boundary"); an opening menu waits for that bucket.
            publishedMenuData.keySet().removeAll(recompiled);for(long device:recompiled)presentation.markIfLoaded(device);
            // Open menus follow their devices to the replacement islands' buckets.
            presentation.rekey();
        }
    }
    /** A certified island's materialisation: only accounting (the diagnostic observer) and the save mark need it.
     * It queues no view refresh of its own. */
    private void replayed(List<IslandCoordinator.Snapshot> changed) {
        if(observer!=null){var timings=new HashMap<Long,IslandCoordinator.Metrics>();for(var island:changed)runtime.coordinator().metrics(island.id()).ifPresent(m->timings.put(island.id(),m));observer.accept(changed,Map.copyOf(timings));}
        data.setDirty();
    }
    private void published(List<IslandCoordinator.Snapshot> changed) {
        if(observer!=null){var timings=new HashMap<Long,IslandCoordinator.Metrics>();for(var island:changed)runtime.coordinator().metrics(island.id()).ifPresent(m->timings.put(island.id(),m));observer.accept(changed,Map.copyOf(timings));}
        // A thermo-domain hold has its own, rate-limited line with the full detail (domainHeld); every other hold keeps this one.
        data.setDirty();var ids=new HashSet<Long>();for(var snapshot:changed){ids.add(snapshot.id());if(snapshot.status().startsWith("HELD")&&!snapshot.status().startsWith(com.wormzjl.createcheme.runtime.ProcessSolveServices.THERMO_DOMAIN))CreateChemE.LOGGER.warn("fluid_island={} committed_tick={} status={}",snapshot.id(),snapshot.clock().committedTick(),snapshot.status());}
        // Only loaded devices are marked; their bucket presents them. A publication itself pushes nothing.
        for(long island:ids)for(long physical:registry.members(island))presentation.markIfLoaded(physical);
        if(options.debugChat()&&(lastDebugChat==Long.MIN_VALUE||topology.onlineTick()-lastDebugChat>=20)) {
            changed.stream().filter(s->s.status().startsWith("HELD")).findFirst().ifPresent(s->{
                lastDebugChat=topology.onlineTick();String message="Fluid network "+s.id()+": "+s.status();if(message.length()>180)message=message.substring(0,180)+" (see log)";
                var text=net.minecraft.network.chat.Component.literal(message);for(var player:server.getPlayerList().getPlayers())player.sendSystemMessage(text);
            });
        }
    }
    @Override public void close(){owned();if(closed)return;closed=true;publishedMenuData.clear();runtime.close();data.setDirty();}
}
