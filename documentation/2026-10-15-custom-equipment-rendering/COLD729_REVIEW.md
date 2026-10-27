# Cold fuzz seed 729 (Opus, 2026-10-20), `ff71d370` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-20: "Trace cold seed 729 on this branch (a6a305a1's split where a CO2 gas meets LNG), physics first, fix it structurally, re-run only the affected parts." Probes and prototype: `tools/new-units/od4-holds/r5/`. Logs: `tools/new-units/logs/cold729-2026-10-20.tar.gz`. Code guide: no change (not merged; at the merge the network chapter records the arrival check's stock-alone case).

## 1. Physics first (island 16, 11 blocks, six junctions)
- Two sources fill two 1 m3 N2 tanks (1 atm) through 20 and 50 mm lines and a filter:
  - a CO2-rich gas source at 257 K and 3.16 MPa (the spec's fractions: 56 % CO2, 25 % CH4, 19 % N2);
  - an LNG source at 162 K and 2.65 MPa.
- Expected: the LNG cold freezes the gas's CO2 (18.3 kg of crystal in the gas-side tank, 183 K). The LNG source stops once the island passes 2.65 MPa. The gas source brings everything to its 3.16 MPa and the flow ends. 0.10.0 reaches that: STEADY, no flow since 97.8 s.
- At rest each junction is what it holds. The filter's two ends and the gas-side tee hold 1.1 to 3.5 kg of the cold mixture (172 to 209 K, two-phase, CO2 crystal in two of them). A few g/s of gas still reach them; they stay cold and two-phase.

## 2. What the code did instead (`ca21bab8`; held B at 62.75 s)
- Each step starts from the balance, which states a junction as what is delivered to it: the 257 K gas.
- The arrival check (`arrivingPhases`) lays each junction out for its stock plus what arrives at the step's start flows. Since 0.7.0 it has skipped a junction nothing arrives at. So the three junctions at rest kept the gas seed, two of them as gas beside crystal.
- A gas layout cannot hold that stock's enthalpy at 3.14 MPa. The tee's enthalpy row stayed at 0.174 at every step size down to 9.5e-7 s. The pass never converged, so no phase check ran, and the island held.
- 0.10.0 has the same skip, but its path to rest did not trip it (its tee ends two-phase, without crystal). The branch's path, changed by `a6a305a1`, did.

## 3. Fix (`ff71d370`; owning stage: the arrival seed)
- Where nothing arrives, the junction's mixture is its stock alone. Its own split of that stock is formed as for any junction: the engine's PH answer is the start, the rows' closure the verdict.
- A seed that already holds every phase of its stock is kept to the bit. The `!arrives` exception is removed (3 lines).

## 4. Results (javac lane; chain-100 on Gradle)
- 729 alone: clean, STEADY with no flow since 97.8 s, as on 0.10.0. 5 984 Newton iterations, against 5 887 on 0.10.0 and 15 021 (held) on `ca21bab8`.
- Named worlds, all clean: cold 509, 592, 170, 757, 768, 285, 583, 743, 145, 322, 354 and 889, each at exactly `ca21bab8`'s iteration count; bundled 583, 743, 145, 322, 354, 889 and 285.
- Quick check (200 seeds, 4 workers): held sets identical to `ca21bab8`'s (cold 192, bundled 195, defaults 200).
- Newton iterations against `ca21bab8`: cold 662 178 against 651 223 (+1.7 %); bundled 168 234 against 171 977 (−2.2 %); defaults 13 125, identical.
- Targeted classes 148/149 (the one failure is the known lane failure), every reference line identical to `ca21bab8`'s. Chain-100 exact.

## 5. A second finding, not the hold: D61 labels (proposed, not applied)
- Before the rest, the gas-side tank stands at 3.163 MPa, 3 kPa above the gas source, and sends its slurry back. It passes the tee (207 K, the crystal stays), then enters the gas junction beside the source (244 to 257 K): 0.035 to 0.1 kg/s at 1.9 % crystal. That is 1 to 2 g/s of crystal that must melt there, about 1 kW.
- The two stages book that crystal differently:
  - The Newton's D61 split reads the donor's population list. A Newton state whose seed held no solid names none, so the tee's crystal arrives in the rows unmelted.
  - The reconstruction labels the same crystal with the island's populations and melts it.
- The equation gate therefore refuses the gas junction's solid and enthalpy rows by an amount proportional to the step: 9 refusals on 0.10.0, 47 on `ca21bab8`, 12 on `ff71d370`. Each refined step passes at a small dt, so this costs refinements but held nothing here.
- Prototype A (`r5/prototypes/a-junction-crystals-island-labels-against-ca21bab8.patch`): `JunctionCrystals` carries the island's populations and splits an unlabelled stream over them. On 729 with the arrival fix: 0 refusals, clean in 8 930 iterations, but the island ends FULL with crystal in one vessel instead of STEADY with two. Not measured further.

## 6. Proposition for the owner
- (a) Keep `ff71d370` and re-run the final lane (full suite, campaign) before merge prep. The arrival check now runs at junctions nothing reaches too, and only the campaign covers every distribution at full size.
- (b) Decide whether D61's label gap is its own item, before or after the merge: prototype A, a quick check, and an explanation of 729's FULL ending.
