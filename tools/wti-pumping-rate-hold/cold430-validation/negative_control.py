"""Disable only targeted regularized-row bisection, expect cold 430 to fail, and restore exact bytes."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
WT = HERE.parents[2]
JDK = Path(r"C:\Program Files\Java\jdk-21.0.11\bin")
SOURCE = WT / "src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java"
TEST = "com.wormzjl.icheme.runtime.fluid.HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold"
OUT = HERE / "negative-control"
OUT.mkdir(parents=True, exist_ok=True)

original = SOURCE.read_bytes()
text = original.decode("utf-8")
newline = "\r\n" if "\r\n" in text else "\n"
text = text.replace("\r\n", "\n")
needle = "if(targeted){if(++sweeps>12)break;"
assert text.count(needle) == 1
disabled = text.replace(needle, "if(false&&targeted){if(++sweeps>12)break;", 1)
env = os.environ.copy()
env["JAVA_HOME"] = str(JDK.parent)
env["PATH"] = str(JDK) + os.pathsep + env.get("PATH", "")
args = [str(WT / "gradlew.bat"), "fluidRuntimeTest", "--tests", TEST, "--max-workers=4"]
result = None
try:
    SOURCE.write_bytes(disabled.replace("\n", newline).encode("utf-8"))
    with (OUT / "gradle.log").open("w", encoding="utf-8") as stream:
        result = subprocess.run(args, cwd=WT, env=env, stdout=stream, stderr=subprocess.STDOUT)
    result_xml = WT / "build/test-results/fluidRuntimeTest/TEST-com.wormzjl.icheme.runtime.fluid.HoldClassWorldsTest.xml"
    shutil.copy(result_xml, OUT / "gate.xml")
    xml = (OUT / "gate.xml").read_text(encoding="utf-8")
    suite = ET.fromstring(xml)
    assert result.returncode != 0, "disabled fallback unexpectedly kept up"
    assert suite.attrib["tests"] == "1" and suite.attrib["failures"] == "1" and suite.attrib["errors"] == "0"
    assert "cold 430 filling transient keeps up" in xml
    assert "FAILED_ITERATE_HELD Held[tick=406" in xml
finally:
    SOURCE.write_bytes(original)
    assert SOURCE.read_bytes() == original
    (OUT / "restoration.json").write_text(json.dumps({
        "restored": True,
        "candidateSha256": hashlib.sha256(original).hexdigest(),
        "disabledSha256": hashlib.sha256(disabled.replace("\n", newline).encode("utf-8")).hexdigest(),
        "disabledRule": "if(false&&targeted)",
        "expectedGateFailure": result is not None and result.returncode != 0,
    }, indent=2), encoding="utf-8")

print("Single disabled-fallback gate failed with the expected tick-406 hold; candidate source restored byte-for-byte.")
