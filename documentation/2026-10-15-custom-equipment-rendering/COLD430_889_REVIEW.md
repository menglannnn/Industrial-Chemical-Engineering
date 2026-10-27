# Cold 430 and bundled 889 on the converging balance (Opus, 2026-10-21), `d139fe93` + `8d69949d` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-21: "Option (a): trace cold 430 (from its 8-block gate world) and bundled 889 physics first, fix structurally, affected parts only, then the final lane." Follows `FINAL_LANE_E5357594_REVIEW.md` section 5. Probes and patches: `tools/new-units/od4-holds/r9/`. Logs: `tools/new-units/logs/cold430-889-2026-10-21.tar.gz`. Code guide: no change (not merged).

## 1. Cold 430, physics first (gate `HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold`, 8 blocks, cold pilot package)
- At tick 404, two pipes join an empty tank (101 kPa, 298 K) to junctions fed by a CO2-rich gas generator (4.89 MPa, 293 K) and an LNG generator (884 kPa, 128 K).
- What should happen: the gas expands into the tank through lines choked at the 100 m/s cap, and the LNG generator is pushed back. This is a filling transient with every junction between 0.1 and 4.9 MPa, and no state the step cannot represent.
- What the code did (`a6b14b22` to `e5357594`): the mint's balance ended unconverged after 30 iterations and 0 sweeps, at a residual of 5.5 kg/s against a 13.4 kg/s scale.
  - Junctions whose every line is choked or one-way have flat rows. The diagonal floor turned those rows into Newton steps of 1e13 to 1e16 Pa.
  - The half-hull limit cut each step to 4e-8 of its length, and the Armijo test accepted 1e-7 of the merit as a descent. The sweep, which bisects each junction onto its own root, only ran where the Newton did not descend, so it never ran.
  - The step started from the unbalanced point and held E6 at tick 406 (Newton iteration limit at pass 0).
  - On G1, the secants over 1e-7 of the pressure gave the flat rows a slope by chance, and the tenth iteration escaped.
## 2. Fix A, `d139fe93` (owning stage: the balance's Newton, `BalanceNet.solveOnce`)
- Where a step's linear root lies outside half the hull (limit < 1), the sweep goes first, and the Newton line search runs only where the sweep does not improve. The existing cap of 12 sweeps per solve holds.
- No rule is added: A changes when the balance's existing method for a flat row is tried.
- The gate now asserts keep-up. Proof: on `e5357594` it holds at 406 (Newton iteration limit, residual 0.77).
## 3. Bundled 889, physics first (23 blocks; `PumpCavitationTest`'s starved suction has the same mechanism)
- A water generator feeds a pump through a suction line that cannot carry the pump's target. The pump cavitates, and its suction stands at the vapour pressure (D59 (4)).
- Real life: water drawn down a line is a compressed liquid, and throttling warms it by Δh = v(1 − αT)ΔP.
  - From 1 atm at 298.15 K that is 91 J/kg, or 22 mK. Its vapour pressure is then 3 173.9 Pa, not the 3 169.8 Pa of 298.15 K.
  - After 889's 309 kPa descent the water is 0.06 K warmer. At its own vapour pressure it arrives as saturated liquid and nothing flashes, which is the 889 gate's "stays liquid".
- What the code did (`aca82e8f` on): the balance's water floor read the vapour pressure at the junction's stated temperature, which was stated at another pressure.
  - At that floor, 1e-4 (889) and 3.7e-5 (starved gate) of the arriving water's mass lies above the saturated liquid's enthalpy.
  - The floor cannot correct itself. Below the liquid's bubble pressure, restating the junction returns Tsat(floor), so every lower floor is a fixed point.
  - `mixed()`'s TP bisection cannot state the two phases of pure water. It ends straddling the latent-heat jump, and the midpoint's side is round-off. In 889 it landed on steam.
  - A pure-water junction holding steam beside its water makes the suction limit's row (P = Psat(T)) its own phase rule, so the Jacobian is singular. 889 held at pass 0, tick 2876.
## 4. Forms (gates and the two worlds; the quick check on the chosen form only)
- B: the engine's PH answer for water alone. This gives the two-phase answer at the stale floor, and two pump gates held on trace steam. Rejected.
- C: the bisection ends on the side nearer the delivered enthalpy.
  - 889 was clean, but the starved gate held at tick 107 (singular). The junction was minted liquid at 298.15 K, and the rate solve failed.
  - The step's arrival layout at the stale floor then formed 1.3e-6 steam by moles, 5 % by volume.
- D, chosen with C (`8d69949d`): `BalanceNet.waterFloor` states the floor at the saturation temperature of the junction water's own enthalpy.
  - The computation is one Newton step on the saturated liquid's enthalpy (two TP flashes), once per connection.
  - Vapour beside the water, or a hydrocarbon beside it, keeps Psat(T).
  - With the floor at the arriving liquid's bubble point, both worlds keep up without C (one-test proofs). C stays to remove the round-off coin flip at that floor.
## 5. Results (against `e5357594`)
- Gates:
  - `PumpCavitationTest` 4/4 (the starved gate fails on A + C, which proves D);
  - HoldClassWorldsTest 430 keeps up, 850 has 0 refusals;
  - cold 430 and bundled 889 clean (one worker).
- Targeted classes 147/149 under the quick check's load. The failures are `FluidCheckpointFormatTest` (the known lane failure) and `aClosureNothingMovesCarries...` (wall deadline at 2675); the second passes alone.
- Quick check, 200 seeds, 4 workers, beside the targeted run and another project's jobs (4 threads):
  - cold 193 against 195: the same held seeds (85, 6, 110, 29, 101) plus F 72 and 118, both clean alone. Newton iterations +3.0 %, phase seeds 6 299 → 7 409;
  - bundled 199 = 199 (157 is the same F load hold), Newton −1.8 %;
  - defaults 200 = 200.
- Final lane: `FINAL_LANE_8D69949D_REVIEW.md`.
## 6. Proposition for the owner
- Keep D. C can be kept (no coin flip at a water suction floor) or dropped (D alone clears both worlds). A clears 430, but the final lane shows it causes three new worse worlds (cold 311 and 567, defaults 397; `FINAL_LANE_8D69949D_REVIEW.md` section 5).
- Limit: a hydrocarbon beside free water keeps the stated temperature's floor. Its liquid warms on descent too, but no world shows it, and it was not traced.
- Owner decision 2026-10-21, option (b) of `FINAL_LANE_8D69949D_REVIEW.md`: "drop A (d139fe93), keep the water floor fix (8d69949d), then run the final lane". A is reverted as `c7b883cb` (the 430 gate asserts no capability hold again, not keep-up; A stays in `tools/new-units/od4-holds/r9/prototypes/r9-a-sweep-first-against-e5357594.patch`). 430 is open. Final lane: `FINAL_LANE_C7B883CB_REVIEW.md`.
