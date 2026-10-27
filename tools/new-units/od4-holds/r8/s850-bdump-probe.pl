#!/usr/bin/perl -i
# r8 probe (850): the first unconverged rate-solve balances dumped whole: every node (kind, pressure, elevation, phases,
# net inflow of the law flows), every connection (ends, control, mode, closure, diameter, law flow), and the balance's own
# iteration counts and residual. Export-only, PassiveStepSolver.java (after s850-bstart-probe.pl).
while(<>){
  if(/^(\s*)return new Start\(current,law,closed,List\.copyOf\(modes\),converged\);\s*$/){
    my $i=$1;
    print $i.'if(rateOnly&&!converged&&S8DUMP.getAndIncrement()<4){double[] s8net=new double[current.reservoirs().size()];for(int s8e=0;s8e<edges;s8e++){var s8p=current.pipes().get(s8e);s8net[s8p.first()]-=law[s8e];s8net[s8p.second()]+=law[s8e];}'."\n";
    print $i.'  System.out.println("S850 BDUMP iterations="+net.iterations+" sweeps="+net.sweeps+" residual="+net.finalResidual+" scale="+net.scale+" count="+net.count+" anchored="+net.anchored);'."\n";
    print $i.'  for(int s8n=0;s8n<current.reservoirs().size();s8n++){var s8r=current.reservoirs().get(s8n);var s8st=s8r.state();System.out.println("S850 BNODE "+s8n+" kind="+s8r.kind()+" junction="+s8r.junction()+" unknown="+net.unknown[s8n]+" anchor="+net.anchor[s8n]+" p="+s8st.pressure()+" pIn="+graph.reservoirs().get(s8n).state().pressure()+" z="+s8r.elevation()+" T="+s8st.temperature()+" phases="+phaseCode(s8st)+" net="+s8net[s8n]);}'."\n";
    print $i.'  for(int s8e=0;s8e<edges;s8e++){var s8p=current.pipes().get(s8e);System.out.println("S850 BEDGE "+s8e+" id="+s8p.id()+" "+s8p.first()+"->"+s8p.second()+" control="+s8p.control().getClass().getSimpleName()+" mode="+modes.get(s8e)+" bClosed="+boundaryClosed[s8e]+" netClosed="+net.closed[s8e]+" blocked="+s8p.blockedDirections()+" pa="+endPressure(current,s8p,s8p.first())+" pb="+endPressure(current,s8p,s8p.second())+" law="+law[s8e]+" lastQ="+java.util.Arrays.toString(net.lastQ[s8e])+" allowed="+java.util.Arrays.toString(net.allowed[s8e])+" cap="+java.util.Arrays.toString(net.cap[s8e]));}}'."\n";
  }
  if(/^(\s*)private Start balanceStart\(PassiveNetwork graph,List<FlowControl\.Mode> modes,boolean\[\] boundaryClosed,boolean\[\] keep,Runnable checkpoint\) \{/){
    print $1.'static final java.util.concurrent.atomic.AtomicInteger S8DUMP=new java.util.concurrent.atomic.AtomicInteger();'."\n";
  }
  print;
}
