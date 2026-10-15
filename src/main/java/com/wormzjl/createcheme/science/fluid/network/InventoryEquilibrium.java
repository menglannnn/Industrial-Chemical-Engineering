package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.solver.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.thermo.PhaseSupport;
import java.util.*;

/** Refreshes a derived state from conserved inventory before an interval, never inside a network residual. */
public final class InventoryEquilibrium {
    private InventoryEquilibrium() {}
    public static PassiveNetwork refresh(PassiveNetwork graph,FluidThermodynamics model,Runnable checkpoint) {
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();boolean changed=false;
        for(var node:graph.reservoirs()) {
            checkpoint.run();FluidThermodynamics.State state;
            // A committed inventory the property domain refuses names its node; see ThermoDomainViolation.
            try{state=node.fixed()||node.junction()||node.empty()?node.state():solve(model,node.state(),node.inventory(),false,checkpoint);}
            catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoDomainViolation violation){throw violation.at(node.id());}
            changed|=state!=node.state();nodes.add(new PassiveNetwork.Reservoir(node.id(),node.elevation(),state,node.kind(),node.inventory()));
        }
        return changed?new PassiveNetwork(nodes,graph.pipes(),graph.scheduledTransfers()):graph;
    }
    public static FluidThermodynamics.State solve(FluidThermodynamics model,FluidThermodynamics.State previous,PassiveNetwork.Inventory inventory,boolean forceProperties,Runnable checkpoint) {
        return solve(model,previous,inventory,forceProperties,Double.NaN,checkpoint);
    }
    /** {@link #solve} with the wall work of a step booked from the bag volume {@code workFrom} ({@link PhaseLayout#balanceRows}). */
    static FluidThermodynamics.State solve(FluidThermodynamics model,FluidThermodynamics.State previous,PassiveNetwork.Inventory inventory,boolean forceProperties,
                                           double workFrom,Runnable checkpoint) {
        var n=inventory.moles();if(Arrays.stream(n).sum()==0)return close(model,previous,inventory,null,workFrom,checkpoint);
        var old=PhaseLayout.totalAmounts(previous);boolean changedBasis=false;
        if(n.length!=old.length||n.length!=model.hydrocarbon.componentCount()+1)throw new IllegalArgumentException("Inventory equilibrium basis mismatch");
        for(int c=0;c<n.length;c++)changedBasis|=(n[c]==0)!=(old[c]==0)||Math.abs(n[c]-old[c])>1e-12+1e-10*Math.max(n[c],old[c]);
        // Decision D34: the phase set each pass below is laid out for is the flash's, trace phases folded; a candidate that
        // ends with a phase its flash folds is restated from that flash (the regime test) and its last split is folded by
        // repartition, so this energy-volume flash returns no trace phase either.
        var state=(forceProperties||changedBasis?model.flashTP(previous.temperature(),previous.pressure(),n,checkpoint):model.folded(previous)).withSolids(inventory.solids());
        // Owner decision D59 (2): a vessel that holds nothing at the bag's resolution holds a residue, booked exactly in its
        // inventory, and no state to close: it is stated at its flash, at the temperature and pressure it had. Random fuzz
        // seed 707 booked 1e-42 mol of crude and 1e-84 mol of water vapour into an empty 1 m3 tank; the closure's Newton,
        // differencing that water with a step 1e19 times the vessel's contents, found no finite trial at any step size.
        if(PassiveStepSolver.holdsNothing(inventory,state))return state;
        var seen=new HashSet<String>();
        for(int pass=0;pass<16;pass++) {
            checkpoint.run();if(!seen.add(regime(state)))throw new SparseNewton.Nonconvergence("Inventory equilibrium phase cycle");
            FluidThermodynamics.State candidate;
            try{candidate=close(model,state,inventory,null,workFrom,checkpoint);}
            catch(SparseNewton.Nonconvergence failure) {
                if(failure.lastVariables()==null)throw failure;
                var trial=layout(model,state,null).decode(failure.lastVariables(),0);var phase=model.flashTP(trial.temperature(),trial.pressure(),n,checkpoint).withSolids(inventory.solids());
                if(regime(phase).equals(regime(state)))throw failure;state=phase;continue;
            }
            if(candidate==state)return state;
            var stable=model.flashTP(candidate.temperature(),candidate.pressure(),n,checkpoint).withSolids(inventory.solids());
            if(!regime(stable).equals(regime(candidate))){state=stable;continue;}
            return candidate;
        }
        throw new SparseNewton.Nonconvergence("Inventory equilibrium phase limit");
    }
    /**
     * The tolerance to which a vessel's stated state matches its inventory, on the vessel's own equations (component
     * balances over the node's amount, energy, the wall; {@link PhaseLayout#wallClosure}): two orders below the step
     * Newton's 1e-9, so a lag between what a vessel holds and what its state says can never grow to the step's tolerance
     * (review "Hold class E6, grid overshoot (G2)": carried at zero iterations, it did, and the tanks of the extreme grid
     * passed their generator once it jumped).
     */
    public static final double CLOSURE=1e-11;
    /**
     * Decision D36, G2: the vessel's state at what it holds. Its inventory (amounts, internal energy) and its capacity are
     * the conserved truth and fix temperature, pressure and volume through the equation of state and the wall (decision
     * D54: a bag with a stiff wall, {@link PhaseLayout#wallRow}), so the energy and wall equations
     * are solved, in the phase layout of {@code state} and from it, to {@link #CLOSURE}, and the result is given exactly
     * the inventory's amounts ({@link ConservativeTransport#repartition}). A state already within the closure is returned
     * as it is (an island at rest stays bit for bit). The phase set is {@code state}'s: whether it holds is the caller's
     * question (the refresh's regime test above; in a step, the pass loop's phase correction of the reconstructed state).
     * One closure for the interval-start refresh and every reconstruction ({@code ConservativeTransport.reconstruct0}).
     * A vessel holding dry solids only is at the temperature its energy gives the solids, at {@code state}'s pressure.
     *
     * <p>The equilibrium it is closed to is the one the step solves the vessel with: {@code support}, the frozen phase
     * support of the step's own layout for this node (a component the trace truncation holds in one phase has no split
     * unknown and no equilibrium row), or, when null, the support the next solve derives from {@code state} under the
     * model's truncation policy. Closed in full equilibrium instead, a crude tank at rest stood 0.71 Pa (3.2e-6) above
     * the truncated equilibrium the step and its equation gate state, every reconstruction moved it there, the gate refused
     * it at every step size and the polish could not bring it back (review "G2 follow-up", random seeds 400 and 737).
     *
     * <p>{@code workFrom}: the bag volume a step started from ({@link PhaseLayout#bagVolume}), when {@code inventory}'s energy
     * is the step's booked energy before the wall's work; the closed state then holds that energy less
     * {@code P_ambient (V_bag - workFrom)} ({@link PhaseLayout#balanceRows}). {@code NaN}: the inventory's energy is the
     * state's.
     */
    public static FluidThermodynamics.State close(FluidThermodynamics model,FluidThermodynamics.State state,PassiveNetwork.Inventory inventory,PhaseSupport[] support,
                                                  double workFrom,Runnable checkpoint) {
        var n=inventory.moles();double energy=inventory.internalEnergy(),volume=inventory.volume();
        if(Arrays.stream(n).sum()==0) {
            if(inventory.solids().empty())throw new IllegalArgumentException("Empty inventory has no initialized thermodynamic state");
            if(inventory.solids().volume()>volume)throw new IllegalArgumentException("Dry solids exceed vessel capacity");
            double temperature=com.wormzjl.createcheme.science.material.SolidMaterial.REFERENCE_TEMPERATURE+energy/inventory.solids().moments().heatCapacity();
            return model.solidState(temperature,state.pressure(),inventory.solids());
        }
        var layout=layout(model,state,support);var x=layout.encode(state);
        double[] residual=new double[layout.size()];layout.residual(state,n,energy,volume,workFrom,residual,0,x);
        if(norm(residual)<=CLOSURE)return state;
        var solved=SparseNewton.solve(layout.wallClosure(n,energy,volume,workFrom),x,new SparseNewton.Settings(24,CLOSURE,1e-6,24),checkpoint);
        var closed=ConservativeTransport.repartition(model,layout.decode(solved.variables(),0),n);
        // The restatement applies the flash's trace-phase rule (decision D34), so a phase the closure left as a trace is
        // folded away and the closed state is checked in its own layout. A step that folded a phase is solved again from
        // the folded state (the pass loop's recordFolds); the refresh asks its regime test.
        var check=regime(closed).equals(regime(state))?layout:layout(model,closed,null);
        x=check.encode(closed);residual=new double[check.size()];
        check.residual(closed,n,energy,volume,workFrom,residual,0,x);
        if(norm(residual)>1e-8)throw new SparseNewton.Nonconvergence("Inventory closure does not meet full equation closure");
        return closed;
    }
    /**
     * The vessel's own layout of {@code state} under {@code support}, or under the support the model's trace truncation
     * policy gives {@code state} (what the next solve's first pass lays the vessel out with) when it is null or no longer
     * fits {@code state}: a reconstruction that folded a phase (decision D34) or restated a phase appearance as its flash
     * hands over a state the pass loop solves again, laid out from it.
     */
    private static PhaseLayout layout(FluidThermodynamics model,FluidThermodynamics.State state,PhaseSupport[] support) {
        return new PhaseLayout(model,state,null,null,support!=null&&fits(support,state)?support:PhaseLayout.support(state,null,model.traceTruncation(),null));
    }
    /** Whether every component {@code state} holds has a phase under {@code support} that {@code state} holds. */
    private static boolean fits(PhaseSupport[] support,FluidThermodynamics.State state) {
        double[] l=state.liquidView(),v=state.vaporView();if(support.length!=l.length)return false;
        boolean liquid=state.liquidVolume()>0,vapor=Arrays.stream(v).sum()>0;
        for(int c=0;c<l.length;c++) {
            if(support[c]==PhaseSupport.ABSENT){if(l[c]+v[c]>0)return false;continue;}
            if(support[c].liquid()&&!liquid||support[c].vapor()&&!vapor)return false;
        }
        return true;
    }
    static String regime(FluidThermodynamics.State state){return (state.liquidVolume()>0?"L":"")+(state.vaporProperties()!=null?"G":"")+(state.waterLiquid()>0?"W":"")+(state.waterVapor()>0?"S":"");}
    private static double norm(double[] values){double maximum=0;for(double value:values){if(!Double.isFinite(value))return Double.POSITIVE_INFINITY;maximum=Math.max(maximum,Math.abs(value));}return maximum;}
}
