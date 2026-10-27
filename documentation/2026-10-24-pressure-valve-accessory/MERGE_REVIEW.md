# Pressure accessory / equipment arrows merge review

2026-10-24. Owner requested merge and run. Integrated 4ad6dc97 into claude/floating-roof-tanks-81d68b (previous b70e92aa).

- Includes 7cb42268/2fe26e21 equipment-owned arrows and pump joint refinement plus dc174f27/4ad6dc97 pressure accessories, tall actuator and heavy cutoff handles.
- Required clean-source checks ran with the client closed: test 1375 cases, 1371 passed/four known failures; slowGates 185, 184 passed/one known failure. No new failure against the saved b70e92aa baseline. Gradle exits nonzero for those known failures.
- Known quick failures: checkpoint version expectation (portsACompressorAndARefusedMoverRoundTripExactly), old protocol expectation (theProtocolIsFluidSixWithDistinctStaticAndLivePayloads), old pump node expectation (aDrumsFluidNodeIsAShellCell), and existing SightGlassDisplay Math.pow source rule.
- Known slow failure: ProcessUnitTest.everyNozzleNeckAtItsLargestStandsClear. No additional budget failure or replay was needed.
- Captured ten target local files. Nine remain byte-for-byte unchanged. ProcessUnit combines disjoint incoming pump work with its original instrumentation/heater/clearance edits and exactly matches the clean read-only three-way preflight.
- A named stash held only ProcessUnit during fast-forward; exact object 28861c6c77dc2192bc82f73633dabb38998e835d was applied and retained as an already-restored recovery backup. No target local edits were committed or left staged.
- Source and target tracked-ignored-file checks empty. Target jar/material verification passed with all target local edits retained; package audit confirms the new accessory model/class and removal of standalone pressure block assets. All ten post-merge hashes remained unchanged through that build.
- Code-guide equipment, fluid-runtime, client-gui, verification and README snapshots updated to integration 4ad6dc97. Both batches and tools index rows updated. No product instrumentation/probe was added or detached.
- runClient launched from the target after other detected test executors exited; PID 43976, session 41357, responsive Minecraft NeoForge 1.21.1 window and texture/sound resource startup confirmed. Agent issued no world load or game/GUI input. Client left running.
- Mod 0.11.2, checkpoint 18/unit 11 and protocol fluid-13 retained. No main merge or version bump. Standalone pressure blocks were removed; gameplay verification remains manual on a fresh world.
- Evidence/tooling: tools/pressure-valve-accessory/integration/, integrate.py and logs/pressure-valve-merge-logs-2026-10-24.tar.gz. Original focused-test/render archive retained separately.
