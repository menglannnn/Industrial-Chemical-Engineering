# Drum outlet valve regime convergence review

2026-10-23. Base `232b0466`; implementation `83e88bd0`, delivery `19117af7`. Fast-forwarded into `claude/floating-roof-tanks-81d68b`; not main, version unchanged.

## 1. Expected behaviour
- Equal 200 kPa backpressure settings permit unequal flows: liquid head, density and line resistance differ. When the liquid supply cannot sustain liquid-only withdrawal, the bottom port can carry liquid plus vapour.
- A regulating valve at its setpoint may carry positive flow and throttling head. Equality does not mean closed. Existing PhaseDraw already models mixed discharge; no new valve or phase law is needed.

## 2. What the code did
- The log had 117 HELD publications (113 island 99, 4 predecessor 98). The other water/pump island advanced normally. The case is 250 C / 301.325 kPa crude into an 11.519 m3 drum, with both outlets set to 200 kPa.
- Each implicit substep rebuilt valve modes from pressure: at/below setpoint + 0.01 Pa it chose CLOSED, even when the accepted endpoint was REGULATING. Reopening required convergence of that wrong branch while phase-port segments changed.
- Trace failures concentrated on bottom-junction continuity. The earlier gas-only versus UV two-phase result was a seed/phase-branch observation, not proof that the circuit was physically invalid. A UV-seed-only prototype did not establish a remedy.

## 3. Owner-approved correction
- PassiveIntervalSolver passes modes from its matching accepted endpoint directly to each step attempt. PassiveStepSolver uses them for pressure valves. Rejected Newton/step attempts cannot overwrite that argument.
- Existing control/geometry/port identity checks and closure-key invalidation discard stale carry; the ordinary active-set inequalities still choose OPEN, REGULATING or CLOSED. Cold/rate solves retain their existing start.
- No deadband, forced opening, PID, new phase law, conservation tolerance, engine scheduling or format change. Checkpoint 16 / unit 9 and fluid-13 remain unchanged; accepted endpoint modes already persist.

## Validation
- Three new quick-suite regressions pass: 12 x 4.5 s mixed discharge with cumulative component/energy ledgers, interrupted accepted-endpoint replay, and valve-setting invalidation.
- Disabling only the continuation call makes the mixed-discharge test fail with the exact logged refinement message/counts: state-change 3, iteration-limit 9, line-search 14, singular-Jacobian 14. Restored before all positive checks.
- Diagnosis prototype completed 94.5 s; settled averages: inlet 106.52783225, bottom 82.63201753, top 23.89581472 kg/s; bottom liquid 44.1813657 + gas 38.4506519 kg/s. Both endpoint valves regulate.
- Gradle test: 1360 tests, 4 failures. slowGates: 185 tests, 1 failure. Exact chain-100: 1/1; package generation passed. All valve/phase-port/conservation/retained-replay/held-world gates passed. MIXED_GAS_COST: 299 Newton solves, 92.7 MB (time is not a comparison).
- All five unrelated failures reproduce against the original solver: checkpoint expectation 13 vs 16; protocol fluid-11 vs fluid-13; constructed mover shell-cell expectation; SightGlassDisplay Math.pow source rule; existing equipment nozzle-clearance sweep. No unrelated gate or source was changed.
- Fresh-directory server GameTests were stopped after stale fixtures waited for flow from freshly placed generators (now Off). G5 is incomplete; the full gate suite is not green. No save migration, user-world mutation or desktop input.
- Matched 200-seed baseline/candidate check, all distributions: cold 195/200, bundled-random 199/200, defaults 200/200 in both arms; no newly held seeds, changed first-hold classes or increased held-publication counts. Newton iterations: cold 640315 -> 631327; bundled 150585 -> 150121; defaults unchanged.
- One selected-form 1000-seed/120-slice campaign per distribution, eight workers, alone: cold 975/1000, bundled-random 993/1000, defaults 1000/1000 clean. These are coverage results, not 3000 perfect networks or an identical new 1000-seed baseline comparison.
- Full first-hold classes: cold B=3, E1=2, E5=5, E6=9, F=6; bundled E10=1, E2=1, E5=2, E8=1, F=2; defaults none. Seed lists and counters are in campaign-summary.json/archive.
- Additional baseline cohort 201-400, all distributions: cold 198/200, bundled 199/200, defaults 200/200. It confirms bundled 241's E2 singular failure is pre-existing. No worse Newton seed was replayed alone or minimized.
- The two new matched-cohort deadline results (cold 244, bundled 6) each run clean alone, 120 slices; excluded as confirmed new regressions. Other full-campaign C/F cases already appear in the historical campaign. No confirmed new held-world regression was found within the matched coverage.

## Limits and delivery
- Repeatable interval-average mixed discharge is established; perfectly time-independent outlet quality is not. Five accepted / three rejected steps recur later, and endpoint rates differ from averages. Reviewing near-zero-holdup phase availability/complementarity would be a separate owner-approved algorithm/model task.
- Integration fast-forward `232b0466 -> 19117af7`; all 10 unrelated local equipment/test files preserved byte-for-byte (SHA-256). Owned feature branch is clean. No main merge, version bump, push or client launch.
- Code guide: fluid-science.md, verification.md and README.md have branch/integration addenda grounded in the source. Permanent test/resource are on the quick gate; existing suites unchanged.
- Tools: canonical tools/fluid-world-inspector/, including validate_campaign.py, original snapshot/prototypes and validation manifest. No tracked harness was detached. Logs/XMLs/reports compressed as logs/valve-validation-logs-2026-10-23.tar.gz; diagnosis logs remain in their earlier archive.
