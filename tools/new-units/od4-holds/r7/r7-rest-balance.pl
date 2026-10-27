#!/usr/bin/perl -i
# r7 probe (850): at each balance start, whether it converged, the largest junction pressure shift from the solve's input
# states, and the smallest nonzero |law flow| of its open connections. Export-only.
while(<>){
  if(/^(\s*)return new Start\(current,law,closed,List\.copyOf\(modes\),converged\);\s*$/){
    my $i=$1;
    print $i.'if(R2T){double r2s=0,r2m=Double.POSITIVE_INFINITY,r2x=0;for(int r2i=0;r2i<current.reservoirs().size();r2i++)if(current.reservoirs().get(r2i).junction())r2s=Math.max(r2s,Math.abs(current.reservoirs().get(r2i).state().pressure()-graph.reservoirs().get(r2i).state().pressure()));for(double r2l:law){if(r2l!=0)r2m=Math.min(r2m,Math.abs(r2l));r2x=Math.max(r2x,Math.abs(r2l));}System.out.println("R2 BSTART converged="+converged+" shift="+r2s+" minLaw="+r2m+" maxLaw="+r2x+" buffers="+(buffers!=null));}'."\n";
  }
  print;
}
