"""Summarize gated segment and transition lines from one cold-431 trace log."""
from pathlib import Path
import re
import sys


def value(line, name):
    match = re.search(rf"(?:^| ){re.escape(name)}=([^ ]+)", line)
    return match.group(1) if match else "?"


def main(path):
    segments = []
    current = None
    targeted = 0
    for raw in Path(path).read_text(encoding="utf-8").splitlines():
        line = raw.rstrip("\r")
        if line.startswith(("B_TARGETED", "B_TARGET_SWEEP")):
            targeted += 1
        if line.startswith("I_RUN_BEGIN"):
            current = {"duration": value(line, "duration"), "attempts": 0, "accepted": 0,
                       "rejected": 0, "transitions": []}
            segments.append(current)
        elif current is None:
            continue
        elif line.startswith("I_ATTEMPT_BEGIN"):
            current["attempts"] += 1
        elif line.startswith("I_ACCEPT"):
            current["accepted"] += 1
        elif line.startswith("I_REJECT"):
            current["rejected"] += 1
        elif line.startswith("I_TRANSITION"):
            current["transitions"].append({
                "edge": value(line, "edge"), "pipe": value(line, "pipe"),
                "step": value(line, "step"), "velocity": line.split("velocity=")[-1].split(";")[0]
                if "velocity=" in line else "?"})
        elif line.startswith("TRACE_RESULT"):
            print(line)
    print(f"targetedBalanceLines={targeted} segments={len(segments)}")
    for number, segment in enumerate(segments, 1):
        transitions = segment["transitions"]
        zero = sum(t["velocity"] in ("0.0", "-0.0") for t in transitions)
        final = transitions[-1] if transitions else {"edge": "none", "pipe": "none", "step": "none"}
        print(f"segment={number} duration={segment['duration']} attempts={segment['attempts']} "
              f"accepted={segment['accepted']} rejected={segment['rejected']} transitions={len(transitions)} "
              f"zeroVelocity={zero} finalEdge={final['edge']} finalPipe={final['pipe']} finalStep={final['step']}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: python summarize.py <trace-log>")
    main(sys.argv[1])
