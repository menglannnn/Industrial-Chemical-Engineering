"""Collect the completed lane's references/provenance and compress its evidence."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile
import xml.etree.ElementTree as ET
import validate_fix as campaign

lane = campaign.HERE / "fix-final-lane"
stdout = []
prefixes = Counter()
for file in sorted(lane.glob("*-results/TEST-*.xml")):
    for item in ET.parse(file).getroot().iter("system-out"):
        for line in (item.text or "").splitlines():
            if line.strip():
                prefixes[line.split()[0]] += 1
                stdout.append(line)
(lane / "all-printed-references.txt").write_text("\n".join(stdout), encoding="utf-8")
references = [line for line in stdout if line.startswith(("LIQUID_JUNCTION ", "MIXED_GAS_TRANSIENT ", "JUNCTION_ARRIVING_PHASE "))]
assert len(references) == 33
(lane / "33-junction-references.txt").write_text("\n".join(references), encoding="utf-8")
files = [
    "src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java",
    "src/main/java/com/wormzjl/icheme/runtime/fluid/FluidWorldAuthority.java",
    "src/main/java/com/wormzjl/icheme/runtime/fluid/PumpStatus.java",
    "src/test/java/com/wormzjl/icheme/science/fluid/network/PumpRateWarmStartTest.java",
    "src/test/java/com/wormzjl/icheme/runtime/fluid/PumpStatusTest.java",
    "src/fluidGameTest/java/com/wormzjl/icheme/fluid/gametest/FluidPhasePortGameTests.java",
]
manifest = {
    "head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=campaign.WT, text=True).strip(),
    "base": campaign.BASE,
    "candidateForms": {"quick-candidate": "9fa53354", "quick-refined": "07b105ae", "quick-complete": "c49f6654"},
    "chosenApproach": "complete rate point, restricted to the existing carried mover-limit stage",
    "finalScopeSource": "98f3dce3",
    "presentationAndIntegrationSource": "cb89f23a",
    "fullCampaignSource": "c49f6654",
    "finalQualification": "scope-followup/result.json (required merge gates and two matched 200-seed cohorts)",
    "sourceSha256": {name: hashlib.sha256((campaign.WT / name).read_bytes()).hexdigest() for name in files},
    "printedPrefixes": dict(prefixes),
    "negativeSourceRestoration": json.loads((campaign.HERE / "fix-validation/complete-point-negative/restoration.json").read_text()),
}
prior_path = campaign.HERE.parent / "fluid-world-inspector/validation/campaign-summary.json"
current_path = campaign.OUT / "campaign-summary.json"
if prior_path.exists() and current_path.exists():
    prior = json.loads(prior_path.read_text())
    current = json.loads(current_path.read_text())
    comparison = {"reference": str(prior_path), "referenceSource": prior["candidate"],
                  "scope": "Historical 1000-seed reference; current-base matched comparison is separately 1-200.", "distributions": {}}
    for dist, result in current["full"].items():
        old = {seed: name for name, seeds in prior["full"][dist]["classes"].items() for seed in seeds}
        new = {seed: name for name, seeds in result["classes"].items() for seed in seeds}
        comparison["distributions"][dist] = {"newlyHeldVsHistorical": sorted(set(new) - set(old)),
            "changedFirstClassesVsHistorical": {seed: [old.get(seed), new.get(seed)] for seed in set(old) | set(new) if old.get(seed) != new.get(seed)}}
    (lane / "historical-full-comparison.json").write_text(json.dumps(comparison, indent=2))
(lane / "selected-manifest.json").write_text(json.dumps(manifest, indent=2))
archive = campaign.HERE / "logs/wti-pump-fix-logs-2026-10-24.tar.gz"
archive.parent.mkdir(exist_ok=True)
with tarfile.open(archive, "w:gz") as tar:
    for directory in ("fix-final-lane", "fix-validation-campaign", "fix-validation", "cold183-trace", "cold555-trace", "scope-followup", "integration"):
        for file in sorted((campaign.HERE / directory).rglob("*")):
            if file.is_file() and file.suffix not in (".class", ".pyc"):
                tar.add(file, arcname=str(file.relative_to(campaign.HERE)))
    for file in sorted((campaign.WT / "build").glob("wti-pump-fix-*.log")):
        tar.add(file, arcname="gradle-orchestration/" + file.name)
    for name in ("validate_fix.py", "trace_cold183.py", "trace_cold555.py", "negative_control.py", "final_lane.py", "scope_followup.py", "integrate.py", "archive_fix.py"):
        tar.add(campaign.HERE / name, arcname="scripts/" + name)
print(json.dumps({"archive": str(archive), "bytes": archive.stat().st_size, "references": len(references)}))
