package com.wormzjl.createcheme.science.fluid;

import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.TreeMap;

/**
 * Forms probe (tools/unified-thermo-port-forms, batch 2026-09-24-coolprop-low-temperature): how a vessel's own split
 * (InventoryEquilibrium.split) ends when its closure stops short. For each stalled closure: the vapour mole share (over
 * the fluid moles) at the start, at the last iterate before the restatement and after it (the trace fold), in quarter
 * decades, the residual decade, and where 24 more iterations of the same closure from the last iterate take the vapour
 * (the closure's direction). Printed at JVM exit with the prefix FPROBE. Added to an exported copy only
 * (probe/apply-probe.pl); never part of the worktree. The continuation runs with the solver counters off.
 */
public final class FormsProbe {
    private static final TreeMap<String,long[]> ROWS=new TreeMap<>();
    private static boolean hooked;
    private FormsProbe() {}
    private static synchronized void hook(){if(!hooked){hooked=true;Runtime.getRuntime().addShutdownHook(new Thread(FormsProbe::print));}}
    public static void add(String key){hook();synchronized(ROWS){ROWS.computeIfAbsent(key,k->new long[1])[0]++;}}
    /** Quarter-decade bucket of a share: "0", or the floor of log10 to 0.25. */
    public static String q(double share) {
        if(!(share>0))return "0";
        return String.format(java.util.Locale.ROOT,"%.2f",Math.floor(Math.log10(share)*4)/4);
    }
    public static double vapourShare(FluidThermodynamics.State s) {
        double total=PhaseLayout.sum(PhaseLayout.totalAmounts(s));
        return total>0?(PhaseLayout.sum(s.vaporView())+s.waterVapor())/total:0;
    }
    public static double liquidShare(FluidThermodynamics.State s) {
        double total=PhaseLayout.sum(PhaseLayout.totalAmounts(s));
        return total>0?(PhaseLayout.sum(s.liquidView())+s.waterLiquid())/total:0;
    }
    public static String residual(Throwable t) {
        var k=java.util.regex.Pattern.compile("residual ([0-9.eE+-]+)").matcher(String.valueOf(t.getMessage()));
        return k.find()?"res1e"+(int)Math.floor(Math.log10(Double.parseDouble(k.group(1)))):"res?";
    }
    /** The call path of {@code t}: the first ten project frames, "Class.method:line" joined by " < ". */
    public static String where(Throwable t) {
        var b=new StringBuilder();int n=0;
        for(var e:t.getStackTrace()){if(!e.getClassName().startsWith("com.wormzjl")||e.getClassName().endsWith("FormsProbe"))continue;
            if(n++>0)b.append(" < ");String c=e.getClassName();b.append(c.substring(c.lastIndexOf('.')+1)).append('.').append(e.getMethodName()).append(':').append(e.getLineNumber());if(n==10)break;}
        return b.toString();
    }
    private static final java.util.HashMap<String,Integer> FIRST=new java.util.HashMap<>();
    /** Prints {@code line} at once (prefix FPROBE-FIRST) for the first three occurrences of {@code key}. */
    public static void first(String key,String line){synchronized(FIRST){int k=FIRST.merge(key,1,Integer::sum);if(k<=3)System.out.println("FPROBE-FIRST "+key+" #"+k+": "+line);}}
    /** Form 4 (ii)'s direction criterion on a stalled closure from {@code start} to {@code end}, per fluid phase that
     * either holds: "vanishing" where {@code end} holds it under 1e-6 of its fluid amount and a smaller share than
     * {@code start}, "stands" otherwise, with the end share's quarter decade. */
    public static String verdict(FluidThermodynamics.State start,FluidThermodynamics.State end) {
        double[] s={PhaseLayout.sum(start.liquidView()),PhaseLayout.sum(start.vaporView())},e={PhaseLayout.sum(end.liquidView()),PhaseLayout.sum(end.vaporView())};
        double ts=PhaseLayout.sum(PhaseLayout.totalAmounts(start)),te=PhaseLayout.sum(PhaseLayout.totalAmounts(end));
        var b=new StringBuilder();String[] names={"liquid","vapour"};
        for(int k=0;k<2;k++){if(s[k]==0&&e[k]==0)continue;boolean v=e[k]<1e-6*te&&e[k]*ts<s[k]*te;
            b.append(names[k]).append(v?" vanishing":" stands").append(" end ").append(q(e[k]/te)).append(" start ").append(q(s[k]/ts)).append("; ");}
        return b.toString();
    }
    public static void print() {
        synchronized(ROWS) {
            System.out.println("FPROBE key | count");
            for(var e:ROWS.entrySet())System.out.println("FPROBE "+e.getKey()+" | "+e.getValue()[0]);
        }
    }
}
