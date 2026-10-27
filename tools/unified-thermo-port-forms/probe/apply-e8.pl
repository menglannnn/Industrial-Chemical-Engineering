#!/usr/bin/perl
# Form 4's E8 trace (seed 48): InventoryEquilibrium.close's "does not meet full equation closure" refusal is counted by its
# call path, the closure's start and restated regimes and the residual's decade, and its first three occurrences printed
# with the restated state (T, P, amounts by phase, solids, capacity). Argument: the exported tree's root. Needs FormsProbe.
use strict; use warnings;
my $root=shift or die "usage: apply-e8.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.FormsProbe';
my $file="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/InventoryEquilibrium.java";
my $from='        if(norm(residual)>1e-8)throw new SparseNewton.Nonconvergence("Inventory closure does not meet full equation closure");';
my $to='        if(norm(residual)>1e-8){String fpW='.$P.'.where(new Throwable());'.$P.'.add("E8 at "+fpW+" | "+regime(state)+"->"+regime(closed)+" res1e"+(int)Math.floor(Math.log10(norm(residual))));
            '.$P.'.first("E8","T="+closed.temperature()+" P="+closed.pressure()+" l="+java.util.Arrays.toString(closed.liquidView())+" v="+java.util.Arrays.toString(closed.vaporView())
                +" start l="+java.util.Arrays.toString(state.liquidView())+" v="+java.util.Arrays.toString(state.vaporView())+" startT="+state.temperature()+" startP="+state.pressure()
                +" solids="+inventory.solids().massKg()+" capacity="+volume+" workFrom="+workFrom+" res="+norm(residual)+" | "+fpW);
            throw new SparseNewton.Nonconvergence("Inventory closure does not meet full equation closure");}';
open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
my $crlf=$text=~/\r\n/;$text=~s/\r\n/\n/g;
my $count=()=$text=~/\Q$from\E/g; die "$file: expected one match, found $count\n" unless $count==1;
$text=~s/\Q$from\E/$to/;$text=~s/\n/\r\n/g if $crlf;
open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
print "apply-e8: 1 edit applied under $root\n";
