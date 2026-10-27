# Constructed pump/compressor power and audio — branch contracts

Snapshot: `ab35ef47` (`claude/floating-roof-tanks-81d68b`), 2026-10-23, implementation `4b4634f1` and validation `6e5c07e9`, fast-forwarded from `b3b7a347`. These branch facts await consolidation at a future main merge; preserve other branches' guide changes.

## Actuator science (`fluid-science.md`)

- `FlowControl.Pump(requestedVolumeFlow, maximumAddedPressure, enabled)` and `Compressor(requestedVolumeFlow, maximumPressureRatio, enabled)` retain requested settings while Off. Their two-argument authoring constructors set enabled=true.
- `Mover.requestedVolumeFlow()` is configuration; `enabled()` is the command; default `targetVolumeFlow()` returns requested flow when On and zero when Off. Every existing solver/reduction/inlet/transport call continues to use that effective target, with no new equations or modes.
- Existing zero-target CLOSED behavior provides zero transport/work for Off, retaining reverse-flow and phase protections, cavitation, rise scaling, ratio limits and ideal shaft-heat accounting when On. No Create power connection or efficiency setting is added.
- `PhysicalFluidTopology.Device` rejects disabled movers with an empty port list. Only the existing two-role constructed registrations can use the switch; one-block movers retain enabled=true and existing zero-setpoint behavior.
- `IslandCertificate` graph fingerprints now include requested flow and enabled, rather than only effective flow, for strict configuration/replay identity.

## Runtime, persistence and watch protocol (`fluid-runtime.md`)

- `FluidCheckpointCodec.VERSION=15`, `UNIT_FORMAT=8`. Pump/compressor control encodings append strict bool enabled after requested flow and pressure rise/ratio. Both registration/pending-edit and island/certificate-graph writers/readers use these fields. Unknown bool bytes are rejected by `Reader.bool`; numeric constructors retain finite/range validation. All remaining clocks/material/events/optimization encoding is retained. No old formats or migrations.
- `FluidNetwork.PROTOCOL=fluid-12`. `Controls` includes required `Power.ON/OFF`; missing/unknown enums are rejected. `Controls.from` reads requested setpoints and accepted power, and queued edits use the full constructor. Non-ported Off edits fail device validation. GUI admission/revisions/queues/replay/scheduled results keep the existing path.
- `MoverNetwork`, protocol mover-1 on MAIN handlers: C2S watch/unwatch lists, each <=64 unique position+positive-identity targets; S2C `State(target, compressor, running)`, strict bool bytes. It transmits no thermodynamic state or inventory.
- `FluidWorldAuthority.watchMovers` requires a loaded matching receiver, nonempty-port mover registration, exact cell/dimension and distance <=96 blocks. The bounded bidirectional `TankWatchers` bookkeeping is a separate instance from tank/sight-glass watchers.
- `publishedMovers` caches only scheduled state. New watches replay that cache without building a view or advancing simulation, then observe future presentation buckets. Ported-device `PresentationHost.present` publishes command On && requestedFlow>0; actual fluid throughput is not used to infer motor operation.
- `refreshObserved` combines sight-glass and mover requests before observe/unobserve, so one request family cannot cancel another. Removal/unload/logout releases subscriptions; recompilation invalidates caches and rekeys buckets; close clears cached states.
- Equipment binding identity is static metadata in update tags. Bind and release send its block update, enabling client identity checks without putting process state in those packets.

## GUI, sound and animation (`client-gui.md`)

- `FluidDeviceScreen.constructedMover` requires pump/compressor menu kind and equipment datasheet. Only those controls add an On/Off draft button at the top of the scrolled rail; Apply submits it with setpoints. A separate power-state value shows accepted power. Existing one-block field layout is retained; debugger is read-only.
- `MoverSounds` is client-thread-only. A weak set holds only constructed mover node cells, updated by equipment load/data-packet/removal hooks. Identity and current layout binding are checked before accepting or retaining state.
- Once per 20 client ticks, request nearest <=64 nodes within 88 blocks. Position+identity cache and watch sets are ephemeral and cleared/released on lifecycle/range/dimension/disconnect. Already-loaded new-world bindings survive the first level-change poll.
- Play an `AbstractTickableSoundInstance` per running nearby node only within 24 blocks, volume 0.35, Blocks source and linear attenuation. Master/Blocks mute stops it; polling restarts after unmute/resource reload. Loop ticks only validate lifetime/distance; they compute no process state.
- `ModSounds` registers `icheme:pump_running` and `icheme:compressor_running`, mapped by sounds.json to original mono 24 kHz, 3-second Vorbis loops, with translated subtitles. Pump/compressor timbres are procedural authoring output, not borrowed audio.
- `MachineAnimation` selects moving time or time zero from `MoverSounds.running` once per pivot cell. Unknown/Off/zero-setpoint constructed movers are at rest. Non-mover animations and the existing global debug animation toggle are unchanged.

## Equipment lifecycle (`equipment.md`)

- Scope is constructor-built centrifugal pump, reciprocating compressor and centrifugal compressor. Partially built structures remain unregistered/inactive. Existing suction-node identity and discharge-role topology are retained.
- `EquipmentShellBlockEntity.getUpdateTag` now includes its binding identity; bind/release send static metadata updates. `trackMoving` registers the eligible node with the client sound watcher, and release removes it. A cell must match `ShapeRasterizer.fluidCell(shape)` at the current origin to be watched.
- Existing geometry/paint/nozzle/control retention during refresh carries the complete immutable mover control, including enabled and requested flow. No new equipment format or collision/layout change is introduced.
