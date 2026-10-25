package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;
import java.util.ArrayList;
import java.util.List;

/** Illustrative bubble/droplet rings inside a unit aperture, not a particle or flow simulation. */
public final class SightGlassBubbles {
    private SightGlassBubbles(){}
    public record Bubble(double x,double y,double radius){}

    /** Pale translucent rim; the liquid behind its empty centre retains the published colour. */
    public static int colour(int base,int strength){
        if(strength<0||strength>255)throw new IllegalArgumentException("Invalid bubble strength");
        if(strength==0||base>>>24==0)return 0;
        return (150+80*strength/255)<<24|brighten(base>>16&255)<<16|brighten(base>>8&255)<<8|brighten(base&255);
    }

    /** Game ticks animate a stable device seed; all rings remain completely inside the circular window. */
    public static List<Bubble> at(double ticks,long seed,int strength){
        if(!Double.isFinite(ticks)||strength<0||strength>255)throw new IllegalArgumentException("Invalid bubble display");
        if(strength==0)return List.of();
        var result=new ArrayList<Bubble>();int count=Math.max(1,(8*strength+127)/255);
        for(int i=0;i<count;i++){
            double first=fraction(seed+0x9E3779B97F4A7C15L*i),second=fraction(seed+0x632BE59BD9B4E019L*(i+1));
            double period=65+55*second,phase=ticks/period+first;phase-=Math.floor(phase);
            double maximum=.10+.07*second,radius=maximum*DeterministicMath.sin(Math.PI*phase);
            double x=(2*first-1)*.62+.05*DeterministicMath.sin(2*Math.PI*phase+first*6);
            double room=.97-maximum,y=(2*phase-1)*Math.sqrt(room*room-x*x);
            result.add(new Bubble(x,y,radius));
        }
        return List.copyOf(result);
    }

    private static double fraction(long value){value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;value=(value^(value>>>27))*0x94D049BB133111EBL;return ((value^(value>>>31))>>>11)*0x1.0p-53;}
    private static int brighten(int channel){return (int)Math.round(channel+.65*(255-channel));}
}
