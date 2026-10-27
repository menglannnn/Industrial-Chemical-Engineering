# unified-thermo-port-m3a

Purpose: the probes, lane scripts and archived logs of M3a, the D21 follow-ups of the unified thermo port (owner decision
of 2026-10-15: theta = 1e-5; the inner UV search takes a secant only between answers of one classification). Batch
`2026-09-24-coolprop-low-temperature`; report `documentation/2026-09-24-coolprop-low-temperature/M3A_D21_FOLLOWUPS.md`;
commit `cc1a39a1` on `claude/unified-thermo-port` (worktree `.claude/worktrees/coolprop-multiphase-thermo-37f6b0`), on
`8a06b22f`.

Nothing here is a gate and nothing here was ever tracked; no code left a tracked path, so there is nothing to re-attach.
Gradle ran through `tools/unified-thermo-port-m1/gradle-locked.sh` (one run at a time, `-Xshare:off`, `--offline`); the
lane is M1's (`tools/unified-thermo-port-m1/lane/`). No timing anywhere: costs are TP equilibria and kernel calls.

## Contents

| Path | What it is |
|---|---|
| `probes/M3aProbe.java` | Modes: `ph` (PH at 101.325 kPa, f = 1e-6 to 2e-5, with the 2e-6 regression point), `uvrow` (row A's vessel, f = 0 to 2e-5), `triple` (42 UV targets at CO2's triple point with 2e-5 to 1e-3 of N2 or CH4, the inner-stall stress set), `grid` (G5F6's 24 UV round trips), `deep` (deep deposition, f = 2e-6 to 1e-5), `all` (the first four) |
| `probes/run.sh` | `run.sh <label> <src-dir> <main-classes> <res-main> <mode> [trace]`: compiles `<src-dir>`'s `FluidTpEquilibrium.java` and `EquilibriumResult.java` over the compiled classes into `/tmp/m3a/var-<label>` (with innerUv trace prints when `trace`), then runs the probe |
| `probes/trace-inner.js` | The innerUv instrumentation, on with `-Dm3a.trace=1`: `IUV-START`, one `IUV` line per TP answer (classification, residual, slope used, stored slope), `IUV-STALL` |
| `probes/inner-stats.js` | Per-case summary of a traced run: inner searches, inner TP answers, the longest one, stalls |
| `probes/compare.js` | Case-by-case comparison of probe outputs (status, T, P, crystal, TP equilibria / kernel calls) with totals per mode |
| `probes/make-variants.js` | The three inner-slope forms over `FluidTpEquilibrium.java` at `8a06b22f`: V1 compare classifications (chosen, = `cc1a39a1`), V2 reset the stored slope to 1.0 at each classification change, V3 the stored slope used only on its own classification. Theta does not act on the `triple` and `grid` sets |
| `scripts/lane-families.sh` | G4F1, G4F2, G5F1-G5F6 and `NearPureCrystalStepTest` (the D21 rows) through M1's lane on given class directories |
| `scripts/lane-fluid-uv.sh` | The fluid-only UV classes (`FluidTpEquilibriumPhUvTest`, `FluidTpEquilibriumTest`, G3F8, G3F9) and `GasSolidEquilibriumTest` through the lane; an optional first class directory (a traced build) |
| `logs/m3a-logs.tar.gz` | Gradle logs (01 compile at `8a06b22f`; 02 `science.thermo.*` and `science.material.*` on the final code, with a per-class summary), lane outputs at `8a06b22f` and on the final code with the `cmp.js` output, probe outputs (base = `8a06b22f`, V0 = theta only, V1-V3, final), innerUv traces, the variant comparison |

## How to run

From Git Bash, after a Gradle compile of the worktree (`gradle-locked.sh <log> testClasses`); scratch goes to `/tmp/m3a`.

- Probe: `probes/run.sh final <dir holding FluidTpEquilibrium.java and EquilibriumResult.java> <WT>/build/classes/java/main <WT>/build/resources/main all`;
  `deep` for the deep-deposition rows; `triple` with `trace`, then `node probes/inner-stats.js <output>` for the stalls.
- Families: `scripts/lane-families.sh <main> <res-main> <test> <res-test> > out.txt`, then
  `node ../unified-thermo-port-m1/lane/cmp.js before.txt out.txt`.
- Forms: `node probes/make-variants.js <FluidTpEquilibrium.java at 8a06b22f> <out-dir>` writes V1, V2 and V3; copy
  `EquilibriumResult.java` beside each and run it with `run.sh`; compare with `node probes/compare.js a.txt b.txt ...`.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: M3a of the unified thermo port (D21 follow-ups: theta 1e-5, inner UV secant within one classification): the near-pure and triple-point probes, the secant-form comparison, lane scripts, archived logs

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`; `probes/`

### History

- Done 2026-10-16 (`cc1a39a1` on `claude/unified-thermo-port`, merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed))
