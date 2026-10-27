# M3a: D21 follow-ups (theta 1e-5, inner UV secant within one classification)
2026-10-16. Branch `claude/unified-thermo-port`, commit `cc1a39a1` on `8a06b22f` (owner, D21, 2026-10-15). Not pushed.
Tools, probes and logs: `tools/unified-thermo-port-m3a/` (README). Code guide: no change (refreshed at the merge).
## Physics and changes
- theta. N2 traces stay in the vapour and dilute it: the step moves by about f in ln P (12 K times f at 101 kPa). At 1e-5
  the rule's pure step is about 1.4e-5 off: 0.13 mK in PH, and row A's P is 6.9e-7 from the general solve.
  `NEAR_PURE_TRACES` 1e-6 -> 1e-5. `STEP_FLUID_TRACES` stays 1e-4, so the rule keeps at least 90 % of a step (was 99 %).
- innerUv. At fixed T, V(P) steps where a vapour-solid answer meets a vapour-liquid or liquid-solid one with the same
  phase count. The secant across the step (5.6e9) was stored, and the next search stepped 4.8e-16 in ln P, below its
  spacing. A secant is now taken only between answers of the same classification and phase count.
- Forms, on 42 UV targets at CO2's triple point (2e-5..1e-3 N2 or CH4). Chosen, compare only: stalls 5 -> 0, converged
  25 -> 29, TP eq 15701 -> 15834. Rejected: reset the stored slope at each change (+49 %), or per class (+69 %).
## Checks
- `science.thermo.*` and `science.material.*`: 249 tests in 51 classes, 0 failed (one Gradle run on the final code).
- Lane against 8a06b22f: G4F1, G4F2 and G5F1-G5F5 identical. No classification count changed.
- G5F6, two ethane-rich liquids, only round-trip residuals (bound 1e-9): [0,0,.9,.1] 1 MPa UV P 7.8e-12 -> 6.3e-12,
  crystal 9.2e-14 -> 9.7e-16; [0,0,.7,.3] 3 MPa UV P 3.1e-14 -> 1.0e-12. Reason: VL or VS answers beside LS ones get
  no secant, so the search stops elsewhere within tolerance. Same reason: GasSolidEquilibriumTest, 9 lines, <= 1e-12.
- D21 rows unchanged. New rows: PH f = 2e-6 (4 rows) and row A f = 1e-5. The above-bound lines move from 2e-6 to 2e-5.
## Probes, before -> after
- PH 101.325 kPa, f = 2e-6, dh = -1000 J: NOT_CONVERGED, 30 TP eq, 1021 kernel -> rule VAPOR_SOLID 194.627787659 K, 2, 87.
- Stall: CO2 with 1e-4 CH4, target 0.3 mol vapour and 1e-4 liquid at the triple point. 5 inner searches of 100 TP eq,
  1856 TP eq, 229535 kernel -> none, 1421, 179324. Both NOT_CONVERGED at the three-phase step (13 of 42 fail, untyped).
- Deep deposition at 101.325 kPa, f 2e-6..1e-5, vapour share 0.5..1e-4: 30 of 30 converge, handover 1.25 mK at f/1e-4.
## Where innerUv is reached
- Every UV solve, any competition: the start of a two-phase or wet feed, and each temperature of the nested search.
  Fluid-only tests run it (374 searches in FluidTpEquilibriumPhUvTest, 513 in G3F8) and print the same lines. Fluid-only,
  the rule acts on VAPOR_LIQUID beside LIQUID_LIQUID and on derivative-free single phases; wet answers are all FREE_WATER.
- Bundled worlds call only the engine's TP (`NetworkPhaseEngine.tp`): nothing bundled moves now. M2's vessel seeding and
  M3's crystal refresh will run fluid-only UV through it. For the owner: confirm "compare only" as the reading.
