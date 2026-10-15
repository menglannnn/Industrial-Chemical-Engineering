package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.science.math.DeterministicMath;

import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import com.wormzjl.createcheme.science.thermo.ThermoComponent;
import java.util.List;

/** Fluid-specific property snapshot. Does not replace or modify the column's property model. */
public final class HydrocarbonModel {
    /** The pressure the liquid equation of state is evaluated at before the global compressibility response carries
     * it to the state's own; a formulation constant, not a domain bound. */
    public static final double REFERENCE_PRESSURE = 2e6;
    /** A numerical guard on the hydrocarbon <em>partial</em> pressure a vapour is evaluated at, which in a gas that is
     * nearly all steam can be arbitrarily small; the state's own pressure is bounded by the package's domain. */
    public static final double VAPOR_PARTIAL_PRESSURE_FLOOR = 1e-6;
    private final MaterialCatalog.Package propertyPackage;
    private final TranslatedPengRobinson translated;
    private final GlobalLiquidResponse liquidResponse;
    private final String revision;
    /** Every temperature and pressure range comes from the package's data; see {@link FluidDomain}. */
    private final FluidDomain domain;

    public HydrocarbonModel(MaterialCatalog catalog,String packageId,double compressibility) {
        propertyPackage=catalog.requirePackage(packageId);
        liquidResponse=new GlobalLiquidResponse(compressibility);
        domain=FluidDomain.of(catalog,packageId);
        List<ThermoComponent> components=propertyPackage.properties().stream().map(p->new ThermoComponent(
                p.component(),p.pr().criticalTemperature(),p.pr().criticalPressure(),p.pr().acentricFactor(),p.molecularWeight())).toList();
        int count=components.size();
        double[][] interactions=new double[count][count], cp=new double[count][6], low=new double[count][];
        double[] joints=new double[count];
        for(int i=0;i<count;i++) {
            for(int j=0;j<count;j++) interactions[i][j]=propertyPackage.interactions().get(i).get(j);
            var property=propertyPackage.properties().get(i);
            for(int j=0;j<6;j++) cp[i][j]=property.cp().get(j);
            var segment=property.lowTemperatureCp();
            joints[i]=segment==null?Double.NEGATIVE_INFINITY:segment.belowKelvin();
            if(segment!=null){low[i]=new double[6];for(int j=0;j<6;j++)low[i][j]=segment.coefficients().get(j);}
        }
        var calibrations=catalog.fluidData().volumeReferences();
        double[] shifts=new double[count];
        for(int i=0;i<count;i++) {
            var property=propertyPackage.properties().get(i);
            var point=calibrations.get(property.component());
            double t=point==null?property.standardTemperature():point.temperatureKelvin();
            double targetVolume=point==null?property.molecularWeight()/property.density():point.molarVolumeCubicMetres();
            double referenceP=point==null?property.standardPressure():point.pressurePascal();
            double rawVolume=pureLiquidCompressibility(components.get(i),interactions[i][i],t,REFERENCE_PRESSURE)
                    *FluidThermodynamics.R*t/REFERENCE_PRESSURE;
            shifts[i]=targetVolume*DeterministicMath.exp(compressibility*(referenceP-REFERENCE_PRESSURE))-rawVolume;
        }
        translated=new TranslatedPengRobinson(components,interactions,cp,shifts,joints,low);
        revision=catalog.fluidThermoFingerprint(packageId)+":fluid-shared-k-v1:fluid-domain-data-v1:cp-segments-v1:catalog-volume-reference-v1:k="+Double.toHexString(compressibility);
    }

    public String revision() { return revision; }
    /** The package's fluid domain: the envelope and every component's range. */
    public FluidDomain domain() { return domain; }
    public int componentCount() { return propertyPackage.components().size(); }
    public List<String> components() { return propertyPackage.components(); }
    public double molecularWeight(int i) { return propertyPackage.properties().get(i).molecularWeight(); }
    public MaterialCatalog.Package propertyPackage() { return propertyPackage; }

    public Phase phase(double t,double p,double[] amounts,PengRobinsonKernel.Root root) {
        return phase(t,p,amounts,root,translated.prepare(t));
    }
    public TranslatedPengRobinson.Workspace prepare(double t){return translated.prepare(t);}
    /** The translated EOS this model evaluates; the package's derivative test differentiates against it. */
    TranslatedPengRobinson translated(){return translated;}
    /** Caller-owned derivative storage for an analytic node Jacobian; one per solve, refilled per node. */
    public TranslatedPengRobinson.Derivatives newDerivatives(){return translated.newDerivatives();}
    /**
     * Every first derivative of one hydrocarbon phase, filled into {@code output} without allocating.
     *
     * <p>This is the equation of state at the pressure given, which is what {@link #phase} passes it for a
     * vapor. For a liquid {@link #phase} evaluates at {@link #REFERENCE_PRESSURE} and then applies
     * {@link GlobalLiquidResponse}; a caller assembling a liquid node block must differentiate that
     * correction itself, because nothing here applies it.</p>
     */
    public void differentiate(double t,double p,double[] amounts,PengRobinsonKernel.Root root,
                              TranslatedPengRobinson.Workspace terms,TranslatedPengRobinson.Derivatives output) {
        if(amounts.length!=componentCount())throw new IllegalArgumentException("Hydrocarbon basis mismatch");
        translated.differentiate(t,p,amounts,root,terms,output);
    }
    /**
     * The log fugacity coefficients {@link #phase} would return, written into {@code out} with the same arithmetic and
     * the same domain checks, and the vapor-branch flag; no {@link Phase} record and no coefficient copies. For the
     * flash's equilibrium iteration.
     */
    public boolean logFugacityInto(double t,double p,double[] amounts,PengRobinsonKernel.Root root,TranslatedPengRobinson.Workspace terms,double[] out) {
        checkPhase(t,p,amounts,root);
        if(root==PengRobinsonKernel.Root.VAPOR) {
            var gas=translated.evaluateValues(t,p,amounts,root,terms);
            System.arraycopy(gas.logFugacityCoefficientsView(),0,out,0,out.length);
            return gas.vaporBranch();
        }
        var reference=translated.evaluateValues(t,REFERENCE_PRESSURE,amounts,PengRobinsonKernel.Root.LIQUID,terms);
        var liquid=liquidResponse.evaluate(t,p,REFERENCE_PRESSURE,reference.molarVolume(),
                reference.volumeTemperatureDerivative(),reference.volumeSecondTemperatureDerivative(),
                reference.molarEnthalpy(),reference.heatCapacity());
        liquidResponse.logFugacityInto(t,p,REFERENCE_PRESSURE,liquid.pressureIntegral(),
                reference.logFugacityCoefficientsView(),reference.partialMolarVolumesView(),out);
        return false;
    }
    /**
     * The checks of {@link #phase} and {@link #logFugacityInto}: the basis, a finite state, and every carried component within
     * its own temperature range (a thermo-domain violation otherwise). A liquid is evaluated at the state pressure, which
     * must lie in the range of every component it carries; a vapour at its hydrocarbon partial pressure, which is only
     * guarded numerically here and capped by the envelope, since the state that carries it checks the total pressure.
     */
    private void checkPhase(double t,double p,double[] amounts,PengRobinsonKernel.Root root) {
        if(amounts.length!=componentCount())throw new IllegalArgumentException("Hydrocarbon basis mismatch");
        if(!Double.isFinite(t)||!Double.isFinite(p))throw new IllegalArgumentException("Fluid hydrocarbon state is not finite");
        domain.checkPhaseTemperature(t,amounts);
        if(root==PengRobinsonKernel.Root.VAPOR) {
            if(p<VAPOR_PARTIAL_PRESSURE_FLOOR)throw new IllegalArgumentException("Hydrocarbon vapour partial pressure below the numerical floor");
            if(p>domain.envelope().maximumPressure())domain.checkEnvelope(t,p);
        } else domain.check(t,p,amounts,null,0);
    }
    public Phase phase(double t,double p,double[] amounts,PengRobinsonKernel.Root root,TranslatedPengRobinson.Workspace terms) {
        checkPhase(t,p,amounts,root);
        // The same numbers from the workspace's own buffer, copying only the coefficients the returned record keeps (the
        // vapor's ln phi; the liquid's is the response's own array).
        if(root==PengRobinsonKernel.Root.VAPOR) {
            var gas=translated.evaluateValues(t,p,amounts,root,terms);
            return new Phase(gas.molarVolume(),gas.molarEnthalpy(),gas.molarInternalEnergy(),
                    gas.volumePressureDerivative(),gas.logFugacityCoefficientsView().clone(),gas.vaporBranch());
        }
        var reference=translated.evaluateValues(t,REFERENCE_PRESSURE,amounts,PengRobinsonKernel.Root.LIQUID,terms);
        var liquid=liquidResponse.evaluate(t,p,REFERENCE_PRESSURE,reference.molarVolume(),
                reference.volumeTemperatureDerivative(),reference.volumeSecondTemperatureDerivative(),
                reference.molarEnthalpy(),reference.heatCapacity());
        double[] phi=liquidResponse.logFugacity(t,p,REFERENCE_PRESSURE,liquid.pressureIntegral(),
                reference.logFugacityCoefficientsView(),reference.partialMolarVolumesView());
        return new Phase(liquid.molarVolume(),liquid.molarEnthalpy(),liquid.molarInternalEnergy(),
                liquid.volumePressureDerivative(),phi,false);
    }

    /** The coefficients belong to the record from construction on. {@link #phase} is the only place
     * one is built, from an array allocated for it: the liquid response's result, or the vapor
     * evaluation's own, whose enclosing record it discards on the same line. */
    public record Phase(double molarVolume,double molarEnthalpy,double molarInternalEnergy,
                        double volumePressureDerivative,double[] logFugacity,boolean vaporBranch) {
        @Override public double[] logFugacity() { return logFugacity.clone(); }
        /** The coefficients themselves, for the solver packages. The caller must not mutate them. */
        public double[] logFugacityView() { return logFugacity; }
    }
    /**
     * The liquid-root compressibility factor of pure component {@code c} at {@code t} and {@code p} on the Peng-Robinson
     * 1978 cubic with classical quadratic mixing, written operation for operation as the reference {@code PengRobinson78}
     * (test tree, pinned by {@code PengRobinson78Test}) evaluates a pure composition: for one component the mixing rules
     * give {@code a_mix = sqrt(a a) (1 - k_cc)} and {@code b_mix = b} exactly, so the result is the reference's bit for bit.
     */
    private static double pureLiquidCompressibility(ThermoComponent c,double kcc,double t,double p) {
        if(!Double.isFinite(t)||t<=0.0||!Double.isFinite(p)||p<=0.0)throw new IllegalArgumentException("Temperature and pressure must be finite and positive");
        double r=FluidThermodynamics.R,tc=c.criticalTemperatureKelvin(),pc=c.criticalPressurePascal(),omega=c.acentricFactor();
        double criticalA=0.45724*square(r)*square(tc)/pc;
        double coVolume=0.07780*r*tc/pc;
        double kappa=omega<=0.491?0.37464+1.54226*omega-0.26992*square(omega)
                :0.379642+1.48503*omega-0.164423*square(omega)+0.016666*omega*square(omega);
        double sqrtReducedTemperature=Math.sqrt(t/tc);
        double alphaTerm=1.0+kappa*(1.0-sqrtReducedTemperature);
        double a=criticalA*square(alphaTerm);
        double aMix=Math.sqrt(a*a)*(1.0-kcc),bMix=coVolume;
        double reducedA=aMix*p/(square(r)*square(t));
        double reducedB=bMix*p/(r*t);
        if(Math.abs(reducedA)<1.0e-14&&Math.abs(reducedB)<1.0e-14)return 1.0;
        double c2=-(1.0-reducedB);
        double c1=reducedA-3.0*square(reducedB)-2.0*reducedB;
        double c0=-(reducedA*reducedB-square(reducedB)-reducedB*square(reducedB));
        double[] roots=realCubicRoots(c2,c1,c0);
        java.util.Arrays.sort(roots);
        for(double root:roots)if(root>reducedB+1.0e-12)return root;
        throw new IllegalStateException("Peng-Robinson equation has no physical liquid root");
    }
    /** The real roots of z^3 + c2 z^2 + c1 z + c0 = 0, as {@code PengRobinson78} finds them. */
    private static double[] realCubicRoots(double c2,double c1,double c0) {
        double p=c1-square(c2)/3.0;
        double q=2.0*c2*square(c2)/27.0-c2*c1/3.0+c0;
        double discriminant=square(q)/4.0+p*p*p/27.0;
        double offset=c2/3.0;
        if(discriminant>1.0e-16) {
            double root=Math.sqrt(discriminant);
            return new double[]{DeterministicMath.cbrt(-q/2.0+root)+DeterministicMath.cbrt(-q/2.0-root)-offset};
        }
        if(Math.abs(discriminant)<=1.0e-16) {
            double u=DeterministicMath.cbrt(-q/2.0);
            return new double[]{2.0*u-offset,-u-offset};
        }
        double radius=2.0*Math.sqrt(-p/3.0);
        double angle=DeterministicMath.acos(Math.clamp((3.0*q/(2.0*p))*Math.sqrt(-3.0/p),-1.0,1.0))/3.0;
        return new double[]{radius*DeterministicMath.cos(angle)-offset,radius*DeterministicMath.cos(angle-2.0*Math.PI/3.0)-offset,
                radius*DeterministicMath.cos(angle-4.0*Math.PI/3.0)-offset};
    }
    private static double square(double value){return value*value;}
}
