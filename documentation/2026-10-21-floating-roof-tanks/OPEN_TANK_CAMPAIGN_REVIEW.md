# Open tank campaign (WP5): 1000 seeds at `796da897`, form B
2026-10-21. One campaign: cold random, bundled random and bundled defaults, 1000 seeds, 8 workers, counters on, alone on
the machine. Gradle-free javac lane (`d40/lane.sh` export, never the worktree). Bundled: `floating-roof-tanks` harness with
`holdFuzz.openTanks=true` (353 of 1000 seeds open a tank; open-tank seeds feed methane-free crude). Baseline: final lane fl8
at `c7b883cb` (0.10.1), the last main campaign. 0.11.0 to 0.11.2 changed no solver code and ran none.
Tooling and logs: `tools/floating-roof-tanks/campaign/`, `logs/campaign-logs-2026-10-21.tar.gz`. Product code unchanged; code guide: no change.

## 1. Expected behaviour
- Seeds with no open tank: the same results as fl8, seed by seed. Only class F (wall deadline) moves with load.
- An open tank fed a liquid that does not boil at 1 atm stands at 101 325 Pa and spills its net inflow. Permanent gas
  (N2, CH4) that reaches it vents: a gas-only open tank is a 1-atm sink, like a void. A boiling feed is outside the design.

## 2. What the runs showed
| Distribution | fl8 clean | `796da897` clean | New holds | Seeds without an open tank |
|---|---|---|---|---|
| cold random | 975 | 973 | F 118, 183 (B->F 833) | 994 of 1000 seed lines identical; the 6 others are wall deadlines |
| bundled random | 994 | 990 | E4 354, E6 598, E6 895, E5 678, F 6 | 645 of 647 clean in both; all but seed 6 identical |
| bundled defaults | 1000 | 1000 | none | 647 identical; 353 open-tank seeds all clean |

- Alone replay (one batch, one worker): cold 118 and 183 clean, cold 833 holds B at tick 1240 as in fl8, bundled 6
  clean. No new class C or F hold remains. The javac lane gives the Gradle lane's results.
- Open-tank seeds, bundled random: newly clean 241 (was E2); 157 F->E9 and 579 E5->E10 (their crude changed).
- The four new holds are in open-tank seeds. Each one replays alone, one worker:
  - **895 (E6), minimized 15 -> 11 blocks, tick 115.** N2 at 352 kPa (260 K) and a CH4 generator at 123 kPa feed a
    gas-only open tank and a void, two 1-atm sinks in parallel. No liquid is involved: the designed vent path. With the
    tank closed (fl8) the seed is clean. Step [107,115]: 20 substeps fail, 616 Newton iterations, 1 001 backtracks,
    residual 0.51. Between steps the tank is restated empty (`PassiveIntervalSolver.ventOpen`, D54 ambient: 1 m3 N2,
    298.15 K). Within a step, the gas the tank receives accumulates at pinned pressure.
  - **598 (E6), minimized 14 -> 7 blocks.** Water (296 K) and methane-free crude (345 K, 255 kPa) feed one open tank.
    At the hold the tank holds 0.44 m3 of vapour at 101 325 Pa and 342 K: the combined feed boils at 1 atm (water's
    33 kPa plus the crude's C2-C4). Outside the design: removing methane does not stabilise crude at 345 K.
  - 354 (E4, not traced; 2-trace limit): its successor island holds an open tank with water, fed by water at 320 K and
    CH4 gas at 361 K. No boiling liquid. 678 (E5, not traced): crude at 310 K, water, N2 and CH4 feed open tanks.
- Counters, bundled random, the 989 seeds clean in both (subset reruns at `ef64bd06` and `796da897`). The 645 seeds
  without an open tank are bitwise identical. The 344 open-tank seeds (their crude also changed): active-set passes
  +23.6 %, step attempts +34.9 %, Newton iterations -5.2 %, residual evaluations -20.8 %, phase seeds -65 %. All 989:
  passes +15.0 %, steps +21.6 %, Newton -3.6 %.
- Bundled defaults (all clean, feeds unchanged): active-set passes 28 981 -> 33 522 (+15.7 %), step attempts +20.0 %,
  Newton +3.3 %, residual evaluations +5.2 %, phase seeds 1 -> 1. This is the open vessel's own cost: fed open tanks
  spill and never rest.

## 3. Proposal (bitwise and budget green: off the open tank only wall deadlines move, no new C/F hold survives alone)
- **Owner decision, seed 895 (in-design defect):** permanent gas into a gas-only open tank holds where a void does not.
  (a) Within the step, model a gas-only open vessel as the void-like 1-atm boundary it is (its gas out equals its gas
  in), which removes the post-step emptying special case in `ventOpen`. This is the structural option. (b) Keep the form,
  trace the step's rows on the 11-block world (`campaign/trace`, KEEP list in the logs) and fix the failing stage.
  354 and 678 also carry gas into open tanks and may share the cause: re-run them after (a) or (b).
- **Harness option for the owner:** methane-free crude still boils at 1 atm above about 340 K (598). Choose one: also
  strip C2-C4, cap the crude generator's temperature in open-tank seeds (for example 320 K), or keep reporting such holds
  as out of design.
