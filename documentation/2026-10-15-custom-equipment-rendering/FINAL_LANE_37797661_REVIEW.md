# Final lane on `37797661` (Opus, 2026-10-20), `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-20: "Run the final lane (full suite, GameTests, 1000-seed campaign) on 37797661 against m23. No merge or push." The lane is m23's, re-pointed: Gradle alone, no dev client. Logs: `tools/new-units/logs/final-lane-37797661-2026-10-20.tar.gz`. Code guide: no change (not merged).

## Verdict: NOT GREEN by one world, cold 654 B, from `37797661`
- Gates are green: full suite 1 572 tests, 0 failures, 0 errors; chain-100 exact; GameTests 31/31.
- Campaign against m23: cold 972 clean against 962; bundled 989 against 990; defaults 1 000 = 1 000.
- Alone, on one worker, `37797661` against `2def39dc`: every newly held world but 654 is clean, or held the same on both trees (section 2).
- Machine: another project's `geosim` jobs held about 6 of the 16 cores during the whole lane. The cold campaign took 553 s of wall time against 302 s on `2def39dc`'s lane (fl3). Wall-deadline (F) holds are therefore load here, and cost is read from counters.

## 1. Campaign, seed by seed
- Cold against m23:
  - Newly clean (13): 72, 178, 272, 285, 316, 397, 543, 567, 628, 694, 705, 866, 919.
  - Newly held: 183 F, 654 B, 698 F.
  - Class changes: 6 B→F, 446 E6→E5, 555 B→E5, 673 E5→F, 957 B→E5, 994 E6→F (at 9844).
- Cold against fl3 (`2def39dc`): 72, 543 and 705 newly clean; 183, 654, 673 and 698 newly held.
- Bundled against m23: 678 F newly held (and 579 E10→E5, as on fl3). Defaults identical.
- New C/F holds against either base, all replayed alone: cold 6, 183, 651, 673, 698, 994 and bundled 678; 654 was replayed too.
## 2. Alone (one worker, two trees side by side under the same load; Newton iterations)
| Seed | `2def39dc` | `37797661` | Reading |
|---|---|---|---|
| 698 | clean, 39 145 | clean, 39 513 | load: 117 wall deadlines in the campaign (417 s against 5.6 s) |
| 183 | 3 holds (deadline) | clean, 253 481 | wall time only, as in rounds 5 to 7 |
| 673 | 1 hold | clean, 28 942 | load |
| 678 (bundled) | clean | clean | load |
| 6, 651 | 7 / 13 holds | 7 / 13 holds | the same on both, not `37797661`'s |
| 994 | held at 5144 (E6) | held from 11044 | first hold a wall deadline, then Newton holds on the path it left; clean (112 584) when replayed earlier |
| 654 | clean, 4 976 | 9 holds B, 5 974 | from `37797661` |
## 3. 654, the one worse world (not traced: lean rule, owner's call)
- The world: a CO2-rich gas generator (281 K, 3.22 MPa) and an LNG generator (110 K, 180 kPa), with compressors, filters, a pump and a tank.
- At tick 2629 a filter, a compressor and a void are placed. Island 50's next interval holds at 2631 with "Matrix values must be finite" (class B, trace underflow). Island 53 follows at 2668.
- Clean on m23 and on `2def39dc` (STEADY on every island). It is the only new non-budget hold.
## 4. Counters
- Cold Newton iterations: 5 494 117, against 2 975 100 on fl3 and 2 772 420 on m23. Step attempts: 338 011 against 245 852.
- The rise is the load. Each wall-deadline hold burns its interval's iterations: 698 alone used 39 513 iterations clean, but 412 s under deadlines in the campaign.
- The alone pairs give +4.4 % over seven seeds, nearly all of it 994's held path. The 200-seed quick check gave −1.4 % (`COLD994_REVIEW.md`).
- Equation-gate refusals: 5 612 against 6 392.
- Bundled Newton: 820 852 against 824 332 (−0.4 %); gate refusals 613 against 688. Defaults identical.
## 5. Proposition for the owner
- (a) Recommended: trace 654 next on this branch. Its hold follows a placement and comes from the arrival layout rule, so physics first, with the affected parts re-run, then merge prep.
- (b) Merge-prep now, with 654 recorded as a known worse world against 13 cold worlds cleared since m23.
- A cost judgement needs a run with the machine free (no `geosim` jobs); this lane's wall times and deadline holds cannot judge it.
