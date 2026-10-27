package com.wormzjl.icheme.runtime.fluid;
import java.util.*;
import com.wormzjl.icheme.science.material.MaterialRuntime;
public class Cold183Trace {
 public static void main(String[] args) {
  MaterialRuntime.with(HoldFuzzTest.CATALOG,HoldFuzzTest.PACKAGE,()->{
   var r=new Random(183);var blocks=HoldFuzzTest.world(r);var placements=HoldFuzzTest.placements(r,blocks);
   var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
   for(var p:placements) {
    while(w.ledger.onlineTick()<p.tick()&&w.held.isEmpty())w.tick();
    if(!w.held.isEmpty())break;
    System.out.println("PLACE tick="+p.tick()+" "+p.block().label());w.place(p.block());
   }
   while(w.ledger.onlineTick()<15000&&w.held.isEmpty())w.tick();
   for(var h:w.held)System.out.println("FIRST_HOLD "+h+"\n"+w.describe(h.island()));
   return null;
  });
 }
}
