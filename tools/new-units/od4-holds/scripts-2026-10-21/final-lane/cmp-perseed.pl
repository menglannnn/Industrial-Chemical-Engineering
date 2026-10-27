#!/usr/bin/env perl
# m22 (from m21): per-seed COUNTERS_SEED of two one-worker HoldFuzzTest reports on the seeds in both: totals and the seeds that move.
use strict; use warnings;
sub load { my $f=shift; open my $h,'<',$f or die "$f: $!"; my (%c,$s); while(<$h>){ if(/^SEED (\d+) /){$s=$1} elsif(defined $s && /^\s+COUNTERS_SEED (.*)/){ my %k=map{split /=/}split ' ',$1; $c{$s}=\%k } } \%c }
my ($lab,$bf,$nf)=@ARGV; my ($B,$N)=(load($bf),load($nf)); my @s=sort{$a<=>$b} grep{exists $B->{$_}} keys %$N;
my @keys=qw(newtonIterations newtonSolves stepAttempts equationGateRejections phaseSeeds); my (%tb,%tn,@mv);
for my $s(@s){ my $m=0; for my $k(@keys){ $tb{$k}+=$B->{$s}{$k}; $tn{$k}+=$N->{$s}{$k}; $m=1 if $B->{$s}{$k}!=$N->{$s}{$k} } push @mv,$s if $m }
printf "== %s: %d seeds in both, %d move\n",$lab,scalar @s,scalar @mv;
printf "  %-22s %10d %10d %+8.2f %%\n",$_,$tb{$_},$tn{$_},$tb{$_}?100*($tn{$_}-$tb{$_})/$tb{$_}:0 for @keys;
my @d=sort{abs($N->{$b}{newtonIterations}-$B->{$b}{newtonIterations})<=>abs($N->{$a}{newtonIterations}-$B->{$a}{newtonIterations})}@mv;
print "  moved (newtonIterations base->new): ",join(', ',map{"$_ $B->{$_}{newtonIterations}->$N->{$_}{newtonIterations}"}@d),"\n";
