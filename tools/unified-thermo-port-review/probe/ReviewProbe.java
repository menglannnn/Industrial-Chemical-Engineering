package com.wormzjl.createcheme.science.fluid;

import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Review probe (tools/unified-thermo-port-review, batch 2026-09-24-coolprop-low-temperature): attributes every
 * {@code FluidThermodynamics.phaseCheck} call, and the Newton iterations its own split spends, to the path of call sites
 * that asked (step pass failed iterate, step pass converged point, polish, error probe, vessel closure failed iterate or
 * converged candidate, the crystal refresh), and the Newton iterations spent under each label. Probe 2 adds what the TP
 * answer added (a stand-in liquid of crystal species only, or a phase of its own) and how the vessel's own split ended
 * (converged, last iterate repartitioned, refused). Printed at JVM exit with the prefix RPROBE. Added to an exported copy
 * only (probe/apply-probe.pl, apply-probe2.pl); never part of the worktree.
 */
public final class ReviewProbe {
    private record Frame(String label,long start) {}
    private static final ThreadLocal<ArrayDeque<Frame>> STACK=ThreadLocal.withInitial(ArrayDeque::new);
    /** phaseCheck rows by site path: calls, complete, tpNothing, ownRuns, fallbacks, ownNewtonIterations, adopted, discarded. */
    private static final TreeMap<String,long[]> ROWS=new TreeMap<>();
    /** per label: entries, Newton iterations inside. */
    private static final TreeMap<String,long[]> LABELS=new TreeMap<>();
    private static final TreeMap<String,long[]> SPLITS=new TreeMap<>(),ANSWERS=new TreeMap<>();
    private static boolean hooked;
    private ReviewProbe() {}
    private static synchronized void hook() {if(!hooked){hooked=true;Runtime.getRuntime().addShutdownHook(new Thread(ReviewProbe::print));}}
    public static long iterations() {return SolverDiagnostics.newtonIterations.sum();}
    public static void push(String label) {hook();STACK.get().push(new Frame(label,iterations()));}
    public static void pop() {
        var f=STACK.get().pop();
        synchronized(LABELS){var r=LABELS.computeIfAbsent(f.label(),k->new long[2]);r[0]++;r[1]+=iterations()-f.start();}
    }
    public static <T> T in(String label,Supplier<T> body) {push(label);try{return body.get();}finally{pop();}}
    private static String site(boolean junction) {
        var sb=new StringBuilder();var it=STACK.get().descendingIterator();
        while(it.hasNext()){if(sb.length()>0)sb.append('>');sb.append(it.next().label());}
        if(sb.length()==0)sb.append("(none)");
        return sb.append(junction?" J":" V").toString();
    }
    private static long[] row(boolean junction) {synchronized(ROWS){return ROWS.computeIfAbsent(site(junction),k->new long[8]);}}
    public static void complete(boolean junction) {hook();var r=row(junction);synchronized(ROWS){r[0]++;r[1]++;}}
    public static void tpNothing(boolean junction) {hook();var r=row(junction);synchronized(ROWS){r[0]++;r[2]++;}}
    public static void own(boolean junction,boolean fallback,long before) {hook();var r=row(junction);synchronized(ROWS){r[0]++;r[3]++;if(fallback)r[4]++;r[5]+=iterations()-before;}}
    public static void outcome(boolean junction,boolean adopted) {var r=row(junction);synchronized(ROWS){if(adopted)r[6]++;else r[7]++;}}
    /** Probe 2: the TP answer that asked for a phase: whether its liquid is the stand-in (crystal species only beside a
     * vapour), whether the node holds solids, and the phase bits added (1 liquid, 2 vapour, 4 water liquid, 8 steam). */
    public static void answer(boolean junction,boolean standIn,boolean solids,int added) {
        hook();String k=site(junction)+" standIn="+standIn+" solids="+solids+" added="+added;
        synchronized(ANSWERS){ANSWERS.computeIfAbsent(k,x->new long[1])[0]++;}
    }
    /** Probe 2: how InventoryEquilibrium.split ended. */
    public static void split(String how) {hook();synchronized(SPLITS){SPLITS.computeIfAbsent(how,x->new long[1])[0]++;}}
    public static String msg(Throwable t) {var m=String.valueOf(t.getMessage()).replaceAll("[0-9][0-9.eE+-]*","#");return m.length()>80?m.substring(0,80):m;}
    /** Probe 2b: a stalled own closure, bucketed: the failure kind, the decade of its final residual, the decade of the vapour
     * volume share of its last iterate (over the vessel capacity), and whether the inventory holds solids. */
    public static String stall(Throwable t,com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.State last,com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.Inventory inventory) {
        String m=String.valueOf(t.getMessage());var k=java.util.regex.Pattern.compile("residual ([0-9.eE+-]+)").matcher(m);
        String res=k.find()?"res~1e"+(int)Math.floor(Math.log10(Double.parseDouble(k.group(1)))):"res?";
        double vs=last.vaporVolume()/inventory.volume(),ls=last.liquidVolume()/inventory.volume(),ss=inventory.solids().volume()/inventory.volume();
        String dec=vs<=0?"vap=0":"vap~1e"+(int)Math.floor(Math.log10(vs));String liq=ls<=0?"liq=0":"liq~1e"+(int)Math.floor(Math.log10(ls));
        double room=1-(last.liquidVolume()+last.vaporVolume()+last.waterVolume()+inventory.solids().volume())/inventory.volume();String rm=room<=0?"room<=0":"room~1e"+(int)Math.floor(Math.log10(room));
        double vm=(com.wormzjl.createcheme.science.fluid.solver.PhaseLayout.sum(last.vaporView())+last.waterVapor())/Math.max(1e-300,com.wormzjl.createcheme.science.fluid.solver.PhaseLayout.sum(com.wormzjl.createcheme.science.fluid.solver.PhaseLayout.totalAmounts(last)));String vmol=vm<=0?"vapMol=0":"vapMol~1e"+(int)Math.floor(Math.log10(vm));
        return (m.startsWith("Newton iteration limit")?"iterLimit":m.startsWith("Newton line search")?"lineSearch":"other")+" "+res+" "+dec+" "+vmol+" "+liq+" "+rm+" solidsShare~"+(ss<=0?"0":"1e"+(int)Math.floor(Math.log10(ss)));
    }
    public static void print() {
        synchronized(ROWS){synchronized(LABELS){synchronized(SPLITS){synchronized(ANSWERS){
            System.out.println("RPROBE phaseCheck by site path (V vessel, J junction) | calls complete tpNothing ownRuns fallbacks ownNewtonIterations adopted discarded");
            for(var e:ROWS.entrySet())System.out.println("RPROBE "+e.getKey()+" | "+Arrays.toString(e.getValue()));
            System.out.println("RPROBE label | entries newtonIterationsInside");
            for(var e:LABELS.entrySet())System.out.println("RPROBE "+e.getKey()+" | "+Arrays.toString(e.getValue()));
            System.out.println("RPROBE TP answers that asked for a phase | count");
            for(var e:ANSWERS.entrySet())System.out.println("RPROBE "+e.getKey()+" | "+e.getValue()[0]);
            System.out.println("RPROBE own split outcomes | count");
            for(var e:SPLITS.entrySet())System.out.println("RPROBE "+e.getKey()+" | "+e.getValue()[0]);
            System.out.println("RPROBE totalNewtonIterations="+iterations());
        }}}}
    }
}
