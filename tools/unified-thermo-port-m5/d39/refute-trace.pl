# D39 B trace hook on a lane export (never the worktree): prints a "D39REFUTE" line where PassiveStepSolver refutes a
# proposal (dt, pass, node, refuted phase bits, the proposed phase's share of the node's moles at the seed and at the
# failed iterate, the iterate's T and P, the failure message).
# Usage (worktree root): F=/tmp/d39-<name>/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java
#   perl tools/unified-thermo-port-m5/d39/refute-trace.pl < $F > /tmp/PSS.java && cp /tmp/PSS.java $F
# then lane.sh compile <name> ... and lane.sh run <name> <selector>. Prints "replaced 1" on stderr.
undef $/; $_=<STDIN>;
my $hook='{var sd=proposal.seed();var it=iterate.get(proposal.node());double sv=java.util.Arrays.stream(sd.vaporView()).sum(),st=sv+java.util.Arrays.stream(sd.liquidView()).sum()+sd.waterLiquid()+sd.waterVapor(),iv=java.util.Arrays.stream(it.vaporView()).sum(),itot=iv+java.util.Arrays.stream(it.liquidView()).sum()+it.waterLiquid()+it.waterVapor();System.out.println("D39REFUTE dt="+dt+" pass="+pass+" node="+proposal.node()+" phases="+refuted+" seedVapShare="+(sv/st)+" iterVapShare="+(iv/itot)+" T="+it.temperature()+" P="+it.pressure()+" msg="+failure.getMessage());}';
my $n = s/(if\(refuted!=0\)\{)(foldedPhases\[proposal\.node\(\)\]\|=refuted;)/${1}${hook}${2}/;
print STDERR "replaced $n\n"; print;
