# Inline accessory integration plan

2026-10-25. Owner requests merge and run; existing target is claude/floating-roof-tanks-81d68b in rename-ichem-mod-30e50c, starting ff9fdfed. Source f5a43d1e includes accessory refinements5a6e1ecc/955d6618 and inline movers e61751aa plus model fixes8a910a20/52b9f8b0.

- Build merge candidate in owned worktree on codex/inline-mover-integration: no-ff merge71ee777e. Preserve ten unrelated target files byte-for-byte; incoming paths have no overlap. Never stash, stage or change those files.
- Run required quick and slow suites plus jar, with no client or other timing run active. Keep baseline failures separate from regressions; compare exact messages with tools/gauge-accessories/integration/qualification.json.
- Initial qualification exposed two filter drawing regressions. Follow-up ccf3a5d3 retains complete overhang for authoring filter models and closes body ends behind differently tessellated nozzles. Geometry tests now validate the permitted overhang and the scaled service cover below/downstream. Pin the current checkpoint assertion to19 rather than stale13.20 focused tests and123 software views pass; rerun full quick/slow after correction.
- Update affected guide chapters from final code and record canonical logs/review/index for both batches. No new recipe, migration, protocol or mod version bump. Required formats are checkpoint19/unit12; owner-approved incompatible-fluid reset remains.
- After qualification, fast-forward the target checkout to the reviewed integration, verify all ten hashes and compile/package its existing local work. Launch normal runClient there and leave it running; open no world or GUI automatically. No desktop input or new in-game test.

Owner pause2026-10-25: target was never updated and no client launched. Candidate ccf3a5d3 completed quick1402/2 and slow185/1 with zero new failures against the retained exact messages. Those results precede the requested grounded support/border layout revision b2269322, delivered on codex/inline-mover-supports. Do not resume integration/client launch until the owner resumes it; rerun the required suites on the then-current candidate.
