package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;

/** HOLDS probe (never commit): how a vanishing vapour phase's properties behave as its amounts shrink. */
class TraceVapourProbeTest {
    @Test void probe() {
        var model=FluidTestSupport.networkModel();int n=MaterialTestBasis.NETWORK;
        double t=298.148,p=129913.33,wl=55288.46,psat=model.saturationPressure(t);
        for(double s:new double[]{1e-20,1e-31,1e-60,1e-100,1e-140,1e-150,1e-153,1e-155,1e-157,1e-160,1e-170,1e-200,1e-250,1e-300,1e-305,1e-310,1e-315}) {
            double nN2=40*s,wv=nN2*psat/(p-psat);
            double[] l=new double[n],v=new double[n];v[MaterialTestBasis.NITROGEN]=nN2;
            var a=model.adoptingState(t,p,l.clone(),v.clone(),wl,wv,p-psat,null);
            double[] v2=v.clone();v2[MaterialTestBasis.NITROGEN]*=1+1e-6;
            var b=model.adoptingState(t,p,l.clone(),v2,wl,wv,p-psat,null);
            double[] v3=v.clone();v3[MaterialTestBasis.NITROGEN]*=1+2e-6;
            var c=model.adoptingState(t,p,l.clone(),v3,wl,wv,p-psat,null);
            double dlnpw=(Math.log(b.waterPartialPressure())-Math.log(a.waterPartialPressure()))/Math.log1p(1e-6);
            double dlnpw2=(Math.log(c.waterPartialPressure())-Math.log(a.waterPartialPressure()))/Math.log1p(2e-6);
            System.out.println("TRACE_VAPOUR scale="+s+" nN2="+nN2+" gasVol="+a.vaporVolume()+" row="+Math.log(a.waterPartialPressure()/psat)
                    +" dln(pw)/dln(nN2) h=1e-6: "+dlnpw+" h=2e-6: "+dlnpw2+" volume="+a.volume()+" U="+a.internalEnergy());
        }
    }
}
