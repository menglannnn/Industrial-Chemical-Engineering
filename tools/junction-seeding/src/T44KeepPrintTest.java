package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.FlowControl;
import com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import org.junit.jupiter.api.Test;

/**
 * t44b (2026-10-19): prints a fuzz seed's kept placements ({@code -Dprint.seed}, {@code -Dprint.keep} "i@tick,..." from a
 * minimizer KEEP line, default the whole seed) as {@code EventPathWorld.place} lines with the exact specs, for a gate
 * test's transcription. Compiled on the javac lane beside the hold-fuzz harness and the M5 trace sources
 * ({@code tools/unified-thermo-port-m5/trace/src}); the cold harness with {@code -DholdFuzz.cold=true}.
 */
class T44KeepPrintTest {
    @Test void print() {
        if(Boolean.getBoolean("holdFuzz.cold")) {
            var catalog=PilotCryogenicTestCatalog.catalog();var network=FluidMaterialCatalog.networkPackage(catalog);
            MaterialRuntime.with(catalog,network,()->{body();return null;});
        } else body();
    }
    private static void body() {
        long seed=Long.getLong("print.seed",15);String keep=System.getProperty("print.keep");
        var list=M5SolveLogTest.list(seed,keep);
        System.out.println("T44KEEP seed="+seed+" keep="+keep+" layout "+M5MinimizerTest.render(list));
        for(var p:list) {
            var b=p.block();String control;
            if(b.control() instanceof FlowControl.Passive)control="passive";
            else if(b.control() instanceof FlowControl.PressureValve v)control="new FlowControl.PressureValve("+v.targetPressure()+")";
            else if(b.control() instanceof FlowControl.Pump u)control="new FlowControl.Pump("+u.targetVolumeFlow()+","+u.maximumAddedPressure()+")";
            else if(b.control() instanceof FlowControl.Compressor c)control="new FlowControl.Compressor("+c.targetVolumeFlow()+","+c.maximumPressureRatio()+")";
            else control=String.valueOf(b.control());
            String spec=b.spec()==null?"null":"new FluidDeviceSpec("+b.spec().volume()+","+b.spec().temperature()+","+b.spec().pressure()+",new double[]{"+exact(b.spec().composition())+"}"
                    +(b.spec().solids()==null?"":",/* solids */ "+b.spec().solids())+") /* "+b.fluid()+" */";
            System.out.println("T44PLACE tick="+p.tick()+" w.place("+b.x()+","+b.y()+","+b.z()+",Kind."+b.kind()+","+b.facing()+","+control+","+spec+","+b.bore()+");");
        }
    }
    private static String exact(double[] v) {
        var sb=new StringBuilder();
        for(double d:v){if(sb.length()>0)sb.append(',');sb.append(Double.toString(d));}
        return sb.toString();
    }
}
