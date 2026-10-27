#!/usr/bin/env perl
# Applies a block list of exact text replacements to a source tree (the lead's behaviour-free cleanup of 2026-10-17).
# Usage: perl apply-edits.pl <tree root> <edits file>. Blocks: "@@ <path> <count>", the old text, a line ">>", the new
# text, up to the next "@@" line. Each old text must occur exactly <count> times (all are replaced). A file with CRLF
# line ends gets CRLF in the texts. Nothing is written unless every block of every file matches.
use strict; use warnings;
my ($root, $edits) = @ARGV; die "usage: apply-edits.pl <root> <edits>" unless $root && $edits;
open my $e, '<:raw', $edits or die "$edits: $!"; my @lines = map { s/\r?\n\z//r } <$e>; close $e;
my (@blocks, $cur, $mode);
for my $l (@lines) {
  if ($l =~ /^@@ (\S+) (\d+)$/) { push @blocks, $cur if $cur; $cur = { file => $1, count => $2, old => [], new => [] }; $mode = 'old'; next; }
  next unless $cur;
  if ($mode eq 'old' && $l eq '>>') { $mode = 'new'; next; }
  push @{ $cur->{$mode} }, $l;
}
push @blocks, $cur if $cur;
my (%byfile, @order); for (@blocks) { push @order, $_->{file} unless $byfile{$_->{file}}; push @{ $byfile{$_->{file}} }, $_; }
my ($failed, %out) = (0);
for my $file (@order) {
  my $path = "$root/$file"; local $/; open my $h, '<:raw', $path or die "$path: $!"; my $s = <$h>; close $h;
  my $nl = $s =~ /\r\n/ ? "\r\n" : "\n";
  for my $b (@{ $byfile{$file} }) {
    my $old = join($nl, @{ $b->{old} }); my $new = join($nl, @{ $b->{new} });
    my $n = () = $s =~ /\Q$old\E/g;
    if ($n != $b->{count}) { print "MISMATCH $file: found $n, want $b->{count}: ", substr($old, 0, 100), "\n"; $failed++; next; }
    $s =~ s/\Q$old\E/$new/g;
  }
  $out{$file} = $s;
}
die "$failed block(s) did not match; nothing written\n" if $failed;
for my $file (@order) { open my $h, '>:raw', "$root/$file" or die; print $h $out{$file}; close $h; print "edited $file (", scalar @{ $byfile{$file} }, " blocks)\n"; }
