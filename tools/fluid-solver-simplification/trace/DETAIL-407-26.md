# Detail: seeds 407 and 26 (t40, 2026-10-18; the full first version of BUNDLED_HOLDS_TRACE.md)

Bundled random seeds 407 and 26 (E8, interval attempt limit): held at `573b050b`, clean at m11 `688f4cd7`. Everything ran on the Gradle-free javac lane from `git archive` exports, with the probes applied to the exports only, at most 4 workers, and no dev client running; the owner's geosim JVMs ran throughout. Material: `tools/fluid-solver-simplification/trace/` (README, lane pair minimizer, probes, `logs/t40-logs.tar.gz`). Code guide: no change (analysis only).

## One mechanism, both seeds

**Minimized worlds** (pair-minimal: held at `573b050b`, clean at `688f4cd7`; KEEP lists in the tool README). 407, 3 blocks: an empty 1 m3 tank (its nitrogen at 1 atm is the placeholder state of an empty bag, D54), one 0.05 m pipe, and a crude generator (Tia Juana light with methane) at 311 K and 316 kPa; held interval [1435,1535]. 26, 10 blocks: crude at 294 K and 311 kPa at y=0 feeds two tanks at y=-2 through five 0.05 m pipes, with a 0.02 m pipe to a dead-ended pump; held interval [1361,1461].

**Bisect.** Both worlds are clean at `688f4cd7` and hold identically at form A `6f1a524e`, at C, B, the limit 30 and K6 (`573b050b` changes no main code after K6). **The commit is form A.**

**Physics.**
- 407: liquid flows in at 58.6 to 53 kg/s. The cap is 168 kg/s, so r = 0.32 to 0.35 and the line is not saturated. The crude's methane flashes in the slack bag at 1 atm and dissolves again as the pressure rises: the bubble pressure is 139 kPa when the tank is full with 855 kg. The last bubble closes, then the liquid compresses. The trace gives K = 1.1 GPa (23 kPa for 2.1e-5 of the volume), so dP/dt = K Q/V = 70 MPa/s and the tank reaches 316 kPa within about 3 ms, as the flow stops.
- 407, steady state: liquid-full at 316 kPa, 177 kPa above its bubble pressure, no vapour, no flow. The baseline does this in 44 accepted and 17 rejected steps, a ramp from 139 to 312 kPa in 25 steps of 0.1 to 0.7 ms.
- 26: the tank fills at 41 kg/s and settles liquid-full at 327.8 kPa (311 kPa plus 2 m of crude, 16.8 kPa) with 862.5 kg and no flow. The baseline takes 60 accepted and 23 rejected steps.

**What the code does** (407, at t = 3.4227 s of the interval).
- A two-phase state holds a 21 mL bubble at 139.05 kPa: 2.6e-7 of the tank's moles. Pass 0 converges with the bubble shrinking, and D38 C's `vanishing` rule (below `VANISHING_SHARE` 1e-6 of the moles, and shrinking) folds it. Pass 1, liquid only, converges on the stretched liquid at 116.2 kPa (-16 %).
- At the next step, the converged point's check finds that liquid below its bubble pressure and proposes vapour. The vessel's closure gives back the same 21 mL at 139.05 kPa (+20 %).
- Every Newton converges (residual 1e-10 or less) and no row fails; what flips is the vessel's vapour split and its pressure. Both jumps are independent of the step size, so each is accepted only as an "algebraic jump", which needs a halved step (`step <= .51 previousStep`).
- Result: the step falls from 1.8e-3 to 1e-15 s over 259 fold/proposal cycles. The interval stops at the 1024-attempt cap (1005 accepted, 19 rejected) with 3.42 of 5 s done: HELD E8.
- 26 runs the same cycle: a 5.5 mL bubble at its bubble pressure, 112.1 kPa, against stretched liquid at 105.0 kPa (-6.3 % and +6.7 % against the 5 % cap). The step falls to 1e-10 s; 998 accepted, 26 rejected, 4.27 of 5 s done.

**How form A triggers it.** Form A's rule is not the cause.
- In 407 the smooth cap passes 1.2e-5 to 2.7e-5 less than the law. When the step grid reaches the last bubble the tank holds 0.017 kg less (854.638 against 854.655 kg), so the bubble is 1.67e-4 m3 instead of 1.47e-4.
- The under-filled step's stretch then reaches the slack bag's 101 325 Pa floor (-27 %), against the over-filled step's +28 %: within the jump test's factor 0.9, so it is accepted as a jump. At the baseline the same step gave -18 % against +37 %; it was refused and halved, and the bubble went on shrinking.
- In 26 the flows agree to four digits and the tank is 0.004 kg lighter (6e-6). In 407 the baseline goes through the same cycle (4 folds, 2 re-proposals) and leaves it.
- Systematic: all 32 supply pressures (300 to 331 kPa) and all 16 temperatures (300 to 325 K) of the 407 world hold at form A, and none holds at the baseline. No coarser form of form A would remove the cycle: the defect is in D38 C, which is older than the batch.

## Proposition (not implemented; probe `patches/proposition-vanishing-both-shares.patch` on a `573b050b` export)

**Rule.** D38 C (`FluidThermodynamics.vanishing`, the thermo stage, where the trace fold belongs): a shrinking phase is nothing only when it holds less than `VANISHING_SHARE` of its node's fluid **both by moles and by volume**. The direction of the test (shrinking) stays in moles.

**Physics.** A fold at fixed amounts, energy and volume moves a liquid node's pressure by about K times the folded volume share. Below 1e-6 of the volume that is at most 1.1 to 2.2 kPa (crude to water), 2 % or less at 1 atm and under the 5 % state cap; in a gas node it is about 1e-6 relative. A gas holds about 100 times its mole share in volume, so a mole test alone drops a bubble holding 2e-5 of the vessel. 1e-6 of the volume is form C's bag resolution `HOLDS_NOTHING` ("a millilitre in a cubic metre has no pressure of its own"), so a vessel gets one statement of "nothing". The rule deletes nothing and adds a volume read of about 5 lines; a liquid in a gas still folds by moles.

**Effect (probe).**
- The two worlds and the whole seeds 407 and 26 are clean.
- Quick check of seeds 1-200 on the same lane: bundled random 198, against HEAD's 197 (26 cleared; 116 and 157 hold as before); cold random 191, the same seeds as HEAD. Counters on the seeds clean in both runs: cold Newton iterations -3.1 %, residual evaluations -3.7 %, Jacobian builds -3.3 %, phase refusals -28 % (1348 to 965); bundled about -0.3 %.
- Of the 9 new bundled holds, 26 and 407 clear. 116, 358, 573 and 678 (E5), 988 (E6), and 301 and 788 (F at 4 workers) remain: other mechanisms.
- Suites against `k6s`: science-fluid-network 184/184, runtime-fluid 302/302. Two printed physics lines move: the oil-water overflow at slice 43 (P +0.18 Pa, gas out 7.5 to 10.3 g), and methane at 120 K (+0.012 K, -10 Pa, 2 substeps to 1).

**Risks.** A vapour in a liquid now stays down to about 1e-8 of the moles, so a closure that stops short keeps more small bubbles; form C's collapse rule still folds failed iterates. D38 C's recorded reason still holds: dry ice at 1e-9 to 3e-9 of the moles is about 3e-7 of the volume and folds. Not run: the defaults distribution, the GameTests, chain-100 and the 1000-seed campaign. **Rejected variant, volume alone:** It folds a liquid in a gas from about 1e-3 of the moles (a drained water phase): 4 tests fail (`PhasePortPriorityTest` x3, `PhysicalFluidTopologyTest` stack); bundled 197 with 788 cleared.

**Alternative in the controller stage.** Accept a vessel's change of phase set as a jump without halving. Time advances, but the pressure flips by ±16 % per step until the inflow closes the bubble, and a real jump at a phase onset passes unguarded. Not recommended.

## Owner's choices

1. Land the D38 C amendment (both shares) as one commit on `claude/fluid-solver-simplification`, followed by an Opus final test: full lane, G2, GameTests and the 1000-seed campaign against m12.
2. Or keep the branch and accept 26 and 407 (form A only triggers a cycle that exists at the baseline), or use the controller alternative (not recommended).
3. Next trace, if wanted: the smallest remaining new bundled holds, 678 (E5, 23 blocks) and 988 (E6, 25 blocks).
