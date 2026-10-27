package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;

/** D34 scratch probe: the trace vessel of the gate (methane saturated with crude_pc08 plus a liquid trace) and its static
 * drives; prints D34PROBE lines. Not a test of anything. */
class TraceColumnProbe extends PortFixture {
    @Test void probe() {
        int methane=index("Methane"),heavy=index("crude_pc08");
        System.out.println("D34PROBE components="+model.components());
        System.out.println("D34PROBE mw methane="+mw[methane]+" pc08="+mw[heavy]);
        double t=369,p=430000;double[] n=new double[mw.length];n[methane]=p/(FluidThermodynamics.R*t);n[heavy]=1e-2;
        var wet=model.flashTP(t,p,n,NOOP);
        System.out.println("D34PROBE wet Vliq="+wet.liquidVolume()+" Vvap="+wet.vaporVolume()+" vap pc08="+wet.vapor()[heavy]+" liq pc08="+wet.liquid()[heavy]);
        for(double trace:new double[]{2e-5,3e-5,4e-5,5e-5}) {
            double[] m=n.clone();m[heavy]=wet.vapor()[heavy]+trace;
            var unit=model.flashTP(t,p,m,NOOP);for(int c=0;c<m.length;c++)m[c]/=unit.volume();
            var s=model.flashTP(t,p,m,NOOP);
            double liquidMass=model.phaseMass(s,FluidThermodynamics.OIL);
            System.out.println("D34PROBE trace="+trace+" V="+s.volume()+" Vliq="+s.liquidVolume()+" mliq="+liquidMass+" rho_l="+(liquidMass/s.liquidVolume())+" rho_g="+model.vaporDensity(s)+" holdsOil="+FluidThermodynamics.holdsPhase(s,FluidThermodynamics.OIL));
        }
    }
}
