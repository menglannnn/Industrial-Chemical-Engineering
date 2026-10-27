"""Compile and run the ignored cold-431 prefix diagnostic, serially and without counters."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

HERE = Path(__file__).resolve().parent
WT = HERE.parents[2]
JDK = Path(r"C:\Program Files\Java\jdk-21.0.11\bin")
WORK = HERE / "work"
EVIDENCE = HERE / "evidence"
BASE = "cb89f23ac31f67933caeaf0fb5e11aa92f8612d4"
SOLVER = "src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java"
INTEGRATOR = "src/main/java/com/wormzjl/icheme/science/fluid/network/SolidEventIntegrator.java"
PATCH = HERE.parent / "cold430-validation/candidate-armijo.patch"
CLASS = "com.wormzjl.icheme.runtime.fluid.HoldFuzzTest"
CANDIDATE_SHA256 = "d28a3b6d96565d61964213318d3d9d0ef12d7debd53b1a780c4cdf30c0f4350e"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def paths():
    classpath = WT / "build/cold430-validation/test-runtime-classpath.txt"
    return classpath.read_text(encoding="utf-8").strip().split(os.pathsep)


def invoke(args, log, timeout=275):
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open("w", encoding="utf-8") as stream:
        try:
            result = subprocess.run([str(a) for a in args], cwd=WT, stdout=stream,
                                    stderr=subprocess.STDOUT, timeout=timeout)
        except subprocess.TimeoutExpired as exc:
            raise RuntimeError(f"timed out and stopped: {log}") from exc
    if result.returncode:
        raise RuntimeError(f"exit {result.returncode}: {log}\n{log.read_text(errors='replace')[-6000:]}")


def compile_sources(zero_fix=False):
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=WT, text=True).strip()
    assert head == BASE, f"expected {BASE}, got {head}"
    diff = subprocess.check_output(["git", "diff", "--", "src/main/java", "src/test/java"], cwd=WT, text=True)
    if zero_fix:
        product = subprocess.check_output(["git", "diff", "--name-only", "--", "src/main/java"],
                                          cwd=WT, text=True).splitlines()
        assert product == [INTEGRATOR], f"only {INTEGRATOR} may override the product baseline: {product}"
        untracked = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "--", "src/main/java"],
                                            cwd=WT, text=True).strip()
        assert not untracked, f"untracked product Java is not allowed: {untracked}"
        solver_diff = subprocess.check_output(["git", "diff", "--", SOLVER], cwd=WT, text=True)
        assert not solver_diff, "the signed-zero experiment must leave PassiveStepSolver.java unchanged"
        fixed_integrator = (WT / INTEGRATOR).read_bytes()
    else:
        assert not diff, "tracked Java differs from the baseline"
        fixed_integrator = None
    if WORK.exists():
        shutil.rmtree(WORK)
    WORK.mkdir(parents=True)
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    suffix = "-zero-fix" if zero_fix else ""
    cp = paths()
    harness = WORK / "harness"
    harness.mkdir()
    invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", os.pathsep.join(cp),
            "-d", harness, HERE / "harness/HoldFuzzTest.java", HERE / "harness/M3Run.java"],
           EVIDENCE / f"compile-harness{suffix}.log")
    interval = SOLVER.replace("PassiveStepSolver", "PassiveIntervalSolver")
    baseline_data = subprocess.check_output(["git", "show", f"{BASE}:{SOLVER}"], cwd=WT)
    interval_data = subprocess.check_output(["git", "show", f"{BASE}:{interval}"], cwd=WT)
    for form in ("baseline", "candidate"):
        source = WORK / f"{form}-src" / SOLVER
        source.parent.mkdir(parents=True)
        source.write_bytes(baseline_data)
    relative_dest = (WORK / "candidate-src").relative_to(WT).as_posix()
    invoke(["git", "apply", f"--directory={relative_dest}", "--include=*PassiveStepSolver.java", PATCH],
           EVIDENCE / f"apply-candidate{suffix}.log", timeout=30)
    candidate = WORK / "candidate-src" / SOLVER
    assert sha(candidate) == CANDIDATE_SHA256, "candidate solver hash differs from the preserved form 2"
    original_hashes = {form: sha(WORK / f"{form}-src" / SOLVER) for form in ("baseline", "candidate")}
    if zero_fix:
        for form in ("baseline", "candidate"):
            (WORK / f"{form}-src" / INTEGRATOR).write_bytes(fixed_integrator)
    from instrument import instrument
    for form in ("baseline", "candidate"):
        source = WORK / f"{form}-src" / SOLVER
        interval_source = WORK / f"{form}-src" / interval
        interval_source.write_bytes(interval_data)
        instrument(source, interval_source)
        dest = WORK / form
        dest.mkdir()
        sources = [source, interval_source]
        if zero_fix:
            sources.append(WORK / f"{form}-src" / INTEGRATOR)
        invoke([JDK / "javac.exe", "-encoding", "UTF-8", "-proc:none", "-cp", os.pathsep.join(cp),
                "-d", dest] + sources, EVIDENCE / f"compile-{form}{suffix}.log")
    manifest = {
        "baseline": BASE,
        "worktree": str(WT),
        "scope": "cold-random seed 431, island 42, gated copied-source diagnostics; prefix only",
        "baselineSolverSha256": original_hashes["baseline"],
        "candidateSolverSha256": original_hashes["candidate"],
        "candidatePatchSha256": sha(PATCH),
        "instrumentedSha256": {form: {SOLVER: sha(WORK / f"{form}-src" / SOLVER),
                                      interval: sha(WORK / f"{form}-src" / interval)}
                               for form in ("baseline", "candidate")},
        "instrumentationSha256": sha(HERE / "instrument.py"),
        "harnessSha256": {p.name: sha(p) for p in (HERE / "harness").glob("*.java")},
        "classpathSha256": sha(WT / "build/cold430-validation/test-runtime-classpath.txt"),
        "counters": False,
        "workers": 1,
    }
    if zero_fix:
        class_file = Path(INTEGRATOR).relative_to("src/main/java").with_suffix(".class")
        class_hashes = {form: sha(WORK / form / class_file) for form in ("baseline", "candidate")}
        assert len(set(class_hashes.values())) == 1, "the two arms compiled different transport guards"
        manifest.update({
            "scope": manifest["scope"] + "; signed-zero transport guard override",
            "allowedProductOverride": INTEGRATOR,
            "passiveStepSolverWorkingTreeUnchanged": True,
            "fixedIntegratorSourceSha256": sha(WORK / "baseline-src" / INTEGRATOR),
            "fixedIntegratorClassSha256": class_hashes,
        })
    manifest_name = "manifest-zero-fix.json" if zero_fix else "manifest.json"
    (EVIDENCE / manifest_name).write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    invoke([JDK / "java.exe", "-version"], EVIDENCE / f"java-version{suffix}.log", timeout=30)
    print(f"Compiled the prefix harness and exact baseline/form-2 solver copies{suffix}", flush=True)


CHECKPOINT = EVIDENCE / "baseline-tick2158-checkpoint"


def run(form, label="prefix", attempts=4, capture=False, inject=False, zero_fix=False):
    if zero_fix:
        manifest = json.loads((EVIDENCE / "manifest-zero-fix.json").read_text(encoding="utf-8"))
        class_file = Path(INTEGRATOR).relative_to("src/main/java").with_suffix(".class")
        assert sha(WORK / form / class_file) == manifest["fixedIntegratorClassSha256"][form], \
            "the selected arm does not contain the recorded signed-zero guard"
        assert sha(WORK / f"{form}-src" / INTEGRATOR) == manifest["fixedIntegratorSourceSha256"], \
            "the selected arm's guard source differs from the manifest"
    cp = [str(WORK / "harness"), str(WORK / form)] + paths()
    report = EVIDENCE / f"{label}-{form}-report.txt"
    args = [JDK / "java.exe", "-Xshare:off", "-Xmx6g", "-Duser.language=en", "-Duser.country=US",
            "-DholdFuzz.seeds=431", "-DholdFuzz.threads=1", "-DholdFuzz.slices=120",
            "-DholdFuzz.counters=false", "-DholdFuzz.verbose=false", "-DholdFuzz.cold=true",
            "-DholdFuzz.traceIsland=42", f"-DholdFuzz.traceAttempts={attempts}", f"-DholdFuzz.form={form}"]
    if capture:
        args.append(f"-DholdFuzz.captureCheckpoint={CHECKPOINT}")
    if inject:
        args.append(f"-DholdFuzz.injectCheckpoint={CHECKPOINT}")
    args += [f"-DholdFuzz.out={report}", "-cp", os.pathsep.join(cp), "M3Run", CLASS]
    print(f"START cold-431 {label} {form}; one worker, no counters", flush=True)
    invoke(args, EVIDENCE / f"{label}-{form}-run.log")
    text = report.read_text(encoding="utf-8")
    assert f"TRACE_PREFIX_COMPLETE targetAttempts={attempts}" in text, f"prefix did not complete: {report}"
    assert "EXCEPTION" not in text, f"diagnostic exception: {report}"
    for line in text.splitlines():
        if line.startswith(("SEED", "SUMMARY")):
            print(line, flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("compile", "run", "all", "capture", "matched", "stage"))
    parser.add_argument("form", nargs="?", choices=("baseline", "candidate"))
    parser.add_argument("--label", help="evidence label for a stage run; defaults to stage")
    parser.add_argument("--zero-fix", action="store_true",
                        help="compile or run the explicitly scoped signed-zero guard experiment")
    args = parser.parse_args()
    if args.zero_fix and args.phase not in ("compile", "stage"):
        parser.error("--zero-fix is valid only for compile or stage; use distinct stage evidence labels")
    if args.phase in ("compile", "all"):
        compile_sources(args.zero_fix)
    if args.phase == "run":
        assert args.form
        run(args.form)
    elif args.phase == "all":
        run("baseline")
        run("candidate")
    elif args.phase == "capture":
        run("baseline", "capture", 1, capture=True)
    elif args.phase == "matched":
        run("baseline", "matched", 1, inject=True)
        run("candidate", "matched", 1, inject=True)
    elif args.phase == "stage":
        forms = (args.form,) if args.form else ("baseline", "candidate")
        for form in forms:
            run(form, args.label or "stage", 1, zero_fix=args.zero_fix)
