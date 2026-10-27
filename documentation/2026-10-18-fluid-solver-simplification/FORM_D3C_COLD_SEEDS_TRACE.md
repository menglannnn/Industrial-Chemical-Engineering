# Cold seeds 199, 6 and 15 under form D3c (thread t42b, 2026-10-19)

Owner decision of 2026-10-19 on `FORM_D_GATES_TRACE.md`: trace these seeds before D3c lands. Method:
- Each seed was pair-minimized on the javac lane: kept in its D3c behaviour at D3c and clean at `6c073298`, budgets lifted. Seed 199 used the total-work class "W" at ≥ 250,000 checks.
- Then traced with t40's pass, Newton and step probes. Exports only; nothing in the worktree changed.
- Material: `tools/fluid-solver-simplification/trace-d3/` (`lane-pair-minimize-t42.sh`, `src/T42CandidatesTest.java`, `patches/form-d3d-mixture-choke.patch`, `logs/t42b-logs.tar.gz`).
- All three worlds hold an LNG generator that feeds 1 atm voids or empty tanks, so they carry flashing two-phase junctions.

## Seed 199 (cost ×9, no hold): 9 of 19 blocks
- **World.** An LNG generator (168 K, 4.88 MPa, 96 % methane) and a CO2-rich gas generator (305 K, 5.12 MPa: 57 % CO2, 24 % methane, 20 % nitrogen) feed junctions that mix the two streams and vent through 50 mm into a 1 atm void, plus 20 and 10 mm from the LNG side to a second void.
- **Physics.** The mixing junction sits near the mixture's critical region (4.5 to 5 MPa, 190 to 240 K) and its vent is choked. More LNG makes the mix denser, so the vent passes more mass, the junction pressure falls, and still more LNG flows. That positive feedback (a Ledinegg-type flow excursion) makes the steady state multistable. A real plant settles on one branch and stays there, with hysteresis. The three trees reach three branches: baseline liquid-rich (188 K, vent 63.7 kg/s), D3 gas-rich (240 K, 20.2 kg/s), D3c two-phase (200 K, 38.8 kg/s).
- **What D3c did.** At its steady state, alternate 5 s intervals start with a step (h = 1 s, from the committed flows) that lands on the liquid branch (LNG 29.5 kg/s, vent 47.5 kg/s); the next steps return to 38.8 kg/s. In the other intervals that first step fails twice ("Matrix values must be finite"). The island holds only junctions, and the 5 % state-change control measures vessels only, so the 22 % jump is accepted. The interval results never repeat, so the island never earns its STEADY certificate: it solves every interval (124 costly solves against 8).
- **Mechanism.** D3c's own wiring picks the branch: its throat share gives the two-phase mixing junction 118 m/s, where D3 and the baseline give `min(100 m/s, acoustic)`. An older rule (junctions outside the step-change measure) then lets each interval's first step hop between branches. The cost also grows with the cap's level alone: 1.8 s with the old law at 260 m/s, against 0.31 s.

## Seeds 6 (new hold, B) and 15 (new hold, E6): 12 of 25 and 8 of 13 blocks
- **Worlds.**
  - Seed 6: an LNG generator (164 K, 5.6 MPa) is placed last, into 50 to 100 mm lines, an empty 1 m3 tank and 1 atm voids.
  - Seed 15: an LNG generator (153 K, 2.75 MPa) is joined by a 20 mm pipe to an empty tank, and through a filter and 10 mm lines to a second empty tank and a void.
- **Physics.** Subcooled LNG floods a 1 atm network. The liquid lines run at the configured cap: 77.5 kg/s per 50 mm line in seed 6, 13 kg/s through 20 mm in seed 15. It flashes to 112 to 144 K in the downstream junctions, whose vents pass only 0.4 to 0.9 kg/s, so the tanks fill and their pressure rises toward the LNG's bubble pressure. No junction can stand above the generator.
- **What the code did.** In the first interval after the generator joins, the Newton iterates put junctions at 4 to 19 MPa, 2 to 6 times the source pressure, at every step size, on the baseline as well.
  - The baseline converges by luck: seed 6 at h = 0.05 s after 5 rejections, seed 15 at h = 0.003 s after 7.
  - D3c fails all 20 attempts. Its two-phase junction caps are 1.4 to 1.8 times higher at the same start point.
- **Mechanism: an older rule that higher caps expose.** These marginal cold starts tip under every raised cap: D2, D3 and D3c (seed 15), D3 and D3c (seed 6), D3d and the old law at 260 m/s (both).
- **Proposition (solver stage, numerical guard, not prototyped).** A trial step that puts a passive junction's pressure outside the range its neighbours can drive is cut back, as `maximumStep` already does for amounts. The range is the highest neighbour pressure plus static columns and pump or compressor rises, and the lowest less the columns. No root changes, and no hold is added. Its own item: it also covers the baseline's marginal worlds. Risk: the rise and column terms must be complete, or a real solution is cut off.

## The one corrected form, D3d (`form-d3d-mixture-choke.patch`): rejected
- **Rule:** D3c with the bulk limit bounded by the homogeneous mixture's choke, `min(throat share, √γ·K(γ)·acoustic)`. Gas-only states are unchanged; a bubbly bulk drops to 0.67 of today's value.
- **Seeds:** 199 is cured (240 ms); 6 (B) and 15 (E5) still hold.
- **World 1** rests at 37.3 Pa, inside the band of decision 2.
- **Suites:** 182/184 and 300/303. New failure: `LiquidCo2LetDownTest`, where the 5 cm let-down now leaves 0.91 kg of dry ice. Moved tests: 36 + 45 against `6c073298` (D3c: 34 + 42).
- **Quick check:** cold 190 / bundled 198 / defaults 200 (D3c 191 / 198 / 200; baseline 191 / 198 / 200). Cold 157 holds anew (E5); cold 85 held on the wall budget and is clean alone. Seed 48 costs +48 % (D3c +16 %), so 48 does not share 199's mechanism and was not traced.

**Bundled seed 988** (orchestrator): clean under D3c and D3d (607 Newton iterations; the baseline holds E6 at 2,610). It is also clean on the baseline with the old law's limit at 150 or 260 m/s. So the clearance comes from the cap's level: `ρ·100` and `ρ·χ·c_T` both scale with P, so form D adds no new pressure handle, as t40 found.

## Choices for the owner
1. Keep D3c as the form (D3d rejected)?
2. Seeds 6 and 15: open the junction pressure-range guard as its own solver item (it covers the baseline's marginal cold starts too), or accept the two holds with D3c, which still counts cold 191?
3. Seed 199: accept its cost (no hold; 2.2 s over 120 slices on one seed in 200), or open an item counting junctions in the step's state-change measure, so a first step cannot hop between steady branches?
4. Decision 2 (world 1's column band) is recorded in `FORM_D_GATES_TRACE.md`, to be applied when D3c lands.
