#!/usr/bin/env perl
# Replaces line ranges of a file from a spec, bottom-up so earlier line numbers stay valid. Usage:
#   perl condense.pl <file> <spec.pl>
# The spec is a perl file returning a list of [start, count, phrase, replacement]: the block of `count` lines from line
# `start` (1-based) is replaced by `replacement` (a string, possibly empty or multi-line; one trailing newline is
# dropped), after checking that the block contains `phrase` (line breaks, comment stars and runs of whitespace
# ignored). The file's line ending is kept. Dies without writing when a check fails.
use strict; use warnings; use File::Spec;
my ($f, $spec) = @ARGV; die "usage: condense.pl <file> <spec.pl>" unless $f && $spec;
my @edits = do File::Spec->rel2abs($spec); die "spec $spec: $@" if $@; die "spec returned nothing" unless @edits;
open my $h, '<', $f or die "$f: $!"; my @l = <$h>; close $h;
my $eol = (@l && $l[0] =~ /\r\n$/) ? "\r\n" : "\n";
sub norm { my $s = shift; $s =~ s/\r?\n\s*\*?\s*/ /g; $s =~ s/\s+/ /g; return $s; }
my %seen;
for my $e (sort { $b->[0] <=> $a->[0] } @edits) {
  my ($start, $count, $check, $new) = @$e;
  die "edit at $start given twice" if $seen{$start}++;
  die "line $start is past the end" if $start < 1 || $start + $count - 1 > @l;
  my $block = join "", @l[$start - 1 .. $start + $count - 2];
  die "block at $start does not contain '$check'" unless index(norm($block), norm($check)) >= 0;
  $new =~ s/\n\z//;
  my @rep = $new eq '' ? () : map { "$_$eol" } split /\n/, $new, -1;
  splice @l, $start - 1, $count, @rep;
}
open my $o, '>', $f or die "$f: $!"; binmode $o; print $o @l; close $o;
printf "%s: %d edits applied\n", $f, scalar @edits;
