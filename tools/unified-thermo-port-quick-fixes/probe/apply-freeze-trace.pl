#!/usr/bin/env perl
# Trace of the fixed-volume freeze step (FluidTpEquilibrium.freezingAtVolume, 703d69b3): prints a DBG line for each try
# (its p0 and the TP calls before it), each pressure on the line (T_m, beta, V_spec/V - 1, ln(V_spec/V), slope, the next
# step in ln P), a warm side without liquid water, the exit where beta lies beyond [0, 1] on the root's side, and which
# caller tried it (DBG site newton / nested). With -Ddbg.tp=true also every counted TP call of the search (DBG TP).
# Applied to an exported tree only:  perl apply-freeze-trace.pl /tmp/forms-<name>, then recompile that file into main/:
#   javac -nowarn -encoding UTF-8 -proc:none -d <tree>/main -cp "<tree>/main;<libs of main-cp.txt>" <the file>
use strict; use warnings;
my $d = shift or die "usage: apply-freeze-trace.pl <tree>";
my $f = "$d/src/main/java/com/wormzjl/createcheme/science/thermo/phase/FluidTpEquilibrium.java";
local $/; open my $h, '<:raw', $f or die "$f: $!"; my $s = <$h>; close $h;
$s =~ s{(        w\.freezeAttempts\+\+;\r?\n)}{$1        System.out.println("DBG freeze attempt " + w.freezeAttempts + " p0 " + p0 + " outer " + w.outerEvaluations);\n} or die "attempt";
$s =~ s{(            double next = p - gap / slope;\r?\n)}{$1            System.out.println("DBG k " + k + " p " + p + " t " + t + " beta " + beta + " gap " + gap + " r " + DeterministicMath.log(v / volume) + " slope " + slope + " step " + (next - p) / p + " outer " + w.outerEvaluations);\n} or die "iterate";
$s =~ s{(            if \(warm == null\) \{\r?\n                // No liquid water)}{            if (warm == null) System.out.println("DBG warm null at " + p);\n$1} or die "warm";
$s =~ s{(            if \(beta < 0\.0 && gap < 0\.0 \|\| beta > 1\.0 && gap > 0\.0\) \{\r?\n)}{$1                System.out.println("DBG exit beta " + beta + " gap " + gap);\n} or die "exit";
$s =~ s{(        EquilibriumResult result = evaluateTp\(t, p, competition, w\);\r?\n        w\.outerEvaluations\+\+;\r?\n)}{$1        if (Boolean.getBoolean("dbg.tp")) System.out.println("DBG TP " + w.outerEvaluations + " t " + t + " p " + p + " " + result.status() + " " + result.classification() + (result.freeWater() == null ? "" : " ice " + result.freeWater().ice() + " liq " + result.freeWater().liquidWater()));\n} or die "tp";
$s =~ s{(                if \(w\.wet && f0 \* g0 <= 0\.0 && \(straddlesFreeze\(trial, current\) \|\| straddlesFreeze\(current, trial\)\)\) \{\r?\n)}{$1                    System.out.println("DBG site newton");\n} or die "newton";
$s =~ s{(            if \(w\.wet && last != null && crossesMelting\(last, t, last\.phases\(\)\.get\(0\)\.pressure\(\)\)\) \{\r?\n)}{$1                System.out.println("DBG site nested t " + t);\n} or die "nested";
open $h, '>:raw', $f or die; print $h $s; close $h; print "applied freeze trace to $d\n";
