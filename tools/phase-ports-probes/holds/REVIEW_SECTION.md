## Island holds F1 and F2 (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/phase-ports-compressor`, branch `claude/phase-ports-compressor` at `82f2d2f0`, owner's Windows machine, JDK 21.0.11, uncommitted (the orchestrator commits). Material: `tools/phase-ports-probes/holds/` (README, patches, probes, logs numbered as cited).
- The defects: F1 and F2 of the in-game check (section "In-game check (2026-09-27)"): a pump on a tank's bottom face held its island with water left (`Interval substep limit`, last `Newton iteration limit at residual 1.60e-9` / `1.63e-9`); scenario 1's tank fed liquid-full with its outlets open held (`Substep refinement exhausted: Newton iteration limit at residual 1.147e-9`).

### 1. Reproduction (runtime path, fail on `82f2d2f0`)

Both build the in-game blocks with `PhysicalFluidTopology.compile` (world defaults: 1 m blocks, 0.05 m pipes, 0.01 m outlets, the registry's idle seed) and run consecutive 5 s jobs on one `RetainedSolver`, the committed interval handed over at each dispatch, as `IslandCoordinator` does.

| test | fixture | on `82f2d2f0` (`logs/15-baseline-new-tests-fail.txt`) |
|---|---|---|
| `PumpBottomPortDrainTest` (2 tests) | 1000 L N2 tank at 1 atm filled from a 130 kPa water generator until it rests (200.4 kg of water, 130000.0 Pa), generator removed, pump under it facing down (`Pump(target, 500 kPa)`), one pipe, 1 atm void; target 0.01 and 0.001 m3/s; must drain and end `INLET_WRONG_PHASE` with at most one slice of target volume left | 0.01: HELD at slice 3 (15 s), 53.1 kg left, `Interval substep limit ... accepted=504, rejected=520, reasons={Backward-Euler state change #=2, Newton iteration limit ...=518}, last=... residual 1.066e-9`; 0.001: HELD at slice 18 (90 s), 112.8 kg left, `accepted=505, rejected=519 ... residual 1.319e-9`. In game: 1.63e-9 / 1.60e-9, same keys |
| `LiquidFullTankOutletsTest` | the same tank with a 130 kPa water generator on its west face (0.05 m) and 0.01 m pipes on its top, east and bottom faces to 1 atm voids; must fill, run liquid-full and overflow steadily for 60 slices | full at slice 25; HELD at slice 53 (265 s), 996.04 kg, vapour share 1.5e-31, `Substep refinement exhausted: Newton iteration limit at residual 4.157e-10; active-set pass=0; reasons={Newton iteration limit ...=20}`. In game: 996.0 kg, 1.147e-9, same key (the tank's headspace below 1 % sets the 1e-10 tolerance) |

### 2. Root cause, by numbers (instrumented copy, `src/instr-holds-dump.patch`, `logs/01`-`03`)

**One mechanism for both: a vanishing vapour phase holding a nitrogen trace, whose water-saturation row gets a wrong finite-difference derivative.** Nitrogen does not dissolve in free water, so the nitrogen a water-full node keeps stays a vapour phase that decays geometrically and never to zero:

- F1: the pump's own junction (the block between the tank's LIQUID port and the pump) was minted from the island's first boundary, the tank, so it started with the tank's gas; the water flowing through washes it out by backward Euler, `n' = n (m/dt)/(m/dt + Q)`, about 1.8 decades per slice at 0.001 m3/s. At the failing step (0.01 m3/s, dt 0.235 s): junction 11168 mol water (201.2 kg), vapour share 7.0e-32, N2 6.5e-31 mol, water vapour 1.8e-32 mol.
- F2: the tank's last nitrogen, drawn by the top (VAPOR) port at its D11 capacity `m/(N dt)` with `N = 2` (the top and the bottom port), is halved at every step (3 steps per slice, 0.9 decades per slice). At the failure: 55288 mol water, vapour share 7.6e-32, N2 3.9e-30 mol, water vapour 9.7e-32 mol.

The failing row (every dump, every step size): the node's `ln(p_w / P_sat)` (F1 row 10 = the junction's; F2 row 4 = the tank's). Every other row of the island is at or below 1e-15 (F1) and 7e-13 (F2, the same node's partial-pressure closure). The residual of that row after 20 iterations: F1 9.8e-9 (dt 0.235 s), 2.3e-9 (0.039 s), 1.6e-9 (0.0098 s), 1.3e-9 (0.0025 s); F2 1.63e-7 at dt 0.25, 0.0625, 0.0156, 0.0039, 0.00098 s. It does not grow as the step shrinks, and neither the modes, the segments nor the phase set change (`pass=0`, one segment code), so neither class 4 (rate-form floor) nor a row discontinuity.

- **Not a rounding floor.** The row's measured floor (the residual change under random 1-2 ulp perturbations of every unknown): 2.4e-15..8.4e-15 (F1), 1.4e-15..5.8e-15 (F2), six orders below the stall.
- **Not the liquid-full stiffness.** F2's volume and pressure rows are at 1e-16; the tank's pressure is carried by the water.
- **The derivative.** `p_w = n_wv R T / V_gas` with `V_gas = n_N2 v_m`, so the row is `ln(n_wv / n_N2) + const`, and its derivative in the water-vapour unknown (scaled by the node's total amount) is `1/x`. That unknown is 1.6e-36 (F1) / 1.8e-36 (F2). The difference step was `1e-6 * max(1e-30, seed, |x|) = 1e-36`: the `1e-30` in `PhaseLayout.differenceScale` meant as a zero guard floors every trace below it, so the step is 0.6-1.1 of the unknown itself and the secant `ln(1 + h/x)/(h/x)` is 22-32 % below the tangent. The Newton overshoots that unknown by `1/k - 1` and the row alternates in sign (traces of a fresh-workspace re-solve of the failing pass, `logs/02`, `03`): F2, after its start-up, `-3.95e-4, 1.08e-4, -2.94e-5, 8.04e-6, -2.19e-6, 5.99e-7, -1.63e-7` (0.27 per iteration, iteration 20 at 1.63e-7 against the 1e-10 of a headspace below 1 %); F1 0.27, then 0.49 after a refresh as the trace shrank to 9.4e-37 (`h/x` = 1.06), and 0.06 at the larger trace of the earlier dump (where the fresh re-solve converged in 13 iterations and the solve on the forked chord preconditioner did not). 20 iterations then end a little above the tolerance at every step size, and the controller halves 500 times.
- Why the plain bottom pipe drained (scenario 1) and the pump did not: a pipe run collapses into one connection, so there is no junction between the tank and the void; the pump block owns a junction.
- Other suspects read in the dumps and ruled out: the D11 segment (edge 0 of F1: water marginal, frozen, never moves), the level head (490 Pa on the F1 tank, a constant of the row it enters), the pump (PUMP_TARGET, head -27.5 kPa, its target row 0.0, suction = the junction's bulk water).

**A second layer behind it (F2, seen once the first was fixed, `logs/04`, `06`).** At about 2e-150 mol of vapour, `FluidThermodynamics.acoustic` (the trace gas phase's velocity limit, which the D11 mixture cap of the vent reads) forms `mass * compliance` of order `n^2 * 4e-9`, which goes subnormal and at 2e-158 mol underflows to zero: the bound became garbage then infinite, and F2 held at slice 195 (vapour share 6.9e-160) with the vent edge's hydraulic row stalled at 8.3e-4 on line-search stalls.

### 3. Fix (pure numerical repairs, no model change; implemented)

| file | change | bitwise where |
|---|---|---|
| `science/fluid/solver/PhaseLayout.java` | `differenceStepScale`, `traceDifferenceScale`, `seedDifferenceScale`, `TRACE_DIFFERENCE_GUARD`: the difference step of an amount whose value and seed both lie below 1e-30 is taken relative to its own size (`own = max(|value|, seed)`; the 1e-30 guard only at zero). `differenceScale` (the floor the trace clamp is derived from) is unchanged; the single-node `fixedInventory` adapter uses the new scale | every amount whose value or seed is at least 1e-30 of its node (the old `max(1e-30, own)` is `own` there) |
| `science/fluid/network/PassiveStepSolver.java` | `Equations.seedDifferenceScales`, filled with the layout's seed scale for every amount column; `Equations.differenceScale` returns `PhaseLayout.traceDifferenceScale(max(seed, |value|))` for those, `max(floor, |value|)` for every other column | the same |
| `science/fluid/thermo/FluidThermodynamics.java` | `acoustic`: a product `M C` below `Double.MIN_NORMAL` is evaluated as `sqrt(V/M) sqrt(V/C)` | every product at or above `MIN_NORMAL` |

No tolerance, assertion, iteration budget or conservation check changed.

After the fix (`logs/11`): F1 at 0.01 m3/s drains to 3.30 kg in slice 3 and is `INLET_WRONG_PHASE` from slice 4 (flow 0.0); at 0.001 m3/s to 3.25 kg in slice 39, refused from slice 40; F2 runs full at slice 25 and overflows 60 slices with the tank's mass constant (`steady=60`), the top outlet drawing water. Recovered rejections left in these runs: F1 0.01 slice 0, 2 Newton limits (start-up); slice 4, 2 `Singular Newton Jacobian` as the pump is refused (the junction then has no open connection); state-change halvings.

### 4. Gates (each once, idle machine, logs in `tools/phase-ports-probes/holds/logs/`)

| # | command | result |
|---|---|---|
| G0 | the two new classes (`logs/11`) | 3/3 |
| G1 | `./gradlew --no-configuration-cache --no-build-cache test --rerun --tests com.wormzjl.createcheme.science.fluid.* --tests com.wormzjl.createcheme.runtime.fluid.* --console=plain --continue` (`logs/12`) | **475/475** in 105 classes (472 + 3); the 33 junction lines **identical** to `windows-lane/logs/01-junction-lines-win.txt`; **MIXED_GAS_COST 286** Newton solves |
| G1' | every printed fixture line of G1 against the same suite on `82f2d2f0` (`logs/15`, `16`; `src/StdoutCompare.java`, timings masked, map entries sorted) | 5574 lines, **0 differ** (the new tests excluded) |
| G2 | `./gradlew --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact --console=plain` (`logs/13`) | **0.000e+00** on state/moles, temperature, phase fraction and flow |
| G3 | the adjacent selection (`logs/14`) | **45/45** in 7 classes |

### 5. What remains: the bottom of the double range (owner decision)

The fix removes the holds seen in game and carries these traces from 1e-31 down to about 1e-300 of their node, but the traces still never reach zero, and near the bottom of the double range no difference step can serve them: the row's Jacobian entries, of order `1/x`, overflow the factorization at about 1e-303, and below `MIN_NORMAL` the trace's own amounts carry too few bits to resolve its ratio (`src/TraceVapourProbeTest.java`, `logs/05`: the phase's properties are exact down to 4e-304 mol, and the saturation row moves by 4.3e-9 at 4e-314 mol, above the 1e-10 tolerance). Measured on the fixed tree (`logs/17`; `src/LiquidFullTankLifetimeProbeTest.java`, `src/FedPumpLifetimeProbeTest.java`):

- F2 run on: HELD at slice 354 (1770 s, 1645 s of steady overflow), vapour share 1.4e-303, `Substep refinement exhausted: Matrix values must be finite`.
- A pumped water line whose pump junction was minted from a tank (F1's layout with a 130 kPa generator keeping the tank fed, 0.001 m3/s): HELD at slice 173 (865 s), junction nitrogen 4e-299 mol, `Interval substep limit ... Matrix values must be finite`. (With the generator as the island's first boundary the junction is minted as pure water and never holds a trace: 400 slices clean, `logs/07`.)

So a player's overflowing tank holds after about 27 minutes and such a pumped line after about 14. Any water line through a junction (pump, valve, filter, branch) minted from a gas-holding boundary meets it. Every remedy decides what a trace is, so the options are the owner's:

- **T1 (recommended): flush traces below a threshold.** At each accepted step's commit, a component whose amount in a vessel or junction is below `1e-24` of that node's total (the prototype's threshold) is removed and the node re-flashed at its T and P; the vanishing phase then vanishes. Prototype `src/option-t1-trace-flush.patch` (19 lines in `PassiveIntervalSolver`, removed amounts not booked). Measured on the fixed tree (`logs/18`, `19`): F2 run on, full at slice 25, nitrogen gone at slice 42, steady; the fed pump line 400 slices (2000 s) with 3 accepted / 0 rejected steps per slice; fluid suites 477/477 (both lifetime probes included), 33 junction lines identical, MIXED_GAS_COST 286, all 5574 printed fixture lines identical, exact regression 0.000e+00. Decisions it needs: the threshold (relative, or absolute near one molecule, 1.7e-24 mol) and whether the removed amount is booked (a boundary transfer or the node's outflow, keeping the ledger exact) or deleted (at most 1e-24 of the node per component and step, far inside `checkConservation`'s 1e-10 mol).
- **T2 (measured, rejected): amount-form equilibrium rows for a trace vapour** (rows weighted by the vapour's share below 1e-12, `src/option-t2-trace-phase-weight.patch`, measured with an earlier variant of the difference step floored at 1e-290): still holds, F2 at slice 348 (vapour share 3.5e-298) and the fed pump line at slice 169 on `No finite Jacobian trial` (`logs/09`): the Jacobian itself cannot be formed there, whatever the row weight. It also loosens the trace phase's equilibrium, which the tolerance rule forbids.
- **T3 (not prototyped): scale each trace unknown by its own seed.** Keeps the Jacobian O(1) down to `MIN_NORMAL`, a larger change to `PhaseLayout`'s encoding, and still needs T1 with a threshold of `MIN_NORMAL` below it (subnormal amounts cannot resolve their ratio).
- **T4 (F2 only, not measured): D11's capacity split among the ends whose segment draws the phase** (A21 changed), so the top port takes the whole gas cap in one step and the drained-seed path (A27) ends the phase. Leaves F1's junction washout.

### 6. Material

`tools/phase-ports-probes/holds/`: `README.md`; `src/instr-holds-dump.patch` (dump of the failing step with a per-row rounding floor, and a traced fresh re-solve; against the fixed tree, never commit), `src/option-t1-trace-flush.patch`, `src/option-t2-trace-phase-weight.patch`, the probes `TraceVapourProbeTest`, `FedPumpLifetimeProbeTest`, `LiquidFullTankLifetimeProbeTest` (copy into `src/test/java/com/wormzjl/createcheme/runtime/fluid/` to run, then delete), `StdoutCompare.java`; `logs/00`-`19`.
