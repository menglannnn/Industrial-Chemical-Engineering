package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.material.SolidMaterialCatalog.SolidMaterial;
import com.wormzjl.createcheme.science.thermo.phase.CrystalPhaseEvaluator;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The crystals a network package admits, as solids (owner decisions D22 (5) and D23; the plan
 * {@code documentation/2026-09-24-coolprop-low-temperature/PRECIPITATED_SOLIDS_PLAN.md}): a crystal is a second kind of
 * population in the vessel's {@code SolidInventory}, beside the custom solids and under the same rules (key, transport,
 * filters, outlet checks, slurry viscosity), whose material record adds the fluid component it is made of and its energy
 * offset. Inside a step it is an inert population; only a vessel's refresh ({@code InventoryEquilibrium}) exchanges its
 * mass with its component, with the crystal's chemical potential ({@link Crystal#logFugacityCoefficient}, the anchored
 * model the equilibrium engine competes) equal to the component's.
 *
 * <p>Constant properties. A population's moments are main's: a constant density and heat capacity, and the stored energy
 * {@code c (T - 298.15)} per kg. The crystal's own model (Jaeger and Span for CO2-I, anchored to the fluid family) varies:
 * CO2-I's density from 1654 kg/m3 at 110 K to 1545 at its triple point, its heat capacity from 0.94 to 1.34 kJ/(kg K).
 * The record takes them where the pure crystal meets its own fluid at the network's ambient pressure
 * ({@link PhaseLayout#AMBIENT_PRESSURE}, the outside of every vessel, D54): the crystal's normal transition, for CO2-I its
 * normal sublimation point (194.6 K, 1571 kg/m3, 1.255 kJ/(kg K)), the state a slack tank of dry ice sits at. There the
 * crystal's energy is the model's exactly: {@code u0 = u_s(T_ref, P_amb) - c (T_ref - 298.15)} on the network's datum
 * (the spine's energy less the formation enthalpy, D10), so a refresh's latent heat is the model's at {@code T_ref} and
 * departs from it by the integral of the heat capacity's change elsewhere: for CO2-I 0.04 kJ/mol at its triple point
 * (0.5 % of the fusion enthalpy), 0.17 kJ/mol at 150 K and 0.59 kJ/mol at 110 K (0.7 and 2.3 % of the sublimation
 * enthalpy).</p>
 */
public final class NetworkCrystals {
    /** A package without crystals: the fluid-only network, unchanged. */
    static final NetworkCrystals NONE=new NetworkCrystals(List.of());
    private final List<Crystal> crystals;
    private NetworkCrystals(List<Crystal> crystals){this.crystals=List.copyOf(crystals);}

    /**
     * One admitted crystal: its record as a solid ({@link SolidMaterial#crystal()} set), the network basis index and
     * molar mass (kg/mol) of the component it is made of, the temperature its constants were taken at, and the anchored
     * model of the equilibrium engine.
     */
    public record Crystal(SolidMaterial material,int component,double molarMass,double referenceTemperature,
                          CrystalPhaseEvaluator.Anchored anchored) {
        public Crystal{Objects.requireNonNull(material);Objects.requireNonNull(anchored);}
        /** {@code ln phi_s = (g_s(T, P) - g_ig(T, P)) / (R T)}: the crystal's chemical potential against its species' ideal gas
         * (the package's spine), the reference the fluid's {@code ln (x phi P)} is stated on (continued past the record's range). */
        public double logFugacityCoefficient(double temperature,double pressure) {
            var gas=anchored.idealGas();
            double ideal=gas.enthalpy(temperature)-temperature*gas.entropy(temperature,pressure);
            return (anchored.chemicalPotential(temperature,pressure)-ideal)/(com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.GAS_CONSTANT*temperature);
        }
        /** Moles of the component in {@code massKg} of this crystal. */
        public double moles(double massKg){return massKg/molarMass;}
    }

    /** The admitted crystals, in declaration order; empty on a package without a crystal competition. */
    public List<Crystal> all(){return crystals;}
    public boolean isEmpty(){return crystals.isEmpty();}
    /** The crystal whose record is {@code material}, or null for a custom solid. */
    public Crystal of(SolidMaterial material) {
        if(material.crystal()==null)return null;
        for(var c:crystals)if(c.material().equals(material))return c;
        throw new IllegalArgumentException("Crystal "+material.id()+" ("+material.revision()+") is not a crystal of this package");
    }

    /** The crystals of {@code model}'s package as solids, from its engine's anchored crystals. */
    static NetworkCrystals of(FluidThermodynamics model,NetworkPhaseEngine engine,List<String> ids,String packageId) {
        if(ids.isEmpty())return NONE;
        var formation=model.hydrocarbon.spine().orElseThrow(()->new IllegalStateException("Crystals compete on a spine package only")).formationReference();
        var species=engine.species();var built=new ArrayList<Crystal>();
        for(String id:ids) {
            var anchored=engine.engine().anchoredCrystal(id);
            if(anchored==null)throw new IllegalStateException("The network's engine has no evaluator of crystal "+id);
            int component=model.hydrocarbon.components().indexOf(anchored.component());
            if(component<0)throw new IllegalStateException("Crystal "+id+" is made of "+anchored.component()+", which the network basis does not carry");
            double molarMass=model.molecularWeight(component);
            // The pure crystal's transition at the ambient pressure: its normal sublimation (or melting) point.
            double[] pure=new double[species.size()];pure[species.indexOf(anchored.component())]=1;
            var onset=engine.engine().depositionTemperature(id,PhaseLayout.AMBIENT_PRESSURE,species,pure,engine.engine().newWorkspace());
            if(onset.status()!=EquilibriumResult.Status.CONVERGED)
                throw new IllegalStateException("Crystal "+id+": no transition of the pure crystal at the ambient pressure ("+onset.detail()+")");
            double t=onset.temperature(),p=PhaseLayout.AMBIENT_PRESSURE;
            var state=anchored.properties(t,p);
            double internalEnergy=state.enthalpy()-formation.offsetJoulesPerMole(component)-p*state.volume();
            double density=molarMass/state.volume(),heatCapacity=state.heatCapacity()/molarMass;
            double offset=internalEnergy/molarMass-heatCapacity*(t-SolidMaterial.REFERENCE_TEMPERATURE);
            var material=new SolidMaterial(id,anchored.record().revision()+"@"+packageId,density,heatCapacity,new SolidMaterial.Crystal(anchored.component(),offset));
            built.add(new Crystal(material,component,molarMass,t,anchored));
        }
        return new NetworkCrystals(built);
    }
}
