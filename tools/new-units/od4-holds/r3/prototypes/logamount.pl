#!/usr/bin/perl
# prototype: a vessel's minor-phase single-phase component amounts as logarithms (PhaseLayout), on an export.
use strict; my $root=shift or die;
sub edit { my($f,$code)=@_; local @ARGV=("$root/$f"); local $^I=""; my $n=0; while(<>){ s/\x0d\x0a$/\x0a/; $n+=$code->(); print; } print STDERR "$f: $n edits\n"; }
edit("src/main/java/com/wormzjl/createcheme/science/fluid/solver/PhaseLayout.java", sub {
  my $c=0;
  if(/^    private final int solidIndex;$/){ $_.="    private final boolean[] logAmount;\n"; $c++; }
  if(/^        solidIndex=solidSupport\?offset:-1;if\(solidSupport\)offset\+=3;size=offset;$/){ $_.="        logAmount=new boolean[size];\n        if(liquidActive&&vaporActive){double nlq=sum(l),nvp=sum(v);for(int i:components)if(this.support[i].singlePhase()&&l[i]+v[i]>0&&(this.support[i].liquid()?nlq<nvp:nvp<nlq))logAmount[amountIndex[i]]=true;}\n"; $c++; }
  if(s/^        if\(!resolveTraces\)for\(int local=0;local<size;local\+\+\)if\(totalAmountVariable\(local\)\)/        if(!resolveTraces)for(int local=0;local<size;local++)if(linearAmount(local))/){ $c++; }
  if(s/for\(int c:components\)if\(conservedReference\[c\]==0\)\{int at=amountIndex\[c\];/for(int c:components)if(conservedReference[c]==0&&!logAmount[amountIndex[c]]){int at=amountIndex[c];/){ $c++; }
  if(/^    public double amountScale\(\)\{return amountScale;\}$/){ $_.="    public boolean linearAmount(int local){return totalAmountVariable(local)&&!logAmount[local];}\n    public double amount(int local,double value){return amountScale*(logAmount[local]?DeterministicMath.exp(value):value);}\n"; $c++; }
  if(s/^    public double differenceScale\(int local,double value\)\{return Math.max\(Math.abs\(value\),totalAmountVariable\(local\)\?/    public double differenceScale(int local,double value){return Math.max(Math.abs(value),linearAmount(local)?/){ $c++; }
  if(s/^        if\(!totalAmountVariable\(local\)\)return differenceScale\(local,value\);$/        if(!linearAmount(local))return differenceScale(local,value);/){ $c++; }
  if(s/^            else x\[amountIndex\[i\]\]=\(l\[i\]\+v\[i\]\)\/amountScale;$/            else x[amountIndex[i]]=logAmount[amountIndex[i]]?DeterministicMath.log(Math.max(Double.MIN_NORMAL,(l[i]+v[i])\/amountScale)):(l[i]+v[i])\/amountScale;/){ $c++; }
  if(s/^            else if\(liquidIndex\[i\]>=0\)l\[i\]=amountScale\*variables\[offset\+liquidIndex\[i\]\];$/            else if(liquidIndex[i]>=0)l[i]=amount(liquidIndex[i],variables[offset+liquidIndex[i]]);/){ $c++; }
  if(s/^            else v\[i\]=amountScale\*variables\[offset\+vaporIndex\[i\]\];$/            else v[i]=amount(vaporIndex[i],variables[offset+vaporIndex[i]]);/){ $c++; }
  if(s/for\(int c=0;c<x.length;c\+\+\)if\(totalAmountVariable\(c\)&&x\[c\]>0&&direction\[c\]<0\)/for(int c=0;c<x.length;c++)if(linearAmount(c)&&x[c]>0&&direction[c]<0)/){ $c++; }
  return $c; });
edit("src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java", sub { my $c=0;
  if(s/amountVariables\[column\]=layout\[node\]\.totalAmountVariable\(local\);/amountVariables[column]=layout[node].linearAmount(local);/){ $c++; }
  if(s/left\+=x\[offsets\[node\]\+local\];/left+=layout[node].amount(local,x[offsets[node]+local])\/layout[node].amountScale();/){ $c++; }
  return $c; });
edit("src/main/java/com/wormzjl/createcheme/science/fluid/network/VesselCrystals.java", sub { my $c=0;
  if(s/if\(layout\.totalAmountVariable\(c\)&&x\[c\]>0&&direction\[c\]<0\)/if(layout.linearAmount(c)&&x[c]>0&&direction[c]<0)/){ $c++; }
  return $c; });
