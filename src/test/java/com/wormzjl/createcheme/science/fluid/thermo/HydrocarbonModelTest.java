package com.wormzjl.createcheme.science.fluid.thermo;

import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.PengRobinsonKernel;
import org.junit.jupiter.api.Test;

class HydrocarbonModelTest {
    @Test void sameCompressionAppliesToPureLiquidAndCrudeMixturesWithoutChangingColumnData() {
        var catalog=MaterialCatalog.bundled();var original=catalog.requirePackage("createcheme:tjl20_methane").fingerprint();
        var model=new HydrocarbonModel(catalog,"createcheme:tjl20_methane");
        double[] liquid=new double[model.componentCount()];liquid[6]=1;
        var pure=model.phase(300,1e6,liquid,PengRobinsonKernel.Root.LIQUID);
        assertEquals(1e-9,-pure.volumePressureDerivative()/pure.molarVolume(),1e-22);
        liquid[6]=.05;liquid[12]=.45;liquid[com.wormzjl.createcheme.science.material.MaterialTestBasis.CRUDE-1]=.5;
        var crude=model.phase(350,1e6,liquid,PengRobinsonKernel.Root.LIQUID);
        assertEquals(1e-9,-crude.volumePressureDerivative()/crude.molarVolume(),1e-22);
        assertTrue(crude.molarVolume()>0);
        assertEquals(original,catalog.requirePackage("createcheme:tjl20_methane").fingerprint());
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
    @Test void sharedWaterResponsePreservesHeatAndPressureIdentities() {
        var model=com.wormzjl.createcheme.fluid.support.FluidTestSupport.model(MaterialCatalog.bundled(),"createcheme:tjl20_methane");
        for(double t:new double[]{298.15,350,375})for(double p:new double[]{101325,1e6,2e6}) {
            var s=model.waterLiquid(t,p);double dt=.001,dp=10;
            double vp=(model.waterLiquid(t,p+dp).molarVolume()-model.waterLiquid(t,p-dp).molarVolume())/(2*dp);
            double vt=(model.waterLiquid(t+dt,p).molarVolume()-model.waterLiquid(t-dt,p).molarVolume())/(2*dt);
            double hp=(model.waterLiquid(t,p+dp).molarEnthalpy()-model.waterLiquid(t,p-dp).molarEnthalpy())/(2*dp);
            assertEquals(-1e-9*s.molarVolume(),vp,1e-20);
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
