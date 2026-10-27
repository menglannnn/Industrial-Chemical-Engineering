# Open tank quick check (WP5): bitwise gate, vent forms A and B
2026-10-21. `3178bada` (WP1 `cb54c5d3` + WP2) against main `ef64bd06`. Gradle-free javac lane, bundled distributions only
(owner rule), 200 seeds, 4 threads, counters on. Tooling and logs: `tools/floating-roof-tanks/`. Product code unchanged.

## 1. Expected behaviour
- With no open tank, every result is bitwise the same as on main (plan section 4).
- An open tank fed a stabilised liquid (vapour pressure below 1 atm) stands at 101 325 Pa, vents nothing and spills its
  net inflow once full. Entrained gas (nitrogen) vents. A feed that boils at 1 atm is outside the design: a hold it causes
  is reported, not fixed.
- Form B vents the gas the step starts with as fixed terms. Form A removes the gas after each step, so a degassing tank
  starts every step at its bubble point. Expect A to need more phase seeds and passes.

## 2. What the runs showed
**Bitwise gate (option off): green.** `3178bada` with the patched harness equals `ef64bd06` with the old one, seed by seed,
on both distributions: same layouts, holds, end ticks, island statuses and classes. The defaults counters are identical.
The random whole-run counters differ (Newton iterations 148 621 against 148 773) only through seed 157. That seed is a
class-F wall-deadline hold on both, at the same ticks, and its work before the deadline follows the machine load. On the
other 199 seeds the counters are identical.

| Option on: 90 of 187 one-block tanks open, in 69 seeds | OFF | Form B | Form A |
|---|---|---|---|
| random, clean | 199 (F: 157) | 199 (E5: 12, one transient publication) | 197 (E1: 4, persistent; F, then E5/E8: 12, 26) |
| defaults, clean | 200 | 200 | 200 (A = B bitwise: no gas reaches a tank) |

| Counter, 196 random seeds clean in OFF, B and A (B > OFF: probably the feature, fed open tanks spill, never rest) | OFF | B | A | A / B |
|---|---|---|---|---|
| active-set passes | 24 889 | 32 428 | 37 946 | +17 % |
| Newton iterations | 127 943 | 165 174 | 200 051 | +21 % |
| Jacobian builds / residual evaluations | 38 450 / 289 622 | 51 455 / 372 704 | 74 408 / 682 552 | +45 % / +83 % |
| step attempts | 20 554 | 27 522 | 28 259 | +3 % |
| phase seeds | 209 | 218 | 3 291 | x15 |

**New holds (seeds 4 and 12 traced; 26 has the same feed as 12).** In each one, a feed that boils at 1 atm reaches an
open tank. This is outside the design (owner rule): reported, not fixed.
- Seed 12, B, E5: the bundled live crude (`tia_juana_light_methane`, 295 K, 232 kPa, all liquid) feeds the open tank. In
  the same seed's closed tank at 1 atm this feed is 8 % gas by volume. The probe `holdFuzz.deadCrude` takes the methane
  out of the crude: then seeds 12 and 26 run clean under both forms.
- Seed 4, A, E1: crude at 369 K and 261 kPa, already two-phase, flows at 48.6 kg/s into a spilling open tank. Under B it is
  STEADY. Without methane the 96 °C crude still boils: B vents 1.0 of 50.9 kg/s and is STEADY, and A still holds (E5).
  A's cause is the liquid restated at its bubble point every step (`PassiveIntervalSolver.java:375`, `ventOpen`), the
  plan's named risk.
- No fault in the open-vessel code is shown: no new hold has a non-boiling feed, and no run raised an exception.

## 3. Proposal
- Take **form B** for the 1000-seed campaign (FR1 as recommended). It has more clean seeds (199 against 197) and none of
  the pair's persistent holds. On the seeds clean in both forms, A costs 21 % more Newton iterations, 83 % more residual
  evaluations and 15 times the phase seeds.
- Run the campaign with `tools/floating-roof-tanks/hold-fuzz-bundled` and `-PholdFuzzOpenTanks=true`, bundled only.
- Owner choice: the bundled crude is live (methane) and at 96 °C boils even without it, so every crude feed into an open
  tank is outside the design: keep reporting such holds, or add a stabilised-crude option to the campaign.
