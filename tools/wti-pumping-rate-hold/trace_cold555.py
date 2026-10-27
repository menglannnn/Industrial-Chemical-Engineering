"""Second and final minimized first-hold trace: new full-campaign cold 555, no cost replay."""
from pathlib import Path
import subprocess
import validate_fix as campaign

out = campaign.HERE / "cold555-trace"
out.mkdir(exist_ok=True)
relative = Path("com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java")
source = subprocess.check_output(["git", "show", "c49f6654:src/main/java/" + relative.as_posix()], cwd=campaign.WT, text=True, encoding="utf-8")
needle = '        lastSolve=null;\n        if(!Double.isFinite(dt)'
assert source.count(needle) == 1
source = source.replace(needle, '        boolean traceAccepted=lastSolve!=null;\n'+needle)
needle = '        var seeds=rateOnly&&previousRateOnly&&carriedPoint(graph,identities(graph))'
assert source.count(needle) == 1
source = source.replace(needle, '''        if(rateOnly&&previousRateOnly&&carriedPoint(graph,identities(graph))&&!samePhaseLayouts(previousOutputStates,inputStates))
            System.out.println("RATE_PHASE_CARRY accepted="+traceAccepted+" nodes="+Arrays.toString(nodeIds(graph))+" modes="+previousModes);
'''+needle)
dest = out / "src" / relative
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source, encoding="utf-8")
driver = out / "src/Cold555Trace.java"
template = (campaign.HERE / "cold183-trace/src/Cold183Trace.java").read_text(encoding="utf-8")
driver.write_text(template.replace("Cold183Trace", "Cold555Trace").replace("new Random(183)", "new Random(555)"), encoding="utf-8")
classes = out / "classes"
classes.mkdir(exist_ok=True)
cp = [str(campaign.OUT / "cold"), *campaign.paths()]
campaign.invoke([campaign.JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", ";".join(cp), "-d", classes, dest, driver], out / "compile.log")
campaign.invoke([campaign.JDK / "java.exe", "-Xmx3g", "-DholdFuzz.cold=true", "-cp", ";".join([str(classes), *cp]), "com.wormzjl.icheme.runtime.fluid.Cold555Trace"], out / "trace.log")
print((out / "trace.log").read_text()[-6000:])
