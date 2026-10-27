# Pump/compressor power and sound

2026-10-23; owned branch `codex/pump-compressor-switch-sound`, base integration `b3b7a347`. Owner requests an on/off switch and running sound; explicitly chose **stop flow using the existing zero-flow mode** when off.

- Add an enabled flag to pump/compressor controls, default on. Preserve requested flow and pressure-rise/ratio limits while off; effective solver target becomes zero through the existing closed mode. Retain all phase/reverse-flow/work/heat rules.
- Owner scope correction: **only constructed machines** (centrifugal pump, reciprocating compressor, centrifugal compressor). One-block devices keep their behavior and receive no switch, sound or animation changes. Reject disabling one-block registrations. Use the existing GUI, permission/revision checks, queued edits, cached menu replay and scheduled publications.
- Add a translated On/Off control to the operating rail. It is applied with existing Apply; accepted control state is authoritative.
- Persist requested flow plus enabled in active/pending registrations and unit controls. Bump fluid checkpoint 14 -> 15, unit 7 -> 8 and menu protocol fluid-11 -> fluid-12. Validate all reads; no migrations or old-world checks.
- Add original seamless pump/compressor loop assets with positional attenuation and Blocks-category volume. Sound follows published enabled state (and a positive requested flow), not solver work in the render/tick hook.
- Explicit client subscriptions request bounded nearby mover states; replay cached state only, then update on existing engine buckets. Stop loops on Off, zero setpoint, removal, unload, range/dimension change or disconnect. No contents in ordinary block updates.
- Reuse that requested state to stop constructed-mover animation while off, preserving the independent existing global animation debug switch and other moving units.
- Keep target client's current run untouched. Compile/package only, no tests or automated GUI checks; owner performs fresh-world functional/audio checks. No automatic merge/version bump.
- Record changelog, short review, guide contracts and index entries. Keep audio authoring tools/dependencies/logs in canonical ignored `tools/pump-compressor-controls/`.
