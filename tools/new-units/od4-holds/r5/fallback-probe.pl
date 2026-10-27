#!/usr/bin/perl -i
# r5 round 7 probe (994): on an r2-traced + probe.pl export with prototype A (or 416f8601/2def39dc) applied, prints
# "R2 FALLBACK" wherever JunctionCrystals.split falls back to the island's populations with a melting share:
# junction, donor, flow, the solid rate melted and its share. Export-only, never a tracked source.
# Usage: perl -i fallback-probe.pl <export>/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java
while(<>){
  s/^(\s*)(var split=melting\.split\(node,source\.solids\(\),sourceAmounts\.length\);double solidRate=Math\.abs\(flow\)\*carried\.solids\.mass\(\)\/carried\.mass;)\s*$/$1$2\n$1if(R2T&&!(source.solids().massKg()>0)&&split.share()>0)System.out.println("R2 FALLBACK node="+node+" donor="+mixed+" flow="+flow+" solidRate="+solidRate+" share="+split.share());\n/;
  print;
}
