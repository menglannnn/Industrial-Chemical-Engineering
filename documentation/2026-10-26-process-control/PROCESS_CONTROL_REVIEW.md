# Process-control implementation review
2026-10-27. Branch `codex/process-control`, base main `600549d9` (0.13.2); unmerged.

## Expected behaviour
Pressure/temperature/flow/level sensors report accepted measurements; ideal local valves regulate within the fluid solve, independently of menus. Shared geometry drives height, hydrostatic head and the fixed D/3 boot weir. Ordinary bottoms co-draw equal liquid volumes; boot drain and HC outlet retain their approved fallback orders.

## Change and result
- `0f3a569a`: sensors/housings/ghosts/GUI, instance identities and scheduled installation/recovery; modular pressure/flow targets; shared geometry; simultaneous/conditional draws and conservative weir split.
- `21193cbf`: the chunk-load GameTest permits the new static Sensors list, while still rejecting live process data.
- `a33ec4a6`: the block entity recognizes every Valve implementation and retains its forward direction; the native client exposed the old PressureValve-only check. A new GameTest pins projected and committed refreshes.
- `b92385c6`: retain the registered local actuator bore through compressed runs, fixed-opening equations, identity/certificates and checkpoints; narrower connected pipes do not redefine the opening characteristic.
- Bare valve: fully open / “Lacking components”. One P or flow module only. SI defaults: 200 kPa absolute and .01 m³/s actual. No PID, actuator lag or shaft work.
- Geometry covers existing cylinder/sphere/drum-head/boot capacities without moving nozzles or changing layout/stock. Percentages are height percentages; unavailable is explicit, accepted empty level is zero, overrange remains raw.
- The weir complementarity includes simultaneous arrivals/other drains. Independent constraints solve together; dependent mass rows/shared pressure taps hold accepted openings with affected-loop status. Target limits do not hold islands.
- Fluid facts/accessory facts schema 2 retain old images and add new corpus images. Geometry and modular settings append with old-behaviour defaults; old registry IDs remain. State format 22 rebuilds incompatible caches. Legacy placed pressure valves convert once; probe removal survives reload.
- `ControlBinding` and DATA_SYSTEM_CONTRACT.md define later level endpoint/target/action/fault behaviour. Cables and remote level regulation are deferred.

## Verification
- Final model lane at `01c5250c`: quick 1476/1476 and slow 187/187, no skips/errors. All 33 liquid/mixed-gas reference lines match archived 8d4c8df6 apart from time/allocation; MIXED_GAS_COST = 299 Newton solves, 94.6 MB. Chain-100 exact replay has zero deviations.
- Fresh fluid GameTests: 23/23 after correcting the static-metadata whitelist. Post-client fitting regression: 24/24 at `a33ec4a6`.
- One MCP dev-client session, pinned bridge 0.4.18/background mode: actual item fitting/modules, duplicate-source rejection, shift-wrench removal/replacement, blank housings/ghosts, unavailable readings, 25 °C flowing temperature, empty digital level 0 m/0%, and actual-volume regulation at .0001 m³/s (about .1 kg/s, .3% opening). The two-method fitting fix was hot-reloaded in that same owned client, which then closed before testing.
- Focused local-bore, carry, topology and checkpoint/old-corpus checks pass. After tooling cleanup, current-code quick 1477/1477 and slow 187/187 pass, no skips/errors (`post-review-gates.log`, `post-review-results/`).
- The single 1000-seed campaign predates the final fitting/bore metadata refinements; its selected outlet/regulation equations are unchanged by those refinements. Counter and class evidence is retained, without another campaign.
- Campaign clean counts: cold random 975/1000; bundled random 993/1000; bundled defaults 1000/1000. No new C/F holds; one new primary E5 world, seed 236. See CONTROL_NUMERICS_REVIEW.md.

## Owner review before merge
The feature is implemented on the branch. Seed 236 remains an unresolved numerical regression, minimized/traced to a nearly water-full vessel with an extremely small HC phase; the exact mathematical cause is not established. Resolve this before merge, or explicitly accept the documented limitation. No new phase cutoff or physics simplification was introduced.
Release proposal: 0.14.0 (minor feature/model batch); `mod_version` remains 0.13.2 until the owner chooses. No merge/push/PR was performed.
Code guide: process-control chapter added; instrument-accessories, fluid-science, fluid-runtime, equipment, client-gui, verification and README updated from this branch.
Tooling: `tools/process-control/` (canonical main checkout); sources/probes never enter the mod or gate source sets. Logs are compressed there; native screenshots are under evidence/. Unmerged scratch outputs are retained in the indexed worktree tmp/.
