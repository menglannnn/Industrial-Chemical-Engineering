# Square flange shoulders review

2026-10-23 · Square shoulders `acd0fd14`, seam/inspection `fa81579b`; target `claude/floating-roof-tanks-81d68b` updated to `7bf50b7c`.

## Expected behaviour

- The owner requested a flat flange plate joined to a constant-diameter pipe with a sharp 90° shoulder, removing the conical neck shown in the screenshot.
- At the rendered 0.40625 m pipe OD, neck OD remains 0.40625 m up to the 0.5928125 m flange; the radial step is 0.09328125 m.
- At the capped 1 m OD extreme, the old profile was already 1 m at the plate, so that extreme is unchanged. Hydraulic/thermal calculations and conserved quantities are unchanged at all sizes.

## What the code did

- `PipeSizes.hubOutside` grew the neck to 1.15 times pipe OD, capped by flange OD; the rendered pipe grew to 0.4671875 m, producing the sloping shoulder.
- This shared size drives pipe spool/end flanges, pipeline devices and equipment nozzles.

## Implemented proposal and owner validation

- The owner explicitly chose removal of the sloped neck. `hubOutside` now returns the pipe OD; existing equal-radius frustums render as cylinders, and each flange's flat back makes the 90° shoulder.
- Flange outside diameter/thickness and straight-neck length are unchanged. The shared inner-radius/bolt-clearance calculation follows the straight neck on both mating halves.
- Updated the shared size/pipe geometry comments. No replacement model, compatibility code, new test or production probe.
- Actual-mesh side renders in `renders/before.png` and `renders/after.png` confirm the slope is gone; both have 317 polygons.
- `gradlew.bat jar --max-workers=4`: PASS. The owner saved and closed the dev client before the required merge suites were started.
- Owner subsequently said no tests were needed. Stopped the already-started `test slowGates` run immediately; no further tests were run. The known nozzle-clearance gate had emitted its existing failure before cancellation; this run is not recorded as a completed suite.
- Fast-forwarded the target after checking no overlap with local edits; all ten pre-existing modified/untracked target files were SHA-256 verified unchanged. No version bump or main merge.
- Relaunched `runClient` from the updated target for owner judgement; no automated in-game check.
- Code-guide draft: square-flange addendum in equipment chapter, snapshot `13b30376`, copied into this batch for the future main merge.
- Local tooling: `tools/square-flanges/` (actual-mesh render harness, exact `b3bb813e` baseline sizing source, README and compressed logs).
- Status: In progress since 2026-10-23; main integration and version choice remain separate.

## Seam and pipe-inspection follow-up

- Code `fa81579b`: spool flange rims split into two plates with a 1/128 m (7.8125 mm) dark center band, using existing OPENING material and the same outside envelope.
- Joined pipeline end flanges each draw a half-width edge band; the line half also makes a seam visible against equipment nozzles. Blind/unconnected flanges retain the plain rim. `End.joined` carries only this drawing distinction and survives fitting shortening.
- Actual-mesh `renders/seam.png` shows the junction line; the sample is 335 polygons (317 before the stripe). The bore, flange dimensions, collisions and simulation are unchanged.
- `FluidDeviceBlock.useWithoutItem` passes pipe clicks through. The existing Fluid Debugger path alone opens inspection; other device GUIs and Pipeline Tool actions are preserved. Removed the now-unneeded empty-main-hand/offhand tool exception.
- `gradlew.bat jar --max-workers=4`: PASS. No tests were run for these follow-ups, per the owner's instruction.
- Target fast-forwarded from `13b30376` to `7bf50b7c`; all ten pre-existing local files preserved byte-for-byte. Client relaunched there at 11:18 SGT for owner judgement; no automatic in-game check.
