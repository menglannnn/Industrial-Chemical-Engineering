package com.wormzjl.createcheme.science.fluid.thermo;

import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import org.junit.jupiter.api.Test;

class HydrocarbonModelTest {
    /**
     * The direct liquid path (P3 WP4): a hydrocarbon liquid is the translated PR78 at the state pressure, so its volume,
     * {@code ln phi} and compressibility are the equation of state's own there, with no reference pressure and no global
     * compressibility; the column's data is untouched.
     */
    @Test void aLiquidIsTheEquationOfStateAtTheStatePressureWithItsOwnCompressibility() {
        var catalog=MaterialCatalog.bundled();var original=catalog.requirePackage("createcheme:tjl20_methane").fingerprint();
        var model=new HydrocarbonModel(catalog,"createcheme:tjl20_methane");
        var eos=model.translated();
        double[] liquid=new double[model.componentCount()];liquid[6]=1;
        double[][] states={{300,1e6},{300,101325},{350,1e6},{350,2e6}};
        double[] pureKappa=new double[states.length];
        for(int s=0;s<states.length;s++) {
            double t=states[s][0],p=states[s][1];
            var phase=model.phase(t,p,liquid,PengRobinsonKernel.Root.LIQUID);
            var direct=eos.evaluate(t,p,liquid,PengRobinsonKernel.Root.LIQUID,eos.prepare(t));
            assertEquals(Double.doubleToLongBits(direct.molarVolume()),Double.doubleToLongBits(phase.molarVolume()),t+" K "+p+" Pa");
            assertEquals(Double.doubleToLongBits(direct.molarEnthalpy()),Double.doubleToLongBits(phase.molarEnthalpy()));
            assertArrayEquals(direct.logFugacityCoefficients(),phase.logFugacity(),0);
            assertEquals(direct.isothermalCompressibility(),phase.isothermalCompressibility(),1e-24);
            pureKappa[s]=phase.isothermalCompressibility();
            assertDoesNotThrow(()->model.phase(t,p,liquid,PengRobinsonKernel.Root.LIQUID,model.prepare(t),true),"a liquid root beside a vapour");
        }
        // The compressibility is a property of the state: it rises with temperature, and is not one global number.
        assertTrue(pureKappa[2]>pureKappa[0],"kappa(350 K) > kappa(300 K)");
        assertNotEquals(1e-9,pureKappa[0]);
        liquid[6]=.05;liquid[12]=.45;liquid[com.wormzjl.createcheme.science.material.MaterialTestBasis.CRUDE-1]=.5;
        var crude=model.phase(350,1e6,liquid,PengRobinsonKernel.Root.LIQUID);
        assertTrue(crude.molarVolume()>0&&crude.isothermalCompressibility()>0);
        assertEquals(original,catalog.requirePackage("createcheme:tjl20_methane").fingerprint());
        assertTrue(model.revision().contains(":"+HydrocarbonModel.FORMULATION+":"),model.revision());
        assertFalse(model.revision().contains("k="),"no compressibility in the revision");
    }
    @Test void liquidPressureCorrectionSatisfiesTheChemicalPotentialVolumeIdentity() {
        var model=new HydrocarbonModel(MaterialCatalog.bundled(),"createcheme:tjl20_methane");
        double[] n=new double[model.componentCount()];n[6]=1;
        double p=1e6,dtp=10,t=300;
        double gl=model.phase(t,p-dtp,n,PengRobinsonKernel.Root.LIQUID).logFugacity()[6]+Math.log(p-dtp);
        double gh=model.phase(t,p+dtp,n,PengRobinsonKernel.Root.LIQUID).logFugacity()[6]+Math.log(p+dtp);
        double volume=(gh-gl)/(2*dtp)*8.31446261815324*t;
        assertEquals(model.phase(t,p,n,PengRobinsonKernel.Root.LIQUID).molarVolume(),volume,1e-10);
    }

    // ---------------- from HybridDerivativeTest ----------------
    /** Liquid water is Region 1 at the state pressure: its own dv/dP (not a global compressibility; the state's
     * {@code waterCompressibility}) and the Maxwell identity dh/dP = v - T dv/dT, including the metastable liquid below
     * saturation (350 K at 10 kPa). */
    @Test void stateWaterPreservesHeatAndPressureIdentities() {
        var model=com.wormzjl.createcheme.fluid.support.FluidTestSupport.model(MaterialCatalog.bundled(),"createcheme:tjl20_methane");
        double[] none=new double[model.hydrocarbon.componentCount()];
        for(double t:new double[]{298.15,350,375})for(double p:new double[]{1e4,101325,1e6,2e6}) {
            var s=model.waterLiquid(t,p);double dt=.001,dp=10;
            double vp=(model.waterLiquid(t,p+dp).molarVolume()-model.waterLiquid(t,p-dp).molarVolume())/(2*dp);
            double vt=(model.waterLiquid(t+dt,p).molarVolume()-model.waterLiquid(t-dt,p).molarVolume())/(2*dt);
            double hp=(model.waterLiquid(t,p+dp).molarEnthalpy()-model.waterLiquid(t,p-dp).molarEnthalpy())/(2*dp);
            double kappa=model.state(t,p,none,none,1,0,p).waterCompressibility();
            assertEquals(-kappa*s.molarVolume(),vp,1e-6*kappa*s.molarVolume());
            assertTrue(kappa>3.5e-10&&kappa<6e-10,"water's own compressibility at "+t+" K: "+kappa);
            assertEquals(s.molarVolume()-t*vt,hp,1e-11);
        }
    }
    @Test void liquidMixtureChemicalPotentialPressureDerivativesSumToPhysicalVolume() {
        var model=new HydrocarbonModel(MaterialCatalog.bundled(),"createcheme:tjl20_methane");
        double[] n=new double[com.wormzjl.createcheme.science.material.MaterialTestBasis.CRUDE];n[0]=.01;n[6]=.09;n[12]=.4;n[com.wormzjl.createcheme.science.material.MaterialTestBasis.CRUDE-1]=.5;
        double t=350,p=1e6,dp=10;
        var mid=model.phase(t,p,n,PengRobinsonKernel.Root.LIQUID);
        var a=model.phase(t,p-dp,n,PengRobinsonKernel.Root.LIQUID).logFugacity();var b=model.phase(t,p+dp,n,PengRobinsonKernel.Root.LIQUID).logFugacity();
        double v=0;for(int i=0;i<n.length;i++)v+=n[i]*(b[i]-a[i]+Math.log((p+dp)/(p-dp)))/(2*dp)*FluidThermodynamics.R*t;
        assertEquals(mid.molarVolume(),v,1e-9*mid.molarVolume()+1e-12);
    }
}
