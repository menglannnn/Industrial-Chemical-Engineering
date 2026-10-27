package com.wormzjl.icheme.runtime.fluid;
import com.wormzjl.icheme.science.fluid.network.Probe236;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Seed-236 minimized world (ProcessTraceTest's nine placements) with the failure probe active from trace236.from. Scratch only. */
class Trace236Test {
 @Test void trace(){
  long from=Long.getLong("trace236.from",5580),until=Long.getLong("trace236.until",13000);
  var r=new Random(236);var all=HoldFuzzTest.placements(r,HoldFuzzTest.world(r));var list=new ArrayList<HoldFuzzTest.Placement>();
  for(int i:new int[]{1,5,6,10,11,13,15,17,19})list.add(new HoldFuzzTest.Placement(153,all.get(i).block()));
  for(var p:list)System.out.println("PLACE "+p.block().label()+" @"+p.block().position());
  var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),true);
  var probe=Probe236.install(w.model.hydrocarbon.components(),12);probe.model=w.model;
  w.before=(a,c)->{probe.active=a.slice().startTick()>=from;
   if(probe.active){System.out.println("SOLVE island="+a.islandId()+" slice="+a.slice().startTick()+".."+a.slice().endTick());System.out.println(M5TraceWorld.describe(c.snapshot(),w.model));}};
  w.after=s->{if(s.start()>=from-400)System.out.println("RESULT slice="+s.start()+".."+s.end()+" accepted="+s.accepted()+" rejected="+s.rejected()+" reasons="+s.reasons()+" detail="+s.detail());};
  try{w.placeAll(list);while(w.held.isEmpty()&&w.ledger.onlineTick()<until)w.tick();}
  finally{Probe236.uninstall();}
  System.out.println("HELD "+w.held+" fails="+probe+" tick="+w.ledger.onlineTick());
 }
}
