"""Insert gated cold-431 diagnostics into copied solver sources only."""
from pathlib import Path

PROPERTY = "icheme.cold431.trace"


def replace_once(text, old, new, path):
    count = text.count(old)
    assert count == 1, f"{path}: expected one instrumentation site, found {count}: {old[:100]}"
    return text.replace(old, new, 1)


def instrument_step(path):
    path = Path(path)
    text = path.read_text(encoding="utf-8")
    original = text
    text = replace_once(text, "public final class PassiveStepSolver {",
        "public final class PassiveStepSolver {\n    private static boolean cold431Traced(){return System.getProperty(\"icheme.cold431.trace\")!=null;}", path)
    text = replace_once(text,
        "    private Result solve(PassiveNetwork inputGraph,double dt,Runnable checkpoint,boolean rateOnly,List<FlowControl.Mode> acceptedEndpointModes) {",
        "    private Result solve(PassiveNetwork inputGraph,double dt,Runnable checkpoint,boolean rateOnly,List<FlowControl.Mode> acceptedEndpointModes) {\n"
        "        if(cold431Traced())System.out.println(\"S_STEP_BEGIN dt=\"+dt+\" rateOnly=\"+rateOnly+\" nodes=\"+inputGraph.reservoirs().size()+\" pipes=\"+inputGraph.pipes().size());", path)
    text = replace_once(text,
        "            else{start=balanceStart(graph,modes,boundaryClosed,keep,checkpoint);balanceFor=inputGraph;balanceCache=start;balanceClosedFor=boundaryClosed.clone();}",
        "            else{start=balanceStart(graph,modes,boundaryClosed,keep,checkpoint);balanceFor=inputGraph;balanceCache=start;balanceClosedFor=boundaryClosed.clone();"
        "if(cold431Traced())System.out.println(\"S_BALANCE_RETURN converged=\"+start.converged());}", path)
    text = replace_once(text,
        "            checkpoint.run();SolverDiagnostics.count(SolverDiagnostics.activeSetPasses);",
        "            checkpoint.run();SolverDiagnostics.count(SolverDiagnostics.activeSetPasses);\n"
        "            if(cold431Traced())System.out.println(\"S_PASS_BEGIN pass=\"+pass+\" maximum=\"+maximumPasses);", path)
    text = replace_once(text,
        "            try{numerical=SparseNewton.solve(equations,equations.initial(),new SparseNewton.Settings(SparseNewton.ITERATIONS,tolerance,1e-6,24),checkpoint,workspace);}",
        "            try{numerical=SparseNewton.solve(equations,equations.initial(),new SparseNewton.Settings(SparseNewton.ITERATIONS,tolerance,1e-6,24),checkpoint,workspace);"
        "if(cold431Traced())System.out.println(\"S_NEWTON_OK pass=\"+pass+\" residual=\"+numerical.residualNorm()+\" iterations=\"+numerical.iterations()+\" evaluations=\"+numerical.residualEvaluations());}", path)
    text = replace_once(text,
        "            catch(SparseNewton.Nonconvergence failure) {\n                if(failure.lastVariables()==null)throw failure;",
        "            catch(SparseNewton.Nonconvergence failure) {\n"
        "                if(cold431Traced())System.out.println(\"S_NEWTON_FAIL pass=\"+pass+\" message=\"+failure.getMessage());\n"
        "                if(failure.lastVariables()==null)throw failure;", path)
    text = replace_once(text,
        "        int edges=graph.pipes().size();\n        var current=graph;boolean converged=true;BalanceNet net=null;double[] p=null;",
        "        int edges=graph.pipes().size();\n        var current=graph;boolean converged=true;BalanceNet net=null;double[] p=null;\n"
        "        if(cold431Traced())System.out.println(\"S_BALANCE_BEGIN edges=\"+edges);", path)
    text = replace_once(text,
        "            p=net.solve(checkpoint);converged=net.converged;",
        "            p=net.solve(checkpoint);converged=net.converged;\n"
        "            if(cold431Traced())System.out.println(\"S_BALANCE_ROUND round=\"+round+\" converged=\"+converged+\" residual=\"+net.finalResidual+\" iterations=\"+net.iterations+\" sweeps=\"+net.sweeps+\" p=\"+Arrays.toString(p));", path)
    text = replace_once(text,
        "        BalanceNet(PassiveNetwork graph,PassiveNetwork buffers,List<FlowControl.Mode> modes,boolean[] boundaryClosed,Runnable checkpoint) {",
        "        BalanceNet(PassiveNetwork graph,PassiveNetwork buffers,List<FlowControl.Mode> modes,boolean[] boundaryClosed,Runnable checkpoint) {\n"
        "            if(cold431Traced())System.out.println(\"B_NET_CONSTRUCT_BEGIN\");", path)
    text = replace_once(text,
        "        private void prepare(int e,Runnable checkpoint) {\n            var pipe=graph.pipes().get(e);var mode=modes.get(e);",
        "        private void prepare(int e,Runnable checkpoint) {\n"
        "            if(cold431Traced())System.out.println(\"B_PREPARE edge=\"+e);\n"
        "            var pipe=graph.pipes().get(e);var mode=modes.get(e);", path)
    text = replace_once(text,
        "                    try{var at=model.equilibriumPH(s.temperature(),tp[k],h,held.clone(),false,checkpoint);",
        "                    if(cold431Traced())System.out.println(\"B_TABLE node=\"+j+\" point=\"+k+\" pressure=\"+tp[k]);\n"
        "                    try{var at=model.equilibriumPH(s.temperature(),tp[k],h,held.clone(),false,checkpoint);", path)
    text = replace_once(text,
        "                if(ok){tableP[j]=tp;tableRho[j]=tr;tableRhoV[j]=tv;TABLE_CACHE.put(s,new double[][]{tp,tr,tv});}\n            }\n        }\n        /** The held data",
        "                if(ok){tableP[j]=tp;tableRho[j]=tr;tableRhoV[j]=tv;TABLE_CACHE.put(s,new double[][]{tp,tr,tv});}\n            }\n"
        "            if(cold431Traced())System.out.println(\"B_NET_CONSTRUCT_END count=\"+count+\" band=\"+band+\" low=\"+low+\" high=\"+high+\" scale=\"+scale+\" unknown=\"+Arrays.toString(unknown));\n"
        "        }\n        /** The held data", path)
    text = replace_once(text,
        "            double[] residual=new double[count];double merit=residual(p,residual,null);",
        "            double[] residual=new double[count];double merit=residual(p,residual,null);\n"
        "            if(cold431Traced())System.out.println(\"B_BEGIN count=\"+count+\" band=\"+band+\" low=\"+low+\" high=\"+high+\" scale=\"+scale+\" p=\"+Arrays.toString(p)+\" residual=\"+maximum(residual));", path)
    text = replace_once(text,
        "                checkpoint.run();finalResidual=maximum(residual);",
        "                checkpoint.run();finalResidual=maximum(residual);\n"
        "                if(cold431Traced())System.out.println(\"B_ITER iteration=\"+iteration+\" merit=\"+merit+\" residual=\"+finalResidual+\" tolerance=\"+tolerance);", path)
    text = replace_once(text,
        "                    for(double s:step)largest=Math.max(largest,Math.abs(s));",
        "                    if(cold431Traced())System.out.println(\"B_NEWTON iteration=\"+iteration+\" floor=\"+floor+\" step=\"+Arrays.toString(step));\n"
        "                    for(double s:step)largest=Math.max(largest,Math.abs(s));", path)
    targeted = "if(targeted){if(++sweeps>12)break;double[] was=p.clone(),wasResidual=residual.clone();double wasMerit=merit;"
    if targeted in text:
        text = replace_once(text, targeted,
            "if(targeted){if(cold431Traced())System.out.println(\"B_TARGETED iteration=\"+iteration+\" regularized=\"+Arrays.toString(regularized)+\" step=\"+Arrays.toString(step));if(++sweeps>12)break;double[] was=p.clone(),wasResidual=residual.clone();double wasMerit=merit;", path)
        text = replace_once(text,
            "                    sweep(p,regularized);merit=residual(p,residual,null);",
            "                    sweep(p,regularized);merit=residual(p,residual,null);\n"
            "                    if(cold431Traced())System.out.println(\"B_TARGET_SWEEP before=\"+wasMerit+\" after=\"+merit+\" p=\"+Arrays.toString(p));", path)
    text = replace_once(text,
        "                        if(trialMerit<(1-1e-4*fraction)*merit){System.arraycopy(trial,0,p,0,nodes);",
        "                        if(trialMerit<(1-1e-4*fraction)*merit){if(cold431Traced())System.out.println(\"B_ARMIJO iteration=\"+iteration+\" fraction=\"+fraction+\" before=\"+merit+\" after=\"+trialMerit);System.arraycopy(trial,0,p,0,nodes);", path)
    text = replace_once(text,
        "                if(!accepted){if(++sweeps>12)break;double[] was=p.clone(),wasResidual=residual.clone();double wasMerit=merit;sweep(p);merit=residual(p,residual,null);",
        "                if(!accepted){if(cold431Traced())System.out.println(\"B_FALLBACK iteration=\"+iteration);if(++sweeps>12)break;double[] was=p.clone(),wasResidual=residual.clone();double wasMerit=merit;sweep(p);merit=residual(p,residual,null);\n"
        "                    if(cold431Traced())System.out.println(\"B_FALLBACK_SWEEP before=\"+wasMerit+\" after=\"+merit);", path)
    text = replace_once(text,
        "            finalResidual=maximum(residual);converged=finalResidual<=tolerance;\n            return p;",
        "            finalResidual=maximum(residual);converged=finalResidual<=tolerance;\n"
        "            if(cold431Traced())System.out.println(\"B_END converged=\"+converged+\" residual=\"+finalResidual+\" iterations=\"+iterations+\" sweeps=\"+sweeps+\" p=\"+Arrays.toString(p));\n"
        "            return p;", path)
    text = replace_once(text,
        "        PassiveNetwork seeded(double[] p,Runnable checkpoint) {\n            double[] q=new double[edges];",
        "        PassiveNetwork seeded(double[] p,Runnable checkpoint) {\n"
        "            if(cold431Traced())System.out.println(\"B_SEEDED_BEGIN\");\n"
        "            double[] q=new double[edges];", path)
    assert text != original
    path.write_text(text, encoding="utf-8", newline="\n")


def instrument_interval(path):
    path = Path(path)
    text = path.read_text(encoding="utf-8")
    original = text
    text = replace_once(text, "public final class PassiveIntervalSolver {",
        "public final class PassiveIntervalSolver {\n    private static boolean cold431Traced(){return System.getProperty(\"icheme.cold431.trace\")!=null;}", path)
    text = replace_once(text,
        "    public Result solve(PassiveNetwork initial,double duration,Settings settings,Runnable checkpoint,double startingStep) {",
        "    public Result solve(PassiveNetwork initial,double duration,Settings settings,Runnable checkpoint,double startingStep) {\n"
        "        if(cold431Traced())System.out.println(\"I_INTERVAL_BEGIN duration=\"+duration+\" startingStep=\"+startingStep);", path)
    text = replace_once(text,
        "        var conversions=InventoryEquilibrium.conversions(initial,model,checkpoint);",
        "        var conversions=InventoryEquilibrium.conversions(initial,model,checkpoint);\n"
        "        if(cold431Traced())System.out.println(\"I_CONVERSIONS converted=\"+conversions.converted());", path)
    text = replace_once(text,
        "        var start=InventoryEquilibrium.completed(initial,conversions,model,checkpoint);",
        "        var start=InventoryEquilibrium.completed(initial,conversions,model,checkpoint);\n"
        "        if(cold431Traced())System.out.println(\"I_REFRESH_COMPLETE\");", path)
    text = replace_once(text,
        "        if(!Double.isFinite(startingStep)||startingStep<=0)throw new IllegalArgumentException(\"Positive finite starting step required\");\n        PassiveNetwork accepted=refreshed.graph();",
        "        if(!Double.isFinite(startingStep)||startingStep<=0)throw new IllegalArgumentException(\"Positive finite starting step required\");\n"
        "        if(cold431Traced())System.out.println(\"I_RUN_BEGIN duration=\"+duration+\" startingStep=\"+startingStep);\n"
        "        PassiveNetwork accepted=refreshed.graph();", path)
    text = replace_once(text,
        "            checkpoint.run();double step=Math.min(h,duration-elapsed);",
        "            checkpoint.run();double step=Math.min(h,duration-elapsed);\n"
        "            if(cold431Traced())System.out.println(\"I_ATTEMPT_BEGIN attempt=\"+attempt+\" elapsed=\"+elapsed+\" step=\"+step+\" accepted=\"+acceptedCount+\" rejected=\"+rejectedCount);", path)
    text = replace_once(text,
        "                var result=implicit.solve(start,step,checkpoint,carriedFlows(accepted)!=null?carriedModes:null);",
        "                if(cold431Traced())System.out.println(\"I_STEP_CALL coldSeed=\"+(start!=accepted));\n"
        "                var result=implicit.solve(start,step,checkpoint,carriedFlows(accepted)!=null?carriedModes:null);\n"
        "                if(cold431Traced())System.out.println(\"I_STEP_RETURN flows=\"+Arrays.toString(result.massFlows()));", path)
    text = replace_once(text,
        "                accepted=replace(accepted,result);last=result;elapsed+=step;acceptedCount++;consecutiveRejects=0;previousStateChange=0;",
        "                if(cold431Traced())System.out.println(\"I_ACCEPT attempt=\"+attempt+\" change=\"+change+\" step=\"+step);\n"
        "                accepted=replace(accepted,result);last=result;elapsed+=step;acceptedCount++;consecutiveRejects=0;previousStateChange=0;", path)
    text = replace_once(text,
        "            }catch(SolidEventIntegrator.Transition transition) {",
        "            }catch(SolidEventIntegrator.Transition transition) {\n"
        "                if(cold431Traced())System.out.println(\"I_TRANSITION attempt=\"+attempt+\" step=\"+step+\" atStart=\"+transition.atStart+\" edge=\"+transition.edge+\" pipe=\"+transition.pipeId+\" message=\"+transition.getMessage());", path)
    text = replace_once(text,
        "            }catch(SparseNewton.Nonconvergence|IllegalArgumentException|StateChangeRejection rejected) {\n                lastRejection=String.valueOf(rejected.getMessage());",
        "            }catch(SparseNewton.Nonconvergence|IllegalArgumentException|StateChangeRejection rejected) {\n"
        "                if(cold431Traced())System.out.println(\"I_REJECT attempt=\"+attempt+\" step=\"+step+\" type=\"+rejected.getClass().getSimpleName()+\" message=\"+rejected.getMessage());\n"
        "                lastRejection=String.valueOf(rejected.getMessage());", path)
    text = replace_once(text,
        "    private PassiveNetwork coldRateSeed(PassiveNetwork graph,Runnable checkpoint) {\n        if(graph.pipes().isEmpty()||graph.reservoirs().stream().noneMatch(PassiveNetwork.Reservoir::junction))return graph;\n        var rate=implicit.portRate(graph,checkpoint);",
        "    private PassiveNetwork coldRateSeed(PassiveNetwork graph,Runnable checkpoint) {\n"
        "        if(cold431Traced())System.out.println(\"I_COLD_RATE_BEGIN\");\n"
        "        if(graph.pipes().isEmpty()||graph.reservoirs().stream().noneMatch(PassiveNetwork.Reservoir::junction))return graph;\n        var rate=implicit.portRate(graph,checkpoint);\n"
        "        if(cold431Traced())System.out.println(\"I_COLD_RATE_RETURN converged=\"+(rate!=null));", path)
    assert text != original
    path.write_text(text, encoding="utf-8", newline="\n")


def instrument(step_path, interval_path):
    instrument_step(step_path)
    instrument_interval(interval_path)
