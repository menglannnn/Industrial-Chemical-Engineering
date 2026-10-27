# Fluid display follow-up plan

2026-10-23. Owner reports tiny sight-glass/valve inventory models and an isolated WTI generator phase split. Base integration `beb5c2fc`; owned branch `codex/fluid-display-followups`.

- Give only the two accessory item models dedicated GUI transforms, sized to fill a 16 px inventory slot without clipping. Preserve installed geometry, collision, hydraulics and other item display contexts.
- Trace the generator bar to its published fixed-source phase state. Reproduce WTI Light Export at 473.15 K / 300000 Pa using the current network preset and the same FluidDeviceSpec.initialize path; compare volume, mass and mole fractions and verify isolated compilation preserves that state.
- Owner approved a Volume/Mass selector after seeing 2.06% liquid by volume versus 66.47% by mass. Keep it separate from composition-entry/flow-rate basis, remember it only for the client session, include fluid moles times kg/mol and solids kg, and label the selected basis clearly.
- Read only cached client state when switching display. No flash, process input, solve, payload or format change; no migrations or old-world testing. Fresh model data for the audit.
- Compile/package and draw actual-item-mesh before/after slot previews offline. No new tests or automated GUI checks; all test suites must wait until the user's active client is closed. Required merge checks remain applicable.
- Save the concrete review, code-guide contracts and tooling evidence. Integrate/relaunch only after the user saves/closes the active client; preserve the ten unrelated target-local files. No main merge or version bump.
