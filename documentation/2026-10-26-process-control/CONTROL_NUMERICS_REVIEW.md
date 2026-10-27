# Control/outlet numerical qualification
2026-10-27. One selected form; 8-worker campaigns run alone. Classification: existing tools/phase-ports-probes/hold-fuzz/classify.sh.

## Expected behaviour
A bottom outlet co-draws the two liquid volumes until a phase is exhausted, then continues with the remaining liquid. At a boot weir, incoming water and competing drains enter the same end-inventory balance. Ideal controllers read that trial state, never delayed GUI samples.

## Observed results
| Distribution | Main 600549d9 quick | Candidate quick | Full candidate clean |
|---|---:|---:|---:|
| Cold random | 195/200 | 195/200 | 975/1000 |
| Bundled random | 199/200 | 199/200 | 993/1000 |
| Bundled defaults | 200/200 | 200/200 | 1000/1000 |
Quick hold classes/seeds are unchanged. Cold/default quick counters are identical; bundled Newton solves 34079 -> 34492, residual evaluations 365917 -> 383063. Wall times are not used to infer a cost improvement.
Full-campaign counters (cold / bundled random / defaults): implicit solves 286043 / 120407 / 26515; Newton iterations 3086510 / 934238 / 69218; residual evaluations 11922361 / 3017386 / 131111. These are campaign counters, not separate per-seed Newton measurements.
Archived full baseline: tools/backward-compat/logs/final-lane-8d4c8df6-2026-10-26.tar.gz (975 / 994 / 1000 clean). Current primary classes:
- Cold: B 220,431,833; E1 85,988; E5 6,110,752,792,803; E6 29,101,206,430,446,788,919,957,994; F 244,475,651,683,855,907.
- Bundled random: E10 430; E2 241; E5 236,431,579; E8 698; F 157. Defaults: none.
- Worse new worlds by classifier: E5, seed 236 only. Cold 833's former first-budget classification becomes B, already seen in the archived alone replay. No new C or F world requires an alone budget replay.

## What the code did in the new world
The existing minimizer reduced seed 236 from 20 blocks to nine placements, all at tick 153: two supplies (water at 350 K/346 kPa and crude at 341 K/332 kPa), five pipes, one vessel and a void. It first holds at tick 5680, committed start 5580, with line-search residual about .53. The 1 m³ vessel contains about 1 m³ water and 8.66e-24 m³ HC liquid (roughly 7e-20 mol of one heavy component). The junction also retains a tiny HC trace. The crude branch is pressure-opposed at that state; the water/drain balance should continue after trace-phase depletion.
This is a numerical E5 stall (the harness additionally tags trace underflow from its rejection reasons), not a controller conflict or a new typed phase hold. The failed graph and exact amounts are in trace-236.log. This evidence locates the affected phase-depletion boundary; it does not prove the exact Jacobian/continuation defect.

## Proposal for the owner
Recommended: keep the branch unmerged and investigate the existing trace-phase/depletion equation conditioning, with exact component/energy bookkeeping, before release. Alternative: accept this single newly exposed held world as a documented release limitation. No arbitrary trace cutoff, discarded component or new reference model is proposed or implemented here.
The targeted physics checks pass: equal-volume draw/depletion, boot fallback, competing inlet/drain retention, and 1/.5/.2 s weir steps without threshold chatter. Pressure/flow target limits, dependent-loop fallback and independent-loop regulation are gated. Future cable/level-loop step-size tests remain in the later batch.
