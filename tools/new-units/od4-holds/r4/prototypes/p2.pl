#!/usr/bin/perl
# t285 prototype P2 (export only): a junction's own split is its own rows closed at its pressure and enthalpy
# (PhaseLayout.junctionClosure), from the engine's answer; a failed closure proposes nothing (D40 A); the arrival seed uses
# the same split. Usage: perl p2.pl <export-root>
use strict; my $r=shift; my $S="$r/src/main/java/com/wormzjl/createcheme/science/fluid";
sub edit { my($f,$code)=@_; local $/; open(my $h,"<",$f) or die $f; my $t=<$h>; close $h; my $o=$t; $t=$code->($t); die "no change $f" if $t eq $o; open($h,">",$f); print $h $t; close $h; }
edit("$S/solver/PhaseLayout.java", sub { my $t=shift;
  my $add = <<'J';
    /** t285 P2: a junction's own rows isolated at {@code pressure}: the mass fractions and specific enthalpy it holds. */
    public SparseNewton.Equations junctionClosure(double[] massFractions,double specificEnthalpy,double pressure) {
        double[] w=massFractions.clone();
        return new SparseNewton.Equations(){
            private volatile FluidThermodynamics.Prepared prepared;
            public int size(){return PhaseLayout.this.size;}
            public double differenceScale(int column,double value){return PhaseLayout.this.differenceStepScale(column,value);}
            public double maximumStep(double[] x,double[] direction){double alpha=1;for(int c=0;c<x.length;c++)if(totalAmountVariable(c)&&x[c]>0&&direction[c]<0)alpha=Math.min(alpha,.99*x[c]/-direction[c]);return alpha;}
            public int[][] columnRows(){int[][] rows=new int[size()][size()];for(int[] column:rows)for(int i=0;i<column.length;i++)column[i]=i;return rows;}
            public double[] residual(double[] x){
                double temperature=PhaseLayout.this.temperature(x,0);var local=prepared;
                if(local==null||local.temperature()!=temperature)prepared=local=model.prepare(temperature);
                double[] f=new double[size()];junctionResidual(decode(x,0,local),w,specificEnthalpy,0,pressure,f,0,x,local,1);return f;
            }
        };
    }
J
  $t=~s/(    \/\*\* A vessel's closure at what it holds: the amounts, the energy and the wall)/$add$1/ or die "PL anchor"; $t });
edit("$S/thermo/FluidThermodynamics.java", sub { my $t=shift;
  $t=~s/if\(junction\)\{start=tpSeed\(kept,equilibrium,prepared\);own=equilibriumPH\(kept\.temperature\(\),kept\.pressure\(\),state\.enthalpy\(\)-state\.solidMoments\(\)\.enthalpy\(state\.temperature\(\),state\.pressure\(\)\),totals,checkpoint\);\}/if(junction){var engine=equilibriumPH(kept.temperature(),kept.pressure(),state.enthalpy()-state.solidMoments().enthalpy(state.temperature(),state.pressure()),totals,checkpoint);start=engine!=null?engine.withSolidState(kept.solids(),kept.solidMoments()):tpSeed(kept,equilibrium,prepared);own=vesselSplit==null?null:vesselSplit.apply(start);}/ or die "FT junction";
  $t=~s/if\(own!=null\|\|junction\) \{/if(own!=null) {/ or die "FT own";
  $t });
edit("$S/network/PassiveStepSolver.java", sub { my $t=shift;
  $t=~s/node\.junction\(\)\?null:start->vesselSplit\(node,state,start,checkpoint\)/node.junction()?start->junctionSplit(state,start,checkpoint):start->vesselSplit(node,state,start,checkpoint)/ or die "PSS split";
  $t=~s/try\{var split=model\.equilibriumPH\(weighted\/mass,seed\.pressure\(\),enthalpy,n,false,checkpoint\);mixture=model\.folded\(split!=null\?split:model\.flashTP\(weighted\/mass,seed\.pressure\(\),n,checkpoint\),true\);\}/try{var split=model.equilibriumPH(weighted\/mass,seed.pressure(),enthalpy,n,false,checkpoint);var engineStart=split!=null?split:model.flashTP(weighted\/mass,seed.pressure(),n,checkpoint);var closed=junctionClosure(n,enthalpy,seed.pressure(),engineStart,checkpoint);if(closed==null)continue;mixture=model.folded(closed,true);}/ or die "PSS arrive";
  my $add = <<'J';
    /** t285 P2: a junction's own split at a pass's point {@code state}: its own rows closed at its pressure and enthalpy
     * from {@code start}; {@code null} where that closure fails. */
    private FluidThermodynamics.State junctionSplit(FluidThermodynamics.State state,FluidThermodynamics.State start,Runnable checkpoint) {
        return junctionClosure(PhaseLayout.totalAmounts(state),state.enthalpy(),state.mass(),state.pressure(),start,checkpoint);
    }
    private FluidThermodynamics.State junctionClosure(double[] n,double enthalpy,double p,FluidThermodynamics.State start,Runnable checkpoint) {
        return junctionClosure(n,enthalpy,molarMass(n),p,start,checkpoint);
    }
    private FluidThermodynamics.State junctionClosure(double[] n,double enthalpy,double mass,double p,FluidThermodynamics.State start,Runnable checkpoint) {
        try{
            var layout=new PhaseLayout(model,start,null,null,PhaseLayout.support(model,start,null,null,true));
            double[] w=new double[n.length];
            for(int i=0;i<n.length;i++)w[i]=n[i]*(i==n.length-1?model.waterMolecularWeight:model.hydrocarbon.molecularWeight(i))/mass;
            var solved=SparseNewton.solve(layout.junctionClosure(w,enthalpy/mass,p),layout.encode(start),new SparseNewton.Settings(SparseNewton.ITERATIONS,1e-10,1e-6,24),checkpoint);
            var closed=layout.decode(solved.variables(),0);
            if(Boolean.getBoolean("t285.trace"))System.out.println("T285 CLOSE ok T="+closed.temperature()+" P="+closed.pressure()+" vapMol="+java.util.Arrays.stream(closed.vaporView()).sum()+" liqMol="+java.util.Arrays.stream(closed.liquidView()).sum()+" startVap="+java.util.Arrays.stream(start.vaporView()).sum()+" it="+solved.iterations());
            return closed;
        }
        catch(SparseNewton.Nonconvergence|IllegalArgumentException failed){if(Boolean.getBoolean("t285.trace"))System.out.println("T285 CLOSE fail "+failed.getMessage()+" startVap="+java.util.Arrays.stream(start.vaporView()).sum()+" T="+start.temperature()+" P="+p);return null;}
    }
J
  $t=~s/(\n)([ \t]*\/\*\*\r?\n\s+\* Owner decision D26: a vessel's own split at a pass)/$1$add$2/ or die "PSS anchor"; $t });
print "p2 applied\n";
