"""Compare cold-430 candidate and cb89f23a on the existing hold-fuzz harnesses, serially."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

HERE = Path(__file__).resolve().parent
WT = HERE.parents[2]
JDK = Path(r"C:\Program Files\Java\jdk-21.0.11\bin")
OUT = HERE / "campaign"
BASE = "cb89f23ac31f67933caeaf0fb5e11aa92f8612d4"
CLASS = "com.wormzjl.icheme.runtime.fluid.HoldFuzzTest"
DISTRIBUTIONS = ("cold-random", "bundled-random", "bundled-defaults")


def paths():
    classpath = WT / "build/cold430-validation/test-runtime-classpath.txt"
    return classpath.read_text(encoding="utf-8").strip().split(os.pathsep)


def invoke(args, log, timeout=275):
    with log.open("w", encoding="utf-8") as stream:
        try:
            result = subprocess.run([str(a) for a in args], cwd=WT, stdout=stream,
                                    stderr=subprocess.STDOUT, timeout=timeout)
        except subprocess.TimeoutExpired as exc:
            raise RuntimeError(f"timed out and stopped: {log}") from exc
    if result.returncode:
        raise RuntimeError(f"exit {result.returncode}: {log}\n{log.read_text(errors='replace')[-6000:]}")


def compile_sources():
    OUT.mkdir(parents=True, exist_ok=True)
    restoration = HERE / "negative-control/restoration.json"
    if restoration.exists():
        expected = json.loads(restoration.read_text(encoding="utf-8"))["candidateSha256"]
        source = WT / "src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java"
        assert hashlib.sha256(source.read_bytes()).hexdigest() == expected, "candidate source differs from focused validation"
    cp = paths()
    for kind in ("cold", "bundled"):
        dest = OUT / kind
        dest.mkdir(parents=True, exist_ok=True)
        harness = HERE / f"harness/hold-fuzz-{kind}/HoldFuzzTest.java"
        invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", os.pathsep.join(cp),
                "-sourcepath", WT / "src/test/java", "-d", dest, harness, HERE / "harness/M3Run.java"],
               OUT / f"compile-{kind}.log")
    sources = []
    for name in ("PassiveIntervalSolver", "PassiveStepSolver"):
        source = f"src/main/java/com/wormzjl/icheme/science/fluid/network/{name}.java"
        data = subprocess.check_output(["git", "show", f"{BASE}:{source}"], cwd=WT)
        dest = OUT / "baseline-src" / source
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
        sources.append(dest)
    dest = OUT / "baseline"
    dest.mkdir(parents=True, exist_ok=True)
    invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", os.pathsep.join(cp),
            "-d", dest, *sources], OUT / "compile-baseline.log")
    manifest = {
        "baseline": BASE,
        "worktree": str(WT),
        "candidateHead": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=WT, text=True).strip(),
        "candidateDiff": subprocess.check_output(["git", "diff", "--", "src/main/java", "src/test/java"], cwd=WT, text=True),
        "harnessSha256": {str(p.relative_to(HERE)): hashlib.sha256(p.read_bytes()).hexdigest()
                          for p in (HERE / "harness").rglob("*.java")},
    }
    (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print("Compiled existing cold/bundled harnesses and cb89f23a baseline solver overrides", flush=True)


def run(form, distribution, seeds, workers, label):
    kind = "cold" if distribution == "cold-random" else "bundled"
    cp = [str(OUT / kind)]
    if form == "baseline":
        cp.append(str(OUT / "baseline"))
    cp += paths()
    report = OUT / f"{label}-{distribution}-report.txt"
    args = [JDK / "java.exe", "-Xshare:off", "-Xmx6g", "-Duser.language=en", "-Duser.country=US",
            f"-DholdFuzz.seeds={seeds}", f"-DholdFuzz.threads={workers}", "-DholdFuzz.counters=true",
            f"-DholdFuzz.cold={str(kind == 'cold').lower()}",
            f"-DholdFuzz.defaults={str(distribution == 'bundled-defaults').lower()}",
            f"-DholdFuzz.out={report}", "-cp", os.pathsep.join(cp), "M3Run", CLASS]
    print(f"START {label} {distribution} seeds={seeds} workers={workers}", flush=True)
    invoke(args, OUT / f"{label}-{distribution}-run.log")
    bash = Path(r"C:\Program Files\Git\bin\bash.exe")
    invoke([bash, HERE / "harness/classify.sh", report], OUT / f"{label}-{distribution}-classify.log", timeout=30)
    for line in report.read_text(encoding="utf-8").splitlines():
        if line.startswith(("SUMMARY", "COUNTERS", "CLASS")):
            print(line, flush=True)


def report(label, distribution):
    path = OUT / f"{label}-{distribution}-report.txt"
    lines = path.read_text(encoding="utf-8").splitlines()
    summary = next(line for line in lines if line.startswith("SUMMARY"))
    counter = next(line for line in lines if line.startswith("COUNTERS"))
    classes = {}
    for line in Path(str(path) + ".classes.txt").read_text(encoding="utf-8").splitlines():
        name, fields = line.split("seeds=", 1)
        classes[name.strip()] = [int(n) for n in fields.split("list=", 1)[1].split()]
    holds = {int(m[1]): int(m[2]) for line in lines
             if (m := re.match(r"SEED (\d+).* holds=(\d+)", line))}
    return {"summary": summary,
            "counters": {k: int(v) for k, v in re.findall(r"(\w+)=(\d+)", counter)},
            "classes": classes, "heldPublications": holds}


def summarize(include_full=True):
    result = {"baseline": BASE, "candidate": "WORKTREE", "quick": {}, "full": {}}
    for distribution in DISTRIBUTIONS:
        base = report("quick-baseline", distribution)
        candidate = report("quick-candidate", distribution)
        by_seed = lambda r: {seed: name for name, seeds in r["classes"].items() for seed in seeds}
        a, b = by_seed(base), by_seed(candidate)
        changed = {seed: [a.get(seed), b.get(seed)] for seed in set(a) | set(b) if a.get(seed) != b.get(seed)}
        worse = {seed: [base["heldPublications"][seed], count, b.get(seed)]
                 for seed, count in candidate["heldPublications"].items()
                 if count > base["heldPublications"][seed]}
        delta = {name: candidate["counters"].get(name, 0) - base["counters"].get(name, 0)
                 for name in set(base["counters"]) | set(candidate["counters"])}
        result["quick"][distribution] = {
            "baseline": base, "candidate": candidate,
            "newlyHeldSeeds": sorted(set(b) - set(a)), "recoveredSeeds": sorted(set(a) - set(b)),
            "changedFirstHoldClasses": changed, "moreHeldPublications": worse, "counterDelta": delta,
        }
        print(distribution, "quick new=", result["quick"][distribution]["newlyHeldSeeds"],
              "recovered=", result["quick"][distribution]["recoveredSeeds"],
              "changed classes=", changed, "more publications=", worse, flush=True)
        if include_full:
            full = report("full-candidate", distribution)
            result["full"][distribution] = full
            print("FULL", full["summary"], flush=True)
            print("FULL classes", full["classes"], flush=True)
    result["scope"] = "Exact cb89f23a baseline comparison covers seeds 1-200; selected-form coverage is 1-1000."
    name = "campaign-summary.json" if include_full else "quick-summary.json"
    (OUT / name).write_text(json.dumps(result, indent=2), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("compile", "run", "summarize", "quick-summarize"))
    parser.add_argument("form", nargs="?", choices=("baseline", "candidate"))
    parser.add_argument("distribution", nargs="?", choices=DISTRIBUTIONS)
    parser.add_argument("seeds", nargs="?", default="1-200")
    parser.add_argument("workers", nargs="?", type=int, default=4)
    parser.add_argument("label", nargs="?")
    args = parser.parse_args()
    if args.phase == "compile":
        compile_sources()
    elif args.phase == "summarize":
        summarize(True)
    elif args.phase == "quick-summarize":
        summarize(False)
    else:
        assert args.form and args.distribution
        label = args.label or ("quick-" if args.seeds == "1-200" else "full-") + args.form
        run(args.form, args.distribution, args.seeds, args.workers, label)
