package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.solver.*;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.ThermoHold;
import com.wormzjl.createcheme.science.thermo.PhaseSupport;
import java.util.*;
import java.util.function.UnaryOperator;

/** Refreshes a derived state from conserved inventory before an interval, never inside a network residual. */
public final class InventoryEquilibrium {
    private InventoryEquilibrium() {}
    /**
     * An island after its refresh: {@code graph}, the island itself where nothing moved; {@code boundaries}, the ambient's
     * work on the bags whose crystals froze or melted (decision D54, booked at the vessel as the step's transport books
     * it); {@code converted}, whether any stock changed (a crystal froze or melted, or a stock's sizes of one solid were
     * merged), in which case the island is not at rest.
     */
    public record Refreshed(PassiveNetwork graph,List<ConservativeTransport.BoundaryTransfer> boundaries,boolean converted) {
        public Refreshed{boundaries=List.copyOf(boundaries);}
    }
    /**
     * The island's states from what it holds, at an interval or solid-event segment start: each vessel's state closed to
     * its inventory ({@link #solve}), then its crystals equilibrated with its fluid ({@link VesselCrystals}, owner decision
     * D23: the one place crystal mass moves), and every stock reduced to one size per solid (D23, amended 2026-10-15:
     * {@link SolidInventory#oneSizePerSolid}): the vessels, the junction buffers and the filter cakes. A stock already
     * holding one size per solid, a vessel at its crystal equilibrium and an island of fluids only keep every bit.
     */
    public static Refreshed refresh(PassiveNetwork graph,FluidThermodynamics model,Runnable checkpoint) {
        var nodes=new ArrayList<PassiveNetwork.Reservoir>();var boundaries=new ArrayList<ConservativeTransport.BoundaryTransfer>();
        boolean changed=false,converted=false;
        for(var node:graph.reservoirs()) {
            checkpoint.run();PassiveNetwork.Reservoir refreshed;
            // A committed inventory whose phases the network holds on names its node, directly or through the failed
            // closure such a hold stopped; see ThermoHold.
            try{refreshed=refresh(node,model,boundaries,checkpoint);}
            catch(com.wormzjl.createcheme.science.fluid.thermo.ThermoHold hold){throw hold.at(node.id());}
            catch(SparseNewton.Nonconvergence failure){throw failure.at(node.id());}
            changed|=refreshed!=node;converted|=refreshed.inventory()!=node.inventory();nodes.add(refreshed);
        }
        var pipes=graph.pipes();List<PassiveNetwork.Pipe> merged=null;
        for(int i=0;i<pipes.size();i++) {
            var filter=pipes.get(i).filter();if(filter==null)continue;
            var cake=filter.captured().oneSizePerSolid();if(cake==filter.captured())continue;
            if(merged==null)merged=new ArrayList<>(pipes);
            merged.set(i,pipes.get(i).withFilter(new InlineFilter(filter.capacity(),filter.cleanResistance(),cake,filter.energyJoule(),filter.stoppedAtCapacity())));
        }
        if(merged!=null)changed=converted=true;
        return new Refreshed(changed?new PassiveNetwork(nodes,merged==null?pipes:merged,graph.scheduledTransfers()):graph,boundaries,converted);
    }
    /** One node of {@link #refresh}; the ambient's work on a vessel whose bag moved is added to {@code boundaries}. */
    private static PassiveNetwork.Reservoir refresh(PassiveNetwork.Reservoir node,FluidThermodynamics model,List<ConservativeTransport.BoundaryTransfer> boundaries,
                                                    Runnable checkpoint) {
        if(node.fixed()||node.empty())return node;
        var inventory=node.inventory();var solids=inventory.solids().oneSizePerSolid();
        if(solids!=inventory.solids())inventory=new PassiveNetwork.Inventory(inventory.volume(),inventory.moles(),inventory.internalEnergy(),solids);
        if(node.junction()) {
            // A junction's buffer is a stock of its solids; its state carries the same populations at its own amount scale.
            if(inventory==node.inventory())return node;
            var held=node.state().solids();
            var state=held.empty()?node.state():node.state().withSolids(solids.scale(held.massKg()/solids.massKg()));
            return new PassiveNetwork.Reservoir(node.id(),node.elevation(),state,node.kind(),inventory);
        }
        FluidThermodynamics.State state=null;RuntimeException unclosed=null;
        try{state=solve(model,node.state(),inventory,Double.NaN,checkpoint);}
        catch(SparseNewton.Nonconvergence|ThermoHold failure) {
            // A fluid far past a crystal's saturation (it arrived within the interval, D23 (5)) may have no fluid-only state
            // the network carries: the crystal test states the vessel from its inventory, or the failure stands.
            if(!VesselCrystals.competes(model,inventory,node.state()))throw failure;
            unclosed=failure;
        }
        var settled=state!=null?VesselCrystals.settle(model,inventory,state,checkpoint):VesselCrystals.settle(model,inventory,node.state(),false,checkpoint);
        if(state==null){if(settled==null)throw unclosed;state=settled.state();}
        if(settled!=null) {
            state=settled.state();inventory=settled.inventory();
            if(settled.wallWork()!=0)boundaries.add(new ConservativeTransport.BoundaryTransfer(node.id(),new double[inventory.moles().length],settled.wallWork()));
        }
        return state==node.state()&&inventory==node.inventory()?node:new PassiveNetwork.Reservoir(node.id(),node.elevation(),state,node.kind(),inventory);
    }
    /** The state of a vessel's inventory, with the wall work of a step booked from the bag volume {@code workFrom}
     * ({@link PhaseLayout#balanceRows}; {@code NaN}: none). */
    static FluidThermodynamics.State solve(FluidThermodynamics model,FluidThermodynamics.State previous,PassiveNetwork.Inventory inventory,
                                           double workFrom,Runnable checkpoint) {
        var n=inventory.moles();if(Arrays.stream(n).sum()==0)return close(model,previous,inventory,null,workFrom,checkpoint);
        var old=PhaseLayout.totalAmounts(previous);boolean changedBasis=false;
        if(n.length!=old.length||n.length!=model.hydrocarbon.componentCount()+1)throw new IllegalArgumentException("Inventory equilibrium basis mismatch");
        for(int c=0;c<n.length;c++)changedBasis|=(n[c]==0)!=(old[c]==0)||Math.abs(n[c]-old[c])>1e-12+1e-10*Math.max(n[c],old[c]);
        // Decision D34: the phase set each pass below is laid out for has its trace phases folded; a candidate that ends
        // with a phase below the trace share is restated folded (the regime test) and its last split is folded by
        // repartition, so this energy-volume flash returns no trace phase either.
        var state=(changedBasis?model.flashTP(previous.temperature(),previous.pressure(),n,checkpoint):model.folded(previous)).withSolids(inventory.solids());
        // Owner decision D59 (2): a vessel that holds no fluid at the bag's resolution holds a residue, booked exactly in its
        // inventory, and no fluid state to close: it is stated at its flash with its solids, at the temperature and pressure
        // it had. Random fuzz seed 707 booked 1e-42 mol of crude and 1e-84 mol of water vapour into an empty 1 m3 tank; the
        // closure's Newton, differencing that water with a step 1e19 times the vessel's contents, found no finite trial at
        // any step size.
        if(PassiveStepSolver.holdsNoFluid(inventory,state))return state;
        return passes(model,state,new Closure(){
            @Override public FluidThermodynamics.State close(FluidThermodynamics.State start){return InventoryEquilibrium.close(model,start,inventory,null,workFrom,checkpoint);}
            @Override public FluidThermodynamics.State iterate(FluidThermodynamics.State start,double[] variables){return layout(model,start,null).decode(variables,0);}
            @Override public FluidThermodynamics.State split(FluidThermodynamics.State start){return InventoryEquilibrium.split(model,start,inventory,workFrom,checkpoint);}
        },checkpoint);
    }
    /**
     * A vessel's closure in the phase layout of a state: the inner step of the pass loop ({@link #passes}). The vessel's
     * own ({@link #close}: its fluid and inert solids at what it holds), or the crystal refresh's joint closure (its fluid
     * beside its crystals, their amounts unknowns, {@code VesselCrystals}, decision D30).
     */
    interface Closure {
        /** The vessel closed, laid out with {@code start}'s phases; a {@link SparseNewton.Nonconvergence}, with its last
         * iterate where it has one, where it stops short. */
        FluidThermodynamics.State close(FluidThermodynamics.State start);
        /** The fluid at {@code variables}, an iterate of {@link #close} from {@code start}, as its layout decodes it. */
        FluidThermodynamics.State iterate(FluidThermodynamics.State start,double[] variables);
        /** The vessel's own split from {@code start} (owner decision D26, {@link InventoryEquilibrium#split}): the closure, or
         * where it stops short its last iterate given the amounts it holds; {@code null} where it leaves neither. */
        FluidThermodynamics.State split(FluidThermodynamics.State start);
    }
    /**
     * The vessel's equilibrium from {@code state}: {@code closure} solved in a phase layout, and the layout decided by the
     * outer phase check (owner decisions D25 and D26, {@code FluidThermodynamics.phaseCheck}). A phase leaves only when the
     * closure empties it (the trace fold of its point; a failed closure's last iterate is folded as a trial layout); where
     * the TP answer finds a phase missing, the vessel is seeded at its own split, the closure started from the TP answer
     * with the phases the vessel holds, and no populated phase set is swapped for another on a TP verdict: a vessel of pure
     * water at its saturation line keeps its liquid and gains the steam its volume and energy give. A phase the check
     * removed from a converged answer, or the own split refused (the D26 amendment), is not added again within this solve
     * (A85); a failed iterate's check reads that record and never writes it (decision D28 A'), so a phase it removed comes
     * back where a converged answer finds it missing, and a repeated regime is the cycle that refuses the solve. One rule
     * for a vessel's own closure ({@link #solve}) and for the crystal refresh's joint closure (decision D30), so a phase
     * that has no room leaves both.
     */
    static FluidThermodynamics.State passes(FluidThermodynamics model,FluidThermodynamics.State state,Closure closure,Runnable checkpoint) {
        var seen=new HashSet<String>();int[] excluded={0};
        UnaryOperator<FluidThermodynamics.State> own=closure::split;
        for(int pass=0;pass<16;pass++) {
            checkpoint.run();if(!seen.add(regime(state)))throw new SparseNewton.Nonconvergence("Inventory equilibrium phase cycle");
            FluidThermodynamics.State candidate;
            try{candidate=closure.close(state);}
            catch(SparseNewton.Nonconvergence failure) {
                if(failure.lastVariables()==null)throw failure;
                var trial=closure.iterate(state,failure.lastVariables());
                // Decision D28 A': a failed closure's check is a trial layout, not a verdict: it reads the A85 record and never
                // writes to it (a copy). Laid out again with the solids of the pass's state (the vessel's own closure: its
                // inventory's).
                var checked=model.phaseCheck(trial,false,excluded.clone(),0,null,own,checkpoint);
                if(regime(checked).equals(regime(state)))throw failure;
                state=checked.withSolids(state.solids());continue;
            }
            if(candidate==state)return state;
            var checked=model.phaseCheck(candidate,false,excluded,0,null,own,checkpoint);
            if(checked!=candidate){state=checked;continue;}
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
     * as it is (an island at rest stays bit for bit). The phase set is {@code state}'s less any phase the restatement folds
     * away (decision D34), the vessel then closed again without it (E8): whether the set holds is the caller's question
     * (the refresh's pass loop, {@link #passes}; in a step, the pass loop's phase correction of the reconstructed state).
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
            double temperature=com.wormzjl.createcheme.science.material.SolidMaterialCatalog.SolidMaterial.REFERENCE_TEMPERATURE+energy/inventory.solids().moments().heatCapacity();
            return model.solidState(temperature,state.pressure(),inventory.solids());
        }
        var layout=layout(model,state,support);var x=layout.encode(state);
        double[] residual=new double[layout.size()];layout.residual(state,n,energy,volume,workFrom,residual,0,x);
        if(norm(residual)<=CLOSURE)return state;
        var solved=SparseNewton.solve(layout.wallClosure(n,energy,volume,workFrom),x,new SparseNewton.Settings(24,CLOSURE,1e-6,24),checkpoint);
        var closed=ConservativeTransport.repartition(model,layout.decode(solved.variables(),0),n);
        // The restatement applies the flash's trace-phase rule (decision D34), so a phase the closure left as a trace is
        // folded away; the vessel is then closed again without it, in the folded state's own layout (decision D30's form:
        // the fold, then the closure again without the phase; E8). Folded and only checked, a liquid-full LNG tank beside its
        // dry ice whose closure left its last bubble just under the trace share (1e-9 of its moles, 2e-8 of its volume at
        // 1.14 MPa) missed its volume by that bubble, 1.9e-8 against the 1e-8 check, and its step was refused at every size
        // (cold seed 48, 10 224 times). A fold only removes a phase (a restatement adds one only to a state the closure left
        // with no hydrocarbon phase, ConservativeTransport.repartition), so this ends. A step whose reconstruction folded a
        // phase is solved again from the folded state (the pass loop's recordFolds).
        if(!regime(closed).equals(regime(state))) {
            try{return close(model,closed,inventory,null,workFrom,checkpoint);}
            // Its iterate is one of the folded layout, which a caller decoding in the layout of {@code state} cannot read.
            catch(SparseNewton.Nonconvergence failure){throw new SparseNewton.Nonconvergence(failure.getMessage(),null,failure.phaseHold());}
        }
        x=layout.encode(closed);residual=new double[layout.size()];
        layout.residual(closed,n,energy,volume,workFrom,residual,0,x);
        if(norm(residual)>1e-8)throw new SparseNewton.Nonconvergence("Inventory closure does not meet full equation closure");
        return closed;
    }
    /**
     * Owner decision D26: a vessel's own split, the seed of a phase the outer phase check adds
     * ({@code FluidThermodynamics.phaseCheck}): its closure ({@link #close}) at {@code inventory}, from {@code start} (a state
     * holding the phases to lay the vessel out with), the wall's work booked from the bag volume {@code workFrom}: the
     * equilibrium of its energy and volume while the wall is taut, of its enthalpy at the ambient pressure while slack (of
     * its energy there with {@code workFrom} {@code NaN}, as at the interval's refresh, {@link #solve}).
     * Where the closure stops short of its tolerance its last iterate, the nearest it came, is the split, given the
     * inventory's amounts and restated with the trace fold ({@link ConservativeTransport#repartition}): from the TP split of
     * a liquid-full vessel cooled past its bubble point (several vessel volumes of vapour) the closure's 24 iterations end
     * within about 1e-5 of the bubble it holds. Converged or not, the split answers by where the closure went: a phase it
     * drove under the trace share is not in it, and the check refuses that phase for the solve (the D26 amendment), as it
     * refuses one the closure is still driving to nothing ({@code FluidThermodynamics.vanishing}, form 4).
     * {@code null} where the closure leaves no iterate or refuses the state; the check then seeds its TP-based state.
     */
    static FluidThermodynamics.State split(FluidThermodynamics model,FluidThermodynamics.State start,PassiveNetwork.Inventory inventory,double workFrom,
                                           Runnable checkpoint) {
        try{return close(model,start,inventory,null,workFrom,checkpoint);}
        catch(SparseNewton.Nonconvergence failure) {
            if(failure.lastVariables()==null)return null;
            try{return ConservativeTransport.repartition(model,layout(model,start,null).decode(failure.lastVariables(),0),inventory.moles());}
            catch(IllegalArgumentException refused){return null;}
        }
        catch(IllegalArgumentException refused){return null;}
    }
    /**
     * The vessel's own layout of {@code state} under {@code support}, or under the support the model's trace truncation
     * policy gives {@code state} (what the next solve's first pass lays the vessel out with) when it is null or no longer
     * fits {@code state}: a reconstruction that folded a phase (decision D34) or restated a phase appearance as its flash
     * hands over a state the pass loop solves again, laid out from it.
     */
    private static PhaseLayout layout(FluidThermodynamics model,FluidThermodynamics.State state,PhaseSupport[] support) {
        return new PhaseLayout(model,state,null,null,support!=null&&fits(support,state)?support:PhaseLayout.support(model,state,null,null,false));
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
