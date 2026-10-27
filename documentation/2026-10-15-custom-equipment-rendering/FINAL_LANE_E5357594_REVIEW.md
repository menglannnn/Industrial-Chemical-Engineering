# Final lane on `e5357594` (Opus, 2026-10-21), `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-21: "Option (a): commit G3, change the 889 gate to assert the cavitation physics ... trace still pipe 850 on G2 physics first, then run the final lane." The head carries `bde9013c` (`COLD654_REVIEW.md`), `a6b14b22` + `aca82e8f` (`BALANCE_CONVERGENCE_REVIEW.md`) and `e5357594` (`STILL850_REVIEW.md`) on `37797661` (`FINAL_LANE_37797661_REVIEW.md`, "fl5"). Reference: m23 at `8be1efef`. Logs: `tools/new-units/logs/final-lane-e5357594-2026-10-21.tar.gz`. Code guide: no change (not merged).

## Verdict: gates green and the campaign better than m23 (cold 975 against 962), but NOT GREEN by new worse worlds: 475 and 855 hold F alone, and 430 (from `a6b14b22`), 788, 919 and bundled 889 are newly held E (section 5)
## 1. Gates (one run, m23's lane script, alone on the machine; one geosim job of another project held 2 threads)
- Full test: 1 572 tests, 0 failures, 0 errors.
- Chain-100 exact (deviation 0).
- Fluid GameTests 31/31.
## 2. Campaign, 1000 seeds, 8 workers
| Distribution | m23 | fl5 (`37797661`) | `e5357594` |
|---|---|---|---|
| cold random | 962 | 972 | 975 |
| bundled random | 990 | 989 | 993 |
| bundled defaults | 1000 | 1000 | 1000 |
- Cold against m23: 17 cleared and 4 newly held. The cleared seeds:
  - E5: 72, 161, 178, 543, 567, 673, 694, 705, 727;
  - F: 272, 285, 628, 866;
  - B: 397, 555; E6: 316; E2: 787. Against fl5: 8 cleared, including 654 (B) and 183, 673, 698 (F), and 5 newly held.
- Newly held by class (from the campaign's classifier):
  - cold E6: 430 (12 blocks, tick 406, Newton iteration limit at pass 0), 788 (26 blocks, tick 1570) and 919 (15 blocks; held E5 on m23, clean on fl5);
  - cold F: 475 and 855;
  - bundled E2: 889 (23 blocks, singular Jacobian at pass 0, tick 2876).
- Held on both with a changed class: 6 B → E5, 29 B → E6, 957 B → E6 (same first tick), 994 E6 from 4744 (the crystal family, `BALANCE_CONVERGENCE_REVIEW.md` section 5).
- New C/F holds against m23: cold 244, 475, 855, 907; bundled 431. All five are replayed alone (section 3).
## 3. New C/F holds replayed alone (one worker, one batch)
- Cold:
  - 244 holds F from tick 10545 (1 hold). On m23 it held 11 times, E6 from 1953. Better.
  - 907 holds F from 2875 (10 holds). On m23 it held 19 times, B from 2052 then E. Better.
  - 475 holds F from 1137 (island 45, 34.3 kg of crystals in 6 vessels) and 855 holds F from 1886. Both were clean on m23 and fl5. These are new budget holds that are real, not load.
- Bundled 431 holds E5 at 1377, as on m23. Its campaign F was load.

## 4. Cost (counters; time only indicative under the other project's load)
- Cold Newton iterations: 3.10 M, against 2.77 M on m23 and 5.49 M on fl5 (fl5 ran under ~6 cores of load, with deadline holds). Totals mix held and clean seeds, and 13 more seeds run to the end than on m23.
- Time over seeds clean in both: cold +6.6 % against m23 and −6.2 % against fl5; bundled −15.4 % and −6.2 %.
- Bundled Newton 0.810 M (m23 0.811 M); defaults 69 184 (74 403).
## 5. Proposition for the owner
- The worse worlds are 430 (E6, from `a6b14b22`), 475 and 855 (F alone), 788 and 919 (E6) and bundled 889 (E2). None of them was replayed on older trees (lean rule).
  - 430's 8-block gate world `HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold` holds E6 at 406 on `a6b14b22` and later, not on `bde9013c`. The gate asserts no capability hold, not keep-up, so it passes; the balance review missed the line.
- (a) Recommended: physics-first traces of the two smallest new worlds, 430 (from its 8-block gate world) and bundled 889 (23 blocks), before merge prep. A fix in the owning stage is then checked on the affected parts and the quick check.
- (b) Go to merge prep now. Net result: cold +13 against m23 and +3 against fl5, bundled +3, defaults equal, gates green. 430, 475, 788, 855, 919 and bundled 889 stay as open worlds.
- Not merged or pushed.

