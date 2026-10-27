# M2: the fluid network model on the equilibrium engine (complete; STACK_PASS open for the owner)

2026-10-16. Branch `claude/unified-thermo-port`, worktree `coolprop-multiphase-thermo-37f6b0`, plan M2 with D24 to D27. Not
pushed. Commits: `c91e3234` data, `0ab70bc8` model, `26b1511e` deferred tests, `e59da6ee` D24 A (Rachford-Rice in the minor
phase's fraction), `8a06b22f` D25 (no TP verdict removes a populated phase; the trace fold, also of a failed pass's last
iterate), M3a `cc1a39a1`, `54c1d24d` D26 A, `33ebad06` D27, `a0f20364` references. Detached material:
`tools/unified-thermo-port-m2/` (README, probes and specs, staged messages, diffs, `logs/m2-logs.tar.gz` with logs 00-46).

## D26 A (`54c1d24d`): an added phase starts at the node's own split
- Physics: a node at fixed amounts and energy (and volume, or pressure) has one equilibrium; an added phase starts there.
  Junction: the engine's PH answer at its pressure and enthalpy (`NetworkPhaseEngine.ph`, the spine's formation datum).
  Vessel: its own closure (`InventoryEquilibrium.split` calls `close()` from the TP answer with the lacking phases: UV
  taut, PH at ambient slack; no new closure code; where it stops short, its last iterate seeds). The seed is folded (D34,
  D41) and taken only if it still adds a phase. The 1e-6 nucleus is deleted; `phaseCorrection` stops at its node.
- innerUv (M3a) is not on this path: a vessel's split is the network's closure, not the engine's UV.
- The TP state seeds where a node's own flash fails (`phaseSeedFallbacks`): never used (quick check 258 seeds, 0).
- Seeds 35, 86, 116 clean. LiquidFullBubblePointTest 4/4, states as at `8a06b22f` (C2H6 interval 4: 7 substeps, was 9).
  Moved: PhasePortPriority overflow slice 56, the full tank loses its last gas by another substep path (9/5, was 10/6)
  and ends 0.24 Pa lower (1.1e-10 of its volume at water's kappa), within 0.01 Pa the next slice.

## D27 (`33ebad06`): two references restated for water's own compressibility
- Cadence: reference pair 0.25/0.125 s, bounds unchanged. Region 1's 4.56e-10 1/Pa (not 1e-9) shortens the two tanks'
  equalisation from 56 to 25 s, and first-order error grows with step over time constant (old pair 0.33 %). Now 0.16 %
  of 0.25 %; the cadences lag the finer reference by up to 1.77 % of 2 % (20 s; adaptive 1.45 %).
- McpGameplay: the capture's 996.05 kg filled 0.99898 m3 at 997.07 kg/m3, so the replay began with a 1 L refill. Fresh
  fixture (graph only, no shim): each node keeps its captured T, P and minor components and its major one fills 1 m3
  (tank: same N2, 55345.42 mol water, 997.06 kg). Default and refined flows agree to 4e-6 %; N2 1.6e-17 mol at 1000 s.

## Quick check and references
- 200 seeds, both distributions, 8 workers. Random: main `7a3ea017` 195 clean (C 101 175, E10 4 200, E3 157); `8a06b22f`
  193 (+ E7 35 86); head code `33ebad06` 195 with main's classes. Defaults 200 in all three. Newton iterations random
  114868 / 118470 / 115216, defaults 23104 / 21211 / 21211. No new hold.
- At `a0f20364`, once. Full `test`: 1414 tests, 1 failure (STACK_PASS). Legacy pin on JDK 21.0.11: REVISION
  `95cd8e6d...:direct-liquid-v1:tp-engine-v1:...`, DIGEST `dc9a8919...9e02`; the spine test's REVISION line restored.
- chain-100 re-captured (3 accepted, 0 rejected). Against the 0.7.0 capture: mass 2.9e-5, liquid fraction 4.4e-5, flows
  2.2e-4. Its 1 m3 nodes are charged by the model at 350 K, and M2's phases state the charge differently: the crude liquid
  (PR78 at the state pressure) takes 42.92 L in node 1 (was 42.88), Region 1's water 0.09 % less, each charge is 2.8e-5
  heavier. Newton solves 4 (main 104: its 100 node flashes are the engine's now).
- 33 junction lines, equal to the D26 suites': 24 MIXED_GAS lines and MIXED_GAS_COST 339 as main; 8 LIQUID_JUNCTION
  lines move with the water: m_J +0.10 % (997.05, not 996.0 kg/m3), generators' Q +0.05 % (root of the density), tanks'
  Q(1 s) x0.44 (a full tank delivers its own expansion). 118 fluid classes print as in the D26 suites (timings aside).

## Open for the owner: STACK_PASS (the upper tank keeps 0.47 g of water, bound 0.3 g)
- Physics: the last 1.3 g drains through the bottom outlet and the gas stays saturated; the tank ends dry.
- Code: a failed pass drains that water to a trace; the fold removes it and A85 keeps it out, so later passes state it
  as steam, 25 % supersaturated at 295.7 K. D26's own split condenses 0.0685 mol at 298.96 K, 450 Pa above rest; the
  Newton then meets gate-refused water columns, drains slowly, and rests with a puddle whose 5 mPa head stays closed.
- Options: A (recommended, own task): a failed pass's iterate is not a state, so it folds and excludes nothing; D25's
  fold stays for the solve's answers; measured on the quick check. B: vessels seed a nucleus again (D26 B for vessels).
  C: widen the bound to 0.5 g (a patch).
