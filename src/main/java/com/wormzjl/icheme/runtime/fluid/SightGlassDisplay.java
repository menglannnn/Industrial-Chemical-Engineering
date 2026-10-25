package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.util.List;

/** A display-only reduction of accepted transport. No inventory, equilibrium or optical model is calculated. */
public final class SightGlassDisplay {
    private SightGlassDisplay(){}
    public record Sample(int argb,int bubbles) {
        public static final Sample CLEAR=new Sample(0,0);
        public Sample{if(bubbles<0||bubbles>255||(argb>>>24==0&&bubbles!=0))throw new IllegalArgumentException("Invalid sight glass display");}
    }

    /** Colour and illustrative multiphase motion; speed uses the existing five-online-second window. */
    public static Sample of(FluidView view,List<String> components,double[] weights,MaterialCatalog catalog,String packageId) {
        var info=view.pipeInfo();
        if(info==null||info.connections().stream().noneMatch(c->c.velocityMetresPerSecond()>0))return Sample.CLEAR;
        var moved=info.contents();var volumes=moved.phaseVolumes();
        if(!(volumes[0]+volumes[1]>0))return Sample.CLEAR;
        var phases=moved.phaseMoles();double[] liquid=new double[components.size()];
        for(int c=0;c<liquid.length;c++)liquid[c]=phases[0][c]+phases[1][c];
        // Bubble density follows gas occupancy, r/(1+r) for gas/liquid ratio r; haze remains independent.
        int bubbles=bubbleStrength(volumes);
        return new Sample(turbid(TankDisplay.mix(liquid,components,weights,catalog,packageId),volumes,moved.solids().volume()),bubbles);
    }
    static int bubbleStrength(double[] volumes){
        double liquid=volumes[0]+volumes[1];
        return liquid>0&&volumes[2]>0?Math.max(1,(int)Math.round(255*(volumes[2]/(liquid+volumes[2])))):0;
    }

    /** Presentation policy, also used by the offline render sheets; does not change material appearances or physics. */
    static int turbid(int base,double[] volumes,double solids) {
        if(!(volumes[0]+volumes[1]>0))return 0;
        double haze=haze(volumes,solids),white=.25*haze;
        int alpha=Math.max(200,base>>>24);
        alpha=(int)Math.round(alpha+(255-alpha)*haze);
        return alpha<<24|whiten(base>>16&255,white)<<16|whiten(base>>8&255,white)<<8|whiten(base&255,white);
    }

    private static double haze(double[] volumes,double solids){
        double total=volumes[0]+volumes[1]+volumes[2]+solids,squares=0;
        for(double volume:volumes){double fraction=volume/total;squares+=fraction*fraction;}
        double fraction=solids/total;return Math.clamp(2*(1-squares-fraction*fraction),0,1);
    }

    private static int whiten(int channel,double amount){return (int)Math.round(channel+(255-channel)*amount);}
}
