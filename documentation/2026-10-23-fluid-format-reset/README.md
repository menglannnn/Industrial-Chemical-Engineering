# 2026-10-23-fluid-format-reset

Purpose: Reset incompatible fluid simulation formats and rebind placed devices instead of crashing startup

Status: Implemented 2026-10-26 (0.12.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [FORMAT_RESET_PLAN.md](FORMAT_RESET_PLAN.md)
- [FORMAT_RESET_REVIEW.md](FORMAT_RESET_REVIEW.md)
- [MERGE_PLAN.md](MERGE_PLAN.md)
- [MERGE_REVIEW.md](MERGE_REVIEW.md)
- [CODE_GUIDE_ADDENDUM.md](CODE_GUIDE_ADDENDUM.md)

## Code areas

Checkpoint/pack/unit mismatch reset, queued chunk discovery, complete equipment rebinding; checkpoint 15 / unit 8 unchanged; `jar`, owner `runClient`

## History

- Implemented 2026-10-26 (0.12.0, `2178a7c2`, fast-forward, not pushed; in progress since 2026-10-23 (target `claude/floating-roof-tanks-81d68b` integrated at `7234bb46`; destructive reset selected by owner; ten local changes preserved; target jar passed; no tests; client restarted; owner checks/main integration pending))
