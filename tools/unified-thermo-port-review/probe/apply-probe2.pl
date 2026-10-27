#!/usr/bin/perl
# Probe 2, applied after apply-probe.pl: what the TP answer added in phaseCheck (stand-in liquid or a phase of its own)
# and how InventoryEquilibrium.split ended. Argument: the exported tree's root. Each edit matches exactly once.
use strict; use warnings;
my $root=shift or die "usage: apply-probe2.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.ReviewProbe';
my $base="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my @edits=(
 ["$base/thermo/FluidThermodynamics.java",
  '        var start=tpSeed(kept,equilibrium,prepared);',
  "        var start=tpSeed(kept,equilibrium,prepared);$P.answer(junction,condensedShareOnly(equilibrium),!kept.solids().empty(),phaseBits(equilibrium)&~phaseBits(kept)&~excluded);"],
 ["$base/network/InventoryEquilibrium.java",
  '        try{return close(model,start,inventory,null,workFrom,checkpoint);}',
  "        try{var rpC=close(model,start,inventory,null,workFrom,checkpoint);$P.split(\"converged\");return rpC;}"],
 ["$base/network/InventoryEquilibrium.java",
  '            try{return ConservativeTransport.repartition(model,layout(model,start,null).decode(failure.lastVariables(),0),inventory.moles());}',
  "            try{var rpR=ConservativeTransport.repartition(model,layout(model,start,null).decode(failure.lastVariables(),0),inventory.moles());$P.split($P.stall(failure,rpR,inventory));return rpR;}"],
);
for my $e (@edits) {
  my ($file,$from,$to)=@$e;
  open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
  my $count=()=$text=~/\Q$from\E/g;
  die "$file: expected one match, found $count for: $from\n" unless $count==1;
  $text=~s/\Q$from\E/$to/;
  open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
}
print "apply-probe2: ".scalar(@edits)." edits applied under $root\n";
