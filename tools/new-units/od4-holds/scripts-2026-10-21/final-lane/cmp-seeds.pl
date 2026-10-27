#!/usr/bin/env perl
# t48: compares two campaign reports seed by seed (the classify.sh classes file gives each held seed's class; the
# report gives blocks and the first HELD tick). Usage: cmp-seeds.pl <label> <base report> <new report>
# Prints clean counts, newly held (class, blocks, first tick), newly clean (was class), held both with class or first
# tick changed, and the new C/F holds (held C or F in the new run, not C or F in the base).
use strict; use warnings;
sub load { my $r = shift; my (%cls, %blk, %tick, %seen);
  open my $h, '<', "$r.classes.txt" or die "$r.classes.txt: $!";
  while (<$h>) { next unless /^(\S+)\s.*list=\s*(.*)$/; my $c = $1; $cls{$_} = $c for split ' ', $2 } close $h;
  open $h, '<', $r or die "$r: $!"; my $s;
  while (<$h>) { if (/^SEED (\d+) blocks=(\d+)/) { $s = $1; $blk{$s} = $2; $seen{$s} = 1 }
    elsif (defined $s && /^\s+HELD tick=(\d+)/ && !exists $tick{$s}) { $tick{$s} = $1 } } close $h;
  return { cls => \%cls, blk => \%blk, tick => \%tick, seen => \%seen } }
my ($lab, $bf, $nf) = @ARGV; my ($B, $N) = (load($bf), load($nf));
my @seeds = sort { $a <=> $b } keys %{$N->{seen}};
my $cb = grep { !exists $B->{cls}{$_} } keys %{$B->{seen}}; my $cn = grep { !exists $N->{cls}{$_} } @seeds;
print "== $lab: clean base=$cb new=$cn\n";
my @nh = grep { exists $N->{cls}{$_} && !exists $B->{cls}{$_} } @seeds;
print "  newly held (", scalar @nh, "):\n";
printf "    %d %s blocks=%d tick=%s\n", $_, $N->{cls}{$_}, $N->{blk}{$_}, $N->{tick}{$_} // '-' for @nh;
my @nc = grep { !exists $N->{cls}{$_} && exists $B->{cls}{$_} } @seeds;
print "  newly clean (", scalar @nc, "): ", join(', ', map { "$_ (was $B->{cls}{$_})" } @nc), "\n";
my @ch = grep { exists $N->{cls}{$_} && exists $B->{cls}{$_} &&
  ($N->{cls}{$_} ne $B->{cls}{$_} || ($N->{tick}{$_} // '') ne ($B->{tick}{$_} // '')) } @seeds;
print "  held both, class or first tick changed (", scalar @ch, "):\n";
printf "    %d %s@%s -> %s@%s\n", $_, $B->{cls}{$_}, $B->{tick}{$_} // '-', $N->{cls}{$_}, $N->{tick}{$_} // '-' for @ch;
my $cf = sub { my $c = shift; defined $c && $c =~ /^[CF]/ };
my @ncf = grep { $cf->($N->{cls}{$_}) && !$cf->($B->{cls}{$_}) } @seeds;
print "  new C/F holds: @ncf\n";
