# 2026-10-15-code-guide-findings

Purpose: The findings of the code-guide rewrite, decided by the owner ("no backward compatibility at current stage; others fix as recommended"): filter recovery queued as an engine event, `view` a cached read, column screen replays its last published state on open, compatibility paths removed (world-less checkpoint refused, column save retention and `INCOMPATIBLE`, preset diameter and appearance defaults), column stage-drop field and client preset schema 2

Status: Implemented 2026-10-15 (0.7.2). Index: [../INDEX.md](../INDEX.md).

## Documents

- [CODE_GUIDE_FINDINGS_REVIEW.md](CODE_GUIDE_FINDINGS_REVIEW.md)

## Code areas

`FluidWorldAuthority`, `FluidSavedData`, `ColumnPresentation`, `ColumnCalculatorV3BlockEntity`, `V3ClientPresets`, `ColumnCalculatorV3Screen`, `MaterialCatalog`, `MaterialPresets`; `test`, `fluidSolverRegression`, `runFluidGameTestServer`, `runColumnGameTestServer`, `runMcpClient`

## History

- Implemented 2026-10-15 (main `26ec48de`, mod 0.7.2; unit 1260 green after one wording fix, G2 exact 0, fluid GameTests 33/33, column 2/2; GUI 3/3 through the bridge)
