package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * t15 (start flows, 2026-10-13): replays minimized fuzz worlds given as -Dt15cards="seed:i@tick,i@tick,...;seed:..."
 * (KEEP lines of HoldMinimizerTest or T13TraceProbeTest.minimize; holdFuzz.defaults selects the distribution) for
 * -Dt15slices slices (default 120) after their last placement, and prints for each: its placements as EventPathWorld lines
 * with the seed's exact doubles (T15_PLACE), the first hold with its island (T15_HELD) or T15_CLEAN, and the islands at
 * the end. Not a gate: compiled only with t15.init.gradle.
 */
class T15CardProbeTest {
    @Test void cards() {
        String cards=System.getProperty("t15cards");if(cards==null||cards.isEmpty())return;
        int slices=Integer.getInteger("t15slices",120);
        for(var card:cards.split(";")) {
            var parts=card.split(":");long seed=Long.parseLong(parts[0]);
            var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);
            var list=new ArrayList<HoldFuzzTest.Placement>();
            for(var item:parts[1].split(",")){var p=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(p[1]),original.get(Integer.parseInt(p[0])).block()));}
            var out=new StringBuilder("T15_CARD seed="+seed+" defaults="+Boolean.getBoolean("holdFuzz.defaults")+" blocks="+list.size()+"\n");
            for(var p:list) {
                var b=p.block();
                String control=switch(b.control()){case FlowControl.Passive ignored->"passive";
                    case FlowControl.Pump q->"new FlowControl.Pump("+q.targetVolumeFlow()+","+q.maximumAddedPressure()+","+q.efficiency()+")";
                    case FlowControl.Compressor c->"new FlowControl.Compressor("+c.targetVolumeFlow()+","+c.maximumPressureRatio()+","+c.efficiency()+")";
                    case FlowControl.PressureValve v->"new FlowControl.PressureValve("+v.targetPressure()+")";};
                String spec=b.spec()==null?"null":"spec("+b.fluid()+","+b.spec().temperature()+","+b.spec().pressure()+")";
                out.append("T15_PLACE @").append(p.tick()).append(" w.place(").append(b.x()).append(',').append(b.y()).append(',').append(b.z()).append(",Kind.").append(b.kind())
                        .append(',').append(b.facing()).append(',').append(control).append(',').append(spec).append(',').append(b.bore()).append(");\n");
            }
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
            for(var p:list){w.runTo(p.tick());w.place(p.block());}
            long end=list.getLast().tick()+100L*slices;
            while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
            if(w.held.isEmpty())out.append("T15_CLEAN seed=").append(seed).append(" tick=").append(w.ledger.onlineTick()).append('\n');
            else{var h=w.held.getFirst();out.append("T15_HELD seed=").append(seed).append(' ').append(h).append('\n').append(w.describe(h.island()));}
            for(var s:w.coordinator.observe())out.append("T15_END ").append(w.describe(s.id()));
            System.out.print(out);
        }
    }
}
