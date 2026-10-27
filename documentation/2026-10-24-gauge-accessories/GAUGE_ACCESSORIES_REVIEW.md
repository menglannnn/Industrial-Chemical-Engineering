# Gauge accessories review

- Feature commit: `e927f599`, `codex/gauge-accessories`, based on `1ba146c4`; no automatic merge/version bump.
- Added pressure/temperature items, range editor, separate reservoir sockets, pipe mounts, requested readings and reusable wrench.
- Defaults: 0–10 bar(g), 0–200°C; finite absolute SI endpoints remain fixed across display/reference changes.
- Instruments add no fluid storage/nodes/limits/flashes. Existing control, port-head and section-loss laws supply accepted observations.
- Physical mapping covers ordinary and reversed runs, proper halves between opposing actuators, and excludes disconnected/pruned pipes.
- No-flow temperature retains its last valid reading with stale status; observation/display caches are ephemeral.
- Accessory storage1 persists configured gauges, fitting instances/revisions, slot tombstones and pending player operations/deadline.
- Installation/removal revalidates target, item/hand, reach/occlusion, permission and revision before transfer; refund removal is idempotent.
- Existing valve removal composes with pending bore edits; all axes preserve identity, contents and conservation accounting.
- Pipe body loot and registry recovery have separate ownership; no duplicated accessory loot. Creative breaking suppresses refunds.
- Native refresh keeps stable attachment IDs/mount names and updated transforms; relocated reservoir stock keeps its identity/inventory.
- Owner refinements: previews/highlights only on constructed equipment; pipe use/readout/recovery remain. Native steel preview alpha56/88 and thin muted ring alpha135 replace neon fill/cuboids.
- Common pressure units are server-safe and shared with fluid GUI. Static BE metadata contains no live readings.
- Protocol instruments-1, checkpoint18/unit11/fluid-14 and mod0.11.2 retained.
- Validation: **61 focused tests passed**, package generation passed; no new GameTests or GUI automation suite.
- Classes: GaugeAccessoriesTest9, PipelineShutoffValveTest5, PhysicalFluidTopologyTest16, FluidGuiConsistencyTest4, RestResolutionTest4, IslandCertificateTest23.
- Software verification: **125 views**, 53 outward front/tick faces, clamped endpoint/over-range needles, separate face/tick/label/needle layers; min/default/max pipes and all reservoir headings/sizes.
- One fresh-world MCP lane used the pinned osguard bridge: item/ghost assets, scheduled installation, upright dial, hover range and isolated-pressure unavailable confirmed. Client closed cleanly.
- MCP initially needed pauseOnLostFocus:false; commands were entered through its ChatScreen because its execute_command KubeJS fallback was incompatible. No desktop input was used.
- Range editor interaction was not completed through the bridge; its broader in-game validation remains owner-run. Final highlight-only refinements were judged by software renders.
- Full quick/slow merge qualification was deliberately not run for this feature delivery; it remains mandatory before any later merge. Prior integration baseline failures are recorded in the existing integration batch, not claimed resolved here.
- Tooling: canonical `tools/gauge-accessories/`; no tracked temporary harness/task or measurement switch was introduced or detached.
- Code guide: new instrument-accessories chapter and updates to equipment/client-gui/fluid-runtime/verification.

Manual checks remaining: both-hand configuration and Save/Cancel/Enter/Escape; survival consume/refund/full inventory; invalid/occupied mounts; range/ref display changes; top/bottom pressure versus reservoir; flowing/stopped temperature; pipe corner invalidation; every valve/wrench interaction; chunk reload/native refresh; multiplayer/range/dimension release.
