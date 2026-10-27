# Old flat document names

The flat `documentation/` folder was split into batch folders on 2026-09-23 ([DOCUMENTATION_ORGANIZATION_2026-09-23.md](DOCUMENTATION_ORGANIZATION_2026-09-23.md)). File names were kept, so a file cited by its old path `documentation/<FILE>` is found in the table below. Moved out of `INDEX.md` on 2026-10-26; the table is not extended for later batches.

## Tracked files that still cite the old flat paths

These tracked files refer to documents by their flat pre-2026-09-23 path; find the file by name in the lookup table below. Update the references when those files are next edited.

| Tracked file | Cites | Now at |
|---|---|---|
| `build.gradle` (`prepareMcpControlMod` error text) | `documentation/FLUID_NETWORK_PROGRESS.md` | `2026-09-15-fluid-network/` |
| `src/main/.../runtime/fluid/PhysicalFluidTopology.java`, `src/main/.../science/fluid/network/ConservativeTransport.java`, `src/test/.../runtime/fluid/FullTankSolidsEventTest.java` | `documentation/FULL_TANK_SOLIDS_EVENT.md` | `2026-09-18-solid-phase-fluid-system/` |
| `src/main/.../science/fluid/network/PassiveStepSolver.java`, `src/main/.../science/fluid/solver/PhaseLayout.java`, `src/test/.../runtime/fluid/FilterBlockLineIslandTest.java`, `FullTankSolidsEventTest.java` | `documentation/JUNCTION_PHANTOM_TRACE.md`, `DEAD_HEADED_LINE.md`, `ELEVATED_LINE_PROBE.md`, `PUMP_SHUTOFF_ACTIVE_SET.md`, `HYDRAULIC_ROW_SCALE.md`, `TANK_NODE_BLOCK.md` | `2026-09-18-solid-phase-fluid-system/` |
| `src/test/.../runtime/fluid/DeadHeadedLineIslandTest.java`, `ElevatedBlockLineIslandTest.java` | `documentation/DEAD_HEADED_LINE.md`, `documentation/ELEVATED_LINE_PROBE.md` | `2026-09-18-solid-phase-fluid-system/` |
| `src/main/.../science/column/v3/V3FreeWaterContinuation.java` | `documentation/V3_FREE_WATER_TRAYS_REVIEW.md` | `2026-09-08-v3-free-water-trays/` |
| `src/test/.../science/column/v3/V3ConvergenceClosureTest.java` | `documentation/V3_TRUNCATION_EVALUATION.md`, `documentation/V3_CLOSURE_AND_BAND_REVIEW.md` | `2026-09-07-v3-trace-scaling-and-truncation/` |
| `src/test/resources/v3-benchmarks/v3-cold-core-v1.json` (`executionReport`) | `documentation/V3_COLD_CORE_BENCHMARK_RESULTS.md` | `2026-09-05-v3-solver-core-review/` |
| `src/test/.../science/column/v3/thermo/V3Tjl19PropertyPackage.java` | `docs/tjl19-property-provenance.md` | `research/crude-regrouping/notes/tjl19-property-provenance.md` (moved there on 2026-09-17) |
| `MATERIALS.md` | `research/viscosity-tools/`, `research/crude-assays/appearances.json`, `research/crude-regrouping/notes/CRUDE_ASSAY_CONVERSION.md` | unchanged (research folders were not renamed) |

## File lookup

Every file directly inside a batch folder, and every sub-folder, with the batch that holds it. Old flat references `documentation/<FILE>` resolve through this table.

| File or folder | Batch folder | Note |
|---|---|---|
| `archive/` | [2026-08-31-v3-stage-trace-truncation](../2026-08-31-v3-stage-trace-truncation/) | folder, 23 entries |
| `BEND_SOLVER_FEASIBILITY.md` | [2026-09-09-solver-feasibility-studies](../2026-09-09-solver-feasibility-studies/) |  |
| `CHEMSEP_NATIVE_PSEUDOCOMPONENT_TRIAL.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `CHEMSEP_PETROLEUM_REPLACEMENT.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `CHEMSEP_TJL19_PROGRESSIVE_TEST.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `CHEMSEP_TJL19_ROOT_CAUSE.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `CHEMSEP_V3_TJL19_TRIAL.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `codex-branch/` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) | folder, 5 entries |
| `convergence-time-optimization-2026-09-09.md` | [2026-09-09-v3-convergence-time](../2026-09-09-v3-convergence-time/) |  |
| `CRUDE_REGROUPING_DATA_GENERATION_REVIEW.md` | [2026-09-17-crude-regrouping](../2026-09-17-crude-regrouping/) |  |
| `CRUDE_REGROUPING_PLAN_REVIEW.md` | [2026-09-17-crude-regrouping](../2026-09-17-crude-regrouping/) |  |
| `CUSTOM_EQUIPMENT_RENDERING_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `CUSTOM_EQUIPMENT_POC_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_CONSTRUCTOR_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `VESSEL_SUPPORTS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `PROCESS_EQUIPMENT_PHOTO_SURVEY.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `PROCESS_EQUIPMENT_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `PROCESS_EQUIPMENT_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `DEVICE_RENDERING_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `DRUM_OUTLET_SHADING_GUI_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `DRUM_SKIRT_CONNECTIONS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `COLUMN_AUXILIARIES_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `COLUMN_AUXILIARIES_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `STANDALONE_UNITS_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `STANDALONE_UNITS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_POLISH_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_POLISH_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_DETAILS_PHOTO_SURVEY.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_DETAILS_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_DETAILS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `EQUIPMENT_PERFORMANCE_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MACHINES_HEATERS_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MACHINES_HEATERS_SURVEY.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MACHINES_HEATERS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MACHINES_SCALE_RESEARCH.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `METALLIC_LOOK_RESEARCH.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MACHINES_HEATERS_SECOND_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MACHINES_HEATERS_THIRD_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `GHOST_PIPE_SPACING_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `HEATER_JOINTS_MACHINE_MOTION_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `HEATER_JOINTS_MACHINE_MOTION_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_SURVEY_STIRRED_ADSORPTION.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_SURVEY_HYDROPROCESSING.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_SURVEY_ROTATING.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_SURVEY_FCC_COKER.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_SURVEY_SOLID_FUEL.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `NEW_UNITS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `UNIT_ENGINE_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Abandoned 2026-10-18 (owner went back to the kit consolidation before any engine code) |
| `KIT_CONSOLIDATION_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `KIT_CONSOLIDATION_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `SAVE_REFRESH_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `FCC_PROPORTIONS_RESEARCH.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `FCC_PROPORTIONS_PLAN.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `FCC_PROPORTIONS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `MERGE_PREP_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) |  |
| `FINAL_TEST_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | NOT GREEN 2026-10-20 (OD4) |
| `FINAL_TEST_M23_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | GREEN 2026-10-20 |
| `OD4_HOLDS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Round 3 uncommitted 2026-10-20 (owner decision E5, amending D38 C: a shrinking hydrocarbon liquid vanishes on its volume share; D34 gate and 322 7/7 under seed perturbations; 145 354 889 322 583 743 clean; targeted 19 classes green) |
| `OD4_COLD285_GATE889_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Implemented on branch 2026-10-20 (`a6a305a1` on `claude/stoic-bell-ae6019`, not merged: a junction's own split is its own rows, owner option (a); 889 gate in `PumpCavitationTest`; 285 clean and STEADY; affected classes 148/149, the one failure also on base in the lane; relevant-part check 2026-10-20 found cold seed 170 (pass-1 flash seed); fixed as `48fb5678` (a flashing junction's proposal starts at the island's balance; quick check cold 193, bundled 196 against base 192, 195); final lane NOT GREEN 2026-10-20, `FINAL_LANE_48FB5678_REVIEW.md`; flash seed designed as `ca21bab8` (a junction where vapour first appears is seeded under the pressure its connections settle it at), final lane NOT GREEN by one world (729), `FLASH_SEED_REVIEW.md`) |
| `FINAL_LANE_48FB5678_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Concluded 2026-10-21 (merged as 0.10.1, main `144321e7`, not pushed). History: NOT GREEN 2026-10-20 (gates green: 1 572 tests, chain-100 exact, GameTests 31/31; cold campaign 960 against m23's 962, five worlds worse alone: 509, 729, 757 from `a6a305a1`, 592, 768 from `48fb5678`; owner decision open: do not merge, next item = the seed pressure of a junction flashing far below its bubble pressure; done as `ca21bab8`, `FLASH_SEED_REVIEW.md`) |
| `FLASH_SEED_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Implemented on branch 2026-10-20 (`ca21bab8` on `claude/stoic-bell-ae6019`, not merged: a junction where vapour first appears is seeded where its own connections settle it, connections restarted at their law, phase check from the lowest pressure up; 509, 592, 170, 757, 768, 285 clean alone; final lane NOT GREEN by one world: gates green (1 572 tests, chain-100 exact, GameTests 31/31), cold 971 against m23's 962, bundled 990, defaults 1 000; 729 B worse alone, from `a6a305a1`, not traced; owner decision open: trace 729 or merge-prep; 729 traced and fixed as `ff71d370`, `COLD729_REVIEW.md`) |
| `COLD729_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Implemented on branch 2026-10-20 (`ff71d370` on `claude/stoic-bell-ae6019`, not merged: a junction nothing reaches at a step's start flows is seeded as its own stock (arrival check skip since 0.7.0); 729 clean and STEADY as on 0.10.0; named worlds clean at `ca21bab8`'s counts; quick check held sets identical, cold Newton +1.7 %; targeted classes and chain-100 unchanged; second finding D61 labels (gate refusals, prototype A) proposed, not applied; owner decision open: final lane, D61 item) |
| `D61_LABELS_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Implemented on branch 2026-10-20 (`416f8601` + `2def39dc` on `claude/stoic-bell-ae6019`, not merged: D61 junction split reads an unlabelled Newton solid stream as the island's populations, as the reconstruction does; 729 0 gate refusals, ends FULL (round-off flows on a filter island never certify: certificate item proposed). Final lane GREEN against m23: gates green (2 failures of 416f8601 fixed in 2def39dc, targeted 86/86), cold 973 vs 962 (11 cleared, none newly held, no new C/F), bundled 990, defaults 1 000; vs ca21bab8, 994 held again as on m23 (vanishing liquid at 215.6 K; A melts only 6e-11 kg/s there). Owner decision open: keep A (recommended) or revert to ff71d370) |
| `COLD994_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Implemented on branch 2026-10-20 (`37797661` on `claude/stoic-bell-ae6019`, not merged: the arrival check keeps a junction seed only where its liquid and gas are its mixture's and it holds every water phase of it (`laidOutFor`; water keeps the superset for the D10 trace, form D1 dropped it and held bundled 157 E4). 994 clean (four junctions stated as the 215.5 K two-phase gas delivered to them over 3 to 14 g of warm gas). Quick check vs A: cold 194/193 (72 cleared), bundled 196/195 (6 cleared), none newly held; named 20/20 clean; targeted 148/149; chain-100 exact; two HoldClassWorldsTest lines moved (round 8). Next: the final lane) |
| `FINAL_LANE_37797661_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Concluded 2026-10-21 (merged as 0.10.1, main `144321e7`, not pushed). History: NOT GREEN 2026-10-20 by one world (gates green: 1 572 tests, chain-100 exact, GameTests 31/31; campaign vs m23: cold 972 vs 962 (13 cleared), bundled 989 vs 990, defaults 1 000; alone vs 2def39dc: 698, 183, 673, bundled 678 load only (geosim jobs held ~6 cores), 6 and 651 the same on both, 654 B from 37797661 (Matrix values must be finite after a placement at 2629). Proposed: trace 654 next, then merge prep) |
| `COLD654_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Implemented on branch 2026-10-20 (`bde9013c` on `claude/stoic-bell-ae6019`, not merged: the balance start closes a one-way line for the solve only off a converged balance. 654's unconverged balance left a run of three junctions at 99.0 kPa under a new void at 101.3 kPa, its only outlet for 0.10 kg/s of choked inflow; the closure read off that iterate shut the void line and every pass went to 13-15 MPa. Forms D3, E (seed is the mixture) rejected. 654, 994 clean alone; quick check vs 37797661: cold 194 = 194, bundled 197/196 (151 cleared), defaults 200; named all clean but cold Newton +33 % (729, 743; 743 ends FULL, not STEADY: its right generator-line closures came off unconverged balances too); targeted no line moved; chain-100 exact. Proposed: make the balance converge (owning stage), then the final lane) |
| `BALANCE_CONVERGENCE_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: In progress since 2026-10-21 (`a6b14b22` + G3 `aca82e8f` on `claude/stoic-bell-ae6019`, not merged; owner chose (a) 2026-10-21: G3 committed, 889 gate asserts the cavitation; 850 in `STILL850_REVIEW.md`). Two balance defects fixed: `BalanceNet.runFlow` ended its search on an open bracket (1e-14 of infinity), so a search started under its root stopped after one laminar step (a 1 cm line read 1 484 kg/s, law 1.536); slopes over 1e-7 of the pressure were secants across zero flow (now 1e-9). Unconverged balances 729 124 -> 7, 743 140 -> 37 (STEADY again). Quick vs bde9013c: cold 195/194, bundled 198/197, gate refusals 564 -> 101, nothing newly held; named all clean; chain-100 exact. Gates: starved-suction pump (round 1 forgot the water floor; G3 stored fixes it but flips the 889 gate's pump label at the head/suction corner) and still pipe 850 (25 refusals, untraced). 994 E6 (crystal family, not the balance). Proposed: commit G3, 889 gate asserts cavitation physics, trace 850, then final lane |
| `STILL850_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: In progress since 2026-10-21 (`e5357594` on `claude/stoic-bell-ae6019`, not merged; final lane in `FINAL_LANE_E5357594_REVIEW.md`). Still pipe 850 on G2: the interval-start rate solve (taut tank held as a port 1.36 kPa under its junction) failed 8 intervals in a row, so no pipe was still and a 2e-12 kg/s round-off flow read DEPOSITION (25 refusals). Fix: where the rate solve fails, still pipes from the last accepted step's carried flows (`PassiveIntervalSolver.carriedFlows`). Gate 0; targeted 148/149; quick vs a6b14b22: cold 195 = 195 (same held), bundled 199/198 (116 cleared), defaults 200; Newton +1.1 % / +1.0 %. Residual: rate solve fails at a taut vessel held as a port (design item) |
| `FINAL_LANE_E5357594_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Concluded 2026-10-21 (merged as 0.10.1, main `144321e7`, not pushed). History: In progress since 2026-10-21 (final lane on `e5357594`, `claude/stoic-bell-ae6019`, not merged or pushed): gates green (1 572 tests, chain-100 exact, GameTests 31/31); campaign cold 975 vs m23 962 / fl5 972, bundled 993 vs 990 / 989, defaults 1000. NOT GREEN by new worse worlds: 475 and 855 hold F alone (clean on m23 and fl5); 430 E6 from `a6b14b22` (its 8-block gate world too), 788 and 919 E6, bundled 889 E2. 244 and 907 better than m23, bundled 431 E5 as on m23. Proposed: trace 430 and bundled 889 before merge prep |
| `COLD430_889_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: In progress since 2026-10-21 (`8d69949d` on `claude/stoic-bell-ae6019`, not merged; A `d139fe93` reverted as `c7b883cb` by owner decision (b); final lanes `FINAL_LANE_8D69949D_REVIEW.md`, `FINAL_LANE_C7B883CB_REVIEW.md`). Cold 430: the mint's balance took 4e-8 of each 1e13-1e16 Pa Newton step on flat (choked or one-way) rows and never swept; A sweeps first where a step leaves half the hull, and the 8-block gate now asserts keep-up. Bundled 889 and the starved suction: the water floor read the vapour pressure at the junction's stated temperature, 22-60 mK under the throttled liquid's own, so 4e-5 to 1e-4 flashed and the pure-water two-phase junction made the suction row singular; D states the floor at the junction water's own enthalpy (`BalanceNet.waterFloor`), C ends the water bisection on the nearer side. Quick check: cold 193 (F 72 and 118, clean alone), bundled 199, defaults 200 |
| `FINAL_LANE_8D69949D_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Concluded 2026-10-21 (merged as 0.10.1, main `144321e7`, not pushed). History: In progress since 2026-10-21 (final lane on `8d69949d`, `claude/stoic-bell-ae6019`, not merged or pushed): gates green (1 572 tests, chain-100 exact, GameTests 31/31; PUMP_CAVITATION_ARRIVAL back on m23's value); campaign cold 976 vs m23 962 / fl6 975, bundled 993 vs 990 / 993, defaults 999 vs 1000. 430, 788, 994, 244 and bundled 889 cleared. NOT GREEN by three new worse worlds from A (d139fe93; one-seed attribution): cold 311 E5, cold 567 B, defaults 397 E6 (18 blocks, water pump); 475 F alone and 855 E6 still held. Owner chose (b) 2026-10-21: A dropped (c7b883cb), final lane in `FINAL_LANE_C7B883CB_REVIEW.md` |
| `FINAL_LANE_C7B883CB_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Concluded 2026-10-21 (merged as 0.10.1, main `144321e7`, not pushed). History: In progress since 2026-10-21 (final lane on `c7b883cb` = e5357594 + water floor C + D, A reverted; `claude/stoic-bell-ae6019`, not merged or pushed): gates green (1 572 tests, chain-100 exact, GameTests 31/31); campaign cold 975 / bundled 994 / defaults 1000 against fl6 975 / 993 / 1000 and m23 962 / 990 / 1000; nothing newly held against fl6 (bundled 889 cleared); against m23 the same four worse worlds as fl6 (430, 788 E6; 475, 855 F alone). Bundled Newton +14.7 % against fl6 (not attributed). Owner chose (a) 2026-10-21: merge prep in `MERGE_PREP_0_10_1_REVIEW.md` |
| `MERGE_PREP_0_10_1_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Implemented 2026-10-21 (main `144321e7`, 0.10.1, not pushed). History: Ready to merge since 2026-10-21 (`144321e7` on `claude/stoic-bell-ae6019`, not merged or pushed): CHANGELOG [0.10.1] - 2026-10-21, mod_version 0.10.1, code guide fluid-science + README (snapshot c7b883cb, to set at the merge), lane scripts stored in `tools/new-units/od4-holds/scripts-2026-10-21/`, nothing tracked detached; gates on 144321e7: 1 572 tests, chain-100 exact, GameTests 31/31; fast-forward of main 2e28a20a possible |
| `FINAL_TEST_M22_REVIEW.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | NOT GREEN 2026-10-20 (fix 2 of f983306b: two HoldClassWorldsTest gates, four worse worlds) |
| `MOVED_REFERENCES.md` | [2026-10-15-custom-equipment-rendering](../2026-10-15-custom-equipment-rendering/) | Through round 12 2026-10-21 (merged as 0.10.1, main `144321e7`). 2026-10-20, rounds 3 to 8 (E5: D34 gate line and a log4j notice; round 6: `ca21bab8` and `ff71d370` move nothing against `a6a305a1`; round 7: `416f8601` + `2def39dc` move nothing against `ca21bab8`; round 8: `37797661` moves two HoldClassWorldsTest lines, the 778 gate exactly at rest) |
| `DEAD_HEADED_LINE.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `deleted-branches-2026-09-09.txt` | [repository](./) |  |
| `DSTWU_SHORTCUT_FEASIBILITY.md` | [2026-09-09-solver-feasibility-studies](../2026-09-09-solver-feasibility-studies/) |  |
| `DWSIM_DATA_GENERATION.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `DWSIM_NATIVE_PSEUDOCOMPONENT_TRIAL.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `DWSIM_REBOILER_DUTY_INVESTIGATION.md` | [2026-09-09-dwsim-chemsep-trials](../2026-09-09-dwsim-chemsep-trials/) |  |
| `ELEVATED_LINE_PROBE.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `FLUID_ISLAND_REST_PLAN_REVIEW.md` | [2026-09-23-fluid-scheduling-rest](../2026-09-23-fluid-scheduling-rest/) |  |
| `FLUID_ISLAND_REST_PLAN.md` | [2026-09-23-fluid-scheduling-rest](../2026-09-23-fluid-scheduling-rest/) |  |
| `FLUID_LUNA_PHYSICAL_TEST_REVIEW.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_LUNA_TEST_REVIEW.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_NETWORK_ACCEPTANCE.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_NETWORK_BUILD.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_NETWORK_IMPLEMENTATION_REVIEW.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_NETWORK_PROGRESS.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_NETWORK_STRESS_TEST.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_POOL_MEASUREMENT.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_PROPERTY_COMPATIBILITY.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_SOL_TEST_REVIEW.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_SOLVER_OPTIMIZATION_PROGRESS.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_SOLVER_OPTIMIZATION_REVIEW.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FLUID_SOLVER_REGRESSION_RERECORD.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `FLUID_TEST_EXECUTION_ORDER.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `FULL_TANK_SOLIDS_EVENT.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `handoff-bounded-solve-path/` | [2026-09-18-tray-pressure-drop](../2026-09-18-tray-pressure-drop/) | folder, 2 entries |
| `HYBRID_SOLVER_CODE_GUIDE.md` | [reference](../reference/) | pointer to `code-guide/` since 2026-10-15 |
| `code-guide/` | [reference](../reference/) | progressive-disclosure code guide (README + seven chapters, `equilibrium-engine.md` added 2026-10-19) |
| `HYDRAULIC_ROW_SCALE.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `JUNCTION_PHANTOM_TRACE.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `newton-alternatives-2026-09-08.md` | [2026-09-08-v3-newton-alternatives](../2026-09-08-v3-newton-alternatives/) |  |
| `plan-2-experiment-results-2026-09-09.md` | [2026-09-08-v3-newton-alternatives](../2026-09-08-v3-newton-alternatives/) |  |
| `PUMP_SHUTOFF_ACTIVE_SET.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `REACTION_ENTHALPY_KINETICS_TOOL_RESEARCH.md` | [2026-09-01-reaction-thermochemistry-tool](../2026-09-01-reaction-thermochemistry-tool/) |  |
| `REACTION_TOOL_IMPLEMENTATION_PLAN.md` | [2026-09-01-reaction-thermochemistry-tool](../2026-09-01-reaction-thermochemistry-tool/) |  |
| `screenshots/` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) | folder, 32 entries |
| `screenshots/` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) | folder, 278 entries |
| `SIMULATION_ENGINE_AND_FLUID_NETWORK_DESIGN_REVIEW.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `SIMULATION_ENGINE_AND_FLUID_NETWORK_DESIGN.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `SIMULATION_ENGINE_AND_FLUID_NETWORK_WORK_ORDER.md` | [2026-09-15-fluid-network](../2026-09-15-fluid-network/) |  |
| `SOLID_PHASE_FILTER_ISLAND_DIAGNOSIS.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `SOLID_PHASE_FIXES_2_PROGRESS.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `SOLID_PHASE_FIXES_PROGRESS.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `SOLID_PHASE_FLUID_SYSTEM_IMPLEMENTATION_REVIEW.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `SOLID_PHASE_FLUID_SYSTEM_PLAN_REVIEW.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `SOLID_PHASE_GUI_PASS.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `SOLID_PHASE_STALL_DIAGNOSIS.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `solid-phase-fluid-system-plan.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `sources/` | [2026-09-01-v3-holland-benchmark](../2026-09-01-v3-holland-benchmark/) | folder, 1 entries |
| `sources/` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) | folder, 1 entries |
| `TANK_NODE_BLOCK.md` | [2026-09-18-solid-phase-fluid-system](../2026-09-18-solid-phase-fluid-system/) |  |
| `timing-precision-analysis-2026-09-09.md` | [2026-09-08-v3-newton-alternatives](../2026-09-08-v3-newton-alternatives/) |  |
| `TRAY_PRESSURE_IMPLEMENTATION_NOTES.md` | [2026-09-18-tray-pressure-drop](../2026-09-18-tray-pressure-drop/) |  |
| `TRAY_PRESSURE_METHOD_REVIEW.md` | [2026-09-18-tray-pressure-drop](../2026-09-18-tray-pressure-drop/) |  |
| `v1-calculator-removal-plan.md` | [2026-09-09-v1-calculator-removal](../2026-09-09-v1-calculator-removal/) |  |
| `V3_55KPA_WALL_ACTION_PLAN.md` | [2026-08-31-v3-stage-trace-truncation](../2026-08-31-v3-stage-trace-truncation/) |  |
| `V3_A0_WET_LANE_REVIEW.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_CDU_CONVERGENCE_RISK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_CDU17_RETIREMENT_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_CLOSURE_AND_BAND_REVIEW.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_COLD_CORE_BENCHMARK_PLAN.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_COLD_CORE_BENCHMARK_RESULTS.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_COLD_DOE_A0_RERUN.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_COLD_DOE_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_COLD_DOE_FAILURE_ANALYSIS.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_CONDENSER_ENERGY_AUDIT_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_CONVERGENCE_ANALYSIS.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_CONVERGENCE_TIME_OPTIMIZATION_PROPOSALS.md` | [2026-09-09-v3-convergence-time](../2026-09-09-v3-convergence-time/) |  |
| `V3_CORE_FIXES_RESULTS.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_F06_FAILED_TRAY_PROFILE.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_FREE_WATER_CONTINUATION_REVIEW.md` | [2026-09-08-v3-free-water-trays](../2026-09-08-v3-free-water-trays/) |  |
| `V3_FREE_WATER_TRAYS_PLAN.md` | [2026-09-08-v3-free-water-trays](../2026-09-08-v3-free-water-trays/) |  |
| `V3_FREE_WATER_TRAYS_REVIEW.md` | [2026-09-08-v3-free-water-trays](../2026-09-08-v3-free-water-trays/) |  |
| `V3_FULL_CDU_PLAN.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_FULL_CONVERGENCE_PLAN.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_FUNDAMENTAL_SOLVER_REDESIGN.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_HANDOFF_2026-09-08_REVIEW_PROBES.log` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_HANDOFF_2026-09-08_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_HANDOFF_2026-09-08.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_HANDOFF_FIXES_VERIFICATION_PROBES.log` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_HANDOFF_FIXES_VERIFICATION_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_HANDOFF_REVIEW_FIXES.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_HOLLAND_BENCHMARK_CASE.md` | [2026-09-01-v3-holland-benchmark](../2026-09-01-v3-holland-benchmark/) |  |
| `V3_LITERATURE_CDU_HANDOFF_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_LITERATURE_CDU_HANDOFF.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_LITERATURE_CDU_PA_PLAN.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_LITERATURE_TOP_TEMPERATURE_CHECK.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_MAIN_SOLVER_REVIEW.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_NEURAL_INITIALIZER_CONTRACT.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V3_NEURAL_INITIALIZER_MODEL_SURVEY.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V3_OPTIMIZED_SOLVER_REGRESSION_FIX_REVIEW.md` | [2026-09-09-v3-convergence-time](../2026-09-09-v3-convergence-time/) |  |
| `V3_OPTIMIZED_SOLVER_ROBUSTNESS_REVIEW.md` | [2026-09-09-v3-convergence-time](../2026-09-09-v3-convergence-time/) |  |
| `V3_PHASE_SPECIFIC_TRUNCATION_PLAN.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_PHASE_SPECIFIC_TRUNCATION_REVIEW.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_PUMPAROUND_GUI_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_PUMPAROUND_HEAT_EXCHANGER_PLAN.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_PUMPAROUND_STEAM_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_PUMPAROUND_TRANSPORT_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_RAMP_PREDICTOR_REVIEW.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_REDESIGN_ALTERNATIVES_NOTES.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_REDESIGN_FORMULATION_NOTES.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_REDESIGN_NUMERICS_NOTES.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_REFINERY_INITIALIZER_REQUIREMENTS.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V3_SIDE_DRAW_LITERATURE_CASE.md` | [2026-08-31-v3-side-draws](../2026-08-31-v3-side-draws/) |  |
| `V3_SIDE_DRAW_PLAN.md` | [2026-08-31-v3-side-draws](../2026-08-31-v3-side-draws/) |  |
| `V3_SIDE_DRAW_RESULTS.md` | [2026-08-31-v3-side-draws](../2026-08-31-v3-side-draws/) |  |
| `V3_SIDE_DRAW_REVIEW.md` | [2026-08-31-v3-side-draws](../2026-08-31-v3-side-draws/) |  |
| `V3_SOLVER_DESIGN_REVIEW.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_STAGE_TRACE_TRUNCATION_PLAN.md` | [2026-08-31-v3-stage-trace-truncation](../2026-08-31-v3-stage-trace-truncation/) |  |
| `V3_STEAM_STRIPPING_PLAN.md` | [2026-09-01-v3-steam-stripping](../2026-09-01-v3-steam-stripping/) |  |
| `V3_STEAM_STRIPPING_REVIEW.md` | [2026-09-01-v3-steam-stripping](../2026-09-01-v3-steam-stripping/) |  |
| `V3_THESIS_PUMPAROUND_ARRANGEMENT.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V3_TJL19_WET_DRAW_STALL_ROOT_CAUSE.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_TJL19_WET_DRAW_TRAY_BALANCES.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_TRACE_BALANCE_REMEDIES_REVIEW.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_TRACE_PAIR_ANCHOR_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_TRUNCATION_EVALUATION.md` | [2026-09-07-v3-trace-scaling-and-truncation](../2026-09-07-v3-trace-scaling-and-truncation/) |  |
| `V3_TRUNCATION_FAILURE_INVESTIGATION.md` | [2026-09-05-v3-solver-core-review](../2026-09-05-v3-solver-core-review/) |  |
| `V3_TRUNCATION_OPTIMIZATION.md` | [2026-08-31-v3-side-draws](../2026-08-31-v3-side-draws/) |  |
| `V3_VDU_CASE_A_PLAN_REVIEW.md` | [2026-08-31-v3-vdu](../2026-08-31-v3-vdu/) |  |
| `V3_VDU_CASE_A_PLAN.md` | [2026-08-31-v3-vdu](../2026-08-31-v3-vdu/) |  |
| `V3_VDU_LITERATURE_AND_THERMO_DATA.md` | [2026-08-31-v3-vdu](../2026-08-31-v3-vdu/) |  |
| `V3_W1_DRY_TRAY_ACTIVATION_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W1_DRY_TRAY_FOUNDATION.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W1_W10_DRY_STATE_PROBE.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W1_W10_DRY_TARGET_RERESOLVE.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W1_W10_LIVE_HANDOFF_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W1_W5B_DRY_TRAY_RERESOLVE.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W2_W3_CONDITIONING_REVIEW.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W2_W3_DIRECT_OFFENDER_AUTOPSY.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W2_W3_NULL_DIRECTION_AUTOPSY.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W2_W3_RANK_ONE_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W3_HARD_COMMON_MODE_CONSTRAINT_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W3_TERMINAL_RESCUE_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_W4_STARVATION_SCREEN_BENCHMARK.md` | [2026-09-01-v3-full-cdu-draw-wall](../2026-09-01-v3-full-cdu-draw-wall/) |  |
| `V3_WALL_AND_40MW_BALANCE_ANALYSIS.md` | [2026-09-06-v3-literature-cdu-pumparounds](../2026-09-06-v3-literature-cdu-pumparounds/) |  |
| `V4_BENCHMARK_POPULATION_REVIEW.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V4_HYBRID_INITIALIZER_REVIEW.md` | [2026-09-17-crude-regrouping](../2026-09-17-crude-regrouping/) |  |
| `V4_IMPLEMENTATION_RESULTS.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V4_LNN_ONLY_GAP_ANALYSIS.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V4_TRANSFORMER_INITIALIZER_REVIEW.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
| `V4_TRANSFORMER_TRAINING_GUIDE.md` | [2026-09-10-v4-neural-initializer](../2026-09-10-v4-neural-initializer/) |  |
