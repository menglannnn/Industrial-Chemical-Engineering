#!/usr/bin/env perl
# Compares two Gradle JUnit XML result folders (t36 final test): test names both ways, then each test class's
# <system-out> and <system-err> (Gradle writes them per class, not per testcase) line by line.
# Usage: perl compare-junit-xml.pl <baseline dir> <new dir>
# Output: BASELINE ONLY / NEW ONLY tests, per-class FAILURE lines (failure or error elements), then for every class
# whose output differs each differing line, "-" baseline and "+" new, tagged T (timing only: equal once wall ms,
# seconds, rates, byte and allocation counts and stack-trace line numbers are blanked, as notime2.pl) or P (anything
# else). Object hashes (@1a2b3c) are blanked first. Last line: the counts.
use strict; use warnings;
sub norm { my $s = shift;
  $s =~ s/\bms=[\d.]+//g; $s =~ s/[\d.]+ ms\b//g; $s =~ s/^\[\d\d:\d\d:\d\d\]//;
  $s =~ s/\{(\d+=[\d.]+(, )?)+\}//g if $s =~ /median ms/;
  $s =~ s/bytes=\d+//g; $s =~ s/allocatedMB=[\d.]+//g; $s =~ s/\.java:\d+\)/.java)/g; $s =~ s/allocated MB[^|]*\|//g;
  $s =~ s/wall ms[^|]*\|//g; $s =~ s/wallMs=\d+//g; $s =~ s/elapsed[_ ]ms=[\d.]+//g; $s =~ s/seedMsSum=\d+//g;
  $s =~ s/\b(nanos|Nanos|millis|Millis|seconds)=\d+//g; $s =~ s/\b\d+(\.\d+)? (ms|s|ns|us)\b//g;
  $s =~ s/\b[\d.]+(e[+-]?\d+)? ?(\/s|per s(econd)?)\b//g; return $s }
sub unx { my $s = shift; $s =~ s/&lt;/</g; $s =~ s/&gt;/>/g; $s =~ s/&quot;/"/g; $s =~ s/&apos;/'/g; $s =~ s/&amp;/&/g; $s }
sub load { my $dir = shift; my %c;
  for my $f (glob "$dir/TEST-*.xml") { local $/; open my $h, '<:raw', $f or die "$f: $!"; my $x = <$h>; close $h;
    my ($cls) = $x =~ /<testsuite name="([^"]+)"/ or die "$f: no testsuite";
    my %t; while ($x =~ /<testcase name="([^"]*)"[^>]*?(\/>|>(.*?)<\/testcase>)/sg) {
      my ($n, $body) = (unx($1), $3 // '');  # parameterised methods of one class share display names ("[1] X"): number repeats
      my $k = $n; my $r = 1; $k = "$n #" . ++$r while exists $t{$k};
      $t{$k} = ($body =~ /<(failure|error)\b/) ? $1 : 'ok'; }
    my %o; for my $s ('system-out', 'system-err') {
      my ($txt) = $x =~ /<$s><!\[CDATA\[(.*?)\]\]><\/$s>/s; $txt //= ''; $txt =~ s/\r//g;
      $o{$s} = [map { (my $l = $_) =~ s/@[0-9a-f]{4,}/@#/g; $l } split /\n/, $txt, -1]; }
    $c{$cls} = { tests => \%t, out => \%o }; }
  return \%c }
# Line diff: common prefix and suffix trimmed, LCS on the normalised middle; returns [tag, -line, +line] rows.
sub diff { my ($a, $b) = @_; my @r; my ($i, $j) = (0, 0); my ($ea, $eb) = ($#$a, $#$b);
  while ($i <= $ea && $j <= $eb && $a->[$i] eq $b->[$j]) { $i++; $j++ }
  while ($ea >= $i && $eb >= $j && $a->[$ea] eq $b->[$eb]) { $ea--; $eb-- }
  my @x = @$a[$i .. $ea]; my @y = @$b[$j .. $eb]; my @nx = map { norm($_) } @x; my @ny = map { norm($_) } @y;
  if (@x == @y && !grep { $nx[$_] ne $ny[$_] } 0 .. $#x) {  # same length, timing-only per position: no LCS needed
    return map { $x[$_] ne $y[$_] ? ['T', $x[$_], $y[$_]] : () } 0 .. $#x }
  my @L; for my $p (reverse 0 .. $#nx) { for my $q (reverse 0 .. $#ny) {
    $L[$p][$q] = $nx[$p] eq $ny[$q] ? 1 + ($L[$p+1][$q+1] // 0) : (($L[$p+1][$q] // 0) > ($L[$p][$q+1] // 0) ? $L[$p+1][$q] : ($L[$p][$q+1] // 0)); } }
  my ($p, $q) = (0, 0);
  while ($p < @nx || $q < @ny) {
    if ($p < @nx && $q < @ny && $nx[$p] eq $ny[$q]) { push @r, ['T', $x[$p], $y[$q]] if $x[$p] ne $y[$q]; $p++; $q++ }
    elsif ($q < @ny && ($p >= @nx || ($L[$p][$q+1] // 0) >= ($L[$p+1][$q] // 0))) { push @r, ['P', undef, $y[$q++]] }
    else { push @r, ['P', $x[$p++], undef] } }
  return @r }
my ($bd, $nd) = @ARGV; die "usage: compare-junit-xml.pl <baseline dir> <new dir>\n" unless $bd && $nd;
my ($B, $N) = (load($bd), load($nd));
my ($tb, $tn, $only_b, $only_n, $fail, $cmp, $same, $tonly, $phys, $plines, $tlines) = (0) x 11;
for my $c (sort keys %$B) { for my $t (sort keys %{$B->{$c}{tests}}) { $tb++;
  unless ($N->{$c} && exists $N->{$c}{tests}{$t}) { $only_b++; print "BASELINE ONLY $c.$t\n" } } }
for my $c (sort keys %$N) { for my $t (sort keys %{$N->{$c}{tests}}) { $tn++;
  my $st = $N->{$c}{tests}{$t}; if ($st ne 'ok') { $fail++; print "FAILURE ($st) $c.$t\n" }
  unless ($B->{$c} && exists $B->{$c}{tests}{$t}) { $only_n++; print "NEW ONLY $c.$t\n" } } }
for my $c (sort keys %$N) { next unless $B->{$c}; $cmp++; my @rows;
  for my $s ('system-out', 'system-err') { push @rows, map { [$s, @$_] } diff($B->{$c}{out}{$s}, $N->{$c}{out}{$s}) }
  if (!@rows) { $same++; next }
  my $np = grep { $_->[1] eq 'P' } @rows; $np ? $phys++ : $tonly++; $plines += $np; $tlines += @rows - $np;
  print(($np ? "PHYSICS     " : "TIMING-ONLY "), "$c\n");
  for my $r (@rows) { my ($s, $tag, $m, $p) = @$r; my $w = $s eq 'system-err' ? ' [err]' : '';
    print "  $tag$w - ", substr($m, 0, 400), "\n" if defined $m; print "  $tag$w + ", substr($p, 0, 400), "\n" if defined $p; } }
print "tests: baseline $tb, new $tn, baseline only $only_b, new only $only_n, failures $fail; classes compared $cmp: ",
  "same $same, timing only $tonly, physics $phys (lines: physics $plines, timing $tlines)\n";
