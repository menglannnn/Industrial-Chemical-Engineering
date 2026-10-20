package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import com.wormzjl.createcheme.fluid.support.LegacyLiquidPath;
import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.science.fluid.network.InventoryEquilibrium;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.createcheme.science.thermo.reference.HelmholtzFluid;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Continuity of the direct liquid path (P3 WP4, plan section 2.6): from the qualified region below 2 MPa into the new
 * one above it, nothing is special about 2 MPa any more; below 2 MPa the direct liquid differs from the retired
 * reference-pressure liquid only by what the old path approximated; and a closed vessel of liquid nitrogen compressed
 * through 2 MPa by the network's own UV solve follows the equation of state's adiabat.
 */
class DirectLiquidContinuityTest {
    private static final String NETWORK="createcheme:tjl20_methane_nitrogen";
    private final MaterialCatalog catalog=MaterialCatalog.bundled();
    private final FluidThermodynamics model=FluidThermodynamics.forNetwork(catalog,NETWORK);

    /**
     * Item 1: along isotherms from just above saturation to 10 MPa the liquid root exists (three roots, or one
     * liquid-like root) at every pressure, the density meets the D7 targets against the Helmholtz oracle (compressed
     * liquid 3 % up to Tr 0.85, the declared 10 % between 0.85 and 0.95), and the P1 study's direct-evaluation numbers
     * (anchor (a), the network's own anchors) are reproduced.
     */
    @Test void isothermsCrossTwoMegapascalsWithALiquidRootAndMeetTheTargets() {
        var hydrocarbon=model.hydrocarbon;var eos=hydrocarbon.translated();
        record Isotherms(String component,String fluid,double[] temperatures){}
        var cases=List.of(new Isotherms("Nitrogen","Nitrogen",new double[]{77,90,100,110}),
                new Isotherms("Methane","Methane",new double[]{114.34,133.39,152.45,171.51}),
                new Isotherms("Ethane","Ethane",new double[]{183.19,213.73,244.26,274.79}));
        var report=new StringBuilder();
        for(var c:cases) {
            var oracle=HelmholtzFluid.load(c.fluid());int index=hydrocarbon.components().indexOf(c.component());
            double[] pure=new double[hydrocarbon.componentCount()];pure[index]=1;double b=eos.kernel().coVolume(index);
            for(double t:c.temperatures()) {
                double tr=t/oracle.criticalTemperature(),target=tr<=.85?.03:.10,worst=0,start=Math.max(1e5,1.02*oracle.saturation(t).pressure());
                var terms=eos.prepare(t);
                for(double p=start;p<=1e7*(1+1e-12);p*=Math.pow(1e7/start,1.0/40)) {
                    var values=eos.evaluateValues(t,p,pure,Root.LIQUID,terms);
                    assertTrue(values.physicalRootCount()==3||HydrocarbonModel.liquidLike(values),c.component()+" liquid root at "+t+" K, "+p+" Pa");
                    double model=1/values.molarVolume(),reference=oracle.density(t,p,HelmholtzFluid.Root.LIQUID);
                    worst=Math.max(worst,Math.abs(model/reference-1));
                }
                report.append(String.format(Locale.ROOT,"%s %.2f K (Tr %.3f): worst |rho| deviation %.2f %% (target %.0f %%)%n",c.component(),t,tr,100*worst,100*target));
                assertTrue(worst<=target,c.component()+" at "+t+" K: "+worst);
            }
        }
        // The P1 study, section 2.5, anchor (a): the same translated PR78 evaluated directly (CoolProp 8.0.0 reference).
        double[][] p1={{77.36,5e6,-0.08},{77.36,10e6,-0.17},{100,10e6,0.34}};
        var nitrogen=HelmholtzFluid.load("Nitrogen");int n2=hydrocarbon.components().indexOf("Nitrogen");
        double[] pure=new double[hydrocarbon.componentCount()];pure[n2]=1;
        for(double[] row:p1) {
            double rho=1/eos.evaluate(row[0],row[1],pure,Root.LIQUID,eos.prepare(row[0])).molarVolume();
            double deviation=100*(rho/nitrogen.density(row[0],row[1],HelmholtzFluid.Root.LIQUID)-1);
            report.append(String.format(Locale.ROOT,"P1 check N2 %.2f K %.0f MPa: %+.3f %% (P1: %+.2f %%)%n",row[0],row[1]/1e6,deviation,row[2]));
            assertEquals(row[2],deviation,.02,"P1 direct evaluation at "+row[0]+" K, "+row[1]+" Pa");
        }
        System.out.print(report);
    }

    /**
     * Item 1, second half: second differences of v(P) and h(P) along each liquid isotherm vary smoothly through 2 MPa
     * (no jump), for the hydrocarbon liquid and for liquid water.
     */
    @Test void secondDifferencesShowNoJumpAtTwoMegapascals() {
        var hydrocarbon=model.hydrocarbon;var eos=hydrocarbon.translated();
        double[] crude=crudeLiquid();
        for(double t:new double[]{300,350,375}) {
            var terms=eos.prepare(t);
            smooth("crude liquid v at "+t+" K",p->eos.evaluate(t,p,crude,Root.LIQUID,terms).molarVolume());
            smooth("crude liquid h at "+t+" K",p->eos.evaluate(t,p,crude,Root.LIQUID,terms).molarEnthalpy());
            smooth("water v at "+t+" K",p->model.waterLiquid(t,p).molarVolume());
            smooth("water h at "+t+" K",p->model.waterLiquid(t,p).molarEnthalpy());
        }
        int n2=hydrocarbon.components().indexOf("Nitrogen");double[] pure=new double[hydrocarbon.componentCount()];pure[n2]=1;
        for(double t:new double[]{77,90,100}) {
            var terms=eos.prepare(t);
            smooth("N2 liquid v at "+t+" K",p->eos.evaluate(t,p,pure,Root.LIQUID,terms).molarVolume());
            smooth("N2 liquid h at "+t+" K",p->eos.evaluate(t,p,pure,Root.LIQUID,terms).molarEnthalpy());
        }
    }
    private static void smooth(String what,java.util.function.DoubleUnaryOperator f) {
        double h=5e4;int n=20;double[] y=new double[2*n+1];
        for(int j=-n;j<=n;j++)y[j+n]=f.applyAsDouble(2e6+j*h);
        double[] second=new double[y.length-2];double largest=0;
        for(int j=1;j<y.length-1;j++){second[j-1]=y[j+1]-2*y[j]+y[j-1];largest=Math.max(largest,Math.abs(second[j-1]));}
        for(int j=1;j<second.length;j++)
            assertTrue(Math.abs(second[j]-second[j-1])<=.02*largest,what+": second difference jumps at "+(2e6+(j-n)*h)+" Pa: "+second[j-1]+" -> "+second[j]);
    }
    private double[] crudeLiquid() {
        var feed=V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane").crudeFeed("createcheme:tia_juana_light_methane");
        var basis=feed.moleFractions();double[] x=new double[model.hydrocarbon.componentCount()];
        var crude=V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane");
        // The heavy part of the assay: the cuts, a liquid at every state of the grid.
        for(int c=0;c<basis.length;c++){String id=crude.componentBasis().componentId(c);if(id.startsWith("crude_"))x[model.hydrocarbon.components().indexOf(id)]=basis[c];}
        return x;
    }

    /**
     * Item 2: on the {@code FluidPropertyCoverageTest} grid (293.15-375 K x 50 kPa-2 MPa, the network package and the six
     * presets), the direct liquid's volume, enthalpy and {@code ln phi} differ from the retired path's by at most the
     * first-order bound: the anchor term (the change of the translations, {@code sum x_i dc_i} in volume, {@code P dc}
     * in enthalpy and {@code P dc_i/RT} in {@code ln phi} at 2 MPa, where both paths evaluate the same cubic) plus
     * {@code |P - 2 MPa|} times the largest difference of the pressure derivatives at the two ends
     * ({@code v kappa_EOS - v k} for volume, {@code dh/dP} for enthalpy, {@code vbar_i/RT} for {@code ln phi}).
     */
    @Test void belowTwoMegapascalsTheLiquidMovesOnlyByTheFirstOrderCorrection() {
        var summary=new StringBuilder();
        for(String packageId:List.of(NETWORK,"createcheme:tjl20_methane","createcheme:wti_light_export_tjl20","createcheme:cold_lake_blend_tjl20",
                "createcheme:upper_zakum_tjl20","createcheme:bonga_tjl20","createcheme:dalia_tjl20")) {
            var direct=packageId.equals(NETWORK)?model:FluidTestSupport.model(catalog,packageId);
            var legacy=new LegacyLiquidPath(catalog,packageId);var eos=direct.hydrocarbon.translated();
            String assayPackage=packageId.equals(NETWORK)?"createcheme:tjl20_methane":packageId;
            var assay=V3PengRobinsonThermo.fromRegisteredPackage(assayPackage);
            var feed=assay.crudeFeed(catalog.requirePackage(assayPackage).assays().keySet().iterator().next()).moleFractions();
            double[] n=new double[direct.componentCount()];
            for(int c=0;c<feed.length;c++)n[direct.components().indexOf(assay.componentBasis().componentId(c))]=feed[c];
            if(packageId.equals(NETWORK))n[direct.components().indexOf("Nitrogen")]=.1;
            n[n.length-1]=.2;
            double worstVolume=0,worstEnthalpy=0,worstPhi=0,largestVolume=0;int states=0;
            for(double t:new double[]{293.15,298.15,300,325,350,375})for(double p:new double[]{50000,101325,500000,1e6,2e6}) {
                var state=direct.flashTP(t,p,n,()->{});if(state.liquidVolume()==0)continue;
                double[] x=state.liquid();double total=0;for(double value:x)total+=value;for(int i=0;i<x.length;i++)x[i]/=total;
                var terms=eos.prepare(t);
                var now=eos.evaluateValues(t,p,x,Root.LIQUID,terms);
                double v=now.molarVolume(),h=now.molarEnthalpy(),kv=-now.volumePressureDerivative(),dh=now.enthalpyPressureDerivative();
                double[] phi=now.logFugacityCoefficientsView().clone(),vbar=now.partialMolarVolumesView().clone();
                var high=eos.evaluateValues(t,2e6,x,Root.LIQUID,terms);
                double kvHigh=-high.volumePressureDerivative(),dhHigh=high.enthalpyPressureDerivative();double[] vbarHigh=high.partialMolarVolumesView().clone();
                var old=legacy.liquid(t,p,x);var oldHigh=legacy.liquid(t,2e6,x);
                double shift=0;double[] dc=new double[x.length];
                for(int i=0;i<x.length;i++){dc[i]=legacy.directShift(i)-legacy.legacyShift(i);shift+=x[i]*dc[i];}
                double span=2e6-p,rt=FluidThermodynamics.R*t;
                double volumeBound=span*Math.max(Math.abs(kv+old.volumePressureDerivative()),Math.abs(kvHigh+oldHigh.volumePressureDerivative()))*1.05+1e-12*v;
                double volumeError=Math.abs(v-old.molarVolume()-shift);
                assertTrue(volumeError<=volumeBound,packageId+" "+t+" K "+p+" Pa volume: "+volumeError+" > "+volumeBound);
                double enthalpyBound=span*Math.max(Math.abs(dh-old.enthalpyPressureDerivative()),Math.abs(dhHigh-oldHigh.enthalpyPressureDerivative()))*1.05+1e-9*Math.abs(h)+1e-9;
                double enthalpyError=Math.abs(h-old.molarEnthalpy()-2e6*shift);
                assertTrue(enthalpyError<=enthalpyBound,packageId+" "+t+" K "+p+" Pa enthalpy: "+enthalpyError+" > "+enthalpyBound);
                for(int i=0;i<x.length;i++) {
                    if(x[i]<=0)continue;
                    double bound=span*Math.max(Math.abs(vbar[i]-old.partialMolarVolumes()[i]),Math.abs(vbarHigh[i]-oldHigh.partialMolarVolumes()[i]))/rt*1.05+1e-10;
                    double error=Math.abs(phi[i]-old.logFugacity()[i]-2e6*dc[i]/rt);
                    assertTrue(error<=bound,packageId+" "+t+" K "+p+" Pa ln phi["+i+"]: "+error+" > "+bound);
                    worstPhi=Math.max(worstPhi,Math.abs(phi[i]-old.logFugacity()[i]));
                }
                worstVolume=Math.max(worstVolume,Math.abs(v/old.molarVolume()-1));largestVolume=Math.max(largestVolume,volumeBound/v);
                worstEnthalpy=Math.max(worstEnthalpy,Math.abs(h-old.molarEnthalpy()));states++;
            }
            summary.append(String.format(Locale.ROOT,"%s: %d liquid states, |dv/v| <= %.3e, |dh| <= %.2f J/mol, |d ln phi| <= %.3e%n",
                    packageId,states,worstVolume,worstEnthalpy,worstPhi));
            assertTrue(states>0,packageId);
        }
        System.out.print(summary);
    }

    /**
     * Item 3: a closed 1 m3 vessel of liquid nitrogen at 90 K compressed reversibly from 0.5 to 8 MPa by the network's
     * own UV solve ({@link InventoryEquilibrium}, the vessel's volume closure and energy rows), on a catalog whose
     * nitrogen and envelope ceilings are raised to 10 MPa (the bundled package keeps 2 MPa until WP7): every state
     * conserves the inventory, {@code P(V)} has no kink at 2 MPa, and the temperature rise equals the equation of
     * state's adiabatic compression {@code int T (dv/dT)_P / c_p dP} within 1 %.
     */
    @Test void aClosedLiquidNitrogenVesselCompressedThroughTwoMegapascalsFollowsTheAdiabat() {
        var widened=widenedNitrogen(1e7);
        int n2=widened.hydrocarbon.components().indexOf("Nitrogen");
        double[] unit=new double[widened.componentCount()];unit[n2]=1;
        double[] n=unit.clone();n[n2]=1/widened.flashTP(90,5e5,unit,()->{}).volume();
        var state=widened.flashTP(90,5e5,n,()->{});
        double volume=1,energy=state.internalEnergy();
        var pressures=new ArrayList<Double>();var volumes=new ArrayList<Double>();
        pressures.add(state.pressure());volumes.add(volume);
        double eosRise=0,t0=state.temperature();
        double[] previousSlope=adiabaticSlope(widened,state);
        while(state.pressure()<8e6) {
            double next=volume*(1-2e-4);
            var predictor=solve(widened,state,next,energy-state.pressure()*(next-volume),n);
            var corrected=solve(widened,predictor,next,energy-.5*(state.pressure()+predictor.pressure())*(next-volume),n);
            double[] slope=adiabaticSlope(widened,corrected);
            eosRise+=.5*(previousSlope[0]+slope[0])*(corrected.pressure()-state.pressure());previousSlope=slope;
            energy=energy-.5*(state.pressure()+corrected.pressure())*(next-volume);volume=next;state=corrected;
            double[] totals=PhaseLayout.totalAmounts(state);
            for(int c=0;c<n.length;c++)assertEquals(n[c],totals[c],1e-10*n[n2]+1e-12,"component "+c+" conserved");
            assertEquals(volume,state.volume(),1e-9*volume,"volume closes");
            assertEquals(0,state.vaporVolume(),"liquid-full");
            pressures.add(state.pressure());volumes.add(volume);
        }
        double rise=state.temperature()-t0;
        System.out.printf(Locale.ROOT,"N2 90 K 0.5 -> %.3f MPa in %d steps: dT network %.5f K, EOS adiabat %.5f K (%+.3f %%)%n",
                state.pressure()/1e6,pressures.size()-1,rise,eosRise,100*(rise/eosRise-1));
        assertEquals(eosRise,rise,.01*Math.abs(eosRise),"temperature rise against the EOS adiabat");
        // No kink: the secant bulk modulus K = -dP/d ln V between successive states rises smoothly, and its increment is
        // the same on both sides of 2 MPa (a kink would be a jump of the increment where the retired path changed regime).
        var moduli=new ArrayList<Double>();
        for(int k=1;k<pressures.size();k++)moduli.add(-(pressures.get(k)-pressures.get(k-1))/Math.log(volumes.get(k)/volumes.get(k-1)));
        double crossing=Double.NaN;
        for(int k=2;k<moduli.size();k++) {
            double before=moduli.get(k-1)-moduli.get(k-2),after=moduli.get(k)-moduli.get(k-1);
            assertEquals(moduli.get(k-1),moduli.get(k),.01*moduli.get(k-1),"bulk modulus continuity at "+pressures.get(k)+" Pa");
            assertEquals(before,after,.1*Math.abs(before),"bulk modulus increment continuity at "+pressures.get(k)+" Pa");
            if(pressures.get(k-1)<2e6&&pressures.get(k)>=2e6)crossing=after/before-1;
        }
        System.out.printf(Locale.ROOT,"bulk modulus %.1f -> %.1f MPa; its increment changes by %.2e across 2 MPa%n",moduli.get(0)/1e6,moduli.get(moduli.size()-1)/1e6,crossing);
    }
    /** {@code dT/dP} along the adiabat, {@code T (dv/dT)_P / c_p}, from the equation of state's own derivatives. */
    private static double[] adiabaticSlope(FluidThermodynamics model,FluidThermodynamics.State state) {
        double[] x=state.liquid();double total=0;for(double value:x)total+=value;for(int i=0;i<x.length;i++)x[i]/=total;
        var values=HydrocarbonValues.of(model.hydrocarbon,state.temperature(),state.pressure(),x,Root.LIQUID);
        return new double[]{state.temperature()*values.volumeTemperatureDerivative()/values.heatCapacity()};
    }
    private static FluidThermodynamics.State solve(FluidThermodynamics model,FluidThermodynamics.State previous,double volume,double energy,double[] n) {
        return FluidTestSupport.inventoryState(model,previous,new PassiveNetwork.Inventory(volume,n,energy));
    }
    private FluidThermodynamics widenedNitrogen(double ceiling) {
        var resources=new HashMap<>(catalog.resources());
        for(String key:List.of("data/createcheme/materials/properties/nitrogen.json","data/createcheme/materials/packages/tjl20_nitrogen.json")) {
            var o=JsonParser.parseString(resources.get(key)).getAsJsonObject();o.getAsJsonObject("fluid_domain").addProperty("pressure_max_pascal",ceiling);
            resources.put(key,o.toString());
        }
        return FluidThermodynamics.forNetwork(MaterialCatalog.parse(resources),NETWORK);
    }
}
