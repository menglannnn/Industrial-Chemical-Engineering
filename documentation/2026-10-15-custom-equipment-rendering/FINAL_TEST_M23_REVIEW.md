# Final test m23 of the custom equipment rendering merge (Opus, 2026-10-20)

## Verdict: GREEN
G2 chain-100 is exact. The GameTests pass 31/31. The defaults counters are identical. Against 0.9.2 (m20), two worlds are newly held, both cold and both F. Cold 285 is OD4's own world: with OD4 off it repeats 0.9.2's counters exactly. Cold 866 is not worse: OD4 does not touch it, and its work is 9 % below 0.9.2's. It holds only on wall time, and is clean when run alone. Bundled 145, 889, 322, 354 and cold 583, 743 are clean.
The lane ran at `8be1efef`, the only Gradle run. Owner scope: no full unit suite. JAVA_OPTS=-Xshare:off, no dev client, no code change, no commit. Five owner geosim JVMs were running (about 10 of 16 logical CPUs). The defaults campaign has identical counters, but its seedMsSum is 70.5 s against m22's 45.5 s. Logs and scripts: `tools/new-units/logs/merge-final-lane-m23-2026-10-20.tar.gz` (main checkout).

## 1. G2 chain-100 exact: deviation 0. Fluid GameTests: "All 31 required tests passed".

## 2. Campaign: 1000 seeds, 8 workers (`cmp-seeds.txt`, against m20, m21, m22)
| Distribution | Clean m20 / m21 / m22 / m23 | Newly held vs m20 | Newly clean vs m20 | vs m21 |
|---|---|---|---|---|
| cold | 955 / 955 / 956 / 962 | 285 F (28 blocks, t3421), 866 F (22, t10277) | 35, 118, 547, 592, 629, 729, 757, 779, 897 | held: none; clean: 118, 547, 592, 629, 729, 768, 897 |
| bundled | 990 / 988 / 988 / 990 | none | none | held: none; clean: 145, 889 |
| defaults | 1000 each | none | none | none |
- Defaults: the whole COUNTERS line equals m20, m21 and m22 (74 403 Newton iterations, 26 391 Jacobian builds).
- Against m22, newly clean: cold 118, 583, 629, 729, 743, 897 and bundled 322, 354. Newly held: none.
- m21's cold F holds: 285 is still held (F at 3421 and 7971). 866 is held in the campaign (F at 10277); its alone runs are below.
- Held in both m20 and m23, with a changed class: 651 B→F, 6 E6→B, 555 E5→B, 673 F→E5, 694 F→E5, 919 B→E5, 994 E5→E6, bundled 116 E9→E5.
- **New C/F holds, alone** (`06-alone`, one worker): cold 285, 651 and 866 all hold F.
  - 651 was held at 0.9.2 (B at 849, nonfinite matrix, then no more work). It holds F with OD4 on and off alike, so it is not a worse world.
  - Bundled 157 is new F only against m21; it was F at m20.
- **OD4 off at 8be1efef** (scratch copy, `od/rule-off-od4-half-link.patch`, `09-od4off`): 285 is clean and its counters equal 0.9.2's (2 680 iterations). 651 and 866 still hold.
- **0.9.2 at the same load** (`10-m20-sameload`): 285 is clean, 651 holds B at 849, and 866 is clean (15 639 iterations, 52 s).

## 3. The two new worse worlds against m20
- **Cold 866** (22 blocks, island 34).
  - Physics: an LNG generator at 533 kPa and 131 K feeds two 1 m3 tanks through 10 and 20 mm lines. Every other branch is a dead end. Expected steady state: the island is at rest at the generator's pressure plus the LNG head of about 4.3 kPa/m (545.8 kPa at z = -3, 12.9 kPa above). The tanks are 38 to 42 % liquid under 136 K vapour, which condenses slowly. CO2 frost (33.8 kg) blocks three lines.
  - The code reaches exactly that state: STEADY, replaying 1.2e-4 kg/s from 523.9 s. Its 0.9.2 end state is the same.
  - The island has no valve or pump, so OD4 does not touch it: with OD4 off its counters are identical (`11-trace`).
  - Its work is below 0.9.2's: 14 243 Newton iterations against 15 639, 11 067 Jacobian builds against 12 333.
  - It holds F (wall deadline) at 10177, 15 s before its steady certificate, in the 8-worker campaign and as the third seed in a one-worker JVM (85 s). A single-seed run is clean in 50.8 s.
  - So it is a wall-budget edge world, not a worse one. m21's attribution of 866 to OD4 was wrong: there, too, OD4 on and off had the same counters (15 693 and 15 639).
- **Cold 285** (28 blocks, island 43): OD4's own world.
  - Physics: LNG at 264 kPa and 120 K passes through a 50 mm suction, then a pump (0.01 m3/s, 500 kPa) and 10 mm lines, to three voids at 1 atm. Expected steady state: a constant throughflow, limited by friction in the 10 mm lines and the pump's head limit. The liquid flashes near the voids and cools itself. Nothing in it is singular.
  - With OD4 the pump's discharge half link is 10 mm (the line's bore) instead of the block's own.
  - The code reaches that state:
    - 0.802 kg/s, of which 0.566 goes straight to a void and 0.236 to a junction at 125.3 kPa and 115.4 K (1.3 % vapour), which splits it 0.115 / 0.121;
    - pump mode PUMP_HEAD_LIMIT; discharge junction at 181.9 kPa.
  - At 0.9.2: 1.040 kg/s.
  - The cost: 26 113 Newton iterations against 2 680, and 301 phase seeds of which 300 are refused (at 0.9.2: 3). Two intervals miss the wall budget (F at 3421 and 7971).
  - Since fix 2 (`8fd61a13`, the PH seed; m22's fix-off runs), the island also never rests: it ends FULL. At m21 it ended STEADY at 0.802 kg/s.

## Proposition for the owner
- Release 0.10.0 on this lane.
- Note 285 as an open cost item of the OD4 world: a flashing 10 mm discharge junction whose phase seed is refused 300 times and which no longer rests since `8fd61a13`.
- Note 866 as a wall-budget edge world. It was held in the m21, m22 and m23 campaigns at 58 to 87 s per seed, and clean at 50 to 61 s. 0.9.2 was clean at this load (52 s).
- Code guide: no change (test lane only). This lane did not touch `documentation/INDEX.md`.
