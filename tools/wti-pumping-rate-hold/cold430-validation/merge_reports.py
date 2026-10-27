"""Merge sequential HoldFuzzTest batches into one logical campaign report and classify it."""
import argparse
from pathlib import Path
import re

from validate import HERE, OUT, invoke


def merge(label, distribution, parts):
    blocks = []
    totals = {"seeds": 0, "clean": 0, "wallMs": 0, "seedMsSum": 0}
    counters = {}
    order = []
    threads = slices = None
    suffix = ""
    for part in parts:
        path = OUT / f"{part}-{distribution}-report.txt"
        current = None
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.startswith("SEED "):
                if current is not None:
                    blocks.append(current)
                current = [line]
            elif line.startswith("SUMMARY"):
                if current is not None:
                    blocks.append(current)
                    current = None
                match = re.fullmatch(r"SUMMARY seeds=(\d+) clean=(\d+) threads=(\d+) slices=(\d+) wallMs=(\d+) seedMsSum=(\d+)(.*)", line)
                assert match, line
                values = match.groups()
                for name, value in zip(("seeds", "clean", "wallMs", "seedMsSum"),
                                       (values[0], values[1], values[4], values[5])):
                    totals[name] += int(value)
                assert threads is None or threads == int(values[2])
                assert slices is None or slices == int(values[3])
                threads, slices, suffix = int(values[2]), int(values[3]), values[6]
            elif line.startswith("COUNTERS"):
                for token in line[len("COUNTERS "):].split():
                    name, value = token.split("=", 1)
                    if name not in counters:
                        order.append(name)
                        counters[name] = ""
                    if value:
                        counters[name] = str(int(counters[name] or 0) + int(value))
            elif current is not None:
                current.append(line)
        assert current is None, f"unterminated report: {path}"
    seeds = [int(block[0].split()[1]) for block in blocks]
    assert len(seeds) == totals["seeds"] and len(set(seeds)) == len(seeds)
    blocks.sort(key=lambda block: int(block[0].split()[1]))
    lines = [line for block in blocks for line in block]
    lines.append(f"SUMMARY seeds={totals['seeds']} clean={totals['clean']} threads={threads} slices={slices} wallMs={totals['wallMs']} seedMsSum={totals['seedMsSum']}{suffix}")
    lines.append("COUNTERS" + "".join(f" {name}={counters[name]}" for name in order))
    report = OUT / f"{label}-{distribution}-report.txt"
    report.write_text("\n".join(lines) + "\n", encoding="utf-8")
    bash = Path(r"C:\Program Files\Git\bin\bash.exe")
    invoke([bash, HERE / "harness/classify.sh", report], OUT / f"{label}-{distribution}-classify.log", timeout=30)
    print(lines[-2])
    print(lines[-1])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("label")
    parser.add_argument("distribution")
    parser.add_argument("parts", nargs="+")
    args = parser.parse_args()
    merge(args.label, args.distribution, args.parts)
