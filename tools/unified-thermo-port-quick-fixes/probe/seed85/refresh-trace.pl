#!/usr/bin/env perl
# Seed 85 trace: prints a QFREFRESH line for every vessel refresh that settles (inventory before and after, residue test). File: the vessel refresh in science/fluid/network/InventoryEquilibrium.java.
# Usage: perl refresh-trace.pl <file> (the named file of an export of 31e25f6f; CRLF as git archive writes it).
use strict; use warnings;
my $f = shift; local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = "        if(settled!=null) {";
my $add = '        if(settled!=null)System.out.println("QFREFRESH node="+node.id()+" residue="+PassiveStepSolver.holdsNothing(inventory,state)+" mass="+java.util.Arrays.toString(inventory.moles())+" solidsBefore="+inventory.solids().massKg()+" solidsAfter="+settled.inventory().solids().massKg()+" fluidAfter="+java.util.Arrays.toString(settled.inventory().moles())+" T="+state.temperature()+" P="+state.pressure()+" Tafter="+settled.state().temperature()+" capacity="+inventory.volume()+" energy="+inventory.internalEnergy());' . "\r\n";
my $n = index($s, $from); die "not found" if $n < 0;
substr($s, $n, 0) = $add;
open $h, '>:raw', $f or die; print $h $s; close $h; print "ok\n";
