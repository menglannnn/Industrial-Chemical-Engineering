# Pipeline tool plan

Date: 2026-10-23. Owner-approved plan, with minimum automated verification and owner-run in-game checks.

## Behaviour

- New managed worktree on `codex/pipeline-tool`, based on main `ef64bd06`.
- Creative-only `icheme:pipeline_tool`, stack size one, reusable without durability.
- Each tool keeps its own target internal diameter in metres: default 0.05, inclusive range 0.001–1.
- Shift + right-click in air or against a block opens one numeric field with Save and Cancel.
- Enter saves; Escape cancels. Invalid input shows an error and disables Save.
- Settings saves bind to the exact tool and used hand, with server-side lifetime and value checks.
- Normal right-click queues a resize of the clicked IChemE pipe; its other settings stay intact.
- Reapplying the same diameter is a no-op; bends and junctions are individual pipes too.
- Owner follow-up 2026-10-23: new pipes inherit the largest adjacent connected pipe's latest diameter; standalone pipes default to 0.05 m. The owner chose largest when neighboring sizes differ.
- The existing engine edit and geometry refresh paths own the change; no forced solve or process publication.
- Both hands work with vanilla hand priority; an offhand tool works with an empty main hand on a pipe.
- Add creative listing, tooltip, item model, translations and an Unreleased changelog line; omit a recipe.

## Verification and delivery

- One focused test class guards component persistence/network round-trips and invalid-value rejection.
- Run only that test through Gradle, with no active dev client; it compiles the implementation too.
- The owner performs in-game checks; no new GameTests, automated client run or GUI checks.
- Keep the branch and worktree for the owner's checks. A future merge retains the normal merge checks and version decision.
- Save this plan and a short review in this batch, update the documentation index and document item/GUI contracts.
