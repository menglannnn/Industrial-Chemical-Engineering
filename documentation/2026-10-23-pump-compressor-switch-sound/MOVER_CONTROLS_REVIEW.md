# Constructed mover controls/audio — review

2026-10-23; code `4b4634f1`, validation `6e5c07e9`, branch `codex/pump-compressor-switch-sound`, base `b3b7a347`.

## Expected behavior

- Owner restricted this feature to constructor-built centrifugal pumps and both compressor models. One-block devices receive no switch or sound.
- Owner chose Off to stop flow through the existing zero-flow mode. At pump default 0.01 m³/s / 500 kPa reference rise, On can supply 5 kW hydraulic power at reference density; Off has Q=0 and shaft work=0. At compressor default 0.05 m³/s, ratio 3 and 1 atm suction, ideal work is about 5.57 kW; Off is zero. Existing solver limits still decide actual On flow/work.
- Keep configured flow and rise/ratio across Off/On and reload. Sound and animation follow accepted motor-command state on the engine's publication schedule.

## Previous behavior and implementation

- Before this change there was no persistent power switch or running audio; constructed moving parts animated cosmetically independent of process controls.
- `FlowControl.Mover` now retains requested flow and enabled. Its effective target is zero while off, so existing CLOSED behavior handles flow/reverse flow/work without new solver rows or modes. Two-argument authoring constructors default On.
- Only ported mover registrations may be disabled; validation rejects one-block Off settings, including saved/pending registrations. The one-block GUI/physics remain unchanged.
- Constructed controls show a translated On/Off draft button (press Apply), plus accepted power state. Existing permission, reach, revision, queue, cached replay and scheduled acknowledgement paths are retained.
- Checkpoint 15 / unit 8 saves requested flow and strict enabled flags in active/pending registrations and island/certificate graphs. Fingerprints include both. Menu protocol is fluid-12, with required ON/OFF enum validation. Fresh worlds only; no migration or old-world checks.
- Original three-second mono Vorbis loops distinguish pump motor/water noise from compressor hum/pulsation. These contain no borrowed recording and use Blocks volume, linear attenuation and volume 0.35.
- Client explicitly requests up to 64 nearest constructed nodes within 88 blocks; server validates loaded node/identity/dimension/ported mover/range 96. Protocol mover-1 replays cached state only, then sends scheduled command state; no process contents or solve on request.
- Sound is active for accepted On with a positive requested setpoint, including idle/phase-protected motors; it is not an inference of accepted fluid throughput. Loops start within 24 blocks and stop on Off/zero setpoint, binding removal/unload, range/dimension change or disconnect. Existing phase protections still prevent transport as before.
- Constructed animation uses that requested state; unknown/Off movers are at rest. Other units and the global animation debug switch keep their behavior. Identity bind/release is synchronized as static node metadata, with no process contents in block updates.

## Verification and owner checks

- Final `gradlew.bat jar --max-workers=4`: PASS. JSON parses; both Ogg assets and sound definitions are in the packaged jar; `git diff --check` clean. No tests added or run, no GUI/in-game checks performed.
- Audio authoring decoded each file as 72,000 mono frames / 24 kHz (3 s); peaks 0.234 pump / 0.337 compressor, RMS 0.093 / 0.111. Compression and loop seam metrics are recorded in the tool log.
- Owner fresh-world checklist: all three constructed kinds; toggle Off/Apply and On/Apply; retained flow/head/ratio; zero flow/work while off; reload off and pending edits; phase/reverse restrictions; read-only debugger; sound direction/volume/mute/unmute; model animation/global debug toggle; removal, layout rebind, unload, range, dimension and disconnect; one-block GUI and silence unchanged.
- Code guide: `CODE_GUIDE_ADDENDUM.md` and owned fluid-science/runtime/client/equipment chapter addenda; canonical main consolidation remains due at a future main merge.
- Tool cleanup: only runtime code and original Ogg assets tracked. Audio authoring source/dependencies and compressed logs are in canonical ignored `tools/pump-compressor-controls/`; no detached product code or new Gradle task.
- Delivered feature tip `ab35ef47`, then owner-requested fast-forward to `claude/floating-roof-tanks-81d68b` at the same commit. All ten target local changes preserved; target jar passed; restarted client left running. See MERGE_REVIEW.md. No main merge/version bump; use a fresh world for format 15 / unit 8.
