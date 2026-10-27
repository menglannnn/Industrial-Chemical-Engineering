package com.wormzjl.createcheme.science.fluid;

import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.Locale;
import java.util.function.Function;

/**
 * Trace switch of the M3 cold-arrival trace (tools/unified-thermo-port-m3-arrival, never tracked): added to an exported
 * tree under /tmp/m3/trace-<commit> only. The harness turns {@link #on} for the online ticks of the held interval
 * (system properties m3arr.from and m3arr.to); every print site tests it first. Single-threaded use only (one seed, one
 * worker).
 */
public final class M3ArrivalTrace {
    private M3ArrivalTrace(){}
    public static volatile boolean on;
    /** The step solver's decoder of a Newton vector into per-node temperature and pressure, set per pass. */
    public static volatile Function<double[],String> describer;
    private static int lines;
    private static final int MAX=Integer.getInteger("m3arr.max",60000);
    public static void p(String format,Object... args) {
        if(!on||lines>=MAX)return;
        lines++;
        System.out.println("M3ARR "+String.format(Locale.ROOT,format,args));
    }
    public static String describe(double[] x) {
        var d=describer;
        if(d==null||x==null)return "-";
        try{return d.apply(x);}catch(RuntimeException failure){return "describe failed: "+failure.getMessage();}
    }
    public static String v(double[] values) {
        if(values==null)return "null";
        var b=new StringBuilder("[");
        for(int i=0;i<values.length;i++){if(i>0)b.append(',');b.append(String.format(Locale.ROOT,"%.4g",values[i]));}
        return b.append(']').toString();
    }
    public static String shorten(String s){return s==null?"null":s.length()>240?s.substring(0,240)+"...":s;}
    /** A node state: T, P, the phase volumes, energy, mass, phase bits (1 liquid, 2 vapour, 4 free water, 8 steam), amounts. */
    public static String state(FluidThermodynamics.State s) {
        if(s==null)return "null";
        return String.format(Locale.ROOT,"T=%.5f P=%.1f bits=%d liqV=%.4e vapV=%.4e V=%.5e U=%.6e H=%.6e m=%.6g l=%s v=%s w=%.3g/%.3g",
                s.temperature(),s.pressure(),FluidThermodynamics.phaseBits(s),s.liquidVolume(),s.vaporVolume(),s.volume(),s.internalEnergy(),s.enthalpy(),s.mass(),
                v(s.liquidView()),v(s.vaporView()),s.waterLiquid(),s.waterVapor());
    }
    public static String graph(PassiveNetwork g) {
        var b=new StringBuilder();
        for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);b.append("\n    node ").append(i).append(" id=").append(n.id()).append(' ').append(n.kind()).append(' ').append(state(n.state()));}
        for(int i=0;i<g.pipes().size();i++){var p=g.pipes().get(i);b.append("\n    pipe ").append(i).append(' ').append(p.first()).append("->").append(p.second()).append(' ').append(p.control()).append(" area=").append(p.minimumArea()).append(" blocked=").append(p.blockedDirections());}
        return b.toString();
    }
}
