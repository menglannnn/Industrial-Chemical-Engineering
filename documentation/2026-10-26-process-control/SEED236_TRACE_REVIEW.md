# Seed 236 trace: diagnosis (physics first)
2026-10-27, Claude (Opus 5.5), branch `claude/process-control-fixes` at `fa62fd81`. Minimized world (nine blocks), probe runs only, no fix. The hold is the same on `8523595a`.

## 1. Expected behaviour
- Water flows from the supply (346 kPa, z = 3) through the junction (365 kPa, z = 1) and the 1 m³ tank (374 kPa, z = 0) out of the tank's 0.02 m bottom line to the atmosphere at z = -2. The flow is steady at 6.90 kg/s. The pressures are hydrostatic: 346 + rho·g·2 m = 365 kPa.
- The crude supply (332 kPa) is 33 kPa below the junction, so no crude enters and its line carries nothing.
- An earlier crude inflow left traces in the network:
  - the junction holds 18 crude components, 6e-21 to 1.9e-19 mol each, about 0.2 fg in total, 1e-22 of its 2111 mol;
  - the tank holds 7.0e-20 mol of crude_pc01.
- Real life: these traces are dissolved molecules. They wash out with the water (junction residence 5.5 s) and change no pressure, temperature or flow. Nothing should stop.

## 2. What the code did
1. **Seed.** Each component that arrives at the tank but is not in its start inventory is seeded at 1e-12 of the tank's moles: 5.4e-8 mol for each of 17 components (decision D40 A). This is 10^13 to 10^15 times what actually arrives.
   - The tank starts with crude_pc01 only. Every arrival below 5.4e-20 mol (1e-24 of the tank) is flushed at each accepted step, and the LIQUID outlet draws out the tank's own trace.
   - The seeded mixture is rich in light ends, so the seed is two-phase: 3.2e-10 m³ of liquid and 1.05e-9 m³ of gas.
2. **Fold.** Both hydrocarbon phases are below the D34 trace share: 9.2e-7 mol in total, 1.7e-11 of the tank. But the A84 fold moves a trace phase only into a phase that stands, and free water is the only standing phase. Both phases therefore stay. The tank carries 12 two-phase components with 12 equilibrium rows for about 1e-21 mol.
3. **Failure A** (steps from 2 s down to 1/32 s, 7 rejections): `No finite Jacobian trial at variable 21`, the tank's methane amount.
   - A component with no start amount gets the bulk difference floor: 1e-10 of the tank's moles, or 5.4e-6 mol.
   - The iterate's hydrocarbon liquid holds about 1e-18 mol.
   - The plus step makes that liquid almost pure methane at 350 K, which has no liquid root. The minus step makes the amount negative.
   - Sixteen halvings leave the step at 8e-11 mol, still 10^7 times too large.
4. **Failure B** (steps from 1/64 s down to 3.8e-6 s, 13 rejections): `line search stalled at 0.52997`, raised in `InventoryEquilibrium.close` during `ConservativeTransport.reconstruct`.
   - The network Newton converges. Its rows for arriving components are stated over the tank's whole amount, so it never resolves the trace's composition.
   - The reconstruction then gives the tank its exact amounts. The LIQUID outlet's equal-volume co-draw has drawn the tank's own trace out within the step (D11 capacity), so the tank ends with only what arrived: 2.8e-21 mol at 1/64 s, proportional to dt.
   - A TP flash of that composition at 350.0 K and 374.2 kPa gives all liquid. Gas first appears between 2.2 and 2.0 bar (vapour fraction 0.0015 at 2.0 bar).
   - The closure is still asked for two phases. It drives the gas from 7e-25 to 9e-32 m³, while the equilibrium rows stay at -0.44 to -0.53. `close` folds a phase only after its own Newton converges, so it never gets there.
   - The residual is the same at every step size because the arriving composition scales with dt.
5. **Hold.** The 7 + 13 rejections make 20 in a row, which gives "Substep refinement exhausted" (E5). The classifier also tags b:trace-underflow because the reasons include "No finite Jacobian".

Every failing path predates the branch. `foldTracePhases`, the 1e-12 entry traces, the bulk difference floor and `close` are unchanged since `600549d9`. The branch makes the state reachable through its LIQUID co-draw, which empties the tank's own trace each step, so every step starts with arriving components only. PLAUSIBLE: the base's water-first draw kept that trace in the tank. Not run on the base. Finding 4's end stream does not take part: the outlet carries 6.9 kg/s.

## 3. Proposal (for the owner to choose)
- **Option 1 (recommended): complete D34 for water-full nodes.**
  - Rule: when every hydrocarbon phase of a vessel or junction is below 1e-9 of its moles and free water is its only standing phase, the hydrocarbons are omitted traces. They get no unknown, no row and no equilibrium, in the step and in the closure.
  - The reconstruction carries them exactly and flushes them below 1e-24, as now. A share of 1e-9 or more is a phase appearance through the existing pass.
  - This is D41's rule for a junction's carried trace gas (`PhaseLayout.support`, `carriedTrace`), extended to vessels and to liquid traces.
  - Effect: only below 5.4e-5 mol (about 5 mg of crude) in a 1 m³ water tank, or 2.1e-6 mol in this junction. Amounts and energy stay exact in the reconstruction. The step's energy row leaves out at most about 1.6 J of 2.2e9 J (7e-10).
  - It deletes the trace two-phase layout in water-full nodes and the junction-only, gas-only condition in `support`.
  - Risks: one more appearance crossing at about 5 mg of hydrocarbon. The trace's state holds the phases its own TP flash gives, without rows.
- **Option 2: two narrower rules.**
  - (a) A84: when both hydrocarbon phases are traces beside standing water, the smaller folds into the larger. This removes B.
  - (b) An arriving component's difference step comes from what arrives over the step, not from the bulk floor. This removes A.
  - Both keep the unknowns. Rule (b) changes the D40 A seeding contract.
- **Option 3: a consistency item, not enough on its own.** `close` applies the pass loop's collapse rule (D38 C, `VANISHING_COLLAPSE`) to a failed iterate: it folds the collapsing gas and closes again.
  - This removes B only. Steps of 1/32 s or more still fail A, so the island would crawl at 1/64 s or less, about 320 substeps per 5 s slice.

Probe:
- Code: `tools/process-control/seed236-probe/`: a temporary hook patch against `fa62fd81`, `Probe236` and `Trace236Test`. It needs the bundled harness.
- Logs: `tools/process-control/logs/seed236-trace-logs-2026-10-27.tar.gz`.
- Code guide: no change (diagnosis only).
