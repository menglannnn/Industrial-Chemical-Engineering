#!/usr/bin/perl -i
# r6 probe (654): at each solve with dt<1e-3, after the start closures: which connections are closed for the solve
# (boundaryClosed) and their modes, and the balance start's convergence. Export-only.
my $seen=0;
while(<>){
  if(!$seen&&/^(\s*)closeIllegalStarts\(graph,modes,boundaryClosed,keep\);\s*$/){
    my $i=$1;$seen=1;
    print $i.'if(R2T&&dt<1e-3){var r2b=new StringBuilder("R2 CLOSED dt="+dt+" rate="+rateOnly+" balance="+(balanceCache!=null&&balanceFor==inputGraph?balanceCache.converged():"none")+" pre=");for(int r2e=0;r2e<boundaryClosed.length;r2e++)if(boundaryClosed[r2e])r2b.append(r2e).append(",");System.out.println(r2b);}'."\n";
    print;
    print $i.'if(R2T&&dt<1e-3){var r2b=new StringBuilder("R2 CLOSED post=");for(int r2e=0;r2e<boundaryClosed.length;r2e++)if(boundaryClosed[r2e])r2b.append(r2e).append(",");System.out.println(r2b);}'."\n";
    next;
  }
  print;
}
