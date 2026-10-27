# D39: a failed iterate proposes nothing, and a proposal the next pass refutes is refused

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, 2026-10-18, owner decision D39 (2026-10-18). Two commits on `482264fc`: `1482c277` (seed 430) and `00ee8385` (seed 48). Tools and logs: `tools/unified-thermo-port-m5/d39/` (README; `logs/d39-logs.tar.gz`). Tests ran on the Gradle-free javac lane. Seeds ran through `bisect-seeds.sh` (Gradle, one worker), with no dev client running.

## 1. A failed iterate is not a state (`1482c277`, seed 430, option B)

**Physics.** An empty 1 m3 bag is fed 7 kg/s of LNG and CO2-rich gas, let down to 1 atm. It fills at its 1 atm bubble point, about 112 K. No node can cool below about 110 K, so no third fluid phase forms anywhere.

**What the code did.** The failed-iterate check (D28 A′) formed TP answers and own splits at failed iterates. Those iterates held junction n5 on the 1 Pa box floor, and its PH answer seeded n5 at 51 K with 13 300 m3 of gas (pass-start |r| 1.5e12). The near-empty tank's later iterates fell to 17 K. The TP answer there held the island at tick 406 on "a third fluid phase ... at 17.35 K" (class C), a state the network never reached.

**Change.** `PassiveStepSolver`'s failure branch calls `foldedIterate`, which applies `FluidThermodynamics.folded(state, junction, from)` node by node, one node per call. That is the trace fold plus rule C's vanishing fold against the step's start. There is no TP answer and no own split, and the A85 record is neither read nor written. `phaseCorrection` now checks converged points only.
- Deleted: the addition branch for failed iterates (the `phaseCheck` call on a copied record, its TP answer and its own split). Option A's catch is not needed, because no failed iterate is flashed.
- Kept: a vessel closure's failed iterate (`InventoryEquilibrium.passes`) keeps D28 A′, because a closure has fixed n, U and V and no smaller step.

**Gate.** `HoldClassWorldsTest#aFailedIterateRaisesNoPhaseHold` places the pair-minimal 8 blocks and asserts that no hold is a phase hold. Rule off (`rule-1-off.patch`), the test run alone holds at tick 406 on "a third fluid phase, which the network does not carry at 17.35 K, 101325 Pa". With the rule, the world still holds a step stall at tick 406 (E5, |r| 75.3, active-set pass 4). That is its open class: the flashing top run (M8 option C).

## 2. A proposal the next pass refutes is refused (`00ee8385`, seed 48, option B)

**Physics.** A 1.95 MPa, 149 K LNG generator fills an empty 1 m3 tank through a 0.1 m line at its cap. The tank's bottom drains 2 m down into a second empty tank. The upper tank flashes at its 1 atm bubble point, 113.07 K, and while both bags are slack the lower tank receives that liquid. No stream carries potential energy (S1), so the lower tank holds the liquid exactly at its bubble point, with no vapour. Once the upper bag is taut, within the first 0.05 s, it pressurises (206 kPa at tick 1255), and its liquid flashes on arrival in the lower tank: that vapour is real.

**What the code did.** The lower tank's liquid-only pass converged at 113.069 K. Its check proposed 3.4e-6 of the tank's moles as vapour, which the own split kept because it is above the 1e-6 share. The two-phase pass drove the vapour under 1e-8 and stalled at every step size (|r| 2e-8, then 4.04e-8). The island held at tick 1257 (E5). Rule C cannot fold that vapour: it grows against the step's start, where the tank held none.

**Change.** `PassiveStepSolver` keeps the solve's `Proposal` until a pass converges: the node, the phases the converged point's check added, the node's seed and the point. A failed pass whose iterate holds a proposed phase vanishing from that seed refutes it. The test is `FluidThermodynamics.vanishing`, now public: under VANISHING_SHARE and below the seed's share. The refused bits go to the A85 record, as an own split's refusal does, and the pass is solved again from the converged point, with the node laid out without the phase.
- One test and one record serve three rules: rule C (from the step's start), the D26 amendment (from the own split's start) and this rule (from the proposal's seed).
- The test runs after the cavitation, cap and emptying corrections and before the fold. Only a proposed phase is refuted, so D28 A′ stays for every populated phase. Deleted: the bubble-point stall that no step size resolved.

**Gate.** `HoldClassWorldsTest#aBubbleTheNextPassDrivesToNothingIsRefused` places the pair-minimal 6 blocks.
- The world keeps up to tick 1754. Both tanks end as liquid with CO2 frost and no vapour: 2.44 MPa, 158.6 K and 27.4 kg of frost; 2.45 MPa, 155.0 K and 34.6 kg.
- Traced with `refute-trace.pl`, the rule refutes 4 proposals in the first interval (dt 7.8e-4 to 3.1e-3 s). The vapour is 3.42e-6 of the moles at the seed, and 6.2e-9 and 8e-11 at the iterates, at 113.069 K.
- Rule off (`rule-2-off.patch`), the test run alone holds at tick 1257 (E5, |r| = 4.0367655433637406e-8, active-set pass 1).

**Conservation.** Both rules only choose a layout. A fold moves moles between one node's phases at its T and P, and a refutation returns to a converged pass's reconstruction; neither books anything. The committed step is a converged pass's reconstruction, closed to its booked amounts and U. `checkConservation` passed on every committed step of the suites and the gates.

## Suites and moved numbers (javac lane, each commit against its parent)

At both commits these all pass: science.fluid.network 184, runtime.fluid 297 then 298, runtime 29, science.fluid.solver 21, .transport 9, .topology 5, .thermo 66, science.material 82, science.thermo 34, .phase 78, .qualification 39, .reference 25, network 29 and client.gui 56. science.fluid passes 15 of 16 (PackagedSparseLuTest needs the built jar, as at the base). No reference was re-recorded. STACK_PASS is unchanged (upper tank 103304.54 Pa, lower tank 292.5152 kg), and so are the 33 junction lines (MIXED_GAS_COST newtonSolves 340).

Moved by `1482c277`. Failed iterates no longer gain phases, so the substep paths differ:
- WaterIntoCryogenicMethaneTest: the three cases take 1 to 3 more substeps. At 115 K interval 1 ends 3.1 Pa higher with 0.24519 m3 of liquid (against 0.24523). In gate (c), interval 5 ends 0.21 K warmer, and interval 6 ends with 36.44 kg of ice at 1.6256 MPa (against 37.63 kg at 1.6460 MPa): the stiff freeze takes another path.
- LiquidCo2LetDownTest 4 mm, slice 2: 8 substeps, 5 rejected (against 3 and 0); the state is the same. The D38 gate: the tank moves 0.04 Pa, and junction 6 holds 0.98665 kg (against 0.98799). CrystalFlushBudgetTest row E: 2089 checkpoints (against 2928); the state is the same. PhasePortPriorityTest, pentane slice 56: one rejection gives another reason.

Moved by `00ee8385`, in rejected attempts only, with the same states: PhasePortPriorityTest pentane slice 56 (a liquid-carrier rejection, not a singular Jacobian), and WaterIntoCryogenicMethaneTest gate (c) interval 6 (3 rejections against 2; one boundary reopened).

## Seeds alone at `00ee8385`

- 430 (12 blocks): E5 at tick 406 (|r| 75.3, active-set pass 4), no longer C. No hold is a capability hold.
- 48 (15 blocks): clean to tick 13254, with 70.9 kg of CO2 frost in two vessels. It is the costliest of the four: 22 576 Newton solves and 10 898 step attempts; its island is still FULL at the end, fed and vented.
- 30: clean to tick 13593 (721 Newton solves). 19 (27 blocks): E5 at tick 2826 (|r| 267.8, active-set pass 4), against E7 at `482264fc`. Not traced (lean rule).

**Code guide:** no change now, because the guide describes main. At the merge, `fluid-science.md` step 6 gains the failure branch's fold-only check (D39) and the refuted proposal (D39 B), and step 7's `phaseCorrection` covers converged points only. `verification.md` gains the two gates.
