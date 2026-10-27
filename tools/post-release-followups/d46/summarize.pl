#!/usr/bin/perl
# t52: sums the D46SEED lines of a probe run. freshKg is t44's metric (the buffers minted at junction ids new to the
# seed); createdAbsKg is what the unbooked buffers actually put into or took out of the vessels (sum over components of
# |kg|, signed per component), createdNetKg the total mass (zero by the mass pin), createdMJ the energy.
# Usage: perl summarize.pl <run log> <report> <label>
use strict; use warnings;
my ($log,$report,$label)=@ARGV; open my $h,"<",$log or die "$log: $!";
my %r; while(<$h>){ next unless /^D46SEED seed=(\d+) (.*)/; my $s=$1; my %f=map{split /=/,$_,2} split ' ',$2; $r{$s}=\%f; }
my %held; if(open my $q,"<",$report){while(<$q>){$held{$1}=$2 if /^SEED (\d+) .* classes=\[([^\]]+)\]/}}
my ($fresh,$created,$mj,$turn,$net,$n,$largestFresh,$lfSeed,$installs,$carried,$reminted)=(0)x11; my @big; my ($over1,$over01)=(0,0);
for my $s (sort {$a<=>$b} keys %r){ my $f=$r{$s}; next unless defined $f->{createdAbsKg}; $n++;
  $fresh+=$f->{freshKg}; $created+=$f->{createdAbsKg}; $mj+=abs($f->{createdMJ}); $turn+=$f->{turnoverKg};
  $installs+=$f->{installs}; $carried+=$f->{carried}; $reminted+=$f->{reminted};
  $net=abs($f->{createdNetKg}) if abs($f->{createdNetKg})>$net;
  if($f->{largestFreshKg}>$largestFresh){$largestFresh=$f->{largestFreshKg};$lfSeed=$s}
  $over1++ if $f->{createdAbsKg}>1; $over01++ if $f->{createdAbsKg}>0.1;
  push @big,[$s,$f->{createdAbsKg},$f->{worstKg},$f->{worstC},$f->{waterKg},$f->{createdMJ},$f->{turnoverKg},$f->{freshKg}]; }
@big=sort {$b->[1]<=>$a->[1]} @big;
printf "D46SUM %s seeds=%d installs=%d carried=%d reminted=%d freshKg=%.1f largestFresh=%.2f(seed %s) createdAbsKg=%.3f createdMJ=%.2f turnoverKg=%.0f created/turnover=%.2e seeds>0.1kg=%d seeds>1kg=%d max|net|=%.1e\n",
  $label,$n,$installs,$carried,$reminted,$fresh,$largestFresh,$lfSeed,$created,$mj,$turn,$turn>0?$created/$turn:0,$over01,$over1,$net;
for my $b (@big[0..($#big<7?$#big:7)]){ printf "  seed %d createdAbsKg=%.4f worst=%.4f kg (component %d) water=%.4f kg energy=%.3f MJ turnover=%.1f kg fresh=%.2f kg%s\n",@$b,$held{$b->[0]}?" held ".$held{$b->[0]}:""; }
