# Post-0.9.0 follow-ups: plan (t51, Opus, 2026-10-19)

Branch `claude/post-0.9.0` from `5b49f940` (0.9.0). One commit per item; gates from the t48 pair-minimal worlds, rule-off patches in `tools/post-release-followups/`.

## 1. Bundled 145: one buffer sizing for a minted and a carried junction
- **Physics.** A junction is a fitting of fixed volume; its mixing lag does not depend on what fills it. In 145, 21.5 kg/s of water pass junction 15. Sized on water, its buffer is 0.05 s x 190 kg/s cap = 9.5 kg, a 0.44 s lag at that flow. The carry kept the 0.011 kg buffer sized on the N2 placeholder: a 0.5 ms lag, a stiff row, and E9 at 3767.
- **Form.** `PassiveNetwork.withJunctionStates(graph, from, model)` carries each junction's committed state and sizes its buffer on that state by the mint's own rule (`sizeJunctionHoldups`). There is then one sizing rule, applied wherever a junction gets its state. The carried state and endpoint are unchanged, so 483's carry stays exact. `IslandCoordinator` passes its model. The javadocs of the sizing ("a topology edit ... mints again") and of the carry are restated. About 15 lines in two files.
- **Gate.** `JunctionSeedingGateTest`: 145's 16 blocks run to 2440 + 4000 ticks and keep up. Rule-off proof: `rule-off-carry-sizing.patch` (the carry keeps the old buffer) holds.
- **Risk.** t48's export probe of the same rule ran 145 clean, but it moved the S1 counters: cold +1.66 % against m15 where m16 had +0.91 %, bundled +0.53 % against +1.07 %. Cold may therefore rise against `5b49f940` and miss the cost gate; that is judged on counters. D46: the resized buffer is unbooked mass at a carry, as it was at the m15 re-mint. D46 itself is the owner's decision; no `D46_MINT_MASS_REVIEW.md` exists yet.
- **Option (not built).** Size every buffer on its current state at each interval start. This also covers an island minted on the placeholder and never replaced (2114 to 2152 on every tree). It moves every junction's buffer each interval and so moves broad references.

## 2. Cold 850: a still pipe deposits nothing
- **Physics.** A pipe at rest carries no solids, so it neither deposits nor carries them. In 850 the 20 mm line from junction 6 to the tank carries 1.3e-23 m/s, roundoff. The tank holds 2.9 kg of CO2 crystal. The sign of the roundoff picked the tank as the donor, so the line read DEPOSITION (the suspension velocity is 1.275 m/s). The line then produced 40 rejections per interval and, at 9079, an E5.
- **Form.** `SolidEventIntegrator.failed` reads a flow at or below the connection's D42 resolution flow (`NetworkReduction.resolutionFlow`; 0.017 kg/s for 1 m of 50 mm water pipe) as no flow. Such a pipe raises no transition of any kind (deposition, bore, slush, immobile liquid, population). It keeps its open or closed state. A clogged filter is still checked, because it does not depend on flow. The resolution is computed only for a connection that would otherwise fail, so the cost stays off the common path. This absorbs the `flow!=0` special case of the population test. The D42 javadocs are restated: the resolution now serves the rest test and the solid transition test. About 12 lines.
- **Gate.** 850's 20 blocks run past 9179 and keep up. Rule-off: `rule-off-still-pipe.patch`.
- **Risk.** It widens D42's resolution beyond the rest test, which the owner had reserved it for (the brief asks for this). A line starting from rest now closes when its flow crosses the resolution, not at zero. References with solids can move.

## 3. Stale comments (comment only)
- `FluidCheckpointCodec` `:25` changes to `FluidFormat = 11` (`VERSION`); `:42` changes to "format 10 and older are refused".
- `PassiveIntervalSolver:174`: the cold start is the rate solve started from the balance, and the balance itself when that rate solve fails.
- `FluidTpEquilibrium.forPackage`: a wet request is NOT_IMPLEMENTED, crystals or not. The contract refuses it first, as WATER_CHEMISTRY_NOT_MODELLED, only when a species that needs aqueous chemistry is present.
- `ThermoIdentity`: the column's neural sidecars key on `MaterialCatalog.physicsFingerprint` (the axis's science without the water revision and the ice), not on `revision()`.

## 4. Reader findings (MATERIALS_FACTS)
- **(a) Built.** The example becomes the bundled `tjl20_methane` record, with only its density (357.0) and `source` changed, so its `fluid_domain` is 10 MPa. `MaterialPresetsTest` pins that the example differs from the bundled record only in those two fields, so the example cannot drift again. MATERIALS.md drops the sentence "older copy, 2 MPa". The hold on a running world stays: it is the reload guard's designed answer to a property change.
- **(b) Option, not built.** Checking `ice` at catalog validation would make `/reload` refuse such a datapack, which the player sees. Today the catalog publishes, and the reload guard's model build fails and holds the network (`HELD: property data changed`).
- **(c) Built (clear defect).** `fluidThermoFingerprint` drops the calibration point of a component whose record has `volume_translation`, but only a spine package reads that translation (`SpinePackageData`). A non-spine package's `HydrocarbonModel` uses the calibration point, which is then not hashed. The fix: drop the point only where the package is a spine package. No bundled revision moves: only the `pilot_*` records carry the translation, and only the spine package `pilot_cryogenic` uses them.
- **(d) Built.** `SolidMaterialCatalog.parse` reads the fields through `JsonFields` and prefixes the file, so the error names the file and the field (for example `.../solids/x.json: revision: required field`). The ill-typed case gets the same treatment. One test.

## Evidence
The javac-lane suites (science-fluid-thermo, science-fluid-network, runtime-fluid, plus the material and codec tests). Then the 200-seed quick check on three distributions against `5b49f940` with 4 workers, judged on counters, together with the named runs and chain-100. `MOVED_REFERENCES.md` records what moves.

## Outcome (built `a313400b`, `50fd053f`, `afddb22c`, `6c00db3e`; D46 gate `73fbe48c`; numbers in `MOVED_REFERENCES.md`)
- Item 2's transcription of 850 keeps up even without the rule, because the hold rests on the roundoff history. Its gate therefore reads the mechanism: 0 solid transitions at rest with the rule, 1 146 with the rule off.

## Owner decisions of 2026-10-19 on the options, and what the build found
- **Option 3 (D33 amended: a still pipe keeps its closure). Built `e5042683`, reverted `c6b7d07b`. Back to the owner.** At cold 15's interval start the port graph has no rate solve, with the pipe open or closed, so nothing shows whether the bed would clear. Form A keeps the closure when no rate solve shows a flow. It fixes cold 15 (its gate is cold 15's pair-minimal world, 11 blocks; with the rule off it holds E5 at 10939). But it keeps `PumpCavitationTest`'s LNG suction line closed, where the pump would draw 2.5 m/s through 10 mm and clear the bed. Form B reads the drive across the closed pipe from a rate solve with it closed (`option3-formB.patch`, applied on `e5042683`). The pump test passes, but cold 15 still holds. Choosing needs an owner assumption, for example "a mover on the island clears what it draws through".
- **Option 1 (every buffer sized at each interval start). Built `fbb57602`, reverted `42915e01`: it missed the cost gate.** On the seeds clean in both: cold Newton iterations +25.1 % and Jacobian builds +76.0 %; bundled +21.0 % / +49.4 %. Cold 15 holds at 2439 on the three-phase limit. Bundled 188: unbooked water reaching the tank falls from 1.286 to 0.219 kg, but the resizes delete 1.606 kg of buffer mass, unbooked, so total mass is no longer exact. `a313400b`'s one sizing stays.
- **Option 2 (4b). Built `340e43da`.** A network water model without `ice` is refused at loading, with its file and field named, so `/reload` refuses such data.
- **Option 4: not built** (owner).
- **Round 3** (t53's m18 not green):
  - **Fixture `987db979`:** the CDU17 test fixture's water record carries its ice. The owner's words: "ice is water as solid".
  - **Still pipe `235d69bb`:** still is roundoff (within the Newton's tolerance), decided once at the interval start. It replaces `50fd053f`'s in-step test on D42's resolution flow. Gates: 850, and 829 (8 blocks).
  - **Still held:** cold 778 and 743.
