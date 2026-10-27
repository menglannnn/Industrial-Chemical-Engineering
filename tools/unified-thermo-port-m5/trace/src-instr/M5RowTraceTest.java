package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.M5RowTrace;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * M5 trace (tools/unified-thermo-port-m5/trace, never tracked; needs patches/instr-m5-trace-hooks-on-7bd5fafc.patch and
 * -Pm5.instr=true): replays worlds and prints one island solve with the step solver's passes, changes and failed Newtons
 * ({@link M5RowTrace}), its substep attempts and its network at the interval's start.
 * {@code -Dreplay.cases="case|case..."}, a case {@code seed[/keep][#island:start][!off]}: keep is a minimizer KEEP list (',' or
 * ';' between items); without {@code #island:start} the first solve that holds is printed; {@code !off} sets the coordinator budgets
 * out of the way (600 s) for that case. {@code -Dreplay.slices} (40).
 */
class M5RowTraceTest {
    @Test void trace() {
        M5RowTrace.install();
        for(var c:System.getProperty("replay.cases","322").split("\\|"))M5TraceWorld.within(()->traceCase(c.trim()));
    }
    /** The TP answer at each vessel's own temperature, pressure and total amounts: which phases its state should hold. */
    static String tpAnswers(com.wormzjl.createcheme.science.fluid.network.PassiveNetwork g,com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics model) {
        var b=new StringBuilder();
        for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);if(n.kind()!=com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.NodeKind.RESERVOIR&&!n.junction())continue;var st=n.state();
            try{var tp=model.flashTP(st.temperature(),st.pressure(),com.wormzjl.createcheme.science.fluid.solver.PhaseLayout.totalAmounts(st),()->{});
                b.append(String.format(java.util.Locale.ROOT,"    TP n%d %s T=%.4f P=%.2f: state phases=%d Vliq=%.4e | TP answer phases=%d Vliq=%.4e Vvap=%.4e%n",i,n.kind(),st.temperature(),st.pressure(),com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.phaseBits(st),st.liquidVolume(),com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.phaseBits(tp),tp.liquidVolume(),tp.vaporVolume()));}
            catch(RuntimeException failure){b.append("    TP n"+i+" failed: "+failure+"\n");}}
        return b.toString();
    }
    void traceCase(String c) {
        boolean off=c.endsWith("!off");if(off)c=c.substring(0,c.length()-4);
        long island=-1,start=-1;
        String head=c;
        int target=c.lastIndexOf('#');
        if(target>0){var t=c.substring(target+1).split(":");island=Long.parseLong(t[0]);start=Long.parseLong(t[1]);head=c.substring(0,target);}
        String keep=null;long seed;
        if(head.contains("/")){seed=Long.parseLong(head.substring(0,head.indexOf('/')));keep=head.substring(head.indexOf('/')+1).replace(';',',');}
        else seed=Long.parseLong(head);
        String wall=System.getProperty("holdFuzz.wallNanos"),soft=System.getProperty("holdFuzz.softNanos");
        if(off){System.setProperty("holdFuzz.wallNanos","600000000000");System.setProperty("holdFuzz.softNanos","590000000000");}
        try {
            var list=M5SolveLogTest.list(seed,keep);var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),true);M5RowTrace.MODEL=w.model;
            System.out.println("M5TRACE case="+c+(off?" budgets=off":"")+" seed="+seed+" layout "+M5MinimizerTest.render(list));
            SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;
            boolean[] done={false};int[] attemptsBefore={0};String[] network={null};long fixedIsland=island,fixedStart=start;
            w.before=(a,cmd)->{
                if(done[0]||fixedIsland>0&&(a.islandId()!=fixedIsland||a.slice().startTick()!=fixedStart))return;
                M5RowTrace.reset();M5RowTrace.OUT.set(new StringBuilder());M5RowTrace.ON.set(true);attemptsBefore[0]=SolverDiagnostics.attempts().size();
                network[0]=M5TraceWorld.describe(cmd.snapshot(),w.model)+tpAnswers(cmd.snapshot(),w.model);
            };
            boolean history=Boolean.getBoolean("replay.history"),all=Boolean.getBoolean("replay.historyAll");
            w.after=s->{
                // replay.historyAll (M7): every vessel and junction (T to 1e-4 K, P to 0.01 Pa, mass, solids, phase bits) and the
                // interval's average flows, so two commits' trajectories can be diffed interval by interval.
                if(history&&s.result()!=null)s.result().candidate().ifPresent(r->{var b=new StringBuilder("M5HIST island="+s.island()+" ["+s.start()+","+s.end()+"] acc="+s.accepted()+" rej="+s.rejected());
                    var g=r.graph();for(int i=0;i<g.reservoirs().size();i++){var n=g.reservoirs().get(i);var kind=n.kind();var st=n.state();
                        if(all&&(kind==com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.NodeKind.RESERVOIR||n.junction()))b.append(String.format(java.util.Locale.ROOT," n%d%s:T=%.4f,P=%.2f,m=%.6g,Vliq=%.4e,sol=%.4g,ph=%d",i,n.junction()?"J":"V",st.temperature(),st.pressure(),st.mass(),st.liquidVolume(),st.solids().massKg(),com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.phaseBits(st)));
                        else if(!all&&kind==com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.NodeKind.RESERVOIR)b.append(String.format(java.util.Locale.ROOT," n%d:T=%.2f,P=%.0f,Vliq=%.3e,phases=%d",i,st.temperature(),st.pressure(),st.liquidVolume(),com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics.phaseBits(st)));}
                    if(all){b.append(" flows=[");for(double f:r.averageMassFlows())b.append(String.format(java.util.Locale.ROOT,"%.6g ",f));b.append(']');}
                    System.out.println(b);});
                if(history&&s.result()!=null&&s.result().candidate().isEmpty())System.out.println("M5HIST island="+s.island()+" ["+s.start()+","+s.end()+"] no candidate: "+s.detail());
                if(!M5RowTrace.ON.get())return;
                M5RowTrace.ON.set(false);var buffer=M5RowTrace.OUT.get();M5RowTrace.OUT.remove();
                if(fixedIsland<0&&!s.detail().startsWith("HELD"))return;
                done[0]=true;
                var attempts=SolverDiagnostics.attempts();var b=new StringBuilder("M5SOLVE island="+s.island()+" ["+s.start()+","+s.end()+"] checks="+s.checkpoints()+" ms="+s.nanos()/1_000_000
                        +" acc="+s.accepted()+" rej="+s.rejected()+" reasons="+s.reasons()+" detail="+s.detail()+"\nM5NET at the interval start\n"+network[0]+"M5ATTEMPTS");
                for(int i=attemptsBefore[0];i<attempts.size();i++){var t=attempts.get(i);b.append(String.format(Locale.ROOT," [%d %.4g %s %s %.3g]",t.index(),t.step(),t.accepted()?"ok":"rej",t.dominant(),t.error()));}
                System.out.println(b);System.out.print(buffer);System.out.println("M5END island="+s.island()+" passes="+M5RowTrace.passes+" fails="+M5RowTrace.fails);
            };
            var refused=w.placeAll(list);for(var r:refused)System.out.println("M5TRACE REFUSED "+r);
            long end=list.getLast().tick()+100L*Integer.getInteger("replay.slices",40);
            // replay.after (M7): keep running this many ticks after the traced solve (its M5HIST lines; nothing else is traced).
            long after=Long.getLong("replay.after",0);while(w.ledger.onlineTick()<end&&!done[0])w.tick();
            for(long stop=w.ledger.onlineTick()+after;done[0]&&w.ledger.onlineTick()<Math.min(end,stop);)w.tick();
            SolverDiagnostics.ENABLED=false;
            for(var h:w.held)System.out.println("M5TRACE HELD "+h);
            if(!done[0])System.out.println("M5TRACE case="+c+": no solve matched");
        } finally {
            if(off){if(wall==null)System.clearProperty("holdFuzz.wallNanos");else System.setProperty("holdFuzz.wallNanos",wall);if(soft==null)System.clearProperty("holdFuzz.softNanos");else System.setProperty("holdFuzz.softNanos",soft);}
        }
    }
}
