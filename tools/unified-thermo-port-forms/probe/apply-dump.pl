#!/usr/bin/perl
# Applies the forms dump probe to an exported tree (argument: the tree's root): PassiveStepSolver.vesselSplit prints, for
# the first three own splits of a vessel holding solids whose start (the TP-based seed) holds a vapour the point lacks and whose split ends under 1e-7 of vapour (or none),
# the point as Java literals (T, P, liquid, vapour, water, hydrocarbon partial pressure, solids, capacity, energy), the
# start's vapour share and the split's (null where the closure left nothing). Gate (a)'s rig is read from it. One literal
# substring replacement that must match exactly once (CRLF kept). Needs probe/FormsProbe.java.
use strict; use warnings;
my $root=shift or die "usage: apply-dump.pl <exported tree>";
my $P='com.wormzjl.createcheme.science.fluid.FormsProbe';
my $file="$root/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java";
my $from='        return InventoryEquilibrium.split(model,start,new PassiveNetwork.Inventory(capacity,PhaseLayout.totalAmounts(state),state.internalEnergy(),state.solids()),
                PhaseLayout.bagVolume(state,capacity),checkpoint);';
my $to='        var fpOut=InventoryEquilibrium.split(model,start,new PassiveNetwork.Inventory(capacity,PhaseLayout.totalAmounts(state),state.internalEnergy(),state.solids()),
                PhaseLayout.bagVolume(state,capacity),checkpoint);
        if(!state.solids().empty()&&state.vaporProperties()==null&&start.vaporProperties()!=null&&(fpOut==null||'.$P.'.vapourShare(fpOut)<1e-7)){
            var fpB=new StringBuilder();for(var fpQ:state.solids().populations())fpB.append(fpQ.material().id()).append("@").append(fpQ.size().metres()).append("=").append(fpQ.massKg()).append(";");
            '.$P.'.first("DUMP","node="+node.id()+" T="+state.temperature()+" P="+state.pressure()+" l="+java.util.Arrays.toString(state.liquidView())+" v="+java.util.Arrays.toString(state.vaporView())
                +" wl="+state.waterLiquid()+" wv="+state.waterVapor()+" pc="+state.hydrocarbonPartialPressure()+" solids="+fpB+" capacity="+capacity+" U="+state.internalEnergy()
                +" liquidVolume="+state.liquidVolume()+" solidsVolume="+state.solids().volume()+" bag="+PhaseLayout.bagVolume(state,capacity)
                +" startT="+start.temperature()+" startP="+start.pressure()+" startVap="+'.$P.'.vapourShare(start)+" startLiquidStandIn="+model.condensedShareOnly(start)
                +" splitVap="+(fpOut==null?"null":String.valueOf('.$P.'.vapourShare(fpOut))));}
        return fpOut;';
open my $in,'<:raw',$file or die "$file: $!"; local $/; my $text=<$in>; close $in;
my $crlf=$text=~/\r\n/;$text=~s/\r\n/\n/g;
my $count=()=$text=~/\Q$from\E/g;
die "$file: expected one match, found $count\n" unless $count==1;
$text=~s/\Q$from\E/$to/;
$text=~s/\n/\r\n/g if $crlf;
open my $out,'>:raw',$file or die "$file: $!"; print $out $text; close $out;
print "apply-dump: 1 edit applied under $root\n";
