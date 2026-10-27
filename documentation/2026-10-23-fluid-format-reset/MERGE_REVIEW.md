# Fluid format reset — integration review

2026-10-23; target `claude/floating-roof-tanks-81d68b`, delivery `7234bb46` (code `49e57860`).

- Fast-forwarded from ab35ef47. Incoming seven paths were disjoint from the nine modified tracked files and untracked diagnostic test. All ten local files and status entries were preserved exactly; SHA256 comparison passed. Nothing from that work was staged. Tracked ignored file lists were empty.
- Source and target `gradlew.bat jar --max-workers=4`: PASS. No tests added/run and no automated GUI/world interaction or test world fixture.
- Restarted target `runClient --max-workers=4`, session 57784; responsive Minecraft NeoForge 1.21.1 process 48904 left running.
- Passive startup evidence: at 16:32:04 the client log records checkpoint 14 triggering RESET, a format-15 empty authority at online tick 0, and solver STARTED; Dev joins at 16:32:05. No desktop/bridge input was sent. Detailed equipment/flow checks remain with the owner.
- Owner explicitly chose reset and inventory loss. Version mismatch now rebuilds placed devices using defaults; no migration or old-body decoding. Damaged current-format data retains strict validation.
- Main stays ef64bd06, mod_version 0.11.2. No main merge or bump. Future main delivery retains its required checks and owner version choice.
- Guide: owned runtime/equipment addenda and batch CODE_GUIDE_ADDENDUM snapshot 7234bb46; main chapter consolidation remains due.
- Tool cleanup/evidence: canonical ignored tools/fluid-format-reset/ holds compressed build/startup/owner-crash and preservation archives. No investigation code, harness, new Gradle task or switch was added.
