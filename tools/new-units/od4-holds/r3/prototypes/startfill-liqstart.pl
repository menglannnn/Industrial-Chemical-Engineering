#!/usr/bin/perl
# experiment C1': the phase port's column filled by the start state's phase volumes (as D11's capacities), each at the trial's density
# (the start's where the trial no longer holds the phase).
use strict; my $root=shift or die; my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local @ARGV=($f); local $^I=""; my $n=0; my $skip=0;
while(<>){ s/\x0d\x0a$/\x0a/;
  if(/^    static double columnDensity\(FluidThermodynamics model,FluidThermodynamics.State s,PassiveNetwork.PhasePort port,double volume\) \{$/){
    print "    static double columnDensity(FluidThermodynamics model,FluidThermodynamics.State s,PassiveNetwork.PhasePort port,double volume){return columnDensity(model,s,s,port,volume);}\n";
    print "    static double columnDensity(FluidThermodynamics model,FluidThermodynamics.State start,FluidThermodynamics.State s,PassiveNetwork.PhasePort port,double volume) {\n";
    print "        if(port==PassiveNetwork.PhasePort.BULK)return endDensity(model,s,port);\n";
    print "        int leading=PhaseDraw.leading(model,start,port);\n";
    print "        if(leading<0)return start==s?s.mass()/s.volume():columnDensity(model,s,s,port,volume);\n";
    print "        if(!(FluidThermodynamics.phaseVolume(start,leading)<volume))return standing(model,start,s,leading);\n";
    print "        int[] order=PhaseDraw.order(model,start,port);var filled=PhaseDraw.fill(start,order,volume,1);\n";
    print "        double mass=0;\n";
    print "        for(int phase:order) {\n";
    print "            if(!(filled[phase]>0))continue;\n";
    print "            if(filled[phase]==volume)return standing(model,start,s,phase);\n";
    print "            mass+=standing(model,start,s,phase)*filled[phase];\n";
    print "        }\n";
    print "        return mass/volume;\n";
    print "    }\n";
    print "    private static double standing(FluidThermodynamics model,FluidThermodynamics.State start,FluidThermodynamics.State s,int phase){return phaseDensity(model,start==s||!(phase!=FluidThermodynamics.GAS)&&FluidThermodynamics.holdsPhase(s,phase)?s:start,phase);}\n";
    $skip=1; $n++; next; }
  if($skip){ if(/^    \}$/){ $skip=0; } next; }
  if(s/:columnEnds\[edge\]==0\?columnDensity\(st\[a\],pipe.firstPort\(\),pipe.volume\(\)\):columnDensity\(st\[b\],pipe.secondPort\(\),pipe.volume\(\)\);/:columnEnds[edge]==0?columnDensity(model,graph.reservoirs().get(a).state(),st[a],pipe.firstPort(),pipe.volume()):columnDensity(model,graph.reservoirs().get(b).state(),st[b],pipe.secondPort(),pipe.volume());/){$n++;}
  print; }
print STDERR "startfill edits: $n\n";
