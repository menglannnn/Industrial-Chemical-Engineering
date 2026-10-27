#!/usr/bin/perl
# t44b: sums the MINT lines of a one-worker, verbose hold-fuzz run (mint-mass.pl probe) per seed: the total unbooked
# mint mass, the largest single mint and its seed, the number of mints. Usage: perl mint-mass-parse.pl <run log> [label]
use strict; use warnings;
my $f=shift or die; my $label=shift//$f; open my $h,"<",$f or die "$f: $!";
my (%total,%largest,%count); my ($seed,$sumSeed,$maxSeed,$n)=("?",0,0,0); my @pending;
while(<$h>) {
  if(/^MINT id=(\d+) massKg=([0-9.eE+-]+) rho=([0-9.eE+-]+) v=([0-9.eE+-]+) liq=([0-9.eE+-]+)/){push @pending,[$2,$1,$3,$5];next}
  if(/FUZZ_VERBOSE_END seed=(\d+)/){my $s=$1; for my $m(@pending){$total{$s}+=$m->[0];$count{$s}++;$largest{$s}=$m if !$largest{$s}||$m->[0]>$largest{$s}[0]} @pending=()}
}
my ($grand,$mints)=(0,0); my ($worstSeed,$worst)=("-",[0]); my ($heavySeed,$heavy)=("-",0);
for my $s (keys %total){$grand+=$total{$s};$mints+=$count{$s};if($largest{$s}[0]>$worst->[0]){$worst=$largest{$s};$worstSeed=$s} if($total{$s}>$heavy){$heavy=$total{$s};$heavySeed=$s}}
printf "MINTMASS %s seeds=%d mints=%d totalKg=%.3f largestKg=%.3f (seed %s, node %s, rho %.1f, liquid share %.2f) heaviestSeed=%s (%.3f kg)\n",
  $label,scalar(keys %total),$mints,$grand,$worst->[0],$worstSeed,$worst->[1]//"-",$worst->[2]//0,$worst->[3]//0,$heavySeed,$heavy;
