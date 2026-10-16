package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import com.wormzjl.createcheme.science.column.v3.thermo.V3WaterProperties;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.IdealGasFunction;
import com.wormzjl.createcheme.science.thermo.phase.CrystalPhaseEvaluator;
import com.wormzjl.createcheme.science.thermo.phase.CubicPhaseEvaluator;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumRequest;
import com.wormzjl.createcheme.science.thermo.phase.EquilibriumResult;
import com.wormzjl.createcheme.science.thermo.phase.FluidTpEquilibrium;
import com.wormzjl.createcheme.science.thermo.phase.FreeWaterIce;
import com.wormzjl.createcheme.science.thermo.phase.FreeWaterModel;
import com.wormzjl.createcheme.science.thermo.phase.PhaseCompetition;
import com.wormzjl.createcheme.science.thermo.phase.PhaseContract;
import java.util.ArrayList;
import java.util.List;

/**
 * The equilibrium engine of one network model: {@link FluidTpEquilibrium} on the network package's contract, with the
 * network's own free water, as the one service that decides a node's phases ({@link FluidThermodynamics#equilibriumTP}:
 * the outer active-set check, the inventory refresh, placement and boundary charges), and of a pipe junction's own split
 * where the outer check adds a phase ({@link #ph}, D26).
 *
 * <p>The evaluator carries the network's exact equation of state: on a package with a reference spine
 * {@link CubicPhaseEvaluator#forPackage} (the same {@link SpinePackageData} as {@link HydrocarbonModel}); on a package
 * without one the package's PR78 constants and constant interactions with {@link HydrocarbonModel}'s own translations
 * (they enter the vapour branch's shift and the gas volume of the free-water rule, so a translation-free evaluator would
 * put the partial pressure elsewhere). Volume and {@code ln phi} are then bit for bit the network's. The ideal-gas
 * functions of a spine-less package are its {@code shifted_polynomial_5} fits on the network's sensible datum (zero
 * enthalpy and entropy at 298.15 K and 1e5 Pa, the network's own ideal-gas enthalpy): the engine needs them for the
 * states it reports, which the network does not read, and for a junction's PH seed (its own rows then solve it); TP phase
 * decisions are the cubic's and the water rule's.</p>
 *
 * <p>The engine's workspaces are per-thread scratch ({@link FluidTpEquilibrium.Workspace}), kept in a thread-local of
 * this instance so a model shared by several island workers never shares one; the engine carries nothing between calls
 * but prepared temperatures, recomputed bit for bit.</p>
 */
final class NetworkPhaseEngine {
    /** The standard entropy of water vapour at 298.15 K and 1e5 Pa, J/(mol K) (JANAF): the steam's entropy anchor. */
    static final double STEAM_STANDARD_ENTROPY=188.835;
    private final FluidTpEquilibrium engine;
    private final List<String> species;
    private final ThreadLocal<FluidTpEquilibrium.Workspace> workspaces;

    private NetworkPhaseEngine(FluidTpEquilibrium engine) {
        this.engine=engine;species=engine.contract().components();
        workspaces=ThreadLocal.withInitial(engine::newWorkspace);
    }

    /**
     * The engine of {@code fluid}'s package with {@code settings} ({@link FluidTpEquilibrium.Settings#DEFAULT} for the
     * network). A package that admits crystals gets its whole crystal set qualified ({@link PhaseContract#forPackage},
     * D8/D17) and one evaluator per crystal; every other package keeps the fluid-only network contract.
     */
    static NetworkPhaseEngine of(FluidThermodynamics fluid,MaterialCatalog catalog,String packageId,MaterialCatalog.Water water,
                                 FluidTpEquilibrium.Settings settings) {
        var propertyPackage=catalog.requirePackage(packageId);
        CubicPhaseEvaluator evaluator;
        if(!propertyPackage.spine().isEmpty())evaluator=CubicPhaseEvaluator.forPackage(catalog,packageId);
        else {
            var translated=fluid.hydrocarbon.translated();
            int n=propertyPackage.components().size();double[] translations=new double[n];
            List<IdealGasFunction> gas=new ArrayList<>(n);
            for(int i=0;i<n;i++) {
                translations[i]=translated.translation(i);
                var property=propertyPackage.properties().get(i);
                var range=catalog.fluidValidity(packageId,propertyPackage.components().get(i));
                gas.add(new CoefficientIdealGas(property,range.map(MaterialCatalog.Validity::minimumTemperature).orElse(property.minimumTemperature()),
                        range.map(MaterialCatalog.Validity::maximumTemperature).orElse(property.maximumTemperature())));
            }
            evaluator=CubicPhaseEvaluator.fromPackage(propertyPackage,propertyPackage.components(),translations,gas);
        }
        var crystals=fluid.crystals();
        if(!crystals.isEmpty()) {
            var contract=PhaseContract.forPackage(catalog,packageId);
            var competition=contract.crystalCompetition();
            if(competition==null||!competition.crystals().equals(new java.util.TreeSet<>(crystals)))
                throw new IllegalStateException("Package "+packageId+": the network's crystals "+crystals+" are not the contract's competition "+competition);
            List<CrystalPhaseEvaluator> solids=new ArrayList<>();
            for(String id:crystals)solids.add(CrystalPhaseEvaluator.forPackage(catalog,packageId,id));
            return new NetworkPhaseEngine(new FluidTpEquilibrium(contract,evaluator,settings,new NetworkFreeWater(fluid,water),solids));
        }
        var contract=PhaseContract.forNetworkPackage(catalog,packageId);
        return new NetworkPhaseEngine(new FluidTpEquilibrium(contract,evaluator,settings,new NetworkFreeWater(fluid,water)));
    }

    /** The TP equilibrium of {@code overall} (the network's conserved basis, water last) at {@code (t, p)}: under the
     * package's crystal competition when {@code crystals} ({@link PhaseCompetition#FLUID_AND_CRYSTALS}), fluid phases only
     * otherwise. */
    EquilibriumResult tp(double t,double p,double[] overall,boolean crystals) {
        return engine.tp(EquilibriumRequest.tp(t,p,species,overall,crystals?PhaseCompetition.FLUID_AND_CRYSTALS:PhaseCompetition.FLUID_ONLY),workspaces.get());
    }
    /** The PH equilibrium of {@code overall} at {@code p} with enthalpy {@code h} (J, on the engine's datum), under the
     * competition {@code crystals} selects as for {@link #tp}: a pipe junction's own split (D26). */
    EquilibriumResult ph(double p,double h,double[] overall,boolean crystals) {
        return engine.ph(EquilibriumRequest.ph(p,h,species,overall,crystals?PhaseCompetition.FLUID_AND_CRYSTALS:PhaseCompetition.FLUID_ONLY),workspaces.get());
    }
    /**
     * The equilibrium of {@code overall} at internal energy {@code u} (J, the engine's datum) and volume {@code v} (m3), the
     * package's crystals competing where {@code crystals} (as for {@link #tp}) and the free water's ice by water's own rule
     * (WP4): the crystal test of a taut vessel's refresh ({@code InventoryEquilibrium}, D23), D21's near-pure step included.
     * Warm-started at {@code (hintT, hintP)} when both are finite
     * ({@link FluidTpEquilibrium#uv(EquilibriumRequest, com.wormzjl.createcheme.science.thermo.phase.EquilibriumService.Workspace, double, double)}).
     */
    EquilibriumResult crystalUv(double u,double v,double[] overall,boolean crystals,double hintT,double hintP) {
        var request=EquilibriumRequest.uv(u,v,species,overall,crystals?PhaseCompetition.FLUID_AND_CRYSTALS:PhaseCompetition.FLUID_ONLY);
        return Double.isFinite(hintT)&&Double.isFinite(hintP)?engine.uv(request,workspaces.get(),hintT,hintP):engine.uv(request,workspaces.get());
    }
    /** The engine itself: the crystal evaluators and the onset functions the network's crystal records are built from. */
    FluidTpEquilibrium engine(){return engine;}
    /** The engine's basis: the network's conserved components, water last. */
    List<String> species(){return species;}

    /**
     * The network's free water as the engine's {@link FreeWaterModel}: the very methods {@link FluidThermodynamics#state}
     * uses (its saturation pressure, its liquid water at the state pressure, its steam partial-pressure limit), its
     * ice below the melting line ({@link FluidThermodynamics#freeWaterIce}, WP4) and its
     * ideal steam ({@link Steam}).
     */
    static final class NetworkFreeWater implements FreeWaterModel {
        private final FluidThermodynamics fluid;
        private final MaterialCatalog.Water water;
        NetworkFreeWater(FluidThermodynamics fluid,MaterialCatalog.Water water) {
            this.fluid=fluid;this.water=water;
        }
        @Override public String revision(){return water.revision();}
        @Override public double saturationPressure(double t){return fluid.saturationPressure(t);}
        @Override public double vaporPartialPressureLimit(double t){return FluidThermodynamics.waterVaporPressureLimit(t);}
        @Override public void liquid(double t,double p,double[] out) {
            var liquid=fluid.waterLiquid(t,p);out[0]=liquid.molarVolume();out[1]=liquid.molarEnthalpy();
        }
        @Override public IdealGasFunction vapor(){return fluid.steam();}
        @Override public FreeWaterIce ice(){return fluid.freeWaterIce();}
    }

    /**
     * The network's ideal steam: the water record's Shomate polynomial (the column's {@code vaporMolarEnthalpy}, the
     * network's {@code vaporWaterEnthalpy}), its entropy anchored at {@link #STEAM_STANDARD_ENTROPY}. Past the record's
     * range, from its triple point to its maximum temperature, the heat capacity is held at the end value and the enthalpy
     * and entropy integrate it (owner rule of 2026-10-16), so value and slope are continuous at both ends.
     */
    static final class Steam implements IdealGasFunction {
        private final MaterialCatalog.Water water;
        private final double minimum,maximum,offset,lowCp,highCp,lowEnthalpy,highEnthalpy,lowEntropy,highEntropy;
        Steam(MaterialCatalog.Water water) {
            this.water=water;minimum=water.triplePoint();maximum=water.maximumTemperature();
            offset=STEAM_STANDARD_ENTROPY-shomateEntropy(298.15);
            lowCp=V3WaterProperties.dVaporMolarEnthalpyDT(water,minimum);highCp=V3WaterProperties.dVaporMolarEnthalpyDT(water,maximum);
            lowEnthalpy=V3WaterProperties.vaporMolarEnthalpy(water,minimum);highEnthalpy=V3WaterProperties.vaporMolarEnthalpy(water,maximum);
            lowEntropy=shomateEntropy(minimum)+offset;highEntropy=shomateEntropy(maximum)+offset;
        }
        private double shomateEntropy(double t) {
            double x=t/1000.0;var c=water.shomate();
            return c.get(0)*DeterministicMath.log(x)+c.get(1)*x+c.get(2)*x*x/2.0+c.get(3)*x*x*x/3.0-c.get(4)/(2.0*x*x);
        }
        private static void require(double t) {
            if(!(t>0)||!Double.isFinite(t))throw new IllegalArgumentException("Steam temperature must be positive and finite: "+t);
        }
        @Override public double heatCapacity(double t) {
            require(t);return t<minimum?lowCp:t>maximum?highCp:V3WaterProperties.dVaporMolarEnthalpyDT(water,t);
        }
        @Override public double enthalpy(double t) {
            require(t);
            return t<minimum?lowEnthalpy+lowCp*(t-minimum):t>maximum?highEnthalpy+highCp*(t-maximum):V3WaterProperties.vaporMolarEnthalpy(water,t);
        }
        @Override public double entropy(double t,double p) {
            require(t);if(!(p>0)||!Double.isFinite(p))throw new IllegalArgumentException("Steam entropy needs a positive finite pressure");
            double s=t<minimum?lowEntropy+lowCp*DeterministicMath.log(t/minimum):t>maximum?highEntropy+highCp*DeterministicMath.log(t/maximum):shomateEntropy(t)+offset;
            return s-FluidThermodynamics.R*DeterministicMath.log(p/1.0e5);
        }
        @Override public double minimumTemperature(){return minimum;}
        @Override public double maximumTemperature(){return maximum;}
        @Override public String revision(){return water.revision();}
    }

    /**
     * A {@code shifted_polynomial_5} ideal-gas fit (and its optional low-temperature segment) as an
     * {@link IdealGasFunction} on the network's sensible datum: {@code c_p = sum a_k (T - 298.15)^k}, the enthalpy its
     * integral from 298.15 K (the network's own ideal-gas enthalpy), the entropy {@code integral c_p/T dT} from 298.15 K
     * minus {@code R ln(P/1e5)}; below a segment's joint the segment continues both from their values there. Past the fit's
     * range {@code [minimum, maximum]} the heat capacity is held at the end value and the enthalpy and entropy integrate it
     * (owner rule of 2026-10-16), as {@link TranslatedPengRobinson}'s coefficient path does.
     */
    static final class CoefficientIdealGas implements IdealGasFunction {
        private static final double REFERENCE=298.15;
        private final double[] main,low,mainPowers,lowPowers;
        private final double joint,jointEnthalpy,jointEntropy,minimum,maximum;
        private final String revision;
        CoefficientIdealGas(MaterialCatalog.Property property,double minimum,double maximum) {
            main=property.cp().stream().mapToDouble(Double::doubleValue).toArray();
            var segment=property.lowTemperatureCp();
            low=segment==null?null:segment.coefficients().stream().mapToDouble(Double::doubleValue).toArray();
            joint=segment==null?Double.NEGATIVE_INFINITY:segment.belowKelvin();
            mainPowers=powers(main);lowPowers=low==null?null:powers(low);
            jointEnthalpy=low==null?0:antiderivative(main,joint)-antiderivative(low,joint);
            jointEntropy=low==null?0:entropyIntegral(mainPowers,joint)-entropyIntegral(lowPowers,joint);
            this.minimum=minimum;this.maximum=maximum;revision=property.id()+":shifted_polynomial_5";
        }
        @Override public double heatCapacity(double t) {
            return fitHeatCapacity(edge(t));
        }
        @Override public double enthalpy(double t) {
            double e=edge(t),h=e<joint?antiderivative(low,e)+jointEnthalpy:antiderivative(main,e);
            return e==t?h:h+fitHeatCapacity(e)*(t-e);
        }
        @Override public double entropy(double t,double p) {
            double e=edge(t);if(!(p>0))throw new IllegalArgumentException("Ideal-gas entropy needs a positive pressure");
            double integral=e<joint?entropyIntegral(lowPowers,e)+jointEntropy:entropyIntegral(mainPowers,e);
            if(e!=t)integral+=fitHeatCapacity(e)*DeterministicMath.log(t/e);
            return integral-FluidThermodynamics.R*DeterministicMath.log(p/1.0e5);
        }
        @Override public double minimumTemperature(){return minimum;}
        @Override public double maximumTemperature(){return maximum;}
        @Override public String revision(){return revision;}
        private double fitHeatCapacity(double t) {
            double[] a=t<joint?low:main;double delta=t-REFERENCE,cp=0;
            for(int k=a.length-1;k>=0;k--)cp=cp*delta+a[k];
            return cp;
        }
        /** {@code t} inside the fit's range, the nearer end outside it; a temperature that is not positive is refused. */
        private double edge(double t) {
            if(!(t>0)||!Double.isFinite(t))throw new IllegalArgumentException("Ideal-gas temperature must be positive and finite: "+t);
            return t<minimum?minimum:t>maximum?maximum:t;
        }
        /** {@code integral_298.15^T c_p dT}, the Horner form the translated model uses. */
        private static double antiderivative(double[] a,double t) {
            double delta=t-REFERENCE,h=0;
            for(int k=a.length-1;k>=0;k--)h=h*delta+a[k]/(k+1);
            return h*delta;
        }
        /** The same polynomial in powers of {@code T}: {@code sum_m p_m T^m}. */
        private static double[] powers(double[] a) {
            double[] p=new double[a.length];
            for(int k=0;k<a.length;k++) {
                double binomial=1;
                for(int m=0;m<=k;m++) {
                    p[m]+=a[k]*binomial*DeterministicMath.pow(-REFERENCE,k-m);
                    binomial=binomial*(k-m)/(m+1);
                }
            }
            return p;
        }
        /** {@code integral_298.15^T c_p/T dT} of the polynomial in powers of {@code T}. */
        private static double entropyIntegral(double[] p,double t) {
            double integral=p[0]*DeterministicMath.log(t/REFERENCE);
            double tm=1,rm=1;
            for(int m=1;m<p.length;m++){tm*=t;rm*=REFERENCE;integral+=p[m]*(tm-rm)/m;}
            return integral;
        }
    }
}
