package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.fluid.thermo.NetworkCrystals;
import com.wormzjl.createcheme.science.material.SolidMaterialCatalog.SolidMaterial;
import java.util.TreeMap;

/**
 * The crystal part of a junction (owner decision D61 of 2026-10-19, batch {@code 2026-10-20-crystal-junction-holds};
 * amends D23, "crystal mass moves only in a vessel's refresh", for junctions): a crystal above its melting point melts on
 * arrival in a junction. The one statement of it, read by the step's junction rows ({@code PassiveStepSolver.Equations})
 * and by the reconstruction ({@link ConservativeTransport}), so the Newton point states what the booking books.
 *
 * <p>Physics. A junction is a fitting full of fluid with no refresh of its own. Above the pure crystal's melting
 * temperature ({@link NetworkCrystals.Crystal#meltingTemperature}) no amount of the crystal is stable beside any fluid,
 * so what it receives of that crystal melts at once, and what it holds of it melts too. The crystal is booked as its
 * fluid component with the crystal's own enthalpy, its stored {@code c (T - 298.15) + P v} plus its energy offset, so
 * the fusion (and, for a gas, the vaporisation) heat is charged to the junction: cold seed 778's 0.011 kg/s of CO2 crystal
 * arriving 90 K above its melting point in a 2.39 kg/s CO2 stream at 307 K costs about 7.6 kW, 2 K of that stream. Below
 * its melting point a crystal stays an inert population, as in every vessel's step (D23).</p>
 *
 * <p>Decided once per step, on the junction's state at the step's start (the graph both stages read): a switch inside a
 * residual would put a jump in the junction's rows. A rate solve holds its junctions at their stock (D46) and melts
 * nothing. The melted mass leaves the solid ledger and enters the component and energy ledgers as an internal
 * conversion ({@link ConservativeTransport.Melted}); the world's ledger counts a crystal as its component with its offset
 * already, so nothing changes there.</p>
 */
final class JunctionCrystals {
    /** Per node, the crystals that melt there by material id; null for a node where none does. */
    private final TreeMap<String,NetworkCrystals.Crystal>[] melting;
    /**
     * The island's solid populations, its holders' and its injections': the basis the reconstruction labels every solid
     * stream with ({@link ConservativeTransport}), and so what a Newton state carries whose moments its seed names no
     * population of (see {@link #split}).
     */
    private final SolidInventory island;
    private JunctionCrystals(TreeMap<String,NetworkCrystals.Crystal>[] melting,SolidInventory island){this.melting=melting;this.island=island;}

    /** The junctions of {@code graph} above the melting point of a crystal the island holds; null where none is. */
    @SuppressWarnings("unchecked")
    static JunctionCrystals of(PassiveNetwork graph,FluidThermodynamics model) {
        TreeMap<String,SolidMaterial> materials=null;var island=SolidInventory.EMPTY;
        for(var node:graph.reservoirs()){materials=crystals(node.inventory().solids(),materials);materials=crystals(node.state().solids(),materials);island=island.plus(node.inventory().solids());}
        for(var transfer:graph.scheduledTransfers())if(transfer instanceof ScheduledTransfer.Injection in){materials=crystals(in.solidsPerSecond(),materials);island=island.plus(in.solidsPerSecond());}
        if(materials==null)return null;
        var crystals=model.crystalSolids();
        var melting=new TreeMap[graph.reservoirs().size()];boolean any=false;
        for(int i=0;i<melting.length;i++) {
            var node=graph.reservoirs().get(i);if(!node.junction()||node.fixed())continue;
            double t=node.state().temperature(),p=node.state().pressure();
            for(var material:materials.values()){var crystal=crystals.of(material);
                if(t>crystal.meltingTemperature(p)){if(melting[i]==null)melting[i]=new TreeMap<String,NetworkCrystals.Crystal>();melting[i].put(material.id(),crystal);any=true;}}
        }
        return any?new JunctionCrystals(melting,island):null;
    }
    /** {@code into} with the crystal materials of {@code stock} added (allocated on the first one; null while there is none). */
    private static TreeMap<String,SolidMaterial> crystals(SolidInventory stock,TreeMap<String,SolidMaterial> into) {
        for(var p:stock.populations())if(p.material().crystal()!=null){if(into==null)into=new TreeMap<>();into.putIfAbsent(p.material().id(),p.material());}
        return into;
    }
    /** Whether a crystal melts in {@code node}. */
    boolean any(int node){return melting[node]!=null;}
    /** The crystal of {@code material} if it melts in {@code node}, else null (an inert solid, a crystal below its point). */
    NetworkCrystals.Crystal melts(int node,SolidMaterial material){return melting[node]==null?null:melting[node].get(material.id());}

    /**
     * What melts in {@code node} of a solid stock whose population split is {@code list}'s (a Newton state's solids: its
     * aggregate moments over the seed's population list), per kilogram of the stock's solids: {@code mass[c]} kg of
     * component {@code c}, {@code energy} J of offsets, and the moments it takes away, {@code volume} m3 and
     * {@code capacity} J/K; {@code all} where every population melts (the stock leaves no solid).
     *
     * <p>A state whose seed held no solid names no population, yet its moments carry what reached it within the step: the
     * island's populations ({@link #island}), which the reconstruction books and melts. Read as inert, the Newton point
     * kept as a solid what the booking melted, and the equation gate refused the junction's solid and enthalpy rows by an
     * amount proportional to the step (cold fuzz seed 729: 0.035 to 0.1 kg/s at 1.9 % crystal from a 207 K junction into a
     * 244 K gas junction, about 1 kW of melting; 12 refused steps, each passed only after refinement).</p>
     */
    Split split(int node,SolidInventory list,int components) {
        if(!(list.massKg()>0))list=island;
        double total=list.massKg();var mass=new double[components];double energy=0,volume=0,capacity=0;boolean all=total>0;
        if(total>0)for(var p:list.populations()) {
            var crystal=melts(node,p.material());if(crystal==null){all=false;continue;}
            double f=p.massKg()/total;mass[crystal.component()]+=f;
            energy+=f*p.material().crystal().energyOffset();volume+=f/p.material().density();capacity+=f*p.material().heatCapacity();
        }
        return new Split(mass,energy,volume,capacity,all);
    }
    record Split(double[] mass,double energy,double volume,double capacity,boolean all) {
        /** The share of the stock's solid mass that melts. */
        double share(){double s=0;for(double m:mass)s+=m;return s;}
    }
    /** {@code stock} without what melts in {@code node}. */
    SolidInventory kept(int node,SolidInventory stock) {
        if(!any(node))return stock;
        var kept=new java.util.ArrayList<SolidInventory.Population>();
        for(var p:stock.populations())if(melts(node,p.material())==null)kept.add(p);
        return kept.size()==stock.populations().size()?stock:new SolidInventory(kept);
    }
}
