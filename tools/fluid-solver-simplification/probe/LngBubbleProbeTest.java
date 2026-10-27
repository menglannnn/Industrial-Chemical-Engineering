package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import org.junit.jupiter.api.Test;

/** t38: the phases of cold seed 397's LNG (111.657 K) at a few pressures, to pick a two-phase supplier for the K6 gate. */
class LngBubbleProbeTest {
    @Test void phases() {
        var catalog=PilotCryogenicTestCatalog.catalog();var network=FluidMaterialCatalog.networkPackage(catalog);
        MaterialRuntime.with(catalog,network,()->{
            var model=FluidThermodynamics.forNetwork(catalog,network);
            double[] n={0.0019051322577280617,0.8927278421557977,0.06093423437004952,0.04443279121642472,0};
            for(double t:new double[]{111.8,112.0,112.2,112.4,112.6,113.0}) {double p=101325;
                var s=model.flashTP(t,p,n,()->{});
                System.out.println("LNG_PROBE T="+t+" P="+p+" liqV="+s.liquidVolume()+" vapV="+s.vaporVolume()+" liqMol="+java.util.Arrays.stream(s.liquidView()).sum()+" vapMol="+java.util.Arrays.stream(s.vaporView()).sum());
            }
            return null;});
    }
}
