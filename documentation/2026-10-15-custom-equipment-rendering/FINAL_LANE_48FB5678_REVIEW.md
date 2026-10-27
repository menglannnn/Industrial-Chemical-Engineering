# Final lane of branch `claude/stoic-bell-ae6019` at `48fb5678` (Opus, 2026-10-20)

## Verdict: NOT GREEN
The gates pass. The campaign holds cold 960 clean against m23's 962. Alone, five cold worlds are worse than on base `2e28a20a`. Three come from `a6a305a1` (a junction's own split) and two from `48fb5678` (the balanced flash seed).
The lane is m23's (`tools/new-units/logs/merge-final-lane-m23-2026-10-20.tar.gz`), re-pointed to this worktree, with the full test added on owner request. Gradle ran alone, JAVA_OPTS=-Xshare:off, no dev client; one owner geosim JVM (2 threads) was running. Alone replays ran on the javac lane, one worker, nothing else running. Logs: `tools/new-units/logs/final-lane-48fb5678-2026-10-20.tar.gz`.

## 1. Gates
- Full test: 1 572 tests, 0 failures, 0 errors. The javac lane's `FluidCheckpointFormatTest` failure was an artefact of that lane.
- G2 chain-100 exact (deviation 0). Fluid GameTests: all 31 passed.

## 2. Campaign: 1000 seeds, 8 workers, against m23 (`8be1efef`, 0.10.0's tree)
| Distribution | Clean m23 / now | Newly held | Newly clean |
|---|---|---|---|
| cold | 962 / 960 | 15 F, 183 F, 509 E6, 592 F, 729 E6, 757 F, 768 F, 779 F, 788 F | 72, 285, 397, 567, 628, 694, 994 |
| bundled | 990 / 990 | none (579 changes class E10 → E5) | none |
| defaults | 1000 / 1000 | none | none |
- Cold counters: Newton iterations 2 853 503 against 2 772 420 (+2.9 %). Seed time 2 452 s against 2 161 s (+13 %). Phase seeds 16 583 against 33 247; refusals 58 645 against 20 938.
- Bundled: Newton iterations +1.0 %. Defaults: 74 392 against 74 403.

## 3. New C/F holds and the E6 worlds, alone (Newton iterations; H = held)
| Seed | base `2e28a20a` | `a6a305a1` | `48fb5678` | Verdict |
|---|---|---|---|---|
| 509 | 288 | 76 109 H | 76 109 H | worse, from `a6a305a1` (its arrival split) |
| 729 | 5 887 | 19 193 H | 21 655 H | worse, from `a6a305a1` (its phase-check split) |
| 757 | 23 194 | 29 670 H | 30 298 H | worse, from `a6a305a1` |
| 592 | 5 153 | 4 817 | 46 852 H | worse, from `48fb5678` (not traced) |
| 768 | 25 646 | 27 400 | 33 121 H | worse, from `48fb5678` |
| 6 | 6 440 H (B) | 40 209 H | 15 358 H | held on all three |
| 15, 183, 779, 788 | clean | | clean | wall time only |
| 555, 673, 919 | held | | held | held on base too |

## 4. Seed 509, physics first (island 16, traced)
- An LNG junction at 161.6 K is committed liquid at 170 kPa, about a tenth of its bubble pressure. It must flash. The network's own answer, reached on base, is 1.23 MPa at 155.5 K with half the moles vapour, at 312 kg/s through the island.
- What the code does on both commits: the arrival seed is the junction's adiabatic split at the carried 170 kPa, 119.9 K with a third vapour. That split is physically right but at the wrong pressure. Every step attempt stalls (residual 313 in pass 0, iteration limit in pass 1).
- Base got through only because its arrival seed was the TP flash at 161 K: 99.99 % vapour, 396 kJ/kg too much enthalpy. A light seed let the network raise the pressure.
- With base's arrival seed on `a6a305a1`: 509 is clean (290 iterations), but 170 holds and 285 holds once. The arrival split is part of what fixed 285.
- Seeding at the balance start does not reach 509. The balance gives 168 kPa, not 1.23 MPa, and the fluid-only engine answers nothing there. Patch kept as `tools/new-units/od4-holds/r4/prototypes/p3-arrival-balanced-vapour-against-48fb5678.patch`; the branch stays at `48fb5678`.
- In 170 and 509 the network settles at 70 to 80 % of the bubble pressure (0.92 of 1.3 MPa, 1.23 of about 1.5 MPa). The liquid pass's pressure is a third and a tenth of it. Only the balance's 801 kPa in 170 came close.

## 5. Proposition for the owner
- (a) Do not merge the branch as it is. Cold 285 stays the open OD4 cost item that 0.10.0 shipped with.
- (b) Next design item: the seed pressure of a junction where vapour first appears far below its bubble pressure (170, 509; check 729, 757, 592, 768). It needs a physical predictor of where the network settles; the balance start is not one for 509. Re-run this lane after it.
- (c) Or merge as is and accept five worse cold worlds against the seven cleared, among them 285. Not recommended.
- Code guide: no change (the branch is not merged).
