// Applies the M3 cold-arrival trace prints to an exported tree (never to a worktree): node apply-prints.js <tree root>.
// Every edit is an exact string that must occur once in its file at both a0f20364 and 76e87740; the script stops at the
// first anchor that does not. Each inserted statement carries /*M3ARR*/ and prints only while M3ArrivalTrace.on.
const fs=require('fs'),path=require('path');
const root=process.argv[2];if(!root){console.error('usage: node apply-prints.js <tree root>');process.exit(2);}
const base=path.join(root,'src/main/java/com/wormzjl/createcheme/science/fluid');
const T='com.wormzjl.createcheme.science.fluid.M3ArrivalTrace';
const edits=[
 // ---- the interval: its start graph, each attempt, each acceptance and rejection
 ['network/PassiveIntervalSolver.java',"        // h is the controller's estimate; each attempt uses it truncated to the rest of the interval.",
  `        ${T}.p("INTERVAL duration=%.6g startingStep=%.6g acceptance=%s graph:%s",duration,startingStep,acceptance,${T}.on?${T}.graph(accepted):"");/*M3ARR*/\n$&`],
 ['network/PassiveIntervalSolver.java',"var result=implicit.solve(start,step,checkpoint,acceptance);",
  `${T}.p("ATTEMPT %d elapsed=%.6g step=%.6g coldSeed=%b",attempt,elapsed,step,start!=accepted);/*M3ARR*/$&`],
 ['network/PassiveIntervalSolver.java',"accepted=replace(accepted,result);last=result;elapsed+=step;acceptedCount++;consecutiveRejects=0;previousStateChange=0;",
  `$&${T}.p("ACCEPTED attempt %d elapsed=%.6g change=%.4g flows=%s graph:%s",attempt,elapsed,change,${T}.v(result.massFlows()),${T}.on?${T}.graph(accepted):"");/*M3ARR*/`],
 ['network/PassiveIntervalSolver.java',"lastRejection=String.valueOf(rejected.getMessage());",
  `$&${T}.p("REJECTED attempt %d step=%.6g %s: %s",attempt,step,rejected.getClass().getSimpleName(),${T}.shorten(lastRejection));/*M3ARR*/`],
 // ---- the step solver's passes: seeds, supports, the Newton's outcome, the branch each pass takes
 ['network/PassiveStepSolver.java',"var supports=supports(graph,seeds,reachable,promoted);",
  `$&if(${T}.on){var m3=new StringBuilder();for(int m3i=0;m3i<seeds.size();m3i++){var m3n=graph.reservoirs().get(m3i);if(m3n.fixed())continue;m3.append("\\n    node ").append(m3i).append(" id=").append(m3n.id()).append(' ').append(m3n.kind()).append(" code=").append(phases[m3i]).append(" support=").append(supports[m3i]==null?"null":java.util.Arrays.toString(supports[m3i])).append(" folded=").append(foldedPhases[m3i]).append("\\n      seed ").append(${T}.state(seeds.get(m3i)));}${T}.p("PASS %d dt=%.6g modes=%s caps=%s%s",pass,dt,modes,java.util.Arrays.toString(caps),m3);}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"System.arraycopy(equations.capDirections,0,caps,0,caps.length);",
  `$&{final var m3eq=equations;final var m3g=graph;${T}.describer=m3x->{var m3b=new StringBuilder();for(int m3i=0;m3i<m3eq.layout.length;m3i++){if(m3eq.layout[m3i]==null)continue;m3b.append(String.format(java.util.Locale.ROOT,"id%d:T=%.5f,P=%.1f ",m3g.reservoirs().get(m3i).id(),m3eq.layout[m3i].temperature(m3x,m3eq.offsets[m3i]),1e5*Math.exp(m3x[m3eq.offsets[m3i]+m3eq.layout[m3i].pressureVariable()])));}m3b.append("flows=[");for(int m3e=0;m3e<m3g.pipes().size();m3e++)m3b.append(String.format(java.util.Locale.ROOT,"%.5g ",m3x[m3eq.edgeOffset+m3e]));return m3b.append(']').toString();};}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(failure.lastVariables()==null)throw failure;",
  `${T}.p("NEWTON FAILED pass %d: %s; hold=%s; last iterate %s",pass,${T}.shorten(failure.getMessage()),failure.domainViolation()==null?"none":${T}.shorten(failure.domainViolation().getMessage()),${T}.describe(failure.lastVariables()));/*M3ARR*/$&`],
 ['network/PassiveStepSolver.java',"if(cavitates(equations,failure.lastVariables(),modes,boundaryClosed,caps,tolerance))continue;",
  `if(cavitates(equations,failure.lastVariables(),modes,boundaryClosed,caps,tolerance)){${T}.p("BRANCH cavitates");continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(equations.correctCaps(failure.lastVariables(),caps,tolerance))continue;",
  `if(equations.correctCaps(failure.lastVariables(),caps,tolerance)){${T}.p("BRANCH correctCaps caps=%s",java.util.Arrays.toString(caps));continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(drained!=null){emptying=drained;continue;}",
  `if(drained!=null){${T}.p("BRANCH runsDry");emptying=drained;continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(changedSeeds==null)changedSeeds=equations.drainedSeeds(checkpoint);",
  `if(${T}.on)try{var m3=new StringBuilder();var m3last=equations.states(failure.lastVariables());for(int m3i=0;m3i<m3last.size();m3i++){if(graph.reservoirs().get(m3i).fixed())continue;m3.append("\\n    node ").append(m3i).append(" last iterate ").append(${T}.state(m3last.get(m3i)));if(changedSeeds!=null)m3.append("\\n    node ").append(m3i).append(" new seed ").append(${T}.state(changedSeeds.get(m3i)));}${T}.p("FAILED-PASS %d phase correction of the last iterate: %s%s",pass,changedSeeds==null?"none":"changed seeds",m3);}catch(RuntimeException m3f){${T}.p("FAILED-PASS print failed: %s",m3f.getMessage());}/*M3ARR*/$&if(${T}.on&&changedSeeds!=null)${T}.p("FAILED-PASS %d continues with new seeds",pass);/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"double[] x=numerical.variables();var states=equations.states(x);",
  `$&if(${T}.on){var m3=new StringBuilder();for(int m3i=0;m3i<states.size();m3i++){if(graph.reservoirs().get(m3i).fixed())continue;m3.append("\\n    node ").append(m3i).append(' ').append(${T}.state(states.get(m3i)));}${T}.p("NEWTON CONVERGED pass %d iterations=%d residual=%.3g %s%s",pass,numerical.iterations(),numerical.residualNorm(),${T}.describe(x),m3);}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(reactivate(equations,states,promoted)>0){seeds=states;continue;}",
  `if(reactivate(equations,states,promoted)>0){${T}.p("BRANCH reactivate");seeds=states;continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(emptying!=null){var kept=keptEmptying(states,emptying,emptied);if(kept!=null){emptying=kept;continue;}}",
  `if(emptying!=null){var kept=keptEmptying(states,emptying,emptied);if(kept!=null){${T}.p("BRANCH keptEmptying");emptying=kept;continue;}}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(changed&&capChange>=0){previousCaps=caps.clone();continue;}",
  `if(changed&&capChange>=0){${T}.p("BRANCH cap change edge %d -> %d",capChange,capNext);previousCaps=caps.clone();continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(changed){seeds=states;previousPipes=pipeIdentities;",
  `if(changed){${T}.p("BRANCH device/boundary change modes=%s closed=%s",modes,java.util.Arrays.toString(boundaryClosed));seeds=states;previousPipes=pipeIdentities;`],
 ['network/PassiveStepSolver.java',"if(equations.segmentsMoved(flows)){seeds=states;",
  `if(equations.segmentsMoved(flows)){${T}.p("BRANCH segmentsMoved");seeds=states;`],
 ['network/PassiveStepSolver.java',"if(filled(equations,projection)){seeds=projection.states();continue;}",
  `if(filled(equations,projection)){${T}.p("BRANCH filled");seeds=projection.states();continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(phaseAppeared(graph,states,projection.states())){seeds=projection.states();continue;}",
  `if(phaseAppeared(graph,states,projection.states())){${T}.p("BRANCH phaseAppeared");seeds=projection.states();continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"if(recordFolds(graph,states,projection.states(),foldedPhases)){SolverDiagnostics.count(SolverDiagnostics.traceFoldPasses);seeds=projection.states();continue;}",
  `if(recordFolds(graph,states,projection.states(),foldedPhases)){${T}.p("BRANCH recordFolds folded=%s",java.util.Arrays.toString(foldedPhases));SolverDiagnostics.count(SolverDiagnostics.traceFoldPasses);seeds=projection.states();continue;}/*M3ARR*/`],
 ['network/PassiveStepSolver.java',"var changedSeeds=phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases);if(changedSeeds!=null){seeds=changedSeeds;continue;}",
  `var changedSeeds=phaseCorrection(graph,projection.states(),checkpoint,equations.cachedPrepared,foldedPhases);if(${T}.on){var m3=new StringBuilder();for(int m3i=0;m3i<projection.states().size();m3i++){if(graph.reservoirs().get(m3i).fixed())continue;m3.append("\\n    node ").append(m3i).append(" reconstructed ").append(${T}.state(projection.states().get(m3i)));if(changedSeeds!=null)m3.append("\\n    node ").append(m3i).append(" new seed ").append(${T}.state(changedSeeds.get(m3i)));}${T}.p("CONVERGED-PASS %d phase correction %s folded=%s%s",pass,changedSeeds==null?"none":"changed seeds",java.util.Arrays.toString(foldedPhases),m3);}/*M3ARR*/if(changedSeeds!=null){seeds=changedSeeds;continue;}`],
 ['network/PassiveStepSolver.java',`if(maximumResidual>gate){SolverDiagnostics.count(SolverDiagnostics.equationGateRejections);throw new SparseNewton.Nonconvergence("Conservative reconstruction fails equation gate: "+maximumResidual);}`,
  `${T}.p("GATE maximumResidual=%.3g gate=%.1g",maximumResidual,gate);/*M3ARR*/$&`],
 // ---- the outer phase check and every TP answer adopted in the window
 ['thermo/FluidThermodynamics.java',"var equilibrium=folded(equilibriumTP(kept.temperature(),kept.pressure(),totals,checkpoint,prepared).withSolidState(kept.solids(),kept.solidMoments()),junction);",
  `$&${T}.p("PHASECHECK junction=%b excluded=%d crystalsCompete=%b\\n      kept %s\\n      TP   %s",junction,excluded,crystalsCompete(kept.temperature(),totals),${T}.state(kept),${T}.state(equilibrium));/*M3ARR*/`],
 ['thermo/FluidThermodynamics.java',"return (phaseBits(seed)&~phaseBits(kept)&~excluded)==0?kept:seed;",
  `${T}.p("PHASECHECK own=%s -> %s\\n      seed %s",own==null?"none (TP seed)":"own split",(phaseBits(seed)&~phaseBits(kept)&~excluded)==0?"kept":"seed",${T}.state(seed));/*M3ARR*/$&`],
 ['thermo/FluidThermodynamics.java',"        switch(result.classification()) {",
  `        ${T}.p("ADOPT T=%.5f P=%.1f class=%s overall=%s",t,p,result.classification(),${T}.v(overall));/*M3ARR*/\n$&`],
 // ---- the Newton: each iteration's start, full step and line search
 ['solver/SparseNewton.java',"double alpha=Math.min(1,equations.maximumStep(x,direction));boolean accepted=false;",
  `$&StringBuilder m3bt=${T}.on?new StringBuilder():null;if(m3bt!=null){double[] m3full=x.clone();for(int m3i=0;m3i<n;m3i++)m3full[m3i]+=direction[m3i];${T}.p("NEWTON it=%d norm=%.6g alpha0=%.4g fresh=%b at %s | full step to %s",iteration,norm,alpha,fresh,${T}.describe(x),${T}.describe(m3full));}/*M3ARR*/`],
 ['solver/SparseNewton.java',"double[] next=evaluate(equations,candidate,n);double nextNorm=norm(next);",
  `$&if(m3bt!=null)m3bt.append(String.format(java.util.Locale.ROOT," %.3g:%.4g",alpha,nextNorm));/*M3ARR*/`],
 ['solver/SparseNewton.java',"}catch(IllegalArgumentException outsideDomain){\n                    // A smaller Newton step may stay in the valid domain.",
  `$&\n                    if(m3bt!=null){String m3m=String.valueOf(outsideDomain.getMessage());m3bt.append(String.format(java.util.Locale.ROOT," %.3g:X(%s @ %s)",alpha,m3m.substring(0,Math.min(60,m3m.length())),${T}.describe(candidate)));}/*M3ARR*/`],
 ['solver/SparseNewton.java',"            if(!accepted) {",
  `            if(m3bt!=null)${T}.p("  line search accepted=%b:%s",accepted,m3bt);/*M3ARR*/\n$&`],
];
// All edits are made in memory first and written only when every anchor was found once (no partial application). The
// exported trees use CRLF: an anchor's or an insertion's line break is written as the file's own.
const files=new Map();let applied=0;
for(const [file,find0,replace0] of edits) {
  const p=path.join(base,file);if(!files.has(p))files.set(p,fs.readFileSync(p,'utf8'));const text=files.get(p);
  const eol=text.includes('\r\n')?'\r\n':'\n';
  const find=find0.split('\n').join(eol),replace=replace0.split('\n').join(eol);
  const count=text.split(find).length-1;
  if(count!==1){console.error('anchor found '+count+' times in '+file+': '+find0.slice(0,90));process.exit(1);}
  files.set(p,text.replace(find,()=>replace.split('$&').join(find)));applied++;
}
for(const [p,text] of files)fs.writeFileSync(p,text);
console.log('apply-prints: '+applied+' edits in '+root);
