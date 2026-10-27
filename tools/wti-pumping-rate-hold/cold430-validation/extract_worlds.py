"""Extract selected existing HoldFuzzTest worlds without running a solver."""
from pathlib import Path
import re

from validate import HERE, OUT

REPORT = HERE / "baseline-reports"
SEEDS = {101, 220, 855, 907}
SOURCES = {
    "historical-c49": REPORT / "full-candidate-cold-random-report.txt",
    "selected-full": OUT / "full-candidate-cold-random-report.txt",
    "exact-quick-cb89": OUT / "quick-baseline-cold-random-report.txt",
    "exact-cohort-cb89": REPORT / "cohort-candidate-cold-random-report.txt",
}

blocks = []
for label, path in SOURCES.items():
    raw = path.read_text(encoding="utf-8")
    for block in re.split(r"(?=^SEED \d+ )", raw, flags=re.M):
        match = re.match(r"SEED (\d+) ", block)
        if match and int(match[1]) in SEEDS:
            blocks.append(f"===== {label} seed {match[1]} =====\n{block.strip()}")
output = OUT / "worse-worlds-extract.txt"
output.write_text("\n\n".join(blocks) + "\n", encoding="utf-8")
print(output)
