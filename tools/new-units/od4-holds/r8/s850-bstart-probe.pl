#!/usr/bin/perl -i
# r8 probe (850): every balance start (rate or step solve), whether it converged, its largest junction pressure shift
# from the solve's input, its smallest and largest |law flow|, and its closures. Export-only, PassiveStepSolver.java.
while(<>){
  if(/^(\s*)return new Start\(current,law,closed,List\.copyOf\(modes\),converged\);\s*$/){
    my $i=$1;
    print $i.'{double s8s=0,s8m=Double.POSITIVE_INFINITY,s8x=0;int s8c=0;for(int s8i=0;s8i<current.reservoirs().size();s8i++)if(current.reservoirs().get(s8i).junction())s8s=Math.max(s8s,Math.abs(current.reservoirs().get(s8i).state().pressure()-graph.reservoirs().get(s8i).state().pressure()));for(double s8l:law){if(s8l!=0)s8m=Math.min(s8m,Math.abs(s8l));s8x=Math.max(s8x,Math.abs(s8l));}for(boolean s8b:closed)if(s8b)s8c++;System.out.println("S850 BSTART rate="+rateOnly+" converged="+converged+" shift="+s8s+" minLaw="+s8m+" maxLaw="+s8x+" closed="+s8c+" modes="+modes);}'."\n";
  }
  print;
}
