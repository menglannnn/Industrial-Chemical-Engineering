# Fluid display follow-up integration plan

2026-10-23. Continue the owner's merge/runclient request with the larger accessory icons and approved generator Volume/Mass selector. Source `a591404f` (`c1e567da` implementation), target `claude/floating-roof-tanks-81d68b` at `beb5c2fc`.

- The owner reported another hold before restart; record it for later without debugging. Their client subsequently exited cleanly and saved every dimension at 23:09:40; final format-17 evidence is archived separately.
- Run required quick/slow fixture checks with the client closed. Compare with the same five already reproduced baseline failures; add no tests, GameTests or campaigns for display-only changes.
- Recheck target branch/head/index and all ten unrelated local file SHA-256 hashes. Fast-forward only across non-overlapping paths; verify those hashes after integration. Keep main/version untouched.
- Update source-grounded GUI guide/integration snapshots and indexes, archive logs, package the integrated target with its preserved edits, then run ordinary runClient and confirm startup. Do not open a world or send desktop input.
