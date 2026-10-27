"""Compare the selected full campaign with preserved reports, seed by seed."""
import json
from pathlib import Path
import re

from validate_sign_floor import DISTRIBUTIONS, HERE, OUT

HISTORICAL = HERE.parent / "cold430-validation" / "baseline-reports"
FORM1 = HERE.parent / "cold430-validation" / "campaign"


def read(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    classes = {}
    for line in Path(str(path) + ".classes.txt").read_text(encoding="utf-8").splitlines():
        name, fields = line.split("seeds=", 1)
        for seed in fields.split("list=", 1)[1].split():
            classes[int(seed)] = name.strip()
    holds = {int(m[1]): int(m[2]) for line in lines
             if (m := re.match(r"SEED (\d+).* holds=(\d+)", line))}
    summary = next(line for line in lines if line.startswith("SUMMARY"))
    return {"classes": classes, "holds": holds, "summary": summary}


def compare(base, candidate):
    keys = sorted(set(base["holds"]) & set(candidate["holds"]))
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
        "historicalSource": "Preserved c49f6654 reports; watchlist only, before the final mover-limit scope correction",
        "form1Source": "Preserved rejected targeted-bisection form; not a baseline",
        "distributions": {},
    }
    for distribution in DISTRIBUTIONS:
        candidate = read(OUT / f"full-candidate-{distribution}-report.txt")
        historical = read(HISTORICAL / f"full-candidate-{distribution}-report.txt")
        form1 = read(FORM1 / f"full-candidate-{distribution}-report.txt")
        entry = {
            "candidateSummary": candidate["summary"],
            "historicalToCandidate": compare(historical, candidate),
            "form1ToCandidate": compare(form1, candidate),
        }
        result["distributions"][distribution] = entry
        print(distribution, "historical", json.dumps(entry["historicalToCandidate"], sort_keys=True), flush=True)
        print(distribution, "form1", json.dumps(entry["form1ToCandidate"], sort_keys=True), flush=True)
    output = OUT / "full-comparison.json"
    output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print("WROTE", output, flush=True)


if __name__ == "__main__":
    run()
