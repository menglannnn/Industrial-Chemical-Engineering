# Quick-check fixes on claude/unified-thermo-port (2026-10-17)

Commits on 31e25f6f: 8220d4bc (X), fd0dc531 (cold 85), 87c106f6 (cold 183); follow-up db6b1e6f (box), 95e4325d (dry
end); behaviour-free cleanup 04f84aa5, cc2f8c37; option (a) e4769fa2; the fixed-volume freeze 703d69b3; pure water's
triple point 7bd5fafc. Each compiles main and all 312 test sources; no Gradle, gates or campaign run. Tools, logs and
the detail trimmed from this report: `tools/unified-thermo-port-quick-fixes/`. Code guide at merge: thermo
(`RootAbsent`, `NumericalBox` partial pressure, `freezingAtVolume`, `waterTriplePoint`), fluid network (`holdsNoFluid`,
D33 dry end, `conversions`, `mayConvert`, `competes`), phase holds.

## X (bundled 145, 157, 164), cold 85, cold 183, cleanup
- X: a vapour's partial pressure is at most its node's, but its unknown had no bound (6.6e25 to 1.5e292 Pa, where the
  cubic loses its root). 8220d4bc `RootAbsent`; db6b1e6f the numerical box bounds it. Option: a 1e-6 Pa floor.
- 85: solids ride the liquid (D33): a vessel holding solids and no fluid gives nothing. fd0dc531 a kept vessel keeps its
  solids; 95e4325d `holdsNoFluid` is the dry end. Newton solves 668 -> 915 (not traced). Option: the ambient viscosity.
- 183: CO2-I in LNG at 140.9 K, 620 kPa; the crystal solve stopped on its first phase. 87c106f6: it reads all phases.
- 04f84aa5, cc2f8c37: phase holds are named so; `kij` moved into its test; `phaseSeedFallbacks` removed.

## Option (a), the fallback gate: e4769fa2
- Physics: the refresh before the rest test is for crystals, and water freezes only at or below T_m(P). Fix: `competes`
  (water at T <= T_m(P), or held ice); `conversions` visits only `mayConvert` nodes; `refresh` still visits every node.
- Gate `PrecipitatedSolidsTest#aWarmWaterVesselHasNothingToConvertAndAColdOneFreezes` (rules `skip`, `melting` off
  fail). No physical number moved; counters fell (LevelHeadTest's rest intervals, 12 and 100 Newton solves to 0).

## The fixed-volume freeze step: 703d69b3 (WP4 option 7)
- Physics: a rigid vessel of gas over water cooled below 0 °C freezes on its melting line, T = T_m(P): the ice fraction
  from U, P from V (the ice's 9 % more volume compresses the gas); a liquid-full vessel pressurises along the line.
- `FluidTpEquilibrium.freezingAtVolume`: at each P the warm side's TP at (T_m(P), P) gives the ice from the energy; a
  secant in P closes the volume. Gates fail with rule `uvfreeze` off: N2 over water from 263.15 K lands at 273.152218 K,
  105532.782 Pa, ice 0.1856, in 9 TP calls; gate (b) 15 TP calls (85 before). Only gate (b)'s TP count moved.

## Pure water's triple point: 7bd5fafc (the lead's direction, the limit of WP4's freeze step)
- Physics: pure water with only its own steam in a rigid vessel cooled through 0 °C stands at its triple point, 273.16 K
  and 611.657 Pa, ice, liquid and steam together, until its liquid is gone (then ice and steam on the sublimation line).
  No gas cushion: the amounts follow from the mole, energy and volume balances (steam 3.71 m3/mol, liquid 1.802e-5, ice
  1.965e-5). Before, U(T) at such a volume stepped by the latent heat within 2e-9 K, and UV refused the target as a step
  of U(T) (489 TP calls) or did not converge (2798).
- Fix: `FluidTpEquilibrium.waterTriplePoint`, in `pureTriplePoint`'s stage before the UV search, for a feed with no
  hydrocarbon: the ice record's anchor (`FreeWaterIce.triplePressure`, new) and the rule's steam, liquid and ice there,
  corners evaluated once per service, over the shared `tripleBalances`, `solve3` and `insideTriangle`.
- `insideTriangle` (CO2's `pureTriplePoint` too) sets a negative amount to 0 keeping the moles, and accepts only if the
  energy (1e-6 N R T) and ln V (1e-6) still meet the specification. Its 1e-6 N mole tolerance took liquid-full vessels
  (67 and 307 MPa) and 1e-7 mol of steam short (V 2 % off) in at the triple point, as CONVERGED.
- Gate `FreeWaterIceTest#pureWaterWithItsOwnSteamInARigidVesselStandsAtTheTriplePoint`: 1 mol in twice its liquid's
  volume at ice 0.1, 0.5, 0.9: exactly 273.16 K and 611.657 Pa, steam 4.81e-6 to 4.46e-6 mol, balances closed to 1e-9,
  0 TP calls; rule `watertriple` off fails it (a step of U(T)). Gate `#pureWaterJustOutsideItsTriplePoint...`: 1e-5 mol
  beyond the edge without ice boils at 273.160790 K, 611.6922 Pa (13 TP calls); without liquid sublimes at 273.158411 K,
  611.5771 Pa (54); 1e-10 mol of steam short is compressed on the melting line, 273.156095 K, 53263.5 Pa (15). Same
  with `watertriple` off, by design; rule `triangle` (the mole tolerance) fails it (V 2e-5 off at the triple point).
- 293 science and 177 runtime tests pass; against 703d69b3 no printed number moves, CO2's triple points included
  (wall-clock lines aside). FreezeUvProbe (a), 2 kJ/mol out of such a vessel: the triple point, ice 0.333, 0 TP calls.
