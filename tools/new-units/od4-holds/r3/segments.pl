#!/usr/bin/perl
# r3 probe: a failed pass's phase-port segment codes and the vessels' liquid capacities (kg/s), -Dr3.trace=true (after passes.pl).
use strict; my $root=shift or die; my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local @ARGV=($f); local $^I="";
while(<>){ s/\x0d\x0a$/\x0a/;
  if(/^\s*if\(R3T\)System.out.println\("R3 FAILED-ITERATE/){ $_.='                if(R3T){StringBuilder r3s=new StringBuilder("R3 SEGMENTS codes="+Arrays.toString(equations.segmentCodes())+" capacities(liquid kg/s)=");for(int q=0;q<equations.capacities.length;q++)if(equations.capacities[q]!=null)r3s.append(q).append(":").append(Arrays.toString(equations.capacities[q])).append(" ");r3s.append(" start flow0="+equations.initialPoint[equations.edgeOffset]);System.out.println(r3s);}'."\n"; }
  print; }
