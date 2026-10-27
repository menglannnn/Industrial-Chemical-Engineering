from pathlib import Path
import sys

root = Path(sys.argv[1])
tool = Path(__file__).parent
prefix = 'src/main/java/com/wormzjl/icheme/science/fluid/'
files = {}
p = root / (prefix+'network/PassiveStepSolver.java')
s = p.read_text(encoding='utf-8')
needle = 'var equations=new Equations(graph,dt,modes,boundaryClosed,seeds,reachable,supports,pipeIdentities);'
assert s.count(needle) == 1
s = s.replace(needle,needle+'\n            AuditTrace.pass(graph,seeds,dt,pass,modes,equations.segmentCodes(),equations.columnCodes());')
s = s.replace('private List<FlowControl.Mode> previousModes=List.of();','private List<FlowControl.Mode> previousModes=List.of();\n    List<FlowControl.Mode> auditValveModes=List.of();')
s = s.replace('previousPipes=List.of();previousNodeIds=new long[0];previousModes=List.of();','previousPipes=List.of();previousNodeIds=new long[0];previousModes=List.of();auditValveModes=List.of();')
s = s.replace('previousPipes=ids;previousNodeIds=nodes;previousModes=committed.endpointModes();','previousPipes=ids;previousNodeIds=nodes;previousModes=committed.endpointModes();auditValveModes=committed.endpointModes();')
needle = 'modes.addAll(initialModes(graph));'
assert s.count(needle) == 1
s = s.replace(needle,needle+'''\n        if(Boolean.getBoolean("fluid.audit.carryValve")&&!rateOnly&&auditValveModes!=null&&auditValveModes.size()==modes.size())for(int edge=0;edge<modes.size();edge++)
            if(graph.pipes().get(edge).control() instanceof FlowControl.PressureValve)modes.set(edge,auditValveModes.get(edge));''')
needle = 'if(next!=mode&&deviceChange<0){deviceChange=i;deviceNext=next;}'
s = s.replace(needle,'if(next!=mode)AuditTrace.event("valve/actuator edge="+pipe.id()+" "+mode+" -> "+next+" q="+flows[i]+" head="+heads[i]+" upstream="+up.pressure());'+needle)
needle = 'if(equations.segmentsMoved(flows)){seeds=states;'
s = s.replace(needle,'if(equations.segmentsMoved(flows)){AuditTrace.event("phase-port segments moved");seeds=states;')
needle = 'if(changedSeeds!=null){changedSeeds=flashSeeds(graph,modes,boundaryClosed,projection.states(),changedSeeds,checkpoint);'
s = s.replace(needle,'if(changedSeeds!=null){AuditTrace.event("phase correction");changedSeeds=flashSeeds(graph,modes,boundaryClosed,projection.states(),changedSeeds,checkpoint);')
files['network/PassiveStepSolver.java'] = s
p = root / (prefix+'network/PassiveIntervalSolver.java')
s = p.read_text(encoding='utf-8')
needle = 'var result=implicit.solve(start,step,checkpoint);'
assert s.count(needle) == 1
s = s.replace(needle,'implicit.auditValveModes=carriedFlows(accepted)!=null?carriedModes:List.of();\n                '+needle)
files['network/PassiveIntervalSolver.java'] = s
p = root / (prefix+'solver/SparseNewton.java')
s = p.read_text(encoding='utf-8')
s = s.replace('if(norm<=settings.tolerance())return new Result(x,norm,iteration,calls,lastNonzeros,pattern.groups.size());','com.wormzjl.icheme.science.fluid.network.AuditTrace.newton(equations,x,f,"it="+iteration);\n            if(norm<=settings.tolerance())return new Result(x,norm,iteration,calls,lastNonzeros,pattern.groups.size());')
files['solver/SparseNewton.java'] = s
for name, text in files.items():
    path = tool / 'trace-src/com/wormzjl/icheme/science/fluid' / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text,encoding='utf-8')
print('Prepared trace-only copies; production sources unchanged.')
