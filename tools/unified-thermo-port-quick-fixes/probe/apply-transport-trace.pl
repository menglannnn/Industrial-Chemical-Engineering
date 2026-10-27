#!/usr/bin/env perl
# Trace for cold seed 85 (E9 "Invalid pipe transport properties"): prints the first 40 refusals in PipeResistance (mass
# flow, density, viscosity) as QFPIPE lines, and every node transport row whose density or viscosity is not finite and
# positive as a QFTRANS line (the state's T, P, phase amounts, volumes, solids, and the per-phase viscosities).
# Applied to an exported tree only: perl apply-transport-trace.pl /tmp/forms-<name>
use strict; use warnings;
my $d = shift or die "usage: apply-transport-trace.pl <tree>";
sub edit { my ($f, $from, $to) = @_; local $/; open my $h, '<', $f or die "$f: $!"; my $s = <$h>; close $h;
  my $n = ($s =~ s/\Q$from\E/$to/); die "pattern not found in $f: $from" unless $n; open $h, '>', $f or die; print $h $s; close $h; }
my $p = "$d/src/main/java/com/wormzjl/createcheme/science/fluid/network/PipeResistance.java";
edit($p, '            throw new IllegalArgumentException("Invalid pipe transport properties");',
  '            if(QF_COUNT.incrementAndGet()<=40)System.out.println("QFPIPE massFlow="+massFlow+" density="+density+" viscosity="+viscosity);'
  . "\n" . '            throw new IllegalArgumentException("Invalid pipe transport properties");');
edit($p, 'private PipeResistance() {}', 'private PipeResistance() {} static final java.util.concurrent.atomic.AtomicInteger QF_COUNT=new java.util.concurrent.atomic.AtomicInteger();');
my $s = "$d/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
edit($s, '            if(!drawsPhase[node])return bulk;',
  '            if(!(bulk.density>0)||!(bulk.viscosity>0)||!Double.isFinite(bulk.density)||!Double.isFinite(bulk.viscosity))qfTrace(node,state,prepared,bulk);' . "\n"
  . '            if(!drawsPhase[node])return bulk;');
edit($s, '        private static Transport phase(FluidThermodynamics.PhaseStream stream) {',
  '        private static final java.util.concurrent.atomic.AtomicInteger QF_TRANS=new java.util.concurrent.atomic.AtomicInteger();' . "\n"
  . '        private void qfTrace(int node,FluidThermodynamics.State s,FluidThermodynamics.Prepared prepared,Transport bulk) {' . "\n"
  . '            if(QF_TRANS.incrementAndGet()>20)return;' . "\n"
  . '            double t=s.temperature();String lv="-",wv="-",vv="-";' . "\n"
  . '            try{if(s.liquidVolume()>0)lv=String.valueOf(model.viscosity.liquid(t,s.liquidView(),prepared==null?null:prepared.viscosities()).pascalSeconds());}catch(RuntimeException e){lv="ERR "+e;}' . "\n"
  . '            try{if(s.waterVolume()>0)wv=String.valueOf(model.viscosity.waterLiquid(t,prepared==null?null:prepared.viscosities()));}catch(RuntimeException e){wv="ERR "+e;}' . "\n"
  . '            try{if(s.vaporVolume()>0)vv=String.valueOf(model.viscosity.vapor(t,s.vaporView(),s.waterVapor(),viscosities,prepared==null?null:prepared.viscosities()));}catch(RuntimeException e){vv="ERR "+e;}' . "\n"
  . '            System.out.println("QFTRANS node="+node+" id="+graph.reservoirs().get(node).id()+" junction="+graph.reservoirs().get(node).junction()+" T="+t+" P="+s.pressure()+" pc="+s.hydrocarbonPartialPressure()' . "\n"
  . '                +" density="+bulk.density+" viscosity="+bulk.viscosity+" mass="+s.mass()+" volume="+s.volume()+" Vl="+s.liquidVolume()+" Vw="+s.waterVolume()+" Vv="+s.vaporVolume()' . "\n"
  . '                +" wl="+s.waterLiquid()+" wv="+s.waterVapor()+" liquid="+java.util.Arrays.toString(s.liquidView())+" vapor="+java.util.Arrays.toString(s.vaporView())' . "\n"
  . '                +" solids="+java.util.Arrays.toString(s.solidMoments().values())+" muL="+lv+" muW="+wv+" muV="+vv+" bulkDrawsGas="+FluidThermodynamics.bulkDrawsGas(s));' . "\n"
  . '        }' . "\n"
  . '        private static Transport phase(FluidThermodynamics.PhaseStream stream) {');
print "applied transport trace to $d\n";
