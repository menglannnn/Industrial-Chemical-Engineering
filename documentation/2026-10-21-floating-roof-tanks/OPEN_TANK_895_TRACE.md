# Open tank seed 895: trace and the owner's hold-then-vent rule (prototype)
2026-10-21. Trace at `796da897`, prototype on `4c15e508` (same solver), Gradle-free javac lane, one worker per seed, 4
threads for the quick check. Product code and worktree unchanged; code guide: no change. Tooling
`tools/floating-roof-tanks/trace-895/`, logs `tools/floating-roof-tanks/logs/trace-895-logs-2026-10-21.tar.gz`.
## 1. Expected behaviour (895, 11 blocks)
- N2 at 352 kPa, 260 K feeds a header; the CH4 generator (123 kPa) is below it, so its line stays shut. One 0.02 m line
  leaves the header at the 100 m/s cap (rho 4.0 kg/m3): 0.126 kg/s. The split junction (~185 kPa) sends 0.066 kg/s
  into the open tank (capped) and 0.060 kg/s through the filter to the void; both sinks stand at 101 325 Pa.
- Steady state: tank gas out = gas in = 0.066 kg/s; it holds at most one step's arrivals (0.026 kg at 0.4 s steps).
  Liquid that arrives stays in the tank.
## 2. What the code did instead (`796da897`)
- `ventOpen` emptied the gas-only tank after each accepted step, so every step laid it out from that step's arrivals
  only (`arrivingPhases`/`emptyBagSplit`). Its row scales are those arrivals (0.46 mol, 1.4 kJ at dt 0.2 s; 0.029 mol,
  86 J at 0.0125 s), proportional to dt: a given relative inflow error is the same residual at every step size, and
  with every line at its velocity cap (flat flow derivatives) the Newton stalls. Step [107,115]: 20 sizes, 0.4 s to
  7.6e-7 s, residuals 0.13 to 1.8, not falling with dt; the tank's rows reach 0.1 to 0.5 at the last iterate.
- Below ~1e-6 s the arrival is under the bag's resolution (1 mL) and the tank is laid out dry (a 1-atm receiver); the
  edge rows still stall (filter line law flow -9.5 kg/s against a 0.06 kg/s cap), so a void-like tank fails too.
  Closed, the tank carries its gas, its scales stay fixed, and the step commits after 4 rejections.
## 3. Owner rule (2026-10-21) and prototype (`trace-895/owner-rule-gas-vent-4c15e508.patch`, +21/-35)
Gas-only open vessels follow form B: keep what arrives in the step, vent the start gas over the step as constant terms.
Physics forces only out = in at steady state; the one-step lag is a game-level assumption (holdup <= one step's inflow).
- `OpenVessel.releases`: the vent no longer needs liquid. `PassiveIntervalSolver.ventOpen` and `Vented` deleted (no
  open-vessel FLUSH; the trace flush is unchanged).
- Zero end inventory (vents all, nothing arrives): `reconstruct` threw "Candidate overdraws a reservoir" at every step
  size. Absorbed by existing structure: `OpenVessel.releasesAll` (releases take every amount, no solids); in
  `reconstruct` such a vessel that no connection reaches or draws from is a kept node (D54 untouched-vessel rule) ending
  with zero moles, its leftover energy (U - H_vent = -P0 V_bag) booked as ambient work at the vessel as for an emptying
  one; the step result's "emptying vessel shows the ambient" line now covers any vessel ending with nothing. The
  emptying junction path does not fit (it drains the stock through connections).
- Probes (`trace-895/OpenTankGasProbeTest.java`): 1 m3 N2 alone or beside a void vents 40.89 mol in slice 0, then empty
  (ambient); fed from 1.5 bar, holds 5.67 mol and vents 14.17 mol per 5 s; ledgers exact. `OpenTankTest` 6/6,
  `OpenTankRuntimeTest` 2/2, `PresentationBucketTest` 10/10.
## 4. Seeds (bundled random, new harness, one worker): base `4c15e508` vs prototype
| Seed | Base | Prototype | Notes |
|---|---|---|---|
| 895 | E, 2 holds | clean ("venting 0.0749 kg/s") | this mechanism |
| 598 | clean | clean | the 320 K harness cap removed the boiling feed |
| 354 | E, 10 holds | identical | island 29: open tank holding water (320 K) fed water + CH4 gas (361 K) via a junction; 80 active-set passes, gate 1.27e-8 > 1e-8 |
| 678 | E, 19 holds | identical | island 32: empty open tank filling with 310 K crude (LIQUID port; VAPOR port on a crude-generator/void junction); half of 1024 substeps stall |
Not the 895 mechanism: 354 and 678 hold on liquid-holding or filling open tanks (both clean closed); each needs a trace.
## 5. Quick check, 200 seeds, `-DholdFuzz.openTanks=true`, 4 threads
| Distribution | Clean base / proto | Counters, proto vs base | Seeds differing |
|---|---|---|---|
| bundled random | 200 / 200 | step attempts -14.3 %, active-set passes -11.7 %, Newton solves -9.7 %, iterations -6.9 %, residual evals -5.1 %, gate rejections 1 -> 0 | 19 |
| bundled defaults | 200 / 200 | identical | 0 |
## 6. Proposal for the owner
- Adopt the prototype. It deletes `ventOpen`/`Vented` and the open-vessel FLUSH; a gas-only vent becomes a rate. Risks: a gas-only vessel with nothing arriving is still laid out with zero targets (converged in one substep in the
  probes, not proven in general); a BULK outlet of a gas-only open vessel could draw gas the vent already takes (only
  phase ports are capped; the same gap exists for liquid vessels). Next: trace 354 and 678; one full campaign.
