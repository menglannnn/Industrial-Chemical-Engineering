# 2026-10-21-floating-roof-tanks

Purpose: Owner request 2026-10-21: scalable floating-roof tanks in the equipment constructor, registered as fluid reservoirs that keep only their liquid at ambient pressure (an open vessel: the D54 bag kept slack, its gas vented by a one-step-lagged draw, liquid above `V_max` spilled), a roof that floats on the level and a sight glass in the liquid's colour; the impact on the fluid solver (no new Newton unknowns; worlds without an open tank bitwise unchanged); owner decisions FR1-FR8

Status: Implemented 2026-10-26 (0.12.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [FLOATING_ROOF_TANKS_PLAN.md](FLOATING_ROOF_TANKS_PLAN.md)
- [REFERENCE_PHOTO_SURVEY.md](REFERENCE_PHOTO_SURVEY.md)
- [figures/floating-roof-tank-concept.svg](figures/floating-roof-tank-concept.svg)
- [figures/storage-sphere-concept.svg](figures/storage-sphere-concept.svg)

## Code areas

`PassiveNetwork.Inventory`, `PhaseLayout.wallRow`, `ConservativeTransport`, `NetworkReduction.atRest`, `IslandCertificate`, `FluidDeviceSpec`, `FluidCheckpointCodec` (format 13 / unit 6 planned), `FluidNetwork` (`fluid-11` planned), `EquipmentShape` (`Kind.TANK`), `VesselPlacement.Form`, `EquipmentStructure`, `EquipmentNetwork` (`equipment-10` planned), client tank renderer

## History

- Implemented 2026-10-26 (0.12.0, `2178a7c2`, fast-forward, not pushed
- in progress since 2026-10-21 (branch `claude/floating-roof-tanks-81d68b` from `ef64bd06`: WP1 science `cb54c5d3`, WP2 runtime `3178bada` (format 13 / unit 6, `fluid-11`), WP4 server display path `2d799ca9`
- quick check: gate green, form B kept and form A deleted `796da897`
- campaign done `796da897` (`OPEN_TANK_CAMPAIGN_REVIEW.md`)
- seed 895 traced and the owner's hold-then-vent rule for gas-only open vessels prototyped on `4c15e508` (`OPEN_TANK_895_TRACE.md`: 895 clean, quick check 200/200, 354 and 678 another mechanism, not merged)
- seeds 354, 678, 598, 12 traced on `86776654` with the reverted harness (`OPEN_TANK_354_678_TRACE.md`: release of all start excess per step, finite release rate prototyped, quick check 200/200, owner to choose tau, not merged)
- WP3 geometry on `claude/wonderful-banach-1f1152`, merge awaits the owner
- owner change 2026-10-21: two identical bottom LIQUID ports and a top OVERFLOW port
- owner decisions 2026-10-21: version 0.11.3, FR1-FR8 as recommended, no volatile test cases, a storage sphere added (drum physics, fixed ports)
- concept sheets approved 2026-10-21 with a stair wound round both vessels)
- 2026-10-25: resumed after 85 integrated Codex commits (`BRANCH_RESUME_REVIEW.md`)
- 0.8 m pipe cap and lower skirt `a590f592`, rotatable skirted bottom outlet and boot drum oil outlet `f8532707`, every unit nozzle a 0.8 m neck with listed touches `407c3f24` (`NOZZLE_SKIRT_OUTLET_REVIEW.md`)
- quick suite 1411/1411, neck gate green
- reactor skirts `36e0f9fb`, boot drain neck `bae8620a`
- ladders `e2619287` and rolling ladder plus tank wall collision `68af29c2`, a small tank platform's inner edge railed either side of the ladder `d8746d00` with simulated-player gates (`LADDER_REVIEW.md`
- gasifier and 15 m column ladders left to the big-structure rework)
- code consolidation `7b3137bf`, `2d3593c8`, `38825518`, `36fb0965`, `65886b15` (main files 356 to 320, test files 329 to 319, every test kept, no format change
- `CONSOLIDATION_REVIEW.md`)
- owner in-game test done
- consolidation `7b3137bf`..`4f8ab994` (`CONSOLIDATION_REVIEW.md`)
- GameTests repaired (`6fe81478`, `c1d44b9b`, `0be3ef64`)
- final lane GREEN at `e8ec6455`, equal to fl8 on every held seed (`FINAL_LANE_E8EC6455_REVIEW.md`)
- merge awaits the owner)
