package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.SolverOwnership;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;

/**
 * Adaptive internal substeps. The requested simulated interval is independent of the caller's wall deadline.
 *
 * <p>Every step is one backward-Euler step, {@link PassiveStepSolver#solve}: one Newton with its active-set passes, one
 * exact conservative reconstruction, the equation gate and the conservation audit. A converged step is accepted when no
 * vessel's pressure or mass changed by more than {@link #STATE_CAP} of itself. The basis and its measurements are in
 * documentation/2026-09-24-mixed-gas-junction/HANDOFF_REVIEW.md 8.4-8.9 and the batch's BE_INTEGRATOR_PLAN.md: against
 * the second-order TR-BDF2 it replaced, one Newton solve per step instead of two plus an embedded companion, no rate
 * solve per step, and zero nonconvergence on the mixed-gas junction matrices where TR-BDF2 failed on its rate solve;
 * trajectories are first order (a decay lags by up to one 5 s cycle), which the owner accepted (decision D4).
 */
public final class PassiveIntervalSolver {
    /**
     * The largest relative change of a vessel's (a {@code RESERVOIR} node's) pressure, over max(100 Pa, P0), or mass an
     * accepted step may make. Junctions are not in the measure: their pressure is algebraic (the Newton's net-mass row)
     * and their mass is pinned. 0.02 and 0.05 gave the same zero nonconvergence; 0.05 takes fewer steps (review 8.6 (e)).
     */
    static final double STATE_CAP=.05;
    private final PassiveStepSolver implicit;
    private final FluidThermodynamics model;
    /** A converged step the state-change controller refused. */
    private static final class StateChangeRejection extends RuntimeException {
        private StateChangeRejection(String message){super(message);}
    }
    public PassiveIntervalSolver(FluidThermodynamics model){this(model,SolverOwnership.confinedToCurrentThread());}
    /** Retained across island jobs: every nested workspace answers to the supplied latch. */
    public PassiveIntervalSolver(FluidThermodynamics model,SolverOwnership ownership) {
        this.model=Objects.requireNonNull(model);implicit=new PassiveStepSolver(model,Objects.requireNonNull(ownership));
    }
    /** The step bounds and the attempt cap of an interval. There is no tolerance: the backward-Euler controller's step
     * bound is {@link #STATE_CAP} (the relative tolerance of the TR-BDF2 error estimate went with it). */
    public record Settings(double initialStep,double maximumStep,int maximumAttempts) {
        public Settings {
            if(!Double.isFinite(initialStep)||initialStep<=0||!Double.isFinite(maximumStep)||maximumStep<initialStep
                    ||maximumAttempts<1)throw new IllegalArgumentException("Invalid interval settings");
        }
        public static Settings defaults(){return new Settings(1,20,1024);}
    }
    public record Result(PassiveNetwork graph,double advancedSeconds,double[] averageMassFlows,int acceptedSubsteps,int rejectedSubsteps,
                         double pumpWorkJoule,List<ConservativeTransport.BoundaryTransfer> boundaries,Map<String,Integer> rejectionReasons,
                         List<FlowControl.Mode> endpointModes,double[] endpointHeads,double[] endpointFlows,PassiveStepSolver.Acceptance acceptance,List<PipeTransfer> pipeTransfers) {
        public Result {averageMassFlows=averageMassFlows.clone();boundaries=List.copyOf(boundaries);rejectionReasons=Collections.unmodifiableMap(new LinkedHashMap<>(rejectionReasons));endpointModes=List.copyOf(endpointModes);endpointHeads=endpointHeads.clone();endpointFlows=endpointFlows.clone();Objects.requireNonNull(acceptance);pipeTransfers=List.copyOf(pipeTransfers);}
        @Override public double[] averageMassFlows(){return averageMassFlows.clone();}
        @Override public double[] endpointHeads(){return endpointHeads.clone();}
        /** The mass flows (kg/s) of the interval's last accepted step: the flows at its end state, which the next job's first
         * step starts from ({@link #replayStart}). The mass the interval transported is {@link #averageMassFlows}. */
        @Override public double[] endpointFlows(){return endpointFlows.clone();}
    }
    public Result solve(PassiveNetwork initial,double duration,Settings settings,Runnable checkpoint) {
        return solve(initial,duration,settings,checkpoint,settings.initialStep());
    }
    /**
     * Starts the adaptive controller from {@code startingStep} instead of {@code initialStep},
     * bounded by the interval and by {@code maximumStep}. A caller that solves the same island
     * repeatedly may pass {@link #nextStepEstimate()} of the previous interval. The island runtime
     * does not: it starts every ordinary interval at {@code min(initialStep, duration)}, a function
     * of the interval alone, so that a certified replay is bitwise (see {@link #replayStart}).
     */
    public Result solve(PassiveNetwork initial,double duration,Settings settings,Runnable checkpoint,double startingStep) {
        var committed=PassiveNetwork.sizeJunctionHoldups(initial,model);
        // Owner decision D33: a transport closure stands for its own interval only, so the interval starts with every one
        // read open, and every path reads that start: the rest test, the slice's inlet decision, the refresh, the solid
        // events (which decide the closures again at t0) and the plain integrate of an island with no filter, solids or
        // liquid, where nothing decides one again (a gas carries no solid). A rest interval changes nothing: it keeps the
        // committed graph.
        initial=NetworkReduction.reopened(committed);
        // Owner decisions D19, D42 and D58: an island nothing can drive by more than the gauge reads is at rest and is not
        // solved (NetworkReduction step 6); a mover refused for the slice moves nothing.
        boolean[] refused=decideInlets(initial,duration);
        // Owner decision D23 (6): the refresh runs before the rest test, because a supersaturated closed tank is not at rest:
        // an island whose refresh froze or melted a crystal (or merged a stock's sizes) is solved from the refreshed state,
        // its boundaries kept. Every package has a crystal, the free water's ice (WP4), so only the stocks that may convert
        // are refreshed here: an island where none may (no solids, no crystal able to form) reaches its rest test with no
        // closure and no checkpoint, as on a package without crystals before the ice.
        var conversions=InventoryEquilibrium.conversions(initial,model,checkpoint);
        if(Double.isFinite(duration)&&duration>0&&!conversions.converted()&&NetworkReduction.atRest(initial,model,null,refused)){SolverDiagnostics.count(SolverDiagnostics.restIntervals);return committedInlets(NetworkReduction.restInterval(committed,duration,refused));}
        // Owner decision D37 (S3): the interval's one refresh completes the rest test's (InventoryEquilibrium.completed), and
        // the steps or the solid events start from it, its boundaries first. Whether the solid events run is read where the
        // rest test's refresh left the island when a stock converted, and on the island as it came otherwise.
        var monitored=conversions.converted()?conversions.graph():initial;
        var start=InventoryEquilibrium.completed(initial,conversions,model,checkpoint);
        Result result;
        if(com.wormzjl.createcheme.science.fluid.transport.SolidMobility.monitored(monitored)||model.solidSettings.immobileViscosity()<.002&&monitored.reservoirs().stream().anyMatch(n->n.state().waterVolume()>0))result=new SolidEventIntegrator(model,this).solve(start,duration,settings,checkpoint,startingStep);
        else result=integrate(start,duration,settings,checkpoint,PassiveStepSolver.Acceptance.FULL,StageGuard.NONE,startingStep);
        return committedInlets(result);
    }
    public Result solveApproximate(PassiveNetwork initial,double duration,Settings settings,Runnable checkpoint,StageGuard guard) {
        // The interval starts with every transport closure read open (owner decision D33; see solve).
        initial=NetworkReduction.reopened(PassiveNetwork.sizeJunctionHoldups(initial,model));
        if(com.wormzjl.createcheme.science.fluid.transport.SolidMobility.requiresFull(model,initial))throw new ApproximationAnchor.Rejected("Solid/filter intervals require a full solve");
        StageGuard checked=(states,modes)->{
            for(var state:states)if(state.solidMoments().mass()>0||com.wormzjl.createcheme.science.fluid.transport.SolidMobility.immobileLiquid(model,state))throw new ApproximationAnchor.Rejected("Solid transition requires a full solve");
            guard.check(states,modes);
        };
        decideInlets(initial,duration);
        return committedInlets(integrate(InventoryEquilibrium.Refreshed.as(initial),duration,settings,checkpoint,PassiveStepSolver.Acceptance.APPROXIMATE,checked,settings.initialStep()));
    }
    /**
     * The endpoint modes of the island's committed interval and the pipe ids they belong to: the hysteresis input of the
     * inlet phase check (decision D6). Restated by {@link #replayStart} from the committed interval, and advanced to each
     * interval this solver returns (a re-solve of the same start state decides the same, since the rule is idempotent on
     * one state: refused-after = refuse(share, refused-before) is a fixed point of itself).
     */
    private List<FlowControl.Mode> inletModes;
    private List<Long> inletKey=List.of();
    private static List<Long> pipeIds(PassiveNetwork graph){var ids=new ArrayList<Long>(graph.pipes().size());for(var pipe:graph.pipes())ids.add(pipe.id());return ids;}
    /**
     * Decision D6 (plan 3.6, 3.7): at a slice's start, on its committed state {@code initial} and the committed endpoint
     * modes, which pumps and compressors are refused for the whole slice because their supply is the wrong phase
     * ({@link InletPhase#decide} over the slice {@code duration}); handed to the step solver, which holds them in
     * {@link FlowControl.Mode#INLET_WRONG_PHASE} (closed) in every step, rate seed and segment of the slice. The input is
     * the committed endpoint mode, never solver-local history, so a certified island woken with a fresh solver decides
     * exactly as the island that solved every interval. {@code initial} is the interval start, its transport closures
     * read open (owner decision D33, {@link NetworkReduction#reopened}): the solid events decide them again at the
     * interval's t0, with the movers this decision leaves free. Read as they stood, a suction line the last interval
     * closed left its pump "inlet dry", and a refused pump cannot drive the flow that would suspend the crystals again
     * (the owner's fix A of F2, 2026-10-16).
     */
    private boolean[] decideInlets(PassiveNetwork initial,double duration) {
        var modes=inletKey.equals(pipeIds(initial))?inletModes:null;
        boolean[] refused=InletPhase.decide(model,initial,modes,duration);
        var ids=new HashSet<Long>();for(int i=0;i<refused.length;i++)if(refused[i])ids.add(initial.pipes().get(i).id());
        implicit.refuseInlets(ids);
        return refused;
    }
    private Result committedInlets(Result result) {
        inletModes=result.endpointModes();inletKey=pipeIds(result.graph());
        return result;
    }
    /** The step the controller would try next after the last successful interval, before the
     * interval boundary truncated it; 0 until an interval has been integrated. */
    public double nextStepEstimate(){return nextStepEstimate;}
    private double nextStepEstimate;

    /**
     * An interval that a stage guard cut short, as the accepted prefix plus the transition that
     * stopped it. {@code result} is {@code null} when the transition was already there at the
     * interval's own t0 and nothing advanced, and its {@code advancedSeconds} is below the
     * requested duration otherwise, with {@code averageMassFlows} averaged over what it did
     * advance. No public path returns one: a partial interval may not commit, and the only caller
     * is {@link SolidEventIntegrator}, which closes the connection and integrates the remainder.
     */
    record Prefix(Result result,SolidEventIntegrator.Transition transition) {}
    /** Transition rejections spent refining one segment towards a single event. Each one halves the
     * step, so this is a bound on how far below the controller's own estimate the search may go
     * before the event is simply declared where the integration currently stands. */
    private static final int MAXIMUM_TRANSITION_REJECTIONS=40;
    private static final String TRANSITION_REJECTION="Solid transport transition inside the step";
    private static final String REOPEN_REJECTION="Backward-Euler boundary reopened";

    /** The interval from {@code start}, the island's refresh at the interval's start (its graph and the boundaries the
     * refresh booked, which the result's boundaries begin with). */
    Result integrate(InventoryEquilibrium.Refreshed start,double duration,Settings settings,Runnable checkpoint,PassiveStepSolver.Acceptance acceptance,
                     StageGuard guard,double startingStep) {
        var prefix=run(start,duration,settings,checkpoint,acceptance,guard,startingStep);
        // No caller of the public contract can handle a prefix, so a guard that declares a
        // transition on this path escapes exactly as it did before there was a prefix at all.
        if(prefix.transition()!=null)throw prefix.transition();
        return prefix.result();
    }
    /** {@link #integrate} with the prefix returned rather than thrown; see {@link Prefix}. */
    Prefix integrateToTransition(InventoryEquilibrium.Refreshed start,double duration,Settings settings,Runnable checkpoint,PassiveStepSolver.Acceptance acceptance,
                                 StageGuard guard,double startingStep) {
        return run(start,duration,settings,checkpoint,acceptance,guard,startingStep);
    }
    /**
     * The interval, stepped by backward Euler under the state-change controller.
     *
     * <ul>
     * <li><b>Acceptance.</b> A converged step is accepted when the largest relative change of a vessel's pressure (over
     * max(100 Pa, P0)) or mass (over m0) is at most {@link #STATE_CAP}; otherwise it is refused and the step halved. A
     * change that halving did not reduce - at least 0.9 of the refused change at twice the step - is an algebraic jump of
     * the node's state, not a trajectory error, and the step that shows it is accepted (water entering a dry gas tank
     * drops its pressure about 5 % at any step size, review 8.6 (b)). A step at or below the transition floor is accepted
     * whatever its change. There is no rule on accepted-mode transitions: a symmetric pair of void edges opens and closes
     * together, so a one-transition rule halved every event to the floor and drove a resting line into the rounding
     * floor of its rows (review 8.6 (c) and (f)).</li>
     * <li><b>Growth.</b> After an accepted step the estimate grows by {@code clamp(0.9 sqrt(cap/change), 0.5, 2)} (2 at
     * zero change), bounded by the maximum step. A Newton failure, an equation-gate or conservation failure halves.</li>
     * <li><b>Boundary reopen.</b> A converged step that holds a passive run closed although its own end states drive the
     * run in an allowed direction ({@link PassiveStepSolver#reopenable}) is refused and solved again with that run
     * exempt from the start-of-solve closures; each run at most once per attempted step (review 8.8 (c)).</li>
     * <li><b>Cold start.</b> While no step of this structure has been accepted (after compile, a topology change or a
     * job start with no committed interval on it), one rate solve of the port graph supplies the junctions' starting
     * states; the balanced pressure seed treats vessel neighbours as held (review 8.6 (b) additions 1 and 2).</li>
     * <li><b>Events.</b> The stage guard's {@code checkRate} runs at every step start, on the previous accepted endpoint,
     * else on the endpoint carried from the last interval while no connection's identity or closure changed, else (a
     * guard that can declare something, first step) on one rate solve of the port graph; {@code checkFilters} runs on
     * every step's end. A declared transition is searched onto by halving, exactly as before.</li>
     * <li><b>Limits.</b> The attempt cap and the twenty-consecutive-rejection limit; transition refusals have their own
     * bound. An exhausted interval states its rejection map (an approach to a phase hold ends on twenty rejections, review
     * 8.8 (a)).</li>
     * </ul>
     */
    private Prefix run(InventoryEquilibrium.Refreshed refreshed,double duration,Settings settings,Runnable checkpoint,PassiveStepSolver.Acceptance acceptance,
                       StageGuard guard,double startingStep) {
        if(!Double.isFinite(duration)||duration<=0)throw new IllegalArgumentException("Positive finite interval required");
        if(!Double.isFinite(startingStep)||startingStep<=0)throw new IllegalArgumentException("Positive finite starting step required");
        PassiveNetwork accepted=refreshed.graph();
        // h is the controller's estimate; each attempt uses it truncated to the rest of the interval.
        double elapsed=0,h=Math.min(startingStep,settings.maximumStep);int acceptedCount=0,rejectedCount=0,consecutiveRejects=0;PassiveStepSolver.Result last=null;
        double[] transferred=new double[accepted.pipes().size()];
        double work=0;var boundaries=new ArrayList<ConservativeTransport.BoundaryTransfer>(refreshed.boundaries());
        var pipeTransfers=new PipeTransfer.Accumulator();
        var rejectionReasons=new LinkedHashMap<String,Integer>();
        String lastRejection="";
        // A transition is declared rather than refined once the step that would contain it is this
        // short, so it is located to within one such step of where it physically is.
        double transitionFloor=Math.max(1e-6,1e-9*duration);
        // The controller's own estimate at the moment the search onto an event began. Refining onto
        // a transition halves the step down to the floor, and that final micro-step is a statement
        // about where the event is, not about what the trajectory can take: handing it to the next
        // segment made every closure cost another twenty accepted substeps climbing back out.
        int transitionRejects=0;double beforeTransition=0;SolidEventIntegrator.Transition declared=null;
        // The last refused state change and its step, for the algebraic-jump test.
        double previousStateChange=0,previousStateStep=0;
        // Runs already reopened for the attempted step (same t0 and step).
        var reopened=new HashSet<PassiveNetwork.Pipe.Identity>();double reopenElapsed=-1,reopenStep=-1;
        // Rejections a phase hold decided: the last one, how many, and whether the latest rejection was one.
        com.wormzjl.createcheme.science.fluid.thermo.ThermoHold lastHold=null;int holdRejections=0;boolean lastWasHold=false;
        for(int attempt=0;attempt<settings.maximumAttempts&&elapsed<duration&&declared==null;attempt++) {
            checkpoint.run();double step=Math.min(h,duration-elapsed);
            try {
                if(accepted.reservoirs().stream().anyMatch(n->n.kind()==PassiveNetwork.NodeKind.PORT))throw new IllegalArgumentException("Internal ports cannot be integrated");
                // The start-of-step guard reads the previous accepted endpoint, which is this step's t0.
                if(last!=null)guard.checkRate(liveFilters(accepted),last.states(),last.modes(),last.massFlows());
                // The first step of an interval or segment reads the endpoint the last accepted step left, carried
                // across the call while no connection's identity or closure changed: without it a cake left a hair
                // below capacity at an interval boundary was never declared clogged (review 8.6 (f), 8.8 (a)).
                else if(carriedFlows(accepted)!=null)guard.checkRate(liveFilters(accepted),accepted.reservoirs().stream().map(PassiveNetwork.Reservoir::state).toList(),carriedModes,carriedFlows.clone());
                // With nothing carried (a new segment whose closures changed, or a fresh solver) and a guard that can
                // declare something (the solid event integrator's), one rate solve of the port graph supplies the t0
                // flows; a failed rate solve skips the t0 check (PassiveStepSolver.portRate).
                else if(guard!=StageGuard.NONE&&attempt==0) {
                    var rate=implicit.portRate(accepted,checkpoint,acceptance);
                    if(rate!=null)guard.checkRate(liveFilters(accepted),rate.states(),rate.modes(),rate.massFlows());
                }
                PassiveNetwork start=accepted;
                if(implicit.backwardEulerCold(accepted)) {
                    if(coldSeedFor!=accepted){coldSeed=coldRateSeed(accepted,checkpoint,acceptance);coldSeedFor=accepted;}
                    start=coldSeed;
                }
                var result=implicit.solve(start,step,checkpoint,acceptance);
                if(elapsed!=reopenElapsed||step!=reopenStep){reopened.clear();reopenElapsed=elapsed;reopenStep=step;}
                var candidates=new LinkedHashSet<>(implicit.reopenable(start,result));candidates.removeAll(reopened);
                if(!candidates.isEmpty()) {
                    reopened.addAll(candidates);implicit.reopenNext(candidates);rejectedCount++;
                    SolverDiagnostics.attempt(attempt,step,false,"be-reopen",candidates.size());
                    count(rejectionReasons,REOPEN_REJECTION);
                    continue;
                }
                guard.checkFilters(result.filters(),result.states(),result.modes(),result.massFlows());
                double change=stateChange(accepted,result.states());
                boolean jump=change>STATE_CAP&&previousStateChange>0&&step<=.51*previousStateStep&&change>=.9*previousStateChange;
                boolean ok=step<=transitionFloor||change<=STATE_CAP||jump;
                SolverDiagnostics.attempt(attempt,step,ok,ok&&jump&&step>transitionFloor?"be-jump":ok&&step<=transitionFloor&&change>STATE_CAP?"be-floor":"be-state",change);
                if(!ok){previousStateChange=change;previousStateStep=step;throw new StateChangeRejection("Backward-Euler state change "+change);}
                accepted=replace(accepted,result);last=result;elapsed+=step;acceptedCount++;consecutiveRejects=0;previousStateChange=0;
                var flush=flushTraces(accepted,checkpoint);if(flush!=null)accepted=flush.graph();
                implicit.markBackwardEulerAccepted(accepted);
                carriedModes=result.modes();carriedFlows=result.massFlows();carriedKey=flowKey(accepted);
                // The cold-start graphs are read only while the structure is cold, and the step solver's last solve only
                // by a test of the Jacobian; a retained island keeps neither (review 8.7 (c) E2).
                coldSeed=null;coldSeedFor=null;implicit.releaseLastSolve();
                work+=result.pumpWorkJoule();boundaries.addAll(result.boundaries());pipeTransfers.add(result.pipeTransfers(),1);
                if(flush!=null)boundaries.addAll(flush.boundaries());
                var flows=result.massFlows();for(int i=0;i<transferred.length;i++)transferred[i]+=step*flows[i];
                double factor=change>0?Math.clamp(.9*Math.sqrt(STATE_CAP/change),.5,2):2;
                h=Math.min(settings.maximumStep,Math.max(h,step)*factor);
                // Owner decision D42: an island the gauge reads at rest at this accepted step (NetworkReduction step 6 on its
                // end state and its flows) is no longer stepped. The rest of the interval is the identity: nothing moves, so
                // the step's transfers stand as booked and the interval's averages are what the steps moved.
                if(acceptance==PassiveStepSolver.Acceptance.FULL&&elapsed<duration&&NetworkReduction.atRest(accepted,model,flows,implicit.sliceRefused(accepted))) {
                    SolverDiagnostics.count(SolverDiagnostics.restStops);elapsed=duration;
                }
            }catch(SolidEventIntegrator.Transition transition) {
                // A rejection kind of its own. The step is not accepted, so no accepted step ever
                // contains a state past the transition; the search is a plain halving onto the
                // event, and it is separate from the other rejections both in the substep counts and
                // in the consecutive-rejection limit, which answers for a step size the controller
                // could not make work rather than for a physical regime boundary.
                SolverDiagnostics.count(SolverDiagnostics.solidTransitionRejections);
                SolverDiagnostics.attempt(attempt,step,false,"transition",Double.NaN);
                if(transition.atStart||step<=transitionFloor||transitionRejects>=MAXIMUM_TRANSITION_REJECTIONS){declared=transition;break;}
                if(transitionRejects==0)beforeTransition=h;
                transitionRejects++;
                if(rejectionReasons.size()<8||rejectionReasons.containsKey(TRANSITION_REJECTION))rejectionReasons.merge(TRANSITION_REJECTION,1,Integer::sum);
                h=step*.5;
            }catch(SparseNewton.Nonconvergence|IllegalArgumentException|StateChangeRejection rejected) {
                lastRejection=String.valueOf(rejected.getMessage());
                if(!(rejected instanceof StateChangeRejection))SolverDiagnostics.attempt(attempt,step,false,diagnosticReason(rejected),Double.NaN);
                // A trial a phase hold refused - directly, or a Newton pass whose failing line search it stopped - is
                // counted under its own key (one per phase set; never folded into "Other"), so a checkpoint, a view and the
                // certificate's transition check all see it.
                var hold=SparseNewton.phaseHold(rejected);
                if(hold!=null){rejectionReasons.merge(hold.reasonKey(),1,Integer::sum);lastHold=hold;holdRejections++;}
                else count(rejectionReasons,String.valueOf(rejected.getMessage()).replaceAll("[-+]?[0-9]+(?:\\.[0-9]+)?(?:[Ee][-+]?[0-9]+)?","#"));
                lastWasHold=hold!=null;
                rejectedCount++;consecutiveRejects++;h=step*.5;
                if(consecutiveRejects>=20||elapsed+h==elapsed)throw new SparseNewton.Nonconvergence("Substep refinement exhausted: "+rejected.getMessage()+"; reasons="+rejectionReasons,null,phaseHoldCause(lastWasHold,lastHold,holdRejections,rejectedCount));
            }
        }
        if(declared==null&&elapsed<duration)throw new SparseNewton.Nonconvergence("Interval substep limit; no partial interval may commit: advanced="+elapsed+" of "+duration+" s, accepted="+acceptedCount+", rejected="+rejectedCount+", reasons="+rejectionReasons+", last="+lastRejection,
                null,phaseHoldCause(lastWasHold,lastHold,holdRejections,rejectedCount));
        // A segment that ended on a declared transition hands the next one the estimate it held
        // before the search, not the micro-step the search finished on.
        nextStepEstimate=declared!=null&&transitionRejects>0?beforeTransition:h;
        if(last==null)return new Prefix(null,Objects.requireNonNull(declared,"A completed interval always has a last substep"));
        // An unbroken interval advanced exactly what was requested and normalizes by it; a prefix
        // normalizes by what it did advance, so the caller's advanced*averageMassFlows is still the
        // transported mass.
        double normalizer=declared==null?duration:elapsed;
        for(int i=0;i<transferred.length;i++)transferred[i]/=normalizer;
        return new Prefix(new Result(accepted,elapsed,transferred,acceptedCount,rejectedCount,work,boundaries,rejectionReasons,last.modes(),last.devicePressureChanges(),last.massFlows(),acceptance,pipeTransfers.snapshot()),declared);
    }
    /**
     * The share of a node's total amount below which a component is flushed at an accepted step (owner decision
     * 2026-09-27, option T1 of the phase-ports review's "Island holds F1 and F2"). A component that cannot leave a node
     * through any phase the node still exchanges (nitrogen left in a water-full node: it does not dissolve in free
     * water) shrinks geometrically and never reaches zero; near 1e-303 mol the Jacobian's 1/x entries overflow and the
     * island holds. The threshold is far below every tolerance of the solver and below the last bit of the node's
     * total, so the flush changes no other number of the node.
     */
    static final double TRACE_FLUSH_FRACTION=1e-24;
    /** The graph after a flush and what the flush removed, as boundary transfers at the flushed nodes themselves. */
    private record Flush(PassiveNetwork graph,List<ConservativeTransport.BoundaryTransfer> boundaries) {}
    /**
     * Removes every component below {@link #TRACE_FLUSH_FRACTION} of its vessel's or junction's total amount, restates
     * the node at its temperature and pressure, and books exactly what it removed: a boundary transfer at the node's
     * own id, signed like a port's (positive = into the network, so the removed moles are negative), whose energy is
     * the change of the node's owned total energy. The inventory's internal-energy field is kept, so that change is the
     * potential energy of the removed mass alone. The interval's ledger (owned after - owned before = boundaries)
     * therefore closes by construction, and a checkpoint or a certificate sees the flush as any other boundary
     * transfer; a certificate is not issued on an interval that contains one ({@code IslandCertificate.issue}), since
     * a one-time removal is not a rate. Returns null when nothing was flushed.
     */
    private Flush flushTraces(PassiveNetwork graph,Runnable checkpoint) {
        List<PassiveNetwork.Reservoir> nodes=null;List<ConservativeTransport.BoundaryTransfer> booked=null;double[] weights=null;
        for(int i=0;i<graph.reservoirs().size();i++) {
            var node=graph.reservoirs().get(i);if(node.fixed())continue;
            var held=node.inventory().moles();double total=0;for(double amount:held)total+=amount;
            double[] kept=null;
            for(int c=0;c<held.length;c++)if(held[c]>0&&held[c]<TRACE_FLUSH_FRACTION*total){if(kept==null)kept=held.clone();kept[c]=0;}
            if(kept==null)continue;
            checkpoint.run();
            if(weights==null)weights=model.molecularWeights();
            var removed=new double[held.length];double removedMass=0;
            for(int c=0;c<held.length;c++){removed[c]=kept[c]-held[c];removedMass-=removed[c]*weights[c];}
            var old=node.state();var state=model.flashTP(old.temperature(),old.pressure(),kept,checkpoint);
            if(!old.solids().empty())state=state.withSolids(old.solids());
            if(nodes==null){nodes=new ArrayList<>(graph.reservoirs());booked=new ArrayList<>();}
            nodes.set(i,new PassiveNetwork.Reservoir(node.id(),node.elevation(),state,node.kind(),
                    new PassiveNetwork.Inventory(node.inventory().volume(),kept,node.inventory().internalEnergy(),node.inventory().solids())));
            booked.add(new ConservativeTransport.BoundaryTransfer(node.id(),removed,-removedMass*PassiveStepSolver.GRAVITY*node.elevation()));
            SolverDiagnostics.count(SolverDiagnostics.traceFlushes);
        }
        return nodes==null?null:new Flush(new PassiveNetwork(nodes,graph.pipes(),graph.scheduledTransfers()),booked);
    }
    private static void count(Map<String,Integer> reasons,String reason) {
        if(reasons.size()<8||reasons.containsKey(reason))reasons.merge(reason,1,Integer::sum);else reasons.merge("Other",1,Integer::sum);
    }
    /**
     * The phase hold an interval that could not commit failed on, or null: its last rejection was a phase hold, or phase
     * holds were the dominant reason (more than half of the interval's rejections). An interval that only met one on
     * the way, and failed for another reason, is a numerical failure.
     */
    static com.wormzjl.createcheme.science.fluid.thermo.ThermoHold phaseHoldCause(boolean lastWasHold,
            com.wormzjl.createcheme.science.fluid.thermo.ThermoHold lastHold,int holdRejections,int rejected) {
        return lastWasHold||2*holdRejections>rejected?lastHold:null;
    }
    /** The diagnostic reason of an attempt that threw ("newton|message" with numbers masked). */
    private static String diagnosticReason(RuntimeException rejected) {
        String message=String.valueOf(rejected.getMessage()).replaceAll("[-+]?[0-9]+(?:\\.[0-9]+)?(?:[Ee][-+]?[0-9]+)?","#");
        if(message.length()>90)message=message.substring(0,90);
        return (rejected instanceof SparseNewton.Nonconvergence?"newton":"other")+"|"+message;
    }
    /** The cold-start graph with its junction states replaced by one rate solve's, and the graph it was built for. */
    private PassiveNetwork coldSeed,coldSeedFor;
    /**
     * The cold-start graph: the junctions' states replaced by one rate solve's of the port graph, whose flows the step
     * solver's warm start then carries into the first steps; the graph unchanged if it has no junction or the rate solve
     * fails. From the compiled property guess alone a pumped line into a dry tank, with three liquid junctions, failed
     * at every step size (review 8.6 (b), run 88d).
     */
    private PassiveNetwork coldRateSeed(PassiveNetwork graph,Runnable checkpoint,PassiveStepSolver.Acceptance acceptance) {
        if(graph.pipes().isEmpty()||graph.reservoirs().stream().noneMatch(PassiveNetwork.Reservoir::junction))return graph;
        var rate=implicit.portRate(graph,checkpoint,acceptance);
        if(rate==null)return graph;
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();
        for(int i=0;i<graph.reservoirs().size();i++){var node=graph.reservoirs().get(i);
            nodes.add(node.junction()?new PassiveNetwork.Reservoir(node.id(),node.elevation(),rate.states().get(i),node.kind(),node.inventory()):node);}
        return new PassiveNetwork(nodes,graph.pipes(),graph.scheduledTransfers());
    }
    /**
     * The start of an island job ({@code graph}, the island's committed state) given the island's committed interval
     * ({@code committed}; null for an island with none). Afterwards this solver holds exactly what a fresh solver derives
     * from the committed interval: when the job's island is the committed interval's (the same pipe identities and node
     * ids), its endpoint (modes, heads and flows) is carried into the job as every accepted step's endpoint is carried
     * into the next step inside a job: the step solver starts its first step from it ({@link PassiveStepSolver#replayStart})
     * and the start-of-step guard reads it ({@link #carriedFlows}). Otherwise nothing is carried. The cold-start seed
     * graphs are dropped.
     *
     * <p>Why committed state only: a certified island is woken with a fresh solver, a loaded world has fresh solvers, and
     * a replay must reproduce the island that solved every interval bit for bit. A Newton workspace carried across a job
     * boundary opened the next job's first solve on an older factorization, the converged pressures differed in the last
     * digits, and through the controller's step growth (a continuous function of the change) the difference reached the
     * committed inventories (review 8.9 (c), decision D8). The endpoint is committed state, so a fresh solver handed the
     * same committed interval starts where the retained one does.
     *
     * <p>Why the endpoint is carried: flows are continuous in time, so a slice's first instant has the flows the last one
     * ended with, and a connection at rest stays at rest. Started instead from the static per-connection estimate, a
     * riser standing at rest restarted at its velocity cap and the Newton walked it back by halving until it ran out of
     * iterations (card E6-S of the phase-ports batch, "The step starts from the committed flows" in its review).
     */
    public void replayStart(PassiveNetwork graph,Result committed) {
        graph=PassiveNetwork.sizeJunctionHoldups(graph,model);
        coldSeed=null;coldSeedFor=null;
        boolean continued=committed!=null&&PassiveStepSolver.sameStructure(graph,committed.graph())
                &&committed.endpointModes().size()==graph.pipes().size()&&committed.endpointFlows().length==graph.pipes().size();
        implicit.replayStart(graph,continued?committed:null);
        // The start-of-step guard and the inlet check's hysteresis (decision D6) read the committed endpoint.
        carriedModes=continued?committed.endpointModes():null;carriedFlows=continued?committed.endpointFlows():null;carriedKey=continued?flowKey(committed.graph()):List.of();
        inletModes=continued?committed.endpointModes():null;
        inletKey=continued?pipeIds(graph):List.of();
    }
    /** The accepted modes and flows of the last accepted step, carried across intervals for the first step's guard;
     * used only while no connection's identity or closure changed ({@link #flowKey}). */
    private List<FlowControl.Mode> carriedModes;
    private double[] carriedFlows;
    private List<Long> carriedKey=List.of();
    private static List<Long> flowKey(PassiveNetwork graph) {
        var key=new ArrayList<Long>();for(var pipe:graph.pipes()){key.add(pipe.id());key.add((long)pipe.blockedDirections());}
        return key;
    }
    private double[] carriedFlows(PassiveNetwork graph) {
        return carriedFlows!=null&&carriedModes!=null&&carriedKey.equals(flowKey(graph))?carriedFlows:null;
    }
    /** Reference density for a vessel's mass change, kg/m3 (owner decision 2026-09-27: liquid hydrocarbon service). A
     * vessel's mass change is measured against at least the mass it holds liquid-full at this density, so a nearly
     * empty vessel with a through-flow cannot lock the step controller (hold F7). The pressure check and the cap are
     * unchanged. */
    static final double STATE_MASS_REFERENCE_DENSITY=900;
    /** The largest relative pressure or mass change of a vessel (a RESERVOIR node); the mass change is relative to the
     * larger of the vessel's mass and {@link #STATE_MASS_REFERENCE_DENSITY} times its volume: the state's while the wall
     * carries it, the capacity while it is slack or empty (decision D54: a slack bag's own volume is its contents', which
     * would make a nearly empty bag with a through-flow the step controller's lock again). An empty vessel holds no mass. */
    private static double stateChange(PassiveNetwork before,List<FluidThermodynamics.State> after) {
        double change=0;
        for(int i=0;i<after.size();i++) {
            var node=before.reservoirs().get(i);if(node.kind()!=PassiveNetwork.NodeKind.RESERVOIR)continue;
            var a=node.state();var b=after.get(i);double capacity=node.inventory().volume();
            double mass=node.empty()?0:a.mass(),volume=!node.empty()&&PhaseLayout.taut(a,capacity)?a.volume():capacity;
            change=Math.max(change,Math.abs(b.pressure()-a.pressure())/Math.max(100,a.pressure()));
            change=Math.max(change,Math.abs(b.mass()-mass)/Math.max(1e-12,Math.max(mass,STATE_MASS_REFERENCE_DENSITY*volume)));
        }
        return change;
    }
    /** The cake each filter edge of this graph carries right now, for the start-of-step guard. */
    private static Map<Long,InlineFilter> liveFilters(PassiveNetwork graph) {
        Map<Long,InlineFilter> filters=null;
        for(var pipe:graph.pipes())if(pipe.filter()!=null){if(filters==null)filters=new HashMap<>();filters.put(pipe.id(),pipe.filter());}
        return filters==null?Map.of():filters;
    }
    public static PassiveNetwork replace(PassiveNetwork graph,PassiveStepSolver.Result result) {
        var states=result.states();
        if(states.size()!=graph.reservoirs().size())throw new IllegalArgumentException("State count mismatch");
        var reservoirs=new ArrayList<PassiveNetwork.Reservoir>();
        for(int i=0;i<states.size();i++){var old=graph.reservoirs().get(i);reservoirs.add(new PassiveNetwork.Reservoir(old.id(),old.elevation(),states.get(i),old.kind(),result.inventories().get(i)));}
        return new PassiveNetwork(reservoirs,graph.pipes().stream().map(p->result.filters().containsKey(p.id())?p.withFilter(result.filters().get(p.id())):p).toList(),graph.scheduledTransfers());
    }
    /**
     * A check the interval solver runs on every step it takes: {@link #checkRate} on the state a step starts from,
     * {@link #checkFilters} on the step's converged end. It throws to refuse the step: a
     * {@link SolidEventIntegrator.Transition} to declare a solid transport event (the interval solver then searches the
     * step onto it), an {@link ApproximationAnchor.Rejected} to send an approximate interval back to a full solve.
     */
    @FunctionalInterface public interface StageGuard {
        StageGuard NONE=(states,modes)->{};
        void check(List<FluidThermodynamics.State> states,List<FlowControl.Mode> modes);
        default void checkFlow(List<FluidThermodynamics.State> states,List<FlowControl.Mode> modes,double[] flows){check(states,modes);}
        default void checkFilters(Map<Long,InlineFilter> filters,List<FluidThermodynamics.State> states,List<FlowControl.Mode> modes,double[] flows){checkFlow(states,modes,flows);}
        /**
         * The state at the start of a step - the previous accepted step's end, the end carried from the last interval, or
         * one rate solve of the port graph when nothing is carried - together with the live cake of the graph being stepped.
         * A transition seen here is at the step's own t0 and is therefore already exactly located: the interval solver
         * declares it at the current elapsed time instead of refining the step towards it.
         */
        default void checkRate(Map<Long,InlineFilter> filters,List<FluidThermodynamics.State> states,List<FlowControl.Mode> modes,double[] flows){checkFlow(states,modes,flows);}
    }
}
