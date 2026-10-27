"""Single serial reference lane for the selected pump fix; run with no client/campaign.

Uses the repository's existing tasks and harnesses. A fresh GameTest server is
bounded because pre-existing fixtures wait for newly default-Off generators.
No Gradle source sets, switches, product instrumentation or world imports.
"""
import json
import os
import shutil
import subprocess
import xml.etree.ElementTree as ET
import validate_fix as campaign

out = campaign.HERE / "fix-final-lane"
out.mkdir(exist_ok=True)
env = os.environ.copy()
env["JAVA_HOME"] = str(campaign.JDK.parent)
head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=campaign.WT, text=True).strip()
result = {"head": head, "base": campaign.BASE, "stages": {}}


def gradle(name, args, timeout=None):
    print("START", name, flush=True)
    with (out / f"{name}.log").open("w", encoding="utf-8") as log:
        process = subprocess.Popen([str(campaign.WT / "gradlew.bat"), *args], cwd=campaign.WT,
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            code = process.wait(timeout=timeout)
            result["stages"][name] = {"exit": code}
        except subprocess.TimeoutExpired:
            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], stdout=log, stderr=subprocess.STDOUT)
            process.wait(timeout=30)
            result["stages"][name] = {"status": "incomplete-time-bound", "seconds": timeout}
    (out / "result.json").write_text(json.dumps(result, indent=2))
    print("END", name, result["stages"][name], flush=True)


def collect(task):
    destination = out / f"{task}-results"
    shutil.copytree(campaign.WT / "build/test-results" / task, destination, dirs_exist_ok=True)
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    failures = []
    lines = []
    for file in destination.glob("TEST-*.xml"):
        root = ET.parse(file).getroot()
        for key in totals:
            totals[key] += int(root.get(key, 0))
        for case in root.findall("testcase"):
            for failure in case.findall("failure"):
                failures.append({"test": f"{case.get('classname')}.{case.get('name')}", "message": failure.get("message")})
        for stdout in root.iter("system-out"):
            lines += [line for line in (stdout.text or "").splitlines()
                      if "JUNCTION" in line or "MIXED_GAS_COST" in line or "maxAbsDiff" in line or "REGRESSION" in line]
    result["stages"][task] = {"totals": totals, "failures": failures}
    (out / f"{task}-printed-lines.txt").write_text("\n".join(lines), encoding="utf-8")
    (out / "result.json").write_text(json.dumps(result, indent=2))
    print(task, totals, flush=True)


gradle("test-slow-jar", ["test", "slowGates", "jar", "--continue", "--max-workers=8"])
collect("test")
collect("slowGates")
gradle("chain-100-exact", ["--no-configuration-cache", "fluidSolverRegression", "-PfluidRegressionMode=exact", "--max-workers=8"])
collect("fluidSolverRegression")
gradle("fresh-fluid-gametest", ["--no-daemon", "--no-configuration-cache", "runFluidGameTestServer",
       f"-PfluidGameTestRunId=wti-rate-{head[:8]}-20261024", "--max-workers=8"], timeout=180)
for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
    campaign.run("full-candidate", "candidate", distribution, "1-1000", 8)
campaign.summarize()
print("FINAL_LANE_DONE", head, flush=True)
