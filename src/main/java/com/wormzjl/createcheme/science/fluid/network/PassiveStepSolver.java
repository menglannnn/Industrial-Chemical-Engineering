package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.SolverOwnership;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.linalg.SparseLuSolver;
import com.wormzjl.createcheme.science.fluid.solver.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.math.DeterministicMath;
import com.wormzjl.createcheme.science.thermo.NumericalBox;
import com.wormzjl.createcheme.science.thermo.PhaseSupport;
import java.util.*;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.transport.SlurryTransport;

/** One simultaneous backward-Euler step with a fixed phase regime; no nested TP/UV/PH flash. */
public final class PassiveStepSolver {
    public enum Acceptance { FULL, APPROXIMATE }
    public static final double GRAVITY=9.80665;
    /**
     * How far below its own difference floor a solid moment unknown stops bounding the Newton step
     * length. The solid difference floors are {@code max(1e-6, encoded value)}, so on a node whose
     * moments are dust this is a scaled value of {@value #SOLID_CLAMP_FRACTION}e-6.
     *
     * <p>Swept on the ten- and thirty-reservoir water chains at 0 (the unfloored rule), 1e-6, 1e-3
     * and 1, i.e. scaled floors of 0, 1e-12, 1e-9 and 1e-6. All three nonzero values give exactly
     * the same accepted/rejected counts and no line-search stall anywhere; 0 still fails the
     * thirty-reservoir chain outright. The smallest that works is kept, so the exemption covers
     * only values at which {@code x + alpha*d} is bitwise {@code x} for every unknown of order one
     * and no backtrack could produce a different residual anyway.
     */
    private static final double SOLID_CLAMP_FRACTION=1e-6;
    /**
     * How far below its own difference floor a fluid amount unknown stops bounding the Newton step
     * length, and what a trial keeps of it instead: {@link Equations#project} holds such a trace at
     * {@value #TRACE_RETENTION} of its value rather than let the whole step shrink to that trace's
     * size. The same fraction as {@link #SOLID_CLAMP_FRACTION}, for the same reason: at a value this
     * far below the scale the Jacobian's own differences resolve the unknown on, the one-sided
     * difference step is a million times the unknown itself.
     *
     * <p>The case it settles is a connection whose flow the step reverses. A vessel's transported
     * amounts are linearized with the donor of the current flow sign, so a step that turns a
     * connection around predicts that the vessel on the far side loses the <em>near</em> side's
     * composition. Where that vessel holds a species only as a trace - the {@code 1e-12} entry trace
     * {@code initialPhaseSeeds} plants for every reachable component, or the 1e-14 to 1e-30 a reversal
     * has already carried there - the prediction drives it negative by orders of magnitude, and
     * {@link Equations#maximumStep}'s {@code .99*x/-d} rule then cut every step of the whole island
     * to that trace's size: 2e-10 of a step at the first iteration, 2e-12 at the next, until the line
     * search reported a stall at a residual its own direction would have removed. Measured on every
     * pumped fill of a chain of nitrogen tanks with the placement defaults: water entering the first
     * tank evaporates into its dry nitrogen, cools it about 17 K to its wet-bulb temperature within a
     * few milliseconds, its pressure drops about 4.5 kPa, and every connection downstream reverses;
     * 73 of the 76 Newton failures of the three-tank chain's first interval and every one of the
     * six-tank chain's held retries in game ({@code Newton line search stalled at residual 0.096...})
     * were this limiter on a steam trace in the second tank. See
     * {@code documentation/fluid-followups/FLUID_PUMPED_FILL_REVIEW.md}.
     *
     * <p>Projecting instead is safe because such a trace is invisible to every row the Newton
     * converges on: its material row is scaled by the node's whole amount (a gas node, or a trace
     * the node did not hold at the start of the step), so a trace below the floor moves it by less
     * than {@code 1e-10}, below the tightest tolerance; its equilibrium and closure rows carry it at
     * the same order. Inventories are rebuilt from the converged flows by
     * {@link ConservativeTransport#reconstruct}, so conservation never depends on the value the
     * projection kept. In a liquid-full node whose trace is resolved against its own reference the
     * difference floor is that trace itself, so only dust a millionth of it is exempt.
     */
    private static final double TRACE_CLAMP_FRACTION=1e-6,TRACE_RETENTION=.01;
    /**
     * How far below its stated capacity the saturated filter inlet aims. The committed cake is
     * reconstructed from the converged flow by a different summation than the loading row solves,
     * so a law aimed exactly at capacity lands within a unit in the last place of it on either
     * side, and a cake one ULP over its own capacity is not a state the owned inventory should
     * ever hold. Aiming a hair under makes the landing one-sided while leaving it at capacity to
     * far better than any audit resolves; {@link SolidEventIntegrator} treats a cake this close to
     * capacity as having met it.
     */
    private static final double FILTER_CAPACITY_MARGIN=1e-12;
    /**
     * The share of a row's gap a chord step may leave before it stops being worth taking ({@link Equations#linearisationHolds}).
     * A chord step on a row whose reused Jacobian states the slope s where the row's own is t moves that row by t/s of its
     * gap and leaves |1 - t/s| of it: a stated slope twice the row's own closes half (the turbulent law walking towards
     * rest, A371), and one two thirds of it overshoots by half and the residual changes sign. Past one half either way a
     * chord step does no better than the halving a fresh step makes, so the reused Jacobian saves nothing.
     */
    static final double CHORD_CONTRACTION=.5;
    /** Whether a chord step still closes at least half of a row's gap where the Jacobian states the slope {@code stated} and
     * the row's own is {@code own} ({@link #CHORD_CONTRACTION}). */
    static boolean chordHolds(double stated,double own){return Math.abs(1-own/stated)<=CHORD_CONTRACTION;}
    /**
     * The holdup of a junction, in seconds of its largest connection's velocity-cap flow: a junction's buffer holds
     * {@code m_J = HOLDUP_TAU * max over its connections of rho_seed * minimumArea * velocityLimit(seed)} of fluid, minted
     * from its seed state when it is first solved; see {@link PassiveNetwork#sizeJunctionHoldups}.
     *
     * <p>Why a junction has a buffer: a junction without one mixes by a ratio closure, the inflow-weighted mixture of
     * what arrives, which is singular at zero through-flow, and an island coming to rest drives every junction there. With
     * a holdup the junction's mixing and enthalpy rows are the backward-Euler vessel rows
     * {@code (m_J/dt + Q) w = (m_J/dt) w_old + sum q w_e}, regular at Q = 0, and the reconstruction states the junction with
     * the same row, its mass pinned at m_J ({@code ConservativeTransport.pinnedFlows}). The buffer is the row's state, not
     * inventory (owner decision D46): nothing is booked at a mint or a destroy, and the ledger reports the buffers' change
     * as its own term ({@link #checkConservation}). The holdup is a numerical
     * regularisation sized to the flow, not to the throughput over a step (the longest step is 20 s): its mixing lag at
     * full flow is m_J/Q = tau. See documentation/2026-09-24-mixed-gas-junction/HANDOFF_REVIEW.md 7.2, 7.6 and 7.10, and
     * decision D2 of that batch's DECISION_LOG.md.
     */
    static final double HOLDUP_TAU=.05;
    /** A connection's cap state ({@link Equations#capDirections}): on its law, or not decided yet (decided on the next
     * pass's start point). A capped connection's state is the end its cap flow leaves, 0 or 1. */
    static final int CAP_OPEN=-1,CAP_START=-2;
    /**
     * The movers (pipe ids) refused for the slice in progress ({@link FlowControl.Mode#INLET_WRONG_PHASE}, decision D6),
     * set by the interval solver at each slice start ({@link #refuseInlets}) and read by every solve until the next;
     * null for a step solver no interval drives, which decides at each solve ({@link #refusedInlets}).
     */
    private Set<Long> sliceRefusals;
    /** The movers (pipe ids) refused for the slice that starts now; see {@link PassiveIntervalSolver} and {@link InletPhase}. */
    void refuseInlets(Set<Long> pipeIds){sliceRefusals=Set.copyOf(pipeIds);}
    /** The movers refused for the slice in progress ({@link #refuseInlets}), one flag per connection of {@code graph}; null
     * when no interval solver set them. */
    boolean[] sliceRefused(PassiveNetwork graph) {
        if(sliceRefusals==null)return null;
        boolean[] refused=new boolean[graph.pipes().size()];
        for(int i=0;i<refused.length;i++)refused[i]=graph.pipes().get(i).control() instanceof FlowControl.Mover&&sliceRefusals.contains(graph.pipes().get(i).id());
        return refused;
    }
    /**
     * Which connections of {@code graph} this solve holds in {@link FlowControl.Mode#INLET_WRONG_PHASE}: the slice's
     * refusals when an interval solver set them ({@link #refuseInlets}: decided once per slice at its start, on the
     * committed state and the committed endpoint modes, so every step, rate seed and certified replay of the slice agrees);
     * otherwise, for a step solver used on its own, {@link InletPhase#decide} on this solve's input states, with the
     * carried modes as the hysteresis input and the step as the slice (a rate solve: the leading phase).
     */
    private boolean[] refusedInlets(PassiveNetwork graph,boolean carryModes,double dt) {
        boolean[] refused=new boolean[graph.pipes().size()];
        if(sliceRefusals!=null) {
            for(int i=0;i<refused.length;i++)refused[i]=graph.pipes().get(i).control() instanceof FlowControl.Mover&&sliceRefusals.contains(graph.pipes().get(i).id());
            return refused;
        }
        if(graph.pipes().stream().noneMatch(pipe->pipe.control() instanceof FlowControl.Mover))return refused;
        return InletPhase.decide(model,graph,carryModes?previousModes:null,rateOnly?0:dt);
    }
    /**
     * Whether a mover's target mass flow on the start state ({@code Q rho_s}, the suction end's density) exceeds the
     * velocity cap of a link on its suction walk ({@link InletPhase#walk}: each link's cap on the stream its own donor end
     * draws, {@link #massFlowLimit}), or a link of the walk may not carry flow towards the device at all. Such a target
     * has no solution (the target row fixes the device's flow, the cap the suction line's, and the junction balances
     * equate them), so the device starts on its head limit, where the flow is free (plan 3.6, "Target above what the
     * suction can deliver"). A port's D11 capacities bound only the phases before its last, never its total (decision
     * A22), so they are no limit here; a port that would run into another phase is the inlet check's business. A device
     * drawing straight from its first node walks no link, and the pump edge's own cap is tested by the caller.
     */
    private boolean suctionCapped(PassiveNetwork graph,int edge,FlowControl.Mover mover) {
        var walk=InletPhase.walk(graph,edge);
        if(walk.links().length==0)return false;
        var pipe=graph.pipes().get(edge);
        double target=suctionMassFlow(mover.targetVolumeFlow(),graph.reservoirs().get(pipe.first()).state(),pipe.firstPort());
        for(int k=0;k<walk.links().length;k++) {
            var link=graph.pipes().get(walk.links()[k]);int donor=walk.donors()[k];
            if(!boundaryAllowed(graph,link,link.first()==donor?1:-1))return true;
            if(target>massFlowLimit(link,graph.reservoirs().get(donor).state(),link.portAt(donor)))return true;
        }
        return false;
    }
    /** Connections the next step solve may not close at its start; set by {@link #reopenNext}, consumed by that solve. */
    private Set<PassiveNetwork.Pipe.Identity> keepOpen=Set.of();
    /** Exempts these connections from the start-of-solve closures ({@link #closeDeadHeads}, {@link #closeIllegalStarts})
     * of the next step solve only; the pass loop may still close them on a converged point. */
    void reopenNext(Set<PassiveNetwork.Pipe.Identity> identities){keepOpen=Set.copyOf(identities);}
    /** The driving pressure (Pa) a closed run's end states must exceed in an allowed direction to be reopened. */
    static double reopenBand(double pa,double pb){return Math.max(1,1e-6*Math.max(Math.abs(pa),Math.abs(pb)));}
    /**
     * The passive connections a converged step holds closed ({@code boundaryClosed}, i.e. accepted mode CLOSED; not a
     * connection blocked both ways before the solve) whose run - the maximal chain of closed passive connections through
     * degree-two junctions no actuator touches, the run {@link #closeDeadHeads} decides on - is driven by the step's own
     * end states in a direction every link allows, by more than {@link #reopenBand}. A forward run stands in the first
     * end's fluid and a reverse run in the second's, as in {@link #closeDeadHeads}. A junction end counts only while one
     * of its connections is open in the step, so its pressure is a solved hydraulic pressure; a junction every
     * connection of which is closed says nothing.
     *
     * <p>Why: {@link #closeDeadHeads} and {@link #closeIllegalStarts} decide a closure on the solve's start point, and the
     * pass loop never reopens a boundary closure within a solve. A backward-Euler step is solved once, so a step that
     * starts closed and should open part-way (an injection into tanks at rest, a withdrawal from a dead-headed tank) would
     * be solved shut throughout, and the tanks would bottle up or starve for the whole step: 2.8 kPa at 13 s on a 5 s
     * slice (review 8.8 (b) and (c)). The interval solver refuses such a step and solves it again with the run exempt.
     */
    Set<PassiveNetwork.Pipe.Identity> reopenable(PassiveNetwork graph,Result result) {
        int edges=graph.pipes().size(),nodes=graph.reservoirs().size();
        var states=result.states();var modes=result.modes();
        if(modes.size()!=edges||states.size()!=nodes)return Set.of();
        boolean[] closed=new boolean[edges];boolean[] open=new boolean[nodes];boolean any=false;
        for(int e=0;e<edges;e++) {
            var pipe=graph.pipes().get(e);
            closed[e]=pipe.control() instanceof FlowControl.Passive&&pipe.blockedDirections()!=3&&modes.get(e)==FlowControl.Mode.CLOSED;any|=closed[e];
            if(modes.get(e)!=FlowControl.Mode.CLOSED&&modes.get(e)!=FlowControl.Mode.INLET_WRONG_PHASE){open[pipe.first()]=true;open[pipe.second()]=true;}
        }
        if(!any)return Set.of();
        int[] degree=new int[nodes];boolean[] actuated=new boolean[nodes];
        for(var pipe:graph.pipes()) {
            degree[pipe.first()]++;degree[pipe.second()]++;
            if(!(pipe.control() instanceof FlowControl.Passive)){actuated[pipe.first()]=true;actuated[pipe.second()]=true;}
        }
        boolean[] interior=new boolean[nodes];
        for(int i=0;i<nodes;i++)interior[i]=graph.reservoirs().get(i).junction()&&degree[i]==2&&!actuated[i];
        int[][] nodeEdges=new int[nodes][];
        for(int i=0;i<nodes;i++)nodeEdges[i]=new int[degree[i]];
        Arrays.fill(degree,0);
        for(int edge=0;edge<edges;edge++){var pipe=graph.pipes().get(edge);nodeEdges[pipe.first()][degree[pipe.first()]++]=edge;nodeEdges[pipe.second()][degree[pipe.second()]++]=edge;}
        boolean[] taken=new boolean[edges];var reopen=new LinkedHashSet<PassiveNetwork.Pipe.Identity>();
        // A phase port never refuses a direction (decision D11: it draws its next phase instead), so only the static
        // boundary rule decides which way a run may be reopened.
        for(int edge=0;edge<edges;edge++) {
            if(taken[edge]||!closed[edge])continue;
            var pipe=graph.pipes().get(edge);
            var chain=new ArrayList<Integer>();var order=new ArrayList<Integer>();
            order.add(pipe.first());chain.add(edge);order.add(pipe.second());taken[edge]=true;
            for(int end=0;end<2;end++) {
                int at=end==0?pipe.first():pipe.second(),from=edge;
                while(interior[at]) {
                    int next=nodeEdges[at][0]==from?nodeEdges[at][1]:nodeEdges[at][0];
                    if(taken[next]||!closed[next])break;
                    taken[next]=true;var step=graph.pipes().get(next);
                    int beyond=step.first()==at?step.second():step.first();
                    if(end==0){chain.addFirst(next);order.addFirst(beyond);}else{chain.add(next);order.add(beyond);}
                    at=beyond;from=next;
                }
            }
            int start=order.getFirst(),finish=order.getLast();
            if(start==finish)continue;
            if(graph.reservoirs().get(start).junction()&&!open[start]||graph.reservoirs().get(finish).junction()&&!open[finish])continue;
            boolean forwardAllowed=true,reverseAllowed=true;
            for(int position=0;position<chain.size();position++) {
                var link=graph.pipes().get(chain.get(position));boolean aligned=link.first()==order.get(position);
                forwardAllowed&=boundaryAllowed(graph,link,aligned?1:-1);
                reverseAllowed&=boundaryAllowed(graph,link,aligned?-1:1);
            }
            var a=states.get(start);var b=states.get(finish);
            // Each end of the run stands in what its own end port draws, at its own end's driving pressure.
            var startPort=graph.pipes().get(chain.getFirst()).portAt(start);var finishPort=graph.pipes().get(chain.getLast()).portAt(finish);
            double pa=endPressure(graph.reservoirs().get(start),startPort,a),pb=endPressure(graph.reservoirs().get(finish),finishPort,b);
            double dz=graph.reservoirs().get(finish).elevation()-graph.reservoirs().get(start).elevation(),difference=pa-pb;
            double band=reopenBand(pa,pb);
            double forwardDrive=difference-endDensity(a,startPort)*GRAVITY*dz,reverseDrive=difference-endDensity(b,finishPort)*GRAVITY*dz;
            if(forwardAllowed&&forwardDrive>band||reverseAllowed&&reverseDrive<-band)for(int link:chain)reopen.add(graph.pipes().get(link).identity());
        }
        return reopen;
    }
    private final FluidThermodynamics model;
    private final SolverOwnership ownership;
    private final Map<WorkspaceKey,SparseNewton.Workspace> workspaces=new LinkedHashMap<>();
    private final Map<WorkspaceKey,SparseNewton.Workspace> structures=new LinkedHashMap<>();
    private List<PassiveNetwork.Pipe.Identity> previousPipes=List.of();
    private long[] previousNodeIds=new long[0];
    private double[] previousFlows=new double[0],previousHeads=new double[0];
    /** The input states {@link #previousFlows} were recorded at, and the input states of the solve in progress: a
     * rate-only solve warm-starts only from flows recorded at value-identical input states; see {@link #startPoint}. */
    private List<FluidThermodynamics.State> previousInputStates=List.of(),inputStates=List.of();
    /**
     * The device active set the last pass of the last solve on this graph settled on.
     *
     * <p>A pump standing on its head limit, or at its own shutoff corner, is a property of the
     * island's state and not of one step. Restarting every solve on the target branch makes each
     * one re-walk the whole power-law hydraulic curve down from a flow the head limit has already
     * refused - 9.96 kg/s to 0.07 near shutoff - and the closer the discharge sits to shutoff the
     * longer that walk is, until it no longer fits in the Newton iteration budget at any step size.
     * Starting where the previous point ended makes the first pass the right one and its Newton a
     * correction rather than a descent. Valid only while {@link #previousPipes} and
     * {@link #previousNodeIds} still describe this graph.
     */
    private List<FlowControl.Mode> previousModes=List.of();
    /**
     * The velocity cap states ({@link Equations#capDirections}) the last pass of the last solve on this graph settled on,
     * recorded with {@link #previousModes} (decision A271): a line choked at the end of one step starts the next choked,
     * as a pump on its head limit does, and a converged point that refutes it releases it. At a job start, the committed
     * interval's endpoint modes and flows ({@link #replayStart}).
     */
    private int[] previousCaps=new int[0];
    /** The structure of the last solve this solver accepted; {@link #closeIllegalStarts} acts only on it. */
    private List<PassiveNetwork.Pipe.Identity> acceptedPipes=List.of();
    private long[] acceptedNodeIds=new long[0];
    /** The structure of the last step the interval solver's controller accepted (a converged solve it refused does not
     * count): the cold start ({@link #backwardEulerCold}) lasts until then. */
    private List<PassiveNetwork.Pipe.Identity> beAcceptedPipes=List.of();
    private long[] beAcceptedNodeIds=new long[0];
    /** The interval solver accepted a step of this structure. */
    void markBackwardEulerAccepted(PassiveNetwork graph){beAcceptedPipes=identities(graph);beAcceptedNodeIds=nodeIds(graph);}
    /** Whether no step of this structure has been accepted yet: the cold start after compile, a topology change, or a job
     * start with no committed interval on this structure. */
    boolean backwardEulerCold(PassiveNetwork graph){return !(beAcceptedPipes.equals(identities(graph))&&Arrays.equals(beAcceptedNodeIds,nodeIds(graph)));}
    /**
     * Puts this solver, at the start of an island job, in the state a fresh solver would be in given only the island's
     * committed interval {@code committed} (null when the job's island has none on this structure), so a job's solves
     * depend on the committed interval and the slice and not on what this solver did in earlier jobs (see
     * {@link PassiveIntervalSolver#replayStart}).
     * <ul>
     * <li>No Newton opens on a factorization from an earlier job: every retained workspace's numeric Jacobian and LU are
     * dropped, and the dt-keyed workspaces themselves. The sparsity pattern, colouring and fill-reducing ordering stay:
     * they are functions of the structure only.</li>
     * <li>The warm start is the committed endpoint: the first step starts from the committed interval's end flows and
     * heads ({@link #startPoint}), recorded at its end states, exactly as every later step starts from the step before.
     * The pump active-set carry ({@link #previousModes}) is its endpoint modes (a pump on its head limit or CLOSED there
     * opens there). Nothing comes from whatever this solver solved last (a refused step, an uncommitted planner trial, a
     * rate solve). With no committed interval on this structure, the first solve starts from {@code initialMassFlows} of
     * the job's states.</li>
     * <li>"This structure has been accepted" (the cold start and the void-start history gate) is true exactly when the
     * island has a committed interval on this structure.</li>
     * <li>The reopen exemption and the retained last solve are dropped.</li>
     * </ul>
     */
    void replayStart(PassiveNetwork graph,PassiveIntervalSolver.Result committed) {
        ownership.check("Each executing island job needs its own step workspace");
        // Exactly a fresh solver's numerics: no dt-keyed workspace, and every structure's latest workspace replaced by a
        // fork that keeps its pattern, colouring and ordering (functions of the structure) and no Jacobian or LU. An
        // invalidated workspace left under its dt key would not inherit the chord this job builds on its first step, so a
        // carried job refreshed its Jacobian where a fresh one reused it (RetainedSolverTest).
        workspaces.clear();
        for(var entry:structures.entrySet())entry.setValue(entry.getValue().forkStructure());
        previousFlows=new double[0];previousHeads=new double[0];previousInputStates=List.of();inputStates=List.of();
        previousPipes=List.of();previousNodeIds=new long[0];previousModes=List.of();previousCaps=new int[0];
        keepOpen=Set.of();lastSolve=null;
        if(committed!=null) {
            var ids=identities(graph);long[] nodes=nodeIds(graph);
            beAcceptedPipes=ids;beAcceptedNodeIds=nodes;acceptedPipes=ids;acceptedNodeIds=nodes;
            previousPipes=ids;previousNodeIds=nodes;previousModes=committed.endpointModes();
            // A connection the committed point left on its cap starts there, in the direction of its end flow.
            previousCaps=new int[ids.size()];
            for(int i=0;i<previousCaps.length;i++)previousCaps[i]=capMode(previousModes.get(i))?committed.endpointFlows()[i]>=0?0:1:CAP_OPEN;
            previousFlows=committed.endpointFlows();previousHeads=committed.endpointHeads();
            previousInputStates=committed.graph().reservoirs().stream().map(PassiveNetwork.Reservoir::state).toList();
        } else {beAcceptedPipes=List.of();beAcceptedNodeIds=new long[0];acceptedPipes=List.of();acceptedNodeIds=new long[0];}
    }
    /** The same pipe identities and node ids. */
    static boolean sameStructure(PassiveNetwork a,PassiveNetwork b){return identities(a).equals(identities(b))&&Arrays.equals(nodeIds(a),nodeIds(b));}
    /** The last successful solve, for the block Jacobian check; see {@link #acceptedEquations}. */
    record LastSolve(Equations equations,double[] variables) {}
    private LastSolve lastSolve;
    private boolean rateOnly;
    /** Owned by this solver, which the ownership latch confines to one worker at a time. */
    private final com.wormzjl.createcheme.science.fluid.transport.MixtureViscosity.Workspace viscosities=
            new com.wormzjl.createcheme.science.fluid.transport.MixtureViscosity.Workspace();
    /** The island's retained transport linear algebra; an island holds exactly one. */
    private final ConservativeTransport.Workspace transport;
    public PassiveStepSolver(FluidThermodynamics model){this(model,SolverOwnership.confinedToCurrentThread());}
    public PassiveStepSolver(FluidThermodynamics model,SolverOwnership ownership) {
        this.model=Objects.requireNonNull(model);this.ownership=Objects.requireNonNull(ownership);
        transport=new ConservativeTransport.Workspace(ownership);
    }
    public record Result(List<FluidThermodynamics.State> states,double[] massFlows,double deltaTime,SparseNewton.Result numerical,
                         List<FlowControl.Mode> modes,double[] devicePressureChanges,double pumpWorkJoule,double[] externalMoles,double externalEnergyJoule,
                         List<PassiveNetwork.Inventory> inventories,List<ConservativeTransport.BoundaryTransfer> boundaries,List<PipeTransfer> pipeTransfers,Map<Long,InlineFilter> filters) {
        public Result(List<FluidThermodynamics.State> states,double[] massFlows,double deltaTime,SparseNewton.Result numerical,List<FlowControl.Mode> modes,double[] devicePressureChanges,double pumpWorkJoule,double[] externalMoles,double externalEnergyJoule,List<PassiveNetwork.Inventory> inventories,List<ConservativeTransport.BoundaryTransfer> boundaries,List<PipeTransfer> pipeTransfers){this(states,massFlows,deltaTime,numerical,modes,devicePressureChanges,pumpWorkJoule,externalMoles,externalEnergyJoule,inventories,boundaries,pipeTransfers,Map.of());}
        public Result {filters=Map.copyOf(filters);states=List.copyOf(states);massFlows=massFlows.clone();modes=List.copyOf(modes);devicePressureChanges=devicePressureChanges.clone();externalMoles=externalMoles.clone();inventories=List.copyOf(inventories);boundaries=List.copyOf(boundaries);pipeTransfers=List.copyOf(pipeTransfers);}
        @Override public double[] massFlows(){return massFlows.clone();}
        @Override public double[] devicePressureChanges(){return devicePressureChanges.clone();}
        @Override public double[] externalMoles(){return externalMoles.clone();}
    }
    public Result solve(PassiveNetwork graph,double dt,Runnable checkpoint) {
        return solve(graph,dt,checkpoint,Acceptance.FULL);
    }
    public Result solveRate(PassiveNetwork graph,Runnable checkpoint,Acceptance acceptance){return solve(graph,1,checkpoint,acceptance,true);}
    /**
     * One rate solve of {@code graph} with its vessels held as ports ({@link PassiveNetwork.NodeKind#PORT}, dt = 1), or
     * null when it has no connection or does not converge. Both start-of-interval checks read the island through it: the
     * interval solver's guard ({@code PassiveIntervalSolver}) and the solid closure check ({@link SolidEventIntegrator}).
     * The rate solve only pre-empts a closure the steps would otherwise find, and on an island's first interval its
     * junctions hold their minted start state, so a rate solve that fails is no evidence of anything: it declares no
     * closure, and the steps, whose guards see every transition at t0 and inside the interval, decide (option R1 of the
     * phase-ports review, hold class E1).
     */
    Result portRate(PassiveNetwork graph,Runnable checkpoint,Acceptance acceptance) {
        if(graph.pipes().isEmpty())return null;
        var ports=new ArrayList<PassiveNetwork.Reservoir>();
        for(var node:graph.reservoirs())ports.add(new PassiveNetwork.Reservoir(node.id(),node.elevation(),node.state(),
                node.kind()==PassiveNetwork.NodeKind.RESERVOIR?PassiveNetwork.NodeKind.PORT:node.kind(),node.inventory()));
        try{return solveRate(new PassiveNetwork(ports,graph.pipes()),checkpoint,acceptance);}
        catch(SparseNewton.Nonconvergence|IllegalArgumentException|ApproximationAnchor.Rejected failed){return null;}
    }
    public Result solve(PassiveNetwork graph,double dt,Runnable checkpoint,Acceptance acceptance) {return solve(graph,dt,checkpoint,acceptance,false);}
    private Result solve(PassiveNetwork inputGraph,double dt,Runnable checkpoint,Acceptance acceptance,boolean rateOnly) {
        // A junction minted by its constructor and not yet sized to its connections is sized here (no-op otherwise).
        inputGraph=PassiveNetwork.sizeJunctionHoldups(inputGraph,model);
        this.rateOnly=rateOnly;
        inputStates=inputGraph.reservoirs().stream().map(PassiveNetwork.Reservoir::state).toList();
        Objects.requireNonNull(acceptance);SolverDiagnostics.count(SolverDiagnostics.implicitSolves);
        ownership.check("Each executing island job needs its own step workspace");
        lastSolve=null;
        if(!Double.isFinite(dt)||dt<=0)throw new IllegalArgumentException("Positive finite substep required");
        // A cold junction's pressure guess replaced by the balanced one; a Newton starting guess only, the junction's
        // owned inventory is untouched. See {@link #seedBoundaryJunctions}.
        var graph=seedBoundaryJunctions(inputGraph,checkpoint);
        if(graph.pipes().isEmpty()&&graph.scheduledTransfers().isEmpty())return new Result(graph.reservoirs().stream().map(PassiveNetwork.Reservoir::state).toList(),new double[0],dt,
                new SparseNewton.Result(new double[0],0,0,0,0,0),List.of(),new double[0],0,new double[model.hydrocarbon.componentCount()+1],0,
                graph.reservoirs().stream().map(PassiveNetwork.Reservoir::inventory).toList(),List.of(),List.of());
        var modes=new ArrayList<FlowControl.Mode>();
        boolean carryModes=previousModes.size()==graph.pipes().size()&&previousPipes.equals(identities(graph))&&Arrays.equals(previousNodeIds,nodeIds(graph));
        // Pumps this solve has recognized as standing at their own shutoff corner; see
        // {@link #shutoffBand}. Not part of any workspace or cycle key: it changes no equation, it
        // only forbids one transition from being taken back within this solve.
        boolean[] atShutoff=new boolean[graph.pipes().size()];
        for(var pipe:graph.pipes())modes.add(switch(pipe.control()) {
            case FlowControl.Passive ignored->FlowControl.Mode.PASSIVE;
            case FlowControl.Mover pump->pump.targetVolumeFlow()==0?FlowControl.Mode.CLOSED:
                    graph.pipes().stream().anyMatch(p->p.blockedDirections()!=0)?FlowControl.Mode.PUMP_HEAD_LIMIT:
                    pump.targetVolumeFlow()>pipe.minimumArea()*endVelocityLimit(graph.reservoirs().get(pipe.first()).state(),pipe.firstPort())?FlowControl.Mode.PUMP_HEAD_LIMIT:
                    suctionCapped(graph,modes.size(),pump)?FlowControl.Mode.PUMP_HEAD_LIMIT:FlowControl.Mode.PUMP_TARGET;
            case FlowControl.PressureValve valve->graph.reservoirs().get(pipe.first()).state().pressure()>valve.targetPressure()+.01
                    ?FlowControl.Mode.VALVE_OPEN:FlowControl.Mode.CLOSED;
        });
        // A pump the previous point left on its head limit, on its suction limit or at its shutoff corner opens there;
        // see {@link #previousModes}. Only those three: the target branch is decided against this
        // step's own states above, and the velocity cap is carried as its own state ({@link #previousCaps}).
        if(carryModes)for(int i=0;i<modes.size();i++) {
            var pipe=graph.pipes().get(i);
            if(!(pipe.control() instanceof FlowControl.Mover pump)||pump.targetVolumeFlow()==0)continue;
            var carried=previousModes.get(i);
            if(carried!=FlowControl.Mode.PUMP_HEAD_LIMIT&&carried!=FlowControl.Mode.PUMP_SUCTION_LIMIT&&carried!=FlowControl.Mode.CLOSED)continue;
            modes.set(i,carried);
            // The one place a pump that stood at its shutoff corner is offered its head limit back.
            // The test is taken on the accepted state this step starts from, once per solve, so it
            // is not a transition the pass sequence can take again; see {@link #atShutoff}.
            if(carried!=FlowControl.Mode.CLOSED)continue;
            var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
            double suction=endDensity(a.state(),pipe.firstPort());
            double margin=riseLimit(pump,suction,a.state().pressure())-(endPressure(b,pipe.secondPort(),b.state())-endPressure(a,pipe.firstPort(),a.state())
                    +suction*GRAVITY*(b.elevation()-a.elevation()));
            if(margin>shutoffBand(Math.max(a.state().pressure(),b.state().pressure()),1e-9))modes.set(i,FlowControl.Mode.PUMP_HEAD_LIMIT);
            else atShutoff[i]=true;
        }
        // A connection closed to all transport before the solve starts is in the same active-set
        // state the pass-by-pass closure below produces, actuator included: an actuator left on its
        // own setpoint across a closed edge is a second equation for a flow the closure has already
        // decided, and on a junction whose other edges are closed too it is one equation too many
        // and the factorization is singular.
        boolean[] boundaryClosed=new boolean[graph.pipes().size()];
        for(int i=0;i<boundaryClosed.length;i++)if(graph.pipes().get(i).blockedDirections()==3) {
            boundaryClosed[i]=true;
            if(!(graph.pipes().get(i).control() instanceof FlowControl.Passive))modes.set(i,FlowControl.Mode.CLOSED);
        }
        // Decision D6: a pump or compressor refused for this slice because its supply is the wrong phase is closed for the
        // whole solve like a connection blocked both ways (its flow is exactly zero, no pass reopens it, nothing reaches
        // its discharge through it), and holds the mode INLET_WRONG_PHASE, which the carry never restates; see
        // {@link #refusedInlets}.
        boolean[] refused=refusedInlets(inputGraph,carryModes,dt);
        for(int i=0;i<refused.length;i++)if(refused[i]){boundaryClosed[i]=true;modes.set(i,FlowControl.Mode.INLET_WRONG_PHASE);}
        // A run the interval solver reopened is exempt from the start-of-solve closures of this one solve; the pass loop
        // may still close it on a converged point. See {@link #reopenable}.
        boolean[] keep=null;
        if(!keepOpen.isEmpty()&&!rateOnly){var ids=identities(graph);keep=new boolean[ids.size()];for(int i=0;i<keep.length;i++)keep[i]=keepOpen.contains(ids.get(i));keepOpen=Set.of();}
        closeDeadHeads(graph,boundaryClosed,keep);
        closeIllegalStarts(graph,modes,boundaryClosed,keep);
        // Decision D12: in the cold start's rate seed, the generators the one-way estimate shows receiving start closed.
        graph=closeColdReceivingGenerators(graph,boundaryClosed,keep,checkpoint);
        // Hold F8: a mover held at its target flow into (or out of) a sealed pocket of junctions has no root.
        relieveSealedPockets(graph,modes,boundaryClosed,atShutoff);
        // The species this island can carry, and the trial states built from them, are read off the
        // connections this pass leaves open - so they are stated after the closure above and not
        // before it. A connection the closure has taken carries exactly zero by a row of its own,
        // for the whole solve, so it delivers nothing; seeding a vessel behind it with an entry
        // trace of a component only that connection could bring puts an unknown on a nonnegativity
        // boundary whose only root is zero, which is what used to refuse the whole Newton step. See
        // {@link #closeDeadHeads} and documentation/DEAD_HEADED_LINE.md.
        var reachable=reachableComponents(graph,boundaryClosed);
        var seeds=initialPhaseSeeds(graph,dt,checkpoint,reachable,modes,boundaryClosed);
        var seen=new HashSet<PassKey>();
        // Constant for this solve: every pass keys on the same graph identity and component support.
        var pipeIdentities=identities(graph);
        long[] nodeIds=nodeIds(graph);byte[] kinds=new byte[nodeIds.length];
        for(int i=0;i<kinds.length;i++)kinds[i]=(byte)graph.reservoirs().get(i).kind().ordinal();
        boolean[] componentMask=componentMask(graph);
        // Components this solve has reactivated, per node. Reactivation is monotone within one solve
        // - a later pass's seed may not undo it - which is what keeps the support half of the cycle
        // key monotone and the pass sequence finite; demotion happens at the next solve's seed.
        boolean[][] promoted=new boolean[seeds.size()][];
        // Decision D34 with A85: the phases (phaseCode bits) the trace rule removed at each node in this solve, by the
        // reconstruction's fold or by a phase correction whose flash folded them, and those a node's own split refused at a
        // converged point (the D26 amendment: its closure drove the TP answer's phase under the trace share). Monotone like
        // the reactivations above: the phase correction does not give such a phase back within this solve, and the next
        // solve decides from its own start state. Without it, a condensate that converges just below the trace share
        // is folded, the gas left behind is cooler by the condensate's latent heat, its flash condenses more than the share
        // again, and the pass sequence repeats (a humid vented tank: 1.0e-9 folded, 2.4e-9 given back).
        int[] foldedPhases=new int[seeds.size()];
        // The velocity cap's active set (decision A270): each connection's cap state, decided on the first pass's start point
        // and revised by this loop at converged points, like a device's mode ({@link Equations#capTransition}).
        int[] caps=new int[graph.pipes().size()];Arrays.fill(caps,CAP_START);
        // A line the previous point left on its cap starts there; every other is decided on this solve's start point.
        if(carryModes&&previousCaps.length==caps.length)for(int i=0;i<caps.length;i++)if(previousCaps[i]>=0)caps[i]=previousCaps[i];
        int maximumPasses=Math.min(512,16+2*graph.reservoirs().size()+2*graph.pipes().size());
        // Decision D54: the vessels this solve has found running dry within the step (a failed Newton drove what they hold to
        // nothing); each pass solves them as junctions holding their stock (emptyingGraph), null while there is none.
        // A vessel a converged pass has confirmed as emptying is not released again within the solve ({@link #keptEmptying}).
        final var original=graph;boolean[] emptying=null,emptied=new boolean[graph.reservoirs().size()];
        for(int pass=0;pass<maximumPasses;pass++) {
            checkpoint.run();SolverDiagnostics.count(SolverDiagnostics.activeSetPasses);
            graph=emptying==null?original:emptyingGraph(original,emptying);emptyings=emptying==null?null:emptyings(original,emptying,dt);
            for(int i=0;i<kinds.length;i++)kinds[i]=(byte)graph.reservoirs().get(i).kind().ordinal();
            // Decision D34: the phases a pass is laid out for are the flash's, decided here once per pass and never inside a
            // residual or a Jacobian probe; a seed taken from a converged point of an earlier pass may still hold a phase its
            // Newton drove below the trace share.
            seeds=foldedSeeds(graph,seeds);
            // A junction owns stock like a vessel: it keeps the undirected composition basis
            // {@link #reachableComponents} gives every node and its own state as its seed.
            // A dry vessel (decision D54: empty, nothing laid out for it yet) has its own code: it carries no unknowns.
            int[] phases=new int[seeds.size()];for(int i=0;i<phases.length;i++)phases[i]=dry(graph.reservoirs().get(i),seeds.get(i))?DRY:phaseCode(seeds.get(i));
            byte[] modeCodes=new byte[modes.size()];for(int i=0;i<modeCodes.length;i++)modeCodes[i]=(byte)modes.get(i).ordinal();
            var supports=supports(graph,seeds,reachable,promoted);
            var equations=new Equations(graph,dt,modes,caps,boundaryClosed,seeds,reachable,supports,pipeIdentities);
            System.arraycopy(equations.capDirections,0,caps,0,caps.length);
            // The active-set state is exactly what varies between passes, so the structure key is
            // also the cycle key: repeating one means the pass sequence cannot make progress. The
            // frozen junction donors belong to it for the same reason the modes do - they decide
            // which equations this pass states, and the pass loop revises them. So do the phase-port segments, which the
            // cycle key carries beside the structure (see {@link PassKey}).
            var structure=new WorkspaceKey(0,nodeIds,kinds,pipeIdentities,phases,componentMask,supportCodes(supports),modeCodes,boundaryClosed.clone(),equations.junctionDonorFirst);
            // The cap states are part of the cycle key, not of the workspace key: a capped row has the columns of the row it
            // replaces, and the workspaces are shared across cap states as they were when the cap was a branch of the row.
            if(!seen.add(new PassKey(structure,equations.segmentCodes(),foldedPhases.clone(),capCodes(caps))))throw new SparseNewton.Nonconvergence("Phase/device active-set cycle");
            var key=new WorkspaceKey(Double.doubleToLongBits(dt),nodeIds,kinds,pipeIdentities,phases,componentMask,structure.supports,modeCodes,structure.boundaryClosed,structure.junctionDonors);
            var workspace=workspaces.get(key);
            SolverDiagnostics.count(workspace==null?SolverDiagnostics.workspaceBuilds:SolverDiagnostics.workspaceReuses);
            if(workspace==null){if(workspaces.size()>=4)workspaces.remove(workspaces.keySet().iterator().next());var previous=structures.get(structure);workspace=previous==null?new SparseNewton.Workspace(ownership):previous.forkPreconditioner();workspaces.put(key,workspace);}
            if(structures.size()>=4&&!structures.containsKey(structure))structures.remove(structures.keySet().iterator().next());structures.put(structure,workspace);
            SparseNewton.Result numerical;
            // Small headspaces amplify inventory roundoff into pressure/flow errors; solve these
            // more tightly - but only a node that holds a finite inventory has a headspace at all.
            // A generator and a void carry a prescribed boundary state, and a junction a
            // normalized guess with no volume and no stock; none of them owns roundoff for a small
            // headspace to amplify, and none of them contributes an unknown or a row, so none may
            // tighten the tolerance every other node is then held to. A port does keep its say: it
            // is a finite reservoir held fixed for one instantaneous rate evaluation, and holding
            // a liquid-full pair of them at rest to 1e-10 kg/s is exactly what the tolerance buys.
            //
            // A filter block compiles to two junctions seeded from the island's first boundary
            // state, which on a water line is liquid-full, so every placed filter line demanded
            // 1e-10 of nodes that cannot reach it - a tank with a little water in it floors its
            // water saturation row near 1.03e-10 - and the island refined its step to nothing.
            //
            // Decision D54: only a vessel whose wall carries it has a headspace that amplifies anything. A slack vessel is at
            // the ambient whatever it holds, and a dry one holds no fluid.
            boolean smallHeadspace=false;
            for(int i=0;i<seeds.size();i++) {
                var node=graph.reservoirs().get(i);var kind=node.kind();
                if(kind!=PassiveNetwork.NodeKind.RESERVOIR&&kind!=PassiveNetwork.NodeKind.PORT||dry(node,seeds.get(i))||holdsNoFluid(node))continue;
                if(seeds.get(i).vaporVolume()/seeds.get(i).volume()<.01&&PhaseLayout.taut(seeds.get(i),node.inventory().volume())){smallHeadspace=true;break;}
            }
            // 1e-11 stalls on caloric cancellation in nearly liquid-full water with trace N2.
            // Keep two orders of margin to the unchanged 1e-8 full-equation gate; every final
            // reconstruction and the independent interval accuracy/accounting checks still run.
            double tolerance=acceptance==Acceptance.APPROXIMATE?1e-6:smallHeadspace?1e-10:1e-9;
            try{numerical=SparseNewton.solve(equations,equations.initial(),new SparseNewton.Settings(20,tolerance,1e-6,24),checkpoint,workspace);}
            catch(SparseNewton.Nonconvergence failure) {
                if(failure.lastVariables()==null)throw failure;
                double[] trialFlows=Arrays.copyOfRange(failure.lastVariables(),equations.edgeOffset,equations.edgeOffset+graph.pipes().size());
                // Owner decision D59 (4): a pump whose target or head limit pulled its suction below the vapour pressure of the
                // water it draws cavitates; the pass is solved again with it on its suction limit. Taken before the cap
                // corrections: the iterate stands in a vapour that does not exist, so the caps it would decide read nothing.
                if(cavitates(equations,failure.lastVariables(),modes,boundaryClosed,caps,tolerance))continue;
                // Decision A274: a pass that states the wrong cap flows can have no root inside the property domain, and the
                // failed Newton's last iterate shows which: a capped line whose drive there has fallen below its cap's
                // pressure drop is released (random seed 101 of the world defaults: a tank's outflow into a junction stayed
                // capped after the pump drawing from that junction was refused for the slice, and the Newton drove the
                // junction to 2 MPa pushing the stated flow out through a filter, against a drive reversed by 1.9 MPa);
                // otherwise, a line on its law that the iterate holds at its cap (the trial projection, A275, keeps its flow
                // there while its drive asks for more) is put on its cap (defaults seed 164: two lines through a gas-filled
                // junction stalled the pass at 0.15 at every step size wanting more than their caps). A line cannot carry
                // more than its cap, and a stated flow its ends cannot push is no state, so a Newton walking either went
                // through states that do not exist. The pass is solved again from its start. Taken before the phase
                // corrections below, which read the same iterate.
                if(equations.correctCaps(failure.lastVariables(),caps,tolerance))continue;
                // Decision D54: a vessel the failed iterate drained to nothing runs dry within the step. Its end state has no
                // contents, so the pass laid out with them has no root (every step size fails the same way); it is solved
                // again with the vessel as a junction holding its stock, which ends the step empty (emptyingGraph).
                var drained=equations.runsDry(failure.lastVariables(),emptying);
                if(drained!=null){emptying=drained;continue;}
                // A point the Newton never reached is deliberately not read as evidence about this
                // pass's frozen junction donors, nor about the junction basis those donors state,
                // unlike the converged tests further down. Measured on the filter block line:
                // turning a junction onto a diverged trial's flow sign hands it the tank's nitrogen
                // as a seeded trace nothing delivers, and every swept pressure lost an interval by
                // it - 400 kPa went from interval 8 back to 7, and from a tank settled at
                // 399999.99971 Pa to one at 399998.24 Pa. Widening the junction basis from the same
                // diverged trial is the same mistake with a bigger blast radius, and it is what
                // used to put the phantom trace back at every refinement level: the trial's flows
                // at a stalled point are -1.14e-6 kg/s on a line whose generator drives it
                // forwards, and reading them as backflow let the tank's nitrogen into both of the
                // filter's zero-holdup junctions; see documentation/JUNCTION_PHANTOM_TRACE.md.
                var changedSeeds=phaseCorrection(graph,equations.states(failure.lastVariables()),checkpoint,null,foldedPhases.clone());
                // Decision D11: a phase a vessel's ports draw at their full capacity leaves the vessel within the step; a
                // pass whose seed still holds it may have no root (see drainedSeeds).
                if(changedSeeds==null)changedSeeds=equations.drainedSeeds(checkpoint);
                if(changedSeeds==null)throw new SparseNewton.Nonconvergence(failure.getMessage()+"; active-set pass="+pass,failure.lastVariables(),failure.domainViolation());
                seeds=changedSeeds;continue;
            }
            double[] x=numerical.variables();var states=equations.states(x);double[] flows=new double[graph.pipes().size()],heads=new double[flows.length];
            // The converged Newton point is not phase-checked here: the reconstructed point below, which is what an accepted
            // step commits, is, and the two agree to the Newton tolerance, so checking both asks one question twice at a TP
            // flash per single-phase node each (review 8.7 (c) D: 43 % of the wall time of a flowing island at 0.1 s).
            // The same outer stability question the phase correction above answers for a whole phase,
            // asked per component of the frozen trace support: this converged point's own fugacity
            // coefficients decide whether an omitted phase is still a trace.
            if(reactivate(equations,states,promoted)>0){seeds=states;continue;}
            // Decision D54: an emptying vessel whose outlet pressure the converged point puts above the ambient does not run
            // dry (what reaches it is more than its outlets take at the ambient): it holds contents at the step's end and is
            // solved again as the vessel it is, unless an earlier converged pass of this solve has confirmed it emptying.
            if(emptying!=null){var kept=keptEmptying(states,emptying,emptied);if(kept!=null){emptying=kept;continue;}}
            boolean changed=false;int illegalDirection=-1,capChange=-1,capNext=CAP_OPEN,deviceChange=-1;FlowControl.Mode deviceNext=null;
            for(int i=0;i<flows.length;i++) {
                flows[i]=x[equations.edgeOffset+i];heads[i]=equations.controlOffsets[i]<0?0:x[equations.controlOffsets[i]]*1e5;
                if(boundaryClosed[i]||modes.get(i)==FlowControl.Mode.CLOSED){flows[i]=0;x[equations.edgeOffset+i]=0;}
                // Decision D54: a dry end has nothing to give; an outflow from it within the forbidden-direction tolerance
                // below is no flow ({@link #outOfDryEnd}).
                var pipe=graph.pipes().get(i);
                if(flows[i]!=0&&Math.abs(flows[i])<=1e-10&&outOfDryEnd(graph,pipe,flows[i])){flows[i]=0;x[equations.edgeOffset+i]=0;}
                // What the first (suction) end draws at this flow: its bulk at a BULK end, the priority stream at a phase port.
                var up=states.get(pipe.first());var suction=equations.drawn(i,0,equations.cachedTransport[pipe.first()],flows[i]);
                double rho=suction.density;var mode=modes.get(i);var next=mode;
                if(!boundaryAllowed(graph,pipe,flows[i])&&Math.abs(flows[i])>1e-10){if(illegalDirection<0)illegalDirection=i;continue;}
                if(boundaryClosed[i])continue;
                next=nextMode(equations,i,mode,flows[i],heads[i],states,up,suction,tolerance,atShutoff[i]);
                if(next!=mode&&deviceChange<0){deviceChange=i;deviceNext=next;}
                // A capped connection the point refutes is released before a new cap is stated (decision A272).
                if(capChange<0||caps[capChange]==CAP_OPEN&&caps[i]>=0){int cap=equations.capTransition(i,x,tolerance);if(cap!=caps[i]){capChange=i;capNext=cap;}}
            }
            // Decision A272: one active-set change per pass, a cap change first, then a device's mode, then a boundary
            // closure. A point where a line carries more than its cap, or states a cap flow its drive cannot push, is no point
            // a device may read its mode from (random seed 336: a compressor drawing 0.081 kg/s through a suction line choked
            // well below it turned between its head limit and its target, the line's cap never stated, until the pass
            // sequence cycled). A boundary closure is never taken back within a solve, and a forbidden direction can be the
            // symptom of a wrong cap as of a wrong device mode, so the reversible corrections come first.
            if(capChange>=0){caps[capChange]=capNext;changed=true;}
            else if(deviceChange>=0) {
                // A device that changes mode has its cap decided afresh on the next pass's start point.
                var mode=modes.get(deviceChange);modes.set(deviceChange,deviceNext);caps[deviceChange]=CAP_START;changed=true;
                if(deviceNext==FlowControl.Mode.CLOSED&&(mode==FlowControl.Mode.PUMP_HEAD_LIMIT||mode==FlowControl.Mode.PUMP_SUCTION_LIMIT))atShutoff[deviceChange]=true;
            }
            // A passive connection showing a direction its own boundary forbids, next to a device
            // that has just decided it cannot run in its current mode, is that device's symptom and
            // not an event of its own: a pump held above its shutoff head pushes its whole suction
            // line backwards, and the generator edge feeding it is the first thing that shows it.
            // The pass applies one active-set change, so whichever of the two is taken first
            // discards the other - and {@code boundaryClosed} is never cleared again within a solve,
            // so taking the symptom first pins the wrong active set for good: the closed suction
            // edge forces the pump's own flow to zero through the junction mass balance, the
            // reverse-flow test that would have closed the pump never fires again, and the pump is
            // left at its head limit on a junction sitting exactly on the upwind and
            // junction inflow-floor discontinuities of the former zero-holdup junction rows, where the Newton
            // cannot converge at any step size. Deciding the device first leaves the boundary
            // closure to a later pass, where it is applied only if the direction is still illegal
            // once every device runs in a mode it can actually hold.
            if(!changed&&illegalDirection>=0){closeBoundary(graph,illegalDirection,boundaryClosed,modes,caps);changed=true;}
            // Decision A272: a cap change re-solves the same pass from its own start (its seeds: what fills each connection,
            // which phases exist, the junctions' start states; and its start flows and phase-port segments): it changes which
            // equation states one flow, not the point the pass was laid out on. Re-seeded from the converged two-phase
            // junction, random seed 35's next pass stated the line's column 365 kg/m3 lighter, its drive fell 3.6 kPa below
            // the cap drop, and the cap left and entered again until the pass sequence cycled; started from the capped
            // pass's flows, a tank's released bottom outlet started at the 7.8 kg/s water cap it had been stating against a
            // drive that pushes 0.3 kg/s and stalled (LiquidFullTankOutletsTest).
            // A confirmed emptying (keptEmptying) was decided on the network this active set states; a cap, a device mode or a
            // closure changes what reaches the vessel, so it is decided again (world defaults 853: a placed tank's 0.68 kg,
            // confirmed emptying at 98.9 kPa, then a closure of a generator's connection put it at 113 kPa, filling).
            if(changed)Arrays.fill(emptied,false);
            if(changed&&capChange>=0){previousCaps=caps.clone();continue;}
            if(changed){seeds=states;previousPipes=pipeIdentities;previousNodeIds=nodeIds;previousInputStates=inputStates;previousFlows=flows.clone();previousHeads=heads.clone();previousModes=List.copyOf(modes);previousCaps=caps.clone();continue;}
            // Decision D11: the phase-port segments this pass froze must be the ones its own converged flows land in
            // (which phases a port draws at its capacity and which one carries the rest); otherwise the pass is solved
            // again, starting from these flows, under the segments they decide (see PhaseDraw). Like an active-set change,
            // and the cycle key carries the segments, so the sequence stays finite. An island without a phase port has none.
            if(equations.segmentsMoved(flows)){seeds=states;previousPipes=pipeIdentities;previousNodeIds=nodeIds;previousInputStates=inputStates;previousFlows=flows.clone();previousHeads=heads.clone();previousModes=List.copyOf(modes);previousCaps=caps.clone();continue;}
            for(int edge=0;edge<flows.length;edge++) {
                var pipe=graph.pipes().get(edge);var upstream=states.get(flows[edge]>=0?pipe.first():pipe.second());
                double cap=pipe.drawPort(flows[edge])==PassiveNetwork.PhasePort.BULK?massFlowLimit(pipe,upstream,PassiveNetwork.PhasePort.BULK)
                        :equations.capLimit(edge,flows[edge]>=0?0:1,equations.cachedTransport[flows[edge]>=0?pipe.first():pipe.second()]);
                if(Math.abs(flows[edge])>cap*(1+2e-8)+1e-12)throw new SparseNewton.Nonconvergence("Velocity constraint did not close");
            }
            // What each phase-port outflow drew, phase by phase, at these flows on this candidate: the reconstruction books it
            // and the transfer samples it (decision D11).
            var draws=equations.draws(flows,states);
            var projection=ConservativeTransport.reconstruct(graph,states,flows,heads,dt,model,checkpoint,transport,rateOnly,draws,equations.supports,emptyings);
            // Decision D54: a dry vessel this pass's flows reached holds what they delivered (booked at the ambient, closed
            // at its wall); the pass is solved again with it laid out from that state, so its end is the step's implicit
            // end like every vessel's.
            if(filled(equations,projection)){seeds=projection.states();continue;}
            // Hold class E3: a node this pass solved with no hydrocarbon phase, to which the booked flows deliver a
            // hydrocarbon its layout omitted, was restated by the reconstruction as its flash (ConservativeTransport
            // .repartition); the pass is solved again from it, as from any phase appearance. Before, that restatement
            // failed and every step size was refused.
            if(phaseAppeared(graph,states,projection.states())){seeds=projection.states();continue;}
            // Decision D34: the reconstruction folded a phase this pass solved (its booked share fell below the trace share),
            // so the pass is solved again from the folded states: the Newton, the reconstruction and the equation gate then
            // read the same phases.
            if(recordFolds(graph,states,projection.states(),foldedPhases)){SolverDiagnostics.count(SolverDiagnostics.traceFoldPasses);seeds=projection.states();continue;}
            var changedSeeds=phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases);if(changedSeeds!=null){seeds=changedSeeds;continue;}
            var reconstructed=x.clone();
            for(int node=0;node<projection.states().size();node++)if(equations.layout[node]!=null){var encoded=equations.layout[node].encode(projection.states().get(node));System.arraycopy(encoded,0,reconstructed,equations.offsets[node],encoded.length);}
            SolverDiagnostics.count(SolverDiagnostics.verificationResiduals);
            double maximumResidual=0;for(double residual:equations.residual(reconstructed))maximumResidual=Math.max(maximumResidual,Math.abs(residual));
            double gate=acceptance==Acceptance.FULL?1e-8:1e-6;
            if(maximumResidual>gate) {
                // Decision D13: a refused point gets one polish on a fresh Jacobian before the step is refused; see
                // {@link #polishRefusedPoint}. Reached only after the gate has refused, so every point it accepts is untouched.
                var polished=polishRefusedPoint(graph,equations,reconstructed,dt,modes,boundaryClosed,promoted,atShutoff,foldedPhases,tolerance,gate,checkpoint);
                if(polished!=null&&polished.closure()>=0) {
                    // The booked states show a drive the converged point did not state: a one-way end that would receive
                    // (random fuzz seed 71: a tank filled full of water stood 0.01 Pa above what its generator holds, the
                    // polish pushed that back into the generator, and the step was refused at every step size). The
                    // boundary is closed as at a converged point, and the pass is solved again from the reconstruction,
                    // the state each vessel holds.
                    closeBoundary(graph,polished.closure(),boundaryClosed,modes,caps);Arrays.fill(emptied,false);
                    seeds=projection.states();previousPipes=pipeIdentities;previousNodeIds=nodeIds;previousInputStates=inputStates;previousFlows=flows.clone();previousHeads=heads.clone();previousModes=List.copyOf(modes);previousCaps=caps.clone();continue;
                }
                if(polished!=null){numerical=polished.numerical();x=polished.variables();states=polished.states();System.arraycopy(polished.flows(),0,flows,0,flows.length);
                    System.arraycopy(polished.heads(),0,heads,0,heads.length);draws=polished.draws();
                    projection=polished.projection();reconstructed=polished.reconstructed();maximumResidual=polished.residual();}
            }
            if(maximumResidual>gate){SolverDiagnostics.count(SolverDiagnostics.equationGateRejections);throw new SparseNewton.Nonconvergence("Conservative reconstruction fails equation gate: "+maximumResidual);}
            if(acceptance==Acceptance.APPROXIMATE)checkApproximation(equations,reconstructed,projection.states(),flows,workspace,checkpoint,tolerance);
            // The ledger reads the island as it is: an emptying vessel is a vessel that ends empty, not the junction it stood
            // as in this pass.
            checkConservation(original,projection);
            previousPipes=pipeIdentities;previousNodeIds=nodeIds;previousInputStates=inputStates;previousFlows=flows.clone();previousHeads=heads.clone();previousModes=List.copyOf(modes);previousCaps=caps.clone();
            acceptedPipes=pipeIdentities;acceptedNodeIds=nodeIds;
            var acceptedModes=new ArrayList<>(modes);
            for(int edge=0;edge<flows.length;edge++) {
                var pipe=graph.pipes().get(edge);
                if(boundaryClosed[edge]&&pipe.control() instanceof FlowControl.Passive)acceptedModes.set(edge,FlowControl.Mode.CLOSED);
                // The accepted mode reports the cap state the solve ran in (decision A273): a connection on its cap stated
                // its cap flow.
                else if(caps[edge]>=0) {
                    acceptedModes.set(edge,switch(pipe.control()){case FlowControl.Passive ignored->FlowControl.Mode.VELOCITY_LIMITED;case FlowControl.Mover ignored->FlowControl.Mode.PUMP_VELOCITY_LIMIT;case FlowControl.PressureValve ignored->FlowControl.Mode.VALVE_VELOCITY_LIMIT;});
                }
            }
            if(SolverDiagnostics.ENABLED)SolverDiagnostics.count(SolverDiagnostics.solidMomentProjectionsAtAcceptedPoints,
                    equations.negativeSolidUnknowns(x)+equations.negativeSolidUnknowns(reconstructed));
            lastSolve=new LastSolve(equations,x);
            // A vessel that ran dry within the step holds nothing at its end and shows the ambient (decision D54).
            var endStates=projection.states();
            if(emptying!=null){var ambient=NetworkReduction.ambient(model);endStates=new ArrayList<>(endStates);for(int i=0;i<emptying.length;i++)if(emptying[i])endStates.set(i,ambient);}
            var accepted=new Result(endStates,flows,dt,numerical,acceptedModes,heads,projection.pumpWork(),projection.externalMoles(),projection.externalEnergy(),projection.inventories(),projection.boundaries(),PipeTransfer.sample(model,graph,projection.states(),flows,dt,draws),projection.filters());
            // The retained last solve keeps its equations and point, not their per-node decode caches (states, transport
            // rows, prepared temperature workspaces): a later reader re-decodes (review 8.7 (c) E2).
            equations.releaseDecodeCache();
            return accepted;
        }
        throw new SparseNewton.Nonconvergence("Device active-set limit");
    }
    /**
     * Closes connection {@code edge}'s boundary for the rest of the solve: a direction its one-way end forbids (a void
     * supplying, a generator receiving, a dry end giving). Decision A272: a closure changes what reaches a junction at its
     * ends, so a cap decided there was decided on a supply that is gone; those connections are decided again on the next
     * pass's start point (random seed 336: a line filling a tank kept the cap it took while a void fed its junction, and the
     * next pass drew the junction to 11 kPa to bring that cap down to the trickle left).
     */
    private static void closeBoundary(PassiveNetwork graph,int edge,boolean[] boundaryClosed,List<FlowControl.Mode> modes,int[] caps) {
        var pipe=graph.pipes().get(edge);boundaryClosed[edge]=true;
        if(!(pipe.control() instanceof FlowControl.Passive))modes.set(edge,FlowControl.Mode.CLOSED);
        for(int end:new int[]{pipe.first(),pipe.second()})if(graph.reservoirs().get(end).junction())
            for(int other=0;other<caps.length;other++){var link=graph.pipes().get(other);if(link.first()==end||link.second()==end)caps[other]=CAP_START;}
    }
    /** A point the equation gate refused, re-solved once and passing the gate, or the boundary its point shows must close
     * ({@code closure}, -1 for none); see {@link #polishRefusedPoint}. */
    private record Polished(SparseNewton.Result numerical,double[] variables,List<FluidThermodynamics.State> states,double[] flows,double[] heads,
                            double[][] draws,ConservativeTransport.Projection projection,double[] reconstructed,double residual,int closure) {
        /** A polished point that flows the way connection {@code edge}'s boundary forbids: no point to accept, a closure to take. */
        static Polished closing(int edge){return new Polished(null,null,null,null,null,null,null,null,Double.NaN,edge);}
    }
    /**
     * Decision D13 (PHASE_PORTS_REVIEW.md "Vent gate defect", "D13"): one re-solve of a point the reconstruction's equation
     * gate refused, from its reconstruction ({@code x}: the converged point with every node at its reconstructed state, the
     * flows the Newton's), on a fresh Jacobian of this step's own dt and state, at 1e-2 times the
     * pass's Newton tolerance; the re-solved point is checked exactly as the pass loop checks a converged point before it
     * accepts it (merged with decision D11: no trace reactivation due, no device mode change ({@link #nextMode}), no
     * illegal direction, the phase-port segments its flows land in the ones the pass froze, the velocity constraint in its
     * D11 form, the reconstruction with its own phase draws, the converged-point phase correction) and gated again.
     * Returns it only if all of that holds; a point that flows the way a boundary forbids returns that boundary
     * ({@link Polished#closing}), which the pass loop closes as at a converged point; otherwise null, and the step is refused
     * exactly as before.
     *
     * <p>Why it is needed: the Newton's last iterates usually run on a chord Jacobian inherited from another step (the
     * workspace keyed by dt forks the previous structure's factorization), so a balance row can close only to the
     * tolerance, which is stated over the node's total amount. The reconstruction books that node's exact amounts and
     * states the vessel at them (decision D36, G2), and a vessel whose gas is a small share of its amounts (a nitrogen
     * cushion over water: 0.1-0.2 %) sees that residual magnified by n_total/n_gas in its cushion's pressure, and so in the
     * rows of its connections - about 500 times, where the gate keeps one order of margin. A fresh Jacobian closes such a
     * row in one iteration (measured 1.6e-14 to 1.5e-12), so the polish passes where halving the step could lock the
     * controller (the D9 manometer ran out its substep limit). It starts from the reconstruction, the state the vessel
     * holds: the converged point itself may already meet the polish's tolerance (measured 9.2e-12), and a re-solve from
     * there returns it at zero iterations and sees nothing of the pressure its booked amounts give the cushion.
     *
     * <p>Only a refused point reaches this method, so every step the gate accepts is bitwise what it was; the fresh
     * workspace is local and never retained, so no later solve's preconditioner changes either.
     */
    private Polished polishRefusedPoint(PassiveNetwork graph,Equations equations,double[] x,double dt,List<FlowControl.Mode> modes,
                                        boolean[] boundaryClosed,boolean[][] promoted,boolean[] atShutoff,int[] foldedPhases,double tolerance,double gate,Runnable checkpoint) {
        SolverDiagnostics.count(SolverDiagnostics.equationGatePolishes);
        SparseNewton.Result numerical;
        try{numerical=SparseNewton.solve(equations,x,new SparseNewton.Settings(20,tolerance*1e-2,1e-6,24),checkpoint,new SparseNewton.Workspace(ownership));}
        catch(SparseNewton.Nonconvergence failure){return null;}
        double[] variables=numerical.variables();var states=equations.states(variables);
        // A trace the polished point would reactivate is an active-set change the pass loop would take (reactivate), not an
        // accepted point: asked on copies, so nothing of the pass's own state moves.
        if(model.traceTruncation().enabled())for(int node=0;node<states.size();node++) {
            var layout=equations.layout[node];
            if(layout==null||layout.singlePhaseComponentCount()==0)continue;
            boolean[] flags=promoted[node]==null?new boolean[model.hydrocarbon.componentCount()]:promoted[node].clone();
            if(layout.reactivate(states.get(node),model.traceTruncation(),flags)>0)return null;
        }
        double[] flows=new double[graph.pipes().size()],heads=new double[flows.length];
        for(int edge=0;edge<flows.length;edge++) {
            flows[edge]=variables[equations.edgeOffset+edge];heads[edge]=equations.controlOffsets[edge]<0?0:variables[equations.controlOffsets[edge]]*1e5;
            if(boundaryClosed[edge]||modes.get(edge)==FlowControl.Mode.CLOSED){flows[edge]=0;variables[equations.edgeOffset+edge]=0;}
            var pipe=graph.pipes().get(edge);
            if(flows[edge]!=0&&Math.abs(flows[edge])<=1e-10&&outOfDryEnd(graph,pipe,flows[edge])){flows[edge]=0;variables[equations.edgeOffset+edge]=0;}
            if(!boundaryAllowed(graph,pipe,flows[edge])&&Math.abs(flows[edge])>1e-10)return Polished.closing(edge);
            if(boundaryClosed[edge])continue;
            var suction=equations.drawn(edge,0,equations.cachedTransport[pipe.first()],flows[edge]);
            if(nextMode(equations,edge,modes.get(edge),flows[edge],heads[edge],states,states.get(pipe.first()),suction,tolerance,atShutoff[edge])!=modes.get(edge))return null;
            if(equations.capTransition(edge,variables,tolerance)!=equations.capDirections[edge])return null;
        }
        if(equations.segmentsMoved(flows))return null;
        for(int edge=0;edge<flows.length;edge++) {
            var pipe=graph.pipes().get(edge);var upstream=states.get(flows[edge]>=0?pipe.first():pipe.second());
            double cap=pipe.drawPort(flows[edge])==PassiveNetwork.PhasePort.BULK?massFlowLimit(pipe,upstream,PassiveNetwork.PhasePort.BULK)
                    :equations.capLimit(edge,flows[edge]>=0?0:1,equations.cachedTransport[flows[edge]>=0?pipe.first():pipe.second()]);
            if(Math.abs(flows[edge])>cap*(1+2e-8)+1e-12)return null;
        }
        var draws=equations.draws(flows,states);
        var projection=ConservativeTransport.reconstruct(graph,states,flows,heads,dt,model,checkpoint,transport,rateOnly,draws,equations.supports,emptyings);
        if(phaseAppeared(graph,states,projection.states())||phaseFolded(graph,states,projection.states())
                ||phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases.clone())!=null)return null;
        var reconstructed=variables.clone();
        for(int node=0;node<projection.states().size();node++)if(equations.layout[node]!=null){var encoded=equations.layout[node].encode(projection.states().get(node));System.arraycopy(encoded,0,reconstructed,equations.offsets[node],encoded.length);}
        SolverDiagnostics.count(SolverDiagnostics.verificationResiduals);
        double residual=0;for(double value:equations.residual(reconstructed))residual=Math.max(residual,Math.abs(value));
        if(residual>gate)return null;
        SolverDiagnostics.count(SolverDiagnostics.equationGatePolishesAccepted);
        return new Polished(numerical,variables,states,flows,heads,draws,projection,reconstructed,residual,-1);
    }
    /**
     * The device mode the pass loop moves connection {@code i} to from {@code mode} at a converged point ({@code flow},
     * {@code head}, the node {@code states}, the first node's state {@code up} and what its first end draws, {@code suction});
     * {@code mode} itself when the point holds it. The one statement of the pump and valve transitions, read by the pass
     * loop and by {@link #polishRefusedPoint}, which must accept a point only where the pass loop would.
     */
    private FlowControl.Mode nextMode(Equations equations,int i,FlowControl.Mode mode,double flow,double head,List<FluidThermodynamics.State> states,
                                      FluidThermodynamics.State up,Equations.Transport suction,double tolerance,boolean atShutoff) {
        var graph=equations.graph;var pipe=graph.pipes().get(i);double rho=suction.density;var next=mode;
        if(pipe.control() instanceof FlowControl.Mover pump) {
            double limit=riseLimit(pump,rho,states.get(pipe.first()).pressure()),margin=limit-demand(graph,pipe,states,rho);
            double band=shutoffBand(equations.pressureScales[i],tolerance);
            if(pump.targetVolumeFlow()==0)next=FlowControl.Mode.CLOSED;
            else if(mode==FlowControl.Mode.PUMP_TARGET&&pump.targetVolumeFlow()>pipe.minimumArea()*suction.velocityLimit*(1+1e-8))next=FlowControl.Mode.PUMP_HEAD_LIMIT;
            else if(mode==FlowControl.Mode.PUMP_TARGET&&head>limit+.01)next=FlowControl.Mode.PUMP_HEAD_LIMIT;
            else if(mode==FlowControl.Mode.PUMP_HEAD_LIMIT&&margin<band)next=FlowControl.Mode.CLOSED;
            else if(mode==FlowControl.Mode.PUMP_HEAD_LIMIT&&flow/rho>pump.targetVolumeFlow()*(1+1e-8))next=FlowControl.Mode.PUMP_TARGET;
            else if(mode==FlowControl.Mode.CLOSED&&!atShutoff&&margin>band)next=FlowControl.Mode.PUMP_HEAD_LIMIT;
            // Owner decision D59 (4), cavitation, the mirror of the head limit: a pump whose target or head limit holds its
            // suction below the vapour pressure of the water it draws goes on its suction limit; there its flow is what the
            // suction line delivers at that pressure, and it leaves when that is more than the target, when holding the
            // suction there needs more head than the pump has, or when the suction line delivers nothing at that pressure
            // (its source cannot lift the water to the pump: the pump runs dry and stands at its shutoff for the solve).
            else if((mode==FlowControl.Mode.PUMP_TARGET||mode==FlowControl.Mode.PUMP_HEAD_LIMIT)&&cavitationMargin(equations,i,states)<-band)next=FlowControl.Mode.PUMP_SUCTION_LIMIT;
            else if(mode==FlowControl.Mode.PUMP_SUCTION_LIMIT&&flow/rho>pump.targetVolumeFlow()*(1+1e-8))next=FlowControl.Mode.PUMP_TARGET;
            else if(mode==FlowControl.Mode.PUMP_SUCTION_LIMIT&&head>limit+.01)next=FlowControl.Mode.PUMP_HEAD_LIMIT;
            else if(mode==FlowControl.Mode.PUMP_SUCTION_LIMIT&&flow<-1e-10)next=FlowControl.Mode.CLOSED;
        }else if(pipe.control() instanceof FlowControl.PressureValve valve) {
            if(mode==FlowControl.Mode.VALVE_REGULATING&&flow<-1e-10)next=FlowControl.Mode.CLOSED;
            else if(mode==FlowControl.Mode.VALVE_REGULATING&&flow>equations.capLimit(i,0,equations.cachedTransport[pipe.first()])*(1+1e-8))next=FlowControl.Mode.VALVE_OPEN;
            else if(mode==FlowControl.Mode.VALVE_REGULATING&&head<-.01)next=FlowControl.Mode.VALVE_OPEN;
            else if(mode==FlowControl.Mode.VALVE_OPEN&&flow<-1e-10)next=FlowControl.Mode.CLOSED;
            else if(mode==FlowControl.Mode.VALVE_OPEN&&!graph.reservoirs().get(pipe.first()).fixed()&&up.pressure()<valve.targetPressure()-.01&&flow>1e-10)next=FlowControl.Mode.VALVE_REGULATING;
            else if(mode==FlowControl.Mode.CLOSED&&up.pressure()>valve.targetPressure()+.01&&head>.01)next=FlowControl.Mode.VALVE_REGULATING;
        }
        return next;
    }
    /**
     * Owner decision D59 (4): how far the suction end of connection {@code edge} stands above the vapour pressure of the
     * water its pump draws, read on {@code states} (Pa); positive infinity where the connection is not a liquid pump or the
     * pass laid its suction out with no free water (a compressor draws gas; a hydrocarbon liquid has no vapour pressure the
     * model states). The temperature is the state's, or the pass's seed's where the state stands outside the water data (a
     * failed iterate that flashed its suction to the triple point).
     */
    private double cavitationMargin(Equations equations,int edge,List<FluidThermodynamics.State> states) {
        var pipe=equations.graph.pipes().get(edge);int a=pipe.first();var seed=equations.seeds.get(a);
        if(!(pipe.control() instanceof FlowControl.Pump)||pipe.firstPort()==PassiveNetwork.PhasePort.VAPOR||!FluidThermodynamics.holdsPhase(seed,FluidThermodynamics.WATER))
            return Double.POSITIVE_INFINITY;
        var state=states.get(a);double vapour;
        try{vapour=model.saturationPressure(state.temperature());}catch(IllegalArgumentException outsideData){vapour=model.saturationPressure(seed.temperature());}
        return endPressure(equations.graph.reservoirs().get(a),pipe.firstPort(),state)-vapour;
    }
    /**
     * Owner decision D59 (4) on a failed Newton: the first pump at its target or head limit whose suction the last iterate
     * holds below the vapour pressure ({@link #cavitationMargin}, by more than {@link #shutoffBand}) is put on its suction
     * limit, its cap decided afresh; true if one was. The iterate went where the pump pulled it, and a suction below the
     * vapour pressure is a state the water does not have: there the pump cavitates. Like the cap corrections of a failed
     * iterate (A274), it changes which row states one connection and is part of the cycle key, so the sequence stays finite.
     */
    private boolean cavitates(Equations equations,double[] last,List<FlowControl.Mode> modes,boolean[] boundaryClosed,int[] caps,double tolerance) {
        List<FluidThermodynamics.State> states=null;
        for(int i=0;i<modes.size();i++) {
            var mode=modes.get(i);
            if(boundaryClosed[i]||mode!=FlowControl.Mode.PUMP_TARGET&&mode!=FlowControl.Mode.PUMP_HEAD_LIMIT||!(equations.graph.pipes().get(i).control() instanceof FlowControl.Pump))continue;
            if(states==null)try{states=equations.states(last);}catch(IllegalArgumentException outsideDomain){return false;}
            if(cavitationMargin(equations,i,states)<-shutoffBand(equations.pressureScales[i],tolerance)){modes.set(i,FlowControl.Mode.PUMP_SUCTION_LIMIT);caps[i]=CAP_START;return true;}
        }
        return false;
    }
    /** Value key over compact codes instead of rendered strings and boxed lists; the arrays belong
     * to the key from construction on, so a caller must hand over a snapshot of anything it mutates.
     * {@code supports} is the per-node, per-component phase support flattened in node order: it
     * decides how many unknowns and rows each node block has, so the sparsity pattern, the colouring,
     * the ordering and every retained factorization belong to it as much as to the phase code. */
    private record WorkspaceKey(long stepBits,long[] nodeIds,byte[] kinds,List<PassiveNetwork.Pipe.Identity> pipes,
                                int[] phases,boolean[] componentMask,byte[] supports,byte[] modes,boolean[] boundaryClosed,
                                boolean[] junctionDonors) {
        @Override public boolean equals(Object other) {
            return other instanceof WorkspaceKey key&&stepBits==key.stepBits&&Arrays.equals(nodeIds,key.nodeIds)
                    &&Arrays.equals(kinds,key.kinds)&&pipes.equals(key.pipes)&&Arrays.equals(phases,key.phases)
                    &&Arrays.equals(componentMask,key.componentMask)&&Arrays.equals(supports,key.supports)
                    &&Arrays.equals(modes,key.modes)&&Arrays.equals(boundaryClosed,key.boundaryClosed)
                    &&Arrays.equals(junctionDonors,key.junctionDonors);
        }
        @Override public int hashCode() {
            int hash=31*Long.hashCode(stepBits)+Arrays.hashCode(nodeIds);
            hash=31*(31*hash+Arrays.hashCode(kinds))+pipes.hashCode();
            hash=31*(31*hash+Arrays.hashCode(phases))+Arrays.hashCode(componentMask);
            hash=31*hash+Arrays.hashCode(supports);
            hash=31*(31*hash+Arrays.hashCode(modes))+Arrays.hashCode(boundaryClosed);
            return 31*hash+Arrays.hashCode(junctionDonors);
        }
    }
    /**
     * The cycle key of one active-set pass: its structure and the phase-port segments it froze ({@link PhaseDraw.Segment}).
     * A segment changes the values of a port's stream but no structural entry (a node any end of which draws a phase keeps
     * every column entry whatever it draws), so it is not part of the workspace key and a re-decided segment keeps the
     * workspace and its preconditioner; it is part of the cycle key, because the pass loop revises it, and a pass
     * sequence that returns to a segment it has already solved under cannot make progress. It also carries the phases the
     * trace rule has removed and the own splits have refused so far in the solve (decision D34, A85; the D26 amendment):
     * that record only grows, so a pass that returns to an earlier structure after a fold or a refusal is a pass under a
     * different phase-correction rule, not a cycle, and the sequence stays finite.
     */
    private record PassKey(WorkspaceKey structure,byte[] segments,int[] folded,byte[] caps) {
        @Override public boolean equals(Object other){return other instanceof PassKey key&&structure.equals(key.structure)&&Arrays.equals(segments,key.segments)&&Arrays.equals(folded,key.folded)&&Arrays.equals(caps,key.caps);}
        @Override public int hashCode(){return 31*(31*(31*structure.hashCode()+Arrays.hashCode(segments))+Arrays.hashCode(folded))+Arrays.hashCode(caps);}
    }
    /**
     * Every node's frozen per-component phase support for one pass, derived from that pass's seeds
     * and never from a Newton iterate, with the components this solve has already reactivated kept in
     * both phases. A fixed node owns no unknowns and gets none.
     */
    /** The cap states of a pass as codes for its cycle key ({@link PassKey}). */
    private static byte[] capCodes(int[] caps){byte[] codes=new byte[caps.length];for(int i=0;i<caps.length;i++)codes[i]=(byte)caps[i];return codes;}
    private PhaseSupport[][] supports(PassiveNetwork graph,List<FluidThermodynamics.State> seeds,boolean[][] reachable,boolean[][] promoted) {
        var supports=new PhaseSupport[seeds.size()][];
        for(int node=0;node<supports.length;node++)
            if(!graph.reservoirs().get(node).fixed()&&!dry(graph.reservoirs().get(node),seeds.get(node)))
                supports[node]=PhaseLayout.support(model,seeds.get(node),Arrays.copyOf(reachable[node],model.hydrocarbon.componentCount()),promoted[node],
                        graph.reservoirs().get(node).junction());
        return supports;
    }
    /**
     * Components whose omitted phase the converged point no longer supports omitting, marked for the
     * next pass; returns how many. The test runs on the Newton solution of the reduced system, where
     * the fugacity coefficients of both phases are converged and the equation of state has filled
     * {@code ln phi_i} for the omitted phase at infinite dilution, which is what the inequality needs.
     * Every node is swept before any pass is repeated, so one reactivation pass restores everything
     * the state asks for instead of one component at a time.
     */
    private int reactivate(Equations equations,List<FluidThermodynamics.State> states,boolean[][] promoted) {
        if(!model.traceTruncation().enabled())return 0;
        int components=model.hydrocarbon.componentCount(),restored=0;
        for(int node=0;node<states.size();node++) {
            var layout=equations.layout[node];
            if(layout==null||layout.singlePhaseComponentCount()==0)continue;
            boolean[] flags=promoted[node]==null?new boolean[components]:promoted[node];
            int added=layout.reactivate(states.get(node),model.traceTruncation(),flags);
            if(added>0){promoted[node]=flags;restored+=added;}
        }
        if(restored>0)SolverDiagnostics.count(SolverDiagnostics.traceReactivations,restored);
        return restored;
    }
    /** The support arrays as one flat byte array for the workspace and cycle keys. */
    private byte[] supportCodes(PhaseSupport[][] supports) {
        int components=model.hydrocarbon.componentCount();
        byte[] codes=new byte[supports.length*components];
        for(int node=0;node<supports.length;node++) {
            if(supports[node]==null){Arrays.fill(codes,node*components,(node+1)*components,(byte)-1);continue;}
            for(int c=0;c<components;c++)codes[node*components+c]=(byte)supports[node][c].ordinal();
        }
        return codes;
    }
    /** This graph's connections as the part of themselves the retained solver state may key on. */
    private static List<PassiveNetwork.Pipe.Identity> identities(PassiveNetwork graph) {
        var identities=new ArrayList<PassiveNetwork.Pipe.Identity>(graph.pipes().size());
        for(var pipe:graph.pipes())identities.add(pipe.identity());
        return List.copyOf(identities);
    }
    private static long[] nodeIds(PassiveNetwork graph) {
        long[] ids=new long[graph.reservoirs().size()];
        for(int i=0;i<ids.length;i++)ids[i]=graph.reservoirs().get(i).id();
        return ids;
    }
    /** Components any vessel or scheduled injection can supply; a Newton pass carries unknowns for
     * all of them, so an absent component can still arrive through a pipe. Depends on the graph
     * only, so it is evaluated once per solve rather than once per active-set pass. */
    private boolean[] componentMask(PassiveNetwork graph) {
        boolean[] mask=new boolean[model.hydrocarbon.componentCount()];
        for(var node:graph.reservoirs())if(node.kind()!=PassiveNetwork.NodeKind.VOID){var amounts=node.inventory().moles();for(int c=0;c<mask.length;c++)mask[c]|=amounts[c]>0;}
        for(var transfer:graph.scheduledTransfers())if(transfer instanceof ScheduledTransfer.Injection input){var amounts=input.molesPerSecond();for(int c=0;c<mask.length;c++)mask[c]|=amounts[c]>0;}
        return mask;
    }
    /** Online trust check against a contracting correction of the complete coupled equations, not separate flashes. */
    private void checkApproximation(Equations equations,double[] point,List<FluidThermodynamics.State> candidate,double[] flows,SparseNewton.Workspace workspace,Runnable checkpoint,double tolerance) {
        var estimate=SparseNewton.estimateCorrection(equations,point,checkpoint,workspace);var probe=estimate.probe();var corrected=equations.states(probe);
        double inflation=estimate.initialResidual()>1e-12?1/(1-Math.min(.5,estimate.probeResidual()/estimate.initialResidual())):1;
        if(phaseCorrection(equations.graph,corrected,checkpoint)!=null)throw new ApproximationAnchor.Rejected("Error probe changes a phase regime");
        for(int i=0;i<candidate.size();i++) {
            var a=candidate.get(i);var b=corrected.get(i);
            if(inflation*Math.abs(a.pressure()-b.pressure())>.001*b.pressure()+.1
                    ||inflation*Math.abs(a.temperature()-b.temperature())>.2
                    ||inflation*Math.abs(a.vaporVolume()/a.volume()-b.vaporVolume()/b.volume())>.002
                    ||inflation*Math.abs(a.liquidVolume()/a.volume()-b.liquidVolume()/b.volume())>.002
                    ||inflation*Math.abs(a.waterVolume()/a.volume()-b.waterVolume()/b.volume())>.002)
                throw new ApproximationAnchor.Rejected("Coupled property-error estimate exceeds the fallback trust profile");
        }
        for(int edge=0;edge<flows.length;edge++) {
            var pipe=equations.graph.pipes().get(edge);double q=probe[equations.edgeOffset+edge],floor=1e-10;
            for(int node:new int[]{pipe.first(),pipe.second()})if(!equations.graph.reservoirs().get(node).junction()&&!equations.graph.reservoirs().get(node).fixed())floor+=1e-9*candidate.get(node).mass()/equations.dt;
            if(inflation*Math.abs(q-flows[edge])>floor+.002*Math.abs(q)
                    ||Math.signum(q)!=Math.signum(flows[edge])&&Math.max(Math.abs(q),Math.abs(flows[edge]))>floor
                    ||!boundaryAllowed(equations.graph,pipe,q)&&Math.abs(q)>floor)
                throw new ApproximationAnchor.Rejected("Coupled flow-error estimate exceeds the fallback trust profile");
            double head=equations.controlOffsets[edge]<0?0:probe[equations.controlOffsets[edge]]*1e5;
            var up=corrected.get(pipe.first());var mode=equations.modes.get(edge);
            if(pipe.control() instanceof FlowControl.Mover pump&&mode!=FlowControl.Mode.INLET_WRONG_PHASE) {
                // A pump the solve closed at its own shutoff corner holds a head within
                // {@link #shutoffBand} of its maximum by construction, so the probe reads the
                // reopening test against that same band rather than a fixed 0.01 Pa.
                double band=shutoffBand(equations.pressureScales[edge],tolerance);
                double limit=riseLimit(pump,endDensity(up,pipe.firstPort()),up.pressure());
                if(q<-floor||mode!=FlowControl.Mode.CLOSED&&head>limit+.01||mode==FlowControl.Mode.CLOSED&&pump.targetVolumeFlow()>0&&head<limit-band)
                    throw new ApproximationAnchor.Rejected("Error probe changes pump feasibility");
            }else if(pipe.control() instanceof FlowControl.PressureValve valve) {
                if(q<-floor||mode==FlowControl.Mode.VALVE_REGULATING&&head<-.01
                        ||mode==FlowControl.Mode.VALVE_OPEN&&up.pressure()<valve.targetPressure()-.01&&q>floor
                        ||mode==FlowControl.Mode.CLOSED&&up.pressure()>valve.targetPressure()+.01&&head>.01)
                    throw new ApproximationAnchor.Rejected("Error probe changes valve feasibility");
            }
            if(Math.abs(q)>velocityCap(equations,edge,q,corrected)*(1+2e-8)+floor)throw new ApproximationAnchor.Rejected("Error probe violates velocity constraint");
        }
    }
    /** Physical species can traverse passive pipes in either direction, actuators only downstream.
     * Junction property guesses own no inventory and therefore cannot introduce a species. {@code closed} names the connections this pass has already pinned to a flow of exactly zero;
     * they carry no species in either direction. {@code null} is no closure at all. */
    private boolean[][] reachableComponents(PassiveNetwork graph,boolean[] closed) {
        int count=model.hydrocarbon.componentCount()+1,nodes=graph.reservoirs().size();
        var possible=new BitSet[nodes];var outgoing=new ArrayList<List<Integer>>(nodes);
        for(int i=0;i<nodes;i++) {
            possible[i]=new BitSet(count);outgoing.add(new ArrayList<>());
            var node=graph.reservoirs().get(i);
            if(node.kind()!=PassiveNetwork.NodeKind.VOID) {
                var amounts=node.inventory().moles();for(int c=0;c<count;c++)if(amounts[c]>0)possible[i].set(c);
            }
        }
        for(var transfer:graph.scheduledTransfers())if(transfer instanceof ScheduledTransfer.Injection injection) {
            var amounts=injection.molesPerSecond();for(int c=0;c<count;c++)if(amounts[c]>0)possible[injection.node()].set(c);
        }
        for(int edge=0;edge<graph.pipes().size();edge++) {var pipe=graph.pipes().get(edge);
            if(closed!=null&&closed[edge])continue;
            if(!(pipe.control() instanceof FlowControl.Mover pump)||pump.targetVolumeFlow()>0) {
                if(boundaryAllowed(graph,pipe,1))outgoing.get(pipe.first()).add(pipe.second());
                if(pipe.control() instanceof FlowControl.Passive&&boundaryAllowed(graph,pipe,-1))outgoing.get(pipe.second()).add(pipe.first());
            }
        }
        var queue=new ArrayDeque<Integer>();var queued=new boolean[nodes];
        for(int i=0;i<nodes;i++){queue.add(i);queued[i]=true;}
        while(!queue.isEmpty()) {
            int from=queue.remove();queued[from]=false;
            for(int to:outgoing.get(from)) {
                int before=possible[to].cardinality();possible[to].or(possible[from]);
                if(before!=possible[to].cardinality()&&!queued[to]){queued[to]=true;queue.add(to);}
            }
        }
        var result=new boolean[nodes][count];
        for(int i=0;i<nodes;i++) {
            // A hydraulically isolated junction retains only its arbitrary property guess.
            if(possible[i].isEmpty()&&graph.reservoirs().get(i).junction()) {
                var amounts=PhaseLayout.totalAmounts(graph.reservoirs().get(i).state());for(int c=0;c<count;c++)if(amounts[c]>0)possible[i].set(c);
            }
            for(int c=possible[i].nextSetBit(0);c>=0;c=possible[i].nextSetBit(c+1))result[i][c]=true;
        }
        return result;
    }
    /**
     * A trial point for one pass, per node.
     *
     * <p>A vessel is seeded as it always was: its own inventory, plus a quarter of its mass at
     * most of what is about to arrive, which is a correction to a stock it already owns. A
     * junction is seeded with its own state: its owned holdup is a small stock whose mixture the
     * backward-Euler rows move from where it is - with one addition, the water trace below.
     *
     * <p><b>Water trace in a junction (decision D10 of the phase-ports batch).</b> A junction whose reachable set holds
     * water ({@link #reachableComponents}) while its own state holds none is seeded with its state flashed at its own
     * temperature and pressure with a water entry trace of {@code 1e-12} of its total amount - the entry trace a vessel
     * gets for every reachable component it lacks. A node's {@link PhaseLayout} carries water (its water unknowns, its
     * water balance row and the junction's water mixing row) only when its seed holds water, and the hydrocarbon
     * components a junction can receive already enter through the reachable mask; water has no mask entry, so without the
     * trace a junction seeded dry that receives unsaturated water vapour - which does not change its phase code, so the
     * converged-point phase correction never reseeds it - is solved without the water the reconstruction then books into
     * it, and the equation gate refuses every step (1.3e-8 at 0.5 % water; PHASE_PORTS_REVIEW.md WP1 section 8). The
     * junction's owned inventory is untouched: the seed is a starting point and a basis, the rows start from the stock.
     * A junction whose flash with the trace leaves the property domain keeps its state (the trace is a basis, never a
     * reason to refuse a step).
     *
     * <p><b>A junction's first pass admits the phase that arrives (group L, PHASE_PORTS_REVIEW.md "Group L: a junction's
     * first step admits the phase that arrives (2026-10-13)", A300-A303).</b> The phases of a junction in a step are those
     * of what its mixing rows leave in it, and those follow from the step's inflow; see {@link #arrivingPhases}.
     */
    private List<FluidThermodynamics.State> initialPhaseSeeds(PassiveNetwork graph,double dt,Runnable checkpoint,boolean[][] reachable,
                                                              List<FlowControl.Mode> modes,boolean[] boundaryClosed) {
        int count=model.hydrocarbon.componentCount()+1,nodes=graph.reservoirs().size();
        double[] flows=initialMassFlows(graph);
        var seeds=new ArrayList<FluidThermodynamics.State>(nodes);
        for(int nodeIndex=0;nodeIndex<nodes;nodeIndex++) {
            var node=graph.reservoirs().get(nodeIndex);var available=reachable[nodeIndex];
            var state=node.state();
            if(node.junction()&&available[count-1]&&state.waterLiquid()+state.waterVapor()==0){seeds.add(waterTraceSeed(state,checkpoint));continue;}
            // A vessel that holds no fluid keeps its own state, which lays it out dry (decision D54, see dry); what arrives
            // in it is laid out by arrivingPhases.
            if(node.fixed()||node.junction()||holdsNoFluidVessel(node)){seeds.add(state);continue;}
            var n=PhaseLayout.totalAmounts(state);double total=Arrays.stream(n).sum();boolean changed=false;
            // This is only a trial guess. The accumulation equations still start from the exact original inventory.
            for(int i=0;i<count;i++)if(available[i]&&n[i]==0)changed=true;
            double weightedTemperature=state.mass()*state.temperature(),seedMass=state.mass();
            if(changed)for(var transfer:graph.scheduledTransfers())if(transfer instanceof ScheduledTransfer.Injection input&&input.node()==nodeIndex) {
                var rates=input.molesPerSecond();for(int i=0;i<count;i++)n[i]+=dt*rates[i];
            }
            if(changed)for(int edge=0;edge<flows.length;edge++) {
                var pipe=graph.pipes().get(edge);
                if(pipe.first()!=nodeIndex&&pipe.second()!=nodeIndex)continue;
                double flow=flows[edge];int receiver=flow>=0?pipe.second():pipe.first(),donor=flow>=0?pipe.first():pipe.second();
                if(receiver!=nodeIndex||!boundaryAllowed(graph,pipe,flow))continue;
                var incoming=graph.reservoirs().get(donor).state();double mass=Math.min(state.mass()*.25,dt*Math.abs(flow));var port=pipe.drawPort(flow);
                var feed=endMoles(incoming,port);double feedMass=endMass(incoming,port);
                for(int i=0;i<count;i++)n[i]+=mass*feed[i]/feedMass;
                weightedTemperature+=mass*incoming.temperature();seedMass+=mass;
            }
            for(int i=0;i<count;i++)if(available[i]&&n[i]==0)n[i]=total*1e-12;
            try{seeds.add(changed?model.flashTP(weightedTemperature/seedMass,state.pressure(),n,checkpoint).withSolidState(state.solids(),state.solidMoments()):state);}
            catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold hold){throw hold.at(graph.reservoirs().get(nodeIndex).id());}
        }
        return arrivingPhases(graph,seeds,modes,boundaryClosed,dt,checkpoint);
    }
    /** A dry junction state flashed at its own temperature and pressure with a water entry trace of 1e-12 of its total
     * amount; the state itself if that flash leaves the property domain. See {@link #initialPhaseSeeds}. */
    private FluidThermodynamics.State waterTraceSeed(FluidThermodynamics.State state,Runnable checkpoint) {
        var n=PhaseLayout.totalAmounts(state);n[n.length-1]=Arrays.stream(n).sum()*1e-12;
        try{return model.flashTP(state.temperature(),state.pressure(),n,checkpoint).withSolidState(state.solids(),state.solidMoments());}
        catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold outside){return state;}
    }
    /**
     * Group L (A300-A303): {@code seeds} with every junction laid out for the phases the step's inflow leaves in it. A
     * fitting full of air that water reaches is full of water a few milliseconds later; a step solve states that through
     * the junction's backward-Euler mixing rows, {@code (m_J/dt + Q) w = (m_J/dt) w_old + sum q w_e}, and its phases are
     * those of that mixture. A pass is laid out for its seeds' phases ({@link PhaseLayout}), so a junction seeded as its
     * held gas has no unknown for the water its rows must hold: the residual sits in its mixing and enthalpy rows, the
     * trial temperatures leave the data (water below 273.16 K) and the step halves to its floor (world defaults 745 under
     * the physical start: an 11 g air buffer receiving about 7 kg/s of water).
     *
     * <p>Each junction's mixture over the step is its held stock plus what its connections deliver at the pass's start
     * flows ({@link #startPoint}) over {@code dt}, each the stream its donor end draws ({@link #endMoles}), at the
     * mass-weighted temperature (the vessel seed's rule above) and the seed's pressure, flashed and folded as the pass folds
     * a junction (D34, D41). Where that mixture holds a phase the seed lacks, the seed becomes the mixture at the seed's
     * mass (a junction's amounts are a basis; its rows are intensive). A junction donor delivers its own updated seed, so a
     * phase reaches a run of junctions in one stage. A junction whose seed already holds every phase of its mixture keeps
     * its seed to the bit, as does every vessel that holds something. A rate-only solve holds a junction's composition at
     * its stock, so it lays out the stock's phases and this does nothing there.
     *
     * <p>A vessel that holds no fluid (decision D54) is laid out from what its connections deliver over {@code dt}, no fluid
     * held, beside whatever solids it holds: the delivered amounts themselves (a vessel's rows are its amounts) at the bag's own split (decision D26,
     * {@link #emptyBagSplit}), the equilibrium of what arrives with the enthalpy it brings, at the ambient pressure while
     * the bag is slack and at the capacity once what arrives overfills it, folded as a vessel: the root of the vessel's
     * own rows at the start flows. Where the engine answers nothing the network carries, the TP flash of the delivered
     * amounts at their mass-weighted temperature and the ambient pressure seeds it. That flash is an isothermal let-down,
     * which no throttled stream is: LNG arriving at 139 K and 1.14 MPa is, at 139 K and 1 atm, a vapour of several bag
     * volumes beside its CO2 frost, where the bag holds boiling LNG near 112 K, and a first pass laid out without that
     * liquid cooled the methane vapour to its 90.69 K floor at every step size (cold seeds 34 and 114). One nothing reaches
     * stays dry.
     */
    private List<FluidThermodynamics.State> arrivingPhases(PassiveNetwork graph,List<FluidThermodynamics.State> seeds,List<FlowControl.Mode> modes,
                                                           boolean[] boundaryClosed,double dt,Runnable checkpoint) {
        if(rateOnly||graph.reservoirs().stream().noneMatch(node->node.junction()&&!node.fixed()||holdsNoFluidVessel(node)))return seeds;
        int nodes=graph.reservoirs().size(),edges=graph.pipes().size();double[] flows=new double[edges];
        startPoint(graph,modes,boundaryClosed,identities(graph),null,flows,new double[edges],new boolean[edges]);
        List<FluidThermodynamics.State> result=null;
        for(int round=0;round<=nodes;round++) {
            boolean changed=false;
            for(int j=0;j<nodes;j++) {
                var node=graph.reservoirs().get(j);boolean empty=holdsNoFluidVessel(node);if(!(node.junction()&&!node.fixed()||empty))continue;
                var current=result==null?seeds:result;var seed=current.get(j);
                if(empty&&!dry(node,seed))continue;
                double[] n=node.inventory().moles().clone();double heldMass=molarMass(n),mass=heldMass,weighted=heldMass*node.state().temperature(),heat=0;boolean arrives=false;
                for(int edge=0;edge<flows.length;edge++) {
                    var pipe=graph.pipes().get(edge);double flow=flows[edge];
                    if(!(Math.abs(flow)>0)||(flow>=0?pipe.second():pipe.first())!=j||!boundaryAllowed(graph,pipe,flow))continue;
                    int donor=flow>=0?pipe.first():pipe.second();var stream=current.get(donor);var port=pipe.drawPort(flow);
                    double delivered=dt*Math.abs(flow),streamMass=endMass(stream,port);double[] moles=endMoles(stream,port);
                    for(int c=0;c<n.length;c++)n[c]+=delivered*moles[c]/streamMass;
                    mass+=delivered;weighted+=delivered*stream.temperature();arrives=true;
                    heat+=delivered*(endSpecificEnthalpy(stream,port)+GRAVITY*(graph.reservoirs().get(donor).elevation()-node.elevation()));
                }
                if(empty) {
                    if(!arrives||!(mass>0))continue;
                    FluidThermodynamics.State delivered;
                    try{delivered=model.folded(model.flashTP(weighted/mass,PhaseLayout.AMBIENT_PRESSURE,n,checkpoint),false);}
                    catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold outside){continue;}
                    // What arrives may itself be nothing at the bag's resolution: the vessel then stays dry.
                    if(PhaseLayout.bagVolume(delivered,node.inventory().volume())<=HOLDS_NOTHING*node.inventory().volume())continue;
                    var own=emptyBagSplit(node,weighted/mass,n,heat,checkpoint);
                    if(result==null)result=new ArrayList<>(seeds);
                    var arrived=own==null?delivered:own;var held=node.inventory().solids();
                    result.set(j,held.empty()?arrived:arrived.withSolids(held));changed=true;continue;
                }
                if(!arrives||!(heldMass>0))continue;
                for(int c=0;c<n.length;c++)n[c]*=seed.mass()/mass;
                FluidThermodynamics.State mixture;
                try{mixture=model.folded(model.flashTP(weighted/mass,seed.pressure(),n,checkpoint),true);}
                catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold outside){continue;}
                if((phaseCode(mixture)&~phaseCode(seed)&15)==0)continue;
                if(result==null)result=new ArrayList<>(seeds);
                result.set(j,mixture.withSolidState(seed.solids(),seed.solidMoments()));changed=true;
            }
            if(!changed)break;
        }
        return result==null?seeds:result;
    }
    /**
     * Decisions D54 and D26: an empty bag's own split of what arrives over a step, the amounts {@code n} with the enthalpy
     * {@code h} they bring (J, the network's datum, each stream's drop in potential included, as the vessel's energy row
     * books it). The engine's PH answer at the ambient pressure ({@code FluidThermodynamics.equilibriumPH}, under the
     * competition at the arrival's mass-weighted temperature {@code t}) holds the phases; the vessel's closure from it
     * ({@link InventoryEquilibrium#split}, the bag empty at the step's start, so the wall's work is booked from a zero
     * volume) is the equilibrium of that enthalpy at the ambient while the bag is slack and of its energy at the capacity
     * once what arrives overfills it; the PH answer itself where the closure leaves nothing. Folded as a vessel.
     * {@code null} where the engine answers nothing the network carries.
     */
    private FluidThermodynamics.State emptyBagSplit(PassiveNetwork.Reservoir node,double t,double[] n,double h,Runnable checkpoint) {
        try {
            var answer=model.equilibriumPH(t,PhaseLayout.AMBIENT_PRESSURE,h,n,checkpoint);
            if(answer==null)return null;
            var start=model.folded(answer,false);
            var own=InventoryEquilibrium.split(model,start,new PassiveNetwork.Inventory(node.inventory().volume(),n.clone(),h),0,checkpoint);
            return own==null?start:model.folded(own,false);
        }
        catch(IllegalArgumentException refused){return null;}
    }
    /** Value identity of two node-state lists (T, P, amounts, mass, volume, enthalpy). */
    private static boolean sameStates(List<FluidThermodynamics.State> a,List<FluidThermodynamics.State> b) {
        if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++) {
            var x=a.get(i);var y=b.get(i);if(x==y)continue;
            if(Double.doubleToLongBits(x.temperature())!=Double.doubleToLongBits(y.temperature())||Double.doubleToLongBits(x.pressure())!=Double.doubleToLongBits(y.pressure())
                    ||Double.doubleToLongBits(x.mass())!=Double.doubleToLongBits(y.mass())||Double.doubleToLongBits(x.volume())!=Double.doubleToLongBits(y.volume())
                    ||Double.doubleToLongBits(x.enthalpy())!=Double.doubleToLongBits(y.enthalpy())
                    ||Double.doubleToLongBits(x.waterLiquid())!=Double.doubleToLongBits(y.waterLiquid())||Double.doubleToLongBits(x.waterVapor())!=Double.doubleToLongBits(y.waterVapor())
                    ||!Arrays.equals(x.liquid(),y.liquid())||!Arrays.equals(x.vapor(),y.vapor()))return false;
        }
        return true;
    }
    /**
     * The balanced pressure seed of a cold junction: every junction of degree three or more joined by open passive
     * connections to nodes that hold their pressure over the solve, whose guess lies outside its neighbours' pressure
     * range, starts from the pressure at which its connections' estimated flows balance, and from the mixture those
     * flows deliver at that pressure. A property guess outside the range makes every flow of the star point the same way
     * and the Newton descend from a state no solution is near (the 7-of-32 static matrix of review 6.1). A starting guess
     * only: the junction's owned inventory is not touched.
     */
    private PassiveNetwork seedBoundaryJunctions(PassiveNetwork graph,Runnable checkpoint) {
        List<PassiveNetwork.Reservoir> nodes=null;
        for(int node=0;node<graph.reservoirs().size();node++) {
            var junction=graph.reservoirs().get(node);
            if(!junction.junction()||!junction.state().solids().empty())continue;
            double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;int degree=0;boolean eligible=true;
            for(var pipe:graph.pipes())if(pipe.first()==node||pipe.second()==node) {
                degree++;int other=pipe.first()==node?pipe.second():pipe.first();
                // A vessel neighbour counts as held while this structure is cold: with no rate solve per step, a step is the
                // only solve a cold junction ever sees, and a vessel's pressure moves by at most the state cap over it (review
                // 8.6 (b), run 85d). Only until a step of the structure has been accepted: at a hydrostatic rest the junction
                // pressure lies outside its neighbours' range and the seed would fire at every step (run 85b: 480 seeds,
                // 119040 flashes in one case).
                boolean held=graph.reservoirs().get(other).fixed()||graph.reservoirs().get(other).kind()==PassiveNetwork.NodeKind.RESERVOIR&&backwardEulerCold(graph);
                if(!held||!(pipe.control() instanceof FlowControl.Passive)
                        ||pipe.filter()!=null||pipe.blockedDirections()!=0){eligible=false;break;}
                double pressure=endPressure(graph.reservoirs().get(other),pipe.portAt(other),graph.reservoirs().get(other).state());
                low=Math.min(low,pressure);high=Math.max(high,pressure);
            }
            if(!eligible||degree<3||!(high>low))continue;
            double pressure=junction.state().pressure();
            if(pressure>low&&pressure<high)continue; // Keep an already interior/settled guess.
            var seed=balancedBoundarySeed(graph,node,low,high,checkpoint);
            if(nodes==null)nodes=new ArrayList<>(graph.reservoirs());
            nodes.set(node,new PassiveNetwork.Reservoir(junction.id(),junction.elevation(),seed,
                junction.kind(),junction.inventory()));
        }
        return nodes==null?graph:new PassiveNetwork(nodes,graph.pipes(),graph.scheduledTransfers());
    }

    /** The seed itself: eight sweeps of a 36-step bisection on the junction pressure for zero net estimated inflow, then a
     * 30-step bisection on the temperature at which the delivered mixture has the delivered specific enthalpy. */
    private FluidThermodynamics.State balancedBoundarySeed(PassiveNetwork graph,int node,double low,double high,Runnable checkpoint) {
        var junction=graph.reservoirs().get(node);var seed=junction.state();
        double stock=PhaseLayout.sum(PhaseLayout.totalAmounts(seed));
        for(int sweep=0;sweep<8;sweep++) {
            checkpoint.run();
            var nodes=new ArrayList<>(graph.reservoirs());
            nodes.set(node,new PassiveNetwork.Reservoir(junction.id(),junction.elevation(),seed,junction.kind(),junction.inventory()));
            var trial=new PassiveNetwork(nodes,graph.pipes());
            double lo=low,hi=high;
            for(int k=0;k<36;k++) {
                double pressure=lo+(hi-lo)*.5,net=0;
                for(var pipe:graph.pipes())if(pipe.first()==node||pipe.second()==node) {
                    double flow=initialMassFlow(trial,pipe,pipe.first()==node?pressure:endPressure(trial,pipe,pipe.first()),
                        pipe.second()==node?pressure:endPressure(trial,pipe,pipe.second()));
                    if(boundaryAllowed(trial,pipe,flow))net+=pipe.first()==node?-flow:flow;
                }
                if(net>0)lo=pressure;else hi=pressure;
            }
            double pressure=lo+(hi-lo)*.5,mass=0,heat=0,tLow=Double.POSITIVE_INFINITY,tHigh=0;
            var amounts=new double[model.hydrocarbon.componentCount()+1];
            for(var pipe:graph.pipes())if(pipe.first()==node||pipe.second()==node) {
                double flow=initialMassFlow(trial,pipe,pipe.first()==node?pressure:endPressure(trial,pipe,pipe.first()),
                    pipe.second()==node?pressure:endPressure(trial,pipe,pipe.second()));
                int donor=flow>=0?pipe.first():pipe.second();
                if(donor==node||!boundaryAllowed(trial,pipe,flow))continue;
                var source=nodes.get(donor);double rate=Math.abs(flow);var port=pipe.portAt(donor);
                var n=endMoles(source.state(),port);double sourceMass=endMass(source.state(),port);
                for(int c=0;c<n.length;c++)amounts[c]+=rate*n[c]/sourceMass;
                mass+=rate;heat+=rate*(endSpecificEnthalpy(source.state(),port)+GRAVITY*(source.elevation()-junction.elevation()));
                tLow=Math.min(tLow,source.state().temperature());tHigh=Math.max(tHigh,source.state().temperature());
            }
            if(!(mass>0))return seed;
            double total=PhaseLayout.sum(amounts);for(int c=0;c<amounts.length;c++)amounts[c]*=stock/total;
            // The streams' own window, 20 K either side, inside the numerical box (owner rule of 2026-10-16: no data range
            // narrows a seed).
            double lower=Math.max(tLow-20,NumericalBox.MINIMUM_TEMPERATURE),upper=Math.min(tHigh+20,NumericalBox.MAXIMUM_TEMPERATURE);
            for(int k=0;k<30;k++) {
                double t=lower+(upper-lower)*.5;
                var point=model.flashTP(t,pressure,amounts,checkpoint);
                if(point.enthalpy()/point.mass()<heat/mass)lower=t;else upper=t;
            }
            seed=model.flashTP(lower+(upper-lower)*.5,pressure,amounts,checkpoint);
        }
        return seed;
    }

    /** Rounds of {@link #fixedPointStarts}: one, measured against the bound of eight on the quick check (fuzz seeds 1-200:
     * 176 / 187 against 175 / 186 clean, random / world defaults; decision D54, logs es15 and es16). */
    private static final int FIXED_POINT_ROUNDS=1;
    /**
     * Owner decisions D54 (nothing at the mint) and D55 (the guess from the connected suppliers): the start guess of a
     * minted island's junctions, the island-wide fixed point's last round reached. A junction holds nothing a placement
     * put there, so the guess has no physical claim: its buffer's 0.05 s relaxation (D25) washes it out, and the guess
     * only has to be a point the first step's Newton starts near, of a fluid a source supplies. Round 0 is
     * {@link #supplierSeeds}; then, each round: one rate solve of the port graph with every junction's composition held
     * at the current iterate, on a fresh solver (a cold rate solve, as the first interval's cold seed is: the solver's own
     * laws, columns, one-way rules, D12 closures and device active sets); each junction then becomes the flow-weighted
     * mixture of the streams that solve drives into it (a vessel's or boundary's end stream, a junction donor's current
     * mixture; amounts and enthalpy per kilogram, the enthalpy with its height gain), flashed at its solved pressure; a
     * junction nothing flows into keeps its composition at its solved pressure. It stops when no junction's mass fractions
     * move by more than 1e-9 and no pressure by more than 1e-9 of itself, after {@link #FIXED_POINT_ROUNDS} rounds, or at
     * a round whose rate solve fails (or leaves the property domain), and returns the last round it completed: round 0
     * when the first one fails. Per-kilogram states, one per node, null for every node that is not a junction.
     */
    List<FluidThermodynamics.State> fixedPointStarts(PassiveNetwork graph) {
        int nodes=graph.reservoirs().size(),count=model.hydrocarbon.componentCount()+1;
        Runnable none=()->{};
        var states=supplierSeeds(graph,none);
        if(graph.pipes().isEmpty())return states;
        try {
            for(int round=0;round<FIXED_POINT_ROUNDS;round++) {
                var trial=new ArrayList<PassiveNetwork.Reservoir>(nodes);
                for(int i=0;i<nodes;i++){var node=graph.reservoirs().get(i);trial.add(node.junction()?new PassiveNetwork.Reservoir(node.id(),node.elevation(),states.get(i),PassiveNetwork.NodeKind.JUNCTION):node);}
                var held=PassiveNetwork.sizeJunctionHoldups(new PassiveNetwork(trial,graph.pipes(),graph.scheduledTransfers()),model);
                var rate=new PassiveStepSolver(model).portRate(held,none,Acceptance.FULL);
                if(rate==null)return states;
                double[] q=rate.massFlows();
                // Per junction: amounts per kilogram and specific enthalpy of what it holds, iterated over the junctions.
                double[][] y=new double[nodes][];double[] h=new double[nodes],inflow=new double[nodes],tLow=new double[nodes],tHigh=new double[nodes];
                for(int i=0;i<nodes;i++)if(graph.reservoirs().get(i).junction()) {
                    var s=states.get(i);var n=PhaseLayout.totalAmounts(s);y[i]=new double[count];for(int c=0;c<count;c++)y[i][c]=n[c]/s.mass();h[i]=s.enthalpy()/s.mass();
                }
                for(int sweep=0;sweep<=nodes+200;sweep++) {
                    double change=0;
                    for(int j=0;j<nodes;j++) {
                        var junction=graph.reservoirs().get(j);if(!junction.junction())continue;
                        double total=0,heat=0,lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;double[] amounts=new double[count];
                        for(int e=0;e<q.length;e++) {
                            var pipe=graph.pipes().get(e);double flow=q[e];if(flow==0)continue;
                            int donor=flow>0?pipe.first():pipe.second(),receiver=flow>0?pipe.second():pipe.first();
                            if(receiver!=j||!boundaryAllowed(graph,pipe,flow))continue;
                            double rate1=Math.abs(flow);var source=graph.reservoirs().get(donor);double specific,temperature;double[] per;
                            if(source.junction()){per=y[donor];specific=h[donor];temperature=states.get(donor).temperature();}
                            else{var port=pipe.portAt(donor);var s=source.state();var n=endMoles(s,port);double m=endMass(s,port);per=new double[count];for(int c=0;c<count;c++)per[c]=n[c]/m;specific=endSpecificEnthalpy(s,port);temperature=s.temperature();}
                            for(int c=0;c<count;c++)amounts[c]+=rate1*per[c];
                            heat+=rate1*(specific+GRAVITY*(source.elevation()-junction.elevation()));total+=rate1;
                            lo=Math.min(lo,temperature);hi=Math.max(hi,temperature);
                        }
                        inflow[j]=total;if(!(total>0))continue;
                        tLow[j]=lo;tHigh[j]=hi;
                        for(int c=0;c<count;c++){double v=amounts[c]/total;change=Math.max(change,Math.abs(v-y[j][c])*model.molecularWeight(c));y[j][c]=v;}
                        double specific=heat/total;change=Math.max(change,Math.abs(specific-h[j])/Math.max(1,Math.abs(specific)));h[j]=specific;
                    }
                    if(change<=1e-14)break;
                }
                boolean moved=false;var next=new ArrayList<FluidThermodynamics.State>(states);
                for(int j=0;j<nodes;j++) {
                    if(!graph.reservoirs().get(j).junction())continue;
                    var old=states.get(j);double pressure=rate.states().get(j).pressure();FluidThermodynamics.State state;
                    if(!(inflow[j]>0))state=model.flashTP(old.temperature(),pressure,PhaseLayout.totalAmounts(old),none);
                    else {
                        double lower=Math.max(tLow[j]-20,NumericalBox.MINIMUM_TEMPERATURE),upper=Math.min(tHigh[j]+20,NumericalBox.MAXIMUM_TEMPERATURE);
                        for(int k=0;k<60&&upper-lower>1e-9;k++) {
                            double t=.5*(lower+upper);var point=model.flashTP(t,pressure,y[j].clone(),none);
                            if(point.enthalpy()/point.mass()<h[j])lower=t;else upper=t;
                        }
                        state=model.flashTP(.5*(lower+upper),pressure,y[j].clone(),none);
                    }
                    var a=PhaseLayout.totalAmounts(old);var b=PhaseLayout.totalAmounts(state);
                    for(int c=0;c<count&&!moved;c++)moved=Math.abs(b[c]*model.molecularWeight(c)/state.mass()-a[c]*model.molecularWeight(c)/old.mass())>1e-9;
                    moved|=Math.abs(state.pressure()-old.pressure())>1e-9*Math.abs(old.pressure());
                    next.set(j,state);
                }
                states=next;
                if(!moved)return states;
            }
        } catch(SparseNewton.Nonconvergence|IllegalArgumentException refused){return states;} // a rejected state: the numerical box, a phase hold
        return states;
    }
    /**
     * Owner decision D55, round 0 of {@link #fixedPointStarts}: a junction's start guess is what its connected suppliers
     * would send it. A supplier is a node that is not a junction and holds something (a vessel holding contents, a
     * generator, a port); a void and a dry end (a vessel that holds no fluid among them) send nothing ({@link #boundaryAllowed}). Each
     * supplier joined to a junction sends the stream its end draws ({@link #endMoles}, as the rate estimate reads it) at
     * the flow its static drive gives against the ambient ({@link #supplyFlow}); a junction is the flow-weighted mixture
     * of what reaches it (amounts per kilogram, enthalpy per kilogram with its height gain), flashed at the pressure where
     * the start estimate's flows into and out of it balance ({@link #balancePressure}). A junction no supplier drives into
     * takes it hop by hop from the junctions that have it (a breadth-first sweep, each hop the mixture of the neighbours
     * seeded before it, weighted by the flow that seeded them; a vessel that holds no fluid passes the sweep through). A
     * junction neither reaches (every drive towards it points away, as at a rest, or a vessel holding contents lies
     * between) takes the streams of the suppliers joined to it, one share per supplier, and the sweep runs again from
     * those. The ambient is the pressure of an empty node, never a fluid: a junction no source reaches (an island that
     * holds no stock) keeps the state it was built with as a placeholder, and nothing moves there
     * ({@link NetworkReduction#atRest}).
     *
     * <p>A mover's suction line is seeded first, from the stream it draws (owner's fix A of F2, 2026-10-16): the junctions
     * its supply walk crosses ({@link InletPhase#walk}: the device's own junction and the run of junctions beyond it) take
     * the stream of the supplier the walk ends at, as that end draws it ({@link #endStream}): the supplier's own phase at
     * its own temperature and pressure. That is already a state, so it is neither flashed nor moved to a balance pressure.
     * A mover's target row converts its volume flow at its suction junction's density, so that junction must hold what
     * the mover draws: a pump under a vented LNG tank kept the 298 K nitrogen placeholder (1.1 kg/m3 for 447 kg/m3), its
     * first rate solve moved 0.006 kg/s for 2.24 kg/s, and the suction line closed on deposition at every interval start.
     */
    private List<FluidThermodynamics.State> supplierSeeds(PassiveNetwork graph,Runnable checkpoint) {
        int nodes=graph.reservoirs().size(),count=model.hydrocarbon.componentCount()+1;
        var states=new ArrayList<FluidThermodynamics.State>(nodes);
        for(var node:graph.reservoirs())states.add(node.junction()?node.state():null);
        double[][] amounts=new double[nodes][count];double[] mass=new double[nodes],heat=new double[nodes],tLow=new double[nodes],tHigh=new double[nodes];
        boolean[] seeded=new boolean[nodes],drawn=new boolean[nodes];double[] weight=new double[nodes];
        for(int e=0;e<graph.pipes().size();e++) {
            if(!(graph.pipes().get(e).control() instanceof FlowControl.Mover))continue;
            var walk=InletPhase.walk(graph,e);
            if(walk.via()<0||graph.reservoirs().get(walk.node()).junction())continue;
            boolean supplies=true;
            for(int k=0;k<walk.links().length&&supplies;k++){var link=graph.pipes().get(walk.links()[k]);supplies=boundaryAllowed(graph,link,link.first()==walk.donors()[k]?1:-1);}
            var supplier=graph.reservoirs().get(walk.node());var last=graph.pipes().get(walk.via());
            var seed=supplies?endStream(supplier.state(),last.portAt(walk.node())):null;
            if(seed==null)continue;
            double q=supplyFlow(graph,last,walk.node());if(!(q>0))q=1;
            for(int k=-1;k<walk.links().length-1;k++) {
                int j=k<0?graph.pipes().get(e).first():walk.donors()[k];
                if(seeded[j])continue;
                states.set(j,seed);seeded[j]=drawn[j]=true;weight[j]=q;
            }
        }
        for(boolean driven:new boolean[]{true,false}) {
            Arrays.fill(mass,0);Arrays.fill(heat,0);Arrays.fill(tLow,Double.POSITIVE_INFINITY);Arrays.fill(tHigh,Double.NEGATIVE_INFINITY);
            for(var row:amounts)Arrays.fill(row,0);
            for(var pipe:graph.pipes())for(int end=0;end<2;end++) {
                int supplier=end==0?pipe.first():pipe.second(),junction=end==0?pipe.second():pipe.first();
                var s=graph.reservoirs().get(supplier);
                // A supplier whose end is dry (one holding no fluid, dryEnd) sends nothing: both flows below are zero.
                if(!graph.reservoirs().get(junction).junction()||seeded[junction]||s.junction())continue;
                double q=driven?supplyFlow(graph,pipe,supplier):boundaryAllowed(graph,pipe,end==0?1:-1)?1:0;
                if(!(q>0))continue;
                var port=pipe.portAt(supplier);double[] n=endMoles(s.state(),port);double m=endMass(s.state(),port);
                for(int c=0;c<count;c++)amounts[junction][c]+=q*n[c]/m;
                heat[junction]+=q*(endSpecificEnthalpy(s.state(),port)+GRAVITY*(s.elevation()-graph.reservoirs().get(junction).elevation()));
                mass[junction]+=q;tLow[junction]=Math.min(tLow[junction],s.state().temperature());tHigh[junction]=Math.max(tHigh[junction],s.state().temperature());
            }
            for(int j=0;j<nodes;j++)if(mass[j]>0&&seedAt(states,j,amounts[j],mass[j],heat[j],tLow[j],tHigh[j],PhaseLayout.AMBIENT_PRESSURE,checkpoint)){seeded[j]=true;weight[j]=mass[j];}
            // The sweep: hop by hop from the junctions seeded so far; a vessel that holds no fluid passes it through.
            for(boolean grew=true;grew;) {
                grew=false;var next=new boolean[nodes];
                Arrays.fill(mass,0);Arrays.fill(heat,0);Arrays.fill(tLow,Double.POSITIVE_INFINITY);Arrays.fill(tHigh,Double.NEGATIVE_INFINITY);
                for(var row:amounts)Arrays.fill(row,0);
                for(int j=0;j<nodes;j++) {
                    if(!graph.reservoirs().get(j).junction()||seeded[j])continue;
                    var z=graph.reservoirs().get(j).elevation();
                    for(int k:sweepNeighbours(graph,j)) {
                        if(!seeded[k])continue;var s=states.get(k);double w=weight[k];
                        double[] n=PhaseLayout.totalAmounts(s);for(int c=0;c<count;c++)amounts[j][c]+=w*n[c]/s.mass();
                        heat[j]+=w*(s.enthalpy()/s.mass()+GRAVITY*(graph.reservoirs().get(k).elevation()-z));mass[j]+=w;
                        tLow[j]=Math.min(tLow[j],s.temperature());tHigh[j]=Math.max(tHigh[j],s.temperature());
                    }
                }
                for(int j=0;j<nodes;j++)if(mass[j]>0&&seedAt(states,j,amounts[j],mass[j],heat[j],tLow[j],tHigh[j],PhaseLayout.AMBIENT_PRESSURE,checkpoint)){next[j]=grew=true;weight[j]=mass[j];}
                for(int j=0;j<nodes;j++)seeded[j]|=next[j];
            }
        }
        // The seeds' pressure: each seeded junction where the start estimate's flows into and out of it balance
        // ({@link #balancePressure}), over the seeds of the sweep before, its mixture kept. A drawn stream keeps its own.
        var trial=new ArrayList<>(graph.reservoirs());
        for(int sweep=0;sweep<8;sweep++) {
            for(int j=0;j<nodes;j++)if(seeded[j]){var node=graph.reservoirs().get(j);trial.set(j,new PassiveNetwork.Reservoir(node.id(),node.elevation(),states.get(j),PassiveNetwork.NodeKind.JUNCTION));}
            var network=new PassiveNetwork(trial,graph.pipes());boolean moved=false;
            for(int j=0;j<nodes;j++) {
                if(!seeded[j]||drawn[j])continue;var s=states.get(j);double pressure=balancePressure(network,j);
                if(Math.abs(pressure-s.pressure())>1e-9*s.pressure()
                        &&seedAt(states,j,PhaseLayout.totalAmounts(s),s.mass(),s.enthalpy(),s.temperature(),s.temperature(),pressure,checkpoint))moved=true;
            }
            if(!moved)break;
        }
        return states;
    }
    /**
     * Owner decision D55: the pressure of junction {@code j} at which the start estimate's flows into and out of it
     * ({@link #initialMassFlow}: each connection against its other end's own pressure, on the column its rule states, as
     * far as {@link #boundaryAllowed} lets it) balance, bisected between the lowest and highest of its neighbours' end
     * pressures referred to its height through either end's column, within the numerical box. A junction is a node of
     * the pipework, so its pressure is what its connections' flows make it, never the ambient's: seeded at the ambient a
     * liquid junction stood 9.8 kPa above a 1 m spill and 240 kPa below its two generators, and its first Newton did not
     * converge (logs es36).
     */
    private double balancePressure(PassiveNetwork graph,int j) {
        var junction=graph.reservoirs().get(j);double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        for(var pipe:graph.pipes())if(pipe.first()==j||pipe.second()==j) {
            int k=pipe.first()==j?pipe.second():pipe.first();var other=graph.reservoirs().get(k);
            double end=endPressure(graph,pipe,k),dz=junction.elevation()-other.elevation();
            for(double rho:new double[]{endDensity(other.state(),pipe.portAt(k)),endDensity(junction.state(),pipe.portAt(j))}) {
                low=Math.min(low,end-rho*GRAVITY*dz);high=Math.max(high,end-rho*GRAVITY*dz);
            }
        }
        low=Math.max(low,NumericalBox.MINIMUM_PRESSURE);high=Math.min(high,NumericalBox.MAXIMUM_PRESSURE);
        if(!(high>low))return junction.state().pressure();
        for(int k=0;k<60&&high-low>1e-9*high;k++) {
            double pressure=.5*(low+high),net=0;
            for(var pipe:graph.pipes())if(pipe.first()==j||pipe.second()==j) {
                double flow=initialMassFlow(graph,pipe,pipe.first()==j?pressure:endPressure(graph,pipe,pipe.first()),pipe.second()==j?pressure:endPressure(graph,pipe,pipe.second()));
                if(boundaryAllowed(graph,pipe,flow))net+=pipe.first()==j?-flow:flow;
            }
            if(net>0)low=pressure;else high=pressure;
        }
        return .5*(low+high);
    }
    /** The junctions one hop from junction {@code j} for {@link #supplierSeeds}' sweep: joined to it by a connection, or
     * through one or more vessels that hold no fluid ({@link #holdsNoFluid}: none of their own to send). */
    private static List<Integer> sweepNeighbours(PassiveNetwork graph,int j) {
        var found=new ArrayList<Integer>();var seen=new HashSet<Integer>();var open=new ArrayDeque<Integer>();open.add(j);seen.add(j);
        while(!open.isEmpty()) {
            int at=open.poll();
            for(var pipe:graph.pipes()) {
                int other=pipe.first()==at?pipe.second():pipe.second()==at?pipe.first():-1;
                if(other<0||!seen.add(other))continue;
                var node=graph.reservoirs().get(other);
                if(node.junction())found.add(other);else if(holdsNoFluid(node))open.add(other);
            }
        }
        Collections.sort(found);return found;
    }
    /** {@link #supplierSeeds}' flash of junction {@code j}'s mixture (amounts and enthalpy summed over {@code mass}) at
     * {@code pressure}, per kilogram: the temperature at which the mixture has the mixed enthalpy, bisected within 20 K of
     * its streams' temperatures inside the numerical box (owner rule of 2026-10-16: no data range narrows the window, so a
     * cold stream carrying a species below its data, LNG with CO2 dissolved at 111.6 K, is seeded like any other). False,
     * the junction left as it was, where a flash is refused. A mover's suction line does not come here (see above). */
    private boolean seedAt(List<FluidThermodynamics.State> states,int j,double[] amounts,double mass,double heat,double tLow,double tHigh,double pressure,Runnable checkpoint) {
        double[] y=new double[amounts.length];for(int c=0;c<y.length;c++)y[c]=amounts[c]/mass;
        double lower=Math.max(tLow-20,NumericalBox.MINIMUM_TEMPERATURE),upper=Math.min(tHigh+20,NumericalBox.MAXIMUM_TEMPERATURE);
        try {
            for(int k=0;k<60&&upper-lower>1e-9;k++) {
                double t=.5*(lower+upper);var point=model.flashTP(t,pressure,y.clone(),checkpoint);
                if(point.enthalpy()/point.mass()<heat/mass)lower=t;else upper=t;
            }
            states.set(j,model.flashTP(.5*(lower+upper),pressure,y.clone(),checkpoint));
        } catch(IllegalArgumentException refused){return false;} // a rejected state: the numerical box, a phase hold
        return true;
    }
    /**
     * The flow (kg/s, at least zero) supplier {@code supplier} sends into the junction at the other end of {@code pipe}
     * before anything is solved, as the start estimate reads a connection ({@link #initialMassFlow}): a mover's suction
     * flow out of its suction end; otherwise the supplier's end pressure over the ambient, less the column of its own end
     * stream up to the junction, through the connection's loss, capped by the velocity limit. Zero where that direction is
     * refused ({@link #boundaryAllowed}) or the drive points back.
     */
    private double supplyFlow(PassiveNetwork graph,PassiveNetwork.Pipe pipe,int supplier) {
        boolean first=pipe.first()==supplier;
        if(!boundaryAllowed(graph,pipe,first?1:-1))return 0;
        var s=graph.reservoirs().get(supplier);var port=pipe.portAt(supplier);
        if(pipe.control() instanceof FlowControl.Mover pump)
            return first?Math.min(suctionMassFlow(pump.targetVolumeFlow(),s.state(),port),massFlowLimit(pipe,s.state(),port)):0;
        double rho=endDensity(s.state(),port),mu=endViscosity(s.state(),port);
        double drive=endPressure(s,port,s.state())-PhaseLayout.AMBIENT_PRESSURE-rho*GRAVITY*(graph.reservoirs().get(first?pipe.second():pipe.first()).elevation()-s.elevation());
        if(!(drive>0))return 0;
        double lo=0,hi=1;while(pipe.pressureDrop(hi,rho,mu)<drive&&hi<1e6)hi*=2;
        for(int j=0;j<50;j++){double mid=(lo+hi)/2;if(pipe.pressureDrop(mid,rho,mu)>drive)hi=mid;else lo=mid;}
        return Math.min((lo+hi)/2,massFlowLimit(pipe,s.state(),port));
    }
    /** The mass of a conserved amount vector, water last. */
    private double molarMass(double[] amounts) {
        double mass=0;for(int c=0;c<amounts.length;c++)mass+=amounts[c]*model.molecularWeight(c);
        return mass;
    }
    /**
     * The whole island's connection flows as the states this solve starts at imply them, with
     * every chain of zero-holdup junctions carrying one flow.
     *
     * <p>{@link #initialMassFlow} answers one connection at a time, against the two pressures at
     * its ends. On a vessel that is the right question. Across a zero-holdup junction it is not:
     * the junction stores nothing, so the connections on either side of it carry the <em>same</em>
     * flow, and the junction's own pressure - which is what the per-connection answer reads - is
     * an output of the previous solve carrying that solve's roundoff. On a settled line the
     * pressures along the chain differ by micropascals and the per-connection answer is noise: at
     * 150 kPa it said the tank was pushing -7.2e-7 kg/s back into the filter's outlet junction
     * while the filter was pushing +1.6e-5 kg/s forward into the same junction, a pattern no
     * junction can be in. Everything a pass states about a junction - which species it mixes, what
     * that mixture is, which end of each connection donates into it - is read off this point, so
     * stating it from a point like that is what left the tank's nitrogen on a junction nothing was
     * feeding; see {@code documentation/JUNCTION_PHANTOM_TRACE.md}.
     *
     * <p>So a maximal run of degree-two junctions joined by passive connections is contracted to
     * one resistance between the two nodes at its ends, which do own their pressures, and the
     * single flow that resistance passes is laid on every connection in it. An actuator is never
     * contracted through, because its own setpoint and not the chain's resistance decides what it
     * passes; a junction of degree three or more is not contracted either, because its split needs
     * the solve and not an estimate.
     */
    private double[] initialMassFlows(PassiveNetwork graph) {
        int edges=graph.pipes().size(),nodes=graph.reservoirs().size();
        double[] q=new double[edges];
        for(int edge=0;edge<edges;edge++)q[edge]=initialMassFlow(graph,graph.pipes().get(edge));
        int[] degree=new int[nodes];boolean[] actuated=new boolean[nodes];
        for(var pipe:graph.pipes()) {
            degree[pipe.first()]++;degree[pipe.second()]++;
            if(!(pipe.control() instanceof FlowControl.Passive)){actuated[pipe.first()]=true;actuated[pipe.second()]=true;}
        }
        boolean[] interior=new boolean[nodes];boolean any=false;
        for(int i=0;i<nodes;i++)any|=interior[i]=graph.reservoirs().get(i).junction()&&degree[i]==2&&!actuated[i];
        if(!any)return q;
        int[][] nodeEdges=new int[nodes][];
        for(int i=0;i<nodes;i++)nodeEdges[i]=new int[degree[i]];
        Arrays.fill(degree,0);
        for(int edge=0;edge<edges;edge++) {
            var pipe=graph.pipes().get(edge);
            nodeEdges[pipe.first()][degree[pipe.first()]++]=edge;nodeEdges[pipe.second()][degree[pipe.second()]++]=edge;
        }
        boolean[] taken=new boolean[edges];
        for(int edge=0;edge<edges;edge++) {
            if(taken[edge])continue;
            var pipe=graph.pipes().get(edge);
            if(!interior[pipe.first()]&&!interior[pipe.second()])continue;
            // Walk out of this connection in both directions until a node that owns a pressure.
            var chain=new ArrayList<Integer>();var order=new ArrayList<Integer>();
            order.add(pipe.first());chain.add(edge);order.add(pipe.second());taken[edge]=true;
            for(int end=0;end<2;end++) {
                int at=end==0?pipe.first():pipe.second(),from=edge;
                while(interior[at]) {
                    int next=nodeEdges[at][0]==from?nodeEdges[at][1]:nodeEdges[at][0];
                    if(taken[next])break;
                    taken[next]=true;var step=graph.pipes().get(next);
                    int beyond=step.first()==at?step.second():step.first();
                    if(end==0){chain.addFirst(next);order.addFirst(beyond);}else{chain.add(next);order.add(beyond);}
                    at=beyond;from=next;
                }
            }
            int start=order.getFirst(),finish=order.getLast();
            if(start==finish||interior[start]||interior[finish])continue;
            var a=graph.reservoirs().get(start);var b=graph.reservoirs().get(finish);
            // The run stands in what its end ports draw, at their ends' driving pressures.
            var startPort=graph.pipes().get(chain.getFirst()).portAt(start);var finishPort=graph.pipes().get(chain.getLast()).portAt(finish);
            double pa=endPressure(a,startPort,a.state()),pb=endPressure(b,finishPort,b.state());
            // The run's static drive states each link's column as that link's flow row does, on the link's own ends: a run
            // that rises over a hump and falls beyond it stands in what fills each leg, never in one end-to-end column.
            double column=0;
            for(int position=0;position<chain.size();position++) {
                var link=graph.pipes().get(chain.get(position));int u=order.get(position),v=order.get(position+1);
                var nu=graph.reservoirs().get(u);var nv=graph.reservoirs().get(v);
                var portU=link.portAt(u);var portV=link.portAt(v);
                double first=endDensity(nu.state(),portU),second=endDensity(nv.state(),portV),dz=nv.elevation()-nu.elevation();
                if(!donates(graph,link,u))first=second;else if(!donates(graph,link,v))second=first;
                double difference=endPressure(nu,portU,nu.state())-endPressure(nv,portV,nv.state());
                column+=(columnFromFirst(first,second,difference,dz,difference>=0)?first:second)*GRAVITY*dz;
            }
            double driving=pa-pb-column;
            // The loss reads the stream of the end the drive leaves, as for one connection ({@link #initialMassFlow}).
            var source=(driving>=0?a:b).state();var sourcePort=driving>=0?startPort:finishPort;
            double rho=endDensity(source,sourcePort),mu=endViscosity(source,sourcePort);
            double lo=0,hi=1;
            while(chainPressureDrop(graph,chain,hi,rho,mu)<Math.abs(driving)&&hi<1e6)hi*=2;
            for(int j=0;j<50;j++) {
                double mid=(lo+hi)/2;
                if(chainPressureDrop(graph,chain,mid,rho,mu)>Math.abs(driving))hi=mid;else lo=mid;
            }
            double magnitude=(lo+hi)/2;
            var donor=graph.reservoirs().get(driving>=0?start:finish).state();var donorPort=driving>=0?startPort:finishPort;
            for(int link:chain)magnitude=Math.min(magnitude,massFlowLimit(graph.pipes().get(link),donor,donorPort));
            double flow=Math.copySign(magnitude,driving);
            for(int position=0;position<chain.size();position++) {
                var link=graph.pipes().get(chain.get(position));
                q[chain.get(position)]=link.first()==order.get(position)?flow:-flow;
            }
        }
        return q;
    }
    /**
     * The connection flows and actuator heads a pass with this active set starts from, which is
     * the whole of {@link Equations#buildInitial}'s edge half.
     *
     * <p>It lives here rather than in {@link Equations} because the pass reads it twice: once to
     * lay it into its own initial point, and once - before the equations exist - in
     * {@link #closeIllegalStarts}, which reads the direction each connection starts in.
     *
     * <p>{@code seeds} may be {@code null} before this solve has any, which only a regulating
     * valve reads, and no pass can start in that mode.
     */
    private void startPoint(PassiveNetwork graph,List<FlowControl.Mode> modes,boolean[] boundaryClosed,
                            List<PassiveNetwork.Pipe.Identity> pipeIdentities,List<FluidThermodynamics.State> seeds,
                            double[] flows,double[] heads,boolean[] headSet) {
        boolean warmFlow=graph.reservoirs().stream().anyMatch(node->node.junction()||node.fixed())
                ||graph.pipes().stream().anyMatch(pipe->phaseCode(graph.reservoirs().get(pipe.first()).state())!=phaseCode(graph.reservoirs().get(pipe.second()).state()));
        // At a job start the previous point is the committed interval's endpoint; see {@link #replayStart}.
        boolean previousAvailable=previousPipes.equals(pipeIdentities)&&Arrays.equals(previousNodeIds,nodeIds(graph))&&previousFlows.length==graph.pipes().size();
        // A rate-only solve's warm start is only as good as the state it was recorded at: it is a cold start's solve, or
        // a start-of-step guard's, whose previous flows belong to another point in time, and a rate solve warm-started
        // from cap-level flows of seconds ago stalls in the junction rows at rest (review 7.7). It uses them only when
        // they were recorded at value-identical input states (a re-solve of the same point, or a later pass of it).
        if(rateOnly&&!sameStates(previousInputStates,inputStates))previousAvailable=false;
        // Built once, and only when this point has no history to start from; see
        // {@link #initialMassFlows}.
        double[] estimate=warmFlow&&!previousAvailable?initialMassFlows(graph):null;
        for(int i=0;i<graph.pipes().size();i++) {
            var pipe=graph.pipes().get(i);var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
            if(boundaryClosed[i]||modes.get(i)==FlowControl.Mode.CLOSED)continue;
            if(modes.get(i)==FlowControl.Mode.PUMP_TARGET){flows[i]=suctionMassFlow(((FlowControl.Mover)pipe.control()).targetVolumeFlow(),a.state(),pipe.firstPort());continue;}
            // A connection whose pump is holding its limit starts from a flow that limit can
            // produce, never from one it has already refused; see {@link #headLimitMassFlow}.
            // A point already on the limit - the previous pass, or the previous accepted step
            // of a settled island - carries its own flow forward as before.
            // A pump the carry has just offered its head limit back from CLOSED starts from the limit's own flow and head,
            // not from the closed point's (zero flow and the head the closed pump held), a head the pass will not have.
            // From that closed head a long step's pass chose the discharge tank's bulk density for the column and ended
            // every 5 s step past the shutoff, so the pump re-closed for good 12.3 kPa short (review 8.8 (c)).
            boolean reopened=previousModes.size()==graph.pipes().size()&&previousModes.get(i)==FlowControl.Mode.CLOSED;
            if(modes.get(i)==FlowControl.Mode.PUMP_HEAD_LIMIT&&pipe.control() instanceof FlowControl.Mover pump
                    &&(reopened||!(previousAvailable&&previousHeads[i]<=riseLimit(pump,endDensity(a.state(),pipe.firstPort()),a.state().pressure())))) {
                flows[i]=headLimitMassFlow(graph,pipe,pump);
                heads[i]=riseLimit(pump,endDensity(a.state(),pipe.firstPort()),a.state().pressure())/1e5;headSet[i]=true;
                continue;
            }
            if(previousAvailable){
                // A flow the connection's boundary forbids (into a generator, out of a void, a blocked way) is no state to
                // start from, however small: the pass loop tolerates one below 1e-10 kg/s at a converged point, and carried
                // into the next step a Newton that converges without an iteration kept it there for good. It starts at rest.
                flows[i]=boundaryAllowed(graph,pipe,previousFlows[i])?previousFlows[i]:0;heads[i]=previousHeads[i]/1e5;headSet[i]=true;
                if(modes.get(i)==FlowControl.Mode.VALVE_REGULATING&&pipe.control() instanceof FlowControl.PressureValve valve) {
                    double predictedDrop=a.state().pressure()-(seeds==null?a.state():seeds.get(pipe.first())).pressure();
                    if(predictedDrop>0)flows[i]*=Math.clamp((a.state().pressure()-valve.targetPressure())/predictedDrop,0,1);
                    double rho=endDensity(a.state(),pipe.firstPort());
                    heads[i]=(endPressure(a,pipe.firstPort(),a.state())-endPressure(b,pipe.secondPort(),b.state())-rho*GRAVITY*(b.elevation()-a.elevation())
                            -pipe.pressureDrop(flows[i],rho,endViscosity(a.state(),pipe.firstPort())))/1e5;
                }
                continue;
            }
            if(!warmFlow)continue;
            // Decision D54: a dry end gives nothing, so an estimate out of one starts at rest (A190's rule for a carried flow).
            flows[i]=outOfDryEnd(graph,pipe,estimate[i])?0:estimate[i];
        }
        if(!hasSolids(graph)&&graph.pipes().stream().noneMatch(pipe->pipe.filter()!=null))return;
        int nodes=graph.reservoirs().size();int[] degree=new int[nodes];
        for(var pipe:graph.pipes()){degree[pipe.first()]++;degree[pipe.second()]++;}
        int[][] nodeEdges=new int[nodes][];
        for(int i=0;i<nodes;i++)nodeEdges[i]=new int[degree[i]];
        Arrays.fill(degree,0);
        for(int edge=0;edge<graph.pipes().size();edge++) {
            var pipe=graph.pipes().get(edge);
            nodeEdges[pipe.first()][degree[pipe.first()]++]=edge;nodeEdges[pipe.second()][degree[pipe.second()]++]=edge;
        }
        boolean[] known=new boolean[graph.pipes().size()];
        for(int i=0;i<known.length;i++)known[i]=modes.get(i)==FlowControl.Mode.PUMP_TARGET||boundaryClosed[i]||modes.get(i)==FlowControl.Mode.CLOSED;
        for(int pass=0;pass<known.length;pass++) {
            boolean changed=false;
            for(int node=0;node<nodes;node++)if(graph.reservoirs().get(node).junction()) {
                int missing=-1;double net=0;boolean usable=true;
                for(int edge:nodeEdges[node]) {
                    if(!known[edge]){if(missing>=0){usable=false;break;}missing=edge;continue;}
                    var pipe=graph.pipes().get(edge);double q=flows[edge];boolean receiving=node==(q>=0?pipe.second():pipe.first());
                    double factor=receiving&&pipe.filter()!=null?1-solidShare(graph.reservoirs().get(q>=0?pipe.first():pipe.second()).state(),pipe.drawPort(q)):1;
                    net+=receiving?Math.abs(q)*factor:-Math.abs(q);
                }
                if(!usable||missing<0)continue;
                var pipe=graph.pipes().get(missing);double q=pipe.first()==node?net:-net;
                if(node==(q>=0?pipe.second():pipe.first())&&pipe.filter()!=null){var donor=graph.reservoirs().get(q>=0?pipe.first():pipe.second()).state();q/=Math.max(1e-12,1-solidShare(donor,pipe.drawPort(q)));}
                if(boundaryAllowed(graph,pipe,q)){flows[missing]=q;known[missing]=true;changed=true;}
            }
            if(!changed)break;
        }
    }
    private static double chainPressureDrop(PassiveNetwork graph,List<Integer> chain,double flow,double rho,double mu) {
        double drop=0;for(int link:chain)drop+=graph.pipes().get(link).pressureDrop(flow,rho,mu);
        return drop;
    }
    private double initialMassFlow(PassiveNetwork graph,PassiveNetwork.Pipe pipe) {
        return initialMassFlow(graph,pipe,endPressure(graph,pipe,pipe.first()),endPressure(graph,pipe,pipe.second()));
    }
    private double initialMassFlow(PassiveNetwork graph,PassiveNetwork.Pipe pipe,double firstPressure,double secondPressure) {
        var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
        if(pipe.control() instanceof FlowControl.Mover pump)
            return Math.min(suctionMassFlow(pump.targetVolumeFlow(),a.state(),pipe.firstPort()),massFlowLimit(pipe,a.state(),pipe.firstPort()));
        double driving=staticDrive(graph,pipe,firstPressure-secondPressure);
        // The loss reads the stream of the end the drive leaves, as the row's loss reads its flow's donor.
        var donor=(driving>=0?a:b).state();var donorPort=driving>=0?pipe.firstPort():pipe.secondPort();
        double rho=endDensity(donor,donorPort),mu=endViscosity(donor,donorPort);
        double lo=0,hi=1;while(pipe.pressureDrop(hi,rho,mu)<Math.abs(driving)&&hi<1e6)hi*=2;
        for(int j=0;j<50;j++){double mid=(lo+hi)/2;if(pipe.pressureDrop(mid,rho,mu)>Math.abs(driving))hi=mid;else lo=mid;}
        return Math.copySign(Math.min((lo+hi)/2,massFlowLimit(pipe,donor,pipe.drawPort(driving))),driving);
    }
    /**
     * The static driving pressure of one passive connection from end {@code a} (drawn through {@code aPort}) to end {@code b}
     * (a contracted run sums its links' columns the same way, {@link #initialMassFlows}), given
     * their end pressures' {@code difference}, with the column {@link #columnFromFirst} states: the start estimate's
     * drive. Its own start sign, where both columns are admissible, is the sign of {@code difference} (the estimate is
     * the start point, so it has no earlier start to read): the row then reads the flow this drive produces and states the
     * same column. An end that may not donate into the connection has no column of its own ({@link #donates}).
     */
    private double staticDrive(PassiveNetwork graph,PassiveNetwork.Pipe pipe,double difference) {
        var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
        double first=endDensity(a.state(),pipe.firstPort()),second=endDensity(b.state(),pipe.secondPort()),dz=b.elevation()-a.elevation();
        if(!donates(graph,pipe,pipe.first()))first=second;else if(!donates(graph,pipe,pipe.second()))second=first;
        return difference-(columnFromFirst(first,second,difference,dz,difference>=0)?first:second)*GRAVITY*dz;
    }
    /**
     * Owner decision D59 (5), t28: whether end {@code node} of {@code pipe} may fill the connection, that is, whether the
     * connection may carry flow out of that end ({@link #boundaryAllowed}). A void only receives, a generator's far end can
     * never send into it, a blocked way and a dry end (decision D54) send nothing: such an end's fluid never stands in the
     * connection, so its column is not one the connection can have; the connection stands in what the other end sends.
     * This is the one statement of that fact for every static column (the row's {@link Equations#columnEnds}, the start
     * estimate's {@link #staticDrive} and a run's in {@link #initialMassFlows}); before, only a dry end was read so, and a
     * void line whose junction stood below the ambient was given the void's air column, its flow started out of the void
     * on the cap, and the pass loop closed it, where its water column drains into the void (world defaults 244).
     */
    static boolean donates(PassiveNetwork graph,PassiveNetwork.Pipe pipe,int node) {
        return boundaryAllowed(graph,pipe,node==pipe.first()?1:-1);
    }
    /**
     * The one column rule of a vertical connection: whether its static head is stated in the first end's column
     * ({@code first}, the density of what that end's port draws) rather than the second's, given the end pressures'
     * {@code difference} (actuator included) and {@code dz = z_second - z_first}. The flow row states it once per pass
     * ({@link Equations#columnEnds}, whose comment gives the reasons) and the start estimate states it on the same
     * question ({@link #staticDrive}), so the estimate never starts a connection on a column the row does not read.
     * <ul>
     * <li>Both columns drive one way: the column of the end that drive leaves (the only admissible one).</li>
     * <li>The two drives point towards each other (neither column admissible, where a settled line lives): the column
     * nearest its own rest.</li>
     * <li>Both admissible (the pipe can stand full of either fluid): the start's direction {@code forwardStart}, the
     * history of which fluid is in the pipe.</li>
     * </ul>
     */
    static boolean columnFromFirst(double first,double second,double difference,double dz,boolean forwardStart) {
        double drivingFirst=difference-first*GRAVITY*dz,drivingSecond=difference-second*GRAVITY*dz;
        if(drivingFirst>=0&&drivingSecond>=0)return true;
        if(drivingFirst<=0&&drivingSecond<=0)return false;
        if(drivingFirst<0)return Math.abs(drivingFirst)<=Math.abs(drivingSecond);
        return forwardStart;
    }
    /**
     * The flow a pump holding its maximum added pressure can actually push through its own
     * connection, from the start-of-step endpoint states: the same bisection
     * {@link #initialMassFlow} runs for a passive connection, with the pump's limit added to the
     * driving pressure.
     *
     * <p>A pump only reaches {@code PUMP_HEAD_LIMIT} because its target flow was refused, and the
     * pass that refused it leaves that target flow behind as the warm start. Near shutoff the two
     * are orders of magnitude apart - 9.96 kg/s against 0.07 - and the connection's loss is a power
     * law in between, so Newton walks down it by roughly halving the flow per iteration and the
     * closer the discharge sits to shutoff the further it has to walk. That walk is what runs out
     * of iterations, and it does so at every step size, which is why refining the step cannot help.
     * Seeding from the limit the pass is actually imposing starts the walk where it would have
     * ended.
     */
    private double headLimitMassFlow(PassiveNetwork graph,PassiveNetwork.Pipe pipe,FlowControl.Mover pump) {
        var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
        double rho=endDensity(a.state(),pipe.firstPort()),mu=endViscosity(a.state(),pipe.firstPort());
        double driving=endPressure(a,pipe.firstPort(),a.state())-endPressure(b,pipe.secondPort(),b.state())-rho*GRAVITY*(b.elevation()-a.elevation())+riseLimit(pump,rho,a.state().pressure());
        if(driving<=0)return 0;
        double lo=0,hi=1;while(pipe.pressureDrop(hi,rho,mu)<driving&&hi<1e6)hi*=2;
        for(int j=0;j<50;j++){double mid=(lo+hi)/2;if(pipe.pressureDrop(mid,rho,mu)>driving)hi=mid;else lo=mid;}
        return Math.min((lo+hi)/2,Math.min(massFlowLimit(pipe,a.state(),pipe.firstPort()),pump.targetVolumeFlow()*rho));
    }
    /**
     * A mover's pressure-rise limit ({@link FlowControl.Mover#riseLimit}); a compressor's is {@code (r_max - 1) P_s} on its
     * suction pressure (decision D7 of the phase-ports batch). A pump's is the setting, which is the rise for water at 298.15 K and 1 atm
     * ({@link FluidThermodynamics#pumpReferenceDensity()}), scaled by the suction's density over water's - a head, the
     * one quantity a real pump fixes. On water the limit is the setting within water's compressibility; on nitrogen at
     * 1 atm it is about 1/870 of it, so a pump moving gas between closed vessels reaches its limit, and then its shutoff,
     * within a second instead of evacuating the suction vessel until the gas left the property domain (F1 review,
     * section 5, option P1; documentation/fluid-followups/FLUID_PUMP_AND_THERMO_DOMAIN_REVIEW.md).
     *
     * <p>The density is the density of what the suction end draws ({@link #endDensity}): the suction node's bulk density,
     * mass over volume of everything it holds, at a BULK end, and the drawn phase's at a phase port - the same density
     * its target row converts the volume flow with and the velocity clamp caps that end with. No mode, band or switch is added; the target, head-limit and closed modes,
     * the shutoff band and the velocity clamp are decided exactly as before, on this limit.
     *
     * <p>The density is the trial's: the head-limit row reads the suction's decoded state, so the Jacobian carries the
     * limit's dependence on that node's unknowns, and every mode test reads the same density at the converged point.
     */
    private double riseLimit(FlowControl.Mover mover,double suctionDensity,double suctionPressure) {
        return mover.riseLimit(suctionDensity,suctionPressure,model.pumpReferenceDensity());
    }
    /** The velocity cap (kg/s) the solve {@code equations} imposes on connection {@code edge} carrying {@code q}, read on
     * the node states {@code states}: {@link #massFlowLimit} of a BULK donor end (the form it always had), the pass's
     * {@link Equations#capLimit} of a phase port. */
    private double velocityCap(Equations equations,int edge,double q,List<FluidThermodynamics.State> states) {
        var pipe=equations.graph.pipes().get(edge);int donor=q>=0?pipe.first():pipe.second();
        if(pipe.drawPort(q)==PassiveNetwork.PhasePort.BULK)return massFlowLimit(pipe,states.get(donor),PassiveNetwork.PhasePort.BULK);
        return equations.capLimit(edge,q>=0?0:1,equations.transportAt(donor,states.get(donor)));
    }
    /** The velocity cap's mass flow of what leaves {@code donor} through {@code port} at a vanishing flow (the stream
     * {@link #endDensity} names): its density, area and velocity limit, multiplied in the order the bulk form
     * {@code mass/volume*area*velocityLimit} always was, so a BULK end is that form to the bit. The cap a pass imposes on
     * a priority mixture is {@link Equations#capLimit}. */
    private double massFlowLimit(PassiveNetwork.Pipe pipe,FluidThermodynamics.State donor,PassiveNetwork.PhasePort port) {
        return endDensity(donor,port)*pipe.minimumArea()*endVelocityLimit(donor,port);
    }
    /**
     * The driving pressure of one end of a connection: the node's pressure plus the static offset of the port at that
     * end ({@link #portHead}: the level head at a vessel's LIQUID port, zero elsewhere). Every static driving pressure
     * the solver states - the hydraulic row ({@code edgeRows}), the pass's column choice ({@code columnEnds}), the
     * start-of-solve closures ({@link #closeDeadHeads}, {@link #illegalWithEitherDensity}), the start flows
     * ({@link #initialMassFlows}, {@link #initialMassFlow}, the balanced junction seed), the valve start head, the pump's
     * head-limit start ({@link #headLimitMassFlow}, which the reopen start calls), its carry offer and shutoff margin
     * ({@link #demand}) and the boundary-reopen test ({@link #reopenable}) - reads an end's pressure through here and
     * nowhere else, because they must agree: a closure decided on one pressure and reopened on another bottles a line
     * for good (review 8.8 (c) of the mixed-gas batch;
     * documentation/2026-09-26-phase-ports-and-compressor/LEVEL_HEAD_REVIEW.md 2.3). The offset applies in both
     * directions: it is a pressure, not a withdrawal rule.
     */
    double endPressure(PassiveNetwork.Reservoir node,PassiveNetwork.PhasePort port,FluidThermodynamics.State state) {
        return state.pressure()+portHead(node,port,state);
    }
    /** {@link #endPressure} of the end of {@code pipe} at {@code node}, on that node's own state in {@code graph}. */
    double endPressure(PassiveNetwork graph,PassiveNetwork.Pipe pipe,int node) {
        var reservoir=graph.reservoirs().get(node);
        return endPressure(reservoir,pipe.portAt(node),reservoir.state());
    }
    /**
     * The height every vessel's level head is stated over (decision D9, option B of
     * documentation/2026-09-26-phase-ports-and-compressor/LEVEL_HEAD_REVIEW.md): one metre, the one-block tank's own
     * height at the default 1 m3 volume. A constant, not a setting: a prismatic tank of this height over the footprint
     * {@code V/H}. Multi-block tanks, when they exist, state their structure's height here instead.
     */
    public static final double LEVEL_HEAD_HEIGHT=1;
    /**
     * The static head of a port over its node's pressure: the level head of a bottom outlet (decision D9, option B), zero
     * everywhere else. At a LIQUID port of a vessel (RESERVOIR, or its rate-solve copy PORT) the node's pressure is the
     * headspace's, and the bottom stands below the weight of the condensed phases over the tank's footprint
     * {@code A = V/H}:
     *
     * <p>{@code h = g * m_c * H / V}, with {@code m_c} the condensed mass of {@code state} (hydrocarbon liquid, free water
     * and every solid, {@link FluidThermodynamics#liquidMass}), {@code H} = {@link #LEVEL_HEAD_HEIGHT} and {@code V} the
     * vessel's inventory volume (its identity, not the trial state's EOS volume).
     *
     * <p>Linear in the decoded condensed amounts and independent of T and P except through the phase split; every caller
     * evaluates it on the state it is stating a pressure for (the hydraulic row on the Newton's trial state, so the block
     * Jacobian's re-decoded node carries its derivative with no new structural entry). It is a pressure, so it applies in
     * both directions: inflow through a bottom port works against it. It does not enter the port's capacities (those read
     * the phases' masses, decision D11) nor the step controller. A VAPOR or BULK port, a node of any other
     * kind, and a state with no condensed mass get exactly {@code 0.0}, so {@code P + 0.0} is {@code P} to the bit:
     * gas-only and all-BULK islands are the doubles they were.
     */
    double portHead(PassiveNetwork.Reservoir node,PassiveNetwork.PhasePort port,FluidThermodynamics.State state){return portHead(model,node,port,state);}
    /** {@link #portHead} for a caller outside a solve (the network reduction stage): the same arithmetic on {@code model}. */
    static double portHead(FluidThermodynamics model,PassiveNetwork.Reservoir node,PassiveNetwork.PhasePort port,FluidThermodynamics.State state) {
        if(port!=PassiveNetwork.PhasePort.LIQUID||node.kind()!=PassiveNetwork.NodeKind.RESERVOIR&&node.kind()!=PassiveNetwork.NodeKind.PORT)return 0;
        double condensed=model.liquidMass(state);
        return condensed>0?GRAVITY*condensed*LEVEL_HEAD_HEIGHT/node.inventory().volume():0;
    }
    /**
     * The density of what leaves a node through {@code port} at a vanishing flow: the bulk {@code mass/volume} at a BULK
     * end, and at a phase port the stream of the first phase of its priority order the state holds
     * ({@link PhaseDraw#leading}; decision D11): the vapour's ({@link FluidThermodynamics#vaporDensity}, as before) or a
     * liquid's with its solid share; the bulk's when the state holds none of the three. Every static column, sign test and
     * start-flow velocity cap reads an end's density through here: a sign test sees which way the fluid the port would
     * draw first stands in the connection, and a gas-only tank's bottom port or a liquid-full tank's top port reads the
     * phase it then draws (the gas, the lighter liquid) instead of the bulk of decision A8.
     */
    private double endDensity(FluidThermodynamics.State s,PassiveNetwork.PhasePort port){return endDensity(model,s,port);}
    /** {@link #endDensity} for a caller outside a solve (the network reduction stage): the same arithmetic on {@code model}. */
    static double endDensity(FluidThermodynamics model,FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        return phase<0?s.mass()/s.volume():phase==FluidThermodynamics.GAS?model.vaporDensity(s):model.phaseMass(s,phase)/FluidThermodynamics.phaseVolume(s,phase);
    }
    /** The velocity limit of what leaves a node through {@code port} at a vanishing flow ({@link #endDensity}'s stream);
     * the bulk's at a BULK end and when the state holds none of the phases. */
    private double endVelocityLimit(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        return phase<0?model.velocityLimit(s):model.phaseVelocityLimit(s,phase);
    }
    /** The viscosity of what leaves a node through {@code port} at a vanishing flow; the bulk's at a BULK end and when the
     * state holds none of the phases. */
    private double endViscosity(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        return phase<0?viscosity(s):model.phaseViscosity(s,phase,viscosities,null);
    }
    /** The solid mass share of what leaves a node through {@code port} at a vanishing flow: none in the gas, a liquid's share
     * of the solids over its stream's mass, the bulk share at a BULK end and when the state holds none of the phases. */
    private double solidShare(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        if(phase<0)return s.solidMoments().mass()/s.mass();
        return phase==FluidThermodynamics.GAS?0:FluidThermodynamics.phaseSolidShare(s,phase)*s.solidMoments().mass()/model.phaseMass(s,phase);
    }
    /** The mass flow of a suction volume flow of what leaves through {@code port}, {@code Q m / V}, multiplied and divided
     * in the order the bulk start flows always were ({@code Q*mass/volume}, not {@code Q*(mass/volume)}). */
    private double suctionMassFlow(double volumeFlow,FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        if(phase<0)return volumeFlow*s.mass()/s.volume();
        return volumeFlow*model.phaseMass(s,phase)/FluidThermodynamics.phaseVolume(s,phase);
    }
    /** The conserved amounts (water last) of what leaves through {@code port} at a vanishing flow; the node's total amounts
     * at a BULK end and when the state holds none of the phases. Over {@link #endMass} they are the moles per kilogram of the
     * fluid the connection carries: every start guess these three helpers feed is a fluid flash, so the solids a liquid or a
     * bulk end also draws are left behind, as {@link #endStream} leaves them (their mass and stored energy are not the
     * fluid's: LNG drawn with 1.3 % of dry ice at its stored energy flashed 1.3 % of its methane to vapour). */
    private double[] endMoles(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        return phase<0?PhaseLayout.totalAmounts(s):FluidThermodynamics.phaseMoles(s,phase);
    }
    /** The fluid mass of {@link #endMoles}' stream, kg: the phase's (or the node's) mass without its solids; a dry end, which
     * draws no fluid ({@link #endStream} is null there), keeps its whole mass. */
    private double endMass(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        double mass=phase<0?s.mass():model.phaseMass(s,phase),solids=s.solidMoments().mass();
        if(solids==0)return mass;
        double fluid=mass-solidShare(s,phase)*solids;
        return fluid>0?fluid:mass;
    }
    /** The fluid's specific enthalpy of {@link #endMoles}' stream, J/kg, its solids left behind (see {@link #endMoles}). */
    private double endSpecificEnthalpy(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port);
        double whole=phase<0?s.enthalpy()/s.mass():model.phaseSpecificEnthalpy(s,phase,null),solids=s.solidMoments().mass();
        if(solids==0)return whole;
        double mass=phase<0?s.mass():model.phaseMass(s,phase),fluid=mass-solidShare(s,phase)*solids;
        if(!(fluid>0))return whole;
        return (whole*mass-solidShare(s,phase)*s.solidMoments().enthalpy(s.temperature(),s.pressure()))/fluid;
    }
    /** The share of the node's solids a stream of {@code phase} draws: all of them at a BULK end ({@code phase < 0}). */
    private static double solidShare(FluidThermodynamics.State s,int phase){return phase<0?1:FluidThermodynamics.phaseSolidShare(s,phase);}
    /**
     * The fluid of {@link #endMoles}' stream as a state: the phase amounts {@code port} draws from {@code s} at a vanishing
     * flow (all of them at a BULK end and when the state holds none of the phases), per kilogram of fluid, at the node's
     * own temperature and pressure, its solids left behind. Built from the node's own amounts ({@link FluidThermodynamics#state}):
     * no flash, so nothing a flash's window or answer could refuse. Null when the stream carries no fluid (dry solids).
     */
    private FluidThermodynamics.State endStream(FluidThermodynamics.State s,PassiveNetwork.PhasePort port) {
        int phase=PhaseDraw.leading(model,s,port),n=model.hydrocarbon.componentCount();
        boolean gas=phase<0||phase==FluidThermodynamics.GAS,oil=phase<0||phase==FluidThermodynamics.OIL,water=phase<0||phase==FluidThermodynamics.WATER;
        double[] liquid=new double[n],vapour=new double[n],l=s.liquidView(),v=s.vaporView();
        double waterLiquid=water?s.waterLiquid():0,waterVapour=gas?s.waterVapor():0,mass=(waterLiquid+waterVapour)*model.molecularWeight(n);
        for(int c=0;c<n;c++){liquid[c]=oil?l[c]:0;vapour[c]=gas?v[c]:0;mass+=(liquid[c]+vapour[c])*model.molecularWeight(c);}
        if(!(mass>0))return null;
        for(int c=0;c<n;c++){liquid[c]/=mass;vapour[c]/=mass;}
        return model.state(s.temperature(),s.pressure(),liquid,vapour,waterLiquid/mass,waterVapour/mass,gas?s.hydrocarbonPartialPressure():0);
    }
    /**
     * The head this connection demands of its device at the point one active-set pass converged
     * to: the discharge pressure less the suction pressure, plus the static column between them.
     *
     * <p>This is the one scalar the {@code PUMP_HEAD_LIMIT}/{@code CLOSED} pair is decided on, and
     * it is literally the same expression in both modes. In {@code PUMP_HEAD_LIMIT} the actuator
     * row pins the head to the maximum, so the hydraulic row makes this {@code maximum - loss(q)}:
     * the margin below is then the edge's own pressure loss, positive for forward flow and
     * negative for reverse. In {@code CLOSED} the actuator row pins the flow to zero, so the free
     * head unknown <em>is</em> this demand and the margin is how much head the pump still has in
     * hand. Reading one quantity in both modes is what makes the pair a threshold on a line rather
     * than two one-sided tests of two different unknowns.
     */
    private double demand(PassiveNetwork graph,PassiveNetwork.Pipe pipe,List<FluidThermodynamics.State> states,double density) {
        return endPressure(graph.reservoirs().get(pipe.second()),pipe.secondPort(),states.get(pipe.second()))
                -endPressure(graph.reservoirs().get(pipe.first()),pipe.firstPort(),states.get(pipe.first()))
                +density*GRAVITY*(graph.reservoirs().get(pipe.second()).elevation()-graph.reservoirs().get(pipe.first()).elevation());
    }
    /**
     * How much head margin this edge's own rows can actually resolve, and therefore the band around
     * the shutoff corner inside which a device's mode may not be decided by the sign of a flow.
     *
     * <p>The pressure-dimensioned rows of an edge are stated in {@link Equations#pressureScales},
     * so a solve converged to {@code tolerance} has decided them to {@code tolerance*scale} Pa -
     * 1e-4 Pa on an atmospheric island and 6e-4 Pa at 6 bar. Through the line's resistance that is
     * a flow resolution of order 1e-5 kg/s, while the rule this replaces closed a pump on a reverse
     * flow of 1e-10 kg/s: five orders of magnitude below anything the solve decided the sign of.
     * The floor of 0.01 Pa is the band the {@code CLOSED} side already used, kept so that the
     * reopening test is bit for bit the test it was on every island whose scale is at or below
     * 1e5 Pa - which is every island the regression reference was captured on.
     *
     * <p>Both sides of the pair now read the same margin against this one band, so there is no
     * interval in which a pump at its shutoff corner is left holding its head limit. That corner is
     * degenerate - head at the maximum and zero flow satisfy both modes' equations at once - and
     * {@code CLOSED} is its well-posed representative: it pins the flow to exactly zero through the
     * actuator row instead of leaving the solve to find zero by iteration, which is what keeps a
     * zero-holdup junction off its own upwind and
     * junction inflow-floor discontinuities of the former zero-holdup junction rows.
     */
    private static double shutoffBand(double pressureScale,double tolerance) {
        return Math.max(.01,tolerance*Math.max(1e5,pressureScale));
    }
    private static boolean canClamp(FlowControl.Mode mode) {
        return mode==FlowControl.Mode.PASSIVE||mode==FlowControl.Mode.PUMP_HEAD_LIMIT||mode==FlowControl.Mode.PUMP_SUCTION_LIMIT||mode==FlowControl.Mode.VALVE_OPEN;
    }
    /** Whether an accepted mode reports a connection on its velocity cap. */
    private static boolean capMode(FlowControl.Mode mode) {
        return mode==FlowControl.Mode.VELOCITY_LIMITED||mode==FlowControl.Mode.PUMP_VELOCITY_LIMIT||mode==FlowControl.Mode.VALVE_VELOCITY_LIMIT;
    }
    /** {@link #phaseCorrection(PassiveNetwork, List, Runnable, FluidThermodynamics.Prepared[], int[])} with no prepared
     * temperatures and no record of folded phases. */
    private List<FluidThermodynamics.State> phaseCorrection(PassiveNetwork graph,List<FluidThermodynamics.State> states,Runnable checkpoint) {
        return phaseCorrection(graph,states,checkpoint,null,null);
    }
    /**
     * The outer phase check of a pass's point, node by node ({@link FluidThermodynamics#phaseCheck}, owner decisions D25 and
     * D26): a phase leaves a node only when the solve empties it (the trace fold of the point, at a converged point whose
     * reconstruction already folded it as at an unsuccessful pass's last iterate), a phase the node's TP answer holds and
     * the node does not is added at the node's own split (a junction's PH answer, a vessel's closure of what the point
     * holds: {@link #vesselSplit}), and no populated phase set is swapped for another on a TP verdict. Never inside a Newton
     * residual or Jacobian evaluation. One node is corrected per call, as one active-set change per pass, and the nodes
     * after it are not checked (their verdicts would be discarded, and an own split is a flash); null when none changes.
     *
     * <p>{@code prepared}, when given, is the pass's per-node prepared temperature state: a node's flash at exactly that
     * temperature reuses its Peng-Robinson workspace instead of preparing a new one (the arithmetic is the same).
     * {@code foldedPhases}, when given, holds per node the phases ({@link #phaseCode} bits) the trace rule removed in this
     * solve and those a node's own split refused (A85; the D26 amendment): the check does not add them back, and writes
     * there a phase its fold removes from a converged point and a phase its own split refuses there, so the sequence of
     * passes stays finite and a refused phase costs one own split per solve. A failed pass's iterate is checked on a copy
     * (decision D28 A'): its check is a trial layout, which reads the record and never writes to it.
     */
    private List<FluidThermodynamics.State> phaseCorrection(PassiveNetwork graph,List<FluidThermodynamics.State> states,Runnable checkpoint,FluidThermodynamics.Prepared[] prepared,int[] foldedPhases) {
        for(int i=0;i<states.size();i++) {
            var state=states.get(i);var node=graph.reservoirs().get(i);if(node.fixed())continue;
            FluidThermodynamics.State checked;
            try{checked=model.phaseCheck(state,node.junction(),foldedPhases,i,prepared==null?null:prepared[i],
                    node.junction()?null:start->vesselSplit(node,state,start,checkpoint),checkpoint);}
            catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold hold){throw hold.at(node.id());}
            if(checked==state)continue;
            var corrected=new ArrayList<>(states);corrected.set(i,checked);return corrected;
        }
        return null;
    }
    /**
     * Owner decision D26: a vessel's own split at a pass's point {@code state} ({@link InventoryEquilibrium#split}), from
     * {@code start}: the closure of what the point holds, its amounts and internal energy, the wall's work booked from its
     * bag volume, so the equilibrium of its energy and volume while the wall is taut and of its enthalpy at the ambient
     * pressure while slack; {@code null} where that closure fails.
     */
    private FluidThermodynamics.State vesselSplit(PassiveNetwork.Reservoir node,FluidThermodynamics.State state,FluidThermodynamics.State start,Runnable checkpoint) {
        double capacity=node.inventory().volume();
        return InventoryEquilibrium.split(model,start,new PassiveNetwork.Inventory(capacity,PhaseLayout.totalAmounts(state),state.internalEnergy(),state.solids()),
                PhaseLayout.bagVolume(state,capacity),checkpoint);
    }
    /**
     * The pass's seeds with their trace phases folded ({@code FluidThermodynamics.folded}, decision D34); the list itself
     * when no seed holds one. Fixed nodes carry no unknowns and keep their boundary state.
     */
    private List<FluidThermodynamics.State> foldedSeeds(PassiveNetwork graph,List<FluidThermodynamics.State> seeds) {
        List<FluidThermodynamics.State> result=null;
        for(int i=0;i<seeds.size();i++) {
            if(graph.reservoirs().get(i).fixed())continue;
            var seed=seeds.get(i);FluidThermodynamics.State folded;
            try{folded=model.folded(seed,graph.reservoirs().get(i).junction());}
            catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold hold){throw hold.at(graph.reservoirs().get(i).id());}
            if(folded==seed)continue;
            if(result==null)result=new ArrayList<>(seeds);
            result.set(i,folded);
        }
        return result==null?seeds:result;
    }
    /** {@link #phaseFolded}, recording the folded phases ({@link #phaseCode} bits) of each node in {@code foldedPhases}. */
    private static boolean recordFolds(PassiveNetwork graph,List<FluidThermodynamics.State> solved,List<FluidThermodynamics.State> reconstructed,int[] foldedPhases) {
        if(!phaseFolded(graph,solved,reconstructed))return false;
        for(int i=0;i<solved.size();i++)if(!graph.reservoirs().get(i).fixed())foldedPhases[i]|=phaseCode(solved.get(i))&~phaseCode(reconstructed.get(i))&15;
        return true;
    }
    /**
     * Whether the reconstruction folded away a phase the pass solved (decision D34): a node's restated state lacks a
     * hydrocarbon liquid, free water or gas phase its Newton state holds, or its gas lost its water vapour (a carried gas,
     * decision D41). The restatement ({@code ConservativeTransport.repartition}) keeps every phase whose booked amounts are
     * not all zero, so a phase it drops is a phase it folded.
     */
    private static boolean phaseFolded(PassiveNetwork graph,List<FluidThermodynamics.State> solved,List<FluidThermodynamics.State> reconstructed) {
        for(int i=0;i<solved.size();i++) {
            if(graph.reservoirs().get(i).fixed())continue;
            var before=solved.get(i);var after=reconstructed.get(i);
            if(before.liquidVolume()>0&&after.liquidVolume()==0||before.waterLiquid()>0&&after.waterLiquid()==0
                    ||before.vaporVolume()>0&&after.vaporVolume()==0||before.waterVapor()>0&&after.waterVapor()==0)return true;
        }
        return false;
    }
    /**
     * Whether the reconstruction gave a hydrocarbon phase to a node the Newton state holds none in (hold class E3). A
     * junction the booked amounts give a carried trace gas has none to give (decision D41): the next pass would omit it
     * again ({@code PhaseLayout.support}), so it is the junction's state, not an appearance.
     */
    private static boolean phaseAppeared(PassiveNetwork graph,List<FluidThermodynamics.State> solved,List<FluidThermodynamics.State> reconstructed) {
        for(int i=0;i<solved.size();i++) {
            var before=solved.get(i);var after=reconstructed.get(i);
            if(before.liquidVolume()>0||before.vaporProperties()!=null||after.liquidVolume()==0&&after.vaporProperties()==null)continue;
            if(graph.reservoirs().get(i).junction()&&FluidThermodynamics.carriedTrace(after))continue;
            double[] held=PhaseLayout.totalAmounts(before),booked=PhaseLayout.totalAmounts(after);double total=PhaseLayout.sum(booked);
            for(int c=0;c<held.length;c++)if(held[c]==0&&booked[c]>=PassiveIntervalSolver.TRACE_FLUSH_FRACTION*total)return true;
        }
        return false;
    }
    /** Which phases this state carries: the four fluid flags of {@link FluidThermodynamics#phaseBits} (hydrocarbon liquid 1,
     * hydrocarbon vapour 2, free water 4, steam 8) and 16 for solids. */
    private static int phaseCode(FluidThermodynamics.State state) {
        return (state.solidMoments().mass()>0?16:0)|FluidThermodynamics.phaseBits(state);
    }
    /**
     * Closes, before the first pass, every passive connection whose own starting point leaves it no
     * direction it is allowed to carry.
     *
     * <p>The hydraulic row of a connection is {@code driving - loss(q)}, and {@code loss} is zero at
     * zero flow and strictly increasing away from it, so a root with {@code q > 0} exists only when
     * the driving pressure a forward flow stands in is positive, and a root with {@code q < 0} only
     * when the driving pressure a reverse flow stands in is negative. A connection one of whose ends
     * forbids a direction - a generator, a void, a directionally blocked pipe - therefore has no
     * admissible root at all when the only sign with a root is the forbidden one, and its one
     * admissible answer is a flow of exactly zero. That is the same answer
     * {@code boundaryClosed} already produces, and the pass loop already reaches it the moment a
     * converged point shows a direction the boundary refuses. The whole defect is that on a
     * dead-headed line the pass cannot converge to that point: the reverse root lies across a flow
     * of zero, where the friction slope changes by three orders between a liquid and a gas, and the
     * step limiter has its own reasons to refuse the journey. So the closure is taken from the
     * starting point instead, which says the same thing without asking the Newton to travel there -
     * the pre-interval rate pass of {@link SolidEventIntegrator} closes a transport failure the same
     * way rather than letting the interval discover it.
     *
     * <p>Three conditions keep this a statement that cannot be wrong for the solve it is made in.
     *
     * <ul>
     * <li><b>Both columns are asked.</b> A forward flow stands in the fluid of {@link
     * PassiveNetwork.Pipe#first} and a reverse flow in that of {@link PassiveNetwork.Pipe#second},
     * so each direction is tested against the static head of its own column - the same pairing
     * {@link Equations#columnEnds} states a connection's head on. A direction is refused only
     * when the column that would fill it does not drive it.
     * <li><b>Neither end of the run is a zero-holdup junction.</b> A junction owns no volume, so
     * the pressure it carries into a pass is an output of the previous solve rather than a property
     * of any stock, and it is free to move as far as the hydraulics need within this one. A
     * vessel's and a boundary's are not: a boundary holds its pressure fixed by construction, and a
     * vessel's pressure moves only with what it receives, monotonically against the flow that
     * delivers it. So across a run between a boundary and a vessel a driving pressure that refuses
     * a direction at the starting point refuses it everywhere the solve can go - delivering in the
     * allowed direction only pushes it further away - while a junction's pressure says nothing. The
     * run is the one {@link #initialMassFlows} already contracts: a maximal chain of passive
     * connections through degree-two junctions, which carry the one flow the chain passes and own
     * no pressure of their own, so a static column across the chain is the column across its two
     * ends and nothing in between enters it. A run of one connection is the ordinary case.
     * <li><b>Only passive connections.</b> An actuator decides its own mode, and the device-first
     * rule of the pass loop exists precisely so that a boundary direction is never closed on
     * evidence that is really a device's symptom. A pre-pass closure of an actuator's connection
     * would take that decision before the device had spoken at all. A run through a junction an
     * actuator touches is left alone for the same reason.
     * </ul>
     *
     * <p>It cannot cycle. The closure is monotone within a solve, it is taken once before any pass,
     * it is read by the existing pass loop exactly as a closure taken by that loop would be, and
     * it is never cleared - so it adds no transition to the active-set sequence and cannot lengthen
     * it. It is per solve, so a line whose generator is raised afterwards is decided again from the
     * new starting point and opens.
     */
    private void closeDeadHeads(PassiveNetwork graph,boolean[] boundaryClosed,boolean[] keep) {
        int edges=graph.pipes().size(),nodes=graph.reservoirs().size();
        int[] degree=new int[nodes];boolean[] actuated=new boolean[nodes];
        for(var pipe:graph.pipes()) {
            degree[pipe.first()]++;degree[pipe.second()]++;
            if(!(pipe.control() instanceof FlowControl.Passive)){actuated[pipe.first()]=true;actuated[pipe.second()]=true;}
        }
        boolean[] interior=new boolean[nodes];
        for(int i=0;i<nodes;i++)interior[i]=graph.reservoirs().get(i).junction()&&degree[i]==2&&!actuated[i];
        int[][] nodeEdges=new int[nodes][];
        for(int i=0;i<nodes;i++)nodeEdges[i]=new int[degree[i]];
        Arrays.fill(degree,0);
        for(int edge=0;edge<edges;edge++) {
            var pipe=graph.pipes().get(edge);
            nodeEdges[pipe.first()][degree[pipe.first()]++]=edge;nodeEdges[pipe.second()][degree[pipe.second()]++]=edge;
        }
        boolean[] taken=new boolean[edges];
        for(int edge=0;edge<edges;edge++) {
            if(taken[edge]||boundaryClosed[edge]||!(graph.pipes().get(edge).control() instanceof FlowControl.Passive))continue;
            var pipe=graph.pipes().get(edge);
            // The maximal passive run through degree-two junctions containing this connection, as
            // a node order and the links between consecutive nodes; see {@link #initialMassFlows}.
            var chain=new ArrayList<Integer>();var order=new ArrayList<Integer>();
            order.add(pipe.first());chain.add(edge);order.add(pipe.second());taken[edge]=true;
            for(int end=0;end<2;end++) {
                int at=end==0?pipe.first():pipe.second(),from=edge;
                while(interior[at]) {
                    int next=nodeEdges[at][0]==from?nodeEdges[at][1]:nodeEdges[at][0];
                    if(taken[next]||!(graph.pipes().get(next).control() instanceof FlowControl.Passive))break;
                    taken[next]=true;var step=graph.pipes().get(next);
                    int beyond=step.first()==at?step.second():step.first();
                    if(end==0){chain.addFirst(next);order.addFirst(beyond);}else{chain.add(next);order.add(beyond);}
                    at=beyond;from=next;
                }
            }
            int start=order.getFirst(),finish=order.getLast();
            if(start==finish||graph.reservoirs().get(start).junction()||graph.reservoirs().get(finish).junction())continue;
            // Which way the run as a whole may carry: every link must allow its own share of it.
            boolean forwardAllowed=true,reverseAllowed=true;
            for(int position=0;position<chain.size();position++) {
                var link=graph.pipes().get(chain.get(position));boolean aligned=link.first()==order.get(position);
                forwardAllowed&=boundaryAllowed(graph,link,aligned?1:-1);
                reverseAllowed&=boundaryAllowed(graph,link,aligned?-1:1);
            }
            if(forwardAllowed&&reverseAllowed)continue;
            var a=graph.reservoirs().get(start);var b=graph.reservoirs().get(finish);
            // Each direction in the column of what its donor end port draws, at the ends' driving pressures.
            var startPort=graph.pipes().get(chain.getFirst()).portAt(start);var finishPort=graph.pipes().get(chain.getLast()).portAt(finish);
            double dz=b.elevation()-a.elevation(),difference=endPressure(a,startPort,a.state())-endPressure(b,finishPort,b.state());
            boolean forward=forwardAllowed&&difference-endDensity(a.state(),startPort)*GRAVITY*dz>0;
            boolean reverse=reverseAllowed&&difference-endDensity(b.state(),finishPort)*GRAVITY*dz<0;
            if(keep!=null){boolean kept=false;for(int link:chain)kept|=keep[link];if(kept)continue;}
            if(!forward&&!reverse)for(int link:chain)boundaryClosed[link]=true;
        }
    }
    /**
     * The {@code illegalDirection} closure of the pass loop, taken once at the start of the solve on the point pass 0
     * starts from.
     *
     * <p>A passive connection whose start-point flow runs in a direction its own boundary forbids
     * ({@link #boundaryAllowed}) is closed for this solve exactly as that rule closes it after a converged pass.
     * The direction is the one {@link #startPoint} gives the connection; where the start point gives it none
     * ({@code |q| <= 1e-10}, the rule's own threshold, which is every connection the previous accepted point had
     * closed, since a closed connection records exactly zero), it is the direction of {@link #initialMassFlow} on
     * the current states. Nothing is carried from the previous solve: the closure is derived afresh from this
     * solve's start point, so a connection whose pressures turn legal opens again at the next solve.
     *
     * <p>Passive connections only. The comment above the {@code illegalDirection} rule decides devices first: a
     * passive edge showing an illegal direction next to a device that cannot hold its mode is that device's
     * symptom, and closing the symptom first pins the wrong active set. Before any pass no device has been
     * decided, so a device connection is left to the pass loop.
     *
     * <p>Why: an island coming to rest next to a void or a generator leaves the one-way connections to it with flows that
     * point the forbidden way at the start of the next solve; the pass loop would close them only after a pass that
     * converged, and at rest that pass is the zero-flow stall of review 7.8. Only once this solver has accepted a solve on
     * the same structure (pipe identities and node ids): a first solve's junction pressures are the compiler's property
     * guess, not a state. And only where the static driving pressure forbids the direction with either end's density
     * ({@link #illegalWithEitherDensity}). {@code keep} names the connections the interval solver reopened.
     */
    private void closeIllegalStarts(PassiveNetwork graph,List<FlowControl.Mode> modes,boolean[] boundaryClosed,boolean[] keep) {
        // Decision D54: a passive run whose static drive points out of a dry end (a vessel holding no fluid, the top port of a
        // slack vessel holding no gas) carries nothing: the end gives nothing and the run stands in what its other end would
        // send (the column of a dry end is the other end's), so the drive is no guess. Closed for this solve, on every solve.
        for(int i=0;i<graph.pipes().size();i++) {
            var pipe=graph.pipes().get(i);var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
            if(boundaryClosed[i]||!(pipe.control() instanceof FlowControl.Passive)||keep!=null&&keep[i])continue;
            boolean dryA=dryEnd(a,pipe.firstPort()),dryB=dryEnd(b,pipe.secondPort());if(!dryA&&!dryB)continue;
            double drive=staticDrive(graph,pipe,endPressure(a,pipe.firstPort(),a.state())-endPressure(b,pipe.secondPort(),b.state()));
            if(dryA&&drive>0||dryB&&drive<0||dryA&&dryB)boundaryClosed[i]=true;
        }
        if(!(acceptedPipes.equals(identities(graph))&&Arrays.equals(acceptedNodeIds,nodeIds(graph))))return;
        int edges=graph.pipes().size();
        double[] start=new double[edges];
        startPoint(graph,modes,boundaryClosed,identities(graph),null,start,new double[edges],new boolean[edges]);
        double[] estimate=null;
        for(int i=0;i<edges;i++) {
            var pipe=graph.pipes().get(i);
            if(boundaryClosed[i]||!(pipe.control() instanceof FlowControl.Passive))continue;
            if(keep!=null&&keep[i])continue;
            double q=start[i];
            if(Math.abs(q)<=1e-10){if(estimate==null)estimate=initialMassFlows(graph);q=estimate[i];}
            if(Math.abs(q)>1e-10&&!boundaryAllowed(graph,pipe,q)) {
                if(illegalWithEitherDensity(graph,pipe,q))boundaryClosed[i]=true;
            }
        }
    }
    /**
     * Whether a connection in {@code mode} relates the pressures of its two ends: every open connection except one whose
     * flow is prescribed (closed, refused, a mover at its target flow) or whose row fixes its first end's pressure outright
     * (a regulating valve, or a pump on its suction limit, which anchors that end instead; see {@link #pockets}).
     */
    private static boolean couples(FlowControl.Mode mode,boolean closed) {
        return !closed&&switch(mode){case CLOSED,INLET_WRONG_PHASE,PUMP_TARGET,VALVE_REGULATING,PUMP_SUCTION_LIMIT->false;default->true;};
    }
    /** Whether a connection in {@code mode} fixes its first end's pressure by its own row: a regulating valve (its set
     * point) and a pump on its suction limit (the vapour pressure, owner decision D59 (4)). */
    private static boolean anchors(FlowControl.Mode mode){return mode==FlowControl.Mode.VALVE_REGULATING||mode==FlowControl.Mode.PUMP_SUCTION_LIMIT;}
    private static boolean closedEdge(FlowControl.Mode mode,boolean closed){return closed||mode==FlowControl.Mode.CLOSED||mode==FlowControl.Mode.INLET_WRONG_PHASE;}
    private static int root(int[] parent,int node){while(parent[node]!=node){parent[node]=parent[parent[node]];node=parent[node];}return node;}
    /**
     * The pockets of a pass (hold F8 of the phase-ports batch): for every node, the pocket it belongs to (the index of a
     * member), or -1. A pocket is a set of junctions joined by pressure-coupling connections ({@link #couples}) with no
     * pressure anchor: no coupling connection to a node that is not a junction (a vessel, whose pressure its stock sets, a
     * port, a generator or a void, whose pressure is given) and no regulating valve drawing from it (whose row fixes the
     * pressure of its first end). A junction's pressure is algebraic - its mass is pinned, so only the hydraulics place it -
     * and nothing places a pocket's pressure level: shifting every member's pressure together changes no row but the rows of
     * the connections on its boundary, which are all prescribed-flow rows with a free head or none.
     */
    private static int[] pockets(PassiveNetwork graph,List<FlowControl.Mode> modes,boolean[] boundaryClosed) {
        int nodes=graph.reservoirs().size();int[] parent=new int[nodes];for(int i=0;i<nodes;i++)parent[i]=i;
        boolean[] anchored=new boolean[nodes];
        for(int i=0;i<nodes;i++)anchored[i]=!graph.reservoirs().get(i).junction();
        for(int e=0;e<graph.pipes().size();e++) {
            var pipe=graph.pipes().get(e);var mode=modes.get(e);
            if(!boundaryClosed[e]&&anchors(mode))anchored[pipe.first()]=true;
            if(couples(mode,boundaryClosed[e])){int a=root(parent,pipe.first()),b=root(parent,pipe.second());if(a!=b)parent[Math.max(a,b)]=Math.min(a,b);}
        }
        boolean[] rootAnchored=new boolean[nodes];for(int i=0;i<nodes;i++)if(anchored[i])rootAnchored[root(parent,i)]=true;
        int[] pocket=new int[nodes];for(int i=0;i<nodes;i++){int r=root(parent,i);pocket[i]=rootAnchored[r]?-1:r;}
        return pocket;
    }
    /**
     * Hold F8 of the phase-ports batch (2026-09-27): the start modes of a pass must leave every pocket ({@link #pockets})
     * a root. A pocket whose boundary carries only closed connections and movers at their target flow balances its mass
     * against the targets alone - its members' mass rows sum to the targets' net inflow, which no unknown of the pocket
     * moves - so with a nonzero target it has no root, and its pressure level has no row at all: the factorization is
     * singular before the first Newton step (a pump discharging into a pressure control valve that is closed at the start,
     * or into an inline filter with nothing past it). The pass loop can only leave an active set from a converged point, so
     * such a start was refused at every step size and the island held.
     *
     * <p>What the pocket can do instead is decided here, in the order the physics does it, and only at the start of the
     * solve (the pass loop's transitions decide from the first converged point on, unchanged):
     * <ul>
     * <li>a mover pushing into the pocket raises its pressure until a closed pressure control valve drawing from it opens:
     * such a valve whose far end has a pressure anchor starts regulating (its row holds the pocket at its set point and
     * its flow carries the targets out);</li>
     * <li>otherwise nothing can take the targets: a mover (at its target or at its head limit) with a sealed pocket on either
     * side - every way out of it other than through the mover closed ({@link #sealedWithout}) - can carry nothing, and it
     * starts closed at its shutoff for this solve ({@code atShutoff}, which the pass loop never reopens within the solve,
     * as for a pump it has itself found at its shutoff corner). The pocket is then isolated and keeps its pressure
     * ({@code Equations.retainedPressures}). Starting it at its head limit instead also has a root, but one that places
     * the pocket at the mover's shutoff pressure, which for a compressor or a strong pump lies outside the property
     * package's range (the placement-sequence fuzz: a dead-ended compressor's pocket at 2.5 MPa of nitrogen, a pump's
     * dead-ended suction pulled below 100 Pa), and the island waited on a thermo-domain hold a zero flow never reaches.</li>
     * </ul>
     * Repeated until no pocket is left without a root (each round changes at least one mode and never undoes one). An
     * island without such a pocket keeps its start modes exactly.
     */
    private static void relieveSealedPockets(PassiveNetwork graph,List<FlowControl.Mode> modes,boolean[] boundaryClosed,boolean[] atShutoff) {
        int edges=graph.pipes().size();int[][] nodeEdges=incidence(graph);
        for(int round=0;round<=edges;round++) {
            int[] pocket=pockets(graph,modes,boundaryClosed);int nodes=pocket.length;
            int[] inflow=new int[nodes],outflow=new int[nodes];boolean[] free=new boolean[nodes];
            for(int e=0;e<edges;e++) {
                var pipe=graph.pipes().get(e);var mode=modes.get(e);
                if(closedEdge(mode,boundaryClosed[e]))continue;
                for(int end=0;end<2;end++) {
                    int node=end==0?pipe.first():pipe.second(),other=end==0?pipe.second():pipe.first();
                    if(pocket[node]<0||pocket[other]==pocket[node])continue;
                    if(mode==FlowControl.Mode.PUMP_TARGET&&pipe.control() instanceof FlowControl.Mover mover&&mover.targetVolumeFlow()>0){if(end==1)inflow[pocket[node]]++;else outflow[pocket[node]]++;}
                    else free[pocket[node]]=true;
                }
            }
            boolean changed=false;
            for(int r=0;r<nodes;r++) {
                if(inflow[r]==0||free[r])continue;
                for(int e=0;e<edges;e++) {
                    var pipe=graph.pipes().get(e);
                    if(pipe.control() instanceof FlowControl.PressureValve&&modes.get(e)==FlowControl.Mode.CLOSED&&!boundaryClosed[e]&&pocket[pipe.first()]==r&&pocket[pipe.second()]<0)
                        {modes.set(e,FlowControl.Mode.VALVE_REGULATING);changed=true;}
                }
            }
            for(int e=0;e<edges;e++) {
                var pipe=graph.pipes().get(e);var mode=modes.get(e);
                if(boundaryClosed[e]||mode!=FlowControl.Mode.PUMP_TARGET&&mode!=FlowControl.Mode.PUMP_HEAD_LIMIT&&mode!=FlowControl.Mode.PUMP_SUCTION_LIMIT)continue;
                // Owner decision D58 (A4): what feeds the suction must reach it, and what relieves the discharge must carry flow
                // away from it; a connection whose one-way rules forbid that direction relieves nothing. The mover's own ends
                // first (D59 (4), world defaults 826): a discharge that is a generator, or a suction that is a void, is a
                // pocket of one node that only supplies or only receives, so the mover has no legal flow at all.
                if(!boundaryAllowed(graph,pipe,1)||sealedWithout(graph,nodeEdges,modes,boundaryClosed,pipe.first(),e,-1)||sealedWithout(graph,nodeEdges,modes,boundaryClosed,pipe.second(),e,1))
                    {modes.set(e,FlowControl.Mode.CLOSED);atShutoff[e]=true;changed=true;}
            }
            if(!changed)return;
        }
    }
    private static int[][] incidence(PassiveNetwork graph) {
        int nodes=graph.reservoirs().size();int[] degree=new int[nodes];
        for(var pipe:graph.pipes()){degree[pipe.first()]++;degree[pipe.second()]++;}
        int[][] nodeEdges=new int[nodes][];for(int i=0;i<nodes;i++)nodeEdges[i]=new int[degree[i]];Arrays.fill(degree,0);
        for(int e=0;e<graph.pipes().size();e++){var pipe=graph.pipes().get(e);nodeEdges[pipe.first()][degree[pipe.first()]++]=e;nodeEdges[pipe.second()][degree[pipe.second()]++]=e;}
        return nodeEdges;
    }
    /**
     * Whether, with connection {@code edge} left out, junction {@code node} lies in a sealed pocket: the junctions it reaches
     * through pressure-coupling connections ({@link #couples}) touch no node that is not a junction, no regulating valve
     * draws from them, and every other connection leaving them is closed. Whatever {@code edge} carries into such a pocket
     * its mass rows send back out through {@code edge} alone, so {@code edge} carries exactly zero at every root.
     */
    static boolean sealedWithout(PassiveNetwork graph,int[][] nodeEdges,List<FlowControl.Mode> modes,boolean[] boundaryClosed,int node,int edge) {
        return sealedWithout(graph,nodeEdges,modes,boundaryClosed,node,edge,0);
    }
    /**
     * {@link #sealedWithout} for a mover's side (owner decision D58, A4): with {@code relief} 1 the pocket must let flow out
     * (a mover's discharge), with -1 it must let flow in (its suction), and a connection whose one-way rules
     * ({@link #boundaryAllowed}; a pump, compressor or valve passes only from its first end to its second) forbid that
     * direction counts as closed: a discharge whose only way out is into a generator, or a suction whose only feed is a void
     * or a dry end, is sealed. With 0 every open connection counts, either way.
     */
    static boolean sealedWithout(PassiveNetwork graph,int[][] nodeEdges,List<FlowControl.Mode> modes,boolean[] boundaryClosed,int node,int edge,int relief) {
        if(!graph.reservoirs().get(node).junction())return false;
        var members=new HashSet<Integer>();var queue=new ArrayDeque<Integer>();members.add(node);queue.add(node);
        while(!queue.isEmpty()) {
            int at=queue.remove();
            for(int e:nodeEdges[at]) {
                if(e==edge)continue;var mode=modes.get(e);
                if(closedEdge(mode,boundaryClosed[e]))continue;
                var p=graph.pipes().get(e);int other=p.first()==at?p.second():p.first();
                if(relief!=0&&other!=at) {
                    double flow=(p.first()==at?1:-1)*relief;
                    if(!(p.control() instanceof FlowControl.Passive)&&flow<0||!boundaryAllowed(graph,p,flow))continue;
                }
                if(!couples(mode,boundaryClosed[e])){if(members.contains(other)&&!anchors(mode))continue;return false;}
                if(!graph.reservoirs().get(other).junction())return false;
                if(members.add(other))queue.add(other);
            }
        }
        return true;
    }
    /**
     * Decision D12 (phase-ports batch): a generator never receives, from the cold start's first pass on. On a rate solve
     * with no accepted solve of its structure behind it - the cold start's rate seed ({@code PassiveIntervalSolver}'s
     * {@code coldRateSeed}), whose converged junction states and flows the first steps start from - a passive generator
     * run (the generator's connection and the degree-two junctions after it, the run {@link #closeDeadHeads} decides on)
     * that ends at a junction of degree three or more is closed for this solve when the island's one-way pressure
     * estimate ({@link #oneWayPressures}) puts that junction above the generator by more than {@link #reopenBand}; if any
     * run is closed, the island's junctions start from the estimate's pressures (their own temperature and composition; a
     * Newton starting guess only, the owned inventories are untouched).
     *
     * <p>Why: {@link #closeDeadHeads} skips a run that ends at a junction, and {@link #closeIllegalStarts} waits for an
     * accepted solve because a first solve's junction pressures are the compiler's property guess, not a state. So pass 0
     * of the cold rate seed treated every generator as a two-way fixed-pressure boundary: between generators tens of kPa
     * apart, a through-flow network whose low generators are sinks, most of it on the velocity cap's branch (a row that
     * does not depend on the end pressures), with junction pressure columns near empty. The rate seed failed, the steps
     * started from the compiler's guess with the same two-way generators, and every step was refused at "active-set
     * pass=0" at every step size. Measured on the extreme-topology fixtures: closing the receiving generators from the
     * compiler's junction guess does not converge, a uniform junction pressure with them closed converges only near the
     * solution; the estimate supplies both the closures and the start. Once the rate seed has converged its structure
     * counts as accepted, so the first step's {@link #closeIllegalStarts} closes the generators that seed shows receiving
     * before that step's pass 0 (documentation/2026-09-26-phase-ports-and-compressor/PHASE_PORTS_REVIEW.md, D12).
     *
     * <p>The estimate is a steady state with every held node (vessel as a port, generator, void) at its start pressure; a
     * closure it decides binds this rate solve only, and the step solves decide afresh on the seeded states
     * ({@link #closeIllegalStarts}, the interval solver's {@link #reopenable}). A step solve is not given the closure: on a
     * first step solve without a converged rate seed it was measured insufficient (4 of the 6 full-fixture runs still failed
     * in pass 0). An island in which no generator would receive is solved exactly as before (the estimate changes no state
     * and closes nothing). Islands with an actuator or solids are left to the pass loop, as the start closures leave
     * actuated runs (the device-first rule).
     *
     * <p>An inline filter is a passive connection here (hold class E1 of the phase-ports batch, 2026-09-28): an island
     * with a filter runs this rate solve twice, as the cold rate seed and as {@code SolidEventIntegrator}'s interval-start
     * closure check, whose failure holds the island; excluding it left the two-way generator of the defect above in every
     * filter island, and a 1 atm water generator three blocks below its junction stalled that check at "Newton line search
     * stalled" at every interval (a generator one-way by decision D12). The estimate reads the filter's connection by its
     * block's geometry, a start guess like every other number it uses.
     */
    private PassiveNetwork closeColdReceivingGenerators(PassiveNetwork graph,boolean[] boundaryClosed,boolean[] keep,Runnable checkpoint) {
        if(!rateOnly||acceptedPipes.equals(identities(graph))&&Arrays.equals(acceptedNodeIds,nodeIds(graph)))return graph;
        if(graph.reservoirs().stream().noneMatch(node->node.kind()==PassiveNetwork.NodeKind.GENERATOR)||hasSolids(graph))return graph;
        // An actuated connection is left out of the estimate and of the runs (class E2, A69): the pass loop decides devices first.
        int edges=graph.pipes().size(),nodes=graph.reservoirs().size();
        int[] degree=new int[nodes];
        for(var pipe:graph.pipes()){degree[pipe.first()]++;degree[pipe.second()]++;}
        int[][] nodeEdges=new int[nodes][];
        for(int i=0;i<nodes;i++)nodeEdges[i]=new int[degree[i]];
        int[] filled=new int[nodes];
        for(int edge=0;edge<edges;edge++){var pipe=graph.pipes().get(edge);nodeEdges[pipe.first()][filled[pipe.first()]++]=edge;nodeEdges[pipe.second()][filled[pipe.second()]++]=edge;}
        // Each open generator run onto a junction of degree three or more: its links from the generator on, and its far end.
        var runs=new ArrayList<List<Integer>>();var ends=new ArrayList<int[]>();
        for(int g=0;g<nodes;g++)if(graph.reservoirs().get(g).kind()==PassiveNetwork.NodeKind.GENERATOR)for(int first:nodeEdges[g]) {
            var chain=new ArrayList<Integer>();int at=g,from=first;boolean usable=true;
            while(true) {
                if(boundaryClosed[from]||keep!=null&&keep[from]||!(graph.pipes().get(from).control() instanceof FlowControl.Passive)){usable=false;break;}
                chain.add(from);var link=graph.pipes().get(from);at=link.first()==at?link.second():link.first();
                if(!graph.reservoirs().get(at).junction()||degree[at]!=2)break;
                from=nodeEdges[at][0]==from?nodeEdges[at][1]:nodeEdges[at][0];
            }
            if(usable&&at!=g&&graph.reservoirs().get(at).junction()){runs.add(chain);ends.add(new int[]{g,at});}
        }
        if(runs.isEmpty())return graph;
        double[] pressures=oneWayPressures(graph,boundaryClosed,nodeEdges,checkpoint);
        if(pressures==null)return graph;
        boolean closed=false;
        for(int r=0;r<runs.size();r++) {
            var chain=runs.get(r);int g=ends.get(r)[0],far=ends.get(r)[1];
            var generator=graph.reservoirs().get(g);var port=graph.pipes().get(chain.getFirst()).portAt(g);
            double pg=endPressure(generator,port,generator.state()),pf=pressures[far];
            double drive=pg-pf-endDensity(generator.state(),port)*GRAVITY*(graph.reservoirs().get(far).elevation()-generator.elevation());
            if(drive<-reopenBand(pg,pf)){for(int link:chain)boundaryClosed[link]=true;closed=true;}
        }
        if(!closed)return graph;
        var seeded=new ArrayList<>(graph.reservoirs());
        for(int i=0;i<nodes;i++) {
            var node=graph.reservoirs().get(i);
            if(!node.junction()||Double.isNaN(pressures[i]))continue;
            var state=node.state();
            try{seeded.set(i,new PassiveNetwork.Reservoir(node.id(),node.elevation(),model.flashTP(state.temperature(),pressures[i],PhaseLayout.totalAmounts(state),checkpoint),node.kind(),node.inventory()));}
            catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold outside){/* keeps its guess */}
        }
        return new PassiveNetwork(seeded,graph.pipes(),graph.scheduledTransfers());
    }
    /**
     * The one-way pressure estimate of {@link #closeColdReceivingGenerators}: every junction's pressure at which the
     * island's open connections balance with every held node (a non-junction) at its start end pressure, each connection
     * carrying the flow of {@link #initialMassFlow}'s law in the column of its donor end (the run's loss inverted, capped
     * at the donor's velocity limit) and nothing in a direction {@link #boundaryAllowed} refuses - so a generator below
     * its junction carries nothing. A junction donor holding gas only has its density, and so its cap, scaled with its
     * pressure (ideal gas at its own temperature): the junctions start from the compiler's guess, not a state, and a cap
     * read at the guessed density put the grid's junctions 10 kPa too high; it is also the one pressure dependence a
     * capped junction outflow has, so a junction whose connections are all capped keeps a pressure column. Solved by a
     * damped Newton on the junction pressures alone (per-connection difference derivatives), the iterate held within the
     * held pressures' hull widened by the island's largest static column. Returns per node the estimated pressure of a
     * junction some held node reaches, NaN elsewhere, or null if the Newton does not converge (nothing is then closed).
     */
    private double[] oneWayPressures(PassiveNetwork graph,boolean[] boundaryClosed,int[][] nodeEdges,Runnable checkpoint) {
        int edges=graph.pipes().size(),nodes=graph.reservoirs().size();
        boolean[] open=new boolean[edges];
        for(int e=0;e<edges;e++)open[e]=!boundaryClosed[e]&&graph.pipes().get(e).blockedDirections()!=3&&graph.pipes().get(e).control() instanceof FlowControl.Passive;
        // The unknowns: junctions a held node reaches through open connections.
        int[] unknown=new int[nodes];Arrays.fill(unknown,-1);int n=0;
        var queue=new ArrayDeque<Integer>();boolean[] seen=new boolean[nodes];
        for(int i=0;i<nodes;i++)if(!graph.reservoirs().get(i).junction()){seen[i]=true;queue.add(i);}
        while(!queue.isEmpty()){int at=queue.remove();
            for(int e:nodeEdges[at])if(open[e]){var pipe=graph.pipes().get(e);int other=pipe.first()==at?pipe.second():pipe.first();
                if(!seen[other]){seen[other]=true;unknown[other]=n++;queue.add(other);}}}
        if(n==0)return null;
        // The unknowns are numbered in breadth-first order from the held nodes, so a connection joins two unknowns at most
        // a front's width apart: the Jacobian is banded and its elimination costs n b^2, not n^3.
        int band=0;
        for(int e=0;e<edges;e++){var pipe=graph.pipes().get(e);if(open[e]&&unknown[pipe.first()]>=0&&unknown[pipe.second()]>=0)band=Math.max(band,Math.abs(unknown[pipe.first()]-unknown[pipe.second()]));}
        var estimate=new OneWay(graph,open,unknown);
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,zLow=Double.POSITIVE_INFINITY,zHigh=Double.NEGATIVE_INFINITY,rhoMax=0,scale=0;
        double[] p=new double[nodes];
        for(int i=0;i<nodes;i++){var node=graph.reservoirs().get(i);p[i]=node.state().pressure();zLow=Math.min(zLow,node.elevation());zHigh=Math.max(zHigh,node.elevation());
            estimate.gas[i]=node.junction()&&node.state().liquidVolume()==0&&node.state().waterVolume()==0&&node.state().pressure()>0;estimate.reference[i]=node.state().pressure();}
        for(int e=0;e<edges;e++) {
            if(!open[e])continue;var pipe=graph.pipes().get(e);
            for(int dir=0;dir<2;dir++) {
                int donor=dir==0?pipe.first():pipe.second();var node=graph.reservoirs().get(donor);var port=pipe.portAt(donor);
                estimate.rho[e][dir]=endDensity(node.state(),port);estimate.mu[e][dir]=endViscosity(node.state(),port);estimate.cap[e][dir]=massFlowLimit(pipe,node.state(),port);
                estimate.allowed[e][dir]=boundaryAllowed(graph,pipe,dir==0?1:-1);
                rhoMax=Math.max(rhoMax,estimate.rho[e][dir]);scale=Math.max(scale,estimate.cap[e][dir]);
                estimate.endPressures[e][dir]=endPressure(graph,pipe,donor);
                if(!node.junction()){low=Math.min(low,estimate.endPressures[e][dir]);high=Math.max(high,estimate.endPressures[e][dir]);}
            }
        }
        if(!(high>=low))return null;
        double margin=rhoMax*GRAVITY*(zHigh-zLow)+1;low=Math.max(low-margin,1e-3*high);high+=margin;
        for(int i=0;i<nodes;i++)if(unknown[i]>=0)p[i]=Math.clamp(p[i],low,high);
        double[] residual=new double[n];
        double merit=estimate.residual(p,residual,null,band);
        double tolerance=1e-9*Math.max(1,scale);
        int sweeps=0;
        for(int iteration=0;iteration<200;iteration++) {
            checkpoint.run();
            if(maximum(residual)<=tolerance)return oneWayResult(graph,unknown,p);
            double[][] jacobian=new double[n][2*band+1];estimate.residual(p,new double[n],jacobian,band);
            double floor=0;for(int i=0;i<n;i++)floor=Math.max(floor,Math.abs(jacobian[i][band]));floor=Math.max(floor*1e-12,Double.MIN_NORMAL);
            for(int i=0;i<n;i++)if(Math.abs(jacobian[i][band])<floor)jacobian[i][band]=-floor;
            double[] step=solveBanded(jacobian,band,residual);
            boolean accepted=false;
            if(step!=null) {
                // A step at most half the hull, then halved until the squared residual falls.
                double largest=0;for(double s:step)largest=Math.max(largest,Math.abs(s));
                double limit=Math.min(1,.5*(high-low)/Math.max(largest,Double.MIN_NORMAL));
                double[] trial=p.clone(),trialResidual=new double[n];
                for(double fraction=limit;fraction>1e-6*limit;fraction*=.5) {
                    for(int i=0;i<nodes;i++)if(unknown[i]>=0)trial[i]=Math.clamp(p[i]-fraction*step[unknown[i]],low,high);
                    double trialMerit=estimate.residual(trial,trialResidual,null,band);
                    if(trialMerit<(1-1e-4*fraction)*merit){System.arraycopy(trial,0,p,0,nodes);System.arraycopy(trialResidual,0,residual,0,n);merit=trialMerit;accepted=true;break;}
                }
            }
            // Where the Newton step does not descend (a cap or a one-way kink), one Gauss-Seidel sweep: each junction in turn
            // to the pressure at which its own net inflow vanishes, its neighbours held. The net inflow is nonincreasing in
            // the junction's pressure, so a sweep never moves away from the solution.
            if(!accepted) {
                if(++sweeps>100)return null;
                estimate.sweep(p,low,high);merit=estimate.residual(p,residual,null,band);
            }
        }
        return maximum(residual)<=tolerance?oneWayResult(graph,unknown,p):null;
    }
    private static double maximum(double[] values){double m=0;for(double v:values)m=Math.max(m,Math.abs(v));return m;}
    private static double[] oneWayResult(PassiveNetwork graph,int[] unknown,double[] p) {
        double[] result=new double[p.length];
        for(int i=0;i<p.length;i++)result[i]=unknown[i]>=0?p[i]:graph.reservoirs().get(i).junction()?Double.NaN:p[i];
        return result;
    }
    /** The connection law and the held data of {@link #oneWayPressures}. */
    private static final class OneWay {
        final PassiveNetwork graph;final boolean[] open;final int[] unknown;
        final double[][] rho,mu,cap,endPressures;final boolean[][] allowed;final boolean[] gas;final double[] reference;
        OneWay(PassiveNetwork graph,boolean[] open,int[] unknown) {
            this.graph=graph;this.open=open;this.unknown=unknown;int edges=open.length,nodes=unknown.length;
            rho=new double[edges][2];mu=new double[edges][2];cap=new double[edges][2];endPressures=new double[edges][2];allowed=new boolean[edges][2];
            gas=new boolean[nodes];reference=new double[nodes];
        }
        /** Half the squared net inflows of the unknown junctions at {@code p} (into {@code residual}); with {@code jacobian},
         * their derivatives in the junction pressures by per-connection differences, in band storage (row i, column j at
         * {@code [i][j - i + band]}). */
        double residual(double[] p,double[] residual,double[][] jacobian,int band) {
            Arrays.fill(residual,0);
            for(int e=0;e<open.length;e++) {
                if(!open[e])continue;var pipe=graph.pipes().get(e);int a=pipe.first(),b=pipe.second();
                double pa=unknown[a]>=0?p[a]:endPressures[e][0],pb=unknown[b]>=0?p[b]:endPressures[e][1];
                double q=flow(e,pa,pb);
                if(unknown[a]>=0)residual[unknown[a]]-=q;
                if(unknown[b]>=0)residual[unknown[b]]+=q;
                if(jacobian==null)continue;
                for(int end=0;end<2;end++) {
                    int node=end==0?a:b,column=unknown[node];if(column<0)continue;
                    double at=end==0?pa:pb,delta=1e-7*Math.max(1,Math.abs(at));
                    double dq=((end==0?flow(e,pa+delta,pb):flow(e,pa,pb+delta))-q)/delta;
                    if(unknown[a]>=0)jacobian[unknown[a]][column-unknown[a]+band]-=dq;
                    if(unknown[b]>=0)jacobian[unknown[b]][column-unknown[b]+band]+=dq;
                }
            }
            double merit=0;for(double r:residual)merit+=r*r;
            return .5*merit;
        }
        /** One Gauss-Seidel sweep over the unknown junctions in index order (a 60-step bisection each within the hull). */
        void sweep(double[] p,double low,double high) {
            for(int node=0;node<unknown.length;node++) {
                if(unknown[node]<0)continue;
                double lo=low,hi=high;
                for(int k=0;k<60&&hi-lo>1e-9*high;k++) {
                    double mid=.5*(lo+hi),net=0;p[node]=mid;
                    for(int e=0;e<open.length;e++) {
                        if(!open[e])continue;var pipe=graph.pipes().get(e);if(pipe.first()!=node&&pipe.second()!=node)continue;
                        int a=pipe.first(),b=pipe.second();
                        double q=flow(e,unknown[a]>=0?p[a]:endPressures[e][0],unknown[b]>=0?p[b]:endPressures[e][1]);
                        net+=b==node?q:-q;
                    }
                    if(net>0)lo=mid;else hi=mid;
                }
                p[node]=.5*(lo+hi);
            }
        }
        /** The connection's flow at end pressures {@code pa}, {@code pb}: in the column of the donor the direction draws
         * from, nothing in a refused direction or between the two columns. */
        double flow(int e,double pa,double pb) {
            var pipe=graph.pipes().get(e);
            double dz=graph.reservoirs().get(pipe.second()).elevation()-graph.reservoirs().get(pipe.first()).elevation();
            double forwardScale=scale(pipe.first(),pa),reverseScale=scale(pipe.second(),pb);
            double forward=pa-pb-rho[e][0]*forwardScale*GRAVITY*dz;
            if(forward>0)return allowed[e][0]?runFlow(pipe,forward,rho[e][0]*forwardScale,mu[e][0],cap[e][0]*forwardScale):0;
            double reverse=pa-pb-rho[e][1]*reverseScale*GRAVITY*dz;
            if(reverse<0)return allowed[e][1]?-runFlow(pipe,-reverse,rho[e][1]*reverseScale,mu[e][1],cap[e][1]*reverseScale):0;
            return 0;
        }
        /** The density ratio of a gas-only junction donor at pressure {@code pressure}; 1 for any other donor. */
        private double scale(int node,double pressure){return unknown[node]>=0&&gas[node]?pressure/reference[node]:1;}
        /** The flow a run passes at a positive driving pressure: its loss inverted, capped at {@code cap}. */
        private static double runFlow(PassiveNetwork.Pipe pipe,double driving,double rho,double mu,double cap) {
            if(!(cap>0))return 0;
            double capLoss=pipe.pressureDrop(cap,rho,mu);
            if(capLoss<=driving)return cap;
            double lo=0,hi=cap,q=cap*Math.sqrt(driving/capLoss);
            for(int k=0;k<100;k++) {
                var loss=pipe.loss(q,rho,mu);double f=loss.pressureDrop()-driving;
                if(Math.abs(f)<=1e-14*driving)break;
                if(f>0)hi=q;else lo=q;
                double next=q-f/loss.massFlowDerivative();q=next>lo&&next<hi?next:.5*(lo+hi);
                if(hi-lo<=1e-15*cap)break;
            }
            return q;
        }
    }
    /** Banded Gaussian elimination without pivoting ({@code a} in band storage of half-width {@code band}, overwritten):
     * the solution of {@code a x = b}, or null at a pivot that vanishes against its row. The estimate's Jacobian is a
     * weighted graph Laplacian (diagonally dominant up to the gas scaling), which needs no pivoting. */
    private static double[] solveBanded(double[][] a,int band,double[] rightHandSide) {
        int n=rightHandSide.length;double[] b=rightHandSide.clone();
        for(int column=0;column<n;column++) {
            double pivot=a[column][band],size=0;
            for(int k=0;k<=2*band;k++)size=Math.max(size,Math.abs(a[column][k]));
            if(!(Math.abs(pivot)>1e-14*size))return null;
            for(int row=column+1;row<=Math.min(n-1,column+band);row++) {
                double factor=a[row][column-row+band]/pivot;if(factor==0)continue;
                for(int k=column;k<=Math.min(n-1,column+band);k++)a[row][k-row+band]-=factor*a[column][k-column+band];
                b[row]-=factor*b[column];
            }
        }
        double[] x=new double[n];
        for(int row=n-1;row>=0;row--){double sum=b[row];for(int k=row+1;k<=Math.min(n-1,row+band);k++)sum-=a[row][k-row+band]*x[k];x[row]=sum/a[row][band];}
        return x;
    }
    /**
     * Whether the static driving pressure {@code P_a - P_b - rho g (z_b - z_a)} has the sign of {@code flow} with rho = either endpoint's density.
     * {@link #initialMassFlow}, which supplies the direction of a connection the previous point had closed, states
     * the row's column ({@link #columnFromFirst}), and where the pipe could stand full of either fluid that is the
     * column of the endpoint with the higher pressure. On a falling water line into a nitrogen tank that is the
     * tank's gas once the tank passes the generator's pressure, so the estimate reads a 4 m water column as 4 m of
     * nitrogen and reports a 4.8 kg/s backflow into the generator while the column still drives 4.2 kPa forward
     * (ElevatedBlockLineIslandTest, run 76a); the connection then stays closed for good, because a closed connection
     * records zero and the next start reads the same estimate. Between two gas endpoints (the void edges of review
     * 7.8) the two densities give the same sign and the rule fires as before.
     */
    private boolean illegalWithEitherDensity(PassiveNetwork graph,PassiveNetwork.Pipe pipe,double flow) {
        var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());
        double dp=endPressure(a,pipe.firstPort(),a.state())-endPressure(b,pipe.secondPort(),b.state()),dz=b.elevation()-a.elevation();
        // Each end's density is what its own port draws first (decision D11; the bulk when it holds no phase).
        double drivingFirst=dp-endDensity(a.state(),pipe.firstPort())*GRAVITY*dz;if(!(Math.signum(drivingFirst)==Math.signum(flow)))return false;
        double drivingSecond=dp-endDensity(b.state(),pipe.secondPort())*GRAVITY*dz;if(!(Math.signum(drivingSecond)==Math.signum(flow)))return false;
        return true;
    }
    /**
     * Whether a connection may carry {@code flow}'s direction: not blocked that way, not out of a void or into a generator,
     * not out of a dry end ({@link #dryEnd}). A phase port never refuses a direction for want of its first phase (decision
     * D11): a vessel that lacks it draws the next one of the port's priority order ({@link PhaseDraw}).
     */
    static boolean boundaryAllowed(PassiveNetwork graph,PassiveNetwork.Pipe pipe,double flow) {
        var a=graph.reservoirs().get(pipe.first()).kind();var b=graph.reservoirs().get(pipe.second()).kind();
        return !pipe.blocked(flow)&&!(flow<0&&(a==PassiveNetwork.NodeKind.GENERATOR||b==PassiveNetwork.NodeKind.VOID)
                ||flow>0&&(b==PassiveNetwork.NodeKind.GENERATOR||a==PassiveNetwork.NodeKind.VOID))
                &&!outOfDryEnd(graph,pipe,flow);
    }
    /**
     * Whether {@code flow} leaves its connection through a dry end ({@link #dryEnd}). Such an end gives nothing in the
     * step: a larger flow is a forbidden direction ({@link #boundaryAllowed}), and one within that rule's tolerance,
     * 1e-10 kg/s, is no flow, at a converged point and at a polished one alike. A vessel that starts the step holding
     * nothing has nothing to book out, whether the pass laid it out dry or laid it out from what reached it (random seed
     * 708: an empty tank looped twice to one junction converged with 1.6e-22 kg/s in by one line and out by the other,
     * and the booking, which starts from nothing, ended it at zero at every step size).
     */
    static boolean outOfDryEnd(PassiveNetwork graph,PassiveNetwork.Pipe pipe,double flow) {
        return flow>0&&dryEnd(graph.reservoirs().get(pipe.first()),pipe.firstPort())||flow<0&&dryEnd(graph.reservoirs().get(pipe.second()),pipe.secondPort());
    }
    /**
     * Decision D54 (every vessel a bag with a stiff wall): an end with nothing behind it to draw. A vessel that holds no
     * fluid ({@link #holdsNoFluid}: placed empty, or holding only solids, which ride the liquid while a dry gas carries
     * none, decision D33) has nothing to carry anything out, so all its ports are dry; a top (VAPOR) port of a slack vessel
     * that holds no gas is dry too: the liquid stands below it at the ambient, and what is above it is the atmosphere the
     * model does not carry. The model never draws air.
     */
    static boolean dryEnd(PassiveNetwork.Reservoir node,PassiveNetwork.PhasePort port) {
        if(node.kind()!=PassiveNetwork.NodeKind.RESERVOIR&&node.kind()!=PassiveNetwork.NodeKind.PORT)return false;
        if(holdsNoFluid(node))return true;
        return port==PassiveNetwork.PhasePort.VAPOR&&!FluidThermodynamics.holdsPhase(node.state(),FluidThermodynamics.GAS)
                &&!PhaseLayout.taut(node.state(),node.inventory().volume());
    }
    /**
     * A vessel, or its rate-solve copy, that holds no fluid (decision D54: placed empty, or holding only solids): its fluid,
     * the bag less its solids, at most the bag's resolution, {@link #HOLDS_NOTHING} of its capacity (owner decision D59 (2),
     * t28). That resolution is the one at which the bag's volume is already snapped to the capacity (A311,
     * {@link PhaseLayout#bagVolume}); below it a vessel's fluid is roundoff, typically a residue a junction's buffer leaves in
     * an emptied tank (world defaults 322: 1.3e-71 kg; 333: 2.2e-17 kg). Solids ride the liquid and a dry gas carries none
     * (owner decision D33), and without a fluid they have nothing to exchange with (OD-P4), so whatever solids it holds,
     * such a vessel is neither a feed nor stock and has no fluid to state: an end of it gives nothing ({@link #dryEnd}), its
     * column is the other end's, a junction seed's sweep passes through it, the rest test does not count it, a step lays it
     * out dry ({@link #dry}), and the booking states it at its flash with no closure ({@link InventoryEquilibrium#solve}).
     * One rule where holding nothing was the test before (decision D54 of a vessel placed empty). The residue stays booked
     * in the vessel's inventory (the ledger is exact, D46): nothing is deleted or flushed. A mover's dry suction keeps its
     * port band (A334, {@link InletPhase}).
     */
    static boolean holdsNoFluid(PassiveNetwork.Reservoir node) {
        if(node.kind()!=PassiveNetwork.NodeKind.RESERVOIR&&node.kind()!=PassiveNetwork.NodeKind.PORT)return false;
        return holdsNoFluid(node.inventory(),node.state());
    }
    /** {@link #holdsNoFluid(PassiveNetwork.Reservoir)} for a vessel's {@code inventory} at {@code state}. */
    static boolean holdsNoFluid(PassiveNetwork.Inventory inventory,FluidThermodynamics.State state) {
        boolean none=true;for(double n:inventory.moles())if(n!=0){none=false;break;}
        if(none)return true;
        double capacity=inventory.volume(),bag=PhaseLayout.bagVolume(state,capacity);
        return (inventory.solids().empty()?bag:bag-inventory.solids().volume())<=HOLDS_NOTHING*capacity;
    }
    /**
     * A vessel a step lays out dry when nothing arrives in it (decision D54) and books nothing for when nothing reaches it:
     * one that holds no fluid at the bag's resolution (owner decision D59 (2)), whatever solids it holds. A residue below
     * it has no phases worth stating: random seed 708 kept a 4e-33 kg crude residue in a tank and a pass laid out another
     * tank from a 2e-29 kg roundoff arrival, both as two phases, and their fugacity rows, stated for that roundoff, stalled
     * the line search at every step size. Solids without a fluid neither move nor change in a step (they ride the liquid,
     * decision D33, and have no fluid to exchange with, OD-P4): a bed of them has no unknowns until a fluid arrives, and a
     * layout of it with none found no state to decode at all ("Empty/nonfinite fluid properties").
     */
    static boolean holdsNoFluidVessel(PassiveNetwork.Reservoir node) {
        return node.kind()==PassiveNetwork.NodeKind.RESERVOIR&&holdsNoFluid(node);
    }
    /** The bag's resolution as a share of its capacity: the share below which a vessel holds no fluid (D59 (2), A311). */
    static final double HOLDS_NOTHING=1e-12;
    /**
     * Decision D54 (a vessel is a bag; a bag can be emptied): {@code graph} with every vessel that runs dry within the step
     * ({@code emptying}) standing as a junction that holds its stock. A vessel drained to nothing ends the step with no
     * contents, so it has no state of its own to solve for at the step's end; what leaves it over the step is what it held
     * and what reached it, mixed (the junction's backward-Euler rows with the stock as their held term), at the pressure its
     * outlets drain it at, at most the ambient (the outlets run partly full once its contents are gone). The junction's
     * buffer is the vessel's inventory, its energy field in enthalpy form the vessel's energy plus the ambient's work on its
     * collapsing bag, {@code U + P_ambient V_bag}; its continuity row gains the stock's mass over the step
     * ({@link #emptyings}), and its ports draw the bulk (everything leaves). The vessel's own record is untouched: the
     * reconstruction books it to nothing ({@link ConservativeTransport.Emptying}).
     */
    private static PassiveNetwork emptyingGraph(PassiveNetwork graph,boolean[] emptying) {
        var nodes=new ArrayList<>(graph.reservoirs());
        for(int i=0;i<emptying.length;i++)if(emptying[i]) {
            var node=graph.reservoirs().get(i);var stock=node.inventory();
            double work=PhaseLayout.AMBIENT_PRESSURE*PhaseLayout.bagVolume(node.state(),stock.volume());
            nodes.set(i,new PassiveNetwork.Reservoir(node.id(),node.elevation(),node.state(),PassiveNetwork.NodeKind.JUNCTION,
                    new PassiveNetwork.Inventory(stock.volume(),stock.moles(),stock.internalEnergy()+work,stock.solids())));
        }
        var pipes=new ArrayList<PassiveNetwork.Pipe>(graph.pipes().size());
        for(var pipe:graph.pipes())pipes.add(emptying[pipe.first()]||emptying[pipe.second()]
                ?pipe.withPorts(emptying[pipe.first()]?PassiveNetwork.PhasePort.BULK:pipe.firstPort(),emptying[pipe.second()]?PassiveNetwork.PhasePort.BULK:pipe.secondPort()):pipe);
        return new PassiveNetwork(nodes,pipes,graph.scheduledTransfers());
    }
    /** Per node of {@code graph}, the stock an emptying vessel passes over the step and the ambient's work on its bag. */
    private ConservativeTransport.Emptying[] emptyings(PassiveNetwork graph,boolean[] emptying,double dt) {
        var result=new ConservativeTransport.Emptying[emptying.length];
        for(int i=0;i<emptying.length;i++)if(emptying[i]) {
            var node=graph.reservoirs().get(i);var stock=node.inventory();
            result[i]=new ConservativeTransport.Emptying((molarMass(stock.moles())+stock.solids().massKg())/dt,
                    PhaseLayout.AMBIENT_PRESSURE*PhaseLayout.bagVolume(node.state(),stock.volume()));
        }
        return result;
    }
    /** This pass's emptying vessels ({@link #emptyingGraph}), read by the junction continuity rows; null for none. */
    private ConservativeTransport.Emptying[] emptyings;
    /**
     * {@code emptying} with every vessel released that a converged pass puts above the ambient (it holds contents at the
     * step's end after all), or null when none is. A vessel a converged pass of this solve has held at or below the
     * ambient is confirmed in {@code emptied} and never released again within the solve: whether a bag runs dry within
     * the step is decided once, as {@code atShutoff} decides a pump's shutoff corner once (random seed 678: the emptying
     * pass converged at 91.3 kPa, the phase correction laid the vessel's last 3e-17 kg of crude out as two-phase, that pass
     * converged a little above the ambient, the release restored the vessel form, whose Newton has no root, and the pass
     * sequence cycled at every step size).
     */
    private static boolean[] keptEmptying(List<FluidThermodynamics.State> states,boolean[] emptying,boolean[] emptied) {
        boolean[] result=null;
        for(int i=0;i<emptying.length;i++) {
            if(!emptying[i]||emptied[i])continue;
            if(!(states.get(i).pressure()>PhaseLayout.AMBIENT_PRESSURE*(1+1e-9))){emptied[i]=true;continue;}
            if(result==null)result=emptying.clone();
            result[i]=false;
        }
        return result;
    }
    /** The phase code of a dry vessel in a pass's structure key: it carries no unknowns. */
    private static final int DRY=32;
    /**
     * Decision D54: whether a pass lays {@code node} out as dry, with no unknowns. A vessel that holds no fluid at the step
     * start ({@link #holdsNoFluidVessel}), whose seed is still its own state (nothing has been laid out for it), is a
     * receiver at the ambient with no fluid: its state is its pressure and the solids it holds, if any, and it gives
     * nothing (its ends are dry, {@link #dryEnd}). Once the start flows deliver into it ({@link #arrivingPhases}) or a pass's
     * flows reach it ({@link #filled}), its seed is what arrives and it is laid out like every vessel.
     */
    static boolean dry(PassiveNetwork.Reservoir node,FluidThermodynamics.State seed){return holdsNoFluidVessel(node)&&seed==node.state();}
    /** Whether a converged pass's reconstruction books a fluid into a vessel the pass laid out dry: more than none at the
     * bag's resolution ({@link #holdsNoFluid}); a roundoff it receives stays booked and the vessel stays dry. */
    private static boolean filled(Equations equations,ConservativeTransport.Projection projection) {
        for(int node=0;node<equations.dry.length;node++)
            if(equations.dry[node]&&!holdsNoFluid(projection.inventories().get(node),projection.states().get(node)))return true;
        return false;
    }
    /**
     * The step's ledger (owner decision D46): the change of the vessels and filter cakes equals the boundary transfers
     * plus the junction term, per component, per solid population and for energy (pump work included). The junction term
     * is the change of the junctions' buffers (a junction's buffer is the state of its relaxation row, m_J w and m_J h,
     * not inventory) plus what a rate evaluation's frozen junctions held back ({@link ConservativeTransport.HeldJunctions}).
     * Each tolerance is scaled as it always was, by the boundaries' turnover and the stocks held, buffers included.
     */
    void checkConservation(PassiveNetwork graph,ConservativeTransport.Projection projection) {
        int count=model.hydrocarbon.componentCount()+1;double[] before=new double[count],after=new double[count],turnover=new double[count];double eb=0,ea=0,energyScale=0;
        double[] junction=projection.held().moles(),bufferedBefore=new double[count],bufferedAfter=new double[count];double junctionEnergy=projection.held().energyJoule();
        for(var boundary:projection.boundaries()){var n=boundary.moles();for(int c=0;c<count;c++)turnover[c]+=Math.abs(n[c]);energyScale+=Math.abs(boundary.totalEnergyJoule());}
        // A rate evaluation's held accumulation was a pseudo-boundary at each junction before D46 and counts in the turnover
        // as it did, junction by junction.
        var gross=projection.held().grossMoles();for(int c=0;c<count;c++)turnover[c]+=gross[c];energyScale+=projection.held().grossEnergyJoule();
        for(int i=0;i<graph.reservoirs().size();i++) {
            var reservoir=graph.reservoirs().get(i);var old=reservoir.inventory();var next=projection.inventories().get(i);
            if(reservoir.fixed())continue;
            var a=old.moles();var b=next.moles();double ma=old.solids().massKg(),mb=next.solids().massKg();
            for(int j=0;j<count;j++){double mw=model.molecularWeight(j);ma+=a[j]*mw;mb+=b[j]*mw;}
            double ebNode=old.internalEnergy()+ma*GRAVITY*reservoir.elevation(),eaNode=next.internalEnergy()+mb*GRAVITY*reservoir.elevation();
            if(reservoir.junction()){for(int j=0;j<count;j++){junction[j]+=b[j]-a[j];bufferedBefore[j]+=a[j];bufferedAfter[j]+=b[j];}junctionEnergy+=eaNode-ebNode;}
            else{for(int j=0;j<count;j++){before[j]+=a[j];after[j]+=b[j];}eb+=ebNode;ea+=eaNode;}
            energyScale+=Math.abs(old.internalEnergy())+Math.abs(ma*GRAVITY*reservoir.elevation());
        }
        // Per population: vessels and cakes before [0] and after [1], boundaries [2], buffers before [3] and after [4], and a
        // rate evaluation's held accumulation [5] (net, signed as the junction term).
        var solidBalance=new TreeMap<SolidInventory.Key,double[]>();
        for(int i=0;i<graph.reservoirs().size();i++)if(!graph.reservoirs().get(i).fixed()){
            int buffer=graph.reservoirs().get(i).junction()?3:0;
            for(var p:graph.reservoirs().get(i).inventory().solids().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[buffer]+=p.massKg();
            for(var p:projection.inventories().get(i).solids().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[buffer+1]+=p.massKg();
        }
        for(var p:projection.held().gained().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[5]+=p.massKg();
        for(var p:projection.held().lost().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[5]-=p.massKg();
        for(var pipe:graph.pipes())if(pipe.filter()!=null){
            var old=pipe.filter();var next=projection.filters().getOrDefault(pipe.id(),old);eb+=old.energyJoule();ea+=next.energyJoule();energyScale+=Math.abs(old.energyJoule())+Math.abs(next.energyJoule());
            for(var p:old.captured().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[0]+=p.massKg();
            for(var p:next.captured().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[1]+=p.massKg();
        }
        for(var boundary:projection.boundaries())for(var p:boundary.solids().populations())solidBalance.computeIfAbsent(p.key(),k->new double[6])[2]+=boundary.solidDirection()*p.massKg();
        for(var b:solidBalance.values())if(Math.abs(b[0]+b[2]-b[1]-(b[4]-b[3]+b[5]))>1e-10+1e-8*Math.max(Math.max(b[0]+b[3],b[1]+b[4]),Math.abs(b[2]-b[5])))throw new SparseNewton.Nonconvergence("Solid population balance failed");
        var external=projection.externalMoles();
        for(int i=0;i<count;i++)if(Math.abs(before[i]+external[i]-after[i]-junction[i])>1e-10+1e-8*Math.max(turnover[i],Math.max(before[i]+bufferedBefore[i],after[i]+bufferedAfter[i])))throw new SparseNewton.Nonconvergence("Component balance failed: "+i);
        if(Math.abs(ea-eb+junctionEnergy-projection.pumpWork()-projection.externalEnergy())>1e-4+1e-6*(energyScale+Math.abs(projection.pumpWork())))throw new SparseNewton.Nonconvergence("Total energy balance failed");
    }
    private double carrierViscosity(FluidThermodynamics.State s) {
        if(!FluidThermodynamics.holdsFluid(s))return carrierViscosity(ambient());
        double value=0,volume=s.liquidVolume()+s.waterVolume()+s.vaporVolume();
        if(s.liquidVolume()>0)value+=s.liquidVolume()*model.viscosity.liquid(s.temperature(),s.liquidView()).pascalSeconds();
        if(s.waterVolume()>0)value+=s.waterVolume()*model.viscosity.waterLiquid(s.temperature());
        if(s.vaporVolume()>0)value+=s.vaporVolume()*model.viscosity.vapor(s.temperature(),s.vaporView(),s.waterVapor());
        return value/volume;
    }
    static boolean hasSolids(PassiveNetwork graph) {
        return graph.reservoirs().stream().anyMatch(n->n.state().solidMoments().mass()>0||!n.inventory().solids().empty())
                ||graph.scheduledTransfers().stream().anyMatch(t->t instanceof ScheduledTransfer.Injection in&&!in.solidsPerSecond().empty());
    }
    private double viscosity(FluidThermodynamics.State s) {
        return viscosity(s,null);
    }
    /** The ambient's state ({@link NetworkReduction#ambient}), read for an end that holds no fluid ({@link #viscosity}). */
    private FluidThermodynamics.State ambient;
    private FluidThermodynamics.State ambient(){if(ambient==null)ambient=NetworkReduction.ambient(model);return ambient;}
    /** {@code prepared} must be this state's own temperature bundle, or {@code null} to evaluate
     * the pure-component terms here as the standalone paths do.
     *
     * <p>A state holding no fluid (a vessel holding only solids, decision D33) has no stream: its ends are dry and pass
     * nothing ({@link #dryEnd}). The solver still reads a stream at every end where no flow passes (each node's at every
     * decode, a closed connection's row, a start estimate, a row's scale), and there it reads an empty vessel's, the
     * ambient's (decision D54: the state an empty vessel holds), not the 0/0 of no fluid ({@link #carrierViscosity} too). */
    private double viscosity(FluidThermodynamics.State s,FluidThermodynamics.Prepared prepared) {
        if(!FluidThermodynamics.holdsFluid(s))return viscosity(ambient(),null);
        var terms=prepared==null?null:prepared.viscosities();
        double value=0;
        if(s.liquidVolume()>0)value+=s.liquidVolume()*model.viscosity.liquid(s.temperature(),s.liquidView(),terms).pascalSeconds();
        if(s.waterVolume()>0)value+=s.waterVolume()*model.viscosity.waterLiquid(s.temperature(),terms);
        double liquidVolume=s.liquidVolume()+s.waterVolume(),solidVolume=s.solidMoments().volume();
        if(liquidVolume>0&&solidVolume>0){double phi=solidVolume/(liquidVolume+solidVolume);value=SlurryTransport.effectiveViscosity(value/liquidVolume,Math.min(phi,0.62-1e-9))*(liquidVolume+solidVolume);}
        if(s.vaporVolume()>0)value+=s.vaporVolume()*model.viscosity.vapor(s.temperature(),s.vaporView(),s.waterVapor(),viscosities,terms);
        return value/(liquidVolume==0?s.vaporVolume():s.volume());
    }
    /** The equations of the last accepted solve, for the check that the block Jacobian sweep and an
     * independent column-by-column difference of {@link Equations#residual} produce the same entries;
     * {@code null} until a solve has been accepted. Nothing in production reads it. */
    /** Drops the retained last solve; the interval solver does after every accepted step. */
    void releaseLastSolve(){lastSolve=null;}
    Equations acceptedEquations(){return lastSolve==null?null:lastSolve.equations();}
    /** The point {@link #acceptedEquations} converged to. */
    double[] acceptedPoint(){return lastSolve==null?null:lastSolve.variables().clone();}
    /** The rows a block Jacobian sweep has to re-evaluate for one perturbed node or edge column. */
    final class Equations implements SparseNewton.Equations {
        final double[][] solidTargets,solidIncoming;
        final int[] filterOffsets;
        /** The connections as the warm-flow check may compare them: never including the cake. They are the solve's own
         * connections, also in a pass that stands an emptying vessel as a junction (decision D54, {@link #emptyingGraph}):
         * that pass draws the vessel's bulk through the same connections, so it starts from the same carried flows as
         * every other pass of the step (random seed 257: from the static estimate instead, the drain to a void below
         * started inward, stood in the void's air, and the emptying pass converged with the vessel above the ambient). */
        final List<PassiveNetwork.Pipe.Identity> pipeIdentities;
        final PassiveNetwork graph;final double dt;final PhaseLayout[] layout;final int[] offsets;
        /** Per node, the frozen phase support its layout states (null for a fixed node): the equilibrium the reconstruction
         * closes each vessel to ({@link InventoryEquilibrium#close}), so the equation gate checks it in its own model. */
        final PhaseSupport[][] supports;
        /** Per node, whether this pass lays it out dry ({@link PassiveStepSolver#dry}): no unknowns, its state its own. */
        final boolean[] dry;
        /** Per vessel, the bag volume of the state the step starts from ({@link PhaseLayout#bagVolume}; zero for an empty
         * one): its energy row books the wall's work from it (decision D54). */
        final double[] bagStart;
        /** Per vessel, the wall's branches ({@link PhaseLayout#wall}) at the point the Jacobian in use was built at (decision
         * A423): {@code sweepWall} is read by the rows while a build is bracketed ({@link #linearising}), null otherwise;
         * {@code builtWall} by the chord test ({@link #linearisationHolds}). Null for a junction and for a dry vessel. */
        PhaseLayout.Wall[] sweepWall,builtWall;
        final int edgeOffset,size;final double[][] oldAmounts;int[][] sparsity;final List<FlowControl.Mode> modes;final int[] controlOffsets;final boolean[] boundaryClosed;final List<FluidThermodynamics.State> seeds;
        final double[][] cachedVariables;final FluidThermodynamics.State[] cachedStates;final Transport[] cachedTransport;
        /** Which nodes an end draws phases of ({@link PassiveNetwork.PhasePort} VAPOR or LIQUID) through a connection this pass
         * leaves open: only those nodes build their phase streams at each decode, and only their non-amount columns keep
         * their material-row entries ({@link #buildSparsity}). A passive connection the boundary rule has closed carries
         * exactly zero by its own row ({@code f = q}) at every iterate, so its donor terms are zero whatever it would draw,
         * and it neither builds a stream nor costs its node a structural zero. A graph with no open phase port has none and
         * is solved as it always was. */
        final boolean[] drawsPhase;
        /**
         * Each vessel's per-phase capacity (decision D11; {@link PhaseDraw}), kg/s by phase id ({@link FluidThermodynamics#GAS},
         * {@link FluidThermodynamics#OIL}, {@link FluidThermodynamics#WATER}), for a node with a phase port whose connection
         * may carry outflow from it; null for every other node:
         * <pre>c = m_start / (N dt)</pre>
         * with {@code m_start} the phase stream's mass on the accepted state the step starts from (its solid share included;
         * zero for a phase it does not hold), {@code N} the vessel's phase-port ends whose connection may carry outflow from
         * it by the static rule ({@link #boundaryAllowed}: not blocked, not into a generator; start-of-solve closures do not
         * change it, so it is a constant of the solve), an equal share of what the vessel holds, and {@code dt} this solve's
         * step - the backward-Euler step the interval solver attempts, the interval's remainder when that is shorter, halved
         * with every retry - never the slice. A rate solve has no step: every capacity is
         * infinite, and a port draws the first phase of its order its vessel holds.
         */
        final double[][] capacities;
        /** Which phases each node's seed of this pass holds ({@link PhaseDraw#held}). */
        final boolean[][] seedHeld;
        /** The phase streams of each node with a phase port on the accepted state the step starts from, by phase id (null
         * for a phase it does not hold): what a pinned phase is drawn at ({@link PhaseDraw}). */
        final Transport[][] startPhases;
        /**
         * The phase-port segment of each connection end for this pass ({@link PhaseDraw.Segment}; {@code [edge][0]} the first
         * end, {@code [edge][1]} the second), null for a BULK end and for a passive connection the boundary rule has closed.
         * Decided once per pass, from the pass's start flow ({@link #initialPoint}) and its seeds, like
         * {@link #junctionDonorFirst}: which phases are pinned at their capacity and which one carries the rest is a kink of
         * the stream's composition in the flow, so freezing it keeps each Newton residual smooth; the pass loop re-decides it
         * from the converged flow ({@link #segmentsMoved}).
         */
        final PhaseDraw.Segment[][] segments;
        /** Per connection end ({@code [edge][end]}), the pressure drop of the velocity cap of the direction leaving that end
         * ({@link #capDrop}), and the end transport it was computed on: one entry per end, kept while the decode cache keeps
         * that end's transport, and dropped with it ({@link #releaseDecodeCache}). */
        final Transport[][] capSources;final double[][] capPressureDrops;
        final FluidThermodynamics.Prepared[] cachedPrepared;
        /**
         * Whether a device in this island prescribes its own mass flow this pass. A saturated
         * filter inlet and a pump holding a target volume flow are two equations for the same edge
         * once continuity has related them, so the filter capacity is not imposed as a law while
         * one is active; those islands meet the capacity by step refusal instead, exactly as every
         * island did before the law existed. A pump on its head limit prescribes a pressure rather
         * than a flow and leaves the edge free, so it does not count.
         */
        final boolean prescribedFlow;
        /** Whether this island carries the three aggregate solid moment unknowns on every node;
         * the whole-island predicate {@link PhaseLayout} is built with, evaluated once. */
        final boolean solidSupport;
        final boolean[] amountVariables,solidVariables;
        final double[] differenceFloors;
        /** Each amount column's seed difference scale ({@link PhaseLayout#seedDifferenceScale}), -1 for every other column:
         * what {@link #differenceScale} differences a trace below {@link PhaseLayout#TRACE_DIFFERENCE_GUARD} against. */
        final double[] seedDifferenceScales;
        /** Below this value an amount unknown's nonnegativity is not a live constraint on the step
         * length: a solid moment at or below it is dust (see {@link #SOLID_CLAMP_FRACTION}), and a
         * fluid amount at or below it is a trace that {@link #project} keeps positive instead (see
         * {@link #TRACE_CLAMP_FRACTION}). */
        final double[] clampFloors;
        /** Edge indices incident to each node, ascending - which is the order the whole-island
         * residual accumulates that node's targets, energy and junction inflows in, so a per-node
         * re-assembly reproduces every accumulator bit for bit. */
        final int[][] nodeEdges;
        /** The pressure a junction no open connection reaches keeps, or 0 for every other node.
         * Depends on the pass's active set, so it is built with the equations and not per
         * evaluation; see {@link PhaseLayout#junctionRows}. */
        final double[] retainedPressures;
        /**
         * The pressure each edge's pressure-dimensioned rows are stated in, so that the single
         * Newton tolerance means the same <em>relative</em> closure on every island.
         *
         * <p>Those rows - the hydraulic balance, a pump's head limit, a valve's regulated inlet
         * pressure and an open valve's zero head - used to divide by a literal 1e5 Pa. That is a
         * fixed unit, not a scale: at a tolerance of 1e-9 it demands 1e-4 Pa of closure whatever
         * the island runs at, which is a relative 1e-9 at atmospheric pressure and a relative
         * 2.5e-10 at 400 kPa. The second is below what the endpoint pressures themselves can be
         * resolved to once they are reconstructed and re-encoded through the logarithmic pressure
         * unknown, so a driven line settles a fraction of a milli-pascal apart and the island
         * stalls there forever - and the more resistive the edge, the larger the imbalance its own
         * row shows, which is why an inline filter (clean resistance ~150x a plain pipe's) is what
         * this stops first.
         *
         * <p>The scale is {@code max(1e5, max(P_first, P_second))} from the endpoint states of
         * <em>this pass</em>, so:
         *
         * <ul>
         * <li>it is constant through the whole Newton solve, which keeps it out of the Jacobian
         *     entirely: no derivative of the scale, no new sparsity, and the block sweep and the
         *     coloured whole-island sweep still evaluate the identical row;</li>
         * <li>the 1e5 Pa floor leaves every island at or below atmospheric pressure bit-identical
         *     to what it was, so only islands that actually run above 1 bar move at all;</li>
         * <li>it is per edge rather than per island, because an island may span a 4 bar header and
         *     an atmospheric vent and only the rows that sit at high pressure should be loosened. A
         *     single island-wide maximum would relax an atmospheric branch by the header's factor
         *     for no reason.</li>
         * </ul>
         *
         * <p>Taking the live iterate's pressures instead was rejected: the scale would then be a
         * function of the unknowns, so its derivative would enter every one of those rows, the
         * {@code max} would put a kink in them, and the rows would stop being a fixed positive
         * multiple of the physical equation - all of it to track a pressure that a converging
         * Newton is already driving to the pass's own endpoint pressures.
         */
        final double[] pressureScales;
        /** This pass's starting point, built once: {@link #junctionDonorFirst} is read off it. */
        final double[] initialPoint;
        /**
         * Which end of each connection donates into a zero-holdup junction for the whole of this
         * pass, taken from the point the pass starts at rather than from the live iterate.
         *
         * <p>A junction's mass fractions and its specific enthalpy are the mixture of what arrives,
         * a ratio that does not shrink with the flows that form it. Deciding the donor inside the
         * residual therefore put a finite jump in those rows at exactly zero flow, and the
         * one-sided differences the Jacobian is built from only ever sample the positive side of
         * it: on a settled island the Newton direction points the other way, so the derivative the
         * step was computed from is not the derivative along the step. Measured on the valve block
         * line at 400 kPa, the linear model predicted a residual of 8e-25 and the full step
         * produced 1.7e-3, twenty orders apart, with every one of the twenty-four backtracks
         * landing on the same plateau because the jump is a step and not a slope. That is the
         * whole of why a tank behind a filter or a valve held while the same tank on plain pipes
         * runs indefinitely: a plain pipe carries its donor's properties only inside terms already
         * multiplied by the flow, so its own upwind switch is continuous and its jump is zero.
         *
         * <p>Freezing it makes one pass's residual smooth in its own unknowns, which is the rule
         * every other switch in this solver already follows - device modes, phase regimes, trace
         * support and boundary closure are all decided once per pass and revised by the outer
         * loop. The outer loop does the same here: a converged point whose flow disagrees with the
         * donor it was solved under is one more active-set change, and the donor signs join the
         * cycle key so the pass sequence stays finite. Only the junction mixture reads them; the
         * reservoir targets, the net-flow balance and every edge row keep the live upwind, so an
         * accepted point is upwinded exactly as {@link ConservativeTransport} reconstructs it.
         */
        final boolean[] junctionDonorFirst;
        /**
         * The density each connection's static head {@code rho*g*dz} is stated with for the whole
         * of this pass, read off the point the pass starts at rather than from the live iterate.
         *
         * <p>It is {@link #junctionDonorFirst}'s twin on the hydraulic row, and the reason is the
         * same. {@code edgeRows} used to take the head's density from the upwind end of the
         * <em>current</em> flow, so a connection whose two ends hold fluids of different density
         * had a finite jump of {@code (rho_first - rho_second)*g*dz} in its own row at exactly
         * zero flow - and the head does not shrink with the flow, so that jump is degree zero,
         * exactly like a zero-holdup junction's mixture. The one-sided differences the Jacobian is
         * built from sample only the branch the iterate is already on, so the line search is
         * handed a direction whose derivative does not hold across the switch, and a settled line
         * lands on a plateau it cannot leave. Every fixture in the tree used to be flat, where
         * {@code dz} is zero and the term vanished; measured on a water generator four blocks
         * below a nitrogen-charged tank at 400 kPa, the row jumped by 2.754e-2 - the predicted
         * {@code (996.31-715.46)*9.80665*4/400000} to every digit - the linear model predicted
         * 1.08e-19, the full step produced 2.76e-2, and all twenty-four backtracks landed on the
         * same 2.716e-2 plateau. See documentation/ELEVATED_LINE_PROBE.md.
         *
         * <p>Freezing it is also the physically honest reading. A connection owns no holdup, so
         * the fluid whose weight the head is has to be inferred, and what a vertical pipe from a
         * water generator into a gas space actually contains is the water that was pushed into it
         * - not whichever end momentarily reads as upstream of a 1e-8 kg/s flow. Stating it once
         * per pass makes the column a property of the connection for that pass, the row a smooth
         * function of the pass's own unknowns, and the rest point {@code P_b = P_a - rho*g*dz}
         * an actual root instead of a value the residual steps over.
         *
         * <p>Which column it is, is stated below from the pass's own seed pressures and elevations
         * - never from the start point's flow sign, which at a settled line is a numerical zero and
         * means nothing, and never from a converged flow either. It is deliberately <em>not</em>
         * revised by the outer active-set loop, which is the one place it differs from the junction
         * donor, and the difference is measured rather than argued: the two columns of a line near
         * its own hydrostatic balance each put the flow on the other one - the heavier column
         * leaves the tank marginally over-filled, so the flow is negative, and the lighter one then
         * leaves it marginally under-filled, so it is positive - so a rule that turned the head
         * over on a converged disagreement would alternate forever, and if the column joined the
         * cycle key it would alternate for exactly two passes and then throw. The pass keeps the
         * column it solved under; the flow that disagrees with it is small by construction, and the
         * connection's own boundary closure or the next step's seeds take it from there. The
         * friction term keeps the live donor: {@code pressureDrop} is odd in the flow and zero at
         * zero, so its upwind switch is continuous and carries no jump to freeze.
         *
         * <p>It is also deliberately not part of {@link WorkspaceKey}, unlike the junction donors,
         * and that too was measured rather than argued. Adding it - as a real boolean per connected
         * elevation and a constant for every flat one, so that a flat island's workspace identity
         * could not move - kept the pass sequence honest in principle but cost the vertical line
         * standing at its own hydrostatic balance its whole run: from forty intervals settled at
         * 360918.3068 Pa to a hold on {@code Newton iteration limit at residual 2.202e-7}, because
         * the extra key component churns the workspace cache and the modified Newton loses the
         * preconditioner it was reusing. It belongs where {@link #pressureScales} belongs: a
         * quantity each pass reads off its own seeds, revised only by the seeds moving.
         *
         * <p>What is frozen is which end's fluid stands in the connection (0 the first end's stream, 1 the second's), never
         * its density (t27's A377, taken by t28): the row reads that end's density on its own trial state, the state its loss
         * reads. A riser at rest then balances its own fluid's weight at the state solved for, and a dead-end loop of risers
         * carries nothing but the column's discretisation (one end's density per riser: a gas's density changes by
         * {@code g rho / P}, about 1e-4 per metre at 480 kPa). Frozen at the seeds, the columns of random 498's dead-end
         * pocket (seeded at 118 to 124 kPa, converged at 397 kPa) did not cancel round its loops: its first rate seed carried
         * 1.8e-5 kg/s round them, 2.4e-7 kg/s on the trial's densities. The end is fixed for the pass, so the row stays smooth
         * in its unknowns: no jump between the two columns at zero flow.
         */
        final int[] columnEnds;
        /** Residual scratch. Every one of these is written before it is read within a single
         * evaluation and never escapes it, and the evaluations of one solve are sequential on the
         * worker holding the latch, so they are filled again rather than allocated again. The
         * returned residual is not among them: {@link SparseNewton} holds the current and the
         * candidate residual at the same time. */
        final double[][] targets,incoming;
        final double[] energy,incomingMass,incomingEnergy,netMass,fractions;
        /** The weight of the mixing and enthalpy rows the last {@link #junctionInflow} call set: they are stated in
         * amount form over the junction's own mass; see there. */
        double junctionWeight=1;
        Equations(PassiveNetwork graph,double dt,List<FlowControl.Mode> modes,int[] caps,boolean[] boundaryClosed,List<FluidThermodynamics.State> seeds,boolean[][] reachable,
                  PhaseSupport[][] supports,List<PassiveNetwork.Pipe.Identity> pipeIdentities) {
            this.modes=List.copyOf(modes);capDirections=caps.clone();this.pipeIdentities=pipeIdentities;
            prescribedFlow=this.modes.contains(FlowControl.Mode.PUMP_TARGET);
            this.seeds=List.copyOf(seeds);this.supports=supports;
            this.boundaryClosed=boundaryClosed.clone();
            this.graph=graph;this.dt=dt;solidSupport=hasSolids(graph);int count=graph.reservoirs().size();layout=new PhaseLayout[count];offsets=new int[count];oldAmounts=new double[count][];
            capSources=new Transport[graph.pipes().size()][2];capPressureDrops=new double[graph.pipes().size()][2];
            cachedVariables=new double[count][];cachedStates=new FluidThermodynamics.State[count];cachedTransport=new Transport[count];
            cachedPrepared=new FluidThermodynamics.Prepared[count];
            drawsPhase=new boolean[count];
            for(int edge=0;edge<graph.pipes().size();edge++)for(int end=0;end<2;end++) {
                var pipe=graph.pipes().get(edge);
                if(this.boundaryClosed[edge]&&pipe.control() instanceof FlowControl.Passive)continue;
                if((end==0?pipe.firstPort():pipe.secondPort())!=PassiveNetwork.PhasePort.BULK)drawsPhase[end==0?pipe.first():pipe.second()]=true;
            }
            capacities=capacities(graph,dt);
            seedHeld=new boolean[count][];startPhases=new Transport[count][];
            for(int i=0;i<count;i++)if(drawsPhase[i]){seedHeld[i]=PhaseDraw.held(this.seeds.get(i));startPhases[i]=transport(i,graph.reservoirs().get(i).state(),null).phases;}
            int cursor=0;dry=new boolean[count];bagStart=new double[count];
            for(int i=0;i<count;i++) {
                var node=graph.reservoirs().get(i);
                dry[i]=dry(node,this.seeds.get(i));
                // The bag volume the step starts from, whose wall work each vessel's energy row books (decision D54).
                if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR)bagStart[i]=node.empty()?0:PhaseLayout.bagVolume(node.state(),node.inventory().volume());
            }
            for(int i=0;i<count;i++) {
                var state=graph.reservoirs().get(i).state();offsets[i]=cursor;oldAmounts[i]=graph.reservoirs().get(i).inventory().moles();
                if(!graph.reservoirs().get(i).fixed()&&!dry[i]) {
                    layout[i]=new PhaseLayout(model,seeds.get(i),Arrays.copyOf(reachable[i],model.hydrocarbon.componentCount()),oldAmounts[i],supports[i],solidSupport);cursor+=layout[i].size();
                    if(SolverDiagnostics.ENABLED) {
                        int omitted=layout[i].singlePhaseComponentCount();
                        if(omitted>0){SolverDiagnostics.traceOmittedUnknowns.add(omitted);SolverDiagnostics.truncatedNodePasses.increment();}
                    }
                }
            }
            edgeOffset=cursor;cursor+=graph.pipes().size();controlOffsets=new int[graph.pipes().size()];Arrays.fill(controlOffsets,-1);
            for(int i=0;i<controlOffsets.length;i++)if(!(graph.pipes().get(i).control() instanceof FlowControl.Passive))controlOffsets[i]=cursor++;
            filterOffsets=new int[graph.pipes().size()];Arrays.fill(filterOffsets,-1);for(int i=0;i<filterOffsets.length;i++)if(graph.pipes().get(i).filter()!=null)filterOffsets[i]=cursor++;
            size=cursor;
            amountVariables=new boolean[size];solidVariables=new boolean[size];
            differenceFloors=new double[size];clampFloors=new double[size];Arrays.fill(differenceFloors,1);
            seedDifferenceScales=new double[size];Arrays.fill(seedDifferenceScales,-1);
            for(int node=0;node<count;node++)if(layout[node]!=null)for(int local=0;local<layout[node].size();local++) {
                int column=offsets[node]+local;
                amountVariables[column]=layout[node].totalAmountVariable(local);differenceFloors[column]=layout[node].differenceScale(local,0);
                if(amountVariables[column])seedDifferenceScales[column]=layout[node].seedDifferenceScale(local);
                solidVariables[column]=layout[node].solidVariable(local);
                if(solidVariables[column])clampFloors[column]=SOLID_CLAMP_FRACTION*differenceFloors[column];
                else if(amountVariables[column])clampFloors[column]=TRACE_CLAMP_FRACTION*differenceFloors[column];
            }
            int components=oldAmounts[0].length;
            targets=new double[count][components];incoming=new double[count][components];fractions=new double[components];
            solidTargets=new double[count][3];solidIncoming=new double[count][3];
            energy=new double[count];incomingMass=new double[count];incomingEnergy=new double[count];netMass=new double[count];
            for(int node=0;node<count;node++)if(layout[node]!=null)cachedVariables[node]=new double[layout[node].size()];
            int[] degree=new int[count];
            for(var pipe:graph.pipes()){degree[pipe.first()]++;degree[pipe.second()]++;}
            nodeEdges=new int[count][];for(int node=0;node<count;node++)nodeEdges[node]=new int[degree[node]];
            Arrays.fill(degree,0);
            for(int edge=0;edge<graph.pipes().size();edge++) {
                var pipe=graph.pipes().get(edge);
                nodeEdges[pipe.first()][degree[pipe.first()]++]=edge;nodeEdges[pipe.second()][degree[pipe.second()]++]=edge;
            }
            pressureScales=new double[graph.pipes().size()];
            for(int edge=0;edge<pressureScales.length;edge++) {
                var pipe=graph.pipes().get(edge);
                pressureScales[edge]=Math.max(1e5,Math.max(seeds.get(pipe.first()).pressure(),seeds.get(pipe.second()).pressure()));
            }
            // A junction no open connection reaches keeps the pressure it had (PhaseLayout.junctionRows). The same holds for a
            // sealed pocket of junctions (hold F8; see pockets): its members are joined by open connections, but every
            // connection on its boundary is closed, so its members' mass rows sum to zero identically - one of them says
            // nothing the others do not - and its pressure level has no row. The pocket's first member keeps its pressure
            // in place of its mass row; the others keep theirs. A pocket of one junction is the isolated junction, as before.
            retainedPressures=new double[count];
            int[] pocket=pockets(graph,this.modes,this.boundaryClosed);boolean[] unsealed=new boolean[count],retained=new boolean[count];
            for(int edge=0;edge<graph.pipes().size();edge++) {
                var pipe=graph.pipes().get(edge);if(closedEdge(this.modes.get(edge),this.boundaryClosed[edge]))continue;
                int a=pipe.first(),b=pipe.second();
                if(pocket[a]>=0&&pocket[b]!=pocket[a])unsealed[pocket[a]]=true;
                if(pocket[b]>=0&&pocket[a]!=pocket[b])unsealed[pocket[b]]=true;
            }
            for(int node=0;node<count;node++) {
                if(layout[node]==null||!graph.reservoirs().get(node).junction()||pocket[node]<0||unsealed[pocket[node]]||retained[pocket[node]])continue;
                retained[pocket[node]]=true;retainedPressures[node]=graph.reservoirs().get(node).state().pressure();
            }
            initialPoint=buildInitial();
            junctionDonorFirst=new boolean[graph.pipes().size()];
            for(int edge=0;edge<junctionDonorFirst.length;edge++) {
                var pipe=graph.pipes().get(edge);
                // A connection with no junction at either end donates into no mixture, so it has no
                // frozen donor to state and none to key a workspace on. Leaving it constant is what
                // keeps an island without a single zero-holdup node bit for bit what it was.
                junctionDonorFirst[edge]=!graph.reservoirs().get(pipe.first()).junction()&&!graph.reservoirs().get(pipe.second()).junction()
                        ||initialPoint[edgeOffset+edge]>=0;
            }
            columnEnds=new int[graph.pipes().size()];
            for(int edge=0;edge<columnEnds.length;edge++) {
                var pipe=graph.pipes().get(edge);
                var start=seeds.get(pipe.first());var end=seeds.get(pipe.second());
                // Each end's column is what its own port draws first (decision D11; the bulk when it holds no phase).
                double first=endDensity(start,pipe.firstPort()),second=endDensity(end,pipe.secondPort());
                // Decisions D54 and D59 (5): an end that may not fill the connection (a dry end, a void, a generator's far
                // end, a blocked way) has nothing to stand in it; it stands in what the other end sends ({@link #donates}).
                int only=-1;
                if(!donates(graph,pipe,pipe.first())){first=second;only=1;}
                else if(!donates(graph,pipe,pipe.second())){second=first;only=0;}
                double dz=graph.reservoirs().get(pipe.second()).elevation()-graph.reservoirs().get(pipe.first()).elevation();
                // The same pressure difference the row drives on, actuator included: a pump's own
                // head is the larger part of what pushes its connection, so a column decided
                // without it is decided on the suction/discharge gap alone and always reads the
                // discharge as the donor.
                double actuator=controlOffsets[edge]<0?0:initialPoint[controlOffsets[edge]]*1e5;
                double difference=endPressure(graph.reservoirs().get(pipe.first()),pipe.firstPort(),start)-endPressure(graph.reservoirs().get(pipe.second()),pipe.secondPort(),end)
                        +(pipe.control() instanceof FlowControl.Mover?actuator:-actuator);
                // The column that is its own donor. A column states which end fills the connection,
                // so a column is admissible exactly when the driving pressure it produces points
                // away from the end it was read off: the first end's when that pressure is forward,
                // the second end's when it is backward. Where both columns agree on the direction
                // only one of them is admissible and there is nothing to decide - a line pouring
                // downhill stands in what feeds it from above, and a line the pressures cannot lift
                // stands in what would come back down it.
                //
                // Taking the start point's upwind end instead reads a numerical zero as a
                // direction, and both readings of that zero are wrong somewhere. A water generator
                // four blocks under a nitrogen tank at the same pressure sits at a flow of exactly
                // zero; read as the water column it asks for 39 kPa the generator has not got, so
                // the pass is sent looking for a reverse flow the whole way across zero and stalls
                // on the friction kink there, where the slope changes by three orders between the
                // two fluids. Read the other way, a liquid line resting on its own two-metre column
                // whose ends differ only by water's compressibility takes the lighter end's
                // density, and the 0.18 Pa that leaves drives a permanent 8.3e-7 kg/s through a
                // graph that has to be at rest.
                //
                // Two cases are left, and they are opposites rather than one band.
                //
                // Neither column admissible - the driving pressure is below the heavier head and
                // above the lighter one - is where a settled line actually lives, and it is the
                // same fact that forbids revising the head on a converged flow, since there each
                // column puts the flow on the other one. The column to state is then the one
                // nearest its own rest point, which makes rest a fixed point of the rule as well as
                // of the equations: a line standing at {@code P_a - P_b = rho*g*dz} has no driving
                // pressure at all under {@code rho} and the whole difference between the heads
                // under the other one, so it keeps the column it is balanced under.
                //
                // Both columns admissible is genuine bistability - the connection can stand full of
                // either fluid and be at rest in both - and there the start point's flow sign is
                // the right evidence and the only evidence, because which fluid is in the pipe is
                // exactly the history the model does not otherwise carry. Deciding it by rest point
                // instead stops a filling line at the lighter column's balance: the falling
                // four-block line at 400 kPa settled at 429851 Pa against the 439082 Pa its own
                // water column demands, because the tank's own mixture balances 9 kPa earlier and
                // the line met that point first.
                //
                // On an island whose devices all sit at one y both tests read the same pressure
                // difference and the column they pick multiplies a {@code dz} of zero, so a flat
                // island is bitwise what it was whichever end is named.
                //
                // The rule is {@link #columnFromFirst}, which the start estimate states too.
                columnEnds[edge]=only>=0?only:columnFromFirst(first,second,difference,dz,initialPoint[edgeOffset+edge]>=0)?0:1;
            }
            segments=startInsideCaps();
            startCaps();
            anchorJunctions();
        }
        /**
         * The segments of this pass, deciding them from its start flows, and those start flows moved inside the velocity
         * cap of the priority mixture they land in (decision D11). The start point starts every connection inside its own
         * velocity cap ({@link #initialMassFlow}), but that cap is the one of the stream a port draws at a vanishing flow
         * (the water of a wet tank's bottom port); a start flow beyond the first phase's capacity lands in a mixture whose
         * cap ({@link #capLimit}) can be far lower (a volume flow of mostly gas), and a pass started there has to walk its
         * Newton the whole way down: measured on a 5 s step of a 2 MPa tank with 2 % water, 137.7 kg/s of start flow
         * against a mixture cap of about 8.7 kg/s, the Newton stalled and the phase correction cycled. Only a connection
         * drawing through a phase port moves (a sign never changes, so the frozen junction donors and head columns
         * read off the start point are unchanged); the segments are then decided again from the moved flows, since a
         * flow moved below the pinned total lands in a segment with fewer pinned phases, whose cap is at least as high. An
         * end drawing the phase the start point read keeps its flow (the start point already starts inside that cap, to
         * the bit); one whose seed no longer holds that phase (a phase correction) moves inside the cap of the one it
         * draws instead.
         */
        private PhaseDraw.Segment[][] startInsideCaps() {
            int edges=graph.pipes().size();
            var decided=decideSegments(Arrays.copyOfRange(initialPoint,edgeOffset,edgeOffset+edges));
            for(int round=0;round<3;round++) {
                boolean moved=false;
                for(int edge=0;edge<edges;edge++) {
                    double q=initialPoint[edgeOffset+edge];int end=q>=0?0:1;var segment=decided[edge][end];
                    if(segment==null)continue;
                    var pipe=graph.pipes().get(edge);int node=end==0?pipe.first():pipe.second();
                    double limit=capLimit(segment,edge,end,transportAt(node,seeds.get(node)));
                    if(Math.abs(q)>limit){initialPoint[edgeOffset+edge]=Math.copySign(limit,q);moved=true;}
                }
                if(!moved)break;
                decided=decideSegments(Arrays.copyOfRange(initialPoint,edgeOffset,edgeOffset+edges));
            }
            return decided;
        }
        /** {@link #capacities} of {@code graph} (its states are the step's start states). */
        private double[][] capacities(PassiveNetwork graph,double dt) {
            int nodes=graph.reservoirs().size();int[] ends=new int[nodes];
            for(var pipe:graph.pipes())for(int end=0;end<2;end++) {
                if((end==0?pipe.firstPort():pipe.secondPort())==PassiveNetwork.PhasePort.BULK||!boundaryAllowed(graph,pipe,end==0?1:-1))continue;
                ends[end==0?pipe.first():pipe.second()]++;
            }
            double[][] result=new double[nodes][];
            for(int node=0;node<nodes;node++) {
                if(ends[node]==0)continue;
                var start=graph.reservoirs().get(node).state();result[node]=new double[3];
                for(int phase=0;phase<3;phase++)result[node][phase]=rateOnly?Double.POSITIVE_INFINITY
                        :FluidThermodynamics.holdsPhase(start,phase)?model.phaseMass(start,phase)/(ends[node]*dt):0;
            }
            return result;
        }
        /** The segment of every open phase-port end at the connection flows {@code flows} and this pass's seeds; each end's
         * priority order is read off the node's state on the accepted state the step starts from, like its capacities. */
        private PhaseDraw.Segment[][] decideSegments(double[] flows) {
            var result=new PhaseDraw.Segment[graph.pipes().size()][2];
            for(int edge=0;edge<result.length;edge++)for(int end=0;end<2;end++) {
                var pipe=graph.pipes().get(edge);var port=end==0?pipe.firstPort():pipe.secondPort();int node=end==0?pipe.first():pipe.second();
                if(port==PassiveNetwork.PhasePort.BULK||boundaryClosed[edge]&&pipe.control() instanceof FlowControl.Passive)continue;
                var order=PhaseDraw.order(model,graph.reservoirs().get(node).state(),port);
                // An end no connection of which may carry outflow from its vessel (into a generator, blocked) draws nothing
                // over the step; its capacities are none, and the first phase its seed holds names its stream.
                var capacity=capacities[node]==null?new double[3]:capacities[node];
                result[edge][end]=PhaseDraw.Segment.decide(order,capacity,seedHeld[node],end==0?flows[edge]:-flows[edge]);
            }
            return result;
        }
        /**
         * The seeds of a pass whose Newton failed, with every vessel whose phase-port segments together draw a phase's whole
         * start content (each end that may draw from it pins the phase, at its equal share of the capacity: {@code sum c dt
         * = m_start}) seeded without that content: its seed's amounts less the phase's start amounts, flashed at the seed's
         * temperature and pressure; null when no seed changes its phase regime that way. A phase drawn to zero over the step
         * usually ends it on its own phase boundary: a gas no liquid dissolves (nitrogen over free water) cannot exist once
         * it has all left, and a pass that keeps its unknowns has no root, while the phase correction of the failed iterate
         * (which still holds some of it) finds nothing to change - a step refused at every size, since halving the step
         * doubles the capacity and the pinned draw is still the whole phase. A starting point and a phase regime only, like
         * the phase correction: the accumulation rows start from the vessel's inventory, and a converged pass that turns out
         * to hold the phase after all is corrected back by the converged-point phase correction.
         */
        List<FluidThermodynamics.State> drainedSeeds(Runnable checkpoint) {
            List<FluidThermodynamics.State> result=null;
            for(int node=0;node<layout.length;node++) {
                if(layout[node]==null||!drawsPhase[node]||capacities[node]==null)continue;
                var seed=seeds.get(node);var start=graph.reservoirs().get(node).state();
                double[] drawn=new double[3];
                for(int edge=0;edge<segments.length;edge++)for(int end=0;end<2;end++) {
                    var segment=segments[edge][end];var pipe=graph.pipes().get(edge);
                    if(segment==null||(end==0?pipe.first():pipe.second())!=node)continue;
                    for(int phase:segment.pinned())drawn[phase]+=segment.capacity()[phase]*dt;
                }
                double[] amounts=PhaseLayout.totalAmounts(seed);boolean any=false;
                for(int phase=0;phase<3;phase++) {
                    if(!seedHeld[node][phase]||!FluidThermodynamics.holdsPhase(start,phase)||drawn[phase]<model.phaseMass(start,phase)*(1-1e-9))continue;
                    var n=FluidThermodynamics.phaseMoles(start,phase);
                    for(int c=0;c<amounts.length;c++)amounts[c]=Math.max(0,amounts[c]-n[c]);
                    any=true;
                }
                if(!any||!(PhaseLayout.sum(amounts)>0))continue;
                FluidThermodynamics.State drained;
                try{drained=model.flashTP(seed.temperature(),seed.pressure(),amounts,checkpoint).withSolidState(seed.solids(),seed.solidMoments());}
                catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold outside){continue;}
                if(phaseCode(drained)==phaseCode(seed))continue;
                if(result==null)result=new ArrayList<>(seeds);
                result.set(node,drained);
            }
            return result;
        }
        /**
         * Decision D54: {@code emptying} with every vessel the failed iterate {@code x} drains to nothing added (what it holds
         * there is below 1e-6 of its stock, and the iterate's connections take at least half its stock out over the step),
         * or null when there is none. A vessel a scheduled transfer names is left to its transfer.
         */
        boolean[] runsDry(double[] x,boolean[] emptying) {
            boolean[] result=null;
            for(int node=0;node<layout.length;node++) {
                var reservoir=graph.reservoirs().get(node);
                if(layout[node]==null||reservoir.kind()!=PassiveNetwork.NodeKind.RESERVOIR||dry[node])continue;
                final int at=node;if(graph.scheduledTransfers().stream().anyMatch(t->t.node()==at))continue;
                double held=0,left=0;for(double n:oldAmounts[node])held+=n;
                if(!(held>0))continue;
                for(int local=0;local<layout[node].size();local++)if(layout[node].totalAmountVariable(local)&&!layout[node].solidVariable(local))left+=x[offsets[node]+local];
                left*=layout[node].amountScale();
                if(!(left<1e-6*held))continue;
                double out=0;for(int edge:nodeEdges[node]){var pipe=graph.pipes().get(edge);double q=x[edgeOffset+edge];out+=pipe.first()==node?q:-q;}
                double stock=molarMass(oldAmounts[node])+reservoir.inventory().solids().massKg();
                if(!(out*dt>=.5*stock))continue;
                if(result==null)result=emptying==null?new boolean[layout.length]:emptying.clone();
                result[node]=true;
            }
            return result;
        }
        /** This pass's segments as codes for the cycle key ({@link PassKey}); empty for a graph without an open phase port. */
        byte[] segmentCodes() {
            int count=0;for(var ends:segments)for(var segment:ends)if(segment!=null)count++;
            byte[] codes=new byte[count];int at=0;
            for(var ends:segments)for(var segment:ends)if(segment!=null)codes[at++]=segment.code();
            return codes;
        }
        /** Whether the converged connection flows {@code flows} land an open phase-port end in another segment than the one
         * this pass froze (the pass loop then solves the pass again with the segments re-decided from them). */
        boolean segmentsMoved(double[] flows) {
            boolean any=false;for(boolean draws:drawsPhase)any|=draws;
            if(!any)return false;
            var decided=decideSegments(flows);
            for(int edge=0;edge<segments.length;edge++)for(int end=0;end<2;end++)
                if(segments[edge][end]!=null&&!segments[edge][end].samePhases(decided[edge][end]))return true;
            return false;
        }
        /**
         * What connection {@code edge} carries out of its end {@code end} for the connection flow {@code q}, on that end's
         * node transport {@code node}: the node's bulk at a BULK end (or a closed connection's), the one phase stream the
         * segment draws at the end state where it pins none, and otherwise the priority stream's mixture ({@link PhaseDraw}):
         * the pinned phases at their capacities, at the step's start state ({@link #startPhases}), and the marginal phase at
         * the end state with the rest of {@code |q|} (below the pinned total, the pinned phases in proportion to their
         * capacities). The mixture is a {@link Transport} of rates: its {@code mass} is the mass flow it describes and every
         * extensive field the matching rate, so every donor term the solver states per kilogram ({@code moving * moles /
         * mass}, the solid share, the specific enthalpy) reads it unchanged; its density is the no-slip mixture's (mass over
         * the sum of the phases' volume flows), its viscosity the phases' volume-flow average (the bulk's own rule), its
         * velocity limit the smallest of its phases' (the cap itself is {@link #capLimit}).
         */
        Transport drawn(int edge,int end,Transport node,double q) {
            var segment=segments[edge][end];
            if(segment==null||node.phases==null)return node;
            int marginal=segment.marginalAt(node.held());var pinned=segment.pinned();
            if(pinned.length==0)return marginal<0?node:node.phases[marginal];
            var pipe=graph.pipes().get(edge);var start=startPhases[end==0?pipe.first():pipe.second()];
            if(pinned.length==1&&marginal<0)return start[pinned[0]];
            double total=segment.pinnedTotal(),basis=marginal<0?total:Math.max(Math.abs(q),total);
            var rates=segment.rates(marginal,basis);
            double[] moles=new double[node.moles.length];double volume=0,viscous=0,heat=0,limit=Double.POSITIVE_INFINITY,solidMass=0,solidVolume=0,solidCapacity=0;
            for(int k=0;k<6;k++) {
                if(!(rates[k]>0)&&!(k<3&&k==marginal))continue;
                var stream=k<3?node.phases[k]:start[k-3];double rate=rates[k],perMass=rate/stream.mass;
                for(int c=0;c<moles.length;c++)moles[c]+=perMass*stream.moles[c];
                double flowVolume=perMass*stream.volume;volume+=flowVolume;viscous+=flowVolume*stream.viscosity;heat+=rate*stream.specificEnthalpy;
                limit=Math.min(limit,stream.velocityLimit);
                solidMass+=perMass*stream.solids.mass();solidVolume+=perMass*stream.solids.volume();solidCapacity+=perMass*stream.solids.heatCapacity();
            }
            return new Transport(moles,basis,volume,basis/volume,viscous/volume,heat/basis,limit,new SolidInventory.Moments(solidMass,solidVolume,solidCapacity),null);
        }
        /**
         * The velocity cap (kg/s) of the direction leaving end {@code end} of {@code edge}, on that end's node transport: the
         * drawn stream's {@code density * area * velocityLimit} at a BULK end and where one phase is drawn at the end state
         * (the form the cap always had, to the bit); for a priority mixture, a volume flow of {@code area * v}, {@code v} the
         * smallest velocity limit of its phases (decision D11's default): the pinned phases at their capacities and the
         * marginal one filling the rest, {@code C + rho_m (A v - sum c/rho)}, or, where the pinned phases alone would exceed
         * it (or no marginal phase is left), the pinned mixture scaled down to it, {@code C A v / sum c/rho} (continuous at
         * {@code A v = sum c/rho}).
         */
        double capLimit(int edge,int end,Transport node){return capLimit(segments[edge][end],edge,end,node);}
        /**
         * The pressure drop of {@code edge} (no inline filter) at the velocity cap of the direction leaving end {@code end},
         * on that end's node transport {@code node}: the loss of the drawn stream at {@link #capLimit}. Cached per end while
         * {@code node} is the same object: a coloured or block Jacobian perturbs a few nodes and the decode cache hands every
         * other end its unchanged immutable transport, so an end's cap is recomputed only when its own node moves.
         */
        private double capDrop(int edge,int end,Transport node) {
            if(capSources[edge][end]!=node) {
                double limit=capLimit(edge,end,node);var capTransport=drawn(edge,end,node,limit);
                capPressureDrops[edge][end]=graph.pipes().get(edge).pressureDrop(limit,capTransport.density,capTransport.viscosity);capSources[edge][end]=node;
            }
            return capPressureDrops[edge][end];
        }
        /** {@link #capLimit} under {@code segment} in place of this pass's own. */
        private double capLimit(PhaseDraw.Segment segment,int edge,int end,Transport node) {
            var pipe=graph.pipes().get(edge);
            if(segment==null||node.phases==null)return node.density*pipe.minimumArea()*node.velocityLimit;
            int marginal=segment.marginalAt(node.held());var pinned=segment.pinned();
            if(pinned.length==0){var stream=marginal<0?node:node.phases[marginal];return stream.density*pipe.minimumArea()*stream.velocityLimit;}
            var start=startPhases[end==0?pipe.first():pipe.second()];
            double velocity=marginal<0?Double.POSITIVE_INFINITY:node.phases[marginal].velocityLimit,total=segment.pinnedTotal(),pinnedVolume=0;
            for(int phase:pinned){velocity=Math.min(velocity,start[phase].velocityLimit);pinnedVolume+=segment.capacity()[phase]/start[phase].density;}
            double room=pipe.minimumArea()*velocity;
            return marginal>=0&&pinnedVolume<=room?total+node.phases[marginal].density*(room-pinnedVolume):total*(room/pinnedVolume);
        }
        /** The carrier viscosity of what {@code edge} draws out of end {@code end} (an inline filter's cake resistance scales
         * with it): the bulk's at a BULK end, the phase's own where one phase is drawn at the end state, and the phases'
         * volume-flow average for a priority mixture (the pinned ones' on the start state). */
        double drawnCarrierViscosity(int edge,int end,FluidThermodynamics.State state,Transport node,double q) {
            var segment=segments[edge][end];
            if(segment==null||node.phases==null)return carrierViscosity(state);
            int marginal=segment.marginalAt(node.held());var pinned=segment.pinned();
            if(pinned.length==0)return marginal<0?carrierViscosity(state):model.phaseCarrierViscosity(state,marginal,viscosities);
            var pipe=graph.pipes().get(edge);int n=end==0?pipe.first():pipe.second();var startState=graph.reservoirs().get(n).state();var start=startPhases[n];
            double total=segment.pinnedTotal();var rates=segment.rates(marginal,marginal<0?total:Math.max(Math.abs(q),total));double volume=0,value=0;
            for(int k=0;k<6;k++) {
                if(!(rates[k]>0))continue;
                var stream=k<3?node.phases[k]:start[k-3];double flowVolume=rates[k]/stream.density;volume+=flowVolume;
                value+=flowVolume*model.phaseCarrierViscosity(k<3?state:startState,k%3,viscosities);
            }
            return volume>0?value/volume:model.phaseCarrierViscosity(startState,pinned[0],viscosities);
        }
        /** The mass flow each phase supplies on every connection whose outflow end drew phases at the flows {@code flows} on
         * {@code states} (the converged candidate), as {@link PhaseDraw.Segment#rates}' six entries (end-state rates by phase
         * id, then start-state rates by phase id): what the reconstruction books and the transfer samples; the end state's
         * gas alone where a bulk end drew the gas of a donor holding solids but no liquid (owner decision D33, the stream
         * {@link #transport} gave it); null for an edge that drew the bulk or carried nothing, and null altogether when no
         * edge drew phases. */
        double[][] draws(double[] flows,List<FluidThermodynamics.State> states) {
            double[][] result=null;
            for(int edge=0;edge<flows.length;edge++) {
                double q=flows[edge];if(q==0)continue;
                int end=q>=0?0:1;var segment=segments[edge][end];
                var pipe=graph.pipes().get(edge);var donor=states.get(end==0?pipe.first():pipe.second());double[] rates;
                if(segment==null) {
                    if(!FluidThermodynamics.bulkDrawsGas(donor))continue;
                    rates=new double[6];rates[FluidThermodynamics.GAS]=Math.abs(q);
                } else {
                    int marginal=segment.marginalAt(PhaseDraw.held(donor));
                    if(marginal<0&&segment.pinned().length==0)continue;
                    rates=segment.rates(marginal,Math.abs(q));
                }
                if(result==null)result=new double[flows.length][];
                result[edge]=rates;
            }
            return result;
        }
        /** This node's transport rows at {@code state} with no prepared temperature bundle (for a point other than the
         * pass's decoded one: the reconstruction's, the error probe's). */
        Transport transportAt(int node,FluidThermodynamics.State state){return transport(node,state,null);}
        private void buildSparsity() {
            int count=layout.length;
            BitSet[] columns=new BitSet[size];for(int i=0;i<size;i++)columns[i]=new BitSet(size);
            for(int node=0;node<count;node++)for(int c=offsets[node];c<offsets[node]+nodeSize(node);c++)columns[c].set(offsets[node],offsets[node]+nodeSize(node));
            for(int edge=0;edge<graph.pipes().size();edge++) {
                var pipe=graph.pipes().get(edge);int er=edgeOffset+edge;
                for(int node:new int[]{pipe.first(),pipe.second()}) {
                    columns[er].set(offsets[node],offsets[node]+nodeSize(node));
                    for(int c=offsets[node];c<offsets[node]+nodeSize(node);c++) {
                        columns[c].set(er);
                        for(int receiver:new int[]{pipe.first(),pipe.second()})columns[c].set(offsets[receiver],offsets[receiver]+nodeSize(receiver));
                    }
                }
                columns[er].set(er);
                int filter=filterOffsets[edge];if(filter>=0){columns[filter].set(er);columns[filter].set(filter);columns[er].set(filter);for(int node:new int[]{pipe.first(),pipe.second()})for(int c=offsets[node];c<offsets[node]+nodeSize(node);c++)columns[c].set(filter);}
                int control=controlOffsets[edge];
                if(control>=0) {
                    columns[control].set(er);columns[control].set(control);columns[er].set(control);
                    for(int node:new int[]{pipe.first(),pipe.second()}) {
                        columns[control].set(offsets[node],offsets[node]+nodeSize(node));
                        for(int c=offsets[node];c<offsets[node]+nodeSize(node);c++)columns[c].set(control);
                    }
                }
            }
            // Exact structural zeros: phase allocation/T/P cannot change bulk component flow at
            // fixed total amounts and kg/s. Suppress cancellation-noise derivatives, not small physics.
            // Only for a BULK end: what a phase port carries is the phase split itself, which the
            // node's phase allocation, temperature and pressure move, so a node any end of which draws a
            // phase keeps every entry of its columns (plan 3.3 item 1).
            BitSet materialRows=new BitSet(size);
            for(int node=0;node<count;node++)if(layout[node]!=null) {
                int components=layout[node].componentBalanceCount();
                materialRows.set(offsets[node],offsets[node]+components-(graph.reservoirs().get(node).junction()?1:0));
                if(graph.reservoirs().get(node).junction())materialRows.set(offsets[node]+components+1);
            }
            for(int node=0;node<count;node++)if(layout[node]!=null&&!drawsPhase[node])for(int local=0;local<layout[node].size();local++)
                if(!layout[node].totalAmountVariable(local))columns[offsets[node]+local].andNot(materialRows);
            sparsity=new int[size][];for(int c=0;c<size;c++)sparsity[c]=columns[c].stream().toArray();
        }
        public int size(){return size;}
        /** {@link PhaseLayout#differenceStepScale} for a node's amount column (an amount below the trace guard at its value and
         * its seed is differenced relative to its own size; bitwise the old {@code max(floor, |value|)} otherwise), and
         * {@code max(floor, |value|)} for every other column. */
        public double differenceScale(int column,double value) {
            double seed=seedDifferenceScales[column];
            if(seed>=0)return PhaseLayout.traceDifferenceScale(Math.max(seed,Math.abs(value)));
            return Math.max(differenceFloors[column],Math.abs(value));
        }
        /**
         * Whether a chord step on the Jacobian built at {@code built} still closes at least half of every row's gap at
         * {@code now} ({@link #chordHolds}): the slopes a connection's row states are its loss slope in its flow and its
         * slope in each end's pressure unknown.
         *
         * <p>The loss slope (the turbulent walk on gas lines, B2 of the phase-ports batch, decision A371). In real life a
         * line's resistance follows its flow: on the turbulent branch it is proportional to the flow, on the laminar branch
         * constant. A Newton that walks a gas line from its start estimate (random seed 498: 2.0e-3 kg/s, Re 15 000) to a
         * root near rest (1.5e-5 kg/s, Re 109) halves its flow per fresh step; a chord step that keeps the resistance of a
         * flow twice as large closes only half of that line's gap, the residual halves, and the solver's contraction test
         * (refresh above 0.8) reads the halving as progress until the iteration limit. Both slopes are read on the same
         * donor state, the one at {@code now}. A cake's law is linear in the flow, and a closed connection's row is its flow.
         *
         * <p>The pressure slopes (t29, random seed 886). A row's slope in an end's pressure unknown, {@code ln(P / 1e5)}, is
         * that end's pressure: a node whose pressure has moved since the Jacobian was built has every row through it stated
         * with the old pressure. A filter's two junctions seeded 373 kPa apart (the generator's side and the void's) stood
         * near 143 kPa when the last Jacobian was built and near 222 kPa at the iterate, while the filter between them drops
         * 105 Pa at its cap: the chord steps stated its row's pressure columns at 1/1.55 of their own, overshot its gap by
         * 0.55 each, the residual changed sign and fell by 0.55 per step, read as progress, until the iteration limit at
         * 1.9e-3. The rows are unchanged, so is every root; what moves is which Jacobian a step uses.
         */
        @Override public boolean linearisationHolds(double[] built,double[] now) {
            if(built==null||built.length!=now.length)return true;
            decode(now);
            for(int edge=0;edge<graph.pipes().size();edge++) {
                var pipe=graph.pipes().get(edge);
                if(pipe.filter()!=null||boundaryClosed[edge])continue;
                double from=built[edgeOffset+edge],to=now[edgeOffset+edge];
                if(from==to)continue;
                var transport=drawn(edge,to>=0?0:1,cachedTransport[to>=0?pipe.first():pipe.second()],to);
                double then=pipe.loss(from,transport.density,transport.viscosity).massFlowDerivative();
                double slope=pipe.loss(to,transport.density,transport.viscosity).massFlowDerivative();
                if(!chordHolds(then,slope))return false;
            }
            for(int node=0;node<layout.length;node++)if(layout[node]!=null) {
                int at=offsets[node]+layout[node].pressureVariable();
                if(!chordHolds(1,DeterministicMath.exp(now[at]-built[at])))return false;
            }
            // Decision A423: a vessel whose wall changed branch since the build is on slopes the Jacobian does not state.
            if(builtWall!=null)for(int node=0;node<layout.length;node++)if(builtWall[node]!=null
                    &&!builtWall[node].equals(PhaseLayout.wall(cachedStates[node],graph.reservoirs().get(node).inventory().volume())))return false;
            return true;
        }
        /** Decision A423: the wall's branches of every vessel at {@code base}, read by the sweep's rows and the chord test. */
        @Override public void linearising(double[] base) {
            decode(base);var walls=new PhaseLayout.Wall[layout.length];
            for(int node=0;node<layout.length;node++)if(layout[node]!=null&&!graph.reservoirs().get(node).junction())
                walls[node]=PhaseLayout.wall(cachedStates[node],graph.reservoirs().get(node).inventory().volume());
            builtWall=sweepWall=walls;
        }
        @Override public void linearised(){sweepWall=null;}
        public double maximumStep(double[] variables,double[] direction) {
            double alpha=1;
            // Approach a nonnegative amount boundary directly instead of repeatedly halving a step
            // that misses it by roundoff. This bounds a trial step; it never clips accepted inventory.
            // Below clampFloors the unknown is not a boundary this step has to respect: a solid
            // moment 1e-30 to 1e-70 below its own scale would otherwise bound alpha at 1e-12 to
            // 1e-18, at which x+alpha*d is bitwise x for every unknown of order one and no
            // backtrack can ever produce a different residual.
            for(int c=0;c<edgeOffset;c++)if(amountVariables[c]&&variables[c]>clampFloors[c]&&direction[c]<0)alpha=Math.min(alpha,.99*variables[c]/-direction[c]);
            return alpha;
        }
        /**
         * The other half of the trace rule in {@link #maximumStep}: a fluid amount at or below its
         * floor does not bound the step, so a trial that would take it below {@value #TRACE_RETENTION}
         * of its value keeps that fraction instead, which is the share the {@code .99} rule would have
         * left it while scaling every other unknown by the same tiny factor. The rest of the step is
         * taken as the Newton direction says; see {@link #TRACE_CLAMP_FRACTION}. Solid moments keep
         * their own projection in {@link PhaseLayout#decode}.
         */
        @Override public void project(double[] variables,double[] candidate) {
            for(int c=0;c<edgeOffset;c++)
                if(amountVariables[c]&&!solidVariables[c]&&variables[c]<=clampFloors[c]&&candidate[c]<TRACE_RETENTION*variables[c])
                    candidate[c]=TRACE_RETENTION*variables[c];
            projectFlows(candidate);
        }
        /**
         * Decision A275: a line carries at most its cap, in a trial as in a converged point, as an amount is never negative.
         * A trial that puts a line on its law past the cap flow of the end it leaves (on the trial's own donor state) is
         * moved to that cap flow; a capped line's row states it already. Before the cap was a mode, the flow row's clamp
         * did this to every trial; without it the first steps of a pump starting on a junction at 1 atm drew kilograms of
         * nitrogen per second from a void branch whose cap is a fraction of that, and the Newton ended on a plateau at 4e-8
         * (InlineFilterLineHoldTest). A trial outside the property domain is left to the residual, which refuses it.
         */
        private void projectFlows(double[] candidate) {
            boolean any=false;for(int edge=0;edge<capDirections.length&&!any;edge++)any=capDirections[edge]==CAP_OPEN&&clamps(edge);
            if(!any)return;
            try {
                decode(candidate);
                for(int edge=0;edge<capDirections.length;edge++) {
                    if(capDirections[edge]!=CAP_OPEN||!clamps(edge))continue;
                    var pipe=graph.pipes().get(edge);double q=candidate[edgeOffset+edge];int end=q>=0?0:1;
                    var node=cachedTransport[end==0?pipe.first():pipe.second()];
                    double limit=pipe.filter()==null?capLimit(edge,end,node):filterCapFlow(edge,end,node,drawn(edge,end,node,q));
                    if(Math.abs(q)>limit)candidate[edgeOffset+edge]=Math.copySign(limit,q);
                }
            }catch(IllegalArgumentException outsideDomain){/* the residual refuses the trial */}
        }
        /** Solid unknowns this point leaves negative, i.e. where {@link PhaseLayout#decode}'s
         * nonnegativity projection would be active. Must be zero at every accepted point. */
        int negativeSolidUnknowns(double[] x) {
            int count=0;for(int c=0;c<edgeOffset;c++)if(solidVariables[c]&&x[c]<0)count++;
            return count;
        }
        int nodeSize(int node){return layout[node]==null?0:layout[node].size();}
        public int[][] columnRows(){if(sparsity==null)buildSparsity();return sparsity;}
        double[] initial(){return initialPoint.clone();}
        private double[] buildInitial() {
            double[] x=new double[size];for(int i=0;i<layout.length;i++)if(layout[i]!=null){var encoded=layout[i].encode(seeds.get(i));System.arraycopy(encoded,0,x,offsets[i],encoded.length);}
            int edges=graph.pipes().size();
            double[] q=new double[edges],head=new double[edges];boolean[] headSet=new boolean[edges];
            startPoint(graph,modes,boundaryClosed,pipeIdentities,seeds,q,head,headSet);
            for(int i=0;i<edges;i++) {
                x[edgeOffset+i]=q[i];
                if(headSet[i]&&controlOffsets[i]>=0)x[controlOffsets[i]]=head[i];
            }
            for(int i=0;i<filterOffsets.length;i++)if(filterOffsets[i]>=0)x[filterOffsets[i]]=graph.pipes().get(i).filter().loading();
            return x;
        }
        List<FluidThermodynamics.State> states(double[] x) {
            decode(x);
            var states=new ArrayList<FluidThermodynamics.State>(layout.length);
            for(var state:cachedStates)states.add(state);
            return states;
        }
        /** Drops the per-node decode cache; the next decode rebuilds every node exactly. */
        void releaseDecodeCache(){Arrays.fill(cachedStates,null);Arrays.fill(cachedTransport,null);Arrays.fill(cachedPrepared,null);
            for(var sources:capSources)Arrays.fill(sources,null);}
        /** Brings the per-node decode cache up to {@code x}; the residual reads the arrays directly. */
        private void decode(double[] x) {
            for(int i=0;i<layout.length;i++) {
                boolean same=cachedStates[i]!=null&&(layout[i]==null||Arrays.mismatch(x,offsets[i],offsets[i]+layout[i].size(),cachedVariables[i],0,cachedVariables[i].length)<0);
                if(same)continue;
                if(layout[i]!=null) {
                    double temperature=layout[i].temperature(x,offsets[i]);
                    if(cachedPrepared[i]==null||cachedPrepared[i].temperature()!=temperature)cachedPrepared[i]=model.prepare(temperature);
                }
                FluidThermodynamics.State state;
                // A trial a typed thermodynamic hold refused names the node it was refused at; see ThermoHold.
                try{state=layout[i]==null?graph.reservoirs().get(i).state():layout[i].decode(x,offsets[i],cachedPrepared[i]);}
                catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold hold){throw hold.at(graph.reservoirs().get(i).id());}
                var transport=transport(i,state,cachedPrepared[i]);
                cachedStates[i]=state;cachedTransport[i]=transport;
                if(layout[i]!=null)System.arraycopy(x,offsets[i],cachedVariables[i],0,layout[i].size());
            }
        }
        /**
         * What a connection carries out of one node, per kilogram and for its loss and cap: the conserved amounts and their
         * mass (the normaliser), the volume and density, the viscosity, the specific enthalpy, the velocity limit and the
         * solid moments. A node's cached entry is its BULK stream, the whole state exactly as the donor terms have always
         * read it ({@code mass} is {@code state.mass()}, so {@code moles/mass} is the double {@code moles/state.mass()} was),
         * and on a donor holding solids but no liquid its gas stream, which its bulk end draws (owner decision D33: solids
         * ride the liquid, {@link FluidThermodynamics#bulkDrawsGas}; {@link #draws} books it as that gas);
         * {@code phases} are its phase streams by phase id ({@link FluidThermodynamics#GAS}, {@link FluidThermodynamics#OIL},
         * {@link FluidThermodynamics#WATER}; null for a phase the state does not hold), built only for a node an end of which
         * is a phase port (plan 3.2; decision D11). What a port end draws of them is {@link Equations#drawn}.
         */
        private record Transport(double[] moles,double mass,double volume,double density,double viscosity,double specificEnthalpy,double velocityLimit,
                                 SolidInventory.Moments solids,Transport[] phases) {
            /** Which phases this node's state holds (a phase stream is built exactly for a phase it holds). */
            boolean[] held(){return new boolean[]{phases[0]!=null,phases[1]!=null,phases[2]!=null};}
        }
        /** This node's transport rows at {@code state}: the bulk stream in the arithmetic it always had (the gas stream where
         * a bulk end draws the gas, D33), plus its phase streams where an end of it is a phase port. The one builder for the
         * whole-island decode and the block Jacobian sweep. The gas stream's amounts per kilogram are the fluid's totals over
         * its mass, so the structural zeros of a BULK end's columns ({@link #buildSparsity}) hold for it as well. */
        private Transport transport(int node,FluidThermodynamics.State state,FluidThermodynamics.Prepared prepared) {
            var bulk=FluidThermodynamics.bulkDrawsGas(state)?phase(model.phaseStream(state,FluidThermodynamics.GAS,prepared,viscosities))
                    :new Transport(PhaseLayout.totalAmounts(state),state.mass(),state.volume(),state.mass()/state.volume(),viscosity(state,prepared),
                    state.enthalpy()/state.mass(),model.velocityLimit(state),state.solidMoments(),null);
            if(!drawsPhase[node])return bulk;
            var phases=new Transport[3];
            for(int phase=0;phase<3;phase++){var stream=model.phaseStream(state,phase,prepared,viscosities);if(stream!=null)phases[phase]=phase(stream);}
            return new Transport(bulk.moles,bulk.mass,bulk.volume,bulk.density,bulk.viscosity,bulk.specificEnthalpy,bulk.velocityLimit,bulk.solids,phases);
        }
        private static Transport phase(FluidThermodynamics.PhaseStream stream) {
            return new Transport(stream.molesView(),stream.mass(),stream.volume(),stream.mass()/stream.volume(),stream.viscosity(),
                    stream.specificEnthalpy(),stream.velocityLimit(),stream.solidMoments(),null);
        }
        public double[] residual(double[] x) {
            decode(x);double[] f=new double[size];
            assemble(x,cachedStates,cachedTransport,cachedPrepared,f);
            return f;
        }
        /**
         * One complete residual from already decoded node states. The three passes are the whole
         * island's version of the three assemblies a block Jacobian sweep performs for a single
         * perturbed column, and they are the only place each row's formula exists.
         */
        private void assemble(double[] x,FluidThermodynamics.State[] st,Transport[] tr,FluidThermodynamics.Prepared[] pr,double[] f) {
            for(int node=0;node<layout.length;node++)if(layout[node]!=null)nodeAccumulate(node,x,st,tr);
            for(int edge=0;edge<graph.pipes().size();edge++)edgeRows(edge,x,st,tr,f);
            for(int node=0;node<layout.length;node++)if(layout[node]!=null)nodeRows(node,x,st,pr,f);
        }
        /**
         * This node's accumulated backward-Euler targets: the inventory it started from, the
         * scheduled transfers that name it, and every incident edge in edge order - which is the
         * order the whole-island loop reached them in, so the sums are the same doubles. A node
         * without a layout owns nothing and is only ever a donor or receiver, so its accumulators
         * are never read.
         */
        private void nodeAccumulate(int node,double[] x,FluidThermodynamics.State[] st,Transport[] tr) {
            double[] target=targets[node],in=incoming[node];
            System.arraycopy(graph.reservoirs().get(node).inventory().solids().moments().values(),0,solidTargets[node],0,3);Arrays.fill(solidIncoming[node],0);
            System.arraycopy(oldAmounts[node],0,target,0,target.length);Arrays.fill(in,0);
            double stored=graph.reservoirs().get(node).inventory().internalEnergy(),mass=0,heat=0,net=0;
            double elevation=graph.reservoirs().get(node).elevation();
            for(var transfer:graph.scheduledTransfers()) {
                if(transfer.node()!=node)continue;
                var state=st[node];
                if(transfer instanceof ScheduledTransfer.Withdrawal withdrawal) {
                    // A module's withdrawal takes the vessel's bulk, its solids at their share of the mass (D23 (6)), on the
                    // state's own amounts: the node's transport is what a connection's bulk end draws (the gas, under D33).
                    double withdrawn=dt*withdrawal.massKgPerSecond();var n=PhaseLayout.totalAmounts(state);
                    for(int c=0;c<n.length;c++)target[c]-=withdrawn*n[c]/state.mass();
                    stored-=withdrawn*state.enthalpy()/state.mass();
                    var moments=state.solidMoments().values();for(int c=0;c<3;c++)solidTargets[node][c]-=withdrawn*moments[c]/state.mass();
                } else if(transfer instanceof ScheduledTransfer.Injection input) {
                    var rates=input.molesPerSecond();double massRate=0;
                    for(int c=0;c<rates.length;c++){target[c]+=dt*rates[c];massRate+=rates[c]*model.molecularWeight(c);}
                    massRate+=input.solidsPerSecond().massKg();var moments=input.solidsPerSecond().moments().values();for(int c=0;c<3;c++)solidTargets[node][c]+=dt*moments[c];
                    stored+=dt*(input.totalEnergyPerSecond()-massRate*GRAVITY*elevation);
                }
            }
            for(int edge:nodeEdges[node]) {
                var pipe=graph.pipes().get(edge);int a=pipe.first(),b=pipe.second();double flow=x[edgeOffset+edge];
                // The donor terms read the stream of the end the flow leaves: the donor's bulk at a BULK end (the same
                // doubles as its whole state), its drawn phase at a phase port.
                boolean first=node==a;int donor=flow>=0?a:b;var upstream=st[donor];var transport=drawn(edge,flow>=0?0:1,tr[donor],flow);
                boolean receiver=node==(flow>=0?b:a);
                double captureFraction=pipe.filter()==null?0:transport.solids.mass()/transport.mass;
                double deliveredFlow=Math.abs(flow)*(1-captureFraction);
                net+=receiver?deliveredFlow:-Math.abs(flow);
                var amounts=transport.moles;double moving=dt*flow;
                for(int c=0;c<amounts.length;c++){double moved=moving*amounts[c]/transport.mass;target[c]+=first?-moved:moved;}
                var moments=transport.solids.values();
                for(int c=0;c<3;c++)if(!receiver||pipe.filter()==null)solidTargets[node][c]+=(first?-1:1)*moving*moments[c]/transport.mass;
                double donorZ=graph.reservoirs().get(donor).elevation(),h=transport.specificEnthalpy;
                stored+=(first?-1:1)*moving*(h+GRAVITY*(donorZ-elevation));
                // What arrives at this node, on the live upwind: read only by a junction's rows, which mix it into the
                // junction's owned stock like a vessel's inflow.
                int mixed=donor;
                if(receiver) {
                    var source=st[mixed];var carried=transport;var sourceAmounts=carried.moles;
                    double sourceCapture=pipe.filter()==null?0:carried.solids.mass()/carried.mass;
                    double sourceZ=graph.reservoirs().get(mixed).elevation(),sourceEnthalpy=carried.specificEnthalpy;
                    mass+=Math.abs(flow)*(1-sourceCapture);
                    for(int c=0;c<sourceAmounts.length;c++)in[c]+=Math.abs(flow)*sourceAmounts[c]/carried.mass;
                    heat+=Math.abs(flow)*(sourceEnthalpy+GRAVITY*(sourceZ-elevation));
                    var sourceMoments=carried.solids.values();
                    if(pipe.filter()==null)for(int c=0;c<3;c++)solidIncoming[node][c]+=Math.abs(flow)*sourceMoments[c]/carried.mass;
                    else heat-=Math.abs(flow)*(carried.solids.enthalpy(source.temperature(),source.pressure())/carried.mass+sourceCapture*GRAVITY*(sourceZ-elevation));
                }
                if(receiver&&pipe.filter()!=null) {
                    double capturedRate=Math.abs(flow)*(transport.solids.enthalpy(upstream.temperature(),upstream.pressure())/transport.mass+captureFraction*GRAVITY*(donorZ-elevation));
                    stored-=dt*capturedRate;
                }
                if(!first&&pipe.control() instanceof FlowControl.Mover mover) {
                    int control=controlOffsets[edge];double head=control<0?0:x[control]*1e5;
                    // Metered on the suction stream's density (its bulk at a BULK end) and, for a compressor, the suction
                    // node's pressure at this trial (decision D8: suction properties only).
                    double power=mover.power(flow,head,drawn(edge,0,tr[a],flow).density,st[a].pressure());
                    stored+=dt*power;heat+=power;
                }
            }
            // An emptying vessel standing as a junction (decision D54, emptyingGraph) passes its stock as well as what arrives.
            energy[node]=stored;incomingMass[node]=mass;incomingEnergy[node]=heat;netMass[node]=emptyings!=null&&emptyings[node]!=null?net+emptyings[node].drain():net;
        }
        /**
         * Whether {@code node} is a junction
         * whose every connection other than {@code edge} is closed (closed by the boundary rule or a device in mode
         * CLOSED). Its mass row then forces zero flow through {@code edge} at every solution, so the edge's velocity cap
         * can never be active there, and its row's only job is to place the junction pressure. The capped branch has no
         * pressure column (in every form), so an iterate that lands on it with the flow already fixed by the mass row
         * makes the Jacobian structurally singular; form B's re-weighting of the hydraulic row by
         * pressureScale/min(pressureScale, 2 limitDrop) lets the line search accept exactly such a step
         * (IslandCertificateTest's dead-headed pump, review 7.11). Such an edge gets no cap.
         *
         * <p>The same holds when {@code node} reaches other junctions through open connections that couple their pressures
         * but the set it forms has no other way in or out: a sealed pocket behind {@code edge} (hold F8 of the phase-ports
         * batch; see {@link #pockets}): the pocket's mass rows force zero flow through {@code edge}, and on its capped branch
         * nothing places the pocket's pressure - a pipe from a generator into an inline filter's two junctions with a closed
         * pump behind them was singular at the rate pass of every interval. So {@code node} is a dead end for {@code edge}
         * when, with {@code edge} left out, the junctions its coupling connections reach touch no vessel, port, generator or
         * void, no regulating valve draws from them, and every other connection leaving them is closed. A junction whose other
         * connections are all closed is the pocket of one, as before.
         */
        private boolean deadEnd(int node,int edge) {
            if(deadEnds==null){deadEnds=new byte[2*graph.pipes().size()];}
            int slot=2*edge+(graph.pipes().get(edge).first()==node?0:1);
            if(deadEnds[slot]==0)deadEnds[slot]=(byte)(sealedWithout(node,edge)?1:2);
            return deadEnds[slot]==1;
        }
        /** Per edge end, whether that end is a dead end for the edge (1), not (2), or not yet decided (0); per pass. */
        private byte[] deadEnds;
        private boolean sealedWithout(int node,int edge){return PassiveStepSolver.sealedWithout(graph,nodeEdges,modes,boundaryClosed,node,edge);}
        /** Whether connection {@code edge} has a velocity cap in this pass: a mode that throttles (passive, a pump on its head
         * limit, an open valve), not closed, and no dead end at either end ({@link #deadEnd}). */
        boolean clamps(int edge) {
            var pipe=graph.pipes().get(edge);
            return canClamp(modes.get(edge))&&!boundaryClosed[edge]&&!deadEnd(pipe.first(),edge)&&!deadEnd(pipe.second(),edge);
        }
        /**
         * Each connection's cap state for this pass (decision A270): {@link #CAP_OPEN} on its law, or the end its cap flow
         * leaves (0 the first, 1 the second) while it runs at its cap. Given by the pass loop; a connection the loop has not
         * decided ({@link #CAP_START}) is decided once, at construction, on this pass's start point ({@link #startCaps}).
         */
        final int[] capDirections;
        /** What the last {@link #edgeRows} of a capped-capable connection read: its driving pressure, the cap's pressure drop
         * in the direction it used, that direction, and the row's pressure unit. Scratch for {@link #capTransition} and
         * {@link #startCaps}; never read by a residual. */
        private double rowDriving,rowLimitDrop,rowCapScale;private int rowDirection;
        /**
         * The start cap state of every connection neither the pass loop nor the carry decided (decision A271): on the cap
         * when its driving pressure at this pass's start point exceeds the cap's pressure drop, in the driving pressure's
         * direction (a device's or a filter's: its start flow's), otherwise on its law. It is the branch the flow row's
         * clamp read at a Newton's first iterate before the cap was a mode, so a Newton that never crossed the cap is solved
         * as it was. A connection that cannot be capped in this pass (closed, a dead end, a mode with no cap) is on its law.
         */
        /** The connections {@link #startCaps} put on their cap in this pass (decided on the start point, not by the pass loop). */
        private boolean[] startCapped;
        private void startCaps() {
            startCapped=new boolean[capDirections.length];
            boolean any=false;
            for(int edge=0;edge<capDirections.length;edge++) {
                if(!clamps(edge))capDirections[edge]=CAP_OPEN;
                else if(capDirections[edge]==CAP_START)any=true;
            }
            if(!any)return;
            double[] scratch=new double[size];
            try {
                decode(initialPoint);
                for(int edge=0;edge<capDirections.length;edge++) {
                    if(capDirections[edge]!=CAP_START)continue;
                    capDirections[edge]=CAP_OPEN;edgeRows(edge,initialPoint,cachedStates,cachedTransport,scratch);
                    if(Math.abs(rowDriving)>rowLimitDrop){capDirections[edge]=rowDirection;startCapped[edge]=true;}
                }
            }catch(IllegalArgumentException outsideDomain){for(int edge=0;edge<capDirections.length;edge++)if(capDirections[edge]==CAP_START)capDirections[edge]=CAP_OPEN;}
        }
        /**
         * Owner decision D59 (5), the capped junctions (A276, A341), t28: a junction whose lines are all choked must still be
         * able to balance. A junction holds nothing (D46), so its mass balance is what places its pressure. A line on its law
         * reads that pressure; a line on its cap reads it only when the cap leaves the junction (the cap flow of the
         * junction's own state, rho_J A v_lim, which rises with its pressure); a cap that enters it, a mover at its target and
         * a closed line read it not at all. So a junction with no line on its law balances only if some cap leaves it and some
         * cap enters it: then its pressure settles where its outflow caps carry what its inflow caps bring (random 529: a
         * junction between a generator's capped line and two capped vents). Where every cap enters it, nothing places its
         * pressure and the step is singular (random 529, 498: a junction whose three lines all enter it at their caps; world
         * defaults 228: a pump's discharge junction left with the pump's target, a closed generator line and a void line
         * entered on its cap from a failed iterate); where every cap leaves it and nothing enters, its balance has no root
         * (random 529: a junction seeded at its generator's pressure whose four lines all leave it). In real life its
         * pressure moves until one of its lines leaves its choke: the one whose drive exceeds its cap's pressure drop by the
         * least, in pascals, which is the shift of the junction's pressure that unchokes it. That line is on its law in this
         * pass, read on the pass's start point, decided for the whole pass with the other cap states before the Newton.
         *
         * <p>The same holds for a group of junctions joined by lines on their law (the pockets of hold F8, with capped lines
         * as their boundary): their law lines carry their pressures together, so the group's level is placed only by a law
         * line to a node that is not a junction, a row that fixes a member's pressure (a regulating valve, a pump on its
         * suction limit), or caps on its boundary both leaving and entering it (random 498: two junctions joined by one law
         * line whose five boundary caps all enter them). One line is released per group and round, until every group can
         * balance; a group with no capped line on its boundary is F8's.
         *
         * <p>Only a cap the start point decided is released ({@link #startCaps}): a cap the pass loop entered from a converged
         * point or a failed iterate (A271, A274) is evidence from a Newton that ran, and releasing it returns the pass loop to
         * the state that produced it (random 915: a generator line entered on its cap by a failed iterate, beside a pump at its
         * target, released again, and the passes repeated).
         */
        private void anchorJunctions() {
            int nodes=layout.length,edges=graph.pipes().size();double[] excess=null;
            for(int round=0;round<=edges;round++) {
                // Groups of junctions joined by connections on their law; a group is anchored by a law connection to a node
                // that is not a junction, or by a connection whose row fixes a member's pressure.
                int[] parent=new int[nodes];for(int i=0;i<nodes;i++)parent[i]=i;
                boolean[] anchoredNode=new boolean[nodes];
                for(int e=0;e<edges;e++) {
                    var mode=modes.get(e);if(closedEdge(mode,boundaryClosed[e])||capDirections[e]>=0)continue;
                    var pipe=graph.pipes().get(e);int a=pipe.first(),b=pipe.second();
                    boolean ja=graph.reservoirs().get(a).junction(),jb=graph.reservoirs().get(b).junction();
                    if(couples(mode,false)){if(ja&&jb){int ra=root(parent,a),rb=root(parent,b);if(ra!=rb)parent[Math.max(ra,rb)]=Math.min(ra,rb);}else{anchoredNode[a]|=ja;anchoredNode[b]|=jb;}}
                    else if(anchors(mode))anchoredNode[a]|=ja;
                }
                boolean[] anchored=new boolean[nodes],leaves=new boolean[nodes],enters=new boolean[nodes];
                for(int i=0;i<nodes;i++)if(anchoredNode[i])anchored[root(parent,i)]=true;
                int[] best=new int[nodes];Arrays.fill(best,-1);double[] least=new double[nodes];Arrays.fill(least,Double.POSITIVE_INFINITY);
                for(int e=0;e<edges;e++) {
                    if(capDirections[e]<0||closedEdge(modes.get(e),boundaryClosed[e]))continue;
                    var pipe=graph.pipes().get(e);int donor=capDirections[e]==0?pipe.first():pipe.second(),other=donor==pipe.first()?pipe.second():pipe.first();
                    boolean jd=graph.reservoirs().get(donor).junction(),jo=graph.reservoirs().get(other).junction();
                    int rd=jd?root(parent,donor):-1,ro=jo?root(parent,other):-1;
                    if(rd>=0&&rd==ro)continue;
                    if(excess==null) {
                        excess=new double[edges];Arrays.fill(excess,Double.NaN);
                        try{decode(initialPoint);}catch(IllegalArgumentException outsideDomain){return;}
                    }
                    if(Double.isNaN(excess[e])) {
                        try{edgeRows(e,initialPoint,cachedStates,cachedTransport,new double[size]);excess[e]=Math.abs(rowDriving)-rowLimitDrop;}
                        catch(IllegalArgumentException outsideDomain){excess[e]=Double.POSITIVE_INFINITY;}
                    }
                    if(startCapped[e])for(int r:new int[]{rd,ro})if(r>=0&&excess[e]<least[r]){least[r]=excess[e];best[r]=e;}
                    if(rd>=0)leaves[rd]=true;
                    if(ro>=0)enters[ro]=true;
                }
                boolean changed=false;
                for(int r=0;r<nodes;r++)if(best[r]>=0&&!anchored[r]&&!(leaves[r]&&enters[r])&&capDirections[best[r]]>=0){capDirections[best[r]]=CAP_OPEN;changed=true;}
                if(!changed)return;
            }
        }
        /**
         * The cap state the pass loop moves connection {@code edge} to at the converged point {@code x}, or its own when the
         * point holds it (decision A271). On its law, it enters the cap when its flow exceeds the cap flow of the end that
         * flow leaves, by the margin the velocity constraint was always checked with ({@code 2e-8} relative); on its cap, it
         * leaves when the driving pressure in its direction falls short of the cap's pressure drop by more than the row
         * resolves ({@code tolerance} of its pressure unit): then the line could not push the cap flow, and runs on its law.
         * Inside that band a connection keeps its mode, so a point at the cap's edge, where both modes are the same
         * equation, never alternates.
         */
        int capTransition(int edge,double[] x,double tolerance) {
            if(!clamps(edge))return CAP_OPEN;
            decode(x);
            double[] scratch=new double[size];
            edgeRows(edge,x,cachedStates,cachedTransport,scratch);
            int cap=capDirections[edge];
            if(cap>=0)return (cap==0?rowDriving:-rowDriving)<rowLimitDrop-tolerance*rowCapScale?CAP_OPEN:cap;
            var pipe=graph.pipes().get(edge);double q=x[edgeOffset+edge];int end=q>=0?0:1;
            var node=cachedTransport[end==0?pipe.first():pipe.second()];
            double limit=pipe.filter()==null?capLimit(edge,end,node):filterCapFlow(edge,end,node,drawn(edge,end,node,q));
            return Math.abs(q)>limit*(1+2e-8)+1e-12?end:CAP_OPEN;
        }
        /**
         * The cap corrections a failed Newton's last iterate {@code x} shows (decision A274), applied to {@code caps}; true when
         * any was made. Every capped connection {@link #capTransition} would release there is released; when none is, every
         * connection on its law that the iterate holds at its cap ({@link #projectFlows}: its flow at the cap flow of the end
         * it leaves, its drive past the cap's pressure drop in that direction) is put on its cap. A drive past the cap drop
         * at a flow still below the cap is a Newton not yet converged, not a line at its cap. A point outside the property
         * domain changes nothing.
         */
        boolean correctCaps(double[] x,int[] caps,double tolerance) {
            int[] released=caps.clone(),entered=caps.clone();boolean release=false,enter=false;
            try {
                double[] scratch=new double[size];
                for(int edge=0;edge<caps.length;edge++) {
                    if(!clamps(edge))continue;
                    if(capDirections[edge]>=0){if(capTransition(edge,x,tolerance)==CAP_OPEN){released[edge]=CAP_OPEN;release=true;}continue;}
                    decode(x);edgeRows(edge,x,cachedStates,cachedTransport,scratch);
                    var pipe=graph.pipes().get(edge);double q=x[edgeOffset+edge];int end=q>=0?0:1;var node=cachedTransport[end==0?pipe.first():pipe.second()];
                    double limit=pipe.filter()==null?capLimit(edge,end,node):filterCapFlow(edge,end,node,drawn(edge,end,node,q));
                    // The drive in the flow's direction, as capTransition releases on it: a line is choked only by a drive
                    // that pushes its flow (random seed 200: a filter line drawing 0.99 kg/s against a 325 kPa drive was
                    // released and entered again at the same point, and the pass sequence cycled).
                    if(Math.abs(q)>=limit*(1-1e-9)&&end==rowDirection&&(rowDirection==0?rowDriving:-rowDriving)>rowLimitDrop){entered[edge]=rowDirection;enter=true;}
                }
            }catch(IllegalArgumentException outsideDomain){return false;}
            if(release)System.arraycopy(released,0,caps,0,caps.length);
            else if(enter)System.arraycopy(entered,0,caps,0,caps.length);
            return release||enter;
        }
        /**
         * The cap flow (kg/s) of an inline filter's connection leaving end {@code end}: the drawn stream's velocity cap
         * ({@link #capLimit}) and, in a step that may fill the cake, the room the cake has left over the step's own
         * retention per kilogram of {@code drawn}, the stream the flow carries.
         */
        private double filterCapFlow(int edge,int end,Transport node,Transport drawn) {
            var pipe=graph.pipes().get(edge);
            double limit=capLimit(edge,end,node);
            if(!rateOnly&&!prescribedFlow) {
                // A filter has a second inlet limit: the cake it can still hold. The
                // retained volume at the end of this step is the same expression the
                // loading row below states, so bounding the inlet by the room divided by
                // the step's own retention per unit mass makes the filter fill exactly at
                // a step boundary instead of past one. A rate solve carries no step and no
                // retention, so it is exempt, and a base already at capacity is left to
                // the stage guard rather than throttled to a degenerate zero limit.
                double retainedPerMass=drawn.solids.volume()/drawn.mass;
                double room=pipe.filter().capacity()*(1-FILTER_CAPACITY_MARGIN)-pipe.filter().captured().volume();
                if(retainedPerMass>0&&room>0)limit=Math.min(limit,room/(dt*retainedPerMass));
            }
            return limit;
        }
        /** The hydraulic row of one edge and its actuator row, from the two endpoint states alone. */
        private void edgeRows(int edge,double[] x,FluidThermodynamics.State[] st,Transport[] tr,double[] f) {
            var pipe=graph.pipes().get(edge);int a=pipe.first(),b=pipe.second();double flow=x[edgeOffset+edge];
            // The loss and a device's cap read the stream of the end the flow leaves.
            int donor=flow>=0?a:b;var transport=drawn(edge,flow>=0?0:1,tr[donor],flow);double rho=transport.density;
            var suction=drawn(edge,0,tr[a],flow);
            double dz=graph.reservoirs().get(b).elevation()-graph.reservoirs().get(a).elevation();
            double loss=pipe.pressureDrop(flow,rho,transport.viscosity);
            double filterCoefficient=0;
            if(pipe.filter()!=null){
                // The cake is loaded, and its resistance scaled, per kilogram of the drawn stream: its fluid volume, its
                // carrier's viscosity and its solids.
                var state=st[donor];double fluidVolume=transport.volume-transport.solids.volume();double carrier=drawnCarrierViscosity(edge,flow>=0?0:1,state,tr[donor],flow);
                double loading=x[filterOffsets[edge]];
                filterCoefficient=pipe.filter().cleanResistance()*(carrier/.001)*(1+99*loading*loading)*fluidVolume/transport.mass;loss=filterCoefficient*flow;
                double volume=pipe.filter().captured().volume()+(rateOnly?0:dt)*Math.abs(flow)*transport.solids.volume()/transport.mass;
                f[filterOffsets[edge]]=loading-volume/pipe.filter().capacity();
            }
            int control=controlOffsets[edge];double head=control<0?0:x[control]*1e5;
            double signedHead=pipe.control() instanceof FlowControl.Mover?head:-head;
            // The static column is this pass's, never the live iterate's upwind end; see
            // {@link #columnEnds}. On an island whose devices all sit at one y, {@code dz} is
            // zero and this term is bitwise the zero it always was.
            // A pump's column is the fluid it moves, the suction node's transported density at this trial - the density
            // its rise limit, its target row, its velocity clamp and the pass loop's shutoff margin ({@link #demand}) read -
            // never the destination's (owner rule, decision D6 of the mixed-gas junction batch): the stream its suction end
            // draws, which is the suction's bulk density at a BULK end.
            // The column end is the pass's; its density is the trial's ({@link #columnEnds}).
            double column=pipe.control() instanceof FlowControl.Mover?suction.density
                    :columnEnds[edge]==0?endDensity(st[a],pipe.firstPort()):endDensity(st[b],pipe.secondPort());
            double driving=endPressure(graph.reservoirs().get(a),pipe.firstPort(),st[a])-endPressure(graph.reservoirs().get(b),pipe.secondPort(),st[b])-column*GRAVITY*dz+signedHead;
            // Every pressure-dimensioned row of this edge is stated in the island's own pressure,
            // never in a fixed 1e5 Pa unit; see {@link #pressureScales}. The throttled-inlet row
            // below and the closed-edge row are mass flows and keep their own flow scale, and the
            // pump's target row is a volume flow.
            double pressureScale=pressureScales[edge];
            f[edgeOffset+edge]=(driving-loss)/pressureScale;
            // No cap on an edge a dead-ended junction can only carry zero through; see {@link #deadEnd}.
            if(clamps(edge)) {
                // The velocity cap is an active-set mode of the connection (decision A270; PHASE_PORTS_REVIEW.md "The
                // velocity cap as an active-set mode (2026-10-13)"): {@link #capDirections} says, for the whole pass,
                // whether this connection runs on its law or at its cap, and in which direction. On the cap its row
                // states the cap flow of the end it leaves, so it reads that end's state and neither end's pressure: a
                // choked line passes what its upstream state lets through, whatever stands downstream. Which mode a pass
                // runs in is the pass loop's decision ({@link #capTransition}), never the live iterate's, so the row is
                // smooth in its own unknowns; before, the row switched branch at |driving| = limitDrop inside the
                // residual, and a junction between capped lines saw its pressure column vanish and come back from one
                // iterate to the next (hold group E6-C). On the law, a plain passive connection's scale reads both ends'
                // cap drops, so it needs no direction; a device or a filter keeps its flow's donor (a pump or a valve
                // decides its direction through its own active set; a filter's cap is its cake's resistance on the
                // drawn carrier, re-read at every evaluation).
                boolean byDriving=pipe.filter()==null&&pipe.control() instanceof FlowControl.Passive;
                int cap=capDirections[edge];
                int direction=cap>=0?cap:(byDriving?driving>=0:flow>=0)?0:1;
                // The direction's donor end: for a device or a filter the flow's donor, which is the same end.
                var capSource=tr[direction==0?a:b];
                double limitDrop;
                if(pipe.filter()==null)limitDrop=capDrop(edge,direction,capSource);
                else limitDrop=filterCoefficient*filterCapFlow(edge,direction,capSource,transport);
                // A saturated pressure/flow law: the unused driving pressure is throttled.
                // The same bounded flow unknown enters every component and enthalpy balance.
                // No post-solve clipping, temperature prescription, or inventory adjustment.
                //
                // Both modes are stated in one pressure unit, the loss against the driving pressure on the law and against
                // the cap's own drop on the cap, over S = min(pressureScale, 2 scaleDrop). The capped branch used to be a flow row,
                // (+-limit - q)/limit, on a scale 80x apart from the hydraulic row's on a 50 mm line, and a Newton crossing
                // the cap exit stalled on the mismatch; rescaling only the capped branch left the same mismatch at the
                // cap's edge (review 7.1, 7.5). The floor keeps a filter with no room left from a zero scale.
                //
                // On a plain passive connection scaleDrop is the smaller of the two ends' cap drops, not the donor's.
                // The scale weighs the row; it says nothing about which way the fluid goes, and at a zero-flow root
                // neither end is the donor. Taken from the donor chosen by the driving pressure's sign, S jumped
                // exactly at that root wherever the two ends hold different fluids (water at one end, nitrogen at the
                // other: cap drops orders apart), every differenced Jacobian straddled the jump (the pressure column's
                // step far exceeds the driving pressure near the root) and the Newton contracted only linearly, to
                // the iteration limit at every step size (hold class E6 of the phase-ports batch, random seed 33).
                // The smaller drop keeps the property the cap scale was chosen for, both branches in one unit no
                // coarser than the cap's own drop, on either side of the root. A device or a filter keeps its flow
                // donor's drop: a device does not turn round inside a pass (its active set closes it), and a filter's
                // drop is its cake's resistance on the drawn carrier, re-read at every evaluation.
                double scaleDrop=byDriving?Math.min(limitDrop,capDrop(edge,1-direction,tr[direction==0?b:a])):limitDrop;
                double capScale=Math.max(Math.min(pressureScale,2*scaleDrop),1e-12);
                f[edgeOffset+edge]=(driving-loss)/capScale;
                rowDriving=driving;rowLimitDrop=limitDrop;rowDirection=direction;rowCapScale=capScale;
                // On the cap: loss(q) = the loss at the cap flow of the end it leaves, i.e. q is that cap flow, stated in
                // the hydraulic row's own pressure unit (the two branches of a connection must share one unit; review 7.1).
                if(cap>=0)f[edgeOffset+edge]=((direction==0?limitDrop:-limitDrop)-loss)/capScale;
            }
            if(boundaryClosed[edge]&&control<0)f[edgeOffset+edge]=flow;
            if(control>=0)f[control]=switch(modes.get(edge)) {
                case PUMP_TARGET->(flow/suction.density-((FlowControl.Mover)pipe.control()).targetVolumeFlow())/.01;
                // The limit reads the suction's density at this trial, so the row carries it into the Jacobian: the block
                // sweep re-evaluates this edge's rows for every perturbed column of its first node, and the sparsity
                // already declares the actuator row on both endpoints' columns (buildSparsity).
                case PUMP_HEAD_LIMIT->(head-riseLimit((FlowControl.Mover)pipe.control(),suction.density,st[a].pressure()))/pressureScale;
                // Cavitation (owner decision D59 (4)): the suction end stands at the vapour pressure of the water at its
                // temperature; the head is free, and the hydraulic row with the suction line's sets the flow.
                case PUMP_SUCTION_LIMIT->(endPressure(graph.reservoirs().get(a),pipe.firstPort(),st[a])-model.saturationPressure(st[a].temperature()))/pressureScale;
                case VALVE_REGULATING->(st[a].pressure()-((FlowControl.PressureValve)pipe.control()).targetPressure())/pressureScale;
                case VALVE_OPEN->head/pressureScale;
                // A mover refused for the slice (decision D6) is closed: its flow is exactly zero.
                case CLOSED,INLET_WRONG_PHASE->flow;
                case PASSIVE,VELOCITY_LIMITED,PUMP_VELOCITY_LIMIT,VALVE_VELOCITY_LIMIT->throw new IllegalStateException("Presentation-only or passive mode has actuator unknown");
            };
        }
        /**
         * The Jacobian from per-node block perturbations instead of a coloured whole-island sweep.
         *
         * <p>One local column at a time, for every node that owns one: the node is decoded at the
         * perturbed point and only the rows that can see it are re-assembled - its own block, the
         * block of each neighbour across an incident edge (whose backward-Euler targets and
         * aggregate solid targets carry this node's composition, solid moments and specific
         * enthalpy when it is the donor), and the hydraulic, actuator and filter-loading rows of
         * those edges. The flow, actuator and filter columns sweep the same way over their own
         * edge. Every such row is produced by the same {@link #nodeAccumulate},
         * {@link #edgeRows} and {@link #nodeRows} the whole-island residual uses, in the same
         * accumulation order, so each entry is the double the coloured sweep computed: the
         * colouring guarantees that no other column in a group can reach a row, which is exactly
         * the statement that evaluating that row with one column perturbed gives the same value.</p>
         *
         * <p>The cost this removes is the assembly, not the decodes. The coloured sweep already
         * decodes each node once per one of its own columns - the colour groups are
         * {@code (column, independent node set)} pairs - but it pays a complete island residual,
         * every node block and every edge row, for each of those 143-191 groups.</p>
         */
        public int differentiateEntries(double[] x,double[] f,double differenceStep,int[] entryOffsets,int[] entryRows,
                                        double[] entries,Runnable checkpoint) {
            if(sparsity==null)buildSparsity();
            FluidThermodynamics.State[] st,baseStates;Transport[] tr,baseTransport;FluidThermodynamics.Prepared[] pr,basePrepared;
            double[] trial,perturbed;
            try {
                decode(x);
                st=cachedStates.clone();tr=cachedTransport.clone();pr=cachedPrepared.clone();
                baseStates=cachedStates.clone();baseTransport=cachedTransport.clone();basePrepared=cachedPrepared.clone();
                trial=x.clone();perturbed=new double[size];
            }catch(IllegalArgumentException outsideDomain) {
                SolverDiagnostics.count(SolverDiagnostics.jacobianBlockFallbacks);return -1;
            }
            int columns=0;
            try {
                for(int node=0;node<layout.length;node++) {
                    if(layout[node]==null)continue;
                    for(int local=0;local<layout[node].size();local++) {
                        checkpoint.run();columns++;
                        int column=offsets[node]+local;
                        double step=differenceStep*differenceScale(column,x[column]);
                        trial[column]+=step;
                        double temperature=layout[node].temperature(trial,offsets[node]);
                        var prepared=basePrepared[node]!=null&&basePrepared[node].temperature()==temperature?basePrepared[node]:model.prepare(temperature);
                        var state=layout[node].decode(trial,offsets[node],prepared);
                        st[node]=state;pr[node]=prepared;
                        tr[node]=transport(node,state,prepared);
                        // Seed every neighbour before recomputing any of them: parallel pipes reach
                        // the same node twice, and a seed must never overwrite a recomputed row.
                        for(int edge:nodeEdges[node])seed(other(edge,node),perturbed,f);
                        nodeAccumulate(node,trial,st,tr);nodeRows(node,trial,st,pr,perturbed);
                        for(int edge:nodeEdges[node]) {
                            edgeRows(edge,trial,st,tr,perturbed);
                            int other=other(edge,node);
                            if(layout[other]!=null&&carries(edge,node,trial)) {
                                nodeAccumulate(other,trial,st,tr);nodeTargetRows(other,trial,st,perturbed);
                            }
                        }
                        write(entryOffsets,entryRows,entries,column,perturbed,f,step);
                        st[node]=baseStates[node];tr[node]=baseTransport[node];pr[node]=basePrepared[node];
                        trial[column]=x[column];
                    }
                }
                for(int edge=0;edge<graph.pipes().size();edge++) {
                    checkpoint.run();
                    var pipe=graph.pipes().get(edge);
                    // The filter's retained-volume unknown is the third column of an edge. It enters
                    // this edge's clogging resistance and its own loading row and nothing else - no
                    // node block carries it, which is what buildSparsity declares - so its whole
                    // stencil is what one edge assembly writes.
                    for(int which=0;which<3;which++) {
                        int column=which==0?edgeOffset+edge:which==1?controlOffsets[edge]:filterOffsets[edge];
                        if(column<0)continue;
                        columns++;
                        double step=differenceStep*differenceScale(column,x[column]);
                        trial[column]+=step;
                        edgeRows(edge,trial,st,tr,perturbed);
                        // No node is decoded again, so only the two endpoints' inflow rows move.
                        if(which<2)for(int node:new int[]{pipe.first(),pipe.second()})if(layout[node]!=null) {
                            seed(node,perturbed,f);nodeAccumulate(node,trial,st,tr);nodeTargetRows(node,trial,st,perturbed);
                        }
                        write(entryOffsets,entryRows,entries,column,perturbed,f,step);
                        trial[column]=x[column];
                    }
                }
            }catch(IllegalArgumentException outsideDomain) {
                // A perturbed node left the property domain. The coloured sweep has a bounded
                // one-sided stencil for exactly that, so hand the whole build back to it.
                SolverDiagnostics.count(SolverDiagnostics.jacobianBlockFallbacks);return -1;
            }
            SolverDiagnostics.count(SolverDiagnostics.jacobianBlockColumns,columns);
            return columns;
        }
        private int other(int edge,int node) {
            var pipe=graph.pipes().get(edge);return pipe.first()==node?pipe.second():pipe.first();
        }
        /** A row this column cannot move keeps the value the base residual already produced for it. */
        private void seed(int node,double[] perturbed,double[] f) {
            if(layout[node]!=null)System.arraycopy(f,offsets[node],perturbed,offsets[node],layout[node].size());
        }
        private void write(int[] entryOffsets,int[] entryRows,double[] entries,int column,double[] perturbed,double[] f,double step) {
            for(int entry=entryOffsets[column];entry<entryOffsets[column+1];entry++) {
                int row=entryRows[entry];entries[entry]=(perturbed[row]-f[row])/step;
            }
        }
        /** The balance, energy, volume, equilibrium and closure rows this node owns. */
        private void nodeRows(int node,double[] x,FluidThermodynamics.State[] st,FluidThermodynamics.Prepared[] pr,double[] f) {
            if(!graph.reservoirs().get(node).junction())
                layout[node].residual(st[node],targets[node],energy[node],graph.reservoirs().get(node).inventory().volume(),bagStart[node],f,offsets[node],x,pr[node],sweepWall==null?null:sweepWall[node]);
            else layout[node].junctionResidual(st[node],fractions,junctionInflow(node),netMass[node],retainedPressures[node],f,offsets[node],x,pr[node],junctionWeight);
            nodeSolidRows(node,x,st,f);
        }
        /**
         * Only the rows a change of this node's inflow can move. A block sweep that perturbs one of
         * this node's <em>neighbours</em> leaves the decoded state here untouched, so the volume
         * closure, the equilibrium rows and the amount normalization are the base residual's own
         * doubles and the sweep seeds them from it rather than recomputing them.
         */
        private void nodeTargetRows(int node,double[] x,FluidThermodynamics.State[] st,double[] f) {
            if(!graph.reservoirs().get(node).junction())
                layout[node].balanceRows(st[node],targets[node],energy[node],graph.reservoirs().get(node).inventory().volume(),bagStart[node],f,offsets[node],x,sweepWall==null?null:sweepWall[node]);
            else layout[node].junctionRows(st[node],fractions,junctionInflow(node),netMass[node],retainedPressures[node],f,offsets[node],x,junctionWeight);
            nodeSolidRows(node,x,st,f);
        }
        /**
         * This node's three aggregate solid rows against the targets <em>this</em> evaluation
         * accumulated, which is what makes them rows a neighbour's column can move.
         *
         * <p>{@link PhaseLayout#balanceRows} and {@link PhaseLayout#junctionRows} already write
         * these rows, but against the layout's own seed reference, because a layout evaluated on its
         * own has no accumulated target. Both whole-island assembly and block sweep therefore have
         * to overwrite them here, with the same targets in the same order, or a neighbour's column
         * would be differentiated against the seed reference while the base residual holds the real
         * one - a silently wrong derivative rather than a missing one.
         */
        private void nodeSolidRows(int node,double[] x,FluidThermodynamics.State[] st,double[] f) {
            if(!solidSupport)return;
            if(!graph.reservoirs().get(node).junction()) {
                layout[node].solidRows(st[node],x,offsets[node],solidTargets[node],false,f);return;
            }
            // A junction's solid moments are its owned stock's, mixed with what arrives like its fluid; a rate-only solve
            // holds them at the stock's own (see junctionInflow).
            var inventory=graph.reservoirs().get(node).inventory();
            double retained=(molarMass(oldAmounts[node])+inventory.solids().massKg())/dt,total=incomingMass[node]+retained;
            var solids=solidIncoming[node].clone();var held=inventory.solids().moments().values();
            for(int c=0;c<3;c++)solids[c]=rateOnly?held[c]/(retained*dt):(solids[c]+held[c]/dt)/total;
            layout[node].solidRows(st[node],x,offsets[node],solids,true,f);
        }
        /**
         * Fills {@link #fractions} with the mass fractions this junction's rows mix to and returns the specific enthalpy,
         * and sets {@link #junctionWeight}.
         *
         * <p>A step solve is backward Euler over the step on the junction's owned inventory,
         * {@code (m/dt + Q) w = (m/dt) w_old + sum q w_e} and {@code (m/dt + Q) h = E_old/dt + sum q (h_e + g dz)}, with m,
         * w_old and E_old = m h_old (the inventory's energy field, in enthalpy form) read from the graph this step solves -
         * the same numbers the vessel row of {@code ConservativeTransport.reconstruct} reads. The rows are stated in amount
         * form over the junction's own mass, {@code (dt/m)(m/dt + Q)(w - w_mix)}: a row at the Newton tolerance then bounds
         * the step's species error at tol m_J, and the rounding floor is {@code eps (1 + dt Q/m)}, which does not grow as
         * the step shrinks (in rate form, {@code eps (m/dt + Q)} kg/s, it did: every refinement chain ended on it; review
         * 8.3 class 4 and 8.7 (c) C).
         *
         * <p>A rate-only solve evaluates a differential state at its own value: the junction's composition and enthalpy
         * are held at its owned inventory's, its pressure stays algebraic, and the reconstruction books its accumulation
         * as a pseudo-boundary at the junction ({@code ConservativeTransport.reconstruct}, frozen junctions).
         */
        private double junctionInflow(int node) {
            var inventory=graph.reservoirs().get(node).inventory();var held=oldAmounts[node];
            double heldMass=molarMass(held)+inventory.solids().massKg(),total=incomingMass[node]+heldMass/dt;
            if(rateOnly) {
                junctionWeight=1;
                for(int c=0;c<fractions.length;c++)fractions[c]=held[c]*model.molecularWeight(c)/heldMass;
                return inventory.internalEnergy()/heldMass;
            }
            junctionWeight=total*dt/Math.max(heldMass,1e-300);
            for(int c=0;c<fractions.length;c++)
                fractions[c]=(incoming[node][c]+held[c]/dt)*model.molecularWeight(c)/total;
            return (incomingEnergy[node]+inventory.internalEnergy()/dt)/total;
        }
        /** Whether this edge can carry a change of {@code node}'s decoded state into the other
         * endpoint's rows: the transported amounts and enthalpy are the donor's, a junction's
         * mixture is this pass's frozen donor's, and a pump's shaft power is metered on the first
         * endpoint's density. */
        private boolean carries(int edge,int node,double[] x) {
            var pipe=graph.pipes().get(edge);
            return node==(x[edgeOffset+edge]>=0?pipe.first():pipe.second())
                    ||graph.reservoirs().get(other(edge,node)).junction()&&node==(junctionDonorFirst[edge]?pipe.first():pipe.second())
                    ||node==pipe.first()&&pipe.control() instanceof FlowControl.Mover;
        }
    }
}
