"""Run the repository's existing hold-fuzz distributions against baseline/candidate classpaths.

No sources are attached to Gradle or changed in a checkout. Compile the existing harnesses and
two original baseline solver classes into this tool's ignored validation directory. Run serially
with other solver measurements stopped: quick = 200 seeds/form/distribution, full = 1000 chosen.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess

HERE = Path(__file__).resolve().parent
MAIN = HERE.parent.parent
WT = Path(r"C:\Users\wormz\.codex\worktrees\pipeline-tool\CreateChemE")
JDK = Path(r"C:\Program Files\Java\jdk-21.0.11\bin")
CACHE = Path.home() / ".gradle/caches/modules-2/files-2.1"
OUT = HERE / "validation"
BASE = "232b0466"
CLASS = "com.wormzjl.icheme.runtime.fluid.HoldFuzzTest"


def paths():
    cp = (HERE / "classpath.txt").read_text().strip().split(";")[1:]
    cp += [str(WT / "build/classes/java/test"), str(WT / "build/resources/test")]
    for group in ("org.junit.jupiter", "org.junit.platform", "org.opentest4j", "org.apiguardian"):
        cp += [str(p) for p in (CACHE / group).rglob("*.jar") if "sources" not in p.name and "javadoc" not in p.name]
    cp += [str(p) for p in (WT / "build/moddev/artifacts").glob("neoforge-*-merged.jar")]
    cp += (WT / "build/moddev/fluidGameTestServerLegacyClasspath.txt").read_text().splitlines()
    return cp


def invoke(args, log):
    with log.open("w", encoding="utf-8") as stream:
        result = subprocess.run([str(a) for a in args], cwd=WT, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(f"exit {result.returncode}: {log}\n{log.read_text(errors='replace')[-6000:]}")


def compile_sources():
    OUT.mkdir(exist_ok=True)
    cp = paths()
    for kind in ("cold", "bundled"):
        dest = OUT / kind
        dest.mkdir(exist_ok=True)
        harness = MAIN / f"tools/unified-thermo-port-forms/hold-fuzz-{kind}/src/HoldFuzzTest.java"
        invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", ";".join(cp),
                "-sourcepath", WT / "src/test/java", "-d", dest, harness,
                MAIN / "tools/unified-thermo-port-m3/scripts/M3Run.java"], OUT / f"compile-{kind}.log")
    sources = []
    for name in ("PassiveIntervalSolver", "PassiveStepSolver"):
        source = f"src/main/java/com/wormzjl/icheme/science/fluid/network/{name}.java"
        data = subprocess.check_output(["git", "show", f"{BASE}:{source}"], cwd=WT)
        dest = OUT / "baseline-src" / source
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
        sources.append(dest)
    dest = OUT / "baseline"
    dest.mkdir(exist_ok=True)
    invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", ";".join(cp),
            "-d", dest, *sources], OUT / "compile-baseline.log")
    (OUT / "manifest.json").write_text(json.dumps({"baseline": BASE, "worktree": str(WT),
        "harnessRoot": str(MAIN / "tools/unified-thermo-port-forms"),
        "candidateDiff": subprocess.check_output(["git", "diff", "--", "src/main/java"], cwd=WT, text=True)}, indent=2))
    print("Compiled existing cold/bundled harnesses and baseline solver overrides", flush=True)


def run(label, form, distribution, seeds, workers):
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
            f"-DholdFuzz.out={report}", "-cp", ";".join(cp), "M3Run", CLASS]
    print(f"START {label} {distribution} seeds={seeds} workers={workers}", flush=True)
    invoke(args, OUT / f"{label}-{distribution}-run.log")
    bash = Path(r"C:\Program Files\Git\bin\bash.exe")
    invoke([bash, str(MAIN / "tools/phase-ports-probes/hold-fuzz/classify.sh"), str(report)],
           OUT / f"{label}-{distribution}-classify.log")
    for line in report.read_text().splitlines():
        if line.startswith(("SUMMARY", "COUNTERS", "CLASS")):
            print(line, flush=True)


def report(label, distribution):
    path = OUT / f"{label}-{distribution}-report.txt"
    lines = path.read_text().splitlines()
    summary = next(line for line in lines if line.startswith("SUMMARY"))
    counter = next(line for line in lines if line.startswith("COUNTERS"))
    classes = {}
    for line in Path(str(path) + ".classes.txt").read_text().splitlines():
        name, fields = line.split("seeds=", 1)
        classes[name.strip()] = [int(n) for n in fields.split("list=", 1)[1].split()]
    holds = {int(m[1]): int(m[2]) for line in lines
             if (m := re.match(r"SEED (\d+).* holds=(\d+)", line))}
    return dict(summary=summary, counters={k: int(v) for k, v in re.findall(r"(\w+)=(\d+)", counter)},
                classes=classes, heldPublications=holds)


def summarize():
    result = {"baseline": BASE, "candidate": "83e88bd0", "quick": {}, "full": {}}
    for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
        base = report("quick-baseline", distribution)
        candidate = report("quick-candidate", distribution)
        by_seed = lambda r: {seed: name for name, seeds in r["classes"].items() for seed in seeds}
        a, b = by_seed(base), by_seed(candidate)
        new = sorted(set(b) - set(a))
        changed = {seed: [a.get(seed), b.get(seed)] for seed in set(a) | set(b) if a.get(seed) != b.get(seed)}
        worse = {seed: [base["heldPublications"][seed], count, b.get(seed)]
                 for seed, count in candidate["heldPublications"].items() if count > base["heldPublications"][seed]}
        result["quick"][distribution] = dict(baseline=base, candidate=candidate,
            newlyHeldSeeds=new, changedFirstHoldClasses=changed, moreHeldPublications=worse)
        result["full"][distribution] = report("full-candidate", distribution)
        print(distribution, "quick new=", new, "changed classes=", changed, "more publications=", worse)
        print(result["full"][distribution]["summary"])
        print(result["full"][distribution]["classes"])
    result["scope"] = "Current baseline comparison covers seeds 1-200; full selected-form coverage is 1-1000."
    (OUT / "campaign-summary.json").write_text(json.dumps(result, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("compile", "quick", "full", "baseline-gates", "summary", "followup"))
    args = parser.parse_args()
    if args.phase == "compile":
        compile_sources()
    elif args.phase == "summary":
        summarize()
    elif args.phase == "followup":
        # The full chosen form found bundled seed 241 outside the initial comparison. Check its
        # 200-seed cohort on every baseline distribution, never replay a worse Newton seed alone.
        for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
            run("cohort-baseline", "baseline", distribution, "201-400", 4)
        # Full eight-worker cold 244 / bundled 6 reached deadlines absent in their matched
        # baseline cohorts. Batch these C/F replays alone; no other worse seed is replayed.
        run("alone-candidate", "candidate", "cold-random", "244", 1)
        run("alone-candidate", "candidate", "bundled-random", "6", 1)
    elif args.phase == "baseline-gates":
        selectors = [
            "runtime.fluid.FluidCheckpointCodecTest#portsACompressorAndARefusedMoverRoundTripExactly",
            "runtime.fluid.FluidPacketCodecTest#theProtocolIsFluidSixWithDistinctStaticAndLivePayloads",
            "science.equipment.ShapeRasterizerTest#aDrumsFluidNodeIsAShellCell",
            "science.fluid.DeterministicMathSourceTest#fluidSourcesCallNoNonCorrectlyRoundedMathFunction",
            "science.equipment.ProcessUnitTest#everyNozzleNeckAtItsLargestStandsClear",
        ]
        cp = [str(OUT / "cold"), str(OUT / "baseline"), *paths()]
        log = OUT / "baseline-existing-failures.log"
        with log.open("w", encoding="utf-8") as stream:
            result = subprocess.run([str(JDK / "java.exe"), "-Xmx6g", "-cp", ";".join(cp), "M3Run",
                *[f"com.wormzjl.icheme.{s}" for s in selectors]], cwd=WT, stdout=stream, stderr=subprocess.STDOUT)
        output = log.read_text()
        print("\n".join(line[:250] for line in output.splitlines() if line.startswith("M3RUN ") and " at " not in line))
        if result.returncode != 1 or "found=5 succeeded=0 failed=5" not in output or "ClassNotFoundException" in output:
            raise RuntimeError(f"Unexpected baseline result: {log}")
    else:
        forms = ("baseline", "candidate") if args.phase == "quick" else ("candidate",)
        for form in forms:
            for distribution in ("cold-random", "bundled-random", "bundled-defaults"):
                run(f"{args.phase}-{form}", form, distribution,
                    "1-200" if args.phase == "quick" else "1-1000", 4 if args.phase == "quick" else 8)
