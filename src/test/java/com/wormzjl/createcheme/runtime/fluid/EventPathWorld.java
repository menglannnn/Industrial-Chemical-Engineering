package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.runtime.BoundedCpuSolveService;
import com.wormzjl.createcheme.runtime.ProcessSolveServices;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A world on the event path for the hold-class gates (phase-ports batch, "Hold class ..." sections of
 * PHASE_PORTS_REVIEW.md): the real {@link PhysicalRegistry} and {@link IslandCoordinator} with one synchronous worker, run
 * tick by tick as {@code FluidWorldAuthority.tick} runs, blocks placed through {@link PhysicalRegistry#submit} as
 * {@code FluidWorldAuthority.place} places them, with the control, spec and bore a player set at placement. The same world
 * as the placement-sequence fuzz ({@code tools/phase-ports-probes/hold-fuzz/}), which found the classes.
 */
final class EventPathWorld {
    static final String DIM="minecraft:overworld";
    static final MaterialCatalog CATALOG=MaterialCatalog.bundled();
    static final BoundedCpuSolveService.CancellationToken NEVER=new BoundedCpuSolveService.CancellationToken() {
        public long deadlineNanos(){return Long.MAX_VALUE;}public boolean isDeadlineExceeded(){return false;}
        public boolean isCancellationRequested(){return false;}public void throwIfCancellationRequested(){}
    };
    record Held(long tick,long island,long committed,String status) {}

    final FluidThermodynamics model=FluidTestSupport.networkModel();
    final WorldTopologyLedger ledger=new WorldTopologyLedger(WorldTopologyLedger.Snapshot.empty(CATALOG));
    final Map<Long,IslandCoordinator.Attempt> attempts=new LinkedHashMap<>();
    final Map<Long,ProcessSolveServices.FluidIslandCommand> commands=new HashMap<>();
    long requests;
    /** Every published HELD status, with the online tick it was published at. */
    final List<Held> held=new ArrayList<>();
    final IslandCoordinator coordinator;
    final PhysicalRegistry registry;

    EventPathWorld() {
        coordinator=new IslandCoordinator(new IslandCoordinator.Dispatcher() {
            public int availableWorkers(){return 1-attempts.size();}
            public long nextRequestId(){return ++requests;}
            public boolean submit(IslandCoordinator.Attempt attempt,ProcessSolveServices.FluidIslandCommand command){attempts.put(attempt.slice().requestId(),attempt);commands.put(attempt.slice().requestId(),command);return true;}
            public void cancel(long request){}
        },changed->{for(var s:changed)if(s.status().startsWith("HELD"))held.add(new Held(ledger.onlineTick(),s.id(),s.clock().committedTick(),s.status()));},
                ()->0L,new IslandCoordinator.Settings(2_000_000_000L,1_500_000_000L,64,false,100,CertificatePolicy.defaults()),IslandCoordinator.CommitHook.NO_MATERIAL,ledger::onlineTick);
        registry=new PhysicalRegistry(ledger,model,InlineFilter.empty(),new PhysicalRegistry.Host() {
            public IslandCoordinator coordinator(){return coordinator;}
            public void topology(String dimension,Set<UUID> events,Set<Long> affected,List<IslandCoordinator.Replacement> replacements,long committed,long online,
                                 Map<Long,PassiveNetwork.Reservoir> additions,Set<Long> removals,Map<Long,InlineFilter> releasedFilters,Runnable commit) {
                coordinator.topology(events,affected,replacements,model,committed,online,additions,removals,releasedFilters,commit);
            }
            public boolean acceptsRecovery(UUID player){return true;}
            public void refused(UUID event,String reason){}
            public void committed(Set<Long> removed){}
        });
        registry.load();
    }
    /** The world's default control of a device kind (FluidWorldAuthority.place). */
    static FlowControl defaultControl(Kind kind) {
        return switch(kind){case PUMP->new FlowControl.Pump(.01,500000,1);case COMPRESSOR->new FlowControl.Compressor(.05,3.0,1);case VALVE->new FlowControl.PressureValve(200000);default->new FlowControl.Passive();};
    }
    /** A 1 m3 nitrogen charge at 298.15 K and 1 atm: a tank's or a void's default. */
    static FluidDeviceSpec nitrogen(){return FluidDeviceSpec.nitrogen(CATALOG);}
    /** A generator's spec: one component at a temperature and pressure. */
    static FluidDeviceSpec pure(int component,double temperature,double pressure) {
        double[] n=new double[FluidDeviceSpec.nitrogen(CATALOG).composition().length];n[component]=1;return new FluidDeviceSpec(1,temperature,pressure,n);
    }
    /** Places one block at the current online tick; returns its identity. */
    long place(int x,int y,int z,Kind kind,PhysicalFluidTopology.Direction facing,FlowControl control,FluidDeviceSpec spec,double bore) {
        long id=ledger.nextIdentity();
        var device=new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position(DIM,x,y,z),kind,facing,new PipeResistance.Geometry(1,bore,PipeResistance.DEFAULT_ROUGHNESS_METRES,0),control);
        registry.submit(List.of(new WorldTopologyLedger.Edit(id,new WorldTopologyLedger.Registration(device,spec!=null?spec:nitrogen(),0))),id+1,null);
        return id;
    }
    /** A block with its kind's default control and a nitrogen spec (boundaries: a 1 m3 charge at 298.15 K, 1 atm). */
    long place(int x,int y,int z,Kind kind,PhysicalFluidTopology.Direction facing,double bore){return place(x,y,z,kind,facing,defaultControl(kind),null,bore);}
    /** One online tick as FluidWorldAuthority.tick runs it, with the worker's jobs run to completion in the tick. */
    void tick() {
        ledger.tick();coordinator.tick();
        if(ledger.hasPendingEvents()){registry.applyPending();coordinator.pumpIfUseful();}
        for(long request:List.copyOf(attempts.keySet())) {
            var attempt=attempts.remove(request);var command=commands.remove(request);
            var result=(ProcessSolveServices.FluidIslandSolveResult)command.solve(NEVER);
            coordinator.completed(attempt,Optional.of(result));
        }
        coordinator.pump();
    }
    void runTo(long tick){while(ledger.onlineTick()<tick)tick();}
    /** No hold, every placement applied, and every island within one cadence of the online tick. */
    void assertKeptUp(String label) {
        for(var h:held)System.out.println("EVENT_PATH_HELD "+label+" "+h);
        if(!held.isEmpty())for(var s:coordinator.snapshots()) {
            System.out.println("EVENT_PATH_ISLAND "+s.id()+" "+s.status()+" flows="+s.lastResult().map(r->java.util.Arrays.toString(r.averageMassFlows())).orElse("-"));
            for(var node:s.graph().reservoirs())System.out.println("  node "+node.id()+" "+node.kind()+" z="+node.elevation()+" empty="+node.empty()+" P="+node.state().pressure()
                    +" T="+node.state().temperature()+" m="+node.state().mass()+" liq="+node.state().liquidVolume()+" water="+node.state().waterVolume()+" vap="+node.state().vaporVolume());
            for(var pipe:s.graph().pipes())System.out.println("  pipe "+pipe.first()+"->"+pipe.second()+" "+pipe.control()+" ports="+pipe.firstPort()+"/"+pipe.secondPort());
        }
        assertTrue(held.isEmpty(),label+": holds "+held);
        assertTrue(ledger.events().isEmpty(),label+": every placement applied");
        for(var s:coordinator.snapshots())assertTrue(ledger.onlineTick()-s.clock().committedTick()<s.clock().cadenceTicks(),label+": island "+s.id()+" keeps up: committed "+s.clock().committedTick()+" at "+ledger.onlineTick()+", "+s.status());
    }
}
