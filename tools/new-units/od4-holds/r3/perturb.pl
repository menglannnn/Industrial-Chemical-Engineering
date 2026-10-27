#!/usr/bin/perl
# r3 robustness probe: the arriving-phase seed of PassiveStepSolver.arrivingPhases perturbed on an export (never a tracked source).
# perl perturb.pl <export root> tp <eps>  : the m21 TP flash at the mixing temperature times (1+eps) (r2's knife-edge probe; eps 0 = m21)
# perl perturb.pl <export root> ph <eps>  : the PH split with the stock-plus-deliveries enthalpy times (1+eps)
use strict; my($root,$mode,$eps)=@ARGV; die "usage" unless defined $eps;
my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local @ARGV=($f); local $^I=""; my $n=0;
my $from=qr/try\{var split=model\.equilibriumPH\(weighted\/mass,seed\.pressure\(\),enthalpy,n,false,checkpoint\);mixture=model\.folded\(split!=null\?split:model\.flashTP\(weighted\/mass,seed\.pressure\(\),n,checkpoint\),true\);\}/;
while(<>){ s/\x0d\x0a$/\x0a/;
  if($mode eq 'tp'){ $n+=s/$from/try{mixture=model.folded(model.flashTP(weighted\/mass*(1+($eps)),seed.pressure(),n,checkpoint),true);}/; }
  else { $n+=s/$from/try{var split=model.equilibriumPH(weighted\/mass,seed.pressure(),enthalpy*(1+($eps)),n,false,checkpoint);mixture=model.folded(split!=null?split:model.flashTP(weighted\/mass,seed.pressure(),n,checkpoint),true);}/; }
  print; }
die "perturb: no arriving seed line in $f\n" unless $n==1; print STDERR "perturb $mode $eps\n";
