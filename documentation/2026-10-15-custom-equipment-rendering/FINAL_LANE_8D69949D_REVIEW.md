# Final lane on `8d69949d` (Opus, 2026-10-21), `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-21: "Option (a): trace cold 430 (from its 8-block gate world) and bundled 889 physics first, fix structurally, affected parts only, then the final lane." The head carries `d139fe93` (A) and `8d69949d` (C + D, both in `COLD430_889_REVIEW.md`) on `e5357594` (`FINAL_LANE_E5357594_REVIEW.md`, "fl6"). Reference: m23 at `8be1efef`. Logs: `tools/new-units/logs/final-lane-8d69949d-2026-10-21.tar.gz`. Code guide: no change (not merged).

## Verdict: gates green, cold 976 and bundled 993 better than m23, 430 and 889 cleared, but NOT GREEN: three new worse worlds, all from A (section 5)
## 1. Gates (one run, m23's lane script, alone; another project's two 2-thread jobs ran beside it)
- Full test: 1 572 tests, 0 failures, 0 errors. Chain-100 exact (deviation 0). Fluid GameTests 31/31.
- Printed lines against fl6 (MOVED_REFERENCES round 11):
  - `PUMP_CAVITATION_ARRIVAL` is back on its m23 value: suction limit, 14 675.5 Pa (D: the floor of the arriving liquid, 22 mK warmer), steam share 0. fl6 had the head limit, 14 659.8 Pa and a steam share of 3.8e-5.
  - `D46_GATE`: created water is unchanged at 1.2865 kg. The bound rose from 1.653 to 3.214 kg, by 1.561 kg, which is one 20 mm liquid-water buffer's m_J (1.56 kg).
  - The rest is round-off (STATIC_COLUMN drop line 3.3e-4 of 9.6e-9 kg/s, MELT_GATE 1e-10 kg/s).
## 2. Campaign, 1000 seeds, 8 workers
| Distribution | m23 | fl6 (`e5357594`) | `8d69949d` |
|---|---|---|---|
| cold random | 962 | 975 | 976 |
| bundled random | 990 | 993 | 993 |
| bundled defaults | 1000 | 1000 | 999 |
- Against fl6:
  - cleared: cold 244, 430, 788, 994 and bundled 889;
  - newly held: cold 311 (E5, 28 blocks, tick 2885), 567 (B, 20 blocks, 1896; E5 on m23) and 768 (F), bundled 678 (F) and defaults 397 (E6, 18 blocks, 3257).
- Against m23:
  - cold: 18 cleared, 4 newly held (311, 475, 768, 855);
  - bundled: 4 cleared, 678 newly held.
- New C/F holds (against m23 or fl6), replayed alone in one batch with one worker:
  - 768 and bundled 678 are clean: load.
  - 475 holds F from 1137 (as on fl6, real) and 907 F from 2875 (as on fl6, better than m23).
  - 833 holds F from 1306 (F on m23 from 1403).
## 3. Attribution of the new worse worlds (one seed each, javac lane, one worker)
- On `d139fe93` (A alone), cold 311, cold 567 and defaults 397 hold exactly as on `8d69949d`, and all three were clean on fl6. They come from A, not from C or D.
- 855 (E6 now, F on fl6, clean on m23) and 919 (E6) stay as open worlds.
## 4. Cost (counters; time only indicative, 4 threads of other load)
- Newton iterations:
  - cold 2.92 M, against fl6 3.10 M (−5.6 %) and m23 2.77 M (+5.4 %, with 14 more seeds running to the end);
  - bundled 0.823 M (m23 0.811 M); defaults 71 534 (m23 74 403).
- Time over seeds clean in both: cold +5.2 % against fl6 and +10.2 % against m23; bundled −13.7 % and −25.9 %; defaults +0.3 % and −20.8 %.
## 5. Proposition for the owner
- A clears 430 by sweeping first wherever a Newton step leaves half the hull. It may also sweep where the Newton would have reached the root (untraced); the balance's own comment records that "a sweep across a choked junction's flat range can walk away from a root the Newton has all but reached".
- (a) Recommended: trace the two smallest new worlds from A physics first, defaults 397 (18 blocks: a water pump, tanks and a 200 kPa valve) and cold 567 (20 blocks). Then a form of A that keeps the Newton where it descends by more than round-off is checked on 430, those two and the quick check.
- (b) Keep D + C and drop A. 430 holds again. 311, 567 and 397 were clean on fl6, which has no A; that they stay clean under C + D, and what cleared 788 and 994, are untested.
- (c) Merge prep as is. Net: cold +14 against m23 and +1 against fl6, bundled +3, defaults −1.
- Not merged or pushed.
