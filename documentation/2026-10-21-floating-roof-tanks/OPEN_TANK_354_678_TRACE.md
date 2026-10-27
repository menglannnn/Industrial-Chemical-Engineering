# Open tank holds 354, 678, 598, 12: trace and a finite release rate (prototype)
2026-10-21, `86776654`, reverted harness (live crude, drawn temperatures; owner: boiling feeds that vent are in design).
Gradle-free javac lane on `git archive` exports, one worker per seed, 4 threads for the quick check. Product code and
worktree unchanged; code guide: no change. Tooling `tools/floating-roof-tanks/trace-354-678/`, logs
`tools/floating-roof-tanks/logs/trace-354-678-logs-2026-10-21.tar.gz`.
## Reproduction (bundled random, open tanks; 895 and 26 are clean at `86776654`)
| Seed | `86776654` | Mechanism | Trace |
|---|---|---|---|
| 598 | E6, 13 holds (crude 345 K) | below | minimized 14 -> 7 blocks, traced |
| 678 | E5, 14 holds (water now 341 K) | below | minimized 23 -> 16 blocks, traced |
| 354 | E4, 10 holds, gate 1.32e-8 | water tank sparged with CH4: its gas each step is that step's arrivals | classified only |
| 12 | E5 (quick check, 27 blocks) | crude 295 K into an open tank | classified only |
## 1. Expected behaviour (flows are finite: over a step dt -> 0 nothing leaves, the step ends at its start state)
- 598: crude 49.9 kg/s (345 K, 255 kPa) and water 0.43 kg/s (296 K) enter one open tank. At 1 atm the crude flashes:
  0.63 kg/s of light ends (about 0.35 m3/s of vapour) vent, the liquid cools to its 1-atm bubble point, 341.1 K, and
  49.7 kg/s spill. Steady: out = in, the level at the shell top, almost no vapour under the roof.
- 678: crude 61 kg/s (310 K) flashes its methane and spills at 308 K; CH4 at 3.44 bar enters the tank bottom through a
  junction at the 100 m/s cap (0.48 kg/s); gas leaves by the vent (about 1.1 kg/s in all) and the VAPOR line (0.57 kg/s).
## 2. What the code did instead
- An open vessel releases its whole start gas and its whole start liquid above the shell top over each step, whatever the
  step's length: a rate of excess/dt, unbounded as dt -> 0. A halved step is no closer to its start, so refinement
  never helps, and the held gas and band swing with the last step size (678 tank between slices: vapour 0.08 to 0.90 m3,
  liquid 2.006 to 2.063 m3).
- 598, step after an accepted 2.36 s substep: the tank starts with 0.83 m3 of vapour, all of it vented; the end must hold
  only what flashes within dt (0.35 m3/s x dt). 20 sizes from 0.21 s to 6.5e-6 s fail (Newton limit, residual 1.3e-5
  to 13.7) on the tank's steam saturation row and Dalton row; the iterates' vapour runs 0.003 to 20 m3.
- 678: all 20 sizes from 1 s to 1.9e-6 s fail (residual 2.9 to 11.6) at the methane junction feeding the tank bottom:
  a trace liquid of crude heavies (about 1e-7 of its mass, carried from the closed crude tank beside it) takes equilibrium rows
  to 3 to 11, the choked CH4 line's law row stays at 0.596, the tank's rows do not move. Not probed apart; prototype A
  clears it. Closed tanks: minimized 598 clean, minimized 678 only an F (wall deadline).
## 3. Options for the owner (physics forces a finite release rate, not its time; recommended: A at tau = 1 s, or B)
- **A (forced structure, prototyped):** a step releases the share min(1, dt/tau) of its start excess (gas, and liquid
  above the shell top); tau one constant. The current rule is its dt >= tau limit, so the end-empty path (D54 kept node)
  stays for long steps. Deletes the vent's own capacity rule (vent and spill take one subtraction). Risks: tau is
  unforced; the holdup is rate x tau (598: vapour 0.44 m3 at tau 1 s, 1.74 m3 at 5 s); per-interval status rates
  swing (598 minimized, tau 1 s: holdup 1755 to 1822 kg).
- **B:** A with two times: vent tau 5 s (one engine cycle, "vent them next cycle"), spill tau 1 s or a weir law. At tau 5 s
  for spill too, a 257 kg/s tank stood 1.5 m3 above its rim and its line flow fell 4.4 % (LIQUID-port head). Not run.
- **C:** implicit release: vent and spill as unknown outflows of the step (end at or below bubble point and rim). Deletes
  the lag, the constant terms, `releasesAll` and the vent branch of `drainedSeeds`; adds unknowns and a bubble-point/rim
  active set, and reverses the owner's 895 rule (a gas-only tank becomes void-like).
## 4. Prototype A (`trace-354-678/release-share-86776654.patch`, +21/-15, `-Dicheme.openTank.releaseTime`)
- Seeds 598, 678, 354, 895, 12 (one worker): all clean at tau 1 s and at tau 5 s. `OpenTankTest` 8/8,
  `OpenTankRuntimeTest` 2/2, `PresentationBucketTest` 10/10.
| Quick check (200 seeds) | Clean | Step attempts | Passes | Newton iterations | Residual evals | Gate rej. | Seeds differing |
|---|---|---|---|---|---|---|---|
| random, base `86776654` | 199 (12 E5) | 24 773 | 30 297 | 173 652 | 435 137 | 0 | - |
| random, A tau 1 s | 200 | -4.9 % | -5.7 % | -1.5 % | +0.0 % | 2 | 9 (all open-tank) |
| random, A tau 5 s | 200 | -13.5 % | -15.0 % | -21.4 % | -17.0 % | 3 | 25 |
| defaults, A tau 1 s / 5 s | 200 / 200 | +0.2 % / -2.1 % | +0.2 % / -1.9 % | +0.3 % / -4.6 % | +0.2 % / -6.8 % | 0 | 1 / 11 |
