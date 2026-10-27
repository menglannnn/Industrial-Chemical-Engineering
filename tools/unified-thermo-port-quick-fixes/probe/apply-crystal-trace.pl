#!/usr/bin/env perl
# Trace for cold seed 183 (E9 "the fluid left by ... disagrees with the crystal"): prints, where depositFromFluid refuses
# its answer, the state (T, P, feed), the search's last f (phase 0), its iterations, and each fluid phase's kind, amount
# and (mu_i - mu_s)/RT as a QFXTAL line. Applied to an exported tree only: perl apply-crystal-trace.pl /tmp/forms-<name>
use strict; use warnings;
my $d = shift or die "usage: apply-crystal-trace.pl <tree>";
my $f = "$d/src/main/java/com/wormzjl/createcheme/science/thermo/phase/FluidTpEquilibrium.java";
local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = '            if (!(residual <= DEPOSITION_TOLERANCE)) {';
my $add = '            if (!(residual <= DEPOSITION_TOLERANCE)) { StringBuilder qf = new StringBuilder("QFXTAL t=" + t + " p=" + p + " f=" + f + " iterations=" + iterations + " class=" + reduced.classification() + " feed=" + java.util.Arrays.toString(java.util.Arrays.copyOf(w.feed, count)) + " z=" + z + " s=" + s);'
 . ' for (PhaseAmounts qp : reduced.phases()) qf.append(" | ").append(qp.kind()).append(" total=").append(qp.total()).append(" f_k=").append((qp.state().chemicalPotential(i) - solidMu) / rt);'
 . ' System.out.println(qf); }' . "\r\n";
my $n = index($s, $from); die "not found" if $n < 0;
substr($s, $n, 0) = $add;
open $h, '>:raw', $f or die; print $h $s; close $h; print "applied crystal trace to $d\n";
