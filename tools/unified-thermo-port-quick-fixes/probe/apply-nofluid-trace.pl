#!/usr/bin/env perl
# Follow-up trace (decision 2, dry end): which vessels holding solids the widened rule judges as holding no fluid, in an
# exported tree of 95e4325d (/tmp/forms-<name>; the worktree is never touched). Prints at most 12 `QFNOFLUID` lines (a
# vessel with solids and a fluid at most the bag's resolution: its solids kg, moles, bag m3, capacity, T, whether its
# state holds a fluid), 12 `QFDRYBED` lines (solids and no moles at all) and 5 `QFSTANDIN` lines (a viscosity read on a
# state holding no fluid, answered by the ambient's). Cold seed 85 printed only QFNOFLUID: roundoff tanks of 1e-126 to
# 1e-92 kg of crystal and 1e-62 to 1e-45 mol of fluid at 298.15 K.
#   perl apply-nofluid-trace.pl /tmp/forms-<name>; index-compile.sh <name>; run-seed.sh <name> 85 <label>; grep ^QF <log>
use strict; use warnings;
my $d = shift; my $f = "$d/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = 'return (inventory.solids().empty()?bag:bag-inventory.solids().volume())<=HOLDS_NOTHING*capacity;';
my $to = 'boolean qfResult=(inventory.solids().empty()?bag:bag-inventory.solids().volume())<=HOLDS_NOTHING*capacity;'
 . 'if(qfResult&&!inventory.solids().empty()&&QF_NOFLUID.incrementAndGet()<=12)System.out.println("QFNOFLUID solids="+inventory.solids().massKg()+" moles="+java.util.Arrays.stream(inventory.moles()).sum()+" bag="+bag+" cap="+capacity+" T="+state.temperature()+" holdsFluid="+FluidThermodynamics.holdsFluid(state));'
 . 'return qfResult;';
my $n = index($s, $from); die "nf1" if $n < 0; substr($s, $n, length($from)) = $to;
my $from2 = 'boolean none=true;for(double n:inventory.moles())if(n!=0){none=false;break;}' . "\r\n" . '        if(none)return true;';
my $to2 = 'boolean none=true;for(double n:inventory.moles())if(n!=0){none=false;break;}' . "\r\n" . '        if(none){if(!inventory.solids().empty()&&QF_DRY.incrementAndGet()<=12)System.out.println("QFDRYBED solids="+inventory.solids().massKg()+" T="+state.temperature()+" holdsFluid="+FluidThermodynamics.holdsFluid(state));return true;}';
$n = index($s, $from2); die "nf2" if $n < 0; substr($s, $n, length($from2)) = $to2;
my $from3 = 'static final double HOLDS_NOTHING=1e-12;';
$n = index($s, $from3); die "nf3" if $n < 0;
substr($s, $n, length($from3)) = $from3 . ' static final java.util.concurrent.atomic.AtomicInteger QF_NOFLUID=new java.util.concurrent.atomic.AtomicInteger(),QF_DRY=new java.util.concurrent.atomic.AtomicInteger(),QF_STANDIN=new java.util.concurrent.atomic.AtomicInteger();';
my $from4 = 'if(!FluidThermodynamics.holdsFluid(s))return viscosity(ambient(),null);';
$n = index($s, $from4); die "nf4" if $n < 0;
substr($s, $n, length($from4)) = 'if(!FluidThermodynamics.holdsFluid(s)){if(QF_STANDIN.incrementAndGet()<=5)System.out.println("QFSTANDIN T="+s.temperature()+" solids="+s.solids().massKg());return viscosity(ambient(),null);}';
open $h, '>:raw', $f or die; print $h $s; close $h; print "probe applied\n";
