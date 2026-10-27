#!/usr/bin/env perl
# Prints every comment block of at least MIN lines (default 6) of a Java file with its line numbers, and the first code
# line after it. Usage: perl blocks.pl [--min N] <file>. Each block starts with "@@ <start> <count>".
use strict; use warnings;
my $min = 6; if (@ARGV && $ARGV[0] eq '--min') { shift; $min = shift; }
my $f = shift or die "usage: blocks.pl [--min N] <file>";
open my $h, '<', $f or die "$f: $!"; my @l = <$h>; close $h;
my ($in, $start, @run) = (0, 0);
sub flush { my ($start, $run, $next) = @_; return unless @$run >= $min;
  printf "@@ %d %d\n", $start, scalar @$run; print "$_\n" for @$run; printf "   -> %s\n\n", $next // '(end)'; }
for my $i (0 .. $#l) { my $x = $l[$i]; $x =~ s/\r?\n$//; my $s = $x; $s =~ s/^\s+|\s+$//g;
  my $isc = 0;
  if ($in) { $isc = 1; $in = 0 if $s =~ m{\*/}; }
  elsif ($s =~ m{^//}) { $isc = 1; }
  elsif ($s =~ m{^/\*}) { $isc = 1; $in = 1 unless $s =~ m{\*/}; }
  if ($isc) { $start = $i + 1 unless @run; push @run, $x; next; }
  if (@run) { my $next = $x; $next =~ s/^\s+//; flush($start, \@run, substr($next, 0, 120)); @run = (); }
}
flush($start, \@run, undef) if @run;
