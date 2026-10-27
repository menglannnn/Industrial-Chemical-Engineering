# Seed 236 fix: omitted hydrocarbon traces in vessels (option 1, narrowed)
2026-10-27, Claude (Opus 5.5), branch `claude/process-control-fixes`. Owner chose option 1 of SEED236_TRACE_REVIEW.md. Two measured findings narrowed it; both are listed under "Differences from the proposal".

## What changed
- `FluidThermodynamics.omittedTrace(state, junction)` replaces `carriedTrace` (D41).
  - A node's hydrocarbon is an omitted trace when free water is its only standing phase (no steam) and its hydrocarbon is below the D34 share (1e-9 of its fluid moles).
  - Junction: a carried dry gas only. This is the old rule, bit for bit.
  - Vessel: liquid and gas, while their volume is at most `OMITTED_TRACE_VOLUME` = 1e-15 of the vessel's.
- `PhaseLayout.support` makes every hydrocarbon of an omitted node ABSENT: no unknown, no row, in the step and in `InventoryEquilibrium.close`, whose layout is derived from the state.
  - `equilibriumResidual` states no phase pair when no hydrocarbon is laid out, even if the seed holds two trace phases.
- `PassiveStepSolver.omittedArrival`: a vessel whose held plus arriving hydrocarbon over the step is an omitted trace is seeded with its own state, without the 1e-12 entry traces (D40 A) that made the phantom two-phase seed.
  - `phaseAppeared` skips an omitted trace, as it skipped a carried gas.
- The reconstruction is unchanged: it books the trace exactly, and the state holds it in the phases its own flash gives.

## Differences from the proposal (measured)
1. **Vessel bound in volume, not 1e-9 of moles.** A liquid-full taut vessel's pressure is set by water's compressibility (4.5e-10 /Pa).
   - A trace whose volume the Newton leaves out, but the reconstruction includes, moves the closed pressure by share / kappa. That is about 44 Pa at 1e-9 of the moles for a heavy liquid, about 1e-4 on the hydraulic rows against the 1e-8 gate.
   - At 1e-15 of the volume the shift is 2e-6 Pa: 2e-11 of a 1e5 Pa row.
2. **Junction liquid traces stay laid out.** Omitting them was tried first, in the seed-236 world.
   - At tick 1980 the junction's 3.6e-8 mol of crude (share 6.5e-10) was omitted. The tank after it holds 3.4e-7 mol, above the vessel bound, so it was laid out.
   - The Newton's stream carried no crude while the reconstruction delivered it. The tank's balance rows missed by 0.46 to 1.47 (4e-6 at the smallest step), and the gate refused every step: a new hold at tick 2080.
   - A junction's outflow feeds vessels that resolve what they hold, so its liquid trace must stay in the transport.

## Results (commit `8b91a822`, changelog `b58c166e`)
- **Focused tests:** 31 quick classes, 139 tests, and the slow gates `HoldClassWorldsTest` (40) and `JunctionSeedingGateTest` (7). 0 failures.
- **Minimized world:** now `TracePhaseFoldTest.waterThroughATankHoldingACrudeTraceKeepsUp` (`EventPathWorld`, to tick 7000, 1.3 s).
  - Passes with the fix.
  - On `fa62fd81` source it holds exactly as seed 236 did: tick 5680, residual 0.52997, 7 Jacobian and 13 line-search rejections.
  - Probe: the slice from 5580 lays the tank out as 3 unknowns (water, T, P) and is accepted in 3 substeps, 0 rejected (before: 20 rejected).
- **Bundled seed 236** (1 thread, counters), run alone: 0 holds to the seed's end, tick 13840. Its islands end STEADY or FULL.
  - Newton: 1095 solves and 2647 iterations for the whole seed. The held runs used 1260 and 5974 up to their hold.
- **Quick check** (seeds 1-200, 8 threads, counters, each run alone). Base: an export of `8523595a`. Fix: `b58c166e`, which includes `fa62fd81`.
  - Cold random: 195/200 on both, with the same held seeds (E1 85; E5 6, 110; E6 29, 101) and identical counters.
  - Bundled defaults: 200/200 on both, with identical counters.
  - Bundled random: base 198/200 (F 6, 157), fix 199/200 (F 157).
    - Base seed 6 is "HELD: wall deadline" at ticks 1837 and 1937. It reaches the same end on both (tick 14053, same islands), so this is a wall-clock hold, not an effect of the fix.
    - Over the 198 seeds clean in both: Newton iterations 126613 to 126796 (+0.14 %), residual evaluations +0.31 %, equation-gate rejections 10 to 13.
  - Seed 236 lies outside seeds 1-200; its result is the replay above.
  - Runner, reports and counters: tools/process-control/quick.ps1, logs/seed236-fix-quick-logs-2026-10-27.tar.gz.

## Risks and open items
- A vessel's omitted trace that leaves through a BULK end is in the reconstruction's outflow but not in the Newton's.
  - Downstream, a vessel that holds that component as a resolved trace sees the difference relative to its own amount.
  - The bound limits the omitted trace to about 1e-11 mol per m³ of vessel, so such a vessel would have to hold under about 1e-3 mol for the gate to see it.
  - D41's junction gas has always carried the same structure. Not seen in the tests run.
- Through a phase port the omitted trace leaves as the start state's pinned phase (D11), the same draw in the Newton and in the reconstruction. Arrivals stay until flushed (1e-24 of the vessel) or until they cross the bound and appear.
- Before merge: the full 1000-seed campaign, if the owner chooses this form. Not run.
- Code guide: `fluid-science.md` (seeding and D41) needs the vessel rule and `OMITTED_TRACE_VOLUME` at merge.
