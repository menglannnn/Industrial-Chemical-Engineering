# Pump/compressor port arrows — plan

2026-10-23; branch `codex/mover-port-markings`, base `7234bb46`.

1. Expected: the model communicates the same direction as the fluid registration. Suction points from pipe into machine; discharge points from machine into pipe, for every heading and nozzle elevation. These are fixed design directions, including while off or disconnected.
2. Previously: models had no visible direction marking; nozzle sizes/positions alone did not identify the two process ports. Some compressor nozzle necks are hidden inside support tables.
3. Owner requested arrows on the connections, then explicitly clarified: “Arrow is enough, no words.” Use word-free black arrows on yellow panels, repeated around exposed nozzle necks and the adjacent pipe section.

- Scope: constructed centrifugal/split-case pumps, reciprocating compressor and centrifugal compressor. Read SUCTION/DISCHARGE nozzle roles; leave auxiliary coolant ports and one-block mover models alone.
- Scale decals with actual connected nozzle/pipe geometry; inherit every equipment transform. Bake into the existing chunk-model caches, with no change to hydraulic topology, collision, controls, sounds or engine schedule.
- Derive adjacent pipe port-role bits server-side with its existing geometry refresh. Include only static metadata in block updates; add it to model cache keys. No persistent/process payload or format bump.
- Overlay pipe markings only on actual exposed axial facets and merge coplanar pieces before selecting four viewing sides. This covers below-table ports without drawing through structures or across glass windows.
- Compile/package only, no tests or automated GUI/in-game checks. Render actual production mesh/materials for four machine variants, four headings and smallest/largest drawn pipe sizes.
- Record changelog, short review, guide contracts and indices; keep offline rendering source/logs in canonical tools. Deliver feature branch; current integration client keeps running, with integration/restart left for owner request.

Owner refinement, 2026-10-23: keep arrows only on the adjoining pipe section. Remove the equipment-side set entirely; SINGLE_ARROW_REVIEW.md records the resulting implementation and checks.
