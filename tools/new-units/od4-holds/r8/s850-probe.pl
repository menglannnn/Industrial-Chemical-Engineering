#!/usr/bin/perl -i
# r8 probe (850): the interval-start rate solve (null or not, how many still pipes, largest flow) and every refused mobility
# check (pipe, flow, still flag, reason, velocity against its minimum). Export-only, SolidEventIntegrator.java.
while(<>){
  if(/^(\s*)var candidate=rates\.portRate\(graph,checkpoint\);\s*$/){
    print; my $i=$1;
    print $i.'{double s8l=0;int s8n=0;if(candidate!=null){double[] s8f=candidate.massFlows();for(double s8q:s8f)s8l=Math.max(s8l,Math.abs(s8q));boolean[] s8s=still(s8f);for(boolean s8b:s8s)if(s8b)s8n++;}System.out.println("S850 RATE null="+(candidate==null)+" still="+s8n+" largest="+s8l);}'."\n";
    next;
  }
  if(/^(\s*)double candidate=closureRatio\(check\);\s*$/ && !$done){
    my $i=$1; $done=1;
    print $i.'System.out.println("S850 REFUSE pipe="+pipe.id()+" i="+i+" flow="+flow+" still="+(still==null?"null":String.valueOf(still[i]))+" reason="+check.reason()+" v="+check.velocity()+" vmin="+check.minimumVelocity());'."\n";
  }
  print;
}
