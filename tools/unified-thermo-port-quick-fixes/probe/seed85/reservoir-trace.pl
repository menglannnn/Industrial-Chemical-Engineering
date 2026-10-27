#!/usr/bin/env perl
# Seed 85 trace: prints a QFRES line and stack (first 4) where a vessel snapshot is built with solids in its state and none, or no fluid, in its inventory. File: science/fluid/network/PassiveNetwork.java.
# Usage: perl reservoir-trace.pl <file> (the named file of an export of 31e25f6f; CRLF as git archive writes it).
use strict; use warnings;
my $f = shift; local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = 'if(!Double.isFinite(elevation)||inventory.moles.length!=state.componentCount())throw new IllegalArgumentException("Invalid reservoir snapshot");}';
my $to = 'if(!Double.isFinite(elevation)||inventory.moles.length!=state.componentCount())throw new IllegalArgumentException("Invalid reservoir snapshot");'
 . 'if(kind==NodeKind.RESERVOIR&&!state.solids().empty()&&(inventory.solids.empty()||java.util.Arrays.stream(inventory.moles).sum()==0)&&QF_RES.incrementAndGet()<=4){System.out.println("QFRES id="+id+" stateSolids="+state.solids().massKg()+" invSolids="+inventory.solids.massKg()+" invMoles="+java.util.Arrays.toString(inventory.moles)+" T="+state.temperature());new Throwable("QFRES").printStackTrace(System.out);}}'
 . ' static final java.util.concurrent.atomic.AtomicInteger QF_RES=new java.util.concurrent.atomic.AtomicInteger();';
my $n = index($s, $from); die "not found" if $n < 0;
substr($s, $n, length($from)) = $to;
open $h, '>:raw', $f or die; print $h $s; close $h; print "ok\n";
