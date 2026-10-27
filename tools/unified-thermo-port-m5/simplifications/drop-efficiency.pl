#!/usr/bin/perl
# S6: rewrites every "new FlowControl.Pump(a,b,c)" and "new FlowControl.Compressor(a,b,c)" (also with the package
# prefix) to "(a,b)": the third top-level argument, the efficiency, is dropped. Parentheses are matched, so arguments
# may be calls. Prints "file: n" per rewritten file and the dropped arguments that are not exactly 1.
#   perl drop-efficiency.pl <java files...>
use strict; use warnings;
for my $file (@ARGV) {
    open my $in, '<', $file or die "$file: $!"; local $/; my $s = <$in>; close $in;
    my ($out, $n, $pos) = ('', 0, 0);
    while ($s =~ /new\s+(?:[\w.]*\.)?(?:FlowControl\.)(Pump|Compressor)\(/g) {
        my $open = pos($s) - 1; my $depth = 0; my @commas; my $close;
        for (my $i = $open; $i < length $s; $i++) {
            my $c = substr($s, $i, 1);
            if ($c eq '(') { $depth++ } elsif ($c eq ')') { $depth--; if ($depth == 0) { $close = $i; last } }
            elsif ($c eq ',' && $depth == 1) { push @commas, $i }
        }
        die "$file: unbalanced at $open" unless defined $close;
        next unless @commas == 2;
        my $dropped = substr($s, $commas[1] + 1, $close - $commas[1] - 1);
        print "$file: dropped efficiency '$dropped'\n" unless $dropped =~ /^\s*1(\.0*)?\s*$/;
        $out .= substr($s, $pos, $commas[1] - $pos); $pos = $close; $n++;
        pos($s) = $close;
    }
    $out .= substr($s, $pos);
    if ($n) { open my $o, '>', $file or die; binmode $o; print $o $out; close $o; print "$file: $n\n" }
}
