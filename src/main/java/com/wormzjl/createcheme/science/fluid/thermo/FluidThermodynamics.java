package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.science.column.v3.thermo.V3WaterProperties;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.transport.MixtureViscosity;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.material.SolidMaterialCatalog;
import com.wormzjl.createcheme.science.thermo.NumericalBox;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import com.wormzjl.createcheme.science.thermo.TraceTruncationPolicy;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.createcheme.science.thermo.phase.FluidTpEquilibrium;
import com.wormzjl.createcheme.science.thermo.phase.PhaseContract;
import com.wormzjl.createcheme.science.thermo.phase.PhaseKind;
import com.wormzjl.createcheme.science.fluid.state.EnergyReference;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Immutable fluid property snapshot and standalone TP initializer; coupled steps consume phase() directly. Phase
 * decisions (the TP initializer, the outer active-set check, the inventory refresh) come from the equilibrium engine
 * through {@link #equilibriumTP}; residuals never flash.
 */
public final class FluidThermodynamics {
    public static final double R=8.31446261815324;
    public static final double DEFAULT_MAXIMUM_VELOCITY=100;
    /**
     * The network's ceiling on the trace cutoff, a thousandth of the column's. The column verifies a
     * truncated flash against an unmasked reference and declares explicit error budgets, so it may
     * ask for a percent; the network has no reference to answer to inside a Newton pass, and the
     * energy it can misplace by folding an omitted phase into the retained one is bounded by
     * {@code sum_omitted y_i n_V |h_i^V - h_i^L|}, i.e. by the cutoff times the phase amount times
     * the largest enthalpy of vaporization on the basis. At 1e-5 that is still inside the interval
     * controller's own energy gate; above it, it is not.
     */
    public static final double MAX_TRACE_CUTOFF_MOLE_FRACTION=1e-5;
    /** The gameplay default for a network model built without an explicit cutoff, and the default of
     * the {@code fluidTraceCutoffMoleFraction} config entry that supplies one: ten times inside the
     * ceiling. 0 is the exact off switch, the identical numerical path rather than a small cutoff. */
    public static final double DEFAULT_TRACE_CUTOFF_MOLE_FRACTION=1e-6;
    public final HydrocarbonModel hydrocarbon;
    public final com.wormzjl.createcheme.science.fluid.transport.SlurryTransport.Settings solidSettings;
    public final MixtureViscosity viscosity;
    public final SolidMaterialCatalog solids;
    public final double waterMolecularWeight;
    /** Resolved once from the catalog and package this model was built for, instead of through the
     * calculation context on every water evaluation: the context costs a {@code Context} allocation
     * and a {@code ThreadLocal} set/remove per call, and a coefficient read inside costs a
     * {@code ThreadLocal} lookup and two immutable-map probes. */
    private final MaterialCatalog.Water water;
    /** The free water's ideal steam, continued past the water record's range ({@link NetworkPhaseEngine.Steam}). */
    private final NetworkPhaseEngine.Steam steam;
    private final double waterEnthalpyOffset;
    private final double maximumVelocity;
    /** Carried on the model, like {@link #maximumVelocity}, because every retained solver, workspace
     * and factorization is keyed on the model: replacing it is what makes a changed cutoff reach a
     * running island deterministically, and {@code ApproximationAnchor.revision} carries it so a
     * saved fallback anchor built under a different unknown set cannot be reused. */
    private final TraceTruncationPolicy tracePolicy;

    /**
     * The canonical gameplay basis, which includes nitrogen. {@code packageId} is resolved through
     * {@link FluidMaterialCatalog#resolveNetworkPackage}, which accepts only the registered network package (any
     * other id is refused; there is no migration), and a package without nitrogen is refused
     * rather than silently run on a shorter basis. Direct construction retains an explicitly supplied
     * test/science basis.
     *
     * <p>No overload takes a single number after the package: the retired liquid-compressibility argument sat there,
     * and a {@code (catalog, package, maximumVelocity)} form would silently give an old call a velocity limit of
     * 1e-9 m/s. The velocity limit comes with the trace cutoff instead.</p>
     */
    public static FluidThermodynamics forNetwork(MaterialCatalog catalog,String packageId) {
        return forNetwork(catalog,packageId,DEFAULT_MAXIMUM_VELOCITY,DEFAULT_TRACE_CUTOFF_MOLE_FRACTION);
    }
    /** {@code traceCutoffMoleFraction} is the configured network cutoff: 0 is the exact off switch,
     * and the ceiling is {@link #MAX_TRACE_CUTOFF_MOLE_FRACTION}. */
    public static FluidThermodynamics forNetwork(MaterialCatalog catalog,String packageId,
                                                 double maximumVelocity,double traceCutoffMoleFraction) {
        return forNetwork(catalog,packageId,maximumVelocity,traceCutoffMoleFraction,com.wormzjl.createcheme.science.fluid.transport.SlurryTransport.Settings.defaults());
    }
    public static FluidThermodynamics forNetwork(MaterialCatalog catalog,String packageId,double maximumVelocity,double traceCutoffMoleFraction,com.wormzjl.createcheme.science.fluid.transport.SlurryTransport.Settings solidSettings) {
        TraceTruncationPolicy.requireCutoff(traceCutoffMoleFraction,MAX_TRACE_CUTOFF_MOLE_FRACTION);
        return new FluidThermodynamics(catalog,FluidMaterialCatalog.resolveNetworkPackage(catalog,packageId),
                maximumVelocity,TraceTruncationPolicy.of(traceCutoffMoleFraction),solidSettings);
    }

    /** Placement initializer only. Persistence must restore inventory instead of calling this again. */
    public State initialNitrogenCharge(double volume,double temperature,double pressure,Runnable checkpoint) {
        if(!Double.isFinite(volume)||volume<=0)throw new IllegalArgumentException("Positive finite vessel volume required");
        int nitrogen=hydrocarbon.components().indexOf(FluidMaterialCatalog.NITROGEN);if(nitrogen<0)throw new IllegalStateException("Network basis has no nitrogen");
        double[] n=new double[hydrocarbon.componentCount()+1];n[nitrogen]=1;
        var unit=flashTP(temperature,pressure,n,checkpoint);n[nitrogen]=volume/unit.volume();
        return flashTP(temperature,pressure,n,checkpoint);
    }

    public FluidThermodynamics(MaterialCatalog catalog,String packageId,double maximumVelocity,TraceTruncationPolicy tracePolicy,com.wormzjl.createcheme.science.fluid.transport.SlurryTransport.Settings solidSettings) {
        this.solidSettings=Objects.requireNonNull(solidSettings);
        if(!Double.isFinite(maximumVelocity)||maximumVelocity<=0)throw new IllegalArgumentException("Positive finite maximum velocity required");
        TraceTruncationPolicy.requireCutoff(Objects.requireNonNull(tracePolicy,"tracePolicy").cutoffMoleFraction(),MAX_TRACE_CUTOFF_MOLE_FRACTION);
        this.tracePolicy=tracePolicy;
        this.maximumVelocity=maximumVelocity;
        hydrocarbon=new HydrocarbonModel(catalog,packageId);
        viscosity=new MixtureViscosity(catalog,packageId);solids=catalog.solids();
        waterMolecularWeight=catalog.requirePackage(packageId).water().molarMass();
        // The same resolution the context form performs, including its fallback package.
        water=com.wormzjl.createcheme.science.material.MaterialRuntime.with(catalog,packageId,
                com.wormzjl.createcheme.science.material.MaterialRuntime::water);
        steam=new NetworkPhaseEngine.Steam(water);
        domain=hydrocarbon.domain();
        // The crystal competition: the package's admitted crystals, on a spine package only (a crystal is anchored to the
        // spine family's liquid, and its enthalpy is moved to the network datum by the spine's formation enthalpies). None
        // on every bundled network package: the fluid-only path is unchanged.
        var admitted=hydrocarbon.spine().isPresent()?PhaseContract.admittedCrystals(catalog,packageId):List.<String>of();
        crystalIds=List.copyOf(admitted);crystalSpecies=new boolean[hydrocarbon.componentCount()];
        double ceiling=Double.POSITIVE_INFINITY;
        for(String crystal:admitted) {
            var record=catalog.crystals().get(crystal);int i=hydrocarbon.components().indexOf(record.component());
            if(i<0)throw new IllegalArgumentException("Crystal "+crystal+" of package "+packageId+" is made of "+record.component()+", which the network basis does not carry");
            crystalSpecies[i]=true;ceiling=Math.min(ceiling,record.maximumTemperature());
        }
        crystalCeiling=ceiling;
        // Region 1 at 298.15 K and 1 atm itself, the stable liquid: the network's water datum and the pump's density.
        var reference=WaterRegion1.evaluate(water,298.15,101325);
        waterEnthalpyOffset=V3WaterProperties.liquidMolarEnthalpy(water,298.15)-reference.specificEnthalpy()*waterMolecularWeight;
        pumpReferenceDensity=waterMolecularWeight/(reference.specificVolume()*waterMolecularWeight);
        this.catalog=catalog;
    }
    /** The catalog this model was built from; the phase engine is built from it on first use. */
    private final MaterialCatalog catalog;
    /** The equilibrium engine that decides this model's phases; built on the first phase decision. */
    private volatile NetworkPhaseEngine phaseEngine;
    NetworkPhaseEngine phaseEngine() {
        var engine=phaseEngine;
        if(engine==null)synchronized(this) {
            engine=phaseEngine;
            if(engine==null)phaseEngine=engine=NetworkPhaseEngine.of(this,catalog,hydrocarbon.propertyPackage().id(),water,FluidTpEquilibrium.Settings.DEFAULT);
        }
        return engine;
    }
    /** The package's crystals as solids ({@link NetworkCrystals}); built with the phase engine on first use, empty (and no
     * engine built) on a package without a crystal competition. */
    private volatile NetworkCrystals crystalSolids;
    public NetworkCrystals crystalSolids() {
        if(crystalIds.isEmpty())return NetworkCrystals.NONE;
        var crystals=crystalSolids;
        if(crystals==null)synchronized(this) {
            crystals=crystalSolids;
            if(crystals==null)crystalSolids=crystals=NetworkCrystals.of(this,phaseEngine(),crystalIds,hydrocarbon.propertyPackage().id());
        }
        return crystals;
    }
    /**
     * The record a solid population's material id names (owner decision D23: crystals are solids): a crystal this package
     * admits ({@link #crystalSolids}), else a custom solid of the catalog ({@link #solids}); the catalog keeps the two kinds'
     * ids apart, so an id names one record. An id that names neither is refused: nothing read back (a checkpoint's
     * population, a pending recovery) can carry a solid this package does not define. Builds the crystals (and the
     * phase engine) only for an id among the package's crystals.
     */
    public SolidMaterialCatalog.SolidMaterial solid(String id) {
        if(crystalIds.contains(id))for(var crystal:crystalSolids().all())if(crystal.material().id().equals(id))return crystal.material();
        var custom=solids.materials().get(id);
        if(custom==null)throw new IllegalArgumentException("Unknown solid material: "+id+" names no crystal of package "+domain.packageId()+" and no solid material record");
        return custom;
    }
    /**
     * The crystal answer of a vessel's refresh (owner decision D23): the equilibrium engine's answer for the vessel's fluid
     * and crystals together, the package's crystals competing, as the network states it. {@code temperature} and
     * {@code pressure} are the answer's; {@code fluid} its fluid phases as a network state without solids (null where the
     * crystals hold everything); {@code crystalMoles} the moles each of the package's crystals holds
     * ({@link NetworkCrystals#all} order, all zero where no crystal is stable); {@code volume} the fluid's and the
     * crystals' volume, the crystals in the engine's own model.
     */
    public record CrystalAnswer(double temperature,double pressure,State fluid,double[] crystalMoles,double volume) {
        public CrystalAnswer{crystalMoles=crystalMoles.clone();}
        @Override public double[] crystalMoles(){return crystalMoles.clone();}
        /** Whether a crystal is stable in this answer. */
        public boolean holdsCrystal(){for(double n:crystalMoles)if(n>0)return true;return false;}
    }
    /**
     * The crystal test of a taut vessel (D23, with D21's near-pure step): the equilibrium of {@code totals} (the conserved
     * basis, water last; dry) with the crystals competing at internal energy {@code energy} (J, the network's datum: the
     * fluid's and the crystals' true energy, every inert solid's excluded) and volume {@code volume} (m3, the bag less the
     * inert solids), warm-started at {@code (hintT, hintP)} when both are finite. {@code checkpoint} runs as for
     * {@link #equilibriumTP}. Refusals are typed where the engine types them: a phase set the engine does not implement or
     * a crystal step it does not answer the {@link UnsupportedPhases} of kind {@code CRYSTAL_PHASES} or
     * {@code CRYSTAL_STEP}, the critical band's {@link CriticalBandHold}; any other failure is a plain
     * {@link IllegalArgumentException}.
     */
    public CrystalAnswer crystalsAtVolume(double[] totals,double energy,double volume,double hintT,double hintP,Runnable checkpoint) {
        checkpoint.run();
        var result=phaseEngine().crystalUv(engineEnergy(energy,totals),volume,totals,hintT,hintP);
        for(int evaluation=result.diagnostics().kernelEvaluations();evaluation>0;evaluation--)checkpoint.run();
        return crystalAnswer(result,totals);
    }
    /** {@link #crystalsAtVolume} for a slack vessel: the equilibrium at {@code pressure} (the ambient) with enthalpy
     * {@code enthalpy} (J, the network's datum, the fluid's and the crystals' only). */
    public CrystalAnswer crystalsAtPressure(double[] totals,double enthalpy,double pressure,Runnable checkpoint) {
        checkpoint.run();
        var result=phaseEngine().ph(pressure,engineEnergy(enthalpy,totals),totals,true);
        for(int evaluation=result.diagnostics().kernelEvaluations();evaluation>0;evaluation--)checkpoint.run();
        return crystalAnswer(result,totals);
    }
    /** A network-datum energy of {@code totals} on the engine's datum (D10): the formation enthalpies added per species. */
    private double engineEnergy(double energy,double[] totals) {
        var spine=hydrocarbon.spine().orElseThrow(()->new IllegalStateException("Crystals compete on a spine package only"));
        var formation=spine.formationReference();
        return EnergyReference.sensible(formation.components()).rebase(energy,Arrays.copyOf(totals,hydrocarbon.componentCount()),formation);
    }
    private CrystalAnswer crystalAnswer(EquilibriumResult result,double[] totals) {
        var crystals=crystalSolids();
        switch(result.status()) {
            case CONVERGED->{}
            case UNSUPPORTED->{
                if(result.detail().startsWith(EquilibriumResult.CRYSTAL_PHASES))throw new UnsupportedPhases(domain.packageId(),Double.NaN,Double.NaN,
                        UnsupportedPhases.Kind.CRYSTAL_PHASES,"a crystal beside a phase set the engine does not implement ("+result.detail()+")");
                throw new IllegalStateException("The network's equilibrium engine refused a crystal request it must answer: "+result.detail());
            }
            case NOT_CONVERGED->{
                if(result.detail().startsWith(EquilibriumResult.CRYSTAL_STEP))throw new UnsupportedPhases(domain.packageId(),Double.NaN,Double.NaN,
                        UnsupportedPhases.Kind.CRYSTAL_STEP,"a near-pure feed's crystal step the pure species' step does not answer ("+result.detail()+")");
                if(result.diagnostics().criticalBand())throw new CriticalBandHold(domain.packageId(),Double.NaN,Double.NaN,result.detail());
                throw new IllegalArgumentException("Crystal equilibrium did not converge: "+result.detail());
            }
        }
        var first=result.phases().getFirst();double t=first.temperature(),p=first.pressure();
        if(result.classification()==EquilibriumResult.Classification.LIQUID_LIQUID)throw new UnsupportedPhases(domain.packageId(),t,p,UnsupportedPhases.Kind.LIQUID_LIQUID,
                "a liquid-liquid split (two liquid-like hydrocarbon phases); the fluid network carries one hydrocarbon liquid");
        var all=crystals.all();double[] moles=new double[all.size()];double volume=0;
        int n=hydrocarbon.componentCount();double[] liquid=new double[n],vapor=new double[n];boolean fluid=false,liquidSlot=false,vaporSlot=false;
        var terms=hydrocarbon.prepare(t);
        for(var phase:result.phases()) {
            if(phase.kind()==PhaseKind.SOLID) {
                int k=-1;for(int i=0;i<all.size();i++)if(all.get(i).material().id().equals(phase.crystal()))k=i;
                if(k<0)throw new IllegalStateException("The engine answered crystal "+phase.crystal()+", which the network does not carry");
                moles[k]+=phase.amount(all.get(k).component());volume+=phase.total()*phase.state().molarVolume();continue;
            }
            boolean toLiquid=switch(phase.kind()) {
                case LIQUID->true;
                case VAPOR->false;
                default->HydrocarbonModel.liquidLike(hydrocarbon.translated().evaluateValues(t,p,Arrays.copyOf(phase.amountsView(),n),phase.state().root(),terms));
            };
            if(toLiquid?liquidSlot:vaporSlot)throw new UnsupportedPhases(domain.packageId(),t,p,UnsupportedPhases.Kind.CRYSTAL_PHASES,"two fluid phases of one slot beside a crystal");
            if(toLiquid)liquidSlot=true;else vaporSlot=true;
            System.arraycopy(phase.amountsView(),0,toLiquid?liquid:vapor,0,n);fluid=true;
        }
        State state=fluid?state(t,p,liquid,vapor,0,0,p,terms,null):null;
        return new CrystalAnswer(t,p,state,moles,volume+(state==null?0:state.volume()));
    }
    /** The package's data ranges: its envelope, the input box of a device setting ({@link FluidDomain}). */
    public FluidDomain domain(){return domain;}
    /**
     * The data ranges the state at network node {@code node} lies outside, into {@code notices} (owner rule of
     * 2026-10-16: every model continues past its data and the island's status line reports it, RANGE_EXTRAPOLATION_PLAN
     * section 5): the package envelope; each carried component's ideal-gas data and the viscosity data of each phase it
     * is in; free liquid water's Region 1 range (its viscosity's too) and the pressure its metastable liquid has data to;
     * the steam's range and qualified partial pressure; and each crystal's record. Reads the state and the declared ranges
     * only: no model is evaluated but the water's saturation pressure.
     */
    public void dataRanges(State state,long node,DataRangeNotices notices) {
        double t=state.temperature(),p=state.pressure();
        var envelope=domain.envelope();
        notices.check("package envelope temperature",t,envelope.minimumTemperature(),envelope.maximumTemperature(),"K",node);
        notices.check("package envelope pressure",p,envelope.minimumPressure(),envelope.maximumPressure(),"Pa",node);
        var eos=hydrocarbon.translated();var names=hydrocarbon.components();
        double[] liquid=state.liquidView(),vapor=state.vaporView();
        for(int i=0;i<liquid.length;i++) {
            if(!(liquid[i]>0)&&!(vapor[i]>0))continue;
            String name=names.get(i);
            notices.check(name+" ideal-gas heat capacity",t,eos.idealGasMinimumTemperature(i),eos.idealGasMaximumTemperature(i),"K",node);
            var liquidData=liquid[i]>0?viscosity.liquidData(i,t):null;
            if(liquidData!=null)notices.check(name+" liquid viscosity",t,liquidData.minimumTemperatureKelvin(),liquidData.maximumTemperatureKelvin(),"K",node);
            var vaporData=vapor[i]>0?viscosity.vaporData(i):null;
            if(vaporData!=null)notices.check(name+" vapour viscosity",t,vaporData.minimumTemperatureKelvin(),vaporData.maximumTemperatureKelvin(),"K",node);
        }
        if(state.waterLiquid()>0) {
            notices.check("liquid water",t,water.triplePoint(),WaterRegion1.REGION_1_MAXIMUM_TEMPERATURE,"K",node);
            if(t>=water.triplePoint()&&t<=WaterRegion1.REGION_1_MAXIMUM_TEMPERATURE)notices.check("liquid water pressure",p,
                    saturationPressure(t)-WaterRegion1.METASTABLE_MARGIN_PASCAL,WaterRegion1.REGION_1_MAXIMUM_PRESSURE,"Pa",node);
        }
        if(state.waterVapor()>0) {
            notices.check("water vapour",t,steam.minimumTemperature(),steam.maximumTemperature(),"K",node);
            notices.check("water vapour partial pressure",state.waterPartialPressure(),0,waterVaporPressureLimit(t),"Pa",node);
        }
        for(var population:state.solids().populations()) {
            if(population.material().crystal()==null)continue;
            var record=crystalSolids().of(population.material()).anchored().record();
            notices.check(record.component()+" solid",t,record.minimumTemperature(),record.maximumTemperature(),"K",node);
            notices.check(record.component()+" solid pressure",p,record.minimumPressure(),record.maximumPressure(),"Pa",node);
        }
    }
    private final FluidDomain domain;
    /** The crystals the package admits, in declaration order; empty without a crystal competition. */
    private final List<String> crystalIds;
    /** Whether a hydrocarbon component is the species of an admitted crystal. */
    private final boolean[] crystalSpecies;
    /** The lowest ceiling of the admitted crystals' records: above it no crystal competes (the fluid-only domain applies). */
    private final double crystalCeiling;
    /**
     * The density a pump's "maximum pressure rise" is stated for: liquid water at 298.15 K and 101,325 Pa, from this
     * model's own water (IF97 Region 1 at that state), 997.05 kg/m3 (NIST: 997.0). A
     * pump's rise limit on another fluid is the setting scaled by the density of
     * what it withdraws over this one: a head, so a pump that pushes water 500 kPa pushes nitrogen at 1 atm about
     * 575 Pa. See {@code PassiveStepSolver.riseLimit}.
     */
    public double pumpReferenceDensity(){return pumpReferenceDensity;}
    private final double pumpReferenceDensity;
    /** Conserved component basis: registered hydrocarbon/gas components, followed by water. */
    public int componentCount() { return hydrocarbon.componentCount()+1; }

    /** Immutable conserved-component names, including water in the final slot. */
    public java.util.List<String> components() {
        var components=new java.util.ArrayList<>(hydrocarbon.components());
        components.add("Water");
        return java.util.List.copyOf(components);
    }

    /** Molecular weight in kg/mol for an index in the complete conserved basis. */
    public double molecularWeight(int component) {
        return component==hydrocarbon.componentCount()?waterMolecularWeight:hydrocarbon.molecularWeight(component);
    }

    /** Independently owned molecular-weight array in conserved-component order. */
    public double[] molecularWeights() {
        double[] weights=new double[componentCount()];
        for(int c=0;c<weights.length;c++)weights[c]=molecularWeight(c);
        return weights;
    }
    /**
     * The free water's saturation pressure: the Wagner-Pruss equation, below the water model's triple point the
     * supercooled liquid's ({@link V3WaterProperties#wagnerPruss}), infinite from the critical temperature on (water there
     * is steam).
     */
    public double saturationPressure(double t) {
        if(!Double.isFinite(t))throw new IllegalArgumentException("Non-finite water temperature");
        return t>=water.criticalTemperature()?Double.POSITIVE_INFINITY:V3WaterProperties.wagnerPruss(water,t);
    }
    /** The saturation pressure of a prepared node temperature, evaluated once for that temperature. */
    public double saturationPressure(Prepared prepared) { return own(prepared).saturationPressure(); }
    /** The molar enthalpy of the free water's ideal steam, J/mol ({@link NetworkPhaseEngine.Steam}). */
    public double vaporWaterEnthalpy(double t) {
        if(!Double.isFinite(t))throw new IllegalArgumentException("Non-finite water temperature");
        return steam.enthalpy(t);
    }
    /** The free water's ideal steam: the phase engine's water vapour and the notices' data range. */
    NetworkPhaseEngine.Steam steam(){return steam;}
    public double maximumVelocityMetresPerSecond(){return maximumVelocity;}
    /** The per-node phase support the step solver freezes its Newton unknowns with. */
    public TraceTruncationPolicy traceTruncation(){return tracePolicy;}
    /** A hydraulic rate bound only; temperature and phase allocation still come from the EOS/energy equations. */
    public double velocityLimit(State state){return Math.min(maximumVelocity,isothermalAcousticBound(state));}
    /**
     * Frozen-phase isothermal acoustic bound used by the configured hydraulic velocity clamp: the node's compliance is
     * {@code sum_phases V_phase kappa_phase}, the hydrocarbon liquid's and the free water's own isothermal
     * compressibility at the state (the equation of state's and Region 1's), and the gas's.
     */
    public double isothermalAcousticBound(State state) {
        double compliance=state.waterVolume()*state.waterCompressibility();
        if(state.liquidVolume()>0)compliance+=state.liquidVolume()*state.liquidProperties().isothermalCompressibility();
        if(state.vaporVolume()>0) {
            double stiffness=state.waterPartialPressure();
            if(state.vaporProperties()!=null){var gas=state.vaporProperties();stiffness+=-gas.molarVolume()/gas.volumePressureDerivative();}
            compliance+=state.vaporVolume()/stiffness;
        }
        double speed=state.volume()/Math.sqrt(state.mass()*compliance);
        if(!Double.isFinite(speed)||speed<=0)throw new IllegalArgumentException("Invalid acoustic flow-domain bound");
        return speed;
    }
    /**
     * The fluid a phase-selective vessel outlet draws, read off the vessel's own state with no flash and no extra
     * equation-of-state root: every term below is a sum over the phase amounts, volumes and molar properties
     * {@link #state} already computed (plan table 3.2 of documentation/2026-09-26-phase-ports-and-compressor).
     * <ul>
     * <li>The vapour stream is the hydrocarbon vapour and the water vapour: {@code moles} are {@code vapor} with the
     * water vapour in the last slot, its volume is the gas volume, its enthalpy {@code n_v h_v + n_wv h_wv(T)}, and it
     * carries no solid.</li>
     * <li>The condensed phase is the hydrocarbon liquid, the free water and every solid; a port draws its two liquids
     * one after the other (decision D11), so each liquid is its own stream ({@link #phaseStream}): {@code moles} are
     * {@code liquid} with a zero water slot (OIL) or only the free water in the last slot (WATER), its volume the
     * liquid's volume plus its volume share of the solids, its enthalpy {@code n_l h_l} or {@code n_wl h_wl(T,P)} plus
     * that share of {@code H_solids(T,P)}. {@link #liquidMass}, {@link #liquidStreamVolume} and {@link #liquidDensity}
     * state the whole condensed phase (the level head's mass and a level's volume).</li>
     * </ul>
     * The streams partition the state: their moles, masses, volumes and enthalpies add up to the state's own. The
     * velocity limit is {@link #isothermalAcousticBound}'s formula restricted to the stream's phases,
     * {@code V_s / sqrt(M_s * compliance_s)}, i.e. {@code sqrt(stiffness/rho_v)} for the vapour and
     * {@code sqrt(1/(kappa rho))} (solids incompressible) for a liquid, capped by the configured maximum as
     * {@link #velocityLimit} caps the bulk: a vapour drawn off a wet tank is limited by the vapour's own acoustic bound,
     * not by the far lower bound of the two-phase mixture. The viscosity is the stream's own terms of the volume
     * average the step solver states for the bulk (the slurry correction over the liquid's share of the solids).
     *
     * <p>A stream whose phase the state does not hold is not built: the builders return {@code null}, and what an
     * absent stream means is the reader's rule (the step solver's: a port draws the next phase of its priority order,
     * decision D11).
     */
    public record PhaseStream(double[] moles,double mass,double volume,double viscosity,double specificEnthalpy,double velocityLimit,
                              SolidInventory.Moments solidMoments) {
        public PhaseStream {Objects.requireNonNull(moles);Objects.requireNonNull(solidMoments);}
        /** The moles themselves, for the solver packages. The caller must not mutate them. */
        public double[] molesView(){return moles;}
        @Override public double[] moles(){return moles.clone();}
    }
    /** Whether the state holds a gas phase to draw. */
    public static boolean holdsVapor(State state){return state.vaporVolume()>0;}
    /** Whether the state holds a hydrocarbon liquid or free water to draw (solids alone do not flow as a liquid). */
    public static boolean holdsLiquid(State state){return state.liquidVolume()+state.waterVolume()>0;}
    /** Mass of the vapour stream: hydrocarbon vapour then water vapour, summed in the order {@link #state} sums the whole. */
    public double vaporMass(State state) {
        double mass=0;var v=state.vaporView();
        for(int i=0;i<v.length;i++)mass+=v[i]*hydrocarbon.molecularWeight(i);
        return mass+state.waterVapor()*waterMolecularWeight;
    }
    /** Mass of the condensed phase: hydrocarbon liquid, free water, then every solid (the level head's mass). */
    public double liquidMass(State state) {
        double mass=0;var l=state.liquidView();
        for(int i=0;i<l.length;i++)mass+=l[i]*hydrocarbon.molecularWeight(i);
        return mass+state.waterLiquid()*waterMolecularWeight+state.solidMoments().mass();
    }
    /** Volume of the condensed phase: the two liquid volumes and the solid volume. */
    public static double liquidStreamVolume(State state){return state.liquidVolume()+state.waterVolume()+state.solidMoments().volume();}
    /** The vapour stream's density, for a state that {@link #holdsVapor}. */
    public double vaporDensity(State state){return vaporMass(state)/state.vaporVolume();}
    /** The condensed phase's density (its mass over its volume), for a state that {@link #holdsLiquid}. */
    public double liquidDensity(State state){return liquidMass(state)/liquidStreamVolume(state);}
    /** The vapour stream's specific enthalpy (J/kg); {@code prepared} as for {@link #state}, or null. */
    public double vaporSpecificEnthalpy(State state,Prepared prepared) {
        double h=0;
        if(state.vaporProperties()!=null)h+=total(state.vaporView())*state.vaporProperties().molarEnthalpy();
        if(state.waterVapor()>0)h+=state.waterVapor()*(prepared==null?vaporWaterEnthalpy(state.temperature()):match(prepared,state.temperature()).vaporEnthalpy());
        return h/vaporMass(state);
    }
    /** The vapour stream's velocity limit: {@code min(maximum, V_v/sqrt(M_v V_v/stiffness))}. */
    public double vaporVelocityLimit(State state) {
        double stiffness=state.waterPartialPressure();
        if(state.vaporProperties()!=null){var gas=state.vaporProperties();stiffness+=-gas.molarVolume()/gas.volumePressureDerivative();}
        return Math.min(maximumVelocity,acoustic(state.vaporVolume(),vaporMass(state),state.vaporVolume()/stiffness));
    }
    /**
     * {@code V / sqrt(M C)}. The product of a phase's mass and compliance goes subnormal for a trace phase (a vanishing
     * nitrogen vapour below about 2e-150 mol, whose mass and compliance are each of order its amount): it then keeps too
     * few bits, and below about 2e-158 mol it is zero and the bound infinite. Such a product is evaluated as
     * {@code sqrt(V/M) sqrt(V/C)}, ratios of like-sized quantities, which is the same number to rounding; every product
     * at or above {@link Double#MIN_NORMAL} takes the original expression, bit for bit (phase-ports in-game defect F2,
     * tools/phase-ports-probes/holds/).
     */
    private static double acoustic(double volume,double mass,double compliance) {
        double product=mass*compliance;
        double speed=product>=Double.MIN_NORMAL?volume/Math.sqrt(product):Math.sqrt(volume/mass)*Math.sqrt(volume/compliance);
        if(!Double.isFinite(speed)||speed<=0)throw new IllegalArgumentException("Invalid acoustic flow-domain bound");
        return speed;
    }
    /** The vapour stream's viscosity: the vapour term of the solver's volume average, over the gas volume. */
    public double vaporViscosity(State state,MixtureViscosity.Workspace scratch,Prepared prepared) {
        var terms=prepared==null?null:match(prepared,state.temperature()).viscosities();
        return viscosity.vapor(state.temperature(),state.vaporView(),state.waterVapor(),scratch,terms);
    }
    /** The vapour stream of {@code state}, or null when it holds no gas. */
    public PhaseStream vaporStream(State state,Prepared prepared,MixtureViscosity.Workspace scratch) {
        if(!holdsVapor(state))return null;
        var v=state.vaporView();double[] n=Arrays.copyOf(v,v.length+1);n[v.length]=state.waterVapor();
        return new PhaseStream(n,vaporMass(state),state.vaporVolume(),vaporViscosity(state,scratch,prepared),
                vaporSpecificEnthalpy(state,prepared),vaporVelocityLimit(state),SolidInventory.Moments.ZERO);
    }
    /*
     * The three phases a vessel port can draw one after another (decision D11 of
     * documentation/2026-09-26-phase-ports-and-compressor: ports carry mixed phases by priority). GAS is the vapour stream
     * above. The condensed phase splits into the hydrocarbon liquid (OIL) and the free water (WATER), each carrying its
     * volume share of every solid population: the solids are held as a uniform suspension in the combined liquid, the
     * carrier the solid mobility check already reads (volume-averaged density and viscosity of both liquids), so a phase
     * of the liquid draws its share of them. The OIL and WATER streams partition the condensed phase (moles, mass,
     * volume, enthalpy and solids add up to the condensed phase's), and with one liquid present its stream carries every
     * solid (a share of exactly 1.0). All read the state's own amounts and molar properties: no flash.
     */
    public static final int GAS=0,OIL=1,WATER=2;
    /** Whether {@code state} holds {@code phase} ({@link #GAS}, {@link #OIL} or {@link #WATER}) to draw: a positive
     * volume of it. */
    public static boolean holdsPhase(State state,int phase) {
        return switch(phase){case GAS->state.vaporVolume()>0;case OIL->state.liquidVolume()>0;case WATER->state.waterVolume()>0;default->throw new IllegalArgumentException("phase "+phase);};
    }
    /**
     * Owner decision D33 (2026-10-16): solids ride the liquid. A connection's BULK end draws a uniform share of what its
     * donor holds, the solids carried by the liquid at {@code solids * liquid drawn / liquid held}: wherever a liquid is
     * held that is the bulk share, the number it always was. Where none is held nothing carries them: the gas leaves
     * through the end and the solids stay (frost on a tank's walls, a dry bed), zero solid from a dry gas. So the end draws
     * the donor's gas exactly where that differs from its bulk: a gas beside solids, with no liquid (a stand-in liquid,
     * D29 A, is a liquid here, as it is booked). The solids are read as the populations the state carries, which a step's
     * iterates keep from their pass's seed, so the answer is fixed for a pass. The one reading of a bulk end: the stage
     * guard's outlet (the GAS outlet, {@code SolidEventIntegrator}), the static end reads ({@code PhaseDraw.leading}),
     * and the step's donor terms and the reconstruction's booking ({@code PassiveStepSolver.Equations}).
     */
    public static boolean bulkDrawsGas(State state) {
        return !(state.liquidVolume()>0)&&!(state.waterVolume()>0)&&state.vaporVolume()>0&&!state.solids().empty();
    }
    /** The share of the state's solids a phase stream carries: none in the gas, the liquids' in proportion to their
     * volumes ({@code 1.0} exactly for the only liquid held). */
    public static double phaseSolidShare(State state,int phase) {
        if(phase==GAS)return 0;
        double oil=state.liquidVolume(),water=state.waterVolume();
        return phase==OIL?oil/(oil+water):water/(oil+water);
    }
    /** The own density (no solids) of a liquid phase the state holds: its fluid mass over its volume. */
    public double liquidPhaseDensity(State state,int phase) {
        if(phase==WATER)return state.waterLiquid()*waterMolecularWeight/state.waterVolume();
        double mass=0;var l=state.liquidView();for(int i=0;i<l.length;i++)mass+=l[i]*hydrocarbon.molecularWeight(i);
        return mass/state.liquidVolume();
    }
    /** The heavier of the state's two liquids by their own densities: {@link #OIL} only when both are held and the
     * hydrocarbon liquid is the denser, {@link #WATER} otherwise (which one is named when at most one is held does not
     * matter to a priority order: an absent phase is skipped). */
    public int heavierLiquid(State state) {
        return state.liquidVolume()>0&&state.waterVolume()>0&&liquidPhaseDensity(state,OIL)>liquidPhaseDensity(state,WATER)?OIL:WATER;
    }
    /** Mass of a phase stream, its solid share included; {@link #vaporMass} for the gas. */
    public double phaseMass(State state,int phase) {
        if(phase==GAS)return vaporMass(state);
        double mass=0;
        if(phase==OIL){var l=state.liquidView();for(int i=0;i<l.length;i++)mass+=l[i]*hydrocarbon.molecularWeight(i);}
        else mass=state.waterLiquid()*waterMolecularWeight;
        return mass+phaseSolidShare(state,phase)*state.solidMoments().mass();
    }
    /** Volume of a phase stream: the gas volume, or the liquid's volume plus its share of the solid volume. */
    public static double phaseVolume(State state,int phase) {
        if(phase==GAS)return state.vaporVolume();
        return (phase==OIL?state.liquidVolume():state.waterVolume())+phaseSolidShare(state,phase)*state.solidMoments().volume();
    }
    /** The conserved amounts (water last) of a phase stream. */
    public static double[] phaseMoles(State state,int phase) {
        var view=phase==GAS?state.vaporView():state.liquidView();double[] n=new double[view.length+1];
        switch(phase) {
            case GAS->{System.arraycopy(view,0,n,0,view.length);n[view.length]=state.waterVapor();}
            case OIL->System.arraycopy(view,0,n,0,view.length);
            default->n[view.length]=state.waterLiquid();
        }
        return n;
    }
    /** The solid moments a phase stream carries: its share of the state's. */
    public static SolidInventory.Moments phaseSolidMoments(State state,int phase) {
        double share=phaseSolidShare(state,phase);if(share==0)return SolidInventory.Moments.ZERO;
        var m=state.solidMoments();return new SolidInventory.Moments(share*m.mass(),share*m.volume(),share*m.heatCapacity());
    }
    /** A phase stream's specific enthalpy (J/kg), its solid share included; {@code prepared} as for {@link #state}. */
    public double phaseSpecificEnthalpy(State state,int phase,Prepared prepared) {
        if(phase==GAS)return vaporSpecificEnthalpy(state,prepared);
        double h=0,t=state.temperature(),p=state.pressure();
        if(phase==OIL){if(state.liquidProperties()!=null)h+=total(state.liquidView())*state.liquidProperties().molarEnthalpy();}
        else if(state.waterLiquid()>0)h+=state.waterLiquid()*waterLiquid(t,p,prepared).molarEnthalpy;
        h+=phaseSolidShare(state,phase)*state.solidMoments().enthalpy(t,p);
        return h/phaseMass(state,phase);
    }
    /** A phase stream's velocity limit: {@link #vaporVelocityLimit} for the gas, {@code min(maximum, V/sqrt(M V_liquid
     * kappa))} for a liquid with its solids (incompressible), {@code kappa} the liquid's own isothermal compressibility at
     * the state (the equation of state's for the hydrocarbon liquid, Region 1's for the free water). */
    public double phaseVelocityLimit(State state,int phase) {
        if(phase==GAS)return vaporVelocityLimit(state);
        double compliance=phase==WATER?state.waterVolume()*state.waterCompressibility()
                :state.liquidVolume()>0?state.liquidVolume()*state.liquidProperties().isothermalCompressibility():0;
        return Math.min(maximumVelocity,acoustic(phaseVolume(state,phase),phaseMass(state,phase),compliance));
    }
    /** A phase stream's viscosity: {@link #vaporViscosity} for the gas; a liquid's own viscosity with the slurry
     * correction for its share of the solids, over the stream's volume. */
    public double phaseViscosity(State state,int phase,MixtureViscosity.Workspace scratch,Prepared prepared) {
        if(phase==GAS)return vaporViscosity(state,scratch,prepared);
        var terms=prepared==null?null:match(prepared,state.temperature()).viscosities();
        double t=state.temperature(),liquid,value;
        if(phase==OIL){liquid=state.liquidVolume();value=liquid*viscosity.liquid(t,state.liquidView(),terms).pascalSeconds();}
        else{liquid=state.waterVolume();value=liquid*viscosity.waterLiquid(t,terms);}
        double solid=phaseSolidShare(state,phase)*state.solidMoments().volume();
        if(solid>0){double phi=solid/(liquid+solid);value=com.wormzjl.createcheme.science.fluid.transport.SlurryTransport.effectiveViscosity(value/liquid,Math.min(phi,0.62-1e-9))*(liquid+solid);}
        return value/(liquid+solid);
    }
    /** The carrier viscosity of a phase stream (no slurry correction), which an inline filter's cake resistance scales
     * with: the gas's own, or the liquid's own. */
    public double phaseCarrierViscosity(State state,int phase,MixtureViscosity.Workspace scratch) {
        if(phase==GAS)return vaporViscosity(state,scratch,null);
        return phase==OIL?viscosity.liquid(state.temperature(),state.liquidView()).pascalSeconds():viscosity.waterLiquid(state.temperature());
    }
    /** The stream of one phase of {@code state}, or null when it does not hold that phase; {@link #vaporStream} for the
     * gas. */
    public PhaseStream phaseStream(State state,int phase,Prepared prepared,MixtureViscosity.Workspace scratch) {
        if(!holdsPhase(state,phase))return null;
        if(phase==GAS)return vaporStream(state,prepared,scratch);
        return new PhaseStream(phaseMoles(state,phase),phaseMass(state,phase),phaseVolume(state,phase),phaseViscosity(state,phase,scratch,prepared),
                phaseSpecificEnthalpy(state,phase,prepared),phaseVelocityLimit(state,phase),phaseSolidMoments(state,phase));
    }
    /** The amount sum {@link #state} forms ({@code sum}), without its validation: a state's own amounts are valid. */
    private static double total(double[] values){double total=0;for(double value:values)total+=value;return total;}
    /**
     * The partial pressure up to which the free water's steam is qualified as an ideal gas, Pa (0.125 MPa near ambient
     * to 2 MPa from 900 K): a data range, which a notice reports (owner rule of 2026-10-16); above it the ideal steam
     * continues.
     */
    public static double waterVaporPressureLimit(double t) {
        if(t>=900)return 2e6;if(t>=750)return 1.5e6;if(t>=600)return .8e6;
        if(t>=500)return .4e6;if(t>=450)return .25e6;if(t>=400)return .15e6;return .125e6;
    }
    /**
     * Liquid water at the state pressure, into the prepared temperature's Region 1 workspace (a fresh one for a
     * standalone call): Region 1 and its continuation past its range ({@link WaterRegion1#evaluateLiquid}).
     */
    private WaterRegion1.Workspace waterAt(double t,double p,Prepared prepared) {
        var workspace=prepared==null?new WaterRegion1.Workspace():prepared.water();
        WaterRegion1.evaluateLiquid(water,t,p,workspace);
        return workspace;
    }
    public WaterLiquid waterLiquid(double t,double p) { return waterLiquid(t,p,null); }
    /** The Region 1 temperature terms and the saturation pressure are functions of temperature alone, so a prepared
     * temperature keeps them for every pressure trial at that temperature; the pressure terms are evaluated per call. */
    public WaterLiquid waterLiquid(double t,double p,Prepared prepared) {
        var w=waterAt(t,p,match(prepared,t));
        return new WaterLiquid(w.specificVolume()*waterMolecularWeight,w.specificEnthalpy()*waterMolecularWeight+waterEnthalpyOffset);
    }

    /** Direct properties of specified phase amounts, without an inner flash. */
    public State state(double t,double p,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,double hydrocarbonPressure) {
        return state(t,p,liquid.clone(),vapor.clone(),waterLiquid,waterVapor,hydrocarbonPressure,null,null);
    }
    public State state(double t,double p,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,double hydrocarbonPressure,TranslatedPengRobinson.Workspace terms) {
        return state(t,p,liquid.clone(),vapor.clone(),waterLiquid,waterVapor,hydrocarbonPressure,terms,null);
    }
    /** Every temperature-only property comes from the prepared bundle; a standalone caller passes
     * {@code null} and each one is computed here, exactly as the mixing terms already were. */
    public State state(double t,double p,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,double hydrocarbonPressure,Prepared prepared) {
        return state(t,p,liquid.clone(),vapor.clone(),waterLiquid,waterVapor,hydrocarbonPressure,null,match(prepared,t));
    }
    /** The returned state adopts {@code liquid} and {@code vapor} instead of copying them: for a
     * caller that allocated them for this call and never reads or writes them again. */
    public State adoptingState(double t,double p,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,double hydrocarbonPressure,Prepared prepared) {
        return state(t,p,liquid,vapor,waterLiquid,waterVapor,hydrocarbonPressure,null,match(prepared,t));
    }
    private State state(double t,double p,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,double hydrocarbonPressure,
                        TranslatedPengRobinson.Workspace terms,Prepared prepared) {
        if(SolverDiagnostics.ENABLED) {
            SolverDiagnostics.stateCalls.increment();
            if(SolverDiagnostics.inJacobian())SolverDiagnostics.stateCallsInJacobian.increment();
        }
        int n=hydrocarbon.componentCount();
        if(liquid.length!=n||vapor.length!=n)throw new IllegalArgumentException("Fluid state basis mismatch");
        if(!Double.isFinite(t)||!Double.isFinite(p)||!Double.isFinite(waterLiquid)||!Double.isFinite(waterVapor))throw new IllegalArgumentException("Fluid state is not finite");
        // No data range bounds a state (owner rule of 2026-10-16: every property continues past its data, and the
        // interval's notice reports it); only the numerical box refuses a trial, a plain IllegalArgumentException.
        NumericalBox.require(t,p);
        // Free liquid water below its triple point would freeze; ice is not modelled yet: a capability hold. Water vapour
        // there is carried (a humid gas continues).
        if(waterLiquid>0&&t<water.triplePoint())throw new UnsupportedPhases(domain.packageId(),t,p,UnsupportedPhases.Kind.ICE,
                waterLiquid+" mol of free liquid water below the water model's triple point "+water.triplePoint()+" K");
        double nl=sum(liquid),nv=sum(vapor),volume=0,h=0,mass=0,gasVolume=0,vl=0,vw=0;
        if(terms==null&&(nl>0||nv>0))terms=prepared==null?hydrocarbon.prepare(t):prepared.pengRobinson();
        HydrocarbonModel.Phase lp=null,vp=null;
        // The liquid at the state pressure; with a vapour beside it, a liquid slot on a single vapour-like root is refused.
        if(nl>0) {lp=hydrocarbon.phase(t,p,liquid,PengRobinsonKernel.Root.LIQUID,terms,nv>0);vl=nl*lp.molarVolume();volume+=vl;h+=nl*lp.molarEnthalpy();}
        if(nv>0) {vp=hydrocarbon.phase(t,hydrocarbonPressure,vapor,PengRobinsonKernel.Root.VAPOR,terms);gasVolume=nv*vp.molarVolume();h+=nv*vp.molarEnthalpy();}
        if(waterLiquid<0||waterVapor<0||!Double.isFinite(waterLiquid+waterVapor))throw new IllegalArgumentException("Invalid water split");
        double kw=0;
        if(waterLiquid>0) {
            var w=waterAt(t,p,prepared);double molarVolume=w.specificVolume()*waterMolecularWeight;
            vw=waterLiquid*molarVolume;volume+=vw;h+=waterLiquid*(w.specificEnthalpy()*waterMolecularWeight+waterEnthalpyOffset);
            kw=-w.volumePressureDerivative()/w.specificVolume();
        }
        if(waterVapor>0) {if(nv==0)gasVolume=waterVapor*R*t/p;h+=waterVapor*(prepared==null?vaporWaterEnthalpy(t):prepared.vaporEnthalpy());}
        double pw=waterVapor>0?waterVapor*R*t/gasVolume:0;
        volume+=gasVolume;
        for(int i=0;i<n;i++)mass+=(liquid[i]+vapor[i])*hydrocarbon.molecularWeight(i);
        mass+=(waterLiquid+waterVapor)*waterMolecularWeight;
        if(!(volume>0)||!(mass>0)||!Double.isFinite(h))throw new IllegalArgumentException("Empty/nonfinite fluid properties");
        return new State(t,p,liquid,vapor,waterLiquid,waterVapor,hydrocarbonPressure,pw,volume,h,h-p*volume,mass,
                vl,vw,gasVolume,lp,vp,SolidInventory.EMPTY,SolidInventory.Moments.ZERO,kw);
    }

    /** Stationary dry inventory. Pressure is retained as an initialization hint, not a gas pressure. */
    public State solidState(double temperature,double pressure,com.wormzjl.createcheme.science.fluid.state.SolidInventory solids) {
        if(solids.empty()||!Double.isFinite(temperature)||!Double.isFinite(pressure))throw new IllegalArgumentException("Dry solid state needs solids and a finite temperature and pressure");
        NumericalBox.require(temperature,pressure);
        var moments=solids.moments();var empty=new double[hydrocarbon.componentCount()];
        return new State(temperature,pressure,empty,empty,0,0,0,0,moments.volume(),
                moments.enthalpy(temperature,pressure),moments.internalEnergy(temperature),moments.mass(),0,0,0,null,null,solids,moments,0);
    }

    /**
     * Owner decision D34 (phase-ports batch, 2026-09-28): a fluid phase holding less than this share of its node's fluid
     * amount is a trace phase and does not exist; its material is in the node's other phase (A83). The share is taken in
     * moles over every fluid phase and component of the node, solids not counted: the unit the Newton states a node's
     * balances in (its tolerance is a share of the node's total amount), so a phase below it is below what the solve
     * resolves, and the same number for a condensate film in a gas and for the last bubble in a liquid.
     */
    public static final double PHASE_TRACE_FRACTION=1e-9;
    /**
     * The share under which a vessel's own closure that is shrinking a proposed phase is driving it to nothing, converged
     * or not ({@link #vanishing}; the D26 amendment by direction, owner decision of 2026-10-16, form 4). It is what a
     * closure that stops short resolves: its 24 iterations end at a residual near 1e-6 of the node's amount, and cold seed
     * 48's liquid-full tank beside dry ice stalls with its vapour at 1e-9 to 3e-9 of the moles and still falling, which the
     * trace share alone keeps or drops at rounding.
     */
    public static final double VANISHING_SHARE=1e-6;
    /**
     * Folds the trace phases of one split in place (owner decisions D33, D34): nothing leaves the node and nothing is
     * booked, so every component amount, and with it the node's mass, is unchanged. The rule (A84):
     * <ul>
     * <li>a trace hydrocarbon liquid goes into the gas phase when that holds hydrocarbons (a gas of water vapour alone has
     * no hydrocarbon partial pressure to state it at);</li>
     * <li>a trace of free water goes into the gas phase as water vapour;</li>
     * <li>a trace gas phase goes into the liquids, its hydrocarbons into the hydrocarbon liquid and its water vapour into
     * the free water, when each part it holds has its liquid;</li>
     * <li>a trace gas whose gas components have no hydrocarbon liquid to go into, in a node with free water, is a carried
     * gas (owner decision D41, {@link #carriedGas}): its water vapour goes into the free water and its gas components stay
     * a dry gas at the node's pressure. A trace gas is no gas space, so it has no saturation row of its own;</li>
     * <li>a trace phase none of whose receiving phases exists stays otherwise: a trace of hydrocarbon liquid in free water
     * with no gas. A receiving phase that is itself a trace receives nothing.</li>
     * </ul>
     * {@code water} is {free water, water vapour}. Returns whether anything moved. This is the rule; {@link #folded} and
     * every flash apply it to a state.
     */
    public boolean foldTracePhases(double[] liquid,double[] vapor,double[] water) {
        double nl=0,nh=0;for(double x:liquid)nl+=x;for(double x:vapor)nh+=x;
        double nv=nh+water[1],nw=water[0],total=nl+nv+nw;
        if(!(total>0))return false;
        double limit=PHASE_TRACE_FRACTION*total;boolean moved=false;
        if(nl>0&&nl<limit&&nh>0&&nv>=limit){for(int i=0;i<liquid.length;i++){vapor[i]+=liquid[i];liquid[i]=0;}moved=true;}
        if(nw>0&&nw<limit&&nv>=limit){water[1]+=water[0];water[0]=0;moved=true;}
        if(nv>0&&nv<limit&&(nh==0||nl>=limit)&&(water[1]==0||nw>=limit)) {
            for(int i=0;i<vapor.length;i++){liquid[i]+=vapor[i];vapor[i]=0;}water[0]+=water[1];water[1]=0;moved=true;
        }
        else if(nv>0&&nv<limit&&nh>0&&nl<limit&&water[1]>0&&nw>=limit){water[0]+=water[1];water[1]=0;moved=true;}
        return moved;
    }
    /**
     * Owner decision D41 (phase-ports batch, 2026-10-13): a gas bubble in a liquid-full pipe is carried by the liquid, at the
     * liquid's velocity, to the next gas space (a vessel's gas space, a void, a gas-filled junction), where it joins the
     * gas there. In the pipe it sits at the liquid's temperature and pressure with its own volume, and it is in no
     * equilibrium of its own with the water around it: its water vapour is a negligible part of a negligible phase. Such a
     * gas is a <em>carried gas</em>: a dry gas at the node's pressure (its hydrocarbon partial pressure is the pressure),
     * whose water is in the free water, so the node states no saturation row and no partial-pressure row for it. It leaves
     * with the liquid in proportion (a junction's outflow is its bulk), and the first node with a gas space gives it its
     * vapour again in its own flash. A node holds a carried gas, rather than a gas space, when
     * <ul>
     * <li>its gas is a trace ({@link #foldTracePhases}), whatever the node: a trace gas is no gas space;</li>
     * <li>it is a pipe junction ({@code junction}) whose only liquid is free water and whose free water fills more of it
     * than its gas: the water is the continuous phase and the gas is bubbles in it. A vessel separates its gas upwards
     * into a gas space by gravity and keeps the saturation of that space; a junction is a piece of pipe the liquid runs
     * through, held 0.05 s of its peak flow (D25).</li>
     * </ul>
     * Returns whether {@code state} holds a gas of the second kind that is not yet dry.
     */
    public static boolean carriedGas(State state,boolean junction) {
        if(!junction||!(state.waterVapor()>0)||!(state.waterLiquid()>0)||state.liquidVolume()>0||!(state.vaporVolume()<state.waterVolume()))return false;
        for(double x:state.vaporView())if(x>0)return true;
        return false;
    }
    /**
     * Whether {@code state} holds a carried trace gas (decision D41): a dry gas, below {@link #PHASE_TRACE_FRACTION} of the
     * node's moles, in free water with no hydrocarbon liquid. Below that share the gas is below what a junction's rows
     * resolve (they are stated over the junction's whole holdup, decision D34, A83), so a junction's step layout carries no
     * unknown for it: its gas components are an omitted trace of the junction ({@code PhaseLayout.support}), carried
     * exactly by the transport reconstruction like every other component, and the junction's state keeps them as this
     * dry gas.
     */
    public static boolean carriedTrace(State state) {
        if(state.liquidVolume()>0||state.waterVapor()>0||!(state.waterLiquid()>0))return false;
        double nh=0;for(double x:state.vaporView())nh+=x;
        return nh>0&&nh<PHASE_TRACE_FRACTION*(nh+state.waterLiquid());
    }
    /**
     * The hydrocarbon partial pressure of a split the trace rule restated ({@link #foldTracePhases}): a gas holding
     * hydrocarbons and no water vapour, the carried gas of decision D41, is at the node's pressure, as a layout decodes a
     * dry gas; any other split keeps the one it was stated at.
     */
    public static double foldedHydrocarbonPressure(double pressure,double hydrocarbonPressure,double[] vapor,double waterVapor) {
        if(waterVapor>0)return hydrocarbonPressure;
        for(double x:vapor)if(x>0)return pressure;
        return hydrocarbonPressure;
    }
    /**
     * {@code state} restated at its temperature and pressure with its trace phases folded ({@link #foldTracePhases}),
     * its solids kept; the state itself when it holds none. The fluid network's one statement of which phases a node has:
     * every flash ({@link #flashTP}), the reconstruction's restatement of a node and the phase set each Newton pass is
     * laid out for pass through it. Never inside a residual: a fold is a jump of the state function.
     */
    public State folded(State state) {
        var result=folded(state,null);
        return result==state||state.solids().empty()?result:result.withSolidState(state.solids(),state.solidMoments());
    }
    /**
     * {@link #folded(State)} for a node that is a pipe junction ({@code junction}) or a vessel, with the carried gas of
     * decision D41 ({@link #carriedGas}): a liquid-full junction's gas is restated dry at the junction's pressure, its
     * water vapour in the free water. Nothing leaves the node and nothing is booked, as for the trace fold.
     */
    public State folded(State state,boolean junction) {
        var result=folded(state);
        if(!carriedGas(result,junction))return result;
        SolverDiagnostics.count(SolverDiagnostics.traceFolds);
        var carried=state(result.temperature(),result.pressure(),result.liquid(),result.vapor(),result.waterLiquid()+result.waterVapor(),0,result.pressure(),null,null);
        return result.solids().empty()?carried:carried.withSolidState(result.solids(),result.solidMoments());
    }
    private State folded(State state,TranslatedPengRobinson.Workspace terms) {
        double[] l=state.liquid(),v=state.vapor(),water={state.waterLiquid(),state.waterVapor()};
        if(!foldTracePhases(l,v,water))return state;
        SolverDiagnostics.count(SolverDiagnostics.traceFolds);
        return state(state.temperature(),state.pressure(),l,v,water[0],water[1],
                foldedHydrocarbonPressure(state.pressure(),state.hydrocarbonPartialPressure(),v,water[1]),terms,null);
    }

    /** A state's fluid phases as bits: hydrocarbon liquid 1, hydrocarbon vapour 2, free water 4, water vapour 8. */
    public static int phaseBits(State state) {
        return (state.liquidVolume()>0?1:0)|(state.vaporProperties()!=null?2:0)|(state.waterLiquid()>0?4:0)|(state.waterVapor()>0?8:0);
    }
    /**
     * The outer phase check of one node of the fluid network (owner decisions D25 and D26), never inside a residual. A node
     * at fixed amounts, energy and volume has one equilibrium (a pipe junction: at its mixing rows' enthalpy and the
     * network's pressure); the node's own rows find it once its layout holds every phase that equilibrium needs, while a
     * TP answer at the node's temperature and pressure tells which phases are missing but not how much of each the node
     * holds. So the check never swaps one populated phase set for another on a TP verdict, and an added phase starts at the
     * node's own split:
     * <ul>
     * <li>a phase leaves only when the solve empties it: the trace fold ({@link #folded}, D34) removes a phase {@code state}
     * holds below the trace share, also where {@code state} is an unsuccessful solve's last iterate;</li>
     * <li>a node holding every phase its material can form (both hydrocarbon phases or none, both water phases or none)
     * has none to add, and no TP answer is formed. A stand-in liquid ({@link #condensedShareOnly}: a crystal's share
     * carried condensed between refreshes, crystal species only beside a vapour) is no liquid of the node's own (owner
     * decision D31 B, for every node), so it never makes the node liquid-complete; in a TP answer it is a phase like any
     * other, proposed to a node that holds no liquid at all and decided by the node's own split ({@link #lacking});</li>
     * <li>where the TP answer ({@link #flashTP}: {@link #equilibriumTP} with its fold) holds a phase the node lacks that is
     * not in the solve's record ({@code record}, A85), the node's own split decides it (D26), the equilibrium of what it
     * holds: a pipe junction's PH answer at its pressure and enthalpy ({@link #equilibriumPH}), a vessel's
     * {@code vesselSplit}, the network's closure of its inventory (UV while its wall is taut, PH at the ambient pressure
     * while slack), started from the TP answer with the node's populated phases it lacks ({@link #tpSeed}). Where the
     * node's own flash fails, or none is given, that TP-based state seeds instead; it is only a seed, the node's rows give
     * the answer;</li>
     * <li>the own split decides by where its closure went, converged or not (the D26 amendment of 2026-10-16, by its
     * direction in form 4): a proposed phase it drove under the trace share, which the seed's fold (D34, and D41's carried
     * gas at a junction) then removes, or that a vessel's closure is driving to nothing ({@link #vanishing}), is refused,
     * and the refusal is written to the record, so the TP answer does not propose it again in this solve: a boiling liquid
     * in a vessel with no room for a bubble pressurises and stays liquid, a drop formed in a gas at its dew point
     * re-evaporates. A proposed phase that stands seeds the node, and the seed is taken only when it then holds a phase the
     * node lacks that is not in the record and none that was refused: nothing the fold removes is added, so the check and
     * the fold agree.</li>
     * </ul>
     * The rule decides which phases a node is laid out with; how much of each it holds is its rows' answer. It holds for
     * any material: a pure water node at its saturation pressure, whose TP answer is all liquid on one side and all steam
     * on the other, keeps its liquid and gains the steam its energy and volume give; a rigid vessel cooled past its bubble
     * point gains the small vapour of its own energy and volume, not the TP split's several vessel volumes; a liquid
     * junction let down below its bubble point starts flashed at its enthalpy, and water and a gas mixed in a junction
     * start with the gas saturated with steam where that makes the gas the larger phase (the bubbles grow), as a dry
     * carried gas where it does not.
     *
     * <p>{@code record}, {@code at}: the solve's A85 record of the node's phases ({@link #phaseBits}), {@code record[at]}:
     * read as the phases the solve removed or refused, which are not added, and written with the phases this check removes
     * (the fold, a phase the seed lacks) and refuses; {@code null} reads none and writes nothing. A trial check (a failed
     * iterate, decision D28 A') passes a copy. {@code junction}: whether the node is a pipe junction (the carried gas of
     * D41 in the fold, its own split the PH answer); {@code prepared}, when given, is the node's prepared temperature state
     * (used only at exactly that temperature); {@code vesselSplit}, a vessel's own split from a start holding the phases to
     * lay it out with, or {@code null} where the vessel's flash fails (ignored for a junction).
     *
     * @return {@code state} itself when it keeps its phases and gains none; otherwise the node's new seed
     */
    public State phaseCheck(State state,boolean junction,int[] record,int at,Prepared prepared,UnaryOperator<State> vesselSplit,Runnable checkpoint) {
        var kept=folded(state,junction);var checked=kept;
        int excluded=record==null?0:record[at],refused=0;
        boolean hydrocarbonComplete=ownPhaseBits(kept)==phaseBits(kept)&&(kept.liquidProperties()==null)==(kept.vaporProperties()==null);
        boolean waterComplete=kept.waterLiquid()>0&&kept.waterVapor()>0||kept.waterLiquid()+kept.waterVapor()==0;
        if(!(hydrocarbonComplete&&waterComplete)) {
            if(prepared!=null&&prepared.temperature()!=kept.temperature())prepared=null;
            int n=hydrocarbon.componentCount();double[] totals=new double[n+1];
            for(int i=0;i<n;i++)totals[i]=kept.liquidView()[i]+kept.vaporView()[i];
            totals[n]=kept.waterLiquid()+kept.waterVapor();
            var equilibrium=folded(equilibriumTP(kept.temperature(),kept.pressure(),totals,checkpoint,prepared).withSolidState(kept.solids(),kept.solidMoments()),junction);
            int proposed=lacking(kept,equilibrium)&~excluded;
            if(proposed!=0) {
                var start=tpSeed(kept,equilibrium,prepared);
                // A junction's energy is its enthalpy (its mixing rows, D46): the solve's, before the fold restated it.
                State own=junction?equilibriumPH(kept.temperature(),kept.pressure(),state.enthalpy()-state.solidMoments().enthalpy(state.temperature(),state.pressure()),totals,checkpoint)
                        :vesselSplit==null?null:vesselSplit.apply(start);
                SolverDiagnostics.count(own==null?SolverDiagnostics.phaseSeedFallbacks:SolverDiagnostics.phaseSeeds);
                var seed=folded(own==null?start:own.withSolidState(kept.solids(),kept.solidMoments()),junction);
                // The D26 amendment: the own split's verdict on a proposed phase is where its closure took it; the TP-based
                // fallback seed is no closure and refuses nothing.
                if(own!=null)refused=proposed&(~lacking(kept,seed)|(junction?0:vanishing(start,own)));
                if(refused!=0)SolverDiagnostics.count(SolverDiagnostics.phaseRefusals);
                int added=lacking(kept,seed)&~excluded;
                if(added!=0&&(added&refused)==0)checked=seed;
            }
        }
        if(record!=null)record[at]|=phaseBits(state)&~phaseBits(checked)|refused;
        return checked;
    }
    /**
     * {@link #phaseBits} of the phases a node holds as its own, for {@link #phaseCheck} (owner decision D31 B, for every
     * node): a stand-in liquid ({@link #condensedShareOnly}) is a crystal's condensed share, no liquid of the node's own, so
     * its liquid bit is not set and a node holding one is never liquid-complete.
     */
    private int ownPhaseBits(State state){return condensedShareOnly(state)?phaseBits(state)&~1:phaseBits(state);}
    /**
     * The phases ({@link #phaseBits}) of {@code answer} that {@code node} lacks, for {@link #phaseCheck}: a liquid of its own
     * where the node holds none of its own (a stand-in in its slot does not count, D31 B), and a stand-in where the node holds
     * no liquid at all. A stand-in is a phase like any other (owner decision of 2026-10-16, form 4): it carries the energy of
     * the frost a stream brings until the next refresh (liquid CO2 let down into a tank of its gas freezes in part at once),
     * and the node's own split decides it, keeping it where it stands and refusing it where its closure drives it to
     * nothing (a supersaturated gas whose CO2 has no liquid to stand in).
     */
    private int lacking(State node,State answer){return ownPhaseBits(answer)&~ownPhaseBits(node)|phaseBits(answer)&~phaseBits(node);}
    /**
     * The phases ({@link #phaseBits}) a vessel's own closure from {@code start} is driving to nothing at {@code end}, its
     * answer or the last iterate it stopped at (the D26 amendment by direction, owner decision of 2026-10-16, form 4): each
     * holds less than {@link #VANISHING_SHARE} of {@code end}'s fluid amount and a smaller share than it held at
     * {@code start}. A phase the closure grows, however small, is not among them.
     */
    static int vanishing(State start,State end) {
        double[] s=phaseAmounts(start),e=phaseAmounts(end);double ts=sum(s),te=sum(e);int bits=0;
        for(int k=0;k<4;k++)if(e[k]<VANISHING_SHARE*te&&e[k]*ts<s[k]*te)bits|=1<<k;
        return bits;
    }
    /** A state's fluid amount by phase, in {@link #phaseBits} order: hydrocarbon liquid, vapour, free water, water vapour. */
    private static double[] phaseAmounts(State state){return new double[]{sum(state.liquidView()),sum(state.vaporView()),state.waterLiquid(),state.waterVapor()};}
    /**
     * The TP-based state of {@link #phaseCheck} (decision D26): the TP answer {@code equilibrium} at {@code kept}'s
     * temperature and pressure, for {@code kept}'s totals, with the populated phases of {@code kept} it lacks, so that no
     * TP verdict takes a populated phase away (D25). Where the answer holds every phase of {@code kept} it is the answer;
     * otherwise each component is shared between the answer's phases and those kept phases in proportion to what each
     * holds of it (pure water the answer states all steam: half its water liquid, half steam), and a gas holding
     * hydrocarbons and steam is at Dalton's hydrocarbon partial pressure, a dry gas at the node's pressure. The answer
     * itself where that gas would pass the steam approximation's qualified partial pressure. The start of a vessel's own
     * closure, and the seed where a node's own flash fails.
     */
    private State tpSeed(State kept,State equilibrium,Prepared prepared) {
        int answer=phaseBits(equilibrium),lacking=phaseBits(kept)&~answer;
        if(lacking==0)return equilibrium;
        double[] kl=kept.liquidView(),kv=kept.vaporView(),el=equilibrium.liquidView(),ev=equilibrium.vaporView();
        double[] l=new double[kl.length],v=new double[kl.length];double nv=0;
        for(int c=0;c<l.length;c++) {
            double a=(answer&1)!=0?el[c]:(lacking&1)!=0?kl[c]:0,b=(answer&2)!=0?ev[c]:(lacking&2)!=0?kv[c]:0,total=kl[c]+kv[c];
            l[c]=b>0?total*(a/(a+b)):total;v[c]=total-l[c];nv+=v[c];
        }
        double a=(answer&4)!=0?equilibrium.waterLiquid():(lacking&4)!=0?kept.waterLiquid():0;
        double b=(answer&8)!=0?equilibrium.waterVapor():(lacking&8)!=0?kept.waterVapor():0,water=kept.waterLiquid()+kept.waterVapor();
        double wl=b>0?water*(a/(a+b)):water,wv=water-wl,p=kept.pressure();
        try{return adoptingState(kept.temperature(),p,l,v,wl,wv,nv>0&&wv>0?p*(nv/(nv+wv)):p,prepared).withSolidState(kept.solids(),kept.solidMoments());}
        catch(IllegalArgumentException steamLimit){return equilibrium;}
    }

    /**
     * Initializer, boundary and outer-check flash; the time-step solver never calls it in a residual evaluation: the
     * equilibrium of {@link #equilibriumTP} with its trace phases folded ({@link #folded}).
     */
    public State flashTP(double t,double p,double[] overall,Runnable checkpoint) {
        return flashTP(t,p,overall,checkpoint,null);
    }
    /** {@link #flashTP} that uses {@code prepared}'s Peng-Robinson workspace when it is for exactly {@code t}, instead of
     * preparing a new one (the arithmetic is the same). */
    public State flashTP(double t,double p,double[] overall,Runnable checkpoint,Prepared prepared) {
        TranslatedPengRobinson.Workspace given=prepared!=null&&prepared.temperature()==t?prepared.pengRobinson():null;
        return folded(equilibriumTP(t,p,overall,checkpoint,prepared),given);
    }
    /**
     * The equilibrium split of {@link #flashTP} before its trace phases are folded. Not a state the network lays out or
     * commits: {@code flashTP} is that, {@code folded(equilibriumTP(...))}. The step solver's phase correction reads both
     * to know which phase the trace rule, rather than the equilibrium, removed (decision D34, A85).
     *
     * <p>The phases of {@code overall} (the conserved basis, water last) at {@code (t, p)} are decided by the equilibrium
     * engine ({@link NetworkPhaseEngine}): the tangent-plane stability test first, then the split with its second-order
     * finish, the declared critical band, the phase identification parameter for a one-root phase, and the network's
     * separate free-water rule; this adapter states that answer as a network {@link State} with the network's own direct
     * phase evaluations ({@link #state}). The Wilson-started successive substitution and its root heuristic are
     * retired.</p>
     *
     * <p>Slots: a split's lighter phase is the vapour slot and its denser phase the liquid slot; a phase with three roots
     * takes its root's slot; a one-root phase the engine labels liquid-like or vapour-like takes that slot, and a
     * {@code FLUID}-labelled one (a pure supercritical fluid, a merged near-critical split) the slot its own phase
     * identification parameter gives. With water the engine's free-water record gives the water split and the
     * hydrocarbon vapour's partial pressure. A pure component exactly at its own coexistence pressure is a single
     * liquid here (the split is undetermined by T and P).</p>
     *
     * <p>Refusals: a state outside the numerical box is a plain {@link IllegalArgumentException} (checked before the
     * engine runs; no data range bounds a state); free liquid water below its triple point is the typed
     * {@link UnsupportedPhases.Kind#ICE} hold, as in {@link #state}, decided before the engine runs where the water exceeds
     * what the hydrocarbons carry as vapour at its saturation pressure; an answer the engine could not converge inside the
     * declared critical band is the typed
     * {@link CriticalBandHold}; a liquid-liquid split (two liquid-like hydrocarbon phases) and a split with a product proved
     * unstable (a third fluid phase indicated) are {@link UnsupportedPhases}, since a node carries one hydrocarbon liquid
     * and one vapour; any other failure is a plain {@link IllegalArgumentException}. Pure water needs no phase decision
     * (all liquid at or above its saturation pressure, all steam below).</p>
     *
     * <p>{@code checkpoint} runs once before the engine and once per equation-of-state evaluation the engine reports, so
     * a budget clock counts the flash's work (the engine itself has no checkpoint; one call is bounded).</p>
     *
     * <p>Crystals. On a package that admits crystals a dry request carrying a crystal's species at or below the
     * crystals' ceiling is made under the crystal competition ({@link #crystalsCompete}). A TP answer never moves
     * material into a crystal: crystal mass moves only in a vessel's refresh (owner decision D23,
     * {@code InventoryEquilibrium}), so a step, a transport node and a boundary carry a supersaturated fluid, and the
     * receiving vessel's next refresh precipitates it. Where a crystal forms, the answer's vapour is the vapour and the
     * liquid slot takes the rest, the crystal's share condensed in it (see {@link #adopt}). A crystal beside a phase set
     * the engine does not implement (two liquids, three fluid phases, two crystals) is the typed
     * {@link UnsupportedPhases.Kind#CRYSTAL_PHASES} hold.</p>
     */
    public State equilibriumTP(double t,double p,double[] overall,Runnable checkpoint,Prepared prepared) {
        TranslatedPengRobinson.Workspace given=prepared!=null&&prepared.temperature()==t?prepared.pengRobinson():null;
        SolverDiagnostics.count(SolverDiagnostics.flashCalls);
        int n=hydrocarbon.componentCount();if(overall.length!=n+1)throw new IllegalArgumentException("Fluid basis mismatch");
        double[] hc=Arrays.copyOf(overall,n);double nh=sum(hc),w=overall[n];
        if(!Double.isFinite(w)||w<0||nh+w<=0)throw new IllegalArgumentException("Empty fluid initialization");
        if(!Double.isFinite(t)||!Double.isFinite(p))throw new IllegalArgumentException("Fluid initialization is not finite");
        // Refused before any equilibrium is formed: outside the numerical box no model is trusted at all.
        NumericalBox.require(t,p);
        if(nh==0)return state(t,p,hc,hc,p>=saturationPressure(t)?w:0,p>=saturationPressure(t)?0:w,0);
        // Free liquid water below its triple point is certain where the water exceeds what the hydrocarbons, all vapour, carry
        // at its saturation pressure: the ice hold (a capability limit, ice is not modelled yet) comes before the hydrocarbon
        // phases are decided, whatever limit they would meet.
        if(w>0&&t<water.triplePoint()) {
            double psat=saturationPressure(t),carried=p>psat?nh*psat/(p-psat):Double.POSITIVE_INFINITY;
            if(w>carried)throw new UnsupportedPhases(domain.packageId(),t,p,UnsupportedPhases.Kind.ICE,
                    (w-carried)+" mol or more of free liquid water below the water model's triple point "+water.triplePoint()+" K");
        }
        checkpoint.run();
        var result=phaseEngine().tp(t,p,overall,crystalsCompete(t,overall));
        for(int evaluation=result.diagnostics().kernelEvaluations();evaluation>0;evaluation--)checkpoint.run();
        return adopt(t,p,overall,result,given);
    }
    /**
     * A pipe junction's own split (owner decision D26, {@link #phaseCheck}), and the phases of an empty bag's
     * ({@code PassiveStepSolver.emptyBagSplit}, D54): the engine's PH equilibrium of {@code overall}
     * (the conserved basis, water last) at the junction's pressure {@code p} and its fluid enthalpy {@code h} (J, the
     * network's datum), under the competition of the junction's TP request at {@code t} ({@link #crystalsCompete}), as a
     * network state ({@link #adopt}, unfolded); {@code null} where the engine does not converge or answers a phase set the
     * network cannot carry. The engine of a spine package states energies on the spine's formation datum, the network on
     * its sensible one (D10), so the target moves by the formation enthalpies; a spine-less package's engine is on the
     * network's datum. {@code checkpoint} runs as for {@link #equilibriumTP}. Never inside a residual.
     */
    public State equilibriumPH(double t,double p,double h,double[] overall,Runnable checkpoint) {
        var spine=hydrocarbon.spine();double target=h;
        if(spine.isPresent()) {
            var formation=spine.get().formationReference();
            target=EnergyReference.sensible(formation.components()).rebase(h,Arrays.copyOf(overall,hydrocarbon.componentCount()),formation);
        }
        checkpoint.run();
        var result=phaseEngine().ph(p,target,overall,crystalsCompete(t,overall));
        for(int evaluation=result.diagnostics().kernelEvaluations();evaluation>0;evaluation--)checkpoint.run();
        if(!result.converged()||result.phases().isEmpty())return null;
        try{return adopt(result.phases().getFirst().temperature(),p,overall,result,null);}
        catch(IllegalArgumentException refused){return null;}
    }

    /** The crystals the package admits, in declaration order; empty without a crystal competition. */
    List<String> crystals(){return crystalIds;}
    /**
     * Whether a request on {@code overall} (conserved basis, water last) at {@code t} is made under the crystal competition:
     * the package admits crystals, the request carries a crystal's species and no water (crystals beside water would
     * compete with hydrates, D8; free liquid water below 273.16 K holds as ice not modelled yet), and {@code t} is at or
     * below the crystals' ceiling (the competition's existence limit, not a data range: above it no crystal is stable).
     * Otherwise fluid-only.
     */
    public boolean crystalsCompete(double t,double[] overall) {
        if(crystalIds.isEmpty()||overall[overall.length-1]!=0||!(t<=crystalCeiling))return false;
        for(int i=0;i<crystalSpecies.length;i++)if(crystalSpecies[i]&&overall[i]>0)return true;
        return false;
    }

    /**
     * Owner decision D29 A: whether {@code state}'s hydrocarbon liquid is the stand-in's condensed share alone (see
     * {@link #adopt}): a liquid holding crystal species only, beside a vapour, as a frost answer's crystal is carried. It is
     * no phase of the answer, so its composition says nothing of which components a liquid there holds: the trace support
     * reads the answer's own fluid phase, the vapour, and the stand-in admits every component the node holds, as the
     * supersaturated solution it stands for ({@code PhaseLayout.support}). In the outer phase check it is no liquid of the
     * node's own (owner decision D31 B, {@link #phaseCheck}): it never makes the node liquid-complete; a TP answer's
     * stand-in is proposed like any phase where the node holds no liquid, and the node's own split decides it (form 4).
     * Never on a package without crystals.
     */
    public boolean condensedShareOnly(State state) {
        if(crystalIds.isEmpty()||!(state.liquidVolume()>0)||state.vaporProperties()==null)return false;
        double[] l=state.liquidView();
        for(int i=0;i<l.length;i++)if(l[i]>0&&!crystalSpecies[i])return false;
        return true;
    }

    /** The engine's answer as a network state (see {@link #equilibriumTP}), or its typed refusal; {@code terms}, when
     * given, is the prepared Peng-Robinson workspace of exactly {@code t}. */
    State adopt(double t,double p,double[] overall,EquilibriumResult result,TranslatedPengRobinson.Workspace terms) {
        switch(result.status()) {
            case CONVERGED->{}
            case NOT_CONVERGED->{
                if(result.diagnostics().criticalBand())throw new CriticalBandHold(domain.packageId(),t,p,result.detail());
                // A converged split with a product proved unstable: a third fluid phase is indicated, which a node cannot
                // carry; a typed hold like the critical band's, never adopted with fewer phases. A product whose stability
                // the re-check left unresolved is the same hold short of the proof: the split is no answer, and a third
                // phase is not excluded (the design review's E9: a junction's arrival flash held untyped, naming no node).
                boolean unstable=result.detail().startsWith(EquilibriumResult.UNSTABLE_PRODUCT);
                if(unstable||result.detail().startsWith(EquilibriumResult.UNRESOLVED_PRODUCT))throw new UnsupportedPhases(domain.packageId(),t,p,
                        UnsupportedPhases.Kind.THREE_PHASES,"a third fluid phase is "+(unstable?"indicated":"not excluded")+" ("+result.detail()
                        +"); the fluid network carries one hydrocarbon liquid and one hydrocarbon vapour");
                throw new IllegalArgumentException("Fluid TP equilibrium did not converge: "+result.detail());
            }
            case UNSUPPORTED->{
                // A crystal beside a phase set the engine does not implement (two liquids, three fluid phases, two
                // crystals): held typed, never answered with fewer phases.
                if(result.detail().startsWith(EquilibriumResult.CRYSTAL_PHASES))throw new UnsupportedPhases(domain.packageId(),t,p,
                        UnsupportedPhases.Kind.CRYSTAL_PHASES,"a crystal beside a phase set the engine does not implement ("+result.detail()+")");
                throw new IllegalStateException("The network's equilibrium engine refused a request it must answer: "+result.detail());
            }
            default->throw new IllegalStateException("The network's equilibrium engine refused a request it must answer: "+result.detail());
        }
        int n=hydrocarbon.componentCount();double[] liquid=new double[n],vapor=new double[n];
        double waterLiquid=0,waterVapor=0,hydrocarbonPressure=p;
        if(terms==null)terms=hydrocarbon.prepare(t);
        switch(result.classification()) {
            // Two liquid-like hydrocarbon phases: the network carries one hydrocarbon liquid, so the lighter liquid must
            // not take the vapour slot; a typed hold, never adopted with fewer phases.
            case LIQUID_LIQUID->throw new UnsupportedPhases(domain.packageId(),t,p,UnsupportedPhases.Kind.LIQUID_LIQUID,
                    "a liquid-liquid split (two liquid-like hydrocarbon phases); the fluid network carries one hydrocarbon liquid");
            // A crystal in the answer. A TP phase decision never moves material into a crystal (owner decision D23: only a
            // vessel's refresh does), so the mobile phases of these amounts carry the crystal's share until then, and that
            // share is condensed matter: the vapour is the answer's vapour (none beside a crystal out of a liquid, or a
            // crystal alone) and the liquid slot takes the rest, a supersaturated liquid, per component by difference and
            // never negative, so the amounts are conserved exactly. In the vapour slot the crystal's mass would take a
            // gas's volume and energy: a CO2 let-down below the triple point, stated all vapour, ran to CO2's 90 K floor.
            // Its own fluid split with the crystal held out is a CO2-rich liquid, beside a methane liquid a third fluid
            // phase: row E of the pilot world held at its first step.
            case VAPOR_SOLID,LIQUID_SOLID,VAPOR_LIQUID_SOLID,SOLID_PRESENT->{
                var classification=result.classification();
                if(classification==EquilibriumResult.Classification.VAPOR_SOLID||classification==EquilibriumResult.Classification.VAPOR_LIQUID_SOLID)
                    for(var phase:result.phases())if(phase.kind()==PhaseKind.VAPOR||phase.kind()==PhaseKind.FLUID)System.arraycopy(phase.amountsView(),0,vapor,0,n);
                for(int i=0;i<n;i++) {
                    liquid[i]=Math.max(overall[i]-vapor[i],0.0);
                    vapor[i]=overall[i]-liquid[i];
                }
            }
            // A pure component at its own coexistence pressure, the split undetermined by T and P: a single liquid, or
            // the vapour of a pure sublimation coexistence.
            case PURE_COEXISTENCE_UNDERDETERMINED->{
                boolean sublimation=false;for(var state:result.coexisting())sublimation|=state.kind()==PhaseKind.SOLID;
                System.arraycopy(overall,0,sublimation?vapor:liquid,0,n);
            }
            case FREE_WATER->{
                var split=result.freeWater();
                waterLiquid=split.liquidWater();waterVapor=split.waterVapor();hydrocarbonPressure=split.hydrocarbonPressure();
                for(var phase:result.phases()) {
                    if(phase.kind()==PhaseKind.VAPOR)System.arraycopy(phase.amountsView(),0,vapor,0,n);
                    else if(phase.kind()==PhaseKind.LIQUID)System.arraycopy(phase.amountsView(),0,liquid,0,n);
                }
            }
            default->{
                for(var phase:result.phases()) {
                    boolean liquidSlot=switch(phase.kind()) {
                        case LIQUID->true;
                        case VAPOR->false;
                        default->{
                            // A pure supercritical fluid or a merged near-critical split: one root, whose own phase
                            // identification parameter decides the slot (both slots evaluate that same root).
                            double[] amounts=Arrays.copyOf(phase.amountsView(),n);
                            yield HydrocarbonModel.liquidLike(hydrocarbon.translated().evaluateValues(t,p,amounts,phase.state().root(),terms));
                        }
                    };
                    System.arraycopy(phase.amountsView(),0,liquidSlot?liquid:vapor,0,n);
                }
            }
        }
        return state(t,p,liquid,vapor,waterLiquid,waterVapor,hydrocarbonPressure,terms,null);
    }

    /**
     * A node's phases the engine could not converge inside the declared critical band: the engine's typed
     * {@code CRITICAL_BAND} failure, a {@link ThermoHold}; no split is fabricated and no single phase is claimed stable.
     */
    public static final class CriticalBandHold extends ThermoHold {
        public CriticalBandHold(String packageId,double temperature,double pressure,String detail) {
            this(packageId,temperature,pressure,detail,NO_NODE);
        }
        private CriticalBandHold(String packageId,double temperature,double pressure,String detail,long node) {
            super("Critical band: the phases at "+temperature+" K, "+pressure+" Pa (package "+packageId+")"
                    +" did not converge inside the declared critical band; "+detail,packageId,temperature,pressure,detail,node);
        }
        @Override public CriticalBandHold at(long node) {
            return node()!=NO_NODE||node==NO_NODE?this:new CriticalBandHold(packageId,temperature,pressure,detail,node);
        }
        @Override public String reasonKey(){return REASON_PREFIX+"critical band";}
        @Override public boolean sameAs(ThermoHold other) {
            return other instanceof CriticalBandHold band&&band.node()==node()&&band.packageId.equals(packageId);
        }
        @Override String what(){return "the phases did not converge inside the declared critical band";}
    }

    /**
     * An equilibrium answer the network cannot carry, typed with the equilibrium service's reason: a node has one
     * hydrocarbon liquid slot and one vapour slot (D8: a phase set outside the qualified ones is refused, never answered
     * with fewer phases). The kinds: a liquid-liquid split, a third fluid phase (indicated by a converged split whose
     * product the stability re-check proved unstable, or not excluded where it left one unresolved), a crystal beside a
     * phase set the engine does not implement, a crystal step the engine does not answer, and free liquid water below
     * its melting point while ice is not modelled.
     */
    public static final class UnsupportedPhases extends ThermoHold {
        /** What the network cannot carry. */
        public enum Kind {
            LIQUID_LIQUID,
            /** A converged split whose product the stability re-check proved unstable ({@link EquilibriumResult#UNSTABLE_PRODUCT})
             * or left unresolved ({@link EquilibriumResult#UNRESOLVED_PRODUCT}): a third fluid phase indicated or not
             * excluded. */
            THREE_PHASES,
            /** The engine's typed {@link EquilibriumResult#CRYSTAL_PHASES} hold under the crystal competition (two liquids
             * beside the crystal, three fluid phases beside it, two crystals); the engine's reason is
             * {@code NOT_IMPLEMENTED}. */
            CRYSTAL_PHASES,
            /** The engine's typed {@link EquilibriumResult#CRYSTAL_STEP} hold of a vessel refresh's crystal answer (D21 option
             * C): a near-pure feed's crystal step the pure species' step does not answer; another attempt at the same
             * material meets the same step, so the island holds at once. Also an answer that froze a vessel's other species
             * with its crystals ({@code VesselCrystals}: their traces leave a fluid, D21), which the refresh names the vessel
             * with. */
            CRYSTAL_STEP,
            /** Free liquid water below the water model's triple point, 273.16 K: the liquid would freeze, and ice is not
             * modelled yet (an interim capability limit of the range-extrapolation batch, until its ice crystal exists).
             * Water vapour below it is carried: a humid gas continues. */
            ICE;
            /** The equilibrium service's reason for this kind. */
            EquilibriumResult.UnsupportedReason reason() {
                return this==CRYSTAL_PHASES||this==CRYSTAL_STEP||this==ICE?EquilibriumResult.UnsupportedReason.NOT_IMPLEMENTED
                        :EquilibriumResult.UnsupportedReason.PHASE_COMPETITION_NOT_QUALIFIED;
            }
        }
        private final Kind kind;
        public UnsupportedPhases(String packageId,double temperature,double pressure,Kind kind,String detail) {
            this(packageId,temperature,pressure,kind,detail,NO_NODE);
        }
        private UnsupportedPhases(String packageId,double temperature,double pressure,Kind kind,String detail,long node) {
            super("Unsupported phases: "+kind.reason()+" ("+kind+") at "+temperature+" K, "+pressure+" Pa (package "+packageId+"): "+detail,
                    packageId,temperature,pressure,detail,node);
            this.kind=Objects.requireNonNull(kind);
        }
        /** What the network cannot carry. */
        public Kind kind(){return kind;}
        @Override public UnsupportedPhases at(long node) {
            return node()!=NO_NODE||node==NO_NODE?this:new UnsupportedPhases(packageId,temperature,pressure,kind,detail,node);
        }
        @Override public String reasonKey(){return REASON_PREFIX+"phases "+kind;}
        @Override public boolean sameAs(ThermoHold other) {
            return other instanceof UnsupportedPhases phases&&phases.kind==kind&&phases.node()==node()&&phases.packageId.equals(packageId);
        }
        @Override String what() {
            return switch(kind) {
                case LIQUID_LIQUID->"two hydrocarbon liquids, which the network does not carry";
                case THREE_PHASES->"a third fluid phase, which the network does not carry";
                case CRYSTAL_PHASES->"a crystal beside a phase set the equilibrium engine does not implement";
                case CRYSTAL_STEP->"a near-pure crystal step the equilibrium engine does not answer";
                case ICE->"free water below its melting point: ice not modelled yet";
            };
        }
    }
    private static double sum(double[] values) {double total=0;for(double value:values){if(value<0||!Double.isFinite(value))throw new IllegalArgumentException("Invalid phase amount");total+=value;}return total;}

    /** The temperature-only property bundle for one exact node temperature. */
    public Prepared prepare(double temperature) { return new Prepared(this,temperature); }

    private Prepared own(Prepared prepared) {
        if(prepared.owner!=this)throw new IllegalArgumentException("Prepared temperature belongs to another model");
        return prepared;
    }
    private Prepared match(Prepared prepared,double t) {
        if(prepared!=null&&own(prepared).temperature!=t)throw new IllegalArgumentException("Prepared temperature belongs to another state");
        return prepared;
    }
    /**
     * Everything at one exact temperature that neither composition nor pressure can change: the PR
     * mixing terms, the temperature terms of IF97 Region 1 (in its workspace, which also evaluates the
     * pressure terms of each trial without allocating), the water saturation pressure and the ideal-gas
     * water-vapor enthalpy.
     *
     * <p>A colored Jacobian perturbs a node's composition and pressure columns dozens of times at an
     * unchanged temperature, and every residual at that temperature repeats them, so this is where
     * those evaluations meet. Each member is computed when it is first asked for: a state evaluates
     * only the properties of the phases it holds.</p>
     */
    public static final class Prepared {
        private final FluidThermodynamics owner;
        private final double temperature;
        private TranslatedPengRobinson.Workspace terms;
        private WaterRegion1.Workspace water;
        private MixtureViscosity.Prepared transport;
        private double saturationPressure=Double.NaN,vaporEnthalpy=Double.NaN;
        private Prepared(FluidThermodynamics owner,double temperature) {
            if(!Double.isFinite(temperature)||temperature<=0)throw new IllegalArgumentException("Invalid prepared temperature");
            this.owner=owner;this.temperature=temperature;
        }
        public double temperature() { return temperature; }
        /** The prepared Peng-Robinson workspace of this temperature. It is mutable scratch, so a
         * {@code Prepared} - like the node cache that holds it - belongs to one solving thread. */
        TranslatedPengRobinson.Workspace pengRobinson() {
            return terms==null?terms=owner.hydrocarbon.prepare(temperature):terms;
        }
        /** This temperature's Region 1 workspace: the temperature terms once, the pressure terms per trial. */
        WaterRegion1.Workspace water() {
            return water==null?water=new WaterRegion1.Workspace():water;
        }
        double saturationPressure() {
            return Double.isNaN(saturationPressure)?saturationPressure=owner.saturationPressure(temperature):saturationPressure;
        }
        double vaporEnthalpy() {
            return Double.isNaN(vaporEnthalpy)?vaporEnthalpy=owner.vaporWaterEnthalpy(temperature):vaporEnthalpy;
        }
        /** The pure-component transport terms of this temperature, for the decode's Transport row. */
        public MixtureViscosity.Prepared viscosities() {
            return transport==null?transport=owner.viscosity.prepare(temperature):transport;
        }
    }
    public record WaterLiquid(double molarVolume,double molarEnthalpy) {}
    /**
     * The phase amounts belong to the record from construction on. {@link #state} is the only
     * place one is built: it copies a caller's arrays, and {@link #adoptingState} is the entry for
     * a caller that allocated them for this call alone, so a state still never shares an array with
     * anything a caller can reach. {@code waterCompressibility} is the free liquid water's own isothermal
     * compressibility at the state (Region 1's {@code -(dv/dP)/v}, 1/Pa), zero without free water.
     */
    public record State(double temperature,double pressure,double[] liquid,double[] vapor,double waterLiquid,double waterVapor,
                        double hydrocarbonPartialPressure,double waterPartialPressure,double volume,double enthalpy,double internalEnergy,
                        double mass,double liquidVolume,double waterVolume,double vaporVolume,HydrocarbonModel.Phase liquidProperties,
                        HydrocarbonModel.Phase vaporProperties, SolidInventory solids, SolidInventory.Moments solidMoments,
                        double waterCompressibility) {
        public State { Objects.requireNonNull(solids); Objects.requireNonNull(solidMoments); }
        public State withSolids(SolidInventory inventory) { return withSolidState(inventory,inventory.moments()); }
        /** Moment-only trial states never become owned inventories before population reconstruction. */
        public State withSolidState(SolidInventory inventory,SolidInventory.Moments moments) {
            return new State(temperature,pressure,liquid,vapor,waterLiquid,waterVapor,hydrocarbonPartialPressure,waterPartialPressure,
                    volume-solidMoments.volume()+moments.volume(),
                    enthalpy-solidMoments.enthalpy(temperature,pressure)+moments.enthalpy(temperature,pressure),
                    internalEnergy-solidMoments.internalEnergy(temperature)+moments.internalEnergy(temperature),
                    mass-solidMoments.mass()+moments.mass(),liquidVolume,waterVolume,vaporVolume,liquidProperties,vaporProperties,inventory,moments,
                    waterCompressibility);
        }
        /** Conserved component basis length, with water in the final position. */
        public int componentCount(){return liquid.length+1;}
        @Override public double[] liquid(){return liquid.clone();}
        @Override public double[] vapor(){return vapor.clone();}
        /** The amounts themselves, for the solver packages. The caller must not mutate them. */
        public double[] liquidView(){return liquid;}
        public double[] vaporView(){return vapor;}
    }
}
