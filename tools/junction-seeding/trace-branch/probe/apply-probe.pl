#!/usr/bin/perl
# t45: inserts the T45Probe calls into an export's PassiveStepSolver.java / PassiveIntervalSolver.java (export only).
# Usage: perl apply-probe.pl <export-root>   (works on 33455d4c and 32bf3fa4 exports; prints which hooks it placed)
use strict; my $root=shift; my $N="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network";
sub edit { my($f,$code)=@_; local $/; open(my $h,'<',$f) or die "$f"; my $s=<$h>; close $h; $s=~s/\r\n/\n/g; my $n=$code->(\$s); open($h,'>',$f) or die; print $h $s; close $h; print "$f: $n hooks\n"; }
edit("$N/PassiveStepSolver.java", sub { my $s=shift; my $n=0;
  # fallback: balance rounds
  $n+=($$s=~s/(            p=net\.solve\(checkpoint\);converged=net\.converged;\n)/$1            if(T45Probe.ON){var sb=new StringBuilder("T45BAL round="+round+" conv="+converged+" it="+net.iterations+" sweeps="+net.sweeps+" hull=["+net.low\/1000+","+net.high\/1000+"] p=");for(int i=0;i<current.reservoirs().size();i++)if(net.unknown[i]>=0)sb.append(String.format(java.util.Locale.ROOT," %d:%.2f",current.reservoirs().get(i).id(),p[i]\/1000));System.out.println(sb);double[] bq=new double[current.pipes().size()];for(int e=0;e<bq.length;e++)bq[e]=net.flow(e,net.endAt(e,0,p),net.endAt(e,1,p));T45Probe.flows("T45BAL-q round="+round,current,bq);var cb=new StringBuilder("T45BAL-caps round="+round);for(int e=0;e<bq.length;e++){var pp=current.pipes().get(e);cb.append(String.format(java.util.Locale.ROOT," e%d rho=%.4g\/%.4g cap=%.4g\/%.4g tab=%s\/%s",e,net.rho[e][0],net.rho[e][1],net.cap[e][0],net.cap[e][1],net.tableP[pp.first()]!=null,net.tableP[pp.second()]!=null));}System.out.println(cb);T45Probe.nodes("T45BAL-in round="+round,current,model);}\n/);
  $n+=($$s=~s/(        if\(!converged\)SolverDiagnostics\.count\(SolverDiagnostics\.balanceUnconverged\);\n)/        T45Probe.nodes("T45BAL-out",current,model);T45Probe.flows("T45BAL-law",current,law);\n$1/);
  # baseline: fixed-point starts at the mint
  $n+=($$s=~s/(        var states=supplierSeeds\(graph,none\);\n)/$1        T45Probe.states("T45FP-supplier",graph,states,model);\n/);
  $n+=($$s=~s/(                var rate=new PassiveStepSolver\(model\)\.portRate\(held,none\);\n)/$1                if(rate!=null){T45Probe.flows("T45FP-rate-q",held,rate.massFlows());T45Probe.states("T45FP-rate",held,rate.states(),model);}\n/);
  $n+=($$s=~s/(                states=next;\n                if\(!moved\)return states;\n)/                T45Probe.states("T45FP-out",graph,next,model);\n$1/);
  $n; });
edit("$N/PassiveIntervalSolver.java", sub { my $s=shift; my $n=0;
  $n+=($$s=~s/(        var rate=implicit\.portRate\(graph,checkpoint\);\n)/        T45Probe.nodes("T45RATE-in",graph,model);\n$1        if(T45Probe.ON){if(rate==null)System.out.println("T45RATE failed");else{System.out.println("T45RATE numerical="+rate.numerical()+" modes="+rate.modes());T45Probe.flows("T45RATE-q",graph,rate.massFlows());T45Probe.states("T45RATE-root",graph,rate.states(),model);}}\n/);
  $n; });
