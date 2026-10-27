# Trace of the smallest new bundled holds (t40, Opus, 2026-10-18/19)

New bundled random holds of the solver simplification, each pair-minimized (held at `573b050b`, clean at m11 `688f4cd7`), bisected over A `6f1a524e`, C, B, the limit 30 and K6, and traced on the Gradle-free javac lane. Probes went into `git archive` exports only, with at most 4 workers. All four worlds bisect to **form A**. Material: `tools/fluid-solver-simplification/trace/` (README, KEEP lists, probes, logs `t40-logs.tar.gz`, the full first version of this report `DETAIL-407-26.md`). Code guide: no change (analysis only).

## 407 and 26 (E8): decided, landed in `6c073298`

- **Physics.** An empty tank fills with crude below its line cap (r = 0.32). The last bubble closes and the liquid (K = 1.1 GPa) reaches the supply pressure within 3 ms, ending liquid-full at 316 kPa (407) or 327.8 kPa (26).
- **Code.** D38 C's mole-share fold dropped the last 21 mL bubble (2.6e-7 of the moles), leaving stretched liquid at 116 kPa. The next check proposed the bubble back at 139 kPa, and each jump halved the step, down to 1e-15 s. Form A only trimmed the delivered mass by 2e-5.
- **Decision.** The owner took the amendment: a shrinking phase is nothing only below 1e-6 both by moles and by volume. t41's final test 2 is green, and 26 and 407 are newly clean in the campaign.

## 678 (E5, pair-minimal 9 blocks): a receiver at its own bubble point

- **World.** Crude at 310 K and 332 kPa joins at tick 1700 through a 0.01 m line (cap 6.7 kg/s, flow 0.85 kg/s) into an empty tank at z=-2. That tank's bottom port drains 3 m down a 0.05 m line to a second empty tank. Held interval [1701,1704].
- **Physics.** The crude flashes in the upper slack bag at 1 atm (45 mL of vapour beside 60 mL of liquid). Saturated liquid drains at 0.76 kg/s into the lower bag, also at 1 atm and 310.40 K: it lands exactly at its bubble point, so no vapour forms there. Steady state, in about 34 online minutes: both tanks liquid-full at 332 kPa plus their heads (8 and 34 kPa), with no flow.
- **Code.** At t = 0.05 s the lower tank (88 mL of liquid) converges liquid-only. Its converged check proposes vapour, because the TP answer exactly at the bubble point holds some, and its own split seeds 5.8e-11 m3 (6.5e-7 of its fluid volume).
- The proposal is kept: the closure grows the vapour slightly (to 8.6e-11 m3), so neither `vanishing` test fires and form C's collapse rule does not either. The pass laid out with that vapour has no root: the ln(V/L) split is a near-null direction, and the line search stalls at 1.17e-6 and then 9.45e-7, at every step from 0.05 s to 1e-7 s. 21 rejections: E5 at tick 1704. This is t37's group K3 again.
- **Why form A.** At the baseline the upper bag's bottom port sends a two-phase stream (0.79 L of vapour beside 2.6 L of liquid by t = 1 s). The receiver is never liquid-only at its bubble point: 0 proposals at the baseline against 20 under form A.
- **Proposition (probe `patches/proposition-proposal-at-resolution.patch` on `6c073298`).** In `phaseCheck`'s D26 test, a proposed phase that the node's own split holds below `VANISHING_SHARE` both by moles and by volume is nothing, whatever its direction. The test is the amended fold's statement applied to a phase arriving, so a phase is nothing below the resolution whether it leaves or arrives.
  - It contains both direction tests, `vanishing(start,own)` and `vanishing(from,own)`, so they are deleted; it adds a 5-line `nothing(state)`.
  - Effect: the 9-block world and the whole seed 678 are clean. The quick check (seeds 1-200) gives the same held seeds as HEAD in both distributions (bundled 197, cold 191); cold residual evaluations -3.3 %, Newton iterations -1.7 %.
  - Suites 184/184 and 303/303. Three tiny printed moves: a 3.3e-10 water trace in a drained gas tank is no longer proposed back (P -0.15 Pa); a bottom-port liquid trace moves 4.968 to 4.958 mL.
  - Risk: a real phase born below 1e-6 of its node waits until its own split holds more.

## 988 (E6, pair-minimal 11 blocks): a choked series at its first step

- **World.** Methane at 261 K and 148.3 kPa joins at tick 1944 through three runs of 0.02 m bore (3.1e-4 m2) and a filter into an empty tank 6 m higher, at its bottom port.
- **Physics.** The gas chokes at the game's velocity cap (100 m/s) in the last run, 0.032 kg/s. The bag inflates at 0.043 m3/s, is full in 23 s, then reaches 148.3 kPa in about 11 s more. Steady state: 1.1 kg of methane at the supply pressure (the gas head is 60 Pa), with no flow.
- **Code.** The step's start puts law flows of 0.0519 kg/s on all three runs, 1.5 to 3.3 times their caps (saturation slopes 0.018 down to 2e-5). For 23 iterations the Newton crosses the knee of q_law (1+r^8)^(-1/8), with backtracks and the norm rising to 0.8. A stale-Jacobian chord then converges by 0.32 per iteration (fresh steps reduce by 0.15).
- The chord is refreshed only when a step reduces by less than 0.8, or after 8 steps. At iteration 30 the residual is 1.44e-8 against a tolerance of 1e-9, the same at every step from 0.05 s to 2.4e-5 s (the holdups barely move). Below that the junction rows stall at 0.0346. 20 rejections: E6.
- At the baseline the cap active set states the bottleneck run at its cap, and the step converges in 0 iterations.
- **Diagnostic.** A Newton limit of 40 clears 988 (and the new holds 301 and 788). t38 measured that limit at +6 % residual evaluations and 2 cold seeds lost: a budget, not a cure.
- **Pressure handle (form D).** For gas, form D's cap ∝ P is what the density cap ρ·100 m/s already gives: the cap ratio across the junction is 1.0835 against a pressure ratio of 1.0834, so dq/dlnP = q exists today. In 988's join a P-dependent gas cap changes neither the slopes in the law flow ((q_cap/q_law)^9) nor the chord's rate. It would change 988 only if form D also changed the saturation form or the start flows. Its handle matters for liquids, whose cap barely depends on P.
- **Proposition, not probed (it touches every booking).** Make the carried flow q the edge unknown, and state the hydraulic row as the law at the flow the cap inverts: drive = loss(q (1-(q/q_cap)^8)^(-1/8)).
  - Junction balances become linear in their unknowns, and a choked run is pinned by one stiff diagonal entry instead of a vanishing slope in every balance.
  - It deletes the law-flow carry beside the transported flow and the saturation read at each booking (`Equations.flow`). It adds the inverse law and a numerical box |q| < q_cap, as a rejected trial step.
  - Cost: about a day of work in `PassiveStepSolver`. It moves the references near a cap at round-off level; the checkpoint carries flows either way.

## Owner's choices

1. 678: land the proposal-at-resolution test (it deletes two direction tests) on the branch, then run the final lane.
2. 988: (a) the change of variable above, through t38 and alongside form D, which does not remove 988's mechanism for gas; (b) a Newton limit of 40 as a stopgap (+6 % residual evaluations); or (c) accept E6 at deep saturation for now.
3. Remaining new bundled holds: 116, 358 and 573 (E5, 24 to 27 blocks), neither bisected nor traced; 301 and 788 (F) clear with the limit of 40.
