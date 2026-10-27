#!/usr/bin/perl
# Applies the forms trace probe to an exported tree (argument: the tree's root), on top of nothing else: (1) the untyped TP
# refusal "Fluid TP equilibrium did not converge" (FluidThermodynamics.adopt) is counted by its call path and its first
# three occurrences printed with T, P and the feed; (2) phaseCheck's TP flash refused by it is counted with the node's
# phase bits, own bits (D31 B) and whether its liquid is the stand-in; (3) a BULK end drawing a dry donor's gas (D33,
# PassiveStepSolver draws) is counted by whether the donor holds a crystal and its solids mass. Seeds 55 (E9) and 101.
# One literal substring replacement per edit, each matching exactly once (CRLF kept). Needs probe/FormsProbe.java.
use strict; use warnings;
my $root=shift or die "usage: apply-trace.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.FormsProbe';
my $base="$root/src/main/java/com/wormzjl/createcheme/science/fluid";
my @edits=(
 ["$base/thermo/FluidThermodynamics.java",
  '                throw new IllegalArgumentException("Fluid TP equilibrium did not converge: "+result.detail());',
  "                {String fpW=$P.where(new Throwable());$P.add(\"E9 tp-refused at \"+fpW);\n"
 ."                 $P.first(\"E9\",\"t=\"+t+\" p=\"+p+\" overall=\"+java.util.Arrays.toString(overall)+\" detail=\"+result.detail()+\" | \"+fpW);}\n"
 ."                throw new IllegalArgumentException(\"Fluid TP equilibrium did not converge: \"+result.detail());"],
 ["$base/thermo/FluidThermodynamics.java",
  '            var equilibrium=folded(equilibriumTP(kept.temperature(),kept.pressure(),totals,checkpoint,prepared).withSolidState(kept.solids(),kept.solidMoments()),junction);',
  "            State fpEq;try{fpEq=equilibriumTP(kept.temperature(),kept.pressure(),totals,checkpoint,prepared);}\n"
 ."            catch(IllegalArgumentException fpX){$P.add(\"E9 phaseCheck refused junction=\"+junction+\" bits=\"+phaseBits(kept)+\" own=\"+ownPhaseBits(kept)+\" standIn=\"+condensedShareOnly(kept)+\" solids=\"+!kept.solids().empty()+\" record=\"+excluded);throw fpX;}\n"
 ."            var equilibrium=folded(fpEq.withSolidState(kept.solids(),kept.solidMoments()),junction);"],
 ["$base/network/PassiveStepSolver.java",
  '                    if(!FluidThermodynamics.bulkDrawsGas(donor))continue;',
  "                    if(!FluidThermodynamics.bulkDrawsGas(donor))continue;\n"
 ."                    $P.add(\"D33 gas draw crystal=\"+!donor.solids().crystals().isEmpty()+\" solids-kg \"+$P.q(donor.solids().massKg()));"],
);
for my $e (@edits) {
  my ($file,$from,$to)=@$e;
  open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
  my $count=()=$text=~/\Q$from\E/g;
  die "$file: expected one match, found $count for: $from\n" unless $count==1;
  $text=~s/\Q$from\E/$to/;
  open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
}
print "apply-trace: ".scalar(@edits)." edits applied under $root\n";
