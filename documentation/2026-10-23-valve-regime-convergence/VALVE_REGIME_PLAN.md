# Valve regime convergence investigation plan

2026-10-23. Owner: inspect test-world convergence errors, then trace the equal-setpoint drum outlet valves and suggest a fix.
Code snapshot: `232b0466`, integrated branch `claude/floating-roof-tanks-81d68b`; product changes only in the owned worktree if later authorized.

- Inspect current client logs, identify failing islands and preserve evidence.
- Read current-format (16/unit 9) saved topology, controls, states and accepted endpoint history without editing the world or testing compatibility.
- Start with the physical expectation: hydrostatic head and phase-selective outlets permit unequal flows and a mixed bottom discharge at equal valve setpoints.
- Distinguish valve mode initialization/reselection from phase-port draw segmentation and phase appearance. Reproduce the exact numerical symptom on copied numeric data.
- Compare the existing path with a conserved UV phase seed and accepted-endpoint valve-mode continuation; use at most these three forms, no broad solver campaign.
- Keep trace instrumentation and prototype changes in canonical ignored tooling, never in production files or live classpaths.
- Propose the smallest structural change preserving existing hydraulic laws, conservation and engine scheduling. Mark any remaining time-discretization effect explicitly.
- Diagnosis ended without production changes or tests. Owner then requested "Test and validate", authorizing the following validation extension.

## Validation extension, 2026-10-23
- Implement accepted-only valve-mode continuation on `codex/valve-regime-validation` in the owned worktree, based on `232b0466`. Keep the step argument local to each attempt and retain every existing constitutive and acceptance gate.
- Add a numeric initial-value regression fixture, not a Minecraft save reader. Check sustained mixed discharge, cumulative component/energy conservation, interrupted replay and invalidation after a control change.
- Disable only the continuation call and run only its regression to prove it catches the original failure; restore immediately.
- Run the quick/slow gates, exact chain-100 reference and fresh server GameTests once. Separate pre-existing failures with the unchanged solver, without changing unrelated contracts or equipment.
- Compare baseline/candidate on the existing 200-seed cold-random, bundled-random and bundled-defaults distributions; then one 1000-seed campaign for the selected form with eight workers. Classify new holds with the existing classifier and replay only new C/F budget cases alone.
- Keep campaign adapters, source overrides and logs in canonical ignored `tools/fluid-world-inspector/`. Archive the evidence and update the guide/index/changelog. The owner subsequently requested "looks good, merge when done": fast-forward `claude/floating-roof-tanks-81d68b` after validation, preserving its unrelated local equipment edits. No main merge, version bump, live-world edit or client launch.
