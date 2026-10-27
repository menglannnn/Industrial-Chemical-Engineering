"""Summarize Armijo-refined candidate against preserved exact-current matched reports; no solver run."""
import json
from pathlib import Path
import re

from validate import BASE, DISTRIBUTIONS, HERE, OUT

BASELINES = HERE / "baseline-reports"


def read(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    classes = {}
    for line in Path(str(path) + ".classes.txt").read_text(encoding="utf-8").splitlines():
        name, fields = line.split("seeds=", 1)
        classes[name.strip()] = [int(n) for n in fields.split("list=", 1)[1].split()]
    holds = {int(m[1]): int(m[2]) for line in lines
             if (m := re.match(r"SEED (\d+).* holds=(\d+)", line))}
    counter = next(line for line in lines if line.startswith("COUNTERS"))
    return {
        "summary": next(line for line in lines if line.startswith("SUMMARY")),
        "counters": {k: int(v) for k, v in re.findall(r"(\w+)=(\d+)", counter)},
        "classes": classes,
        "heldPublications": holds,
    }


def compare(base, candidate):
    by_seed = lambda r: {seed: name for name, seeds in r["classes"].items() for seed in seeds}
    a, b = by_seed(base), by_seed(candidate)
    return {
        "baseline": base,
        "candidate": candidate,
        "newlyHeldSeeds": sorted(set(b) - set(a)),
        "recoveredSeeds": sorted(set(a) - set(b)),
        "changedFirstHoldClasses": {seed: [a.get(seed), b.get(seed)] for seed in set(a) | set(b)
                                    if a.get(seed) != b.get(seed)},
        "moreHeldPublications": {seed: [base["heldPublications"][seed], count, b.get(seed)]
                                 for seed, count in candidate["heldPublications"].items()
                                 if count > base["heldPublications"][seed]},
        "counterDelta": {name: candidate["counters"].get(name, 0) - base["counters"].get(name, 0)
                         for name in set(base["counters"]) | set(candidate["counters"])},
    }


result = {
    "baseline": BASE,
    "candidate": "Armijo-refined WORKTREE",
    "candidateSha256": json.loads((HERE / "negative-control-armijo/restoration.json").read_text())["candidateSha256"],
    "scope": "Fresh exact-current quick baseline seeds 1-200 and preserved exact-current cohort baseline seeds 401-600; no refined-form full campaign.",
    "matched": {},
}
for scope, seeds, baseline_dir, baseline_label in (
        ("1-200", "1-200", OUT, "quick-baseline"),
        ("401-600", "401-600", BASELINES, "cohort-candidate")):
    candidate_label = "quick-armijo" if seeds == "1-200" else "cohort-armijo"
    result["matched"][scope] = {}
    for distribution in DISTRIBUTIONS:
        name = f"{distribution}-report.txt"
        comparison = compare(read(baseline_dir / f"{baseline_label}-{name}"), read(OUT / f"{candidate_label}-{name}"))
        result["matched"][scope][distribution] = comparison
        print(scope, distribution, comparison["candidate"]["summary"], flush=True)
        print("new=", comparison["newlyHeldSeeds"], "recovered=", comparison["recoveredSeeds"],
              "changed=", comparison["changedFirstHoldClasses"], "more=", comparison["moreHeldPublications"], flush=True)
output = OUT / "armijo-matched-summary.json"
output.write_text(json.dumps(result, indent=2), encoding="utf-8")
print("WROTE", output, flush=True)
