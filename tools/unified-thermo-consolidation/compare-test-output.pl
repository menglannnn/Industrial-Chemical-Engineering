#!/usr/bin/env perl
# Compares the printed output of index-lane test logs test by test: each line a test prints is keyed by the M3RUN line
# that follows it ("Class method()"), object hashes (@1a2b3c) are blanked. Usage:
#   perl compare-test-output.pl <new log> <baseline log>...
# Prints, for each test present in both, the lines that differ (baseline "-", new "+"), and the tests found in one only.
use strict; use warnings;
sub load { my ($f, $into) = @_; open my $h, '<', $f or die "$f: $!"; my @pending;
  while (my $l = <$h>) { $l =~ s/\r?\n$//;
    if ($l =~ /^M3RUN (SUCCESSFUL|FAILED|ABORTED|SKIPPED) (\S+ \S+)/) { $into->{$2} = [@pending]; @pending = (); next; }
    next if $l =~ /^M3RUN /;
    $l =~ s/@[0-9a-f]{4,}/@#/g; push @pending, $l; }
  close $h; }
my ($new, @bases) = @ARGV; die "usage: compare-test-output.pl <new> <baseline>..." unless $new && @bases;
my (%n, %b); load($new, \%n); load($_, \%b) for @bases;
my ($same, $moved, $only) = (0, 0, 0);
for my $k (sort keys %n) {
  if (!exists $b{$k}) { $only++; print "NEW ONLY $k\n"; next; }
  my @x = @{$b{$k}}; my @y = @{$n{$k}};
  if ("@x" eq "@y") { $same++; next; }
  $moved++; print "MOVED $k\n";
  my $max = @x > @y ? scalar @x : scalar @y;
  for my $i (0 .. $max - 1) { my $p = $x[$i] // ''; my $q = $y[$i] // '';
    next if $p eq $q; print "  - ", substr($p, 0, 400), "\n  + ", substr($q, 0, 400), "\n"; }
}
for my $k (sort keys %b) { print "BASELINE ONLY $k\n" unless exists $n{$k}; }
print "tests compared: ", $same + $moved, ", same output: $same, moved: $moved, new only: $only\n";
