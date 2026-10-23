package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.util.List;

/** A display-only reduction of accepted transport. No inventory, equilibrium or optical model is calculated. */
public final class SightGlassDisplay {
    private SightGlassDisplay(){}

    /** Packed ARGB, or zero when no liquid is currently passing. Speed uses the existing five-online-second window. */
    public static int colour(FluidView view,List<String> components,double[] weights,MaterialCatalog catalog,String packageId) {
        var info=view.pipeInfo();
        if(info==null||info.connections().stream().noneMatch(c->c.velocityMetresPerSecond()>0))return 0;
        var moved=info.contents();var volumes=moved.phaseVolumes();
        if(!(volumes[0]+volumes[1]>0))return 0;
        var phases=moved.phaseMoles();double[] liquid=new double[components.size()];
        for(int c=0;c<liquid.length;c++)liquid[c]=phases[0][c]+phases[1][c];
        return turbid(TankDisplay.mix(liquid,components,weights,catalog,packageId),volumes,moved.solids().volume());
    }

    /** Presentation policy, also used by the offline render sheets; does not change material appearances or physics. */
    static int turbid(int base,double[] volumes,double solids) {
        if(!(volumes[0]+volumes[1]>0))return 0;
        double total=volumes[0]+volumes[1]+volumes[2]+solids,squares=0;
        for(double volume:volumes)squares+=Math.pow(volume/total,2);
        squares+=Math.pow(solids/total,2);
        double haze=Math.clamp(2*(1-squares),0,1),white=.25*haze;
        int alpha=Math.max(200,base>>>24);
        alpha=(int)Math.round(alpha+(255-alpha)*haze);
        return alpha<<24|whiten(base>>16&255,white)<<16|whiten(base>>8&255,white)<<8|whiten(base&255,white);
    }

    private static int whiten(int channel,double amount){return (int)Math.round(channel+(255-channel)*amount);}
}
