#!/usr/bin/env perl
# Seed 85 trace: prints a QFDONOR line (the donor end's state, phases, solids) where PassiveStepSolver's start flow meets a donor viscosity that is not positive. File: science/fluid/network/PassiveStepSolver.java.
# Usage: perl donor-trace.pl <file> (the named file of an export of 31e25f6f; CRLF as git archive writes it).
use strict; use warnings;
my $f = shift; local $/; open my $h, '<:raw', $f or die; my $s = <$h>; close $h;
my $from = "        double rho=endDensity(donor,donorPort),mu=endViscosity(donor,donorPort);";
my $add = "\r\n" . '        if(!(mu>0)){System.out.println("QFDONOR first="+pipe.first()+" second="+pipe.second()+" driving="+driving+" donorPort="+donorPort+" leading="+PhaseDraw.leading(model,donor,donorPort)+" rho="+rho+" mu="+mu+" T="+donor.temperature()+" P="+donor.pressure()+" mass="+donor.mass()+" volume="+donor.volume()+" Vl="+donor.liquidVolume()+" Vw="+donor.waterVolume()+" Vv="+donor.vaporVolume()+" wl="+donor.waterLiquid()+" wv="+donor.waterVapor()+" liquid="+java.util.Arrays.toString(donor.liquidView())+" vapor="+java.util.Arrays.toString(donor.vaporView())+" solids="+java.util.Arrays.toString(donor.solidMoments().values())+" pops="+donor.solids()+" junction="+(driving>=0?a:b).junction()+" id="+(driving>=0?a:b).id());}';
my $n = index($s, $from); die "not found" if $n < 0;
substr($s, $n + length($from), 0) = $add;
open $h, '>:raw', $f or die; print $h $s; close $h; print "ok\n";
