# GameTests (src/fluidGameTest) - test review part

Tree: `main` 138f4993 (0.11.0). Criteria: `../CRITERIA.md`. Scope: 18 Java files, 3 structures and the mods.toml. That is 31 GameTests in
`icheme_fluid_test` (G5, `runFluidGameTestServer`), 2 in `icheme_column_test` (`runColumnGameTestServer`) and 1 in
`icheme_fluid_benchmark` (`fluidServerBenchmark`). Method rows are authoritative. The class row sums them up.
I compared each test with the unit tests under `src/test/java/com/wormzjl/icheme/runtime/` and `science/fluid/network/`.

## FluidHarnessGameTests - TRIM (5 of 7 archived)
| Class / GameTest | Verdict | Code | Reason (max 20 words; for A2 name the kept test) |
|---|---|---|---|
| registeredPipesAndReservoirsKeepSimulatingAcrossUnloadedChunks | KEEP | K6 | Only guard: simulation runs with every chunk unloaded, never loads a chunk, and identity survives a reload. |
| gameplayPlacementTransfersFluidAndRemovalClosesThePersistentOwnershipLedger | ARCHIVE | A2 | Nitrogen ledger is 0 since D54. Kept: WorldTopologyLedgerTest.constructionAndDestructionCountOnceAndNeverReuseTheOldIdentity; world flow by registeredPipes... |
| savedDataKeepsADepletedNitrogenChargeAndAtomicWritesKeepThePreviousCheckpoint | ARCHIVE | A2 | FluidCheckpointStoreTest.aHalfWrittenDamagedOrMissingPack... and aCrashBetweenAPack...; IO-worker save: FluidCheckpointGameTests.aSavedRestingTank... |
| worldRuntimeCatchesUpNineIntervalsThroughSharedWorkers | ARCHIVE | A2 | IslandCoordinatorTest.nonblockingBarrierCommitsACompleteRoundAndThenCatchesUpFairly; the runtime's server wiring is exercised by every kept world GameTest. |
| sparseSolverLoadsInServer | KEEP | K6 | Solve is A2 (SparseLuSolverTest). Kept as only carrier of documented -PfluidGameTestDeliberateFailure proof that G5 reports failure. |
| fluidSnapshotRunsThroughTheSharedServiceAndReturnsOnServerThread | ARCHIVE | A2 | FluidCompletionBudgetGameTests.sixtyFifthTerminal... (same submit path, server-thread completion, candidate present). |
| shortJobsResumeBetweenTicksWithoutExceedingTheDrainBudget | ARCHIVE | A2 | FluidCompletionBudgetGameTests.sixtyFifthTerminalWaitsForTheNextTickWithoutAZeroBudgetDrain (between-tick wakeups, cap 64, wall bound). |

## FluidCheckpointGameTests - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| aSavedRestingTankReloadsCertifiedWithItsStatusLineAndNoSolve | KEEP | K2 | Only round-trip through Minecraft SavedData.save(File), NeoForge IO worker and NBT DataVersion wrapper; certificate restored without solve. |

## FluidCompletionBudgetGameTests - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| sixtyFifthTerminalWaitsForTheNextTickWithoutAZeroBudgetDrain | KEEP | K3 | Tick hook drains at most 64 completions per tick on the real server mailbox. No unit equivalent (CompletionWakeupTest covers wakeups only). |
| anExhaustedBudgetOwesOneContinuationThatNeverReentersItsTick | KEEP | K3 | Only guard that a stranded completion is routed later without other activity. Its counter pins are A3-like. |

## FluidPacketGameTests - KEEP (1 of 4 archived; TRIM threshold not met)
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| generatorMenuAcceptsAmbientCrudeWithoutChangingTemperature | ARCHIVE | A2 | FluidDeviceSpecDomainTest.aGeneratorIsCheckedAgainstTheEnvelopeAndControlsAreOnlyStructural; handler acceptance by realServerRejects... |
| createAndVanillaMovementRejectEveryFluidBlock | KEEP | K6 | Only guard: a Create contraption or piston moving a fluid block would desync the registry and lose material. |
| unloadedMenuDoesNotReloadItsChunkAndReplacementInvalidatesIdentity | KEEP | K6 | Only guard: menu validity loads no chunk and adds no ticket. An old menu never binds a replacement block. |
| realServerRejectsInvalidStaleDistantReadOnlyAndFloodedEdits | KEEP | K2 | Only guard of the real edit handler's admission: menu, position, identity, probe, distance, mayBuild, flood, stale revision. |

## FluidPhasePortGameTests - TRIM (2 of 3 archived)
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| aTanksTopSideAndBottomOutletsEachCarryTheirPhase | KEEP | K4 | Fixed-tick read (A141). Its print is compared bitwise per lane. Only check that block faces map to ports in the world. |
| aPumpOnAGasTankIsRefusedWithItsReasonOnItsStatusLine | ARCHIVE | A2 | InletPhaseTest.thePumpsThresholdsAndReason and refusedPumpsCarryNothingAndTheIslandsWaterLineIntegratesBitwise. |
| aCompressorMovesNitrogenToItsRatioLimitAndCloses | ARCHIVE | A2 | CompressorTest.aTransferRunsOnTargetThenOnItsRatioLimitThenClosesAtShutoffAtEitherCadence (+ theDischargeOfARatioThree...Warmer). |

## FluidPlacementGameTests - ARCHIVE
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| fiveThousandDevicesPlacedAtOnceApplyInBatchesAndOneMoreCostsLittle | ARCHIVE | A4 | Wall-time asserts (<30 s, <10 ms). Kept: PhysicalRegistryTest.placementCostIsFlatInWorldSize (counter compiledDevices) and aTicksEventsApplyOnceInOrderAsOneBatch. |

## FluidPresentationGameTests - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| aMenuOpeningReplaysCachedDataThenFollowsItsIslandsBucket | KEEP | K3 | Engine-owned presentation: real FluidDeviceMenu open and close subscribe and unsubscribe; cached replay; bucket ticks. |
| aQueuedEditIsAcknowledgedOnTheFollowingBucket | KEEP | K3 | Only guard that the real packet handler answers nothing. Queued, Applied and Refused arrive on buckets, tick-exact. |
| aCertifiedIslandsStatusLineArrivesWithItsBucket | KEEP | K3 | Only check that a certified island's delivered view is materialised to its bucket tick (viewCommittedTick). Kept as doubtful. |
| identityBindingOnAChunkLoadCarriesNoViewData | KEEP | K3 | Only guard: block-entity load forces no update. Update tag is identity and geometry only; the view waits for the bucket. |

## FluidPropertyReloadGameTests - KEEP (1 of 2 archived)
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| betweenTickRoutingHoldsChangedScienceAndRestoringItResumesConservedState | KEEP | K6 | Only test of the server's between-tick reload routing: held view, frozen inventory, decode refused, conserved recovery. |
| aCertifiedIslandIsFrozenByAHoldAndRequalifiesAfterResume | ARCHIVE | A2 | IslandCertificateTest.aPropertyHoldFreezesACertifiedIslandAndResumeRequalifiesOverTheConfirmCount. |

## FluidPumpedFillGameTests - ARCHIVE
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| aPlacedPumpedThreeTankChainFills | ARCHIVE | A2 | FluidPumpedFillLineTest.aPumpedThreeTankChainFillsToThePumpsShutoffAndCertifies (same placement defaults, real coordinator, holds, budget). |

## FluidPumpStartupGameTests - ARCHIVE
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| defaultWaterPumpStartsIntoNitrogenReservoir | ARCHIVE | A2 | PumpJunctionStartupTest.waterAndAmbientCrudeStart...; FluidPumpedFillLineTest.everyPumpedChainCommitsItsFirstTickFromAColdSolver; pump block in world: SolidPhaseGameTests. |
| heatedCrudePumpStartsIntoNitrogenReservoir | ARCHIVE | A2 | PumpJunctionStartupTest.waterAndAmbientCrudeStartThroughAZeroStoragePumpInletWithoutInventingUpstreamNitrogen. The unit case is ambient, not 350 K and 1 MPa. |

## FluidTopologyGameTests - ARCHIVE
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| anUnresolvedIslandHorizonDoesNotBlockUnrelatedPhysicalEdits | ARCHIVE | A2 | WorldTopologyLedgerTest.independentLaterEventCanCommitWithoutAdvancingTheBlockedEventsTimestamp; PhysicalRegistryTest.aHeldEventKeepsItsTickAndOrdersTheEventsThatDependOnIt. |

## MaterialReloadGameTests - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| columnPresetsLoadWithRevisionChecks | KEEP | K6 | Only test of ColumnCalculatorV3BlockEntity.tryLoadPreset: revision bump, result cleared, stale revision refused. |
| materialReloadIsAtomic | KEEP | K2 | Only test of the real datapack /reload: valid override published, invalid one refused, previous snapshot kept, restore. |

## SolidPhaseGameTests - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| filterCaptureRecoveryPersistenceAndBreakDropConserveOwnedSolids | KEEP | K1 | Only guard of solids conservation into player inventory and item drops; recovery not duplicated; save round-trip. |

## ColumnRegroupingGameTests (namespace icheme_column_test) - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| allCrudePresetsCalculateThroughTheMinecraftAdmissionAndCompletionPath | KEEP | K6 | Only server test of V3 column admission, completion and block publication. Not in lane.sh; last run at 0.8.0. |
| unreadableColumnDataIsRefusedAndReplacedByAFreshInput | KEEP | K3 | Only guard of the no-compatibility rule for column NBT: refused, never interpreted, saved fresh. No unit equivalent. |

## FluidServerBenchmark (namespace icheme_fluid_benchmark) - KEEP
| Class / GameTest | Verdict | Code | Reason |
|---|---|---|---|
| pacedServerQualification | KEEP | task | Documented owner task fluidServerBenchmark (verification.md "Cost claim", 60+60 s rule). Not in the unit suite, so A4 does not apply. |

## Helpers and resources
| Item | Verdict | Code | Reason |
|---|---|---|---|
| FluidTestMod.java | KEEP | - | Mod entry for all three namespaces. Registers the benchmark listeners. |
| TankCharge.java | KEEP | - | Used by the kept FluidPhasePortGameTests outlets test. |
| FluidContentionProbe.java | KEEP | - | Used by the benchmark's `contention` profile. |
| resources/data/icheme_fluid_test/structure/empty.nbt | KEEP | - | Template of every kept fluid GameTest. |
| resources/data/icheme_column_test/structure/empty.nbt | KEEP | - | Template of the kept column GameTests. |
| resources/data/icheme_fluid_benchmark/structure/empty.nbt | KEEP | - | Template of the benchmark. |
| resources/META-INF/neoforge.mods.toml | KEEP | - | Declares the icheme_fluid_test mod. |

## Summary
- GameTest methods (34): KEEP 20, ARCHIVE 14, TRIM 0. G5 namespace: 31 tests become 17 (KEEP 17, ARCHIVE 14). Column 2/2 KEEP, benchmark 1/1 KEEP.
- Classes (14 test holders): KEEP 8, TRIM 2 (FluidHarnessGameTests, FluidPhasePortGameTests), ARCHIVE 4 (Placement, PumpedFill, PumpStartup, Topology). FluidPacket and FluidPropertyReload each archive one method below the TRIM threshold.
- Helpers and resources: all 7 KEEP.
- Unsure: sparseSolverLoadsInServer (kept only for the deliberate-failure flag); aCertifiedIslandsStatusLineArrivesWithItsBucket; heatedCrudePump (350 K case has no unit equivalent); the pump-refused archive (device-named reason text).
- Follow-ups if applied: "31 GameTests" in verification.md, fluid-runtime.md and lane.sh expectations; ColumnRegrouping is in no lane.
