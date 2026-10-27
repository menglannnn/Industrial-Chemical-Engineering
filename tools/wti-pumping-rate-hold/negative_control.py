"""Disable only complete-point phase carry for its one gate, then restore exact bytes."""
import hashlib
import json
import os
import shutil
import subprocess
import validate_fix as campaign

source = campaign.WT / "src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java"
original = source.read_bytes()
text = original.decode("utf-8")
newline = "\r\n" if "\r\n" in text else "\n"
text = text.replace("\r\n", "\n")
needle = """var seeds=rateOnly&&previousRateOnly&&carriedMoverLimit&&carriedPoint(graph,identities(graph))
                &&!samePhaseLayouts(previousOutputStates,inputStates)?previousOutputStates
                :initialPhaseSeeds(graph,dt,checkpoint,reachable,modes,boundaryClosed);"""
assert text.count(needle) == 1
disabled = text.replace(needle, "var seeds=initialPhaseSeeds(graph,dt,checkpoint,reachable,modes,boundaryClosed);")
out = campaign.HERE / "fix-validation/complete-point-negative"
out.mkdir(parents=True, exist_ok=True)
env = os.environ.copy()
env["JAVA_HOME"] = str(campaign.JDK.parent)
args = [str(campaign.WT / "gradlew.bat"), "test", "--tests", "*PumpRateWarmStartTest.repeatedRateCallsKeepTheFlashedPumpLimitConvergent", "--max-workers=8"]
try:
    source.write_bytes(disabled.replace("\n", newline).encode("utf-8"))
    with (out / "gradle.log").open("w", encoding="utf-8") as stream:
        run = subprocess.run(args, cwd=campaign.WT, env=env, stdout=stream, stderr=subprocess.STDOUT)
    shutil.copy(campaign.WT / "build/test-results/test/TEST-com.wormzjl.icheme.science.fluid.network.PumpRateWarmStartTest.xml", out / "gate.xml")
    assert run.returncode != 0
    assert 'failures="1"' in (out / "gate.xml").read_text(encoding="utf-8")
finally:
    source.write_bytes(original)
    assert source.read_bytes() == original
    (out / "restoration.json").write_text(json.dumps({"sha256": hashlib.sha256(original).hexdigest(), "restored": True}, indent=2))
print("Single disabled-rule gate failed as expected; selected source restored byte-for-byte.")
