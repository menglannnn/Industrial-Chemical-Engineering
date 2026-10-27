#!/usr/bin/perl -i
# r6 probe (654): where the balance's pressure solve ends unconverged, its budget, residual and hull, and per unknown
# junction its pressure and net inflow, with each open connection's flow. The loss inversion warm-starts from lastQ, so
# the probe restores it: without that the probe alone turned 654 clean (round-off in the warm start). Export-only.
while(<>){
  if(/^(\s*)finalResidual=maximum\(residual\);converged=finalResidual<=tolerance;\s*$/){
    my $i=$1;
    print;
    print $i.'if(R2T&&!converged){double[][] r2lq=new double[lastQ.length][];for(int r2k=0;r2k<lastQ.length;r2k++)r2lq[r2k]=lastQ[r2k].clone();var r2b=new StringBuilder("R2 BALANCE unconverged iterations="+iterations+" sweeps="+sweeps+" residual="+finalResidual+" tolerance="+tolerance+" count="+count+" low="+low+" high="+high);for(int r2n=0;r2n<nodes;r2n++){if(unknown[r2n]<0)continue;double r2q=0;var r2s=new StringBuilder();for(int r2e:nodeEdges[r2n]){if(closed[r2e])continue;var r2p=graph.pipes().get(r2e);double r2f=flow(r2e,endAt(r2e,0,p),endAt(r2e,1,p));r2q+=r2p.second()==r2n?r2f:-r2f;r2s.append(" e"+r2e+"="+r2f);}r2b.append(" | n"+r2n+"#"+graph.reservoirs().get(r2n).id()+" p="+p[r2n]+" net="+r2q+r2s);}for(int r2k=0;r2k<lastQ.length;r2k++)lastQ[r2k]=r2lq[r2k];System.out.println(r2b);}'."\n";
    next;
  }
  print;
}
