package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.util.List;

/**
 * What a client draws of an open (floating-roof) tank (batch 2026-10-21-floating-roof-tanks, plan section 3.4, owner
 * decisions FR4 and FR7): the liquid's height over the bottom and the free water's under it, m, the fill height the shell
 * is drawn against, and the two liquids' colours as packed AARRGGBB tints. Read off a vessel's published view on the
 * engine's schedule; nothing here solves.
 *
 * <p>Colours: each liquid mixes its components' {@link MaterialCatalog.FluidAppearance} subtractively by mass fraction
 * (Beer-Lambert: per colour channel the transmittance {@code T = prod T_i^{w_i}}, {@code T_i} the channel over 255), and
 * its transparency the same way. Free water takes water's appearance.</p>
 */
public record TankDisplay(float level,float waterLevel,float height,int oilArgb,int waterArgb) {
    public TankDisplay {
        if(!Float.isFinite(level)||!Float.isFinite(waterLevel)||!Float.isFinite(height)||level<0||waterLevel<0||waterLevel>level||!(height>0))
            throw new IllegalArgumentException("Invalid tank display");
    }
    /** An empty tank of fill height {@code height}. */
    public static TankDisplay empty(float height){return new TankDisplay(0,0,height,0,0);}

    /**
     * The display of a vessel's view, or null for a view that is not a vessel's. {@code components} is the view's
     * component axis (water last), {@code molecularWeights} its molar masses (kg/mol), {@code packageId} the property
     * package whose appearances name the colours.
     */
    public static TankDisplay of(FluidView view,List<String> components,double[] molecularWeights,MaterialCatalog catalog,String packageId) {
        var tank=view.tank();if(tank==null)return null;
        float height=(float)tank.levelHeight();
        var state=view.state();if(state==null)return empty(height);
        var volumes=state.phaseVolumes();double footprint=state.capacity()/tank.levelHeight();
        float level=(float)(tank.level(state));float water=(float)Math.min(level,volumes[1]/footprint);
        var moles=state.phaseMoles();
        int oil=volumes[0]>0?mix(moles[0],components,molecularWeights,catalog,packageId):0;
        int free=volumes[1]>0?catalog.componentAppearance(packageId,"Water").argb():0;
        return new TankDisplay(level,water,height,oil,free);
    }
    /** The subtractive mix (Beer-Lambert) of the components of {@code moles} by mass fraction, as AARRGGBB. */
    static int mix(double[] moles,List<String> components,double[] molecularWeights,MaterialCatalog catalog,String packageId) {
        double total=0;for(int c=0;c<moles.length;c++)total+=moles[c]*molecularWeights[c];
        if(!(total>0))return 0;
        double r=0,g=0,b=0,clear=0;
        for(int c=0;c<moles.length;c++) {
            double w=moles[c]*molecularWeights[c]/total;if(!(w>0))continue;
            var look=catalog.componentAppearance(packageId,components.get(c));int rgb=look.rgb();
            r+=w*log((rgb>>16&255)/255.0);g+=w*log((rgb>>8&255)/255.0);b+=w*log((rgb&255)/255.0);clear+=w*log(look.transparency());
        }
        int alpha=(int)Math.round((1-Math.exp(clear))*255);
        return alpha<<24|channel(r)<<16|channel(g)<<8|channel(b);
    }
    /** {@code ln x}, floored at the 8-bit channel's darkest step so a black or opaque component does not take the log to minus infinity. */
    private static double log(double x){return Math.log(Math.max(x,.5/255));}
    private static int channel(double logTransmittance){return (int)Math.round(Math.min(1,Math.exp(logTransmittance))*255);}
}
