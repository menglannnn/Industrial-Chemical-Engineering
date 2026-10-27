# WP3 ladders review: wound stairs only when big enough (2026-10-21)

Owner change, quoted: "Only put round ladders on them when they are big enough". Commit `768fc93d` on branch
`claude/wonderful-banach-1f1152`, on `a16977ab`. Not merged or pushed. Only the two storage designs, the lang file
and three tests changed. The roof and ladder hooks (`ShapeRasterizer.floating`) and the nozzles are unchanged.

## Thresholds (one rule per kind)
- **Tank: wound stair from D ≥ 6 m** (`TankDesign.STAIR_SIZE`).
  - In practice spiral tank stairs start at about 20 ft (6 m) of diameter. That figure is an engineering forum rule of
    thumb, not a code value.
  - It is also just above API 650 Annex J's shop-assembled tanks (6 m at most), which carry caged ladders.
  - Render `tank-6x8`: at D 6 the stair still reads as a stair, but its access dominates the tank. Raising the
    threshold is a one-constant change if the owner prefers.
- **Sphere: wound stair from D ≥ 10 m** (`SphereDesign.STAIR_SIZE`).
  - The approved stair follows the upper half to the top. Its landing needs the top platform out to where the stair
    leaves the legs.
  - Under 10 m that is more than half the radius: rp/r = 0.85 at D 4, 0.57 at D 8 and 0.51 at D 10. So a smaller
    sphere's stair never reaches the shell it is meant to follow.

## Below the thresholds
- **Tank:**
  - Caged ladders up the back to the gauger's platform, with part-ring rests every 9 m at most.
  - The ladders alternate between the right side and the back (the columns' `ringAccess` pattern), clear of the
    front's nozzles and sight glass.
  - There is no walkway: a small tank's top angle is its wind girder.
  - The platform is open beside the top ladder, with a short rail over the tank's inside.
- **Sphere:**
  - The kit's caged climb (`ProcessUnit.climb`) up the back, between two legs.
  - Over 9 m the climb has a rest on four posts (every ladder sphere on 8 m legs, for example).
  - A post stands behind each ladder section.
  - A 1.1 m bridge runs from the ladder into the crown ring, railed both sides and braced to the sphere.
- **Old saves:** small vessels placed with the stair layout carry the old layout's stamp. `EquipmentRefresh` rebuilds
  them to the ladder layout when they load. The refresh stamps test now covers both sides of each threshold.

## Gates (one Gradle run, after the dev client had exited)
- `./gradlew test`: 1338 tests, 0 failures.
- `slowGates --tests ProcessUnitTest`: 19/19.
- Shape sweep (`ShapeRasterizerTest`, 32/32): every storage size walked, wound stair at or above each threshold and
  ladders below, each climb at most 9 m, every tank's floating parts, pipes one block apart. The new SHAPES entries,
  tank 5×22 (two rests) and sphere 9 on 8 (one rest), are walked in the box. `EquipmentRefreshTest`: 5/5.

## Renders
`figures/wp3-ladders/` (26 PNGs; tool `tools/storage-vessel-renders/`, `render.sh` updated): ladder tanks 4×4 and
5×22, ladder spheres 4 on 8 and 9 on 8, the smallest stair tank 6×8, and the unchanged tank 40×24 and sphere 24 on 2.

## Open (unchanged)
- On a tall narrow tank the rolling ladder (length H) still passes through the shell; see `tank-5x22`. This is item 1
  of `WP3_GEOMETRY_REVIEW.md`.

Code guide: update `equipment.md` at the merge (the stair sizes, the ladder forms).
