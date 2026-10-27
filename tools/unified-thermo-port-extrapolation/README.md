# unified-thermo-port-extrapolation

Purpose: the probes and the rule-off proofs of the range-extrapolation work (owner rule of 2026-10-16: leaving a data
range never holds an island), WP1 to WP3 of `documentation/2026-09-24-coolprop-low-temperature/RANGE_EXTRAPOLATION_PLAN.md`,
on branch `claude/unified-thermo-port`: commits `333dd6e7` (seed streams fluid-only), `a169fa85` (WP1, WP2), `085676ea`
(WP3 notices), `90d68ca1` (gate a), on top of `1d08ef1a`. Batch `2026-09-24-coolprop-low-temperature`; report
`RANGE_EXTRAPOLATION_WP1_WP3.md` in that batch folder.

Nothing here is a gate and nothing here was ever tracked, so there is nothing to re-attach. Everything runs on the
Gradle-free index lane of `tools/unified-thermo-port-m3/scripts` (`index-compile.sh [tree]`, `index-run.sh <selectors>`),
with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (add `-Dcreatecheme.fluid.scheduler.verify=true` for the
checkpoint tests, as Gradle's `test` task does). Paths name the worktree `.claude/worktrees/coolprop-multiphase-thermo-37f6b0`.

| Path | What it is |
|---|---|
| `rule-off/run.sh` | The rule-off proofs: `bash run.sh all [base]` switches off, in a temporary index (the worktree is never touched), the Andrade slope below a liquid table, the ideal-gas and segment-crystal enthalpy continuations, the numerical box in `FluidThermodynamics.state` and the worker's notice reading, then runs RangeContinuationGateTest, CrystalReferenceTest#segments..., DataRangeContinuationTest#aPressureBelow... and FluidDataRangeIslandTest (all must fail); `bash run.sh cap [base]` removes only the viscosity cap and runs the cap test. Recompile the head afterwards. |
| `probes/LegacyPinPointProbe.java` | LegacyNetworkPathPinTest's digest point by point; run at two trees (`EXTRA_TESTS=<this file> index-compile.sh HEAD`, then on the branch) and diff the `PIN` lines: the re-pin's evidence (only states below the fits' 293.15 K, wet flashes below 273.16 K and the 450 K, 2 MPa wet mixture moved) |
| `probes/SlurrySeedProbe.java` | SlurryTankPumpTest's island before the mint: supplier seeds and the fixed point's junction start; showed the gassy suction junction (112.2 K, 1.3 % vapour) the solids-laden seed stream made |
| `probes/UvCrystalStartProbe.java`, `UvCrystalSingleProbe.java` | GasSolidEquilibriumTest's VAPOR_SOLID UV round trips (y 0.1, 150 K, 1 MPa): the start below the carried data that led to the start window |
| `probes/WetUvRoundTripProbe.java`, `PureWaterUvProbe.java` | FluidTpEquilibriumPhUvTest's wet round trips at 460 K and 1.4 MPa and pure water at 320 K: the EXTRAPOLATED skip and the isochoric-stiffness tolerance |

Probes compile with the tree's tests through `EXTRA_TESTS="<absolute probe path>"` and run with `index-run.sh <package.Class>`.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Probes and rule-off proofs of the range-extrapolation WP1 to WP3 (`333dd6e7`, `a169fa85`, `085676ea`, `90d68ca1`): legacy pin points, the slurry seed stream, crystal and wet UV round trips; `rule-off/run.sh` switches each continuation, the numerical box and the notice reading off in a temporary index (index lane, nothing tracked)

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### History

- Done 2026-10-16 (report `RANGE_EXTRAPOLATION_WP1_WP3.md`)
