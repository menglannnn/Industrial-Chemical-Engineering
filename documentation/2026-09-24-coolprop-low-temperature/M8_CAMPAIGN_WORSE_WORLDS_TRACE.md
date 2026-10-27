# M8 campaign worse worlds: seeds 430 (C) and 48 (E5) minimized and traced (2026-10-18)

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port` at `482264fc`; diagnosis only, nothing committed (cold package fuzz, 4 threads, trace hooks applied per commit and reverted). Each world was minimized as a pair, held at HEAD and clean at its clean parent: 430 from 12 to 8 blocks (C at `482264fc`, clean at `4199dbe6`), 48 from 15 to 6 (E5 at `482264fc`, clean at `22b76697`). A placed tank is an empty bag (D54). Its 298 K nitrogen is the ambient placeholder and holds nothing.

## Seed 430 (C): a capability hold raised at a failed iterate

**Physics.** At tick 404 two pipes join an empty 1 m3 tank (z = -1) to junction n0. One goes through junction n4 to the tank's side, the other through an upper 0.05 m run n5 (z = 0) to its top. n0 is fed by a CO2-rich gas generator (4.89 MPa, 293 K, 84 % CO2) and an LNG generator (884 kPa, 128 K, 97 % methane). Expected: the gas line runs on its cap (3.48 kg/s), and 3.5 kg/s of LNG joins it in n0 (0.80 MPa, 145 K). The mix is let down through n4 (0.36 MPa) and n5 (1 atm) and fills the bag at 7 kg/s. At 1 atm it flashes to its bubble point, about 112 K: 0.35 kg after the first 0.05 s, 14 % of it vapour. n5 must flash too: its liquid at 134 K has a bubble pressure near 0.46 MPa, so the lines into the tank carry a gassy mix. Real CO2 would start to frost at 112 K; the package keeps it dissolved within a step. No node can cool below the mix's 1 atm bubble point, about 110 K.

**What the code did (log 02).**
- The 0.05 s attempt converges once, at pass 5, on exactly that tank (111.889 K, 0.35 kg), but n5 is still liquid at 133.8 K and 100.7 kPa, 22 K above its bubble point. The check flashes n5, which is right. The next pass starts at |r| = 1.05e4 on the tank's top line and fails. No attempt is accepted: 20 attempts down to 9.5e-8 s, 184 passes, 153 failed Newtons, 1.03 M checks.
- Whenever the failed-iterate check (D28 A′, in `solve`'s failure branch) changes a node, the next pass starts from that failed iterate. These iterates hold n5 at 1.0-1.7 Pa, the numerical box's floor, and n5's PH answer there seeds it at 51 K with 13 300 m3 of gas (pass-start |r| = 1.5e12). M7's seed 19 was seeded the same way, at 52 K. The tank holds at most dt × inflow (1e-11 to 3e-3 mol), and in the next iterates it sits at 17-57 K. The 1e-9 trace fold removes its vanished vapour (under 1e-300 m3).
- The 20 rejections are 6 cycles, 6 THREE_PHASES, 3 CRYSTAL_PHASES, 2 stalls, and one each of an iteration limit, a non-finite matrix and a TP non-convergence. All six THREE_PHASES come from one call (`M8HOLD`): `adopt` ← `equilibriumTP` ← `phaseCheck` ← `phaseCorrection` at a failed iterate. They are at 17.07-18.30 K and 101 325 Pa, on 1e-11 to 3e-3 mol. The last attempt ends on one, so `phaseHoldCause` makes the interval C.

**Mechanism and answers (logs 02, 06, bisect).**
- (a) 17.35 K is the tank in pass 7's failed iterate at dt = 9.5e-8 s (6.7e-11 mol, 2.1e-15 m3). It was never committed: the committed tick is 404, where the tank is the empty bag at the ambient.
- (b) The node got there through the chained failed-iterate seeds. It was not a mover (the minimal world has none). It was not the energy rows: the failing rows are the lines' hydraulic rows and the junctions' continuity rows. It was not D34's column, whose heads are the same at D34 to 0.1 Pa where the runs part (log 06). It was not rule C, whose 1e-6 share is never needed. S1, which takes 9.81 J/kg off the 1 m lines, turns the 12-block seed from E7 into C; like seed 30 in M7, that is a redraw of the same search.
- (c) Bisect of the 12-block seed alone: clean at `4199dbe6` (and at M5); E7 from `03bc70f6` (D36) through `22b76697`; C from `eec02758` (S1) on. The hold reads 17.08 K at S1 and `65036f2b`, and 17.35 K at `26090bf0` and HEAD. The D36 revert and rule C leave it held.
- Contrast (log 06). D34 runs passes 0-5 to the same point and parts at pass 6, which fails at 30.3 against HEAD's 99.3. It converges at pass 8 and commits the tank as a liquid at 135.46 K and 1 atm, 24 K above its bubble point. The tank's vapour had been folded at pass 6's failed iterate, and the converged check did not give it back. D34 was clean by chance, with a non-physical tank.
- Rule: broken. The hold states a capability limit ("a third fluid phase") about a state the network never reaches. That state is a failed iterate, which D28 says is not a state, seeded from a junction on the box floor. AGENTS.md wants such excursions to be rejected trials.

**Proposition (owner to choose).**
- **A (minimum).** A typed hold raised by the failed-iterate check is a failed trial: the pass fails as a Newton failure and carries no phase hold. It adds one catch and deletes the C path from failed iterates. 430 becomes E7 or E5, its real class. Real three-phase states keep their C through the Newton's remembered trial holds and through converged points.
- **B (recommended, structural).** Complete D28: a failed iterate may fold what it empties (A′, unchanged) but proposes nothing, so the check takes no TP answer and no own split of a non-state.
  - It absorbs A, because no failed iterate is flashed, and it removes the 1 Pa seeds of 430 and of M7's seed 19. It deletes the addition branch for failed iterates and adds nothing.
  - Risk: a pass that needs a phase to appear before it can converge must find it at a converged point or at a smaller step; the route that D31 A replaced at `a0f20364` used a failed-iterate addition. Unmeasured: one form on the 200-seed quick check. Whether 430 then runs clean is open.
- **C.** M7's option C, the smooth velocity cap. The cap set a flashing line needs is what this search keeps redrawing. Large; it needs a plan.

## Seed 48 (E5): a saturated liquid's vapour that the check proposes and the Newton cannot hold

**Physics.** A 1.95 MPa, 149 K LNG generator (88 % methane, 6 % ethane, 5 % CO2) fills an empty 1 m3 tank through a 0.1 m line on its 100 m/s cap (336 kg/s). That tank's bottom outlet drains through a 0.1 m pipe 2 m down into a second empty tank. Expected: the upper tank flashes at its 1 atm bubble point, 113.07 K. From about 0.0125 s its liquid falls into the lower tank at 57-60 kg/s, driven by the 8.4-10 kPa column; both bags are slack at 101 325 Pa. In reality the fall's g·Δz (19.6 J/kg) is dissipated in the lower tank and flashes about 4e-5 of the liquid (0.006 K of heating). S1 removed that energy, so in the model the lower tank holds liquid exactly at its bubble point, and its vapour is zero.

**What the code did (log 04).**
- Two steps (0.0125 s and 7.8e-4 s) are accepted. At each of the next 20 step sizes, from 1.6e-3 s down to 3.0e-9 s, the lower tank's liquid-only pass converges in 4-9 iterations at 113.069 K. Its check then proposes a vapour of 3.4e-6 of the moles, the TP answer at that point, and the own split keeps it because it is above the 1e-6 share.
- The two-phase pass drives that vapour to 8.8e-9 and stalls, once at |r| = 2.1e-8 and then at 3.96-4.04e-8 at every dt, on the lower tank's four equilibrium rows. Their uniform 4e-8 offset in ln f is one a shrinking vapour cannot close, so there is no root with a vapour. The check and the Newton disagree about the sign of a bubble point's superheat at the 1e-6 level.
- The failed-iterate check removes nothing. 8.8e-9 is above the 1e-9 fold. Rule C's vanishing test needs a smaller share than at the step start, where the tank held no vapour, so a phase the solve grows is never vanishing. The only fold is at the first draining step: a vapour laid out on the empty bag's first arrival and driven to 3.7e-312 m3. The refinement runs out (29 attempts, 24 failed Newtons), and the interval is E5.

**Mechanism (logs 04, 05).** At S4 the stream carries g·z. The lower tank is laid out with 4.4e-5 of its moles as vapour and converges two-phase at 113.070 K, 0.001 K above the upper tank, and the interval runs FULL (23 steps accepted, 9 rejected). S1's 19.6 J/kg is exactly that flash. Here S1 is more than a trigger: it puts the receiving tank on its bubble point, a degenerate state. A level transfer (Δz = 0) of a boiling tank's bottom draw would be degenerate at any commit (expected, not measured).

**Proposition (owner to choose).**
- **A.** Accept S1's hold. A tank draining into a lower empty tank is a plain build, and this one holds at its first transfer. Nothing is deleted or added.
- **B (recommended).** A proposal the next pass refutes is refused.
  - The rule: if a pass laid out with a phase that the previous converged point's check added fails with that phase under VANISHING_SHARE, the next pass lays the node out without it and records the refusal, as the D26 amendment records an own split's refusal.
  - This extends rule C to a phase the solve itself creates. It deletes this dt-independent stall and adds one test in the failure branch. Its evidence is a failed iterate, which D28 A′ kept away from the record; here it is confined to the solve's own proposal.
  - Risk: a phase whose first two-phase pass fails for another reason while the phase is still small is refused for that one step. References at bubble-point transfers may move. Unmeasured.
- **C.** Revert S1. The fall flashes 4e-5 again, and the vertical case is clean by physics (log 05). g·z returns to five energy files (60 lines, M7), and the level case stays degenerate.

Tools: `tools/unified-thermo-port-m5/trace/` (README section "M8 campaign traces": rebased hooks, KEEP lists, commands). Logs: `build/m8-logs/trace/01`-`06` of the worktree; bisect `build/m7-logs/bisect/m8-430-*`.

Code guide: no change.
