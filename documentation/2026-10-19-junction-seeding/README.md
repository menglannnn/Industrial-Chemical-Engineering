# 2026-10-19-junction-seeding

Purpose: One robust, efficient junction seeding mechanism that gives the fluid network solver an accurate quasi-steady start, choked lines included, in place of the incremental start machinery (D12 one-way pressures, D55 junction starts, the boundary junction seed, start flows and warm carry; about 600 code lines); the 100 m/s velocity limit kept for every stream (owner 2026-10-19)

Status: Implemented 2026-10-19 (0.9.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [START_MACHINERY_MAP.md](START_MACHINERY_MAP.md)
- [JUNCTION_SEEDING_PLAN.md](JUNCTION_SEEDING_PLAN.md)
- [BRANCH_SELECTION_TRACE.md](BRANCH_SELECTION_TRACE.md)
- [MOVED_REFERENCES.md](MOVED_REFERENCES.md)
- [FINAL_TEST_REVIEW.md](FINAL_TEST_REVIEW.md)

## Code areas

src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver (start machinery); t48 (Opus lane m16) at `798bedd9` green except the cost gate (bundled Newton +1.2 % against m14, all seed 483), accepted by the owner 2026-10-19; pre-merge fixes (t44d, 2026-10-19): `9ce2553a` unit format 4 and the revision text `line-cap-saturation-v1`, `47391899` the six balance counters detached to `tools/junction-seeding/detached/`, `81bf147f` seven stale comments and MATERIALS.md rewritten from the code; ready for final test `81bf147f`; Opus full-test re-run m17 (t49, 2026-10-19) at `81bf147f` GREEN: 1482 tests, 0 failures, printed output equal to m16 but timings and the core record +10 bytes, chain-100 deviation 0

## History

- Implemented 2026-10-19 (main `5b49f940`, mod 0.9.0, release 0.9.0 by fast-forward of `claude/junction-seeding` with the pre-merge commits `9ce2553a`, `47391899`, `81bf147f`; not pushed; final gates t48 (m16 at `798bedd9`): full test 1482/0, G2 exact, fluid GameTests 31/31, campaign cold 944 / bundled 988 / defaults 1000; t49 (m17 at `81bf147f`): 1482/0, G2 exact; merge plan `2026-09-24-coolprop-low-temperature/MERGE_STACK_PLAN.md`; code guide refreshed at `5b49f940`).
- Open: bundled 145 (one buffer sizing for the mint and the carry) and cold 850 (a flow at or below D42's resolution counts as no flow for the solid transition test) as the next items
- the D46 watch item (unbooked mint mass)
- the in-game check after the merge.
- Next: the custom equipment rendering branch merges as its own batch with checkpoint format 12.
- History: in progress since 2026-10-19 (thread t44, Fable 5.1 max): plan ready 2026-10-19, form A = the island's quasi-steady balance (D12 promoted) as the one start, about 516 code lines deleted and 330 added
- quick check cold 190 / bundled 198 / defaults 200 against 191 / 198 / 200
- 31 of 47 held cold and 5 of 6 bundled start-caused seeds clear
- cold seed time 1.9x
- owner approved 2026-10-19 with a cost gate (at most 1.3x cold seed time, no rise of the Newton's counters on clean seeds), the gas-column band and pump start modes from the balance
- built on branch `claude/junction-seeding` from `32bf3fa4` (t44b: `c623b29b`, `43a3c476`, `51093b94`, suites green)
- cost gate missed 2026-10-19 (cold seed time 1.37x on clean seeds, Newton iterations +3.4 % cold / +14 % bundled; quick check 189 / 197 / 200)
- owner: try the plan's fallback (the balance seeds the cold rate solve) under the same gate
- fallback `33455d4c` 2026-10-19: cold Newton neutral (iterations -2.8 %), cold seed time 1.35x and bundled Newton +15 % still miss the gate, the bundled rise from four islands landing on another steady branch
- owner: trace the branch first (thread t45, `BRANCH_SELECTION_TRACE.md`)
- t45 2026-10-19 (Opus, analysis only): "FULL" is an uncertified island's solve detail, not a full tank
- seed 4 has one steady state, reached at 1019 s instead of 119 s because the mint's balance starts from the placeholder (the island's lowest-id boundary state, crude) and stops at 2 rounds, and the frozen-junction rate seed keeps the cut iterate
- seed 123 is the same family (a water placeholder self-locks an unconverged balance; minimized tank 113 kg of water against 20.6 kg)
- 200 and 78 are not flips
- three forms quick-checked (ambient placeholder, ambient + 8 rounds, 8 rounds) are rejected or mixed
- proposition: the balance's round 0 from D55's supplier composition (about 70 of the deleted lines)
- owner 2026-10-19: t44 builds it as the last form under the same gate (defaults included) and measures the unbooked mint mass (D46)
- mint form `c51df66a` + CHANGELOG `8e470a3e` 2026-10-19: cost gate met at the line (Newton iterations -4.1 / -0.1 / -23 %, Jacobian builds -4.5 / -2.8 / -14 % on clean seeds; cold seed time 1.30x), seeds 4 and 123 as the baseline, quick check 189 / 195 / 200 against 191 / 197 / 200, 21 of 30 cold and 4 of 5 bundled start-caused seeds above 200 clear, D46 unbooked mint mass half the baseline's (watch item)
- Opus final test t46 2026-10-19 (lane m15 at `8e470a3e`, `FINAL_TEST_REVIEW.md`): GREEN, full test 1480 / 0 failures, chain-100 0, GameTests 31/31, campaign cold 942 / bundled 988 / defaults 1000 against 924 / 985 / 1000, Newton iterations on clean seeds -4.1 / -1.5 / -20.7 %
- 20 new held worlds by mechanism (9 with the start signature)
- traced: bundled 483 (an unchanged island re-minted, the balance's round 1 flips a header to water) and cold 271 (the balance leaves a back-pressure valve CLOSED), two propositions for the owner
- two unlisted moved lines (`FluidPumpedFillLineTest` first interval, `FluidDataRangeIslandTest` roundoff)
- owner 2026-10-19: both propositions built in this batch (t44c), then one more Opus final lane
- a merge plan for the four stacked batches is being prepared (thread t47, `2026-09-24-coolprop-low-temperature/MERGE_STACK_PLAN.md`)
- follow-ups `373c8fee` (the carry) and `bbbd8626` (valve modes), CHANGELOG `798bedd9`
- Opus final test 2, t48 2026-10-19 (lane m16 at `798bedd9`, `FINAL_TEST_REVIEW.md`): not green on the cost gate only: full test 1482 / 0 failures, chain-100 0, GameTests 31/31, campaign cold 944 / bundled 988 / defaults 1000 (cold 271 654 737 and bundled 483 cleared; new holds bundled 145 and cold 850 traced: a carried junction buffer sized on a placeholder gas, a still pipe's deposition test on a roundoff flow; two propositions)
- clean-seed Newton iterations against m15 +0.9 / +1.1 % (cold / bundled; the carry and the valve rule, five worlds carry most)
- the batch against m14 cold -4.1 %, bundled +1.2 % (seed 483 alone, -0.3 % without it), defaults -21 %
- owner 2026-10-19: result accepted (the gate met in substance)
- bundled 145 buffer sizing and cold 850 resolution flow are the next batch's items
- pre-merge fixes (t44d) then an Opus full-test re-run
- merge plan in `2026-09-24-coolprop-low-temperature/MERGE_STACK_PLAN.md`
- merged 2026-10-19
