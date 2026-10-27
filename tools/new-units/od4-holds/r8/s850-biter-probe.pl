#!/usr/bin/perl -i
# r7 probe (balance convergence): per Newton iteration of the balance's solveOnce, the merit, whether the step descended,
# its fraction and size; why a sweep ends the solve; and, for an unconverged balance with one unknown, its net inflow
# scanned around the end point (lastQ restored). Export-only.
while(<>){
  if(/^(\s*)if\(!accepted\)\{if\(\+\+sweeps>12\)break;double\[\] was=p\.clone\(\),wasResidual=residual\.clone\(\);double wasMerit=merit;sweep\(p\);merit=residual\(p,residual,null\);\s*$/){
    my $i=$1;
    print $i.'if(rateOnly)System.out.println("S850 BITER it="+iteration+" count="+count+" merit="+merit+" max="+maximum(residual)+" accepted="+accepted+" took="+took+" largest="+largest+" sweeps="+sweeps);'."\n";
    print;
    next;
  }
  if(/^(\s*)if\(!\(merit<wasMerit\)\)\{System\.arraycopy\(was,0,p,0,nodes\);System\.arraycopy\(wasResidual,0,residual,0,count\);merit=wasMerit;break;\}\s*$/){
    my $i=$1;
    print $i.'if(rateOnly)System.out.println("S850 BSWEEP merit="+merit+" was="+wasMerit+(merit<wasMerit?" kept":" rejected: end"));'."\n";
    print;
    next;
  }
  if(/^(\s*)if\(moved<1e-12\)break;\}\s*$/){
    my $i=$1;
    print $i.'if(rateOnly&&moved<1e-12)System.out.println("S850 BSWEEP moved<1e-12: end");'."\n";
    print;
    next;
  }
  if(/^(\s*)finalResidual=maximum\(residual\);converged=finalResidual<=tolerance;\s*$/){
    my $i=$1;
    print;
    print $i.'if(rateOnly&&!converged&&count==1){double[][] r2lq=new double[lastQ.length][];for(int r2k=0;r2k<lastQ.length;r2k++)r2lq[r2k]=lastQ[r2k].clone();int r2u=0;while(unknown[r2u]!=0)r2u++;double r2p=p[r2u];var r2b=new StringBuilder("S850 BSCAN n="+r2u+" p="+r2p+" tol="+tolerance);for(int r2s=-12;r2s<=12;r2s++){double r2d=r2s==0?0:Math.signum(r2s)*r2p*Math.pow(10,-10+Math.abs(r2s)*0.5);p[r2u]=r2p+r2d;double r2q=0;for(int r2e:nodeEdges[r2u]){if(closed[r2e])continue;var r2pp=graph.pipes().get(r2e);double r2f=flow(r2e,endAt(r2e,0,p),endAt(r2e,1,p));r2q+=r2pp.second()==r2u?r2f:-r2f;}r2b.append(" ").append(String.format("%.3g:%.4g",r2d,r2q));}p[r2u]=r2p;for(int r2k=0;r2k<lastQ.length;r2k++)lastQ[r2k]=r2lq[r2k];System.out.println(r2b);}'."\n";
    next;
  }
  print;
}
