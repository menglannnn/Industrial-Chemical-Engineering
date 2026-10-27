# Final test of the custom equipment rendering merge (m21, Opus, 2026-10-20)

Lane at `8f9009b3` (merge `6cd0d020` of `main` 0.9.2 + `03be157b` + `8f9009b3`). Baseline: m20 (`bb9de943`, code = `449d398c`), `tools/crystal-junction-holds/logs/m20-logs.tar.gz`. Alone on Gradle; the owner's two geosim JVMs ran. No code change, no commit. Logs: `tools/new-units/logs/merge-final-lane-2026-10-20.tar.gz` (main checkout).

## Verdict: NOT GREEN
1 test fails and the campaign is not identical. Every difference comes from OD4, which is on the branch since `d1701780f` (2026-10-16): a pump's, compressor's or valve's half link takes the bore of the pipe it joins. Main has no OD4. With OD4 off (patch `09-od4off/rule-off-od4-half-link.patch`), the 3 moved tests and every moved seed are identical to 0.9.2. The merge's own fluid changes are nothing in worlds without drums. `MERGE_PREP_REVIEW.md` says "Main kept the OD4 half link and the RWCWR rig". That is wrong: `449d398c` has neither (its rig is RWCPR).

## 1. Full test: 1571 tests, 1 failure (`--rerun`, 4 min 33 s, `01-*`)
- Count: m20's 1495 + 76 new tests, none removed. The 76: `science.equipment` 71 (ShapeRasterizer 25, ProcessUnit 19, ColumnStructure 7, LineGeometry 7, EquipmentRefresh 5, Paint 4, VesselPlacement 3, UnitText 1), `DrumReservoirTest` 2, `LevelHeadTest` 1, `PhysicalFluidTopologyTest` 1, `FluidCheckpointCodecTest.aVesselsLevelHeightRoundTrips` 1. `FluidCheckpointFormatTest` refuses 11 inside an existing test. No other count changed.
- **FAIL** `JunctionSeedingGateTest.aJunctionCarriedAcrossAReplacementHasItsBufferSizedOnWhatItHolds`. This is main's gate on bundled 145, and it holds E7 at 3867 (see item 4). With OD4 off it passes, and its output is byte-identical to m20 (`09-od4off/08-*`).

## 2. Printed output (`compare-junit-xml.txt`, 272 classes): 252 equal, 14 timing only, 6 physics
- Expected: `FluidPumpedFillLineTest` (rig RWCWR, OD4). `FluidCheckpointFormatTest` and `FluidCheckpointStoreTest`: the unit grows by 8 bytes per island (250 000 → 258 000 and 2 280 000 → 2 360 000 bytes), which is the level height in unit format 5.
- OD4, not announced:
  - `LiquidCo2LetDownTest` (main's): the 0.004 m slice's valve flow goes 0.28264 → 0.23068 kg/s. The valve's half now has the 4 mm bore of the let-down pipe.
  - `PumpCavitationTest`: the steady flow goes 8.0132 → 7.2201, with suction pressure equal to vapour pressure (3173.93 Pa) as before.
  - `JunctionSeedingGateTest`: the failure above.
- With OD4 off, all three print exactly the m20 lines.

## 3. G2 and GameTests
- G2 chain-100 exact: deviation 0 (`02-*`).
- GameTests: 31/31 (`03-*`).

## 4. Campaign (1000 seeds, 8 workers, `04-*`, `05-*`, `cmp-seeds.txt`)
| Distribution | Clean m20 → m21 | Newly held | Newly clean (was) | Held both, changed |
|---|---|---|---|---|
| cold | 955 → 955 | 285 F 28 t3421; 768 F 27 t7736; 866 F 22 t5677 | 35 (F), 757 (B), 779 (B) | 6 E6→F @2028; 694 F→E5 @961 |
| bundled | 990 → 988 | 145 E7 25 t3867; 889 E9 23 t2876 | none | 6 F t2055→1837; 116 E9 t1879→1277; 157 F→E4 @845 |
| defaults | 1000 → 1000 | none | none | none; counters identical (74 403 iterations, 26 391 builds) |
- **Alone** (`06-alone`): cold 6, 285, 768 and 866 still hold F (wall deadline). At m20, 285 took 0.7 s per seed; now it takes 10.3 s.
- **Clean in both, one worker, per seed** (`07-clean`, `counters-clean.txt`):
  - cold: 943 seeds, 30 move; Newton iterations −2.03 % (1 291 060 → 1 264 802). Largest moves: 999 33 097 → 3 465 and 698 21 172 → 32 104.
  - bundled: 988 seeds, 30 move; −0.19 %. Largest: 333 4 771 → 3 457 and 826 3 589 → 4 420.
- **OD4 off** (`09-od4off`), on the 38 cold and 35 bundled seeds that moved:
  - All 62 clean ones match m20's per-seed counters exactly.
  - 145, 889, 285, 768 and 866 are clean, and 757 and 779 hold B again.
  - The rest differ only by an F hold's timing (35, 694, bundled 6 and 157, as in m20's alone replays).

## Two smallest new worse worlds (not minimized)
- **Bundled 145** (25 blocks; the failing gate's world).
  - Physics: a water generator (374 kPa, 354 K) fills two 1 m³ tanks and drains through 50 mm lines to a void at 1 atm. Steady state: a constant throughflow of about 46 kg/s, with the 150 kPa pressure valve throttling a steady 12.6 kg/s.
  - At 0.9.2 the valve block's own 10 mm bore choked that branch. With OD4 both halves are 50 mm, and the set point lies between the valve's junctions (154.6 and 146.3 kPa).
  - The code: from 3767 every substep is refused with a "Phase/device active-set cycle" (40 of 58 refusals), and the island holds E7 at 3867.
- **Bundled 889** (23 blocks). At 2873 a pump (0.001 m³/s, 1000 kPa) joins a void, a 398 kPa water generator and a tank through 10 mm lines. Physics: the tank fills at no more than the pump's rate, with no singular point. The code: every step size gives a nonfinite trial residual (20 of 20), so the island holds E9 at 2876, three ticks after the pump joins. Which actuator's bore changed is not traced (actuator bores are not printed).

## Proposition for the owner
- (a) Keep OD4, since it is the plant's choice. Then treat the active-set cycle of a pressure valve whose set point lies inside its operating band, in the active-set stage. Treat 889's nonfinite trial too. Re-pin the gate on 145 after the fix, and announce the LetDown and PumpCavitation lines in a `MOVED_REFERENCES.md`.
- (b) Merge with OD4 off and bring it back as its own batch with a quick check. This gives a merge identical to 0.9.2 for worlds without drums, but `FluidPumpedFillLineTest` (RWCWR) was not run with OD4 off.
- Code guide: no change (test only). This lane did not touch `documentation/INDEX.md`.
