# Tank gauge and bubble-direction integration

Status: In progress since 2026-10-25. Owner requested merge into the established `claude/floating-roof-tanks-81d68b` target.

- Target inspected at `fffbd514`; clean tracked/untracked status. It advanced after `f658697c` with equipment/layout/ladder work.
- Owned candidate `codex/tank-gauge-integration` starts at that target and merges `codex/tank-gauge-enclosure` (`db936513`, includes `e5591c04`). Three-way merge is conflict-free, preserving both lines of work.
- Incoming changes: accepted-direction bubbles/protocol3 and custom continuous level-gauge glass/round equalizing connections. Existing target equipment changes remain.
- Run required quick and slow suites plus package generation on the combined code, with no dev client or competing measurement lane. Compare failures against retained/current integration evidence, reporting baseline separately.
- Update code guide and both batch indexes/reviews. Keep ignored tooling/evidence canonical and compress logs. No one-off probe/task/exclusion or format migration.
- Recheck the target head/status before fast-forwarding it to the qualified candidate. Do not change `main`, choose a release version or start a client for this merge-only request.
