"""Incremental checks for the carried-mover scope correction; no second full lane.

The one full campaign found cold 555 in passive-loop seeding, outside the affected
mover-limit carry. Compare the original quick cohort and its 401-600 cohort using
the final scoped code, serially. Never replay a new Newton hold as a cost run.
"""
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
import validate_fix as campaign

out = campaign.HERE / "scope-followup"
out.mkdir(exist_ok=True)
result = {"head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=campaign.WT, text=True).strip(),
          "scope": "Original 1-200 comparison and matched 401-600 follow-up; one prior full campaign only.", "gates": {}, "comparisons": {}}
for task in ("test", "slowGates"):
    dest = out / f"{task}-results"
    shutil.copytree(campaign.WT / "build/test-results" / task, dest, dirs_exist_ok=True)
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    failures = []
    for file in dest.glob("TEST-*.xml"):
        root = ET.parse(file).getroot()
        for key in totals: totals[key] += int(root.get(key, 0))
        failures += [f"{case.get('classname')}.{case.get('name')}" for case in root.findall("testcase") if case.find("failure") is not None]
    result["gates"][task] = {"totals": totals, "failures": failures}
    print(task, totals, flush=True)
for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
    campaign.run("quick-scoped", "candidate", distribution, "1-200", 4)
for form in ("baseline", "candidate"):
    for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
        campaign.run(f"cohort-{form}", form, distribution, "401-600", 4)
for cohort, baseline, candidate in (("1-200", "quick-baseline", "quick-scoped"), ("401-600", "cohort-baseline", "cohort-candidate")):
    result["comparisons"][cohort] = {}
    for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
        a, b = campaign.report(baseline, distribution), campaign.report(candidate, distribution)
        classes = lambda r: {seed: cls for cls, seeds in r["classes"].items() for seed in seeds}
        ca, cb = classes(a), classes(b)
        worse = {seed: [a["heldPublications"][seed], count, cb.get(seed)] for seed, count in b["heldPublications"].items() if count>a["heldPublications"][seed]}
        result["comparisons"][cohort][distribution] = {"baseline": a, "candidate": b,
                "newlyHeld": sorted(set(cb)-set(ca)), "changedClasses": {seed: [ca.get(seed), cb.get(seed)] for seed in set(ca)|set(cb) if ca.get(seed)!=cb.get(seed)}, "moreHeldPublications": worse}
        print(cohort, distribution, "new=", sorted(set(cb)-set(ca)), "more=", worse, flush=True)
(out / "result.json").write_text(json.dumps(result, indent=2))
print("SCOPE_FOLLOWUP_DONE", result["head"], flush=True)
