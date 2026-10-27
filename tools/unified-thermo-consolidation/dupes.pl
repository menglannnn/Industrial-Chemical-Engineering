#!/usr/bin/env perl
# Duplicate-code finder: windows of W normalised code lines (whitespace collapsed; blank, comment-only and brace-only
# lines skipped) that occur at two or more places. Adjacent windows merge into runs. Usage:
#   perl dupes.pl [--window W] [--min M] <dirs or files...>     (default W=6, runs of at least M=8 code lines)
# Prints runs by length: "<lines> <file:line-line> <file:line-line> ..." (only the first 6 places of a cluster).
use strict; use warnings; use File::Find; use Digest::MD5 qw(md5_hex);
my ($W, $M, $NORM) = (6, 8, 0);
while (@ARGV && $ARGV[0] =~ /^--/) { my $o = shift; if ($o eq '--window') { $W = shift } elsif ($o eq '--min') { $M = shift } elsif ($o eq '--norm') { $NORM = 1 } }
my @files; for my $a (@ARGV) { if (-d $a) { find(sub { push @files, $File::Find::name if /\.java$/ }, $a) } else { push @files, $a } }
my %win;   # hash -> [ [file, idx] ... ]
my %lines; # file -> [ [lineno, text] ... ] code lines only
for my $f (sort @files) {
  open my $h, '<', $f or die "$f: $!"; my $n = 0; my $in = 0; my @code;
  while (my $x = <$h>) { $n++; $x =~ s/\r?\n$//; my $s = $x; $s =~ s/^\s+|\s+$//g;
    if ($in) { $in = 0 if $s =~ m{\*/}; next }
    next if $s eq '' || $s =~ m{^//} || $s =~ m{^\*};
    if ($s =~ m{^/\*}) { $in = 1 unless $s =~ m{\*/}; next }
    $s =~ s{//.*$}{}; $s =~ s/\s+/ /g; $s =~ s/^\s+|\s+$//g;
    if ($NORM) { $s =~ s/"[^"]*"/S/g; $s =~ s/\b[A-Za-z_]\w*\b/_/g; $s =~ s/\b\d[\w.]*\b/N/g; }
    next if $s =~ /^[{}();]*$/ || $s =~ /^(import|package) /;
    push @code, [$n, $s];
  }
  close $h; $lines{$f} = \@code;
  for my $i (0 .. $#code - $W + 1) { my $k = md5_hex(join "\n", map { $_->[1] } @code[$i .. $i + $W - 1]); push @{$win{$k}}, [$f, $i]; }
}
# runs: for each pair of places sharing a window, extend while consecutive windows also match
my %seen; my @runs;
for my $k (keys %win) { my @p = @{$win{$k}}; next if @p < 2;
  for my $a (0 .. $#p - 1) { for my $b ($a + 1 .. $#p) { my ($fa, $ia) = @{$p[$a]}; my ($fb, $ib) = @{$p[$b]};
    next if $fa eq $fb && abs($ia - $ib) < $W;   # overlapping self-match
    my $id = "$fa:$ia:$fb:$ib"; next if $seen{$id};
    # walk back to the start of the run
    my ($sa, $sb) = ($ia, $ib);
    while ($sa > 0 && $sb > 0 && $lines{$fa}[$sa - 1][1] eq $lines{$fb}[$sb - 1][1]) { $sa--; $sb--; }
    my ($ea, $eb) = ($sa, $sb);
    while ($ea < $#{$lines{$fa}} && $eb < $#{$lines{$fb}} && $lines{$fa}[$ea + 1][1] eq $lines{$fb}[$eb + 1][1]) { $ea++; $eb++; }
    for my $o (0 .. $ea - $sa - $W + 1) { $seen{"$fa:" . ($sa + $o) . ":$fb:" . ($sb + $o)} = 1; }
    my $len = $ea - $sa + 1; next if $len < $M;
    push @runs, [$len, "$fa:$lines{$fa}[$sa][0]-$lines{$fa}[$ea][0]", "$fb:$lines{$fb}[$sb][0]-$lines{$fb}[$eb][0]"];
  } }
}
my %cl; for my $r (sort { $b->[0] <=> $a->[0] } @runs) { my ($len, $a, $b) = @$r; my $key = $a; $key = $cl{$b} if $cl{$b}; $key = $cl{$a} if $cl{$a}; $cl{$a} //= $key; $cl{$b} //= $key; }
my %out; for my $r (@runs) { my ($len, $a, $b) = @$r; my $key = $cl{$a} // $a; $out{$key}{len} = $len if !$out{$key}{len} || $len > $out{$key}{len}; $out{$key}{p}{$a} = 1; $out{$key}{p}{$b} = 1; }
my $sum = 0;
for my $k (sort { $out{$b}{len} <=> $out{$a}{len} } keys %out) { my @p = sort keys %{$out{$k}{p}}; $sum += $out{$k}{len} * (@p - 1); printf "%4d  %s\n", $out{$k}{len}, join('  ', @p[0 .. ($#p < 5 ? $#p : 5)]) . (@p > 6 ? "  (+" . (@p - 6) . ")" : ''); }
print "duplicate code lines beyond the first copy (sum over clusters): $sum\n";
