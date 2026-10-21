package com.wormzjl.icheme.science.fluid.thermo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaterRegion1Test {
    @Test void matchesOfficialRegionOneCheckpoints() {
        checkpoint(300,3e6,.00100215168,115331.273,112324.818,4173.01218);
        checkpoint(300,80e6,.000971180894,184142.828,106448.356,4010.08987);
        checkpoint(500,3e6,.00120241800,975542.239,971934.985,4655.80682);
    }
    @Test void verifiesAnalyticPressureAndTemperatureDerivatives() {
        var s=WaterRegion1.evaluate(300,3e6);
        var tl=WaterRegion1.evaluate(299.99,3e6); var th=WaterRegion1.evaluate(300.01,3e6);
        var pl=WaterRegion1.evaluate(300,3e6-100); var ph=WaterRegion1.evaluate(300,3e6+100);
        relative(s.volumeTemperatureDerivative(),(th.specificVolume()-tl.specificVolume())/.02);
        relative(s.volumePressureDerivative(),(ph.specificVolume()-pl.specificVolume())/200);
        relative(s.specificHeatCapacity(),(th.specificEnthalpy()-tl.specificEnthalpy())/.02);
        relative(s.enthalpyPressureDerivative(),(ph.specificEnthalpy()-pl.specificEnthalpy())/200);
    }
    /**
     * Owner rule of 2026-10-16: the liquid continues past Region 1 (the metastable liquid below saturation, the region's
     * sums above 100 MPa, the held slopes past 623.15 K) and only a state that is not positive and finite is refused.
     */
    @Test void continuesPastItsRegionAndRefusesOnlyAStateThatIsNotPositive() {
        var edge=WaterRegion1.evaluate(WaterRegion1.REGION_1_MAXIMUM_TEMPERATURE,80e6);
        var hot=WaterRegion1.evaluate(650,80e6);
        double step=650-WaterRegion1.REGION_1_MAXIMUM_TEMPERATURE;
        assertEquals(edge.specificVolume()+edge.volumeTemperatureDerivative()*step,hot.specificVolume(),1e-15);
        assertEquals(edge.specificEnthalpy()+edge.specificHeatCapacity()*step,hot.specificEnthalpy(),1e-6);
        assertTrue(WaterRegion1.evaluate(500,1e5).specificVolume()>0,"the metastable liquid below saturation");
        assertTrue(WaterRegion1.evaluate(300,101e6).specificVolume()>0,"the region's sums above 100 MPa");
        assertThrows(IllegalArgumentException.class,()->WaterRegion1.evaluate(0,1e5));
        assertThrows(IllegalArgumentException.class,()->WaterRegion1.evaluate(300,0));
        assertThrows(IllegalArgumentException.class,()->WaterRegion1.evaluate(Double.NaN,1e5));
        assertThrows(IllegalArgumentException.class,()->WaterRegion1.diluteVaporViscosity(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->WaterRegion1.diluteVaporViscosity(0));
        assertTrue(WaterRegion1.diluteVaporViscosity(300)>9e-6);
        double cold=WaterRegion1.diluteVaporViscosity(200);
        assertTrue(cold>0&&cold<WaterRegion1.diluteVaporViscosity(WaterRegion1.VISCOSITY_LOW),"the power-law continuation");
    }
    private static void checkpoint(double t,double p,double v,double h,double u,double cp) {
        var s=WaterRegion1.evaluate(t,p);
        assertEquals(v,s.specificVolume(),5e-12);
        assertEquals(h,s.specificEnthalpy(),.001);
        assertEquals(u,s.specificInternalEnergy(),.001);
        assertEquals(cp,s.specificHeatCapacity(),.0001);
    }
    private static void relative(double expected,double actual) {
        assertEquals(expected,actual,Math.abs(expected)*1e-5+1e-18);
    }
}
