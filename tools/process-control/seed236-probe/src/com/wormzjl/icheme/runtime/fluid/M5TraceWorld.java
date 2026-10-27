package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.runtime.BoundedCpuSolveService;
import com.wormzjl.icheme.runtime.ProcessSolveServices;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import java.util.function.*;

/**
 * M5 trace (tools/unified-thermo-port-m5/trace, never tracked): HoldFuzzTest.World as the bundled harness builds it, with one
 * {@link Solve} record per island solve. A solve's {@code checkpoints} is the number of cancellation checks the solve made
 * (every Newton iteration, line-search trial, Jacobian stage, active-set pass, substep attempt and flash checks one): a
 * deterministic work count, unlike its wall time, so a budget hold (class F) can be minimized and traced on it.
 * The coordinator's budgets are HoldFuzzTest's (holdFuzz.wallNanos, holdFuzz.softNanos).
 */
final class M5TraceWorld {
    record Solve(long island,long start,long end,long checkpoints,long nanos,String detail,int accepted,int rejected,
                 Map<String,Integer> reasons,PassiveNetwork snapshot,ProcessSolveServices.FluidIslandSolveResult result) {}
    /**
     * M7 (2026-10-17): runs {@code body} as HoldFuzzTest.runSeed runs a seed: inside the pilot catalog's MaterialRuntime
     * context when the attached harness is a cold one with holdFuzz.cold set (its COLD and PACKAGE fields, read by
     * reflection so this compiles against the bundled harness too), directly otherwise.
     */
    static <T> T within(Supplier<T> body) {
        try {
            var cold=HoldFuzzTest.class.getDeclaredField("COLD");cold.setAccessible(true);
            if(cold.getBoolean(null)){var pkg=HoldFuzzTest.class.getDeclaredField("PACKAGE");pkg.setAccessible(true);
                return com.wormzjl.icheme.science.material.MaterialRuntime.with(HoldFuzzTest.CATALOG,(String)pkg.get(null),body);}
        }catch(NoSuchFieldException bundled){/* the bundled harness: no cold option */}
        catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        return body.get();
    }
    static void within(Runnable body){within(()->{body.run();return null;});}
    /** Counts the solve's cancellation checks; never cancels. */
    static final class CountingToken implements BoundedCpuSolveService.CancellationToken {
        long calls;
        public long deadlineNanos(){return Long.MAX_VALUE;}
        public boolean isDeadlineExceeded(){return false;}
        public boolean isCancellationRequested(){return false;}
        public void throwIfCancellationRequested(){calls++;}
    }
    final FluidThermodynamics model;
    final WorldTopologyLedger ledger=new WorldTopologyLedger(WorldTopologyLedger.Snapshot.empty(HoldFuzzTest.CATALOG));
    final Map<Long,IslandCoordinator.Attempt> attempts=new LinkedHashMap<>();
    final Map<Long,ProcessSolveServices.FluidIslandCommand> commands=new HashMap<>();
    long requests;
    final List<HoldFuzzTest.Held> held=new ArrayList<>();
    final IslandCoordinator coordinator;
    final PhysicalRegistry registry;
    final List<Solve> solves=new ArrayList<>();
    final boolean keepSnapshots;
    /** Called on the ticking thread around each solve. */
    BiConsumer<IslandCoordinator.Attempt,ProcessSolveServices.FluidIslandCommand> before=(a,c)->{};
    Consumer<Solve> after=s->{};
    M5TraceWorld(FluidThermodynamics model,boolean keepSnapshots) {
        this.model=model;this.keepSnapshots=keepSnapshots;
        coordinator=new IslandCoordinator(new IslandCoordinator.Dispatcher() {
            public int availableWorkers(){return 1-attempts.size();}
            public long nextRequestId(){return ++requests;}
            public boolean submit(IslandCoordinator.Attempt attempt,ProcessSolveServices.FluidIslandCommand command){attempts.put(attempt.slice().requestId(),attempt);commands.put(attempt.slice().requestId(),command);return true;}
            public void cancel(long request){}
        },changed->{for(var s:changed)if(s.status().startsWith("HELD"))held.add(new HoldFuzzTest.Held(ledger.onlineTick(),s.id(),s.clock().committedTick(),s.status()));},
                ()->0L,new IslandCoordinator.Settings(Long.getLong("holdFuzz.wallNanos",2_000_000_000L),Long.getLong("holdFuzz.softNanos",1_500_000_000L),64,false,100,CertificatePolicy.defaults()),ledger::onlineTick);
        registry=new PhysicalRegistry(ledger,model,InlineFilter.empty(),new PhysicalRegistry.Host() {
            public IslandCoordinator coordinator(){return coordinator;}
            public void topology(String dimension,Set<UUID> events,Set<Long> affected,List<IslandCoordinator.Replacement> replacements,long committed,long online,
                                 Map<Long,PassiveNetwork.Reservoir> additions,Set<Long> removals,Map<Long,InlineFilter> releasedFilters,Runnable commit) {
                coordinator.topology(events,affected,replacements,model,committed,online,additions,removals,releasedFilters,commit);
            }
            public boolean acceptsRecovery(UUID player){return true;}
            public void refused(UUID event,String reason){}
            public void committed(Set<Long> removed,Set<Long> recompiled){}
        });
        registry.load();
    }
    /** As HoldFuzzTest.World.place: the block's own control, bore and spec. */
    void place(HoldFuzzTest.Block b) {
        long id=ledger.nextIdentity();
        var device=new PhysicalFluidTopology.Device(id,b.position(),b.kind(),b.facing(),new PipeResistance.Geometry(1,b.bore(),PipeResistance.DEFAULT_ROUGHNESS_METRES,0),b.control());
        var spec=b.spec()!=null?b.spec():FluidDeviceSpec.nitrogen(HoldFuzzTest.CATALOG);
        registry.submit(List.of(new WorldTopologyLedger.Edit(id,new WorldTopologyLedger.Registration(device,spec,0))),id+1,null);
    }
    void tick() {
        ledger.tick();coordinator.tick();
        if(ledger.hasPendingEvents()){registry.applyPending();coordinator.pumpIfUseful();}
        for(long request:List.copyOf(attempts.keySet())) {
            var attempt=attempts.remove(request);var command=commands.remove(request);
            before.accept(attempt,command);
            var token=new CountingToken();long started=System.nanoTime();
            var result=(ProcessSolveServices.FluidIslandSolveResult)command.solve(token);
            long nanos=System.nanoTime()-started;var candidate=result.candidate();
            var solve=new Solve(attempt.islandId(),attempt.slice().startTick(),attempt.slice().endTick(),token.calls,nanos,result.detail(),
                    candidate.map(PassiveIntervalSolver.Result::acceptedSubsteps).orElse(-1),candidate.map(PassiveIntervalSolver.Result::rejectedSubsteps).orElse(-1),
                    candidate.map(PassiveIntervalSolver.Result::rejectionReasons).orElse(Map.of()),keepSnapshots?command.snapshot():null,keepSnapshots?result:null);
            solves.add(solve);after.accept(solve);
            coordinator.completed(attempt,Optional.of(result));
        }
        coordinator.pump();
    }
    void runTo(long tick){while(ledger.onlineTick()<tick)tick();}
    /** Places a list as HoldFuzzTest.run does (a refused spec is skipped) and returns the refusals. */
    List<String> placeAll(List<HoldFuzzTest.Placement> list) {
        var refused=new ArrayList<String>();
        for(var p:list){runTo(p.tick());try{place(p.block());}catch(IllegalArgumentException invalid){refused.add(p.block().label()+": "+invalid.getMessage());}}
        return refused;
    }
    /** A network's nodes and connections, as HoldFuzzTest.World.describe prints an island's. */
    static String describe(PassiveNetwork g,FluidThermodynamics model) {
        var b=new StringBuilder();var names=model.hydrocarbon.components();
        for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);var st=n.state();
            b.append("    node ").append(i).append(" id=").append(n.id()).append(' ').append(n.kind()).append(" z=").append(n.elevation()).append(" P=").append(st.pressure()).append(" T=").append(st.temperature())
                    .append(" m=").append(st.mass()).append(" V=").append(st.volume()).append(" liq=").append(st.liquidVolume()).append(" water=").append(st.waterVolume()).append(" vap=").append(st.vaporVolume())
                    .append(" solids=").append(st.solids().massKg()).append('\n');
            b.append("      L[mol]=").append(amounts(st.liquidView(),names)).append(" V[mol]=").append(amounts(st.vaporView(),names)).append(" waterL=").append(st.waterLiquid()).append(" waterV=").append(st.waterVapor()).append('\n');}
        for(int i=0;i<g.pipes().size();i++){var p=g.pipes().get(i);
            b.append("    pipe ").append(i).append(" id=").append(p.id()).append(' ').append(p.first()).append("->").append(p.second()).append(' ').append(p.control()).append(" area=").append(p.minimumArea()).append(" blocked=").append(p.blockedDirections()).append(" filter=").append(p.filter()!=null).append(" ports=").append(p.firstPort()).append('/').append(p.secondPort()).append('\n');}
        return b.toString();
    }
    static String amounts(double[] a,List<String> names) {
        var b=new StringBuilder("{");for(int i=0;i<a.length;i++)if(a[i]!=0)b.append(b.length()>1?",":"").append(i<names.size()?names.get(i):"c"+i).append('=').append(String.format(Locale.ROOT,"%.4g",a[i]));return b.append('}').toString();
    }
}
