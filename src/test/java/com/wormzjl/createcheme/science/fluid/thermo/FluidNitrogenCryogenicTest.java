package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Nitrogen is valid - not merely allowed - from its triple point to the package maximum (F4, T1.3). Every reference is
 * the NIST WebBook (Span et al. 2000 equation of state; Lemmon and Jacobsen 2004 transport) as fetched into
 * {@code documentation/fluid-followups/f4-logs/nist/}. The model is the network's own: PR78 with CoolProp constants, the
 * liquid at the state pressure (the direct liquid path of P3 WP4; before it, the 2 MPa reference carried by a global
 * 1e-9 1/Pa compressibility), the translation anchored at the NIST liquid volume at 90 K and 2 MPa at that state, the
 * NIST Shomate-based fit, its heat capacity held below the fit's 273.16 K lower end (owner decision D37, simplification
 * S4, which deleted F4's segment below it).
 *
 * <p>Tolerances are chosen from the measured numbers, not beyond them: saturation pressure measured +1.21 % (PR78 with
 * CoolProp's acentric factor; tolerance 1.5 %; +1.23 % on the retired 2 MPa path), saturated liquid density -0.07 %
 * (tolerance 0.5 %; +0.34 % on the retired path, whose global 1e-9 1/Pa expanded the liquid too little from 2 MPa to
 * 101 kPa), vapour Cp at 1 atm -0.96 % at 100 K and +0.04 % at 200 K (tolerance 1 %, the brief's; at 100 K PR78's
 * residual heat capacity near saturation is 0.3 J/mol/K low, the held ideal-gas part 0.03 % above NIST; -0.99 % and
 * +0.03 % with F4's segment).
 */
class FluidNitrogenCryogenicTest {
    private static final String NETWORK="createcheme:tjl20_methane_nitrogen";
    private final MaterialCatalog catalog=MaterialCatalog.bundled();
    private final FluidThermodynamics model=FluidThermodynamics.forNetwork(catalog,NETWORK);
    private final int nitrogen=model.hydrocarbon.components().indexOf("Nitrogen");
    private final double[] pure=pure();
    private double[] pure(){double[] x=new double[model.hydrocarbon.componentCount()];x[model.hydrocarbon.components().indexOf("Nitrogen")]=1;return x;}
    /** NIST at 77.355 K: saturation 101,325.059 Pa, saturated liquid 806.0844 kg/m3 (isotherm-77.355K.tsv). */
    private static final double NBP=77.355,NIST_SATURATION=101325.059474,NIST_LIQUID_DENSITY=806.084401826;

    private double logFugacity(double t,double p,PengRobinsonKernel.Root root){return model.hydrocarbon.phase(t,p,pure,root).logFugacityView()[nitrogen];}
    /** The pressure where the network's liquid and vapour fugacities of pure nitrogen agree, by bisection in log P. */
    private double saturation(double t) {
        double low=2e4,high=4e5;
        for(int k=0;k<200;k++){double p=Math.sqrt(low*high);if(logFugacity(t,p,PengRobinsonKernel.Root.LIQUID)>logFugacity(t,p,PengRobinsonKernel.Root.VAPOR))low=p;else high=p;}
        return Math.sqrt(low*high);
    }

    @Test void saturationPressureAtTheNormalBoilingPointAgreesWithNist() {
        double p=saturation(NBP);
        System.out.printf(Locale.ROOT,"nitrogen Psat(77.355 K): model %.3f Pa, NIST %.3f Pa, %+.3f %%%n",p,NIST_SATURATION,100*(p/NIST_SATURATION-1));
        assertEquals(1,p/NIST_SATURATION,.015,"PR78 saturation at the normal boiling point");
    }

    @Test void saturatedLiquidDensityAtTheNormalBoilingPointAgreesWithNist() {
        double density=model.hydrocarbon.molecularWeight(nitrogen)/model.hydrocarbon.phase(NBP,NIST_SATURATION,pure,PengRobinsonKernel.Root.LIQUID).molarVolume();
        System.out.printf(Locale.ROOT,"nitrogen saturated liquid density (77.355 K): model %.4f kg/m3, NIST %.4f, %+.3f %%%n",density,NIST_LIQUID_DENSITY,100*(density/NIST_LIQUID_DENSITY-1));
        assertEquals(1,density/NIST_LIQUID_DENSITY,.005,"translated PR78 liquid density");
    }

    /** The vapour heat capacity the network integrates: the enthalpy of the network's own vapour state, differenced. */
    @Test void vapourHeatCapacityAt100And200KelvinAgreesWithNistWithinOnePercent() {
        double[][] nist={{100,30.0249292065},{200,29.2321313603}};
        for(double[] row:nist) {
            double t=row[0],h=1e-3;
            double cp=(vapourEnthalpy(t+h,101325)-vapourEnthalpy(t-h,101325))/(2*h);
            System.out.printf(Locale.ROOT,"nitrogen vapour Cp(%.0f K, 1 atm): model %.5f J/mol/K, NIST %.5f, %+.3f %%%n",t,cp,row[1],100*(cp/row[1]-1));
            assertEquals(1,cp/row[1],.01,"vapour Cp at "+t+" K");
        }
    }
    private double vapourEnthalpy(double t,double p) {
        double[] none=new double[pure.length];
        return model.state(t,p,none,pure,0,0,p).enthalpy();
    }

    /**
     * NIST zero-pressure heat capacity (0.1 and 1 kPa isobars extrapolated; cp0-zero-pressure-63-303K.tsv). Below the fit's
     * 273.16 K lower end the network holds the fit's value there, 29.111 J/(mol K): within 0.05 % of NIST down to the
     * triple point (D37 S4). A hold at the Shomate fit's own 298.15 K end, 29.124 J/(mol K), would miss by 0.07 %.
     */
    @Test void theHeldIdealGasHeatCapacityBelow273KelvinFollowsNist() {
        double[][] nist={{63.16,29.1025374159},{78.16,29.1030121811},{98.16,29.1036606132},{148.16,29.1054103345},
                {198.16,29.1074493210},{248.16,29.1114752464},{273.16,29.1163251898}};
        var eos=model.hydrocarbon.translated();
        double worst=0;
        for(double[] row:nist) {
            double cp0=eos.evaluateValues(row[0],100,pure,PengRobinsonKernel.Root.VAPOR,eos.prepare(row[0])).idealGasHeatCapacity();
            assertEquals(1,cp0/row[1],5e-4,"ideal-gas Cp at "+row[0]+" K: "+cp0);
            worst=Math.max(worst,Math.abs(cp0/row[1]-1));
        }
        System.out.printf(Locale.ROOT,"nitrogen ideal-gas Cp held below 273.16 K: worst %.4f %% of NIST zero-pressure Cp (63.16-273.16 K)%n",100*worst);
    }

    @Test void theNetworkPackageDeclaresTheNitrogenTriplePointAsItsMinimum() {
        var envelope=catalog.fluidValidity(NETWORK).orElseThrow();var own=catalog.fluidValidity(NETWORK,"Nitrogen").orElseThrow();
        assertEquals(63.151,envelope.minimumTemperature());assertEquals(63.151,own.minimumTemperature());
        // 10 MPa since P3 WP7 (design section 8.1: nitrogen and the network envelope widened; the cuts keep 2 MPa).
        assertEquals(900,envelope.maximumTemperature());assertEquals(100,envelope.minimumPressure());assertEquals(1e7,envelope.maximumPressure());
        assertEquals(63.151,catalog.requirePackage(NETWORK).properties().get(nitrogen).minimumTemperature(),"the record's own range starts at its triple point");
        // A vapour at 5 kPa (below its 12.5 kPa saturation pressure) is a state just above the triple point and, since the
        // owner rule of 2026-10-16 (a freezing point is never a validity bound), just below it too.
        double[] n=new double[model.componentCount()];n[nitrogen]=1;
        assertDoesNotThrow(()->model.flashTP(63.2,5000,n,()->{}));
        var below=assertDoesNotThrow(()->model.flashTP(63.1,5000,n,()->{}));
        assertTrue(below.vaporVolume()>0&&below.liquidVolume()==0,"a gas below the triple point");
    }

    /** Liquid nitrogen at its normal boiling point is a whole network state, with a viscosity from the NIST table. */
    @Test void liquidNitrogenIsAWholeNetworkState() {
        double[] n=new double[model.componentCount()];n[nitrogen]=1;
        var state=model.flashTP(NBP,2e5,n,()->{});
        assertTrue(state.liquidVolume()>0&&state.vaporVolume()==0,"compressed liquid below its saturation temperature");
        double mu=model.viscosity.liquid(NBP,state.liquid()).pascalSeconds();
        // NIST saturated liquid at 77.355 K: 160.661 uPa.s (the table is the saturated liquid).
        assertEquals(1,mu/160.661395320e-6,.005,"liquid viscosity "+mu);
        assertTrue(model.viscosity.vapor(100,pure,0)>0);
    }
}
