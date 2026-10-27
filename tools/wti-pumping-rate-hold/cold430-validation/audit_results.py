"""Audit retained cold-430 evidence and the restored product tree; run no solver."""
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

from validate import BASE, HERE, OUT, WT


def junit(name):
    root = ET.parse(HERE / name).getroot()
    suite = root if root.tag == "testsuite" else root.find("testsuite")
    return {key: int(suite.attrib[key]) for key in ("tests", "failures", "errors", "skipped")}


def world(label, distribution="cold-random"):
    path = OUT / f"{label}-{distribution}-report.txt"
    lines = path.read_text(encoding="utf-8").splitlines()
    result = {"summary": next(line for line in lines if line.startswith("SUMMARY")), "seeds": {}}
    seed = None
    for line in lines:
        if match := re.match(r"SEED (\d+) blocks=(\d+) ms=(\d+) end=(\d+) holds=(\d+) classes=\[(.*)\] refused=(\d+) islands=(.*)", line):
            seed = int(match[1])
            result["seeds"][seed] = {"blocks": int(match[2]), "endTick": int(match[4]),
                                      "heldPublications": int(match[5]), "classes": match[6].split(", ") if match[6] else [],
                                      "refused": int(match[7]), "finalIslands": match[8], "holds": [], "exceptions": []}
        elif seed is not None and line.startswith("  HELD "):
            match = re.match(r"  HELD tick=(\d+) island=(\d+) committed=(\d+) class=([^ ]+) status=(.*)", line)
            result["seeds"][seed]["holds"].append({"tick": int(match[1]), "island": int(match[2]),
                                                       "committed": int(match[3]), "class": match[4], "status": match[5]})
        elif seed is not None and line.startswith("  EXCEPTION "):
            result["seeds"][seed]["exceptions"].append(line.removeprefix("  EXCEPTION "))
    return result


def gate_log(name):
    text = (HERE / name).read_text(encoding="utf-8")
    total, failed = re.search(r"(\d+) tests completed, (\d+) failed", text).groups()
    return {"tests": int(total), "failed": int(failed),
            "failures": re.findall(r"^(\S+Test > \S+\(\)) FAILED$", text, re.MULTILINE)}


strict = json.loads((OUT / "campaign-summary.json").read_text(encoding="utf-8"))
coverage = json.loads((OUT / "coverage-comparison.json").read_text(encoding="utf-8"))
armijo = json.loads((OUT / "armijo-matched-summary.json").read_text(encoding="utf-8"))
matched = {}
for scope, distributions in armijo["matched"].items():
    matched[scope] = {}
    for distribution, comparison in distributions.items():
        matched[scope][distribution] = {
            "baselineSummary": comparison["baseline"]["summary"], "candidateSummary": comparison["candidate"]["summary"],
            "newlyHeldSeeds": comparison["newlyHeldSeeds"], "recoveredSeeds": comparison["recoveredSeeds"],
            "changedFirstHoldClasses": comparison["changedFirstHoldClasses"],
            "moreHeldPublications": comparison["moreHeldPublications"],
            "newtonIterations": [comparison["baseline"]["counters"]["newtonIterations"],
                                 comparison["candidate"]["counters"]["newtonIterations"]],
        }
result = {
    "baseline": BASE,
    "productDiffAfterRejection": subprocess.check_output(["git", "diff", "--", "src/main/java", "src/test/java"], cwd=WT, text=True),
    "form1StrictImprovement": {
        "candidateSha256": json.loads((HERE / "negative-control-strict/restoration.json").read_text())["candidateSha256"],
        "focused": junit("focused-candidate.xml"), "negativeControl": junit("negative-control-strict/gate.xml"),
        "quick": {distribution: {key: values[key] for key in ("newlyHeldSeeds", "recoveredSeeds", "changedFirstHoldClasses", "moreHeldPublications")}
                  for distribution, values in strict["quick"].items()},
        "full": {distribution: {"summary": values["summary"], "classes": values["classes"]}
                 for distribution, values in strict["full"].items()},
        "exactCurrentCoverage": coverage["distributions"]["cold-random"]["exactFinalCoverage"],
        "repositoryGates": {"test": gate_log("quick-suite.log"), "slowGates": gate_log("slow-gates.log")},
    },
    "form2Armijo": {
        "candidateSha256": armijo["candidateSha256"], "focused": junit("focused-armijo.xml"),
        "negativeControl": junit("negative-control-armijo/gate.xml"), "matched": matched,
        "isolatedBudget": {form: world(f"isolated-budget-{form}-armijo-nonverbose") for form in ("baseline", "candidate")},
        "cold555": world("diagnostic-armijo-555"),
    },
    "form3BestAcceptedMerit": {"focused": junit("focused-best.xml")},
    "restoredBaselineBuild": "BUILD SUCCESSFUL" in (HERE / "restored-baseline-final.log").read_text(encoding="utf-8"),
}
assert not result["productDiffAfterRejection"]
assert result["restoredBaselineBuild"]
assert result["form1StrictImprovement"]["focused"]["failures"] == 0
assert result["form2Armijo"]["focused"]["failures"] == 0
assert result["form3BestAcceptedMerit"]["focused"]["failures"] == 1
for form in ("baseline", "candidate"):
    budget = result["form2Armijo"]["isolatedBudget"][form]["seeds"]
    assert budget[431]["endTick"] == 14158 and not budget[431]["exceptions"]
    assert budget[475]["endTick"] == 13010 and budget[475]["heldPublications"] == 0
assert result["form2Armijo"]["isolatedBudget"]["baseline"]["seeds"][431]["heldPublications"] == 7
assert result["form2Armijo"]["isolatedBudget"]["candidate"]["seeds"][431]["heldPublications"] == 11
assert result["form2Armijo"]["cold555"]["seeds"][555]["heldPublications"] == 0
(HERE / "validation-summary.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
print("Form 1 full:", {name: values["summary"] for name, values in result["form1StrictImprovement"]["full"].items()})
print("Form 1 exact-current differences:", result["form1StrictImprovement"]["exactCurrentCoverage"])
print("Form 2 matched:", {scope: {name: values["moreHeldPublications"] for name, values in distributions.items()}
                           for scope, distributions in matched.items()})
for form, values in result["form2Armijo"]["isolatedBudget"].items():
    print("Isolated", form, {seed: (case["heldPublications"], case["endTick"], case["finalIslands"])
                             for seed, case in values["seeds"].items()})
print("Focused:", result["form1StrictImprovement"]["focused"], result["form2Armijo"]["focused"], result["form3BestAcceptedMerit"]["focused"])
print("Restored baseline: no Java diff; compile successful")
