package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.runtime.BoundedCpuSolveService;
import com.wormzjl.createcheme.runtime.ProcessSolveServices;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Hold F8 of the phase-ports batch (2026-09-27): in the GUI-layout check's first dev-client run a line was placed block by
 * block at z = 40 (tools/phase-ports-probes/gui-layout/line.txt, client log logs/03): a water generator, a junction pipe
 * with a branch pipe to a void, a pump facing east, a pipe, an inline filter facing east, a pipe, a pressure control valve
 * facing east, a pipe and a void. 60 ms after the filter was placed the island logged {@code fluid_island=46
 * committed_tick=11283 status=HELD: Singular Newton Jacobian: Sparse LU rejected a singular matrix; active-set pass=0}, and
 * it stayed held at that tick for the rest of the run (9 lines; the later placements queued behind it). The pump
 * discharged at its target flow into the filter's two junctions, which nothing drained: a sealed pocket with a prescribed
 * inflow has no root (PassiveStepSolver.relieveSealedPockets). The same start held every pump discharging into a pressure
 * control valve, which starts closed.
 *
 * <p>Replayed on the world's event path: the real {@link PhysicalRegistry} and {@link IslandCoordinator} (a synchronous
 * worker), every block placed as {@code FluidWorldAuthority.place} places it (its default control and spec) at the online
 * tick the client log gives it (the filter at 11283, the others from the log's wall-clock stamps at 20 TPS), and the world
 * run tick by tick as {@code FluidWorldAuthority.tick} runs it. On the tree the hold was found on the run reproduces the
 * client log's nine held lines at committed tick 11283. Every island must commit to the end without a hold.
 */
class InlineFilterLineHoldTest {
    static final String DIM="minecraft:overworld";
    static final int Y=-59,Z=40;
    final FluidThermodynamics model=FluidTestSupport.networkModel();
    final MaterialCatalog catalog=MaterialCatalog.bundled();
    static final BoundedCpuSolveService.CancellationToken NEVER=new BoundedCpuSolveService.CancellationToken() {
        public long deadlineNanos(){return Long.MAX_VALUE;}public boolean isDeadlineExceeded(){return false;}
        public boolean isCancellationRequested(){return false;}public void throwIfCancellationRequested(){}
    };
    record Held(long tick,long island,long committed,String status) {}

    /** A world: its ledger, a coordinator on the ledger's online epoch with one synchronous worker, and the registry. */
    final class World {
        final WorldTopologyLedger ledger;
        final Map<Long,IslandCoordinator.Attempt> attempts=new LinkedHashMap<>();
        final Map<Long,ProcessSolveServices.FluidIslandCommand> commands=new HashMap<>();
        long requests;
        /** Every published HELD status, with the online tick it was published at. */
        final List<Held> held=new ArrayList<>();
        final IslandCoordinator coordinator;
        final PhysicalRegistry registry;
        World(){this(WorldTopologyLedger.Snapshot.empty(catalog),List.of());}
        /** A world loaded from a save: its topology and its islands, registered as FluidWorldAuthority's constructor does. */
        World(WorldTopologyLedger.Snapshot topology,List<IslandCoordinator.Snapshot> islands) {
            ledger=new WorldTopologyLedger(topology);
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
            for(var island:islands)coordinator.register(island,model);
            registry.load();
        }
        /** FluidWorldAuthority.place: the block's default control, a 1 m block of 0.05 m bore, water for a generator,
         * nitrogen for everything else, a 1 m3 tank at 298.15 K and 1 atm. */
        long place(int x,int y,int z,Kind kind,PhysicalFluidTopology.Direction facing) {
            var spec=kind==Kind.GENERATOR?FluidDeviceSpec.water(catalog):FluidDeviceSpec.nitrogen(catalog);
            if(kind==Kind.RESERVOIR)spec=new FluidDeviceSpec(1,298.15,101325,spec.composition());
            return place(x,y,z,kind,facing,spec,.05);
        }
        /** The same with a player's settings made at placement: the device's spec and the block's bore. */
        long place(int x,int y,int z,Kind kind,PhysicalFluidTopology.Direction facing,FluidDeviceSpec spec,double bore) {
            long id=ledger.nextIdentity();
            var control=switch(kind){case PUMP->new FlowControl.Pump(.01,500000,1);case COMPRESSOR->new FlowControl.Compressor(.05,3.0,1);case VALVE->new FlowControl.PressureValve(200000);default->new FlowControl.Passive();};
            var device=new PhysicalFluidTopology.Device(id,new PhysicalFluidTopology.Position(DIM,x,y,z),kind,facing,new PipeResistance.Geometry(1,bore,PipeResistance.DEFAULT_ROUGHNESS_METRES,0),control);
            registry.submit(List.of(new WorldTopologyLedger.Edit(id,new WorldTopologyLedger.Registration(device,spec,0))),id+1,null);
            return id;
        }
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
        String describe(long island) {
            var s=coordinator.observe(island);var b=new StringBuilder("island "+island+" committed="+s.clock().committedTick()+" status="+s.status()+"\n");
            var g=s.graph();
            for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);var st=n.state();
                b.append("  node ").append(i).append(" id=").append(n.id()).append(' ').append(n.kind()).append(" z=").append(n.elevation()).append(" P=").append(st.pressure()).append(" T=").append(st.temperature()).append(" m=").append(st.mass()).append('\n');}
            for(int i=0;i<g.pipes().size();i++){var p=g.pipes().get(i);
                b.append("  pipe ").append(i).append(' ').append(p.first()).append("->").append(p.second()).append(' ').append(p.control()).append(" blocked=").append(p.blockedDirections()).append(" filter=").append(p.filter()!=null).append('\n');}
            return b.toString();
        }
        /** No hold, every placement applied, and every island (a certified one materialised) within one cadence of the
         * online tick (a flowing island's next slice falls due at its committed tick plus the cadence). */
        void assertKeptUp(String label) {
            for(var h:held)System.out.println("F8_HELD "+label+" "+h);
            assertTrue(held.isEmpty(),label+": holds "+held);
            assertTrue(ledger.events().isEmpty(),label+": every placement applied");
            for(var s:coordinator.snapshots())assertTrue(ledger.onlineTick()-s.clock().committedTick()<s.clock().cadenceTicks(),label+": island "+s.id()+" keeps up: committed "+s.clock().committedTick()+" at "+ledger.onlineTick()+", "+s.status());
        }
    }

    /** The client log's line in its order and at its ticks (the filter at 11283, the committed tick of every held line):
     * tick, x, kind, facing, z offset. */
    static final Object[][] LINE={
            {11058,0,Kind.GENERATOR,PhysicalFluidTopology.Direction.EAST,0},
            {11093,1,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,0},
            {11127,1,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,1},
            {11162,1,Kind.VOID,PhysicalFluidTopology.Direction.NORTH,2},
            {11204,2,Kind.PUMP,PhysicalFluidTopology.Direction.EAST,0},
            {11239,3,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,0},
            {11283,4,Kind.FILTER,PhysicalFluidTopology.Direction.EAST,0},
            {11317,5,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,0},
            {11368,6,Kind.VALVE,PhysicalFluidTopology.Direction.EAST,0},
            {11402,7,Kind.PIPE,PhysicalFluidTopology.Direction.NORTH,0},
            {11436,8,Kind.VOID,PhysicalFluidTopology.Direction.NORTH,0}};

    /** Places the first {@code blocks} of the line at their ticks and runs {@code after} ticks past the last one. */
    void run(World w,int blocks,long after) {
        long end=0;
        for(int i=0;i<blocks;i++) {
            var b=LINE[i];long tick=((Integer)b[0]).longValue();w.runTo(tick);
            w.place((Integer)b[1],Y,Z+(Integer)b[4],(Kind)b[2],(PhysicalFluidTopology.Direction)b[3]);end=tick;
        }
        w.runTo(end+after);
    }

    /** The whole line, in the client's order: no hold, and the pump runs at its target through the regulating valve. */
    @Test void theInGameLineNeverHolds() {
        var w=new World();run(w,LINE.length,2400);w.assertKeptUp("in-game line");
        var island=w.coordinator.snapshots().stream().filter(s->s.graph().pipes().size()>=6).findFirst().orElseThrow();
        var modes=island.lastResult().orElseThrow().endpointModes();
        System.out.println("F8_END "+w.describe(island.id())+" modes="+modes);
        assertTrue(modes.contains(FlowControl.Mode.PUMP_TARGET)&&modes.contains(FlowControl.Mode.VALVE_REGULATING),"the pump delivers its target through the valve: "+modes);
    }
    /** Every intermediate dead end the line passes through (the pump into one pipe, into the filter alone, into the
     * filter and a pipe, into the valve with nothing past it), each run for two minutes on its own. */
    @Test void everyPrefixOfTheLineNeverHolds() {
        for(int blocks=1;blocks<=LINE.length;blocks++){var w=new World();run(w,blocks,2400);w.assertKeptUp(blocks+" blocks");}
    }
    /**
     * Straight lines along +x, every block placed in one tick with the world's defaults and run 3000 ticks. Letters: G
     * generator, P pipe, M pump (east), F filter (east), V valve (east), O void, T tank. On the tree the hold was found on,
     * every line with a pump discharging into a valve or into a filter with nothing past it held at its first slice
     * (GPMPVPO, GPMPF, GPMPV, GPMPVP, GPMPFPVPO, GPMPFP, GPMPVPT, GPMPFPVPT).
     */
    @Test void pumpLinesIntoValvesAndFiltersNeverHold() {
        var ends=new LinkedHashMap<String,List<FlowControl.Mode>>();var pumpOwners=new LinkedHashMap<String,Long>();
        for(String layout:List.of("GPMPVPO","GPMPFPO","GPMPF","GPMPV","GPMPVP","GPMPFPVPO","GPMPFP","GPMP","GPMPPO","GPMPFPT","GPMPVPT","GPMPFPVPT")) {
            var w=new World();w.runTo(100);long pump=0;
            for(int i=0;i<layout.length();i++) {
                var kind=switch(layout.charAt(i)){case 'G'->Kind.GENERATOR;case 'P'->Kind.PIPE;case 'M'->Kind.PUMP;case 'F'->Kind.FILTER;case 'V'->Kind.VALVE;case 'O'->Kind.VOID;case 'T'->Kind.RESERVOIR;default->throw new IllegalArgumentException(layout);};
                long id=w.place(i,0,0,kind,kind==Kind.PUMP||kind==Kind.FILTER||kind==Kind.VALVE?PhysicalFluidTopology.Direction.EAST:PhysicalFluidTopology.Direction.NORTH);
                if(kind==Kind.PUMP)pump=id;
            }
            w.runTo(3100);w.assertKeptUp(layout);
            var island=w.coordinator.snapshots().stream().max(Comparator.comparingInt(s->s.graph().pipes().size())).orElseThrow();
            ends.put(layout,island.lastResult().map(PassiveIntervalSolver.Result::endpointModes).orElse(List.of()));pumpOwners.put(layout,w.registry.owner(pump));
        }
        ends.forEach((layout,modes)->System.out.println("F8_LAYOUT "+layout+" "+modes));
        // A pump that can deliver runs at its target: through the valve regulating at its set point, or through the filter.
        for(String layout:List.of("GPMPVPO","GPMPFPVPO"))assertTrue(ends.get(layout).containsAll(List.of(FlowControl.Mode.PUMP_TARGET,FlowControl.Mode.VALVE_REGULATING)),layout+": "+ends.get(layout));
        assertTrue(ends.get("GPMPFPO").contains(FlowControl.Mode.PUMP_TARGET),"GPMPFPO: "+ends.get("GPMPFPO"));
        // A pump discharging into a dead end carries nothing: owner decision D18 (2026-09-28) leaves the whole dead-end branch,
        // the pump included, out of the solve (it stood at its shutoff, closed, while it was solved; restated for the owner
        // to confirm, PHASE_PORTS_REVIEW.md "Boundary split, dead ends and rest").
        for(String layout:List.of("GPMPF","GPMPFP","GPMPV","GPMPVP","GPMP"))assertNull(pumpOwners.get(layout),layout+": the pump is in no island");
    }
    /**
     * The same pocket entered from the other side (hold search seed 61, tools/phase-ports-probes/hold-fuzz): a nitrogen
     * generator at 406 kPa, placed first, so every junction is minted with its gas; a pump refused its gas supply (closed)
     * discharging into an inline filter; and a water generator at 215 kPa feeding the filter's outlet through a 0.01 m pipe.
     * The filter's two junctions form a pocket whose only way in or out is that pipe, and at the minted 406 kPa its row
     * started on the velocity cap's branch, which has no pressure column: the rate pass at every interval's start was
     * singular. The pocket forces that pipe's flow to zero, so the pipe is a dead end for the cap, like a single junction's.
     */
    @Test void aFilterPocketFedThroughASmallPipeNeverHolds() {
        var w=new World();w.runTo(100);var east=PhysicalFluidTopology.Direction.EAST;var north=PhysicalFluidTopology.Direction.NORTH;
        double[] n2=new double[FluidDeviceSpec.nitrogen(catalog).composition().length];n2[com.wormzjl.createcheme.science.material.MaterialTestBasis.NITROGEN]=1;
        double[] water=FluidDeviceSpec.water(catalog).composition();
        w.place(0,0,0,Kind.GENERATOR,north,new FluidDeviceSpec(1,272.66,405737.6,n2),.05);
        w.place(1,0,0,Kind.PIPE,north,FluidDeviceSpec.nitrogen(catalog),.05);
        w.place(2,0,0,Kind.PUMP,east,FluidDeviceSpec.nitrogen(catalog),.05);
        w.place(3,0,0,Kind.PIPE,north,FluidDeviceSpec.nitrogen(catalog),.01);
        w.place(4,0,0,Kind.FILTER,east,FluidDeviceSpec.nitrogen(catalog),.01);
        w.place(5,0,0,Kind.PIPE,north,FluidDeviceSpec.nitrogen(catalog),.01);
        w.place(6,0,0,Kind.GENERATOR,north,new FluidDeviceSpec(1,282.45,215395.3,water),.05);
        w.runTo(3100);w.assertKeptUp("filter pocket fed through a small pipe");
    }
}
