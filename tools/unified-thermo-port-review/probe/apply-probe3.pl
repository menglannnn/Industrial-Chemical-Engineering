#!/usr/bin/perl
# Probe 3 (rule-off, applied after apply-probe.pl and apply-probe2.pl): a vessel's own closure that does not converge adds
# no phase. InventoryEquilibrium.split returns null instead of the stalled iterate repartitioned, and phaseCheck keeps the
# node's phases where its own split is null (no TP-based fallback seed). Argument: the exported tree's root.
use strict; use warnings;
my $root=shift or die "usage: apply-probe3.pl <exported tree>";
my $base="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my @edits=(
 ["$base/network/InventoryEquilibrium.java",
  'com.wormzjl.createcheme.science.fluid.ReviewProbe.msg(failure));return rpR;}',
  'com.wormzjl.createcheme.science.fluid.ReviewProbe.msg(failure));return null;}'],
 ["$base/thermo/FluidThermodynamics.java",
  '        var seed=folded(own==null?start:own.withSolidState(kept.solids(),kept.solidMoments()),junction);',
  '        if(own==null)return kept;var seed=folded(own.withSolidState(kept.solids(),kept.solidMoments()),junction);'],
);
for my $e (@edits) {
  my ($file,$from,$to)=@$e;
  open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
  my $count=()=$text=~/\Q$from\E/g;
  die "$file: expected one match, found $count for: $from\n" unless $count==1;
  $text=~s/\Q$from\E/$to/;
  open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
}
print "apply-probe3: ".scalar(@edits)." edits applied under $root\n";
