#!/usr/bin/env perl
# As condense.pl, for several files from one spec: the spec returns a list of [file, start, count, phrase, replacement],
# file relative to the current directory. Each file's edits are applied bottom-up; every check runs before any file
# is written. The phrase check ignores line breaks, comment stars and runs of whitespace. Usage:
#   perl condense-multi.pl <spec.pl>
use strict; use warnings; use File::Spec;
my ($spec) = @ARGV; die "usage: condense-multi.pl <spec.pl>" unless $spec;
my @edits = do File::Spec->rel2abs($spec); die "spec $spec: $@" if $@; die "spec returned nothing" unless @edits;
my (%lines, %eol, %done);
for my $e (@edits) { my $f = $e->[0]; next if $lines{$f};
  open my $h, '<', $f or die "$f: $!"; my @l = <$h>; close $h; $lines{$f} = \@l; $eol{$f} = (@l && $l[0] =~ /\r\n$/) ? "\r\n" : "\n"; }
sub norm { my $s = shift; $s =~ s/\r?\n\s*\*?\s*/ /g; $s =~ s/\s+/ /g; return $s; }
my %seen;
for my $e (sort { $a->[0] cmp $b->[0] || $b->[1] <=> $a->[1] } @edits) {
  my ($f, $start, $count, $check, $new) = @$e; my $l = $lines{$f};
  die "$f: edit at $start given twice" if $seen{"$f:$start"}++;
  die "$f: line $start is past the end" if $start < 1 || $start + $count - 1 > @$l;
  my $block = join "", @$l[$start - 1 .. $start + $count - 2];
  die "$f: block at $start does not contain '$check'" unless index(norm($block), norm($check)) >= 0;
  $new =~ s/\n\z//;
  my @rep = $new eq '' ? () : map { "$_$eol{$f}" } split /\n/, $new, -1;
  splice @$l, $start - 1, $count, @rep; $done{$f}++;
}
for my $f (sort keys %done) { open my $o, '>', $f or die "$f: $!"; binmode $o; print $o @{$lines{$f}}; close $o; printf "%s: %d edits applied\n", $f, $done{$f}; }
