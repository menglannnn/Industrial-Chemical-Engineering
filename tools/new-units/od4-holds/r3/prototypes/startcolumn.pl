#!/usr/bin/perl
# experiment E1: a phase port's static column read on the step's start state (the head no longer moves with the trial's trace).
use strict; my $root=shift or die; my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local @ARGV=($f); local $^I=""; my $n=0;
while(<>){ s/\x0d\x0a$/\x0a/;
  if(s/:columnEnds\[edge\]==0\?columnDensity\(st\[a\],pipe.firstPort\(\),pipe.volume\(\)\):columnDensity\(st\[b\],pipe.secondPort\(\),pipe.volume\(\)\);/:columnEnds[edge]==0?(pipe.firstPort()==PassiveNetwork.PhasePort.BULK?columnDensity(st[a],pipe.firstPort(),pipe.volume()):columnDensity(graph.reservoirs().get(a).state(),pipe.firstPort(),pipe.volume())):(pipe.secondPort()==PassiveNetwork.PhasePort.BULK?columnDensity(st[b],pipe.secondPort(),pipe.volume()):columnDensity(graph.reservoirs().get(b).state(),pipe.secondPort(),pipe.volume()));/){$n++;}
  print; }
print STDERR "startcolumn edits: $n\n";
