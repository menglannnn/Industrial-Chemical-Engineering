#!/usr/bin/perl
# t285 prototype P2c (export only, on top of p2b.pl): the junction closure reads the D38 C vanishing rule on every state it
# evaluates; a phase of its start driven below VANISHING_SHARE and shrinking ends the closure at once, refused (no crawl).
use strict; my $r=shift; my $S="$r/src/main/java/com/wormzjl/createcheme/science/fluid";
sub edit { my($f,$code)=@_; local $/; open(my $h,"<",$f) or die $f; my $t=<$h>; close $h; my $o=$t; $t=$code->($t); die "no change $f" if $t eq $o; open($h,">",$f); print $h $t; close $h; }
edit("$S/solver/PhaseLayout.java", sub { my $t=shift;
  $t=~s/public SparseNewton.Equations junctionClosure\(double\[\] massFractions,double specificEnthalpy,double pressure\) \{/public static final class Vanished extends RuntimeException{public Vanished(){super(null,null,false,false);}}\n    public SparseNewton.Equations junctionClosure(double[] massFractions,double specificEnthalpy,double pressure,FluidThermodynamics.State start) {/ or die "sig";
  $t=~s/double\[\] f=new double\[size\(\)\];junctionResidual\(decode\(x,0,local\),w,specificEnthalpy,0,pressure,f,0,x,local,1\);return f;/var decoded=decode(x,0,local);if(FluidThermodynamics.vanishing(start,decoded)!=0)throw new Vanished();double[] f=new double[size()];junctionResidual(decoded,w,specificEnthalpy,0,pressure,f,0,x,local,1);return f;/ or die "res"; $t });
edit("$S/network/PassiveStepSolver.java", sub { my $t=shift;
  $t=~s/layout\.junctionClosure\(w,enthalpy\/mass,p\)/layout.junctionClosure(w,enthalpy\/mass,p,start)/ or die "call";
  $t=~s/catch\(SparseNewton\.Nonconvergence\|IllegalArgumentException failed\)\{if\(Boolean\.getBoolean\("t285\.trace"\)\)/catch(PhaseLayout.Vanished vanished){return null;}\n        catch(SparseNewton.Nonconvergence|IllegalArgumentException failed){if(Boolean.getBoolean("t285.trace"))/ or die "catch"; $t });
print "p2c applied\n";
