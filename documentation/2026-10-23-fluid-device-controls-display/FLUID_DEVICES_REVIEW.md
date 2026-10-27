# Fluid device controls and displays review

Date: 2026-10-23. Code: `0f208c52`, `89168361`; branch `codex/fluid-device-controls-display`.
Status: integrated into `claude/floating-roof-tanks-81d68b` at `232b0466`; owner in-game checks and main integration pending. See INTEGRATION_REVIEW.md.

## Expected behaviour
- A void removing 1 mol/s shows 0.001 kmol/s or 3.6 kmol/h of the actual removed components and their phases/solids.
- A new generator produces zero flow until enabled. Switching Off removes its hydraulic source contacts while preserving its configured feed.
- A 0.01 m3/s suction setting displays 36 m3/h; changing the display basis leaves the engine setting unchanged.

## Previous behaviour
- The void table read its dummy nitrogen boundary state, not the stream being removed.
- Generators were always connected. GUI summaries and transport tables assumed hours while suction-flow inputs used seconds.

## Implemented owner request
- `PipePresentation.boundaryInflow` reduces accepted incoming streams only. Each island is divided by its own accepted duration before rates are summed; the payload expresses one second of transport, with no fictitious void contents.
- `Device.generatorEnabled` is serialized for active and pending registrations. Placement defaults Off; server-validated Apply queues the edit; rotation retains the flag. Non-generator false flags and malformed boolean bytes are rejected.
- The existing topology event/recompile path closes/opens contacts. No new solver model, process calculation on open, or unsolicited publication was introduced.
- `FlowRateUnit` and the shared screen apply the same /s or /h basis to summaries, connections, transported components/solids and suction drafts. Captured solids and vessel stocks remain quantities; velocity remains m/s.
- Unit conversion preserves valid drafts; invalid draft conversion reports an error. Compact footer labels retain five usable controls at minimum width. Phase selection follows available contents until the player chooses a phase.
- Checkpoint 15 -> 16, storage unit 8 -> 9, menu fluid-12 -> fluid-13. Existing incompatible-format reset applies: fluid inventory/settings are discarded and registrations rebuild from placed blocks; use a fresh world. No migration added.

- Owner follow-up: generator feed now has the shared four-phase volume ratio bar. Reservoir volume reads current / maximum L from State.volume and State.capacity; hover includes both full values.

## Verification and delivery
- `gradlew.bat jar --max-workers=4`: BUILD SUCCESSFUL (JDK 21), including final builds after automatic phase selection and the phase-bar/capacity follow-up.
- `git diff --check` passed; tracked ignored-file inventory empty. No tests, GameTests, automated GUI checks, or in-game interaction run, following owner preference.
- Initial feature delivery left the integration client alone. Subsequent owner-requested integration at `232b0466` includes the single-layer arrow fix; regular client launched, no main merge or version bump.
- Code guide: `CODE_GUIDE_ADDENDUM.md` and owned guide chapter addenda cover runtime, GUI and format contracts; canonical main chapter bodies remain main facts.
- Tooling: `tools/fluid-device-controls-display/` holds compressed build logs only; no investigation code or harness was added.

## Owner fresh-world checklist
- Place generator connected by pipe to a void: default Off, then Apply On/Off and wait for scheduled results. Verify settings and power survive reload and rotation.
- Void water, gas, mixed phases and slurry; inspect percentages and both component rates. Exercise multiple attached islands, no flow and initial publication before flow starts.
- Toggle /s and /h in pipe/debugger, tank connection, pump, compressor, generator and void screens; 0.01 m3/s <-> 36 m3/h without a setpoint change. Check SI bounds on Apply and invalid drafts.
- Confirm transported solids use the selected rate basis, captured solids/tank inventory stay kg, manual phase selection stays put, and the footer/rail scroll at small GUI sizes.
- Generator feed phase fractions should match its configured phases, including solids; check the bar while On and Off. Reservoir current / maximum L should keep the fixed capacity as fill changes, including empty and full states and compact-screen hover.
