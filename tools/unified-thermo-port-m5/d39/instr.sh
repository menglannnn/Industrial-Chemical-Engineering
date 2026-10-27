#!/usr/bin/env bash
# D38 C trace hooks on a lane export (never the worktree): prints a "D38V fold" line where phaseCheck's fold removes a
# phase only the vanishing bits name (j= junction, bits, rec= identity of the A85 record: one value per solve on the
# converged path, a new one per call on a failed iterate's copy), a "D38V refuse" line where the D26 trajectory test
# refuses a proposed phase, and a "D38V cycle" line before PassiveStepSolver throws "Phase/device active-set cycle".
# Usage (worktree root): bash instr.sh <name>   (after lane.sh export <name> <tree with commit 67e5f3e7's phaseCheck>),
# then lane.sh compile <name> all and lane.sh run <name> <selector>.
set -eu
D=/tmp/d39-${1:?name}/src/main/java/com/wormzjl/createcheme/science/fluid
F=$D/thermo/FluidThermodynamics.java; P=$D/network/PassiveStepSolver.java
perl -0pi -e 's/(        var kept=folded\(state,junction,shrinking\);var checked=kept;\r?\n)/$1        if(from!=null){int vb=vanishing(from,state);if(vb!=0&&kept!=folded(state,junction,0))System.out.println("D38V fold j="+junction+" bits="+vb+" rec="+(record==null?0:System.identityHashCode(record))+" at="+at+" now="+java.util.Arrays.toString(phaseAmounts(state))+" from="+java.util.Arrays.toString(phaseAmounts(from)));}\n/ or die "no fold site"' "$F"
perl -0pi -e 's/(                if\(own!=null\)refused=proposed&\(~lacking\(kept,seed\)\|vanishing\(start,own\)[^\n]*\n)/$1                if(own!=null&&(vanishing(start,own)&proposed)!=0)System.out.println("D38V refuse j="+junction+" proposed="+proposed+" bits="+vanishing(start,own)+" own="+java.util.Arrays.toString(phaseAmounts(own))+" start="+java.util.Arrays.toString(phaseAmounts(start)));\n/ or die "no refuse site"' "$F"
perl -0pi -e 's/if\(!seen\.add\(new PassKey\(structure,equations\.segmentCodes\(\),foldedPhases\.clone\(\),capCodes\(caps\)\)\)\)throw new SparseNewton\.Nonconvergence\("Phase\/device active-set cycle"\);/if(!seen.add(new PassKey(structure,equations.segmentCodes(),foldedPhases.clone(),capCodes(caps)))){System.out.println("D38V cycle pass="+pass+" dt="+dt+" phases="+java.util.Arrays.toString(phases)+" rec="+java.util.Arrays.toString(foldedPhases));throw new SparseNewton.Nonconvergence("Phase\/device active-set cycle");}/ or die "no cycle site"' "$P"
grep -c 'D38V' "$F" "$P"
