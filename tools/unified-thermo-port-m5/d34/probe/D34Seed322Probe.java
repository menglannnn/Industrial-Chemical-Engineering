package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import java.util.*;
import org.junit.jupiter.api.Test;

/** D34 scratch: the exact blocks of fuzz seed 322's minimized KEEP list (full precision), and the original layout. */
class D34Seed322Probe {
    @Test void blocks() {
        var r=new Random(322);var blocks=HoldFuzzTest.world(r);var placements=HoldFuzzTest.placements(r,blocks);
        String keep="0@498,1@498,3@498,4@498,6@498,7@498,8@498,9@498,10@498,13@498,14@680,16@729,17@770";
        for(int k=0;k<placements.size();k++){var p=placements.get(k);System.out.println("D34ORIG "+k+" tick="+p.tick()+" "+p.block().label());}
        for(var item:keep.split(",")) {
            var parts=item.split("@");var b=placements.get(Integer.parseInt(parts[0])).block();
            String spec=b.spec()==null?"null":("T="+b.spec().temperature()+" P="+b.spec().pressure()+" V="+b.spec().volume()+" n="+Arrays.toString(b.spec().composition()));
            String control=b.control() instanceof FlowControl.Passive?"passive":String.valueOf(b.control());
            System.out.println("D34KEEP "+parts[0]+"@"+parts[1]+" "+b.kind()+" "+b.x()+","+b.y()+","+b.z()+" facing="+b.facing()+" bore="+b.bore()+" control="+control+" fluid="+b.fluid()+" spec "+spec);
        }
    }
}
