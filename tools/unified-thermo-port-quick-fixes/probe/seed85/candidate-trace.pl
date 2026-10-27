#!/usr/bin/env perl
# Seed 85 trace: prints a QFCAND line for every unlaid vessel whose Newton candidate carries solids (the candidate's phases and solids beside the old state). File: science/fluid/network/ConservativeTransport.java at 31e25f6f.
# Usage: perl candidate-trace.pl <file> (the named file of an export of 31e25f6f; CRLF as git archive writes it).
use strict; use warnings;
my $f = shift; local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = "            var reservoir=graph.reservoirs().get(node);if(index[node]<0){moles[node]=old[node];states.add(candidate.get(node));continue;}";
my $to = "            var reservoir=graph.reservoirs().get(node);if(index[node]<0){moles[node]=old[node];states.add(candidate.get(node));"
 . 'if(reservoir.kind()==PassiveNetwork.NodeKind.RESERVOIR&&!candidate.get(node).solids().empty()){var qc=candidate.get(node);var qo=reservoir.state();System.out.println("QFCAND node="+reservoir.id()+" candT="+qc.temperature()+" candP="+qc.pressure()+" candL="+java.util.Arrays.toString(qc.liquidView())+" candV="+java.util.Arrays.toString(qc.vaporView())+" candWl="+qc.waterLiquid()+" candWv="+qc.waterVapor()+" candSolids="+qc.solids()+" oldT="+qo.temperature()+" oldV="+java.util.Arrays.toString(qo.vaporView())+" oldSolids="+qo.solids().massKg()+" sameAsOld="+(qc==qo));}'
 . "continue;}";
my $n = index($s, $from); die "not found" if $n < 0;
substr($s, $n, length($from)) = $to;
open $h, '>:raw', $f or die; print $h $s; close $h; print "ok\n";
