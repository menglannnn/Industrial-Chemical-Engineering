package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Straight lines at z = 0 along +x, every block placed in one tick (100) with the world's defaults, run 3000 ticks
 * (60 slices at the 100-tick cadence): which layouts around a pump hold. Letters: G generator, P pipe, M pump (east),
 * F filter (east), V valve (east), O void, T tank.
 */
class F8LayoutProbeTest {
    static Kind kind(char c){return switch(c){case 'G'->Kind.GENERATOR;case 'P'->Kind.PIPE;case 'M'->Kind.PUMP;case 'F'->Kind.FILTER;case 'V'->Kind.VALVE;case 'O'->Kind.VOID;case 'T'->Kind.RESERVOIR;case 'C'->Kind.COMPRESSOR;default->throw new IllegalArgumentException(""+c);};}
    @Test void layouts() {
        var outer=new InlineFilterLineHoldTest();
        var summary=new ArrayList<String>();
        for(String layout:List.of("GPMPVPO","GPMPFPO","GPMPF","GPMPV","GPMPVP","GPMPFPVPO","GPMPFP","GPMP","GPMPPO","TPMPVPT","GPMPFPT","GPMPVPT","GPMPFPVPT")) {
            var w=outer.new World();w.runTo(100);
            for(int i=0;i<layout.length();i++){var k=kind(layout.charAt(i));w.place(i,0,0,k,k==Kind.PUMP||k==Kind.FILTER||k==Kind.VALVE||k==Kind.COMPRESSOR?com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.EAST:com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.NORTH);}
            w.runTo(3100);
            String line=layout+": holds="+w.held.size()+(w.held.isEmpty()?"":" first="+w.held.getFirst().tick()+" "+w.held.getFirst().status());
            for(var s:w.coordinator.observe())line+=" | island "+s.id()+" committed="+s.clock().committedTick()+" modes="+s.lastResult().map(r->r.endpointModes().toString()).orElse("-");
            summary.add(line);System.out.println("F8_LAYOUT "+line);
        }
        summary.forEach(s->System.out.println("F8_LAYOUT_SUMMARY "+s));
    }
}
