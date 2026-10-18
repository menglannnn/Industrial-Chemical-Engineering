package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import java.util.*;

/**
 * The physical registry's compiled form and its topology events, apart from the server: which island owns each
 * registered device, what the compiler said about each device, which devices sit in which chunk, and the path that
 * queues an edit as a ledger event and later applies it to the islands. {@link FluidWorldAuthority} owns one and
 * supplies the world through {@link Host}; a test supplies an {@link IslandCoordinator} of its own. An event costs time
 * in the part of the registry it touches, never in the size of the world: {@link #submit} walks from the edited
 * position through the devices no island owns and no queued event edits, and stops at the first device an island owns
 * (that island is fenced whole) or a queued event edits (that event orders this one); {@link #applyPending} applies the
 * queued events in batches (every queued event of one tick and one dimension whose owners are aligned), compiling only
 * the connected components of the devices it touched and of the islands it replaces. The compiled form is kept per
 * device, so replacing a component replaces exactly the entries of its devices; an island no event touched is never
 * read, recompiled, materialised or replaced.
 */
public final class PhysicalRegistry {
    /** What the registry needs from its world. Called on the owning thread only. */
    public interface Host {
        IslandCoordinator coordinator();
        /** Replaces the affected islands for a batch of events (see {@link IslandCoordinator#topology}); {@code commit}
         * runs inside, once the change is validated. */
        void topology(String dimension,Set<UUID> events,Set<Long> affected,List<IslandCoordinator.Replacement> replacements,long committed,long online,
                      Map<Long,PassiveNetwork.Reservoir> additions,Set<Long> removals,Map<Long,InlineFilter> releasedFilters,Runnable commit);
        /** Whether a recovery's player can take its item now (online, with a free slot). */
        boolean acceptsRecovery(UUID player);
        /** An applied event did nothing for the player who asked; the reason reaches them with the reply to their input. */
        void refused(UUID event,String reason);
        /** An application committed; {@code removed} are the devices it took out of the registry, {@code recompiled} the
         * surviving devices of the part it compiled: their island (or the absence of one) may have changed. */
        void committed(Set<Long> removed,Set<Long> recompiled);
    }
    private record ChunkKey(String dimension,long chunk) {}
    private final WorldTopologyLedger topology;
    private final FluidThermodynamics model;
    private final double[] weights;
    private final InlineFilter emptyFilter;
    private final Host host;
    private final Map<Long,String> diagnostics=new HashMap<>();
    private final Map<Long,List<PhysicalFluidTopology.View>> pipeViews=new HashMap<>();
    private final Map<Long,Map<Long,PhysicalFluidTopology.Direction>> tankFaces=new HashMap<>();
    /** Each owned device's island; for a fixed boundary (a generator or a void), the lowest of its islands. */
    private final Map<Long,Long> owners=new HashMap<>();
    /**
     * Every island of each owned fixed boundary. A generator or a void belongs to every island that reaches it (owner
     * decision D17): islands that meet only at one are separate, and an edit near one changes only the islands its walk
     * reaches through devices that are not fixed boundaries. An edit of the boundary itself changes all of them.
     */
    private final Map<Long,TreeSet<Long>> fixedOwners=new HashMap<>();
    /** The state of every applied fixed boundary, the one every island holding it carries (nothing changes it). */
    private final Map<Long,PassiveNetwork.Reservoir> fixedStates=new HashMap<>();
    private final Map<Long,List<Long>> members=new HashMap<>();
    private final Map<ChunkKey,Set<Long>> chunkMembers=new HashMap<>();
    private int unapplied;

    /** {@code emptyFilter} is a new filter's cake: the configured capacity and resistance, nothing captured. */
    public PhysicalRegistry(WorldTopologyLedger topology,FluidThermodynamics model,InlineFilter emptyFilter,Host host) {
        this.topology=Objects.requireNonNull(topology);this.model=Objects.requireNonNull(model);weights=model.molecularWeights();
        this.emptyFilter=Objects.requireNonNull(emptyFilter);this.host=Objects.requireNonNull(host);
    }
    /** Compiles the loaded registry once and binds every device to the island a checkpoint restored for it. */
    public void load() {
        var islands=host.coordinator().snapshots();
        // A saved island is found again by what only it holds: its connections (compiled pipe identities, filters included),
        // its tanks, and for an island with no connection its one boundary. A generator or a void may be in several islands.
        var stock=new HashMap<Long,PassiveNetwork.Reservoir>();var cakes=new HashMap<Long,InlineFilter>();var byPipe=new HashMap<Long,Long>();var byTank=new HashMap<Long,Long>();var byLone=new HashMap<Long,Long>();
        for(var island:islands) {
            for(var node:island.graph().reservoirs())if(!node.junction()&&node.id()>0) {
                stock.put(node.id(),node);if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR)byTank.put(node.id(),island.id());
                if(island.graph().pipes().isEmpty())byLone.put(node.id(),island.id());
            }
            for(var pipe:island.graph().pipes()){byPipe.put(pipe.id(),island.id());if(pipe.filter()!=null)cakes.put(pipe.id()-Long.MIN_VALUE,pipe.filter());}
        }
        for(var r:topology.latest().values())if(r.device().kind()==TopologyCompiler.Kind.FILTER)cakes.putIfAbsent(r.device().id(),emptyFilter);
        var active=topology.active();
        for(var entry:stock.entrySet()){var r=active.get(entry.getKey());if(r!=null&&r.device().fixed())fixedStates.put(entry.getKey(),entry.getValue());}
        // A disconnected filter's ports stand at the ambient (NetworkReduction.ambient), as an empty vessel does.
        var compiled=PhysicalFluidTopology.compile(active.values().stream().map(WorldTopologyLedger.Registration::device).toList(),stock,cakes,
                com.wormzjl.createcheme.science.fluid.network.NetworkReduction.ambient(model));
        diagnostics.putAll(compiled.diagnostics());pipeViews.putAll(compiled.pipeViews());tankFaces.putAll(compiled.tankFaces());
        for(var island:compiled.islands()) {
            var graph=island.graph();
            var ids=java.util.stream.Stream.concat(java.util.stream.Stream.concat(graph.pipes().stream().map(p->byPipe.get(p.id())),
                    graph.reservoirs().stream().filter(n->n.kind()==PassiveNetwork.NodeKind.RESERVOIR).map(n->byTank.get(n.id()))),
                    graph.pipes().isEmpty()?graph.reservoirs().stream().filter(n->!n.junction()&&n.id()>0).map(n->byLone.get(n.id())):java.util.stream.Stream.<Long>empty())
                    .filter(Objects::nonNull).distinct().toList();
            if(ids.size()!=1)throw new IllegalStateException("Saved topology and hydraulic ownership disagree");
            long saved=ids.getFirst();if(members.containsKey(saved))throw new IllegalStateException("Saved topology and hydraulic ownership disagree");
            var devices=island.physicalIds().stream().sorted().toList();members.put(saved,devices);for(long id:devices)bind(id,saved);
        }
        // Every island the checkpoint restored holds a device: one that compiles to none would be saved again with nothing
        // to name it by (D32, the island of one a join left behind).
        for(var island:islands)if(!members.containsKey(island.id()))throw new IllegalStateException("Saved island "+island.id()+" holds no device");
        var registered=new HashSet<Long>();for(var r:active.values())if(r.device().kind()==TopologyCompiler.Kind.RESERVOIR)registered.add(r.device().id());
        var owned=new HashSet<Long>();for(var s:islands)for(var n:s.graph().reservoirs())if(n.kind()==PassiveNetwork.NodeKind.RESERVOIR)owned.add(n.id());
        if(!registered.equals(owned))throw new IllegalStateException("Physical registry would lose or duplicate saved reservoir ownership");
        for(var r:active.values())chunkAdd(r);
    }

    // ---- reads ----

    /** Every registration with the queued events applied: what the world has been told, applied or not. A live view. */
    public Map<Long,WorldTopologyLedger.Registration> registrations(){return topology.latest();}
    /** The registration at a position, queued events applied. */
    public Optional<WorldTopologyLedger.Registration> at(PhysicalFluidTopology.Position position){return Optional.ofNullable(topology.latestAt(position));}
    /** The island that owns a device, or null; for a generator or a void in several islands, the lowest of them. */
    public Long owner(long device){return owners.get(device);}
    /** Every island a device belongs to (several only for a generator or a void, owner decision D17), in identity order. */
    public Set<Long> islands(long device){return Collections.unmodifiableSet(new TreeSet<>(islandsOf(device)));}
    /** The devices an island owns, in identity order. */
    public List<Long> members(long island){return members.getOrDefault(island,List.of());}
    /** What the compiler said about a device (a dead end, no boundary, an invalid pump), or null. */
    public String diagnostic(long device){return diagnostics.get(device);}
    /** The compiled connections a device's view reports. */
    public List<PhysicalFluidTopology.View> pipeViews(long device){return pipeViews.getOrDefault(device,List.of());}
    /** A tank's compiled connections by the face each leaves it by (compiled pipe id to face), from the same compile as its island. */
    public Map<Long,PhysicalFluidTopology.Direction> tankFaces(long tank){return tankFaces.getOrDefault(tank,Map.of());}
    /** The applied devices in one chunk. */
    public Collection<Long> chunk(String dimension,long chunk){var ids=chunkMembers.get(new ChunkKey(dimension,chunk));return ids==null?List.of():Collections.unmodifiableSet(ids);}
    @FunctionalInterface public interface ChunkVisitor{void visit(String dimension,long chunk,Collection<Long> devices);}
    /** Every chunk that holds an applied device, with its devices. */
    public void forEachChunk(ChunkVisitor visitor){chunkMembers.forEach((key,ids)->visitor.visit(key.dimension(),key.chunk(),Collections.unmodifiableSet(ids)));}
    /** A filter's captured solids: its island's, or an empty cake for a registered filter no island holds yet; null otherwise. */
    public InlineFilter cake(long filter) {
        Long island=owners.get(filter);
        if(island!=null)for(var pipe:host.coordinator().snapshot(island).graph().pipes())if(pipe.filter()!=null&&pipe.id()==PhysicalFluidTopology.filterIdentity(filter))return pipe.filter();
        var record=topology.latest().get(filter);return record!=null&&record.device().kind()==TopologyCompiler.Kind.FILTER?emptyFilter:null;
    }
    /** Events queued since the last {@link #applyPending()}. */
    public int unapplied(){return unapplied;}

    // ---- events ----

    /**
     * Queues edits as one ledger event and fences it on the islands it touches; {@link #applyPending()} applies it. The
     * touched set is the edited devices, every device joined to an edited position before or after the edit, and every
     * device reached from them through applied devices that no island owns and no queued event edits. An owned device
     * stands for its island, which the fence covers whole, and a queued device for the event that edits it; the walk
     * stops at both. A fixed boundary (a generator or a void) the event does not edit stops the walk and stands for none
     * of its islands (owner decision D17); one the event edits stands for all of them.
     */
    public WorldTopologyLedger.Event submit(List<WorldTopologyLedger.Edit> edits,long nextId,WorldTopologyLedger.Recovery recovery) {
        var latest=topology.latest();var proposed=new HashMap<Long,Optional<WorldTopologyLedger.Registration>>();var proposedAt=new HashMap<PhysicalFluidTopology.Position,WorldTopologyLedger.Registration>();
        var starts=new LinkedHashSet<PhysicalFluidTopology.Position>();var touched=new HashSet<Long>();
        for(var edit:edits) {
            var change=proposed.get(edit.id());var old=change!=null?change.orElse(null):latest.get(edit.id());if(old!=null){starts.add(old.device().position());proposedAt.remove(old.device().position(),old);}
            touched.add(edit.id());
            if(edit.replacement()==null)proposed.put(edit.id(),Optional.empty());
            else {
                var record=edit.replacement();starts.add(record.device().position());proposed.put(edit.id(),Optional.of(record));proposedAt.put(record.device().position(),record);
                if(record.device().boundary()&&(old==null||!record.spec().equals(old.spec()))){var initialized=record.spec().initialize(record.device(),model,()->{});MaterialRuntime.active().solids().validate(initialized.inventory().solids());}
            }
        }
        // The registry this event proposes: the queued one with these edits on top.
        java.util.function.Function<PhysicalFluidTopology.Position,WorldTopologyLedger.Registration> at=p->{
            var mine=proposedAt.get(p);if(mine!=null)return mine;var record=topology.latestAt(p);return record==null||proposed.containsKey(record.device().id())?null:record;
        };
        var queue=new ArrayDeque<PhysicalFluidTopology.Position>(starts);var seen=new HashSet<PhysicalFluidTopology.Position>();
        // Both sides of a removed link are visited, then connections are followed in the proposed registry. A neighbour
        // the edited position joins neither before nor after the edit is not touched: it shares no flow path with the
        // edit, and fencing its island would make the event wait on an unrelated network (a held one, for good).
        for(var start:starts)for(var d:PhysicalFluidTopology.Direction.values()) {
            var next=start.offset(d);
            if(joined(topology.latestAt(start),topology.latestAt(next),d)||joined(at.apply(start),at.apply(next),d))queue.add(next);
        }
        while(!queue.isEmpty()) {
            var position=queue.removeFirst();if(!seen.add(position))continue;var record=at.apply(position);if(record==null)continue;long id=record.device().id();touched.add(id);
            // A fixed boundary the event does not edit is never walked through: what lies beyond it is another island.
            if(!starts.contains(position)&&(record.device().fixed()||standsForItsComponent(id)||topology.pending(id)))continue;
            for(var d:PhysicalFluidTopology.Direction.values()) {
                var neighbor=at.apply(position.offset(d));if(neighbor!=null&&record.device().joins(neighbor.device(),d))queue.add(neighbor.device().position());
            }
        }
        var edited=new HashSet<Long>();for(var edit:edits)edited.add(edit.id());
        var prepared=topology.queue(edits,touched,nextId,recovery);var affected=new HashSet<Long>();for(long id:touched)affected.addAll(standsFor(id,edited));
        host.coordinator().fence(prepared.event().id(),prepared.event().tick(),affected);topology.commit(prepared);unapplied++;
        return prepared.event();
    }
    private static boolean joined(WorldTopologyLedger.Registration a,WorldTopologyLedger.Registration b,PhysicalFluidTopology.Direction d){return a!=null&&b!=null&&a.device().joins(b.device(),d);}
    /** Applies what is ready, at most sixteen batches (see the class comment); returns whether one queued a solid recovery. */
    public boolean applyPending() {
        unapplied=0;boolean queuedRecovery=false;
        for(int count=0;count<16&&topology.hasPendingEvents();count++){var batch=nextBatch();if(batch==null)break;queuedRecovery|=apply(batch);}
        return queuedRecovery;
    }
    private record Batch(List<WorldTopologyLedger.Event> events,Set<Long> affected,String dimension,long tick) {}
    /**
     * The next batch, or null: in queue order, every event of the first applicable tick and dimension that meets no
     * earlier event left out and whose owners (the islands fenced for it, and the owners of what it touches) are
     * aligned at its tick. A filter recovery is applied alone. No island snapshot is built here.
     */
    private Batch nextBatch() {
        var coordinator=host.coordinator();var taken=new ArrayList<WorldTopologyLedger.Event>();var affected=new HashSet<Long>();String dimension=null;long tick=-1;
        var blockedIds=new HashSet<Long>();var blockedPositions=new HashSet<PhysicalFluidTopology.Position>();
        for(var footprint:topology.footprints()) {
            var event=footprint.event();if(tick>=0&&event.tick()>tick)break;
            String eventDimension=footprint.positions().iterator().next().dimension();
            boolean take=!footprint.meets(blockedIds,blockedPositions)&&(dimension==null||dimension.equals(eventDimension))&&(event.recovery()==null||taken.isEmpty());
            Set<Long> owners=take?owners(event):Set.of();
            if(take)take=coordinator.aligned(event.id(),owners);
            if(!take){blockedIds.addAll(footprint.ids());blockedPositions.addAll(footprint.positions());continue;}
            taken.add(event);affected.addAll(owners);dimension=eventDimension;tick=event.tick();
            if(event.recovery()!=null)break;
        }
        return taken.isEmpty()?null:new Batch(List.copyOf(taken),Set.copyOf(affected),dimension,tick);
    }
    /** An event's owners: islands already fenced for it (the coordinator's fence index) and the current owners of the
     * identities it touches; a missing fence is installed. */
    private Set<Long> owners(WorldTopologyLedger.Event event) {
        var coordinator=host.coordinator();var affected=new HashSet<Long>(coordinator.fencedIslands(event.id()));
        var edited=edited(event);for(long id:event.touched())affected.addAll(standsFor(id,edited));
        for(long id:affected)if(!coordinator.hasFence(id,event.id()))coordinator.fence(event.id(),event.tick(),List.of(id));
        return affected;
    }
    private static WorldTopologyLedger.Registration current(Map<Long,Optional<WorldTopologyLedger.Registration>> changed,Map<Long,WorldTopologyLedger.Registration> active,long id) {
        var change=changed.get(id);return change!=null?change.orElse(null):active.get(id);
    }
    /** Applies one batch; returns whether it queued a solid recovery. */
    private boolean apply(Batch batch) {
        var coordinator=host.coordinator();var events=batch.events();var affected=batch.affected();var active=topology.active();
        // What the affected islands hold - boundary stock and filter cakes - read once each, already aligned.
        var stock=new HashMap<Long,PassiveNetwork.Reservoir>();var cakes=new HashMap<Long,InlineFilter>();
        // A junction's buffer is not inventory (owner decision D46): the junctions of an island the batch replaces leave
        // with it and the recompile mints new ones, and the ledger books neither. A junction's composition therefore
        // resets at a topology edit.
        for(long island:affected) {
            var graph=coordinator.snapshot(island).graph();
            for(var node:graph.reservoirs())if(!node.junction()&&node.id()>0)stock.put(node.id(),node);
            for(var pipe:graph.pipes())if(pipe.filter()!=null)cakes.put(pipe.id()-Long.MIN_VALUE,pipe.filter());
        }
        WorldTopologyLedger.RecoveredSolid recovered=null;InlineFilter released=null;long recoveredFilter=0;
        for(var event:events)if(event.recovery()!=null) {
            var request=event.recovery();var cake=cakes.get(request.filterId());var record=active.get(request.filterId());
            if(record!=null&&cake!=null&&!cake.captured().empty()&&(request.player()==null||host.acceptsRecovery(request.player()))){
                recovered=new WorldTopologyLedger.RecoveredSolid(record.device().position(),request.player(),cake.captured(),cake.energyJoule());released=cake;recoveredFilter=request.filterId();cakes.put(request.filterId(),cake.cleared());
            // The event still applies (its revision is spent); the refusal reaches the player with the reply to
            // the recovery input on its menu's next bucket, never as a message pushed from here.
            }else if(request.player()!=null)host.refused(event.id(),"Filter unchanged: inventory full or no captured solids");
        }
        // The registry after the batch, as changes over the applied one. The ledger books every construction and
        // destruction in order; the islands see only the net change (a tank placed and broken in one batch is neither).
        var changed=new LinkedHashMap<Long,Optional<WorldTopologyLedger.Registration>>();
        var additions=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();var removed=new LinkedHashMap<Long,PassiveNetwork.Reservoir>();
        var constructed=new ArrayList<PassiveNetwork.Reservoir>();var destroyed=new ArrayList<PassiveNetwork.Reservoir>();
        for(var event:events)for(var edit:event.edits()) {
            var old=current(changed,active,edit.id());var replacement=edit.replacement();
            if(replacement==null) {
                changed.put(edit.id(),Optional.empty());var previous=stock.remove(edit.id());
                if(previous!=null&&previous.kind()==PassiveNetwork.NodeKind.RESERVOIR){destroyed.add(previous);if(additions.remove(edit.id())==null)removed.put(edit.id(),previous);}
            } else {
                changed.put(edit.id(),Optional.of(replacement));
                if(replacement.device().boundary()&&(old==null||!old.spec().equals(replacement.spec()))) {
                    var initialized=replacement.spec().initialize(replacement.device(),model,()->{});stock.put(edit.id(),initialized);
                    if(initialized.kind()==PassiveNetwork.NodeKind.RESERVOIR){constructed.add(initialized);additions.put(edit.id(),initialized);}
                }
            }
        }
        var changedAt=new HashMap<PhysicalFluidTopology.Position,WorldTopologyLedger.Registration>();for(var change:changed.values())change.ifPresent(r->changedAt.put(r.device().position(),r));
        java.util.function.Function<PhysicalFluidTopology.Position,WorldTopologyLedger.Registration> at=p->{
            var mine=changedAt.get(p);if(mine!=null)return mine;var record=topology.activeAt(p);return record==null||changed.containsKey(record.device().id())?null:record;
        };
        // The connected components, after the batch, of every device the events touched and every device of the islands
        // they replace: the only part of the registry the batch can change. A fixed boundary the batch does not edit ends
        // the walk (it is a leaf of what is compiled): beyond it lie other islands, which the batch does not change.
        var editedIds=new HashSet<Long>();for(var event:events)editedIds.addAll(edited(event));
        var selected=new HashSet<Long>();for(var event:events)selected.addAll(event.touched());for(long island:affected)selected.addAll(members(island));
        var component=new LinkedHashMap<Long,WorldTopologyLedger.Registration>();var queue=new ArrayDeque<WorldTopologyLedger.Registration>();
        for(long id:selected){var record=current(changed,active,id);if(record!=null&&component.putIfAbsent(id,record)==null)queue.add(record);}
        while(!queue.isEmpty()) {
            var record=queue.removeFirst();if(record.device().fixed()&&!editedIds.contains(record.device().id()))continue;
            for(var d:PhysicalFluidTopology.Direction.values()) {
                if(!record.device().connects(d))continue;var neighbor=at.apply(record.device().position().offset(d));
                if(neighbor!=null&&record.device().joins(neighbor.device(),d)&&component.putIfAbsent(neighbor.device().id(),neighbor)==null)queue.add(neighbor);
            }
        }
        var componentStock=new HashMap<Long,PassiveNetwork.Reservoir>();var componentCakes=new HashMap<Long,InlineFilter>();
        for(var record:component.values()) {
            long id=record.device().id();
            if(record.device().boundary()) {
                // A fixed boundary's state is the same in every island that holds it and nothing changes it: one met at the
                // edge of the walk, whose islands the batch does not replace, is read from the registry's copy.
                var state=stock.get(id);if(state==null&&record.device().fixed())state=fixedStates.get(id);
                if(state==null)throw new IllegalStateException("Topology event reached boundary "+id+" outside the islands it fenced");componentStock.put(id,state);
            }
            if(record.device().kind()==TopologyCompiler.Kind.FILTER) {
                var cake=cakes.get(id);
                // A filter on a dead-end branch (owner decision D18) sits in a dormant island of its own, which the walk may
                // reach beyond the islands the batch replaces without the event touching it: its cake is read from there, and
                // it must come out of this compile dormant again (checked below), so that island stays as it is.
                if(cake==null&&active.containsKey(id)&&dormantFilter(id)&&!affected.contains(owners.get(id)))cake=dormantCake(id);
                if(cake==null){if(active.containsKey(id))throw new IllegalStateException("Topology event reached filter "+id+" outside the islands it fenced");cake=emptyFilter;}
                componentCakes.put(id,cake);
            }
        }
        var compiled=PhysicalFluidTopology.compile(component.values().stream().map(WorldTopologyLedger.Registration::device).toList(),componentStock,componentCakes,
                com.wormzjl.createcheme.science.fluid.network.NetworkReduction.ambient(model));
        FluidRuntimeDiagnostics.count(FluidRuntimeDiagnostics.topologyBatches);FluidRuntimeDiagnostics.count(FluidRuntimeDiagnostics.topologyEvents,events.size());
        FluidRuntimeDiagnostics.count(FluidRuntimeDiagnostics.compiledDevices,component.size());
        var replacements=new ArrayList<IslandCoordinator.Replacement>();var replacementMembers=new LinkedHashMap<Long,List<Long>>();long nextId=topology.nextIdentity();
        for(var island:compiled.islands())if(island.physicalIds().stream().anyMatch(selected::contains)) {
            // A fixed boundary the compile left alone (no connection in the part it compiled) keeps any island of it the
            // batch does not replace, and gets no island of its own; one with no such island is an island of one, as ever.
            if(island.graph().pipes().isEmpty()&&island.physicalIds().size()==1) {
                long only=island.physicalIds().iterator().next();
                if(fixedDevice(only)&&islandsOf(only).stream().anyMatch(other->!affected.contains(other)))continue;
            }
            for(long device:island.physicalIds()){Long holder=owners.get(device);if(holder!=null&&!affected.contains(holder)&&!fixedDevice(device))
                throw new IllegalStateException("Topology event reached device "+device+" outside the islands it fenced");}
            // The recompile's junctions are minted here (start state, decision D37, and buffer size, D25); nothing is booked (D46).
            long id=nextId++;replacements.add(new IslandCoordinator.Replacement(id,PassiveNetwork.mintJunctions(island.graph(),model)));replacementMembers.put(id,island.physicalIds().stream().sorted().toList());
        }
        var eventIds=new LinkedHashSet<UUID>();for(var event:events)eventIds.add(event.id());
        var preparation=topology.applyBatch(List.copyOf(eventIds),constructed,destroyed,weights,nextId);
        var prepared=recovered==null?preparation:topology.withRecovery(preparation,recovered);
        var removedDevices=new HashSet<Long>();var removedRecords=new ArrayList<WorldTopologyLedger.Registration>();var addedRecords=new ArrayList<WorldTopologyLedger.Registration>();
        for(var entry:changed.entrySet()){var before=active.get(entry.getKey());if(entry.getValue().isEmpty()){if(before!=null){removedDevices.add(entry.getKey());removedRecords.add(before);}}else if(before==null)addedRecords.add(entry.getValue().orElseThrow());}
        host.topology(batch.dimension(),eventIds,affected,replacements,batch.tick(),topology.onlineTick(),additions,removed.keySet(),recovered==null?Map.of():Map.of(PhysicalFluidTopology.filterIdentity(recoveredFilter),released),()->{
            topology.commit(prepared);
            for(long id:component.keySet()){diagnostics.remove(id);pipeViews.remove(id);tankFaces.remove(id);}
            for(long id:removedDevices){diagnostics.remove(id);pipeViews.remove(id);tankFaces.remove(id);owners.remove(id);fixedOwners.remove(id);fixedStates.remove(id);}
            diagnostics.putAll(compiled.diagnostics());pipeViews.putAll(compiled.pipeViews());tankFaces.putAll(compiled.tankFaces());
            for(var entry:componentStock.entrySet())if(fixedDevice(entry.getKey()))fixedStates.put(entry.getKey(),entry.getValue());
            for(long island:affected){var devices=members.remove(island);if(devices!=null)for(long device:devices)unbind(device,island);}
            replacementMembers.forEach((island,devices)->{members.put(island,devices);for(long device:devices)bind(device,island);});
            for(var record:removedRecords)chunkRemove(record);for(var record:addedRecords)chunkAdd(record);
            var recompiled=new HashSet<>(component.keySet());recompiled.removeAll(removedDevices);
            host.committed(Set.copyOf(removedDevices),Set.copyOf(recompiled));
        });
        // A queued event that reaches a replacement island is fenced on it now: it will change that island. It reaches it
        // through a device the island owns, or through devices no island owns: submit's walk stopped at a device a queued
        // event placed, and this batch may have left that device owned by no island (a dead-end pipe), so the island
        // beyond it was never fenced for the later event, whose own component walk will reach it.
        if(!replacements.isEmpty()) {
            var fresh=new HashSet<Long>();for(var replacement:replacements)fresh.add(replacement.id());
            for(var future:topology.events())for(long owner:reached(future,component.keySet()))
                if(fresh.contains(owner)&&!coordinator.hasFence(owner,future.id()))coordinator.fence(future.id(),future.tick(),List.of(owner));
        }
        return recovered!=null;
    }
    /**
     * The owners of the applied devices reached from an event's touched devices (those of them in {@code within}, a union
     * of connected components of the applied registry): an owned device stands for its island, and the walk goes on through
     * devices no island owns, following connections as {@link #apply} compiles them. A fixed boundary stands for its
     * islands only when the event edits it, and is never walked through.
     */
    private Set<Long> reached(WorldTopologyLedger.Event event,Set<Long> within) {
        var found=new HashSet<Long>();var seen=new HashSet<Long>();var queue=new ArrayDeque<WorldTopologyLedger.Registration>();var edited=edited(event);
        for(long id:event.touched())if(within.contains(id)&&seen.add(id)){var record=topology.active().get(id);if(record!=null)queue.add(record);}
        while(!queue.isEmpty()) {
            var record=queue.removeFirst();long id=record.device().id();
            found.addAll(standsFor(id,edited));if(record.device().fixed()||standsForItsComponent(id))continue;
            for(var d:PhysicalFluidTopology.Direction.values()) {
                if(!record.device().connects(d))continue;var neighbor=topology.activeAt(record.device().position().offset(d));
                if(neighbor!=null&&record.device().joins(neighbor.device(),d)&&seen.add(neighbor.device().id()))queue.add(neighbor);
            }
        }
        return found;
    }
    /**
     * Whether a device's island stands for everything connected to it, so a walk may stop there: it is owned, and not a
     * filter in a dormant island of its own (the compiler gives each filter of a component with no boundary, and each filter
     * on a dead-end branch, its own island, so a chain of such filters is one component and several islands).
     */
    private boolean standsForItsComponent(long id) {
        return owners.containsKey(id)&&!dormantFilter(id);
    }
    /** Whether an applied filter sits in a dormant island of its own (no destination on its branch, or no boundary at all). */
    private boolean dormantFilter(long filter) {
        Long owner=owners.get(filter);var record=topology.active().get(filter);
        return owner!=null&&record!=null&&record.device().kind()==TopologyCompiler.Kind.FILTER&&members.getOrDefault(owner,List.of()).size()==1;
    }
    /** A dormant filter's cake, as its island holds it (read without materialising: nothing moves it there). */
    private InlineFilter dormantCake(long filter) {
        long identity=PhysicalFluidTopology.filterIdentity(filter);
        for(var pipe:host.coordinator().observe(owners.get(filter)).graph().pipes())if(pipe.id()==identity)return pipe.filter();
        return null;
    }
    /** Whether a registered device (applied, else queued) is a fixed boundary: a generator or a void. */
    private boolean fixedDevice(long id) {
        var record=topology.active().get(id);if(record==null)record=topology.latest().get(id);
        return record!=null&&record.device().fixed();
    }
    /** Every island a device belongs to: all of a fixed boundary's, the one of any other owned device, none otherwise. */
    private Set<Long> islandsOf(long device) {
        var shared=fixedOwners.get(device);if(shared!=null)return shared;
        Long owner=owners.get(device);return owner==null?Set.of():Set.of(owner);
    }
    /**
     * The islands an event's touched device stands for: every island of a device the event edits, the island of any
     * other owned device, and none for a fixed boundary it only passes (islands that share a generator or a void are
     * independent of each other, owner decision D17). A lone boundary is the exception: an edit that touches it joins it,
     * and the batch that puts it into the joined island ends its island of one, so that island is fenced and replaced
     * with the rest (D32).
     */
    private Set<Long> standsFor(long device,Set<Long> edited) {
        if(edited.contains(device))return islandsOf(device);
        if(!fixedDevice(device))return islandsOf(device);
        Long lone=loneIsland(device);return lone==null?Set.of():Set.of(lone);
    }
    /** The island of one of a fixed boundary no connection reaches (its only member), or null. */
    private Long loneIsland(long device) {
        for(long island:islandsOf(device))if(members.getOrDefault(island,List.of()).equals(List.of(device)))return island;
        return null;
    }
    private static Set<Long> edited(WorldTopologyLedger.Event event){var ids=new HashSet<Long>();for(var edit:event.edits())ids.add(edit.id());return ids;}
    private void bind(long device,long island) {
        if(fixedDevice(device)){var set=fixedOwners.computeIfAbsent(device,ignored->new TreeSet<>());set.add(island);owners.put(device,set.first());}
        else owners.put(device,island);
    }
    private void unbind(long device,long island) {
        var set=fixedOwners.get(device);
        if(set!=null){set.remove(island);if(set.isEmpty()){fixedOwners.remove(device);owners.remove(device);}else owners.put(device,set.first());}
        else owners.remove(device,island);
    }
    private void chunkAdd(WorldTopologyLedger.Registration record){var p=record.device().position();chunkMembers.computeIfAbsent(new ChunkKey(p.dimension(),chunkOf(p)),ignored->new LinkedHashSet<>()).add(record.device().id());}
    private void chunkRemove(WorldTopologyLedger.Registration record){var p=record.device().position();var key=new ChunkKey(p.dimension(),chunkOf(p));var ids=chunkMembers.get(key);if(ids!=null&&ids.remove(record.device().id())&&ids.isEmpty())chunkMembers.remove(key);}
    /** The packed chunk position of a block position (Minecraft's {@code ChunkPos.asLong}). */
    static long chunkOf(PhysicalFluidTopology.Position p){return (long)(p.x()>>4)&4294967295L|((long)(p.z()>>4)&4294967295L)<<32;}
}
