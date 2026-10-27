#!/usr/bin/env perl
# Comment metrics per Java file: total, code, comment and blank lines, the largest comment block and the lines held
# in blocks of at least MIN lines (default 8). Usage: perl comments.pl [--min N] <dir or files...>
use strict; use warnings; use File::Find;
my $min = 8; if (@ARGV && $ARGV[0] eq '--min') { shift; $min = shift; }
my @files; for my $a (@ARGV) { if (-d $a) { find(sub { push @files, $File::Find::name if /\.java$/ }, $a) } else { push @files, $a } }
my ($T, $C, $B, $K, $L) = (0, 0, 0, 0, 0);
my @rows;
for my $f (sort @files) {
  open my $h, '<', $f or die "$f: $!"; my @l = <$h>; close $h;
  my ($in, $tot, $com, $blank, $run, $maxrun, $big, $nbig) = (0, scalar @l, 0, 0, 0, 0, 0, 0);
  my @runs;
  for my $x (@l) { $x =~ s/\r?\n$//; my $s = $x; $s =~ s/^\s+|\s+$//g;
    my $isc = 0;
    if ($in) { $isc = 1; $in = 0 if $s =~ m{\*/}; }
    elsif ($s eq '') { $blank++; }
    elsif ($s =~ m{^//}) { $isc = 1; }
    elsif ($s =~ m{^/\*}) { $isc = 1; $in = 1 unless $s =~ m{\*/}; }
    if ($isc) { $com++; $run++; } else { push @runs, $run if $run; $maxrun = $run if $run > $maxrun; $run = 0; }
  }
  push @runs, $run if $run; $maxrun = $run if $run > $maxrun;
  for (@runs) { if ($_ >= $min) { $big += $_; $nbig++; } }
  my $code = $tot - $com - $blank;
  push @rows, [$tot, $code, $com, $blank, $maxrun, $nbig, $big, $f];
  $T += $tot; $C += $com; $B += $blank; $K += $code; $L += $big;
}
printf "%6s %6s %6s %5s %5s %4s %6s %s\n", 'total', 'code', 'comm', 'blank', 'maxrn', 'nbig', 'inbig', 'file';
for my $r (sort { $b->[6] <=> $a->[6] || $b->[2] <=> $a->[2] } @rows) { printf "%6d %6d %6d %5d %5d %4d %6d %s\n", @$r; }
printf "SUM total=%d code=%d comment=%d blank=%d in-big-blocks(>=%d)=%d files=%d\n", $T, $K, $C, $B, $min, $L, scalar @rows;
