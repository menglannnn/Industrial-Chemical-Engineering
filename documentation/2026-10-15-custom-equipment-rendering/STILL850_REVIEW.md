# Still pipe 850 on the converging balance (Opus, 2026-10-21), `e5357594` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-21: "Option (a): commit G3, change the 889 gate to assert the cavitation physics ... trace still pipe 850 on G2 physics first, then run the final lane." Follows `BALANCE_CONVERGENCE_REVIEW.md`. Gate: `HoldClassWorldsTest.aStillPipeBesideATankHoldingCrystalsDepositsNothing`. Probes: `tools/new-units/od4-holds/r8/`. Code guide: no change (not merged).

## 1. Physics first
- The world: a CO2-rich gas generator (2.83 MPa, 270 K, fed through 10 mm) and two LNG generators (0.83 and 2.05 MPa, pushed back) share a 1 m3 tank. The tank is taut: 0.998 m3 of LNG at 155.7 K plus 2.9 kg of frozen CO2.
- At rest, nothing flows, and the pipe from the junction beside the tank carries nothing and deposits nothing. The step's own state at the refusals:
  - The tank and junction 2 beside it stand at 2 831 321.6 Pa.
  - Junctions 5 and 0, 1 m lower, stand 2 597 Pa higher: a 1 m two-phase column of about 265 kg/m3.
  - Junction 0 is 1.05 kPa above the gas generator, so the one-way feed carries nothing.
  - The taut tank takes no gas. Its pressure follows its temperature (+0.9 mK over these intervals).
## 2. What the code did (`aca82e8f`, the G2 + G3 tree)
- At each interval start, `SolidEventIntegrator.solve` decides which pipes are still from one rate solve of the port graph (vessels held as ports).
  - Over the settling minutes after the valve at 3652, that rate solve failed on 10 intervals, eight of them in a row.
  - The port graph holds the taut tank at its refreshed pressure, 1.36 kPa under junction 2, and the balance restates the junctions as the generator's gas. So the port graph has a gas path of 0.02 to 0.07 kg/s from the generator into the tank, which the island does not have.
  - That path crosses the one-way feed's kink and the riser's dead band. Five balances ended unconverged (residual 0.057 kg/s against a tolerance of 7.8e-5), and the rate Newton ended on its iteration limit.
- With no rate solve, `still` stayed null and every pipe was judged by the sign of its flow.
  - The steps left 2e-12 to 4e-53 kg/s in the tank pipe, and that sign named the tank, which holds crystals, as the donor. It read DEPOSITION against a 1.275 m/s suspension velocity.
  - That gave 25 refusals in the eighth failing interval, and the pipe was closed.
- G2 is not the mechanism. On the G1 tree the same rate solve fails 7 times, 5 of them from converged balances. Its run of failures was four intervals long and ended before the step's flows fell to round-off. G2 only moved which intervals fail.
## 3. Fix `H`, committed as `e5357594` (the owning stage: the still rule in `SolidEventIntegrator`)
- Where the rate solve fails, the still pipes are read off the flows the last accepted step left on the structure. These are `PassiveIntervalSolver.carriedFlows`, the t0 flows its own start-of-step check already reads.
- The same round-off bound applies: 1e-9 of the largest flow, at least 1 kg/s. A failed rate solve still declares no new closure (D33/D62, `carriedWithoutRate` unchanged). A structure with nothing carried keeps the old reading.
- Changed: one branch and one accessor made package-private. No rule is added: the owner's still rule (t53) is stated for a failed rate solve too.
## 4. Results (against `aca82e8f` unless stated)
- Gate 850: 25 refusals → 0. Targeted classes 148/149 (the known `FluidCheckpointFormatTest` lane failure); pump gates 4/4.
- Quick check, 200 seeds, 4 workers (against `a6b14b22`, so G3 is included). Nothing is newly held:
  - cold 195 = 195, the same held seeds (85, 6, 110, 29, 101), Newton +1.1 %;
  - bundled 199 against 198 (116 cleared from E5; 157 is the same F load hold), Newton +1.0 %. Gate refusals 101 → 248 and phase seeds 37 → 247 are 116 running to its end;
  - defaults 200.
## 5. Residual and proposition
- The rate solve fails at rest where a taut vessel is held as a port: the port graph has a flow the island does not have. This costs failed Newton solves and loses the t0 closure pre-emption in those intervals. Not changed here.
- Proposition: keep `H`. The port model at a taut vessel is a separate design item if the owner wants it. The final lane on `e5357594` is in `FINAL_LANE_E5357594_REVIEW.md`.
