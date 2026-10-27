# Functional pump and compressor equipment

Accepted by the owner on 2026-10-23. Branch `codex/functional-pumps-compressors`, based on `7bf50b7c`, in the existing owned `pipeline-tool/CreateChemE` worktree.

## Intended behavior

- Activate constructor-built centrifugal pumps, reciprocating compressors and centrifugal compressors through their existing suction and discharge nozzles.
- Reuse the existing self-powered controls, phase protections, resistance, work and heat accounting. Visual dimensions do not set operating limits; no Create kinetics, efficiency setting or new solver model.
- Defaults: pump 0.01 m³/s and 500000 Pa reference pressure rise; compressors 0.05 m³/s and maximum pressure ratio 3.
- Register only whole builds, as one device per structure. Auxiliary oil-cooler ports remain decorative.

## Connection and lifecycle contracts

- Extend physical ports with explicit vessel/suction/discharge roles; a multi-cell mover has one bulk suction and one bulk discharge. Its suction flange cell is its identity binding and hydraulic elevation.
- Compile drive direction from the connected nozzle, including perpendicular nozzles, differing elevations and rotated structures. Keep one-block devices and reservoir phase ports working through the same adapter.
- Construction, edits, dismantling and layout replacement use queued engine events. Reload binds saved identities without registration or a solve.
- Layout refresh keeps controls, paint and nozzle sizes; moving the suction binding queues removal plus a new identity retaining settings.
- Persist roles for active registrations and pending edits. Fluid checkpoint 13 → 14, unit format 6 → 7; validate new data, require a fresh world, add no migration.

## GUI contracts

- Normal right-click on any machine cell opens existing operating controls, with its datasheet tab and equipment title.
- The existing Fluid Debugger opens diagnostics. Individual-pipe inspection remains debugger-only; both Pipeline Tool actions remain as implemented.
- Reuse server validation, identity/revision checks, cached menu-open replay, subscriptions and scheduled publications. Opening and loading never force a solve.

## Verification and delivery

- Owner requests no tests: add no tests, run compilation/package generation only, leave all fresh-world in-game checks to the owner.
- Manual checklist: all three designs, rotated/perpendicular/elevated ports, controls/limits/phase protection, reload/settings/identity, disconnection/dismantling, GUI/debugger access and existing one-block devices.
- Add an Unreleased changelog line; record a short review, contract addendum and batch index entry.
- Deliver the feature branch without a merge to main or a mod-version bump.
