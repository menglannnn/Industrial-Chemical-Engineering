package com.wormzjl.icheme.client.gui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Shared fluid-screen control positions, including the minimum supported 300px GUI width. */
public final class FluidGuiLayout {
    private FluidGuiLayout(){}
    public record Slot(int x,int width){}
    /** Close, Apply, composition/flow basis, temperature, pressure and flow-time unit. */
    public static List<Slot> footer(int width) {
        if(width<300)throw new IllegalArgumentException("Fluid GUI width below minimum");
        int[] natural={50,60,90,70,80,60},minimum={38,40,44,32,38,32};
        int available=width-50,total=Arrays.stream(natural).sum(),floor=Arrays.stream(minimum).sum();
        int[] sizes=natural.clone();
        if(available<total)for(int i=0;i<sizes.length;i++)sizes[i]=minimum[i]+(natural[i]-minimum[i])*(available-floor)/(total-floor);
        var slots=new ArrayList<Slot>();slots.add(new Slot(10,sizes[0]));slots.add(new Slot(16+sizes[0],sizes[1]));
        int x=width-10;for(int i=2;i<6;i++)x-=sizes[i];x-=18;
        for(int i=2;i<6;i++){slots.add(new Slot(x,sizes[i]));x+=sizes[i]+6;}
        return List.copyOf(slots);
    }
    public static List<Slot> tabs(int width,int count) {
        if(width<300||count<1||count>4)throw new IllegalArgumentException("Invalid fluid tabs");
        int size=Math.min(130,(width-20-6*(count-1))/count);var slots=new ArrayList<Slot>();
        for(int i=0;i<count;i++)slots.add(new Slot(10+i*(size+6),size));
        return List.copyOf(slots);
    }
}
