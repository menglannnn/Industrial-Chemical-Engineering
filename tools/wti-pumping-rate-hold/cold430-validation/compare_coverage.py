"""Compare the selected full campaign with exact final coverage and the documented historical full baseline."""
import json
from pathlib import Path
import re

from validate import DISTRIBUTIONS, HERE, OUT

BASELINES = HERE / "baseline-reports"


def read(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    classes = {}
    for line in Path(str(path) + ".classes.txt").read_text(encoding="utf-8").splitlines():
        name, fields = line.split("seeds=", 1)
        for seed in fields.split("list=", 1)[1].split():
            classes[int(seed)] = name.strip()
    holds = {int(m[1]): int(m[2]) for line in lines
             if (m := re.match(r"SEED (\d+).* holds=(\d+)", line))}
    return {"classes": classes, "holds": holds}


def compare(base, candidate, seeds):
    keys = sorted(set(base["holds"]) & set(candidate["holds"]) & set(seeds))
    a = {seed: base["classes"].get(seed) for seed in keys}
    b = {seed: candidate["classes"].get(seed) for seed in keys}
    return {
        "cases": len(keys),
        "newlyHeld": sorted(seed for seed in keys if a[seed] is None and b[seed] is not None),
        "recovered": sorted(seed for seed in keys if a[seed] is not None and b[seed] is None),
        "changedFirstHoldClasses": {seed: [a[seed], b[seed]] for seed in keys if a[seed] != b[seed]},
        "moreHeldPublications": {seed: [base["holds"][seed], candidate["holds"][seed]]
                                  for seed in keys if candidate["holds"][seed] > base["holds"][seed]},
        "fewerHeldPublications": {seed: [base["holds"][seed], candidate["holds"][seed]]
                                   for seed in keys if candidate["holds"][seed] < base["holds"][seed]},
    }


def run():
    result = {
        "historicalFullSource": "c49f6654, before the accepted final mover-limit scope correction",
        "exactFinalCoverage": "cb89f23a: new quick-baseline seeds 1-200 and preserved cohort-candidate seeds 401-600",
        "outsideCoverage": "Historical c49f6654 full reports only; differences are watchlist evidence, not attribution to this candidate",
        "distributions": {},
    }
    for distribution in DISTRIBUTIONS:
        candidate = read(OUT / f"full-candidate-{distribution}-report.txt")
        historical = read(BASELINES / f"full-candidate-{distribution}-report.txt")
        quick = read(OUT / f"quick-baseline-{distribution}-report.txt")
        cohort = read(BASELINES / f"cohort-candidate-{distribution}-report.txt")
        exact = {"classes": quick["classes"] | cohort["classes"], "holds": quick["holds"] | cohort["holds"]}
        exact_seeds = set(range(1, 201)) | set(range(401, 601))
        outside = set(range(1, 1001)) - exact_seeds
        entry = {
            "historicalFullAllSeeds": compare(historical, candidate, range(1, 1001)),
            "exactFinalCoverage": compare(exact, candidate, exact_seeds),
            "historicalOutsideFinalCoverage": compare(historical, candidate, outside),
        }
        result["distributions"][distribution] = entry
        for scope, comparison in entry.items():
            print(distribution, scope, json.dumps(comparison, sort_keys=True), flush=True)
    output = OUT / "coverage-comparison.json"
    output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print("WROTE", output, flush=True)


if __name__ == "__main__":
    run()
