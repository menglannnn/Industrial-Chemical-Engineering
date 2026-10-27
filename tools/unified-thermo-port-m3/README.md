# unified-thermo-port-m3

Purpose: the scripts, the rule-off proofs, the cold-package fuzz option, the probes and the archived logs of milestone M3
(crystals as solids, owner decisions D22 (5) and D23, `PRECIPITATED_SOLIDS_PLAN.md`) of the unified thermo port: branch
`claude/unified-thermo-port`, commits `acc775b6` (ThermoHold supertype), `675db18b` (crystal kind, one size per solid,
outlet mean, slush), `4f7ba970` (refresh closure), `92d39275` (condensed stand-in adoption), `34b80286` (rest lift),
`7b5f1f4c`, `aef76c89`, `1eeb8673` (tests), then `63926c52` (D30: the refresh's joint closure in the vessel's pass loop,
E1), `741ce603` (D28 A′: a failed iterate's fold is a trial layout and never writes A85) and `76e87740` (D29 A: a frost
answer's stand-in liquid takes its trace support from the answer's fluid), on top of M2's `a0f20364`. Batch `2026-09-24-coolprop-low-temperature`; report
`documentation/2026-09-24-coolprop-low-temperature/M3_CRYSTALS_AS_SOLIDS.md`; plan `MAIN_ADAPTATION_PLAN.md` (M3).

Nothing here is a gate and nothing here was ever tracked. Paths name the M3 worktree
(`.claude/worktrees/coolprop-multiphase-thermo-37f6b0`); edit `WT=` in a script to run it elsewhere.

## Contents

| Path | What it is |
|---|---|
| `fuzz.sh` | The quick check and the cold fuzz: the placement-sequence hold fuzz, 8 workers, one Gradle invocation per distribution through `tools/unified-thermo-port-m1/gradle-locked.sh`, classified by `tools/phase-ports-probes/hold-fuzz/classify.sh` of the main checkout. Bundled (M2's harness, the worktree's `tools/unified-thermo-port-m2/hold-fuzz`, by default): `bash fuzz.sh <label> [seeds]`; cold: `HARNESS=<this folder>/hold-fuzz DISTS=random EXTRA=-PholdFuzzCold=true bash fuzz.sh <label> [seeds]`; reports in the worktree's `build/m3-logs` (`OUT=`) |
| `hold-fuzz/` | M2's adapted harness with the cold option (`-PholdFuzzCold=true`): the pilot package as the network package (`PilotCryogenicTestCatalog`, every seed in its `MaterialRuntime` context), generators drawn as LNG (110-170 K, 0.5-5 % CO2, up to 8 % ethane and 1 % N2, 1.3-3 times methane's vapour pressure, at most 6 MPa) or a CO2-rich gas (50-95 % CO2 in CH4/N2, 1-6 MPa, warm enough to be a gas); the SEED line adds `crystalKg`/`crystalVessels` at the end; `-Pm3Trace=true` forwards `m3.trace` for the trace prints |
| `rule-off/` | The nine unit-gate proofs: `rules.txt` (gate, rule, main file, spec) and `run.sh [gate...]`, which switches a rule off with `edit.js`, compiles on the javac lane, runs that one `PrecipitatedSolidsTest` method and restores the source byte for byte |
| `scripts/javac-lane.sh`, `M3Run.java` | Single tests outside Gradle: `main` compiles the main sources changed since `a0f20364` against `build/classes/java/main`, `test <files>`, `run <Class[#method]>`. Valid only while `build/classes/java/main` holds the head's classes |
| `scripts/index-compile.sh`, `index-run.sh` | A commit's (or the staged index's) own main and test classes, compiled whole with javac (`EXTRA_TESTS=` adds probe sources), then run with M3Run: used to check each commit compiles and to bisect the moved printed lines |
| `scripts/hunks.js` | Picks hunks of a one-file diff for `git apply --cached --recount` (how the working tree was split into the eight commits) |
| `scripts/printed-moves.js` | The printed lines of two test-result folders that move once timings and object hashes are masked (M2's `printed-delta.js` masks, plus `elapsed ms=`, the V3 lines' `<n> s` and `@hash`): `node printed-moves.js <before dir> <after dir>` |
| `probes/M3CrystalProbeTest.java` | The development probe: closed N2+CO2 below frost, LNG with CO2, half-full pure LNG, slack dry ice, the phi scan. Probes run on the javac lane: `javac-lane.sh main`, `javac-lane.sh test <probe file>`, `javac-lane.sh run <package.Class>` |
| `probes/M3ColdRefreshProbeTest.java` | The cold fuzz's E1 holds (seeds 35, 72, 148): the vessel from its M3TRACE line, the refresh, the fluid-only closure, the drive, the engine's answer and a copy of the joint closure with its rows at the start and at its last iterate |
| `probes/M3ColdArrivalProbeTest.java` | The cold fuzz's new C holds (seeds 34, 75): the LNG generator's TP answers and one interval into an empty tank (run with the harness on the index lane, at the head and at `a0f20364`) |
| `probes/specs/` | Temporary edits, never committed (`git checkout --` afterwards): `refresh-trace-print`, `island-failure-print` (M3TRACE lines and stacks), `adopt-m2-form` (M2's crystal adoption on the M3 head, log 18) |
| `staging/` | The two patches staged against `63926c52`, with `README.md`; both applied on the owner's go (2026-10-16), so neither is to be applied again. `d28-option-a-prime.patch` (D28 A′, committed as `741ce603`): a failed iterate's fold reads the A85 record and never writes to it. `d29-option-a.patch` (D29 A, committed as `76e87740`): which trace-support paths change and why bundled worlds stay identical |
| `logs/m3-logs.tar.gz` | See below |

## Logs (`logs/m3-logs.tar.gz`)

`10` full `test` at `1eeb8673` with its results and the printed-line delta and diff against M2's log 46; `11` the
bundled quick check; `12` cold smoke (16 seeds); `13` the cold fuzz at `1eeb8673`; `14` the cold fuzz on the port head's
code (`a0f20364`, the tree checked out and restored); `15`-`17` single-seed traces (148; 35 and 72; 34) with their test
XML; `18` the cold fuzz with M2's adoption on the M3 head; `19` the cold probes; `rule-off/` the nine proofs;
`adoption-evidence/` the three adoption forms on the M3 tests (`three-run1`: the plan's held-out form, row E
THREE_PHASES; `bydiff-run1`: M2's form, the let-down at CO2's 90 K floor; `standin-run1`: the condensed stand-in, all
pass; `cdi-run1`, `rowe-dbg`, `RowEProbeTest.java` and the `spec-*.json` edits behind them); `commits/` the patches and
messages of the split; `bisect/README.txt` (the commit that moved each printed line); `gradle-compile-1.log`. After the
first report (D30): `20` the six M3 test classes on the javac lane with E1's change (27 of 27, every printed line as in
log 10); `21` cold seeds 35, 72 and 148 (`fuzz.sh 21-e1-seeds 35,72,148`, cold): 35 and 148 clean, 72 held where
`a0f20364` holds it (tick 1080, island 30). D28 A′ probed alone on `63926c52`: `22` STACK_PASS run alone through
`gradle-locked.sh` (it passes; the upper tank ends with 0.0 kg of liquid; the XML is copied from the console), and `23` cold
seeds 35, 72 and 148: 35 and 148 clean with the same crystal, and 72 held E6 at tick 1318, island 32. After the owner's
go, at `76e87740` (D30, D28 A′, D29 A), one lane, each once: `24` the bundled quick check (`fuzz.sh 24-quick-76e87740`:
every seed line, class and counter as log 11), `25` the cold quick check (179 clean), `26` the full `test --continue`
(1441 tests, 0 failures) with its results (`26-results/`), the printed-line delta and diff against log 10 (M2's scripts)
and `26-printed-moves-vs-10.txt` (`scripts/printed-moves.js`: only STACK_PASS moves beyond timings).

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: M3 of the unified thermo port (crystals as solids: the D23 refresh closure, one size per solid in each stock, slush at 0.5, solid islands at rest): the cold-package hold-fuzz option (LNG with CO2, CO2 let-downs), the rule-off specs of the nine unit gates, the stand-in and closure probes, staged patches, archived logs

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`; cold fuzz option; `staging/`

### History

- Done 2026-10-16 (`76e87740` on `claude/unified-thermo-port`, merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed); staged patches applied as `741ce603` and `76e87740`)
