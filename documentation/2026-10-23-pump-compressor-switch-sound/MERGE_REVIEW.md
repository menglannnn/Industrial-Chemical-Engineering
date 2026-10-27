# Constructed mover controls/audio — integration review

2026-10-23; requested target `claude/floating-roof-tanks-81d68b`, delivered `ab35ef47`.

- Expected: constructed centrifugal pump and both compressors gain accepted On/Off controls and positional motor audio. Off closes flow using the existing zero-flow mode while retaining configured setpoints. One-block devices keep their behavior.
- Target previously ended at `b3b7a347`, without this batch. Fast-forward now includes implementation `4b4634f1`, enum refusal fix `6e5c07e9` and changelog metadata `ab35ef47`.
- All nine modified tracked files and the untracked `TmpDiagTest.java` are preserved byte-for-byte against SHA256 backups. Incoming paths were disjoint. Nothing from those local changes was staged or committed. Tracked ignored file lists were empty.
- Target `gradlew.bat jar --max-workers=4`: PASS. No tests added or run; no automatic GUI/in-game checks. Owner waived suites for this task.
- Target `runClient` started successfully; Minecraft NeoForge 1.21.1 window responsive, PID 45360, Gradle session 95886. Sound engine and resource atlases initialized. Client left running for owner checks.
- Fluid checkpoint 15 / unit 8 requires a fresh world. No old-world checks, migration, main merge or version bump. mod_version remains 0.11.2.
- Guide contracts: batch CODE_GUIDE_ADDENDUM and owned fluid-science/runtime/client/equipment addenda now snapshot integration `ab35ef47`. Canonical main chapters remain main facts, awaiting future main consolidation.
- Tool cleanup: no investigation code remains in tracked product paths. Original audio authoring/dependencies and compressed preservation/build/startup evidence live in canonical ignored `tools/pump-compressor-controls/`.
- Owner fresh-world behavior/audio checklist remains in MOVER_CONTROLS_REVIEW.md. Required main-merge checks and owner version choice remain due for any future main merge.
