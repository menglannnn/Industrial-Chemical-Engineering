"""Trace targeted sweeps in the existing cold-555 first-hold reproduction using a copied solver."""
import hashlib
import json
import os
from pathlib import Path

from validate import HERE, OUT, invoke, paths, WT, JDK

TRACE = HERE / "cold555-recurrence"
RELATIVE = Path("com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java")
SOURCE = WT / "src/main/java" / RELATIVE
DRIVER = TRACE / "Cold555Trace.java"
CLASSES = TRACE / "classes"

original = SOURCE.read_bytes()
expected = json.loads((HERE / "negative-control-strict/restoration.json").read_text(encoding="utf-8"))["candidateSha256"]
assert hashlib.sha256(original).hexdigest() == expected, "cold-555 trace requires the recorded candidate"
text = original.decode("utf-8").replace("\r\n", "\n")
needle = "if(targeted){if(++sweeps>12)break;double[] was=p.clone(),wasResidual=residual.clone();double wasMerit=merit;"
assert text.count(needle) == 1
text = text.replace(needle, needle + '\n                    if(Boolean.getBoolean("cold430.validation.trace"))System.out.println("C555_TARGET nodes="+Arrays.toString(nodeIds(graph))+" iteration="+iteration+" floor="+floor+" regularized="+Arrays.toString(regularized)+" step="+Arrays.toString(step));', 1)
needle = "sweep(p,regularized);merit=residual(p,residual,null);"
assert text.count(needle) == 1
text = text.replace(needle, needle + '\n                    if(Boolean.getBoolean("cold430.validation.trace"))System.out.println("C555_SWEEP improved="+(merit<wasMerit)+" before="+wasMerit+" after="+merit+" was="+Arrays.toString(was)+" now="+Arrays.toString(p));', 1)
dest = TRACE / "src" / RELATIVE
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(text, encoding="utf-8", newline="\n")
CLASSES.mkdir(parents=True, exist_ok=True)
cp = [str(OUT / "cold"), *paths()]
invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", os.pathsep.join(cp),
        "-d", CLASSES, dest, DRIVER], TRACE / "compile.log")
invoke([JDK / "java.exe", "-Xshare:off", "-Xmx3g", "-Duser.language=en", "-Duser.country=US",
        "-DholdFuzz.cold=true", "-Dcold430.validation.trace=true",
        "-cp", os.pathsep.join([str(CLASSES), *cp]), "com.wormzjl.icheme.runtime.fluid.Cold555Trace"],
       TRACE / "trace.log")
(TRACE / "provenance.json").write_text(json.dumps({
    "candidateSha256": expected,
    "instrumentedSha256": hashlib.sha256(dest.read_bytes()).hexdigest(),
    "productSourceUnchanged": SOURCE.read_bytes() == original,
    "scope": "Copied solver only; existing cold-555 placement sequence stops at its first hold.",
}, indent=2), encoding="utf-8")
print(TRACE / "trace.log", flush=True)
