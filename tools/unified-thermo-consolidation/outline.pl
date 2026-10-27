#!/usr/bin/env perl
# Outline of a Java file: type and member declarations at indentation 0 to 4 with their line, and comment blocks of at
# least MIN lines (default 8) with their start line, length and first words. Usage: perl outline.pl [--min N] <file>
use strict; use warnings;
my $min = 8; if (@ARGV && $ARGV[0] eq '--min') { shift; $min = shift; }
my $f = shift or die "usage: outline.pl [--min N] <file>";
open my $h, '<', $f or die "$f: $!"; my @l = <$h>; close $h;
my ($in, $start, $run, $first) = (0, 0, 0, '');
my @out;
for my $i (0 .. $#l) { my $x = $l[$i]; $x =~ s/\r?\n$//; my $s = $x; $s =~ s/^\s+|\s+$//g;
  my $isc = 0;
  if ($in) { $isc = 1; $in = 0 if $s =~ m{\*/}; }
  elsif ($s =~ m{^//}) { $isc = 1; }
  elsif ($s =~ m{^/\*}) { $isc = 1; $in = 1 unless $s =~ m{\*/}; }
  if ($isc) { if (!$run) { $start = $i + 1; $first = $s; $first =~ s{^(/\*\*?|//|\*)\s*}{}; } $run++; next; }
  if ($run >= $min) { push @out, sprintf("%5d  C%-4d %s", $start, $run, substr($first, 0, 90)); }
  $run = 0;
  next if $s eq '';
  my $ind = length($x) - length($x =~ s/^\s+//r);
  next if $ind > 4;
  if ($s =~ /\b(class|record|enum|interface)\s+\w+/ && $s !~ /^(return|new)\b/) { push @out, sprintf("%5d  %s%s", $i + 1, ' ' x $ind, substr($s, 0, 110)); next; }
  if ($s =~ /^(?:(?:public|private|protected|static|final|abstract|synchronized|default)\s+)*[\w<>\[\], .?]+\s+\w+\s*\(/ && $s !~ /^(?:return|new|if|for|while|switch|else|throw)\b/ && $s !~ /=/ ) {
    push @out, sprintf("%5d  %s%s", $i + 1, ' ' x $ind, substr($s, 0, 110)); next; }
  if ($ind <= 4 && $s =~ /^(?:(?:public|private|protected|static|final|transient|volatile)\s+)+[\w<>\[\], .?]+\s+\w+\s*(=|;)/) { push @out, sprintf("%5d  %sF %s", $i + 1, ' ' x $ind, substr($s, 0, 100)); }
}
if ($run >= $min) { push @out, sprintf("%5d  C%-4d %s", $start, $run, substr($first, 0, 90)); }
print "$_\n" for @out;
