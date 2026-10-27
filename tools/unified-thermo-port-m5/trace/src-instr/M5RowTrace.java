package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.science.fluid.solver.PhaseLayout;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.thermo.PhaseSupport;
import java.lang.reflect.Field;
import java.util.*;

/**
 * M5 trace (tools/unified-thermo-port-m5/trace, never tracked): the consumer of the temporary hook
 * {@code PassiveStepSolver.M5_TRACE} (patches/instr-m5-trace-hooks-on-7bd5fafc.patch). On the thread that sets {@link #ON},
 * prints every active-set pass of the step solver (M5PASS: modes, closures, caps, phase codes per node), each converged pass
 * (M5CONV), each active-set change (M5CHANGE), a cycle (M5CYCLE) and each failed Newton (M5FAIL: the largest residual rows by
 * node, edge and row meaning at the failed iterate and at the pass's start point, and the node states at both).
 * Phase codes: 1 hydrocarbon liquid, 2 hydrocarbon vapour, 4 free water, 8 steam, 16 solids, 32 dry.
 */
public final class M5RowTrace {
    public static final ThreadLocal<Boolean> ON=ThreadLocal.withInitial(()->false);
    /** The network model, for component names (set by the probe). */
    public static FluidThermodynamics MODEL;
    public static int fails,passes,fullFails=Integer.getInteger("m5.fullFails",3),maxPassLines=Integer.getInteger("m5.passLines",400);
    /** Where the lines go while set (one solve buffered, printed by the probe if it is the one traced); else stdout. */
    public static final ThreadLocal<StringBuilder> OUT=new ThreadLocal<>();
    static void emit(Object line){var b=OUT.get();if(b==null)System.out.println(line);else b.append(line).append('\n');}
    public static void install() {
        PassiveStepSolver.M5_TRACE=M5RowTrace::accept;
        // M8: the phase-hold hook of patches/instr-m8-phase-hold-hook-on-482264fc.patch, where it is applied.
        try{FluidThermodynamics.class.getField("M8_HOLD").set(null,(java.util.function.Consumer<String>)M5RowTrace::hold);}
        catch(NoSuchFieldException|IllegalAccessException absent){}
    }
    public static int holds;
    /** M8HOLD: a typed phase hold FluidThermodynamics.adopt raises, with the code path that asked for the flash. */
    static void hold(String what) {
        if(!ON.get())return;
        if(++holds>Integer.getInteger("m8.holdLines",40))return;
        var b=new StringBuilder("M8HOLD #"+holds+" "+what+"\n  at");int frames=0;
        for(var f:new Throwable().getStackTrace()) {
            if(!f.getClassName().startsWith("com.wormzjl")||f.getClassName().contains("M5RowTrace"))continue;
            b.append(" ").append(f.getClassName().replaceAll(".*[.]","")).append(".").append(f.getMethodName()).append(":").append(f.getLineNumber());
            if(++frames>=16)break;
        }
        emit(b);
    }
    public static void reset(){fails=0;passes=0;holds=0;}
    static void accept(Object[] e) {
        if(!ON.get())return;
        String kind=(String)e[0];var eq=(PassiveStepSolver.Equations)e[1];int pass=(Integer)e[2];double dt=(Double)e[3];
        try {
            switch(kind) {
                case "pass"->{if(++passes>maxPassLines)return;
                    @SuppressWarnings("unchecked") var modes=(List<FlowControl.Mode>)e[4];var closed=(boolean[])e[5];var caps=(int[])e[6];var phases=(int[])e[7];
                    var b=new StringBuilder("M5PASS pass="+pass+" dt="+dt+" rate="+e[8]+" phases="+Arrays.toString(phases)+" caps="+Arrays.toString(caps)+" closed=[");
                    for(int i=0;i<closed.length;i++)if(closed[i])b.append(i).append(' ');
                    b.append("] modes=[");for(int i=0;i<modes.size();i++)b.append(i).append(':').append(abbreviate(modes.get(i))).append(' ');
                    b.append(']');
                    // M7: the static columns of the pass's start point on every edge with a height difference.
                    if(Boolean.getBoolean("m5.columns"))b.append("\n  columns at pass start: ").append(columns(eq,eq.initial()));
                    emit(b);}
                case "converged"->{if(passes>maxPassLines)return;var x=(double[])e[4];emit("M5CONV pass="+pass+" dt="+dt+" iterations="+e[5]+" "+brief(eq,x));}
                case "change"->{if(passes>maxPassLines)return;emit("M5CHANGE pass="+pass+" dt="+dt+" capChange="+e[4]+" capNext="+e[5]+" deviceChange="+e[6]+" deviceNext="+e[7]+" illegalDirection="+e[8]);}
                case "cycle"->emit("M5CYCLE pass="+pass+" dt="+dt);
                case "fail"->{var failure=(SparseNewton.Nonconvergence)e[4];fails++;
                    double[] x=failure.lastVariables();
                    var b=new StringBuilder("M5FAIL #"+fails+" pass="+pass+" dt="+dt+" "+failure.getMessage()+(failure.phaseHold()!=null?" [phase hold: "+failure.phaseHold().getMessage()+"]":""));
                    if(x==null){emit(b.append(" (no iterate)"));return;}
                    boolean full=fails<=fullFails||fails%8==0;
                    double[] r=eq.residual(x),r0=eq.residual(eq.initial());
                    b.append("\n  at failed iterate: ").append(rows(eq,r,full?10:4)).append("\n  at pass start:     ").append(rows(eq,r0,full?6:3));
                    if(full)b.append("\n").append(states(eq,eq.initial(),"start")).append(states(eq,x,"iterate")).append(edges(eq,x,r));
                    else b.append("\n  ").append(brief(eq,x));
                    emit(b);}
                default->{}
            }
        }catch(RuntimeException failure){emit("M5TRACE print failed: "+failure);}
    }
    static String abbreviate(FlowControl.Mode m){return switch(m.name()){case "PASSIVE"->"P";case "CLOSED"->"C";case "VELOCITY_LIMITED"->"VL";default->m.name();};}
    /** The largest rows of {@code r}, each with its node or edge and its meaning. */
    static String rows(PassiveStepSolver.Equations eq,double[] r,int count) {
        Integer[] order=new Integer[r.length];for(int k=0;k<r.length;k++)order[k]=k;
        Arrays.sort(order,(a,b)->Double.compare(Math.abs(r[b]),Math.abs(r[a])));
        double norm=0;for(double v:r)norm=Math.max(norm,Math.abs(v));
        var b=new StringBuilder("max|r|="+String.format(Locale.ROOT,"%.3e",norm));
        for(int k=0;k<Math.min(count,r.length);k++){int row=order[k];b.append(" [").append(label(eq,row)).append(" r=").append(String.format(Locale.ROOT,"%.3e",r[row])).append(']');}
        return b.toString();
    }
    static String label(PassiveStepSolver.Equations eq,int row) {
        for(int node=0;node<eq.layout.length;node++) {
            var layout=eq.layout[node];if(layout==null)continue;int off=eq.offsets[node];
            if(row>=off&&row<off+layout.size()){var labels=rowLabels(eq,node);int local=row-off;return "n"+node+(eq.graph.reservoirs().get(node).junction()?"J":"V")+" "+(local<labels.size()?labels.get(local):"row"+local);}
        }
        int pipes=eq.graph.pipes().size();
        if(row>=eq.edgeOffset&&row<eq.edgeOffset+pipes){int e=row-eq.edgeOffset;var p=eq.graph.pipes().get(e);return "e"+e+" "+p.first()+"->"+p.second()+" "+abbreviate(eq.modes.get(e))+" cap="+eq.capDirections[e];}
        for(int e=0;e<pipes;e++)if(eq.controlOffsets[e]==row)return "control e"+e;
        return "row"+row;
    }
    static Object field(Object target,String name) {
        try{Field f=PhaseLayout.class.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    static String name(PassiveStepSolver.Equations eq,int i){var names=MODEL.hydrocarbon.components();return i<names.size()?names.get(i):"water";}
    /** The meaning of each local row of a node's layout, in PhaseLayout.balanceRows/junctionRows then equilibriumResidual order. */
    static List<String> rowLabels(PassiveStepSolver.Equations eq,int node) {
        var layout=eq.layout[node];var labels=new ArrayList<String>();
        int[] components=(int[])field(layout,"components");var support=(PhaseSupport[])field(layout,"support");
        int wl=(Integer)field(layout,"waterLiquidIndex"),wv=(Integer)field(layout,"waterVaporIndex"),pp=(Integer)field(layout,"partialPressureIndex"),solid=(Integer)field(layout,"solidIndex");
        boolean liquid=(Boolean)field(layout,"liquidActive"),vapor=(Boolean)field(layout,"vaporActive");
        if(!eq.graph.reservoirs().get(node).junction()) {
            for(int i:components)labels.add("bal "+name(eq,i));
            if(wl>=0||wv>=0)labels.add("bal water");
            labels.add("energy");labels.add("wall");
        } else {
            int[] basis=(int[])field(layout,"junctionBasis");
            for(int k=0;k<basis.length-1;k++)labels.add("frac "+name(eq,basis[k]));
            labels.add("continuity");labels.add("enthalpy");labels.add("normalize");
        }
        if(solid>=0){labels.add("solid0");labels.add("solid1");labels.add("solid2");}
        if(liquid&&vapor)for(int i:components)if(support[i]==PhaseSupport.BOTH)labels.add("eq "+name(eq,i));
        if(wl>=0&&wv>=0)labels.add("water sat");
        if(pp>=0)labels.add("pp closure");
        return labels;
    }
    static String brief(PassiveStepSolver.Equations eq,double[] x) {
        var b=new StringBuilder();var st=eq.states(x);
        for(int n=0;n<st.size();n++){if(eq.layout[n]==null)continue;var s=st.get(n);b.append(String.format(Locale.ROOT,"n%d:T=%.3f,P=%.1f,L=%.3e,V=%.3e ",n,s.temperature(),s.pressure(),s.liquidVolume(),s.vaporVolume()));}
        b.append("flows=[");for(int e=0;e<eq.graph.pipes().size();e++)b.append(String.format(Locale.ROOT,"%.4g ",x[eq.edgeOffset+e]));
        return b.append(']').toString();
    }
    static String states(PassiveStepSolver.Equations eq,double[] x,String what) {
        var b=new StringBuilder();var st=eq.states(x);var names=MODEL.hydrocarbon.components();
        for(int n=0;n<st.size();n++) {
            if(eq.layout[n]==null)continue;var s=st.get(n);var node=eq.graph.reservoirs().get(n);
            b.append(String.format(Locale.ROOT,"  %s n%d id=%d %s z=%.0f T=%.4f P=%.2f m=%.5g V=%.5g Vliq=%.4g Vvap=%.4g Vwater=%.4g phases=%d %s%n",what,n,node.id(),node.kind(),node.elevation(),s.temperature(),s.pressure(),s.mass(),s.volume(),s.liquidVolume(),s.vaporVolume(),s.waterVolume(),FluidThermodynamics.phaseBits(s),labels(s)));
            b.append("    L[mol]=").append(amounts(s.liquidView(),names)).append(" V[mol]=").append(amounts(s.vaporView(),names)).append(" waterL=").append(s.waterLiquid()).append(" waterV=").append(s.waterVapor()).append('\n');
        }
        return b.toString();
    }
    static String edges(PassiveStepSolver.Equations eq,double[] x,double[] r) {
        var b=new StringBuilder();var st=eq.states(x);
        for(int e=0;e<eq.graph.pipes().size();e++){var p=eq.graph.pipes().get(e);
            b.append(String.format(Locale.ROOT,"  edge %d %d->%d %s cap=%d closed=%s flow=%.6g row=%.3e ports=%s/%s %s%n",e,p.first(),p.second(),abbreviate(eq.modes.get(e)),eq.capDirections[e],eq.boundaryClosed[e],x[eq.edgeOffset+e],r[eq.edgeOffset+e],p.firstPort(),p.secondPort(),column(eq,st,e)));}
        return b.toString();
    }
    // ---------------- M7 (2026-10-17): static columns and one-root labels ----------------
    /** The static column of edge {@code e} on states {@code st}: the pass's column end, its density (kg/m3) and head (Pa). */
    static String column(PassiveStepSolver.Equations eq,List<FluidThermodynamics.State> st,int e) {
        var p=eq.graph.pipes().get(e);double dz=eq.graph.reservoirs().get(p.second()).elevation()-eq.graph.reservoirs().get(p.first()).elevation();
        int end=eq.columnEnds[e];int node=end==0?p.first():p.second();var port=end==0?p.firstPort():p.secondPort();
        double rho;String kind;
        try {
            if(p.control() instanceof FlowControl.Mover){rho=reflect("endColumn",st.get(p.first()),p.firstPort(),Double.NaN);node=p.first();kind="mover";}
            else{rho=reflect("columnDensity",st.get(node),port,p.volume());kind="passive";}
        }catch(RuntimeException failure){return "column=? ("+failure.getMessage()+")";}
        return String.format(Locale.ROOT,"dz=%.0f colEnd=%d(n%d %s) rho_col=%.4g head=%.4g Pa",dz,end,node,kind,rho,rho*PassiveStepSolver.GRAVITY*dz);
    }
    static String columns(PassiveStepSolver.Equations eq,double[] x) {
        var b=new StringBuilder();var st=eq.states(x);
        for(int e=0;e<eq.graph.pipes().size();e++){var p=eq.graph.pipes().get(e);
            if(eq.graph.reservoirs().get(p.second()).elevation()==eq.graph.reservoirs().get(p.first()).elevation())continue;
            b.append("[e").append(e).append(' ').append(p.first()).append("->").append(p.second()).append(' ').append(column(eq,st,e)).append("] ");}
        return b.toString();
    }
    /** PassiveStepSolver's static column statements by reflection: columnDensity (D34 on) or endColumn (D36 on; endDensity before). */
    static double reflect(String name,FluidThermodynamics.State s,PassiveNetwork.PhasePort port,double volume) {
        try {
            if(name.equals("columnDensity")){var m=PassiveStepSolver.class.getDeclaredMethod("columnDensity",FluidThermodynamics.class,FluidThermodynamics.State.class,PassiveNetwork.PhasePort.class,double.class);m.setAccessible(true);return (Double)m.invoke(null,MODEL,s,port,volume);}
            java.lang.reflect.Method m;
            try{m=PassiveStepSolver.class.getDeclaredMethod("endColumn",FluidThermodynamics.class,FluidThermodynamics.State.class,PassiveNetwork.PhasePort.class);}
            catch(NoSuchMethodException beforeD36){m=PassiveStepSolver.class.getDeclaredMethod("endDensity",FluidThermodynamics.class,FluidThermodynamics.State.class,PassiveNetwork.PhasePort.class);}
            m.setAccessible(true);return (Double)m.invoke(null,MODEL,s,port);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(name+": "+failure,failure);}
    }
    /** Each hydrocarbon slot's density and its root's phase identification parameter and Z (the one-root label rule). */
    static String labels(FluidThermodynamics.State s) {
        var b=new StringBuilder();
        try {
            if(s.liquidVolume()>0)b.append(String.format(Locale.ROOT,"rhoL=%.4g %s ",MODEL.liquidDensity(s),pip(s.temperature(),s.pressure(),s.liquidView(),com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root.LIQUID)));
            if(s.vaporVolume()>0)b.append(String.format(Locale.ROOT,"rhoV=%.4g %s",MODEL.vaporDensity(s),pip(s.temperature(),s.hydrocarbonPartialPressure(),s.vaporView(),com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root.VAPOR)));
        }catch(RuntimeException failure){b.append("labels? ").append(failure.getMessage());}
        return b.toString();
    }
    static String pip(double t,double p,double[] amounts,com.wormzjl.createcheme.science.thermo.PengRobinsonKernel.Root root) {
        double total=0;for(double a:amounts)total+=a;if(!(total>0))return "";
        try {
            var m=com.wormzjl.createcheme.science.fluid.thermo.HydrocarbonModel.class.getDeclaredMethod("translated");m.setAccessible(true);
            var translated=(com.wormzjl.createcheme.science.fluid.thermo.TranslatedPengRobinson)m.invoke(MODEL.hydrocarbon);
            var values=translated.evaluateValues(t,p,amounts.clone(),root,translated.prepare(t));
            return String.format(Locale.ROOT,"(PIP=%.4f Z=%.4f roots=%d)",values.phaseIdentificationParameter(),values.compressibility(),values.physicalRootCount());
        }catch(ReflectiveOperationException|RuntimeException failure){return "(PIP? "+failure+")";}
    }
    static String amounts(double[] a,List<String> names) {
        var b=new StringBuilder("{");for(int i=0;i<a.length;i++)if(a[i]!=0)b.append(b.length()>1?",":"").append(i<names.size()?names.get(i):"c"+i).append('=').append(String.format(Locale.ROOT,"%.4g",a[i]));return b.append('}').toString();
    }
}
