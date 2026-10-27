## Island hold F7: state-change rejections on a fed tank (2026-09-27)

- Author: Claude (Opus 5.5), worktree `.claude/worktrees/remove-agents-attribution-line-3d0c97`, branch `claude/phase-ports-compressor` at `fdadcd45`, owner's Windows machine, JDK 21.0.11. **No code changed**: the hold comes from two recorded designs working together (D11's per-step capacity and the backward-Euler state-change controller), and every remedy decides one of them, so the options are the owner's (fix-assumptions rule). Material: `tools/phase-ports-probes/holds2/` (README, the reproduction test, probes, option patches, logs numbered as cited).
- The defect: observation (b) of "GUI fixes F3-F5 (2026-09-27)": in the first client run a 1000 L N2 tank placed at 101.3 kPa, 25 C, with 0.05 m pipes on its top, bottom, north and east faces to 101325 Pa voids and a 101325 Pa water generator on its west face, logged 14 `status=HELD: Interval substep limit; no partial interval may commit: advanced=3.96875 of 5.0 s, accepted=508, rejected=516, reasons={... Backward-Euler state change #=511 ...}` lines, committed tick 1135 to 1785 in steps of 50 (`gui-fixes/logs/06`). The "second run" of the check was not a repeat of the rig: it reopened the same world at tick 6709 after the generator had been set to 130 kPa (the 130 kPa variant below fills the tank and never holds).

### 1. Reproduction (runtime path; `src/FedTankOpenOutletsTest.java`, fails on `fdadcd45`, `logs/05`)

The blocks are placed in the client log's order at its online ticks (20 TPS from the world load: tank 664, pipes 698-834, voids 868-970, generator 1007) and the island is run as the coordinator runs it: each placement is a topology event whose new island starts its slice ladder at one tick (1, 2, 4, ... 64, then the 100-tick cadence), a slice ends at the next event (its fence), a hold halves the slice on a fresh `RetainedSolver`. The run is the in-game one to the last digit: 14 holds at starts 1134, 1184, ... 1784, each `advanced=3.96875 of 5.0 s, accepted=508, rejected=516`, the last refused changes `0.08460994579887296`, `0.08545046943000091`, ..., the 13th ending on `Newton iteration limit at residual 4.67e-6`, the 14th `advanced=4.2157...`, as in the client log (whose ticks are one later: the test's ticks come from the log's wall-clock stamps; what matters is the 37 ticks between the last void and the generator, over which the tank vents 23 Pa into the bottom void, the head of the gas column).

Trigger (variant scan `src/FedTankOpenOutletsProbeTest.java`, `logs/03`; 140 slices after the last placement each):

| variant | holds | most attempts in a slice (of 1024) | final pool |
|---|---|---|---|
| in-game order, generator at 1007 | **14** (then locked at 15.6 ms) | 848 | 1.10 kg |
| generator at 1005, 1006, 1009, 1010, 1013, 1028 | 0 | 885-887 | 0.43 kg |
| generator at 1008, 1011, 1018 | 0 | 438-462 | 0.85 kg |
| generator and its pipe before the outlets | **67** (every 5 s slice from tick 1097 to the end; 2.5 s retries commit) | 646 | 0.28 kg |
| every block in one event | **19** | 832 | 0.54 kg |
| 0.01 m outlets | 0 | 145 | 0.30 kg |
| generator at 130 kPa | 0 (the tank fills, 713 kg) | 26 | 712 kg |

So the trigger is not a particular order: every variant with 0.05 m outlets and a 101.3 kPa feed runs at 43-87 % of the attempt limit or over it, and which one holds depends on where the start-up transient leaves the step (below).

### 2. Root cause, by numbers (`src/instr-f7-step-trace.patch`, `logs/02`, `04`)

**The vessel's inventory is a function of the step size, so the state-change controller refuses every step increase.**

- **The state.** The tank sinks below the voids (92.7 kPa at 1134, 91.6 kPa at rest), so its top and side outlets close (voids do not feed back) and the bottom line, whose water column falls two blocks to its void, can carry more than the feed. D11 (review "D11", section 2) pins the bottom port's water at its capacity `c = m_w,start / (N dt)`, `N = 2` (the top VAPOR and bottom LIQUID ends; BULK sides do not count), gas marginal. Gas can leave only through that port, so the tank settles exactly where the pinned water equals the feed and no gas is drawn: the pool is `m_w = N q dt`. In the held slice: `q` = 13.13 kg/s, `dt` = 7.8125 ms, `m_w` = 0.2052 kg = 2 x 13.13 x 0.0078125; the tank holds 1.2354 kg in all (1.03 kg of it gas).
- **The refusal.** A step `dt'` from that state moves the pool towards `N q dt'`: `dm = q (dt' - dt)`, `dm/m = q dt / m` for a doubling = 13.14 x 0.0078125 / 1.2354 = 0.0831; measured 0.0830 (the log's 0.0846-0.0863). The bottom flow in the refused step is exactly the halved capacity (6.59 kg/s = 0.2052 / (2 x 0.015625)). The pressure moves 0.09 %; the refusal is the vessel's mass (its water), not a vanishing phase (the measure is the total mass, of which the water is 17 %), not a junction (the island has none), not a physical transient (at rest feed = drain to 1e-14), not the step floor.
- **The lock.** Refused at 15.625 ms, halved to 7.8125 ms: accepted with a change of 3e-7 (the pool's fixed point), so the growth factor is 2 (`clamp(0.9 sqrt(cap/change), 0.5, 2)`), and 15.625 ms is tried again. Every accepted step is followed by one refusal: 5 s at 7.8125 ms is 640 accepted steps, 1280 attempts; the limit of 1024 ends at 508 accepted = 3.96875 s. The halved 2.5 s retry needs 320 + 326 attempts and commits, the next 5 s slice holds again: the committed tick creeps 50 ticks per hold.
- **Where the lock sits.** Once the pool is built at `dt`, a doubling is refused whenever `q dt / m > 0.05` (here `dt` > 4.7 ms), at any `dt` above that; every slice restarts at 1 s and halves down to the same power of two, so the lock is kept. The transient decides the level: 7.8 ms (the in-game run: 1280 attempts per 5 s), 15.6 ms (885), 31.25 ms (430, `logs/02`; there the retry after each refusal also fails once on `Newton iteration limit`, 107 of 217 refusals per slice, because the steady state sits on the D11 segment boundary, pinned water with zero gas). The worst case, `dt` just above `0.05 m / q`, costs `2 T q / (0.05 m)` attempts per slice: a hold is possible wherever a phase-port draw is capacity-bound in a vessel whose through-flow `q / m` exceeds about 5 per second (this rig: 11 per second).
- Neither piece is unintended on its own: D11 states that halving the step doubles the capacity (A27), and the controller's cap, growth law and halving are the BE plan's (section 2, review 8.4-8.8). Together they make the committed state depend on the step and lock the controller.

### 3. Options (measured; none implemented)

The 5 % cap, the Newton and conservation tolerances and the attempt limit are unchanged in all of them.

| option | change | F7 (both tests of `FedTankOpenOutletsTest`) | other effects (measured) |
|---|---|---|---|
| O1 (D11, model) | capacity `m / (N max(dt, tau0))` | `tau0` = 0.05 s: **still HELD** (ticks 5534 / 5497, `Substep refinement exhausted: Phase/device active-set cycle`), locked at 31 ms (430 attempts per slice) (`logs/06`). `tau0` = 5 s: passes, 30-42 attempts, pool 146 kg (`logs/07`) | `tau0` = 5 s fails 7 D11 fixtures (`PhasePortPriorityTest` 5, `PumpBottomPortDrainTest` 2; names in `logs/07`): a pinned draw can no longer take a phase to zero in one step. Rejected |
| **O2 (controller measure, recommended)** | a vessel's mass change over `max(m0, rho_ref V)`, `rho_ref` = 1000 kg/m3 (5 % of the vessel's liquid capacity per step, a 5 % level change); pressure measure unchanged | passes: 15 (in-game order) / 40 (generator first) attempts per slice at most; the pool settles at 70.08 kg (91.21 kPa) for every staged variant and 69.29 kg for the generator-first and all-at-once ones; the variant scan 0 holds in all 14 (`logs/08`, `13`) | fluid suites 475/478 (`logs/10`): the 33 junction lines **identical**, **MIXED_GAS_COST 286**; exact regression **0.000e+00** (`logs/12`); three re-baselines: `McpGameplayRegressionTest.belowSeaLevelNitrogenTankAcceptsWaterAfterAnIdlePeriodAndPressureEdit` (a recorded pressure 130299.51 -> 129958.70 Pa), `InletPhaseTest.refusedPumpsCarryNothingAndTheIslandsWaterLineIntegratesBitwise` (the water line against the line alone at 5 s, bitwise before, now 2.4e-13 relative: another vessel's pressure now sets some steps), `VentGatePolishTest.theBulkVentControlIsNeverRefusedByTheGate` (its premise "the run passes because refused points were polished" no longer arises: 0 polishes). With `rho_ref` = 100 kg/m3 (`logs/14`): F7 passes (20 / 40 attempts, pool 42 kg), only the `InletPhaseTest` difference remains of the three |
| O3 (controller law) | a state-change refusal scales the step by `clamp(0.9 sqrt(cap/change), 0.1, 0.9)` instead of halving; no growth on the step after it | passes but still locked (443 attempts per slice, pool 0.71 kg): the cost still grows with `q / m` (`logs/09`) | fluid suites (`logs/11`): a **new nonconvergence** (`SolidChainTransportTest`: 20 refusals at 0.0526, the algebraic-jump rule needs a halving), 11 junction lines moved, MIXED_GAS_COST 312. Rejected |

Not prototyped: crediting the step's inflow of a phase to its D11 capacity (a through-flow passes without a pool; changes D11's rows); splitting the capacity among the ends that draw (review "Island holds F1 and F2", T4): halves the pool, the lock condition is the same.

**Recommendation: O2.** It removes the lock by construction (a doubling is refused only when `q dt > 0.05 rho_ref V`, 50 kg per m3 of vessel per step), so its worst case is `2 T q / (0.05 rho_ref V)` attempts, below the limit for any feed under about 5 t/s per m3 of vessel; gas transients stay controlled by the pressure measure (a gas's mass moves with its pressure); junction lines and chain-100 do not move. What it costs: a capacity-bound pool follows the step the controller now takes (70 kg in this rig instead of 0.2-1.1 kg; it was step-dependent before too, 0.28-1.10 kg across the variants), and three tests to restate. Decisions it needs: `rho_ref` (1000 or 100 kg/m3) and the three re-baselines. The accepted fix brings `FedTankOpenOutletsTest` into `src/test` as its gate.

### 4. Gates

None run: no code changed. The option measurements above ran one Gradle invocation at a time on an idle machine.

### 5. Material

`tools/phase-ports-probes/holds2/`: `README.md`; `src/FedTankOpenOutletsTest.java` (reproduction and acceptance test), `src/FedTankOpenOutletsProbeTest.java` (variants, tracer), `src/instr-f7-step-trace.patch`, `src/option-o1-capacity-time-floor.patch`, `src/option-o2-mass-floor-liquid-capacity.patch`, `src/option-o3-error-scaled-reduction.patch`; `logs/01`-`14`.
