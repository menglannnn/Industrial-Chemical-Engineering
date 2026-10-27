# Process-control merge review (0.14.1)
2026-10-27, Claude (Opus 5.5). Branch `claude/process-control-merge` at `0e697177`. It holds `codex/process-control` (`8523595a`), the review fixes (`fa62fd81`, `8b91a822`, `b58c166e`) and main 0.14.0 (`4a7c807b`). Version 0.14.1 is the owner's choice. MERGED 2026-10-27, with the owner's approval: main fast-forwarded from `4a7c807b` to `0e697177`. Not pushed.

## Merge of main 0.14.0 (`6445a3b8`)
- **Facts schema 3.** Main's schema 2 (pipe joints, rack lanes) stays. Process control's additions follow it as schema 3: a device spec's vessel geometry, and the `control_valve` control.
  - Main's schema-2 corpus image stays. The branch's schema-2 image was never released; a schema-3 image written by the merged code replaces it (11 registrations, 2 laid pipes, 1 lane, 3 vessels, 1 shaped, 1 control valve).
  - `FluidFactsCompatTest` now also counts shaped vessels and control valves. Older images default to 0.
- **A merge defect, found by auditing every `Device` constructor call.** `InstrumentService.editSensorValve` rebuilt a pipe with the short constructor. That reset a laid pipe's joints to all six faces, so configuring its valve would have reconnected it to every neighbour. It now uses `Device.withControl`, which copies every field.
- Other conflicts: `.gitignore` (main's comment), CHANGELOG, `PhysicalRegistry` and `FluidDeviceBlockEntity` (both sides kept).

## Code-review findings 5 and 6 (owner: fix 5 and 6, leave 7 and 8)
- **5, `ab8cb8ba`: flowmeter taps.** A tap is an observation point that the compile puts on the island's pipe. The checkpoint does not save it, and installing a meter queued nothing.
  - `PhysicalRegistry.load` attaches the compiled taps to the restored islands.
  - A certificate replay keeps the island's own taps.
  - Installing or removing a meter queues the host's registration at its next revision, so the island recompiles on the engine schedule.
  - Tests: `PipelineFittingTest` (install, reload through the real checkpoint, removal; a resting island through its restored certificate). Each fails with its rule disabled.
- **6, `3ed2351c`: flow-valve rest.** A configured flow valve only throttles. It passes the static drive forward, nothing back, and nothing at a zero target, and the solve agrees on all three.
  - Test: `FlowControlTest.aFlowValveRestsWhenNothingDrivesThroughIt`. It fails under the old rule.

## Final lane (alone, 8 workers; logs in tools/process-control/logs/merge-lane-logs-2026-10-27.tar.gz)
| Gate | Result |
|---|---|
| `test` (quick suite) | 1558/1558 |
| `slowGates` | 187/187 |
| chain-100 exact | zero deviation (state, temperature, phase fraction, flow) |
| Fluid GameTests | 27/27 (process control 23, pipe laying 4) |

| Campaign, 1000 seeds | Codex final lane (before `a33ec4a6`) | Merge `0e697177` |
|---|---|---|
| Cold random | 975 | 975, the same 25 held seeds |
| Bundled random | 993 (E10 430; E2 241; E5 236, 431, 579; E8 698; F 157) | 994: seed 236 clean, the others the same |
| Bundled defaults | 1000 | 1000 |

- No seed is newly held.
- Campaign counters, codex → merge (they include held seeds):
  - Newton iterations: cold 3086510 → 3075567, bundled 934238 → 927682, defaults 69218 → 69188.
  - Residual evaluations: cold 11922361 → 11799266, bundled 3017386 → 2933527.
- **Reference lines.** The 33 junction lines, `MIXED_GAS_COST newtonSolves=299` included, are identical to the codex lane once allocation byte counts are removed.
  - Of 8494 printed lines, two classes move in physical values:
    - `LiquidFullTankOutletsTest` (51 lines): at most 6.5e-9 relative (tank pressure 129932.8972 → 129932.8980 Pa).
    - `PhasePortPriorityTest` (267 lines): values agree to about 1e-14, and the accepted and rejected step totals are the same. Only the rejection-reason mix shifts.
  - These moves fit the amount-weighted mixed stream (`fa62fd81`) acting on trace phases.
  - The other moves are timings, allocations, the state-format number in load warnings, and main's new `PipeLineCompatTest` lines.

## Code guide, tools, open items
- Code guide: README, process-control, fluid-science, fluid-runtime, instrument-accessories and verification (client-gui and equipment: label and snapshot), snapshot `0e697177`. Applied to the main checkout on 2026-10-27, with the owner's approval, as an in-place patch (55 hunks, clean dry run first). At the fast-forward, the snapshot lines' "pending" note becomes main.
- Tools: tools/process-control/ (`quick.ps1`, `merge-lane/`: the lane runner, the printed-line extractor, the move and constructor-audit scripts). Nothing was detached from tracked code. `git ls-files -i -c --exclude-standard` is empty.
- Open, not in this merge:
  - Findings 7 (volume-flow conflicts) and 8 (dense dependence check cost), and the minor items 9-12.
  - Cables and remote level control (a later batch). No in-game check: the fixes are server logic, covered by the gates.
