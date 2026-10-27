"""Summarize first/last recorded holds for selected existing worlds; no solver run."""
import json
import re

from validate import OUT
from extract_worlds import SEEDS, SOURCES

result = {}
for label, path in SOURCES.items():
    raw = path.read_text(encoding="utf-8")
    for block in re.split(r"(?=^SEED \d+ )", raw, flags=re.M):
        match = re.match(r"SEED (\d+).* holds=(\d+)", block)
        if not match or int(match[1]) not in SEEDS:
            continue
        seed = int(match[1])
        holds = [{"tick": int(m[1]), "island": int(m[2]), "committed": int(m[3]), "status": m[4]}
                 for m in re.finditer(r"^  HELD tick=(\d+) island=(\d+) committed=(\d+) class=\S+ status=(.*)$", block, flags=re.M)]
        assert len(holds) == int(match[2])
        result[f"{label}:{seed}"] = {
            "header": block.splitlines()[0],
            "ticks": [hold["tick"] for hold in holds],
            "first": holds[0] if holds else None,
            "last": holds[-1] if holds else None,
        }
output = OUT / "worse-worlds-summary.json"
output.write_text(json.dumps(result, indent=2), encoding="utf-8")
print(output)
