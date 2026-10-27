#!/usr/bin/perl
# t285 (export only, on top of p2b+p2c): the junction closure owned by FluidThermodynamics (junctionClosure), used by
# phaseCheck when the caller gives no split, so a direct phaseCheck call on a junction states the same rule.
use strict; my $r=shift; my $f="$r/src/main/java/com/wormzjl/createcheme/science/fluid/thermo/FluidThermodynamics.java";
local $/; open(my $h,"<",$f) or die; my $t=<$h>; close $h;
$t=~s/own=vesselSplit==null\?null:vesselSplit\.apply\(start\);\}/own=vesselSplit!=null?vesselSplit.apply(start):junctionClosure(totals,state.enthalpy(),state.mass(),kept.pressure(),start,checkpoint);}/ or die "own";
my $add=<<'J';
    /** t285 P2d: a junction's own rows closed at {@code p} and its specific enthalpy from {@code start}; null where it fails or a phase vanishes. */
    public State junctionClosure(double[] n,double h,double mass,double p,State start,Runnable checkpoint) {
        try{
            var layout=new PhaseLayout(this,start,null,null,PhaseLayout.support(this,start,null,null,true));
            double[] w=new double[n.length];
            for(int i=0;i<n.length;i++)w[i]=n[i]*(i==n.length-1?waterMolecularWeight:hydrocarbon.molecularWeight(i))/mass;
            var solved=com.wormzjl.createcheme.science.fluid.solver.SparseNewton.solve(layout.junctionClosure(w,h/mass,p,start),layout.encode(start),
                    new com.wormzjl.createcheme.science.fluid.solver.SparseNewton.Settings(com.wormzjl.createcheme.science.fluid.solver.SparseNewton.ITERATIONS,1e-10,1e-6,24),checkpoint);
            return layout.decode(solved.variables(),0);
        }
        catch(PhaseLayout.Vanished|com.wormzjl.createcheme.science.fluid.solver.SparseNewton.Nonconvergence|IllegalArgumentException failed){return null;}
    }
J
$t=~s/(\n[ \t]*\/\*\*\r?\n\s+\* \{\@link #phaseBits\} of the phases a node holds as its own)/\n$add$1/ or die "anchor";
open($h,">",$f); print $h $t; close $h; print "p2d applied\n";
