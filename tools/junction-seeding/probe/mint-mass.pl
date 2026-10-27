#!/usr/bin/perl
# t44b (owner request of 2026-10-19): the unbooked mint mass (D46), a lane-export probe. Inserts into
# PassiveNetwork.sizeJunctionHoldups(graph, model, starts) one line per minted junction:
#   MINT id=<node id> massKg=<the holdup mass m_J the mint books from nothing> rho=<kg/m3> v=<velocity limit m/s> liq=<liquid volume share>
# Run the hold-fuzz harness with one worker and -DholdFuzz.verbose=true, so FUZZ_VERBOSE_END seed=<n> lines close each seed;
# mint-mass-parse.pl sums the lines per seed. Usage: perl mint-mass.pl /tmp/d40-<export>
use strict; use warnings;
my $export=shift or die "usage: mint-mass.pl <export dir>";
my $f="$export/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveNetwork.java";
open my $in,"<",$f or die "$f: $!"; local $/; my $s=<$in>; close $in; my $crlf=$s=~/\r\n/; $s=~s/\r\n/\n/g;
my $n=($s=~s/(            double mass=PassiveStepSolver\.HOLDUP_TAU\*capacity;\n            if\(!\(mass>0\)\)mass=1;\n)/$1            System.out.println("MINT id="+node.id()+" massKg="+mass+" rho="+rho+" v="+velocity+" liq="+(state.volume()>0?(state.liquidVolume()+state.waterVolume())\/state.volume():0));\n/);
die "no match in $f" unless $n==1;
$s=~s/\n/\r\n/g if $crlf;
open my $out,">",$f or die; print $out $s; close $out; print "mint probe applied to $f\n";
