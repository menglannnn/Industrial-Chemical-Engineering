package com.wormzjl.icheme.science.fluid.thermo;

import java.util.Locale;

/**
 * M9 trace (tools/unified-thermo-port-test-world, 2026-10-18; never tracked): the consumer of the temporary hook
 * {@code FluidThermodynamics.M9_CHECK} (unified-thermo-port-m5/trace/patches/instr-m9-phase-check-hook-on-00ee8385-after-m5.patch).
 * While M5RowTrace.ON is set on the thread, prints each phase check: the node's state, the solve's start ({@code from}),
 * the fold, the TP answer, the proposal, the own split (a vessel's closure or a junction's PH answer), the seed, the
 * refusal and the verdict. Amounts in mol, volumes in m3.
 */
public final class M9CheckTrace {
    private M9CheckTrace() {}
    public static int lines;
    static final int MAX=Integer.getInteger("m9.lines",300);
    public static void install(){FluidThermodynamics.M9_CHECK=M9CheckTrace::accept;lines=0;}
    static double sum(double[] a){double s=0;for(double x:a)s+=x;return s;}
    static String st(FluidThermodynamics.State s) {
        if(s==null)return "-";
        double l=sum(s.liquidView()),v=sum(s.vaporView());
        return String.format(Locale.ROOT,"{T=%.4f P=%.1f nL=%.6g nV=%.6g nW=%.4g nSteam=%.4g VL=%.6g VV=%.6g xCO2L=%.4g yCO2V=%.4g}",s.temperature(),s.pressure(),l,v,s.waterLiquid(),s.waterVapor(),
                s.liquidVolume(),s.vaporVolume(),l>0&&s.liquidView().length>3?s.liquidView()[3]/l:Double.NaN,v>0&&s.vaporView().length>3?s.vaporView()[3]/v:Double.NaN);
    }
    static void accept(Object[] e) {
        if(!com.wormzjl.icheme.science.fluid.network.M5RowTrace.ON.get())return;
        if(++lines>MAX)return;
        var state=(FluidThermodynamics.State)e[0];var from=(FluidThermodynamics.State)e[1];var kept=(FluidThermodynamics.State)e[2];var tp=(FluidThermodynamics.State)e[3];var start=(FluidThermodynamics.State)e[4];var own=(FluidThermodynamics.State)e[5];var seed=(FluidThermodynamics.State)e[6];
        int proposed=(Integer)e[7],refused=(Integer)e[8],excluded=(Integer)e[9];var checked=(FluidThermodynamics.State)e[10];boolean junction=(Boolean)e[11];int shrinking=(Integer)e[12],at=(Integer)e[13];
        String verdict=checked==state?"same":checked==kept?"folded":"CHANGED";
        System.out.println("M9CHECK #"+lines+" node="+at+(junction?" junction":" vessel")+" shrinking="+shrinking+" excluded="+excluded+" proposed="+proposed+" refused="+refused+" -> "+verdict
                +"\n   state  ="+st(state)+"\n   from   ="+st(from)+(kept!=state?"\n   kept   ="+st(kept):"")+(tp!=null?"\n   tp     ="+st(tp):"")
                +(start!=null?"\n   start  ="+st(start):"")+(own!=null?"\n   own    ="+st(own):"")+(seed!=null?"\n   seed   ="+st(seed):"")+(checked!=state&&checked!=kept?"\n   checked="+st(checked):""));
    }
}
