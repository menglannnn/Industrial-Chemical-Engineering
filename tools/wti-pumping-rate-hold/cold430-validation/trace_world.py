"""Run one permitted diagnostic world with optional solver trace classes; never for cost comparison."""
import argparse
import os
from pathlib import Path

from validate import HERE, OUT, invoke, paths


def run(form, distribution, seed, label):
    kind = "cold" if distribution == "cold-random" else "bundled"
    cp = [str(OUT / kind)]
    if form == "baseline":
        cp.append(str(OUT / "baseline"))
    elif form == "trace":
        cp.append(str(OUT / "trace-classes"))
    cp += paths()
    report = OUT / f"{label}-{distribution}-report.txt"
    args = [Path(r"C:\Program Files\Java\jdk-21.0.11\bin\java.exe"),
            "-Xshare:off", "-Xmx6g", "-Duser.language=en", "-Duser.country=US",
            f"-DholdFuzz.seeds={seed}", "-DholdFuzz.threads=1", "-DholdFuzz.verbose=true",
            f"-DholdFuzz.cold={str(kind == 'cold').lower()}",
            f"-DholdFuzz.defaults={str(distribution == 'bundled-defaults').lower()}",
            f"-DholdFuzz.out={report}"]
    if form == "trace":
        args.append("-Dcold430.validation.trace=true")
    args += ["-cp", os.pathsep.join(cp), "M3Run", "com.wormzjl.icheme.runtime.fluid.HoldFuzzTest"]
    print(f"DIAGNOSTIC {label} form={form} distribution={distribution} seed={seed} workers=1", flush=True)
    invoke(args, OUT / f"{label}-{distribution}-run.log")
    bash = Path(r"C:\Program Files\Git\bin\bash.exe")
    invoke([bash, HERE / "harness/classify.sh", report], OUT / f"{label}-{distribution}-classify.log", timeout=30)
    for line in report.read_text(encoding="utf-8").splitlines():
        if line.startswith(("SEED ", "  HELD ", "SUMMARY")):
            print(line, flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("form", choices=("baseline", "candidate", "trace"))
    parser.add_argument("distribution")
    parser.add_argument("seed")
    parser.add_argument("label")
    args = parser.parse_args()
    run(args.form, args.distribution, args.seed, args.label)
