# OD4 holds: bundled 145, 889 (rounds 1-2) and the D34 upper tank (round 3), 2026-10-20 (Opus)

Rounds 1-2 committed as `f983306b` and `8fd61a13` (logs `tools/new-units/logs/od4-holds-r2-2026-10-20.tar.gz`). Round 3 is on `48546bb7`; it ends in the owner's E5 decision, implemented uncommitted. Its logs are in `tools/new-units/logs/od4-holds-r3-2026-10-20.tar.gz`, its probes and prototypes in `tools/new-units/od4-holds/r3/` (main checkout).

## Rounds 1-2 (kept)
- 145: the cycle key carries each pass's static column ends (`PassiveStepSolver.PassKey`). The valve ends OPEN at 12.56 kg/s.
- 889: the arriving-phase seed of a junction is the fluid-only PH split of the buffer's own enthalpy plus its deliveries (`arrivingPhases`). It replaces the TP flash, which left out 2.3 MJ/kg of latent heat. Cold 583 and 743 and bundled 354 are clean.
## Round 3: the D34 gate (minimized bundled 322), physics
- Upper tank: 1 m3 at 429.2 kPa and 362.7 K. It holds 142.9 mol of gas (99.999 % methane, 1.2e-3 mol of light crude vapour). It is near its dew point: each step, the methane fed at 357 K condenses 2.2e-4 mol (0.077 mL, 98 % pc06-pc12, 1.7 % methane).
- That is 1.5e-6 of the tank's moles but 7.7e-8 of its volume. The drop line is 1 m of 50 mm (2 L) over a 2 m drop. The lower tank is a dead end at 437 K.
- Real tank: the condensate runs down the gas-filled line as a film within about a second, with an equal volume of gas coming up. The line stays a 45 Pa gas column and the upper tank stays dry, which is what the gate asserts. Nothing is singular.
## Round 3: what the code does
- 0.9.2 refuses the proposed condensate every step (D39 B), and the tank stays dry. Under any perturbation, one proposal's pass converges and a step is accepted wet (3.6e-8 m3). From then on, every wet-start step fails at every dt above 3.7e-4 s:
  - at 1.8e-4 s a pass converges, the vanishing fold puts the liquid back into the gas, and the next check proposes 7.7e-8 m3 again;
  - the island steps at 2-4e-4 s and holds F (wall deadline) at 4497.
- The four rows (node0+29..32) are the equilibrium rows of pc02-pc05. All 12 equilibrium rows of the tank carry the same residual (1e-5 to 1e-3): the splits disagree uniformly with ln(nV/nL).
- The Newton direction is a near-null mode: scale the whole trace liquid, change the drop-line flow by -m_L/dt per unit, and let the lower tank's T and P follow. Measured gains are 3 100 to 17 000 (splits -0.139 for a residual of 4.5e-5; -1.66 for 1.1e-3).
- Its only restoring rows are sub-pascal on the 4.3e5 Pa scale of the hydraulic row:
  - the D34 head of the trace standing in the line, 8.6e6 Pa/m3, so 0.3 to 0.7 Pa;
  - a stiff liquid drain: 0.135 kg/s per Pa, laminar, so 1 300 /s;
  - the lower tank's response.
- Along the mode the equilibrium rows curve as lambda^2/2 (the single-phase heavies are linear amounts, the light shares are log ratios). The linear model fails at t = 0.01 already, and the line search cycles (period 4) to the iteration limit.
- The pass is laid out with the liquid as the marginal phase (segment code 2, c_L = 1.67e-5 kg/s at dt = 2 s). Its root drains the new condensate at F of about 1.6e-5 kg/s, at the D11 pinned boundary, and at rest the drop line's upwind switches between the tank's liquid and the lower tank's gas.
## Round 3: threshold-free forms tried (prototypes); none is robust
- Robustness matrix (`r3/robust.sh`): the D34 gate and fuzz 322, on the PH seed and on the TP seed at (1+0), (1+1e-12), (1+1e-9), (1-1e-9), plus PH enthalpy (1+-1e-9).
- Each line gives the gates passed, then the 322 runs clean, out of 7:
  - HEAD: gate 1/7, 322 3/7 (the knife edge reproduced).
  - Column fill and densities read on the step's start state: gate 6/7, 322 1/7.
  - Start fill with trial densities: gate 0/1. Start liquid density only: gate 5/7, 322 1/7.
  - Trace-phase amounts as logarithms (PhaseLayout): gate held at 4297, and the 322 runs take 30-75 s.
  - Upwind stated on the Jacobian's base branch (semismooth, as A423): gate held at 4497.
  - Both of the last two together: gate 0/3; with the start-state column as well: gate 0/7, 322 0/7.
## Round 3: owner decision E5 (2026-10-20, amending D38 C), implemented uncommitted
- The owner's words: "Take option 1, E5: a shrinking hydrocarbon liquid below VANISHING_SHARE by volume counts as vanishing (amending D38 C)." Not taken: (b) a film-drain rule for a gas-standing bottom line, (c) reverting `8fd61a13`.
- The decision lives in the stage that owns the vanishing test. `FluidThermodynamics.belowShare` is the one share criterion of `vanishing` (the D38 C fold, D39 B refutation) and of `nothing` (the own split's refusal):
  - every phase must be below VANISHING_SHARE by volume;
  - water and either gas must also be below it by moles;
  - a hydrocarbon liquid is judged on its volume alone.
- No new constant; the javadoc records the reason (1.5e-6 of the moles, 7.7e-8 of the volume). The mirror of the 21 mL bubble (D38 C, 2026-10-18): a liquid in a gas vessel holds about twenty times its volume share in moles.
- Robustness on the real code: gate 7/7, 322 7/7 (`r3/robust.sh real`). 322 takes 6.4-7.3 s.
- Replays on the real code, all clean:
  - bundled 145, 354, 889, 322: phase seeds 1 151, refusals 1 143;
  - cold 583, 743.
- Targeted Gradle run (owner: affected parts only), 19 classes, 135 tests, 0 failures: HoldClassWorldsTest, TracePhaseFoldTest, JunctionSeedingGateTest, JunctionWaterTraceTest, PumpCavitationTest, LiquidCo2LetDownTest, FluidPumpedFillLineTest, PumpBottomPortDrainTest, FluidCheckpointFormatTest, FluidCheckpointStoreTest, VesselPhaseSeedTest, JunctionPhaseAppearanceTest, LevelHeadTest, SolidChainTransportTest, JunctionStartFormTest, LiquidFullBubblePointTest, PrecipitatedSolidsTest, NetworkPhaseEngineTest, FluidThermodynamicsTest.
- Printed lines: two lines move, listed in `MOVED_REFERENCES.md` round 3.
  - The D34 gate passes. Its lower tank's 0.43 mL trace liquid now vanishes, the upper pressure moves 0.27 Pa and the gas column 0.009 Pa.
  - A log4j notice appears. It is not physics.
- Not run (owner): the full suite and the 200-seed quick check. Code guide: `fluid-science.md` "Phase check and folds" gets the belowShare rule at the merge.
