# Final lane on `c7b883cb` (Opus, 2026-10-21), `claude/stoic-bell-ae6019`, not merged or pushed
Owner decision 2026-10-21, option (b) of `FINAL_LANE_8D69949D_REVIEW.md`: "drop A (d139fe93), keep the water floor fix (8d69949d), then run the final lane". `c7b883cb` reverts `d139fe93`. The head is `e5357594` (fl6, `FINAL_LANE_E5357594_REVIEW.md`) plus C + D (`8d69949d`, `COLD430_889_REVIEW.md`). Reference: m23 at `8be1efef`. Logs: `tools/new-units/logs/final-lane-c7b883cb-2026-10-21.tar.gz`. Code guide: no change (not merged).

## Verdict: better than fl6 with nothing newly held (bundled 889 cleared); against m23 the same four worse worlds as fl6 (430, 788 E6; 475, 855 F alone)
## 1. Gates (one run, m23's lane script, alone on the machine)
- Full test: 1 572 tests, 0 failures, 0 errors. Chain-100 exact (deviation 0). Fluid GameTests 31/31.
- Printed lines against fl6 (MOVED_REFERENCES round 12):
  - `PUMP_CAVITATION_ARRIVAL` is m23's value again (suction limit, 14 675.5 Pa, steam share 0).
  - `D46_GATE`: created water is unchanged at 1.286502 kg. The bound rose from 1.653 to 11.410 kg, by 9.757 kg, which is one 50 mm liquid-water buffer's m_J (0.05 s × 100 m/s × 994 kg/m3 × 1.96e-3 m2). The world's pump is 50 mm, and its suction junction is now minted on liquid water.
  - The HoldClassWorldsTest lines that moved on fl7 (MELT_GATE, STATIC_COLUMN) are fl6's again; they moved with A.
## 2. Campaign, 1000 seeds, 8 workers
| Distribution | m23 | fl6 (`e5357594`) | fl7 (`8d69949d`, with A) | `c7b883cb` |
|---|---|---|---|---|
| cold random | 962 | 975 | 976 | 975 |
| bundled random | 990 | 993 | 993 | 994 |
| bundled defaults | 1000 | 1000 | 999 | 1000 |
- Against fl6:
  - cold: the same held seeds with the same classes and first ticks;
  - bundled: 889 cleared and none newly held; 431 F → E5 at the same tick (as on m23);
  - defaults: equal.
- Against m23:
  - cold: 17 cleared, 4 newly held (430 E6, 475 F, 788 E6, 855 F), all held on fl6 the same way;
  - bundled: 4 cleared (6, 116, 123, 151), none newly held.
- New C/F holds against m23 (cold 244, 475, 855, 907), replayed alone in one batch with one worker:
  - 244 is clean (load).
  - 475 holds F from 1137 (3 holds) and 855 F from 1886 (9 holds), as alone on fl6: real budget holds.
  - 907 holds F from 2875 with 8 holds. On fl6 alone it held 10 times; on m23, 19 times, B from 2052.
## 3. Cost (counters; time only indicative)
- Newton iterations:
  - cold 3.05 M: fl6 3.10 M (−1.4 %), m23 2.77 M (+10.2 %, with 13 more seeds run to the end);
  - bundled 0.930 M: fl6 0.810 M (+14.7 %), m23 0.811 M;
  - defaults 69 218: fl6 69 184, m23 74 403.
- Equation-gate rejections, cold: 2 789 (fl6 6 261, m23 100).
- The bundled rise is not attributed per seed (no per-seed counters in an 8-worker campaign). 698, held E8 in both with 37 holds, took 19.7 s against 14.1 s.
- Time over seeds clean in both (no other load this time): cold −3.8 % against fl6 and +2.2 % against m23; bundled −28.7 % and −40.4 %; defaults −28.4 % and −43.6 %.
## 4. Proposition for the owner
- (a) Recommended: merge prep on `c7b883cb`. Nothing is newly held against fl6. Against m23: cold +13, bundled +4, defaults equal, gates green.
  - Open worlds: 430 and 788 (E6), 475 and 855 (F alone), plus the residual design item of `STILL850_REVIEW.md`.
  - The bundled Newton +14.7 % is the one cost item to watch.
- (b) First trace 475 and 855 (F alone, 27 and 26 blocks; clean on m23), physics first, then the final lane.
- Not merged or pushed.
