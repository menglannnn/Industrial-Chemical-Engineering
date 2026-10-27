"""Read existing final-code hold reports; no solver, world load or client action.

Use the repository classifier's existing first-hold classes. Count a distribution
and seed as one case, regardless of repeated publications or later recovery.
Keep the earlier full campaign separate from the final scoped-code coverage.
"""
from collections import Counter
import json
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parent
reports = root / "fix-validation-campaign"
out = root / "remaining-holds"
out.mkdir(exist_ok=True)
distributions = ("cold-random", "bundled-random", "bundled-defaults")


def read(label, distribution):
    path = reports / f"{label}-{distribution}-report.txt"
    raw = path.read_text(encoding="utf-8")
    classes = {}
    for line in Path(str(path) + ".classes.txt").read_text().splitlines():
        name, fields = line.split("seeds=", 1)
        for seed in fields.split("list=", 1)[1].split():
            classes[int(seed)] = name.strip()
    cases = {}
    for block in re.split(r"(?=^SEED \d+ )", raw, flags=re.M):
        header = block.splitlines()[0] if block else ""
        match = re.match(r"SEED (\d+).* holds=(\d+).* islands=(.*)", header)
        if match is None:
            continue
        seed, count = int(match[1]), int(match[2])
        holds = [dict(tick=int(m[1]), island=int(m[2]), committed=int(m[3]), status=m[4])
                 for m in re.finditer(r"^  HELD tick=(\d+) island=(\d+) committed=(\d+) class=\S+ status=(.*)$", block, flags=re.M)]
        assert len(holds) == count, (path, seed, count, len(holds))
        cases[seed] = dict(distribution=distribution, seed=seed, firstClass=classes.get(seed),
                           heldPublications=count, heldAtEnd=":HELD" in match[3], finalIslands=match[3],
                           firstHold=holds[0] if holds else None, lastHold=holds[-1] if holds else None,
                           layout=next((line.strip()[7:] for line in block.splitlines() if line.startswith("  layout ")), ""),
                           report=str(path.relative_to(root)))
    return cases


verified = {}
for label in ("quick-scoped", "cohort-candidate"):
    for distribution in distributions:
        for seed, case in read(label, distribution).items():
            key = (distribution, seed)
            assert key not in verified
            verified[key] = case
held = [case for case in verified.values() if case["heldPublications"]]
counts = Counter(case["firstClass"] for case in held)
held.sort(key=lambda case: (-counts[case["firstClass"]], case["firstClass"], case["distribution"], case["seed"]))
historical_unchecked = []
historical_clean_on_recheck = []
for distribution in distributions:
    for seed, case in read("full-candidate", distribution).items():
        if not case["heldPublications"]:
            continue
        final = verified.get((distribution, seed))
        if final is None:
            historical_unchecked.append(case)
        elif not final["heldPublications"]:
            historical_clean_on_recheck.append(case)
result = dict(date="2026-10-24", integration="cb89f23a", numericalSource="98f3dce3",
              finalCoverage="Seeds 1-200 and 401-600 in each of three distributions; no new solve",
              classification="Existing classify.sh first hold per distribution/seed; not a root-cause diagnosis",
              totalCases=len(verified), casesWithHold=len(held), heldAtEnd=sum(case["heldAtEnd"] for case in held),
              heldPublications=sum(case["heldPublications"] for case in held),
              firstHoldClasses=dict(counts.most_common()), finalCases=held,
              historicalFullSource="c49f6654 (before final scope correction)",
              historicalOutsideFinalCoverage=historical_unchecked,
              historicalCleanOnFinalRecheck=historical_clean_on_recheck)
latest_classes = {}
for distribution in distributions:
    cases = [case for case in held if case["heldAtEnd"] and case["distribution"] == distribution]
    if not cases:
        continue
    extract = ["# Read-only latest-hold extract for cases still held at end; no new solve."]
    for case in cases:
        last = case["lastHold"]
        final_ids = {int(m[1]) for m in re.finditer(r"(?:^|;)(\d+):HELD", case["finalIslands"])}
        assert last["island"] in final_ids, case
        extract += [f"SEED {case['seed']} extracted=latest-held-at-end",
                    f"  HELD tick={last['tick']} island={last['island']} committed={last['committed']} status={last['status']}"]
    extract += [f"SUMMARY extracted_cases={len(cases)} distribution={distribution}"]
    path = out / f"latest-held-{distribution}-extract.txt"
    path.write_text("\n".join(extract)+"\n", encoding="utf-8")
    subprocess.run([r"C:\Program Files\Git\bin\bash.exe", str(root.parent / "phase-ports-probes/hold-fuzz/classify.sh"), str(path)],
                   stdout=subprocess.DEVNULL, check=True)
    for line in Path(str(path)+".classes.txt").read_text().splitlines():
        name, fields = line.split("seeds=", 1)
        latest_classes.setdefault(name.strip(), []).extend(dict(distribution=distribution, seed=int(seed)) for seed in fields.split("list=", 1)[1].split())
result["latestHeldAtEndClasses"] = latest_classes
physical = []
for case in held:
    if not case["heldAtEnd"]:
        continue
    feeds = []
    for feed in re.finditer(r"(\d+):gen\[(\S+) (\d+)K (\d+)kPa\]@(-?\d+,-?\d+,-?\d+)", case["layout"]):
        feeds.append(dict(label=feed[2], kelvinRounded=int(feed[3]), kPaAbsoluteRounded=int(feed[4]), position=feed[5]))
    crystals = re.search(r"crystalKg=([\d.eE+-]+) crystalVessels=(\d+)", case["finalIslands"])
    physical.append(dict(distribution=case["distribution"], seed=case["seed"], feeds=feeds,
        endCrystalKgWorldTotal=float(crystals[1]) if crystals else None,
        endCrystalVesselsWorldTotal=int(crystals[2]) if crystals else None,
        solidTransitionInLatestRejections="Solid transport transition inside the step" in case["lastHold"]["status"],
        valveSetpointsKPa=[int(x) for x in re.findall(r"valve\[(\d+)kPa\]", case["layout"])],
        pumpSettings=[dict(m3s=float(q), referenceRiseKPa=int(p)) for q,p in re.findall(r"pump\[([\d.]+),(\d+)kPa\]",case["layout"])],
        sourceScope="Final whole-world layout; retained crystal total is not an identified held-island inventory"))
physical_result = dict(integration="cb89f23a", cases=physical,
    limits="Boundary labels round T/P; no flow direction, flash split, held-node T/P or crystal population was newly computed. Physical regimes in the review are hypotheses supported by these observations, not established hold causes.")
(out / "physics-observations-2026-10-24.json").write_text(json.dumps(physical_result, indent=2), encoding="utf-8")
(out / "classification-2026-10-24.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
print("FINAL_CASES", result["totalCases"], "WITH_HOLD", result["casesWithHold"], "HELD_AT_END", result["heldAtEnd"], "PUBLICATIONS", result["heldPublications"])
for cls, count in counts.most_common():
    print("CLASS", count, cls, [(case["distribution"], case["seed"]) for case in held if case["firstClass"] == cls])
for case in held:
    print("CASE", case["distribution"], case["seed"], "publications", case["heldPublications"], "heldAtEnd", case["heldAtEnd"], "FIRST", case["firstHold"]["status"].split("; reasons=")[0])
print("HISTORICAL_UNCHECKED", len(historical_unchecked), [(c["distribution"], c["seed"], c["firstClass"]) for c in historical_unchecked])
print("HISTORICAL_CLEAN_ON_RECHECK", [(c["distribution"], c["seed"]) for c in historical_clean_on_recheck])
print("LATEST_HELD_AT_END", latest_classes)
