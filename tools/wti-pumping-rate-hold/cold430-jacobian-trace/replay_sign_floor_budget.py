"""Replay new budget-class seeds alone, without diagnostics or solver counters."""
import argparse
import hashlib
import os

from validate_sign_floor import CANDIDATE_SHA256, HARNESS, JDK, OUT, SOLVER, WT, invoke, paths


def run(form, distribution, seeds, label):
    kind = "cold" if distribution == "cold-random" else "bundled"
    cp = [str(OUT / kind)]
    if form == "baseline":
        cp.append(str(OUT / "baseline"))
    else:
        actual = hashlib.sha256((WT / SOLVER).read_bytes()).hexdigest()
        assert actual == CANDIDATE_SHA256, f"candidate source changed: {actual}"
    cp += paths()
    report = OUT / f"{label}-{distribution}-report.txt"
    args = [JDK / "java.exe", "-Xshare:off", "-Xmx6g", "-Duser.language=en", "-Duser.country=US",
            f"-DholdFuzz.seeds={seeds}", "-DholdFuzz.threads=1",
            f"-DholdFuzz.cold={str(kind == 'cold').lower()}",
            f"-DholdFuzz.defaults={str(distribution == 'bundled-defaults').lower()}",
            f"-DholdFuzz.out={report}"]
    args += ["-cp", os.pathsep.join(cp), "M3Run", "com.wormzjl.icheme.runtime.fluid.HoldFuzzTest"]
    print(f"BUDGET_REPLAY {label} form={form} distribution={distribution} seeds={seeds} workers=1", flush=True)
    invoke(args, OUT / f"{label}-{distribution}-run.log")
    bash = os.environ.get("BASH", r"C:\Program Files\Git\bin\bash.exe")
    invoke([bash, HARNESS / "classify.sh", report], OUT / f"{label}-{distribution}-classify.log", timeout=30)
    for line in report.read_text(encoding="utf-8").splitlines():
        if line.startswith(("SEED ", "  HELD ", "  EXCEPTION ", "SUMMARY")):
            print(line, flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("form", choices=("baseline", "candidate"))
    parser.add_argument("distribution", choices=("cold-random", "bundled-random", "bundled-defaults"))
    parser.add_argument("seeds", help="A seed, comma-separated seeds, or an inclusive range")
    parser.add_argument("label")
    args = parser.parse_args()
    run(args.form, args.distribution, args.seeds, args.label)
