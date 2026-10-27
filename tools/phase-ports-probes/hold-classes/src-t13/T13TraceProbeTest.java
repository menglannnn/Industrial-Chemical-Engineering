package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.PassiveStepSolver;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

/**
 * t13 (2026-10-13): the E6 card rebuilt by mechanism. Needs src/instr-t13-failing-rows.patch applied (working tree only,
 * never commit): every failed Newton of a step pass is reported to a per-thread sink with its residual history, its worst
 * rows named by equation, every node's state and every connection's drive, loss, scale and cap drops.
 *
 * <p>{@code trace}: -Dt13seeds=1,19 (holdFuzz.defaults for the distribution) [-Dt13keep=i@tick,... for one seed's
 * minimized list], -Dt13out=dir. Replays each seed alone, tick by tick as HoldFuzzTest.run does, to its first hold, and
 * writes the hold tick's reports to {@code <dir>/<d|r>-<seed>.txt} with a T13_SIG line (the signature the minimizer keeps).
 *
 * <p>{@code minimize}: -Dt13min=seed -Dt13sig=regex. Greedy block removal (as HoldMinimizerTest) keeping a first hold of
 * class E6 whose signature matches the regex. Not part of any gate suite.
 */
class T13TraceProbeTest {
    record Trace(String cls,HoldFuzzTest.Held held,String signature,List<String> reports,String island,long tick,String formed) {}

    static Trace run(List<HoldFuzzTest.Placement> placements,int slices) {
        var reports=new ArrayList<String>();PassiveStepSolver.T13_ON=true;
        PassiveStepSolver.T13_SINK.set(reports::add);SparseNewton.T13_HIST.set(new StringBuilder());SparseNewton.T13_X.set(new ArrayList<>());
        var formed=new HashMap<Long,long[]>();
        var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
        try {
            long end=placements.getLast().tick()+100L*slices;int next=0;
            while(w.ledger.onlineTick()<end) {
                // As HoldFuzzTest.run: runTo(p.tick) then place, so a placement lands after the ticks before it.
                while(next<placements.size()&&w.ledger.onlineTick()>=placements.get(next).tick()){try{w.place(placements.get(next).block());}catch(IllegalArgumentException refused){}next++;}
                reports.clear();w.tick();
                for(var s:w.coordinator.observe())formed.putIfAbsent(s.id(),new long[]{w.ledger.onlineTick(),s.clock().committedTick()});
                if(!w.held.isEmpty()) {
                    var h=w.held.getFirst();var cls=HoldMinimizerTest.cls(h.status());var f=formed.get(h.island());
                    String sig=signature(cls,reports)+(f!=null&&f[1]==h.committed()?" first":" later")+flags(reports);
                    return new Trace(cls,h,sig,List.copyOf(reports),w.describe(h.island()),w.ledger.onlineTick(),f==null?"?":"seen="+f[0]+" startCommitted="+f[1]);
                }
            }
            return new Trace("clean",null,"clean",List.of(),"",w.ledger.onlineTick(),"");
        }catch(Throwable t){return new Trace("X",null,"X "+t,List.of(),"",w.ledger.onlineTick(),"");}
        finally{PassiveStepSolver.T13_SINK.remove();SparseNewton.T13_HIST.remove();SparseNewton.T13_X.remove();}
    }
    static final Pattern ROW=Pattern.compile("\\[(flow e(\\d+)|control e\\d+|filter e\\d+|node (\\d+) (\\S+)) r=");
    /** The hold's signature: the last iteration-limit report of the hold tick (the final step size), its worst row's equation
     * and the kind of node (or the two ends of the connection) it belongs to. */
    static String signature(String cls,List<String> reports) {
        String last=null;for(var r:reports)if(r.contains("msg=Newton iteration limit"))last=r;
        if(last==null)return cls+" no-limit-report";
        var lines=last.split("\n");String rows=Arrays.stream(lines).filter(l->l.startsWith("  rows:")).findFirst().orElse("");
        var m=ROW.matcher(rows);if(!m.find())return cls+" ?";
        String what;
        if(m.group(2)!=null) {
            int e=Integer.parseInt(m.group(2));String pipe=Arrays.stream(lines).filter(l->l.startsWith("  pipe "+e+" ")).findFirst().orElse("");
            var ends=Pattern.compile("pipe \\d+ (\\d+)->(\\d+)").matcher(pipe);String kinds="?";
            if(ends.find())kinds=kind(lines,ends.group(1))+"-"+kind(lines,ends.group(2));
            what="flow "+kinds;
        } else if(m.group(3)!=null)what=kind(lines,m.group(3))+" "+m.group(4).replaceAll("c\\d+","c");
        else what=m.group(1).replaceAll(" e\\d+","");
        var res=Pattern.compile("residual (\\S+)").matcher(lines[0]);
        return cls+" "+what+" res="+(res.find()?res.group(1):"?");
    }
    /** The group flags of the last iteration-limit report: " forbid" (a connection flows the way its boundary forbids: a void
     * supplying, a generator receiving), " capped" (the worst row's connection sits on its velocity cap in three quarters of
     * the Newton's iterates or more), " mixed" (its two ends' columns drive opposite ways: a standing interface in real life). */
    static String flags(List<String> reports) {
        String last=null;for(var r:reports)if(r.contains("msg=Newton iteration limit"))last=r;
        if(last==null)return "";
        var lines=last.split("\n");var kinds=new HashMap<String,Character>();var b=new StringBuilder();
        for(var l:lines){var m=Pattern.compile("^  node (\\d+) id=-?\\d+ (\\w+)( J)?").matcher(l);if(m.find())kinds.put(m.group(1),m.group(3)!=null?'J':m.group(2).charAt(0));}
        for(var l:lines){var m=Pattern.compile("^  pipe \\d+ (\\d+)->(\\d+) .* closed=(\\w+) q=(\\S+)").matcher(l);if(!m.find())continue;double q=Double.parseDouble(m.group(4));
            char a=kinds.getOrDefault(m.group(1),'?'),c=kinds.getOrDefault(m.group(2),'?');
            if(m.group(3).equals("false")&&Math.abs(q)>1e-10&&(q<0&&(a=='G'||c=='V')||q>0&&(c=='G'||a=='V'))){b.append(" forbid");break;}}
        var traj=Arrays.stream(lines).filter(l->l.startsWith("  traj flow")).findFirst();
        if(traj.isPresent()){var m=Pattern.compile("\\[q=\\S+ d=(\\S+) l=(\\S+) r=\\S+\\]").matcher(traj.get());int n=0,capped=0;
            while(m.find()){n++;double d=Double.parseDouble(m.group(1)),l=Double.parseDouble(m.group(2));if(Math.abs(d)>1.5*Math.abs(l)&&Math.abs(d)>1)capped++;}
            if(n>0&&capped*4>=n*3)b.append(" capped");
            var label=traj.get().replaceAll("^  traj flow e(\\d+):.*","$1");
            if(Arrays.stream(lines).anyMatch(l->l.startsWith("  pipe "+label+" ")&&l.contains(" MIXED")))b.append(" mixed");}
        return b.toString();
    }
    /** A node's kind with its phases: R/J/G/V and g (vapour) l (hydrocarbon liquid) w (free water). */
    static String kind(String[] lines,String node) {
        for(var l:lines)if(l.startsWith("  node "+node+" ")) {
            String k=l.contains(" J ")?"J":l.contains("GENERATOR")?"G":l.contains("VOID")?"V":l.contains("PORT")?"P":"R";
            var m=Pattern.compile(" vap=(\\S+) liq=(\\S+) wat=(\\S+) ").matcher(l);
            if(m.find())k+=":"+(Double.parseDouble(m.group(1))>0?"g":"")+(Double.parseDouble(m.group(2))>0?"l":"")+(Double.parseDouble(m.group(3))>0?"w":"");
            return k;
        }
        return "?";
    }
    static List<HoldFuzzTest.Placement> list(long seed,String keep) {
        var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);
        if(keep==null||keep.isEmpty())return original;
        var list=new ArrayList<HoldFuzzTest.Placement>();
        for(var item:keep.split(",")){var parts=item.split("@");list.add(new HoldFuzzTest.Placement(Long.parseLong(parts[1]),original.get(Integer.parseInt(parts[0])).block()));}
        return list;
    }
    static String render(List<HoldFuzzTest.Placement> list){var b=new StringBuilder();for(var p:list)b.append(p.tick()).append(':').append(p.block().label()).append(' ');return b.toString();}

    @Test void trace() throws Exception {
        String seeds=System.getProperty("t13seeds");if(seeds==null)return;
        var out=Path.of(System.getProperty("t13out","build/t13"));Files.createDirectories(out);int slices=Integer.getInteger("holdFuzz.slices",120);
        for(long seed:HoldFuzzTest.seeds(seeds)) {
            var placements=list(seed,System.getProperty("t13keep"));long started=System.nanoTime();
            var t=run(placements,slices);
            var b=new StringBuilder("T13_SEED seed="+seed+" defaults="+HoldFuzzTest.DEFAULTS+" blocks="+placements.size()+" ms="+(System.nanoTime()-started)/1_000_000+" keep="+System.getProperty("t13keep","")+"\n");
            b.append("T13_LAYOUT ").append(render(placements)).append('\n');
            b.append("T13_HELD ").append(t.held()).append('\n').append("T13_FORMED ").append(t.formed()).append('\n').append("T13_SIG ").append(t.signature()).append('\n').append("T13_ISLAND ").append(t.island());
            b.append("T13_REPORTS ").append(t.reports().size()).append('\n');
            // Every failed pass in one line; the first two and the last four in full.
            for(int i=0;i<t.reports().size();i++){var lines=t.reports().get(i).split("\n");b.append("T13_PASS ").append(i).append(' ').append(lines[0]).append(" |").append(lines.length>2?lines[2]:"").append(" |").append(lines.length>1?lines[1]:"").append('\n');}
            for(int i=0;i<t.reports().size();i++)if(i<2||i>=t.reports().size()-4)b.append("T13_FULL ").append(i).append(' ').append(t.reports().get(i)).append('\n');
            Files.writeString(out.resolve((HoldFuzzTest.DEFAULTS?"d-":"r-")+seed+(System.getProperty("t13keep")==null?"":"-min")+".txt"),b.toString(),StandardCharsets.UTF_8);
            System.out.println("T13_DONE seed="+seed+" "+t.held()+" SIG "+t.signature());
        }
    }

    @Test void minimize() throws Exception {
        Long seed=Long.getLong("t13min");if(seed==null)return;int slices=Integer.getInteger("minimize.slices",40);
        var sig=Pattern.compile(System.getProperty("t13sig","E6 .*"));
        var list=new ArrayList<>(list(seed,System.getProperty("t13keep")));var start=list(seed,null);
        var first=run(list,slices);
        System.out.println("T13MIN seed="+seed+" start blocks="+list.size()+" sig="+first.signature()+" held="+first.held());
        if(!sig.matcher(first.signature()).matches()){System.out.println("T13MIN seed="+seed+" start does not match "+sig);return;}
        var pool=Executors.newFixedThreadPool(Integer.getInteger("minimize.threads",4));
        boolean progress=true;
        while(progress) {
            progress=false;var futures=new ArrayList<Future<Trace>>();
            for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->run(candidate,slices)));}
            for(int i=0;i<futures.size();i++) {
                if(futures.get(i)==null)continue;var o=futures.get(i).get();
                if(sig.matcher(o.signature()).matches()){list.remove(i);progress=true;System.out.println("T13MIN seed="+seed+" drop -> "+list.size()+" "+o.signature());for(var f:futures)if(f!=null)f.cancel(true);break;}
            }
        }
        pool.shutdown();
        var last=run(list,slices);var keep=new StringBuilder();
        for(var p:list)for(int k=0;k<start.size();k++)if(start.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
        System.out.println("T13MIN seed="+seed+" KEEP "+keep);
        System.out.println("T13MIN seed="+seed+" RESULT blocks="+list.size()+" sig="+last.signature()+" held="+last.held()+"\nT13MIN seed="+seed+" LIST "+render(list));
    }
}
