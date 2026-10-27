# Pressure valve accessory and cutoff lever review

2026-10-24. Implementation dc174f27 / delivery 4ad6dc97; branch codex/pressure-valve-accessory from 2fe26e21, owned pipeline-tool worktree. Integrated into claude/floating-roof-tanks-81d68b at 4ad6dc97 on 2026-10-24.

- Expected: a pressure-control accessory installs on a straight pipe, keeps its bore/identity/settings and uses the existing regulating law. The model has a full-height actuator; the cutoff lever looks heavier.
- Previous behavior: pressure valves were separate block items. Their actuator was shrunk into one cell; the cutoff lever was short/thin.
- Delivered: pressure_control_valve is now a stack-64 creative/command accessory, no recipe. Either hand installs; survival consumes one and ordinary breaking drops pipe plus accessory. Duplicate installs consume nothing. Corners, junctions and existing accessories are refused.
- Aim along the run selects the flow arrow. Installed pressure_control blockstate locks the existing facing axis; later side pipes cannot connect. Empty-hand use opens existing pressure controls (default 200000 Pa); debugger and Pipeline Tool retain their actions.
- Registration remains Kind.PIPE and keeps the same block entity, ID, geometry, spec and pending edits. FlowControl.PressureValve determines hydraulicKind VALVE for compilation and GUI. This keeps physical pipe connectivity, including direct boundary connections, and its own sized resistance. No new constitutive equation or solver mode.
- Shared registry validation checks projected neighbors/revision and rejects another valve on a pressure host. Invalid pressure-plus-cutoff registrations are rejected. Saves carry the existing pressure control in active/pending registrations; checkpoint 18/unit 11 and protocol fluid-13 stay unchanged.
- Chunk rebuilding reads the blockstate and creates the pressure control directly, never an intermediate passive registration/forced solve. Publication restores committed static orientation/flag. Cached menu replay and approximately five-second engine publications are unchanged.
- Removed standalone pressure-valve block registration, model/blockstate/loot assets and block tag references. No compatibility/migration path; use fresh-world manual checks.
- PressureValveGeometry has a globe casting, one facet-fitted arrow band clear of the bonnet, bolted bonnet, sealed packing head, exposed stem/yoke and a bolted diaphragm. Horizontal actuator overhangs upward; vertical actuator points north. Largest model top is 2.0298 blocks above host origin; lateral envelope remains within its cell.
- Shared LineGeometry.overhanging retains clipped decorative pieces beyond the host cell. Host collision remains ordinary pipe collision. The old compressed actuator builder is replaced by the shared new builder.
- Shutoff lever is 0.36-0.95 m long, at least 0.065 m wide and 0.035 m thick, with a wider root, underside rib and heavy yellow grip/end cap. Parallel=open and crosswise=closed remain unchanged.
- GUI item transforms fit both icons: pressure 8.041 x 14.240 px, cutoff 14.244 x 13.277 px within a 16 px slot.
- Verification: 3 new PipelinePressureValveTest cases, 4 existing shutoff cases, 4 existing check cases and 7 existing LineGeometryTest gates pass; jar/material verification pass. No full suite or game/GUI automation.
- Existing geometry envelope gate now permits actuator overhang only in its stem direction; UV/outward-winding/ray-facing gates remain. The ray gate found missing bonnet/diaphragm end caps, which were closed before the final pass.
- 39 software scenes cover all axes, diameter extremes, open/closed cutoff, both pressure directions, standalone accessories and the old/new cutoff comparison. Final preview, details, axis sheets and icon sheet visually reviewed.
- Tools: tools/pressure-valve-accessory/; no probe/harness added to product paths. Renderer baseline is 2fe26e21 shutoff geometry with only the class renamed. Compressed logs and summaries retained there.
- Code guide: equipment, fluid-runtime, client-gui and verification updated from this branch. Integration 4ad6dc97 retains mod 0.11.2 and formats 18/11; client launch follows the required merge checks.

Manual fresh-world checklist: either-hand installation, survival consume/refund, duplicate/exclusive-accessory rejection, locked sides, arrow direction, pressure GUI/validation, cached/scheduled updates, debugger, both Pipeline Tool actions, resize/reload, actuator overhang and heavy-lever open/closed appearance.
