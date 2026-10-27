#!/usr/bin/perl
# prototype F2: the Jacobian sweep states each connection's upwind on the branch of its base point (semismooth, as A423).
use strict; my $root=shift or die;
my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
local @ARGV=($f); local $^I=""; my $n=0; my $line=0;
while(<>){ s/\x0d\x0a$/\x0a/; $line++;
  if(/^        PhaseLayout.Wall\[\] sweepWall,builtWall;$/){ $_.="        boolean[] sweepForward,builtForward;\n        boolean forward(int edge,double flow){return sweepForward!=null?sweepForward[edge]:flow>=0;}\n"; $n++; }
  if(/^            builtWall=sweepWall=walls;$/){ $_.="            var signs=new boolean[graph.pipes().size()];for(int edge=0;edge<signs.length;edge++)signs[edge]=base[edgeOffset+edge]>=0;builtForward=sweepForward=signs;\n"; $n++; }
  if(s/^        \@Override public void linearised\(\)\{sweepWall=null;\}$/        \@Override public void linearised(){sweepWall=null;sweepForward=null;}/){ $n++; }
  if(/^            if\(built==null\|\|built.length!=now.length\)return true;$/){ $_.="            if(builtForward!=null)for(int edge=0;edge<builtForward.length;edge++)if(!boundaryClosed[edge]&&(now[edgeOffset+edge]>=0)!=builtForward[edge])return false;\n"; $n++; }
  if(s/^            var pipe=graph.pipes\(\).get\(edge\);int end=y>=0\?0:1;$/            var pipe=graph.pipes().get(edge);int end=forward(edge,y)?0:1;/){ $n++; }
  if(s/boolean first=node==a;int donor=flow>=0\?a:b;var upstream=st\[donor\];var transport=drawn\(edge,flow>=0\?0:1,tr\[donor\],flow\);/boolean first=node==a;boolean fw=forward(edge,flow);double through=fw?flow:-flow;int donor=fw?a:b;var upstream=st[donor];var transport=drawn(edge,fw?0:1,tr[donor],flow);/){ $n++; }
  if(s/^                boolean receiver=node==\(flow>=0\?b:a\);$/                boolean receiver=node==(fw?b:a);/){ $n++; }
  if($line>3563 && $line<3638){ my $k=s/Math\.abs\(flow\)/through/g; $n+=$k; }
  if(s/int donor=flow>=0\?a:b;var transport=drawn\(edge,flow>=0\?0:1,tr\[donor\],flow\);double rho=transport.density;/boolean fw=forward(edge,flow);int donor=fw?a:b;var transport=drawn(edge,fw?0:1,tr[donor],flow);double rho=transport.density;/){ $n++; }
  if(s/double carrier=drawnCarrierViscosity\(edge,flow>=0\?0:1,state,tr\[donor\],flow\);/double carrier=drawnCarrierViscosity(edge,fw?0:1,state,tr[donor],flow);/){ $n++; }
  print; }
print STDERR "branch edits: $n\n";
