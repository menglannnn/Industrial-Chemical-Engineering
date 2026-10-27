# Cold fuzz seed 994 (Opus, 2026-10-20), `37797661` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-20: "Keep A (416f8601 + 2def39dc). Next: trace cold 994's vanishing-liquid hold, physics first, re-run only the affected parts." Follows `D61_LABELS_REVIEW.md` section 5. Probes and prototype: `tools/new-units/od4-holds/r6/`. Logs: `tools/new-units/logs/cold994-2026-10-20.tar.gz`. Code guide: no change (not merged; at the merge the network chapter records `laidOutFor` in the arrival check).

## 1. Physics first (island 50, 15 nodes)
- Two sources share one network: a CO2-rich gas at 289 K and 1.0007 MPa, and LNG at 140.9 K and 1.047 MPa, 46 kPa higher.
- The LNG pushes through two junctions toward the gas side and fills two cold tanks. CO2 freezes where the gas meets it: one junction's three pipes are closed by beds. Two hot gas tanks (352 and 354 K) sit on the gas side.
- The gas side's four junctions each hold 3 to 14 g of warm gas, with stock enthalpy −10 to +44 kJ/kg.
- Expected: over a step of 1 µs nothing reaches them, so each stays what it holds, and the step is trivial. Over a long step (dt ≫ m/Q, about 0.1 s) a junction becomes what is delivered to it.
## 2. What the code did instead (A, `2def39dc`; held E6 from tick 5144, then from 7119 to the end)
- At the failing step's start the four junctions' property states are the balance's statement of what is delivered to them: 215.5 K, gas plus 6.5 % (molar) of a CO2-rich liquid, h −131 to −136 kJ/kg. Junction 14 is stated at −741 kJ/kg against its stock's −373.
- The arrival check (`arrivingPhases`) forms each junction's mixture: its stock plus dt times what arrives. At 1.1e-6 s that is the warm gas.
- But the check kept any seed that held every phase of its mixture, so it kept the two-phase seed with its extra liquid.
- A Newton pass cannot empty a phase, so every refinement down to 1.1e-6 s started in a layout its answer was not in:
  - pass 0 ended singular or stalled on the four enthalpy rows (residual 0.7 to 1.0 even at 1e-6 s);
  - the later passes ran those junctions to 380 to 390 K.
- m23 (0.10.0's lane) and A hold here; `ca21bab8` and `ff71d370` only stepped past it. A's fallback melts at most 6e-11 kg/s here. The traces of A and `ff71d370` first differ at the 14th digit of one step size (round 7).
## 3. Fix (`37797661`; owning stage: the arrival seed)
- `laidOutFor(mixture, seed)`: a seed is kept only where its hydrocarbon phases (liquid, gas) are its mixture's and it holds every water phase of the mixture. Otherwise it becomes the mixture's own split, as before.
- Water keeps the superset. A seed's water may be the entry trace of decision D10 (4.6e-12 mol of steam), which a dry mixture lacks.
- Form D1 (`r6/prototypes/`) compared all phases for equality and dropped that trace. Bundled 157 then held E4 at tick 493: the gate refused n5 by 1.67 × dt, with the reconstruction booking the arriving water the rows had no unknown for.
- The change is 3 code lines plus javadoc; two forms were tried.
## 4. Results (javac lane, one worker unless stated; Gradle for chain-100)
- 994 alone: clean, FULL, 8.77 kg of crystal in 3 vessels, 112 584 Newton iterations (A held; `ff71d370` clean at 122 771).
- Quick check, 200 seeds, 4 workers, against A:
  - cold 194 against 193 (72 cleared, was E5), nothing newly held, Newton −1.4 %;
  - bundled 196 against 195 (6 cleared, was E6), nothing newly held, Newton −3.9 %, gate refusals 733 → 545;
  - defaults identical.
- Named worlds: 13 cold and 7 bundled, all clean.
  - Cold Newton is +2.1 % against `ff71d370`, nearly all of it 729 (5 984 → 9 152, which ends FULL as with A).
  - Bundled: one seed moves, 322 by +18.
- Targeted classes: 148/149, the one failure being the known lane failure. Chain-100 exact.
- Two `HoldClassWorldsTest` lines move (in `MOVED_REFERENCES.md` round 8):
  - The 778 melt gate's tank line goes from −1.02e-4 kg/s to exactly 0. The gate asserts |q| < 1e-3.
  - `VANISHING_RULE` node 17's state mass goes from 0.505 to 0.514 kg at the same density. A junction's stock is its buffer, so that mass carries no physics.
  - `PUMP_CAVITATION_LNG` and the rest move at 1e-13.
- Machine note: another project's `geosim` jobs held about 8 cores during these runs. Wall-deadline (F) holds are load artefacts here, so cost is read from counters. Cold 183 F held on D1 and on `2def39dc` alike.
## 5. Residual
- A seed the new rule replaces loses a D10 water trace when its mixture holds no water. The existing missing-phase replacement does the same. Neither showed in the quick check; if one holds, the trace belongs in the mixture's amounts.
## 6. Proposition for the owner
- (a) Recommended: keep `37797661`. The branch then needs its final lane (full suite, GameTests, campaign) before merge prep, because the arrival check now replaces seeds in both directions.
- (b) Still open from round 7: the certificate item (729 ends FULL on round-off flows past a filter) and the branch's gate-refusal rise (100 on m23, about 6 400 on the branch; bundled quick 545 here).
