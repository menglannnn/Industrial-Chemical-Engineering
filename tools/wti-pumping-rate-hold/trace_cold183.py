"""Trace the smallest new E5 world from the matched quick campaign; no cost replay.

Uses the existing cold placement harness and stops at the first hold. Logging reads
only cached data and already computed results. All instrumented classes stay here.
"""
from pathlib import Path
import subprocess
import validate_fix as campaign

out = campaign.HERE / "cold183-trace"
out.mkdir(exist_ok=True)
relative = Path("com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java")
source = subprocess.check_output(["git", "show", "9fa53354:src/main/java/" + relative.as_posix()], cwd=campaign.WT, text=True, encoding="utf-8")
needle = "if(rateOnly&&!samePhaseLayouts(previousOutputStates,inputStates)) {"
assert source.count(needle) == 1
source = source.replace(needle, needle + '''
            System.out.println("RATE_CACHE_DROP previousRate="+previousRateOnly+" outputCount="+previousOutputStates.size()
                    +" sameInput="+sameStates(previousInputStates,inputStates)+" nodes="+Arrays.toString(nodeIds(inputGraph)));
''')
dest = out / "src" / relative
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source, encoding="utf-8")
driver = out / "src/Cold183Trace.java"
driver.write_text('''package com.wormzjl.icheme.runtime.fluid;
import java.util.*;
import com.wormzjl.icheme.science.material.MaterialRuntime;
public class Cold183Trace {
 public static void main(String[] args) {
  MaterialRuntime.with(HoldFuzzTest.CATALOG,HoldFuzzTest.PACKAGE,()->{
   var r=new Random(183);var blocks=HoldFuzzTest.world(r);var placements=HoldFuzzTest.placements(r,blocks);
   var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
   for(var p:placements) {
    while(w.ledger.onlineTick()<p.tick()&&w.held.isEmpty())w.tick();
    if(!w.held.isEmpty())break;
    System.out.println("PLACE tick="+p.tick()+" "+p.block().label());w.place(p.block());
   }
   while(w.ledger.onlineTick()<15000&&w.held.isEmpty())w.tick();
   for(var h:w.held)System.out.println("FIRST_HOLD "+h+"\\n"+w.describe(h.island()));
   return null;
  });
 }
}
''', encoding="utf-8")
classes = out / "classes"
classes.mkdir(exist_ok=True)
cp = [str(campaign.OUT / "cold"), *campaign.paths()]
campaign.invoke([campaign.JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", ";".join(cp), "-d", classes, dest, driver], out / "compile.log")
campaign.invoke([campaign.JDK / "java.exe", "-Xmx3g", "-DholdFuzz.cold=true", "-cp", ";".join([str(classes), *cp]), "com.wormzjl.icheme.runtime.fluid.Cold183Trace"], out / "trace.log")
print((out / "trace.log").read_text()[-9000:])
