#!/usr/bin/env perl
# Seed 85 trace: prints a QFRECON line for every non-junction node a reconstruction books with solids and no fluid. File: science/fluid/network/ConservativeTransport.java.
# Usage: perl reconstruct-trace.pl <file> (the named file of an export of 31e25f6f; CRLF as git archive writes it).
use strict; use warnings;
my $f = shift; local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = "        return new Projection(inventories,states,boundaries,external,externalEnergy,pumpWork,filters,held);";
my $add = '        for(int qn=0;qn<inventories.size();qn++){var qi=inventories.get(qn);double qs=0;for(double v:qi.moles())qs+=v;if(qs==0&&!qi.solids().empty()&&!graph.reservoirs().get(qn).junction())System.out.println("QFRECON node="+graph.reservoirs().get(qn).id()+" kind="+graph.reservoirs().get(qn).kind()+" emptying="+(emptying!=null&&emptying[qn]!=null)+" endMass="+endMass[qn]+" solids="+qi.solids().massKg()+" energy="+qi.internalEnergy()+" oldMoles="+java.util.Arrays.toString(graph.reservoirs().get(qn).inventory().moles())+" oldSolids="+graph.reservoirs().get(qn).inventory().solids().massKg()+" stateT="+states.get(qn).temperature()+" index="+index[qn]);}' . "\r\n";
my $n = index($s, $from); die "not found" if $n < 0;
substr($s, $n, 0) = $add;
open $h, '>:raw', $f or die; print $h $s; close $h; print "ok\n";
