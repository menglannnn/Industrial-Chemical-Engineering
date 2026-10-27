package com.wormzjl.icheme.runtime.fluid;
import java.util.*;
import org.junit.jupiter.api.Test;
class ProcessTraceTest {
 @Test void trace(){
  var r=new Random(236);var all=HoldFuzzTest.placements(r,HoldFuzzTest.world(r));var list=new ArrayList<HoldFuzzTest.Placement>();
  for(int i:new int[]{1,5,6,10,11,13,15,17,19})list.add(new HoldFuzzTest.Placement(153,all.get(i).block()));
  var w=new M5TraceWorld(HoldFuzzTest.MODELS.get(),true);boolean[] printed={false};
  w.after=s->{if(!printed[0]&&s.result().candidate().isEmpty()){
   printed[0]=true;System.out.println("TRACE236 start="+s.start()+" end="+s.end()+" detail="+s.detail());
   System.out.println(M5TraceWorld.describe(s.snapshot(),w.model));
   for(var n:s.snapshot().reservoirs()){
    var st=n.state();System.out.println("TRACE236 EXACT node="+n.id()+" T="+st.temperature()+" P="+st.pressure()+" L="+Arrays.toString(st.liquidView())+" V="+Arrays.toString(st.vaporView())+" waterL="+st.waterLiquid()+" waterV="+st.waterVapor()+" hcP="+st.hydrocarbonPartialPressure()+" inventoryMoles="+Arrays.toString(n.inventory().moles())+" inventoryEnergy="+n.inventory().internalEnergy());
   }
  }};
  w.placeAll(list);while(w.held.isEmpty()&&w.ledger.onlineTick()<13000)w.tick();
 }
}
