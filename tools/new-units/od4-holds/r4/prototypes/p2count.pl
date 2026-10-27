#!/usr/bin/perl
# t285: closure counters for an export already carrying P2 (export only). Prints at JVM exit:
# T285COUNT closures=<n> failed=<n> closureNewtonIterations=<sum of newtonIterations deltas across closures> (exact with one worker).
use strict; my $r=shift; my $f="$r/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local $/; open(my $h,"<",$f) or die; my $t=<$h>; close $h;
my $add = <<'J';
    static final java.util.concurrent.atomic.LongAdder T285_N=new java.util.concurrent.atomic.LongAdder(),T285_F=new java.util.concurrent.atomic.LongAdder(),T285_IT=new java.util.concurrent.atomic.LongAdder();
    static{Runtime.getRuntime().addShutdownHook(new Thread(()->System.out.println("T285COUNT closures="+T285_N.sum()+" failed="+T285_F.sum()+" closureNewtonIterations="+T285_IT.sum())));}
    private FluidThermodynamics.State junctionClosure(double[] n,double enthalpy,double mass,double p,FluidThermodynamics.State start,Runnable checkpoint) {
        long before=SolverDiagnostics.newtonIterations.sum();T285_N.increment();
        try{var closed=junctionClosure0(n,enthalpy,mass,p,start,checkpoint);if(closed==null)T285_F.increment();return closed;}
        finally{T285_IT.add(SolverDiagnostics.newtonIterations.sum()-before);}
    }
J
$t=~s/    private FluidThermodynamics.State junctionClosure\(double\[\] n,double enthalpy,double mass,double p,FluidThermodynamics.State start,Runnable checkpoint\) \{/$add    private FluidThermodynamics.State junctionClosure0(double[] n,double enthalpy,double mass,double p,FluidThermodynamics.State start,Runnable checkpoint) {/ or die "anchor";
open($h,">",$f); print $h $t; close $h; print "p2count applied\n";
