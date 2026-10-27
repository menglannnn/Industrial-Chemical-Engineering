# Uniform fluid equipment GUI

2026-10-24. Owner requests separate reservoir top/bottom pressures, valve flow, shared phase ratios and pressure units, with common GUI code. Volume/Mole/Mass selector confirmed by owner.

- Branch codex/fluid-gui-consistency in owned pipeline-tool worktree, based on e518862d (includes pending handle-tip fix).
- Publish tank capacity and top/bottom absolute pressures from the same existing condensed-head arithmetic used by outlets; empty reservoir pressures are undefined and capacity remains visible.
- Reuse accepted transport display for pump/compressor/valve flows and phase tables; keep generator configured-feed and reservoir stored-content meanings.
- Shared Volume/Mole/Mass selector on every fluid equipment screen. Moles include fluid phases and conserved-component crystals; unknown custom-solid molar amounts are explicitly excluded.
- Shared Pa/kPa/bar/MPa selector converts all pressure readouts, gradients, heads and inputs; SI-aware numeric drafts retain untouched original values through repeated unit changes. Flow time and temperature use the same draft mechanism.
- One readings-row plan drives values, phase selector, status and scrolling. Common tabs/footer fit minimum 300px width; compact phase tabs retain full hover names. Keep full status and clipping/scrolling.
- Extend live Tank presentation and bump wire protocol fluid-13 to fluid-14, with strict validation; checkpoint 18/unit 11 and simulation physics/schedule stay unchanged. No compatibility/migration or old-world tests.
- Focused unit/DTO/presentation checks and package build; at most one fresh-world GUI review through the pinned Minecraft MCP lane if control is available. No GameTests or real desktop input.
- Record review, index, code-guide contracts and compressed local evidence; deliver feature branch without automatic merge/version bump.
