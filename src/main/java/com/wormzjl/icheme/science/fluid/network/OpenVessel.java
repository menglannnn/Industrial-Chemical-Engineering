package com.wormzjl.icheme.science.fluid.network;

import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;

import static com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics.GAS;
import static com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics.OIL;
import static com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics.WATER;

/**
 * A vessel open to the atmosphere under a floating roof ({@link PassiveNetwork.Inventory#open}; batch
 * 2026-10-21-floating-roof-tanks, owner decisions FR1, FR2 and FR8). The roof floats on the liquid, so the surface stands
 * at the ambient pressure (its wall is never taut, {@link PassiveNetwork.Inventory#wallCapacity}) and the vessel keeps
 * only its liquid:
 * <ul>
 * <li><b>Vent (form B).</b> The gas a step starts with leaves over that step at its start composition and enthalpy, as
 * a phase drawn at its capacity is drawn (decision D11): constant terms in the vessel's balance rows, no unknown. Gas
 * that forms within the step leaves in the next one. A vessel that starts a step with gas and no liquid vents nothing in
 * the step; the accepted step's flush empties it ({@code PassiveIntervalSolver.flushTraces}).</li>
 * <li><b>Overflow.</b> The liquid held above the vessel's volume (its fill height) up to the shell top is what an
 * {@link PassiveNetwork.PhasePort#OVERFLOW} port can draw over the step, lighter liquid first.</li>
 * <li><b>Spill.</b> The liquid above the shell top, {@link #FREEBOARD} over the fill height, leaves over the step like
 * the vent, lighter liquid first, with its share of the solids.</li>
 * </ul>
 * The liquids stand stratified: the heavier ({@link FluidThermodynamics#heavierLiquid}) from the bottom, the lighter on
 * it, each with its volume share of the solids ({@link FluidThermodynamics#phaseVolume}). Vent and spill leave the world,
 * booked as boundary transfers of their kinds at the vessel ({@link ConservativeTransport.BoundaryTransfer.Kind}).
 */
public final class OpenVessel {
    private OpenVessel() {}
    /** The shell's height over the fill height, m: the roof's own depth when it stands at the overflow nozzle. */
    public static final double FREEBOARD=1;
    /**
     * Form A of the vent, the comparison form of the plan (section 3.1): the accepted step's flush removes every open
     * vessel's gas, and the step itself vents nothing. Off (form B) unless the system property
     * {@code icheme.openTank.postStepVent} is {@code true}; read by the quick check's comparison only.
     */
    static final boolean POST_STEP_VENT=Boolean.getBoolean("icheme.openTank.postStepVent");

    /** What one release takes out of its vessel over a step: amounts (mol, water last), energy {@code sum m h} (J),
     * solids and their moments. */
    public record Release(double[] moles,double energyJoule,SolidInventory solids,SolidInventory.Moments moments) {
        public Release{moles=moles.clone();}
        @Override public double[] moles(){return moles.clone();}
        double mass(double[] molecularWeight) {
            double mass=moments.mass();for(int c=0;c<moles.length;c++)mass+=moles[c]*molecularWeight[c];
            return mass;
        }
    }
    /** A vessel's vent and spill over one step; either may be null. */
    public record Releases(Release vent,Release spill) {
        /** The vent and the spill that are present, in that order. */
        java.util.List<Release> each(){return vent==null?spill==null?java.util.List.of():java.util.List.of(spill):spill==null?java.util.List.of(vent):java.util.List.of(vent,spill);}
    }

    /** The shell's volume, m3: the volume at the fill height raised by {@link #FREEBOARD}. */
    public static double topVolume(PassiveNetwork.Inventory inventory) {
        return inventory.volume()*(inventory.levelHeight()+FREEBOARD)/inventory.levelHeight();
    }
    /** The volume the vessel's condensed phases fill, m3 (the liquids and the solids they carry). */
    static double condensedVolume(FluidThermodynamics.State state) {
        return FluidThermodynamics.liquidStreamVolume(state);
    }
    /** Whether {@code node} is an open vessel, or its rate-solve copy, whose start state holds liquid above its fill
     * height: the condition for its OVERFLOW ports to draw anything. */
    static boolean overflows(PassiveNetwork.Reservoir node) {
        return node.inventory().open()&&FluidThermodynamics.holdsLiquid(node.state())&&condensedVolume(node.state())>node.inventory().volume();
    }
    /** Whether an open vessel has anything to give the atmosphere: gas, or liquid above its fill height. Such a vessel is
     * never at rest ({@link NetworkReduction#atRest}). */
    static boolean releasing(PassiveNetwork.Reservoir node) {
        if(node.kind()!=PassiveNetwork.NodeKind.RESERVOIR||!node.inventory().open()||node.empty())return false;
        return FluidThermodynamics.holdsPhase(node.state(),GAS)||overflows(node);
    }

    /**
     * The volume of each phase (by phase id, gas none) the stratified liquids of {@code state} hold between the heights
     * whose volumes from the bottom are {@code lower} and {@code upper}, m3: the heavier liquid fills from the bottom, the
     * lighter stands on it.
     */
    static double[] band(FluidThermodynamics model,FluidThermodynamics.State state,double lower,double upper) {
        double[] band=new double[3];if(!(upper>lower))return band;
        int heavy=model.heavierLiquid(state),light=heavy==OIL?WATER:OIL;
        double heavyTop=FluidThermodynamics.holdsPhase(state,heavy)?FluidThermodynamics.phaseVolume(state,heavy):0;
        double lightTop=heavyTop+(FluidThermodynamics.holdsPhase(state,light)?FluidThermodynamics.phaseVolume(state,light):0);
        band[heavy]=Math.max(0,Math.min(upper,heavyTop)-Math.max(lower,0));
        band[light]=Math.max(0,Math.min(upper,lightTop)-Math.max(lower,heavyTop));
        return band;
    }
    /**
     * The mass (kg, by phase id) of each liquid an OVERFLOW port can draw over one step from {@code node}'s start state: the
     * band between its fill height and the shell top. Zeros for a vessel that does not overflow ({@link #overflows}).
     */
    static double[] overflowBand(FluidThermodynamics model,PassiveNetwork.Reservoir node) {
        double[] mass=new double[3];if(!overflows(node))return mass;
        var state=node.state();double held=condensedVolume(state);
        var band=band(model,state,node.inventory().volume(),Math.min(held,topVolume(node.inventory())));
        for(int phase=OIL;phase<=WATER;phase++)if(band[phase]>0)mass[phase]=model.phaseMass(state,phase)*band[phase]/FluidThermodynamics.phaseVolume(state,phase);
        return mass;
    }

    /**
     * What {@code node} gives the atmosphere over a step that starts from its state: its gas (the vent, form B, when the
     * vessel holds liquid as well) and the liquid above its shell top (the spill). Null for a node that is not an open
     * vessel or gives nothing. Each component's release is at most what the vessel holds of it, so roundoff between a
     * state's phase split and its inventory never books out more than it holds.
     */
    static Releases releases(FluidThermodynamics model,PassiveNetwork.Reservoir node) {
        if(node.kind()!=PassiveNetwork.NodeKind.RESERVOIR||!node.inventory().open()||node.empty()||PassiveStepSolver.holdsNoFluid(node))return null;
        var state=node.state();var held=node.inventory().moles();
        if(!FluidThermodynamics.holdsLiquid(state))return null;
        Release vent=null,spill=null;
        if(!POST_STEP_VENT&&FluidThermodynamics.holdsPhase(state,GAS)) {
            var n=FluidThermodynamics.phaseMoles(state,GAS);
            for(int c=0;c<n.length;c++){n[c]=Math.min(n[c],held[c]);held[c]-=n[c];}
            vent=new Release(n,model.phaseMass(state,GAS)*model.phaseSpecificEnthalpy(state,GAS,null),SolidInventory.EMPTY,SolidInventory.Moments.ZERO);
        }
        double top=topVolume(node.inventory()),liquid=condensedVolume(state);
        if(liquid>top) {
            var band=band(model,state,top,liquid);double[] n=new double[held.length];double energy=0;
            var solids=SolidInventory.EMPTY;double solidMass=0,solidVolume=0,solidCapacity=0;
            for(int phase=OIL;phase<=WATER;phase++) {
                if(!(band[phase]>0))continue;
                double share=band[phase]/FluidThermodynamics.phaseVolume(state,phase);
                var phaseMoles=FluidThermodynamics.phaseMoles(state,phase);
                for(int c=0;c<n.length;c++)n[c]+=share*phaseMoles[c];
                energy+=share*model.phaseMass(state,phase)*model.phaseSpecificEnthalpy(state,phase,null);
                var moments=FluidThermodynamics.phaseSolidMoments(state,phase);
                if(moments.mass()>0) {
                    solids=solids.plus(state.solids().scale(share*FluidThermodynamics.phaseSolidShare(state,phase)));
                    solidMass+=share*moments.mass();solidVolume+=share*moments.volume();solidCapacity+=share*moments.heatCapacity();
                }
            }
            for(int c=0;c<n.length;c++)n[c]=Math.min(n[c],held[c]);
            spill=new Release(n,energy,solids,new SolidInventory.Moments(solidMass,solidVolume,solidCapacity));
        }
        return vent==null&&spill==null?null:new Releases(vent,spill);
    }
}
