# Tank gauge cap flicker review

2026-10-24. Implementation fde01628 directly on claude/floating-roof-tanks-81d68b at owner request. No new branch or merge.

1. Expected: the red level indicator surrounds the liquid window; its upper rim and liquid surface are distinct, including at the gauge's full stop.
2. Previously: clipping the solid marker to glassHigh put its top polygon on the same plane as the closed liquid top. Both faced up and overlapped over the central window, producing the owner's striped/flickering top.
3. Implemented for owner validation: a hollow collar with an opening .25 m across around the .24 m liquid column, retaining the .32 m outer width and .07 m height. No artificial height nudge or full-level exception.

- TankGaugeGeometry caches markerFaces separately from closed bandFaces, constructed from four Mesh boxes. The 5 mm opening clearance prevents positive-area contact between the annular caps and the liquid cap. TankRenderer.column shares scaling/colour/lighting for both face lists.
- Actual mesh audit: 3009 level/end-stop cases, including nine old full/overfull cap coincidences; old combined top/bottom overlap .1152 m2, corrected overlap zero. Annular cap area and no positive-area self-overlap pass.
- Target compileJava/jar and material/resource checks pass. No JUnit suite or in-game automation while client PID 19828 is active; the full 1383/185 merge qualification belongs to 81cabbe9 before this drawing-only follow-up, not this hotfix.
- All ten unrelated modified/untracked files remain byte-identical and unstaged. Only TankGaugeGeometry, TankRenderer and the batch changelog are committed. Direct branch work follows the owner's explicit instruction to fix the current branch.
- No simulation, fluid colour, geometry proportions, persistence, protocol, controls or publication change. The prior client then closed normally, with all dimensions saved. Relaunched the fixed branch via runClient (PID 47252/session 27137), responding with assets/JEI loaded. No world input, save loading or forced shutdown performed.
- Evidence: tools/tank-indicator-ports/integration/GaugeCapAudit.java, cap-audit.log, cap-fix-build.log, cap-fix-before/after.json; compressed cap-fix archive under tools/tank-indicator-ports/logs. Owner screenshot retained in the parent tank batch screenshots/.
- Code-guide equipment/client-gui/verification and README updated to direct branch fix. No temporary product instrumentation, new Gradle task, main merge or version bump.
