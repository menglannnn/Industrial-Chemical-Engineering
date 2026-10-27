#!/usr/bin/perl
# The review's rule-off probe 3 (an unconverged own closure adds nothing) on an exported 76e87740 tree, with form 4 (ii)'s
# criterion evaluated on every stalled own split (FormsProbe.verdict, counted, printed at exit): reproduces the review's
# run 03 world (seed 48 held at the methane floor after two steps, 40 stalled closures adding a liquid to a gas tank) and
# says which of those stalls the direction criterion would refuse. Argument: the exported tree's root. Needs FormsProbe.
use strict; use warnings;
my $root=shift or die "usage: apply-ruleoff-criterion.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.FormsProbe';
my $base="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my @edits=(
 ["$base/network/InventoryEquilibrium.java",
  '            try{return ConservativeTransport.repartition(model,layout(model,start,null).decode(failure.lastVariables(),0),inventory.moles());}',
  "            try{var fpR=ConservativeTransport.repartition(model,layout(model,start,null).decode(failure.lastVariables(),0),inventory.moles());$P.add(\"stall \"+$P.residual(failure)+\" \"+$P.verdict(start,fpR));return null;}"],
 ["$base/thermo/FluidThermodynamics.java",
  '        var seed=folded(own==null?start:own.withSolidState(kept.solids(),kept.solidMoments()),junction);',
  '        if(own==null)return kept;var seed=folded(own.withSolidState(kept.solids(),kept.solidMoments()),junction);'],
);
for my $e (@edits) {
  my ($file,$from,$to)=@$e;
  open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
  my $crlf=$text=~/\r\n/;$text=~s/\r\n/\n/g;
  my $count=()=$text=~/\Q$from\E/g;
  die "$file: expected one match, found $count for: $from\n" unless $count==1;
  $text=~s/\Q$from\E/$to/;$text=~s/\n/\r\n/g if $crlf;
  open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
}
print "apply-ruleoff-criterion: ".scalar(@edits)." edits applied under $root\n";
