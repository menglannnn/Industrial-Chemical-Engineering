# Functional machines — target integration review

Date: 2026-10-23. Owner requested merge into `claude/floating-roof-tanks-81d68b`.

- Target checkout: `D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/rename-ichem-mod-30e50c`.
- Fast-forwarded `7bf50b7c` → `29ff371b`, including implementation `4e96e8ab` and its Unreleased changelog entry. No merge to main, version bump or push.
- Target had nine modified Java files and one untracked diagnostic test. Only ShapeRasterizer overlapped the incoming feature, with separate hunks.
- Recorded SHA-256 hashes for all ten local paths and computed the expected three-way ShapeRasterizer combination before integration.
- Preserved only that file in a descriptively named, tagged stash, fast-forwarded, then restored it with `git stash apply --index`. A first restore with core.autocrlf=false was refused because of checked-out CRLF text; normal Git line-ending handling restored it cleanly.
- Verified nine paths byte-for-byte unchanged; ShapeRasterizer's normalized text exactly matches the precomputed combination. The original ten dirty paths remain, with no staged changes.
- The temporary stash/tag are cleaned up after verified restoration; original local ShapeRasterizer/base/feature/combined text and preservation hashes are archived with integration evidence. Pre-existing stash entries are untouched.
- `gradlew.bat jar --max-workers=4` on the merged target, including its retained local work: PASS. No tests or in-game checks run, preserving the owner's no-tests preference. No client was launched.
- `git diff --check`: clean. `git ls-files -i -c --exclude-standard`: empty.
- Owned branch code-guide chapter addenda are refreshed at integration snapshot `29ff371b`; canonical copies are in this batch, with canonical main-guide consolidation due at a future main merge.
- Evidence: `tools/functional-pumps-compressors/logs/functional-pumps-compressors-integration-logs-2026-10-23.tar.gz`.
- Owner checks require a fresh world: checkpoint 14 / storage unit 7. Operating controls, phase protections, reload, rotated/elevated ports and GUI manual checks remain pending.
