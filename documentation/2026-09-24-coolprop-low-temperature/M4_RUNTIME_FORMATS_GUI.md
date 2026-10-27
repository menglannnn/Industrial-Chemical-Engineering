# M4 - Runtime, formats, presentation, GUI, consumers

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, 2026-10-16, on `76e87740` (M3). Plan `MAIN_ADAPTATION_PLAN.md` M4. Tools and logs: `tools/unified-thermo-port-m4/` (README, `logs/m4-logs.tar.gz`). No solver code touched; CHANGELOG, `mod_version` and the code guide are left to the merge.

## Commits
- `5e72eb2a` column: the V3 column and the neural binding refuse a crystal package typed (`CrystalPhasesUnavailable`), and the column a spine package (`IdealGasFitUnavailable`); `CrystalPackageConsumersTest` ported.
- `cbf2579f` fluid: a saved solid population names its record, a crystal of the package or a custom solid (format 9 reader); a crystal id may not also name a custom record.
- `61693616` fluid: the topology ledger books a crystal as its component's moles with its true energy (u0 m added back).
- `ea1c23d9` test: a tank resting with its dry ice certifies, replays and restores the crystal exactly.
- `57f20ea9` test: a reload that changes a crystal record holds the world.
- `177141ac` network: menu protocol `fluid-10`; MenuData refuses a crystal off the fluid axis.
- `61a1eba0` gui: Solids tab one row per solid at its mean size (arrivals merged as the next refresh merges them); a crystal reads "Carbon dioxide (solid)", id and hint on hover; language entries.

## Formats
Checkpoint 9 and unit format 2 (constants from M2, `0ab70bc8`). A format 9 solid is refused when its id names no crystal of the island's package and no custom record, or its revision, density or heat capacity differ (fresh-world message). Formats 1, 2, 3, 7, 8 refused (`FluidCheckpointFormatTest`); its 1000-island byte counts are unchanged (257000 unit, 142935 core). Protocol `fluid-10`.

## Tests
- M4 run: `fluidRuntimeTest` 62 classes, 309 tests; `CrystalPackageConsumersTest` 4 and `SpineNetworkPathTest` 7; 0 failures.
- Rule-off proofs (javac lane, the one test alone, the edit on an exported copy only):
  - codec, record lookup replaced by the saved record: "Expected IllegalArgumentException to be thrown, but nothing was thrown";
  - ledger, crystal booked as a custom solid: "CarbonDioxide: what was placed as gas leaves as gas and dry ice, expected 8.1208 but was 2.5288";
  - certificate, node-solids evidence off: "an interval that changed the crystal does not qualify, expected null but was IslandCertificate";
  - reload guard, crystal science keyed by id: "a changed crystal Gibbs function, expected HELD: property data changed ... but was no hold".
- Full `test`: 266 classes, 1450 tests, 0 failures, 0 errors (M3 log 26: 265, 1441; plus `CrystalPackageConsumersTest` and 5 new cases). Printed lines 8581 to 8583 and no M3 line changed: the two added lines are the new ledger and certificate tests' prints (`printed-moves.js` counts the shifted line after one as moved).

## GUI check (C4 re-read for D23; screenshots `gui/`, notes `gui/M4_GUI_CHECK_NOTES.md`)
Expectation: cold N2 (150, then 120 K) and CO2 gas (220 K), both at 300 kPa, mix in a 1 m3 tank vented to 101 kPa. CO2 at about 78 kPa partial pressure, against a sublimation pressure of about 27 kPa at 180 K, deposits as dry ice. Its 26 kJ/mol sublimation heat limits it to a few per cent of the feed: about 0.06 kg/s at 147 kmol/h.
- PASS: fresh world `format=9` (00c); views replay the last publication and advance in buckets (01, 10, 10b); Solids tab one row "Carbon dioxide (solid", 250.0 um, 0.00129 kg (10). With a top vent: 2.6, 3.2, 5.6, 7.6 kg, about 0.07 kg/s (18 to 20).
- PASS: rail solid share 0.4 % for 5.6 kg (3.6 L of 1000 L), and Mass includes it (19); basis and unit toggles keep the row (12); top port "gas outlet, drawing gas 141.5 kmol/h" (17).
- PASS: save and reload 1, `format=9 islands=3 certificates_saved=3 certificates_restored=3`, crystal 0.00129 kg before and after (13, 15).
- Not exercised: the hover tooltip (it needs the real cursor), a typed hold, a pending event across a reload. Save and reload 2 crashed (item 1).

## Open items (for the lead; none is M4 code)
1. **Crash on save after a reload, in main 0.8.0 runtime code (`PhysicalRegistry` unchanged by the port).**
   - A lone generator or void is an island of one. When a pipe joins it to another island, `PhysicalRegistry.apply` never lists that island in `affected`.
   - So the island stays in the coordinator, certified, and is saved: the probe shows islands 5, 6 and 10 beside the joined island 17.
   - After a reload, `load()` gives it no members, so the next save throws `NoSuchElementException` in `FluidWorldAuthority.capture` (`members(id).getFirst()`). The world then crashes at every later save.
   - A (recommended): a batch that puts a fixed boundary into a replacement island also retires that boundary's island of one (fenced, in `affected`); capture and load refuse an island with no device.
   - B: drop member-less islands at load. This is a patch and leaves the in-session leak.
2. **A frosting tank's side outlet shuts its gas, and the closure can stick.**
   - Physics: gas leaves a gas-filled tank through any nozzle, and frost stays on the walls.
   - Code: plan item 4 keeps only the gas port open. The side "mixed outlet" closes both ways ("blocked with solid: NO_LIQUID_CARRIER"), so the tank fills to the feed pressure and stops (07b, 16).
   - After that frost sublimed, the island certified at rest with the closure still in place: no solids, and 199 kPa across it (probe at 10:13). The cause is that `NetworkReduction.atRest` lifts closures only while a node holds solids.
   - A: the rest test reads every closure as open at an interval start, solids or not.
   - B (owner decision): a side or bottom outlet of a tank with no liquid passes its gas and keeps its solids.
3. The name column cuts "Carbon dioxide (solid)" at 854x480 and at GUI widths near 480; the hover shows the full name.
