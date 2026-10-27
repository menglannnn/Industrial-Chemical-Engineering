# Pipe-applied pressure control valve and heavier shutoff lever

2026-10-24, codex/pressure-valve-accessory, based on 2fe26e21 in the owned pipeline-tool worktree.

- Convert the existing pressure_control_valve item into a stack-64 accessory for straight/end/unconnected pipes, both hands, creative listing, no recipe, survival consume/refund, duplicate no-op and one accessory per pipe.
- Aim along the run selects its flow direction. Axis locks after installation; empty-hand interaction opens existing pressure controls, initially 200 kPa. Preserve debugger and both Pipeline Tool actions.
- Keep the host pipe, block entity, identity, spec and queued bore edits. Reuse PressureValve control and compile that host as a valve actuator; no new solver law or process update on interaction/load.
- Installation blockstate is pressure_control; existing persisted control discriminates the hydraulic function. Retain checkpoint 18/unit 11 and protocol; no migration or old-world checks. Remove standalone pressure block registration/assets.
- Globe casting, visible arrow, bolted bonnet, open yoke/stem and large diaphragm actuator; decorative overhang above the host cell is retained. Vertical runs mount actuator northward. Ordinary host-pipe collision remains.
- Increase shutoff lever length, width and thickness; add reinforced root, underside rib and heavier yellow grip/end cap.
- One focused adapter test class plus compile/package; render all axes, minimum/default/maximum diameters and open/closed levers. No new GameTests/GUI automation or in-game check.
- Document contracts, short review and indexed local tooling; deliver feature branch without automatic merge/version bump.
