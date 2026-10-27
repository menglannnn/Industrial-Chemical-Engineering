package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.NetworkReduction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * t16 probe (tools only; needs src/scratch-t16-switch.patch): runs one minimized fuzz list in two modes of the rest test
 * (-Dt16.modes=exact,on) and prints, per mode, every island's committed interval (tick, status, node pressures and
 * inventories), then the first committed interval at which the two modes differ, and the held island's nodes with their
 * component amounts. -Dt16.seed=761 -Dt16.keep=... (holdFuzz.defaults for the distribution).
 */
class T16DivergeProbeTest {
    record Row(long island,long committed,String status,String nodes) {}
    static List<Row> run(String mode,List<HoldFuzzTest.Placement> list,int slices,StringBuilder heldNodes) {
        NetworkReduction.SCRATCH_T16.set(mode);var rows=new ArrayList<Row>();var last=new HashMap<Long,Long>();
        try {
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
            long end=list.getLast().tick()+100L*slices;int next=0;
            while(w.ledger.onlineTick()<end) {
                while(next<list.size()&&w.ledger.onlineTick()>=list.get(next).tick()){try{w.place(list.get(next).block());}catch(IllegalArgumentException refused){}next++;}
                w.tick();
                for(var s:w.coordinator.observe()) {
                    long c=s.clock().committedTick();if(Objects.equals(last.get(s.id()),c))continue;last.put(s.id(),c);
                    var b=new StringBuilder();
                    for(var n:s.graph().reservoirs())b.append(n.id()).append(':').append(n.state().pressure()).append(n.fixed()?"":" n="+Arrays.toString(n.inventory().moles())).append("; ");
                    rows.add(new Row(s.id(),c,s.status(),b.toString()));
                }
                if(!w.held.isEmpty()){var h=w.held.getFirst();heldNodes.append(mode).append(" HELD ").append(h).append('\n').append(w.describe(h.island()));
                    for(var n:w.coordinator.observe(h.island()).graph().reservoirs())heldNodes.append("    inventory ").append(n.id()).append(' ').append(n.kind()).append(' ').append(Arrays.toString(n.inventory().moles())).append('\n');break;}
            }
        } finally{NetworkReduction.SCRATCH_T16.remove();}
        return rows;
    }
    @Test void diverge() {
        Long seed=Long.getLong("t16.seed");if(seed==null)return;
        var list=T16CrudeCaseTest.list(seed,System.getProperty("t16.keep"));var modes=System.getProperty("t16.modes","exact,on").split(",");
        var held=new StringBuilder();var a=run(modes[0],list,120,held);var b=run(modes[1],list,120,held);
        for(int i=0;i<Math.min(a.size(),b.size());i++)if(!a.get(i).equals(b.get(i))) {
            System.out.println("T16DIVERGE first difference at row "+i+"\n  "+modes[0]+": "+a.get(i)+"\n  "+modes[1]+": "+b.get(i));
            for(int k=Math.max(0,i-3);k<i;k++)System.out.println("  same before: "+a.get(k));
            break;
        }
        System.out.println("T16DIVERGE rows "+modes[0]+"="+a.size()+" "+modes[1]+"="+b.size()+"\n"+held);
    }
}
