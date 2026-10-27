package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Group C (trace truncation in the flash, decision D34): prints a minimized fuzz world (-Dreplay.seed, -Dreplay.keep from a
 * HoldMinimizerTest KEEP line) as EventPathWorld placements with the seed's exact doubles, the source of the gate tests
 * in TracePhaseFoldTest. Not a gate: compiled only with hold-classes.init.gradle.
 */
class FoldGateWorldsProbeTest {
    @Test void printPlacements() {
        String keep=System.getProperty("replay.keep");if(keep==null)return;long seed=Long.getLong("replay.seed");
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);
        var out=new StringBuilder("FOLD_WORLD seed="+seed+"\n");
        for(var item:keep.split(",")) {
            var parts=item.split("@");var b=original.get(Integer.parseInt(parts[0])).block();
            String control=switch(b.control()){case FlowControl.Passive ignored->"passive";
                case FlowControl.Pump p->"new FlowControl.Pump("+p.targetVolumeFlow()+","+p.maximumAddedPressure()+","+p.efficiency()+")";
                case FlowControl.Compressor c->"new FlowControl.Compressor("+c.targetVolumeFlow()+","+c.maximumPressureRatio()+","+c.efficiency()+")";
                case FlowControl.PressureValve v->"new FlowControl.PressureValve("+v.targetPressure()+")";};
            String spec=b.spec()==null?"null":"spec("+b.fluid()+","+b.spec().temperature()+","+b.spec().pressure()+")";
            out.append("  @").append(parts[1]).append(" w.place(").append(b.x()).append(',').append(b.y()).append(',').append(b.z()).append(",Kind.").append(b.kind())
               .append(',').append(b.facing()).append(',').append(control).append(',').append(spec).append(',').append(b.bore()).append(");\n");
        }
        System.out.print(out);
    }
}
